<template>
  <section class="art-project-workspace-header" aria-label="项目列表筛选与操作" :style="headerStyle">
    <article
      v-for="cardModel in visibleCardModels"
      :key="cardModel.card.key"
      class="art-project-workspace-header__card"
      :style="cardStyle(cardModel.card)"
    >
      <div v-for="rowModel in cardModel.rows" :key="`${cardModel.card.key}-${rowModel.rowIndex}`" class="art-project-workspace-header__row">
        <div
          v-for="itemId in rowModel.itemIds"
          :key="itemId"
          class="art-project-workspace-header__item"
          :class="itemClasses(itemId)"
          :style="itemStyle(rowModel.spans, itemId)"
        >
          <template v-if="itemKey(itemId) === 'title'">
            <slot name="title">
              <h2 class="art-project-workspace-header__title" :title="titleText">{{ titleText }}</h2>
            </slot>
          </template>
          <div
            v-else-if="itemKey(itemId) === 'compactGroup'"
            :ref="(element) => setCompactGroupRef(itemId, element)"
            class="art-project-workspace-header__compact-group"
            :class="`is-${compactGroupStage(itemId)}`"
            :style="compactGroupStyle(itemId)"
          >
            <div
              v-for="childKey in inlineCompactGroupItems(itemId)"
              :key="childKey"
              class="art-project-workspace-header__compact-item"
              :class="[`is-${childKey}`, { 'is-compact-button': isCompactGroupButton(childKey) }]"
            >
              <slot :name="childKey"></slot>
            </div>
            <el-popover
              v-if="collapsedCompactGroupItems(itemId).length"
              v-model:visible="compactGroupPopoverVisible[itemId]"
              placement="bottom-end"
              trigger="click"
              :width="220"
              popper-class="art-workspace-compact-overflow-popper"
            >
              <template #reference>
                <el-button class="art-project-workspace-header__more-button" :aria-label="compactGroupCollapseText(itemId)">
                  <el-icon><MoreFilled /></el-icon>
                  <span>{{ compactGroupCollapseText(itemId) }}</span>
                </el-button>
              </template>
              <div class="art-project-workspace-header__overflow-menu" role="group" aria-label="折叠的表头操作">
                <div
                  v-for="childKey in collapsedCompactGroupItems(itemId)"
                  :key="childKey"
                  class="art-project-workspace-header__overflow-item"
                  :class="`is-${childKey}`"
                  @click="compactGroupPopoverVisible[itemId] = false"
                >
                  <slot :name="childKey"></slot>
                </div>
              </div>
            </el-popover>
          </div>
          <slot v-else-if="!isSpacer(itemId)" :name="itemKey(itemId)"></slot>
        </div>
      </div>
    </article>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, onUpdated, reactive, watch, type ComponentPublicInstance } from 'vue';
import type {
  ArtWorkspaceHeaderCardConfig,
  ArtWorkspaceHeaderItemKey,
  ArtWorkspaceHeaderPageConfig,
  ArtWorkspaceHeaderPageKey
} from '@/api/crehn/detailDisplay';
import {
  isWorkspaceHeaderSpacerKey,
  resolveWorkspaceHeaderRuntimeGridSpans,
  useArtDetailDisplayConfig,
  workspaceHeaderCollapsibleButtonKeys,
  workspaceHeaderLayoutItemKey
} from './artDetailDisplayConfig';

const props = withDefaults(
  defineProps<{
    pageKey: ArtWorkspaceHeaderPageKey;
    config?: ArtWorkspaceHeaderPageConfig;
    titleVariables?: Record<string, string | number | undefined>;
  }>(),
  {
    titleVariables: () => ({})
  }
);

const slots = useSlots();
const { detailDisplayConfig } = useArtDetailDisplayConfig();
const pageConfig = computed(() => props.config || detailDisplayConfig.value.workspaceHeader.pages[props.pageKey]);
const titleText = computed(() =>
  pageConfig.value.titleTemplate.replace(/\{([A-Za-z][A-Za-z0-9]*)\}/g, (_match, key: string) => String(props.titleVariables[key] ?? '')).trim()
);

