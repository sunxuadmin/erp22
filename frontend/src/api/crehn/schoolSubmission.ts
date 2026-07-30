import request from '@/utils/request';
import type { ProjectVO } from './types';

export const listSchoolSubmission = (activityId?: string | number) =>
  request<ProjectVO[]>({
    url: '/crehn/school-submission/pending',
    method: 'get',
    params: activityId ? { activityId } : undefined
  });

export const reviewSchoolProject = (data: {
  projectId: string | number;
  result: 'pass' | 'return';
  opinion?: string;
}) => request({ url: '/crehn/school-submission/review', method: 'post', data });

export const finalSubmitSchoolProjects = (data: {
  activityId: string | number;
  projectIds: Array<string | number>;
}) => request({ url: '/crehn/school-submission/final-submit', method: 'post', data });
