import type {
  ReviewScoreSheetColumnVO,
  ReviewScoreSheetFooterFieldVO,
  ReviewScoreSheetFooterRowVO,
  ReviewScoreSheetSignaturePlacementVO,
  ReviewScoreSheetSignatureVO,
  ReviewScoreSheetTemplateVO,
  ReviewTaskVO
} from '@/api/crehn/types';
import { scoreModeLabel, scoreResultText } from '../review/scorePresentation';

export const defaultScoreSheetColumns = (): ReviewScoreSheetColumnVO[] => [
  { key: 'projectNo', label: '项目编号', visible: true },
  { key: 'projectName', label: '项目名称', visible: true },
  { key: 'categoryName', label: '类别', visible: true },
  { key: 'scoreMode', label: '评分模式', visible: true },
  { key: 'status', label: '状态', visible: false },
  { key: 'scoreResult', label: '评审结果', visible: true },
  { key: 'scoreTime', label: '评分时间', visible: true },
  { key: 'reviewerName', label: '评审老师', visible: true }
];

export const defaultScoreSheetFooterRows = (): ReviewScoreSheetFooterRowVO[] => [
  {
    left: { label: '时间：', type: 'text', lineLength: 'medium' },
    right: {
      label: '评分老师签字：',
      type: 'signature',
      lineLength: 'long',
      slotKey: 'reviewer_signature',
      signatureButtonText: '点击签名',
      signatureHintText: '或拖入默认签名',
      signatureTimeText: '签名时间：',
      signatureRequired: true,
      signatureTimeVisible: true
    }
  }
];

export const emptyScoreSheetTemplate = (templateName = '新评分表模板'): ReviewScoreSheetTemplateVO => ({
  templateName,
  title: '评审打分表',
  columns: defaultScoreSheetColumns(),
  footerRows: defaultScoreSheetFooterRows(),
  defaultTemplate: false,
  version: 0
});

export const cloneScoreSheetTemplate = (template: ReviewScoreSheetTemplateVO): ReviewScoreSheetTemplateVO => {
  const cloned = JSON.parse(JSON.stringify(template)) as ReviewScoreSheetTemplateVO;
  cloned.columns = cloned.columns || defaultScoreSheetColumns();
  cloned.footerRows = cloned.footerRows || defaultScoreSheetFooterRows();
  return cloned;
};

export const scoreTaskToPreviewRow = (task: ReviewTaskVO, reviewerName = '评审老师'): Record<string, string> => ({
  projectNo: task.projectNo || '-',
  projectName: task.projectName || '-',
  categoryName: task.categoryName || '-',
  scoreMode: scoreModeLabel(task.scoreMode),
  status: '已提交',
  scoreResult: scoreResultText(task, true),
  scoreTime: task.scoreSubmittedAt || '-',
  reviewerName: reviewerName || '评审老师'
});

export const exampleScoreSheetRows = (): Array<Record<string, string>> => [
  {
    projectNo: 'AR20260001',
    projectName: '示例项目',
    categoryName: '声乐',
    scoreMode: '百分制',
    status: '已提交',
    scoreResult: '90.00',
    scoreTime: '2026-07-14 10:30:00',
    reviewerName: '评审老师'
  }
];

export const validateScoreSheetTemplate = (template: ReviewScoreSheetTemplateVO): string | undefined => {
  if (!template.templateName?.trim()) return '请输入模板名称';
  if (!template.title?.trim()) return '请输入表格标题';
  if ((template.columns || []).filter((column) => column.visible).length < 2) return '至少需要显示两列';
  if ((template.columns || []).some((column) => !column.label?.trim())) return '列名称不能为空';
  if ((template.footerRows || []).length > 6) return '底部签字区最多配置6行';
  if ((template.footerRows || []).some((row) => !row.left?.label?.trim() && !row.right?.label?.trim())) {
    return '底部签字区每行至少配置一个字段';
  }
  const signatureCount = (template.footerRows || [])
    .flatMap((row) => [row.left, row.right])
    .filter((field) => isScoreSheetSignatureField(field)).length;
  if (signatureCount > 1) return '评分表最多配置一个评分老师签名槽';
  return undefined;
};

/** Compatibility keeps existing “评分/评审老师签字” templates usable before they are saved again. */
export const isScoreSheetSignatureField = (field?: ReviewScoreSheetFooterFieldVO | null) => {
  if (!field) return false;
  if (field.type === 'signature') return true;
  return /评(?:分|审|委).*老师?.*签(?:字|名)/.test(String(field.label || '').replace(/\s/g, ''));
};

