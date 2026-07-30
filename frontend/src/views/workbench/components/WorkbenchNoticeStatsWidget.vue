<template>
  <section class="workbench-stage-notice">
    <div class="stage-notice-top" :style="topLayoutStyle">
      <div class="stage-notice-copy">
        <span>公告</span>
        <strong>{{ noticeTitle }}</strong>
        <p v-html="noticeContentHtml"></p>
      </div>

      <div class="stage-notice-activity">
        <span>当前活动</span>
        <h2>{{ activityName }}</h2>
      </div>

      <aside class="stage-notice-calendar" :style="calendarCardStyle" aria-label="当前时间">
        <div>{{ calendarInfo.monthText }}</div>
        <strong>{{ calendarInfo.day }}</strong>
        <span>{{ calendarInfo.weekday }}</span>
        <small v-if="resolvedConfig.calendarShowTime !== false">{{ calendarInfo.timeText }}</small>
      </aside>
    </div>

    <el-skeleton v-if="loading" :rows="3" animated />
    <div v-else class="stage-notice-stats" :style="statsGridStyle">
      <article
        v-for="item in visibleStats"
        :key="item.key"
        class="stage-notice-stat"
        :class="[
          `stage-notice-stat--${cssSafeKey(item.key)}`,
          patternModeClass(item.patternMode),
          patternPresetClass(item.patternPreset),
          patternPositionClass(item.patternPosition),
          {
            'is-hover-text-enabled': isHoverTextEnabled,
            'is-hover-pattern-enabled': isHoverPatternEnabled
          }
        ]"
        :style="statStyle(item)"
      >
        <span v-if="item.patternMode !== 'none'" class="stage-notice-stat__pattern" :style="patternStyle(item)" aria-hidden="true">
          <svg-icon
            v-if="normalizePatternMode(item.patternMode) === 'icon' && item.patternIcon"
            class-name="stage-notice-stat__svg-icon"
            :icon-class="item.patternIcon"
          />
          <span v-else-if="normalizePatternMode(item.patternMode) === 'text'" class="stage-notice-stat__text-symbol">
            {{ normalizePatternText(item.patternText) }}
          </span>
        </span>
        <span class="stage-notice-stat__glass" :style="glassStyle(item)" aria-hidden="true"></span>
        <i class="stage-notice-stat__accent" :style="{ backgroundColor: item.accentColor }"></i>
        <strong :style="{ color: item.titleColor, fontSize: `${item.fontSize}px` }">{{ item.label }}</strong>
        <span class="stage-notice-stat__numbers" :style="{ color: item.numberColor }">
          <b>{{ statValue(item.key) }}</b>
        </span>
        <small v-if="secondaryVisible(item)" class="stage-notice-stat__secondary" :style="secondaryStyle(item)">
          <span>{{ item.secondary?.label }}</span>
          <b :style="{ color: item.secondary?.numberColor || item.numberColor }">{{ secondaryValue(item) }}</b>
        </small>
      </article>
    </div>
    <div v-if="runtimeStyleHtml" v-html="runtimeStyleHtml"></div>
  </section>
</template>

<script setup lang="ts">
import { getAuditWorkbench, listAuditActivityOptions, listAuditCategoryOptions, listAuditProject } from '@/api/crehn/audit';
import { listSchoolInfo } from '@/api/crehn/config';
import { getReviewWorkbench } from '@/api/crehn/review';
import { getUploadOverview, getUploadSummary } from '@/api/crehn/result';
import { getRegisterNotice } from '@/api/login';
import { workbenchAssetUrl } from '@/api/system/workbench';

type NoticeMode = 'admin' | 'auditor' | 'reviewer';

type NoticeStatSecondaryConfig = {
  visible?: boolean;
  label?: string;
  valueKey?: string;
  fontSize?: number;
  textColor?: string;
  numberColor?: string;
};

type HoverEffectMode = 'none' | 'text' | 'pattern' | 'textPattern';

type NoticeStatConfig = {
  key: string;
  label: string;
  visible?: boolean;
  order?: number;
  fontSize?: number;
  titleColor?: string;
  numberColor?: string;
  backgroundColor?: string;
  borderColor?: string;
  accentColor?: string;
  customCss?: string;
  patternMode?: string;
  patternPreset?: string;
  patternIcon?: string;
  patternText?: string;
  patternColor?: string;
  patternOpacity?: number;
  patternScale?: number;
  patternPosition?: string;
  patternImageKey?: string;
  patternImageName?: string;
  patternImageSize?: string;
  patternImageRepeat?: string;
  glassColor?: string;
  glassOpacity?: number;
  glassBlur?: number;
  textureOpacity?: number;
  highlightOpacity?: number;
  secondary?: NoticeStatSecondaryConfig;
};

type NoticeStatsConfig = {
  topHeight: number;
  topGap: number;
  noticeRatio: number;
  activityRatio: number;
  calendarWidth: number;
  calendarHeight: number;
  calendarRadius: number;
  calendarBackground: string;
  calendarTextColor: string;
  calendarMonthFontSize: number;
  calendarDayFontSize: number;
  calendarWeekdayFontSize: number;
  calendarTimeFontSize: number;
  calendarTimeBackground: string;
  calendarShowTime: boolean;
  calendarShowSeconds: boolean;
  categoryColumns: number;
  categoryGap: number;
  categoryRadius: number;
  hoverEffectMode: HoverEffectMode;
  hoverScaleEnabled: boolean;
  hoverScale: number;
  categoryAreaBackground: string;
  componentCss: string;
  dateCss: string;
  categories: NoticeStatConfig[];
};

const props = defineProps<{ title: string; configJson?: string; mode: NoticeMode }>();

const loading = ref(false);
const notices = ref<any[]>([]);
const activityName = ref('2026 大学生艺术展演活动');
const now = ref(new Date());
const values = ref<Record<string, number>>({});
let clockTimer: number | undefined;

const baseStatStyle = {
  fontSize: 15,
  titleColor: '#082f49',
  numberColor: '#2563eb',
  backgroundColor: '#f8fbff',
  borderColor: '#bfd7ff',
  accentColor: '#2563eb',
  customCss: ''
};

