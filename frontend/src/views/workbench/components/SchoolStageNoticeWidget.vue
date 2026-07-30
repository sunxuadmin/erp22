<template>
  <section class="school-stage-notice">
    <div class="stage-notice-top" :style="topLayoutStyle">
      <div class="stage-notice-copy">
        <span>公告</span>
        <strong>{{ noticeTitle }}</strong>
        <p v-html="noticeContentHtml"></p>
        <div v-if="showNoticeQuotaSummary" class="stage-notice-quota" :style="quotaSummaryStyle">
          <template v-if="quotaSummaryTokens.length">
            <span class="stage-notice-quota__template">
              <template v-for="token in quotaSummaryTokens" :key="token.key">
                <strong v-if="token.variable">{{ token.value }}</strong>
                <template v-else>{{ token.text }}</template>
              </template>
            </span>
          </template>
          <template v-else>
            <span v-for="item in quotaSummaryItems" :key="item.key" class="stage-notice-quota__item">
              <em>{{ item.label }}</em>
              <strong>{{ item.value }}</strong>
            </span>
          </template>
        </div>
        <div v-if="showNoticeRatioSummary" class="stage-notice-ratio-summary" :style="quotaRatioStyle">
          <span v-for="item in noticeRatioItems" :key="item.key">
            <em>{{ item.label }}</em>
            <strong>{{ item.count }}</strong>
            <small v-if="hasNumber(item.percent)">{{ item.percent }}%</small>
          </span>
        </div>
        <div v-if="noticeAttachments.length" class="stage-notice-downloads">
          <el-button
            v-for="(attachment, index) in noticeAttachments"
            :key="attachment.ossId || index"
            size="small"
            type="primary"
            plain
            icon="Download"
            @click="downloadNoticeAttachment(attachment)"
          >
            {{ attachmentName(attachment, index) }}
          </el-button>
        </div>
      </div>

      <div class="stage-notice-activity">
        <span>当前活动</span>
        <h2>{{ activityName }}</h2>
      </div>

      <aside class="stage-notice-calendar" :style="calendarCardStyle" aria-label="当前日期">
        <div>{{ calendarInfo.monthText }}</div>
        <strong>{{ calendarInfo.day }}</strong>
        <span>{{ calendarInfo.weekday }}</span>
        <small v-if="resolvedConfig.calendarShowTime !== false">{{ calendarInfo.timeText }}</small>
      </aside>
    </div>

    <el-skeleton v-if="loading" :rows="3" animated />
    <template v-else>
      <div class="stage-notice-stats" :style="statsGridStyle">
        <article
          v-for="item in groupSummaries"
          :key="item.key"
          class="stage-notice-stat"
          :class="[
            `stage-notice-stat--${cssSafeKey(item.key)}`,
            patternModeClass(item.patternMode),
            patternPresetClass(item.patternPreset),
            patternPositionClass(item.patternPosition),
            {
              'is-clickable': item.linkEnabled !== false,
              'is-hover-text-enabled': isHoverTextEnabled,
              'is-hover-pattern-enabled': isHoverPatternEnabled,
              'has-quota-hover': item.quotaDisplayMode === 'hover' && quotaItems(item).length > 0,
              'has-quota-top-right': item.quotaDisplayMode === 'always' && item.quotaPosition === 'topRight' && quotaItems(item).length > 0,
              'is-quota-open': openQuotaCardKey === item.key
            }
          ]"
          :style="categoryStyle(item)"
          :aria-label="`${item.name}报送入口`"
          :role="item.linkEnabled !== false ? 'button' : undefined"
          :tabindex="item.linkEnabled !== false ? 0 : undefined"
          @click="openGroupSummary(item)"
          @keydown.enter.prevent="openGroupSummary(item)"
          @keydown.space.prevent="openGroupSummary(item)"
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
          <button
            v-if="item.quotaDisplayMode === 'hover' && quotaItems(item).length"
            class="stage-notice-stat__quota-trigger"
            type="button"
            :aria-expanded="openQuotaCardKey === item.key"
            :aria-label="`查看${item.name}限额`"
            @click.stop="toggleQuotaCard(item.key)"
          >
            限额
          </button>
          <div
            v-if="item.quotaDisplayMode === 'always' && item.quotaPosition === 'topRight' && quotaItems(item).length"
            class="stage-notice-stat__quota-inline stage-notice-stat__quota-inline--top-right"
          >
            <span v-for="quotaItem in quotaItems(item)" :key="quotaItem.key">{{ quotaItem.label }} {{ quotaItem.value }}</span>
          </div>
          <strong :style="{ color: item.titleColor, fontSize: `${item.fontSize}px` }">{{ item.name }}</strong>
          <span
            class="stage-notice-stat__numbers"
            :style="{ color: item.numberColor }"
            :title="`${item.name}：草稿 ${item.draft} / 提交 ${item.submitted} / 通过 ${item.completed}`"
          >
            <b>{{ item.draft }}</b
            ><em>/</em><b>{{ item.submitted }}</b
            ><em>/</em><b>{{ item.completed }}</b>
          </span>
          <div
            v-if="item.quotaDisplayMode === 'always' && item.quotaPosition === 'belowStats' && quotaItems(item).length"
            class="stage-notice-stat__quota-inline stage-notice-stat__quota-inline--below"
          >
            <span v-for="quotaItem in quotaItems(item)" :key="quotaItem.key">{{ quotaItem.label }} {{ quotaItem.value }}</span>
          </div>
          <small class="stage-notice-stat__legend">草稿 / 提交 / 通过</small>
          <div
            v-if="item.quotaDisplayMode === 'always' && item.quotaPosition === 'bottom' && quotaItems(item).length"
            class="stage-notice-stat__quota-inline stage-notice-stat__quota-inline--bottom"
          >
            <span v-for="quotaItem in quotaItems(item)" :key="quotaItem.key">{{ quotaItem.label }} {{ quotaItem.value }}</span>
          </div>
          <div v-if="item.quotaDisplayMode === 'hover' && quotaItems(item).length" class="stage-notice-stat__quota-popover" role="status" @click.stop>
            <span v-for="quotaItem in quotaItems(item)" :key="quotaItem.key">
              <em>{{ quotaItem.label }}</em>
              <strong>{{ quotaItem.value }}</strong>
            </span>
          </div>
        </article>
      </div>
    </template>
    <div v-if="runtimeStyleHtml" v-html="runtimeStyleHtml"></div>
  </section>
