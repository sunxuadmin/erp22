import {
  LAYOUT_DISPLAY_SCALE_DEFAULT_CUSTOM,
  normalizeLayoutDisplayScaleMode,
  normalizeLayoutDisplayScalePercent,
  type LayoutDisplayScaleMode
} from '@/utils/layoutDisplayScale';

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

export interface WorkbenchStylePreset {
  label: string;
  value: WorkbenchStyleConfig;
}

const sidebarDefaults = {
  sidebarBackground: '#f4f8ff',
  sidebarBackdropBlur: 0,
  sidebarItemBackground: 'transparent',
  sidebarItemHoverBackground: '#e7f0ff',
  sidebarBorderColor: '#d9e6f6'
};

const layoutDisplayDefaults = {
  layoutScaleMode: 'default' as LayoutDisplayScaleMode,
  customScalePercent: LAYOUT_DISPLAY_SCALE_DEFAULT_CUSTOM
};

export const defaultWorkbenchStyleConfig: WorkbenchStyleConfig = {
  enabled: true,
  preset: 'sky',
  ...layoutDisplayDefaults,
  pageBackground: 'linear-gradient(180deg,#f5f9ff 0%,#f8fbff 44%,#ffffff 100%)',
  cardBackground: '#ffffff',
  cardBorder: '#d8e6f5',
  accentColor: '#2563eb',
  headingColor: '#0f2f5f',
  calendarBackground: 'linear-gradient(180deg,#2563eb 0%,#0f62b9 100%)',
  shadow: '0 4px 14px rgba(37,99,235,0.05)',
  ...sidebarDefaults
};

export const workbenchStylePresets: WorkbenchStylePreset[] = [
  {
    label: '天蓝渐变',
    value: defaultWorkbenchStyleConfig
  },
  {
    label: '政务蓝',
    value: {
      enabled: true,
      preset: 'official',
      ...layoutDisplayDefaults,
      pageBackground: 'linear-gradient(180deg,#f4f8ff 0%,#f8fbff 55%,#ffffff 100%)',
      cardBackground: '#ffffff',
      cardBorder: '#dbe7f4',
      accentColor: '#2563eb',
      headingColor: '#1e3a8a',
      calendarBackground: 'linear-gradient(180deg,#2563eb,#0f5ea8)',
      shadow: '0 10px 24px rgba(30,64,175,0.07)',
      ...sidebarDefaults
    }
  },
  {
    label: '青蓝清爽',
    value: {
      enabled: true,
      preset: 'cyan',
      ...layoutDisplayDefaults,
      pageBackground: 'linear-gradient(180deg,#f0fffc 0%,#f5fbff 56%,#ffffff 100%)',
      cardBackground: '#ffffff',
      cardBorder: '#d3edf0',
      accentColor: '#0891b2',
      headingColor: '#0f4c81',
      calendarBackground: 'linear-gradient(180deg,#22d3ee,#0284c7)',
      shadow: '0 10px 24px rgba(8,145,178,0.07)',
      ...sidebarDefaults
    }
  },
  {
    label: '绿蓝柔和',
    value: {
      enabled: true,
      preset: 'mint',
      ...layoutDisplayDefaults,
      pageBackground: 'linear-gradient(180deg,#f3fff8 0%,#f5fbff 54%,#ffffff 100%)',
      cardBackground: '#ffffff',
      cardBorder: '#d5eadf',
      accentColor: '#0d9488',
      headingColor: '#0f766e',
      calendarBackground: 'linear-gradient(180deg,#2dd4bf,#0ea5e9)',
      shadow: '0 10px 24px rgba(13,148,136,0.07)',
      ...sidebarDefaults
    }
  },
  {
    label: '白色简洁',
    value: {
      enabled: true,
      preset: 'clean',
      ...layoutDisplayDefaults,
      pageBackground: 'linear-gradient(180deg,#f8fbff 0%,#ffffff 100%)',
      cardBackground: '#ffffff',
      cardBorder: '#e5edf5',
      accentColor: '#2563eb',
      headingColor: '#082f49',
      calendarBackground: 'linear-gradient(180deg,#60a5fa,#2563eb)',
      shadow: '0 8px 20px rgba(5,86,148,0.05)',
      ...sidebarDefaults
    }
  }
];

