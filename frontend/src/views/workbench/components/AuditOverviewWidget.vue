<template>
  <section class="workbench-card">
    <div class="workbench-card__head">
      <div>
        <span>AUDIT</span>
        <h2>{{ title }}</h2>
      </div>
      <el-button icon="Refresh" text :loading="loading" @click="loadData" />
    </div>
    <div class="metric-grid">
      <div v-for="item in metrics" :key="item.label" class="metric-item">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { listAuditProject } from '@/api/crehn/audit';

defineProps<{ title: string }>();

const loading = ref(false);
const metrics = ref([
  { label: '待审核', value: 0, status: 'submitted' },
  { label: '已通过', value: 0, status: 'audit_passed' },
  { label: '已退回', value: 0, status: 'returned' },
  { label: '全部', value: 0, status: '' }
]);

const loadData = async () => {
  loading.value = true;
  try {
    const rows = await Promise.all(metrics.value.map((item) => listAuditProject({ pageNum: 1, pageSize: 1, status: item.status })));
    metrics.value = metrics.value.map((item, index) => ({ ...item, value: Number((rows[index] as any).total || 0) }));
  } finally {
    loading.value = false;
  }
};

onMounted(loadData);
</script>
