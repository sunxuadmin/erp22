<template>
  <section
    class="workbench-card school-project-list"
    :class="{
      'is-maximized': maximized,
      'is-page-all': pageSizeSelection === 'all',
      'is-standalone': standalone,
      'is-expanded': expandedMode
    }"
    :style="componentSurfaceStyle"
    :aria-label="props.title || '统一提交列表'"
  >
    <div
      class="school-project-submit-layout"
      :class="{
        'is-standalone': standalone,
        'is-expanded': expandedMode,
        'is-navigator-collapsed': navigatorCollapsed || !activeHeaderConfig.sidebarEnabled
      }"
    >
      <ProjectBrowseNavigator
        v-if="expandedMode && activeHeaderConfig.sidebarEnabled && !navigatorCollapsed"
        :activities="activityOptions"
        :activity-id="activeActivityId"
        :show-activity="false"
        :categories="categoryList"
        :category-count-map="navigatorStatsMap"
        :selected-category-key="selectedBrowseCategoryKey"
        :show-unit-browse="false"
        :show-all-button="false"
        compact-sidebar
        force-sidebar
        collapsible
        sidebar-title="筛选项目"
        @update:collapsed="navigatorCollapsed = $event"
        @select-category="handleBrowseCategorySelection"
      >
        <template v-if="groupOptions.length" #category-prefix>
          <div class="school-project-group-filter" aria-label="项目组别筛选">
            <button
              v-for="item in groupFilterOptions"
              :key="item.value || 'all'"
              type="button"
              :class="{ 'is-active': activeGroupCode === item.value }"
              @click="handleGroupSelection(item.value)"
            >
              {{ item.label }}<span>（{{ item.count }}）</span>
            </button>
          </div>
        </template>
      </ProjectBrowseNavigator>

      <div class="school-project-submit-main">
        <ArtProjectWorkspaceHeader page-key="schoolSubmit" :config="activeHeaderConfig" :title-variables="{ activityName: selectedActivityName }">
          <template #navigatorToggle>
            <ArtWorkspaceConfigButton
              v-if="expandedMode && activeHeaderConfig.sidebarEnabled"
              :icon="navigatorCollapsed ? 'Expand' : 'Fold'"
              :text="navigatorCollapsed ? activeHeaderConfig.navigatorToggleButton.text : activeHeaderConfig.navigatorToggleButton.alternateText"
              :tooltip="
                navigatorCollapsed ? activeHeaderConfig.navigatorToggleButton.tooltip : activeHeaderConfig.navigatorToggleButton.alternateTooltip
              "
              @click="navigatorCollapsed = !navigatorCollapsed"
            />
          </template>
          <template #categoryFilter>
            <div class="school-project-header-filter">
              <el-dropdown
                v-if="!expandedMode"
                class="school-project-home-category"
                trigger="click"
                @command="handleCategoryCommand"
                @visible-change="handleCategoryMenuVisibleChange"
              >
                <button class="summary-pill summary-pill--category" type="button" :title="`当前类别：${activeCategoryLabel}`">
                  <span class="summary-pill__label">{{ activeCategoryLabel }}</span>
                  <el-icon class="summary-pill__arrow"><arrow-down /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu class="school-project-category-menu">
                    <el-dropdown-item v-if="categoryAllTab" :command="categoryAllTab.key" :class="{ 'is-active': categoryAllTab.key === activeTab }">
                      <span class="category-menu-item category-menu-item--all">
                        <span>{{ categoryAllTab.label }}</span>
                        <em v-if="categoryAllTab.showCount">{{ categoryAllTab.count }}</em>
                      </span>
                    </el-dropdown-item>
                    <template v-for="tab in categoryMenuTree" :key="tab.key">
                      <el-dropdown-item :command="tab.key" :class="{ 'is-active': tab.key === activeTab, 'is-category-group': tab.children?.length }">
                        <span class="category-menu-item category-menu-item--root">
                          <button
                            v-if="tab.children?.length"
                            class="category-menu-toggle"
                            type="button"
                            :aria-label="isCategoryMenuGroupExpanded(tab.key) ? `收起${tab.label}` : `展开${tab.label}`"
                            @click.stop.prevent="toggleCategoryMenuGroup(tab.key)"
                          >
                            <el-icon :class="{ 'is-expanded': isCategoryMenuGroupExpanded(tab.key) }"><arrow-right /></el-icon>
                          </button>
                          <span>{{ tab.label }}</span>
                          <em v-if="tab.showCount">{{ tab.count }}</em>
                        </span>
                      </el-dropdown-item>
                      <template v-if="tab.children?.length && isCategoryMenuGroupExpanded(tab.key)">
                        <el-dropdown-item
                          v-for="child in tab.children"
                          :key="child.key"
                          :command="child.key"
                          :class="{ 'is-active': child.key === activeTab }"
                        >
                          <span class="category-menu-item category-menu-item--child">
                            <span>{{ child.label }}</span>
                            <em v-if="child.showCount">{{ child.count }}</em>
                          </span>
                        </el-dropdown-item>
                      </template>
                    </template>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <ArtCategoryTreeDropdown
                v-else
                :tree="schoolCategoryTree"
                :count-map="navigatorStatsMap"
                :selected-key="selectedBrowseCategoryKey"
                :disabled="!activeActivityId"
                :placeholder="schoolHeaderText('categoryFilter', 'categoryPlaceholder', '全部类别')"
                :all-label="schoolHeaderText('categoryFilter', 'categoryPlaceholder', '全部类别')"
                @select="handleBrowseCategorySelection"
              />
            </div>
          </template>
          <template #groupFilter>
            <div v-if="groupOptions.length" class="school-project-header-filter">
              <el-select
                v-model="activeGroupCode"
                clearable
                :placeholder="schoolHeaderText('groupFilter', 'groupPlaceholder', '全部组别')"
                @change="handleGroupSelection"
              >
                <el-option :label="schoolHeaderText('groupFilter', 'groupPlaceholder', '全部组别')" value="" />
                <el-option v-for="item in groupOptions" :key="item.groupCode" :label="item.groupName || item.groupCode" :value="item.groupCode" />
              </el-select>
            </div>
          </template>
          <template #status>
            <ArtStatusTabs
              :items="schoolStatusTabItems"
              :model-value="activeStatus || 'all'"
              :collapse-on-overflow="activeHeaderConfig.statusCollapseOnOverflow"
              aria-label="上报状态"
              @change="handleStatusCommand(String($event || 'all'))"
            />
          </template>
          <template #progress>
            <div
              v-if="resolvedConfig.progress.visible"
              class="school-project-audit-progress"
              :style="schoolAuditProgressStyle"
              aria-label="审核总进度"
            >
              <div class="school-project-audit-progress__text">
                <strong>{{ resolvedConfig.progress.title }}</strong>
                <span>{{ schoolAuditProgressText }}</span>
              </div>
              <el-progress
                :percentage="schoolAuditProgress.percentage"
                :stroke-width="resolvedConfig.progress.height"
                :show-text="false"
                :color="schoolAuditProgressColor"
              />
            </div>
          </template>
          <template #search>
            <div class="school-project-header-controls art-list-header-search">
              <el-input
                v-model="projectNameKeyword"
                class="school-project-name-search"
                clearable
                :placeholder="schoolHeaderText('search', 'placeholder', '请输入项目名称')"
                @keyup.enter="refreshList"
                @clear="refreshList"
              />
              <el-button class="school-project-filter-primary" icon="Search" @click="refreshList">{{
                schoolHeaderText('search', 'search', '搜索')
              }}</el-button>
              <el-button class="school-project-filter-btn" icon="Refresh" @click="resetFilters">{{
                schoolHeaderText('search', 'reset', '重置')
              }}</el-button>
            </div>
          </template>
          <template #columnWidthReset>
            <ArtWorkspaceConfigButton
              icon="RefreshLeft"
              :text="activeHeaderConfig.columnWidthResetButton.text"
              :tooltip="activeHeaderConfig.columnWidthResetButton.tooltip"
              :disabled="!hasLocalColumnWidths"
              @click="restoreBackendColumnWidths"
            />
          </template>
          <template #navigationButton>
            <ArtWorkspaceConfigButton
              :icon="activeHeaderConfig.navigationButton.iconVisible === false ? '' : 'Link'"
              :text="activeHeaderConfig.navigationButton.text"
              :tooltip="activeHeaderConfig.navigationButton.tooltip"
              :appearance-style="navigationButtonAppearanceStyle"
              @click="handleNavigation"
            />
          </template>
          <template #home>
            <ArtWorkspaceConfigButton
              v-if="maximized || showBackButton || !expandedMode"
              :icon="activeHeaderConfig.homeButton.iconVisible === false ? '' : 'FullScreen'"
              :text="activeHeaderConfig.homeButton.text"
              :tooltip="activeHeaderConfig.homeButton.tooltip"
              :appearance-style="homeButtonAppearanceStyle"
              @click="handleHome"
            />
          </template>
        </ArtProjectWorkspaceHeader>
        <el-alert v-if="managedActivityError" class="mb-2" type="warning" :closable="false" :title="managedActivityError" />
        <div
          ref="tableShellRef"
          class="school-project-table-shell art-resizable-table-shell art-resizable-table-shell--fit"
          :class="{ 'is-compact': compactTable }"
        >
          <el-table
            ref="tableRef"
            v-loading="loading"
            :data="projectList"
            :border="true"
            :class="['art-list-table', 'is-wrap-wrap', tableAppearanceClass]"
            :style="tableAppearanceStyle"
            height="100%"
            row-key="id"
            :empty-text="schoolTablePage.emptyText"
            @header-dragend="handleColumnResize"
          >
            <el-table-column
              v-if="serialColumn"
              type="index"
              column-key="serial"
              :label="serialColumn.label"
              :width="fittedColumnWidth('serial')"
              :fixed="serialColumn.fixed"
              :resizable="serialColumn.resizable"
              label-class-name="school-project-resizable-header"
              align="center"
              :index="serialIndex"
            />
            <el-table-column
              v-for="column in nonActionColumns"
              :key="column.key"
              :column-key="column.key"
              :label="column.label"
              :prop="columnProp(column.key)"
              :width="fittedColumnWidth(column.key)"
              :resizable="column.resizable"
              :align="column.key === 'status' ? 'center' : 'left'"
              :fixed="column.fixed"
              :class-name="schoolProjectColumnClassName(column)"
              label-class-name="school-project-resizable-header"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <template v-if="column.key === 'categoryName'">{{ categoryDisplayName(row) }}</template>
                <ArtListStatusTag
                  v-else-if="column.key === 'status'"
                  :semantic="statusSemantic(row.status)"
                  :label="schoolStatusText(statusSemantic(row.status), statusLabel(row.status))"
                  :tag-type="statusType(row.status)"
                />
                <template v-else>{{ schoolColumnText(row, column) }}</template>
              </template>
            </el-table-column>
            <el-table-column
              v-if="actionColumn"
              :key="actionColumn.key"
              :column-key="actionColumn.key"
              :label="actionColumn.label"
              :width="fittedColumnWidth(actionColumn.key)"
              :min-width="actionColumnMinWidth(actionColumn)"
              :fixed="actionColumn.fixed"
              :resizable="actionColumn.resizable"
              align="left"
              header-align="left"
              label-class-name="school-project-resizable-header"
              :class-name="schoolProjectColumnClassName(actionColumn)"
              :show-overflow-tooltip="false"
            >
              <template #default="{ row }">
                <div class="school-project-actions">
                  <el-button
                    class="school-project-action school-project-action--view"
                    type="primary"
                    link
                    size="small"
                    :icon="actionIcon('View')"
                    @click="openProject(row)"
                  >
                    {{ schoolActionText('view', '查看') }}
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="school-project-pagination-footer">
          <div class="school-project-pager">
            <el-button class="school-project-page-btn" plain icon="ArrowLeft" :disabled="query.pageNum <= 1" @click="changePage(query.pageNum - 1)"
              >上页</el-button
            >
            <span>{{ pageInfo }}</span>
            <el-button
              class="school-project-page-btn"
              plain
              icon="ArrowRight"
              :disabled="query.pageNum >= pageCount"
              @click="changePage(query.pageNum + 1)"
              >下页</el-button
            >
          </div>
          <div class="school-project-page-size">
            <span>每页</span>
            <el-select v-model="pageSizeSelection" size="small" @change="handlePageSizeSelectionChange">
              <el-option label="10" value="10" />
              <el-option label="15" value="15" />
              <el-option label="20" value="20" />
              <el-option v-if="expandedMode" label="50" value="50" />
              <el-option label="全部" value="all" />
              <el-option label="自定义" value="custom" />
            </el-select>
            <el-input-number
              v-if="pageSizeSelection === 'custom'"
              v-model="customPageSize"
              class="school-project-custom-page-size"
              size="small"
              :min="1"
              :max="50"
              controls-position="right"
              placeholder="条数"
              @change="handleCustomPageSizeChange"
            />
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { listAvailableActivity, listSchoolCategoryCatalog } from '@/api/crehn/activity';
import type {
  ArtAuditProgressConfig,
  ArtListStatusSemantic,
  ArtReviewListColumnConfig,
  ArtWorkspaceHeaderButtonAppearanceConfig,
  ArtWorkspaceHeaderPageConfig
} from '@/api/crehn/detailDisplay';
import { listActivityReportRuleSchoolOptions } from '@/api/crehn/config';
import { listMyProject, listMyProjectCategoryStats } from '@/api/crehn/project';
import { ActivityCategoryVO, ActivityRuleGroupOptionVO, ActivityVO, ProjectCategoryStatsVO, ProjectVO } from '@/api/crehn/types';
import { buildSchoolCategoryMenuTree, isCategoryGroup } from '@/utils/artCategory';
import { useUserStore } from '@/store/modules/user';
import { useArtListTableAppearance } from '@/composables/useArtListTableAppearance';
import { artTableColumnValue, artTableDisplayText, useArtListTablePage } from '@/composables/useArtListTableConfig';
import { resolveArtTableFitWidths, type ArtResizableColumn } from '@/composables/useArtTableColumnWidths';
import { useManagedArtActivity } from '@/composables/useManagedArtActivity';
import ArtCategoryTreeDropdown from '@/views/crehn/components/ArtCategoryTreeDropdown.vue';
import ArtProjectWorkspaceHeader from '@/views/crehn/components/ArtProjectWorkspaceHeader.vue';
import ArtWorkspaceConfigButton from '@/views/crehn/components/ArtWorkspaceConfigButton.vue';
import ArtStatusTabs from '@/views/crehn/components/ArtStatusTabs.vue';
import ArtListStatusTag from '@/views/crehn/components/ArtListStatusTag.vue';
import ProjectBrowseNavigator from '@/views/crehn/components/ProjectBrowseNavigator.vue';
import {
  WORKSPACE_HEADER_LAYOUT_VERSION,
  defaultWorkspaceHeaderPage,
  defaultSchoolSubmitHomeHeader,
  defaultSchoolSubmitMaximizedHeader,
  defaultSchoolSubmitProgress,
  loadArtDetailDisplayConfig,
  normalizeWorkspaceHeaderPage,
  useArtDetailDisplayConfig,
  workspaceHeaderItemText
} from '@/views/crehn/components/artDetailDisplayConfig';
import { isExternalWorkspaceNavigationTarget, resolveWorkspaceNavigationTarget } from '@/views/crehn/components/artWorkspaceNavigation';
import { normalizeSchoolProjectCategoryAllLabel, schoolProjectColumnWidthSignature } from '@/views/workbench/schoolProjectSubmitConfig';
import type { ElTable } from 'element-plus';

