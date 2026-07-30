import request from '@/utils/request';

export const listSchoolAccount = (query: any) => {
  return request({ url: '/crehn/school-account/list', method: 'get', params: query });
};

export const listAvailableSchoolAccount = (query: any) => {
  return request({ url: '/crehn/school-account/available', method: 'get', params: query });
};

export const getSchoolAccount = (userId: string | number) => {
  return request({ url: `/crehn/school-account/${userId}`, method: 'get' });
};

export const createArtAccount = (data: any) => {
  return request({ url: '/crehn/school-account', method: 'post', data });
};

export const reviewSchoolAccount = (userId: string | number, schoolReviewStatus: string, opinion?: string) => {
  return request({ url: '/crehn/school-account/review', method: 'put', data: { userId, schoolReviewStatus, opinion } });
};

export const updateSchoolAccount = (data: any) => {
  return request({ url: '/crehn/school-account', method: 'put', data });
};

export const bindSchoolAccount = (userId: string | number, schoolId: string | number) => {
  return request({ url: `/crehn/school-account/${userId}/bind/${schoolId}`, method: 'put' });
};

export const unbindSchoolAccount = (userId: string | number) => {
  return request({ url: `/crehn/school-account/${userId}/unbind`, method: 'put' });
};

export const resetSchoolAccountPwd = (userId: string | number, password: string) => {
  return request({
    url: '/crehn/school-account/resetPwd',
    method: 'put',
    headers: {
      isEncrypt: true,
      repeatSubmit: false
    },
    data: { userId, password }
  });
};

export const deleteSchoolAccount = (userIds: string | number | Array<string | number>) => {
  return request({ url: `/crehn/school-account/${userIds}`, method: 'delete' });
};

export const restoreSchoolAccount = (userIds: string | number | Array<string | number>) => {
  return request({ url: `/crehn/school-account/restore/${userIds}`, method: 'put' });
};

export const importSchoolAccountUrl = () => {
  return import.meta.env.VITE_APP_BASE_API + '/crehn/school-account/importData';
};
