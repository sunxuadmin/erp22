import type { CategoryFileRequirementVO, ProjectFileVO, ProjectVO } from '@/api/crehn/types';
import { normalizeReviewMessage } from '@/utils/artReviewMessage';

const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg', 'tif', 'tiff'];
const videoExtensions = ['mp4', 'mov', 'webm', 'avi', 'mkv', 'flv', 'mpeg', 'mpg', 'm4v'];
const audioExtensions = ['mp3', 'wav', 'flac', 'aac', 'm4a', 'wma', 'ogg'];
const documentExtensions = ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'txt', 'rtf'];
const mediaTypeLabels: Record<string, string> = {
  image: '图片',
  video: '视频',
  audio: '音频',
  document: '文档',
  office: '文档',
  pdf: '文档'
};

const parseRule = (text?: string) => {
  if (!text) return {} as Record<string, any>;
  try {
    const parsed = JSON.parse(text);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? (parsed as Record<string, any>) : {};
  } catch {
    return {} as Record<string, any>;
  }
};

const numeric = (value: unknown) => {
  const result = Number(value);
  return Number.isFinite(result) && result > 0 ? result : undefined;
};

const uniqueText = (items: Array<string | undefined>) => [...new Set(items.map((item) => String(item || '').trim()).filter(Boolean))];

export const detailFileExtension = (file: ProjectFileVO) =>
  String(file.fileExt || file.originalName?.split('.').pop() || '')
    .trim()
    .replace(/^\./, '')
    .toLowerCase();

export const detailMediaTypeLabel = (file: ProjectFileVO) => {
  const rawType = String(file.mediaType || '')
    .trim()
    .toLowerCase();
  if (mediaTypeLabels[rawType]) return mediaTypeLabels[rawType];
  const mimeType = String(file.mimeType || '')
    .trim()
    .toLowerCase();
  if (mimeType.startsWith('image/')) return '图片';
  if (mimeType.startsWith('video/')) return '视频';
  if (mimeType.startsWith('audio/')) return '音频';
  const ext = detailFileExtension(file);
  if (imageExtensions.includes(ext)) return '图片';
  if (videoExtensions.includes(ext)) return '视频';
  if (audioExtensions.includes(ext)) return '音频';
  if (documentExtensions.includes(ext) || mimeType === 'application/pdf') return '文档';
  return ext || mimeType || file.fileTypeCode ? '文件' : '';
};

export const detailFileFormat = (file: ProjectFileVO) => {
  const ext = detailFileExtension(file);
  if (ext) return ext.toUpperCase();
  const mimeType = String(file.mimeType || '');
  const mimeFormat = mimeType.includes('/') ? mimeType.split('/')[1] : '';
  return mimeFormat ? mimeFormat.split(';')[0].toUpperCase() : '';
};

export const detailDuration = (duration?: number) => {
  const totalSeconds = Number(duration);
  if (!Number.isFinite(totalSeconds) || totalSeconds <= 0) return '';
  const roundedSeconds = Math.round(totalSeconds * 10) / 10;
  const hours = Math.floor(roundedSeconds / 3600);
  const minutes = Math.floor((roundedSeconds % 3600) / 60);
  const seconds = Math.round((roundedSeconds % 60) * 10) / 10;
  const secondsText = seconds > 0 ? `${seconds.toFixed(1).replace(/\.0$/, '')}秒` : '';
  if (hours > 0) return `${hours}小时${minutes > 0 ? `${minutes}分` : ''}${secondsText}`;
  if (minutes > 0) return `${minutes}分${secondsText}`;
  return secondsText || '0秒';
};

export const detailFileRequirement = (project: ProjectVO | undefined, file: ProjectFileVO) => {
  const requirements = project?.fileRequirements || [];
  return (
    requirements.find((item) => file.requirementId && String(item.id) === String(file.requirementId)) ||
    requirements.find((item) => file.fileTypeCode && String(item.fileTypeCode) === String(file.fileTypeCode))
  );
};

