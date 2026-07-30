import { describe, expect, it } from 'vitest';
import { normalizeSchoolProjectCategoryAllLabel, schoolProjectColumnWidthSignature } from './schoolProjectSubmitConfig';

describe('school project submit configuration boundaries', () => {
  it('upgrades built-in all-category labels while preserving custom text', () => {
    expect(normalizeSchoolProjectCategoryAllLabel()).toBe('全部类别');
    expect(normalizeSchoolProjectCategoryAllLabel('全部')).toBe('全部类别');
    expect(normalizeSchoolProjectCategoryAllLabel('汇总')).toBe('全部类别');
    expect(normalizeSchoolProjectCategoryAllLabel('所有参评类别')).toBe('所有参评类别');
  });

  it('changes the personal-width signature when the backend layout changes', () => {
    const base = [{ key: 'projectName', width: 220, minWidth: 180, fixed: 'none', visible: true }];
    expect(schoolProjectColumnWidthSignature(base)).not.toBe(schoolProjectColumnWidthSignature([{ ...base[0], width: 260 }]));
    expect(schoolProjectColumnWidthSignature(base)).not.toBe(schoolProjectColumnWidthSignature([{ ...base[0], fixed: 'left' }]));
  });
});
