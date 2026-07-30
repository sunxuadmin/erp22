<template>
  <el-dropdown class="art-category-tree-dropdown" :disabled="disabled" trigger="click" @command="handleCommand" @visible-change="handleVisibleChange">
    <button class="art-category-tree-dropdown__trigger" type="button" :disabled="disabled" :title="`当前类别：${selectedLabel}`">
      <span>{{ selectedLabel }}</span>
      <el-icon><arrow-down /></el-icon>
    </button>
    <template #dropdown>
      <el-dropdown-menu class="art-category-tree-menu">
        <el-dropdown-item :command="allCommand" :class="{ 'is-active': !selectedKey }">
          <span class="art-category-tree-menu__item art-category-tree-menu__item--all">
            <span>{{ allLabel }}</span>
            <em>{{ allCount }}</em>
          </span>
        </el-dropdown-item>
        <template v-for="node in tree" :key="String(node.id)">
          <CategoryMenuBranch :node="node" :selected-key="selectedKey" :expanded-keys="expandedKeys" :count-map="countMap" @toggle="toggleNode" />
        </template>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { defineComponent, h, type PropType, type VNode } from 'vue';
import { ElDropdownItem, ElIcon } from 'element-plus';
import { ArrowRight } from '@element-plus/icons-vue';
import type { ActivityCategoryTreeNode } from '@/utils/artCategory';

type CountValue = number | { totalCount?: number; count?: number };

interface ArtCategoryTreeSelection {
  key: string;
  label: string;
  categoryIds: Array<string | number>;
  leaf: boolean;
}

const props = withDefaults(
  defineProps<{
    tree: ActivityCategoryTreeNode[];
    countMap?: Record<string, CountValue>;
    selectedKey?: string;
    disabled?: boolean;
    allLabel?: string;
    placeholder?: string;
  }>(),
  {
    countMap: () => ({}),
    selectedKey: '',
    disabled: false,
    allLabel: '全部',
    placeholder: '全部类别'
  }
);

const emit = defineEmits<{
  select: [selection: ArtCategoryTreeSelection];
}>();

const allCommand = '__all_categories__';
const expandedKeys = ref<Set<string>>(new Set());

const countOf = (value?: CountValue) => (typeof value === 'number' ? value : Number(value?.totalCount ?? value?.count ?? 0));
const leafCategoryIds = (node: ActivityCategoryTreeNode): Array<string | number> => {
  if (node.children?.length) return node.children.flatMap(leafCategoryIds);
  if (node.virtualGroup || node.id === undefined || node.id === null) return [];
  return [node.id];
};
const nodeCount = (node: ActivityCategoryTreeNode) => leafCategoryIds(node).reduce<number>((sum, id) => sum + countOf(props.countMap[String(id)]), 0);
const flatNodes = computed(() => {
  const rows: ActivityCategoryTreeNode[] = [];
  const append = (nodes: ActivityCategoryTreeNode[]) => {
    nodes.forEach((node) => {
      rows.push(node);
      if (node.children?.length) append(node.children);
    });
  };
  append(props.tree);
  return rows;
});
const selectedNode = computed(() => flatNodes.value.find((node) => String(node.id) === String(props.selectedKey || '')));
const selectedLabel = computed(() => String(selectedNode.value?.categoryName || props.placeholder));
const allCount = computed(() => {
  const ids = new Set(props.tree.flatMap(leafCategoryIds).map(String));
  return [...ids].reduce((sum, id) => sum + countOf(props.countMap[id]), 0);
});

const resetExpandedKeys = () => {
  expandedKeys.value = new Set(flatNodes.value.filter((node) => node.children?.length).map((node) => String(node.id)));
};
const handleVisibleChange = (visible: boolean) => {
  if (visible) resetExpandedKeys();
};
const toggleNode = (key: string) => {
  const next = new Set(expandedKeys.value);
  if (next.has(key)) next.delete(key);
  else next.add(key);
  expandedKeys.value = next;
};
const handleCommand = (command: string) => {
  if (command === allCommand) {
    emit('select', { key: '', label: props.allLabel, categoryIds: [], leaf: false });
    return;
  }
  const node = flatNodes.value.find((item) => String(item.id) === String(command));
  if (!node) return;
  emit('select', {
    key: String(node.id),
    label: String(node.categoryName || node.id),
    categoryIds: leafCategoryIds(node),
    leaf: !node.children?.length && !node.virtualGroup
  });
};

