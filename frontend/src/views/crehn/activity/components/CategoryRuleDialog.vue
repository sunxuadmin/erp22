<template>
  <!-- ART-OWNER: FE.ACTIVITY_CONFIG.CATEGORY_RULE -->
  <ArtPopupDialog v-model="visibleModel" :title="title" width="760px" append-to-body>
    <el-form :model="categoryFormDraft" label-width="100px">
      <el-alert
        class="mb-3"
        type="info"
        :closable="false"
        :title="
          nodeTypeModel === 'group'
            ? '大类只作为学校菜单分组；空大类不会显示，有启用且菜单可见的子类别后自动显示。'
            : '报送类别可以选择所属大类，也可以不归属大类直接作为一级可点击菜单；类别编码建议保持稳定。'
        "
      />
      <el-form-item label="节点类型">
        <el-radio-group v-model="nodeTypeModel" :disabled="nodeTypeLocked">
          <el-radio-button value="group">大类</el-radio-button>
          <el-radio-button value="category">报送类别</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="nodeTypeModel === 'group' ? '大类编码' : '类别编码'">
        <el-input
          v-model="categoryFormDraft.categoryCode"
          :placeholder="nodeTypeModel === 'group' ? '如 performance_group' : '如 performance_instrumental'"
        />
      </el-form-item>
      <el-form-item :label="nodeTypeModel === 'group' ? '大类名称' : '类别名称'"><el-input v-model="categoryFormDraft.categoryName" /></el-form-item>
      <el-form-item v-if="nodeTypeModel === 'category'" label="所属大类">
        <el-select v-model="categoryParentKeyDraft" class="w-full" clearable filterable placeholder="不选择则直接作为一级菜单">
          <el-option v-for="item in categoryGroupNodes" :key="groupOptionKey(item)" :label="groupOptionLabel(item)" :value="groupOptionKey(item)" />
        </el-select>
      </el-form-item>
      <template v-if="nodeTypeModel === 'category'">
        <el-divider content-position="left">大艺展规则</el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="配置模板">
              <el-select
                v-model="categoryRuleFormDraft.templateCode"
                clearable
                filterable
                class="w-full"
                placeholder="选择后自动带出规则建议"
                @change="handleTemplateChange"
              >
                <el-option v-for="item in categoryRuleTemplateOptions" :key="item.templateCode" :label="item.label" :value="item.templateCode" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="校验器">
              <el-select v-model="categoryRuleFormDraft.validatorType" clearable class="w-full" @change="handleValidatorChange">
                <el-option v-for="item in validatorTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="categoryRuleFormDraft.validatorType && categoryRuleFormDraft.validatorType !== 'none'" :span="12">
            <el-form-item label="校验器名称">
              <el-input v-model="categoryRuleFormDraft.validatorLabel" placeholder="用于后台展示，不影响校验编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12"
            ><el-form-item label="主名称字段标签"
              ><el-input v-model="categoryRuleFormDraft.projectNameLabel" placeholder="如 申报单位 / 节目名称 / 作品名称 / 论文标题" /></el-form-item
          ></el-col>
          <el-col :span="12">
            <el-form-item label="输入框宽度">
              <el-select v-model="categoryRuleFormDraft.projectNameWidthMode" class="w-full">
                <el-option label="默认" value="default" />
                <el-option label="短" value="short" />
                <el-option label="中" value="medium" />
                <el-option label="长" value="long" />
                <el-option label="整行" value="full" />
                <el-option label="自定义" value="custom" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="categoryRuleFormDraft.projectNameWidthMode === 'custom'" :span="12"
            ><el-form-item label="自定义宽度"
              ><el-input v-model="categoryRuleFormDraft.projectNameWidthCustom" placeholder="如：360px 或 50%" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="名称必填"><el-switch v-model="categoryRuleFormDraft.projectNameRequired" /></el-form-item
          ></el-col>
          <el-col :span="12">
            <el-form-item label="默认技术校验">
              <el-select v-model="categoryRuleFormDraft.mediaTechnicalCheckMode" clearable class="w-full" placeholder="默认按规则硬校验">
                <el-option label="按规则硬校验" value="" />
                <el-option label="仅人工复核提示" value="manual" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"
            ><el-form-item label="扩展模块"
              ><el-switch v-model="categoryRuleFormDraft.extensionModule" active-text="扩展" inactive-text="正式" /></el-form-item
          ></el-col>
          <el-col :span="12">
            <el-form-item label="报送提示标题">
              <el-input v-model="categoryRuleFormDraft.reportingTipTitle" placeholder="默认：报送提示" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="报送提示内容">
              <el-input
                v-model="categoryRuleFormDraft.reportingTipText"
                type="textarea"
                :rows="3"
                placeholder="默认：当前小类别暂无单独提示，请按活动通知和上传要求完成上报。"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <template v-if="categoryRuleFormDraft.validatorType === 'workshop'">
          <el-divider content-position="left">工作坊校验设置</el-divider>
          <el-row :gutter="12">
            <el-col :span="8"
              ><el-form-item label="学生人数"><el-switch v-model="workshopRuleSettingsDraft.validateStudentCount" /></el-form-item
            ></el-col>
            <el-col :span="16">
              <el-form-item label="学生字段">
                <el-select
                  v-model="workshopRuleSettingsDraft.studentCountField"
                  clearable
                  filterable
                  allow-create
                  class="w-full"
                  placeholder="选择或输入学生字段 field_key"
                >
                  <el-option v-for="item in peopleFieldOptions" :key="item.fieldKey" :label="fieldOptionLabel(item)" :value="item.fieldKey!" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12"
              ><el-form-item label="学生最少"
                ><el-input-number v-model="workshopRuleSettingsDraft.minStudentCount" class="w-full" :min="0" :precision="0" /></el-form-item
            ></el-col>
            <el-col :span="12"
              ><el-form-item label="学生最多"
                ><el-input-number v-model="workshopRuleSettingsDraft.maxStudentCount" class="w-full" :min="0" :precision="0" /></el-form-item
            ></el-col>

            <el-col :span="8"
              ><el-form-item label="教师人数"><el-switch v-model="workshopRuleSettingsDraft.validateTeacherCount" /></el-form-item
            ></el-col>
            <el-col :span="16">
              <el-form-item label="教师字段">
                <el-select
                  v-model="workshopRuleSettingsDraft.teacherCountField"
                  clearable
                  filterable
                  allow-create
                  class="w-full"
                  placeholder="选择或输入指导教师字段 field_key"
                >
                  <el-option v-for="item in peopleFieldOptions" :key="item.fieldKey" :label="fieldOptionLabel(item)" :value="item.fieldKey!" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12"
              ><el-form-item label="教师最少"
                ><el-input-number v-model="workshopRuleSettingsDraft.minTeacherCount" class="w-full" :min="0" :precision="0" /></el-form-item
            ></el-col>
            <el-col :span="12"
              ><el-form-item label="教师最多"
                ><el-input-number v-model="workshopRuleSettingsDraft.maxTeacherCount" class="w-full" :min="0" :precision="0" /></el-form-item
            ></el-col>

            <el-col :span="12"
              ><el-form-item label="校验总人数"><el-switch v-model="workshopRuleSettingsDraft.validateTotalMemberCount" /></el-form-item
            ></el-col>
            <el-col :span="12"
              ><el-form-item label="总人数上限"
                ><el-input-number v-model="workshopRuleSettingsDraft.maxTotalMemberCount" class="w-full" :min="0" :precision="0" /></el-form-item
            ></el-col>
            <el-col :span="12"
              ><el-form-item label="单校单项"><el-switch v-model="workshopRuleSettingsDraft.requireSingleProjectPerSchool" /></el-form-item
            ></el-col>
            <el-col :span="12"
              ><el-form-item label="未获奖承诺"><el-switch v-model="workshopRuleSettingsDraft.requireNoPreviousAwardCommitment" /></el-form-item
            ></el-col>
            <el-col :span="12"
              ><el-form-item label="校验视频格式"><el-switch v-model="workshopRuleSettingsDraft.validateVideoExt" /></el-form-item
            ></el-col>
            <el-col :span="12"
              ><el-form-item label="允许格式"
                ><el-input v-model="workshopRuleSettingsDraft.videoAllowedExt" placeholder="mp4,mpg,mpeg" /></el-form-item
            ></el-col>
            <el-col :span="12"
              ><el-form-item label="校验视频时长"><el-switch v-model="workshopRuleSettingsDraft.validateVideoDuration" /></el-form-item
            ></el-col>
            <el-col :span="12"
              ><el-form-item label="最长秒数"
                ><el-input-number v-model="workshopRuleSettingsDraft.maxVideoDurationSeconds" class="w-full" :min="0" :precision="0" /></el-form-item
            ></el-col>
          </el-row>
        </template>
        <el-form-item label="高级规则JSON">
          <el-input
            v-model="categoryFormDraft.ruleJson"
            type="textarea"
            :rows="6"
            placeholder='可配置专项校验开关；例如 {"requireNoIdentityInVideo":false,"validateVideoBitrate":false}'
          />
        </el-form-item>
        <el-form-item label="填报提示">
          <el-input v-model="categoryFormDraft.tipText" type="textarea" :rows="3" placeholder="显示在学校端报送类别说明区域；不填则使用默认提示" />
        </el-form-item>
        <el-form-item label="名额限制"><el-input-number v-model="categoryFormDraft.quotaLimit" class="w-full" /></el-form-item>
      </template>
      <el-form-item label="排序"><el-input-number v-model="categoryFormDraft.sortOrder" class="w-full" /></el-form-item>
      <el-form-item label="菜单显示"><el-switch v-model="menuVisibleModel" active-text="显示" inactive-text="隐藏" /></el-form-item>
      <el-form-item label="业务启用"><el-switch v-model="categoryFormDraft.enabled" active-text="启用" inactive-text="停用" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button v-if="restoreEnabled" type="success" @click="emit('restore', createDraft())">恢复</el-button>
      <el-button v-if="purgeEnabled" type="danger" @click="emit('purge', createDraft())">永久删除</el-button>
      <el-button v-if="deleteEnabled" type="danger" @click="emit('delete', createDraft())">删除</el-button>
      <el-button type="primary" @click="emit('confirm', createDraft())">确定</el-button>
      <el-button @click="visibleModel = false">取消</el-button>
    </template>
  </ArtPopupDialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import type { ActivityCategoryVO, CategoryFieldSchemaVO } from '@/api/crehn/types';
