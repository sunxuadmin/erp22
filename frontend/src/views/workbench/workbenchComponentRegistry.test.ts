import { describe, expect, it } from 'vitest';
import {
  WORKBENCH_COMPONENT_DEFINITIONS,
  WORKBENCH_COMPONENT_KEYS,
  WORKBENCH_SCHOOL_COMPONENT_KEYS,
  WORKBENCH_STAGE_NOTICE_COMPONENT_KEYS,
  workbenchComponentConfigKind,
  workbenchWidthToken
} from './workbenchComponentRegistry';

describe('workbench component registry', () => {
  it('keeps every runtime component key unique and registered', () => {
    const keys = Object.values(WORKBENCH_COMPONENT_KEYS);
    expect(new Set(keys).size).toBe(keys.length);
    expect(WORKBENCH_COMPONENT_DEFINITIONS.map((item) => item.key).sort()).toEqual([...keys].sort());
  });

  it('shares role filtering and editable config contracts', () => {
    expect(WORKBENCH_SCHOOL_COMPONENT_KEYS.has(WORKBENCH_COMPONENT_KEYS.schoolProjectList)).toBe(true);
    expect(WORKBENCH_STAGE_NOTICE_COMPONENT_KEYS.has(WORKBENCH_COMPONENT_KEYS.reviewStageNotice)).toBe(true);
    expect(workbenchComponentConfigKind(WORKBENCH_COMPONENT_KEYS.auditWorkbench)).toBe('auditWorkbench');
    expect(workbenchComponentConfigKind(WORKBENCH_COMPONENT_KEYS.schoolSubmission)).toBeUndefined();
  });

  it('normalizes unsupported layout widths to full width', () => {
    expect(workbenchWidthToken('1/2')).toBe('1/2');
    expect(workbenchWidthToken('unexpected')).toBe('1/1');
  });
});
