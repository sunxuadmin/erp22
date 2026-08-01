export type DashboardRoleType = 'school' | 'admin' | 'auditor' | 'reviewer' | 'viewer' | 'generic';
export type DashboardSourceKind = 'metric' | 'series' | 'list';
export type DashboardBlockType = 'metric' | 'progress' | 'donut' | 'bar' | 'line' | 'ranking' | 'list' | 'action';
export type DashboardActionType = 'none' | 'school_create' | 'school_submit' | 'school_projects' | 'audit_list' | 'review_list' | 'project_overview';
export type DashboardFrameMode = 'default' | 'custom' | 'transparent';

export interface DashboardSourceOption {
  key: string;
  label: string;
  kind: DashboardSourceKind;
  description: string;
  format?: 'number' | 'percent';
}

export interface DashboardActionOption {
  value: DashboardActionType;
  label: string;
  description: string;
}

export interface DashboardActionConfig {
  type: DashboardActionType;
  label?: string;
  activityId?: string | number;
  categoryId?: string | number;
  categoryName?: string;
  status?: string;
  scoreStatus?: string;
  lockCategory?: boolean;
}

export interface DashboardHeaderActionConfig {
  visible: boolean;
  label: string;
  path: string;
}

export interface DashboardBlockConfig {
  id: string;
  type: DashboardBlockType;
  title: string;
  subtitle?: string;
  sourceKey?: string;
  visible: boolean;
  span: number;
  height: number;
  color?: string;
  backgroundColor?: string;
  unit?: string;
  decimals?: number;
  showLegend?: boolean;
  showLabels?: boolean;
  limit?: number;
  action: DashboardActionConfig;
}

export type DashboardWarningRuleKey = 'quota_usage_rate' | 'returned_count' | 'draft_count';

export interface DashboardWarningRule {
  key: DashboardWarningRuleKey;
  label: string;
  enabled: boolean;
  threshold: number;
  severity: 'warning' | 'danger' | 'info';
}

export interface UniversalDashboardTheme {
  preset: string;
  accentColor: string;
  backgroundColor: string;
  surfaceColor: string;
  textColor: string;
  mutedColor: string;
  borderColor: string;
  chartColors: string[];
}

export interface UniversalDashboardConfig {
  version: number;
  roleType: DashboardRoleType;
  showHeader: boolean;
  headerKicker: string;
  headerAction: DashboardHeaderActionConfig;
  subtitle: string;
  refreshLabel: string;
  emptyChartText: string;
  emptyRankingText: string;
  emptyListText: string;
  listActionFallbackLabel: string;
  actionFallbackLabel: string;
  actionFallbackDescription: string;
  columns: number;
  gap: number;
  radius: number;
  frameMode: DashboardFrameMode;
  frameBackground: string;
  frameBorderVisible: boolean;
  frameShadowVisible: boolean;
  hoverLiftEnabled: boolean;
  hoverBorderEnabled: boolean;
  theme: UniversalDashboardTheme;
  warningRules: DashboardWarningRule[];
  blocks: DashboardBlockConfig[];
}

export interface DashboardSeriesItem {
  name: string;
  value: number;
  extra?: string;
}

export interface DashboardListItem {
  id: string;
  projectId?: string | number;
  title: string;
  subtitle?: string;
  badge?: string;
  time?: string;
  tone?: 'warning' | 'danger' | 'info' | 'success';
}

export interface UniversalDashboardData {
  metrics: Record<string, number>;
  series: Record<string, DashboardSeriesItem[]>;
  lists: Record<string, DashboardListItem[]>;
}

export const UNIVERSAL_DASHBOARD_COMPONENT_KEY = 'universal_dashboard';
export const UNIVERSAL_DASHBOARD_CONFIG_MAX_LENGTH = 48000;