const defaultAdminStats: NoticeStatConfig[] = [
  { key: 'school_total', label: '学校总数', visible: true, order: 1, ...baseStatStyle },
  {
    key: 'submitted_schools',
    label: '已报送学校',
    visible: true,
    order: 2,
    ...baseStatStyle,
    numberColor: '#0891b2',
    backgroundColor: '#f7fcff',
    borderColor: '#c7e6f4',
    accentColor: '#0284c7'
  },
  {
    key: 'unsubmitted_schools',
    label: '未报送学校',
    visible: true,
    order: 3,
    ...baseStatStyle,
    numberColor: '#d97706',
    backgroundColor: '#fffaf0',
    borderColor: '#f4d99d',
    accentColor: '#d97706'
  },
  {
    key: 'project_total',
    label: '项目总数',
    visible: true,
    order: 4,
    ...baseStatStyle,
    numberColor: '#059669',
    backgroundColor: '#f6fdfa',
    borderColor: '#cdeee1',
    accentColor: '#0d9488'
  },
  {
    key: 'pending_audit',
    label: '待审核项目',
    visible: true,
    order: 5,
    ...baseStatStyle,
    numberColor: '#4f46e5',
    backgroundColor: '#f5f7ff',
    borderColor: '#cbd5ff',
    accentColor: '#4f46e5'
  },
  {
    key: 'audit_passed',
    label: '已通过项目',
    visible: true,
    order: 6,
    ...baseStatStyle,
    numberColor: '#16a34a',
    backgroundColor: '#f6fdfa',
    borderColor: '#cdeee1',
    accentColor: '#16a34a'
  }
];

const defaultAuditStats: NoticeStatConfig[] = [
  { key: 'assigned_total', label: '分配总数', visible: true, order: 1, ...baseStatStyle },
  {
    key: 'pending_audit',
    label: '待审核',
    visible: true,
    order: 2,
    ...baseStatStyle,
    numberColor: '#d97706',
    backgroundColor: '#fffaf0',
    borderColor: '#f4d99d',
    accentColor: '#d97706'
  },
  {
    key: 'audited_total',
    label: '已审核',
    visible: true,
    order: 3,
    ...baseStatStyle,
    numberColor: '#0891b2',
    backgroundColor: '#f7fcff',
    borderColor: '#c7e6f4',
    accentColor: '#0284c7',
    secondary: { visible: true, label: '我已审核', valueKey: 'my_audited_total', fontSize: 13, textColor: '#5c7087', numberColor: '#0891b2' }
  },
  {
    key: 'audit_passed',
    label: '审核通过',
    visible: true,
    order: 4,
    ...baseStatStyle,
    numberColor: '#16a34a',
    backgroundColor: '#f6fdfa',
    borderColor: '#cdeee1',
    accentColor: '#16a34a',
    secondary: { visible: true, label: '我已审核通过', valueKey: 'my_audit_passed', fontSize: 13, textColor: '#527162', numberColor: '#16a34a' }
  },
  {
    key: 'returned',
    label: '审核退回',
    visible: true,
    order: 5,
    ...baseStatStyle,
    numberColor: '#dc2626',
    backgroundColor: '#fef2f2',
    borderColor: '#fecaca',
    accentColor: '#ef4444',
    secondary: { visible: true, label: '我已审核退回', valueKey: 'my_returned', fontSize: 13, textColor: '#7f4d4d', numberColor: '#dc2626' }
  }
];

const defaultReviewStats: NoticeStatConfig[] = [
  { key: 'assigned_total', label: '分配总数', visible: true, order: 1, ...baseStatStyle },
  {
    key: 'pending_score',
    label: '待评分',
    visible: true,
    order: 2,
    ...baseStatStyle,
    numberColor: '#d97706',
    backgroundColor: '#fffaf0',
    borderColor: '#f4d99d',
    accentColor: '#d97706'
  },
  {
    key: 'draft_score',
    label: '草稿',
    visible: true,
    order: 3,
    ...baseStatStyle,
    numberColor: '#0891b2',
    backgroundColor: '#f7fcff',
    borderColor: '#c7e6f4',
    accentColor: '#0284c7'
  },
  {
    key: 'submitted_score',
    label: '已提交',
    visible: true,
    order: 4,
    ...baseStatStyle,
    numberColor: '#16a34a',
    backgroundColor: '#f6fdfa',
    borderColor: '#cdeee1',
    accentColor: '#16a34a'
  },
  {
    key: 'locked_by_other',
    label: '已被锁定',
    visible: true,
    order: 5,
    ...baseStatStyle,
    numberColor: '#dc2626',
    backgroundColor: '#fef2f2',
    borderColor: '#fecaca',
    accentColor: '#ef4444'
  }
];

const defaultNoticeLayoutConfig = {
  topHeight: 220,
  topGap: 18,
  noticeRatio: 0.9,
  activityRatio: 1.4,
  calendarWidth: 190,
  calendarHeight: 220,
  calendarRadius: 8,
  calendarBackground: 'var(--workbench-calendar-bg, linear-gradient(180deg, #2563eb 0%, #0f62b9 100%))',
  calendarTextColor: '#ffffff',
  calendarMonthFontSize: 14,
  calendarDayFontSize: 54,
  calendarWeekdayFontSize: 17,
  calendarTimeFontSize: 12,
  calendarTimeBackground: 'rgba(255,255,255,0.18)',
  calendarShowTime: true,
  calendarShowSeconds: true
};

const defaultConfig = computed<NoticeStatsConfig>(() => ({
  ...defaultNoticeLayoutConfig,
  categoryColumns: props.mode === 'admin' ? 6 : 5,
  categoryGap: 12,
  categoryRadius: 8,
  hoverEffectMode: 'textPattern',
  hoverScaleEnabled: true,
  hoverScale: 1.04,
  categoryAreaBackground: '#f8fbff',
  componentCss: '',
  dateCss: '',
  categories: props.mode === 'admin' ? defaultAdminStats : props.mode === 'reviewer' ? defaultReviewStats : defaultAuditStats
}));

const clampNumber = (value: unknown, fallback: number, min: number, max: number) => {
  const numeric = Number(value ?? fallback);
  if (!Number.isFinite(numeric)) return fallback;
  return Math.min(max, Math.max(min, numeric));
};

const normalizeHoverScale = (value?: number) => Math.min(1.3, Math.max(1, Number(value || 1.08)));
const hoverEffectModes: HoverEffectMode[] = ['none', 'text', 'pattern', 'textPattern'];
const normalizeHoverEffectMode = (value?: string, enabled?: boolean): HoverEffectMode => {
  const mode = String(value || '') as HoverEffectMode;
  if (hoverEffectModes.includes(mode)) return mode;
  return enabled === false ? 'none' : 'text';
};
const hoverTextEnabled = (mode?: string) => {
  const normalized = normalizeHoverEffectMode(mode, false);
  return normalized === 'text' || normalized === 'textPattern';
};
const hoverPatternEnabled = (mode?: string) => {
  const normalized = normalizeHoverEffectMode(mode, false);
  return normalized === 'pattern' || normalized === 'textPattern';
};

const resolvedConfig = computed(() => parseConfig(props.configJson));

const calendarInfo = computed(() => {
  const date = now.value;
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
  const timeParts = [String(date.getHours()).padStart(2, '0'), String(date.getMinutes()).padStart(2, '0')];
  if (resolvedConfig.value.calendarShowSeconds !== false) {
    timeParts.push(String(date.getSeconds()).padStart(2, '0'));
  }
  return {
    monthText: `${date.getFullYear()}年${date.getMonth() + 1}月`,
    day: String(date.getDate()).padStart(2, '0'),
    weekday: weekdays[date.getDay()],
    timeText: timeParts.join(':')
  };
});

