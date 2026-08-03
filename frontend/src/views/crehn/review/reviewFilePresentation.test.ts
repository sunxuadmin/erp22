import { describe, expect, it } from 'vitest';
import { formatSize, formatValue, parseJsonObject, pdfViewerUrl, previewTypeOf, shortText } from './reviewFilePresentation';

describe('review file presentation', () => {
  it('resolves preview types from file extension and conversion status', () => {
    expect(previewTypeOf({ fileExt: 'PDF' })).toBe('pdf');
    expect(previewTypeOf({ fileExt: 'mp4' })).toBe('video');
    expect(previewTypeOf({ fileExt: 'png' })).toBe('image');
    expect(previewTypeOf({ fileExt: 'docx', previewStatus: 'converted' })).toBe('pdf');
  });

  it('formats file sizes and viewer URLs', () => {
    expect(formatSize(1024 * 1024)).toBe('1.00 MB');
    expect(formatSize()).toBe('-');
    expect(pdfViewerUrl('https://example.test/file.pdf#page=2')).toBe('https://example.test/file.pdf#toolbar=1&navpanes=0&scrollbar=1');
  });

  it('keeps field parsing and formatting readable', () => {
    expect(parseJsonObject('{"name":"李明"}')).toEqual({ name: '李明' });
    expect(parseJsonObject('[]')).toEqual({});
    expect(formatValue({ name: '李明', major: '绘画' })).toBe('姓名：李明，专业：绘画');
    expect(shortText('a'.repeat(65))).toBe(`${'a'.repeat(64)}...`);
  });
});
