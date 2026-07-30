<template>
  <div class="app-container art-home" :class="{ 'is-component-maximized': maximizedComponentKey }">
    <div v-if="noticeCssHtml" v-html="noticeCssHtml"></div>

    <section v-if="accountType !== 'school' && !hasStageNoticeLayout && !maximizedComponentKey" class="notice-board">
      <div class="notice-main">
        <div class="notice-heading">
          <span>NOTICE</span>
          <h1>公告</h1>
        </div>

        <el-skeleton v-if="loading" :rows="4" animated />

        <div v-else-if="notices.length" class="notice-list">
          <article v-for="notice in notices" :key="notice.id || notice.templateCode || notice.noticeTitle" class="notice-card">
            <div class="notice-card-head">
              <h2>{{ notice.noticeTitle }}</h2>
              <time v-if="notice.createTime">{{ formatDate(notice.createTime) }}</time>
            </div>
            <div class="notice-content" v-html="noticeHtml(notice.noticeContent)"></div>
          </article>
        </div>

        <el-empty v-else description="暂无公告" />
      </div>

      <aside class="calendar-card" aria-label="当前日期">
        <div class="calendar-month">{{ calendarInfo.monthText }}</div>
        <div class="calendar-day">{{ calendarInfo.day }}</div>
        <div class="calendar-week">{{ calendarInfo.weekday }}</div>
        <div class="calendar-date">{{ calendarInfo.fullDate }}</div>
      </aside>
    </section>

    <el-skeleton v-if="workbenchLoading" class="workbench-skeleton" :rows="6" animated />
    <section v-else-if="visibleLayouts.length" class="workbench-grid" :class="{ 'is-maximized': maximizedComponentKey }">
      <div
        v-for="item in visibleLayouts"
        :key="item.componentKey"
        class="workbench-cell"
        :class="[
          widthClass(item.width),
          {
            'is-maximized-target': maximizedComponentKey === item.componentKey,
            'is-maximized-hidden': maximizedComponentKey && maximizedComponentKey !== item.componentKey
          }
        ]"
      >
        <component
          :is="componentMap[item.componentKey]"
          v-if="componentMap[item.componentKey]"
          v-bind="componentProps(item)"
          v-on="componentEvents(item)"
        />
      </div>
    </section>
  </div>
</template>

<script setup name="Index" lang="ts">
import { getRegisterNotice } from '@/api/login';
import { listCurrentWorkbench } from '@/api/system/workbench';
import { WorkbenchLayoutVO } from '@/api/system/workbench/types';
import { useAppStore } from '@/store/modules/app';
import { useUserStore } from '@/store/modules/user';
import AdminCategoryWidget from '@/views/workbench/components/AdminCategoryWidget.vue';
import AdminStageNoticeWidget from '@/views/workbench/components/AdminStageNoticeWidget.vue';
import AdminSummaryWidget from '@/views/workbench/components/AdminSummaryWidget.vue';
import AuditOverviewWidget from '@/views/workbench/components/AuditOverviewWidget.vue';
import AuditPendingWidget from '@/views/workbench/components/AuditPendingWidget.vue';
import AuditStageNoticeWidget from '@/views/workbench/components/AuditStageNoticeWidget.vue';
import AuditWorkbenchWidget from '@/views/workbench/components/AuditWorkbenchWidget.vue';
import ProjectUploadOverviewWidget from '@/views/workbench/components/ProjectUploadOverviewWidget.vue';
import QuotaRatioWidget from '@/views/workbench/components/QuotaRatioWidget.vue';
import ReviewStageNoticeWidget from '@/views/workbench/components/ReviewStageNoticeWidget.vue';
import ReviewWorkbenchWidget from '@/views/workbench/components/ReviewWorkbenchWidget.vue';
import SchoolProjectSubmitWidget from '@/views/workbench/components/SchoolProjectSubmitWidget.vue';
import SchoolStageNoticeWidget from '@/views/workbench/components/SchoolStageNoticeWidget.vue';
import SchoolSubmissionWidget from '@/views/workbench/components/SchoolSubmissionWidget.vue';
import {
  WORKBENCH_SCHOOL_COMPONENT_KEYS,
  WORKBENCH_STAGE_NOTICE_COMPONENT_KEYS,
  workbenchWidthToken
} from '@/views/workbench/workbenchComponentRegistry';
import { defineAsyncComponent, type Component } from 'vue';

const appStore = useAppStore();
const userStore = useUserStore();
const loading = ref(false);
const workbenchLoading = ref(false);
const notices = ref<any[]>([]);
const layouts = ref<WorkbenchLayoutVO[]>([]);
const maximizedComponentKey = ref<string>();
const now = ref(new Date());
const UniversalDashboardWidget = defineAsyncComponent(() => import('@/views/workbench/components/UniversalDashboardWidget.vue'));