const hoverEffectMode = computed(() => normalizeHoverEffectMode(resolvedConfig.value.hoverEffectMode, resolvedConfig.value.hoverScaleEnabled));
const isHoverTextEnabled = computed(() => hoverTextEnabled(hoverEffectMode.value));
const isHoverPatternEnabled = computed(() => hoverPatternEnabled(hoverEffectMode.value));
const normalizedHoverScale = computed(() => normalizeHoverScale(resolvedConfig.value.hoverScale));
const visibleStats = computed(() =>
  resolvedConfig.value.categories.filter((item) => item.visible !== false).sort((a, b) => Number(a.order || 0) - Number(b.order || 0))
);
const primaryNotice = computed(() => notices.value[0] || {});
const noticeTitle = computed(() => {
  const fallbackTitle = props.mode === 'admin' ? '管理员公告' : props.mode === 'reviewer' ? '评审公告' : '审核公告';
  return primaryNotice.value.noticeTitle || `${props.title || fallbackTitle}`;
});
const noticeContentHtml = computed(() =>
  escapeHtml(primaryNotice.value.noticeContent || '请关注当前活动进度，及时处理相关工作。').replace(/\n/g, '<br/>')
);

const statsGridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${Math.max(1, Number(resolvedConfig.value.categoryColumns || 5))}, minmax(0, 1fr))`,
  gap: `${Math.max(0, Number(resolvedConfig.value.categoryGap || 0))}px`,
  background: isDefaultStatsAreaBackground(resolvedConfig.value.categoryAreaBackground) ? 'transparent' : resolvedConfig.value.categoryAreaBackground,
  '--stage-stat-hover-scale': String(normalizedHoverScale.value)
}));
const topHeightPx = computed(() => `${clampNumber(resolvedConfig.value.topHeight, 220, 160, 360)}px`);

const topLayoutStyle = computed(
  () =>
    ({
      gridTemplateColumns: `minmax(220px, ${clampNumber(resolvedConfig.value.noticeRatio, 0.9, 0.5, 2)}fr) minmax(320px, ${clampNumber(
        resolvedConfig.value.activityRatio,
        1.4,
        0.5,
        2.5
      )}fr) minmax(120px, ${clampNumber(resolvedConfig.value.calendarWidth, 190, 120, 320)}px)`,
      gap: `${clampNumber(resolvedConfig.value.topGap, 18, 8, 40)}px`,
      gridAutoRows: topHeightPx.value,
      '--stage-top-height': topHeightPx.value
    }) as Record<string, string>
);

const calendarCardStyle = computed(
  () =>
    ({
      width: `${clampNumber(resolvedConfig.value.calendarWidth, 190, 120, 320)}px`,
      height: topHeightPx.value,
      minHeight: topHeightPx.value,
      maxHeight: topHeightPx.value,
      borderRadius: `${clampNumber(resolvedConfig.value.calendarRadius, 8, 0, 24)}px`,
      background: resolvedConfig.value.calendarBackground || defaultNoticeLayoutConfig.calendarBackground,
      color: resolvedConfig.value.calendarTextColor || '#ffffff',
      '--stage-calendar-month-size': `${clampNumber(resolvedConfig.value.calendarMonthFontSize, 14, 10, 24)}px`,
      '--stage-calendar-day-size': `${clampNumber(resolvedConfig.value.calendarDayFontSize, 54, 28, 84)}px`,
      '--stage-calendar-weekday-size': `${clampNumber(resolvedConfig.value.calendarWeekdayFontSize, 17, 12, 28)}px`,
      '--stage-calendar-time-size': `${clampNumber(resolvedConfig.value.calendarTimeFontSize, 12, 10, 22)}px`,
      '--stage-calendar-time-bg': resolvedConfig.value.calendarTimeBackground || 'rgba(255,255,255,0.18)'
    }) as Record<string, string>
);

const runtimeStyleHtml = computed(() => {
  const styles = [
    resolvedConfig.value.componentCss,
    wrapCss('.workbench-stage-notice .stage-notice-calendar', resolvedConfig.value.dateCss),
    `.workbench-stage-notice .stage-notice-top > .stage-notice-copy,
