<template>
  <section
    v-if="isTopbar"
    class="project-browse-navigator project-browse-navigator--top"
    :class="navigatorClasses"
    :style="navigatorCss"
    aria-label="项目浏览筛选"
  >
    <div
      v-if="showsActivityField"
      class="project-browse-navigator__top-field project-browse-navigator__top-field--activity"
      :class="{ 'is-title-display': usesActivityTitle }"
    >
      <span v-if="!usesActivityTitle">{{ pageConfig.labels.activity }}</span>
      <div v-if="usesActivityTitle" class="project-browse-navigator__activity-display" :class="activityDisplayClasses" :style="activityDisplayStyle">
        <strong :title="activityDisplayText">{{ activityDisplayText }}</strong>
        <el-popover v-if="activities.length > 1" placement="bottom-start" :width="360" trigger="click">
          <template #reference
            ><el-button link type="primary">{{ activityTitleConfig.switchText }}</el-button></template
          >
          <div class="project-browse-navigator__activity-options">
            <button
              v-for="item in activities"
              :key="String(item.id)"
              type="button"
              :class="{ 'is-active': String(item.id) === String(activityId) }"
              @click="emit('update:activityId', item.id!)"
            >
              {{ activityOptionLabel(item) }}
            </button>
          </div>
        </el-popover>
      </div>
      <el-select v-else :model-value="activityId" :disabled="activities.length <= 1" @update:model-value="emit('update:activityId', $event)">
        <el-option v-for="item in activities" :key="String(item.id)" :label="activityOptionLabel(item)" :value="item.id!" />
      </el-select>
    </div>
    <slot v-if="$slots['top-category']" name="top-category" :page-config="pageConfig"></slot>
    <template v-else>
      <div v-if="isFieldVisible('category')" class="project-browse-navigator__top-field project-browse-navigator__top-field--category">
        <span>{{ pageConfig.labels.category }}</span>
        <div class="project-browse-navigator__top-options">
          <button type="button" :class="{ 'is-active': !selectedCategoryKey }" @click="selectAll">全部</button>
          <button
            v-for="item in categoryTree"
            :key="String(item.id)"
            type="button"
            :class="{ 'is-active': isMajorSelected(item) }"
            @click="selectCategory(item)"
          >
            {{ item.categoryName || item.id }}<em>（{{ nodeCount(item) }}）</em>
          </button>
        </div>
      </div>
      <div
        v-if="isFieldVisible('category') && selectedMajorCategory?.children?.length"
        class="project-browse-navigator__top-field project-browse-navigator__top-field--category"
      >
        <span>子类</span>
        <div class="project-browse-navigator__top-options">
          <button
            type="button"
            :class="{ 'is-active': String(selectedMajorCategory.id) === String(selectedCategoryKey) }"
            @click="selectCategory(selectedMajorCategory)"
          >
            全部子类
          </button>
          <button
            v-for="item in selectedMajorCategory.children"
            :key="String(item.id)"
            type="button"
            :class="{ 'is-active': String(item.id) === String(selectedCategoryKey) }"
            @click="selectCategory(item)"
          >
            {{ item.categoryName || item.id }}<em>（{{ nodeCount(item) }}）</em>
          </button>
        </div>
      </div>
      <div v-for="filter in visibleSecondaryFilters" :key="filter.key" class="project-browse-navigator__top-field">
        <span>{{ secondaryFilterLabel(filter) }}</span>
        <div class="project-browse-navigator__top-options">
          <button type="button" :class="{ 'is-active': !secondaryFilterValues[filter.key] }" @click="updateSecondaryFilter(filter.key, undefined)">
            全部
          </button>
          <button
            v-for="option in filter.options"
            :key="String(option.value)"
            type="button"
            :class="{ 'is-active': String(secondaryFilterValues[filter.key] || '') === String(option.value) }"
            @click="updateSecondaryFilter(filter.key, option.value)"
          >
            {{ option.label }}<em v-if="option.count !== undefined">（{{ option.count }}）</em>
          </button>
        </div>
      </div>
    </template>
    <div v-if="showUnitField" class="project-browse-navigator__top-field project-browse-navigator__top-field--school">
      <span>{{ pageConfig.labels.school }}</span>
      <el-select :model-value="selectedUnitId" clearable filterable :placeholder="`搜索${pageConfig.labels.school}`" @update:model-value="selectUnit">
        <el-option v-for="item in unitOptions" :key="String(item.id)" :label="item.label" :value="item.id" />
      </el-select>
    </div>
  </section>

  <el-button
    v-else-if="isCollapsed"
    class="project-browse-navigator__collapsed-trigger"
    :style="navigatorCss"
    text
    icon="Filter"
    title="展开筛选"
    aria-label="展开筛选"
    @click="emit('update:collapsed', false)"
  />

  <aside
    v-else-if="compactSidebar"
    class="project-browse-navigator is-compact-sidebar"
    :class="navigatorClasses"
    :style="navigatorCss"
    aria-label="项目筛选"
  >
    <div class="project-browse-navigator__compact-toolbar">
      <strong>{{ sidebarTitle }}</strong>
      <div class="project-browse-navigator__compact-actions">
        <el-button
          v-if="collapsible"
          class="project-browse-navigator__collapse"
          text
          circle
          icon="Fold"
          title="收起筛选"
          aria-label="收起筛选"
          @click="emit('update:collapsed', true)"
        />
      </div>
    </div>

    <div v-if="showsActivityField" class="project-browse-navigator__compact-section">
      <span v-if="!usesActivityTitle" class="project-browse-navigator__section-label">{{ pageConfig.labels.activity }}</span>
      <div v-if="usesActivityTitle" class="project-browse-navigator__activity-display" :class="activityDisplayClasses" :style="activityDisplayStyle">
        <strong :title="activityDisplayText">{{ activityDisplayText }}</strong>
        <el-popover v-if="activities.length > 1" placement="bottom-start" :width="360" trigger="click">
          <template #reference
            ><el-button link type="primary">{{ activityTitleConfig.switchText }}</el-button></template
          >
          <div class="project-browse-navigator__activity-options">
            <button
              v-for="item in activities"
              :key="String(item.id)"
              type="button"
              :class="{ 'is-active': String(item.id) === String(activityId) }"
              @click="emit('update:activityId', item.id!)"
            >
              {{ activityOptionLabel(item) }}
            </button>
          </div>
        </el-popover>
      </div>
      <el-select
        v-else
        :model-value="activityId"
        class="project-browse-navigator__compact-activity"
        :disabled="activities.length <= 1"
        :teleported="false"
        @update:model-value="emit('update:activityId', $event)"
      >
        <el-option v-for="item in activities" :key="String(item.id)" :label="activityOptionLabel(item)" :value="item.id!" />
      </el-select>
    </div>

    <div v-if="showUnitField" class="project-browse-navigator__compact-mode" role="tablist" aria-label="浏览方式">
      <button
        type="button"
        role="tab"
        :aria-selected="innerBrowseMode === 'category'"
        :class="{ 'is-active': innerBrowseMode === 'category' }"
        @click="innerBrowseMode = 'category'"
      >
        按类别查看
      </button>
      <button
        type="button"
        role="tab"
        :aria-selected="innerBrowseMode === 'school'"
        :class="{ 'is-active': innerBrowseMode === 'school' }"
        @click="innerBrowseMode = 'school'"
      >
        按学校查看
      </button>
    </div>

    <div v-if="$slots['category-prefix'] && (!showUnitField || innerBrowseMode === 'category')" class="project-browse-navigator__compact-section">
      <span class="project-browse-navigator__section-label">组别</span>
      <slot name="category-prefix"></slot>
    </div>

    <div
      v-if="showUnitField && innerBrowseMode === 'school'"
      class="project-browse-navigator__compact-section project-browse-navigator__compact-units"
    >
      <div class="project-browse-navigator__section-heading">
        <span class="project-browse-navigator__section-label">{{ pageConfig.labels.school }}</span>
        <button type="button" :class="{ 'is-active': !hasSelectedUnit }" @click="selectUnit()">全部学校</button>
      </div>
      <el-input
        v-if="unitSearchable"
        v-model="unitKeyword"
        class="project-browse-navigator__unit-search"
        clearable
        prefix-icon="Search"
        placeholder="搜索学校名称或代码"
      />
      <div class="project-browse-navigator__unit-list">
        <button
          v-for="item in visibleUnitOptions"
          :key="String(item.id)"
          type="button"
          class="project-browse-navigator__unit"
          :class="{ 'is-active': String(item.id) === String(selectedUnitId || '') }"
          @click="emit('select-unit', item.id)"
        >
          <span :title="item.label">{{ item.label }}</span
          ><em>{{ item.count || 0 }}</em>
        </button>
        <el-empty v-if="!visibleUnitOptions.length" :image-size="48" :description="unitKeyword ? '暂无匹配学校' : '暂无学校数据'" />
      </div>
    </div>

    <div
      v-if="isFieldVisible('category') && (!showUnitField || innerBrowseMode === 'category' || hasSelectedUnit)"
      class="project-browse-navigator__compact-section project-browse-navigator__compact-categories"
    >
      <div class="project-browse-navigator__section-heading">
        <span class="project-browse-navigator__section-label">{{ innerBrowseMode === 'school' ? '类别' : pageConfig.labels.category }}</span>
        <button type="button" :class="{ 'is-active': !selectedCategoryKey }" @click="selectAllCategories">全部类别</button>
      </div>
      <div class="project-browse-navigator__category-scroll">
        <el-tree
          v-if="categoryTree.length"
          ref="categoryTreeRef"
          node-key="id"
          :data="categoryTree"
          :props="{ label: 'categoryName', children: 'children' }"
          :current-node-key="selectedCategoryKey"
          highlight-current
          default-expand-all
          @node-click="selectCategory"
        >
          <template #default="{ data }">
            <span class="project-browse-navigator__node">
              <span :title="String(data.categoryName || data.id)">{{ data.categoryName || data.id }}</span>
              <em>{{ nodeCount(data) }}</em>
            </span>
          </template>
        </el-tree>
        <el-empty v-else :image-size="48" :description="emptyCategoryText" />
      </div>
    </div>

    <el-empty
      v-if="showUnitField && innerBrowseMode === 'school' && !hasSelectedUnit"
      class="project-browse-navigator__school-hint"
      :image-size="42"
      description="请选择学校后查看类别"
    />

    <div v-if="visibleSecondaryFilters.length" class="project-browse-navigator__secondary">
      <el-select
        v-for="filter in visibleSecondaryFilters"
        :key="filter.key"
        :model-value="secondaryFilterValues[filter.key]"
        clearable
        filterable
        :placeholder="`全部${filter.label}`"
        @update:model-value="updateSecondaryFilter(filter.key, $event)"
      >
        <el-option
          v-for="option in filter.options"
          :key="String(option.value)"
          :label="option.count === undefined ? option.label : `${option.label}（${option.count}）`"
          :value="option.value"
        />
      </el-select>
    </div>
  </aside>

  <aside v-else class="project-browse-navigator" :class="navigatorClasses" :style="navigatorCss" aria-label="项目浏览导航">
    <div class="project-browse-navigator__heading">
      <div
        v-if="showsActivityField && usesActivityTitle"
        class="project-browse-navigator__activity-display"
        :class="activityDisplayClasses"
        :style="activityDisplayStyle"
      >
        <strong :title="activityDisplayText">{{ activityDisplayText }}</strong>
        <el-popover v-if="activities.length > 1" placement="bottom-start" :width="360" trigger="click">
          <template #reference
            ><el-button link type="primary">{{ activityTitleConfig.switchText }}</el-button></template
          >
          <div class="project-browse-navigator__activity-options">
            <button
              v-for="item in activities"
              :key="String(item.id)"
              type="button"
              :class="{ 'is-active': String(item.id) === String(activityId) }"
              @click="emit('update:activityId', item.id!)"
            >
              {{ activityOptionLabel(item) }}
            </button>
          </div>
        </el-popover>
      </div>
      <el-select
        v-else-if="showsActivityField"
        :model-value="activityId"
        class="project-browse-navigator__activity-select"
        :disabled="activities.length <= 1"
        :teleported="false"
        @update:model-value="emit('update:activityId', $event)"
      >
        <el-option v-for="item in activities" :key="String(item.id)" :label="activityOptionLabel(item)" :value="item.id!" />
      </el-select>
    </div>

    <el-tabs v-model="innerBrowseMode" stretch class="project-browse-navigator__tabs">
      <el-tab-pane v-if="isFieldVisible('category')" label="按类别查看" name="category">
        <slot name="category-prefix"></slot>
        <el-scrollbar max-height="min(64vh, 620px)">
          <el-tree
            v-if="categoryTree.length"
            ref="categoryTreeRef"
            node-key="id"
            :data="categoryTree"
            :props="{ label: 'categoryName', children: 'children' }"
            :current-node-key="selectedCategoryKey"
            highlight-current
            accordion
            @node-click="selectCategory"
          >
            <template #default="{ data }">
              <span class="project-browse-navigator__node">
                <span :title="String(data.categoryName || data.id)">{{ data.categoryName || data.id }}</span>
                <em>（{{ nodeCount(data) }}）</em>
              </span>
            </template>
          </el-tree>
          <el-empty v-else :image-size="56" :description="emptyCategoryText" />
        </el-scrollbar>
      </el-tab-pane>
      <el-tab-pane v-if="showUnitField" label="按单位查看" name="school">
        <el-input
          v-if="unitSearchable"
          v-model="unitKeyword"
          class="project-browse-navigator__unit-search"
          clearable
          prefix-icon="Search"
          placeholder="搜索单位名称或代码"
        />
        <el-scrollbar max-height="min(58vh, 560px)">
          <button
            v-for="item in visibleUnitOptions"
            :key="String(item.id)"
            type="button"
            class="project-browse-navigator__unit"
            :class="{ 'is-active': String(item.id) === String(selectedUnitId || '') }"
            @click="emit('select-unit', item.id)"
          >
            <span :title="item.label">{{ item.label }}</span
            ><em>（{{ item.count || 0 }}）</em>
          </button>
          <el-empty v-if="!visibleUnitOptions.length" :image-size="48" :description="unitKeyword ? '暂无匹配单位' : '暂无单位数据'" />
        </el-scrollbar>
      </el-tab-pane>
    </el-tabs>

    <div v-if="visibleSecondaryFilters.length" class="project-browse-navigator__secondary">
      <el-select
        v-for="filter in visibleSecondaryFilters"
        :key="filter.key"
        :model-value="secondaryFilterValues[filter.key]"
        clearable
        filterable
        :placeholder="`全部${filter.label}`"
        @update:model-value="updateSecondaryFilter(filter.key, $event)"
      >
        <el-option
          v-for="option in filter.options"
          :key="String(option.value)"
          :label="option.count === undefined ? option.label : `${option.label}（${option.count}）`"
          :value="option.value"
        />
      </el-select>
    </div>

    <el-button v-if="showAllButton" class="project-browse-navigator__all" plain @click="selectAll">{{ allLabel }}</el-button>
  </aside>
