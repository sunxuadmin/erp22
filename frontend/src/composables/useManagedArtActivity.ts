import type { ActivityVO } from '@/api/crehn/types';
import { useArtDetailDisplayConfig } from '@/views/crehn/components/artDetailDisplayConfig';
import type { Ref } from 'vue';

export const resolveManagedArtActivity = (activityOptions: ActivityVO[], configuredActivityId?: string | number) => {
  const configuredId = String(configuredActivityId ?? '').trim();
  if (configuredId) {
    return activityOptions.find((item) => String(item.id) === configuredId);
  }
  return activityOptions.find((item) => item.status === 'enabled');
};

export const useManagedArtActivity = (activityOptions: Ref<ActivityVO[]>) => {
  const { detailDisplayConfig } = useArtDetailDisplayConfig();
  const configuredActivityId = computed(() => String(detailDisplayConfig.value.workspaceHeader.managedActivityId || '').trim());
  const managedActivity = computed(() => resolveManagedArtActivity(activityOptions.value, configuredActivityId.value));
  const managedActivityId = computed(() => String(managedActivity.value?.id || configuredActivityId.value));
  const managedActivityError = computed(() => {
    if (configuredActivityId.value && !managedActivity.value) return '管理员指定的当前活动不在您的可用范围内';
    if (!managedActivity.value) return '当前没有可用的启用活动';
    return '';
  });
  const resolveManagedActivityId = () => managedActivity.value?.id;

  return { managedActivityId, managedActivity, managedActivityError, resolveManagedActivityId };
};
