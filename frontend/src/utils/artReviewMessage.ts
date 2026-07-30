type MessageRule = {
  pattern: RegExp;
  replacement: string | ((match: RegExpMatchArray) => string);
};

const previewStatusLabel = (status?: string) =>
  (
    ({
      queued: 'PDF 预览任务已排队，请稍后刷新',
      converting: '正在生成 PDF 预览',
      processing: '正在生成预览',
      converted: 'PDF 预览已生成',
      failed: 'PDF 预览生成失败',
      not_required: '无需生成预览'
    }) as Record<string, string>
  )[status || ''] ||
  status ||
  '';

const applyRules = (text: string, rules: MessageRule[]) => {
  for (const rule of rules) {
    const match = text.match(rule.pattern);
    if (!match) continue;
    return typeof rule.replacement === 'function' ? rule.replacement(match) : text.replace(rule.pattern, rule.replacement);
  }
  return '';
};

const fileSuffix = (match: RegExpMatchArray, index = 1) => (match[index] ? `：${match[index].trim()}` : '');

const previewRules: MessageRule[] = [
  { pattern: /^PDF preview queued; please refresh later\.?$/i, replacement: 'PDF 预览任务已排队，请稍后刷新' },
  { pattern: /^PDF preview queued; retry requested\.?$/i, replacement: 'PDF 预览任务已重新排队，请稍后刷新' },
  { pattern: /^Previous PDF preview conversion was interrupted, requeued\.?$/i, replacement: '上一次 PDF 预览转换被中断，已重新排队' },
  { pattern: /^Generating PDF preview\.?$/i, replacement: '正在生成 PDF 预览' },
  { pattern: /^PDF preview generated\.?$/i, replacement: 'PDF 预览已生成' },
  { pattern: /^PDF preview conversion failed(?::\s*(.*))?$/i, replacement: (match) => `PDF 预览转换失败${fileSuffix(match)}` },
  {
    pattern: /^Office file uploaded,\s*PDF preview conversion failed(?::\s*(.*))?$/i,
    replacement: (match) => `文件已上传，但 PDF 预览转换失败${fileSuffix(match)}`
  },
  { pattern: /^Office file uploaded,\s*but PDF preview was not generated\.?$/i, replacement: '文件已上传，但未生成 PDF 预览文件' },
  { pattern: /^Office file uploaded,\s*but generated preview is not a valid PDF\.?$/i, replacement: '文件已上传，但生成的预览文件不是有效 PDF' },
  { pattern: /^Office file uploaded,\s*PDF preview conversion was interrupted\.?$/i, replacement: '文件已上传，但 PDF 预览转换被中断' },
  {
    pattern:
      /^Office file uploaded,\s*PDF preview conversion failed\.\s*Install LibreOffice\/soffice or configure crehn\.office\.soffice-path\.(?:\s*Details:\s*(.*))?$/i,
    replacement: (match) =>
      `文件已上传，但 PDF 预览转换失败。请安装 LibreOffice/soffice，或配置 crehn.office.soffice-path${match[1] ? `。详细信息：${match[1]}` : ''}`
  },
  { pattern: /^source file path is empty$/i, replacement: '源文件路径为空' }
];

