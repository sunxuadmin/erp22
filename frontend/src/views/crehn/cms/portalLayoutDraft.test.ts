import { describe, expect, it } from 'vitest';
import type { PortalHomeComponent, PortalPageLayout } from '@/api/crehn/cms';
import { createPortalLayoutComponentDraft, parsePortalLayoutComponents } from './portalLayoutDraft';

const component = (componentKey: string, gridX: number): PortalHomeComponent => ({
  id: componentKey,
  siteId: 1,
  componentKey,
  componentType: 'hero',
  dataSourceCode: 'manual',
  configJson: '{}',
  gridX,
  gridY: 0,
  gridW: 6,
  gridH: 1,
  enabled: true
});

describe('portal layout component draft', () => {
  it('preserves the selected inactive layout snapshot instead of the current canvas', () => {
    const saved = [component('saved', 1)];
    const current = [component('current', 5)];
    const layout: PortalPageLayout = { active: false, componentJson: JSON.stringify(saved) };

    const draft = createPortalLayoutComponentDraft(layout, current);

    expect(draft).toEqual(saved);
    expect(draft).not.toBe(saved);
  });

  it('uses the current canvas for the active layout so drag changes can be saved', () => {
    const saved = [component('saved', 1)];
    const current = [component('saved', 7)];
    const layout: PortalPageLayout = { active: true, componentJson: JSON.stringify(saved) };

    expect(createPortalLayoutComponentDraft(layout, current)).toEqual(current);
  });

  it('uses the current canvas for a new layout', () => {
    const current = [component('new-layout', 3)];

    expect(createPortalLayoutComponentDraft(undefined, current)).toEqual(current);
  });

  it('blocks an existing layout whose component snapshot is invalid', () => {
    expect(createPortalLayoutComponentDraft({ active: false, componentJson: '{invalid' }, [])).toBeUndefined();
    expect(createPortalLayoutComponentDraft({ active: true, componentJson: '{invalid' }, [])).toBeUndefined();
    expect(parsePortalLayoutComponents('{}')).toBeUndefined();
  });
});
