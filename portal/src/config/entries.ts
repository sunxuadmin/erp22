export type EntryToneName = string;
export type PerformanceMode = "auto" | "full" | "lite";
export type BackgroundMode = "gradient" | "solid" | "image";
export type DecorElementType =
  | "glass-disc"
  | "glow-orb"
  | "ring"
  | "arc"
  | "polygon"
  | "sparkle"
  | "image";
export type DecorMotionType = "none" | "float" | "drift" | "orbit" | "breathe" | "rotate";

export interface HeroContent {
  documentTitle: string;
  brand: string;
  brandEn: string;
  brandLogo: string;
  eyebrow: string;
  title: string;
  subtitle: string;
  description: string;
  collectionLabel: string;
  collectionTitle: string;
  footerLead: string;
  footer: string;
}

export interface LayoutSettings {
  topbarOffsetX: number;
  topbarOffsetY: number;
  heroOffsetX: number;
  heroOffsetY: number;
  entriesOffsetX: number;
  entriesOffsetY: number;
  footerOffsetX: number;
  footerOffsetY: number;
  noticeOffsetX: number;
  noticeOffsetY: number;
}

export interface MotionSettings {
  enabled: boolean;
  userToggle: boolean;
  speed: number;
  intensity: number;
  dustEnabled: boolean;
  dustCount: number;
  glassBubblesEnabled: boolean;
  glassBubbleCount: number;
  lightCurtainEnabled: boolean;
  titleEntrance: boolean;
  titleSheenEnabled: boolean;
  titleSheenDuration: number;
  titleSheenDelay: number;
  titleSheenIntensity: number;
  heroBubbleDuration: number;
  heroBubbleTravel: number;
  v1DecorMotionEnabled: boolean;
  v1DecorDuration: number;
  v1DecorTravel: number;
  v1DecorRotation: number;
  cardEntrance: boolean;
  cardBorderGlow: boolean;
  respectReducedMotion: boolean;
}

export interface IridescenceSettings {
  color: readonly [number, number, number];
  speed: number;
  amplitude: number;
  maxFps: number;
  maxDpr: number;
  opacity: number;
  brightness: number;
  saturation: number;
  dimOpacity: number;
  mouseReact: boolean;
  pauseWhenHidden: boolean;
}

export interface OfficialBackgroundSettings {
  baseDarkness: number;
  textureOpacity: number;
  ribbonOpacity: number;
  haloOpacity: number;
  auroraOpacity: number;
  waveOpacity: number;
  ringOpacity: number;
}

export interface BackgroundSettings {
  iridescence: IridescenceSettings;
  official: OfficialBackgroundSettings;
  motion: MotionSettings;
  visual: VisualBackgroundSettings;
}

export interface DecorMotionSettings {
  enabled: boolean;
  type: DecorMotionType;
  duration: number;
  delay: number;
  distanceX: number;
  distanceY: number;
  rotation: number;
  easing: string;
}

export interface DecorElement {
  id: string;
  enabled: boolean;
  type: DecorElementType;
  image: string;
  x: number;
  y: number;
  width: number;
  height: number;
  rotation: number;
  color: string;
  secondaryColor: string;
  opacity: number;
  blur: number;
  backdropBlur: number;
  borderOpacity: number;
  zIndex: number;
  hideOnMobile: boolean;
  motion: DecorMotionSettings;
}

export interface VisualBackgroundSettings {
  mode: BackgroundMode;
  builtInDecorEnabled: boolean;
  solidColor: string;
  gradientAngle: number;
  image: string;
  imageOpacity: number;
  imagePosition: string;
  imageSize: string;
  imageBlur: number;
  overlayColor: string;
  overlayOpacity: number;
  brightness: number;
  contrast: number;
  saturation: number;
  elements: DecorElement[];
}

export interface PerformanceSettings {
  mode: PerformanceMode;
  mobileLiteEnabled: boolean;
  mobileMaxWidth: number;
  lowHardwareConcurrency: number;
  lowDeviceMemory: number;
  respectSaveData: boolean;
  liteDisableMotion: boolean;
  liteGlassBlur: number;
  liteGlassSaturation: number;
}

export interface EntryTone {
  name: EntryToneName;
  rgb: string;
  deep: string;
  soft: string;
  edge: string;
}

