import type { ArtWorkspaceHeaderItemKey } from '@/api/crehn/detailDisplay';

export type WorkspaceHeaderOuterDropEdge = 'before' | 'after';

export interface WorkspaceHeaderOuterDropTarget {
  itemIndex: number;
  edge: WorkspaceHeaderOuterDropEdge;
}

export const shouldDropWorkspaceHeaderItemOutsideCompactGroup = (itemKey: ArtWorkspaceHeaderItemKey) =>
  itemKey === 'compactGroup' || itemKey === 'fixedSpacer' || itemKey === 'autoSpacer';

export const resolveWorkspaceHeaderOuterDropTarget = (
  targetItemIndex: number,
  clientX: number,
  targetLeft: number,
  targetWidth: number
): WorkspaceHeaderOuterDropTarget => {
  const edge: WorkspaceHeaderOuterDropEdge = targetWidth > 0 && clientX >= targetLeft + targetWidth / 2 ? 'after' : 'before';
  return {
    itemIndex: targetItemIndex + (edge === 'after' ? 1 : 0),
    edge
  };
};

export const adjustWorkspaceHeaderInsertIndex = (sourceItemIndex: number, targetItemIndex: number, sameRow: boolean) =>
  sameRow && sourceItemIndex < targetItemIndex ? targetItemIndex - 1 : targetItemIndex;
