<template>
  <div class="p-2 score-sheet-page">
    <el-alert
      class="mb-[10px]"
      type="info"
      show-icon
      :closable="false"
      title="评分表模板为全系统共享配置。可保存多个模板并指定一个默认模板；模板只定义评分老师签名槽，不保存任何个人手写签名图片。"
    />

    <el-card v-loading="loading" shadow="hover" class="mb-[10px] template-toolbar-card">
      <div class="template-toolbar">
        <div class="template-selector">
          <span>当前模板</span>
          <el-select v-model="selectedTemplateKey" style="width: 280px" @change="handleTemplateChange">
            <el-option v-for="item in templates" :key="templateKey(item)" :label="item.templateName" :value="templateKey(item)">
              <span>{{ item.templateName }}</span>
              <el-tag v-if="item.defaultTemplate" class="ml-[8px]" size="small" type="success">默认</el-tag>
            </el-option>
          </el-select>
          <el-tag type="info" effect="plain">{{ templates.length }}/20 个</el-tag>
          <el-tag v-if="form.defaultTemplate" type="success" effect="plain">默认模板</el-tag>
        </div>
        <div class="template-toolbar-actions">
          <el-button v-hasPermi="['crehn:reviewSheetTemplate:edit']" icon="Plus" :disabled="templates.length >= 20" @click="createTemplate">
            新建
          </el-button>
          <el-button v-hasPermi="['crehn:reviewSheetTemplate:edit']" icon="CopyDocument" :disabled="!form" @click="copyTemplate">复制</el-button>
          <el-button v-hasPermi="['crehn:reviewSheetTemplate:edit']" icon="Select" :disabled="form.defaultTemplate || !form.id" @click="makeDefault">
            设为默认
          </el-button>
          <el-button
            v-hasPermi="['crehn:reviewSheetTemplate:edit']"
            type="danger"
            plain
            icon="Delete"
            :disabled="form.defaultTemplate || templates.length <= 1 || !form.id"
            @click="removeTemplate"
          >
            删除
          </el-button>
        </div>
      </div>
    </el-card>

    <div class="score-sheet-layout">
      <el-card v-loading="loading" shadow="hover" class="score-sheet-editor">
        <template #header>
          <div class="card-header">
            <div>
              <strong>模板设置</strong>
              <span>模板修改需要点击保存，不会实时覆盖共享配置。</span>
            </div>
            <el-tag type="info" effect="plain">版本 {{ form.version }}</el-tag>
          </div>
        </template>

        <ScoreSheetTemplateEditor v-model="form" />

        <div class="template-meta">
          <span>最后修改人：{{ form.updatedByName || '尚未保存' }}</span>
          <span>修改时间：{{ form.updateTime || '-' }}</span>
        </div>
        <div class="editor-actions">
          <el-button icon="Refresh" @click="loadTemplates(form.id)">刷新</el-button>
          <el-button v-hasPermi="['crehn:reviewSheetTemplate:edit']" icon="RefreshLeft" @click="restoreDefault">恢复系统版式</el-button>
          <el-button v-hasPermi="['crehn:reviewSheetTemplate:edit']" type="primary" icon="Check" :loading="saving" @click="saveTemplate">
            保存模板
          </el-button>
        </div>
      </el-card>

      <el-card shadow="hover" class="score-sheet-preview">
        <template #header>
          <div class="card-header">
            <div>
              <strong>导出预览</strong>
              <span>示例数据用于检查列顺序、评分老师签名槽和页脚是否越界。</span>
            </div>
            <el-tag type="success" effect="plain">{{ visibleColumnCount }} 列</el-tag>
          </div>
        </template>
        <ScoreSheetPaperPreview :template="form" :rows="previewRows" :export-time="previewExportTime" show-optional-signature />
      </el-card>
    </div>
  </div>
</template>

<script setup name="ArtReviewScoreSheet" lang="ts">
import {
  createReviewScoreSheetTemplate,
  deleteReviewScoreSheetTemplate,
  listReviewScoreSheetTemplates,
  resetReviewScoreSheetTemplateById,
  setDefaultReviewScoreSheetTemplate,
  updateReviewScoreSheetTemplate
} from '@/api/crehn/review';
import type { ReviewScoreSheetTemplateVO } from '@/api/crehn/types';
import { ElMessageBox } from 'element-plus';
import ScoreSheetPaperPreview from './components/ScoreSheetPaperPreview.vue';
import ScoreSheetTemplateEditor from './components/ScoreSheetTemplateEditor.vue';
import { cloneScoreSheetTemplate, emptyScoreSheetTemplate, exampleScoreSheetRows, validateScoreSheetTemplate } from './scoreSheet';

// ART-OWNER: FE.REVIEW.SCORE_SHEET
const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const loading = ref(false);
const saving = ref(false);
const templates = ref<ReviewScoreSheetTemplateVO[]>([]);
const selectedTemplateKey = ref('');
const form = ref<ReviewScoreSheetTemplateVO>(emptyScoreSheetTemplate('默认评审打分表'));
const previewRows = exampleScoreSheetRows();

