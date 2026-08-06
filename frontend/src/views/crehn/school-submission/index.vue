<template>
  <div class="p-2">
    <el-card class="mb-2" shadow="never">
      <el-form inline>
        <el-form-item label="活动">
          <el-select v-model="activityId" clearable filterable placeholder="全部可用活动" style="width: 260px">
            <el-option v-for="activity in activityOptions" :key="String(activity.id)" :label="activity.activityName" :value="String(activity.id)" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="load">查询</el-button>
          <el-button v-hasPermi="['crehn:schoolSubmission:submit']" type="success" icon="Promotion" :disabled="!selected.length" @click="finalSubmit">
            最终提交所选推荐作品
          </el-button>
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false" title="流程：参赛者本人提交 → 学校审核推荐/退回 → 学校按活动最终提交。最终提交会生成不可变快照。" />
    </el-card>
    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" border @selection-change="selected = $event">
        <el-table-column type="selection" width="48" :selectable="(row) => row.status === 'school_approved'" />
        <el-table-column label="作品编号" prop="projectNo" min-width="130" />
        <el-table-column label="作品名称" prop="projectName" min-width="220" show-overflow-tooltip />
        <el-table-column label="类别" prop="categoryName" min-width="150" />
        <el-table-column label="参赛者提交时间" prop="participantSubmittedAt" min-width="170" />
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核意见" prop="currentAuditOpinion" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'participant_submitted'">
              <el-button v-hasPermi="['crehn:schoolSubmission:review']" link type="success" @click="review(row, 'pass')"> 推荐 </el-button>
              <el-button v-hasPermi="['crehn:schoolSubmission:review']" link type="danger" @click="openReturn(row)"> 退回 </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="returnVisible" title="退回参赛者修改" width="500px">
      <el-input v-model="returnOpinion" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="必须填写退回原因" />
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="danger" @click="submitReturn">确认退回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { ProjectVO } from '@/api/crehn/types';
import type { ActivityVO } from '@/api/crehn/types';
import { listAvailableActivity } from '@/api/crehn/activity';
import { finalSubmitSchoolProjects, listSchoolSubmission, reviewSchoolProject } from '@/api/crehn/schoolSubmission';

const activityId = ref('');
const activityOptions = ref<ActivityVO[]>([]);
const loading = ref(false);
const rows = ref<ProjectVO[]>([]);
const selected = ref<ProjectVO[]>([]);
const returnVisible = ref(false);
const returnOpinion = ref('');
const currentProject = ref<ProjectVO>();

const statusLabel = (status?: string) =>
  ({
    participant_submitted: '待学校审核',
    school_approved: '学校已推荐',
    participant_returned: '已退回参赛者'
  })[status || ''] ||
  status ||
  '-';

const statusType = (status?: string) => (status === 'school_approved' ? 'success' : status === 'participant_returned' ? 'danger' : 'warning');

const load = async () => {
  loading.value = true;
  try {
    const response: any = await listSchoolSubmission(activityId.value || undefined);
    rows.value = response.data || [];
    selected.value = [];
  } finally {
    loading.value = false;
  }
};

const review = async (row: ProjectVO, result: 'pass' | 'return', opinion?: string) => {
  await reviewSchoolProject({ projectId: row.id!, result, opinion });
  ElMessage.success(result === 'pass' ? '已推荐该作品' : '已退回参赛者修改');
  await load();
};

const openReturn = (row: ProjectVO) => {
  currentProject.value = row;
  returnOpinion.value = '';
  returnVisible.value = true;
};

const submitReturn = async () => {
  if (!returnOpinion.value.trim()) {
    ElMessage.warning('退回原因不能为空');
    return;
  }
  await review(currentProject.value!, 'return', returnOpinion.value.trim());
  returnVisible.value = false;
};

const finalSubmit = async () => {
  const activityIds = new Set(selected.value.map((item) => String(item.activityId)));
  if (activityIds.size !== 1) {
    ElMessage.warning('一次最终提交只能选择同一活动作品');
    return;
  }
  await ElMessageBox.confirm(`确认最终提交所选 ${selected.value.length} 项作品？提交后将形成不可变快照。`, '学校最终提交', { type: 'warning' });
  await finalSubmitSchoolProjects({
    activityId: selected.value[0].activityId!,
    projectIds: selected.value.map((item) => item.id!)
  });
  ElMessage.success('学校最终提交成功');
  await load();
};

onMounted(async () => {
  const response: any = await listAvailableActivity();
  activityOptions.value = response.data || [];
  await load();
});
</script>
