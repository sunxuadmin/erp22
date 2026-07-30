// ART-OWNER: FE.PROJECT_EDIT.FILE_PREVIEW
import { Document, Picture, VideoPlay } from '@element-plus/icons-vue';
import { getProject, requestProjectFilePreview } from '@/api/crehn/project';
import type { ProjectFileVO } from '@/api/crehn/types';
import { normalizePreviewMessage as normalizeArtPreviewMessage } from '@/utils/artReviewMessage';
import { computed, ref } from 'vue';
import type { Ref } from 'vue';

type ProjectFilePreviewOptions = {
  projectId: () => string | number | undefined;
  fileList: Ref<ProjectFileVO[]>;
  selectedFileId: Ref<string | number | undefined>;
  selectFile: (file: ProjectFileVO) => void;
  refreshProjectFiles: () => Promise<void>;
  notifySuccess: (message: string) => void;
  notifyWarning: (message: string) => void;
};

export function useProjectFilePreview(options: ProjectFilePreviewOptions) {
  const materialPreviewFileId = ref<string | number>();
  const materialPreviewVisible = ref(false);
  const materialPreviewLoading = ref(false);
  let materialPreviewRequestId = 0;

  const materialPreviewFile = computed(() => options.fileList.value.find((item) => String(item.id) === String(materialPreviewFileId.value)));

  const fileExt = (file?: ProjectFileVO) => {
    const ext = file?.fileExt || file?.originalName?.split('.').pop() || '';
    return ext.toLowerCase().replace(/^\./, '');
  };

  const isVideoFile = (file?: ProjectFileVO) => {
    const ext = fileExt(file);
    return file?.mediaType === 'video' || ['mp4', 'mov', 'mpg', 'mpeg'].includes(ext);
  };

  const isImageFile = (file?: ProjectFileVO) => {
    const ext = fileExt(file);
    return file?.mediaType === 'image' || ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext);
  };

  const isOfficeFile = (file?: ProjectFileVO) => {
    return ['doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx'].includes(fileExt(file));
  };

  const isPendingOfficePreview = (file?: ProjectFileVO) => {
    return !!file && isOfficeFile(file) && ['queued', 'converting', 'processing'].includes(file.previewStatus || '');
  };

  const filePreviewUrl = (file?: ProjectFileVO) => {
    if (!file) return '';
    if (file.previewPath) return file.previewPath;
    const ext = fileExt(file);
    if (isVideoFile(file) || isImageFile(file) || ext === 'pdf') return file.storagePath || '';
    return '';
  };

  const absoluteFileUrl = (url?: string) => {
    if (!url) return '';
    if (/^(https?:)?\/\//i.test(url) || /^(blob|data):/i.test(url)) return url;
    if (url.startsWith('/')) return `${window.location.origin}${url}`;
    return url;
  };

  const previewOpenUrl = (file?: ProjectFileVO) => absoluteFileUrl(filePreviewUrl(file));

  const canRequestPreview = (file?: ProjectFileVO) => {
    if (!file || !options.projectId() || !file.id || !isOfficeFile(file)) return false;
    if (previewOpenUrl(file)) return false;
    return !['queued', 'converting', 'processing'].includes(file.previewStatus || '');
  };

  const previewActionLabel = (file?: ProjectFileVO) => (file?.previewStatus === 'failed' ? '重试预览' : '生成预览');

  const resourceFileIcon = (file?: ProjectFileVO) => {
    if (isVideoFile(file)) return VideoPlay;
    if (isImageFile(file)) return Picture;
    return Document;
  };

  const resourceFileIconClass = (file?: ProjectFileVO) => {
    if (isVideoFile(file)) return 'is-video';
    if (isImageFile(file)) return 'is-image';
    return 'is-document';
  };

  const canInlinePreview = (file?: ProjectFileVO) => {
    if (!filePreviewUrl(file)) return false;
    const ext = fileExt(file);
    const previewExt = file?.previewExt?.toLowerCase();
    return ext === 'pdf' || previewExt === 'pdf' || file?.previewStatus === 'converted';
  };

  const pdfViewerUrl = (file?: ProjectFileVO) => {
    const url = previewOpenUrl(file);
    if (!url) return '';
    const base = url.split('#')[0];
    return `${base}#toolbar=0&navpanes=0&scrollbar=1`;
  };

  const refreshFileAccessUrl = async (file?: ProjectFileVO) => {
    const projectId = options.projectId();
    if (!projectId || !file?.id) return file;
    try {
      const { data } = await getProject(projectId);
      options.fileList.value = data.files || [];
      const freshFile = options.fileList.value.find((item) => String(item.id) === String(file.id));
      if (freshFile) {
        options.selectedFileId.value = freshFile.id;
        return freshFile;
      }
    } catch {
      // Keep the current file object as a fallback if refreshing fails.
    }
    return file;
  };

  const openMaterialPreview = async (file?: ProjectFileVO) => {
    if (!file?.id) return;
    const requestId = ++materialPreviewRequestId;
    options.selectFile(file);
    materialPreviewFileId.value = file.id;
    materialPreviewVisible.value = true;
    materialPreviewLoading.value = true;
    try {
      const freshFile = await refreshFileAccessUrl(file);
      if (requestId === materialPreviewRequestId && freshFile?.id) {
        materialPreviewFileId.value = freshFile.id;
      }
    } finally {
      if (requestId === materialPreviewRequestId) {
        materialPreviewLoading.value = false;
      }
    }
  };

  const handleMaterialPreviewClosed = () => {
    materialPreviewRequestId += 1;
    materialPreviewLoading.value = false;
    materialPreviewFileId.value = undefined;
  };

  const openUrl = (url?: string, message = '暂无可打开的文件地址') => {
    const target = absoluteFileUrl(url);
    if (!target) {
      options.notifyWarning(message);
      return;
    }
    const opened = window.open(target, '_blank', 'noopener,noreferrer');
    if (!opened) {
      options.notifyWarning('浏览器拦截了新窗口，请允许弹窗后重试');
    }
  };

  const generatePreview = async (file?: ProjectFileVO) => {
    const projectId = options.projectId();
    if (!projectId || !file?.id) return;
    const retry = file.previewStatus === 'failed';
    const res = await requestProjectFilePreview(projectId, file.id);
    const index = options.fileList.value.findIndex((item) => String(item.id) === String(file.id));
    if (index >= 0) {
      options.fileList.value[index] = { ...options.fileList.value[index], ...res.data };
    }
    options.selectedFileId.value = file.id;
    options.notifySuccess(retry ? '已重新提交预览转换，请稍后刷新查看' : '已提交预览转换，请稍后刷新查看');
  };

  const refreshPreviewStatus = async (file?: ProjectFileVO) => {
    if (!file?.id) return;
    await options.refreshProjectFiles();
    options.notifySuccess('预览状态已刷新');
  };

  const openPreview = async (file?: ProjectFileVO) => {
    const freshFile = await refreshFileAccessUrl(file);
    if (!previewOpenUrl(freshFile) && canRequestPreview(freshFile)) {
      await generatePreview(freshFile);
      return;
    }
    openUrl(previewOpenUrl(freshFile), '当前文件还没有可预览地址');
  };

  const openOriginalFile = async (file?: ProjectFileVO) => {
    const freshFile = await refreshFileAccessUrl(file);
    openUrl(freshFile?.storagePath, '当前文件还没有原文件地址');
  };

  const printPdfPreview = async (file?: ProjectFileVO) => {
    const freshFile = await refreshFileAccessUrl(file);
    const url = previewOpenUrl(freshFile);
    if (!url) {
      options.notifyWarning('当前文件还没有 PDF 预览地址');
      return;
    }
    const frame = document.createElement('iframe');
    frame.style.position = 'fixed';
    frame.style.right = '0';
    frame.style.bottom = '0';
    frame.style.width = '0';
    frame.style.height = '0';
    frame.style.border = '0';
    frame.src = pdfViewerUrl(freshFile);
    frame.onload = () => {
      try {
        frame.contentWindow?.focus();
        frame.contentWindow?.print();
      } catch {
        openUrl(url, '无法直接打印，请在新窗口中打印');
      } finally {
        setTimeout(() => frame.remove(), 3000);
      }
    };
    document.body.appendChild(frame);
  };

  const downloadPdfPreview = async (file?: ProjectFileVO) => {
    const freshFile = await refreshFileAccessUrl(file);
    const url = previewOpenUrl(freshFile);
    if (!url) {
      options.notifyWarning('当前文件还没有 PDF 预览地址');
      return;
    }
    const link = document.createElement('a');
    link.href = url;
    link.download = `${freshFile?.originalName || 'preview'}.pdf`;
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    link.click();
  };

  const normalizePreviewMessage = (message?: string, status?: string) => normalizeArtPreviewMessage(message, status);

  const previewFallbackText = (file: ProjectFileVO) => {
    if (file.previewStatus === 'queued' || file.previewStatus === 'converting' || file.previewStatus === 'processing') return '预览生成中，请稍后。';
    if (file.previewStatus === 'failed')
      return normalizePreviewMessage(file.previewMessage, file.previewStatus) || '预览转换失败，可下载原文件查看。';
    if (isOfficeFile(file)) return '预览生成中，请稍后。也可下载原文件查看。';
    return '该格式暂不支持直接预览，可下载原文件查看。';
  };

  const checkStatusLabel = (status?: string) => {
    const labels: Record<string, string> = { passed: '通过', warning: '需人工关注', failed: '不通过' };
    return labels[status || ''] || status || '未校验';
  };

  const formatFileSize = (value?: number) => {
    if (!value) return '';
    const units = ['B', 'KB', 'MB', 'GB'];
    let size = Number(value);
    let index = 0;
    while (size >= 1024 && index < units.length - 1) {
      size /= 1024;
      index += 1;
    }
    return `${size.toFixed(index === 0 ? 0 : 2)}${units[index]}`;
  };

  const formatBitrate = (value?: number) => {
    if (!value) return '';
    const bitrate = Number(value);
    if (bitrate >= 1000000) return `${(bitrate / 1000000).toFixed(2)}Mbps`;
    return `${bitrate}bps`;
  };

  return {
    canInlinePreview,
    canRequestPreview,
    checkStatusLabel,
    downloadPdfPreview,
    filePreviewUrl,
    formatBitrate,
    formatFileSize,
    generatePreview,
    handleMaterialPreviewClosed,
    isImageFile,
    isPendingOfficePreview,
    isVideoFile,
    materialPreviewFile,
    materialPreviewFileId,
    materialPreviewLoading,
    materialPreviewVisible,
    normalizePreviewMessage,
    openMaterialPreview,
    openOriginalFile,
    openPreview,
    pdfViewerUrl,
    previewActionLabel,
    previewFallbackText,
    previewOpenUrl,
    printPdfPreview,
    refreshPreviewStatus,
    resourceFileIcon,
    resourceFileIconClass
  };
}
