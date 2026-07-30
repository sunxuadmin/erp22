import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import {
  ActivityCategoryPurgeCheckVO,
  ActivityCategoryVO,
  ActivityConfigImportResultVO,
  ActivityConfigMergePreviewVO,
  ActivityDeleteCheckVO,
  ActivityVO,
  CategoryFieldSchemaVO,
  CategoryFileRequirementVO
} from './types';

export const listActivity = (query: any): AxiosPromise<ActivityVO[]> => {
  return request({ url: '/crehn/activity/list', method: 'get', params: query });
};

export const listActivityOptions = (): AxiosPromise<ActivityVO[]> => {
  return request({ url: '/crehn/activity/options', method: 'get' });
};

export const listCategoryOptions = (activityId: string | number): AxiosPromise<ActivityCategoryVO[]> => {
  return request({ url: `/crehn/activity/category/options/${activityId}`, method: 'get' });
};

export const getActivity = (id: string | number): AxiosPromise<ActivityVO> => {
  return request({ url: `/crehn/activity/${id}`, method: 'get' });
};

export const addActivity = (data: ActivityVO) => {
  return request({ url: '/crehn/activity', method: 'post', data });
};

export const updateActivity = (data: ActivityVO) => {
  return request({ url: '/crehn/activity', method: 'put', data });
};

export const delActivity = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/activity/${ids}`, method: 'delete' });
};

export const restoreActivity = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/activity/restore/${ids}`, method: 'put' });
};

export const checkActivityDelete = (id: string | number): AxiosPromise<ActivityDeleteCheckVO> => {
  return request({ url: `/crehn/activity/${id}/delete-check`, method: 'get' });
};

export const checkActivityPurge = (id: string | number): AxiosPromise<ActivityDeleteCheckVO> => {
  return request({ url: `/crehn/activity/${id}/purge-check`, method: 'get' });
};

export const purgeActivity = (id: string | number) => {
  return request({ url: `/crehn/activity/${id}/purge`, method: 'delete' });
};

export const exportActivityConfigJson = (id: string | number) => {
  return request({ url: `/crehn/activity/${id}/config-package/export-json`, method: 'get', responseType: 'blob' });
};

export const exportActivityConfigExcel = (id: string | number) => {
  return request({ url: `/crehn/activity/${id}/config-package/export-excel`, method: 'get', responseType: 'blob' });
};

export const importActivityConfig = (file: File): AxiosPromise<ActivityConfigImportResultVO> => {
  const data = new FormData();
  data.append('file', file);
  return request({ url: '/crehn/activity/config-package/import', method: 'post', data });
};

export const previewActivityConfigMerge = (activityId: string | number, file: File): AxiosPromise<ActivityConfigMergePreviewVO> => {
  const data = new FormData();
  data.append('file', file);
  return request({ url: `/crehn/activity/${activityId}/config-package/merge-preview`, method: 'post', data });
};

export const mergeActivityConfig = (activityId: string | number, file: File): AxiosPromise<ActivityConfigMergePreviewVO> => {
  const data = new FormData();
  data.append('file', file);
  return request({ url: `/crehn/activity/${activityId}/config-package/merge-import`, method: 'post', data });
};

export const listCategory = (activityId: string | number, query?: any): AxiosPromise<ActivityCategoryVO[]> => {
  return request({ url: `/crehn/activity/category/list/${activityId}`, method: 'get', params: query });
};

export const addCategory = (data: ActivityCategoryVO) => {
  return request({ url: '/crehn/activity/category', method: 'post', data });
};

export const copyCategory = (id: string | number): AxiosPromise<ActivityCategoryVO> => {
  return request({ url: `/crehn/activity/category/${id}/copy`, method: 'post' });
};

export const updateCategory = (data: ActivityCategoryVO) => {
  return request({ url: '/crehn/activity/category', method: 'put', data });
};

export const delCategory = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/activity/category/${ids}`, method: 'delete' });
};

export const restoreCategory = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/activity/category/restore/${ids}`, method: 'put' });
};

export const checkCategoryDelete = (id: string | number): AxiosPromise<ActivityCategoryPurgeCheckVO> => {
  return request({ url: `/crehn/activity/category/${id}/delete-check`, method: 'get' });
};

export const checkCategoryPurge = (id: string | number): AxiosPromise<ActivityCategoryPurgeCheckVO> => {
  return request({ url: `/crehn/activity/category/${id}/purge-check`, method: 'get' });
};

export const purgeCategory = (id: string | number) => {
  return request({ url: `/crehn/activity/category/${id}/purge`, method: 'delete' });
};

export const listField = (categoryId: string | number, query?: any): AxiosPromise<CategoryFieldSchemaVO[]> => {
  return request({ url: `/crehn/activity/field/list/${categoryId}`, method: 'get', params: query });
};

export const addField = (data: CategoryFieldSchemaVO) => {
  return request({ url: '/crehn/activity/field', method: 'post', data });
};

export const updateField = (data: CategoryFieldSchemaVO) => {
  return request({ url: '/crehn/activity/field', method: 'put', data });
};

export const delField = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/activity/field/${ids}`, method: 'delete' });
};

export const restoreField = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/activity/field/restore/${ids}`, method: 'put' });
};

export const getFieldUsageCount = (id: string | number): AxiosPromise<number> => {
  return request({ url: `/crehn/activity/field/${id}/usage-count`, method: 'get' });
};

export const listFileRequirement = (categoryId: string | number, query?: any): AxiosPromise<CategoryFileRequirementVO[]> => {
  return request({ url: `/crehn/activity/file-requirement/list/${categoryId}`, method: 'get', params: query });
};

export const addFileRequirement = (data: CategoryFileRequirementVO) => {
  return request({ url: '/crehn/activity/file-requirement', method: 'post', data });
};

export const updateFileRequirement = (data: CategoryFileRequirementVO) => {
  return request({ url: '/crehn/activity/file-requirement', method: 'put', data });
};

export const delFileRequirement = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/activity/file-requirement/${ids}`, method: 'delete' });
};

export const restoreFileRequirement = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/activity/file-requirement/restore/${ids}`, method: 'put' });
};

export const getFileRequirementUsageCount = (id: string | number): AxiosPromise<number> => {
  return request({ url: `/crehn/activity/file-requirement/${id}/usage-count`, method: 'get' });
};

export const uploadFileRequirementTemplate = (categoryId: string | number, file: File) => {
  const data = new FormData();
  data.append('file', file);
  return request({ url: '/crehn/activity/file-requirement/template/upload', method: 'post', params: { categoryId }, data });
};

export const listAvailableActivity = (): AxiosPromise<ActivityVO[]> => {
  return request({ url: '/crehn/activity/school/available', method: 'get' });
};

export const listAvailableCategory = (activityId: string | number): AxiosPromise<ActivityCategoryVO[]> => {
  return request({ url: `/crehn/activity/school/${activityId}/category`, method: 'get' });
};

export const listSchoolCategoryCatalog = (activityId: string | number): AxiosPromise<ActivityCategoryVO[]> => {
  return request({ url: `/crehn/activity/school/${activityId}/category/catalog`, method: 'get' });
};

export const listSchoolField = (categoryId: string | number): AxiosPromise<CategoryFieldSchemaVO[]> => {
  return request({ url: `/crehn/activity/school/category/${categoryId}/field`, method: 'get' });
};

export const listSchoolFileRequirement = (categoryId: string | number): AxiosPromise<CategoryFileRequirementVO[]> => {
  return request({ url: `/crehn/activity/school/category/${categoryId}/file-requirement`, method: 'get' });
};