const templateKey = (template: ReviewScoreSheetTemplateVO) => String(template.id ?? 'virtual-default');
const visibleColumnCount = computed(() => form.value.columns.filter((column) => column.visible).length);
const previewExportTime = computed(() => {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`;
});

const applyTemplate = (template?: ReviewScoreSheetTemplateVO) => {
  form.value = template ? cloneScoreSheetTemplate(template) : emptyScoreSheetTemplate();
  selectedTemplateKey.value = templateKey(form.value);
};

const loadTemplates = async (preferredId?: string | number) => {
  loading.value = true;
  try {
    const { data } = await listReviewScoreSheetTemplates();
    templates.value = data || [];
    const preferredKey = preferredId == null ? selectedTemplateKey.value : String(preferredId);
    const selected =
      templates.value.find((item) => templateKey(item) === preferredKey) ||
      templates.value.find((item) => item.defaultTemplate) ||
      templates.value[0];
    applyTemplate(selected);
  } finally {
    loading.value = false;
  }
};

const handleTemplateChange = (value: string) => {
  const selected = templates.value.find((item) => templateKey(item) === value);
  if (selected) applyTemplate(selected);
};

const promptTemplateName = async (title: string, initialValue: string) => {
  const result = await ElMessageBox.prompt('请输入模板名称', title, {
    inputValue: initialValue,
    inputPattern: /\S+/,
    inputErrorMessage: '模板名称不能为空',
    inputValidator: (value) => (String(value || '').trim().length <= 100 ? true : '模板名称不能超过100个字符')
  });
  return String(result.value || '').trim();
};

const createTemplate = async () => {
  try {
    const name = await promptTemplateName('新建评分表模板', `评分表模板${templates.value.length + 1}`);
    const { data } = await createReviewScoreSheetTemplate(emptyScoreSheetTemplate(name));
    proxy?.$modal.msgSuccess('模板已新建');
    await loadTemplates(data.id);
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return;
  }
};

const copyTemplate = async () => {
  try {
    const name = await promptTemplateName('复制评分表模板', `${form.value.templateName}-副本`);
    const copy = cloneScoreSheetTemplate(form.value);
    delete copy.id;
    copy.templateName = name;
    copy.defaultTemplate = false;
    copy.version = 0;
    delete copy.updateBy;
    delete copy.updatedByName;
    delete copy.updateTime;
    const { data } = await createReviewScoreSheetTemplate(copy);
    proxy?.$modal.msgSuccess('模板已复制');
    await loadTemplates(data.id);
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return;
  }
};

const handleConflict = async (error: unknown) => {
  const message = error instanceof Error ? error.message : String(error || '');
  if (!message.includes('模板已被其他用户修改')) return false;
  await proxy?.$modal.alertWarning('模板已被其他用户修改，将为你载入最新版本。');
  await loadTemplates(form.value.id);
  return true;
};

const saveTemplate = async () => {
  const validationMessage = validateScoreSheetTemplate(form.value);
  if (validationMessage) {
    proxy?.$modal.msgWarning(validationMessage);
    return;
  }
  saving.value = true;
  try {
    const response = form.value.id
      ? await updateReviewScoreSheetTemplate(form.value.id, form.value)
      : await createReviewScoreSheetTemplate(form.value);
    proxy?.$modal.msgSuccess('模板已保存，后续导出可立即使用');
    await loadTemplates(response.data.id);
  } catch (error) {
    await handleConflict(error);
  } finally {
    saving.value = false;
  }
};

const makeDefault = async () => {
  if (!form.value.id) return;
  try {
    await proxy?.$modal.confirm(`确认将“${form.value.templateName}”设为默认导出模板吗？`);
  } catch {
    return;
  }
  const { data } = await setDefaultReviewScoreSheetTemplate(form.value.id);
  proxy?.$modal.msgSuccess('默认模板已更新');
  await loadTemplates(data.id);
};

const removeTemplate = async () => {
  if (!form.value.id) return;
  try {
    await proxy?.$modal.confirm(`确认删除模板“${form.value.templateName}”吗？`);
  } catch {
    return;
  }
  await deleteReviewScoreSheetTemplate(form.value.id);
  proxy?.$modal.msgSuccess('模板已删除');
  selectedTemplateKey.value = '';
  await loadTemplates();
};

const restoreDefault = async () => {
  try {
    await proxy?.$modal.confirm('确认将当前模板恢复为系统默认标题、列顺序和签字区吗？');
  } catch {
    return;
  }
  if (!form.value.id) {
    const restored = emptyScoreSheetTemplate(form.value.templateName);
    restored.defaultTemplate = form.value.defaultTemplate;
    form.value = restored;
    return;
  }
  saving.value = true;
  try {
    const { data } = await resetReviewScoreSheetTemplateById(form.value.id, form.value.version);
    proxy?.$modal.msgSuccess('已恢复系统默认版式');
    await loadTemplates(data.id);
  } catch (error) {
    await handleConflict(error);
  } finally {
    saving.value = false;
  }
};

onMounted(loadTemplates);
</script>

<style scoped lang="scss">
.template-toolbar-card :deep(.el-card__body) {
  padding: 14px 16px;
}

.template-toolbar,
.template-selector,
.template-toolbar-actions,
.card-header,
.editor-actions,
.template-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.template-toolbar {
  justify-content: space-between;
  flex-wrap: wrap;
}

.template-selector > span:first-child {
  font-weight: 600;
}

.score-sheet-layout {
  display: grid;
  grid-template-columns: minmax(470px, 0.95fr) minmax(540px, 1.2fr);
  gap: 10px;
  align-items: start;
}

.card-header {
  align-items: flex-start;
  justify-content: space-between;

  > div:first-child {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  strong {
    font-size: 16px;
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}

.template-meta {
  justify-content: space-between;
  margin-top: 16px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.editor-actions {
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 1180px) {
  .score-sheet-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .template-toolbar,
  .template-selector,
  .template-toolbar-actions,
  .template-meta {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
