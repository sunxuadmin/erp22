<template>
  <div class="p-2">
    <el-card shadow="hover" class="mb-[10px]">
      <el-form :model="queryParams" :inline="true">
        <el-form-item label="注册码">
          <el-input v-model="queryParams.code" placeholder="请输入注册码" clearable @keyup.enter="getList" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryParams.codeType" clearable placeholder="全部类型">
            <el-option label="学校" value="school" />
            <el-option label="专家" value="expert" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable placeholder="全部状态">
            <el-option label="未使用" value="unused" />
            <el-option label="使用中" value="active" />
            <el-option label="已使用" value="used" />
            <el-option label="已作废" value="voided" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button v-hasPermi="['crehn:registration:add']" type="primary" plain icon="Plus" @click="openForm()">生成</el-button>
          <el-button v-hasPermi="['crehn:registration:add']" type="success" plain icon="Grid" @click="openBatch">批量生成</el-button>
          <el-button v-hasPermi="['crehn:registration:export']" type="warning" plain icon="Download" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <el-table v-loading="loading" border :data="codeList">
        <el-table-column label="注册码" prop="code" min-width="180" />
        <el-table-column label="类型" prop="codeType" width="90">
          <template #default="scope">{{ typeLabel(scope.row.codeType) }}</template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="100">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="学校" min-width="180">
          <template #default="scope">{{ schoolNameMap[scope.row.schoolId] || scope.row.schoolId || '-' }}</template>
        </el-table-column>
        <el-table-column label="手机号" prop="boundPhone" width="130" />
        <el-table-column label="角色" prop="roleKey" width="130" />
        <el-table-column label="自动审核" prop="autoApprove" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.autoApprove ? 'success' : 'info'">{{ scope.row.autoApprove ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="绑定活动" min-width="180">
          <template #default="scope">{{ activityNameMap[scope.row.activityId] || scope.row.activityId || '-' }}</template>
        </el-table-column>
        <el-table-column label="次数" width="90">
          <template #default="scope">{{ scope.row.usedCount || 0 }}/{{ scope.row.maxUseCount || 1 }}</template>
        </el-table-column>
        <el-table-column label="有效期" prop="expireAt" width="170" />
        <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="scope">
            <el-button v-hasPermi="['crehn:registration:edit']" link type="primary" icon="Edit" @click="openForm(scope.row)">绑定</el-button>
            <el-button v-hasPermi="['crehn:registration:resend']" link type="primary" icon="Refresh" @click="resend(scope.row)">重发</el-button>
            <el-button v-hasPermi="['crehn:registration:void']" link type="danger" icon="CircleClose" @click="voidCode(scope.row)">作废</el-button>
            <el-button v-hasPermi="['crehn:registration:usage']" link type="primary" icon="Document" @click="openUsage(scope.row)">记录</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <ArtPopupDialog v-model="formDialog.visible" :title="formDialog.title" width="680px" append-to-body>
      <el-form :model="form" label-width="110px">
        <el-form-item label="类型" required>
          <el-select v-model="form.codeType" class="w-full" :disabled="!!form.id">
            <el-option label="学校" value="school" />
            <el-option label="专家" value="expert" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.id" label="注册码"><el-input v-model="form.code" disabled /></el-form-item>
        <el-form-item label="绑定学校">
          <el-select v-model="form.schoolId" clearable filterable class="w-full" placeholder="选择学校名称">
            <el-option v-for="item in schoolOptions" :key="item.id" :label="schoolLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="绑定手机号"><el-input v-model="form.boundPhone" /></el-form-item>
        <el-form-item label="绑定角色">
          <div class="role-picker">
            <el-select v-model="form.roleKey" filterable class="w-full" placeholder="选择业务角色">
              <el-option v-for="item in roleOptions" :key="item.roleKey" :label="`${item.roleName}（${item.roleKey}）`" :value="item.roleKey" />
            </el-select>
            <el-button type="primary" plain icon="Plus" @click="openRoleDialog">新增</el-button>
          </div>
        </el-form-item>
        <el-form-item label="自动审核"><el-switch v-model="form.autoApprove" /></el-form-item>
        <el-form-item label="绑定活动">
          <el-select v-model="form.activityId" clearable filterable class="w-full" placeholder="选择活动简称/名称">
            <el-option v-for="item in activityOptions" :key="item.id" :label="activityLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大次数"><el-input-number v-model="form.maxUseCount" :min="1" class="w-full" /></el-form-item>
        <el-form-item label="有效期"><el-date-picker v-model="form.expireAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="w-full" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">保存</el-button>
        <el-button @click="formDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="batchDialog.visible" title="批量生成学校注册码" width="960px" append-to-body>
      <el-form :model="batchForm" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="绑定活动">
              <el-select v-model="batchForm.activityId" clearable filterable class="w-full" placeholder="活动简称/名称">
                <el-option v-for="item in activityOptions" :key="item.id" :label="activityLabel(item)" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="自动审核"><el-switch v-model="batchForm.autoApprove" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="绑定角色">
              <div class="role-picker">
                <el-select v-model="batchForm.roleKey" filterable class="w-full" placeholder="选择业务角色">
                  <el-option v-for="item in roleOptions" :key="item.roleKey" :label="`${item.roleName}（${item.roleKey}）`" :value="item.roleKey" />
                </el-select>
                <el-button type="primary" plain icon="Plus" @click="openRoleDialog">新增</el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="有效期"><el-date-picker v-model="batchForm.expireAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" class="w-full" /></el-form-item>
          </el-col>
        </el-row>
        <el-table border :data="batchRows">
          <el-table-column label="学校" min-width="260">
            <template #default="scope">
              <el-select v-model="scope.row.schoolId" clearable filterable placeholder="可选；不绑定则注册时选择/新增" class="w-full">
                <el-option v-for="item in schoolOptions" :key="item.id" :label="schoolLabel(item)" :value="item.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="绑定手机号" width="160">
            <template #default="scope"><el-input v-model="scope.row.boundPhone" /></template>
          </el-table-column>
          <el-table-column label="备注" min-width="180">
            <template #default="scope"><el-input v-model="scope.row.remark" /></template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="scope"><el-button link type="danger" icon="Minus" @click="removeBatchRow(scope.$index)">删除</el-button></template>
          </el-table-column>
        </el-table>
        <div class="mt-2">
          <el-button type="primary" plain icon="Plus" @click="addBatchRow">增加一行</el-button>
        </div>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitBatch">生成注册码</el-button>
        <el-button @click="batchDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="roleDialog.visible" title="快速新增业务角色" width="520px" append-to-body>
      <el-form :model="roleForm" label-width="110px">
        <el-form-item label="角色名称" required><el-input v-model="roleForm.roleName" placeholder="例如：学校申报员、公告管理员" /></el-form-item>
        <el-form-item label="角色类型">
          <el-select v-model="roleForm.roleType" class="w-full">
            <el-option label="学校" value="school" />
            <el-option label="专家" value="expert" />
            <el-option label="审核老师" value="auditor" />
            <el-option label="管理员" value="admin" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限字符"><el-input v-model="roleForm.roleKey" placeholder="可选，例如 art_notice_admin" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitRole">保存</el-button>
        <el-button @click="roleDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="usageDialog.visible" title="注册码使用记录" width="760px" append-to-body>
      <el-table border :data="usageList">
        <el-table-column label="使用人ID" prop="userId" width="100" />
        <el-table-column label="绑定账号" prop="boundAccount" min-width="130" />
        <el-table-column label="使用时间" prop="usedAt" width="170" />
        <el-table-column label="IP" prop="ipAddress" width="130" />
        <el-table-column label="UserAgent" prop="userAgent" min-width="240" show-overflow-tooltip />
      </el-table>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ArtRegistrationCode" lang="ts">
