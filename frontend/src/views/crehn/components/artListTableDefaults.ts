import type { ArtReviewListColumnConfig } from '@/api/crehn/detailDisplay';

export const ART_LIST_TABLE_LAYOUT_VERSION = 4;

export const reviewListBuiltinColumnOptions: Array<{
  key: string;
  label: string;
  width: number;
  minWidth: number;
  visible: boolean;
  fixed: ArtReviewListColumnConfig['fixed'];
}> = [
  { key: 'projectNo', label: '项目编号', width: 135, minWidth: 90, visible: true, fixed: 'none' },
  { key: 'projectName', label: '项目名称', width: 180, minWidth: 120, visible: true, fixed: 'none' },
  { key: 'activityName', label: '活动', width: 180, minWidth: 120, visible: false, fixed: 'none' },
  { key: 'categoryName', label: '类别', width: 110, minWidth: 80, visible: true, fixed: 'none' },
  { key: 'groupOrNature', label: '组别', width: 100, minWidth: 70, visible: true, fixed: 'none' },
  { key: 'programForm', label: '类别细分', width: 110, minWidth: 80, visible: false, fixed: 'none' },
  { key: 'scoreMode', label: '评分模式', width: 90, minWidth: 70, visible: false, fixed: 'none' },
  { key: 'scoreResult', label: '评审结果', width: 120, minWidth: 80, visible: true, fixed: 'none' },
  { key: 'scoreSubmittedAt', label: '签字时间', width: 146, minWidth: 100, visible: true, fixed: 'none' },
  { key: 'scoreStatus', label: '状态', width: 84, minWidth: 70, visible: false, fixed: 'none' },
  { key: 'actions', label: '操作', width: 110, minWidth: 110, visible: true, fixed: 'right' }
];

const legacyReviewBuiltinColumnOrder = [
  'projectNo',
  'projectName',
  'activityName',
  'categoryName',
  'groupOrNature',
  'programForm',
  'scoreMode',
  'scoreResult',
  'scoreSubmittedAt',
  'scoreStatus',
  'actions'
];
const legacyReviewDefaultVisibility = new Map<string, boolean>([
  ['projectNo', true],
  ['projectName', true],
  ['activityName', true],
  ['categoryName', true],
  ['groupOrNature', true],
  ['programForm', false],
  ['scoreMode', true],
  ['scoreResult', true],
  ['scoreSubmittedAt', true],
  ['scoreStatus', true],
  ['actions', true]
]);
const reviewDefaultColumnVisibility = new Map(reviewListBuiltinColumnOptions.map((column) => [column.key, column.visible]));

const isLegacyReviewDefaultColumns = (value?: ArtReviewListColumnConfig[]) => {
  if (!Array.isArray(value)) return false;
  const builtinColumns = value.filter((column) => column?.source !== 'form');
  const builtinKeys = builtinColumns.map((column) => String(column?.key || ''));
  return (
    builtinKeys.length === legacyReviewBuiltinColumnOrder.length &&
    builtinKeys.every((key, index) => key === legacyReviewBuiltinColumnOrder[index]) &&
    builtinColumns.every((column) => column.deleted !== true && column.visible === legacyReviewDefaultVisibility.get(column.key))
  );
};

export const migrateLegacyReviewDefaultColumns = (
  columns: ArtReviewListColumnConfig[],
  sourceColumns: ArtReviewListColumnConfig[] | undefined,
  sourceVersion: number
) => {
  if (sourceVersion >= ART_LIST_TABLE_LAYOUT_VERSION || !isLegacyReviewDefaultColumns(sourceColumns)) return columns;
  return columns.map((column) => {
    const visible = reviewDefaultColumnVisibility.get(column.key);
    return visible === undefined ? column : { ...column, visible, deleted: false };
  });
};

export const migrateLegacyActionFixedRight = (columns: ArtReviewListColumnConfig[], sourceVersion: number) =>
  sourceVersion >= ART_LIST_TABLE_LAYOUT_VERSION
    ? columns
    : columns.map((column) => (column.key === 'actions' ? { ...column, fixed: 'right' as const } : column));

export const artReviewGroupDisplayText = (...values: unknown[]): string => {
  const value = values.find((item) => item !== undefined && item !== null && String(item).trim() !== '');
  const text = value === undefined ? '' : String(value).trim();
  return text === '未分组' ? '' : text;
};
