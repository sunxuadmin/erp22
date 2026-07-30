import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import { UserMessageVO } from './types';

export const listMyMessage = (query: any): AxiosPromise<UserMessageVO[]> => {
  return request({ url: '/crehn/message/my-list', method: 'get', params: query });
};

export const getUnreadMessageCount = (): AxiosPromise<number> => {
  return request({ url: '/crehn/message/unread-count', method: 'get' });
};

export const markMessageRead = (id: string | number) => {
  return request({ url: `/crehn/message/${id}/read`, method: 'put' });
};

export const markAllMessageRead = () => {
  return request({ url: '/crehn/message/read-all', method: 'put' });
};