</template>

<script setup lang="ts">
import type { ArtBrowseNavigatorFieldKey, ArtBrowseNavigatorPageKey } from '@/api/crehn/detailDisplay';
import type { ActivityCategoryVO, ActivityVO } from '@/api/crehn/types';
import { useUserStore } from '@/store/modules/user';
import { buildSchoolCategoryMenuTree, type ActivityCategoryTreeNode } from '@/utils/artCategory';
import { loadArtDetailDisplayConfig, useArtDetailDisplayConfig } from './artDetailDisplayConfig';

type CountValue = number | { totalCount?: number; count?: number };
type UnitOption = { id: string | number; label: string; count?: number; keywords?: string[] };
type CategorySelection = { key: string; categoryIds: Array<string | number>; label: string };
type SecondaryFilter = { key: string; label: string; options: Array<{ label: string; value: string; count?: number }> };

const props = withDefaults(
  defineProps<{
    activities: ActivityVO[];
    activityId?: string | number;
    pageKey?: ArtBrowseNavigatorPageKey;
    categories: ActivityCategoryVO[];
    categoryCountMap?: Record<string, CountValue>;
    selectedCategoryKey?: string;
    browseMode?: 'category' | 'school';
    showUnitBrowse?: boolean;
    showActivity?: boolean;
    unitOptions?: UnitOption[];
    selectedUnitId?: string | number;
    unitSearchable?: boolean;
    showAllButton?: boolean;
    allLabel?: string;
    emptyCategoryText?: string;
    secondaryFilters?: SecondaryFilter[];
    secondaryFilterValues?: Record<string, string | undefined>;
    compactSidebar?: boolean;
    forceSidebar?: boolean;
    forceTop?: boolean;
    collapsible?: boolean;
    collapsed?: boolean;
    sidebarTitle?: string;
    activityTitleVariables?: Record<string, string | number | undefined>;
  }>(),
  {
    categoryCountMap: () => ({}),
    pageKey: 'projectView',
    selectedCategoryKey: '',
    browseMode: 'category',
    showUnitBrowse: true,
    showActivity: true,
    unitOptions: () => [],
    unitSearchable: true,
    showAllButton: true,
    allLabel: '所有上报',
    emptyCategoryText: '暂无可用类别',
    secondaryFilters: () => [],
    secondaryFilterValues: () => ({}),
    compactSidebar: false,
    forceSidebar: false,
    forceTop: false,
    collapsible: false,
    collapsed: false,
    sidebarTitle: '筛选项目',
    activityTitleVariables: () => ({})
  }
);

