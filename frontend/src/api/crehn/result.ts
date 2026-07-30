import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import {
  ProjectUploadOverviewVO,
  ProjectQuotaRatioSummaryVO,
  ProjectUploadSummaryVO,
  ReviewAwardRuleVO,
  ReviewProjectOverviewVO,
  ReviewResultReadinessVO,
  ReviewResultLogVO,
  ReviewResultVO,
  ReviewScoreSummaryConfigVO,
  ReviewScoreSummaryDetailVO,
  ReviewScoreSummaryVO,
  ShowcaseOrderVO
} from './types';

export const listReviewResult = (query: any): AxiosPromise<ReviewResultVO[]> => {
  return request({ url: '/crehn/result/list', method: 'get', params: query });
};

export const listMyReviewResult = (query: any): AxiosPromise<ReviewResultVO[]> => {
  return request({ url: '/crehn/result/my-list', method: 'get', params: query });
};

export const generateReviewResult = (data: { activityId?: string | number; categoryId?: string | number; remark?: string }) => {
  return request({ url: '/crehn/result/generate', method: 'post', data });
};

export const getReviewResultReadiness = (activityId?: string | number, categoryId?: string | number): AxiosPromise<ReviewResultReadinessVO> => {
  return request({ url: '/crehn/result/readiness', method: 'get', params: { activityId, categoryId } });
};

export const withdrawGeneratedReviewResult = (data: { activityId?: string | number; categoryId?: string | number; remark?: string }) => {
  return request({ url: '/crehn/result/generate/withdraw', method: 'post', data });
};

export const publishReviewResult = (data: {
  activityId?: string | number;
  categoryId?: string | number;
  showScore?: boolean;
  showRank?: boolean;
  showComment?: boolean;
  notifySchoolAccounts?: boolean;
  remark?: string;
}) => {
  return request({ url: '/crehn/result/publish', method: 'post', data });
};

export const unpublishReviewResult = (data: { activityId?: string | number; categoryId?: string | number; remark?: string }) => {
  return request({ url: '/crehn/result/unpublish', method: 'post', data });
};

export const listReviewAwardRules = (activityId?: string | number, categoryId?: string | number): AxiosPromise<ReviewAwardRuleVO[]> => {
  return request({ url: '/crehn/result/award-rule/list', method: 'get', params: { activityId, categoryId } });
};

export const saveReviewAwardRules = (data: { activityId?: string | number; categoryId?: string | number; rules: ReviewAwardRuleVO[] }) => {
  return request({ url: '/crehn/result/award-rule/save', method: 'post', data });
};

export const listReviewResultLogs = (query: any): AxiosPromise<ReviewResultLogVO[]> => {
  return request({ url: '/crehn/result/log/list', method: 'get', params: query });
};

export const getUploadSummary = (query: any): AxiosPromise<ProjectUploadSummaryVO> => {
  return request({ url: '/crehn/result/upload-summary', method: 'get', params: query });
};

export const getUploadOverview = (query: any): AxiosPromise<ProjectUploadOverviewVO> => {
  return request({ url: '/crehn/result/upload-overview', method: 'get', params: query });
};

export const getQuotaRatioSummary = (query: any): AxiosPromise<ProjectQuotaRatioSummaryVO> => {
  return request({ url: '/crehn/result/quota-ratio-summary', method: 'get', params: query });
};

export const listProjectReviewOverview = (query: any): AxiosPromise<ReviewProjectOverviewVO[]> => {
  return request({ url: '/crehn/result/project-overview/list', method: 'get', params: query });
};

export const listShowcaseOrders = (query: any): AxiosPromise<ShowcaseOrderVO[]> => {
  return request({ url: '/crehn/result/showcase-order/list', method: 'get', params: query });
};

export const saveShowcaseOrders = (data: {
  activityId?: string | number;
  categoryId?: string | number;
  remark?: string;
  items: ShowcaseOrderVO[];
}) => {
  return request({ url: '/crehn/result/showcase-order/save', method: 'post', data });
};

export const listReviewScoreSummary = (query: any): AxiosPromise<ReviewScoreSummaryVO[]> => {
  return request({ url: '/crehn/result/score-summary/page', method: 'get', params: query });
};

export const listReviewScoreSummaryFilterOptions = (activityId?: string | number, categoryId?: string | number): AxiosPromise<any> => {
  return request({ url: '/crehn/result/score-summary/options', method: 'get', params: { activityId, categoryId } });
};

export const getReviewScoreSummaryDetail = (projectId: string | number, query?: any): AxiosPromise<ReviewScoreSummaryDetailVO> => {
  return request({ url: `/crehn/result/score-summary/${projectId}`, method: 'get', params: query });
};

export const getReviewScoreSummaryConfig = (activityId?: string | number, categoryId?: string | number): AxiosPromise<any> => {
  return request({ url: '/crehn/result/score-warning-config', method: 'get', params: { activityId, categoryId } });
};

export const saveReviewScoreSummaryConfig = (data: ReviewScoreSummaryConfigVO) => {
  const warning = data.warning || {};
  return request({
    url: '/crehn/result/score-warning-config',
    method: 'post',
    data: {
      activityId: data.activityId,
      categoryId: data.categoryId,
      enabled: warning.enabled,
      formulaType: warning.formula || 'range_average',
      formulaExpression: warning.formulaExpression,
      thresholdPercent: warning.thresholdPercent,
      normalText: warning.normalText,
      warningText: warning.warningText,
      insufficientText: warning.insufficientText,
      unsupportedText: warning.unsupportedText,
      columnsJson: data.columns ? JSON.stringify(data.columns) : undefined,
      scoreColumnCount: data.scoreColumnCount
    }
  });
};