</template>

<script setup lang="ts">
import { listAvailableActivity, listSchoolCategoryCatalog } from '@/api/crehn/activity';
import { getMyProjectQuotaOverview } from '@/api/crehn/project';
import { ActivityCategoryVO, ProjectQuotaOverviewItemVO, ProjectQuotaOverviewVO, ProjectQuotaRatioBriefVO } from '@/api/crehn/types';
import { getRegisterNotice } from '@/api/login';
import { workbenchAssetUrl } from '@/api/system/workbench';
import { useAppStore } from '@/store/modules/app';
import { useUserStore } from '@/store/modules/user';
import {
  ActivityCategoryTreeNode,
  buildActivityCategoryTree,
  isCategoryGroup,
  isCategoryMenuAvailable,
  isCategoryMenuVisible,
  sortCategoryNodes
} from '@/utils/artCategory';

const props = defineProps<{ title: string; configJson?: string }>();
const { proxy } = getCurrentInstance() as ComponentInternalInstance;

type GroupSummary = {
  key: string;
  name: string;
  activityId?: string | number;
  sourceType?: 'group' | 'category';
  sourceId?: string | number;
  categoryId?: string | number;
  categoryCode?: string;
  categoryName?: string;
  quota?: number;
  used: number;
  remaining?: number;
  total: number;
  draft: number;
  submitted: number;
  completed: number;
  ratioItems: ProjectQuotaRatioBriefVO[];
  fontSize: number;
  titleColor: string;
  numberColor: string;
  backgroundColor: string;
  borderColor: string;
  accentColor: string;
  customCss: string;
  patternMode: string;
  patternPreset: string;
  patternIcon: string;
  patternText: string;
  patternColor: string;
  patternOpacity: number;
  patternScale: number;
  patternPosition: string;
  patternImageKey: string;
  patternImageName: string;
  patternImageSize: string;
  patternImageRepeat: string;
  glassColor: string;
  glassOpacity: number;
  glassBlur: number;
  textureOpacity: number;
  highlightOpacity: number;
  showQuota?: boolean;
  showSubmitted?: boolean;
  linkEnabled?: boolean;
  quotaDisplayMode: 'none' | 'always' | 'hover';
  quotaFields: string[];
  quotaPosition: 'topRight' | 'belowStats' | 'bottom';
  routeGroupName: string;
};

type StageCategoryConfig = {
  key: string;
  label: string;
  activityId?: string | number;
  sourceType?: 'group' | 'category';
  sourceId?: string | number;
  sourceCode?: string;
  sourceName?: string;
  visible?: boolean;
  order?: number;
  aliases?: string[];
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
  showQuota?: boolean;
  showSubmitted?: boolean;
  linkEnabled?: boolean;
  quotaDisplayMode?: 'none' | 'always' | 'hover';
  quotaFields?: string[];
  quotaPosition?: 'topRight' | 'belowStats' | 'bottom';
};

type SchoolTypeLabelReplacement = {
  source: string;
  target: string;
};

type HoverEffectMode = 'none' | 'text' | 'pattern' | 'textPattern';

type StageNoticeConfig = {
  categoryConfigMode?: 'legacy' | 'activity';
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
  quotaSummaryEnabled: boolean;
  quotaSummaryFields: string[];
  quotaSummaryTemplate: string;
  quotaSummaryFontSize: number;
  quotaSummaryTextColor: string;
  quotaSummaryNumberColor: string;
  quotaSummaryBackground: string;
  quotaSummaryBorderColor: string;
  quotaSummaryRadius: number;
  quotaSummaryGap: number;
  quotaSummaryCss: string;
  quotaRatioEnabled: boolean;
  quotaRatioMaxItems: number;
  quotaRatioFontSize: number;
  quotaRatioTextColor: string;
  quotaRatioNumberColor: string;
  quotaRatioBackground: string;
  quotaRatioBorderColor: string;
  quotaRatioRadius: number;
  quotaRatioCss: string;
  cardRatioEnabled: boolean;
  schoolTypeLabelReplacements: SchoolTypeLabelReplacement[];
  categoryColumns: number;
  categoryGap: number;
  categoryRadius: number;
  hoverEffectMode: HoverEffectMode;
  hoverScaleEnabled: boolean;
  hoverScale: number;
  categoryAreaBackground: string;
  componentCss: string;
  dateCss: string;
  categories: StageCategoryConfig[];
};

type NoticeAttachment = {
  ossId?: string | number;
  fileName?: string;
  originalName?: string;
  url?: string;
};

type QuotaSummaryToken = {
  key: string;
  text?: string;
  value?: string;
  variable?: boolean;
};

const appStore = useAppStore();
const userStore = useUserStore();
const loading = ref(false);
const notices = ref<any[]>([]);
const activityName = ref('2026 大学生艺术展演活动');
const activeActivityId = ref<string | number>();
const categories = ref<ActivityCategoryVO[]>([]);
const quotaOverview = ref<ProjectQuotaOverviewVO>({});
const openQuotaCardKey = ref('');
const now = ref(new Date());
let clockTimer: number | undefined;

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
  calendarShowSeconds: true,
  quotaSummaryEnabled: true,
  quotaSummaryFields: ['schoolType', 'totalCount', 'usedCount', 'quotaLimit', 'remainingCount'],
  quotaSummaryTemplate: '',
  quotaSummaryFontSize: 13,
  quotaSummaryTextColor: '#214567',
  quotaSummaryNumberColor: '#1d4ed8',
  quotaSummaryBackground: '#f4f9ff',
  quotaSummaryBorderColor: '#bfdbfe',
  quotaSummaryRadius: 8,
  quotaSummaryGap: 8,
  quotaSummaryCss: '',
  quotaRatioEnabled: false,
  quotaRatioMaxItems: 4,
  quotaRatioFontSize: 12,
  quotaRatioTextColor: '#40566f',
  quotaRatioNumberColor: '#2563eb',
  quotaRatioBackground: '#ffffff',
  quotaRatioBorderColor: '#d8e6f5',
  quotaRatioRadius: 8,
  quotaRatioCss: '',
  cardRatioEnabled: false,
  schoolTypeLabelReplacements: [{ source: '高职', target: '高职高专' }]
};

