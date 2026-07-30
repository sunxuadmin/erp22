<template>
  <section class="workbench-card universal-dashboard" :style="rootStyle">
    <header v-if="resolvedConfig.showHeader" class="universal-dashboard__head">
      <div>
        <span>{{ resolvedConfig.headerKicker }}</span>
        <h2>{{ title || '数据看板' }}</h2>
        <p v-if="resolvedConfig.subtitle">{{ resolvedConfig.subtitle }}</p>
      </div>
      <div class="universal-dashboard__head-actions">
        <el-button v-if="resolvedConfig.headerAction.visible" type="primary" link :disabled="preview" @click="runHeaderAction">
          {{ resolvedConfig.headerAction.label }}
          <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
        <el-button v-if="!preview" :loading="loading" link icon="Refresh" :aria-label="`${resolvedConfig.refreshLabel}数据看板`" @click="loadData">
          {{ resolvedConfig.refreshLabel }}
        </el-button>
      </div>
    </header>

    <el-alert v-if="loadError" class="universal-dashboard__alert" :title="loadError" type="warning" :closable="false" show-icon />

    <div v-loading="loading" class="universal-dashboard__grid">
      <article
        v-for="item in visibleBlocks"
        :key="item.id"
        class="universal-dashboard__block"
        :class="[`is-${item.type}`, { 'is-clickable': hasAction(item) }]"
        :style="blockStyle(item)"
        :tabindex="hasAction(item) ? 0 : undefined"
        @click="runAction(item)"
        @keydown.enter.prevent="runAction(item)"
        @keydown.space.prevent="runAction(item)"
      >
        <template v-if="item.type === 'metric'">
          <div class="universal-dashboard__metric-accent" />
          <div class="universal-dashboard__metric-copy">
            <span>{{ item.title }}</span>
            <strong :style="{ color: item.color }">{{ metricText(item) }}</strong>
            <small v-if="item.subtitle">{{ item.subtitle }}</small>
          </div>
          <el-icon v-if="hasAction(item)" class="universal-dashboard__corner"><ArrowRight /></el-icon>
        </template>

        <template v-else-if="item.type === 'progress'">
          <div class="universal-dashboard__block-title">
            <div>
              <strong>{{ item.title }}</strong>
              <span v-if="item.subtitle">{{ item.subtitle }}</span>
            </div>
            <em>{{ progressValue(item).toFixed(item.decimals || 0) }}%</em>
          </div>
          <el-progress
            type="dashboard"
            :percentage="progressValue(item)"
            :width="118"
            :stroke-width="10"
            :color="item.color || resolvedConfig.theme.accentColor"
          />
        </template>

        <template v-else-if="['donut', 'bar', 'line'].includes(item.type)">
          <div class="universal-dashboard__block-title">
            <div>
              <strong>{{ item.title }}</strong>
              <span v-if="item.subtitle">{{ item.subtitle }}</span>
            </div>
          </div>
          <UniversalDashboardChart
            v-if="seriesOf(item).length"
            class="universal-dashboard__chart"
            :type="chartType(item)"
            :title="item.title"
            :data="seriesOf(item)"
            :theme="resolvedConfig.theme"
            :show-legend="item.showLegend"
            :show-labels="item.showLabels"
            :limit="item.limit"
          />
          <el-empty v-else :image-size="52" :description="resolvedConfig.emptyChartText" />
        </template>

        <template v-else-if="item.type === 'ranking'">
          <div class="universal-dashboard__block-title">
            <div>
              <strong>{{ item.title }}</strong>
              <span v-if="item.subtitle">{{ item.subtitle }}</span>
            </div>
          </div>
          <div v-if="rankingRows(item).length" class="universal-dashboard__ranking">
            <div v-for="(row, index) in rankingRows(item)" :key="`${row.name}-${index}`" class="universal-dashboard__ranking-row">
              <b :class="{ 'is-top': index < 3 }">{{ index + 1 }}</b>
              <div>
                <span>{{ row.name }}</span>
                <i><em :style="{ width: `${rankingWidth(item, row.value)}%`, background: rankingColor(index) }" /></i>
              </div>
              <strong>{{ formatNumber(row.value, item.decimals) }}{{ item.unit || '' }}</strong>
            </div>
          </div>
          <el-empty v-else :image-size="52" :description="resolvedConfig.emptyRankingText" />
        </template>

        <template v-else-if="item.type === 'list'">
          <div class="universal-dashboard__block-title">
            <div>
              <strong>{{ item.title }}</strong>
              <span v-if="item.subtitle">{{ item.subtitle }}</span>
            </div>
            <el-button v-if="hasAction(item)" type="primary" link size="small" @click.stop="runAction(item)">
              {{ item.action?.label || resolvedConfig.listActionFallbackLabel }}
              <el-icon class="el-icon--right"><ArrowRight /></el-icon>
            </el-button>
          </div>
          <div v-if="listRows(item).length" class="universal-dashboard__list">
            <div v-for="row in listRows(item)" :key="row.id" class="universal-dashboard__list-row">
              <i :class="`is-${row.tone || 'info'}`" />
              <div>
                <strong :class="{ 'is-project-link': row.projectId }" :title="row.title" @click.stop="openProject(row)">{{ row.title }}</strong>
                <span v-if="row.subtitle">{{ row.subtitle }}</span>
              </div>
              <em v-if="row.badge">{{ row.badge }}</em>
              <time v-if="row.time">{{ formatTime(row.time) }}</time>
            </div>
          </div>
          <el-empty v-else :image-size="52" :description="resolvedConfig.emptyListText" />
        </template>

        <template v-else-if="item.type === 'action'">
          <div class="universal-dashboard__action-icon" :style="{ color: item.color }">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="universal-dashboard__action-copy">
            <strong>{{ item.title }}</strong>
            <span>{{ item.subtitle || actionDescription(item) }}</span>
          </div>
          <el-button :color="item.color || resolvedConfig.theme.accentColor" round @click.stop="runAction(item)">
            {{ item.action?.label || resolvedConfig.actionFallbackLabel }}
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
        </template>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { getAuditWorkbench, listAuditProject } from '@/api/crehn/audit';
