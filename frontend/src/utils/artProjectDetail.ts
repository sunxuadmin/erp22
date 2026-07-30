import type {
  CategoryFieldSchemaVO,
  CategoryFileRequirementVO,
  ProjectFileVO,
  ProjectMemberVO,
  ProjectVO,
  ReviewResultVO,
  ReviewScoreVO
} from '@/api/crehn/types';
import { normalizeReviewMessage } from '@/utils/artReviewMessage';
import { GENERIC_TABLE_DATA_KEY, normalizeGenericTableData, normalizeGenericTableTemplates } from '@/utils/artGenericTable';

export interface ProjectDetailRow {
  key: string;
  label: string;
  value: string;
}

export interface ProjectMemberField {
  fieldKey: string;
  fieldLabel: string;
  fieldType?: string;
  required?: boolean;
  optionsJson?: string;
  optionsList?: string[];
  sortOrder?: number;
  groups?: string[];
}

export interface ProjectReviewWorkbookOptions {
  reviewResult?: ReviewResultVO | Record<string, any>;
  reviewScores?: Array<ReviewScoreVO | Record<string, any>>;
}

type WorkbookSheet = {
  name: string;
  rows: any[][];
};

type MemberGroup = 'author' | 'participant';

const EMPTY_TEXT = '-';

const formFieldLabels: Record<string, string> = {
  raw: '原始内容',
  groupName: '组别',
  projectType: '项目类型',
  programForm: '节目/成果形式',
  achievementType: '成果类型',
  achievement_type: '成果类型',
  projectNature: '项目性质',
  project_nature: '项目性质',
  performanceOrder: '展演顺序',
  performance_order: '展演顺序',
  originalCommitment: '原创承诺',
  original_commitment: '原创承诺',
  originalityCommitment: '原创承诺',
  originality_commitment: '原创承诺',
  authorizationCommitment: '授权承诺',
  authorization_commitment: '授权承诺',
  authorizedCommitment: '授权承诺',
  authorized_commitment: '授权承诺',
  noIdentityInVideo: '视频无身份信息确认',
  no_identity_in_video: '视频无身份信息确认',
  noldentityInVideo: '视频无身份信息确认',
  noPreviousAwardCommitment: '无往届获奖承诺',
  no_previous_award_commitment: '无往届获奖承诺',
  submitterType: '提交主体',
  submitter_type: '提交主体',
  workshopName: '工作坊名称',
  workshopPlan: '工作坊方案',
  workshopSummary: '工作坊总结',
  topicCategory: '选题类别',
  paperAbstract: '简介/摘要',
  abstract: '摘要',
  summary: '简介/摘要',
  keywords: '关键词',
  bodyText: '正文文本',
  body_text: '正文/案例正文',
  mainText: '正文文本',
  main_text: '正文文本',
  references: '参考文献',
  referenceList: '参考文献',
  reference_list: '参考文献',
  unpublishedCommitment: '未公开发表承诺',
  unpublished_commitment: '未公开发表承诺',
  unitName: '提交单位',
  unit_name: '提交单位',
  creationDescription: '创作说明',
  creation_description: '创作说明',
  caseText: '案例正文',
  case_text: '案例正文'
};

const memberFieldLabels: Record<string, string> = {
  memberType: '类型',
  name: '姓名',
  identityNo: '身份证号/护照号',
  nation: '民族',
  age: '年龄',
  gender: '性别',
  studentNo: '学号/工作证号',
  department: '所在院系',
  grade: '年级',
  majorCode: '专业代码',
  major: '专业名称',
  phone: '联系方式',
  remark: '备注',
  roleName: '角色/分工',
  photo: '证件照',
  studentReport: '学籍报告',
  status: '状态'
};

const commonValueLabels: Record<string, string> = {
  true: '是',
  false: '否',
  individual: '个人',
  personal: '个人',
  collective: '集体',
  group: '集体',
  unit: '单位',
  organization: '单位',
  school: '学校',
  student: '学生',
  author: '作者',
  teacher: '指导教师',
  completer: '完成人',
  contributor: '完成人',
  male: '男',
  female: '女',
  active: '有效',
  inactive: '停用',
  draft: '草稿',
  submitted: '待审核',
  returned: '已退回',
  audit_passed: '已通过',
  passed: '通过',
  warning: '需人工关注',
  failed: '不通过'
};