export interface EntryItem {
  id: string;
  enabled: boolean;
  number: string;
  logo: string;
  backgroundImage: string;
  backgroundOpacity: number;
  backgroundPosition: string;
  backgroundSize: string;
  backgroundBlur: number;
  title: string;
  description: string;
  href: string;
  target: "_self" | "_blank";
  labelEn: string;
  notice: string;
  tone: EntryTone;
}

export interface ThemeSettings {
  pageBgStart: string;
  pageBgMiddle: string;
  pageBgEnd: string;
  blueGlowRgb: string;
  brandText: string;
  brandSubtext: string;
  heroTitle: string;
  heroGradientMiddle: string;
  heroAccent: string;
  heroSubtitle: string;
  bodyText: string;
  cardTitle: string;
  cardText: string;
  sectionTitle: string;
  footerText: string;
  footerLine: string;
  footerFontSize: number;
  glassCardOpacity: number;
  glassCardMobileOpacity: number;
  glassBorderOpacity: number;
  glassBlur: number;
  glassContrast: number;
  glassSaturation: number;
  glassTextOpacity: number;
  glassHighlightOpacity: number;
  glassShadowOpacity: number;
  particleOpacity: number;
  particleSize: number;
  particleGlow: number;
  topbarOpacity: number;
  topbarBorderOpacity: number;
  topbarShadowOpacity: number;
  logoTileOpacity: number;
  noticeFollowTopbar: boolean;
  noticeOpacity: number;
  noticeBorderOpacity: number;
  noticeText: string;
}

export interface EntryMessages {
  maintenance: string;
  entering: string;
  enteringDelayMs: number;
}

export interface VersionOption {
  id: string;
  label: string;
}

export interface VersionSettings {
  active: string;
  allowQueryOverride: boolean;
  userToggle: boolean;
  options: VersionOption[];
}

export interface RuntimeEntryItem extends Partial<Omit<EntryItem, "id" | "tone">> {
  id?: string;
  tone?: Partial<EntryTone>;
}

export interface RuntimeDecorElement extends Partial<Omit<DecorElement, "motion">> {
  motion?: Partial<DecorMotionSettings>;
}

export interface ArtEntryVariantConfig {
  hero?: Partial<HeroContent>;
  layout?: Partial<LayoutSettings>;
  theme?: Partial<ThemeSettings>;
  messages?: Partial<EntryMessages>;
  entries?: RuntimeEntryItem[];
  motion?: Partial<MotionSettings>;
  performance?: Partial<PerformanceSettings>;
  background?: Partial<Omit<VisualBackgroundSettings, "elements">> & {
    elements?: RuntimeDecorElement[];
  };
}

export interface ArtEntryRuntimeConfig extends ArtEntryVariantConfig {
  version?: Partial<Omit<VersionSettings, "options">> & { options?: VersionOption[] };
  variants?: Record<string, ArtEntryVariantConfig>;
}

declare global {
  interface Window {
    CREHN_PORTAL_CONFIG?: ArtEntryRuntimeConfig;
  }
}

const sourceRuntimeConfig: ArtEntryRuntimeConfig =
  typeof window === "undefined" ? {} : window.CREHN_PORTAL_CONFIG ?? {};

const defaultVersionSettings: VersionSettings = {
  active: "v1",
  allowQueryOverride: true,
  userToggle: false,
  options: [
    { id: "v1", label: "参考稿版" },
    { id: "v2", label: "竖卡展厅版" }
  ]
};

const runtimeVersion = sourceRuntimeConfig.version ?? {};
const requestedOptions = Array.isArray(runtimeVersion.options)
  ? runtimeVersion.options.filter(
      (item): item is VersionOption =>
        Boolean(item) && typeof item.id === "string" && item.id.trim().length > 0
    )
  : defaultVersionSettings.options;
const configuredOptions = requestedOptions.length > 0 ? requestedOptions : defaultVersionSettings.options;
const optionIds = new Set(configuredOptions.map((item) => item.id));
const configuredActive =
  typeof runtimeVersion.active === "string" && optionIds.has(runtimeVersion.active)
    ? runtimeVersion.active
    : optionIds.has(defaultVersionSettings.active)
      ? defaultVersionSettings.active
      : configuredOptions[0].id;
