import { describe, expect, it } from 'vitest';
import {
  adjustWorkspaceHeaderInsertIndex,
  resolveWorkspaceHeaderOuterDropTarget,
  shouldDropWorkspaceHeaderItemOutsideCompactGroup
} from './artWorkspaceHeaderDrag';

describe('工作区表头拖放路由', () => {
  it('自动空白、固定空白和紧凑组保持在外层栅格', () => {
    expect(shouldDropWorkspaceHeaderItemOutsideCompactGroup('autoSpacer')).toBe(true);
    expect(shouldDropWorkspaceHeaderItemOutsideCompactGroup('fixedSpacer')).toBe(true);
    expect(shouldDropWorkspaceHeaderItemOutsideCompactGroup('compactGroup')).toBe(true);
    expect(shouldDropWorkspaceHeaderItemOutsideCompactGroup('search')).toBe(false);
  });

  it('根据目标组件左右半区计算前后插入位置', () => {
    expect(resolveWorkspaceHeaderOuterDropTarget(2, 120, 100, 100)).toEqual({ itemIndex: 2, edge: 'before' });
    expect(resolveWorkspaceHeaderOuterDropTarget(2, 180, 100, 100)).toEqual({ itemIndex: 3, edge: 'after' });
  });

  it('同行向后移动时补偿来源移除造成的索引变化', () => {
    expect(adjustWorkspaceHeaderInsertIndex(0, 3, true)).toBe(2);
    expect(adjustWorkspaceHeaderInsertIndex(3, 1, true)).toBe(1);
    expect(adjustWorkspaceHeaderInsertIndex(0, 3, false)).toBe(3);
  });
});
