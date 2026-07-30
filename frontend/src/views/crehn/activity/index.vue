<template>
  <!-- ART-REF: FE.ACTIVITY_CONFIG.SHELL -->
  <div class="p-2">
    <el-row :gutter="10" class="activity-config-layout">
      <el-col :xs="24" :sm="24" :md="24" :lg="11" :xl="11" class="activity-left-col">
        <el-collapse-transition>
          <el-card v-show="activityPanelExpanded" shadow="hover" class="activity-summary-card">
            <template #header>
              <div class="activity-summary-header">
                <span>当前活动</span>
                <div class="activity-summary-actions">
                  <el-button v-if="canManageActivity" type="primary" plain icon="Switch" @click="openActivityManager">管理/切换活动</el-button>
                  <el-dropdown :disabled="!currentActivity?.id || selectedActivityDeleted" @command="exportActivityConfig">
                    <el-button type="warning" plain icon="Download" :disabled="!currentActivity?.id || selectedActivityDeleted">导出配置</el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="json">导出 JSON</el-dropdown-item>
                        <el-dropdown-item command="excel">导出 Excel</el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </div>
            </template>
            <el-descriptions :column="1" size="small" border>
              <el-descriptions-item label="活动名称">{{ currentActivity?.activityName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="菜单简称">{{ currentActivity?.menuName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="报名时间"
                >{{ currentActivity?.signupStartAt || '-' }} 至 {{ currentActivity?.signupEndAt || '-' }}</el-descriptions-item
              >
              <el-descriptions-item label="状态">{{ currentActivity?.status || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-collapse-transition>

        <!-- ART-REF: FE.ACTIVITY_CONFIG.CATEGORY_LIST -> components/CategoryListPanel.vue -->
        <CategoryListPanel
          :category-table-data="categoryTableData"
          :current-activity="currentActivity"
          :current-category="currentCategory"
          :category-deleted="categoryDeleted"
          :selected-activity-deleted="selectedActivityDeleted"
          :activity-panel-expanded="activityPanelExpanded"
          :can-edit-category="canEditCategory"
          :can-import-config="canImportCurrentActivityConfig"
          :config-exporting="currentConfigExporting"
          :config-importing="currentConfigImporting"
          :has-categories="categoryList.length > 0"
          @toggle-activity-panel="activityPanelExpanded = !activityPanelExpanded"
          @toggle-deleted="toggleCategoryDeleted"
          @add-group="openCategory(undefined, CATEGORY_NODE_TYPE_GROUP)"
          @add="openCategory(undefined, CATEGORY_NODE_TYPE_CATEGORY)"
          @copy="copyCurrentCategory"
          @open-menu-style="openMenuStyleDialog"
          @export-config="exportCurrentActivityBackup"
          @import-config="previewCurrentActivityConfigImport"
          @select="selectCategory"
          @toggle-menu-visible="toggleCategoryMenuVisible"
          @toggle-enabled="toggleCategoryEnabled"
          @edit="openCategory"
          @remove="removeCategory"
          @restore="restoreCategoryRow"
          @purge="openCategoryPurgeCheck"
          @move-sort="moveCategorySort"
        />
      </el-col>

      <el-col :xs="24" :sm="24" :md="24" :lg="13" :xl="13" class="activity-right-col">
        <el-card shadow="hover" class="config-detail-card">
          <el-tabs v-if="currentCategory?.id && !currentCategoryIsGroup && !currentCategory?.virtualGroup" v-model="activeTab" class="config-tabs">
            <el-tab-pane label="表单字段" name="field" class="config-tab-pane">
              <!-- ART-REF: FE.ACTIVITY_CONFIG.FIELD_SCHEMA -> components/FieldSchemaTab.vue -->
              <FieldSchemaTab
                :field-list="fieldList"
                :has-current-category="!!currentCategory?.id"
                :field-deleted="fieldDeleted"
                :category-deleted="categoryDeleted"
                :selected-activity-deleted="selectedActivityDeleted"
                :can-edit-category="canEditCategory"
                :can-edit-field-schema="canEditFieldSchema"
                @toggle-deleted="toggleFieldDeleted"
                @add="openField()"
                @open-member-template="openMemberFieldDialog"
                @open-form-layout="openFormLayoutDialog"
                @edit="openField"
                @copy="copyFieldRow"
                @restore="restoreFieldRow"
                @remove="removeField"
                @move-sort="moveFieldSort"
              />
            </el-tab-pane>
            <el-tab-pane label="附件要求" name="file" class="config-tab-pane">
              <!-- ART-REF: FE.ACTIVITY_CONFIG.FILE_REQUIREMENT -> components/FileRequirementTab.vue -->
              <div class="field-toolbar">
                <el-button
                  :disabled="!currentCategory?.id || categoryDeleted"
                  :type="requirementDeleted ? 'warning' : 'default'"
                  plain
                  size="small"
                  @click="toggleRequirementDeleted"
                  >已删附件</el-button
                >
                <el-button
                  v-if="!requirementDeleted"
                  v-hasPermi="['crehn:fileRequirement:add']"
                  :disabled="!currentCategory?.id || selectedActivityDeleted"
                  type="primary"
                  plain
                  icon="Plus"
                  class="toolbar-secondary-action"
                  @click="openRequirement()"
                >
                  新增附件要求
                </el-button>
                <el-dropdown class="toolbar-more-menu" trigger="click" @command="handleRequirementMoreCommand">
                  <el-button plain icon="MoreFilled" />
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item :disabled="!currentCategory?.id || requirementDeleted || selectedActivityDeleted" command="add"
                        >新增附件要求</el-dropdown-item
                      >
                      <el-dropdown-item :disabled="!currentCategory?.id || categoryDeleted" command="deleted">{{
                        requirementDeleted ? '查看正常附件' : '查看已删附件'
                      }}</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <el-table border :data="requirementList" height="100%" class="config-table">
                <el-table-column label="类型" width="130">
                  <template #default="scope">{{ formatFileType(scope.row) }}</template>
                </el-table-column>
                <el-table-column label="名称" prop="fileTypeName" min-width="120" />
                <el-table-column label="格式" width="140" show-overflow-tooltip>
                  <template #default="scope">{{ formatAllowedExt(scope.row.allowedExt) }}</template>
                </el-table-column>
                <el-table-column label="单个大小" width="120">
                  <template #default="scope">{{ formatFileSizeRange(scope.row) }}</template>
                </el-table-column>
                <el-table-column label="数量" width="90">
                  <template #default="scope">{{ formatFileCountRange(scope.row) }}</template>
                </el-table-column>
                <el-table-column label="扩展规则" width="90">
                  <template #default="scope"
                    ><el-tag :type="scope.row.ruleJson ? 'success' : 'info'">{{ scope.row.ruleJson ? '已配置' : '无' }}</el-tag></template
                  >
                </el-table-column>
                <el-table-column label="必传" width="70">
                  <template #default="scope">{{ scope.row.required ? '是' : '否' }}</template>
                </el-table-column>
                <el-table-column label="操作" width="112" fixed="left" align="center" class-name="operation-column">
                  <template #default="scope">
                    <span v-if="selectedActivityDeleted" class="category-purge-muted">只读</span>
                    <el-button
                      v-else-if="scope.row.delFlag === '1' || requirementDeleted"
                      v-hasPermi="['crehn:fileRequirement:edit']"
                      link
                      type="success"
                      icon="RefreshLeft"
                      @click="restoreRequirementRow(scope.row)"
                    />
                    <template v-else>
                      <el-button v-hasPermi="['crehn:fileRequirement:edit']" link type="primary" icon="Edit" @click="openRequirement(scope.row)" />
                      <el-button v-hasPermi="['crehn:fileRequirement:remove']" link type="danger" icon="Delete" @click="removeRequirement(scope.row)" />
                    </template>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
          <el-empty
            v-else
            :description="
              currentCategoryIsGroup
                ? '大类只管理层级、排序、菜单显示和业务启用，不配置表单字段或附件要求'
                : '请选择一个报送类别配置表单字段和附件要求'
            "
          />
        </el-card>
      </el-col>
    </el-row>

    <ArtPopupDialog
      v-if="canManageActivity"
      v-model="activityManagerVisible"
      title="活动管理与切换"
      width="min(1100px, calc(100vw - 32px))"
      append-to-body
      destroy-on-close
    >
      <div class="activity-manager-dialog">
        <el-form v-show="showSearch" :model="queryParams" inline class="activity-manager-query">
          <el-form-item label="活动名称">
            <el-input v-model="queryParams.activityName" clearable placeholder="请输入活动名称" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" clearable placeholder="全部状态">
              <el-option label="草稿" value="draft" />
              <el-option label="启用" value="enabled" />
              <el-option label="暂停" value="paused" />
              <el-option label="结束" value="ended" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <ActivityListPanel
          :loading="loading"
          :total="total"
          :query-params="queryParams"
          :show-search="showSearch"
          :activity-list="activityList"
          :current-activity="currentActivity"
          :activity-deleted="activityDeleted"
          :selected-activity-deleted="selectedActivityDeleted"
          :importing="activityConfigImporting"
          @update:show-search="showSearch = $event"
          @update:page="queryParams.pageNum = $event"
          @update:limit="queryParams.pageSize = $event"
          @add="openActivity()"
          @import="handleActivityConfigImport"
          @export="exportActivityConfig"
          @toggle-deleted="toggleActivityDeleted"
          @refresh="getActivityList"
          @select="switchManagedActivity"
          @edit="openActivity"
          @remove="removeActivity"
          @restore="restoreActivityRow"
          @purge="openActivityPurgeCheck"
          @paginate="getActivityList"
        />
      </div>
      <template #footer>
        <el-button @click="activityManagerVisible = false">关闭</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog
      v-model="configMergeDialog.visible"
      title="导入当前活动配置"
      width="min(980px, calc(100vw - 32px))"
      append-to-body
      destroy-on-close
    >
      <div v-if="configMergePreview" class="config-merge-preview">
        <el-alert
          :closable="false"
          type="warning"
          show-icon
          :title="`将“${configMergePreview.sourceActivityName || '配置包'}”增量导入“${configMergePreview.activityName || '当前活动'}”`"
          description="已有的类别编码、同类别字段编码和附件类型编码会跳过；下列新增项只有点击“确认新增”后才会写入。"
        />
        <div class="config-merge-summary">
          <el-tag type="success">新增类别 {{ configMergePreview.addedCategoryCount || 0 }}</el-tag>
          <el-tag type="success">新增字段 {{ configMergePreview.addedFieldCount || 0 }}</el-tag>
          <el-tag type="success">新增附件要求 {{ configMergePreview.addedFileRequirementCount || 0 }}</el-tag>
          <el-tag type="info">跳过 {{ configMergeSkippedTotal }} 项</el-tag>
        </div>
        <el-tabs v-model="configMergeTab">
          <el-tab-pane :label="`待新增 (${configMergeAdditionTotal})`" name="additions">
            <el-empty v-if="!configMergeAdditionTotal" description="没有可新增配置，配置包内容均已存在" :image-size="72" />
            <el-table v-else border :data="configMergePreview.additions || []" max-height="360">
              <el-table-column label="类型" prop="itemTypeLabel" width="92" />
              <el-table-column label="所属类别" min-width="160">
                <template #default="{ row }">{{ row.categoryName || '-' }}（{{ row.categoryCode || '-' }}）</template>
              </el-table-column>
              <el-table-column label="编码" prop="itemCode" min-width="130" show-overflow-tooltip />
              <el-table-column label="名称" prop="itemName" min-width="130" show-overflow-tooltip />
              <el-table-column label="说明" prop="reason" min-width="190" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`已跳过 (${configMergeSkippedTotal})`" name="skipped">
            <el-empty v-if="!configMergeSkippedTotal" description="没有重复配置" :image-size="72" />
            <el-table v-else border :data="configMergePreview.skipped || []" max-height="360">
              <el-table-column label="类型" prop="itemTypeLabel" width="92" />
              <el-table-column label="所属类别" min-width="160">
                <template #default="{ row }">{{ row.categoryName || '-' }}（{{ row.categoryCode || '-' }}）</template>
              </el-table-column>
              <el-table-column label="编码" prop="itemCode" min-width="130" show-overflow-tooltip />
              <el-table-column label="名称" prop="itemName" min-width="130" show-overflow-tooltip />
              <el-table-column label="跳过原因" prop="reason" min-width="220" show-overflow-tooltip />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
      <template #footer>
        <el-button @click="configMergeDialog.visible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!configMergeAdditionTotal"
          :loading="configMergeDialog.submitting"
          @click="confirmCurrentActivityConfigImport"
        >
          确认新增 {{ configMergeAdditionTotal }} 项
        </el-button>
      </template>
    </ArtPopupDialog>

    <!-- ART-REF: FE.ACTIVITY_CONFIG.ACTIVITY_LIST -> components/ActivityListPanel.vue -->
    <ArtPopupDialog v-model="activityDialog.visible" :title="activityDialog.title" width="760px" append-to-body>
      <el-form :model="activityForm" label-width="100px">
        <el-row :gutter="10">
          <el-col :span="12"
            ><el-form-item label="活动名称"><el-input v-model="activityForm.activityName" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="菜单简称"
              ><el-input
                v-model="activityForm.menuName"
                maxlength="32"
                show-word-limit
                placeholder="学校端左侧菜单显示，留空使用活动名称" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="届次"><el-input v-model="activityForm.edition" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="年度"><el-input-number v-model="activityForm.year" class="w-full" /></el-form-item
          ></el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="activityForm.status" class="w-full">
                <el-option label="草稿" value="draft" />
                <el-option label="启用" value="enabled" />
                <el-option label="暂停" value="paused" />
                <el-option label="结束" value="ended" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"
            ><el-form-item label="开始时间"
              ><el-date-picker v-model="activityForm.signupStartAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="结束时间"
              ><el-date-picker v-model="activityForm.signupEndAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item
          ></el-col>
          <el-col :span="24"
            ><el-form-item label="说明"><el-input v-model="activityForm.description" type="textarea" :rows="3" /></el-form-item
          ></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitActivity">确定</el-button>
        <el-button @click="activityDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <!-- ART-REF: FE.ACTIVITY_CONFIG.CATEGORY_RULE -> components/CategoryRuleDialog.vue -->
    <CategoryRuleDialog
      v-model:visible="categoryDialog.visible"
      :title="categoryDialog.title"
      :category-form="categoryForm"
      :node-type="categoryDialogNodeType"
      :menu-visible="categoryDialogMenuVisible"
      :node-type-locked="categoryDialogTypeLocked"
      :category-parent-key="categoryParentKey"
      :category-rule-form="categoryRuleForm"
      :workshop-rule-settings="workshopRuleSettings"
      :category-group-nodes="categoryGroupNodes"
      :people-field-options="categoryDialogPeopleFieldOptions"
      :category-rule-template-options="categoryRuleTemplateOptions"
      :validator-type-options="validatorTypeOptions"
      :group-option-key="categoryGroupOptionKey"
      :group-option-label="categoryGroupOptionLabel"
      :field-option-label="fieldOptionLabel"
      :can-delete="categoryDialogCanDelete"
      :can-restore="categoryDialogCanRestore"
      :can-purge="categoryDialogCanPurge"
      @template-change="handleCategoryRuleTemplateChange"
      @validator-change="handleCategoryRuleValidatorChange"
      @confirm="handleCategoryRuleDialogConfirm"
      @delete="handleCategoryRuleDialogDelete"
      @restore="handleCategoryRuleDialogRestore"
      @purge="handleCategoryRuleDialogPurge"
    />

    <!-- ART-REF: FE.ACTIVITY_CONFIG.MENU_STYLE -->
    <MenuStyleDialog
      ref="menuStyleDialogRef"
      :category-list="categoryList"
      :category-group-nodes="categoryGroupNodes"
      :save-categories="saveMenuStyleCategories"
    />

    <!-- ART-REF: FE.ACTIVITY_CONFIG.FIELD_SCHEMA -> components/FieldSchemaTab.vue -->
    <ArtPopupDialog v-model="fieldDialog.visible" :title="fieldDialog.title" width="760px" append-to-body>
      <el-form :model="fieldForm" label-width="110px">
        <el-form-item label="常用模板">
          <el-select
            v-model="selectedFieldTemplate"
            filterable
            clearable
            class="w-full"
            placeholder="选择常用字段或使用自定义字段"
            @change="applyFieldTemplate"
          >
            <el-option label="自定义字段" value="custom" />
            <el-option v-for="item in fieldTemplates" :key="item.fieldKey" :label="item.fieldLabel" :value="item.fieldKey" />
          </el-select>
        </el-form-item>
        <el-form-item label="field_key">
          <el-input v-model="fieldForm.fieldKey" readonly placeholder="系统自动生成英文键" />
        </el-form-item>
        <el-form-item label="字段名称"
          ><el-input v-model="fieldForm.fieldLabel" placeholder="请输入中文显示名称" @blur="ensureCustomFieldKey"
        /></el-form-item>
        <el-form-item label="字段类型">
          <el-select v-model="fieldForm.fieldType" class="w-full" @change="handleFieldTypeChange">
            <el-option label="单行文本" value="input" />
            <el-option label="多行文本" value="textarea" />
            <el-option label="下拉选择" value="select" />
            <el-option label="单选" value="radio" />
            <el-option label="多选" value="checkbox" />
            <el-option label="日期" value="date" />
            <el-option label="时分秒" value="duration" />
            <el-option label="长宽高尺寸" value="dimension_group" />
            <el-option label="可重复人员组" value="teacher_group" />
            <el-option label="数字" value="number" />
            <el-option label="身份证号" value="id_card" />
          </el-select>
        </el-form-item>
        <el-form-item label="字段校验">
          <el-select v-model="fieldValidationType" class="w-full" clearable placeholder="不校验">
            <el-option v-for="item in validationOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-divider content-position="left">显示设置</el-divider>
        <el-form-item label="框内提示"><el-input v-model="fieldUiSettings.placeholder" placeholder="如：请输入作品名称" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="输入框宽度">
              <el-select v-model="fieldUiSettings.inputWidthMode" class="w-full">
                <el-option v-for="item in inputWidthModeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="fieldUiSettings.inputWidthMode === 'custom'" :span="12">
            <el-form-item label="自定义宽度">
              <el-input v-model="fieldUiSettings.inputWidthCustom" placeholder="如：360px 或 50%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="框内颜色">
              <div class="flex items-center gap-2">
                <el-color-picker v-model="fieldUiSettings.placeholderColor" show-alpha :clearable="false" />
                <el-button link type="primary" @click="restoreDefaultFieldColor('placeholderColor')">默认</el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="框内字号">
              <el-input-number v-model="fieldUiSettings.placeholderFontSize" class="w-full" :min="10" :max="28" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="后缀文字"><el-input v-model="fieldUiSettings.suffixText" placeholder="如：人 / 分钟 / 秒 / 件" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="后缀颜色">
              <div class="flex items-center gap-2">
                <el-color-picker v-model="fieldUiSettings.suffixColor" show-alpha :clearable="false" />
                <el-button link type="primary" @click="restoreDefaultFieldColor('suffixColor')">默认</el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="后缀字号">
              <el-input-number v-model="fieldUiSettings.suffixFontSize" class="w-full" :min="10" :max="28" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="下方提示词">
          <el-input v-model="fieldUiSettings.helpText" type="textarea" :rows="2" placeholder="如：尺寸不超过50cm（长）×50cm（宽）×50cm（高）" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="下方颜色">
              <div class="flex items-center gap-2">
                <el-color-picker v-model="fieldUiSettings.helpColor" show-alpha :clearable="false" />
                <el-button link type="primary" @click="restoreDefaultFieldColor('helpColor')">默认</el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="下方字号">
              <el-input-number v-model="fieldUiSettings.helpFontSize" class="w-full" :min="10" :max="28" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
        <template v-if="isCharacterLimitField || isNumberLimitField">
          <el-divider content-position="left">输入限制</el-divider>
          <el-row v-if="isCharacterLimitField" :gutter="12">
            <el-col :span="8">
              <el-form-item label="最少字符">
                <el-input-number v-model="fieldUiSettings.minLength" class="w-full" :min="0" :precision="0" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="最多字符">
                <el-input-number v-model="fieldUiSettings.maxLength" class="w-full" :min="0" :precision="0" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="显示字数">
                <el-switch v-model="fieldUiSettings.showWordLimit" />
              </el-form-item>
            </el-col>
          </el-row>
          <template v-if="isNumberLimitField">
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="最小值">
                  <el-input-number v-model="fieldUiSettings.numberMin" class="w-full" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="最大值">
                  <el-input-number v-model="fieldUiSettings.numberMax" class="w-full" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="整数">
                  <el-switch v-model="fieldUiSettings.integerOnly" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="小数位">
                  <el-input-number
                    v-model="fieldUiSettings.precision"
                    class="w-full"
                    :min="0"
                    :max="6"
                    :precision="0"
                    :disabled="fieldUiSettings.integerOnly"
                    controls-position="right"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="步进">
                  <el-input-number v-model="fieldUiSettings.step" class="w-full" :min="0" controls-position="right" />
                </el-form-item>
              </el-col>
            </el-row>
          </template>
        </template>
        <template v-if="isKeywordCountField">
          <el-divider content-position="left">关键词数量</el-divider>
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="最少关键词">
                <el-input-number v-model="fieldUiSettings.minKeywordCount" class="w-full" :min="0" :precision="0" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="最多关键词">
                <el-input-number v-model="fieldUiSettings.maxKeywordCount" class="w-full" :min="0" :precision="0" controls-position="right" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
        <el-form-item v-if="isDurationField" label="时长格式">
          <el-select v-model="durationSettings.durationMode" class="w-full">
            <el-option label="时:分:秒" value="hms" />
            <el-option label="分:秒" value="ms" />
            <el-option label="分钟" value="minute" />
            <el-option label="秒" value="second" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isOptionField" label="选项">
          <div class="w-full">
            <div v-for="(_, index) in fieldOptions" :key="index" class="mb-2 flex items-center gap-2">
              <el-input v-model="fieldOptions[index]" placeholder="选项名称" />
              <el-button icon="Minus" circle @click="removeFieldOption(index)" />
            </div>
            <el-button type="primary" plain icon="Plus" @click="addFieldOption">添加选项</el-button>
          </div>
        </el-form-item>
        <el-form-item v-if="isRadioField" label="多级选项">
          <div class="sub-field-panel">
            <el-table border :data="cascadeOptionRows" size="small">
              <el-table-column label="一级" min-width="130">
                <template #default="scope"><el-input v-model="scope.row.level1" placeholder="如：独唱" /></template>
              </el-table-column>
              <el-table-column label="二级选项" min-width="190">
                <template #default="scope"><el-input v-model="scope.row.level2" placeholder="如：美声，民族，流行" /></template>
              </el-table-column>
              <el-table-column label="三级选项" min-width="170">
                <template #default="scope"><el-input v-model="scope.row.level3" placeholder="可选，逗号分隔" /></template>
              </el-table-column>
              <el-table-column label="操作" width="72" fixed="left" align="center" class-name="operation-column">
                <template #default="scope"
                  ><el-button link type="danger" icon="Delete" @click="cascadeOptionRows.splice(scope.$index, 1)"
                /></template>
              </el-table-column>
            </el-table>
            <div class="mt-2 flex items-center gap-2">
              <el-button type="primary" plain icon="Plus" @click="addCascadeOptionRow">添加多级选项</el-button>
              <el-button plain icon="MagicStick" @click="fillPersonalProgramCascadeOptions">填充个人项目选项</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item v-if="isDimensionGroupField" label="尺寸输入项">
          <div class="sub-field-panel">
            <el-table border :data="dimensionRows" size="small">
              <el-table-column label="显示名" min-width="110">
                <template #default="scope"><el-input v-model="scope.row.label" placeholder="如：长" /></template>
              </el-table-column>
              <el-table-column label="field_key" width="110">
                <template #default="scope"><el-input v-model="scope.row.key" disabled /></template>
              </el-table-column>
              <el-table-column label="框内提示" min-width="130">
                <template #default="scope"><el-input v-model="scope.row.placeholder" placeholder="如：长" /></template>
              </el-table-column>
              <el-table-column label="单位" width="110">
                <template #default="scope"><el-input v-model="scope.row.unit" placeholder="cm" /></template>
              </el-table-column>
              <el-table-column label="必填" width="70">
                <template #default="scope"><el-switch v-model="scope.row.required" /></template>
              </el-table-column>
            </el-table>
          </div>
        </el-form-item>
        <el-row v-if="isTeacherGroupField" :gutter="12">
          <el-col :span="12">
            <el-form-item label="最少人数">
              <el-input-number v-model="teacherLimitSettings.minItems" class="w-full" :min="0" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最多人数">
              <el-input-number v-model="teacherLimitSettings.maxItems" class="w-full" :min="1" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="isTeacherGroupField" label="业务角色">
          <el-select v-model="teacherMemberRole" clearable class="w-full" placeholder="选择人数统计角色">
            <el-option label="作者" value="author" />
            <el-option label="指导教师" value="teacher" />
            <el-option label="学生" value="student" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isTeacherGroupField" label="细分字段">
          <div class="sub-field-panel">
            <el-table border :data="teacherSubFields" size="small">
              <el-table-column label="字段名称" min-width="130">
                <template #default="scope"><el-input v-model="scope.row.fieldLabel" placeholder="如：手机号" /></template>
              </el-table-column>
              <el-table-column label="field_key" min-width="130">
                <template #default="scope"
                  ><el-input v-model="scope.row.fieldKey" placeholder="如：phone" @blur="ensureSubFieldKey(scope.row, 'teacher')"
                /></template>
              </el-table-column>
              <el-table-column label="类型" width="120">
                <template #default="scope">
                  <el-select v-model="scope.row.fieldType" class="w-full">
                    <el-option label="单行文本" value="input" />
                    <el-option label="多行文本" value="textarea" />
                    <el-option label="下拉选择" value="select" />
                    <el-option label="单选" value="radio" />
                    <el-option label="多选" value="checkbox" />
                    <el-option label="日期" value="date" />
                    <el-option label="数字" value="number" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="必填" width="70">
                <template #default="scope"><el-switch v-model="scope.row.required" /></template>
              </el-table-column>
              <el-table-column label="排序" width="95">
                <template #default="scope"><el-input-number v-model="scope.row.sortOrder" :min="0" controls-position="right" /></template>
              </el-table-column>
              <el-table-column label="操作" width="72" fixed="left" align="center" class-name="operation-column">
                <template #default="scope"><el-button link type="danger" icon="Delete" @click="teacherSubFields.splice(scope.$index, 1)" /></template>
              </el-table-column>
            </el-table>
            <el-button class="mt-2" type="primary" plain icon="Plus" @click="addTeacherSubField">添加细分字段</el-button>
          </div>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="fieldForm.sortOrder" class="w-full" /></el-form-item>
        <el-form-item label="必填"><el-switch v-model="fieldForm.required" /></el-form-item>
        <el-form-item label="列表脱敏"><el-switch v-model="fieldForm.sensitive" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitField">确定</el-button>
        <el-button @click="fieldDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="formLayoutDialog.visible" title="表单布局配置" width="820px" append-to-body>
      <el-alert
        class="mb-3"
        type="info"
        :closable="false"
        title="布局只影响学校填报页显示，不改变字段排序、校验和提交数据。连续两个半行字段自动组成一行，无法配对时自动按整行显示。"
      />
      <el-form label-width="96px">
        <el-form-item label="布局模式">
          <el-radio-group v-model="formLayoutSettings.mode">
            <el-radio-button value="single">单列</el-radio-button>
            <el-radio-button value="double">自动双列</el-radio-button>
            <el-radio-button value="custom">自定义</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <el-alert class="mb-3" type="success" :closable="false" :title="formLayoutModeTip" />
      <el-table v-if="formLayoutSettings.mode === 'custom' && formLayoutRows.length" border :data="formLayoutRows" max-height="430">
        <el-table-column label="排序" prop="sortOrder" width="72" align="center" />
        <el-table-column label="字段名称" prop="fieldLabel" min-width="170" show-overflow-tooltip />
        <el-table-column label="类型" prop="fieldType" width="120" />
        <el-table-column label="自动建议" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="autoFormLayoutRowSpan(row) === 12 ? 'success' : 'info'">{{ autoFormLayoutRowSpan(row) === 12 ? '半行' : '整行' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="字段布局" width="180">
          <template #default="{ row }">
            <el-select v-model="formLayoutSettings.fieldSpans[row.layoutKey]" class="w-full">
              <el-option label="自动（按字段类型）" :value="0" />
              <el-option label="半行（双列）" :value="12" />
              <el-option label="整行（单列）" :value="24" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else-if="formLayoutSettings.mode === 'custom'" description="当前类别没有可配置的表单字段" />
      <template #footer>
        <el-button type="primary" :loading="formLayoutDialog.saving" @click="submitFormLayout">保存</el-button>
        <el-button @click="formLayoutDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <!-- ART-REF: FE.ACTIVITY_CONFIG.GENERIC_TABLE_TEMPLATE -> components/GenericTableTemplateEditor.vue -->
    <ArtPopupDialog v-model="memberFieldDialog.visible" title="表格模板配置" width="1180px" append-to-body destroy-on-close>
      <el-tabs v-model="activeTableConfigMode">
        <el-tab-pane label="系统成员表" name="member">
          <el-alert
            class="mb-3"
            type="info"
            :closable="false"
            title="原成员表保持兼容：作者组适用于成员类型为“作者”的行，学生/指导教师组适用于其他成员类型。"
          />
          <el-form label-width="130px">
            <el-form-item label="启用成员信息">
              <el-switch v-model="memberTableSettings.enabled" />
              <span class="form-item-tip">关闭后学校端不显示成员信息卡片，也不要求下载或导入成员表。</span>
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="12"
                ><el-form-item label="表格标题"><el-input v-model="memberTableTitle" maxlength="30" placeholder="默认：成员信息" /></el-form-item
              ></el-col>
              <el-col :span="12"
                ><el-form-item label="提示标签"><el-input v-model="memberTipLabel" maxlength="20" placeholder="默认：备注" /></el-form-item
              ></el-col>
            </el-row>
            <el-divider content-position="left">成员类型</el-divider>
            <el-table border :data="memberTypeOptions" size="small">
              <el-table-column label="显示名称" min-width="150"
                ><template #default="scope"><el-input v-model="scope.row.label" maxlength="30" /></template
              ></el-table-column>
              <el-table-column label="数据编码" min-width="150"
                ><template #default="scope"
                  ><el-input
                    v-model="scope.row.code"
                    :disabled="scope.row.builtin"
                    maxlength="32"
                    @blur="ensureMemberTypeCode(scope.row)" /></template
              ></el-table-column>
              <el-table-column label="适用字段" width="140"
                ><template #default="scope"
                  ><el-select v-model="scope.row.group" class="w-full"
                    ><el-option label="作者字段" value="author" /><el-option label="成员字段" value="participant" /></el-select></template
              ></el-table-column>
              <el-table-column label="启用" width="76" align="center"
                ><template #default="scope"><el-switch v-model="scope.row.enabled" /></template
              ></el-table-column>
              <el-table-column label="默认" width="76" align="center"
                ><template #default="scope"
                  ><el-radio
                    :model-value="memberDefaultTypeCode"
                    :label="scope.row.code"
                    :disabled="!scope.row.enabled"
                    @change="setMemberDefaultType(scope.row.code)"
                    ><span class="sr-only">默认</span></el-radio
                  ></template
                ></el-table-column
              >
              <el-table-column label="操作" width="72" fixed="right" align="center"
                ><template #default="scope"
                  ><el-button link type="danger" :disabled="scope.row.builtin" @click="removeMemberType(scope.$index)">删除</el-button></template
                ></el-table-column
              >
            </el-table>
            <el-button class="mt-2" type="primary" plain icon="Plus" @click="addMemberType">添加类型</el-button>
            <el-divider content-position="left">成员数量校验</el-divider>
            <el-row :gutter="12">
              <el-col v-for="item in memberCountRuleRows" :key="item.key" :span="12">
                <el-form-item :label="item.label">
                  <div class="count-rule-row">
                    <el-input-number v-model="memberTableSettings[item.key].min" class="count-rule-input" :min="0" placeholder="最少" />
                    <span class="count-rule-separator">-</span>
                    <el-input-number v-model="memberTableSettings[item.key].max" class="count-rule-input" :min="0" placeholder="最多" />
                    <span>人</span>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="成员信息备注">
              <el-input v-model="memberTipText" type="textarea" :rows="2" placeholder="显示在学校端成员信息按钮后方；不填则使用默认提示" />
            </el-form-item>
            <el-form-item label="采集表底部说明">
              <el-input
                v-model="memberTemplateFooterText"
                type="textarea"
                :rows="5"
                placeholder="显示在下载的师生信息采集表底部；不填则不生成底部说明"
              />
            </el-form-item>
          </el-form>
          <el-tabs v-model="activeMemberFieldGroup">
            <el-tab-pane label="作者字段" name="author">
              <el-table border :data="memberSubFields.author" size="small">
                <el-table-column label="字段名称" min-width="140">
                  <template #default="scope"><el-input v-model="scope.row.fieldLabel" /></template>
                </el-table-column>
                <el-table-column label="field_key" min-width="140">
                  <template #default="scope"><el-input v-model="scope.row.fieldKey" @blur="ensureSubFieldKey(scope.row, 'member')" /></template>
                </el-table-column>
                <el-table-column label="类型" width="130">
                  <template #default="scope"
                    ><el-select v-model="scope.row.fieldType" class="w-full"
                      ><el-option v-for="item in subFieldTypeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select
                  ></template>
                </el-table-column>
                <el-table-column label="校验" width="120">
                  <template #default="scope"
                    ><el-select v-model="scope.row.validationType" class="w-full" clearable
                      ><el-option v-for="item in validationOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select
                  ></template>
                </el-table-column>
                <el-table-column label="选项" min-width="150">
                  <template #default="scope"
                    ><el-input v-model="scope.row.optionsText" :disabled="!optionFieldTypes.includes(scope.row.fieldType)" placeholder="逗号分隔"
                  /></template>
                </el-table-column>
                <el-table-column label="文件名匹配字段" min-width="160">
                  <template #default="scope"
                    ><el-select v-model="scope.row.matchFieldKey" :disabled="!isMemberAttachmentField(scope.row)" clearable placeholder="默认姓名"
                      ><el-option
                        v-for="item in memberMatchFieldOptions('author')"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value" /></el-select
                  ></template>
                </el-table-column>
                <el-table-column label="附件格式" min-width="145">
                  <template #default="scope"
                    ><el-input
                      v-model="scope.row.attachmentAllowedExts"
                      :disabled="scope.row.fieldType !== 'attachment_upload'"
                      placeholder="jpg,pdf,docx"
                  /></template>
                </el-table-column>
                <el-table-column label="必填" width="70">
                  <template #default="scope"><el-switch v-model="scope.row.required" /></template>
                </el-table-column>
                <el-table-column label="排序" width="95">
                  <template #default="scope"><el-input-number v-model="scope.row.sortOrder" :min="0" controls-position="right" /></template>
                </el-table-column>
                <el-table-column label="操作" width="72" fixed="left" align="center" class-name="operation-column">
                  <template #default="scope"
                    ><el-button link type="danger" icon="Delete" @click="removeMemberSubField('author', scope.$index)"
                  /></template>
                </el-table-column>
              </el-table>
              <el-button class="mt-2" type="primary" plain icon="Plus" @click="addMemberSubField('author')">添加作者字段</el-button>
            </el-tab-pane>
            <el-tab-pane label="学生/指导教师字段" name="participant">
              <el-table border :data="memberSubFields.participant" size="small">
                <el-table-column label="字段名称" min-width="140">
                  <template #default="scope"><el-input v-model="scope.row.fieldLabel" /></template>
                </el-table-column>
                <el-table-column label="field_key" min-width="140">
                  <template #default="scope"><el-input v-model="scope.row.fieldKey" @blur="ensureSubFieldKey(scope.row, 'member')" /></template>
                </el-table-column>
                <el-table-column label="类型" width="130">
                  <template #default="scope"
                    ><el-select v-model="scope.row.fieldType" class="w-full"
                      ><el-option v-for="item in subFieldTypeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select
                  ></template>
                </el-table-column>
                <el-table-column label="校验" width="120">
                  <template #default="scope"
                    ><el-select v-model="scope.row.validationType" class="w-full" clearable
                      ><el-option v-for="item in validationOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select
                  ></template>
                </el-table-column>
                <el-table-column label="选项" min-width="150">
                  <template #default="scope"
                    ><el-input v-model="scope.row.optionsText" :disabled="!optionFieldTypes.includes(scope.row.fieldType)" placeholder="逗号分隔"
                  /></template>
                </el-table-column>
                <el-table-column label="文件名匹配字段" min-width="160">
                  <template #default="scope"
                    ><el-select v-model="scope.row.matchFieldKey" :disabled="!isMemberAttachmentField(scope.row)" clearable placeholder="默认姓名"
                      ><el-option
                        v-for="item in memberMatchFieldOptions('participant')"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value" /></el-select
                  ></template>
                </el-table-column>
                <el-table-column label="附件格式" min-width="145">
                  <template #default="scope"
                    ><el-input
                      v-model="scope.row.attachmentAllowedExts"
                      :disabled="scope.row.fieldType !== 'attachment_upload'"
                      placeholder="jpg,pdf,docx"
                  /></template>
                </el-table-column>
                <el-table-column label="必填" width="70">
                  <template #default="scope"><el-switch v-model="scope.row.required" /></template>
                </el-table-column>
                <el-table-column label="排序" width="95">
                  <template #default="scope"><el-input-number v-model="scope.row.sortOrder" :min="0" controls-position="right" /></template>
                </el-table-column>
                <el-table-column label="操作" width="72" fixed="left" align="center" class-name="operation-column">
                  <template #default="scope"
                    ><el-button link type="danger" icon="Delete" @click="removeMemberSubField('participant', scope.$index)"
                  /></template>
                </el-table-column>
              </el-table>
              <el-button class="mt-2" type="primary" plain icon="Plus" @click="addMemberSubField('participant')">添加学生/指导教师字段</el-button>
            </el-tab-pane>
          </el-tabs>
        </el-tab-pane>
        <el-tab-pane label="通用表格模板" name="generic">
          <GenericTableTemplateEditor v-model="genericTableTemplates" />
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button type="primary" :loading="memberFieldDialog.saving" @click="submitMemberFields">保存全部配置</el-button>
        <el-button @click="memberFieldDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <!-- ART-REF: FE.ACTIVITY_CONFIG.FILE_REQUIREMENT -> components/FileRequirementTab.vue -->
    <ArtPopupDialog v-model="requirementDialog.visible" :title="requirementDialog.title" width="760px" append-to-body>
      <el-form :model="requirementForm" label-width="110px">
        <el-form-item label="附件类型">
          <el-select
            v-model="requirementForm.fileTypeCode"
            class="w-full"
            clearable
            filterable
            allow-create
            default-first-option
            placeholder="不选默认所有类型"
            @change="applyRequirementType"
          >
            <el-option v-for="item in fileTypeOptions" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型名称"><el-input v-model="requirementForm.fileTypeName" placeholder="选填，不填按类型自动生成" /></el-form-item>
        <el-form-item label="上传提示语">
          <el-input v-model="requirementForm.tipText" type="textarea" :rows="3" placeholder="填写后学校端上传作品时显示；不填则不显示" />
        </el-form-item>
        <el-divider content-position="left">学校端下载模板</el-divider>
        <el-form-item label="启用模板">
          <el-switch v-model="requirementTemplateSettings.enabled" />
        </el-form-item>
        <template v-if="requirementTemplateSettings.enabled">
          <el-form-item label="下载按钮文字">
            <el-input v-model="requirementTemplateSettings.name" placeholder="例如：下载盖章表模板" />
          </el-form-item>
          <el-form-item label="模板文件">
            <div>
              <el-alert
                v-if="requirementTemplateSettings.ossId && !requirementTemplateSettings.fileName"
                class="mb-2"
                type="warning"
                :closable="false"
                title="历史模板需重新上传一次，才能启用学校端安全下载。"
              />
              <el-upload
                :accept="requirementTemplateAccept"
                :before-upload="beforeRequirementTemplateUpload"
                :http-request="uploadRequirementTemplate"
                :show-file-list="false"
              >
                <el-button type="primary" plain icon="Upload">上传模板文件</el-button>
              </el-upload>
              <div v-if="requirementTemplateSettings.fileName" class="mt-2 flex items-center gap-2">
                <el-tag type="success" effect="plain" class="max-w-[420px] truncate">{{ requirementTemplateSettings.fileName }}</el-tag>
                <el-button link type="danger" @click="clearRequirementTemplate">移除</el-button>
              </div>
              <div class="el-upload__tip">仅允许 xls、xlsx、doc、docx、pdf，单个文件不超过 50MB。</div>
            </div>
          </el-form-item>
        </template>
        <el-divider content-position="left">上传确认项</el-divider>
        <el-form-item label="启用确认">
          <el-switch v-model="uploadConfirmationSettings.enabled" />
        </el-form-item>
        <template v-if="uploadConfirmationSettings.enabled">
          <el-form-item label="确认文案">
            <el-input v-model="uploadConfirmationSettings.text" type="textarea" :rows="3" placeholder="请填写学校端上传作品时需要勾选的确认文案" />
          </el-form-item>
          <el-form-item label="文案颜色">
            <el-color-picker v-model="uploadConfirmationSettings.textColor" show-alpha />
          </el-form-item>
        </template>
        <el-form-item label="格式限制">
          <el-radio-group v-model="allFormatsAllowed">
            <el-radio :label="true">所有格式</el-radio>
            <el-radio :label="false">指定格式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!allFormatsAllowed" label="允许格式">
          <el-select
            v-model="customAllowedExts"
            class="w-full"
            multiple
            clearable
            filterable
            allow-create
            default-first-option
            placeholder="可选择或输入 pdf、docx、psd 等自定义扩展名"
          >
            <el-option v-for="item in allowedExtOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"
            ><el-form-item label="最小大小 MB"
              ><el-input-number
                v-model="requirementRuleSettings.minMb"
                class="w-full"
                :min="0"
                :precision="2"
                placeholder="不填不限制" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="最大大小 MB"
              ><el-input-number v-model="requirementForm.maxSizeMb" class="w-full" :min="0" :precision="0" placeholder="不填不限制" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="最少数量"
              ><el-input-number v-model="requirementForm.minCount" class="w-full" :min="0" :precision="0" placeholder="不填不限制" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="最多数量"
              ><el-input-number v-model="requirementForm.maxCount" class="w-full" :min="1" :precision="0" placeholder="不填不限制" /></el-form-item
          ></el-col>
        </el-row>
        <el-divider content-position="left">技术规则</el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="媒体类型">
              <el-select v-model="requirementRuleSettings.mediaType" clearable class="w-full" placeholder="不限制">
                <el-option label="视频" value="video" />
                <el-option label="图片" value="image" />
                <el-option label="音频" value="audio" />
                <el-option label="文档" value="document" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="附件技术校验">
              <el-select v-model="requirementRuleSettings.technicalCheckMode" clearable class="w-full" placeholder="继承类别默认策略">
                <el-option label="继承类别默认策略" value="" />
                <el-option label="仅人工复核提示" value="format_only" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"
            ><el-form-item label="最小宽度"><el-input-number v-model="requirementRuleSettings.minWidth" class="w-full" :min="0" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="最小高度"><el-input-number v-model="requirementRuleSettings.minHeight" class="w-full" :min="0" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="帧率fps"
              ><el-input-number v-model="requirementRuleSettings.fps" class="w-full" :min="0" :precision="2" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="码率Mbps"
              ><el-input-number v-model="requirementRuleSettings.minBitrateMbps" class="w-full" :min="0" :precision="2" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="最短秒数"
              ><el-input-number v-model="requirementRuleSettings.minDurationSeconds" class="w-full" :min="0" /></el-form-item
          ></el-col>
          <el-col :span="12"
            ><el-form-item label="最长秒数"
              ><el-input-number v-model="requirementRuleSettings.maxDurationSeconds" class="w-full" :min="0" /></el-form-item
          ></el-col>
        </el-row>
        <el-form-item label="规则JSON">
          <el-input
            v-model="requirementForm.ruleJson"
            type="textarea"
            :rows="5"
            placeholder='{"mediaType":"video","minWidth":1920,"minHeight":1080,"fps":25,"fpsTolerance":0.5,"minBitrateMbps":10}'
          />
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="requirementForm.sortOrder" class="w-full" /></el-form-item>
        <el-form-item label="必传"><el-switch v-model="requirementForm.required" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitRequirement">确定</el-button>
        <el-button @click="requirementDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog
      v-model="activityDeleteDialog.visible"
      :title="`${activityDeleteDialog.mode === 'purge' ? '活动永久删除检查' : '活动删除检查'}${activityDeleteCheck?.activityName ? ` - ${activityDeleteCheck.activityName}` : ''}`"
      width="88%"
      top="5vh"
      append-to-body
      destroy-on-close
    >
      <div v-loading="activityDeleteDialog.loading" class="category-purge-dialog">
        <template v-if="activityDeleteCheck">
          <el-alert
            :type="activityDeleteCheck.canDelete ? 'success' : 'error'"
            :closable="false"
            show-icon
            :title="
              activityDeleteCheck.canDelete
                ? activityDeleteDialog.mode === 'purge'
                  ? '检查通过：没有历史项目、上传文件或其他业务引用，可以永久删除活动。'
                  : '检查通过：没有历史项目、上传文件或其他业务引用，可以将活动移入“已删活动”。'
                : activityDeleteCheck.blockMessage || '存在业务数据，不能删除活动。'
            "
          />

          <el-descriptions class="category-purge-summary" :column="4" border>
            <el-descriptions-item label="活动ID">{{ activityDeleteCheck.activityId }}</el-descriptions-item>
            <el-descriptions-item label="活动名称">{{ activityDeleteCheck.activityName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="类别配置">{{ activityDeleteCheck.categoryCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="学校范围">{{ activityDeleteCheck.schoolScopeCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="字段配置">{{ activityDeleteCheck.fieldSchemaCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="附件要求">{{ activityDeleteCheck.fileRequirementCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="历史项目">{{ activityDeleteCheck.projectCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="上传文件">{{ activityDeleteCheck.fileCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="其他业务引用">{{ activityDeleteCheck.referenceCount || 0 }}</el-descriptions-item>
          </el-descriptions>

          <el-alert
            class="category-purge-owned-config"
            type="info"
            :closable="false"
            show-icon
            :title="
              activityDeleteDialog.mode === 'purge'
                ? '类别、字段、附件要求和学校范围属于活动自身配置，检查通过后会一并物理清理；项目、上传文件及其他业务记录不会自动删除，OSS 对象也不会删除。'
                : '普通删除只将活动移入“已删活动”，类别、字段、附件要求和学校范围配置会完整保留，可恢复；任何业务历史或引用都会阻止删除。'
            "
          />

          <el-tabs v-model="activityDeleteTab" class="category-purge-tabs">
            <el-tab-pane :label="`活动配置 (${activityDeleteCheck.categoryCount || 0})`" name="category">
              <el-alert
                v-if="activityDeleteCheck.categoryDetailsTruncated"
                type="warning"
                :closable="false"
                title="类别配置总数准确，明细仅展示前 200 条。"
                class="category-purge-limit-alert"
              />
              <el-table border :data="activityDeleteCheck.categories || []" max-height="360">
                <el-table-column label="类别ID" prop="id" width="150">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="copyPurgeText(String(row.id || ''))">{{ row.id }}</el-button>
                  </template>
                </el-table-column>
                <el-table-column label="类别编码" prop="categoryCode" min-width="160" show-overflow-tooltip />
                <el-table-column label="类别名称" prop="categoryName" min-width="190" show-overflow-tooltip />
                <el-table-column label="父级ID" prop="parentId" width="150" />
                <el-table-column label="旧分组" prop="categoryGroup" min-width="150" show-overflow-tooltip />
                <el-table-column label="字段" prop="fieldSchemaCount" width="72" align="center" />
                <el-table-column label="附件要求" prop="fileRequirementCount" width="90" align="center" />
                <el-table-column label="数据状态" width="92" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.delFlag === '1' ? 'info' : 'success'">{{ row.delFlag === '1' ? '已删除' : '正常' }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`历史项目 (${activityDeleteCheck.projectCount || 0})`" name="project">
              <el-alert
                v-if="activityDeleteCheck.projectDetailsTruncated"
                type="warning"
                :closable="false"
                title="历史项目总数准确，明细仅展示前 200 条。"
                class="category-purge-limit-alert"
              />
              <el-table border :data="activityDeleteCheck.projects || []" max-height="360">
                <el-table-column label="项目ID" prop="id" width="150">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="copyPurgeText(String(row.id || ''))">{{ row.id }}</el-button>
                  </template>
                </el-table-column>
                <el-table-column label="项目编号" prop="projectNo" min-width="150" show-overflow-tooltip />
                <el-table-column label="项目名称" prop="projectName" min-width="190" show-overflow-tooltip />
                <el-table-column label="学校" min-width="180" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.schoolName || '-' }}（{{ row.schoolId || '-' }}）</template>
                </el-table-column>
                <el-table-column label="项目状态" width="110" align="center">
                  <template #default="{ row }">{{ categoryPurgeProjectStatus(row.status) }}</template>
                </el-table-column>
                <el-table-column label="数据状态" width="92" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.delFlag === '1' ? 'info' : row.status === 'recycled' ? 'warning' : 'success'">
                      {{ row.delFlag === '1' ? '逻辑删除' : row.status === 'recycled' ? '回收站' : '正常' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="文件数" prop="fileCount" width="78" align="center" />
                <el-table-column label="操作" width="130" fixed="right" align="center">
                  <template #default="{ row }">
                    <el-button v-if="row.delFlag !== '1' && row.status !== 'recycled'" link type="primary" @click="openPurgeProject(row.id)"
                      >打开项目</el-button
                    >
                    <el-button v-else-if="row.status === 'recycled'" link type="warning" @click="openPurgeProjectRecycle(row.id)">回收站</el-button>
                    <span v-else class="category-purge-muted">复制ID查验</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`上传文件 (${activityDeleteCheck.fileCount || 0})`" name="file">
              <el-alert
                v-if="activityDeleteCheck.fileDetailsTruncated"
                type="warning"
                :closable="false"
                title="上传文件总数准确，明细仅展示前 200 条。"
                class="category-purge-limit-alert"
              />
              <el-table border :data="activityDeleteCheck.files || []" max-height="360">
                <el-table-column label="文件ID" prop="id" width="150">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="copyPurgeText(String(row.id || ''))">{{ row.id }}</el-button>
                  </template>
                </el-table-column>
                <el-table-column label="项目ID" prop="projectId" width="150">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="copyPurgeText(String(row.projectId || ''))">{{ row.projectId }}</el-button>
                  </template>
                </el-table-column>
                <el-table-column label="原文件名" prop="originalName" min-width="220" show-overflow-tooltip />
                <el-table-column label="项目名称" prop="projectName" min-width="180" show-overflow-tooltip />
                <el-table-column label="OSS ID" prop="ossId" width="130" />
                <el-table-column label="文件状态" width="100" align="center">
                  <template #default="{ row }">{{ categoryPurgeFileStatus(row.status) }}</template>
                </el-table-column>
                <el-table-column label="数据状态" width="92" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.delFlag === '1' ? 'info' : 'success'">{{ row.delFlag === '1' ? '逻辑删除' : '正常' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="上传时间" prop="uploadedAt" width="170" />
                <el-table-column label="操作" width="130" fixed="right" align="center">
                  <template #default="{ row }">
                    <el-button v-if="row.status === 'deleted'" link type="warning" @click="openPurgeFileRecycle(row.projectId)">附件回收站</el-button>
                    <el-button
                      v-else-if="row.projectDelFlag !== '1' && row.projectStatus !== 'recycled'"
                      link
                      type="primary"
                      @click="openPurgeProject(row.projectId)"
                    >
                      打开项目
                    </el-button>
                    <span v-else class="category-purge-muted">复制ID查验</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`业务引用 (${activityDeleteCheck.referenceCount || 0})`" name="reference">
              <el-table border :data="activityDeleteCheck.references || []" max-height="360">
                <el-table-column label="业务类型" prop="referenceLabel" min-width="180" />
                <el-table-column label="数据表" prop="referenceType" min-width="190" />
                <el-table-column label="数量" prop="referenceCount" width="90" align="center" />
                <el-table-column label="记录ID" prop="recordIds" min-width="320" show-overflow-tooltip />
                <el-table-column label="操作" width="100" align="center">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="copyPurgeText(row.recordIds || '')">复制ID</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
      <template #footer>
        <el-button v-if="activityDeleteCheck" icon="CopyDocument" :disabled="activityDeleteDialog.loading" @click="copyActivityDeleteReport">
          复制检查明细
        </el-button>
        <el-button v-if="activityDeleteCheck" icon="Refresh" :loading="activityDeleteDialog.loading" @click="refreshActivityDeleteCheck">
          重新检查
        </el-button>
        <el-button
          v-if="activityDeleteCheck?.canDelete"
          v-hasPermi="['crehn:activity:remove']"
          type="danger"
          icon="Delete"
          :loading="activityDeleteDialog.submitting"
          @click="confirmActivityDelete"
        >
          {{ activityDeleteDialog.mode === 'purge' ? '确认永久删除' : '确认删除活动' }}
        </el-button>
        <el-button @click="activityDeleteDialog.visible = false">关闭</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog
      v-model="categoryPurgeDialog.visible"
      :title="`${categoryPurgeDialog.mode === 'purge' ? '类别永久删除检查' : '类别删除检查'}${categoryPurgeCheck?.categoryName ? ` - ${categoryPurgeCheck.categoryName}` : ''}`"
      width="88%"
      top="5vh"
      append-to-body
      destroy-on-close
    >
      <div v-loading="categoryPurgeDialog.loading" class="category-purge-dialog">
        <template v-if="categoryPurgeCheck">
          <el-alert
            :type="categoryPurgeCheck.canPurge ? 'success' : 'error'"
            :closable="false"
            show-icon
            :title="
              categoryPurgeCheck.canPurge
                ? categoryPurgeDialog.mode === 'purge'
                  ? '检查通过：没有子类别、历史项目、上传文件或外部业务引用，可以永久删除。'
                  : '检查通过：没有子类别、历史项目、上传文件或外部业务引用，可以将类别移入“已删类”。'
                : categoryPurgeCheck.blockMessage || '存在业务数据，不能删除类别。'
            "
          />

          <el-descriptions class="category-purge-summary" :column="4" border>
            <el-descriptions-item label="类别ID">{{ categoryPurgeCheck.categoryId }}</el-descriptions-item>
            <el-descriptions-item label="类别编码">{{ categoryPurgeCheck.categoryCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="子类别">{{ categoryPurgeCheck.childCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="历史项目">{{ categoryPurgeCheck.projectCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="上传文件">{{ categoryPurgeCheck.fileCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="外部业务引用">{{ categoryPurgeCheck.referenceCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="字段配置">{{ categoryPurgeCheck.fieldSchemaCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="附件要求配置">{{ categoryPurgeCheck.fileRequirementCount || 0 }}</el-descriptions-item>
          </el-descriptions>

          <el-alert
            class="category-purge-owned-config"
            type="info"
            :closable="false"
            show-icon
            :title="
              categoryPurgeDialog.mode === 'purge'
                ? '检查范围包含正常、回收站和逻辑删除的历史记录。字段配置和附件要求属于类别自身配置，检查通过后会一并物理清理；项目、上传文件和其他业务记录不会自动删除。'
                : '普通删除只将类别移入“已删类”，字段和附件要求配置会完整保留，可恢复；任何子类别、历史项目、上传文件或外部引用都会阻止删除。'
            "
          />

          <el-tabs v-model="categoryPurgeTab" class="category-purge-tabs">
            <el-tab-pane :label="`子类别 (${categoryPurgeCheck.childCount || 0})`" name="child">
              <el-alert
                v-if="categoryPurgeCheck.childDetailsTruncated"
                type="warning"
                :closable="false"
                title="子类别总数准确，明细仅展示前 200 条。"
                class="category-purge-limit-alert"
              />
              <el-table border :data="categoryPurgeCheck.children || []" max-height="360">
                <el-table-column label="类别ID" prop="id" width="150">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="copyPurgeText(String(row.id || ''))">{{ row.id }}</el-button>
                  </template>
                </el-table-column>
                <el-table-column label="类别编码" prop="categoryCode" min-width="170" show-overflow-tooltip />
                <el-table-column label="类别名称" prop="categoryName" min-width="200" show-overflow-tooltip />
                <el-table-column label="当前状态" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.delFlag === '1' ? 'info' : 'success'">{{ row.delFlag === '1' ? '已删除' : '正常' }}</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`历史项目 (${categoryPurgeCheck.projectCount || 0})`" name="project">
              <div class="category-purge-tab-tools">
                <el-alert
                  v-if="categoryPurgeCheck.projectDetailsTruncated"
                  type="warning"
                  :closable="false"
                  title="历史项目总数准确，明细仅展示前 200 条。"
                  class="category-purge-limit-alert"
                />
                <el-button v-if="categoryPurgeCheck.projectCount" link type="primary" icon="Search" @click="openCategoryProjectRecycle">
                  按类别ID打开项目回收站
                </el-button>
              </div>
              <el-table border :data="categoryPurgeCheck.projects || []" max-height="360">
                <el-table-column label="项目ID" prop="id" width="150">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="copyPurgeText(String(row.id || ''))">{{ row.id }}</el-button>
                  </template>
                </el-table-column>
                <el-table-column label="项目编号" prop="projectNo" min-width="150" show-overflow-tooltip />
                <el-table-column label="项目名称" prop="projectName" min-width="190" show-overflow-tooltip />
                <el-table-column label="学校" min-width="180" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.schoolName || '-' }}（{{ row.schoolId || '-' }}）</template>
                </el-table-column>
                <el-table-column label="项目状态" width="110" align="center">
                  <template #default="{ row }">{{ categoryPurgeProjectStatus(row.status) }}</template>
                </el-table-column>
                <el-table-column label="数据状态" width="92" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.delFlag === '1' ? 'info' : row.status === 'recycled' ? 'warning' : 'success'">
                      {{ row.delFlag === '1' ? '逻辑删除' : row.status === 'recycled' ? '回收站' : '正常' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="文件数" prop="fileCount" width="78" align="center" />
                <el-table-column label="操作" width="130" fixed="right" align="center">
                  <template #default="{ row }">
                    <el-button v-if="row.delFlag !== '1' && row.status !== 'recycled'" link type="primary" @click="openPurgeProject(row.id)"
                      >打开项目</el-button
                    >
                    <el-button v-else-if="row.status === 'recycled'" link type="warning" @click="openPurgeProjectRecycle(row.id)">回收站</el-button>
                    <span v-else class="category-purge-muted">复制ID查验</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`上传文件 (${categoryPurgeCheck.fileCount || 0})`" name="file">
              <el-alert
                v-if="categoryPurgeCheck.fileDetailsTruncated"
                type="warning"
                :closable="false"
                title="上传文件总数准确，明细仅展示前 200 条。"
                class="category-purge-limit-alert"
              />
              <el-table border :data="categoryPurgeCheck.files || []" max-height="360">
                <el-table-column label="文件ID" prop="id" width="150">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="copyPurgeText(String(row.id || ''))">{{ row.id }}</el-button>
                  </template>
                </el-table-column>
                <el-table-column label="项目ID" prop="projectId" width="150">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="copyPurgeText(String(row.projectId || ''))">{{ row.projectId }}</el-button>
                  </template>
                </el-table-column>
                <el-table-column label="原文件名" prop="originalName" min-width="220" show-overflow-tooltip />
                <el-table-column label="项目名称" prop="projectName" min-width="180" show-overflow-tooltip />
                <el-table-column label="OSS ID" prop="ossId" width="130" />
                <el-table-column label="文件状态" width="100" align="center">
                  <template #default="{ row }">{{ categoryPurgeFileStatus(row.status) }}</template>
                </el-table-column>
                <el-table-column label="数据状态" width="92" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.delFlag === '1' ? 'info' : 'success'">{{ row.delFlag === '1' ? '逻辑删除' : '正常' }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="上传时间" prop="uploadedAt" width="170" />
                <el-table-column label="操作" width="130" fixed="right" align="center">
                  <template #default="{ row }">
                    <el-button v-if="row.status === 'deleted'" link type="warning" @click="openPurgeFileRecycle(row.projectId)">附件回收站</el-button>
                    <el-button
                      v-else-if="row.projectDelFlag !== '1' && row.projectStatus !== 'recycled'"
                      link
                      type="primary"
                      @click="openPurgeProject(row.projectId)"
                    >
                      打开项目
                    </el-button>
                    <span v-else class="category-purge-muted">复制ID查验</span>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane :label="`外部引用 (${categoryPurgeCheck.referenceCount || 0})`" name="reference">
              <el-table border :data="categoryPurgeCheck.references || []" max-height="360">
                <el-table-column label="业务类型" prop="referenceLabel" min-width="180" />
                <el-table-column label="数据表" prop="referenceType" min-width="190" />
                <el-table-column label="数量" prop="referenceCount" width="90" align="center" />
                <el-table-column label="记录ID" prop="recordIds" min-width="320" show-overflow-tooltip />
                <el-table-column label="操作" width="100" align="center">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="copyPurgeText(row.recordIds || '')">复制ID</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
      <template #footer>
        <el-button v-if="categoryPurgeCheck" icon="CopyDocument" :disabled="categoryPurgeDialog.loading" @click="copyCategoryPurgeReport">
          复制检查明细
        </el-button>
        <el-button v-if="categoryPurgeCheck" icon="Refresh" :loading="categoryPurgeDialog.loading" @click="refreshCategoryPurgeCheck">
          重新检查
        </el-button>
        <el-button
          v-if="categoryPurgeCheck?.canPurge"
          v-hasPermi="['crehn:category:remove']"
          type="danger"
          icon="Delete"
          :loading="categoryPurgeDialog.submitting"
          @click="confirmCategoryDelete"
        >
          {{ categoryPurgeDialog.mode === 'purge' ? '确认永久删除' : '确认删除类别' }}
        </el-button>
        <el-button @click="categoryPurgeDialog.visible = false">关闭</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ArtActivity" lang="ts">
// ART-REF: FE.ACTIVITY_CONFIG.RULE_UTILS -> utils/activityRule.ts
import {
  addActivity,
  addCategory,
  copyCategory,
  addField,
  addFileRequirement,
  checkActivityDelete,
  checkActivityPurge,
  checkCategoryDelete,
  checkCategoryPurge,
  delActivity,
  delCategory,
  delField,
  delFileRequirement,
  exportActivityConfigExcel,
  exportActivityConfigJson,
  getFieldUsageCount,
  getFileRequirementUsageCount,
  importActivityConfig,
  mergeActivityConfig,
  listActivity,
  listCategory,
  listField,
  listFileRequirement,
  purgeActivity,
  purgeCategory,
  previewActivityConfigMerge,
  restoreActivity,
  restoreCategory,
  restoreField,
  restoreFileRequirement,
  updateActivity,
  updateCategory,
  updateField,
  updateFileRequirement,
  uploadFileRequirementTemplate
} from '@/api/crehn/activity';
import {
  ActivityCategoryPurgeCheckVO,
  ActivityCategoryVO,
  ActivityConfigMergePreviewVO,
  ActivityDeleteCheckVO,
  ActivityVO,
  CategoryFieldSchemaVO,
  CategoryFileRequirementVO
} from '@/api/crehn/types';
import {
  ActivityCategoryTreeNode,
  CATEGORY_NODE_TYPE_CATEGORY,
  CATEGORY_NODE_TYPE_GROUP,
  buildActivityCategoryTree,
  findCategoryParentGroup,
  isCategoryGroup,
  isCategoryMenuVisible,
  isLegacyGroupedCategory,
  parseCategoryRule,
  updateCategoryRuleMeta
} from '@/utils/artCategory';
import { checkPermi } from '@/utils/permission';
import {
  GenericTableTemplate,
  normalizeGenericTableTemplates,
  serializeGenericTableTemplates,
  validateGenericTableTemplates
} from '@/utils/artGenericTable';
import { isMemberAttachmentType, normalizeMemberTypeOptions, type MemberTypeOption } from '@/utils/artMemberTable';
import GenericTableTemplateEditor from './components/GenericTableTemplateEditor.vue';
import ActivityListPanel from './components/ActivityListPanel.vue';
import CategoryListPanel from './components/CategoryListPanel.vue';
import CategoryRuleDialog from './components/CategoryRuleDialog.vue';
import FieldSchemaTab from './components/FieldSchemaTab.vue';
import MenuStyleDialog from './components/MenuStyleDialog.vue';
import { parseJsonObject } from './utils/activityRule';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/store/modules/user';

type FormLayoutMode = 'single' | 'double' | 'custom';
type FormLayoutSpan = 0 | 12 | 24;

type FormLayoutSettings = {
  mode: FormLayoutMode;
  fieldSpans: Record<string, FormLayoutSpan>;
};

type FormLayoutRow = {
  layoutKey: string;
  sortOrder: number;
  fieldLabel: string;
  fieldType: string;
  field?: CategoryFieldSchemaVO;
};

type DeleteCheckMode = 'delete' | 'purge';
type SortDirection = 'up' | 'down';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const router = useRouter();
const userStore = useUserStore();

const loading = ref(false);
const showSearch = ref(true);
const total = ref(0);
const activeTab = ref('field');
const activityPanelExpanded = ref(false);
const activityManagerVisible = ref(false);
const activityConfigImporting = ref(false);
const currentConfigExporting = ref(false);
const currentConfigImporting = ref(false);
const configMergeFile = ref<File>();
const configMergePreview = ref<ActivityConfigMergePreviewVO>();
const configMergeTab = ref<'additions' | 'skipped'>('additions');
const configMergeDialog = reactive({ visible: false, submitting: false });
const queryCollapsed = ref(false);
const queryParams = reactive({ pageNum: 1, pageSize: 10, activityName: '', status: '' });

const activityList = ref<ActivityVO[]>([]);
const categoryList = ref<ActivityCategoryVO[]>([]);
const fieldList = ref<CategoryFieldSchemaVO[]>([]);
const requirementList = ref<CategoryFileRequirementVO[]>([]);
const currentActivity = ref<ActivityVO>();
const currentCategory = ref<ActivityCategoryTreeNode>();
const activityDeleted = ref(false);
const categoryDeleted = ref(false);
const fieldDeleted = ref(false);
const requirementDeleted = ref(false);

const activityDialog = reactive({ visible: false, title: '' });
const categoryDialog = reactive({ visible: false, title: '' });
const fieldDialog = reactive({ visible: false, title: '' });
const formLayoutDialog = reactive({ visible: false, saving: false });
const memberFieldDialog = reactive({ visible: false, saving: false });
const requirementDialog = reactive({ visible: false, title: '' });
const menuStyleDialogRef = ref<InstanceType<typeof MenuStyleDialog>>();
const activityDeleteDialog = reactive<{ visible: boolean; loading: boolean; submitting: boolean; mode: DeleteCheckMode }>({
  visible: false,
  loading: false,
  submitting: false,
  mode: 'delete'
});
const activityDeleteCheck = ref<ActivityDeleteCheckVO>();
const activityDeleteTab = ref('project');
const categoryPurgeDialog = reactive<{ visible: boolean; loading: boolean; submitting: boolean; mode: DeleteCheckMode }>({
  visible: false,
  loading: false,
  submitting: false,
  mode: 'purge'
});
const categoryPurgeCheck = ref<ActivityCategoryPurgeCheckVO>();
const categoryPurgeTab = ref('project');

const activityForm = ref<ActivityVO>({});
const categoryForm = ref<ActivityCategoryVO>({});
const categoryDialogNodeType = ref<typeof CATEGORY_NODE_TYPE_GROUP | typeof CATEGORY_NODE_TYPE_CATEGORY>(CATEGORY_NODE_TYPE_CATEGORY);
const categoryDialogMenuVisible = ref(true);
const categoryDialogTypeLocked = ref(false);
const categoryParentKey = ref('');
const categoryRuleForm = ref<any>({});
const categoryDialogFieldList = ref<CategoryFieldSchemaVO[]>([]);
const workshopRuleSettings = ref<WorkshopRuleSettings>(defaultWorkshopRuleSettings());
const fieldForm = ref<CategoryFieldSchemaVO>({});
const formLayoutSettings = ref<FormLayoutSettings>({ mode: 'single', fieldSpans: {} });
const requirementForm = ref<CategoryFileRequirementVO>({});
const requirementRuleSettings = ref<any>({});
const allFormatsAllowed = ref(true);
const customAllowedExts = ref<string[]>([]);
const requirementTemplateSettings = ref<any>({
  enabled: false,
  name: '',
  ossId: '',
  fileName: ''
});
const requirementTemplateFileTypes = ['xls', 'xlsx', 'doc', 'docx', 'pdf'];
const requirementTemplateAccept = requirementTemplateFileTypes.map((item) => `.${item}`).join(',');
const defaultUploadConfirmationText = '我确认视频中不出现省份、学校、姓名、指导教师等身份信息';
const defaultUploadConfirmationTextColor = '#f56c6c';
const uploadConfirmationSettings = ref<any>({
  enabled: false,
  text: defaultUploadConfirmationText,
  textColor: defaultUploadConfirmationTextColor
});
const selectedFieldTemplate = ref('');
const fieldOptions = ref<string[]>([]);
const fieldValidationType = ref('');
const fieldUiSettings = ref<FieldUiSettings>(defaultFieldUiSettings());
const durationSettings = ref<DurationSettings>(defaultDurationSettings());
const cascadeOptionRows = ref<CascadeOptionRow[]>([]);
const dimensionRows = ref<DimensionRow[]>(defaultDimensionRows());
const teacherLimitSettings = ref<TeacherLimitSettings>(defaultTeacherLimitSettings());
const teacherMemberRole = ref('');
const teacherSubFields = ref<SubFieldRow[]>([]);
const activeMemberFieldGroup = ref<'author' | 'participant'>('author');
const activeTableConfigMode = ref<'member' | 'generic'>('member');
const memberTipText = ref('');
const memberTemplateFooterText = ref('');
const memberTableTitle = ref('');
const memberTipLabel = ref('');
const memberTypeOptions = ref<MemberTypeOption[]>([]);
const genericTableTemplates = ref<GenericTableTemplate[]>([]);
const memberTableSettings = ref<MemberTableSettings>(defaultMemberTableSettings());
const memberSubFields = reactive<Record<'author' | 'participant', SubFieldRow[]>>({ author: [], participant: [] });
const optionFieldTypes = ['select', 'radio', 'checkbox'];
const memberCountRuleRows: Array<{ key: keyof Omit<MemberTableSettings, 'enabled'>; label: string }> = [
  { key: 'total', label: '总人数' },
  { key: 'author', label: '作者人数' },
  { key: 'student', label: '学生人数' },
  { key: 'teacher', label: '指导教师人数' },
  { key: 'completer', label: '完成人人数' }
];
const validatorTypeOptions = [
  { label: '艺术表演 performance', value: 'performance', defaultName: '艺术表演校验' },
  { label: '艺术作品 artwork', value: 'artwork', defaultName: '艺术作品校验' },
  { label: '工作坊 workshop', value: 'workshop', defaultName: '工作坊校验' },
  { label: '优秀成果 aesthetic_achievement', value: 'aesthetic_achievement', defaultName: '优秀成果校验' },
  { label: '暂不启用校验', value: 'none', defaultName: '' }
];

const categoryRuleTemplateOptions = [
  {
    label: '声乐',
    templateCode: 'tpl_performance_vocal',
    categoryCode: 'performance_vocal',
    categoryName: '声乐',
    categoryGroup: '艺术表演类',
    validatorType: 'performance',
    projectNameLabel: '节目名称'
  },
  {
    label: '器乐',
    templateCode: 'tpl_performance_instrumental',
    categoryCode: 'performance_instrumental',
    categoryName: '器乐',
    categoryGroup: '艺术表演类',
    validatorType: 'performance',
    projectNameLabel: '节目名称'
  },
  {
    label: '舞蹈',
    templateCode: 'tpl_performance_dance',
    categoryCode: 'performance_dance',
    categoryName: '舞蹈',
    categoryGroup: '艺术表演类',
    validatorType: 'performance',
    projectNameLabel: '节目名称'
  },
  {
    label: '戏剧',
    templateCode: 'tpl_performance_drama',
    categoryCode: 'performance_drama',
    categoryName: '戏剧',
    categoryGroup: '艺术表演类',
    validatorType: 'performance',
    projectNameLabel: '节目名称'
  },
  {
    label: '朗诵',
    templateCode: 'tpl_performance_recitation',
    categoryCode: 'performance_recitation',
    categoryName: '朗诵',
    categoryGroup: '艺术表演类',
    validatorType: 'performance',
    projectNameLabel: '节目名称'
  },
  {
    label: '个人项目',
    templateCode: 'tpl_performance_personal',
    categoryCode: 'performance_personal',
    categoryName: '个人项目',
    categoryGroup: '艺术表演类',
    validatorType: 'performance',
    projectNameLabel: '节目名称'
  },
  {
    label: '美术类',
    templateCode: 'tpl_artwork_fine_art',
    categoryCode: 'artwork_fine_art',
    categoryName: '美术类',
    categoryGroup: '艺术作品类',
    validatorType: 'artwork',
    projectNameLabel: '作品名称'
  },
  {
    label: '大艺展设计类',
    templateCode: 'tpl_artwork_grand_design',
    categoryCode: 'artwork_grand_design',
    categoryName: '大艺展设计类',
    categoryGroup: '艺术作品类',
    validatorType: 'artwork',
    projectNameLabel: '作品名称'
  },
  {
    label: '设计展',
    templateCode: 'tpl_extension_design_exhibition',
    categoryCode: 'extension_design_exhibition',
    categoryName: '设计展',
    categoryGroup: '扩展设计展',
    validatorType: 'artwork',
    projectNameLabel: '作品名称',
    mediaTechnicalCheckMode: 'manual',
    extensionModule: true,
    enabled: false
  },
  {
    label: '影视类',
    templateCode: 'tpl_artwork_film',
    categoryCode: 'artwork_film',
    categoryName: '影视类',
    categoryGroup: '艺术作品类',
    validatorType: 'artwork',
    projectNameLabel: '作品名称'
  },
  {
    label: '高校校长书画作品',
    templateCode: 'tpl_artwork_principal_calligraphy_painting',
    categoryCode: 'artwork_principal',
    categoryName: '高校校长书画作品',
    categoryGroup: '高校校长书画作品',
    validatorType: 'artwork',
    projectNameLabel: '作品名称'
  },
  {
    label: '艺术实践工作坊',
    templateCode: 'tpl_workshop_practice',
    categoryCode: 'workshop',
    categoryName: '艺术实践工作坊',
    categoryGroup: '艺术实践工作坊',
    validatorType: 'workshop',
    projectNameLabel: '工作坊名称'
  },
  {
    label: '学术论文',
    templateCode: 'tpl_achievement_paper',
    categoryCode: 'achievement_paper',
    categoryName: '学术论文',
    categoryGroup: '高校美育改革创新优秀成果',
    validatorType: 'aesthetic_achievement',
    projectNameLabel: '论文标题'
  },
  {
    label: '教学改革案例',
    templateCode: 'tpl_achievement_case',
    categoryCode: 'achievement_case',
    categoryName: '教学改革案例',
    categoryGroup: '高校美育改革创新优秀成果',
    validatorType: 'aesthetic_achievement',
    projectNameLabel: '案例标题'
  }
];

const fileTypeOptions = [
  { code: 'all', name: '所有类型' },
  { code: 'document', name: '文档' },
  { code: 'image', name: '图片' },
  { code: 'video', name: '视频' },
  { code: 'audio', name: '音频' },
  { code: 'archive', name: '压缩包' }
];

const allowedExtOptions = [
  { label: '所有格式', value: '*' },
  { label: '文档：doc,docx,pdf', value: 'doc,docx,pdf' },
  { label: '表格：xls,xlsx', value: 'xls,xlsx' },
  { label: '图片：jpg,jpeg,png,gif,webp', value: 'jpg,jpeg,png,gif,webp' },
  { label: '视频：mp4,mov,avi,mkv', value: 'mp4,mov,avi,mkv' },
  { label: '音频：mp3,wav,m4a', value: 'mp3,wav,m4a' },
  { label: '压缩包：zip,rar,7z', value: 'zip,rar,7z' }
];

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
  { label: '关键词数量', value: 'keyword_count' },
  { label: '备注', value: 'remark' }
];

const inputWidthModeOptions = [
  { label: '默认', value: 'default' },
  { label: '短', value: 'short' },
  { label: '中', value: 'medium' },
  { label: '长', value: 'long' },
  { label: '整行', value: 'full' },
  { label: '自定义', value: 'custom' }
];

type SubFieldRow = {
  fieldKey?: string;
  fieldLabel?: string;
  fieldType?: string;
  required?: boolean;
  sensitive?: boolean;
  validationType?: string;
  optionsJson?: string;
  optionsText?: string;
  matchFieldKey?: string;
  attachmentAllowedExts?: string;
  sortOrder?: number;
};

type FieldUiSettings = {
  placeholder: string;
  placeholderColor: string;
  placeholderFontSize?: number;
  suffixText: string;
  suffixColor: string;
  suffixFontSize?: number;
  helpText: string;
  helpColor: string;
  helpFontSize?: number;
  inputWidthMode: string;
  inputWidthCustom: string;
  minLength?: number;
  maxLength?: number;
  minKeywordCount?: number;
  maxKeywordCount?: number;
  showWordLimit: boolean;
  numberMin?: number;
  numberMax?: number;
  integerOnly: boolean;
  precision?: number;
  step?: number;
};

type DurationSettings = {
  durationMode: string;
};

type CascadeOptionRow = {
  level1: string;
  level2: string;
  level3: string;
};

type DimensionRow = {
  key: string;
  label: string;
  placeholder: string;
  unit: string;
  required: boolean;
};

type TeacherLimitSettings = {
  minItems: number;
  maxItems: number;
};

type MemberCountRuleSettings = {
  min?: number;
  max?: number;
};

type MemberTableSettings = {
  enabled: boolean;
  total: MemberCountRuleSettings;
  author: MemberCountRuleSettings;
  student: MemberCountRuleSettings;
  teacher: MemberCountRuleSettings;
  completer: MemberCountRuleSettings;
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
  nodeType: typeof CATEGORY_NODE_TYPE_GROUP | typeof CATEGORY_NODE_TYPE_CATEGORY;
  menuVisible: boolean;
  categoryParentKey: string;
  categoryRuleForm: Record<string, any>;
  workshopRuleSettings: WorkshopRuleSettings;
};

const subFieldTypeOptions = [
  { label: '单行文本', value: 'input' },
  { label: '多行文本', value: 'textarea' },
  { label: '下拉选择', value: 'select' },
  { label: '单选', value: 'radio' },
  { label: '多选', value: 'checkbox' },
  { label: '日期', value: 'date' },
  { label: '数字', value: 'number' },
  { label: '身份证号', value: 'id_card' },
  { label: '图片上传', value: 'image_upload' },
  { label: 'PDF上传', value: 'pdf_upload' },
  { label: '附件上传', value: 'attachment_upload' }
];

type FieldTemplate = {
  fieldKey: string;
  fieldLabel: string;
  fieldType: string;
  required?: boolean;
  sensitive?: boolean;
  options?: string[];
  validationJson?: string;
};

const defaultDimensionValidationJson = JSON.stringify({
  dimensions: [
    { key: 'length', label: '长', placeholder: '长', unit: 'cm', required: true },
    { key: 'width', label: '宽', placeholder: '宽', unit: 'cm', required: true },
    { key: 'height', label: '高', placeholder: '高', unit: 'cm', required: true }
  ],
  helpText: '尺寸不超过50cm（长）×50cm（宽）×50cm（高）'
});

const fieldTemplates: FieldTemplate[] = [
  { fieldLabel: '姓名', fieldKey: 'student_name', fieldType: 'input', required: true },
  { fieldLabel: '性别', fieldKey: 'gender', fieldType: 'select', options: ['男', '女', '其他'] },
  { fieldLabel: '民族', fieldKey: 'nation', fieldType: 'input' },
  { fieldLabel: '年龄', fieldKey: 'age', fieldType: 'number' },
  { fieldLabel: '身份证号', fieldKey: 'id_card', fieldType: 'id_card', sensitive: true },
  { fieldLabel: '手机号', fieldKey: 'phone', fieldType: 'input', sensitive: true },
  { fieldLabel: '学校名称', fieldKey: 'school_name', fieldType: 'input' },
  { fieldLabel: '学段', fieldKey: 'school_stage', fieldType: 'select', options: ['小学', '初中', '高中', '中职', '高职', '本科', '研究生', '其他'] },
  { fieldLabel: '联系电话', fieldKey: 'contact_phone', fieldType: 'input' },
  { fieldLabel: '邮箱', fieldKey: 'email', fieldType: 'input' },
  { fieldLabel: '班级', fieldKey: 'class_name', fieldType: 'input' },
  { fieldLabel: '年级', fieldKey: 'grade_name', fieldType: 'input' },
  { fieldLabel: '专业', fieldKey: 'major_name', fieldType: 'input' },
  { fieldLabel: '院系', fieldKey: 'department_name', fieldType: 'input' },
  { fieldLabel: '学号', fieldKey: 'student_no', fieldType: 'input' },
  { fieldLabel: '家长姓名', fieldKey: 'parent_name', fieldType: 'input' },
  { fieldLabel: '家长电话', fieldKey: 'parent_phone', fieldType: 'input', sensitive: true },
  { fieldLabel: '家庭地址', fieldKey: 'address', fieldType: 'textarea', sensitive: true },
  { fieldLabel: '活动名称', fieldKey: 'activity_name', fieldType: 'input' },
  { fieldLabel: '项目类别', fieldKey: 'category_name', fieldType: 'input' },
  { fieldLabel: '作品名称', fieldKey: 'work_name', fieldType: 'input', required: true },
  { fieldLabel: '作品类别', fieldKey: 'work_type', fieldType: 'input' },
  { fieldLabel: '作品尺寸', fieldKey: 'work_size', fieldType: 'dimension_group', validationJson: defaultDimensionValidationJson },
  { fieldLabel: '作品时长', fieldKey: 'work_duration', fieldType: 'duration' },
  { fieldLabel: '创作时间', fieldKey: 'creation_time', fieldType: 'date' },
  { fieldLabel: '指导教师', fieldKey: 'teacher_name', fieldType: 'input' },
  { fieldLabel: '指导教师电话', fieldKey: 'teacher_phone', fieldType: 'input' },
  { fieldLabel: '指导老师', fieldKey: 'adviserTeachers', fieldType: 'teacher_group', validationJson: '{"maxItems":3,"memberRole":"teacher"}' },
  { fieldLabel: '指导教师单位', fieldKey: 'teacher_unit', fieldType: 'input' },
  { fieldLabel: '项目负责人', fieldKey: 'leader_name', fieldType: 'input' },
  { fieldLabel: '负责人电话', fieldKey: 'leader_phone', fieldType: 'input' },
  { fieldLabel: '团队成员', fieldKey: 'team_members', fieldType: 'textarea' },
  { fieldLabel: '作品说明', fieldKey: 'work_desc', fieldType: 'textarea' },
  { fieldLabel: '作品简介', fieldKey: 'work_intro', fieldType: 'textarea' },
  { fieldLabel: '上传文件', fieldKey: 'upload_file', fieldType: 'input' },
  { fieldLabel: '审核状态', fieldKey: 'audit_status', fieldType: 'select', options: ['草稿', '待审核', '已驳回', '已通过'] },
  { fieldLabel: '评分状态', fieldKey: 'review_status', fieldType: 'select', options: ['未评分', '评分中', '已评分'] },
  { fieldLabel: '备注', fieldKey: 'remark', fieldType: 'textarea' }
];

const isOptionField = computed(() => optionFieldTypes.includes(fieldForm.value.fieldType || ''));
const isRadioField = computed(() => fieldForm.value.fieldType === 'radio');
const isDurationField = computed(() => fieldForm.value.fieldType === 'duration');
const isTeacherGroupField = computed(() => fieldForm.value.fieldType === 'teacher_group');
const isDimensionGroupField = computed(() => fieldForm.value.fieldType === 'dimension_group');
const isTextLimitField = computed(() => ['input', 'textarea', 'id_card'].includes(fieldForm.value.fieldType || ''));
const isKeywordCountField = computed(
  () => fieldValidationType.value === 'keyword_count' && ['input', 'textarea'].includes(fieldForm.value.fieldType || '')
);
const isCharacterLimitField = computed(() => isTextLimitField.value && !isKeywordCountField.value);
const isNumberLimitField = computed(() => fieldForm.value.fieldType === 'number');
const selectedActivityDeleted = computed(() => currentActivity.value?.delFlag === '1');
const canManageActivity = computed(() => userStore.roles.includes('crehn_admin') || userStore.roles.includes('superadmin'));
const canImportCurrentActivityConfig = computed(() => {
  const requiredPermissions = ['crehn:category:add', 'crehn:field:add', 'crehn:fileRequirement:add'];
  return userStore.permissions.includes('*:*:*') || requiredPermissions.every((permission) => userStore.permissions.includes(permission));
});
const canEditFieldSchema = computed(() => !selectedActivityDeleted.value && checkPermi(['crehn:field:edit']));
const canEditCategory = computed(() => !selectedActivityDeleted.value && checkPermi(['crehn:category:edit']));
const canDeleteCategory = computed(() => canEditCategory.value && checkPermi(['crehn:category:remove']));
const categoryDialogCanDelete = computed(() => canDeleteCategory.value && !!categoryForm.value.id && categoryForm.value.delFlag !== '1');
const categoryDialogCanRestore = computed(() => canEditCategory.value && !!categoryForm.value.id && categoryForm.value.delFlag === '1');
const categoryDialogCanPurge = computed(() => canDeleteCategory.value && !!categoryForm.value.id && categoryForm.value.delFlag === '1');
const configMergeAdditionTotal = computed(
  () =>
    (configMergePreview.value?.addedCategoryCount || 0) +
    (configMergePreview.value?.addedFieldCount || 0) +
    (configMergePreview.value?.addedFileRequirementCount || 0)
);
const configMergeSkippedTotal = computed(
  () =>
    (configMergePreview.value?.skippedCategoryCount || 0) +
    (configMergePreview.value?.skippedFieldCount || 0) +
    (configMergePreview.value?.skippedFileRequirementCount || 0)
);
const formLayoutModeTip = computed(() => {
  if (formLayoutSettings.value.mode === 'double') {
    return '单行文本、下拉、日期、数字等短字段自动两列；多行文本、人员组、尺寸组和选项较多的字段保持整行。';
  }
  if (formLayoutSettings.value.mode === 'custom') {
    return '逐字段选择自动、半行或整行；字段仍按现有排序排列，连续两个半行字段组成一行。';
  }
  return '全部动态字段按整行显示，与现有未配置类别的展示方式一致。';
});
const categoryTableData = computed<ActivityCategoryTreeNode[]>(() => buildActivityCategoryTree(categoryList.value, !categoryDeleted.value));
const categoryGroupNodes = computed<ActivityCategoryTreeNode[]>(() => categoryTableData.value.filter((item) => isCategoryGroup(item)));
const currentCategoryIsGroup = computed(() => isCategoryGroup(currentCategory.value));
const categoryDialogPeopleFieldOptions = computed(() =>
  categoryDialogFieldList.value.filter((item) => item.fieldType === 'teacher_group' && item.delFlag !== '1' && !!item.fieldKey)
);
const activityStatusTextMap: Record<string, string> = {
  draft: '草稿',
  enabled: '启用',
  paused: '暂停',
  ended: '结束',
  deleted: '已删除'
};
const querySummary = computed(() => {
  const name = queryParams.activityName?.trim() || '全部活动';
  const status = activityDeleted.value ? '已删除' : activityStatusTextMap[queryParams.status] || '全部状态';
  return `活动：${name} / 状态：${status}`;
});

const fieldOptionLabel = (field: CategoryFieldSchemaVO) => {
  const label = field.fieldLabel || field.fieldKey || '';
  return field.fieldKey ? `${label} (${field.fieldKey})` : label;
};

const guessPeopleFieldKey = (...keywords: string[]) => {
  const normalizedKeywords = keywords.map((item) => item.toLowerCase());
  const matched = categoryDialogPeopleFieldOptions.value.find((field) => {
    const text = `${field.fieldKey || ''} ${field.fieldLabel || ''}`.toLowerCase();
    return normalizedKeywords.some((keyword) => text.includes(keyword));
  });
  return matched?.fieldKey || '';
};

const ensureWorkshopFieldDefaults = () => {
  if (!workshopRuleSettings.value.studentCountField) {
    workshopRuleSettings.value.studentCountField = guessPeopleFieldKey('student', '学生');
  }
  if (!workshopRuleSettings.value.teacherCountField) {
    workshopRuleSettings.value.teacherCountField = guessPeopleFieldKey('teacher', 'adviser', 'advisor', '指导', '教师', '老师');
  }
};

// ART-REF: FE.ACTIVITY_CONFIG.ACTIVITY_LIST -> components/ActivityListPanel.vue
const getActivityList = async () => {
  loading.value = true;
  const params: any = { ...queryParams };
  if (activityDeleted.value || params.status === 'deleted') {
    params.status = '';
    params.delFlag = '1';
  }
  const res = await listActivity(params);
  activityList.value = res.rows;
  total.value = res.total;
  loading.value = false;
};

const clearSelectedActivityConfig = () => {
  currentActivity.value = undefined;
  currentCategory.value = undefined;
  categoryDeleted.value = false;
  fieldDeleted.value = false;
  requirementDeleted.value = false;
  categoryList.value = [];
  fieldList.value = [];
  requirementList.value = [];
};

const handleActivityDeletedChange = async () => {
  if (activityDeleted.value) {
    queryParams.status = '';
  }
  queryParams.pageNum = 1;
  await getActivityList();
};

const toggleActivityDeleted = async () => {
  activityDeleted.value = !activityDeleted.value;
  await handleActivityDeletedChange();
};

const loadCategoryList = async () => {
  if (!currentActivity.value?.id) return;
  const { data } = await listCategory(currentActivity.value.id, { deleted: categoryDeleted.value });
  categoryList.value = data;
};

const handleCategoryDeletedChange = async () => {
  currentCategory.value = undefined;
  fieldDeleted.value = false;
  requirementDeleted.value = false;
  fieldList.value = [];
  requirementList.value = [];
  await loadCategoryList();
};

const toggleCategoryDeleted = async () => {
  if (!currentActivity.value?.id) return;
  categoryDeleted.value = !categoryDeleted.value;
  await handleCategoryDeletedChange();
};

const loadConfig = async () => {
  if (!currentCategory.value?.id || currentCategory.value.virtualGroup || currentCategoryIsGroup.value || categoryDeleted.value) {
    fieldList.value = [];
    requirementList.value = [];
    return;
  }
  const fieldRes = await listField(currentCategory.value.id, { deleted: fieldDeleted.value });
  const requirementRes = await listFileRequirement(currentCategory.value.id, { deleted: requirementDeleted.value });
  fieldList.value = fieldRes.data;
  requirementList.value = requirementRes.data;
};

const toggleFieldDeleted = async () => {
  if (!currentCategory.value?.id || categoryDeleted.value) return;
  fieldDeleted.value = !fieldDeleted.value;
  await loadConfig();
};

const toggleRequirementDeleted = async () => {
  if (!currentCategory.value?.id || categoryDeleted.value) return;
  requirementDeleted.value = !requirementDeleted.value;
  await loadConfig();
};

const handleRequirementMoreCommand = async (command: string) => {
  if (selectedActivityDeleted.value && command !== 'deleted') {
    proxy?.$modal.msgWarning('已删除活动下的配置为只读，请先恢复活动');
    return;
  }
  if (command === 'add') {
    openRequirement();
    return;
  }
  if (command === 'deleted') {
    await toggleRequirementDeleted();
  }
};

const selectActivity = async (row: ActivityVO) => {
  currentActivity.value = row;
  currentCategory.value = undefined;
  categoryDeleted.value = false;
  fieldDeleted.value = false;
  requirementDeleted.value = false;
  fieldList.value = [];
  requirementList.value = [];
  await loadCategoryList();
};

const ensureActivityManager = () => {
  if (canManageActivity.value) return true;
  proxy?.$modal.msgWarning('仅艺术评审配置管理员或平台超级管理员可管理活动');
  return false;
};

const openActivityManager = async () => {
  if (!ensureActivityManager()) return;
  activityManagerVisible.value = true;
  await getActivityList();
};

const switchManagedActivity = async (row: ActivityVO) => {
  if (!ensureActivityManager() || !row.id) return;
  await selectActivity(row);
  activityManagerVisible.value = false;
  proxy?.$modal.msgSuccess(`已切换到「${row.activityName || '所选活动'}」`);
};

const selectCategory = async (row: ActivityCategoryVO) => {
  currentCategory.value = row as ActivityCategoryTreeNode;
  fieldDeleted.value = false;
  requirementDeleted.value = false;
  await loadConfig();
};

const handleQuery = () => {
  queryParams.pageNum = 1;
  getActivityList();
};

const resetQuery = () => {
  queryParams.activityName = '';
  queryParams.status = '';
  activityDeleted.value = false;
  handleQuery();
};

const openActivity = (row?: ActivityVO) => {
  if (!ensureActivityManager()) return;
  activityForm.value = { status: 'draft', ...row };
  activityDialog.title = row?.id ? '修改活动' : '新增活动';
  activityDialog.visible = true;
};

const submitActivity = async () => {
  if (!ensureActivityManager()) return;
  const editingId = activityForm.value.id;
  activityForm.value.id ? await updateActivity(activityForm.value) : await addActivity(activityForm.value);
  proxy?.$modal.msgSuccess('操作成功');
  activityDialog.visible = false;
  await getActivityList();
  if (editingId && String(currentActivity.value?.id) === String(editingId)) {
    const refreshed = activityList.value.find((item) => String(item.id) === String(editingId));
    if (refreshed) await selectActivity(refreshed);
  }
};

const removeActivity = async (row: ActivityVO) => {
  if (!ensureActivityManager()) return;
  await openActivityDeleteCheck(row, 'delete');
};

const restoreActivityRow = async (row: ActivityVO) => {
  if (!ensureActivityManager()) return;
  await proxy?.$modal.confirm(`确认恢复活动「${row.activityName}」？`);
  await restoreActivity(row.id!);
  proxy?.$modal.msgSuccess('恢复成功');
  await getActivityList();
};

const preferredActivityDeleteTab = (check: ActivityDeleteCheckVO) => {
  if (check.projectCount) return 'project';
  if (check.fileCount) return 'file';
  if (check.referenceCount) return 'reference';
  return 'category';
};

const loadActivityDeleteCheck = async (activityId: string | number) => {
  activityDeleteDialog.loading = true;
  try {
    const { data } = activityDeleteDialog.mode === 'purge' ? await checkActivityPurge(activityId) : await checkActivityDelete(activityId);
    activityDeleteCheck.value = data;
    activityDeleteTab.value = preferredActivityDeleteTab(data);
  } finally {
    activityDeleteDialog.loading = false;
  }
};

const openActivityDeleteCheck = async (row: ActivityVO, mode: DeleteCheckMode) => {
  if (!ensureActivityManager() || !row.id) return;
  activityDeleteDialog.mode = mode;
  activityDeleteCheck.value = undefined;
  activityDeleteDialog.visible = true;
  try {
    await loadActivityDeleteCheck(row.id);
  } catch (error) {
    activityDeleteDialog.visible = false;
    throw error;
  }
};

const openActivityPurgeCheck = async (row: ActivityVO) => {
  await openActivityDeleteCheck(row, 'purge');
};

const refreshActivityDeleteCheck = async () => {
  if (!activityDeleteCheck.value?.activityId) return;
  await loadActivityDeleteCheck(activityDeleteCheck.value.activityId);
};

const confirmActivityDelete = async () => {
  if (!ensureActivityManager()) return;
  const check = activityDeleteCheck.value;
  if (!check?.activityId || !check.canDelete) return;
  const permanent = activityDeleteDialog.mode === 'purge';
  const configSummary = `${check.categoryCount || 0} 个类别、${check.fieldSchemaCount || 0} 个字段、${check.fileRequirementCount || 0} 个附件要求、${check.schoolScopeCount || 0} 条学校范围`;
  await proxy?.$modal.confirm(
    permanent
      ? `确认永久删除活动「${check.activityName}」（ID ${check.activityId}）？此操作不可恢复，并会物理清理活动自身的 ${configSummary}。`
      : `确认删除活动「${check.activityName}」（ID ${check.activityId}）？活动及其 ${configSummary} 将进入只读的“已删活动”，以后可以恢复。`
  );
  activityDeleteDialog.submitting = true;
  try {
    if (permanent) {
      await purgeActivity(check.activityId);
      proxy?.$modal.msgSuccess('活动已永久删除');
    } else {
      await delActivity(check.activityId);
      proxy?.$modal.msgSuccess('活动已删除，可在“已删活动”中恢复');
    }
    activityDeleteDialog.visible = false;
    activityDeleteCheck.value = undefined;
    if (String(currentActivity.value?.id) === String(check.activityId)) {
      clearSelectedActivityConfig();
    }
    await getActivityList();
  } finally {
    activityDeleteDialog.submitting = false;
  }
};

const handleActivityConfigImport = async (file: File) => {
  if (!ensureActivityManager()) return;
  if (!/\.(json|xlsx|xls)$/i.test(file.name)) {
    proxy?.$modal.msgError('仅支持 JSON、Excel(.xlsx/.xls) 活动配置包');
    return;
  }
  await proxy?.$modal.confirm('导入会先校验配置包，校验通过后作为新活动创建，不会覆盖当前活动。确认继续？');
  activityConfigImporting.value = true;
  try {
    const { data } = await importActivityConfig(file);
    proxy?.$modal.msgSuccess(
      `导入成功：${data.activityName || '新活动'}，类别 ${data.categoryCount || 0} 个，字段 ${data.fieldCount || 0} 个，附件要求 ${data.fileRequirementCount || 0} 个`
    );
    await getActivityList();
    const imported = activityList.value.find((item) => String(item.id) === String(data.activityId));
    if (imported) {
      await selectActivity(imported);
    }
  } finally {
    activityConfigImporting.value = false;
  }
};

const exportCurrentActivityBackup = async () => {
  currentConfigExporting.value = true;
  try {
    await exportActivityConfig('json');
  } finally {
    currentConfigExporting.value = false;
  }
};

const previewCurrentActivityConfigImport = async (file: File) => {
  if (!currentActivity.value?.id || selectedActivityDeleted.value) {
    proxy?.$modal.msgWarning(selectedActivityDeleted.value ? '已删除活动不能导入配置，请先恢复活动' : '请先选择活动');
    return;
  }
  if (!canImportCurrentActivityConfig.value) {
    proxy?.$modal.msgError('导入配置需要类别、字段和附件要求的新增权限');
    return;
  }
  if (!/\.(json|xlsx|xls)$/i.test(file.name)) {
    proxy?.$modal.msgError('仅支持 JSON、Excel(.xlsx/.xls) 活动配置包');
    return;
  }

  currentConfigImporting.value = true;
  try {
    const { data } = await previewActivityConfigMerge(currentActivity.value.id, file);
    configMergeFile.value = file;
    configMergePreview.value = data;
    configMergeTab.value = data.hasAdditions ? 'additions' : 'skipped';
    configMergeDialog.visible = true;
  } finally {
    currentConfigImporting.value = false;
  }
};

const confirmCurrentActivityConfigImport = async () => {
  const file = configMergeFile.value;
  const targetActivityId = configMergePreview.value?.activityId;
  if (!file || !targetActivityId || !configMergeAdditionTotal.value) return;

  configMergeDialog.submitting = true;
  try {
    const { data } = await mergeActivityConfig(targetActivityId, file);
    configMergePreview.value = data;
    configMergeDialog.visible = false;
    configMergeFile.value = undefined;
    proxy?.$modal.msgSuccess(
      `已新增：类别 ${data.addedCategoryCount || 0} 个，字段 ${data.addedFieldCount || 0} 个，附件要求 ${data.addedFileRequirementCount || 0} 个；跳过 ${
        (data.skippedCategoryCount || 0) + (data.skippedFieldCount || 0) + (data.skippedFileRequirementCount || 0)
      } 项重复配置`
    );

    if (String(currentActivity.value?.id) === String(targetActivityId)) {
      const selectedCategoryId = currentCategory.value?.id;
      await loadCategoryList();
      if (selectedCategoryId) {
        currentCategory.value = categoryList.value.find((item) => String(item.id) === String(selectedCategoryId));
      }
      await loadConfig();
    }
  } finally {
    configMergeDialog.submitting = false;
  }
};

const exportActivityConfig = async (command: string | number | object) => {
  if (!currentActivity.value?.id || selectedActivityDeleted.value) {
    proxy?.$modal.msgWarning(selectedActivityDeleted.value ? '已删除活动不提供配置导出，请先恢复活动' : '请先选择活动');
    return;
  }
  const type = command === 'excel' ? 'excel' : 'json';
  const data = type === 'json' ? await exportActivityConfigJson(currentActivity.value.id) : await exportActivityConfigExcel(currentActivity.value.id);
  const ext = type === 'json' ? 'json' : 'xlsx';
  saveBlob(data as unknown as BlobPart, `${currentActivity.value.activityName || '活动'}-活动配置包.${ext}`);
};

const saveBlob = (data: BlobPart, filename: string) => {
  const blob = data instanceof Blob ? data : new Blob([data]);
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(link.href);
};

const validatorDefaultName = (validatorType?: string) => validatorTypeOptions.find((item) => item.value === validatorType)?.defaultName || '';

const handleCategoryValidatorChange = () => {
  const type = categoryRuleForm.value.validatorType;
  categoryRuleForm.value.validatorLabel = validatorDefaultName(type);
  if (type === 'workshop') {
    workshopRuleSettings.value = {
      ...defaultWorkshopRuleSettings(),
      ...workshopRuleSettings.value
    };
    ensureWorkshopFieldDefaults();
  }
};

const applyCategoryRuleDialogDraft = (draft: CategoryRuleDialogDraft) => {
  categoryForm.value = { ...draft.categoryForm };
  categoryDialogNodeType.value = draft.nodeType;
  categoryDialogMenuVisible.value = draft.menuVisible;
  categoryParentKey.value = draft.categoryParentKey;
  categoryRuleForm.value = { ...draft.categoryRuleForm };
  workshopRuleSettings.value = { ...draft.workshopRuleSettings };
};

const handleCategoryRuleTemplateChange = (draft: CategoryRuleDialogDraft) => {
  applyCategoryRuleDialogDraft(draft);
  applyCategoryRuleTemplate(draft.categoryRuleForm.templateCode);
};

const handleCategoryRuleValidatorChange = (draft: CategoryRuleDialogDraft) => {
  applyCategoryRuleDialogDraft(draft);
  handleCategoryValidatorChange();
};

const handleCategoryRuleDialogConfirm = async (draft: CategoryRuleDialogDraft) => {
  applyCategoryRuleDialogDraft(draft);
  await submitCategory();
};

const handleCategoryRuleDialogDelete = async (draft: CategoryRuleDialogDraft) => {
  applyCategoryRuleDialogDraft(draft);
  if (!categoryForm.value.id) return;
  await removeCategory(categoryForm.value);
  categoryDialog.visible = false;
};

const handleCategoryRuleDialogRestore = async (draft: CategoryRuleDialogDraft) => {
  applyCategoryRuleDialogDraft(draft);
  if (!categoryForm.value.id) return;
  await restoreCategoryRow(categoryForm.value);
  categoryDialog.visible = false;
};

const handleCategoryRuleDialogPurge = async (draft: CategoryRuleDialogDraft) => {
  applyCategoryRuleDialogDraft(draft);
  if (!categoryForm.value.id) return;
  categoryDialog.visible = false;
  await openCategoryPurgeCheck(categoryForm.value);
};

function defaultWorkshopRuleSettings(): WorkshopRuleSettings {
  return {
    validateStudentCount: true,
    studentCountField: '',
    minStudentCount: 7,
    maxStudentCount: 9,
    validateTeacherCount: true,
    teacherCountField: '',
    minTeacherCount: 1,
    maxTeacherCount: 3,
    validateTotalMemberCount: true,
    maxTotalMemberCount: 12,
    requireSingleProjectPerSchool: true,
    requireNoPreviousAwardCommitment: true,
    validateVideoExt: true,
    videoAllowedExt: 'mp4,mpg,mpeg',
    validateVideoDuration: true,
    maxVideoDurationSeconds: 480
  };
}

const firstRuleListValue = (value: any) => {
  const list = parseRuleStringList(value);
  return list[0] || '';
};

const parseRuleStringList = (value: any): string[] => {
  if (value === undefined || value === null || value === '') return [];
  if (Array.isArray(value)) return value.map((item) => String(item || '').trim()).filter(Boolean);
  return String(value)
    .split(/[,，;；、\s]+/)
    .map((item) => item.trim())
    .filter(Boolean);
};

const ruleNumber = (value: any, defaultValue: number) => {
  if (value === undefined || value === null || value === '') return defaultValue;
  const number = Number(value);
  return Number.isFinite(number) ? number : defaultValue;
};

const parseWorkshopRuleSettings = (rule: any): WorkshopRuleSettings => {
  const settings = defaultWorkshopRuleSettings();
  const memberCountFields = rule?.memberCountFields || {};
  return {
    validateStudentCount: rule?.validateStudentCount !== false,
    studentCountField: firstRuleListValue(rule?.studentCountFields) || firstRuleListValue(memberCountFields.student),
    minStudentCount: ruleNumber(rule?.minStudentCount, settings.minStudentCount),
    maxStudentCount: ruleNumber(rule?.maxStudentCount, settings.maxStudentCount),
    validateTeacherCount: rule?.validateTeacherCount !== false,
    teacherCountField: firstRuleListValue(rule?.teacherCountFields) || firstRuleListValue(memberCountFields.teacher),
    minTeacherCount: ruleNumber(rule?.minTeacherCount, settings.minTeacherCount),
    maxTeacherCount: ruleNumber(rule?.maxTeacherCount, settings.maxTeacherCount),
    validateTotalMemberCount: rule?.validateTotalMemberCount !== false,
    maxTotalMemberCount: ruleNumber(rule?.maxTotalMemberCount, settings.maxTotalMemberCount),
    requireSingleProjectPerSchool: rule?.requireSingleProjectPerSchool !== false,
    requireNoPreviousAwardCommitment: rule?.requireNoPreviousAwardCommitment !== false,
    validateVideoExt: rule?.validateVideoExt !== false,
    videoAllowedExt: parseRuleStringList(rule?.videoAllowedExt).join(',') || settings.videoAllowedExt,
    validateVideoDuration: rule?.validateVideoDuration !== false,
    maxVideoDurationSeconds: ruleNumber(rule?.maxVideoDurationSeconds, settings.maxVideoDurationSeconds)
  };
};

const workshopRuleKeys = [
  'validateStudentCount',
  'studentCountFields',
  'validateTeacherCount',
  'teacherCountFields',
  'memberCountFields',
  'minStudentCount',
  'maxStudentCount',
  'minTeacherCount',
  'maxTeacherCount',
  'validateTotalMemberCount',
  'maxTotalMemberCount',
  'requireSingleProjectPerSchool',
  'requireNoPreviousAwardCommitment',
  'validateVideoExt',
  'videoAllowedExt',
  'validateVideoDuration',
  'maxVideoDurationSeconds'
];

const applyWorkshopRuleToJson = (rule: any, settings?: WorkshopRuleSettings) => {
  workshopRuleKeys.forEach((key) => delete rule[key]);
  if (!settings) return;
  rule.validateStudentCount = !!settings.validateStudentCount;
  rule.studentCountFields = settings.studentCountField ? [settings.studentCountField] : [];
  rule.minStudentCount = Number(settings.minStudentCount || 0);
  rule.maxStudentCount = Number(settings.maxStudentCount || 0);
  rule.validateTeacherCount = !!settings.validateTeacherCount;
  rule.teacherCountFields = settings.teacherCountField ? [settings.teacherCountField] : [];
  rule.minTeacherCount = Number(settings.minTeacherCount || 0);
  rule.maxTeacherCount = Number(settings.maxTeacherCount || 0);
  rule.validateTotalMemberCount = !!settings.validateTotalMemberCount;
  rule.maxTotalMemberCount = Number(settings.maxTotalMemberCount || 0);
  rule.requireSingleProjectPerSchool = !!settings.requireSingleProjectPerSchool;
  rule.requireNoPreviousAwardCommitment = !!settings.requireNoPreviousAwardCommitment;
  rule.validateVideoExt = !!settings.validateVideoExt;
  rule.videoAllowedExt = parseRuleStringList(settings.videoAllowedExt || 'mp4,mpg,mpeg');
  rule.validateVideoDuration = !!settings.validateVideoDuration;
  rule.maxVideoDurationSeconds = Number(settings.maxVideoDurationSeconds || 0);
};

// ART-REF: FE.ACTIVITY_CONFIG.CATEGORY_RULE -> components/CategoryRuleDialog.vue
const openCategory = async (
  row?: ActivityCategoryTreeNode,
  requestedNodeType: typeof CATEGORY_NODE_TYPE_GROUP | typeof CATEGORY_NODE_TYPE_CATEGORY = CATEGORY_NODE_TYPE_CATEGORY
) => {
  if (selectedActivityDeleted.value) {
    proxy?.$modal.msgWarning('已删除活动下的配置为只读，请先恢复活动');
    return;
  }
  const nodeType = row ? (isCategoryGroup(row) ? CATEGORY_NODE_TYPE_GROUP : CATEGORY_NODE_TYPE_CATEGORY) : requestedNodeType;
  categoryDialogNodeType.value = nodeType;
  categoryDialogTypeLocked.value = !!row;
  categoryDialogMenuVisible.value = row ? isCategoryMenuVisible(row) : true;
  const baseRow = row
    ? {
        ...row,
        id: row.virtualGroup ? undefined : row.id,
        children: undefined,
        virtualGroup: undefined,
        categoryCode: row.virtualGroup ? uniqueNewCategoryCode('group') : row.categoryCode
      }
    : {};
  categoryForm.value = { activityId: currentActivity.value?.id, parentId: 0, enabled: true, sortOrder: 0, ...baseRow };
  const parent = row && !isCategoryGroup(row) ? findCategoryParentGroup(row, categoryList.value) : undefined;
  if (parent?.id !== undefined && parent?.id !== null) {
    categoryParentKey.value = categoryGroupOptionKey(parent);
  } else if (row && isLegacyGroupedCategory(row)) {
    categoryParentKey.value = `legacy:${String(row.categoryGroup || '').trim()}`;
  } else {
    categoryParentKey.value = '';
  }
  const rule = parseCategoryRule(row?.ruleJson);
  const projectName = (rule as any).projectName || {};
  categoryDialogFieldList.value = [];
  if (row?.id && !row.virtualGroup && nodeType === CATEGORY_NODE_TYPE_CATEGORY) {
    if (String(row.id) === String(currentCategory.value?.id)) {
      categoryDialogFieldList.value = fieldList.value;
    } else {
      try {
        const fieldRes = await listField(row.id, { deleted: false });
        categoryDialogFieldList.value = fieldRes.data || [];
      } catch {
        categoryDialogFieldList.value = [];
      }
    }
  }
  workshopRuleSettings.value = parseWorkshopRuleSettings(rule);
  if ((rule as any).validatorType === 'workshop') {
    ensureWorkshopFieldDefaults();
  }
  categoryRuleForm.value = {
    templateCode: (rule as any).templateCode || '',
    validatorType: (rule as any).validatorType || '',
    validatorLabel: (rule as any).validatorLabel || validatorDefaultName((rule as any).validatorType),
    projectNameLabel: projectName.label || '',
    projectNameRequired: projectName.required ?? true,
    projectNameWidthMode: normalizeInputWidthMode(projectName.inputWidthMode),
    projectNameWidthCustom: String(projectName.inputWidthCustom || ''),
    mediaTechnicalCheckMode: (rule as any).mediaTechnicalCheckMode || '',
    extensionModule: !!(rule as any).extensionModule,
    reportingTipTitle: (rule as any).reportingTipTitle || '',
    reportingTipText: (rule as any).reportingTipText || ''
  };
  categoryDialog.title = row
    ? nodeType === CATEGORY_NODE_TYPE_GROUP
      ? '配置大类'
      : '修改类别'
    : nodeType === CATEGORY_NODE_TYPE_GROUP
      ? '新增大类'
      : '新增类别';
  categoryDialog.visible = true;
};

const submitCategory = async () => {
  if (selectedActivityDeleted.value) {
    proxy?.$modal.msgWarning('已删除活动下的配置为只读，请先恢复活动');
    return;
  }
  if (!String(categoryForm.value.categoryName || '').trim()) {
    proxy?.$modal.msgError(categoryDialogNodeType.value === CATEGORY_NODE_TYPE_GROUP ? '请输入大类名称' : '请输入类别名称');
    return;
  }
  if (!categoryForm.value.categoryCode || !/^[a-z][a-z0-9_]{1,63}$/.test(categoryForm.value.categoryCode)) {
    proxy?.$modal.msgError('类别编码只能使用小写英文、数字、下划线，并且必须以英文开头');
    return;
  }
  if (String(categoryForm.value.ruleJson || '').trim()) {
    try {
      const parsed = JSON.parse(String(categoryForm.value.ruleJson));
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        proxy?.$modal.msgError('高级规则JSON必须是合法 JSON 对象');
        return;
      }
    } catch {
      proxy?.$modal.msgError('高级规则JSON格式不正确');
      return;
    }
  }
  if (categoryDialogNodeType.value === CATEGORY_NODE_TYPE_CATEGORY && categoryRuleForm.value.validatorType === 'workshop') {
    ensureWorkshopFieldDefaults();
    const settings = workshopRuleSettings.value;
    if (Number(settings.minStudentCount || 0) > Number(settings.maxStudentCount || 0)) {
      proxy?.$modal.msgError('学生最多人数不能小于最少人数');
      return;
    }
    if (Number(settings.minTeacherCount || 0) > Number(settings.maxTeacherCount || 0)) {
      proxy?.$modal.msgError('教师最多人数不能小于最少人数');
      return;
    }
    if (settings.validateStudentCount && !settings.studentCountField && categoryDialogPeopleFieldOptions.value.length) {
      proxy?.$modal.msgError('请选择学生人数统计字段');
      return;
    }
    if (settings.validateTeacherCount && !settings.teacherCountField && categoryDialogPeopleFieldOptions.value.length) {
      proxy?.$modal.msgError('请选择教师人数统计字段');
      return;
    }
  }
  if (categoryDialogNodeType.value === CATEGORY_NODE_TYPE_GROUP) {
    categoryForm.value.parentId = 0;
    categoryForm.value.categoryGroup = String(categoryForm.value.categoryName || '').trim();
  } else {
    const parent = resolveCategoryParentSelection(categoryParentKey.value);
    categoryForm.value.parentId = parent?.id || 0;
    categoryForm.value.categoryGroup = parent?.categoryName || parent?.legacyName || '';
  }
  const rule = parseJsonObject(categoryForm.value.ruleJson);
  const form = categoryRuleForm.value;
  if (categoryDialogNodeType.value === CATEGORY_NODE_TYPE_CATEGORY && form.templateCode) {
    (rule as any).templateCode = form.templateCode;
  } else {
    delete (rule as any).templateCode;
  }
  if (categoryDialogNodeType.value === CATEGORY_NODE_TYPE_CATEGORY && form.validatorType) {
    (rule as any).validatorType = form.validatorType;
  } else {
    delete (rule as any).validatorType;
  }
  if (categoryDialogNodeType.value === CATEGORY_NODE_TYPE_CATEGORY && form.validatorLabel && form.validatorType && form.validatorType !== 'none') {
    (rule as any).validatorLabel = form.validatorLabel;
  } else {
    delete (rule as any).validatorLabel;
  }
  const projectNameLabel = String(form.projectNameLabel || '').trim();
  const projectNameWidthMode = normalizeInputWidthMode(form.projectNameWidthMode);
  const projectNameWidthCustom = String(form.projectNameWidthCustom || '').trim();
  if (projectNameWidthMode === 'custom' && !validCustomInputWidth(projectNameWidthCustom)) {
    proxy?.$modal.msgError('主名称字段的自定义宽度请填写 80px-960px 或 10%-100%');
    return;
  }
  const hasProjectNameConfig = projectNameLabel || projectNameWidthMode !== 'default' || form.projectNameRequired === false;
  if (categoryDialogNodeType.value === CATEGORY_NODE_TYPE_CATEGORY && hasProjectNameConfig) {
    const projectNameRule: Record<string, any> = {
      label: projectNameLabel || '项目名称',
      required: !!form.projectNameRequired
    };
    if (projectNameWidthMode !== 'default') {
      projectNameRule.inputWidthMode = projectNameWidthMode;
    }
    if (projectNameWidthMode === 'custom') {
      projectNameRule.inputWidthCustom = projectNameWidthCustom;
    }
    (rule as any).projectName = projectNameRule;
  } else {
    delete (rule as any).projectName;
  }
  if (categoryDialogNodeType.value === CATEGORY_NODE_TYPE_CATEGORY && form.mediaTechnicalCheckMode) {
    (rule as any).mediaTechnicalCheckMode = form.mediaTechnicalCheckMode;
  } else {
    delete (rule as any).mediaTechnicalCheckMode;
  }
  if (categoryDialogNodeType.value === CATEGORY_NODE_TYPE_CATEGORY && form.extensionModule) {
    (rule as any).extensionModule = true;
  } else {
    delete (rule as any).extensionModule;
  }
  const reportingTipTitle = String(form.reportingTipTitle || '').trim();
  if (categoryDialogNodeType.value === CATEGORY_NODE_TYPE_CATEGORY && reportingTipTitle) {
    (rule as any).reportingTipTitle = reportingTipTitle;
  } else {
    delete (rule as any).reportingTipTitle;
  }
  const reportingTipText = String(form.reportingTipText || '').trim();
  if (categoryDialogNodeType.value === CATEGORY_NODE_TYPE_CATEGORY && reportingTipText) {
    (rule as any).reportingTipText = reportingTipText;
  } else {
    delete (rule as any).reportingTipText;
  }
  applyWorkshopRuleToJson(
    rule,
    categoryDialogNodeType.value === CATEGORY_NODE_TYPE_CATEGORY && form.validatorType === 'workshop' ? workshopRuleSettings.value : undefined
  );
  categoryForm.value.ruleJson = updateCategoryRuleMeta(
    Object.keys(rule).length ? JSON.stringify(rule) : '',
    categoryDialogNodeType.value,
    categoryDialogMenuVisible.value
  );
  categoryForm.value.id ? await updateCategory(categoryForm.value) : await addCategory(categoryForm.value);
  proxy?.$modal.msgSuccess('操作成功');
  categoryDialog.visible = false;
  await loadCategoryList();
};

const categoryGroupOptionKey = (category: ActivityCategoryTreeNode) =>
  category.virtualGroup ? `legacy:${String(category.categoryName || '').trim()}` : `id:${String(category.id || '')}`;

const categoryGroupOptionLabel = (category: ActivityCategoryTreeNode) =>
  category.virtualGroup ? `${category.categoryName || '未命名大类'}（旧分组，保存后可独立控制）` : String(category.categoryName || '未命名大类');

const resolveCategoryParentSelection = (key?: string) => {
  const value = String(key || '').trim();
  if (!value) return undefined;
  if (value.startsWith('legacy:')) {
    return { legacyName: value.slice('legacy:'.length) };
  }
  if (!value.startsWith('id:')) return undefined;
  const id = value.slice('id:'.length);
  const parent = categoryGroupNodes.value.find((item) => !item.virtualGroup && String(item.id) === id);
  return parent ? { id: parent.id, categoryName: parent.categoryName } : undefined;
};

const uniqueNewCategoryCode = (prefix: string) => {
  const used = new Set(categoryList.value.map((item) => String(item.categoryCode || '').trim()));
  let index = 1;
  let code = `${prefix}_${index}`;
  while (used.has(code)) {
    index += 1;
    code = `${prefix}_${index}`;
  }
  return code;
};

const updateCategorySwitch = async (row: ActivityCategoryTreeNode, patch: Partial<ActivityCategoryVO>) => {
  if (!row.id || row.virtualGroup) return;
  const category = { ...row };
  delete category.children;
  delete category.virtualGroup;
  await updateCategory({ ...category, ...patch });
  await loadCategoryList();
  if (String(currentCategory.value?.id) === String(row.id)) {
    currentCategory.value = categoryList.value.find((item) => String(item.id) === String(row.id));
  }
};

const sortByCategoryOrder = (a: ActivityCategoryVO, b: ActivityCategoryVO) => {
  const aOrder = Number(a.sortOrder || 0);
  const bOrder = Number(b.sortOrder || 0);
  const orderDiff = aOrder - bOrder;
  if (orderDiff !== 0) return orderDiff;
  const aId = Number(a.id);
  const bId = Number(b.id);
  if (Number.isFinite(aId) && Number.isFinite(bId)) {
    return aId - bId;
  }
  return String(a.id || '').localeCompare(String(b.id || ''), 'zh-CN');
};

const isRootCategory = (row?: ActivityCategoryVO) => {
  const parentId = row?.parentId;
  return parentId === undefined || parentId === null || parentId === '' || String(parentId) === '0';
};

const getCategorySortScope = (row: ActivityCategoryVO, item: ActivityCategoryVO) => {
  const rowCategory = row as ActivityCategoryTreeNode;
  const itemCategory = item as ActivityCategoryTreeNode;
  const rowIsGroup = isCategoryGroup(rowCategory);
  const itemIsGroup = isCategoryGroup(itemCategory);

  if (rowIsGroup) {
    return itemIsGroup && isRootCategory(itemCategory);
  }

  const rowIsLegacy = isLegacyGroupedCategory(rowCategory) && !rowIsGroup;
  if (rowIsLegacy) {
    const rowGroup = String(rowCategory.categoryGroup || '').trim();
    return !itemIsGroup && isLegacyGroupedCategory(itemCategory) && String(itemCategory.categoryGroup || '').trim() === rowGroup;
  }

  if (isRootCategory(rowCategory)) {
    return !itemIsGroup && isRootCategory(itemCategory) && !isLegacyGroupedCategory(itemCategory);
  }

  return (
    !itemIsGroup &&
    String(itemCategory.parentId || 0) === String(rowCategory.parentId || 0) &&
    !isLegacyGroupedCategory(itemCategory)
  );
};

const getCategorySortSiblings = (row: ActivityCategoryTreeNode) => {
  if (!row.id || row.virtualGroup) return [];
  return categoryList.value.filter((item) => String(item.id) !== '' && getCategorySortScope(row, item));
};

const toCategoryPayload = (row: ActivityCategoryTreeNode): ActivityCategoryVO => {
  const payload: ActivityCategoryVO = { ...(row as ActivityCategoryVO) };
  delete (payload as ActivityCategoryVO & { children?: unknown }).children;
  delete (payload as ActivityCategoryVO & { virtualGroup?: unknown }).virtualGroup;
  return payload;
};

const syncCurrentCategoryAfterListReload = () => {
  if (!currentCategory.value?.id) return;
  const current = categoryList.value.find((item) => String(item.id) === String(currentCategory.value?.id));
  currentCategory.value = current;
};

const moveCategorySort = async (row: ActivityCategoryTreeNode, direction: SortDirection) => {
  if (!canEditCategory.value || categoryDeleted.value || row.virtualGroup || !row.id) return;

  const siblings = getCategorySortSiblings(row);
  if (siblings.length < 2) return;

  const ordered = [...siblings].sort(sortByCategoryOrder);
  const currentIndex = ordered.findIndex((item) => String(item.id) === String(row.id));
  if (currentIndex < 0) return;

  const targetIndex = direction === 'up' ? currentIndex - 1 : currentIndex + 1;
  if (targetIndex < 0 || targetIndex >= ordered.length) return;

  const target = ordered[targetIndex];
  if (!target?.id) return;

  const currentCategoryPayload = toCategoryPayload(row);
  const targetPayload = toCategoryPayload(target as ActivityCategoryTreeNode);
  const currentSortOrder = Number(currentCategoryPayload.sortOrder || 0);
  const targetSortOrder = Number(targetPayload.sortOrder || 0);

  await updateCategory({ ...currentCategoryPayload, sortOrder: targetSortOrder });
  await updateCategory({ ...targetPayload, sortOrder: currentSortOrder });
  await loadCategoryList();
  syncCurrentCategoryAfterListReload();
  proxy?.$modal.msgSuccess('排序已更新');
};

const sortByFieldOrder = (a: CategoryFieldSchemaVO, b: CategoryFieldSchemaVO) => {
  const aOrder = Number(a.sortOrder || 0);
  const bOrder = Number(b.sortOrder || 0);
  const orderDiff = aOrder - bOrder;
  if (orderDiff !== 0) return orderDiff;
  const aId = Number(a.id);
  const bId = Number(b.id);
  if (Number.isFinite(aId) && Number.isFinite(bId)) {
    return aId - bId;
  }
  return String(a.id || '').localeCompare(String(b.id || ''), 'zh-CN');
};

const moveFieldSort = async (row: CategoryFieldSchemaVO, direction: SortDirection) => {
  if (!canEditFieldSchema.value || categoryDeleted.value || fieldDeleted.value || !row.id) return;
  const siblings = [...fieldList.value].sort(sortByFieldOrder);
  const currentIndex = siblings.findIndex((item) => String(item.id) === String(row.id));
  if (currentIndex < 0) return;
  const targetIndex = direction === 'up' ? currentIndex - 1 : currentIndex + 1;
  if (targetIndex < 0 || targetIndex >= siblings.length) return;
  const target = siblings[targetIndex];
  if (!target?.id) return;

  await updateField({ ...row, sortOrder: Number(target.sortOrder || 0) });
  await updateField({ ...target, sortOrder: Number(row.sortOrder || 0) });
  await loadConfig();
  proxy?.$modal.msgSuccess('排序已更新');
};

const toggleCategoryMenuVisible = async (row: ActivityCategoryTreeNode, visible: boolean | string | number) => {
  if (!canEditCategory.value) return;
  const nextVisible = Boolean(visible);
  await updateCategorySwitch(row, {
    ruleJson: updateCategoryRuleMeta(row.ruleJson, isCategoryGroup(row) ? CATEGORY_NODE_TYPE_GROUP : CATEGORY_NODE_TYPE_CATEGORY, nextVisible)
  });
  proxy?.$modal.msgSuccess(nextVisible ? '菜单已显示' : '菜单已隐藏');
};

const toggleCategoryEnabled = async (row: ActivityCategoryTreeNode, enabled: boolean | string | number) => {
  if (!canEditCategory.value) return;
  const nextEnabled = Boolean(enabled);
  await updateCategorySwitch(row, { enabled: nextEnabled });
  proxy?.$modal.msgSuccess(nextEnabled ? '业务已启用' : '业务已停用');
};

const openMenuStyleDialog = () => {
  if (!currentActivity.value?.id || selectedActivityDeleted.value) return;
  menuStyleDialogRef.value?.open();
};

const saveMenuStyleCategories = async (changedCategories: ActivityCategoryVO[]) => {
  if (!changedCategories.length) {
    proxy?.$modal.msgSuccess('没有需要保存的变更');
    return;
  }
  for (const category of changedCategories) {
    await updateCategory(category);
  }
  proxy?.$modal.msgSuccess('保存成功，刷新页面后生效');
  await loadCategoryList();
};

const applyCategoryRuleTemplate = (templateCode?: string) => {
  const template = categoryRuleTemplateOptions.find((item) => item.templateCode === templateCode);
  if (!template) return;
  if (!categoryForm.value.id) {
    categoryForm.value.categoryCode = template.categoryCode;
    categoryForm.value.categoryName = template.categoryName;
    categoryForm.value.categoryGroup = template.categoryGroup;
    categoryForm.value.enabled = template.enabled ?? true;
    const parent = categoryGroupNodes.value.find((item) => String(item.categoryName || '').trim() === String(template.categoryGroup || '').trim());
    categoryParentKey.value = parent ? categoryGroupOptionKey(parent) : '';
  }
  categoryRuleForm.value = {
    ...categoryRuleForm.value,
    templateCode: template.templateCode,
    validatorType: template.validatorType,
    validatorLabel: validatorDefaultName(template.validatorType),
    projectNameLabel: template.projectNameLabel,
    projectNameRequired: true,
    mediaTechnicalCheckMode: template.mediaTechnicalCheckMode || '',
    extensionModule: !!template.extensionModule
  };
  if (template.validatorType === 'workshop') {
    workshopRuleSettings.value = defaultWorkshopRuleSettings();
  }
};

const removeCategory = async (row: ActivityCategoryVO) => {
  await openCategoryDeleteCheck(row, 'delete');
};

const restoreCategoryRow = async (row: ActivityCategoryVO) => {
  await proxy?.$modal.confirm(`确认恢复类别「${row.categoryName}」？`);
  await restoreCategory(row.id!);
  proxy?.$modal.msgSuccess('恢复成功');
  await loadCategoryList();
};

const preferredCategoryPurgeTab = (check: ActivityCategoryPurgeCheckVO) => {
  if (check.childCount) return 'child';
  if (check.projectCount) return 'project';
  if (check.fileCount) return 'file';
  if (check.referenceCount) return 'reference';
  return 'project';
};

const loadCategoryPurgeCheck = async (categoryId: string | number) => {
  categoryPurgeDialog.loading = true;
  try {
    const { data } = categoryPurgeDialog.mode === 'purge' ? await checkCategoryPurge(categoryId) : await checkCategoryDelete(categoryId);
    categoryPurgeCheck.value = data;
    categoryPurgeTab.value = preferredCategoryPurgeTab(data);
  } finally {
    categoryPurgeDialog.loading = false;
  }
};

const openCategoryDeleteCheck = async (row: ActivityCategoryVO, mode: DeleteCheckMode) => {
  if (!row.id) return;
  categoryPurgeDialog.mode = mode;
  categoryPurgeCheck.value = undefined;
  categoryPurgeDialog.visible = true;
  try {
    await loadCategoryPurgeCheck(row.id);
  } catch (error) {
    categoryPurgeDialog.visible = false;
    throw error;
  }
};

const openCategoryPurgeCheck = async (row: ActivityCategoryVO) => {
  await openCategoryDeleteCheck(row, 'purge');
};

const refreshCategoryPurgeCheck = async () => {
  if (!categoryPurgeCheck.value?.categoryId) return;
  await loadCategoryPurgeCheck(categoryPurgeCheck.value.categoryId);
};

const confirmCategoryDelete = async () => {
  const check = categoryPurgeCheck.value;
  if (!check?.categoryId || !check.canPurge) return;
  const permanent = categoryPurgeDialog.mode === 'purge';
  await proxy?.$modal.confirm(
    permanent
      ? `确认永久删除类别「${check.categoryName}」（ID ${check.categoryId}）？此操作不可恢复，并会同时物理清理 ${check.fieldSchemaCount || 0} 个字段配置、${check.fileRequirementCount || 0} 个附件要求配置。`
      : `确认删除类别「${check.categoryName}」（ID ${check.categoryId}）？类别及其 ${check.fieldSchemaCount || 0} 个字段配置、${check.fileRequirementCount || 0} 个附件要求配置会进入“已删类”，以后可以恢复。`
  );
  categoryPurgeDialog.submitting = true;
  try {
    if (permanent) {
      await purgeCategory(check.categoryId);
      proxy?.$modal.msgSuccess('类别已永久删除');
    } else {
      await delCategory(check.categoryId);
      proxy?.$modal.msgSuccess('类别已删除，可在“已删类”中恢复');
    }
    categoryPurgeDialog.visible = false;
    categoryPurgeCheck.value = undefined;
    currentCategory.value = undefined;
    fieldList.value = [];
    requirementList.value = [];
    await loadCategoryList();
  } finally {
    categoryPurgeDialog.submitting = false;
  }
};

const categoryPurgeProjectStatus = (status?: string) => {
  const map: Record<string, string> = {
    draft: '草稿',
    submitted: '已提交',
    returned: '已退回',
    audit_passed: '审核通过',
    recycled: '回收站'
  };
  return map[status || ''] || status || '-';
};

const categoryPurgeFileStatus = (status?: string) => {
  const map: Record<string, string> = {
    active: '有效',
    replaced: '已替换',
    deleted: '回收站'
  };
  return map[status || ''] || status || '-';
};

const openPurgeRoute = (location: { path: string; query?: Record<string, string> }) => {
  const href = router.resolve(location).href;
  window.open(href, '_blank', 'noopener,noreferrer');
};

const openPurgeProject = (projectId?: string | number) => {
  if (!projectId) return;
  openPurgeRoute({ path: `/crehn/project/edit/${projectId}` });
};

const openCategoryProjectRecycle = () => {
  const categoryId = categoryPurgeCheck.value?.categoryId;
  if (!categoryId) return;
  openPurgeRoute({ path: '/crehn/recycle', query: { tab: 'project', categoryId: String(categoryId) } });
};

const openPurgeProjectRecycle = (projectId?: string | number) => {
  const categoryId = categoryPurgeCheck.value?.categoryId;
  const query: Record<string, string> = { tab: 'project' };
  if (categoryId) query.categoryId = String(categoryId);
  if (projectId) query.projectId = String(projectId);
  openPurgeRoute({ path: '/crehn/recycle', query });
};

const openPurgeFileRecycle = (projectId?: string | number) => {
  const query: Record<string, string> = { tab: 'file' };
  if (projectId) query.projectId = String(projectId);
  openPurgeRoute({ path: '/crehn/recycle', query });
};

const copyPurgeText = async (text: string) => {
  if (!text) {
    proxy?.$modal.msgWarning('没有可复制的内容');
    return;
  }
  try {
    await navigator.clipboard.writeText(text);
  } catch {
    const element = document.createElement('textarea');
    element.value = text;
    element.setAttribute('readonly', '');
    element.style.position = 'fixed';
    element.style.left = '-9999px';
    document.body.appendChild(element);
    element.select();
    document.execCommand('copy');
    element.remove();
  }
  proxy?.$modal.msgSuccess('已复制');
};

const buildActivityDeleteReport = () => {
  const check = activityDeleteCheck.value;
  if (!check) return '';
  const permanent = activityDeleteDialog.mode === 'purge';
  const lines = [
    `活动${permanent ? '永久' : ''}删除检查：${check.activityName || '-'}（ID ${check.activityId || '-'}）`,
    `检查结果：${check.canDelete ? `允许${permanent ? '永久删除' : '删除'}` : `不能${permanent ? '永久删除' : '删除'}`}`,
    `历史项目 ${check.projectCount || 0} 个；上传文件 ${check.fileCount || 0} 个；其他业务引用 ${check.referenceCount || 0} 条`,
    `活动自有配置：类别 ${check.categoryCount || 0} 个；字段 ${check.fieldSchemaCount || 0} 个；附件要求 ${check.fileRequirementCount || 0} 个；学校范围 ${check.schoolScopeCount || 0} 条`,
    check.blockMessage || ''
  ].filter(Boolean);

  if (check.categories?.length) {
    lines.push('', '【活动配置明细】');
    check.categories.forEach((item) =>
      lines.push(
        `类别ID ${item.id}｜${item.categoryCode || '-'}｜${item.categoryName || '-'}｜父级 ${item.parentId || 0}｜字段 ${item.fieldSchemaCount || 0}｜附件要求 ${item.fileRequirementCount || 0}｜${item.delFlag === '1' ? '已删除' : '正常'}`
      )
    );
  }
  if (check.projects?.length) {
    lines.push('', '【历史项目明细】');
    check.projects.forEach((item) =>
      lines.push(
        `项目ID ${item.id}｜编号 ${item.projectNo || '-'}｜${item.projectName || '-'}｜学校 ${item.schoolName || '-'}（${item.schoolId || '-'}）｜状态 ${categoryPurgeProjectStatus(item.status)}｜数据状态 ${item.delFlag === '1' ? '逻辑删除' : '正常'}｜文件 ${item.fileCount || 0} 个`
      )
    );
  }
  if (check.files?.length) {
    lines.push('', '【上传文件明细】');
    check.files.forEach((item) =>
      lines.push(
        `文件ID ${item.id}｜项目ID ${item.projectId || '-'}｜${item.originalName || '-'}｜OSS ID ${item.ossId || '-'}｜文件状态 ${categoryPurgeFileStatus(item.status)}｜数据状态 ${item.delFlag === '1' ? '逻辑删除' : '正常'}`
      )
    );
  }
  if (check.references?.length) {
    lines.push('', '【其他业务引用】');
    check.references.forEach((item) =>
      lines.push(`${item.referenceLabel || item.referenceType || '-'}｜${item.referenceCount || 0} 条｜记录ID ${item.recordIds || '-'}`)
    );
  }
  if (check.categoryDetailsTruncated || check.projectDetailsTruncated || check.fileDetailsTruncated) {
    lines.push('', '说明：总数准确；类别、项目或文件明细单项最多展示前 200 条。');
  }
  return lines.join('\n');
};

const copyActivityDeleteReport = async () => {
  await copyPurgeText(buildActivityDeleteReport());
};

const buildCategoryPurgeReport = () => {
  const check = categoryPurgeCheck.value;
  if (!check) return '';
  const permanent = categoryPurgeDialog.mode === 'purge';
  const lines = [
    `类别${permanent ? '永久' : ''}删除检查：${check.categoryName || '-'}（ID ${check.categoryId || '-'}，编码 ${check.categoryCode || '-'}）`,
    `检查结果：${check.canPurge ? `允许${permanent ? '永久删除' : '删除'}` : `不能${permanent ? '永久删除' : '删除'}`}`,
    `子类别 ${check.childCount || 0} 个；历史项目 ${check.projectCount || 0} 个；上传文件 ${check.fileCount || 0} 个；外部业务引用 ${check.referenceCount || 0} 条`,
    `类别自有配置：字段 ${check.fieldSchemaCount || 0} 个；附件要求 ${check.fileRequirementCount || 0} 个`,
    check.blockMessage || ''
  ].filter(Boolean);

  if (check.children?.length) {
    lines.push('', '【子类别明细】');
    check.children.forEach((item) =>
      lines.push(`ID ${item.id}｜${item.categoryCode || '-'}｜${item.categoryName || '-'}｜${item.delFlag === '1' ? '已删除' : '正常'}`)
    );
  }
  if (check.projects?.length) {
    lines.push('', '【历史项目明细】');
    check.projects.forEach((item) =>
      lines.push(
        `项目ID ${item.id}｜编号 ${item.projectNo || '-'}｜${item.projectName || '-'}｜学校 ${item.schoolName || '-'}（${item.schoolId || '-'}）｜状态 ${categoryPurgeProjectStatus(item.status)}｜文件 ${item.fileCount || 0} 个`
      )
    );
  }
  if (check.files?.length) {
    lines.push('', '【上传文件明细】');
    check.files.forEach((item) =>
      lines.push(
        `文件ID ${item.id}｜项目ID ${item.projectId}｜${item.originalName || '-'}｜OSS ID ${item.ossId || '-'}｜文件状态 ${categoryPurgeFileStatus(item.status)}｜数据状态 ${item.delFlag === '1' ? '逻辑删除' : '正常'}`
      )
    );
  }
  if (check.references?.length) {
    lines.push('', '【外部业务引用】');
    check.references.forEach((item) =>
      lines.push(`${item.referenceLabel || item.referenceType || '-'}｜${item.referenceCount || 0} 条｜记录ID ${item.recordIds || '-'}`)
    );
  }
  if (check.childDetailsTruncated || check.projectDetailsTruncated || check.fileDetailsTruncated) {
    lines.push('', '说明：总数准确；子类别、项目或文件明细单项最多展示前 200 条。');
  }
  return lines.join('\n');
};

const copyCategoryPurgeReport = async () => {
  await copyPurgeText(buildCategoryPurgeReport());
};

const copyCurrentCategory = async () => {
  const source = currentCategory.value;
  if (!source?.id || source.virtualGroup || !currentActivity.value?.id || selectedActivityDeleted.value) return;
  const group = isCategoryGroup(source);
  const sourceName = source.categoryName || source.categoryCode;
  await proxy?.$modal.confirm(
    group
      ? `确认复制大类“${sourceName}”及其全部子类别、类别规则、字段和附件要求？不复制活动规则、已报送项目和审核/评审数据。`
      : `确认复制类别“${sourceName}”及其类别规则、字段和附件要求？不复制活动规则、已报送项目和审核/评审数据。`
  );
  const { data } = await copyCategory(source.id);
  await loadCategoryList();
  const copiedCategory = categoryList.value.find((item) => String(item.id) === String(data?.id));
  if (!copiedCategory?.id) {
    throw new Error('复制类别已创建，但未能读取新类别编号，请刷新后检查。');
  }
  currentCategory.value = copiedCategory;
  await loadConfig();
  proxy?.$modal.msgSuccess(`已复制${group ? '大类' : '类别'}为“${copiedCategory.categoryName}”`);
};

const resolveCopyText = (source: string, usedValues: Set<string>, keepText = false) => {
  const fallback = keepText ? '未命名字段' : 'field';
  const base = keepText ? String(source || '').trim() || fallback : normalizeKey(source, fallback);
  for (let serial = 1; serial < 1000; serial += 1) {
    const suffix = '_1'.repeat(serial);
    if (!keepText && suffix.length >= 63) {
      break;
    }
    const candidate = keepText ? `${base}${suffix}` : `${base.slice(0, 63 - suffix.length)}${suffix}`;
    if (!usedValues.has(candidate)) return candidate;
  }
  const fallbackValue = keepText ? fallback : normalizeKey(fallback, 'field');
  return `${fallbackValue.slice(0, 54)}_${Date.now().toString(36)}`.slice(0, 63);
};

const copyFieldRow = async (row: CategoryFieldSchemaVO) => {
  if (!canEditFieldSchema.value || categoryDeleted.value || fieldDeleted.value || !row.id) return;
  if (!currentCategory.value?.id) return;
  if (String(row.categoryId || '') && String(row.categoryId) !== String(currentCategory.value.id)) return;
  if (!fieldList.value.some((item) => String(item.id || '') === String(row.id))) return;
  const usedFieldKeys = new Set(fieldList.value.map((item) => item.fieldKey).filter(Boolean));
  const usedFieldLabels = new Set(
    fieldList.value
      .map((item) => String(item.fieldLabel || item.fieldKey || ''))
      .map((value) => String(value || '').trim())
      .filter(Boolean)
  );
  const nextSortOrder =
    fieldList.value.reduce((maxSortOrder, item) => {
      const sortOrder = Number(item.sortOrder);
      return Number.isFinite(sortOrder) ? Math.max(maxSortOrder, sortOrder) : maxSortOrder;
    }, 0) + 1;
  const copyPayload: CategoryFieldSchemaVO = {
    categoryId: currentCategory.value.id,
    fieldType: row.fieldType,
    required: row.required,
    sensitive: row.sensitive,
    sortOrder: nextSortOrder,
    fieldKey: resolveCopyText(row.fieldKey || row.fieldLabel || '', usedFieldKeys),
    fieldLabel: resolveCopyText(row.fieldLabel || row.fieldKey || '', usedFieldLabels, true),
    optionsJson: row.optionsJson,
    validationJson: row.validationJson
  };
  await addField(copyPayload);
  proxy?.$modal.msgSuccess(`已复制字段“${copyPayload.fieldLabel}”`);
  await loadConfig();
};

const openFormLayoutDialog = () => {
  if (!currentCategory.value?.id || categoryDeleted.value || fieldDeleted.value) {
    proxy?.$modal.msgWarning(
      categoryDeleted.value ? '已删除类别不能修改表单布局，请先恢复类别' : fieldDeleted.value ? '请先切换到正常字段列表' : '请先选择报送类别'
    );
    return;
  }
  if (!canEditCategory.value) {
    proxy?.$modal.msgWarning('当前账号没有修改类别配置的权限');
    return;
  }
  formLayoutSettings.value = parseFormLayoutSettings(currentCategory.value.ruleJson);
  formLayoutDialog.visible = true;
};

const submitFormLayout = async () => {
  if (!currentCategory.value?.id || categoryDeleted.value || !canEditCategory.value) return;
  formLayoutDialog.saving = true;
  try {
    const categoryId = currentCategory.value.id;
    const rule = parseJsonObject(currentCategory.value.ruleJson) as Record<string, any>;
    const mode = normalizeFormLayoutMode(formLayoutSettings.value.mode);
    if (mode === 'single') {
      delete rule.formLayout;
    } else if (mode === 'double') {
      rule.formLayout = { mode };
    } else {
      const fieldSpans: Record<string, 12 | 24> = {};
      formLayoutRows.value.forEach((row) => {
        const span = normalizeFormLayoutSpan(formLayoutSettings.value.fieldSpans[row.layoutKey]);
        if (row.layoutKey && span) {
          fieldSpans[row.layoutKey] = span;
        }
      });
      rule.formLayout = { mode, fieldSpans };
    }
    await updateCategory({ ...currentCategory.value, ruleJson: Object.keys(rule).length ? JSON.stringify(rule) : '' });
    await loadCategoryList();
    const refreshed = categoryList.value.find((item) => String(item.id) === String(categoryId));
    if (refreshed) {
      currentCategory.value = refreshed;
    }
    proxy?.$modal.msgSuccess('表单布局已保存');
    formLayoutDialog.visible = false;
  } finally {
    formLayoutDialog.saving = false;
  }
};

// ART-REF: FE.ACTIVITY_CONFIG.FIELD_SCHEMA -> components/FieldSchemaTab.vue
const openField = (row?: CategoryFieldSchemaVO) => {
  if (selectedActivityDeleted.value) {
    proxy?.$modal.msgWarning('已删除活动下的配置为只读，请先恢复活动');
    return;
  }
  fieldForm.value = { categoryId: currentCategory.value?.id, fieldType: 'input', required: false, sensitive: false, sortOrder: 0, ...row };
  selectedFieldTemplate.value = row?.fieldKey || 'custom';
  fieldOptions.value = parseFieldOptions(row?.optionsJson);
  fieldValidationType.value = parseValidationType(row?.validationJson);
  fieldUiSettings.value = parseFieldUiSettings(row?.validationJson);
  durationSettings.value = parseDurationSettings(row?.validationJson);
  cascadeOptionRows.value = parseCascadeOptionRows(row?.validationJson);
  dimensionRows.value = parseDimensionRows(row?.validationJson);
  teacherLimitSettings.value = parseTeacherLimitSettings(row?.validationJson);
  teacherMemberRole.value = parseTeacherMemberRole(row?.validationJson);
  teacherSubFields.value = parseTeacherSubFields(row?.validationJson);
  if (!row?.id) {
    applyFieldTemplate('custom');
  }
  fieldDialog.title = row?.id ? '修改字段' : '新增字段';
  fieldDialog.visible = true;
};

const submitField = async () => {
  if (selectedActivityDeleted.value) {
    proxy?.$modal.msgWarning('已删除活动下的配置为只读，请先恢复活动');
    return;
  }
  ensureCustomFieldKey();
  if (!/^[A-Za-z][A-Za-z0-9_]{1,63}$/.test(fieldForm.value.fieldKey || '')) {
    proxy?.$modal.msgError('字段编码只能使用英文、数字、下划线，并且必须以英文开头');
    return;
  }
  const normalizedCascadeOptions = normalizeCascadeOptionRows(cascadeOptionRows.value);
  if (isOptionField.value) {
    const options = fieldOptions.value.map((item) => item.trim()).filter(Boolean);
    const radioCascadeOptions =
      isRadioField.value && normalizedCascadeOptions.length ? [...new Set(normalizedCascadeOptions.map((item) => item.level1))] : [];
    const finalOptions = radioCascadeOptions.length ? radioCascadeOptions : options;
    if (finalOptions.length === 0) {
      proxy?.$modal.msgError('下拉/单选/多选字段至少需要 1 个选项');
      return;
    }
    fieldForm.value.optionsJson = JSON.stringify(finalOptions);
  } else {
    fieldForm.value.optionsJson = '';
  }
  const validation = parseJsonObject(fieldForm.value.validationJson);
  if (fieldValidationType.value) {
    validation.type = fieldValidationType.value;
  } else {
    delete validation.type;
  }
  const placeholder = fieldUiSettings.value.placeholder.trim();
  const placeholderColor = String(fieldUiSettings.value.placeholderColor || '').trim();
  const placeholderFontSize = Number(fieldUiSettings.value.placeholderFontSize || 0);
  const suffixText = fieldUiSettings.value.suffixText.trim();
  const suffixColor = String(fieldUiSettings.value.suffixColor || '').trim();
  const suffixFontSize = Number(fieldUiSettings.value.suffixFontSize || 0);
  const helpText = fieldUiSettings.value.helpText.trim();
  const helpColor = String(fieldUiSettings.value.helpColor || '').trim();
  const helpFontSize = Number(fieldUiSettings.value.helpFontSize || 0);
  const inputWidthMode = normalizeInputWidthMode(fieldUiSettings.value.inputWidthMode);
  const inputWidthCustom = fieldUiSettings.value.inputWidthCustom.trim();
  const minLength = Number(fieldUiSettings.value.minLength || 0);
  const maxLength = Number(fieldUiSettings.value.maxLength || 0);
  const minKeywordCount = Number(fieldUiSettings.value.minKeywordCount || 0);
  const maxKeywordCount = Number(fieldUiSettings.value.maxKeywordCount || 0);
  const numberMin = finiteNumber(fieldUiSettings.value.numberMin);
  const numberMax = finiteNumber(fieldUiSettings.value.numberMax);
  const precision = fieldUiSettings.value.integerOnly ? 0 : finiteNumber(fieldUiSettings.value.precision);
  const step = finiteNumber(fieldUiSettings.value.step);
  if (inputWidthMode === 'custom' && !validCustomInputWidth(inputWidthCustom)) {
    proxy?.$modal.msgError('自定义宽度请填写 80px-960px 或 10%-100%');
    return;
  }
  if (isCharacterLimitField.value && minLength > 0 && maxLength > 0 && minLength > maxLength) {
    proxy?.$modal.msgError('最多字符不能小于最少字符');
    return;
  }
  if (isKeywordCountField.value && minKeywordCount > 0 && maxKeywordCount > 0 && minKeywordCount > maxKeywordCount) {
    proxy?.$modal.msgError('最多关键词不能小于最少关键词');
    return;
  }
  if (isNumberLimitField.value && numberMin !== undefined && numberMax !== undefined && numberMin > numberMax) {
    proxy?.$modal.msgError('最大值不能小于最小值');
    return;
  }
  if (placeholder) {
    validation.placeholder = placeholder;
  } else {
    delete validation.placeholder;
  }
  if (placeholderColor) {
    validation.placeholderColor = placeholderColor;
  } else {
    delete validation.placeholderColor;
  }
  if (placeholderFontSize > 0) {
    validation.placeholderFontSize = placeholderFontSize;
  } else {
    delete validation.placeholderFontSize;
  }
  if (suffixText) {
    validation.suffixText = suffixText;
  } else {
    delete validation.suffixText;
  }
  if (suffixColor) {
    validation.suffixColor = suffixColor;
  } else {
    delete validation.suffixColor;
  }
  if (suffixFontSize > 0) {
    validation.suffixFontSize = suffixFontSize;
  } else {
    delete validation.suffixFontSize;
  }
  if (helpText) {
    validation.helpText = helpText;
  } else {
    delete validation.helpText;
  }
  if (helpColor) {
    validation.helpColor = helpColor;
  } else {
    delete validation.helpColor;
  }
  if (helpFontSize > 0) {
    validation.helpFontSize = helpFontSize;
  } else {
    delete validation.helpFontSize;
  }
  if (inputWidthMode !== 'default') {
    validation.inputWidthMode = inputWidthMode;
  } else {
    delete validation.inputWidthMode;
  }
  if (inputWidthMode === 'custom') {
    validation.inputWidthCustom = inputWidthCustom;
  } else {
    delete validation.inputWidthCustom;
  }
  if (isCharacterLimitField.value) {
    if (minLength > 0) {
      validation.minLength = minLength;
    } else {
      delete validation.minLength;
    }
    if (maxLength > 0) {
      validation.maxLength = maxLength;
    } else {
      delete validation.maxLength;
    }
    if (maxLength > 0 && fieldUiSettings.value.showWordLimit === false) {
      validation.showWordLimit = false;
    } else {
      delete validation.showWordLimit;
    }
  } else {
    delete validation.minLength;
    delete validation.maxLength;
    delete validation.showWordLimit;
  }
  if (isKeywordCountField.value) {
    validation.minKeywordCount = minKeywordCount > 0 ? minKeywordCount : 3;
    validation.maxKeywordCount = maxKeywordCount > 0 ? maxKeywordCount : 5;
  } else {
    delete validation.minKeywordCount;
    delete validation.maxKeywordCount;
  }
  if (isNumberLimitField.value) {
    if (numberMin !== undefined) {
      validation.min = numberMin;
    } else {
      delete validation.min;
    }
    if (numberMax !== undefined) {
      validation.max = numberMax;
    } else {
      delete validation.max;
    }
    if (fieldUiSettings.value.integerOnly) {
      validation.integerOnly = true;
      validation.precision = 0;
    } else {
      delete validation.integerOnly;
      if (precision !== undefined && precision >= 0) {
        validation.precision = precision;
      } else {
        delete validation.precision;
      }
    }
    if (step !== undefined && step > 0) {
      validation.step = step;
    } else {
      delete validation.step;
    }
  } else {
    delete validation.min;
    delete validation.max;
    delete validation.integerOnly;
    delete validation.precision;
    delete validation.step;
  }
  if (fieldForm.value.fieldType === 'dimension_group') {
    validation.dimensions = normalizeDimensionRows(dimensionRows.value);
  } else {
    delete validation.dimensions;
  }
  if (fieldForm.value.fieldType === 'duration') {
    validation.durationMode = durationSettings.value.durationMode || 'hms';
  } else {
    delete validation.durationMode;
  }
  if (fieldForm.value.fieldType === 'radio' && normalizedCascadeOptions.length) {
    validation.cascadeOptions = normalizedCascadeOptions;
  } else {
    delete validation.cascadeOptions;
  }
  if (fieldForm.value.fieldType === 'teacher_group') {
    const minItems = Number(teacherLimitSettings.value.minItems || 0);
    const maxItems = Number(teacherLimitSettings.value.maxItems || 3);
    if (maxItems < minItems) {
      proxy?.$modal.msgError('最多人数不能小于最少人数');
      return;
    }
    validation.minItems = minItems;
    validation.maxItems = maxItems;
    if (teacherMemberRole.value) {
      validation.memberRole = teacherMemberRole.value;
    } else {
      delete validation.memberRole;
    }
    validation.teacherFields = normalizeSubFieldRows(teacherSubFields.value, 'teacher');
  } else {
    delete validation.minItems;
    delete validation.maxItems;
    delete validation.maxTeacherCount;
    delete validation.memberRole;
    delete validation.teacherFields;
  }
  fieldForm.value.validationJson = Object.keys(validation).length ? JSON.stringify(validation) : '';
  if (!fieldForm.value.id) {
    fieldForm.value.fieldKey = uniqueFieldKey(fieldForm.value.fieldKey || createCustomFieldKey());
  }
  fieldForm.value.id ? await updateField(fieldForm.value) : await addField(fieldForm.value);
  proxy?.$modal.msgSuccess('操作成功');
  fieldDialog.visible = false;
  await loadConfig();
};

const applyFieldTemplate = (value?: string) => {
  const template = fieldTemplates.find((item) => item.fieldKey === value);
  if (!template) {
    fieldForm.value.fieldKey = fieldForm.value.id ? fieldForm.value.fieldKey : createCustomFieldKey();
    fieldForm.value.fieldType = fieldForm.value.fieldType || 'input';
    fieldForm.value.required = fieldForm.value.required ?? false;
    fieldForm.value.sensitive = fieldForm.value.sensitive ?? false;
    fieldOptions.value = [];
    if (!fieldForm.value.id) {
      fieldValidationType.value = '';
      fieldUiSettings.value = defaultFieldUiSettings();
      durationSettings.value = defaultDurationSettings();
      cascadeOptionRows.value = [];
      dimensionRows.value = defaultDimensionRows();
      teacherLimitSettings.value = defaultTeacherLimitSettings();
      teacherMemberRole.value = '';
    } else {
      fieldUiSettings.value = parseFieldUiSettings(fieldForm.value.validationJson);
      durationSettings.value = parseDurationSettings(fieldForm.value.validationJson);
      cascadeOptionRows.value = parseCascadeOptionRows(fieldForm.value.validationJson);
      dimensionRows.value = parseDimensionRows(fieldForm.value.validationJson);
      teacherLimitSettings.value = parseTeacherLimitSettings(fieldForm.value.validationJson);
      teacherMemberRole.value = parseTeacherMemberRole(fieldForm.value.validationJson);
    }
    return;
  }
  fieldForm.value = {
    ...fieldForm.value,
    fieldKey: fieldForm.value.id ? template.fieldKey : uniqueFieldKey(template.fieldKey),
    fieldLabel: template.fieldLabel,
    fieldType: template.fieldType,
    required: template.required ?? false,
    sensitive: template.sensitive ?? false,
    optionsJson: template.options?.length ? JSON.stringify(template.options) : '',
    validationJson: template.validationJson || ''
  };
  fieldOptions.value = [...(template.options || [])];
  fieldValidationType.value = inferValidationType(template);
  fieldUiSettings.value = parseFieldUiSettings(template.validationJson);
  durationSettings.value = parseDurationSettings(template.validationJson);
  cascadeOptionRows.value = parseCascadeOptionRows(template.validationJson);
  dimensionRows.value = parseDimensionRows(template.validationJson);
  teacherLimitSettings.value = parseTeacherLimitSettings(template.validationJson);
  teacherMemberRole.value = parseTeacherMemberRole(template.validationJson);
  teacherSubFields.value = template.fieldType === 'teacher_group' ? parseTeacherSubFields(template.validationJson) : [];
};

const handleFieldTypeChange = () => {
  if (fieldForm.value.fieldType === 'duration') {
    durationSettings.value = durationSettings.value || defaultDurationSettings();
  }
  if (fieldForm.value.fieldType === 'radio' && cascadeOptionRows.value.length === 0) {
    cascadeOptionRows.value = [];
  }
  if (fieldForm.value.fieldType === 'dimension_group' && dimensionRows.value.length === 0) {
    dimensionRows.value = defaultDimensionRows();
  }
  if (fieldForm.value.fieldType === 'teacher_group' && teacherSubFields.value.length === 0) {
    teacherSubFields.value = defaultTeacherSubFields();
    teacherLimitSettings.value = parseTeacherLimitSettings(fieldForm.value.validationJson);
    teacherMemberRole.value = parseTeacherMemberRole(fieldForm.value.validationJson);
  }
};

const ensureCustomFieldKey = () => {
  const key = fieldForm.value.fieldKey || '';
  if (!key || /[\u4e00-\u9fa5]/.test(key)) {
    fieldForm.value.fieldKey = uniqueFieldKey(createCustomFieldKey());
  }
};

const createCustomFieldKey = () => `custom_${Date.now().toString(36)}`;

const uniqueFieldKey = (baseKey: string) => {
  const normalized = normalizeKey(baseKey, 'field');
  const used = new Set(
    fieldList.value
      .filter((item) => String(item.id || '') !== String(fieldForm.value.id || ''))
      .map((item) => item.fieldKey)
      .filter(Boolean)
  );
  if (!used.has(normalized)) return normalized;
  for (let index = 2; index < 1000; index += 1) {
    const suffix = `_${index}`;
    const candidate = `${normalized.slice(0, 63 - suffix.length)}${suffix}`;
    if (!used.has(candidate)) return candidate;
  }
  return `${normalized.slice(0, 54)}_${Date.now().toString(36)}`;
};

const normalizeKey = (value?: string, prefix = 'field') => {
  const raw = String(value || '').trim();
  let key = raw
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .toLowerCase()
    .replace(/[^a-z0-9_]/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '');
  if (!/^[a-z]/.test(key)) {
    key = `${prefix}_${key || Date.now().toString(36)}`;
  }
  return key.slice(0, 63);
};

const parseFieldOptions = (json?: string) => {
  if (!json) return [];
  try {
    const parsed = JSON.parse(json);
    return Array.isArray(parsed) ? parsed.map((item) => String(item)) : [];
  } catch {
    return [];
  }
};

const normalizeFormLayoutMode = (value: unknown): FormLayoutMode => {
  const mode = String(value || 'single');
  return mode === 'double' || mode === 'custom' ? mode : 'single';
};

const normalizeFormLayoutSpan = (value: unknown): FormLayoutSpan => {
  const span = Number(value);
  return span === 12 || span === 24 ? span : 0;
};

const PROJECT_NAME_LAYOUT_KEY = '__projectName';

const fieldLayoutKey = (field: CategoryFieldSchemaVO) => String(field.fieldKey || field.id || '');

const autoFieldLayoutSpan = (field: CategoryFieldSchemaVO): 12 | 24 => {
  const type = String(field.fieldType || 'input');
  if (['textarea', 'teacher_group', 'dimension_group'].includes(type)) return 24;
  if (field.fieldKey === 'topicCategory' || field.fieldLabel === '选题类别') return 24;
  if (type === 'radio' || type === 'checkbox') {
    const validation = parseJsonObject(field.validationJson) as Record<string, any>;
    if (Array.isArray(validation.cascadeOptions) && validation.cascadeOptions.length) return 24;
    if (parseFieldOptions(field.optionsJson).length > 4) return 24;
  }
  return 12;
};

const projectNameLayoutLabel = computed(() => {
  const rule = parseJsonObject(currentCategory.value?.ruleJson) as Record<string, any>;
  const projectName = rule.projectName && typeof rule.projectName === 'object' && !Array.isArray(rule.projectName) ? rule.projectName : {};
  return String(projectName.label || '项目名称').trim() || '项目名称';
});

const formLayoutRows = computed<FormLayoutRow[]>(() => [
  {
    layoutKey: PROJECT_NAME_LAYOUT_KEY,
    sortOrder: -1,
    fieldLabel: projectNameLayoutLabel.value,
    fieldType: '主名称字段'
  },
  ...fieldList.value.map((field) => ({
    layoutKey: fieldLayoutKey(field),
    sortOrder: Number(field.sortOrder || 0),
    fieldLabel: String(field.fieldLabel || field.fieldKey || '未命名字段'),
    fieldType: String(field.fieldType || 'input'),
    field
  }))
]);

const autoFormLayoutRowSpan = (row: FormLayoutRow): 12 | 24 => (row.field ? autoFieldLayoutSpan(row.field) : 12);

const parseFormLayoutSettings = (ruleJson?: string): FormLayoutSettings => {
  const rule = parseJsonObject(ruleJson) as Record<string, any>;
  const layout = rule.formLayout && typeof rule.formLayout === 'object' && !Array.isArray(rule.formLayout) ? rule.formLayout : {};
  const rawFieldSpans = layout.fieldSpans && typeof layout.fieldSpans === 'object' && !Array.isArray(layout.fieldSpans) ? layout.fieldSpans : {};
  const fieldSpans: Record<string, FormLayoutSpan> = {};
  Object.entries(rawFieldSpans).forEach(([key, value]) => {
    const span = normalizeFormLayoutSpan(value);
    if (key && span) {
      fieldSpans[key] = span;
    }
  });
  formLayoutRows.value.forEach((row) => {
    if (row.layoutKey && fieldSpans[row.layoutKey] === undefined) {
      fieldSpans[row.layoutKey] = 0;
    }
  });
  return {
    mode: normalizeFormLayoutMode(layout.mode),
    fieldSpans
  };
};

const parseFieldUiSettings = (validationJson?: string) => {
  const validation = parseJsonObject(validationJson);
  return {
    placeholder: String((validation as any).placeholder || ''),
    placeholderColor: String((validation as any).placeholderColor || ''),
    placeholderFontSize: Number((validation as any).placeholderFontSize || 0) || undefined,
    suffixText: String((validation as any).suffixText || ''),
    suffixColor: String((validation as any).suffixColor || ''),
    suffixFontSize: Number((validation as any).suffixFontSize || 0) || undefined,
    helpText: String((validation as any).helpText || ''),
    helpColor: String((validation as any).helpColor || ''),
    helpFontSize: Number((validation as any).helpFontSize || 0) || undefined,
    inputWidthMode: normalizeInputWidthMode((validation as any).inputWidthMode),
    inputWidthCustom: String((validation as any).inputWidthCustom || ''),
    minLength: Number((validation as any).minLength || 0) || undefined,
    maxLength: Number((validation as any).maxLength || 0) || undefined,
    minKeywordCount: Number((validation as any).minKeywordCount || 0) || 3,
    maxKeywordCount: Number((validation as any).maxKeywordCount || 0) || 5,
    showWordLimit: (validation as any).showWordLimit !== false,
    numberMin: finiteNumber((validation as any).min),
    numberMax: finiteNumber((validation as any).max),
    integerOnly: !!(validation as any).integerOnly,
    precision: Number.isFinite(Number((validation as any).precision)) ? Number((validation as any).precision) : undefined,
    step: finiteNumber((validation as any).step)
  };
};

function defaultFieldUiSettings(): FieldUiSettings {
  return {
    placeholder: '',
    placeholderColor: '',
    placeholderFontSize: undefined,
    suffixText: '',
    suffixColor: '',
    suffixFontSize: undefined,
    helpText: '',
    helpColor: '',
    helpFontSize: undefined,
    inputWidthMode: 'default',
    inputWidthCustom: '',
    minLength: undefined,
    maxLength: undefined,
    minKeywordCount: 3,
    maxKeywordCount: 5,
    showWordLimit: true,
    numberMin: undefined,
    numberMax: undefined,
    integerOnly: false,
    precision: undefined,
    step: undefined
  };
}

const restoreDefaultFieldColor = (key: 'placeholderColor' | 'suffixColor' | 'helpColor') => {
  fieldUiSettings.value[key] = '';
};

const normalizeInputWidthMode = (value: unknown) => {
  const mode = String(value || 'default');
  return inputWidthModeOptions.some((item) => item.value === mode) ? mode : 'default';
};

const finiteNumber = (value: unknown) => {
  if (value === undefined || value === null || value === '') return undefined;
  const number = Number(value);
  return Number.isFinite(number) ? number : undefined;
};

function defaultMemberTableSettings(): MemberTableSettings {
  return {
    enabled: true,
    total: {},
    author: {},
    student: {},
    teacher: {},
    completer: {}
  };
}

const parseMemberCountRule = (value: any): MemberCountRuleSettings => ({
  min: finiteNumber(value?.min),
  max: finiteNumber(value?.max)
});

const parseMemberTableSettings = (rule: Record<string, any>): MemberTableSettings => {
  const rules =
    rule.memberCountRules && typeof rule.memberCountRules === 'object' && !Array.isArray(rule.memberCountRules) ? rule.memberCountRules : {};
  return {
    enabled: rule.memberTableEnabled !== false,
    total: parseMemberCountRule(rules.total),
    author: parseMemberCountRule(rules.author),
    student: parseMemberCountRule(rules.student),
    teacher: parseMemberCountRule(rules.teacher),
    completer: parseMemberCountRule(rules.completer)
  };
};

const cleanMemberCountRule = (rule: MemberCountRuleSettings) => {
  const result: MemberCountRuleSettings = {};
  const min = finiteNumber(rule.min);
  const max = finiteNumber(rule.max);
  if (min !== undefined) result.min = min;
  if (max !== undefined) result.max = max;
  return result;
};

const applyMemberTableSettings = (rule: Record<string, any>) => {
  if (memberTableSettings.value.enabled) {
    delete rule.memberTableEnabled;
  } else {
    rule.memberTableEnabled = false;
  }
  const countRules: Record<string, MemberCountRuleSettings> = {};
  memberCountRuleRows.forEach((item) => {
    const cleaned = cleanMemberCountRule(memberTableSettings.value[item.key]);
    if (Object.keys(cleaned).length) {
      countRules[item.key] = cleaned;
    }
  });
  if (Object.keys(countRules).length) {
    rule.memberCountRules = countRules;
  } else {
    delete rule.memberCountRules;
  }
};

const validCustomInputWidth = (value: string) => {
  const pxMatch = value.match(/^(\d{2,3})px$/);
  if (pxMatch) {
    const width = Number(pxMatch[1]);
    return width >= 80 && width <= 960;
  }
  const percentMatch = value.match(/^(\d{1,3})%$/);
  if (percentMatch) {
    const width = Number(percentMatch[1]);
    return width >= 10 && width <= 100;
  }
  return false;
};

function defaultDurationSettings(): DurationSettings {
  return { durationMode: 'hms' };
}

const parseDurationSettings = (validationJson?: string): DurationSettings => {
  const validation = parseJsonObject(validationJson);
  const durationMode = String((validation as any).durationMode || 'hms');
  return { durationMode: ['hms', 'ms', 'minute', 'second'].includes(durationMode) ? durationMode : 'hms' };
};

const parseCascadeOptionRows = (validationJson?: string): CascadeOptionRow[] => {
  const validation = parseJsonObject(validationJson);
  const rows = Array.isArray((validation as any).cascadeOptions) ? (validation as any).cascadeOptions : [];
  return groupCascadeOptionRows(normalizeCascadeOptionRows(rows));
};

const normalizeCascadeOptionRows = (rows: any[]): CascadeOptionRow[] => {
  return rows
    .flatMap((row) => {
      const level1 = String(row?.level1 || row?.label || '').trim();
      const level2Items = splitOptionText(row?.level2);
      const level3Items = splitOptionText(row?.level3);
      if (!level1) return [];
      if (!level2Items.length) return [{ level1, level2: '', level3: '' }];
      return level2Items.flatMap((level2) => {
        if (!level3Items.length) return [{ level1, level2, level3: '' }];
        return level3Items.map((level3) => ({ level1, level2, level3 }));
      });
    })
    .filter((row) => row.level1);
};

const splitOptionText = (value: any) => {
  return String(value || '')
    .split(/[,，、;；\n]+/)
    .map((item) => item.trim())
    .filter(Boolean);
};

const groupCascadeOptionRows = (rows: CascadeOptionRow[]): CascadeOptionRow[] => {
  const map = new Map<string, { level2: Set<string>; level3: Set<string> }>();
  rows.forEach((row) => {
    const level1 = row.level1.trim();
    if (!level1) return;
    if (!map.has(level1)) {
      map.set(level1, { level2: new Set(), level3: new Set() });
    }
    if (row.level2) map.get(level1)!.level2.add(row.level2);
    if (row.level3) map.get(level1)!.level3.add(row.level3);
  });
  return [...map.entries()].map(([level1, value]) => ({
    level1,
    level2: [...value.level2].join('，'),
    level3: [...value.level3].join('，')
  }));
};

const addCascadeOptionRow = () => {
  cascadeOptionRows.value.push({ level1: '', level2: '', level3: '' });
};

const fillPersonalProgramCascadeOptions = () => {
  cascadeOptionRows.value = [
    { level1: '独唱', level2: '美声，民族，流行', level3: '' },
    { level1: '独奏', level2: '个人独奏，其他', level3: '' },
    { level1: '独舞', level2: '民族舞，其他', level3: '' },
    { level1: '个人戏曲', level2: '豫剧，其他', level3: '' },
    { level1: '独诵', level2: '朗诵，其他', level3: '' }
  ];
};

function defaultDimensionRows(): DimensionRow[] {
  return [
    { key: 'length', label: '长', placeholder: '长', unit: 'cm', required: true },
    { key: 'width', label: '宽', placeholder: '宽', unit: 'cm', required: true },
    { key: 'height', label: '高', placeholder: '高', unit: 'cm', required: true }
  ];
}

const parseDimensionRows = (validationJson?: string) => {
  const validation = parseJsonObject(validationJson);
  const rows = Array.isArray((validation as any).dimensions) ? (validation as any).dimensions : defaultDimensionRows();
  return normalizeDimensionRows(rows);
};

const normalizeDimensionRows = (rows: any[]): DimensionRow[] => {
  const fallback = defaultDimensionRows();
  return fallback.map((item, index) => {
    const row = rows.find((entry) => String(entry?.key || '') === item.key) || rows[index] || {};
    return {
      key: item.key,
      label: String(row.label || item.label),
      placeholder: String(row.placeholder || row.label || item.placeholder),
      unit: String(row.unit || item.unit),
      required: row.required === undefined ? item.required : !!row.required
    };
  });
};

function defaultTeacherLimitSettings(): TeacherLimitSettings {
  return { minItems: 0, maxItems: 3 };
}

const parseTeacherLimitSettings = (validationJson?: string): TeacherLimitSettings => {
  const validation = parseJsonObject(validationJson);
  const minItems = Number((validation as any).minItems || 0);
  const maxItems = Number((validation as any).maxItems || (validation as any).maxTeacherCount || 3);
  return {
    minItems: Number.isFinite(minItems) && minItems >= 0 ? minItems : 0,
    maxItems: Number.isFinite(maxItems) && maxItems > 0 ? maxItems : 3
  };
};

const parseTeacherMemberRole = (validationJson?: string) => {
  const validation = parseJsonObject(validationJson);
  const role = String((validation as any).memberRole || (validation as any).businessRole || (validation as any).role || '').trim();
  return ['author', 'teacher', 'student'].includes(role) ? role : '';
};

const defaultTeacherSubFields = (): SubFieldRow[] => [
  { fieldKey: 'name', fieldLabel: '姓名', fieldType: 'input', required: false, sortOrder: 1 },
  { fieldKey: 'phone', fieldLabel: '联系电话', fieldType: 'input', required: false, sortOrder: 2 },
  { fieldKey: 'remark', fieldLabel: '备注', fieldType: 'input', required: false, sortOrder: 3 }
];

const parseTeacherSubFields = (validationJson?: string) => {
  const validation = parseJsonObject(validationJson);
  const rows = Array.isArray((validation as any).teacherFields) ? (validation as any).teacherFields : defaultTeacherSubFields();
  return rows.map(toSubFieldRow);
};

const toSubFieldRow = (item: any): SubFieldRow => {
  const validation = parseJsonObject(item?.validationJson);
  const attachmentAllowedExts = validation.attachmentAllowedExts;
  return {
    fieldKey: item?.fieldKey,
    fieldLabel: item?.fieldLabel || item?.fieldKey,
    fieldType: item?.fieldType || 'input',
    required: !!item?.required,
    sensitive: !!item?.sensitive,
    validationType: parseValidationType(item?.validationJson),
    optionsJson: item?.optionsJson,
    optionsText: parseFieldOptions(item?.optionsJson).join(','),
    matchFieldKey: String(validation.matchFieldKey || ''),
    attachmentAllowedExts: Array.isArray(attachmentAllowedExts) ? attachmentAllowedExts.map(String).join(',') : '',
    sortOrder: item?.sortOrder || 0
  };
};

const isMemberAttachmentField = (row: SubFieldRow) => isMemberAttachmentType(row);

const normalizeSubFieldRows = (rows: SubFieldRow[], prefix: string) => {
  const used = new Set<string>();
  return rows
    .map((row, index) => {
      const base = normalizeKey(row.fieldKey || row.fieldLabel || '', prefix);
      let fieldKey = base;
      for (let serial = 2; used.has(fieldKey); serial += 1) {
        fieldKey = `${base.slice(0, 61)}_${serial}`;
      }
      used.add(fieldKey);
      const options = String(row.optionsText || '')
        .split(/[,，\n]/)
        .map((item) => item.trim())
        .filter(Boolean);
      const validation: Record<string, any> = {};
      if (row.validationType) validation.type = row.validationType;
      if (isMemberAttachmentField(row)) {
        validation.matchFieldKey = normalizeMemberAttachmentMatchField(row.matchFieldKey);
        if (row.fieldType === 'attachment_upload') {
          const exts = String(row.attachmentAllowedExts || '')
            .split(/[,，、;；\s]+/)
            .map((item) => item.trim().toLowerCase().replace(/^\./, ''))
            .filter(Boolean);
          if (exts.length) validation.attachmentAllowedExts = Array.from(new Set(exts));
        }
      }
      const validationJson = Object.keys(validation).length ? JSON.stringify(validation) : undefined;
      return {
        fieldKey,
        fieldLabel: row.fieldLabel || fieldKey,
        fieldType: row.fieldType || 'input',
        required: !!row.required,
        sensitive: !!row.sensitive,
        validationJson,
        optionsJson: optionFieldTypes.includes(row.fieldType || '') ? JSON.stringify(options) : undefined,
        sortOrder: row.sortOrder || index + 1
      };
    })
    .filter((row) => row.fieldKey && row.fieldLabel);
};

const normalizeMemberAttachmentMatchField = (value?: string) => {
  const fieldKey = String(value || '').trim();
  return /^[a-zA-Z][a-zA-Z0-9_]{0,63}$/.test(fieldKey) ? fieldKey : 'name';
};

const ensureSubFieldKey = (row: SubFieldRow, prefix: string) => {
  row.fieldKey = normalizeKey(row.fieldKey || row.fieldLabel || '', prefix);
};

const addTeacherSubField = () => {
  teacherSubFields.value.push({ fieldKey: '', fieldLabel: '', fieldType: 'input', required: false, sortOrder: teacherSubFields.value.length + 1 });
};

const memberDefaultTypeCode = computed(() => memberTypeOptions.value.find((item) => item.default)?.code || 'student');

const memberMatchFieldOptions = (group: 'author' | 'participant') => {
  const fixed = [
    { value: 'name', label: '姓名' },
    { value: 'studentNo', label: '学号/工作证号' },
    { value: 'roleName', label: '备注/身份' },
    { value: 'identityNo', label: '身份证号/护照号' },
    { value: 'phone', label: '联系方式' }
  ];
  const rows = memberSubFields[group]
    .filter((item) => item.fieldKey && !isMemberAttachmentType(item))
    .map((item) => ({ value: item.fieldKey!, label: item.fieldLabel || item.fieldKey! }));
  const used = new Set<string>();
  return [...fixed, ...rows].filter((item) => !used.has(item.value) && !!used.add(item.value));
};

const ensureMemberTypeCode = (row: MemberTypeOption) => {
  if (row.builtin) return;
  const base = normalizeKey(row.code || row.label || 'custom_type', 'type').slice(0, 32);
  const used = new Set(memberTypeOptions.value.filter((item) => item !== row).map((item) => item.code));
  let code = base;
  for (let serial = 2; used.has(code); serial += 1) {
    const suffix = `_${serial}`;
    code = `${base.slice(0, 32 - suffix.length)}${suffix}`;
  }
  row.code = code;
};

const addMemberType = () => {
  const row: MemberTypeOption = { code: 'custom_type', label: '自定义类型', group: 'participant', enabled: true, builtin: false };
  ensureMemberTypeCode(row);
  memberTypeOptions.value.push(row);
};

const removeMemberType = (index: number) => {
  const removed = memberTypeOptions.value[index];
  if (!removed || removed.builtin) return;
  memberTypeOptions.value.splice(index, 1);
  if (removed.default) setMemberDefaultType(memberTypeOptions.value.find((item) => item.enabled)?.code || 'student');
};

const setMemberDefaultType = (code: string) => {
  memberTypeOptions.value.forEach((item) => {
    item.default = item.code === code && item.enabled;
  });
};

const openMemberFieldDialog = () => {
  if (selectedActivityDeleted.value) {
    proxy?.$modal.msgWarning('已删除活动下的配置为只读，请先恢复活动');
    return;
  }
  const rule = parseJsonObject(currentCategory.value?.ruleJson);
  const groups = (rule as any).memberFieldGroups || {};
  const savedAuthorRows = savedMemberSubFields(groups, 'author');
  const savedParticipantRows = savedMemberSubFields(groups, 'participant');
  memberTipText.value = String((rule as any).memberTipText || '');
  memberTemplateFooterText.value = String((rule as any).memberTemplateFooterText || (rule as any).memberTemplate?.footerText || '');
  memberTableTitle.value = String((rule as any).memberTableTitle || '');
  memberTipLabel.value = String((rule as any).memberTipLabel || '');
  memberTypeOptions.value = normalizeMemberTypeOptions((rule as any).memberTypeOptions);
  memberTableSettings.value = parseMemberTableSettings(rule as Record<string, any>);
  genericTableTemplates.value = normalizeGenericTableTemplates((rule as any).genericTableTemplates);
  memberSubFields.author.splice(
    0,
    memberSubFields.author.length,
    ...(savedAuthorRows ?? mergeDefaultMemberFields([], defaultAuthorMemberFields(), ['name']))
  );
  memberSubFields.participant.splice(
    0,
    memberSubFields.participant.length,
    ...(savedParticipantRows ?? mergeDefaultMemberFields([], defaultParticipantMemberFields()))
  );
  activeMemberFieldGroup.value = 'author';
  activeTableConfigMode.value = genericTableTemplates.value.length ? 'generic' : 'member';
  memberFieldDialog.visible = true;
};

// Empty saved arrays are intentional: they mean the admin removed all fields in that group.
const savedMemberSubFields = (groups: any, group: 'author' | 'participant') => {
  const value = groups?.[group];
  return Array.isArray(value) ? parseMemberSubFields(value) : undefined;
};

const parseMemberSubFields = (value: any) => (Array.isArray(value) ? value.map(toSubFieldRow) : []);

const defaultAuthorMemberFields = (): SubFieldRow[] => [
  { fieldKey: 'name', fieldLabel: '姓名', fieldType: 'input', required: true, sortOrder: 1 },
  { fieldKey: 'phone', fieldLabel: '联系方式', fieldType: 'input', required: false, validationType: 'phone', sortOrder: 2 },
  { fieldKey: 'roleName', fieldLabel: '备注/身份', fieldType: 'input', required: false, sortOrder: 3 }
];

const defaultParticipantMemberFields = (): SubFieldRow[] => [
  { fieldKey: 'name', fieldLabel: '姓名', fieldType: 'input', required: true, sortOrder: 1 },
  { fieldKey: 'identityNo', fieldLabel: '身份证号/护照号', fieldType: 'input', required: false, validationType: 'id_card', sortOrder: 2 },
  { fieldKey: 'nation', fieldLabel: '民族', fieldType: 'input', required: false, validationType: 'nation', sortOrder: 3 },
  { fieldKey: 'gender', fieldLabel: '性别', fieldType: 'input', required: false, validationType: 'gender', sortOrder: 4 },
  { fieldKey: 'grade', fieldLabel: '年级', fieldType: 'input', required: false, sortOrder: 5 },
  { fieldKey: 'studentNo', fieldLabel: '学号/工作证号', fieldType: 'input', required: false, sortOrder: 6 },
  { fieldKey: 'department', fieldLabel: '所在院系', fieldType: 'input', required: false, sortOrder: 7 },
  { fieldKey: 'majorCode', fieldLabel: '专业代码', fieldType: 'input', required: false, sortOrder: 8 },
  { fieldKey: 'major', fieldLabel: '专业名称', fieldType: 'input', required: false, sortOrder: 9 },
  { fieldKey: 'phone', fieldLabel: '联系方式', fieldType: 'input', required: false, validationType: 'phone', sortOrder: 10 },
  { fieldKey: 'roleName', fieldLabel: '备注/身份', fieldType: 'input', required: false, sortOrder: 11 },
  { fieldKey: 'photo', fieldLabel: '证件照', fieldType: 'image_upload', required: false, sortOrder: 12 },
  { fieldKey: 'studentReport', fieldLabel: '学籍报告', fieldType: 'pdf_upload', required: false, sortOrder: 13 }
];

const mergeDefaultMemberFields = (savedRows: SubFieldRow[], defaults: SubFieldRow[], defaultVisibleKeys?: string[]) => {
  const rows = [...savedRows];
  const existing = new Set(rows.map((item) => item.fieldKey).filter(Boolean));
  defaults.forEach((item) => {
    if (existing.has(item.fieldKey)) return;
    if (defaultVisibleKeys && !defaultVisibleKeys.includes(item.fieldKey || '')) return;
    rows.push({ ...item });
  });
  return rows.sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
};

const addMemberSubField = (group: 'author' | 'participant') => {
  memberSubFields[group].push({ fieldKey: '', fieldLabel: '', fieldType: 'input', required: false, sortOrder: memberSubFields[group].length + 1 });
};

const removeMemberSubField = async (group: 'author' | 'participant', index: number) => {
  const row = memberSubFields[group][index];
  const uploadField = isMemberAttachmentType(row);
  if (uploadField) {
    await proxy?.$modal.confirm(
      `删除「${row.fieldLabel || row.fieldKey}」后，成员列表将隐藏该上传列；已有上传数据不会删除，但页面不再展示。确认继续？`
    );
  }
  memberSubFields[group].splice(index, 1);
};

const submitMemberFields = async () => {
  if (!currentCategory.value?.id || memberFieldDialog.saving) return;
  const genericValidationMessage = validateGenericTableTemplates(genericTableTemplates.value);
  if (genericValidationMessage) {
    activeTableConfigMode.value = 'generic';
    proxy?.$modal.msgWarning(genericValidationMessage);
    return;
  }
  if (!memberTypeOptions.value.some((item) => item.enabled)) {
    activeTableConfigMode.value = 'member';
    proxy?.$modal.msgWarning('请至少启用一种成员类型');
    return;
  }
  if (memberTypeOptions.value.some((item) => !/^[a-z][a-z0-9_]{0,31}$/.test(item.code) || !item.label.trim())) {
    activeTableConfigMode.value = 'member';
    proxy?.$modal.msgWarning('成员类型的数据编码需为字母开头的英文、数字或下划线，且显示名称不能为空');
    return;
  }
  if (new Set(memberTypeOptions.value.map((item) => item.code)).size !== memberTypeOptions.value.length) {
    activeTableConfigMode.value = 'member';
    proxy?.$modal.msgWarning('成员类型的数据编码不能重复');
    return;
  }
  memberFieldDialog.saving = true;
  try {
    const rule = parseJsonObject(currentCategory.value.ruleJson);
    applyMemberTableSettings(rule as Record<string, any>);
    const tipText = memberTipText.value.trim();
    if (tipText) {
      (rule as any).memberTipText = tipText;
    } else {
      delete (rule as any).memberTipText;
    }
    const title = memberTableTitle.value.trim();
    if (title) {
      (rule as any).memberTableTitle = title;
    } else {
      delete (rule as any).memberTableTitle;
    }
    const tipLabel = memberTipLabel.value.trim();
    if (tipLabel) {
      (rule as any).memberTipLabel = tipLabel;
    } else {
      delete (rule as any).memberTipLabel;
    }
    (rule as any).memberTypeOptions = normalizeMemberTypeOptions(memberTypeOptions.value).map(
      ({ code, label, group, enabled, default: isDefault }) => ({
        code,
        label,
        group,
        enabled,
        default: isDefault
      })
    );
    const footerText = memberTemplateFooterText.value.trim();
    if (footerText) {
      (rule as any).memberTemplateFooterText = footerText;
    } else {
      delete (rule as any).memberTemplateFooterText;
    }
    (rule as any).memberFieldGroups = {
      author: normalizeSubFieldRows(memberSubFields.author, 'author'),
      participant: normalizeSubFieldRows(memberSubFields.participant, 'member')
    };
    const genericTemplates = serializeGenericTableTemplates(genericTableTemplates.value);
    if (genericTemplates.length) {
      (rule as any).genericTableTemplates = genericTemplates;
    } else {
      delete (rule as any).genericTableTemplates;
    }
    const ruleJson = JSON.stringify(rule);
    if (new TextEncoder().encode(ruleJson).length > 60 * 1024) {
      activeTableConfigMode.value = 'generic';
      proxy?.$modal.msgWarning('类别规则配置过大，请减少表格模板或字段数量');
      return;
    }
    await updateCategory({ ...currentCategory.value, ruleJson });
    proxy?.$modal.msgSuccess('表格模板配置已保存');
    memberFieldDialog.visible = false;
    await loadCategoryList();
    const refreshed = categoryList.value.find((item) => String(item.id) === String(currentCategory.value?.id));
    if (refreshed) {
      currentCategory.value = refreshed;
    }
  } finally {
    memberFieldDialog.saving = false;
  }
};

const parseValidationType = (json?: string) => {
  if (!json) return '';
  try {
    const parsed = JSON.parse(json);
    return parsed?.type || '';
  } catch {
    return '';
  }
};

const inferValidationType = (template: FieldTemplate) => {
  if (template.fieldType === 'id_card') return 'id_card';
  if (template.fieldType === 'number') return 'number';
  if (template.fieldType === 'date') return 'date';
  if (template.fieldKey.includes('phone')) return 'phone';
  if (template.fieldKey.includes('email')) return 'email';
  if (template.fieldKey.includes('status')) return 'status';
  if (template.fieldKey === 'student_name' || template.fieldKey.endsWith('_name')) return 'name';
  if (template.fieldKey === 'gender') return 'gender';
  if (template.fieldKey === 'nation') return 'nation';
  if (template.fieldKey === 'upload_file') return 'file';
  if (template.fieldKey === 'remark') return 'remark';
  return '';
};

const addFieldOption = () => {
  fieldOptions.value.push('');
};

const removeFieldOption = (index: number) => {
  fieldOptions.value.splice(index, 1);
};

const removeField = async (row: CategoryFieldSchemaVO) => {
  const { data: usageCount } = await getFieldUsageCount(row.id!);
  const message =
    usageCount > 0
      ? `字段「${row.fieldLabel}」已有 ${usageCount} 个项目填写了数据。删除后字段会进入已删除列表，历史数据不会被物理删除，但新填报页面不再展示该字段。确认继续？`
      : `确认删除字段「${row.fieldLabel}」？删除后可在“已删除字段”中恢复。`;
  await proxy?.$modal.confirm(message);
  await delField(row.id!);
  proxy?.$modal.msgSuccess('删除成功');
  await loadConfig();
};

const restoreFieldRow = async (row: CategoryFieldSchemaVO) => {
  await proxy?.$modal.confirm(`确认恢复字段「${row.fieldLabel}」？`);
  await restoreField(row.id!);
  proxy?.$modal.msgSuccess('恢复成功');
  await loadConfig();
};

// ART-REF: FE.ACTIVITY_CONFIG.FILE_REQUIREMENT -> components/FileRequirementTab.vue
const openRequirement = (row?: CategoryFileRequirementVO) => {
  if (selectedActivityDeleted.value) {
    proxy?.$modal.msgWarning('已删除活动下的配置为只读，请先恢复活动');
    return;
  }
  const rule = parseJsonObject(row?.ruleJson);
  const legacyRuleMax = Number((rule as any).maxMb);
  const storedMax = Number(row?.maxSizeMb);
  requirementForm.value = {
    categoryId: currentCategory.value?.id,
    fileTypeCode: 'all',
    minCount: 0,
    maxCount: undefined,
    required: false,
    sortOrder: 0,
    ...row,
    maxSizeMb:
      Number.isFinite(storedMax) && storedMax > 0 ? storedMax : Number.isFinite(legacyRuleMax) && legacyRuleMax > 0 ? legacyRuleMax : undefined
  };
  allFormatsAllowed.value = isAllAllowedExt(row?.allowedExt);
  customAllowedExts.value = parseAllowedExts(row?.allowedExt);
  requirementRuleSettings.value = {
    mediaType: (rule as any).mediaType || '',
    technicalCheckMode: (rule as any).technicalCheckMode || '',
    minMb: (rule as any).minMb,
    minWidth: (rule as any).minWidth,
    minHeight: (rule as any).minHeight,
    fps: (rule as any).fps,
    minBitrateMbps: (rule as any).minBitrateMbps,
    minDurationSeconds: (rule as any).minDurationSeconds,
    maxDurationSeconds: (rule as any).maxDurationSeconds
  };
  uploadConfirmationSettings.value = {
    enabled: !!(rule as any).confirmationRequired || !!(rule as any).confirmationText || !!(rule as any).confirmationFieldKey,
    text: (rule as any).confirmationText || defaultUploadConfirmationText,
    textColor: (rule as any).confirmationTextColor || defaultUploadConfirmationTextColor
  };
  requirementTemplateSettings.value = {
    enabled: !!(rule as any).templateEnabled || !!(rule as any).templateOssId,
    name: String((rule as any).templateName || ''),
    ossId: String((rule as any).templateOssId || ''),
    fileName: String((rule as any).templateFileName || '')
  };
  applyRequirementType();
  requirementDialog.title = row?.id ? '修改附件要求' : '新增附件要求';
  requirementDialog.visible = true;
};

const submitRequirement = async () => {
  if (selectedActivityDeleted.value) {
    proxy?.$modal.msgWarning('已删除活动下的配置为只读，请先恢复活动');
    return;
  }
  normalizeRequirementForm();
  requirementForm.value.id ? await updateFileRequirement(requirementForm.value) : await addFileRequirement(requirementForm.value);
  proxy?.$modal.msgSuccess('操作成功');
  requirementDialog.visible = false;
  await loadConfig();
};

const applyRequirementType = () => {
  const code = requirementForm.value.fileTypeCode || 'all';
  const option = fileTypeOptions.find((item) => item.code === code);
  if (!requirementForm.value.fileTypeName || fileTypeOptions.some((item) => item.name === requirementForm.value.fileTypeName)) {
    requirementForm.value.fileTypeName = option?.name || requirementForm.value.fileTypeName || '所有类型';
  }
};

const normalizeRequirementForm = () => {
  requirementForm.value.fileTypeCode = (requirementForm.value.fileTypeCode || 'all').trim().toLowerCase();
  if (!requirementForm.value.fileTypeName) {
    applyRequirementType();
  }
  requirementForm.value.allowedExt = allFormatsAllowed.value ? undefined : normalizeAllowedExts(customAllowedExts.value).join(',') || undefined;
  const minSizeMb = Number(requirementRuleSettings.value.minMb);
  const maxSizeMb = Number(requirementForm.value.maxSizeMb);
  requirementRuleSettings.value.minMb = Number.isFinite(minSizeMb) && minSizeMb > 0 ? minSizeMb : undefined;
  requirementForm.value.maxSizeMb = Number.isFinite(maxSizeMb) && maxSizeMb > 0 ? maxSizeMb : undefined;
  if (
    requirementRuleSettings.value.minMb &&
    requirementForm.value.maxSizeMb &&
    requirementRuleSettings.value.minMb > requirementForm.value.maxSizeMb
  ) {
    proxy?.$modal.msgError('最大文件大小不能小于最小文件大小');
    throw new Error('invalid file size range');
  }
  if (requirementForm.value.ruleJson) {
    try {
      const parsed = JSON.parse(requirementForm.value.ruleJson);
      if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
        throw new Error('ruleJson must be object');
      }
      const rule = mergeRequirementRuleSettings(parsed as Record<string, any>);
      requirementForm.value.ruleJson = Object.keys(rule).length ? JSON.stringify(rule) : '';
    } catch {
      proxy?.$modal.msgError('规则配置必须是合法 JSON 对象');
      throw new Error('invalid ruleJson');
    }
  } else {
    const rule = mergeRequirementRuleSettings({});
    requirementForm.value.ruleJson = Object.keys(rule).length ? JSON.stringify(rule) : '';
  }
};

const mergeRequirementRuleSettings = (base: Record<string, any>) => {
  const rule = { ...base };
  ['mediaType', 'technicalCheckMode', 'minMb', 'minWidth', 'minHeight', 'fps', 'minBitrateMbps', 'minDurationSeconds', 'maxDurationSeconds'].forEach(
    (key) => {
      const value = requirementRuleSettings.value[key];
      if (value === '' || value === undefined || value === null) {
        delete rule[key];
      } else {
        rule[key] = value;
      }
    }
  );
  delete rule.maxMb;
  if (uploadConfirmationSettings.value.enabled) {
    rule.confirmationRequired = true;
    rule.confirmationFieldKey = 'noIdentityInVideo';
    rule.confirmationText = String(uploadConfirmationSettings.value.text || defaultUploadConfirmationText).trim() || defaultUploadConfirmationText;
    rule.confirmationTextColor =
      String(uploadConfirmationSettings.value.textColor || defaultUploadConfirmationTextColor).trim() || defaultUploadConfirmationTextColor;
  } else {
    delete rule.confirmationRequired;
    delete rule.confirmationFieldKey;
    delete rule.confirmationText;
    delete rule.confirmationTextColor;
  }
  if (requirementTemplateSettings.value.enabled && !String(requirementTemplateSettings.value.ossId || '').trim()) {
    proxy?.$modal.msgError('启用下载模板后，请先上传模板文件');
    throw new Error('missing template file');
  }
  if (requirementTemplateSettings.value.enabled && String(requirementTemplateSettings.value.ossId || '').trim()) {
    rule.templateEnabled = true;
    rule.templateOssId = String(requirementTemplateSettings.value.ossId || '').trim();
    const templateName = String(requirementTemplateSettings.value.name || '').trim();
    if (templateName) {
      rule.templateName = templateName;
    } else {
      delete rule.templateName;
    }
    const templateFileName = String(requirementTemplateSettings.value.fileName || '').trim();
    if (templateFileName) {
      rule.templateFileName = templateFileName;
    } else {
      delete rule.templateFileName;
    }
  } else {
    delete rule.templateEnabled;
    delete rule.templateOssId;
    delete rule.templateName;
    delete rule.templateFileName;
  }
  return rule;
};

const beforeRequirementTemplateUpload = (file: File) => {
  const extension =
    String(file.name || '')
      .split('.')
      .pop()
      ?.toLowerCase() || '';
  if (!requirementTemplateFileTypes.includes(extension)) {
    proxy?.$modal.msgError(`模板文件仅支持 ${requirementTemplateFileTypes.join('/')} 格式`);
    return false;
  }
  if (file.size > 50 * 1024 * 1024) {
    proxy?.$modal.msgError('模板文件大小不能超过 50MB');
    return false;
  }
  return true;
};

const uploadRequirementTemplate = async (options: any) => {
  const categoryId = currentCategory.value?.id || requirementForm.value.categoryId;
  if (!categoryId) {
    const error = new Error('请先选择类别');
    proxy?.$modal.msgError(error.message);
    options.onError?.(error);
    return;
  }
  try {
    const response = await uploadFileRequirementTemplate(categoryId, options.file as File);
    const uploaded = response.data;
    requirementTemplateSettings.value.ossId = String(uploaded.ossId || '');
    requirementTemplateSettings.value.fileName = String(uploaded.fileName || options.file?.name || '');
    options.onSuccess?.(uploaded);
    proxy?.$modal.msgSuccess('模板文件上传成功');
  } catch (error) {
    options.onError?.(error);
  }
};

const clearRequirementTemplate = () => {
  requirementTemplateSettings.value.ossId = '';
  requirementTemplateSettings.value.fileName = '';
};

const formatFileType = (row: CategoryFileRequirementVO) => {
  if (!row.fileTypeCode || row.fileTypeCode === 'all') {
    return '所有类型';
  }
  return fileTypeOptions.find((item) => item.code === row.fileTypeCode)?.name || row.fileTypeCode;
};

const formatAllowedExt = (allowedExt?: string) => {
  if (!allowedExt || allowedExt === '*' || allowedExt === 'all') {
    return '所有格式';
  }
  return allowedExt;
};

const isAllAllowedExt = (allowedExt?: string) =>
  !String(allowedExt || '').trim() || ['*', 'all', '所有格式'].includes(String(allowedExt).trim().toLowerCase());

const parseAllowedExts = (allowedExt?: string) => {
  if (isAllAllowedExt(allowedExt)) return [];
  return String(allowedExt)
    .split(/[,，、;；\s]+/)
    .map((item) => item.trim().toLowerCase().replace(/^\./, ''))
    .filter(Boolean);
};

const normalizeAllowedExts = (extensions: string[]) => Array.from(new Set(extensions.flatMap((item) => parseAllowedExts(item))));

const formatFileSizeRange = (row: CategoryFileRequirementVO) => {
  const rule = parseJsonObject(row.ruleJson) as any;
  const min = Number(rule?.minMb);
  const storedMax = Number(row.maxSizeMb);
  const max = Number.isFinite(storedMax) && storedMax > 0 ? storedMax : Number(rule?.maxMb);
  const minText = Number.isFinite(min) && min > 0 ? `≥${min}MB` : '';
  const maxText = Number.isFinite(max) && max > 0 ? `≤${max}MB` : '';
  return minText || maxText ? [minText, maxText].filter(Boolean).join('，') : '不限制';
};

const formatFileCountRange = (row: CategoryFileRequirementVO) => {
  const min = Number(row.minCount);
  const max = Number(row.maxCount);
  const minText = Number.isFinite(min) && min > 0 ? `≥${min}个` : '';
  const maxText = Number.isFinite(max) && max > 0 ? `≤${max}个` : '';
  return minText || maxText ? [minText, maxText].filter(Boolean).join('，') : '不限制';
};

const removeRequirement = async (row: CategoryFileRequirementVO) => {
  const { data: usageCount } = await getFileRequirementUsageCount(row.id!);
  const name = row.fileTypeName || formatFileType(row);
  const message =
    usageCount > 0
      ? `附件要求「${name}」已有 ${usageCount} 个上传文件关联。删除后附件要求会进入已删除列表，历史附件不会被物理删除，但新填报页面不再展示该附件要求。确认继续？`
      : `确认删除附件要求「${name}」？删除后可在“已删除附件要求”中恢复。`;
  await proxy?.$modal.confirm(message);
  await delFileRequirement(row.id!);
  proxy?.$modal.msgSuccess('删除成功');
  await loadConfig();
};

const restoreRequirementRow = async (row: CategoryFileRequirementVO) => {
  const name = row.fileTypeName || formatFileType(row);
  await proxy?.$modal.confirm(`确认恢复附件要求「${name}」？`);
  await restoreFileRequirement(row.id!);
  proxy?.$modal.msgSuccess('恢复成功');
  await loadConfig();
};

onMounted(() => {
  getActivityList().then(async () => {
    const defaultActivity = activityList.value.find((item) => item.status === 'enabled') || activityList.value[0];
    if (defaultActivity) {
      await selectActivity(defaultActivity);
    }
  });
});
</script>

<style scoped>
.activity-query-card :deep(.el-card__body) {
  padding: 14px 18px;
}

.activity-query-summary {
  display: none;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
}

.activity-query-summary-text {
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-regular);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.activity-query-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
}

.activity-query-form :deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 0;
}

.activity-query-form :deep(.el-input),
.activity-query-form :deep(.el-select) {
  width: min(240px, 58vw);
}

.activity-config-layout {
  --activity-main-height: max(520px, calc(100vh - 194px));
  align-items: flex-start;
  height: var(--activity-main-height);
  overflow: hidden;
}

.activity-left-col,
.activity-right-col {
  height: 100%;
  min-height: 0;
}

.activity-left-col {
  display: flex;
  flex-direction: column;
}

.activity-summary-card {
  flex: 0 0 auto;
  margin-bottom: 10px;
}

.activity-summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 700;
}

.activity-summary-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.activity-manager-dialog {
  min-height: 360px;
}

.activity-manager-query {
  margin-bottom: 10px;
}

.activity-manager-query :deep(.el-form-item) {
  margin-bottom: 8px;
}

.config-merge-preview {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.config-merge-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.config-detail-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.config-detail-card :deep(.el-card__body) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.config-table {
  flex: 1 1 auto;
  min-height: 0;
}

.config-detail-card :deep(.operation-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding-right: 6px;
  padding-left: 6px;
}

.config-detail-card :deep(.operation-column .el-button.is-link) {
  min-width: 28px;
  min-height: 28px;
  padding: 4px 6px;
}

.config-tabs {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  margin-top: 0;
}

.config-tabs :deep(.el-tabs__header) {
  flex: 0 0 auto;
}

.config-tabs :deep(.el-tabs__content) {
  flex: 1 1 auto;
  min-height: 0;
}

.config-tab-pane {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.config-tabs :deep(.el-tab-pane) {
  height: 100%;
  min-height: 0;
}

.menu-style-table {
  width: 100%;
}

.category-purge-dialog {
  min-height: 240px;
}

.category-purge-summary,
.category-purge-owned-config,
.category-purge-tabs {
  margin-top: 14px;
}

.category-purge-tab-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.category-purge-limit-alert {
  flex: 1;
  margin-bottom: 10px;
}

.category-purge-tab-tools .category-purge-limit-alert {
  margin-bottom: 0;
}

.category-purge-muted {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.menu-style-preview {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  font-weight: 700;
}

.menu-style-preview .svg-icon {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
  margin: 0;
}

.menu-style-preview span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.config-tab-pane > .field-toolbar .toolbar-more-menu {
  display: none;
}

.config-name-link {
  max-width: 100%;
  height: auto;
  min-height: 0;
  padding: 0;
  font-weight: 400;
  vertical-align: baseline;
}

.config-name-link :deep(span) {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sub-field-panel {
  width: 100%;
}

.form-item-tip {
  margin-left: 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

@media (max-width: 1440px) {
  .activity-query-summary {
    display: flex;
  }

  .config-tab-pane > .field-toolbar .toolbar-secondary-action {
    display: none;
  }

  .config-tab-pane > .field-toolbar .toolbar-more-menu {
    display: inline-flex;
  }

  .activity-query-card:not(.is-collapsed) .activity-query-summary {
    padding-bottom: 10px;
    margin-bottom: 10px;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }
}

@media (min-width: 1200px) and (max-width: 1600px) {
  .activity-config-layout {
    height: auto;
    overflow: visible;
  }

  .activity-left-col,
  .activity-right-col {
    flex: 0 0 100%;
    width: 100%;
    max-width: 100%;
    height: auto;
  }

  .activity-left-col {
    margin-bottom: 10px;
  }

  .config-detail-card {
    height: 560px;
    min-height: 420px;
  }
}

@media (max-width: 1199px) {
  .activity-config-layout {
    height: auto;
    overflow: visible;
  }

  .activity-left-col,
  .activity-right-col {
    height: auto;
  }

  .activity-left-col {
    margin-bottom: 10px;
  }

  .config-detail-card {
    min-height: 420px;
  }

  .config-detail-card {
    height: 560px;
  }
}

@media (max-width: 768px) {
  .activity-query-card :deep(.el-card__body) {
    padding: 12px;
  }

  .activity-query-form {
    display: block;
  }

  .activity-query-form :deep(.el-form-item) {
    margin-bottom: 10px;
  }

  .activity-query-form :deep(.el-input),
  .activity-query-form :deep(.el-select) {
    width: 100%;
  }

  .config-detail-card {
    min-height: 360px;
  }

  .activity-summary-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .activity-summary-actions {
    width: 100%;
    justify-content: flex-start;
  }
}

.count-rule-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.count-rule-input {
  width: 132px;
}

.count-rule-separator {
  color: var(--el-text-color-secondary);
}
</style>