const queryVersion =
  typeof window === "undefined" ? null : new URLSearchParams(window.location.search).get("version");
const allowQueryOverride = runtimeVersion.allowQueryOverride !== false;

export const activeVersion =
  allowQueryOverride && queryVersion && optionIds.has(queryVersion) ? queryVersion : configuredActive;

export const versionSettings: VersionSettings = {
  active: configuredActive,
  allowQueryOverride,
  userToggle: runtimeVersion.userToggle === true,
  options: configuredOptions
};

const selectedVariant = sourceRuntimeConfig.variants?.[activeVersion] ?? {};
const runtimeConfig: ArtEntryRuntimeConfig = {
  ...sourceRuntimeConfig,
  ...selectedVariant,
  hero: { ...sourceRuntimeConfig.hero, ...selectedVariant.hero },
  layout: { ...sourceRuntimeConfig.layout, ...selectedVariant.layout },
  theme: { ...sourceRuntimeConfig.theme, ...selectedVariant.theme },
  messages: { ...sourceRuntimeConfig.messages, ...selectedVariant.messages },
  motion: { ...sourceRuntimeConfig.motion, ...selectedVariant.motion },
  performance: { ...sourceRuntimeConfig.performance, ...selectedVariant.performance },
  background: {
    ...sourceRuntimeConfig.background,
    ...selectedVariant.background,
    elements: selectedVariant.background?.elements ?? sourceRuntimeConfig.background?.elements
  },
  entries: selectedVariant.entries ?? sourceRuntimeConfig.entries
};

const clampNumber = (value: unknown, fallback: number, min: number, max: number) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? Math.min(max, Math.max(min, parsed)) : fallback;
};

const stringValue = (value: unknown, fallback: string) =>
  typeof value === "string" ? value : fallback;

const booleanValue = (value: unknown, fallback: boolean) =>
  typeof value === "boolean" ? value : fallback;

const defaultHeroContent: HeroContent = {
  documentTitle: "创意河南艺术设计大赛",
  brand: "创意河南艺术设计大赛",
  brandEn: "CREATIVE HENAN DESIGN COMPETITION",
  brandLogo: "./assets/logo-icon.png",
  eyebrow: "全省高等学校艺术设计赛事平台",
  title: "创意 · 设计 · 河南",
  subtitle: "艺术与科技 · 创新与实践",
  description: "第六届“创意河南”艺术设计大赛独立门户。",
  collectionLabel: "COMPETITION SERVICES",
  collectionTitle: "赛事服务",
  footerLead: "CREATIVITY CONNECTS HENAN",
  footer: "© 2026 创意河南艺术设计大赛"
};

export const heroContent: HeroContent = {
  ...defaultHeroContent,
  ...runtimeConfig.hero
};

const defaultLayout: LayoutSettings = {
  topbarOffsetX: 0,
  topbarOffsetY: 0,
  heroOffsetX: 0,
  heroOffsetY: 0,
  entriesOffsetX: 0,
  entriesOffsetY: 0,
  footerOffsetX: 0,
  footerOffsetY: 0,
  noticeOffsetX: 0,
  noticeOffsetY: 0
};

const runtimeLayout = runtimeConfig.layout ?? {};

export const layoutConfig: LayoutSettings = {
  topbarOffsetX: clampNumber(runtimeLayout.topbarOffsetX, defaultLayout.topbarOffsetX, -240, 240),
  topbarOffsetY: clampNumber(runtimeLayout.topbarOffsetY, defaultLayout.topbarOffsetY, -160, 160),
  heroOffsetX: clampNumber(runtimeLayout.heroOffsetX, defaultLayout.heroOffsetX, -240, 240),
  heroOffsetY: clampNumber(runtimeLayout.heroOffsetY, defaultLayout.heroOffsetY, -200, 200),
  entriesOffsetX: clampNumber(runtimeLayout.entriesOffsetX, defaultLayout.entriesOffsetX, -240, 240),
  entriesOffsetY: clampNumber(runtimeLayout.entriesOffsetY, defaultLayout.entriesOffsetY, -200, 200),
  footerOffsetX: clampNumber(runtimeLayout.footerOffsetX, defaultLayout.footerOffsetX, -240, 240),
  footerOffsetY: clampNumber(runtimeLayout.footerOffsetY, defaultLayout.footerOffsetY, -120, 120),
  noticeOffsetX: clampNumber(runtimeLayout.noticeOffsetX, defaultLayout.noticeOffsetX, -240, 240),
  noticeOffsetY: clampNumber(runtimeLayout.noticeOffsetY, defaultLayout.noticeOffsetY, -160, 160)
};