const emit = defineEmits<{
  (event: 'update:activityId', value: string | number): void;
  (event: 'update:browseMode', value: 'category' | 'school'): void;
  (event: 'update:secondaryFilterValues', value: Record<string, string | undefined>): void;
  (event: 'select-category', value: CategorySelection): void;
  (event: 'select-unit', value?: string | number): void;
  (event: 'select-all'): void;
  (event: 'update:collapsed', value: boolean): void;
}>();

const { detailDisplayConfig } = useArtDetailDisplayConfig();
const userStore = useUserStore();
const innerBrowseMode = ref<'category' | 'school'>(props.browseMode);
const unitKeyword = ref('');
const categoryTreeRef = ref<any>();
const categoryTree = computed(() => buildSchoolCategoryMenuTree(props.categories));
const navigatorStyle = computed(() => detailDisplayConfig.value.navigator);
const pageConfig = computed(() => navigatorStyle.value.pages[props.pageKey]);
const isReviewPage = computed(() => props.pageKey === 'review');
const isAuditPage = computed(() => props.pageKey === 'audit');
const reviewTitleConfig = computed(() => detailDisplayConfig.value.reviewWorkbench.activityTitle);
const activityTitleConfig = computed(() =>
  isAuditPage.value && pageConfig.value.activityTitle ? pageConfig.value.activityTitle : reviewTitleConfig.value
);
const showsActivityField = computed(
  () =>
    props.showActivity &&
    (isAuditPage.value ? pageConfig.value.activityTitle?.displayMode !== 'hidden' : pageConfig.value.fields.includes('activity'))
);
const usesActivityTitle = computed(() => isReviewPage.value || isAuditPage.value);
const currentActivity = computed(() => props.activities.find((item) => String(item.id) === String(props.activityId)));
const activityOptionLabel = (item: ActivityVO) => String(item.activityName || '').trim() || '活动名称未配置';
const replaceTemplateVariables = (template: string, values: Record<string, string | number | undefined>) =>
  template.replace(/\{([A-Za-z][A-Za-z0-9]*)\}/g, (_match, key: string) => String(values[key] ?? ''));