const itemKey = (itemId: string) => workspaceHeaderLayoutItemKey(itemId, pageConfig.value);
const isSpacer = (itemId: string) => isWorkspaceHeaderSpacerKey(itemKey(itemId));
const hasContent = (itemId: string) => {
  const key = itemKey(itemId);
  if (key === 'compactGroup') return visibleCompactGroupItems(itemId).length > 0;
  return isWorkspaceHeaderSpacerKey(key) || key === 'title' || Boolean(slots[key]);
};
const rowHasBusinessContent = (row: readonly string[]) => row.some((itemId) => !isSpacer(itemId) && hasContent(itemId));
const interactionSafeItemKeys = new Set<ArtWorkspaceHeaderItemKey>([
  'navigatorToggle',
  'status',
  'search',
  'actions',
  'columnWidthReset',
  'selectAll',
  'unifiedSubmit',
  'navigationButton',
  'home',
  'compactGroup'
]);
const itemClasses = (itemId: string) => {
  const key = itemKey(itemId);
  return [`is-${key}`, { 'is-interaction-safe': interactionSafeItemKeys.has(key) }];
};
const visibleCardModels = computed(() =>
  pageConfig.value.cards
    .map((card) => ({
      card,
      rows: card.rows.flatMap((row, rowIndex) => {
        if (!rowHasBusinessContent(row)) return [];
        const spans = resolveWorkspaceHeaderRuntimeGridSpans(row, pageConfig.value, (candidateId) => hasContent(candidateId));
        return [
          {
            rowIndex,
            spans,
            itemIds: row.filter((itemId) => hasContent(itemId) && (spans[itemId] || 0) > 0)
          }
        ];
      })
    }))
    .filter((cardModel) => cardModel.rows.length > 0)
);
const itemStyle = (spans: Record<string, number>, itemId: string) => {
  const key = itemKey(itemId);
  const align = key === 'compactGroup' ? pageConfig.value.compactGroupInstances[itemId]?.align || 'right' : pageConfig.value.itemConfigs[key].align;
  return {
    gridColumn: `span ${spans[itemId] || 1}`,
    justifyContent: align === 'right' ? 'flex-end' : align === 'center' ? 'center' : 'flex-start'
  };
};
const visibleCompactGroupItems = (itemId: string) =>
  (pageConfig.value.compactGroupInstances[itemId]?.items || []).filter((childKey) => Boolean(slots[childKey]));
const collapsibleCompactGroupKeySet = new Set(workspaceHeaderCollapsibleButtonKeys);
const isCompactGroupButton = (itemKey: ArtWorkspaceHeaderItemKey) => collapsibleCompactGroupKeySet.has(itemKey);
type CompactGroupStage = 'inline' | 'partial' | 'all';
const compactGroupStages = reactive<Record<string, CompactGroupStage>>({});
const compactGroupPopoverVisible = reactive<Record<string, boolean>>({});
const compactGroupElements = new Map<string, HTMLElement>();
const compactGroupIds = new WeakMap<HTMLElement, string>();
const compactGroupFullWidths = new Map<string, number>();
const compactGroupPartialWidths = new Map<string, number>();
const compactGroupEvaluationFrames = new Map<string, number>();
let compactGroupResizeObserver: ResizeObserver | undefined;

const compactGroupStage = (itemId: string): CompactGroupStage => compactGroupStages[itemId] || 'inline';
const compactGroupCollapseText = (itemId: string) => pageConfig.value.compactGroupInstances[itemId]?.collapseText?.trim() || '更多';
const compactGroupCollapsibleItems = (itemId: string) =>
  visibleCompactGroupItems(itemId).filter((childKey) => collapsibleCompactGroupKeySet.has(childKey));
