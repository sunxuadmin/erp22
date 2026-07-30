import type { LayoutDisplayScaleMode } from '@/utils/layoutDisplayScale';

export interface WorkbenchComponentVO {
  componentKey: string;
  componentName: string;
  componentType: string;
  defaultTitle: string;
  defaultWidth: string;
  permission?: string;
  roleKeys: string[];
}

export interface WorkbenchLayoutVO {
  id?: string | number;
  roleId?: string | number;
  roleKey?: string;
  componentKey: string;
  componentName?: string;
  componentType?: string;
  title: string;
  width: string;
  sortOrder: number;
  visible: string;
  configJson?: string;
  permission?: string;
}

export interface WorkbenchRoleOptionVO {
  roleId: string | number;
  roleName: string;
  roleKey: string;
}

export interface WorkbenchAssetUploadVO {
  fileKey: string;
  url?: string;
  originalName?: string;
  fileName?: string;
  size?: number;
  contentType?: string;
}

export interface WorkbenchStyleConfig {
  enabled: boolean;
  preset: string;
  layoutScaleMode: LayoutDisplayScaleMode;
  customScalePercent: number;
  pageBackground: string;
  cardBackground: string;
  cardBorder: string;
  accentColor: string;
  headingColor: string;
  calendarBackground: string;
  shadow: string;
  sidebarBackground?: string;
  sidebarBackdropBlur?: number;
  sidebarItemBackground?: string;
  sidebarItemHoverBackground?: string;
  sidebarBorderColor?: string;
  sBg?: string;
  sBl?: number;
  sIt?: string;
  sHv?: string;
  sBd?: string;
}

export interface WorkbenchStyleSaveResult {
  apiVersion: number;
  config: WorkbenchStyleConfig;
}

export interface SecurityReminderConfig {
  enabled: boolean;
  checkDefaultPassword: boolean;
  checkMissingPhone: boolean;
  checkMissingEmail: boolean;
}

export interface WorkbenchRoleShellConfig {
  configured: boolean;
  navType: 'left' | 'mix' | 'top';
  theme: string;
  radiusBase: number;
  tagsView: boolean;
  tagsIcon: boolean;
  fixedHeader: boolean;
  sidebarLogo: boolean;
  dynamicTitle: boolean;
  breadcrumbVisible: boolean;
  hideHomeBreadcrumb: boolean;
  hideHomeTagsView: boolean;
  navbarTitleVisible: boolean;
  navbarTitle: string;
  navbarTitleAlign: 'left' | 'center' | 'right';
  navbarTitleFontSize: number;
  navbarTitleColor: string;
  navbarTitleFontWeight: 'normal' | 'medium' | 'bold';
  navbarTitleFontFamily: 'system' | 'microsoft-yahei' | 'simhei' | 'simsun' | 'kaiti';
  navbarTitleLogoMode: 'hidden' | 'system' | 'asset';
  navbarTitleLogoAssetKey: string;
  navbarTitleLogoHeight: number;
  showUserNickname: boolean;
  showScreenfull: boolean;
  showSizeSelect: boolean;
  showUserAvatar: boolean;
  userDisplayMode: 'nickname' | 'username' | 'hidden';
  brandTitleMode: 'role' | 'custom' | 'activity' | 'hidden';
  brandTitleKeepVisibleOnCollapse: boolean;
  brandTitle: string;
  brandActivityId?: string | number;
}

export type WorkbenchConfigChangeType = 'add' | 'modify' | 'remove' | 'unchanged' | 'skipped';

export interface WorkbenchConfigImportDiffItem {
  scope: string;
  roleKey?: string;
  roleName?: string;
  path: string;
  changeType: WorkbenchConfigChangeType;
  currentValue?: string;
  importedValue?: string;
  importable: boolean;
}

export interface WorkbenchConfigImportPreviewVO {
  packageVersion: string;
  exportedAt?: string;
  changedCount: number;
  unchangedCount: number;
  skippedCount: number;
  differences: WorkbenchConfigImportDiffItem[];
}