const themePresets: Array<{ label: string; value: UniversalDashboardTheme }> = [
  {
    label: '商务蓝',
    value: {
      preset: 'businessBlue',
      accentColor: '#2563eb',
      backgroundColor: '#ffffff',
      surfaceColor: '#f8fafc',
      textColor: '#0f172a',
      mutedColor: '#64748b',
      borderColor: '#dbe5f0',
      chartColors: ['#2563eb', '#0891b2', '#16a34a', '#d97706', '#7c3aed', '#dc2626']
    }
  },
  {
    label: '沉稳靛青',
    value: {
      preset: 'steadyIndigo',
      accentColor: '#4f46e5',
      backgroundColor: '#ffffff',
      surfaceColor: '#f8f9ff',
      textColor: '#172554',
      mutedColor: '#64748b',
      borderColor: '#d9def8',
      chartColors: ['#4f46e5', '#2563eb', '#0891b2', '#0d9488', '#d97706', '#dc2626']
    }
  },
  {
    label: '清雅青绿',
    value: {
      preset: 'calmTeal',
      accentColor: '#0f766e',
      backgroundColor: '#ffffff',
      surfaceColor: '#f6fbfa',
      textColor: '#123c3a',
      mutedColor: '#5f7473',
      borderColor: '#d5e8e5',
      chartColors: ['#0f766e', '#0891b2', '#2563eb', '#65a30d', '#d97706', '#dc2626']
    }
  },
  {
    label: '专业灰蓝',
    value: {
      preset: 'slateBlue',
      accentColor: '#475569',
      backgroundColor: '#ffffff',
      surfaceColor: '#f8fafc',
      textColor: '#0f172a',
      mutedColor: '#64748b',
      borderColor: '#d8e0e8',
      chartColors: ['#475569', '#2563eb', '#0891b2', '#0d9488', '#d97706', '#b91c1c']
    }
  }
];

export const universalDashboardThemePresets = themePresets.map((item) => ({ label: item.label, value: clone(item.value) }));

export const universalDashboardHeaderActionPathOptions = [
  { value: '/crehn/project/submit', label: '统一上报' },
  { value: '/crehn/project', label: '项目列表' },
  { value: '/crehn/project-view', label: '项目总览' }
];

const commonActions: DashboardActionOption[] = [{ value: 'none', label: '无跳转', description: '仅展示数据，不响应点击' }];

