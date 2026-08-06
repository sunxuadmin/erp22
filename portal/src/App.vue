<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import BackgroundLayer from "./components/BackgroundLayer.vue";
import EntryCard from "./components/EntryCard.vue";
import GlassTuner from "./components/GlassTuner.vue";
import HeroTitle from "./components/HeroTitle.vue";
import PortalContent from "./components/PortalContent.vue";
import VersionTwoLayout from "./components/VersionTwoLayout.vue";
import {
  activeVersion,
  backgroundConfig,
  entryItems,
  entryMessages,
  formatEnteringMessage,
  heroContent,
  layoutConfig,
  performanceConfig,
  themeConfig,
  versionSettings,
  type EntryItem,
  type MotionSettings
} from "./config/entries";

const MOTION_STORAGE_KEY = "art-entry-motion";
const GLASS_STORAGE_KEY = "art-entry-glass-settings-v1";
const LOGO_CLICK_COUNT = 5;
const LOGO_CLICK_WINDOW_MS = 2200;

interface GlassSettings {
  cardOpacity: number;
  topbarOpacity: number;
  topbarBorderOpacity: number;
  topbarShadowOpacity: number;
  logoTileOpacity: number;
  textOpacity: number;
  contrast: number;
  saturation: number;
  blur: number;
  heroTitle: string;
  heroGradientMiddle: string;
  heroAccent: string;
  heroSubtitle: string;
}

interface NavigatorPerformanceHints extends Navigator {
  deviceMemory?: number;
  connection?: { saveData?: boolean };
}

const clamp = (value: unknown, fallback: number, min: number, max: number) => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? Math.min(max, Math.max(min, parsed)) : fallback;
};

const defaultGlassSettings: GlassSettings = {
  cardOpacity: themeConfig.glassCardOpacity,
  topbarOpacity: themeConfig.topbarOpacity,
  topbarBorderOpacity: themeConfig.topbarBorderOpacity,
  topbarShadowOpacity: themeConfig.topbarShadowOpacity,
  logoTileOpacity: themeConfig.logoTileOpacity,
  textOpacity: themeConfig.glassTextOpacity,
  contrast: themeConfig.glassContrast,
  saturation: themeConfig.glassSaturation,
  blur: themeConfig.glassBlur,
  heroTitle: themeConfig.heroTitle,
  heroGradientMiddle: themeConfig.heroGradientMiddle,
  heroAccent: themeConfig.heroAccent,
  heroSubtitle: themeConfig.heroSubtitle
};

function normalizedGlassSettings(value: Partial<GlassSettings> | null | undefined): GlassSettings {
  return {
    cardOpacity: clamp(value?.cardOpacity, defaultGlassSettings.cardOpacity, 0.18, 0.72),
    topbarOpacity: clamp(value?.topbarOpacity, defaultGlassSettings.topbarOpacity, 0, 0.6),
    topbarBorderOpacity: clamp(
      value?.topbarBorderOpacity,
      defaultGlassSettings.topbarBorderOpacity,
      0,
      1
    ),
    topbarShadowOpacity: clamp(
      value?.topbarShadowOpacity,
      defaultGlassSettings.topbarShadowOpacity,
      0,
      0.4
    ),
    logoTileOpacity: clamp(value?.logoTileOpacity, defaultGlassSettings.logoTileOpacity, 0, 0.96),
    textOpacity: clamp(value?.textOpacity, defaultGlassSettings.textOpacity, 0.5, 0.96),
    contrast: clamp(value?.contrast, defaultGlassSettings.contrast, 0.85, 1.3),
    saturation: clamp(value?.saturation, defaultGlassSettings.saturation, 0.75, 1.6),
    blur: clamp(value?.blur, defaultGlassSettings.blur, 6, 32),
    heroTitle: typeof value?.heroTitle === "string" ? value.heroTitle : defaultGlassSettings.heroTitle,
    heroGradientMiddle:
      typeof value?.heroGradientMiddle === "string"
        ? value.heroGradientMiddle
        : defaultGlassSettings.heroGradientMiddle,
    heroAccent: typeof value?.heroAccent === "string" ? value.heroAccent : defaultGlassSettings.heroAccent,
    heroSubtitle:
      typeof value?.heroSubtitle === "string" ? value.heroSubtitle : defaultGlassSettings.heroSubtitle
  };
}

