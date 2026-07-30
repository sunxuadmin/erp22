import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  applyDocumentLayoutDisplayScale,
  normalizeLayoutDisplayScaleMode,
  normalizeLayoutDisplayScalePercent,
  resolveAutoLayoutDisplayScalePercent,
  resolveLayoutDisplayScalePercent
} from './layoutDisplayScale';
import { compactWorkbenchStyleConfig, defaultWorkbenchStyleConfig, normalizeWorkbenchStyleConfig } from './workbenchStyle';

describe('layoutDisplayScale', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('normalizes unsupported modes and custom percentages', () => {
    expect(normalizeLayoutDisplayScaleMode('unknown')).toBe('auto');
    expect(normalizeLayoutDisplayScalePercent(60)).toBe(75);
    expect(normalizeLayoutDisplayScalePercent(120)).toBe(110);
    expect(normalizeLayoutDisplayScalePercent('95')).toBe(95);
  });

  it('maps fixed modes to one global scale', () => {
    expect(resolveLayoutDisplayScalePercent({ mode: 'default' })).toBe(100);
    expect(resolveLayoutDisplayScalePercent({ mode: 'small' })).toBe(90);
    expect(resolveLayoutDisplayScalePercent({ mode: 'large' })).toBe(110);
    expect(resolveLayoutDisplayScalePercent({ mode: 'custom', customScalePercent: 82 })).toBe(82);
  });

  it('uses unscaled available desktop width for automatic mode', () => {
    expect(resolveAutoLayoutDisplayScalePercent(1920, 54)).toBe(100);
    expect(resolveAutoLayoutDisplayScalePercent(1440, 54)).toBe(85);
    expect(resolveAutoLayoutDisplayScalePercent(1280, 54)).toBe(75);
    expect(resolveAutoLayoutDisplayScalePercent(1280, 260)).toBe(75);
  });

  it('leaves mobile layouts at 100 percent', () => {
    expect(resolveAutoLayoutDisplayScalePercent(768, 0, true)).toBe(100);
    expect(resolveLayoutDisplayScalePercent({ mode: 'auto', viewportWidth: 768, mobile: true })).toBe(100);
  });

  it('applies zoom without expanding the document width', () => {
    const setProperty = vi.fn();
    const toggle = vi.fn();
    const documentElement = {
      style: { setProperty },
      dataset: {} as Record<string, string>,
      classList: { toggle }
    };
    vi.stubGlobal('document', { documentElement });

    applyDocumentLayoutDisplayScale(85);

    expect(setProperty).toHaveBeenCalledWith('--app-display-scale', '0.8500');
    expect(setProperty).toHaveBeenCalledWith('--app-display-scale-inverse', '1.176471');
    expect(setProperty).toHaveBeenCalledWith('--app-display-viewport-width', '117.6471vw');
    expect(setProperty).toHaveBeenCalledWith('--app-display-viewport-height', '117.6471dvh');
    expect(setProperty).toHaveBeenCalledWith('--app-display-min-height', '117.6471vh');
    expect(setProperty).not.toHaveBeenCalledWith('--app-display-width', expect.any(String));
    expect(documentElement.dataset.appDisplayScale).toBe('85');
    expect(toggle).toHaveBeenCalledWith('is-app-display-scaled', true);
  });

  it.each([
    [90, '111.1111vw', '111.1111dvh', true],
    [100, '100.0000vw', '100.0000dvh', false],
    [110, '90.9091vw', '90.9091dvh', true]
  ])('keeps the overlay viewport compensated at %i%% display scale', (percent, viewportWidth, viewportHeight, scaled) => {
    const setProperty = vi.fn();
    const toggle = vi.fn();
    vi.stubGlobal('document', {
      documentElement: {
        style: { setProperty },
        dataset: {} as Record<string, string>,
        classList: { toggle }
      }
    });

    applyDocumentLayoutDisplayScale(percent);

    expect(setProperty).toHaveBeenCalledWith('--app-display-viewport-width', viewportWidth);
    expect(setProperty).toHaveBeenCalledWith('--app-display-viewport-height', viewportHeight);
    expect(toggle).toHaveBeenCalledWith('is-app-display-scaled', scaled);
  });

  it('keeps old workbench style JSON compatible with fixed default scale', () => {
    const normalized = normalizeWorkbenchStyleConfig({ enabled: true, preset: 'sky' });
    expect(normalized.layoutScaleMode).toBe('default');
    expect(normalized.customScalePercent).toBe(90);
    expect(compactWorkbenchStyleConfig(normalized)).toMatchObject({
      layoutScaleMode: 'default',
      customScalePercent: 90
    });
    expect(defaultWorkbenchStyleConfig.layoutScaleMode).toBe('default');
  });
});
