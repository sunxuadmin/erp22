import type { ArtListTablePageKey, ArtWorkspaceHeaderPageKey } from '@/api/crehn/detailDisplay';

const workspaceHeaderPageKeys = new Set<ArtListTablePageKey>(['project', 'schoolSubmit', 'audit', 'review', 'projectView']);

export const isWorkspaceHeaderPageKey = (pageKey: ArtListTablePageKey): pageKey is ArtWorkspaceHeaderPageKey => workspaceHeaderPageKeys.has(pageKey);