import { listSchoolInfo } from '@/api/crehn/config';
import { getMyProjectDashboard, getMyProjectQuotaOverview } from '@/api/crehn/project';
import { getUploadOverview, getUploadSummary } from '@/api/crehn/result';
import { getReviewWorkbench } from '@/api/crehn/review';
import type {
  AuditWorkbenchVO,
  ProjectQuotaOverviewVO,
  ProjectUploadOverviewVO,
  ProjectUploadSummaryVO,
  ReviewWorkbenchVO,
  SchoolDashboardProjectVO,
  SchoolDashboardResultVO,
  SchoolDashboardVO
} from '@/api/crehn/types';
import { useUserStore } from '@/store/modules/user';
import { ArrowRight, DataAnalysis } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import UniversalDashboardChart from './UniversalDashboardChart.vue';
import {
  dashboardActionOptions,
  dashboardSourceFormat,
  dashboardSourceOptions,
  parseUniversalDashboardConfig,
  resolveDashboardRole,
  type DashboardBlockConfig,
  type DashboardListItem,
  type DashboardSeriesItem,
  type UniversalDashboardData
} from '../universalDashboard';

const props = withDefaults(
  defineProps<{
    title: string;
    configJson?: string;
    preview?: boolean;
    roleKey?: string;
  }>(),
  { preview: false, configJson: '', roleKey: '' }
);

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const loadError = ref('');
const data = ref<UniversalDashboardData>({ metrics: {}, series: {}, lists: {} });

const runtimeRole = computed(() =>
  props.roleKey ? resolveDashboardRole(props.roleKey) : resolveDashboardRole(userStore.roles, userStore.schoolId || undefined)
);
const resolvedConfig = computed(() => parseUniversalDashboardConfig(props.configJson, runtimeRole.value, !props.preview));
const visibleBlocks = computed(() => resolvedConfig.value.blocks.filter((item) => item.visible !== false));

