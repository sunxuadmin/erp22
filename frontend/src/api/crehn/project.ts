import request from '@/utils/request';
import { AxiosProgressEvent, AxiosPromise } from 'axios';
import {
  GenericTableImportResultVO,
  MemberAttachmentBatchResultVO,
  ProjectCategoryStatsVO,
  ProjectFileDirectUploadInitVO,
  ProjectFileVO,
  ProjectQuotaOverviewVO,
  ProjectSaveForm,
  SchoolDashboardVO,
  ProjectVO
} from './types';

export const listMyProject = (query: any): AxiosPromise<ProjectVO[]> => {
  return request({ url: '/crehn/project/my-list', method: 'get', params: query });
};

export const listMyProjectCategoryStats = (query: any): AxiosPromise<ProjectCategoryStatsVO[]> => {
  return request({ url: '/crehn/project/my-category-stats', method: 'get', params: query });
};

export const getMyProjectQuotaOverview = (query: any): AxiosPromise<ProjectQuotaOverviewVO> => {
  return request({ url: '/crehn/project/my-quota-overview', method: 'get', params: query });
};

export const getMyProjectDashboard = (query?: { activityId?: string | number }): AxiosPromise<SchoolDashboardVO> => {
  return request({ url: '/crehn/project/my-dashboard', method: 'get', params: query });
};

export const getProject = (id: string | number): AxiosPromise<ProjectVO> => {
  return request({ url: `/crehn/project/${id}`, method: 'get' });
};

export const saveProjectDraft = (data: ProjectSaveForm): AxiosPromise<ProjectVO> => {
  return request({ url: '/crehn/project/draft', method: 'post', data });
};

export const submitProject = (id: string | number) => {
  return request({ url: `/crehn/project/${id}/submit`, method: 'post' });
};

export const withdrawSubmitProject = (id: string | number) => {
  return request({ url: `/crehn/project/${id}/withdraw-submit`, method: 'post' });
};

export const recycleProject = (projectId: string | number, reason?: string) => {
  return request({ url: '/crehn/project/recycle', method: 'post', data: { projectId, reason } });
};

export const uploadProjectFile = (
  projectId: string | number,
  requirementId: string | number,
  file: File,
  onUploadProgress?: (event: AxiosProgressEvent) => void
): AxiosPromise<ProjectFileVO> => {
  const data = new FormData();
  data.append('file', file);
  return request({ url: `/crehn/project/${projectId}/file/${requirementId}`, method: 'post', data, onUploadProgress, timeout: 30 * 60 * 1000 });
};

export const initProjectFileDirectUpload = (
  projectId: string | number,
  requirementId: string | number,
  data: { originalName: string; fileSize: number; contentType?: string }
): AxiosPromise<ProjectFileDirectUploadInitVO> => {
  return request({ url: `/crehn/project/${projectId}/file/${requirementId}/direct-upload/init`, method: 'post', data });
};

export const uploadProjectFileToOss = (
  uploadUrl: string,
  file: File,
  contentType?: string,
  onUploadProgress?: (event: AxiosProgressEvent) => void,
  signal?: AbortSignal
) => {
  return new Promise<void>((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    let settled = false;
    const createCanceledError = () => {
      const error = new Error('上传已取消');
      error.name = 'CanceledError';
      (error as any).code = 'ERR_CANCELED';
      (error as any).__CANCEL__ = true;
      (error as any).config = { url: uploadUrl };
      return error;
    };
    function abortUpload() {
      xhr.abort();
      rejectOnce(createCanceledError());
    }
    const cleanup = () => {
      signal?.removeEventListener('abort', abortUpload);
    };
    const resolveOnce = () => {
      if (settled) return;
      settled = true;
      cleanup();
      resolve();
    };
    const rejectOnce = (error: any) => {
      if (settled) return;
      settled = true;
      cleanup();
      reject(error);
    };
    if (signal?.aborted) {
      rejectOnce(createCanceledError());
      return;
    }
    signal?.addEventListener('abort', abortUpload, { once: true });
    xhr.open('PUT', uploadUrl);
    xhr.timeout = 30 * 60 * 1000;
    xhr.setRequestHeader('Content-Type', contentType || file.type || 'application/octet-stream');
    xhr.upload.onprogress = (event) => {
      onUploadProgress?.({
        loaded: event.loaded,
        total: event.lengthComputable ? event.total : file.size
      } as AxiosProgressEvent);
    };
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolveOnce();
      } else {
        const error = new Error(`OSS direct upload failed with status ${xhr.status}`);
        (error as any).response = { status: xhr.status, data: xhr.responseText };
        (error as any).config = { url: uploadUrl };
        rejectOnce(error);
      }
    };
    xhr.onerror = () => {
      const error = new Error('Network Error');
      (error as any).config = { url: uploadUrl };
      rejectOnce(error);
    };
    xhr.ontimeout = () => {
      const error = new Error('timeout of 1800000ms exceeded');
      (error as any).code = 'ECONNABORTED';
      (error as any).config = { url: uploadUrl };
      rejectOnce(error);
    };
    xhr.onabort = () => {
      rejectOnce(createCanceledError());
    };
    xhr.send(file);
  });
};

