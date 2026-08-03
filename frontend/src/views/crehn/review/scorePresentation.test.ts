import { describe, expect, it } from 'vitest';
import { scoreModeLabel, scoreRecordText, scoreResultText } from './scorePresentation';

describe('score presentation', () => {
  it('labels all supported score modes', () => {
    expect(scoreModeLabel('numeric_100')).toBe('百分制');
    expect(scoreModeLabel('grade')).toBe('等级制');
    expect(scoreModeLabel('comment_only')).toBe('仅评语');
  });

  it('uses the mode-specific result without hiding comments', () => {
    expect(scoreResultText({ scoreMode: 'comment_only', commentText: '内容完整' })).toBe('内容完整');
    expect(scoreResultText({ scoreMode: 'grade', gradeValue: '优秀' })).toBe('优秀');
    expect(scoreResultText({ scoreMode: 'numeric_100', scoreValue: 90 })).toBe('90');
    expect(scoreResultText({ scoreMode: 'numeric_100', scoreValue: 90 }, true)).toBe('90.00');
  });

  it('keeps the existing reviewer record fallback order', () => {
    expect(scoreRecordText({ scoreValue: 90, gradeValue: '优秀', commentText: '意见' })).toBe('优秀');
    expect(scoreRecordText({ scoreValue: 90, commentText: '意见' })).toBe('90');
    expect(scoreRecordText({ commentText: '意见' })).toBe('意见');
  });
});