const manualRules: MessageRule[] = [
  {
    pattern: /^Manual review:\s*video no province\/school\/name\/adviser confirmation was not checked\.?$/i,
    replacement: '人工审核：未勾选视频不含省份、学校、姓名、指导教师等身份信息确认项。'
  },
  {
    pattern: /^Manual review:\s*performance video must not show province, school, student name, or adviser name\.?$/i,
    replacement: '人工审核：表演视频不得出现省份、学校、学生姓名或指导教师姓名。'
  },
  { pattern: /^Manual review:\s*verify original or authorization commitment materials\.?$/i, replacement: '人工审核：请核验原创或授权承诺材料。' },
  {
    pattern:
      /^Manual review:\s*verify performance video format, resolution, frame rate, bitrate, duration, file size, fixed camera, and synchronous audio\/video\.?$/i,
    replacement: '人工审核：请核验表演视频格式、分辨率、帧率、码率、时长、文件大小、固定机位及音画同步情况。'
  },
  {
    pattern:
      /^Manual review:\s*verify artwork image\/video format, size, DPI, resolution, bitrate, duration, subtitles, and source attribution according to the activity notice\.?$/i,
    replacement: '人工审核：请按活动通知核验作品图片/视频格式、大小、DPI、分辨率、码率、时长、字幕和来源说明。'
  },
  { pattern: /^Manual review:\s*verify the paper has not been publicly published\.?$/i, replacement: '人工审核：请核验学术论文未公开发表承诺。' },
  {
    pattern: /^Manual review:\s*verify teaching reform case video format, size, and duration:\s*(.*)$/i,
    replacement: (match) => `人工审核：请核验教学改革案例视频格式、大小和时长${fileSuffix(match)}。`
  },
  {
    pattern: /^Manual review:\s*verify teaching reform case image format, size, and DPI:\s*(.*)$/i,
    replacement: (match) => `人工审核：请核验教学改革案例图片格式、大小和 DPI${fileSuffix(match)}。`
  },
  {
    pattern: /^Manual review:\s*verify workshop video format and duration; recommended duration is within\s*(\d+)\s*seconds\.?$/i,
    replacement: (match) => `人工审核：请核验工作坊视频格式和时长，建议时长不超过 ${match[1]} 秒。`
  },
  { pattern: /^Manual review:\s*verify the workshop has not previously won an award\.?$/i, replacement: '人工审核：请核验该工作坊是否未曾获奖。' }
];

