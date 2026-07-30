<template>
  <div class="p-2">
    <el-card shadow="hover" class="mb-[10px]">
      <el-form :model="queryParams" :inline="true">
        <el-form-item label="单位名称">
          <el-input v-model="queryParams.schoolName" clearable placeholder="请输入单位名称" @keyup.enter="getList" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="启用" value="enabled" />
            <el-option label="停用" value="disabled" />
            <el-option label="回收站" value="recycled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button v-hasPermi="['crehn:schoolInfo:edit']" type="primary" plain icon="Plus" @click="openForm()">添加单位</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <el-table v-loading="loading" border :data="rows">
        <el-table-column label="单位名称" prop="schoolName" min-width="220" />
        <el-table-column label="省市区" prop="region" min-width="160" show-overflow-tooltip />
        <el-table-column label="单位地址" prop="address" min-width="220" show-overflow-tooltip />
        <el-table-column label="单位电话" prop="contactPhone" width="150" />
        <el-table-column label="负责人" prop="contactName" width="140" />
        <el-table-column label="状态" prop="status" width="90">
          <template #default="scope">
            <el-tag :type="schoolStatusType(scope.row.status)">{{ schoolStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="影响统计" min-width="220">
          <template #default="scope">
            <div class="impact-counts">
              <el-tag size="small" type="info">账号 {{ numberText(scope.row.accountCount) }}</el-tag>
              <el-tag size="small" type="warning">项目 {{ numberText(scope.row.projectCount) }}</el-tag>
              <el-tag size="small" type="success">附件 {{ numberText(scope.row.fileCount) }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
        <el-table-column label="创建时间" prop="createTime" width="170" />
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="scope">
            <el-button v-hasPermi="['crehn:schoolInfo:edit']" link type="primary" icon="Edit" @click="openForm(scope.row)">编辑</el-button>
            <el-button
              v-if="scope.row.status !== 'recycled'"
              v-hasPermi="['crehn:schoolInfo:remove']"
              link
              type="danger"
              icon="Delete"
              @click="moveToRecycle(scope.row)"
            >
              删除
            </el-button>
            <el-button
              v-else
              v-hasPermi="['crehn:schoolInfo:edit']"
              link
              type="success"
              icon="RefreshLeft"
              @click="restoreSchool(scope.row)"
            >
              恢复
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <ArtPopupDialog v-model="dialog.visible" :title="dialog.title" width="860px" append-to-body>
      <el-form ref="schoolFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="单位名称" prop="schoolName">
          <el-input v-model="form.schoolName" placeholder="请输入单位名称，不能重名" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="省市区">
          <div class="w-full">
            <el-radio-group v-model="regionMode" class="mb-2">
              <el-radio-button label="standard">标准地区</el-radio-button>
              <el-radio-button label="custom">自定义地区</el-radio-button>
            </el-radio-group>
            <el-cascader
              v-if="regionMode === 'standard'"
              v-model="regionCodes"
              class="w-full"
              :options="regionOptions"
              filterable
              clearable
              :props="{ checkStrictly: false }"
              placeholder="请选择省 / 市 / 区县"
              @change="syncStandardRegion"
            />
            <el-row v-else :gutter="10">
              <el-col :span="8"><el-input v-model="addressParts.province" placeholder="省 / 自治区 / 直辖市" /></el-col>
              <el-col :span="8"><el-input v-model="addressParts.city" placeholder="市 / 州 / 盟" /></el-col>
              <el-col :span="8"><el-input v-model="addressParts.district" placeholder="区 / 县 / 乡镇" /></el-col>
            </el-row>
          </div>
        </el-form-item>
        <el-form-item label="街道地址">
          <el-input v-model="form.address" placeholder="选填，街道、门牌号或详细地址" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="单位电话">
          <el-input v-model="form.contactPhone" placeholder="选填" maxlength="32" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.contactName" placeholder="选填" maxlength="64" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="选填" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="enabledSwitch" :disabled="form.status === 'recycled'" active-text="启用" inactive-text="停用" />
          <el-alert
            v-if="form.status === 'recycled'"
            class="mt-2"
            type="warning"
            show-icon
            :closable="false"
            title="该单位在回收站中，关联账号仍保留，但不能继续申报。请先恢复后再启用。"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">保存</el-button>
        <el-button @click="dialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ArtSchoolInfo" lang="ts">
import { codeToText, regionData } from 'element-china-area-data';
import type { CascaderOption } from 'element-plus';
import { deleteSchoolInfo, getSchoolInfoImpact, listSchoolInfo, restoreSchoolInfo, saveSchoolInfo } from '@/api/crehn/config';
import { useRouter } from 'vue-router';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const router = useRouter();
const toCascaderOption = (item: (typeof regionData)[number]): CascaderOption => ({
  value: item.value,
  label: item.label,
  children: item.children?.map(toCascaderOption)
});
const regionOptions: CascaderOption[] = regionData.map(toCascaderOption);

const loading = ref(false);
const rows = ref<any[]>([]);
const total = ref(0);
const schoolFormRef = ref<ElFormInstance>();
const dialog = reactive({ visible: false, title: '' });
const queryParams = reactive<any>({ pageNum: 1, pageSize: 10, schoolName: '', status: '' });
const form = ref<any>({});
const addressParts = reactive({ province: '', city: '', district: '' });
const regionMode = ref<'standard' | 'custom'>('standard');
const regionCodes = ref<string[]>([]);

const enabledSwitch = computed({
  get: () => form.value.status === 'enabled',
  set: (value: boolean) => {
    form.value.status = value ? 'enabled' : 'disabled';
  }
});

const rules: ElFormRules = {
  schoolName: [{ required: true, message: '请输入单位名称', trigger: 'blur' }]
};

const getList = async () => {
  loading.value = true;
  try {
    const res = await listSchoolInfo(queryParams);
    rows.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const resetQuery = () => {
  queryParams.schoolName = '';
  queryParams.status = '';
  queryParams.pageNum = 1;
  getList();
};

const openForm = (row?: any) => {
  form.value = row ? { ...row } : { schoolName: '', address: '', contactPhone: '', contactName: '', remark: '', status: 'enabled' };
  loadAddressParts(form.value);
  dialog.title = row ? '编辑单位' : '添加单位';
  dialog.visible = true;
  nextTick(() => schoolFormRef.value?.clearValidate());
};

const submitForm = async () => {
  await schoolFormRef.value?.validate();
  if (regionMode.value === 'standard') {
    syncStandardRegion();
  }
  form.value.region = [addressParts.province, addressParts.city, addressParts.district].filter(Boolean).join('/');
  form.value.profileJson = JSON.stringify({
    ...parseProfile(form.value.profileJson),
    province: addressParts.province,
    city: addressParts.city,
    district: addressParts.district,
    regionMode: regionMode.value,
    regionCodes: regionCodes.value,
    street: form.value.address || ''
  });
  await saveSchoolInfo(form.value);
  proxy?.$modal.msgSuccess('保存成功');
  dialog.visible = false;
  await getList();
};

const moveToRecycle = async (row: any) => {
  const impact = await fetchSchoolImpact(row);
  const hasImpact = hasAccountOrProject(impact);
  if (hasImpact) {
    await proxy?.$modal.confirm(
      `单位「${row.schoolName}」下存在数据：${impactText(impact)}。\n删除单位不会删除填报或附件，但账号会因单位失效不能继续申报。\n请先处理账号，或继续二次确认移入回收站。`
    );
    await proxy?.$modal.confirm(`二次确认：仍要将单位「${row.schoolName}」移入回收站吗？`);
  } else {
    await proxy?.$modal.confirm(`确认将单位「${row.schoolName}」移入回收站？不会删除填报或附件。`);
  }
  await deleteSchoolInfo(row.id, hasImpact);
  proxy?.$modal.msgSuccess('已移入回收站');
  await getList();
};

const restoreSchool = async (row: any) => {
  await proxy?.$modal.confirm(`确认恢复单位「${row.schoolName}」？恢复后可重新启用关联账号的申报资格。`);
  await restoreSchoolInfo(row.id);
  proxy?.$modal.msgSuccess('恢复成功');
  await getList();
  try {
    await proxy?.$modal.confirm(`是否前往账号管理，为「${row.schoolName}」恢复或创建学校账号并重新授权？`);
    await openAccountAssist(row);
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

const openAccountAssist = async (row: any) => {
  await router.push({
    path: '/crehn/school-account',
    query: {
      mode: 'create',
      schoolId: row.id,
      schoolName: row.schoolName
    }
  });
};

const schoolStatusLabel = (status?: string) => {
  const map: Record<string, string> = { enabled: '启用', disabled: '停用', recycled: '已回收' };
  return map[status || ''] || status || '-';
};

const schoolStatusType = (status?: string): ElTagType => {
  const map: Record<string, ElTagType> = { enabled: 'success', disabled: 'info', recycled: 'danger' };
  return map[status || ''] || 'info';
};

const loadAddressParts = (row: any) => {
  const profile = parseProfile(row?.profileJson);
  const regionParts = String(row?.region || '').split('/').filter(Boolean);
  addressParts.province = profile.province || regionParts[0] || '';
  addressParts.city = profile.city || regionParts[1] || '';
  addressParts.district = profile.district || regionParts[2] || '';
  regionCodes.value = Array.isArray(profile.regionCodes) ? profile.regionCodes : findRegionCodes(addressParts.province, addressParts.city, addressParts.district);
  regionMode.value = profile.regionMode || (regionCodes.value.length ? 'standard' : 'custom');
};

const parseProfile = (value?: string) => {
  if (!value) return {};
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
};

const syncStandardRegion = () => {
  addressParts.province = codeText(regionCodes.value[0]);
  addressParts.city = codeText(regionCodes.value[1]);
  addressParts.district = codeText(regionCodes.value[2]);
};

const codeText = (code?: string) => {
  return code ? codeToText[code] || '' : '';
};

const findRegionCodes = (province?: string, city?: string, district?: string) => {
  const provinceNode = regionData.find((item: any) => item.label === province);
  const cityNode = provinceNode?.children?.find((item: any) => item.label === city);
  const districtNode = cityNode?.children?.find((item: any) => item.label === district);
  return [provinceNode?.value, cityNode?.value, districtNode?.value].filter(Boolean);
};

onMounted(getList);
</script>

<style scoped>
.impact-counts {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}
</style>