const codeTypeLabel = (code?: string) => {
  const value = String(code || '').toLowerCase();
  if (/video|movie/.test(value)) return '视频';
  if (/image|photo|picture/.test(value)) return '图片';
  if (/audio|music|sound/.test(value)) return '音频';
  if (/document|office|pdf|word|excel|ppt/.test(value)) return '文档';
  return value ? '文件' : '';
};

export const detailFileTypeLabel = (project: ProjectVO | undefined, file: ProjectFileVO) => {
  const requirement = detailFileRequirement(project, file);
  const requirementLabel = String(requirement?.fileTypeName || '').trim();
  const readableRequirementLabel = requirementLabel && requirementLabel !== String(file.fileTypeCode || '').trim() ? requirementLabel : '';
  const mediaLabel = detailMediaTypeLabel(file) || codeTypeLabel(file.fileTypeCode);
  if (readableRequirementLabel && mediaLabel && readableRequirementLabel !== mediaLabel) {
    return `${readableRequirementLabel}（${mediaLabel}）`;
  }
  return readableRequirementLabel || mediaLabel;
};

const formatSize = (size?: number) => {
  const value = Number(size);
  if (!Number.isFinite(value) || value <= 0) return '';
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  if (value < 1024 * 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`;
  return `${(value / 1024 / 1024 / 1024).toFixed(2)} GB`;
};

export const detailFilePageCount = (file: ProjectFileVO) => {
  const metadata = parseRule(file.metadataJson);
  const candidates = [
    metadata.pageCount,
    metadata.page_count,
    metadata.pages,
    metadata.nbPages,
    metadata.nb_pages,
    metadata.document?.pageCount,
    metadata.pdf?.pageCount,
    metadata.preview?.pageCount
  ];
  const value = candidates.map(Number).find((item) => Number.isFinite(item) && item > 0);
  return value ? Math.floor(value) : undefined;
};

const normalizedAllowedExt = (value?: string) => {
  const text = String(value || '').trim();
  if (!text || text === '*' || text.toLowerCase() === 'all') return '';
  return text
    .split(/[,，]/)
    .map((item) => item.trim().replace(/^\./, '').toUpperCase())
    .filter(Boolean)
    .join('、');
};

const manualTips = (requirement?: CategoryFileRequirementVO) => {
  const rule = parseRule(requirement?.ruleJson);
  const value = rule.manualCheckTips;
  if (Array.isArray(value)) return value.map((item) => normalizeReviewMessage(String(item))).filter(Boolean);
  if (typeof value === 'string') {
    return value
      .split(/[;；\n]/)
      .map((item) => normalizeReviewMessage(item.trim()))
      .filter(Boolean);
  }
  return [];
};

export const detailTechnicalRequirements = (project: ProjectVO | undefined, file: ProjectFileVO) => {
  const requirement = detailFileRequirement(project, file);
  if (!requirement) return '';
  const rule = parseRule(requirement.ruleJson);
  const requirements: Array<string | undefined> = [];
  const allowedExt = normalizedAllowedExt(requirement.allowedExt);
  if (allowedExt) requirements.push(`允许格式：${allowedExt}`);
  const minMb = numeric(rule.minMb);
  const maxMb = numeric(rule.maxMb) || numeric(requirement.maxSizeMb);
  if (minMb || maxMb) requirements.push(`大小：${minMb ? `≥${minMb} MB` : ''}${minMb && maxMb ? '，' : ''}${maxMb ? `≤${maxMb} MB` : ''}`);
  const minWidth = numeric(rule.minWidth);
  const minHeight = numeric(rule.minHeight);
  if (minWidth || minHeight) requirements.push(`分辨率：≥${minWidth || '-'} × ${minHeight || '-'}`);
  const minDuration = numeric(rule.minDurationSeconds);
  const maxDuration = numeric(rule.maxDurationSeconds);
  if (minDuration || maxDuration) {
    requirements.push(
      `时长：${minDuration ? `≥${detailDuration(minDuration)}` : ''}${minDuration && maxDuration ? '，' : ''}${maxDuration ? `≤${detailDuration(maxDuration)}` : ''}`
    );
  }
  const fps = numeric(rule.fps);
  if (fps) requirements.push(`帧率：${fps} fps${numeric(rule.fpsTolerance) ? ` ± ${numeric(rule.fpsTolerance)}` : ''}`);
  const bitrate = numeric(rule.minBitrateMbps);
  if (bitrate) requirements.push(`码率：≥${bitrate} Mbps`);
  const dpi = numeric(rule.dpi);
  if (dpi) requirements.push(`DPI：${dpi}`);
  if (rule.filenamePattern) requirements.push('文件名称：按模板校验');
  if (Array.isArray(rule.filenameRequiredParts) && rule.filenameRequiredParts.length) {
    requirements.push(`文件名称包含：${rule.filenameRequiredParts.map(String).join('、')}`);
  }
  requirements.push(...manualTips(requirement));
  if (requirement.tipText) requirements.push(normalizeReviewMessage(requirement.tipText));
  const values = uniqueText(requirements);
  return values.length ? `${values.join('；')}；请逐项核对` : '';
};

const isGenericManualMessage = (message?: string) => {
  const value = normalizeReviewMessage(message || '');
  return /技术参数已配置为人工复核|请提交后等待审核/.test(value);
};

const isManualCheckMode = (requirement?: CategoryFileRequirementVO) => {
  const rule = parseRule(requirement?.ruleJson);
  const mode = String(rule.technicalCheckMode || rule.mediaTechnicalCheckMode || '')
    .trim()
    .toLowerCase();
  return !!mode && ['manual', 'tip', 'warn', 'format_only', '提示', '人工'].some((item) => mode.includes(item));
};

export const detailCheckStatusLabel = (project: ProjectVO | undefined, file: ProjectFileVO) => {
  if (file.checkStatus === 'passed') return '自动校验通过';
  if (file.checkStatus === 'failed') return '自动校验不通过';
  const requirement = detailFileRequirement(project, file);
  if (file.checkStatus === 'warning') {
    return isManualCheckMode(requirement) || isGenericManualMessage(file.checkMessage) ? '人工复核' : '自动分析需复核';
  }
  return isManualCheckMode(requirement) ? '人工复核' : '未校验';
};

export const detailCheckFailureMessage = (project: ProjectVO | undefined, file: ProjectFileVO) => {
  const message = normalizeReviewMessage(file.checkMessage || '');
  if (!message || isGenericManualMessage(message)) return '';
  if (file.checkStatus === 'failed') return message;
  if (file.checkStatus === 'warning' && !isManualCheckMode(detailFileRequirement(project, file))) return message;
  return '';
};

export const detailFileFieldValue = (project: ProjectVO | undefined, file: ProjectFileVO, key: string) => {
  const values: Record<string, string> = {
    fileName: file.originalName || '',
    fileType: detailFileTypeLabel(project, file),
    fileExt: detailFileExtension(file).toUpperCase(),
    mimeType: file.mimeType || '',
    fileSize: formatSize(file.fileSize),
    mediaType: detailMediaTypeLabel(file),
    format: detailFileFormat(file),
    resolution: file.width && file.height ? `${file.width} × ${file.height}` : '',
    duration: detailDuration(file.durationSeconds),
    fps: file.fps && Number.isFinite(Number(file.fps)) ? `${Number(file.fps).toFixed(2)} fps` : '',
    bitrate: file.bitrate && Number.isFinite(Number(file.bitrate)) ? `${(Number(file.bitrate) / 1024 / 1024).toFixed(2)} Mbps` : '',
    dpi: file.dpi && Number.isFinite(Number(file.dpi)) ? String(file.dpi) : '',
    checkStatus: detailCheckStatusLabel(project, file),
    technicalRequirements: detailTechnicalRequirements(project, file)
  };
  return values[key] || '';
};

export const detailFileCompactSummary = (project: ProjectVO | undefined, file: ProjectFileVO) => {
  const pageCount = detailFilePageCount(file);
  return uniqueText([
    detailFileFieldValue(project, file, 'fileSize'),
    detailFileFieldValue(project, file, 'duration'),
    pageCount ? `${pageCount}页` : '',
    detailFileFieldValue(project, file, 'resolution')
  ]);
};