const componentMap: Record<string, Component> = {
  school_stage_notice: SchoolStageNoticeWidget,
  admin_stage_notice: AdminStageNoticeWidget,
  audit_stage_notice: AuditStageNoticeWidget,
  review_stage_notice: ReviewStageNoticeWidget,
  school_submission: SchoolSubmissionWidget,
  // 保留旧组件 key 继续承载学校数据看板，统一提交列表使用独立 key 并与独立页面复用同一组件。
  school_project_submit: UniversalDashboardWidget,
  school_project_list: SchoolProjectSubmitWidget,
  audit_overview: AuditOverviewWidget,
  audit_pending: AuditPendingWidget,
  audit_workbench: AuditWorkbenchWidget,
  review_workbench: ReviewWorkbenchWidget,
  admin_project_summary: AdminSummaryWidget,
  admin_upload_summary: AdminCategoryWidget,
  project_upload_overview: ProjectUploadOverviewWidget,
  quota_ratio_overview: QuotaRatioWidget,
  universal_dashboard: UniversalDashboardWidget
};
const accountType = computed(() => {
  const roles = userStore.roles.map((role) => String(role).toLowerCase());
  if (roles.some((role) => role.includes('expert'))) return 'expert';
  if (roles.some((role) => role.includes('school')) || userStore.schoolId) return 'school';
  if (roles.some((role) => role.includes('auditor'))) return 'auditor';
  if (roles.some((role) => role.includes('ops'))) return 'admin';
  if (roles.some((role) => role.includes('admin') || role.includes('super'))) return 'admin';
  return undefined;
});

const visibleLayouts = computed(() =>
  layouts.value.filter((item) => {
    if (item.visible === '1' || !componentMap[item.componentKey]) return false;
    if (WORKBENCH_SCHOOL_COMPONENT_KEYS.has(item.componentKey) && accountType.value !== 'school') return false;
    return true;
  })
);
const hasStageNoticeLayout = computed(() => visibleLayouts.value.some((item) => WORKBENCH_STAGE_NOTICE_COMPONENT_KEYS.has(item.componentKey)));

const componentTitle = (item: WorkbenchLayoutVO) =>
  item.componentKey === 'school_project_submit' && ['我的报送', '所有报送'].includes(String(item.title || '').trim()) ? '学校数据看板' : item.title;

const componentProps = (item: WorkbenchLayoutVO) => ({
  title: componentTitle(item),
  configJson: item.configJson,
  ...(item.componentKey === 'school_project_list'
    ? {
        maximized: maximizedComponentKey.value === item.componentKey,
        roleKey: item.roleKey
      }
    : {}),
  ...(item.componentKey === 'school_project_submit' || item.componentKey === 'universal_dashboard' ? { roleKey: item.roleKey } : {})
});

const componentEvents = (item: WorkbenchLayoutVO) =>
  item.componentKey === 'school_project_list'
    ? {
        'toggle-maximize': (value: boolean) => {
          if (value && appStore.sidebar.opened) {
            appStore.closeSideBar({ withoutAnimation: false });
          }
          maximizedComponentKey.value = value ? item.componentKey : undefined;
        }
      }
    : {};

const calendarInfo = computed(() => {
  const date = now.value;
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
  return {
    monthText: `${date.getFullYear()}年${date.getMonth() + 1}月`,
    day: String(date.getDate()).padStart(2, '0'),
    weekday: weekdays[date.getDay()],
    fullDate: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  };
});

const noticeCssHtml = computed(() => {
  const css = notices.value
    .map((item) => item.customCss || '')
    .join('\n')
    .replace(/<\/style/gi, '<\\/style');
  return css ? `<style>${css}</style>` : '';
});

const escapeHtml = (value: string) => {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
};

const noticeHtml = (value: string) => escapeHtml(value || '暂无公告内容').replace(/\n/g, '<br/>');

const formatDate = (value: string) => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value).slice(0, 10);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
};

const widthClass = (width?: string) => {
  const map: Record<string, string> = {
    '1/1': 'workbench-cell--full',
    '1/2': 'workbench-cell--half',
    '1/3': 'workbench-cell--third',
    '2/3': 'workbench-cell--two-third'
  };
  return map[workbenchWidthToken(width)];
};

const loadNotices = async () => {
  loading.value = true;
  try {
    const res = await getRegisterNotice({
      userType: accountType.value,
      schoolId: userStore.schoolId || undefined
    });
    notices.value = res.data || [];
  } finally {
    loading.value = false;
  }
};

const loadWorkbench = async () => {
  workbenchLoading.value = true;
  try {
    const res = await listCurrentWorkbench();
    layouts.value = (res.data || []).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
  } finally {
    workbenchLoading.value = false;
  }
};

onMounted(() => {
  loadNotices();
  loadWorkbench();
});
</script>

