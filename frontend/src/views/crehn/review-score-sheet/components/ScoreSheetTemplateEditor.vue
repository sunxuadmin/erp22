<template>
  <div class="score-sheet-template-editor">
    <el-form label-position="top">
      <el-form-item v-if="showTemplateName" label="模板名称">
        <el-input v-model="model.templateName" maxlength="100" show-word-limit placeholder="用于区分不同评分表模板" />
      </el-form-item>

      <el-form-item label="表格标题">
        <el-input v-model="model.title" maxlength="100" show-word-limit placeholder="请输入导出表格标题" />
      </el-form-item>

      <el-form-item label="表格列">
        <el-table :data="model.columns" row-key="key" border size="small" class="column-config-table">
          <el-table-column type="index" label="顺序" width="58" align="center" />
          <el-table-column label="列名称" min-width="145">
            <template #default="scope">
              <el-input v-model="scope.row.label" maxlength="30" />
            </template>
          </el-table-column>
          <el-table-column label="导出显示" width="92" align="center">
            <template #default="scope">
              <el-switch v-model="scope.row.visible" />
            </template>
          </el-table-column>
          <el-table-column label="调整顺序" width="118" align="center">
            <template #default="scope">
              <el-button link type="primary" :disabled="scope.$index === 0" @click="moveColumn(scope.$index, -1)">上移</el-button>
              <el-button link type="primary" :disabled="scope.$index === model.columns.length - 1" @click="moveColumn(scope.$index, 1)">
                下移
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form-item>

      <el-alert class="mb-[16px]" type="warning" show-icon :closable="false" title="“状态”列默认不显示；如需展示，可打开对应开关。" />

      <div class="footer-editor-head">
        <div>
          <strong>底部签字区</strong>
          <span>可留空或配置最多 6 行，每行可分别配置左侧和右侧字段。</span>
        </div>
        <el-button type="primary" plain icon="Plus" :disabled="model.footerRows.length >= 6" @click="addFooterRow">新增一行</el-button>
      </div>

      <div v-if="model.footerRows.length" class="footer-row-list">
        <div v-for="(row, rowIndex) in model.footerRows" :key="rowIndex" class="footer-row-card">
          <div class="footer-row-head">
            <span>第 {{ rowIndex + 1 }} 行</span>
            <div>
              <el-button link type="primary" :disabled="rowIndex === 0" @click="moveFooterRow(rowIndex, -1)">上移</el-button>
              <el-button link type="primary" :disabled="rowIndex === model.footerRows.length - 1" @click="moveFooterRow(rowIndex, 1)">下移</el-button>
              <el-button link type="danger" @click="removeFooterRow(rowIndex)">删除</el-button>
            </div>
          </div>
          <div class="footer-side-grid">
            <div v-for="side in footerSides" :key="side.value" class="footer-side-panel">
              <div class="footer-side-head">
                <span>{{ side.label }}</span>
                <el-button v-if="!row[side.value]" link type="primary" icon="Plus" @click="addFooterField(rowIndex, side.value)">添加字段</el-button>
                <el-button v-else link type="danger" icon="Delete" @click="removeFooterField(rowIndex, side.value)">移除</el-button>
              </div>
              <template v-if="row[side.value]">
                <el-input
                  v-model="row[side.value]!.label"
                  maxlength="30"
                  :placeholder="isSignatureField(row[side.value]) ? '正式显示文字，例如：评分老师签字：' : '正式显示文字'"
                />
                <div class="footer-field-options">
                  <el-select v-model="row[side.value]!.type" style="width: 128px" @change="handleFooterFieldTypeChange(rowIndex, side.value)">
                    <el-option label="普通文字" value="text" />
                    <el-option label="导出时间" value="exportTime" />
                    <el-option label="评分老师签名" value="signature" :disabled="signatureTypeDisabled(rowIndex, side.value)" />
                  </el-select>
                  <el-select v-if="row[side.value]!.type !== 'exportTime'" v-model="row[side.value]!.lineLength" style="width: 128px">
                    <el-option label="不显示横线" value="none" />
                    <el-option label="短横线" value="short" />
                    <el-option label="中横线" value="medium" />
                    <el-option label="长横线" value="long" />
                  </el-select>
                  <el-tag v-else-if="row[side.value]!.type === 'exportTime'" type="info" effect="plain">精确到分钟</el-tag>
                  <el-tag v-else-if="isSignatureField(row[side.value])" type="success" effect="plain">只允许一个评分老师签名槽</el-tag>
                </div>
                <div v-if="isSignatureField(row[side.value])" class="footer-signature-copy-grid">
                  <el-input v-model="row[side.value]!.signatureButtonText" maxlength="20" placeholder="按钮文字：点击签名" />
                  <el-input v-model="row[side.value]!.signatureHintText" maxlength="30" placeholder="提示文字：点击签字或点选签名" />
                  <el-input
                    v-model="row[side.value]!.signatureTimeText"
                    maxlength="20"
                    placeholder="签名时间标题：签名时间："
                    :disabled="row[side.value]!.signatureTimeVisible === false"
                  />
                </div>
                <div v-if="isSignatureField(row[side.value])" class="footer-signature-switches">
                  <el-switch v-model="row[side.value]!.signatureRequired" active-text="签名必填" inactive-text="可选签名" />
                  <el-switch v-model="row[side.value]!.signatureTimeVisible" active-text="显示签名时间" inactive-text="隐藏签名时间" />
                </div>
              </template>
              <el-empty v-else :image-size="34" description="该位置留空" />
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂不显示底部签字区" />
    </el-form>
  </div>
