<template>
  <!-- ART-REF: FE.PROJECT_EDIT.SHELL -->
  <div class="project-edit-page">
    <div class="edit-header">
      <div>
        <span class="edit-title">{{ pageTitle }}</span>
        <el-tag v-if="form.status" class="ml-2" :type="statusType(form.status)">{{ statusLabel(form.status) }}</el-tag>
      </div>
      <div class="edit-actions">
        <el-button v-if="editable" type="primary" icon="DocumentChecked" :loading="saving" @click="saveDraft">保存草稿</el-button>
        <el-button v-if="editable && form.id" type="success" icon="UploadFilled" :loading="submitting" :disabled="saving" @click="submitCurrent">提交</el-button>
        <el-button v-if="!viewMode && canWithdrawSubmit(form.status) && form.id" type="warning" icon="RefreshLeft" :loading="withdrawing" :disabled="saving || submitting" @click="withdrawSubmitCurrent">
          撤回提交
        </el-button>
      </div>
    </div>

    <el-alert v-if="form.status === 'returned' && form.currentAuditOpinion" class="mb-2" type="warning" :closable="false" :title="form.currentAuditOpinion" />
    <el-alert v-if="!form.id && managedActivityError" class="mb-2" type="warning" :closable="false" :title="managedActivityError" />
    <el-alert v-else-if="contextResolveError" class="mb-2" type="warning" :closable="false" :title="contextResolveError" />
    <el-alert
      v-if="form.id && !categoryBusinessAvailable"
      class="mb-2"
      type="warning"
      :closable="false"
      title="所属类别或大类已停用，当前项目仅可查看，不能继续编辑、上传或提交。"
    />

    <el-tabs v-model="activeSubTab" class="project-edit-tabs">
      <el-tab-pane label="填报" name="form">
        <!-- ART-REF: FE.PROJECT_EDIT.DYNAMIC_FORM -> components/ProjectDynamicForm.vue -->
        <section class="edit-section form-section">
          <el-form class="report-form" :class="{ 'report-form--wide': formLayoutMode !== 'single' }" :model="form" label-width="150px" :disabled="!editable">
            <el-row :gutter="12">
              <el-col v-if="!hideActivityCategoryControls" :span="12" :xs="24" :sm="24" :md="12">
                <el-form-item label="活动">
                  <el-input :model-value="managedActivity?.activityName || '管理员尚未配置当前活动'" disabled />
                </el-form-item>
              </el-col>
              <el-col v-if="!hideActivityCategoryControls" :span="12" :xs="24" :sm="24" :md="12">
                <el-form-item label="类别">
                  <el-select v-model="form.categoryId" class="w-full" placeholder="请选择类别" :disabled="!!form.id || categoryContextLocked" @change="handleCategoryChange">
                    <el-option v-for="item in categoryOptions" :key="item.id" :label="item.categoryName" :value="item.id!" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="projectNameColumnSpan" :xs="24" :sm="24" :md="projectNameColumnSpan">
                <el-form-item :label="projectNameLabel" :required="projectNameRequired">
                  <el-input v-model="form.projectName" :style="projectNameInputStyle" />
                </el-form-item>
              </el-col>
              <el-col v-if="groupOptions.length" :span="groupColumnSpan" :xs="24" :sm="24" :md="groupColumnSpan">
                <el-form-item label="组别" required>
                  <el-radio-group v-model="form.groupCode" @change="handleGroupChange">
                    <el-radio v-for="item in groupOptions" :key="item.groupCode" :label="item.groupCode!">{{ item.groupName || item.groupCode }}</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col
                v-for="field in fieldList"
                :key="field.id || field.fieldKey"
                :span="dynamicFieldSpan(field)"
                :xs="24"
                :sm="24"
                :md="dynamicFieldSpan(field)"
              >
                <el-form-item :label="field.fieldLabel" :required="field.required" :error="fieldValidationError(field)">
                  <div class="dynamic-field-block" :style="fieldDisplayStyle(field)">
                    <div v-if="field.fieldType === 'teacher_group'" class="teacher-group-editor">
                      <div v-for="(_, index) in teacherGroupRows(field)" :key="index" class="teacher-row">
                        <template v-for="subField in teacherGroupFields(field)" :key="subField.fieldKey">
                          <el-input
                            v-if="subField.fieldType === 'textarea'"
                            v-model="teacherGroupRows(field)[index][subField.fieldKey]"
                            class="teacher-sub-field"
                            type="textarea"
                            :rows="2"
                            :placeholder="subField.fieldLabel"
                          />
                          <el-select
                            v-else-if="optionFieldTypes.includes(subField.fieldType || '')"
                            v-model="teacherGroupRows(field)[index][subField.fieldKey]"
                            class="teacher-sub-field"
                            :multiple="subField.fieldType === 'checkbox'"
                            :placeholder="subField.fieldLabel"
                          >
                            <el-option v-for="option in memberFieldOptions(subField)" :key="option" :label="option" :value="option" />
                          </el-select>
                          <el-date-picker
                            v-else-if="subField.fieldType === 'date'"
                            v-model="teacherGroupRows(field)[index][subField.fieldKey]"
                            class="teacher-sub-field"
                            value-format="YYYY-MM-DD"
                            :placeholder="subField.fieldLabel"
                          />
                          <el-input-number
                            v-else-if="subField.fieldType === 'number'"
                            v-model="teacherGroupRows(field)[index][subField.fieldKey]"
                            class="teacher-sub-field"
                            controls-position="right"
                          />
                          <el-input v-else v-model="teacherGroupRows(field)[index][subField.fieldKey]" class="teacher-sub-field" :placeholder="subField.fieldLabel" />
                        </template>
                        <el-button v-if="editable" link type="danger" icon="Delete" @click="removeTeacherRow(field, Number(index))">删除</el-button>
                      </div>
                      <el-button
                        v-if="editable && teacherGroupRows(field).length < teacherGroupLimit(field)"
                        class="teacher-add-button"
                        size="small"
                        type="primary"
                        plain
                        icon="Plus"
                        @click="addTeacherRow(field)"
                      >
                        添加
                      </el-button>
                    </div>
                    <div v-else-if="fieldRenderType(field) === 'dimension_group'" class="dimension-group-field">
                      <template v-for="(item, index) in dimensionItems(field)" :key="item.key">
                        <span class="dimension-item-label">{{ item.label }}</span>
                        <el-input
                          class="dimension-input"
                          :model-value="dimensionPartValue(field, item.key)"
                          :placeholder="item.placeholder || item.label"
                          @update:model-value="(value: any) => setDimensionPartValue(field, item.key, value)"
                        />
                        <span v-if="item.unit" class="dimension-unit" :style="fieldSuffixStyle(field)">{{ item.unit }}</span>
                        <span v-if="index < dimensionItems(field).length - 1" class="dimension-separator" :style="fieldSuffixStyle(field)">×</span>
                      </template>
                    </div>
                    <div v-else-if="fieldRenderType(field) === 'textarea'" class="field-control-with-suffix" :style="fieldControlStyle(field)">
                      <el-input
                        v-model="formData[field.fieldKey!]"
                        type="textarea"
                        :rows="3"
                        :placeholder="fieldPlaceholder(field)"
                        :maxlength="fieldTextMaxLength(field)"
                        :minlength="fieldTextMinLength(field)"
                        :show-word-limit="fieldShowWordLimit(field)"
                      />
                      <span v-if="fieldSuffixText(field)" class="field-suffix-text" :style="fieldSuffixStyle(field)">{{ fieldSuffixText(field) }}</span>
                    </div>
                    <div v-else-if="fieldRenderType(field) === 'select'" class="field-control-with-suffix" :style="fieldControlStyle(field)">
                      <el-select v-model="formData[field.fieldKey!]" class="w-full" popper-class="art-dynamic-select-dropdown" :placeholder="fieldPlaceholder(field)">
                        <el-option v-for="option in parseOptions(field.optionsJson)" :key="option" :label="option" :value="option" />
                      </el-select>
                      <span v-if="fieldSuffixText(field)" class="field-suffix-text" :style="fieldSuffixStyle(field)">{{ fieldSuffixText(field) }}</span>
                    </div>
                    <div v-else-if="fieldRenderType(field) === 'radio'" class="field-control-with-suffix" :style="fieldControlStyle(field)">
                      <div class="dynamic-radio-field">
                        <template v-if="hasCascadeOptions(field)">
                          <div class="cascade-radio-field">
                            <el-radio-group :model-value="cascadeRadioPartValue(field, 0)" @update:model-value="(value: any) => setCascadeRadioPartValue(field, 0, value)">
                              <el-radio v-for="option in cascadeLevelOptions(field, 0)" :key="option" :label="option">{{ option }}</el-radio>
                            </el-radio-group>
                            <el-radio-group
                              v-if="cascadeLevelOptions(field, 1).length"
                              :model-value="cascadeRadioPartValue(field, 1)"
                              @update:model-value="(value: any) => setCascadeRadioPartValue(field, 1, value)"
                            >
                              <el-radio v-for="option in cascadeLevelOptions(field, 1)" :key="option" :label="option">{{ option }}</el-radio>
                            </el-radio-group>
                            <el-radio-group
                              v-if="cascadeLevelOptions(field, 2).length"
                              :model-value="cascadeRadioPartValue(field, 2)"
                              @update:model-value="(value: any) => setCascadeRadioPartValue(field, 2, value)"
                            >
                              <el-radio v-for="option in cascadeLevelOptions(field, 2)" :key="option" :label="option">{{ option }}</el-radio>
                            </el-radio-group>
                          </div>
                        </template>
                        <template v-else>
                          <el-radio-group v-model="formData[field.fieldKey!]" :class="{ 'topic-radio-grid': topicCategoryField(field) }">
                            <el-radio v-for="option in parseOptions(field.optionsJson)" :key="option" :label="option">{{ option }}</el-radio>
                          </el-radio-group>
                          <span v-if="originalityHintVisible(field)" class="originality-hint">（改编、创作作品不属于原创）</span>
                        </template>
                      </div>
                      <span v-if="fieldSuffixText(field)" class="field-suffix-text" :style="fieldSuffixStyle(field)">{{ fieldSuffixText(field) }}</span>
                    </div>
                    <div v-else-if="fieldRenderType(field) === 'checkbox'" class="field-control-with-suffix" :style="fieldControlStyle(field)">
                      <el-checkbox-group v-model="formData[field.fieldKey!]">
                        <el-checkbox v-for="option in parseOptions(field.optionsJson)" :key="option" :label="option">{{ option }}</el-checkbox>
                      </el-checkbox-group>
                      <span v-if="fieldSuffixText(field)" class="field-suffix-text" :style="fieldSuffixStyle(field)">{{ fieldSuffixText(field) }}</span>
                    </div>
                    <div v-else-if="fieldRenderType(field) === 'date'" class="field-control-with-suffix" :style="fieldControlStyle(field)">
                      <el-date-picker v-model="formData[field.fieldKey!]" value-format="YYYY-MM-DD" class="w-full" :placeholder="fieldPlaceholder(field)" />
                      <span v-if="fieldSuffixText(field)" class="field-suffix-text" :style="fieldSuffixStyle(field)">{{ fieldSuffixText(field) }}</span>
                    </div>
                    <div v-else-if="fieldRenderType(field) === 'duration'" class="field-control-with-suffix" :style="fieldControlStyle(field)">
                      <el-time-picker
                        v-if="durationMode(field) === 'hms'"
                        v-model="formData[field.fieldKey!]"
                        value-format="HH:mm:ss"
                        format="HH:mm:ss"
                        class="w-full"
                        :placeholder="fieldPlaceholder(field) || '时:分:秒'"
                      />
                      <div v-else-if="durationMode(field) === 'ms'" class="duration-part-field">
                        <el-input-number
                          :model-value="durationPartValue(field, 'minutes')"
                          :min="0"
                          controls-position="right"
                          @update:model-value="(value: any) => setDurationPartValue(field, 'minutes', value)"
                        />
                        <span>分</span>
                        <el-input-number
                          :model-value="durationPartValue(field, 'seconds')"
                          :min="0"
                          :max="59"
                          controls-position="right"
                          @update:model-value="(value: any) => setDurationPartValue(field, 'seconds', value)"
                        />
                        <span>秒</span>
                      </div>
                      <div v-else class="duration-part-field">
                        <el-input-number
                          :model-value="durationPartValue(field, durationMode(field) === 'minute' ? 'minutes' : 'seconds')"
                          :min="0"
                          controls-position="right"
                          @update:model-value="(value: any) => setDurationPartValue(field, durationMode(field) === 'minute' ? 'minutes' : 'seconds', value)"
                        />
                        <span>{{ durationMode(field) === 'minute' ? '分钟' : '秒' }}</span>
                      </div>
                      <span v-if="fieldSuffixText(field)" class="field-suffix-text" :style="fieldSuffixStyle(field)">{{ fieldSuffixText(field) }}</span>
                    </div>
                    <div v-else-if="fieldRenderType(field) === 'number'" class="field-control-with-suffix" :style="fieldControlStyle(field)">
                      <el-input-number
                        v-model="formData[field.fieldKey!]"
                        class="w-full"
                        :placeholder="fieldPlaceholder(field)"
                        :min="fieldNumberMin(field)"
                        :max="fieldNumberMax(field)"
                        :precision="fieldNumberPrecision(field)"
                        :step="fieldNumberStep(field)"
                      />
                      <span v-if="fieldSuffixText(field)" class="field-suffix-text" :style="fieldSuffixStyle(field)">{{ fieldSuffixText(field) }}</span>
                    </div>
                    <div v-else class="field-control-with-suffix" :style="fieldControlStyle(field)">
                      <el-input
                        v-model="formData[field.fieldKey!]"
                        :placeholder="fieldPlaceholder(field)"
                        :maxlength="fieldTextMaxLength(field)"
                        :minlength="fieldTextMinLength(field)"
                        :show-word-limit="fieldShowWordLimit(field)"
                      />
                      <span v-if="fieldSuffixText(field)" class="field-suffix-text" :style="fieldSuffixStyle(field)">{{ fieldSuffixText(field) }}</span>
                    </div>
                    <div v-if="fieldHelpText(field)" class="field-help-text" :style="fieldHelpStyle(field)">{{ fieldHelpText(field) }}</div>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </section>

        <!-- ART-REF: FE.PROJECT_EDIT.MATERIAL_UPLOAD -> components/ProjectMaterialUpload.vue -->
        <section class="edit-section">
          <div class="section-toolbar">
            <span class="section-title">{{ uploadSectionTitle }}</span>
            <span class="section-subtitle">按当前类别要求上传作品材料，上传后可在“作品预览”中查看。</span>
          </div>
          <div v-if="requirementList.length" class="work-upload-panel">
            <el-alert
              class="work-upload-tip"
              type="info"
              :closable="false"
              show-icon
              title="选择文件后会自动上传；上传过程中请勿关闭页面，若选错文件可在进度条旁点击“取消上传”。"
            />
            <div class="work-requirement-list">
              <div v-for="requirement in requirementList" :key="requirement.id" class="work-requirement-item">
                <div class="work-requirement-title">
                  <span v-if="requirement.required" class="work-required-mark">*</span>
                  <span>{{ requirement.fileTypeName }}</span>
                  <span class="work-help-mark" :title="workRequirementHint(requirement) || requirementSummary(requirement)">?</span>
                </div>
                <div class="work-upload-action">
                  <el-button
                    v-if="requirementTemplateEnabled(requirement)"
                    class="work-upload-button"
                    type="primary"
                    plain
                    icon="Download"
                    @click="downloadRequirementTemplate(requirement)"
                  >
                    {{ requirementTemplateName(requirement) }}
                  </el-button>
                  <el-upload
                    v-if="editable && canUploadRequirement(requirement)"
                    class="work-inline-upload"
                    :show-file-list="false"
                    :accept="acceptExt(requirement.allowedExt)"
                    :before-upload="(file: any) => beforeUploadMaterial(requirement, file)"
                    :http-request="(option: any) => uploadMaterial(requirement.id, option)"
                  >
                    <el-button class="work-upload-button" type="primary" plain icon="Plus" :disabled="isRequirementUploading(requirement.id)">
                      {{ uploadButtonText(requirement) }}
                    </el-button>
                  </el-upload>
                  <el-button v-else class="work-upload-button is-disabled" plain disabled icon="Plus">
                    {{ editable ? '已达到上传数量上限' : '当前状态不可上传' }}
                  </el-button>
                  <el-tag v-if="filesByRequirement(requirement.id).length" size="small" type="success" effect="plain">
                    已上传 {{ filesByRequirement(requirement.id).length }} 个
                  </el-tag>
                </div>
                <div class="work-requirement-description">
                  {{ workRequirementHint(requirement) || requirementSummary(requirement) }}
                </div>
                <div v-if="uploadErrorFor(requirement.id)" class="work-upload-error" :title="uploadErrorFor(requirement.id)?.message">
                  {{ uploadErrorFor(requirement.id)?.message }}
                </div>
                <div
                  v-if="uploadConfirmationRequired(requirement)"
                  class="work-upload-confirmation"
                  :class="{ 'is-error': uploadConfirmationError(requirement) }"
                  :style="{ '--upload-confirmation-text-color': uploadConfirmationTextColor(requirement) }"
                >
                  <el-checkbox
                    :model-value="uploadConfirmationChecked(requirement)"
                    :disabled="!editable"
                    @change="(value) => setUploadConfirmationChecked(requirement, value)"
                  >
                    {{ uploadConfirmationText(requirement) }}
                  </el-checkbox>
                  <div v-if="uploadConfirmationError(requirement)" class="work-upload-confirmation-error">
                    {{ uploadConfirmationError(requirement) }}
                  </div>
                </div>
                <div v-if="uploadProgressFor(requirement.id)" class="work-upload-progress">
                  <div class="upload-progress-line">
                    <span class="upload-progress-title">{{ uploadProgressFor(requirement.id)?.phaseText || uploadProgressFor(requirement.id)?.fileName || '文件上传中' }}</span>
                    <span class="upload-progress-percent">{{ uploadProgressFor(requirement.id)?.percent || 0 }}%</span>
                    <el-button v-if="canCancelUpload(requirement.id)" class="upload-cancel-button" size="small" link type="danger" icon="Close" @click="cancelUpload(requirement.id)">
                      取消上传
                    </el-button>
                  </div>
                  <el-progress :percentage="uploadProgressFor(requirement.id)?.percent || 0" :stroke-width="6" :show-text="false" />
                </div>
                <div v-if="filesByRequirement(requirement.id).length" class="work-file-section">
                  <div class="work-file-section-title">已上传文件</div>
                  <div class="work-file-list">
                    <div v-for="file in filesByRequirement(requirement.id)" :key="file.id" class="work-file-chip">
                      <div class="work-file-main">
                        <el-icon class="work-file-icon" :class="resourceFileIconClass(file)">
                          <component :is="resourceFileIcon(file)" />
                        </el-icon>
                        <button class="work-file-name" type="button" :title="file.originalName" @click="openMaterialPreview(file)">
                          {{ file.originalName }}
                        </button>
                      </div>
                      <div class="work-file-actions">
                        <el-button class="work-file-action" size="small" link type="primary" icon="View" @click.stop="openMaterialPreview(file)">预览</el-button>
                        <el-button v-if="editable" class="work-file-action" size="small" link type="danger" icon="Delete" @click.stop="removeFile(file)">删除</el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <el-empty v-else class="work-upload-empty" description="当前类别暂无作品附件要求" />
        </section>

        <!-- ART-REF: FE.PROJECT_EDIT.MEMBER_TABLE -> components/ProjectMemberTable.vue -->
        <section v-if="memberTableEnabled" class="edit-section member-section">
          <div class="member-card-head">
            <div class="member-toolbar-row">
              <div class="member-leading-actions">
                <span class="member-section-title">{{ memberTableTitle }}</span>
                <el-button class="member-template-button" type="primary" icon="Download" @click="downloadMemberTemplate">
                  下载采集表
                </el-button>
                <div class="member-import-actions">
                  <el-upload
                    v-if="showAppendMemberImport && editable"
                    :show-file-list="false"
                    accept=".xls,.xlsx"
                    :http-request="(option: any) => importMembers(option, false)"
                  >
                    <el-button class="member-tool-button" type="primary" plain icon="Upload">补充导入</el-button>
                  </el-upload>
                  <el-upload
                    v-if="editable"
                    :show-file-list="false"
                    accept=".xls,.xlsx"
                    :http-request="(option: any) => importMembers(option, true)"
                  >
                    <el-button class="member-tool-button" type="primary" plain icon="Upload">采集表导入</el-button>
                  </el-upload>
                </div>
              </div>
              <div class="member-right-actions">
                <el-button v-if="editable" class="member-tool-button" type="primary" plain icon="Plus" @click="addMember()">添加行</el-button>
                <template v-if="editable">
                  <el-button v-for="field in memberAttachmentFields" :key="`batch-${field.fieldKey}`" class="member-tool-button" plain icon="Upload" @click="openMemberAttachmentBatch(field)">
                    批量上传{{ field.fieldLabel }}
                  </el-button>
                </template>
                <el-button class="member-tool-button" type="primary" plain icon="FullScreen" @click="memberDrawerVisible = true">
                  全屏查看编辑
                </el-button>
              </div>
            </div>
            <span class="member-tip-inline">{{ memberTipLabel }}：{{ memberTipText }}</span>
          </div>
          <input ref="memberAttachmentBatchInput" class="member-attachment-file-input" type="file" multiple :accept="memberBatchAttachmentAccept" @change="handleMemberAttachmentBatchFiles" />
          <el-table class="member-table" border :data="memberList" height="260" table-layout="fixed">
            <el-table-column v-if="editable" label="操作" width="78" align="center" fixed="left">
              <template #default="scope">
                <el-button link type="danger" icon="Delete" @click="removeMember(scope.$index)">删除</el-button>
              </template>
            </el-table-column>
            <el-table-column label="类型" width="112">
              <template #default="scope">
                <el-select v-model="scope.row.memberType" :disabled="!editable">
                  <el-option v-for="item in memberTypeOptions" :key="item.code" :label="item.label" :value="item.code" :disabled="!item.enabled && item.code !== scope.row.memberType" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column
              v-for="field in memberTableFields"
              :key="field.fieldKey"
              :label="memberFieldLabel(field)"
              :min-width="memberFieldColumnWidth(field)"
            >
              <template #default="scope">
                <span v-if="!memberFieldApplies(scope.row, field)" class="member-not-applicable">-</span>
                <el-input
                  v-else-if="field.fieldType === 'textarea'"
                  :model-value="memberFieldValue(scope.row, field)"
                  type="textarea"
                  :rows="2"
                  :disabled="!editable"
                  @update:model-value="(value: any) => setMemberFieldValue(scope.row, field, value)"
                />
                <el-select
                  v-else-if="field.fieldType === 'select'"
                  :model-value="memberFieldValue(scope.row, field)"
                  class="w-full"
                  :disabled="!editable"
                  @update:model-value="(value: any) => setMemberFieldValue(scope.row, field, value)"
                >
                  <el-option v-for="option in memberFieldOptions(field)" :key="option" :label="option" :value="option" />
                </el-select>
                <el-radio-group
                  v-else-if="field.fieldType === 'radio'"
                  :model-value="memberFieldValue(scope.row, field)"
                  :disabled="!editable"
                  @update:model-value="(value: any) => setMemberFieldValue(scope.row, field, value)"
                >
                  <el-radio v-for="option in memberFieldOptions(field)" :key="option" :label="option">{{ option }}</el-radio>
                </el-radio-group>
                <el-checkbox-group
                  v-else-if="field.fieldType === 'checkbox'"
                  :model-value="memberFieldValue(scope.row, field) || []"
                  :disabled="!editable"
                  @update:model-value="(value: any) => setMemberFieldValue(scope.row, field, value)"
                >
                  <el-checkbox v-for="option in memberFieldOptions(field)" :key="option" :label="option">{{ option }}</el-checkbox>
                </el-checkbox-group>
                <el-date-picker
                  v-else-if="field.fieldType === 'date'"
                  :model-value="memberFieldValue(scope.row, field)"
                  value-format="YYYY-MM-DD"
                  class="w-full"
                  :disabled="!editable"
                  @update:model-value="(value: any) => setMemberFieldValue(scope.row, field, value)"
                />
                <el-input-number
                  v-else-if="field.fieldType === 'number'"
                  :model-value="memberFieldValue(scope.row, field)"
                  class="w-full"
                  :disabled="!editable"
                  @update:model-value="(value: any) => setMemberFieldValue(scope.row, field, value)"
                />
                <el-input
                  v-else
                  :model-value="memberFieldValue(scope.row, field)"
                  :disabled="!editable"
                  @update:model-value="(value: any) => setMemberFieldValue(scope.row, field, value)"
                />
              </template>
            </el-table-column>
            <el-table-column v-for="field in memberAttachmentFields" :key="field.fieldKey" :label="memberFieldLabel(field)" min-width="132" align="center">
              <template #default="scope">
                <span v-if="!memberFieldApplies(scope.row, field)" class="member-not-applicable">-</span>
                <template v-else>
                  <el-tag v-if="memberAttachmentUploaded(scope.row, field)" type="success" effect="plain">已上传</el-tag>
                  <el-tag v-else type="info" effect="plain">未上传</el-tag>
                  <el-upload
                    v-if="editable && form.id && scope.row.id"
                    class="member-upload"
                    :show-file-list="false"
                    :accept="memberAttachmentAccept(field)"
                    :http-request="(option: any) => uploadMemberAttachment(scope.row, field, option)"
                  >
                    <el-button size="small" link type="primary">上传</el-button>
                  </el-upload>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="editable" class="member-table-footer">
            <el-button type="primary" plain icon="Plus" @click="addMember()">添加行</el-button>
          </div>
          <el-alert
            v-if="memberBatchAttachmentSummary"
            class="member-batch-result"
            :type="memberBatchAttachmentSkipped ? 'warning' : 'success'"
            :closable="true"
            :title="memberBatchAttachmentSummary"
            :description="memberBatchAttachmentDetails"
            show-icon
            @close="clearMemberBatchAttachmentResult"
          />
        </section>

        <!-- ART-REF: FE.PROJECT_EDIT.MEMBER_TABLE -> components/ProjectMemberTable.vue -->
        <ArtPopupDrawer
          v-if="memberTableEnabled"
          v-model="memberDrawerVisible"
          append-to-body
          class="member-drawer"
          modal-class="member-drawer-modal"
          direction="rtl"
          size="90%"
          :with-header="false"
          :close-on-click-modal="true"
          :guard-unsaved="false"
        >
          <div class="member-drawer-shell">
            <div class="member-drawer-header">
              <div class="member-drawer-title-group">
                <el-tooltip content="关闭" placement="bottom">
                  <el-button class="member-close-button" circle icon="Close" aria-label="关闭" @click="memberDrawerVisible = false" />
                </el-tooltip>
                <div class="member-drawer-title-content">
                  <span class="member-drawer-title">{{ memberTableTitle }}</span>
                  <span class="member-drawer-subtitle">共 {{ memberList.length }} 人，可横向滚动查看和编辑全部字段</span>
                </div>
              </div>
              <div class="member-drawer-actions">
                <el-button class="member-template-button" type="primary" icon="Download" @click="downloadMemberTemplate">
                  下载采集表
                </el-button>
                <el-upload
                  v-if="showAppendMemberImport && editable"
                  :show-file-list="false"
                  accept=".xls,.xlsx"
                  :http-request="(option: any) => importMembers(option, false)"
                >
                  <el-button class="member-tool-button" type="primary" plain icon="Upload">补充导入</el-button>
                </el-upload>
                <el-upload
                  v-if="editable"
                  :show-file-list="false"
                  accept=".xls,.xlsx"
                  :http-request="(option: any) => importMembers(option, true)"
                >
                  <el-button class="member-tool-button" type="primary" plain icon="Upload">采集表导入</el-button>
                </el-upload>
                <el-button v-if="editable" class="member-tool-button" type="primary" plain icon="Plus" @click="addMember()">添加行</el-button>
                <template v-if="editable">
                  <el-button v-for="field in memberAttachmentFields" :key="`drawer-batch-${field.fieldKey}`" class="member-tool-button" plain icon="Upload" @click="openMemberAttachmentBatch(field)">
                    批量上传{{ field.fieldLabel }}
                  </el-button>
                </template>
              </div>
            </div>
            <div class="member-drawer-body">
              <span class="member-tip-inline member-drawer-tip">{{ memberTipLabel }}：{{ memberTipText }}</span>
              <el-table class="member-table member-drawer-table" border :data="memberList" height="calc(100vh - 184px)" table-layout="fixed">
                <el-table-column v-if="editable" label="操作" width="78" align="center" fixed="left">
                  <template #default="scope">
                    <el-button link type="danger" icon="Delete" @click="removeMember(scope.$index)">删除</el-button>
                  </template>
                </el-table-column>
                <el-table-column label="类型" width="112">
                  <template #default="scope">
                    <el-select v-model="scope.row.memberType" :disabled="!editable">
                      <el-option v-for="item in memberTypeOptions" :key="item.code" :label="item.label" :value="item.code" :disabled="!item.enabled && item.code !== scope.row.memberType" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column
                  v-for="field in memberTableFields"
                  :key="field.fieldKey"
                  :label="memberFieldLabel(field)"
                  :min-width="memberFieldColumnWidth(field)"
                >
                  <template #default="scope">
                    <span v-if="!memberFieldApplies(scope.row, field)" class="member-not-applicable">-</span>
                    <el-input
                      v-else-if="field.fieldType === 'textarea'"
                      :model-value="memberFieldValue(scope.row, field)"
                      type="textarea"
                      :rows="2"
                      :disabled="!editable"
                      @update:model-value="(value: any) => setMemberFieldValue(scope.row, field, value)"
                    />
                    <el-select
                      v-else-if="field.fieldType === 'select'"
                      :model-value="memberFieldValue(scope.row, field)"
                      class="w-full"
                      :disabled="!editable"
                      @update:model-value="(value: any) => setMemberFieldValue(scope.row, field, value)"
                    >
                      <el-option v-for="option in memberFieldOptions(field)" :key="option" :label="option" :value="option" />
                    </el-select>
                    <el-radio-group
                      v-else-if="field.fieldType === 'radio'"
                      :model-value="memberFieldValue(scope.row, field)"
                      :disabled="!editable"
                      @update:model-value="(value: any) => setMemberFieldValue(scope.row, field, value)"
                    >
                      <el-radio v-for="option in memberFieldOptions(field)" :key="option" :label="option">{{ option }}</el-radio>
                    </el-radio-group>
                    <el-checkbox-group
                      v-else-if="field.fieldType === 'checkbox'"
                      :model-value="memberFieldValue(scope.row, field) || []"
                      :disabled="!editable"
                      @update:model-value="(value: any) => setMemberFieldValue(scope.row, field, value)"
                    >
                      <el-checkbox v-for="option in memberFieldOptions(field)" :key="option" :label="option">{{ option }}</el-checkbox>
                    </el-checkbox-group>
                    <el-date-picker
                      v-else-if="field.fieldType === 'date'"
                      :model-value="memberFieldValue(scope.row, field)"
                      value-format="YYYY-MM-DD"
                      class="w-full"
                      :disabled="!editable"
                      @update:model-value="(value: any) => setMemberFieldValue(scope.row, field, value)"
                    />
                    <el-input-number
                      v-else-if="field.fieldType === 'number'"
                      :model-value="memberFieldValue(scope.row, field)"
                      class="w-full"
                      :disabled="!editable"
                      @update:model-value="(value: any) => setMemberFieldValue(scope.row, field, value)"
                    />
                    <el-input
                      v-else
                      :model-value="memberFieldValue(scope.row, field)"
                      :disabled="!editable"
                      @update:model-value="(value: any) => setMemberFieldValue(scope.row, field, value)"
                    />
                  </template>
                </el-table-column>
                <el-table-column v-for="field in memberAttachmentFields" :key="field.fieldKey" :label="memberFieldLabel(field)" min-width="132" align="center">
                  <template #default="scope">
                    <span v-if="!memberFieldApplies(scope.row, field)" class="member-not-applicable">-</span>
                    <template v-else>
                      <el-tag v-if="memberAttachmentUploaded(scope.row, field)" type="success" effect="plain">已上传</el-tag>
                      <el-tag v-else type="info" effect="plain">未上传</el-tag>
                      <el-upload
                        v-if="editable && form.id && scope.row.id"
                        class="member-upload"
                        :show-file-list="false"
                        :accept="memberAttachmentAccept(field)"
                        :http-request="(option: any) => uploadMemberAttachment(scope.row, field, option)"
                      >
                        <el-button size="small" link type="primary">上传</el-button>
                      </el-upload>
                    </template>
                  </template>
                </el-table-column>
              </el-table>
              <div v-if="editable" class="member-table-footer">
                <el-button type="primary" plain icon="Plus" @click="addMember()">添加行</el-button>
              </div>
            </div>
          </div>
        </ArtPopupDrawer>

        <!-- ART-REF: FE.PROJECT_EDIT.GENERIC_TABLE -> components/ProjectGenericTables.vue -->
        <ProjectGenericTables
          v-model="genericTableData"
          :templates="genericTableTemplates"
          :editable="editable"
          :project-id="form.id"
          :category-id="form.categoryId"
          :ensure-project="ensureProjectForGenericTable"
          :persist="saveGenericTableImport"
        />
      </el-tab-pane>

      <el-tab-pane label="作品预览" name="files">
        <!-- ART-REF: FE.PROJECT_EDIT.FILE_PREVIEW -> components/ProjectFilePreview.vue -->
        <div class="attachment-layout">
          <el-alert class="mb-2" type="info" :closable="false" show-icon title="点击左侧已上传作品可在右侧预览；Office 文件转换中时请稍后刷新查看。上传入口在填报页的上传作品区域。" />
        </div>

        <section class="project-workspace">
          <aside class="material-panel">
            <div class="resource-title">
              <el-icon><Document /></el-icon>
              <span>资源列表</span>
            </div>
            <el-scrollbar class="material-scroll">
              <div v-for="requirement in requirementList" :key="requirement.id" class="resource-group">
                <div class="resource-group-head" @click="selectRequirement(requirement)">
                  <span class="resource-group-name">{{ requirement.fileTypeName }}</span>
                </div>
                <div v-if="uploadProgressFor(requirement.id)" class="resource-upload-progress">
                  <div class="upload-progress-line">
                    <span class="upload-progress-title">{{ uploadProgressFor(requirement.id)?.phaseText || uploadProgressFor(requirement.id)?.fileName || '文件上传中' }}</span>
                    <span class="upload-progress-percent">{{ uploadProgressFor(requirement.id)?.percent || 0 }}%</span>
                    <el-button v-if="canCancelUpload(requirement.id)" class="upload-cancel-button" size="small" link type="danger" icon="Close" @click.stop="cancelUpload(requirement.id)">
                      取消上传
                    </el-button>
                  </div>
                  <el-progress :percentage="uploadProgressFor(requirement.id)?.percent || 0" :stroke-width="6" :show-text="false" />
                </div>
                <div class="resource-file-list">
                  <button
                    v-for="file in filesByRequirement(requirement.id)"
                    :key="file.id"
                    class="resource-file-row"
                    :class="{ active: String(file.id) === String(selectedFileId) }"
                    type="button"
                    @click.stop="selectFile(file)"
                    @dblclick.stop="openPreview(file)"
                  >
                    <el-icon class="resource-file-icon" :class="resourceFileIconClass(file)">
                      <component :is="resourceFileIcon(file)" />
                    </el-icon>
                    <span class="resource-file-name">{{ file.originalName }}</span>
                    <span v-if="file.checkStatus && file.checkStatus !== 'passed'" class="resource-status-dot" :class="`is-${file.checkStatus}`" :title="normalizeReviewMessage(file.checkMessage) || checkStatusLabel(file.checkStatus)" />
                    <span v-if="file.previewStatus" class="resource-preview-bars" :class="`is-${file.previewStatus}`" :title="normalizePreviewMessage(file.previewMessage, file.previewStatus)">
                      <i></i><i></i><i></i>
                    </span>
                  </button>
                  <div v-if="!filesByRequirement(requirement.id).length" class="resource-empty-row" @click="selectRequirement(requirement)">
                    暂无文件
                  </div>
                </div>
              </div>
              <el-empty v-if="!requirementList.length" description="暂无材料要求" />
            </el-scrollbar>
          </aside>

      <main class="preview-panel">
        <div class="preview-toolbar">
          <div>
            <div class="preview-title">{{ selectedRequirement?.fileTypeName || '材料预览' }}</div>
            <div v-if="selectedFile" class="preview-subtitle">{{ selectedFile.originalName }}</div>
          </div>
          <div class="preview-actions">
            <el-button v-if="selectedFile && previewOpenUrl(selectedFile)" size="small" type="primary" plain icon="View" @click="openPreview(selectedFile)">
              打开预览
            </el-button>
            <el-button v-else-if="selectedFile && isPendingOfficePreview(selectedFile)" size="small" type="primary" plain icon="Refresh" @click="refreshPreviewStatus(selectedFile)">
              刷新状态
            </el-button>
            <el-button v-else-if="selectedFile && canRequestPreview(selectedFile)" size="small" type="primary" plain icon="View" @click="generatePreview(selectedFile)">
              {{ previewActionLabel(selectedFile) }}
            </el-button>
            <el-button v-if="selectedFile && selectedFile.storagePath" size="small" icon="Download" @click="openOriginalFile(selectedFile)">
              下载原文件
            </el-button>
            <el-button v-if="editable && selectedFile" size="small" type="danger" plain icon="Delete" @click="removeFile(selectedFile)">删除</el-button>
          </div>
        </div>

        <div class="rule-panel">
          <div v-if="categoryTip" class="tip-block">
            <div class="tip-title">页面提示</div>
            <div class="tip-content">{{ categoryTip }}</div>
          </div>
          <div class="rule-grid">
            <el-tag v-for="item in selectedRuleDetails" :key="item" effect="plain">{{ item }}</el-tag>
          </div>
          <div v-if="selectedManualTips.length" class="tip-block manual">
            <div class="tip-title">人工审核要点</div>
            <div v-for="tip in selectedManualTips" :key="tip" class="tip-content">{{ tip }}</div>
          </div>
        </div>

        <div class="preview-body">
          <template v-if="selectedFile">
            <video v-if="isVideoFile(selectedFile) && filePreviewUrl(selectedFile)" class="media-viewer" :src="filePreviewUrl(selectedFile)" controls preload="metadata" />
            <img v-else-if="isImageFile(selectedFile) && filePreviewUrl(selectedFile)" class="image-viewer" :src="filePreviewUrl(selectedFile)" :alt="selectedFile.originalName || '图片预览'" />
            <div v-else-if="canInlinePreview(selectedFile)" class="pdf-viewer-shell">
              <div class="pdf-toolbar">
                <span>PDF 预览</span>
                <div>
                  <el-button size="small" icon="Printer" @click="printPdfPreview(selectedFile)">打印 PDF</el-button>
                  <el-button size="small" icon="Download" @click="downloadPdfPreview(selectedFile)">下载 PDF</el-button>
                </div>
              </div>
              <iframe class="doc-viewer" :src="pdfViewerUrl(selectedFile)" />
            </div>
            <div v-else class="preview-state">
              <el-icon><Document /></el-icon>
              <div>{{ previewFallbackText(selectedFile) }}</div>
              <el-button v-if="previewOpenUrl(selectedFile)" type="primary" plain icon="View" @click="openPreview(selectedFile)">打开查看</el-button>
              <el-button v-else-if="isPendingOfficePreview(selectedFile)" type="primary" plain icon="Refresh" @click="refreshPreviewStatus(selectedFile)">刷新状态</el-button>
              <el-button v-else-if="canRequestPreview(selectedFile)" type="primary" plain icon="View" @click="generatePreview(selectedFile)">{{ previewActionLabel(selectedFile) }}</el-button>
            </div>
          </template>
          <div v-else class="preview-state">
            <el-icon><View /></el-icon>
            <div>请选择左侧已上传文件</div>
          </div>
        </div>

        <div v-if="selectedFile" class="tech-panel">
          <span>技术校验：{{ normalizeReviewMessage(selectedFile.checkMessage) || checkStatusLabel(selectedFile.checkStatus) }}</span>
          <span v-if="selectedFile.previewStatus">预览转换：{{ normalizePreviewMessage(selectedFile.previewMessage, selectedFile.previewStatus) }}</span>
          <span v-if="formatFileSize(selectedFile.fileSize)">大小：{{ formatFileSize(selectedFile.fileSize) }}</span>
          <span v-if="selectedFile.width && selectedFile.height">分辨率：{{ selectedFile.width }}×{{ selectedFile.height }}</span>
          <span v-if="selectedFile.durationSeconds">时长：{{ formatDuration(selectedFile.durationSeconds) }}</span>
          <span v-if="selectedFile.fps">帧率：{{ Number(selectedFile.fps).toFixed(2) }}fps</span>
          <span v-if="selectedFile.bitrate">码率：{{ formatBitrate(selectedFile.bitrate) }}</span>
          <span v-if="selectedFile.dpi">DPI：{{ selectedFile.dpi }}</span>
        </div>
      </main>
        </section>
      </el-tab-pane>
    </el-tabs>

    <!-- ART-REF: FE.PROJECT_EDIT.FILE_PREVIEW -> components/ProjectFilePreview.vue -->
    <ArtPopupDialog
      v-model="materialPreviewVisible"
      class="material-preview-dialog"
      :title="materialPreviewFile?.originalName || '文件预览'"
      width="88%"
      top="6vh"
      append-to-body
      destroy-on-close
      @closed="handleMaterialPreviewClosed"
    >
      <div v-loading="materialPreviewLoading" class="preview-body material-preview-body">
        <template v-if="materialPreviewFile">
          <video
            v-if="isVideoFile(materialPreviewFile) && filePreviewUrl(materialPreviewFile)"
            class="media-viewer"
            :src="filePreviewUrl(materialPreviewFile)"
            controls
            preload="metadata"
          />
          <img
            v-else-if="isImageFile(materialPreviewFile) && filePreviewUrl(materialPreviewFile)"
            class="image-viewer"
            :src="filePreviewUrl(materialPreviewFile)"
            :alt="materialPreviewFile.originalName || '图片预览'"
          />
          <div v-else-if="canInlinePreview(materialPreviewFile)" class="pdf-viewer-shell">
            <div class="pdf-toolbar">
              <span>PDF 预览</span>
              <div>
                <el-button size="small" icon="Printer" @click="printPdfPreview(materialPreviewFile)">打印 PDF</el-button>
                <el-button size="small" icon="Download" @click="downloadPdfPreview(materialPreviewFile)">下载 PDF</el-button>
              </div>
            </div>
            <iframe class="doc-viewer" :src="pdfViewerUrl(materialPreviewFile)" />
          </div>
          <div v-else class="preview-state">
            <el-icon><Document /></el-icon>
            <div>{{ previewFallbackText(materialPreviewFile) }}</div>
            <el-button v-if="isPendingOfficePreview(materialPreviewFile)" type="primary" plain icon="Refresh" @click="refreshPreviewStatus(materialPreviewFile)">刷新状态</el-button>
            <el-button v-else-if="canRequestPreview(materialPreviewFile)" type="primary" plain icon="View" @click="generatePreview(materialPreviewFile)">
              {{ previewActionLabel(materialPreviewFile) }}
            </el-button>
            <el-button v-if="materialPreviewFile.storagePath" plain icon="Download" @click="openOriginalFile(materialPreviewFile)">打开原文件</el-button>
          </div>
        </template>
        <div v-else class="preview-state">
          <el-icon><View /></el-icon>
          <div>文件不存在或已被删除</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="materialPreviewVisible = false">关闭</el-button>
      </template>
    </ArtPopupDialog>

    <!-- ART-REF: FE.PROJECT_EDIT.LEAVE_GUARD -> composables/useProjectLeaveGuard.ts -->
    <ProjectLeaveConfirmDialog
      v-model="leaveConfirm.visible"
      :message="leaveConfirm.message"
      :saving="saving"
      @action="resolveLeaveConfirm"
    />
  </div>
