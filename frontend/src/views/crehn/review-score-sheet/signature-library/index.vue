<template>
  <div class="p-2 signature-library-admin-page">
    <el-alert
      class="mb-[10px]"
      type="warning"
      show-icon
      :closable="false"
      title="这里管理评分老师的可复用手写签名。停用、恢复和归档均需记录原因；已保存签名表的历史快照不会被改变。"
    />

    <el-card shadow="hover" class="mb-[10px]">
      <el-form :model="query" :inline="true" @submit.prevent>
        <el-form-item label="签字老师">
          <el-input v-model="query.reviewerKeyword" clearable placeholder="姓名或账号" style="width: 190px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="签名名称">
          <el-input v-model="query.signatureName" clearable placeholder="请输入签名名称" style="width: 180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.libraryStatus" clearable placeholder="全部状态" style="width: 130px">
            <el-option label="可用" value="active" />
            <el-option label="已停用" value="disabled" />
            <el-option label="已归档" value="archived" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button v-hasPermi="['crehn:reviewSheet:signatureManage']" type="primary" icon="Search" @click="search">搜索</el-button>
          <el-button icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <template #header>
        <div class="signature-library-header">
          <strong>评分老师签名库</strong>
          <el-tag type="info" effect="plain">共 {{ total }} 条</el-tag>
        </div>
      </template>
      <el-table v-loading="loading" :data="rows" border size="small" max-height="calc(100vh - 320px)">
        <el-table-column label="手写签名" width="132" align="center">
          <template #default="scope">
            <el-image
              v-if="scope.row.imageUrl || scope.row.url"
              :src="scope.row.imageUrl || scope.row.url"
              fit="contain"
              class="signature-thumb"
              :preview-src-list="[scope.row.imageUrl || scope.row.url]"
              preview-teleported
            />
            <span v-else class="text-[var(--el-text-color-placeholder)]">图片不可用</span>
          </template>
        </el-table-column>
        <el-table-column label="签名名称" prop="signatureName" min-width="150" show-overflow-tooltip />
        <el-table-column label="签字老师" min-width="150" show-overflow-tooltip>
          <template #default="scope">
            <div>{{ scope.row.reviewerName || '-' }}</div>
            <span class="signature-library-muted">{{ scope.row.reviewerUserName || scope.row.reviewerUserId || '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="默认" width="82" align="center">
          <template #default="scope"
            ><el-tag :type="scope.row.defaultSignature ? 'success' : 'info'" effect="plain">{{
              scope.row.defaultSignature ? '默认' : '否'
            }}</el-tag></template
          >
        </el-table-column>
        <el-table-column label="状态" width="92" align="center">
          <template #default="scope"
            ><el-tag :type="statusTag(scope.row.libraryStatus)" effect="plain">{{ statusText(scope.row.libraryStatus) }}</el-tag></template
          >
        </el-table-column>
        <el-table-column label="最后变更" min-width="205" show-overflow-tooltip>
          <template #default="scope">
            <div>{{ scope.row.statusChangedAt || scope.row.updateTime || '-' }}</div>
            <span v-if="scope.row.statusChangedByName" class="signature-library-muted">{{ scope.row.statusChangedByName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作原因" min-width="210" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.statusReason || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="245" fixed="right" align="center">
          <template #default="scope">
            <el-button v-hasPermi="['crehn:reviewSheet:signatureManage']" link type="primary" icon="View" @click="viewSignature(scope.row)"
              >查看</el-button
            >
            <el-button
              v-if="scope.row.libraryStatus === 'active'"
              v-hasPermi="['crehn:reviewSheet:signatureManage:edit']"
              link
              type="warning"
              @click="changeStatus(scope.row, 'disable')"
              >停用</el-button
            >
            <el-button v-else v-hasPermi="['crehn:reviewSheet:signatureManage:edit']" link type="success" @click="changeStatus(scope.row, 'restore')"
              >恢复</el-button
            >
            <el-button
              v-if="scope.row.libraryStatus !== 'archived'"
              v-hasPermi="['crehn:reviewSheet:signatureManage:archive']"
              link
              type="danger"
              @click="changeStatus(scope.row, 'archive')"
              >归档</el-button
            >
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !rows.length" :image-size="76" description="当前筛选下没有签名记录" />
      <pagination v-show="total > 0" v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="load" />
    </el-card>

    <ArtPopupDialog v-model="detailVisible" title="评分老师手写签名" width="520px" append-to-body destroy-on-close>
      <template v-if="current">
        <el-descriptions :column="2" border size="small" class="mb-[12px]">
          <el-descriptions-item label="签字老师">{{ current.reviewerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="签名名称">{{ current.signatureName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(current.libraryStatus) }}</el-descriptions-item>
          <el-descriptions-item label="最后变更">{{ current.statusChangedAt || current.updateTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作原因" :span="2">{{ current.statusReason || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="signature-detail-image">
          <el-image
            v-if="current.imageUrl || current.url"
            :src="current.imageUrl || current.url"
            fit="contain"
            :preview-src-list="[current.imageUrl || current.url]"
            preview-teleported
          />
        </div>
      </template>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ReviewScoreSheetSignatureLibrary" lang="ts">
import { ElMessageBox } from 'element-plus';
import {
  archiveReviewScoreSheetSignatureAdmin,
  disableReviewScoreSheetSignatureAdmin,
  getReviewScoreSheetSignatureAdmin,
  pageReviewScoreSheetSignatureAdmin,
  restoreReviewScoreSheetSignatureAdmin
} from '@/api/crehn/review';
import type { ReviewScoreSheetSignatureAdminQuery, ReviewScoreSheetSignatureAdminVO } from '@/api/crehn/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const defaultQuery = (): ReviewScoreSheetSignatureAdminQuery => ({ pageNum: 1, pageSize: 10 });
const query = reactive<ReviewScoreSheetSignatureAdminQuery>(defaultQuery());
const loading = ref(false);
const rows = ref<ReviewScoreSheetSignatureAdminVO[]>([]);
const total = ref(0);
const detailVisible = ref(false);
const current = ref<ReviewScoreSheetSignatureAdminVO>();

const statusText = (status?: string) => ({ active: '可用', disabled: '已停用', archived: '已归档' })[String(status || 'active')] || '未知';
const statusTag = (status?: string) => (status === 'active' ? 'success' : status === 'disabled' ? 'warning' : 'info');

const load = async () => {
  loading.value = true;
  try {
    const response: any = await pageReviewScoreSheetSignatureAdmin(query);
    const payload = response?.data ?? response ?? {};
    rows.value = payload.rows || [];
    total.value = Number(payload.total || 0);
  } finally {
    loading.value = false;
  }
};

const search = async () => {
  query.pageNum = 1;
  await load();
};

const reset = async () => {
  Object.assign(query, defaultQuery());
  await load();
};

const viewSignature = async (row: ReviewScoreSheetSignatureAdminVO) => {
  if (!row.id) return;
  const { data } = await getReviewScoreSheetSignatureAdmin(row.id);
  current.value = data || row;
  detailVisible.value = true;
};

const changeStatus = async (row: ReviewScoreSheetSignatureAdminVO, action: 'disable' | 'restore' | 'archive') => {
  if (!row.id) return;
  const actionText = { disable: '停用', restore: '恢复', archive: '归档' }[action];
  try {
    const result = await ElMessageBox.prompt(`请填写${actionText}原因：`, `${actionText}评分老师签名`, {
      confirmButtonText: `确认${actionText}`,
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '请填写可追溯的操作原因',
      inputValidator: (value) => (String(value || '').trim() ? true : '操作原因不能为空')
    });
    const reason = String(result.value || '').trim();
    if (action === 'disable') await disableReviewScoreSheetSignatureAdmin(row.id, reason);
    if (action === 'restore') await restoreReviewScoreSheetSignatureAdmin(row.id, reason);
    if (action === 'archive') await archiveReviewScoreSheetSignatureAdmin(row.id, reason);
    proxy?.$modal.msgSuccess(`签名已${actionText}`);
    await load();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      // Shared request handling displays the authoritative service-side error.
    }
  }
};

onMounted(load);
</script>

<style scoped lang="scss">
.signature-library-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.signature-thumb {
  width: 96px;
  height: 50px;
  cursor: zoom-in;
}
.signature-library-muted {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.signature-detail-image {
  display: flex;
  justify-content: center;
  min-height: 160px;
}
.signature-detail-image :deep(.el-image) {
  width: min(100%, 430px);
  min-height: 160px;
}
</style>
