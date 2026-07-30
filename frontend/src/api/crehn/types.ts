export interface ActivityVO {
  id?: string | number;
  activityName?: string;
  menuName?: string;
  edition?: string;
  year?: number;
  organizer?: string;
  undertaker?: string;
  signupStartAt?: string;
  signupEndAt?: string;
  scopeType?: string;
  description?: string;
  status?: string;
  createTime?: string;
  delFlag?: string;
}

export interface ActivityDeleteCategoryVO {
  id?: string | number;
  parentId?: string | number;
  categoryCode?: string;
  categoryName?: string;
  categoryGroup?: string;
  delFlag?: string;
  fieldSchemaCount?: number;
  fileRequirementCount?: number;
}

export interface ActivityDeleteCheckVO {
  activityId?: string | number;
  activityName?: string;
  delFlag?: string;
  canDelete?: boolean;
  categoryCount?: number;
  fieldSchemaCount?: number;
  fileRequirementCount?: number;
  schoolScopeCount?: number;
  projectCount?: number;
  fileCount?: number;
  referenceCount?: number;
  categoryDetailsTruncated?: boolean;
  projectDetailsTruncated?: boolean;
  fileDetailsTruncated?: boolean;
  blockMessage?: string;
  categories?: ActivityDeleteCategoryVO[];
  projects?: ActivityCategoryPurgeProjectVO[];
  files?: ActivityCategoryPurgeFileVO[];
  references?: ActivityCategoryPurgeReferenceVO[];
}

export interface ActivityCategoryVO {
  id?: string | number;
  activityId?: string | number;
  parentId?: string | number;
  categoryCode?: string;
  categoryName?: string;
  quotaLimit?: number;
  categoryGroup?: string;
  ruleJson?: string;
  tipText?: string;
  checkMode?: string;
  sortOrder?: number;
  enabled?: boolean;
  delFlag?: string;
}

export interface ActivityCategoryPurgeChildVO {
  id?: string | number;
  categoryCode?: string;
  categoryName?: string;
  delFlag?: string;
}

export interface ActivityCategoryPurgeProjectVO {
  id?: string | number;
  projectNo?: string;
  projectName?: string;
  schoolId?: string | number;
  schoolName?: string;
  participantUserId?: string | number;
  status?: string;
  delFlag?: string;
  fileCount?: number;
  createTime?: string;
  updateTime?: string;
}

export interface ActivityCategoryPurgeFileVO {
  id?: string | number;
  projectId?: string | number;
  projectNo?: string;
  projectName?: string;
  projectStatus?: string;
  projectDelFlag?: string;
  ossId?: string | number;
  originalName?: string;
  status?: string;
  delFlag?: string;
  uploadedAt?: string;
}

export interface ActivityCategoryPurgeReferenceVO {
  referenceType?: string;
  referenceLabel?: string;
  referenceCount?: number;
  recordIds?: string;
}

export interface ActivityCategoryPurgeCheckVO {
  categoryId?: string | number;
  categoryCode?: string;
  categoryName?: string;
  canPurge?: boolean;
  childCount?: number;
  projectCount?: number;
  fileCount?: number;
  referenceCount?: number;
  fieldSchemaCount?: number;
  fileRequirementCount?: number;
  childDetailsTruncated?: boolean;
  projectDetailsTruncated?: boolean;
  fileDetailsTruncated?: boolean;
  blockMessage?: string;
  children?: ActivityCategoryPurgeChildVO[];
  projects?: ActivityCategoryPurgeProjectVO[];
  files?: ActivityCategoryPurgeFileVO[];
  references?: ActivityCategoryPurgeReferenceVO[];
}

export interface CategoryFieldSchemaVO {
  id?: string | number;
  categoryId?: string | number;
  fieldKey?: string;
  fieldLabel?: string;
  fieldType?: string;
  required?: boolean;
  optionsJson?: string;
  validationJson?: string;
  sensitive?: boolean;
  sortOrder?: number;
  delFlag?: string;
}

export interface CategoryFileRequirementVO {
  id?: string | number;
  categoryId?: string | number;
  fileTypeCode?: string;
  fileTypeName?: string;
  allowedExt?: string;
  maxSizeMb?: number;
  minCount?: number;
  maxCount?: number;
  required?: boolean;
  ruleJson?: string;
  tipText?: string;
  sortOrder?: number;
  delFlag?: string;
}

export interface ActivityConfigImportResultVO {
  activityId?: string | number;
  activityName?: string;
  categoryCount?: number;
  fieldCount?: number;
  fileRequirementCount?: number;
}

export interface ActivityConfigMergeItemVO {
  itemType?: 'category' | 'field' | 'fileRequirement' | string;
  itemTypeLabel?: string;
  categoryCode?: string;
  categoryName?: string;
  itemCode?: string;
  itemName?: string;
  reason?: string;
}

