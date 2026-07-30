<template>
  <div class="p-2 audit-page">
    <div
      class="audit-workspace"
      :class="{ 'is-top-layout': !auditSidebarOpen }"
      :style="{ '--navigator-sidebar-width': `${detailDisplayConfig.navigator.sidebarWidth}px` }"
    >
      <ProjectBrowseNavigator
        v-if="auditSidebarOpen"
        page-key="audit"
        class="audit-filter-card"
        :activities="activityOptions"
        :activity-id="queryParams.activityId"
        :show-activity="false"
        :categories="categoryOptions"
        :category-count-map="auditCategoryCountMap"
        :selected-category-key="String(selectedBrowseCategoryId || '')"
        :browse-mode="browseMode"
        :unit-options="auditUnitOptions"
        :selected-unit-id="queryParams.schoolId"
        compact-sidebar
        force-sidebar
        collapsible
        sidebar-title="筛选项目"
        @update:browse-mode="handleNavigatorBrowseModeChange"
        @update:collapsed="auditSidebarOpen = !$event"
        @select-category="selectNavigatorCategory"
        @select-unit="selectNavigatorUnit"
      />

      <el-card shadow="hover" class="audit-review-card">
        <ArtProjectWorkspaceHeader class="audit-workspace-header" page-key="audit" :title-variables="{ activityName: selectedActivityName }">
          <template #navigatorToggle>
            <ArtWorkspaceConfigButton
              v-if="auditHeaderConfig.sidebarEnabled"
              :icon="auditSidebarOpen ? 'Fold' : 'Expand'"
              :text="auditSidebarOpen ? auditHeaderConfig.navigatorToggleButton.alternateText : auditHeaderConfig.navigatorToggleButton.text"
              :tooltip="auditSidebarOpen ? auditHeaderConfig.navigatorToggleButton.alternateTooltip : auditHeaderConfig.navigatorToggleButton.tooltip"
              @click="auditSidebarOpen = !auditSidebarOpen"
            />
          </template>
          <template #categoryFilter>
            <div class="audit-hierarchy-filter">
              <span>{{ auditHeaderText('categoryFilter', 'categoryLabel', '类别') }}</span>
              <ArtCategoryTreeDropdown
                :tree="categoryTree"
                :count-map="auditCategoryCountMap"
                :selected-key="String(selectedBrowseCategoryId || '')"
                :disabled="!queryParams.activityId"
                :placeholder="auditHeaderText('categoryFilter', 'categoryPlaceholder', '全部类别')"
                :all-label="auditHeaderText('categoryFilter', 'categoryPlaceholder', '全部类别')"
                @select="selectNavigatorCategory"
              />
            </div>
          </template>
          <template #groupFilter>
            <div class="audit-hierarchy-filter">
              <span>{{ auditHeaderText('groupFilter', 'groupLabel', '组别') }}</span>
              <el-select
                :model-value="queryParams.groupCode"
                clearable
                filterable
                :placeholder="auditHeaderText('groupFilter', 'groupPlaceholder', '所有组别')"
                @update:model-value="handleAuditGroupChange"
              >
                <el-option :label="auditHeaderText('groupFilter', 'groupPlaceholder', '所有组别')" value="" />
                <el-option
                  v-for="item in auditGroupOptions"
                  :key="item.value"
                  :label="item.count === undefined ? item.label : `${item.label}（${item.count}）`"
                  :value="item.value"
                />
              </el-select>
            </div>
          </template>
          <template #schoolFilter>
            <div class="audit-hierarchy-filter">
              <span>{{ auditHeaderText('schoolFilter', 'schoolLabel', '学校') }}</span>
              <el-select
                v-model="queryParams.schoolId"
                clearable
                filterable
                :placeholder="auditHeaderText('schoolFilter', 'schoolPlaceholder', '搜索学校')"
                @change="handleAuditSchoolChange"
              >
                <el-option v-for="item in auditUnitOptions" :key="String(item.id)" :label="item.label" :value="item.id" />
              </el-select>
            </div>
          </template>
          <template #progress>
            <div v-if="auditProgressConfig.visible" class="audit-progress" :style="auditProgressStyle">
              <div class="audit-progress__summary">
                <strong>{{ auditProgressConfig.title }}</strong
                ><span>{{ auditProgressText }}</span>
              </div>
              <el-progress
                :percentage="auditProgress.percentage"
                :color="auditProgressColor"
                :stroke-width="auditProgressConfig.height"
                :show-text="false"
              />
            </div>
          </template>
          <template #status>
            <ArtStatusTabs
              :items="auditStatusTabItems"
              :model-value="queryParams.status"
              :collapse-on-overflow="auditHeaderConfig.statusCollapseOnOverflow"
              aria-label="审核状态"
              @change="handleStatusChange(String($event ?? ''))"
            />
          </template>
          <template #search>
            <div class="audit-header-search art-list-header-search">
              <el-input
                v-model="queryParams.projectName"
                class="audit-project-search"
                :placeholder="auditHeaderText('search', 'placeholder', '请输入项目名称')"
                clearable
                @keyup.enter="refreshBrowseData"
              />
              <el-button type="primary" icon="Search" @click="refreshBrowseData">{{ auditHeaderText('search', 'search', '搜索') }}</el-button>
              <el-button icon="Refresh" @click="resetQuery">{{ auditHeaderText('search', 'reset', '重置') }}</el-button>
            </div>
          </template>
          <template #actions>
            <el-button v-if="canManageAuditScope" type="primary" plain icon="UserFilled" @click="openAuditAssignment">{{
              auditHeaderText('actions', 'auditAssignment', '审核权限分配')
            }}</el-button>
          </template>
          <template #columnWidthReset>
            <ArtWorkspaceConfigButton
              icon="RefreshLeft"
              :text="auditHeaderConfig.columnWidthResetButton.text"
              :tooltip="auditHeaderConfig.columnWidthResetButton.tooltip"
              @click="restoreAuditColumnWidths"
            />
          </template>
        </ArtProjectWorkspaceHeader>
        <el-alert v-if="managedActivityError" class="mt-2" type="warning" :closable="false" :title="managedActivityError" />
        <div ref="auditTableShellRef" class="art-resizable-table-shell art-resizable-table-shell--fit">
          <el-table
            v-loading="loading"
            class="audit-project-table art-list-table art-resizable-table art-resizable-table--borderless"
            :class="tableAppearanceClass"
            :style="tableAppearanceStyle"
            border
            highlight-current-row
            :data="projectList"
            :empty-text="auditTablePage.emptyText"
            row-class-name="audit-table-row"
            @header-dragend="handleAuditColumnResize"
            @row-click="openDetail"
          >
            <el-table-column
              v-for="column in auditTableColumns"
              :key="column.key"
              :label="column.label"
              :column-key="column.key"
              :prop="column.source === 'builtin' && !['actions', 'serial'].includes(column.key) ? column.key : undefined"
              :width="auditColumnWidth(column.key)"
              :min-width="column.minWidth"
              :resizable="column.resizable"
              :fixed="column.fixed"
              :align="['serial', 'actions'].includes(column.key) ? 'center' : undefined"
              :class-name="
                column.key === 'serial'
                  ? 'art-table-nowrap-cell'
                  : column.key === 'actions'
                    ? 'audit-action-cell'
                    : column.key === 'projectName' ||
                        column.key === 'categoryName' ||
                        column.key === 'schoolName' ||
                        column.key === 'projectNo' ||
                        column.key === 'submittedAt' ||
                        column.key === 'currentAuditOpinion' ||
                        column.source === 'form'
                      ? 'audit-wrap-cell art-table-configurable-wrap-cell'
                      : 'art-table-nowrap-cell'
              "
              :label-class-name="column.key === 'actions' ? 'art-nonresizable-header' : 'art-resizable-header'"
            >
              <template #default="scope">
                <span v-if="column.key === 'serial'">{{ auditSerialNumber(scope.$index) }}</span>
                <ArtListStatusTag
                  v-else-if="column.key === 'status'"
                  :semantic="statusSemantic(scope.row.status)"
                  :label="auditStatusText(statusSemantic(scope.row.status), statusLabel(scope.row.status))"
                  :tag-type="statusType(scope.row.status)"
                />
                <div v-else-if="column.key === 'actions'" class="audit-row-actions">
                  <el-button
                    class="audit-list-action-button audit-list-action-button--view"
                    type="primary"
                    link
                    :icon="actionIcon('View')"
                    :disabled="!canDetailQuery"
                    :title="canDetailQuery ? '查看项目详情' : '无项目详情查看权限'"
                    @click.stop="openDetail(scope.row)"
                  >
                    {{ auditActionText('view', '查看') }}
                  </el-button>
                  <el-button
                    class="audit-list-action-button audit-list-action-button--return"
                    type="danger"
                    link
                    :icon="actionIcon('Back')"
                    :disabled="!canReturnAuditProject(scope.row)"
                    :title="auditReturnButtonTitle(scope.row)"
                    @click.stop="openAuditAction(scope.row, 'return')"
                  >
                    {{ auditActionText('return', '退回') }}
                  </el-button>
                  <el-button
                    class="audit-list-action-button audit-list-action-button--withdraw"
                    type="warning"
                    link
                    :icon="actionIcon('RefreshLeft')"
                    :disabled="!auditWithdrawAction(scope.row)"
                    :title="auditWithdrawButtonTitle(scope.row)"
                    @click.stop="openAuditWithdrawAction(scope.row)"
                  >
                    {{ auditActionText('withdraw', '撤销') }}
                  </el-button>
                </div>
                <span v-else>{{ auditColumnText(scope.row, column) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
      </el-card>
    </div>

    <ArtPopupDrawer
      v-model="auditAssignmentVisible"
      title="审核权限分配"
      size="80%"
      wide
      append-to-body
      destroy-on-close
      class="audit-assignment-drawer"
    >
      <AuditAssignmentPage v-if="auditAssignmentVisible" />
    </ArtPopupDrawer>

    <el-drawer
      v-model="detailVisible"
      size="90%"
      destroy-on-close
      :close-on-click-modal="true"
      :with-header="false"
      class="audit-detail-drawer art-detail-workbench-drawer art-popup art-popup--drawer is-wide"
    >
      <template v-if="currentProject">
        <div class="art-detail-workbench audit-detail-shell">
          <header class="art-detail-workbench__header">
            <div class="art-detail-workbench__leading">
              <el-tooltip content="关闭" placement="bottom">
                <el-button class="art-detail-workbench__close" circle icon="Close" aria-label="关闭" @click="detailVisible = false" />
              </el-tooltip>
              <div class="art-detail-workbench__title">
                <strong class="art-detail-workbench__name">{{ currentProject.projectName }}</strong>
                <span class="art-detail-workbench__meta">作品编号 · {{ currentProject.projectNo || '暂无编号' }}</span>
                <el-tag size="small" :type="statusType(currentProject.status)">{{ statusLabel(currentProject.status) }}</el-tag>
              </div>
            </div>
            <div class="art-detail-workbench__nav">
              <span v-if="currentProjectPositionText" class="art-detail-workbench__position">{{ currentProjectPositionText }}</span>
              <el-button-group :class="{ 'is-navigation-hint': adjacentNavigationHintActive }">
                <el-button class="detail-nav-button" icon="ArrowLeft" :disabled="!hasPreviousProject" @click="openAdjacentProject(-1)"
                  >上一条</el-button
                >
                <el-button class="detail-nav-button" icon="ArrowRight" :disabled="!hasNextProject" @click="openAdjacentProject(1)">下一条</el-button>
              </el-button-group>
            </div>
          </header>

          <main class="art-detail-workbench__main">
            <section class="art-detail-workbench__content">
              <nav class="art-detail-workbench__tabs" aria-label="审核详情内容">
                <button
                  v-for="tab in availableAuditTabs"
                  :key="tab"
                  type="button"
                  class="art-detail-workbench__tab"
                  :class="{ 'is-active': auditDetailTab === tab }"
                  @click="auditDetailTab = tab"
                >
                  {{ auditTabLabel(tab) }}
                </button>
                <div v-if="auditDetailTab === 'form' && isAuditBasicSectionVisible('downloadWorkbook')" class="art-detail-workbench__tab-extra">
                  <el-button size="small" icon="Download" @click="downloadDetailWorkbook">下载表格</el-button>
                </div>
              </nav>

              <div class="art-detail-workbench__view" :class="{ 'is-scroll': auditDetailTab !== 'preview' }">
                <div v-if="auditDetailTab === 'preview'" class="art-detail-workbench__preview">
                  <div class="art-detail-workbench__preview-toolbar">
                    <strong class="art-detail-workbench__preview-title">{{ selectedAuditFileTitle || '作品预览' }}</strong>
                    <div class="art-detail-workbench__preview-actions">
                      <el-button
                        v-if="selectedAuditFile && canInlinePreview(selectedAuditFile)"
                        size="small"
                        icon="View"
                        @click="openPreview(selectedAuditFile)"
                        >弹窗</el-button
                      >
                      <el-button v-if="selectedAuditFile?.storagePath" size="small" icon="Download" @click="openFile(selectedAuditFile)"
                        >原文件</el-button
                      >
                      <el-button v-if="selectedAuditPreviewUrl" size="small" icon="FullScreen" @click="windowOpen(selectedAuditPreviewUrl)"
                        >新窗口</el-button
                      >
                    </div>
                  </div>
                  <div class="art-detail-workbench__preview-stage">
                    <video v-if="selectedAuditPreviewType === 'video'" :src="selectedAuditPreviewUrl" controls preload="metadata" />
                    <iframe v-else-if="selectedAuditPreviewType === 'pdf'" :src="pdfViewerUrl(selectedAuditPreviewUrl)" />
                    <img v-else-if="selectedAuditPreviewType === 'image'" :src="selectedAuditPreviewUrl" alt="" />
                    <el-empty v-else description="请选择可预览作品文件，或下载原文件查看" />
                  </div>
                </div>

                <template v-else-if="auditDetailTab === 'form'">
                  <el-table v-if="isAuditBasicSectionVisible('formFields')" border :data="formRows">
                    <el-table-column label="字段" prop="label" width="180" />
                    <el-table-column label="内容" min-width="260" class-name="audit-form-content-column">
                      <template #default="scope">
                        <div class="audit-form-value" :class="{ 'is-collapsed': isFormRowCollapsed(scope.row) }">{{ scope.row.value }}</div>
                        <el-button
                          v-if="isLongFormRow(scope.row)"
                          class="audit-form-toggle"
                          link
                          type="primary"
                          size="small"
                          @click.stop="toggleFormRow(scope.row)"
                        >
                          {{ isFormRowCollapsed(scope.row) ? '展开' : '收起' }}
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <ProjectGenericTableReadonly v-if="isAuditBasicSectionVisible('genericTables')" :project="currentProject" />
                  <el-empty v-if="!auditBasicContentVisible" :image-size="54" description="基础信息已在显示设置中隐藏" />
                </template>

                <template v-else-if="auditDetailTab === 'members'">
                  <ArtProjectMemberReadonly :project="currentProject" />
                </template>

                <template v-else-if="auditDetailTab === 'files'">
                  <el-table v-if="detailDisplayConfig.audit.content.fileColumns.length" border :data="currentProject.files || []">
                    <el-table-column v-if="isAuditFileColumnVisible('fileType')" label="材料类型" width="150">
                      <template #default="scope">{{ detailFileTypeLabel(currentProject, scope.row) || '-' }}</template>
                    </el-table-column>
                    <el-table-column
                      v-if="isAuditFileColumnVisible('fileName')"
                      label="文件名"
                      prop="originalName"
                      min-width="180"
                      show-overflow-tooltip
                    />
                    <el-table-column v-if="isAuditFileColumnVisible('fileSize')" label="大小" width="110">
                      <template #default="scope">{{ formatSize(scope.row.fileSize) }}</template>
                    </el-table-column>
                    <el-table-column v-if="isAuditFileColumnVisible('technicalCheck')" label="技术校验" min-width="190">
                      <template #default="scope">
                        <div v-if="scope.row.checkStatus === 'warning'" class="manual-review-cell">
                          <span class="manual-review-hint">{{ detailCheckStatusLabel(currentProject, scope.row) }}</span>
                          <el-popover
                            placement="right-start"
                            trigger="click"
                            :width="360"
                            :fallback-placements="['left-start', 'bottom-start', 'top-start']"
                          >
                            <div class="manual-review-popover">
                              <div v-if="manualReviewSubject(scope.row)" class="manual-review-popover-subject">
                                {{ manualReviewSubject(scope.row) }}
                              </div>
                              <div class="manual-review-popover-content">{{ manualReviewDetail(scope.row) }}</div>
                            </div>
                            <template #reference>
                              <button type="button" class="manual-review-mark" aria-label="查看技术校验详细说明" @click.stop>!</button>
                            </template>
                          </el-popover>
                        </div>
                        <template v-else>
                          <el-tag :type="checkTagType(scope.row.checkStatus)">{{ detailCheckStatusLabel(currentProject, scope.row) }}</el-tag>
                          <div v-if="detailCheckFailureMessage(currentProject, scope.row)" class="text-xs text-red-500">
                            {{ detailCheckFailureMessage(currentProject, scope.row) }}
                          </div>
                        </template>
                      </template>
                    </el-table-column>
                    <el-table-column v-if="isAuditFileColumnVisible('mediaInfo')" label="媒体信息" min-width="220">
                      <template #default="scope">
                        <div class="audit-media-summary">{{ mediaSummary(scope.row) }}</div>
                      </template>
                    </el-table-column>
                    <el-table-column v-if="isAuditFileColumnVisible('actions')" label="操作" width="190" fixed="right">
                      <template #default="scope">
                        <el-button v-if="canInlinePreview(scope.row)" link type="primary" icon="View" @click="openInlineAuditPreview(scope.row)"
                          >预览</el-button
                        >
                        <el-button v-if="scope.row.storagePath" link type="primary" icon="Download" @click="openFile(scope.row)">原文件</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <el-empty v-else :image-size="54" description="作品文件信息已在显示设置中隐藏" />
                </template>

                <template v-else-if="auditDetailTab === 'records'">
                  <el-table v-if="detailDisplayConfig.audit.content.recordColumns.length" border :data="currentProject.auditRecords || []">
                    <el-table-column v-if="isAuditRecordColumnVisible('operator')" label="操作人" width="140">
                      <template #default="scope">{{ auditOperatorLabel(scope.row) }}</template>
                    </el-table-column>
                    <el-table-column v-if="isAuditRecordColumnVisible('result')" label="结果" width="110">
                      <template #default="scope">{{ auditResultLabel(scope.row.auditResult) }}</template>
                    </el-table-column>
                    <el-table-column v-if="isAuditRecordColumnVisible('transition')" label="状态流转" width="180">
                      <template #default="scope">{{ statusLabel(scope.row.fromStatus) }} -> {{ statusLabel(scope.row.toStatus) }}</template>
                    </el-table-column>
                    <el-table-column v-if="isAuditRecordColumnVisible('opinion')" label="意见" prop="opinion" min-width="240" show-overflow-tooltip />
                    <el-table-column v-if="isAuditRecordColumnVisible('time')" label="时间" prop="auditedAt" width="170" />
                  </el-table>
                  <el-empty v-else :image-size="54" description="审核记录信息已在显示设置中隐藏" />
                </template>
              </div>
            </section>

            <aside class="art-detail-workbench__side audit-detail-side">
              <section class="art-detail-workbench__side-section is-grow">
                <div class="art-detail-workbench__side-head">
                  <strong>{{ detailDisplayConfig.audit.title }}</strong>
                  <div class="art-detail-workbench__side-tools">
                    <el-tag size="small" :type="statusType(currentProject.status)">{{ statusLabel(currentProject.status) }}</el-tag>
                  </div>
                </div>
                <dl v-if="auditSummaryItems.length" class="art-detail-workbench__summary">
                  <div v-for="item in auditSummaryItems" :key="item.key" class="art-detail-workbench__summary-row">
                    <dt>{{ item.label }}</dt>
                    <dd>{{ item.value }}</dd>
                  </div>
                </dl>

                <ArtDetailFileList
                  v-if="currentProject.files?.length"
                  class="audit-side-files"
                  :files="currentProject.files"
                  :project="currentProject"
                  :title="detailDisplayConfig.tabs.audit.labels.files"
                  :counter-text="`${currentProject.files.length} 个`"
                  :selected-key="selectedAuditFileId"
                  :detail-fields="detailDisplayConfig.files.fields"
                  :detail-labels="detailDisplayConfig.files.labels"
                  @select="selectAuditSideFile"
                />
              </section>
              <section v-if="detailDisplayConfig.audit.actionsVisible" class="art-detail-workbench__side-section audit-detail-actions">
                <el-button
                  v-if="currentProject.status === 'school_submitted'"
                  v-hasPermi="['crehn:audit:pass']"
                  type="success"
                  icon="Check"
                  @click="openAuditAction(currentProject, 'pass')"
                >
                  通过
                </el-button>
                <el-button
                  v-if="currentProject.status === 'school_submitted'"
                  v-hasPermi="['crehn:audit:return']"
                  type="danger"
                  icon="Close"
                  @click="openAuditAction(currentProject, 'return')"
                >
                  退回
                </el-button>
                <el-button
                  v-if="currentProject.status === 'returned'"
                  v-hasPermi="['crehn:audit:withdrawReturn']"
                  type="warning"
                  icon="RefreshLeft"
                  @click="openAuditAction(currentProject, 'withdrawReturn')"
                >
                  撤销退回
                </el-button>
                <el-button
                  v-if="currentProject.status === 'audit_passed'"
                  v-hasPermi="['crehn:audit:withdrawPass']"
                  type="warning"
                  icon="RefreshLeft"
                  @click="openAuditAction(currentProject, 'withdrawPass')"
                >
                  撤销通过
                </el-button>
                <el-button @click="detailVisible = false">关闭</el-button>
              </section>
            </aside>
          </main>
        </div>
      </template>
    </el-drawer>

    <ArtPopupDialog v-model="auditDialog.visible" :title="auditDialogTitle" width="560px" append-to-body>
      <el-form label-width="96px">
        <el-form-item label="项目">
          <span>{{ auditDialog.project?.projectName || '-' }}</span>
        </el-form-item>
        <el-form-item label="审核意见" :required="auditDialog.action === 'return'">
          <el-input v-model="auditDialog.opinion" type="textarea" :rows="5" placeholder="请输入审核意见" />
        </el-form-item>
        <el-form-item v-if="auditDialog.action === 'return' && auditDialog.rejectTemplates.length" label="原因模板">
          <div class="reject-template-list">
            <el-button v-for="item in auditDialog.rejectTemplates" :key="item" size="small" plain @click="applyRejectTemplate(item)">
              {{ item }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="站内信通知">
          <el-checkbox v-model="auditDialog.notifyEnabled">通知单位账号</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button :type="auditDialogButtonType" @click="submitAuditAction">{{ auditDialogButtonText }}</el-button>
        <el-button @click="auditDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="previewDialog.visible" :title="previewDialog.title" width="80%" wide top="4vh" append-to-body destroy-on-close>
      <video v-if="previewDialog.type === 'video'" class="w-full max-h-[72vh]" :src="previewDialog.url" controls preload="metadata" />
      <div v-else-if="previewDialog.type === 'pdf'" class="pdf-preview-dialog">
        <div class="pdf-preview-toolbar">
          <span>PDF 预览</span>
          <div>
            <el-button size="small" icon="Printer" @click="printPdfPreview">打印 PDF</el-button>
            <el-button size="small" icon="Download" @click="downloadPdfPreview">下载 PDF</el-button>
          </div>
        </div>
        <iframe class="h-[72vh] w-full border-0" :src="pdfViewerUrl(previewDialog.url)" />
      </div>
      <div v-else-if="previewDialog.type === 'image'" class="max-h-[72vh] overflow-auto text-center">
        <img :src="previewDialog.url" class="max-w-full" />
      </div>
      <el-alert v-else type="info" :closable="false" title="该文件类型不支持浏览器内嵌预览，请下载后查看。" />
      <template #footer>
        <el-button v-if="previewDialog.url" type="primary" icon="Download" @click="windowOpen(previewDialog.url)">下载/新窗口打开</el-button>
        <el-button @click="previewDialog.visible = false">关闭</el-button>
      </template>
    </ArtPopupDialog>
  </div>
</template>

<script setup name="ArtAudit" lang="ts">
import { ArrowDown } from '@element-plus/icons-vue';
import {
  getAuditQuotaOverview,
  getAuditProject,
  listAuditActivityOptions,
  listAuditCategoryOptions,
  listAuditProject,
  listAuditSchoolOptions,
  passProject,
  returnProject,
  withdrawPassProject,
  withdrawReturnProject
} from '@/api/crehn/audit';
import {
  getProjectView,
  getProjectViewQuotaOverview,
  listProjectView,
  listProjectViewActivityOptions,
  listProjectViewCategoryOptions,
  listProjectViewSchoolOptions
} from '@/api/crehn/projectView';
import { ActivityCategoryVO, ActivityVO, ProjectFileVO, ProjectQuotaOverviewVO, ProjectVO } from '@/api/crehn/types';
import type {
  ArtAuditBasicSectionKey,
  ArtAuditDetailTab,
  ArtAuditFileColumnKey,
  ArtAuditRecordColumnKey,
  ArtListStatusSemantic,
  ArtReviewListColumnConfig
} from '@/api/crehn/detailDisplay';
import { checkPermi } from '@/utils/permission';
import { buildProjectFormRows, buildProjectMemberFields, exportProjectDetailWorkbook } from '@/utils/artProjectDetail';
import type { ProjectDetailRow } from '@/utils/artProjectDetail';
import { normalizePreviewMessage as normalizeArtPreviewMessage, normalizeReviewMessage } from '@/utils/artReviewMessage';
import ArtDetailFileList from '../components/ArtDetailFileList.vue';
import ArtProjectMemberReadonly from '../components/ArtProjectMemberReadonly.vue';
import {
  detailCheckFailureMessage,
  detailCheckStatusLabel,
  detailFileTypeLabel,
  detailTechnicalRequirements
} from '../components/artDetailFileDisplay';
import { loadArtDetailDisplayConfig, useArtDetailDisplayConfig, workspaceHeaderItemText } from '../components/artDetailDisplayConfig';
import ArtCategoryTreeDropdown from '../components/ArtCategoryTreeDropdown.vue';
import ArtListStatusTag from '../components/ArtListStatusTag.vue';
import ArtProjectWorkspaceHeader from '../components/ArtProjectWorkspaceHeader.vue';
import ArtWorkspaceConfigButton from '../components/ArtWorkspaceConfigButton.vue';
import ArtStatusTabs from '../components/ArtStatusTabs.vue';
import ProjectBrowseNavigator from '../components/ProjectBrowseNavigator.vue';
import ProjectGenericTableReadonly from '../project/components/ProjectGenericTableReadonly.vue';
import { useArtTableColumnWidths } from '@/composables/useArtTableColumnWidths';
import { useArtListTableAppearance } from '@/composables/useArtListTableAppearance';
import { artTableColumnValue, artTableDisplayText, useArtListTablePage } from '@/composables/useArtListTableConfig';
import { useManagedArtActivity } from '@/composables/useManagedArtActivity';
import AuditAssignmentPage from '@/views/crehn/audit-assignment/index.vue';
import { useRoute, useRouter } from 'vue-router';

type AuditAction = 'pass' | 'return' | 'withdrawReturn' | 'withdrawPass';
interface CategoryNode extends ActivityCategoryVO {
  children?: CategoryNode[];
}

interface AuditGroupOption {
  label: string;
  value: string;
  count?: number;
}

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const route = useRoute();
const router = useRouter();

const loading = ref(false);
const total = ref(0);
const projectList = ref<ProjectVO[]>([]);
const currentProject = ref<ProjectVO>();
const currentProjectAnchorIndex = ref(-1);
const adjacentNavigationHintActive = ref(false);
const detailVisible = ref(false);
const auditDetailTab = ref<ArtAuditDetailTab>('preview');
const selectedAuditFileId = ref<string | number>();
const auditAssignmentVisible = ref(false);
const activityOptions = ref<ActivityVO[]>([]);
const { managedActivityError, resolveManagedActivityId } = useManagedArtActivity(activityOptions);
const { tableAppearanceClass, tableAppearanceStyle, actionIcon } = useArtListTableAppearance({
  statusLabels: () => ['draft', 'pending', 'approved', 'returned'].map((key) => auditTablePage.value.statusLabels[key as ArtListStatusSemantic]),
  actionLabels: () => ['view', 'return', 'withdraw'].map((key) => auditTablePage.value.actionLabels[key])
});
const categoryOptions = ref<ActivityCategoryVO[]>([]);
const schoolOptions = ref<any[]>([]);
const browseMode = ref<'category' | 'school'>('category');
const auditSidebarOpen = ref(false);
const selectedBrowseCategoryId = ref<string | number>();
const schoolKeyword = ref('');
const quotaOverview = ref<ProjectQuotaOverviewVO>({});
const navigatorQuotaOverview = ref<ProjectQuotaOverviewVO>({});
const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  activityId: undefined,
  categoryId: undefined,
  categoryIds: undefined,
  groupCode: undefined,
  schoolId: undefined,
  projectName: '',
  status: 'all'
});
const {
  pageConfig: auditTablePage,
  columns: auditDataColumns,
  serialColumn: auditSerialColumn,
  statusText: auditStatusText,
  actionText: auditActionText
} = useArtListTablePage('audit', () => queryParams.categoryId);
const auditTableColumns = computed(() => [...(auditSerialColumn.value ? [auditSerialColumn.value] : []), ...auditDataColumns.value]);
const auditSerialNumber = (index: number) => (queryParams.pageNum - 1) * queryParams.pageSize + index + 1;
const auditWidthStorageKey = computed(() => `crehn:audit-table-widths:v2:${queryParams.activityId || 'all'}:${queryParams.categoryId || 'all'}`);
const {
  tableShellRef: auditTableShellRef,
  columnWidth: auditColumnWidth,
  handleColumnResize: handleAuditColumnResize,
  restoreColumnWidths: restoreAuditColumnWidths
} = useArtTableColumnWidths({ columns: auditTableColumns, storageKey: auditWidthStorageKey, fitContainer: true });
const auditColumnText = (row: ProjectVO, column: ArtReviewListColumnConfig) =>
  artTableDisplayText(artTableColumnValue(row as unknown as Record<string, unknown>, column));
const auditDialog = reactive<{
  visible: boolean;
  action: AuditAction;
  project?: ProjectVO;
  opinion: string;
  notifyEnabled: boolean;
  rejectTemplates: string[];
}>({
  visible: false,
  action: 'pass',
  opinion: '',
  notifyEnabled: false,
  rejectTemplates: []
});
const previewDialog = reactive<{ visible: boolean; title: string; url: string; type: string }>({ visible: false, title: '', url: '', type: '' });
const collapsedFormRows = reactive<Record<string, boolean>>({});
const { detailDisplayConfig } = useArtDetailDisplayConfig();
const auditHeaderConfig = computed(() => detailDisplayConfig.value.workspaceHeader.pages.audit);
const auditHeaderText = (
  itemKey: 'categoryFilter' | 'groupFilter' | 'schoolFilter' | 'status' | 'search' | 'actions',
  textKey: string,
  fallback: string
) => workspaceHeaderItemText(auditHeaderConfig.value, itemKey, textKey, fallback);
const auditProgressConfig = computed(() => detailDisplayConfig.value.auditProgress);

const formRows = computed(() => buildProjectFormRows(currentProject.value));
const memberFields = computed(() => buildProjectMemberFields(currentProject.value));
const memberCount = computed(() => currentProject.value?.members?.length || 0);
const availableAuditTabs = computed(() =>
  detailDisplayConfig.value.tabs.audit.order.filter((tab) => detailDisplayConfig.value.tabs.audit.visibleTabs.includes(tab))
);
const resolveAvailableAuditTab = (tab: ArtAuditDetailTab) =>
  availableAuditTabs.value.includes(tab) ? tab : availableAuditTabs.value[0] || 'preview';
const auditTabLabel = (tab: ArtAuditDetailTab) => {
  const label = detailDisplayConfig.value.tabs.audit.labels[tab];
  if (tab === 'members') return `${label}（${memberCount.value}）`;
  if (tab === 'files') return `${label}（${currentProject.value?.files?.length || 0}）`;
  return label;
};
const isAuditBasicSectionVisible = (key: ArtAuditBasicSectionKey) => detailDisplayConfig.value.audit.content.basicSections.includes(key);
const isAuditFileColumnVisible = (key: ArtAuditFileColumnKey) => detailDisplayConfig.value.audit.content.fileColumns.includes(key);
const isAuditRecordColumnVisible = (key: ArtAuditRecordColumnKey) => detailDisplayConfig.value.audit.content.recordColumns.includes(key);
const auditBasicContentVisible = computed(() => isAuditBasicSectionVisible('formFields') || isAuditBasicSectionVisible('genericTables'));
watch(availableAuditTabs, () => {
  auditDetailTab.value = resolveAvailableAuditTab(auditDetailTab.value);
});
const currentProjectListIndex = computed(() => {
  if (!currentProject.value?.id) return -1;
  return projectList.value.findIndex((item) => String(item.id) === String(currentProject.value?.id));
});
const hasPreviousProject = computed(() => {
  if (currentProjectListIndex.value >= 0) return currentProjectListIndex.value > 0;
  return currentProjectAnchorIndex.value > 0 && projectList.value.length > 0;
});
const hasNextProject = computed(() => {
  if (currentProjectListIndex.value >= 0) return currentProjectListIndex.value < projectList.value.length - 1;
  return currentProjectAnchorIndex.value >= 0 && currentProjectAnchorIndex.value < projectList.value.length;
});
const currentProjectPositionText = computed(() => {
  if (currentProjectListIndex.value >= 0) {
    return `当前页第 ${currentProjectListIndex.value + 1}/${projectList.value.length} 条`;
  }
  if (currentProjectAnchorIndex.value >= 0) {
    return `当前项目已离开筛选结果，本页剩余 ${projectList.value.length} 条`;
  }
  return '';
});
const canAuditList = computed(() => checkPermi(['crehn:audit:list']));
const canManageAuditScope = computed(() => checkPermi(['crehn:auditScope:list']));
const canProjectViewList = computed(() => checkPermi(['crehn:projectView:list']));
const canDetailQuery = computed(() => checkPermi(['crehn:audit:query']) || checkPermi(['crehn:projectView:query']));
const canReturnProject = computed(() => checkPermi(['crehn:audit:return']));
const canWithdrawReturnedProject = computed(() => checkPermi(['crehn:audit:withdrawReturn']));
const canWithdrawPassedProject = computed(() => checkPermi(['crehn:audit:withdrawPass']));
const canReturnAuditProject = (row: ProjectVO) => row.status === 'school_submitted' && !useProjectViewData.value && canReturnProject.value;
const withdrawActionForStatus = (row: ProjectVO): 'withdrawReturn' | 'withdrawPass' | undefined => {
  if (row.status === 'returned') return 'withdrawReturn';
  if (row.status === 'audit_passed') return 'withdrawPass';
  return undefined;
};
const auditWithdrawAction = (row: ProjectVO): 'withdrawReturn' | 'withdrawPass' | undefined => {
  if (useProjectViewData.value) return undefined;
  const action = withdrawActionForStatus(row);
  if (action === 'withdrawReturn' && canWithdrawReturnedProject.value) return action;
  if (action === 'withdrawPass' && canWithdrawPassedProject.value) return action;
  return undefined;
};
const auditReturnButtonTitle = (row: ProjectVO) => {
  if (row.status !== 'school_submitted') return '仅待审核项目可以退回';
  if (useProjectViewData.value) return '当前账号仅可查看项目';
  return canReturnProject.value ? '退回项目' : '无项目退回权限';
};
const auditWithdrawButtonTitle = (row: ProjectVO) => {
  const action = withdrawActionForStatus(row);
  if (!action) return '当前状态没有可撤销的审核操作';
  if (useProjectViewData.value) return '当前账号仅可查看项目';
  if (action === 'withdrawReturn') return canWithdrawReturnedProject.value ? '撤销退回' : '无撤销退回权限';
  return canWithdrawPassedProject.value ? '撤销通过' : '无撤销通过权限';
};
const useProjectViewData = computed(() => !canAuditList.value && canProjectViewList.value);
const statusOptions = computed(() => {
  if (useProjectViewData.value) {
    const options: Array<{ label: string; value: string }> = [];
    if (checkPermi(['crehn:projectView:draft'])) options.push({ label: '草稿', value: 'draft' });
    if (checkPermi(['crehn:projectView:submitted'])) options.push({ label: '待审核', value: 'school_submitted' });
    if (checkPermi(['crehn:projectView:audited'])) options.push({ label: '通过', value: 'audit_passed' }, { label: '退回', value: 'returned' });
    return options.length ? [{ label: '全部', value: '' }, ...options] : options;
  }
  return [
    { label: '全部', value: 'all' },
    { label: '待审核', value: 'school_submitted' },
    { label: '通过', value: 'audit_passed' },
    { label: '退回', value: 'returned' }
  ];
});
const selectedActivityName = computed(
  () => activityOptions.value.find((item) => String(item.id) === String(queryParams.activityId || ''))?.activityName || ''
);
const categoryTree = computed<CategoryNode[]>(() => buildCategoryTree(categoryOptions.value));
const selectedAuditMajorCategory = computed(() => {
  const selectedKey = String(selectedBrowseCategoryId.value || '');
  return categoryTree.value.find((item) => String(item.id) === selectedKey || categoryContains(item, selectedKey));
});
const auditMajorCategoryIds = computed(() => (selectedAuditMajorCategory.value ? collectLeafCategoryIds(selectedAuditMajorCategory.value) : []));
const auditGroupOptions = computed<AuditGroupOption[]>(() => {
  const allowedCategoryIds = new Set(auditMajorCategoryIds.value.map(String));
  const groups = new Map<string, AuditGroupOption>();
  (navigatorQuotaOverview.value.categoryItems || [])
    .filter((item) => !allowedCategoryIds.size || allowedCategoryIds.has(String(item.categoryId)))
    .flatMap((item) => item.ratioItems || [])
    .forEach((item) => {
      const value = String(item.groupCode || item.key || '').trim();
      if (!value) return;
      const current = groups.get(value);
      groups.set(value, {
        value,
        label: String(item.groupName || item.key || value),
        count: Number(current?.count || 0) + Number(item.count || 0)
      });
    });
  return [...groups.values()].sort((left, right) => left.label.localeCompare(right.label, 'zh-CN'));
});

const openAuditAssignment = () => {
  auditAssignmentVisible.value = true;
};
const enabledLeafCategoryIds = computed(() => categoryTree.value.flatMap(collectLeafCategoryIds).filter((id) => !String(id).startsWith('group:')));
const schoolProjectCount = (item: any) => {
  const value = item?.visibleProjectCount ?? item?.projectCount;
  if (value === undefined || value === null || value === '') return undefined;
  const count = Number(value);
  return Number.isFinite(count) ? count : undefined;
};
const visibleSchoolOptions = computed(() => {
  const keyword = schoolKeyword.value.trim().toLowerCase();
  return schoolOptions.value
    .filter((item) => useProjectViewData.value || Number(schoolProjectCount(item) || 0) > 0)
    .filter((item) => {
      if (!keyword) return true;
      return [item.schoolName, item.schoolCode].filter(Boolean).some((value) => String(value).toLowerCase().includes(keyword));
    });
});
const auditCategoryCountMap = computed(() =>
  (navigatorQuotaOverview.value.categoryItems || []).reduce<Record<string, number>>((result, item) => {
    if (item.categoryId !== undefined && item.categoryId !== null) result[String(item.categoryId)] = Number(item.totalCount || 0);
    return result;
  }, {})
);
const auditUnitOptions = computed(() =>
  visibleSchoolOptions.value.map((item) => ({
    id: item.id,
    label: String(item.schoolName || item.id),
    count: schoolProjectCount(item) || 0,
    keywords: [item.schoolCode].filter(Boolean) as string[]
  }))
);
const statusCount = (status?: string) => {
  const totalCount = quotaNumber(quotaOverview.value.totalCount);
  const draftCount = quotaNumber(quotaOverview.value.draftCount);
  const submittedCount = quotaNumber(quotaOverview.value.submittedCount);
  const passedCount = quotaNumber(quotaOverview.value.completedCount);
  const normalized = String(status || '');
  if (!normalized || normalized === 'all') return totalCount;
  if (normalized === 'draft') return draftCount;
  if (normalized === 'school_submitted') return submittedCount;
  if (normalized === 'audit_passed') return passedCount;
  if (normalized === 'returned') return Math.max(0, totalCount - draftCount - submittedCount - passedCount);
  return 0;
};
const auditStatusTabItems = computed(() =>
  statusOptions.value.map((item) => ({
    ...item,
    label: auditHeaderText('status', item.value || 'all', item.label),
    count: statusCount(item.value)
  }))
);

const manualCheckTips = computed(() => {
  const parsed = parseFormData(currentProject.value?.validationResultJson);
  const tips = Array.isArray(parsed.manualCheckTips) ? parsed.manualCheckTips.map((item: string) => normalizeReviewMessage(item)) : [];
  const warnings = Array.isArray(parsed.warnings) ? parsed.warnings.map((item: string) => `技术提示：${normalizeReviewMessage(item)}`) : [];
  return [...tips, ...warnings].filter(Boolean);
});

const isAbstractFormRow = (row: ProjectDetailRow) => /摘要|abstract|summary/i.test(`${row.key} ${row.label}`);
const isLongFormRow = (row: ProjectDetailRow) => isAbstractFormRow(row) && String(row.value || '').length > 120;
const isFormRowCollapsed = (row: ProjectDetailRow) => !!collapsedFormRows[row.key];
const toggleFormRow = (row: ProjectDetailRow) => {
  collapsedFormRows[row.key] = !isFormRowCollapsed(row);
};
const resetFormRowState = () => {
  Object.keys(collapsedFormRows).forEach((key) => delete collapsedFormRows[key]);
};
const manualReviewSubject = (file?: ProjectFileVO) => (file?.originalName ? `文件：${file.originalName}` : '技术校验要求');
const manualReviewDetail = (file?: ProjectFileVO) => {
  if (!file) return manualCheckTips.value.join('\n\n') || '请结合活动要求核验该材料。';
  const failureMessage = detailCheckFailureMessage(currentProject.value, file);
  const technicalRequirements = detailTechnicalRequirements(currentProject.value, file);
  return failureMessage || technicalRequirements || manualCheckTips.value.join('\n\n') || '请结合活动要求逐项核验该材料。';
};

const auditDialogTitle = computed(() => {
  const map: Record<AuditAction, string> = { pass: '审核通过', return: '退回项目', withdrawReturn: '撤销退回', withdrawPass: '撤销通过' };
  return map[auditDialog.action];
});
const auditDialogButtonText = computed(() => auditDialogTitle.value);
const auditDialogButtonType = computed(() => (auditDialog.action === 'return' ? 'danger' : auditDialog.action === 'pass' ? 'success' : 'warning'));

const loadActivityOptions = async () => {
  if (canProjectViewList.value) {
    const { data } = await listProjectViewActivityOptions();
    activityOptions.value = data || [];
  } else {
    const { data } = await listAuditActivityOptions();
    activityOptions.value = data || [];
  }
  const managedId = resolveManagedActivityId();
  queryParams.activityId = managedId === undefined ? undefined : String(managedId);
};

const loadSchoolOptions = async () => {
  const params = {
    activityId: queryParams.activityId,
    categoryIds: queryParams.categoryIds,
    groupCode: queryParams.groupCode,
    status: queryParams.status,
    projectName: queryParams.projectName
  };
  if (browseMode.value === 'school') {
    params.categoryIds = undefined;
    params.groupCode = undefined;
  }
  const res: any = useProjectViewData.value ? await listProjectViewSchoolOptions({ status: 'enabled' }) : await listAuditSchoolOptions(params);
  schoolOptions.value = res.rows || res.data || [];
};

const loadCategoryOptions = async (resetSelection = true) => {
  categoryOptions.value = [];
  if (resetSelection) {
    selectedBrowseCategoryId.value = undefined;
    queryParams.categoryId = undefined;
    queryParams.categoryIds = undefined;
  }
  if (!queryParams.activityId) return;
  if (canProjectViewList.value) {
    const { data } = await listProjectViewCategoryOptions(queryParams.activityId);
    categoryOptions.value = enabledCategoryRows(data || []);
  } else {
    const { data } = await listAuditCategoryOptions(queryParams.activityId);
    categoryOptions.value = enabledCategoryRows(data || []);
  }
};

const handleNavigatorBrowseModeChange = async (mode: 'category' | 'school') => {
  browseMode.value = mode;
  await handleBrowseModeChange();
};
const selectNavigatorCategory = async (selection: { key: string; categoryIds: Array<string | number> }) => {
  selectedBrowseCategoryId.value = selection.key || undefined;
  queryParams.categoryId = undefined;
  queryParams.categoryIds = selection.categoryIds.join(',') || undefined;
  queryParams.groupCode = undefined;
  if (browseMode.value === 'category') queryParams.schoolId = undefined;
  queryParams.pageNum = 1;
  await refreshBrowseData();
};
const handleAuditGroupChange = async (groupCode?: string) => {
  queryParams.groupCode = String(groupCode || '').trim() || undefined;
  queryParams.pageNum = 1;
  await refreshBrowseData();
};
const selectNavigatorUnit = async (schoolId?: string | number) => {
  selectedBrowseCategoryId.value = undefined;
  queryParams.categoryId = undefined;
  queryParams.categoryIds = undefined;
  queryParams.groupCode = undefined;
  queryParams.pageNum = 1;
  if (schoolId === undefined || schoolId === null) {
    queryParams.schoolId = undefined;
    await refreshBrowseData();
    return;
  }
  const school = schoolOptions.value.find((item) => String(item.id) === String(schoolId));
  if (school) await selectSchool(school);
};
const handleAuditSchoolChange = async () => {
  if (browseMode.value === 'school') {
    selectedBrowseCategoryId.value = undefined;
    queryParams.categoryId = undefined;
    queryParams.categoryIds = undefined;
    queryParams.groupCode = undefined;
  }
  queryParams.pageNum = 1;
  await refreshBrowseData();
};

const handleStatusChange = async (status?: string) => {
  if (String(queryParams.status || '') === String(status || '')) return;
  queryParams.status = status;
  queryParams.pageNum = 1;
  await refreshBrowseData();
};

const loadQuotaOverview = async () => {
  if (!queryParams.activityId) {
    quotaOverview.value = {};
    return;
  }
  const params = enabledCategoryScopedQuery();
  params.status = useProjectViewData.value ? undefined : 'all';
  const { data } = useProjectViewData.value ? await getProjectViewQuotaOverview(params) : await getAuditQuotaOverview(params);
  quotaOverview.value = data || {};
};

const loadNavigatorQuotaOverview = async () => {
  if (!queryParams.activityId) {
    navigatorQuotaOverview.value = {};
    return;
  }
  const params = enabledCategoryScopedQuery();
  delete params.groupCode;
  params.categoryIds = enabledLeafCategoryIds.value.length ? enabledLeafCategoryIds.value.join(',') : '-1';
  params.status = useProjectViewData.value ? undefined : 'all';
  const { data } = useProjectViewData.value ? await getProjectViewQuotaOverview(params) : await getAuditQuotaOverview(params);
  navigatorQuotaOverview.value = data || {};
  if (queryParams.groupCode && !auditGroupOptions.value.some((item) => item.value === String(queryParams.groupCode))) {
    queryParams.groupCode = undefined;
    syncRouteQuery();
  }
};

const refreshBrowseData = async () => {
  ensureAllowedStatus(false);
  syncRouteQuery();
  await loadNavigatorQuotaOverview();
  await loadSchoolOptions();
  await loadQuotaOverview();
  await getList();
};

const handleBrowseModeChange = async () => {
  selectedBrowseCategoryId.value = undefined;
  queryParams.categoryId = undefined;
  queryParams.categoryIds = undefined;
  queryParams.groupCode = undefined;
  queryParams.schoolId = undefined;
  queryParams.pageNum = 1;
  await refreshBrowseData();
};

const selectSchool = async (school: any) => {
  queryParams.schoolId = school.id;
  queryParams.pageNum = 1;
  await refreshBrowseData();
};

const getList = async () => {
  ensureAllowedStatus(false);
  loading.value = true;
  try {
    const params = enabledCategoryScopedQuery();
    const res: any = useProjectViewData.value ? await listProjectView(params) : await listAuditProject(params);
    projectList.value = res.rows || [];
    total.value = res.total || 0;
  } finally {
    loading.value = false;
  }
};

const resetQuery = async () => {
  const managedId = resolveManagedActivityId();
  queryParams.activityId = managedId === undefined ? undefined : String(managedId);
  queryParams.categoryId = undefined;
  queryParams.categoryIds = undefined;
  queryParams.groupCode = undefined;
  queryParams.schoolId = undefined;
  selectedBrowseCategoryId.value = undefined;
  queryParams.projectName = '';
  queryParams.status = defaultStatus();
  queryParams.pageNum = 1;
  if (queryParams.activityId) {
    await loadCategoryOptions();
  } else {
    categoryOptions.value = [];
  }
  await refreshBrowseData();
};

const menuGroups = [
  { key: 'performance', label: '艺术表演类' },
  { key: 'artwork', label: '艺术作品类' },
  { key: 'workshop', label: '艺术实践工作坊' },
  { key: 'achievement', label: '高校美育改革创新优秀成果' },
  { key: 'principal', label: '高校校长书画作品' }
];

const quotaNumber = (value: unknown) => {
  const numeric = Number(value || 0);
  return Number.isFinite(numeric) ? numeric : 0;
};
const auditProgress = computed(() => {
  const all = quotaNumber(quotaOverview.value.totalCount);
  const draft = quotaNumber(quotaOverview.value.draftCount);
  const pending = quotaNumber(quotaOverview.value.submittedCount);
  const total = Math.max(all - draft, 0);
  const completed = Math.max(total - pending, 0);
  return {
    total,
    completed,
    pending,
    percentage: total > 0 ? Math.round((completed / total) * 100) : 0
  };
});
const auditProgressText = computed(() =>
  auditProgressConfig.value.template.replace(/\{(completed|total|pending|percentage)\}/g, (_match, key: string) =>
    String(auditProgress.value[key as keyof typeof auditProgress.value])
  )
);
const auditProgressColor = computed(() =>
  auditProgress.value.percentage >= 100 ? auditProgressConfig.value.completeColor : auditProgressConfig.value.activeColor
);
const auditProgressStyle = computed(() => ({
  '--audit-progress-width': `${auditProgressConfig.value.width}px`,
  '--audit-progress-track-color': auditProgressConfig.value.trackColor,
  '--audit-progress-text-color': auditProgressConfig.value.textColor,
  '--audit-progress-font-size': `${auditProgressConfig.value.fontSize}px`
}));
const hasQuotaNumber = (value: unknown) => value !== undefined && value !== null;
const sortCategoryNodes = (nodes: CategoryNode[]) =>
  nodes.sort(
    (a, b) =>
      Number(a.sortOrder || 0) - Number(b.sortOrder || 0) || String(a.categoryName || '').localeCompare(String(b.categoryName || ''), 'zh-Hans-CN')
  );

const isRootCategoryParent = (parentId?: string | number) =>
  parentId === undefined || parentId === null || parentId === '' || String(parentId) === '0';

const enabledCategoryRows = (rows: ActivityCategoryVO[]) => {
  const rowMap = new Map<string, ActivityCategoryVO>();
  rows.forEach((row) => {
    if (row.id !== undefined && row.id !== null) {
      rowMap.set(String(row.id), row);
    }
  });
  const isVisible = (row: ActivityCategoryVO, seen = new Set<string>()): boolean => {
    if (row.enabled === false) return false;
    if (isRootCategoryParent(row.parentId)) return true;
    const parentKey = String(row.parentId);
    if (seen.has(parentKey)) return true;
    const parent = rowMap.get(parentKey);
    if (!parent) return true;
    seen.add(parentKey);
    return isVisible(parent, seen);
  };
  return rows.filter((row) => isVisible(row));
};

const enabledCategoryScopedQuery = () => {
  const params: Record<string, any> = { ...queryParams };
  if (!queryParams.activityId) {
    return params;
  }
  const allowedIds = enabledLeafCategoryIds.value.map((id) => String(id));
  if (!allowedIds.length) {
    params.categoryIds = '-1';
    return params;
  }
  if (params.categoryIds) {
    const allowedSet = new Set(allowedIds);
    const selectedIds = String(params.categoryIds)
      .split(',')
      .map((id) => id.trim())
      .filter((id) => id && allowedSet.has(id));
    params.categoryIds = selectedIds.length ? selectedIds.join(',') : '-1';
    return params;
  }
  params.categoryIds = allowedIds.join(',');
  return params;
};

const buildCategoryTree = (rows: ActivityCategoryVO[]) => {
  const backendTree = buildCategoryTreeByParent(rows);
  if (backendTree.length) {
    return backendTree;
  }
  return buildCategoryTreeByGroup(rows);
};

const buildCategoryTreeByParent = (rows: ActivityCategoryVO[]) => {
  const nodeMap = new Map<string, CategoryNode>();
  rows.forEach((item) => {
    if (item.id === undefined || item.id === null) return;
    nodeMap.set(String(item.id), { ...item, children: [] });
  });
  const roots: CategoryNode[] = [];
  let hasNestedCategory = false;
  nodeMap.forEach((node) => {
    const parent = isRootCategoryParent(node.parentId) ? undefined : nodeMap.get(String(node.parentId));
    if (parent && String(parent.id) !== String(node.id)) {
      parent.children = parent.children || [];
      parent.children.push(node);
      hasNestedCategory = true;
    } else {
      roots.push(node);
    }
  });
  if (!hasNestedCategory) {
    return [];
  }
  return finalizeCategoryTree(roots);
};

const finalizeCategoryTree = (nodes: CategoryNode[]): CategoryNode[] => {
  const sorted = sortCategoryNodes(nodes);
  sorted.forEach((node) => {
    node.children = finalizeCategoryTree(node.children || []);
  });
  return sorted;
};

const buildCategoryTreeByGroup = (rows: ActivityCategoryVO[]) => {
  const grouped = new Map<string, CategoryNode[]>();
  rows.forEach((item) => {
    const key = categoryGroupKey(item);
    const items = grouped.get(key) || [];
    items.push({ ...item, children: [] });
    grouped.set(key, items);
  });
  const roots = menuGroups
    .map((group) => {
      const children = grouped.get(group.key) || [];
      return {
        id: `group:${group.key}`,
        categoryName: group.label,
        children
      };
    })
    .filter((group) => group.children.length);
  const other = grouped.get('other') || [];
  if (other.length) {
    roots.push({ id: 'group:other', categoryName: '其他类别', children: other });
  }
  return roots;
};

const categoryGroupKey = (item: ActivityCategoryVO) => {
  const code = String(item.categoryCode || '').toLowerCase();
  const group = String(item.categoryGroup || '').toLowerCase();
  const groupName = String(item.categoryGroup || item.categoryName || '');
  if (code === 'artwork_principal' || group === 'principal' || groupName.includes('校长书画')) return 'principal';
  if (code.startsWith('performance_') || group === 'performance' || groupName.includes('艺术表演')) return 'performance';
  if (code.startsWith('artwork_') || group === 'artwork' || groupName.includes('艺术作品')) return 'artwork';
  if (code === 'workshop' || code.startsWith('workshop_') || group === 'workshop' || groupName.includes('工作坊')) return 'workshop';
  if (code.startsWith('achievement_') || group === 'achievement' || groupName.includes('美育') || groupName.includes('成果')) return 'achievement';
  return 'other';
};

const collectLeafCategoryIds = (node: CategoryNode): Array<string | number> => {
  if (!node.children?.length) return node.id ? [node.id] : [];
  return node.children.flatMap(collectLeafCategoryIds);
};

function categoryContains(node: CategoryNode, categoryId: string): boolean {
  if (!categoryId) return false;
  return Boolean(node.children?.some((child) => String(child.id) === categoryId || categoryContains(child, categoryId)));
}

const findCategoryNode = (nodes: CategoryNode[], categoryId: string | number): CategoryNode | undefined => {
  for (const node of nodes) {
    if (String(node.id) === String(categoryId)) return node;
    const matched = findCategoryNode(node.children || [], categoryId);
    if (matched) return matched;
  }
  return undefined;
};

const routeQueryText = (value: unknown) => (Array.isArray(value) ? String(value[0] || '') : String(value || ''));
const routeProjectId = () => routeQueryText(route.query.projectId);

const applyRouteQuery = () => {
  const mode = routeQueryText(route.query.browse);
  browseMode.value = mode === 'school' ? 'school' : 'category';
  queryParams.status = routeQueryText(route.query.status) || queryParams.status;
  queryParams.projectName = routeQueryText(route.query.projectName);
  queryParams.schoolId = routeQueryText(route.query.schoolId) || undefined;
  queryParams.groupCode = routeQueryText(route.query.groupCode) || undefined;
  selectedBrowseCategoryId.value = routeQueryText(route.query.categoryNodeId) || undefined;
};

const applySelectedCategoryFromRoute = () => {
  if (!selectedBrowseCategoryId.value) return;
  const node = findCategoryNode(categoryTree.value, selectedBrowseCategoryId.value);
  if (!node) {
    selectedBrowseCategoryId.value = undefined;
    queryParams.categoryIds = undefined;
    return;
  }
  const categoryIds = collectLeafCategoryIds(node);
  queryParams.categoryId = undefined;
  queryParams.categoryIds = categoryIds.length ? categoryIds.join(',') : undefined;
};

const syncRouteQuery = () => {
  const query: Record<string, any> = {
    ...route.query,
    browse: browseMode.value,
    status: queryParams.status || undefined,
    projectName: queryParams.projectName || undefined,
    schoolId: queryParams.schoolId || undefined,
    groupCode: queryParams.groupCode || undefined,
    categoryNodeId: selectedBrowseCategoryId.value || undefined
  };
  delete query.activityId;
  Object.keys(query).forEach((key) => query[key] === undefined && delete query[key]);
  router.replace({ path: route.path, query }).catch(() => {});
};

const routeMatchesState = () => {
  const mode = routeQueryText(route.query.browse) || 'category';
  return (
    mode === browseMode.value &&
    routeQueryText(route.query.status) === String(queryParams.status || '') &&
    routeQueryText(route.query.projectName) === String(queryParams.projectName || '') &&
    routeQueryText(route.query.schoolId) === String(queryParams.schoolId || '') &&
    routeQueryText(route.query.groupCode) === String(queryParams.groupCode || '') &&
    routeQueryText(route.query.categoryNodeId) === String(selectedBrowseCategoryId.value || '') &&
    routeProjectId() === String(currentProject.value?.id || '')
  );
};

const defaultStatus = () => (useProjectViewData.value ? '' : 'all');

const ensureAllowedStatus = (defaultWhenBlank = true) => {
  if (!statusOptions.value.length) {
    queryParams.status = undefined;
    return;
  }
  if (!queryParams.status) {
    if (defaultWhenBlank) {
      queryParams.status = defaultStatus();
    }
    return;
  }
  if (!statusOptions.value.some((item) => item.value === queryParams.status)) {
    queryParams.status = defaultStatus();
  }
};

const openDetail = async (row: ProjectVO) => {
  if (!canDetailQuery.value) return;
  const rowIndex = projectList.value.findIndex((item) => String(item.id) === String(row.id));
  if (rowIndex >= 0) {
    currentProjectAnchorIndex.value = rowIndex;
  } else if (!detailVisible.value) {
    currentProjectAnchorIndex.value = -1;
  }
  const detailRequest = useProjectViewData.value ? getProjectView(row.id!) : getAuditProject(row.id!);
  const [{ data }] = await Promise.all([detailRequest, loadArtDetailDisplayConfig()]);
  resetFormRowState();
  currentProject.value = data;
  auditDetailTab.value = resolveAvailableAuditTab(detailDisplayConfig.value.tabs.audit.defaultTab);
  const firstPreviewFile = data?.files?.find((file) => canInlinePreview(file)) || data?.files?.[0];
  selectedAuditFileId.value = firstPreviewFile ? auditFileKey(firstPreviewFile) : undefined;
  detailVisible.value = true;
};

const openRouteProjectDetail = async () => {
  const projectId = routeProjectId();
  if (!projectId || String(currentProject.value?.id || '') === projectId) {
    return;
  }
  await openDetail({ id: projectId } as ProjectVO);
};

const openAdjacentProject = async (offset: -1 | 1) => {
  const targetIndex =
    currentProjectListIndex.value >= 0 ? currentProjectListIndex.value + offset : currentProjectAnchorIndex.value + (offset === -1 ? -1 : 0);
  const target = projectList.value[targetIndex];
  if (!target) return;
  await openDetail(target);
};

let adjacentNavigationHintTimer: ReturnType<typeof setTimeout> | undefined;
const showAdjacentNavigationHint = async () => {
  if (!hasPreviousProject.value && !hasNextProject.value) return;
  if (adjacentNavigationHintTimer) clearTimeout(adjacentNavigationHintTimer);
  adjacentNavigationHintActive.value = false;
  await nextTick();
  adjacentNavigationHintActive.value = true;
  adjacentNavigationHintTimer = setTimeout(() => {
    adjacentNavigationHintActive.value = false;
  }, 1400);
};

const openAuditAction = async (row: ProjectVO, action: AuditAction) => {
  let target = row;
  if (action === 'return' && row.id) {
    try {
      const { data } = await getAuditProject(row.id);
      target = data || row;
    } catch {
      target = row;
    }
  }
  auditDialog.project = target;
  auditDialog.action = action;
  auditDialog.notifyEnabled = false;
  auditDialog.opinion = defaultOpinion(action);
  auditDialog.rejectTemplates = action === 'return' ? rejectReasonTemplatesFrom(target) : [];
  auditDialog.visible = true;
};
const openAuditWithdrawAction = async (row: ProjectVO) => {
  const action = auditWithdrawAction(row);
  if (action) await openAuditAction(row, action);
};

const submitAuditAction = async () => {
  if (!auditDialog.project?.id) return;
  const completedAction = auditDialog.action;
  const shouldHintAdjacentNavigation =
    detailVisible.value &&
    String(currentProject.value?.id || '') === String(auditDialog.project.id) &&
    (completedAction === 'pass' || completedAction === 'return');
  const opinion = auditDialog.opinion.trim();
  if (completedAction === 'return' && !opinion) {
    proxy?.$modal.msgWarning('请填写退回原因');
    return;
  }
  if (completedAction === 'pass') {
    await passProject(auditDialog.project.id, opinion || defaultOpinion('pass'), auditDialog.notifyEnabled);
  } else if (completedAction === 'return') {
    await returnProject(auditDialog.project.id, opinion, auditDialog.notifyEnabled);
  } else if (completedAction === 'withdrawReturn') {
    await withdrawReturnProject(auditDialog.project.id, opinion || defaultOpinion('withdrawReturn'), auditDialog.notifyEnabled);
  } else {
    await withdrawPassProject(auditDialog.project.id, opinion || defaultOpinion('withdrawPass'), auditDialog.notifyEnabled);
  }
  proxy?.$modal.msgSuccess('操作成功');
  auditDialog.visible = false;
  await getList();
  if (detailVisible.value && String(currentProject.value?.id || '') === String(auditDialog.project.id)) {
    await openDetail(auditDialog.project);
  }
  if (shouldHintAdjacentNavigation) {
    await showAdjacentNavigationHint();
  }
};

const defaultOpinion = (action: AuditAction) => {
  const map: Record<AuditAction, string> = {
    pass: '审核通过',
    return: '',
    withdrawReturn: '撤销退回，恢复待审核',
    withdrawPass: '撤销通过，恢复待审核'
  };
  return map[action];
};

const rejectReasonTemplatesFrom = (project?: ProjectVO) => {
  const parsed = parseFormData(project?.validationResultJson);
  return Array.isArray(parsed.rejectReasonTemplates) ? parsed.rejectReasonTemplates.filter(Boolean) : [];
};

const applyRejectTemplate = (text: string) => {
  auditDialog.opinion = text;
};

const parseFormData = (text?: string) => {
  if (!text) return {};
  try {
    const parsed = JSON.parse(text);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return { raw: text };
  }
};

const downloadDetailWorkbook = () => {
  if (!currentProject.value) return;
  exportProjectDetailWorkbook(currentProject.value, formRows.value, memberFields.value);
};

const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`;
  return `${(size / 1024 / 1024 / 1024).toFixed(2)} GB`;
};

const officeExts = ['doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx'];
const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg', 'tif', 'tiff'];
const videoExtensions = ['mp4', 'mov', 'webm', 'avi', 'mkv', 'flv', 'mpeg', 'mpg', 'm4v'];
const audioExtensions = ['mp3', 'wav', 'flac', 'aac', 'm4a', 'wma', 'ogg'];
const documentExtensions = ['pdf', ...officeExts, 'txt', 'rtf'];
const mediaTypeLabels: Record<string, string> = {
  image: '图片',
  video: '视频',
  audio: '音频',
  document: '文档',
  office: '文档',
  pdf: '文档'
};

const mediaMimeType = (file: ProjectFileVO) =>
  String(file.mimeType || '')
    .trim()
    .toLowerCase();
const mediaExtension = (file: ProjectFileVO) => fileExt(file);
const mediaTypeLabel = (file: ProjectFileVO) => {
  const rawType = String(file.mediaType || '')
    .trim()
    .toLowerCase();
  if (mediaTypeLabels[rawType]) return mediaTypeLabels[rawType];
  const mimeType = mediaMimeType(file);
  if (mimeType.startsWith('image/')) return '图片';
  if (mimeType.startsWith('video/')) return '视频';
  if (mimeType.startsWith('audio/')) return '音频';
  const ext = mediaExtension(file);
  if (imageExtensions.includes(ext)) return '图片';
  if (videoExtensions.includes(ext)) return '视频';
  if (audioExtensions.includes(ext)) return '音频';
  if (documentExtensions.includes(ext) || mimeType === 'application/pdf') return '文档';
  return ext || mimeType ? '文件' : '';
};

const mediaFormat = (file: ProjectFileVO) => {
  const ext = mediaExtension(file);
  if (ext) return ext.toUpperCase();
  const mimeType = mediaMimeType(file);
  const mimeFormat = mimeType.includes('/') ? mimeType.split('/')[1] : '';
  return mimeFormat ? mimeFormat.split(';')[0].toUpperCase() : '';
};

const formatMediaDuration = (duration?: number) => {
  const totalSeconds = Number(duration);
  if (!Number.isFinite(totalSeconds) || totalSeconds <= 0) return '';
  const roundedSeconds = Math.round(totalSeconds * 10) / 10;
  const hours = Math.floor(roundedSeconds / 3600);
  const minutes = Math.floor((roundedSeconds % 3600) / 60);
  const seconds = Math.round((roundedSeconds % 60) * 10) / 10;
  const secondsText = seconds > 0 ? `${seconds.toFixed(1).replace(/\.0$/, '')}秒` : '';
  if (hours > 0) return `${hours}小时${minutes > 0 ? `${minutes}分` : ''}${secondsText}`;
  if (minutes > 0) return `${minutes}分${secondsText}`;
  return secondsText || '0秒';
};

const mediaSummary = (file: ProjectFileVO) => {
  const values: string[] = [];
  const typeLabel = mediaTypeLabel(file);
  const format = mediaFormat(file);
  if (typeLabel) values.push(typeLabel);
  if (format) values.push(`格式：${format}`);
  if (file.width && file.height) values.push(`尺寸：${file.width}x${file.height}`);
  const durationText = formatMediaDuration(file.durationSeconds);
  if (durationText) values.push(`时长：${durationText}`);
  if (file.fps && Number.isFinite(Number(file.fps))) values.push(`帧率：${Number(file.fps).toFixed(2)}fps`);
  if (file.bitrate && Number.isFinite(Number(file.bitrate))) values.push(`码率：${(Number(file.bitrate) / 1024 / 1024).toFixed(2)}Mbps`);
  if (file.dpi && Number.isFinite(Number(file.dpi))) values.push(`DPI：${file.dpi}`);
  return values.length ? values.join(' / ') : '-';
};

const checkTagType = (status?: string) => {
  if (status === 'passed') return 'success';
  if (status === 'warning') return 'warning';
  if (status === 'failed') return 'danger';
  return 'info';
};

const fileExt = (file: ProjectFileVO) => String(file.fileExt || file.originalName?.split('.').pop() || '').toLowerCase();

const auditSummaryValue = (key: string) => {
  const project = currentProject.value;
  if (!project) return '-';
  const values: Record<string, unknown> = {
    activity: project.activityName || project.activityId,
    category: project.categoryName || project.categoryId,
    school: project.schoolName || project.schoolId,
    submittedAt: project.submittedAt,
    auditOpinion: project.currentAuditOpinion
  };
  return String(values[key] || '-');
};

const auditSummaryItems = computed(() =>
  detailDisplayConfig.value.audit.summaryFields.map((key) => ({
    key,
    label: detailDisplayConfig.value.audit.summaryLabels[key] || key,
    value: auditSummaryValue(key)
  }))
);

const previewType = (file: ProjectFileVO) => {
  const ext = fileExt(file);
  if (officeExts.includes(ext) && file.previewPath) return 'pdf';
  if (file.mediaType === 'video' || ['mp4', 'mov', 'webm', 'ogg'].includes(ext)) return 'video';
  if (file.mediaType === 'image' || ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'].includes(ext)) return 'image';
  if (ext === 'pdf') return 'pdf';
  return '';
};

const previewUrl = (file: ProjectFileVO) => (officeExts.includes(fileExt(file)) ? file.previewPath : file.storagePath);
const canInlinePreview = (file: ProjectFileVO) => !!previewUrl(file) && !!previewType(file);
const auditFileKey = (file?: ProjectFileVO) => String(file?.id || file?.originalName || file?.storagePath || file?.previewPath || '');
const selectedAuditFile = computed(() => {
  const files = currentProject.value?.files || [];
  return files.find((file) => auditFileKey(file) === String(selectedAuditFileId.value || '')) || files[0];
});
const selectedAuditPreviewUrl = computed(() => (selectedAuditFile.value ? previewUrl(selectedAuditFile.value) || '' : ''));
const selectedAuditPreviewType = computed(() => (selectedAuditFile.value ? previewType(selectedAuditFile.value) : ''));
const selectedAuditFileTitle = computed(() => selectedAuditFile.value?.originalName || selectedAuditFile.value?.fileTypeCode || '');
const selectAuditSideFile = (file: ProjectFileVO) => {
  const key = auditFileKey(file);
  selectedAuditFileId.value = key;
};

const refreshCurrentProjectFile = async (file: ProjectFileVO) => {
  if (!currentProject.value?.id || !file?.id) return file;
  try {
    const { data } = useProjectViewData.value ? await getProjectView(currentProject.value.id) : await getAuditProject(currentProject.value.id);
    currentProject.value = data;
    return data?.files?.find((item) => String(item.id) === String(file.id)) || file;
  } catch {
    return file;
  }
};

const openInlineAuditPreview = async (file: ProjectFileVO) => {
  if (!availableAuditTabs.value.includes('preview')) {
    await openPreview(file);
    return;
  }
  const freshFile = await refreshCurrentProjectFile(file);
  selectAuditSideFile(freshFile);
  auditDetailTab.value = 'preview';
};

const openPreview = async (file: ProjectFileVO) => {
  const freshFile = await refreshCurrentProjectFile(file);
  const url = previewUrl(freshFile);
  if (!url) return;
  previewDialog.title = freshFile.originalName || '文件预览';
  previewDialog.url = url;
  previewDialog.type = previewType(freshFile);
  previewDialog.visible = true;
};

const openFile = async (file: ProjectFileVO) => {
  const freshFile = await refreshCurrentProjectFile(file);
  if (freshFile.storagePath) {
    windowOpen(freshFile.storagePath);
  }
};

const windowOpen = (url: string) => {
  window.open(url, '_blank', 'noopener,noreferrer');
};

const pdfViewerUrl = (url?: string) => {
  if (!url) return '';
  const base = url.split('#')[0];
  return `${base}#toolbar=0&navpanes=0&scrollbar=1`;
};

