<template>
  <div class="p-2 report-rule-page">
    <el-card shadow="hover" class="mb-[10px]">
      <el-form :model="packageQuery" :inline="true">
        <el-form-item label="规则包">
          <el-input v-model="packageQuery.packageName" clearable placeholder="名称/用途" @keyup.enter="loadPackages" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="packageQuery.status" clearable style="width: 140px">
            <el-option label="草稿" value="draft" />
            <el-option label="已发布" value="published" />
            <el-option label="停用" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="loadPackages">搜索</el-button>
          <el-button type="primary" plain icon="Plus" @click="openPackage()">新增规则包</el-button>
          <el-button plain icon="RefreshRight" @click="restoreDefaultPackage">恢复默认规则包</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="10">
      <el-col :span="9">
        <el-card shadow="hover" class="rule-panel">
          <el-table v-loading="packageLoading" border :data="packages" height="620" highlight-current-row @current-change="selectPackage">
            <el-table-column label="规则包名称" prop="packageName" min-width="180" show-overflow-tooltip />
            <el-table-column label="版本" prop="versionNo" width="90" />
            <el-table-column label="状态" width="90">
              <template #default="scope">
                <el-tag :type="packageStatusType(scope.row.status)">{{ packageStatusLabel(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="启用" width="72">
              <template #default="scope">
                <el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right" align="center">
              <template #default="scope">
                <el-button link type="primary" icon="Edit" @click.stop="openPackage(scope.row)" />
                <el-button link type="danger" icon="Delete" @click.stop="removePackage(scope.row)" />
              </template>
            </el-table-column>
          </el-table>
          <pagination v-show="packageTotal > 0" v-model:page="packageQuery.pageNum" v-model:limit="packageQuery.pageSize" :total="packageTotal" @pagination="loadPackages" />
        </el-card>
      </el-col>

      <el-col :span="15">
        <el-card shadow="hover" class="rule-panel">
          <template #header>
            <div class="panel-head">
              <span>{{ currentPackage?.packageName || '规则明细' }}</span>
              <div>
                <el-button :disabled="!currentPackage?.id" type="primary" plain icon="Plus" @click="openItem()">新增规则</el-button>
                <el-button :disabled="!currentPackage?.id" plain icon="Refresh" @click="loadItems">刷新</el-button>
              </div>
            </div>
          </template>
          <el-table v-loading="itemLoading" border :data="items" height="568">
            <el-table-column label="规则名称" prop="ruleName" min-width="190" show-overflow-tooltip />
            <el-table-column label="分组" width="110">
              <template #default="scope">{{ ruleGroupLabel(scope.row.ruleGroup) }}</template>
            </el-table-column>
            <el-table-column label="类型" width="110">
              <template #default="scope">{{ ruleTypeLabel(scope.row.ruleType) }}</template>
            </el-table-column>
            <el-table-column label="院校类型" width="110">
              <template #default="scope">{{ scope.row.schoolType || '全部' }}</template>
            </el-table-column>
            <el-table-column label="范围" min-width="150">
              <template #default="scope">{{ scope.row.scopeCategoryGroup || scope.row.categoryCode || '当前活动' }}</template>
            </el-table-column>
            <el-table-column label="限制" width="120">
              <template #default="scope">{{ limitLabel(scope.row) }}</template>
            </el-table-column>
            <el-table-column label="执行" width="90">
              <template #default="scope">
                <el-tag :type="enforceTag(scope.row.enforceMode)">{{ enforceLabel(scope.row.enforceMode) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="启用" width="72">
              <template #default="scope">
                <el-switch v-model="scope.row.enabled" @change="toggleItem(scope.row)" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right" align="center">
              <template #default="scope">
                <el-button link type="primary" icon="Edit" @click="openItem(scope.row)" />
                <el-button link type="danger" icon="Delete" @click="removeItem(scope.row)" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <ArtPopupDialog v-model="packageDialog.visible" :title="packageDialog.title" width="720px" append-to-body>
      <el-form :model="packageForm" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="规则包名称" required><el-input v-model="packageForm.packageName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="规则包编码"><el-input v-model="packageForm.packageCode" placeholder="留空自动生成" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="活动类型"><el-input v-model="packageForm.activityType" placeholder="通用可留空" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="版本"><el-input v-model="packageForm.versionNo" placeholder="v1" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-select v-model="packageForm.status" class="w-full">
                <el-option label="草稿" value="draft" />
                <el-option label="已发布" value="published" />
                <el-option label="停用" value="disabled" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="启用"><el-switch v-model="packageForm.enabled" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="说明"><el-input v-model="packageForm.remark" type="textarea" :rows="3" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitPackage">确定</el-button>
        <el-button @click="packageDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="itemDialog.visible" :title="itemDialog.title" width="980px" append-to-body>
      <el-form :model="itemForm" label-width="116px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="规则名称" required><el-input v-model="itemForm.ruleName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="规则编码"><el-input v-model="itemForm.ruleCode" placeholder="留空亦可" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="规则分组">
              <el-select v-model="itemForm.ruleGroup" class="w-full">
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
              <el-select v-model="itemForm.ruleType" class="w-full">
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
              <el-select v-model="itemForm.enforceMode" class="w-full">
                <el-option label="提醒" value="warn" />
                <el-option label="强制执行" value="block" />
                <el-option label="人工复核" value="manual" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="院校类型"><el-input v-model="itemForm.schoolType" placeholder="本科院校/高职高专院校" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="大类范围"><el-input v-model="itemForm.scopeCategoryGroup" placeholder="艺术表演类" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="类别编码"><el-input v-model="itemForm.categoryCode" placeholder="performance_vocal" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="统计字段"><el-input v-model="itemForm.targetFieldKey" placeholder="categoryCode/displayGroup" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="目标值"><el-input v-model="itemForm.targetValue" placeholder="甲组/performance_personal" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="比例方向">
              <el-select v-model="itemForm.operator" clearable class="w-full">
                <el-option label="不超过" value="max" />
                <el-option label="不低于" value="min" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="数量上限"><el-input-number v-model="itemForm.limitCount" class="w-full" :min="0" :precision="0" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="比例值%"><el-input-number v-model="itemForm.ratioValue" class="w-full" :min="0" :max="100" :precision="2" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="排序"><el-input-number v-model="itemForm.sortOrder" class="w-full" :precision="0" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="启用"><el-switch v-model="itemForm.enabled" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="提示文案"><el-input v-model="itemForm.message" type="textarea" :rows="2" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="扩展 JSON"><el-input v-model="itemForm.ruleJson" type="textarea" :rows="4" placeholder='例如 {"excludeTargetValue":"performance_personal"}' /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="itemForm.remark" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitItem">确定</el-button>
        <el-button @click="itemDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ReportRulePackage" lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, reactive, ref } from 'vue';
import {
  deleteReportRuleItem,
  deleteReportRulePackage,
  listReportRuleItem,
  listReportRulePackage,
  restoreDefaultReportRulePackage,
  saveReportRuleItem,
  saveReportRulePackage
} from '@/api/crehn/config';
import type { ReportRuleItemVO, ReportRulePackageVO } from '@/api/crehn/types';

const DEFAULT_PACKAGE_CODE = 'henan_art_show_default_2026';
const packageLoading = ref(false);
const itemLoading = ref(false);
const packages = ref<ReportRulePackageVO[]>([]);
const items = ref<ReportRuleItemVO[]>([]);
const packageTotal = ref(0);
const currentPackage = ref<ReportRulePackageVO>();

const packageQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  packageName: '',
  status: ''
});

const packageDialog = reactive({ visible: false, title: '' });
const itemDialog = reactive({ visible: false, title: '' });
const packageForm = ref<ReportRulePackageVO>({});
const itemForm = ref<ReportRuleItemVO>({});

const loadPackages = async () => {
  packageLoading.value = true;
  try {
    const res: any = await listReportRulePackage(packageQuery);
    packages.value = res.rows || res.data || [];
    packageTotal.value = res.total || packages.value.length;
    if (!currentPackage.value && packages.value.length) {
      selectPackage(packages.value[0]);
    }
  } finally {
    packageLoading.value = false;
  }
};

const selectPackage = (row?: ReportRulePackageVO) => {
  currentPackage.value = row;
  loadItems();
};

const itemQuery = computed(() => ({
  pageNum: 1,
  pageSize: 1000,
  packageId: currentPackage.value?.id
}));

const loadItems = async () => {
  if (!currentPackage.value?.id) {
    items.value = [];
    return;
  }
  itemLoading.value = true;
  try {
    const res: any = await listReportRuleItem(itemQuery.value);
    items.value = res.rows || res.data || [];
  } finally {
    itemLoading.value = false;
  }
};

const openPackage = (row?: ReportRulePackageVO) => {
  packageForm.value = {
    status: 'draft',
    enabled: true,
    versionNo: 'v1',
    ...row
  };
  packageDialog.title = row?.id ? '修改规则包' : '新增规则包';
  packageDialog.visible = true;
};

const submitPackage = async () => {
  await saveReportRulePackage(packageForm.value);
  ElMessage.success('保存成功');
  packageDialog.visible = false;
  await loadPackages();
};

const restoreDefaultPackage = async () => {
  await ElMessageBox.confirm('将恢复并补齐系统默认规则包，已有默认规则会按最新文件要求更新，其他自定义规则不受影响。', '恢复默认规则包', { type: 'warning' });
  const res: any = await restoreDefaultReportRulePackage();
  ElMessage.success('默认规则包已恢复');
  currentPackage.value = undefined;
  await loadPackages();
  const restored = packages.value.find((item) => item.id === res.data || item.packageCode === DEFAULT_PACKAGE_CODE);
  if (restored) {
    selectPackage(restored);
  }
};

const removePackage = async (row: ReportRulePackageVO) => {
  await ElMessageBox.confirm(`确认删除规则包“${row.packageName}”？`, '删除确认', { type: 'warning' });
  await deleteReportRulePackage(row.id!);
  ElMessage.success('删除成功');
  if (currentPackage.value?.id === row.id) {
    currentPackage.value = undefined;
    items.value = [];
  }
  await loadPackages();
};

const openItem = (row?: ReportRuleItemVO) => {
  if (!currentPackage.value?.id) {
    ElMessage.warning('请先选择规则包');
    return;
  }
  itemForm.value = {
    packageId: currentPackage.value.id,
    ruleGroup: 'quota',
    ruleType: 'count',
    enforceMode: 'warn',
    enabled: true,
    sortOrder: items.value.length + 1,
    ...row
  };
  itemDialog.title = row?.id ? '修改规则' : '新增规则';
  itemDialog.visible = true;
};

const submitItem = async () => {
  itemForm.value.packageId = currentPackage.value?.id;
  await saveReportRuleItem(itemForm.value);
  ElMessage.success('保存成功');
  itemDialog.visible = false;
  await loadItems();
};

const toggleItem = async (row: ReportRuleItemVO) => {
  await saveReportRuleItem(row);
  ElMessage.success(row.enabled ? '已启用' : '已停用');
};

const removeItem = async (row: ReportRuleItemVO) => {
  await ElMessageBox.confirm(`确认删除规则“${row.ruleName}”？`, '删除确认', { type: 'warning' });
  await deleteReportRuleItem(row.id!);
  ElMessage.success('删除成功');
  await loadItems();
};

const packageStatusLabel = (value?: string) => ({ draft: '草稿', published: '已发布', disabled: '停用' })[value || 'draft'] || value || '草稿';
const packageStatusType = (value?: string) => (value === 'published' ? 'success' : value === 'disabled' ? 'info' : 'warning');
const ruleGroupLabel = (value?: string) => ({ quota: '名额', ratio: '比例', member: '成员', file: '附件', special: '特殊' })[value || 'quota'] || value || '名额';
const ruleTypeLabel = (value?: string) =>
  ({ count: '数量', ratio: '比例', quota_count: '名额', category_count: '类别数', total_count: '总数', member: '成员', file: '附件', custom: '自定义' })[value || 'count'] ||
  value ||
  '数量';
const enforceLabel = (value?: string) => ({ warn: '提醒', block: '强制', manual: '复核' })[value || 'warn'] || value || '提醒';
const enforceTag = (value?: string) => (value === 'block' ? 'danger' : value === 'manual' ? 'warning' : 'success');
const limitLabel = (row: ReportRuleItemVO) => {
  if (row.limitCount != null) return `≤ ${row.limitCount}`;
  if (row.ratioValue != null) return `${row.operator === 'min' ? '≥' : '≤'} ${row.ratioValue}%`;
  if (row.minValue != null || row.maxValue != null) return `${row.minValue ?? '-'} ~ ${row.maxValue ?? '-'}`;
  return '-';
};

onMounted(loadPackages);
</script>

<style scoped>
.report-rule-page :deep(.el-card__body) {
  padding: 12px;
}

.rule-panel {
  min-height: 686px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
</style>