const roleSourceOptions: Record<DashboardRoleType, DashboardSourceOption[]> = {
  school: [
    { key: 'school.total_count', label: '本校项目总数', kind: 'metric', description: '当前学校可见项目总数' },
    { key: 'school.draft_count', label: '草稿项目', kind: 'metric', description: '当前学校草稿数量' },
    { key: 'school.submitted_count', label: '已提交项目', kind: 'metric', description: '当前学校已提交数量' },
    { key: 'school.completed_count', label: '已完成项目', kind: 'metric', description: '审核通过等已完成数量' },
    { key: 'school.returned_count', label: '退回待处理', kind: 'metric', description: '被审核退回、需要学校处理的项目数' },
    { key: 'school.published_result_count', label: '已公布结果', kind: 'metric', description: '当前学校已经发布的结果数量' },
    { key: 'school.quota_limit', label: '总名额', kind: 'metric', description: '当前活动可用总名额' },
    { key: 'school.remaining_count', label: '剩余名额', kind: 'metric', description: '当前活动剩余名额' },
    { key: 'school.completion_rate', label: '填报完成率', kind: 'metric', description: '已完成数占项目总数比例', format: 'percent' },
    { key: 'school.quota_usage_rate', label: '名额使用率', kind: 'metric', description: '已使用名额占总名额比例', format: 'percent' },
    { key: 'school.status_distribution', label: '项目状态分布', kind: 'series', description: '草稿、已提交和已完成数量' },
    { key: 'school.category_distribution', label: '类别填报分布', kind: 'series', description: '各类别填报数量' },
    { key: 'school.category_progress', label: '类别名额使用率', kind: 'series', description: '各类别已用名额比例', format: 'percent' },
    { key: 'school.alerts', label: '填报预警', kind: 'list', description: '按后台阈值计算的名额、退回和草稿预警' },
    { key: 'school.recent_edited', label: '最近编辑', kind: 'list', description: '本校最近修改的项目' },
    { key: 'school.recent_submitted', label: '最近提交', kind: 'list', description: '本校最近提交的项目' },
    { key: 'school.recent_passed', label: '最近通过', kind: 'list', description: '本校最近审核通过的项目' },
    { key: 'school.recent_results', label: '最新已公布结果', kind: 'list', description: '仅显示已发布且学校可见的结果字段' }
  ],
  admin: [
    { key: 'admin.school_total', label: '学校总数', kind: 'metric', description: '系统可见学校总数' },
    { key: 'admin.submitted_schools', label: '已报送学校', kind: 'metric', description: '已有项目的学校数' },
    { key: 'admin.unsubmitted_schools', label: '未报送学校', kind: 'metric', description: '尚未报送的学校数' },
    { key: 'admin.project_total', label: '项目总数', kind: 'metric', description: '当前数据范围内项目总数' },
    { key: 'admin.pending_audit', label: '待审核项目', kind: 'metric', description: '状态为待审核的项目数' },
    { key: 'admin.audit_passed', label: '已通过项目', kind: 'metric', description: '审核通过项目数' },
    { key: 'admin.returned', label: '已退回项目', kind: 'metric', description: '审核退回项目数' },
    { key: 'admin.participation_rate', label: '学校参与率', kind: 'metric', description: '已报送学校占学校总数比例', format: 'percent' },
    { key: 'admin.status_distribution', label: '项目状态分布', kind: 'series', description: '各项目状态数量及占比' },
    { key: 'admin.category_distribution', label: '类别项目分布', kind: 'series', description: '各小类别项目数量' },
    { key: 'admin.group_distribution', label: '大类项目分布', kind: 'series', description: '各艺术大类项目数量' },
    { key: 'admin.school_ranking', label: '学校报送排行', kind: 'series', description: '学校报送项目数量排行' }
  ],
  auditor: [
    { key: 'audit.assigned_total', label: '分配项目', kind: 'metric', description: '当前审核员可见项目总数' },
    { key: 'audit.pending_count', label: '待审核', kind: 'metric', description: '当前审核员待审核项目数' },
    { key: 'audit.audited_count', label: '已审核', kind: 'metric', description: '通过和退回项目合计' },
    { key: 'audit.pass_count', label: '审核通过', kind: 'metric', description: '当前数据范围内通过项目数' },
    { key: 'audit.return_count', label: '审核退回', kind: 'metric', description: '当前数据范围内退回项目数' },
    { key: 'audit.pass_rate', label: '审核通过率', kind: 'metric', description: '通过数占已审核数比例', format: 'percent' },
    { key: 'audit.my_total', label: '本人审核总数', kind: 'metric', description: '本人执行通过或退回的记录数' },
    { key: 'audit.status_distribution', label: '审核状态分布', kind: 'series', description: '待审核、通过和退回数量' },
    { key: 'audit.group_distribution', label: '本人审核大类分布', kind: 'series', description: '本人各大类审核记录数量' },
    { key: 'audit.group_pass_rate', label: '各大类通过率', kind: 'series', description: '本人各大类审核通过比例', format: 'percent' }
  ],
  reviewer: [
    { key: 'review.assigned_total', label: '分配任务', kind: 'metric', description: '当前评审员分配任务总数' },
    { key: 'review.pending_score', label: '待评分', kind: 'metric', description: '尚未保存评分的任务数' },
    { key: 'review.draft_score', label: '评分草稿', kind: 'metric', description: '已保存草稿的任务数' },
    { key: 'review.submitted_score', label: '已提交评分', kind: 'metric', description: '已经提交的评分数' },
    { key: 'review.locked_by_other', label: '已锁定', kind: 'metric', description: '当前被其他会话锁定的任务数' },
    { key: 'review.completion_rate', label: '评分完成率', kind: 'metric', description: '已提交评分占分配任务比例', format: 'percent' },
    { key: 'review.status_distribution', label: '评分状态分布', kind: 'series', description: '待评分、草稿、已提交和锁定数量' },
    { key: 'review.group_distribution', label: '评审大类分布', kind: 'series', description: '各大类分配任务数量' },
    { key: 'review.group_completion', label: '各大类完成率', kind: 'series', description: '各大类评分提交比例', format: 'percent' }
  ],
  viewer: [
    { key: 'viewer.project_total', label: '项目总数', kind: 'metric', description: '当前查看范围项目总数' },
    { key: 'viewer.draft_count', label: '未提交', kind: 'metric', description: '当前查看范围草稿数' },
    { key: 'viewer.pending_count', label: '待审核', kind: 'metric', description: '当前查看范围待审核数' },
    { key: 'viewer.audited_count', label: '已审核', kind: 'metric', description: '当前查看范围已审核数' },
    { key: 'viewer.returned_count', label: '已退回', kind: 'metric', description: '当前查看范围退回数' },
    { key: 'viewer.status_distribution', label: '项目状态分布', kind: 'series', description: '当前查看范围状态分布' },
    { key: 'viewer.category_distribution', label: '类别项目分布', kind: 'series', description: '当前查看范围类别分布' },
    { key: 'viewer.group_distribution', label: '大类项目分布', kind: 'series', description: '当前查看范围大类分布' }
  ],
  generic: []
};