const defaultTheme: ThemeSettings = {
  pageBgStart: "#edf9fb",
  pageBgMiddle: "#edf4ff",
  pageBgEnd: "#eef2ff",
  blueGlowRgb: "70, 128, 244",
  brandText: "#112e5d",
  brandSubtext: "rgba(47, 67, 98, .58)",
  heroTitle: "#079fe2",
  heroGradientMiddle: "#2781ee",
  heroAccent: "#5748d8",
  heroSubtitle: "rgba(10, 46, 98, .9)",
  bodyText: "rgba(35, 62, 98, .68)",
  cardTitle: "#0b326b",
  cardText: "rgba(42, 63, 91, .67)",
  sectionTitle: "#123a75",
  footerText: "rgba(34, 47, 67, .52)",
  footerLine: "rgba(47, 63, 88, .18)",
  footerFontSize: 10,
  glassCardOpacity: 0.72,
  glassCardMobileOpacity: 0.62,
  glassBorderOpacity: 0.74,
  glassBlur: 6,
  glassContrast: 0.85,
  glassSaturation: 1.6,
  glassTextOpacity: 0.96,
  glassHighlightOpacity: 0.92,
  glassShadowOpacity: 0.12,
  particleOpacity: 0.24,
  particleSize: 3,
  particleGlow: 10,
  topbarOpacity: 0.08,
  topbarBorderOpacity: 0.62,
  topbarShadowOpacity: 0.1,
  logoTileOpacity: 0.48,
  noticeFollowTopbar: true,
  noticeOpacity: 0.18,
  noticeBorderOpacity: 0.62,
  noticeText: "#13396e"
};

const runtimeTheme = runtimeConfig.theme ?? {};

export const themeConfig: ThemeSettings = {
  ...defaultTheme,
  ...runtimeTheme,
  footerFontSize: clampNumber(runtimeTheme.footerFontSize, defaultTheme.footerFontSize, 8, 18),
  glassCardOpacity: clampNumber(runtimeTheme.glassCardOpacity, defaultTheme.glassCardOpacity, 0.08, 0.96),
  glassCardMobileOpacity: clampNumber(
    runtimeTheme.glassCardMobileOpacity,
    defaultTheme.glassCardMobileOpacity,
    0.08,
    0.98
  ),
  glassBorderOpacity: clampNumber(runtimeTheme.glassBorderOpacity, defaultTheme.glassBorderOpacity, 0.1, 1),
  glassBlur: clampNumber(runtimeTheme.glassBlur, defaultTheme.glassBlur, 6, 32),
  glassContrast: clampNumber(runtimeTheme.glassContrast, defaultTheme.glassContrast, 0.85, 1.3),
  glassSaturation: clampNumber(runtimeTheme.glassSaturation, defaultTheme.glassSaturation, 0.75, 1.6),
  glassTextOpacity: clampNumber(runtimeTheme.glassTextOpacity, defaultTheme.glassTextOpacity, 0.5, 0.96),
  glassHighlightOpacity: clampNumber(
    runtimeTheme.glassHighlightOpacity,
    defaultTheme.glassHighlightOpacity,
    0.2,
    1
  ),
  glassShadowOpacity: clampNumber(
    runtimeTheme.glassShadowOpacity,
    defaultTheme.glassShadowOpacity,
    0,
    0.4
  ),
  particleOpacity: clampNumber(runtimeTheme.particleOpacity, defaultTheme.particleOpacity, 0, 0.6),
  particleSize: clampNumber(runtimeTheme.particleSize, defaultTheme.particleSize, 1, 6),
  particleGlow: clampNumber(runtimeTheme.particleGlow, defaultTheme.particleGlow, 0, 24),
  topbarOpacity: clampNumber(runtimeTheme.topbarOpacity, defaultTheme.topbarOpacity, 0, 0.96),
  topbarBorderOpacity: clampNumber(
    runtimeTheme.topbarBorderOpacity,
    defaultTheme.topbarBorderOpacity,
    0,
    1
  ),
  topbarShadowOpacity: clampNumber(
    runtimeTheme.topbarShadowOpacity,
    defaultTheme.topbarShadowOpacity,
    0,
    0.4
  ),
  logoTileOpacity: clampNumber(runtimeTheme.logoTileOpacity, defaultTheme.logoTileOpacity, 0, 0.96),
  noticeFollowTopbar: booleanValue(runtimeTheme.noticeFollowTopbar, defaultTheme.noticeFollowTopbar),
  noticeOpacity: clampNumber(runtimeTheme.noticeOpacity, defaultTheme.noticeOpacity, 0, 0.96),
  noticeBorderOpacity: clampNumber(
    runtimeTheme.noticeBorderOpacity,
    defaultTheme.noticeBorderOpacity,
    0,
    1
  )
};