function loadGlassSettings() {
  try {
    const saved = window.localStorage.getItem(GLASS_STORAGE_KEY);
    return saved ? normalizedGlassSettings(JSON.parse(saved) as Partial<GlassSettings>) : { ...defaultGlassSettings };
  } catch {
    return { ...defaultGlassSettings };
  }
}

const notice = ref("");
const savedMotion = window.localStorage.getItem(MOTION_STORAGE_KEY);
const motionEnabled = ref(
  backgroundConfig.motion.enabled &&
    (savedMotion === null ? backgroundConfig.motion.enabled : savedMotion !== "off")
);
const glassSettings = ref<GlassSettings>(loadGlassSettings());
const glassTunerOpen = ref(false);
const viewportWidth = ref(window.innerWidth);
const reducedMotionQuery = window.matchMedia("(prefers-reduced-motion: reduce)");
const systemReducedMotion = ref(reducedMotionQuery.matches);
const navigatorHints = navigator as NavigatorPerformanceHints;
const lowHardwareConcurrency =
  navigator.hardwareConcurrency > 0 &&
  navigator.hardwareConcurrency <= performanceConfig.lowHardwareConcurrency;
const lowDeviceMemory =
  typeof navigatorHints.deviceMemory === "number" &&
  navigatorHints.deviceMemory <= performanceConfig.lowDeviceMemory;
const saveDataEnabled = performanceConfig.respectSaveData && navigatorHints.connection?.saveData === true;
const brandLogoUrl = heroContent.brandLogo || "./assets/logo-icon.png";
const isEditorPreview =
  ["127.0.0.1", "localhost"].includes(window.location.hostname) &&
  new URLSearchParams(window.location.search).get("editorPreview") === "1";
let noticeTimer: number | undefined;
let logoClickTimer: number | undefined;
let logoClickStartedAt = 0;
let logoClickCounter = 0;

const isLiteMode = computed(() => {
  if (performanceConfig.mode === "lite") return true;
  if (performanceConfig.mode === "full") return false;

  return (
    (backgroundConfig.motion.respectReducedMotion && systemReducedMotion.value) ||
    saveDataEnabled ||
    lowHardwareConcurrency ||
    lowDeviceMemory ||
    (performanceConfig.mobileLiteEnabled && viewportWidth.value <= performanceConfig.mobileMaxWidth)
  );
});

const effectiveMotionEnabled = computed(
  () => motionEnabled.value && !(isLiteMode.value && performanceConfig.liteDisableMotion)
);

const renderedMotion = computed<MotionSettings>(() => {
  if (!isLiteMode.value || !performanceConfig.liteDisableMotion) return backgroundConfig.motion;

  return {
    ...backgroundConfig.motion,
    dustEnabled: backgroundConfig.motion.dustEnabled,
    dustCount: Math.min(backgroundConfig.motion.dustCount, 4),
    glassBubblesEnabled: false,
    lightCurtainEnabled: false
  };
});

const performanceStatusLabel = computed(() => {
  if (!isLiteMode.value) return "完整模式：动画与玻璃效果按当前设置运行";
  if (performanceConfig.mode === "lite") return "节能模式：已由 config.js 强制启用";
  if (backgroundConfig.motion.respectReducedMotion && systemReducedMotion.value) {
    return "节能模式：已遵循系统“减少动态效果”设置";
  }
  if (saveDataEnabled) return "节能模式：已检测到浏览器节省流量设置";
  if (performanceConfig.mobileLiteEnabled && viewportWidth.value <= performanceConfig.mobileMaxWidth) {
    return `节能模式：当前宽度不超过 ${performanceConfig.mobileMaxWidth}px`;
  }
  if (lowDeviceMemory) return "节能模式：已检测到设备内存较低";
  return "节能模式：已检测到处理器并发能力较低";
});

