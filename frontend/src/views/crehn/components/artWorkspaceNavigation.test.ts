import { describe, expect, it } from 'vitest';
import { normalizeWorkspaceNavigationUrl, resolveWorkspaceNavigationTarget, workspaceNavigationPresetsForRole } from './artWorkspaceNavigation';

describe('工作区跳转按钮', () => {
  it('按角色只提供适用的预设目标', () => {
    expect(workspaceNavigationPresetsForRole('school').map((item) => item.key)).toEqual(['schoolSubmit', 'schoolProjects', 'home']);
    expect(workspaceNavigationPresetsForRole('expert').map((item) => item.key)).toEqual(['reviewTasks', 'home']);
  });

  it('默认学校预设跳转统一提交独立页', () => {
    expect(
      resolveWorkspaceNavigationTarget(
        {
          targetMode: 'preset',
          presetKey: 'schoolSubmit',
          customUrl: ''
        },
        'school'
      )
    ).toBe('/crehn/project/submit');
  });

  it('只接受站内绝对路径以及 http 或 https 地址', () => {
    expect(normalizeWorkspaceNavigationUrl('/crehn/project')).toBe('/crehn/project');
    expect(normalizeWorkspaceNavigationUrl('https://example.com/a')).toBe('https://example.com/a');
    expect(normalizeWorkspaceNavigationUrl('//example.com/a')).toBe('');
    expect(normalizeWorkspaceNavigationUrl('javascript:alert(1)')).toBe('');
    expect(normalizeWorkspaceNavigationUrl('relative/path')).toBe('');
  });
});
