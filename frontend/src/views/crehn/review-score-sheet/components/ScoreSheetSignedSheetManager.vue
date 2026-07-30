<template>
  <div :class="['signed-sheet-manager', { 'is-embedded': embedded }]">
    <el-card v-if="!embedded" shadow="hover" class="mb-[10px] signed-sheet-intro-card">
      <el-alert type="info" show-icon :closable="false" :title="introText" />
    </el-card>

    <el-card shadow="hover" class="mb-[10px] signed-sheet-filter-card">
      <el-form :model="query" :inline="true" class="signed-sheet-filter-form">
        <el-form-item label="活动">
          <el-select v-model="query.activityId" clearable filterable placeholder="全部活动" style="width: 230px" @change="handleActivityChange">
            <el-option v-for="item in activityOptions" :key="item.id" :label="item.activityName" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-select
            v-model="query.categoryId"
            clearable
            filterable
            placeholder="全部类别"
            style="width: 180px"
            :disabled="!query.activityId"
            @change="handleCategoryChange"
          >
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.categoryName" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isAdmin" label="签字老师">
          <el-select
            v-model="query.reviewerUserId"
            clearable
            filterable
            remote
            reserve-keyword
            placeholder="全部签字老师"
            :remote-method="loadReviewerOptions"
            :loading="reviewerLoading"
            style="width: 210px"
          >
            <el-option v-for="item in reviewerOptions" :key="reviewerIdOf(item)" :label="reviewerLabel(item)" :value="reviewerIdOf(item)!" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 130px">
            <el-option label="有效" value="active" />
            <el-option label="已撤回" value="withdrawn" />
          </el-select>
        </el-form-item>
        <el-form-item label="提交时间">
          <el-date-picker
            v-model="signedAtRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 350px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="search">搜索</el-button>
          <el-button icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="signed-sheet-table-card">
      <template #header>
        <div class="signed-sheet-table-header">
          <div>
            <strong>{{ title }}</strong>
            <span>{{
              isAdmin
                ? '可按范围查看全部签名表；无签名提交会明确标识，撤回操作会记录实际操作者。'
                : '仅显示本人签名表；结果生成前可填写原因自行撤回，撤回后评分恢复为可修改状态。'
            }}</span>
          </div>
          <el-tag type="info" effect="plain">共 {{ total }} 张</el-tag>
        </div>
      </template>

      <el-table v-loading="loading" :data="sheets" border size="small" class="signed-sheet-table" max-height="calc(100vh - 370px)">
        <el-table-column label="活动" min-width="160" show-overflow-tooltip>
          <template #default="scope">{{ activityLabel(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="类别" min-width="110" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.categoryName || scope.row.categoryId || '-' }}</template>
        </el-table-column>
        <el-table-column label="签字老师" min-width="115" show-overflow-tooltip>
          <template #default="scope">{{ scope.row.reviewerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="评分数" width="88" align="center">
          <template #default="scope">{{ scope.row.total ?? scope.row.rows?.length ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="提交方式" width="94" align="center">
          <template #default="scope">
            <el-tag :type="isUnsignedSubmission(scope.row) ? 'info' : 'success'" effect="plain">
              {{ isUnsignedSubmission(scope.row) ? '无签名提交' : '已签名' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" prop="signedAt" min-width="158" />
        <el-table-column label="状态" width="92" align="center">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row)" effect="plain">{{ statusLabel(scope.row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="撤回审计" min-width="245" show-overflow-tooltip>
          <template #default="scope">
            <template v-if="isWithdrawn(scope.row)">
              <div class="withdraw-audit-cell">
                <span>{{ withdrawalActor(scope.row) }} · {{ withdrawalModeLabel(scope.row) }}</span>
                <span>{{ withdrawalTime(scope.row) || '-' }}</span>
                <span :title="withdrawalReason(scope.row)">原因：{{ withdrawalReason(scope.row) || '-' }}</span>
              </div>
            </template>
            <span v-else class="signed-sheet-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="252" fixed="right" align="center" class-name="signed-sheet-actions">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="openPreview(scope.row)">查看</el-button>
            <el-tooltip v-if="isWithdrawn(scope.row)" content="已撤回评分提交单仅供审计查看，不能打印或下载" placement="top">
              <span>
                <el-button link type="info" icon="Printer" disabled>打印</el-button>
              </span>
            </el-tooltip>
            <el-button v-else link type="primary" icon="Printer" @click="printSheet(scope.row)">打印</el-button>
            <el-tooltip v-if="isWithdrawn(scope.row)" content="已撤回评分提交单仅供审计查看，不能打印或下载" placement="top">
              <span>
                <el-button link type="info" icon="Download" disabled>下载</el-button>
              </span>
            </el-tooltip>
            <el-button v-else link type="success" icon="Download" @click="downloadSheet(scope.row)">下载</el-button>
            <el-button
              v-if="canWithdraw && !isWithdrawn(scope.row)"
              link
              type="danger"
              icon="RefreshLeft"
              :loading="withdrawLoadingId === sheetIdOf(scope.row)"
              @click="withdrawSheet(scope.row)"
            >
              撤回
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !sheets.length" :image-size="76" description="当前筛选下还没有签名表" />
      <pagination v-show="total > 0" v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="load" />
    </el-card>

    <ArtPopupDialog
      v-model="previewVisible"
      :title="isWithdrawn(currentSheet) ? `已撤回${sheetRecordLabel(currentSheet)}审计预览` : `${sheetRecordLabel(currentSheet)}预览`"
      width="90%"
      top="4vh"
      append-to-body
      destroy-on-close
    >
      <div v-if="currentSheet" class="signed-sheet-preview-shell">
        <el-alert
          v-if="isWithdrawn(currentSheet)"
          class="mb-[10px]"
          type="warning"
          show-icon
          :closable="false"
          :title="withdrawnPreviewText(currentSheet)"
        />
        <ScoreSheetPaperPreview
          :template="currentSheet.template || fallbackTemplate"
          :rows="currentRows"
          :export-time="currentSheet.exportTime || currentSheet.signedAt || ''"
          :signature="sheetSignature(currentSheet)"
          :signature-placement="sheetPlacement(currentSheet)"
          table-max-height="52vh"
          compact
        />
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button type="primary" plain icon="Printer" :disabled="isWithdrawn(currentSheet)" @click="printSheet(currentSheet)">打印表格</el-button>
        <el-button type="success" icon="Download" :disabled="isWithdrawn(currentSheet)" @click="downloadSheet(currentSheet)">下载表格</el-button>
        <el-button
          v-if="canWithdraw && currentSheet && !isWithdrawn(currentSheet)"
          type="danger"
          plain
          icon="RefreshLeft"
          :loading="withdrawLoadingId === sheetIdOf(currentSheet)"
          @click="withdrawSheet(currentSheet)"
        >
          撤回签名表
        </el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup lang="ts">
import { listActivityOptions, listCategoryOptions } from '@/api/crehn/activity';
import {
  exportReviewScoreSheetSignedSheet,
  getReviewScoreSheetSignedSheet,
  listMyReviewActivityOptions,
  listMyReviewCategoryOptions,
  listReviewScoreSheetSignedSheetReviewerOptions,
  pageReviewScoreSheetSignedSheets,
  withdrawReviewScoreSheetSignedSheet
} from '@/api/crehn/review';
import type {
  ActivityCategoryVO,
  ActivityVO,
  ReviewScoreSheetSignaturePlacementVO,
  ReviewScoreSheetSignatureVO,
  ReviewScoreSheetSignedSheetQuery,
  ReviewScoreSheetSignedSheetReviewerOptionVO,
  ReviewScoreSheetSignedSheetVO
} from '@/api/crehn/types';
import { checkPermi } from '@/utils/permission';
import { blobValidate } from '@/utils/ruoyi';
import { ElMessageBox } from 'element-plus';
import FileSaver from 'file-saver';
import ScoreSheetPaperPreview from './ScoreSheetPaperPreview.vue';
import { emptyScoreSheetTemplate, openScoreSheetPrintPreview, scoreTaskToPreviewRow } from '../scoreSheet';

const props = withDefaults(
  defineProps<{
    mode: 'self' | 'admin';
    embedded?: boolean;
    autoLoad?: boolean;
  }>(),
  { embedded: false, autoLoad: true }
);

const emit = defineEmits<{
  (e: 'changed'): void;
}>();

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const defaultQuery = (): ReviewScoreSheetSignedSheetQuery => ({ pageNum: 1, pageSize: 10, status: '' });
const query = reactive<ReviewScoreSheetSignedSheetQuery>(defaultQuery());
const signedAtRange = ref<string[]>([]);
const loading = ref(false);
const sheets = ref<ReviewScoreSheetSignedSheetVO[]>([]);
const total = ref(0);
const activityOptions = ref<ActivityVO[]>([]);
const categoryOptions = ref<ActivityCategoryVO[]>([]);
const reviewerOptions = ref<ReviewScoreSheetSignedSheetReviewerOptionVO[]>([]);
const reviewerLoading = ref(false);
const previewVisible = ref(false);
const currentSheet = ref<ReviewScoreSheetSignedSheetVO>();
const withdrawLoadingId = ref<string | number>();
const fallbackTemplate = emptyScoreSheetTemplate();

const isAdmin = computed(() => props.mode === 'admin');
const canWithdraw = computed(() => !isAdmin.value || checkPermi(['crehn:reviewSheet:withdraw']));
const title = computed(() => (isAdmin.value ? '签名评分表' : '我的签名表'));
const introText = computed(() =>
  isAdmin.value
    ? '签名评分表保存评分、模板、可选手写签名与提交时间的历史快照。管理员强制撤回必须填写原因，且会留下操作者审计信息。'
    : '这里仅显示本人签名表。结果生成前可填写原因自行撤回；撤回后评分恢复为可修改状态并保留审计记录。'
);
const currentRows = computed(() =>
  (currentSheet.value?.rows || []).map((row) => scoreTaskToPreviewRow(row, currentSheet.value?.reviewerName || '评审老师'))
);

const sheetIdOf = (sheet?: ReviewScoreSheetSignedSheetVO) => sheet?.id ?? sheet?.sheetId;
const activityLabel = (sheet: ReviewScoreSheetSignedSheetVO) => sheet.activityName || sheet.activityId || '-';
const reviewerIdOf = (option: ReviewScoreSheetSignedSheetReviewerOptionVO) => option.reviewerUserId ?? option.userId;
const reviewerLabel = (option: ReviewScoreSheetSignedSheetReviewerOptionVO) =>
  option.reviewerName || option.nickName || option.userName || String(reviewerIdOf(option) || '-');

const isWithdrawn = (sheet?: ReviewScoreSheetSignedSheetVO) => {
  if (!sheet) return false;
  const status = String(sheet.status || '')
    .trim()
    .toLowerCase();
  return !!sheet.withdrawnAt || !!sheet.revokedAt || ['withdrawn', 'revoked', 'void', 'cancelled', 'canceled'].includes(status);
};
const statusLabel = (sheet: ReviewScoreSheetSignedSheetVO) => (isWithdrawn(sheet) ? '已撤回' : '有效');
const statusTagType = (sheet: ReviewScoreSheetSignedSheetVO) => (isWithdrawn(sheet) ? 'warning' : 'success');
const isUnsignedSubmission = (sheet?: ReviewScoreSheetSignedSheetVO) => String(sheet?.submissionMode || '').toUpperCase() === 'UNSIGNED';
const sheetRecordLabel = (sheet?: ReviewScoreSheetSignedSheetVO) => (isUnsignedSubmission(sheet) ? '评分提交单' : '签名表');
const withdrawalActor = (sheet: ReviewScoreSheetSignedSheetVO) => sheet.withdrawnByName || sheet.revokedByName || '未知操作者';
const withdrawalTime = (sheet: ReviewScoreSheetSignedSheetVO) => sheet.withdrawnAt || sheet.revokedAt || '';
const withdrawalReason = (sheet: ReviewScoreSheetSignedSheetVO) => sheet.withdrawReason || sheet.revokeReason || '';
const withdrawalModeLabel = (sheet: ReviewScoreSheetSignedSheetVO) =>
  String(sheet.withdrawalMode || '').toUpperCase() === 'SELF' ? '本人撤回' : '管理员撤回';
const withdrawnPreviewText = (sheet: ReviewScoreSheetSignedSheetVO) =>
  `该${sheetRecordLabel(sheet)}已于 ${withdrawalTime(sheet) || '-'} 由${withdrawalActor(sheet)}${withdrawalModeLabel(sheet)}；原因：${withdrawalReason(sheet) || '-'}。仅可审计查看，不能打印或下载。`;

const sheetSignature = (sheet?: ReviewScoreSheetSignedSheetVO): ReviewScoreSheetSignatureVO | undefined => {
  if (sheet?.signature) return sheet.signature;
  if (!sheet?.signatureId || !sheet.signatureUrl) return undefined;
  return { id: sheet.signatureId, signatureName: sheet.signatureName || '评分老师签名', imageUrl: sheet.signatureUrl };
};

const sheetPlacement = (sheet?: ReviewScoreSheetSignedSheetVO): ReviewScoreSheetSignaturePlacementVO | undefined => {
  const placement = sheet?.placement || sheet?.signaturePlacement;
  return placement
    ? {
        ...placement,
        slotKey: placement.slotKey || sheet?.slotKey || 'reviewer_signature',
        signedAt: placement.signedAt || sheet?.signedAt
      }
    : undefined;
};

const normalizePagedResult = (response: any) => {
  const payload = response?.data ?? response;
  const rows = Array.isArray(payload) ? payload : payload?.rows || [];
  return { rows: rows as ReviewScoreSheetSignedSheetVO[], total: Number(payload?.total ?? rows.length) };
};

const load = async () => {
  loading.value = true;
  try {
    const response = await pageReviewScoreSheetSignedSheets({
      ...query,
      ownerOnly: props.mode === 'self',
      signedAtStart: signedAtRange.value?.[0] || undefined,
      signedAtEnd: signedAtRange.value?.[1] || undefined
    });
    const result = normalizePagedResult(response);
    sheets.value = result.rows;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
};

const loadActivityOptions = async () => {
  const response = props.mode === 'admin' ? await listActivityOptions() : await listMyReviewActivityOptions();
  activityOptions.value = response.data || [];
};

const loadReviewerOptions = async (keyword = '') => {
  if (!isAdmin.value) return;
  reviewerLoading.value = true;
  try {
    const { data } = await listReviewScoreSheetSignedSheetReviewerOptions({
      activityId: query.activityId,
      categoryId: query.categoryId,
      keyword
    });
    reviewerOptions.value = data || [];
  } finally {
    reviewerLoading.value = false;
  }
};

const handleActivityChange = async () => {
  query.categoryId = undefined;
  categoryOptions.value = [];
  query.reviewerUserId = undefined;
  reviewerOptions.value = [];
  if (query.activityId) {
    const response = props.mode === 'admin' ? await listCategoryOptions(query.activityId) : await listMyReviewCategoryOptions(query.activityId);
    categoryOptions.value = response.data || [];
  }
  await loadReviewerOptions();
};

const handleCategoryChange = async () => {
  query.reviewerUserId = undefined;
  reviewerOptions.value = [];
  await loadReviewerOptions();
};

const search = async () => {
  query.pageNum = 1;
  await load();
};

const reset = async () => {
  Object.assign(query, defaultQuery());
  signedAtRange.value = [];
  categoryOptions.value = [];
  reviewerOptions.value = [];
  await load();
};

const resolveSheet = async (sheet?: ReviewScoreSheetSignedSheetVO) => {
  const sheetId = sheetIdOf(sheet);
  if (!sheetId) return undefined;
  if (sheet?.template && sheet.rows?.length) return sheet;
  const { data } = await getReviewScoreSheetSignedSheet(sheetId);
  return data;
};

const openPreview = async (sheet: ReviewScoreSheetSignedSheetVO) => {
  const detail = await resolveSheet(sheet);
  if (!detail) return;
  currentSheet.value = detail;
  previewVisible.value = true;
};

const printSheet = async (sheet?: ReviewScoreSheetSignedSheetVO) => {
  if (!sheet || isWithdrawn(sheet)) {
    proxy?.$modal.msgWarning('已撤回签名表仅供审计查看，不能打印');
    return;
  }
  const detail = await resolveSheet(sheet);
  if (!detail || isWithdrawn(detail)) {
    proxy?.$modal.msgWarning('已撤回签名表仅供审计查看，不能打印');
    return;
  }
  const rows = (detail.rows || []).map((row) => scoreTaskToPreviewRow(row, detail.reviewerName || '评审老师'));
  const opened = openScoreSheetPrintPreview(
    detail.template || fallbackTemplate,
    rows,
    detail.exportTime || detail.signedAt || '',
    sheetPlacement(detail),
    sheetSignature(detail)
  );
  if (!opened) proxy?.$modal.msgWarning('浏览器拦截了打印窗口，请允许当前网站打开弹窗后重试');
};

const downloadSheet = async (sheet?: ReviewScoreSheetSignedSheetVO) => {
  const sheetId = sheetIdOf(sheet);
  if (!sheetId) return;
  if (isWithdrawn(sheet)) {
    proxy?.$modal.msgWarning('已撤回签名表仅供审计查看，不能下载');
    return;
  }
  try {
    const blob = await exportReviewScoreSheetSignedSheet(sheetId);
    if (!blobValidate(blob)) {
      const text = await new Blob([blob as any]).text();
      const error = JSON.parse(text || '{}');
      proxy?.$modal.msgError(error.msg || '下载签名表失败');
      return;
    }
    const detail = sheet || currentSheet.value;
    const categoryName = detail?.categoryName || '评审打分表';
    FileSaver.saveAs(
      new Blob([blob as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
      `${categoryName.replace(/[\\/:*?"<>|]/g, '_')}-签名评审打分表.xlsx`
    );
  } catch {
    // Request interception has already displayed the authoritative server error.
  }
};

const withdrawSheet = async (sheet?: ReviewScoreSheetSignedSheetVO) => {
  const sheetId = sheetIdOf(sheet);
  if (!sheetId || isWithdrawn(sheet) || withdrawLoadingId.value === sheetId) return;
  try {
    const result = await ElMessageBox.prompt(
      isAdmin.value
        ? '管理员强制撤回会记录您的身份，并整体解锁该签字表内的评分。请填写撤回原因：'
        : '撤回后将整体解锁该签字表内的评分，并保留您的身份和撤回原因。请确认填写：',
      '撤回签名表',
      {
        confirmButtonText: '确认撤回',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputPlaceholder: '例如：发现评分表签名位置有误，需要重新签名',
        inputValidator: (value) => (String(value || '').trim() ? true : '撤回原因不能为空')
      }
    );
    const reason = String(result.value || '').trim();
    withdrawLoadingId.value = sheetId;
    const { data } = await withdrawReviewScoreSheetSignedSheet(sheetId, { reason });
    if (currentSheet.value && String(sheetIdOf(currentSheet.value)) === String(sheetId)) {
      currentSheet.value = data || currentSheet.value;
    }
    proxy?.$modal.msgSuccess('签名表已撤回，原记录已保留审计留痕');
    emit('changed');
    await load();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      // The shared request layer displays service-side refusal, including a published result lock.
    }
  } finally {
    withdrawLoadingId.value = undefined;
  }
};

const open = async (nextQuery: ReviewScoreSheetSignedSheetQuery = {}) => {
  if (!activityOptions.value.length) await loadActivityOptions();
  Object.assign(query, defaultQuery(), nextQuery);
  signedAtRange.value = nextQuery.signedAtStart || nextQuery.signedAtEnd ? [nextQuery.signedAtStart || '', nextQuery.signedAtEnd || ''] : [];
  categoryOptions.value = [];
  if (query.activityId) {
    const response = props.mode === 'admin' ? await listCategoryOptions(query.activityId) : await listMyReviewCategoryOptions(query.activityId);
    categoryOptions.value = response.data || [];
  }
  await loadReviewerOptions();
  await load();
};

onMounted(async () => {
  await loadActivityOptions();
  if (props.autoLoad) await open();
});

defineExpose({ load, open });
</script>

<style scoped lang="scss">
.signed-sheet-filter-card :deep(.el-card__body) {
  padding-bottom: 4px;
}

.signed-sheet-filter-form {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
}

.signed-sheet-table-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  > div {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  strong {
    font-size: 16px;
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 13px;
  }
}

.withdraw-audit-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;

  span:last-child {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.signed-sheet-muted {
  color: var(--el-text-color-placeholder);
}

.signed-sheet-preview-shell {
  padding: 2px;
}

@media (max-width: 960px) {
  .signed-sheet-filter-form {
    :deep(.el-form-item) {
      margin-right: 8px;
    }

    :deep(.el-date-editor) {
      width: min(350px, 100%) !important;
    }
  }
}

@media (max-width: 680px) {
  .signed-sheet-table-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .signed-sheet-filter-form {
    align-items: stretch;
    flex-direction: column;

    :deep(.el-form-item) {
      display: flex;
      margin-right: 0;
    }

    :deep(.el-form-item__content) {
      flex: 1;
    }

    :deep(.el-select),
    :deep(.el-date-editor) {
      width: 100% !important;
    }
  }
}
</style>
