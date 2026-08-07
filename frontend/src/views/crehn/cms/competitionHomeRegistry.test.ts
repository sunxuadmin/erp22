import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import {
  competitionComponentConfigKeys,
  competitionComponentRegistry,
  createCompetitionHomePreset,
  getCompetitionDefaultConfig,
  parseCompetitionComponentConfig
} from './competitionHomeRegistry';

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

  it('creates distinct v1 and v2 field presets instead of changing only component keys', () => {
    const v1 = createCompetitionHomePreset('v1', 1);
    const v2 = createCompetitionHomePreset('v2', 1);
    const v1Hero = v1.find((item) => item.componentType === 'competition-hero');
    const v2Hero = v2.find((item) => item.componentType === 'competition-hero');
    const v1Tracks = v1.find((item) => item.componentType === 'competition-tracks');
    const v2Tracks = v2.find((item) => item.componentType === 'competition-tracks');

    expect(v1Hero?.configJson).not.toBe(v2Hero?.configJson);
    expect(v1Tracks?.configJson).not.toBe(v2Tracks?.configJson);
    expect(JSON.parse(v1Hero?.configJson || '{}').documentTitle).toBe('创意河南｜第六届全省高校艺术设计大赛');
    expect(JSON.parse(v2Hero?.configJson || '{}').documentTitle).toBe('创意河南｜艺术 × 科技明亮版');
  });

  it('keeps reference-specific enablement, deadline and direction codes in each version', () => {
    const v1 = createCompetitionHomePreset('v1', 1);
    const v2 = createCompetitionHomePreset('v2', 1);
    const config = (items: typeof v1, type: string) => JSON.parse(items.find((item) => item.componentType === type)?.configJson || '{}');

    expect(v1.find((item) => item.componentType === 'competition-art-tech')?.enabled).toBe(false);
    expect(v2.find((item) => item.componentType === 'competition-art-tech')?.enabled).toBe(true);
    expect(config(v1, 'competition-key-facts')).toMatchObject({ deadlineAt: '2026-10-01T00:00:00+08:00', invalidCountdownValue: '—' });
    expect(config(v2, 'competition-key-facts')).toMatchObject({ deadlineAt: '2026-10-01T00:00:00+08:00', invalidCountdownValue: '—' });
    expect(config(v1, 'competition-tracks').trackADirections).toContain('01|视觉传达');
    expect(config(v2, 'competition-tracks').trackADirections).toContain('V01|视觉传达');
    expect(config(v2, 'competition-art-tech').title).toBe('当创意连接算法、材料与空间，\n设计开始拥有新的感官。');
    expect(config(v2, 'competition-key-facts').countdownLabel).toBe('DAYS');
    expect(config(v2, 'competition-journey').stepPrefix).toBe('STEP');
    expect(config(v2, 'competition-notice-downloads').noticeArtMark).toBe('CH\n2026');
    expect(config(v2, 'competition-footer').brandMark).toBe('CH');
  });

  it('serializes every editable field as a scalar in both presets', () => {
    for (const version of ['v1', 'v2'] as const) {
      for (const component of createCompetitionHomePreset(version, 1)) {
        const config = JSON.parse(component.configJson || '{}') as Record<string, unknown>;
        const keys = competitionComponentConfigKeys[component.componentType] || [];
        expect(Object.keys(config).sort()).toEqual([...keys].sort());
        expect(Object.values(config).every((value) => ['string', 'number', 'boolean'].includes(typeof value))).toBe(true);
        expect(JSON.parse(JSON.stringify(config))).toEqual(config);
      }
    }
  });

  it('uses version defaults only for missing legacy fields and preserves explicit empty strings', () => {
    const config = parseCompetitionComponentConfig({
      componentKey: 'v2-hero',
      componentType: 'competition-hero',
      configJson: JSON.stringify({ competitionTitle: '', visualStyle: 'sculpture' })
    });

    expect(config.competitionTitle).toBe('');
    expect(config.eyebrow).toBe(getCompetitionDefaultConfig('competition-hero', 'v2').eyebrow);
    expect(config.yearText).toBe('2026');
    expect(config).not.toHaveProperty('visualStyle');
  });

  it('keeps every frontend component field synchronized with the Java whitelist', () => {
    const validator = readFileSync(
      resolve(process.cwd(), '../backend/ruoyi-modules/ruoyi-crehn/src/main/java/org/dromara/crehn/cms/service/impl/PortalHomeConfigurationValidator.java'),
      'utf8'
    );

    for (const [componentType, fields] of Object.entries(competitionComponentConfigKeys)) {
      const entry = validator.match(new RegExp(`Map\\.entry\\("${componentType}", Set\\.of\\(([^)]*)\\)\\)`, 's'));
      expect(entry, `${componentType} whitelist`).not.toBeNull();
      const javaFields = [...(entry?.[1] || '').matchAll(/"([^"]+)"/g)].map((match) => match[1]).sort();
      expect(javaFields).toEqual([...fields].sort());
    }
  });
});
