<template>
  <div class="generic-template-editor">
    <el-alert
      class="mb-3"
      type="info"
      :closable="false"
      title="通用表格模板独立保存在类别规则中，不会改动表单字段或原成员表；学校端可在线填写，也可按模板下载、导入 Excel。"
    />
    <div class="template-toolbar">
      <div>
        <el-button type="primary" plain icon="Plus" :disabled="localTemplates.length >= MAX_GENERIC_TABLE_TEMPLATES" @click="addTemplate"
          >新增模板</el-button
        >
        <el-button plain icon="CopyDocument" :disabled="!activeTemplate || localTemplates.length >= MAX_GENERIC_TABLE_TEMPLATES" @click="copyTemplate"
          >复制当前模板</el-button
        >
        <el-button type="danger" plain icon="Delete" :disabled="!activeTemplate" @click="removeTemplate">删除当前模板</el-button>
      </div>
      <span class="template-limit">最多 {{ MAX_GENERIC_TABLE_TEMPLATES }} 个模板</span>
    </div>

    <el-empty v-if="!localTemplates.length" description="尚未配置通用表格模板" />
    <template v-else>
      <el-tabs v-model="activeKey" type="card" class="template-tabs">
        <el-tab-pane v-for="item in sortedTemplates" :key="item.templateKey" :name="item.templateKey">
          <template #label>
            <span>{{ item.templateName || item.templateKey }}</span>
            <el-tag v-if="item.enabled === false" class="ml-1" size="small" type="info">停用</el-tag>
          </template>
        </el-tab-pane>
      </el-tabs>

      <el-form v-if="activeTemplate" label-width="118px" class="template-form">
        <el-row :gutter="14">
          <el-col :span="8">
            <el-form-item label="模板名称"><el-input v-model="activeTemplate.templateName" maxlength="50" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="模板标识">
              <el-input v-model="activeTemplate.templateKey" maxlength="64" :disabled="templateHasDataHint" />
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="启用"><el-switch v-model="activeTemplate.enabled" /></el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="排序"><el-input-number v-model="activeTemplate.sortOrder" :min="0" controls-position="right" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="填报提示">
          <el-input v-model="activeTemplate.tipText" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="显示在学校端表格上方" />
        </el-form-item>

        <el-divider content-position="left">行数与 Excel</el-divider>
        <el-row :gutter="14">
          <el-col :span="6">
            <el-form-item label="最少行数"
              ><el-input-number v-model="activeTemplate.minRows" :min="0" :max="MAX_GENERIC_TABLE_ROWS" controls-position="right"
            /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="最多行数"
              ><el-input-number v-model="activeTemplate.maxRows" :min="0" :max="MAX_GENERIC_TABLE_ROWS" controls-position="right"
            /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="预留空白行"
              ><el-input-number v-model="activeTemplate.blankRows" :min="0" :max="MAX_GENERIC_TABLE_ROWS" controls-position="right"
            /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="Sheet 名称"><el-input v-model="activeTemplate.sheetName" maxlength="31" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="下载文件名"><el-input v-model="activeTemplate.fileName" placeholder="例如：作品数量采集表.xlsx" /></el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="第一行标题"><el-input v-model="activeTemplate.excelTitle" placeholder="不填则从字段标题行开始" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="末尾行说明">
          <el-input
            v-model="activeTemplate.footerText"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="生成在 Excel 数据区末尾"
          />
        </el-form-item>

        <el-divider content-position="left">表格字段</el-divider>
        <el-table border :data="activeTemplate.fields" size="small" max-height="300">
          <el-table-column label="操作" width="64" fixed="left" align="center">
            <template #default="scope"><el-button link type="danger" icon="Delete" @click="removeField(scope.$index)" /></template>
          </el-table-column>
          <el-table-column label="字段名称" min-width="150">
            <template #default="scope"><el-input v-model="scope.row.fieldLabel" maxlength="50" /></template>
          </el-table-column>
          <el-table-column label="field_key" min-width="150">
            <template #default="scope"><el-input v-model="scope.row.fieldKey" maxlength="64" /></template>
          </el-table-column>
          <el-table-column label="类型" width="130">
            <template #default="scope">
              <el-select v-model="scope.row.fieldType" class="w-full">
                <el-option v-for="item in fieldTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="校验" width="120">
            <template #default="scope">
              <el-select v-model="scope.row.validationType" class="w-full" clearable>
                <el-option v-for="item in validationOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="选项" min-width="170">
            <template #default="scope"
              ><el-input v-model="scope.row.optionsText" :disabled="!optionFieldTypes.includes(scope.row.fieldType)" placeholder="逗号分隔"
            /></template>
          </el-table-column>
          <el-table-column label="必填" width="70" align="center">
            <template #default="scope"><el-switch v-model="scope.row.required" /></template>
          </el-table-column>
          <el-table-column label="列宽" width="110">
            <template #default="scope"><el-input-number v-model="scope.row.width" :min="100" :max="600" controls-position="right" /></template>
          </el-table-column>
          <el-table-column label="排序" width="100">
            <template #default="scope"><el-input-number v-model="scope.row.sortOrder" :min="0" controls-position="right" /></template>
          </el-table-column>
        </el-table>
        <el-button
          class="mt-2"
          type="primary"
          plain
          icon="Plus"
          :disabled="activeTemplate.fields.length >= MAX_GENERIC_TABLE_COLUMNS"
          @click="addField"
        >
          添加字段
        </el-button>
      </el-form>
    </template>
  </div>
