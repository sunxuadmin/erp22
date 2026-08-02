import request from '@/utils/request';
import type { AxiosPromise } from 'axios';
import type { CategoryFieldSchemaVO } from './types';

export const saveManagedActivity = (activityId: string | number) => {
  return request({
    url: `/crehn/detail-display-config/managed-activity/${activityId}`,
    method: 'put'
  });
};

export type ArtAuditDetailTab = 'preview' | 'form' | 'members' | 'files' | 'records';
export type ArtScoreDetailTab = 'preview' | 'form' | 'members' | 'files' | 'scores';
export type ArtAuditBasicSectionKey = 'formFields' | 'genericTables' | 'downloadWorkbook';
export type ArtAuditFileColumnKey = 'fileType' | 'fileName' | 'fileSize' | 'technicalCheck' | 'mediaInfo' | 'actions';
export type ArtAuditRecordColumnKey = 'operator' | 'result' | 'transition' | 'opinion' | 'time';
export type ArtScoreBasicSectionKey = 'submittedAt' | 'scoreVisibility' | 'formFields' | 'genericTables';
export type ArtScoreFileColumnKey = 'fileType' | 'fileName' | 'fileSize' | 'previewStatus' | 'actions';
export type ArtScoreRecordColumnKey = 'reviewer' | 'result' | 'comment';

export interface ArtDetailPopupTabConfig<T extends string> {
  order: T[];
  visibleTabs: T[];
  labels: Record<T, string>;
  defaultTab: T;
}

export interface ArtDetailTabConfig {
  basicInfoLabel: string;
  projectFilesLabel: string;
  auditDefaultTab: ArtAuditDetailTab;
  scoreDefaultTab: ArtScoreDetailTab;
  audit: ArtDetailPopupTabConfig<ArtAuditDetailTab>;
  score: ArtDetailPopupTabConfig<ArtScoreDetailTab>;
}

export interface ArtAuditContentConfig {
  basicSections: ArtAuditBasicSectionKey[];
  fileColumns: ArtAuditFileColumnKey[];
  recordColumns: ArtAuditRecordColumnKey[];
}

export interface ArtDetailAuditConfig {
  title: string;
  summaryFields: string[];
  summaryLabels: Record<string, string>;
  actionsVisible: boolean;
  content: ArtAuditContentConfig;
}

export interface ArtDetailFileConfig {
  fields: string[];
  labels: Record<string, string>;
}

export interface ArtDetailScoreConfig {
  title: string;
  scoreLabel: string;
  gradeLabel: string;
  commentLabel: string;
  statusVisible: boolean;
  commentVisible: boolean;
  saveDraftVisible: boolean;
  fileListVisible: boolean;
  content: {
    basicSections: ArtScoreBasicSectionKey[];
    fileColumns: ArtScoreFileColumnKey[];
    recordColumns: ArtScoreRecordColumnKey[];
  };
}

export type ArtBrowseNavigatorPageKey = 'audit' | 'projectView' | 'review';
export type ArtBrowseNavigatorLayout = 'sidebar' | 'top';
export type ArtBrowseNavigatorFieldKey = 'activity' | 'category' | 'groupOrNature' | 'programForm' | 'school';

export interface ArtBrowseNavigatorActivityTitleConfig {
  displayMode: 'title' | 'hidden';
  template: string;
  fontSize: number;
  color: string;
  fontWeight: 'normal' | 'medium' | 'bold';
  align: 'left' | 'center' | 'right';
  switchText: string;
  allowWrap: boolean;
}

export interface ArtBrowseNavigatorPageConfig {
  layout: ArtBrowseNavigatorLayout;
  fields: ArtBrowseNavigatorFieldKey[];
  labels: Record<ArtBrowseNavigatorFieldKey, string>;
  activityTitle?: ArtBrowseNavigatorActivityTitleConfig;
}

export interface ArtBrowseNavigatorConfig {
  titleFontSize: number;
  itemFontSize: number;
  countFontSize: number;
  rowHeight: number;
  itemGap: number;
  panelPadding: number;
  backgroundColor: string;
  textColor: string;
  activeColor: string;
  hoverBackground: string;
  hoverBackgroundOpacity: number;
  selectedBackground: string;
  selectedBackgroundOpacity: number;
  selectedTextColor: string;
  borderColor: string;
  backgroundOpacity: number;
  dividerVisible: boolean;
  dividerColor: string;
  dividerOpacity: number;
  dividerWidth: number;
  borderRadius: number;
  sidebarWidth: number;
  sidebarMaxHeight: number;
  topHeight: number;
  borderVisible: boolean;
  shadowVisible: boolean;
  pages: Record<ArtBrowseNavigatorPageKey, ArtBrowseNavigatorPageConfig>;
}

