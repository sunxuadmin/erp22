import request from '@/utils/request';

export const listSchoolInfo = (query: any) => {
  return request({ url: '/crehn/school-info/list', method: 'get', params: query });
};

export const listSchoolOptions = (query?: any) => {
  return request({ url: '/crehn/school-info/options', method: 'get', params: query });
};

export const saveSchoolInfo = (data: any) => {
  return request({ url: '/crehn/school-info', method: 'post', data });
};

export const getSchoolInfoImpact = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/school-info/impact/${ids}`, method: 'get' });
};

export const deleteSchoolInfo = (ids: string | number | Array<string | number>, confirmed = false) => {
  return request({ url: `/crehn/school-info/${ids}`, method: 'delete', params: { confirmed } });
};

export const restoreSchoolInfo = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/school-info/restore/${ids}`, method: 'put' });
};

export const importSchoolInfoUrl = () => {
  return import.meta.env.VITE_APP_BASE_API + '/crehn/school-info/importData';
};

export const listSchoolField = (query?: any) => {
  return request({ url: '/crehn/school-field/list', method: 'get', params: query });
};

export const saveSchoolField = (data: any) => {
  return request({ url: '/crehn/school-field', method: 'post', data });
};

export const listNoticeTemplate = (query: any) => {
  return request({ url: '/crehn/notice-template/list', method: 'get', params: query });
};

export const saveNoticeTemplate = (data: any) => {
  return request({ url: '/crehn/notice-template', method: 'post', data });
};

export const listBusinessRoleOptions = (query?: any) => {
  return request({ url: '/crehn/business-role/options', method: 'get', params: query });
};

export const addBusinessRole = (data: any) => {
  return request({ url: '/crehn/business-role', method: 'post', data });
};

export const listActivityScope = (query: any) => {
  return request({ url: '/crehn/activity-scope/list', method: 'get', params: query });
};

export const assignActivityScope = (data: any) => {
  return request({ url: '/crehn/activity-scope/assign', method: 'post', data });
};

export const deleteActivityScope = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/activity-scope/${ids}`, method: 'delete' });
};

export const listUploadTemplate = (query: any) => {
  return request({ url: '/crehn/upload-template/list', method: 'get', params: query });
};

export const saveUploadTemplate = (data: any) => {
  return request({ url: '/crehn/upload-template', method: 'post', data });
};

export const copyUploadTemplate = (id: string | number) => {
  return request({ url: `/crehn/upload-template/copy/${id}`, method: 'post' });
};

export const saveTemplateFromCategory = (categoryId: string | number, templateName: string) => {
  return request({ url: `/crehn/upload-template/from-category/${categoryId}`, method: 'post', params: { templateName } });
};

export const syncUploadTemplatesFromActivity = (activityId: string | number) => {
  return request({ url: `/crehn/upload-template/sync-from-activity/${activityId}`, method: 'post' });
};

export const applyUploadTemplate = (data: any) => {
  return request({ url: '/crehn/upload-template/apply', method: 'post', data });
};

export const listReportRulePackage = (query: any) => {
  return request({ url: '/crehn/report-rule/package/list', method: 'get', params: query });
};

export const saveReportRulePackage = (data: any) => {
  return request({ url: '/crehn/report-rule/package', method: 'post', data });
};

export const restoreDefaultReportRulePackage = () => {
  return request({ url: '/crehn/report-rule/package/restore-default', method: 'post' });
};

export const deleteReportRulePackage = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/report-rule/package/${ids}`, method: 'delete' });
};

export const listReportRuleItem = (query: any) => {
  return request({ url: '/crehn/report-rule/item/list', method: 'get', params: query });
};

export const listReportRuleItemOptions = (query?: any) => {
  return request({ url: '/crehn/report-rule/item/options', method: 'get', params: query });
};

export const saveReportRuleItem = (data: any) => {
  return request({ url: '/crehn/report-rule/item', method: 'post', data });
};

export const deleteReportRuleItem = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/report-rule/item/${ids}`, method: 'delete' });
};

export const listActivityReportRule = (query: any) => {
  return request({ url: '/crehn/report-rule/activity/list', method: 'get', params: query });
};

export const listActivityReportRuleOptions = (query?: any) => {
  return request({ url: '/crehn/report-rule/activity/options', method: 'get', params: query });
};

export const listActivityReportRuleSchoolOptions = (query: any) => {
  return request({ url: '/crehn/report-rule/activity/school-options', method: 'get', params: query });
};

export const saveActivityReportRule = (data: any) => {
  return request({ url: '/crehn/report-rule/activity', method: 'post', data });
};

export const deleteActivityReportRule = (ids: string | number | Array<string | number>) => {
  return request({ url: `/crehn/report-rule/activity/${ids}`, method: 'delete' });
};

export const applyReportRulePackage = (data: any) => {
  return request({ url: '/crehn/report-rule/activity/apply', method: 'post', data });
};

export const restoreDefaultActivityReportRule = (data: any) => {
  return request({ url: '/crehn/report-rule/activity/restore-default', method: 'post', data });
};