const defaultStageNoticeConfig: StageNoticeConfig = {
  ...defaultNoticeLayoutConfig,
  categoryColumns: 5,
  categoryGap: 12,
  categoryRadius: 8,
  hoverEffectMode: 'textPattern',
  hoverScaleEnabled: true,
  hoverScale: 1.04,
  categoryAreaBackground: '#f8fbff',
  componentCss: '',
  dateCss: '',
  categories: [
    {
      key: 'performance',
      label: '艺术表演类',
      aliases: ['performance', 'performance_', '声乐', '器乐', '舞蹈', '戏剧', '朗诵'],
      visible: true,
      order: 1,
      fontSize: 15,
      titleColor: '#082f49',
      numberColor: '#2563eb',
      backgroundColor: '#f8fbff',
      borderColor: '#bfd7ff',
      accentColor: '#2563eb',
      customCss: '',
      showQuota: true,
      showSubmitted: true,
      linkEnabled: true
    },
    {
      key: 'artwork',
      label: '艺术作品类',
      aliases: ['artwork', 'artwork_', '美术', '设计', '影视'],
      visible: true,
      order: 2,
      fontSize: 15,
      titleColor: '#083344',
      numberColor: '#0891b2',
      backgroundColor: '#f7fcff',
      borderColor: '#c7e6f4',
      accentColor: '#0284c7',
      customCss: '',
      showQuota: true,
      showSubmitted: true,
      linkEnabled: true
    },
    {
      key: 'workshop',
      label: '艺术实践工作坊',
      aliases: ['workshop', '工作坊'],
      visible: true,
      order: 3,
      fontSize: 15,
      titleColor: '#064e3b',
      numberColor: '#059669',
      backgroundColor: '#f6fdfa',
      borderColor: '#cdeee1',
      accentColor: '#0d9488',
      customCss: '',
      showQuota: true,
      showSubmitted: true,
      linkEnabled: true
    },
    {
      key: 'achievement',
      label: '高校美育改革创新优秀成果',
      aliases: ['achievement', 'achievement_', '论文', '案例', '成果', '美育'],
      visible: true,
      order: 4,
      fontSize: 15,
      titleColor: '#422006',
      numberColor: '#d97706',
      backgroundColor: '#fffaf0',
      borderColor: '#f4d99d',
      accentColor: '#d97706',
      customCss: '',
      showQuota: true,
      showSubmitted: true,
      linkEnabled: true
    },
    {
      key: 'principal',
      label: '高校校长书画作品',
      aliases: ['principal', 'artwork_principal', '校长书画'],
      visible: true,
      order: 5,
      fontSize: 15,
      titleColor: '#312e81',
      numberColor: '#4f46e5',
      backgroundColor: '#f5f7ff',
      borderColor: '#cbd5ff',
      accentColor: '#4f46e5',
      customCss: '',
      showQuota: true,
      showSubmitted: true,
      linkEnabled: true
    }
  ]
};

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
const defaultQuotaFields = ['quotaLimit', 'usedCount', 'remainingCount'];
const normalizeQuotaFields = (value?: string[]) => {
  const allowed = new Set(defaultQuotaFields);
  const fields = (Array.isArray(value) ? value : defaultQuotaFields).map((item) => String(item || '')).filter((item) => allowed.has(item));
  return fields.length ? fields : [...defaultQuotaFields];
};
const normalizeQuotaDisplayMode = (value?: string, showQuota?: boolean): 'none' | 'always' | 'hover' => {
  if (value === 'none' || value === 'always' || value === 'hover') return value;
  return showQuota === false ? 'none' : 'hover';
};
const normalizeQuotaPosition = (value?: string): 'topRight' | 'belowStats' | 'bottom' =>
  value === 'belowStats' || value === 'bottom' || value === 'topRight' ? value : 'topRight';

const resolvedConfig = computed<StageNoticeConfig>(() => parseStageNoticeConfig(props.configJson));

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

const primaryNotice = computed(() => notices.value[0] || {});
const noticeTitle = computed(() => primaryNotice.value.noticeTitle || '报送公告');
const noticeContentHtml = computed(() =>
  escapeHtml(primaryNotice.value.noticeContent || '请按活动要求完成项目填报，并在截止时间前统一提交。').replace(/\n/g, '<br/>')
);
const noticeAttachments = computed<NoticeAttachment[]>(() => normalizeNoticeAttachments(primaryNotice.value));

const hoverEffectMode = computed(() => normalizeHoverEffectMode(resolvedConfig.value.hoverEffectMode, resolvedConfig.value.hoverScaleEnabled));
const isHoverTextEnabled = computed(() => hoverTextEnabled(hoverEffectMode.value));
const isHoverPatternEnabled = computed(() => hoverPatternEnabled(hoverEffectMode.value));
const normalizedHoverScale = computed(() => normalizeHoverScale(resolvedConfig.value.hoverScale));
const visibleCategoryConfigs = computed(() =>
  resolvedConfig.value.categories
    .filter(
      (item) =>
        item.visible !== false &&
        (resolvedConfig.value.categoryConfigMode !== 'activity' || String(item.activityId ?? '') === String(activeActivityId.value ?? ''))
    )
    .sort((a, b) => Number(a.order || 0) - Number(b.order || 0))
);
const formatSchoolTypeLabel = (value?: unknown) => {
  const text = String(value || '').trim();
  if (!text) return '';
  const matched = resolvedConfig.value.schoolTypeLabelReplacements.find((item) => String(item.source || '').trim() === text);
  return matched?.target || text;
};
const quotaSchoolTypeText = computed(() => formatSchoolTypeLabel(quotaOverview.value.schoolType) || quotaOverview.value.schoolName || '当前学校');
const quotaSummaryFieldOptions = [
  { key: 'schoolType', label: '院校类型' },
  { key: 'schoolName', label: '学校名称' },
  { key: 'totalCount', label: '总上报' },
  { key: 'usedCount', label: '已用名额' },
  { key: 'quotaLimit', label: '总限额' },
  { key: 'remainingCount', label: '剩余' },
  { key: 'draftCount', label: '草稿' },
  { key: 'submittedCount', label: '已提交' },
  { key: 'completedCount', label: '已通过' }
];
const quotaVariableAliases: Record<string, string> = {
  schoolType: 'schoolType',
  schoolName: 'schoolName',
  totalCount: 'totalCount',
  usedCount: 'usedCount',
  quotaLimit: 'quotaLimit',
  remainingCount: 'remainingCount',
  draftCount: 'draftCount',
  submittedCount: 'submittedCount',
  completedCount: 'completedCount',
  学校类别变量: 'schoolType',
  院校类型变量: 'schoolType',
  学校类别: 'schoolType',
  院校类型: 'schoolType',
  学校名称变量: 'schoolName',
  学校名称: 'schoolName',
  总上报变量: 'totalCount',
  中上报变量: 'totalCount',
  总上报: 'totalCount',
  已用名额变量: 'usedCount',
  已用名额: 'usedCount',
  总限额变量: 'quotaLimit',
  总限额: 'quotaLimit',
  剩余变量: 'remainingCount',
  剩余名额变量: 'remainingCount',
  剩余: 'remainingCount',
  草稿变量: 'draftCount',
  草稿: 'draftCount',
  已提交变量: 'submittedCount',
  提交变量: 'submittedCount',
  已提交: 'submittedCount',
  通过变量: 'completedCount',
  已通过变量: 'completedCount',
  已通过: 'completedCount'
};