import type { ActivityCategoryTreeNode } from '@/utils/artCategory';

type CategoryNodeType = 'group' | 'category';

type CategoryRuleForm = {
  templateCode?: string;
  validatorType?: string;
  validatorLabel?: string;
  projectNameLabel?: string;
  projectNameRequired?: boolean;
  projectNameWidthMode?: string;
  projectNameWidthCustom?: string;
  mediaTechnicalCheckMode?: string;
  extensionModule?: boolean;
  reportingTipTitle?: string;
  reportingTipText?: string;
};

type WorkshopRuleSettings = {
  validateStudentCount: boolean;
  studentCountField: string;
  minStudentCount: number;
  maxStudentCount: number;
  validateTeacherCount: boolean;
  teacherCountField: string;
  minTeacherCount: number;
  maxTeacherCount: number;
  validateTotalMemberCount: boolean;
  maxTotalMemberCount: number;
  requireSingleProjectPerSchool: boolean;
  requireNoPreviousAwardCommitment: boolean;
  validateVideoExt: boolean;
  videoAllowedExt: string;
  validateVideoDuration: boolean;
  maxVideoDurationSeconds: number;
};

type CategoryRuleDialogDraft = {
  categoryForm: ActivityCategoryVO;
  nodeType: CategoryNodeType;
  menuVisible: boolean;
  categoryParentKey: string;
  categoryRuleForm: CategoryRuleForm;
  workshopRuleSettings: WorkshopRuleSettings;
};