const roleActionOptions: Record<DashboardRoleType, DashboardActionOption[]> = {
  school: [
    ...commonActions,
    { value: 'school_create', label: '指定类别直接填报', description: '进入项目填报并锁定所选活动和类别' },
    { value: 'school_submit', label: '统一上报', description: '进入学校统一上报页面' },
    { value: 'school_projects', label: '查看本校项目', description: '进入本校项目列表' }
  ],
  admin: [
    ...commonActions,
    { value: 'audit_list', label: '进入审核列表', description: '进入审核页面并携带状态筛选' },
    { value: 'project_overview', label: '进入项目总览', description: '进入项目查看总览' }
  ],
  auditor: [...commonActions, { value: 'audit_list', label: '进入审核列表', description: '进入本人有权访问的审核列表' }],
  reviewer: [...commonActions, { value: 'review_list', label: '进入评分列表', description: '进入本人评分任务列表' }],
  viewer: [...commonActions, { value: 'project_overview', label: '进入项目总览', description: '进入只读项目总览' }],
  generic: commonActions
};

const blockTypeOptions: Array<{ value: DashboardBlockType; label: string; sourceKind?: DashboardSourceKind }> = [
  { value: 'metric', label: '数字指标', sourceKind: 'metric' },
  { value: 'progress', label: '百分比/进度', sourceKind: 'metric' },
  { value: 'donut', label: '环形图', sourceKind: 'series' },
  { value: 'bar', label: '柱状图', sourceKind: 'series' },
  { value: 'line', label: '折线图', sourceKind: 'series' },
  { value: 'ranking', label: '排行榜', sourceKind: 'series' },
  { value: 'list', label: '动态列表', sourceKind: 'list' },
  { value: 'action', label: '快捷按钮' }
];

export const universalDashboardBlockTypeOptions = blockTypeOptions.map((item) => ({ ...item }));

const block = (
  id: string,
  type: DashboardBlockType,
  title: string,
  sourceKey: string | undefined,
  span: number,
  height: number,
  color?: string,
  action?: DashboardActionConfig
): DashboardBlockConfig => ({
  id,
  type,
  title,
  sourceKey,
  visible: true,
  span,
  height,
  color,
  decimals: 0,
  showLegend: true,
  showLabels: true,
  limit: 8,
  action: action || { type: 'none' }
});