export interface ArtAuditProgressConfig {
  visible: boolean;
  title: string;
  template: string;
  height: number;
  width: number;
  activeColor: string;
  completeColor: string;
  trackColor: string;
  textColor: string;
  fontSize: number;
}

export type ArtWorkspaceHeaderPageKey = 'project' | 'audit' | 'review' | 'schoolSubmit' | 'projectView';
export type ArtWorkspaceHeaderItemKey =
  | 'title'
  | 'navigatorToggle'
  | 'filters'
  | 'reviewScope'
  | 'categoryFilter'
  | 'groupFilter'
  | 'schoolFilter'
  | 'progress'
  | 'status'
  | 'search'
  | 'actions'
  | 'columnWidthReset'
  | 'selectAll'
  | 'unifiedSubmit'
  | 'navigationButton'
  | 'home'
  | 'compactGroup'
  | 'fixedSpacer'
  | 'autoSpacer';
export type ArtWorkspaceHeaderStatusAlign = 'left' | 'center' | 'right';
export type ArtWorkspaceHeaderItemAlign = 'left' | 'center' | 'right';
export type ArtWorkspaceHeaderWidthMode = 'fixed' | 'auto';
export type ArtWorkspaceHeaderCompactCollapseMode = 'none' | 'overflow' | 'all';
export type ArtWorkspaceHeaderSpacerKey = 'fixedSpacer' | 'autoSpacer';
export type ArtWorkspaceHeaderRepeatableKey = ArtWorkspaceHeaderSpacerKey | 'compactGroup';

export interface ArtWorkspaceHeaderButtonAppearanceConfig {
  backgroundColor: string;
  backgroundOpacity: number;
  textColor: string;
  borderVisible: boolean;
  borderColor: string;
  borderOpacity: number;
  borderWidth: number;
  borderRadius: number;
  hoverBackgroundColor: string;
  hoverBackgroundOpacity: number;
  hoverTextColor: string;
  hoverBorderColor: string;
  hoverBorderOpacity: number;
}

export interface ArtWorkspaceHeaderButtonConfig {
  text: string;
  alternateText: string;
  tooltip: string;
  alternateTooltip: string;
  align: ArtWorkspaceHeaderItemAlign;
  iconVisible?: boolean;
  variant?: 'default' | 'plain' | 'text';
  showCount?: boolean;
  appearance?: ArtWorkspaceHeaderButtonAppearanceConfig;
}

export interface ArtWorkspaceHeaderNavigationButtonConfig extends ArtWorkspaceHeaderButtonConfig {
  targetMode: 'preset' | 'custom';
  presetKey: string;
  customUrl: string;
  openMode: 'current' | 'new';
}

export interface ArtWorkspaceHeaderButtonStyleConfig {
  fontSize: number;
  fontWeight: number;
  iconSize: number;
  height: number;
}

export interface ArtWorkspaceHeaderItemConfig {
  align: ArtWorkspaceHeaderItemAlign;
  widthMode: ArtWorkspaceHeaderWidthMode;
  span: number;
  texts: Record<string, string>;
}

export interface ArtWorkspaceHeaderSpacerInstanceConfig {
  type: ArtWorkspaceHeaderSpacerKey;
  span: number;
  allowStatusBorrow: boolean;
}

export interface ArtWorkspaceHeaderCompactGroupInstanceConfig {
  widthMode: ArtWorkspaceHeaderWidthMode;
  span: number;
  align: ArtWorkspaceHeaderItemAlign;
  gap: number;
  items: ArtWorkspaceHeaderItemKey[];
  collapseMode: ArtWorkspaceHeaderCompactCollapseMode;
  pinnedItems: ArtWorkspaceHeaderItemKey[];
  collapseText: string;
}

export interface ArtWorkspaceHeaderCardConfig {
  key: string;
  rows: string[][];
  backgroundColor: string;
  backgroundOpacity: number;
  borderRadius: number;
  borderVisible: boolean;
}