const rootStyle = computed(
  () =>
    ({
      '--dashboard-columns': String(resolvedConfig.value.columns),
      '--dashboard-gap': `${resolvedConfig.value.gap}px`,
      '--dashboard-radius': `${resolvedConfig.value.radius}px`,
      '--dashboard-accent': resolvedConfig.value.theme.accentColor,
      '--dashboard-bg': resolvedConfig.value.theme.backgroundColor,
      '--dashboard-surface': resolvedConfig.value.theme.surfaceColor,
      '--dashboard-text': resolvedConfig.value.theme.textColor,
      '--dashboard-muted': resolvedConfig.value.theme.mutedColor,
      '--dashboard-border': resolvedConfig.value.theme.borderColor,
      '--dashboard-frame-bg':
        resolvedConfig.value.frameMode === 'transparent'
          ? 'transparent'
          : resolvedConfig.value.frameMode === 'custom'
            ? resolvedConfig.value.frameBackground
            : resolvedConfig.value.theme.backgroundColor,
      '--dashboard-frame-border':
        resolvedConfig.value.frameMode === 'transparent' || !resolvedConfig.value.frameBorderVisible
          ? 'transparent'
          : resolvedConfig.value.theme.borderColor,
      '--dashboard-frame-shadow':
        resolvedConfig.value.frameMode === 'transparent' || !resolvedConfig.value.frameShadowVisible ? 'none' : '0 4px 14px rgba(15, 23, 42, 0.06)',
      '--dashboard-frame-padding': resolvedConfig.value.frameMode === 'transparent' ? '0px' : '20px',
      '--dashboard-hover-transform': resolvedConfig.value.hoverLiftEnabled ? 'translateY(-2px)' : 'none',
      '--dashboard-hover-shadow': resolvedConfig.value.hoverLiftEnabled
        ? '0 8px 22px color-mix(in srgb, var(--dashboard-block-color) 13%, transparent)'
        : 'none',
      '--dashboard-hover-border': resolvedConfig.value.hoverBorderEnabled ? 'var(--dashboard-block-color)' : 'var(--dashboard-border)'
    }) as Record<string, string>
);

const formatNumber = (value: number, decimals = 0) =>
  Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: decimals, maximumFractionDigits: decimals });

const metricValue = (item: DashboardBlockConfig) => Number(data.value.metrics[item.sourceKey || ''] || 0);
const metricText = (item: DashboardBlockConfig) => {
  const suffix = item.unit || (dashboardSourceFormat(runtimeRole.value, item.sourceKey) === 'percent' ? '%' : '');
  return `${formatNumber(metricValue(item), item.decimals)}${suffix}`;
};
const progressValue = (item: DashboardBlockConfig) => Math.min(100, Math.max(0, metricValue(item)));
const seriesOf = (item: DashboardBlockConfig) => data.value.series[item.sourceKey || ''] || [];
const chartType = (item: DashboardBlockConfig) => item.type as 'donut' | 'bar' | 'line';
const rankingRows = (item: DashboardBlockConfig) =>
  [...seriesOf(item)].sort((a, b) => Number(b.value || 0) - Number(a.value || 0)).slice(0, Math.max(3, Math.min(20, Number(item.limit || 8))));
const rankingWidth = (item: DashboardBlockConfig, value: number) => {
  const max = Math.max(...rankingRows(item).map((row) => Number(row.value || 0)), 1);
  return Math.max(4, Math.min(100, (Number(value || 0) / max) * 100));
};
const rankingColor = (index: number) =>
  resolvedConfig.value.theme.chartColors[index % Math.max(1, resolvedConfig.value.theme.chartColors.length)] ||
  resolvedConfig.value.theme.accentColor;
const statusText: Record<string, string> = { draft: '草稿', submitted: '待审核', returned: '已退回', audit_passed: '已通过' };
const formatTime = (value?: string) => {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
};
const buildSchoolAlerts = (): DashboardListItem[] => {
  const metrics = data.value.metrics;
  return (resolvedConfig.value.warningRules || [])
    .filter((rule) => rule.enabled)
    .flatMap((rule) => {
      const value = Number(
        rule.key === 'quota_usage_rate'
          ? metrics['school.quota_usage_rate']
          : rule.key === 'returned_count'
            ? metrics['school.returned_count']
            : metrics['school.draft_count']
      );
      if (value < Number(rule.threshold || 0)) return [];
      const detail = rule.key === 'quota_usage_rate' ? `当前 ${formatNumber(value, 1)}%` : `${formatNumber(value)} 项`;
      return [{ id: `alert-${rule.key}`, title: rule.label, subtitle: detail, tone: rule.severity } satisfies DashboardListItem];
    });
};
const listRows = (item: DashboardBlockConfig) =>
  item.sourceKey === 'school.alerts'
    ? buildSchoolAlerts()
    : (data.value.lists[item.sourceKey || ''] || []).slice(0, Math.max(3, Number(item.limit || 8)));

const hasAction = (item: DashboardBlockConfig) => {
  const type = item.action?.type || 'none';
  return type !== 'none' && dashboardActionOptions(runtimeRole.value).some((option) => option.value === type);
};

const actionDescription = (item: DashboardBlockConfig) =>
  dashboardActionOptions(runtimeRole.value).find((option) => option.value === item.action?.type)?.description ||
  resolvedConfig.value.actionFallbackDescription;

