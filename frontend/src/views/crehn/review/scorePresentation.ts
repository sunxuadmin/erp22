export type ReviewScoreDisplaySource = {
  scoreMode?: string;
  scoreValue?: number | string | null;
  gradeValue?: string | null;
  commentText?: string | null;
};

export const scoreModeLabel = (value?: string) => (value === 'grade' ? '等级制' : value === 'comment_only' ? '仅评语' : '百分制');

export const scoreResultText = (row: ReviewScoreDisplaySource, formatNumeric = false) => {
  if (row.scoreMode === 'comment_only') return row.commentText || '-';
  if (row.scoreMode === 'grade') return row.gradeValue || '-';
  if (row.scoreValue === null || row.scoreValue === undefined) return '-';
  return formatNumeric ? Number(row.scoreValue).toFixed(2) : String(row.scoreValue);
};

export const scoreRecordText = (row: ReviewScoreDisplaySource) => row.gradeValue || String(row.scoreValue ?? row.commentText ?? '-');
