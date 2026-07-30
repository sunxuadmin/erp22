import {
  getArtDetailDisplayConfig,
  getArtListTableConfig,
  updateArtDetailDisplayConfig,
  updateArtListTableConfig,
  type ArtAuditBasicSectionKey,
  type ArtBrowseNavigatorFieldKey,
  type ArtBrowseNavigatorPageConfig,
  type ArtBrowseNavigatorPageKey,
  type ArtAuditDetailTab,
  type ArtAuditFileColumnKey,
  type ArtAuditRecordColumnKey,
  type ArtBrowseNavigatorActivityTitleConfig,
  type ArtAuditProgressConfig,
  type ArtWorkspaceHeaderCardConfig,
  type ArtWorkspaceHeaderButtonAppearanceConfig,
  type ArtWorkspaceHeaderCompactGroupInstanceConfig,
  type ArtWorkspaceHeaderConfig,
  type ArtWorkspaceHeaderItemKey,
  type ArtWorkspaceHeaderRepeatableKey,
  type ArtWorkspaceHeaderSpacerInstanceConfig,
  type ArtWorkspaceHeaderSpacerKey,
  type ArtWorkspaceHeaderStatusAlign,
  type ArtWorkspaceHeaderPageConfig,
  type ArtWorkspaceHeaderPageKey,
  type ArtListActionAppearanceConfig,
  type ArtListTableAppearanceConfig,
  type ArtListTableConfigVO,
  type ArtListTableLayoutConfig,
  type ArtListTablePageConfig,
  type ArtListTablePageKey,
  type ArtListStatusAppearanceConfig,
  type ArtDetailPopupTabConfig,
  type ArtDetailDisplayConfigVO,
  type ArtScoreBasicSectionKey,
  type ArtScoreDetailTab,
  type ArtScoreFileColumnKey,
  type ArtScoreRecordColumnKey,
  type ArtReviewListColumnConfig,
  type ArtReviewWorkbenchDisplayConfig
} from '@/api/crehn/detailDisplay';
import { normalizeWorkspaceNavigationUrl } from './artWorkspaceNavigation';
import {
  ART_LIST_TABLE_LAYOUT_VERSION,
  migrateLegacyActionFixedRight,
  migrateLegacyReviewDefaultColumns,
  reviewListBuiltinColumnOptions
} from './artListTableDefaults';

export { reviewListBuiltinColumnOptions } from './artListTableDefaults';

export const navigatorPageOptions: Array<{ key: ArtBrowseNavigatorPageKey; label: string }> = [
  { key: 'audit', label: '项目审核' },
  { key: 'projectView', label: '上报进度' },
  { key: 'review', label: '专家评分' }
];

export const workspaceHeaderPageOptions: Array<{ key: ArtWorkspaceHeaderPageKey; label: string }> = [
  { key: 'project', label: '类别上报' },
  { key: 'audit', label: '项目审核' },
  { key: 'review', label: '专家评分' },
  { key: 'schoolSubmit', label: '学校统一提交' },
  { key: 'projectView', label: '上报进度' }
];
export const WORKSPACE_HEADER_LAYOUT_VERSION = 8;

export const workspaceHeaderItemOptions: Array<{ key: ArtWorkspaceHeaderItemKey; label: string }> = [
  { key: 'title', label: '大标题' },
  { key: 'navigatorToggle', label: '左侧筛选开关' },
  { key: 'filters', label: '路径筛选摘要' },
  { key: 'reviewScope', label: '评审范围' },
  { key: 'categoryFilter', label: '类别筛选' },
  { key: 'groupFilter', label: '组别筛选' },
  { key: 'schoolFilter', label: '学校筛选' },
  { key: 'progress', label: '进度' },
  { key: 'status', label: '状态 Tab' },
  { key: 'search', label: '搜索' },
  { key: 'actions', label: '页面操作' },
  { key: 'columnWidthReset', label: '恢复默认列宽' },
  { key: 'selectAll', label: '全选' },
  { key: 'unifiedSubmit', label: '统一提交' },
  { key: 'navigationButton', label: '跳转按钮' },
  { key: 'home', label: '首页' },
  { key: 'compactGroup', label: '紧凑组件组' },
  { key: 'fixedSpacer', label: '固定空白' },
  { key: 'autoSpacer', label: '自动空白' }
];

const workspaceHeaderItemKeys = new Set<ArtWorkspaceHeaderItemKey>(workspaceHeaderItemOptions.map((item) => item.key));
const workspaceHeaderSpacerKeys = new Set<ArtWorkspaceHeaderSpacerKey>(['fixedSpacer', 'autoSpacer']);
const workspaceHeaderRepeatableKeys = new Set<ArtWorkspaceHeaderRepeatableKey>(['compactGroup', 'fixedSpacer', 'autoSpacer']);
const workspaceHeaderBusinessItemKeys = new Set<ArtWorkspaceHeaderItemKey>(
  workspaceHeaderItemOptions
    .map((item) => item.key)
    .filter((item): item is ArtWorkspaceHeaderItemKey => !workspaceHeaderRepeatableKeys.has(item as ArtWorkspaceHeaderRepeatableKey))
);
export const workspaceHeaderCollapsibleButtonKeys: ArtWorkspaceHeaderItemKey[] = [
  'navigatorToggle',
  'actions',
  'columnWidthReset',
  'selectAll',
  'unifiedSubmit',
  'navigationButton',
  'home'
];
const workspaceHeaderCollapsibleButtonKeySet = new Set(workspaceHeaderCollapsibleButtonKeys);

type WorkspaceHeaderLayoutLookup = {
  readonly spacerInstances: Readonly<Record<string, Readonly<Pick<ArtWorkspaceHeaderSpacerInstanceConfig, 'type' | 'span' | 'allowStatusBorrow'>>>>;
  readonly compactGroupInstances: Readonly<
    Record<
      string,
      Readonly<Pick<ArtWorkspaceHeaderCompactGroupInstanceConfig, 'widthMode' | 'span' | 'align' | 'gap'>> & {
        readonly items: readonly ArtWorkspaceHeaderItemKey[];
      }
    >
  >;
};

type WorkspaceHeaderGridLookup = WorkspaceHeaderLayoutLookup & {
  readonly itemConfigs: {
    readonly [K in ArtWorkspaceHeaderItemKey]: {
      readonly widthMode: ArtWorkspaceHeaderPageConfig['itemConfigs'][K]['widthMode'];
      readonly span: number;
    };
  };
};

export const isWorkspaceHeaderSpacerKey = (item: string): item is ArtWorkspaceHeaderSpacerKey =>
  workspaceHeaderSpacerKeys.has(item as ArtWorkspaceHeaderSpacerKey);

export const isWorkspaceHeaderRepeatableKey = (item: string): item is ArtWorkspaceHeaderRepeatableKey =>
  workspaceHeaderRepeatableKeys.has(item as ArtWorkspaceHeaderRepeatableKey);

export const workspaceHeaderLayoutItemKey = (itemId: string, page: WorkspaceHeaderLayoutLookup): ArtWorkspaceHeaderItemKey =>
  page.spacerInstances[itemId]?.type || (page.compactGroupInstances[itemId] ? 'compactGroup' : (itemId as ArtWorkspaceHeaderItemKey));

export const createWorkspaceHeaderSpacerInstance = (
  page: Pick<ArtWorkspaceHeaderPageConfig, 'itemConfigs' | 'spacerInstances'>,
  type: ArtWorkspaceHeaderSpacerKey
) => {
  let sequence = 1;
  let instanceId = `${type}-${sequence}`;
  while (page.spacerInstances[instanceId]) {
    sequence += 1;
    instanceId = `${type}-${sequence}`;
  }
  page.spacerInstances[instanceId] = {
    type,
    span: type === 'fixedSpacer' ? numberInRange(page.itemConfigs.fixedSpacer.span, 1, 1, 12) : 1,
    allowStatusBorrow: false
  };
  return instanceId;
};

export const createWorkspaceHeaderCompactGroupInstance = (
  page: Pick<ArtWorkspaceHeaderPageConfig, 'itemConfigs' | 'compactGroupInstances'>,
  items: ArtWorkspaceHeaderItemKey[] = []
) => {
  let sequence = 1;
  let instanceId = `compactGroup-${sequence}`;
  while (page.compactGroupInstances[instanceId]) {
    sequence += 1;
    instanceId = `compactGroup-${sequence}`;
  }
  const defaults = page.itemConfigs.compactGroup;
  page.compactGroupInstances[instanceId] = {
    widthMode: defaults.widthMode,
    span: defaults.span,
    align: defaults.align,
    gap: 10,
    items: items.slice(0, 8),
    collapseMode: 'none',
    pinnedItems: items.includes('unifiedSubmit') ? ['unifiedSubmit'] : items.includes('navigationButton') ? ['navigationButton'] : [],
    collapseText: '更多'
  };
  return instanceId;
};

export const navigatorFieldOptions: Array<{ key: ArtBrowseNavigatorFieldKey; label: string }> = [
  { key: 'activity', label: '活动' },
  { key: 'category', label: '大类 / 子类' },
  { key: 'groupOrNature', label: '组别' },
  { key: 'programForm', label: '类别细分' },
  { key: 'school', label: '学校' }
];

const navigatorFieldKeys = new Set<ArtBrowseNavigatorFieldKey>(navigatorFieldOptions.map((item) => item.key));
const navigatorPageKeys = new Set<ArtBrowseNavigatorPageKey>(navigatorPageOptions.map((item) => item.key));
const defaultNavigatorLabels = (): Record<ArtBrowseNavigatorFieldKey, string> =>
  Object.fromEntries(navigatorFieldOptions.map((item) => [item.key, item.label])) as Record<ArtBrowseNavigatorFieldKey, string>;
const defaultAuditActivityTitle = (): ArtBrowseNavigatorActivityTitleConfig => ({
  displayMode: 'title',
  template: '{activityName}',
  fontSize: 15,
  color: '#29445f',
  fontWeight: 'bold',
  align: 'left',
  switchText: '切换活动',
  allowWrap: true
});
const defaultAuditProgress = (): ArtAuditProgressConfig => ({
  visible: true,
  title: '审核进度',
  template: '已审核 {completed}/{total}',
  height: 8,
  width: 240,
  activeColor: '#2563eb',
  completeColor: '#16a34a',
  trackColor: '#e5e7eb',
  textColor: '#64748b',
  fontSize: 13
});

export const defaultSchoolSubmitProgress = (): ArtAuditProgressConfig => ({
  ...defaultAuditProgress(),
  title: '审核总进度',
  template: '已审核 {completed}/{total}（{percentage}%）'
});

const defaultPrimaryButtonAppearance = (): ArtWorkspaceHeaderButtonAppearanceConfig => ({
  backgroundColor: '#2563eb',
  backgroundOpacity: 100,
  textColor: '#ffffff',
  borderVisible: false,
  borderColor: '#2563eb',
  borderOpacity: 100,
  borderWidth: 1,
  borderRadius: 8,
  hoverBackgroundColor: '#1d4ed8',
  hoverBackgroundOpacity: 100,
  hoverTextColor: '#ffffff',
  hoverBorderColor: '#1d4ed8',
  hoverBorderOpacity: 100
});

const defaultHomeButtonAppearance = (): ArtWorkspaceHeaderButtonAppearanceConfig => ({
  backgroundColor: '#ffffff',
  backgroundOpacity: 100,
  textColor: '#334e68',
  borderVisible: true,
  borderColor: '#d8e2ee',
  borderOpacity: 100,
  borderWidth: 1,
  borderRadius: 8,
  hoverBackgroundColor: '#f4f8ff',
  hoverBackgroundOpacity: 100,
  hoverTextColor: '#2563eb',
  hoverBorderColor: '#93b4f8',
  hoverBorderOpacity: 100
});

const defaultWorkspaceHeaderCard = (
  key: string,
  items: ArtWorkspaceHeaderItemKey[],
  secondRow: ArtWorkspaceHeaderItemKey[] = []
): ArtWorkspaceHeaderCardConfig => ({
  key,
  rows: secondRow.length ? [items, secondRow] : [items],
  backgroundColor: '#ffffff',
  backgroundOpacity: 100,
  borderRadius: 8,
  borderVisible: true
});

const workspaceHeaderItemConfig = (
  align: ArtWorkspaceHeaderStatusAlign,
  widthMode: 'fixed' | 'auto',
  span: number,
  texts: Record<string, string> = {}
) => ({
  align,
  widthMode,
  span,
  texts
});