export const cloneWorkbenchStyle = (config: WorkbenchStyleConfig): WorkbenchStyleConfig => JSON.parse(JSON.stringify(config));

const presetByKey = (preset?: string) => workbenchStylePresets.find((item) => item.value.preset === preset)?.value;

export const normalizeWorkbenchStyleConfig = (value?: Partial<WorkbenchStyleConfig>): WorkbenchStyleConfig => {
  const base = {
    ...cloneWorkbenchStyle(defaultWorkbenchStyleConfig),
    ...(presetByKey(value?.preset) || {})
  };
  const sidebarBackground = value?.sidebarBackground || value?.sBg || base.sidebarBackground || sidebarDefaults.sidebarBackground;
  const sidebarBackdropBlur = Number(value?.sidebarBackdropBlur ?? value?.sBl ?? base.sidebarBackdropBlur ?? sidebarDefaults.sidebarBackdropBlur);
  const sidebarItemBackground = value?.sidebarItemBackground || value?.sIt || base.sidebarItemBackground || sidebarDefaults.sidebarItemBackground;
  const sidebarItemHoverBackground =
    value?.sidebarItemHoverBackground || value?.sHv || base.sidebarItemHoverBackground || sidebarDefaults.sidebarItemHoverBackground;
  const sidebarBorderColor = value?.sidebarBorderColor || value?.sBd || base.sidebarBorderColor || sidebarDefaults.sidebarBorderColor;
  return {
    ...base,
    ...value,
    enabled: value?.enabled !== false,
    preset: value?.preset || base.preset,
    layoutScaleMode: normalizeLayoutDisplayScaleMode(value?.layoutScaleMode ?? base.layoutScaleMode),
    customScalePercent: normalizeLayoutDisplayScalePercent(value?.customScalePercent ?? base.customScalePercent),
    sidebarBackground,
    sidebarBackdropBlur: Number.isFinite(sidebarBackdropBlur) ? Math.min(30, Math.max(0, sidebarBackdropBlur)) : sidebarDefaults.sidebarBackdropBlur,
    sidebarItemBackground,
    sidebarItemHoverBackground,
    sidebarBorderColor
  };
};

export const parseWorkbenchStyleConfig = (raw: unknown): WorkbenchStyleConfig => {
  if (!raw) return cloneWorkbenchStyle(defaultWorkbenchStyleConfig);
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw || '{}') : raw;
    return normalizeWorkbenchStyleConfig(parsed || {});
  } catch {
    return cloneWorkbenchStyle(defaultWorkbenchStyleConfig);
  }
};

export const isWorkbenchStyleConfigPayload = (raw: unknown): boolean => {
  if (!raw) return false;
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw;
    return Boolean(parsed) && typeof parsed === 'object' && !Array.isArray(parsed);
  } catch {
    return false;
  }
};