.workbench-stage-notice .stage-notice-top > .stage-notice-activity,
.workbench-stage-notice .stage-notice-top > .stage-notice-calendar {
  height: var(--stage-top-height, 220px) !important;
  min-height: var(--stage-top-height, 220px) !important;
  max-height: var(--stage-top-height, 220px) !important;
}`,
    ...visibleStats.value.map((item) => wrapCss(`.workbench-stage-notice .stage-notice-stat--${cssSafeKey(item.key)}`, item.customCss))
  ]
    .map(sanitizeCss)
    .filter(Boolean)
    .join('\n');
  return styles ? `<style>${styles}</style>` : '';
});

const statStyle = (item: NoticeStatConfig) => ({
  borderColor: item.borderColor || '#d8e6f5',
  background: item.backgroundColor || '#f8fbff',
  borderRadius: `${Math.max(0, Number(resolvedConfig.value.categoryRadius || 0))}px`
});

const defaultVisualConfig = (_key: string, _label: string, accentColor: string, _backgroundColor?: string) => {
  return {
    patternMode: 'icon',
    patternPreset: 'softFlow',
    patternIcon: 'chart',
    patternText: '艺',
    patternColor: accentColor,
    patternOpacity: 0.07,
    patternScale: 0.8,
    patternPosition: 'center',
    patternImageKey: '',
    patternImageName: '',
    patternImageSize: 'cover',
    patternImageRepeat: 'no-repeat',
    glassColor: 'rgba(255,255,255,1)',
    glassOpacity: 0.9,
    glassBlur: 28,
    textureOpacity: 0.21,
    highlightOpacity: 0.5
  };
};

const nearlyEqualNumber = (value: unknown, expected: number) => Math.abs(Number(value) - expected) < 0.001;

const isPreviousFlatGlassDefault = (
  item: Pick<
    NoticeStatConfig,
    | 'patternMode'
    | 'patternPreset'
    | 'patternOpacity'
    | 'patternScale'
    | 'patternImageKey'
    | 'glassColor'
    | 'glassOpacity'
    | 'glassBlur'
    | 'textureOpacity'
    | 'highlightOpacity'
  >
) => {
  return (
    String(item.patternMode || 'preset') === 'preset' &&
    String(item.patternPreset || '') === 'pureGlass' &&
    !item.patternImageKey &&
    nearlyEqualNumber(item.patternOpacity, 0.08) &&
    nearlyEqualNumber(item.patternScale, 1.25) &&
    String(item.glassColor || '').replace(/\s/g, '') === 'rgba(255,255,255,0.62)' &&
    nearlyEqualNumber(item.glassOpacity, 0.78) &&
    nearlyEqualNumber(item.glassBlur, 16) &&
    nearlyEqualNumber(item.textureOpacity, 0.06) &&
    nearlyEqualNumber(item.highlightOpacity, 0.38)
  );
};

const isPreviousLayeredGlassDefault = (
  item: Pick<
    NoticeStatConfig,
    | 'patternMode'
    | 'patternPreset'
    | 'patternOpacity'
    | 'patternScale'
    | 'patternImageKey'
    | 'glassColor'
    | 'glassOpacity'
    | 'glassBlur'
    | 'textureOpacity'
    | 'highlightOpacity'
  >
) => {
  return (
    String(item.patternMode || 'preset') === 'preset' &&
    String(item.patternPreset || '') === 'softFlow' &&
    !item.patternImageKey &&
    nearlyEqualNumber(item.patternOpacity, 0.18) &&
    nearlyEqualNumber(item.patternScale, 1.45) &&
    String(item.glassColor || '').replace(/\s/g, '') === 'rgba(255,255,255,0.46)' &&
    nearlyEqualNumber(item.glassOpacity, 0.58) &&
    nearlyEqualNumber(item.glassBlur, 18) &&
    nearlyEqualNumber(item.textureOpacity, 0.12) &&
    nearlyEqualNumber(item.highlightOpacity, 0.5)
  );
};

const isLegacyDefaultVisualConfig = (
  item: Pick<
    NoticeStatConfig,
    | 'patternMode'
    | 'patternPreset'
    | 'patternOpacity'
    | 'patternScale'
    | 'patternImageKey'
    | 'glassColor'
    | 'glassOpacity'
    | 'glassBlur'
    | 'textureOpacity'
    | 'highlightOpacity'
  >
) => isPreviousFlatGlassDefault(item) || isPreviousLayeredGlassDefault(item);

const visualizedStat = (item: NoticeStatConfig): NoticeStatConfig => {
  const accentColor = item.accentColor || '#2563eb';
  const visual = defaultVisualConfig(item.key, item.label, accentColor, item.backgroundColor);
  const visualSource = isLegacyDefaultVisualConfig(item) ? { ...item, ...visual } : item;
  const patternMode = normalizePatternMode(visualSource.patternMode || (visualSource.patternImageKey ? 'image' : visual.patternMode));
  const patternColor = String(visualSource.patternColor || '').trim();
  return {
    ...item,
    accentColor,
    patternMode,
    patternPreset: normalizePatternPreset(visualSource.patternPreset || visual.patternPreset),
    patternIcon: normalizePatternIcon(visualSource.patternIcon || visual.patternIcon),
    patternText: normalizePatternText(visualSource.patternText || visual.patternText),
    patternColor: patternColor || visual.patternColor,
    patternOpacity: Number(visualSource.patternOpacity ?? visual.patternOpacity),
    patternScale: Number(visualSource.patternScale ?? visual.patternScale),
    patternPosition: visualSource.patternPosition || visual.patternPosition,
    patternImageKey: visualSource.patternImageKey || '',
    patternImageName: visualSource.patternImageName || '',
    patternImageSize: normalizePatternImageSize(visualSource.patternImageSize || visual.patternImageSize),
    patternImageRepeat: normalizePatternImageRepeat(visualSource.patternImageRepeat || visual.patternImageRepeat),
    glassColor: visualSource.glassColor || visual.glassColor,
    glassOpacity: Number(visualSource.glassOpacity ?? visual.glassOpacity),
    glassBlur: Number(visualSource.glassBlur ?? visual.glassBlur),
    textureOpacity: Number(visualSource.textureOpacity ?? visual.textureOpacity),
    highlightOpacity: Number(visualSource.highlightOpacity ?? visual.highlightOpacity)
  };
};

const validPatternModes = ['preset', 'icon', 'text', 'image', 'none'];
const validPatternPresets = ['softFlow', 'stageBeam', 'geoGrid', 'inkPaper', 'medalRelief', 'calligraphyMark', 'pureGlass'];
const validPatternPositions = ['center', 'right', 'left', 'bottom'];
const validPatternImageSizes = ['cover', 'contain', 'auto', '80%', '120%'];
const validPatternImageRepeats = ['no-repeat', 'repeat'];
const legacyPatternPresetMap: Record<string, string> = {
  performance: 'stageBeam',
  artwork: 'inkPaper',
  workshop: 'geoGrid',
  achievement: 'medalRelief',
  calligraphy: 'calligraphyMark',
  school: 'geoGrid',
  review: 'softFlow',
  score: 'stageBeam',
  ribbon: 'softFlow'
};
const legacyPatternIconMap: Record<string, string> = {
  performance: 'guide',
  artwork: 'example',
  workshop: 'build',
  achievement: 'star',
  calligraphy: 'edit',
  school: 'education',
  review: 'dict',
  score: 'chart',
  ribbon: 'message'
};
const normalizePatternMode = (value?: string) => (validPatternModes.includes(String(value || '')) ? String(value) : 'preset');
const normalizePatternPreset = (value?: string) => {
  const raw = String(value || '');
  const mapped = legacyPatternPresetMap[raw] || raw;
  return validPatternPresets.includes(mapped) ? mapped : 'softFlow';
};
const normalizePatternIcon = (value?: string) => {
  const raw = String(value || '').trim();
  return legacyPatternIconMap[raw] || raw || 'date';
};
const normalizePatternText = (value?: string) => {
  const text = Array.from(String(value || '').trim())
    .slice(0, 8)
    .join('');
  return text || '艺';
};
const normalizePatternPosition = (value?: string) => (validPatternPositions.includes(String(value || '')) ? String(value) : 'center');
const normalizePatternImageSize = (value?: string) => (validPatternImageSizes.includes(String(value || '')) ? String(value) : 'cover');
const normalizePatternImageRepeat = (value?: string) => (validPatternImageRepeats.includes(String(value || '')) ? String(value) : 'no-repeat');
const patternModeClass = (value?: string) => `stage-notice-stat__pattern-mode--${normalizePatternMode(value)}`;
const patternPresetClass = (value?: string) => `stage-notice-stat__pattern--${normalizePatternPreset(value)}`;
const patternPositionClass = (value?: string) => `stage-notice-stat__pattern-position--${normalizePatternPosition(value)}`;
const isDefaultStatsAreaBackground = (value?: string) => {
  const background = String(value || '')
    .replace(/\s/g, '')
    .toLowerCase();
  return !background || background === '#f8fbff' || background === 'rgb(248,251,255)';
};
const patternPositionValue = (value?: string) => {
  switch (normalizePatternPosition(value)) {
    case 'right':
      return 'right center';
    case 'left':
      return 'left center';
    case 'bottom':
      return 'center bottom';
    default:
      return 'center center';
  }
};
const patternStyle = (item: NoticeStatConfig) => {
  const opacity = String(clampNumber(item.patternOpacity, 0.07, 0, 0.75));
  const patternScale = clampNumber(item.patternScale, 0.8, 0.8, 3);
  const patternHoverScale = patternScale * normalizedHoverScale.value;
  if (normalizePatternMode(item.patternMode) === 'image') {
    return {
      opacity,
      backgroundImage: item.patternImageKey ? `url("${workbenchAssetUrl(item.patternImageKey)}")` : 'none',
      backgroundSize: normalizePatternImageSize(item.patternImageSize),
      backgroundRepeat: normalizePatternImageRepeat(item.patternImageRepeat),
      backgroundPosition: patternPositionValue(item.patternPosition),
      '--stage-stat-pattern-scale': String(patternScale),
      '--stage-stat-pattern-hover-scale': String(patternHoverScale)
    };
  }
  return {
    color: item.patternColor || item.accentColor || '#2563eb',
    opacity,
    '--stage-stat-pattern-scale': String(patternScale),
    '--stage-stat-pattern-hover-scale': String(patternHoverScale)
  };
};

const glassStyle = (item: NoticeStatConfig) => ({
  '--stage-stat-glass-color': item.glassColor || 'rgba(255,255,255,1)',
  '--stage-stat-glass-opacity': String(clampNumber(item.glassOpacity, 0.9, 0, 1)),
  '--stage-stat-glass-blur': `${clampNumber(item.glassBlur, 28, 0, 28)}px`,
  '--stage-stat-texture-opacity': normalizePatternMode(item.patternMode) === 'none' ? '0' : String(clampNumber(item.textureOpacity, 0.21, 0, 0.65)),
  '--stage-stat-highlight-opacity': String(clampNumber(item.highlightOpacity, 0.5, 0, 1))
});

const statValue = (key: string) => values.value[key] || 0;
const secondaryVisible = (item: NoticeStatConfig) => item.secondary?.visible === true && !!item.secondary.valueKey;
const secondaryValue = (item: NoticeStatConfig) => values.value[item.secondary?.valueKey || ''] || 0;
const secondaryStyle = (item: NoticeStatConfig) => ({
  color: item.secondary?.textColor || item.titleColor || '#5c7087',
  fontSize: `${clampNumber(item.secondary?.fontSize, 13, 10, 20)}px`
});

const parseConfig = (value?: string): NoticeStatsConfig => {
  let custom: Partial<NoticeStatsConfig> = {};
  if (value) {
    try {
      custom = JSON.parse(value);
    } catch {
      custom = {};
    }
  }
  const hoverEffectMode = normalizeHoverEffectMode(custom.hoverEffectMode, custom.hoverScaleEnabled ?? defaultConfig.value.hoverScaleEnabled);
  return {
    ...defaultConfig.value,
    ...custom,
    hoverEffectMode,
    hoverScaleEnabled: hoverEffectMode !== 'none',
    hoverScale: normalizeHoverScale(custom.hoverScale ?? defaultConfig.value.hoverScale),
    categories: mergeItems(defaultConfig.value.categories, custom.categories).map(visualizedStat)
  };
};

const mergeItems = (base: NoticeStatConfig[], custom?: NoticeStatConfig[]) => {
  const customMap = new Map((custom || []).map((item) => [item.key, item]));
  const merged = base.map((item) => {
    const customItem = customMap.get(item.key);
    if (!customItem) return { ...item };
    const secondary = item.secondary || customItem.secondary ? { ...(item.secondary || {}), ...(customItem.secondary || {}) } : undefined;
    return { ...item, ...customItem, ...(secondary ? { secondary } : {}) };
  });
  (custom || []).forEach((item) => {
    if (!merged.some((baseItem) => baseItem.key === item.key)) {
      merged.push(item);
    }
  });
  return merged;
};

const escapeHtml = (value: string) =>
  String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

const wrapCss = (selector: string, value?: string) => {
  const css = String(value || '').trim();
  if (!css) return '';
  return css.includes('{') ? css : `${selector} { ${css} }`;
};

const sanitizeCss = (value?: string) =>
  String(value || '')
    .replace(/<\/style/gi, '<\\/style')
    .replace(/<script/gi, '/* script')
    .replace(/<\/script>/gi, 'script */');

const cssSafeKey = (value: string) => String(value || '').replace(/[^a-zA-Z0-9_-]/g, '-');

const statusCount = (summary: any, status: string) => Number((summary.statusItems || []).find((item: any) => item.key === status)?.count || 0);

const loadCurrentActivityName = async () => {
  if (props.mode === 'reviewer') {
    const { data } = await getReviewWorkbench();
    return data?.activityName;
  }
  if (props.mode !== 'auditor') {
    const { data } = await getUploadOverview({});
    return data?.activityName || data?.activities?.[0]?.activityName;
  }
  const { data } = await listAuditActivityOptions();
  const activity = (data || [])[0];
  if (activity?.id) {
    await listAuditCategoryOptions(activity.id);
  }
  return activity?.activityName;
};

const loadNoticeAndActivity = async () => {
  const userType = props.mode === 'reviewer' ? 'expert' : props.mode;
  const [noticeRes, activityRes] = await Promise.allSettled([getRegisterNotice({ userType }), loadCurrentActivityName()]);
  if (noticeRes.status === 'fulfilled') {
    notices.value = noticeRes.value.data || [];
  }
  if (activityRes.status === 'fulfilled') {
    activityName.value = activityRes.value || activityName.value;
  }
};

const loadAdminStats = async () => {
  const [summaryRes, schoolRes]: any[] = await Promise.all([getUploadSummary({}), listSchoolInfo({ pageNum: 1, pageSize: 1 })]);
  const summary = summaryRes.data || {};
  const schoolTotal = Number(schoolRes.total || schoolRes.rows?.length || 0);
  const submittedSchools = Number(summary.schoolCount || 0);
  values.value = {
    school_total: schoolTotal,
    submitted_schools: submittedSchools,
    unsubmitted_schools: Math.max(0, schoolTotal - submittedSchools),
    project_total: Number(summary.totalCount || 0),
    pending_audit: statusCount(summary, 'submitted'),
    audit_passed: statusCount(summary, 'audit_passed')
  };
};

const loadAuditStats = async () => {
  const [allRes, pendingRes, passedRes, returnedRes, workbenchRes]: any[] = await Promise.all([
    listAuditProject({ pageNum: 1, pageSize: 1 }),
    listAuditProject({ pageNum: 1, pageSize: 1, status: 'submitted' }),
    listAuditProject({ pageNum: 1, pageSize: 1, status: 'audit_passed' }),
    listAuditProject({ pageNum: 1, pageSize: 1, status: 'returned' }),
    getAuditWorkbench()
  ]);
  const passed = Number(passedRes.total || 0);
  const returned = Number(returnedRes.total || 0);
  const myStats = workbenchRes.data?.myStats || {};
  values.value = {
    assigned_total: Number(allRes.total || 0),
    pending_audit: Number(pendingRes.total || 0),
    audited_total: passed + returned,
    audit_passed: passed,
    returned,
    my_audited_total: Number(myStats.totalCount || 0),
    my_audit_passed: Number(myStats.passCount || 0),
    my_returned: Number(myStats.returnCount || 0)
  };
};

const loadReviewStats = async () => {
  const { data } = await getReviewWorkbench();
  values.value = {
    assigned_total: Number(data?.stats?.assigned_total || 0),
    pending_score: Number(data?.stats?.pending_score || 0),
    draft_score: Number(data?.stats?.draft_score || 0),
    submitted_score: Number(data?.stats?.submitted_score || 0),
    locked_by_other: Number(data?.stats?.locked_by_other || 0)
  };
};

const loadData = async () => {
  loading.value = true;
  try {
    await loadNoticeAndActivity();
    if (props.mode === 'admin') {
      await loadAdminStats();
    } else if (props.mode === 'reviewer') {
      await loadReviewStats();
    } else {
      await loadAuditStats();
    }
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadData();
  clockTimer = window.setInterval(() => {
    now.value = new Date();
  }, 1000);
});
onActivated(loadData);
onBeforeUnmount(() => {
  if (clockTimer) {
    window.clearInterval(clockTimer);
  }
});
</script>

<style scoped lang="scss">
.workbench-stage-notice {
  padding: 0;
  overflow: visible;
}

.stage-notice-top {
  display: grid;
  gap: 18px;
  align-items: stretch;
  padding: 0;
  background: transparent;
}

.stage-notice-copy,
.stage-notice-activity,
.stage-notice-calendar {
  min-width: 0;
  height: var(--stage-top-height, 220px);
  min-height: var(--stage-top-height, 220px);
  max-height: var(--stage-top-height, 220px);
  box-sizing: border-box;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.035);
}

.stage-notice-copy {
  padding: 18px;
  border: 1px solid #e8edf5;
  background: #ffffff;
}

.stage-notice-copy span,
.stage-notice-activity span {
  color: #2563eb;
  font-size: 12px;
  font-weight: 900;
}

.stage-notice-copy strong {
  display: block;
  margin-top: 8px;
  color: #102a43;
  font-size: 18px;
  line-height: 1.35;
}

.stage-notice-copy p {
  margin: 10px 0 0;
  color: #40566f;
  font-size: 14px;
  line-height: 1.7;
}

.stage-notice-activity {
  display: grid;
  place-items: center;
  align-content: center;
  padding: 22px;
  border: 1px solid #e8edf5;
  background: #ffffff;
  text-align: center;
}

.stage-notice-activity h2 {
  margin: 10px 0 0;
  color: #0f2f5f;
  font-size: 26px;
  line-height: 1.3;
  font-weight: 950;
}

.stage-notice-calendar {
  padding: 20px;
  align-self: stretch;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 7px;
  color: #ffffff;
  background: var(--workbench-calendar-bg, linear-gradient(180deg, #2563eb 0%, #0f62b9 100%));
}

.stage-notice-calendar div {
  font-size: var(--stage-calendar-month-size, 14px);
  font-weight: 800;
}

.stage-notice-calendar strong {
  font-size: var(--stage-calendar-day-size, 54px);
  line-height: 1;
  font-weight: 950;
}

.stage-notice-calendar span {
  font-size: var(--stage-calendar-weekday-size, 17px);
  font-weight: 900;
}

.stage-notice-calendar small {
  padding: 5px 9px;
  border-radius: 999px;
  background: var(--stage-calendar-time-bg, rgba(255, 255, 255, 0.18));
  font-size: var(--stage-calendar-time-size, 12px);
}

.stage-notice-stats {
  display: grid;
  margin-top: 16px;
  padding: 0;
  border-top: 0;
}

.stage-notice-stat {
  position: relative;
  min-height: 96px;
  padding: 16px 14px 16px 18px;
  display: grid;
  align-content: center;
  gap: 9px;
  overflow: hidden;
  border: 1px solid #d8e6f5;
  box-shadow: none;
  transition: none;
}

.stage-notice-stat__pattern,
.stage-notice-stat__glass {
  position: absolute;
  pointer-events: none;
}

.stage-notice-stat__pattern {
  z-index: 0;
  color: #2563eb;
  transform: scale(var(--stage-stat-pattern-scale, 1.72));
  transform-origin: center;
  transition: transform 0.18s ease;
}

.stage-notice-stat__pattern::before,
.stage-notice-stat__pattern::after {
  position: absolute;
  inset: 0;
  content: '';
}

.stage-notice-stat__pattern-position--center .stage-notice-stat__pattern {
  inset: -24%;
}

.stage-notice-stat__pattern-position--right .stage-notice-stat__pattern {
  top: -28%;
  right: -42%;
  width: 92%;
  height: 156%;
}

.stage-notice-stat__pattern-position--left .stage-notice-stat__pattern {
  top: -28%;
  left: -42%;
  width: 92%;
  height: 156%;
}

.stage-notice-stat__pattern-position--bottom .stage-notice-stat__pattern {
  left: -18%;
  right: -18%;
  bottom: -62%;
  height: 138%;
}

.stage-notice-stat__pattern-mode--image .stage-notice-stat__pattern {
  background-color: transparent;
}

.stage-notice-stat__pattern-mode--image .stage-notice-stat__pattern::before,
.stage-notice-stat__pattern-mode--image .stage-notice-stat__pattern::after {
  display: none;
}

.stage-notice-stat__pattern-mode--icon .stage-notice-stat__pattern::before,
.stage-notice-stat__pattern-mode--icon .stage-notice-stat__pattern::after {
  display: none;
}

.stage-notice-stat__pattern-mode--text .stage-notice-stat__pattern::before,
.stage-notice-stat__pattern-mode--text .stage-notice-stat__pattern::after {
  display: none;
}

.stage-notice-stat__pattern :deep(.stage-notice-stat__svg-icon) {
  width: 100%;
  height: 100%;
  color: currentColor;
  fill: currentColor;
}

.stage-notice-stat__text-symbol {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  overflow: hidden;
  color: currentColor;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
  font-size: 64px;
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0;
  white-space: nowrap;
}

.stage-notice-stat__pattern--softFlow .stage-notice-stat__pattern::before {
  background:
    radial-gradient(ellipse at 18% 28%, currentColor 0 13%, transparent 14%),
    radial-gradient(ellipse at 80% 76%, currentColor 0 16%, transparent 17%),
    linear-gradient(126deg, transparent 0 34%, currentColor 35% 42%, transparent 43% 100%);
  filter: blur(1.2px);
}

.stage-notice-stat__pattern--softFlow .stage-notice-stat__pattern::after {
  background:
    linear-gradient(112deg, transparent 0 32%, currentColor 33% 36%, transparent 37% 100%),
    linear-gradient(138deg, transparent 0 58%, currentColor 59% 61%, transparent 62% 100%);
  filter: blur(0.8px);
  opacity: 0.48;
}

.stage-notice-stat__pattern--stageBeam .stage-notice-stat__pattern::before {
  background:
    radial-gradient(circle at 50% 64%, currentColor 0 8%, transparent 9%),
    conic-gradient(
      from 205deg at 50% 78%,
      transparent 0 16deg,
      currentColor 18deg 26deg,
      transparent 28deg 72deg,
      currentColor 74deg 82deg,
      transparent 84deg
    );
}

.stage-notice-stat__pattern--stageBeam .stage-notice-stat__pattern::after {
  background:
    linear-gradient(68deg, transparent 0 44%, currentColor 45% 48%, transparent 49%),
    linear-gradient(112deg, transparent 0 44%, currentColor 45% 48%, transparent 49%);
  opacity: 0.62;
}

.stage-notice-stat__pattern--geoGrid .stage-notice-stat__pattern::before {
  background: linear-gradient(90deg, currentColor 1px, transparent 1px), linear-gradient(0deg, currentColor 1px, transparent 1px);
  background-size: 26px 26px;
}

.stage-notice-stat__pattern--geoGrid .stage-notice-stat__pattern::after {
  background:
    radial-gradient(circle at 28% 32%, currentColor 0 7%, transparent 8%), radial-gradient(circle at 72% 36%, currentColor 0 6%, transparent 7%),
    radial-gradient(circle at 58% 72%, currentColor 0 8%, transparent 9%);
}

.stage-notice-stat__pattern--inkPaper .stage-notice-stat__pattern::before {
  background:
    radial-gradient(ellipse at 32% 38%, currentColor 0 15%, transparent 16%),
    radial-gradient(ellipse at 62% 62%, currentColor 0 11%, transparent 12%),
    linear-gradient(118deg, transparent 0 42%, currentColor 43% 50%, transparent 51%);
  filter: blur(1.2px);
}

.stage-notice-stat__pattern--inkPaper .stage-notice-stat__pattern::after {
  background: radial-gradient(ellipse at 76% 30%, currentColor 0 7%, transparent 8%), linear-gradient(90deg, currentColor 1px, transparent 1px);
  background-size:
    100% 100%,
    18px 18px;
  opacity: 0.4;
}

.stage-notice-stat__pattern--medalRelief .stage-notice-stat__pattern::before {
  background:
    conic-gradient(from 18deg, currentColor 0 10deg, transparent 10deg 26deg, currentColor 26deg 36deg, transparent 36deg 52deg),
    radial-gradient(circle at 50% 50%, transparent 0 24%, currentColor 25% 31%, transparent 32%);
}

.stage-notice-stat__pattern--medalRelief .stage-notice-stat__pattern::after {
  top: 43%;
  background:
    linear-gradient(72deg, transparent 0 45%, currentColor 46% 49%, transparent 50%),
    linear-gradient(108deg, transparent 0 45%, currentColor 46% 49%, transparent 50%);
}

.stage-notice-stat__pattern--calligraphyMark .stage-notice-stat__pattern::before {
  border: 10px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  transform: rotate(-24deg);
}

.stage-notice-stat__pattern--calligraphyMark .stage-notice-stat__pattern::after {
  background:
    linear-gradient(118deg, transparent 0 42%, currentColor 43% 54%, transparent 55%),
    radial-gradient(ellipse at 66% 72%, currentColor 0 13%, transparent 14%);
  filter: blur(0.6px);
}

.stage-notice-stat__pattern--pureGlass .stage-notice-stat__pattern::before {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0.1) 48%, transparent 70%),
    repeating-linear-gradient(135deg, currentColor 0 1px, transparent 1px 18px);
  filter: blur(0.2px);
}

.stage-notice-stat__pattern--pureGlass .stage-notice-stat__pattern::after {
  display: none;
}

.stage-notice-stat__pattern--performance .stage-notice-stat__pattern::before {
  background:
    radial-gradient(ellipse at 50% 82%, transparent 35%, currentColor 36% 39%, transparent 40%),
    radial-gradient(circle at 50% 68%, currentColor 0 8%, transparent 9%),
    linear-gradient(63deg, transparent 0 43%, currentColor 44% 47%, transparent 48% 100%),
    linear-gradient(117deg, transparent 0 43%, currentColor 44% 47%, transparent 48% 100%);
}

.stage-notice-stat__pattern--performance .stage-notice-stat__pattern::after {
  background:
    radial-gradient(circle at 30% 25%, currentColor 0 4%, transparent 5%), radial-gradient(circle at 70% 25%, currentColor 0 4%, transparent 5%),
    linear-gradient(90deg, transparent 0 18%, currentColor 19% 21%, transparent 22% 78%, currentColor 79% 81%, transparent 82%);
  transform: rotate(-8deg);
}

.stage-notice-stat__pattern--artwork .stage-notice-stat__pattern::before {
  background:
    linear-gradient(135deg, transparent 0 26%, currentColor 27% 34%, transparent 35% 100%),
    linear-gradient(25deg, transparent 0 45%, currentColor 46% 53%, transparent 54% 100%),
    radial-gradient(ellipse at 64% 35%, currentColor 0 13%, transparent 14%);
}

.stage-notice-stat__pattern--artwork .stage-notice-stat__pattern::after {
  background:
    radial-gradient(ellipse at 34% 72%, currentColor 0 9%, transparent 10%), radial-gradient(ellipse at 48% 58%, currentColor 0 7%, transparent 8%),
    radial-gradient(ellipse at 58% 73%, currentColor 0 10%, transparent 11%);
  filter: blur(1px);
}

.stage-notice-stat__pattern--workshop .stage-notice-stat__pattern::before {
  background: linear-gradient(90deg, currentColor 1px, transparent 1px), linear-gradient(0deg, currentColor 1px, transparent 1px);
  background-size: 28px 28px;
}

.stage-notice-stat__pattern--workshop .stage-notice-stat__pattern::after {
  background:
    radial-gradient(circle at 28% 32%, currentColor 0 8%, transparent 9%), radial-gradient(circle at 72% 36%, currentColor 0 7%, transparent 8%),
    radial-gradient(circle at 58% 72%, currentColor 0 9%, transparent 10%),
    linear-gradient(42deg, transparent 0 47%, currentColor 48% 50%, transparent 51% 100%);
}

.stage-notice-stat__pattern--achievement .stage-notice-stat__pattern::before {
  background:
    conic-gradient(from 18deg, currentColor 0 10deg, transparent 10deg 26deg, currentColor 26deg 36deg, transparent 36deg 52deg),
    radial-gradient(circle at 50% 50%, transparent 0 24%, currentColor 25% 31%, transparent 32%);
}

.stage-notice-stat__pattern--achievement .stage-notice-stat__pattern::after {
  top: 43%;
  background:
    linear-gradient(72deg, transparent 0 45%, currentColor 46% 49%, transparent 50% 100%),
    linear-gradient(108deg, transparent 0 45%, currentColor 46% 49%, transparent 50% 100%);
}

.stage-notice-stat__pattern--calligraphy .stage-notice-stat__pattern::before {
  border: 10px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  transform: rotate(-24deg);
}

.stage-notice-stat__pattern--calligraphy .stage-notice-stat__pattern::after {
  background:
    linear-gradient(118deg, transparent 0 42%, currentColor 43% 54%, transparent 55% 100%),
    radial-gradient(ellipse at 66% 72%, currentColor 0 13%, transparent 14%);
  filter: blur(0.6px);
}

.stage-notice-stat__pattern--school .stage-notice-stat__pattern::before {
  background:
    linear-gradient(
      90deg,
      transparent 0 12%,
      currentColor 13% 18%,
      transparent 19% 31%,
      currentColor 32% 37%,
      transparent 38% 50%,
      currentColor 51% 56%,
      transparent 57% 69%,
      currentColor 70% 75%,
      transparent 76%
    ),
    linear-gradient(0deg, currentColor 0 7%, transparent 8% 100%), linear-gradient(135deg, transparent 0 44%, currentColor 45% 55%, transparent 56%);
}

.stage-notice-stat__pattern--review .stage-notice-stat__pattern::before {
  background:
    radial-gradient(circle at 30% 30%, currentColor 0 9%, transparent 10%), radial-gradient(circle at 70% 70%, currentColor 0 9%, transparent 10%),
    linear-gradient(45deg, transparent 0 45%, currentColor 46% 51%, transparent 52% 100%);
}

.stage-notice-stat__pattern--review .stage-notice-stat__pattern::after {
  border: 8px solid currentColor;
  border-left: 0;
  border-bottom: 0;
  transform: rotate(45deg) scale(0.64);
}

.stage-notice-stat__pattern--score .stage-notice-stat__pattern::before {
  background: linear-gradient(
    90deg,
    currentColor 0 9%,
    transparent 10% 20%,
    currentColor 21% 34%,
    transparent 35% 45%,
    currentColor 46% 64%,
    transparent 65% 75%,
    currentColor 76% 100%
  );
  clip-path: polygon(0 72%, 12% 52%, 24% 66%, 38% 28%, 52% 48%, 68% 18%, 82% 42%, 100% 12%, 100% 100%, 0 100%);
}

.stage-notice-stat__pattern--ribbon .stage-notice-stat__pattern::before {
  background:
    radial-gradient(ellipse at 28% 30%, currentColor 0 16%, transparent 17%),
    radial-gradient(ellipse at 72% 70%, currentColor 0 18%, transparent 19%),
    linear-gradient(126deg, transparent 0 38%, currentColor 39% 45%, transparent 46% 100%);
}

.stage-notice-stat__pattern--ribbon .stage-notice-stat__pattern::after {
  border: 7px solid currentColor;
  border-left-color: transparent;
  border-bottom-color: transparent;
  border-radius: 42% 58% 45% 55%;
  transform: rotate(26deg);
}

.stage-notice-stat__glass {
  inset: 0;
  z-index: 1;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.5), rgba(255, 255, 255, 0.12) 38%, rgba(255, 255, 255, 0.26) 72%),
    var(--stage-stat-glass-color, rgba(255, 255, 255, 1));
  opacity: var(--stage-stat-glass-opacity, 0.9);
  backdrop-filter: blur(var(--stage-stat-glass-blur, 28px)) saturate(1.32) contrast(1.04);
  -webkit-backdrop-filter: blur(var(--stage-stat-glass-blur, 28px)) saturate(1.32) contrast(1.04);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.76),
    inset 0 -1px 0 rgba(148, 163, 184, 0.16),
    inset 14px 0 26px rgba(255, 255, 255, 0.16);
}

.stage-notice-stat__glass::before,
.stage-notice-stat__glass::after {
  position: absolute;
  inset: 0;
  content: '';
}

.stage-notice-stat__glass::before {
  background:
    radial-gradient(circle at 22% 18%, rgba(255, 255, 255, 0.54), transparent 19%),
    linear-gradient(90deg, rgba(255, 255, 255, 0.48) 1px, transparent 1px), linear-gradient(0deg, rgba(255, 255, 255, 0.34) 1px, transparent 1px);
  background-size:
    100% 100%,
    18px 18px,
    18px 18px;
  opacity: var(--stage-stat-texture-opacity, 0.21);
}

.stage-notice-stat__glass::after {
  border: 1px solid rgba(255, 255, 255, 0.78);
  box-shadow:
    inset 0 0 24px rgba(255, 255, 255, var(--stage-stat-highlight-opacity, 0.5)),
    inset 0 -12px 22px rgba(148, 163, 184, 0.08);
  opacity: var(--stage-stat-highlight-opacity, 0.5);
}

.stage-notice-stat strong {
  position: relative;
  z-index: 2;
}

.stage-notice-stat__accent {
  position: absolute;
  z-index: 3;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 4px;
  border-radius: 999px;
}

.stage-notice-stat__numbers {
  position: relative;
  z-index: 2;
  display: inline-block;
  width: fit-content;
  transform-origin: left center;
  transition: transform 0.18s ease;
}

.stage-notice-stat__numbers b {
  font-size: 28px;
  line-height: 1;
  font-weight: 950;
}

.stage-notice-stat__secondary {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 5px;
  line-height: 1.3;
  font-weight: 700;
}

.stage-notice-stat__secondary b {
  font-size: 1.08em;
  line-height: 1;
  font-weight: 950;
}

.stage-notice-stat.is-hover-text-enabled:hover .stage-notice-stat__numbers,
.stage-notice-stat.is-hover-text-enabled:focus-visible .stage-notice-stat__numbers {
  transform: scale(var(--stage-stat-hover-scale, 1.08));
}

.stage-notice-stat.is-hover-pattern-enabled:hover .stage-notice-stat__pattern,
.stage-notice-stat.is-hover-pattern-enabled:focus-visible .stage-notice-stat__pattern {
  transform: scale(var(--stage-stat-pattern-hover-scale, var(--stage-stat-pattern-scale, 1.72)));
}

@media (max-width: 1200px) {
  .stage-notice-top,
  .stage-notice-stats {
    grid-template-columns: 1fr;
  }
}
</style>