const defaultWorkspaceHeaderItemConfigs = (
  key: ArtWorkspaceHeaderPageKey,
  statusAlign: ArtWorkspaceHeaderStatusAlign
): ArtWorkspaceHeaderPageConfig['itemConfigs'] => ({
  title: workspaceHeaderItemConfig('left', 'auto', 4),
  navigatorToggle: workspaceHeaderItemConfig('left', 'fixed', 1),
  filters: workspaceHeaderItemConfig(
    'left',
    'auto',
    3,
    key === 'projectView'
      ? {
          categoryPlaceholder: '筛选分类',
          allCategories: '全部类别',
          allUnits: '全部单位',
          progressSuffix: '上报进度'
        }
      : {}
  ),
  reviewScope: workspaceHeaderItemConfig('left', 'auto', 12, {
    scopeLabel: '评审范围',
    allScopeLabel: '全部评审范围',
    selectPlaceholder: '请选择评审范围'
  }),
  categoryFilter: workspaceHeaderItemConfig(
    'left',
    key === 'schoolSubmit' ? 'fixed' : 'auto',
    key === 'schoolSubmit' ? 2 : 3,
    key === 'schoolSubmit' ? { categoryPlaceholder: '全部类别' } : { categoryLabel: '类别', categoryPlaceholder: '全部类别' }
  ),
  groupFilter: workspaceHeaderItemConfig(
    'left',
    'auto',
    3,
    key === 'audit'
      ? { groupLabel: '组别', groupPlaceholder: '所有组别' }
      : key === 'schoolSubmit'
        ? { groupPlaceholder: '全部组别' }
        : { groupLabel: '组别', groupPlaceholder: '全部组别' }
  ),
  schoolFilter: workspaceHeaderItemConfig('left', 'auto', 3, {
    schoolLabel: '学校',
    schoolPlaceholder: '搜索学校'
  }),
  progress: workspaceHeaderItemConfig('right', 'fixed', 3, key === 'review' ? { remainingTemplate: '还有 {pending} 个作品未评分' } : {}),
  status: workspaceHeaderItemConfig(
    statusAlign,
    'fixed',
    3,
    key === 'review'
      ? { all: '全部', none: '未评分', draft: '已评分', submitted: '已签字' }
      : key === 'projectView'
        ? { all: '全部', draft: '草稿', school_submitted: '待审核', audit_passed: '已通过', returned: '已退回' }
        : key === 'schoolSubmit'
          ? { all: '全部', draft: '草稿', school_submitted: '提交待审核', audit_passed: '通过', returned: '退回' }
          : { all: '全部', draft: '草稿', school_submitted: '待审核', audit_passed: '通过', returned: '退回' }
  ),
  search: workspaceHeaderItemConfig('left', 'fixed', 4, { placeholder: '请输入项目名称', search: '搜索', reset: '重置' }),
  actions: workspaceHeaderItemConfig(
    'right',
    'fixed',
    2,
    key === 'project'
      ? { add: '添加' }
      : key === 'audit'
        ? { settings: '设置', restoreColumns: '恢复默认', auditAssignment: '审核权限分配' }
        : key === 'review'
          ? { lockedSignature: '查看已签评分表' }
          : {}
  ),
  columnWidthReset: workspaceHeaderItemConfig('right', 'fixed', 1),
  selectAll: workspaceHeaderItemConfig('left', 'fixed', 1),
  unifiedSubmit: workspaceHeaderItemConfig('right', 'fixed', 2),
  navigationButton: workspaceHeaderItemConfig('right', 'fixed', 2),
  home: workspaceHeaderItemConfig('right', 'fixed', 1),
  compactGroup: workspaceHeaderItemConfig('right', 'fixed', 4),
  fixedSpacer: workspaceHeaderItemConfig('left', 'fixed', 1),
  autoSpacer: workspaceHeaderItemConfig('left', 'auto', 1)
});

export const defaultWorkspaceHeaderPage = (key: ArtWorkspaceHeaderPageKey): ArtWorkspaceHeaderPageConfig => {
  const titleTemplate =
    key === 'project'
      ? '{categoryName}上报列表'
      : key === 'audit'
        ? '{activityName}项目审核'
        : key === 'review'
          ? '{activityName}评审工作台'
          : key === 'projectView'
            ? '{activityName}上报进度'
            : '{activityName}学校统一上报';
  const statusAlign: ArtWorkspaceHeaderStatusAlign = key === 'audit' ? 'left' : 'center';
  const page: ArtWorkspaceHeaderPageConfig = {
    titleTemplate,
    titleFontSize: 20,
    titleFontWeight: 800,
    titleColor: '#102a43',
    sidebarEnabled: true,
    sidebarDefaultExpanded: true,
    statusAlign,
    statusCollapseOnOverflow: true,
    statusItems: Object.keys(defaultWorkspaceHeaderItemConfigs(key, statusAlign).status.texts).map((statusKey, index) => ({
      key: statusKey,
      visible: true,
      order: index + 1
    })),
    componentGap: 10,
    rowMinHeight: 36,
    paddingTop: 10,
    paddingBottom: 10,
    paddingInline: 14,
    marginTop: 0,
    marginBottom: 0,
    navigatorToggleButton: {
      text: '展开左侧筛选',
      alternateText: '收起左侧筛选',
      tooltip: '展开左侧筛选',
      alternateTooltip: '收起左侧筛选',
      align: 'left'
    },
    columnWidthResetButton: {
      text: '恢复默认列宽',
      alternateText: '',
      tooltip: '恢复默认列宽',
      alternateTooltip: '',
      align: 'right'
    },
    selectAllButton: {
      text: '全选',
      alternateText: key === 'review' ? '全取消' : '取消全选',
      tooltip: key === 'review' ? '选择当前类别全部评分项目' : '选择当前页可提交项目',
      alternateTooltip: key === 'review' ? '取消选择当前类别全部评分项目' : '取消选择当前页项目',
      align: 'left',
      iconVisible: true,
      variant: 'default',
      showCount: true,
      appearance: defaultHomeButtonAppearance()
    },
    unifiedSubmitButton: {
      text: key === 'review' ? '统一提交签字' : '统一提交',
      alternateText: '',
      tooltip: key === 'review' ? '检查当前类别评分并进入评分表签字' : '提交当前选中的项目',
      alternateTooltip: '',
      align: 'right',
      iconVisible: true,
      variant: 'default',
      showCount: true,
      appearance: defaultPrimaryButtonAppearance()
    },
    navigationButton: {
      text: '跳转',
      alternateText: '',
      tooltip: '打开目标页面',
      alternateTooltip: '',
      align: 'right',
      iconVisible: true,
      variant: 'default',
      showCount: false,
      appearance: defaultHomeButtonAppearance(),
      targetMode: 'preset',
      presetKey: key === 'schoolSubmit' ? 'schoolSubmit' : 'home',
      customUrl: '',
      openMode: 'current'
    },
    homeButton: {
      text: '首页',
      alternateText: '',
      tooltip: '返回首页',
      alternateTooltip: '',
      align: 'right',
      iconVisible: true,
      variant: 'default',
      showCount: false,
      appearance: defaultHomeButtonAppearance()
    },
    buttonStyle: {
      fontSize: 14,
      fontWeight: 400,
      iconSize: 14,
      height: 32
    },
    itemConfigs: defaultWorkspaceHeaderItemConfigs(key, statusAlign),
    spacerInstances: {},
    compactGroupInstances: {},
    cards:
      key === 'project'
        ? [defaultWorkspaceHeaderCard('primary', ['title', 'actions'])]
        : key === 'projectView'
          ? [defaultWorkspaceHeaderCard('filters', ['navigatorToggle', 'filters', 'status', 'search', 'actions', 'columnWidthReset'])]
          : key === 'schoolSubmit'
            ? [
                defaultWorkspaceHeaderCard('batch', ['selectAll', 'title', 'navigatorToggle', 'search', 'columnWidthReset', 'unifiedSubmit', 'home']),
                defaultWorkspaceHeaderCard('filters', ['categoryFilter', 'groupFilter', 'status'])
              ]
            : key === 'review'
              ? [
                  defaultWorkspaceHeaderCard('primary', [
                    'title',
                    'navigatorToggle',
                    'search',
                    'selectAll',
                    'actions',
                    'unifiedSubmit',
                    'columnWidthReset'
                  ]),
                  defaultWorkspaceHeaderCard('filters', ['reviewScope'], ['categoryFilter', 'groupFilter', 'progress', 'status'])
                ]
              : [
                  defaultWorkspaceHeaderCard('primary', ['title', 'navigatorToggle', 'search', 'actions', 'columnWidthReset']),
                  defaultWorkspaceHeaderCard(
                    'filters',
                    key === 'audit'
                      ? ['status', 'categoryFilter', 'groupFilter', 'schoolFilter', 'progress']
                      : ['categoryFilter', 'groupFilter', 'progress', 'status']
                  )
                ]
  };
  if (key === 'schoolSubmit') {
    const groupId = createWorkspaceHeaderCompactGroupInstance(page, ['columnWidthReset', 'unifiedSubmit', 'home']);
    page.cards[0].rows[0] = ['selectAll', 'title', 'navigatorToggle', 'search', groupId];
  }
  return page;
};

export const defaultSchoolSubmitHomeHeader = (): ArtWorkspaceHeaderPageConfig => {
  const page = defaultWorkspaceHeaderPage('schoolSubmit');
  page.sidebarEnabled = false;
  page.sidebarDefaultExpanded = false;
  page.titleTemplate = '{activityName}统一提交';
  page.homeButton = {
    ...page.homeButton,
    text: '最大化',
    tooltip: '最大化组件'
  };
  page.navigationButton = {
    ...page.navigationButton,
    text: '进入统一提交',
    tooltip: '进入统一提交页面',
    presetKey: 'schoolSubmit'
  };
  page.columnWidthResetButton = {
    ...page.columnWidthResetButton,
    text: ''
  };
  page.spacerInstances = {
    'autoSpacer-1': {
      type: 'autoSpacer',
      span: 1,
      allowStatusBorrow: false
    }
  };
  page.compactGroupInstances = {};
  const groupId = createWorkspaceHeaderCompactGroupInstance(page, ['columnWidthReset', 'navigationButton', 'home']);
  page.cards = [
    {
      key: 'home-toolbar',
      rows: [['categoryFilter', 'status', 'autoSpacer-1', groupId], []],
      backgroundColor: '#ffffff',
      backgroundOpacity: 0,
      borderRadius: 0,
      borderVisible: false
    }
  ];
  return page;
};

export const defaultSchoolSubmitMaximizedHeader = (): ArtWorkspaceHeaderPageConfig => {
  const page = defaultWorkspaceHeaderPage('schoolSubmit');
  page.homeButton = {
    ...page.homeButton,
    text: '退出最大化',
    tooltip: '退出最大化'
  };
  return page;
};

export const workspaceHeaderItemText = (
  config: {
    readonly itemConfigs: {
      readonly [K in ArtWorkspaceHeaderItemKey]: { readonly texts: Readonly<Record<string, string>> };
    };
  },
  itemKey: ArtWorkspaceHeaderItemKey,
  textKey: string,
  fallback = ''
) => config.itemConfigs[itemKey]?.texts?.[textKey] ?? fallback;

export const resolveWorkspaceHeaderGridSpans = (
  row: readonly string[],
  page: WorkspaceHeaderGridLookup,
  visible: (itemId: string, itemKey: ArtWorkspaceHeaderItemKey) => boolean = () => true
) => {
  const items = row.map((id) => ({ id, key: workspaceHeaderLayoutItemKey(id, page) })).filter((item) => visible(item.id, item.key));
  const widthMode = (item: (typeof items)[number]) =>
    item.key === 'autoSpacer'
      ? 'auto'
      : item.key === 'fixedSpacer'
        ? 'fixed'
        : item.key === 'compactGroup'
          ? page.compactGroupInstances[item.id]?.widthMode || 'fixed'
          : page.itemConfigs[item.key].widthMode;
  const configuredSpan = (item: (typeof items)[number]) =>
    item.key === 'fixedSpacer'
      ? page.spacerInstances[item.id]?.span || 1
      : item.key === 'compactGroup'
        ? page.compactGroupInstances[item.id]?.span || 4
        : page.itemConfigs[item.key].span;
  const spans = Object.fromEntries(items.map((item) => [item.id, widthMode(item) === 'auto' ? 1 : configuredSpan(item)])) as Record<string, number>;
  const autoItems = items.filter((item) => widthMode(item) === 'auto');
  const fixedItems = items.filter((item) => widthMode(item) !== 'auto');
  let fixedTotal = fixedItems.reduce((total, item) => total + spans[item.id], 0);
  const minimumTotal = fixedTotal + autoItems.length;
  if (minimumTotal > 12) {
    let excess = minimumTotal - 12;
    while (excess > 0) {
      const reducible = fixedItems.filter((item) => spans[item.id] > 1).sort((left, right) => spans[right.id] - spans[left.id]);
      if (!reducible.length) break;
      reducible.forEach((item) => {
        if (excess <= 0 || spans[item.id] <= 1) return;
        spans[item.id] -= 1;
        excess -= 1;
      });
    }
    fixedTotal = fixedItems.reduce((total, item) => total + spans[item.id], 0);
  }
  if (autoItems.length) {
    const remaining = Math.max(autoItems.length, 12 - fixedTotal);
    const base = Math.floor(remaining / autoItems.length);
    let remainder = remaining % autoItems.length;
    autoItems.forEach((item) => {
      spans[item.id] = base + (remainder > 0 ? 1 : 0);
      remainder -= remainder > 0 ? 1 : 0;
    });
  }
  return spans;
};

