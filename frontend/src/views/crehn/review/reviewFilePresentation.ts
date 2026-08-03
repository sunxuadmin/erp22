import type { ProjectFileVO } from '@/api/crehn/types';
import { normalizePreviewMessage } from '@/utils/artReviewMessage';

const VIDEO_EXTENSIONS = ['mp4', 'mov', 'mpeg', 'mpg'];
const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'webp'];
const PREVIEW_EXTENSIONS = [...IMAGE_EXTENSIONS, ...VIDEO_EXTENSIONS, 'pdf'];

export const canPreview = (file: ProjectFileVO) => {
  const ext = (file.fileExt || '').toLowerCase();
  return PREVIEW_EXTENSIONS.includes(ext) || (file.previewStatus === 'converted' && !!file.previewPath);
};

export const previewTypeOf = (file?: ProjectFileVO) => {
  if (!file) return '';
  const ext = (file.fileExt || '').toLowerCase();
  if (file.previewStatus === 'converted' || ext === 'pdf') return 'pdf';
  if (VIDEO_EXTENSIONS.includes(ext)) return 'video';
  if (IMAGE_EXTENSIONS.includes(ext)) return 'image';
  return canPreview(file) ? 'pdf' : '';
};

export const previewTagType = (status?: string) => (status === 'converted' ? 'success' : status === 'failed' ? 'danger' : 'warning');

export const previewMessage = (file: ProjectFileVO) => normalizePreviewMessage(file.previewMessage, file.previewStatus);

export const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size >= 1024 * 1024 * 1024) return `${(size / 1024 / 1024 / 1024).toFixed(2)} GB`;
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(2)} MB`;
  return `${(size / 1024).toFixed(1)} KB`;
};

export const parseJsonObject = (text?: string): Record<string, unknown> => {
  if (!text) return {};
  try {
    const parsed: unknown = JSON.parse(text);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? (parsed as Record<string, unknown>) : {};
  } catch {
    return {};
  }
};

export const parseStringArray = (text?: string) => {
  if (!text) return [];
  try {
    const parsed: unknown = JSON.parse(text);
    return Array.isArray(parsed) ? parsed.map(String) : [];
  } catch {
    return [];
  }
};

const objectFieldLabel = (key: string) =>
  (
    ({
      name: '姓名',
      teacherName: '姓名',
      teacher_name: '姓名',
      instructorName: '姓名',
      instructor_name: '姓名',
      roleName: '角色',
      role_name: '角色',
      title: '职称',
      phone: '电话',
      mobile: '手机',
      unit: '单位',
      school: '学校',
      department: '院系',
      major: '专业'
    }) as Record<string, string>
  )[key] || key;

export const formatValue = (value: unknown): string => {
  if (value === undefined || value === null || value === '') return '-';
  if (Array.isArray(value)) return value.map((item) => formatValue(item)).filter((item) => item && item !== '-').join('、') || '-';
  if (typeof value === 'object') {
    const entries = Object.entries(value)
      .filter(([, item]) => item !== undefined && item !== null && item !== '')
      .map(([key, item]) => `${objectFieldLabel(key)}：${formatValue(item)}`);
    return entries.length ? entries.join('，') : JSON.stringify(value);
  }
  return String(value);
};

export const shouldExpandText = (text: string) => text.length > 64 || text.includes('\n');

export const shortText = (text: string) => (shouldExpandText(text) ? `${text.replace(/\s+/g, ' ').slice(0, 64)}...` : text);

export const pdfViewerUrl = (url?: string) => {
  if (!url) return '';
  return `${url.split('#')[0]}#toolbar=1&navpanes=0&scrollbar=1`;
};
