<template>
  <div class="p-2 score-summary-page">
    <el-card shadow="hover" class="art-list-card score-summary-card">
      <div class="art-list-toolbar art-list-toolbar--wrap">
        <div class="art-list-toolbar__filters">
          <span class="art-list-toolbar__label">活动</span>
          <el-select v-model="queryParams.activityId" clearable filterable placeholder="全部活动" style="width: 230px" @change="handleActivityChange">
            <el-option v-for="item in activityOptions" :key="item.id" :label="item.activityName" :value="item.id!" />
          </el-select>
          <span class="art-list-toolbar__label">类别</span>
          <el-select
            v-model="queryParams.categoryId"
            clearable
            filterable
            placeholder="全部类别"
            style="width: 190px"
            :disabled="!queryParams.activityId"
            @change="handleCategoryChange"
          >
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.categoryName" :value="item.id!" />
          </el-select>
        </div>
        <div class="art-list-toolbar__actions">
          <el-input
            v-model="queryParams.projectName"
            class="art-list-toolbar__search"
            clearable
            placeholder="请输入节目名称"
            @keyup.enter="handleSearch"
          />
          <el-button v-hasPermi="['crehn:reviewScoreSummary:list']" type="primary" icon="Search" @click="handleSearch">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-popover placement="bottom-end" :width="460" trigger="click">
            <template #reference><el-button icon="Filter">更多筛选</el-button></template>
            <div class="art-list-more-filter">
              <label class="art-list-more-filter__item">
                <span>形式</span>
                <el-select v-model="queryParams.programForm" clearable filterable placeholder="全部形式">
                  <el-option v-for="item in programFormOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </label>
              <label class="art-list-more-filter__item">
                <span>甲乙组/个人</span>
                <el-select v-model="queryParams.groupOrNature" clearable filterable placeholder="全部">
                  <el-option v-for="item in groupOrNatureOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </label>
              <label class="art-list-more-filter__item">
                <span>学校</span>
                <el-select v-model="queryParams.schoolId" clearable filterable placeholder="全部学校">
                  <el-option v-for="item in schoolOptions" :key="item.id" :label="schoolLabel(item)" :value="item.id!" />
                </el-select>
              </label>
              <label class="art-list-more-filter__item">
                <span>平均分排序</span>
                <el-select v-model="queryParams.averageSort">
                  <el-option label="降序" value="desc" />
                  <el-option label="升序" value="asc" />
                  <el-option label="不排序" value="none" />
                </el-select>
              </label>
            </div>
          </el-popover>
          <el-button v-hasPermi="['crehn:reviewScoreSummary:export']" icon="Download" @click="exportSummary('current')">导出当前页</el-button>
          <el-button v-hasPermi="['crehn:reviewScoreSummary:export']" icon="Download" @click="exportSummary('all')">导出全部</el-button>
          <el-button v-hasPermi="['crehn:reviewScoreSummary:config']" icon="Setting" @click="openConfig">汇总配置</el-button>
        </div>
      </div>
      <el-alert
        class="score-summary-notice mb-[10px]"
        type="info"
        :closable="false"
        show-icon
        title="平均分按当前已提交评分实时计算；详情同时保留最近一次生成结果的平均分作为审计对照。评分、姓名、签名和时间均为只读数据。"
      />
      <el-table
        v-loading="loading"
        :border="false"
        :data="rows"
        row-key="projectId"
        class="art-list-table score-summary-table art-viewable-table"
        @row-click="openDetail"
      >
        <el-table-column
          v-for="column in visibleDataColumns"
          :key="column.fieldKey"
          :label="column.label"
          :prop="column.fieldKey"
          :width="summaryColumnWidth(column)"
          :min-width="summaryColumnMinWidth(column)"
          show-overflow-tooltip
          align="center"
        >
          <template #default="scope">
            <template v-if="isScoreColumn(column.fieldKey)">
              {{ formatScore(scoreAt(scope.row, scoreSlot(column.fieldKey))) }}
            </template>
            <template v-else-if="column.fieldKey === 'currentAverageScore'">
              {{ formatScore(scope.row.currentAverageScore) }}
            </template>
            <template v-else-if="column.fieldKey === 'warningText'">
              <el-tag :type="warningTagType(scope.row.warning?.status)">{{ scope.row.warning?.text || '-' }}</el-tag>
            </template>
            <template v-else>{{ displayColumnValue(scope.row, column.fieldKey) }}</template>
          </template>
        </el-table-column>
        <el-table-column v-if="!visibleDataColumns.length" label="暂无可显示字段" min-width="300" />
        <el-table-column fixed="right" label="操作" width="140" align="center">
          <template #default="scope">
            <div class="art-list-row-actions">
              <el-button
                v-hasPermi="['crehn:reviewScoreSummary:detail']"
                class="art-list-row-action"
                link
                type="primary"
                icon="View"
                @click.stop="openDetail(scope.row)"
              >
                查看详情
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="loadRows" />
    </el-card>

    <ArtPopupDialog v-model="detailVisible" title="分数汇总详情" width="90%" wide append-to-body destroy-on-close>
      <el-descriptions v-if="currentDetail" :column="3" border class="mb-[14px]">
        <el-descriptions-item label="节目名称">{{ currentDetail.projectName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="类别">{{ currentDetail.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="学校">{{ currentDetail.schoolName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="形式">{{ currentDetail.programForm || '-' }}</el-descriptions-item>
        <el-descriptions-item label="甲乙组/个人">{{ currentDetail.groupOrNature || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前平均分">{{ formatScore(currentDetail.currentAverageScore) }}</el-descriptions-item>
        <el-descriptions-item label="最近生成结果平均分">{{ formatScore(currentDetail.resultAverageScore) }}</el-descriptions-item>
        <el-descriptions-item label="结果生成时间">{{ currentDetail.resultGeneratedAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="评分提示">
          <el-tag :type="warningTagType(currentDetail.warning?.status)">{{ currentDetail.warning?.text || '-' }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">节目详情与附件</el-divider>
      <div v-if="currentDetail" class="detail-project-content">
        <div v-if="currentDetail.attachments?.length" class="attachment-list">
          <el-link
            v-for="file in currentDetail.attachments"
            :key="file.id || file.fileId || file.fileName"
            :href="file.previewUrl || file.url"
            target="_blank"
            type="primary"
          >
            {{ file.fileName || file.name || '附件' }}
          </el-link>
        </div>
        <span v-else class="muted-text">暂无可查看附件</span>
      </div>

      <el-divider content-position="left">评分详情</el-divider>
      <el-table v-loading="detailLoading" border :data="currentDetail?.scoreItems || []">
        <el-table-column label="评分序号" prop="slot" width="90" align="center" />
        <el-table-column label="评委老师" prop="reviewerName" min-width="150" />
        <el-table-column label="评分" width="100" align="center">
          <template #default="scope">
            {{
              scope.row.scoreValue !== null && scope.row.scoreValue !== undefined ? formatScore(scope.row.scoreValue) : scope.row.gradeValue || '-'
            }}
          </template>
        </el-table-column>
        <el-table-column label="评分时间" prop="submittedAt" width="180" />
        <el-table-column label="电子签名" min-width="220">
          <template #default="scope">
            <div v-if="scope.row.signature?.signatureUrl || scope.row.signatureUrl" class="signature-cell">
              <el-image
                :src="scope.row.signature?.signatureUrl || scope.row.signatureUrl"
                fit="contain"
                class="signature-image"
                :preview-src-list="[scope.row.signature?.signatureUrl || scope.row.signatureUrl]"
                preview-teleported
              />
              <span
                >{{ scope.row.signature?.signatureName || scope.row.signatureName || '已签名' }}
                {{ scope.row.signature?.signatureSignedAt || scope.row.signatureSignedAt || '' }}</span
              >
              <el-tag v-if="scope.row.signature?.scoreMismatch" type="danger" size="small">当前评分已变化</el-tag>
            </div>
            <el-tag v-else-if="scope.row.signature?.signatureStatus === 'withdrawn' || scope.row.signatureStatus === 'withdrawn'" type="warning"
              >签名已撤回</el-tag
            >
            <el-tag
              v-else-if="String(scope.row.signature?.submissionMode || scope.row.submissionMode || '').toUpperCase() === 'UNSIGNED'"
              type="info"
            >
              无签名提交
            </el-tag>
            <span v-else class="muted-text">未签名</span>
          </template>
        </el-table-column>
        <el-table-column label="评分意见" prop="commentText" min-width="180" show-overflow-tooltip />
      </el-table>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button></template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="configVisible" title="分数汇总配置" width="900px" append-to-body destroy-on-close>
      <el-alert
        class="mb-[12px]"
        type="warning"
        :closable="false"
        show-icon
        title="配置只控制字段显示、表头和提示规则，不会修改真实评分、评委、签名或评分时间。"
      />
      <el-form label-width="130px">
        <el-form-item label="评分列数量">
          <el-input-number v-model="configForm.scoreColumnCount" :min="1" :max="20" controls-position="right" />
        </el-form-item>
        <el-form-item label="显示字段">
          <div class="column-config-list">
            <div v-for="column in configForm.columns" :key="column.fieldKey" class="column-config-row">
              <el-checkbox v-model="column.visible" :disabled="column.fieldKey === 'actions'">显示</el-checkbox>
              <el-input v-model="column.label" placeholder="表头文字" maxlength="30" :disabled="column.fieldKey === 'actions'" />
              <el-input-number v-model="column.sortOrder" :min="1" :max="99" controls-position="right" :disabled="column.fieldKey === 'actions'" />
              <el-input-number v-model="column.width" :min="70" :max="420" controls-position="right" :disabled="column.fieldKey === 'actions'" />
              <span class="config-field-key">{{ column.fieldKey === 'actions' ? '固定右侧（140px）' : column.fieldKey }}</span>
            </div>
          </div>
        </el-form-item>
        <el-divider content-position="left">评分提示规则</el-divider>
        <el-form-item label="启用预警">
          <el-switch v-model="configForm.warning.enabled" />
        </el-form-item>
        <el-form-item label="差异公式">
          <el-select v-model="configForm.warning.formula" style="width: 360px">
            <el-option label="(最高分-最低分)÷平均分×100%" value="range_average" />
            <el-option label="(最高分-最低分)÷最高分×100%" value="range_max" />
            <el-option label="(最高分-最低分)÷最低分×100%" value="range_min" />
            <el-option label="自定义公式" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="configForm.warning.formula === 'custom'" label="自定义表达式">
          <el-input v-model="configForm.warning.formulaExpression" maxlength="200" show-word-limit placeholder="例如：(RANGE / AVG) * 100" />
          <div class="formula-help">可用变量：MAX 最高分、MIN 最低分、AVG 平均分、COUNT 有效评分数、RANGE 最高分-最低分；仅支持四则运算和括号。</div>
        </el-form-item>
        <el-form-item label="预警阈值(%)">
          <el-input-number v-model="configForm.warning.thresholdPercent" :min="0" :max="10000" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="正常提示"><el-input v-model="configForm.warning.normalText" maxlength="30" /></el-form-item>
        <el-form-item label="超出提示"><el-input v-model="configForm.warning.warningText" maxlength="30" /></el-form-item>
        <el-form-item label="评分不足"><el-input v-model="configForm.warning.insufficientText" maxlength="30" /></el-form-item>
        <el-form-item label="无法计算"><el-input v-model="configForm.warning.unsupportedText" maxlength="30" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configVisible = false">取消</el-button>
        <el-button v-hasPermi="['crehn:reviewScoreSummary:config']" type="primary" :loading="configSaving" @click="saveConfig">保存配置</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup lang="ts" name="ReviewScoreSummary">
import { listActivityOptions, listCategoryOptions } from '@/api/crehn/activity';
import { listAuditSchoolOptions } from '@/api/crehn/audit';
import {
  getReviewScoreSummaryConfig,
  getReviewScoreSummaryDetail,
  listReviewScoreSummary,
  listReviewScoreSummaryFilterOptions,
  saveReviewScoreSummaryConfig
} from '@/api/crehn/result';
import type {
  ActivityCategoryVO,
  ActivityVO,
  ReviewScoreSummaryColumnVO,
  ReviewScoreSummaryConfigVO,
  ReviewScoreSummaryDetailVO,
  ReviewScoreSummaryFilterOptionsVO,
  ReviewScoreSummaryVO
} from '@/api/crehn/types';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const DEFAULT_COLUMNS: ReviewScoreSummaryColumnVO[] = [
  { fieldKey: 'rowNo', label: '序号', visible: true, sortOrder: 1, width: 70 },
  { fieldKey: 'projectName', label: '节目名称', visible: true, sortOrder: 2, width: 180 },
  { fieldKey: 'categoryName', label: '类别', visible: true, sortOrder: 3, width: 120 },
  { fieldKey: 'programForm', label: '形式', visible: true, sortOrder: 4, width: 120 },
  { fieldKey: 'groupOrNature', label: '甲乙组/个人', visible: true, sortOrder: 5, width: 130 },
  { fieldKey: 'schoolName', label: '学校', visible: true, sortOrder: 6, width: 160 },
  { fieldKey: 'score1', label: '评分1', visible: true, sortOrder: 7, width: 90 },
  { fieldKey: 'score2', label: '评分2', visible: true, sortOrder: 8, width: 90 },
  { fieldKey: 'score3', label: '评分3', visible: true, sortOrder: 9, width: 90 },
  { fieldKey: 'currentAverageScore', label: '平均分', visible: true, sortOrder: 10, width: 100 },
  { fieldKey: 'warningText', label: '评分提示', visible: true, sortOrder: 11, width: 110 },
  { fieldKey: 'actions', label: '操作', visible: true, sortOrder: 12, width: 140 }
];

const DEFAULT_WARNING = {
  enabled: true,
  formula: 'range_average',
  formulaExpression: '(RANGE / AVG) * 100',
  thresholdPercent: 10,
  normalText: '正常',
  warningText: '需复核',
  insufficientText: '评分不足',
  unsupportedText: '无法判断'
};

const cloneColumns = () => DEFAULT_COLUMNS.map((column) => ({ ...column }));
const cloneWarning = () => ({ ...DEFAULT_WARNING });
const EXPANDABLE_SUMMARY_COLUMN_KEYS = new Set(['projectName', 'categoryName', 'programForm', 'schoolName']);
const DEFAULT_COLUMN_WIDTHS = Object.fromEntries(DEFAULT_COLUMNS.map((column) => [column.fieldKey, column.width || 120]));

const loading = ref(false);
const detailLoading = ref(false);
const configSaving = ref(false);
const total = ref(0);
const rows = ref<ReviewScoreSummaryVO[]>([]);
const activityOptions = ref<ActivityVO[]>([]);
const categoryOptions = ref<ActivityCategoryVO[]>([]);
const schoolOptions = ref<any[]>([]);
const programFormOptions = ref<string[]>([]);
const groupOrNatureOptions = ref<string[]>([]);
const detailVisible = ref(false);
const configVisible = ref(false);
const currentDetail = ref<ReviewScoreSummaryDetailVO>();
const configForm = reactive<ReviewScoreSummaryConfigVO>({
  columns: cloneColumns(),
  scoreColumnCount: 3,
  warning: cloneWarning()
});
const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  activityId: undefined,
  categoryId: undefined,
  schoolId: undefined,
  projectName: '',
  programForm: undefined,
  groupOrNature: undefined,
  averageSort: 'desc'
});

const visibleColumns = computed(() => {
  const columns = configForm.columns || [];
  const configuredScoreSlots = columns.filter((item) => isScoreColumn(item.fieldKey)).map((item) => scoreSlot(item.fieldKey));
  const scoreCount = Math.max(Number(configForm.scoreColumnCount || 3), maxScoreSlot.value, ...configuredScoreSlots);
  const scoreColumns = Array.from({ length: scoreCount }, (_, index) => {
    const fieldKey = `score${index + 1}`;
    return (
      columns.find((item) => item.fieldKey === fieldKey) || { fieldKey, label: `评分${index + 1}`, visible: true, sortOrder: 7 + index, width: 90 }
    );
  });
  return [...columns.filter((item) => !isScoreColumn(item.fieldKey)), ...scoreColumns]
    .filter((item) => item.visible !== false)
    .sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0));
});
const visibleDataColumns = computed(() => {
  const columns = visibleColumns.value.filter((column) => column.fieldKey !== 'actions');
  const warningColumns = columns.filter((column) => column.fieldKey === 'warningText');
  return [...columns.filter((column) => column.fieldKey !== 'warningText'), ...warningColumns];
});
const maxScoreSlot = computed(() => Math.max(0, ...rows.value.flatMap((row) => (row.scoreItems || []).map((item) => Number(item.slot || 0)))));

const schoolLabel = (item: any) => [item.schoolName, item.schoolCode].filter(Boolean).join(' / ');
const formatScore = (value?: number) => (value === null || value === undefined ? '-' : Number(value).toFixed(2));
const scoreSlot = (fieldKey?: string) => Number(String(fieldKey || '').replace('score', ''));
const isScoreColumn = (fieldKey?: string) => /^score\d+$/.test(String(fieldKey || ''));
const scoreAt = (row: ReviewScoreSummaryVO, slot: number) => row.scoreItems?.find((item) => Number(item.slot) === slot)?.scoreValue;
const warningTagType = (status?: string) => (status === 'warning' ? 'danger' : status === 'normal' ? 'success' : 'warning');
const isExpandableSummaryColumn = (column: ReviewScoreSummaryColumnVO) => EXPANDABLE_SUMMARY_COLUMN_KEYS.has(column.fieldKey || '');
const configuredColumnWidth = (column: ReviewScoreSummaryColumnVO) => Number(column.width || DEFAULT_COLUMN_WIDTHS[column.fieldKey || ''] || 120);
const summaryColumnWidth = (column: ReviewScoreSummaryColumnVO) => (isExpandableSummaryColumn(column) ? undefined : configuredColumnWidth(column));
const summaryColumnMinWidth = (column: ReviewScoreSummaryColumnVO) => (isExpandableSummaryColumn(column) ? configuredColumnWidth(column) : undefined);
const displayColumnValue = (row: ReviewScoreSummaryVO, fieldKey?: string) => {
  if (!fieldKey) return '-';
  const value = (row as any)[fieldKey];
  if (fieldKey === 'rowNo') return value || '-';
  return value === null || value === undefined || value === '' ? '-' : value;
};

const loadActivityOptions = async () => {
  const { data } = await listActivityOptions();
  activityOptions.value = data || [];
  if (!queryParams.activityId && activityOptions.value.length === 1) {
    queryParams.activityId = activityOptions.value[0].id;
    await loadCategoryOptions();
  }
};

const loadCategoryOptions = async () => {
  queryParams.categoryId = undefined;
  categoryOptions.value = [];
  queryParams.programForm = undefined;
  queryParams.groupOrNature = undefined;
  if (queryParams.activityId) {
    const { data } = await listCategoryOptions(queryParams.activityId);
    categoryOptions.value = data || [];
  }
};

const loadFilterOptions = async () => {
  try {
    const { data } = (await listReviewScoreSummaryFilterOptions(queryParams.activityId, queryParams.categoryId)) as {
      data?: ReviewScoreSummaryFilterOptionsVO;
    };
    programFormOptions.value = data?.programForms || [];
    groupOrNatureOptions.value = data?.groupOrNatures || [];
  } catch {
    programFormOptions.value = [];
    groupOrNatureOptions.value = [];
  }
  if (queryParams.programForm && !programFormOptions.value.includes(queryParams.programForm)) queryParams.programForm = undefined;
  if (queryParams.groupOrNature && !groupOrNatureOptions.value.includes(queryParams.groupOrNature)) queryParams.groupOrNature = undefined;
};

const loadSchoolOptions = async () => {
  const res: any = await listAuditSchoolOptions({ status: 'enabled' });
  schoolOptions.value = res.rows || res.data || [];
};

const loadRows = async () => {
  loading.value = true;
  try {
    const res: any = await listReviewScoreSummary(queryParams);
    rows.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const loadConfig = async () => {
  if (!queryParams.activityId || !queryParams.categoryId) {
    Object.assign(configForm, { columns: cloneColumns(), scoreColumnCount: 3, warning: cloneWarning() });
    return;
  }
  try {
    const { data } = await getReviewScoreSummaryConfig(queryParams.activityId, queryParams.categoryId);
    let columns = cloneColumns();
    if (data?.columnsJson) {
      try {
        const parsed = JSON.parse(data.columnsJson);
        if (Array.isArray(parsed) && parsed.length) columns = parsed;
      } catch {
        columns = cloneColumns();
      }
    }
    Object.assign(configForm, {
      ...data,
      columns,
      scoreColumnCount: data?.scoreColumnCount || 3,
      warning: {
        ...cloneWarning(),
        enabled: data?.enabled ?? DEFAULT_WARNING.enabled,
        formula: data?.formulaType || DEFAULT_WARNING.formula,
        formulaExpression: data?.formulaExpression || DEFAULT_WARNING.formulaExpression,
        thresholdPercent: data?.thresholdPercent ?? DEFAULT_WARNING.thresholdPercent,
        normalText: data?.normalText || DEFAULT_WARNING.normalText,
        warningText: data?.warningText || DEFAULT_WARNING.warningText,
        insufficientText: data?.insufficientText || DEFAULT_WARNING.insufficientText,
        unsupportedText: data?.unsupportedText || DEFAULT_WARNING.unsupportedText
      }
    });
  } catch {
    Object.assign(configForm, { columns: cloneColumns(), scoreColumnCount: 3, warning: cloneWarning() });
  }
};

const handleActivityChange = async () => {
  await loadCategoryOptions();
  await loadFilterOptions();
  await loadConfig();
  await loadRows();
};

const handleCategoryChange = async () => {
  queryParams.programForm = undefined;
  queryParams.groupOrNature = undefined;
  await loadFilterOptions();
  await loadConfig();
  await loadRows();
};

const handleSearch = async () => {
  queryParams.pageNum = 1;
  await loadRows();
};

const resetQuery = async () => {
  Object.assign(queryParams, {
    pageNum: 1,
    categoryId: undefined,
    schoolId: undefined,
    projectName: '',
    programForm: undefined,
    groupOrNature: undefined,
    averageSort: 'desc'
  });
  await loadFilterOptions();
  await loadConfig();
  await loadRows();
};

const openDetail = async (row: ReviewScoreSummaryVO) => {
  if (!row.projectId) return;
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    const { data } = await getReviewScoreSummaryDetail(row.projectId, { activityId: row.activityId, categoryId: row.categoryId });
    currentDetail.value = data;
  } finally {
    detailLoading.value = false;
  }
};

const openConfig = async () => {
  if (!queryParams.activityId || !queryParams.categoryId) {
    proxy?.$modal.msgWarning('请先选择活动和类别');
    return;
  }
  await loadConfig();
  configVisible.value = true;
};

const saveConfig = async () => {
  configSaving.value = true;
  try {
    await saveReviewScoreSummaryConfig({
      ...configForm,
      activityId: queryParams.activityId,
      categoryId: queryParams.categoryId,
      columns: configForm.columns?.map((column) => ({ ...column }))
    });
    proxy?.$modal.msgSuccess('分数汇总配置已保存');
    configVisible.value = false;
    await loadRows();
  } finally {
    configSaving.value = false;
  }
};

const exportSummary = (scope: 'all' | 'current') => {
  const suffix = scope === 'all' ? '全部' : '当前页';
  proxy?.download(
    '/crehn/result/score-summary/export',
    {
      ...queryParams,
      scope,
      columnsJson: JSON.stringify(configForm.columns || [])
    },
    `分数汇总_${suffix}_${new Date().getTime()}.xlsx`
  );
};

onMounted(async () => {
  await Promise.all([loadActivityOptions(), loadSchoolOptions()]);
  await loadFilterOptions();
  await loadConfig();
  await loadRows();
});
</script>

<style scoped>
.score-summary-table {
  width: 100%;
}

.score-summary-notice {
  color: #64748b;
  border-color: #e8eef6;
}

.detail-project-content,
.attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.muted-text {
  color: var(--el-text-color-secondary);
}

.signature-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.signature-image {
  width: 96px;
  height: 42px;
}

.column-config-list {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 8px;
}

.column-config-row {
  display: grid;
  grid-template-columns: 70px minmax(160px, 1fr) 110px 110px minmax(170px, 0.8fr);
  align-items: center;
  gap: 8px;
}

.config-field-key {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.formula-help {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
  margin-top: 4px;
}
</style>
