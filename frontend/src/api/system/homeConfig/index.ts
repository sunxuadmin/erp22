import request from '@/utils/request';

export const getHomeSiteConfig = () => {
  return request({ url: '/system/home-config/site', method: 'get' });
};

export const saveHomeSiteConfig = (data: any) => {
  return request({ url: '/system/home-config/site', method: 'put', data });
};

export const listHomeModule = () => {
  return request({ url: '/system/home-config/module/list', method: 'get' });
};

export const saveHomeModule = (data: any) => {
  return request({ url: '/system/home-config/module', method: 'post', data });
};

export const sortHomeModule = (data: any[]) => {
  return request({ url: '/system/home-config/module/sort', method: 'put', data });
};

export const deleteHomeModule = (ids: string | number | Array<string | number>) => {
  return request({ url: `/system/home-config/module/${ids}`, method: 'delete' });
};

export const listHomeCard = (query?: any) => {
  return request({ url: '/system/home-config/card/list', method: 'get', params: query });
};

export const saveHomeCard = (data: any) => {
  return request({ url: '/system/home-config/card', method: 'post', data });
};

export const sortHomeCard = (data: any[]) => {
  return request({ url: '/system/home-config/card/sort', method: 'put', data });
};

export const deleteHomeCard = (ids: string | number | Array<string | number>) => {
  return request({ url: `/system/home-config/card/${ids}`, method: 'delete' });
};

export const listHomeStage = (query?: any) => {
  return request({ url: '/system/home-config/stage/list', method: 'get', params: query });
};

export const saveHomeStage = (data: any) => {
  return request({ url: '/system/home-config/stage', method: 'post', data });
};

export const deleteHomeStage = (ids: string | number | Array<string | number>) => {
  return request({ url: `/system/home-config/stage/${ids}`, method: 'delete' });
};
