export const GENERIC_TABLE_DATA_KEY = '__genericTables';
export const MAX_GENERIC_TABLE_TEMPLATES = 10;
export const MAX_GENERIC_TABLE_COLUMNS = 30;
export const MAX_GENERIC_TABLE_ROWS = 500;
export const MAX_GENERIC_TABLE_DATA_BYTES = 48 * 1024;

export type GenericTableFieldType = 'input' | 'textarea' | 'number' | 'select' | 'radio' | 'checkbox' | 'date';

export interface GenericTableColumn {
  fieldKey: string;
  fieldLabel: string;
  fieldType: GenericTableFieldType;
  validationType?: string;
  optionsText?: string;
  optionsJson?: string;
  required: boolean;
  sortOrder: number;
  width?: number;
}

export interface GenericTableTemplate {
  templateKey: string;
  templateName: string;
  enabled: boolean;
  tipText?: string;
  minRows?: number;
  maxRows?: number;
  excelTitle?: string;
  fileName?: string;
  sheetName?: string;
  blankRows?: number;
  footerText?: string;
  sortOrder: number;
  fields: GenericTableColumn[];
}

export type GenericTableRow = Record<string, any>;
export type GenericTableData = Record<string, GenericTableRow[]>;

const fieldTypes: GenericTableFieldType[] = ['input', 'textarea', 'number', 'select', 'radio', 'checkbox', 'date'];
const optionFieldTypes = ['select', 'radio', 'checkbox'];
const keyPattern = /^[a-z][a-z0-9_]{1,63}$/;

const numberOrUndefined = (value: unknown, min = 0, max = Number.MAX_SAFE_INTEGER) => {
  if (value === '' || value === null || value === undefined) return undefined;
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return undefined;
  return Math.min(max, Math.max(min, Math.floor(parsed)));
};

const parseOptions = (value: unknown) => {
  if (Array.isArray(value)) return value.map((item) => String(item).trim()).filter(Boolean);
  if (typeof value !== 'string' || !value.trim()) return [];
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed)) return parsed.map((item) => String(item).trim()).filter(Boolean);
  } catch {
    // Legacy or manually entered values may be comma-separated instead of JSON.
  }
  return value
    .split(/[,，、;；\n]+/)
    .map((item) => item.trim())
    .filter(Boolean);
};

const validationTypeOf = (value: unknown) => {
  if (!value) return '';
  if (typeof value === 'object' && !Array.isArray(value)) return String((value as Record<string, any>).type || '');
  if (typeof value !== 'string') return '';
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' ? String(parsed.type || '') : '';
  } catch {
    return value;
  }
};

const uniqueKey = (base: string, used: Set<string>) => {
  let value = base;
  let index = 2;
  while (used.has(value)) value = `${base}_${index++}`;
  used.add(value);
  return value;
};

export const createGenericTableColumn = (index = 0, usedKeys = new Set<string>()): GenericTableColumn => {
  const fieldKey = uniqueKey(`column_${index + 1}`, usedKeys);
  return {
    fieldKey,
    fieldLabel: `字段${index + 1}`,
    fieldType: 'input',
    required: false,
    sortOrder: index + 1,
    width: 160
  };
};

export const createGenericTableTemplate = (index = 0, usedKeys = new Set<string>()): GenericTableTemplate => {
  const templateKey = uniqueKey(`table_${Date.now().toString(36)}_${index + 1}`, usedKeys);
  return {
    templateKey,
    templateName: `表格模板${index + 1}`,
    enabled: true,
    minRows: 0,
    maxRows: 100,
    fileName: `表格模板${index + 1}.xlsx`,
    sheetName: '数据采集表',
    blankRows: 20,
    sortOrder: index + 1,
    fields: [createGenericTableColumn(0)]
  };
};

export const cloneGenericTableTemplate = (source: GenericTableTemplate, index: number, usedKeys: Set<string>): GenericTableTemplate => {
  const clone = JSON.parse(JSON.stringify(source)) as GenericTableTemplate;
  clone.templateKey = uniqueKey(`table_${Date.now().toString(36)}_${index + 1}`, usedKeys);
  clone.templateName = `${source.templateName || '表格模板'}（副本）`;
  clone.fileName = `${clone.templateName}.xlsx`;
  clone.sortOrder = index + 1;
  return clone;
};

export const genericTableFieldOptions = (column: GenericTableColumn) => parseOptions(column.optionsText || column.optionsJson);

