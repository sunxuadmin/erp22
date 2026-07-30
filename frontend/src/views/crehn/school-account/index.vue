<template>
  <div class="p-2">
    <el-tabs v-model="activeTab" class="account-tabs">
      <el-tab-pane label="账号审核" name="accounts">
        <el-card shadow="hover" class="mb-[10px]">
          <el-form :model="queryParams" :inline="true">
            <el-form-item label="账号">
              <el-input v-model="queryParams.userName" placeholder="请输入账号/英文登录账号" clearable @keyup.enter="getList" />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="queryParams.schoolReviewStatus" clearable placeholder="全部状态">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
              <el-button icon="Refresh" @click="resetQuery">重置</el-button>
              <el-button v-hasPermi="['crehn:schoolAccount:add']" type="success" plain icon="Upload" @click="openAccountImport">导入</el-button>
              <el-button v-hasPermi="['crehn:schoolAccount:export']" type="warning" plain icon="Download" @click="exportAccount">导出</el-button>
              <el-button plain icon="Download" @click="downloadAccountTemplate">下载模板</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="hover">
          <el-table v-loading="loading" border :data="accountList">
        <el-table-column label="用户ID" prop="userId" width="90" />
        <el-table-column label="账号" prop="userName" min-width="130" />
        <el-table-column label="英文登录账号" prop="loginAlias" min-width="130" show-overflow-tooltip />
        <el-table-column label="昵称" prop="nickName" min-width="130" />
        <el-table-column label="所属单位" min-width="220">
          <template #default="scope">
            <div class="school-cell">
              <span>{{ schoolNameMap[scope.row.schoolId] || scope.row.schoolId || '-' }}</span>
              <el-tag v-if="schoolStatus(scope.row.schoolId) !== 'enabled'" size="small" type="danger">
                {{ schoolStatusLabel(schoolStatus(scope.row.schoolId)) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="审核状态" prop="schoolReviewStatus" width="110">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.schoolReviewStatus)">{{ statusLabel(scope.row.schoolReviewStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="审核人" prop="schoolReviewBy" width="100" />
        <el-table-column label="审核时间" prop="schoolReviewTime" width="170" />
        <el-table-column label="审核意见" prop="schoolReviewOpinion" min-width="160" show-overflow-tooltip />
        <el-table-column label="创建时间" prop="createTime" width="170" />
        <el-table-column label="操作" width="340" fixed="right" align="center">
          <template #default="scope">
            <el-button
              v-if="scope.row.delFlag === '1' || queryParams.schoolReviewStatus === 'deleted'"
              v-hasPermi="['crehn:schoolAccount:restore']"
              link
              type="success"
              icon="RefreshLeft"
              @click="restoreAccount(scope.row)"
            >
              恢复
            </el-button>
            <template v-else>
            <el-button
              v-if="scope.row.schoolReviewStatus === 'pending_review'"
              v-hasPermi="['crehn:schoolAccount:review']"
              link
              type="success"
              icon="Check"
              @click="changeStatus(scope.row, 'enabled')"
            >
              通过
            </el-button>
            <el-button
              v-if="scope.row.schoolReviewStatus === 'pending_review'"
              v-hasPermi="['crehn:schoolAccount:review']"
              link
              type="danger"
              icon="Close"
              @click="openReject(scope.row)"
            >
              驳回
            </el-button>
            <el-button
              v-if="scope.row.schoolReviewStatus === 'enabled'"
              v-hasPermi="['crehn:schoolAccount:review']"
              link
              type="warning"
              icon="CircleClose"
              @click="changeStatus(scope.row, 'disabled')"
            >
              停用
            </el-button>
            <el-button
              v-if="scope.row.schoolReviewStatus === 'disabled'"
              v-hasPermi="['crehn:schoolAccount:review']"
              link
              type="success"
              icon="CircleCheck"
              @click="changeStatus(scope.row, 'enabled')"
            >
              启用
            </el-button>
            <el-button
              v-if="scope.row.schoolReviewStatus === 'locked'"
              v-hasPermi="['crehn:schoolAccount:review']"
              link
              type="success"
              icon="Unlock"
              @click="changeStatus(scope.row, 'enabled')"
            >
              解锁
            </el-button>
            <el-button
              v-if="scope.row.schoolReviewStatus === 'rejected'"
              v-hasPermi="['crehn:schoolAccount:review']"
              link
              type="primary"
              icon="RefreshLeft"
              @click="changeStatus(scope.row, 'pending_review')"
            >
              重新审核
            </el-button>
            <el-button
              v-if="['enabled', 'disabled'].includes(scope.row.schoolReviewStatus)"
              v-hasPermi="['crehn:schoolAccount:edit']"
              link
              type="primary"
              icon="Edit"
              @click="openEdit(scope.row)"
            >
              修改
            </el-button>
            <el-button
              v-if="['enabled', 'disabled', 'locked'].includes(scope.row.schoolReviewStatus)"
              v-hasPermi="['crehn:schoolAccount:resetPwd']"
              link
              type="primary"
              icon="Key"
              @click="openResetPwd(scope.row)"
            >
              重置密码
            </el-button>
            <el-button
              v-if="scope.row.schoolId"
              v-hasPermi="['crehn:projectView:list']"
              link
              type="primary"
              icon="View"
              @click="viewSchoolProjects(scope.row)"
            >
              查看报送
            </el-button>
            <el-button
              v-if="scope.row.schoolReviewStatus === 'rejected'"
              v-hasPermi="['crehn:schoolAccount:remove']"
              link
              type="danger"
              icon="Delete"
              @click="removeAccount(scope.row)"
            >
              删除
            </el-button>
            <el-button
              v-if="scope.row.schoolReviewStatus === 'rejected'"
              v-hasPermi="['crehn:schoolAccount:review']"
              link
              type="warning"
              icon="CircleClose"
              @click="changeStatus(scope.row, 'disabled')"
            >
              停用
            </el-button>
            </template>
          </template>
        </el-table-column>
          </el-table>
          <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane v-if="canViewRegistration" label="注册码/邀请" name="codes">
        <RegistrationCodeManager />
      </el-tab-pane>

      <el-tab-pane label="创建账号" name="create">
        <el-card shadow="hover">
          <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="110px" class="account-create-form">
            <el-row :gutter="16">
              <el-col :xs="24" :md="12">
                <el-form-item label="账号类型" prop="userType">
                  <el-select v-model="createForm.userType" class="w-full" @change="handleCreateTypeChange">
                    <el-option label="学校账号" value="school" />
                    <el-option label="专家账号" value="expert" />
                    <el-option label="审核老师" value="auditor" />
                    <el-option label="后台管理员" value="admin" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="绑定单位" :required="createForm.userType === 'school'">
                  <el-select v-model="createForm.schoolId" clearable filterable class="w-full" placeholder="学校账号必须选择单位">
                    <el-option v-for="item in enabledSchoolOptions" :key="item.id" :label="schoolLabel(item)" :value="item.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="登录账号" prop="userName"><el-input v-model="createForm.userName" /></el-form-item>
              </el-col>
              <el-col v-if="createForm.userType === 'school'" :xs="24" :md="12">
                <el-form-item label="英文登录账号" prop="loginAlias"><el-input v-model="createForm.loginAlias" /></el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="账号昵称" prop="nickName"><el-input v-model="createForm.nickName" /></el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="初始密码" prop="password"><el-input v-model="createForm.password" type="password" show-password /></el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="角色" prop="roleIds">
                  <div class="role-picker">
                    <el-select v-model="createForm.roleIds" multiple filterable class="w-full" placeholder="选择账号角色">
                      <el-option v-for="item in roleOptions" :key="item.roleId" :label="`${item.roleName}（${item.roleKey}）`" :value="item.roleId" />
                    </el-select>
                    <el-button v-if="canCreateBusinessRole" type="primary" plain icon="Plus" @click="openRoleDialog">新增</el-button>
                  </div>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="审核状态">
                  <el-select v-model="createForm.schoolReviewStatus" class="w-full" :disabled="createForm.userType !== 'school'">
                    <el-option label="直接启用" value="enabled" />
                    <el-option label="待审核" value="pending_review" />
                    <el-option label="已停用" value="disabled" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="手机号"><el-input v-model="createForm.phonenumber" /></el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="邮箱"><el-input v-model="createForm.email" /></el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="备注"><el-input v-model="createForm.remark" type="textarea" :rows="3" /></el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button v-hasPermi="['crehn:schoolAccount:add']" type="primary" icon="Plus" @click="submitCreateAccount">创建账号</el-button>
              <el-button icon="Refresh" @click="resetCreateForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <ArtPopupDialog v-model="rejectDialog.visible" title="驳回学校账号" width="520px" append-to-body>
      <el-form :model="rejectForm" label-width="90px">
        <el-form-item label="账号"><el-input v-model="rejectForm.userName" disabled /></el-form-item>
        <el-form-item label="驳回原因" required>
          <el-input v-model="rejectForm.opinion" type="textarea" :rows="4" placeholder="请填写驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitReject">确定</el-button>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="editDialog.visible" title="修改学校账号" width="560px" append-to-body>
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="账号"><el-input v-model="editForm.userName" /></el-form-item>
        <el-form-item label="英文登录账号"><el-input v-model="editForm.loginAlias" /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="editForm.nickName" /></el-form-item>
        <el-form-item label="绑定单位">
          <el-select v-model="editForm.schoolId" clearable filterable class="w-full" placeholder="选择单位">
            <el-option v-for="item in enabledSchoolOptions" :key="item.id" :label="schoolLabel(item)" :value="item.id" />
          </el-select>
          <div v-if="schoolStatus(editForm.schoolId) !== 'enabled'" class="invalid-school-tip">当前绑定单位已失效，请更换为启用单位。</div>
        </el-form-item>
        <el-form-item label="手机号"><el-input v-model="editForm.phonenumber" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="editForm.email" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="editForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitEdit">保存</el-button>
        <el-button @click="editDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="resetDialog.visible" title="重置学校账号密码" width="460px" append-to-body>
      <el-form :model="resetForm" label-width="90px">
        <el-form-item label="账号"><el-input v-model="resetForm.userName" disabled /></el-form-item>
        <el-form-item label="新密码" required><el-input v-model="resetForm.password" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitResetPwd">确定</el-button>
        <el-button @click="resetDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="roleDialog.visible" title="快速新增业务角色" width="520px" append-to-body>
      <el-form :model="roleForm" label-width="110px">
        <el-form-item label="角色名称" required><el-input v-model="roleForm.roleName" placeholder="例如：学校申报员、审核老师" /></el-form-item>
        <el-form-item label="角色类型">
          <el-select v-model="roleForm.roleType" class="w-full">
            <el-option label="学校" value="school" />
            <el-option label="专家" value="expert" />
            <el-option label="审核老师" value="auditor" />
            <el-option label="管理员" value="admin" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限字符"><el-input v-model="roleForm.roleKey" placeholder="可选，例如 crehn_school_submitter" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitRole">保存</el-button>
        <el-button @click="roleDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="upload.open" title="学校账号导入" width="430px" append-to-body>
      <el-upload
        ref="uploadRef"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="upload.headers"
        :action="upload.url + '?updateSupport=' + upload.updateSupport"
        :disabled="upload.isUploading"
        :on-progress="handleFileUploadProgress"
        :on-success="handleFileSuccess"
        :auto-upload="false"
        drag
      >
        <el-icon class="el-icon--upload">
          <i-ep-upload-filled />
        </el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="text-center el-upload__tip">
            <div class="el-upload__tip">
              <el-checkbox v-model="upload.updateSupport" :true-label="1" :false-label="0" />
              覆盖已存在账号基础资料
            </div>
            <span>覆盖不重置密码、不覆盖角色；改绑单位需在表格中填写“允许改绑=是”。</span>
            <el-link type="primary" :underline="false" style="font-size: 12px; vertical-align: baseline" @click="downloadAccountTemplate">下载模板</el-link>
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button type="primary" @click="submitFileForm">确定</el-button>
        <el-button @click="upload.open = false">取消</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ArtSchoolAccount" lang="ts">
import RegistrationCodeManager from '../registration/index.vue';
import { addBusinessRole, getSchoolInfoImpact, listBusinessRoleOptions, listSchoolOptions } from '@/api/crehn/config';
import {
  createArtAccount,
  deleteSchoolAccount,
  getSchoolAccount,
  importSchoolAccountUrl,
  listSchoolAccount,
  restoreSchoolAccount,
  resetSchoolAccountPwd,
  reviewSchoolAccount,
  updateSchoolAccount
} from '@/api/crehn/schoolAccount';
import { globalHeaders } from '@/utils/request';
import { checkPermi } from '@/utils/permission';
import { useRoute, useRouter } from 'vue-router';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const router = useRouter();
const route = useRoute();
const canViewRegistration = computed(() => checkPermi(['crehn:registration:list']));
const canCreateBusinessRole = computed(() => checkPermi(['crehn:registration:add']));

const activeTab = ref('accounts');
const loading = ref(false);
const total = ref(0);
const accountList = ref<any[]>([]);
const schoolOptions = ref<any[]>([]);
const roleOptions = ref<any[]>([]);
const queryParams = reactive<any>({ pageNum: 1, pageSize: 10, userName: '', schoolReviewStatus: '', schoolId: undefined });
const rejectDialog = reactive({ visible: false });
const editDialog = reactive({ visible: false });
const resetDialog = reactive({ visible: false });
const roleDialog = reactive({ visible: false });
const rejectForm = ref<any>({});
const editForm = ref<any>({});
const resetForm = ref<any>({});
const roleForm = ref<any>({});
const createFormRef = ref<ElFormInstance>();
const uploadRef = ref<ElUploadInstance>();
const upload = reactive<ImportOption>({
  open: false,
  title: '',
  isUploading: false,
  updateSupport: 0,
  headers: globalHeaders(),
  url: importSchoolAccountUrl()
});

const defaultCreateForm = () => ({
  userType: 'school',
  schoolId: undefined,
  userName: '',
  loginAlias: '',
  nickName: '',
  password: '',
  roleIds: [] as Array<number | string>,
  schoolReviewStatus: 'enabled',
  phonenumber: '',
  email: '',
  remark: ''
});
const createForm = ref<any>(defaultCreateForm());

const statusOptions = [
  { label: '待审核', value: 'pending_review', type: 'warning' },
  { label: '已启用', value: 'enabled', type: 'success' },
  { label: '已驳回', value: 'rejected', type: 'danger' },
  { label: '已停用', value: 'disabled', type: 'info' },
  { label: '已锁定', value: 'locked', type: 'danger' },
  { label: '已删除', value: 'deleted', type: 'danger' }
] satisfies Array<{ label: string; value: string; type: ElTagType }>;

const createRules = {
  userType: [{ required: true, message: '请选择账号类型', trigger: 'change' }],
  userName: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { min: 2, max: 30, message: '账号长度必须在 2 到 30 个字符之间', trigger: 'blur' }
  ],
  loginAlias: [
    {
      pattern: /^[A-Za-z0-9_-]{2,64}$/,
      message: '英文登录账号仅支持 2-64 位字母、数字、下划线或中横线',
      trigger: 'blur'
    }
  ],
  nickName: [{ required: true, message: '请输入账号昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    {
      pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,30}$/,
      message: '密码须为8至30位，并同时包含字母和数字',
      trigger: 'blur'
    }
  ],
  roleIds: [{ required: true, message: '请选择账号角色', trigger: 'change' }]
};