export const scoreSheetSignatureSlotKey = (field?: ReviewScoreSheetFooterFieldVO | null) => {
  if (!isScoreSheetSignatureField(field)) return '';
  return String(field?.slotKey || 'reviewer_signature');
};

/** Keep older template snapshots readable while allowing each signature slot
 * to customize its editing guidance and the formal timestamp prefix. */
export const scoreSheetSignatureButtonText = (field?: ReviewScoreSheetFooterFieldVO | null) => field?.signatureButtonText ?? '点击签名';

export const scoreSheetSignatureHintText = (field?: ReviewScoreSheetFooterFieldVO | null) => field?.signatureHintText ?? '或拖入默认签名';

export const scoreSheetSignatureTimeText = (field?: ReviewScoreSheetFooterFieldVO | null) => field?.signatureTimeText ?? '签名时间：';

/** Old templates remain signature-required until an administrator explicitly changes the setting. */
export const scoreSheetSignatureRequired = (field?: ReviewScoreSheetFooterFieldVO | null) => field?.signatureRequired !== false;

/** This only affects visible output; the server still retains the submission audit time. */
export const scoreSheetSignatureTimeVisible = (field?: ReviewScoreSheetFooterFieldVO | null) => field?.signatureTimeVisible !== false;

const escapePrintHtml = (value: unknown) =>
  String(value ?? '').replace(/[&<>"']/g, (character) => {
    const entities: Record<string, string> = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#39;'
    };
    return entities[character];
  });

const printColumnWeights: Record<string, number> = {
  projectNo: 16,
  projectName: 26,
  categoryName: 12,
  scoreMode: 10,
  status: 10,
  scoreResult: 11,
  scoreTime: 17,
  reviewerName: 12
};

const renderPrintFooterField = (
  field: ReviewScoreSheetFooterRowVO['left'],
  exportTime: string,
  side: 'left' | 'right',
  signaturePlacement?: ReviewScoreSheetSignaturePlacementVO | null,
  signature?: ReviewScoreSheetSignatureVO | null
) => {
  if (!field?.label) return `<div class="footer-side is-${side}"></div>`;
  const value = field.type === 'exportTime' ? `<span class="footer-time">${escapePrintHtml(exportTime)}</span>` : '';
  const line = field.type !== 'exportTime' && field.lineLength !== 'none' ? `<span class="footer-line is-${field.lineLength}"></span>` : '';
  const slotKey = scoreSheetSignatureSlotKey(field);
  const isPlaced = !!slotKey && signaturePlacement?.slotKey === slotKey;
  const imageUrl = isPlaced ? signaturePlacement?.signatureUrl || signature?.imageUrl || signature?.url : '';
  const placementStyle = isPlaced
    ? `left:${Math.max(0, Math.min(1, Number(signaturePlacement?.x ?? 0))) * 100}%;top:${Math.max(0, Math.min(1, Number(signaturePlacement?.y ?? 0))) * 100}%;width:${Math.max(0.01, Math.min(1, Number(signaturePlacement?.width ?? 0.54))) * 100}%;height:${Math.max(0.01, Math.min(1, Number(signaturePlacement?.height ?? 0.76))) * 100}%;`
    : '';
  const signatureVisible = !!slotKey && (isPlaced || scoreSheetSignatureRequired(field));
  const signatureHtml = signatureVisible
    ? `<span class="signature-slot">${line}${imageUrl ? `<img class="signature-image" src="${escapePrintHtml(imageUrl)}" alt="评分老师签名" style="${placementStyle}">` : ''}${
        isPlaced && signaturePlacement?.signedAt && scoreSheetSignatureTimeVisible(field)
          ? `<span class="signature-time">${escapePrintHtml(scoreSheetSignatureTimeText(field))}${escapePrintHtml(signaturePlacement.signedAt)}</span>`
          : ''
      }</span>`
    : slotKey
      ? ''
      : line;
  if (slotKey && !signatureVisible) return `<div class="footer-side is-${side}"></div>`;
  return `<div class="footer-side is-${side}"><span class="footer-label">${escapePrintHtml(field.label)}</span>${value}${signatureHtml}</div>`;
};