const motionStyles = computed<Record<string, string | number>>(() => {
  const speed = Math.min(1.2, Math.max(0.35, backgroundConfig.motion.speed));
  const intensity = Math.min(1, Math.max(0, backgroundConfig.motion.intensity));
  const titleSheenDuration = Math.min(4, Math.max(0.4, backgroundConfig.motion.titleSheenDuration));
  const titleSheenDelay = Math.min(5, Math.max(0, backgroundConfig.motion.titleSheenDelay));
  const titleSheenIntensity = Math.min(1, Math.max(0, backgroundConfig.motion.titleSheenIntensity));
  const titleSheenOpacity = 0.28 + titleSheenIntensity * 0.7;
  const heroBubbleDuration = Math.min(60, Math.max(8, backgroundConfig.motion.heroBubbleDuration));
  const heroBubbleTravel = Math.min(48, Math.max(4, backgroundConfig.motion.heroBubbleTravel));
  const v1DecorDuration = Math.min(120, Math.max(20, backgroundConfig.motion.v1DecorDuration));
  const v1DecorTravel = Math.min(60, Math.max(4, backgroundConfig.motion.v1DecorTravel));
  const v1DecorRotation = Math.min(12, Math.max(0, backgroundConfig.motion.v1DecorRotation));
  const glassBlur = isLiteMode.value
    ? Math.min(glassSettings.value.blur, performanceConfig.liteGlassBlur)
    : glassSettings.value.blur;
  const glassSaturation = isLiteMode.value
    ? Math.min(glassSettings.value.saturation, performanceConfig.liteGlassSaturation)
    : glassSettings.value.saturation;
  const particleOpacity = isLiteMode.value ? Math.min(themeConfig.particleOpacity, 0.12) : themeConfig.particleOpacity;
  const particleSize = isLiteMode.value ? Math.min(themeConfig.particleSize, 3) : themeConfig.particleSize;
  const particleGlow = isLiteMode.value ? Math.min(themeConfig.particleGlow, 6) : themeConfig.particleGlow;

  return {
    "--motion-play-state": effectiveMotionEnabled.value ? "running" : "paused",
    "--dust-duration": `${Math.round(34 / speed)}s`,
    "--bubble-duration": `${Math.round(64 / speed)}s`,
    "--sweep-duration": `${Math.round(28 / speed)}s`,
    "--dust-opacity": (0.5 * intensity).toFixed(3),
    "--bubble-opacity": (0.72 * intensity).toFixed(3),
    "--ambient-opacity": (0.22 * intensity).toFixed(3),
    "--sweep-opacity": (0.18 * intensity).toFixed(3),
    "--title-sheen-duration": `${titleSheenDuration}s`,
    "--title-sheen-delay": `${titleSheenDelay}s`,
    "--title-sheen-opacity": titleSheenOpacity.toFixed(3),
    "--title-sheen-soft-opacity": (titleSheenOpacity * 0.72).toFixed(3),
    "--title-sheen-mid-opacity": (titleSheenOpacity * 0.8).toFixed(3),
    "--hero-bubble-duration": `${heroBubbleDuration}s`,
    "--hero-bubble-x": `${heroBubbleTravel.toFixed(1)}px`,
    "--hero-bubble-x-negative": `${(-heroBubbleTravel).toFixed(1)}px`,
    "--hero-bubble-y": `${(heroBubbleTravel * 0.55).toFixed(1)}px`,
    "--hero-bubble-y-negative": `${(-heroBubbleTravel * 0.55).toFixed(1)}px`,
    "--v1-drift-duration-fast": `${(v1DecorDuration * 0.82).toFixed(1)}s`,
    "--v1-drift-duration-base": `${v1DecorDuration.toFixed(1)}s`,
    "--v1-drift-duration-slow": `${(v1DecorDuration * 1.18).toFixed(1)}s`,
    "--v1-drift-duration-slower": `${(v1DecorDuration * 1.42).toFixed(1)}s`,
    "--v1-drift-x": `${v1DecorTravel.toFixed(1)}px`,
    "--v1-drift-x-negative": `${(-v1DecorTravel).toFixed(1)}px`,
    "--v1-drift-x-small": `${(v1DecorTravel * 0.46).toFixed(1)}px`,
    "--v1-drift-x-small-negative": `${(-v1DecorTravel * 0.46).toFixed(1)}px`,
    "--v1-drift-y": `${(v1DecorTravel * 0.7).toFixed(1)}px`,
    "--v1-drift-y-negative": `${(-v1DecorTravel * 0.7).toFixed(1)}px`,
    "--v1-drift-y-small": `${(v1DecorTravel * 0.34).toFixed(1)}px`,
    "--v1-drift-y-small-negative": `${(-v1DecorTravel * 0.34).toFixed(1)}px`,
    "--v1-drift-rotate": `${v1DecorRotation.toFixed(2)}deg`,
    "--v1-drift-rotate-negative": `${(-v1DecorRotation).toFixed(2)}deg`,
    "--page-bg-start": themeConfig.pageBgStart,
    "--page-bg-middle": themeConfig.pageBgMiddle,
    "--page-bg-end": themeConfig.pageBgEnd,
    "--page-solid-color": backgroundConfig.visual.solidColor,
    "--background-gradient-angle": `${backgroundConfig.visual.gradientAngle}deg`,
    "--blue-glow-rgb": themeConfig.blueGlowRgb,
    "--brand-text-color": themeConfig.brandText,
    "--brand-subtext-color": themeConfig.brandSubtext,
    "--hero-title-color": glassSettings.value.heroTitle,
    "--hero-gradient-middle-color": glassSettings.value.heroGradientMiddle,
    "--hero-accent-color": glassSettings.value.heroAccent,
    "--hero-subtitle-color": glassSettings.value.heroSubtitle,
    "--body-text-color": themeConfig.bodyText,
    "--card-title-color": themeConfig.cardTitle,
    "--card-text-color": themeConfig.cardText,
    "--section-title-color": themeConfig.sectionTitle,
    "--footer-text-color": themeConfig.footerText,
    "--footer-line-color": themeConfig.footerLine,
    "--footer-font-size": `${themeConfig.footerFontSize}px`,
    "--layout-topbar-x": `${layoutConfig.topbarOffsetX}px`,
    "--layout-topbar-y": `${layoutConfig.topbarOffsetY}px`,
    "--layout-hero-x": `${layoutConfig.heroOffsetX}px`,
    "--layout-hero-y": `${layoutConfig.heroOffsetY}px`,
    "--layout-entries-x": `${layoutConfig.entriesOffsetX}px`,
    "--layout-entries-y": `${layoutConfig.entriesOffsetY}px`,
    "--layout-footer-x": `${layoutConfig.footerOffsetX}px`,
    "--layout-footer-y": `${layoutConfig.footerOffsetY}px`,
    "--layout-notice-x": `${layoutConfig.noticeOffsetX}px`,
    "--layout-notice-y": `${layoutConfig.noticeOffsetY}px`,
    "--glass-card-opacity": glassSettings.value.cardOpacity,
    "--glass-card-soft-opacity": (glassSettings.value.cardOpacity * 0.32).toFixed(3),
    "--glass-card-mobile-opacity": themeConfig.glassCardMobileOpacity,
    "--glass-border-opacity": themeConfig.glassBorderOpacity,
    "--glass-blur": `${glassBlur}px`,
    "--glass-contrast": glassSettings.value.contrast,
    "--glass-saturation": glassSaturation,
    "--glass-text-opacity": glassSettings.value.textOpacity,
    "--glass-highlight-opacity": themeConfig.glassHighlightOpacity,
    "--glass-shadow-opacity": isLiteMode.value ? Math.min(themeConfig.glassShadowOpacity, 0.08) : themeConfig.glassShadowOpacity,
    "--particle-opacity": particleOpacity,
    "--particle-size": `${particleSize}px`,
    "--particle-glow": `${particleGlow}px`,
    "--topbar-opacity": glassSettings.value.topbarOpacity,
    "--topbar-border-opacity": glassSettings.value.topbarBorderOpacity,
    "--topbar-shadow-opacity": glassSettings.value.topbarShadowOpacity,
    "--logo-tile-opacity": glassSettings.value.logoTileOpacity,
    "--notice-opacity": themeConfig.noticeFollowTopbar
      ? glassSettings.value.topbarOpacity
      : themeConfig.noticeOpacity,
    "--notice-border-opacity": themeConfig.noticeFollowTopbar
      ? glassSettings.value.topbarBorderOpacity
      : themeConfig.noticeBorderOpacity,
    "--notice-text-color": themeConfig.noticeText
  };
});