const schoolNameMap = computed<Record<string, string>>(() =>
  Object.fromEntries(schoolOptions.value.map((item) => [item.id, schoolLabel(item)]))
);
const schoolMap = computed<Record<string, any>>(() => Object.fromEntries(schoolOptions.value.map((item) => [item.id, item])));
const enabledSchoolOptions = computed(() => schoolOptions.value.filter((item) => item.status === 'enabled'));

const getOptions = async () => {
  const [schoolRes, roleRes] = await Promise.all([listSchoolOptions({}), listBusinessRoleOptions({})]);
  schoolOptions.value = schoolRes.data || [];
  roleOptions.value = roleRes.data || [];
  fillDefaultRole();
};

const getList = async () => {
  loading.value = true;
  const params: any = { ...queryParams };
  if (params.schoolReviewStatus === 'deleted') {
    params.schoolReviewStatus = '';
    params.delFlag = '1';
  }
  const res = await listSchoolAccount(params);
  accountList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

const resetQuery = () => {
  queryParams.userName = '';
  queryParams.schoolReviewStatus = '';
  queryParams.schoolId = undefined;
  queryParams.pageNum = 1;
  getList();
};

const openAccountImport = () => {
  upload.open = true;
};

const exportAccount = () => {
  proxy?.download('crehn/school-account/export', { ...queryParams }, `school_account_${new Date().getTime()}.xlsx`);
};

const downloadAccountTemplate = () => {
  proxy?.download('crehn/school-account/importTemplate', {}, `school_account_template_${new Date().getTime()}.xlsx`);
};

const handleFileUploadProgress = () => {
  upload.isUploading = true;
};

const handleFileSuccess = (response: any, file: UploadFile) => {
  upload.open = false;
  upload.isUploading = false;
  uploadRef.value?.handleRemove(file);
  ElMessageBox.alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + '</div>', '导入结果', {
    confirmButtonText: '确定',
    dangerouslyUseHTMLString: true
  });
  getList();
};