<style lang="scss">
.art-home {
  min-height: calc(100vh - 118px);
  color: #0f2437;
  background: var(--workbench-page-background, #f6f8fc);
}

.art-home.is-component-maximized {
  position: absolute;
  top: var(--layout-header-height, 92px);
  right: 0;
  bottom: 0;
  left: 0;
  height: auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
}

.notice-board {
  min-height: 260px;
  padding: 28px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 22px;
  align-items: stretch;
  border: 1px solid var(--workbench-card-border, #d8e6f5);
  border-radius: 8px;
  background: var(--workbench-card-bg, #ffffff);
  box-shadow: var(--workbench-card-shadow, none);
}

.notice-main {
  min-width: 0;
}

.calendar-card {
  padding: 22px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  border-radius: 8px;
  color: #ffffff;
  background: var(--workbench-calendar-bg, linear-gradient(180deg, #0f766e, #0a8edb));
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.12);
}

.calendar-month {
  font-size: 15px;
  font-weight: 800;
  opacity: 0.92;
}

.calendar-day {
  font-size: 58px;
  line-height: 1;
  font-weight: 950;
}

.calendar-week {
  font-size: 18px;
  font-weight: 900;
}

.calendar-date {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  font-size: 13px;
}

.notice-heading {
  position: relative;
  margin-bottom: 24px;
  padding-left: 18px;
  text-align: left;
}

.notice-heading::before {
  content: '';
  position: absolute;
  left: 0;
  top: 3px;
  width: 5px;
  height: calc(100% - 6px);
  border-radius: 3px;
  background: var(--workbench-accent, #2563eb);
}

.notice-heading span,
.workbench-card__head span {
  color: var(--workbench-accent, #005ea8);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.16em;
}

.notice-heading h1 {
  margin: 7px 0 0;
  color: var(--workbench-heading, #002f6c);
  font-size: 26px;
  line-height: 1.2;
  font-weight: 950;
}

.notice-list {
  display: grid;
  gap: 16px;
}

.notice-card {
  padding: 22px;
  border: 1px solid var(--workbench-card-border, #d8e6f5);
  border-radius: 8px;
  background: var(--workbench-card-bg, #ffffff);
  box-shadow: var(--workbench-card-shadow, 0 4px 14px rgba(37, 99, 235, 0.05));
}

.notice-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.notice-card h2 {
  margin: 0;
  color: #102a43;
  font-size: 20px;
  line-height: 1.35;
}

.notice-card time {
  flex: 0 0 auto;
  color: #829ab1;
  font-size: 13px;
  font-weight: 700;
}

.notice-content {
  color: #40566f;
  line-height: 1.8;
  font-size: 15px;
}

.workbench-skeleton,
.workbench-grid {
  margin-top: 16px;
}

.workbench-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.workbench-grid.is-maximized {
  flex: 1;
  min-height: 0;
  gap: 0;
  margin-top: 0;
  overflow: hidden;
}

.workbench-cell {
  min-width: 0;
}

.workbench-cell.is-maximized-hidden {
  display: none;
}

.workbench-cell.is-maximized-target {
  flex: 0 0 100%;
  height: 100%;
  min-height: 0;
}

.workbench-cell--full {
  flex: 0 0 100%;
}

.workbench-cell--half {
  flex: 0 0 calc(50% - 8px);
}

.workbench-cell--third {
  flex: 0 0 calc(33.333% - 11px);
}

.workbench-cell--two-third {
  flex: 0 0 calc(66.666% - 6px);
}

.workbench-card {
  height: 100%;
  padding: 20px;
  border: 1px solid var(--workbench-card-border, #d8e6f5);
  border-radius: 8px;
  background: var(--workbench-card-bg, #ffffff);
  box-shadow: var(--workbench-card-shadow, none);
}

.art-home .workbench-card .el-table {
  font-size: 14px;
  color: #25364a;
  --el-table-border-color: #d8e6f5;
  --el-table-header-bg-color: #eef4ff;
  --el-table-row-hover-bg-color: #f5f9ff;
}

.art-home .workbench-card .el-table th.el-table__cell {
  background: #eef4ff;
  color: #1f3b64;
  font-size: 14px;
  font-weight: 800;
}

.art-home .workbench-card .el-table td.el-table__cell {
  color: #25364a;
}

.art-home .workbench-card .el-table .el-table__cell {
  padding: 11px 0;
}

.art-home .workbench-card .el-table .cell {
  line-height: 24px;
}

.art-home .workbench-card .el-table .el-tag {
  height: 26px;
  padding: 0 8px;
  font-size: 13px;
  line-height: 24px;
}

.workbench-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
}

.workbench-card__head h2 {
  margin: 6px 0 0;
  color: var(--workbench-heading, #082f49);
  font-size: 20px;
  line-height: 1.25;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-item {
  min-height: 90px;
  padding: 16px;
  display: grid;
  align-content: center;
  gap: 8px;
  border: 1px solid var(--workbench-card-border, #d8e6f5);
  border-radius: 8px;
  background: #f8fbff;
}

.metric-item span {
  color: #606f7b;
  font-size: 13px;
}

.metric-item strong {
  color: var(--workbench-heading, #102a43);
  font-size: 28px;
  line-height: 1;
}

@media (max-width: 900px) {
  .notice-board {
    padding: 20px;
    grid-template-columns: 1fr;
  }

  .notice-card-head,
  .workbench-card__head {
    flex-direction: column;
  }

  .workbench-cell--half,
  .workbench-cell--third,
  .workbench-cell--two-third {
    flex-basis: 100%;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