</template>

<script setup lang="ts">
import {
  GenericTableTemplate,
  MAX_GENERIC_TABLE_COLUMNS,
  MAX_GENERIC_TABLE_ROWS,
  MAX_GENERIC_TABLE_TEMPLATES,
  cloneGenericTableTemplate,
  createGenericTableColumn,
  createGenericTableTemplate,
  normalizeGenericTableTemplates
} from '@/utils/artGenericTable';

const props = defineProps<{ modelValue: GenericTableTemplate[] }>();
const emit = defineEmits<{ (e: 'update:modelValue', value: GenericTableTemplate[]): void }>();

const localTemplates = ref<GenericTableTemplate[]>([]);
const activeKey = ref('');
let lastEmitted: GenericTableTemplate[] | undefined;

watch(
  () => props.modelValue,
  (value) => {
    if (value === lastEmitted) return;
    localTemplates.value = normalizeGenericTableTemplates(value);
    if (!localTemplates.value.some((item) => item.templateKey === activeKey.value)) activeKey.value = localTemplates.value[0]?.templateKey || '';
  },
  { immediate: true }
);

watch(
  localTemplates,
  (value) => {
    lastEmitted = value;
    emit('update:modelValue', value);
  },
  { deep: true }
);

const sortedTemplates = computed(() => [...localTemplates.value].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0)));
const activeTemplate = computed(() => localTemplates.value.find((item) => item.templateKey === activeKey.value));
const templateHasDataHint = true;
const optionFieldTypes = ['select', 'radio', 'checkbox'];
const fieldTypeOptions = [
  { label: '单行文本', value: 'input' },
  { label: '多行文本', value: 'textarea' },
  { label: '数字', value: 'number' },
  { label: '下拉单选', value: 'select' },
  { label: '平铺单选', value: 'radio' },
  { label: '多选', value: 'checkbox' },
  { label: '日期', value: 'date' }
];
const validationOptions = [
  { label: '手机号', value: 'phone' },
  { label: '邮箱', value: 'email' },
  { label: '身份证号', value: 'idcard' },
  { label: '整数', value: 'integer' },
  { label: '非负数', value: 'nonnegative' }
];

const addTemplate = () => {
  const usedKeys = new Set(localTemplates.value.map((item) => item.templateKey));
  const template = createGenericTableTemplate(localTemplates.value.length, usedKeys);
  localTemplates.value.push(template);
  activeKey.value = template.templateKey;
};

const copyTemplate = () => {
  if (!activeTemplate.value) return;
  const usedKeys = new Set(localTemplates.value.map((item) => item.templateKey));
  const template = cloneGenericTableTemplate(activeTemplate.value, localTemplates.value.length, usedKeys);
  localTemplates.value.push(template);
  activeKey.value = template.templateKey;
};

const removeTemplate = async () => {
  if (!activeTemplate.value) return;
  await ElMessageBox.confirm(
    `删除模板“${activeTemplate.value.templateName}”？已填报项目中的历史数据不会被自动删除，但学校端将不再显示该表格。`,
    '删除模板',
    { type: 'warning' }
  );
  const index = localTemplates.value.findIndex((item) => item.templateKey === activeKey.value);
  localTemplates.value.splice(index, 1);
  activeKey.value = localTemplates.value[Math.max(0, index - 1)]?.templateKey || '';
};

const addField = () => {
  if (!activeTemplate.value) return;
  const usedKeys = new Set(activeTemplate.value.fields.map((item) => item.fieldKey));
  activeTemplate.value.fields.push(createGenericTableColumn(activeTemplate.value.fields.length, usedKeys));
};

const removeField = (index: number) => {
  activeTemplate.value?.fields.splice(index, 1);
};
</script>

<style scoped>
.template-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.template-limit {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.template-tabs {
  margin-bottom: 12px;
}

.template-form :deep(.el-form-item) {
  margin-bottom: 14px;
}
</style>