const defaultBlocks: Record<DashboardRoleType, DashboardBlockConfig[]> = {
  school: [
    block('school-alerts', 'list', '填报预警', 'school.alerts', 4, 280, '#d97706', { type: 'school_projects', label: '处理项目' }),
    block('school-progress', 'progress', '填报完成率', 'school.completion_rate', 3, 280, '#0f766e'),
    block('school-category', 'bar', '类别填报进度', 'school.category_progress', 5, 280),
    block('school-edited', 'list', '最近编辑', 'school.recent_edited', 4, 290, '#2563eb', { type: 'school_submit', label: '统一上报' }),
    block('school-submitted', 'list', '最近提交', 'school.recent_submitted', 4, 290, '#0891b2', { type: 'school_submit', label: '统一上报' }),
    block('school-passed', 'list', '最近通过', 'school.recent_passed', 4, 290, '#16a34a', { type: 'school_submit', label: '统一上报' }),
    block('school-results', 'list', '最新已公布结果', 'school.recent_results', 6, 300, '#7c3aed')
  ],
  admin: [
    block('admin-school-total', 'metric', '学校总数', 'admin.school_total', 2, 126, '#2563eb'),
    block('admin-submitted-schools', 'metric', '已报送学校', 'admin.submitted_schools', 2, 126, '#0891b2'),
    block('admin-unsubmitted-schools', 'metric', '未报送学校', 'admin.unsubmitted_schools', 2, 126, '#d97706'),
    block('admin-project-total', 'metric', '项目总数', 'admin.project_total', 2, 126, '#0f766e'),
    block('admin-pending', 'metric', '待审核项目', 'admin.pending_audit', 2, 126, '#4f46e5', {
      type: 'audit_list',
      label: '进入待审核',
      status: 'submitted'
    }),
    block('admin-passed', 'metric', '已通过项目', 'admin.audit_passed', 2, 126, '#16a34a'),
    block('admin-status', 'donut', '项目状态分布', 'admin.status_distribution', 4, 310),
    block('admin-category', 'bar', '类别项目分布', 'admin.category_distribution', 4, 310),
    block('admin-school-ranking', 'ranking', '学校报送排行', 'admin.school_ranking', 4, 310),
    block('admin-group-trend', 'line', '大类项目对比', 'admin.group_distribution', 6, 300),
    block('admin-participation', 'progress', '学校参与率', 'admin.participation_rate', 2, 240, '#0891b2'),
    block('admin-action', 'action', '进入项目总览', undefined, 4, 160, '#2563eb', { type: 'project_overview', label: '查看全部项目' })
  ],
  auditor: [
    block('audit-total', 'metric', '分配项目', 'audit.assigned_total', 3, 126, '#2563eb'),
    block('audit-pending', 'metric', '待审核', 'audit.pending_count', 3, 126, '#d97706', {
      type: 'audit_list',
      label: '进入待审核',
      status: 'submitted'
    }),
    block('audit-audited', 'metric', '已审核', 'audit.audited_count', 3, 126, '#0891b2'),
    block('audit-pass-rate', 'metric', '通过率', 'audit.pass_rate', 3, 126, '#16a34a'),
    block('audit-status', 'donut', '审核状态分布', 'audit.status_distribution', 4, 300),
    block('audit-group', 'bar', '本人审核大类分布', 'audit.group_distribution', 4, 300),
    block('audit-group-rate', 'line', '各大类通过率', 'audit.group_pass_rate', 4, 300),
    block('audit-action', 'action', '审核工作入口', undefined, 12, 160, '#4f46e5', {
      type: 'audit_list',
      label: '进入审核工作台',
      status: 'submitted'
    })
  ],
  reviewer: [
    block('review-total', 'metric', '分配任务', 'review.assigned_total', 3, 126, '#2563eb'),
    block('review-pending', 'metric', '待评分', 'review.pending_score', 3, 126, '#d97706', {
      type: 'review_list',
      label: '进入待评分',
      scoreStatus: 'none'
    }),
    block('review-draft', 'metric', '评分草稿', 'review.draft_score', 3, 126, '#0891b2'),
    block('review-submitted', 'metric', '已提交', 'review.submitted_score', 3, 126, '#16a34a'),
    block('review-status', 'donut', '评分状态分布', 'review.status_distribution', 4, 300),
    block('review-group', 'bar', '评审大类分布', 'review.group_distribution', 4, 300),
    block('review-progress', 'line', '各大类完成率', 'review.group_completion', 4, 300),
    block('review-action', 'action', '评分工作入口', undefined, 12, 160, '#0f766e', { type: 'review_list', label: '进入评分工作台' })
  ],
  viewer: [
    block('viewer-total', 'metric', '项目总数', 'viewer.project_total', 3, 126, '#475569'),
    block('viewer-draft', 'metric', '未提交', 'viewer.draft_count', 3, 126, '#64748b'),
    block('viewer-pending', 'metric', '待审核', 'viewer.pending_count', 3, 126, '#d97706'),
    block('viewer-audited', 'metric', '已审核', 'viewer.audited_count', 3, 126, '#16a34a'),
    block('viewer-status', 'donut', '项目状态分布', 'viewer.status_distribution', 4, 300),
    block('viewer-category', 'bar', '类别项目分布', 'viewer.category_distribution', 8, 300),
    block('viewer-group', 'line', '大类项目对比', 'viewer.group_distribution', 8, 280),
    block('viewer-action', 'action', '项目查看入口', undefined, 4, 160, '#475569', { type: 'project_overview', label: '进入项目总览' })
  ],
  generic: []
};

export const resolveDashboardRole = (roleKeys?: string | string[], schoolId?: string | number): DashboardRoleType => {
  const values = (Array.isArray(roleKeys) ? roleKeys : [roleKeys || ''])
    .map((item) =>
      String(item || '')
        .trim()
        .toLowerCase()
    )
    .filter(Boolean);
  if (values.some((item) => item.includes('school'))) return 'school';
  if (values.some((item) => item.includes('audit_supervisor') || item.includes('score_summary') || item.includes('result_admin'))) return 'viewer';
  if (values.some((item) => item.includes('admin') || item === 'superadmin' || item.includes('ops'))) return 'admin';
  if (values.some((item) => item.includes('reviewer') || item.includes('expert'))) return 'reviewer';
  if (values.some((item) => item.includes('auditor'))) return 'auditor';
  if (values.some((item) => item.includes('project_viewer') || item.includes('project-viewer'))) return 'viewer';
  if (values.some((item) => item.includes('participant') || item.includes('cms_editor') || item.includes('cms_publisher'))) return 'generic';
  return values.length === 0 && schoolId ? 'school' : 'generic';
};

const defaultThemeForRole = (role: DashboardRoleType) => {
  const preset =
    role === 'school' || role === 'reviewer' ? 'calmTeal' : role === 'auditor' ? 'steadyIndigo' : role === 'viewer' ? 'slateBlue' : 'businessBlue';
  return clone(themePresets.find((item) => item.value.preset === preset)?.value || themePresets[0].value);
};