export interface ActivityConfigMergePreviewVO {
  activityId?: string | number;
  activityName?: string;
  sourceActivityName?: string;
  applied?: boolean;
  hasAdditions?: boolean;
  addedCategoryCount?: number;
  addedFieldCount?: number;
  addedFileRequirementCount?: number;
  skippedCategoryCount?: number;
  skippedFieldCount?: number;
  skippedFileRequirementCount?: number;
  additions?: ActivityConfigMergeItemVO[];
  skipped?: ActivityConfigMergeItemVO[];
}

export interface ProjectFileVO {
  id?: string | number;
  projectId?: string | number;
  requirementId?: string | number;
  ossId?: string | number;
  previewOssId?: string | number;
  fileTypeCode?: string;
  originalName?: string;
  storagePath?: string;
  previewPath?: string;
  previewExt?: string;
  previewStatus?: string;
  previewMessage?: string;
  previewGeneratedAt?: string;
  previewRetryCount?: number;
  previewStartedAt?: string;
  fileExt?: string;
  fileSize?: number;
  mimeType?: string;
  mediaType?: string;
  durationSeconds?: number;
  width?: number;
  height?: number;
  fps?: number;
  bitrate?: number;
  dpi?: number;
  metadataJson?: string;
  checkStatus?: string;
  checkMessage?: string;
  versionNo?: number;
  uploadedBy?: string | number;
  uploadedAt?: string;
  status?: string;
  updateBy?: string | number;
  updateTime?: string;
  deletedByName?: string;
}

export interface ProjectFileDirectUploadInitVO {
  uploadUrl?: string;
  uploadToken?: string;
  objectKey?: string;
  fileName?: string;
  originalName?: string;
  contentType?: string;
  fileSize?: number;
  expiresInSeconds?: number;
}

export interface ProjectMemberVO {
  id?: string | number;
  projectId?: string | number;
  activityId?: string | number;
  schoolId?: string | number;
  memberType?: string;
  name?: string;
  identityNo?: string;
  nation?: string;
  age?: string | number;
  gender?: string;
  studentNo?: string;
  department?: string;
  grade?: string;
  majorCode?: string;
  major?: string;
  phone?: string;
  remark?: string;
  roleName?: string;
  photoOssId?: string | number;
  photoPath?: string;
  studentReportOssId?: string | number;
  studentReportPath?: string;
  extraJson?: string;
  sortOrder?: number;
  status?: string;
}

export interface MemberAttachmentBatchItemVO {
  fileName?: string;
  memberId?: string | number;
  matchedValue?: string;
  status?: 'success' | 'skipped';
  message?: string;
}

export interface MemberAttachmentBatchResultVO {
  fieldKey?: string;
  matchedCount?: number;
  skippedCount?: number;
  items?: MemberAttachmentBatchItemVO[];
}

export interface ProjectAuditRecordVO {
  id?: string | number;
  projectId?: string | number;
  fromStatus?: string;
  toStatus?: string;
  auditResult?: string;
  opinion?: string;
  auditedBy?: string | number;
  auditedByName?: string;
  auditedAt?: string;
}

export interface AuditWorkbenchProjectVO {
  id?: string | number;
  projectName?: string;
  schoolName?: string;
  categoryName?: string;
  status?: string;
  submittedAt?: string;
}

export interface AuditWorkbenchStatsVO {
  totalCount?: number;
  passCount?: number;
  returnCount?: number;
}

export interface AuditWorkbenchGroupVO {
  groupKey?: string;
  groupName?: string;
  sortOrder?: number;
  totalCount?: number;
  passCount?: number;
  returnCount?: number;
}

export interface AuditWorkbenchRecordVO {
  recordId?: string | number;
  projectId?: string | number;
  projectName?: string;
  schoolName?: string;
  categoryName?: string;
  auditResult?: string;
  opinion?: string;
  status?: string;
  auditedAt?: string;
}

export interface AuditWorkbenchVO {
  pendingProjects?: AuditWorkbenchProjectVO[];
  myStats?: AuditWorkbenchStatsVO;
  myGroupItems?: AuditWorkbenchGroupVO[];
  myRecords?: AuditWorkbenchRecordVO[];
}

export interface UserMessageVO {
  id?: string | number;
  receiverUserId?: string | number;
  senderUserId?: string | number;
  schoolId?: string | number;
  activityId?: string | number;
  categoryId?: string | number;
  projectId?: string | number;
  messageType?: string;
  sourceType?: string;
  sourceId?: string | number;
  title?: string;
  content?: string;
  readStatus?: string;
  readTime?: string;
  sentAt?: string;
  createTime?: string;
}