const pageClasses = computed(() => [
  "page-shell",
  `page-version-${activeVersion.replace(/[^a-z0-9_-]/gi, "-")}`,
  effectiveMotionEnabled.value ? "motion-enabled" : "motion-paused",
  isLiteMode.value ? "performance-lite" : "performance-full",
  backgroundConfig.motion.respectReducedMotion ? "respect-reduced-motion" : "",
  backgroundConfig.motion.titleEntrance ? "has-title-entrance" : "",
  backgroundConfig.motion.titleSheenEnabled ? "has-title-sheen" : "",
  activeVersion === "v1" && backgroundConfig.motion.v1DecorMotionEnabled ? "has-v1-decor-motion" : "",
  backgroundConfig.motion.cardEntrance ? "has-card-entrance" : "",
  backgroundConfig.motion.cardBorderGlow ? "has-card-border-glow" : "",
  entryItems.length > 4 ? "has-many-entries" : "",
  isEditorPreview ? "editor-preview" : ""
]);

watch(notice, (value) => {
  window.clearTimeout(noticeTimer);
  if (value) noticeTimer = window.setTimeout(() => (notice.value = ""), 2400);
});

watch(motionEnabled, (enabled) => {
  window.localStorage.setItem(MOTION_STORAGE_KEY, enabled ? "on" : "off");
});

