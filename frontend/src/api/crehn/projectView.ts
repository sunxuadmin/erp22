import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { ActivityCategoryVO, ActivityVO, ProjectQuotaOverviewVO, ProjectVO } from './types';

export const listProjectView = (query: any): AxiosPromise<ProjectVO[]> => {
  return request({ url: '/crehn/project-view/list', method: 'get', params: query });
};

export const listProjectViewStats = (query: any) => {
  return request({ url: '/crehn/project-view/stats', method: 'get', params: query });
};

export const getProjectViewQuotaOverview = (query: any): AxiosPromise<ProjectQuotaOverviewVO> => {
  return request({ url: '/crehn/project-view/quota-overview', method: 'get', params: query });
};

export const getProjectView = (id: string | number, withFile = true): AxiosPromise<ProjectVO> => {
  return request({ url: `/crehn/project-view/${id}`, method: 'get', params: { withFile } });
};

export const recycleProjectView = (projectId: string | number, reason?: string) => {
  return request({ url: '/crehn/project-view/recycle', method: 'post', data: { projectId, reason } });
};

export const listProjectViewActivityOptions = (): AxiosPromise<ActivityVO[]> => {
  return request({ url: '/crehn/project-view/activity/options', method: 'get' });
};

export const listProjectViewCategoryOptions = (activityId: string | number): AxiosPromise<ActivityCategoryVO[]> => {
  return request({ url: `/crehn/project-view/category/options/${activityId}`, method: 'get' });
};

export const listProjectViewSchoolOptions = (query?: any) => {
  return request({ url: '/crehn/project-view/school/options', method: 'get', params: query });
};
