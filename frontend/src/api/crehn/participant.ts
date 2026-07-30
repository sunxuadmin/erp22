import request from '@/utils/request';

export interface ParticipantProfile {
  id?: string | number;
  userId?: string | number;
  activityId?: string | number;
  schoolId?: string | number;
  participantName?: string;
  identityType?: string;
  identityNoMasked?: string;
  phonenumber?: string;
  email?: string;
  status?: string;
  confirmedAt?: string;
  createTime?: string;
}

export interface ParticipantActivationIssue {
  userName?: string;
  participantName?: string;
  phonenumber?: string;
  activationCode?: string;
  expiresAt?: string;
}

export const listParticipant = (params: Record<string, unknown>) =>
  request({ url: '/crehn/participant-account/list', method: 'get', params });

export const importParticipant = (data: FormData) =>
  request<ParticipantActivationIssue[]>({
    url: '/crehn/participant-account/import',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 10 * 60 * 1000
  });

export const downloadParticipantTemplate = () =>
  request({
    url: '/crehn/participant-account/template',
    method: 'post',
    responseType: 'blob'
  });

export const reissueParticipantCode = (data: {
  participantId: string | number;
  expiresAt: string;
  reason: string;
}) => request<ParticipantActivationIssue>({ url: '/crehn/participant-account/reissue', method: 'post', data });

export const activateParticipant = (data: {
  activationCode: string;
  password: string;
  profileConfirmation: 'confirmed';
}) => request({ url: '/crehn/participant-account/activate', method: 'post', data });