</template>

<script setup name="ArtProjectEdit" lang="ts">
// ART-REF: FE.PROJECT_EDIT.CONTEXT -> composables/useProjectEditContext.ts
// ART-REF: FE.PROJECT_EDIT.RULE_UTILS -> utils/projectRule.ts
import { listAvailableActivity, listAvailableCategory, listSchoolCategoryCatalog, listSchoolField, listSchoolFileRequirement } from '@/api/crehn/activity';
import { listActivityReportRuleSchoolOptions } from '@/api/crehn/config';
import {
  batchUploadProjectMemberAttachment,
  completeProjectFileDirectUpload,
  deleteProjectFile,
  downloadProjectMemberTemplate,
  getProject,
  importProjectMembers,
  initProjectFileDirectUpload,
  saveProjectDraft,
  submitProject,
  uploadProjectFileToOss,
  uploadProjectMemberAttachment,
  withdrawSubmitProject
} from '@/api/crehn/project';
import {
  ActivityCategoryVO,
  ActivityRuleGroupOptionVO,
  ActivityVO,
  CategoryFieldSchemaVO,
  CategoryFileRequirementVO,
  MemberAttachmentBatchResultVO,
  ProjectFileVO,
  ProjectMemberVO,
  ProjectSaveForm,
  ProjectVO
} from '@/api/crehn/types';
import { Document, Picture, VideoPlay, View } from '@element-plus/icons-vue';
import { useManagedArtActivity } from '@/composables/useManagedArtActivity';
import { useTagsViewStore } from '@/store/modules/tagsView';
import { normalizePreviewMessage as normalizeArtPreviewMessage, normalizeReviewMessage } from '@/utils/artReviewMessage';
import { isCategoryBusinessAvailable, isCategoryGroup } from '@/utils/artCategory';
import {
  GENERIC_TABLE_DATA_KEY,
  GenericTableData,
  GenericTableTemplate,
  normalizeGenericTableTemplates,
  validateGenericTableData
} from '@/utils/artGenericTable';
import {
  isMemberAttachmentType,
  memberAttachmentDefaultExts,
  memberDefaultType,
  memberTypeGroup,
  normalizeMemberTypeOptions
} from '@/utils/artMemberTable';
import { blobValidate } from '@/utils/ruoyi';
import FileSaver from 'file-saver';
import ProjectGenericTables from './components/ProjectGenericTables.vue';
import ProjectLeaveConfirmDialog from './components/ProjectLeaveConfirmDialog.vue';
import { loadArtDetailDisplayConfig } from '../components/artDetailDisplayConfig';
import { useRoute, useRouter } from 'vue-router';
import { useProjectFilePreview } from './composables/useProjectFilePreview';
import { useProjectLeaveGuard } from './composables/useProjectLeaveGuard';

