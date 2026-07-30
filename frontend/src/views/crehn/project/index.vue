<template>
  <div class="p-2">
    <el-card shadow="never" class="mb-[10px] reporting-entry-card">
      <template #header>
        <div class="section-toolbar">
          <span>{{ reportingEntryTitle }}</span>
        </div>
      </template>
      <div v-if="visibleReportingCategories.length" class="reporting-category-shell">
        <div v-if="visibleReportingCategories.length > 1" class="reporting-category-switcher">
          <button
            v-for="category in visibleReportingCategories"
            :key="category.id"
            type="button"
            class="reporting-category-switch"
            :class="{ active: String(selectedReportingCategory?.id || '') === String(category.id) }"
            @click="selectReportingCategory(category)"
          >
            {{ category.categoryName }}
          </button>
        </div>
        <div v-if="selectedReportingCategory" class="reporting-overview">
          <div class="reporting-overview-main">
            <div class="reporting-overview-title">{{ selectedReportingCategory.categoryName }}</div>
            <div class="reporting-overview-stats">{{ categoryStatsText(selectedReportingCategory) }}</div>
            <el-button
              v-hasPermi="['crehn:project:add']"
              class="reporting-overview-button"
              type="primary"
              icon="Plus"
              @click="openProjectByCategory(selectedReportingCategory)"
            >
              添加报送
            </el-button>
          </div>
          <div class="reporting-overview-tip">
            <div class="reporting-tip-title">{{ reportingTipTitle(selectedReportingCategory) }}</div>
            <div class="reporting-tip-content">{{ reportingTipText(selectedReportingCategory) }}</div>
          </div>
        </div>
        <el-empty v-else description="请选择小类别" />
      </div>
      <el-alert v-if="managedActivityError" type="warning" :closable="false" :title="managedActivityError" />
      <el-empty v-else-if="!visibleReportingCategories.length" description="当前大类暂无可报送类别" />
    </el-card>

    <el-card shadow="hover">
      <ArtProjectWorkspaceHeader
        page-key="project"
        :title-variables="{ activityName: selectedActivityName, categoryName: selectedReportingCategory?.categoryName || '当前类别' }"
      >
        <template #actions>
          <el-button
            v-if="canAddProject"
            class="table-add-button"
            type="primary"
            icon="Plus"
            :disabled="!selectedReportingCategory"
            @click="openProject()"
          >
            {{ projectHeaderText('actions', 'add', '添加') }}
          </el-button>
        </template>
      </ArtProjectWorkspaceHeader>
      <div ref="tableShellRef" class="art-resizable-table-shell art-resizable-table-shell--fit">
        <el-table
          v-loading="loading"
          border
          :data="projectList"
          :empty-text="projectTablePage.emptyText"
          class="project-list-table art-list-table art-resizable-table art-resizable-table--borderless art-viewable-table"
          :class="tableAppearanceClass"
          :style="tableAppearanceStyle"
          @header-dragend="handleColumnResize"
          @row-click="openProjectView"
        >
          <el-table-column
            v-for="column in projectWidthColumns"
            :key="column.key"
            :column-key="column.key"
            :label="column.label"
            :prop="column.source === 'builtin' && !['actions', 'serial'].includes(column.key) ? column.key : undefined"
            :width="columnWidth(column.key)"
            :min-width="column.minWidth"
            :fixed="column.fixed"
            :resizable="column.resizable"
            :align="['serial', 'actions'].includes(column.key) ? 'center' : undefined"
            :class-name="
              column.key === 'serial'
                ? 'art-table-nowrap-cell'
                : column.key === 'projectName' || column.key === 'currentAuditOpinion' || column.source === 'form'
                  ? 'art-table-configurable-wrap-cell'
                  : 'art-table-nowrap-cell'
            "
            :label-class-name="column.key === 'actions' ? 'art-fixed-action-header' : 'art-resizable-header'"
          >
            <template #default="scope">
              <span v-if="column.key === 'serial'">{{ projectSerialNumber(scope.$index) }}</span>
              <ArtListStatusTag
                v-else-if="column.key === 'status'"
                :semantic="statusSemantic(scope.row.status)"
                :label="statusText(statusSemantic(scope.row.status), statusLabel(scope.row.status))"
                :tag-type="statusType(scope.row.status)"
              />
              <div v-else-if="column.key === 'actions'" class="project-row-actions art-list-row-actions">
                <el-button
                  class="project-row-action art-list-row-action"
                  :class="canEdit(scope.row.status) ? 'project-row-action--edit' : 'project-row-action--view'"
                  link
                  type="primary"
                  :icon="actionIcon(canEdit(scope.row.status) ? 'Edit' : 'View')"
                  @click.stop="canEdit(scope.row.status) ? openProject(scope.row) : openProjectView(scope.row)"
                  >{{ canEdit(scope.row.status) ? actionText('edit', '编辑') : actionText('view', '查看') }}</el-button
                >
                <el-button
                  class="project-row-action art-list-row-action"
                  :class="
                    canWithdrawSubmit(scope.row) || scope.row.status === 'audit_passed'
                      ? 'project-row-action--withdraw'
                      : 'project-row-action--submit'
                  "
                  link
                  :type="canWithdrawSubmit(scope.row) || scope.row.status === 'audit_passed' ? 'warning' : 'success'"
                  :icon="actionIcon(canWithdrawSubmit(scope.row) || scope.row.status === 'audit_passed' ? 'RefreshLeft' : 'Upload')"
                  :disabled="projectSubmitActionDisabled(scope.row)"
                  :title="projectSubmitActionTitle(scope.row)"
                  @click.stop="handleProjectSubmitAction(scope.row)"
                  >{{
                    canWithdrawSubmit(scope.row) || scope.row.status === 'audit_passed'
                      ? actionText('withdraw', '撤回')
                      : actionText('submit', '提交')
                  }}</el-button
                >
                <el-button
                  class="project-row-action art-list-row-action project-row-action--delete"
                  link
                  type="danger"
                  :icon="actionIcon('Delete')"
                  :disabled="projectDeleteActionDisabled(scope.row)"
                  :title="projectDeleteActionTitle(scope.row)"
                  @click.stop="handleProjectDeleteAction(scope.row)"
                  >{{ actionText('delete', '删除') }}</el-button
                >
              </div>
              <span v-else>{{ projectColumnText(scope.row, column) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <ArtPopupDialog v-model="dialog.visible" :title="dialog.title" width="920px" append-to-body>
      <el-alert
        v-if="form.status === 'returned' && form.currentAuditOpinion"
        class="mb-3"
        type="warning"
        :closable="false"
        :title="form.currentAuditOpinion"
      />
      <el-form :model="form" label-width="110px" :disabled="!canEdit(form.status)">
        <el-row :gutter="10">
          <el-col :span="12">
            <el-form-item label="活动">
              <el-input :model-value="selectedActivityName || '管理员尚未配置当前活动'" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类别">
              <el-select v-model="form.categoryId" class="w-full" placeholder="请选择类别" @change="handleCategoryChange">
                <el-option v-for="item in categoryOptions" :key="item.id" :label="item.categoryName" :value="item.id!" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="项目名称">
              <el-input v-model="form.projectName" />
            </el-form-item>
          </el-col>
          <el-col v-if="groupOptions.length" :span="12">
            <el-form-item label="组别" required>
              <el-select v-model="form.groupCode" class="w-full" placeholder="请选择组别" @change="handleGroupChange">
                <el-option v-for="item in groupOptions" :key="item.groupCode" :label="item.groupName || item.groupCode" :value="item.groupCode!" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="categoryTip" :span="24">
            <el-alert class="mb-2" type="info" :closable="false" :title="categoryTip" />
          </el-col>
          <el-col v-for="field in fieldList" :key="field.id" :span="wideField(field) ? 24 : 12">
            <el-form-item :label="field.fieldLabel" :required="field.required">
              <div v-if="field.fieldType === 'textarea'" class="field-control-limiter" :style="fieldControlStyle(field)">
                <el-input
                  v-model="formData[field.fieldKey!]"
                  type="textarea"
                  :rows="3"
                  :maxlength="fieldTextMaxLength(field)"
                  :minlength="fieldTextMinLength(field)"
                  :show-word-limit="fieldShowWordLimit(field)"
                />
              </div>
              <div v-else-if="field.fieldType === 'select'" class="field-control-limiter" :style="fieldControlStyle(field)">
                <el-select v-model="formData[field.fieldKey!]" class="w-full" popper-class="art-dynamic-select-dropdown">
                  <el-option v-for="option in parseOptions(field.optionsJson)" :key="option" :label="option" :value="option" />
                </el-select>
              </div>
              <div v-else-if="field.fieldType === 'radio'" class="dynamic-radio-field">
                <el-radio-group v-model="formData[field.fieldKey!]" :class="{ 'topic-radio-grid': topicCategoryField(field) }">
                  <el-radio v-for="option in parseOptions(field.optionsJson)" :key="option" :label="option">{{ option }}</el-radio>
                </el-radio-group>
                <span v-if="originalityHintVisible(field)" class="originality-hint">（改编、创作作品不属于原创）</span>
              </div>
              <el-checkbox-group v-else-if="field.fieldType === 'checkbox'" v-model="formData[field.fieldKey!]">
                <el-checkbox v-for="option in parseOptions(field.optionsJson)" :key="option" :label="option">{{ option }}</el-checkbox>
              </el-checkbox-group>
              <div v-else-if="field.fieldType === 'date'" class="field-control-limiter" :style="fieldControlStyle(field)">
                <el-date-picker v-model="formData[field.fieldKey!]" value-format="YYYY-MM-DD" class="w-full" />
              </div>
              <div v-else-if="field.fieldType === 'number'" class="field-control-limiter" :style="fieldControlStyle(field)">
                <el-input-number
                  v-model="formData[field.fieldKey!]"
                  class="w-full"
                  :min="fieldNumberMin(field)"
                  :max="fieldNumberMax(field)"
                  :precision="fieldNumberPrecision(field)"
                  :step="fieldNumberStep(field)"
                />
              </div>
              <div v-else class="field-control-limiter" :style="fieldControlStyle(field)">
                <el-input
                  v-model="formData[field.fieldKey!]"
                  :maxlength="fieldTextMaxLength(field)"
                  :minlength="fieldTextMinLength(field)"
                  :show-word-limit="fieldShowWordLimit(field)"
                />
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-divider content-position="left">成员信息</el-divider>
      <div class="mb-2 flex flex-wrap gap-2">
        <el-button v-if="canEdit(form.status)" size="small" type="primary" plain icon="Plus" @click="addMember('student')">学生</el-button>
        <el-button v-if="canEdit(form.status)" size="small" type="primary" plain icon="Plus" @click="addMember('author')">作者</el-button>
        <el-button v-if="canEdit(form.status)" size="small" type="primary" plain icon="Plus" @click="addMember('teacher')">指导教师</el-button>
        <el-button v-if="canEdit(form.status)" size="small" type="primary" plain icon="Plus" @click="addMember('completer')">完成人</el-button>
      </div>
      <el-table border :data="memberList">
        <el-table-column label="类型" width="130">
          <template #default="scope">
            <el-select v-model="scope.row.memberType" :disabled="!canEdit(form.status)">
              <el-option label="学生" value="student" />
              <el-option label="作者" value="author" />
              <el-option label="指导教师" value="teacher" />
              <el-option label="完成人" value="completer" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="姓名" min-width="130"
          ><template #default="scope"><el-input v-model="scope.row.name" :disabled="!canEdit(form.status)" /></template
        ></el-table-column>
        <el-table-column label="学号" min-width="130"
          ><template #default="scope"><el-input v-model="scope.row.studentNo" :disabled="!canEdit(form.status)" /></template
        ></el-table-column>
        <el-table-column label="院系" min-width="140"
          ><template #default="scope"><el-input v-model="scope.row.department" :disabled="!canEdit(form.status)" /></template
        ></el-table-column>
        <el-table-column label="专业" min-width="120"
          ><template #default="scope"><el-input v-model="scope.row.major" :disabled="!canEdit(form.status)" /></template
        ></el-table-column>
        <el-table-column label="角色" min-width="120"
          ><template #default="scope"><el-input v-model="scope.row.roleName" :disabled="!canEdit(form.status)" /></template
        ></el-table-column>
        <el-table-column v-if="canEdit(form.status)" label="操作" width="90" align="center">
          <template #default="scope"
            ><el-button link type="danger" icon="Delete" @click="memberList.splice(scope.$index, 1)">删除</el-button></template
          >
        </el-table-column>
      </el-table>

      <el-divider content-position="left">材料上传</el-divider>
      <el-table border :data="requirementList">
        <el-table-column label="材料" prop="fileTypeName" min-width="140" />
        <el-table-column label="要求" min-width="220">
          <template #default="scope">
            {{ scope.row.required ? '必传' : '选传' }}，{{ formatAllowedExt(scope.row.allowedExt) }}，{{ scope.row.minCount || 0 }}-{{
              scope.row.maxCount || 1
            }}
            个
          </template>
        </el-table-column>
        <el-table-column label="已上传" min-width="220">
          <template #default="scope">
            <div class="flex flex-wrap gap-1">
              <el-tag
                v-for="file in filesByRequirement(scope.row.id)"
                :key="file.id"
                class="mb-1"
                :closable="canEdit(form.status)"
                :disable-transitions="true"
                @close="removeFile(file)"
              >
                <el-link v-if="file.storagePath" type="primary" :href="file.storagePath" target="_blank">{{ file.originalName }}</el-link>
                <span v-else>{{ file.originalName }}</span>
                <el-tag
                  v-if="file.checkStatus && file.checkStatus !== 'passed'"
                  class="ml-1"
                  size="small"
                  :type="file.checkStatus === 'warning' ? 'warning' : 'danger'"
                >
                  {{ normalizeReviewMessage(file.checkMessage) || file.checkStatus }}
                </el-tag>
                <el-tag v-if="file.previewStatus" class="ml-1" size="small" :type="previewTagType(file.previewStatus)">
                  {{ normalizePreviewMessage(file.previewMessage, file.previewStatus) }}
                </el-tag>
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="260" align="center">
          <template #default="scope">
            <div class="material-upload-cell">
              <el-upload
                :show-file-list="false"
                :disabled="!canEdit(form.status)"
                :accept="acceptExt(scope.row.allowedExt)"
                :before-upload="(file: any) => beforeUploadMaterial(scope.row, file)"
                :http-request="(option: any) => uploadMaterial(scope.row.id, option)"
              >
                <el-button type="primary" link icon="Upload" :disabled="!canEdit(form.status)">上传</el-button>
              </el-upload>
              <span v-if="uploadErrorFor(scope.row.id)" class="material-upload-error" :title="uploadErrorFor(scope.row.id)?.message">
                {{ uploadErrorFor(scope.row.id)?.message }}
              </span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button v-if="canEdit(form.status)" type="primary" @click="saveDraft">保存草稿</el-button>
        <el-button v-if="canEdit(form.status) && form.id" type="success" @click="submitCurrent">提交</el-button>
        <el-button @click="dialog.visible = false">关闭</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ArtProject" lang="ts">
import { listAvailableActivity, listAvailableCategory, listSchoolField, listSchoolFileRequirement } from '@/api/crehn/activity';
import { listActivityReportRuleSchoolOptions } from '@/api/crehn/config';
import type { ArtListStatusSemantic, ArtReviewListColumnConfig } from '@/api/crehn/detailDisplay';
import {
  completeProjectFileDirectUpload,
  deleteProjectFile,
  getProject,
  initProjectFileDirectUpload,
  listMyProject,
  listMyProjectCategoryStats,
  recycleProject,
  saveProjectDraft,
  submitProject,
  uploadProjectFileToOss,
  withdrawSubmitProject
} from '@/api/crehn/project';
import { useRoute, useRouter } from 'vue-router';
import {
  ActivityCategoryVO,
  ActivityRuleGroupOptionVO,
  ActivityVO,
  CategoryFieldSchemaVO,
  CategoryFileRequirementVO,
  ProjectCategoryStatsVO,
  ProjectFileVO,
  ProjectMemberVO,
  ProjectSaveForm,
  ProjectVO
} from '@/api/crehn/types';
import { checkPermi } from '@/utils/permission';
import { normalizePreviewMessage, normalizeReviewMessage } from '@/utils/artReviewMessage';
import { useArtListTableAppearance } from '@/composables/useArtListTableAppearance';
import { artTableColumnValue, artTableDisplayText, useArtListTablePage } from '@/composables/useArtListTableConfig';
import { useArtTableColumnWidths } from '@/composables/useArtTableColumnWidths';
import { useManagedArtActivity } from '@/composables/useManagedArtActivity';
import { loadArtDetailDisplayConfig, useArtDetailDisplayConfig, workspaceHeaderItemText } from '@/views/crehn/components/artDetailDisplayConfig';
import ArtListStatusTag from '@/views/crehn/components/ArtListStatusTag.vue';
import ArtProjectWorkspaceHeader from '@/views/crehn/components/ArtProjectWorkspaceHeader.vue';

type UploadErrorState = {
  message: string;
  fileName?: string;
};

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const route = useRoute();
const router = useRouter();

const loading = ref(false);
const total = ref(0);
const projectList = ref<ProjectVO[]>([]);
const activityOptions = ref<ActivityVO[]>([]);
const { managedActivityError, resolveManagedActivityId } = useManagedArtActivity(activityOptions);
const { tableAppearanceClass, tableAppearanceStyle, actionIcon } = useArtListTableAppearance({
  statusLabels: () => ['draft', 'pending', 'approved', 'returned'].map((key) => projectTablePage.value.statusLabels[key as ArtListStatusSemantic]),
  actionLabels: () => ['view', 'edit', 'submit', 'withdraw', 'delete'].map((key) => projectTablePage.value.actionLabels[key])
});
const categoryOptions = ref<ActivityCategoryVO[]>([]);
const fieldList = ref<CategoryFieldSchemaVO[]>([]);
const requirementList = ref<CategoryFileRequirementVO[]>([]);
const groupOptions = ref<ActivityRuleGroupOptionVO[]>([]);
const categoryTip = ref('');
const fileList = ref<ProjectFileVO[]>([]);
const memberList = ref<ProjectMemberVO[]>([]);
const formData = ref<Record<string, any>>({});
const form = ref<ProjectVO>({ status: 'draft' });
const uploadErrorMap = ref<Record<string, UploadErrorState>>({});
const dialog = reactive({ visible: false, title: '' });
const queryParams = reactive({ pageNum: 1, pageSize: 10, projectName: '', status: '' });
const activeActivityId = ref<string | number>();
const selectedActivityName = computed(
  () => activityOptions.value.find((item) => String(item.id) === String(activeActivityId.value || ''))?.activityName || ''
);
const { detailDisplayConfig } = useArtDetailDisplayConfig();
const projectHeaderConfig = computed(() => detailDisplayConfig.value.workspaceHeader.pages.project);
const projectHeaderText = (itemKey: 'actions', textKey: string, fallback: string) =>
  workspaceHeaderItemText(projectHeaderConfig.value, itemKey, textKey, fallback);
const reportingCategoryList = ref<ActivityCategoryVO[]>([]);
const selectedReportingCategoryId = ref<string | number>();
const categoryStatsMap = ref<Record<string, ProjectCategoryStatsVO>>({});
const canAddProject = computed(() => checkPermi(['crehn:project:add']));
const canSubmitProject = computed(() => checkPermi(['crehn:project:submit']));
const canRemoveProject = computed(() => checkPermi(['crehn:project:remove']));
const reportGroupDefs = [
  { key: 'all', label: '全部项目', aliases: [] },
  { key: 'performance', label: '艺术表演类', aliases: ['艺术表演类', '表演类节目', '艺术表演类节目', '表演'] },
  { key: 'artwork', label: '艺术作品类', aliases: ['艺术作品类', '艺术作品', '作品'] },
  { key: 'workshop', label: '艺术实践工作坊', aliases: ['艺术实践工作坊', '工作坊'] },
  {
    key: 'achievement',
    label: '高校美育改革创新优秀成果',
    aliases: ['高校美育改革创新优秀成果', '美育改革创新优秀成果', '高校美育改革创新成果', '优秀成果', '美育']
  },
  { key: 'principal', label: '高校校长书画作品', aliases: ['高校校长书画作品', '校长书画'] }
];
const defaultReportingTipTitle = '报送提示';
const defaultReportingTipText = '当前小类别暂无单独提示，请按活动通知和上传要求完成上报。';
const routeQueryText = (value: unknown) => (Array.isArray(value) ? String(value[0] || '') : String(value || ''));
const currentMenuCategoryGroup = computed(() => routeQueryText(route.query.categoryGroup));
const currentReportGroupKey = computed(() => routeQueryText(route.query.group) || 'all');
const currentMenuCategoryId = computed(() => routeQueryText(route.query.categoryId));
const currentMenuCategoryCode = computed(() => routeQueryText(route.query.categoryCode));
const currentMenuCategoryName = computed(() => routeQueryText(route.query.categoryName));
const currentMenuCategoryKeyword = computed(() => routeQueryText(route.query.categoryKeyword || route.query.keyword));
const isSpecificCategoryMenu = computed(() =>
  Boolean(currentMenuCategoryId.value || currentMenuCategoryCode.value || currentMenuCategoryName.value || currentMenuCategoryKeyword.value)
);
const visibleReportingCategories = computed(() => reportingCategoryList.value.filter(categoryMatchesCurrentGroup).filter(categoryMatchesCurrentMenu));
const selectedReportingCategory = computed(() => {
  const selected = visibleReportingCategories.value.find((item) => String(item.id || '') === String(selectedReportingCategoryId.value || ''));
  return selected || visibleReportingCategories.value[0];
});
const {
  pageConfig: projectTablePage,
  columns: projectTableColumns,
  serialColumn: projectSerialColumn,
  statusText,
  actionText
} = useArtListTablePage('project', () => selectedReportingCategory.value?.id);
const projectColumnText = (row: ProjectVO, column: ArtReviewListColumnConfig) =>
  artTableDisplayText(artTableColumnValue(row as Record<string, unknown>, column));
const selectedReportingCategoryIds = computed(() => {
  const id = selectedReportingCategory.value?.id;
  return id === undefined || id === null ? [] : [id];
});
const primaryReportingCategory = computed(() => (visibleReportingCategories.value.length === 1 ? visibleReportingCategories.value[0] : undefined));
const reportingEntryTitle = computed(() => {
  if (primaryReportingCategory.value) return `${primaryReportingCategory.value.categoryName}报送`;
  if (currentMenuCategoryGroup.value) return `${currentMenuCategoryGroup.value}报送`;
  return currentReportGroupKey.value === 'all'
    ? '节目和作品报送'
    : `${reportGroupDefs.find((item) => item.key === currentReportGroupKey.value)?.label || '项目'}报送`;
});

const categoryStatsText = (category?: ActivityCategoryVO) => {
  const stats = category?.id ? categoryStatsMap.value[String(category.id)] : undefined;
  return `报送${stats?.totalCount || 0}项 / 提交${stats?.submittedCount || 0}项 / 完成${stats?.completedCount || 0}项`;
};

const parseRuleObject = (json?: string): Record<string, any> => {
  if (!json) return {};
  try {
    const parsed = JSON.parse(json);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? (parsed as Record<string, any>) : {};
  } catch {
    return {};
  }
};

const normalizeRuleText = (value: unknown, fallback: string) => {
  const text = typeof value === 'string' ? value.trim() : '';
  return text || fallback;
};

const categoryRuleOf = (category?: ActivityCategoryVO) => parseRuleObject(category?.ruleJson);

const reportingTipTitle = (category?: ActivityCategoryVO) => {
  const rule = categoryRuleOf(category);
  return normalizeRuleText(rule.reportingTipTitle, defaultReportingTipTitle);
};

const reportingTipText = (category?: ActivityCategoryVO) => {
  const rule = categoryRuleOf(category);
  return normalizeRuleText(rule.reportingTipText, normalizeRuleText(category?.tipText, defaultReportingTipText));
};

const resolveReportGroupKey = (groupName?: string) => {
  const text = groupName || '';
  return (
    reportGroupDefs.find((group) => group.key !== 'all' && [group.label, ...group.aliases].some((alias) => text.includes(alias)))?.key || 'other'
  );
};

const categoryGroupName = (category: ActivityCategoryVO) => category.categoryGroup || inferCategoryGroup(category.categoryName);

const categoryMatchesCurrentGroup = (category: ActivityCategoryVO) => {
  if (currentMenuCategoryGroup.value) {
    return categoryGroupName(category) === currentMenuCategoryGroup.value;
  }
  if (currentReportGroupKey.value === 'all') {
    return true;
  }
  return resolveReportGroupKey(categoryGroupName(category)) === currentReportGroupKey.value;
};

const categoryMatchesCurrentMenu = (category: ActivityCategoryVO) => {
  if (currentMenuCategoryId.value && String(category.id) !== currentMenuCategoryId.value) return false;
  if (currentMenuCategoryCode.value && String(category.categoryCode || '') !== currentMenuCategoryCode.value) return false;
  if (currentMenuCategoryName.value) {
    const categoryName = category.categoryName || '';
    if (!categoryName.includes(currentMenuCategoryName.value) && !currentMenuCategoryName.value.includes(categoryName)) return false;
  }
  if (currentMenuCategoryKeyword.value) {
    const categoryName = category.categoryName || '';
    const keywords = currentMenuCategoryKeyword.value.split(/[,，、\s]+/).filter(Boolean);
    if (keywords.length && !keywords.some((keyword) => categoryName.includes(keyword))) return false;
  }
  return true;
};

const getList = async () => {
  loading.value = true;
  try {
    const params: Record<string, any> = { ...queryParams };
    if (
      visibleReportingCategories.value.length ||
      currentMenuCategoryGroup.value ||
      currentReportGroupKey.value !== 'all' ||
      isSpecificCategoryMenu.value
    ) {
      const categoryIds = selectedReportingCategoryIds.value;
      if (!categoryIds.length) {
        projectList.value = [];
        total.value = 0;
        return;
      }
      params.categoryIds = categoryIds.join(',');
    }
    const res = await listMyProject(params);
    projectList.value = res.rows;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
};

const openProject = async (row?: ProjectVO) => {
  if (row?.id) {
    await router.push({ path: `/crehn/project/edit/${row.id}`, query: buildEditQuery(row) });
    return;
  }
  const category = selectedReportingCategory.value || primaryReportingCategory.value;
  if (await openEditableProjectIfExists(category)) {
    return;
  }
  if (category?.id) {
    await router.push({ path: '/crehn/project/edit', query: buildCreateQuery(category) });
    return;
  }
  if (currentMenuCategoryGroup.value || isSpecificCategoryMenu.value) {
    await router.push({ path: '/crehn/project/edit', query: buildCreateQuery() });
    return;
  }
  await router.push('/crehn/project/edit');
};

const openProjectView = async (row: ProjectVO) => {
  if (!row.id) return;
  await router.push({ path: `/crehn/project/edit/${row.id}`, query: { ...buildEditQuery(row), mode: 'view' } });
};

const openProjectByCategory = async (category: ActivityCategoryVO) => {
  if (await openEditableProjectIfExists(category)) {
    return;
  }
  await router.push({ path: '/crehn/project/edit', query: buildCreateQuery(category) });
};

const selectReportingCategory = async (category: ActivityCategoryVO) => {
  if (category.id === undefined || category.id === null || String(selectedReportingCategoryId.value || '') === String(category.id)) return;
  selectedReportingCategoryId.value = category.id;
  queryParams.pageNum = 1;
  await getList();
};

const buildCreateQuery = (category?: ActivityCategoryVO) => {
  const query: Record<string, any> = {
    returnPath: route.fullPath
  };
  const title = categoryFillTitle(category?.categoryName || currentMenuCategoryName.value);
  if (title) {
    query.title = title;
  }
  if (activeActivityId.value) {
    query.activityId = activeActivityId.value;
  }
  if (category?.id || currentMenuCategoryId.value) {
    query.categoryId = category?.id || currentMenuCategoryId.value;
  }
  if (category?.categoryCode || currentMenuCategoryCode.value) {
    query.categoryCode = category?.categoryCode || currentMenuCategoryCode.value;
  }
  if (currentMenuCategoryName.value) {
    query.categoryName = currentMenuCategoryName.value;
  }
  if (currentMenuCategoryKeyword.value) {
    query.categoryKeyword = currentMenuCategoryKeyword.value;
  }
  if (currentMenuCategoryGroup.value) {
    query.categoryGroup = currentMenuCategoryGroup.value;
  }
  if (category?.id || currentMenuCategoryId.value || category?.categoryCode || currentMenuCategoryCode.value) {
    query.lockCategory = '1';
  }
  return query;
};

const buildEditQuery = (row: ProjectVO) => {
  const query: Record<string, any> = { returnPath: route.fullPath };
  const title = categoryFillTitle(row.categoryName);
  if (title) {
    query.title = title;
  }
  return query;
};

const editableProjectStatusList = ['draft', 'returned'];

const findEditableProjectInRows = (rows: ProjectVO[], category?: ActivityCategoryVO) => {
  const categoryId = category?.id || currentMenuCategoryId.value;
  const activityId = activeActivityId.value;
  return rows.find((row) => {
    if (!canEdit(row.status)) return false;
    if (categoryId && String(row.categoryId || '') !== String(categoryId)) return false;
    if (activityId && String(row.activityId || '') !== String(activityId)) return false;
    return true;
  });
};

const fetchEditableProject = async (category?: ActivityCategoryVO) => {
  const localProject = findEditableProjectInRows(projectList.value, category);
  if (localProject?.id) return localProject;

  const categoryId = category?.id || currentMenuCategoryId.value;
  if (!categoryId) return undefined;

  for (const status of editableProjectStatusList) {
    const res = await listMyProject({
      pageNum: 1,
      pageSize: 1,
      status,
      activityId: activeActivityId.value || undefined,
      categoryIds: String(categoryId)
    });
    const row = findEditableProjectInRows(res.rows || [], category);
    if (row?.id) return row;
  }
  return undefined;
};

const openEditableProjectIfExists = async (category?: ActivityCategoryVO) => {
  const existingProject = await fetchEditableProject(category);
  if (!existingProject?.id) return false;
  const categoryName = category?.categoryName || existingProject.categoryName || '当前类别';
  try {
    await proxy?.$modal.confirm(
      `「${categoryName}」已有未提交项目「${projectDisplayName(existingProject)}」，请继续编辑该草稿，避免重复新增。是否继续编辑？`
    );
    await router.push({ path: `/crehn/project/edit/${existingProject.id}`, query: buildEditQuery(existingProject) });
  } catch {
    // 用户取消时停留在当前列表页，不再打开空白新增页。
  }
  return true;
};

const categoryFillTitle = (categoryName?: string) => {
  const name = String(categoryName || '').trim();
  return name ? `${name}填报` : undefined;
};

const inferCategoryGroup = (name?: string) => {
  const text = name || '';
  if (text.includes('校长书画')) return '高校校长书画作品';
  if (['声乐', '器乐', '舞蹈', '戏剧', '朗诵', '个人'].some((item) => text.includes(item))) return '艺术表演类';
  if (['美术', '设计', '影视', '校长'].some((item) => text.includes(item))) return '艺术作品类';
  if (text.includes('工作坊')) return '艺术实践工作坊';
  if (['论文', '案例', '成果', '美育'].some((item) => text.includes(item))) return '高校美育改革创新优秀成果';
  return '其他';
};

const projectWidthColumns = computed(() => [...(projectSerialColumn.value ? [projectSerialColumn.value] : []), ...projectTableColumns.value]);
const projectSerialNumber = (index: number) => (queryParams.pageNum - 1) * queryParams.pageSize + index + 1;
const projectTableWidthStorageKey = computed(
  () => `art-project-table-widths:${activeActivityId.value || 'all'}:${selectedReportingCategory.value?.id || 'all'}`
);
const { tableShellRef, columnWidth, handleColumnResize } = useArtTableColumnWidths({
  columns: projectWidthColumns,
  storageKey: projectTableWidthStorageKey,
  fitContainer: true
});

const syncSelectedReportingCategory = () => {
  if (!visibleReportingCategories.value.length) {
    selectedReportingCategoryId.value = undefined;
    return;
  }
  const stillVisible = visibleReportingCategories.value.some((item) => String(item.id || '') === String(selectedReportingCategoryId.value || ''));
  if (!stillVisible) {
    selectedReportingCategoryId.value = visibleReportingCategories.value[0].id;
  }
};

const loadReportingCatalog = async () => {
  const res = await listAvailableActivity();
  activityOptions.value = res.data || [];
  activeActivityId.value = resolveManagedActivityId();
  await loadReportingCategories();
};

const loadReportingCategories = async () => {
  if (!activeActivityId.value) {
    reportingCategoryList.value = [];
    categoryStatsMap.value = {};
    projectList.value = [];
    total.value = 0;
    return;
  }
  const res = await listAvailableCategory(activeActivityId.value);
  reportingCategoryList.value = res.data || [];
  syncSelectedReportingCategory();
  await loadReportingStats();
  queryParams.pageNum = 1;
  await getList();
};

const loadReportingStats = async () => {
  if (!activeActivityId.value || !reportingCategoryList.value.length) {
    categoryStatsMap.value = {};
    return;
  }
  const categoryIds = reportingCategoryList.value.map((item) => item.id).filter((id) => id !== undefined && id !== null);
  const res = await listMyProjectCategoryStats({
    activityId: activeActivityId.value,
    categoryIds: categoryIds.join(',')
  });
  const nextMap: Record<string, ProjectCategoryStatsVO> = {};
  (res.data || []).forEach((item) => {
    if (item.categoryId !== undefined && item.categoryId !== null) {
      nextMap[String(item.categoryId)] = item;
    }
  });
  categoryStatsMap.value = nextMap;
};

const handleCategoryChange = async () => {
  clearAllUploadErrors();
  formData.value = {};
  const category = categoryOptions.value.find((item) => String(item.id) === String(form.value.categoryId));
  categoryTip.value = category?.tipText || '';
  form.value.groupCode = undefined;
  form.value.groupName = undefined;
  await loadSchema(form.value.categoryId);
  await loadGroupOptions(form.value.activityId, form.value.categoryId);
};

const loadCategoryOptions = async (activityId?: string | number) => {
  if (!activityId) return;
  const { data } = await listAvailableCategory(activityId);
  categoryOptions.value = data;
};

const loadSchema = async (categoryId?: string | number) => {
  if (!categoryId) return;
  const fieldRes = await listSchoolField(categoryId);
  const requirementRes = await listSchoolFileRequirement(categoryId);
  fieldList.value = fieldRes.data;
  requirementList.value = requirementRes.data;
  normalizeFormDataForFields();
};

const loadGroupOptions = async (activityId?: string | number, categoryId?: string | number) => {
  if (!activityId) return;
  const { data } = await listActivityReportRuleSchoolOptions({ activityId, categoryId, enabled: true });
  const seen = new Set<string>();
  groupOptions.value = (data || []).filter((item: ActivityRuleGroupOptionVO) => {
    if (!item.groupCode || seen.has(item.groupCode) || !usesTopLevelGroup(item)) return false;
    seen.add(item.groupCode);
    return true;
  });
};

const handleGroupChange = () => {
  const option = groupOptions.value.find((item) => item.groupCode === form.value.groupCode);
  form.value.groupName = option?.groupName || option?.groupCode;
};

const saveDraft = async () => {
  const data: ProjectSaveForm = {
    id: form.value.id,
    activityId: form.value.activityId,
    categoryId: form.value.categoryId,
    projectName: form.value.projectName,
    groupCode: form.value.groupCode,
    groupName: form.value.groupName,
    formDataJson: JSON.stringify(formData.value),
    members: normalizeMembers()
  };
  const res = await saveProjectDraft(data);
  form.value = res.data;
  fileList.value = res.data.files || [];
  memberList.value = res.data.members || memberList.value;
  proxy?.$modal.msgSuccess('草稿已保存');
  await getList();
  await loadReportingStats();
};

const refreshCurrentProject = async () => {
  if (!form.value.id) return;
  clearAllUploadErrors();
  const { data } = await getProject(form.value.id);
  form.value = data;
  formData.value = data.formDataJson ? JSON.parse(data.formDataJson) : {};
  fileList.value = data.files || [];
  memberList.value = data.members || [];
  fieldList.value = data.fieldSchemas || fieldList.value;
  requirementList.value = data.fileRequirements || requirementList.value;
  normalizeFormDataForFields();
};

const setUploadError = (key: string, message: string, fileName?: string) => {
  uploadErrorMap.value = { ...uploadErrorMap.value, [key]: { message, fileName } };
};

const clearUploadError = (key: string) => {
  const next = { ...uploadErrorMap.value };
  delete next[key];
  uploadErrorMap.value = next;
};

const clearAllUploadErrors = () => {
  uploadErrorMap.value = {};
};

const uploadErrorFor = (requirementId?: string | number) => {
  if (requirementId === undefined || requirementId === null) return undefined;
  return uploadErrorMap.value[String(requirementId)];
};

const isAllExtAllowed = (allowedExt?: string) => {
  const value = String(allowedExt || '')
    .trim()
    .toLowerCase();
  return !value || value === '*' || value === 'all';
};

const normalizeAllowedExts = (allowedExt?: string) => {
  if (isAllExtAllowed(allowedExt)) return [];
  return String(allowedExt)
    .split(/[,，、;\s]+/)
    .map((item) => item.trim().toLowerCase().replace(/^\./, ''))
    .filter(Boolean);
};

const fileNameExt = (file?: File) => {
  const name = file?.name || '';
  return name.includes('.') ? name.split('.').pop()!.toLowerCase() : '';
};

const formatUploadSizeLimit = (maxSizeMb: number) => String(maxSizeMb).replace(/\.0+$/, '');

const materialLabel = (requirement: CategoryFileRequirementVO) => requirement.fileTypeName || '材料';

const formatUploadErrorMessage = (error: unknown) => {
  const err = error as any;
  const message = (typeof error === 'string' ? error : '') || err?.response?.data?.msg || err?.response?.data?.message || err?.message || '';
  const normalized = String(message || '').trim();
  if (!normalized || normalized === 'Error') return '上传失败：请稍后重试。';
  if (String(err?.config?.url || '').includes('aliyuncs.com')) {
    if (err?.response?.status === 403) return '上传失败：对象存储拒绝直传，请检查 OSS CORS 或直传签名配置。';
    if (normalized === 'Network Error') return '上传失败：对象存储直传失败，请检查 OSS CORS 或网络连接。';
  }
  if (normalized === 'Network Error') return '上传失败：网络连接异常，请稍后重试。';
  if (/timeout/i.test(normalized)) return '上传失败：请求超时，请稍后重试。';
  return normalized.startsWith('上传失败') ? normalized : `上传失败：${normalized}`;
};

const beforeUploadMaterial = (requirement: CategoryFileRequirementVO, file: File) => {
  const key = String(requirement.id);
  clearUploadError(key);
  const allowedExts = normalizeAllowedExts(requirement.allowedExt);
  const ext = fileNameExt(file);
  if (allowedExts.length && (!ext || !allowedExts.includes(ext))) {
    setUploadError(key, `上传失败：${materialLabel(requirement)}仅支持 ${allowedExts.join('/')} 格式，请重新选择文件。`, file?.name);
    return false;
  }
  const maxSizeMb = Number(requirement.maxSizeMb || 0);
  if (maxSizeMb > 0 && (file?.size || 0) > maxSizeMb * 1024 * 1024) {
    setUploadError(key, `上传失败：文件大小不能超过 ${formatUploadSizeLimit(maxSizeMb)}MB，请重新选择文件。`, file?.name);
    return false;
  }
  return true;
};

const uploadMaterial = async (requirementId: string | number, option: any) => {
  const key = String(requirementId);
  const file = option.file as File;
  clearUploadError(key);
  try {
    if (!form.value.id) {
      await saveDraft();
    }
    const initRes = await initProjectFileDirectUpload(form.value.id!, requirementId, {
      originalName: file.name,
      fileSize: file.size,
      contentType: file.type || 'application/octet-stream'
    });
    const direct = initRes.data;
    if (!direct?.uploadUrl || !direct?.uploadToken || !direct?.objectKey) {
      throw new Error('直传地址生成失败，请联系管理员确认 OSS 配置');
    }
    await uploadProjectFileToOss(direct.uploadUrl, file, direct.contentType || file.type || 'application/octet-stream', (event) => {
      const total = event.total || file?.size || 0;
      const loaded = event.loaded || 0;
      const percent = total > 0 ? Math.min(96, Math.round((loaded / total) * 96)) : 0;
      option.onProgress?.({ percent });
    });
    const res = await completeProjectFileDirectUpload(form.value.id!, requirementId, {
      uploadToken: direct.uploadToken,
      objectKey: direct.objectKey,
      originalName: file.name,
      fileSize: file.size,
      contentType: direct.contentType || file.type || 'application/octet-stream'
    });
    await refreshCurrentProject();
    option.onSuccess?.(res.data);
    if (res.data?.checkStatus === 'failed') {
      const message = normalizeReviewMessage(res.data.checkMessage) || '技术校验未通过，请查看提示后替换文件';
      setUploadError(key, `已上传但校验未通过：${message}`, file?.name);
      proxy?.$modal.msgWarning('文件已上传，但技术校验未通过');
    } else {
      clearUploadError(key);
      proxy?.$modal.msgSuccess('上传成功');
    }
  } catch (error) {
    setUploadError(key, formatUploadErrorMessage(error), file?.name);
    option.onError?.(error);
    throw error;
  }
};

const removeFile = async (file: ProjectFileVO) => {
  if (!form.value.id || !file.id || !canEdit(form.value.status)) return;
  await deleteProjectFile(form.value.id, file.id);
  fileList.value = fileList.value.filter((item) => item.id !== file.id);
};

const submitCurrent = async () => {
  if (!String(form.value.projectName || '').trim()) {
    proxy?.$modal.msgWarning('请先填写项目名称后再提交');
    return;
  }
  await saveDraft();
  await refreshCurrentProject();
  await submitProject(form.value.id!);
  proxy?.$modal.msgSuccess('提交成功');
  dialog.visible = false;
  await getList();
  await loadReportingStats();
};

const submitRow = async (row: ProjectVO) => {
  if (!String(row.projectName || '').trim()) {
    proxy?.$modal.msgWarning('该项目名称为空，请先进入编辑页补全后再提交');
    return;
  }
  await proxy?.$modal.confirm(`确认提交项目「${projectDisplayName(row)}」？提交后不可修改。`);
  await submitProject(row.id!);
  proxy?.$modal.msgSuccess('提交成功');
  await getList();
  await loadReportingStats();
};

const withdrawSubmitRow = async (row: ProjectVO) => {
  await proxy?.$modal.confirm(`确认撤回项目「${projectDisplayName(row)}」的提交？撤回后可继续编辑并重新提交。`);
  await withdrawSubmitProject(row.id!);
  proxy?.$modal.msgSuccess('已撤回提交，可继续编辑');
  await getList();
  await loadReportingStats();
};

const projectSubmitActionDisabled = (row: ProjectVO) => {
  if (!canSubmitProject.value) return true;
  return !canEdit(row.status) && !canWithdrawSubmit(row);
};

const projectSubmitActionTitle = (row: ProjectVO) => {
  if (!canSubmitProject.value) return '无提交或撤回权限';
  if (row.status === 'audit_passed') return '项目已审核通过，不能撤回';
  if (!canEdit(row.status) && !canWithdrawSubmit(row)) return '当前状态不能提交或撤回';
  return '';
};

const handleProjectSubmitAction = async (row: ProjectVO) => {
  if (projectSubmitActionDisabled(row)) return;
  if (canWithdrawSubmit(row)) {
    await withdrawSubmitRow(row);
    return;
  }
  await submitRow(row);
};

const projectDeleteActionDisabled = (row: ProjectVO) => !canRemoveProject.value || !canEdit(row.status);
const projectDeleteActionTitle = (row: ProjectVO) => {
  if (!canRemoveProject.value) return '无删除权限';
  if (!canEdit(row.status)) return '请先撤回后删除';
  return '';
};
const handleProjectDeleteAction = async (row: ProjectVO) => {
  if (projectDeleteActionDisabled(row)) return;
  await recycleRow(row);
};

const recycleRow = async (row: ProjectVO) => {
  await proxy?.$modal.confirm('是否删除');
  await recycleProject(row.id!);
  proxy?.$modal.msgSuccess('已移入回收站');
  await getList();
  await loadReportingStats();
};

const projectDisplayName = (row: ProjectVO) => {
  return String(row.projectName || row.projectNo || row.categoryName || '未命名项目').trim();
};

const filesByRequirement = (requirementId?: string | number) => {
  return fileList.value.filter((file) => String(file.requirementId) === String(requirementId));
};

const addMember = (memberType = 'student') => {
  memberList.value.push({ memberType, status: 'active', sortOrder: memberList.value.length + 1 });
};

const normalizeMembers = () => {
  return memberList.value
    .map((item, index) => ({
      ...item,
      memberType: item.memberType || 'student',
      status: item.status || 'active',
      sortOrder: item.sortOrder || index + 1
    }))
    .filter((item) => item.name || item.studentNo);
};

const formatAllowedExt = (allowedExt?: string) => {
  if (!allowedExt || allowedExt === '*' || allowedExt === 'all') {
    return '所有格式';
  }
  return allowedExt;
};

const acceptExt = (allowedExt?: string) => {
  if (!allowedExt || allowedExt === '*' || allowedExt === 'all') {
    return undefined;
  }
  return allowedExt
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
    .map((item) => (item.startsWith('.') ? item : `.${item}`))
    .join(',');
};

const previewTagType = (status?: string) => {
  if (status === 'converted') return 'success';
  if (status === 'failed') return 'danger';
  return 'info';
};

const parseOptions = (text?: string) => {
  if (!text) return [];
  try {
    return JSON.parse(text);
  } catch {
    return [];
  }
};

const parseRuleJson = (text?: string) => {
  if (!text) return {};
  try {
    const parsed = JSON.parse(text);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
};

const quotaTargetFieldKey = (ruleJson?: string) => {
  const rule = parseRuleJson(ruleJson) as Record<string, any>;
  return String(rule.targetFieldKey || rule.groupFieldKey || '');
};

const usesTopLevelGroup = (rule: ActivityRuleGroupOptionVO) => {
  const targetFieldKey = quotaTargetFieldKey(rule.ruleJson);
  return !targetFieldKey || targetFieldKey === 'groupCode' || targetFieldKey === '__group_code';
};

const topicCategoryField = (field: CategoryFieldSchemaVO) => field.fieldKey === 'topicCategory' || field.fieldLabel === '选题类别';

const originalityHintVisible = (field: CategoryFieldSchemaVO) =>
  ['isOriginal', 'is_original'].includes(field.fieldKey || '') || field.fieldLabel === '是否原创';

const wideField = (field: CategoryFieldSchemaVO) => field.fieldType === 'textarea' || topicCategoryField(field);

const fieldRuleNumber = (field: CategoryFieldSchemaVO, key: string) => {
  const rule = parseRuleJson(field.validationJson) as Record<string, any>;
  const value = rule[key];
  if (value === undefined || value === null || value === '') return undefined;
  const number = Number(value);
  return Number.isFinite(number) ? number : undefined;
};

const fieldTextMaxLength = (field: CategoryFieldSchemaVO) => {
  if (!['input', 'textarea', 'id_card'].includes(field.fieldType || '')) return undefined;
  const maxLength = fieldRuleNumber(field, 'maxLength');
  return maxLength && maxLength > 0 ? Math.floor(maxLength) : undefined;
};

const fieldTextMinLength = (field: CategoryFieldSchemaVO) => {
  if (!['input', 'textarea', 'id_card'].includes(field.fieldType || '')) return undefined;
  const minLength = fieldRuleNumber(field, 'minLength');
  return minLength && minLength > 0 ? Math.floor(minLength) : undefined;
};

const fieldShowWordLimit = (field: CategoryFieldSchemaVO) => {
  if (!fieldTextMaxLength(field)) return false;
  const rule = parseRuleJson(field.validationJson) as Record<string, any>;
  return rule.showWordLimit !== false;
};

const fieldNumberMin = (field: CategoryFieldSchemaVO) => (field.fieldType === 'number' ? fieldRuleNumber(field, 'min') : undefined);

const fieldNumberMax = (field: CategoryFieldSchemaVO) => (field.fieldType === 'number' ? fieldRuleNumber(field, 'max') : undefined);

const fieldNumberPrecision = (field: CategoryFieldSchemaVO) => {
  if (field.fieldType !== 'number') return undefined;
  const rule = parseRuleJson(field.validationJson) as Record<string, any>;
  if (rule.integerOnly === true) return 0;
  const precision = fieldRuleNumber(field, 'precision');
  return precision !== undefined && precision >= 0 ? Math.floor(precision) : undefined;
};

const fieldNumberStep = (field: CategoryFieldSchemaVO) => {
  if (field.fieldType !== 'number') return undefined;
  const step = fieldRuleNumber(field, 'step');
  return step !== undefined && step > 0 ? step : undefined;
};

const inputWidthByMode = (mode: string) => {
  const widths: Record<string, string> = {
    short: '180px',
    medium: '280px',
    long: '420px',
    full: '100%'
  };
  return widths[mode] || '';
};

const normalizeCustomInputWidth = (value: unknown) => {
  const text = String(value || '').trim();
  const pxMatch = text.match(/^(\d{2,3})px$/);
  if (pxMatch) {
    const width = Number(pxMatch[1]);
    return width >= 80 && width <= 960 ? `${width}px` : '';
  }
  const percentMatch = text.match(/^(\d{1,3})%$/);
  if (percentMatch) {
    const width = Number(percentMatch[1]);
    return width >= 10 && width <= 100 ? `${width}%` : '';
  }
  return '';
};

const defaultInputWidth = (field: CategoryFieldSchemaVO) => {
  if (field.fieldType === 'number') return '180px';
  if (field.fieldType === 'date') return '240px';
  return '';
};

const fieldControlStyle = (field: CategoryFieldSchemaVO) => {
  const rule = parseRuleJson(field.validationJson) as Record<string, any>;
  const mode = String(rule.inputWidthMode || 'default');
  const width = mode === 'custom' ? normalizeCustomInputWidth(rule.inputWidthCustom) : inputWidthByMode(mode) || defaultInputWidth(field);
  return width && width !== '100%' ? ({ '--art-field-control-width': width } as Record<string, string>) : {};
};

const normalizeFormDataForFields = () => {
  fieldList.value.forEach((field) => {
    if (field.fieldType === 'checkbox' && field.fieldKey && !Array.isArray(formData.value[field.fieldKey])) {
      formData.value[field.fieldKey] = [];
    }
  });
};

const canEdit = (status?: string) => !status || status === 'draft' || status === 'returned' || status === 'participant_returned';
const canWithdrawSubmit = (row: ProjectVO) =>
  row.status === 'participant_submitted' || (row.status === 'school_submitted' && !row.schoolFinalBatchId);
const statusLabels: Record<string, string> = {
  draft: '草稿',
  participant_submitted: '已提交，待学校审核',
  participant_returned: '学校退回修改',
  school_approved: '学校已推荐',
  school_submitted: '学校已最终提交',
  returned: '省级审核退回',
  audit_passed: '审核通过'
};
type ElTagType = 'primary' | 'success' | 'warning' | 'danger' | 'info';
const statusTypes: Record<string, ElTagType> = {
  draft: 'info',
  participant_submitted: 'warning',
  participant_returned: 'danger',
  school_approved: 'primary',
  school_submitted: 'warning',
  returned: 'danger',
  audit_passed: 'success'
};
const statusSemantics: Record<string, ArtListStatusSemantic> = {
  draft: 'draft',
  participant_submitted: 'pending',
  participant_returned: 'returned',
  school_approved: 'pending',
  school_submitted: 'pending',
  returned: 'returned',
  audit_passed: 'approved'
};
const statusLabel = (status?: string) => statusLabels[status || ''] || status || '-';
const statusType = (status?: string): ElTagType => statusTypes[status || ''] || 'info';
const statusSemantic = (status?: string): ArtListStatusSemantic => statusSemantics[status || ''] || 'draft';

watch(
  () => [
    route.query.categoryGroup,
    route.query.group,
    route.query.categoryId,
    route.query.categoryCode,
    route.query.categoryName,
    route.query.categoryKeyword,
    route.query.keyword
  ],
  async () => {
    queryParams.pageNum = 1;
    syncSelectedReportingCategory();
    await getList();
  }
);

onMounted(async () => {
  await loadArtDetailDisplayConfig(true);
  await loadReportingCatalog();
});
</script>

<style scoped lang="scss">
.section-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.activity-select {
  width: 260px;
}

.project-row-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.project-row-action {
  min-width: 58px;
  margin-left: 0 !important;
}

.material-upload-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 0;
}

.material-upload-error {
  flex: 1 1 160px;
  min-width: 120px;
  color: var(--el-color-danger);
  font-size: 12px;
  line-height: 18px;
  text-align: left;
  word-break: break-all;
}

.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 40px;
  font-weight: 600;
  color: #303133;
}

.table-add-button {
  flex: 0 0 auto;
  min-width: 92px;
  height: 36px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: 0 2px 6px rgb(64 158 255 / 18%);
}

.reporting-category-shell {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reporting-category-switcher {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.reporting-category-switch {
  height: 32px;
  padding: 0 14px;
  color: #606266;
  font-size: 13px;
  background: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.16s ease;
}

.reporting-category-switch:hover,
.reporting-category-switch.active {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.reporting-overview {
  display: grid;
  grid-template-columns: minmax(240px, 320px) minmax(0, 1fr);
  align-items: stretch;
  gap: 14px;
}

.reporting-overview-main,
.reporting-overview-tip {
  min-width: 0;
  min-height: 104px;
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}

.reporting-overview-main {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-color: var(--el-color-primary-light-5);
  box-shadow: inset 3px 0 0 var(--el-color-primary);
}

.reporting-overview-title {
  overflow: hidden;
  display: -webkit-box;
  color: #1f2937;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  word-break: break-all;
}

.reporting-overview-stats {
  margin: 8px 0 14px;
  color: #909399;
  font-size: 13px;
  line-height: 1.45;
  word-break: break-word;
}

.reporting-overview-button {
  align-self: flex-start;
  min-width: 116px;
  height: 36px;
  font-weight: 700;
}

.reporting-tip-title {
  color: #303133;
  font-size: 15px;
  font-weight: 700;
}

.reporting-tip-content {
  margin-top: 10px;
  color: #606266;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.topic-radio-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(260px, 1fr));
  gap: 12px 28px;
  width: 100%;
}

.topic-radio-grid :deep(.el-radio) {
  height: auto;
  margin-right: 0;
  white-space: normal;
  line-height: 1.5;
}

.dynamic-radio-field {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0 12px;
  min-width: 0;
}

.field-control-limiter {
  width: min(100%, var(--art-field-control-width, 100%));
  max-width: 100%;
}

.field-control-limiter :deep(.el-input),
.field-control-limiter :deep(.el-select),
.field-control-limiter :deep(.el-textarea),
.field-control-limiter :deep(.el-date-editor),
.field-control-limiter :deep(.el-input-number) {
  width: 100%;
}

.originality-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 900px) {
  .reporting-overview {
    grid-template-columns: 1fr;
  }

  .topic-radio-grid {
    grid-template-columns: 1fr;
  }

  .field-control-limiter {
    width: 100%;
  }
}
</style>
