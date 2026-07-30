export const SCHOOL_PROJECT_ALL_CATEGORY_LABEL = '全部类别';

export const normalizeSchoolProjectCategoryAllLabel = (value?: string): string => {
  const label = String(value || '').trim();
  return !label || label === '全部' || label === '汇总' ? SCHOOL_PROJECT_ALL_CATEGORY_LABEL : label;
};

export const schoolProjectColumnWidthSignature = (
  columns: Array<{
    key: string;
    width?: number;
    minWidth?: number;
    fixed?: string | boolean;
    visible?: boolean;
  }>
): string =>
  JSON.stringify(
    columns.map((column) => [column.key, Number(column.width) || 0, Number(column.minWidth) || 0, column.fixed || 'none', column.visible !== false])
  );