const printPdfPreview = () => {
  if (!previewDialog.url) return;
  const frame = document.createElement('iframe');
  frame.style.position = 'fixed';
  frame.style.right = '0';
  frame.style.bottom = '0';
  frame.style.width = '0';
  frame.style.height = '0';
  frame.style.border = '0';
  frame.src = pdfViewerUrl(previewDialog.url);
  frame.onload = () => {
    try {
      frame.contentWindow?.focus();
      frame.contentWindow?.print();
    } catch {
      windowOpen(previewDialog.url);
    } finally {
      setTimeout(() => frame.remove(), 3000);
    }
  };
  document.body.appendChild(frame);
};

const downloadPdfPreview = () => {
  if (!previewDialog.url) return;
  const link = document.createElement('a');
  link.href = previewDialog.url;
  link.download = `${previewDialog.title || 'preview'}.pdf`;
  link.target = '_blank';
  link.rel = 'noopener noreferrer';
  link.click();
};

const normalizePreviewMessage = (message?: string, status?: string) => {
  return normalizeArtPreviewMessage(message, status);
};

const previewTagType = (status?: string) => {
  if (status === 'converted') return 'success';
  if (status === 'failed') return 'danger';
  return 'info';
};

const statusLabels: Record<string, string> = { draft: '草稿', submitted: '待审核', returned: '已退回', audit_passed: '已通过' };
type ElTagType = 'primary' | 'success' | 'warning' | 'info' | 'danger';
const statusTypes: Record<string, ElTagType> = { draft: 'info', submitted: 'warning', returned: 'danger', audit_passed: 'success' };
const statusSemantics: Record<string, ArtListStatusSemantic> = {
  draft: 'draft',
  submitted: 'pending',
  returned: 'returned',
  audit_passed: 'approved'
};
const statusLabel = (status?: string) => statusLabels[status || ''] || status || '-';
const statusType = (status?: string): ElTagType => statusTypes[status || ''] || 'info';
const statusSemantic = (status?: string): ArtListStatusSemantic => statusSemantics[status || ''] || 'draft';
const auditResultLabel = (result?: string) =>
  ({ pass: '通过', return: '退回', withdraw_return: '撤销退回', withdraw_pass: '撤销通过' })[result || ''] || result || '-';
