<template>
  <section class="workbench-card">
    <div class="workbench-card__head">
      <div>
        <span>PENDING</span>
        <h2>{{ title }}</h2>
      </div>
      <el-button type="primary" text @click="$router.push('/crehn/audit')">查看全部</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" border empty-text="暂无待审核项目">
      <el-table-column label="项目名称" prop="projectName" min-width="180" show-overflow-tooltip />
      <el-table-column label="学校" prop="schoolName" min-width="130" show-overflow-tooltip />
      <el-table-column label="类别" prop="categoryName" min-width="130" show-overflow-tooltip />
      <el-table-column label="提交时间" prop="submittedAt" width="160" />
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { listAuditProject } from '@/api/crehn/audit';

defineProps<{ title: string }>();

const loading = ref(false);
const rows = ref<any[]>([]);

const loadData = async () => {
  loading.value = true;
  try {
    const res: any = await listAuditProject({ pageNum: 1, pageSize: 6, status: 'submitted' });
    rows.value = res.rows || res.data || [];
  } finally {
    loading.value = false;
  }
};

onMounted(loadData);
</script>