const defaultPerformance: PerformanceSettings = {
  mode: "auto",
  mobileLiteEnabled: true,
  mobileMaxWidth: 820,
  lowHardwareConcurrency: 4,
  lowDeviceMemory: 4,
  respectSaveData: true,
  liteDisableMotion: true,
  liteGlassBlur: 10,
  liteGlassSaturation: 1.04
};

const runtimePerformance = runtimeConfig.performance ?? {};
const configuredPerformanceMode: PerformanceMode =
  runtimePerformance.mode === "full" || runtimePerformance.mode === "lite"
    ? runtimePerformance.mode
    : "auto";

export const performanceConfig: PerformanceSettings = {
  mode: configuredPerformanceMode,
  mobileLiteEnabled: booleanValue(runtimePerformance.mobileLiteEnabled, defaultPerformance.mobileLiteEnabled),
  mobileMaxWidth: clampNumber(
    runtimePerformance.mobileMaxWidth,
    defaultPerformance.mobileMaxWidth,
    320,
    1600
  ),
  lowHardwareConcurrency: clampNumber(
    runtimePerformance.lowHardwareConcurrency,
    defaultPerformance.lowHardwareConcurrency,
    1,
    32
  ),
  lowDeviceMemory: clampNumber(runtimePerformance.lowDeviceMemory, defaultPerformance.lowDeviceMemory, 1, 32),
  respectSaveData: booleanValue(runtimePerformance.respectSaveData, defaultPerformance.respectSaveData),
  liteDisableMotion: booleanValue(runtimePerformance.liteDisableMotion, defaultPerformance.liteDisableMotion),
  liteGlassBlur: clampNumber(runtimePerformance.liteGlassBlur, defaultPerformance.liteGlassBlur, 0, 18),
  liteGlassSaturation: clampNumber(
    runtimePerformance.liteGlassSaturation,
    defaultPerformance.liteGlassSaturation,
    0.75,
    1.2
  )
};

const defaultMessages: EntryMessages = {
  maintenance: "系统正在维护中",
  entering: "正在进入“{title}”...",
  enteringDelayMs: 450
};

const runtimeMessages = runtimeConfig.messages ?? {};

export const entryMessages: EntryMessages = {
  maintenance: stringValue(runtimeMessages.maintenance, defaultMessages.maintenance),
  entering: stringValue(runtimeMessages.entering, defaultMessages.entering),
  enteringDelayMs: clampNumber(runtimeMessages.enteringDelayMs, defaultMessages.enteringDelayMs, 0, 5000)
};