const validationRules: MessageRule[] = [
  { pattern: /^performance project allows at most\s*(\d+)\s*advisers$/i, replacement: (match) => `艺术表演项目指导教师人数不能超过 ${match[1]} 人` },
  { pattern: /^original or authorization commitment is required$/i, replacement: '请确认原创或授权承诺' },
  {
    pattern: /^video no province\/school\/name\/adviser confirmation is required$/i,
    replacement: '请确认视频中不出现省份、学校、姓名、指导教师等身份信息'
  },
  { pattern: /^student can submit only one performance project$/i, replacement: '同一学生只能提交一个艺术表演项目' },
  { pattern: /^performance video must be MP4\/MOV:\s*(.*)$/i, replacement: (match) => `表演视频必须为 MP4/MOV 格式${fileSuffix(match)}` },
  {
    pattern: /^performance video must not exceed\s*(\d+(?:\.\d+)?)MB:\s*(.*)$/i,
    replacement: (match) => `表演视频大小不能超过 ${match[1]}MB${fileSuffix(match, 2)}`
  },
  {
    pattern: /^video resolution is below\s*(\d+)x(\d+):\s*(.*)$/i,
    replacement: (match) => `视频分辨率不能低于 ${match[1]}x${match[2]}${fileSuffix(match, 3)}`
  },
  {
    pattern: /^video frame rate must be about\s*(\d+(?:\.\d+)?)fps:\s*(.*)$/i,
    replacement: (match) => `视频帧率需约为 ${match[1]}fps${fileSuffix(match, 2)}`
  },
  {
    pattern: /^video bitrate is below\s*(\d+(?:\.\d+)?)Mbps:\s*(.*)$/i,
    replacement: (match) => `视频码率不能低于 ${match[1]}Mbps${fileSuffix(match, 2)}`
  },
  {
    pattern: /^performance video duration exceeds configured limit:\s*(.*)$/i,
    replacement: (match) => `表演视频时长超过配置限制${fileSuffix(match)}`
  },
  { pattern: /^film artwork allows at most\s*(\d+)\s*authors$/i, replacement: (match) => `影视类作品作者人数不能超过 ${match[1]} 人` },
  { pattern: /^artwork allows at most\s*(\d+)\s*authors$/i, replacement: (match) => `艺术作品作者人数不能超过 ${match[1]} 人` },
  { pattern: /^film artwork allows at most\s*(\d+)\s*advisers$/i, replacement: (match) => `影视类作品指导教师人数不能超过 ${match[1]} 人` },
  { pattern: /^regular artwork requires exactly\s*(\d+)\s*adviser$/i, replacement: (match) => `普通作品指导教师人数必须为 ${match[1]} 人` },
  {
    pattern: /^artwork creation description must not exceed\s*(\d+)\s*characters$/i,
    replacement: (match) => `作品创作说明不能超过 ${match[1]} 个字符`
  },
  { pattern: /^author can submit only one artwork$/i, replacement: '同一作者只能提交一件艺术作品' },
  { pattern: /^student can submit only one artwork$/i, replacement: '同一学生只能提交一件艺术作品' },
  { pattern: /^AIGC animation must fill AI tool usage ratio$/i, replacement: 'AIGC 动画短片请填写 AI 工具使用比例' },
  { pattern: /^AIGC animation must fill creation process description$/i, replacement: 'AIGC 动画短片请填写创作过程说明' },
  { pattern: /^artwork image must be JPG\/JPEG:\s*(.*)$/i, replacement: (match) => `作品图片必须为 JPG/JPEG 格式${fileSuffix(match)}` },
  {
    pattern: /^artwork image must be at least\s*(\d+(?:\.\d+)?)MB:\s*(.*)$/i,
    replacement: (match) => `作品图片大小不能小于 ${match[1]}MB${fileSuffix(match, 2)}`
  },
  { pattern: /^image DPI missing; manual review required:\s*(.*)$/i, replacement: (match) => `图片 DPI 无法识别，需人工复核${fileSuffix(match)}` },
  { pattern: /^artwork image DPI is below\s*(\d+):\s*(.*)$/i, replacement: (match) => `作品图片 DPI 不能低于 ${match[1]}${fileSuffix(match, 2)}` },
  { pattern: /^film artwork video must be MP4\/MOV:\s*(.*)$/i, replacement: (match) => `影视类作品视频必须为 MP4/MOV 格式${fileSuffix(match)}` },
  {
    pattern: /^film artwork video must not exceed\s*(\d+(?:\.\d+)?)MB:\s*(.*)$/i,
    replacement: (match) => `影视类作品视频大小不能超过 ${match[1]}MB${fileSuffix(match, 2)}`
  },
  {
    pattern: /^film artwork video duration exceeds configured limit:\s*(.*)$/i,
    replacement: (match) => `影视类作品视频时长超过配置限制${fileSuffix(match)}`
  },
  {
    pattern: /^film artwork video bitrate is below configured limit:\s*(.*)$/i,
    replacement: (match) => `影视类作品视频码率低于配置要求${fileSuffix(match)}`
  },
  { pattern: /^paper allows at most\s*(\d+)\s*authors$/i, replacement: (match) => `学术论文作者人数不能超过 ${match[1]} 人` },
  {
    pattern: /^paper abstract should be\s*(\d+)\s*to\s*(\d+)\s*characters$/i,
    replacement: (match) => `学术论文摘要应为 ${match[1]} 至 ${match[2]} 个字符`
  },
  { pattern: /^paper requires\s*(\d+)\s*to\s*(\d+)\s*keywords$/i, replacement: (match) => `学术论文关键词应为 ${match[1]} 至 ${match[2]} 个` },
  { pattern: /^paper body must be at least\s*(\d+)\s*characters$/i, replacement: (match) => `学术论文正文不能少于 ${match[1]} 个字符` },
  { pattern: /^paper references are required$/i, replacement: '请填写学术论文参考文献' },
  { pattern: /^paper unpublished commitment is required$/i, replacement: '请确认学术论文未公开发表承诺' },
  { pattern: /^teaching reform case must be submitted by an organization\/unit$/i, replacement: '教学改革案例须以组织或单位名义提交' },
  { pattern: /^teaching reform case allows at most\s*(\d+)\s*completers$/i, replacement: (match) => `教学改革案例完成人不能超过 ${match[1]} 人` },
  {
    pattern: /^teaching reform case text must not exceed\s*(\d+)\s*characters$/i,
    replacement: (match) => `教学改革案例正文不能超过 ${match[1]} 个字符`
  },
  { pattern: /^teaching reform case must include background$/i, replacement: '教学改革案例需包含背景' },
  { pattern: /^teaching reform case must include practices$/i, replacement: '教学改革案例需包含做法' },
  { pattern: /^teaching reform case must include effects$/i, replacement: '教学改革案例需包含成效' },
  { pattern: /^teaching reform case must include suggestions$/i, replacement: '教学改革案例需包含建议' },
  { pattern: /^teaching reform case allows at most\s*(\d+)\s*optional video$/i, replacement: (match) => `教学改革案例视频不能超过 ${match[1]} 个` },
  {
    pattern: /^teaching reform case video must be MP4\/MOV:\s*(.*)$/i,
    replacement: (match) => `教学改革案例视频必须为 MP4/MOV 格式${fileSuffix(match)}`
  },
  {
    pattern: /^teaching reform case video must not exceed\s*(\d+(?:\.\d+)?)MB:\s*(.*)$/i,
    replacement: (match) => `教学改革案例视频大小不能超过 ${match[1]}MB${fileSuffix(match, 2)}`
  },
  {
    pattern: /^teaching reform case video duration exceeds configured limit:\s*(.*)$/i,
    replacement: (match) => `教学改革案例视频时长超过配置限制${fileSuffix(match)}`
  },
  { pattern: /^teaching reform case allows at most\s*(\d+)\s*optional images$/i, replacement: (match) => `教学改革案例图片不能超过 ${match[1]} 张` },
  {
    pattern: /^teaching reform case image must be JPG\/JPEG:\s*(.*)$/i,
    replacement: (match) => `教学改革案例图片必须为 JPG/JPEG 格式${fileSuffix(match)}`
  },
  {
    pattern: /^teaching reform case image must be at least\s*(\d+(?:\.\d+)?)MB:\s*(.*)$/i,
    replacement: (match) => `教学改革案例图片大小不能小于 ${match[1]}MB${fileSuffix(match, 2)}`
  },
  {
    pattern: /^teaching reform case image DPI is below\s*(\d+):\s*(.*)$/i,
    replacement: (match) => `教学改革案例图片 DPI 不能低于 ${match[1]}${fileSuffix(match, 2)}`
  },
  { pattern: /^workshop requires\s*(\d+)\s*to\s*(\d+)\s*students$/i, replacement: (match) => `工作坊学生人数应为 ${match[1]} 至 ${match[2]} 人` },
  { pattern: /^workshop requires\s*(\d+)\s*to\s*(\d+)\s*advisers$/i, replacement: (match) => `工作坊指导教师人数应为 ${match[1]} 至 ${match[2]} 人` },
  { pattern: /^workshop total member count must not exceed\s*(\d+)$/i, replacement: (match) => `工作坊总人数不能超过 ${match[1]} 人` },
  { pattern: /^same school can submit only one workshop in this activity category$/i, replacement: '同一学校在当前活动类别下只能提交一个工作坊' },
  { pattern: /^workshop video must be MP4\/MPG\/MPEG:\s*(.*)$/i, replacement: (match) => `工作坊视频必须为 MP4/MPG/MPEG 格式${fileSuffix(match)}` },
  {
    pattern: /^workshop video duration exceeds configured limit:\s*(.*)$/i,
    replacement: (match) => `工作坊视频时长超过配置限制${fileSuffix(match)}`
  },
  { pattern: /^workshop no previous award commitment is required$/i, replacement: '请确认工作坊未曾获奖承诺' },
  { pattern: /^required workshop field missing:\s*(.*)$/i, replacement: (match) => `工作坊必填字段缺失${fileSuffix(match)}` }
];