export const resolveWorkspaceHeaderRuntimeGridSpans = (
  row: readonly string[],
  page: WorkspaceHeaderGridLookup,
  visible: (itemId: string, itemKey: ArtWorkspaceHeaderItemKey) => boolean = () => true
) => {
  const spans = resolveWorkspaceHeaderGridSpans(row, page, visible);
  row.forEach((itemId, index) => {
    const spacer = page.spacerInstances[itemId];
    if (index === 0 || spacer?.type !== 'autoSpacer' || spacer.allowStatusBorrow !== true) return;
    const previousId = row[index - 1];
    if (workspaceHeaderLayoutItemKey(previousId, page) !== 'status') return;
    if (!visible(previousId, 'status') || !visible(itemId, 'autoSpacer')) return;
    const spacerSpan = spans[itemId] || 0;
    if (!spacerSpan || !spans[previousId]) return;
    spans[previousId] += spacerSpan;
    spans[itemId] = 0;
  });
  return spans;
};

const defaultWorkspaceHeader = (): ArtWorkspaceHeaderConfig => ({
  layoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
  managedActivityId: undefined,
  pages: {
    project: defaultWorkspaceHeaderPage('project'),
    audit: defaultWorkspaceHeaderPage('audit'),
    review: defaultWorkspaceHeaderPage('review'),
    schoolSubmit: defaultWorkspaceHeaderPage('schoolSubmit'),
    projectView: defaultWorkspaceHeaderPage('projectView')
  }
});

export const defaultArtListStatusColors = (): ArtListStatusAppearanceConfig['colors'] => ({
  draft: '#6B778C',
  pending: '#B88736',
  approved: '#4D8E62',
  returned: '#BC626B',
  unscored: '#B88736',
  scored: '#4D8E62',
  scoreDraft: '#6B778C',
  locked: '#9A7844'
});

export const defaultArtListActionColors = (): ArtListActionAppearanceConfig['colors'] => ({
  view: '#4F73B8',
  score: '#4F73B8',
  edit: '#4F73B8',
  return: '#BC626B',
  withdraw: '#B88736',
  submit: '#4D8E62',
  delete: '#BC626B'
});

const defaultStatusAppearance = (): ArtListStatusAppearanceConfig => ({
  borderVisible: true,
  iconVisible: false,
  uniformWidth: true,
  colors: defaultArtListStatusColors()
});

const defaultRowActionAppearance = (): ArtListActionAppearanceConfig => ({
  borderVisible: false,
  iconVisible: true,
  uniformWidth: true,
  disabledOpacity: 36,
  colors: defaultArtListActionColors()
});

export const defaultListTableAppearance = (): ArtListTableAppearanceConfig => ({
  globalColorOverride: true,
  headerBackground: '#eef4ff',
  headerTextColor: '#1f3b64',
  textColor: '#334155',
  hoverBackground: '#f8fbff',
  hoverBackgroundOpacity: 100,
  borderColor: '#e8eef6',
  horizontalBorderVisible: true,
  horizontalBorderColor: '#e8eef6',
  horizontalBorderOpacity: 100,
  verticalBorderVisible: false,
  verticalBorderColor: '#e8eef6',
  verticalBorderOpacity: 0,
  outerBorderVisible: true,
  outerBorderColor: '#e8eef6',
  outerBorderOpacity: 60,
  headerFontSize: 14,
  bodyFontSize: 14,
  buttonFontSize: 14,
  rowHeight: 44,
  borderRadius: 8,
  borderMode: 'horizontal',
  wrapMode: 'wrap',
  headerWrap: false,
  striped: false,
  actionStyle: 'link',
  statusAppearance: defaultStatusAppearance(),
  rowActionAppearance: defaultRowActionAppearance()
});

export const artListTablePageOptions: Array<{ key: ArtListTablePageKey; label: string }> = [
  { key: 'project', label: '类别上报' },
  { key: 'schoolSubmit', label: '学校统一提交' },
  { key: 'audit', label: '项目审核' },
  { key: 'review', label: '专家评分' },
  { key: 'projectView', label: '上报进度' }
];

const tableColumn = (key: string, label: string, width: number, minWidth: number, visible = true): ArtReviewListColumnConfig => ({
  key,
  source: 'builtin',
  label,
  visible,
  deleted: false,
  width,
  minWidth,
  fixed: key === 'selection' ? 'left' : key === 'actions' ? 'right' : 'none'
});

const defaultStatusLabels = () => ({
  draft: '草稿',
  pending: '待审核',
  approved: '已通过',
  returned: '已退回',
  unscored: '未评分',
  scored: '已评分',
  scoreDraft: '评分草稿',
  locked: '他人评分'
});

const defaultActionLabels = () => ({
  view: '查看',
  score: '评分',
  edit: '编辑',
  return: '退回',
  withdraw: '撤回',
  submit: '提交',
  delete: '删除'
});

const defaultTablePage = (emptyText: string, columns: ArtReviewListColumnConfig[]): ArtListTablePageConfig => ({
  emptyText,
  selectionFixed: 'none',
  serialVisible: true,
  serialWidth: 66,
  serialFixed: 'none',
  columns,
  categoryColumns: {},
  statusLabels: defaultStatusLabels(),
  actionLabels: defaultActionLabels()
});
const tablePageLabels = (
  page: ArtListTablePageConfig,
  statusLabels: Partial<ArtListTablePageConfig['statusLabels']> = {},
  actionLabels: Partial<ArtListTablePageConfig['actionLabels']> = {}
): ArtListTablePageConfig => ({
  ...page,
  statusLabels: { ...page.statusLabels, ...statusLabels },
  actionLabels: { ...page.actionLabels, ...actionLabels }
});

export const defaultArtListTableLayout = (): ArtListTableLayoutConfig => ({
  version: ART_LIST_TABLE_LAYOUT_VERSION,
  pages: {
    project: tablePageLabels(
      defaultTablePage('暂无上报项目', [
        tableColumn('projectNo', '项目编号', 150, 90),
        tableColumn('projectName', '项目名称', 220, 120),
        tableColumn('groupName', '组别', 120, 80, false),
        tableColumn('categoryName', '小类别', 150, 90, false),
        tableColumn('status', '状态', 110, 76),
        tableColumn('submittedAt', '提交时间', 170, 110),
        tableColumn('currentAuditOpinion', '意见', 180, 110),
        tableColumn('actions', '操作', 220, 220)
      ]),
      { pending: '已提交', approved: '审核通过', returned: '退回修改' }
    ),
    schoolSubmit: tablePageLabels(
      defaultTablePage('暂无报送项目', [
        tableColumn('selection', '选择', 58, 52),
        tableColumn('categoryName', '小类别', 150, 90),
        tableColumn('groupName', '组别', 120, 80, false),
        tableColumn('projectName', '项目名称', 360, 160),
        tableColumn('status', '状态', 130, 76),
        tableColumn('updateTime', '修改时间', 190, 110),
        tableColumn('submittedAt', '提交时间', 190, 110),
        tableColumn('currentAuditOpinion', '意见', 260, 120, false),
        tableColumn('actions', '操作', 220, 220)
      ]),
      { pending: '提交待审核', approved: '通过', returned: '退回' }
    ),
    audit: defaultTablePage('暂无待审核项目', [
      tableColumn('projectNo', '作品编号', 180, 90),
      tableColumn('projectName', '项目名称', 190, 120),
      tableColumn('categoryName', '小类别', 120, 80),
      tableColumn('groupName', '组别', 110, 80, false),
      tableColumn('schoolName', '单位', 140, 90),
      tableColumn('submittedAt', '提交时间', 146, 100),
      tableColumn('currentAuditOpinion', '审核意见', 160, 100, false),
      tableColumn('status', '状态', 84, 70),
      tableColumn('actions', '操作', 200, 200)
    ]),
    review: {
      ...tablePageLabels(
        defaultTablePage('暂无评分任务', defaultReviewListColumns()),
        { locked: '已被评分', scored: '已签字', scoreDraft: '已评分' },
        { view: '查看评分', edit: '修改评分', score: '开始评分' }
      ),
      selectionFixed: 'left'
    },
    projectView: defaultTablePage('暂无上报项目', [
      tableColumn('projectNo', '项目编号', 150, 90),
      tableColumn('projectName', '项目名称', 190, 120),
      tableColumn('categoryName', '小类别', 140, 80),
      tableColumn('groupName', '组别', 110, 80, false),
      tableColumn('schoolName', '单位', 160, 100),
      tableColumn('submittedAt', '提交时间', 170, 110),
      tableColumn('status', '状态', 110, 76),
      tableColumn('actions', '操作', 130, 130)
    ])
  }
});
const defaultNavigatorPage = (key: ArtBrowseNavigatorPageKey): ArtBrowseNavigatorPageConfig => {
  const labels = defaultNavigatorLabels();
  if (key === 'audit') {
    labels.category = '类别';
    labels.groupOrNature = '组别';
    labels.programForm = '子类';
  }
  return {
    layout: 'sidebar',
    fields:
      key === 'review'
        ? ['activity', 'category', 'groupOrNature', 'programForm']
        : key === 'audit'
          ? ['category', 'groupOrNature', 'school']
          : ['activity', 'category', 'groupOrNature', 'programForm', 'school'],
    labels,
    activityTitle: key === 'audit' ? defaultAuditActivityTitle() : undefined
  };
};

export const auditDefaultTabOptions: Array<{ key: ArtAuditDetailTab; label: string }> = [
  { key: 'preview', label: '作品预览' },
  { key: 'form', label: '基础信息' },
  { key: 'members', label: '成员信息' },
  { key: 'files', label: '作品文件' },
  { key: 'records', label: '审核记录' }
];

export const scoreDefaultTabOptions: Array<{ key: ArtScoreDetailTab; label: string }> = [
  { key: 'preview', label: '作品预览' },
  { key: 'form', label: '基础信息' },
  { key: 'members', label: '成员信息' },
  { key: 'files', label: '作品文件' },
  { key: 'scores', label: '评分记录' }
];

export const auditBasicSectionOptions: Array<{ key: ArtAuditBasicSectionKey; label: string }> = [
  { key: 'formFields', label: '项目表单信息' },
  { key: 'genericTables', label: '填报表格' },
  { key: 'downloadWorkbook', label: '下载表格按钮' }
];

export const auditFileColumnOptions: Array<{ key: ArtAuditFileColumnKey; label: string }> = [
  { key: 'fileType', label: '材料类型' },
  { key: 'fileName', label: '文件名' },
  { key: 'fileSize', label: '大小' },
  { key: 'technicalCheck', label: '技术校验' },
  { key: 'mediaInfo', label: '媒体信息' },
  { key: 'actions', label: '操作' }
];

export const auditRecordColumnOptions: Array<{ key: ArtAuditRecordColumnKey; label: string }> = [
  { key: 'operator', label: '操作人' },
  { key: 'result', label: '结果' },
  { key: 'transition', label: '状态流转' },
  { key: 'opinion', label: '意见' },
  { key: 'time', label: '时间' }
];

export const scoreBasicSectionOptions: Array<{ key: ArtScoreBasicSectionKey; label: string }> = [
  { key: 'submittedAt', label: '提交时间' },
  { key: 'scoreVisibility', label: '评分可见说明' },
  { key: 'formFields', label: '项目表单信息' },
  { key: 'genericTables', label: '填报表格' }
];