const defaultBackgroundConfig: BackgroundSettings = {
  iridescence: {
    color: [0.58, 0.72, 1],
    speed: 0.04,
    amplitude: 0.025,
    maxFps: 18,
    maxDpr: 1,
    opacity: 0.38,
    brightness: 0.82,
    saturation: 1.28,
    dimOpacity: 0.2,
    mouseReact: false,
    pauseWhenHidden: true
  },
  official: {
    baseDarkness: 0.22,
    textureOpacity: 0.1,
    ribbonOpacity: 0.28,
    haloOpacity: 0.46,
    auroraOpacity: 0.2,
    waveOpacity: 0.24,
    ringOpacity: 0.2
  },
  motion: {
    enabled: true,
    userToggle: true,
    speed: 0.65,
    intensity: 0.48,
    dustEnabled: true,
    dustCount: 12,
    glassBubblesEnabled: true,
    glassBubbleCount: 5,
    lightCurtainEnabled: true,
    titleEntrance: true,
    titleSheenEnabled: false,
    titleSheenDuration: 2.6,
    titleSheenDelay: 0.55,
    titleSheenIntensity: 0.72,
    heroBubbleDuration: 22,
    heroBubbleTravel: 18,
    v1DecorMotionEnabled: true,
    v1DecorDuration: 48,
    v1DecorTravel: 22,
    v1DecorRotation: 4,
    cardEntrance: true,
    cardBorderGlow: true,
    respectReducedMotion: true
  },
  visual: {
    mode: "gradient",
    builtInDecorEnabled: true,
    solidColor: "#edf4ff",
    gradientAngle: 135,
    image: "",
    imageOpacity: 0.22,
    imagePosition: "center",
    imageSize: "cover",
    imageBlur: 0,
    overlayColor: "#ffffff",
    overlayOpacity: 0,
    brightness: 1,
    contrast: 1,
    saturation: 1,
    elements: [
      {
        id: "default-glass-disc-left",
        enabled: true,
        type: "glass-disc",
        image: "",
        x: 5,
        y: 18,
        width: 310,
        height: 310,
        rotation: 0,
        color: "#d9f8ff",
        secondaryColor: "#ebe8ff",
        opacity: 0.18,
        blur: 1,
        backdropBlur: 18,
        borderOpacity: 0.48,
        zIndex: 1,
        hideOnMobile: false,
        motion: {
          enabled: true,
          type: "float",
          duration: 42,
          delay: 0,
          distanceX: 18,
          distanceY: 22,
          rotation: 2,
          easing: "ease-in-out"
        }
      },
      {
        id: "default-glow-right",
        enabled: true,
        type: "glow-orb",
        image: "",
        x: 76,
        y: 12,
        width: 360,
        height: 360,
        rotation: 0,
        color: "#9dd9ff",
        secondaryColor: "#c8a8ff",
        opacity: 0.13,
        blur: 36,
        backdropBlur: 0,
        borderOpacity: 0,
        zIndex: 0,
        hideOnMobile: false,
        motion: {
          enabled: true,
          type: "drift",
          duration: 56,
          delay: -8,
          distanceX: 28,
          distanceY: 16,
          rotation: 0,
          easing: "ease-in-out"
        }
      }
    ]
  }
};

const runtimeBackground = runtimeConfig.background ?? {};
const defaultDecorMotion: DecorMotionSettings = {
  enabled: true,
  type: "float",
  duration: 40,
  delay: 0,
  distanceX: 16,
  distanceY: 20,
  rotation: 2,
  easing: "ease-in-out"
};

const normalizeDecorElement = (value: RuntimeDecorElement, index: number): DecorElement => {
  const motion = value.motion ?? {};
  const allowedTypes: DecorElementType[] = [
    "glass-disc",
    "glow-orb",
    "ring",
    "arc",
    "polygon",
    "sparkle",
    "image"
  ];
  const allowedMotionTypes: DecorMotionType[] = ["none", "float", "drift", "orbit", "breathe", "rotate"];

  return {
    id: stringValue(value.id, `decor-${index + 1}`).trim() || `decor-${index + 1}`,
    enabled: booleanValue(value.enabled, true),
    type: allowedTypes.includes(value.type as DecorElementType)
      ? (value.type as DecorElementType)
      : "glass-disc",
    image: stringValue(value.image, "").trim(),
    x: clampNumber(value.x, 50, -30, 130),
    y: clampNumber(value.y, 50, -30, 130),
    width: clampNumber(value.width, 180, 12, 1200),
    height: clampNumber(value.height, 180, 12, 1200),
    rotation: clampNumber(value.rotation, 0, -360, 360),
    color: stringValue(value.color, "#d9f8ff"),
    secondaryColor: stringValue(value.secondaryColor, "#ebe8ff"),
    opacity: clampNumber(value.opacity, 0.2, 0, 1),
    blur: clampNumber(value.blur, 0, 0, 120),
    backdropBlur: clampNumber(value.backdropBlur, 0, 0, 48),
    borderOpacity: clampNumber(value.borderOpacity, 0.4, 0, 1),
    zIndex: clampNumber(value.zIndex, 1, 0, 4),
    hideOnMobile: booleanValue(value.hideOnMobile, false),
    motion: {
      enabled: booleanValue(motion.enabled, defaultDecorMotion.enabled),
      type: allowedMotionTypes.includes(motion.type as DecorMotionType)
        ? (motion.type as DecorMotionType)
        : defaultDecorMotion.type,
      duration: clampNumber(motion.duration, defaultDecorMotion.duration, 4, 240),
      delay: clampNumber(motion.delay, defaultDecorMotion.delay, -240, 240),
      distanceX: clampNumber(motion.distanceX, defaultDecorMotion.distanceX, -240, 240),
      distanceY: clampNumber(motion.distanceY, defaultDecorMotion.distanceY, -240, 240),
      rotation: clampNumber(motion.rotation, defaultDecorMotion.rotation, -180, 180),
      easing: stringValue(motion.easing, defaultDecorMotion.easing)
    }
  };
};

