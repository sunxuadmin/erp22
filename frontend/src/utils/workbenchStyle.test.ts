import { describe, expect, it } from 'vitest';
import {
  cloneWorkbenchStyle,
  compactWorkbenchStyleConfig,
  defaultWorkbenchStyleConfig,
  isWorkbenchStyleConfigPayload,
  parseWorkbenchStyleConfig,
  workbenchStyleConfigsEqual,
  workbenchStylePreviewVars,
  workbenchStyleVars
} from './workbenchStyle';

describe('workbenchStyle', () => {
  it('isolates a disabled preview from workbench variables inherited from the document root', () => {
    const disabled = cloneWorkbenchStyle(defaultWorkbenchStyleConfig);
    disabled.enabled = false;

    expect(workbenchStyleVars(disabled)).toEqual({});

    const previewVars = workbenchStylePreviewVars(disabled);
    expect(previewVars['--workbench-page-background']).toBe('initial');
    expect(previewVars['--workbench-sidebar-bg']).toBe('initial');
    expect(previewVars['--workbench-card-bg']).toBe('initial');
    expect(Object.values(previewVars).every((value) => value === 'initial')).toBe(true);
  });

  it('uses the configured variables directly while preview styling is enabled', () => {
    const enabled = cloneWorkbenchStyle(defaultWorkbenchStyleConfig);
    enabled.accentColor = '#0891b2';

    expect(workbenchStylePreviewVars(enabled)).toEqual(workbenchStyleVars(enabled));
    expect(workbenchStylePreviewVars(enabled)['--workbench-accent']).toBe('#0891b2');
  });

  it('compares persisted workbench styles after compatible normalization', () => {
    const expected = cloneWorkbenchStyle(defaultWorkbenchStyleConfig);
    expected.enabled = false;
    expected.sidebarBackdropBlur = 6;
    const persisted = parseWorkbenchStyleConfig(JSON.stringify(compactWorkbenchStyleConfig(expected)));

    expect(workbenchStyleConfigsEqual(expected, persisted)).toBe(true);

    persisted.enabled = true;
    expect(workbenchStyleConfigsEqual(expected, persisted)).toBe(false);
  });

  it('rejects malformed or non-object server payloads before reporting save success', () => {
    expect(isWorkbenchStyleConfigPayload(JSON.stringify(compactWorkbenchStyleConfig(defaultWorkbenchStyleConfig)))).toBe(true);
    expect(isWorkbenchStyleConfigPayload(compactWorkbenchStyleConfig(defaultWorkbenchStyleConfig))).toBe(true);
    expect(isWorkbenchStyleConfigPayload('{"enabled":false')).toBe(false);
    expect(isWorkbenchStyleConfigPayload('[]')).toBe(false);
    expect(isWorkbenchStyleConfigPayload('')).toBe(false);
  });
});