type ConfigItem = {
  key: string;
  label: string;
  sourceType?: 'group' | 'category';
  sourceId?: string | number;
  sourceCode?: string;
  sourceName?: string;
  sourceLevel?: 1 | 2;
  visible?: boolean;
  order?: number;
  width?: number;
  aliases?: string[];
  showCount?: boolean;
};

type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger';

type ProjectListConfig = {
  componentBackground: string;
  componentBackgroundOpacity: number;
  componentBorderVisible: boolean;
  componentBorderColor: string;
  componentBorderRadius: number;
  defaultTabKey?: string;
  expandedPadding: number;
  expandedGap: number;
  expandedBackground: string;
  expandedBackgroundOpacity: number;
  expandedLayoutVersion: number;
  sceneHeadersVersion: number;
  homeHeader?: ArtWorkspaceHeaderPageConfig;
  maximizedHeader?: ArtWorkspaceHeaderPageConfig;
  progress: ArtAuditProgressConfig;
  tabs: ConfigItem[];
};

const ACTION_COLUMN_WIDTH = 220;
const ACTION_COLUMN_MIN_WIDTH = 150;

const props = withDefaults(
  defineProps<{
    title: string;
    configJson?: string;
    standalone?: boolean;
    initialStatus?: string;
    maximized?: boolean;
    showBackButton?: boolean;
    roleKey?: string;
  }>(),
  {
    standalone: false,
    initialStatus: '',
    maximized: false,
    showBackButton: false,
    roleKey: ''
  }
);
const emit = defineEmits<{ (event: 'toggle-maximize', value: boolean): void }>();

const defaultConfig: ProjectListConfig = {
  componentBackground: '#ffffff',
  componentBackgroundOpacity: 0,
  componentBorderVisible: false,
  componentBorderColor: '#e8edf5',
  componentBorderRadius: 8,
  defaultTabKey: 'all',
  expandedPadding: 0,
  expandedGap: 16,
  expandedBackground: '#f6f8fc',
  expandedBackgroundOpacity: 0,
  expandedLayoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
  sceneHeadersVersion: 2,
  homeHeader: defaultSchoolSubmitHomeHeader(),
  maximizedHeader: defaultSchoolSubmitMaximizedHeader(),
  progress: defaultSchoolSubmitProgress(),
  tabs: [
    { key: 'all', label: '全部类别', aliases: [], visible: true, order: 1, showCount: true },
    { key: 'vocal', label: '声乐', aliases: ['声乐', 'performance_vocal'], visible: true, order: 2, showCount: true },
    { key: 'instrumental', label: '器乐', aliases: ['器乐', 'performance_instrumental'], visible: true, order: 3, showCount: true },
    { key: 'dance', label: '舞蹈', aliases: ['舞蹈', 'performance_dance'], visible: true, order: 4, showCount: true },
    { key: 'drama', label: '戏剧', aliases: ['戏剧', 'performance_drama'], visible: true, order: 5, showCount: true },
    { key: 'recitation', label: '朗诵', aliases: ['朗诵', 'performance_recitation'], visible: true, order: 6, showCount: true },
    { key: 'fine_art', label: '美术类', aliases: ['美术', 'artwork_fine_art'], visible: true, order: 7, showCount: true },
    { key: 'grand_design', label: '大艺展设计类', aliases: ['大艺展设计', 'artwork_grand_design'], visible: true, order: 8, showCount: true },
    { key: 'design', label: '设计展', aliases: ['设计展', 'artwork_design'], visible: true, order: 9, showCount: true },
    { key: 'film', label: '影视类', aliases: ['影视', '影像', 'artwork_film'], visible: true, order: 10, showCount: true },
    { key: 'principal', label: '高校校长书画作品', aliases: ['校长书画', 'artwork_principal'], visible: true, order: 11, showCount: true }
  ]
};

