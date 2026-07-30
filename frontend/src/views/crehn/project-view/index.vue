<template>
  <div class="p-2 project-view-page">
    <div
      class="art-list-workspace art-list-workspace--with-navigator project-view-workspace"
      :class="{ 'is-top-layout': navigatorLayout === 'top' }"
      :style="{ '--navigator-sidebar-width': `${detailDisplayConfig.navigator.sidebarWidth}px` }"
    >
      <ProjectBrowseNavigator
        page-key="projectView"
        class="art-list-workspace__navigator"
        :force-sidebar="projectViewSidebarOpen"
        :force-top="!projectViewSidebarOpen"
        :activities="activityOptions"
        :activity-id="queryParams.activityId"
        :categories="categoryOptions"
        :category-count-map="categoryCountMap"
        :selected-category-key="String(selectedBrowseCategoryId || '')"
        :browse-mode="browseMode"
        :unit-options="projectViewUnitOptions"
        :selected-unit-id="queryParams.schoolId"
        compact-sidebar
        collapsible
        empty-category-text="请选择活动"
        @update:activity-id="handleNavigatorActivityChange"
        @update:browse-mode="handleNavigatorBrowseModeChange"
        @update:collapsed="projectViewSidebarOpen = !$event"
        @select-category="selectNavigatorCategory"
        @select-unit="selectNavigatorUnit"
      >
        <template #top-category="{ pageConfig }">
          <div v-if="pageConfig.fields.includes('category')" class="project-view-top-category-filter">
            <span>{{ pageConfig.labels.category }}</span>
            <el-select
              v-model="selectedBrowseCategoryId"
              clearable
              filterable
              class="project-view-category-filter"
              placeholder="全部类别"
              @change="handleCategoryFilterChange"
            >
              <el-option v-for="item in categoryFilterOptions" :key="item.id" :label="item.label" :value="item.id" />
            </el-select>
          </div>
        </template>
      </ProjectBrowseNavigator>

      <el-card shadow="hover" class="art-list-card art-list-workspace__main project-view-list-card">
        <ArtProjectWorkspaceHeader page-key="projectView" :title-variables="{ activityName: selectedActivityName }">
          <template #navigatorToggle>
            <ArtWorkspaceConfigButton
              v-if="projectViewHeaderConfig.sidebarEnabled"
              :icon="projectViewSidebarOpen ? 'Fold' : 'Expand'"
              :text="
                projectViewSidebarOpen
                  ? projectViewHeaderConfig.navigatorToggleButton.alternateText
                  : projectViewHeaderConfig.navigatorToggleButton.text
              "
              :tooltip="
                projectViewSidebarOpen
                  ? projectViewHeaderConfig.navigatorToggleButton.alternateTooltip
                  : projectViewHeaderConfig.navigatorToggleButton.tooltip
              "
              @click="projectViewSidebarOpen = !projectViewSidebarOpen"
            />
          </template>
          <template #filters>
            <div class="project-view-header-context">
              <span class="art-list-toolbar__context" :title="currentPathText">{{ currentPathText }}</span>
            </div>
          </template>
          <template #status>
            <ArtStatusTabs
              :items="statusOptions"
              :model-value="queryParams.status"
              :collapse-on-overflow="projectViewHeaderConfig.statusCollapseOnOverflow"
              aria-label="项目状态"
              @change="handleStatusChange(String($event ?? ''))"
            />
          </template>
          <template #search>
            <div class="project-view-header-search art-list-header-search">
              <el-input
                v-model="queryParams.projectName"
                class="art-list-toolbar__search"
                clearable
                :placeholder="projectViewHeaderText('search', 'placeholder', '请输入项目名称')"
                @keyup.enter="refreshProjectView"
                @clear="refreshProjectView"
              />
              <el-button type="primary" icon="Search" @click="refreshProjectView">{{ projectViewHeaderText('search', 'search', '搜索') }}</el-button>
              <el-button icon="Refresh" @click="resetQuery">{{ projectViewHeaderText('search', 'reset', '重置') }}</el-button>
            </div>
          </template>
          <template #columnWidthReset>
            <ArtWorkspaceConfigButton
              icon="RefreshLeft"
              :text="projectViewHeaderConfig.columnWidthResetButton.text"
              :tooltip="projectViewHeaderConfig.columnWidthResetButton.tooltip"
              @click="restoreProjectViewColumnWidths"
            />
          </template>
        </ArtProjectWorkspaceHeader>
        <div ref="projectViewTableShellRef" class="art-resizable-table-shell art-resizable-table-shell--fit">
          <el-table
            v-loading="loading"
            border
            :data="projectList"
            :empty-text="projectViewTablePage.emptyText"
            class="art-list-table art-resizable-table art-resizable-table--borderless art-viewable-table"
            :class="tableAppearanceClass"
            :style="tableAppearanceStyle"
            @header-dragend="handleProjectViewColumnResize"
            @row-click="openDetail"
          >
            <el-table-column
              v-for="column in projectViewTableColumns"
              :key="column.key"
              :column-key="column.key"
              :label="column.label"
              :prop="column.source === 'builtin' && !['actions', 'serial'].includes(column.key) ? column.key : undefined"
              :width="projectViewColumnWidth(column.key)"
              :min-width="column.minWidth"
              :resizable="column.resizable"
              :fixed="column.fixed"
              :align="['serial', 'actions'].includes(column.key) ? 'center' : undefined"
              :class-name="
                column.key === 'actions' || ['serial', 'status'].includes(column.key) ? 'art-table-nowrap-cell' : 'art-table-configurable-wrap-cell'
              "
              :label-class-name="column.key === 'actions' ? 'art-fixed-action-header' : 'art-resizable-header'"
            >
              <template #default="scope">
                <span v-if="column.key === 'serial'">{{ projectViewSerialNumber(scope.$index) }}</span>
                <ArtListStatusTag
                  v-else-if="column.key === 'status'"
                  :semantic="projectViewStatusSemantic(scope.row.status)"
                  :label="projectViewStatusText(projectViewStatusSemantic(scope.row.status), statusLabel(scope.row.status))"
                  :tag-type="statusType(scope.row.status)"
                />
                <div v-else-if="column.key === 'actions'" class="art-list-row-actions">
                  <el-button
                    v-hasPermi="['crehn:projectView:query']"
                    class="art-list-row-action art-list-row-action--view"
                    link
                    type="primary"
                    :icon="actionIcon('View')"
                    @click.stop="openDetail(scope.row)"
                    >{{ projectViewActionText('view', '查看') }}</el-button
                  >
                  <el-button
                    v-hasPermi="['crehn:projectView:remove']"
                    class="art-list-row-action art-list-row-action--delete"
                    link
                    type="danger"
                    :icon="actionIcon('Delete')"
                    @click.stop="recycleRow(scope.row)"
                    >{{ projectViewActionText('delete', '删除') }}</el-button
                  >
                </div>
                <span v-else>{{ projectViewColumnText(scope.row, column) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
      </el-card>
    </div>

    <ArtPopupDrawer v-model="detailVisible" title="上报详情" size="80%" wide>
      <template v-if="currentProject">
        <el-descriptions border :column="2">
          <el-descriptions-item label="项目编号">{{ currentProject.projectNo }}</el-descriptions-item>
          <el-descriptions-item label="项目名称">{{ currentProject.projectName }}</el-descriptions-item>
          <el-descriptions-item label="活动">{{ currentProject.activityName || currentProject.activityId }}</el-descriptions-item>
          <el-descriptions-item label="类别">{{ currentProject.categoryName || currentProject.categoryId }}</el-descriptions-item>
          <el-descriptions-item label="单位">{{ currentProject.schoolName || currentProject.schoolId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(currentProject.status)">{{ statusLabel(currentProject.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ currentProject.submittedAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核意见" :span="2">{{ currentProject.currentAuditOpinion || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="section-head">
          <el-divider content-position="left">表单内容</el-divider>
          <el-button size="small" icon="Download" @click="downloadDetailWorkbook">下载表格</el-button>
        </div>
        <el-table border :data="formRows">
          <el-table-column label="字段" prop="label" width="180" />
          <el-table-column label="内容" prop="value" min-width="260" show-overflow-tooltip />
        </el-table>

        <ProjectGenericTableReadonly :project="currentProject" />

        <el-divider content-position="left">成员信息</el-divider>
        <el-table border :data="currentProject.members || []">
          <el-table-column label="类型" width="110">
            <template #default="scope">{{ memberTypeLabel(scope.row.memberType) }}</template>
          </el-table-column>
          <el-table-column v-for="field in memberFields" :key="field.fieldKey" :label="memberFieldLabel(field)" min-width="140" show-overflow-tooltip>
            <template #default="scope">{{ formatProjectMemberValue(scope.row, field) }}</template>
          </el-table-column>
        </el-table>

        <el-divider content-position="left">附件</el-divider>
        <el-table border :data="currentProject.files || []">
          <el-table-column label="材料类型" prop="fileTypeCode" width="120" />
          <el-table-column label="文件名" prop="originalName" min-width="180" show-overflow-tooltip />
          <el-table-column label="大小" width="120">
            <template #default="scope">{{ formatSize(scope.row.fileSize) }}</template>
          </el-table-column>
          <el-table-column label="技术校验" min-width="190">
            <template #default="scope">
              <el-tag :type="checkTagType(scope.row.checkStatus)">{{ checkStatusLabel(scope.row.checkStatus) }}</el-tag>
              <div class="text-xs text-gray-500">{{ normalizeReviewMessage(scope.row.checkMessage) || '-' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="下载/预览" min-width="220">
            <template #default="scope">
              <el-button v-if="canInlinePreview(scope.row)" link type="primary" icon="View" @click="openPreview(scope.row)">预览</el-button>
              <el-button v-if="scope.row.storagePath" link type="primary" icon="Download" @click="openFile(scope.row)">下载原文件</el-button>
              <span v-if="!scope.row.storagePath && !scope.row.previewPath">-</span>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </ArtPopupDrawer>

    <ArtPopupDialog v-model="previewDialog.visible" :title="previewDialog.title" width="80%" wide top="4vh" append-to-body destroy-on-close>
      <video v-if="previewDialog.type === 'video'" class="w-full max-h-[72vh]" :src="previewDialog.url" controls preload="metadata" />
      <iframe v-else-if="previewDialog.type === 'pdf'" class="h-[72vh] w-full border-0" :src="pdfViewerUrl(previewDialog.url)" />
      <div v-else-if="previewDialog.type === 'image'" class="max-h-[72vh] overflow-auto text-center">
        <img :src="previewDialog.url" class="max-w-full" />
      </div>
      <el-alert v-else type="info" :closable="false" title="该文件类型不支持浏览器内预览，请下载后查看。" />
      <template #footer>
        <el-button v-if="previewDialog.url" type="primary" icon="Download" @click="windowOpen(previewDialog.url)">下载/新窗口打开</el-button>
        <el-button @click="previewDialog.visible = false">关闭</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ArtProjectView" lang="ts">
import {
  getProjectView,
  listProjectView,
  listProjectViewActivityOptions,
  listProjectViewCategoryOptions,
  listProjectViewSchoolOptions,
  listProjectViewStats,
  recycleProjectView
} from '@/api/crehn/projectView';
import { ActivityCategoryVO, ActivityVO, ProjectFileVO, ProjectVO } from '@/api/crehn/types';
import type { ArtListStatusSemantic, ArtReviewListColumnConfig } from '@/api/crehn/detailDisplay';
import { useArtTableColumnWidths } from '@/composables/useArtTableColumnWidths';
import { useArtListTableAppearance } from '@/composables/useArtListTableAppearance';
import { artTableColumnValue, artTableDisplayText, useArtListTablePage } from '@/composables/useArtListTableConfig';
import { checkPermi } from '@/utils/permission';
import { normalizeReviewMessage } from '@/utils/artReviewMessage';
import { loadArtDetailDisplayConfig, useArtDetailDisplayConfig, workspaceHeaderItemText } from '../components/artDetailDisplayConfig';
import ArtProjectWorkspaceHeader from '../components/ArtProjectWorkspaceHeader.vue';
import ArtWorkspaceConfigButton from '../components/ArtWorkspaceConfigButton.vue';
import ArtStatusTabs from '../components/ArtStatusTabs.vue';
import ArtListStatusTag from '../components/ArtListStatusTag.vue';
import ProjectBrowseNavigator from '../components/ProjectBrowseNavigator.vue';
import ProjectGenericTableReadonly from '../project/components/ProjectGenericTableReadonly.vue';
import {
  buildProjectFormRows,
  buildProjectMemberFields,
  exportProjectDetailWorkbook,
  formatProjectMemberValue,
  memberFieldLabel,
  memberTypeLabel
} from '@/utils/artProjectDetail';
import { useRoute } from 'vue-router';

interface CategoryNode extends ActivityCategoryVO {
  children?: CategoryNode[];
  count?: number;
}

interface CategoryFilterOption {
  id: string | number;
  label: string;
}

interface CountItem {
  id: string | number;
  totalCount?: number;
}

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const route = useRoute();

const loading = ref(false);
const total = ref(0);
const browseMode = ref<'category' | 'school'>('category');
const projectViewSidebarOpen = ref(false);
const activityOptions = ref<ActivityVO[]>([]);
const categoryOptions = ref<ActivityCategoryVO[]>([]);
const schoolOptions = ref<any[]>([]);
const schoolKeyword = ref('');
const categoryCountMap = ref<Record<string, number>>({});
const schoolCountMap = ref<Record<string, number>>({});
const projectList = ref<ProjectVO[]>([]);
const currentProject = ref<ProjectVO>();
const selectedBrowseCategoryId = ref<string | number>();
const detailVisible = ref(false);
const previewDialog = reactive<{ visible: boolean; title: string; url: string; type: string }>({ visible: false, title: '', url: '', type: '' });
const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  activityId: undefined,
  categoryId: undefined,
  categoryIds: undefined,
  schoolId: undefined,
  projectName: '',
  status: undefined
});
const {
  pageConfig: projectViewTablePage,
  columns: projectViewDataColumns,
  serialColumn: projectViewSerialColumn,
  statusText: projectViewStatusText,
  actionText: projectViewActionText
} = useArtListTablePage('projectView', () => queryParams.categoryId);
const projectViewTableColumns = computed(() => [
  ...(projectViewSerialColumn.value ? [projectViewSerialColumn.value] : []),
  ...projectViewDataColumns.value
]);
const projectViewSerialNumber = (index: number) => (queryParams.pageNum - 1) * queryParams.pageSize + index + 1;
const projectViewWidthStorageKey = computed(
  () => `crehn:project-view-table-widths:v2:${queryParams.activityId || 'all'}:${queryParams.categoryId || 'all'}`
);
const {
  tableShellRef: projectViewTableShellRef,
  columnWidth: projectViewColumnWidth,
  handleColumnResize: handleProjectViewColumnResize,
  restoreColumnWidths: restoreProjectViewColumnWidths
} = useArtTableColumnWidths({ columns: projectViewTableColumns, storageKey: projectViewWidthStorageKey, fitContainer: true });
const projectViewColumnText = (row: ProjectVO, column: ArtReviewListColumnConfig) =>
  artTableDisplayText(artTableColumnValue(row as unknown as Record<string, unknown>, column));

const routeQueryText = (value: unknown) => (Array.isArray(value) ? String(value[0] || '') : String(value || ''));

const categoryTree = computed<CategoryNode[]>(() => buildCategoryTree(categoryOptions.value));
const categoryFilterOptions = computed<CategoryFilterOption[]>(() => flattenCategoryTree(categoryTree.value));
const visibleSchoolOptions = computed(() => {
  const keyword = schoolKeyword.value.trim().toLowerCase();
  return schoolOptions.value
    .map((item) => ({ ...item, projectCount: schoolCount(item.id) }))
    .filter((item) => item.projectCount > 0 || String(queryParams.schoolId || '') === String(item.id))
    .filter((item) => {
      if (!keyword) return true;
      return [item.schoolName, item.schoolCode].filter(Boolean).some((value) => String(value).toLowerCase().includes(keyword));
    });
});
const projectViewUnitOptions = computed(() =>
  visibleSchoolOptions.value.map((item) => ({
    id: item.id,
    label: String(item.schoolName || item.id),
    count: Number(item.projectCount || 0),
    keywords: [item.schoolCode].filter(Boolean) as string[]
  }))
);
const { detailDisplayConfig } = useArtDetailDisplayConfig();
const { tableAppearanceClass, tableAppearanceStyle, actionIcon } = useArtListTableAppearance({
  statusLabels: () =>
    ['draft', 'pending', 'approved', 'returned'].map((key) => projectViewTablePage.value.statusLabels[key as ArtListStatusSemantic]),
  actionLabels: () => ['view', 'delete'].map((key) => projectViewTablePage.value.actionLabels[key])
});
const projectViewHeaderConfig = computed(() => detailDisplayConfig.value.workspaceHeader.pages.projectView);
const projectViewHeaderText = (itemKey: 'filters' | 'status' | 'search', textKey: string, fallback: string) =>
  workspaceHeaderItemText(projectViewHeaderConfig.value, itemKey, textKey, fallback);
const navigatorLayout = computed(() => (projectViewSidebarOpen.value ? 'sidebar' : 'top'));
const selectedActivityName = computed(
  () => activityOptions.value.find((item) => String(item.id) === String(queryParams.activityId))?.activityName || '未选择活动'
);
const statusOptions = computed(() => {
  const options: Array<{ label: string; value: string }> = [];
  if (checkPermi(['crehn:projectView:draft'])) {
    options.push({ label: projectViewHeaderText('status', 'draft', '草稿'), value: 'draft' });
  }
  if (checkPermi(['crehn:projectView:submitted'])) {
    options.push({ label: projectViewHeaderText('status', 'school_submitted', '待审核'), value: 'school_submitted' });
  }
  if (checkPermi(['crehn:projectView:audited'])) {
    options.push(
      { label: projectViewHeaderText('status', 'audit_passed', '已通过'), value: 'audit_passed' },
      { label: projectViewHeaderText('status', 'returned', '已退回'), value: 'returned' }
    );
  }
  return options.length ? [{ label: projectViewHeaderText('status', 'all', '全部'), value: '' }, ...options] : options;
});
const selectedCategory = computed(() => categoryOptions.value.find((item) => String(item.id) === String(selectedBrowseCategoryId.value)));
const selectedSchool = computed(() => schoolOptions.value.find((item) => String(item.id) === String(queryParams.schoolId)));
const currentPathText = computed(() => {
  const categoryPath = selectedBrowseCategoryId.value
    ? categoryPathText(selectedBrowseCategoryId.value)
    : projectViewHeaderText('filters', 'allCategories', '全部类别');
  const allUnits = projectViewHeaderText('filters', 'allUnits', '全部单位');
  const school = selectedSchool.value ? schoolLabel(selectedSchool.value) : allUnits;
  const suffix = projectViewHeaderText('filters', 'progressSuffix', '上报进度');
  return browseMode.value === 'category' ? `${categoryPath} > ${allUnits} > ${suffix}` : `${school} > ${categoryPath} > ${suffix}`;
});

const formRows = computed(() => buildProjectFormRows(currentProject.value));
const memberFields = computed(() => buildProjectMemberFields(currentProject.value));

const loadActivityOptions = async () => {
  const { data } = await listProjectViewActivityOptions();
  activityOptions.value = data || [];
  if (!queryParams.activityId && activityOptions.value.length === 1) {
    queryParams.activityId = activityOptions.value[0].id;
    await loadCategoryOptions();
  }
};

const loadCategoryOptions = async () => {
  categoryOptions.value = [];
  selectedBrowseCategoryId.value = undefined;
  queryParams.categoryId = undefined;
  queryParams.categoryIds = undefined;
  if (!queryParams.activityId) return;
  const { data } = await listProjectViewCategoryOptions(queryParams.activityId);
  categoryOptions.value = data || [];
  applyDefaultCategorySelection();
};

const loadSchoolOptions = async () => {
  const res: any = await listProjectViewSchoolOptions({});
  schoolOptions.value = res.rows || res.data || [];
};

const loadProjectViewStats = async () => {
  ensureAllowedStatus(false);
  if (!queryParams.activityId) {
    categoryCountMap.value = {};
    schoolCountMap.value = {};
    return;
  }
  const { data } = await listProjectViewStats({
    activityId: queryParams.activityId,
    status: queryParams.status,
    projectName: queryParams.projectName
  });
  categoryCountMap.value = toCountMap(data?.categoryCounts || []);
  schoolCountMap.value = toCountMap(data?.schoolCounts || []);
};

const toCountMap = (rows: CountItem[]) =>
  rows.reduce<Record<string, number>>((map, item) => {
    if (item.id !== undefined && item.id !== null) {
      map[String(item.id)] = Number(item.totalCount || 0);
    }
    return map;
  }, {});

const refreshProjectView = async () => {
  queryParams.pageNum = 1;
  await loadProjectViewStats();
  await getList();
};

const handleStatusChange = async (status?: string) => {
  queryParams.status = status || '';
  await refreshProjectView();
};

const handleActivityChange = async () => {
  await loadCategoryOptions();
  queryParams.pageNum = 1;
  await refreshProjectView();
};

const handleNavigatorActivityChange = async (activityId: string | number) => {
  queryParams.activityId = activityId;
  await handleActivityChange();
};
const handleNavigatorBrowseModeChange = (mode: 'category' | 'school') => {
  browseMode.value = mode;
  handleBrowseModeChange();
};
const selectNavigatorCategory = (selection: { key: string; categoryIds: Array<string | number> }) => {
  selectedBrowseCategoryId.value = selection.key || undefined;
  queryParams.categoryId = undefined;
  queryParams.categoryIds = selection.categoryIds.join(',') || undefined;
  queryParams.schoolId = undefined;
  queryParams.pageNum = 1;
  getList();
};
const selectNavigatorUnit = (schoolId?: string | number) => {
  if (schoolId === undefined || schoolId === null) {
    queryParams.schoolId = undefined;
    queryParams.pageNum = 1;
    getList();
    return;
  }
  const school = schoolOptions.value.find((item) => String(item.id) === String(schoolId));
  if (school) selectSchool(school);
};

const handleBrowseModeChange = () => {
  selectedBrowseCategoryId.value = undefined;
  queryParams.categoryId = undefined;
  queryParams.categoryIds = undefined;
  queryParams.schoolId = undefined;
  queryParams.pageNum = 1;
  getList();
};

const selectSchool = (school: any) => {
  queryParams.schoolId = school.id;
  queryParams.pageNum = 1;
  getList();
};

const handleCategoryFilterChange = (categoryId?: string | number) => {
  const node = categoryId ? findCategoryNode(categoryTree.value, categoryId) : undefined;
  const categoryIds = node ? collectLeafCategoryIds(node) : [];
  queryParams.categoryId = undefined;
  queryParams.categoryIds = categoryIds.length ? categoryIds.join(',') : undefined;
  queryParams.pageNum = 1;
  getList();
};

const getList = async () => {
  ensureAllowedStatus(false);
  loading.value = true;
  try {
    const res: any = await listProjectView(queryParams);
    projectList.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const resetQuery = async () => {
  queryParams.activityId = activityOptions.value.length === 1 ? activityOptions.value[0].id : undefined;
  queryParams.categoryId = undefined;
  queryParams.categoryIds = undefined;
  selectedBrowseCategoryId.value = undefined;
  queryParams.schoolId = undefined;
  queryParams.projectName = '';
  queryParams.status = defaultStatus();
  queryParams.pageNum = 1;
  if (queryParams.activityId) {
    await loadCategoryOptions();
  } else {
    categoryOptions.value = [];
  }
  await refreshProjectView();
};

const applyRouteQuery = () => {
  const activityId = routeQueryText(route.query.activityId);
  const schoolId = routeQueryText(route.query.schoolId);
  const status = routeQueryText(route.query.status);
  const projectName = routeQueryText(route.query.projectName);
  const mode = routeQueryText(route.query.mode || route.query.browse);
  if (activityId) {
    queryParams.activityId = activityId;
  }
  if (schoolId) {
    browseMode.value = 'school';
    queryParams.schoolId = schoolId;
  } else if (mode === 'school') {
    browseMode.value = 'school';
  }
  if (status) {
    queryParams.status = status;
  }
  if (projectName) {
    queryParams.projectName = projectName;
  }
};

const openDetail = async (row: ProjectVO) => {
  const { data } = await getProjectView(row.id!);
  currentProject.value = data;
  detailVisible.value = true;
};

const recycleRow = async (row: ProjectVO) => {
  await proxy?.$modal.confirm(`确认删除上报“${row.projectName || row.projectNo || row.id}”？删除后将进入回收站。`);
  await recycleProjectView(row.id!);
  proxy?.$modal.msgSuccess('已移入回收站');
  await loadProjectViewStats();
  await getList();
};

const menuGroups = [
  { key: 'performance', label: '艺术表演类' },
  { key: 'artwork', label: '艺术作品类' },
  { key: 'workshop', label: '艺术实践工作坊' },
  { key: 'achievement', label: '高校美育改革创新优秀成果' },
  { key: 'principal', label: '高校校长书画作品' }
];

const categoryCount = (categoryId?: string | number) =>
  categoryId === undefined || categoryId === null ? 0 : categoryCountMap.value[String(categoryId)] || 0;
const schoolCount = (schoolId?: string | number) => (schoolId === undefined || schoolId === null ? 0 : schoolCountMap.value[String(schoolId)] || 0);
const sumNodeCounts = (nodes: CategoryNode[]) => nodes.reduce((sum, node) => sum + (node.count || 0), 0);

const buildCategoryTree = (rows: ActivityCategoryVO[]) => {
  const grouped = new Map<string, CategoryNode[]>();
  rows.forEach((item) => {
    const key = categoryGroupKey(item);
    const items = grouped.get(key) || [];
    items.push({ ...item, count: categoryCount(item.id), children: [] });
    grouped.set(key, items);
  });
  const roots = menuGroups
    .map((group) => {
      const children = grouped.get(group.key) || [];
      return {
        id: `group:${group.key}`,
        categoryName: group.label,
        count: sumNodeCounts(children),
        children
      };
    })
    .filter((group) => group.children.length);
  const other = grouped.get('other') || [];
  if (other.length) {
    roots.push({ id: 'group:other', categoryName: '其他类别', count: sumNodeCounts(other), children: other });
  }
  return roots;
};

const categoryGroupKey = (item: ActivityCategoryVO) => {
  const code = String(item.categoryCode || '').toLowerCase();
  const group = String(item.categoryGroup || '').toLowerCase();
  if (code === 'artwork_principal' || group === 'principal') return 'principal';
  if (code.startsWith('performance_') || group === 'performance') return 'performance';
  if (code.startsWith('artwork_') || group === 'artwork') return 'artwork';
  if (code === 'workshop' || code.startsWith('workshop_') || group === 'workshop') return 'workshop';
  if (code.startsWith('achievement_') || group === 'achievement') return 'achievement';
  return 'other';
};

const collectLeafCategoryIds = (node: CategoryNode): Array<string | number> => {
  if (!node.children?.length) return node.id ? [node.id] : [];
  return node.children.flatMap(collectLeafCategoryIds);
};

const firstLeafCategoryNode = (nodes: CategoryNode[]): CategoryNode | undefined => {
  for (const node of nodes) {
    const child = firstLeafCategoryNode(node.children || []);
    if (child) return child;
    if (node.id && !String(node.id).startsWith('group:')) return node;
  }
  return undefined;
};

const applyDefaultCategorySelection = () => {
  if (selectedBrowseCategoryId.value || queryParams.categoryIds) return;
  const node = firstLeafCategoryNode(categoryTree.value);
  if (!node) return;
  selectedBrowseCategoryId.value = node.id;
  const categoryIds = collectLeafCategoryIds(node);
  queryParams.categoryId = undefined;
  queryParams.categoryIds = categoryIds.length ? categoryIds.join(',') : undefined;
};

const findCategoryNode = (nodes: CategoryNode[], categoryId: string | number): CategoryNode | undefined => {
  for (const node of nodes) {
    if (String(node.id) === String(categoryId)) return node;
    const matched = findCategoryNode(node.children || [], categoryId);
    if (matched) return matched;
  }
  return undefined;
};

const categoryPathText = (categoryId: string | number) => categoryNodePath(categoryTree.value, categoryId).join(' > ') || '全部类别';

const categoryNodePath = (nodes: CategoryNode[], categoryId: string | number, parents: string[] = []): string[] => {
  for (const node of nodes) {
    const name = node.categoryName || String(node.id);
    const path = [...parents, name];
    if (String(node.id) === String(categoryId)) return path;
    const matched = categoryNodePath(node.children || [], categoryId, path);
    if (matched.length) return matched;
  }
  return [];
};

const flattenCategoryTree = (nodes: CategoryNode[], level = 0): CategoryFilterOption[] =>
  nodes.flatMap((node) => {
    const label = `${'  '.repeat(level)}${node.categoryName || node.id}（${node.count || 0}）`;
    return [{ id: node.id!, label }, ...flattenCategoryTree(node.children || [], level + 1)];
  });

const schoolLabel = (item: any) => [item.schoolName, item.schoolCode].filter(Boolean).join(' / ');

const defaultStatus = () => {
  return '';
};

const ensureAllowedStatus = (defaultWhenBlank = true) => {
  if (!statusOptions.value.length) {
    queryParams.status = undefined;
    return;
  }
  if (!queryParams.status) {
    if (defaultWhenBlank) {
      queryParams.status = defaultStatus();
    }
    return;
  }
  if (!statusOptions.value.some((item) => item.value === queryParams.status)) {
    queryParams.status = defaultStatus();
  }
};

const downloadDetailWorkbook = () => {
  if (!currentProject.value) return;
  exportProjectDetailWorkbook(currentProject.value, formRows.value, memberFields.value);
};

const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`;
  return `${(size / 1024 / 1024 / 1024).toFixed(2)} GB`;
};

const checkStatusLabel = (status?: string) => ({ passed: '通过', warning: '需人工关注', failed: '不通过' })[status || ''] || status || '未校验';
const checkTagType = (status?: string) => {
  if (status === 'passed') return 'success';
  if (status === 'warning') return 'warning';
  if (status === 'failed') return 'danger';
  return 'info';
};

const fileExt = (file: ProjectFileVO) => String(file.fileExt || file.originalName?.split('.').pop() || '').toLowerCase();
const officeExts = ['doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx'];
const previewType = (file: ProjectFileVO) => {
  const ext = fileExt(file);
  if (officeExts.includes(ext) && file.previewPath) return 'pdf';
  if (file.mediaType === 'video' || ['mp4', 'mov', 'webm', 'ogg'].includes(ext)) return 'video';
  if (file.mediaType === 'image' || ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext)) return 'image';
  if (ext === 'pdf') return 'pdf';
  return '';
};
const previewUrl = (file: ProjectFileVO) => (officeExts.includes(fileExt(file)) ? file.previewPath : file.storagePath);
const canInlinePreview = (file: ProjectFileVO) => !!previewUrl(file) && !!previewType(file);

const refreshCurrentProjectFile = async (file: ProjectFileVO) => {
  if (!currentProject.value?.id || !file?.id) return file;
  try {
    const { data } = await getProjectView(currentProject.value.id);
    currentProject.value = data;
    return data.files?.find((item) => String(item.id) === String(file.id)) || file;
  } catch {
    return file;
  }
};

const openPreview = async (file: ProjectFileVO) => {
  const freshFile = await refreshCurrentProjectFile(file);
  const url = previewUrl(freshFile);
  if (!url) return;
  previewDialog.title = freshFile.originalName || '文件预览';
  previewDialog.url = url;
  previewDialog.type = previewType(freshFile);
  previewDialog.visible = true;
};

const openFile = async (file: ProjectFileVO) => {
  const freshFile = await refreshCurrentProjectFile(file);
  if (freshFile.storagePath) windowOpen(freshFile.storagePath);
};

const windowOpen = (url: string) => {
  window.open(url, '_blank', 'noopener,noreferrer');
};

const pdfViewerUrl = (url?: string) => {
  if (!url) return '';
  return `${url.split('#')[0]}#toolbar=0&navpanes=0&scrollbar=1`;
};

const statusLabels: Record<string, string> = { draft: '草稿', submitted: '待审核', returned: '已退回', audit_passed: '已通过' };
type ElTagType = 'primary' | 'success' | 'warning' | 'info' | 'danger';
const statusTypes: Record<string, ElTagType> = { draft: 'info', submitted: 'warning', returned: 'danger', audit_passed: 'success' };
const projectViewStatusSemantics: Record<string, ArtListStatusSemantic> = {
  draft: 'draft',
  submitted: 'pending',
  returned: 'returned',
  audit_passed: 'approved'
};
const statusLabel = (status?: string) => statusLabels[status || ''] || status || '-';
const statusType = (status?: string): ElTagType => statusTypes[status || ''] || 'info';
const projectViewStatusSemantic = (status?: string): ArtListStatusSemantic => projectViewStatusSemantics[status || ''] || 'draft';

onMounted(async () => {
  try {
    await loadArtDetailDisplayConfig(true);
    projectViewSidebarOpen.value = detailDisplayConfig.value.navigator.pages.projectView.layout === 'sidebar';
    applyRouteQuery();
    ensureAllowedStatus();
    await Promise.all([loadActivityOptions(), loadSchoolOptions()]);
    applyRouteQuery();
    await refreshProjectView();
  } catch (error: any) {
    proxy?.$modal.msgError(error?.msg || '加载上报进度数据失败');
  }
});
</script>

<style scoped>
.project-view-page :deep(.el-card__body) {
  overflow-x: auto;
}

.project-view-workspace {
  display: grid;
  grid-template-columns: minmax(260px, var(--navigator-sidebar-width, 340px)) minmax(0, 1fr);
  gap: 10px;
  align-items: start;
}

.project-view-workspace.is-top-layout {
  grid-template-columns: minmax(0, 1fr);
}

.browse-panel {
  min-height: 520px;
  min-width: 0;
}

.project-view-list-card {
  min-width: 0;
}

.project-view-list-card :deep(.art-project-workspace-header) {
  margin-bottom: 10px;
}

.project-view-header-context,
.project-view-header-search {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.project-view-top-category-filter {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.project-view-top-category-filter > span {
  flex: 0 0 auto;
  color: var(--navigator-text-color);
  font-size: var(--navigator-item-size);
  font-weight: 800;
  white-space: nowrap;
}

.project-view-category-filter {
  width: clamp(180px, 14vw, 240px);
}

.browse-panel :deep(.el-tabs__item) {
  font-size: 14px;
}

.browse-panel :deep(.el-tabs__header) {
  margin-bottom: 8px;
}

.browse-panel :deep(.el-tree-node__content) {
  height: 40px;
  font-size: 14px;
  color: #303133;
}

.browse-panel :deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.category-node-label {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 14px;
}

.category-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-count {
  flex: 0 0 auto;
  font-size: 14px;
  color: #64748b;
}

.table-head,
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.table-filter {
  margin-top: 10px;
}

.school-search {
  margin-bottom: 8px;
}

.school-item {
  display: flex;
  width: 100%;
  min-height: 40px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  font-size: 14px;
  color: #334155;
  text-align: left;
  border: 0;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
}

.school-item:hover,
.school-item.active {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.school-item .school-name {
  min-width: 0;
  font-size: 14px;
  line-height: 20px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.school-item .school-count {
  flex: 0 0 auto;
  font-size: 14px;
  line-height: 20px;
  color: #64748b;
}

.section-head {
  margin-top: 16px;
}

.section-head .el-divider {
  flex: 1;
  margin-right: 0;
}

@media (max-width: 1180px) {
  .project-view-workspace {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .project-view-top-category-filter {
    width: 100%;
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .project-view-top-category-filter .project-view-category-filter {
    flex: 1 1 220px;
    width: auto;
  }
}
</style>