export const backgroundConfig: BackgroundSettings = {
  ...defaultBackgroundConfig,
  motion: {
    ...defaultBackgroundConfig.motion,
    ...runtimeConfig.motion
  },
  visual: {
    ...defaultBackgroundConfig.visual,
    ...runtimeBackground,
    mode:
      runtimeBackground.mode === "solid" || runtimeBackground.mode === "image"
        ? runtimeBackground.mode
        : "gradient",
    builtInDecorEnabled: booleanValue(
      runtimeBackground.builtInDecorEnabled,
      defaultBackgroundConfig.visual.builtInDecorEnabled
    ),
    gradientAngle: clampNumber(
      runtimeBackground.gradientAngle,
      defaultBackgroundConfig.visual.gradientAngle,
      0,
      360
    ),
    imageOpacity: clampNumber(
      runtimeBackground.imageOpacity,
      defaultBackgroundConfig.visual.imageOpacity,
      0,
      1
    ),
    imageBlur: clampNumber(runtimeBackground.imageBlur, defaultBackgroundConfig.visual.imageBlur, 0, 40),
    overlayOpacity: clampNumber(
      runtimeBackground.overlayOpacity,
      defaultBackgroundConfig.visual.overlayOpacity,
      0,
      1
    ),
    brightness: clampNumber(runtimeBackground.brightness, defaultBackgroundConfig.visual.brightness, 0.4, 1.8),
    contrast: clampNumber(runtimeBackground.contrast, defaultBackgroundConfig.visual.contrast, 0.5, 1.8),
    saturation: clampNumber(runtimeBackground.saturation, defaultBackgroundConfig.visual.saturation, 0, 2),
    elements: (
      Array.isArray(runtimeBackground.elements)
        ? runtimeBackground.elements
        : defaultBackgroundConfig.visual.elements
    )
      .slice(0, 30)
      .map(normalizeDecorElement)
      .filter((item) => item.enabled)
  }
};

const defaultEntryItems: EntryItem[] = [
  {
    id: "participant",
    enabled: true,
    number: "01",
    logo: "",
    backgroundImage: "",
    backgroundOpacity: 0.14,
    backgroundPosition: "center",
    backgroundSize: "cover",
    backgroundBlur: 0,
    title: "参赛者登录",
    description: "激活账号、填报作品并查看提交状态",
    href: "/admin/login",
    target: "_self",
    labelEn: "PARTICIPANT",
    notice: defaultMessages.maintenance,
    tone: {
      name: "mint",
      rgb: "18, 212, 198",
      deep: "#0c8f86",
      soft: "rgba(190, 255, 247, .6)",
      edge: "rgba(18, 212, 198, .36)"
    }
  },
  {
    id: "school",
    enabled: true,
    number: "02",
    logo: "",
    backgroundImage: "",
    backgroundOpacity: 0.14,
    backgroundPosition: "center",
    backgroundSize: "cover",
    backgroundBlur: 0,
    title: "学校工作台",
    description: "名单、审核推荐与最终批次提交",
    href: "/admin/login",
    target: "_self",
    labelEn: "SCHOOL",
    notice: defaultMessages.maintenance,
    tone: {
      name: "violet",
      rgb: "150, 88, 240",
      deep: "#7350cf",
      soft: "rgba(237, 222, 255, .66)",
      edge: "rgba(150, 88, 240, .34)"
    }
  },
  {
    id: "activate",
    enabled: true,
    number: "03",
    logo: "",
    backgroundImage: "",
    backgroundOpacity: 0.14,
    backgroundPosition: "center",
    backgroundSize: "cover",
    backgroundBlur: 0,
    title: "账号激活",
    description: "使用学校发放的一次性激活码设置本人密码",
    href: "/admin/activate",
    target: "_self",
    labelEn: "ACTIVATE",
    notice: defaultMessages.maintenance,
    tone: {
      name: "gold",
      rgb: "232, 170, 62",
      deep: "#9b6818",
      soft: "rgba(255, 238, 190, .62)",
      edge: "rgba(232, 170, 62, .34)"
    }
  }
  ,
  {
    id: "notice",
    enabled: true,
    number: "04",
    logo: "",
    backgroundImage: "",
    backgroundOpacity: 0.14,
    backgroundPosition: "center",
    backgroundSize: "cover",
    backgroundBlur: 0,
    title: "通知与资讯",
    description: "查看赛事通知、指南与时间安排",
    href: "#news",
    target: "_self",
    labelEn: "NEWS",
    notice: defaultMessages.maintenance,
    tone: {
      name: "gold",
      rgb: "232, 170, 62",
      deep: "#9b6818",
      soft: "rgba(255, 238, 190, .62)",
      edge: "rgba(232, 170, 62, .34)"
    }
  }
];