const inlineCompactGroupItems = (itemId: string) => {
  const items = visibleCompactGroupItems(itemId);
  const group = pageConfig.value.compactGroupInstances[itemId];
  const stage = compactGroupStage(itemId);
  if (!group || stage === 'inline' || group.collapseMode === 'none') return items;
  if (stage === 'all') return items.filter((childKey) => !collapsibleCompactGroupKeySet.has(childKey));
  const pinnedItems = new Set(group.pinnedItems || []);
  return items.filter((childKey) => !collapsibleCompactGroupKeySet.has(childKey) || pinnedItems.has(childKey));
};
const collapsedCompactGroupItems = (itemId: string) => {
  const inlineItems = new Set(inlineCompactGroupItems(itemId));
  return compactGroupCollapsibleItems(itemId).filter((childKey) => !inlineItems.has(childKey));
};
const setCompactGroupStage = (itemId: string, stage: CompactGroupStage) => {
  if (compactGroupStage(itemId) === stage) return;
  compactGroupStages[itemId] = stage;
  if (stage === 'inline') compactGroupPopoverVisible[itemId] = false;
  nextTick(() => scheduleCompactGroupEvaluation(itemId));
};
const compactGroupRequiredWidth = (element: HTMLElement, gap: number) => {
  const children = Array.from(element.children) as HTMLElement[];
  return (
    children.reduce((total, child) => total + Math.max(child.scrollWidth, child.getBoundingClientRect().width), 0) +
    Math.max(0, children.length - 1) * gap
  );
};
const evaluateCompactGroup = (itemId: string) => {
  const element = compactGroupElements.get(itemId);
  const group = pageConfig.value.compactGroupInstances[itemId];
  if (!element || !group || element.clientWidth <= 0) return;
  const mode = group.collapseMode || 'none';
  if (mode === 'none' || compactGroupCollapsibleItems(itemId).length === 0) {
    compactGroupFullWidths.delete(itemId);
    compactGroupPartialWidths.delete(itemId);
    setCompactGroupStage(itemId, 'inline');
    return;
  }
  const availableWidth = element.clientWidth;
  const requiredWidth = compactGroupRequiredWidth(element, group.gap ?? pageConfig.value.componentGap);
  const stage = compactGroupStage(itemId);
  if (stage === 'inline') {
    compactGroupFullWidths.set(itemId, Math.max(compactGroupFullWidths.get(itemId) || 0, requiredWidth));
    if (requiredWidth > availableWidth + 1) setCompactGroupStage(itemId, mode === 'all' ? 'all' : 'partial');
    return;
  }
  const fullWidth = compactGroupFullWidths.get(itemId) || 0;
  if (stage === 'partial') {
    if (fullWidth > 0 && availableWidth >= fullWidth + 4) {
      setCompactGroupStage(itemId, 'inline');
      return;
    }
    compactGroupPartialWidths.set(itemId, Math.max(compactGroupPartialWidths.get(itemId) || 0, requiredWidth));
    if (requiredWidth > availableWidth + 1) setCompactGroupStage(itemId, 'all');
    return;
  }
  if (mode === 'overflow') {
    const partialWidth = compactGroupPartialWidths.get(itemId) || 0;
    if (partialWidth > 0 && availableWidth >= partialWidth + 4) {
      setCompactGroupStage(itemId, 'partial');
      return;
    }
  }
  if (fullWidth > 0 && availableWidth >= fullWidth + 4) setCompactGroupStage(itemId, 'inline');
};
function scheduleCompactGroupEvaluation(itemId: string) {
  const previousFrame = compactGroupEvaluationFrames.get(itemId);
  if (previousFrame !== undefined) cancelAnimationFrame(previousFrame);
  compactGroupEvaluationFrames.set(
    itemId,
    requestAnimationFrame(() => {
      compactGroupEvaluationFrames.delete(itemId);
      evaluateCompactGroup(itemId);
    })
  );
}
const setCompactGroupRef = (itemId: string, element: Element | ComponentPublicInstance | null) => {
  const nextElement = element instanceof HTMLElement ? element : null;
  const previousElement = compactGroupElements.get(itemId);
  if (previousElement === nextElement) return;
  if (previousElement) compactGroupResizeObserver?.unobserve(previousElement);
  if (!nextElement) {
    compactGroupElements.delete(itemId);
    return;
  }
  compactGroupElements.set(itemId, nextElement);
  compactGroupIds.set(nextElement, itemId);
  compactGroupResizeObserver?.observe(nextElement);
  nextTick(() => scheduleCompactGroupEvaluation(itemId));
};
const resetCompactGroupPresentations = () => {
  compactGroupFullWidths.clear();
  compactGroupPartialWidths.clear();
  Object.keys(compactGroupStages).forEach((itemId) => {
    compactGroupStages[itemId] = 'inline';
  });
  nextTick(() => compactGroupElements.forEach((_element, itemId) => scheduleCompactGroupEvaluation(itemId)));
};