const fixedMemberFieldKeys = [
  'memberType',
  'name',
  'identityNo',
  'nation',
  'age',
  'gender',
  'studentNo',
  'department',
  'grade',
  'majorCode',
  'major',
  'phone',
  'remark',
  'roleName',
  'status'
];

export const buildProjectFormRows = (project?: ProjectVO): ProjectDetailRow[] => {
  const data = parseJsonObject(project?.formDataJson, project?.formDataJson ? { raw: project.formDataJson } : {});
  const schemas = [...(project?.fieldSchemas || [])]
    .filter((item) => item.fieldKey)
    .sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0));
  const usedKeys = new Set<string>();
  const rows: ProjectDetailRow[] = [];

  schemas.forEach((field, index) => {
    const key = String(field.fieldKey);
    usedKeys.add(key);
    rows.push({
      key,
      label: field.fieldLabel || readableFieldLabel(key, index, formFieldLabels),
      value: formatProjectFieldValue((data as Record<string, any>)[key], field)
    });
  });

  if (schemas.length) {
    return rows;
  }

  Object.entries(data).forEach(([key, value], index) => {
    if (usedKeys.has(key) || key === GENERIC_TABLE_DATA_KEY) return;
    rows.push({
      key,
      label: readableFieldLabel(key, rows.length + index, formFieldLabels),
      value: formatProjectFieldValue(value)
    });
  });

  return rows;
};

export const buildProjectMemberFields = (project?: ProjectVO): ProjectMemberField[] => {
  const groups = parseRuleJson(project?.categoryRuleJson).memberFieldGroups || {};
  const configuredFields = {
    author: normalizeMemberFieldSchemas(groups.author, 'author'),
    participant: normalizeMemberFieldSchemas(groups.participant, 'participant')
  };
  const hasConfiguredFields = configuredFields.author.length > 0 || configuredFields.participant.length > 0;
  const sourceFields = hasConfiguredFields ? [...configuredFields.author, ...configuredFields.participant] : guideMemberFields();
  const map = new Map<string, ProjectMemberField>();

  sourceFields.forEach((field) => {
    const normalized = normalizeMemberUploadField(field);
    if (!normalized.fieldKey) return;
    const existing = map.get(normalized.fieldKey);
    if (existing) {
      existing.groups = [...new Set([...(existing.groups || []), ...(normalized.groups || [])])];
      existing.required = !!existing.required || !!normalized.required;
      existing.sortOrder = Math.min(existing.sortOrder || normalized.sortOrder || 0, normalized.sortOrder || existing.sortOrder || 0);
      return;
    }
    map.set(normalized.fieldKey, { ...normalized });
  });

  const discoveredGroups = new Map<string, Set<MemberGroup>>();
  const discoverField = (fieldKey: string, member: ProjectMemberVO) => {
    if (!fieldKey || fieldKey === 'memberType') return;
    const groups = discoveredGroups.get(fieldKey) || new Set<MemberGroup>();
    groups.add(memberGroupKey(member));
    discoveredGroups.set(fieldKey, groups);
  };
  (project?.members || []).forEach((member) => {
    fixedMemberFieldKeys.forEach((fieldKey) => {
      if (fieldKey !== 'memberType' && hasValue((member as any)[fieldKey])) discoverField(fieldKey, member);
    });
    if (member.photoPath || member.photoOssId) discoverField('photo', member);
    if (member.studentReportPath || member.studentReportOssId) discoverField('studentReport', member);
    Object.entries(parseJsonObject(member.extraJson)).forEach(([fieldKey, value]) => {
      if (hasValue(value)) discoverField(fieldKey, member);
    });
  });
  discoveredGroups.forEach((groups, fieldKey) => {
    if (map.has(fieldKey)) return;
    map.set(fieldKey, {
      fieldKey,
      fieldLabel: memberFieldLabels[fieldKey] || fieldKey,
      fieldType: fieldKey === 'photo' ? 'image_upload' : fieldKey === 'studentReport' ? 'pdf_upload' : 'input',
      sortOrder: 1000 + map.size,
      groups: [...groups]
    });
  });

  return [...map.values()].sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0));
};