export const completeProjectFileDirectUpload = (
  projectId: string | number,
  requirementId: string | number,
  data: { uploadToken: string; objectKey: string; originalName: string; fileSize: number; contentType?: string }
): AxiosPromise<ProjectFileVO> => {
  return request({
    url: `/crehn/project/${projectId}/file/${requirementId}/direct-upload/complete`,
    method: 'post',
    data,
    timeout: 30 * 60 * 1000
  });
};

export const downloadProjectMemberTemplate = (params?: { projectId?: string | number; categoryId?: string | number }) => {
  return request({ url: '/crehn/project/member/template', method: 'get', params, responseType: 'blob' });
};

export const importProjectMembers = (projectId: string | number, file: File, replace = false): AxiosPromise<ProjectVO> => {
  const data = new FormData();
  data.append('file', file);
  data.append('replace', String(replace));
  return request({ url: `/crehn/project/${projectId}/member/import`, method: 'post', data });
};

export const downloadProjectGenericTableTemplate = (params: { tableKey: string; projectId?: string | number; categoryId?: string | number }) => {
  return request({ url: '/crehn/project/table/template', method: 'get', params, responseType: 'blob' });
};

export const importProjectGenericTable = (projectId: string | number, tableKey: string, file: File): AxiosPromise<GenericTableImportResultVO> => {
  const data = new FormData();
  data.append('file', file);
  return request({ url: `/crehn/project/${projectId}/table/${encodeURIComponent(tableKey)}/import`, method: 'post', data });
};

export const uploadProjectMemberPhoto = (projectId: string | number, memberId: string | number, file: File): AxiosPromise<ProjectVO> => {
  const data = new FormData();
  data.append('file', file);
  return request({ url: `/crehn/project/${projectId}/member/${memberId}/photo`, method: 'post', data });
};

export const uploadProjectMemberStudentReport = (projectId: string | number, memberId: string | number, file: File): AxiosPromise<ProjectVO> => {
  const data = new FormData();
  data.append('file', file);
  return request({ url: `/crehn/project/${projectId}/member/${memberId}/student-report`, method: 'post', data });
};

export const uploadProjectMemberAttachment = (
  projectId: string | number,
  memberId: string | number,
  fieldKey: string,
  file: File
): AxiosPromise<ProjectVO> => {
  const data = new FormData();
  data.append('file', file);
  return request({ url: `/crehn/project/${projectId}/member/${memberId}/attachment/${encodeURIComponent(fieldKey)}`, method: 'post', data });
};

export const batchUploadProjectMemberAttachment = (
  projectId: string | number,
  fieldKey: string,
  files: File[]
): AxiosPromise<MemberAttachmentBatchResultVO> => {
  const data = new FormData();
  files.forEach((file) => data.append('files', file));
  return request({ url: `/crehn/project/${projectId}/member/attachment/${encodeURIComponent(fieldKey)}/batch`, method: 'post', data, timeout: 30 * 60 * 1000 });
};

export const requestProjectFilePreview = (projectId: string | number, fileId: string | number): AxiosPromise<ProjectFileVO> => {
  return request({ url: `/crehn/project/${projectId}/file/${fileId}/preview`, method: 'post' });
};

export const deleteProjectFile = (projectId: string | number, fileId: string | number) => {
  return request({ url: `/crehn/project/${projectId}/file/${fileId}`, method: 'delete' });
};

export const listRecycleProjectFile = (query: any): AxiosPromise<ProjectFileVO[]> => {
  return request({ url: '/crehn/project/recycle/file/list', method: 'get', params: query });
};

export const listRecycleProject = (query: any): AxiosPromise<ProjectVO[]> => {
  return request({ url: '/crehn/project/recycle/project/list', method: 'get', params: query });
};

export const restoreRecycleProject = (projectId: string | number) => {
  return request({ url: `/crehn/project/recycle/project/${projectId}/restore`, method: 'put' });
};

export const purgeRecycleProject = (projectIds: string | number | Array<string | number>) => {
  return request({ url: `/crehn/project/recycle/project/${projectIds}`, method: 'delete' });
};

export const restoreRecycleProjectFile = (fileId: string | number) => {
  return request({ url: `/crehn/project/recycle/file/${fileId}/restore`, method: 'put' });
};

export const downloadRecycleProjectFile = (fileId: string | number) => {
  return request({ url: `/crehn/project/recycle/file/${fileId}/download`, method: 'get', responseType: 'blob' });
};

export const purgeRecycleProjectFile = (fileIds: string | number | Array<string | number>) => {
  return request({ url: `/crehn/project/recycle/file/${fileIds}`, method: 'delete' });
};