watch(() => pageConfig.value.compactGroupInstances, resetCompactGroupPresentations, { deep: true });
onMounted(() => {
  if (typeof ResizeObserver !== 'undefined') {
    compactGroupResizeObserver = new ResizeObserver((entries) => {
      entries.forEach((entry) => {
        const itemId = compactGroupIds.get(entry.target as HTMLElement);
        if (itemId) scheduleCompactGroupEvaluation(itemId);
      });
    });
    compactGroupElements.forEach((element) => compactGroupResizeObserver?.observe(element));
  }
  resetCompactGroupPresentations();
});
onUpdated(() => {
  compactGroupElements.forEach((_element, itemId) => scheduleCompactGroupEvaluation(itemId));
});
onBeforeUnmount(() => {
  compactGroupResizeObserver?.disconnect();
  compactGroupEvaluationFrames.forEach((frame) => cancelAnimationFrame(frame));
});
const compactGroupStyle = (itemId: string) => {
  const group = pageConfig.value.compactGroupInstances[itemId];
  const justifyContent = group?.align === 'left' ? 'flex-start' : group?.align === 'center' ? 'center' : 'flex-end';
  return {
    gap: `${group?.gap ?? pageConfig.value.componentGap}px`,
    justifyContent
  };
};
const cardStyle = (
  card: Readonly<Pick<ArtWorkspaceHeaderCardConfig, 'backgroundColor' | 'backgroundOpacity' | 'borderRadius' | 'borderVisible'>>
) => ({
  '--art-workspace-card-background': `color-mix(in srgb, ${card.backgroundColor} ${card.backgroundOpacity}%, transparent)`,
  '--art-workspace-card-radius': `${card.borderRadius}px`,
  '--art-workspace-card-border': card.borderVisible ? '1px solid var(--el-border-color-lighter)' : '0'
});
const headerStyle = computed(() => ({
  '--art-workspace-component-gap': `${pageConfig.value.componentGap}px`,
  '--art-workspace-button-font-size': `${pageConfig.value.buttonStyle.fontSize}px`,
  '--art-workspace-button-font-weight': String(pageConfig.value.buttonStyle.fontWeight),
  '--art-workspace-button-icon-size': `${pageConfig.value.buttonStyle.iconSize}px`,
  '--art-workspace-button-height': `${pageConfig.value.buttonStyle.height}px`,
  '--art-workspace-title-color': pageConfig.value.titleColor,
  '--art-workspace-title-font-size': `${pageConfig.value.titleFontSize}px`,
  '--art-workspace-title-font-weight': String(pageConfig.value.titleFontWeight),
  '--art-workspace-title-align': pageConfig.value.itemConfigs.title.align,
  '--art-workspace-row-min-height': `${pageConfig.value.rowMinHeight}px`,
  '--art-workspace-padding-top': `${pageConfig.value.paddingTop}px`,
  '--art-workspace-padding-bottom': `${pageConfig.value.paddingBottom}px`,
  '--art-workspace-padding-inline': `${pageConfig.value.paddingInline}px`,
  marginTop: `${pageConfig.value.marginTop}px`,
  marginBottom: `${pageConfig.value.marginBottom}px`
}));
</script>

