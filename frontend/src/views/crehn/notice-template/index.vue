<template>
  <div class="p-2 notice-template-page">
    <el-card shadow="hover" class="mb-[10px]">
      <el-form :model="queryParams" :inline="true">
        <el-form-item label="公告名称">
          <el-input v-model="queryParams.templateName" clearable placeholder="公告名称或标题" />
        </el-form-item>
        <el-form-item label="公告类型">
          <el-select v-model="queryParams.noticeGroup" clearable placeholder="全部" style="width: 180px">
            <el-option label="注册/后台公告" value="注册公告" />
            <el-option label="展演阶段公告" value="展演阶段" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
          <el-button type="primary" plain icon="Plus" @click="openForm()">新增公告</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <el-table border :data="rows">
        <el-table-column label="公告名称" prop="templateName" min-width="160" />
        <el-table-column label="公告标题" prop="noticeTitle" min-width="220" />
        <el-table-column label="公告类型" width="140">
          <template #default="scope">{{ noticeTypeLabel(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="账号类型" width="120">
          <template #default="scope">{{ targetUserLabel(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="附件" width="90" align="center">
          <template #default="scope">{{ attachmentCount(scope.row.attachmentOssIds) }}</template>
        </el-table-column>
        <el-table-column label="启用" prop="enabled" width="90" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="sortOrder" width="90" align="center" />
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="openForm(scope.row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <ArtPopupDialog v-model="dialog.visible" :title="dialog.title" width="760px" append-to-body>
      <el-form :model="form" label-width="104px">
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12">
            <el-form-item label="公告类型" required>
              <el-select v-model="form.noticeType" class="w-full" @change="onNoticeTypeChange">
                <el-option v-for="item in noticeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="公告名称" required>
              <el-input v-model="form.templateName" maxlength="60" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="公告标题" required>
          <el-input v-model="form.noticeTitle" maxlength="100" />
        </el-form-item>
        <el-form-item label="公告内容" required>
          <el-input v-model="form.noticeContent" type="textarea" :rows="8" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-row v-if="form.noticeType !== 'stage'" :gutter="12">
          <el-col :xs="24" :sm="12">
            <el-form-item label="账号类型" required>
              <el-select v-model="form.targetUserType" class="w-full">
                <el-option label="学校" value="school" />
                <el-option label="专家" value="expert" />
                <el-option label="管理员" value="admin" />
                <el-option label="审核员" value="auditor" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="附件">
          <file-upload
            v-model="form.attachmentOssIds"
            :limit="8"
            :file-size="30"
            :file-type="['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'pdf', 'zip', 'rar', 'png', 'jpg', 'jpeg']"
          />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12">
            <el-form-item label="启用">
              <el-switch v-model="form.enabled" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" class="w-full" :min="0" :step="1" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">保存</el-button>
        <el-button @click="dialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ArtNoticeTemplate" lang="ts">
import { listNoticeTemplate, saveNoticeTemplate } from '@/api/crehn/config';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const rows = ref<any[]>([]);
const total = ref(0);
const queryParams = reactive<any>({ pageNum: 1, pageSize: 10 });
const dialog = reactive({ visible: false, title: '' });
const form = ref<any>({});

const REGISTER_NOTICE_GROUP = '注册公告';
const STAGE_NOTICE_GROUP = '展演阶段';

const noticeTypeOptions = [
  { label: '后台首页公告', value: 'workbench', noticeGroup: REGISTER_NOTICE_GROUP, defaultUserType: 'admin' },
  { label: '注册公告', value: 'register', noticeGroup: REGISTER_NOTICE_GROUP, defaultUserType: 'school' },
  { label: '展演阶段公告', value: 'stage', noticeGroup: STAGE_NOTICE_GROUP, defaultUserType: undefined }
];

const getList = async () => {
  const res = await listNoticeTemplate(queryParams);
  rows.value = res.rows || [];
  total.value = res.total || 0;
};

const typeOption = (type?: string) => noticeTypeOptions.find((item) => item.value === type) || noticeTypeOptions[0];

const deriveNoticeType = (row?: any) => {
  if (row?.noticeGroup === STAGE_NOTICE_GROUP) return 'stage';
  if (['admin', 'auditor', 'expert'].includes(String(row?.targetUserType || ''))) return 'workbench';
  return 'register';
};

const createDefaultForm = () => {
  const option = typeOption('workbench');
  return {
    enabled: true,
    sortOrder: 0,
    noticeType: option.value,
    noticeGroup: option.noticeGroup,
    targetMode: 'user_type',
    targetUserType: option.defaultUserType,
    templateCode: `notice_${Date.now()}`,
    templateName: option.label,
    noticeTitle: '',
    noticeContent: '',
    attachmentOssIds: ''
  };
};

const openForm = (row?: any) => {
  form.value = row ? { ...row, noticeType: deriveNoticeType(row) } : createDefaultForm();
  dialog.title = row ? '编辑公告' : '新增公告';
  dialog.visible = true;
};

const onNoticeTypeChange = () => {
  const option = typeOption(form.value.noticeType);
  form.value.noticeGroup = option.noticeGroup;
  if (!form.value.templateName || noticeTypeOptions.some((item) => item.label === form.value.templateName)) {
    form.value.templateName = option.label;
  }
  if (option.value === 'stage') {
    form.value.targetMode = 'all';
    form.value.targetUserType = undefined;
    form.value.activityId = undefined;
  } else {
    form.value.targetMode = 'user_type';
    form.value.targetUserType = form.value.targetUserType || option.defaultUserType;
  }
};

const submitForm = async () => {
  if (!String(form.value.templateName || '').trim()) {
    proxy?.$modal.msgError('请填写公告名称');
    return;
  }
  if (!String(form.value.noticeTitle || '').trim()) {
    proxy?.$modal.msgError('请填写公告标题');
    return;
  }
  if (!String(form.value.noticeContent || '').trim()) {
    proxy?.$modal.msgError('请填写公告内容');
    return;
  }
  const option = typeOption(form.value.noticeType);
  const payload = { ...form.value, noticeGroup: option.noticeGroup };
  if (option.value === 'stage') {
    payload.targetMode = 'all';
    payload.targetUserType = undefined;
    payload.activityId = undefined;
  } else {
    payload.targetMode = 'user_type';
    payload.targetUserType = payload.targetUserType || option.defaultUserType;
  }
  delete payload.noticeType;
  await saveNoticeTemplate(payload);
  proxy?.$modal.msgSuccess('保存成功');
  dialog.visible = false;
  await getList();
};

const attachmentCount = (ids?: string) => (ids ? ids.split(',').filter(Boolean).length : 0);
const typeLabel = (type?: string) => ({ school: '学校', expert: '专家', admin: '管理员', auditor: '审核员' })[type || ''] || '全部';
const targetUserLabel = (row: any) => (row.noticeGroup === STAGE_NOTICE_GROUP ? '全部' : typeLabel(row.targetUserType));
const noticeTypeLabel = (row: any) => typeOption(deriveNoticeType(row)).label;

onMounted(async () => {
  await getList();
});
</script>

<style scoped lang="scss">
.notice-template-page {
  :deep(.el-input-number) {
    width: 100%;
  }
}
</style>