const submitFileForm = () => {
  uploadRef.value?.submit();
};

const changeStatus = async (row: any, status: string) => {
  await proxy?.$modal.confirm(`确认将学校账号「${row.userName}」设为${statusLabel(status)}？`);
  await reviewSchoolAccount(row.userId, status);
  proxy?.$modal.msgSuccess('操作成功');
  await getList();
};

const openReject = (row: any) => {
  rejectForm.value = { userId: row.userId, userName: row.userName, opinion: '' };
  rejectDialog.visible = true;
};

const submitReject = async () => {
  if (!rejectForm.value.opinion?.trim()) {
    proxy?.$modal.msgError('请填写驳回原因');
    return;
  }
  await reviewSchoolAccount(rejectForm.value.userId, 'rejected', rejectForm.value.opinion.trim());
  proxy?.$modal.msgSuccess('操作成功');
  rejectDialog.visible = false;
  await getList();
};

const openEdit = async (row: any) => {
  const { data } = await getSchoolAccount(row.userId);
  editForm.value = { ...data };
  editDialog.visible = true;
};

const submitEdit = async () => {
  if (!ensureEnabledSchoolSelected(editForm.value.schoolId)) {
    return;
  }
  await updateSchoolAccount(editForm.value);
  proxy?.$modal.msgSuccess('保存成功');
  editDialog.visible = false;
  await getList();
};

