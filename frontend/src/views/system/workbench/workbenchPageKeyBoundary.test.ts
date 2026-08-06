import { describe, expect, it } from 'vitest';
import type { ArtListTablePageKey } from '@/api/crehn/detailDisplay';
import { isWorkspaceHeaderPageKey } from './workbenchPageKeyBoundary';

describe('workbench page key boundary', () => {
  it('only exposes pages backed by workspace header configuration', () => {
    const pageKeys: ArtListTablePageKey[] = [
      'project',
      'schoolSubmit',
      'audit',
      'review',
      'projectView',
      'reviewAssignment',
      'scoreSummary',
      'signedSheets'
    ];

    expect(pageKeys.filter(isWorkspaceHeaderPageKey)).toEqual(['project', 'schoolSubmit', 'audit', 'review', 'projectView']);
  });
});
