<template>
  <div class="p-2 recycle-page">
    <el-card shadow="hover">
      <template #header>
        <div class="recycle-header">
          <span>作品/附件回收站</span>
          <el-alert
            class="recycle-tip"
            type="warning"
            :closable="false"
            show-icon
            title="回收站用于防止误删。永久清理只移除系统记录；对象存储原文件仍按存储策略和后端清理任务处理。已评分或已生成结果的作品禁止永久清理。"
          />
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="项目回收站" name="project">
          <el-form :model="projectQuery" :inline="true" class="mb-2">
            <el-form-item label="项目名称">
              <el-input v-model="projectQuery.projectName" clearable placeholder="请输入项目名称" @keyup.enter="loadProjects" />
            </el-form-item>
            <el-form-item label="活动ID">
              <el-input v-model="projectQuery.activityId" clearable style="width: 120px" />
            </el-form-item>
            <el-form-item label="类别ID">
              <el-input v-model="projectQuery.categoryId" clearable style="width: 120px" />
            </el-form-item>
            <el-form-item label="项目ID">
              <el-input v-model="projectQuery.projectId" clearable style="width: 140px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="loadProjects">查询</el-button>
              <el-button icon="Refresh" @click="resetProjectQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="projectLoading" border :data="projectRows">
            <el-table-column type="index" label="#" width="60" />
            <el-table-column label="项目编号" prop="projectNo" width="150" />
            <el-table-column label="项目名称" prop="projectName" min-width="190" show-overflow-tooltip />
            <el-table-column label="活动" prop="activityName" min-width="180" show-overflow-tooltip />
            <el-table-column label="类别" prop="categoryName" min-width="140" show-overflow-tooltip />
            <el-table-column label="删除前状态" width="120">
              <template #default="{ row }">{{ statusLabel(row.recycledFromStatus) }}</template>
            </el-table-column>
            <el-table-column label="删除账号" min-width="130" show-overflow-tooltip>
              <template #default="{ row }">{{ row.recycledByName || row.recycledBy || '-' }}</template>
            </el-table-column>
            <el-table-column label="删除时间" prop="recycledAt" width="170" />
            <el-table-column label="删除原因" prop="recycleReason" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="180" fixed="right" align="center">
              <template #default="{ row }">
                <el-button v-hasPermi="['crehn:recycle:restore']" link type="success" icon="RefreshLeft" @click="restoreProject(row)">恢复</el-button>
                <el-button v-hasPermi="['crehn:recycle:remove']" link type="danger" icon="Delete" @click="purgeProject(row)">永久清理</el-button>
              </template>
            </el-table-column>
          </el-table>
          <pagination v-show="projectTotal > 0" v-model:page="projectQuery.pageNum" v-model:limit="projectQuery.pageSize" :total="projectTotal" @pagination="loadProjects" />
        </el-tab-pane>

        <el-tab-pane label="附件回收站" name="file">
          <el-form :model="fileQuery" :inline="true" class="mb-2">
            <el-form-item label="文件名">
              <el-input v-model="fileQuery.originalName" clearable placeholder="请输入文件名" @keyup.enter="loadFiles" />
            </el-form-item>
            <el-form-item label="媒体类型">
              <el-select v-model="fileQuery.mediaType" clearable placeholder="全部" style="width: 140px">
                <el-option label="视频" value="video" />
                <el-option label="图片" value="image" />
                <el-option label="文档" value="document" />
              </el-select>
            </el-form-item>
            <el-form-item label="项目ID">
              <el-input v-model="fileQuery.projectId" clearable placeholder="可选" style="width: 130px" @keyup.enter="loadFiles" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="loadFiles">查询</el-button>
              <el-button icon="Refresh" @click="resetFileQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="fileLoading" border :data="fileRows">
            <el-table-column type="index" label="#" width="60" />
            <el-table-column label="文件名" prop="originalName" min-width="240" show-overflow-tooltip />
            <el-table-column label="项目ID" prop="projectId" width="100" />
            <el-table-column label="要求ID" prop="requirementId" width="100" />
            <el-table-column label="类型" prop="mediaType" width="90">
              <template #default="{ row }">
                <el-tag>{{ row.mediaType || row.fileExt || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="大小" prop="fileSize" width="110">
              <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column label="技术校验" width="110">
              <template #default="{ row }">
                <el-tag :type="checkType(row.checkStatus)">{{ checkLabel(row.checkStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="预览状态" prop="previewStatus" width="120">
              <template #default="{ row }">
                <el-tag :type="previewStatusType(row.previewStatus)">{{ previewLabel(row.previewStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="上传时间" prop="uploadedAt" width="170" />
            <el-table-column label="删除账号" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.deletedByName || row.updateBy || '-' }}</template>
            </el-table-column>
            <el-table-column label="删除时间" prop="updateTime" width="170" />
            <el-table-column label="操作" width="260" fixed="right" align="center">
              <template #default="{ row }">
                <el-button :disabled="!canPreview(row)" link type="primary" icon="View" @click="openPreview(row)">预览</el-button>
                <el-button link type="primary" icon="Download" @click="downloadFile(row)">下载</el-button>
                <el-button v-hasPermi="['crehn:recycle:restore']" link type="success" icon="RefreshLeft" @click="restoreFile(row)">恢复</el-button>
                <el-button v-hasPermi="['crehn:recycle:remove']" link type="danger" icon="Delete" @click="purgeFile(row)">永久清理</el-button>
              </template>
            </el-table-column>
          </el-table>
          <pagination v-show="fileTotal > 0" v-model:page="fileQuery.pageNum" v-model:limit="fileQuery.pageSize" :total="fileTotal" @pagination="loadFiles" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <ArtPopupDialog v-model="previewDialog.visible" :title="previewDialog.title" width="80%" wide append-to-body>
      <video v-if="previewDialog.type === 'video'" class="preview-media" :src="previewDialog.url" controls preload="metadata" />
      <div v-else-if="previewDialog.type === 'image'" class="preview-image-wrap">
        <img :src="previewDialog.url" :alt="previewDialog.title" />
      </div>
      <div v-else-if="previewDialog.type === 'pdf'" class="preview-pdf">
        <iframe :src="pdfViewerUrl(previewDialog.url)" />
      </div>
      <el-empty v-else description="当前文件暂无可在线预览内容，可下载原文件查看" />
      <template #footer>
        <el-button v-if="previewDialog.url" icon="Download" @click="windowOpen(previewDialog.url)">新窗口打开</el-button>
        <el-button @click="previewDialog.visible = false">关闭</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ArtRecycle" lang="ts">
import FileSaver from 'file-saver';
import {
  downloadRecycleProjectFile,
  listRecycleProject,
  listRecycleProjectFile,
  purgeRecycleProject,
  purgeRecycleProjectFile,
  restoreRecycleProject,
  restoreRecycleProjectFile
} from '@/api/crehn/project';
import { ProjectFileVO, ProjectVO } from '@/api/crehn/types';
import { useRoute } from 'vue-router';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const route = useRoute();

const activeTab = ref('project');
const projectLoading = ref(false);
const fileLoading = ref(false);
const projectRows = ref<ProjectVO[]>([]);
const fileRows = ref<ProjectFileVO[]>([]);
const projectTotal = ref(0);
const fileTotal = ref(0);
const projectQuery = reactive<any>({ pageNum: 1, pageSize: 10, projectName: '', activityId: '', categoryId: '', projectId: '' });
const fileQuery = reactive<any>({ pageNum: 1, pageSize: 10, originalName: '', mediaType: '', projectId: '' });
const previewDialog = reactive({ visible: false, title: '', url: '', type: '' });

const handleTabChange = () => {
  if (activeTab.value === 'project') loadProjects();
  else loadFiles();
};

const loadProjects = async () => {
  projectLoading.value = true;
  try {
    const { projectId, ...baseQuery } = projectQuery;
    const params = {
      ...baseQuery,
      id: projectId || undefined,
      activityId: projectQuery.activityId || undefined,
      categoryId: projectQuery.categoryId || undefined
    };
    const res = await listRecycleProject(params);
    projectRows.value = res.rows || [];
    projectTotal.value = res.total || 0;
  } finally {
    projectLoading.value = false;
  }
};

const loadFiles = async () => {
  fileLoading.value = true;
  try {
    const params = { ...fileQuery, projectId: fileQuery.projectId || undefined };
    const res = await listRecycleProjectFile(params);
    fileRows.value = res.rows || [];
    fileTotal.value = res.total || 0;
  } finally {
    fileLoading.value = false;
  }
};

const resetProjectQuery = () => {
  Object.assign(projectQuery, { pageNum: 1, projectName: '', activityId: '', categoryId: '', projectId: '' });
  loadProjects();
};

const resetFileQuery = () => {
  Object.assign(fileQuery, { pageNum: 1, originalName: '', mediaType: '', projectId: '' });
  loadFiles();
};

const restoreProject = async (row: ProjectVO) => {
  await proxy?.$modal.confirm(`确认恢复项目“${row.projectName}”？`);
  await restoreRecycleProject(row.id!);
  proxy?.$modal.msgSuccess('恢复成功');
  await loadProjects();
};

const purgeProject = async (row: ProjectVO) => {
  await proxy?.$modal.confirm(`确认永久清理项目“${row.projectName}”？已评分或已生成结果的项目会被系统拒绝清理。`);
  await purgeRecycleProject(row.id!);
  proxy?.$modal.msgSuccess('已永久清理项目记录');
  await loadProjects();
};

const restoreFile = async (row: ProjectFileVO) => {
  await proxy?.$modal.confirm(`确认恢复附件“${row.originalName}”？`);
  await restoreRecycleProjectFile(row.id!);
  proxy?.$modal.msgSuccess('恢复成功');
  await loadFiles();
};

const purgeFile = async (row: ProjectFileVO) => {
  await proxy?.$modal.confirm(`确认永久清理附件记录“${row.originalName}”？该操作不立即删除对象存储中的原始文件。`);
  await purgeRecycleProjectFile(row.id!);
  proxy?.$modal.msgSuccess('已永久清理记录');
  await loadFiles();
};

const downloadFile = async (row: ProjectFileVO) => {
  if (!row.id) return;
  const blob = (await downloadRecycleProjectFile(row.id)) as unknown as Blob;
  FileSaver.saveAs(blob, row.originalName || `project-file-${row.id}`);
};

const openPreview = (row: ProjectFileVO) => {
  const url = previewOpenUrl(row);
  if (!url) {
    proxy?.$modal.msgWarning('当前文件暂无可预览地址，可下载原文件查看');
    return;
  }
  previewDialog.title = row.originalName || '附件预览';
  previewDialog.url = url;
  previewDialog.type = resolvePreviewType(row);
  previewDialog.visible = true;
};

const fileExt = (file?: ProjectFileVO) => String(file?.fileExt || file?.originalName?.split('.').pop() || '').toLowerCase();
const videoExts = ['mp4', 'mov', 'mpg', 'mpeg', 'webm'];
const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'webp'];

const resolvePreviewType = (file?: ProjectFileVO) => {
  const ext = fileExt(file);
  if (videoExts.includes(ext)) return 'video';
  if (imageExts.includes(ext)) return 'image';
  if (ext === 'pdf' || file?.previewExt?.toLowerCase() === 'pdf' || file?.previewStatus === 'converted') return 'pdf';
  return '';
};

const previewUrl = (file?: ProjectFileVO) => {
  if (!file) return '';
  if (file.previewPath) return file.previewPath;
  const ext = fileExt(file);
  if (videoExts.includes(ext) || imageExts.includes(ext) || ext === 'pdf') return file.storagePath || '';
  return '';
};

const absoluteFileUrl = (url?: string) => {
  if (!url) return '';
  if (/^https?:\/\//i.test(url) || url.startsWith('blob:') || url.startsWith('data:')) return url;
  const origin = window.location.origin;
  return `${origin}${url.startsWith('/') ? '' : '/'}${url}`;
};

const previewOpenUrl = (file?: ProjectFileVO) => absoluteFileUrl(previewUrl(file));
const canPreview = (file?: ProjectFileVO) => !!previewOpenUrl(file) && !!resolvePreviewType(file);
const pdfViewerUrl = (url?: string) => (url ? `${url.split('#')[0]}#toolbar=0&navpanes=0&scrollbar=1` : '');
const windowOpen = (url?: string) => {
  if (url) window.open(url, '_blank', 'noopener,noreferrer');
};

const formatSize = (size?: number) => {
  if (!size && size !== 0) return '-';
  const units = ['B', 'KB', 'MB', 'GB'];
  let value = Number(size);
  let index = 0;
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024;
    index += 1;
  }
  return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
};

const statusLabel = (status?: string) => {
  const map: Record<string, string> = { draft: '草稿', submitted: '待审核', returned: '已退回', audit_passed: '已通过', recycled: '回收站' };
  return map[status || ''] || status || '-';
};

const checkLabel = (status?: string) => {
  const map: Record<string, string> = { passed: '通过', warning: '提示', failed: '失败' };
  return map[status || ''] || status || '-';
};

const checkType = (status?: string): ElTagType => {
  const map: Record<string, ElTagType> = { passed: 'success', warning: 'warning', failed: 'danger' };
  return map[status || ''] || 'info';
};

const previewLabel = (status?: string) => {
  const map: Record<string, string> = { queued: '排队中', converting: '转换中', converted: '可预览', failed: '转换失败' };
  return map[status || ''] || status || '原文件预览';
};

const previewStatusType = (status?: string): ElTagType => {
  const map: Record<string, ElTagType> = { queued: 'warning', converting: 'warning', converted: 'success', failed: 'danger' };
  return map[status || ''] || 'info';
};

const queryText = (value: unknown) => {
  if (Array.isArray(value)) return value[0] ? String(value[0]) : '';
  return value == null ? '' : String(value);
};

const applyRouteFilters = () => {
  const tab = queryText(route.query.tab);
  activeTab.value = tab === 'file' ? 'file' : 'project';
  projectQuery.categoryId = queryText(route.query.categoryId);
  projectQuery.projectId = queryText(route.query.projectId);
  fileQuery.projectId = queryText(route.query.projectId);
};

onMounted(() => {
  applyRouteFilters();
  handleTabChange();
});

watch(
  () => route.fullPath,
  () => {
    applyRouteFilters();
    handleTabChange();
  }
);
</script>

<style scoped>
.recycle-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.recycle-tip {
  max-width: 900px;
}

.preview-media {
  width: 100%;
  max-height: 72vh;
  background: #111827;
}

.preview-image-wrap {
  max-height: 72vh;
  overflow: auto;
  text-align: center;
}

.preview-image-wrap img {
  max-width: 100%;
}

.preview-pdf iframe {
  width: 100%;
  height: 72vh;
  border: 0;
}
</style>
