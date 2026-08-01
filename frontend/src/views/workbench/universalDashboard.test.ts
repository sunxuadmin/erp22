import { describe, expect, it } from 'vitest';
import { defaultUniversalDashboardConfig, resolveDashboardRole } from './universalDashboard';

describe('universal dashboard role mapping', () => {
  it.each([
    ['superadmin', 'admin'],
    ['crehn_admin', 'admin'],
    ['crehn_sub_admin', 'admin'],
    ['crehn_school', 'school'],
    ['crehn_participant', 'generic'],
    ['crehn_auditor', 'auditor'],
    ['crehn_reviewer', 'reviewer'],
    ['crehn_score_summary', 'viewer'],
    ['crehn_result_admin', 'viewer'],
    ['crehn_cms_editor', 'generic'],
    ['crehn_cms_publisher', 'generic'],
    ['crehn_audit_supervisor', 'viewer']
  ] as const)('maps %s to %s', (roleKey, expected) => {
    expect(resolveDashboardRole(roleKey)).toBe(expected);
  });

  it('does not treat a participant as a school role only because a school id exists', () => {
    expect(resolveDashboardRole('crehn_participant', 100)).toBe('generic');
  });

  it('keeps the existing administrator priority when a user also has the participant role', () => {
    expect(resolveDashboardRole(['crehn_admin', 'crehn_participant'])).toBe('admin');
  });

  it('provides a safe empty default dashboard for roles without registered data sources', () => {
    const config = defaultUniversalDashboardConfig('generic');
    expect(config.blocks).toEqual([]);
    expect(config.headerAction.visible).toBe(false);
  });
});