const quotaValueText = (key: string) => {
  const overview = quotaOverview.value;
  const values: Record<string, unknown> = {
    schoolType: quotaSchoolTypeText.value,
    schoolName: overview.schoolName || '',
    totalCount: overview.totalCount,
    usedCount: overview.usedCount,
    quotaLimit: hasNumber(overview.quotaLimit) ? overview.quotaLimit : '-',
    remainingCount: hasNumber(overview.remainingCount) ? overview.remainingCount : '-',
    draftCount: overview.draftCount,
    submittedCount: overview.submittedCount,
    completedCount: overview.completedCount
  };
  const value = values[key];
  if (value === undefined || value === null || value === '') return '-';
  return String(value);
};

const quotaSummaryItems = computed(() => {
  const fields = resolvedConfig.value.quotaSummaryFields?.length
    ? resolvedConfig.value.quotaSummaryFields
    : defaultNoticeLayoutConfig.quotaSummaryFields;
  return quotaSummaryFieldOptions
    .filter((item) => fields.includes(item.key))
    .map((item) => ({
      ...item,
      value: quotaValueText(item.key)
    }));
});

const quotaSummaryTokens = computed<QuotaSummaryToken[]>(() => {
  const template = String(resolvedConfig.value.quotaSummaryTemplate || '').trim();
  if (!template) return [];
  const tokens: QuotaSummaryToken[] = [];
  const pattern = /\{([^{}]+)\}/g;
  let cursor = 0;
  let index = 0;
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(template))) {
    if (match.index > cursor) {
      tokens.push({ key: `text-${index++}`, text: template.slice(cursor, match.index) });
    }
    const rawKey = String(match[1] || '').trim();
    const normalizedKey = quotaVariableAliases[rawKey] || rawKey;
    const isVariable = Boolean(quotaVariableAliases[rawKey] || quotaSummaryFieldOptions.some((item) => item.key === normalizedKey));
    tokens.push({
      key: `var-${index++}-${normalizedKey}`,
      value: isVariable ? quotaValueText(normalizedKey) : `{${rawKey}}`,
      variable: isVariable
    });
    cursor = match.index + match[0].length;
  }
  if (cursor < template.length) {
    tokens.push({ key: `text-${index++}`, text: template.slice(cursor) });
  }
  return tokens;
});

const noticeRatioItems = computed(() => {
  const maxItems = Math.max(1, Math.min(12, Number(resolvedConfig.value.quotaRatioMaxItems || 4)));
  return groupSummaries.value
    .flatMap((group) =>
      group.ratioItems.map((ratio, index) => ({
        key: `${group.key}-${ratio.key || ratio.groupCode || ratio.groupName || index}`,
        label: ratio.message || `${group.name} ${ratio.groupName || ratio.groupCode || '比例'}`,
        count: numberText(ratio.count),
        percent: ratio.percent
      }))
    )
    .slice(0, maxItems);
});

const showNoticeQuotaSummary = computed(
  () =>
    resolvedConfig.value.quotaSummaryEnabled !== false &&
    !!quotaOverview.value.activityId &&
    (quotaSummaryTokens.value.length > 0 || quotaSummaryItems.value.length > 0)
);
const showNoticeRatioSummary = computed(() => resolvedConfig.value.quotaRatioEnabled === true && noticeRatioItems.value.length > 0);

const availableRootNodes = computed<ActivityCategoryTreeNode[]>(() =>
  buildActivityCategoryTree(categories.value, true)
    .map((node) => {
      if (!isCategoryGroup(node)) {
        return isCategoryMenuAvailable(node, categories.value) ? node : undefined;
      }
      if (node.enabled === false || !isCategoryMenuVisible(node)) return undefined;
      const children = sortCategoryNodes((node.children || []).filter((item) => isCategoryMenuAvailable(item, categories.value)));
      return children.length ? { ...node, children } : undefined;
    })
    .filter((item): item is ActivityCategoryTreeNode => Boolean(item))
);
const categoryOverviewMap = computed(
  () =>
    new Map(
      (quotaOverview.value.categoryItems || [])
        .filter((item) => item.categoryId !== undefined && item.categoryId !== null)
        .map((item) => [String(item.categoryId), item])
    )
);
const resolveConfiguredSourceNode = (config: StageCategoryConfig) =>
  availableRootNodes.value.find((node) => {
    const sourceType = isCategoryGroup(node) ? 'group' : 'category';
    if (config.sourceType && config.sourceType !== sourceType) return false;
    if (config.sourceId !== undefined && config.sourceId !== null && String(node.id) === String(config.sourceId)) return true;
    if (config.sourceCode && String(node.categoryCode || '') === String(config.sourceCode)) return true;
    return !!config.sourceName && String(node.categoryName || '') === String(config.sourceName);
  });
