<template>
  <div class="p-2">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="按单位授权" name="school">
        <el-alert title="按单位授权，账号继承单位授权" type="info" show-icon :closable="false" class="mb-[10px]" />
        <el-card shadow="hover" class="mb-[10px]">
          <el-form :model="schoolQuery" :inline="true">
            <el-form-item label="单位">
              <el-input v-model="schoolQuery.schoolName" placeholder="请输入单位名称" clearable @keyup.enter="loadSchoolRows" />
            </el-form-item>
            <el-form-item label="院校类型">
              <el-select v-model="schoolQuery.schoolType" clearable filterable allow-create placeholder="全部类型" style="width: 160px">
                <el-option v-for="item in schoolTypeOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="loadSchoolRows">搜索</el-button>
              <el-button icon="Refresh" @click="resetSchoolQuery">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="hover">
          <el-table border :data="schoolRows">
            <el-table-column label="单位名称" prop="schoolName" min-width="220" />
            <el-table-column label="院校类型" width="120">
              <template #default="scope">
                <el-tag v-if="scope.row.schoolType" type="success">{{ scope.row.schoolType }}</el-tag>
                <el-tag v-else type="danger">未设置</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" prop="status" width="100">
              <template #default="scope">
                <el-tag :type="schoolStatusTag(scope.row.status)">{{ schoolStatusText(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="联系人" prop="contactName" width="120" />
            <el-table-column label="联系电话" prop="contactPhone" width="140" />
            <el-table-column label="绑定账号" width="110">
              <template #default="scope">
                <el-tag type="info">{{ accountCountMap[scope.row.id] || 0 }} 个</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="已授权活动" min-width="320">
              <template #default="scope">
                <el-space wrap>
                  <el-tag v-for="activityId in authorizedActivityIds(scope.row.id)" :key="activityId" type="success">
                    {{ activityNameMap[activityId] || activityId }}
                  </el-tag>
                  <span v-if="authorizedActivityIds(scope.row.id).length === 0" class="text-muted">暂无授权</span>
                </el-space>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right" align="center">
              <template #default="scope">
                <el-button
                  v-hasPermi="['crehn:activityScope:edit']"
                  link
                  type="primary"
                  icon="Edit"
                  :disabled="!isEnabledSchool(scope.row)"
                  @click="openSchoolAssign(scope.row)"
                >
                  授权活动
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="按活动授权" name="activity">
        <el-alert title="按单位授权，账号继承单位授权" type="info" show-icon :closable="false" class="mb-[10px]" />
        <el-card shadow="hover" class="mb-[10px]">
          <el-form :model="queryParams" :inline="true">
            <el-form-item label="活动">
              <el-select v-model="queryParams.activityId" clearable filterable placeholder="活动简称/名称" style="width: 240px">
                <el-option v-for="item in activityOptions" :key="item.id" :label="activityLabel(item)" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="单位">
              <el-select v-model="queryParams.schoolId" clearable filterable placeholder="单位名称" style="width: 240px">
                <el-option v-for="item in schoolOptions" :key="item.id" :label="schoolLabel(item)" :value="item.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
              <el-button type="primary" plain icon="Plus" @click="openAssign">批量授权</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="hover">
          <el-table border :data="rows">
            <el-table-column label="活动" min-width="220">
              <template #default="scope">{{ activityNameMap[scope.row.activityId] || scope.row.activityId }}</template>
            </el-table-column>
            <el-table-column label="单位" min-width="220">
              <template #default="scope">{{ schoolNameMap[scope.row.schoolId] || scope.row.schoolId }}</template>
            </el-table-column>
            <el-table-column label="启用" prop="enabled" width="90">
              <template #default="scope"><el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="授权时间" prop="assignedAt" width="180" />
            <el-table-column label="备注" prop="remark" min-width="180" />
            <el-table-column label="操作" width="90" fixed="right" align="center">
              <template #default="scope">
                <el-button v-hasPermi="['crehn:activityScope:remove']" link type="danger" icon="Delete" @click="removeScope(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <ArtPopupDialog v-model="schoolDialog.visible" title="按单位授权活动" width="720px" append-to-body>
      <el-form :model="schoolForm" label-width="90px">
        <el-form-item label="单位"><el-input v-model="schoolForm.schoolName" disabled /></el-form-item>
        <el-form-item label="授权活动">
          <el-checkbox-group v-model="schoolForm.activityIds" class="activity-check-list">
            <el-checkbox v-for="item in activityOptions" :key="item.id" :value="item.id" border>
              {{ activityLabel(item) }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="schoolForm.remark" placeholder="选填，授权说明" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitSchoolAssign">保存授权</el-button>
        <el-button @click="schoolDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="dialog.visible" title="批量授权活动给单位" width="860px" append-to-body>
      <el-form :model="form" label-width="90px">
        <el-form-item label="活动" required>
          <el-select v-model="form.activityId" filterable placeholder="选择活动简称/名称" class="w-full">
            <el-option v-for="item in activityOptions" :key="item.id" :label="activityLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位" required>
          <el-table ref="schoolTableRef" border height="320" :data="schoolOptions" @selection-change="onSchoolSelectionChange">
            <el-table-column type="selection" width="48" :selectable="isEnabledSchool" />
            <el-table-column label="单位名称" prop="schoolName" min-width="220" />
            <el-table-column label="院校类型" prop="schoolType" width="120" />
            <el-table-column label="状态" prop="status" width="100">
              <template #default="scope">
                <el-tag :type="schoolStatusTag(scope.row.status)">{{ schoolStatusText(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="联系人" prop="contactName" width="120" />
            <el-table-column label="联系电话" prop="contactPhone" width="140" />
          </el-table>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitAssign">授权选中单位</el-button>
        <el-button @click="dialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ArtActivityScope" lang="ts">
import { listActivityOptions } from '@/api/crehn/activity';
import { listSchoolAccount } from '@/api/crehn/schoolAccount';
import { assignActivityScope, deleteActivityScope, listActivityScope, listSchoolOptions } from '@/api/crehn/config';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const activeTab = ref('school');
const rows = ref<any[]>([]);
const activityOptions = ref<any[]>([]);
const schoolOptions = ref<any[]>([]);
const accountRows = ref<any[]>([]);
const selectedSchools = ref<any[]>([]);
const queryParams = reactive<any>({});
const schoolQuery = reactive<any>({ schoolName: '', schoolType: '' });
const schoolTypeOptions = ['本科院校', '高职高专'];
const dialog = reactive({ visible: false });
const schoolDialog = reactive({ visible: false });
const form = ref<any>({});
const schoolForm = ref<any>({});

const activityNameMap = computed<Record<string, string>>(() =>
  Object.fromEntries(activityOptions.value.map((item) => [item.id, activityLabel(item)]))
);
const schoolNameMap = computed<Record<string, string>>(() =>
  Object.fromEntries(schoolOptions.value.map((item) => [item.id, schoolLabel(item)]))
);
const accountCountMap = computed<Record<string, number>>(() => {
  const map: Record<string, number> = {};
  accountRows.value.forEach((item) => {
    if (item.schoolId) {
      map[item.schoolId] = (map[item.schoolId] || 0) + 1;
    }
  });
  return map;
});
const schoolRows = computed(() =>
  schoolOptions.value.filter((item) => {
    const nameOk = !schoolQuery.schoolName || item.schoolName?.includes(schoolQuery.schoolName);
    const typeOk = !schoolQuery.schoolType || item.schoolType === schoolQuery.schoolType;
    return nameOk && typeOk;
  })
);

const getOptions = async () => {
  const activityRes = await listActivityOptions();
  activityOptions.value = activityRes.data || [];
  const schoolRes = await listSchoolOptions({});
  schoolOptions.value = schoolRes.data || [];
  try {
    const accountRes: any = await listSchoolAccount({ pageNum: 1, pageSize: 2000 });
    accountRows.value = accountRes.rows || [];
  } catch (e) {
    accountRows.value = [];
  }
};

const getList = async () => {
  const { data } = await listActivityScope(queryParams);
  rows.value = data || [];
};

const loadSchoolRows = async () => {
  await getOptions();
  await getList();
};

const resetSchoolQuery = () => {
  schoolQuery.schoolName = '';
  schoolQuery.schoolType = '';
  loadSchoolRows();
};

const authorizedScopes = (schoolId: string | number) => rows.value.filter((item) => item.schoolId === schoolId && item.enabled);
const authorizedActivityIds = (schoolId: string | number) => authorizedScopes(schoolId).map((item) => item.activityId);
const isEnabledSchool = (school: any) => school?.status === 'enabled';
const schoolStatusText = (status?: string) => {
  const map: Record<string, string> = {
    enabled: '启用',
    disabled: '停用',
    recycled: '回收站'
  };
  return map[status || ''] || status || '未知';
};
const schoolStatusTag = (status?: string) => {
  if (status === 'enabled') {
    return 'success';
  }
  if (status === 'recycled') {
    return 'danger';
  }
  return 'info';
};

const openSchoolAssign = (school: any) => {
  if (!isEnabledSchool(school)) {
    proxy?.$modal.msgError('只能给启用状态的单位授权活动');
    return;
  }
  schoolForm.value = {
    schoolId: school.id,
    schoolName: schoolLabel(school),
    activityIds: authorizedActivityIds(school.id),
    originalActivityIds: authorizedActivityIds(school.id),
    remark: ''
  };
  schoolDialog.visible = true;
};

const submitSchoolAssign = async () => {
  const schoolId = schoolForm.value.schoolId;
  const school = schoolOptions.value.find((item) => item.id === schoolId);
  if (!isEnabledSchool(school)) {
    proxy?.$modal.msgError('只能给启用状态的单位授权活动');
    return;
  }
  const nextIds = schoolForm.value.activityIds || [];
  const currentScopes = authorizedScopes(schoolId);
  const currentIds = currentScopes.map((item) => item.activityId);
  const addIds = nextIds.filter((id: any) => !currentIds.includes(id));
  const removeScopes = currentScopes.filter((scope) => !nextIds.includes(scope.activityId));
  for (const activityId of addIds) {
    await assignActivityScope({ activityId, schoolIds: [schoolId], remark: schoolForm.value.remark });
  }
  if (removeScopes.length > 0) {
    await deleteActivityScope(removeScopes.map((item) => item.id));
  }
  proxy?.$modal.msgSuccess('授权已保存');
  schoolDialog.visible = false;
  await getList();
};

const openAssign = () => {
  form.value = { activityId: queryParams.activityId, remark: '' };
  selectedSchools.value = [];
  dialog.visible = true;
};

const onSchoolSelectionChange = (selection: any[]) => {
  selectedSchools.value = selection;
};

const submitAssign = async () => {
  if (!form.value.activityId) {
    proxy?.$modal.msgError('请选择活动');
    return;
  }
  const schoolIds = selectedSchools.value.map((item) => item.id);
  if (schoolIds.length === 0) {
    proxy?.$modal.msgError('请至少选择一个单位');
    return;
  }
  if (selectedSchools.value.some((item) => !isEnabledSchool(item))) {
    proxy?.$modal.msgError('只能给启用状态的单位授权活动');
    return;
  }
  await assignActivityScope({ ...form.value, schoolIds });
  proxy?.$modal.msgSuccess('授权成功');
  dialog.visible = false;
  await getList();
};

const removeScope = async (row: any) => {
  await proxy?.$modal.confirm(`确认删除「${schoolNameMap.value[row.schoolId] || row.schoolId}」的活动授权？`);
  await deleteActivityScope(row.id);
  proxy?.$modal.msgSuccess('删除成功');
  await getList();
};

const activityLabel = (item: any) => item.edition ? `${item.edition} - ${item.activityName}` : item.activityName;
const schoolLabel = (item: any) => item.schoolType ? `${item.schoolName}（${item.schoolType}）` : item.schoolName;

onMounted(async () => {
  await getOptions();
  await getList();
});
</script>

<style scoped>
.activity-check-list {
  display: grid;
  width: 100%;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 8px;
}

.activity-check-list :deep(.el-checkbox) {
  margin-right: 0;
}

.text-muted {
  color: var(--el-text-color-secondary);
}
</style>
