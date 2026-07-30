<template>
  <div
    class="review-scope-navigator"
    :class="[`is-${config.displayMode}`, `is-progress-${config.progressPlacement}`]"
    :style="navigatorStyle"
    aria-label="已分配评审范围"
  >
    <span v-if="label" class="review-scope-navigator__label">{{ label }}</span>

    <div v-if="config.displayMode === 'buttons'" class="review-scope-navigator__buttons">
      <el-tooltip v-for="scope in displayOptions" :key="scope.scopeKey" :content="scopeTooltip(scope)" placement="top">
        <span class="review-scope-navigator__button-wrap">
          <el-button
            :type="modelValue === scope.scopeKey ? 'primary' : undefined"
            :plain="modelValue !== scope.scopeKey"
            :style="scopeProgressStyle(scope)"
            :disabled="scope.totalCount <= 0"
            @click="selectScope(scope.scopeKey)"
          >
            <span class="review-scope-navigator__button-content">
              <span>{{ scope.label }}</span>
              <strong v-if="config.showCount">{{ scope.completedCount }}/{{ scope.totalCount }}</strong>
            </span>
            <span
              v-if="config.progressVisible && config.progressPlacement === 'inside'"
              class="review-scope-navigator__inside-track"
              aria-hidden="true"
            >
              <span class="review-scope-navigator__segment is-submitted"></span>
              <span class="review-scope-navigator__segment is-draft"></span>
            </span>
          </el-button>
        </span>
      </el-tooltip>
    </div>

    <div v-else class="review-scope-navigator__select-wrap" :style="scopeProgressStyle(selectedScope)">
      <el-select
        class="review-scope-navigator__select"
        :model-value="modelValue"
        :placeholder="placeholder"
        @change="selectScope(String($event ?? ''))"
      >
        <el-option
          v-for="scope in displayOptions"
          :key="scope.scopeKey"
          :label="optionText(scope)"
          :value="scope.scopeKey"
          :disabled="scope.totalCount <= 0"
        />
      </el-select>
      <span v-if="config.progressVisible && config.progressPlacement === 'inside'" class="review-scope-navigator__inside-track" aria-hidden="true">
        <span class="review-scope-navigator__segment is-submitted"></span>
        <span class="review-scope-navigator__segment is-draft"></span>
      </span>
    </div>

    <div
      v-if="config.progressVisible && config.progressPlacement === 'below'"
      class="review-scope-navigator__below-track"
      :style="scopeProgressStyle(selectedScope)"
      role="progressbar"
      :aria-valuemin="0"
      :aria-valuemax="selectedScope.totalCount"
      :aria-valuenow="selectedScope.completedCount"
      :aria-label="`${selectedScope.label}评分进度`"
    >
      <span class="review-scope-navigator__segment is-submitted"></span>
      <span class="review-scope-navigator__segment is-draft"></span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ArtReviewProgressConfig, ArtReviewScopeNavigatorConfig } from '@/api/crehn/detailDisplay';
import type { ReviewTaskScopeOptionVO } from '@/api/crehn/types';

type DisplayScope = {
  scopeKey: string;
  label: string;
  completedCount: number;
  submittedCount: number;
  totalCount: number;
};