export interface ProjectVO {
  id?: string | number;
  activityId?: string | number;
  activityName?: string;
  schoolId?: string | number;
  schoolName?: string;
  categoryId?: string | number;
  categoryIds?: Array<string | number>;
  categoryName?: string;
  categoryRuleJson?: string;
  categoryTipText?: string;
  categoryCheckMode?: string;
  projectNo?: string;
  projectName?: string;
  groupCode?: string;
  groupName?: string;
  formDataJson?: string;
  validationResultJson?: string;
  status?: string;
  recycledFromStatus?: string;
  recycledBy?: string | number;
  recycledByName?: string;
  recycledAt?: string;
  recycleReason?: string;
  submittedAt?: string;
  submittedBy?: string | number;
  participantSubmittedAt?: string;
  schoolReviewStatus?: string;
  schoolReviewedAt?: string;
  schoolFinalBatchId?: string | number;
  configVersionId?: string | number;
  snapshotVersionId?: string | number;
  rowVersion?: string | number;
  currentAuditOpinion?: string;
  createTime?: string;
  updateTime?: string;
  files?: ProjectFileVO[];
  members?: ProjectMemberVO[];
  auditRecords?: ProjectAuditRecordVO[];
  fieldSchemas?: CategoryFieldSchemaVO[];
  fileRequirements?: CategoryFileRequirementVO[];
}

export interface AuditAssignmentVO {
  id?: string | number;
  activityId?: string | number;
  activityName?: string;
  categoryId?: string | number;
  categoryIds?: Array<string | number>;
  categoryName?: string;
  auditorUserId?: string | number;
  auditorUserIds?: Array<string | number>;
  auditorUserName?: string;
  auditorNickName?: string;
  batchOperation?: 'merge' | 'remove' | 'sync';
  batchTarget?: 'assignment' | 'project';
  schoolScopeMode?: 'all' | 'whitelist' | 'blacklist';
  schoolIdsJson?: string;
  allowQuery?: boolean;
  allowPass?: boolean;
  allowReturn?: boolean;
  allowWithdrawPass?: boolean;
  allowWithdrawReturn?: boolean;
  allowDownload?: boolean;
  status?: string;
  assignedBy?: string | number;
  assignedAt?: string;
  createTime?: string;
}

export interface ProjectSaveForm {
  id?: string | number;
  activityId?: string | number;
  categoryId?: string | number;
  projectName?: string;
  groupCode?: string;
  groupName?: string;
  formDataJson?: string;
  members?: ProjectMemberVO[];
}

export interface GenericTableImportResultVO {
  tableKey?: string;
  rows?: Record<string, any>[];
  importedCount?: number;
}

export interface ProjectCategoryStatsVO {
  categoryId?: string | number;
  totalCount?: number;
  draftCount?: number;
  submittedCount?: number;
  completedCount?: number;
}

export interface ProjectQuotaRatioBriefVO {
  key?: string;
  groupCode?: string;
  groupName?: string;
  count?: number;
  percent?: number;
  limitCount?: number;
  ratioValue?: number;
  operator?: string;
  message?: string;
}

export interface ProjectQuotaOverviewItemVO {
  id?: string;
  categoryId?: string | number;
  categoryCode?: string;
  categoryName?: string;
  groupKey?: string;
  groupName?: string;
  sortOrder?: number;
  quotaLimit?: number;
  remainingCount?: number;
  totalCount?: number;
  draftCount?: number;
  submittedCount?: number;
  completedCount?: number;
  usedCount?: number;
  ratioItems?: ProjectQuotaRatioBriefVO[];
}

export interface ProjectQuotaOverviewVO {
  activityId?: string | number;
  activityName?: string;
  schoolId?: string | number;
  schoolName?: string;
  schoolType?: string;
  totalCount?: number;
  draftCount?: number;
  submittedCount?: number;
  completedCount?: number;
  usedCount?: number;
  quotaLimit?: number;
  remainingCount?: number;
  groupItems?: ProjectQuotaOverviewItemVO[];
  categoryItems?: ProjectQuotaOverviewItemVO[];
}

export interface SchoolDashboardProjectVO {
  projectId?: string | number;
  projectName?: string;
  categoryName?: string;
  status?: string;
  time?: string;
}

export interface SchoolDashboardResultVO {
  projectId?: string | number;
  projectName?: string;
  categoryName?: string;
  awardLevel?: string;
  rankNo?: number;
  averageScore?: number;
  finalGrade?: string;
  publishedAt?: string;
}

export interface SchoolDashboardVO {
  totalCount?: number;
  draftCount?: number;
  submittedCount?: number;
  returnedCount?: number;
  passedCount?: number;
  publishedResultCount?: number;
  recentEdited?: SchoolDashboardProjectVO[];
  recentSubmitted?: SchoolDashboardProjectVO[];
  recentPassed?: SchoolDashboardProjectVO[];
  recentResults?: SchoolDashboardResultVO[];
}