export const dashboardSourceOptions = (role: DashboardRoleType, kind?: DashboardSourceKind) => {
  const rows = roleSourceOptions[role] || [];
  return rows.filter((item) => !kind || item.kind === kind).map((item) => ({ ...item }));
};

export const dashboardActionOptions = (role: DashboardRoleType) => (roleActionOptions[role] || commonActions).map((item) => ({ ...item }));

export const defaultUniversalDashboardConfig = (role: DashboardRoleType): UniversalDashboardConfig => {
  const theme = defaultThemeForRole(role);
  return {
    version: 2,
    roleType: role,
    showHeader: true,
    headerKicker: 'DATA WORKBENCH',
    headerAction: { visible: role === 'school', label: '统一上报', path: '/crehn/project/submit' },
    subtitle:
      role === 'school'
        ? '本校填报进度、名额和类别概览'
        : role === 'auditor'
          ? '仅展示当前审核员有权访问的项目和本人审核记录'
          : role === 'reviewer'
            ? '仅展示当前评审员已分配的评分任务'
            : role === 'viewer'
              ? '只读项目数据概览'
              : role === 'generic'
                ? '当前角色暂未配置数据组件'
                : '学校报送、项目审核和类别分布概览',
    refreshLabel: '刷新',
    emptyChartText: '暂无统计数据',
    emptyRankingText: '暂无排行数据',
    emptyListText: '暂无动态数据',
    listActionFallbackLabel: '进入',
    actionFallbackLabel: '立即进入',
    actionFallbackDescription: '进入相关业务页面',
    columns: 12,
    gap: 12,
    radius: 10,
    frameMode: 'default',
    frameBackground: theme.backgroundColor,
    frameBorderVisible: true,
    frameShadowVisible: false,
    hoverLiftEnabled: true,
    hoverBorderEnabled: true,
    theme,
    warningRules:
      role === 'school'
        ? [
            { key: 'quota_usage_rate', label: '名额使用接近上限', enabled: true, threshold: 80, severity: 'warning' },
            { key: 'returned_count', label: '存在退回待处理项目', enabled: true, threshold: 1, severity: 'danger' },
            { key: 'draft_count', label: '存在待提交草稿', enabled: true, threshold: 1, severity: 'info' }
          ]
        : [],
    blocks: clone(defaultBlocks[role])
  };
};

const allowedBlockTypes = new Set(blockTypeOptions.map((item) => item.value));
const allowedActionTypes = new Set<DashboardActionType>([
  'none',
  'school_create',
  'school_submit',
  'school_projects',
  'audit_list',
  'review_list',
  'project_overview'
]);

const clamp = (value: unknown, fallback: number, min: number, max: number) => {
  const numeric = Number(value);
  return Number.isFinite(numeric) ? Math.min(max, Math.max(min, numeric)) : fallback;
};

const text = (value: unknown, fallback = '', maxLength = 80) =>
  String(value ?? fallback)
    .trim()
    .slice(0, maxLength);

