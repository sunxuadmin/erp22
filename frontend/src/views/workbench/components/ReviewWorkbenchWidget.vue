<template>
  <section class="workbench-card review-workbench" :style="rootStyle">
    <div class="review-workbench__head">
      <div>
        <span v-if="resolvedConfig.kickerVisible !== false" :style="{ fontSize: `${resolvedConfig.kickerFontSize}px` }">
          {{ resolvedConfig.kickerText }}
        </span>
        <h2 v-if="resolvedConfig.titleVisible !== false" :style="{ fontSize: `${resolvedConfig.titleFontSize}px` }">{{ displayTitle }}</h2>
      </div>
      <div class="review-workbench__actions">
        <el-button
          v-if="resolvedConfig.listButtonVisible !== false"
          class="review-workbench__list-button"
          type="primary"
          icon="EditPen"
          @click="goReviewList"
        >
          {{ resolvedConfig.listButtonText }}
        </el-button>
        <el-button v-if="resolvedConfig.refreshVisible !== false" icon="Refresh" text :loading="loading" @click="loadData" />
      </div>
    </div>

    <div v-if="visibleCards.length" class="review-workbench__cards" :style="cardsGridStyle">
      <article v-for="item in visibleCards" :key="item.key" class="review-workbench__card" :style="cardStyle(item)" @click="goCard(item)">
        <img v-if="item.imageUrl" :src="item.imageUrl" alt="" />
        <i v-else :style="{ backgroundColor: item.accentColor }"></i>
        <div>
          <strong :style="{ color: item.titleColor }">{{ item.label }}</strong>
          <span :style="{ color: item.numberColor }">{{ valueOf(item.valueKey || item.key) }}</span>
        </div>
      </article>
    </div>

    <el-tabs v-model="activeTab" class="review-workbench__tabs">
      <el-tab-pane :label="resolvedConfig.pendingTabLabel" name="pending">
        <el-table
          v-loading="loading && !loaded"
          :data="pendingTasks"
          :border="false"
          :max-height="420"
          row-class-name="review-workbench__table-row"
          empty-text="暂无待评分作品"
          @row-click="goTask"
        >
          <el-table-column label="作品名称" prop="projectName" min-width="220" show-overflow-tooltip />
          <el-table-column label="类别" prop="categoryName" min-width="150" show-overflow-tooltip />
          <el-table-column label="提交时间" prop="submittedAt" width="170" />
          <el-table-column label="操作" width="110" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" icon="EditPen" @click.stop="goTask(row)">评分</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane :label="resolvedConfig.recentTabLabel" name="recent">
        <el-table
          v-loading="loading && !loaded"
          :data="recentScores"
          :border="false"
          :max-height="420"
          row-class-name="review-workbench__table-row"
          empty-text="暂无最近评分"
          @row-click="goScore"
        >
          <el-table-column label="作品名称" prop="projectName" min-width="220" show-overflow-tooltip />
          <el-table-column label="类别" prop="categoryName" min-width="140" show-overflow-tooltip />
          <el-table-column label="得分/等级" width="110">
            <template #default="{ row }">{{ scoreText(row) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'submitted' ? 'success' : 'info'">{{ row.status === 'submitted' ? '已提交' : '草稿' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" prop="submittedAt" width="170">
            <template #default="{ row }">{{ row.submittedAt || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" icon="View" @click.stop="goScore(row)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
    <div v-if="runtimeStyleHtml" v-html="runtimeStyleHtml"></div>
  </section>
</template>

<script setup lang="ts">
import { getReviewWorkbench } from '@/api/crehn/review';
import { ReviewScoreVO, ReviewTaskVO, ReviewWorkbenchVO } from '@/api/crehn/types';

type ReviewWorkbenchCardConfig = {
  key: string;
  label: string;
  valueKey?: string;
  visible?: boolean;
  order?: number;
  imageUrl?: string;
  titleColor?: string;
  numberColor?: string;
  backgroundColor?: string;
  borderColor?: string;
  accentColor?: string;
  linkStatus?: string;
};

type ReviewWorkbenchConfig = {
  kickerVisible: boolean;
  kickerText: string;
  kickerFontSize: number;
  titleVisible: boolean;
  titleText: string;
  titleFontSize: number;
  refreshVisible: boolean;
  listButtonVisible: boolean;
  listButtonText: string;
  cardColumns: number;
  pendingTabLabel: string;
  recentTabLabel: string;
  componentCss: string;
  cards: ReviewWorkbenchCardConfig[];
};

const props = defineProps<{ title: string; configJson?: string }>();

