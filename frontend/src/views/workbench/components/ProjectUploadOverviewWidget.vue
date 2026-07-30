<template>
  <section class="workbench-card upload-overview-widget">
    <div class="workbench-card__head upload-overview-head">
      <div>
        <span>SUBMISSION</span>
        <h2>{{ title }}</h2>
      </div>
      <div class="upload-overview-tools">
        <el-select
          v-if="activityOptions.length"
          v-model="activityId"
          filterable
          placeholder="选择活动"
          class="upload-overview-activity"
          @change="loadData"
        >
          <el-option v-for="item in activityOptions" :key="item.id" :label="item.activityName" :value="item.id" />
        </el-select>
        <el-button icon="Refresh" text :loading="loading" @click="loadData" />
      </div>
    </div>

    <el-skeleton v-if="loading && !loaded" :rows="6" animated />
    <template v-else>
      <div class="upload-overview-metrics">
        <div v-for="item in summaryMetrics" :key="item.label" class="metric-item upload-overview-metric">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>

      <div class="upload-overview-section">
        <div class="upload-overview-section__title">
          <h3>大类别总览</h3>
          <span>{{ overview.activityName || '暂无活动' }}</span>
        </div>
        <div v-if="groupItems.length" class="upload-overview-grid">
          <article v-for="item in groupItems" :key="item.id || item.groupKey" class="upload-overview-tile">
            <header>
              <strong>{{ displayName(item) }}</strong>
              <span>{{ numberOf(item.totalCount) }}</span>
            </header>
            <div class="upload-overview-statuses">
              <span>总上传 {{ numberOf(item.totalCount) }}</span>
              <span>未提交 {{ numberOf(item.draftCount) }}</span>
              <span>待审核 {{ numberOf(item.submittedCount) }}</span>
              <span>已审核 {{ numberOf(item.auditedCount) }}</span>
              <span>已退回 {{ numberOf(item.returnedCount) }}</span>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无大类别统计" />
      </div>

      <div class="upload-overview-section">
        <div class="upload-overview-section__title">
          <h3>小类别明细</h3>
          <span>按菜单二级分类汇总</span>
        </div>
        <el-table :data="categoryItems" border :max-height="420" empty-text="暂无小类别统计">
          <el-table-column label="大类别" prop="groupName" min-width="160" show-overflow-tooltip />
          <el-table-column label="小类别" prop="categoryName" min-width="180" show-overflow-tooltip />
          <el-table-column label="总上传" prop="totalCount" width="100" align="center">
            <template #default="{ row }">{{ numberOf(row.totalCount) }}</template>
          </el-table-column>
          <el-table-column label="未提交" prop="draftCount" width="100" align="center">
            <template #default="{ row }">{{ numberOf(row.draftCount) }}</template>
          </el-table-column>
          <el-table-column label="待审核" prop="submittedCount" width="100" align="center">
            <template #default="{ row }">{{ numberOf(row.submittedCount) }}</template>
          </el-table-column>
          <el-table-column label="已审核" prop="auditedCount" width="100" align="center">
            <template #default="{ row }">{{ numberOf(row.auditedCount) }}</template>
          </el-table-column>
          <el-table-column label="已退回" prop="returnedCount" width="100" align="center">
            <template #default="{ row }">{{ numberOf(row.returnedCount) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </template>
  </section>
</template>

<script setup lang="ts">
import { getUploadOverview } from '@/api/crehn/result';
import { ProjectUploadOverviewCategoryVO, ProjectUploadOverviewVO } from '@/api/crehn/types';

defineProps<{ title: string }>();

const loading = ref(false);
const loaded = ref(false);
const activityId = ref<string | number>();
const overview = ref<ProjectUploadOverviewVO>({});

const activityOptions = computed(() => overview.value.activities || []);
const groupItems = computed(() => overview.value.groupItems || []);
const categoryItems = computed(() => overview.value.categoryItems || []);
const summary = computed(() => overview.value.summary || {});

const numberOf = (value?: number) => Number(value || 0);
const displayName = (item: ProjectUploadOverviewCategoryVO) => item.categoryName || item.groupName || '未分类';

const summaryMetrics = computed(() => [
  { label: '总上传', value: numberOf(summary.value.totalCount) },
  { label: '未提交', value: numberOf(summary.value.draftCount) },
  { label: '待审核', value: numberOf(summary.value.submittedCount) },
  { label: '已审核', value: numberOf(summary.value.auditedCount) },
  { label: '已退回', value: numberOf(summary.value.returnedCount) }
]);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await getUploadOverview({ activityId: activityId.value });
    overview.value = res.data || {};
    if (overview.value.activityId && overview.value.activityId !== activityId.value) {
      activityId.value = overview.value.activityId;
    }
    loaded.value = true;
  } finally {
    loading.value = false;
  }
};

onMounted(loadData);
</script>

<style scoped lang="scss">
.upload-overview-widget {
  overflow: hidden;
}

.upload-overview-head {
  align-items: center;
}

.upload-overview-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-overview-activity {
  width: 300px;
}

.upload-overview-metrics {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.upload-overview-metric {
  min-height: 82px;
}

.upload-overview-section {
  margin-top: 22px;
}

.upload-overview-section__title {
  margin-bottom: 12px;
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}

.upload-overview-section__title h3 {
  margin: 0;
  color: #082f49;
  font-size: 18px;
  line-height: 1.35;
  font-weight: 900;
}

.upload-overview-section__title span {
  color: #606f7b;
  font-size: 13px;
}

.upload-overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.upload-overview-tile {
  min-height: 124px;
  padding: 16px;
  border: 1px solid #d9e8f6;
  border-radius: 8px;
  background: #f8fbff;
}

.upload-overview-tile header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.upload-overview-tile strong {
  color: #102a43;
  font-size: 16px;
  line-height: 1.35;
}

.upload-overview-tile header span {
  color: #005ea8;
  font-size: 26px;
  line-height: 1;
  font-weight: 950;
}

.upload-overview-statuses {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
  color: #375a7f;
  font-size: 14px;
  line-height: 1.4;
}

@media (max-width: 1200px) {
  .upload-overview-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .upload-overview-head,
  .upload-overview-section__title {
    align-items: stretch;
    flex-direction: column;
  }

  .upload-overview-tools {
    width: 100%;
  }

  .upload-overview-activity {
    width: 100%;
  }

  .upload-overview-metrics,
  .upload-overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
