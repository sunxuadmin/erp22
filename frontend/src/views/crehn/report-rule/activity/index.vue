<template>
  <div class="p-2 activity-report-rule-page">
    <el-card shadow="hover" class="mb-[10px]">
      <el-form :model="query" :inline="true">
        <el-form-item label="活动">
          <el-select v-model="query.activityId" filterable placeholder="选择活动" style="width: 280px" @change="handleActivityChange">
            <el-option v-for="item in activities" :key="item.id" :label="activityLabel(item)" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则包">
          <el-select v-model="applyForm.packageId" clearable filterable placeholder="选择规则包" style="width: 260px">
            <el-option v-for="item in packages" :key="item.id" :label="packageLabel(item)" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="applyForm.overwrite">覆盖当前活动规则</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="loadRules">搜索</el-button>
          <el-button type="primary" plain icon="Connection" :disabled="!query.activityId || !applyForm.packageId" @click="applyPackage">应用规则包</el-button>
          <el-button type="primary" plain icon="RefreshRight" :disabled="!query.activityId" @click="restoreDefaultRules">恢复默认规则</el-button>
          <el-button type="primary" plain icon="Plus" :disabled="!query.activityId" @click="openRule()">新增规则</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <el-table v-loading="loading" border :data="rows" height="650">
        <el-table-column label="规则名称" prop="ruleName" min-width="200" show-overflow-tooltip />
        <el-table-column label="分组" width="90">
          <template #default="scope">{{ ruleGroupLabel(scope.row.ruleGroup) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="scope">{{ ruleTypeLabel(scope.row.ruleType) }}</template>
        </el-table-column>
        <el-table-column label="院校类型" width="120">
          <template #default="scope">{{ scope.row.schoolType || '全部' }}</template>
        </el-table-column>
        <el-table-column label="类别" min-width="150">
          <template #default="scope">{{ categoryName(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="统计字段" width="130">
          <template #default="scope">{{ scope.row.targetFieldKey || ruleJsonValue(scope.row.ruleJson, 'targetFieldKey') || '顶层组别' }}</template>
        </el-table-column>
        <el-table-column label="目标值" width="150">
          <template #default="scope">{{ scope.row.targetValue || '全部' }}</template>
        </el-table-column>
        <el-table-column label="范围" min-width="150">
          <template #default="scope">{{ scope.row.scopeCategoryGroup || ruleJsonValue(scope.row.ruleJson, 'scopeCategoryGroup') || '当前活动' }}</template>
        </el-table-column>
        <el-table-column label="限制" width="120">
          <template #default="scope">{{ limitLabel(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="执行" width="92">
          <template #default="scope">
            <el-tag :type="enforceTag(scope.row.enforceMode)">{{ enforceLabel(scope.row.enforceMode) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="72">
          <template #default="scope">
            <el-switch v-model="scope.row.enabled" @change="toggleRule(scope.row)" />
          </template>
        </el-table-column>
        <el-table-column label="提示" prop="message" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="132" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="openRule(scope.row)" />
            <el-button link type="danger" icon="Delete" @click="removeRule(scope.row)" />
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="loadRules" />
    </el-card>

    <ArtPopupDialog v-model="dialog.visible" :title="dialog.title" width="980px" append-to-body>
      <el-form :model="form" label-width="116px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="规则名称" required><el-input v-model="form.ruleName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="规则编码"><el-input v-model="form.ruleCode" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="规则分组">
              <el-select v-model="form.ruleGroup" class="w-full">
                <el-option label="名额配置" value="quota" />
                <el-option label="比例配置" value="ratio" />
                <el-option label="成员配置" value="member" />
                <el-option label="附件配置" value="file" />
                <el-option label="特殊要求" value="special" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="规则类型">
              <el-select v-model="form.ruleType" class="w-full">
                <el-option label="数量上限" value="count" />
                <el-option label="比例限制" value="ratio" />
                <el-option label="名额数量" value="quota_count" />
                <el-option label="类别数量" value="category_count" />
                <el-option label="总量数量" value="total_count" />
                <el-option label="成员/作者" value="member" />
                <el-option label="附件材料" value="file" />
                <el-option label="自定义" value="custom" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="执行方式">
              <el-select v-model="form.enforceMode" class="w-full">
                <el-option label="提醒" value="warn" />
                <el-option label="强制执行" value="block" />
                <el-option label="人工复核" value="manual" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="院校类型"><el-input v-model="form.schoolType" placeholder="本科院校/高职高专院校" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="活动类别">
              <el-select v-model="form.categoryId" clearable filterable class="w-full" placeholder="全部类别" @change="syncCategoryCode">
                <el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="item.id!" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="类别编码"><el-input v-model="form.categoryCode" placeholder="跨活动复用时使用" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="大类范围"><el-input v-model="form.scopeCategoryGroup" placeholder="艺术表演类" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="统计字段"><el-input v-model="form.targetFieldKey" placeholder="categoryCode/displayGroup" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="目标值"><el-input v-model="form.targetValue" placeholder="甲组/performance_personal" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="比例方向">
              <el-select v-model="form.operator" clearable class="w-full">
                <el-option label="不超过" value="max" />
                <el-option label="不低于" value="min" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="数量上限"><el-input-number v-model="form.limitCount" class="w-full" :min="0" :precision="0" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="比例值%"><el-input-number v-model="form.ratioValue" class="w-full" :min="0" :max="100" :precision="2" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="排序"><el-input-number v-model="form.sortOrder" class="w-full" :precision="0" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="提示文案"><el-input v-model="form.message" type="textarea" :rows="2" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="扩展 JSON"><el-input v-model="form.ruleJson" type="textarea" :rows="4" placeholder='例如 {"excludeTargetValue":"performance_personal"}' /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitRule">确定</el-button>
        <el-button @click="dialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ActivityReportRule" lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { listActivityOptions, listCategoryOptions } from '@/api/crehn/activity';
import {
  applyReportRulePackage,
  deleteActivityReportRule,
  listActivityReportRule,
  listReportRulePackage,
  restoreDefaultActivityReportRule,
  saveActivityReportRule
} from '@/api/crehn/config';
import type { ActivityCategoryVO, ActivityReportRuleVO, ActivityVO, ReportRulePackageVO } from '@/api/crehn/types';

const DEFAULT_PACKAGE_CODE = 'henan_art_show_default_2026';
const loading = ref(false);
const activities = ref<ActivityVO[]>([]);
const packages = ref<ReportRulePackageVO[]>([]);
const categories = ref<ActivityCategoryVO[]>([]);
const rows = ref<ActivityReportRuleVO[]>([]);
const total = ref(0);

const query = reactive({
  pageNum: 1,
  pageSize: 20,
  activityId: undefined as string | number | undefined
});

const applyForm = reactive({
  packageId: undefined as string | number | undefined,
  overwrite: true
});

const dialog = reactive({ visible: false, title: '' });
const form = ref<ActivityReportRuleVO>({});

const loadActivities = async () => {
  const { data } = await listActivityOptions();
  activities.value = data || [];
  if (!query.activityId && activities.value.length) {
    query.activityId = activities.value[0].id;
  }
};

const loadPackages = async () => {
  const res: any = await listReportRulePackage({ pageNum: 1, pageSize: 1000, enabled: true });
  packages.value = res.rows || res.data || [];
  if (!applyForm.packageId) {
    const defaultPackage = packages.value.find((item) => item.packageCode === DEFAULT_PACKAGE_CODE) || packages.value[0];
    applyForm.packageId = defaultPackage?.id;
  }
};

const loadCategories = async () => {
  if (!query.activityId) {
    categories.value = [];
    return;
  }
  const { data } = await listCategoryOptions(query.activityId);
  categories.value = data || [];
};

const handleActivityChange = async () => {
  query.pageNum = 1;
  await loadCategories();
  await loadRules();
};

const loadRules = async () => {
  if (!query.activityId) {
    rows.value = [];
    return;
  }
  loading.value = true;
  try {
    const res: any = await listActivityReportRule(query);
    rows.value = res.rows || res.data || [];
    total.value = res.total || rows.value.length;
  } finally {
    loading.value = false;
  }
};

const applyPackage = async () => {
  await ElMessageBox.confirm('确认把所选规则包应用到当前活动？覆盖后当前活动已有新版规则会被替换。', '应用规则包', { type: 'warning' });
  await applyReportRulePackage({
    activityId: query.activityId,
    packageId: applyForm.packageId,
    overwrite: applyForm.overwrite
  });
  ElMessage.success('应用成功');
  await loadRules();
};

const restoreDefaultRules = async () => {
  const modeText = applyForm.overwrite ? '覆盖当前活动已有规则后恢复默认规则' : '保留当前活动已有规则，仅补齐缺少的默认规则';
  await ElMessageBox.confirm(`确认${modeText}？默认规则会保持“提醒”执行方式，可在列表中改为强制执行。`, '恢复默认规则', { type: 'warning' });
  await restoreDefaultActivityReportRule({
    activityId: query.activityId,
    overwrite: applyForm.overwrite
  });
  ElMessage.success('默认规则已恢复');
  await loadPackages();
  const defaultPackage = packages.value.find((item) => item.packageCode === DEFAULT_PACKAGE_CODE);
  if (defaultPackage?.id) {
    applyForm.packageId = defaultPackage.id;
  }
  await loadRules();
};

const openRule = (row?: ActivityReportRuleVO) => {
  if (!query.activityId) {
    ElMessage.warning('请先选择活动');
    return;
  }
  form.value = {
    activityId: query.activityId,
    ruleGroup: 'quota',
    ruleType: 'count',
    enforceMode: 'warn',
    enabled: true,
    sortOrder: rows.value.length + 1,
    ...row
  };
  dialog.title = row?.id ? '修改活动规则' : '新增活动规则';
  dialog.visible = true;
};

const submitRule = async () => {
  form.value.activityId = query.activityId;
  await saveActivityReportRule(form.value);
  ElMessage.success('保存成功');
  dialog.visible = false;
  await loadRules();
};

const toggleRule = async (row: ActivityReportRuleVO) => {
  await saveActivityReportRule(row);
  ElMessage.success(row.enabled ? '已启用' : '已停用');
};

const removeRule = async (row: ActivityReportRuleVO) => {
  await ElMessageBox.confirm(`确认删除规则“${row.ruleName}”？`, '删除确认', { type: 'warning' });
  await deleteActivityReportRule(row.id!);
  ElMessage.success('删除成功');
  await loadRules();
};

const syncCategoryCode = () => {
  const selected = categories.value.find((item) => item.id === form.value.categoryId);
  if (selected?.categoryCode) {
    form.value.categoryCode = selected.categoryCode;
  }
};

const activityLabel = (item: ActivityVO) => item.menuName || `${item.activityName || '活动'}${item.year ? `（${item.year}）` : ''}`;
const packageLabel = (item: ReportRulePackageVO) => `${item.packageName || item.packageCode}${item.versionNo ? ` / ${item.versionNo}` : ''}`;
const categoryName = (row: ActivityReportRuleVO) => {
  const category = categories.value.find((item) => item.id === row.categoryId || item.categoryCode === row.categoryCode);
  return category?.categoryName || row.categoryCode || '全部类别';
};
const ruleJsonValue = (ruleJson?: string, key?: string) => {
  if (!ruleJson || !key) return '';
  try {
    const parsed = JSON.parse(ruleJson);
    return parsed?.[key] || '';
  } catch {
    return '';
  }
};
const ruleGroupLabel = (value?: string) => ({ quota: '名额', ratio: '比例', member: '成员', file: '附件', special: '特殊' })[value || 'quota'] || value || '名额';
const ruleTypeLabel = (value?: string) =>
  ({ count: '数量', ratio: '比例', quota_count: '名额', category_count: '类别数', total_count: '总数', member: '成员', file: '附件', custom: '自定义' })[value || 'count'] ||
  value ||
  '数量';
const enforceLabel = (value?: string) => ({ warn: '提醒', block: '强制', manual: '复核' })[value || 'warn'] || value || '提醒';
const enforceTag = (value?: string) => (value === 'block' ? 'danger' : value === 'manual' ? 'warning' : 'success');
const limitLabel = (row: ActivityReportRuleVO) => {
  if (row.limitCount != null) return `≤ ${row.limitCount}`;
  if (row.ratioValue != null) return `${row.operator === 'min' ? '≥' : '≤'} ${row.ratioValue}%`;
  if (row.minValue != null || row.maxValue != null) return `${row.minValue ?? '-'} ~ ${row.maxValue ?? '-'}`;
  return '-';
};

onMounted(async () => {
  await Promise.all([loadActivities(), loadPackages()]);
  await loadCategories();
  await loadRules();
});
</script>

<style scoped>
.activity-report-rule-page :deep(.el-card__body) {
  padding: 12px;
}
</style>