const router = useRouter();
const loading = ref(false);
const loaded = ref(false);
const activeTab = ref('pending');
const overview = ref<ReviewWorkbenchVO>({});

const defaultCardStyle = {
  titleColor: '#082f49',
  numberColor: '#2563eb',
  backgroundColor: '#f8fbff',
  borderColor: '#bfd7ff',
  accentColor: '#2563eb'
};

const defaultConfig: ReviewWorkbenchConfig = {
  kickerVisible: true,
  kickerText: 'SCORE',
  kickerFontSize: 12,
  titleVisible: true,
  titleText: '',
  titleFontSize: 20,
  refreshVisible: true,
  listButtonVisible: true,
  listButtonText: '进入评分列表',
  cardColumns: 5,
  pendingTabLabel: '待评分',
  recentTabLabel: '最近评分',
  componentCss: '',
  cards: [
    { key: 'assigned_total', label: '分配总数', visible: true, order: 1, ...defaultCardStyle, linkStatus: 'all' },
    {
      key: 'pending_score',
      label: '待评分',
      visible: true,
      order: 2,
      ...defaultCardStyle,
      numberColor: '#d97706',
      backgroundColor: '#fffaf0',
      borderColor: '#f4d99d',
      accentColor: '#d97706',
      linkStatus: 'none'
    },
    {
      key: 'draft_score',
      label: '草稿',
      visible: true,
      order: 3,
      ...defaultCardStyle,
      numberColor: '#0891b2',
      backgroundColor: '#f7fcff',
      borderColor: '#c7e6f4',
      accentColor: '#0284c7',
      linkStatus: 'draft'
    },
    {
      key: 'submitted_score',
      label: '已提交',
      visible: true,
      order: 4,
      ...defaultCardStyle,
      numberColor: '#16a34a',
      backgroundColor: '#f6fdfa',
      borderColor: '#cdeee1',
      accentColor: '#16a34a',
      linkStatus: 'submitted'
    },
    {
      key: 'locked_by_other',
      label: '已锁定',
      visible: true,
      order: 5,
      ...defaultCardStyle,
      numberColor: '#dc2626',
      backgroundColor: '#fef2f2',
      borderColor: '#fecaca',
      accentColor: '#ef4444'
    }
  ]
};

const clampNumber = (value: unknown, fallback: number, min: number, max: number) => {
  const numeric = Number(value ?? fallback);
  if (!Number.isFinite(numeric)) return fallback;
  return Math.min(max, Math.max(min, numeric));
};

const parseConfig = (value?: string): ReviewWorkbenchConfig => {
  let custom: Partial<ReviewWorkbenchConfig> = {};
  if (value) {
    try {
      custom = JSON.parse(value);
    } catch {
      custom = {};
    }
  }
  return {
    ...defaultConfig,
    ...custom,
    kickerVisible: custom.kickerVisible !== false,
    titleVisible: custom.titleVisible !== false,
    refreshVisible: custom.refreshVisible !== false,
    listButtonVisible: custom.listButtonVisible !== false,
    kickerFontSize: clampNumber(custom.kickerFontSize, defaultConfig.kickerFontSize, 10, 24),
    titleFontSize: clampNumber(custom.titleFontSize, defaultConfig.titleFontSize, 14, 32),
    cardColumns: clampNumber(custom.cardColumns, defaultConfig.cardColumns, 1, 6),
    cards: mergeCards(defaultConfig.cards, custom.cards)
  };
};

const mergeCards = (base: ReviewWorkbenchCardConfig[], custom?: ReviewWorkbenchCardConfig[]) => {
  const customMap = new Map((custom || []).map((item) => [item.key, item]));
  const rows = base.map((item) => ({ ...item, ...(customMap.get(item.key) || {}) }));
  (custom || []).forEach((item) => {
    if (!rows.some((row) => row.key === item.key)) rows.push(item);
  });
  return rows;
};

const sanitizeCss = (value?: string) =>
  String(value || '')
    .replace(/<\/style/gi, '<\\/style')
    .replace(/<script/gi, '/* script')
    .replace(/<\/script>/gi, 'script */');

const resolvedConfig = computed(() => parseConfig(props.configJson));
const displayTitle = computed(() => resolvedConfig.value.titleText || props.title || '评分工作台');
const pendingTasks = computed(() => overview.value.pendingTasks || []);
const recentScores = computed(() => overview.value.recentScores || []);
const visibleCards = computed(() =>
  resolvedConfig.value.cards.filter((item) => item.visible !== false).sort((a, b) => Number(a.order || 0) - Number(b.order || 0))
);
const cardsGridStyle = computed(() => ({ gridTemplateColumns: `repeat(${resolvedConfig.value.cardColumns}, minmax(0, 1fr))` }));
const rootStyle = computed(() => ({ '--review-workbench-card-columns': String(resolvedConfig.value.cardColumns) }));
const runtimeStyleHtml = computed(() => {
  const css = sanitizeCss(resolvedConfig.value.componentCss);
  return css ? `<style>${css}</style>` : '';
});

