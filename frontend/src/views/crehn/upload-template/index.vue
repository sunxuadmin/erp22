<template>
  <div class="p-2">
    <el-card shadow="hover" class="mb-[10px]">
      <el-form :model="queryParams" :inline="true">
        <el-form-item label="模板名称"><el-input v-model="queryParams.templateName" clearable @keyup.enter="getList" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryParams.categoryGroup" clearable placeholder="全部分类" style="width: 220px">
            <el-option v-for="item in templateGroups" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
          <el-button type="primary" plain icon="Plus" @click="openForm()">新增模板</el-button>
          <el-button plain icon="Link" @click="openCategoryTemplate">从子类别生成模板</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover">
      <el-table v-loading="loading" border :data="rows">
        <el-table-column label="模板名称" prop="templateName" min-width="180" />
        <el-table-column label="分类" prop="categoryGroup" width="190" />
        <el-table-column label="检查模式" prop="checkMode" width="110" />
        <el-table-column label="启用" prop="enabled" width="90">
          <template #default="scope"><el-tag :type="scope.row.enabled ? 'success' : 'info'">{{ scope.row.enabled ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="排序" prop="sortOrder" width="90" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="openForm(scope.row)">编辑</el-button>
            <el-button link type="primary" icon="CopyDocument" @click="copyRow(scope.row)">复制</el-button>
            <el-button link type="success" icon="Connection" @click="openApply(scope.row)">应用</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </el-card>

    <ArtPopupDialog v-model="dialog.visible" :title="dialog.title" width="1080px" append-to-body>
      <el-form :model="form" label-width="110px">
        <el-row :gutter="10">
          <el-col :span="12"><el-form-item label="模板名称" required><el-input v-model="form.templateName" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="模板分类">
              <el-select v-model="form.categoryGroup" filterable allow-create class="w-full">
                <el-option v-for="item in templateGroups" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="检查模式">
              <el-select v-model="form.checkMode" class="w-full">
                <el-option label="人工检查" value="manual" />
                <el-option label="自动检查" value="auto" />
                <el-option label="自动+人工" value="mixed" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6"><el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="排序"><el-input-number v-model="form.sortOrder" class="w-full" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="填报提示"><el-input v-model="form.tipText" type="textarea" :rows="2" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="项目名称标签"><el-input v-model="ruleForm.projectName.label" placeholder="项目名称" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="项目名称必填"><el-switch v-model="ruleForm.projectName.required" /></el-form-item></el-col>
        </el-row>

        <el-tabs>
          <el-tab-pane label="申报字段">
            <div class="template-helper-toolbar">
              <el-button type="primary" plain icon="MagicStick" @click="completeShowcaseFields(false)">补齐展演标准字段</el-button>
              <el-button plain icon="RefreshRight" @click="completeShowcaseFields(true)">重置为展演标准字段</el-button>
              <el-text type="info">用于组别、项目类型、节目形式、展演顺序和原创承诺，保存后可被作品汇总和展演顺序读取。</el-text>
            </div>
            <el-table border :data="fieldRows">
              <el-table-column label="字段名称" min-width="150"><template #default="scope"><el-input v-model="scope.row.fieldLabel" /></template></el-table-column>
              <el-table-column label="类型" width="140">
                <template #default="scope">
                  <el-select v-model="scope.row.fieldType">
                    <el-option v-for="type in fieldTypeOptions" :key="type.value" :label="type.label" :value="type.value" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="校验" width="140">
                <template #default="scope">
                  <el-select v-model="scope.row.validationType" clearable placeholder="不校验">
                    <el-option v-for="item in validationOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="详细规则JSON" min-width="220">
                <template #default="scope">
                  <el-input
                    v-model="scope.row.validationJsonText"
                    type="textarea"
                    :rows="2"
                    placeholder='如 {"maxLength":200}'
                  />
                </template>
              </el-table-column>
              <el-table-column label="必填" width="80"><template #default="scope"><el-switch v-model="scope.row.required" /></template></el-table-column>
              <el-table-column label="脱敏" width="80"><template #default="scope"><el-switch v-model="scope.row.sensitiveFlag" /></template></el-table-column>
              <el-table-column label="选项" min-width="210">
                <template #default="scope">
                  <el-select
                    v-model="scope.row.optionsList"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    collapse-tags
                    collapse-tags-tooltip
                    placeholder="添加选项"
                    :disabled="!optionFieldTypes.includes(scope.row.fieldType)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="排序" width="100"><template #default="scope"><el-input-number v-model="scope.row.sortOrder" /></template></el-table-column>
              <el-table-column label="操作" width="80"><template #default="scope"><el-button link type="danger" icon="Minus" @click="fieldRows.splice(scope.$index, 1)">删除</el-button></template></el-table-column>
            </el-table>
            <el-button class="mt-2" type="primary" plain icon="Plus" @click="addField()">添加字段</el-button>
          </el-tab-pane>

          <el-tab-pane label="成员字段">
            <div class="member-field-title">作者字段</div>
            <el-table border :data="memberFieldRows.author">
              <el-table-column label="字段名称" min-width="150"><template #default="scope"><el-input v-model="scope.row.fieldLabel" /></template></el-table-column>
              <el-table-column label="字段编码" min-width="140"><template #default="scope"><el-input v-model="scope.row.fieldKey" placeholder="自动生成" /></template></el-table-column>
              <el-table-column label="类型" width="130">
                <template #default="scope">
                  <el-select v-model="scope.row.fieldType">
                    <el-option v-for="type in memberFieldTypeOptions" :key="type.value" :label="type.label" :value="type.value" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="必填" width="80"><template #default="scope"><el-switch v-model="scope.row.required" /></template></el-table-column>
              <el-table-column label="选项" min-width="210">
                <template #default="scope">
                  <el-select v-model="scope.row.optionsList" multiple filterable allow-create default-first-option collapse-tags collapse-tags-tooltip placeholder="添加选项" :disabled="!optionFieldTypes.includes(scope.row.fieldType)" />
                </template>
              </el-table-column>
              <el-table-column label="排序" width="100"><template #default="scope"><el-input-number v-model="scope.row.sortOrder" /></template></el-table-column>
              <el-table-column label="操作" width="80"><template #default="scope"><el-button link type="danger" icon="Minus" @click="memberFieldRows.author.splice(scope.$index, 1)">删除</el-button></template></el-table-column>
            </el-table>
            <el-button class="mt-2 mb-4" type="primary" plain icon="Plus" @click="addMemberField('author')">添加作者字段</el-button>

            <div class="member-field-title">学生/指导教师字段</div>
            <el-table border :data="memberFieldRows.participant">
              <el-table-column label="字段名称" min-width="150"><template #default="scope"><el-input v-model="scope.row.fieldLabel" /></template></el-table-column>
              <el-table-column label="字段编码" min-width="140"><template #default="scope"><el-input v-model="scope.row.fieldKey" placeholder="自动生成" /></template></el-table-column>
              <el-table-column label="类型" width="130">
                <template #default="scope">
                  <el-select v-model="scope.row.fieldType">
                    <el-option v-for="type in memberFieldTypeOptions" :key="type.value" :label="type.label" :value="type.value" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="必填" width="80"><template #default="scope"><el-switch v-model="scope.row.required" /></template></el-table-column>
              <el-table-column label="选项" min-width="210">
                <template #default="scope">
                  <el-select v-model="scope.row.optionsList" multiple filterable allow-create default-first-option collapse-tags collapse-tags-tooltip placeholder="添加选项" :disabled="!optionFieldTypes.includes(scope.row.fieldType)" />
                </template>
              </el-table-column>
              <el-table-column label="排序" width="100"><template #default="scope"><el-input-number v-model="scope.row.sortOrder" /></template></el-table-column>
              <el-table-column label="操作" width="80"><template #default="scope"><el-button link type="danger" icon="Minus" @click="memberFieldRows.participant.splice(scope.$index, 1)">删除</el-button></template></el-table-column>
            </el-table>
            <el-button class="mt-2" type="primary" plain icon="Plus" @click="addMemberField('participant')">添加学生/指导教师字段</el-button>
          </el-tab-pane>

          <el-tab-pane label="附件要求">
            <el-table border :data="fileRows">
              <el-table-column label="材料类型" min-width="150"><template #default="scope"><el-input v-model="scope.row.fileTypeName" placeholder="如 作品照片" /></template></el-table-column>
              <el-table-column label="类型编码" width="140"><template #default="scope"><el-input v-model="scope.row.fileTypeCode" placeholder="photo/document" /></template></el-table-column>
              <el-table-column label="允许格式" min-width="180"><template #default="scope"><el-input v-model="scope.row.allowedExt" placeholder="所有格式 / jpg,png,pdf" /></template></el-table-column>
              <el-table-column label="大小MB" width="110"><template #default="scope"><el-input-number v-model="scope.row.maxSizeMb" /></template></el-table-column>
              <el-table-column label="最少" width="90"><template #default="scope"><el-input-number v-model="scope.row.minCount" /></template></el-table-column>
              <el-table-column label="最多" width="90"><template #default="scope"><el-input-number v-model="scope.row.maxCount" /></template></el-table-column>
              <el-table-column label="必传" width="80"><template #default="scope"><el-switch v-model="scope.row.required" /></template></el-table-column>
              <el-table-column label="提示语" min-width="240"><template #default="scope"><el-input v-model="scope.row.tipText" type="textarea" :rows="2" placeholder="显示在学校端上传作品/附件列表中" /></template></el-table-column>
              <el-table-column label="规则JSON" min-width="260"><template #default="scope"><el-input v-model="scope.row.ruleJson" type="textarea" :rows="2" /></template></el-table-column>
              <el-table-column label="操作" width="80"><template #default="scope"><el-button link type="danger" icon="Minus" @click="fileRows.splice(scope.$index, 1)">删除</el-button></template></el-table-column>
            </el-table>
            <el-button class="mt-2" type="primary" plain icon="Plus" @click="addFile()">添加附件要求</el-button>
          </el-tab-pane>

          <el-tab-pane label="规则说明">
            <el-row :gutter="10">
              <el-col :span="8"><el-form-item label="是否启用组别"><el-switch v-model="ruleForm.groupEnabled" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="关闭比例限制"><el-switch v-model="ruleForm.ratioDisabled" /></el-form-item></el-col>
              <el-col :span="8"><el-form-item label="原创承诺"><el-switch v-model="ruleForm.requiresOriginality" /></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="校验类型"><el-input v-model="ruleForm.validatorType" placeholder="performance/artwork/workshop/aesthetic_achievement" /></el-form-item></el-col>
              <el-col :span="24">
                <el-divider content-position="left">标准字段映射</el-divider>
              </el-col>
              <el-col :span="8">
                <el-form-item label="组别字段">
                  <el-select v-model="canonicalForm.groupName" clearable filterable class="w-full" placeholder="选择申报字段">
                    <el-option v-for="item in fieldRows" :key="fieldOptionKey(item)" :label="fieldOptionLabel(item)" :value="fieldOptionKey(item)" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="项目类型">
                  <el-select v-model="canonicalForm.projectType" clearable filterable class="w-full" placeholder="选择申报字段">
                    <el-option v-for="item in fieldRows" :key="fieldOptionKey(item)" :label="fieldOptionLabel(item)" :value="fieldOptionKey(item)" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="节目形式">
                  <el-select v-model="canonicalForm.programForm" clearable filterable class="w-full" placeholder="选择申报字段">
                    <el-option v-for="item in fieldRows" :key="fieldOptionKey(item)" :label="fieldOptionLabel(item)" :value="fieldOptionKey(item)" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="展演顺序">
                  <el-select v-model="canonicalForm.performanceOrder" clearable filterable class="w-full" placeholder="选择申报字段">
                    <el-option v-for="item in fieldRows" :key="fieldOptionKey(item)" :label="fieldOptionLabel(item)" :value="fieldOptionKey(item)" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="原创声明">
                  <el-select v-model="canonicalForm.originalityCommitment" clearable filterable class="w-full" placeholder="选择申报字段">
                    <el-option v-for="item in fieldRows" :key="fieldOptionKey(item)" :label="fieldOptionLabel(item)" :value="fieldOptionKey(item)" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="24"><el-form-item label="人工审核提示"><el-input v-model="ruleForm.manualCheckTipsText" type="textarea" :rows="3" /></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="驳回原因模板"><el-input v-model="ruleForm.rejectReasonTemplatesText" type="textarea" :rows="3" /></el-form-item></el-col>
              <el-col :span="24"><el-form-item label="规则备注"><el-input v-model="ruleForm.remark" type="textarea" :rows="3" /></el-form-item></el-col>
            </el-row>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">保存</el-button>
        <el-button @click="dialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="applyDialog.visible" title="应用模板到类别" width="640px" append-to-body>
      <el-form :model="applyForm" label-width="120px">
        <el-form-item label="活动">
          <el-select v-model="applyForm.activityId" filterable class="w-full" @change="loadApplyCategories">
            <el-option v-for="item in activityOptions" :key="item.id" :label="activityLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="applyForm.categoryId" filterable class="w-full">
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.categoryName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="应用方式">
          <el-select v-model="applyForm.applyMode" class="w-full">
            <el-option label="追加到当前类别" value="append" />
            <el-option label="覆盖字段、附件和说明" value="overwrite" />
            <el-option label="只应用字段" value="fields_only" />
            <el-option label="只应用附件要求" value="files_only" />
            <el-option label="只应用规则说明" value="rules_only" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitApply">应用</el-button>
        <el-button @click="applyDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="categoryTemplateDialog.visible" title="从子类别生成模板" width="640px" append-to-body>
      <el-form :model="categoryTemplateForm" label-width="120px">
        <el-form-item label="活动">
          <el-select v-model="categoryTemplateForm.activityId" filterable class="w-full" @change="loadCategoryTemplateCategories">
            <el-option v-for="item in activityOptions" :key="item.id" :label="activityLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="子类别">
          <el-select v-model="categoryTemplateForm.categoryId" filterable class="w-full" @change="syncCategoryTemplateName">
            <el-option v-for="item in categoryTemplateOptions" :key="item.id" :label="item.categoryName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板名称">
          <el-input v-model="categoryTemplateForm.templateName" placeholder="不填则使用子类别名称 + 模板" />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          title="生成全部时，会按所选活动下每个启用子类别生成独立模板；已有同编码模板会用当前类别配置更新。"
        />
      </el-form>
      <template #footer>
        <el-button @click="submitAllCategoryTemplates">生成全部子类别模板</el-button>
        <el-button type="primary" @click="submitCategoryTemplate">生成当前子类别</el-button>
        <el-button @click="categoryTemplateDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ArtUploadTemplate" lang="ts">
