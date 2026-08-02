import { describe, expect, it, vi } from 'vitest';

vi.mock('@/api/crehn/detailDisplay', () => ({
  getArtDetailDisplayConfig: vi.fn(),
  getArtListTableConfig: vi.fn(),
  updateArtDetailDisplayConfig: vi.fn(),
  updateArtListTableConfig: vi.fn()
}));

import { artListTablePageOptions, defaultArtListTableLayout, normalizeArtListTableLayout } from './artDetailDisplayConfig';

describe('business table page registration', () => {
  it('registers the three controlled business pages', () => {
    expect(artListTablePageOptions.map((item) => item.key)).toEqual([
      'project',
      'schoolSubmit',
      'audit',
      'review',
      'projectView',
      'reviewAssignment',
      'scoreSummary',
      'signedSheets'
    ]);
  });

  it('keeps required columns and protected actions in defaults', () => {
    const pages = defaultArtListTableLayout().pages;
    expect(pages.reviewAssignment.columns.map((column) => column.key)).toContain('actions');
    expect(pages.scoreSummary.columns.map((column) => column.key)).toEqual(
      expect.arrayContaining(['projectName', 'currentAverageScore', 'warningText', 'actions'])
    );
    expect(pages.signedSheets.columns.map((column) => column.key)).toEqual(
      expect.arrayContaining(['submissionMode', 'signedAt', 'withdrawalAudit', 'actions'])
    );
    expect(pages.reviewAssignment.columns.find((column) => column.key === 'actions')?.deleted).toBe(false);
  });

  it('normalizes the new pages and ignores unknown columns', () => {
    const normalized = normalizeArtListTableLayout({
      version: 1,
      pages: {
        scoreSummary: {
          columns: [
            { key: 'projectName', source: 'builtin', label: '节目', visible: true, deleted: false, width: 260, minWidth: 140, fixed: 'none' },
            { key: 'unknown', source: 'builtin', label: '不应出现', visible: true, deleted: false, width: 100, minWidth: 80, fixed: 'none' },
            { key: 'actions', source: 'builtin', label: '动作', visible: true, deleted: true, width: 120, minWidth: 120, fixed: 'none' }
          ]
        }
      } as any
    });
    const page = normalized.pages.scoreSummary;
    expect(page.columns.map((column) => column.key)).not.toContain('unknown');
    expect(page.columns.find((column) => column.key === 'projectName')?.label).toBe('节目');
    expect(page.columns.find((column) => column.key === 'actions')?.deleted).toBe(false);
  });
});