const props = defineProps<{
  visible: boolean;
  title: string;
  categoryForm: ActivityCategoryVO;
  nodeType: CategoryNodeType;
  menuVisible: boolean;
  nodeTypeLocked: boolean;
  categoryParentKey: string;
  categoryRuleForm: CategoryRuleForm;
  workshopRuleSettings: WorkshopRuleSettings;
  categoryGroupNodes: ActivityCategoryTreeNode[];
  peopleFieldOptions: CategoryFieldSchemaVO[];
  categoryRuleTemplateOptions: Array<{ label: string; templateCode: string }>;
  validatorTypeOptions: Array<{ label: string; value: string }>;
  groupOptionKey: (item: ActivityCategoryTreeNode) => string;
  groupOptionLabel: (item: ActivityCategoryTreeNode) => string;
  fieldOptionLabel: (item: CategoryFieldSchemaVO) => string;
  canDelete: boolean;
  canRestore: boolean;
  canPurge: boolean;
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  templateChange: [draft: CategoryRuleDialogDraft];
  validatorChange: [draft: CategoryRuleDialogDraft];
  confirm: [draft: CategoryRuleDialogDraft];
  delete: [draft: CategoryRuleDialogDraft];
  restore: [draft: CategoryRuleDialogDraft];
  purge: [draft: CategoryRuleDialogDraft];
}>();

