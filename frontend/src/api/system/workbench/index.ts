import request from '@/utils/request';
import { AxiosPromise } from 'axios';
import {
  SecurityReminderConfig,
  WorkbenchAssetUploadVO,
  WorkbenchConfigImportPreviewVO,
  WorkbenchComponentVO,
  WorkbenchLayoutVO,
  WorkbenchRoleShellConfig,
  WorkbenchStyleSaveResult,
  WorkbenchRoleOptionVO,
  WorkbenchStyleConfig
} from './types';

export const WORKBENCH_STYLE_SAVE_API_VERSION = 2;

export const listWorkbenchComponents = (roleId?: string | number): AxiosPromise<WorkbenchComponentVO[]> => {
  return request({ url: '/system/workbench/components', method: 'get', params: { roleId } });
};

export const listWorkbenchRoleOptions = (): AxiosPromise<WorkbenchRoleOptionVO[]> => {
  return request({ url: '/system/workbench/role-options', method: 'get' });
};

export const listCurrentWorkbench = (): AxiosPromise<WorkbenchLayoutVO[]> => {
  return request({ url: '/system/workbench/current', method: 'get' });
};

export const getCurrentWorkbenchComponent = (componentKey: string): AxiosPromise<WorkbenchLayoutVO | null> => {
  return request({ url: `/system/workbench/current/component/${componentKey}`, method: 'get' });
};

export const listRoleWorkbench = (roleId: string | number): AxiosPromise<WorkbenchLayoutVO[]> => {
  return request({ url: `/system/workbench/role/${roleId}`, method: 'get' });
};

export const getCurrentRoleShellConfig = (): AxiosPromise<WorkbenchRoleShellConfig> => {
  return request({ url: '/system/workbench/role-shell/current', method: 'get' });
};

export const getRoleShellConfig = (roleId: string | number): AxiosPromise<WorkbenchRoleShellConfig> => {
  return request({ url: `/system/workbench/role/${roleId}/shell`, method: 'get' });
};

export const saveRoleShellConfig = (roleId: string | number, data: WorkbenchRoleShellConfig) => {
  return request({ url: `/system/workbench/role/${roleId}/shell`, method: 'put', data });
};

export const restoreRoleShellConfig = (roleId: string | number) => {
  return request({ url: `/system/workbench/role/${roleId}/shell/restore-default`, method: 'put' });
};

export const getNavbarTitleConfig = (): AxiosPromise<string> => {
  return request({ url: '/system/workbench/navbar-title', method: 'get' });
};

export const saveNavbarTitleConfig = (data: { visible: boolean; title: string }) => {
  return request({ url: '/system/workbench/navbar-title', method: 'put', data });
};

export const getWorkbenchStyleConfig = (forceFresh = false): AxiosPromise<string> => {
  return request({
    url: '/system/workbench/style',
    method: 'get',
    params: forceFresh ? { _ts: Date.now() } : undefined,
    headers: forceFresh
      ? {
          'Cache-Control': 'no-cache',
          Pragma: 'no-cache'
        }
      : undefined
  });
};

export const saveWorkbenchStyleConfig = (data: WorkbenchStyleConfig): AxiosPromise<WorkbenchStyleSaveResult> => {
  return request({ url: '/system/workbench/style', method: 'put', data });
};

export const getSecurityReminderConfig = (): AxiosPromise<string> => {
  return request({ url: '/system/workbench/security-reminder', method: 'get' });
};

export const saveSecurityReminderConfig = (data: SecurityReminderConfig) => {
  return request({ url: '/system/workbench/security-reminder', method: 'put', data });
};

export const uploadWorkbenchAsset = (file: File): AxiosPromise<WorkbenchAssetUploadVO> => {
  const data = new FormData();
  data.append('file', file);
  return request({ url: '/system/workbench/asset/upload', method: 'post', data });
};

export const deleteWorkbenchAsset = (key: string) => {
  return request({ url: '/system/workbench/asset/delete', method: 'delete', params: { key } });
};

export const exportWorkbenchConfigPackage = (): AxiosPromise<Blob> => {
  return request({ url: '/system/workbench/config-package/export', method: 'get', responseType: 'blob' });
};

export const previewWorkbenchConfigPackage = (file: File): AxiosPromise<WorkbenchConfigImportPreviewVO> => {
  const data = new FormData();
  data.append('file', file);
  return request({ url: '/system/workbench/config-package/preview', method: 'post', data });
};

export const importWorkbenchConfigPackage = (file: File) => {
  const data = new FormData();
  data.append('file', file);
  return request({ url: '/system/workbench/config-package/import', method: 'post', data });
};

export const workbenchAssetUrl = (key?: string) => {
  if (!key) return '';
  return `${import.meta.env.VITE_APP_BASE_API}/system/workbench/asset?key=${encodeURIComponent(key)}`;
};

export const saveRoleWorkbench = (roleId: string | number, data: WorkbenchLayoutVO[]) => {
  return request({ url: `/system/workbench/role/${roleId}`, method: 'put', data });
};

export const restoreRoleWorkbenchComponentDefault = (roleId: string | number, componentKey: string) => {
  return request({ url: `/system/workbench/role/${roleId}/component/${componentKey}/restore-default`, method: 'put' });
};