export const compactWorkbenchStyleConfig = (config: WorkbenchStyleConfig): WorkbenchStyleConfig => ({
  enabled: config.enabled !== false,
  preset: config.preset || 'custom',
  layoutScaleMode: normalizeLayoutDisplayScaleMode(config.layoutScaleMode),
  customScalePercent: normalizeLayoutDisplayScalePercent(config.customScalePercent),
  pageBackground: config.pageBackground || defaultWorkbenchStyleConfig.pageBackground,
  cardBackground: config.cardBackground || defaultWorkbenchStyleConfig.cardBackground,
  cardBorder: config.cardBorder || defaultWorkbenchStyleConfig.cardBorder,
  accentColor: config.accentColor || defaultWorkbenchStyleConfig.accentColor,
  headingColor: config.headingColor || defaultWorkbenchStyleConfig.headingColor,
  calendarBackground: config.calendarBackground || defaultWorkbenchStyleConfig.calendarBackground,
  shadow: config.shadow || defaultWorkbenchStyleConfig.shadow,
  sBg: compactColor(config.sidebarBackground || sidebarDefaults.sidebarBackground),
  sBl: Number(config.sidebarBackdropBlur ?? sidebarDefaults.sidebarBackdropBlur),
  sIt: compactColor(config.sidebarItemBackground || sidebarDefaults.sidebarItemBackground),
  sHv: compactColor(config.sidebarItemHoverBackground || sidebarDefaults.sidebarItemHoverBackground),
  sBd: compactColor(config.sidebarBorderColor || sidebarDefaults.sidebarBorderColor)
});

const compactColor = (value: string) =>
  String(value || '')
    .replace(/\s*,\s*/g, ',')
    .replace(/,0\./g, ',.');

export const workbenchStyleConfigsEqual = (left: WorkbenchStyleConfig, right: WorkbenchStyleConfig): boolean =>
  JSON.stringify(compactWorkbenchStyleConfig(left)) === JSON.stringify(compactWorkbenchStyleConfig(right));

const workbenchStyleVarKeys = [
  '--workbench-page-background',
  '--workbench-card-bg',
  '--workbench-card-border',
  '--workbench-accent',
  '--workbench-heading',
  '--workbench-calendar-bg',
  '--workbench-card-shadow',
  '--workbench-shell-background',
  '--workbench-sidebar-bg',
  '--workbench-sidebar-blur',
  '--workbench-sidebar-item-bg',
  '--workbench-sidebar-item-hover-bg',
  '--workbench-sidebar-border'
] as const;

export const workbenchStyleVars = (config: WorkbenchStyleConfig) => {
  const normalized = normalizeWorkbenchStyleConfig(config);
  if (!normalized.enabled) return {};
  return {
    '--workbench-page-background': normalized.pageBackground,
    '--workbench-card-bg': normalized.cardBackground,
    '--workbench-card-border': normalized.cardBorder,
    '--workbench-accent': normalized.accentColor,
    '--workbench-heading': normalized.headingColor,
    '--workbench-calendar-bg': normalized.calendarBackground,
    '--workbench-card-shadow': normalized.shadow,
    '--workbench-shell-background': normalized.pageBackground,
    '--workbench-sidebar-bg': normalized.sidebarBackground || sidebarDefaults.sidebarBackground,
    '--workbench-sidebar-blur': `${normalized.sidebarBackdropBlur ?? sidebarDefaults.sidebarBackdropBlur}px`,
    '--workbench-sidebar-item-bg': normalized.sidebarItemBackground || sidebarDefaults.sidebarItemBackground,
    '--workbench-sidebar-item-hover-bg': normalized.sidebarItemHoverBackground || sidebarDefaults.sidebarItemHoverBackground,
    '--workbench-sidebar-border': normalized.sidebarBorderColor || sidebarDefaults.sidebarBorderColor
  };
};

export const workbenchStylePreviewVars = (config: WorkbenchStyleConfig): Record<string, string> => {
  const normalized = normalizeWorkbenchStyleConfig(config);
  if (normalized.enabled) return workbenchStyleVars(normalized);
  return Object.fromEntries(workbenchStyleVarKeys.map((key) => [key, 'initial']));
};

export const applyWorkbenchStyleVars = (config: WorkbenchStyleConfig) => {
  if (typeof document === 'undefined') return;
  workbenchStyleVarKeys.forEach((key) => document.documentElement.style.removeProperty(key));
  Object.entries(workbenchStyleVars(config)).forEach(([key, value]) => {
    document.documentElement.style.setProperty(key, String(value));
  });
};
