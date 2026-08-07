import { describe, expect, it } from 'vitest';
import { competitionComponentRegistry, createCompetitionHomePreset, parseCompetitionComponentConfig } from './competitionHomeRegistry';

describe('competition home component registry', () => {
  it('keeps component types and template keys unique', () => {
    const types = competitionComponentRegistry.map((item) => item.type);
    expect(new Set(types).size).toBe(types.length);

    for (const version of ['v1', 'v2'] as const) {
      const preset = createCompetitionHomePreset(version, 1);
      expect(preset).toHaveLength(types.length);
      expect(new Set(preset.map((item) => item.componentKey)).size).toBe(preset.length);
      expect(preset.every((item) => item.gridW === 12 && item.dataSourceCode === 'manual')).toBe(true);
    }
  });

  it('merges stored fields over registered defaults', () => {
    const config = parseCompetitionComponentConfig({
      componentType: 'competition-hero',
      configJson: JSON.stringify({ title: '自定义标题' })
    });
    expect(config.title).toBe('自定义标题');
    expect(config.visualStyle).toBe('sculpture');
  });

  it('keeps editable scalar values unchanged through JSON serialization', () => {
    const config = parseCompetitionComponentConfig({
      componentType: 'competition-nav',
      configJson: JSON.stringify({ actionLabel: '立即报送', sticky: false })
    });

    expect(JSON.parse(JSON.stringify(config))).toMatchObject({ actionLabel: '立即报送', sticky: false });
  });
});
