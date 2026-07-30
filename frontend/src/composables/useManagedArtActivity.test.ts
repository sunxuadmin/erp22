import { describe, expect, it, vi } from 'vitest';
import type { ActivityVO } from '@/api/crehn/types';
import { resolveManagedArtActivity } from './useManagedArtActivity';

vi.mock('@/views/crehn/components/artDetailDisplayConfig', () => ({
  useArtDetailDisplayConfig: () => ({ detailDisplayConfig: { value: { workspaceHeader: {} } } })
}));

const activities = [
  { id: 101, activityName: '往届活动', status: 'ended' },
  { id: 202, activityName: '当前活动', status: 'enabled' }
] as ActivityVO[];

describe('resolveManagedArtActivity', () => {
  it('未单独配置时默认使用活动配置中的启用活动', () => {
    expect(resolveManagedArtActivity(activities)?.id).toBe(202);
  });

  it('显式配置活动时优先使用配置值', () => {
    expect(resolveManagedArtActivity(activities, '101')?.id).toBe(101);
  });

  it('显式配置不存在时不静默切换到其他活动', () => {
    expect(resolveManagedArtActivity(activities, '999')).toBeUndefined();
  });
});