const optionalNumberSum = (items: ProjectQuotaOverviewItemVO[], key: 'quotaLimit' | 'remainingCount') => {
  const available = items.filter((item) => hasNumber(item[key]));
  return available.length ? available.reduce((sum, item) => sum + Number(item[key] || 0), 0) : undefined;
};
const overviewItemValues = (item?: ProjectQuotaOverviewItemVO) => ({
  quota: hasNumber(item?.quotaLimit) ? Number(item?.quotaLimit) : undefined,
  remaining: hasNumber(item?.remainingCount) ? Number(item?.remainingCount) : undefined,
  used: Number(item?.usedCount || 0),
  total: Number(item?.totalCount || 0),
  draft: Number(item?.draftCount || 0),
  submitted: Number(item?.submittedCount || 0),
  completed: Number(item?.completedCount || 0),
  ratioItems: item?.ratioItems || []
});
const sourceOverviewValues = (node: ActivityCategoryTreeNode) => {
  if (isCategoryGroup(node)) {
    const groupName = String(node.categoryName || '').trim();
    const groupCode = String(node.categoryCode || '').trim();
    const groupItem = (quotaOverview.value.groupItems || []).find(
      (item) =>
        (!!groupName && String(item.groupName || '').trim() === groupName) || (!!groupCode && String(item.groupKey || '').trim() === groupCode)
    );
    if (groupItem) {
      return overviewItemValues(groupItem);
    }
  }
  const leafNodes = isCategoryGroup(node) ? node.children || [] : [node];
  const items = leafNodes
    .map((leaf) => categoryOverviewMap.value.get(String(leaf.id)))
    .filter((item): item is ProjectQuotaOverviewItemVO => Boolean(item));
  return {
    quota: optionalNumberSum(items, 'quotaLimit'),
    remaining: optionalNumberSum(items, 'remainingCount'),
    used: items.reduce((sum, item) => sum + Number(item.usedCount || 0), 0),
    total: items.reduce((sum, item) => sum + Number(item.totalCount || 0), 0),
    draft: items.reduce((sum, item) => sum + Number(item.draftCount || 0), 0),
    submitted: items.reduce((sum, item) => sum + Number(item.submittedCount || 0), 0),
    completed: items.reduce((sum, item) => sum + Number(item.completedCount || 0), 0),
    ratioItems: items.flatMap((item) => item.ratioItems || [])
  };
};
const toConfiguredSourceSummary = (config: StageCategoryConfig): GroupSummary | undefined => {
  const node = resolveConfiguredSourceNode(config);
  if (!node) return undefined;
  const sourceType = isCategoryGroup(node) ? 'group' : 'category';
  const values = sourceOverviewValues(node);
  return baseGroupSummary(config, {
    name: config.label || String(node.categoryName || '报送类别'),
    activityId: activeActivityId.value,
    sourceType,
    sourceId: node.id,
    categoryId: sourceType === 'category' ? node.id : undefined,
    categoryCode: sourceType === 'category' ? node.categoryCode : undefined,
    categoryName: sourceType === 'category' ? node.categoryName : undefined,
    quota: values.quota,
    used: values.used,
    remaining: values.remaining,
    total: values.total,
    draft: values.draft,
    submitted: values.submitted,
    completed: values.completed,
    ratioItems: values.ratioItems,
    routeGroupName: sourceType === 'group' ? String(node.categoryName || config.label) : categoryGroupName(node, config.label)
  });
};

const groupSummaries = computed<GroupSummary[]>(() => {
  if (resolvedConfig.value.categoryConfigMode === 'activity') {
    return visibleCategoryConfigs.value.map(toConfiguredSourceSummary).filter((item): item is GroupSummary => Boolean(item));
  }
  const overviewItems = quotaOverview.value.groupItems || [];
  if (overviewItems.length) {
    return visibleCategoryConfigs.value
      .map((group) => {
        const overviewItem = overviewItems.find((item) => item.groupKey === group.key || item.id === group.key || item.groupName === group.label);
        return overviewItem ? toGroupSummary(group, overviewItem) : undefined;
      })
      .filter(Boolean) as GroupSummary[];
  }
  return visibleCategoryConfigs.value
    .map((group) => {
      const groupCategories = categories.value.filter((category) => categoryGroupKey(category) === group.key);
      if (!groupCategories.length) {
        return undefined;
      }
      const routeGroupName = categoryGroupName(groupCategories[0], group.label);
      const quota = groupCategories.reduce((sum, category) => sum + Number(category.quotaLimit || 0), 0);
      return baseGroupSummary(group, {
        name: group.label,
        quota,
        used: 0,
        total: 0,
        draft: 0,
        submitted: 0,
        completed: 0,
        ratioItems: [],
        routeGroupName
      });
    })
    .filter(Boolean) as GroupSummary[];
});

const isDefaultStatsAreaBackground = (value?: string) => {
  const background = String(value || '')
    .replace(/\s/g, '')
    .toLowerCase();
  return !background || background === '#f8fbff' || background === 'rgb(248,251,255)';
};

const statsGridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${Math.max(1, Number(resolvedConfig.value.categoryColumns || 5))}, minmax(0, 1fr))`,
  gap: `${Math.max(0, Number(resolvedConfig.value.categoryGap || 0))}px`,
  background: isDefaultStatsAreaBackground(resolvedConfig.value.categoryAreaBackground) ? 'transparent' : resolvedConfig.value.categoryAreaBackground,
  '--stage-stat-hover-scale': String(normalizedHoverScale.value)
}));

const toGroupSummary = (group: StageCategoryConfig, item: ProjectQuotaOverviewItemVO) =>
  baseGroupSummary(group, {
    name: group.label || item.groupName || item.categoryName || '报送类别',
    quota: hasNumber(item.quotaLimit) ? Number(item.quotaLimit) : undefined,
    used: Number(item.usedCount || 0),
    remaining: item.remainingCount,
    total: Number(item.totalCount || 0),
    draft: Number(item.draftCount || 0),
    submitted: Number(item.submittedCount || 0),
    completed: Number(item.completedCount || 0),
    ratioItems: (item.ratioItems || []).filter((ratio) => Number(ratio.count || 0) > 0 || hasNumber(ratio.limitCount) || hasNumber(ratio.ratioValue)),
    routeGroupName: item.groupName || group.label
  });

const baseGroupSummary = (
  group: StageCategoryConfig,
  values: Pick<
    GroupSummary,
    | 'name'
    | 'activityId'
    | 'sourceType'
    | 'sourceId'
    | 'categoryId'
    | 'categoryCode'
    | 'categoryName'
    | 'quota'
    | 'used'
    | 'total'
    | 'draft'
    | 'submitted'
    | 'completed'
    | 'ratioItems'
    | 'routeGroupName'
  > & {
    remaining?: number;
  }
): GroupSummary => {
  const accentColor = group.accentColor || '#2563eb';
  const visual = defaultVisualConfig(group.key, group.label || values.name, accentColor, group.backgroundColor);
  const visualSource = isLegacyDefaultVisualConfig(group) ? { ...group, ...visual } : group;
  const patternMode = normalizePatternMode(visualSource.patternMode || (visualSource.patternImageKey ? 'image' : visual.patternMode));
  const patternColor = String(visualSource.patternColor || '').trim();
  return {
    key: group.key,
    ...values,
    fontSize: Number(group.fontSize || 15),
    titleColor: group.titleColor || '#102a43',
    numberColor: group.numberColor || '#2563eb',
    backgroundColor: group.backgroundColor || '#f8fbff',
    borderColor: group.borderColor || '#dbeafe',
    accentColor,
    customCss: group.customCss || '',
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
    highlightOpacity: Number(visualSource.highlightOpacity ?? visual.highlightOpacity),
    showQuota: group.showQuota !== false,
    showSubmitted: group.showSubmitted !== false,
    linkEnabled: group.linkEnabled !== false,
    quotaDisplayMode: normalizeQuotaDisplayMode(group.quotaDisplayMode, group.showQuota),
    quotaFields: normalizeQuotaFields(group.quotaFields),
    quotaPosition: normalizeQuotaPosition(group.quotaPosition)
  };
};

const numberText = (value: unknown) => {
  const numeric = Number(value || 0);
  return Number.isFinite(numeric) ? numeric : 0;
};

const hasNumber = (value: unknown) => value !== undefined && value !== null;
const quotaItems = (item: GroupSummary) => {
  if (!hasNumber(item.quota)) return [];
  const remaining = hasNumber(item.remaining) ? Number(item.remaining) : Math.max(0, Number(item.quota || 0) - Number(item.used || 0));
  const values: Record<string, { label: string; value: number }> = {
    quotaLimit: { label: '总限额', value: Number(item.quota || 0) },
    usedCount: { label: '已用', value: Number(item.used || 0) },
    remainingCount: { label: '剩余', value: remaining }
  };
  return normalizeQuotaFields(item.quotaFields)
    .map((key) => (values[key] ? { key, ...values[key] } : undefined))
    .filter((quotaItem): quotaItem is { key: string; label: string; value: number } => Boolean(quotaItem));
};
const toggleQuotaCard = (key: string) => {
  openQuotaCardKey.value = openQuotaCardKey.value === key ? '' : key;
};

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

const quotaSummaryStyle = computed(
  () =>
    ({
      fontSize: `${clampNumber(resolvedConfig.value.quotaSummaryFontSize, 13, 11, 24)}px`,
      color: resolvedConfig.value.quotaSummaryTextColor || '#214567',
      background: resolvedConfig.value.quotaSummaryBackground || '#f4f9ff',
      borderColor: resolvedConfig.value.quotaSummaryBorderColor || '#bfdbfe',
      borderRadius: `${clampNumber(resolvedConfig.value.quotaSummaryRadius, 8, 0, 24)}px`,
      gap: `${clampNumber(resolvedConfig.value.quotaSummaryGap, 8, 0, 24)}px`,
      '--stage-notice-quota-number-color': resolvedConfig.value.quotaSummaryNumberColor || '#1d4ed8'
    }) as Record<string, string>
);

const quotaRatioStyle = computed(
  () =>
    ({
      fontSize: `${clampNumber(resolvedConfig.value.quotaRatioFontSize, 12, 10, 20)}px`,
      color: resolvedConfig.value.quotaRatioTextColor || '#40566f',
      background: resolvedConfig.value.quotaRatioBackground || '#ffffff',
      borderColor: resolvedConfig.value.quotaRatioBorderColor || '#d8e6f5',
      borderRadius: `${clampNumber(resolvedConfig.value.quotaRatioRadius, 8, 0, 24)}px`,
      '--stage-notice-ratio-number-color': resolvedConfig.value.quotaRatioNumberColor || '#2563eb'
    }) as Record<string, string>
);

const categoryStyle = (item: GroupSummary) => ({
  borderColor: item.borderColor,
  background: item.backgroundColor,
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
    StageCategoryConfig,
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
    StageCategoryConfig,
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
    StageCategoryConfig,
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
const patternStyle = (item: GroupSummary) => {
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
    color: item.patternColor || item.accentColor,
    opacity,
    '--stage-stat-pattern-scale': String(patternScale),
    '--stage-stat-pattern-hover-scale': String(patternHoverScale)
  };
};

const glassStyle = (item: GroupSummary) => ({
  '--stage-stat-glass-color': item.glassColor || 'rgba(255,255,255,1)',
  '--stage-stat-glass-opacity': String(clampNumber(item.glassOpacity, 0.9, 0, 1)),
  '--stage-stat-glass-blur': `${clampNumber(item.glassBlur, 28, 0, 28)}px`,
  '--stage-stat-texture-opacity': normalizePatternMode(item.patternMode) === 'none' ? '0' : String(clampNumber(item.textureOpacity, 0.21, 0, 0.65)),
  '--stage-stat-highlight-opacity': String(clampNumber(item.highlightOpacity, 0.5, 0, 1))
});

const runtimeStyleHtml = computed(() => {
  const styles = [
    resolvedConfig.value.componentCss,
    wrapCss('.school-stage-notice .stage-notice-quota', resolvedConfig.value.quotaSummaryCss),
    wrapCss('.school-stage-notice .stage-notice-ratio-summary', resolvedConfig.value.quotaRatioCss),
    wrapCss('.school-stage-notice .stage-notice-calendar', resolvedConfig.value.dateCss),
    `.school-stage-notice .stage-notice-top > .stage-notice-copy,
