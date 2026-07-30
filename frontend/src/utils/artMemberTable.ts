export type MemberFieldGroup = 'author' | 'participant';

export interface MemberTypeOption {
  code: string;
  label: string;
  group: MemberFieldGroup;
  enabled: boolean;
  default?: boolean;
  builtin?: boolean;
}

const builtinMemberTypes: MemberTypeOption[] = [
  { code: 'student', label: '学生', group: 'participant', enabled: true, builtin: true },
  { code: 'author', label: '作者', group: 'author', enabled: true, builtin: true },
  { code: 'teacher', label: '指导教师', group: 'participant', enabled: true, builtin: true },
  { code: 'completer', label: '完成人', group: 'participant', enabled: true, builtin: true }
];

const validCode = (value: unknown) => /^[a-z][a-z0-9_]{0,31}$/.test(String(value || '').trim());

export const normalizeMemberTypeOptions = (value: unknown): MemberTypeOption[] => {
  const rows = Array.isArray(value) ? value : [];
  const source = new Map(rows.map((item: any) => [String(item?.code || '').trim(), item]));
  const result = builtinMemberTypes.map((builtin) => {
    const item = source.get(builtin.code) || {};
    return {
      ...builtin,
      label: String(item.label || builtin.label).trim() || builtin.label,
      enabled: item.enabled !== false,
      default: item.default === true
    };
  });
  const usedCodes = new Set(result.map((item) => item.code));
  rows.forEach((item: any) => {
    const code = String(item?.code || '').trim();
    if (!validCode(code) || usedCodes.has(code)) return;
    const label = String(item?.label || '').trim();
    if (!label) return;
    usedCodes.add(code);
    result.push({
      code,
      label,
      group: item.group === 'author' ? 'author' : 'participant',
      enabled: item.enabled !== false,
      default: item.default === true,
      builtin: false
    });
  });
  const defaultOption = result.find((item) => item.default && item.enabled) || result.find((item) => item.enabled) || result[0];
  return result.map((item) => ({ ...item, default: item.code === defaultOption.code }));
};

export const memberDefaultType = (value: unknown) => normalizeMemberTypeOptions(value).find((item) => item.default)?.code || 'student';

export const memberTypeGroup = (value: unknown, code?: string) =>
  normalizeMemberTypeOptions(value).find((item) => item.code === code)?.group || 'participant';

export const isMemberAttachmentType = (field?: { fieldKey?: string; fieldType?: string }) =>
  ['image_upload', 'pdf_upload', 'attachment_upload'].includes(field?.fieldType || '') || ['photo', 'studentReport'].includes(field?.fieldKey || '');

export const memberAttachmentDefaultExts = (field?: { fieldKey?: string; fieldType?: string }) => {
  if (field?.fieldKey === 'photo' || field?.fieldType === 'image_upload') return ['jpg', 'jpeg', 'png', 'webp'];
  if (field?.fieldKey === 'studentReport' || field?.fieldType === 'pdf_upload') return ['pdf'];
  return ['jpg', 'jpeg', 'png', 'webp', 'pdf', 'doc', 'docx', 'xls', 'xlsx'];
};