const runtimeEntries = Array.isArray(runtimeConfig.entries) ? runtimeConfig.entries : [];

const fallbackTonePalette = defaultEntryItems.map((entry) => entry.tone);
const configuredEntrySource: RuntimeEntryItem[] =
  runtimeConfig.entries === undefined ? defaultEntryItems : runtimeEntries;
const usedEntryIds = new Set<string>();

export const entryItems: EntryItem[] = configuredEntrySource
  .map((override, index): EntryItem | null => {
    if (!override || booleanValue(override.enabled, true) === false) return null;

    const matchingDefault =
      (override.id ? defaultEntryItems.find((entry) => entry.id === override.id) : undefined) ??
      defaultEntryItems[index];
    const paletteTone = fallbackTonePalette[index % fallbackTonePalette.length];
    const fallback: EntryItem = matchingDefault ?? {
      ...defaultEntryItems[0],
      id: `entry-${index + 1}`,
      number: String(index + 1).padStart(2, "0"),
      title: "未命名活动",
      description: "",
      labelEn: `ENTRY ${index + 1}`,
      tone: paletteTone
    };
    const requestedId = stringValue(override.id, fallback.id).trim() || `entry-${index + 1}`;
    let uniqueId = requestedId;
    let suffix = 2;
    while (usedEntryIds.has(uniqueId)) {
      uniqueId = `${requestedId}-${suffix}`;
      suffix += 1;
    }
    usedEntryIds.add(uniqueId);

    return {
      ...fallback,
      id: uniqueId,
      enabled: true,
      number:
        stringValue(override.number, "").trim() ||
        String(index + 1).padStart(2, "0"),
      logo: stringValue(override.logo, fallback.logo).trim(),
      backgroundImage: stringValue(override.backgroundImage, fallback.backgroundImage).trim(),
      backgroundOpacity: clampNumber(
        override.backgroundOpacity,
        fallback.backgroundOpacity,
        0,
        1
      ),
      backgroundPosition: stringValue(
        override.backgroundPosition,
        fallback.backgroundPosition
      ),
      backgroundSize: stringValue(override.backgroundSize, fallback.backgroundSize),
      backgroundBlur: clampNumber(override.backgroundBlur, fallback.backgroundBlur, 0, 24),
      title: stringValue(override.title, fallback.title).trim() || "未命名活动",
      description: stringValue(override.description, fallback.description),
      href: stringValue(override.href, fallback.href).trim(),
      target: override.target === "_blank" ? "_blank" : "_self",
      labelEn: stringValue(override.labelEn, fallback.labelEn),
      notice: stringValue(override.notice, entryMessages.maintenance),
      tone: {
        ...fallback.tone,
        ...paletteTone,
        ...(override.tone ?? {}),
        name: stringValue(override.tone?.name, fallback.tone.name)
      }
    };
  })
  .filter((entry): entry is EntryItem => entry !== null);

export const formatEnteringMessage = (title: string) =>
  entryMessages.entering.split("{title}").join(title);