const auditOperatorLabel = (record: any) => record?.auditedByName || record?.auditedBy || '-';

onMounted(async () => {
  await loadArtDetailDisplayConfig(true);
  applyRouteQuery();
  ensureAllowedStatus();
  await loadActivityOptions();
  await loadSchoolOptions();
  await loadCategoryOptions(false);
  applySelectedCategoryFromRoute();
  await refreshBrowseData();
  await openRouteProjectDetail();
});

watch(
  () => route.fullPath,
  async () => {
    if (routeMatchesState()) return;
    applyRouteQuery();
    ensureAllowedStatus();
    applySelectedCategoryFromRoute();
    await refreshBrowseData();
    await openRouteProjectDetail();
  }
);
</script>

<style scoped>
.audit-header-search {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.audit-header-search .audit-project-search {
  width: 210px;
}

.audit-hierarchy-filter {
  display: grid;
  width: 100%;
  grid-template-columns: auto minmax(0, 1fr);
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.audit-hierarchy-filter > span {
  flex: 0 0 auto;
  color: var(--navigator-text-color, #29445f);
  font-weight: 800;
  white-space: nowrap;
}

.audit-hierarchy-filter :deep(.el-select),
.audit-hierarchy-filter :deep(.art-category-tree-dropdown) {
  width: 100%;
}

.audit-hierarchy-filter :deep(.el-select__wrapper) {
  min-height: 34px;
}

.audit-progress {
  display: grid;
  width: min(100%, var(--audit-progress-width));
  min-width: min(100%, 220px);
  gap: 6px;
  color: var(--audit-progress-text-color);
  font-size: var(--audit-progress-font-size);
}

.audit-progress__summary {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  white-space: nowrap;
}

.audit-progress__summary strong {
  flex: 0 0 auto;
  color: #16324f;
  font-weight: 800;
}

.audit-progress__summary span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.audit-progress :deep(.el-progress) {
  width: 100%;
}

.audit-progress :deep(.el-progress-bar__outer) {
  background-color: var(--audit-progress-track-color);
}

@media (max-width: 768px) {
  .audit-progress {
    width: 100%;
    min-width: 0;
    margin-left: 0;
  }

  .audit-progress :deep(.el-progress) {
    width: 100%;
  }
}

.audit-page :deep(.el-card__body) {
  overflow-x: auto;
}

.audit-list-toolbar {
  display: flex;
  min-width: 880px;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  padding: 7px 10px;
  overflow-x: auto;
  color: #334155;
  background: #f8fbff;
  border: 1px solid #dbe7f5;
  border-radius: 6px;
}

.audit-activity-button,
.audit-toolbar-action {
  flex: 0 0 auto;
}

.audit-activity-button {
  min-width: 76px;
}

.audit-button-caret {
  margin-left: 2px;
  color: #64748b;
  font-size: 11px;
}

.audit-activity-popover {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.audit-activity-options {
  display: grid;
  gap: 4px;
}

.audit-activity-options button {
  width: 100%;
  padding: 8px 10px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #29445f;
  cursor: pointer;
  text-align: left;
}

.audit-activity-options button:hover,
.audit-activity-options button.is-active {
  background: #eff6ff;
  color: #2563eb;
}

.audit-current-activity {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-project-search {
  width: 190px;
  min-width: 160px;
  margin-left: auto;
  flex: 0 0 190px;
}

.audit-workspace {
  display: grid;
  grid-template-columns: minmax(260px, var(--navigator-sidebar-width, 340px)) minmax(0, 1fr);
  gap: 10px;
  align-items: start;
}

.audit-workspace.is-top-layout {
  grid-template-columns: minmax(0, 1fr);
}

.audit-workspace.is-top-layout .audit-filter-card {
  position: static;
}

.audit-filter-card {
  position: sticky;
  top: 10px;
  min-width: 0;
}

.audit-review-card {
  min-width: 0;
  margin-bottom: 10px;
}

.audit-browser-section {
  min-width: 0;
}

.audit-browser-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.audit-browser-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.audit-browser-title strong {
  color: #111827;
  font-size: 14px;
}

.audit-browser-title span {
  overflow: hidden;
  color: #64748b;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.audit-browser-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
}

.audit-browser-tabs :deep(.el-tabs__item) {
  font-size: 14px;
}

.audit-browser-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.audit-browser-scroll {
  padding: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.audit-category-tree {
  width: 100%;
  min-width: 0;
  background: transparent;
}

.audit-category-tree :deep(.el-tree-node__content) {
  height: 40px;
  font-size: 14px;
  color: #303133;
}

.audit-category-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.category-node-label {
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  min-width: 0;
  padding-right: 8px;
  font-size: 14px;
}

.category-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.school-filter-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 8px;
  margin-bottom: 8px;
}

.school-list {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 0;
}

.school-item {
  display: flex;
  width: 100%;
  min-height: 40px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  font-size: 14px;
  color: #334155;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 4px;
}

.school-item:hover,
.school-item.active {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.school-item .school-name {
  min-width: 0;
  overflow: hidden;
  font-size: 14px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.school-item .school-count {
  flex: 0 0 auto;
  font-size: 14px;
  line-height: 20px;
  color: #64748b;
}

.audit-column-settings {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.audit-column-settings-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.audit-column-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
}

.audit-column-options :deep(.el-checkbox) {
  min-width: 0;
  margin-right: 0;
}

.audit-column-settings-tip {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.audit-column-settings-divider {
  margin: 0;
}

.audit-assignment-setting-button {
  width: 100%;
  margin-left: 0;
}

.audit-overview-btn {
  width: 100%;
}

.audit-project-table :deep(.audit-project-no-cell .cell) {
  line-height: 1.45;
}

.audit-project-table :deep(.audit-wrap-cell .cell) {
  line-height: 1.45;
}

.audit-project-table :deep(.audit-action-cell .cell) {
  padding: 4px 6px;
}

.audit-row-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: nowrap;
  gap: 4px;
}

.audit-row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.audit-row-actions :deep(.el-button > span) {
  overflow-wrap: normal;
  text-align: center;
  white-space: nowrap;
  word-break: keep-all;
}

.audit-list-action-button {
  flex: 0 0 auto;
  min-width: 0;
  min-height: 32px;
  padding: 4px 3px;
  font-weight: 600;
}

.audit-list-action-button.is-disabled {
  cursor: not-allowed;
  opacity: 0.36;
}

.audit-page :deep(.audit-table-row) {
  cursor: pointer;
}

.audit-page :deep(.audit-table-row:hover > .el-table__cell) {
  background-color: #f8fafc;
}

:global(.audit-detail-drawer .el-drawer__body) {
  height: 100%;
  padding: 0;
  overflow: hidden;
}

.audit-side-files {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #e4edf7;
}

.audit-detail-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.audit-detail-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.audit-detail-actions :deep(.el-button) {
  min-width: 76px;
}

.detail-nav-button {
  min-height: 34px;
  padding-right: 14px;
  padding-left: 14px;
  font-weight: 600;
}

.art-detail-workbench__nav .el-button-group.is-navigation-hint .detail-nav-button:not(.is-disabled) {
  animation: audit-navigation-hint 1.3s ease-out;
}

@keyframes audit-navigation-hint {
  35% {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    border-color: var(--el-color-primary-light-5);
    box-shadow: 0 0 0 3px rgb(64 158 255 / 8%);
  }

  70% {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    border-color: var(--el-color-primary-light-7);
    box-shadow: 0 0 0 2px rgb(64 158 255 / 5%);
  }
}

.audit-form-content-column :deep(.cell) {
  overflow: visible;
  white-space: normal;
}

.audit-form-value {
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  line-height: 1.6;
}

.audit-form-value.is-collapsed {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}

.audit-form-toggle {
  height: auto;
  padding: 2px 0;
  margin-top: 3px;
  font-size: 12px;
}

.manual-review-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.manual-review-popover {
  color: #1f2937;
}

.manual-review-popover-subject {
  margin-bottom: 5px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.manual-review-popover-content {
  max-height: 220px;
  overflow: auto;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.manual-review-hint {
  color: #b45309;
  font-size: 12px;
  line-height: 1.4;
}

.manual-review-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  padding: 0;
  color: #b45309;
  font-family: inherit;
  font-size: 14px;
  font-weight: 800;
  line-height: 1;
  cursor: pointer;
  background: #fffbeb;
  border: 1px solid #f59e0b;
  border-radius: 50%;
  transition:
    background-color 0.2s ease,
    box-shadow 0.2s ease;
}

.manual-review-mark:hover,
.manual-review-mark:focus-visible {
  background: #fef3c7;
  box-shadow: 0 0 0 3px rgb(245 158 11 / 15%);
  outline: none;
}

.audit-media-summary {
  line-height: 1.55;
  white-space: normal;
  overflow-wrap: anywhere;
}

.pdf-preview-dialog {
  display: flex;
  flex-direction: column;
  min-height: 72vh;
  background: #fff;
}

.pdf-preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  padding: 6px 10px;
  color: #334155;
  border-bottom: 1px solid #e5e7eb;
  background: #f8fafc;
}

.reject-template-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 1180px) {
  .audit-workspace {
    grid-template-columns: 1fr;
  }

  .audit-filter-card {
    position: static;
  }
}

@media (max-width: 768px) {
  .audit-browser-head {
    align-items: stretch;
    flex-direction: column;
  }

  .audit-browser-title span {
    white-space: normal;
  }

  .school-filter-row {
    grid-template-columns: 1fr;
  }

  .audit-detail-actions {
    justify-content: stretch;
  }

  .audit-detail-actions :deep(.el-button) {
    flex: 1 1 120px;
  }
}
</style>