type RuleJson = Record<string, any>;
type FormLayoutMode = 'single' | 'double' | 'custom';
type FormLayoutConfig = {
  mode: FormLayoutMode;
  fieldSpans: Record<string, 12 | 24>;
};
type ElTagType = 'primary' | 'success' | 'warning' | 'danger' | 'info';
type UploadProgressState = {
  percent: number;
  fileName: string;
  loaded: number;
  total: number;
  phaseText?: string;
  cancelable?: boolean;
};
type UploadErrorState = {
  message: string;
  fileName?: string;
};
type LocalVideoMetadata = {
  readable?: boolean;
  durationSeconds?: number;
  width?: number;
  height?: number;
  bitrate?: number;
};
type DimensionItem = {
  key: string;
  label: string;
  placeholder?: string;
  unit?: string;
  required?: boolean;
};
type CascadeOptionSchema = {
  level1: string;
  level2?: string;
  level3?: string;
};
type MemberFieldSchema = {
  fieldKey: string;
  fieldLabel: string;
  fieldType?: string;
  required?: boolean;
  optionsJson?: string;
  optionsList?: string[];
  validationJson?: string;
  sortOrder?: number;
  groups?: string[];
};

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const route = useRoute();
const router = useRouter();
const tagsViewStore = useTagsViewStore();

const activityOptions = ref<ActivityVO[]>([]);
const { managedActivity, managedActivityError, resolveManagedActivityId } = useManagedArtActivity(activityOptions);
const categoryOptions = ref<ActivityCategoryVO[]>([]);
const fieldList = ref<CategoryFieldSchemaVO[]>([]);
const requirementList = ref<CategoryFileRequirementVO[]>([]);
const groupOptions = ref<ActivityRuleGroupOptionVO[]>([]);
const categoryTip = ref('');
const categoryBusinessAvailable = ref(true);
const fileList = ref<ProjectFileVO[]>([]);
const memberList = ref<ProjectMemberVO[]>([]);
const memberAttachmentBatchInput = ref<HTMLInputElement>();
const memberBatchAttachmentField = ref<MemberFieldSchema>();
const memberBatchAttachmentResult = ref<MemberAttachmentBatchResultVO>();
const formData = ref<Record<string, any>>({});
const form = ref<ProjectVO>({ status: 'draft' });
const activeSubTab = ref('form');
const selectedRequirementId = ref<string | number>();
const selectedFileId = ref<string | number>();
const uploadProgressMap = ref<Record<string, UploadProgressState>>({});
const uploadErrorMap = ref<Record<string, UploadErrorState>>({});
const categoryFieldErrors = ref<Record<string, string>>({});
const uploadConfirmationErrors = ref<Record<string, string>>({});
const uploadAbortControllerMap = new Map<string, AbortController>();
const lastSavedSnapshot = ref('');
const memberDrawerVisible = ref(false);
const saving = ref(false);
const submitting = ref(false);
const withdrawing = ref(false);
const contextResolveError = ref('');
const optionFieldTypes = ['select', 'radio', 'checkbox'];
const showAppendMemberImport = false;
const defaultMemberTipText = '每个节目或作品单独导入师生信息采集表；采集表导入会先清空当前人员后导入。删除人员请在列表中操作。';

const pageTitle = computed(() => {
  if (form.value.projectName) return form.value.projectName;
  return route.params.id ? '查看项目' : '新增项目';
});
const routeQueryText = (value: unknown) => (Array.isArray(value) ? String(value[0] || '') : String(value || ''));
const viewMode = computed(() => routeQueryText(route.query.mode) === 'view');
const editable = computed(() => !viewMode.value && canEdit(form.value.status) && categoryBusinessAvailable.value);
const selectedRequirement = computed(() => requirementList.value.find((item) => String(item.id) === String(selectedRequirementId.value)));
const selectedFile = computed(() => fileList.value.find((item) => String(item.id) === String(selectedFileId.value)));
const selectedRuleDetails = computed(() => (selectedRequirement.value ? ruleDetails(selectedRequirement.value) : []));
const selectedManualTips = computed(() => manualTips(selectedRequirement.value));
const hasUnsavedChanges = computed(() => snapshotPayload(buildSaveData()) !== lastSavedSnapshot.value);
const hasActiveUploads = computed(() => Object.values(uploadProgressMap.value).some((item) => item.percent < 100));
const shouldConfirmClose = computed(() => editable.value);
const routeCategoryLockRequested = computed(() => routeQueryText(route.query.lockCategory) === '1' || Boolean(route.query.categoryId || route.query.categoryCode));
const categoryContextLocked = computed(() => routeCategoryLockRequested.value && Boolean(form.value.categoryId) && !form.value.id);
const hideActivityCategoryControls = computed(() => !!form.value.id || categoryContextLocked.value);
const currentCategoryName = computed(() => {
  const category = categoryOptions.value.find((item) => String(item.id) === String(form.value.categoryId));
  return category?.categoryName || form.value.categoryName || routeQueryText(route.query.categoryName) || '';
});
const currentCategoryTitleLabel = computed(() => {
  const categoryName = currentCategoryName.value.trim();
  if (categoryName) return categoryName;
  const categoryId = routeQueryText(form.value.categoryId || route.query.categoryId);
  if (categoryId) return `类别${categoryId}`;
  const projectId = routeQueryText(route.params.id);
  return projectId ? `项目${projectId}` : '';
});
const categoryRule = computed<RuleJson>(() => parseRuleJson(currentCategoryRuleJson.value));
const genericTableTemplates = computed<GenericTableTemplate[]>(() => normalizeGenericTableTemplates(categoryRule.value.genericTableTemplates));
const emptyGenericTableData: GenericTableData = {};
const genericTableData = computed<GenericTableData>({
  get: () => (formData.value[GENERIC_TABLE_DATA_KEY] && typeof formData.value[GENERIC_TABLE_DATA_KEY] === 'object'
    ? formData.value[GENERIC_TABLE_DATA_KEY] as GenericTableData
    : emptyGenericTableData),
  set: (value) => {
    formData.value[GENERIC_TABLE_DATA_KEY] = value;
  }
});
const formLayoutConfig = computed<FormLayoutConfig>(() => normalizeFormLayoutConfig(categoryRule.value.formLayout));
const formLayoutMode = computed(() => formLayoutConfig.value.mode);
const dynamicFieldSpanMap = computed<Record<string, 12 | 24>>(() => buildDynamicFieldSpanMap());
const memberTableEnabled = computed(() => categoryRule.value.memberTableEnabled !== false);
const memberTableTitle = computed(() => String(categoryRule.value.memberTableTitle || '').trim() || '成员信息');
const memberTipLabel = computed(() => String(categoryRule.value.memberTipLabel || '').trim() || '备注');
const memberTipText = computed(() => String(categoryRule.value.memberTipText || '').trim() || defaultMemberTipText);
const memberTypeOptions = computed(() => normalizeMemberTypeOptions(categoryRule.value.memberTypeOptions));
const defaultMemberType = computed(() => memberDefaultType(categoryRule.value.memberTypeOptions));
const memberBatchAttachmentAccept = computed(() => (memberBatchAttachmentField.value ? memberAttachmentAccept(memberBatchAttachmentField.value) : ''));
const memberBatchAttachmentSummary = computed(() => {
  const result = memberBatchAttachmentResult.value;
  if (!result) return '';
  return `批量上传完成：成功匹配 ${result.matchedCount || 0} 个，未处理 ${result.skippedCount || 0} 个`;
});
const memberBatchAttachmentSkipped = computed(() => Number(memberBatchAttachmentResult.value?.skippedCount || 0) > 0);
const memberBatchAttachmentDetails = computed(() => (memberBatchAttachmentResult.value?.items || [])
  .filter((item) => item.status !== 'success')
  .map((item) => `${item.fileName || '未命名文件'}：${item.message || '未匹配'}`)
  .join('；'));
const currentCategoryRuleJson = computed(() => {
  const category = categoryOptions.value.find((item) => String(item.id) === String(form.value.categoryId));
  return form.value.categoryRuleJson || category?.ruleJson || '';
});
const projectNameConfig = computed(() => {
  const value = categoryRule.value.projectName;
  return value && typeof value === 'object' && !Array.isArray(value) ? value : {};
});
const projectNameLabel = computed(() => String(projectNameConfig.value.label || '项目名称'));
const projectNameRequired = computed(() => !!projectNameConfig.value.required);
const projectNameWidthMode = computed(() => normalizeInputWidthMode(projectNameConfig.value.inputWidthMode));
const projectNameInputWidth = computed(() => {
  const mode = projectNameWidthMode.value;
  return mode === 'custom' ? normalizeCustomInputWidth(projectNameConfig.value.inputWidthCustom) : inputWidthByMode(mode);
});
const PROJECT_NAME_LAYOUT_KEY = '__projectName';
const projectNameLayoutSpan = computed<12 | 24>(() => {
  if (formLayoutMode.value === 'double') return 12;
  if (formLayoutMode.value === 'custom') {
    const configured = formLayoutConfig.value.fieldSpans[PROJECT_NAME_LAYOUT_KEY];
    return configured === 24 ? 24 : 12;
  }
  return projectNameWidthMode.value === 'full' ? 24 : groupOptions.value.length ? 12 : 24;
});
const projectNameCanPair = computed(() => {
  if (groupOptions.value.length) return true;
  const firstField = fieldList.value[0];
  return !!firstField && preferredDynamicFieldSpan(firstField) === 12;
});
const projectNameColumnSpan = computed<12 | 24>(() => projectNameLayoutSpan.value === 12 && !projectNameCanPair.value ? 24 : projectNameLayoutSpan.value);
const groupColumnSpan = computed<12 | 24>(() => (projectNameColumnSpan.value === 12 ? 12 : 24));
const projectNameInputStyle = computed(() => {
  const width = projectNameInputWidth.value;
  return width && width !== '100%' ? { width, maxWidth: '100%' } : {};
});
const projectNameMissingMessage = computed(() => `请先填写${projectNameLabel.value}后再提交`);
const uploadSectionTitle = computed(() => String(form.value.projectName || '').trim() || projectNameLabel.value);
const configuredMemberFields = computed(() => {
  const groups = categoryRule.value.memberFieldGroups || {};
  return {
    author: normalizeMemberFieldSchemas(groups.author, 'author'),
    participant: normalizeMemberFieldSchemas(groups.participant, 'participant')
  };
});
const hasConfiguredMemberFields = computed(() => configuredMemberFields.value.author.length > 0 || configuredMemberFields.value.participant.length > 0);
const memberTableFields = computed<MemberFieldSchema[]>(() => {
  const map = new Map<string, MemberFieldSchema>();
  const fields = hasConfiguredMemberFields.value
    ? [...configuredMemberFields.value.author, ...configuredMemberFields.value.participant]
    : guideMemberFields();
  fields.forEach((field) => {
    if (memberUploadField(field)) return;
    const existing = map.get(field.fieldKey);
    if (existing) {
      existing.groups = [...new Set([...(existing.groups || []), ...(field.groups || [])])];
      existing.required = !!existing.required || !!field.required;
      return;
    }
    map.set(field.fieldKey, { ...field });
  });
  return [...map.values()].sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
});
const memberAttachmentFields = computed<MemberFieldSchema[]>(() => {
  const source = hasConfiguredMemberFields.value
    ? [...configuredMemberFields.value.author, ...configuredMemberFields.value.participant]
    : [
        { fieldKey: 'photo', fieldLabel: '证件照', fieldType: 'image_upload', groups: ['participant'], sortOrder: 12 },
        { fieldKey: 'studentReport', fieldLabel: '学籍报告', fieldType: 'pdf_upload', groups: ['participant'], sortOrder: 13 }
      ];
  const map = new Map<string, MemberFieldSchema>();
  source.filter((field) => memberUploadField(field)).forEach((field) => {
    const existing = map.get(field.fieldKey);
    if (existing) {
      existing.groups = [...new Set([...(existing.groups || []), ...(field.groups || [])])];
      existing.required = !!existing.required || !!field.required;
      return;
    }
    map.set(field.fieldKey, { ...field });
  });
  return [...map.values()].sort((left, right) => (left.sortOrder || 0) - (right.sortOrder || 0));
});