export const normalizeGenericTableTemplates = (value: unknown): GenericTableTemplate[] => {
  if (!Array.isArray(value)) return [];
  const usedTemplateKeys = new Set<string>();
  return value.slice(0, MAX_GENERIC_TABLE_TEMPLATES).map((raw: any, templateIndex) => {
    const fallbackTemplate = createGenericTableTemplate(templateIndex, usedTemplateKeys);
    const requestedKey = String(raw?.templateKey || '').trim();
    if (requestedKey && keyPattern.test(requestedKey) && requestedKey !== fallbackTemplate.templateKey && !usedTemplateKeys.has(requestedKey)) {
      usedTemplateKeys.delete(fallbackTemplate.templateKey);
      usedTemplateKeys.add(requestedKey);
      fallbackTemplate.templateKey = requestedKey;
    }
    const usedFieldKeys = new Set<string>();
    const rawFields = Array.isArray(raw?.fields) ? raw.fields : [];
    const fields = rawFields.slice(0, MAX_GENERIC_TABLE_COLUMNS).map((field: any, fieldIndex: number) => {
      const requestedFieldKey = String(field?.fieldKey || '').trim();
      const fallbackField = createGenericTableColumn(fieldIndex, usedFieldKeys);
      if (
        requestedFieldKey &&
        keyPattern.test(requestedFieldKey) &&
        requestedFieldKey !== fallbackField.fieldKey &&
        !usedFieldKeys.has(requestedFieldKey)
      ) {
        usedFieldKeys.delete(fallbackField.fieldKey);
        usedFieldKeys.add(requestedFieldKey);
        fallbackField.fieldKey = requestedFieldKey;
      }
      const fieldType = fieldTypes.includes(field?.fieldType) ? field.fieldType : 'input';
      const options = parseOptions(field?.optionsJson ?? field?.optionsText);
      return {
        ...fallbackField,
        fieldKey: fallbackField.fieldKey,
        fieldLabel: String(field?.fieldLabel || fallbackField.fieldLabel).trim(),
        fieldType,
        validationType: validationTypeOf(field?.validationJson ?? field?.validationType),
        optionsText: options.join('，'),
        optionsJson: optionFieldTypes.includes(fieldType) && options.length ? JSON.stringify(options) : undefined,
        required: !!field?.required,
        sortOrder: numberOrUndefined(field?.sortOrder, 0) ?? fieldIndex + 1,
        width: numberOrUndefined(field?.width, 100, 600) ?? 160
      };
    });
    return {
      ...fallbackTemplate,
      templateName: String(raw?.templateName || fallbackTemplate.templateName).trim(),
      enabled: raw?.enabled !== false,
      tipText: String(raw?.tipText || ''),
      minRows: numberOrUndefined(raw?.minRows, 0, MAX_GENERIC_TABLE_ROWS) ?? 0,
      maxRows: numberOrUndefined(raw?.maxRows, 0, MAX_GENERIC_TABLE_ROWS) ?? 100,
      excelTitle: String(raw?.excelTitle || ''),
      fileName: String(raw?.fileName || fallbackTemplate.fileName),
      sheetName: String(raw?.sheetName || fallbackTemplate.sheetName),
      blankRows: numberOrUndefined(raw?.blankRows, 0, MAX_GENERIC_TABLE_ROWS) ?? 20,
      footerText: String(raw?.footerText || ''),
      sortOrder: numberOrUndefined(raw?.sortOrder, 0) ?? templateIndex + 1,
      fields: fields.length ? fields : [createGenericTableColumn(0)]
    };
  });
};

export const serializeGenericTableTemplates = (templates: GenericTableTemplate[]) =>
  normalizeGenericTableTemplates(templates).map((template, templateIndex) => ({
    ...template,
    sortOrder: template.sortOrder || templateIndex + 1,
    fileName: String(template.fileName || `${template.templateName}.xlsx`).replace(/\.xls$/i, '.xlsx'),
    fields: template.fields.map((field, fieldIndex) => {
      const options = genericTableFieldOptions(field);
      return {
        fieldKey: field.fieldKey.trim(),
        fieldLabel: field.fieldLabel.trim(),
        fieldType: field.fieldType,
        validationJson: field.validationType ? JSON.stringify({ type: field.validationType }) : undefined,
        optionsJson: optionFieldTypes.includes(field.fieldType) && options.length ? JSON.stringify(options) : undefined,
        required: !!field.required,
        sortOrder: field.sortOrder || fieldIndex + 1,
        width: field.width || 160
      };
    })
  }));