const activityDisplayText = computed(() => {
  const values = {
    activityName: currentActivity.value?.activityName || '',
    activityYear: currentActivity.value?.year || '',
    activityEdition: currentActivity.value?.edition || '',
    reviewerName: userStore.nickname || '',
    ...props.activityTitleVariables
  };
  return replaceTemplateVariables(activityTitleConfig.value.template, values).trim() || (currentActivity.value ? '活动名称未配置' : '请选择活动');
});
const activityDisplayClasses = computed(() => ({
  'is-centered': activityTitleConfig.value.align === 'center',
  'is-right': activityTitleConfig.value.align === 'right',
  'is-nowrap': !activityTitleConfig.value.allowWrap
}));
const activityDisplayStyle = computed(() => ({
  '--review-activity-font-size': `${activityTitleConfig.value.fontSize}px`,
  '--review-activity-color': activityTitleConfig.value.color,
  '--review-activity-font-weight':
    activityTitleConfig.value.fontWeight === 'normal' ? '400' : activityTitleConfig.value.fontWeight === 'medium' ? '500' : '700'
}));
const isTopbar = computed(() => props.forceTop || (!props.forceSidebar && pageConfig.value.layout === 'top'));
const isCollapsed = computed(() => !isTopbar.value && props.compactSidebar && props.collapsible && props.collapsed);
const isFieldVisible = (key: ArtBrowseNavigatorFieldKey) => pageConfig.value.fields.includes(key);
const showUnitField = computed(() => props.showUnitBrowse && isFieldVisible('school'));
const hasSelectedUnit = computed(() => props.selectedUnitId !== undefined && props.selectedUnitId !== null && props.selectedUnitId !== '');
const visibleSecondaryFilters = computed(() => props.secondaryFilters.filter((filter) => isFieldVisible(filter.key as ArtBrowseNavigatorFieldKey)));
const secondaryFilterLabel = (filter: SecondaryFilter) => pageConfig.value.labels[filter.key as ArtBrowseNavigatorFieldKey] || filter.label;
const navigatorClasses = computed(() => ({
  'is-borderless': !navigatorStyle.value.borderVisible,
  'is-shadowless': !navigatorStyle.value.shadowVisible,
  'has-dividers': navigatorStyle.value.dividerVisible
}));
const navigatorCss = computed(() => ({
  '--navigator-title-size': `${navigatorStyle.value.titleFontSize}px`,
  '--navigator-item-size': `${navigatorStyle.value.itemFontSize}px`,
  '--navigator-count-size': `${navigatorStyle.value.countFontSize}px`,
  '--navigator-row-height': `${navigatorStyle.value.rowHeight}px`,
  '--navigator-item-gap': `${navigatorStyle.value.itemGap}px`,
  '--navigator-panel-padding': `${navigatorStyle.value.panelPadding}px`,
  '--navigator-background': navigatorStyle.value.backgroundColor,
  '--navigator-background-opacity': `${navigatorStyle.value.backgroundOpacity}%`,
  '--navigator-text-color': navigatorStyle.value.textColor,
  '--navigator-active-color': navigatorStyle.value.activeColor,
  '--navigator-hover-background': `color-mix(in srgb, ${navigatorStyle.value.hoverBackground} ${navigatorStyle.value.hoverBackgroundOpacity}%, transparent)`,
  '--navigator-selected-background': `color-mix(in srgb, ${navigatorStyle.value.selectedBackground} ${navigatorStyle.value.selectedBackgroundOpacity}%, transparent)`,
  '--navigator-selected-text-color': navigatorStyle.value.selectedTextColor,
  '--navigator-border-color': navigatorStyle.value.borderColor,
  '--navigator-divider-color': `color-mix(in srgb, ${navigatorStyle.value.dividerColor} ${navigatorStyle.value.dividerOpacity}%, transparent)`,
  '--navigator-divider-width': `${navigatorStyle.value.dividerWidth}px`,
  '--navigator-sidebar-width': `${navigatorStyle.value.sidebarWidth}px`,
  '--navigator-sidebar-max-height': `${navigatorStyle.value.sidebarMaxHeight}px`,
  '--navigator-top-height': `${navigatorStyle.value.topHeight}px`
}));
const visibleUnitOptions = computed(() => {
  const keyword = unitKeyword.value.trim().toLowerCase();
  if (!keyword) return props.unitOptions;
  return props.unitOptions.filter((item) =>
    [item.label, ...(item.keywords || [])].some((text) =>
      String(text || '')
        .toLowerCase()
        .includes(keyword)
    )
  );
});