const valueOf = (key?: string) => Number(overview.value.stats?.[key || ''] || 0);
const cardLinkStatus = (item: ReviewWorkbenchCardConfig) => {
  if (item.key === 'locked_by_other') return '';
  return item.linkStatus || (item.key === 'assigned_total' ? 'all' : '');
};
const hasCardLink = (item: ReviewWorkbenchCardConfig) => !!cardLinkStatus(item);
const cardStyle = (item: ReviewWorkbenchCardConfig) => ({
  borderColor: item.borderColor || defaultCardStyle.borderColor,
  background: item.backgroundColor || defaultCardStyle.backgroundColor,
  cursor: hasCardLink(item) ? 'pointer' : 'default'
});
const scoreText = (row: ReviewScoreVO) => row.gradeValue || (row.scoreValue ?? '-');

const loadData = async () => {
  loading.value = true;
  try {
    const { data } = await getReviewWorkbench();
    overview.value = data || {};
    loaded.value = true;
  } finally {
    loading.value = false;
  }
};

const goReviewList = () => {
  router.push({ path: '/crehn/review' });
};

const goCard = (item: ReviewWorkbenchCardConfig) => {
  const linkStatus = cardLinkStatus(item);
  if (!linkStatus) return;
  router.push({ path: '/crehn/review', query: linkStatus === 'all' ? {} : { scoreStatus: linkStatus } });
};

const goTask = (row: ReviewTaskVO) => {
  router.push({ path: '/crehn/review', query: { scoreStatus: 'none', assignmentId: row.assignmentId, projectId: row.projectId } });
};

const goScore = (row: ReviewScoreVO) => {
  router.push({ path: '/crehn/review', query: { assignmentId: row.assignmentId, projectId: row.projectId } });
};

onMounted(loadData);
onActivated(loadData);
</script>

<style scoped lang="scss">
.review-workbench {
  overflow: hidden;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  background: #ffffff;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.025);
}

.review-workbench__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.review-workbench__head span {
  display: block;
  color: #2563eb;
  font-weight: 900;
}

.review-workbench__head h2 {
  margin: 4px 0 0;
  color: #082f49;
  font-weight: 950;
}

.review-workbench__actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.review-workbench__list-button.el-button {
  color: #ffffff;
  border-color: #2563eb;
  background: #2563eb;
}

.review-workbench__cards {
  display: grid;
  grid-template-columns: repeat(var(--review-workbench-card-columns, 5), minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.review-workbench__card {
  min-width: 0;
  min-height: 110px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid #d8e6f5;
  border-radius: 8px;
}

.review-workbench__card img {
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  object-fit: cover;
  border-radius: 8px;
}

.review-workbench__card i {
  width: 6px;
  height: 48px;
  flex: 0 0 6px;
  border-radius: 6px;
}

.review-workbench__card div {
  min-width: 0;
}

.review-workbench__card strong {
  display: block;
  font-size: 14px;
  font-weight: 900;
}

.review-workbench__card span {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  line-height: 1;
  font-weight: 950;
}

.review-workbench__tabs {
  margin-top: 2px;
}

.review-workbench .review-workbench__tabs :deep(.el-table) {
  --el-table-border-color: #eef2f7;
  --el-table-header-bg-color: #f8fafc;
  --el-table-row-hover-bg-color: #f6f9fd;
}

.review-workbench .review-workbench__tabs :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.review-workbench .review-workbench__tabs :deep(.el-table th.el-table__cell) {
  border-right: 0;
  border-bottom-color: #e8edf5;
  background: #f8fafc;
}

.review-workbench .review-workbench__tabs :deep(.el-table td.el-table__cell) {
  border-right: 0;
  border-bottom-color: #eef2f7;
}

.review-workbench__tabs :deep(.review-workbench__table-row) {
  cursor: pointer;
}

.review-workbench__tabs :deep(.review-workbench__table-row:hover > td.el-table__cell) {
  background: #f8fbff;
}

@media (max-width: 1100px) {
  .review-workbench__cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .review-workbench__head {
    align-items: flex-start;
    flex-direction: column;
  }

  .review-workbench__cards {
    grid-template-columns: 1fr;
  }
}
</style>