export const memberTypeLabel = (type?: string) => {
  const key = String(type || '').trim();
  return commonValueLabels[key] || commonValueLabels[key.toLowerCase()] || key || EMPTY_TEXT;
};

export const memberFieldLabel = (field: ProjectMemberField) =>
  `${field.required ? '* ' : ''}${field.fieldLabel || readableFieldLabel(field.fieldKey, 0, memberFieldLabels)}`;

export const formatProjectMemberValue = (member: ProjectMemberVO, field: ProjectMemberField) => {
  if (!memberFieldApplies(member, field)) return EMPTY_TEXT;
  if (field.fieldKey === 'photo') {
    return member.photoPath || member.photoOssId ? '已上传' : '未上传';
  }
  if (field.fieldKey === 'studentReport') {
    return member.studentReportPath || member.studentReportOssId ? '已上传' : '未上传';
  }
  const value = fixedMemberFieldKeys.includes(field.fieldKey) ? (member as any)[field.fieldKey] : parseJsonObject(member.extraJson)[field.fieldKey];
  return formatProjectFieldValue(value, field);
};

export const exportProjectDetailWorkbook = (
  project: ProjectVO,
  formRows = buildProjectFormRows(project),
  memberFields = buildProjectMemberFields(project),
  options: ProjectReviewWorkbookOptions = {}
) => {
  const basicSheetRows = buildBasicInfoSheetRows(project, formRows);
  const formSheetRows = [['字段', '内容'], ...formRows.map((item) => [item.label, item.value])];
  const memberSheetRows = [
    ['类型', ...memberFields.map((field) => memberFieldLabel(field))],
    ...(project.members || []).map((member) => [
      memberTypeLabel(member.memberType),
      ...memberFields.map((field) => formatProjectMemberValue(member, field))
    ])
  ];
  const sheets: WorkbookSheet[] = [
    { name: '基本信息', rows: basicSheetRows },
    { name: '表单内容', rows: formSheetRows },
    { name: '成员信息', rows: memberSheetRows },
    { name: '附件清单', rows: buildAttachmentSheetRows(project) }
  ];
  sheets.push(...buildGenericTableSheets(project));
  const reviewSheetRows = buildReviewResultSheetRows(project, options);
  if (reviewSheetRows) {
    sheets.push({ name: '评审结果', rows: reviewSheetRows });
  }
  const xml = workbookXml(sheets);
  downloadTextFile(xml, `${project.projectName || project.projectNo || '作品上报详情'}.xls`, 'application/vnd.ms-excel;charset=utf-8;');
};

const buildGenericTableSheets = (project: ProjectVO): WorkbookSheet[] => {
  const rule = parseRuleJson(project.categoryRuleJson);
  const formData = parseJsonObject(project.formDataJson);
  const tableData = normalizeGenericTableData(formData[GENERIC_TABLE_DATA_KEY]);
  return normalizeGenericTableTemplates(rule.genericTableTemplates)
    .filter((template) => template.enabled !== false)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    .map((template, index) => {
      const fields = [...template.fields].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
      const rows = tableData[template.templateKey] || [];
      const sheetRows: any[][] = [
        fields.map((field) => field.fieldLabel),
        ...rows.map((row) =>
          fields.map((field) => {
            const value = row[field.fieldKey];
            if (Array.isArray(value)) return value.join('、');
            return formatCellValue(value);
          })
        )
      ];
      if (template.footerText) sheetRows.push([`说明：${template.footerText}`]);
      return { name: `表格${index + 1}-${template.templateName}`, rows: sheetRows };
    });
};

const buildBasicInfoSheetRows = (project: ProjectVO, formRows: ProjectDetailRow[]) => {
  const rows = [['字段', '内容']];
  const add = (label: string, value: any) => rows.push([label, formatCellValue(value)]);
  add('项目编号', project.projectNo);
  add('项目名称', project.projectName);
  add('单位', project.schoolName || project.schoolId);
  add('活动名称', project.activityName || project.activityId);
  add('节目类别/类别', project.categoryName || project.categoryId);
  add('组别', firstValue(project.groupName, formRowValue(formRows, ['groupName', 'group_name'], ['组别'])));
  add('项目类型', formRowValue(formRows, ['projectType', 'project_type'], ['项目类型']));
  add(
    '节目/成果形式',
    formRowValue(formRows, ['programForm', 'program_form', 'achievementType', 'achievement_type'], ['节目/成果形式', '节目形式', '成果类型'])
  );
  add('展演顺序', formRowValue(formRows, ['performanceOrder', 'performance_order'], ['展演顺序']));
  add('状态', projectStatusLabel(project.status));
  add('提交时间', project.submittedAt);
  add('审核意见', project.currentAuditOpinion);
  add('成员数量', project.members?.length);
  add('附件数量', project.files?.length);
  add('创建时间', project.createTime);
  add('更新时间', project.updateTime);
  return rows;
};