const handleCreateTypeChange = () => {
  createForm.value.schoolId = createForm.value.userType === 'school' ? createForm.value.schoolId : undefined;
  createForm.value.schoolReviewStatus = createForm.value.userType === 'school' ? 'enabled' : undefined;
  createForm.value.loginAlias = createForm.value.userType === 'school' ? createForm.value.loginAlias : '';
  fillDefaultRole();
};

const fillDefaultRole = () => {
  if (createForm.value.roleIds?.length) {
    return;
  }
  const typeRoleMap: Record<string, string[]> = {
    school: ['crehn_school', 'school'],
    expert: ['crehn_expert', 'expert'],
    auditor: ['crehn_auditor', 'auditor'],
    admin: ['crehn_admin']
  };
  const keys = typeRoleMap[createForm.value.userType] || [];
  const role = roleOptions.value.find((item) => keys.includes(item.roleKey));
  if (role?.roleId) {
    createForm.value.roleIds = [role.roleId];
  }
};

const resetCreateForm = () => {
  createForm.value = defaultCreateForm();
  fillDefaultRole();
  createFormRef.value?.clearValidate();
};

const submitCreateAccount = async () => {
  await createFormRef.value?.validate();
  if (createForm.value.userType === 'school' && !ensureEnabledSchoolSelected(createForm.value.schoolId)) {
    return;
  }
  await createArtAccount(createForm.value);
  proxy?.$modal.msgSuccess('账号创建成功');
  resetCreateForm();
  activeTab.value = 'accounts';
  await getList();
};

