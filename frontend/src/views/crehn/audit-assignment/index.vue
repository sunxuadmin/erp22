<template>
  <div class="p-2 audit-assignment-page">
    <el-card shadow="hover" class="mb-[10px]">
      <el-form :model="queryParams" :inline="true">
        <el-form-item label="活动">
          <el-select
            v-model="queryParams.activityId"
            clearable
            filterable
            placeholder="全部活动"
            style="width: 240px"
            @change="handleQueryActivityChange"
          >
            <el-option v-for="item in activityOptions" :key="item.id" :label="item.activityName" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-select
            v-model="queryParams.categoryId"
            clearable
            filterable
            placeholder="全部类别"
            style="width: 190px"
            :disabled="!queryParams.activityId"
          >
            <el-option v-for="item in queryCategoryOptions" :key="item.id" :label="item.categoryName" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 130px">
            <el-option label="启用" value="active" />
            <el-option label="停用" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button v-hasPermi="['crehn:auditScope:add']" type="success" icon="Operation" @click="openForm()">批量维护</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <el-alert
        class="mb-2"
        type="info"
        :closable="false"
        show-icon
        title="按活动合并维护审核权限：可向所选类别添加人员、移除人员，或把所选人员的类别同步为当前选择。移除只停用授权，不删除历史审核记录。"
      />
      <el-table v-loading="loading" border :data="assignmentList">
        <el-table-column label="活动" prop="activityName" min-width="180" show-overflow-tooltip />
        <el-table-column label="类别" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.categoryName || '全部类别' }}</template>
        </el-table-column>
        <el-table-column label="审核账号" min-width="150">
          <template #default="{ row }">{{ auditorLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="学校范围" min-width="170">
          <template #default="{ row }">
            <el-tag size="small" :type="schoolScopeType(row.schoolScopeMode)">{{ schoolScopeLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限" min-width="260">
          <template #default="{ row }">
            <el-tag v-if="row.allowQuery" class="mr-1" size="small">查看</el-tag>
            <el-tag v-if="row.allowPass" class="mr-1" size="small" type="success">通过</el-tag>
            <el-tag v-if="row.allowReturn" class="mr-1" size="small" type="danger">退回</el-tag>
            <el-tag v-if="row.allowWithdrawPass" class="mr-1" size="small" type="warning">撤销通过</el-tag>
            <el-tag v-if="row.allowWithdrawReturn" class="mr-1" size="small" type="warning">撤销退回</el-tag>
            <el-tag v-if="row.allowDownload" size="small" type="info">下载/预览</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'">{{ row.status === 'active' ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-hasPermi="['crehn:auditScope:edit']" link type="primary" icon="Edit" @click="openForm(row)">编辑</el-button>
            <el-button v-hasPermi="['crehn:auditScope:remove']" link type="danger" icon="CircleClose" @click="handleDelete(row)">停用</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <ArtPopupDialog v-model="formVisible" :title="form.id ? '编辑审核权限' : '合并批量维护审核权限'" width="800px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item v-if="!form.id" label="维护方式">
          <el-radio-group v-model="form.batchOperation">
            <el-radio-button label="merge">添加/更新</el-radio-button>
            <el-radio-button label="remove">移除</el-radio-button>
            <el-radio-button label="sync">同步类别</el-radio-button>
          </el-radio-group>
          <div class="batch-operation-tip">{{ batchOperationTip }}</div>
        </el-form-item>
        <el-form-item label="活动" prop="activityId">
          <el-select v-model="form.activityId" filterable placeholder="请选择活动" style="width: 100%" @change="handleFormActivityChange">
            <el-option v-for="item in activityOptions" :key="item.id" :label="item.activityName" :value="item.id!" />
          </el-select>
        </el-form-item>
        <AssignmentScopeSelector
          v-model:category-ids="form.categoryIds"
          v-model:school-scope-mode="form.schoolScopeMode"
          v-model:school-ids="selectedSchoolIds"
          :category-options="formCategoryOptions"
          :school-options="schoolOptions"
          :category-multiple-limit="form.id ? 1 : 0"
          :disabled="!form.activityId"
          :show-school-scope="Boolean(form.id) || form.batchOperation !== 'remove'"
        />
        <el-form-item v-if="form.id" label="审核账号" prop="auditorUserId">
          <el-select
            v-model="form.auditorUserId"
            filterable
            remote
            reserve-keyword
            placeholder="输入账号或姓名搜索"
            :remote-method="loadAuditorOptions"
            :loading="auditorLoading"
            style="width: 100%"
          >
            <el-option v-for="item in auditorOptions" :key="item.userId" :label="auditorOptionLabel(item)" :value="item.userId" />
          </el-select>
        </el-form-item>
        <el-form-item v-else label="审核账号" prop="auditorUserIds">
          <el-select
            v-model="form.auditorUserIds"
            multiple
            filterable
            remote
            reserve-keyword
            collapse-tags
            collapse-tags-tooltip
            placeholder="可批量选择审核账号，支持输入账号或姓名搜索"
            :remote-method="loadAuditorOptions"
            :loading="auditorLoading"
            style="width: 100%"
          >
            <el-option v-for="item in auditorOptions" :key="item.userId" :label="auditorOptionLabel(item)" :value="item.userId" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.id || form.batchOperation !== 'remove'" label="授权权限">
          <el-checkbox v-model="form.allowQuery">查看作品</el-checkbox>
          <el-checkbox v-model="form.allowPass">通过</el-checkbox>
          <el-checkbox v-model="form.allowReturn">退回</el-checkbox>
          <el-checkbox v-model="form.allowWithdrawPass">撤销通过</el-checkbox>
          <el-checkbox v-model="form.allowWithdrawReturn">撤销退回</el-checkbox>
          <el-checkbox v-model="form.allowDownload">下载/预览附件</el-checkbox>
        </el-form-item>
        <el-form-item v-if="form.id || form.batchOperation !== 'remove'" label="快速设置">
          <el-button type="primary" plain @click="applyFullAudit">完整审核权限</el-button>
          <el-button plain @click="applyReadonly">仅查看</el-button>
        </el-form-item>
        <el-form-item v-if="form.id" label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="active">启用</el-radio>
            <el-radio label="disabled">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">保存</el-button>
        <el-button @click="formVisible = false">取消</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ArtAuditAssignment" lang="ts">
import { listActivityOptions, listCategoryOptions } from '@/api/crehn/activity';
import {
  deleteAuditAssignment,
  listAuditAssignment,
  listAuditAssignmentSchoolOptions,
  listAuditorOptions,
  maintainAuditAssignmentBatch,
  previewAuditAssignmentBatch,
  updateAuditAssignment
} from '@/api/crehn/audit';
import { ActivityCategoryVO, ActivityVO, AssignmentBatchPreviewVO, AssignmentSchoolOptionVO, AuditAssignmentVO } from '@/api/crehn/types';
import AssignmentScopeSelector from '@/views/crehn/components/AssignmentScopeSelector.vue';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const loading = ref(false);
const total = ref(0);
const assignmentList = ref<AuditAssignmentVO[]>([]);
const activityOptions = ref<ActivityVO[]>([]);
const queryCategoryOptions = ref<ActivityCategoryVO[]>([]);
const formCategoryOptions = ref<ActivityCategoryVO[]>([]);
const auditorOptions = ref<any[]>([]);
const auditorLoading = ref(false);
const schoolOptions = ref<AssignmentSchoolOptionVO[]>([]);
const selectedSchoolIds = ref<Array<string | number>>([]);
const formVisible = ref(false);
const formRef = ref<ElFormInstance>();

const queryParams = reactive<any>({ pageNum: 1, pageSize: 10, activityId: undefined, categoryId: undefined, status: 'active' });
const form = reactive<AuditAssignmentVO>({
  categoryIds: [],
  auditorUserIds: [],
  batchOperation: 'merge',
  batchTarget: 'assignment',
  schoolScopeMode: 'all',
  schoolIdsJson: '',
  allowQuery: true,
  allowPass: true,
  allowReturn: true,
  allowWithdrawPass: false,
  allowWithdrawReturn: true,
  allowDownload: true,
  status: 'active'
});

const rules: ElFormRules = {
  activityId: [{ required: true, message: '请选择活动', trigger: 'change' }],
  categoryIds: [{ type: 'array', required: true, min: 1, message: '请至少选择一个类别', trigger: 'change' }],
  auditorUserId: [{ required: true, message: '请选择审核账号', trigger: 'change' }],
  auditorUserIds: [{ type: 'array', required: true, min: 1, message: '请至少选择一个审核账号', trigger: 'change' }]
};

const loadActivityOptions = async () => {
  const { data } = await listActivityOptions();
  activityOptions.value = data || [];
};

const loadSchoolOptions = async () => {
  const { data } = await listAuditAssignmentSchoolOptions();
  schoolOptions.value = data || [];
};

const handleQueryActivityChange = async () => {
  queryParams.categoryId = undefined;
  queryCategoryOptions.value = [];
  if (queryParams.activityId) {
    const { data } = await listCategoryOptions(queryParams.activityId);
    queryCategoryOptions.value = data || [];
  }
};

const handleFormActivityChange = async () => {
  form.categoryId = undefined;
  form.categoryIds = [];
  formCategoryOptions.value = [];
  if (form.activityId) {
    const { data } = await listCategoryOptions(form.activityId);
    formCategoryOptions.value = data || [];
  }
};

const loadAuditorOptions = async (keyword?: string) => {
  auditorLoading.value = true;
  try {
    const { data } = await listAuditorOptions(keyword || '');
    auditorOptions.value = data || [];
  } finally {
    auditorLoading.value = false;
  }
};

const getList = async () => {
  loading.value = true;
  try {
    const res: any = await listAuditAssignment(queryParams);
    assignmentList.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const resetQuery = async () => {
  Object.assign(queryParams, { pageNum: 1, activityId: undefined, categoryId: undefined, status: 'active' });
  queryCategoryOptions.value = [];
  await getList();
};

const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    activityId: undefined,
    categoryId: undefined,
    categoryIds: [],
    auditorUserId: undefined,
    auditorUserIds: [],
    batchOperation: 'merge',
    batchTarget: 'assignment',
    schoolScopeMode: 'all',
    schoolIdsJson: '',
    allowQuery: true,
    allowPass: true,
    allowReturn: true,
    allowWithdrawPass: false,
    allowWithdrawReturn: true,
    allowDownload: true,
    status: 'active'
  });
  formCategoryOptions.value = [];
  selectedSchoolIds.value = [];
};

const openForm = async (row?: AuditAssignmentVO) => {
  resetForm();
  await loadAuditorOptions('');
  if (row) {
    Object.assign(form, row);
    form.categoryIds = row.categoryId ? [row.categoryId] : [];
    form.auditorUserIds = [];
    form.schoolScopeMode = row.schoolScopeMode || 'all';
    selectedSchoolIds.value = parseIdArray(row.schoolIdsJson);
    if (form.activityId) {
      const { data } = await listCategoryOptions(form.activityId);
      formCategoryOptions.value = data || [];
    }
  }
  formVisible.value = true;
};

const applyFullAudit = () => {
  Object.assign(form, {
    allowQuery: true,
    allowPass: true,
    allowReturn: true,
    allowWithdrawPass: true,
    allowWithdrawReturn: true,
    allowDownload: true
  });
};

const applyReadonly = () => {
  Object.assign(form, {
    allowQuery: true,
    allowPass: false,
    allowReturn: false,
    allowWithdrawPass: false,
    allowWithdrawReturn: false,
    allowDownload: false
  });
};

const submitForm = async () => {
  await formRef.value?.validate();
  if ((form.id || form.batchOperation !== 'remove') && form.schoolScopeMode !== 'all' && !selectedSchoolIds.value.length) {
    proxy?.$modal.msgError('学校白名单或黑名单至少选择一所学校');
    return;
  }
  form.schoolIdsJson = form.schoolScopeMode === 'all' ? '' : JSON.stringify(selectedSchoolIds.value);
  if (form.id) {
    form.categoryId = form.categoryIds?.[0];
    await updateAuditAssignment(form);
  } else {
    form.categoryId = undefined;
    form.auditorUserId = undefined;
    form.batchTarget = 'assignment';
    const { data: preview } = await previewAuditAssignmentBatch(form);
    await proxy?.$modal.confirm(batchPreviewText(preview));
    await maintainAuditAssignmentBatch(form);
  }
  proxy?.$modal.msgSuccess('保存成功');
  formVisible.value = false;
  await getList();
};

const handleDelete = async (row: AuditAssignmentVO) => {
  await proxy?.$modal.confirm(`确认停用审核授权：${auditorLabel(row)}？历史审核记录不会删除。`);
  await deleteAuditAssignment(row.id!);
  proxy?.$modal.msgSuccess('已停用');
  await getList();
};

const auditorOptionLabel = (item: any) => {
  const name = item.nickName || item.userName || item.userId || '-';
  return item.userName ? `${name}（${item.userName}）` : String(name);
};

const auditorLabel = (row: AuditAssignmentVO) => {
  const name = row.auditorNickName || row.auditorUserName || row.auditorUserId || '-';
  return row.auditorUserName ? `${name}（${row.auditorUserName}）` : String(name);
};

const parseIdArray = (text?: string) => {
  try {
    const parsed = text ? JSON.parse(text) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

const schoolScopeLabel = (row: AuditAssignmentVO) => {
  if (row.schoolScopeMode === 'whitelist') return `白名单 ${parseIdArray(row.schoolIdsJson).length} 所`;
  if (row.schoolScopeMode === 'blacklist') return `黑名单 ${parseIdArray(row.schoolIdsJson).length} 所`;
  return '全部学校';
};

const schoolScopeType = (mode?: string) => (mode === 'whitelist' ? 'success' : mode === 'blacklist' ? 'warning' : 'info');
const batchOperationTip = computed(
  () =>
    ({
      merge: '为所选人员添加所选类别；已有相同人员和类别时更新学校范围及审核动作，并恢复已停用授权。',
      remove: '仅停用所选人员在所选类别的审核授权，不删除账号和历史审核记录。',
      sync: '所选人员在该活动中的有效类别将与当前选择完全一致：补充所选类别，停用未选择类别。'
    })[form.batchOperation || 'merge']
);
const batchPreviewText = (preview?: AssignmentBatchPreviewVO) =>
  `执行预览：新增 ${preview?.createCount || 0} 条，更新 ${preview?.updateCount || 0} 条，停用 ${preview?.disableCount || 0} 条，不变 ${
    preview?.unchangedCount || 0
  } 条。确认继续？`;

onMounted(async () => {
  await Promise.all([loadActivityOptions(), loadSchoolOptions()]);
  await getList();
});
</script>

<style scoped>
.batch-operation-tip {
  width: 100%;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}
</style>
