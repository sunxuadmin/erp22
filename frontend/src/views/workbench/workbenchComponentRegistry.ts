export const WORKBENCH_COMPONENT_KEYS = {
  schoolStageNotice: 'school_stage_notice',
  adminStageNotice: 'admin_stage_notice',
  auditStageNotice: 'audit_stage_notice',
  reviewStageNotice: 'review_stage_notice',
  schoolSubmission: 'school_submission',
  schoolDashboard: 'school_project_submit',
  schoolProjectList: 'school_project_list',
  auditOverview: 'audit_overview',
  auditPending: 'audit_pending',
  auditWorkbench: 'audit_workbench',
  reviewWorkbench: 'review_workbench',
  adminProjectSummary: 'admin_project_summary',
  adminUploadSummary: 'admin_upload_summary',
  projectUploadOverview: 'project_upload_overview',
  quotaRatioOverview: 'quota_ratio_overview',
  universalDashboard: 'universal_dashboard'
} as const;

export type WorkbenchComponentKey = (typeof WORKBENCH_COMPONENT_KEYS)[keyof typeof WORKBENCH_COMPONENT_KEYS];
export type WorkbenchLayoutWidth = '1/1' | '1/2' | '1/3' | '2/3';
export type WorkbenchComponentConfigKind = 'projectSubmit' | 'stageNotice' | 'auditWorkbench' | 'reviewWorkbench' | 'dashboard';
export type WorkbenchAccountType = 'school' | 'admin' | 'auditor' | 'expert';

export interface WorkbenchComponentDefinition {
  key: WorkbenchComponentKey;
  configKind?: WorkbenchComponentConfigKind;
  accountTypes?: readonly WorkbenchAccountType[];
  previewBlocks: number;
}

const keys = WORKBENCH_COMPONENT_KEYS;

export const WORKBENCH_COMPONENT_DEFINITIONS: readonly WorkbenchComponentDefinition[] = [
  { key: keys.schoolStageNotice, configKind: 'stageNotice', accountTypes: ['school'], previewBlocks: 3 },
  { key: keys.adminStageNotice, configKind: 'stageNotice', accountTypes: ['admin'], previewBlocks: 3 },
  { key: keys.auditStageNotice, configKind: 'stageNotice', accountTypes: ['auditor'], previewBlocks: 3 },
  { key: keys.reviewStageNotice, configKind: 'stageNotice', accountTypes: ['expert'], previewBlocks: 3 },
  { key: keys.schoolSubmission, accountTypes: ['school'], previewBlocks: 3 },
  { key: keys.schoolDashboard, configKind: 'dashboard', accountTypes: ['school'], previewBlocks: 4 },
  { key: keys.schoolProjectList, configKind: 'projectSubmit', accountTypes: ['school'], previewBlocks: 4 },
  { key: keys.auditOverview, accountTypes: ['auditor'], previewBlocks: 3 },
  { key: keys.auditPending, accountTypes: ['auditor'], previewBlocks: 3 },
  { key: keys.auditWorkbench, configKind: 'auditWorkbench', accountTypes: ['auditor'], previewBlocks: 4 },
  { key: keys.reviewWorkbench, configKind: 'reviewWorkbench', accountTypes: ['expert'], previewBlocks: 4 },
  { key: keys.adminProjectSummary, accountTypes: ['admin'], previewBlocks: 3 },
  { key: keys.adminUploadSummary, accountTypes: ['admin'], previewBlocks: 3 },
  { key: keys.projectUploadOverview, accountTypes: ['admin'], previewBlocks: 3 },
  { key: keys.quotaRatioOverview, accountTypes: ['admin'], previewBlocks: 3 },
  { key: keys.universalDashboard, configKind: 'dashboard', previewBlocks: 4 }
] as const;

const definitionMap = new Map(WORKBENCH_COMPONENT_DEFINITIONS.map((item) => [item.key, item]));

export const WORKBENCH_STAGE_NOTICE_COMPONENT_KEYS = new Set<string>([
  keys.schoolStageNotice,
  keys.adminStageNotice,
  keys.auditStageNotice,
  keys.reviewStageNotice
]);

export const WORKBENCH_SCHOOL_COMPONENT_KEYS = new Set<string>([
  keys.schoolStageNotice,
  keys.schoolSubmission,
  keys.schoolDashboard,
  keys.schoolProjectList
]);

export const workbenchComponentDefinition = (componentKey?: string) =>
  componentKey ? definitionMap.get(componentKey as WorkbenchComponentKey) : undefined;

export const workbenchComponentConfigKind = (componentKey?: string) => workbenchComponentDefinition(componentKey)?.configKind;

export const workbenchComponentPreviewBlocks = (componentKey?: string) => workbenchComponentDefinition(componentKey)?.previewBlocks || 3;

export const workbenchWidthLabel = (width?: string) =>
  ({ '1/1': '整行', '1/2': '半宽', '1/3': '三分之一', '2/3': '三分之二' })[String(width || '')] || width || '自动宽度';

export const workbenchWidthToken = (width?: string): WorkbenchLayoutWidth => (width === '1/2' || width === '1/3' || width === '2/3' ? width : '1/1');
