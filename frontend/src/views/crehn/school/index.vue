<template>
  <div class="p-2">
    <el-card shadow="never">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="单位管理" name="unit">
          <el-form :model="queryParams" :inline="true" class="mb-3">
            <el-form-item label="单位名称">
              <el-input v-model="queryParams.schoolName" clearable placeholder="请输入单位名称" @keyup.enter="getSchoolList" />
            </el-form-item>
            <el-form-item label="院校类型">
              <el-select v-model="queryParams.schoolType" clearable filterable allow-create placeholder="全部类型" style="width: 160px">
                <el-option v-for="item in schoolTypeOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="queryParams.status" clearable placeholder="全部状态" style="width: 140px">
                <el-option label="启用" value="enabled" />
                <el-option label="停用" value="disabled" />
                <el-option label="回收站" value="recycled" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="getSchoolList">查询</el-button>
              <el-button icon="Refresh" @click="resetSchoolQuery">重置</el-button>
              <el-button v-hasPermi="['crehn:schoolInfo:edit']" type="primary" plain icon="Plus" @click="openSchoolForm()">新增单位</el-button>
              <el-button v-hasPermi="['crehn:schoolInfo:edit']" type="success" plain icon="Upload" @click="openSchoolImport(false)">增量导入</el-button>
              <el-button v-hasPermi="['crehn:schoolInfo:edit']" type="primary" plain icon="UploadFilled" @click="openSchoolImport(true)">覆盖导入</el-button>
              <el-button type="warning" plain icon="Download" @click="exportSchoolInfo">导出</el-button>
              <el-button plain icon="Download" @click="downloadSchoolTemplate">下载模板</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="schoolLoading" border :data="schoolRows">
            <el-table-column label="单位名称" prop="schoolName" min-width="220" show-overflow-tooltip />
            <el-table-column label="院校类型" prop="schoolType" width="130">
              <template #default="{ row }">
                <el-tag v-if="row.schoolType" type="success">{{ row.schoolType }}</el-tag>
                <el-tag v-else type="danger">未设置</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="地区" prop="region" min-width="150" show-overflow-tooltip />
            <el-table-column label="地址" prop="address" min-width="220" show-overflow-tooltip />
            <el-table-column label="负责人" prop="contactName" width="120" />
            <el-table-column label="联系电话" prop="contactPhone" width="150" />
            <el-table-column label="状态" prop="status" width="90">
              <template #default="{ row }">
                <el-tag :type="schoolStatusType(row.status)">{{ schoolStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="影响统计" min-width="220">
              <template #default="{ row }">
                <div class="impact-counts">
                  <el-tag size="small" type="info">账号 {{ numberText(row.accountCount) }}</el-tag>
                  <el-tag size="small" type="warning">项目 {{ numberText(row.projectCount) }}</el-tag>
                  <el-tag size="small" type="success">附件 {{ numberText(row.fileCount) }}</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" prop="createTime" width="170" />
            <el-table-column label="操作" width="300" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" icon="User" @click="openAccountDrawer(row)">账号</el-button>
                <el-button v-hasPermi="['crehn:schoolInfo:edit']" link type="primary" icon="Edit" @click="openSchoolForm(row)">编辑</el-button>
                <el-button
                  v-if="row.status !== 'recycled'"
                  v-hasPermi="['crehn:schoolInfo:remove']"
                  link
                  type="danger"
                  icon="Delete"
                  @click="moveSchoolToRecycle(row)"
                >
                  删除
                </el-button>
                <el-button v-else v-hasPermi="['crehn:schoolInfo:edit']" link type="success" icon="RefreshLeft" @click="restoreSchool(row)">恢复</el-button>
              </template>
            </el-table-column>
          </el-table>
          <pagination
            v-show="schoolTotal > 0"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            :total="schoolTotal"
            @pagination="getSchoolList"
          />
        </el-tab-pane>

        <el-tab-pane label="账号管理" name="account">
          <SchoolAccountManager />
        </el-tab-pane>

        <el-tab-pane label="账号安全" name="security">
          <AccountSecurityConfig />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <ArtPopupDialog v-model="schoolDialog.visible" :title="schoolDialog.title" width="760px" append-to-body>
      <el-form ref="schoolFormRef" :model="schoolForm" :rules="schoolRules" label-width="96px">
        <el-form-item label="单位名称" prop="schoolName">
          <el-input v-model="schoolForm.schoolName" maxlength="128" show-word-limit placeholder="请输入单位名称" />
        </el-form-item>
        <el-form-item label="院校类型">
          <el-select v-model="schoolForm.schoolType" clearable filterable allow-create class="w-full" placeholder="请选择或输入院校类型">
            <el-option v-for="item in schoolTypeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="地区">
          <el-input v-model="schoolForm.region" maxlength="128" placeholder="例如：河南省/郑州市" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="schoolForm.address" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="schoolForm.contactName" maxlength="64" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="schoolForm.contactPhone" maxlength="32" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="schoolForm.contactEmail" maxlength="128" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="schoolForm.status" :disabled="schoolForm.status === 'recycled'">
            <el-radio-button label="enabled">启用</el-radio-button>
            <el-radio-button label="disabled">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="schoolForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitSchoolForm">保存</el-button>
        <el-button @click="schoolDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="schoolUpload.open" :title="schoolUpload.title" width="460px" append-to-body>
      <el-alert class="mb-3" type="info" :closable="false" show-icon :title="schoolUpload.updateSupport ? '覆盖导入会按单位ID、学校代码、学校名称匹配已有单位，只覆盖Excel中填写的非空字段。' : '增量导入用于新增单位，已存在单位会跳过。'" />
      <el-upload
        ref="schoolUploadRef"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="schoolUpload.headers"
        :action="schoolUpload.url"
        :data="{ updateSupport: schoolUpload.updateSupport }"
        :disabled="schoolUpload.isUploading"
        :on-progress="handleSchoolUploadProgress"
        :on-success="handleSchoolUploadSuccess"
        :auto-upload="false"
        drag
      >
        <el-icon class="el-icon--upload">
          <i-ep-upload-filled />
        </el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="text-center el-upload__tip">
            <span>{{ schoolUpload.updateSupport ? '适用于导出后批量补充院校类型；空白单元格不会清空原数据。' : '仅允许导入 xls、xlsx 文件；学校名称为必填，已存在学校会跳过。' }}</span>
            <el-link type="primary" :underline="false" style="font-size: 12px; vertical-align: baseline" @click="downloadSchoolTemplate">下载模板</el-link>
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button type="primary" @click="submitSchoolUpload">确定</el-button>
        <el-button @click="schoolUpload.open = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDrawer v-model="accountDrawer.visible" :title="`${currentSchool?.schoolName || ''} - 绑定账号`" size="900px" append-to-body>
      <div class="mb-3">
        <el-button v-hasPermi="['crehn:schoolAccount:edit']" type="primary" icon="Plus" @click="openBindDialog">新增绑定账号</el-button>
        <el-button icon="Refresh" @click="getBoundAccounts">刷新</el-button>
      </div>
      <el-table v-loading="accountLoading" border :data="accountRows">
        <el-table-column label="登录账号" prop="userName" min-width="150" />
        <el-table-column label="姓名/名称" prop="nickName" min-width="140" />
        <el-table-column label="手机" prop="phonenumber" min-width="130" />
        <el-table-column label="审核状态" prop="schoolReviewStatus" width="110">
          <template #default="{ row }">
            <el-tag :type="accountStatusType(row.schoolReviewStatus)">{{ accountStatusLabel(row.schoolReviewStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="系统状态" prop="status" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'info'">{{ row.status === '0' ? '正常' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="170" />
        <el-table-column label="操作" width="110" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-hasPermi="['crehn:schoolAccount:edit']" link type="danger" icon="Close" @click="unbindAccount(row)">解绑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="accountTotal > 0"
        v-model:page="accountQuery.pageNum"
        v-model:limit="accountQuery.pageSize"
        :total="accountTotal"
        @pagination="getBoundAccounts"
      />
    </ArtPopupDrawer>

    <ArtPopupDialog v-model="bindDialog.visible" title="新增绑定账号" width="760px" append-to-body>
      <el-tabs v-model="bindMode">
        <el-tab-pane label="选择已有账号" name="existing">
          <el-form :model="availableQuery" :inline="true" class="mb-2">
            <el-form-item label="账号">
              <el-input v-model="availableQuery.userName" clearable placeholder="登录账号" @keyup.enter="getAvailableAccounts" />
            </el-form-item>
            <el-form-item label="姓名">
              <el-input v-model="availableQuery.nickName" clearable placeholder="姓名/名称" @keyup.enter="getAvailableAccounts" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="getAvailableAccounts">查询</el-button>
              <el-button icon="Refresh" @click="resetAvailableQuery">重置</el-button>
            </el-form-item>
          </el-form>
          <el-alert class="mb-2" type="info" :closable="false" show-icon title="这里只显示未绑定单位的学校账号。绑定后账号会启用并归属当前单位。" />
          <el-table v-loading="availableLoading" border :data="availableRows" @selection-change="handleAvailableSelection">
            <el-table-column type="selection" width="50" />
            <el-table-column label="登录账号" prop="userName" min-width="150" />
            <el-table-column label="姓名/名称" prop="nickName" min-width="140" />
            <el-table-column label="手机" prop="phonenumber" min-width="130" />
            <el-table-column label="审核状态" prop="schoolReviewStatus" width="110">
              <template #default="{ row }">
                <el-tag :type="accountStatusType(row.schoolReviewStatus)">{{ accountStatusLabel(row.schoolReviewStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" prop="createTime" width="170" />
          </el-table>
          <pagination
            v-show="availableTotal > 0"
            v-model:page="availableQuery.pageNum"
            v-model:limit="availableQuery.pageSize"
            :total="availableTotal"
            @pagination="getAvailableAccounts"
          />
        </el-tab-pane>

        <el-tab-pane label="创建新账号" name="create">
          <el-form ref="accountFormRef" :model="accountForm" :rules="accountRules" label-width="96px">
            <el-form-item label="登录账号" prop="userName">
              <el-input v-model="accountForm.userName" maxlength="30" />
            </el-form-item>
            <el-form-item label="姓名/名称" prop="nickName">
              <el-input v-model="accountForm.nickName" maxlength="30" />
            </el-form-item>
            <el-form-item label="初始密码" prop="password">
              <el-input v-model="accountForm.password" type="password" show-password maxlength="30" />
            </el-form-item>
            <el-form-item label="角色" prop="roleIds">
              <el-select v-model="accountForm.roleIds" multiple filterable placeholder="请选择学校业务角色" class="w-full">
                <el-option v-for="role in roleOptions" :key="role.roleId" :label="role.roleName" :value="role.roleId" />
              </el-select>
            </el-form-item>
            <el-form-item label="手机">
              <el-input v-model="accountForm.phonenumber" maxlength="11" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="accountForm.email" maxlength="50" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button v-if="bindMode === 'existing'" type="primary" @click="submitBindExisting">绑定所选账号</el-button>
        <el-button v-else type="primary" @click="submitAccountForm">创建并绑定</el-button>
        <el-button @click="bindDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ArtSchool" lang="ts">
import AccountSecurityConfig from './components/AccountSecurityConfig.vue';
import SchoolAccountManager from '@/views/crehn/school-account/index.vue';
import { deleteSchoolInfo, getSchoolInfoImpact, importSchoolInfoUrl, listBusinessRoleOptions, listSchoolInfo, restoreSchoolInfo, saveSchoolInfo } from '@/api/crehn/config';
import { bindSchoolAccount, createArtAccount, listAvailableSchoolAccount, listSchoolAccount, unbindSchoolAccount } from '@/api/crehn/schoolAccount';
import { globalHeaders } from '@/utils/request';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const activeTab = ref('unit');
const schoolLoading = ref(false);
const schoolRows = ref<any[]>([]);
const schoolTotal = ref(0);
const schoolFormRef = ref<ElFormInstance>();
const schoolUploadRef = ref<ElUploadInstance>();
const schoolDialog = reactive({ visible: false, title: '' });
const queryParams = reactive<any>({ pageNum: 1, pageSize: 10, schoolName: '', schoolType: '', status: '' });
const schoolForm = ref<any>({});
const schoolTypeOptions = ['本科院校', '高职高专'];
const schoolUpload = reactive<ImportOption>({
  open: false,
  title: '',
  isUploading: false,
  updateSupport: 0,
  headers: globalHeaders(),
  url: importSchoolInfoUrl()
});
const schoolRules: ElFormRules = {
  schoolName: [{ required: true, message: '请输入单位名称', trigger: 'blur' }]
};

const currentSchool = ref<any>();
const accountDrawer = reactive({ visible: false });
const accountLoading = ref(false);
const accountRows = ref<any[]>([]);
const accountTotal = ref(0);
const accountQuery = reactive<any>({ pageNum: 1, pageSize: 10, schoolId: undefined });

const bindDialog = reactive({ visible: false });
const bindMode = ref('existing');
const availableLoading = ref(false);
const availableRows = ref<any[]>([]);
const availableTotal = ref(0);
const availableSelection = ref<any[]>([]);
const availableQuery = reactive<any>({ pageNum: 1, pageSize: 10, userName: '', nickName: '' });
const accountFormRef = ref<ElFormInstance>();
const accountForm = ref<any>({});
const roleOptions = ref<any[]>([]);
const accountRules: ElFormRules = {
  userName: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  nickName: [{ required: true, message: '请输入姓名/名称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  roleIds: [{ required: true, message: '请选择角色', trigger: 'change' }]
};

const getSchoolList = async () => {
  schoolLoading.value = true;
  try {
    const res = await listSchoolInfo(queryParams);
    schoolRows.value = res.rows || [];
    schoolTotal.value = res.total || 0;
  } finally {
    schoolLoading.value = false;
  }
};

const resetSchoolQuery = () => {
  queryParams.schoolName = '';
  queryParams.schoolType = '';
  queryParams.status = '';
  queryParams.pageNum = 1;
  getSchoolList();
};

const openSchoolForm = (row?: any) => {
  schoolForm.value = row ? { ...row } : { schoolName: '', schoolType: '', status: 'enabled', region: '', address: '', contactName: '', contactPhone: '', contactEmail: '' };
  schoolDialog.title = row ? '编辑单位' : '新增单位';
  schoolDialog.visible = true;
  nextTick(() => schoolFormRef.value?.clearValidate());
};

const submitSchoolForm = async () => {
  await schoolFormRef.value?.validate();
  await saveSchoolInfo(schoolForm.value);
  proxy?.$modal.msgSuccess('保存成功');
  schoolDialog.visible = false;
  await getSchoolList();
};

const moveSchoolToRecycle = async (row: any) => {
  const impact = await fetchSchoolImpact(row);
  const hasImpact = hasAccountOrProject(impact);
  if (hasImpact) {
    await proxy?.$modal.confirm(
      `单位“${row.schoolName}”下存在数据：${impactText(impact)}。\n删除单位不会删除填报或附件，但账号会因单位失效不能继续申报。\n请先处理账号，或继续二次确认移入回收站。`
    );
    await proxy?.$modal.confirm(`二次确认：仍要将单位“${row.schoolName}”移入回收站吗？`);
  } else {
    await proxy?.$modal.confirm(`确认将单位“${row.schoolName}”移入回收站？不会删除填报或附件。`);
  }
  await deleteSchoolInfo(row.id, hasImpact);
  proxy?.$modal.msgSuccess('已移入回收站');
  await getSchoolList();
};

const restoreSchool = async (row: any) => {
  await proxy?.$modal.confirm(`确认恢复单位“${row.schoolName}”？`);
  await restoreSchoolInfo(row.id);
  proxy?.$modal.msgSuccess('恢复成功');
  await getSchoolList();
  try {
    await proxy?.$modal.confirm(`是否打开账号抽屉，为“${row.schoolName}”恢复或创建学校账号并重新授权？`);
    await openAccountDrawer({ ...row, status: 'enabled' });
  } catch {
    // 用户取消辅助入口时，单位恢复本身已完成。
  }
};

const fetchSchoolImpact = async (row: any) => {
  const { data } = await getSchoolInfoImpact(row.id);
  return data?.[0] || row || {};
};

const hasAccountOrProject = (impact: any) => Number(impact?.accountCount || 0) > 0 || Number(impact?.projectCount || 0) > 0;

const numberText = (value?: number | string) => Number(value || 0);

const impactText = (impact: any) =>
  `账号 ${numberText(impact?.accountCount)} 个、项目 ${numberText(impact?.projectCount)} 个、附件 ${numberText(impact?.fileCount)} 个`;

const openSchoolImport = (updateSupport = false) => {
  schoolUpload.title = updateSupport ? '单位覆盖导入' : '单位增量导入';
  schoolUpload.updateSupport = updateSupport ? 1 : 0;
  schoolUpload.open = true;
  nextTick(() => schoolUploadRef.value?.clearFiles());
};

const exportSchoolInfo = () => {
  proxy?.download('crehn/school-info/export', { ...queryParams }, `school_info_${new Date().getTime()}.xlsx`);
};

const downloadSchoolTemplate = () => {
  proxy?.download('crehn/school-info/importTemplate', {}, `school_info_template_${new Date().getTime()}.xlsx`);
};

const handleSchoolUploadProgress = () => {
  schoolUpload.isUploading = true;
};

const handleSchoolUploadSuccess = (response: any, file: UploadFile) => {
  schoolUpload.open = false;
  schoolUpload.isUploading = false;
  schoolUploadRef.value?.handleRemove(file);
  ElMessageBox.alert("<div style='overflow: auto;overflow-x: hidden;max-height: 70vh;padding: 10px 20px 0;'>" + response.msg + '</div>', '导入结果', {
    confirmButtonText: '确定',
    dangerouslyUseHTMLString: true
  });
  getSchoolList();
};

const submitSchoolUpload = () => {
  schoolUploadRef.value?.submit();
};

const openAccountDrawer = async (row: any) => {
  currentSchool.value = row;
  accountQuery.pageNum = 1;
  accountQuery.schoolId = row.id;
  accountDrawer.visible = true;
  await getBoundAccounts();
};

const getBoundAccounts = async () => {
  if (!accountQuery.schoolId) return;
  accountLoading.value = true;
  try {
    const res = await listSchoolAccount(accountQuery);
    accountRows.value = res.rows || [];
    accountTotal.value = res.total || 0;
  } finally {
    accountLoading.value = false;
  }
};

const openBindDialog = async () => {
  if (!currentSchool.value?.id) {
    proxy?.$modal.msgError('请先选择单位');
    return;
  }
  bindMode.value = 'existing';
  resetAccountForm();
  bindDialog.visible = true;
  await Promise.all([getAvailableAccounts(), loadRoleOptions()]);
  nextTick(() => accountFormRef.value?.clearValidate());
};

const getAvailableAccounts = async () => {
  availableLoading.value = true;
  try {
    const res = await listAvailableSchoolAccount(availableQuery);
    availableRows.value = res.rows || [];
    availableTotal.value = res.total || 0;
  } finally {
    availableLoading.value = false;
  }
};

const resetAvailableQuery = () => {
  availableQuery.userName = '';
  availableQuery.nickName = '';
  availableQuery.pageNum = 1;
  getAvailableAccounts();
};

const handleAvailableSelection = (rows: any[]) => {
  availableSelection.value = rows;
};

const submitBindExisting = async () => {
  if (!availableSelection.value.length) {
    proxy?.$modal.msgError('请选择要绑定的账号');
    return;
  }
  await proxy?.$modal.confirm(`确认将 ${availableSelection.value.length} 个账号绑定到“${currentSchool.value.schoolName}”？`);
  for (const row of availableSelection.value) {
    await bindSchoolAccount(row.userId, currentSchool.value.id);
  }
  proxy?.$modal.msgSuccess('绑定成功');
  bindDialog.visible = false;
  await Promise.all([getBoundAccounts(), getAvailableAccounts()]);
};

const resetAccountForm = () => {
  accountForm.value = {
    userType: 'school',
    schoolId: currentSchool.value?.id,
    schoolReviewStatus: 'enabled',
    userName: '',
    nickName: '',
    password: '',
    roleIds: [],
    phonenumber: '',
    email: ''
  };
};

const loadRoleOptions = async () => {
  if (!roleOptions.value.length) {
    const res = await listBusinessRoleOptions({ userType: 'school' });
    roleOptions.value = res.data || [];
  }
};

const submitAccountForm = async () => {
  await accountFormRef.value?.validate();
  await createArtAccount({ ...accountForm.value, schoolId: currentSchool.value?.id });
  proxy?.$modal.msgSuccess('账号已创建并绑定');
  bindDialog.visible = false;
  await getBoundAccounts();
};

const unbindAccount = async (row: any) => {
  await proxy?.$modal.confirm(`确认解绑账号“${row.userName}”？解绑后账号会停用，可再次绑定到单位后启用。`);
  await unbindSchoolAccount(row.userId);
  proxy?.$modal.msgSuccess('已解绑并停用');
  await getBoundAccounts();
};

const schoolStatusLabel = (status?: string) => {
  const map: Record<string, string> = { enabled: '启用', disabled: '停用', recycled: '回收站' };
  return map[status || ''] || status || '-';
};

const schoolStatusType = (status?: string): ElTagType => {
  const map: Record<string, ElTagType> = { enabled: 'success', disabled: 'info', recycled: 'danger' };
  return map[status || ''] || 'info';
};

const accountStatusLabel = (status?: string) => {
  const map: Record<string, string> = { pending_review: '待审核', enabled: '已启用', rejected: '已驳回', disabled: '已停用', locked: '已锁定' };
  return map[status || ''] || status || '-';
};

const accountStatusType = (status?: string): ElTagType => {
  const map: Record<string, ElTagType> = { pending_review: 'warning', enabled: 'success', rejected: 'danger', disabled: 'info', locked: 'danger' };
  return map[status || ''] || 'info';
};

onMounted(getSchoolList);
</script>

<style scoped>
.impact-counts {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}
</style>