export const validateGenericTableTemplates = (templates: GenericTableTemplate[]) => {
  if (templates.length > MAX_GENERIC_TABLE_TEMPLATES) return `通用表格最多保存 ${MAX_GENERIC_TABLE_TEMPLATES} 个模板`;
  const templateKeys = new Set<string>();
  for (const template of templates) {
    if (!template.templateName.trim()) return '请填写表格模板名称';
    if (!keyPattern.test(template.templateKey)) return `表格“${template.templateName}”的模板标识须以小写字母开头，只能包含小写字母、数字和下划线`;
    if (templateKeys.has(template.templateKey)) return `模板标识“${template.templateKey}”重复`;
    templateKeys.add(template.templateKey);
    if (!template.fields.length) return `表格“${template.templateName}”至少需要一个字段`;
    if (template.fields.length > MAX_GENERIC_TABLE_COLUMNS) return `表格“${template.templateName}”最多配置 ${MAX_GENERIC_TABLE_COLUMNS} 个字段`;
    if ((template.maxRows ?? 0) > 0 && (template.minRows ?? 0) > (template.maxRows ?? 0))
      return `表格“${template.templateName}”的最少行数不能大于最多行数`;
    if ((template.blankRows ?? 0) > MAX_GENERIC_TABLE_ROWS)
      return `表格“${template.templateName}”的 Excel 空白行不能超过 ${MAX_GENERIC_TABLE_ROWS} 行`;
    if ((template.sheetName || '').length > 31) return `表格“${template.templateName}”的 Sheet 名称不能超过 31 个字符`;
    const fieldKeys = new Set<string>();
    for (const field of template.fields) {
      if (!field.fieldLabel.trim()) return `表格“${template.templateName}”存在未填写名称的字段`;
      if (!keyPattern.test(field.fieldKey)) return `字段“${field.fieldLabel}”的 field_key 须以小写字母开头，只能包含小写字母、数字和下划线`;
      if (fieldKeys.has(field.fieldKey)) return `表格“${template.templateName}”内 field_key“${field.fieldKey}”重复`;
      fieldKeys.add(field.fieldKey);
      if (optionFieldTypes.includes(field.fieldType) && !genericTableFieldOptions(field).length) return `字段“${field.fieldLabel}”需要配置选项`;
    }
  }
  return '';
};

export const normalizeGenericTableData = (value: unknown): GenericTableData => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return {};
  return Object.fromEntries(
    Object.entries(value as Record<string, unknown>).map(([key, rows]) => [
      key,
      Array.isArray(rows) ? rows.filter((row) => row && typeof row === 'object' && !Array.isArray(row)) : []
    ])
  );
};

const hasValue = (value: unknown) => (Array.isArray(value) ? value.length > 0 : value !== null && value !== undefined && String(value).trim() !== '');

export const validateGenericTableData = (templates: GenericTableTemplate[], data: GenericTableData) => {
  for (const template of templates.filter((item) => item.enabled !== false)) {
    const rows = (data[template.templateKey] || []).filter((row) => template.fields.some((field) => hasValue(row?.[field.fieldKey])));
    if (rows.length > MAX_GENERIC_TABLE_ROWS) return `“${template.templateName}”不能超过 ${MAX_GENERIC_TABLE_ROWS} 行`;
    if (rows.length < (template.minRows || 0)) return `“${template.templateName}”至少填写 ${template.minRows} 行`;
    if ((template.maxRows || 0) > 0 && rows.length > (template.maxRows || 0)) return `“${template.templateName}”最多填写 ${template.maxRows} 行`;
    for (let rowIndex = 0; rowIndex < rows.length; rowIndex += 1) {
      for (const field of template.fields) {
        const value = rows[rowIndex]?.[field.fieldKey];
        if (field.required && !hasValue(value)) return `“${template.templateName}”第 ${rowIndex + 1} 行的“${field.fieldLabel}”不能为空`;
        if (!hasValue(value)) continue;
        if (field.fieldType === 'number' && !Number.isFinite(Number(value)))
          return `“${template.templateName}”第 ${rowIndex + 1} 行的“${field.fieldLabel}”须为数字`;
        if (field.validationType === 'phone' && !/^1\d{10}$/.test(String(value)))
          return `“${template.templateName}”第 ${rowIndex + 1} 行的“${field.fieldLabel}”格式不正确`;
        if (field.validationType === 'email' && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(String(value)))
          return `“${template.templateName}”第 ${rowIndex + 1} 行的“${field.fieldLabel}”格式不正确`;
        if (['idcard', 'id_card'].includes(field.validationType || '') && !/(^\d{15}$)|(^\d{17}[0-9Xx]$)/.test(String(value)))
          return `“${template.templateName}”第 ${rowIndex + 1} 行的“${field.fieldLabel}”格式不正确`;
        if (field.validationType === 'integer' && !Number.isInteger(Number(value)))
          return `“${template.templateName}”第 ${rowIndex + 1} 行的“${field.fieldLabel}”须为整数`;
        if (field.validationType === 'nonnegative' && Number(value) < 0)
          return `“${template.templateName}”第 ${rowIndex + 1} 行的“${field.fieldLabel}”不能小于 0`;
        const options = genericTableFieldOptions(field);
        const values = Array.isArray(value) ? value : [value];
        if (options.length && values.some((item) => !options.includes(String(item))))
          return `“${template.templateName}”第 ${rowIndex + 1} 行的“${field.fieldLabel}”不在可选范围内`;
      }
    }
  }
  if (new TextEncoder().encode(JSON.stringify(data)).length > MAX_GENERIC_TABLE_DATA_BYTES) return '通用表格数据过大，请精简内容或拆分申报';
  return '';
};