export interface ArtWorkspaceHeaderPageConfig {
  titleTemplate: string;
  titleFontSize: number;
  titleFontWeight: number;
  titleColor: string;
  sidebarEnabled: boolean;
  sidebarDefaultExpanded: boolean;
  statusAlign: ArtWorkspaceHeaderStatusAlign;
  statusCollapseOnOverflow: boolean;
  statusItems: Array<{ key: string; visible: boolean; order: number }>;
  componentGap: number;
  rowMinHeight: number;
  paddingTop: number;
  paddingBottom: number;
  paddingInline: number;
  marginTop: number;
  marginBottom: number;
  navigatorToggleButton: ArtWorkspaceHeaderButtonConfig;
  columnWidthResetButton: ArtWorkspaceHeaderButtonConfig;
  selectAllButton: ArtWorkspaceHeaderButtonConfig;
  unifiedSubmitButton: ArtWorkspaceHeaderButtonConfig;
  navigationButton: ArtWorkspaceHeaderNavigationButtonConfig;
  homeButton: ArtWorkspaceHeaderButtonConfig;
  buttonStyle: ArtWorkspaceHeaderButtonStyleConfig;
  itemConfigs: Record<ArtWorkspaceHeaderItemKey, ArtWorkspaceHeaderItemConfig>;
  spacerInstances: Record<string, ArtWorkspaceHeaderSpacerInstanceConfig>;
  compactGroupInstances: Record<string, ArtWorkspaceHeaderCompactGroupInstanceConfig>;
  cards: ArtWorkspaceHeaderCardConfig[];
}

export interface ArtWorkspaceHeaderConfig {
  layoutVersion: number;
  managedActivityId?: string | number;
  pages: Record<ArtWorkspaceHeaderPageKey, ArtWorkspaceHeaderPageConfig>;
}

export type ArtListTableBorderMode = 'horizontal' | 'grid' | 'none';
export type ArtListTableWrapMode = 'nowrap' | 'wrap' | 'two-line';
export type ArtListStatusSemantic = 'draft' | 'pending' | 'approved' | 'returned' | 'unscored' | 'scored' | 'scoreDraft' | 'locked';
export type ArtListActionSemantic = 'view' | 'score' | 'edit' | 'return' | 'withdraw' | 'submit' | 'delete';

export interface ArtListStatusAppearanceConfig {
  borderVisible: boolean;
  iconVisible: boolean;
  uniformWidth: boolean;
  colors: Record<ArtListStatusSemantic, string>;
}

export interface ArtListActionAppearanceConfig {
  borderVisible: boolean;
  iconVisible: boolean;
  uniformWidth: boolean;
  disabledOpacity: number;
  colors: Record<ArtListActionSemantic, string>;
}

export interface ArtListTableAppearanceConfig {
  globalColorOverride: boolean;
  headerBackground: string;
  headerTextColor: string;
  textColor: string;
  hoverBackground: string;
  hoverBackgroundOpacity: number;
  borderColor: string;
  horizontalBorderVisible: boolean;
  horizontalBorderColor: string;
  horizontalBorderOpacity: number;
  verticalBorderVisible: boolean;
  verticalBorderColor: string;
  verticalBorderOpacity: number;
  outerBorderVisible: boolean;
  outerBorderColor: string;
  outerBorderOpacity: number;
  headerFontSize: number;
  bodyFontSize: number;
  buttonFontSize: number;
  rowHeight: number;
  borderRadius: number;
  borderMode: ArtListTableBorderMode;
  wrapMode: ArtListTableWrapMode;
  headerWrap: boolean;
  striped: boolean;
  actionStyle: 'link' | 'button';
  statusAppearance: ArtListStatusAppearanceConfig;
  rowActionAppearance: ArtListActionAppearanceConfig;
}

export type ArtReviewListColumnSource = 'builtin' | 'form';
export type ArtListColumnFixed = 'none' | 'left' | 'right';

export interface ArtReviewListColumnConfig {
  key: string;
  source: ArtReviewListColumnSource;
  fieldKey?: string;
  label: string;
  visible: boolean;
  deleted?: boolean;
  width: number;
  minWidth: number;
  fixed: ArtListColumnFixed | false;
}