watch(
  glassSettings,
  (value) => window.localStorage.setItem(GLASS_STORAGE_KEY, JSON.stringify(value)),
  { deep: true, flush: "sync" }
);

function handleViewportChange() {
  viewportWidth.value = window.innerWidth;
}

function handleReducedMotionChange(event: MediaQueryListEvent) {
  systemReducedMotion.value = event.matches;
}

function handleGlobalKeydown(event: KeyboardEvent) {
  if (event.key === "Escape" && glassTunerOpen.value) glassTunerOpen.value = false;
}

onMounted(() => {
  document.title = heroContent.documentTitle;
  window.addEventListener("resize", handleViewportChange, { passive: true });
  window.addEventListener("keydown", handleGlobalKeydown);
  reducedMotionQuery.addEventListener("change", handleReducedMotionChange);
});

onBeforeUnmount(() => {
  window.clearTimeout(noticeTimer);
  window.clearTimeout(logoClickTimer);
  window.removeEventListener("resize", handleViewportChange);
  window.removeEventListener("keydown", handleGlobalKeydown);
  reducedMotionQuery.removeEventListener("change", handleReducedMotionChange);
});

function handleBrandLogoClick() {
  const now = Date.now();
  if (logoClickCounter === 0 || now - logoClickStartedAt > LOGO_CLICK_WINDOW_MS) {
    logoClickCounter = 1;
    logoClickStartedAt = now;
  } else {
    logoClickCounter += 1;
  }

  window.clearTimeout(logoClickTimer);

  if (logoClickCounter >= LOGO_CLICK_COUNT) {
    logoClickCounter = 0;
    logoClickStartedAt = 0;
    glassTunerOpen.value = !glassTunerOpen.value;
    return;
  }

  logoClickTimer = window.setTimeout(() => {
    logoClickCounter = 0;
    logoClickStartedAt = 0;
  }, Math.max(0, LOGO_CLICK_WINDOW_MS - (now - logoClickStartedAt)));
}

function applyGlassSettings(value: GlassSettings) {
  glassSettings.value = normalizedGlassSettings(value);
}

function resetGlassSettings() {
  glassSettings.value = { ...defaultGlassSettings };
  window.localStorage.removeItem(GLASS_STORAGE_KEY);
}

function activateEntry(entry: EntryItem) {
  const href = entry.href.trim();
  const isUnsafe = /^(?:javascript|data|vbscript):/i.test(href);

  if (!href || href === "#" || isUnsafe) {
    notice.value = entry.notice || entryMessages.maintenance;
    return;
  }

  notice.value = formatEnteringMessage(entry.title);

  if (entry.target === "_blank") {
    const popup = window.open("about:blank", "_blank");
    if (popup) popup.opener = null;

    window.setTimeout(() => {
      if (popup && !popup.closed) {
        popup.location.replace(href);
        return;
      }
      window.location.assign(href);
    }, entryMessages.enteringDelayMs);
    return;
  }

  window.setTimeout(() => window.location.assign(href), entryMessages.enteringDelayMs);
}