const blockStyle = (item: DashboardBlockConfig) => ({
  gridColumn: `span ${Math.min(resolvedConfig.value.columns, Math.max(1, Number(item.span || 1)))}`,
  height: `${Math.max(110, Number(item.height || 126))}px`,
  '--dashboard-block-color': item.color || resolvedConfig.value.theme.accentColor,
  background: item.backgroundColor || resolvedConfig.value.theme.surfaceColor
});

const responseData = <T,>(result: PromiseSettledResult<any>): T | undefined =>
  result.status === 'fulfilled' ? (result.value?.data as T) : undefined;
const responseTotal = (result: PromiseSettledResult<any>) =>
  result.status === 'fulfilled' ? Number(result.value?.total ?? result.value?.data?.total ?? 0) : 0;
const percent = (numerator: number, denominator: number) => (denominator > 0 ? Number(((numerator / denominator) * 100).toFixed(1)) : 0);
const numberOf = (value: unknown) => Number(value || 0);

const buildPreviewData = (): UniversalDashboardData => {
  const metrics: Record<string, number> = {};
  const series: Record<string, DashboardSeriesItem[]> = {};
  const lists: Record<string, DashboardListItem[]> = {};
  dashboardSourceOptions(runtimeRole.value).forEach((source, index) => {
    if (source.kind === 'metric') {
      metrics[source.key] = source.format === 'percent' ? [68, 76, 84, 92][index % 4] : [181, 72, 51, 13, 6, 175][index % 6];
      return;
    }
    if (source.kind === 'list') {
      lists[source.key] = ['河南省大学生艺术展演', '节目名称示例', '类别填报进度'].map((title, rowIndex) => ({
        id: `${source.key}-${rowIndex}`,
        title,
        subtitle: rowIndex === 0 ? '艺术表演类' : '最近更新的项目',
        badge: rowIndex === 0 ? '待处理' : undefined,
        time: `2026-07-${String(18 - rowIndex).padStart(2, '0')} 10:30:00`,
        tone: rowIndex === 0 ? 'warning' : 'info'
      }));
      return;
    }
    const names = source.key.includes('status')
      ? ['待处理', '已完成', '已通过', '已退回']
      : source.key.includes('school')
        ? ['示范大学', '艺术学院', '师范大学', '职业学院', '综合大学']
        : ['艺术表演类', '艺术作品类', '实践工作坊', '美育成果', '校长书画'];
    series[source.key] = names.map((name, rowIndex) => ({
      name,
      value: source.format === 'percent' ? 54 + rowIndex * 9 : 18 + (names.length - rowIndex) * 11
    }));
  });
  return { metrics, series, lists };
};

const loadSchoolData = async (): Promise<UniversalDashboardData> => {
  const [overviewRes, dashboardRes] = await Promise.all([getMyProjectQuotaOverview({}), getMyProjectDashboard()]);
  const overview = overviewRes.data || {};
  const dashboard = (dashboardRes.data || {}) as SchoolDashboardVO;
  const value = overview as ProjectQuotaOverviewVO;
  const total = numberOf(value.totalCount);
  const used = numberOf(value.usedCount);
  const quota = numberOf(value.quotaLimit);
  const categoryItems = value.categoryItems || [];
  return {
    metrics: {
      'school.total_count': total,
      'school.draft_count': numberOf(value.draftCount),
      'school.submitted_count': numberOf(value.submittedCount),
      'school.completed_count': numberOf(value.completedCount),
      'school.returned_count': numberOf(dashboard.returnedCount),
      'school.published_result_count': numberOf(dashboard.publishedResultCount),
      'school.quota_limit': quota,
      'school.remaining_count': numberOf(value.remainingCount),
      'school.completion_rate': percent(numberOf(value.completedCount), total),
      'school.quota_usage_rate': percent(used, quota)
    },
    series: {
      'school.status_distribution': [
        { name: '草稿', value: numberOf(value.draftCount) },
        { name: '已提交', value: numberOf(value.submittedCount) },
        { name: '已完成', value: numberOf(value.completedCount) }
      ],
      'school.category_distribution': categoryItems.map((item) => ({
        name: item.categoryName || item.groupName || '未分类',
        value: numberOf(item.totalCount)
      })),
      'school.category_progress': categoryItems.map((item) => ({
        name: item.categoryName || item.groupName || '未分类',
        value: percent(numberOf(item.usedCount), numberOf(item.quotaLimit))
      }))
    },
    lists: {
      'school.recent_edited': toProjectListItems(dashboard.recentEdited, 'info'),
      'school.recent_submitted': toProjectListItems(dashboard.recentSubmitted, 'warning'),
      'school.recent_passed': toProjectListItems(dashboard.recentPassed, 'success'),
      'school.recent_results': toResultListItems(dashboard.recentResults)
    }
  };
};