const DEFAULT_COLUMN_WIDTHS: Record<string, number> = {
  selection: 58,
  serial: 70,
  categoryName: 150,
  projectName: 360,
  status: 130,
  updateTime: 190,
  submittedAt: 190,
  actions: ACTION_COLUMN_WIDTH,
  currentAuditOpinion: 260
};

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const loading = ref(false);
const projectList = ref<ProjectVO[]>([]);
const total = ref(0);
const allTotal = ref(0);
const allProjectRows = ref<ProjectVO[]>([]);
const groupOptions = ref<ActivityRuleGroupOptionVO[]>([]);
const activeGroupCode = ref('');
const activeTab = ref('');
const browseCategoryActive = ref(false);
const browseCategoryIds = ref<Array<string | number>>([]);
const selectedBrowseCategoryKey = ref('');
const activeStatus = ref(props.initialStatus || '');
const tableRef = ref<InstanceType<typeof ElTable>>();
const activityOptions = ref<ActivityVO[]>([]);
const { managedActivityError, resolveManagedActivityId } = useManagedArtActivity(activityOptions);
const { detailDisplayConfig } = useArtDetailDisplayConfig();
const categoryList = ref<ActivityCategoryVO[]>([]);
const activeActivityId = ref<string | number>();
const selectedActivityName = computed(
  () => activityOptions.value.find((item) => String(item.id) === String(activeActivityId.value || ''))?.activityName || ''
);
const schoolHeaderConfig = computed(() => detailDisplayConfig.value.workspaceHeader.pages.schoolSubmit);
const schoolCategoryTree = computed(() => buildSchoolCategoryMenuTree(categoryList.value));
const statsMap = ref<Record<string, ProjectCategoryStatsVO>>({});
const query = reactive({ pageNum: 1, pageSize: props.standalone ? 50 : 10 });
type PageSizeSelection = '10' | '15' | '20' | '50' | 'all' | 'custom';
const pageSizeSelection = ref<PageSizeSelection>(props.standalone ? '50' : '10');
const customPageSize = ref(30);
const pageSizeSelectionBeforeMaximize = ref<PageSizeSelection>();
const customPageSizeBeforeMaximize = ref(30);
const projectNameKeyword = ref('');
const tableShellRef = ref<HTMLElement>();
const tableShellWidth = ref(0);
const compactTable = ref(false);
const localColumnWidths = ref<Record<string, number>>({});
const navigatorCollapsed = ref(false);
const expandedCategoryMenuKeys = ref<Set<string>>(new Set());
let tableResizeObserver: ResizeObserver | undefined;

const resolvedConfig = computed(() => mergeConfig(defaultConfig, parseConfig(props.configJson)));
const expandedMode = computed(() => props.standalone || props.maximized);
const activeHeaderConfig = computed(() => {
  const configured = props.standalone
    ? schoolHeaderConfig.value
    : props.maximized
      ? resolvedConfig.value.maximizedHeader
      : resolvedConfig.value.homeHeader;
  const fallback = props.standalone
    ? defaultWorkspaceHeaderPage('schoolSubmit')
    : props.maximized
      ? defaultSchoolSubmitMaximizedHeader()
      : defaultSchoolSubmitHomeHeader();
  const source = normalizeWorkspaceHeaderPage('schoolSubmit', JSON.parse(JSON.stringify(configured || fallback)), WORKSPACE_HEADER_LAYOUT_VERSION);
  const unavailableItems = new Set<string>();
  if (!expandedMode.value) unavailableItems.add('title');
  if (!expandedMode.value || !source.sidebarEnabled) unavailableItems.add('navigatorToggle');
  if (!groupOptions.value.length) unavailableItems.add('groupFilter');
  if (!resolvedConfig.value.progress.visible) unavailableItems.add('progress');
  if (!source.statusItems.some((item) => item.visible !== false)) unavailableItems.add('status');
  if (props.standalone && !props.showBackButton) {
    unavailableItems.add('home');
  }
  if (!unavailableItems.size) return source;
  const compactGroupInstances = Object.fromEntries(
    Object.entries(source.compactGroupInstances).map(([instanceId, group]) => [
      instanceId,
      {
        ...group,
        items: group.items.filter((item) => !unavailableItems.has(item)),
        pinnedItems: group.pinnedItems.filter((item) => !unavailableItems.has(item))
      }
    ])
  );
  return {
    ...source,
    compactGroupInstances,
    cards: source.cards.map((card) => ({
      ...card,
      rows: card.rows.map((row) => row.filter((item) => !unavailableItems.has(item)))
    }))
  };
});
const schoolHeaderText = (itemKey: 'categoryFilter' | 'groupFilter' | 'status' | 'search', textKey: string, fallback: string) =>
  workspaceHeaderItemText(activeHeaderConfig.value, itemKey, textKey, fallback);
const handleHome = () => {
  if (props.maximized) {
    emit('toggle-maximize', false);
    return;
  }
  if (!props.standalone) {
    emit('toggle-maximize', true);
    return;
  }
  router.push('/index');
};
const handleNavigation = async () => {
  const target = resolveWorkspaceNavigationTarget(activeHeaderConfig.value.navigationButton, props.roleKey || userStore.roles.join('_'));
  if (!target) {
    proxy?.$modal.msgWarning('跳转地址无效，请在工作台配置中重新设置');
    return;
  }
  const openInNewWindow = activeHeaderConfig.value.navigationButton.openMode === 'new';
  if (isExternalWorkspaceNavigationTarget(target) || openInNewWindow) {
    const href = isExternalWorkspaceNavigationTarget(target) ? target : router.resolve(target).href;
    window.open(href, '_blank', 'noopener,noreferrer');
    return;
  }
  await router.push(target);
};
const clampNumber = (value: unknown, fallback: number, min: number, max: number) =>
  Math.min(Math.max(Number.isFinite(Number(value)) ? Number(value) : fallback, min), max);
const colorWithOpacity = (color: string, opacity: number) => {
  const value = String(color || '#ffffff').trim();
  const alpha = clampNumber(opacity, 100, 0, 100) / 100;
  const shortHex = value.match(/^#([0-9a-f]{3})$/i)?.[1];
  const longHex = value.match(/^#([0-9a-f]{6})$/i)?.[1];
  const hex = shortHex
    ? shortHex
        .split('')
        .map((char) => `${char}${char}`)
        .join('')
    : longHex;
  if (!hex) return alpha === 0 ? 'transparent' : value;
  const number = Number.parseInt(hex, 16);
  return `rgba(${(number >> 16) & 255}, ${(number >> 8) & 255}, ${number & 255}, ${alpha})`;
};
const buttonAppearanceStyle = (appearance?: ArtWorkspaceHeaderButtonAppearanceConfig): Record<string, string> | undefined =>
  appearance
    ? {
        '--art-button-background-color': colorWithOpacity(appearance.backgroundColor, appearance.backgroundOpacity),
        '--art-button-text-color': appearance.textColor,
        '--art-button-border-color': appearance.borderVisible ? colorWithOpacity(appearance.borderColor, appearance.borderOpacity) : 'transparent',
        '--art-button-border-width': `${appearance.borderVisible ? appearance.borderWidth : 0}px`,
        '--art-button-border-radius': `${appearance.borderRadius}px`,
        '--art-button-hover-background-color': colorWithOpacity(appearance.hoverBackgroundColor, appearance.hoverBackgroundOpacity),
        '--art-button-hover-text-color': appearance.hoverTextColor,
        '--art-button-hover-border-color': appearance.borderVisible
          ? colorWithOpacity(appearance.hoverBorderColor, appearance.hoverBorderOpacity)
          : 'transparent'
      }
    : undefined;
const navigationButtonAppearanceStyle = computed(() => buttonAppearanceStyle(activeHeaderConfig.value.navigationButton.appearance));
const homeButtonAppearanceStyle = computed(() => buttonAppearanceStyle(activeHeaderConfig.value.homeButton.appearance));
const componentSurfaceStyle = computed(() => ({
  '--navigator-sidebar-width': `${detailDisplayConfig.value.navigator.sidebarWidth}px`,
  '--school-project-expanded-gap': `${resolvedConfig.value.expandedGap}px`,
  padding: expandedMode.value ? `${resolvedConfig.value.expandedPadding}px` : '0',
  background: expandedMode.value
    ? colorWithOpacity(resolvedConfig.value.expandedBackground, resolvedConfig.value.expandedBackgroundOpacity)
    : colorWithOpacity(resolvedConfig.value.componentBackground, resolvedConfig.value.componentBackgroundOpacity),
  border: resolvedConfig.value.componentBorderVisible ? `1px solid ${resolvedConfig.value.componentBorderColor}` : '0',
  borderRadius: `${resolvedConfig.value.componentBorderRadius}px`
}));
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / query.pageSize)));
const pageInfo = computed(() => `${query.pageNum}/${pageCount.value}`);
const categoryNameMap = computed<Record<string, string>>(() =>
  Object.fromEntries(
    categoryList.value
      .filter((item) => !isCategoryGroup(item) && item.id !== undefined && item.id !== null)
      .map((item) => [String(item.id), String(item.categoryName || '')])
  )
);
const groupFilteredRows = computed(() => {
  if (!activeGroupCode.value) return allProjectRows.value;
  return allProjectRows.value.filter((row) => String(row.groupCode || '') === activeGroupCode.value);
});
const groupFilterOptions = computed(() => [
  ...groupOptions.value.map((item) => ({
    value: String(item.groupCode || ''),
    label: String(item.groupName || item.groupCode || '未命名组别'),
    count: allProjectRows.value.filter((row) => String(row.groupCode || '') === String(item.groupCode || '')).length
  })),
  { value: '', label: '全部', count: allProjectRows.value.length }
]);
const navigatorStatsMap = computed<Record<string, ProjectCategoryStatsVO>>(() => {
  if (!activeGroupCode.value) return statsMap.value;
  return groupFilteredRows.value.reduce<Record<string, ProjectCategoryStatsVO>>((result, row) => {
    if (row.categoryId === undefined || row.categoryId === null) return result;
    const key = String(row.categoryId);
    const item = result[key] || { categoryId: row.categoryId, totalCount: 0 };
    item.totalCount = Number(item.totalCount || 0) + 1;
    result[key] = item;
    return result;
  }, {});
});