function selectVersion(versionId: string) {
  const url = new URL(window.location.href);
  url.searchParams.set("version", versionId);
  window.location.assign(url.toString());
}
</script>

<template>
  <main :class="pageClasses" :style="motionStyles">
    <BackgroundLayer
      :motion="renderedMotion"
      :version="activeVersion"
      :visual="backgroundConfig.visual"
    />

    <header class="art-topbar">
      <div class="art-topbar__inner">
        <div class="art-brand">
          <button
            class="art-brand-mark art-brand-mark--control"
            type="button"
            aria-label="品牌标识（连续点击五次打开显示调节）"
            aria-controls="glass-tuner"
            :aria-expanded="glassTunerOpen"
            data-testid="brand-logo-trigger"
            @click="handleBrandLogoClick"
          >
            <img :src="brandLogoUrl" alt="" />
          </button>
          <span class="art-brand-copy">
            <strong>{{ heroContent.brand }}</strong>
            <small>{{ heroContent.brandEn }}</small>
          </span>
        </div>
        <div class="visual-controls">
          <div v-if="versionSettings.userToggle" class="version-switcher" aria-label="页面版本切换">
            <button
              v-for="option in versionSettings.options"
              :key="option.id"
              type="button"
              :class="{ 'is-active': activeVersion === option.id }"
              :aria-pressed="activeVersion === option.id"
              @click="selectVersion(option.id)"
            >
              {{ option.label }}
            </button>
          </div>
          <button
            v-if="backgroundConfig.motion.enabled && backgroundConfig.motion.userToggle"
            class="motion-toggle motion-toggle--invisible"
            :class="{ 'is-paused': !effectiveMotionEnabled }"
            type="button"
            :disabled="isLiteMode && performanceConfig.liteDisableMotion"
            :aria-pressed="effectiveMotionEnabled"
            :aria-label="isLiteMode ? '节能模式已暂停背景动效' : motionEnabled ? '暂停背景动效' : '开启背景动效'"
            :title="isLiteMode ? '节能模式已暂停背景动效' : motionEnabled ? '暂停背景动效' : '开启背景动效'"
            @click="motionEnabled = !motionEnabled"
          >
            <span class="motion-toggle__icon" aria-hidden="true" />
          </button>
        </div>
      </div>
    </header>

    <GlassTuner
      v-if="glassTunerOpen"
      :model-value="glassSettings"
      :defaults="defaultGlassSettings"
      :performance-label="performanceStatusLabel"
      @update:model-value="applyGlassSettings"
      @close="glassTunerOpen = false"
      @reset="resetGlassSettings"
    />

    <VersionTwoLayout
      v-if="activeVersion === 'v2'"
      :content="heroContent"
      :entries="entryItems"
      @activate="activateEntry"
    />

    <section v-else class="hero-layout" aria-label="艺术展演活动入口">
      <HeroTitle :content="heroContent" />
      <section class="entry-panel" aria-label="活动入口列表">
        <div class="entry-panel-glow" aria-hidden="true" />
        <div class="entry-section-heading">
          <span>{{ heroContent.collectionLabel }}</span>
          <i aria-hidden="true" />
          <strong>{{ heroContent.collectionTitle }}</strong>
        </div>
        <div class="entry-list">
          <EntryCard
            v-for="(entry, index) in entryItems"
            :key="entry.id"
            :entry="entry"
            :index="index"
            @activate="activateEntry"
          />
        </div>
      </section>
    </section>

    <PortalContent />

    <footer class="page-footer" aria-label="版权信息">
      <span>{{ heroContent.footerLead }}</span>
      <i aria-hidden="true" />
      <span>{{ heroContent.footer }}</span>
    </footer>

    <div class="notice-toast" :class="{ 'is-visible': notice }" role="status" aria-live="polite">
      <span>{{ notice || entryMessages.maintenance }}</span>
    </div>
  </main>
</template>
