import axios from 'axios';
import FileSaver from 'file-saver';
import errorCode from '@/utils/errorCode';
import { blobValidate } from '@/utils/ruoyi';
import { LoadingInstance } from 'element-plus/es/components/loading/src/loading';
import { globalHeaders } from '@/utils/request';

const baseURL = import.meta.env.VITE_APP_BASE_API;
let downloadLoadingInstance: LoadingInstance;
export default {
  async oss(ossId: string | number) {
    const url = baseURL + '/resource/oss/download/' + ossId;
    downloadLoadingInstance = ElLoading.service({ text: '正在下载数据，请稍候', background: 'rgba(0, 0, 0, 0.7)' });
    try {
      const res = await axios({
        method: 'get',
        url: url,
        responseType: 'blob',
        headers: globalHeaders()
      });
      const isBlob = blobValidate(res.data);
      if (isBlob) {
        const blob = new Blob([res.data], { type: 'application/octet-stream' });
        FileSaver.saveAs(blob, decodeURIComponent(res.headers['download-filename'] as string));
      } else {
        this.printErrMsg(res.data);
      }
      downloadLoadingInstance.close();
    } catch (r) {
      console.error(r);
      ElMessage.error('下载文件出现错误，请联系管理员！');
      downloadLoadingInstance.close();
    }
  },
  async noticeAttachment(ossId: string | number) {
    const url = baseURL + '/crehn/notice-template/attachment/' + ossId + '/download';
    downloadLoadingInstance = ElLoading.service({ text: '正在下载文件，请稍候', background: 'rgba(0, 0, 0, 0.7)' });
    try {
      const res = await axios({
        method: 'get',
        url: url,
        responseType: 'blob',
        headers: globalHeaders()
      });
      const isBlob = blobValidate(res.data);
      if (isBlob) {
        const blob = new Blob([res.data], { type: 'application/octet-stream' });
        const fileName = decodeURIComponent((res.headers['download-filename'] as string) || '公告附件');
        FileSaver.saveAs(blob, fileName);
      } else {
        this.printErrMsg(res.data);
      }
      downloadLoadingInstance.close();
    } catch (r) {
      console.error(r);
      ElMessage.error('下载文件出现错误，请联系管理员！');
      downloadLoadingInstance.close();
    }
  },
  async fileRequirementTemplate(categoryId: string | number, requirementId: string | number) {
    const url = baseURL + `/crehn/activity/school/category/${categoryId}/file-requirement/${requirementId}/template/download`;
    downloadLoadingInstance = ElLoading.service({ text: '正在下载模板文件，请稍候', background: 'rgba(0, 0, 0, 0.7)' });
    try {
      const res = await axios({
        method: 'get',
        url,
        responseType: 'blob',
        headers: globalHeaders()
      });
      const isBlob = blobValidate(res.data);
      if (isBlob) {
        const blob = new Blob([res.data], { type: 'application/octet-stream' });
        const fileName = decodeURIComponent((res.headers['download-filename'] as string) || '下载模板');
        FileSaver.saveAs(blob, fileName);
      } else {
        this.printErrMsg(res.data);
      }
      downloadLoadingInstance.close();
    } catch (r) {
      console.error(r);
      ElMessage.error('下载模板文件出现错误，请联系管理员！');
      downloadLoadingInstance.close();
    }
  },
  async zip(url: string, name: string) {
    url = baseURL + url;
    downloadLoadingInstance = ElLoading.service({ text: '正在下载数据，请稍候', background: 'rgba(0, 0, 0, 0.7)' });
    try {
      const res = await axios({
        method: 'get',
        url: url,
        responseType: 'blob',
        headers: globalHeaders()
      });
      const isBlob = blobValidate(res.data);
      if (isBlob) {
        const blob = new Blob([res.data], { type: 'application/zip' });
        FileSaver.saveAs(blob, name);
      } else {
        this.printErrMsg(res.data);
      }
      downloadLoadingInstance.close();
    } catch (r) {
      console.error(r);
      ElMessage.error('下载文件出现错误，请联系管理员！');
      downloadLoadingInstance.close();
    }
  },
  async printErrMsg(data: any) {
    const resText = await data.text();
    const rspObj = JSON.parse(resText);
    const errMsg = errorCode[rspObj.code] || rspObj.msg || errorCode['default'];
    ElMessage.error(errMsg);
  }
};