const sortedVisible = <T extends ConfigItem>(items: T[]) =>
  [...items].filter((item) => item.visible !== false).sort((a, b) => Number(a.order || 0) - Number(b.order || 0));
const {
  pageConfig: schoolTablePage,
  columns: schoolTableColumns,
  serialColumn,
  statusText: schoolStatusText,
  actionText: schoolActionText
} = useArtListTablePage('schoolSubmit', () => {
  const categoryIds = expandedMode.value ? activeCategoryIds.value : activeCategoryTab.value?.categoryIds || [];
  return categoryIds.length === 1 ? categoryIds[0] : undefined;
});
const visibleStats = computed(() =>
  [...activeHeaderConfig.value.statusItems]
    .filter((item) => item.visible !== false)
    .sort((a, b) => Number(a.order || 0) - Number(b.order || 0))
    .map((item) => ({
      ...item,
      label:
        item.key === 'all'
          ? schoolHeaderText('status', item.key, '全部')
          : schoolStatusText(statusSemantic(item.key), statusLabels[item.key] || item.key)
    }))
);
const visibleStatusStats = computed(() => visibleStats.value.filter((item) => item.key !== 'all'));
const visibleColumns = computed(() => schoolTableColumns.value.filter((column) => !['selection', 'serial'].includes(column.key)));
const nonActionColumns = computed(() => visibleColumns.value.filter((column) => column.key !== 'actions'));
const actionColumn = computed(() => visibleColumns.value.find((column) => column.key === 'actions'));
const configuredColumnKeys = computed(() => new Set(schoolTableColumns.value.map((column) => column.key).filter((key) => key !== 'actions')));
const columnWidthStorageKey = computed(() => {
  const userId = String(userStore.userId || 'anonymous');
  const roleKey = String(props.roleKey || userStore.roles.join('_') || 'default');
  return `art-workbench-column-widths:v1:${userId}:${roleKey}:school_project_list`;
});
const columnWidthConfigSignature = computed(() => schoolProjectColumnWidthSignature(schoolTableColumns.value));
const hasLocalColumnWidths = computed(() => Object.keys(localColumnWidths.value).length > 0);

type CategoryTabSource = ConfigItem & {
  categoryIds: Array<string | number>;
  sourceType: 'group' | 'category';
  sourceLevel: 1 | 2;
  children?: CategoryTabSource[];
};
type CategoryTabItem = ConfigItem & {
  categoryIds: Array<string | number>;
  count: number;
  sourceConfigKey?: string;
  children?: CategoryTabItem[];
};

const categorySourceKey = (sourceType: 'group' | 'category', sourceId?: string | number, sourceCode?: string, sourceName?: string) =>
  `${sourceType}:${String(sourceId ?? sourceCode ?? sourceName ?? '')}`;

const activityCategoryTabTreeSources = computed<CategoryTabSource[]>(() => {
  let order = 2;
  return buildSchoolCategoryMenuTree(categoryList.value).map((root) => {
    if (isCategoryGroup(root)) {
      const children = root.children || [];
      const childIds = children.map((item) => item.id).filter((id): id is string | number => id !== undefined && id !== null);
      const group: CategoryTabSource = {
        key: categorySourceKey('group', root.id, root.categoryCode, root.categoryName),
        label: String(root.categoryName || root.categoryCode || '未命名一级类别'),
        sourceType: 'group',
        sourceId: root.id,
        sourceCode: String(root.categoryCode || ''),
        sourceName: String(root.categoryName || ''),
        sourceLevel: 1,
        categoryIds: childIds,
        aliases: [root.categoryName, root.categoryCode].filter(Boolean).map(String),
        visible: true,
        order: order++,
        showCount: true,
        children: []
      };
      group.children = children
        .filter((child) => child.id !== undefined && child.id !== null)
        .map((child) => ({
          key: categorySourceKey('category', child.id, child.categoryCode, child.categoryName),
          label: String(child.categoryName || child.categoryCode || '未命名二级类别'),
          sourceType: 'category',
          sourceId: child.id,
          sourceCode: String(child.categoryCode || ''),
          sourceName: String(child.categoryName || ''),
          sourceLevel: 2,
          categoryIds: [child.id],
          aliases: [child.categoryName, child.categoryCode].filter(Boolean).map(String),
          visible: true,
          order: order++,
          showCount: true
        }));
      return group;
    }
    return {
      key: categorySourceKey('category', root.id, root.categoryCode, root.categoryName),
      label: String(root.categoryName || root.categoryCode || '未命名一级类别'),
      sourceType: 'category',
      sourceId: root.id,
      sourceCode: String(root.categoryCode || ''),
      sourceName: String(root.categoryName || ''),
      sourceLevel: 1,
      categoryIds: root.id === undefined || root.id === null ? [] : [root.id],
      aliases: [root.categoryName, root.categoryCode].filter(Boolean).map(String),
      visible: true,
      order: order++,
      showCount: true
    };
  });
});
const activityCategoryTabSources = computed<CategoryTabSource[]>(() =>
  activityCategoryTabTreeSources.value.flatMap((source) => [source, ...(source.children || [])])
);

const tabMatchesCategorySource = (tab: ConfigItem, source: CategoryTabSource) => {
  if (tab.key === source.key) return true;
  if (tab.sourceType === source.sourceType && tab.sourceId !== undefined && String(tab.sourceId) === String(source.sourceId)) return true;
  if (tab.sourceCode && source.sourceCode && tab.sourceCode === source.sourceCode) return true;
  const sourceAliases = new Set([source.sourceCode, source.sourceName].filter(Boolean));
  return (tab.aliases || []).some((alias) => sourceAliases.has(String(alias || '').trim()));
};

const configuredCategoryTab = (source: CategoryTabSource) => {
  const tabs = resolvedConfig.value.tabs;
  return (
    tabs.find((tab) => tab.key === source.key) ||
    tabs.find((tab) => tab.sourceType === source.sourceType && tab.sourceId !== undefined && String(tab.sourceId) === String(source.sourceId)) ||
    tabs.find((tab) => tabMatchesCategorySource(tab, source))
  );
};

const categoryTabs = computed<CategoryTabItem[]>(() => {
  const allConfig = resolvedConfig.value.tabs.find((tab) => tab.key === 'all') || defaultConfig.tabs[0];
  const tabs = [
    {
      ...allConfig,
      key: 'all',
      label: normalizeSchoolProjectCategoryAllLabel(allConfig.label),
      categoryIds: [] as Array<string | number>,
      sourceConfigKey: allConfig.key
    },
    ...activityCategoryTabSources.value.map((source) => {
      const configured = configuredCategoryTab(source);
      const hasCustomLabel = Boolean(configured?.label && configured.sourceName && String(configured.label) !== String(configured.sourceName));
      return {
        ...source,
        children: undefined,
        visible: configured?.visible !== false,
        order: Number(configured?.order || source.order),
        showCount: configured?.showCount !== false,
        label: hasCustomLabel || (configured && !configured.sourceName) ? String(configured?.label || source.label) : source.label,
        sourceConfigKey: configured?.key
      };
    })
  ];
  return sortedVisible(tabs).map((tab) => {
    const categoryIds = tab.categoryIds || [];
    const count =
      tab.key === 'all'
        ? groupFilteredRows.value.length
        : categoryIds.reduce<number>((sum, id) => sum + Number(navigatorStatsMap.value[String(id)]?.totalCount || 0), 0);
    return { ...tab, categoryIds, count };
  });
});
const categoryTabMap = computed(() => new Map(categoryTabs.value.map((tab) => [tab.key, tab])));
const categoryAllTab = computed(() => categoryTabMap.value.get('all'));
const categoryMenuTree = computed<CategoryTabItem[]>(() => {
  const result: CategoryTabItem[] = [];
  activityCategoryTabTreeSources.value.forEach((source) => {
    const tab = categoryTabMap.value.get(source.key);
    if (!tab) return;
    const children = (source.children || [])
      .map((child) => categoryTabMap.value.get(child.key))
      .filter((child): child is CategoryTabItem => Boolean(child))
      .sort((a, b) => Number(a.order || 0) - Number(b.order || 0));
    result.push({ ...tab, children });
  });
  return result.sort((a, b) => Number(a.order || 0) - Number(b.order || 0));
});
const resetExpandedCategoryMenuGroups = () => {
  expandedCategoryMenuKeys.value = new Set(categoryMenuTree.value.filter((tab) => tab.children?.length).map((tab) => tab.key));
};
const handleCategoryMenuVisibleChange = (visible: boolean) => {
  if (visible) resetExpandedCategoryMenuGroups();
};
const isCategoryMenuGroupExpanded = (key: string) => expandedCategoryMenuKeys.value.has(key);
const toggleCategoryMenuGroup = (key: string) => {
  const next = new Set(expandedCategoryMenuKeys.value);
  if (next.has(key)) next.delete(key);
  else next.add(key);
  expandedCategoryMenuKeys.value = next;
};