import { listActivityOptions, listCategoryOptions } from '@/api/crehn/activity';
import { applyUploadTemplate, copyUploadTemplate, listUploadTemplate, saveTemplateFromCategory, saveUploadTemplate, syncUploadTemplatesFromActivity } from '@/api/crehn/config';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const loading = ref(false);
const total = ref(0);
const rows = ref<any[]>([]);
const activityOptions = ref<any[]>([]);
const categoryOptions = ref<any[]>([]);
const categoryTemplateOptions = ref<any[]>([]);
const queryParams = reactive({ pageNum: 1, pageSize: 10, templateName: '', categoryGroup: '' });
const dialog = reactive({ visible: false, title: '' });
const applyDialog = reactive({ visible: false });
const categoryTemplateDialog = reactive({ visible: false });
const form = ref<any>({});
const applyForm = ref<any>({ applyMode: 'append' });
const categoryTemplateForm = ref<any>({});
const fieldRows = ref<any[]>([]);
const fileRows = ref<any[]>([]);
const ruleForm = ref<any>({ projectName: { label: '项目名称', required: false } });
const canonicalForm = reactive<Record<string, string>>({
  groupName: '',
  projectType: '',
  programForm: '',
  performanceOrder: '',
  originalityCommitment: ''
});
const memberFieldRows = reactive<Record<'author' | 'participant', any[]>>({ author: [], participant: [] });
const optionFieldTypes = ['select', 'radio', 'checkbox'];
const showcaseStandardFields = [
  { fieldKey: 'groupName', fieldLabel: '组别', fieldType: 'select', required: false, optionsText: '甲组,乙组,高校组,中小学组' },
  { fieldKey: 'projectType', fieldLabel: '项目类型', fieldType: 'select', required: false, optionsText: '艺术表演节目,艺术作品,艺术实践工作坊,高校美育改革创新成果' },
  { fieldKey: 'programForm', fieldLabel: '节目形式', fieldType: 'select', required: false, optionsText: '声乐,器乐,舞蹈,戏剧,朗诵,书法,绘画,摄影,短片,论文,案例' },
  { fieldKey: 'performanceOrder', fieldLabel: '展演顺序', fieldType: 'input', required: false, optionsText: '' },
  { fieldKey: 'originalityCommitment', fieldLabel: '原创声明', fieldType: 'checkbox', required: true, optionsText: '确认作品为原创或已取得授权' }
];
const fieldTypeOptions = [
  { label: '单行文本', value: 'input' },
  { label: '多行文本', value: 'textarea' },
  { label: '下拉选择', value: 'select' },
  { label: '单选', value: 'radio' },
  { label: '多选', value: 'checkbox' },
  { label: '日期', value: 'date' },
  { label: '数字', value: 'number' },
  { label: '手机号', value: 'phone' },
  { label: '邮箱', value: 'email' },
  { label: '文件', value: 'file' }
];
const memberFieldTypeOptions = fieldTypeOptions.filter((item) => item.value !== 'file');
const validationOptions = [
  { label: '姓名', value: 'name' },
  { label: '性别', value: 'gender' },
  { label: '民族', value: 'nation' },
  { label: '身份证号', value: 'id_card' },
  { label: '手机号', value: 'phone' },
  { label: '邮箱', value: 'email' },
  { label: '数字', value: 'number' },
  { label: '日期', value: 'date' },
  { label: '文件', value: 'file' },
  { label: '状态', value: 'status' },
  { label: '备注', value: 'remark' }
];
const templateGroups = ['艺术表演节目', '艺术作品', '艺术实践工作坊', '高校美育改革创新成果'];

