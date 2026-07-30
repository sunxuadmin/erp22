import request from '@/utils/request';

export interface PortalSite {
  id?: string | number;
  siteCode?: string;
  siteName?: string;
  domainName?: string;
  themeJson?: string;
  seoJson?: string;
  filingNo?: string;
  status?: string;
}

export interface PortalChannel {
  id?: string | number;
  siteId?: string | number;
  parentId?: string | number;
  channelCode?: string;
  channelName?: string;
  navigationPosition?: string;
  sortOrder?: number;
  visibility?: string;
  status?: string;
}

export interface PortalArticle {
  id?: string | number;
  siteId?: string | number;
  channelId?: string | number;
  activityId?: string | number;
  articleCode?: string;
  title?: string;
  summary?: string;
  contentMarkdown?: string;
  coverMediaId?: string | number;
  authorName?: string;
  sourceName?: string;
  tags?: string;
  pinned?: boolean;
  visibility?: string;
  status?: string;
  scheduledAt?: string;
  publishedAt?: string;
  currentVersionId?: string | number;
  rowVersion?: number;
}

export interface PortalHomeComponent {
  id?: string | number;
  siteId?: string | number;
  pageCode?: string;
  componentKey?: string;
  componentType?: string;
  dataSourceCode?: string;
  configJson?: string;
  gridX?: number;
  gridY?: number;
  gridW?: number;
  gridH?: number;
  sortOrder?: number;
  enabled?: boolean;
}

export interface PortalMediaAsset {
  id?: string | number;
  siteId?: string | number;
  ossId?: string | number;
  mediaType?: string;
  title?: string;
  altText?: string;
  copyrightOwner?: string;
  sourceName?: string;
  contentSha256?: string;
  scanStatus?: string;
  status?: string;
}

export const listPortalSites = () => request<PortalSite[]>({ url: '/crehn/cms/site/list', method: 'get' });
export const savePortalSite = (data: PortalSite) => request<PortalSite>({ url: '/crehn/cms/site', method: 'put', data });
export const listPortalChannels = (siteId: string | number) =>
  request<PortalChannel[]>({ url: '/crehn/cms/channel/list', method: 'get', params: { siteId } });
export const savePortalChannel = (data: PortalChannel) => request<PortalChannel>({ url: '/crehn/cms/channel', method: 'put', data });
export const listPortalMedia = (siteId: string | number) =>
  request<PortalMediaAsset[]>({ url: '/crehn/cms/media/list', method: 'get', params: { siteId } });
export const savePortalMedia = (data: PortalMediaAsset) =>
  request<PortalMediaAsset>({ url: '/crehn/cms/media', method: 'put', data });
export const approvePortalMedia = (mediaId: string | number, reason: string) =>
  request({ url: `/crehn/cms/media/${mediaId}/approve`, method: 'post', params: { reason } });
export const listPortalArticles = (params: Record<string, unknown>) =>
  request({ url: '/crehn/cms/article/list', method: 'get', params });
export const savePortalArticle = (data: PortalArticle) =>
  request<PortalArticle>({ url: '/crehn/cms/article/draft', method: 'put', data });
export const submitPortalArticleReview = (articleId: string | number) =>
  request({ url: `/crehn/cms/article/${articleId}/review`, method: 'post' });
export const publishPortalArticle = (data: {
  articleId: string | number;
  scheduledAt?: string;
  reason: string;
}) => request<PortalArticle>({ url: '/crehn/cms/article/publish', method: 'post', data });
export const offlinePortalArticle = (data: { articleId: string | number; reason: string }) =>
  request({ url: '/crehn/cms/article/offline', method: 'post', data });
export const listPortalHomeComponents = (siteId: string | number) =>
  request<PortalHomeComponent[]>({ url: '/crehn/cms/home/list', method: 'get', params: { siteId } });
export const savePortalHomeComponent = (data: PortalHomeComponent) =>
  request<PortalHomeComponent>({ url: '/crehn/cms/home', method: 'put', data });
export const publishPortalHome = (siteId: string | number, reason: string) =>
  request({ url: `/crehn/cms/home/${siteId}/publish`, method: 'post', params: { reason } });