const toProjectListItems = (rows: SchoolDashboardProjectVO[] | undefined, tone: DashboardListItem['tone']) =>
  (rows || []).map((row, index) => ({
    id: `project-${row.projectId || index}`,
    projectId: row.projectId,
    title: row.projectName || '未命名项目',
    subtitle: row.categoryName || '未分类',
    badge: statusText[row.status || ''] || '',
    time: row.time,
    tone
  }));

const toResultListItems = (rows: SchoolDashboardResultVO[] | undefined) =>
  (rows || []).map((row, index) => {
    const values = [
      row.awardLevel,
      row.rankNo ? `第 ${row.rankNo} 名` : '',
      row.averageScore !== undefined ? `平均分 ${row.averageScore}` : '',
      row.finalGrade
    ]
      .filter(Boolean)
      .join(' · ');
    return {
      id: `result-${row.projectId || index}`,
      projectId: row.projectId,
      title: row.projectName || '项目结果',
      subtitle: [row.categoryName, values].filter(Boolean).join(' · '),
      time: row.publishedAt,
      tone: 'success' as const
    };
  });

const statusCount = (summary: ProjectUploadSummaryVO, key: string) => numberOf((summary.statusItems || []).find((item) => item.key === key)?.count);

const loadAdminData = async (): Promise<UniversalDashboardData> => {
  const results = await Promise.allSettled([getUploadSummary({}), getUploadOverview({}), listSchoolInfo({ pageNum: 1, pageSize: 1 })]);
  const summary = responseData<ProjectUploadSummaryVO>(results[0]) || {};
  const overview = responseData<ProjectUploadOverviewVO>(results[1]) || {};
  const schoolTotal = responseTotal(results[2]);
  const submittedSchools = numberOf(summary.schoolCount);
  const projectTotal = numberOf(summary.totalCount);
  if (results.every((item) => item.status === 'rejected')) throw new Error('暂无权限读取看板数据');
  return {
    metrics: {
      'admin.school_total': schoolTotal,
      'admin.submitted_schools': submittedSchools,
      'admin.unsubmitted_schools': Math.max(0, schoolTotal - submittedSchools),
      'admin.project_total': projectTotal,
      'admin.pending_audit': statusCount(summary, 'submitted'),
      'admin.audit_passed': statusCount(summary, 'audit_passed'),
      'admin.returned': statusCount(summary, 'returned'),
      'admin.participation_rate': percent(submittedSchools, schoolTotal)
    },
    series: {
      'admin.status_distribution': (summary.statusItems || []).map((item) => ({
        name: item.label || item.key || '未分类',
        value: numberOf(item.count)
      })),
      'admin.category_distribution': (summary.categoryItems || []).map((item) => ({
        name: item.label || item.key || '未分类',
        value: numberOf(item.count)
      })),
      'admin.group_distribution': (summary.groupItems?.length ? summary.groupItems : overview.groupItems || []).map((item: any) => ({
        name: item.label || item.groupName || item.key || '未分类',
        value: numberOf(item.count ?? item.totalCount)
      })),
      'admin.school_ranking': (summary.schoolItems || []).map((item) => ({
        name: item.label || item.key || '未命名学校',
        value: numberOf(item.count)
      }))
    },
    lists: {}
  };
};

const loadAuditData = async (): Promise<UniversalDashboardData> => {
  const results = await Promise.allSettled([
    getAuditWorkbench(),
    listAuditProject({ pageNum: 1, pageSize: 1 }),
    listAuditProject({ pageNum: 1, pageSize: 1, status: 'submitted' }),
    listAuditProject({ pageNum: 1, pageSize: 1, status: 'audit_passed' }),
    listAuditProject({ pageNum: 1, pageSize: 1, status: 'returned' })
  ]);
  const workbench = responseData<AuditWorkbenchVO>(results[0]) || {};
  const assigned = responseTotal(results[1]);
  const pending = responseTotal(results[2]);
  const passed = responseTotal(results[3]);
  const returned = responseTotal(results[4]);
  const audited = passed + returned;
  if (results.every((item) => item.status === 'rejected')) throw new Error('暂无权限读取审核数据');
  return {
    metrics: {
      'audit.assigned_total': assigned,
      'audit.pending_count': pending,
      'audit.audited_count': audited,
      'audit.pass_count': passed,
      'audit.return_count': returned,
      'audit.pass_rate': percent(passed, audited),
      'audit.my_total': numberOf(workbench.myStats?.totalCount)
    },
    series: {
      'audit.status_distribution': [
        { name: '待审核', value: pending },
        { name: '审核通过', value: passed },
        { name: '审核退回', value: returned }
      ],
      'audit.group_distribution': (workbench.myGroupItems || []).map((item) => ({
        name: item.groupName || item.groupKey || '未分类',
        value: numberOf(item.totalCount)
      })),
      'audit.group_pass_rate': (workbench.myGroupItems || []).map((item) => ({
        name: item.groupName || item.groupKey || '未分类',
        value: percent(numberOf(item.passCount), numberOf(item.totalCount))
      }))
    },
    lists: {}
  };
};

