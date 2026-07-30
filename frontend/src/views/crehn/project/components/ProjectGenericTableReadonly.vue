<template>
  <template v-if="templates.length">
    <el-divider content-position="left">填报表格</el-divider>
    <el-tabs type="border-card" class="readonly-table-tabs">
      <el-tab-pane v-for="template in templates" :key="template.templateKey" :label="template.templateName">
        <el-alert v-if="template.tipText" class="mb-2" type="info" :closable="false" :title="template.tipText" />
        <el-table border :data="rowsFor(template)" size="small" max-height="280" empty-text="未填写">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column
            v-for="field in sortedFields(template)"
            :key="field.fieldKey"
            :label="field.fieldLabel"
            :min-width="field.width || 140"
            show-overflow-tooltip
          >
            <template #default="scope">{{ formatValue(scope.row[field.fieldKey]) }}</template>
          </el-table-column>
        </el-table>
        <div v-if="template.footerText" class="readonly-table-footer">{{ template.footerText }}</div>
      </el-tab-pane>
    </el-tabs>
  </template>
</template>

<script setup lang="ts">
import type { ProjectVO } from '@/api/crehn/types';
import { GENERIC_TABLE_DATA_KEY, GenericTableTemplate, normalizeGenericTableData, normalizeGenericTableTemplates } from '@/utils/artGenericTable';

const props = defineProps<{ project?: ProjectVO }>();

const parseObject = (value?: string) => {
  if (!value) return {};
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? (parsed as Record<string, any>) : {};
  } catch {
    return {};
  }
};

const templates = computed(() => {
  const rule = parseObject(props.project?.categoryRuleJson);
  return normalizeGenericTableTemplates(rule.genericTableTemplates)
    .filter((item) => item.enabled !== false)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
});
const tableData = computed(() => {
  const formData = parseObject(props.project?.formDataJson);
  return normalizeGenericTableData(formData[GENERIC_TABLE_DATA_KEY]);
});
const sortedFields = (template: GenericTableTemplate) => [...template.fields].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
const rowsFor = (template: GenericTableTemplate) => tableData.value[template.templateKey] || [];
const formatValue = (value: unknown) => {
  if (Array.isArray(value)) return value.join('、') || '-';
  if (value === null || value === undefined || String(value).trim() === '') return '-';
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
};
</script>

<style scoped>
.readonly-table-tabs {
  margin-bottom: 14px;
}

.readonly-table-footer {
  margin-top: 8px;
  padding: 8px 10px;
  color: var(--el-text-color-secondary);
  white-space: pre-wrap;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}
</style>