const activeCategoryIds = computed(() =>
  browseCategoryActive.value ? browseCategoryIds.value : categoryTabs.value.find((tab) => tab.key === activeTab.value)?.categoryIds || []
);
const allCategoriesSelected = computed(() => (browseCategoryActive.value ? browseCategoryIds.value.length === 0 : activeTab.value === 'all'));
const activeCategoryTab = computed(() => categoryTabs.value.find((tab) => tab.key === activeTab.value) || categoryTabs.value[0]);
const activeCategoryLabel = computed(() => activeCategoryTab.value?.label || normalizeSchoolProjectCategoryAllLabel());
const activeCategoryCount = computed(() => activeCategoryTab.value?.count || 0);
const syncBrowseSelectionFromActiveTab = () => {
  if (!expandedMode.value) return;
  const tab = categoryTabMap.value.get(activeTab.value) || categoryAllTab.value;
  browseCategoryActive.value = true;
  browseCategoryIds.value = tab?.categoryIds || [];
  selectedBrowseCategoryKey.value = tab?.key === 'all' ? '' : String(tab?.sourceId ?? '');
};
const summaryRows = computed(() => {
  if (allCategoriesSelected.value) return groupFilteredRows.value;
  const ids = new Set(activeCategoryIds.value.map((id) => String(id)));
  return groupFilteredRows.value.filter((row) => row.categoryId !== undefined && row.categoryId !== null && ids.has(String(row.categoryId)));
});
const statusSummary = computed(() =>
  summaryRows.value.reduce<Record<string, number>>(
    (summary, row) => {
      const key = row.status || 'draft';
      summary[key] = (summary[key] || 0) + 1;
      return summary;
    },
    { draft: 0, submitted: 0, returned: 0, audit_passed: 0 }
  )
);
const schoolAuditProgress = computed(() => {
  const pending = statusSummary.value.submitted || 0;
  const completed = (statusSummary.value.audit_passed || 0) + (statusSummary.value.returned || 0);
  const total = pending + completed;
  return {
    pending,
    completed,
    total,
    percentage: total > 0 ? Math.round((completed / total) * 100) : 0
  };
});
const schoolAuditProgressText = computed(() =>
  resolvedConfig.value.progress.template
    .replaceAll('{completed}', String(schoolAuditProgress.value.completed))
    .replaceAll('{total}', String(schoolAuditProgress.value.total))
    .replaceAll('{pending}', String(schoolAuditProgress.value.pending))
    .replaceAll('{percentage}', String(schoolAuditProgress.value.percentage))
);
const schoolAuditProgressColor = computed(() =>
  schoolAuditProgress.value.total > 0 && schoolAuditProgress.value.completed >= schoolAuditProgress.value.total
    ? resolvedConfig.value.progress.completeColor
    : resolvedConfig.value.progress.activeColor
);
const schoolAuditProgressStyle = computed(() => ({
  width: `${resolvedConfig.value.progress.width}px`,
  color: resolvedConfig.value.progress.textColor,
  fontSize: `${resolvedConfig.value.progress.fontSize}px`,
  '--school-audit-progress-track': resolvedConfig.value.progress.trackColor
}));

const statusLabels: Record<string, string> = {
  draft: '草稿',
  submitted: '待审核',
  returned: '已退回',
  audit_passed: '已通过'
};

const statusTypes: Record<string, TagType> = {
  draft: 'info',
  submitted: 'warning',
  returned: 'danger',
  audit_passed: 'success'
};

const parseConfig = (value?: string): Partial<ProjectListConfig> => {
  if (!value) return {};
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
};

const normalizeSchoolProgress = (value: Partial<ArtAuditProgressConfig> | undefined, fallback: ArtAuditProgressConfig): ArtAuditProgressConfig => ({
  visible: typeof value?.visible === 'boolean' ? value.visible : fallback.visible,
  title: String(value?.title || fallback.title)
    .trim()
    .slice(0, 20),
  template: String(value?.template || fallback.template)
    .trim()
    .slice(0, 80),
  height: clampNumber(value?.height, fallback.height, 4, 20),
  width: clampNumber(value?.width, fallback.width, 120, 480),
  activeColor: value?.activeColor || fallback.activeColor,
  completeColor: value?.completeColor || fallback.completeColor,
  trackColor: value?.trackColor || fallback.trackColor,
  textColor: value?.textColor || fallback.textColor,
  fontSize: clampNumber(value?.fontSize, fallback.fontSize, 12, 20)
});

const mergeConfig = (base: ProjectListConfig, custom: Partial<ProjectListConfig>): ProjectListConfig => ({
  componentBackground: custom.componentBackground || base.componentBackground,
  componentBackgroundOpacity: clampNumber(custom.componentBackgroundOpacity, base.componentBackgroundOpacity, 0, 100),
  componentBorderVisible: custom.componentBorderVisible === true,
  componentBorderColor: custom.componentBorderColor || base.componentBorderColor,
  componentBorderRadius: clampNumber(custom.componentBorderRadius, base.componentBorderRadius, 0, 32),
  defaultTabKey: custom.defaultTabKey || base.defaultTabKey,
  expandedPadding: clampNumber(custom.expandedPadding, base.expandedPadding, 0, 32),
  expandedGap: clampNumber(custom.expandedGap, base.expandedGap, 0, 32),
  expandedBackground: custom.expandedBackground || base.expandedBackground,
  expandedBackgroundOpacity: clampNumber(custom.expandedBackgroundOpacity, base.expandedBackgroundOpacity, 0, 100),
  expandedLayoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
  sceneHeadersVersion: 2,
  homeHeader: normalizeWorkspaceHeaderPage(
    'schoolSubmit',
    custom.sceneHeadersVersion === 2 ? custom.homeHeader || base.homeHeader : base.homeHeader,
    WORKSPACE_HEADER_LAYOUT_VERSION
  ),
  maximizedHeader: normalizeWorkspaceHeaderPage(
    'schoolSubmit',
    custom.sceneHeadersVersion === 2 ? custom.maximizedHeader || base.maximizedHeader : base.maximizedHeader,
    WORKSPACE_HEADER_LAYOUT_VERSION
  ),
  progress: normalizeSchoolProgress(custom.progress, base.progress),
  tabs: mergeItems(base.tabs, custom.tabs).map((item) =>
    item.key === 'all' ? { ...item, label: normalizeSchoolProjectCategoryAllLabel(item.label) } : item
  )
});

const mergeItems = (base: ConfigItem[], custom?: ConfigItem[]) => {
  const customMap = new Map((custom || []).map((item) => [item.key, item]));
  const merged = base.map((item) => ({ ...item, ...(customMap.get(item.key) || {}) }));
  (custom || []).forEach((item) => {
    if (!merged.some((baseItem) => baseItem.key === item.key)) {
      merged.push(item);
    }
  });
  return merged;
};

const statValue = (key: string) => {
  if (key === 'all') return summaryRows.value.length;
  return statusSummary.value[key] || 0;
};
const schoolStatusTabItems = computed(() =>
  visibleStats.value.map((item) => ({
    label: item.label,
    value: item.key,
    count: statValue(item.key)
  }))
);

const statusLabel = (status?: string) => statusLabels[status || ''] || status || '-';
const statusType = (status?: string): TagType => statusTypes[status || ''] || 'info';
const statusSemantics: Record<string, ArtListStatusSemantic> = {
  draft: 'draft',
  submitted: 'pending',
  returned: 'returned',
  audit_passed: 'approved'
};
const statusSemantic = (status?: string): ArtListStatusSemantic => statusSemantics[status || ''] || 'draft';
const categoryDisplayName = (row: ProjectVO) => {
  if (row.categoryName) return row.categoryName;
  if (row.categoryId !== undefined && row.categoryId !== null) {
    return categoryNameMap.value[String(row.categoryId)] || '-';
  }
  return '-';
};

const cleanQueryText = (value?: unknown) => String(value ?? '').trim();
const categoryFillTitle = (categoryName?: unknown, categoryId?: string | number, projectId?: string | number) => {
  const name = cleanQueryText(categoryName);
  if (name && name !== '-') return `${name}填报`;
  const id = cleanQueryText(categoryId);
  if (id) return `类别${id}填报`;
  const fallbackProjectId = cleanQueryText(projectId);
  return fallbackProjectId ? `项目${fallbackProjectId}填报` : '未分类填报';
};

const buildEditQuery = (row: ProjectVO) => {
  const categoryName = categoryDisplayName(row);
  const query: Record<string, any> = {
    returnPath: route.fullPath,
    title: categoryFillTitle(categoryName, row.categoryId, row.id)
  };
  if (categoryName && categoryName !== '-') {
    query.categoryName = categoryName;
  }
  if (row.categoryId !== undefined && row.categoryId !== null) {
    query.categoryId = row.categoryId;
  }
  if (row.activityId !== undefined && row.activityId !== null) {
    query.activityId = row.activityId;
  }
  return query;
};

const serialIndex = (index: number) => (query.pageNum - 1) * query.pageSize + index + 1;
const columnProp = (key: string) => (key === 'actions' ? undefined : key);
const schoolProjectColumnClassName = (column: ArtReviewListColumnConfig) => {
  if (column.key === 'actions') return 'school-project-action-cell art-table-nowrap-cell';
  if (column.key === 'status') return 'school-project-wrap-cell art-table-nowrap-cell';
  return 'school-project-wrap-cell art-table-configurable-wrap-cell';
};
const actionColumnMinWidth = (column: ArtReviewListColumnConfig) =>
  Math.max(
    ACTION_COLUMN_MIN_WIDTH,
    Number.isFinite(Number(column.minWidth)) ? Number(column.minWidth) : 0,
    Number.isFinite(Number(column.width)) ? Number(column.width) : 0
  );
