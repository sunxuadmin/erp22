import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import {
  ActivityCategoryVO,
  ActivityVO,
  AssignmentBatchPreviewVO,
  CategoryFieldSchemaVO,
  ProjectVO,
  ReviewAssignmentVO,
  ReviewAssignmentCategoryConfigVO,
  ReviewAssignmentScopePlanForm,
  ReviewProjectOverviewVO,
  ReviewScoreAdminForm,
  ReviewScoreSheetExportForm,
  ReviewScoreSheetExportPreviewVO,
  ReviewScoreSheetSignatureAdminQuery,
  ReviewScoreSheetSignatureAdminVO,
  ReviewScoreSheetSignatureVO,
  ReviewScoreSheetSignedSheetForm,
  ReviewScoreSheetSignedSheetPreviewVO,
  ReviewScoreSheetSignedSheetQuery,
  ReviewScoreSheetSignedSheetReviewerOptionVO,
  ReviewScoreSheetSignedSheetVO,
  ReviewScoreSheetSignedSheetWithdrawForm,
  ReviewScoreSheetTemplateVO,
  ReviewScoreVO,
  ReviewTaskNavigatorVO,
  ReviewTaskVO,
  ReviewWorkbenchVO
} from './types';

export const listReviewAssignment = (query: any): AxiosPromise<ReviewAssignmentVO[]> => {
  return request({ url: '/crehn/review/assignment/list', method: 'get', params: query });
};

export const getReviewAssignment = (id: string | number): AxiosPromise<ReviewAssignmentVO> => {
  return request({ url: `/crehn/review/assignment/${id}`, method: 'get' });
};

export const addReviewAssignment = (data: ReviewAssignmentVO) => {
  return request({ url: '/crehn/review/assignment', method: 'post', data });
};

export const previewReviewAssignmentBatch = (data: ReviewAssignmentVO): AxiosPromise<AssignmentBatchPreviewVO> => {
  return request({ url: '/crehn/review/assignment/batch/preview', method: 'post', data });
};

export const maintainReviewAssignmentBatch = (data: ReviewAssignmentVO): AxiosPromise<AssignmentBatchPreviewVO> => {
  return request({ url: '/crehn/review/assignment/batch', method: 'post', data });
};

export const previewReviewAssignmentScopePlan = (data: ReviewAssignmentScopePlanForm): AxiosPromise<AssignmentBatchPreviewVO> => {
  return request({ url: '/crehn/review/assignment/scope-plan/preview', method: 'post', data });
};

export const maintainReviewAssignmentScopePlan = (data: ReviewAssignmentScopePlanForm): AxiosPromise<AssignmentBatchPreviewVO> => {
  return request({ url: '/crehn/review/assignment/scope-plan', method: 'post', data });
};

export const listReviewAssignmentProjects = (query: any): AxiosPromise<ReviewProjectOverviewVO[]> => {
  return request({ url: '/crehn/review/assignment/project/list', method: 'get', params: query });
};

export const updateReviewAssignment = (data: ReviewAssignmentVO) => {
  return request({ url: '/crehn/review/assignment', method: 'put', data });
};