export interface ArtReviewActivityTitleConfig {
  visible: boolean;
  template: string;
  fontSize: number;
  color: string;
  fontWeight: 'normal' | 'medium' | 'bold';
  align: 'left' | 'center';
  switchText: string;
  allowWrap: boolean;
}

export interface ArtReviewProgressConfig {
  visible: boolean;
  template: string;
  showPercentage: boolean;
  showRemaining: boolean;
  height: number;
  width: number;
  activeColor: string;
  successColor: string;
  signedColor: string;
  trackColor: string;
  textColor: string;
  fontSize: number;
}

export type ArtReviewScopeDisplayMode = 'buttons' | 'select';
export type ArtReviewScopeProgressPlacement = 'inside' | 'below';

export interface ArtReviewScopeNavigatorConfig {
  visible: boolean;
  displayMode: ArtReviewScopeDisplayMode;
  showCount: boolean;
  progressVisible: boolean;
  progressPlacement: ArtReviewScopeProgressPlacement;
}

export interface ArtReviewSignatureConfig {
  hintVisible: boolean;
  hintText: string;
  hintFontSize: number;
  hintFontWeight: 'normal' | 'medium' | 'bold';
  hintColor: string;
  hintBackgroundColor: string;
  hintBorderColor: string;
  buttonText: string;
  buttonFontSize: number;
  buttonFontWeight: 'normal' | 'medium' | 'bold';
  buttonTextColor: string;
}

export interface ArtReviewWorkbenchDisplayConfig {
  activityTitle: ArtReviewActivityTitleConfig;
  scopeNavigator: ArtReviewScopeNavigatorConfig;
  progress: ArtReviewProgressConfig;
  signature: ArtReviewSignatureConfig;
  columns: ArtReviewListColumnConfig[];
  categoryColumns: Record<string, ArtReviewListColumnConfig[]>;
}

export type ArtListTablePageKey =
  | 'project'
  | 'schoolSubmit'
  | 'audit'
  | 'review'
  | 'projectView'
  | 'reviewAssignment'
  | 'scoreSummary'
  | 'signedSheets';

export interface ArtListTablePageConfig {
  emptyText: string;
  selectionFixed: 'none' | 'left';
  serialVisible: boolean;
  serialWidth: number;
  serialFixed: 'none' | 'left';
  columns: ArtReviewListColumnConfig[];
  categoryColumns: Record<string, ArtReviewListColumnConfig[]>;
  statusLabels: Record<ArtListStatusSemantic, string>;
  actionLabels: Record<ArtListActionSemantic, string>;
}

export interface ArtListTableLayoutConfig {
  version: number;
  pages: Record<ArtListTablePageKey, ArtListTablePageConfig>;
}

export interface ArtListTableConfigVO {
  appearance: ArtListTableAppearanceConfig;
  layout: ArtListTableLayoutConfig;
}

export interface ArtDetailDisplayConfigVO {
  tabs: ArtDetailTabConfig;
  audit: ArtDetailAuditConfig;
  files: ArtDetailFileConfig;
  score: ArtDetailScoreConfig;
  navigator: ArtBrowseNavigatorConfig;
  auditProgress: ArtAuditProgressConfig;
  workspaceHeader: ArtWorkspaceHeaderConfig;
  listTableAppearance: ArtListTableAppearanceConfig;
  listTableLayout: ArtListTableLayoutConfig;
  reviewWorkbench: ArtReviewWorkbenchDisplayConfig;
}

export const getArtDetailDisplayConfig = (): AxiosPromise<ArtDetailDisplayConfigVO> =>
  request({
    url: '/crehn/detail-display-config',
    method: 'get'
  });

export const updateArtDetailDisplayConfig = (data: ArtDetailDisplayConfigVO) =>
  request({
    url: '/crehn/detail-display-config',
    method: 'put',
    data
  });

export const getArtListTableConfig = (): AxiosPromise<ArtListTableConfigVO> =>
  request({
    url: '/crehn/detail-display-config/table',
    method: 'get'
  });

export const updateArtListTableConfig = (data: ArtListTableConfigVO) =>
  request({
    url: '/crehn/detail-display-config/table',
    method: 'put',
    data
  });

export const listArtDetailDisplayCategoryFields = (categoryId: string | number): AxiosPromise<CategoryFieldSchemaVO[]> =>
  request({
    url: `/crehn/detail-display-config/category-fields/${categoryId}`,
    method: 'get'
  });