</template>

<script setup lang="ts">
import type { ReviewScoreSheetFooterFieldVO, ReviewScoreSheetFooterRowVO, ReviewScoreSheetTemplateVO } from '@/api/crehn/types';

const props = withDefaults(
  defineProps<{
    modelValue: ReviewScoreSheetTemplateVO;
    showTemplateName?: boolean;
  }>(),
  {
    showTemplateName: true
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: ReviewScoreSheetTemplateVO): void;
}>();

const model = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
});

const footerSides = [
  { label: '左侧字段', value: 'left' as const },
  { label: '右侧字段', value: 'right' as const }
];

const signatureFieldCount = () =>
  model.value.footerRows.flatMap((row) => [row.left, row.right]).filter((field) => field?.type === 'signature').length;

const isSignatureField = (field?: ReviewScoreSheetFooterFieldVO | null) => field?.type === 'signature';

const ensureSignatureDefaults = () => {
  model.value.footerRows
    .flatMap((row) => [row.left, row.right])
    .forEach((field) => {
      if (!isSignatureField(field)) return;
      field.signatureRequired ??= true;
      field.signatureTimeVisible ??= true;
    });
};

watch(
  () => model.value.footerRows,
  () => ensureSignatureDefaults(),
  { deep: true, immediate: true }
);

const signatureTypeDisabled = (rowIndex: number, side: 'left' | 'right') => {
  const current = model.value.footerRows[rowIndex]?.[side];
  return current?.type !== 'signature' && signatureFieldCount() >= 1;
};

const emitClone = () => emit('update:modelValue', { ...model.value, columns: [...model.value.columns], footerRows: [...model.value.footerRows] });

const moveColumn = (index: number, offset: number) => {
  const nextIndex = index + offset;
  if (nextIndex < 0 || nextIndex >= model.value.columns.length) return;
  const columns = [...model.value.columns];
  [columns[index], columns[nextIndex]] = [columns[nextIndex], columns[index]];
  model.value.columns = columns;
  emitClone();
};

const addFooterRow = () => {
  if (model.value.footerRows.length >= 6) return;
  model.value.footerRows = [
    ...model.value.footerRows,
    {
      left: { label: '', type: 'text', lineLength: 'none' },
      right: null
    }
  ];
  emitClone();
};

const removeFooterRow = (index: number) => {
  model.value.footerRows = model.value.footerRows.filter((_, rowIndex) => rowIndex !== index);
  emitClone();
};

const moveFooterRow = (index: number, offset: number) => {
  const nextIndex = index + offset;
  if (nextIndex < 0 || nextIndex >= model.value.footerRows.length) return;
  const rows = [...model.value.footerRows];
  [rows[index], rows[nextIndex]] = [rows[nextIndex], rows[index]];
  model.value.footerRows = rows;
  emitClone();
};

const addFooterField = (rowIndex: number, side: 'left' | 'right') => {
  const field: ReviewScoreSheetFooterFieldVO = { label: '', type: 'text', lineLength: 'none' };
  const rows = [...model.value.footerRows];
  rows[rowIndex] = { ...rows[rowIndex], [side]: field } as ReviewScoreSheetFooterRowVO;
  model.value.footerRows = rows;
  emitClone();
};

const handleFooterFieldTypeChange = (rowIndex: number, side: 'left' | 'right') => {
  const field = model.value.footerRows[rowIndex]?.[side];
  if (!field) return;
  if (field.type === 'signature') {
    field.slotKey = field.slotKey || 'reviewer_signature';
    field.lineLength = field.lineLength === 'none' ? 'long' : field.lineLength;
    if (!field.label?.trim()) field.label = '评分老师签字：';
    field.signatureButtonText ??= '点击签名';
    field.signatureHintText ??= '点击签字或点选签名';
    field.signatureTimeText ??= '签名时间：';
    field.signatureRequired ??= true;
    field.signatureTimeVisible ??= true;
  } else {
    delete field.slotKey;
    delete field.signatureButtonText;
    delete field.signatureHintText;
    delete field.signatureTimeText;
    delete field.signatureRequired;
    delete field.signatureTimeVisible;
  }
  emitClone();
};

const removeFooterField = (rowIndex: number, side: 'left' | 'right') => {
  const rows = [...model.value.footerRows];
  rows[rowIndex] = { ...rows[rowIndex], [side]: null } as ReviewScoreSheetFooterRowVO;
  model.value.footerRows = rows;
  emitClone();
};
</script>

<style scoped lang="scss">
.column-config-table {
  width: 100%;
}

.footer-editor-head,
.footer-row-head,
.footer-side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.footer-editor-head {
  margin: 4px 0 12px;

  > div:first-child {
    display: flex;
    flex-direction: column;
    gap: 3px;
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}

.footer-row-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.footer-row-card {
  padding: 10px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);
}

.footer-row-head {
  margin-bottom: 8px;
  font-weight: 600;
}

.footer-side-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.footer-side-panel {
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 5px;
  background: var(--el-bg-color);
}

.footer-side-head {
  margin-bottom: 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.footer-field-options {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.footer-signature-copy-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 8px;
}

.footer-signature-switches {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 10px;
}

@media (max-width: 760px) {
  .footer-signature-copy-grid {
    grid-template-columns: 1fr;
  }
}

.footer-side-panel :deep(.el-empty) {
  padding: 4px 0;
}

@media (max-width: 760px) {
  .footer-side-grid {
    grid-template-columns: 1fr;
  }
}
</style>