const getList = async () => {
  loading.value = true;
  const res = await listUploadTemplate(queryParams);
  rows.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

const loadActivities = async () => {
  const { data } = await listActivityOptions();
  activityOptions.value = data || [];
};

const openForm = (row?: any) => {
  form.value = row ? { ...row } : { enabled: true, sortOrder: 0, checkMode: 'mixed', categoryGroup: '艺术作品' };
  fieldRows.value = parseArray(form.value.fieldSchemaJson).map(toFieldRow);
  fileRows.value = parseArray(form.value.fileRequirementJson).map(toFileRow);
  ruleForm.value = parseObject(form.value.ruleJson);
  ensureRuleDefaults();
  hydrateCanonicalForm();
  hydrateMemberFieldRows(ruleForm.value.memberFieldGroups);
  if (!row) {
    addField('作品名称', 'input', true);
    addField('是否原创', 'radio', true, '是,否');
    addMemberField('author', '作者姓名', 'name', 'input', true);
    addMemberField('participant', '姓名', 'name', 'input', true);
    addMemberField('participant', '学号', 'studentNo', 'input', false);
    addFile('作品材料', 'material', '所有格式', 500, 1, 5, true);
  }
  dialog.title = row ? '编辑上传规则模板' : '新增上传规则模板';
  dialog.visible = true;
};

const submitForm = async () => {
  normalizeRuleForm();
  syncCanonicalToRuleForm();
  ruleForm.value.memberFieldGroups = {
    author: memberFieldRows.author.map(toMemberFieldPayload),
    participant: memberFieldRows.participant.map(toMemberFieldPayload)
  };
  const payload = {
    ...form.value,
    fieldSchemaJson: JSON.stringify(fieldRows.value.map(toFieldPayload)),
    fileRequirementJson: JSON.stringify(fileRows.value.map(toFilePayload)),
    ruleJson: JSON.stringify(ruleForm.value || {})
  };
  await saveUploadTemplate(payload);
  proxy?.$modal.msgSuccess('保存成功');
  dialog.visible = false;
  await getList();
};

const copyRow = async (row: any) => {
  await copyUploadTemplate(row.id);
  proxy?.$modal.msgSuccess('复制成功');
  await getList();
};

const openApply = (row: any) => {
  applyForm.value = { templateId: row.id, applyMode: 'append' };
  categoryOptions.value = [];
  applyDialog.visible = true;
};

const loadApplyCategories = async () => {
  categoryOptions.value = applyForm.value.activityId ? (await listCategoryOptions(applyForm.value.activityId)).data || [] : [];
};

const submitApply = async () => {
  await applyUploadTemplate(applyForm.value);
  proxy?.$modal.msgSuccess('应用成功');
  applyDialog.visible = false;
};

const openCategoryTemplate = () => {
  categoryTemplateForm.value = {};
  categoryTemplateOptions.value = [];
  categoryTemplateDialog.visible = true;
};

const loadCategoryTemplateCategories = async () => {
  categoryTemplateForm.value.categoryId = undefined;
  categoryTemplateForm.value.templateName = '';
  categoryTemplateOptions.value = categoryTemplateForm.value.activityId
    ? (await listCategoryOptions(categoryTemplateForm.value.activityId)).data || []
    : [];
};

const syncCategoryTemplateName = () => {
  const category = categoryTemplateOptions.value.find((item) => String(item.id) === String(categoryTemplateForm.value.categoryId));
  categoryTemplateForm.value.templateName = category?.categoryName ? `${category.categoryName}上传规则模板` : '';
};

const submitCategoryTemplate = async () => {
  if (!categoryTemplateForm.value.categoryId) {
    proxy?.$modal.msgWarning('请选择子类别');
    return;
  }
  await saveTemplateFromCategory(categoryTemplateForm.value.categoryId, categoryTemplateForm.value.templateName || '');
  proxy?.$modal.msgSuccess('子类别模板已生成/更新');
  categoryTemplateDialog.visible = false;
  await getList();
};

const submitAllCategoryTemplates = async () => {
  if (!categoryTemplateForm.value.activityId) {
    proxy?.$modal.msgWarning('请选择活动');
    return;
  }
  await proxy?.$modal.confirm('确认按当前活动下所有启用子类别生成/同步上传规则模板？已有同编码模板会被当前类别配置更新。');
  const res: any = await syncUploadTemplatesFromActivity(categoryTemplateForm.value.activityId);
  const count = Array.isArray(res.data) ? res.data.length : 0;
  proxy?.$modal.msgSuccess(`已生成/同步 ${count} 个子类别模板`);
  categoryTemplateDialog.visible = false;
  await getList();
};

const addField = (fieldLabel = '', fieldType = 'input', required = false, optionsText = '') => {
  fieldRows.value.push({
    fieldLabel,
    fieldType,
    required,
    sensitiveFlag: false,
    validationType: inferValidationType(fieldLabel, fieldType),
    optionsList: optionsText ? optionsText.split(/[,，]/).map((item) => item.trim()).filter(Boolean) : [],
    sortOrder: fieldRows.value.length + 1
  });
};

const completeShowcaseFields = async (reset = false) => {
  if (reset) {
    await proxy?.$modal.confirm('确认重置申报字段为展演标准字段？现有申报字段会被替换。');
    fieldRows.value = [];
  }
  showcaseStandardFields.forEach((preset) => upsertFieldPreset(preset));
  canonicalForm.groupName = 'groupName';
  canonicalForm.projectType = 'projectType';
  canonicalForm.programForm = 'programForm';
  canonicalForm.performanceOrder = 'performanceOrder';
  canonicalForm.originalityCommitment = 'originalityCommitment';
  ruleForm.value.groupEnabled = true;
  ruleForm.value.requiresOriginality = true;
  proxy?.$modal.msgSuccess(reset ? '已重置为展演标准字段' : '已补齐展演标准字段');
};

const upsertFieldPreset = (preset: any) => {
  const existing = fieldRows.value.find((item) => item.fieldKey === preset.fieldKey || item.fieldLabel === preset.fieldLabel);
  const payload = {
    fieldKey: preset.fieldKey,
    fieldLabel: preset.fieldLabel,
    fieldType: preset.fieldType,
    required: preset.required,
    sensitiveFlag: false,
    validationType: inferValidationType(preset.fieldLabel, preset.fieldType),
    optionsList: preset.optionsText ? splitOptions(preset.optionsText) : [],
    sortOrder: existing?.sortOrder || fieldRows.value.length + 1
  };
  if (existing) {
    Object.assign(existing, payload);
  } else {
    fieldRows.value.push(payload);
  }
};

const addMemberField = (
  group: 'author' | 'participant',
  fieldLabel = '',
  fieldKey = '',
  fieldType = 'input',
  required = false,
  optionsText = ''
) => {
  memberFieldRows[group].push({
    fieldLabel,
    fieldKey,
    fieldType,
    required,
    optionsList: optionsText ? splitOptions(optionsText) : [],
    sortOrder: memberFieldRows[group].length + 1
  });
};

const addFile = (fileTypeName = '', fileTypeCode = '', allowedExt = '所有格式', maxSizeMb = 100, minCount = 0, maxCount = 1, required = false, ruleJson = '', tipText = '') => {
  fileRows.value.push({ fileTypeName, fileTypeCode, allowedExt, maxSizeMb, minCount, maxCount, required, ruleJson, tipText, sortOrder: fileRows.value.length + 1 });
};

const parseArray = (value?: string) => {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

const parseObject = (value?: string) => {
  if (!value) return {};
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
};

const makeKey = (label: string, prefix: string) => {
  const known: Record<string, string> = {
    作品名称: 'work_name',
    是否原创: 'is_original',
    原创承诺: 'original_commitment',
    授权承诺: 'authorization_commitment',
    姓名: 'name',
    作者姓名: 'name',
    学号: 'studentNo',
    院系: 'department',
    专业: 'major',
    角色: 'roleName',
    指导教师: 'name'
  };
  return known[label] || `${prefix}_${Date.now()}_${Math.floor(Math.random() * 1000)}`;
};

const toFieldRow = (item: any) => ({
  ...item,
  sensitiveFlag: item.sensitiveFlag || item.sensitive,
  validationType: parseObject(item.validationJson).type || '',
  validationJsonText: formatJsonText(item.validationJson),
  optionsList: parseArray(item.optionsJson).map((option) => String(option))
});
const toFileRow = (item: any) => ({ ...item });
const toMemberFieldRow = (item: any) => ({
  ...item,
  fieldType: item.fieldType || 'input',
  optionsList: Array.isArray(item.optionsList) ? item.optionsList : parseArray(item.optionsJson).map((option) => String(option)),
  sortOrder: item.sortOrder || 0
});
const toFieldPayload = (item: any) => ({
  fieldKey: item.fieldKey || makeKey(item.fieldLabel, 'field'),
  fieldLabel: item.fieldLabel,
  fieldType: item.fieldType,
  required: !!item.required,
  validationJson: buildFieldValidationJson(item),
  optionsJson: optionFieldTypes.includes(item.fieldType) ? JSON.stringify((item.optionsList || []).map((v: string) => v.trim()).filter(Boolean)) : undefined,
  sensitiveFlag: !!item.sensitiveFlag,
  sortOrder: item.sortOrder || 0
});
const toMemberFieldPayload = (item: any) => ({
  fieldKey: item.fieldKey || makeKey(item.fieldLabel, 'member'),
  fieldLabel: item.fieldLabel,
  fieldType: item.fieldType || 'input',
  required: !!item.required,
  optionsJson: optionFieldTypes.includes(item.fieldType) ? JSON.stringify((item.optionsList || []).map((v: string) => v.trim()).filter(Boolean)) : undefined,
  sortOrder: item.sortOrder || 0
});
const toFilePayload = (item: any) => ({
  fileTypeCode: item.fileTypeCode || makeKey(item.fileTypeName, 'file'),
  fileTypeName: item.fileTypeName,
  allowedExt: item.allowedExt === '所有格式' ? '' : item.allowedExt,
  maxSizeMb: item.maxSizeMb,
  minCount: item.minCount || 0,
  maxCount: item.maxCount || 1,
  required: !!item.required,
  tipText: item.tipText,
  ruleJson: normalizeJsonText(item.ruleJson),
  sortOrder: item.sortOrder || 0
});
const normalizeJsonText = (text?: string) => {
  if (!text) return undefined;
  try {
    const parsed = JSON.parse(text);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? JSON.stringify(parsed) : undefined;
  } catch {
    return text;
  }
};
const formatJsonText = (text?: string) => {
  if (!text) return '';
  try {
    const parsed = JSON.parse(text);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? JSON.stringify(parsed) : text;
  } catch {
    return text;
  }
};
const buildFieldValidationJson = (item: any) => {
  const text = String(item.validationJsonText || '').trim();
  let parsed: Record<string, any> = {};
  if (text) {
    try {
      const value = JSON.parse(text);
      if (value && typeof value === 'object' && !Array.isArray(value)) {
        parsed = value;
      } else {
        return text;
      }
    } catch {
      return text;
    }
  }
  if (item.validationType) {
    parsed.type = item.validationType;
  } else {
    delete parsed.type;
  }
  return Object.keys(parsed).length ? JSON.stringify(parsed) : undefined;
};
const normalizeRuleForm = () => {
  if (!ruleForm.value) ruleForm.value = {};
  ensureRuleDefaults();
  if (ruleForm.value.manualCheckTipsText) {
    ruleForm.value.manualCheckTips = String(ruleForm.value.manualCheckTipsText).split(/\r?\n/).map((item: string) => item.trim()).filter(Boolean);
  }
  if (ruleForm.value.rejectReasonTemplatesText) {
    ruleForm.value.rejectReasonTemplates = String(ruleForm.value.rejectReasonTemplatesText).split(/\r?\n/).map((item: string) => item.trim()).filter(Boolean);
  }
};
const ensureRuleDefaults = () => {
  if (!ruleForm.value) ruleForm.value = {};
  ruleForm.value.projectName = {
    label: '项目名称',
    required: false,
    ...(ruleForm.value.projectName || {})
  };
  ruleForm.value.canonicalFieldKeys = {
    ...(ruleForm.value.canonicalFieldKeys || {})
  };
};
const hydrateCanonicalForm = () => {
  const keys = ruleForm.value?.canonicalFieldKeys || {};
  canonicalForm.groupName = firstCanonicalKey(keys.groupName);
  canonicalForm.projectType = firstCanonicalKey(keys.projectType);
  canonicalForm.programForm = firstCanonicalKey(keys.programForm);
  canonicalForm.performanceOrder = firstCanonicalKey(keys.performanceOrder);
  canonicalForm.originalityCommitment = firstCanonicalKey(keys.originalityCommitment);
};
const syncCanonicalToRuleForm = () => {
  const canonicalKeys: Record<string, string[]> = {};
  Object.entries(canonicalForm).forEach(([key, value]) => {
    if (value) canonicalKeys[key] = [value];
  });
  ruleForm.value.canonicalFieldKeys = canonicalKeys;
};
const firstCanonicalKey = (value: any) => {
  if (Array.isArray(value)) return String(value[0] || '');
  return value ? String(value) : '';
};
const hydrateMemberFieldRows = (groups?: any) => {
  memberFieldRows.author.splice(0, memberFieldRows.author.length, ...parseMemberFieldGroup(groups?.author));
  memberFieldRows.participant.splice(0, memberFieldRows.participant.length, ...parseMemberFieldGroup(groups?.participant));
};
const parseMemberFieldGroup = (value?: any) => {
  return Array.isArray(value) ? value.map(toMemberFieldRow) : [];
};
const splitOptions = (text: string) => text.split(/[,，]/).map((item) => item.trim()).filter(Boolean);
const activityLabel = (item: any) => item.edition ? `${item.edition} - ${item.activityName}` : item.activityName;
const fieldOptionKey = (item: any) => {
  if (!item.fieldKey) {
    item.fieldKey = makeKey(item.fieldLabel, 'field');
  }
  return item.fieldKey;
};
const fieldOptionLabel = (item: any) => `${item.fieldLabel || '未命名字段'}（${fieldOptionKey(item)}）`;
const inferValidationType = (label: string, fieldType: string) => {
  if (fieldType === 'number') return 'number';
  if (fieldType === 'date') return 'date';
  if (fieldType === 'phone') return 'phone';
  if (fieldType === 'email') return 'email';
  if (label.includes('身份证')) return 'id_card';
  if (label.includes('手机号') || label.includes('电话')) return 'phone';
  if (label.includes('邮箱')) return 'email';
  if (label.includes('性别')) return 'gender';
  if (label.includes('民族')) return 'nation';
  if (label.includes('状态')) return 'status';
  if (label.includes('文件')) return 'file';
  if (label.includes('备注')) return 'remark';
  if (label.includes('姓名') || label.includes('教师') || label.includes('负责人')) return 'name';
  return '';
};

onMounted(async () => {
  await loadActivities();
  await getList();
});
</script>

<style scoped>
.member-field-title {
  margin: 8px 0;
  font-weight: 600;
  color: #303133;
}

.template-helper-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
</style>