const color = (value: unknown, fallback: string) => {
  const normalized = String(value || '').trim();
  return /^(#[0-9a-f]{3,8}|rgba?\([\d\s.,%]+\)|hsla?\([\d\s.,%]+\)|transparent)$/i.test(normalized) ? normalized : fallback;
};

const defaultWarningRules = () => defaultUniversalDashboardConfig('school').warningRules;
const normalizeWarningRules = (value: DashboardWarningRule[] | undefined, role: DashboardRoleType) => {
  if (role !== 'school') return [];
  const allowed = new Set<DashboardWarningRuleKey>(['quota_usage_rate', 'returned_count', 'draft_count']);
  const byKey = new Map((Array.isArray(value) ? value : defaultWarningRules()).map((item) => [item.key, item]));
  return defaultWarningRules()
    .map((fallback) => {
      const item = byKey.get(fallback.key);
      return {
        key: fallback.key,
        label: text(item?.label, fallback.label, 40),
        enabled: item?.enabled !== false,
        threshold: clamp(
          item?.threshold,
          fallback.threshold,
          fallback.key === 'quota_usage_rate' ? 1 : 1,
          fallback.key === 'quota_usage_rate' ? 100 : 9999
        ),
        severity: ['warning', 'danger', 'info'].includes(String(item?.severity || '')) ? item!.severity : fallback.severity
      } as DashboardWarningRule;
    })
    .filter((item) => allowed.has(item.key));
};

const normalizeTheme = (value: Partial<UniversalDashboardTheme> | undefined, fallback: UniversalDashboardTheme): UniversalDashboardTheme => ({
  preset: text(value?.preset, fallback.preset, 32),
  accentColor: color(value?.accentColor, fallback.accentColor),
  backgroundColor: color(value?.backgroundColor, fallback.backgroundColor),
  surfaceColor: color(value?.surfaceColor, fallback.surfaceColor),
  textColor: color(value?.textColor, fallback.textColor),
  mutedColor: color(value?.mutedColor, fallback.mutedColor),
  borderColor: color(value?.borderColor, fallback.borderColor),
  chartColors: (Array.isArray(value?.chartColors) ? value?.chartColors : fallback.chartColors)
    .slice(0, 10)
    .map((item, index) => color(item, fallback.chartColors[index % fallback.chartColors.length]))
});

const normalizeHeaderAction = (
  value: Partial<DashboardHeaderActionConfig> | undefined,
  fallback: DashboardHeaderActionConfig
): DashboardHeaderActionConfig => ({
  visible: value?.visible ?? fallback.visible,
  label: text(value?.label, fallback.label, 30) || fallback.label,
  path: universalDashboardHeaderActionPathOptions.some((item) => item.value === value?.path) ? String(value?.path) : fallback.path
});

const normalizeAction = (value: DashboardActionConfig | undefined, role: DashboardRoleType): DashboardActionConfig => {
  const roleAllowed = new Set(dashboardActionOptions(role).map((item) => item.value));
  const type = allowedActionTypes.has(value?.type || 'none') && roleAllowed.has(value?.type || 'none') ? value?.type || 'none' : 'none';
  const activityId = typeof value?.activityId === 'string' || typeof value?.activityId === 'number' ? value.activityId : undefined;
  const categoryId = typeof value?.categoryId === 'string' || typeof value?.categoryId === 'number' ? value.categoryId : undefined;
  const status = ['submitted', 'audit_passed', 'returned'].includes(String(value?.status || '')) ? String(value?.status) : '';
  const scoreStatus = ['none', 'draft', 'submitted'].includes(String(value?.scoreStatus || '')) ? String(value?.scoreStatus) : '';
  return {
    type,
    label: text(value?.label, '', 30),
    activityId,
    categoryId,
    categoryName: text(value?.categoryName, '', 60),
    status,
    scoreStatus,
    lockCategory: value?.lockCategory !== false
  };
};

const defaultActionDescription = (role: DashboardRoleType, type: DashboardActionType) =>
  dashboardActionOptions(role).find((item) => item.value === type)?.description || '';

const normalizeBlock = (value: Partial<DashboardBlockConfig>, role: DashboardRoleType, index: number, theme: UniversalDashboardTheme) => {
  const type = allowedBlockTypes.has(value.type as DashboardBlockType) ? (value.type as DashboardBlockType) : 'metric';
  const kind = blockTypeOptions.find((item) => item.value === type)?.sourceKind;
  const allowedSources = new Set(dashboardSourceOptions(role, kind).map((item) => item.key));
  const sourceKey =
    !kind || allowedSources.has(String(value.sourceKey || '')) ? text(value.sourceKey, '', 80) : dashboardSourceOptions(role, kind)[0]?.key;
  const isSchoolRecentList = role === 'school' && ['school-edited', 'school-submitted', 'school-passed'].includes(String(value.id));
  const actionValue = isSchoolRecentList ? { ...(value.action || {}), type: 'school_submit' as const, label: '统一上报' } : value.action;
  const action = normalizeAction(actionValue, role);
  return {
    id: text(value.id, `dashboard-block-${index + 1}`, 80) || `dashboard-block-${index + 1}`,
    type,
    title: text(value.title, blockTypeOptions.find((item) => item.value === type)?.label || '数据卡片', 60),
    subtitle: text(value.subtitle, type === 'action' ? defaultActionDescription(role, action.type) : '', 100),
    sourceKey: sourceKey || undefined,
    visible: value.visible !== false,
    span: clamp(value.span, type === 'metric' ? 3 : 4, 2, 12),
    height: clamp(value.height, type === 'metric' ? 126 : type === 'action' ? 160 : 300, 110, 520),
    color: color(value.color, theme.accentColor),
    backgroundColor: color(value.backgroundColor, theme.surfaceColor),
    unit: text(value.unit, '', 12),
    decimals: clamp(value.decimals, 0, 0, 2),
    showLegend: value.showLegend !== false,
    showLabels: value.showLabels !== false,
    limit: clamp(value.limit, 8, 3, 20),
    action
  } as DashboardBlockConfig;
};

export const normalizeUniversalDashboardConfig = (value: Partial<UniversalDashboardConfig> | undefined, fallbackRole: DashboardRoleType) => {
  const role = value?.roleType && ['school', 'admin', 'auditor', 'reviewer', 'viewer'].includes(value.roleType) ? value.roleType : fallbackRole;
  const defaults = defaultUniversalDashboardConfig(role);
  const theme = normalizeTheme(value?.theme, defaults.theme);
  const sourceBlocks = Array.isArray(value?.blocks) ? value?.blocks : defaults.blocks;
  return {
    version: 2,
    roleType: role,
    showHeader: value?.showHeader !== false,
    headerKicker: text(value?.headerKicker, defaults.headerKicker, 30),
    headerAction: normalizeHeaderAction(value?.headerAction, defaults.headerAction),
    subtitle: text(value?.subtitle, defaults.subtitle, 120),
    refreshLabel: text(value?.refreshLabel, defaults.refreshLabel, 20),
    emptyChartText: text(value?.emptyChartText, defaults.emptyChartText, 40),
    emptyRankingText: text(value?.emptyRankingText, defaults.emptyRankingText, 40),
    emptyListText: text(value?.emptyListText, defaults.emptyListText, 40),
    listActionFallbackLabel: text(value?.listActionFallbackLabel, defaults.listActionFallbackLabel, 20),
    actionFallbackLabel: text(value?.actionFallbackLabel, defaults.actionFallbackLabel, 20),
    actionFallbackDescription: text(value?.actionFallbackDescription, defaults.actionFallbackDescription, 60),
    columns: clamp(value?.columns, defaults.columns, 4, 12),
    gap: clamp(value?.gap, defaults.gap, 4, 28),
    radius: clamp(value?.radius, defaults.radius, 0, 24),
    frameMode: ['default', 'custom', 'transparent'].includes(String(value?.frameMode || ''))
      ? (value?.frameMode as DashboardFrameMode)
      : defaults.frameMode,
    frameBackground: color(value?.frameBackground, defaults.frameBackground),
    frameBorderVisible: value?.frameBorderVisible !== false,
    frameShadowVisible: value?.frameShadowVisible === true,
    hoverLiftEnabled: value?.hoverLiftEnabled !== false,
    hoverBorderEnabled: value?.hoverBorderEnabled !== false,
    theme,
    warningRules: normalizeWarningRules(value?.warningRules, role),
    blocks: sourceBlocks.slice(0, 40).map((item, index) => normalizeBlock(item, role, index, theme))
  } as UniversalDashboardConfig;
};

export const parseUniversalDashboardConfig = (raw: unknown, fallbackRole: DashboardRoleType, forceRole = false) => {
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw || '{}') : raw || {};
    const value = parsed as Partial<UniversalDashboardConfig>;
    return normalizeUniversalDashboardConfig(forceRole ? { ...value, roleType: fallbackRole } : value, fallbackRole);
  } catch {
    return defaultUniversalDashboardConfig(fallbackRole);
  }
};

