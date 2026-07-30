<template>
  <section class="workbench-card">
    <div class="workbench-card__head">
      <div>
        <span>SUMMARY</span>
        <h2>{{ title }}</h2>
      </div>
      <el-button icon="Refresh" text :loading="loading" @click="loadData" />
    </div>
    <div class="metric-grid">
      <div class="metric-item">
        <span>报送总数</span>
        <strong>{{ summary.totalCount || 0 }}</strong>
      </div>
      <div class="metric-item">
        <span>学校数</span>
        <strong>{{ summary.schoolCount || 0 }}</strong>
      </div>
      <div v-for="item in statusItems" :key="item.label" class="metric-item">
        <span>{{ item.label }}</span>
        <strong>{{ item.count || 0 }}</strong>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { getUploadSummary } from '@/api/crehn/result';

defineProps<{ title: string }>();

const loading = ref(false);
const summary = ref<any>({});
const statusItems = computed(() => (summary.value.statusItems || []).slice(0, 4));

const loadData = async () => {
  loading.value = true;
  try {
    const res: any = await getUploadSummary({});
    summary.value = res.data || {};
  } finally {
    loading.value = false;
  }
};

onMounted(loadData);
</script>