const genericManualText = (text: string) => {
  if (!/^Manual review:/i.test(text)) return '';
  return text
    .replace(/^Manual review:\s*/i, '人工审核：')
    .replace(/\bverify\b/gi, '请核验')
    .replace(/\bvideo\b/gi, '视频')
    .replace(/\bimage\b/gi, '图片')
    .replace(/\bformat\b/gi, '格式')
    .replace(/\bfile size\b/gi, '文件大小')
    .replace(/\bsize\b/gi, '大小')
    .replace(/\bduration\b/gi, '时长')
    .replace(/\bframe rate\b/gi, '帧率')
    .replace(/\bbitrate\b/gi, '码率')
    .replace(/\bresolution\b/gi, '分辨率')
    .replace(/\badviser\b/gi, '指导教师');
};

export const normalizeReviewMessage = (message?: string, status?: string) => {
  const text = String(message || '').trim();
  if (!text) return status ? previewStatusLabel(status) : '';
  const withoutRetry = text.replace(/;\s*max retry reached$/i, '；已达到最大重试次数');
  return (
    applyRules(withoutRetry, previewRules) ||
    applyRules(withoutRetry, manualRules) ||
    applyRules(withoutRetry, validationRules) ||
    genericManualText(withoutRetry) ||
    withoutRetry
  );
};

export const normalizePreviewMessage = (message?: string, status?: string) => {
  const normalized = normalizeReviewMessage(message);
  return normalized || previewStatusLabel(status);
};