import { listActivityOptions } from '@/api/crehn/activity';
import { addBusinessRole, listBusinessRoleOptions, listSchoolOptions } from '@/api/crehn/config';
import {
  addRegistrationCode,
  batchAddRegistrationCode,
  getRegistrationCode,
  listRegistrationCode,
  listRegistrationUsage,
  resendRegistrationCode,
  updateRegistrationCode,
  voidRegistrationCode
} from '@/api/crehn/registration';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const loading = ref(false);
const total = ref(0);
const codeList = ref<any[]>([]);
const usageList = ref<any[]>([]);
const activityOptions = ref<any[]>([]);
const schoolOptions = ref<any[]>([]);
const roleOptions = ref<any[]>([]);
const queryParams = reactive({ pageNum: 1, pageSize: 10, code: '', codeType: '', status: '' });
const formDialog = reactive({ visible: false, title: '' });
const batchDialog = reactive({ visible: false });
const usageDialog = reactive({ visible: false });
const roleDialog = reactive({ visible: false });
const form = ref<any>({});
const batchForm = ref<any>({});
const batchRows = ref<any[]>([]);
const roleForm = ref<any>({});

const activityNameMap = computed<Record<string, string>>(() =>
  Object.fromEntries(activityOptions.value.map((item) => [item.id, activityLabel(item)]))
);
const schoolNameMap = computed<Record<string, string>>(() =>
  Object.fromEntries(schoolOptions.value.map((item) => [item.id, schoolLabel(item)]))
);