const loadReviewData = async (): Promise<UniversalDashboardData> => {
  const { data: workbench = {} } = await getReviewWorkbench();
  const value = workbench as ReviewWorkbenchVO;
  const stats = value.stats || {};
  const assigned = numberOf(stats.assigned_total);
  const submitted = numberOf(stats.submitted_score);
  return {
    metrics: {
      'review.assigned_total': assigned,
      'review.pending_score': numberOf(stats.pending_score),
      'review.draft_score': numberOf(stats.draft_score),
      'review.submitted_score': submitted,
      'review.locked_by_other': numberOf(stats.locked_by_other),
      'review.completion_rate': percent(submitted, assigned)
    },
    series: {
      'review.status_distribution': [
        { name: '待评分', value: numberOf(stats.pending_score) },
        { name: '草稿', value: numberOf(stats.draft_score) },
        { name: '已提交', value: submitted },
        { name: '已锁定', value: numberOf(stats.locked_by_other) }
      ],
      'review.group_distribution': (value.groupItems || []).map((item) => ({
        name: item.groupName || item.groupKey || '未分类',
        value: numberOf(item.totalCount)
      })),
      'review.group_completion': (value.groupItems || []).map((item) => ({
        name: item.groupName || item.groupKey || '未分类',
        value: percent(numberOf(item.submittedCount), numberOf(item.totalCount))
      }))
    },
    lists: {}
  };
};

const loadViewerData = async (): Promise<UniversalDashboardData> => {
  const { data: overview = {} } = await getUploadOverview({});
  const value = overview as ProjectUploadOverviewVO;
  const summary = value.summary || {};
  return {
    metrics: {
      'viewer.project_total': numberOf(summary.totalCount),
      'viewer.draft_count': numberOf(summary.draftCount),
      'viewer.pending_count': numberOf(summary.submittedCount),
      'viewer.audited_count': numberOf(summary.auditedCount),
      'viewer.returned_count': numberOf(summary.returnedCount)
    },
    series: {
      'viewer.status_distribution': [
        { name: '未提交', value: numberOf(summary.draftCount) },
        { name: '待审核', value: numberOf(summary.submittedCount) },
        { name: '已审核', value: numberOf(summary.auditedCount) },
        { name: '已退回', value: numberOf(summary.returnedCount) }
      ],
      'viewer.category_distribution': (value.categoryItems || []).map((item) => ({
        name: item.categoryName || '未分类',
        value: numberOf(item.totalCount)
      })),
      'viewer.group_distribution': (value.groupItems || []).map((item) => ({ name: item.groupName || '未分类', value: numberOf(item.totalCount) }))
    },
    lists: {}
  };
};

const loadData = async () => {
  if (props.preview) {
    data.value = buildPreviewData();
    return;
  }
  loading.value = true;
  loadError.value = '';
  try {
    data.value =
      runtimeRole.value === 'school'
        ? await loadSchoolData()
        : runtimeRole.value === 'auditor'
          ? await loadAuditData()
          : runtimeRole.value === 'reviewer'
            ? await loadReviewData()
            : runtimeRole.value === 'viewer'
              ? await loadViewerData()
              : await loadAdminData();
  } catch (error: any) {
    data.value = { metrics: {}, series: {}, lists: {} };
    loadError.value = error?.message || '看板数据暂时不可用，请稍后刷新';
  } finally {
    loading.value = false;
  }
};

