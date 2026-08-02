import type {
  ArtListActionSemantic,
  ArtListColumnFixed,
  ArtListTableAppearanceConfig,
  ArtListTablePageKey,
  ArtReviewListColumnConfig
} from '@/api/crehn/detailDisplay';

type ArtListRuntimePageConfig = {
  actionLabels: Readonly<Record<ArtListActionSemantic, string>>;
  statusLabels: Readonly<Record<string, string>>;
  serialVisible?: boolean;
  serialWidth?: number;
  serialFixed?: 'none' | 'left';
};

export type ArtListRuntimeColumn = Omit<ArtReviewListColumnConfig, 'fixed'> & {
  fixed: 'left' | 'right' | false;
  resizable: boolean;
};

export const artListActionSlots: Record<ArtListTablePageKey, ArtListActionSemantic[][]> = {
  project: [['view', 'edit'], ['submit', 'withdraw'], ['delete']],
  schoolSubmit: [['view'], ['submit', 'withdraw'], ['delete']],
  audit: [['view'], ['return'], ['withdraw']],
  review: [['view', 'edit', 'score']],
  projectView: [['view'], ['delete']],
  reviewAssignment: [['view', 'edit'], ['delete']],
  scoreSummary: [['view']],
  signedSheets: [['view'], ['withdraw']]
};

const labelLength = (value: unknown) => Math.max(1, Array.from(String(value || '').trim()).length);
const fixedWeight: Record<ArtListColumnFixed, number> = { left: 0, none: 1, right: 2 };
const fixedZone = (fixed: ArtReviewListColumnConfig['fixed']): ArtListColumnFixed => fixed || 'none';
const actionButtonHorizontalPadding = 8;
const actionIconLabelGap = 6;
const actionButtonMetricSafety = 2;
const actionButtonGap = 12;
const actionCellHorizontalPadding = 24;
const actionCellMetricSafety = 4;

export const artListColumnFixedValue = (fixed?: ArtReviewListColumnConfig['fixed']) => (fixed === 'left' || fixed === 'right' ? fixed : false);

export const sortArtListColumnsByFixedZone = <T extends Pick<ArtReviewListColumnConfig, 'fixed' | 'key'>>(columns: readonly T[]): T[] =>
  columns
    .map((column, index) => ({ column, index }))
    .sort(
      (left, right) =>
        fixedWeight[fixedZone(left.column.fixed)] - fixedWeight[fixedZone(right.column.fixed)] ||
        Number(left.column.key === 'actions') - Number(right.column.key === 'actions') ||
        left.index - right.index
    )
    .map(({ column }) => column);

export const artListActionColumnMinimumWidth = (
  pageKey: ArtListTablePageKey,
  pageConfig: ArtListRuntimePageConfig,
  appearance: ArtListTableAppearanceConfig
) => {
  const slots = artListActionSlots[pageKey] ?? artListActionSlots.project;
  const slotLabelLengths = slots.map((slot) => Math.max(...slot.map((semantic) => labelLength(pageConfig.actionLabels[semantic]))));
  const uniformLabelLength = Math.max(...slotLabelLengths);
  const labelWidths = appearance.rowActionAppearance.uniformWidth ? slots.map(() => uniformLabelLength) : slotLabelLengths;
  const iconWidth = appearance.rowActionAppearance.iconVisible ? appearance.buttonFontSize + actionIconLabelGap : 0;
  const perButtonExtra = actionButtonHorizontalPadding + iconWidth + actionButtonMetricSafety;
  const buttonsWidth = labelWidths.reduce((sum, length) => sum + length * appearance.buttonFontSize + perButtonExtra, 0);
  return Math.ceil(buttonsWidth + Math.max(0, slots.length - 1) * actionButtonGap + actionCellHorizontalPadding + actionCellMetricSafety);
};

export const artListStatusColumnMinimumWidth = (pageConfig: ArtListRuntimePageConfig, appearance: ArtListTableAppearanceConfig) => {
  const labelWidth = Math.max(...Object.values(pageConfig.statusLabels).map(labelLength)) * appearance.buttonFontSize;
  const iconWidth = appearance.statusAppearance.iconVisible ? 20 : 0;
  return Math.ceil(labelWidth + iconWidth + 36);
};

export const artListSerialColumn = (pageConfig: ArtListRuntimePageConfig): ArtListRuntimeColumn | undefined => {
  if (pageConfig.serialVisible === false) return undefined;
  const width = Math.max(48, Number(pageConfig.serialWidth) || 66);
  return {
    key: 'serial',
    source: 'builtin',
    label: '序号',
    visible: true,
    deleted: false,
    width,
    minWidth: 48,
    fixed: pageConfig.serialFixed === 'left' ? 'left' : false,
    resizable: false
  };
};

export const toArtListRuntimeColumns = (
  pageKey: ArtListTablePageKey,
  columns: readonly ArtReviewListColumnConfig[],
  pageConfig: ArtListRuntimePageConfig,
  appearance: ArtListTableAppearanceConfig
): ArtListRuntimeColumn[] => {
  const actionMinimumWidth = artListActionColumnMinimumWidth(pageKey, pageConfig, appearance);
  const statusMinimumWidth = artListStatusColumnMinimumWidth(pageConfig, appearance);
  return sortArtListColumnsByFixedZone(columns).map((column) => {
    const isActions = column.key === 'actions';
    const isStatus = column.key === 'status' || column.key === 'scoreStatus';
    const minimumWidth = isActions ? actionMinimumWidth : isStatus ? statusMinimumWidth : column.minWidth;
    const width = isActions || isStatus ? Math.max(column.width, column.minWidth, minimumWidth) : column.width;
    const fixed = artListColumnFixedValue(column.fixed);
    return {
      ...column,
      width,
      minWidth: isActions || isStatus ? Math.max(column.minWidth, minimumWidth) : column.minWidth,
      fixed,
      resizable: !isActions && !fixed
    };
  });
};
