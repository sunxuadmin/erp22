<template>
  <section class="workbench-card audit-workbench" :style="rootStyle">
    <div v-if="headerVisible" class="workbench-card__head audit-workbench__head">
      <div class="audit-workbench__title">
        <span v-if="resolvedConfig.kickerVisible !== false" :style="{ fontSize: `${resolvedConfig.kickerFontSize}px` }">
          {{ resolvedConfig.kickerText }}
        </span>
        <h2 v-if="resolvedConfig.titleVisible !== false" :style="{ fontSize: `${resolvedConfig.titleFontSize}px` }">{{ displayTitle }}</h2>
      </div>

      <div v-if="resolvedConfig.metricsVisible !== false" class="audit-workbench__header-metrics">
        <span v-for="item in myMetricItems" :key="item.key" class="audit-workbench__header-metric" :style="metricItemStyle(item)">
          <em>{{ item.label }}</em>
          <strong>{{ item.value }}</strong>
        </span>
      </div>

      <div class="audit-workbench__actions">
        <el-button
          v-if="resolvedConfig.auditListButtonVisible !== false"
          class="audit-workbench__list-button"
          type="primary"
          icon="List"
          @click="goAuditList"
        >
          {{ resolvedConfig.auditListButtonText }}
        </el-button>
        <el-button v-if="resolvedConfig.refreshVisible !== false" icon="Refresh" text :loading="loading" @click="loadData" />
      </div>
    </div>

    <el-tabs v-if="visibleTabNames.length" v-model="activeTab" class="audit-workbench__tabs">
      <el-tab-pane v-if="pendingTabVisible" :label="resolvedConfig.pendingTabLabel" name="pending">
        <div class="audit-workbench__toolbar">
          <strong>{{ resolvedConfig.pendingSectionTitle }}</strong>
        </div>
        <el-table v-loading="loading && !loaded" :data="pendingProjects" border :max-height="420" empty-text="暂无待审核项目">
          <el-table-column label="项目名称" prop="projectName" min-width="220" show-overflow-tooltip />
          <el-table-column label="学校" prop="schoolName" min-width="160" show-overflow-tooltip />
          <el-table-column label="类别" prop="categoryName" min-width="140" show-overflow-tooltip />
          <el-table-column label="提交时间" prop="submittedAt" width="170" />
          <el-table-column label="操作" width="110" fixed="right" align="center">
            <template #default="{ row }">
              <el-button link type="primary" icon="EditPen" @click="goPendingProject(row)">审核</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane v-if="mineTabVisible" :label="resolvedConfig.mineTabLabel" name="mine">
        <div v-if="resolvedConfig.groupVisible !== false" class="audit-workbench__section">
          <div class="audit-workbench__section-title">
            <strong>{{ resolvedConfig.groupSectionTitle }}</strong>
            <span>{{ resolvedConfig.groupSectionSubtitle }}</span>
          </div>
          <div v-if="myGroupItems.length" class="audit-workbench__groups" :style="groupGridStyle">
            <article v-for="item in myGroupItems" :key="item.groupKey" class="audit-workbench__group">
              <header>
                <strong>{{ item.groupName || '其他类别' }}</strong>
                <span>{{ numberOf(item.totalCount) }}</span>
              </header>
              <div>
                <span>通过 {{ numberOf(item.passCount) }}</span>
                <span>退回 {{ numberOf(item.returnCount) }}</span>
              </div>
            </article>
          </div>
          <el-empty v-else description="暂无本人审核统计" />
        </div>

        <div v-if="resolvedConfig.recordsVisible !== false" class="audit-workbench__section">
          <div class="audit-workbench__section-title">
            <strong>{{ resolvedConfig.recordsSectionTitle }}</strong>
            <span>{{ resolvedConfig.recordsSectionSubtitle }}</span>
          </div>
          <el-table v-loading="loading && !loaded" :data="myRecords" border :max-height="420" empty-text="暂无本人审核记录">
            <el-table-column label="项目名称" prop="projectName" min-width="220" show-overflow-tooltip />
            <el-table-column label="学校" prop="schoolName" min-width="150" show-overflow-tooltip />
            <el-table-column label="类别" prop="categoryName" min-width="130" show-overflow-tooltip />
            <el-table-column label="结果" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="auditResultType(row.auditResult)">{{ auditResultLabel(row.auditResult) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="审核时间" prop="auditedAt" width="170" />
            <el-table-column label="操作" width="100" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" icon="View" @click="goRecordProject(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
    <el-empty v-else description="暂无可显示内容" />
    <div v-if="runtimeStyleHtml" v-html="runtimeStyleHtml"></div>
  </section>
</template>

<script setup lang="ts">
import { getAuditWorkbench } from '@/api/crehn/audit';
import { AuditWorkbenchProjectVO, AuditWorkbenchRecordVO, AuditWorkbenchVO } from '@/api/crehn/types';

type AuditWorkbenchConfig = {
  kickerVisible: boolean;
  kickerText: string;
  kickerFontSize: number;
  titleVisible: boolean;
  titleText: string;
  titleFontSize: number;
  refreshVisible: boolean;
  defaultTab: string;
  pendingTabVisible: boolean;
  pendingTabLabel: string;
  pendingSectionTitle: string;
  auditListButtonVisible: boolean;
  auditListButtonText: string;
  mineTabVisible: boolean;
  mineTabLabel: string;
  metricsVisible: boolean;
  metricColumns: number;
  metricTotalLabel: string;
  metricPassLabel: string;
  metricReturnLabel: string;
  metricLabelColor: string;
  metricTotalColor: string;
  metricPassColor: string;
  metricReturnColor: string;
  metricFontSize: number;
  metricNumberFontSize: number;
  groupVisible: boolean;
  groupSectionTitle: string;
  groupSectionSubtitle: string;
  groupColumns: number;
  recordsVisible: boolean;
  recordsSectionTitle: string;
  recordsSectionSubtitle: string;
  componentCss: string;
};

type AuditWorkbenchMetricItem = {
  key: string;
  label: string;
  value: number;
  color: string;
};

const props = defineProps<{ title: string; configJson?: string }>();

const router = useRouter();
const activeTab = ref('pending');
const tabInitialized = ref(false);
const loading = ref(false);
const loaded = ref(false);
const overview = ref<AuditWorkbenchVO>({});

const auditRoutePath = '/crehn/audit';

const defaultConfig: AuditWorkbenchConfig = {
  kickerVisible: true,
  kickerText: 'AUDIT',
  kickerFontSize: 12,
  titleVisible: true,
  titleText: '',
  titleFontSize: 20,
  refreshVisible: true,
  defaultTab: 'pending',
  pendingTabVisible: true,
  pendingTabLabel: '待审核',
  pendingSectionTitle: '最近待审核',
  auditListButtonVisible: true,
  auditListButtonText: '进入审核列表',
  mineTabVisible: true,
  mineTabLabel: '我的审核',
  metricsVisible: true,
  metricColumns: 3,
  metricTotalLabel: '我审核总数',
  metricPassLabel: '通过',
  metricReturnLabel: '退回',
  metricLabelColor: '#5c7087',
  metricTotalColor: '#2563eb',
  metricPassColor: '#16a34a',
  metricReturnColor: '#dc2626',
  metricFontSize: 13,
  metricNumberFontSize: 18,
  groupVisible: true,
  groupSectionTitle: '大类简版统计',
  groupSectionSubtitle: '按本人通过/退回记录汇总',
  groupColumns: 5,
  recordsVisible: true,
  recordsSectionTitle: '最近审核记录',
  recordsSectionSubtitle: '仅统计本人点击通过/退回的项目',
  componentCss: ''
};

const pendingProjects = computed(() => overview.value.pendingProjects || []);
const myGroupItems = computed(() => overview.value.myGroupItems || []);
const myRecords = computed(() => (overview.value.myRecords || []).slice(0, 10));
const myStats = computed(() => overview.value.myStats || {});
const numberOf = (value?: number) => Number(value || 0);
const clampNumber = (value: unknown, fallback: number, min: number, max: number) => {
  const numeric = Number(value ?? fallback);
  if (!Number.isFinite(numeric)) return fallback;
  return Math.min(max, Math.max(min, numeric));
};

const parseConfig = (value?: string): AuditWorkbenchConfig => {
  let custom: Partial<AuditWorkbenchConfig> = {};
  if (value) {
    try {
      custom = JSON.parse(value);
    } catch {
      custom = {};
    }
  }
  return {
    kickerVisible: custom.kickerVisible !== false,
    kickerText: custom.kickerText || defaultConfig.kickerText,
    kickerFontSize: clampNumber(custom.kickerFontSize, defaultConfig.kickerFontSize, 10, 24),
    titleVisible: custom.titleVisible !== false,
    titleText: custom.titleText || '',
    titleFontSize: clampNumber(custom.titleFontSize, defaultConfig.titleFontSize, 14, 32),
    refreshVisible: custom.refreshVisible !== false,
    defaultTab: custom.defaultTab || defaultConfig.defaultTab,
    pendingTabVisible: custom.pendingTabVisible !== false,
    pendingTabLabel: custom.pendingTabLabel || defaultConfig.pendingTabLabel,
    pendingSectionTitle: custom.pendingSectionTitle || defaultConfig.pendingSectionTitle,
    auditListButtonVisible: custom.auditListButtonVisible !== false,
    auditListButtonText: custom.auditListButtonText || defaultConfig.auditListButtonText,
    mineTabVisible: custom.mineTabVisible !== false,
    mineTabLabel: custom.mineTabLabel || defaultConfig.mineTabLabel,
    metricsVisible: custom.metricsVisible !== false,
    metricColumns: clampNumber(custom.metricColumns, defaultConfig.metricColumns, 1, 4),
    metricTotalLabel: custom.metricTotalLabel || defaultConfig.metricTotalLabel,
    metricPassLabel: custom.metricPassLabel || defaultConfig.metricPassLabel,
    metricReturnLabel: custom.metricReturnLabel || defaultConfig.metricReturnLabel,
    metricLabelColor: custom.metricLabelColor || defaultConfig.metricLabelColor,
    metricTotalColor: custom.metricTotalColor || defaultConfig.metricTotalColor,
    metricPassColor: custom.metricPassColor || defaultConfig.metricPassColor,
    metricReturnColor: custom.metricReturnColor || defaultConfig.metricReturnColor,
    metricFontSize: clampNumber(custom.metricFontSize, defaultConfig.metricFontSize, 10, 20),
    metricNumberFontSize: clampNumber(custom.metricNumberFontSize, defaultConfig.metricNumberFontSize, 12, 28),
    groupVisible: custom.groupVisible !== false,
    groupSectionTitle: custom.groupSectionTitle || defaultConfig.groupSectionTitle,
    groupSectionSubtitle: custom.groupSectionSubtitle || defaultConfig.groupSectionSubtitle,
    groupColumns: clampNumber(custom.groupColumns, defaultConfig.groupColumns, 1, 6),
    recordsVisible: custom.recordsVisible !== false,
    recordsSectionTitle: custom.recordsSectionTitle || defaultConfig.recordsSectionTitle,
    recordsSectionSubtitle: custom.recordsSectionSubtitle || defaultConfig.recordsSectionSubtitle,
    componentCss: custom.componentCss || ''
  };
};

const sanitizeCss = (value?: string) =>
  String(value || '')
    .replace(/<\/style/gi, '<\\/style')
    .replace(/<script/gi, '/* script')
    .replace(/<\/script>/gi, 'script */');

const resolvedConfig = computed(() => parseConfig(props.configJson));
const displayTitle = computed(() => resolvedConfig.value.titleText || props.title || '审核工作台');
const headerVisible = computed(
  () =>
    resolvedConfig.value.kickerVisible !== false ||
    resolvedConfig.value.titleVisible !== false ||
    resolvedConfig.value.metricsVisible !== false ||
    resolvedConfig.value.auditListButtonVisible !== false ||
    resolvedConfig.value.refreshVisible !== false
);
const pendingTabVisible = computed(() => resolvedConfig.value.pendingTabVisible !== false);
const mineTabVisible = computed(() => resolvedConfig.value.mineTabVisible !== false);
const visibleTabNames = computed(() => {
  const tabs: string[] = [];
  if (pendingTabVisible.value) tabs.push('pending');
  if (mineTabVisible.value) tabs.push('mine');
  return tabs;
});
const rootStyle = computed(() => ({
  '--audit-workbench-group-columns': String(resolvedConfig.value.groupColumns),
  '--audit-workbench-metric-label-color': resolvedConfig.value.metricLabelColor,
  '--audit-workbench-metric-font-size': `${resolvedConfig.value.metricFontSize}px`,
  '--audit-workbench-metric-number-font-size': `${resolvedConfig.value.metricNumberFontSize}px`
}));
const groupGridStyle = computed(() => ({ gridTemplateColumns: `repeat(${resolvedConfig.value.groupColumns}, minmax(0, 1fr))` }));
const runtimeStyleHtml = computed(() => {
  const css = sanitizeCss(resolvedConfig.value.componentCss);
  return css ? `<style>${css}</style>` : '';
});

const myMetricItems = computed<AuditWorkbenchMetricItem[]>(() => [
  {
    key: 'total',
    label: resolvedConfig.value.metricTotalLabel,
    value: numberOf(myStats.value.totalCount),
    color: resolvedConfig.value.metricTotalColor
  },
  {
    key: 'pass',
    label: resolvedConfig.value.metricPassLabel,
    value: numberOf(myStats.value.passCount),
    color: resolvedConfig.value.metricPassColor
  },
  {
    key: 'return',
    label: resolvedConfig.value.metricReturnLabel,
    value: numberOf(myStats.value.returnCount),
    color: resolvedConfig.value.metricReturnColor
  }
]);

const metricItemStyle = (item: AuditWorkbenchMetricItem) => ({
  '--audit-workbench-metric-number-color': item.color
});

const loadData = async () => {
  loading.value = true;
  try {
    const { data } = await getAuditWorkbench();
    overview.value = data || {};
    loaded.value = true;
  } finally {
    loading.value = false;
  }
};

const pushAuditRoute = (query: Record<string, string | number | undefined>) => {
  const normalizedQuery = Object.fromEntries(Object.entries(query).filter(([, value]) => value !== undefined && value !== ''));
  router.push({ path: auditRoutePath, query: normalizedQuery });
};

const goAuditList = () => {
  pushAuditRoute({ status: 'submitted' });
};

const goPendingProject = (row: AuditWorkbenchProjectVO) => {
  pushAuditRoute({ status: 'submitted', projectId: row.id });
};

const goRecordProject = (row: AuditWorkbenchRecordVO) => {
  pushAuditRoute({ status: row.status, projectId: row.projectId });
};

const auditResultLabel = (result?: string) => ({ pass: '通过', return: '退回' })[result || ''] || result || '-';
const auditResultType = (result?: string) => (result === 'pass' ? 'success' : result === 'return' ? 'danger' : 'info');

watch(
  resolvedConfig,
  () => {
    const tabs = visibleTabNames.value;
    if (!tabs.length) return;
    if (!tabInitialized.value) {
      activeTab.value = tabs.includes(resolvedConfig.value.defaultTab) ? resolvedConfig.value.defaultTab : tabs[0];
      tabInitialized.value = true;
      return;
    }
    if (!tabs.includes(activeTab.value)) {
      activeTab.value = tabs.includes(resolvedConfig.value.defaultTab) ? resolvedConfig.value.defaultTab : tabs[0];
    }
  },
  { immediate: true }
);

onMounted(loadData);
onActivated(loadData);
</script>

<style scoped lang="scss">
.audit-workbench {
  overflow: hidden;
}

.audit-workbench__head {
  align-items: center;
  gap: 16px;
}

.audit-workbench__title {
  min-width: 150px;
}

.audit-workbench__title span {
  display: block;
}

.audit-workbench__header-metrics {
  flex: 1;
  min-width: 260px;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 8px 18px;
}

.audit-workbench__header-metric {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  color: var(--audit-workbench-metric-label-color, #5c7087);
  font-size: var(--audit-workbench-metric-font-size, 13px);
  line-height: 1.3;
  white-space: nowrap;
}

.audit-workbench__header-metric em {
  font-style: normal;
  font-weight: 800;
}

.audit-workbench__header-metric strong {
  color: var(--audit-workbench-metric-number-color, #2563eb);
  font-size: var(--audit-workbench-metric-number-font-size, 18px);
  line-height: 1;
  font-weight: 950;
}

.audit-workbench__actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.audit-workbench__list-button.el-button {
  min-width: 136px;
  color: #ffffff;
  border-color: #2563eb;
  background: #2563eb;
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.12);
}

.audit-workbench__list-button.el-button:hover,
.audit-workbench__list-button.el-button:focus,
.audit-workbench__list-button.el-button:active {
  color: #ffffff;
  border-color: #1d4ed8;
  background: #1d4ed8;
}

.audit-workbench__list-button :deep(.el-icon),
.audit-workbench__list-button :deep(span) {
  color: inherit;
}

.audit-workbench__tabs {
  margin-top: -2px;
}

.audit-workbench__toolbar,
.audit-workbench__section-title {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.audit-workbench__toolbar strong,
.audit-workbench__section-title strong {
  color: #082f49;
  font-size: 16px;
  font-weight: 900;
}

.audit-workbench__section-title span {
  color: #6b7c8f;
  font-size: 13px;
}

.audit-workbench__section {
  margin-top: 20px;
}

.audit-workbench__section:first-child {
  margin-top: 0;
}

.audit-workbench__groups {
  display: grid;
  grid-template-columns: repeat(var(--audit-workbench-group-columns, 5), minmax(0, 1fr));
  gap: 10px;
}

.audit-workbench__group {
  min-width: 0;
  padding: 14px;
  border: 1px solid #d8e6f5;
  border-radius: 8px;
  background: #f8fbff;
}

.audit-workbench__group header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.audit-workbench__group header strong {
  color: #102a43;
  font-size: 14px;
  line-height: 1.35;
}

.audit-workbench__group header span {
  color: #2563eb;
  font-size: 24px;
  line-height: 1;
  font-weight: 950;
}

.audit-workbench__group div {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  color: #375a7f;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .audit-workbench__groups {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .audit-workbench__head,
  .audit-workbench__toolbar,
  .audit-workbench__section-title {
    align-items: stretch;
    flex-direction: column;
  }

  .audit-workbench__header-metrics {
    min-width: 0;
  }

  .audit-workbench__actions {
    justify-content: flex-start;
  }

  .audit-workbench__groups {
    grid-template-columns: 1fr;
  }
}
</style>