<style scoped>
.art-project-workspace-header {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.art-project-workspace-header__card {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: var(--art-workspace-padding-top, 10px) var(--art-workspace-padding-inline, 14px) var(--art-workspace-padding-bottom, 10px);
  background: var(--art-workspace-card-background, var(--el-bg-color));
  border: var(--art-workspace-card-border, 1px solid var(--el-border-color-lighter));
  border-radius: var(--art-workspace-card-radius, var(--el-border-radius-base));
}

.art-project-workspace-header__row {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  min-width: 0;
  align-items: center;
  gap: var(--art-workspace-component-gap, 10px);
  min-height: var(--art-workspace-row-min-height, 36px);
  overflow: clip;
  overflow-clip-margin: 4px;
}

.art-project-workspace-header__item {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.art-project-workspace-header__item.is-interaction-safe {
  position: relative;
  overflow: visible;
}

.art-project-workspace-header__item.is-interaction-safe:is(:hover, :focus-within) {
  z-index: 2;
}

.art-project-workspace-header__compact-group {
  display: flex;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  align-items: center;
  flex-wrap: nowrap;
  overflow: visible;
}

.art-project-workspace-header__compact-item {
  display: inline-flex;
  max-width: 100%;
  min-width: 0;
  align-items: center;
  flex: 1 1 auto;
  overflow: visible;
  white-space: nowrap;
}

.art-project-workspace-header__compact-item.is-status {
  flex-grow: 2;
}

.art-project-workspace-header__compact-item.is-compact-button {
  max-width: none;
  min-width: max-content;
  flex: 0 0 auto;
}

.art-project-workspace-header__more-button {
  flex: 0 0 auto;
  min-width: max-content;
}

.art-project-workspace-header__overflow-menu {
  display: grid;
  gap: 6px;
}

.art-project-workspace-header__overflow-item {
  display: grid;
  min-width: 0;
}

.art-project-workspace-header__overflow-item :deep(.el-button),
.art-project-workspace-header__overflow-item :deep(.art-workspace-config-button) {
  width: 100%;
  margin-left: 0;
  justify-content: flex-start;
}

.art-project-workspace-header__overflow-item :deep(.el-tooltip__trigger) {
  width: 100%;
}

:global(.art-workspace-compact-overflow-popper) {
  max-width: min(280px, calc(100vw - 24px));
}

.art-project-workspace-header__item.is-title {
  min-width: 0;
}

.art-project-workspace-header__item:is(.is-fixedSpacer, .is-autoSpacer) {
  min-height: 1px;
  pointer-events: none;
}

.art-project-workspace-header :deep(.art-workspace-config-button) {
  height: var(--art-workspace-button-height, 32px);
  font-size: var(--art-workspace-button-font-size, 14px);
  font-weight: var(--art-workspace-button-font-weight, 400);
}

.art-project-workspace-header :deep(.el-button),
.art-project-workspace-header :deep(.art-status-tabs__item) {
  font-size: var(--art-workspace-button-font-size, 14px);
  font-weight: var(--art-workspace-button-font-weight, 400);
}

.art-project-workspace-header :deep(.el-button) {
  height: var(--art-workspace-button-height, 32px);
}

.art-project-workspace-header :deep(.art-workspace-config-button.is-icon-only) {
  width: var(--art-workspace-button-height, 32px);
}

.art-project-workspace-header :deep(.art-workspace-config-button .el-icon) {
  font-size: var(--art-workspace-button-icon-size, 14px);
}

.art-project-workspace-header__title {
  width: 100%;
  min-width: 0;
  margin: 0;
  overflow: hidden;
  color: var(--art-workspace-title-color, #102a43);
  font-size: var(--art-workspace-title-font-size, 20px);
  font-weight: var(--art-workspace-title-font-weight, 800);
  line-height: 1.4;
  text-align: var(--art-workspace-title-align, left);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.art-project-workspace-header__item :deep(.el-select),
.art-project-workspace-header__item :deep(.el-input),
.art-project-workspace-header__item :deep(.art-category-tree-dropdown) {
  max-width: 100%;
  min-width: 0;
}
</style>
