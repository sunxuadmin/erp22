<template>
  <el-dialog
    v-bind="attrs"
    v-model="visible"
    :class="popupClasses"
    :close-on-click-modal="closeOnClickModal"
    :close-on-press-escape="closeOnPressEscape"
    :before-close="requestClose"
  >
    <div class="art-popup__content" @input.capture="markDirty" @change.capture="markDirty">
      <slot />
    </div>
    <template v-if="$slots.header" #header>
      <slot name="header" />
    </template>
    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus';
import './art-popup.scss';

defineOptions({ inheritAttrs: false });

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    dirty?: boolean;
    closeOnClickModal?: boolean;
    closeOnPressEscape?: boolean;
    guardUnsaved?: boolean;
    wide?: boolean;
  }>(),
  {
    closeOnClickModal: true,
    closeOnPressEscape: true,
    guardUnsaved: true,
    wide: false
  }
);

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
}>();

const attrs = useAttrs();
const localDirty = ref(false);
const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
});
const isDirty = computed(() => localDirty.value || Boolean(props.dirty));
const popupClasses = computed(() => ['art-popup', 'art-popup--dialog', { 'is-dirty': isDirty.value, 'is-wide': props.wide }]);

watch(
  () => props.modelValue,
  (opened) => {
    if (opened) localDirty.value = false;
  }
);

const markDirty = (event: Event) => {
  if (!props.guardUnsaved || localDirty.value) return;
  const target = event.target as HTMLElement | null;
  if (!target || target.closest('[data-art-popup-ignore-dirty]')) return;
  localDirty.value = true;
};

const requestClose = async (done: () => void) => {
  if (!props.guardUnsaved || !isDirty.value) {
    done();
    return;
  }
  try {
    await ElMessageBox.confirm('当前内容尚未保存，关闭后本次修改将丢失。', '确认关闭？', {
      confirmButtonText: '不保存并关闭',
      cancelButtonText: '继续编辑',
      confirmButtonClass: 'el-button--danger',
      type: 'warning',
      closeOnClickModal: false,
      closeOnPressEscape: false
    });
    localDirty.value = false;
    done();
  } catch {
    // 用户选择继续编辑，保持当前弹窗状态。
  }
};
</script>
