import type { ArtListActionSemantic, ArtListStatusSemantic, ArtListTablePageKey, ArtReviewListColumnConfig } from '@/api/crehn/detailDisplay';
import { useArtDetailDisplayConfig } from '@/views/crehn/components/artDetailDisplayConfig';
import { artListSerialColumn, toArtListRuntimeColumns } from './artListTableRuntime';

export {
  artListActionColumnMinimumWidth,
  artListActionSlots,
  artListSerialColumn,
  artListStatusColumnMinimumWidth,
  toArtListRuntimeColumns,
  type ArtListRuntimeColumn
} from './artListTableRuntime';

type CategoryIdGetter = () => string | number | undefined;

const nestedValue = (source: unknown, path: string): unknown =>
  path.split('.').reduce<unknown>((value, key) => (value && typeof value === 'object' ? (value as Record<string, unknown>)[key] : undefined), source);

const isDisplayValueBlank = (value: unknown): boolean =>
  value === undefined || value === null || String(value).trim() === '' || String(value).trim() === '-';

const resolveRecordValue = (source: Record<string, unknown>, key: string): string | undefined => {
  if (!source) return undefined;
  const raw = source[key];
  if (isDisplayValueBlank(raw)) return undefined;
  return String(raw).trim();
};

const resolveRecordValueIgnoreCase = (source: Record<string, unknown>, keys: string[]): string | undefined => {
  for (const key of keys) {
    const direct = resolveRecordValue(source, key);
    if (!isDisplayValueBlank(direct)) return direct;
  }
  const sourceMap = new Map<string, unknown>(
    Object.entries(source).map(([key, value]) => [String(key).toLowerCase(), value])
  );
  for (const key of keys) {
    const found = sourceMap.get(key.toLowerCase());
    if (!isDisplayValueBlank(found)) return String(found).trim();
  }
  return undefined;
};

const resolveProjectGroupName = (row: Record<string, unknown>): string | undefined => {
  if (!row) return undefined;
  for (const value of [row.groupOrNature, row.groupName, row.groupCode]) {
    if (!isDisplayValueBlank(value)) {
      return String(value).trim();
    }
  }

  const formData = parseArtProjectFormData(String(row.formDataJson || ''));
  if (!formData || typeof formData !== 'object') return undefined;
  const formGroupName = resolveRecordValueIgnoreCase(formData, [
    'groupName',
    'group_name',
    'group',
    'groupCode',
    'group_code',
    'displayGroup',
    'display_group',
    'designGroup',
    'design_group',
    'projectNature',
    'project_nature'
  ]);
  return formGroupName;
};

export const parseArtProjectFormData = (json?: string): Record<string, unknown> => {
  if (!json) return {};
  try {
    const value = JSON.parse(json);
    return value && typeof value === 'object' && !Array.isArray(value) ? (value as Record<string, unknown>) : {};
  } catch {
    return {};
  }
};

export const artTableColumnValue = (row: Record<string, unknown>, column: ArtReviewListColumnConfig): unknown => {
  if (column.key === 'groupOrNature' || column.key === 'groupName' || column.key === 'groupCode') {
    const value = resolveProjectGroupName(row);
    return isDisplayValueBlank(value) ? undefined : value;
  }
  if (column.source === 'form') {
    const displayFields = row.displayFields;
    const fieldKey = String(column.fieldKey || '');
    if (displayFields && typeof displayFields === 'object' && fieldKey in (displayFields as Record<string, unknown>)) {
      const value = (displayFields as Record<string, unknown>)[fieldKey];
      if (!isDisplayValueBlank(value)) return value;
      return undefined;
    }
    const value = nestedValue(parseArtProjectFormData(String(row.formDataJson || '')), fieldKey);
    if (!isDisplayValueBlank(value)) return value;
    return undefined;
  }
  if (column.key === 'programForm') return row.programForm ?? nestedValue(parseArtProjectFormData(String(row.formDataJson || '')), 'programForm');
  return row[column.key];
};

export const artTableDisplayText = (value: unknown): string => {
  if (value === undefined || value === null || String(value).trim() === '' || String(value).trim() === '-') return '/';
  if (Array.isArray(value))
    return (
      value
        .map(artTableDisplayText)
        .filter((item) => item !== '/')
        .join('、') || '/'
    );
  if (typeof value === 'object') {
    const record = value as Record<string, unknown>;
    return String(record.label ?? record.name ?? record.value ?? JSON.stringify(value));
  }
  return String(value);
};

export const useArtListTablePage = (pageKey: ArtListTablePageKey, categoryId?: CategoryIdGetter) => {
  const { detailDisplayConfig } = useArtDetailDisplayConfig();
  const pageConfig = computed(() => detailDisplayConfig.value.listTableLayout.pages[pageKey]);
  const columns = computed(() => {
    const key = String(categoryId?.() || '');
    const configured = key ? pageConfig.value.categoryColumns[key] : undefined;
    const visibleColumns = (configured || pageConfig.value.columns).filter((column) => !column.deleted && column.visible);
    return toArtListRuntimeColumns(pageKey, visibleColumns, pageConfig.value, detailDisplayConfig.value.listTableAppearance);
  });
  const serialColumn = computed(() => artListSerialColumn(pageConfig.value));
  const statusText = (semantic: ArtListStatusSemantic, fallback: string) => pageConfig.value.statusLabels[semantic] || fallback;
  const actionText = (semantic: ArtListActionSemantic, fallback: string) => pageConfig.value.actionLabels[semantic] || fallback;

  return { pageConfig, columns, serialColumn, statusText, actionText };
};
