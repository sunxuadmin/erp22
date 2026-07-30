import type { ArtWorkspaceHeaderNavigationButtonConfig } from '@/api/crehn/detailDisplay';
import type { WorkbenchAccountType } from '@/views/workbench/workbenchComponentRegistry';

export type ArtWorkspaceNavigationPresetKey =
  | 'schoolSubmit'
  | 'schoolProjects'
  | 'auditProjects'
  | 'reviewTasks'
  | 'projectOverview'
  | 'resultManagement'
  | 'home';

export type ArtWorkspaceNavigationPreset = {
  key: ArtWorkspaceNavigationPresetKey;
  label: string;
  path: string;
  accountTypes: readonly WorkbenchAccountType[];
};

const presets: readonly ArtWorkspaceNavigationPreset[] = [
  { key: 'schoolSubmit', label: '统一提交独立页', path: '/crehn/project/submit', accountTypes: ['school'] },
  { key: 'schoolProjects', label: '我的报送项目', path: '/crehn/project', accountTypes: ['school'] },
  { key: 'auditProjects', label: '项目审核', path: '/crehn/audit', accountTypes: ['auditor'] },
  { key: 'reviewTasks', label: '专家评分任务', path: '/crehn/review', accountTypes: ['expert'] },
  { key: 'projectOverview', label: '项目总览', path: '/crehn/project-view', accountTypes: ['admin', 'auditor'] },
  { key: 'resultManagement', label: '结果管理', path: '/crehn/result', accountTypes: ['admin'] },
  { key: 'home', label: '工作台首页', path: '/index', accountTypes: ['school', 'admin', 'auditor', 'expert'] }
];

export const resolveWorkbenchAccountType = (roleKey?: string): WorkbenchAccountType => {
  const role = String(roleKey || '')
    .trim()
    .toLowerCase();
  if (role.includes('school') || role.includes('学校')) return 'school';
  if (role.includes('audit') || role.includes('审核')) return 'auditor';
  if (role.includes('review') || role.includes('expert') || role.includes('评审') || role.includes('专家')) return 'expert';
  return 'admin';
};

export const workspaceNavigationPresetsForRole = (roleKey?: string) => {
  const accountType = resolveWorkbenchAccountType(roleKey);
  return presets.filter((item) => item.accountTypes.includes(accountType));
};

export const defaultWorkspaceNavigationPreset = (roleKey?: string): ArtWorkspaceNavigationPresetKey =>
  workspaceNavigationPresetsForRole(roleKey)[0]?.key || 'home';

export const normalizeWorkspaceNavigationUrl = (value?: string): string => {
  const url = String(value || '').trim();
  if (!url || /\s/.test(url)) return '';
  if (url.startsWith('/') && !url.startsWith('//')) return url;
  try {
    const parsed = new URL(url);
    return parsed.protocol === 'http:' || parsed.protocol === 'https:' ? parsed.toString() : '';
  } catch {
    return '';
  }
};

export const resolveWorkspaceNavigationTarget = (
  config: Pick<ArtWorkspaceHeaderNavigationButtonConfig, 'targetMode' | 'presetKey' | 'customUrl'>,
  roleKey?: string
) => {
  if (config.targetMode === 'custom') return normalizeWorkspaceNavigationUrl(config.customUrl);
  const available = workspaceNavigationPresetsForRole(roleKey);
  return available.find((item) => item.key === config.presetKey)?.path || available[0]?.path || '/index';
};

export const isExternalWorkspaceNavigationTarget = (target: string) => /^https?:\/\//i.test(target);