export interface ReviewAssignmentVO {
  id?: string | number;
  activityId?: string | number;
  activityName?: string;
  categoryId?: string | number;
  categoryIds?: Array<string | number>;
  categoryName?: string;
  projectIdsJson?: string;
  excludedProjectIdsJson?: string;
  reviewerUserId?: string | number;
  reviewerUserIds?: Array<string | number>;
  reviewerUserName?: string;
  reviewerNickName?: string;
  batchOperation?: 'merge' | 'remove' | 'sync';
  batchTarget?: 'assignment' | 'project';
  schoolScopeMode?: 'all' | 'whitelist' | 'blacklist';
  schoolIdsJson?: string;
  projectFilterJson?: string;
  scoreMode?: string;
  scoreRuleJson?: string;
  exclusiveMode?: string;
  scoreVisibilityPolicy?: string;
  hideSchoolInfo?: boolean;
  hideMemberInfo?: boolean;
  hiddenFieldKeysJson?: string;
  status?: string;
  assignedBy?: string | number;
  assignedAt?: string;
  createTime?: string;
  projectCount?: number;
  submittedCount?: number;
}

export interface ReviewAssignmentScopeGroupVO {
  schoolTypes?: string[];
  fieldValues?: Record<string, string[]>;
}

export interface ReviewAssignmentProjectFilterVO {
  schoolTypes?: string[];
  fieldValues?: Record<string, string[]>;
  scopeGroups?: ReviewAssignmentScopeGroupVO[];
}

export interface ReviewAssignmentScopeUnitVO {
  unitKey?: string;
  pathLabel?: string;
  fieldValues?: Record<string, string>;
  projectCount?: number;
  requiredReviewerCount?: number;
  minimumAssignedReviewerCount?: number;
  maximumAssignedReviewerCount?: number;
  shortageProjectCount?: number;
  assignmentStatus?: 'unassigned' | 'shortage' | 'assigned';
}

export interface ReviewAssignmentCategoryConfigVO {
  activityId?: string | number;
  categoryId?: string | number;
  categoryName?: string;
  scopeFieldKeys?: string[];
  visibilityConfigured?: boolean;
  hideSchoolInfo?: boolean;
  hideMemberInfo?: boolean;
  hiddenFieldKeysJson?: string;
  scopeUnits?: ReviewAssignmentScopeUnitVO[];
}

export interface AssignmentBatchPreviewVO {
  operation?: 'merge' | 'replace' | 'remove' | 'sync';
  target?: 'assignment' | 'project' | 'scope_plan';
  createCount?: number;
  updateCount?: number;
  disableCount?: number;
  unchangedCount?: number;
  preservedSubmittedScoreCount?: number;
  preservedDraftScoreCount?: number;
  orphanDraftScoreCount?: number;
  lockedScoreCount?: number;
}

export interface ReviewAssignmentScopePlanScopeVO {
  categoryId: string | number;
  scopeMode: 'all' | 'units';
  unitKeys: string[];
}

export interface ReviewAssignmentScopePlanForm {
  activityId: string | number;
  reviewerUserIds: Array<string | number>;
  operation: 'merge' | 'remove';
  scopes: ReviewAssignmentScopePlanScopeVO[];
  scoreMode?: string;
  scoreRuleJson?: string;
  exclusiveMode?: string;
  scoreVisibilityPolicy?: string;
}

export interface AssignmentSchoolOptionVO {
  id?: string | number;
  schoolName?: string;
  schoolCode?: string;
  schoolType?: string;
  status?: string;
}

export interface ReviewScoreVO {
  id?: string | number;
  assignmentId?: string | number;
  projectId?: string | number;
  projectNo?: string;
  projectName?: string;
  activityId?: string | number;
  activityName?: string;
  categoryId?: string | number;
  categoryName?: string;
  reviewerUserId?: string | number;
  reviewerUserName?: string;
  reviewerNickName?: string;
  scoreValue?: number;
  gradeValue?: string;
  commentText?: string;
  status?: string;
  submittedAt?: string;
  createTime?: string;
}

export interface ReviewScoreAdminForm {
  scoreId?: string | number;
  scoreValue?: number;
  gradeValue?: string;
  commentText?: string;
  reason?: string;
}

export interface ReviewTaskVO {
  assignmentId?: string | number;
  scopeKey?: string;
  projectId?: string | number;
  activityId?: string | number;
  activityName?: string;
  categoryId?: string | number;
  categoryName?: string;
  categoryIds?: string;
  schoolId?: string | number;
  programForm?: string;
  groupOrNature?: string;
  projectNo?: string;
  projectName?: string;
  projectStatus?: string;
  submittedAt?: string;
  scoreMode?: string;
  scoreRuleJson?: string;
  exclusiveMode?: string;
  scoreVisibilityPolicy?: string;
  hideSchoolInfo?: boolean;
  hideMemberInfo?: boolean;
  hiddenFieldKeysJson?: string;
  scoreId?: string | number;
  scoreValue?: number;
  gradeValue?: string;
  commentText?: string;
  scoreStatus?: string;
  scoreSubmittedAt?: string;
  lockedByOther?: boolean;
  lockedMessage?: string;
  displayFields?: Record<string, string>;
  project?: ProjectVO;
  peerScores?: ReviewScoreVO[];
}