export const openScoreSheetPrintPreview = (
  template: ReviewScoreSheetTemplateVO,
  rows: Array<Record<string, string | number | undefined>>,
  exportTime: string,
  signaturePlacement?: ReviewScoreSheetSignaturePlacementVO | null,
  signature?: ReviewScoreSheetSignatureVO | null
) => {
  const visibleColumns = (template.columns || []).filter((column) => column.visible);
  const totalWeight = visibleColumns.reduce((sum, column) => sum + (printColumnWeights[column.key] || 12), 0);
  const colgroup = visibleColumns
    .map((column) => `<col style="width:${(((printColumnWeights[column.key] || 12) / totalWeight) * 100).toFixed(2)}%">`)
    .join('');
  const headers = visibleColumns.map((column) => `<th>${escapePrintHtml(column.label)}</th>`).join('');
  const body = rows
    .map(
      (row) =>
        `<tr>${visibleColumns
          .map((column) => `<td class="${column.key === 'projectName' ? 'is-left' : ''}">${escapePrintHtml(row[column.key] ?? '-')}</td>`)
          .join('')}</tr>`
    )
    .join('');
  const footer = (template.footerRows || [])
    .map(
      (row) =>
        `<div class="footer-row">${renderPrintFooterField(row.left, exportTime, 'left', signaturePlacement, signature)}${renderPrintFooterField(
          row.right,
          exportTime,
          'right',
          signaturePlacement,
          signature
        )}</div>`
    )
    .join('');
  const title = escapePrintHtml(template.title || '评审打分表');
  const printWindow = window.open('', '_blank', 'width=1280,height=900');
  if (!printWindow) return false;

  printWindow.opener = null;
  printWindow.document.open();
  printWindow.document.write(`<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${title}</title>
  <style>
    @page { size: A4 landscape; margin: 12mm; }
    * { box-sizing: border-box; }
    body { margin: 0; color: #111827; font-family: "Microsoft YaHei", "PingFang SC", sans-serif; font-size: 10.5pt; }
    h1 { margin: 0 0 8mm; font-size: 18pt; text-align: center; }
    table { width: 100%; border-collapse: collapse; table-layout: fixed; }
    thead { display: table-header-group; }
    tr { break-inside: avoid; page-break-inside: avoid; }
    th, td { padding: 2.2mm 1.8mm; border: 0.3mm solid #64748b; text-align: center; vertical-align: middle; word-break: break-word; }
    th { background: #eaf1fb; font-weight: 700; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    td.is-left { text-align: left; }
    .footer-list { display: flex; flex-direction: column; gap: 5mm; margin-top: 9mm; }
    .footer-row { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 16mm; min-height: 7mm; break-inside: avoid; page-break-inside: avoid; }
     .footer-side { display: flex; min-width: 0; align-items: flex-end; gap: 2mm; white-space: nowrap; }
     .footer-side.is-right { justify-content: flex-end; text-align: right; }
     .footer-label, .footer-time { overflow: hidden; text-overflow: ellipsis; }
     .footer-line { display: inline-block; min-width: 18mm; border-bottom: 0.3mm solid #111827; }
     .footer-line.is-short { width: 25mm; }
     .footer-line.is-medium { width: 45mm; }
     .footer-line.is-long { width: 65mm; }
     .signature-slot { position: relative; display: inline-block; width: 65mm; height: 18mm; vertical-align: bottom; }
     .signature-slot .footer-line { position: absolute; right: 0; bottom: 5mm; width: 100%; }
     .signature-image { position: absolute; z-index: 1; object-fit: contain; object-position: center; }
     .signature-time { position: absolute; right: 0; bottom: -1mm; color: #475569; font-size: 7.5pt; white-space: nowrap; }
    @media screen { body { padding: 12mm; background: #f1f5f9; } .print-sheet { padding: 12mm; background: #fff; box-shadow: 0 4px 18px rgb(15 23 42 / 14%); } }
  </style>
</head>
<body>
  <main class="print-sheet">
    <h1>${title}</h1>
    <table>
      <colgroup>${colgroup}</colgroup>
      <thead><tr>${headers}</tr></thead>
      <tbody>${body}</tbody>
    </table>
    <div class="footer-list">${footer}</div>
  </main>
</body>
</html>`);
  printWindow.document.close();

  const triggerPrint = () => {
    const images = Array.from(printWindow.document.images);
    Promise.all(
      images.map(
        (image) =>
          new Promise<void>((resolve) => {
            if (image.complete) {
              resolve();
              return;
            }
            image.addEventListener('load', () => resolve(), { once: true });
            image.addEventListener('error', () => resolve(), { once: true });
          })
      )
    ).finally(() =>
      window.setTimeout(() => {
        printWindow.focus();
        printWindow.print();
      }, 120)
    );
  };
  if (printWindow.document.readyState === 'complete') {
    triggerPrint();
  } else {
    printWindow.addEventListener('load', triggerPrint, { once: true });
  }
  printWindow.addEventListener('afterprint', () => printWindow.close(), { once: true });
  return true;
};