const buildAttachmentSheetRows = (project: ProjectVO) => {
  const rows: any[][] = [
    [
      '序号',
      '材料类型',
      '附件要求',
      '文件名',
      '扩展名',
      'MIME 类型',
      '大小',
      '技术校验',
      '校验说明',
      '媒体类型',
      '分辨率',
      '时长',
      '帧率',
      '码率',
      'DPI',
      '上传时间'
    ]
  ];
  const requirements = project.fileRequirements || [];
  (project.files || []).forEach((file, index) => {
    const requirement = requirementForFile(file, requirements);
    rows.push([
      index + 1,
      materialTypeLabel(file, requirement),
      requirement?.fileTypeName || requirement?.fileTypeCode || file.fileTypeCode || EMPTY_TEXT,
      file.originalName || EMPTY_TEXT,
      file.fileExt || fileExtension(file.originalName) || EMPTY_TEXT,
      file.mimeType || EMPTY_TEXT,
      formatFileSize(file.fileSize),
      checkStatusLabel(file.checkStatus),
      normalizeReviewMessage(file.checkMessage) || file.checkMessage || EMPTY_TEXT,
      mediaTypeLabel(file.mediaType),
      resolutionText(file),
      formatDuration(file.durationSeconds),
      formatFps(file.fps),
      formatBitrate(file.bitrate),
      formatCellValue(file.dpi),
      file.uploadedAt || file.updateTime || EMPTY_TEXT
    ]);
  });
  return rows;
};

const buildReviewResultSheetRows = (project: ProjectVO, options: ProjectReviewWorkbookOptions) => {
  const projectAny = project as Record<string, any>;
  const reviewResult = reviewResultFrom(projectAny, options);
  const scoreSummary = parseJsonObject(reviewResult?.scoreSummaryJson || projectAny.scoreSummaryJson);
  const reviewScores = reviewScoresFrom(projectAny, options, scoreSummary);
  const taskScore = scoreFromProjectLike(projectAny);
  const scores = reviewScores.length ? reviewScores : taskScore ? [taskScore] : [];
  if (!reviewResult && !scores.length) return null;

  const rows: any[][] = [['分组', '字段', '内容']];
  if (reviewResult) {
    addReviewSummaryRows(rows, reviewResult, scoreSummary);
  } else if (taskScore) {
    rows.push(['汇总', '评分模式', scoreModeLabel(projectAny.scoreMode)]);
    rows.push(['汇总', '当前评分状态', scoreStatusLabel(projectAny.scoreStatus)]);
    rows.push(['汇总', '当前分数', formatScore(projectAny.scoreValue)]);
    rows.push(['汇总', '当前等级', formatCellValue(projectAny.gradeValue)]);
    rows.push(['汇总', '评分提交时间', formatCellValue(projectAny.scoreSubmittedAt)]);
  }

  if (scores.length) {
    rows.push([]);
    rows.push(['评分明细', '评委', '分数', '等级', '状态', '提交时间', '意见']);
    scores.forEach((score) => {
      rows.push([
        '评分明细',
        score.reviewerNickName || score.reviewerUserName || score.reviewerUserId || EMPTY_TEXT,
        formatScore(score.scoreValue),
        formatCellValue(score.gradeValue),
        scoreStatusLabel(score.status),
        formatCellValue(score.submittedAt),
        formatCellValue(score.commentText)
      ]);
    });
  }
  return rows;
};