export interface ReviewTaskNavigatorOptionVO {
  id?: string;
  label?: string;
  count?: number;
}

export interface ReviewTaskScopeOptionVO {
  scopeKey: string;
  assignmentId?: string | number;
  categoryId: string | number;
  categoryName?: string;
  scopeLabel?: string;
  wholeCategory?: boolean;
  totalCount?: number;
  completedCount?: number;
  lockedCount?: number;
  pendingCount?: number;
}

export interface ReviewTaskListColumnVO {
  key: string;
  source: 'builtin' | 'form';
  fieldKey?: string;
  label: string;
  visible: boolean;
  width: number;
  minWidth: number;
}

export interface ReviewTaskNavigatorVO {
  categoryOptions?: ReviewTaskNavigatorOptionVO[];
  unitOptions?: ReviewTaskNavigatorOptionVO[];
  programFormOptions?: ReviewTaskNavigatorOptionVO[];
  groupOrNatureOptions?: ReviewTaskNavigatorOptionVO[];
  scopeOptions?: ReviewTaskScopeOptionVO[];
  totalCount?: number;
  completedCount?: number;
  lockedCount?: number;
  categoryTotalCount?: number;
  categoryCompletedCount?: number;
  categoryLockedCount?: number;
  displayColumns?: ReviewTaskListColumnVO[];
}

export interface ReviewScoreSheetColumnVO {
  key: string;
  label: string;
  visible: boolean;
}

export interface ReviewScoreSheetFooterFieldVO {
  label: string;
  type: 'text' | 'exportTime' | 'signature';
  lineLength: 'none' | 'short' | 'medium' | 'long';
  /**
   * Stable identifier for the reviewer signature drop zone. It deliberately
   * lives in the shared layout, while the actual handwritten image stays in a
   * reviewer-owned signed-sheet record.
   */
  slotKey?: string;
  /** Editing-only copy displayed on the signature action button. */
  signatureButtonText?: string;
  /** Editing-only copy displayed beside the empty signature slot. */
  signatureHintText?: string;
  /** Formal timestamp prefix retained in preview, print, and Excel snapshots. */
  signatureTimeText?: string;
  /** Signature is mandatory by default; false permits an unsigned category submission. */
  signatureRequired?: boolean;
  /** Controls visible signature time only; the server-side submission audit remains intact. */
  signatureTimeVisible?: boolean;
}

export interface ReviewScoreSheetFooterRowVO {
  left?: ReviewScoreSheetFooterFieldVO | null;
  right?: ReviewScoreSheetFooterFieldVO | null;
}

export interface ReviewScoreSheetTemplateVO {
  id?: string | number;
  templateName: string;
  title: string;
  columns: ReviewScoreSheetColumnVO[];
  footerRows: ReviewScoreSheetFooterRowVO[];
  footerTimeText?: string;
  footerSignatureText?: string;
  defaultTemplate: boolean;
  version: number;
  updateBy?: string | number;
  updatedByName?: string;
  updateTime?: string;
}

export interface ReviewScoreSheetExportForm {
  activityId: string | number;
  categoryId: string | number;
  scopeKey?: string;
  templateId?: string | number;
  template?: ReviewScoreSheetTemplateVO;
  exportTime?: string;
}

export interface ReviewScoreSheetExportPreviewVO {
  template: ReviewScoreSheetTemplateVO;
  rows: ReviewTaskVO[];
  total: number;
  reviewerName?: string;
  categoryName?: string;
  exportTime: string;
}

/** A reusable handwritten signature that belongs to the current reviewer. */
export interface ReviewScoreSheetSignatureVO {
  id?: string | number;
  signatureName: string;
  /** Canonical OSS URL returned by the signature-library API. */
  imageUrl?: string;
  /** Compatibility alias retained by the backend for older consumers. */
  url?: string;
  defaultSignature?: boolean;
  version?: number;
  /** True when creation found an identical existing signature and reused it. */
  reused?: boolean;
  updateTime?: string;
  createTime?: string;
}

/** Privileged manager view. It never changes the teacher-owned signature API. */
export interface ReviewScoreSheetSignatureAdminVO {
  id?: string | number;
  reviewerUserId?: string | number;
  reviewerName?: string;
  reviewerUserName?: string;
  signatureName?: string;
  ossId?: string | number;
  imageUrl?: string;
  url?: string;
  defaultSignature?: boolean;
  libraryStatus?: 'active' | 'disabled' | 'archived' | string;
  version?: number;
  createTime?: string;
  updateTime?: string;
  statusChangedAt?: string;
  statusChangedByUserId?: string | number;
  statusChangedByName?: string;
  statusReason?: string;
}

export interface ReviewScoreSheetSignatureAdminQuery {
  reviewerUserId?: string | number;
  reviewerKeyword?: string;
  signatureName?: string;
  libraryStatus?: string;
  pageNum?: number;
  pageSize?: number;
}

