<template>
  <div ref="rootRef" class="art-status-tabs" :class="{ 'is-dropdown': dropdownMode, 'is-scrollable': !collapseOnOverflow }" :aria-label="ariaLabel">
    <div v-if="!dropdownMode" ref="trackRef" class="art-status-tabs__track" role="tablist">
      <button
        v-for="item in items"
        :key="itemKey(item.value)"
        class="art-status-tabs__item"
        :class="{ 'is-active': isActive(item.value) }"
        type="button"
        role="tab"
        :aria-selected="isActive(item.value)"
        :disabled="item.disabled"
        @click="selectItem(item.value, item.disabled)"
      >
        <span>{{ item.label }}</span>
        <em v-if="item.count !== undefined">({{ item.count }})</em>
      </button>
    </div>
    <el-select v-else class="art-status-tabs__select" :model-value="activeItemKey" :aria-label="ariaLabel" @change="selectDropdownItem">
      <el-option
        v-for="item in items"
        :key="itemKey(item.value)"
        :label="itemDisplayLabel(item)"
        :value="itemKey(item.value)"
        :disabled="item.disabled"
      />
    </el-select>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

interface ArtStatusTabItem {
  label: string;
  value?: string | number;
  count?: number;
  disabled?: boolean;
}

const props = withDefaults(
  defineProps<{
    items: ArtStatusTabItem[];
    modelValue?: string | number;
    ariaLabel?: string;
    collapseOnOverflow?: boolean;
  }>(),
  {
    modelValue: '',
    ariaLabel: '状态筛选',
    collapseOnOverflow: true
  }
);

const emit = defineEmits<{
  change: [value?: string | number];
}>();

const normalizedValue = (value?: string | number) => String(value ?? '');
const itemKey = (value?: string | number) => normalizedValue(value) || '__all__';
const isActive = (value?: string | number) => normalizedValue(value) === normalizedValue(props.modelValue);
const itemDisplayLabel = (item: ArtStatusTabItem) => `${item.label}${item.count === undefined ? '' : `（${item.count}）`}`;
const activeItemKey = computed(() => itemKey(props.modelValue));
const selectItem = (value?: string | number, disabled?: boolean) => {
  if (!disabled) emit('change', value);
};
const selectDropdownItem = (key: string) => {
  const item = props.items.find((candidate) => itemKey(candidate.value) === key);
  if (item) selectItem(item.value, item.disabled);
};

const rootRef = ref<HTMLElement>();
const trackRef = ref<HTMLElement>();
const dropdownMode = ref(false);
let tabsRequiredWidth = 0;
let resizeObserver: ResizeObserver | undefined;
let evaluationFrame = 0;

const evaluatePresentation = () => {
  cancelAnimationFrame(evaluationFrame);
  evaluationFrame = requestAnimationFrame(() => {
    const root = rootRef.value;
    const host = root?.parentElement;
    if (!root || !host || host.clientWidth <= 0) return;
    if (!props.collapseOnOverflow) {
      dropdownMode.value = false;
      return;
    }
    if (dropdownMode.value) {
      if (tabsRequiredWidth > 0 && host.clientWidth >= tabsRequiredWidth + 4) {
        dropdownMode.value = false;
        nextTick(evaluatePresentation);
      }
      return;
    }
    const requiredWidth = (trackRef.value?.scrollWidth || root.scrollWidth) + 8;
    tabsRequiredWidth = Math.max(tabsRequiredWidth, requiredWidth);
    if (requiredWidth > host.clientWidth + 1) dropdownMode.value = true;
  });
};

const resetPresentation = () => {
  tabsRequiredWidth = 0;
  dropdownMode.value = false;
  nextTick(evaluatePresentation);
};

watch(() => [props.items, props.collapseOnOverflow], resetPresentation, { deep: true });

onMounted(() => {
  const host = rootRef.value?.parentElement;
  if (typeof ResizeObserver !== 'undefined' && host) {
    resizeObserver = new ResizeObserver(evaluatePresentation);
    resizeObserver.observe(host);
  }
  evaluatePresentation();
});

onBeforeUnmount(() => {
  cancelAnimationFrame(evaluationFrame);
  resizeObserver?.disconnect();
});
</script>

<style scoped>
.art-status-tabs {
  max-width: 100%;
  min-height: 32px;
  padding: 0 4px;
  display: inline-flex;
  flex: 0 1 auto;
  align-items: center;
  overflow: hidden;
}

.art-status-tabs.is-scrollable {
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: thin;
}

.art-status-tabs.is-dropdown {
  width: min(190px, 100%);
  min-width: 0;
  padding: 0;
}

.art-status-tabs__track {
  display: inline-flex;
  width: max-content;
  align-items: center;
  gap: 2px;
}

.art-status-tabs__select {
  width: 100%;
  min-width: 0;
}

.art-status-tabs__select :deep(.el-select__wrapper) {
  min-height: 32px;
}

.art-status-tabs__select :deep(.el-select__selected-item) {
  font-size: var(--art-workspace-button-font-size, var(--art-list-button-font-size, 14px));
  font-weight: var(--art-workspace-button-font-weight, 600);
}

.art-status-tabs__item {
  position: relative;
  height: 32px;
  padding: 0 9px;
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 3px;
  color: #475569;
  font-size: var(--art-list-button-font-size, 14px);
  line-height: 30px;
  white-space: nowrap;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  transition:
    color 0.2s ease,
    background-color 0.2s ease,
    border-color 0.2s ease;
}

.art-status-tabs__item em {
  color: #94a3b8;
  font-size: var(--art-list-button-font-size, 14px);
  font-style: normal;
  font-weight: 600;
}

.art-status-tabs__item:hover:not(:disabled) {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.art-status-tabs__item:focus-visible {
  outline: 2px solid var(--el-color-primary-light-5);
  outline-offset: -2px;
  border-radius: var(--app-radius-sm, var(--el-border-radius-small));
}

.art-status-tabs__item.is-active {
  color: var(--el-color-primary);
  border-bottom-color: var(--el-color-primary);
  font-weight: 700;
}

.art-status-tabs__item.is-active em {
  color: var(--el-color-primary);
}

.art-status-tabs__item:disabled {
  color: var(--el-text-color-disabled);
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .art-status-tabs {
    width: 100%;
    justify-content: flex-start;
  }

  .art-status-tabs.is-dropdown {
    width: min(190px, 100%);
  }
}
</style>