watch(
  () => props.browseMode,
  (value) => {
    innerBrowseMode.value = value;
  }
);
watch(innerBrowseMode, (value) => emit('update:browseMode', value));
watch(
  () => props.selectedCategoryKey,
  (value) => nextTick(() => categoryTreeRef.value?.setCurrentKey?.(value || undefined)),
  { immediate: true }
);
onMounted(() => void loadArtDetailDisplayConfig());

const leafCategoryIds = (node: ActivityCategoryTreeNode): Array<string | number> => {
  if (!node.children?.length) return node.id === undefined || node.id === null || node.virtualGroup ? [] : [node.id];
  return node.children.flatMap(leafCategoryIds);
};
const countOf = (value?: CountValue) => (typeof value === 'number' ? value : Number(value?.totalCount ?? value?.count ?? 0));
const nodeCount = (node: ActivityCategoryTreeNode): number =>
  leafCategoryIds(node).reduce<number>((sum, id) => sum + countOf(props.categoryCountMap[String(id)]), 0);
const selectCategory = (node: ActivityCategoryTreeNode) => {
  const categoryIds = leafCategoryIds(node);
  if (categoryIds.length) emit('select-category', { key: String(node.id), categoryIds, label: String(node.categoryName || node.id) });
};
const selectUnit = (value?: string | number) => emit('select-unit', value === '' || value === null ? undefined : value);
const containsNode = (node: ActivityCategoryTreeNode, key: string): boolean =>
  String(node.id) === key || Boolean(node.children?.some((child) => containsNode(child, key)));