const columnWidthLimits: Record<string, { min: number; max: number }> = {
  selection: { min: 44, max: 120 },
  serial: { min: 56, max: 140 },
  categoryName: { min: 90, max: 420 },
  projectName: { min: 180, max: 800 },
  status: { min: 90, max: 260 },
  updateTime: { min: 130, max: 380 },
  submittedAt: { min: 130, max: 380 },
  currentAuditOpinion: { min: 160, max: 800 }
};
const columnWidthLimit = (key: string) => columnWidthLimits[key] || { min: 80, max: 800 };
const normalizeColumnWidth = (key: string, value: unknown, fallback: number) => {
  const limit = columnWidthLimit(key);
  const width = Number.isFinite(Number(value)) ? Math.round(Number(value)) : fallback;
  return Math.min(Math.max(width, limit.min), limit.max);
};
const backendColumnWidth = (key: string) => {
  if (key === 'serial') return normalizeColumnWidth(key, schoolTablePage.value.serialWidth, 66);
  const column = schoolTableColumns.value.find((item) => item.key === key);
  const fallback = DEFAULT_COLUMN_WIDTHS[key] || 120;
  return normalizeColumnWidth(key, column?.width, fallback);
};
const resolvedColumnWidth = (key: string) => normalizeColumnWidth(key, localColumnWidths.value[key], backendColumnWidth(key));
const columnWidth = (column: ArtReviewListColumnConfig) => (column.key === 'actions' ? column.width : resolvedColumnWidth(column.key));
const tableFitColumns = computed<ArtResizableColumn[]>(() => {
  const columns: ArtResizableColumn[] = [];
  if (serialColumn.value) {
    columns.push({
      key: 'serial',
      width: resolvedColumnWidth('serial'),
      minWidth: Math.max(48, Number(serialColumn.value.minWidth) || 0),
      fixed: serialColumn.value.fixed,
      resizable: serialColumn.value.resizable
    });
  }
  nonActionColumns.value.forEach((column) => {
    columns.push({
      key: column.key,
      width: Number(columnWidth(column)) || 120,
      minWidth: Math.max(24, Number(column.minWidth) || 0),
      fixed: column.fixed,
      resizable: column.resizable
    });
  });
  if (actionColumn.value) {
    columns.push({
      key: actionColumn.value.key,
      width: actionColumnMinWidth(actionColumn.value),
      minWidth: actionColumnMinWidth(actionColumn.value),
      fixed: actionColumn.value.fixed,
      resizable: false
    });
  }
  return columns;
});
const fittedColumnWidths = computed(() => {
  const preferredWidths = Object.fromEntries(tableFitColumns.value.map((column) => [column.key, column.width]));
  if (!tableShellWidth.value) return preferredWidths;
  return resolveArtTableFitWidths(tableFitColumns.value, preferredWidths, tableShellWidth.value);
});
const fittedColumnWidth = (key: string) => fittedColumnWidths.value[key];

const loadLocalColumnWidths = () => {
  const widths: Record<string, number> = {};
  try {
    const raw = window.localStorage.getItem(columnWidthStorageKey.value);
    const parsed = raw ? JSON.parse(raw) : undefined;
    const storedWidths =
      parsed &&
      typeof parsed === 'object' &&
      parsed.signature === columnWidthConfigSignature.value &&
      parsed.widths &&
      typeof parsed.widths === 'object'
        ? parsed.widths
        : {};
    if (storedWidths && typeof storedWidths === 'object') {
      Object.entries(storedWidths).forEach(([key, value]) => {
        if (configuredColumnKeys.value.has(key) && Number.isFinite(Number(value))) {
          widths[key] = normalizeColumnWidth(key, value, backendColumnWidth(key));
        }
      });
    }
  } catch {
    // 本地旧数据损坏时忽略，由后台列宽继续兜底。
  }
  localColumnWidths.value = widths;
};

const persistLocalColumnWidths = () => {
  try {
    window.localStorage.setItem(
      columnWidthStorageKey.value,
      JSON.stringify({
        signature: columnWidthConfigSignature.value,
        widths: localColumnWidths.value
      })
    );
  } catch {
    proxy?.$modal.msgWarning('列宽已调整，但浏览器未允许保存本地配置');
  }
};

const handleColumnResize = (newWidth: number, _oldWidth: number, column: { columnKey?: string; property?: string }) => {
  const key = String(column?.columnKey || column?.property || '');
  if (!key || !configuredColumnKeys.value.has(key)) return;
  localColumnWidths.value = {
    ...localColumnWidths.value,
    [key]: normalizeColumnWidth(key, newWidth, backendColumnWidth(key))
  };
  persistLocalColumnWidths();
};

const restoreBackendColumnWidths = async () => {
  try {
    window.localStorage.removeItem(columnWidthStorageKey.value);
  } catch {
    // 即使浏览器拒绝访问存储，也先恢复本次页面的后台列宽。
  }
  localColumnWidths.value = {};
  await nextTick();
  tableRef.value?.doLayout();
  proxy?.$modal.msgSuccess('已恢复后台列宽');
};

const updateCompactTable = () => {
  tableShellWidth.value = Number(tableShellRef.value?.clientWidth || 0);
  compactTable.value = tableShellWidth.value < 1380;
};

const setupTableResizeObserver = () => {
  updateCompactTable();
  if (!tableShellRef.value || typeof ResizeObserver === 'undefined') return;
  tableResizeObserver = new ResizeObserver(updateCompactTable);
  tableResizeObserver.observe(tableShellRef.value);
};

const { tableAppearanceClass, tableAppearanceStyle, actionIcon } = useArtListTableAppearance({
  statusLabels: () => ['draft', 'pending', 'approved', 'returned'].map((key) => schoolTablePage.value.statusLabels[key as ArtListStatusSemantic]),
  actionLabels: () => ['view', 'submit', 'withdraw', 'delete'].map((key) => schoolTablePage.value.actionLabels[key])
});
const schoolColumnText = (row: ProjectVO, column: ArtReviewListColumnConfig) =>
  artTableDisplayText(artTableColumnValue(row as unknown as Record<string, unknown>, column));

const reportRuleTargetFieldKey = (ruleJson?: string) => {
  if (!ruleJson) return '';
  try {
    const parsed = JSON.parse(ruleJson) as Record<string, unknown>;
    return String(parsed.targetFieldKey || parsed.groupFieldKey || '');
  } catch {
    return '';
  }
};

const usesTopLevelGroup = (rule: ActivityRuleGroupOptionVO) => {
  const targetFieldKey = reportRuleTargetFieldKey(rule.ruleJson);
  return !targetFieldKey || targetFieldKey === 'groupCode' || targetFieldKey === '__group_code';
};

const normalizeGroupOptions = (items: ActivityRuleGroupOptionVO[]) => {
  const seen = new Set<string>();
  return items.filter((item) => {
    const code = String(item.groupCode || '').trim();
    if (!code || seen.has(code) || !usesTopLevelGroup(item)) return false;
    seen.add(code);
    return true;
  });
};

const loadActivityCategories = async () => {
  if (!activeActivityId.value) {
    categoryList.value = [];
    groupOptions.value = [];
    await loadSummaryData();
    return;
  }
  const [categoryRes, groupRes] = await Promise.all([
    listSchoolCategoryCatalog(activeActivityId.value),
    listActivityReportRuleSchoolOptions({ activityId: activeActivityId.value, enabled: true })
  ]);
  categoryList.value = categoryRes.data || [];
  groupOptions.value = normalizeGroupOptions(groupRes.data || []);
  if (!groupOptions.value.some((item) => String(item.groupCode || '') === activeGroupCode.value)) {
    activeGroupCode.value = '';
  }
  await loadSummaryData();
  ensureActiveTab();
  syncBrowseSelectionFromActiveTab();
};

const loadCatalog = async () => {
  const activityRes = await listAvailableActivity();
  activityOptions.value = activityRes.data || [];
  activeActivityId.value = resolveManagedActivityId();
  if (expandedMode.value) {
    browseCategoryActive.value = true;
    browseCategoryIds.value = [];
    selectedBrowseCategoryKey.value = '';
  }
  await loadActivityCategories();
};

const loadSummaryData = async () => {
  if (!activeActivityId.value) {
    statsMap.value = {};
    allProjectRows.value = [];
    allTotal.value = 0;
    return;
  }

  const [statsRes, allRes]: any[] = await Promise.all([
    listMyProjectCategoryStats({
      activityId: activeActivityId.value
    }),
    listMyProject({ activityId: activeActivityId.value, pageNum: 1, pageSize: 10000 })
  ]);

  const nextMap: Record<string, ProjectCategoryStatsVO> = {};
  (statsRes.data || []).forEach((item: ProjectCategoryStatsVO) => {
    if (item.categoryId !== undefined && item.categoryId !== null) {
      nextMap[String(item.categoryId)] = item;
    }
  });
  statsMap.value = nextMap;
  allProjectRows.value = allRes.rows || allRes.data || [];
  allTotal.value = Number(allRes.total || allProjectRows.value.length || 0);
};

const defaultActiveTabKey = () => {
  const configuredTab = categoryTabs.value.find(
    (tab) => tab.key === resolvedConfig.value.defaultTabKey || tab.sourceConfigKey === resolvedConfig.value.defaultTabKey
  );
  if (configuredTab) return configuredTab.key;
  const firstMatchedCategory = categoryTabs.value.find((tab) => tab.key !== 'all' && tab.categoryIds.length > 0);
  const firstCategory = categoryTabs.value.find((tab) => tab.key !== 'all');
  return firstMatchedCategory?.key || firstCategory?.key || categoryTabs.value[0]?.key || 'all';
};

const ensureActiveTab = () => {
  if (!categoryTabs.value.some((tab) => tab.key === activeTab.value)) {
    activeTab.value = defaultActiveTabKey();
  }
};

const ensureActiveStatus = () => {
  if (activeStatus.value && !visibleStatusStats.value.some((item) => item.key === activeStatus.value)) {
    activeStatus.value = '';
  }
};

const loadList = async () => {
  loading.value = true;
  try {
    ensureActiveTab();
    ensureActiveStatus();
    const params: Record<string, any> = { ...query };
    if (pageSizeSelection.value === 'all') {
      params.pageSize = 10000;
    }
    if (activeActivityId.value) {
      params.activityId = activeActivityId.value;
    }
    const queryCategoryIds = allCategoriesSelected.value ? [] : activeCategoryIds.value;
    if (!allCategoriesSelected.value && !queryCategoryIds.length) {
      projectList.value = [];
      total.value = 0;
      return;
    }
    if (queryCategoryIds.length) {
      params.categoryIds = queryCategoryIds.join(',');
    }
    if (activeStatus.value) {
      params.status = activeStatus.value;
    }
    if (expandedMode.value && projectNameKeyword.value.trim()) {
      params.projectName = projectNameKeyword.value.trim();
    }
    if (expandedMode.value && activeGroupCode.value) {
      params.groupCode = activeGroupCode.value;
    }
    const res: any = await listMyProject(params);
    projectList.value = res.rows || res.data || [];
    total.value = Number(res.total || projectList.value.length || 0);
    if (pageSizeSelection.value === 'all') {
      query.pageSize = Math.max(total.value, 1);
    }
  } finally {
    loading.value = false;
  }
};

