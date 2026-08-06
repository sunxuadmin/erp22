<template>
  <div class="p-2">
    <el-card class="mb-2" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="活动">
          <el-select v-model="query.activityId" clearable filterable placeholder="全部活动" style="width: 220px">
            <el-option v-for="activity in activityOptions" :key="String(activity.id)" :label="activity.activityName" :value="String(activity.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="姓名"><el-input v-model="query.participantName" clearable /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width: 160px">
            <el-option label="待激活确认" value="pending_confirmation" />
            <el-option label="已激活确认" value="confirmed" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="load">查询</el-button>
          <el-button v-hasPermi="['crehn:participant:import']" icon="Upload" @click="importVisible = true">导入名单</el-button>
          <el-button icon="Download" @click="downloadTemplate">下载模板</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <el-alert
        v-if="issuedCodes.length"
        class="mb-2"
        type="warning"
        :closable="false"
        title="以下激活码仅在本次操作返回，请立即导出或安全分发；系统不保存明文。"
      />
      <el-table v-loading="loading" :data="issuedCodes.length ? issuedCodes : rows" border>
        <template v-if="issuedCodes.length">
          <el-table-column label="登录账号" prop="userName" />
          <el-table-column label="姓名" prop="participantName" />
          <el-table-column label="手机号" prop="phonenumber" />
          <el-table-column label="一次性激活码" prop="activationCode" min-width="240" />
          <el-table-column label="有效期" prop="expiresAt" min-width="170" />
        </template>
        <template v-else>
          <el-table-column label="姓名" prop="participantName" />
          <el-table-column label="证件号" prop="identityNoMasked" />
          <el-table-column label="手机号" prop="phonenumber" />
          <el-table-column label="邮箱" prop="email" />
          <el-table-column label="状态" prop="status">
            <template #default="{ row }">
              <el-tag :type="row.status === 'confirmed' ? 'success' : 'warning'">
                {{ row.status === 'confirmed' ? '已激活确认' : '待激活确认' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="确认时间" prop="confirmedAt" min-width="170" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button v-if="row.status !== 'confirmed'" v-hasPermi="['crehn:participant:reissue']" link type="primary" @click="openReissue(row)">
                重发激活码
              </el-button>
            </template>
          </el-table-column>
        </template>
      </el-table>
      <pagination
        v-if="!issuedCodes.length"
        v-show="total > 0"
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="load"
      />
      <el-button
        v-else
        class="mt-2"
        @click="
          issuedCodes = [];
          load();
        "
        >返回参赛者列表</el-button
      >
    </el-card>

    <el-dialog v-model="importVisible" title="导入参赛者名单" width="520px">
      <el-form label-width="100px">
        <el-form-item label="活动" required>
          <el-select v-model="importForm.activityId" filterable placeholder="请选择活动" style="width: 100%">
            <el-option v-for="activity in activityOptions" :key="String(activity.id)" :label="activity.activityName" :value="String(activity.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="学校">
          <el-select v-model="importForm.schoolId" clearable filterable placeholder="当前学校账号可留空" style="width: 100%">
            <el-option v-for="school in schoolOptions" :key="String(school.id)" :label="school.schoolName" :value="String(school.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="激活截止" required>
          <el-date-picker v-model="importForm.expiresAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="Excel文件" required>
          <input type="file" accept=".xlsx,.xls" @change="chooseFile" />
        </el-form-item>
        <el-form-item label="批次备注"><el-input v-model="importForm.remark" type="textarea" maxlength="500" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">导入并生成激活码</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reissueVisible" title="重新签发激活码" width="480px">
      <el-form label-width="90px">
        <el-form-item label="新有效期" required>
          <el-date-picker v-model="reissueForm.expiresAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="原因" required><el-input v-model="reissueForm.reason" type="textarea" maxlength="500" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reissueVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReissue">确认签发</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { listActivityOptions } from '@/api/crehn/activity';
import { listSchoolOptions } from '@/api/crehn/config';
import type { ActivityVO } from '@/api/crehn/types';
import {
  downloadParticipantTemplate,
  importParticipant,
  listParticipant,
  reissueParticipantCode,
  type ParticipantActivationIssue,
  type ParticipantProfile
} from '@/api/crehn/participant';

const loading = ref(false);
const importing = ref(false);
const rows = ref<ParticipantProfile[]>([]);
const total = ref(0);
const issuedCodes = ref<ParticipantActivationIssue[]>([]);
const activityOptions = ref<ActivityVO[]>([]);
const schoolOptions = ref<Array<{ id?: string | number; schoolName?: string }>>([]);
const importVisible = ref(false);
const reissueVisible = ref(false);
const selectedFile = ref<File>();
const query = reactive({ pageNum: 1, pageSize: 10, activityId: '', participantName: '', status: '' });
const importForm = reactive({ activityId: '', schoolId: '', expiresAt: '', remark: '' });
const reissueForm = reactive({ participantId: '' as string | number, expiresAt: '', reason: '' });

const load = async () => {
  loading.value = true;
  try {
    const response: any = await listParticipant(query);
    rows.value = response.rows || [];
    total.value = response.total || 0;
  } finally {
    loading.value = false;
  }
};

const chooseFile = (event: Event) => {
  selectedFile.value = (event.target as HTMLInputElement).files?.[0];
};

const submitImport = async () => {
  if (!importForm.activityId || !importForm.expiresAt || !selectedFile.value) {
    ElMessage.warning('请填写活动、有效期并选择Excel文件');
    return;
  }
  const data = new FormData();
  data.append('file', selectedFile.value);
  data.append('activityId', importForm.activityId);
  if (importForm.schoolId) data.append('schoolId', importForm.schoolId);
  data.append('expiresAt', importForm.expiresAt);
  if (importForm.remark) data.append('remark', importForm.remark);
  importing.value = true;
  try {
    const response: any = await importParticipant(data);
    issuedCodes.value = response.data || [];
    importVisible.value = false;
  } finally {
    importing.value = false;
  }
};

const downloadTemplate = async () => {
  const response: any = await downloadParticipantTemplate();
  const url = URL.createObjectURL(response as Blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = 'CREHN参赛者名单导入模板.xlsx';
  anchor.click();
  URL.revokeObjectURL(url);
};

const openReissue = (row: ParticipantProfile) => {
  reissueForm.participantId = row.id!;
  reissueForm.expiresAt = '';
  reissueForm.reason = '';
  reissueVisible.value = true;
};

const submitReissue = async () => {
  if (!reissueForm.expiresAt || !reissueForm.reason.trim()) {
    ElMessage.warning('请填写新有效期和重新签发原因');
    return;
  }
  const response: any = await reissueParticipantCode(reissueForm);
  issuedCodes.value = [response.data];
  reissueVisible.value = false;
};

onMounted(async () => {
  const [activityResponse, schoolResponse]: any[] = await Promise.all([listActivityOptions(), listSchoolOptions({ status: '0' })]);
  activityOptions.value = activityResponse.data || [];
  schoolOptions.value = schoolResponse.data || [];
  await load();
});
</script>