/**
 * Position is stored as 0..1 ratios inside the configured signature slot so
 * the signature keeps its place in responsive preview, print, and Excel.
 */
export interface ReviewScoreSheetSignaturePlacementVO {
  slotKey: string;
  signatureId?: string | number;
  signatureName?: string;
  signatureUrl?: string;
  x: number;
  y: number;
  width: number;
  height: number;
  signedAt?: string;
}

export interface ReviewScoreSheetSignedSheetForm {
  activityId: string | number;
  categoryId: string | number;
  /** Opaque assignment scope selected by the current reviewer. */
  scopeKey: string;
  templateId?: string | number;
  template?: ReviewScoreSheetTemplateVO;
  signatureId?: string | number;
  placement?: Pick<ReviewScoreSheetSignaturePlacementVO, 'slotKey' | 'x' | 'y' | 'width' | 'height'>;
}

/** Batch preview derived by the server from the selected review scope. */
export interface ReviewScoreSheetSignedSheetPreviewVO extends ReviewScoreSheetExportPreviewVO {
  existingSheet?: ReviewScoreSheetSignedSheetVO;
  signable?: boolean;
  message?: string;
  assignedTotal?: number;
  completedCount?: number;
  submittedCount?: number;
  pendingCount?: number;
  remainingCount?: number;
  completeAfterSubmit?: boolean;
}

/** Immutable signed-sheet snapshot returned for history, print, and Excel download. */
export interface ReviewScoreSheetSignedSheetVO {
  id?: string | number;
  sheetId?: string | number;
  activityId?: string | number;
  activityName?: string;
  categoryId?: string | number;
  categoryName?: string;
  /** The reviewer who owns this immutable signed-sheet snapshot. */
  reviewerUserId?: string | number;
  reviewerName?: string;
  template?: ReviewScoreSheetTemplateVO;
  rows?: ReviewTaskVO[];
  total?: number;
  signature?: ReviewScoreSheetSignatureVO;
  signatureId?: string | number;
  signatureName?: string;
  signatureUrl?: string;
  slotKey?: string;
  /** Backend snapshot field for the signed-sheet placement. */
  placement?: ReviewScoreSheetSignaturePlacementVO;
  /** Kept only for compatibility with an earlier frontend response shape. */
  signaturePlacement?: ReviewScoreSheetSignaturePlacementVO;
  /** SIGNED when a handwritten signature snapshot exists; UNSIGNED for an approved unsigned submission. */
  submissionMode?: 'SIGNED' | 'UNSIGNED' | string;
  signedAt?: string;
  exportTime?: string;
  /** `active` for an effective sheet; `withdrawn` after a recorded withdrawal. */
  status?: string;
  /** Withdrawal audit fields are retained on the historical snapshot. */
  withdrawnAt?: string;
  withdrawnByUserId?: string | number;
  withdrawnByName?: string;
  withdrawnByRole?: string;
  /** `SELF` means the signing reviewer withdrew it; `ADMIN_FORCE` means an administrator did. */
  withdrawalMode?: string;
  withdrawReason?: string;
  /** Compatibility aliases accepted while old and new service nodes coexist. */
  revokedAt?: string;
  revokedByName?: string;
  revokeReason?: string;
}

export interface ReviewScoreSheetSignedSheetQuery {
  activityId?: string | number;
  categoryId?: string | number;
  /** Used only by the admin summary; the service ignores it for a reviewer. */
  reviewerUserId?: string | number;
  status?: string;
  signedAtStart?: string;
  signedAtEnd?: string;
  /** Admins may request their own records; reviewers are always server-scoped to self. */
  ownerOnly?: boolean;
  pageNum?: number;
  pageSize?: number;
}

/** A reason is mandatory because withdrawal is an auditable state change. */
export interface ReviewScoreSheetSignedSheetWithdrawForm {
  reason: string;
}

/** Reviewer choices limited to people who have signed sheets in the selected scope. */
export interface ReviewScoreSheetSignedSheetReviewerOptionVO {
  reviewerUserId?: string | number;
  userId?: string | number;
  reviewerName?: string;
  userName?: string;
  nickName?: string;
}

export interface ReviewWorkbenchGroupVO {
  groupKey?: string;
  groupName?: string;
  totalCount?: number;
  pendingCount?: number;
  draftCount?: number;
  submittedCount?: number;
}

export interface ReviewWorkbenchVO {
  activityName?: string;
  stats?: Record<string, number>;
  pendingTasks?: ReviewTaskVO[];
  recentScores?: ReviewScoreVO[];
  groupItems?: ReviewWorkbenchGroupVO[];
}

export interface ReviewResultVO {
  id?: string | number;
  activityId?: string | number;
  activityName?: string;
  categoryId?: string | number;
  categoryName?: string;
  projectId?: string | number;
  projectNo?: string;
  projectName?: string;
  schoolId?: string | number;
  schoolName?: string;
  scoreCount?: number;
  totalScore?: number;
  averageScore?: number;
  finalGrade?: string;
  awardLevel?: string;
  awardRemark?: string;
  rankNo?: number;
  scoreSummaryJson?: string;
  resultStatus?: string;
  showScore?: boolean;
  showRank?: boolean;
  showComment?: boolean;
  publishedBy?: string | number;
  publishedAt?: string;
  remark?: string;
  createTime?: string;
}