export const delReviewAssignment = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/review/assignment/${ids}`, method: 'delete' });
};

export const listReviewerOptions = (
  keyword?: string,
  activityId?: string | number,
  categoryId?: string | number,
  includeUserId?: string | number
) => {
  return request({ url: '/crehn/review/reviewer/options', method: 'get', params: { keyword, activityId, categoryId, includeUserId } });
};

export const listApprovedReviewProjectOptions = (
  activityId?: string | number,
  categoryId?: string | number,
  keyword?: string
): AxiosPromise<ProjectVO[]> => {
  return request({ url: '/crehn/review/project/options', method: 'get', params: { activityId, categoryId, keyword } });
};

export const listReviewAssignmentSchoolOptions = (keyword?: string) => {
  return request({ url: '/crehn/review/assignment/school/options', method: 'get', params: { keyword } });
};

export const listReviewAssignmentFilterFields = (categoryId: string | number): AxiosPromise<CategoryFieldSchemaVO[]> => {
  return request({ url: `/crehn/review/assignment/filter-fields/${categoryId}`, method: 'get' });
};

export const getReviewAssignmentCategoryConfig = (categoryId: string | number): AxiosPromise<ReviewAssignmentCategoryConfigVO> => {
  return request({ url: `/crehn/review/assignment/category-config/${categoryId}`, method: 'get' });
};

export const saveReviewAssignmentCategoryConfig = (
  categoryId: string | number,
  data: ReviewAssignmentCategoryConfigVO
): AxiosPromise<ReviewAssignmentCategoryConfigVO> => {
  return request({ url: `/crehn/review/assignment/category-config/${categoryId}`, method: 'put', data });
};

export const listMyReviewTask = (query: any): AxiosPromise<ReviewTaskVO[]> => {
  return request({ url: '/crehn/review/task/my-list', method: 'get', params: query });
};

export const getMyReviewTaskNavigator = (query: any): AxiosPromise<ReviewTaskNavigatorVO> => {
  return request({ url: '/crehn/review/task/navigator', method: 'get', params: query });
};

export const getReviewWorkbench = (): AxiosPromise<ReviewWorkbenchVO> => {
  return request({ url: '/crehn/review/workbench', method: 'get' });
};

export const listMyReviewActivityOptions = (): AxiosPromise<ActivityVO[]> => {
  return request({ url: '/crehn/review/activity/options', method: 'get' });
};

export const listMyReviewCategoryOptions = (activityId: string | number): AxiosPromise<ActivityCategoryVO[]> => {
  return request({ url: `/crehn/review/category/options/${activityId}`, method: 'get' });
};

export const getMyReviewTask = (assignmentId: string | number, projectId: string | number): AxiosPromise<ReviewTaskVO> => {
  return request({ url: `/crehn/review/task/${assignmentId}/${projectId}`, method: 'get' });
};

export const saveReviewScoreDraft = (data: ReviewScoreVO): AxiosPromise<ReviewScoreVO> => {
  return request({ url: '/crehn/review/score/draft', method: 'post', data });
};

export const submitReviewScore = (data: ReviewScoreVO): AxiosPromise<ReviewScoreVO> => {
  return request({ url: '/crehn/review/score/submit', method: 'post', data });
};

export const listProjectReviewScores = (projectId: string | number): AxiosPromise<ReviewScoreVO[]> => {
  return request({ url: `/crehn/review/score/project/${projectId}`, method: 'get' });
};

export const adjustReviewScore = (data: ReviewScoreAdminForm): AxiosPromise<ReviewScoreVO> => {
  return request({ url: '/crehn/review/score/adjust', method: 'post', data });
};

export const returnReviewScore = (data: ReviewScoreAdminForm): AxiosPromise<ReviewScoreVO> => {
  return request({ url: '/crehn/review/score/return', method: 'post', data });
};

// ART-REF: FE.REVIEW.SCORE_SHEET
export const getReviewScoreSheetTemplate = (): AxiosPromise<ReviewScoreSheetTemplateVO> => {
  return request({ url: '/crehn/review-score-sheet/template', method: 'get' });
};

export const listReviewScoreSheetTemplates = (): AxiosPromise<ReviewScoreSheetTemplateVO[]> => {
  return request({ url: '/crehn/review-score-sheet/templates', method: 'get' });
};

export const getReviewScoreSheetTemplateById = (templateId: string | number): AxiosPromise<ReviewScoreSheetTemplateVO> => {
  return request({ url: `/crehn/review-score-sheet/template/${templateId}`, method: 'get' });
};

export const createReviewScoreSheetTemplate = (data: ReviewScoreSheetTemplateVO): AxiosPromise<ReviewScoreSheetTemplateVO> => {
  return request({ url: '/crehn/review-score-sheet/template', method: 'post', data });
};

export const saveReviewScoreSheetTemplate = (data: ReviewScoreSheetTemplateVO): AxiosPromise<ReviewScoreSheetTemplateVO> => {
  return request({ url: '/crehn/review-score-sheet/template', method: 'put', data });
};

export const updateReviewScoreSheetTemplate = (
  templateId: string | number,
  data: ReviewScoreSheetTemplateVO
): AxiosPromise<ReviewScoreSheetTemplateVO> => {
  return request({ url: `/crehn/review-score-sheet/template/${templateId}`, method: 'put', data });
};

export const setDefaultReviewScoreSheetTemplate = (templateId: string | number): AxiosPromise<ReviewScoreSheetTemplateVO> => {
  return request({ url: `/crehn/review-score-sheet/template/${templateId}/default`, method: 'post' });
};

export const deleteReviewScoreSheetTemplate = (templateId: string | number) => {
  return request({ url: `/crehn/review-score-sheet/template/${templateId}`, method: 'delete' });
};

export const resetReviewScoreSheetTemplate = (version: number): AxiosPromise<ReviewScoreSheetTemplateVO> => {
  return request({ url: '/crehn/review-score-sheet/template/reset', method: 'post', params: { version } });
};

export const resetReviewScoreSheetTemplateById = (templateId: string | number, version: number): AxiosPromise<ReviewScoreSheetTemplateVO> => {
  return request({ url: `/crehn/review-score-sheet/template/${templateId}/reset`, method: 'post', params: { version } });
};

export const previewReviewScoreSheet = (data: ReviewScoreSheetExportForm): AxiosPromise<ReviewScoreSheetExportPreviewVO> => {
  return request({ url: '/crehn/review-score-sheet/export/preview', method: 'post', data });
};

export const exportReviewScoreSheet = (data: ReviewScoreSheetExportForm) => {
  return request({
    url: '/crehn/review-score-sheet/export',
    method: 'post',
    data,
    responseType: 'blob'
  });
};

// Handwritten reviewer-signature library. The backend derives the owner from
// the login session; the browser never submits an owner user id.
export const listReviewScoreSheetSignatures = (): AxiosPromise<ReviewScoreSheetSignatureVO[]> => {
  return request({ url: '/crehn/review-score-sheet/signatures', method: 'get' });
};

export const createReviewScoreSheetSignature = (file: File, signatureName: string, defaultSignature = false) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('signatureName', signatureName);
  formData.append('defaultSignature', String(defaultSignature));
  return request({ url: '/crehn/review-score-sheet/signatures', method: 'post', data: formData });
};

/** Replaces only the reusable asset. Existing signed-sheet snapshots remain unchanged. */
export const replaceReviewScoreSheetSignature = (signatureId: string | number, file: File, signatureName: string) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('signatureName', signatureName);
  return request({ url: `/crehn/review-score-sheet/signatures/${signatureId}/replace`, method: 'post', data: formData });
};

export const setDefaultReviewScoreSheetSignature = (signatureId: string | number) => {
  return request({ url: `/crehn/review-score-sheet/signatures/${signatureId}/default`, method: 'post' });
};

export const deleteReviewScoreSheetSignature = (signatureId: string | number) => {
  return request({ url: `/crehn/review-score-sheet/signatures/${signatureId}`, method: 'delete' });
};

/** Privileged cross-reviewer signature-library management; personal APIs above stay owner-scoped. */
export const pageReviewScoreSheetSignatureAdmin = (query?: ReviewScoreSheetSignatureAdminQuery) => {
  return request({ url: '/crehn/review-score-sheet/signature-admin/signatures/page', method: 'get', params: query });
};

export const getReviewScoreSheetSignatureAdmin = (signatureId: string | number): AxiosPromise<ReviewScoreSheetSignatureAdminVO> => {
  return request({ url: `/crehn/review-score-sheet/signature-admin/signatures/${signatureId}`, method: 'get' });
};

export const disableReviewScoreSheetSignatureAdmin = (signatureId: string | number, reason: string) => {
  return request({ url: `/crehn/review-score-sheet/signature-admin/signatures/${signatureId}/disable`, method: 'post', data: { reason } });
};

export const restoreReviewScoreSheetSignatureAdmin = (signatureId: string | number, reason: string) => {
  return request({ url: `/crehn/review-score-sheet/signature-admin/signatures/${signatureId}/restore`, method: 'post', data: { reason } });
};

export const archiveReviewScoreSheetSignatureAdmin = (signatureId: string | number, reason: string) => {
  return request({ url: `/crehn/review-score-sheet/signature-admin/signatures/${signatureId}/archive`, method: 'post', data: { reason } });
};

// Signed-sheet contract: the service derives the next complete draft batch
// from the opaque reviewer scope and snapshots the selected signature.
export const previewReviewScoreSheetSignedSheet = (
  data: Pick<ReviewScoreSheetSignedSheetForm, 'activityId' | 'categoryId' | 'scopeKey' | 'templateId' | 'template'>
): AxiosPromise<ReviewScoreSheetSignedSheetPreviewVO> => {
  return request({ url: '/crehn/review-score-sheet/signed-sheets/preview', method: 'post', data });
};

export const saveReviewScoreSheetSignedSheet = (data: ReviewScoreSheetSignedSheetForm): AxiosPromise<ReviewScoreSheetSignedSheetVO> => {
  return request({ url: '/crehn/review-score-sheet/signed-sheets', method: 'post', data });
};

export const listReviewScoreSheetSignedSheets = (query?: ReviewScoreSheetSignedSheetQuery): AxiosPromise<ReviewScoreSheetSignedSheetVO[]> => {
  return request({ url: '/crehn/review-score-sheet/signed-sheets', method: 'get', params: query });
};

/**
 * Paginated signed-sheet history. The service forces expert callers to their
 * own records and permits the wider scope only for crehn_admin/superadmin.
 */
export const pageReviewScoreSheetSignedSheets = (query?: ReviewScoreSheetSignedSheetQuery) => {
  return request({ url: '/crehn/review-score-sheet/signed-sheets/page', method: 'get', params: query });
};

export const getReviewScoreSheetSignedSheet = (sheetId: string | number): AxiosPromise<ReviewScoreSheetSignedSheetVO> => {
  return request({ url: `/crehn/review-score-sheet/signed-sheets/${sheetId}`, method: 'get' });
};

export const exportReviewScoreSheetSignedSheet = (sheetId: string | number) => {
  return request({
    url: `/crehn/review-score-sheet/signed-sheets/${sheetId}/export`,
    method: 'get',
    responseType: 'blob'
  });
};

/** Historical signer choices for the signed-sheet management page (crehn_admin/superadmin only). */
export const listReviewScoreSheetSignedSheetReviewerOptions = (query?: {
  activityId?: string | number;
  categoryId?: string | number;
  keyword?: string;
}): AxiosPromise<ReviewScoreSheetSignedSheetReviewerOptionVO[]> => {
  return request({ url: '/crehn/review-score-sheet/signed-sheets/reviewer-options', method: 'get', params: query });
};

/** Records an immutable withdrawal audit trail; it never deletes the sheet. */
export const withdrawReviewScoreSheetSignedSheet = (
  sheetId: string | number,
  data: ReviewScoreSheetSignedSheetWithdrawForm
): AxiosPromise<ReviewScoreSheetSignedSheetVO> => {
  return request({ url: `/crehn/review-score-sheet/signed-sheets/${sheetId}/withdraw`, method: 'post', data });
};
