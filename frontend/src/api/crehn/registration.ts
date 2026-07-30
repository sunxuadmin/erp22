import request from '@/utils/request';

export const listRegistrationCode = (query: any) => {
  return request({ url: '/crehn/registration-code/list', method: 'get', params: query });
};

export const getRegistrationCode = (id: string | number) => {
  return request({ url: `/crehn/registration-code/${id}`, method: 'get' });
};

export const addRegistrationCode = (data: any) => {
  return request({ url: '/crehn/registration-code', method: 'post', data });
};

export const batchAddRegistrationCode = (data: any) => {
  return request({ url: '/crehn/registration-code/batch', method: 'post', data });
};

export const updateRegistrationCode = (data: any) => {
  return request({ url: '/crehn/registration-code', method: 'put', data });
};

export const voidRegistrationCode = (id: string | number) => {
  return request({ url: `/crehn/registration-code/${id}/void`, method: 'put' });
};

export const resendRegistrationCode = (id: string | number) => {
  return request({ url: `/crehn/registration-code/${id}/resend`, method: 'put' });
};

export const listRegistrationUsage = (id: string | number) => {
  return request({ url: `/crehn/registration-code/${id}/usage`, method: 'get' });
};
