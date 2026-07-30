<template>
  <el-dialog
    :model-value="modelValue"
    title="离开填报页"
    width="460px"
    append-to-body
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="leave-confirm-body">{{ message }}</div>
    <template #footer>
      <el-button @click="emit('action', 'stay')">取消</el-button>
      <el-button type="danger" plain @click="emit('action', 'discard')">不保存并关闭</el-button>
      <el-button type="primary" :loading="saving" @click="emit('action', 'save')">保存草稿并离开</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
type LeaveConfirmAction = 'save' | 'discard' | 'stay';

defineProps<{
  modelValue: boolean;
  message: string;
  saving: boolean;
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'action', action: LeaveConfirmAction): void;
}>();
</script>

<style scoped lang="scss">
.leave-confirm-body {
  line-height: 1.7;
  color: var(--el-text-color-regular);
}
</style>
