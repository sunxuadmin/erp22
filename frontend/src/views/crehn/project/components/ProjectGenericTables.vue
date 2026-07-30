<template>
  <section v-if="enabledTemplates.length" class="generic-table-card">
    <div class="generic-table-heading">
      <div>
        <h3>填报表格</h3>
        <span>支持在线填写，也可分别下载和导入 Excel 模板</span>
      </div>
    </div>

    <el-tabs v-model="activeKey" type="border-card">
      <el-tab-pane v-for="template in enabledTemplates" :key="template.templateKey" :label="template.templateName" :name="template.templateKey">
        <el-alert v-if="template.tipText" class="mb-3" type="info" :closable="false" :title="template.tipText" show-icon />
        <div class="generic-table-toolbar">
          <div class="generic-table-count">
            已填写 {{ meaningfulRows(template).length }} 行
            <span v-if="template.minRows">，至少 {{ template.minRows }} 行</span>
            <span v-if="template.maxRows">，最多 {{ template.maxRows }} 行</span>
          </div>
          <div class="generic-table-actions">
            <el-button icon="Download" @click="downloadTemplate(template)">下载模板</el-button>
            <el-upload
              v-if="editable"
              :show-file-list="false"
              accept=".xlsx,.xls"
              :http-request="(option: any) => importTemplate(template, option, true)"
            >
              <el-button plain icon="Upload">导入并替换</el-button>
            </el-upload>
            <el-upload
              v-if="editable"
              :show-file-list="false"
              accept=".xlsx,.xls"
              :http-request="(option: any) => importTemplate(template, option, false)"
            >
              <el-button plain icon="Plus">追加导入</el-button>
            </el-upload>
            <el-button
              v-if="editable"
              type="primary"
              plain
              icon="Plus"
              :disabled="!!template.maxRows && rowsFor(template).length >= template.maxRows"
              @click="addRow(template)"
            >
              新增一行
            </el-button>
          </div>
        </div>

        <el-table border :data="rowsFor(template)" class="generic-data-table" empty-text="暂无数据，可新增一行或导入 Excel">
          <el-table-column v-if="editable" label="操作" width="70" fixed="left" align="center">
            <template #default="scope"><el-button link type="danger" icon="Delete" @click="removeRow(template, scope.$index)" /></template>
          </el-table-column>
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column v-for="field in sortedFields(template)" :key="field.fieldKey" :label="field.fieldLabel" :min-width="field.width || 160">
            <template #header> <span v-if="field.required" class="required-mark">*</span>{{ field.fieldLabel }} </template>
            <template #default="scope">
              <el-select v-if="field.fieldType === 'select'" v-model="scope.row[field.fieldKey]" class="w-full" clearable :disabled="!editable">
                <el-option v-for="option in genericTableFieldOptions(field)" :key="option" :label="option" :value="option" />
              </el-select>
              <el-radio-group v-else-if="field.fieldType === 'radio'" v-model="scope.row[field.fieldKey]" :disabled="!editable">
                <el-radio v-for="option in genericTableFieldOptions(field)" :key="option" :label="option">{{ option }}</el-radio>
              </el-radio-group>
              <el-checkbox-group v-else-if="field.fieldType === 'checkbox'" v-model="scope.row[field.fieldKey]" :disabled="!editable">
                <el-checkbox v-for="option in genericTableFieldOptions(field)" :key="option" :label="option">{{ option }}</el-checkbox>
              </el-checkbox-group>
              <el-date-picker
                v-else-if="field.fieldType === 'date'"
                v-model="scope.row[field.fieldKey]"
                class="w-full"
                value-format="YYYY-MM-DD"
                :disabled="!editable"
              />
              <el-input-number
                v-else-if="field.fieldType === 'number'"
                v-model="scope.row[field.fieldKey]"
                class="w-full"
                :disabled="!editable"
                controls-position="right"
              />
              <el-input
                v-else
                v-model="scope.row[field.fieldKey]"
                :type="field.fieldType === 'textarea' ? 'textarea' : 'text'"
                :rows="2"
                :disabled="!editable"
              />
            </template>
          </el-table-column>
        </el-table>
        <div v-if="template.footerText" class="generic-table-footer">{{ template.footerText }}</div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import { downloadProjectGenericTableTemplate, importProjectGenericTable } from '@/api/crehn/project';
import {
  GenericTableData,
  GenericTableRow,
  GenericTableTemplate,
  MAX_GENERIC_TABLE_DATA_BYTES,
  genericTableFieldOptions,
  normalizeGenericTableData
} from '@/utils/artGenericTable';
import { blobValidate } from '@/utils/ruoyi';
import FileSaver from 'file-saver';

const props = defineProps<{
  templates: GenericTableTemplate[];
  modelValue: GenericTableData;
  editable: boolean;
  projectId?: string | number;
  categoryId?: string | number;
  ensureProject: () => Promise<string | number>;
  persist: () => Promise<void>;
}>();
const emit = defineEmits<{
  (e: 'update:modelValue', value: GenericTableData): void;
}>();

