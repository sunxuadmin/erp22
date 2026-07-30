<template>
  <section class="workbench-card quota-ratio-widget">
    <div class="workbench-card__head quota-ratio-head">
      <div>
        <span>RATIO</span>
        <h2>{{ title }}</h2>
      </div>
      <div class="quota-ratio-tools">
        <el-select
          v-if="activityOptions.length"
          v-model="activityId"
          filterable
          placeholder="选择活动"
          class="quota-ratio-activity"
          @change="loadData"
        >
          <el-option v-for="item in activityOptions" :key="item.id" :label="item.activityName" :value="item.id" />
        </el-select>
        <el-button icon="Refresh" text :loading="loading" @click="loadData" />
      </div>
    </div>

    <el-skeleton v-if="loading && !loaded" :rows="5" animated />
    <template v-else>
      <div class="quota-ratio-summary">
        <div class="quota-ratio-summary__item">
          <span>最终项目</span>
          <strong>{{ numberOf(summary.totalCount) }}</strong>
        </div>
        <div class="quota-ratio-summary__item" :class="{ 'is-danger': numberOf(summary.exceededCount) > 0 }">
          <span>预警规则</span>
          <strong>{{ numberOf(summary.exceededCount) }}</strong>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        border
        empty-text="暂无比例规则"
        :row-class-name="rowClassName"
        class="quota-ratio-table"
      >
        <el-table-column label="规则" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            <strong class="quota-ratio-rule-name">{{ ruleName(row) }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="范围" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ scopeText(row) }}</template>
        </el-table-column>
        <el-table-column label="当前比例" min-width="180">
          <template #default="{ row }">
            <div class="quota-ratio-progress">
              <el-progress :percentage="ratioPercent(row)" :status="progressStatus(row)" :stroke-width="10" />
              <span>{{ formatRatio(row.ratio) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="110" align="center">
          <template #default="{ row }">{{ numberOf(row.numerator) }} / {{ numberOf(row.denominator) }}</template>
        </el-table-column>
        <el-table-column label="要求" width="105" align="center">
          <template #default="{ row }">{{ requirementText(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.exceeded ? 'warning' : 'success'">{{ row.statusText || (row.exceeded ? '预警' : '达标') }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </section>
</template>

<script setup lang="ts">
import { getQuotaRatioSummary } from '@/api/crehn/result';
import { ProjectQuotaRatioItemVO, ProjectQuotaRatioSummaryVO } from '@/api/crehn/types';

defineProps<{ title: string }>();

const loading = ref(false);
const loaded = ref(false);
const activityId = ref<string | number>();
const summary = ref<ProjectQuotaRatioSummaryVO>({});

const activityOptions = computed(() => summary.value.activities || []);
const rows = computed(() => summary.value.items || []);

const numberOf = (value?: number) => Number(value || 0);
const formatRatio = (value?: number) => `${Number(value || 0).toFixed(2)}%`;
const ratioPercent = (row: ProjectQuotaRatioItemVO) => Math.min(100, Math.max(0, Number(row.ratio || 0)));
const progressStatus = (row: ProjectQuotaRatioItemVO): 'exception' | undefined => (row.exceeded ? 'exception' : undefined);
const ruleName = (row: ProjectQuotaRatioItemVO) => row.groupName || row.groupCode || row.remark || '未命名规则';
const scopeText = (row: ProjectQuotaRatioItemVO) => row.categoryName || row.scopeCategoryGroup || row.scopeCategoryCodes || '当前活动';
const requirementText = (row: ProjectQuotaRatioItemVO) => `${row.operator === 'min' ? '≥' : '≤'} ${Number(row.ratioValue || 0)}%`;

const rowClassName = ({ row }: { row: ProjectQuotaRatioItemVO }) => (row.exceeded ? 'quota-ratio-row--danger' : '');

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getQuotaRatioSummary({ activityId: activityId.value });
    summary.value = res.data || {};
    if (summary.value.activityId && summary.value.activityId !== activityId.value) {
      activityId.value = summary.value.activityId;
    }
    loaded.value = true;
  } finally {
    loading.value = false;
  }
};

onMounted(loadData);
</script>

<style scoped lang="scss">
.quota-ratio-widget {
  overflow: hidden;
}

.quota-ratio-head {
  align-items: center;
}

.quota-ratio-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quota-ratio-activity {
  width: 260px;
}

.quota-ratio-summary {
  margin-bottom: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.quota-ratio-summary__item {
  min-height: 72px;
  padding: 14px 16px;
  display: grid;
  align-content: center;
  gap: 8px;
  border: 1px solid #e5edf5;
  border-radius: 8px;
  background: #f8fbff;
}

.quota-ratio-summary__item span {
  color: #606f7b;
  font-size: 13px;
}

.quota-ratio-summary__item strong {
  color: #102a43;
  font-size: 26px;
  line-height: 1;
}

.quota-ratio-summary__item.is-danger {
  border-color: #f8d58c;
  background: #fffaf0;
}

.quota-ratio-summary__item.is-danger strong {
  color: #b45309;
}

.quota-ratio-table {
  width: 100%;
}

.quota-ratio-rule-name {
  color: #102a43;
}

.quota-ratio-progress {
  display: grid;
  grid-template-columns: minmax(80px, 1fr) 58px;
  align-items: center;
  gap: 8px;
}

.quota-ratio-progress span {
  color: #375a7f;
  font-weight: 800;
  text-align: right;
}

:deep(.quota-ratio-row--danger td.el-table__cell) {
  background: #fffaf0;
}

:deep(.quota-ratio-row--danger .quota-ratio-rule-name),
:deep(.quota-ratio-row--danger .quota-ratio-progress span) {
  color: #b45309;
}

@media (max-width: 900px) {
  .quota-ratio-head {
    align-items: stretch;
    flex-direction: column;
  }

  .quota-ratio-tools,
  .quota-ratio-activity {
    width: 100%;
  }

  .quota-ratio-summary {
    grid-template-columns: 1fr;
  }
}
</style>