export const applyUniversalDashboardThemePreset = (config: UniversalDashboardConfig, preset: string) => {
  const match = themePresets.find((item) => item.value.preset === preset);
  if (!match) return config;
  return normalizeUniversalDashboardConfig({ ...config, theme: clone(match.value) }, config.roleType);
};

export const createUniversalDashboardBlock = (role: DashboardRoleType, type: DashboardBlockType): DashboardBlockConfig => {
  const kind = blockTypeOptions.find((item) => item.value === type)?.sourceKind;
  const source = dashboardSourceOptions(role, kind)[0];
  return normalizeBlock(
    {
      id: `dashboard-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      type,
      title: type === 'action' ? '快捷入口' : source?.label || '数据卡片',
      sourceKey: source?.key,
      span: type === 'metric' ? 3 : type === 'action' ? 3 : 4,
      height: type === 'metric' ? 126 : type === 'action' ? 160 : 300,
      visible: true,
      action: { type: 'none' }
    },
    role,
    0,
    defaultThemeForRole(role)
  );
};

export const dashboardSourceFormat = (role: DashboardRoleType, sourceKey?: string) =>
  dashboardSourceOptions(role).find((item) => item.key === sourceKey)?.format || 'number';

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value));
}

export const cloneUniversalDashboardConfig = (value: UniversalDashboardConfig) => clone(value);