const addReviewSummaryRows = (rows: any[][], reviewResult: Record<string, any>, scoreSummary: Record<string, any>) => {
  rows.push(['汇总', '评分人数', formatCellValue(reviewResult.scoreCount)]);
  rows.push(['汇总', '总分', formatScore(reviewResult.totalScore)]);
  rows.push(['汇总', '平均分', formatScore(reviewResult.averageScore)]);
  rows.push(['汇总', '最终等级', formatCellValue(reviewResult.finalGrade)]);
  rows.push(['汇总', '奖项', formatCellValue(reviewResult.awardLevel)]);
  rows.push(['汇总', '奖项备注', formatCellValue(reviewResult.awardRemark)]);
  rows.push(['汇总', '排名', formatCellValue(reviewResult.rankNo)]);
  rows.push(['汇总', '结果状态', resultStatusLabel(reviewResult.resultStatus)]);
  rows.push(['汇总', '发布时间', formatCellValue(reviewResult.publishedAt)]);
  rows.push(['汇总', '备注', formatCellValue(reviewResult.remark)]);
  if (scoreSummary.invalidReason) {
    rows.push(['汇总', '评分汇总提示', formatCellValue(scoreSummary.invalidReason)]);
  }
  if (scoreSummary.aggregateMethod) {
    rows.push(['汇总', '汇总方式', formatCellValue(scoreSummary.aggregateMethod)]);
  }
};

const formRowValue = (rows: ProjectDetailRow[], keys: string[], labels: string[]) => {
  const normalizedLabels = labels.map((item) => item.trim());
  const row = rows.find((item) => keys.includes(item.key) || normalizedLabels.includes(item.label));
  return row?.value || EMPTY_TEXT;
};

const firstValue = (...values: any[]) => {
  const value = values.find((item) => hasValue(item) && String(item) !== EMPTY_TEXT);
  return value ?? EMPTY_TEXT;
};

const formatCellValue = (value: any) => (hasValue(value) ? formatProjectFieldValue(value) : EMPTY_TEXT);

const projectStatusLabel = (status?: string) => {
  const key = String(status || '').trim();
  return commonValueLabels[key] || commonValueLabels[key.toLowerCase()] || key || EMPTY_TEXT;
};

const resultStatusLabel = (status?: string) => {
  const map: Record<string, string> = {
    draft: '未生成',
    generated: '已生成',
    published: '已发布',
    unpublished: '未发布'
  };
  const key = String(status || '').trim();
  return map[key] || commonValueLabels[key] || key || EMPTY_TEXT;
};

const scoreStatusLabel = (status?: string) => {
  const map: Record<string, string> = {
    none: '未评分',
    draft: '草稿',
    submitted: '已提交',
    returned: '已退回'
  };
  const key = String(status || '').trim();
  return map[key] || commonValueLabels[key] || key || EMPTY_TEXT;
};

const scoreModeLabel = (value?: string) => (value === 'grade' ? '等级制' : value ? '百分制' : EMPTY_TEXT);

const formatScore = (value: any) => {
  if (!hasValue(value)) return EMPTY_TEXT;
  const score = Number(value);
  if (!Number.isFinite(score)) return formatCellValue(value);
  return Number.isInteger(score) ? String(score) : score.toFixed(2);
};

const requirementForFile = (file: ProjectFileVO, requirements: CategoryFileRequirementVO[]) =>
  requirements.find((item) => item.id !== undefined && String(item.id) === String(file.requirementId)) ||
  requirements.find((item) => item.fileTypeCode && item.fileTypeCode === file.fileTypeCode);

const materialTypeLabel = (file: ProjectFileVO, requirement?: CategoryFileRequirementVO) =>
  requirement?.fileTypeName || requirement?.fileTypeCode || file.fileTypeCode || EMPTY_TEXT;

const fileExtension = (name?: string) => {
  const parts = String(name || '').split('.');
  return parts.length > 1 ? parts.pop() : '';
};