export const scoreFileColumnOptions: Array<{ key: ArtScoreFileColumnKey; label: string }> = [
  { key: 'fileType', label: '材料类型' },
  { key: 'fileName', label: '文件名' },
  { key: 'fileSize', label: '大小' },
  { key: 'previewStatus', label: '预览状态' },
  { key: 'actions', label: '操作' }
];

export const scoreRecordColumnOptions: Array<{ key: ArtScoreRecordColumnKey; label: string }> = [
  { key: 'reviewer', label: '评委' },
  { key: 'result', label: '得分/等级' },
  { key: 'comment', label: '意见' }
];

const defaultPopupTabs = <T extends string>(options: Array<{ key: T; label: string }>): ArtDetailPopupTabConfig<T> => ({
  order: options.map((item) => item.key),
  visibleTabs: options.map((item) => item.key),
  labels: Object.fromEntries(options.map((item) => [item.key, item.label])) as Record<T, string>,
  defaultTab: options[0].key
});

export const auditSummaryFieldOptions = [
  { key: 'activity', label: '活动' },
  { key: 'category', label: '类别' },
  { key: 'school', label: '单位' },
  { key: 'submittedAt', label: '提交时间' },
  { key: 'auditOpinion', label: '审核意见' }
];

export const detailFileFieldOptions = [
  { key: 'fileType', label: '材料类型' },
  { key: 'fileExt', label: '扩展名' },
  { key: 'mimeType', label: 'MIME 类型' },
  { key: 'fileSize', label: '文件大小' },
  { key: 'mediaType', label: '媒体类型' },
  { key: 'format', label: '媒体格式' },
  { key: 'resolution', label: '分辨率' },
  { key: 'duration', label: '时长' },
  { key: 'fps', label: '帧率' },
  { key: 'bitrate', label: '码率' },
  { key: 'dpi', label: 'DPI' },
  { key: 'checkStatus', label: '技术校验' },
  { key: 'technicalRequirements', label: '技术校验要求' }
];

const defaultReviewListColumns = (): ArtReviewListColumnConfig[] =>
  reviewListBuiltinColumnOptions.map((item) => ({ ...item, source: 'builtin' as const, deleted: false }));

const defaultReviewWorkbench = (): ArtReviewWorkbenchDisplayConfig => ({
  activityTitle: {
    visible: true,
    template: '{activityName}',
    fontSize: 15,
    color: '#29445f',
    fontWeight: 'bold',
    align: 'left',
    switchText: '切换活动',
    allowWrap: true
  },
  scopeNavigator: {
    visible: true,
    displayMode: 'buttons',
    showCount: true,
    progressVisible: false,
    progressPlacement: 'inside'
  },
  progress: {
    visible: true,
    template: '本类别 {completed}/{total} 个作品已评分',
    showPercentage: false,
    showRemaining: true,
    height: 8,
    width: 260,
    activeColor: '#2563eb',
    successColor: '#67c23a',
    signedColor: '#16a34a',
    trackColor: '#e5e7eb',
    textColor: '#64748b',
    fontSize: 13
  },
  signature: {
    hintVisible: true,
    hintText: '请选择具体类别后提交签字',
    hintFontSize: 13,
    hintFontWeight: 'medium',
    hintColor: '#8a5b16',
    hintBackgroundColor: '#fff8e8',
    hintBorderColor: '#f3d19e',
    buttonText: '统一提交签字',
    buttonFontSize: 14,
    buttonFontWeight: 'medium',
    buttonTextColor: '#ffffff'
  },
  columns: defaultReviewListColumns(),
  categoryColumns: {}
});

const defaultConfig = (): ArtDetailDisplayConfigVO => ({
  tabs: {
    basicInfoLabel: '基础信息',
    projectFilesLabel: '作品文件',
    auditDefaultTab: 'preview',
    scoreDefaultTab: 'preview',
    audit: defaultPopupTabs(auditDefaultTabOptions),
    score: defaultPopupTabs(scoreDefaultTabOptions)
  },
  audit: {
    title: '审核处理',
    summaryFields: auditSummaryFieldOptions.map((item) => item.key),
    summaryLabels: Object.fromEntries(auditSummaryFieldOptions.map((item) => [item.key, item.label])),
    actionsVisible: true,
    content: {
      basicSections: auditBasicSectionOptions.map((item) => item.key),
      fileColumns: auditFileColumnOptions.map((item) => item.key),
      recordColumns: auditRecordColumnOptions.map((item) => item.key)
    }
  },
  files: {
    fields: ['fileType', 'fileSize', 'format', 'resolution', 'duration', 'fps', 'bitrate', 'dpi', 'checkStatus', 'technicalRequirements'],
    labels: Object.fromEntries(detailFileFieldOptions.map((item) => [item.key, item.label]))
  },
  score: {
    title: '评审评分',
    scoreLabel: '分数',
    gradeLabel: '等级',
    commentLabel: '意见',
    statusVisible: true,
    commentVisible: true,
    saveDraftVisible: true,
    fileListVisible: true,
    content: {
      basicSections: scoreBasicSectionOptions.map((item) => item.key),
      fileColumns: scoreFileColumnOptions.map((item) => item.key),
      recordColumns: scoreRecordColumnOptions.map((item) => item.key)
    }
  },
  navigator: {
    titleFontSize: 15,
    itemFontSize: 14,
    countFontSize: 13,
    rowHeight: 38,
    itemGap: 2,
    panelPadding: 12,
    backgroundColor: '#ffffff',
    textColor: '#29445f',
    activeColor: '#2563eb',
    hoverBackground: '#2563eb',
    hoverBackgroundOpacity: 7,
    selectedBackground: '#2563eb',
    selectedBackgroundOpacity: 10,
    selectedTextColor: '#2563eb',
    borderColor: '#e8edf5',
    backgroundOpacity: 100,
    dividerVisible: true,
    dividerColor: '#e8edf5',
    dividerOpacity: 72,
    dividerWidth: 1,
    borderRadius: 10,
    sidebarWidth: 340,
    sidebarMaxHeight: 720,
    topHeight: 76,
    borderVisible: true,
    shadowVisible: true,
    pages: {
      audit: defaultNavigatorPage('audit'),
      projectView: defaultNavigatorPage('projectView'),
      review: defaultNavigatorPage('review')
    }
  },
  auditProgress: defaultAuditProgress(),
  workspaceHeader: defaultWorkspaceHeader(),
  listTableAppearance: defaultListTableAppearance(),
  listTableLayout: defaultArtListTableLayout(),
  reviewWorkbench: defaultReviewWorkbench()
});

const allowedAuditKeys = new Set(auditSummaryFieldOptions.map((item) => item.key));
const allowedFileKeys = new Set(detailFileFieldOptions.map((item) => item.key));
const allowedAuditBasicSections = new Set(auditBasicSectionOptions.map((item) => item.key));
const allowedAuditFileColumns = new Set(auditFileColumnOptions.map((item) => item.key));
const allowedAuditRecordColumns = new Set(auditRecordColumnOptions.map((item) => item.key));
const allowedScoreBasicSections = new Set(scoreBasicSectionOptions.map((item) => item.key));
const allowedScoreFileColumns = new Set(scoreFileColumnOptions.map((item) => item.key));
const allowedScoreRecordColumns = new Set(scoreRecordColumnOptions.map((item) => item.key));
const state = ref<ArtDetailDisplayConfigVO>(defaultConfig());
let loaded = false;
let pendingLoad: Promise<ArtDetailDisplayConfigVO> | undefined;