const initPage = async () => {
  const projectId = route.params.id as string | undefined;
  if (projectId) {
    const { data } = await getProject(projectId);
    applyProject(data);
    activityOptions.value = [{ id: data.activityId, activityName: data.activityName || `活动 ${data.activityId}` }];
    try {
      const categoryRes = await listSchoolCategoryCatalog(data.activityId!);
      const catalog = categoryRes.data || [];
      const selectedCategory = catalog.find((item) => String(item.id) === String(data.categoryId));
      categoryBusinessAvailable.value = !!selectedCategory && isCategoryBusinessAvailable(selectedCategory, catalog);
      categoryOptions.value = catalog.filter((item) => !isCategoryGroup(item) && isCategoryBusinessAvailable(item, catalog));
      if (selectedCategory && !categoryOptions.value.some((item) => String(item.id) === String(selectedCategory.id))) {
        categoryOptions.value.push(selectedCategory);
      }
    } catch {
      categoryBusinessAvailable.value = false;
    }
    if (!categoryOptions.value.some((item) => String(item.id) === String(data.categoryId))) {
      categoryOptions.value.push({
        id: data.categoryId,
        activityId: data.activityId,
        categoryName: data.categoryName || `类别 ${data.categoryId}`,
        ruleJson: data.categoryRuleJson,
        enabled: false
      });
    }
    const currentCategory = categoryOptions.value.find((item) => String(item.id) === String(data.categoryId));
    categoryTip.value = currentCategory?.tipText || '';
    await loadGroupOptions(data.activityId, data.categoryId);
  } else {
    categoryBusinessAvailable.value = true;
    await loadArtDetailDisplayConfig(true);
    const activityRes = await listAvailableActivity();
    activityOptions.value = activityRes.data;
    form.value = { status: 'draft', activityId: resolveManagedActivityId() };
    const categoryId = routeQueryText(route.query.categoryId);
    const categoryCode = routeQueryText(route.query.categoryCode);
    const categoryName = routeQueryText(route.query.categoryName);
    const categoryKeyword = routeQueryText(route.query.categoryKeyword || route.query.keyword);
    const categoryGroup = routeQueryText(route.query.categoryGroup);
    if (categoryId || categoryCode || categoryName || categoryKeyword || categoryGroup || form.value.activityId) {
      await applyRouteCategoryContext({ categoryId, categoryCode, categoryName, categoryKeyword, categoryGroup });
    }
  }
  selectFirstAvailableMaterial();
  syncRouteTabTitle();
  lastSavedSnapshot.value = snapshotPayload(buildSaveData());
};

const applyProject = (data: ProjectVO) => {
  clearAllUploadErrors();
  form.value = data;
  formData.value = parseJsonObject(data.formDataJson);
  fileList.value = data.files || [];
  memberList.value = (data.members || []).map(normalizeMemberExtra);
  fieldList.value = data.fieldSchemas || [];
  requirementList.value = data.fileRequirements || [];
  const category = categoryOptions.value.find((item) => String(item.id) === String(data.categoryId));
  categoryTip.value = data.categoryTipText || category?.tipText || '';
  normalizeFormDataForFields();
  ensureMemberDefaultRow();
};

const syncRouteTabTitle = () => {
  const categoryLabel = currentCategoryTitleLabel.value.trim();
  if (!categoryLabel) return;
  const title = `${categoryLabel}填报`;
  route.meta.title = title;
  if (routeQueryText(route.query.title) === title) return;
  router.replace({ path: route.path, query: { ...route.query, title } }).catch(() => {});
};

const handleCategoryChange = async () => {
  clearAllUploadErrors();
  categoryBusinessAvailable.value = true;
  formData.value = {};
  memberList.value = [];
  const category = categoryOptions.value.find((item) => String(item.id) === String(form.value.categoryId));
  categoryTip.value = category?.tipText || '';
  form.value.groupCode = undefined;
  form.value.groupName = undefined;
  await loadSchema(form.value.categoryId);
  await loadGroupOptions(form.value.activityId, form.value.categoryId);
  ensureMemberDefaultRow();
  selectFirstAvailableMaterial();
  syncRouteTabTitle();
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
  if (!groupOptions.value.some((item) => item.groupCode === form.value.groupCode)) {
    form.value.groupCode = undefined;
    form.value.groupName = undefined;
  }
};

const handleGroupChange = () => {
  const option = groupOptions.value.find((item) => item.groupCode === form.value.groupCode);
  form.value.groupName = option?.groupName || option?.groupCode;
};

const buildSaveData = (): ProjectSaveForm => ({
  id: form.value.id,
  activityId: form.value.activityId,
  categoryId: form.value.categoryId,
  projectName: form.value.projectName,
  groupCode: groupOptions.value.length ? form.value.groupCode : undefined,
  groupName: groupOptions.value.length ? form.value.groupName : undefined,
  formDataJson: JSON.stringify(formData.value),
  members: memberTableEnabled.value ? normalizeMembers() : []
});

const saveDraft = async () => {
  if (saving.value) return;
  saving.value = true;
  try {
    const res = await saveProjectDraft(buildSaveData());
    applyProject(res.data);
    lastSavedSnapshot.value = snapshotPayload(buildSaveData());
    proxy?.$modal.msgSuccess('草稿已保存');
  } finally {
    saving.value = false;
  }
};

const refreshCurrentProject = async () => {
  if (!form.value.id) return;
  const keepFileId = selectedFileId.value;
  const { data } = await getProject(form.value.id);
  applyProject(data);
  if (keepFileId && fileList.value.some((item) => String(item.id) === String(keepFileId))) {
    selectedFileId.value = keepFileId;
  } else {
    selectFirstAvailableMaterial();
  }
  lastSavedSnapshot.value = snapshotPayload(buildSaveData());
};

const refreshProjectFiles = async () => {
  if (!form.value.id) return;
  const keepFileId = selectedFileId.value;
  const { data } = await getProject(form.value.id);
  fileList.value = data.files || [];
  if (keepFileId && fileList.value.some((item) => String(item.id) === String(keepFileId))) {
    selectedFileId.value = keepFileId;
  } else {
    selectFirstAvailableMaterial();
  }
};

// ART-REF: FE.PROJECT_EDIT.MATERIAL_UPLOAD -> composables/useProjectMaterials.ts
const setUploadProgress = (key: string, value: UploadProgressState) => {
  uploadProgressMap.value = { ...uploadProgressMap.value, [key]: value };
};

const clearUploadProgress = (key: string) => {
  const next = { ...uploadProgressMap.value };
  delete next[key];
  uploadProgressMap.value = next;
};

const uploadProgressFor = (requirementId?: string | number) => {
  if (requirementId === undefined || requirementId === null) return undefined;
  return uploadProgressMap.value[String(requirementId)];
};

const isRequirementUploading = (requirementId?: string | number) => {
  const progress = uploadProgressFor(requirementId);
  return !!progress && progress.percent < 100;
};

const canCancelUpload = (requirementId?: string | number) => {
  if (requirementId === undefined || requirementId === null) return false;
  const key = String(requirementId);
  const controller = uploadAbortControllerMap.get(key);
  return !!controller && !controller.signal.aborted && !!uploadProgressMap.value[key]?.cancelable;
};

const cancelUpload = (requirementId?: string | number) => {
  if (requirementId === undefined || requirementId === null) return;
  const key = String(requirementId);
  const controller = uploadAbortControllerMap.get(key);
  if (!controller || controller.signal.aborted) return;
  const current = uploadProgressMap.value[key];
  setUploadProgress(key, {
    percent: current?.percent || 0,
    fileName: current?.fileName || '文件上传中',
    loaded: current?.loaded || 0,
    total: current?.total || 0,
    phaseText: '正在取消上传...',
    cancelable: false
  });
  controller.abort();
};

const cancelAllUploads = () => {
  uploadAbortControllerMap.forEach((controller) => {
    if (!controller.signal.aborted) {
      controller.abort();
    }
  });
  uploadAbortControllerMap.clear();
};

// ART-REF: FE.PROJECT_EDIT.LEAVE_GUARD -> composables/useProjectLeaveGuard.ts
const { skipLeaveConfirm, leaveConfirm, resolveLeaveConfirm } = useProjectLeaveGuard({
  hasUnsavedChanges,
  shouldConfirmClose,
  hasActiveUploads,
  saveDraft,
  cancelAllUploads,
  closeCurrentProjectTab: async () => {
    await tagsViewStore.delView(route as any);
  },
  warn: (message) => proxy?.$modal.msgWarning(message)
});

const createUploadCanceledError = () => {
  const error = new Error('上传已取消');
  error.name = 'CanceledError';
  (error as any).code = 'ERR_CANCELED';
  return error;
};

const throwIfUploadCanceled = (controller: AbortController) => {
  if (controller.signal.aborted) {
    throw createUploadCanceledError();
  }
};

const isUploadCanceledError = (error: unknown) => {
  const err = error as any;
  const message = String(err?.message || '').toLowerCase();
  return err?.code === 'ERR_CANCELED' || err?.name === 'CanceledError' || err?.name === 'AbortError' || message.includes('cancel') || message.includes('取消');
};

const ensureProjectForGenericTable = async () => {
  if (!form.value.id) await saveDraft();
  if (!form.value.id) throw new Error('请先保存项目后再导入表格');
  return form.value.id;
};

const saveGenericTableImport = async () => {
  await saveDraft();
};

const uploadButtonText = (requirement: CategoryFileRequirementVO) => {
  if (isRequirementUploading(requirement.id)) return '上传中';
  return filesByRequirement(requirement.id).length ? '继续上传' : '点击上传';
};

const waitForUploadCheck = (ms: number) => new Promise((resolve) => window.setTimeout(resolve, ms));

const isUploadTimeoutError = (error: unknown) => {
  const err = error as any;
  const message = String(err?.code || err?.message || err?.response?.data?.msg || '').toLowerCase();
  return message.includes('timeout') || message.includes('ecconnaborted');
};

const uploadedMaterialMatches = (item: ProjectFileVO, requirementId: string | number, file: File, existingFileIds: Set<string>) => {
  if (!item?.id || existingFileIds.has(String(item.id))) return false;
  if (String(item.requirementId) !== String(requirementId)) return false;
  if (item.originalName && file?.name && item.originalName !== file.name) return false;
  const remoteSize = Number(item.fileSize || 0);
  return !remoteSize || !file?.size || remoteSize === file.size;
};

const confirmUploadSavedAfterTimeout = async (requirementId: string | number, file: File, existingFileIds: Set<string>) => {
  for (let index = 0; index < 3; index += 1) {
    await waitForUploadCheck(2000);
    await refreshProjectFiles();
    const uploaded = filesByRequirement(requirementId).find((item) => uploadedMaterialMatches(item, requirementId, file, existingFileIds));
    if (uploaded) {
      return uploaded;
    }
  }
  return undefined;
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

const fileSizeLimits = (requirement: CategoryFileRequirementVO) => {
  const rule = parseRuleJson(requirement.ruleJson);
  const minMb = positiveRuleNumber(rule.minMb);
  const maxValues = [positiveRuleNumber(requirement.maxSizeMb), positiveRuleNumber(rule.maxMb)].filter((item): item is number => item !== undefined);
  return { minMb, maxMb: maxValues.length ? Math.min(...maxValues) : undefined };
};

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

const videoExts = ['mp4', 'mov', 'mpg', 'mpeg', 'avi', 'mkv', 'webm'];
const browserStrictVideoExts = ['mp4', 'm4v', 'webm'];

const positiveRuleNumber = (value: unknown) => {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 ? number : undefined;
};

const hasLocalVideoTechnicalRule = (rule: RuleJson) =>
  ['minWidth', 'minHeight', 'minDurationSeconds', 'maxDurationSeconds', 'minBitrateMbps'].some((key) => positiveRuleNumber(rule[key]));

const technicalTipsOnlyMode = (value: unknown) => {
  const normalized = String(value || '').toLowerCase();
  return ['tip', 'tips', 'manual', 'warn', 'format_only', '提示'].some((item) => normalized.includes(item));
};

const requirementTechnicalTipsOnly = (requirement: CategoryFileRequirementVO) => {
  const rule = parseRuleJson(requirement.ruleJson);
  return technicalTipsOnlyMode(rule.technicalCheckMode) || technicalTipsOnlyMode(rule.mediaTechnicalCheckMode) || technicalTipsOnlyMode(categoryRule.value.mediaTechnicalCheckMode);
};

const technicalReviewMessage = (message: string) => message.replace(/^上传失败：/, '技术参数需人工复核：');

const isVideoRequirement = (requirement: CategoryFileRequirementVO, file: File) => {
  const rule = parseRuleJson(requirement.ruleJson);
  const mediaType = String(rule.mediaType || '').toLowerCase();
  if (mediaType === 'video') return true;
  if (String(file.type || '').toLowerCase().startsWith('video/')) return true;
  const ext = fileNameExt(file);
  if (videoExts.includes(ext)) return true;
  const allowedExts = normalizeAllowedExts(requirement.allowedExt);
  if (allowedExts.some((item) => videoExts.includes(item))) return true;
  return /video|视频/i.test(`${requirement.fileTypeCode || ''} ${requirement.fileTypeName || ''}`);
};

const readLocalVideoMetadata = (file: File): Promise<LocalVideoMetadata> => {
  return new Promise((resolve) => {
    const video = document.createElement('video');
    const url = URL.createObjectURL(file);
    let settled = false;
    const finish = (metadata: LocalVideoMetadata = { readable: false }) => {
      if (settled) return;
      settled = true;
      window.clearTimeout(timer);
      video.removeAttribute('src');
      video.load();
      URL.revokeObjectURL(url);
      resolve(metadata);
    };
    const timer = window.setTimeout(() => finish(), 8000);
    video.preload = 'metadata';
    video.muted = true;
    video.playsInline = true;
    video.onloadedmetadata = () => {
      const durationSeconds = Number.isFinite(video.duration) && video.duration > 0 ? video.duration : undefined;
      finish({
        readable: true,
        durationSeconds,
        width: video.videoWidth || undefined,
        height: video.videoHeight || undefined,
        bitrate: durationSeconds ? Math.round((file.size * 8) / durationSeconds) : undefined
      });
    };
    video.onerror = () => finish();
    video.src = url;
  });
};

const localVideoRuleMessage = (requirement: CategoryFileRequirementVO, metadata: LocalVideoMetadata, file: File) => {
  const rule = parseRuleJson(requirement.ruleJson);
  const minWidth = positiveRuleNumber(rule.minWidth);
  const minHeight = positiveRuleNumber(rule.minHeight);
  const minDuration = positiveRuleNumber(rule.minDurationSeconds);
  const maxDuration = positiveRuleNumber(rule.maxDurationSeconds);
  const minBitrateMbps = positiveRuleNumber(rule.minBitrateMbps);
  const hasDimensionRule = Boolean(minWidth || minHeight);
  const hasDurationRule = Boolean(minDuration || maxDuration);
  const ext = fileNameExt(file);
  if (!metadata.readable) {
    if (hasLocalVideoTechnicalRule(rule) && browserStrictVideoExts.includes(ext)) {
      return '上传失败：视频参数无法在本地识别，请确认文件可正常播放后重新上传。';
    }
    return '';
  }
  if (hasDimensionRule && (!metadata.width || !metadata.height)) {
    return '上传失败：视频分辨率无法识别，请确认文件可正常播放后重新上传。';
  }
  if (hasDurationRule && !metadata.durationSeconds) {
    return '上传失败：视频时长无法识别，请确认文件可正常播放后重新上传。';
  }
  if ((minWidth && metadata.width && metadata.width < minWidth) || (minHeight && metadata.height && metadata.height < minHeight)) {
    return `上传失败：视频分辨率不能低于 ${minWidth || '-'}×${minHeight || '-'}`;
  }
  if (minDuration && metadata.durationSeconds && metadata.durationSeconds < minDuration) {
    return `上传失败：视频时长不能少于 ${formatDuration(minDuration)}`;
  }
  if (maxDuration && metadata.durationSeconds && metadata.durationSeconds > maxDuration) {
    return `上传失败：视频时长不能超过 ${formatDuration(maxDuration)}`;
  }
  if (minBitrateMbps && !metadata.bitrate) {
    return '上传失败：视频码率无法估算，请确认文件可正常播放后重新上传。';
  }
  if (minBitrateMbps && metadata.bitrate && metadata.bitrate < minBitrateMbps * 1024 * 1024) {
    return `上传失败：视频估算码率不能低于 ${minBitrateMbps}Mbps`;
  }
  return '';
};

const beforeUploadMaterial = async (requirement: CategoryFileRequirementVO, file: File) => {
  const key = String(requirement.id);
  clearUploadError(key);
  if (isRequirementUploading(requirement.id)) {
    setUploadError(key, '当前材料正在上传，请等待完成或先取消当前上传。', file?.name);
    return false;
  }
  const allowedExts = normalizeAllowedExts(requirement.allowedExt);
  const ext = fileNameExt(file);
  if (allowedExts.length && (!ext || !allowedExts.includes(ext))) {
    setUploadError(key, `上传失败：${materialLabel(requirement)}仅支持 ${allowedExts.join('/')} 格式，请重新选择文件。`, file?.name);
    return false;
  }
  const { minMb, maxMb } = fileSizeLimits(requirement);
  const fileSize = file?.size || 0;
  if (minMb && fileSize < minMb * 1024 * 1024) {
    setUploadError(key, `上传失败：文件大小不能小于 ${formatUploadSizeLimit(minMb)}MB，请重新选择文件。`, file?.name);
    return false;
  }
  if (maxMb && fileSize > maxMb * 1024 * 1024) {
    setUploadError(key, `上传失败：文件大小不能超过 ${formatUploadSizeLimit(maxMb)}MB，请重新选择文件。`, file?.name);
    return false;
  }
  if (isVideoRequirement(requirement, file)) {
    const localMetadata = await readLocalVideoMetadata(file);
    const localMessage = localVideoRuleMessage(requirement, localMetadata, file);
    if (localMessage) {
      if (requirementTechnicalTipsOnly(requirement)) {
        proxy?.$modal.msgWarning(technicalReviewMessage(localMessage));
        return true;
      }
      setUploadError(key, localMessage, file?.name);
      proxy?.$modal.msgWarning(localMessage.replace(/^上传失败：/, ''));
      return false;
    }
  }
  return true;
};

const uploadMaterial = async (requirementId: string | number, option: any) => {
  const key = String(requirementId);
  const file = option.file as File;
  const existingFileIds = new Set<string>();
  if (isRequirementUploading(requirementId)) {
    const error = new Error('当前材料正在上传，请等待完成或先取消当前上传。');
    setUploadError(key, error.message, file?.name);
    option.onError?.(error);
    return;
  }
  const controller = new AbortController();
  uploadAbortControllerMap.set(key, controller);
  clearUploadError(key);
  try {
    if (!form.value.id) {
      await saveDraft();
    }
    throwIfUploadCanceled(controller);
    selectedRequirementId.value = requirementId;
    filesByRequirement(requirementId).forEach((item) => existingFileIds.add(String(item.id)));
    setUploadProgress(key, { percent: 0, fileName: file?.name || '文件上传中', loaded: 0, total: file?.size || 0, phaseText: '正在准备直传', cancelable: true });
    const initRes = await initProjectFileDirectUpload(form.value.id!, requirementId, {
      originalName: file.name,
      fileSize: file.size,
      contentType: file.type || 'application/octet-stream'
    });
    throwIfUploadCanceled(controller);
    const direct = initRes.data;
    if (!direct?.uploadUrl || !direct?.uploadToken || !direct?.objectKey) {
      throw new Error('直传地址生成失败，请联系管理员确认 OSS 配置');
    }
    await uploadProjectFileToOss(direct.uploadUrl, file, direct.contentType || file.type || 'application/octet-stream', (event) => {
      const total = event.total || file?.size || 0;
      const loaded = event.loaded || 0;
      const percent = total > 0 ? Math.min(96, Math.round((loaded / total) * 96)) : 0;
      const phaseText = percent >= 96 ? '文件已上传，正在校验并登记，请勿关闭页面' : '正在上传至对象存储';
      setUploadProgress(key, { percent, fileName: file?.name || '文件上传中', loaded, total, phaseText, cancelable: true });
      option.onProgress?.({ percent });
    }, controller.signal);
    throwIfUploadCanceled(controller);
    setUploadProgress(key, {
      percent: 99,
      fileName: file?.name || '文件上传中',
      loaded: file?.size || 0,
      total: file?.size || 0,
      phaseText: '文件已上传，正在校验并登记，请勿关闭页面',
      cancelable: false
    });
    uploadAbortControllerMap.delete(key);
    const res = await completeProjectFileDirectUpload(form.value.id!, requirementId, {
      uploadToken: direct.uploadToken,
      objectKey: direct.objectKey,
      originalName: file.name,
      fileSize: file.size,
      contentType: direct.contentType || file.type || 'application/octet-stream'
    });
    setUploadProgress(key, { percent: 100, fileName: file?.name || '文件上传中', loaded: file?.size || 0, total: file?.size || 0, phaseText: '上传完成', cancelable: false });
    const uploadedFile = res.data;
    selectedFileId.value = uploadedFile?.id;
    await refreshProjectFiles();
    option.onSuccess?.(res.data);
    if (uploadedFile?.checkStatus === 'failed') {
      const message = normalizeReviewMessage(uploadedFile.checkMessage) || '技术校验未通过，请查看提示后替换文件';
      setUploadError(key, `已上传但校验未通过：${message}`, file?.name);
      proxy?.$modal.msgWarning('文件已上传，但技术校验未通过');
    } else {
      if (uploadedFile?.checkStatus === 'warning') {
        const message = normalizeReviewMessage(uploadedFile.checkMessage) || '上传成功，需人工关注';
        setUploadError(key, `已上传，需人工复核：${message}`, file?.name);
        proxy?.$modal.msgWarning(message);
      } else {
        clearUploadError(key);
        proxy?.$modal.msgSuccess('上传成功');
      }
    }
  } catch (error) {
    if (isUploadCanceledError(error)) {
      setUploadProgress(key, {
        percent: uploadProgressMap.value[key]?.percent || 0,
        fileName: file?.name || '文件上传中',
        loaded: uploadProgressMap.value[key]?.loaded || 0,
        total: uploadProgressMap.value[key]?.total || file?.size || 0,
        phaseText: '已取消上传',
        cancelable: false
      });
      clearUploadError(key);
      option.onError?.(error);
      ElMessage.info('已取消上传');
      return;
    }
    if (isUploadTimeoutError(error)) {
      setUploadProgress(key, {
        percent: 99,
        fileName: file?.name || '文件上传中',
        loaded: file?.size || 0,
        total: file?.size || 0,
        phaseText: '请求超时，正在确认上传结果...',
        cancelable: false
      });
      const uploadedFile = await confirmUploadSavedAfterTimeout(requirementId, file, existingFileIds);
      if (uploadedFile) {
        selectedFileId.value = uploadedFile.id;
        setUploadProgress(key, { percent: 100, fileName: file?.name || '文件上传中', loaded: file?.size || 0, total: file?.size || 0, phaseText: '上传完成', cancelable: false });
        clearUploadError(key);
        option.onSuccess?.(uploadedFile);
        proxy?.$modal.msgSuccess('上传成功');
        return;
      }
    }
    setUploadError(key, formatUploadErrorMessage(error), file?.name);
    option.onError?.(error);
    throw error;
  } finally {
    uploadAbortControllerMap.delete(key);
    window.setTimeout(() => clearUploadProgress(key), 800);
  }
};

const downloadMemberTemplate = async () => {
  const blob = await downloadProjectMemberTemplate({ projectId: form.value.id, categoryId: form.value.categoryId });
  if (!blobValidate(blob)) {
    const text = await new Blob([blob as any]).text();
    const error = JSON.parse(text || '{}');
    ElMessage.error(error.msg || '下载采集表失败');
    return;
  }
  FileSaver.saveAs(new Blob([blob as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }), '师生信息采集表.xlsx');
};

const applyRouteCategoryContext = async (context: { categoryId?: string; categoryCode?: string; categoryName?: string; categoryKeyword?: string; categoryGroup?: string }) => {
  contextResolveError.value = '';
  const hasCategoryMatcher = Boolean(context.categoryId || context.categoryCode || context.categoryName || context.categoryKeyword);
  const managedId = resolveManagedActivityId();
  if (!managedId) return;
  const candidates = activityOptions.value.filter((item) => String(item.id) === String(managedId));
  for (const activity of candidates) {
    if (!activity.id) continue;
    form.value.activityId = activity.id;
    await loadCategoryOptions(activity.id);
    filterCategoryOptionsByGroup(context.categoryGroup);
    if (!hasCategoryMatcher) {
      if (context.categoryGroup && categoryOptions.value.length === 0) {
        contextResolveError.value = `未找到「${context.categoryGroup}」下的启用活动类别，请先在后台活动配置中维护类别大类。`;
      }
      return;
    }
    const category = findCategoryByContext(context);
    if (category?.id) {
      form.value.categoryId = category.id;
      await handleCategoryChange();
      return;
    }
  }
  const label = context.categoryCode || context.categoryName || context.categoryKeyword || context.categoryId || context.categoryGroup || '当前菜单';
  contextResolveError.value = `未找到与「${label}」对应的启用活动类别，请先在后台活动配置中维护类别编码和上传规则模板。`;
};

const filterCategoryOptionsByGroup = (categoryGroup?: string) => {
  if (!categoryGroup) return;
  categoryOptions.value = categoryOptions.value.filter((item) => categoryGroupName(item) === categoryGroup);
};

const categoryGroupName = (category: ActivityCategoryVO) => category.categoryGroup || inferCategoryGroup(category.categoryName);

const inferCategoryGroup = (name?: string) => {
  const text = name || '';
  if (text.includes('校长书画')) return '高校校长书画作品';
  if (['声乐', '器乐', '舞蹈', '戏剧', '朗诵', '个人'].some((item) => text.includes(item))) return '艺术表演类';
  if (['美术', '设计', '影视', '校长'].some((item) => text.includes(item))) return '艺术作品类';
  if (text.includes('工作坊')) return '艺术实践工作坊';
  if (['论文', '案例', '成果', '美育'].some((item) => text.includes(item))) return '高校美育改革创新优秀成果';
  return '其他';
};

const findCategoryByContext = (context: { categoryId?: string; categoryCode?: string; categoryName?: string; categoryKeyword?: string }) => {
  if (context.categoryId) {
    const category = categoryOptions.value.find((item) => String(item.id) === String(context.categoryId));
    if (category) return category;
  }
  if (context.categoryCode) {
    const category = categoryOptions.value.find((item) => String(item.categoryCode || '') === String(context.categoryCode));
    if (category) return category;
  }
  if (context.categoryName) {
    const category = categoryOptions.value.find((item) => {
      const name = item.categoryName || '';
      return name.includes(context.categoryName!) || context.categoryName!.includes(name);
    });
    if (category) return category;
  }
  if (context.categoryKeyword) {
    const keywords = context.categoryKeyword.split(/[,，、\s]+/).filter(Boolean);
    const category = categoryOptions.value.find((item) => keywords.some((keyword) => (item.categoryName || '').includes(keyword)));
    if (category) return category;
  }
  return undefined;
};

const importMembers = async (option: any, replace: boolean) => {
  if (!form.value.id) {
    await saveDraft();
  }
  try {
    const res = await importProjectMembers(form.value.id!, option.file, replace);
    applyProject(res.data);
    lastSavedSnapshot.value = snapshotPayload(buildSaveData());
    option.onSuccess?.(res.data);
    proxy?.$modal.msgSuccess(replace ? '采集表已导入' : '参展人员已补充导入');
  } catch (error) {
    option.onError?.(error);
    throw error;
  }
};

const uploadMemberAttachment = async (member: ProjectMemberVO, field: MemberFieldSchema, option: any) => {
  if (!form.value.id || !member.id) return;
  try {
    const res = await uploadProjectMemberAttachment(form.value.id, member.id, field.fieldKey, option.file);
    applyProject(res.data);
    lastSavedSnapshot.value = snapshotPayload(buildSaveData());
    option.onSuccess?.(res.data);
    proxy?.$modal.msgSuccess(`${field.fieldLabel || '成员附件'}已上传`);
  } catch (error) {
    option.onError?.(error);
    throw error;
  }
};

const openMemberAttachmentBatch = async (field: MemberFieldSchema) => {
  if (!editable.value) return;
  await saveDraft();
  if (!form.value.id) return;
  memberBatchAttachmentField.value = field;
  memberAttachmentBatchInput.value?.click();
};

const handleMemberAttachmentBatchFiles = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files || []);
  input.value = '';
  const field = memberBatchAttachmentField.value;
  if (!field || !form.value.id || !files.length) return;
  try {
    const { data } = await batchUploadProjectMemberAttachment(form.value.id, field.fieldKey, files);
    memberBatchAttachmentResult.value = data;
    await refreshCurrentProject();
    if (data.skippedCount) {
      proxy?.$modal.msgWarning(`已匹配 ${data.matchedCount || 0} 个，${data.skippedCount} 个未处理，请查看表格下方结果`);
    } else {
      proxy?.$modal.msgSuccess(`已批量匹配 ${data.matchedCount || 0} 个${field.fieldLabel || '附件'}`);
    }
  } finally {
    memberBatchAttachmentField.value = undefined;
  }
};