const selectedMajorCategory = computed(() => categoryTree.value.find((node) => containsNode(node, String(props.selectedCategoryKey || ''))));
const isMajorSelected = (node: ActivityCategoryTreeNode) => containsNode(node, String(props.selectedCategoryKey || ''));
const selectAll = () => {
  emit('select-category', { key: '', categoryIds: [], label: '全部类别' });
  emit('select-all');
};
const selectAllCategories = () => emit('select-category', { key: '', categoryIds: [], label: '全部类别' });
const updateSecondaryFilter = (key: string, value: string | undefined) =>
  emit('update:secondaryFilterValues', { ...props.secondaryFilterValues, [key]: value || undefined });
</script>

<style scoped lang="scss">
.project-browse-navigator {
  min-width: 0;
  padding: var(--navigator-panel-padding, 12px);
  color: var(--navigator-text-color);
  border: 1px solid var(--navigator-border-color);
  border-radius: var(--app-radius-md, var(--el-border-radius-base, 8px));
  background: color-mix(in srgb, var(--navigator-background) var(--navigator-background-opacity), transparent);
  box-shadow: 0 6px 18px rgb(39 76 119 / 4%);
}
.project-browse-navigator.is-borderless {
  border-color: transparent;
}
.project-browse-navigator.is-shadowless {
  box-shadow: none;
}
.project-browse-navigator__activity-display {
  min-width: 0;
  flex: 1 1 auto;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--review-activity-color);
}
.project-browse-navigator__activity-display strong {
  min-width: 0;
  flex: 1 1 auto;
  color: inherit;
  font-size: var(--review-activity-font-size);
  font-weight: var(--review-activity-font-weight);
  line-height: 1.45;
  overflow-wrap: anywhere;
  white-space: normal;
}
.project-browse-navigator__activity-display.is-centered strong {
  text-align: center;
}
.project-browse-navigator__activity-display.is-right strong {
  text-align: right;
}
.project-browse-navigator__activity-display.is-nowrap strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.project-browse-navigator__activity-options {
  display: grid;
  gap: 6px;
  max-height: 360px;
  overflow: auto;
}
.project-browse-navigator__activity-options button {
  width: 100%;
  padding: 8px 10px;
  color: #334155;
  line-height: 1.45;
  text-align: left;
  overflow-wrap: anywhere;
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--app-radius-sm, var(--el-border-radius-small, 4px));
}
.project-browse-navigator__activity-options button:hover {
  color: var(--navigator-text-color);
  background: var(--navigator-hover-background);
  border-color: transparent;
}
.project-browse-navigator__activity-options button.is-active {
  color: var(--navigator-selected-text-color);
  background: var(--navigator-selected-background);
  border-color: var(--el-color-primary-light-7);
}
.project-browse-navigator.is-compact-sidebar {
  width: 100%;
  max-width: 100%;
  max-height: min(var(--navigator-sidebar-max-height, 720px), calc(100vh - 96px));
  padding: var(--navigator-panel-padding, 12px);
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-sizing: border-box;
  overflow: auto;
}
.project-browse-navigator__compact-toolbar,
.project-browse-navigator__section-heading {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.project-browse-navigator__compact-toolbar {
  min-height: 32px;
  padding-bottom: 8px;
  border-bottom: 0;
}
.project-browse-navigator.has-dividers .project-browse-navigator__compact-toolbar {
  border-bottom: var(--navigator-divider-width, 1px) solid var(--navigator-divider-color);
}
.project-browse-navigator__compact-toolbar strong {
  min-width: 0;
  overflow: hidden;
  color: var(--navigator-text-color);
  font-size: var(--navigator-title-size);
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.project-browse-navigator__compact-actions {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 2px;
}
.project-browse-navigator__compact-mode {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px;
  padding: 3px;
  background: color-mix(in srgb, var(--navigator-border-color) 24%, transparent);
  border-radius: var(--app-radius-sm, var(--el-border-radius-small, 4px));
}
.project-browse-navigator__compact-mode > button {
  min-width: 0;
  min-height: 32px;
  padding: 0 8px;
  overflow: hidden;
  color: var(--navigator-text-color);
  font-size: var(--navigator-item-size);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: var(--app-radius-sm, var(--el-border-radius-small, 4px));
}
.project-browse-navigator__compact-mode > button:hover {
  background: var(--navigator-hover-background);
}
.project-browse-navigator__compact-mode > button.is-active {
  color: var(--navigator-selected-text-color);
  background: var(--navigator-selected-background);
  box-shadow: 0 1px 3px rgb(15 23 42 / 8%);
}
.project-browse-navigator__collapse {
  width: 30px;
  min-width: 30px;
  height: 30px;
  padding: 0;
  color: #64748b;
}
.project-browse-navigator__compact-section {
  min-width: 0;
}
.project-browse-navigator__section-label {
  display: block;
  min-width: 0;
  margin-bottom: 7px;
  color: #52657d;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
}
.project-browse-navigator__compact-activity {
  width: 100%;
}
.project-browse-navigator__compact-activity :deep(.el-select__wrapper) {
  min-height: 36px;
  border: 0;
  border-radius: var(--app-radius-sm, var(--el-border-radius-small, 4px));
  background: color-mix(in srgb, var(--navigator-background) 72%, #f6f9fd);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--navigator-border-color) 72%, transparent) inset;
}
.project-browse-navigator__section-heading .project-browse-navigator__section-label {
  margin-bottom: 0;
}
.project-browse-navigator__section-heading > button {
  min-height: 28px;
  padding: 0 8px;
  border: 0;
  border-radius: var(--app-radius-sm, var(--el-border-radius-small, 4px));
  background: transparent;
  color: #64748b;
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}
.project-browse-navigator__section-heading > button:hover {
  color: var(--navigator-text-color);
  background: var(--navigator-hover-background);
}
.project-browse-navigator__section-heading > button.is-active {
  color: var(--navigator-selected-text-color);
  background: var(--navigator-selected-background);
}
.project-browse-navigator__category-scroll {
  margin-top: 6px;
}
.project-browse-navigator__unit-list {
  min-width: 0;
}
.project-browse-navigator__school-hint {
  padding: 2px 0 6px;
}
.project-browse-navigator__school-hint :deep(.el-empty__description) {
  margin-top: 4px;
}
.project-browse-navigator__school-hint :deep(.el-empty__description p) {
  color: #64748b;
  font-size: 12px;
}
.project-browse-navigator__category-scroll :deep(.el-tree) {
  background: transparent;
  color: var(--navigator-text-color);
}
.project-browse-navigator__category-scroll :deep(.el-tree-node__content) {
  position: relative;
  height: var(--navigator-row-height);
  margin: var(--navigator-item-gap) 0;
  padding-right: 6px;
  border-bottom: 0;
  border-radius: var(--app-radius-sm, var(--el-border-radius-small, 4px));
}
.project-browse-navigator.has-dividers .project-browse-navigator__category-scroll :deep(.el-tree-node__content) {
  border-bottom: var(--navigator-divider-width, 1px) solid var(--navigator-divider-color);
}
.project-browse-navigator__category-scroll :deep(.el-tree-node__content:hover) {
  color: var(--navigator-text-color);
  background: var(--navigator-hover-background);
}
.project-browse-navigator__category-scroll :deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--navigator-selected-text-color);
  background: var(--navigator-selected-background);
}
.project-browse-navigator__category-scroll :deep(.el-tree-node.is-current > .el-tree-node__content::before) {
  position: absolute;
  top: 6px;
  bottom: 6px;
  left: 0;
  width: 3px;
  border-radius: 999px;
  background: var(--navigator-active-color);
  content: '';
}
.project-browse-navigator__collapsed-trigger {
  width: 48px;
  min-width: 48px;
  height: 48px;
  margin: 0;
  padding: 0;
  align-self: start;
  border: 1px solid var(--navigator-border-color);
  border-radius: var(--app-radius-md, var(--el-border-radius-base, 8px));
  background: color-mix(in srgb, var(--navigator-background) var(--navigator-background-opacity), transparent);
  box-shadow: 0 6px 18px rgb(39 76 119 / 4%);
  color: var(--navigator-active-color);
}
.project-browse-navigator__collapsed-trigger:hover {
  border-color: color-mix(in srgb, var(--navigator-active-color) 38%, var(--navigator-border-color));
  background: var(--navigator-hover-background);
}
.project-browse-navigator__heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 4px;
}
.project-browse-navigator__activity-select :deep(.el-select__wrapper) {
  min-height: 34px;
  padding: 0 6px;
  color: var(--navigator-text-color);
  font-size: var(--navigator-title-size);
  font-weight: 800;
  text-align: center;
  background: transparent;
  box-shadow: none;
}
.project-browse-navigator__activity-select :deep(.el-select__selected-item) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
}
.project-browse-navigator__activity-select :deep(.el-select__placeholder) {
  justify-content: center;
}
.project-browse-navigator__tabs {
  margin-top: 8px;
}
.project-browse-navigator__tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}
.project-browse-navigator__tabs :deep(.el-tabs__item) {
  padding: 0 6px;
  font-size: var(--navigator-item-size);
  font-weight: 800;
}
.project-browse-navigator__tabs :deep(.el-tree-node__content) {
  height: var(--navigator-row-height);
  margin: var(--navigator-item-gap) 0;
  border-radius: var(--app-radius-sm, var(--el-border-radius-small, 4px));
}
.project-browse-navigator__tabs :deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--navigator-selected-text-color);
  background: var(--navigator-selected-background);
}
.project-browse-navigator__node,
.project-browse-navigator__unit {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: flex-start;
  gap: 6px;
  font-size: var(--navigator-item-size);
}
.project-browse-navigator__node > span,
.project-browse-navigator__unit > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.project-browse-navigator__node em,
.project-browse-navigator__unit em {
  flex: 0 0 auto;
  color: #64748b;
  font-size: var(--navigator-count-size);
  font-style: normal;
}
.project-browse-navigator__unit {
  width: 100%;
  min-height: var(--navigator-row-height);
  padding: 0 8px;
  margin: var(--navigator-item-gap) 0;
  color: var(--navigator-text-color);
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-bottom: 0;
  border-radius: var(--app-radius-sm, var(--el-border-radius-small, 4px));
  justify-content: space-between;
  gap: 8px;
}
.project-browse-navigator.has-dividers .project-browse-navigator__unit {
  border-bottom: var(--navigator-divider-width, 1px) solid var(--navigator-divider-color);
}
.project-browse-navigator__unit:hover {
  color: var(--navigator-text-color);
  background: var(--navigator-hover-background);
}
.project-browse-navigator__unit.is-active {
  color: var(--navigator-selected-text-color);
  background: var(--navigator-selected-background);
}
.project-browse-navigator__unit-search {
  margin-bottom: 8px;
}
.project-browse-navigator__secondary {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}
.project-browse-navigator__all {
  width: 100%;
  margin-top: 12px;
  border-color: var(--navigator-active-color);
  color: var(--navigator-active-color);
}
.project-browse-navigator--top {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 16px;
  min-height: var(--navigator-top-height);
  padding: 10px 14px;
}
.project-browse-navigator__top-field {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}
.project-browse-navigator__top-field > span {
  flex: 0 0 auto;
  color: var(--navigator-text-color);
  font-size: var(--navigator-item-size);
  font-weight: 800;
  white-space: nowrap;
}
.project-browse-navigator__top-field--activity :deep(.el-select),
.project-browse-navigator__top-field--school :deep(.el-select) {
  width: 180px;
}
.project-browse-navigator__top-field--activity.is-title-display {
  width: clamp(240px, 30vw, 560px);
}
.project-browse-navigator__top-options {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 4px;
}
.project-browse-navigator__top-options > button {
  min-height: 30px;
  padding: 0 8px;
  color: var(--navigator-text-color);
  font-size: var(--navigator-item-size);
  line-height: 28px;
  white-space: nowrap;
  cursor: pointer;
  background: transparent;
  border: 1px solid color-mix(in srgb, var(--navigator-border-color) 85%, transparent);
  border-radius: var(--app-radius-sm, var(--el-border-radius-small, 4px));
}
.project-browse-navigator__top-options > button:hover {
  color: var(--navigator-text-color);
  background: var(--navigator-hover-background);
  border-color: color-mix(in srgb, var(--navigator-active-color) 24%, transparent);
}
.project-browse-navigator__top-options > button.is-active {
  color: var(--navigator-selected-text-color);
  background: var(--navigator-selected-background);
  border-color: color-mix(in srgb, var(--navigator-active-color) 45%, transparent);
}
.project-browse-navigator__top-options em {
  color: inherit;
  font-size: calc(var(--navigator-item-size) - 1px);
  font-style: normal;
}
@media (max-width: 768px) {
  .project-browse-navigator--top {
    align-items: stretch;
  }
  .project-browse-navigator__top-field {
    width: 100%;
    align-items: flex-start;
    flex-wrap: wrap;
  }
  .project-browse-navigator__top-field--activity.is-title-display {
    width: 100%;
  }
  .project-browse-navigator__top-field--activity :deep(.el-select),
  .project-browse-navigator__top-field--school :deep(.el-select) {
    flex: 1 1 180px;
    width: auto;
  }
}
</style>