const text = (value: unknown, fallback: string) => String(value || '').trim() || fallback;
const bool = (value: unknown, fallback: boolean) => (typeof value === 'boolean' ? value : fallback);
const uniqueAllowed = (value: unknown, allowed: Set<string>, fallback: string[]) => {
  if (!Array.isArray(value)) return [...fallback];
  return [...new Set(value.map(String).filter((item) => allowed.has(item)))];
};
const typedAllowed = <T extends string>(value: unknown, allowed: Set<T>, fallback: T[]) => uniqueAllowed(value, allowed, fallback) as T[];
const allowedTab = <T extends string>(value: unknown, allowed: Set<T>, fallback: T) => (allowed.has(value as T) ? (value as T) : fallback);
const normalizePopupTabs = <T extends string>(
  value: Partial<ArtDetailPopupTabConfig<T>> | undefined,
  options: Array<{ key: T; label: string }>,
  fallback: ArtDetailPopupTabConfig<T>,
  legacyDefault?: T
): ArtDetailPopupTabConfig<T> => {
  const allowed = new Set(options.map((item) => item.key));
  const configuredOrder = typedAllowed(value?.order, allowed, fallback.order);
  const order = [...configuredOrder, ...fallback.order.filter((key) => !configuredOrder.includes(key))];
  let visibleTabs = typedAllowed(value?.visibleTabs, allowed, fallback.visibleTabs).filter((key) => order.includes(key));
  if (!visibleTabs.length) visibleTabs = [...fallback.visibleTabs];
  if (!visibleTabs.includes('preview' as T) && !visibleTabs.includes('files' as T)) {
    visibleTabs.push(order.find((key) => key === ('preview' as T) || key === ('files' as T)) || fallback.visibleTabs[0]);
  }
  const requestedDefault = allowedTab(value?.defaultTab ?? legacyDefault, allowed, fallback.defaultTab);
  return {
    order,
    visibleTabs: order.filter((key) => visibleTabs.includes(key)),
    labels: Object.fromEntries(options.map((item) => [item.key, text(value?.labels?.[item.key], fallback.labels[item.key])])) as Record<T, string>,
    defaultTab: visibleTabs.includes(requestedDefault) ? requestedDefault : order.find((key) => visibleTabs.includes(key)) || fallback.defaultTab
  };
};
const normalizeFileFields = (value: unknown, fallback: string[]) => {
  if (!Array.isArray(value)) return [...fallback];
  const migrated = value.map((item) => (String(item) === 'checkMessage' ? 'technicalRequirements' : String(item)));
  return uniqueAllowed(migrated, allowedFileKeys, fallback);
};
const color = (value: unknown, fallback: string) => {
  const text = String(value || '').trim();
  return /^#[0-9a-fA-F]{6}$/.test(text) ? text : fallback;
};
const numberInRange = (value: unknown, fallback: number, min: number, max: number) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? Math.min(max, Math.max(min, Math.round(parsed))) : fallback;
};
const safeTemplate = (value: unknown, fallback: string, maxLength = 120) => {
  const resolved = text(value, fallback).replace(/[<>]/g, '').slice(0, maxLength);
  return resolved || fallback;
};
const normalizeReviewListColumns = (value: unknown, fallback: ArtReviewListColumnConfig[]) => {
  const input = Array.isArray(value) ? value : fallback;
  const builtinMap = new Map(reviewListBuiltinColumnOptions.map((item) => [item.key, item]));
  const seen = new Set<string>();
  const columns: ArtReviewListColumnConfig[] = [];
  input.forEach((raw) => {
    if (!raw || typeof raw !== 'object') return;
    const item = raw as Partial<ArtReviewListColumnConfig>;
    const source = item.source === 'form' ? 'form' : 'builtin';
    const fieldKey = String(item.fieldKey || '').trim();
    const rawKey = source === 'form' ? `field:${fieldKey}` : String(item.key || '').trim();
    const key = source === 'form' ? rawKey : rawKey === 'status' ? 'scoreStatus' : rawKey;
    const builtin = builtinMap.get(key);
    if (!key || seen.has(key) || (source === 'builtin' && !builtin) || (source === 'form' && !/^[A-Za-z0-9_.-]{1,80}$/.test(fieldKey))) return;
    seen.add(key);
    const base = builtin || { key, label: fieldKey, width: 140, minWidth: 90, visible: true, fixed: 'none' as const };
    const visibilityLocked = key === 'groupOrNature';
    const deletionLocked = key === 'actions' || visibilityLocked;
    const fixed =
      key === 'actions'
        ? item.fixed === 'none'
          ? 'none'
          : 'right'
        : item.fixed === 'left' || item.fixed === 'right'
          ? item.fixed
          : base.fixed || 'none';
    columns.push({
      key,
      source,
      fieldKey: source === 'form' ? fieldKey : undefined,
      label: text(item.label, base.label).slice(0, 30),
      visible: visibilityLocked ? true : bool(item.visible, base.visible) && !bool(item.deleted, false),
      deleted: deletionLocked ? false : bool(item.deleted, false),
      width: numberInRange(item.width, base.width, key === 'actions' ? 96 : 60, 600),
      minWidth: numberInRange(item.minWidth, base.minWidth, key === 'actions' ? 96 : 48, 300),
      fixed
    });
  });
  reviewListBuiltinColumnOptions.forEach((item) => {
    if (!seen.has(item.key)) columns.push({ ...item, source: 'builtin', deleted: false });
  });
  return columns
    .map((column, index) => ({ column, index }))
    .sort(
      (left, right) =>
        ({ left: 0, none: 1, right: 2 })[left.column.fixed || 'none'] - { left: 0, none: 1, right: 2 }[right.column.fixed || 'none'] ||
        Number(left.column.key === 'actions') - Number(right.column.key === 'actions') ||
        left.index - right.index
    )
    .map(({ column }) => column);
};
const normalizeReviewWorkbench = (value: Partial<ArtReviewWorkbenchDisplayConfig> | undefined): ArtReviewWorkbenchDisplayConfig => {
  const fallback = defaultReviewWorkbench();
  const title = value?.activityTitle;
  const scopeNavigator = value?.scopeNavigator;
  const progress = value?.progress;
  const signature = value?.signature;
  const categoryColumns = Object.fromEntries(
    Object.entries(value?.categoryColumns || {})
      .filter(([key]) => /^\d+$/.test(key))
      .slice(0, 200)
      .map(([key, columns]) => [key, normalizeReviewListColumns(columns, fallback.columns)])
  );
  return {
    activityTitle: {
      visible: bool(title?.visible, fallback.activityTitle.visible),
      template: safeTemplate(title?.template, fallback.activityTitle.template),
      fontSize: numberInRange(title?.fontSize, fallback.activityTitle.fontSize, 12, 28),
      color: color(title?.color, fallback.activityTitle.color),
      fontWeight: title?.fontWeight === 'normal' || title?.fontWeight === 'medium' ? title.fontWeight : 'bold',
      align: title?.align === 'center' ? 'center' : 'left',
      switchText: text(title?.switchText, fallback.activityTitle.switchText).slice(0, 20),
      allowWrap: bool(title?.allowWrap, fallback.activityTitle.allowWrap)
    },
    scopeNavigator: {
      visible: bool(scopeNavigator?.visible, fallback.scopeNavigator.visible),
      displayMode: scopeNavigator?.displayMode === 'select' ? 'select' : 'buttons',
      showCount: bool(scopeNavigator?.showCount, fallback.scopeNavigator.showCount),
      progressVisible: bool(scopeNavigator?.progressVisible, fallback.scopeNavigator.progressVisible),
      progressPlacement: scopeNavigator?.progressPlacement === 'below' ? 'below' : 'inside'
    },
    progress: {
      visible: bool(progress?.visible, fallback.progress.visible),
      template: safeTemplate(progress?.template, fallback.progress.template),
      showPercentage: bool(progress?.showPercentage, fallback.progress.showPercentage),
      showRemaining: bool(progress?.showRemaining, fallback.progress.showRemaining),
      height: numberInRange(progress?.height, fallback.progress.height, 4, 20),
      width: numberInRange(progress?.width, fallback.progress.width, 120, 600),
      activeColor: color(progress?.activeColor, fallback.progress.activeColor),
      successColor: color(progress?.successColor, fallback.progress.successColor),
      signedColor: color(progress?.signedColor, fallback.progress.signedColor),
      trackColor: color(progress?.trackColor, fallback.progress.trackColor),
      textColor: color(progress?.textColor, fallback.progress.textColor),
      fontSize: numberInRange(progress?.fontSize, fallback.progress.fontSize, 12, 20)
    },
    signature: {
      hintVisible: bool(signature?.hintVisible, fallback.signature.hintVisible),
      hintText: text(signature?.hintText, fallback.signature.hintText).slice(0, 60),
      hintFontSize: numberInRange(signature?.hintFontSize, fallback.signature.hintFontSize, 12, 20),
      hintFontWeight: signature?.hintFontWeight === 'normal' || signature?.hintFontWeight === 'bold' ? signature.hintFontWeight : 'medium',
      hintColor: color(signature?.hintColor, fallback.signature.hintColor),
      hintBackgroundColor: color(signature?.hintBackgroundColor, fallback.signature.hintBackgroundColor),
      hintBorderColor: color(signature?.hintBorderColor, fallback.signature.hintBorderColor),
      buttonText: text(signature?.buttonText, fallback.signature.buttonText).slice(0, 30),
      buttonFontSize: numberInRange(signature?.buttonFontSize, fallback.signature.buttonFontSize, 12, 20),
      buttonFontWeight: signature?.buttonFontWeight === 'normal' || signature?.buttonFontWeight === 'bold' ? signature.buttonFontWeight : 'medium',
      buttonTextColor: color(signature?.buttonTextColor, fallback.signature.buttonTextColor)
    },
    columns: normalizeReviewListColumns(value?.columns, fallback.columns),
    categoryColumns
  };
};
const normalizeNavigatorPage = (key: ArtBrowseNavigatorPageKey, value: Partial<ArtBrowseNavigatorPageConfig> | undefined) => {
  const fallback = defaultNavigatorPage(key);
  const fields = Array.isArray(value?.fields)
    ? [
        ...new Set(
          value.fields.map(String).filter((item): item is ArtBrowseNavigatorFieldKey => navigatorFieldKeys.has(item as ArtBrowseNavigatorFieldKey))
        )
      ]
    : fallback.fields;
  const safeFields =
    key === 'review'
      ? fields.filter((item) => item !== 'school')
      : key === 'audit'
        ? fields.filter((item) => item !== 'activity' && item !== 'programForm')
        : fields;
  const activityTitle = key === 'audit' ? value?.activityTitle : undefined;
  const fallbackActivityTitle = defaultAuditActivityTitle();
  return {
    layout: value?.layout === 'top' ? 'top' : 'sidebar',
    fields: safeFields,
    labels: Object.fromEntries(navigatorFieldOptions.map((item) => [item.key, text(value?.labels?.[item.key], fallback.labels[item.key])])) as Record<
      ArtBrowseNavigatorFieldKey,
      string
    >,
    activityTitle:
      key === 'audit'
        ? {
            displayMode: activityTitle?.displayMode === 'hidden' ? 'hidden' : 'title',
            template: safeTemplate(activityTitle?.template, fallbackActivityTitle.template),
            fontSize: numberInRange(activityTitle?.fontSize, fallbackActivityTitle.fontSize, 12, 30),
            color: color(activityTitle?.color, fallbackActivityTitle.color),
            fontWeight:
              activityTitle?.fontWeight === 'normal' || activityTitle?.fontWeight === 'medium'
                ? activityTitle.fontWeight
                : fallbackActivityTitle.fontWeight,
            align: activityTitle?.align === 'center' || activityTitle?.align === 'right' ? activityTitle.align : 'left',
            switchText: text(activityTitle?.switchText, fallbackActivityTitle.switchText).slice(0, 20),
            allowWrap: bool(activityTitle?.allowWrap, fallbackActivityTitle.allowWrap)
          }
        : undefined
  };
};

const normalizeAuditProgress = (value: Partial<ArtAuditProgressConfig> | undefined): ArtAuditProgressConfig => {
  const fallback = defaultAuditProgress();
  return {
    visible: bool(value?.visible, fallback.visible),
    title: text(value?.title, fallback.title).slice(0, 20),
    template: safeTemplate(value?.template, fallback.template).slice(0, 80),
    height: numberInRange(value?.height, fallback.height, 4, 20),
    width: numberInRange(value?.width, fallback.width, 120, 480),
    activeColor: color(value?.activeColor, fallback.activeColor),
    completeColor: color(value?.completeColor, fallback.completeColor),
    trackColor: color(value?.trackColor, fallback.trackColor),
    textColor: color(value?.textColor, fallback.textColor),
    fontSize: numberInRange(value?.fontSize, fallback.fontSize, 12, 20)
  };
};

const normalizeWorkspaceHeaderCard = (
  value: Partial<ArtWorkspaceHeaderCardConfig> | undefined,
  fallback: ArtWorkspaceHeaderCardConfig,
  index: number,
  spacerInstances: ArtWorkspaceHeaderPageConfig['spacerInstances'],
  compactGroupInstances: ArtWorkspaceHeaderPageConfig['compactGroupInstances']
): ArtWorkspaceHeaderCardConfig => {
  const rawRows = Array.isArray(value?.rows) ? value.rows : fallback.rows;
  const normalizeRows = (sourceRows: string[][]) =>
    [0, 1].map((rowIndex) => {
      const row = sourceRows[rowIndex];
      if (!Array.isArray(row)) return [];
      return row
        .map(String)
        .map((itemId) => {
          if (workspaceHeaderBusinessItemKeys.has(itemId as ArtWorkspaceHeaderItemKey)) return itemId;
          return spacerInstances[itemId] || compactGroupInstances[itemId] ? itemId : '';
        })
        .filter(Boolean)
        .slice(0, 12);
    });
  const rows = normalizeRows(rawRows as string[][]);
  const fallbackRows = normalizeRows(fallback.rows);
  return {
    key: String(value?.key || fallback.key || `card-${index + 1}`).slice(0, 40),
    rows: rows.some((row) => row.length) ? rows : fallbackRows,
    backgroundColor: color(value?.backgroundColor, fallback.backgroundColor),
    backgroundOpacity: numberInRange(value?.backgroundOpacity, fallback.backgroundOpacity, 0, 100),
    borderRadius: numberInRange(value?.borderRadius, fallback.borderRadius, 0, 24),
    borderVisible: bool(value?.borderVisible, fallback.borderVisible)
  };
};

const normalizeWorkspaceHeaderSpacerInstances = (
  value: Partial<ArtWorkspaceHeaderPageConfig> | undefined
): ArtWorkspaceHeaderPageConfig['spacerInstances'] =>
  Object.fromEntries(
    Object.entries(value?.spacerInstances || {})
      .filter(
        ([instanceId, config]) =>
          /^[A-Za-z][A-Za-z0-9_-]{0,47}$/.test(instanceId) && Boolean(config) && isWorkspaceHeaderSpacerKey(String(config.type))
      )
      .map(([instanceId, config]) => [
        instanceId,
        {
          type: config.type,
          span: config.type === 'fixedSpacer' ? numberInRange(config.span, 1, 1, 12) : 1,
          allowStatusBorrow: config.type === 'autoSpacer' && config.allowStatusBorrow === true
        } satisfies ArtWorkspaceHeaderSpacerInstanceConfig
      ])
  );

const normalizeWorkspaceHeaderCompactGroupInstances = (
  value: Partial<ArtWorkspaceHeaderPageConfig> | undefined
): ArtWorkspaceHeaderPageConfig['compactGroupInstances'] =>
  Object.fromEntries(
    Object.entries(value?.compactGroupInstances || {})
      .filter(([instanceId, config]) => /^[A-Za-z][A-Za-z0-9_-]{0,47}$/.test(instanceId) && Boolean(config))
      .map(([instanceId, config]) => {
        const align = config.align === 'left' || config.align === 'center' || config.align === 'right' ? config.align : 'right';
        const items = Array.isArray(config.items)
          ? config.items
              .map(String)
              .filter((item): item is ArtWorkspaceHeaderItemKey => workspaceHeaderBusinessItemKeys.has(item as ArtWorkspaceHeaderItemKey))
              .slice(0, 8)
          : [];
        return [
          instanceId,
          {
            widthMode: config.widthMode === 'auto' ? 'auto' : 'fixed',
            span: numberInRange(config.span, 4, 1, 12),
            align,
            gap: numberInRange(config.gap, 10, 0, 32),
            items,
            collapseMode: config.collapseMode === 'overflow' || config.collapseMode === 'all' ? config.collapseMode : 'none',
            pinnedItems: Array.isArray(config.pinnedItems)
              ? config.pinnedItems
                  .map(String)
                  .filter(
                    (item): item is ArtWorkspaceHeaderItemKey =>
                      items.includes(item as ArtWorkspaceHeaderItemKey) &&
                      workspaceHeaderCollapsibleButtonKeySet.has(item as ArtWorkspaceHeaderItemKey)
                  )
              : items.includes('unifiedSubmit')
                ? ['unifiedSubmit']
                : [],
            collapseText:
              String(config.collapseText || '更多')
                .trim()
                .slice(0, 10) || '更多'
          } satisfies ArtWorkspaceHeaderCompactGroupInstanceConfig
        ];
      })
  );

