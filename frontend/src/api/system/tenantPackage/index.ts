import request from '@/utils/request';
import { TenantPkgForm, TenantPkgQuery, TenantPkgVO } from './types';
import { AxiosPromise } from 'axios';

// 查询用户套餐列表
export function listTenantPackage(query?: TenantPkgQuery): AxiosPromise<TenantPkgVO[]> {
  return request({
    url: '/system/tenant/package/list',
    method: 'get',
    params: query
  });
}

// 查询用户套餐下拉选列表
export function selectTenantPackage(): AxiosPromise<TenantPkgVO[]> {
  return request({
    url: '/system/tenant/package/selectList',
    method: 'get'
  });
}

// 查询用户套餐详细
export function getTenantPackage(packageId: string | number): AxiosPromise<TenantPkgVO> {
  return request({
    url: '/system/tenant/package/' + packageId,
    method: 'get'
  });
}

// 新增用户套餐
export function addTenantPackage(data: TenantPkgForm) {
  return request({
    url: '/system/tenant/package',
    method: 'post',
    data: data
  });
}

// 修改用户套餐
export function updateTenantPackage(data: TenantPkgForm) {
  return request({
    url: '/system/tenant/package',
    method: 'put',
    data: data
  });
}

// 用户套餐状态修改
export function changePackageStatus(packageId: number | string, status: string) {
  const data = {
    packageId,
    status
  };
  return request({
    url: '/system/tenant/package/changeStatus',
    method: 'put',
    data: data
  });
}

// 删除用户套餐
export function delTenantPackage(packageId: string | number | Array<string | number>) {
  return request({
    url: '/system/tenant/package/' + packageId,
    method: 'delete'
  });
}
