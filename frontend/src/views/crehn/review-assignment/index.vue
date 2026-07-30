<template>
  <div class="p-2 review-assignment-page">
    <template v-if="!formVisible">
      <el-card shadow="hover" class="mb-[10px]">
        <el-form :model="queryParams" :inline="true">
          <el-form-item label="活动">
            <el-select
              v-model="queryParams.activityId"
              clearable
              filterable
              placeholder="全部活动"
              style="width: 240px"
              @change="handleQueryActivityChange"
            >
              <el-option v-for="item in activityOptions" :key="item.id" :label="item.activityName" :value="item.id!" />
            </el-select>
          </el-form-item>
          <el-form-item label="类别">
            <el-select
              v-model="queryParams.categoryId"
              clearable
              filterable
              placeholder="全部类别"
              style="width: 190px"
              :disabled="!queryParams.activityId"
              @change="handleQueryCategoryChange"
            >
              <el-option v-for="item in queryCategoryOptions" :key="item.id" :label="item.categoryName" :value="item.id!" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 130px">
              <el-option label="启用" value="active" />
              <el-option label="停用" value="disabled" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
            <el-button v-hasPermi="['crehn:reviewAssignment:add']" type="success" icon="Operation" @click="openForm()">批量维护</el-button>
            <el-button v-hasPermi="['crehn:reviewAssignment:add']" type="primary" plain icon="Grid" @click="openMixedScopeWorkspace">
              混合范围分配
            </el-button>
            <el-button type="primary" plain icon="View" @click="openProjectOverview">评审项目总览</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card v-if="queryParams.categoryId" v-loading="categoryConfigLoading" shadow="hover" class="mb-[10px] review-unit-card">
        <template #header>
          <div class="review-unit-head">
            <div>
              <strong>{{ categoryConfig?.categoryName || '当前类别' }} · 评审单元</strong>
              <span>按所选字段自动拆分项目，再对一个或多个单元分配评委。</span>
            </div>
            <div>
              <el-button v-hasPermi="['crehn:reviewAssignment:edit']" icon="SetUp" @click="openScopeConfig">设置拆分字段</el-button>
              <el-button v-hasPermi="['crehn:reviewAssignment:edit']" type="primary" plain icon="Hide" @click="openCategoryVisibility">
                类别可见内容
              </el-button>
            </div>
          </div>
        </template>
        <el-alert
          v-if="!categoryConfig?.scopeFieldKeys?.length"
          class="mb-2"
          type="info"
          :closable="false"
          show-icon
          title="当前未设置拆分字段，全部审核通过项目作为一个评审单元。"
        />
        <div class="review-unit-toolbar">
          <span>已选择 {{ selectedScopeUnits.length }} 个单元</span>
          <el-button
            v-hasPermi="['crehn:reviewAssignment:add']"
            type="success"
            icon="UserFilled"
            :disabled="!selectedScopeUnits.length"
            @click="openScopeAssignment(selectedScopeUnits)"
          >
            为所选单元分配评委
          </el-button>
        </div>
        <el-table
          ref="scopeUnitTableRef"
          border
          :data="categoryConfig?.scopeUnits || []"
          row-key="unitKey"
          @selection-change="selectedScopeUnits = $event"
        >
          <el-table-column type="selection" width="46" reserve-selection />
          <el-table-column label="评审单元" min-width="260">
            <template #default="scope">
              <strong>{{ scope.row.pathLabel }}</strong>
            </template>
          </el-table-column>
          <el-table-column label="项目数" prop="projectCount" width="90" align="center" />
          <el-table-column label="评委覆盖" width="150" align="center">
            <template #default="scope">
              <span>{{ reviewerCoverageText(scope.row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="scope">
              <el-tag :type="scopeUnitStatusType(scope.row.assignmentStatus)">{{ scopeUnitStatusLabel(scope.row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right" align="center">
            <template #default="scope">
              <el-button v-hasPermi="['crehn:reviewAssignment:add']" link type="primary" icon="UserFilled" @click="openScopeAssignment([scope.row])">
                分配评委
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card shadow="hover">
        <el-alert
          class="mb-2"
          type="info"
          :closable="false"
          show-icon
          title="评分任务支持按活动合并维护人员和类别。移除只停用任务：评分草稿保留但不计入结果，已提交评分继续保留并参与结果聚合。"
        />
        <el-table v-loading="loading" border :data="assignmentList">
          <el-table-column label="活动" prop="activityName" min-width="180" show-overflow-tooltip />
          <el-table-column label="类别" prop="categoryName" min-width="140" show-overflow-tooltip />
          <el-table-column label="评审账号" min-width="150">
            <template #default="scope">{{ reviewerLabel(scope.row) }}</template>
          </el-table-column>
          <el-table-column label="学校范围" min-width="160">
            <template #default="scope">
              <el-tag size="small" :type="schoolScopeType(scope.row.schoolScopeMode)">{{ schoolScopeLabel(scope.row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="评分模式" width="110">
            <template #default="scope">{{ scoreModeLabel(scope.row.scoreMode) }}</template>
          </el-table-column>
          <el-table-column label="互斥模式" width="120">
            <template #default="scope">{{ exclusiveLabel(scope.row.exclusiveMode) }}</template>
          </el-table-column>
          <el-table-column label="评分可见" width="130">
            <template #default="scope">
              <el-tag size="small" :type="scoreVisibilityType(scope.row.scoreVisibilityPolicy)">
                {{ scoreVisibilityLabel(scope.row.scoreVisibilityPolicy) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="任务/历史评分" width="180">
            <template #default="scope">
              <el-tag class="mr-1" size="small">有效项目 {{ scope.row.projectCount || 0 }}</el-tag>
              <el-tag size="small" type="success">已交 {{ scope.row.submittedCount || 0 }}</el-tag>
              <el-tag v-if="projectFilterCount(scope.row)" class="mt-1" size="small" type="primary">
                筛选标签 {{ projectFilterCount(scope.row) }} 项
              </el-tag>
              <el-tag v-if="excludedProjectCount(scope.row)" class="mt-1" size="small" type="warning">
                已排除 {{ excludedProjectCount(scope.row) }} 项
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="可见性" min-width="190">
            <template #default="scope">
              <el-tag v-if="scope.row.hideSchoolInfo" class="mr-1" size="small">隐藏学校</el-tag>
              <el-tag v-if="scope.row.hideMemberInfo" class="mr-1" size="small">隐藏成员</el-tag>
              <el-tag v-if="fieldHiddenCount(scope.row)" size="small">隐藏字段 {{ fieldHiddenCount(scope.row) }}</el-tag>
              <el-tag v-if="hiddenFileRequirementCount(scope.row)" class="ml-1" size="small" type="warning">
                隐藏附件 {{ hiddenFileRequirementCount(scope.row) }}
              </el-tag>
              <span
                v-if="
                  !scope.row.hideSchoolInfo && !scope.row.hideMemberInfo && !fieldHiddenCount(scope.row) && !hiddenFileRequirementCount(scope.row)
                "
                >全部可见</span
              >
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="scope">
              <el-tag :type="scope.row.status === 'active' ? 'success' : 'info'">{{ scope.row.status === 'active' ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right" align="center">
            <template #default="scope">
              <el-button v-hasPermi="['crehn:reviewAssignment:edit']" link type="primary" icon="Edit" @click="openForm(scope.row)">编辑</el-button>
              <el-button v-hasPermi="['crehn:reviewAssignment:remove']" link type="danger" icon="CircleClose" @click="handleDelete(scope.row)"
                >停用</el-button
              >
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
      </el-card>
    </template>

    <section v-else class="assignment-workspace">
      <header class="assignment-workspace__header">
        <div class="assignment-workspace__title">
          <el-button circle text icon="ArrowLeft" aria-label="返回评分分配" @click="closeAssignmentWorkspace" />
          <div>
            <h2>{{ dialogTitle }}</h2>
            <p>选择评审范围并安排评委；范围、人数缺口和类别可见内容保持在同一页面核对。</p>
          </div>
        </div>
        <div class="assignment-workspace__header-actions">
          <el-button @click="closeAssignmentWorkspace">取消</el-button>
          <el-button
            type="primary"
            :loading="workspaceSubmitting || workspacePreviewLoading"
            :icon="form.id || workspacePreviewValid ? 'Check' : 'View'"
            @click="submitForm"
          >
            {{ workspaceSubmitLabel }}
          </el-button>
        </div>
      </header>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="assignment-workspace__form">
        <div class="assignment-workspace__context">
          <el-form-item v-if="!form.id" label="维护方式">
            <el-radio-group v-model="form.batchOperation" :disabled="form.batchTarget === 'project'">
              <el-radio-button label="merge">添加/更新</el-radio-button>
              <el-radio-button label="remove">移除</el-radio-button>
              <el-radio-button v-if="form.batchTarget !== 'project'" label="sync">同步类别</el-radio-button>
            </el-radio-group>
            <div class="batch-operation-tip">{{ batchOperationTip }}</div>
          </el-form-item>
          <el-form-item label="活动" prop="activityId">
            <el-select
              v-model="form.activityId"
              filterable
              placeholder="请选择活动"
              :disabled="form.batchTarget === 'project'"
              @change="handleFormActivityChange"
            >
              <el-option v-for="item in activityOptions" :key="item.id" :label="item.activityName" :value="item.id!" />
            </el-select>
          </el-form-item>
          <div class="assignment-workspace__scope-selector">
            <AssignmentScopeSelector
              :category-ids="form.categoryIds"
              v-model:school-scope-mode="form.schoolScopeMode"
              v-model:school-ids="selectedSchoolIds"
              :category-options="formCategoryOptions"
              :school-options="schoolOptions"
              :category-multiple-limit="form.id ? 1 : 0"
              :disabled="!form.activityId"
              :category-disabled="form.batchTarget === 'project'"
              :show-school-scope="Boolean(form.id) || form.batchOperation !== 'remove'"
              layout="grid"
              @update:category-ids="handleFormCategoryIdsChange"
            />
          </div>
        </div>

        <div class="assignment-workspace__body">
          <main class="assignment-workspace__main">
            <section class="workspace-panel workspace-scope-panel">
              <div class="workspace-panel__header">
                <div>
                  <span class="workspace-step">1</span>
                  <div>
                    <h3>选择评审范围</h3>
                    <p>{{ workspaceScopeDescription }}</p>
                  </div>
                </div>
                <el-radio-group v-if="workspaceScopeUnits.length" v-model="workspaceScopeView" size="small">
                  <el-radio-button label="board">分组看板</el-radio-button>
                  <el-radio-button label="table">明细表格</el-radio-button>
                </el-radio-group>
              </div>

              <el-empty v-if="!effectiveCategoryId" :image-size="72" description="先选择一个类别，再查看可分配的评审单元" />
              <el-skeleton v-else-if="categoryConfigLoading" :rows="6" animated />
              <template v-else-if="workspaceScopeUnits.length">
                <div class="workspace-scope-summary">
                  <span v-if="workspaceUsesWholeCategory">
                    当前按<strong>全部 {{ workspaceScopeUnits.length }} 个单元</strong>分配，点击卡片可改为精确范围
                  </span>
                  <span v-else
                    >已选 <strong>{{ selectedWorkspaceUnitCount }}</strong> / {{ workspaceScopeUnits.length }} 个单元</span
                  >
                  <span
                    >单元作品计数 <strong>{{ selectedWorkspaceProjectCount }}</strong> 件</span
                  >
                  <el-button v-if="categoryConfig?.scopeFieldKeys?.length" link type="primary" @click="toggleAllWorkspaceUnits">
                    {{
                      workspaceUsesWholeCategory
                        ? '精确选择全部'
                        : selectedWorkspaceUnitCount === workspaceScopeUnits.length
                          ? '恢复全部类别'
                          : '选择全部'
                    }}
                  </el-button>
                </div>

                <div v-if="workspaceScopeView === 'board'" class="scope-board">
                  <section v-for="group in workspaceScopeGroups" :key="group.key" class="scope-board__group">
                    <div class="scope-board__group-title">
                      <span>{{ group.label }}</span>
                      <small>{{ group.units.length }} 个单元 · {{ group.projectCount }} 件作品</small>
                    </div>
                    <div class="scope-board__grid">
                      <button
                        v-for="unit in group.units"
                        :key="unit.unitKey"
                        type="button"
                        class="scope-unit-tile"
                        :class="[`is-${unit.assignmentStatus || 'unassigned'}`, { 'is-selected': isWorkspaceUnitSelected(unit) }]"
                        @click="toggleWorkspaceUnit(unit)"
                      >
                        <span class="scope-unit-tile__check">
                          <el-icon><Check /></el-icon>
                        </span>
                        <strong>{{ workspaceUnitShortLabel(unit) }}</strong>
                        <span class="scope-unit-tile__path">{{ unit.pathLabel }}</span>
                        <span class="scope-unit-tile__metrics">
                          <span>{{ unit.projectCount || 0 }} 件作品</span>
                          <span>{{ reviewerCoverageText(unit) }}</span>
                        </span>
                        <el-tag size="small" effect="light" :type="scopeUnitStatusType(unit.assignmentStatus)">
                          {{ scopeUnitStatusLabel(unit) }}
                        </el-tag>
                      </button>
                    </div>
                  </section>
                </div>

                <el-table
                  v-else
                  ref="workspaceScopeTableRef"
                  border
                  row-key="unitKey"
                  :data="workspaceScopeUnits"
                  @selection-change="handleWorkspaceTableSelection"
                >
                  <el-table-column type="selection" width="46" reserve-selection />
                  <el-table-column label="评审单元" min-width="260" prop="pathLabel" />
                  <el-table-column label="作品数" width="90" align="center" prop="projectCount" />
                  <el-table-column label="评委覆盖" width="160" align="center">
                    <template #default="{ row }">{{ reviewerCoverageText(row) }}</template>
                  </el-table-column>
                  <el-table-column label="状态" width="120" align="center">
                    <template #default="{ row }">
                      <el-tag :type="scopeUnitStatusType(row.assignmentStatus)">{{ scopeUnitStatusLabel(row) }}</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </template>
              <el-empty v-else :image-size="72" description="该类别暂无可分配的审核通过项目" />
            </section>

            <section v-if="form.batchTarget !== 'project' && (form.id || form.batchOperation !== 'remove')" class="workspace-panel">
              <el-collapse>
                <el-collapse-item name="advanced-scope">
                  <template #title>
                    <div class="workspace-collapse-title">
                      <span class="workspace-step is-secondary">+</span>
                      <div>
                        <h3>其他范围条件</h3>
                        <p>需要时再按院校类型、动态字段或具体作品进一步限定。</p>
                      </div>
                    </div>
                  </template>
                  <div class="workspace-advanced-scope">
                    <div v-if="schoolTypeFilterOptions.length" class="project-filter-config">
                      <div class="project-filter-label">院校类型</div>
                      <el-checkbox-group v-model="selectedProjectFilter.schoolTypes" @change="syncProjectFilterJson">
                        <el-checkbox v-for="item in schoolTypeFilterOptions" :key="item" :label="item">{{ item }}</el-checkbox>
                      </el-checkbox-group>
                    </div>
                    <template v-for="field in projectFilterFields" :key="field.key">
                      <div class="project-filter-label">{{ field.label }}</div>
                      <el-checkbox-group v-model="selectedProjectFilter.fieldValues[field.key]" @change="syncProjectFilterJson">
                        <el-checkbox v-for="item in field.options" :key="item" :label="item">{{ item }}</el-checkbox>
                      </el-checkbox-group>
                    </template>
                    <el-form-item label="指定作品">
                      <el-select
                        v-model="selectedProjectIds"
                        multiple
                        filterable
                        remote
                        clearable
                        :remote-method="loadProjectOptions"
                        :loading="projectLoading"
                        placeholder="不选择表示使用上方范围内的全部作品"
                        :disabled="!effectiveCategoryId"
                      >
                        <el-option v-for="item in visibleProjectOptions" :key="item.id" :label="projectOptionLabel(item)" :value="item.id!" />
                      </el-select>
                    </el-form-item>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </section>

            <section v-else-if="form.batchTarget === 'project'" class="workspace-panel workspace-locked-projects">
              <span class="workspace-step is-secondary">✓</span>
              <div>
                <h3>项目范围已锁定</h3>
                <p>已从“评审项目总览”选择 {{ selectedProjectIds.length }} 个项目，本次只维护这些项目的评审人员。</p>
              </div>
            </section>
          </main>

          <aside class="assignment-workspace__aside">
            <section class="workspace-panel workspace-reviewer-panel">
              <div class="workspace-panel__header">
                <div>
                  <span class="workspace-step">2</span>
                  <div>
                    <h3>安排评委</h3>
                    <p>{{ selectedWorkspaceUnitCount ? `当前已选 ${selectedWorkspaceUnitCount} 个评审单元` : '先在左侧选择评审范围' }}</p>
                  </div>
                </div>
              </div>
              <el-form-item v-if="form.id" label="评审账号" prop="reviewerUserId">
                <el-select
                  v-model="form.reviewerUserId"
                  filterable
                  remote
                  reserve-keyword
                  :placeholder="form.activityId && effectiveCategoryId ? '输入账号或姓名搜索' : '请先选择活动和类别'"
                  :remote-method="loadReviewerOptions"
                  :loading="reviewerLoading"
                  :disabled="!form.activityId || !effectiveCategoryId"
                >
                  <el-option v-for="item in reviewerOptions" :key="item.userId" :label="reviewerOptionLabel(item)" :value="item.userId" />
                </el-select>
              </el-form-item>
              <el-form-item v-else label="评审账号" prop="reviewerUserIds">
                <el-select
                  v-model="form.reviewerUserIds"
                  multiple
                  filterable
                  remote
                  reserve-keyword
                  collapse-tags
                  collapse-tags-tooltip
                  :placeholder="form.activityId && form.categoryIds?.length ? '可批量选择评审账号' : '请先选择活动和类别'"
                  :remote-method="loadReviewerOptions"
                  :loading="reviewerLoading"
                  :disabled="!form.activityId || !form.categoryIds?.length"
                >
                  <el-option v-for="item in reviewerOptions" :key="item.userId" :label="reviewerOptionLabel(item)" :value="item.userId" />
                </el-select>
              </el-form-item>
              <el-form-item v-if="form.id" label="任务状态">
                <el-radio-group v-model="form.status">
                  <el-radio label="active">启用</el-radio>
                  <el-radio label="disabled">停用</el-radio>
                </el-radio-group>
              </el-form-item>
            </section>

            <section v-if="form.id || form.batchOperation !== 'remove'" class="workspace-panel">
              <el-collapse>
                <el-collapse-item name="score">
                  <template #title>
                    <div class="workspace-collapse-title">
                      <span class="workspace-step">3</span>
                      <div>
                        <h3>评分配置</h3>
                        <p>{{ scoreModeLabel(form.scoreMode) }} · {{ exclusiveLabel(form.exclusiveMode) }}</p>
                      </div>
                    </div>
                  </template>
                  <el-row :gutter="10">
                    <el-col :span="12">
                      <el-form-item label="评分模式">
                        <el-select v-model="form.scoreMode">
                          <el-option label="百分制" value="numeric_100" />
                          <el-option label="等级制" value="grade" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="评分互斥">
                        <el-select v-model="form.exclusiveMode">
                          <el-option label="单评委互斥" value="single" />
                          <el-option label="多评委可评" value="multi" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-form-item v-if="form.scoreMode !== 'grade'" label="预设分数">
                    <el-input v-model="quickScoreText" placeholder="例如：30,60,80,90,100" clearable />
                  </el-form-item>
                  <el-form-item label="全部评分可见">
                    <el-select v-model="form.scoreVisibilityPolicy">
                      <el-option label="评分后可见" value="after_submit" />
                      <el-option label="一直不可见" value="hidden" />
                      <el-option label="一直可见" value="always" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="高级评分规则">
                    <el-input v-model="form.scoreRuleJson" type="textarea" :rows="4" />
                  </el-form-item>
                </el-collapse-item>
              </el-collapse>
            </section>

            <section v-if="form.id || form.batchOperation !== 'remove'" class="workspace-panel workspace-visibility-panel">
              <div class="workspace-panel__header">
                <div>
                  <span class="workspace-step">4</span>
                  <div>
                    <h3>类别可见内容</h3>
                    <p>同一类别的全部细分单元统一使用。</p>
                  </div>
                </div>
              </div>
              <div class="workspace-visibility-summary">
                <el-tag v-if="form.hideSchoolInfo" size="small">隐藏学校</el-tag>
                <el-tag v-if="form.hideMemberInfo" size="small">隐藏成员</el-tag>
                <el-tag v-if="fieldHiddenCount(form)" size="small">隐藏字段 {{ fieldHiddenCount(form) }}</el-tag>
                <el-tag v-if="hiddenFileRequirementCount(form)" size="small" type="warning"> 隐藏附件 {{ hiddenFileRequirementCount(form) }} </el-tag>
                <span
                  v-if="!form.hideSchoolInfo && !form.hideMemberInfo && !fieldHiddenCount(form) && !hiddenFileRequirementCount(form)"
                  class="workspace-muted"
                >
                  当前全部可见
                </span>
              </div>
              <el-button v-if="effectiveCategoryId" type="primary" plain icon="Hide" @click="openCategoryVisibility(effectiveCategoryId)">
                设置类别可见内容
              </el-button>
            </section>

            <section class="workspace-panel workspace-preview-panel" :class="{ 'is-ready': workspacePreviewValid }">
              <div class="workspace-panel__header">
                <div>
                  <span class="workspace-step">5</span>
                  <div>
                    <h3>执行预览</h3>
                    <p>
                      {{ form.id ? '单条任务将直接保存修改。' : workspacePreviewValid ? '预览已生成，可以确认保存。' : '保存前先核对影响数量。' }}
                    </p>
                  </div>
                </div>
              </div>
              <div v-if="workspacePreviewValid" class="workspace-preview-grid">
                <div>
                  <strong>{{ workspacePreview?.createCount || 0 }}</strong
                  ><span>新增</span>
                </div>
                <div>
                  <strong>{{ workspacePreview?.updateCount || 0 }}</strong
                  ><span>更新</span>
                </div>
                <div>
                  <strong>{{ workspacePreview?.unchangedCount || 0 }}</strong
                  ><span>不变</span>
                </div>
                <div>
                  <strong>{{ workspacePreview?.disableCount || 0 }}</strong
                  ><span>停用</span>
                </div>
              </div>
              <el-alert v-else-if="!form.id" type="info" :closable="false" title="点击“预览变更”后显示新增、更新、不变和停用数量。" />
              <el-button type="primary" class="workspace-submit-button" :loading="workspaceSubmitting || workspacePreviewLoading" @click="submitForm">
                {{ workspaceSubmitLabel }}
              </el-button>
            </section>
          </aside>
        </div>
      </el-form>
    </section>

    <ArtPopupDrawer v-model="mixedScopeVisible" title="混合范围分配" size="86%" wide append-to-body destroy-on-close :close-on-click-modal="false">
      <div class="mixed-scope-workspace">
        <el-alert type="info" :closable="false" show-icon title="一次为同一批评委设置多个类别的不同范围；选择“全部组”或具体子组，二者自动互斥。" />
        <el-form label-position="top" class="mixed-scope-context">
          <el-form-item label="活动" required>
            <el-select v-model="mixedScopeForm.activityId" filterable placeholder="请选择活动" @change="handleMixedScopeActivityChange">
              <el-option v-for="item in activityOptions" :key="item.id" :label="item.activityName" :value="item.id!" />
            </el-select>
          </el-form-item>
          <el-form-item label="维护方式" required>
            <el-radio-group v-model="mixedScopeForm.operation">
              <el-radio-button label="merge">添加/更新</el-radio-button>
              <el-radio-button label="remove">移除范围</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="评审账号" required>
            <el-select
              v-model="mixedScopeForm.reviewerUserIds"
              multiple
              filterable
              remote
              reserve-keyword
              collapse-tags
              collapse-tags-tooltip
              :remote-method="loadMixedScopeReviewerOptions"
              :loading="mixedScopeReviewerLoading"
              :disabled="!mixedScopeForm.activityId"
              placeholder="输入账号或姓名，可选择多位评委"
            >
              <el-option v-for="item in mixedScopeReviewerOptions" :key="item.userId" :label="reviewerOptionLabel(item)" :value="item.userId" />
            </el-select>
          </el-form-item>
        </el-form>

        <div v-loading="mixedScopeCategoryLoading" class="mixed-scope-categories">
          <section v-for="category in mixedScopeCategories" :key="String(category.categoryId)" class="mixed-scope-category">
            <header>
              <div>
                <strong>{{ category.categoryName }}</strong>
                <span>{{ mixedScopeCategorySummary(category) }}</span>
              </div>
              <el-button v-if="category.enabled" link type="danger" @click="clearMixedScopeCategory(category)">清除</el-button>
            </header>
            <div class="mixed-scope-options">
              <el-checkbox
                :model-value="category.enabled && category.scopeMode === 'all'"
                border
                @change="selectMixedScopeAll(category, Boolean($event))"
              >
                {{ category.units.length ? '全部组' : '全部项目' }}
              </el-checkbox>
              <el-checkbox
                v-for="unit in category.units"
                :key="unit.unitKey"
                :model-value="category.enabled && category.scopeMode === 'units' && category.unitKeys.includes(String(unit.unitKey || ''))"
                border
                @change="toggleMixedScopeUnit(category, String(unit.unitKey || ''), Boolean($event))"
              >
                {{ unit.pathLabel || '未分组' }}
                <small>{{ Number(unit.projectCount || 0) }} 件</small>
              </el-checkbox>
            </div>
          </section>
          <el-empty
            v-if="mixedScopeForm.activityId && !mixedScopeCategoryLoading && !mixedScopeCategories.length"
            :image-size="70"
            description="当前活动没有可分配类别"
          />
          <el-empty v-else-if="!mixedScopeForm.activityId" :image-size="70" description="请选择活动后配置类别范围" />
        </div>

        <el-collapse v-if="mixedScopeForm.operation !== 'remove'" class="mixed-scope-score-config">
          <el-collapse-item name="score-config" title="评分配置（所选类别统一使用）">
            <el-row :gutter="12">
              <el-col :span="8">
                <el-form-item label="评分模式">
                  <el-select v-model="mixedScopeForm.scoreMode">
                    <el-option label="百分制" value="numeric_100" />
                    <el-option label="等级制" value="grade" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="评分互斥">
                  <el-select v-model="mixedScopeForm.exclusiveMode">
                    <el-option label="单评委互斥" value="single" />
                    <el-option label="多评委可评" value="multi" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="评分可见">
                  <el-select v-model="mixedScopeForm.scoreVisibilityPolicy">
                    <el-option label="评分后可见" value="after_submit" />
                    <el-option label="一直不可见" value="hidden" />
                    <el-option label="一直可见" value="always" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="高级评分规则">
              <el-input v-model="mixedScopeForm.scoreRuleJson" type="textarea" :rows="3" />
            </el-form-item>
          </el-collapse-item>
        </el-collapse>

        <section class="mixed-scope-preview" :class="{ 'is-ready': mixedScopePreviewValid }">
          <div>
            <strong>执行预览</strong>
            <span v-if="mixedScopePreviewValid">
              新增 {{ mixedScopePreview?.createCount || 0 }}，更新 {{ mixedScopePreview?.updateCount || 0 }}，停用
              {{ mixedScopePreview?.disableCount || 0 }}，不变 {{ mixedScopePreview?.unchangedCount || 0 }}；范围外草稿
              {{ mixedScopePreview?.orphanDraftScoreCount || 0 }}，已签字锁定 {{ mixedScopePreview?.lockedScoreCount || 0 }}
            </span>
            <span v-else>先选择评委与类别范围，再预览本次变更。</span>
          </div>
          <el-button :loading="mixedScopeLoading" @click="previewMixedScopePlan">预览变更</el-button>
          <el-button type="primary" :disabled="!mixedScopePreviewValid" :loading="mixedScopeLoading" @click="submitMixedScopePlan">
            确认保存
          </el-button>
        </section>
      </div>
    </ArtPopupDrawer>

    <ArtPopupDialog v-model="scopeConfigVisible" title="设置评审单元拆分字段" width="680px" append-to-body>
      <el-alert
        class="mb-3"
        type="info"
        :closable="false"
        show-icon
        title="字段按从上到下的顺序组成路径；例如“组别 → 形式”。同一项目的多选值会自动进入对应的多个评审单元。"
      />
      <el-form label-width="90px">
        <el-form-item label="拆分字段">
          <el-select
            v-model="scopeFieldKeys"
            multiple
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            placeholder="不选择表示整个类别为一个评审单元"
            style="width: 100%"
          >
            <el-option v-for="item in scopeFieldOptions" :key="item.key" :label="item.label" :value="item.key" />
          </el-select>
        </el-form-item>
      </el-form>
      <div v-if="scopeFieldKeys.length" class="scope-order-list">
        <div v-for="(fieldKey, index) in scopeFieldKeys" :key="fieldKey" class="scope-order-item">
          <span class="scope-order-index">{{ index + 1 }}</span>
          <span>{{ scopeFieldLabel(fieldKey) }}</span>
          <div>
            <el-button link icon="ArrowUp" :disabled="index === 0" @click="moveScopeField(index, -1)">上移</el-button>
            <el-button link icon="ArrowDown" :disabled="index === scopeFieldKeys.length - 1" @click="moveScopeField(index, 1)">下移</el-button>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" :loading="scopeConfigSaving" @click="saveScopeConfig">保存并生成评审单元</el-button>
        <el-button @click="scopeConfigVisible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="visibilityConfigVisible" title="类别统一可见内容" width="860px" append-to-body :dirty="visibilityConfigDirty">
      <el-alert
        class="mb-3"
        type="warning"
        :closable="false"
        show-icon
        title="本设置对当前类别的全部评委统一生效。学校和提交人由系统强制隐藏；敏感字段首次配置默认隐藏，仍可逐项取消。"
      />
      <el-form label-position="top" class="visibility-config-form">
        <el-form-item label="基础信息">
          <div class="visibility-base-options">
            <div class="visibility-fixed-rule">
              <div>
                <strong>学校/提交人</strong>
                <span>评审端始终匿名，不允许在这里放开。</span>
              </div>
              <el-tag size="small" type="info">系统强制隐藏</el-tag>
            </div>
            <el-checkbox v-model="visibilityForm.hideMemberInfo" @change="visibilityConfigDirty = true">隐藏成员表格</el-checkbox>
          </div>
        </el-form-item>
        <el-form-item label="隐藏字段">
          <div class="visibility-field-panel">
            <div class="visibility-field-toolbar">
              <div>
                <strong>已隐藏 {{ visibilityHiddenFieldCount }} / {{ visibilityFieldOptions.length }} 个字段</strong>
                <span>敏感字段 {{ visibilitySensitiveFieldKeys.length }} 个</span>
              </div>
              <div class="visibility-field-actions">
                <el-button
                  size="small"
                  type="primary"
                  plain
                  :disabled="!visibilitySensitiveFieldKeys.length"
                  @click="selectVisibilitySensitiveFields"
                >
                  勾选全部敏感信息（{{ visibilitySensitiveFieldKeys.length }}）
                </el-button>
                <el-button size="small" @click="selectAllVisibilityFields">全选当前字段</el-button>
                <el-button size="small" @click="clearVisibilityFields">清空</el-button>
              </div>
            </div>
            <el-input v-model="visibilityFieldKeyword" clearable prefix-icon="Search" placeholder="搜索字段名称或编码" />
            <el-alert
              v-if="visibilityHideAllDynamicFields"
              class="mt-2"
              type="info"
              :closable="false"
              show-icon
              title="当前为“隐藏全部动态字段（包含未来新增字段）”；取消任一字段后将自动转为逐项配置。"
            />
            <el-scrollbar class="visibility-field-scroll" max-height="320px">
              <div v-if="filteredVisibilityFieldOptions.length" class="visibility-field-list">
                <el-checkbox
                  v-for="item in filteredVisibilityFieldOptions"
                  :key="item.key"
                  class="visibility-field-row"
                  :model-value="isVisibilityFieldHidden(item.key)"
                  @change="setVisibilityFieldHidden(item.key, Boolean($event))"
                >
                  <span class="visibility-field-main">
                    <span>
                      <strong>{{ item.label }}</strong>
                      <small>{{ item.key }}</small>
                    </span>
                    <span class="visibility-field-tags">
                      <el-tag v-if="item.sensitive" size="small" type="danger">敏感</el-tag>
                      <el-tag v-if="item.required" size="small">必填</el-tag>
                      <el-tag v-if="item.legacy" size="small" type="warning">历史配置</el-tag>
                      <el-tag v-else-if="item.fieldType" size="small" type="info">{{ item.fieldType }}</el-tag>
                    </span>
                  </span>
                </el-checkbox>
              </div>
              <el-empty v-else :image-size="56" description="没有匹配的字段" />
            </el-scrollbar>
          </div>
        </el-form-item>
        <el-form-item label="上传附件">
          <div class="visibility-file-list">
            <div v-if="!visibilityFileRequirements.length" class="text-sm text-gray-500">当前类别没有配置上传附件。</div>
            <div v-for="item in visibilityFileRequirements" :key="String(item.id)" class="visibility-file-row">
              <div>
                <strong>{{ fileRequirementLabel(item) }}</strong>
                <small>{{ item.fileTypeCode || '未设置文件类别编码' }}</small>
              </div>
              <el-switch
                :model-value="visibilityVisibleFileRequirementIds.includes(String(item.id))"
                inline-prompt
                active-text="评委可见"
                inactive-text="隐藏"
                style="--el-switch-on-color: var(--el-color-success); --el-switch-off-color: var(--el-color-danger)"
                @change="setVisibilityFileVisible(item.id, Boolean($event))"
              />
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="visibilityConfigSaving" @click="saveCategoryVisibility">保存类别设置</el-button>
        <el-button @click="visibilityConfigVisible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDrawer v-model="projectOverviewVisible" title="全部评审项目" size="80%" wide append-to-body destroy-on-close>
      <el-form :model="projectQuery" :inline="true" class="project-overview-filter">
        <el-form-item label="活动">
          <el-select
            v-model="projectQuery.activityId"
            clearable
            filterable
            placeholder="全部活动"
            style="width: 230px"
            @change="handleProjectActivityChange"
          >
            <el-option v-for="item in activityOptions" :key="item.id" :label="item.activityName" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-select
            v-model="projectQuery.categoryId"
            clearable
            filterable
            placeholder="全部类别"
            style="width: 180px"
            :disabled="!projectQuery.activityId"
          >
            <el-option v-for="item in projectCategoryOptions" :key="item.id" :label="item.categoryName" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="学校">
          <el-select v-model="projectQuery.schoolId" clearable filterable placeholder="全部学校" style="width: 210px">
            <el-option v-for="item in schoolOptions" :key="item.id" :label="schoolOptionLabel(item)" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="分配状态">
          <el-select v-model="projectQuery.assignmentStatus" clearable placeholder="全部" style="width: 140px">
            <el-option label="未分配" value="unassigned" />
            <el-option label="人数不足" value="shortage" />
            <el-option label="已分配" value="assigned" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目名称">
          <el-input v-model="projectQuery.projectName" clearable placeholder="输入项目名称" style="width: 190px" @keyup.enter="handleProjectSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleProjectSearch">搜索</el-button>
          <el-button icon="Refresh" @click="resetProjectQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="project-batch-toolbar">
        <span>已选择 {{ selectedOverviewProjects.length }} 个项目；批量操作要求项目属于同一活动和类别。</span>
        <div>
          <el-button
            v-hasPermi="['crehn:reviewAssignment:add']"
            type="success"
            icon="Plus"
            :loading="projectBatchOpening === 'merge'"
            :disabled="!selectedOverviewProjects.length || Boolean(projectBatchOpening)"
            @click="openProjectBatch('merge')"
          >
            补充评审人员
          </el-button>
          <el-button
            v-hasPermi="['crehn:reviewAssignment:remove']"
            type="danger"
            plain
            icon="Minus"
            :loading="projectBatchOpening === 'remove'"
            :disabled="!selectedOverviewProjects.length || Boolean(projectBatchOpening)"
            @click="openProjectBatch('remove')"
          >
            取消评审人员
          </el-button>
        </div>
      </div>
      <el-table
        ref="projectOverviewTableRef"
        v-loading="projectOverviewLoading"
        border
        :data="projectOverviewList"
        @selection-change="selectedOverviewProjects = $event"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column label="项目" min-width="220">
          <template #default="{ row }">
            <div>{{ row.projectName || '-' }}</div>
            <small>{{ row.projectNo || '-' }}</small>
          </template>
        </el-table-column>
        <el-table-column label="活动/类别" min-width="220">
          <template #default="{ row }">{{ row.activityName || '-' }} / {{ row.categoryName || '-' }}</template>
        </el-table-column>
        <el-table-column label="学校" prop="schoolName" min-width="150" show-overflow-tooltip />
        <el-table-column label="当前评审人员" prop="reviewerNames" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reviewerNames || '未分配' }}</template>
        </el-table-column>
        <el-table-column label="任务统计" min-width="240">
          <template #default="{ row }">
            <el-tag class="mr-1" size="small">分配 {{ row.assignmentCount || 0 }}</el-tag>
            <el-tag class="mr-1" size="small" type="warning">待评 {{ row.pendingScoreCount || 0 }}</el-tag>
            <el-tag class="mr-1" size="small" type="info">草稿 {{ row.draftScoreCount || 0 }}</el-tag>
            <el-tag size="small" type="success">已交 {{ row.submittedScoreCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分配状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="assignmentStatusType(row.assignmentStatus)">{{ assignmentStatusLabel(row) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="projectTotal > 0"
        v-model:page="projectQuery.pageNum"
        v-model:limit="projectQuery.pageSize"
        :total="projectTotal"
        @pagination="getProjectOverview"
      />
    </ArtPopupDrawer>
  </div>
</template>

<script setup name="ArtReviewAssignment" lang="ts">
import { listActivityOptions, listCategoryOptions, listFileRequirement } from '@/api/crehn/activity';
import {
  delReviewAssignment,
  getReviewAssignmentCategoryConfig,
  listApprovedReviewProjectOptions,
  listReviewAssignment,
  listReviewAssignmentFilterFields,
  listReviewAssignmentProjects,
  listReviewAssignmentSchoolOptions,
  listReviewerOptions,
  maintainReviewAssignmentBatch,
  maintainReviewAssignmentScopePlan,
  previewReviewAssignmentBatch,
  previewReviewAssignmentScopePlan,
  saveReviewAssignmentCategoryConfig,
  updateReviewAssignment
} from '@/api/crehn/review';
import {
  ActivityCategoryVO,
  ActivityVO,
  AssignmentBatchPreviewVO,
  AssignmentSchoolOptionVO,
  CategoryFieldSchemaVO,
  CategoryFileRequirementVO,
  ProjectVO,
  ReviewAssignmentCategoryConfigVO,
  ReviewAssignmentProjectFilterVO,
  ReviewAssignmentScopePlanForm,
  ReviewAssignmentScopeUnitVO,
  ReviewAssignmentVO,
  ReviewProjectOverviewVO
} from '@/api/crehn/types';
import { isCategoryGroup } from '@/utils/artCategory';
import AssignmentScopeSelector from '@/views/crehn/components/AssignmentScopeSelector.vue';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const route = useRoute();
const router = useRouter();

const loading = ref(false);
const total = ref(0);
const assignmentList = ref<ReviewAssignmentVO[]>([]);
const activityOptions = ref<ActivityVO[]>([]);
const queryCategoryOptions = ref<ActivityCategoryVO[]>([]);
const formCategoryOptions = ref<ActivityCategoryVO[]>([]);
const reviewerOptions = ref<any[]>([]);
const reviewerLoading = ref(false);
const schoolOptions = ref<AssignmentSchoolOptionVO[]>([]);
const selectedSchoolIds = ref<Array<string | number>>([]);
const projectOptions = ref<ProjectVO[]>([]);
const projectLoading = ref(false);
const selectedProjectIds = ref<Array<string | number>>([]);
const projectFilterFields = ref<Array<{ key: string; label: string; options: string[] }>>([]);
const selectedProjectFilter = reactive<Required<ReviewAssignmentProjectFilterVO>>({ schoolTypes: [], fieldValues: {}, scopeGroups: [] });
const categoryConfigLoading = ref(false);
const categoryConfig = ref<ReviewAssignmentCategoryConfigVO>();
const selectedScopeUnits = ref<ReviewAssignmentScopeUnitVO[]>([]);
const scopeUnitTableRef = ref();
const scopeConfigVisible = ref(false);
const scopeConfigSaving = ref(false);
const scopeFieldSchemas = ref<CategoryFieldSchemaVO[]>([]);
const scopeFieldKeys = ref<string[]>([]);
const visibilityConfigVisible = ref(false);
const visibilityConfigSaving = ref(false);
const visibilityConfigDirty = ref(false);
type VisibilityFieldOption = {
  key: string;
  label: string;
  fieldType?: string;
  required: boolean;
  sensitive: boolean;
  legacy: boolean;
};
const visibilityForm = reactive<ReviewAssignmentCategoryConfigVO>({
  scopeFieldKeys: [],
  visibilityConfigured: true,
  hideSchoolInfo: true,
  hideMemberInfo: false,
  hiddenFieldKeysJson: ''
});
const visibilityHiddenFieldKeys = ref<string[]>([]);
const visibilityFieldKeyword = ref('');
const visibilityVisibleFileRequirementIds = ref<string[]>([]);
const visibilityFileRequirements = ref<CategoryFileRequirementVO[]>([]);
const visibilityFieldSchemas = ref<CategoryFieldSchemaVO[]>([]);
const formVisible = ref(false);
const formRef = ref<ElFormInstance>();
const workspaceScopeView = ref<'board' | 'table'>('board');
const workspaceScopeTableRef = ref();
const workspaceTableSyncing = ref(false);
const workspacePreview = ref<AssignmentBatchPreviewVO>();
const workspacePreviewSignature = ref('');
const workspacePreviewLoading = ref(false);
const workspaceSubmitting = ref(false);
const workspaceReturnToProjectOverview = ref(false);
type MixedScopeCategoryState = {
  categoryId: string | number;
  categoryName: string;
  enabled: boolean;
  scopeMode: 'all' | 'units';
  unitKeys: string[];
  units: ReviewAssignmentScopeUnitVO[];
};
const mixedScopeVisible = ref(false);
const mixedScopeLoading = ref(false);
const mixedScopeCategoryLoading = ref(false);
const mixedScopeReviewerLoading = ref(false);
const mixedScopeReviewerOptions = ref<any[]>([]);
const mixedScopeCategories = ref<MixedScopeCategoryState[]>([]);
const mixedScopePreview = ref<AssignmentBatchPreviewVO>();
const mixedScopePreviewSignature = ref('');
const projectOverviewVisible = ref(false);
const projectOverviewLoading = ref(false);
const projectOverviewList = ref<ReviewProjectOverviewVO[]>([]);
const projectTotal = ref(0);
const projectCategoryOptions = ref<ActivityCategoryVO[]>([]);
const selectedOverviewProjects = ref<ReviewProjectOverviewVO[]>([]);
const projectOverviewTableRef = ref();
const projectBatchOpening = ref<'' | 'merge' | 'remove'>('');
const defaultQuickScores = [30, 60, 80, 90, 100];
const defaultScoreRuleJson = JSON.stringify({
  min: 0,
  max: 100,
  step: 0.01,
  precision: 2,
  minReviewers: 3,
  maxReviewers: 8,
  aggregateMethod: 'average',
  quickScores: defaultQuickScores
});
const quickScoreText = ref(defaultQuickScores.join(','));
const mixedScopeForm = reactive<ReviewAssignmentScopePlanForm>({
  activityId: '',
  reviewerUserIds: [],
  operation: 'merge',
  scopes: [],
  scoreMode: 'numeric_100',
  scoreRuleJson: defaultScoreRuleJson,
  exclusiveMode: 'single',
  scoreVisibilityPolicy: 'after_submit'
});

const queryParams = reactive<any>({ pageNum: 1, pageSize: 10, activityId: undefined, categoryId: undefined, status: 'active' });
const projectQuery = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  activityId: undefined,
  categoryId: undefined,
  schoolId: undefined,
  assignmentStatus: undefined,
  projectName: undefined
});
const form = reactive<ReviewAssignmentVO>({
  categoryIds: [],
  reviewerUserIds: [],
  batchOperation: 'merge',
  batchTarget: 'assignment',
  schoolScopeMode: 'all',
  schoolIdsJson: '',
  projectFilterJson: '',
  scoreMode: 'numeric_100',
  scoreRuleJson: defaultScoreRuleJson,
  exclusiveMode: 'single',
  scoreVisibilityPolicy: 'after_submit',
  hideSchoolInfo: false,
  hideMemberInfo: false,
  projectIdsJson: '',
  excludedProjectIdsJson: '',
  hiddenFieldKeysJson: '',
  status: 'active'
});

const rules: ElFormRules = {
  activityId: [{ required: true, message: '请选择活动', trigger: 'change' }],
  categoryIds: [{ type: 'array', required: true, min: 1, message: '请至少选择一个类别', trigger: 'change' }],
  reviewerUserId: [{ required: true, message: '请选择评审账号', trigger: 'change' }],
  reviewerUserIds: [{ type: 'array', required: true, min: 1, message: '请至少选择一个评审账号', trigger: 'change' }]
};

const effectiveCategoryId = computed(() => (form.categoryIds?.length === 1 ? form.categoryIds[0] : undefined));
const mixedScopePlanScopes = computed(() =>
  mixedScopeCategories.value
    .filter((category) => category.enabled && (category.scopeMode === 'all' || category.unitKeys.length))
    .map((category) => ({
      categoryId: category.categoryId,
      scopeMode: category.scopeMode,
      unitKeys: category.scopeMode === 'all' ? [] : [...category.unitKeys]
    }))
);
const mixedScopeDraftSignature = computed(() =>
  JSON.stringify({
    ...mixedScopeForm,
    reviewerUserIds: [...mixedScopeForm.reviewerUserIds].map(String).sort(),
    scopes: mixedScopePlanScopes.value.map((scope) => ({
      ...scope,
      categoryId: String(scope.categoryId),
      unitKeys: [...scope.unitKeys].sort()
    }))
  })
);
const mixedScopePreviewValid = computed(
  () => Boolean(mixedScopePreview.value) && mixedScopePreviewSignature.value === mixedScopeDraftSignature.value
);
const visibleProjectOptions = computed(() => projectOptions.value.filter((item) => schoolMatches(item.schoolId)));
const schoolTypeFilterOptions = computed(() =>
  Array.from(new Set(schoolOptions.value.map((item) => item.schoolType?.trim()).filter((item): item is string => Boolean(item)))).sort()
);
const scopeFieldOptions = computed(() =>
  scopeFieldSchemas.value
    .filter(
      (field) => field.fieldKey && ['select', 'radio', 'checkbox'].includes(field.fieldType || '') && parseStringArray(field.optionsJson).length
    )
    .map((field) => ({ key: field.fieldKey!, label: `${field.fieldLabel || field.fieldKey}（${field.fieldKey}）` }))
);
const visibilityHideAllDynamicFields = computed(() => visibilityHiddenFieldKeys.value.includes('*'));
const visibilityFieldOptions = computed<VisibilityFieldOption[]>(() => {
  const options: VisibilityFieldOption[] = [];
  const knownKeys = new Set<string>();
  visibilityFieldSchemas.value.forEach((field) => {
    const key = field.fieldKey?.trim();
    if (!key || knownKeys.has(key)) return;
    knownKeys.add(key);
    options.push({
      key,
      label: field.fieldLabel || key,
      fieldType: field.fieldType,
      required: Boolean(field.required),
      sensitive: Boolean(field.sensitive),
      legacy: false
    });
  });
  const commonLabels = new Map(commonHiddenFieldOptions.map((item) => [item.key, item.label]));
  visibilityHiddenFieldKeys.value.forEach((key) => {
    if (!key || key === '*' || knownKeys.has(key)) return;
    knownKeys.add(key);
    options.push({
      key,
      label: commonLabels.get(key) || `自定义字段 ${key}`,
      required: false,
      sensitive: false,
      legacy: true
    });
  });
  return options;
});
const filteredVisibilityFieldOptions = computed(() => {
  const keyword = visibilityFieldKeyword.value.trim().toLowerCase();
  if (!keyword) return visibilityFieldOptions.value;
  return visibilityFieldOptions.value.filter((item) => item.key.toLowerCase().includes(keyword) || item.label.toLowerCase().includes(keyword));
});
const visibilitySensitiveFieldKeys = computed(() =>
  visibilityFieldOptions.value.filter((item) => item.sensitive && !item.legacy).map((item) => item.key)
);
const visibilityHiddenFieldCount = computed(() => {
  if (visibilityHideAllDynamicFields.value) return visibilityFieldOptions.value.length;
  const hiddenKeys = new Set(visibilityHiddenFieldKeys.value);
  return visibilityFieldOptions.value.filter((item) => hiddenKeys.has(item.key)).length;
});
const selectedScopeGroupLabels = computed(() =>
  selectedProjectFilter.scopeGroups.map((group) =>
    Object.entries(group.fieldValues || {})
      .map(([key, values]) => `${scopeFieldLabel(key)}：${values.map(scopeValueLabel).join('、')}`)
      .join(' / ')
  )
);
const workspaceScopeUnits = computed(() => {
  if (!effectiveCategoryId.value || String(categoryConfig.value?.categoryId || '') !== String(effectiveCategoryId.value)) return [];
  return categoryConfig.value?.scopeUnits || [];
});
const workspaceScopeGroups = computed(() => {
  const primaryKey = categoryConfig.value?.scopeFieldKeys?.[0];
  const groups = new Map<string, { key: string; label: string; units: ReviewAssignmentScopeUnitVO[]; projectCount: number }>();
  workspaceScopeUnits.value.forEach((unit) => {
    const rawValue = primaryKey ? unit.fieldValues?.[primaryKey] || '__unfilled__' : '__all__';
    const key = String(rawValue);
    const group = groups.get(key) || {
      key,
      label: primaryKey ? `${scopeFieldLabel(primaryKey)}：${scopeValueLabel(key)}` : '全部项目',
      units: [],
      projectCount: 0
    };
    group.units.push(unit);
    group.projectCount += unit.projectCount || 0;
    groups.set(key, group);
  });
  return Array.from(groups.values());
});
const workspaceUsesWholeCategory = computed(() =>
  Boolean(effectiveCategoryId.value && workspaceScopeUnits.value.length && !selectedProjectFilter.scopeGroups.length)
);
const selectedWorkspaceUnits = computed(() => workspaceScopeUnits.value.filter(isWorkspaceUnitSelected));
const selectedWorkspaceUnitCount = computed(() =>
  workspaceUsesWholeCategory.value ? workspaceScopeUnits.value.length : selectedWorkspaceUnits.value.length
);
const selectedWorkspaceProjectCount = computed(() =>
  (workspaceUsesWholeCategory.value ? workspaceScopeUnits.value : selectedWorkspaceUnits.value).reduce(
    (totalCount, unit) => totalCount + (unit.projectCount || 0),
    0
  )
);
const workspaceScopeDescription = computed(() => {
  const fields = categoryConfig.value?.scopeFieldKeys || [];
  if (!effectiveCategoryId.value) return '按类别中的匹配字段选择需要分配的范围。';
  if (!fields.length) return '当前类别没有拆分字段，全部审核通过项目作为一个评审单元。';
  return `按 ${fields.map(scopeFieldLabel).join(' → ')} 展示；同一卡片内字段同时匹配，多个卡片任一匹配。`;
});
const workspaceDraftSignature = computed(() =>
  JSON.stringify({
    form: {
      ...form,
      scoreRuleJson: form.scoreRuleJson || '',
      projectFilterJson: form.projectFilterJson || '',
      projectIdsJson: form.projectIdsJson || ''
    },
    selectedSchoolIds: selectedSchoolIds.value.map(String).sort(),
    selectedProjectIds: selectedProjectIds.value.map(String).sort(),
    quickScoreText: quickScoreText.value,
    scopeGroups: selectedProjectFilter.scopeGroups
  })
);
const workspacePreviewValid = computed(() => Boolean(workspacePreview.value) && workspacePreviewSignature.value === workspaceDraftSignature.value);
const workspaceSubmitLabel = computed(() => {
  if (form.id) return '保存修改';
  return workspacePreviewValid.value ? '确认保存' : '预览变更';
});
const dialogTitle = computed(() => {
  if (form.id) return '编辑评分任务';
  if (form.batchTarget === 'project') return form.batchOperation === 'remove' ? '批量取消项目评审人员' : '批量补充项目评审人员';
  return '合并批量维护评分任务';
});
const batchOperationTip = computed(
  () =>
    ({
      merge:
        form.batchTarget === 'project'
          ? '为所选项目补充评审人员；已有类别级分配时会自动解除这些项目的排除状态。'
          : '为所选人员添加所选类别；已有相同人员和类别时更新配置，并恢复已停用任务。',
      remove:
        form.batchTarget === 'project'
          ? '仅取消所选人员对这些项目的评分任务。评分草稿保留但不计入结果，已提交评分继续参与结果聚合。'
          : '停用所选人员在所选类别的评分任务。评分草稿保留但不计入结果，已提交评分继续参与结果聚合。',
      sync: '所选人员在该活动中的有效类别将与当前选择完全一致：补充所选类别，停用未选择类别。'
    })[form.batchOperation || 'merge']
);

const loadActivityOptions = async () => {
  const { data } = await listActivityOptions();
  activityOptions.value = data || [];
};

const loadSchoolOptions = async () => {
  const { data } = await listReviewAssignmentSchoolOptions();
  schoolOptions.value = data || [];
};

const resetMixedScopeWorkspace = () => {
  Object.assign(mixedScopeForm, {
    activityId: '',
    reviewerUserIds: [],
    operation: 'merge',
    scopes: [],
    scoreMode: 'numeric_100',
    scoreRuleJson: defaultScoreRuleJson,
    exclusiveMode: 'single',
    scoreVisibilityPolicy: 'after_submit'
  });
  mixedScopeCategories.value = [];
  mixedScopeReviewerOptions.value = [];
  mixedScopePreview.value = undefined;
  mixedScopePreviewSignature.value = '';
};

const loadMixedScopeReviewerOptions = async (keyword?: string) => {
  if (!mixedScopeForm.activityId) {
    mixedScopeReviewerOptions.value = [];
    return;
  }
  mixedScopeReviewerLoading.value = true;
  try {
    const { data } = await listReviewerOptions(keyword || '', mixedScopeForm.activityId);
    mixedScopeReviewerOptions.value = data || [];
  } finally {
    mixedScopeReviewerLoading.value = false;
  }
};

const handleMixedScopeActivityChange = async () => {
  mixedScopeForm.reviewerUserIds = [];
  mixedScopeReviewerOptions.value = [];
  mixedScopeCategories.value = [];
  mixedScopePreview.value = undefined;
  mixedScopePreviewSignature.value = '';
  if (!mixedScopeForm.activityId) return;
  mixedScopeCategoryLoading.value = true;
  try {
    const { data } = await listCategoryOptions(mixedScopeForm.activityId);
    const categories = (data || []).filter((item) => !isCategoryGroup(item) && item.enabled !== false && item.id !== undefined && item.id !== null);
    const configs = await Promise.all(categories.map((item) => getReviewAssignmentCategoryConfig(item.id!)));
    mixedScopeCategories.value = categories.map((item, index) => {
      const config = configs[index]?.data;
      return {
        categoryId: item.id!,
        categoryName: item.categoryName || String(item.id),
        enabled: false,
        scopeMode: 'all',
        unitKeys: [],
        units: config?.scopeFieldKeys?.length ? config.scopeUnits || [] : []
      };
    });
    await loadMixedScopeReviewerOptions('');
  } finally {
    mixedScopeCategoryLoading.value = false;
  }
};

const openMixedScopeWorkspace = async () => {
  resetMixedScopeWorkspace();
  mixedScopeVisible.value = true;
  if (queryParams.activityId) {
    mixedScopeForm.activityId = queryParams.activityId;
    await handleMixedScopeActivityChange();
  }
};

const clearMixedScopeCategory = (category: MixedScopeCategoryState) => {
  category.enabled = false;
  category.scopeMode = 'all';
  category.unitKeys = [];
};

const selectMixedScopeAll = (category: MixedScopeCategoryState, selected: boolean) => {
  if (!selected) {
    clearMixedScopeCategory(category);
    return;
  }
  category.enabled = true;
  category.scopeMode = 'all';
  category.unitKeys = [];
};

const toggleMixedScopeUnit = (category: MixedScopeCategoryState, unitKey: string, selected: boolean) => {
  if (!unitKey) return;
  const next = new Set(category.scopeMode === 'units' ? category.unitKeys : []);
  if (selected) next.add(unitKey);
  else next.delete(unitKey);
  category.unitKeys = Array.from(next);
  category.scopeMode = 'units';
  category.enabled = category.unitKeys.length > 0;
  if (!category.enabled) category.scopeMode = 'all';
};

const mixedScopeCategorySummary = (category: MixedScopeCategoryState) => {
  if (!category.enabled) return '未纳入本次维护';
  if (category.scopeMode === 'all') return category.units.length ? '全部组' : '全部项目';
  return `已选 ${category.unitKeys.length} 个组`;
};

const prepareMixedScopePlan = (): ReviewAssignmentScopePlanForm | undefined => {
  if (!mixedScopeForm.activityId) {
    proxy?.$modal.msgError('请选择活动');
    return;
  }
  if (!mixedScopeForm.reviewerUserIds.length) {
    proxy?.$modal.msgError('请至少选择一位评委');
    return;
  }
  if (!mixedScopePlanScopes.value.length) {
    proxy?.$modal.msgError('请至少选择一个类别范围');
    return;
  }
  if (mixedScopeForm.operation !== 'remove' && (!mixedScopeForm.scoreRuleJson || !isJsonObject(mixedScopeForm.scoreRuleJson))) {
    proxy?.$modal.msgError('评分规则必须是 JSON 对象');
    return;
  }
  return {
    ...mixedScopeForm,
    reviewerUserIds: [...mixedScopeForm.reviewerUserIds],
    scopes: mixedScopePlanScopes.value
  };
};

const previewMixedScopePlan = async () => {
  const payload = prepareMixedScopePlan();
  if (!payload) return;
  mixedScopeLoading.value = true;
  try {
    const { data } = await previewReviewAssignmentScopePlan(payload);
    mixedScopePreview.value = data;
    mixedScopePreviewSignature.value = mixedScopeDraftSignature.value;
  } finally {
    mixedScopeLoading.value = false;
  }
};

const submitMixedScopePlan = async () => {
  const payload = prepareMixedScopePlan();
  if (!payload || !mixedScopePreviewValid.value) return;
  await proxy?.$modal.confirm(batchPreviewText(mixedScopePreview.value));
  mixedScopeLoading.value = true;
  try {
    await maintainReviewAssignmentScopePlan(payload);
    proxy?.$modal.msgSuccess('混合范围分配已保存');
    mixedScopeVisible.value = false;
    await getList();
    if (queryParams.categoryId) await loadCategoryConfig(queryParams.categoryId);
  } finally {
    mixedScopeLoading.value = false;
  }
};

const loadCategoryConfig = async (categoryId = queryParams.categoryId) => {
  selectedScopeUnits.value = [];
  scopeUnitTableRef.value?.clearSelection?.();
  if (!categoryId) {
    categoryConfig.value = undefined;
    scopeFieldSchemas.value = [];
    return;
  }
  categoryConfigLoading.value = true;
  try {
    const [configResult, fieldResult] = await Promise.all([
      getReviewAssignmentCategoryConfig(categoryId),
      listReviewAssignmentFilterFields(categoryId)
    ]);
    categoryConfig.value = configResult.data;
    scopeFieldSchemas.value = fieldResult.data || [];
  } finally {
    categoryConfigLoading.value = false;
  }
};

const handleQueryActivityChange = async () => {
  queryParams.categoryId = undefined;
  queryCategoryOptions.value = [];
  categoryConfig.value = undefined;
  selectedScopeUnits.value = [];
  if (queryParams.activityId) {
    const { data } = await listCategoryOptions(queryParams.activityId);
    queryCategoryOptions.value = data || [];
  }
};

const handleQueryCategoryChange = async () => {
  queryParams.pageNum = 1;
  await Promise.all([loadCategoryConfig(), getList()]);
};

const openScopeConfig = async () => {
  if (!queryParams.categoryId) return;
  if (!categoryConfig.value || String(categoryConfig.value.categoryId) !== String(queryParams.categoryId)) {
    await loadCategoryConfig();
  }
  scopeFieldKeys.value = [...(categoryConfig.value?.scopeFieldKeys || [])];
  scopeConfigVisible.value = true;
};

const moveScopeField = (index: number, offset: number) => {
  const target = index + offset;
  if (target < 0 || target >= scopeFieldKeys.value.length) return;
  const next = [...scopeFieldKeys.value];
  [next[index], next[target]] = [next[target], next[index]];
  scopeFieldKeys.value = next;
};

const saveScopeConfig = async () => {
  const categoryId = queryParams.categoryId;
  if (!categoryId) return;
  scopeConfigSaving.value = true;
  try {
    await saveReviewAssignmentCategoryConfig(categoryId, {
      ...categoryConfig.value,
      categoryId,
      scopeFieldKeys: scopeFieldKeys.value,
      visibilityConfigured: Boolean(categoryConfig.value?.visibilityConfigured),
      hideSchoolInfo: categoryConfig.value?.hideSchoolInfo ?? true,
      hideMemberInfo: categoryConfig.value?.hideMemberInfo ?? false,
      hiddenFieldKeysJson: categoryConfig.value?.hiddenFieldKeysJson || ''
    });
    scopeConfigVisible.value = false;
    proxy?.$modal.msgSuccess('评审单元已重新生成，现有分配和评分未改动');
    await loadCategoryConfig(categoryId);
  } finally {
    scopeConfigSaving.value = false;
  }
};

const openCategoryVisibility = async (categoryId = effectiveCategoryId.value || queryParams.categoryId) => {
  if (!categoryId) return;
  const [configResult, fieldResult, fileResult] = await Promise.all([
    getReviewAssignmentCategoryConfig(categoryId),
    listReviewAssignmentFilterFields(categoryId),
    listFileRequirement(categoryId)
  ]);
  const config = configResult.data || {};
  Object.assign(visibilityForm, {
    ...config,
    categoryId,
    visibilityConfigured: true,
    hideSchoolInfo: true,
    hideMemberInfo: config.visibilityConfigured ? Boolean(config.hideMemberInfo) : false
  });
  const fieldSchemas = fieldResult.data || [];
  visibilityFieldSchemas.value = fieldSchemas;
  visibilityFileRequirements.value = fileResult.data || [];
  const hiddenValues = parseStringArray(config.hiddenFieldKeysJson);
  const hiddenFileIds = new Set(
    hiddenValues.filter((item) => item.startsWith(hiddenFileRequirementPrefix)).map((item) => item.substring(hiddenFileRequirementPrefix.length))
  );
  const configuredHiddenFields = hiddenValues.filter((item) => !item.startsWith(hiddenFileRequirementPrefix));
  const defaultSensitiveFields = config.visibilityConfigured
    ? []
    : fieldSchemas.filter((field) => field.sensitive && field.fieldKey).map((field) => String(field.fieldKey));
  visibilityHiddenFieldKeys.value = Array.from(new Set([...configuredHiddenFields, ...defaultSensitiveFields]));
  visibilityFieldKeyword.value = '';
  visibilityVisibleFileRequirementIds.value = visibilityFileRequirements.value
    .map((item) => String(item.id || ''))
    .filter((id) => id && !hiddenFileIds.has(id));
  visibilityConfigDirty.value = false;
  visibilityConfigVisible.value = true;
};

const isVisibilityFieldHidden = (fieldKey: string) => visibilityHideAllDynamicFields.value || visibilityHiddenFieldKeys.value.includes(fieldKey);

const setVisibilityFieldHidden = (fieldKey: string, hidden: boolean) => {
  const next = visibilityHideAllDynamicFields.value
    ? new Set(visibilityFieldOptions.value.map((item) => item.key))
    : new Set(visibilityHiddenFieldKeys.value);
  next.delete('*');
  if (hidden) next.add(fieldKey);
  else next.delete(fieldKey);
  visibilityHiddenFieldKeys.value = Array.from(next);
  visibilityConfigDirty.value = true;
};

const selectVisibilitySensitiveFields = () => {
  if (visibilityHideAllDynamicFields.value) return;
  visibilityHiddenFieldKeys.value = Array.from(new Set([...visibilityHiddenFieldKeys.value, ...visibilitySensitiveFieldKeys.value]));
  visibilityConfigDirty.value = true;
};

const selectAllVisibilityFields = () => {
  visibilityHiddenFieldKeys.value = visibilityFieldOptions.value.map((item) => item.key);
  visibilityConfigDirty.value = true;
};

const clearVisibilityFields = () => {
  visibilityHiddenFieldKeys.value = [];
  visibilityConfigDirty.value = true;
};

const setVisibilityFileVisible = (requirementId: string | number | undefined, visible: boolean) => {
  const id = String(requirementId || '');
  if (!id) return;
  const next = new Set(visibilityVisibleFileRequirementIds.value);
  if (visible) next.add(id);
  else next.delete(id);
  visibilityVisibleFileRequirementIds.value = Array.from(next);
  visibilityConfigDirty.value = true;
};

const saveCategoryVisibility = async () => {
  const categoryId = visibilityForm.categoryId;
  if (!categoryId) return;
  const visibleFileIds = new Set(visibilityVisibleFileRequirementIds.value);
  const hiddenFileKeys = visibilityFileRequirements.value
    .map((item) => String(item.id || ''))
    .filter((id) => id && !visibleFileIds.has(id))
    .map((id) => `${hiddenFileRequirementPrefix}${id}`);
  const hiddenFields = visibilityHiddenFieldKeys.value.map((item) => item.trim()).filter(Boolean);
  const hiddenValues = Array.from(new Set([...hiddenFields, ...hiddenFileKeys]));
  visibilityConfigSaving.value = true;
  try {
    const { data } = await saveReviewAssignmentCategoryConfig(categoryId, {
      ...visibilityForm,
      scopeFieldKeys: visibilityForm.scopeFieldKeys || [],
      visibilityConfigured: true,
      hiddenFieldKeysJson: hiddenValues.length ? JSON.stringify(hiddenValues) : ''
    });
    visibilityConfigDirty.value = false;
    visibilityConfigVisible.value = false;
    if (String(queryParams.categoryId || '') === String(categoryId)) {
      categoryConfig.value = data;
    }
    if (String(effectiveCategoryId.value || '') === String(categoryId)) {
      form.hideSchoolInfo = Boolean(data?.hideSchoolInfo);
      form.hideMemberInfo = Boolean(data?.hideMemberInfo);
      form.hiddenFieldKeysJson = data?.hiddenFieldKeysJson || '';
    }
    proxy?.$modal.msgSuccess('类别可见内容已统一保存');
    await Promise.all([getList(), String(queryParams.categoryId || '') === String(categoryId) ? loadCategoryConfig(categoryId) : Promise.resolve()]);
  } finally {
    visibilityConfigSaving.value = false;
  }
};

const handleFormActivityChange = async () => {
  if (form.batchTarget === 'project') return;
  form.categoryId = undefined;
  form.categoryIds = [];
  form.reviewerUserId = undefined;
  form.reviewerUserIds = [];
  formCategoryOptions.value = [];
  reviewerOptions.value = [];
  projectFilterFields.value = [];
  selectedProjectFilter.schoolTypes = [];
  selectedProjectFilter.fieldValues = {};
  selectedProjectFilter.scopeGroups = [];
  selectedProjectIds.value = [];
  projectOptions.value = [];
  categoryConfig.value = undefined;
  selectedScopeUnits.value = [];
  form.projectIdsJson = '';
  if (form.activityId) {
    const { data } = await listCategoryOptions(form.activityId);
    formCategoryOptions.value = data || [];
  }
  await loadProjectFilterFields();
};

const handleFormCategoryIdsChange = async (categoryIds: Array<string | number>) => {
  form.categoryIds = categoryIds;
  form.categoryId = categoryIds.length === 1 ? categoryIds[0] : undefined;
  form.reviewerUserId = undefined;
  form.reviewerUserIds = [];
  reviewerOptions.value = [];
  if (form.batchTarget !== 'project') {
    selectedProjectIds.value = [];
    projectOptions.value = [];
    form.projectIdsJson = '';
  }
  await loadProjectFilterFields();
  if (effectiveCategoryId.value) {
    await loadCategoryConfig(effectiveCategoryId.value);
    const data = categoryConfig.value;
    if (data?.visibilityConfigured) {
      form.hideSchoolInfo = Boolean(data.hideSchoolInfo);
      form.hideMemberInfo = Boolean(data.hideMemberInfo);
      form.hiddenFieldKeysJson = data.hiddenFieldKeysJson || '';
    }
  } else {
    categoryConfig.value = undefined;
    selectedScopeUnits.value = [];
  }
  await loadReviewerOptions('');
  if (form.batchTarget !== 'project') {
    await loadProjectOptions('');
  }
};

const loadReviewerOptions = async (keyword?: string) => {
  if (!form.activityId || !form.categoryIds?.length) {
    reviewerOptions.value = [];
    return;
  }
  reviewerLoading.value = true;
  try {
    const { data } = await listReviewerOptions(keyword || '', form.activityId, form.id ? effectiveCategoryId.value : undefined, form.reviewerUserId);
    reviewerOptions.value = data || [];
  } finally {
    reviewerLoading.value = false;
  }
};

const loadProjectOptions = async (keyword?: string) => {
  if (!form.activityId || !effectiveCategoryId.value) {
    projectOptions.value = [];
    return;
  }
  projectLoading.value = true;
  try {
    const { data } = await listApprovedReviewProjectOptions(form.activityId, effectiveCategoryId.value, keyword || '');
    projectOptions.value = data || [];
  } finally {
    projectLoading.value = false;
  }
};

const loadProjectFilterFields = async () => {
  const categoryIds = Array.from(new Set((form.categoryIds || []).filter((item) => item !== undefined && item !== null).map(String)));
  if (!categoryIds.length) {
    projectFilterFields.value = [];
    selectedProjectFilter.fieldValues = {};
    selectedProjectFilter.scopeGroups = [];
    syncProjectFilterJson();
    return;
  }
  const results = await Promise.all(categoryIds.map((categoryId) => listReviewAssignmentFilterFields(categoryId)));
  const schemaMaps = results.map((result) => {
    const map = new Map<string, { label: string; options: string[] }>();
    (result.data || []).forEach((field: CategoryFieldSchemaVO) => {
      if (!field.fieldKey || !['select', 'radio', 'checkbox'].includes(field.fieldType || '')) return;
      const options = parseStringArray(field.optionsJson);
      if (options.length) map.set(field.fieldKey, { label: field.fieldLabel || field.fieldKey, options });
    });
    return map;
  });
  const firstMap = schemaMaps[0] || new Map<string, { label: string; options: string[] }>();
  projectFilterFields.value = Array.from(firstMap.entries())
    .map(([key, first]) => {
      const peers = schemaMaps.map((map) => map.get(key));
      if (peers.some((item) => !item)) return undefined;
      const options = first.options.filter((item) => peers.every((peer) => peer?.options.includes(item)));
      return options.length ? { key, label: first.label, options } : undefined;
    })
    .filter((item): item is { key: string; label: string; options: string[] } => Boolean(item));
  const allowedKeys = new Set(projectFilterFields.value.map((item) => item.key));
  const nextValues: Record<string, string[]> = {};
  Object.entries(selectedProjectFilter.fieldValues).forEach(([key, values]) => {
    const field = projectFilterFields.value.find((item) => item.key === key);
    if (!allowedKeys.has(key) || !field) return;
    const kept = values.filter((value) => field.options.includes(value));
    nextValues[key] = kept;
  });
  projectFilterFields.value.forEach((field) => {
    nextValues[field.key] ||= [];
  });
  selectedProjectFilter.fieldValues = nextValues;
  selectedProjectFilter.scopeGroups = selectedProjectFilter.scopeGroups
    .map((group) => ({
      schoolTypes: (group.schoolTypes || []).filter(Boolean),
      fieldValues: Object.fromEntries(
        Object.entries(group.fieldValues || {})
          .filter(([key]) => allowedKeys.has(key))
          .map(([key, values]) => {
            const field = projectFilterFields.value.find((item) => item.key === key);
            const kept = values.filter((value) => value === '__unfilled__' || Boolean(field?.options.includes(value)));
            return [key, kept];
          })
          .filter(([, values]) => (values as string[]).length)
      )
    }))
    .filter((group) => group.schoolTypes.length || Object.keys(group.fieldValues).length);
  syncProjectFilterJson();
};

const getList = async () => {
  loading.value = true;
  try {
    const res: any = await listReviewAssignment(queryParams);
    assignmentList.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const resetQuery = async () => {
  queryParams.pageNum = 1;
  queryParams.activityId = undefined;
  queryParams.categoryId = undefined;
  queryParams.status = 'active';
  queryCategoryOptions.value = [];
  categoryConfig.value = undefined;
  selectedScopeUnits.value = [];
  await getList();
};

const handleProjectActivityChange = async () => {
  projectQuery.categoryId = undefined;
  projectCategoryOptions.value = [];
  if (projectQuery.activityId) {
    const { data } = await listCategoryOptions(projectQuery.activityId);
    projectCategoryOptions.value = data || [];
  }
};

const getProjectOverview = async () => {
  projectOverviewLoading.value = true;
  try {
    const res: any = await listReviewAssignmentProjects(projectQuery);
    projectOverviewList.value = res.rows || [];
    projectTotal.value = res.total || 0;
    selectedOverviewProjects.value = [];
    projectOverviewTableRef.value?.clearSelection?.();
  } finally {
    projectOverviewLoading.value = false;
  }
};

const handleProjectSearch = async () => {
  projectQuery.pageNum = 1;
  await getProjectOverview();
};

const resetProjectQuery = async () => {
  Object.assign(projectQuery, {
    pageNum: 1,
    pageSize: projectQuery.pageSize || 10,
    activityId: undefined,
    categoryId: undefined,
    schoolId: undefined,
    assignmentStatus: undefined,
    projectName: undefined
  });
  projectCategoryOptions.value = [];
  await getProjectOverview();
};

const openProjectOverview = async () => {
  projectOverviewVisible.value = true;
  if (queryParams.activityId) {
    projectQuery.activityId = queryParams.activityId;
    await handleProjectActivityChange();
    projectQuery.categoryId = queryParams.categoryId;
  }
  await getProjectOverview();
};

const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    activityId: undefined,
    categoryId: undefined,
    categoryIds: [],
    reviewerUserId: undefined,
    reviewerUserIds: [],
    batchOperation: 'merge',
    batchTarget: 'assignment',
    schoolScopeMode: 'all',
    schoolIdsJson: '',
    projectFilterJson: '',
    scoreMode: 'numeric_100',
    scoreRuleJson: defaultScoreRuleJson,
    exclusiveMode: 'single',
    scoreVisibilityPolicy: 'after_submit',
    hideSchoolInfo: false,
    hideMemberInfo: false,
    projectIdsJson: '',
    excludedProjectIdsJson: '',
    hiddenFieldKeysJson: '',
    status: 'active'
  });
  formCategoryOptions.value = [];
  selectedProjectIds.value = [];
  projectFilterFields.value = [];
  selectedProjectFilter.schoolTypes = [];
  selectedProjectFilter.fieldValues = {};
  selectedProjectFilter.scopeGroups = [];
  selectedSchoolIds.value = [];
  projectOptions.value = [];
  quickScoreText.value = defaultQuickScores.join(',');
  workspaceScopeView.value = 'board';
  workspacePreview.value = undefined;
  workspacePreviewSignature.value = '';
  workspaceReturnToProjectOverview.value = false;
};

const enterAssignmentWorkspace = async () => {
  formVisible.value = true;
  workspacePreview.value = undefined;
  workspacePreviewSignature.value = '';
  if (route.query.assignmentWorkspace !== '1') {
    await router.push({ query: { ...route.query, assignmentWorkspace: '1' } });
  }
};

const closeAssignmentWorkspace = async () => {
  const returnToProjectOverview = workspaceReturnToProjectOverview.value;
  workspaceReturnToProjectOverview.value = false;
  formVisible.value = false;
  workspacePreview.value = undefined;
  workspacePreviewSignature.value = '';
  if (route.query.assignmentWorkspace === '1') {
    const query = { ...route.query };
    delete query.assignmentWorkspace;
    await router.replace({ query });
  }
  if (queryParams.categoryId) {
    await loadCategoryConfig(queryParams.categoryId);
  }
  if (returnToProjectOverview) {
    projectOverviewVisible.value = true;
  }
};

const openForm = async (row?: ReviewAssignmentVO) => {
  resetForm();
  if (row) {
    Object.assign(form, row);
    form.categoryIds = row.categoryId ? [row.categoryId] : [];
    form.reviewerUserIds = [];
    form.schoolScopeMode = row.schoolScopeMode || 'all';
    selectedSchoolIds.value = parseIdArray(row.schoolIdsJson);
    syncSelectedProjectFilterFromJson();
    form.scoreVisibilityPolicy = form.scoreVisibilityPolicy || 'after_submit';
    if (form.activityId) {
      const { data } = await listCategoryOptions(form.activityId);
      formCategoryOptions.value = data || [];
    }
    syncSelectedProjectsFromJson();
    await loadProjectFilterFields();
    syncQuickScoreTextFromRule();
    await loadProjectOptions('');
    if (effectiveCategoryId.value) {
      await loadCategoryConfig(effectiveCategoryId.value);
    }
  } else {
    syncSelectedProjectFilterFromJson();
  }
  await loadReviewerOptions('');
  await enterAssignmentWorkspace();
};

const openScopeAssignment = async (units: ReviewAssignmentScopeUnitVO[]) => {
  if (!units.length || !categoryConfig.value?.activityId || !categoryConfig.value?.categoryId) return;
  if (units.length > 200) {
    proxy?.$modal.msgError('一次最多选择 200 个评审单元，请缩小范围后再分配');
    return;
  }
  resetForm();
  form.activityId = categoryConfig.value.activityId;
  form.categoryId = categoryConfig.value.categoryId;
  form.categoryIds = [categoryConfig.value.categoryId];
  form.batchOperation = 'merge';
  form.batchTarget = 'assignment';
  const categoryBaseline = assignmentList.value.find((item) => String(item.categoryId || '') === String(categoryConfig.value?.categoryId || ''));
  if (categoryBaseline) {
    form.scoreMode = categoryBaseline.scoreMode || form.scoreMode;
    form.scoreRuleJson = categoryBaseline.scoreRuleJson || form.scoreRuleJson;
    form.exclusiveMode = categoryBaseline.exclusiveMode || form.exclusiveMode;
    form.scoreVisibilityPolicy = categoryBaseline.scoreVisibilityPolicy || form.scoreVisibilityPolicy;
    syncQuickScoreTextFromRule();
  }
  form.hideSchoolInfo = Boolean(categoryConfig.value.hideSchoolInfo);
  form.hideMemberInfo = Boolean(categoryConfig.value.hideMemberInfo);
  form.hiddenFieldKeysJson = categoryConfig.value.hiddenFieldKeysJson || '';
  selectedProjectFilter.scopeGroups = units.map((unit) => ({
    schoolTypes: [],
    fieldValues: Object.fromEntries(Object.entries(unit.fieldValues || {}).map(([key, value]) => [key, [value]]))
  }));
  syncProjectFilterJson();
  const { data } = await listCategoryOptions(form.activityId);
  formCategoryOptions.value = data || [];
  await Promise.all([loadProjectFilterFields(), loadReviewerOptions('')]);
  await enterAssignmentWorkspace();
};

const openProjectBatch = async (operation: 'merge' | 'remove') => {
  const rows = selectedOverviewProjects.value;
  if (!rows.length) {
    proxy?.$modal.msgWarning('请先勾选至少一个项目');
    return;
  }
  const activityIds = new Set(rows.map((item) => String(item.activityId || '')));
  const categoryIds = new Set(rows.map((item) => String(item.categoryId || '')));
  if (activityIds.size !== 1 || categoryIds.size !== 1 || activityIds.has('') || categoryIds.has('')) {
    proxy?.$modal.msgError('批量维护要求所选项目属于同一活动和同一类别');
    return;
  }
  projectBatchOpening.value = operation;
  try {
    resetForm();
    const first = rows[0];
    form.batchTarget = 'project';
    form.batchOperation = operation;
    form.activityId = first.activityId;
    form.categoryId = first.categoryId;
    form.categoryIds = first.categoryId ? [first.categoryId] : [];
    selectedProjectIds.value = rows.map((item) => item.projectId!).filter((item) => item !== undefined && item !== null);
    syncProjectIdsJson();
    if (form.activityId) {
      const { data } = await listCategoryOptions(form.activityId);
      formCategoryOptions.value = data || [];
    }
    await loadReviewerOptions('');
    await loadCategoryConfig(form.categoryId);
    workspaceReturnToProjectOverview.value = true;
    projectOverviewVisible.value = false;
    await enterAssignmentWorkspace();
  } catch {
    formVisible.value = false;
    workspaceReturnToProjectOverview.value = false;
    projectOverviewVisible.value = true;
    proxy?.$modal.msgError(`未能打开“${operation === 'remove' ? '取消' : '补充'}评审人员”，请检查当前类别配置或网络后重试`);
  } finally {
    projectBatchOpening.value = '';
  }
};

const prepareAssignmentForm = async () => {
  await formRef.value?.validate();
  if ((form.id || form.batchOperation !== 'remove') && form.schoolScopeMode !== 'all' && !selectedSchoolIds.value.length) {
    proxy?.$modal.msgError('学校白名单或黑名单至少选择一所学校');
    return false;
  }
  form.schoolIdsJson = form.schoolScopeMode === 'all' ? '' : JSON.stringify(selectedSchoolIds.value);
  if (!form.id || selectedProjectIds.value.length) {
    form.excludedProjectIdsJson = '';
  }
  form.scoreVisibilityPolicy = form.scoreVisibilityPolicy || 'after_submit';
  syncProjectFilterJson();
  syncProjectIdsJson();
  if ((form.id || form.batchOperation !== 'remove') && form.scoreRuleJson && !isJsonObject(form.scoreRuleJson)) {
    proxy?.$modal.msgError('评分规则必须是 JSON 对象');
    return false;
  }
  if ((form.id || form.batchOperation !== 'remove') && !syncQuickScoresToRule()) return false;
  if ((form.id || form.batchOperation !== 'remove') && form.hiddenFieldKeysJson && !isJsonArray(form.hiddenFieldKeysJson)) {
    proxy?.$modal.msgError('隐藏字段必须是 JSON 数组');
    return false;
  }
  if (form.projectIdsJson && !isJsonArray(form.projectIdsJson)) {
    proxy?.$modal.msgError('作品ID必须是 JSON 数组');
    return false;
  }
  return true;
};

const previewWorkspaceChanges = async () => {
  if (!(await prepareAssignmentForm())) return;
  form.categoryId = undefined;
  form.reviewerUserId = undefined;
  workspacePreviewLoading.value = true;
  try {
    const { data } = await previewReviewAssignmentBatch(form);
    workspacePreview.value = data;
    workspacePreviewSignature.value = workspaceDraftSignature.value;
  } finally {
    workspacePreviewLoading.value = false;
  }
};

const submitForm = async () => {
  if (!(await prepareAssignmentForm())) return;
  if (!form.id && !workspacePreviewValid.value) {
    await previewWorkspaceChanges();
    return;
  }
  workspaceSubmitting.value = true;
  try {
    if (form.id) {
      form.categoryId = effectiveCategoryId.value;
      await updateReviewAssignment(form);
    } else {
      form.categoryId = undefined;
      form.reviewerUserId = undefined;
      await proxy?.$modal.confirm(batchPreviewText(workspacePreview.value));
      await maintainReviewAssignmentBatch(form);
    }
    proxy?.$modal.msgSuccess('保存成功');
    await getList();
    if (projectOverviewVisible.value || workspaceReturnToProjectOverview.value) {
      await getProjectOverview();
    }
    await closeAssignmentWorkspace();
  } finally {
    workspaceSubmitting.value = false;
  }
};

const handleDelete = async (row: ReviewAssignmentVO) => {
  await proxy?.$modal.confirm(`确认停用评审分配：${reviewerLabel(row)}？评分草稿会保留但不计入结果，已提交评分继续参与结果聚合。`);
  await delReviewAssignment(row.id!);
  proxy?.$modal.msgSuccess('已停用');
  await getList();
  if (queryParams.categoryId) {
    await loadCategoryConfig(queryParams.categoryId);
  }
};

const commonHiddenFieldOptions = [
  { key: '*', label: '全部动态表单字段' },
  { key: 'school_name', label: '学校名称' },
  { key: 'schoolName', label: '学校名称 schoolName' },
  { key: 'student_name', label: '学生姓名' },
  { key: 'studentName', label: '学生姓名 studentName' },
  { key: 'name', label: '姓名' },
  { key: 'id_card', label: '身份证号' },
  { key: 'idCard', label: '身份证号 idCard' },
  { key: 'teacher_name', label: '指导老师' },
  { key: 'teacherName', label: '指导老师 teacherName' },
  { key: 'teacher', label: '指导老师 teacher' },
  { key: 'adviserTeachers', label: '指导老师 adviserTeachers' },
  { key: 'instructor_name', label: '指导老师 instructor_name' },
  { key: 'phone', label: '联系电话' },
  { key: 'mobile', label: '手机号' },
  { key: 'email', label: '邮箱' },
  { key: 'department', label: '院系' },
  { key: 'major', label: '专业' },
  { key: 'class_name', label: '班级' },
  { key: 'description', label: '作品简述' },
  { key: 'remark', label: '备注' }
];

const reviewerOptionLabel = (item: any) => {
  const name = item.nickName || item.userName || item.userId || '-';
  return item.userName ? `${name}（${item.userName}）` : String(name);
};
const reviewerLabel = (row: ReviewAssignmentVO) => {
  const name = row.reviewerNickName || row.reviewerUserName || row.reviewerUserId || '-';
  return row.reviewerUserName ? `${name}（${row.reviewerUserName}）` : String(name);
};
const scoreModeLabel = (value?: string) => (value === 'grade' ? '等级制' : '百分制');
const exclusiveLabel = (value?: string) => (value === 'multi' ? '多评委可评' : '单评委互斥');
const scoreVisibilityLabel = (value?: string) =>
  ({ hidden: '一直不可见', always: '一直可见', after_submit: '评分后可见' })[value || 'after_submit'] || '评分后可见';
const scoreVisibilityType = (value?: string) => (value === 'always' ? 'success' : value === 'hidden' ? 'info' : 'warning');
const projectOptionLabel = (item: ProjectVO) => `${item.projectName || '-'}${item.projectNo ? `（${item.projectNo}）` : ''}`;
const parseIdArray = (text?: string) => {
  try {
    const parsed = text ? JSON.parse(text) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};
const parseStringArray = (text?: string) => {
  try {
    const parsed = text ? JSON.parse(text) : [];
    return Array.isArray(parsed) ? parsed.map(String).filter(Boolean) : [];
  } catch {
    return [];
  }
};
const schoolMatches = (schoolId?: string | number) => {
  if (form.schoolScopeMode === 'all') return true;
  if (schoolId === undefined || schoolId === null) return false;
  const selected = selectedSchoolIds.value.map(String);
  return form.schoolScopeMode === 'whitelist' ? selected.includes(String(schoolId)) : !selected.includes(String(schoolId));
};
const schoolScopeLabel = (row: ReviewAssignmentVO) => {
  if (row.schoolScopeMode === 'whitelist') return `白名单 ${parseIdArray(row.schoolIdsJson).length} 所`;
  if (row.schoolScopeMode === 'blacklist') return `黑名单 ${parseIdArray(row.schoolIdsJson).length} 所`;
  return '全部学校';
};
const schoolScopeType = (mode?: string) => (mode === 'whitelist' ? 'success' : mode === 'blacklist' ? 'warning' : 'info');
const schoolOptionLabel = (item: AssignmentSchoolOptionVO) =>
  item.schoolCode ? `${item.schoolName || '-'}（${item.schoolCode}）` : item.schoolName || String(item.id || '-');
const assignmentStatusLabel = (row: ReviewProjectOverviewVO) => {
  if (row.assignmentStatus === 'unassigned') return '未分配';
  if (row.assignmentStatus === 'shortage') return `人数不足 ${row.shortageCount || 0}`;
  return '已分配';
};
const assignmentStatusType = (status?: ReviewProjectOverviewVO['assignmentStatus']) =>
  status === 'assigned' ? 'success' : status === 'shortage' ? 'warning' : 'danger';
const scopeFieldLabel = (fieldKey: string) =>
  scopeFieldSchemas.value.find((field) => field.fieldKey === fieldKey)?.fieldLabel ||
  projectFilterFields.value.find((field) => field.key === fieldKey)?.label ||
  fieldKey;
const scopeValueLabel = (value: string) => (value === '__unfilled__' ? '未填写' : value);
const workspaceUnitScopeGroup = (unit: ReviewAssignmentScopeUnitVO): { schoolTypes: string[]; fieldValues: Record<string, string[]> } => ({
  schoolTypes: [],
  fieldValues: Object.fromEntries(
    (categoryConfig.value?.scopeFieldKeys || [])
      .map((fieldKey): [string, string[]] => [fieldKey, [unit.fieldValues?.[fieldKey] || '__unfilled__']])
      .filter(([, values]) => values[0])
  )
});
const workspaceScopeGroupKey = (fieldValues?: Record<string, string[]>) =>
  JSON.stringify(
    Object.entries(fieldValues || {})
      .map(([fieldKey, values]) => [fieldKey, [...values].map(String).sort()] as const)
      .sort(([leftKey], [rightKey]) => leftKey.localeCompare(rightKey))
  );
const isWorkspaceUnitSelected = (unit: ReviewAssignmentScopeUnitVO) => {
  const scopeFieldKeys = categoryConfig.value?.scopeFieldKeys || [];
  if (!scopeFieldKeys.length || !selectedProjectFilter.scopeGroups.length) return false;
  const unitKey = workspaceScopeGroupKey(workspaceUnitScopeGroup(unit).fieldValues);
  return selectedProjectFilter.scopeGroups.some((group) => workspaceScopeGroupKey(group.fieldValues) === unitKey);
};
const workspaceUnitShortLabel = (unit: ReviewAssignmentScopeUnitVO) => {
  const pathParts = String(unit.pathLabel || '')
    .split('/')
    .map((item) => item.trim())
    .filter(Boolean);
  return pathParts[pathParts.length - 1] || unit.pathLabel || '全部项目';
};
const setWorkspaceScopeUnits = (units: ReviewAssignmentScopeUnitVO[]) => {
  if (!(categoryConfig.value?.scopeFieldKeys || []).length) return;
  selectedProjectFilter.scopeGroups = units.map(workspaceUnitScopeGroup);
  syncProjectFilterJson();
};
const toggleWorkspaceUnit = (unit: ReviewAssignmentScopeUnitVO) => {
  if (!(categoryConfig.value?.scopeFieldKeys || []).length) return;
  if (workspaceUsesWholeCategory.value) {
    setWorkspaceScopeUnits([unit]);
    return;
  }
  const selected = selectedWorkspaceUnits.value;
  setWorkspaceScopeUnits(isWorkspaceUnitSelected(unit) ? selected.filter((item) => item.unitKey !== unit.unitKey) : [...selected, unit]);
};
const toggleAllWorkspaceUnits = () => {
  if (!(categoryConfig.value?.scopeFieldKeys || []).length) return;
  if (workspaceUsesWholeCategory.value || selectedWorkspaceUnitCount.value !== workspaceScopeUnits.value.length) {
    setWorkspaceScopeUnits(workspaceScopeUnits.value);
  } else {
    selectedProjectFilter.scopeGroups = [];
    syncProjectFilterJson();
  }
};
const handleWorkspaceTableSelection = (units: ReviewAssignmentScopeUnitVO[]) => {
  if (workspaceTableSyncing.value) return;
  setWorkspaceScopeUnits(units);
};
const syncWorkspaceTableSelection = async () => {
  if (workspaceScopeView.value !== 'table') return;
  await nextTick();
  workspaceTableSyncing.value = true;
  workspaceScopeTableRef.value?.clearSelection?.();
  selectedWorkspaceUnits.value.forEach((unit) => workspaceScopeTableRef.value?.toggleRowSelection?.(unit, true));
  await nextTick();
  workspaceTableSyncing.value = false;
};
const reviewerCoverageText = (unit: ReviewAssignmentScopeUnitVO) => {
  const min = unit.minimumAssignedReviewerCount || 0;
  const max = unit.maximumAssignedReviewerCount || 0;
  const required = unit.requiredReviewerCount || 0;
  const coverage = min === max ? `${min}` : `${min}～${max}`;
  return required ? `${coverage} / 需 ${required} 人` : `${coverage} 人`;
};
const scopeUnitStatusType = (status?: ReviewAssignmentScopeUnitVO['assignmentStatus']) =>
  status === 'assigned' ? 'success' : status === 'shortage' ? 'warning' : 'danger';
const scopeUnitStatusLabel = (unit: ReviewAssignmentScopeUnitVO) => {
  if (unit.assignmentStatus === 'assigned') return '已覆盖';
  if (unit.assignmentStatus === 'shortage') return `不足 ${unit.shortageProjectCount || 0} 项`;
  return '未分配';
};
const batchPreviewText = (preview?: AssignmentBatchPreviewVO) =>
  `执行预览：新增 ${preview?.createCount || 0} 条，更新 ${preview?.updateCount || 0} 条，停用 ${preview?.disableCount || 0} 条，不变 ${
    preview?.unchangedCount || 0
  } 条；保留已提交评分 ${preview?.preservedSubmittedScoreCount || 0} 条、草稿 ${preview?.preservedDraftScoreCount || 0} 条；可能脱离范围的草稿 ${
    preview?.orphanDraftScoreCount || 0
  } 条、受有效签字表锁定 ${preview?.lockedScoreCount || 0} 条。确认继续？`;
const syncSelectedProjectsFromJson = () => {
  try {
    selectedProjectIds.value = form.projectIdsJson ? JSON.parse(form.projectIdsJson) : [];
  } catch {
    selectedProjectIds.value = [];
  }
};
const syncProjectIdsJson = () => {
  form.projectIdsJson = selectedProjectIds.value.length ? JSON.stringify(selectedProjectIds.value) : '';
};
const syncProjectFilterJson = () => {
  const fieldValues = Object.fromEntries(
    Object.entries(selectedProjectFilter.fieldValues)
      .map(([key, values]) => [key, values.filter(Boolean)] as const)
      .filter(([, values]) => values.length)
  );
  const schoolTypes = selectedProjectFilter.schoolTypes.filter(Boolean);
  const scopeGroups = selectedProjectFilter.scopeGroups
    .map((group) => ({
      schoolTypes: (group.schoolTypes || []).filter(Boolean),
      fieldValues: Object.fromEntries(
        Object.entries(group.fieldValues || {})
          .map(([key, values]) => [key, values.filter(Boolean)] as const)
          .filter(([, values]) => values.length)
      )
    }))
    .filter((group) => group.schoolTypes.length || Object.keys(group.fieldValues).length);
  form.projectFilterJson =
    schoolTypes.length || Object.keys(fieldValues).length || scopeGroups.length ? JSON.stringify({ schoolTypes, fieldValues, scopeGroups }) : '';
};
const syncSelectedProjectFilterFromJson = () => {
  try {
    const parsed = form.projectFilterJson ? JSON.parse(form.projectFilterJson) : {};
    selectedProjectFilter.schoolTypes = Array.isArray(parsed?.schoolTypes) ? parsed.schoolTypes.map(String).filter(Boolean) : [];
    selectedProjectFilter.fieldValues = Object.fromEntries(
      Object.entries(parsed?.fieldValues || {})
        .filter(([, values]) => Array.isArray(values))
        .map(([key, values]) => [key, (values as unknown[]).map(String).filter(Boolean)])
    );
    selectedProjectFilter.scopeGroups = Array.isArray(parsed?.scopeGroups)
      ? parsed.scopeGroups
          .map((group: ReviewAssignmentProjectFilterVO) => ({
            schoolTypes: Array.isArray(group?.schoolTypes) ? group.schoolTypes.map(String).filter(Boolean) : [],
            fieldValues: Object.fromEntries(
              Object.entries(group?.fieldValues || {})
                .filter(([, values]) => Array.isArray(values))
                .map(([key, values]) => [key, (values as unknown[]).map(String).filter(Boolean)])
            )
          }))
          .filter((group: { schoolTypes: string[]; fieldValues: Record<string, string[]> }) =>
            Boolean(group.schoolTypes.length || Object.keys(group.fieldValues).length)
          )
      : [];
  } catch {
    selectedProjectFilter.schoolTypes = [];
    selectedProjectFilter.fieldValues = {};
    selectedProjectFilter.scopeGroups = [];
  }
};
const fieldHiddenCount = (row: ReviewAssignmentVO) => {
  try {
    const parsed = row.hiddenFieldKeysJson ? JSON.parse(row.hiddenFieldKeysJson) : [];
    return Array.isArray(parsed) ? parsed.filter((item) => !String(item).startsWith(hiddenFileRequirementPrefix)).length : 0;
  } catch {
    return 0;
  }
};
const hiddenFileRequirementCount = (row: ReviewAssignmentVO) => {
  try {
    const parsed = row.hiddenFieldKeysJson ? JSON.parse(row.hiddenFieldKeysJson) : [];
    return Array.isArray(parsed) ? parsed.filter((item) => String(item).startsWith(hiddenFileRequirementPrefix)).length : 0;
  } catch {
    return 0;
  }
};
const hiddenFileRequirementPrefix = '__file_requirement__:';
const fileRequirementLabel = (item: CategoryFileRequirementVO) =>
  `${item.fileTypeName || item.fileTypeCode || '未命名附件'}${item.required ? '（必传）' : ''}`;
const excludedProjectCount = (row: ReviewAssignmentVO) => parseIdArray(row.excludedProjectIdsJson).length;
const projectFilterCount = (row: ReviewAssignmentVO) => {
  try {
    const parsed = row.projectFilterJson ? JSON.parse(row.projectFilterJson) : {};
    const schoolCount = Array.isArray(parsed?.schoolTypes) && parsed.schoolTypes.length ? 1 : 0;
    const fieldCount = Object.entries(parsed?.fieldValues || {}).filter(([, values]) => Array.isArray(values) && values.length).length;
    const scopeGroupCount = Array.isArray(parsed?.scopeGroups) ? parsed.scopeGroups.length : 0;
    return schoolCount + fieldCount + scopeGroupCount;
  } catch {
    return 0;
  }
};
const scoreRuleObject = () => {
  try {
    const parsed = form.scoreRuleJson ? JSON.parse(form.scoreRuleJson) : {};
    return parsed && !Array.isArray(parsed) && typeof parsed === 'object' ? (parsed as Record<string, unknown>) : undefined;
  } catch {
    return undefined;
  }
};
const syncQuickScoreTextFromRule = () => {
  const configured = scoreRuleObject()?.quickScores;
  const scores = Array.isArray(configured) ? configured.map(Number).filter(Number.isFinite).slice(0, 5) : [];
  quickScoreText.value = (scores.length >= 2 ? scores : defaultQuickScores).join(',');
};
const syncQuickScoresToRule = () => {
  const rule = scoreRuleObject();
  if (!rule) return false;
  if (form.scoreMode === 'grade') {
    delete rule.quickScores;
    form.scoreRuleJson = JSON.stringify(rule);
    return true;
  }
  const tokens = quickScoreText.value
    .split(/[,，\s]+/)
    .map((item) => item.trim())
    .filter(Boolean);
  const scores = tokens.map(Number);
  if (scores.length < 2 || scores.length > 5 || scores.some((score) => !Number.isFinite(score)) || new Set(scores).size !== scores.length) {
    proxy?.$modal.msgError('预设分数需填写 2～5 个有效且不重复的数字');
    return false;
  }
  const min = Number(rule.min ?? 0);
  const max = Number(rule.max ?? 100);
  if (scores.some((score) => score < min || score > max)) {
    proxy?.$modal.msgError(`预设分数必须在 ${min}～${max} 范围内`);
    return false;
  }
  rule.quickScores = scores;
  form.scoreRuleJson = JSON.stringify(rule);
  quickScoreText.value = scores.join(',');
  return true;
};
const isJsonObject = (text: string) => {
  try {
    const parsed = JSON.parse(text);
    return !!parsed && !Array.isArray(parsed) && typeof parsed === 'object';
  } catch {
    return false;
  }
};
const isJsonArray = (text: string) => {
  try {
    return Array.isArray(JSON.parse(text));
  } catch {
    return false;
  }
};

watch([workspaceScopeView, () => workspaceScopeUnits.value.map((unit) => unit.unitKey).join('|')], async ([view]) => {
  if (view === 'table') {
    await syncWorkspaceTableSelection();
  }
});

watch(
  () => route.query.assignmentWorkspace,
  async (workspaceMode) => {
    if (workspaceMode === '1' && !formVisible.value) {
      await openForm();
    } else if (workspaceMode !== '1' && formVisible.value) {
      formVisible.value = false;
      workspacePreview.value = undefined;
      workspacePreviewSignature.value = '';
    }
  }
);

onMounted(async () => {
  await Promise.all([loadActivityOptions(), loadSchoolOptions()]);
  await getList();
  if (route.query.assignmentWorkspace === '1') {
    await openForm();
  }
});
</script>

<style scoped>
.assignment-workspace {
  min-height: calc(100vh - 104px);
  margin: -8px;
  background: var(--el-bg-color-page);
}

.assignment-workspace__header {
  position: sticky;
  z-index: 5;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-height: 76px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: color-mix(in srgb, var(--el-bg-color) 94%, transparent);
  backdrop-filter: blur(8px);
}

.assignment-workspace__title,
.assignment-workspace__title > div,
.assignment-workspace__header-actions,
.workspace-panel__header > div,
.workspace-collapse-title,
.workspace-locked-projects {
  display: flex;
  align-items: center;
}

.assignment-workspace__title {
  gap: 12px;
}

.assignment-workspace__title > div {
  align-items: flex-start;
  flex-direction: column;
}

.assignment-workspace__title h2,
.workspace-panel h3,
.workspace-collapse-title h3,
.workspace-locked-projects h3 {
  margin: 0;
  color: var(--el-text-color-primary);
}

.assignment-workspace__title h2 {
  font-size: 18px;
}

.assignment-workspace__title p,
.workspace-panel__header p,
.workspace-collapse-title p,
.workspace-locked-projects p {
  margin: 3px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.assignment-workspace__header-actions {
  flex: 0 0 auto;
  gap: 8px;
}

.assignment-workspace__form {
  padding: 16px 20px 28px;
}

.assignment-workspace__context {
  display: grid;
  grid-template-columns: minmax(220px, 0.75fr) minmax(260px, 1fr) minmax(480px, 2fr);
  gap: 12px 18px;
  padding: 16px 18px 4px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.assignment-workspace__context :deep(.el-form-item),
.assignment-workspace__aside :deep(.el-form-item) {
  margin-bottom: 14px;
}

.assignment-workspace__context :deep(.el-select),
.assignment-workspace__aside :deep(.el-select),
.workspace-advanced-scope :deep(.el-select) {
  width: 100%;
}

.assignment-workspace__scope-selector {
  min-width: 0;
}

.assignment-workspace__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  align-items: start;
  gap: 16px;
  margin-top: 16px;
}

.assignment-workspace__main,
.assignment-workspace__aside {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.assignment-workspace__aside {
  position: sticky;
  top: 92px;
}

.workspace-panel {
  padding: 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.workspace-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.workspace-panel__header > div,
.workspace-collapse-title {
  gap: 10px;
  min-width: 0;
}

.workspace-panel h3,
.workspace-collapse-title h3,
.workspace-locked-projects h3 {
  font-size: 15px;
}

.workspace-step {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 700;
  background: var(--el-color-primary-light-9);
}

.workspace-step.is-secondary {
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
}

.workspace-scope-summary {
  display: flex;
  align-items: center;
  gap: 18px;
  min-height: 38px;
  margin-bottom: 12px;
  padding: 0 12px;
  border-radius: 6px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  background: var(--el-fill-color-lighter);
}

.workspace-scope-summary .el-button {
  margin-left: auto;
}

.scope-board {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.scope-board__group {
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 7px;
}

.scope-board__group-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 40px;
  padding: 0 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
  background: var(--el-fill-color-extra-light);
}

.scope-board__group-title small {
  color: var(--el-text-color-secondary);
  font-weight: 400;
}

.scope-board__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
  gap: 10px;
  padding: 10px;
}

.scope-unit-tile {
  position: relative;
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 7px;
  min-height: 132px;
  padding: 13px;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  border-radius: 7px;
  color: var(--el-text-color-primary);
  text-align: left;
  background: var(--el-bg-color);
  cursor: pointer;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    background 0.15s ease;
}

.scope-unit-tile:hover {
  border-color: var(--el-color-primary-light-5);
}

.scope-unit-tile.is-selected {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  box-shadow: 0 0 0 1px var(--el-color-primary-light-7);
}

.scope-unit-tile__check {
  position: absolute;
  top: 0;
  right: 0;
  display: none;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 0 0 0 8px;
  color: #fff;
  background: var(--el-color-primary);
}

.scope-unit-tile.is-selected .scope-unit-tile__check {
  display: flex;
}

.scope-unit-tile__path {
  display: -webkit-box;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.scope-unit-tile__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  color: var(--el-text-color-regular);
  font-size: 12px;
}

.scope-unit-tile.is-shortage:not(.is-selected) {
  border-color: var(--el-color-warning-light-5);
}

.scope-unit-tile.is-unassigned:not(.is-selected) {
  border-color: var(--el-color-danger-light-7);
}

.workspace-panel :deep(.el-collapse) {
  border: 0;
}

.workspace-panel :deep(.el-collapse-item__header) {
  height: auto;
  min-height: 44px;
  border: 0;
  line-height: normal;
}

.workspace-panel :deep(.el-collapse-item__wrap) {
  border: 0;
}

.workspace-panel :deep(.el-collapse-item__content) {
  padding: 12px 0 0;
}

.workspace-advanced-scope {
  padding: 4px 2px;
}

.project-filter-label {
  margin: 12px 0 7px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  font-weight: 600;
}

.workspace-locked-projects {
  gap: 10px;
}

.workspace-reviewer-panel {
  border-color: var(--el-color-primary-light-7);
}

.workspace-visibility-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 28px;
  margin-bottom: 12px;
}

.workspace-muted {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.workspace-preview-panel.is-ready {
  border-color: var(--el-color-success-light-5);
}

.workspace-preview-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 14px;
}

.workspace-preview-grid > div {
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  min-height: 58px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
}

.workspace-preview-grid strong {
  color: var(--el-text-color-primary);
  font-size: 18px;
}

.workspace-preview-grid span {
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.workspace-submit-button {
  width: 100%;
  margin-top: 14px;
}

.batch-operation-tip {
  width: 100%;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.project-batch-hint {
  margin-left: 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.project-overview-filter {
  margin-bottom: 4px;
}

.project-batch-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  color: var(--el-text-color-secondary);
}

.project-batch-toolbar > div,
.visibility-field-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.project-batch-toolbar .el-button,
.visibility-field-actions .el-button {
  margin-left: 0;
}

.visibility-config-form :deep(.el-form-item__content),
.visibility-base-options,
.visibility-field-panel {
  width: 100%;
}

.visibility-base-options {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px 18px;
}

.visibility-fixed-rule {
  display: flex;
  flex: 1 1 420px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 52px;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);
}

.visibility-fixed-rule > div,
.visibility-field-toolbar > div:first-child,
.visibility-field-main > span:first-child {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  min-width: 0;
}

.visibility-fixed-rule span,
.visibility-field-toolbar span,
.visibility-field-main small {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.visibility-field-panel {
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 7px;
}

.visibility-field-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-extra-light);
}

.visibility-field-panel > .el-input,
.visibility-field-panel > .el-alert {
  margin: 10px 12px 0;
  width: calc(100% - 24px);
}

.visibility-field-scroll {
  margin-top: 10px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.visibility-field-list {
  display: flex;
  flex-direction: column;
}

.visibility-field-row {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 54px;
  height: auto;
  margin: 0;
  padding: 8px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.visibility-field-row:last-child {
  border-bottom: 0;
}

.visibility-field-row:hover {
  background: var(--el-fill-color-extra-light);
}

.visibility-field-row :deep(.el-checkbox__label) {
  flex: 1 1 auto;
  min-width: 0;
  padding-left: 10px;
}

.visibility-field-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  min-width: 0;
  white-space: normal;
}

.visibility-field-main strong {
  overflow-wrap: anywhere;
}

.visibility-field-tags {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 5px;
}

.review-unit-head,
.review-unit-toolbar,
.scope-order-item,
.visibility-file-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.review-unit-head > div:first-child {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.review-unit-head span,
.review-unit-toolbar,
.visibility-file-row small {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.review-unit-toolbar {
  margin-bottom: 10px;
}

.scope-order-list,
.visibility-file-list {
  width: 100%;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
}

.scope-order-item,
.visibility-file-row {
  min-height: 48px;
  padding: 0 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.scope-order-item:last-child,
.visibility-file-row:last-child {
  border-bottom: 0;
}

.scope-order-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.scope-order-item > span:nth-child(2) {
  flex: 1;
}

.visibility-file-row > div {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.scope-group-summary {
  margin-top: 4px;
  line-height: 1.6;
}

.mixed-scope-workspace {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.mixed-scope-context {
  display: grid;
  grid-template-columns: minmax(220px, 0.8fr) minmax(220px, 0.8fr) minmax(320px, 1.4fr);
  gap: 12px;
  padding: 14px 16px 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
}

.mixed-scope-context :deep(.el-select),
.mixed-scope-score-config :deep(.el-select) {
  width: 100%;
}

.mixed-scope-categories {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 12px;
  min-height: 140px;
}

.mixed-scope-category {
  padding: 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.mixed-scope-category header,
.mixed-scope-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.mixed-scope-category header > div,
.mixed-scope-preview > div {
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: 3px;
}

.mixed-scope-category header span,
.mixed-scope-preview span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.mixed-scope-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.mixed-scope-options :deep(.el-checkbox) {
  height: 34px;
  margin: 0;
}

.mixed-scope-options small {
  margin-left: 6px;
  color: var(--el-text-color-secondary);
}

.mixed-scope-score-config {
  padding: 0 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
}

.mixed-scope-preview {
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}

.mixed-scope-preview.is-ready {
  border-color: var(--el-color-success-light-5);
}

@media (max-width: 780px) {
  .project-batch-toolbar,
  .review-unit-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .assignment-workspace {
    margin: -8px;
  }

  .assignment-workspace__header,
  .assignment-workspace__body,
  .assignment-workspace__context {
    display: flex;
    align-items: stretch;
    flex-direction: column;
  }

  .assignment-workspace__header-actions {
    width: 100%;
  }

  .assignment-workspace__header-actions .el-button {
    flex: 1;
  }

  .assignment-workspace__form {
    padding: 12px;
  }

  .assignment-workspace__aside {
    position: static;
  }

  .scope-board__grid {
    grid-template-columns: 1fr;
  }

  .mixed-scope-context,
  .mixed-scope-categories {
    grid-template-columns: 1fr;
  }

  .mixed-scope-preview {
    align-items: stretch;
    flex-direction: column;
  }

  .visibility-field-toolbar,
  .visibility-field-main {
    align-items: stretch;
    flex-direction: column;
  }

  .visibility-field-actions {
    width: 100%;
  }

  .visibility-field-actions .el-button {
    flex: 1 1 auto;
  }

  .visibility-field-tags {
    justify-content: flex-start;
  }
}

@media (min-width: 781px) and (max-width: 1280px) {
  .assignment-workspace__context {
    grid-template-columns: 1fr 1fr;
  }

  .assignment-workspace__scope-selector {
    grid-column: 1 / -1;
  }

  .assignment-workspace__body {
    grid-template-columns: minmax(0, 1fr) 340px;
  }
}
</style>