const normalizeWorkspaceHeaderItemConfigs = (
  value: Partial<ArtWorkspaceHeaderPageConfig> | undefined,
  fallback: ArtWorkspaceHeaderPageConfig
): ArtWorkspaceHeaderPageConfig['itemConfigs'] => {
  return Object.fromEntries(
    workspaceHeaderItemOptions.map(({ key }) => {
      const source = value?.itemConfigs?.[key];
      const defaultItem = fallback.itemConfigs[key];
      const alignCandidate = source?.align || (key === 'status' ? value?.statusAlign : undefined);
      const align = alignCandidate === 'left' || alignCandidate === 'center' || alignCandidate === 'right' ? alignCandidate : defaultItem.align;
      const texts = Object.fromEntries(
        Object.entries(defaultItem.texts).map(([textKey, defaultText]) => [
          textKey,
          String(source?.texts?.[textKey] ?? defaultText)
            .trim()
            .slice(0, 80)
        ])
      );
      return [
        key,
        {
          align,
          widthMode: source?.widthMode === 'auto' || source?.widthMode === 'fixed' ? source.widthMode : defaultItem.widthMode,
          span: numberInRange(source?.span, defaultItem.span, 1, 12),
          texts
        }
      ];
    })
  ) as ArtWorkspaceHeaderPageConfig['itemConfigs'];
};

export const normalizeWorkspaceHeaderPage = (
  key: ArtWorkspaceHeaderPageKey,
  value: Partial<ArtWorkspaceHeaderPageConfig> | undefined,
  sourceLayoutVersion = WORKSPACE_HEADER_LAYOUT_VERSION
): ArtWorkspaceHeaderPageConfig => {
  const fallback = defaultWorkspaceHeaderPage(key);
  if (sourceLayoutVersion !== WORKSPACE_HEADER_LAYOUT_VERSION) return fallback;
  const legacyReviewScopeLayout = key === 'review' && !Object.prototype.hasOwnProperty.call(value?.itemConfigs || {}, 'reviewScope');
  const itemConfigs = normalizeWorkspaceHeaderItemConfigs(value, fallback);
  const spacerInstances = normalizeWorkspaceHeaderSpacerInstances(value);
  const compactGroupInstances = normalizeWorkspaceHeaderCompactGroupInstances(value);
  Object.entries(fallback.compactGroupInstances).forEach(([instanceId, config]) => {
    if (!compactGroupInstances[instanceId]) {
      compactGroupInstances[instanceId] = { ...config, items: [...config.items], pinnedItems: [...config.pinnedItems] };
    }
  });
  const sourceCards = Array.isArray(value?.cards) && value.cards.length ? value.cards.slice(0, 6) : fallback.cards;
  const cards = sourceCards.map((card, index) =>
    normalizeWorkspaceHeaderCard(card, fallback.cards[index] || fallback.cards[0], index, spacerInstances, compactGroupInstances)
  );
  const usedItems = new Set<ArtWorkspaceHeaderItemKey>();
  const usedSpacerInstances = new Set<string>();
  const usedCompactGroupInstances = new Set<string>();
  const uniqueCards = cards.map((card) => ({
    ...card,
    rows: card.rows.map((row) =>
      row
        .map((itemId) => {
          const itemKey = workspaceHeaderLayoutItemKey(itemId, { spacerInstances, compactGroupInstances });
          if (isWorkspaceHeaderSpacerKey(itemKey)) {
            if (!usedSpacerInstances.has(itemId)) {
              usedSpacerInstances.add(itemId);
              return itemId;
            }
            const sourceSpacer = spacerInstances[itemId];
            const cloneId = createWorkspaceHeaderSpacerInstance({ itemConfigs, spacerInstances }, sourceSpacer.type);
            spacerInstances[cloneId] = { ...sourceSpacer };
            usedSpacerInstances.add(cloneId);
            return cloneId;
          }
          if (itemKey === 'compactGroup') {
            const sourceGroup = compactGroupInstances[itemId];
            if (!sourceGroup) return '';
            let groupId = itemId;
            if (usedCompactGroupInstances.has(groupId)) {
              groupId = createWorkspaceHeaderCompactGroupInstance({ itemConfigs, compactGroupInstances }, sourceGroup.items);
              compactGroupInstances[groupId] = {
                ...sourceGroup,
                items: [...sourceGroup.items],
                pinnedItems: [...sourceGroup.pinnedItems]
              };
            }
            const group = compactGroupInstances[groupId];
            group.items = group.items.filter((childKey) => {
              const itemKey = childKey as ArtWorkspaceHeaderItemKey;
              if (!workspaceHeaderBusinessItemKeys.has(itemKey) || usedItems.has(itemKey)) return false;
              usedItems.add(itemKey);
              return true;
            });
            group.pinnedItems = group.pinnedItems.filter(
              (childKey) => group.items.includes(childKey) && workspaceHeaderCollapsibleButtonKeySet.has(childKey)
            );
            usedCompactGroupInstances.add(groupId);
            return group.items.length ? groupId : '';
          }
          if (usedItems.has(itemKey)) return '';
          usedItems.add(itemKey);
          return itemId;
        })
        .filter(Boolean)
    )
  }));
  if (legacyReviewScopeLayout && !usedItems.has('reviewScope')) {
    const filterCard = uniqueCards.find((card) => card.key === 'filters') || uniqueCards[uniqueCards.length - 1];
    if (filterCard) {
      if (!filterCard.rows[1]?.length) {
        filterCard.rows[1] = [...(filterCard.rows[0] || [])];
        filterCard.rows[0] = ['reviewScope'];
      } else {
        filterCard.rows[0] = ['reviewScope', ...(filterCard.rows[0] || [])];
      }
      usedItems.add('reviewScope');
    }
  }
  if (key === 'schoolSubmit' && !usedItems.has('home')) {
    const batchRow = uniqueCards[0]?.rows[0];
    if (batchRow) {
      batchRow.push('home');
      usedItems.add('home');
    }
  }
  const reviewHasSelectAll = usedItems.has('selectAll');
  const reviewHasUnifiedSubmit = usedItems.has('unifiedSubmit');
  if (key === 'review') {
    const primaryCard = uniqueCards.find((card) => card.rows.some((row) => row.includes('actions'))) || uniqueCards[0];
    const primaryRow = primaryCard?.rows.find((row) => row.includes('actions')) || primaryCard?.rows[0];
    if (primaryRow && !reviewHasSelectAll) {
      const actionsIndex = primaryRow.indexOf('actions');
      primaryRow.splice(actionsIndex < 0 ? primaryRow.length : actionsIndex, 0, 'selectAll');
      usedItems.add('selectAll');
    }
    if (primaryRow && !reviewHasUnifiedSubmit) {
      const actionsIndex = primaryRow.indexOf('actions');
      primaryRow.splice(actionsIndex < 0 ? primaryRow.length : actionsIndex + 1, 0, 'unifiedSubmit');
      usedItems.add('unifiedSubmit');
    }
  }
  const normalizedStatusAlign =
    value?.statusAlign === 'left' || value?.statusAlign === 'center' || value?.statusAlign === 'right' ? value.statusAlign : fallback.statusAlign;
  const fallbackStatusItems = fallback.statusItems;
  const sourceStatusItems = Array.isArray(value?.statusItems) ? value.statusItems : fallbackStatusItems;
  const knownStatusKeys = new Set(fallbackStatusItems.map((item) => item.key));
  const statusItems = sourceStatusItems
    .filter((item) => item && knownStatusKeys.has(String(item.key)))
    .map((item, index) => ({
      key: String(item.key),
      visible: item.visible !== false,
      order: numberInRange(item.order, index + 1, 1, 99)
    }))
    .filter((item, index, items) => items.findIndex((candidate) => candidate.key === item.key) === index);
  fallbackStatusItems.forEach((item) => {
    if (!statusItems.some((candidate) => candidate.key === item.key)) statusItems.push({ ...item });
  });
  const selectAllButton = normalizeWorkspaceHeaderButton(value?.selectAllButton, fallback.selectAllButton, true);
  const unifiedSubmitButton = normalizeWorkspaceHeaderButton(value?.unifiedSubmitButton, fallback.unifiedSubmitButton);
  const navigationButtonBase = normalizeWorkspaceHeaderButton(value?.navigationButton, fallback.navigationButton);
  const navigationButton = {
    ...navigationButtonBase,
    targetMode: value?.navigationButton?.targetMode === 'custom' ? 'custom' : 'preset',
    presetKey:
      String(value?.navigationButton?.presetKey || fallback.navigationButton.presetKey)
        .trim()
        .slice(0, 40) || fallback.navigationButton.presetKey,
    customUrl: normalizeWorkspaceNavigationUrl(value?.navigationButton?.customUrl),
    openMode: value?.navigationButton?.openMode === 'new' ? 'new' : 'current'
  } satisfies ArtWorkspaceHeaderPageConfig['navigationButton'];
  if (key === 'review' && !reviewHasSelectAll) {
    selectAllButton.text = fallback.selectAllButton.text;
    selectAllButton.alternateText = fallback.selectAllButton.alternateText;
    selectAllButton.tooltip = fallback.selectAllButton.tooltip;
    selectAllButton.alternateTooltip = fallback.selectAllButton.alternateTooltip;
  }
  if (key === 'review' && !reviewHasUnifiedSubmit) {
    unifiedSubmitButton.text = fallback.unifiedSubmitButton.text;
    unifiedSubmitButton.tooltip = fallback.unifiedSubmitButton.tooltip;
  }
  Object.keys(spacerInstances).forEach((instanceId) => {
    if (!usedSpacerInstances.has(instanceId)) delete spacerInstances[instanceId];
  });
  Object.keys(compactGroupInstances).forEach((instanceId) => {
    if (!usedCompactGroupInstances.has(instanceId)) delete compactGroupInstances[instanceId];
  });
  return {
    titleTemplate: safeTemplate(value?.titleTemplate, fallback.titleTemplate, 120),
    titleFontSize: numberInRange(value?.titleFontSize, fallback.titleFontSize, 14, 42),
    titleFontWeight: [400, 500, 600, 700, 800, 900].includes(Number(value?.titleFontWeight))
      ? Number(value?.titleFontWeight)
      : fallback.titleFontWeight,
    titleColor: /^#[0-9a-f]{6}$/i.test(String(value?.titleColor || '')) ? String(value?.titleColor) : fallback.titleColor,
    sidebarEnabled: bool(value?.sidebarEnabled, fallback.sidebarEnabled),
    sidebarDefaultExpanded: bool(value?.sidebarDefaultExpanded, fallback.sidebarDefaultExpanded),
    statusAlign: normalizedStatusAlign,
    statusCollapseOnOverflow: bool(value?.statusCollapseOnOverflow, fallback.statusCollapseOnOverflow),
    statusItems,
    componentGap: numberInRange(value?.componentGap, fallback.componentGap, 0, 32),
    rowMinHeight: numberInRange(value?.rowMinHeight, fallback.rowMinHeight, 28, 96),
    paddingTop: numberInRange(value?.paddingTop, fallback.paddingTop, 0, 32),
    paddingBottom: numberInRange(value?.paddingBottom, fallback.paddingBottom, 0, 32),
    paddingInline: numberInRange(value?.paddingInline, fallback.paddingInline, 0, 40),
    marginTop: numberInRange(value?.marginTop, fallback.marginTop, 0, 40),
    marginBottom: numberInRange(value?.marginBottom, fallback.marginBottom, 0, 40),
    navigatorToggleButton: normalizeWorkspaceHeaderButton(value?.navigatorToggleButton, fallback.navigatorToggleButton, true),
    columnWidthResetButton: normalizeWorkspaceHeaderButton(value?.columnWidthResetButton, fallback.columnWidthResetButton),
    selectAllButton,
    unifiedSubmitButton,
    navigationButton,
    homeButton: normalizeWorkspaceHeaderButton(value?.homeButton, fallback.homeButton),
    buttonStyle: {
      fontSize: numberInRange(value?.buttonStyle?.fontSize, fallback.buttonStyle.fontSize, 12, 18),
      fontWeight: [400, 500, 600, 700].includes(Number(value?.buttonStyle?.fontWeight))
        ? Number(value?.buttonStyle?.fontWeight)
        : fallback.buttonStyle.fontWeight,
      iconSize: numberInRange(value?.buttonStyle?.iconSize, fallback.buttonStyle.iconSize, 12, 24),
      height: numberInRange(value?.buttonStyle?.height, fallback.buttonStyle.height, 24, 44)
    },
    itemConfigs,
    spacerInstances,
    compactGroupInstances,
    cards: uniqueCards
  };
};