const runAction = async (item: DashboardBlockConfig) => {
  if (props.preview || !hasAction(item)) return;
  const action = item.action!;
  if (action.type === 'school_create') {
    if (!action.activityId || !action.categoryId) {
      ElMessage.warning('请先在工作台配置中选择填报活动和类别');
      return;
    }
    await router.push({
      path: '/crehn/project/edit',
      query: {
        activityId: String(action.activityId),
        categoryId: String(action.categoryId),
        categoryName: action.categoryName || undefined,
        lockCategory: action.lockCategory === false ? '0' : '1',
        returnPath: '/index',
        title: action.categoryName ? `${action.categoryName}填报` : '项目填报'
      }
    });
    return;
  }
  if (action.type === 'school_submit') {
    await router.push('/crehn/project/submit');
    return;
  }
  if (action.type === 'school_projects') {
    await router.push('/crehn/project');
    return;
  }
  if (action.type === 'audit_list') {
    await router.push({
      path: '/crehn/audit',
      query: {
        status: action.status || undefined,
        activityId: action.activityId ? String(action.activityId) : undefined,
        categoryNodeId: action.categoryId ? String(action.categoryId) : undefined
      }
    });
    return;
  }
  if (action.type === 'review_list') {
    await router.push({ path: '/crehn/review', query: { scoreStatus: action.scoreStatus || undefined } });
    return;
  }
  if (action.type === 'project_overview') {
    await router.push({
      path: '/crehn/project-view',
      query: {
        status: action.status || undefined,
        activityId: action.activityId ? String(action.activityId) : undefined
      }
    });
  }
};

const runHeaderAction = async () => {
  if (props.preview || !resolvedConfig.value.headerAction.visible) return;
  await router.push(resolvedConfig.value.headerAction.path);
};

const openProject = async (row: DashboardListItem) => {
  if (props.preview || !row.projectId) return;
  await router.push({
    path: `/crehn/project/edit/${row.projectId}`,
    query: { mode: 'view', returnPath: '/index' }
  });
};

watch(
  () => [props.preview, props.roleKey, props.configJson],
  () => {
    if (props.preview) data.value = buildPreviewData();
  },
  { immediate: true }
);

onMounted(loadData);
onActivated(loadData);
</script>

<style scoped lang="scss">
.universal-dashboard {
  overflow: hidden;
  color: var(--dashboard-text);
  padding: var(--dashboard-frame-padding);
  border: 1px solid var(--dashboard-frame-border);
  border-radius: var(--dashboard-radius);
  background: var(--dashboard-frame-bg);
  box-shadow: var(--dashboard-frame-shadow);
}