const clearMemberBatchAttachmentResult = () => {
  memberBatchAttachmentResult.value = undefined;
};

const removeFile = async (file: ProjectFileVO) => {
  if (!form.value.id || !file.id || !editable.value) return;
  await proxy?.$modal.confirm(`确认删除文件“${file.originalName}”？删除后将进入附件回收站，由管理员按规则恢复或清理。`);
  await deleteProjectFile(form.value.id, file.id);
  fileList.value = fileList.value.filter((item) => item.id !== file.id);
  if (String(materialPreviewFileId.value) === String(file.id)) {
    materialPreviewVisible.value = false;
  }
  if (String(selectedFileId.value) === String(file.id)) {
    selectedFileId.value = undefined;
    selectFirstAvailableMaterial();
  }
};

const submitCurrent = async () => {
  if (submitting.value) return;
  if (hasActiveUploads.value) {
    proxy?.$modal.msgWarning('文件正在上传，请等待完成或取消上传后再提交');
    return;
  }
  if (projectNameRequired.value && !String(form.value.projectName || '').trim()) {
    proxy?.$modal.msgWarning(projectNameMissingMessage.value);
    return;
  }
  submitting.value = true;
  try {
    await saveDraft();
    await refreshCurrentProject();
    if (projectNameRequired.value && !String(form.value.projectName || '').trim()) {
      proxy?.$modal.msgWarning(projectNameMissingMessage.value);
      return;
    }
    const fieldMissingMessage = validateRequiredCategoryFields();
    if (fieldMissingMessage) {
      proxy?.$modal.msgWarning(fieldMissingMessage);
      return;
    }
    const genericTableMissingMessage = validateGenericTableData(genericTableTemplates.value, genericTableData.value);
    if (genericTableMissingMessage) {
      proxy?.$modal.msgWarning(genericTableMissingMessage);
      return;
    }
    const uploadConfirmationMissingMessage = validateRequiredUploadConfirmations();
    if (uploadConfirmationMissingMessage) {
      proxy?.$modal.msgWarning(uploadConfirmationMissingMessage);
      return;
    }
    const memberMissingMessage = validateRequiredMemberFields();
    if (memberMissingMessage) {
      proxy?.$modal.msgWarning(memberMissingMessage);
      return;
    }
    const memberCountMessage = validateMemberCountRules();
    if (memberCountMessage) {
      proxy?.$modal.msgWarning(memberCountMessage);
      return;
    }
    await submitProject(form.value.id!);
    lastSavedSnapshot.value = snapshotPayload(buildSaveData());
    proxy?.$modal.msgSuccess('提交成功');
    skipLeaveConfirm.value = true;
    await router.push(returnToProjectListPath());
  } finally {
    submitting.value = false;
  }
};

const withdrawSubmitCurrent = async () => {
  if (!form.value.id || withdrawing.value) return;
  await proxy?.$modal.confirm(`确认撤回项目「${String(form.value.projectName || form.value.projectNo || '未命名项目').trim()}」的提交？撤回后可继续编辑并重新提交。`);
  withdrawing.value = true;
  try {
    await withdrawSubmitProject(form.value.id);
    proxy?.$modal.msgSuccess('已撤回提交，可继续编辑');
    await refreshCurrentProject();
  } finally {
    withdrawing.value = false;
  }
};

const returnToProjectListPath = () => {
  const returnPath = routeQueryText(route.query.returnPath);
  return returnPath.startsWith('/crehn/project') || returnPath.startsWith('/crehn') ? returnPath : '/index';
};

// ART-REF: FE.PROJECT_EDIT.MEMBER_TABLE -> composables/useProjectMembers.ts
const addMember = (memberType = defaultMemberType.value) => {
  memberList.value.push(normalizeMemberExtra({ memberType, status: 'active', sortOrder: memberList.value.length + 1 }));
};

const ensureMemberDefaultRow = () => {
  if (!memberTableEnabled.value || memberList.value.length) return;
  addMember();
};

const removeMember = (index: number) => {
  memberList.value.splice(index, 1);
  ensureMemberDefaultRow();
};

const normalizeMembers = () => {
  return memberList.value
    .map((item, index) => ({
      ...item,
      extraJson: JSON.stringify((item as any).__extra || {}),
      memberType: item.memberType || defaultMemberType.value,
      status: item.status || 'active',
      sortOrder: item.sortOrder || index + 1
    }))
    .filter((item) => hasMemberValue(item) || hasMemberExtraValue((item as any).__extra));
};

const hasMemberValue = (member: ProjectMemberVO) => {
  return fixedMemberFieldKeys.some((key) => {
    if (key === 'memberType' || key === 'status') return false;
    const value = (member as any)[key];
    return value !== undefined && value !== null && String(value).trim() !== '';
  });
};

const normalizeMemberExtra = (member: ProjectMemberVO) => {
  const row = { ...member } as ProjectMemberVO & { __extra?: Record<string, any> };
  row.__extra = parseJsonObject(member.extraJson);
  return row;
};

const hasMemberExtraValue = (extra?: Record<string, any>) => {
  if (!extra) return false;
  return Object.values(extra).some((value) => {
    if (Array.isArray(value)) return value.length > 0;
    return value !== undefined && value !== null && String(value).trim() !== '';
  });
};

const guideMemberFields = (): MemberFieldSchema[] => [
  { fieldKey: 'name', fieldLabel: '姓名', fieldType: 'input', groups: ['author', 'participant'], sortOrder: 1 },
  { fieldKey: 'identityNo', fieldLabel: '身份证号/护照号', fieldType: 'input', groups: ['participant'], sortOrder: 2 },
  { fieldKey: 'nation', fieldLabel: '民族', fieldType: 'input', groups: ['participant'], sortOrder: 3 },
  { fieldKey: 'gender', fieldLabel: '性别', fieldType: 'input', groups: ['participant'], sortOrder: 4 },
  { fieldKey: 'grade', fieldLabel: '年级', fieldType: 'input', groups: ['participant'], sortOrder: 5 },
  { fieldKey: 'studentNo', fieldLabel: '学号/工作证号', fieldType: 'input', groups: ['participant'], sortOrder: 6 },
  { fieldKey: 'department', fieldLabel: '所在院系', fieldType: 'input', groups: ['participant'], sortOrder: 7 },
  { fieldKey: 'majorCode', fieldLabel: '专业代码', fieldType: 'input', groups: ['participant'], sortOrder: 8 },
  { fieldKey: 'major', fieldLabel: '专业名称', fieldType: 'input', groups: ['participant'], sortOrder: 9 },
  { fieldKey: 'phone', fieldLabel: '联系方式', fieldType: 'input', groups: ['participant'], sortOrder: 10 },
  { fieldKey: 'roleName', fieldLabel: '备注/身份', fieldType: 'input', groups: ['author', 'participant'], sortOrder: 11 }
];

const defaultMemberFields = (): MemberFieldSchema[] => [
  { fieldKey: 'name', fieldLabel: '姓名', fieldType: 'input', groups: ['author', 'participant'], sortOrder: 1 },
  { fieldKey: 'studentNo', fieldLabel: '学号', fieldType: 'input', groups: ['participant'], sortOrder: 2 },
  { fieldKey: 'department', fieldLabel: '院系', fieldType: 'input', groups: ['participant'], sortOrder: 3 },
  { fieldKey: 'major', fieldLabel: '专业', fieldType: 'input', groups: ['participant'], sortOrder: 4 },
  { fieldKey: 'roleName', fieldLabel: '角色', fieldType: 'input', groups: ['author', 'participant'], sortOrder: 5 }
];

const normalizeMemberFieldSchemas = (value: any, group: 'author' | 'participant'): MemberFieldSchema[] => {
  if (!Array.isArray(value)) return [];
  return value
    .map((item: any, index: number) => ({
      fieldKey: item.fieldKey,
      fieldLabel: item.fieldLabel || item.fieldKey,
      fieldType: item.fieldType || 'input',
      required: !!item.required,
      optionsJson: item.optionsJson,
      optionsList: item.optionsList,
      validationJson: item.validationJson,
      sortOrder: item.sortOrder || index + 1,
      groups: [group]
    }))
    .filter((item) => item.fieldKey);
};

const memberUploadField = (field?: MemberFieldSchema) => isMemberAttachmentType(field);

const memberGroupKey = (member?: ProjectMemberVO) => memberTypeGroup(categoryRule.value.memberTypeOptions, member?.memberType);
const memberFieldApplies = (member: ProjectMemberVO, field: MemberFieldSchema) => {
  const groups = field.groups || ['author', 'participant'];
  return groups.includes(memberGroupKey(member));
};
const memberFieldLabel = (field: MemberFieldSchema) => `${field.required ? '* ' : ''}${field.fieldLabel || field.fieldKey}`;
const memberFieldColumnWidth = (field: MemberFieldSchema) => {
  const fieldWidthMap: Record<string, number> = {
    name: 100,
    identityNo: 160,
    nation: 76,
    gender: 72,
    grade: 72,
    studentNo: 132,
    department: 112,
    majorCode: 104,
    major: 112,
    phone: 128,
    roleName: 136
  };
  if (fieldWidthMap[field.fieldKey]) return fieldWidthMap[field.fieldKey];
  const typeWidthMap: Record<string, number> = {
    number: 100,
    date: 128,
    select: 120,
    radio: 140,
    checkbox: 160,
    textarea: 180
  };
  return typeWidthMap[field.fieldType || ''] || 140;
};
const fixedMemberFieldKeys = ['memberType', 'name', 'studentNo', 'department', 'major', 'roleName', 'status'];
const memberFieldValue = (member: ProjectMemberVO, field: MemberFieldSchema) => {
  if (fixedMemberFieldKeys.includes(field.fieldKey)) {
    return (member as any)[field.fieldKey];
  }
  return ((member as any).__extra || {})[field.fieldKey];
};
const setMemberFieldValue = (member: ProjectMemberVO, field: MemberFieldSchema, value: any) => {
  if (fixedMemberFieldKeys.includes(field.fieldKey)) {
    (member as any)[field.fieldKey] = value;
    return;
  }
  if (!(member as any).__extra) {
    (member as any).__extra = {};
  }
  (member as any).__extra[field.fieldKey] = value;
};
const memberFieldOptions = (field: MemberFieldSchema) => {
  if (Array.isArray(field.optionsList)) return field.optionsList;
  return parseOptions(field.optionsJson);
};

const memberAttachmentRule = (field: MemberFieldSchema) => parseJsonObject(field.validationJson);

const memberAttachmentExts = (field: MemberFieldSchema) => {
  const configured = memberAttachmentRule(field).attachmentAllowedExts;
  const values = Array.isArray(configured) ? configured : [];
  const normalized = values
    .map((item: any) => String(item || '').trim().toLowerCase().replace(/^\./, ''))
    .filter(Boolean);
  return normalized.length ? [...new Set(normalized)] : memberAttachmentDefaultExts(field);
};

