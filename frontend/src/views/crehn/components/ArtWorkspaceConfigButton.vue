<template>
  <el-tooltip :content="resolvedTooltip" placement="top" :disabled="!iconOnly">
    <el-button
      class="art-workspace-config-button"
      :class="{ 'is-icon-only': iconOnly, 'has-custom-appearance': appearanceStyle }"
      :style="appearanceStyle"
      :icon="icon"
      :circle="iconOnly"
      :disabled="disabled"
      :loading="loading"
      :aria-label="resolvedTooltip"
      @click="$emit('click')"
    >
      <span v-if="!iconOnly">{{ normalizedText }}</span>
    </el-button>
  </el-tooltip>
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    icon: string;
    text?: string;
    tooltip: string;
    disabled?: boolean;
    loading?: boolean;
    appearanceStyle?: Record<string, string>;
  }>(),
  {
    text: '',
    disabled: false,
    loading: false,
    appearanceStyle: undefined
  }
);

defineEmits<{ click: [] }>();

const normalizedText = computed(() => props.text.trim());
const iconOnly = computed(() => !normalizedText.value);
const resolvedTooltip = computed(() => props.tooltip.trim() || normalizedText.value || '按钮操作');
</script>

<style scoped>
.art-workspace-config-button.has-custom-appearance {
  color: var(--art-button-text-color);
  background-color: var(--art-button-background-color);
  border-color: var(--art-button-border-color);
  border-width: var(--art-button-border-width);
  border-radius: var(--art-button-border-radius);
}

.art-workspace-config-button.has-custom-appearance:not(.is-disabled):hover,
.art-workspace-config-button.has-custom-appearance:not(.is-disabled):focus-visible {
  color: var(--art-button-hover-text-color);
  background-color: var(--art-button-hover-background-color);
  border-color: var(--art-button-hover-border-color);
}

.art-workspace-config-button.has-custom-appearance.is-disabled {
  opacity: 0.55;
}

.art-workspace-config-button.is-icon-only {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  line-height: 1;
  background: transparent;
  border: 0;
  box-shadow: none;
}

.art-workspace-config-button.is-icon-only :deep(.el-icon) {
  margin: 0 !important;
  font-size: calc(var(--art-workspace-button-font-size, 14px) + 2px);
}

.art-workspace-config-button.is-icon-only:not(.is-disabled):hover,
.art-workspace-config-button.is-icon-only:not(.is-disabled):focus-visible {
  background-color: var(--el-fill-color-light);
  border-color: transparent;
}

.art-workspace-config-button.is-icon-only:not(.is-disabled):focus-visible {
  box-shadow: 0 0 0 2px var(--el-color-primary-light-7);
}
</style>