.universal-dashboard__head {
  margin-bottom: 16px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.universal-dashboard__head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.universal-dashboard__head span {
  color: var(--dashboard-accent);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.universal-dashboard__head h2 {
  margin: 5px 0 0;
  color: var(--dashboard-text);
  font-size: 20px;
}

.universal-dashboard__head p {
  margin: 7px 0 0;
  color: var(--dashboard-muted);
  font-size: 13px;
}

.universal-dashboard__alert {
  margin-bottom: 12px;
}

.universal-dashboard__grid {
  min-height: 120px;
  display: grid;
  grid-template-columns: repeat(var(--dashboard-columns), minmax(0, 1fr));
  gap: var(--dashboard-gap);
}

.universal-dashboard__block {
  position: relative;
  min-width: 0;
  padding: 16px;
  overflow: hidden;
  border: 1px solid var(--dashboard-border);
  border-radius: var(--dashboard-radius);
  background: var(--dashboard-surface);
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.universal-dashboard__block.is-clickable {
  cursor: pointer;
}

.universal-dashboard__block.is-clickable:hover,
.universal-dashboard__block.is-clickable:focus-visible {
  border-color: var(--dashboard-hover-border);
  box-shadow: var(--dashboard-hover-shadow);
  transform: var(--dashboard-hover-transform);
  outline: none;
}

.universal-dashboard__block.is-metric {
  display: flex;
  align-items: center;
}

.universal-dashboard__metric-accent {
  width: 4px;
  align-self: stretch;
  border-radius: 999px;
  background: var(--dashboard-block-color);
}

.universal-dashboard__metric-copy {
  min-width: 0;
  padding-left: 14px;
  display: grid;
  gap: 7px;
}

.universal-dashboard__metric-copy span {
  color: var(--dashboard-text);
  font-size: 14px;
  font-weight: 700;
}

.universal-dashboard__metric-copy strong {
  font-size: clamp(25px, 2.15vw, 34px);
  line-height: 1;
  letter-spacing: -0.02em;
}

.universal-dashboard__metric-copy small,
.universal-dashboard__block-title span,
.universal-dashboard__action-copy span {
  color: var(--dashboard-muted);
  font-size: 12px;
}

.universal-dashboard__corner {
  position: absolute;
  top: 12px;
  right: 12px;
  color: var(--dashboard-block-color);
}

.universal-dashboard__block.is-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
}

.universal-dashboard__block-title {
  width: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.universal-dashboard__block-title > div,
.universal-dashboard__action-copy {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.universal-dashboard__block-title strong,
.universal-dashboard__action-copy strong {
  color: var(--dashboard-text);
  font-size: 15px;
}

.universal-dashboard__block-title em {
  color: var(--dashboard-block-color);
  font-size: 19px;
  font-style: normal;
  font-weight: 800;
}

.universal-dashboard__block-title > .el-button {
  flex: 0 0 auto;
  margin-top: -4px;
}

.universal-dashboard__chart {
  height: calc(100% - 28px);
}

.universal-dashboard__block.is-ranking {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.universal-dashboard__ranking {
  min-height: 0;
  overflow: auto;
  display: grid;
  gap: 12px;
}

.universal-dashboard__ranking-row {
  display: grid;
  grid-template-columns: 26px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
}

.universal-dashboard__ranking-row > b {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  border-radius: 7px;
  color: var(--dashboard-muted);
  background: var(--dashboard-bg);
  font-size: 12px;
}

.universal-dashboard__ranking-row > b.is-top {
  color: #ffffff;
  background: var(--dashboard-accent);
}

.universal-dashboard__ranking-row > div {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.universal-dashboard__ranking-row span {
  overflow: hidden;
  color: var(--dashboard-text);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.universal-dashboard__ranking-row i {
  height: 5px;
  overflow: hidden;
  border-radius: 99px;
  background: var(--dashboard-border);
}

.universal-dashboard__ranking-row i em {
  height: 100%;
  display: block;
  border-radius: inherit;
}

.universal-dashboard__ranking-row > strong {
  color: var(--dashboard-text);
  font-size: 13px;
}

.universal-dashboard__block.is-list {
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.universal-dashboard__list {
  min-height: 0;
  overflow: auto;
  display: grid;
  gap: 10px;
}

.universal-dashboard__list-row {
  min-width: 0;
  display: grid;
  grid-template-columns: 8px minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
}

.universal-dashboard__list-row > i {
  width: 7px;
  height: 7px;
  border-radius: 99px;
  background: var(--dashboard-block-color);
}

.universal-dashboard__list-row > i.is-danger {
  background: #ef4444;
}

.universal-dashboard__list-row > i.is-warning {
  background: #f59e0b;
}

.universal-dashboard__list-row > i.is-success {
  background: #22c55e;
}

.universal-dashboard__list-row > div {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.universal-dashboard__list-row strong,
.universal-dashboard__list-row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.universal-dashboard__list-row strong {
  color: var(--dashboard-text);
  font-size: 13px;
}

.universal-dashboard__list-row strong.is-project-link {
  cursor: pointer;
  color: var(--dashboard-accent);
}

.universal-dashboard__list-row span,
.universal-dashboard__list-row time {
  color: var(--dashboard-muted);
  font-size: 12px;
}

.universal-dashboard__list-row em {
  padding: 2px 6px;
  border-radius: 5px;
  color: var(--dashboard-block-color);
  background: color-mix(in srgb, var(--dashboard-block-color) 10%, transparent);
  font-size: 11px;
  font-style: normal;
  white-space: nowrap;
}

.universal-dashboard__list-row time {
  grid-column: 2 / -1;
}

.universal-dashboard__block.is-action {
  display: flex;
  align-items: center;
  align-content: center;
  flex-wrap: wrap;
  gap: 14px;
}

.universal-dashboard__action-icon {
  width: 48px;
  height: 48px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 13px;
  background: color-mix(in srgb, currentColor 10%, transparent);
  font-size: 24px;
}

.universal-dashboard__action-copy {
  flex: 1;
}

.universal-dashboard__block.is-action .universal-dashboard__action-copy {
  min-width: 120px;
}

.universal-dashboard__block.is-action > .el-button {
  margin-left: auto;
}

@media (max-width: 1100px) {
  .universal-dashboard__block {
    grid-column: span 6 !important;
  }
}

@media (max-width: 720px) {
  .universal-dashboard__grid {
    grid-template-columns: 1fr;
  }

  .universal-dashboard__block {
    grid-column: 1 / -1 !important;
  }

  .universal-dashboard__head {
    align-items: center;
  }

  .universal-dashboard__block.is-action {
    flex-wrap: wrap;
  }
}
</style>