const refresh = async () => {
  await loadSummaryData();
  await loadList();
};

const handleCategoryCommand = async (key: string | number | object) => {
  const tab = categoryTabMap.value.get(String(key));
  if (!tab) return;
  activeTab.value = tab.key;
  browseCategoryActive.value = expandedMode.value;
  browseCategoryIds.value = tab.categoryIds;
  selectedBrowseCategoryKey.value = tab.key === 'all' ? '' : String(tab.sourceId ?? '');
  query.pageNum = 1;
  await loadList();
};

const handleBrowseCategorySelection = async (selection: { key: string; categoryIds: Array<string | number> }) => {
  browseCategoryActive.value = true;
  browseCategoryIds.value = selection.categoryIds;
  selectedBrowseCategoryKey.value = selection.key;
  const matchedTab = selection.categoryIds.length
    ? categoryTabs.value.find((tab) => tab.key !== 'all' && String(tab.sourceId ?? '') === selection.key)
    : categoryAllTab.value;
  if (matchedTab) activeTab.value = matchedTab.key;
  query.pageNum = 1;
  await loadList();
};

const handleGroupSelection = async (groupCode: string) => {
  activeGroupCode.value = groupCode;
  query.pageNum = 1;
  await loadList();
};

const handleStatusCommand = async (key: string) => {
  activeStatus.value = key === 'all' || activeStatus.value === key ? '' : key;
  query.pageNum = 1;
  await loadList();
};

const refreshList = async () => {
  query.pageNum = 1;
  await loadList();
};

const resetFilters = async () => {
  projectNameKeyword.value = '';
  if (expandedMode.value) {
    browseCategoryActive.value = true;
    browseCategoryIds.value = [];
    selectedBrowseCategoryKey.value = '';
    activeTab.value = categoryAllTab.value?.key || 'all';
    activeGroupCode.value = '';
  }
  activeTab.value = categoryTabs.value.some((tab) => tab.key === 'all') ? 'all' : '';
  ensureActiveTab();
  activeStatus.value = '';
  query.pageNum = 1;
  await loadList();
};

const changePage = async (page: number) => {
  query.pageNum = Math.min(Math.max(page, 1), pageCount.value);
  await loadList();
};

const maxCustomPageSize = 50;
const normalizeCustomPageSize = (value: unknown) => Math.min(Math.max(Math.round(Number(value) || 1), 1), maxCustomPageSize);
const pageSizeForSelection = (selection: PageSizeSelection) => {
  if (selection === 'all') return Math.max(total.value, 1);
  if (selection === 'custom') return normalizeCustomPageSize(customPageSize.value);
  return Number(selection);
};

const applyPageSizeSelection = async (selection = pageSizeSelection.value) => {
  pageSizeSelection.value = selection;
  if (selection === 'custom') {
    customPageSize.value = normalizeCustomPageSize(customPageSize.value);
  }
  query.pageSize = pageSizeForSelection(selection);
  query.pageNum = 1;
  await loadList();
};

const handlePageSizeSelectionChange = async (value: string | number) => {
  const selection = String(value) as PageSizeSelection;
  if (['10', '15', '20', '50', 'all', 'custom'].includes(selection)) {
    await applyPageSizeSelection(selection);
  }
};

const handleCustomPageSizeChange = async () => {
  await applyPageSizeSelection('custom');
};

const handleMaximizePageSize = async (value: boolean) => {
  if (props.standalone) return;
  navigatorCollapsed.value = value ? !activeHeaderConfig.value.sidebarDefaultExpanded : false;
  if (value) {
    pageSizeSelectionBeforeMaximize.value = pageSizeSelection.value;
    customPageSizeBeforeMaximize.value = customPageSize.value;
    browseCategoryActive.value = true;
    browseCategoryIds.value = [];
    selectedBrowseCategoryKey.value = '';
    activeTab.value = categoryAllTab.value?.key || 'all';
    activeGroupCode.value = '';
    await applyPageSizeSelection('50');
  } else if (pageSizeSelectionBeforeMaximize.value) {
    browseCategoryActive.value = false;
    browseCategoryIds.value = [];
    selectedBrowseCategoryKey.value = '';
    activeGroupCode.value = '';
    customPageSize.value = customPageSizeBeforeMaximize.value;
    await applyPageSizeSelection(pageSizeSelectionBeforeMaximize.value);
  }
  pageSizeSelectionBeforeMaximize.value = undefined;
  await nextTick();
  tableRef.value?.doLayout();
};

const openProject = async (row: ProjectVO) => {
  if (row.id) {
    await router.push({ path: `/crehn/project/edit/${row.id}`, query: buildEditQuery(row) });
  }
};

watch(
  () => props.configJson,
  async () => {
    ensureActiveTab();
    ensureActiveStatus();
    loadLocalColumnWidths();
    await nextTick();
    tableRef.value?.doLayout();
  }
);

watch([columnWidthStorageKey, columnWidthConfigSignature], async () => {
  loadLocalColumnWidths();
  await nextTick();
  tableRef.value?.doLayout();
});

watch(tableShellWidth, async () => {
  await nextTick();
  tableRef.value?.doLayout();
});

watch(
  () => props.maximized,
  async (value) => {
    await handleMaximizePageSize(value);
  }
);

watch(
  () => props.standalone,
  async () => {
    await nextTick();
    tableRef.value?.doLayout();
  }
);

onMounted(async () => {
  await loadArtDetailDisplayConfig(true);
  navigatorCollapsed.value = expandedMode.value && !activeHeaderConfig.value.sidebarDefaultExpanded;
  loadLocalColumnWidths();
  setupTableResizeObserver();
  await loadCatalog();
  await loadList();
});
onActivated(refresh);
onBeforeUnmount(() => tableResizeObserver?.disconnect());
</script>

<style scoped lang="scss">
.school-project-list {
  height: min(620px, calc(100vh - var(--layout-header-height, 92px) - 48px));
  min-height: 440px;
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-sizing: border-box;
  box-shadow: none;
}