const openRoleDialog = () => {
  roleForm.value = { roleType: createForm.value.userType || 'custom' };
  roleDialog.visible = true;
};

const submitRole = async () => {
  const { data } = await addBusinessRole(roleForm.value);
  proxy?.$modal.msgSuccess('角色已创建');
  roleDialog.visible = false;
  await getOptions();
  if (data?.roleId) {
    createForm.value.roleIds = [data.roleId];
  }
};

const openResetPwd = (row: any) => {
  resetForm.value = { userId: row.userId, userName: row.userName, password: '' };
  resetDialog.visible = true;
};

const submitResetPwd = async () => {
  if (!resetForm.value.password || !/^(?=.*[A-Za-z])(?=.*\d).{8,30}$/.test(resetForm.value.password)) {
    proxy?.$modal.msgError('新密码须为8至30位，并同时包含字母和数字');
    return;
  }
  await resetSchoolAccountPwd(resetForm.value.userId, resetForm.value.password);
  proxy?.$modal.msgSuccess('重置成功');
  resetDialog.visible = false;
  await getList();
};

const removeAccount = async (row: any) => {
  const impact = row.schoolId ? await fetchSchoolImpact(row.schoolId) : null;
  const dataText = impact
    ? `绑定单位下当前有项目 ${numberText(impact.projectCount)} 个、附件 ${numberText(impact.fileCount)} 个。`
    : '该账号未绑定单位或单位不存在。';
  await proxy?.$modal.confirm(
    `确认删除已驳回学校账号「${row.userName}」？\n${dataText}\n不会删除填报，但账号将不可登录；系统用户删除会清角色，恢复需重新授权。`
  );
  await deleteSchoolAccount(row.userId);
  proxy?.$modal.msgSuccess('删除成功');
  await getList();
};