const props = defineProps<{
  modelValue: string;
  options: ReviewTaskScopeOptionVO[];
  config: ArtReviewScopeNavigatorConfig;
  progressConfig: ArtReviewProgressConfig;
  label: string;
  placeholder: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const numberCount = (value: unknown) => Math.max(0, Number(value || 0));
const scopeLabel = (scope: ReviewTaskScopeOptionVO) => {
  const categoryName = String(scope.categoryName || '未命名类别').trim();
  const configuredLabel = String(scope.scopeLabel || '').trim();
  if (scope.wholeCategory || !configuredLabel || configuredLabel === categoryName) return categoryName;
  if (configuredLabel.startsWith(categoryName) || configuredLabel.includes('>') || configuredLabel.includes('＞')) return configuredLabel;
  return `${categoryName} > ${configuredLabel}`;
};
const scopeOptions = computed<DisplayScope[]>(() =>
  props.options.map((scope) => ({
    scopeKey: String(scope.scopeKey || ''),
    label: scopeLabel(scope),
    completedCount: numberCount(scope.completedCount),
    submittedCount: Math.min(numberCount(scope.lockedCount), numberCount(scope.completedCount)),
    totalCount: numberCount(scope.totalCount)
  }))
);
const emptyScope: DisplayScope = { scopeKey: '', label: '未选择评审范围', completedCount: 0, submittedCount: 0, totalCount: 0 };
const displayOptions = computed(() => scopeOptions.value);
const selectedScope = computed(
  () =>
    displayOptions.value.find((scope) => scope.scopeKey === props.modelValue) ||
    displayOptions.value.find((scope) => scope.totalCount > 0) ||
    emptyScope
);
const optionText = (scope: DisplayScope) => (props.config.showCount ? `${scope.label} ${scope.completedCount}/${scope.totalCount}` : scope.label);
const scopeTooltip = (scope: DisplayScope) => {
  const draftCount = Math.max(scope.completedCount - scope.submittedCount, 0);
  const pendingCount = Math.max(scope.totalCount - scope.completedCount, 0);
  if (scope.totalCount <= 0) return `${scope.label}：暂无分配作品`;
  return `${scope.label}：已提交 ${scope.submittedCount}，待提交 ${draftCount}，未评分 ${pendingCount}，共 ${scope.totalCount}`;
};
const scopeProgressStyle = (scope: DisplayScope) => ({
  '--review-scope-submitted-progress': `${scope.totalCount > 0 ? (scope.submittedCount / scope.totalCount) * 100 : 0}%`,
  '--review-scope-draft-progress': `${
    scope.totalCount > 0 ? (Math.max(scope.completedCount - scope.submittedCount, 0) / scope.totalCount) * 100 : 0
  }%`,
  '--review-scope-submitted-color': props.progressConfig.signedColor,
  '--review-scope-draft-color': props.progressConfig.activeColor
});
const navigatorStyle = computed(() => ({
  '--review-scope-track-color': props.progressConfig.trackColor,
  '--review-scope-text-color': props.progressConfig.textColor,
  '--review-scope-progress-height': `${props.progressConfig.height}px`,
  '--review-scope-inside-progress-height': `${Math.min(props.progressConfig.height, 4)}px`
}));
const selectScope = (scopeKey: string) => {
  const scope = displayOptions.value.find((item) => item.scopeKey === scopeKey);
  if (!scope || scope.totalCount <= 0) return;
  emit('update:modelValue', scopeKey);
};
</script>

<style scoped>
.review-scope-navigator {
  display: grid;
  width: 100%;
  min-width: 0;
  grid-template-columns: max-content minmax(0, 1fr);
  align-items: center;
  gap: 8px 12px;
}

.review-scope-navigator__label {
  color: var(--review-scope-text-color, var(--el-text-color-secondary));
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.review-scope-navigator__buttons {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 8px;
}

.review-scope-navigator__button-wrap {
  display: inline-flex;
}

.review-scope-navigator__buttons :deep(.el-button) {
  position: relative;
  height: var(--art-workspace-button-height, 34px);
  margin: 0;
  overflow: hidden;
  border-radius: 7px;
}

.review-scope-navigator__button-content {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.review-scope-navigator__button-content strong {
  font-variant-numeric: tabular-nums;
}

.review-scope-navigator__select-wrap {
  position: relative;
  width: min(360px, 100%);
  min-width: 180px;
  overflow: hidden;
  border-radius: var(--el-border-radius-base);
}

.review-scope-navigator__select {
  width: 100%;
}

.review-scope-navigator__inside-track {
  position: absolute;
  z-index: 2;
  right: 0;
  bottom: 0;
  left: 0;
  height: var(--review-scope-inside-progress-height, 3px);
  overflow: hidden;
  background: var(--review-scope-track-color, var(--el-fill-color-dark));
  pointer-events: none;
}

.review-scope-navigator__inside-track,
.review-scope-navigator__below-track {
  display: flex;
}

.review-scope-navigator__segment {
  display: block;
  height: 100%;
  transition: width 0.2s ease;
}

.review-scope-navigator__segment.is-submitted {
  width: var(--review-scope-submitted-progress, 0%);
  background: var(--review-scope-submitted-color, var(--el-color-success));
}

.review-scope-navigator__segment.is-draft {
  width: var(--review-scope-draft-progress, 0%);
  background: var(--review-scope-draft-color, var(--el-color-primary));
}

.review-scope-navigator__below-track {
  grid-column: 2;
  height: var(--review-scope-progress-height, 8px);
  overflow: hidden;
  border-radius: 999px;
  background: var(--review-scope-track-color, var(--el-fill-color-dark));
}

@media (max-width: 720px) {
  .review-scope-navigator {
    grid-template-columns: 1fr;
  }

  .review-scope-navigator__below-track {
    grid-column: 1;
  }
}
</style>
