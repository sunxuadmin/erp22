<template>
  <section class="workbench-card">
    <div class="workbench-card__head">
      <div>
        <span>CATEGORY</span>
        <h2>{{ title }}</h2>
      </div>
      <el-button icon="Refresh" text :loading="loading" @click="loadData" />
    </div>
    <el-table v-loading="loading" :data="rows" border empty-text="暂无统计数据">
      <el-table-column label="类别" prop="label" min-width="160" show-overflow-tooltip />
      <el-table-column label="数量" prop="count" width="100" align="center" />
      <el-table-column label="占比" prop="percent" width="100" align="center">
        <template #default="{ row }">{{ row.percent || 0 }}%</template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { getUploadSummary } from '@/api/crehn/result';

defineProps<{ title: string }>();

const loading = ref(false);
const rows = ref<any[]>([]);

const loadData = async () => {
  loading.value = true;
  try {
    const res: any = await getUploadSummary({});
    rows.value = (res.data?.categoryItems || []).slice(0, 8);
  } finally {
    loading.value = false;
  }
};

onMounted(loadData);
</script>
