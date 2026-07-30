export type LayoutDisplayScaleMode = 'auto' | 'default' | 'small' | 'large' | 'custom';

export interface LayoutDisplayScaleInput {
  mode: LayoutDisplayScaleMode;
  customScalePercent?: number;
  viewportWidth?: number;
  sidebarWidth?: number;
  mobile?: boolean;
}

export const LAYOUT_DISPLAY_SCALE_MIN = 75;
export const LAYOUT_DISPLAY_SCALE_MAX = 110;
export const LAYOUT_DISPLAY_SCALE_DEFAULT_CUSTOM = 90;
export const LAYOUT_DISPLAY_SCALE_AUTO_MAX = 100;
export const LAYOUT_DISPLAY_SCALE_REFERENCE_WIDTH = 1600;
export const LAYOUT_DISPLAY_SCALE_STEP = 5;

const fixedScalePercent: Record<Exclude<LayoutDisplayScaleMode, 'auto' | 'custom'>, number> = {
  default: 100,
  small: 90,
  large: 110
};

export const normalizeLayoutDisplayScaleMode = (value: unknown): LayoutDisplayScaleMode =>
  value === 'default' || value === 'small' || value === 'large' || value === 'custom' ? value : 'auto';

export const normalizeLayoutDisplayScalePercent = (
  value: unknown,
  fallback = LAYOUT_DISPLAY_SCALE_DEFAULT_CUSTOM,
  max = LAYOUT_DISPLAY_SCALE_MAX
) => {
  const numeric = Number(value);
  const resolved = Number.isFinite(numeric) ? Math.round(numeric) : fallback;
  return Math.min(max, Math.max(LAYOUT_DISPLAY_SCALE_MIN, resolved));
};

const roundToStep = (value: number) => Math.round(value / LAYOUT_DISPLAY_SCALE_STEP) * LAYOUT_DISPLAY_SCALE_STEP;

export const resolveAutoLayoutDisplayScalePercent = (viewportWidth: number, sidebarWidth = 0, mobile = false) => {
  if (mobile || viewportWidth < 992) return 100;
  const availableWidth = Math.max(0, viewportWidth - Math.max(0, sidebarWidth));
  const rawPercent = (availableWidth / LAYOUT_DISPLAY_SCALE_REFERENCE_WIDTH) * 100;
  return normalizeLayoutDisplayScalePercent(roundToStep(rawPercent), LAYOUT_DISPLAY_SCALE_AUTO_MAX, LAYOUT_DISPLAY_SCALE_AUTO_MAX);
};

export const resolveLayoutDisplayScalePercent = ({
  mode,
  customScalePercent = LAYOUT_DISPLAY_SCALE_DEFAULT_CUSTOM,
  viewportWidth = LAYOUT_DISPLAY_SCALE_REFERENCE_WIDTH,
  sidebarWidth = 0,
  mobile = false
}: LayoutDisplayScaleInput) => {
  const normalizedMode = normalizeLayoutDisplayScaleMode(mode);
  if (normalizedMode === 'auto') {
    return resolveAutoLayoutDisplayScalePercent(viewportWidth, sidebarWidth, mobile);
  }
  if (normalizedMode === 'custom') {
    return normalizeLayoutDisplayScalePercent(customScalePercent);
  }
  return fixedScalePercent[normalizedMode];
};

export const layoutDisplayScaleModeLabel = (mode: LayoutDisplayScaleMode) =>
  (
    ({
      auto: '自动适配',
      default: '固定默认',
      small: '固定稍小',
      large: '固定较大',
      custom: '自定义'
    }) as const
  )[mode];

export const applyDocumentLayoutDisplayScale = (percent: number) => {
  if (typeof document === 'undefined') return;
  const normalizedPercent = normalizeLayoutDisplayScalePercent(percent, 100);
  const scale = normalizedPercent / 100;
  const inverse = 1 / scale;
  const root = document.documentElement;
  root.style.setProperty('--app-display-scale', scale.toFixed(4));
  root.style.setProperty('--app-display-scale-inverse', inverse.toFixed(6));
  root.style.setProperty('--app-display-viewport-width', `${(inverse * 100).toFixed(4)}vw`);
  root.style.setProperty('--app-display-viewport-height', `${(inverse * 100).toFixed(4)}dvh`);
  root.style.setProperty('--app-display-min-height', `${(inverse * 100).toFixed(4)}vh`);
  root.dataset.appDisplayScale = String(normalizedPercent);
  root.classList.toggle('is-app-display-scaled', normalizedPercent !== 100);
};