export interface ReviewResultReadinessVO {
  ready?: boolean;
  projectCount?: number;
  activeAssignmentCount?: number;
  requiredReviewerCount?: number;
  expectedScoreCount?: number;
  submittedScoreCount?: number;
  signedScoreCount?: number;
  activeSignedSheetCount?: number;
  blockers?: string[];
  warnings?: string[];
}

export interface ReviewScoreSummaryColumnVO {
  fieldKey?: string;
  label?: string;
  visible?: boolean;
  sortOrder?: number;
  width?: number;
}

export interface ReviewScoreSummaryWarningVO {
  enabled?: boolean;
  formula?: string;
  formulaType?: string;
  formulaExpression?: string;
  thresholdPercent?: number;
  normalText?: string;
  warningText?: string;
  insufficientText?: string;
  unsupportedText?: string;
  status?: string;
  text?: string;
  spreadPercent?: number;
}

export interface ReviewScoreSummaryScoreVO {
  slot?: number;
  reviewerUserId?: string | number;
  reviewerName?: string;
  scoreValue?: number;
  gradeValue?: string;
  commentText?: string;
  submittedAt?: string;
  signature?: {
    signedSheetId?: string | number;
    signatureId?: string | number;
    signatureUrl?: string;
    signatureName?: string;
    signatureSignedAt?: string;
    signatureStatus?: string;
    submissionMode?: 'SIGNED' | 'UNSIGNED' | string;
    withdrawnAt?: string;
    withdrawnByName?: string;
    withdrawalMode?: string;
    withdrawReason?: string;
    scoreMismatch?: boolean;
    signedScoreValue?: number;
  };
  signatureUrl?: string;
  signatureName?: string;
  signatureSignedAt?: string;
  signatureStatus?: string;
  /** SIGNED when a handwritten signature snapshot exists; UNSIGNED for an approved unsigned submission. */
  submissionMode?: 'SIGNED' | 'UNSIGNED' | string;
}

export interface ReviewScoreSummaryVO {
  rowNo?: number;
  activityId?: string | number;
  activityName?: string;
  categoryId?: string | number;
  categoryName?: string;
  projectId?: string | number;
  projectNo?: string;
  projectName?: string;
  schoolId?: string | number;
  schoolName?: string;
  groupOrNature?: string;
  groupName?: string;
  projectNature?: string;
  programForm?: string;
  scoreItems?: ReviewScoreSummaryScoreVO[];
  currentAverageScore?: number;
  resultAverageScore?: number;
  resultGeneratedAt?: string;
  resultStatus?: string;
  warning?: ReviewScoreSummaryWarningVO;
}

export interface ReviewScoreSummaryDetailVO extends ReviewScoreSummaryVO {
  project?: any;
  attachments?: any[];
}

export interface ReviewScoreSummaryConfigVO {
  id?: string | number;
  activityId?: string | number;
  categoryId?: string | number;
  columns?: ReviewScoreSummaryColumnVO[];
  scoreColumnCount?: number;
  warning?: ReviewScoreSummaryWarningVO;
  version?: number;
}

export interface ReviewScoreSummaryFilterOptionsVO {
  programForms?: string[];
  groupOrNatures?: string[];
}

export interface ProjectUploadSummaryItemVO {
  key?: string;
  label?: string;
  count?: number;
  percent?: number;
}

export interface ProjectUploadSchoolSummaryItemVO extends ProjectUploadSummaryItemVO {
  schoolId?: string | number;
  draftCount?: number;
  submittedCount?: number;
  returnedCount?: number;
  passedCount?: number;
}

export interface ProjectUploadSummaryVO {
  totalCount?: number;
  schoolCount?: number;
  statusItems?: ProjectUploadSummaryItemVO[];
  categoryItems?: ProjectUploadSummaryItemVO[];
  schoolItems?: ProjectUploadSchoolSummaryItemVO[];
  groupItems?: ProjectUploadSummaryItemVO[];
  projectTypeItems?: ProjectUploadSummaryItemVO[];
  programFormItems?: ProjectUploadSummaryItemVO[];
  performanceOrderItems?: ProjectUploadSummaryItemVO[];
}

export interface ProjectUploadOverviewActivityVO {
  id?: string | number;
  activityName?: string;
  year?: number;
}

export interface ProjectUploadOverviewCountVO {
  totalCount?: number;
  draftCount?: number;
  submittedCount?: number;
  auditedCount?: number;
  returnedCount?: number;
}