.school-project-list.is-standalone {
  height: 100%;
  min-height: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.school-project-submit-layout {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
}

.school-project-submit-layout.is-expanded {
  flex: 1;
  display: grid;
  grid-template-columns: minmax(260px, var(--navigator-sidebar-width, 340px)) minmax(0, 1fr);
  align-items: stretch;
  gap: var(--school-project-expanded-gap, 16px);
  min-height: 0;
}

.school-project-submit-layout.is-expanded.is-navigator-collapsed {
  grid-template-columns: minmax(0, 1fr);
  gap: 0;
}

.school-project-submit-layout.is-expanded :deep(.project-browse-navigator.is-compact-sidebar) {
  align-self: start;
  max-height: 100%;
}

.school-project-list.is-maximized :deep(.project-browse-navigator__category-scroll .el-tree-node__content) {
  height: 34px;
  margin: 1px 0;
}

.school-project-list.is-maximized :deep(.project-browse-navigator__node) {
  width: 0;
  flex: 1 1 auto;
  justify-content: space-between;
}

.school-project-list.is-maximized :deep(.project-browse-navigator__node em) {
  min-width: 2ch;
  margin-left: auto;
  padding-left: 8px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.school-project-group-filter {
  margin-bottom: 10px;
  padding-bottom: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  border-bottom: 1px solid #eef2f7;
}

.school-project-group-filter button {
  min-height: 30px;
  padding: 0 9px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #40566f;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
}

.school-project-group-filter button span {
  color: #94a3b8;
  font-size: 12px;
}

.school-project-group-filter button:hover,
.school-project-group-filter button.is-active {
  background: #eef5ff;
  color: #2563eb;
}

.school-project-group-filter button.is-active span {
  color: #2563eb;
}

.school-project-submit-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.school-project-header-controls {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: nowrap;
}

.school-project-header-controls :deep(.art-category-tree-dropdown),
.school-project-header-controls :deep(.el-select) {
  width: 180px;
}

.school-project-header-filter {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
}

.school-project-header-filter :deep(.art-category-tree-dropdown),
.school-project-header-filter :deep(.el-select) {
  width: 100%;
  min-width: 0;
}

.school-project-home-category,
.school-project-home-category .summary-pill {
  width: 100%;
  min-width: 0;
}

.school-project-home-category .summary-pill {
  justify-content: space-between;
}

.school-project-home-category .summary-pill__label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.school-project-filter-primary,
.school-project-filter-btn,
.school-project-filter-expand {
  height: 36px;
  border-radius: 8px;
  font-weight: 800;
}

.school-project-filter-expand {
  width: 40px;
  min-width: 40px;
  padding: 0;
  border: 0;
  background: #f4f7fb;
  color: #40566f;
}

.school-project-filter-expand:hover {
  background: #eef5ff;
  color: #2563eb;
}

.school-project-filter-primary {
  border: 0;
  color: #ffffff;
  background: #2563eb;
}

.school-project-filter-primary:hover {
  color: #ffffff;
  background: #1d4ed8;
}

.school-project-filter-btn {
  border: 0;
  color: #40566f;
  background: #f4f7fb;
}

.school-project-filter-btn:hover {
  color: #2563eb;
  background: #eef5ff;
}

.school-project-activity-select {
  width: 240px;
}

.school-project-name-search {
  width: 260px;
}

.school-project-list.is-maximized {
  height: 100%;
  min-height: 0;
}

.school-project-commandbar {
  margin-bottom: 12px;
  padding: 4px 0 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.school-project-commandbar__left,
.school-project-commandbar__right,
.school-project-pager,
.school-project-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.school-project-commandbar__left {
  min-width: 0;
  flex: 1;
  flex-wrap: wrap;
  gap: 4px;
}

.school-project-commandbar__right {
  justify-content: flex-end;
  flex-wrap: wrap;
}

.school-project-commandbar.is-pagination-only {
  justify-content: flex-end;
}

.school-project-table-shell {
  flex: 1;
  min-width: 0;
  min-height: 220px;
  overflow: hidden;
}

.school-project-table-shell :deep(.el-table__fixed-right),
.school-project-table-shell :deep(.el-table-fixed-column--right) {
  box-shadow: none !important;
}

.school-project-column-reset {
  width: 32px;
  min-width: 32px;
  height: 32px;
  padding: 0;
  color: #64748b;
  font-weight: 700;
}

.school-project-column-reset:not(.is-disabled):hover {
  color: #2563eb;
  background: #eef5ff;
}

.school-project-list :deep(.el-table td.el-table__cell .cell) {
  line-height: 1.45;
}

.school-project-select-btn {
  min-width: 72px;
  height: var(--art-workspace-button-height, 32px);
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-color: var(--art-button-border-color, transparent);
  border-width: var(--art-button-border-width, 0);
  border-radius: var(--art-button-border-radius, 8px);
  background: var(--art-button-background-color, transparent);
  box-shadow: none;
  color: var(--art-button-text-color, #40566f);
  font-size: 14px;
  font-weight: var(--art-workspace-button-font-weight, 400);
  line-height: 1;
}

.school-project-select-btn:not(.is-disabled):hover,
.school-project-select-btn.is-selected {
  border-color: var(--art-button-hover-border-color, transparent);
  background: var(--art-button-hover-background-color, #eef5ff);
  color: var(--art-button-hover-text-color, #2563eb);
}

.school-project-select-btn :deep(.el-icon) {
  color: #7890aa;
}

.school-project-select-btn:not(.is-disabled):hover :deep(.el-icon),
.school-project-select-btn.is-selected :deep(.el-icon) {
  color: #2563eb;
}

.summary-pill {
  min-width: 0;
  height: 36px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 0;
  border-radius: 8px;
  background: #ffffff;
  color: #40566f;
  font-family: inherit;
  line-height: 1;
}

.summary-pill span,
.summary-pill strong {
  display: inline-flex;
  align-items: center;
}

.summary-pill span {
  font-size: 14px;
  font-weight: 800;
}

.summary-pill strong {
  color: #2563eb;
  font-size: 18px;
  line-height: 1;
}

.summary-pill--category {
  min-width: 82px;
  padding: 0 8px 0 12px;
  cursor: pointer;
  appearance: none;
  border: 0;
  background: #ffffff;
  color: #1f3b64;
  box-shadow: none;
}

.summary-pill--category:hover,
.summary-pill--category:focus-visible {
  border: 0;
  background: #eef5ff;
  color: #2563eb;
  outline: none;
}

.summary-pill--category .summary-pill__label {
  color: #0f2f5f;
  font-weight: 900;
}

.summary-pill__arrow {
  width: 18px;
  margin-left: 0;
  color: #2563eb;
  font-size: 16px;
  transition: transform 0.16s ease;
}

.summary-pill--category:hover .summary-pill__arrow,
.summary-pill--category:focus-visible .summary-pill__arrow {
  transform: translateY(1px);
}

:global(.school-project-category-menu .el-dropdown-menu__item.is-active) {
  color: #2563eb;
  font-weight: 900;
}

:global(.school-project-category-menu) {
  max-height: min(68vh, 620px);
  overflow-y: auto;
}

:global(.school-project-category-menu .el-dropdown-menu__item.is-category-group) {
  color: #1f3b64;
  font-weight: 900;
}

:global(.category-menu-item) {
  width: 230px;
  min-width: 0;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

:global(.category-menu-item > span) {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.category-menu-item--child) {
  padding-left: 26px;
  box-sizing: border-box;
}

:global(.category-menu-toggle) {
  width: 20px;
  min-width: 20px;
  height: 24px;
  margin-left: -4px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #7890aa;
  cursor: pointer;
}

:global(.category-menu-toggle:hover) {
  background: #eef5ff;
  color: #2563eb;
}

:global(.category-menu-toggle .el-icon) {
  transition: transform 0.16s ease;
}

:global(.category-menu-toggle .el-icon.is-expanded) {
  transform: rotate(90deg);
}

:global(.category-menu-item em) {
  min-width: 22px;
  height: 20px;
  padding: 0 7px;
  border-radius: 999px;
  background: #e7f0ff;
  color: #2563eb;
  font-size: 12px;
  font-style: normal;
  line-height: 20px;
  text-align: center;
}

.school-project-submit-all {
  min-width: 112px;
  height: var(--art-workspace-button-height, 32px);
  padding: 0 16px;
  border-color: var(--art-button-border-color, transparent);
  border-width: var(--art-button-border-width, 0);
  border-radius: var(--art-button-border-radius, 8px);
  background: var(--art-button-background-color, #2563eb);
  box-shadow: none;
  color: var(--art-button-text-color, #ffffff);
  font-weight: var(--art-workspace-button-font-weight, 400);
}

.school-project-submit-all:not(.is-disabled):hover {
  border-color: var(--art-button-hover-border-color, transparent);
  background: var(--art-button-hover-background-color, #1d4ed8);
  color: var(--art-button-hover-text-color, #ffffff);
}

.school-project-submit-all.is-disabled {
  border: 0;
  background: #edf4ff;
  color: #8aa4c2;
}

.school-project-audit-progress {
  display: grid;
  flex: 0 0 auto;
  gap: 6px;
  max-width: min(100%, 480px);
}

.school-project-audit-progress__text {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: inherit;
  font-size: inherit;
  line-height: 1.2;
  white-space: nowrap;
}

.school-project-audit-progress__text strong {
  color: inherit;
  font-weight: 700;
}

.school-project-audit-progress :deep(.el-progress-bar__outer) {
  background-color: var(--school-audit-progress-track, #e5e7eb);
}

.school-project-fullscreen {
  min-width: 86px;
  height: 36px;
  border: 0;
  background: transparent;
  color: #1f3b64;
  font-weight: 800;
}

.school-project-fullscreen:hover {
  border: 0;
  background: #eef5ff;
  color: #2563eb;
}

.school-project-pager span {
  min-width: 56px;
  color: #102a43;
  text-align: center;
  font-weight: 800;
}

.school-project-page-btn {
  min-width: 72px;
  height: 36px;
  border-radius: 8px;
  border: 0;
  background: transparent;
  color: #1f3b64;
  font-weight: 800;
}

.school-project-page-btn:not(.is-disabled):hover {
  border: 0;
  background: #eef5ff;
  color: #2563eb;
}

.school-project-actions {
  justify-content: flex-start;
  flex-wrap: nowrap;
  gap: 8px;
}

.school-project-actions .el-button {
  margin-left: 0;
  padding: 0 2px;
  border: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
  font-weight: 800;
  white-space: nowrap;
}

.school-project-action-tooltip {
  display: inline-flex;
}

.school-project-actions .school-project-action.is-action-disabled {
  cursor: not-allowed;
}

.school-project-page-size {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}

.school-project-pagination-footer {
  min-height: 48px;
  padding-top: 10px;
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  border-top: 1px solid #eef2f7;
}

.school-project-page-size :deep(.el-select) {
  width: 84px;
}

.school-project-page-size :deep(.el-select__wrapper) {
  min-height: 32px;
  padding: 0 8px;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.school-project-custom-page-size {
  width: 90px;
}

.school-project-custom-page-size :deep(.el-input__wrapper) {
  box-shadow: none;
}

.school-project-list :deep(.el-table) {
  --el-table-border-color: #eef2f7;
  --el-table-header-bg-color: #f8fafc;
  --el-table-row-hover-bg-color: #f6f9fd;
}

.school-project-list :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.school-project-list :deep(.el-table th.el-table__cell) {
  border-right: 0;
  border-bottom-color: #e8edf5;
  background: #f8fafc;
}

.school-project-list :deep(.el-table td.el-table__cell) {
  border-right: 0;
  border-bottom-color: #eef2f7;
}

.school-project-list :deep(.el-table th.school-project-resizable-header .cell) {
  position: relative;
  padding-right: 14px;
}

.school-project-list :deep(.el-table th.school-project-resizable-header .cell::after) {
  position: absolute;
  top: 50%;
  right: -5px;
  bottom: auto;
  width: 10px;
  height: 18px;
  border-right: 1px solid rgb(148 163 184 / 42%);
  content: '';
  pointer-events: none;
  transform: translateY(-50%);
  transition:
    top 0.16s ease,
    bottom 0.16s ease,
    height 0.16s ease,
    border-color 0.16s ease;
}

.school-project-list :deep(.el-table th.school-project-resizable-header:hover .cell::after) {
  top: 5px;
  bottom: 5px;
  height: auto;
  border-right: 2px solid #3b82f6;
  transform: none;
}

.school-project-list :deep(.el-table__column-resize-proxy) {
  border-left: 2px solid #2563eb;
}

@media (max-width: 900px) {
  .school-project-list:not(.is-maximized):not(.is-standalone) {
    height: auto;
    min-height: 0;
    overflow: visible;
  }

  .school-project-submit-layout.is-expanded {
    grid-template-columns: 1fr;
  }

  .school-project-title {
    padding: 12px;
  }

  .school-project-title__back {
    position: static;
    margin-top: 8px;
    transform: none;
  }

  .school-project-header-controls {
    align-items: stretch;
    flex-wrap: wrap;
  }

  .school-project-activity-select,
  .school-project-name-search {
    width: min(100%, 320px);
  }

  .school-project-commandbar {
    align-items: stretch;
    flex-direction: column;
  }

  .school-project-commandbar__left,
  .school-project-commandbar__right,
  .school-project-pager {
    justify-content: space-between;
  }

  .school-project-page-size {
    flex-wrap: wrap;
  }

  .school-project-submit-all {
    flex: 1;
  }
}
</style>