const fetchSchoolImpact = async (schoolId: string | number) => {
  const { data } = await getSchoolInfoImpact(schoolId);
  return data?.[0] || null;
};

const numberText = (value?: number | string) => Number(value || 0);

const viewSchoolProjects = async (row: any) => {
  if (!row.schoolId) {
    proxy?.$modal.msgWarning('该账号未绑定学校，无法按学校查看报送');
    return;
  }
  await router.push({
    path: '/crehn/project-view',
    query: {
      mode: 'school',
      schoolId: row.schoolId,
      schoolName: schoolNameMap.value[row.schoolId] || row.schoolName || row.userName || ''
    }
  });
};

const restoreAccount = async (row: any) => {
  await proxy?.$modal.confirm(`确认恢复学校账号「${row.userName}」？`);
  await restoreSchoolAccount(row.userId);
  proxy?.$modal.msgSuccess('恢复成功');
  await getList();
};

const statusLabel = (status?: string) => statusOptions.find((item) => item.value === status)?.label || status || '';
const statusType = (status?: string): ElTagType => statusOptions.find((item) => item.value === status)?.type || 'info';
const schoolLabel = (item: any) => {
  const name = item.schoolType ? `${item.schoolName}（${item.schoolType}）` : item.schoolName;
  return item.status && item.status !== 'enabled' ? `${name} - ${schoolStatusLabel(item.status)}` : name;
};
const schoolStatus = (schoolId?: string | number) => (schoolId ? schoolMap.value[schoolId]?.status || 'missing' : '');
const schoolStatusLabel = (status?: string) => {
  const map: Record<string, string> = { enabled: '有效', disabled: '已停用', recycled: '已回收', missing: '单位不存在' };
  return map[status || ''] || status || '未绑定';
};

const ensureEnabledSchoolSelected = (schoolId?: string | number) => {
  const status = schoolStatus(schoolId);
  if (!schoolId || status !== 'enabled') {
    proxy?.$modal.msgError(`请选择启用状态的单位后再提交，当前单位状态：${schoolStatusLabel(status)}`);
    return false;
  }
  return true;
};

const firstQueryValue = (value: unknown) => (Array.isArray(value) ? value[0] : value);

const routeSchoolId = () => {
  const value = firstQueryValue(route.query.schoolId);
  if (!value) {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isNaN(parsed) ? value : parsed;
};

const routeSchoolName = () => String(firstQueryValue(route.query.schoolName) || '');

const applyRouteAssist = () => {
  const schoolId = routeSchoolId();
  if (!schoolId) {
    return;
  }
  queryParams.schoolId = schoolId;
  if (firstQueryValue(route.query.mode) === 'create') {
    activeTab.value = 'create';
    createForm.value = {
      ...defaultCreateForm(),
      schoolId,
      nickName: routeSchoolName(),
      remark: routeSchoolName() ? `单位恢复后创建/授权：${routeSchoolName()}` : ''
    };
    fillDefaultRole();
    nextTick(() => createFormRef.value?.clearValidate());
  } else {
    activeTab.value = 'accounts';
  }
};

onMounted(async () => {
  await getOptions();
  applyRouteAssist();
  await getList();
});
</script>

<style scoped>
.account-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.account-create-form {
  max-width: 1040px;
}

.role-picker {
  display: grid;
  width: 100%;
  grid-template-columns: 1fr 74px;
  gap: 8px;
}

.school-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.invalid-school-tip {
  margin-top: 6px;
  color: var(--el-color-danger);
  font-size: 12px;
  line-height: 1.4;
}
</style>