const CategoryMenuBranch = defineComponent({
  name: 'ArtCategoryMenuBranch',
  props: {
    node: { type: Object as PropType<ActivityCategoryTreeNode>, required: true },
    selectedKey: { type: String, default: '' },
    expandedKeys: { type: Object as PropType<Set<string>>, required: true },
    countMap: { type: Object as PropType<Record<string, CountValue>>, required: true }
  },
  emits: ['toggle'],
  setup(branchProps, { emit: branchEmit }) {
    const renderNode = (node: ActivityCategoryTreeNode, depth: number): VNode[] => {
      const key = String(node.id);
      const hasChildren = Boolean(node.children?.length);
      const expanded = branchProps.expandedKeys.has(key);
      const ids = leafCategoryIds(node);
      const count = ids.reduce<number>((sum, id) => sum + countOf(branchProps.countMap[String(id)]), 0);
      const item = h(
        ElDropdownItem,
        {
          command: key,
          class: {
            'is-active': key === String(branchProps.selectedKey || ''),
            'is-category-group': hasChildren
          }
        },
        {
          default: () =>
            h(
              'span',
              {
                class: ['art-category-tree-menu__item', depth ? 'art-category-tree-menu__item--child' : 'art-category-tree-menu__item--root'],
                style: depth ? { paddingLeft: `${Math.min(depth, 3) * 26}px` } : undefined
              },
              [
                hasChildren
                  ? h(
                      'button',
                      {
                        class: 'art-category-tree-menu__toggle',
                        type: 'button',
                        'aria-label': expanded ? `收起${node.categoryName || key}` : `展开${node.categoryName || key}`,
                        onClick: (event: MouseEvent) => {
                          event.stopPropagation();
                          event.preventDefault();
                          branchEmit('toggle', key);
                        }
                      },
                      [h(ElIcon, { class: { 'is-expanded': expanded } }, () => h(ArrowRight))]
                    )
                  : null,
                h('span', { title: String(node.categoryName || node.id) }, String(node.categoryName || node.id)),
                h('em', String(count))
              ]
            )
        }
      );
      if (!hasChildren || !expanded) return [item];
      return [item, ...(node.children || []).flatMap((child) => renderNode(child, depth + 1))];
    };
    return () => renderNode(branchProps.node, 0);
  }
});
</script>

<style scoped lang="scss">
.art-category-tree-dropdown {
  width: 100%;
  min-width: 0;
}

.art-category-tree-dropdown__trigger {
  width: 100%;
  min-width: 0;
  min-height: 34px;
  padding: 0 11px;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border: 1px solid var(--el-border-color);
  border-radius: var(--el-border-radius-base);
  background: var(--el-fill-color-blank);
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: border-color 0.16s ease;
}

.art-category-tree-dropdown__trigger:hover,
.art-category-tree-dropdown__trigger:focus-visible {
  border-color: var(--el-color-primary);
  outline: none;
}

.art-category-tree-dropdown__trigger:disabled {
  cursor: not-allowed;
  background: var(--el-disabled-bg-color);
  color: var(--el-disabled-text-color);
}

.art-category-tree-dropdown__trigger > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.art-category-tree-menu) {
  max-height: min(68vh, 620px);
  overflow-y: auto;
}

:global(.art-category-tree-menu .el-dropdown-menu__item.is-active) {
  color: #2563eb;
  background: #eef5ff;
  font-weight: 900;
}

:global(.art-category-tree-menu .el-dropdown-menu__item.is-category-group) {
  color: #1f3b64;
  font-weight: 900;
}

:global(.art-category-tree-menu__item) {
  width: 230px;
  min-width: 0;
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

:global(.art-category-tree-menu__item > span) {
  min-width: 0;
  flex: 1 1 auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.art-category-tree-menu__toggle) {
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

:global(.art-category-tree-menu__toggle:hover) {
  background: #eef5ff;
  color: #2563eb;
}

:global(.art-category-tree-menu__toggle .el-icon) {
  transition: transform 0.16s ease;
}

:global(.art-category-tree-menu__toggle .el-icon.is-expanded) {
  transform: rotate(90deg);
}

:global(.art-category-tree-menu__item em) {
  min-width: 22px;
  height: 20px;
  padding: 0 7px;
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: #e7f0ff;
  color: #2563eb;
  font-size: 12px;
  font-style: normal;
  line-height: 20px;
}
</style>