const localData = ref<GenericTableData>({});
const activeKey = ref('');
let lastEmitted: GenericTableData | undefined;

watch(
  () => props.modelValue,
  (value) => {
    if (value === lastEmitted) return;
    localData.value = normalizeGenericTableData(value);
  },
  { immediate: true }
);

watch(
  localData,
  (value) => {
    lastEmitted = value;
    emit('update:modelValue', value);
  },
  { deep: true }
);

const enabledTemplates = computed(() =>
  [...props.templates.filter((item) => item.enabled !== false)].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
);

watch(
  enabledTemplates,
  (templates) => {
    if (!templates.some((item) => item.templateKey === activeKey.value)) activeKey.value = templates[0]?.templateKey || '';
  },
  { immediate: true }
);

const hasValue = (value: unknown) => (Array.isArray(value) ? value.length > 0 : value !== null && value !== undefined && String(value).trim() !== '');
const rowsFor = (template: GenericTableTemplate) =>
  Array.isArray(localData.value[template.templateKey]) ? localData.value[template.templateKey] : [];
const meaningfulRows = (template: GenericTableTemplate) =>
  rowsFor(template).filter((row) => template.fields.some((field) => hasValue(row[field.fieldKey])));
const sortedFields = (template: GenericTableTemplate) => [...template.fields].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
const newRow = (template: GenericTableTemplate): GenericTableRow =>
  Object.fromEntries(template.fields.map((field) => [field.fieldKey, field.fieldType === 'checkbox' ? [] : undefined]));

const addRow = (template: GenericTableTemplate) => {
  if (!Array.isArray(localData.value[template.templateKey])) localData.value[template.templateKey] = [];
  localData.value[template.templateKey].push(newRow(template));
};
const removeRow = (template: GenericTableTemplate, index: number) => rowsFor(template).splice(index, 1);

const downloadTemplate = async (template: GenericTableTemplate) => {
  const blob = await downloadProjectGenericTableTemplate({
    tableKey: template.templateKey,
    projectId: props.projectId,
    categoryId: props.categoryId
  });
  if (!blobValidate(blob)) {
    const text = await new Blob([blob as any]).text();
    const error = JSON.parse(text || '{}');
    ElMessage.error(error.msg || '下载表格模板失败');
    return;
  }
  const fileName = (template.fileName || `${template.templateName}.xlsx`).replace(/\.xls$/i, '.xlsx');
  FileSaver.saveAs(new Blob([blob as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }), fileName);
};

const importTemplate = async (template: GenericTableTemplate, option: any, replace: boolean) => {
  try {
    if (option.file?.size > 5 * 1024 * 1024) throw new Error('Excel 文件不能超过 5MB');
    const projectId = await props.ensureProject();
    const res = await importProjectGenericTable(projectId, template.templateKey, option.file);
    const importedRows = Array.isArray(res.data?.rows) ? res.data.rows : [];
    const nextRows = replace ? importedRows : [...rowsFor(template), ...importedRows];
    if (template.maxRows && nextRows.length > template.maxRows) throw new Error(`导入后超过最多 ${template.maxRows} 行`);
    const nextData = { ...localData.value, [template.templateKey]: nextRows };
    if (new TextEncoder().encode(JSON.stringify(nextData)).length > MAX_GENERIC_TABLE_DATA_BYTES)
      throw new Error('导入后的通用表格数据过大，请拆分文件或精简内容');
    localData.value = nextData;
    await nextTick();
    await props.persist();
    option.onSuccess?.(res.data);
    ElMessage.success(`${template.templateName}已${replace ? '替换' : '追加'}导入 ${importedRows.length} 行`);
  } catch (error: any) {
    option.onError?.(error);
    ElMessage.error(error?.response?.data?.msg || error?.message || '导入表格失败');
  }
};
</script>

<style scoped>
.generic-table-card {
  margin-top: 18px;
}

.generic-table-heading,
.generic-table-toolbar,
.generic-table-actions {
  display: flex;
  align-items: center;
}

.generic-table-heading {
  justify-content: space-between;
  margin-bottom: 12px;
}

.generic-table-heading h3 {
  margin: 0 0 4px;
  font-size: 17px;
}

.generic-table-heading span,
.generic-table-count {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.generic-table-toolbar {
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.generic-table-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.generic-table-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.required-mark {
  margin-right: 3px;
  color: var(--el-color-danger);
}

.generic-table-footer {
  margin-top: 10px;
  padding: 10px 12px;
  color: var(--el-text-color-secondary);
  white-space: pre-wrap;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

@media (max-width: 900px) {
  .generic-table-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .generic-table-actions {
    justify-content: flex-start;
  }
}
</style>