const memberAttachmentAccept = (field: MemberFieldSchema) => memberAttachmentExts(field).map((item) => `.${item}`).join(',');

const memberAttachmentValue = (member: ProjectMemberVO, field: MemberFieldSchema) => {
  if (field.fieldKey === 'photo') return member.photoOssId || member.photoPath ? { ossId: member.photoOssId, url: member.photoPath } : undefined;
  if (field.fieldKey === 'studentReport') return member.studentReportOssId || member.studentReportPath ? { ossId: member.studentReportOssId, url: member.studentReportPath } : undefined;
  return ((member as any).__extra || {})[field.fieldKey];
};

const memberAttachmentUploaded = (member: ProjectMemberVO, field: MemberFieldSchema) => {
  const value = memberAttachmentValue(member, field);
  if (!value) return false;
  if (typeof value === 'object') return Boolean(value.ossId || value.oss_id || value.url || value.path);
  return String(value).trim() !== '';
};

const validateRequiredCategoryFields = () => {
  const errors: Record<string, string> = {};
  for (const field of fieldList.value) {
    const key = String(field.fieldKey || field.id || '');
    if (!key) continue;
    if (field.fieldType === 'teacher_group') {
      const minItems = teacherGroupMin(field);
      if (minItems > 0) {
        const rows = teacherGroupRows(field).filter((row: Record<string, any>) => Object.values(row || {}).some((value) => String(value || '').trim() !== ''));
        if (rows.length < minItems) {
          errors[key] = `${field.fieldLabel || '可重复人员组'}至少需要填写 ${minItems} 人`;
        }
      }
      continue;
    }
    const keywordMessage = validateKeywordCountField(field);
    if (keywordMessage) {
      errors[key] = keywordMessage;
    }
  }
  categoryFieldErrors.value = errors;
  return Object.values(errors)[0] || '';
};

const validateRequiredUploadConfirmations = () => {
  const errors: Record<string, string> = {};
  for (const requirement of requirementList.value) {
    if (!uploadConfirmationRequired(requirement)) continue;
    const fieldKey = uploadConfirmationFieldKey(requirement);
    if (truthyFormValue(formData.value[fieldKey])) continue;
    const message = `请勾选确认：${uploadConfirmationText(requirement)}`;
    errors[String(requirement.id || fieldKey)] = message;
  }
  uploadConfirmationErrors.value = errors;
  return Object.values(errors)[0] || '';
};

const validateRequiredMemberFields = () => {
  if (!memberTableEnabled.value) return '';
  const fields = [...memberTableFields.value, ...memberAttachmentFields.value];
  for (let index = 0; index < memberList.value.length; index += 1) {
    const member = memberList.value[index];
    for (const field of fields) {
      if (!field.required || !memberFieldApplies(member, field)) continue;
      if (memberUploadField(field)) {
        if (!memberAttachmentUploaded(member, field)) return `第 ${index + 1} 行成员缺少${field.fieldLabel || '附件'}`;
        continue;
      }
      const value = memberFieldValue(member, field);
      if (Array.isArray(value) ? value.length === 0 : value === undefined || value === null || String(value).trim() === '') {
        return `第 ${index + 1} 行成员缺少${field.fieldLabel || field.fieldKey}`;
      }
    }
  }
  return '';
};

const validateMemberCountRules = () => {
  if (!memberTableEnabled.value) return '';
  const rules = categoryRule.value.memberCountRules && typeof categoryRule.value.memberCountRules === 'object' ? categoryRule.value.memberCountRules : {};
  const rows = normalizeMembers();
  const countMap: Record<string, number> = {
    total: rows.length,
    author: rows.filter((item) => item.memberType === 'author').length,
    student: rows.filter((item) => item.memberType === 'student').length,
    teacher: rows.filter((item) => item.memberType === 'teacher').length,
    completer: rows.filter((item) => item.memberType === 'completer').length
  };
  const labels: Record<string, string> = {
    total: '成员',
    author: '作者',
    student: '学生',
    teacher: '指导教师',
    completer: '完成人'
  };
  for (const key of Object.keys(labels)) {
    const rule = rules[key];
    if (!rule || typeof rule !== 'object') continue;
    const min = finiteNumber(rule.min);
    const max = finiteNumber(rule.max);
    const count = countMap[key] || 0;
    if (min !== undefined && count < min) return `${labels[key]}人数应不少于 ${min} 人`;
    if (max !== undefined && count > max) return `${labels[key]}人数应不超过 ${max} 人`;
  }
  return '';
};

const filesByRequirement = (requirementId?: string | number) => {
  return fileList.value.filter((file) => String(file.requirementId) === String(requirementId));
};

const canUploadRequirement = (requirement: CategoryFileRequirementVO) => {
  if (!requirement?.id) return false;
  const maxCount = Number(requirement.maxCount);
  return !Number.isFinite(maxCount) || maxCount <= 0 || filesByRequirement(requirement.id).length < maxCount;
};

const uploadConfirmationRule = (requirement: CategoryFileRequirementVO) => parseRuleJson(requirement.ruleJson);

const requirementTemplateRule = (requirement: CategoryFileRequirementVO) => parseRuleJson(requirement.ruleJson);

const requirementTemplateEnabled = (requirement: CategoryFileRequirementVO) => {
  const rule = requirementTemplateRule(requirement);
  return (!!rule.templateEnabled || !!rule.templateOssId) && !!String(rule.templateOssId || '').trim();
};

const requirementTemplateName = (requirement: CategoryFileRequirementVO) => {
  const rule = requirementTemplateRule(requirement);
  return String(rule.templateName || '下载模板').trim() || '下载模板';
};

const downloadRequirementTemplate = (requirement: CategoryFileRequirementVO) => {
  const categoryId = form.value.categoryId;
  if (!categoryId || !requirement.id) return;
  proxy?.$download.fileRequirementTemplate(categoryId, requirement.id);
};

const uploadConfirmationRequired = (requirement: CategoryFileRequirementVO) => uploadConfirmationRule(requirement).confirmationRequired === true;

const uploadConfirmationFieldKey = (requirement: CategoryFileRequirementVO) => {
  const rule = uploadConfirmationRule(requirement);
  return String(rule.confirmationFieldKey || 'noIdentityInVideo').trim() || 'noIdentityInVideo';
};

const uploadConfirmationText = (requirement: CategoryFileRequirementVO) => {
  const rule = uploadConfirmationRule(requirement);
  return String(rule.confirmationText || '我确认视频中不出现省份、学校、姓名、指导教师等身份信息').trim();
};

const uploadConfirmationTextColor = (requirement: CategoryFileRequirementVO) => {
  const rule = uploadConfirmationRule(requirement);
  return String(rule.confirmationTextColor || '#f56c6c').trim() || '#f56c6c';
};

const uploadConfirmationChecked = (requirement: CategoryFileRequirementVO) => {
  return truthyFormValue(formData.value[uploadConfirmationFieldKey(requirement)]);
};

const uploadConfirmationError = (requirement: CategoryFileRequirementVO) => {
  return uploadConfirmationErrors.value[String(requirement.id || uploadConfirmationFieldKey(requirement))] || '';
};

const setUploadConfirmationChecked = (requirement: CategoryFileRequirementVO, value: any) => {
  const fieldKey = uploadConfirmationFieldKey(requirement);
  formData.value[fieldKey] = value === true;
  if (formData.value[fieldKey]) {
    const key = String(requirement.id || fieldKey);
    const next = { ...uploadConfirmationErrors.value };
    delete next[key];
    uploadConfirmationErrors.value = next;
  }
};

const truthyFormValue = (value: any) => {
  if (value === true) return true;
  if (Array.isArray(value)) return value.length > 0;
  const text = String(value ?? '').trim();
  return text !== '' && text.toLowerCase() !== 'false' && text !== '0';
};

const workRequirementHint = (requirement: CategoryFileRequirementVO) => {
  const tipText = String(requirement.tipText || '').trim();
  return [tipText, requirementSummary(requirement)].filter(Boolean).join('；');
};

const selectRequirement = (requirement: CategoryFileRequirementVO) => {
  selectedRequirementId.value = requirement.id;
  const firstFile = filesByRequirement(requirement.id)[0];
  selectedFileId.value = firstFile?.id;
};

const selectFile = (file: ProjectFileVO) => {
  selectedRequirementId.value = file.requirementId;
  selectedFileId.value = file.id;
};

const {
  downloadPdfPreview,
  generatePreview,
  handleMaterialPreviewClosed,
  materialPreviewFile,
  materialPreviewFileId,
  materialPreviewLoading,
  materialPreviewVisible,
  openMaterialPreview,
  openOriginalFile,
  openPreview,
  printPdfPreview,
  refreshPreviewStatus
} = useProjectFilePreview({
  projectId: () => form.value.id,
  fileList,
  selectedFileId,
  selectFile,
  refreshProjectFiles,
  notifySuccess: (message) => proxy?.$modal.msgSuccess(message),
  notifyWarning: (message) => proxy?.$modal.msgWarning(message)
});

const selectFirstAvailableMaterial = () => {
  if (!requirementList.value.length) {
    selectedRequirementId.value = undefined;
    selectedFileId.value = undefined;
    return;
  }
  const currentRequirement = requirementList.value.find((item) => String(item.id) === String(selectedRequirementId.value));
  selectedRequirementId.value = currentRequirement?.id || requirementList.value[0].id;
  const files = filesByRequirement(selectedRequirementId.value);
  selectedFileId.value = selectedFileId.value && files.some((item) => String(item.id) === String(selectedFileId.value)) ? selectedFileId.value : files[0]?.id;
};

const parseOptions = (text?: string) => {
  if (!text) return [];
  try {
    return JSON.parse(text);
  } catch {
    return [];
  }
};

const parseJsonObject = (text?: string) => {
  if (!text) return {};
  try {
    return JSON.parse(text);
  } catch {
    return {};
  }
};

const normalizeFormLayoutMode = (value: unknown): FormLayoutMode => {
  const mode = String(value || 'single');
  return mode === 'double' || mode === 'custom' ? mode : 'single';
};

const normalizeFormLayoutConfig = (value: unknown): FormLayoutConfig => {
  const layout = value && typeof value === 'object' && !Array.isArray(value) ? (value as Record<string, any>) : {};
  const rawFieldSpans = layout.fieldSpans && typeof layout.fieldSpans === 'object' && !Array.isArray(layout.fieldSpans) ? layout.fieldSpans : {};
  const fieldSpans: Record<string, 12 | 24> = {};
  Object.entries(rawFieldSpans).forEach(([key, spanValue]) => {
    const span = Number(spanValue);
    if (key && (span === 12 || span === 24)) {
      fieldSpans[key] = span;
    }
  });
  return {
    mode: normalizeFormLayoutMode(layout.mode),
    fieldSpans
  };
};

const dynamicFieldLayoutKey = (field: CategoryFieldSchemaVO) => String(field.fieldKey || field.id || '');

const autoDynamicFieldSpan = (field: CategoryFieldSchemaVO): 12 | 24 => {
  const type = String(field.fieldType || 'input');
  if (['textarea', 'teacher_group', 'dimension_group'].includes(type)) return 24;
  if (field.fieldKey === 'topicCategory' || field.fieldLabel === '选题类别') return 24;
  if (type === 'radio' || type === 'checkbox') {
    const validation = parseJsonObject(field.validationJson) as Record<string, any>;
    if (Array.isArray(validation.cascadeOptions) && validation.cascadeOptions.length) return 24;
    const options = parseOptions(field.optionsJson);
    if (Array.isArray(options) && options.length > 4) return 24;
  }
  return 12;
};

const preferredDynamicFieldSpan = (field: CategoryFieldSchemaVO): 12 | 24 => {
  if (formLayoutConfig.value.mode === 'single') return 24;
  if (formLayoutConfig.value.mode === 'custom') {
    const configuredSpan = formLayoutConfig.value.fieldSpans[dynamicFieldLayoutKey(field)];
    if (configuredSpan === 12 || configuredSpan === 24) return configuredSpan;
  }
  return autoDynamicFieldSpan(field);
};

const buildDynamicFieldSpanMap = () => {
  const result: Record<string, 12 | 24> = {};
  let pendingHalfRow = !groupOptions.value.length && projectNameColumnSpan.value === 12;
  for (let index = 0; index < fieldList.value.length; ) {
    const current = fieldList.value[index];
    const currentKey = dynamicFieldLayoutKey(current);
    if (preferredDynamicFieldSpan(current) === 12 && pendingHalfRow) {
      result[currentKey] = 12;
      pendingHalfRow = false;
      index += 1;
      continue;
    }
    if (preferredDynamicFieldSpan(current) === 12 && index + 1 < fieldList.value.length) {
      const next = fieldList.value[index + 1];
      if (preferredDynamicFieldSpan(next) === 12) {
        result[currentKey] = 12;
        result[dynamicFieldLayoutKey(next)] = 12;
        index += 2;
        continue;
      }
    }
    result[currentKey] = 24;
    pendingHalfRow = false;
    index += 1;
  }
  return result;
};

const dynamicFieldSpan = (field: CategoryFieldSchemaVO): 12 | 24 => dynamicFieldSpanMap.value[dynamicFieldLayoutKey(field)] || 24;

const finiteNumber = (value: unknown) => {
  if (value === undefined || value === null || value === '') return undefined;
  const number = Number(value);
  return Number.isFinite(number) ? number : undefined;
};

const topicCategoryField = (field: CategoryFieldSchemaVO) => field.fieldKey === 'topicCategory' || field.fieldLabel === '选题类别';

const originalityHintVisible = (field: CategoryFieldSchemaVO) => ['isOriginal', 'is_original'].includes(field.fieldKey || '') || field.fieldLabel === '是否原创';

const cascadeOptions = (field: CategoryFieldSchemaVO): CascadeOptionSchema[] => {
  const rule = parseRuleJson(field.validationJson);
  const rows = Array.isArray(rule.cascadeOptions) ? rule.cascadeOptions : [];
  return rows
    .map((row: any) => ({
      level1: String(row?.level1 || row?.label || '').trim(),
      level2: String(row?.level2 || '').trim(),
      level3: String(row?.level3 || '').trim()
    }))
    .filter((row) => row.level1);
};

const hasCascadeOptions = (field: CategoryFieldSchemaVO) => fieldRenderType(field) === 'radio' && cascadeOptions(field).length > 0;

const cascadeRadioValue = (field: CategoryFieldSchemaVO) => {
  const key = field.fieldKey;
  if (!key) return [];
  const value = formData.value[key];
  if (Array.isArray(value)) return value.map((item) => String(item || '')).filter(Boolean);
  if (value && typeof value === 'object') {
    return [value.level1, value.level2, value.level3].map((item) => String(item || '')).filter(Boolean);
  }
  const text = String(value || '').trim();
  if (!text) return [];
  const matched = cascadeOptions(field).find((option) => [option.level1, option.level2, option.level3].filter(Boolean).join(' / ') === text || option.level3 === text || option.level2 === text || option.level1 === text);
  return matched ? [matched.level1, matched.level2, matched.level3].filter(Boolean) : [text];
};

const cascadeRadioPartValue = (field: CategoryFieldSchemaVO, level: number) => cascadeRadioValue(field)[level] || '';

const setCascadeRadioPartValue = (field: CategoryFieldSchemaVO, level: number, value: any) => {
  const key = field.fieldKey;
  if (!key) return;
  const current = cascadeRadioValue(field).slice(0, level);
  current[level] = String(value || '');
  formData.value[key] = current.filter(Boolean);
};

const cascadeLevelOptions = (field: CategoryFieldSchemaVO, level: number) => {
  const selected = cascadeRadioValue(field);
  const rows = cascadeOptions(field).filter((row) => {
    if (level >= 1 && row.level1 !== selected[0]) return false;
    if (level >= 2 && row.level2 !== selected[1]) return false;
    return true;
  });
  const key = level === 0 ? 'level1' : level === 1 ? 'level2' : 'level3';
  return [...new Set(rows.map((row) => String((row as any)[key] || '').trim()).filter(Boolean))];
};

const durationMode = (field: CategoryFieldSchemaVO) => {
  const rule = parseRuleJson(field.validationJson);
  const mode = String(rule.durationMode || 'hms');
  return ['hms', 'ms', 'minute', 'second'].includes(mode) ? mode : 'hms';
};

const parseDurationParts = (value: any) => {
  const text = String(value || '').trim();
  if (/^\d{1,3}:\d{2}:\d{2}$/.test(text)) {
    const [hours, minutes, seconds] = text.split(':').map((item) => Number(item || 0));
    return { hours, minutes, seconds };
  }
  if (/^\d{1,4}:\d{2}$/.test(text)) {
    const [minutes, seconds] = text.split(':').map((item) => Number(item || 0));
    return { hours: 0, minutes, seconds };
  }
  const numberValue = Number(text || 0);
  return { hours: 0, minutes: Number.isFinite(numberValue) ? numberValue : 0, seconds: Number.isFinite(numberValue) ? numberValue : 0 };
};

const durationPartValue = (field: CategoryFieldSchemaVO, part: 'hours' | 'minutes' | 'seconds') => {
  const key = field.fieldKey;
  if (!key) return 0;
  return parseDurationParts(formData.value[key])[part] || 0;
};

const padDurationPart = (value: number) => String(Math.max(0, Number(value || 0))).padStart(2, '0');

const setDurationPartValue = (field: CategoryFieldSchemaVO, part: 'hours' | 'minutes' | 'seconds', value: any) => {
  const key = field.fieldKey;
  if (!key) return;
  const parts = parseDurationParts(formData.value[key]);
  parts[part] = Number(value || 0);
  const mode = durationMode(field);
  if (mode === 'ms') {
    formData.value[key] = `${Math.max(0, parts.minutes || 0)}:${padDurationPart(Math.min(59, parts.seconds || 0))}`;
  } else if (mode === 'minute') {
    formData.value[key] = String(Math.max(0, parts.minutes || 0));
  } else if (mode === 'second') {
    formData.value[key] = String(Math.max(0, parts.seconds || 0));
  } else {
    formData.value[key] = `${padDurationPart(parts.hours || 0)}:${padDurationPart(parts.minutes || 0)}:${padDurationPart(parts.seconds || 0)}`;
  }
};

const normalizeDurationValue = (field: CategoryFieldSchemaVO, value: any) => {
  if (!value) return value;
  const text = String(value).trim();
  if (durationMode(field) !== 'hms') return text;
  if (/^\d{1,2}:\d{2}:\d{2}$/.test(text)) {
    return text.padStart(8, '0');
  }
  if (/^\d{1,2}:\d{2}$/.test(text)) {
    return `00:${text.padStart(5, '0')}`;
  }
  return text;
};

const defaultTeacherFields = (): MemberFieldSchema[] => [
  { fieldKey: 'name', fieldLabel: '姓名', fieldType: 'input', sortOrder: 1 },
  { fieldKey: 'phone', fieldLabel: '联系电话', fieldType: 'input', sortOrder: 2 },
  { fieldKey: 'remark', fieldLabel: '备注', fieldType: 'input', sortOrder: 3 }
];

const teacherGroupFields = (field: CategoryFieldSchemaVO): MemberFieldSchema[] => {
  const validation = parseJsonObject(field.validationJson);
  const rows = Array.isArray(validation.teacherFields) ? validation.teacherFields : defaultTeacherFields();
  return rows
    .map((item: any, index: number) => ({
      fieldKey: item.fieldKey,
      fieldLabel: item.fieldLabel || item.fieldKey,
      fieldType: item.fieldType || 'input',
      required: !!item.required,
      optionsJson: item.optionsJson,
      optionsList: item.optionsList,
      sortOrder: item.sortOrder || index + 1
    }))
    .filter((item) => item.fieldKey)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
};

