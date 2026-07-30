import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import {
  ActivityCategoryVO,
  ActivityVO,
  AssignmentBatchPreviewVO,
  AuditAssignmentVO,
  AuditWorkbenchVO,
  ProjectQuotaOverviewVO,
  ProjectVO
} from './types';

export const listAuditProject = (query: any): AxiosPromise<ProjectVO[]> => {
  return request({ url: '/crehn/audit/list', method: 'get', params: query });
};

export const getAuditWorkbench = (): AxiosPromise<AuditWorkbenchVO> => {
  return request({ url: '/crehn/audit/workbench', method: 'get' });
};

export const getAuditQuotaOverview = (query: any): AxiosPromise<ProjectQuotaOverviewVO> => {
  return request({ url: '/crehn/audit/quota-overview', method: 'get', params: query });
};

export const getAuditProject = (id: string | number): AxiosPromise<ProjectVO> => {
  return request({ url: `/crehn/audit/${id}`, method: 'get' });
};

export const listAuditSchoolOptions = (query?: any) => {
  return request({ url: '/crehn/audit/school/options', method: 'get', params: query });
};

export const listAuditActivityOptions = (): AxiosPromise<ActivityVO[]> => {
  return request({ url: '/crehn/audit/activity/options', method: 'get' });
};

export const listAuditCategoryOptions = (activityId: string | number): AxiosPromise<ActivityCategoryVO[]> => {
  return request({ url: `/crehn/audit/category/options/${activityId}`, method: 'get' });
};

export const passProject = (projectId: string | number, opinion?: string, notifyEnabled = false) => {
  return request({ url: '/crehn/audit/pass', method: 'post', data: { projectId, opinion, notifyEnabled } });
};

export const returnProject = (projectId: string | number, opinion: string, notifyEnabled = false) => {
  return request({ url: '/crehn/audit/return', method: 'post', data: { projectId, opinion, notifyEnabled } });
};

export const withdrawReturnProject = (projectId: string | number, opinion?: string, notifyEnabled = false) => {
  return request({ url: '/crehn/audit/withdraw-return', method: 'post', data: { projectId, opinion, notifyEnabled } });
};

export const withdrawPassProject = (projectId: string | number, opinion?: string, notifyEnabled = false) => {
  return request({ url: '/crehn/audit/withdraw-pass', method: 'post', data: { projectId, opinion, notifyEnabled } });
};

export const listAuditAssignment = (query: any): AxiosPromise<AuditAssignmentVO[]> => {
  return request({ url: '/crehn/audit/assignment/list', method: 'get', params: query });
};

export const getAuditAssignment = (id: string | number): AxiosPromise<AuditAssignmentVO> => {
  return request({ url: `/crehn/audit/assignment/${id}`, method: 'get' });
};

export const listAuditorOptions = (keyword?: string) => {
  return request({ url: '/crehn/audit/auditor/options', method: 'get', params: { keyword } });
};

export const listAuditAssignmentSchoolOptions = (keyword?: string) => {
  return request({ url: '/crehn/audit/assignment/school/options', method: 'get', params: { keyword } });
};

export const addAuditAssignment = (data: AuditAssignmentVO) => {
  return request({ url: '/crehn/audit/assignment', method: 'post', data });
};

export const previewAuditAssignmentBatch = (data: AuditAssignmentVO): AxiosPromise<AssignmentBatchPreviewVO> => {
  return request({ url: '/crehn/audit/assignment/batch/preview', method: 'post', data });
};

export const maintainAuditAssignmentBatch = (data: AuditAssignmentVO): AxiosPromise<AssignmentBatchPreviewVO> => {
  return request({ url: '/crehn/audit/assignment/batch', method: 'post', data });
};

export const updateAuditAssignment = (data: AuditAssignmentVO) => {
  return request({ url: '/crehn/audit/assignment', method: 'put', data });
};

export const deleteAuditAssignment = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/audit/assignment/${ids}`, method: 'delete' });
};