.school-stage-notice .stage-notice-top > .stage-notice-activity,
.school-stage-notice .stage-notice-top > .stage-notice-calendar {
  height: var(--stage-top-height, 220px) !important;
  min-height: var(--stage-top-height, 220px) !important;
  max-height: var(--stage-top-height, 220px) !important;
}`,
    ...visibleCategoryConfigs.value.map((item) => wrapCss(`.school-stage-notice .stage-notice-stat--${cssSafeKey(item.key)}`, item.customCss))
  ]
    .map(sanitizeCss)
    .filter(Boolean)
    .join('\n');
  return styles ? `<style>${styles}</style>` : '';
});

const escapeHtml = (value: string) => {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
};

const normalizeSchoolTypeLabelReplacements = (value: unknown, fallback: SchoolTypeLabelReplacement[]) => {
  const source = Array.isArray(value) ? value : fallback;
  return source
    .map((item) => ({
      source: String(item?.source || '').trim(),
      target: String(item?.target || '').trim()
    }))
    .filter((item) => item.source && item.target);
};

const parseStageNoticeConfig = (value?: string): StageNoticeConfig => {
  let custom: Partial<StageNoticeConfig> = {};
  if (value) {
    try {
      custom = JSON.parse(value);
    } catch {
      custom = {};
    }
  }
  const hoverEffectMode = normalizeHoverEffectMode(custom.hoverEffectMode, custom.hoverScaleEnabled ?? defaultStageNoticeConfig.hoverScaleEnabled);
  const activityCategoryMode = custom.categoryConfigMode === 'activity';
  const categorySource = activityCategoryMode ? custom.categories || [] : mergeCategories(defaultStageNoticeConfig.categories, custom.categories);
  return {
    ...defaultStageNoticeConfig,
    ...custom,
    categoryConfigMode: activityCategoryMode ? 'activity' : 'legacy',
    hoverEffectMode,
    hoverScaleEnabled: hoverEffectMode !== 'none',
    hoverScale: normalizeHoverScale(custom.hoverScale ?? defaultStageNoticeConfig.hoverScale),
    schoolTypeLabelReplacements: normalizeSchoolTypeLabelReplacements(
      custom.schoolTypeLabelReplacements,
      defaultStageNoticeConfig.schoolTypeLabelReplacements
    ),
    categories: categorySource.map((item) => ({
      ...item,
      quotaDisplayMode: normalizeQuotaDisplayMode(item.quotaDisplayMode, item.showQuota),
      quotaFields: normalizeQuotaFields(item.quotaFields),
      quotaPosition: normalizeQuotaPosition(item.quotaPosition)
    }))
  };
};

const mergeCategories = (base: StageCategoryConfig[], custom?: StageCategoryConfig[]) => {
  const customMap = new Map((custom || []).map((item) => [item.key, item]));
  const merged = base.map((item) => ({ ...item, ...(customMap.get(item.key) || {}) }));
  (custom || []).forEach((item) => {
    if (!merged.some((baseItem) => baseItem.key === item.key)) {
      merged.push(item);
    }
  });
  return merged;
};

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

const normalizeNoticeAttachments = (notice: any): NoticeAttachment[] => {
  const attachments = Array.isArray(notice?.attachments) ? notice.attachments : [];
  if (attachments.length) {
    return attachments.filter((item: NoticeAttachment) => item?.ossId);
  }
  return String(notice?.attachmentOssIds || '')
    .split(',')
    .map((id) => id.trim())
    .filter(Boolean)
    .map((id, index) => ({ ossId: id, originalName: `公告附件${index + 1}` }));
};

const attachmentName = (attachment: NoticeAttachment, index: number) => attachment.originalName || attachment.fileName || `公告附件${index + 1}`;

const downloadNoticeAttachment = (attachment: NoticeAttachment) => {
  if (!attachment.ossId) {
    return;
  }
  proxy?.$download.noticeAttachment(attachment.ossId);
};

const categoryGroupName = (category: ActivityCategoryVO, fallback?: string) =>
  String(category.categoryGroup || inferCategoryGroup(category.categoryName) || fallback || '').trim();

const categoryGroupKey = (category: ActivityCategoryVO) => {
  const group = String(category.categoryGroup || '').toLowerCase();
  const code = String(category.categoryCode || '').toLowerCase();
  const name = String(category.categoryName || '');
  if (group === 'principal' || group.includes('校长书画') || code.includes('principal') || name.includes('校长书画')) return 'principal';
  const matchedConfig = visibleCategoryConfigs.value.find((item) => {
    const aliases = item.aliases || [];
    return aliases.some((alias) => {
      const keyword = String(alias || '').trim();
      const lowerKeyword = keyword.toLowerCase();
      return keyword && (group === lowerKeyword || code.includes(lowerKeyword) || name.includes(keyword));
    });
  });
  if (matchedConfig) return matchedConfig.key;
  if (group === 'performance' || code.startsWith('performance_') || ['声乐', '器乐', '舞蹈', '戏剧', '朗诵'].some((item) => name.includes(item)))
    return 'performance';
  if (group === 'artwork' || code.startsWith('artwork_') || ['美术', '设计', '影视'].some((item) => name.includes(item))) return 'artwork';
  if (group === 'workshop' || code.includes('workshop') || name.includes('工作坊')) return 'workshop';
  if (group === 'achievement' || code.startsWith('achievement_') || ['论文', '案例', '成果', '美育'].some((item) => name.includes(item)))
    return 'achievement';
  return 'achievement';
};

const inferCategoryGroup = (name?: string) => {
  const text = name || '';
  if (text.includes('校长书画')) return '高校校长书画作品';
  if (['声乐', '器乐', '舞蹈', '戏剧', '朗诵', '个人'].some((item) => text.includes(item))) return '艺术表演类';
  if (['美术', '设计', '影视', '校长'].some((item) => text.includes(item))) return '艺术作品类';
  if (text.includes('工作坊')) return '艺术实践工作坊';
  if (['论文', '案例', '成果', '美育'].some((item) => text.includes(item))) return '高校美育改革创新优秀成果';
  return '';
};

const openGroupSummary = (item: GroupSummary) => {
  if (item.linkEnabled === false) {
    return;
  }
  openQuotaCardKey.value = '';
  appStore.requestSidebarReportGroupOpen({
    targetType: item.sourceType === 'category' ? 'category' : 'group',
    activityId: item.activityId || activeActivityId.value,
    categoryNodeId: item.sourceId,
    categoryId: item.categoryId,
    categoryCode: item.categoryCode,
    categoryName: item.categoryName,
    groupKey: item.key,
    groupName: item.routeGroupName || item.name
  });
};

const loadData = async () => {
  loading.value = true;
  try {
    const [noticeRes, activityRes] = await Promise.all([
      getRegisterNotice({ userType: 'school', schoolId: userStore.schoolId || undefined }),
      listAvailableActivity()
    ]);
    notices.value = noticeRes.data || [];
    const activities = activityRes.data || [];
    const activity = activities[0];
    if (activity) {
      activeActivityId.value = activity.id;
      activityName.value = activity.activityName || activityName.value;
    }
    if (!activeActivityId.value) {
      categories.value = [];
      quotaOverview.value = {};
      return;
    }
    const categoryRes = await listSchoolCategoryCatalog(activeActivityId.value);
    categories.value = categoryRes.data || [];
    const categoryIds = categories.value
      .filter((item) => isCategoryMenuAvailable(item, categories.value))
      .map((item) => item.id)
      .filter((id) => id !== undefined && id !== null);
    if (!categoryIds.length) {
      quotaOverview.value = {};
      return;
    }
    const overviewRes = await getMyProjectQuotaOverview({
      activityId: activeActivityId.value,
      categoryIds: categoryIds.join(',')
    });
    quotaOverview.value = overviewRes.data || {};
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
.school-stage-notice {
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

.stage-notice-quota {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 14px;
  padding: 9px 10px;
  border: 1px solid;
  line-height: 1.45;
}

.stage-notice-quota__item {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
  min-width: 0;
  white-space: nowrap;
}

.stage-notice-quota__item em {
  color: currentColor;
  font-style: normal;
  opacity: 0.78;
}

.stage-notice-quota__item strong,
.stage-notice-quota__template strong {
  color: var(--stage-notice-quota-number-color, #1d4ed8);
  font-weight: 900;
}

.stage-notice-quota__template {
  min-width: 0;
  white-space: normal;
  word-break: break-word;
}

.stage-notice-ratio-summary {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
  padding: 8px 9px;
  border: 1px solid;
  line-height: 1.45;
}

.stage-notice-ratio-summary span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  max-width: 100%;
  padding: 3px 8px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
}

.stage-notice-ratio-summary em {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-style: normal;
}

.stage-notice-ratio-summary strong {
  color: var(--stage-notice-ratio-number-color, #2563eb);
  font-weight: 900;
}

.stage-notice-ratio-summary small {
  color: #64748b;
  font-size: 1em;
}

.stage-notice-downloads {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.stage-notice-downloads :deep(.el-button) {
  max-width: 100%;
  margin-left: 0;
}

.stage-notice-downloads :deep(.el-button span) {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  min-height: 118px;
  padding: 16px 14px 14px 18px;
  display: grid;
  grid-template-rows: auto auto auto;
  align-content: center;
  gap: 8px;
  overflow: hidden;
  border: 1px solid #d8e6f5;
  box-shadow: none;
  text-align: left;
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
  transform: scale(var(--stage-stat-pattern-scale, 1.75));
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
  background:
    linear-gradient(72deg, transparent 0 45%, currentColor 46% 49%, transparent 50% 100%),
    linear-gradient(108deg, transparent 0 45%, currentColor 46% 49%, transparent 50% 100%);
  top: 43%;
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

.stage-notice-stat.is-clickable {
  cursor: pointer;
}

.stage-notice-stat.is-clickable:focus-visible {
  outline: 2px solid var(--el-color-primary-light-5);
  outline-offset: 2px;
}

.stage-notice-stat strong {
  position: relative;
  z-index: 2;
  color: #102a43;
  font-size: 15px;
  line-height: 1.35;
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
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  color: #40566f;
  font-size: 14px;
  line-height: 1.5;
  white-space: nowrap;
  transform-origin: left center;
  transition: transform 0.18s ease;
}

.stage-notice-stat__numbers b {
  font-size: 17px;
  font-weight: 950;
}

.stage-notice-stat__numbers em {
  color: currentColor;
  font-style: normal;
  font-weight: 800;
  opacity: 0.72;
}

.stage-notice-stat__legend {
  position: relative;
  z-index: 2;
  display: grid;
  gap: 3px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.35;
}

.stage-notice-stat__legend span {
  color: #53677d;
  white-space: normal;
}

.stage-notice-stat__quota-trigger {
  position: absolute;
  z-index: 12;
  top: 8px;
  right: 8px;
  padding: 3px 8px;
  border: 1px solid rgba(148, 163, 184, 0.45);
  border-radius: 999px;
  color: #475569;
  background: rgba(255, 255, 255, 0.82);
  font-size: 11px;
  line-height: 1.3;
  cursor: pointer;
  backdrop-filter: blur(8px);
}

.stage-notice-stat__quota-inline {
  position: relative;
  z-index: 4;
  display: flex;
  flex-wrap: wrap;
  gap: 4px 8px;
  color: #52657a;
  font-size: 11px;
  line-height: 1.35;
}

.stage-notice-stat__quota-inline span {
  white-space: nowrap;
}

.stage-notice-stat__quota-inline--top-right {
  position: absolute;
  top: 8px;
  right: 8px;
  max-width: calc(100% - 28px);
  justify-content: flex-end;
  padding: 3px 7px;
  border: 1px solid rgba(148, 163, 184, 0.34);
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(8px);
}

.stage-notice-stat.has-quota-top-right > strong {
  padding-right: 76px;
}

.stage-notice-stat__quota-inline--bottom {
  padding-top: 5px;
  border-top: 1px dashed rgba(148, 163, 184, 0.42);
}

.stage-notice-stat__quota-popover {
  position: absolute;
  z-index: 10;
  left: 12px;
  right: 12px;
  top: 50%;
  display: flex;
  justify-content: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid rgba(148, 163, 184, 0.42);
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.14);
  opacity: 0;
  pointer-events: none;
  transform: translateY(-42%) scale(0.98);
  transition:
    opacity 0.16s ease,
    transform 0.16s ease;
  backdrop-filter: blur(12px);
}

.stage-notice-stat__quota-popover span {
  display: grid;
  min-width: 48px;
  gap: 2px;
  text-align: center;
}

.stage-notice-stat__quota-popover em {
  color: #64748b;
  font-size: 11px;
  font-style: normal;
}

.stage-notice-stat__quota-popover strong {
  color: #0f3c75;
  font-size: 16px;
  line-height: 1.2;
}

.stage-notice-stat.has-quota-hover:hover .stage-notice-stat__quota-popover,
.stage-notice-stat.has-quota-hover:focus-visible .stage-notice-stat__quota-popover,
.stage-notice-stat.has-quota-hover.is-quota-open .stage-notice-stat__quota-popover {
  opacity: 1;
  transform: translateY(-50%) scale(1);
}

.stage-notice-stat.is-hover-text-enabled:hover .stage-notice-stat__numbers,
.stage-notice-stat.is-hover-text-enabled:focus-visible .stage-notice-stat__numbers {
  transform: scale(var(--stage-stat-hover-scale, 1.08));
}

.stage-notice-stat.is-hover-pattern-enabled:hover .stage-notice-stat__pattern,
.stage-notice-stat.is-hover-pattern-enabled:focus-visible .stage-notice-stat__pattern {
  transform: scale(var(--stage-stat-pattern-hover-scale, var(--stage-stat-pattern-scale, 1.75)));
}

@media (max-width: 1200px) {
  .stage-notice-top,
  .stage-notice-stats {
    grid-template-columns: 1fr;
  }
}
</style>