const normalizeTeacherRows = (value: any) => {
  if (Array.isArray(value)) {
    return value.map((item) => ({
      ...item,
      name: item?.name || item?.teacherName || '',
      phone: item?.phone || item?.teacherPhone || '',
      remark: item?.remark || ''
    }));
  }
  if (typeof value === 'string' && value.trim()) {
    return value
      .split(/[、,，;\n]/)
      .map((name) => name.trim())
      .filter(Boolean)
      .map((name) => ({ name, phone: '', remark: '' }));
  }
  return [];
};

const teacherGroupLimit = (field: CategoryFieldSchemaVO) => {
  const validation = parseJsonObject(field.validationJson);
  const maxItems = Number(validation.maxItems || validation.maxTeacherCount || 3);
  return Number.isFinite(maxItems) && maxItems > 0 ? maxItems : 3;
};

const teacherGroupMin = (field: CategoryFieldSchemaVO) => {
  const validation = parseJsonObject(field.validationJson);
  const minItems = Number(validation.minItems || 0);
  return Number.isFinite(minItems) && minItems > 0 ? minItems : 0;
};

const teacherGroupRows = (field: CategoryFieldSchemaVO) => {
  const key = field.fieldKey;
  if (!key) return [];
  if (!Array.isArray(formData.value[key])) {
    formData.value[key] = normalizeTeacherRows(formData.value[key]);
  }
  return formData.value[key];
};

const addTeacherRow = (field: CategoryFieldSchemaVO) => {
  const rows = teacherGroupRows(field);
  if (rows.length >= teacherGroupLimit(field)) return;
  rows.push({ name: '', phone: '', remark: '' });
};

const removeTeacherRow = (field: CategoryFieldSchemaVO, index: number) => {
  teacherGroupRows(field).splice(index, 1);
};

const defaultDimensionItems = (): DimensionItem[] => [
  { key: 'length', label: '长', placeholder: '长', unit: 'cm', required: true },
  { key: 'width', label: '宽', placeholder: '宽', unit: 'cm', required: true },
  { key: 'height', label: '高', placeholder: '高', unit: 'cm', required: true }
];

const dimensionItems = (field: CategoryFieldSchemaVO): DimensionItem[] => {
  const rule = parseRuleJson(field.validationJson);
  const configured = Array.isArray(rule.dimensions) ? rule.dimensions : [];
  const fallback = defaultDimensionItems();
  return fallback.map((item, index) => {
    const row = configured.find((entry: any) => String(entry?.key || '') === item.key) || configured[index] || {};
    return {
      key: item.key,
      label: String(row.label || item.label),
      placeholder: String(row.placeholder || row.label || item.placeholder || item.label),
      unit: String(row.unit || item.unit || ''),
      required: row.required === undefined ? item.required : !!row.required
    };
  });
};

const ensureDimensionValue = (field: CategoryFieldSchemaVO) => {
  const key = field.fieldKey;
  if (!key) return {};
  const value = formData.value[key];
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    formData.value[key] = {};
  }
  return formData.value[key] as Record<string, any>;
};

const dimensionPartValue = (field: CategoryFieldSchemaVO, key: string) => {
  const fieldKey = field.fieldKey;
  if (!fieldKey) return '';
  const value = formData.value[fieldKey];
  return value && typeof value === 'object' && !Array.isArray(value) ? value[key] || '' : '';
};

const setDimensionPartValue = (field: CategoryFieldSchemaVO, key: string, value: any) => {
  ensureDimensionValue(field)[key] = value;
};

const normalizeFormDataForFields = () => {
  fieldList.value.forEach((field) => {
    if (field.fieldType === 'checkbox' && field.fieldKey && !Array.isArray(formData.value[field.fieldKey])) {
      formData.value[field.fieldKey] = [];
    }
    if (field.fieldType === 'duration' && field.fieldKey) {
      formData.value[field.fieldKey] = normalizeDurationValue(field, formData.value[field.fieldKey]);
    }
    if (field.fieldType === 'dimension_group' && field.fieldKey) {
      ensureDimensionValue(field);
    }
    if (field.fieldType === 'teacher_group' && field.fieldKey) {
      if (!formData.value[field.fieldKey] && formData.value.adviserNames) {
        formData.value[field.fieldKey] = formData.value.adviserNames;
      }
      formData.value[field.fieldKey] = normalizeTeacherRows(formData.value[field.fieldKey]).slice(0, teacherGroupLimit(field));
    }
  });
};

const parseRuleJson = (text?: string): RuleJson => {
  if (!text) return {};
  try {
    return JSON.parse(text);
  } catch {
    return {};
  }
};

const quotaTargetFieldKey = (ruleJson?: string) => {
  const rule = parseRuleJson(ruleJson);
  return String(rule.targetFieldKey || rule.groupFieldKey || '').trim();
};

const usesTopLevelGroup = (rule: ActivityRuleGroupOptionVO) => {
  const targetFieldKey = quotaTargetFieldKey(rule.ruleJson);
  return !targetFieldKey || targetFieldKey === 'groupCode' || targetFieldKey === '__group_code';
};

const fieldRenderType = (field: CategoryFieldSchemaVO) => {
  if (field.fieldKey === 'displayGroup' && field.fieldType === 'select') {
    return 'radio';
  }
  return field.fieldType;
};

const fieldRuleOption = (field: CategoryFieldSchemaVO, key: string) => {
  const rule = parseRuleJson(field.validationJson);
  return String(rule[key] || '').trim();
};

const fieldRuleNumber = (field: CategoryFieldSchemaVO, key: string) => {
  const rule = parseRuleJson(field.validationJson);
  const value = rule[key];
  if (value === undefined || value === null || value === '') return undefined;
  const number = Number(value);
  return Number.isFinite(number) ? number : undefined;
};

const fieldValidationType = (field: CategoryFieldSchemaVO) => fieldRuleOption(field, 'type');

const fieldValidationError = (field: CategoryFieldSchemaVO) => {
  const key = String(field.fieldKey || field.id || '');
  return key ? categoryFieldErrors.value[key] || '' : '';
};

const validateKeywordCountField = (field: CategoryFieldSchemaVO) => {
  if (fieldValidationType(field) !== 'keyword_count') return '';
  const key = field.fieldKey;
  if (!key) return '';
  const rawValue = formData.value[key];
  const text = String(rawValue ?? '').trim();
  if (!text && !field.required) return '';
  const minCount = Math.max(0, Math.floor(fieldRuleNumber(field, 'minKeywordCount') ?? 3));
  const maxCount = Math.max(0, Math.floor(fieldRuleNumber(field, 'maxKeywordCount') ?? 5));
  const count = keywordCount(text);
  if (minCount > 0 && count < minCount) {
    return `${field.fieldLabel || '关键词'}应为 ${minCount} 至 ${maxCount || minCount} 个`;
  }
  if (maxCount > 0 && count > maxCount) {
    return `${field.fieldLabel || '关键词'}应为 ${minCount || 0} 至 ${maxCount} 个`;
  }
  return '';
};

const keywordCount = (text: string) => {
  if (!text.trim()) return 0;
  return text.split(/[,，;；、\s]+/).map((item) => item.trim()).filter(Boolean).length;
};

const fieldPlaceholder = (field: CategoryFieldSchemaVO) => fieldRuleOption(field, 'placeholder');

const fieldSuffixText = (field: CategoryFieldSchemaVO) => fieldRuleOption(field, 'suffixText');

const fieldHelpText = (field: CategoryFieldSchemaVO) => fieldRuleOption(field, 'helpText');

const fieldTextMaxLength = (field: CategoryFieldSchemaVO) => {
  if (!['input', 'textarea', 'id_card'].includes(fieldRenderType(field) || '')) return undefined;
  const maxLength = fieldRuleNumber(field, 'maxLength');
  return maxLength && maxLength > 0 ? Math.floor(maxLength) : undefined;
};

const fieldTextMinLength = (field: CategoryFieldSchemaVO) => {
  if (!['input', 'textarea', 'id_card'].includes(fieldRenderType(field) || '')) return undefined;
  const minLength = fieldRuleNumber(field, 'minLength');
  return minLength && minLength > 0 ? Math.floor(minLength) : undefined;
};

const fieldShowWordLimit = (field: CategoryFieldSchemaVO) => {
  if (!fieldTextMaxLength(field)) return false;
  const rule = parseRuleJson(field.validationJson);
  return rule.showWordLimit !== false;
};

const fieldNumberMin = (field: CategoryFieldSchemaVO) => (fieldRenderType(field) === 'number' ? fieldRuleNumber(field, 'min') : undefined);

const fieldNumberMax = (field: CategoryFieldSchemaVO) => (fieldRenderType(field) === 'number' ? fieldRuleNumber(field, 'max') : undefined);

const fieldNumberPrecision = (field: CategoryFieldSchemaVO) => {
  if (fieldRenderType(field) !== 'number') return undefined;
  const rule = parseRuleJson(field.validationJson);
  if (rule.integerOnly === true) return 0;
  const precision = fieldRuleNumber(field, 'precision');
  return precision !== undefined && precision >= 0 ? Math.floor(precision) : undefined;
};

const fieldNumberStep = (field: CategoryFieldSchemaVO) => {
  if (fieldRenderType(field) !== 'number') return undefined;
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

const normalizeInputWidthMode = (value: unknown) => {
  const mode = String(value || 'default');
  return ['default', 'short', 'medium', 'long', 'full', 'custom'].includes(mode) ? mode : 'default';
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
  const renderType = fieldRenderType(field);
  if (renderType === 'number') return '180px';
  if (renderType === 'date' || renderType === 'duration') return '240px';
  return '';
};

const fieldControlStyle = (field: CategoryFieldSchemaVO) => {
  const rule = parseRuleJson(field.validationJson);
  const mode = String(rule.inputWidthMode || 'default');
  const width = mode === 'custom' ? normalizeCustomInputWidth(rule.inputWidthCustom) : inputWidthByMode(mode) || defaultInputWidth(field);
  return width && width !== '100%' ? ({ '--art-field-control-width': width } as Record<string, string>) : {};
};

const fieldDisplayStyle = (field: CategoryFieldSchemaVO) => {
  const rule = parseRuleJson(field.validationJson);
  const style: Record<string, string> = {};
  const placeholderColor = String(rule.placeholderColor || '').trim();
  const placeholderFontSize = Number(rule.placeholderFontSize || 0);
  if (placeholderColor) {
    style['--art-placeholder-color'] = placeholderColor;
  }
  if (placeholderFontSize > 0) {
    style['--art-placeholder-font-size'] = `${placeholderFontSize}px`;
  }
  return style;
};

const fieldSuffixStyle = (field: CategoryFieldSchemaVO) => {
  const rule = parseRuleJson(field.validationJson);
  const style: Record<string, string> = {};
  const color = String(rule.suffixColor || '').trim();
  const fontSize = Number(rule.suffixFontSize || 0);
  if (color) {
    style.color = color;
  }
  if (fontSize > 0) {
    style.fontSize = `${fontSize}px`;
  }
  return style;
};

const fieldHelpStyle = (field: CategoryFieldSchemaVO) => {
  const rule = parseRuleJson(field.validationJson);
  const style: Record<string, string> = {};
  const color = String(rule.helpColor || '').trim();
  const fontSize = Number(rule.helpFontSize || 0);
  if (color) {
    style.color = color;
  }
  if (fontSize > 0) {
    style.fontSize = `${fontSize}px`;
  }
  return style;
};

const stageLabel = (stage?: string) => {
  const labels: Record<string, string> = {
    initial: '初次提交',
    post_selection: '选送后补传',
    per_member: '人员材料'
  };
  return stage ? labels[stage] || stage : '';
};

const ruleDetails = (requirement: CategoryFileRequirementVO) => {
  const rule = parseRuleJson(requirement.ruleJson);
  const details = [
    `格式：${formatAllowedExt(requirement.allowedExt)}`,
    requirementSummary(requirement).split('，').find((item) => item.startsWith('数量')) || '数量不限制'
  ];
  const stage = stageLabel(rule.stage);
  if (stage) details.unshift(stage);
  const { minMb, maxMb } = fileSizeLimits(requirement);
  if (minMb || maxMb) details.push(`大小：${minMb ? `≥${minMb}MB` : ''}${minMb && maxMb ? '，' : ''}${maxMb ? `≤${maxMb}MB` : ''}`);
  if (rule.mediaType) details.push(`类型：${rule.mediaType}`);
  if (rule.minWidth || rule.minHeight) details.push(`分辨率：≥${rule.minWidth || '-'}×${rule.minHeight || '-'}`);
  if (rule.fps) details.push(`帧率：${rule.fps}fps${rule.fpsTolerance ? `±${rule.fpsTolerance}` : ''}`);
  if (rule.minBitrateMbps) details.push(`码率：≥${rule.minBitrateMbps}Mbps`);
  if (rule.minDurationSeconds || rule.maxDurationSeconds) {
    details.push(`时长：${rule.minDurationSeconds ? `≥${formatDuration(rule.minDurationSeconds)}` : ''}${rule.maxDurationSeconds ? `≤${formatDuration(rule.maxDurationSeconds)}` : ''}`);
  }
  if (rule.dpi) details.push(`DPI：${rule.dpi}`);
  if (rule.filenamePattern) details.push('文件名：按模板校验');
  if (Array.isArray(rule.filenameRequiredParts) && rule.filenameRequiredParts.length) details.push(`文件名包含：${rule.filenameRequiredParts.join('、')}`);
  return details;
};

const manualTips = (requirement?: CategoryFileRequirementVO) => {
  const rule = parseRuleJson(requirement?.ruleJson);
  const tips = rule.manualCheckTips;
  if (Array.isArray(tips)) return tips.map((item) => normalizeReviewMessage(String(item))).filter(Boolean);
  if (typeof tips === 'string' && tips.trim()) return tips.split(/[;\n；]/).map((item) => normalizeReviewMessage(item.trim())).filter(Boolean);
  return [];
};

const requirementSummary = (requirement: CategoryFileRequirementVO) => {
  const { minMb, maxMb } = fileSizeLimits(requirement);
  const minCount = Number(requirement.minCount);
  const maxCount = Number(requirement.maxCount);
  const countText = !Number.isFinite(minCount) && !Number.isFinite(maxCount) || (minCount <= 0 && maxCount <= 0)
    ? '数量不限制'
    : `数量${Number.isFinite(minCount) && minCount > 0 ? `≥${minCount}` : ''}${Number.isFinite(minCount) && minCount > 0 && Number.isFinite(maxCount) && maxCount > 0 ? '、' : ''}${Number.isFinite(maxCount) && maxCount > 0 ? `≤${maxCount}` : ''}个`;
  const sizeText = minMb || maxMb ? `，单个大小${minMb ? `≥${formatUploadSizeLimit(minMb)}MB` : ''}${minMb && maxMb ? '、' : ''}${maxMb ? `≤${formatUploadSizeLimit(maxMb)}MB` : ''}` : '';
  return `${requirement.required ? '必传' : '选传'}，${formatAllowedExt(requirement.allowedExt)}，${countText}${sizeText}`;
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

const fileExt = (file?: ProjectFileVO) => {
  const ext = file?.fileExt || file?.originalName?.split('.').pop() || '';
  return ext.toLowerCase().replace(/^\./, '');
};

const isVideoFile = (file?: ProjectFileVO) => {
  const ext = fileExt(file);
  return file?.mediaType === 'video' || ['mp4', 'mov', 'mpg', 'mpeg'].includes(ext);
};

const isImageFile = (file?: ProjectFileVO) => {
  const ext = fileExt(file);
  return file?.mediaType === 'image' || ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext);
};

const isOfficeFile = (file?: ProjectFileVO) => {
  return ['doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx'].includes(fileExt(file));
};

const isPendingOfficePreview = (file?: ProjectFileVO) => {
  return !!file && isOfficeFile(file) && ['queued', 'converting', 'processing'].includes(file.previewStatus || '');
};

const canRequestPreview = (file?: ProjectFileVO) => {
  if (!file || !form.value.id || !file.id || !isOfficeFile(file)) return false;
  if (previewOpenUrl(file)) return false;
  return !['queued', 'converting', 'processing'].includes(file.previewStatus || '');
};

const previewActionLabel = (file?: ProjectFileVO) => (file?.previewStatus === 'failed' ? '重试预览' : '生成预览');

const resourceFileIcon = (file?: ProjectFileVO) => {
  if (isVideoFile(file)) return VideoPlay;
  if (isImageFile(file)) return Picture;
  return Document;
};

const resourceFileIconClass = (file?: ProjectFileVO) => {
  if (isVideoFile(file)) return 'is-video';
  if (isImageFile(file)) return 'is-image';
  return 'is-document';
};

const canInlinePreview = (file?: ProjectFileVO) => {
  if (!filePreviewUrl(file)) return false;
  const ext = fileExt(file);
  const previewExt = file?.previewExt?.toLowerCase();
  return ext === 'pdf' || previewExt === 'pdf' || file?.previewStatus === 'converted';
};

const filePreviewUrl = (file?: ProjectFileVO) => {
  if (!file) return '';
  if (file.previewPath) return file.previewPath;
  const ext = fileExt(file);
  if (isVideoFile(file) || isImageFile(file) || ext === 'pdf') return file.storagePath || '';
  return '';
};

const absoluteFileUrl = (url?: string) => {
  if (!url) return '';
  if (/^(https?:)?\/\//i.test(url) || /^(blob|data):/i.test(url)) return url;
  if (url.startsWith('/')) return `${window.location.origin}${url}`;
  return url;
};

const previewOpenUrl = (file?: ProjectFileVO) => {
  return absoluteFileUrl(filePreviewUrl(file));
};

const pdfViewerUrl = (file?: ProjectFileVO) => {
  const url = previewOpenUrl(file);
  if (!url) return '';
  const base = url.split('#')[0];
  return `${base}#toolbar=0&navpanes=0&scrollbar=1`;
};

const normalizePreviewMessage = (message?: string, status?: string) => {
  return normalizeArtPreviewMessage(message, status);
};

const previewFallbackText = (file: ProjectFileVO) => {
  if (file.previewStatus === 'queued' || file.previewStatus === 'converting' || file.previewStatus === 'processing') return '预览生成中，请稍后。';
  if (file.previewStatus === 'failed') return normalizePreviewMessage(file.previewMessage, file.previewStatus) || '预览转换失败，可下载原文件查看。';
  if (isOfficeFile(file)) return '预览生成中，请稍后。也可下载原文件查看。';
  return '该格式暂不支持直接预览，可下载原文件查看。';
};

const checkStatusLabel = (status?: string) => {
  const labels: Record<string, string> = { passed: '通过', warning: '需人工关注', failed: '不通过' };
  return labels[status || ''] || status || '未校验';
};

const formatFileSize = (value?: number) => {
  if (!value) return '';
  const units = ['B', 'KB', 'MB', 'GB'];
  let size = Number(value);
  let index = 0;
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024;
    index += 1;
  }
  return `${size.toFixed(index === 0 ? 0 : 2)}${units[index]}`;
};

const formatDuration = (seconds?: number) => {
  if (!seconds) return '';
  const total = Math.round(Number(seconds));
  const minutes = Math.floor(total / 60);
  const remain = total % 60;
  return `${minutes}:${String(remain).padStart(2, '0')}`;
};

const formatBitrate = (value?: number) => {
  if (!value) return '';
  const bitrate = Number(value);
  if (bitrate >= 1000000) return `${(bitrate / 1000000).toFixed(2)}Mbps`;
  return `${bitrate}bps`;
};

const snapshotPayload = (payload: ProjectSaveForm) => JSON.stringify(payload);
const canEdit = (status?: string) => !status || status === 'draft' || status === 'returned' || status === 'participant_returned';
const canWithdrawSubmit = (status?: string) => status === 'participant_submitted' || status === 'school_submitted';
const statusLabels: Record<string, string> = {
  draft: '草稿',
  participant_submitted: '已提交，待学校审核',
  participant_returned: '学校退回修改',
  school_approved: '学校已推荐',
  school_submitted: '学校已最终提交',
  returned: '省级审核退回',
  audit_passed: '审核通过'
};
const statusTypes: Record<string, ElTagType> = {
  draft: 'info',
  participant_submitted: 'warning',
  participant_returned: 'danger',
  school_approved: 'primary',
  school_submitted: 'warning',
  returned: 'danger',
  audit_passed: 'success'
};
const statusLabel = (status?: string) => statusLabels[status || ''] || status;
const statusType = (status?: string): ElTagType => statusTypes[status || ''] || 'info';

onMounted(() => {
  initPage();
});
</script>

<style scoped lang="scss">
.project-edit-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 92px);
  min-height: 620px;
  padding: 8px;
  overflow: hidden;
}