export interface ProjectUploadOverviewCategoryVO extends ProjectUploadOverviewCountVO {
  id?: string;
  categoryId?: string | number;
  categoryCode?: string;
  categoryName?: string;
  groupKey?: string;
  groupName?: string;
  groupSortOrder?: number;
  sortOrder?: number;
}

export interface ProjectUploadOverviewVO {
  activityId?: string | number;
  activityName?: string;
  activities?: ProjectUploadOverviewActivityVO[];
  summary?: ProjectUploadOverviewCountVO;
  groupItems?: ProjectUploadOverviewCategoryVO[];
  categoryItems?: ProjectUploadOverviewCategoryVO[];
}

export interface ProjectQuotaRatioItemVO {
  ruleId?: string | number;
  categoryId?: string | number;
  categoryName?: string;
  groupCode?: string;
  groupName?: string;
  targetFieldKey?: string;
  scopeCategoryGroup?: string;
  scopeCategoryCodes?: string;
  operator?: string;
  ratioValue?: number;
  ratio?: number;
  numerator?: number;
  denominator?: number;
  exceeded?: boolean;
  statusText?: string;
  message?: string;
  remark?: string;
}

export interface ProjectQuotaRatioSummaryVO {
  activityId?: string | number;
  activityName?: string;
  totalCount?: number;
  exceededCount?: number;
  activities?: ProjectUploadOverviewActivityVO[];
  items?: ProjectQuotaRatioItemVO[];
}

export interface ReviewProjectOverviewVO {
  activityId?: string | number;
  activityName?: string;
  categoryId?: string | number;
  categoryName?: string;
  projectId?: string | number;
  projectNo?: string;
  projectName?: string;
  schoolId?: string | number;
  schoolName?: string;
  groupName?: string;
  projectType?: string;
  programForm?: string;
  performanceOrder?: string;
  status?: string;
  submittedAt?: string;
  createTime?: string;
  assignmentCount?: number;
  pendingScoreCount?: number;
  submittedScoreCount?: number;
  draftScoreCount?: number;
  requiredReviewerCount?: number;
  shortageCount?: number;
  assignmentStatus?: 'unassigned' | 'shortage' | 'assigned';
  reviewerNames?: string;
  averageScore?: number;
  finalGrade?: string;
  awardLevel?: string;
  rankNo?: number;
  resultStatus?: string;
}

export interface ShowcaseOrderVO {
  activityId?: string | number;
  categoryId?: string | number;
  projectId?: string | number;
  performanceOrder?: string;
  activityName?: string;
  categoryName?: string;
  groupName?: string;
  projectType?: string;
  programForm?: string;
  projectNo?: string;
  projectName?: string;
  schoolName?: string;
  status?: string;
  submittedAt?: string;
  createTime?: string;
}

export interface ReviewAwardRuleVO {
  id?: string | number;
  activityId?: string | number;
  categoryId?: string | number;
  awardLevel?: string;
  ruleType?: string;
  minRank?: number;
  maxRank?: number;
  minScore?: number;
  maxScore?: number;
  sortOrder?: number;
  enabled?: boolean;
  remark?: string;
  createTime?: string;
}

export interface ReviewResultLogVO {
  id?: string | number;
  activityId?: string | number;
  categoryId?: string | number;
  projectId?: string | number;
  scoreId?: string | number;
  targetType?: string;
  actionType?: string;
  beforeJson?: string;
  afterJson?: string;
  reason?: string;
  operatedBy?: string | number;
  operatedByName?: string;
  operatedAt?: string;
  createTime?: string;
}

export interface ActivityRuleGroupOptionVO {
  id?: string | number;
  activityId?: string | number;
  categoryId?: string | number;
  schoolId?: string | number;
  schoolType?: string;
  groupCode?: string;
  groupName?: string;
  ruleType?: string;
  limitCount?: number;
  ratioValue?: number;
  ruleJson?: string;
  enabled?: boolean;
  remark?: string;
}

export interface ReportRulePackageVO {
  id?: string | number;
  packageCode?: string;
  packageName?: string;
  activityType?: string;
  versionNo?: string;
  status?: string;
  enabled?: boolean;
  remark?: string;
  createTime?: string;
}

export interface ReportRuleItemVO {
  id?: string | number;
  packageId?: string | number;
  ruleCode?: string;
  ruleName?: string;
  ruleGroup?: string;
  ruleType?: string;
  scopeType?: string;
  scopeCategoryGroup?: string;
  categoryCode?: string;
  categoryId?: string | number;
  schoolType?: string;
  schoolId?: string | number;
  targetFieldKey?: string;
  targetValue?: string;
  operator?: string;
  limitCount?: number;
  ratioValue?: number;
  minValue?: number;
  maxValue?: number;
  enforceMode?: string;
  message?: string;
  ruleJson?: string;
  enabled?: boolean;
  sortOrder?: number;
  remark?: string;
  createTime?: string;
}

export interface ActivityReportRuleVO extends ReportRuleItemVO {
  activityId?: string | number;
  packageItemId?: string | number;
}