const normalizeWorkspaceHeaderButton = (
  value: Partial<ArtWorkspaceHeaderPageConfig['navigatorToggleButton']> | undefined,
  fallback: ArtWorkspaceHeaderPageConfig['navigatorToggleButton'],
  alternate = false
) => ({
  text: String(value?.text ?? fallback.text)
    .trim()
    .slice(0, 30),
  alternateText: alternate
    ? String(value?.alternateText ?? fallback.alternateText)
        .trim()
        .slice(0, 30)
    : '',
  tooltip:
    String(value?.tooltip ?? fallback.tooltip)
      .trim()
      .slice(0, 50) || fallback.tooltip,
  alternateTooltip: alternate
    ? String(value?.alternateTooltip ?? fallback.alternateTooltip)
        .trim()
        .slice(0, 50) || fallback.alternateTooltip
    : '',
  align: value?.align === 'left' || value?.align === 'center' || value?.align === 'right' ? value.align : fallback.align,
  iconVisible: value?.iconVisible === undefined ? fallback.iconVisible !== false : value.iconVisible !== false,
  variant: value?.variant === 'plain' || value?.variant === 'text' ? value.variant : fallback.variant || 'default',
  showCount: value?.showCount === undefined ? fallback.showCount === true : value.showCount === true,
  appearance: fallback.appearance ? normalizeWorkspaceHeaderButtonAppearance(value?.appearance, fallback.appearance) : undefined
});

const normalizeWorkspaceHeaderButtonAppearance = (
  value: Partial<ArtWorkspaceHeaderButtonAppearanceConfig> | undefined,
  fallback: ArtWorkspaceHeaderButtonAppearanceConfig
): ArtWorkspaceHeaderButtonAppearanceConfig => ({
  backgroundColor: color(value?.backgroundColor, fallback.backgroundColor),
  backgroundOpacity: numberInRange(value?.backgroundOpacity, fallback.backgroundOpacity, 0, 100),
  textColor: color(value?.textColor, fallback.textColor),
  borderVisible: bool(value?.borderVisible, fallback.borderVisible),
  borderColor: color(value?.borderColor, fallback.borderColor),
  borderOpacity: numberInRange(value?.borderOpacity, fallback.borderOpacity, 0, 100),
  borderWidth: numberInRange(value?.borderWidth, fallback.borderWidth, 0, 6),
  borderRadius: numberInRange(value?.borderRadius, fallback.borderRadius, 0, 24),
  hoverBackgroundColor: color(value?.hoverBackgroundColor, fallback.hoverBackgroundColor),
  hoverBackgroundOpacity: numberInRange(value?.hoverBackgroundOpacity, fallback.hoverBackgroundOpacity, 0, 100),
  hoverTextColor: color(value?.hoverTextColor, fallback.hoverTextColor),
  hoverBorderColor: color(value?.hoverBorderColor, fallback.hoverBorderColor),
  hoverBorderOpacity: numberInRange(value?.hoverBorderOpacity, fallback.hoverBorderOpacity, 0, 100)
});

const normalizeWorkspaceHeader = (value: Partial<ArtWorkspaceHeaderConfig> | undefined): ArtWorkspaceHeaderConfig => {
  const sourceLayoutVersion = Number(value?.layoutVersion || 0);
  if (sourceLayoutVersion !== WORKSPACE_HEADER_LAYOUT_VERSION) return defaultWorkspaceHeader();
  return {
    layoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
    managedActivityId:
      value?.managedActivityId === undefined || value.managedActivityId === null || String(value.managedActivityId).trim() === ''
        ? undefined
        : String(value.managedActivityId),
    pages: {
      project: normalizeWorkspaceHeaderPage('project', value?.pages?.project, sourceLayoutVersion),
      audit: normalizeWorkspaceHeaderPage('audit', value?.pages?.audit, sourceLayoutVersion),
      review: normalizeWorkspaceHeaderPage('review', value?.pages?.review, sourceLayoutVersion),
      schoolSubmit: normalizeWorkspaceHeaderPage('schoolSubmit', value?.pages?.schoolSubmit, sourceLayoutVersion),
      projectView: normalizeWorkspaceHeaderPage('projectView', value?.pages?.projectView, sourceLayoutVersion)
    }
  };
};

const normalizeListTableAppearance = (value: Partial<ArtListTableAppearanceConfig> | undefined): ArtListTableAppearanceConfig => {
  const fallback = defaultListTableAppearance();
  const statusFallback = fallback.statusAppearance;
  const actionFallback = fallback.rowActionAppearance;
  const legacyBorderMode = ['horizontal', 'grid', 'none'].includes(String(value?.borderMode)) ? value!.borderMode! : fallback.borderMode;
  const horizontalBorderVisible = bool(value?.horizontalBorderVisible, legacyBorderMode !== 'none');
  const verticalBorderVisible = bool(value?.verticalBorderVisible, legacyBorderMode === 'grid');
  return {
    globalColorOverride: bool(value?.globalColorOverride, fallback.globalColorOverride),
    headerBackground: color(value?.headerBackground, fallback.headerBackground),
    headerTextColor: color(value?.headerTextColor, fallback.headerTextColor),
    textColor: color(value?.textColor, fallback.textColor),
    hoverBackground: color(value?.hoverBackground, fallback.hoverBackground),
    hoverBackgroundOpacity: numberInRange(value?.hoverBackgroundOpacity, fallback.hoverBackgroundOpacity, 0, 100),
    borderColor: color(value?.borderColor, fallback.borderColor),
    horizontalBorderVisible,
    horizontalBorderColor: color(value?.horizontalBorderColor || value?.borderColor, fallback.horizontalBorderColor),
    horizontalBorderOpacity: numberInRange(value?.horizontalBorderOpacity, fallback.horizontalBorderOpacity, 0, 100),
    verticalBorderVisible,
    verticalBorderColor: color(value?.verticalBorderColor || value?.borderColor, fallback.verticalBorderColor),
    verticalBorderOpacity: numberInRange(value?.verticalBorderOpacity, verticalBorderVisible ? 100 : fallback.verticalBorderOpacity, 0, 100),
    outerBorderVisible: bool(value?.outerBorderVisible, legacyBorderMode !== 'none'),
    outerBorderColor: color(value?.outerBorderColor || value?.borderColor, fallback.outerBorderColor),
    outerBorderOpacity: numberInRange(value?.outerBorderOpacity, fallback.outerBorderOpacity, 0, 100),
    headerFontSize: numberInRange(value?.headerFontSize, fallback.headerFontSize, 12, 20),
    bodyFontSize: numberInRange(value?.bodyFontSize, fallback.bodyFontSize, 12, 20),
    buttonFontSize: numberInRange(value?.buttonFontSize, fallback.buttonFontSize, 12, 20),
    rowHeight: numberInRange(value?.rowHeight, fallback.rowHeight, 34, 72),
    borderRadius: numberInRange(value?.borderRadius, fallback.borderRadius, 0, 24),
    borderMode: verticalBorderVisible ? 'grid' : horizontalBorderVisible ? 'horizontal' : 'none',
    wrapMode: value?.wrapMode === 'nowrap' || value?.wrapMode === 'two-line' ? value.wrapMode : 'wrap',
    headerWrap: bool(value?.headerWrap, fallback.headerWrap),
    striped: bool(value?.striped, fallback.striped),
    actionStyle: value?.actionStyle === 'button' ? 'button' : 'link',
    statusAppearance: {
      borderVisible: bool(value?.statusAppearance?.borderVisible, statusFallback.borderVisible),
      iconVisible: bool(value?.statusAppearance?.iconVisible, statusFallback.iconVisible),
      uniformWidth: bool(value?.statusAppearance?.uniformWidth, statusFallback.uniformWidth),
      colors: {
        draft: color(value?.statusAppearance?.colors?.draft, statusFallback.colors.draft),
        pending: color(value?.statusAppearance?.colors?.pending, statusFallback.colors.pending),
        approved: color(value?.statusAppearance?.colors?.approved, statusFallback.colors.approved),
        returned: color(value?.statusAppearance?.colors?.returned, statusFallback.colors.returned),
        unscored: color(value?.statusAppearance?.colors?.unscored, statusFallback.colors.unscored),
        scored: color(value?.statusAppearance?.colors?.scored, statusFallback.colors.scored),
        scoreDraft: color(value?.statusAppearance?.colors?.scoreDraft, statusFallback.colors.scoreDraft),
        locked: color(value?.statusAppearance?.colors?.locked, statusFallback.colors.locked)
      }
    },
    rowActionAppearance: {
      borderVisible: bool(value?.rowActionAppearance?.borderVisible, actionFallback.borderVisible),
      iconVisible: bool(value?.rowActionAppearance?.iconVisible, actionFallback.iconVisible),
      uniformWidth: bool(value?.rowActionAppearance?.uniformWidth, actionFallback.uniformWidth),
      disabledOpacity: numberInRange(value?.rowActionAppearance?.disabledOpacity, actionFallback.disabledOpacity, 10, 100),
      colors: {
        view: color(value?.rowActionAppearance?.colors?.view, actionFallback.colors.view),
        score: color(value?.rowActionAppearance?.colors?.score, actionFallback.colors.score),
        edit: color(value?.rowActionAppearance?.colors?.edit, actionFallback.colors.edit),
        return: color(value?.rowActionAppearance?.colors?.return, actionFallback.colors.return),
        withdraw: color(value?.rowActionAppearance?.colors?.withdraw, actionFallback.colors.withdraw),
        submit: color(value?.rowActionAppearance?.colors?.submit, actionFallback.colors.submit),
        delete: color(value?.rowActionAppearance?.colors?.delete, actionFallback.colors.delete)
      }
    }
  };
};

const normalizeTableColumns = (
  value: ArtReviewListColumnConfig[] | undefined,
  fallback: ArtReviewListColumnConfig[],
  allowForm: boolean,
  pageKey?: ArtListTablePageKey
): ArtReviewListColumnConfig[] => {
  const normalizeFixed = (value: unknown, key: string, fallback: ArtReviewListColumnConfig['fixed']) => {
    const fallbackFixed = fallback || 'none';
    const fixed = value === 'left' || value === 'right' || value === 'none' ? value : fallbackFixed;
    if (key === 'selection') return fixed === 'left' ? 'left' : 'none';
    if (key === 'actions') return fixed === 'right' ? 'right' : 'none';
    return fixed;
  };
  const builtinKeys = new Set(fallback.filter((column) => column.source !== 'form').map((column) => column.key));
  const fallbackMap = new Map(fallback.map((column) => [column.key, column]));
  const seen = new Set<string>();
  const normalized = (Array.isArray(value) ? value : fallback)
    .map((column) => {
      const form = allowForm && column?.source === 'form';
      const fieldKey = String(column?.fieldKey || '').trim();
      const key = form ? `field:${fieldKey}` : String(column?.key || '');
      if (!key || seen.has(key) || (!form && !builtinKeys.has(key)) || (form && !/^[A-Za-z0-9_.-]{1,80}$/.test(fieldKey))) return undefined;
      seen.add(key);
      const defaults = fallbackMap.get(key);
      const visibilityLocked = pageKey === 'review' && key === 'groupOrNature';
      const deletionLocked = key === 'actions' || visibilityLocked;
      const deleted = deletionLocked ? false : bool(column.deleted, false);
      const minFloor = key === 'actions' ? Math.max(96, Number(defaults?.minWidth || 96)) : 48;
      const minWidth = numberInRange(column.minWidth, Number(defaults?.minWidth || 90), minFloor, 400);
      return {
        key,
        source: form ? ('form' as const) : ('builtin' as const),
        fieldKey: form ? fieldKey : undefined,
        label: text(column.label, defaults?.label || fieldKey || key),
        visible: visibilityLocked || (!deleted && bool(column.visible, defaults?.visible !== false)),
        deleted,
        width: numberInRange(column.width, Number(defaults?.width || 140), Math.max(minFloor, minWidth), 800),
        minWidth,
        fixed: normalizeFixed(column.fixed, key, defaults?.fixed || 'none')
      };
    })
    .filter(Boolean) as ArtReviewListColumnConfig[];
  fallback.forEach((column) => {
    if (!seen.has(column.key)) normalized.push(JSON.parse(JSON.stringify(column)) as ArtReviewListColumnConfig);
  });
  const zoneWeight = { left: 0, none: 1, right: 2 };
  return normalized
    .map((column, index) => ({ column, index }))
    .sort(
      (left, right) =>
        zoneWeight[left.column.fixed || 'none'] - zoneWeight[right.column.fixed || 'none'] ||
        Number(left.column.key === 'actions') - Number(right.column.key === 'actions') ||
        left.index - right.index
    )
    .map(({ column }) => column);
};