const getOptions = async () => {
  const activityRes = await listActivityOptions();
  activityOptions.value = activityRes.data || [];
  const schoolRes = await listSchoolOptions({});
  schoolOptions.value = schoolRes.data || [];
  const roleRes = await listBusinessRoleOptions({});
  roleOptions.value = roleRes.data || [];
};

const getList = async () => {
  loading.value = true;
  const res = await listRegistrationCode(queryParams);
  codeList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

const resetQuery = () => {
  queryParams.code = '';
  queryParams.codeType = '';
  queryParams.status = '';
  queryParams.pageNum = 1;
  getList();
};

const openForm = async (row?: any) => {
  if (row?.id) {
    const { data } = await getRegistrationCode(row.id);
    form.value = { ...data };
    formDialog.title = '修改注册码绑定';
  } else {
    form.value = { codeType: 'school', maxUseCount: 1, status: 'unused', autoApprove: false, roleKey: 'crehn_school' };
    formDialog.title = '生成注册码';
  }
  formDialog.visible = true;
};

const openBatch = () => {
  batchForm.value = { codeType: 'school', maxUseCount: 1, autoApprove: false, roleKey: 'crehn_school' };
  batchRows.value = [{ schoolId: undefined, boundPhone: '', remark: '' }];
  batchDialog.visible = true;
};

const openRoleDialog = () => {
  roleForm.value = { roleType: 'custom' };
  roleDialog.visible = true;
};

const addBatchRow = () => {
  batchRows.value.push({ schoolId: undefined, boundPhone: '', remark: '' });
};

const removeBatchRow = (index: number) => {
  batchRows.value.splice(index, 1);
};

const submitForm = async () => {
  form.value.id ? await updateRegistrationCode(form.value) : await addRegistrationCode(form.value);
  proxy?.$modal.msgSuccess('保存成功');
  formDialog.visible = false;
  await getList();
};

const submitBatch = async () => {
  const rows = batchRows.value;
  if (rows.length === 0) {
    proxy?.$modal.msgError('请至少保留一行注册码');
    return;
  }
  await batchAddRegistrationCode({ ...batchForm.value, rows });
  proxy?.$modal.msgSuccess(`已生成 ${rows.length} 个注册码`);
  batchDialog.visible = false;
  await getList();
};

const submitRole = async () => {
  const { data } = await addBusinessRole(roleForm.value);
  proxy?.$modal.msgSuccess('角色已创建');
  roleDialog.visible = false;
  await getOptions();
  if (data?.roleKey) {
    form.value.roleKey = data.roleKey;
    batchForm.value.roleKey = data.roleKey;
  }
};

const voidCode = async (row: any) => {
  await proxy?.$modal.confirm(`确认作废注册码「${row.code}」？`);
  await voidRegistrationCode(row.id);
  proxy?.$modal.msgSuccess('已作废');
  await getList();
};

const resend = async (row: any) => {
  await proxy?.$modal.confirm(`确认重发注册码「${row.code}」？原码会失效并生成新码。`);
  await resendRegistrationCode(row.id);
  proxy?.$modal.msgSuccess('已重发');
  await getList();
};

const openUsage = async (row: any) => {
  const { data } = await listRegistrationUsage(row.id);
  usageList.value = data;
  usageDialog.visible = true;
};

const handleExport = () => {
  proxy?.download('/crehn/registration-code/export', queryParams, `registration_code_${new Date().getTime()}.xlsx`);
};

const activityLabel = (item: any) => item.edition ? `${item.edition} - ${item.activityName}` : item.activityName;
const schoolLabel = (item: any) => item.schoolType ? `${item.schoolName}（${item.schoolType}）` : item.schoolName;
const typeLabel = (type?: string) => ({ school: '学校', expert: '专家', admin: '管理员' }[type || ''] || type || '');
const statusLabel = (status?: string) => ({ unused: '未使用', active: '使用中', used: '已使用', voided: '已作废' }[status || ''] || status || '');
const statusTypes: Record<string, ElTagType> = { unused: 'success', active: 'primary', used: 'info', voided: 'danger' };
const statusType = (status?: string): ElTagType => statusTypes[status || ''] || 'info';

onMounted(async () => {
  await getOptions();
  await getList();
});
</script>

<style scoped>
.role-picker {
  display: grid;
  width: 100%;
  grid-template-columns: 1fr 74px;
  gap: 8px;
}
</style>