const categoryFormDraft = ref<ActivityCategoryVO>({});
const categoryRuleFormDraft = ref<CategoryRuleForm>({});
const workshopRuleSettingsDraft = ref<WorkshopRuleSettings>({} as WorkshopRuleSettings);
const categoryParentKeyDraft = ref('');
const nodeTypeDraft = ref<CategoryNodeType>('category');
const menuVisibleDraft = ref(true);

const syncDrafts = () => {
  categoryFormDraft.value = { ...props.categoryForm };
  categoryRuleFormDraft.value = { ...props.categoryRuleForm };
  workshopRuleSettingsDraft.value = { ...props.workshopRuleSettings };
  categoryParentKeyDraft.value = props.categoryParentKey;
  nodeTypeDraft.value = props.nodeType;
  menuVisibleDraft.value = props.menuVisible;
};

watch(
  () => props.visible,
  (visible) => {
    if (visible) syncDrafts();
  }
);
watch(
  () => props.categoryForm,
  () => {
    if (props.visible) syncDrafts();
  },
  { deep: true }
);
watch(
  () => props.categoryRuleForm,
  () => {
    if (props.visible) syncDrafts();
  },
  { deep: true }
);
watch(
  () => props.workshopRuleSettings,
  () => {
    if (props.visible) syncDrafts();
  },
  { deep: true }
);

const visibleModel = computed({
  get: () => props.visible,
  set: (value: boolean) => emit('update:visible', value)
});
const nodeTypeModel = computed({
  get: () => nodeTypeDraft.value,
  set: (value: CategoryNodeType) => (nodeTypeDraft.value = value)
});
const menuVisibleModel = computed({
  get: () => menuVisibleDraft.value,
  set: (value: boolean) => (menuVisibleDraft.value = value)
});

const canDeleteCurrent = computed(() => props.canDelete && !!categoryFormDraft.value.id && categoryFormDraft.value.delFlag !== '1');
const canRestoreCurrent = computed(() => props.canRestore && !!categoryFormDraft.value.id && categoryFormDraft.value.delFlag === '1');
const canPurgeCurrent = computed(() => props.canPurge && !!categoryFormDraft.value.id && categoryFormDraft.value.delFlag === '1');

const deleteEnabled = canDeleteCurrent;
const restoreEnabled = canRestoreCurrent;
const purgeEnabled = canPurgeCurrent;

const createDraft = (): CategoryRuleDialogDraft => ({
  categoryForm: { ...categoryFormDraft.value },
  nodeType: nodeTypeDraft.value,
  menuVisible: menuVisibleDraft.value,
  categoryParentKey: categoryParentKeyDraft.value,
  categoryRuleForm: { ...categoryRuleFormDraft.value },
  workshopRuleSettings: { ...workshopRuleSettingsDraft.value }
});

const handleTemplateChange = () => emit('templateChange', createDraft());
const handleValidatorChange = () => emit('validatorChange', createDraft());
</script>