const normalizeTableTextMap = <T extends string>(value: Record<T, string> | undefined, fallback: Record<T, string>): Record<T, string> =>
  Object.fromEntries(Object.entries(fallback).map(([key, label]) => [key, text(value?.[key as T], String(label))])) as Record<T, string>;

export const normalizeArtListTableLayout = (value?: Partial<ArtListTableLayoutConfig>): ArtListTableLayoutConfig => {
  const fallback = defaultArtListTableLayout();
  const sourceVersion = numberInRange(value?.version, 1, 1, 100);
  return {
    version: ART_LIST_TABLE_LAYOUT_VERSION,
    pages: Object.fromEntries(
      artListTablePageOptions.map(({ key }) => {
        const source = value?.pages?.[key];
        const defaults = fallback.pages[key];
        let columns = normalizeTableColumns(source?.columns, defaults.columns, false, key);
        if (key === 'review') {
          columns = migrateLegacyReviewDefaultColumns(columns, source?.columns, sourceVersion);
        }
        columns = migrateLegacyActionFixedRight(columns, sourceVersion);
        const categoryColumns = Object.fromEntries(
          Object.entries(source?.categoryColumns || {})
            .filter(([categoryId]) => /^\d+$/.test(categoryId))
            .slice(0, 200)
            .map(([categoryId, configured]) => [
              categoryId,
              migrateLegacyActionFixedRight(normalizeTableColumns(configured, columns, true, key), sourceVersion)
            ])
        );
        return [
          key,
          {
            emptyText: text(source?.emptyText, defaults.emptyText),
            selectionFixed: source?.selectionFixed === 'left' ? 'left' : defaults.selectionFixed,
            serialVisible: bool(source?.serialVisible, defaults.serialVisible),
            serialWidth: numberInRange(source?.serialWidth, defaults.serialWidth, 48, 240),
            serialFixed: source?.serialFixed === 'left' ? 'left' : defaults.serialFixed,
            columns,
            categoryColumns,
            statusLabels: normalizeTableTextMap(source?.statusLabels, defaults.statusLabels),
            actionLabels: normalizeTableTextMap(source?.actionLabels, defaults.actionLabels)
          }
        ];
      })
    ) as Record<ArtListTablePageKey, ArtListTablePageConfig>
  };
};

export const normalizeArtDetailDisplayConfig = (value?: Partial<ArtDetailDisplayConfigVO>): ArtDetailDisplayConfigVO => {
  const fallback = defaultConfig();
  const tabs = value?.tabs;
  const audit = value?.audit;
  const files = value?.files;
  const score = value?.score;
  const navigator = value?.navigator;
  const basicInfoLabel = text(tabs?.basicInfoLabel, fallback.tabs.basicInfoLabel);
  const projectFilesLabel = text(tabs?.projectFilesLabel, fallback.tabs.projectFilesLabel);
  const auditTabFallback = defaultPopupTabs(auditDefaultTabOptions);
  auditTabFallback.labels.form = basicInfoLabel;
  auditTabFallback.labels.files = projectFilesLabel;
  const scoreTabFallback = defaultPopupTabs(scoreDefaultTabOptions);
  scoreTabFallback.labels.form = basicInfoLabel;
  scoreTabFallback.labels.files = projectFilesLabel;
  const auditTabs = normalizePopupTabs(tabs?.audit, auditDefaultTabOptions, auditTabFallback, tabs?.auditDefaultTab);
  const scoreTabs = normalizePopupTabs(tabs?.score, scoreDefaultTabOptions, scoreTabFallback, tabs?.scoreDefaultTab);
  return {
    tabs: {
      basicInfoLabel,
      projectFilesLabel,
      auditDefaultTab: auditTabs.defaultTab,
      scoreDefaultTab: scoreTabs.defaultTab,
      audit: auditTabs,
      score: scoreTabs
    },
    audit: {
      title: text(audit?.title, fallback.audit.title),
      summaryFields: uniqueAllowed(audit?.summaryFields, allowedAuditKeys, fallback.audit.summaryFields),
      summaryLabels: Object.fromEntries(
        auditSummaryFieldOptions.map((item) => [item.key, text(audit?.summaryLabels?.[item.key], fallback.audit.summaryLabels[item.key])])
      ),
      actionsVisible: bool(audit?.actionsVisible, fallback.audit.actionsVisible),
      content: {
        basicSections: typedAllowed(audit?.content?.basicSections, allowedAuditBasicSections, fallback.audit.content.basicSections),
        fileColumns: typedAllowed(audit?.content?.fileColumns, allowedAuditFileColumns, fallback.audit.content.fileColumns),
        recordColumns: typedAllowed(audit?.content?.recordColumns, allowedAuditRecordColumns, fallback.audit.content.recordColumns)
      }
    },
    files: {
      fields: normalizeFileFields(files?.fields, fallback.files.fields),
      labels: Object.fromEntries(detailFileFieldOptions.map((item) => [item.key, text(files?.labels?.[item.key], fallback.files.labels[item.key])]))
    },
    score: {
      title: text(score?.title, fallback.score.title),
      scoreLabel: text(score?.scoreLabel, fallback.score.scoreLabel),
      gradeLabel: text(score?.gradeLabel, fallback.score.gradeLabel),
      commentLabel: text(score?.commentLabel, fallback.score.commentLabel),
      statusVisible: bool(score?.statusVisible, fallback.score.statusVisible),
      commentVisible: bool(score?.commentVisible, fallback.score.commentVisible),
      saveDraftVisible: bool(score?.saveDraftVisible, fallback.score.saveDraftVisible),
      fileListVisible: bool(score?.fileListVisible, fallback.score.fileListVisible),
      content: {
        basicSections: typedAllowed(score?.content?.basicSections, allowedScoreBasicSections, fallback.score.content.basicSections),
        fileColumns: typedAllowed(score?.content?.fileColumns, allowedScoreFileColumns, fallback.score.content.fileColumns),
        recordColumns: typedAllowed(score?.content?.recordColumns, allowedScoreRecordColumns, fallback.score.content.recordColumns)
      }
    },
    navigator: {
      titleFontSize: numberInRange(navigator?.titleFontSize, fallback.navigator.titleFontSize, 12, 22),
      itemFontSize: numberInRange(navigator?.itemFontSize, fallback.navigator.itemFontSize, 12, 20),
      countFontSize: numberInRange(navigator?.countFontSize, fallback.navigator.countFontSize, 10, 18),
      rowHeight: numberInRange(navigator?.rowHeight, fallback.navigator.rowHeight, 30, 60),
      itemGap: numberInRange(navigator?.itemGap, fallback.navigator.itemGap, 0, 16),
      panelPadding: numberInRange(navigator?.panelPadding, fallback.navigator.panelPadding, 8, 24),
      backgroundColor: color(navigator?.backgroundColor, fallback.navigator.backgroundColor),
      textColor: color(navigator?.textColor, fallback.navigator.textColor),
      activeColor: color(navigator?.activeColor, fallback.navigator.activeColor),
      hoverBackground: color(navigator?.hoverBackground || navigator?.activeColor, fallback.navigator.hoverBackground),
      hoverBackgroundOpacity: numberInRange(navigator?.hoverBackgroundOpacity, fallback.navigator.hoverBackgroundOpacity, 0, 100),
      selectedBackground: color(navigator?.selectedBackground || navigator?.activeColor, fallback.navigator.selectedBackground),
      selectedBackgroundOpacity: numberInRange(navigator?.selectedBackgroundOpacity, fallback.navigator.selectedBackgroundOpacity, 0, 100),
      selectedTextColor: color(navigator?.selectedTextColor || navigator?.activeColor, fallback.navigator.selectedTextColor),
      borderColor: color(navigator?.borderColor, fallback.navigator.borderColor),
      backgroundOpacity: numberInRange(navigator?.backgroundOpacity, fallback.navigator.backgroundOpacity, 0, 100),
      dividerVisible: bool(navigator?.dividerVisible, fallback.navigator.dividerVisible),
      dividerColor: color(navigator?.dividerColor || navigator?.borderColor, fallback.navigator.dividerColor),
      dividerOpacity: numberInRange(navigator?.dividerOpacity, fallback.navigator.dividerOpacity, 0, 100),
      dividerWidth: numberInRange(navigator?.dividerWidth, fallback.navigator.dividerWidth, 1, 3),
      borderRadius: numberInRange(navigator?.borderRadius, fallback.navigator.borderRadius, 0, 24),
      sidebarWidth: numberInRange(navigator?.sidebarWidth, fallback.navigator.sidebarWidth, 260, 460),
      sidebarMaxHeight: numberInRange(navigator?.sidebarMaxHeight, fallback.navigator.sidebarMaxHeight, 320, 960),
      topHeight: numberInRange(navigator?.topHeight, fallback.navigator.topHeight, 56, 180),
      borderVisible: bool(navigator?.borderVisible, fallback.navigator.borderVisible),
      shadowVisible: bool(navigator?.shadowVisible, fallback.navigator.shadowVisible),
      pages: Object.fromEntries(
        navigatorPageOptions.map((item) => [
          item.key,
          normalizeNavigatorPage(item.key, navigatorPageKeys.has(item.key) ? navigator?.pages?.[item.key] : undefined)
        ])
      ) as Record<ArtBrowseNavigatorPageKey, ArtBrowseNavigatorPageConfig>
    },
    auditProgress: normalizeAuditProgress(value?.auditProgress),
    workspaceHeader: normalizeWorkspaceHeader(value?.workspaceHeader),
    listTableAppearance: normalizeListTableAppearance(value?.listTableAppearance),
    listTableLayout: normalizeArtListTableLayout(value?.listTableLayout),
    reviewWorkbench: normalizeReviewWorkbench(value?.reviewWorkbench)
  };
};

export const cloneArtDetailDisplayConfig = (value: unknown = state.value) =>
  normalizeArtDetailDisplayConfig(JSON.parse(JSON.stringify(value)) as Partial<ArtDetailDisplayConfigVO>);

export const loadArtDetailDisplayConfig = async (force = false) => {
  if (loaded && !force) return state.value;
  if (pendingLoad) return pendingLoad;
  pendingLoad = getArtDetailDisplayConfig()
    .then(({ data }) => {
      state.value = normalizeArtDetailDisplayConfig(data);
      loaded = true;
      return state.value;
    })
    .catch((error) => {
      console.warn('艺术评审详情显示配置加载失败，已使用默认配置。', error);
      state.value = normalizeArtDetailDisplayConfig(state.value);
      return state.value;
    })
    .finally(() => {
      pendingLoad = undefined;
    });
  return pendingLoad;
};

export const saveArtDetailDisplayConfig = async (value: ArtDetailDisplayConfigVO) => {
  const normalized = normalizeArtDetailDisplayConfig(value);
  normalized.listTableAppearance = JSON.parse(JSON.stringify(state.value.listTableAppearance)) as ArtListTableAppearanceConfig;
  normalized.listTableLayout = JSON.parse(JSON.stringify(state.value.listTableLayout)) as ArtListTableLayoutConfig;
  await updateArtDetailDisplayConfig(normalized);
  state.value = normalized;
  loaded = true;
  return state.value;
};

export const loadArtListTableConfig = async (): Promise<ArtListTableConfigVO> => {
  const { data } = await getArtListTableConfig();
  const normalized: ArtListTableConfigVO = {
    appearance: normalizeListTableAppearance(data?.appearance),
    layout: normalizeArtListTableLayout(data?.layout)
  };
  state.value = {
    ...state.value,
    listTableAppearance: normalized.appearance,
    listTableLayout: normalized.layout
  };
  loaded = true;
  return JSON.parse(JSON.stringify(normalized)) as ArtListTableConfigVO;
};

export const saveArtListTableConfig = async (value: ArtListTableConfigVO): Promise<ArtListTableConfigVO> => {
  const normalized: ArtListTableConfigVO = {
    appearance: normalizeListTableAppearance(value.appearance),
    layout: normalizeArtListTableLayout(value.layout)
  };
  await updateArtListTableConfig(normalized);
  state.value = {
    ...state.value,
    listTableAppearance: normalized.appearance,
    listTableLayout: normalized.layout
  };
  loaded = true;
  return JSON.parse(JSON.stringify(normalized)) as ArtListTableConfigVO;
};

export const resetArtDetailDisplayConfig = () => defaultConfig();

export const useArtDetailDisplayConfig = () => ({
  detailDisplayConfig: readonly(state),
  loadArtDetailDisplayConfig,
  saveArtDetailDisplayConfig
});