.edit-header,
.section-toolbar,
.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.edit-header {
  flex: none;
  min-height: 48px;
  margin-bottom: 8px;
  background: var(--el-bg-color);
  border-bottom: 1px solid #ebeef5;
}

.edit-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.edit-actions,
.member-actions,
.preview-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.project-edit-tabs {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  margin-top: 8px;
  overflow: hidden;
}

.project-edit-tabs :deep(.el-tabs__header) {
  flex: none;
  margin-bottom: 12px;
  background: var(--el-bg-color);
}

.project-edit-tabs :deep(.el-tabs__nav-wrap) {
  padding-left: 22px;
}

.project-edit-tabs :deep(.el-tabs__item) {
  height: 44px;
  padding: 0 24px;
  font-size: 16px;
  font-weight: 600;
}

.project-edit-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.project-edit-tabs :deep(.el-tab-pane) {
  min-height: 100%;
  padding-right: 4px;
}

.attachment-layout {
  margin-bottom: 10px;
}

.edit-section,
.material-panel,
.preview-panel {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 14px;
}

.edit-section {
  margin-bottom: 10px;
}

.form-section {
  width: 100%;
  max-width: none;
  padding: 26px 30px 30px;
  margin-right: 0;
  border-right: 0;
  border-left: 0;
  border-radius: 0;
}

.report-form {
  max-width: 900px;
  margin: 0 auto;
}

.report-form--wide {
  max-width: 1180px;
}

.report-form :deep(.el-form-item) {
  margin-bottom: 26px;
}

.report-form :deep(.el-form-item__label) {
  height: 48px;
  font-size: 16px;
  line-height: 48px;
  color: #303133;
  white-space: nowrap;
}

.report-form :deep(.el-form-item__content) {
  min-height: 48px;
  font-size: 16px;
  line-height: 48px;
}

.report-form :deep(.el-input__wrapper),
.report-form :deep(.el-select__wrapper),
.report-form :deep(.el-input-number) {
  min-height: 48px;
}

.report-form :deep(.el-input__inner),
.report-form :deep(.el-select__placeholder),
.report-form :deep(.el-select__selected-item),
.report-form :deep(.el-textarea__inner) {
  font-size: 16px;
}

.report-form :deep(.el-textarea__inner) {
  min-height: 128px !important;
  padding-top: 12px;
  padding-bottom: 12px;
  line-height: 1.7;
}

.report-form :deep(.el-radio-group),
.report-form :deep(.el-checkbox-group) {
  min-height: 48px;
  align-items: center;
  gap: 14px 28px;
}

.report-form :deep(.el-radio),
.report-form :deep(.el-checkbox) {
  height: 48px;
  margin-right: 10px;
  font-size: 16px;
}

.report-form :deep(.el-radio__label),
.report-form :deep(.el-checkbox__label) {
  padding-left: 10px;
  font-size: 16px;
  color: #303133;
}

.report-form :deep(.el-radio__inner),
.report-form :deep(.el-checkbox__inner) {
  width: 24px;
  height: 24px;
}

.report-form :deep(.el-radio__inner::after) {
  width: 8px;
  height: 8px;
}

.report-form :deep(.el-checkbox__inner::after) {
  left: 8px;
  top: 4px;
  width: 6px;
  height: 12px;
}

.section-title,
.preview-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.section-subtitle {
  font-size: 12px;
  color: #909399;
}

.work-upload-panel {
  margin-top: 12px;
}

.work-upload-tip {
  margin-bottom: 10px;
}

.work-requirement-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 0;
  row-gap: 0;
  padding: 0 26px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.work-requirement-item {
  min-width: 0;
  padding: 24px 18px;
  border-bottom: 1px solid #ebeef5;
}

.work-requirement-item:nth-child(odd) {
  padding-right: 26px;
}

.work-requirement-item:nth-child(even) {
  padding-left: 26px;
}

.work-requirement-item:nth-child(odd):not(:last-child) {
  border-right: 1px solid #ebeef5;
}

.work-requirement-item:last-child {
  border-bottom: 0;
}

.work-requirement-title {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  min-height: 24px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.work-required-mark {
  color: #f56c6c;
  font-weight: 700;
}

.work-help-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  margin-left: 2px;
  font-size: 10px;
  font-weight: 600;
  color: #909399;
  border: 1px solid #c0c4cc;
  border-radius: 50%;
  cursor: help;
}

.work-upload-action {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 0;
  min-height: 34px;
  margin-top: 10px;
}

.work-inline-upload {
  display: inline-flex;
}

.work-upload-error {
  margin-top: 8px;
  color: var(--el-color-danger);
  font-size: 12px;
  line-height: 18px;
  word-break: break-all;
}

.work-upload-button {
  min-width: 118px;
  height: 34px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 500;
}

.work-requirement-description {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.7;
  color: #909399;
}

.work-upload-confirmation {
  margin-top: 10px;
  padding: 8px 10px;
  border: 1px solid transparent;
  border-radius: 4px;
}

.work-upload-confirmation :deep(.el-checkbox__label) {
  color: var(--upload-confirmation-text-color, #f56c6c);
}

.work-upload-confirmation.is-error {
  background: #fef0f0;
  border-color: var(--el-color-danger);
}

.work-upload-confirmation-error {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-color-danger);
}

.work-file-section {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.work-file-section-title {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #606266;
}

.work-file-list {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 2px;
}

.member-section {
  padding: 16px;
  border-color: #dce8f7;
  box-shadow: 0 2px 10px rgb(31 45 61 / 6%);
}

.member-card-head {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.member-toolbar-row,
.member-leading-actions,
.member-import-actions,
.member-right-actions,
.member-drawer-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.member-toolbar-row {
  justify-content: space-between;
}

.member-leading-actions {
  flex: 1 1 560px;
  min-width: 0;
}

.member-right-actions {
  justify-content: flex-end;
  flex: 1 1 300px;
}

.member-import-actions :deep(.el-upload),
.member-drawer-actions :deep(.el-upload) {
  display: inline-flex;
}

.member-section-title {
  display: inline-flex;
  align-items: center;
  min-height: 40px;
  padding: 0 14px 0 12px;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.2;
  color: #303133;
  border-left: 4px solid var(--el-color-primary);
}

.member-template-button,
.member-tool-button,
.member-close-button {
  height: 40px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 600;
}

.member-template-button {
  min-width: 138px;
  box-shadow: 0 3px 8px rgb(64 158 255 / 22%);
}

.member-tool-button {
  min-width: 116px;
}

.member-close-button {
  min-width: 96px;
}

.member-drawer-title-group .member-close-button {
  width: var(--art-popup-close-size);
  min-width: var(--art-popup-close-size);
  height: var(--art-popup-close-size);
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
}

.member-drawer-title-group .member-close-button :deep(.el-icon) {
  font-size: 20px;
}

.member-drawer-title-group .member-close-button:hover {
  border: 0;
  background: var(--el-fill-color);
  color: var(--el-color-primary);
}

.member-tip-inline {
  display: block;
  width: 100%;
  min-width: 220px;
  padding: 8px 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #606266;
  background: #f8fafc;
  border: 1px solid #edf2f7;
  border-radius: 6px;
}

.member-table {
  width: 100%;
  max-width: 100%;
  overflow: hidden;
  border-radius: 6px;
}

.member-table :deep(th.el-table__cell .cell),
.member-table :deep(td.el-table__cell .cell) {
  overflow: visible;
  line-height: 1.35;
  text-overflow: clip;
  white-space: normal;
  overflow-wrap: anywhere;
}

.member-table :deep(.el-table__body-wrapper) {
  max-width: 100%;
  overflow-x: auto;
}

.member-table :deep(.el-input),
.member-table :deep(.el-select),
.member-table :deep(.el-input-number),
.member-table :deep(.el-date-editor) {
  min-width: 0;
  max-width: 100%;
}

.member-table :deep(.el-radio-group),
.member-table :deep(.el-checkbox-group) {
  display: flex;
  flex-wrap: wrap;
  min-width: 0;
  gap: 4px 8px;
}

.member-table-footer {
  display: flex;
  justify-content: center;
  padding: 12px 0 2px;
}

.member-attachment-file-input {
  display: none;
}

.member-batch-result {
  margin-top: 12px;
}

:global(.member-drawer-modal) {
  background-color: rgb(0 0 0 / 48%);
}

:global(.member-drawer) {
  --art-popup-close-size: 44px;
  border-radius: 8px 0 0 8px;
}

:global(.member-drawer .el-drawer__body) {
  padding: 0;
  background: #fff;
}

.member-drawer-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: #fff;
}

.member-drawer-header {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 72px;
  padding: 18px 20px;
  border-bottom: 1px solid #e4e7ed;
}

.member-drawer-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 220px;
}

.member-drawer-title-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.member-drawer-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.member-drawer-subtitle {
  font-size: 13px;
  line-height: 1.4;
  color: #909399;
}

.member-drawer-actions {
  justify-content: flex-end;
}

.member-drawer-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  padding: 16px 20px 20px;
  overflow: hidden;
}

.member-drawer-tip {
  flex: none;
}

.work-file-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
  min-height: 40px;
  gap: 12px;
  padding: 4px 0;
  border-bottom: 1px dashed #ebeef5;
}

.work-file-chip:last-child {
  border-bottom: 0;
}

.work-file-main,
.work-file-actions {
  display: flex;
  align-items: center;
  min-width: 0;
}

.work-file-main {
  flex: 1;
  gap: 8px;
}

.work-file-actions {
  flex: none;
  gap: 8px;
}

.work-file-icon {
  flex: none;
  color: #909399;
}

.work-file-icon.is-image {
  color: var(--el-color-success);
}

.work-file-icon.is-video {
  color: var(--el-color-warning);
}

.work-file-name {
  min-width: 0;
  flex: 1;
  padding: 0;
  min-height: 32px;
  font-size: 13px;
  line-height: 32px;
  color: var(--el-color-primary);
  text-align: left;
  background: transparent;
  border: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.work-file-name:hover {
  text-decoration: underline;
}

.work-file-action {
  min-width: 52px;
  min-height: 32px;
  padding: 0 8px;
}

.work-upload-progress {
  margin-top: 12px;
}

.work-upload-empty {
  margin-top: 12px;
  padding: 24px 0;
  background: #f9fafc;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
}

.teacher-group-editor {
  display: flex;
  flex-direction: column;
  width: 100%;
  gap: 8px;
}

.teacher-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)) 52px;
  gap: 8px;
  align-items: center;
}

.teacher-sub-field {
  width: 100%;
}

.teacher-add-button {
  width: 64px;
  padding-left: 0;
  padding-right: 0;
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
  gap: 0 22px;
  min-width: 0;
}

.cascade-radio-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.cascade-radio-field :deep(.el-radio-group) {
  min-height: 32px;
}

.dynamic-field-block {
  width: 100%;
}

.dynamic-field-block :deep(.el-input__inner::placeholder),
.dynamic-field-block :deep(.el-textarea__inner::placeholder) {
  color: var(--art-placeholder-color, var(--el-text-color-placeholder));
  font-size: var(--art-placeholder-font-size, inherit);
}

.dynamic-field-block :deep(.el-select__placeholder) {
  color: var(--art-placeholder-color, var(--el-text-color-placeholder));
  font-size: var(--art-placeholder-font-size, inherit);
}

.field-control-with-suffix {
  display: flex;
  align-items: center;
  gap: 8px;
  width: min(100%, var(--art-field-control-width, 100%));
  max-width: 100%;
}

.field-control-with-suffix > :deep(.el-input),
.field-control-with-suffix > :deep(.el-select),
.field-control-with-suffix > :deep(.el-textarea),
.field-control-with-suffix > :deep(.el-date-editor),
.field-control-with-suffix > :deep(.el-input-number) {
  flex: 1 1 auto;
  min-width: 0;
}

.field-control-with-suffix > .dynamic-radio-field,
.field-control-with-suffix > :deep(.el-checkbox-group) {
  flex: 1 1 auto;
  min-width: 0;
}

.field-suffix-text {
  flex: 0 0 auto;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 32px;
  white-space: nowrap;
}

.dimension-group-field {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 48px;
}

.dimension-item-label,
.dimension-unit,
.dimension-separator {
  flex: 0 0 auto;
  color: #303133;
  font-size: 16px;
  line-height: 48px;
}

.dimension-input {
  width: 84px;
}

.duration-part-field {
  display: flex;
  align-items: center;
  flex: 1 1 auto;
  gap: 8px;
  min-width: 0;
}

.duration-part-field :deep(.el-input-number) {
  width: 112px;
}

.duration-part-field span {
  flex: 0 0 auto;
  color: #303133;
  font-size: 15px;
  white-space: nowrap;
}

.field-help-text {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 768px) {
  .field-control-with-suffix {
    width: 100%;
  }
}

.originality-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.project-workspace {
  display: grid;
  grid-template-columns: minmax(260px, 20%) minmax(0, 1fr);
  gap: 10px;
  min-height: 560px;
  height: calc(100vh - 220px);
}

.material-panel,
.preview-panel {
  min-width: 0;
}

.material-scroll {
  height: calc(100% - 42px);
  min-height: 360px;
}

.resource-title {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 8px;
  margin: -4px -4px 8px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  background: #f7f8fa;
  border-bottom: 1px solid #ebeef5;
}

.resource-title .el-icon {
  color: var(--el-color-primary);
}

.resource-group {
  margin-bottom: 12px;
}

.resource-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  min-height: 28px;
  padding: 0 8px;
  cursor: pointer;
}

.resource-group-name {
  min-width: 0;
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-upload {
  flex: none;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.resource-group:hover .resource-upload {
  opacity: 1;
}

.rule-line,
.preview-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #606266;
}

.resource-file-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.resource-upload-progress {
  padding: 4px 8px 6px 18px;
}

.upload-progress-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 12px;
  color: #606266;
}

.upload-progress-title {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.upload-progress-percent,
.upload-cancel-button {
  flex: 0 0 auto;
}

.upload-cancel-button {
  height: 20px;
  padding: 0;
  font-size: 12px;
}

.resource-file-row {
  display: flex;
  align-items: center;
  gap: 7px;
  width: 100%;
  height: 30px;
  padding: 0 8px 0 18px;
  color: #303133;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.resource-file-row:hover {
  background: #f5f7fa;
}

.resource-file-row.active {
  background: #e8f3ff;
}

.resource-file-icon {
  flex: none;
  font-size: 14px;
}

.resource-file-icon.is-image {
  color: #00b894;
}

.resource-file-icon.is-video {
  color: #f59e0b;
}

.resource-file-icon.is-document {
  color: var(--el-color-primary);
}

.resource-file-name {
  min-width: 0;
  flex: 1;
  font-size: 13px;
  color: var(--el-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-status-dot {
  flex: none;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #f56c6c;
}

.resource-status-dot.is-warning {
  background: #e6a23c;
}

.resource-status-dot.is-passed {
  background: #67c23a;
}

.resource-preview-bars {
  display: inline-flex;
  flex: none;
  align-items: flex-end;
  gap: 2px;
  width: 14px;
  height: 14px;
  color: var(--el-color-primary);
}

.resource-preview-bars i {
  display: block;
  width: 3px;
  background: currentColor;
  border-radius: 1px;
}

.resource-preview-bars i:nth-child(1) {
  height: 5px;
}

.resource-preview-bars i:nth-child(2) {
  height: 9px;
}

.resource-preview-bars i:nth-child(3) {
  height: 12px;
}

.resource-preview-bars.is-failed {
  color: #f56c6c;
}

.resource-preview-bars.is-processing,
.resource-preview-bars.is-queued {
  color: #e6a23c;
}

.resource-empty-row {
  height: 28px;
  padding-left: 39px;
  font-size: 12px;
  line-height: 28px;
  color: #a8abb2;
  cursor: pointer;
}

.rule-panel {
  padding: 10px;
  margin-top: 10px;
  background: #f7f8fa;
  border-radius: 6px;
}

.rule-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tip-block {
  margin-bottom: 8px;
}

.tip-block.manual {
  margin-top: 8px;
  margin-bottom: 0;
}

.tip-title {
  margin-bottom: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #303133;
}

.tip-content {
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
  white-space: pre-wrap;
}

.preview-body {
  height: 520px;
  margin-top: 10px;
  overflow: hidden;
  background: #111827;
  border-radius: 6px;
}

:global(.material-preview-dialog) {
  max-width: 1440px;
}

:global(.material-preview-dialog .el-dialog__body) {
  padding: 0 20px 20px;
}

.material-preview-body {
  height: min(72vh, 760px);
  margin-top: 0;
}

.media-viewer,
.image-viewer,
.doc-viewer {
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
}

.media-viewer {
  background: #000;
}

.image-viewer {
  object-fit: contain;
  background: #0f172a;
}

.doc-viewer {
  background: #fff;
}

.pdf-viewer-shell {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  background: #fff;
}

.pdf-toolbar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  padding: 6px 10px;
  color: #334155;
  border-bottom: 1px solid #e5e7eb;
  background: #f8fafc;
}

.pdf-viewer-shell .doc-viewer {
  flex: 1;
  min-height: 0;
}

.member-not-applicable {
  color: #a8abb2;
}

.preview-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 100%;
  color: #d1d5db;
}

.preview-state .el-icon {
  font-size: 36px;
}

.tech-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  padding-top: 10px;
  color: #606266;
  font-size: 13px;
}

@media (max-width: 1100px) {
  .work-requirement-list {
    grid-template-columns: minmax(0, 1fr);
  }

  .work-requirement-item:nth-child(odd) {
    padding-right: 18px;
  }

  .work-requirement-item:nth-child(odd):not(:last-child) {
    border-right: 0;
  }

  .work-requirement-item:nth-child(even) {
    padding-left: 18px;
  }

  .form-section {
    padding: 18px 16px;
  }

  .report-form {
    max-width: none;
  }

  .report-form :deep(.el-form-item__label) {
    width: 130px !important;
  }

  .teacher-row {
    grid-template-columns: 1fr;
  }

  .topic-radio-grid {
    grid-template-columns: 1fr;
  }

  .member-leading-actions,
  .member-right-actions {
    flex-basis: 100%;
    justify-content: flex-start;
  }

  .member-template-button,
  .member-tool-button,
  .member-close-button {
    min-width: 112px;
  }

  :global(.member-drawer) {
    width: 100% !important;
    border-radius: 0;
  }

  .member-drawer-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .member-drawer-title-group {
    align-items: flex-start;
  }

  .member-drawer-actions {
    justify-content: flex-start;
    width: 100%;
  }

  .project-workspace {
    grid-template-columns: 1fr;
    height: auto;
  }

  .material-scroll {
    height: auto;
    max-height: 420px;
  }
}

@media (max-width: 680px) {
  .work-requirement-list {
    row-gap: 0;
    padding: 8px;
  }

  .work-requirement-item {
    padding: 14px;
  }

  .work-requirement-item:nth-child(odd),
  .work-requirement-item:nth-child(even) {
    padding-left: 14px;
    padding-right: 14px;
  }

  .work-upload-action {
    justify-content: flex-start;
    width: 100%;
  }

  .work-file-chip {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .work-file-main {
    width: 100%;
  }

  .work-file-actions {
    margin-left: 24px;
  }

  :global(.material-preview-dialog) {
    width: 94% !important;
  }
}
</style>
