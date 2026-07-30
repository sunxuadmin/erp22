// ART-OWNER: FE.PROJECT_EDIT.LEAVE_GUARD
import type { ComputedRef } from 'vue';
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';

export type LeaveConfirmAction = 'save' | 'discard' | 'stay';

interface UseProjectLeaveGuardOptions {
  hasUnsavedChanges: ComputedRef<boolean>;
  shouldConfirmClose: ComputedRef<boolean>;
  hasActiveUploads: ComputedRef<boolean>;
  saveDraft: () => Promise<unknown>;
  cancelAllUploads: () => void;
  closeCurrentProjectTab: () => Promise<void>;
  warn: (message: string) => void;
}

export function useProjectLeaveGuard(options: UseProjectLeaveGuardOptions) {
  const skipLeaveConfirm = ref(false);
  const leaveConfirm = reactive({
    visible: false,
    message: ''
  });
  let leaveConfirmResolver: ((action: LeaveConfirmAction) => void) | undefined;

  const requestLeaveConfirm = () => {
    if (leaveConfirmResolver) {
      leaveConfirmResolver('stay');
    }
    leaveConfirm.message = options.hasUnsavedChanges.value
      ? '当前填报内容还没有保存。离开前请选择保存草稿、不保存并关闭，或取消继续填写。'
      : '当前填报页仍处于可编辑状态。离开前可以再保存一次草稿，也可以直接关闭。';
    leaveConfirm.visible = true;
    return new Promise<LeaveConfirmAction>((resolve) => {
      leaveConfirmResolver = resolve;
    });
  };

  const resolveLeaveConfirm = (action: LeaveConfirmAction) => {
    leaveConfirm.visible = false;
    const resolver = leaveConfirmResolver;
    leaveConfirmResolver = undefined;
    resolver?.(action);
  };

  const confirmProjectLeave = async () => {
    if (skipLeaveConfirm.value || !options.shouldConfirmClose.value) return true;
    if (options.hasActiveUploads.value) {
      options.warn('文件正在上传，请等待完成或取消上传后再离开');
      return false;
    }
    const action = await requestLeaveConfirm();
    if (action === 'stay') return false;
    if (action === 'discard') {
      skipLeaveConfirm.value = true;
      await options.closeCurrentProjectTab();
      return true;
    }
    try {
      await options.saveDraft();
      skipLeaveConfirm.value = true;
      await options.closeCurrentProjectTab();
      return true;
    } catch {
      skipLeaveConfirm.value = false;
      return false;
    }
  };

  const beforeUnload = (event: BeforeUnloadEvent) => {
    if (!options.shouldConfirmClose.value) return;
    event.preventDefault();
    event.returnValue = '';
  };

  onBeforeRouteLeave(async () => confirmProjectLeave());

  onMounted(() => {
    window.addEventListener('beforeunload', beforeUnload);
  });

  onBeforeUnmount(() => {
    resolveLeaveConfirm('stay');
    options.cancelAllUploads();
    window.removeEventListener('beforeunload', beforeUnload);
  });

  return {
    skipLeaveConfirm,
    leaveConfirm,
    resolveLeaveConfirm,
    confirmProjectLeave
  };
}