const formatFileSize = (size?: number) => {
  const value = Number(size || 0);
  if (!Number.isFinite(value) || value <= 0) return EMPTY_TEXT;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  if (value < 1024 * 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`;
  return `${(value / 1024 / 1024 / 1024).toFixed(2)} GB`;
};

const checkStatusLabel = (status?: string) => {
  const map: Record<string, string> = {
    passed: '通过',
    warning: '需人工关注',
    failed: '不通过'
  };
  const key = String(status || '').trim();
  return map[key] || key || '未校验';
};

const mediaTypeLabel = (mediaType?: string) => {
  const map: Record<string, string> = {
    image: '图片',
    video: '视频',
    audio: '音频',
    document: '文档'
  };
  const key = String(mediaType || '').trim();
  return map[key] || key || EMPTY_TEXT;
};

const resolutionText = (file: ProjectFileVO) => (file.width && file.height ? `${file.width}x${file.height}` : EMPTY_TEXT);

const formatDuration = (value?: number) => {
  const seconds = Number(value || 0);
  if (!Number.isFinite(seconds) || seconds <= 0) return EMPTY_TEXT;
  if (seconds < 60) return `${seconds.toFixed(1)} 秒`;
  const totalSeconds = Math.round(seconds);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const remainSeconds = totalSeconds % 60;
  return hours ? `${hours}小时${minutes}分${remainSeconds}秒` : `${minutes}分${remainSeconds}秒`;
};

const formatFps = (value?: number) => {
  const fps = Number(value || 0);
  return Number.isFinite(fps) && fps > 0 ? `${fps.toFixed(2)} fps` : EMPTY_TEXT;
};

const formatBitrate = (value?: number) => {
  const bitrate = Number(value || 0);
  if (!Number.isFinite(bitrate) || bitrate <= 0) return EMPTY_TEXT;
  if (bitrate >= 1024 * 1024) return `${(bitrate / 1024 / 1024).toFixed(2)} Mbps`;
  if (bitrate >= 1024) return `${(bitrate / 1024).toFixed(2)} Kbps`;
  return `${bitrate} bps`;
};

const reviewResultFrom = (project: Record<string, any>, options: ProjectReviewWorkbookOptions) => {
  const result = (options.reviewResult || project.reviewResult || project.reviewResultVo || project.result) as Record<string, any> | undefined;
  if (result && hasReviewResultValue(result)) return result;
  return hasReviewResultValue(project) ? project : undefined;
};

const hasReviewResultValue = (value?: Record<string, any>) =>
  !!value &&
  [
    'scoreCount',
    'totalScore',
    'averageScore',
    'finalGrade',
    'awardLevel',
    'awardRemark',
    'rankNo',
    'scoreSummaryJson',
    'resultStatus',
    'publishedAt'
  ].some((key) => hasValue(value[key]));

const reviewScoresFrom = (project: Record<string, any>, options: ProjectReviewWorkbookOptions, scoreSummary: Record<string, any>) => {
  const source = options.reviewScores || project.reviewScores || project.peerScores || project.scores || scoreSummary.scores;
  return Array.isArray(source) ? source.filter((item) => item && typeof item === 'object') : [];
};

const scoreFromProjectLike = (project: Record<string, any>) => {
  if (!['scoreValue', 'gradeValue', 'commentText', 'scoreStatus', 'scoreSubmittedAt'].some((key) => hasValue(project[key]))) return undefined;
  return {
    reviewerUserId: project.reviewerUserId,
    reviewerUserName: project.reviewerUserName,
    reviewerNickName: project.reviewerNickName,
    scoreValue: project.scoreValue,
    gradeValue: project.gradeValue,
    commentText: project.commentText,
    status: project.scoreStatus,
    submittedAt: project.scoreSubmittedAt
  };
};

const normalizeMemberFieldSchemas = (value: any, group: MemberGroup): ProjectMemberField[] => {
  if (!Array.isArray(value)) return [];
  return value
    .map((item: any, index: number) => {
      const fieldKey = String(item?.fieldKey || '');
      return {
        fieldKey,
        fieldLabel: item?.fieldLabel || readableFieldLabel(fieldKey, index, memberFieldLabels),
        fieldType: item?.fieldType || 'input',
        required: !!item?.required,
        optionsJson: item?.optionsJson,
        optionsList: Array.isArray(item?.optionsList) ? item.optionsList : undefined,
        sortOrder: Number(item?.sortOrder || index + 1),
        groups: [group]
      };
    })
    .filter((item) => item.fieldKey);
};

const guideMemberFields = (): ProjectMemberField[] => [
  { fieldKey: 'name', fieldLabel: '姓名', fieldType: 'input', groups: ['author', 'participant'], sortOrder: 1 },
  { fieldKey: 'identityNo', fieldLabel: '身份证号/护照号', fieldType: 'input', groups: ['participant'], sortOrder: 2 },
  { fieldKey: 'nation', fieldLabel: '民族', fieldType: 'input', groups: ['participant'], sortOrder: 3 },
  { fieldKey: 'gender', fieldLabel: '性别', fieldType: 'input', groups: ['participant'], sortOrder: 4 },
  { fieldKey: 'grade', fieldLabel: '年级', fieldType: 'input', groups: ['participant'], sortOrder: 5 },
  { fieldKey: 'studentNo', fieldLabel: '学号/工作证号', fieldType: 'input', groups: ['participant'], sortOrder: 6 },
  { fieldKey: 'department', fieldLabel: '所在院系', fieldType: 'input', groups: ['participant'], sortOrder: 7 },
  { fieldKey: 'majorCode', fieldLabel: '专业代码', fieldType: 'input', groups: ['participant'], sortOrder: 8 },
  { fieldKey: 'major', fieldLabel: '专业名称', fieldType: 'input', groups: ['participant'], sortOrder: 9 },
  { fieldKey: 'phone', fieldLabel: '联系方式', fieldType: 'input', groups: ['participant'], sortOrder: 10 },
  { fieldKey: 'roleName', fieldLabel: '备注/身份', fieldType: 'input', groups: ['author', 'participant'], sortOrder: 11 }
];

const normalizeMemberUploadField = (field: ProjectMemberField): ProjectMemberField => {
  if (field.fieldKey === 'photo' || field.fieldType === 'image_upload') {
    return { ...field, fieldKey: 'photo', fieldLabel: field.fieldLabel || '证件照', fieldType: 'image_upload' };
  }
  if (field.fieldKey === 'studentReport' || field.fieldType === 'pdf_upload') {
    return { ...field, fieldKey: 'studentReport', fieldLabel: field.fieldLabel || '学籍报告', fieldType: 'pdf_upload' };
  }
  return field;
};

const memberGroupKey = (member?: ProjectMemberVO): MemberGroup => (member?.memberType === 'author' ? 'author' : 'participant');

const memberFieldApplies = (member: ProjectMemberVO, field: ProjectMemberField) => {
  const groups = field.groups || ['author', 'participant'];
  return groups.includes(memberGroupKey(member));
};

const formatProjectFieldValue = (value: any, field?: CategoryFieldSchemaVO | ProjectMemberField): string => {
  if (!hasValue(value)) return EMPTY_TEXT;
  if (Array.isArray(value)) {
    return value.length ? value.map((item) => formatProjectFieldValue(item, field)).join('、') : EMPTY_TEXT;
  }
  if (typeof value === 'boolean') {
    return formatBooleanValue(value, field);
  }
  if (typeof value === 'number') {
    return String(value);
  }
  if (typeof value === 'string') {
    const text = value.trim();
    if (!text) return EMPTY_TEXT;
    if (isBooleanText(text)) return formatBooleanValue(text.toLowerCase() === 'true', field);
    return commonValueLabels[text] || commonValueLabels[text.toLowerCase()] || text;
  }
  if (typeof value === 'object') {
    return formatObjectValue(value, nestedFields(field));
  }
  return String(value);
};

const formatBooleanValue = (value: boolean, field?: CategoryFieldSchemaVO | ProjectMemberField) => {
  if (field?.fieldType === 'checkbox') {
    const options = parseOptions(field.optionsJson);
    if (value) return options.length ? options.join('、') : '已勾选';
    return '未勾选';
  }
  return value ? '是' : '否';
};

const formatObjectValue = (value: Record<string, any>, fields: ProjectMemberField[] = []) => {
  const fieldMap = new Map(fields.map((field) => [field.fieldKey, field.fieldLabel || field.fieldKey]));
  const entries = Object.entries(value).filter(([, entryValue]) => hasValue(entryValue));
  if (!entries.length) return EMPTY_TEXT;
  return entries
    .map(
      ([key, entryValue], index) =>
        `${fieldMap.get(key) || readableFieldLabel(key, index, { ...formFieldLabels, ...memberFieldLabels })}：${formatProjectFieldValue(entryValue)}`
    )
    .join('，');
};

const nestedFields = (field?: CategoryFieldSchemaVO | ProjectMemberField): ProjectMemberField[] => {
  if (!field || !('validationJson' in field) || field.fieldType !== 'teacher_group') return [];
  const validation = parseJsonObject(field.validationJson);
  const rows = Array.isArray(validation.teacherFields)
    ? validation.teacherFields
    : [
        { fieldKey: 'name', fieldLabel: '指导老师', fieldType: 'input', sortOrder: 1 },
        { fieldKey: 'phone', fieldLabel: '电话', fieldType: 'input', sortOrder: 2 },
        { fieldKey: 'remark', fieldLabel: '备注', fieldType: 'input', sortOrder: 3 }
      ];
  return normalizeMemberFieldSchemas(rows, 'participant');
};

const hasValue = (value: any): boolean => {
  if (value === undefined || value === null) return false;
  if (Array.isArray(value)) return value.length > 0;
  if (typeof value === 'object') return Object.values(value).some(hasValue);
  return String(value).trim() !== '';
};

const isBooleanText = (value: string) => ['true', 'false'].includes(value.toLowerCase());

const parseOptions = (value?: string | string[]) => {
  if (Array.isArray(value)) return value.map(String).filter(Boolean);
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed)) return parsed.map(String).filter(Boolean);
  } catch {
    // fall through to plain-text parsing
  }
  return String(value)
    .split(/[\n,，]/)
    .map((item) => item.trim())
    .filter(Boolean);
};

const parseJsonObject = (text?: string, fallback: Record<string, any> = {}) => {
  if (!text) return {};
  try {
    const parsed = JSON.parse(text);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : fallback;
  } catch {
    return fallback;
  }
};

const parseRuleJson = (text?: string) => parseJsonObject(text);

const readableFieldLabel = (key: string, index: number, labelMap: Record<string, string>) => {
  if (labelMap[key]) return labelMap[key];
  if (/[\u4e00-\u9fa5]/.test(key)) return key;
  return `未配置字段${index + 1}`;
};

const workbookXml = (sheets: WorkbookSheet[]) => `<?xml version="1.0" encoding="UTF-8"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <Styles>
  <Style ss:ID="Default" ss:Name="Normal"><Alignment ss:Vertical="Center" ss:WrapText="1"/></Style>
  <Style ss:ID="Header"><Font ss:Bold="1"/><Interior ss:Color="#EAF2F8" ss:Pattern="Solid"/></Style>
 </Styles>
 ${sheets.map(worksheetXml).join('\n')}
</Workbook>`;

const worksheetXml = (sheet: WorkbookSheet) => `<Worksheet ss:Name="${escapeXml(sheetName(sheet.name))}">
 <Table>
  ${worksheetColumnsXml(sheet)}
  ${sheet.rows.map((row, rowIndex) => rowXml(row, rowIndex === 0)).join('\n  ')}
 </Table>
</Worksheet>`;

const worksheetColumnsXml = (sheet: WorkbookSheet) => {
  const columnCount = Math.max(2, ...sheet.rows.map((row) => row.length));
  return Array.from({ length: columnCount }, (_, index) => `<Column ss:Width="${columnWidth(sheet, index)}"/>`).join('\n  ');
};

const columnWidth = (sheet: WorkbookSheet, index: number) => {
  const maxLength = Math.max(...sheet.rows.map((row) => String(row[index] ?? '').length), index === 0 ? 6 : 10);
  return Math.min(360, Math.max(index === 0 ? 70 : 100, maxLength * 8 + 24));
};

const rowXml = (row: any[], header = false) => `<Row>${row.map((cell) => cellXml(cell, header)).join('')}</Row>`;

const cellXml = (value: any, header = false) =>
  `<Cell${header ? ' ss:StyleID="Header"' : ''}><Data ss:Type="String">${escapeXml(String(value ?? EMPTY_TEXT))}</Data></Cell>`;

const escapeXml = (value: string) =>
  value
    .replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f]/g, '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');

const sheetName = (value: string) => {
  const cleaned = value.replace(/[\[\]:*?/\\]/g, '').trim();
  return (cleaned || 'Sheet').slice(0, 31);
};

const fileName = (value: string) => {
  const cleaned = value.replace(/[<>:"/\\|?*]/g, '').trim();
  return cleaned || '作品上报详情.xls';
};

const downloadTextFile = (content: string, name: string, type: string) => {
  const blob = new Blob(['\ufeff', content], { type });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName(name);
  link.click();
  URL.revokeObjectURL(url);
};
