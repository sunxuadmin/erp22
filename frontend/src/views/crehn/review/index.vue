<template>
  <div class="p-2 review-task-page">
    <div
      class="art-list-workspace art-list-workspace--with-navigator review-task-workspace"
      :class="{ 'is-top-layout': !reviewSidebarOpen }"
      :style="{ '--navigator-sidebar-width': `${detailDisplayConfig.navigator.sidebarWidth}px` }"
    >
      <ProjectBrowseNavigator
        v-if="reviewSidebarOpen"
        page-key="review"
        class="art-list-workspace__navigator"
        :activities="activityOptions"
        :activity-id="queryParams.activityId"
        :show-activity="false"
        :categories="categoryOptions"
        :category-count-map="reviewCategoryCountMap"
        :selected-category-key="selectedReviewCategoryKey"
        :browse-mode="'category'"
        :show-unit-browse="false"
        compact-sidebar
        force-sidebar
        collapsible
        sidebar-title="筛选项目"
        empty-category-text="暂无可评类别"
        @update:collapsed="reviewSidebarOpen = !$event"
        @select-category="selectReviewCategory"
      />
      <el-card shadow="hover" class="art-list-card art-list-workspace__main review-task-list-card">
        <ArtProjectWorkspaceHeader page-key="review" :config="reviewRuntimeHeaderConfig" :title-variables="{ activityName: selectedActivityName }">
          <template #navigatorToggle>
            <ArtWorkspaceConfigButton
              v-if="reviewHeaderConfig.sidebarEnabled"
              :icon="reviewSidebarOpen ? 'Fold' : 'Expand'"
              :text="reviewSidebarOpen ? reviewHeaderConfig.navigatorToggleButton.alternateText : reviewHeaderConfig.navigatorToggleButton.text"
              :tooltip="
                reviewSidebarOpen ? reviewHeaderConfig.navigatorToggleButton.alternateTooltip : reviewHeaderConfig.navigatorToggleButton.tooltip
              "
              @click="reviewSidebarOpen = !reviewSidebarOpen"
            />
          </template>
          <template #reviewScope>
            <ReviewScopeNavigator
              v-if="reviewScopeComponentVisible"
              :model-value="selectedReviewScopeKey"
              :options="reviewScopeOptions"
              :config="reviewWorkbenchConfig.scopeNavigator"
              :progress-config="reviewWorkbenchConfig.progress"
              :label="reviewHeaderText('reviewScope', 'scopeLabel', '评审范围')"
              :placeholder="reviewHeaderText('reviewScope', 'selectPlaceholder', '请选择评审范围')"
              @update:model-value="selectReviewScope"
            />
          </template>
          <template #categoryFilter>
            <div class="review-progress-row__filter">
              <span>{{ reviewHeaderText('categoryFilter', 'categoryLabel', '类别') }}</span>
              <ArtCategoryTreeDropdown
                :tree="reviewCategoryTree"
                :count-map="reviewCategoryCountMap"
                :selected-key="selectedReviewCategoryKey"
                :disabled="!queryParams.activityId"
                :placeholder="reviewHeaderText('categoryFilter', 'categoryPlaceholder', '全部类别')"
                :all-label="reviewHeaderText('categoryFilter', 'categoryPlaceholder', '全部类别')"
                @select="selectReviewCategory"
              />
            </div>
          </template>
          <template #groupFilter>
            <div class="review-progress-row__filter">
              <span>{{ reviewHeaderText('groupFilter', 'groupLabel', '组别') }}</span>
              <el-select
                v-model="queryParams.groupOrNature"
                :disabled="!queryParams.activityId"
                clearable
                :placeholder="reviewHeaderText('groupFilter', 'groupPlaceholder', '全部组别')"
                @change="handleReviewGroupChange"
              >
                <el-option :label="reviewHeaderText('groupFilter', 'groupPlaceholder', '全部组别')" value="" />
                <el-option v-for="item in reviewNavigator.groupOrNatureOptions || []" :key="item.id" :label="item.label" :value="item.id" />
              </el-select>
            </div>
          </template>
          <template #progress>
            <div v-if="reviewWorkbenchConfig.progress.visible" class="review-category-progress" :style="reviewProgressStyle">
              <div class="review-category-progress__summary">
                <span class="review-category-progress__text" :title="reviewProgressText">{{ reviewProgressText }}</span>
                <span v-if="reviewWorkbenchConfig.progress.showPercentage" class="review-category-progress__percentage">
                  {{ categoryProgress.percentage }}%
                </span>
                <span
                  v-if="reviewWorkbenchConfig.progress.showRemaining && !categoryProgress.ready && !categoryProgress.locked"
                  class="review-category-progress__remaining"
                  :title="reviewRemainingText"
                >
                  {{ reviewRemainingText }}
                </span>
              </div>
              <el-progress
                :percentage="categoryProgress.percentage"
                :color="reviewProgressColor"
                :stroke-width="reviewWorkbenchConfig.progress.height"
                :show-text="false"
              />
            </div>
          </template>
          <template #status>
            <ArtStatusTabs
              :items="scoreStatusOptions"
              :model-value="queryParams.scoreStatus"
              :collapse-on-overflow="reviewHeaderConfig.statusCollapseOnOverflow"
              aria-label="评分状态"
              @change="selectScoreStatus(String($event ?? ''))"
            />
          </template>
          <template #search>
            <div class="review-header-search art-list-header-search">
              <el-input
                v-model="queryParams.projectName"
                class="art-list-toolbar__search"
                clearable
                :placeholder="reviewHeaderText('search', 'placeholder', '请输入项目名称')"
                @keyup.enter="refreshReviewData"
              />
              <el-button type="primary" icon="Search" @click="refreshReviewData">{{ reviewHeaderText('search', 'search', '搜索') }}</el-button>
              <el-button icon="Refresh" @click="resetQuery">{{ reviewHeaderText('search', 'reset', '重置') }}</el-button>
            </div>
          </template>
          <template #actions>
            <span
              v-if="canUseCategorySignature && !hasSelectedReviewScope && reviewWorkbenchConfig.signature.hintVisible"
              class="review-progress-row__hint"
              :style="reviewSignatureHintStyle"
              >{{ reviewWorkbenchConfig.signature.hintText }}</span
            >
          </template>
          <template #selectAll>
            <el-tooltip :content="selectAllTooltip" placement="top">
              <el-button
                class="review-category-select-all"
                :class="{ 'is-selected': isEntireCategorySelected }"
                :style="selectAllAppearanceStyle"
                :type="isEntireCategorySelected ? 'primary' : undefined"
                :plain="reviewHeaderConfig.selectAllButton.variant === 'plain'"
                :text="reviewHeaderConfig.selectAllButton.variant === 'text'"
                :icon="
                  reviewHeaderConfig.selectAllButton.iconVisible === false ? undefined : isEntireCategorySelected ? 'CircleClose' : 'CircleCheck'
                "
                :disabled="!hasSelectedReviewScope || total <= 0"
                @click="toggleCategorySelection"
              >
                <span v-if="selectAllButtonText">{{ selectAllButtonText }}</span>
              </el-button>
            </el-tooltip>
          </template>
          <template #unifiedSubmit>
            <el-tooltip :content="unifiedSubmitTooltip" placement="top">
              <el-button
                v-if="canUseCategorySignature"
                class="review-category-submit"
                :style="unifiedSubmitAppearanceStyle"
                type="primary"
                :plain="reviewHeaderConfig.unifiedSubmitButton.variant === 'plain'"
                :text="reviewHeaderConfig.unifiedSubmitButton.variant === 'text'"
                :icon="reviewHeaderConfig.unifiedSubmitButton.iconVisible === false ? undefined : submissionProgress.locked ? 'View' : 'EditPen'"
                :loading="exportLoading"
                :disabled="!hasSelectedReviewScope || submissionProgress.draftCount <= 0"
                @click="openUnifiedSubmitPrecheck"
              >
                <span v-if="unifiedSubmitButtonText">{{ unifiedSubmitButtonText }}</span>
              </el-button>
            </el-tooltip>
          </template>
          <template #columnWidthReset>
            <ArtWorkspaceConfigButton
              icon="RefreshLeft"
              :text="reviewHeaderConfig.columnWidthResetButton.text"
              :tooltip="reviewHeaderConfig.columnWidthResetButton.tooltip"
              @click="restoreColumnWidths"
            />
          </template>
        </ArtProjectWorkspaceHeader>
        <el-alert v-if="managedActivityError" class="mt-2" type="warning" :closable="false" :title="managedActivityError" />
        <div ref="tableShellRef" class="art-resizable-table-shell art-resizable-table-shell--fit">
          <el-table
            ref="reviewTableRef"
            v-loading="loading"
            class="art-list-table art-resizable-table art-resizable-table--borderless review-task-table"
            :class="tableAppearanceClass"
            :style="tableAppearanceStyle"
            border
            :data="taskList"
            :row-key="reviewTaskKey"
            :empty-text="reviewTablePage.emptyText"
            row-class-name="review-task-table-row"
            @header-dragend="handleColumnResize"
            @row-click="openDetail"
            @selection-change="handleReviewSelectionChange"
          >
            <el-table-column
              type="selection"
              :width="columnWidth('selection')"
              :fixed="reviewTablePage.selectionFixed === 'left' ? 'left' : false"
              align="center"
              :selectable="reviewRowSelectable"
            />
            <el-table-column
              v-if="reviewTablePage.serialVisible"
              label="序号"
              :width="columnWidth('serial')"
              :fixed="reviewTablePage.serialFixed === 'left' ? 'left' : false"
              align="center"
            >
              <template #default="scope">{{ reviewSerialNumber(scope.$index) }}</template>
            </el-table-column>
            <el-table-column
              v-for="column in visibleReviewColumns"
              :key="column.key"
              :column-key="column.key"
              :label="column.label"
              :prop="reviewColumnProp(column)"
              :width="columnWidth(column.key)"
              :resizable="column.resizable"
              :fixed="column.fixed"
              :align="['groupOrNature', 'groupName', 'actions', 'scoreStatus'].includes(column.key) ? 'center' : 'left'"
              :class-name="reviewColumnClassName(column)"
              :label-class-name="column.key === 'actions' ? 'art-nonresizable-header' : 'art-resizable-header'"
            >
              <template #default="scope">
                <template v-if="['groupOrNature', 'groupName'].includes(column.key)">{{ reviewGroupText(scope.row) }}</template>
                <template v-else-if="column.key === 'scoreMode'">{{ scoreModeLabel(scope.row.scoreMode) }}</template>
                <template v-else-if="column.key === 'scoreResult'">{{ scoreResultText(scope.row) }}</template>
                <ArtListStatusTag
                  v-else-if="column.key === 'scoreStatus'"
                  :semantic="scoreStatusSemantic(scope.row)"
                  :label="reviewStatusText(scoreStatusSemantic(scope.row), scoreStatusLabel(scope.row))"
                  :tag-type="scoreStatusType(scope.row)"
                />
                <div v-else-if="column.key === 'actions'" class="art-list-row-actions">
                  <el-button
                    class="art-list-row-action art-list-row-action--score"
                    link
                    type="primary"
                    :icon="actionIcon(scope.row.scoreStatus === 'submitted' ? 'View' : 'Edit')"
                    @click.stop="openDetail(scope.row)"
                  >
                    {{
                      scope.row.scoreStatus === 'submitted'
                        ? reviewActionText('view', '查看评分')
                        : scope.row.scoreId
                          ? reviewActionText('edit', '修改评分')
                          : reviewActionText('score', '开始评分')
                    }}
                  </el-button>
                </div>
                <template v-else>{{ reviewColumnText(scope.row, column) }}</template>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <pagination v-show="total > 0" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
      </el-card>
    </div>

    <el-drawer
      v-model="detailVisible"
      size="90%"
      destroy-on-close
      :close-on-click-modal="true"
      :with-header="false"
      :before-close="handleDetailBeforeClose"
      class="review-detail-drawer art-detail-workbench-drawer art-popup art-popup--drawer is-wide"
    >
      <template v-if="currentTask">
        <div class="art-detail-workbench review-detail-shell">
          <header class="art-detail-workbench__header">
            <div class="art-detail-workbench__leading">
              <el-tooltip content="关闭" placement="bottom">
                <el-button
                  class="art-detail-workbench__close"
                  :class="{ 'is-dirty': hasUnsavedScoreChange }"
                  circle
                  icon="Close"
                  aria-label="关闭"
                  @click="requestCloseDetail()"
                />
              </el-tooltip>
              <div class="art-detail-workbench__title">
                <strong class="art-detail-workbench__name">{{ currentTask.projectName }}</strong>
                <span class="art-detail-workbench__meta">作品编号 · {{ currentTask.projectNo || '暂无编号' }}</span>
              </div>
            </div>
            <div class="art-detail-workbench__nav">
              <el-button icon="ArrowLeft" :disabled="!canGoPrev" @click="openAdjacentTask(-1)">上一个</el-button>
              <el-button icon="ArrowRight" :disabled="!canGoNext" @click="openAdjacentTask(1)">下一个</el-button>
            </div>
          </header>

          <el-alert
            v-if="currentTask.lockedByOther"
            class="art-detail-workbench__alert"
            type="warning"
            show-icon
            :closable="false"
            title="该作品已由其他评委提交评分。当前分配为单评委互斥模式，不能再提交。"
          />

          <main v-loading="detailLoading" class="art-detail-workbench__main">
            <section class="art-detail-workbench__content">
              <nav class="art-detail-workbench__tabs" aria-label="评审详情内容">
                <button
                  v-for="tab in availableReviewTabs"
                  :key="tab"
                  type="button"
                  class="art-detail-workbench__tab"
                  :class="{ 'is-active': reviewDetailTab === tab }"
                  @click="reviewDetailTab = tab"
                >
                  {{ reviewTabLabel(tab) }}
                </button>
              </nav>

              <div class="art-detail-workbench__view" :class="{ 'is-scroll': reviewDetailTab !== 'preview' }">
                <div v-if="reviewDetailTab === 'preview'" class="art-detail-workbench__preview">
                  <div class="art-detail-workbench__preview-toolbar">
                    <strong class="art-detail-workbench__preview-title">{{ selectedFileTitle || '作品预览' }}</strong>
                    <div class="art-detail-workbench__preview-actions">
                      <el-button v-if="selectedFile && canPreview(selectedFile)" size="small" icon="View" @click="openSelectedPreview"
                        >弹窗</el-button
                      >
                      <el-button v-if="selectedFile?.storagePath" size="small" icon="Download" @click="openSelectedOriginalFile">原文件</el-button>
                      <el-button v-if="selectedPreviewUrl" size="small" icon="FullScreen" @click="windowOpen(selectedPreviewUrl)">新窗口</el-button>
                    </div>
                  </div>
                  <div class="art-detail-workbench__preview-stage">
                    <video v-if="detailPreviewReady && selectedPreviewType === 'video'" :src="selectedPreviewUrl" controls preload="metadata" />
                    <iframe v-else-if="detailPreviewReady && selectedPreviewType === 'pdf'" :src="pdfViewerUrl(selectedPreviewUrl)" />
                    <img v-else-if="detailPreviewReady && selectedPreviewType === 'image'" :src="selectedPreviewUrl" alt="" />
                    <el-empty v-else description="请选择可预览文件，或下载原文件查看" />
                  </div>
                </div>

                <template v-else-if="reviewDetailTab === 'form'">
                  <dl v-if="reviewBasicSummaryVisible" class="art-detail-workbench__summary review-detail-summary">
                    <div v-if="isScoreBasicSectionVisible('submittedAt')" class="art-detail-workbench__summary-row">
                      <dt>提交时间</dt>
                      <dd>{{ currentTask.submittedAt || '-' }}</dd>
                    </div>
                    <div v-if="isScoreBasicSectionVisible('scoreVisibility')" class="art-detail-workbench__summary-row">
                      <dt>评分可见</dt>
                      <dd>{{ scoreVisibilityLabel(currentTask.scoreVisibilityPolicy) }}</dd>
                    </div>
                  </dl>
                  <el-table v-if="isScoreBasicSectionVisible('formFields') && formRows.length" border :data="formRows" size="small">
                    <el-table-column label="名称" prop="label" width="160" />
                    <el-table-column label="内容" min-width="280">
                      <template #default="scope">
                        <div class="review-form-cell">
                          <span>{{ scope.row.shortValue }}</span>
                          <el-button v-if="scope.row.expandable" link type="primary" @click="openFieldDialog(scope.row)">查看全文</el-button>
                        </div>
                      </template>
                    </el-table-column>
                  </el-table>
                  <el-empty v-else-if="isScoreBasicSectionVisible('formFields')" :image-size="54" description="暂无表单内容" />
                  <ProjectGenericTableReadonly v-if="isScoreBasicSectionVisible('genericTables')" :project="currentTask.project" />
                  <el-empty v-if="!reviewBasicContentVisible" :image-size="54" description="基础信息已在显示设置中隐藏" />
                </template>

                <template v-else-if="reviewDetailTab === 'members'">
                  <ArtProjectMemberReadonly :project="currentTask.project" />
                </template>

                <template v-else-if="reviewDetailTab === 'files'">
                  <el-table v-if="detailDisplayConfig.score.content.fileColumns.length" border :data="currentTask.project?.files || []" size="small">
                    <el-table-column v-if="isScoreFileColumnVisible('fileType')" label="材料类型" width="150">
                      <template #default="scope">{{ detailFileTypeLabel(currentTask.project, scope.row) || '-' }}</template>
                    </el-table-column>
                    <el-table-column
                      v-if="isScoreFileColumnVisible('fileName')"
                      label="文件名"
                      prop="originalName"
                      min-width="240"
                      show-overflow-tooltip
                    />
                    <el-table-column v-if="isScoreFileColumnVisible('fileSize')" label="大小" width="110">
                      <template #default="scope">{{ formatSize(scope.row.fileSize) }}</template>
                    </el-table-column>
                    <el-table-column v-if="isScoreFileColumnVisible('previewStatus')" label="预览状态" min-width="150">
                      <template #default="scope">
                        <el-tag v-if="scope.row.previewStatus" size="small" :type="previewTagType(scope.row.previewStatus)">
                          {{ previewMessage(scope.row) || scope.row.previewStatus }}
                        </el-tag>
                        <span v-else>-</span>
                      </template>
                    </el-table-column>
                    <el-table-column v-if="isScoreFileColumnVisible('actions')" label="操作" width="170" fixed="right">
                      <template #default="scope">
                        <el-button v-if="canPreview(scope.row)" link type="primary" icon="View" @click="selectPreviewFile(scope.row)">预览</el-button>
                        <el-button v-if="scope.row.storagePath" link type="primary" icon="Download" @click="openOriginalFile(scope.row)"
                          >原文件</el-button
                        >
                      </template>
                    </el-table-column>
                  </el-table>
                  <el-empty v-else :image-size="54" description="作品文件信息已在显示设置中隐藏" />
                </template>

                <template v-else-if="reviewDetailTab === 'scores'">
                  <el-table
                    v-if="currentTask.peerScores?.length && detailDisplayConfig.score.content.recordColumns.length"
                    border
                    :data="currentTask.peerScores"
                    size="small"
                  >
                    <el-table-column v-if="isScoreRecordColumnVisible('reviewer')" label="评委" min-width="180" show-overflow-tooltip>
                      <template #default="scope">{{ scope.row.reviewerNickName || scope.row.reviewerUserName || scope.row.reviewerUserId }}</template>
                    </el-table-column>
                    <el-table-column v-if="isScoreRecordColumnVisible('result')" label="评审结果" width="180">
                      <template #default="scope">{{ scoreRecordText(scope.row) }}</template>
                    </el-table-column>
                    <el-table-column
                      v-if="isScoreRecordColumnVisible('comment')"
                      label="意见"
                      prop="commentText"
                      min-width="260"
                      show-overflow-tooltip
                    />
                  </el-table>
                  <el-alert
                    v-else
                    type="info"
                    :closable="false"
                    show-icon
                    :title="detailDisplayConfig.score.content.recordColumns.length ? peerScoreEmptyText : '评分记录信息已在显示设置中隐藏。'"
                  />
                </template>
              </div>
            </section>

            <aside class="art-detail-workbench__side review-side-pane">
              <ReviewScorePanel
                :current-task="currentTask"
                :detail-display-config="detailDisplayConfig"
                :score-form="scoreForm"
                :score-locked="scoreLocked"
                :numeric-rule="numericRule"
                :grade-options="gradeOptions"
                :quick-score-options="quickScoreOptions"
                :quick-score-grid-style="quickScoreGridStyle"
                :score-status-type="scoreStatusType"
                :score-status-label="scoreStatusLabel"
                :apply-quick-score="applyQuickScore"
                :save-score="saveScore"
                :save-score-and-next="saveScoreAndNext"
              />

              <section v-if="detailDisplayConfig.score.fileListVisible" class="art-detail-workbench__side-section is-grow review-file-panel">
                <ArtDetailFileList
                  v-if="fileTotalCount"
                  :files="currentTask.project?.files || []"
                  :project="currentTask.project"
                  :title="fileProgressText"
                  :counter-text="`${viewedFileCount}/${fileTotalCount} 已看`"
                  :selected-key="selectedFileId"
                  :viewed-keys="viewedFileKeys"
                  :detail-fields="detailDisplayConfig.files.fields"
                  :detail-labels="detailDisplayConfig.files.labels"
                  @select="selectPreviewFile"
                />
                <el-empty v-else :image-size="48" description="暂无作品文件" />
              </section>
            </aside>
          </main>
        </div>
      </template>
    </el-drawer>

    <ArtPopupDialog
      v-model="previewDialog.visible"
      :title="previewDialog.title"
      width="80%"
      wide
      top="4vh"
      append-to-body
      destroy-on-close
      class="review-preview-dialog"
    >
      <div class="review-dialog-preview-frame">
        <video v-if="previewDialog.type === 'video'" :src="previewDialog.url" controls preload="metadata" />
        <div v-else-if="previewDialog.type === 'pdf'" class="pdf-preview-dialog">
          <div class="pdf-preview-toolbar">
            <span>PDF 预览</span>
            <div>
              <el-button size="small" icon="Printer" @click="printPdfPreview">打印 PDF</el-button>
              <el-button size="small" icon="Download" @click="downloadPdfPreview">下载 PDF</el-button>
            </div>
          </div>
          <iframe :src="pdfViewerUrl(previewDialog.url)" />
        </div>
        <img v-else-if="previewDialog.type === 'image'" :src="previewDialog.url" alt="" />
        <el-alert v-else type="info" :closable="false" title="该格式暂不支持直接预览，可下载原文件查看。" />
      </div>
      <template #footer>
        <el-button v-if="previewDialog.url" type="primary" icon="Download" @click="windowOpen(previewDialog.url)">下载/新窗口打开</el-button>
        <el-button @click="previewDialog.visible = false">关闭</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="fieldDialog.visible" :title="fieldDialog.title" width="640px" append-to-body>
      <pre class="review-field-full-text">{{ fieldDialog.content }}</pre>
      <template #footer>
        <el-button type="primary" @click="fieldDialog.visible = false">知道了</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="submitPrecheck.visible" title="本批评分提交前检查" width="680px" append-to-body :close-on-click-modal="false">
      <div v-loading="submitPrecheck.loading" class="review-submit-precheck">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="活动" :span="2">{{ selectedActivityName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="评审范围">{{ selectedReviewScopeLabel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属类别">{{ selectedSubmissionCategoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交状态">
            <el-tag :type="submitPrecheckReady ? 'success' : submitPreviewFullySubmitted ? 'info' : 'warning'" effect="plain">
              {{ submitPrecheckReady ? '可以提交本批' : submitPreviewFullySubmitted ? '已全部提交' : '暂无待提交评分' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="分配作品">{{ submitPreview?.assignedTotal ?? submissionProgress.total }}</el-descriptions-item>
          <el-descriptions-item label="已评分">
            {{ submitPreview?.completedCount ?? submissionProgress.completed }} / {{ submitPreview?.assignedTotal ?? submissionProgress.total }}
          </el-descriptions-item>
          <el-descriptions-item label="已经提交">{{ submitPreview?.submittedCount ?? submissionProgress.lockedCount }}</el-descriptions-item>
          <el-descriptions-item label="本批可提交">{{ submitPreview?.total ?? submissionProgress.draftCount }}</el-descriptions-item>
          <el-descriptions-item label="尚未评分">{{ submitPreview?.pendingCount ?? submissionProgress.pending }}</el-descriptions-item>
          <el-descriptions-item label="本批后剩余">{{ submitPreview?.remainingCount ?? submissionProgress.pending }}</el-descriptions-item>
        </el-descriptions>

        <el-alert
          :type="submitPrecheckAlert.type"
          show-icon
          :closable="false"
          :title="submitPrecheckAlert.title"
          :description="submitPrecheckAlert.description"
        />
      </div>
      <template #footer>
        <el-button @click="submitPrecheck.visible = false">取消</el-button>
        <el-button v-if="Number(submitPreview?.pendingCount || 0) > 0" type="warning" plain @click="showPendingReviewTasks"> 查看未评分 </el-button>
        <el-button type="primary" :disabled="!submitPrecheckReady" @click="continueToScoreSheet">
          下一步：提交本批 {{ submitPreview?.total || 0 }} 项
        </el-button>
      </template>
    </ArtPopupDialog>

    <el-dialog v-model="closeGuard.visible" title="未保存评分" width="430px" append-to-body :close-on-click-modal="false" :show-close="false">
      <p class="review-close-guard-text">有未保存的评分内容，关闭后不会自动保存。</p>
      <template #footer>
        <el-button @click="cancelCloseGuard">继续评分</el-button>
        <el-button @click="discardAndCloseDetail">不保存关闭</el-button>
        <el-button type="primary" :loading="closeGuard.saving" @click="saveScoreAndClose">保存评分并关闭</el-button>
      </template>
    </el-dialog>
    <ScoreSheetExportDialog ref="scoreSheetExportDialogRef" @saved="refreshReviewData" />
  </div>
</template>

<script setup name="ArtReview" lang="ts">
import {
  getMyReviewTaskNavigator,
  getMyReviewTask,
  listMyReviewActivityOptions,
  listMyReviewCategoryOptions,
  listMyReviewTask,
  previewReviewScoreSheetSignedSheet,
  saveReviewScoreDraft
} from '@/api/crehn/review';
import type {
  ArtListStatusSemantic,
  ArtReviewListColumnConfig,
  ArtScoreBasicSectionKey,
  ArtScoreDetailTab,
  ArtScoreFileColumnKey,
  ArtScoreRecordColumnKey,
  ArtWorkspaceHeaderButtonAppearanceConfig,
  ArtWorkspaceHeaderPageConfig
} from '@/api/crehn/detailDisplay';
import {
  ActivityCategoryVO,
  ActivityVO,
  ProjectFileVO,
  ReviewScoreSheetSignedSheetPreviewVO,
  ReviewScoreVO,
  ReviewTaskNavigatorVO,
  ReviewTaskScopeOptionVO,
  ReviewTaskVO
} from '@/api/crehn/types';
import { useArtTableColumnWidths } from '@/composables/useArtTableColumnWidths';
import { useArtListTableAppearance } from '@/composables/useArtListTableAppearance';
import { useArtListTablePage } from '@/composables/useArtListTableConfig';
import { useManagedArtActivity } from '@/composables/useManagedArtActivity';
import { useUserStore } from '@/store/modules/user';
import { normalizePreviewMessage } from '@/utils/artReviewMessage';
import { buildSchoolCategoryMenuTree } from '@/utils/artCategory';
import { checkPermi } from '@/utils/permission';
import ArtCategoryTreeDropdown from '../components/ArtCategoryTreeDropdown.vue';
import ArtListStatusTag from '../components/ArtListStatusTag.vue';
import ArtProjectWorkspaceHeader from '../components/ArtProjectWorkspaceHeader.vue';
import ArtWorkspaceConfigButton from '../components/ArtWorkspaceConfigButton.vue';
import ArtStatusTabs from '../components/ArtStatusTabs.vue';
import ArtDetailFileList from '../components/ArtDetailFileList.vue';
import ArtProjectMemberReadonly from '../components/ArtProjectMemberReadonly.vue';
import { detailFileTypeLabel } from '../components/artDetailFileDisplay';
import { loadArtDetailDisplayConfig, useArtDetailDisplayConfig, workspaceHeaderItemText } from '../components/artDetailDisplayConfig';
import { artReviewGroupDisplayText } from '../components/artListTableDefaults';
import ProjectBrowseNavigator from '../components/ProjectBrowseNavigator.vue';
import ProjectGenericTableReadonly from '../project/components/ProjectGenericTableReadonly.vue';
import ReviewScopeNavigator from './components/ReviewScopeNavigator.vue';
import ReviewScorePanel from './components/ReviewScorePanel.vue';
import { scoreModeLabel, scoreRecordText, scoreResultText } from './scorePresentation';
import ScoreSheetExportDialog from '../review-score-sheet/components/ScoreSheetExportDialog.vue';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
type ArtCategoryTreeSelection = {
  key: string;
  categoryIds: Array<string | number>;
  leaf?: boolean;
};
const route = useRoute();
const userStore = useUserStore();

const loading = ref(false);
const exportLoading = ref(false);
const reviewTableRef = ref<{
  clearSelection: () => void;
  toggleRowSelection: (row: ReviewTaskVO, selected?: boolean) => void;
}>();
const scoreSheetExportDialogRef = ref<InstanceType<typeof ScoreSheetExportDialog>>();
const total = ref(0);
const taskList = ref<ReviewTaskVO[]>([]);
const currentTask = ref<ReviewTaskVO>();
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailPreviewReady = ref(false);
const reviewDetailTab = ref<ArtScoreDetailTab>('preview');
const activityOptions = ref<ActivityVO[]>([]);
const { managedActivityError, resolveManagedActivityId } = useManagedArtActivity(activityOptions);
const { tableAppearanceClass, tableAppearanceStyle, actionIcon } = useArtListTableAppearance({
  statusLabels: () => ['unscored', 'scored', 'scoreDraft', 'locked'].map((key) => reviewTablePage.value.statusLabels[key as ArtListStatusSemantic]),
  actionLabels: () => ['view', 'edit', 'score'].map((key) => reviewTablePage.value.actionLabels[key])
});
const categoryOptions = ref<ActivityCategoryVO[]>([]);
const reviewNavigator = ref<ReviewTaskNavigatorVO>({});
const selectedReviewCategoryKey = ref('');
const reviewSidebarOpen = ref(false);
const allCategorySelected = ref(false);
const selectedTaskKeys = ref<Set<string>>(new Set());
const excludedTaskKeys = ref<Set<string>>(new Set());
const submitPrecheck = reactive<{ visible: boolean; loading: boolean; preview?: ReviewScoreSheetSignedSheetPreviewVO }>({
  visible: false,
  loading: false,
  preview: undefined
});
let syncingReviewSelection = false;
const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  activityId: undefined,
  categoryId: undefined,
  categoryIds: undefined,
  programForm: undefined,
  groupOrNature: undefined,
  scopeKey: undefined,
  projectName: '',
  scoreStatus: ''
});
const selectedActivityName = computed(
  () => activityOptions.value.find((item) => String(item.id) === String(queryParams.activityId || ''))?.activityName || ''
);
const scoreForm = reactive<any>({ scoreValue: undefined, gradeValue: '', commentText: '' });
const previewDialog = reactive<{ visible: boolean; title: string; url: string; type: string }>({ visible: false, title: '', url: '', type: '' });
const fieldDialog = reactive<{ visible: boolean; title: string; content: string }>({ visible: false, title: '', content: '' });
const closeGuard = reactive<{ visible: boolean; saving: boolean }>({ visible: false, saving: false });
const selectedFileId = ref<string | number>();
const viewedFileKeys = ref<string[]>([]);
const scoreSnapshot = ref('');
const { detailDisplayConfig } = useArtDetailDisplayConfig();
const reviewWorkbenchConfig = computed(() => detailDisplayConfig.value.reviewWorkbench);
const {
  pageConfig: reviewTablePage,
  columns: configuredReviewColumns,
  statusText: reviewStatusText,
  actionText: reviewActionText
} = useArtListTablePage('review', () => queryParams.categoryId);
const reviewHeaderConfig = computed(() => detailDisplayConfig.value.workspaceHeader.pages.review);
const reviewHeaderText = (
  itemKey: 'reviewScope' | 'categoryFilter' | 'groupFilter' | 'progress' | 'status' | 'search' | 'actions',
  textKey: string,
  fallback: string
) => workspaceHeaderItemText(reviewHeaderConfig.value, itemKey, textKey, fallback);
const canUseCategorySignature = checkPermi(['crehn:reviewSheet:export']);
const detailTaskCache = new Map<string, ReviewTaskVO>();
let pendingDetailCloseDone: (() => void) | undefined;
let detailRequestSeq = 0;
let detailPreviewFrame = 0;
let routeDetailOpenTimer: number | undefined;
let routeDetailOpenResolve: (() => void) | undefined;

const routeQueryText = (value: unknown) => (Array.isArray(value) ? String(value[0] || '') : String(value || ''));
const routeScoreStatus = (value: unknown) => {
  const text = routeQueryText(value);
  return text === 'all' ? '' : text;
};
const detailTaskCacheKey = (assignmentId?: string | number, projectId?: string | number) => `${assignmentId || ''}:${projectId || ''}`;
const cacheDetailTask = (data?: ReviewTaskVO) => {
  if (!data?.assignmentId || !data?.projectId) return;
  detailTaskCache.set(detailTaskCacheKey(data.assignmentId, data.projectId), data);
};
const createDetailPlaceholder = (row: ReviewTaskVO): ReviewTaskVO => ({
  ...row,
  activityName: row.activityName || '正在加载',
  categoryName: row.categoryName || '正在加载',
  projectName: row.projectName || '正在加载作品...',
  scoreMode: row.scoreMode || 'numeric_100',
  exclusiveMode: row.exclusiveMode || 'single',
  scoreStatus: row.scoreStatus || 'none',
  scoreVisibilityPolicy: row.scoreVisibilityPolicy || 'after_submit'
});
const scoreStatusOptions = computed(() => [
  { label: reviewHeaderText('status', 'all', '全部'), value: '' },
  { label: reviewHeaderText('status', 'none', '未评分'), value: 'none' },
  { label: reviewHeaderText('status', 'draft', '已评分'), value: 'draft' },
  { label: reviewHeaderText('status', 'submitted', '已签字'), value: 'submitted' }
]);
const reviewScopeOptions = computed(() => reviewNavigator.value.scopeOptions || []);
const selectedReviewScopeKey = computed(() => String(queryParams.scopeKey || ''));
const selectedReviewScope = computed(() => reviewScopeOptions.value.find((scope) => String(scope.scopeKey || '') === selectedReviewScopeKey.value));
const reviewScopeLabel = (scope?: ReviewTaskScopeOptionVO) => {
  if (!scope) return '';
  const categoryName = String(scope.categoryName || '未命名类别').trim();
  const configuredLabel = String(scope.scopeLabel || '').trim();
  if (scope.wholeCategory || !configuredLabel || configuredLabel === categoryName) return categoryName;
  if (configuredLabel.startsWith(categoryName) || configuredLabel.includes('>') || configuredLabel.includes('＞')) return configuredLabel;
  return `${categoryName} > ${configuredLabel}`;
};
const selectedReviewScopeLabel = computed(() => reviewScopeLabel(selectedReviewScope.value));
const hasSelectedReviewScope = computed(() => Boolean(selectedReviewScope.value?.scopeKey) && Number(selectedReviewScope.value?.totalCount || 0) > 0);
const selectedSubmissionCategoryId = computed(() => selectedReviewScope.value?.categoryId);
const selectedSubmissionCategoryName = computed(() => String(selectedReviewScope.value?.categoryName || '').trim());
const reviewScopeComponentVisible = computed(() => reviewWorkbenchConfig.value.scopeNavigator.visible && reviewScopeOptions.value.length > 0);
const reviewRuntimeHeaderConfig = computed(() => {
  const source = reviewHeaderConfig.value as unknown as ArtWorkspaceHeaderPageConfig;
  if (reviewScopeComponentVisible.value) return source;
  const compactGroupInstances = Object.fromEntries(
    Object.entries(source.compactGroupInstances).map(([key, group]) => [
      key,
      {
        ...group,
        items: group.items.filter((item) => item !== 'reviewScope'),
        pinnedItems: group.pinnedItems.filter((item) => item !== 'reviewScope')
      }
    ])
  );
  return {
    ...source,
    compactGroupInstances,
    cards: source.cards.map((card) => ({
      ...card,
      rows: card.rows.map((row) => row.filter((item) => item !== 'reviewScope'))
    }))
  };
});
const categoryProgress = computed(() => {
  const total = Number(reviewNavigator.value.totalCount || 0);
  const completed = Number(reviewNavigator.value.completedCount || 0);
  const lockedCount = Number(reviewNavigator.value.lockedCount || 0);
  return {
    total,
    completed,
    pending: Math.max(total - completed, 0),
    ready: total > 0 && completed === total,
    locked: total > 0 && lockedCount === total,
    percentage: total > 0 ? Math.round((completed / total) * 100) : 0
  };
});
const submissionProgress = computed(() => {
  const total = Number(selectedReviewScope.value?.totalCount || 0);
  const completed = Number(selectedReviewScope.value?.completedCount || 0);
  const lockedCount = Number(selectedReviewScope.value?.lockedCount || 0);
  return {
    total,
    completed,
    pending: Math.max(total - completed, 0),
    draftCount: Math.max(completed - lockedCount, 0),
    ready: total > 0 && completed === total,
    locked: total > 0 && lockedCount === total,
    lockedCount
  };
});
const reviewCategoryTree = computed(() => buildSchoolCategoryMenuTree(categoryOptions.value));
const selectedCategoryName = computed(
  () =>
    categoryOptions.value.find((item) => String(item.id) === String(queryParams.categoryId || selectedReviewCategoryKey.value))?.categoryName ||
    selectedSubmissionCategoryName.value
);
const selectedReviewCount = computed(() =>
  allCategorySelected.value ? Math.max(total.value - excludedTaskKeys.value.size, 0) : Math.min(selectedTaskKeys.value.size, total.value)
);
const isEntireCategorySelected = computed(() => hasSelectedReviewScope.value && total.value > 0 && selectedReviewCount.value === total.value);
const submitPreview = computed(() => submitPrecheck.preview);
const submitPreviewFullySubmitted = computed(
  () =>
    Number(submitPreview.value?.assignedTotal || 0) > 0 &&
    Number(submitPreview.value?.submittedCount || 0) === Number(submitPreview.value?.assignedTotal || 0)
);
const submitPrecheckReady = computed(
  () => hasSelectedReviewScope.value && submitPreview.value?.signable === true && Number(submitPreview.value.total || 0) > 0
);
const submitPrecheckAlert = computed<{ type: 'success' | 'warning' | 'info'; title: string; description: string }>(() => {
  const preview = submitPreview.value;
  if (!preview) {
    return {
      type: 'info',
      title: '正在检查当前评审范围',
      description: '系统将以服务端当前分配和评分状态为准计算本批可提交作品。'
    };
  }
  if (submitPreviewFullySubmitted.value) {
    return {
      type: 'info',
      title: '当前评审范围已全部提交',
      description: '如需修改评分，请先从历史提交批次中撤回对应评分表。'
    };
  }
  if (!submitPrecheckReady.value) {
    return {
      type: 'warning',
      title: '当前评审范围没有待提交的已评分作品',
      description: preview.message || '请先保存完整评分，再重新点击统一提交。'
    };
  }
  if (Number(preview.pendingCount || 0) > 0) {
    return {
      type: 'warning',
      title: `当前评审范围还有 ${preview.pendingCount} 个作品未评分`,
      description: `未评完也可以提交本批 ${preview.total || 0} 项；本批提交后只锁定这些作品，剩余 ${preview.remainingCount || 0} 项下次再提交。`
    };
  }
  return {
    type: 'success',
    title: '本批提交后当前评审范围将全部完成',
    description: `下一步将打开 ${preview.total || 0} 项评分的批次评分表，签字后只锁定本批作品。`
  };
});
const templateText = (template: string, values: Record<string, string | number>) =>
  template.replace(/\{([A-Za-z][A-Za-z0-9]*)\}/g, (_match, key: string) => String(values[key] ?? ''));
const reviewProgressText = computed(() =>
  templateText(reviewWorkbenchConfig.value.progress.template, {
    categoryName: selectedCategoryName.value,
    completed: categoryProgress.value.completed,
    total: categoryProgress.value.total,
    pending: categoryProgress.value.pending,
    percentage: categoryProgress.value.percentage,
    status: categoryProgress.value.locked ? '已签字' : categoryProgress.value.ready ? '已完成' : '评分中'
  })
);
const reviewRemainingText = computed(() =>
  reviewHeaderText('progress', 'remainingTemplate', '还有 {pending} 个作品未评分').replace('{pending}', String(categoryProgress.value.pending))
);
const reviewProgressColor = computed(() =>
  categoryProgress.value.locked
    ? reviewWorkbenchConfig.value.progress.signedColor
    : categoryProgress.value.ready
      ? reviewWorkbenchConfig.value.progress.successColor
      : reviewWorkbenchConfig.value.progress.activeColor
);
const reviewProgressStyle = computed(() => ({
  '--review-progress-width': `${reviewWorkbenchConfig.value.progress.width}px`,
  '--review-progress-track-color': reviewWorkbenchConfig.value.progress.trackColor,
  '--review-progress-text-color': reviewWorkbenchConfig.value.progress.textColor,
  '--review-progress-font-size': `${reviewWorkbenchConfig.value.progress.fontSize}px`
}));
const reviewSignatureHintStyle = computed(() => ({
  '--review-signature-hint-color': reviewWorkbenchConfig.value.signature.hintColor,
  '--review-signature-hint-background': reviewWorkbenchConfig.value.signature.hintBackgroundColor,
  '--review-signature-hint-border': reviewWorkbenchConfig.value.signature.hintBorderColor,
  '--review-signature-hint-font-size': `${reviewWorkbenchConfig.value.signature.hintFontSize}px`,
  '--review-signature-hint-font-weight':
    reviewWorkbenchConfig.value.signature.hintFontWeight === 'bold'
      ? '700'
      : reviewWorkbenchConfig.value.signature.hintFontWeight === 'normal'
        ? '400'
        : '500'
}));
const colorWithOpacity = (value: string, opacity: number) => {
  const alpha = Math.min(100, Math.max(0, Number(opacity || 0))) / 100;
  const normalized = String(value || '').trim();
  const shortHex = normalized.match(/^#([0-9a-f]{3})$/i)?.[1];
  const longHex = normalized.match(/^#([0-9a-f]{6})$/i)?.[1];
  const hex = shortHex
    ? shortHex
        .split('')
        .map((char) => `${char}${char}`)
        .join('')
    : longHex;
  if (!hex) return alpha === 0 ? 'transparent' : value;
  const number = Number.parseInt(hex, 16);
  return `rgba(${(number >> 16) & 255}, ${(number >> 8) & 255}, ${number & 255}, ${alpha})`;
};
const buttonAppearanceStyle = (appearance?: ArtWorkspaceHeaderButtonAppearanceConfig): Record<string, string> | undefined =>
  appearance
    ? {
        '--art-button-background-color': colorWithOpacity(appearance.backgroundColor, appearance.backgroundOpacity),
        '--art-button-text-color': appearance.textColor,
        '--art-button-border-color': appearance.borderVisible ? colorWithOpacity(appearance.borderColor, appearance.borderOpacity) : 'transparent',
        '--art-button-border-width': `${appearance.borderVisible ? appearance.borderWidth : 0}px`,
        '--art-button-border-radius': `${appearance.borderRadius}px`,
        '--art-button-hover-background-color': colorWithOpacity(appearance.hoverBackgroundColor, appearance.hoverBackgroundOpacity),
        '--art-button-hover-text-color': appearance.hoverTextColor,
        '--art-button-hover-border-color': appearance.borderVisible
          ? colorWithOpacity(appearance.hoverBorderColor, appearance.hoverBorderOpacity)
          : 'transparent'
      }
    : undefined;
const selectAllAppearanceStyle = computed(() => buttonAppearanceStyle(reviewHeaderConfig.value.selectAllButton.appearance));
const unifiedSubmitAppearanceStyle = computed(() => buttonAppearanceStyle(reviewHeaderConfig.value.unifiedSubmitButton.appearance));
const selectAllButtonText = computed(() => {
  const config = reviewHeaderConfig.value.selectAllButton;
  const text = isEntireCategorySelected.value ? config.alternateText : config.text;
  if (!config.showCount) return text;
  const count = `${selectedReviewCount.value}/${total.value}`;
  return text ? `${text}（${count}）` : count;
});
const selectAllTooltip = computed(() =>
  isEntireCategorySelected.value ? reviewHeaderConfig.value.selectAllButton.alternateTooltip : reviewHeaderConfig.value.selectAllButton.tooltip
);
const unifiedSubmitButtonText = computed(() => {
  const config = reviewHeaderConfig.value.unifiedSubmitButton;
  const text = submissionProgress.value.locked
    ? reviewHeaderText('actions', 'lockedSignature', '已全部提交')
    : config.text || reviewWorkbenchConfig.value.signature.buttonText;
  if (!config.showCount) return text;
  if (submissionProgress.value.locked) return text ? `${text}（${submissionProgress.value.total}/${submissionProgress.value.total}）` : text;
  return text ? `${text}（待提交 ${submissionProgress.value.draftCount}）` : String(submissionProgress.value.draftCount);
});
const unifiedSubmitTooltip = computed(() => {
  if (!hasSelectedReviewScope.value) return '请先选择一个有分配作品的评审范围';
  if (submissionProgress.value.locked) return '当前评审范围已全部提交；如需修改，请从历史提交批次撤回';
  if (submissionProgress.value.draftCount <= 0) return '当前评审范围没有待提交的已评分作品';
  return reviewHeaderConfig.value.unifiedSubmitButton.tooltip;
});
const reviewColumns = computed(() => {
  return configuredReviewColumns.value;
});
const visibleReviewColumns = computed(() =>
  reviewColumns.value.filter((column) => !column.deleted && column.visible && !['selection', 'serial'].includes(column.key))
);
const reviewResizableColumns = computed(() => [
  {
    key: 'selection',
    width: 50,
    minWidth: 50,
    visible: true,
    fixed: reviewTablePage.value.selectionFixed === 'left' ? ('left' as const) : false,
    resizable: false
  },
  ...(reviewTablePage.value.serialVisible
    ? [
        {
          key: 'serial',
          width: reviewTablePage.value.serialWidth,
          minWidth: 48,
          visible: true,
          fixed: reviewTablePage.value.serialFixed === 'left' ? ('left' as const) : false,
          resizable: false
        }
      ]
    : []),
  ...visibleReviewColumns.value.map((column) => ({
    key: column.key,
    width: column.width,
    minWidth: column.minWidth,
    visible: true,
    fixed: column.fixed,
    resizable: column.resizable
  }))
]);
const reviewColumnStorageFingerprint = computed(() => {
  const signatureValue = JSON.stringify({
    serial: {
      visible: reviewTablePage.value.serialVisible,
      width: reviewTablePage.value.serialWidth,
      fixed: reviewTablePage.value.serialFixed
    },
    selection: {
      fixed: reviewTablePage.value.selectionFixed
    },
    columns: reviewResizableColumns.value.map((column) => ({
      key: column.key,
      width: column.width,
      minWidth: column.minWidth,
      fixed: column.fixed,
      visible: column.visible,
      resizable: column.resizable
    }))
  });
  let hash = 0x811c9dc5;
  for (let index = 0; index < signatureValue.length; index++) {
    hash ^= signatureValue.charCodeAt(index);
    hash = Math.imul(hash, 0x01000193);
  }
  return `h${hash >>> 0}`;
});
const reviewColumnStorageKey = computed(
  () =>
    `crehn:review-table-widths:v1:${String(userStore.userId || 'anonymous')}:${String(queryParams.categoryId || 'default')}:${reviewColumnStorageFingerprint.value}`
);
const { tableShellRef, columnWidth, handleColumnResize, restoreColumnWidths } = useArtTableColumnWidths({
  columns: reviewResizableColumns,
  storageKey: reviewColumnStorageKey,
  fitContainer: true
});
const reviewColumnProp = (column: ArtReviewListColumnConfig) =>
  column.source === 'form' || ['scoreMode', 'scoreResult', 'scoreStatus', 'actions'].includes(column.key) ? undefined : column.key;
const reviewColumnClassName = (column: ArtReviewListColumnConfig) => {
  if (column.key === 'actions') return 'review-action-cell art-table-nowrap-cell';
  return 'review-wrap-cell art-table-configurable-wrap-cell';
};
const reviewColumnText = (row: ReviewTaskVO, column: ArtReviewListColumnConfig) => {
  if (column.source === 'form') return row.displayFields?.[String(column.fieldKey || '')] || '-';
  const value = (row as unknown as Record<string, unknown>)[column.key];
  return value === undefined || value === null || value === '' ? '-' : String(value);
};
const reviewTaskKey = (row: ReviewTaskVO) => `${String(row.assignmentId || '')}:${String(row.projectId || '')}`;
const reviewGroupText = (row: ReviewTaskVO) => artReviewGroupDisplayText(row.groupOrNature, (row as ReviewTaskVO & { groupName?: string }).groupName);
const reviewSerialNumber = (pageIndex: number) =>
  (Math.max(Number(queryParams.pageNum || 1), 1) - 1) * Math.max(Number(queryParams.pageSize || 10), 1) + pageIndex + 1;
const reviewRowSelectable = () => hasSelectedReviewScope.value;
const isReviewRowSelected = (row: ReviewTaskVO) => {
  const key = reviewTaskKey(row);
  return allCategorySelected.value ? !excludedTaskKeys.value.has(key) : selectedTaskKeys.value.has(key);
};
const syncPageReviewSelection = async () => {
  await nextTick();
  if (!reviewTableRef.value) return;
  syncingReviewSelection = true;
  try {
    reviewTableRef.value.clearSelection();
    taskList.value.forEach((row) => {
      if (isReviewRowSelected(row)) reviewTableRef.value?.toggleRowSelection(row, true);
    });
  } finally {
    syncingReviewSelection = false;
  }
};
const clearReviewSelection = async () => {
  allCategorySelected.value = false;
  selectedTaskKeys.value = new Set();
  excludedTaskKeys.value = new Set();
  await syncPageReviewSelection();
};
const selectEntireReviewCategory = async () => {
  allCategorySelected.value = true;
  selectedTaskKeys.value = new Set();
  excludedTaskKeys.value = new Set();
  await syncPageReviewSelection();
};
const toggleCategorySelection = async () => {
  if (!hasSelectedReviewScope.value || total.value <= 0) return;
  if (isEntireCategorySelected.value) {
    await clearReviewSelection();
    return;
  }
  await selectEntireReviewCategory();
};
const handleReviewSelectionChange = (rows: ReviewTaskVO[]) => {
  if (syncingReviewSelection) return;
  const selectedOnPage = new Set(rows.map(reviewTaskKey));
  if (allCategorySelected.value) {
    const nextExcluded = new Set(excludedTaskKeys.value);
    taskList.value.forEach((row) => {
      const key = reviewTaskKey(row);
      if (selectedOnPage.has(key)) nextExcluded.delete(key);
      else nextExcluded.add(key);
    });
    excludedTaskKeys.value = nextExcluded;
    return;
  }
  const nextSelected = new Set(selectedTaskKeys.value);
  taskList.value.forEach((row) => {
    const key = reviewTaskKey(row);
    if (selectedOnPage.has(key)) nextSelected.add(key);
    else nextSelected.delete(key);
  });
  selectedTaskKeys.value = nextSelected;
};
const reviewCategoryCountMap = computed(() =>
  (reviewNavigator.value.categoryOptions || []).reduce<Record<string, number>>((result, item) => {
    if (item.id) result[String(item.id)] = Number(item.count || 0);
    return result;
  }, {})
);
const fileKey = (file?: ProjectFileVO) => String(file?.id || file?.originalName || file?.storagePath || file?.previewPath || '');
const markFileViewed = (file?: ProjectFileVO) => {
  const key = fileKey(file);
  if (!key || viewedFileKeys.value.includes(key)) return;
  viewedFileKeys.value = [...viewedFileKeys.value, key];
};

const scoreLocked = computed(() => currentTask.value?.scoreStatus === 'submitted' || currentTask.value?.lockedByOther);
const configuredReviewTabs = computed(() =>
  detailDisplayConfig.value.tabs.score.order.filter((tab) => detailDisplayConfig.value.tabs.score.visibleTabs.includes(tab))
);
const availableReviewTabs = computed(() =>
  configuredReviewTabs.value.filter((tab) => {
    if (tab === 'members' && currentTask.value?.hideMemberInfo) return false;
    if (tab === 'scores' && currentTask.value?.scoreVisibilityPolicy === 'hidden') return false;
    return true;
  })
);
const resolveAvailableReviewTab = (tab: ArtScoreDetailTab, task?: ReviewTaskVO): ArtScoreDetailTab => {
  const availableTabs = configuredReviewTabs.value.filter((item) => {
    if (item === 'members' && task?.hideMemberInfo) return false;
    if (item === 'scores' && task?.scoreVisibilityPolicy === 'hidden') return false;
    return true;
  });
  return availableTabs.includes(tab) ? tab : availableTabs[0] || 'preview';
};
const configuredReviewDefaultTab = (task?: ReviewTaskVO) => resolveAvailableReviewTab(detailDisplayConfig.value.tabs.score.defaultTab, task);
const reviewTabLabel = (tab: ArtScoreDetailTab) => {
  const label = detailDisplayConfig.value.tabs.score.labels[tab];
  return tab === 'files' ? `${label}（${fileTotalCount.value}）` : label;
};
const isScoreBasicSectionVisible = (key: ArtScoreBasicSectionKey) => detailDisplayConfig.value.score.content.basicSections.includes(key);
const isScoreFileColumnVisible = (key: ArtScoreFileColumnKey) => detailDisplayConfig.value.score.content.fileColumns.includes(key);
const isScoreRecordColumnVisible = (key: ArtScoreRecordColumnKey) => detailDisplayConfig.value.score.content.recordColumns.includes(key);
const reviewBasicSummaryVisible = computed(() => isScoreBasicSectionVisible('submittedAt') || isScoreBasicSectionVisible('scoreVisibility'));
const reviewBasicContentVisible = computed(
  () => reviewBasicSummaryVisible.value || isScoreBasicSectionVisible('formFields') || isScoreBasicSectionVisible('genericTables')
);
watch(availableReviewTabs, () => {
  reviewDetailTab.value = resolveAvailableReviewTab(reviewDetailTab.value, currentTask.value);
});
const numericRule = computed(() => {
  const parsed = parseJsonObject(currentTask.value?.scoreRuleJson);
  return {
    min: Number(parsed.min ?? 0),
    max: Number(parsed.max ?? 100),
    step: Number(parsed.step ?? 0.01),
    precision: Number(parsed.precision ?? 2)
  };
});
const gradeOptions = computed(() => {
  const parsed = parseJsonObject(currentTask.value?.scoreRuleJson);
  return Array.isArray(parsed.options) && parsed.options.length ? parsed.options.map(String) : ['A', 'B', 'C', 'D'];
});
const quickScoreOptions = computed(() => {
  if (currentTask.value?.scoreMode === 'grade') return [];
  const parsed = parseJsonObject(currentTask.value?.scoreRuleJson);
  const configured = (Array.isArray(parsed.quickScores) ? parsed.quickScores : [30, 60, 80, 90, 100])
    .map(Number)
    .filter((item): item is number => Number.isFinite(item));
  return [...new Set<number>(configured)].filter((item) => item >= numericRule.value.min && item <= numericRule.value.max).slice(0, 5);
});
const quickScoreGridStyle = computed(() => ({ gridTemplateColumns: `repeat(${Math.max(quickScoreOptions.value.length, 1)}, minmax(0, 1fr))` }));
const formRows = computed(() => {
  const data = parseJsonObject(currentTask.value?.project?.formDataJson);
  const hiddenKeys = parseStringArray(currentTask.value?.hiddenFieldKeysJson);
  if (hiddenKeys.includes('*')) return [];
  const schemaMap = new Map((currentTask.value?.project?.fieldSchemas || []).map((item) => [item.fieldKey, item.fieldLabel || item.fieldKey]));
  return Object.entries(data)
    .filter(([key]) => key !== '__genericTables' && !hiddenKeys.includes(key))
    .map(([key, value]) => {
      const valueText = formatValue(value);
      return {
        key,
        label: schemaMap.get(key) || key,
        value: valueText,
        shortValue: shortText(valueText),
        expandable: shouldExpandText(valueText)
      };
    });
});
const currentTaskIndex = computed(() =>
  taskList.value.findIndex(
    (item) => String(item.assignmentId) === String(currentTask.value?.assignmentId) && String(item.projectId) === String(currentTask.value?.projectId)
  )
);
const canGoPrev = computed(() => currentTaskIndex.value > 0 || Number(queryParams.pageNum || 1) > 1);
const canGoNext = computed(() => {
  if (currentTaskIndex.value >= 0 && currentTaskIndex.value < taskList.value.length - 1) return true;
  return Number(queryParams.pageNum || 1) * Number(queryParams.pageSize || 10) < total.value;
});
const selectedFile = computed(() => {
  const files = currentTask.value?.project?.files || [];
  return files.find((item) => fileKey(item) === String(selectedFileId.value || '')) || files[0];
});
const selectedPreviewUrl = computed(() => {
  const file = selectedFile.value;
  if (!file) return '';
  return file.previewStatus === 'converted' && file.previewPath ? file.previewPath : file.storagePath || '';
});
const selectedPreviewType = computed(() => (selectedPreviewUrl.value ? previewTypeOf(selectedFile.value) : ''));
const selectedFileTitle = computed(() => selectedFile.value?.originalName || selectedFile.value?.fileTypeCode || '');
const fileTotalCount = computed(() => currentTask.value?.project?.files?.length || 0);
const selectedFileIndex = computed(() => {
  const files = currentTask.value?.project?.files || [];
  return files.findIndex((file) => fileKey(file) === fileKey(selectedFile.value));
});
const fileProgressText = computed(() => {
  const label = detailDisplayConfig.value.tabs.score.labels.files || '作品文件';
  if (!fileTotalCount.value) return `暂无${label}`;
  return `${label} ${Math.max(selectedFileIndex.value + 1, 1)}/${fileTotalCount.value}`;
});
const viewedFileCount = computed(() => {
  const files = currentTask.value?.project?.files || [];
  return files.filter((file) => viewedFileKeys.value.includes(fileKey(file))).length;
});
const peerScoreEmptyText = computed(() => '评委评分始终互盲，不展示其他评委的评分明细。');
const hasUnsavedScoreChange = computed(() => {
  return !!currentTask.value && !scoreLocked.value && !!scoreSnapshot.value && scoreSnapshot.value !== currentScoreSnapshot();
});

const clearRouteDetailOpenTimer = () => {
  if (routeDetailOpenTimer) {
    window.clearTimeout(routeDetailOpenTimer);
    routeDetailOpenTimer = undefined;
  }
  if (routeDetailOpenResolve) {
    routeDetailOpenResolve();
    routeDetailOpenResolve = undefined;
  }
};
const scheduleDetailPreviewReady = () => {
  detailPreviewReady.value = false;
  if (detailPreviewFrame) {
    window.cancelAnimationFrame(detailPreviewFrame);
  }
  detailPreviewFrame = window.requestAnimationFrame(() => {
    detailPreviewReady.value = true;
    detailPreviewFrame = 0;
  });
};
const applyDetailTask = (data: ReviewTaskVO, options?: { preserveSelectedFile?: boolean; resetViewed?: boolean }) => {
  currentTask.value = data;
  const files = data.project?.files || [];
  const keepSelected = options?.preserveSelectedFile && files.some((file) => fileKey(file) === String(selectedFileId.value || ''));
  if (!keepSelected) {
    const firstFile = files[0];
    selectedFileId.value = firstFile ? fileKey(firstFile) : undefined;
  }
  if (options?.resetViewed !== false) {
    viewedFileKeys.value = [];
  }
  markFileViewed(selectedFile.value);
  Object.assign(scoreForm, {
    scoreValue: data.scoreValue,
    gradeValue: data.gradeValue || '',
    commentText: data.commentText || ''
  });
  resetScoreSnapshot();
  scheduleDetailPreviewReady();
};
const openDetailAfterRouteSettles = (row: ReviewTaskVO) =>
  new Promise<void>((resolve) => {
    clearRouteDetailOpenTimer();
    routeDetailOpenResolve = resolve;
    routeDetailOpenTimer = window.setTimeout(async () => {
      routeDetailOpenTimer = undefined;
      routeDetailOpenResolve = undefined;
      await openDetail(row);
      resolve();
    }, 800);
  });
const handleBeforeUnload = (event: BeforeUnloadEvent) => {
  if (!hasUnsavedScoreChange.value) return;
  event.preventDefault();
  event.returnValue = '';
};

const loadActivityOptions = async () => {
  const { data } = await listMyReviewActivityOptions();
  activityOptions.value = data || [];
  const managedId = resolveManagedActivityId();
  queryParams.activityId = managedId;
  if (managedId !== undefined) {
    await loadCategoryOptions();
  }
};

const loadCategoryOptions = async () => {
  await clearReviewSelection();
  queryParams.categoryId = undefined;
  queryParams.categoryIds = undefined;
  categoryOptions.value = [];
  if (queryParams.activityId) {
    const { data } = await listMyReviewCategoryOptions(queryParams.activityId);
    categoryOptions.value = data || [];
  }
};

const reviewScopeStorageKey = () =>
  `crehn:selected-review-scope:v1:${String(userStore.userId || 'anonymous')}:${String(queryParams.activityId || 'none')}`;
const storedReviewScopeKey = () => {
  try {
    return window.localStorage.getItem(reviewScopeStorageKey()) || '';
  } catch {
    return '';
  }
};
const persistReviewScopeKey = (scopeKey: string) => {
  if (!scopeKey) return;
  try {
    window.localStorage.setItem(reviewScopeStorageKey(), scopeKey);
  } catch {
    // Storage can be disabled; the server-scoped first option remains a safe fallback.
  }
};
const ensureSelectedReviewScope = () => {
  const selectableScopes = reviewScopeOptions.value.filter((scope) => Number(scope.totalCount || 0) > 0);
  const currentScope = selectableScopes.find((scope) => String(scope.scopeKey || '') === String(queryParams.scopeKey || ''));
  if (currentScope?.scopeKey) {
    persistReviewScopeKey(String(currentScope.scopeKey));
    return false;
  }
  const remembered = storedReviewScopeKey();
  const nextScope = selectableScopes.find((scope) => String(scope.scopeKey || '') === remembered) || selectableScopes[0];
  const nextKey = nextScope?.scopeKey ? String(nextScope.scopeKey) : '';
  const changed = String(queryParams.scopeKey || '') !== nextKey;
  queryParams.scopeKey = nextKey || undefined;
  if (nextKey) persistReviewScopeKey(nextKey);
  return changed;
};

const loadReviewNavigator = async () => {
  const { data } = await getMyReviewTaskNavigator(queryParams);
  reviewNavigator.value = data || {};
  if (ensureSelectedReviewScope()) {
    const refreshed = await getMyReviewTaskNavigator(queryParams);
    reviewNavigator.value = refreshed.data || {};
  }
};
const refreshReviewData = async () => {
  queryParams.pageNum = 1;
  await loadReviewNavigator();
  await getList();
};
const selectReviewCategory = async (selection: ArtCategoryTreeSelection) => {
  await clearReviewSelection();
  selectedReviewCategoryKey.value = selection.key;
  queryParams.categoryIds = selection.categoryIds.join(',') || undefined;
  const leafSelection = selection.leaf ?? selection.categoryIds.length === 1;
  queryParams.categoryId = leafSelection && selection.categoryIds.length === 1 ? selection.categoryIds[0] : undefined;
  await refreshReviewData();
};
const handleReviewGroupChange = async (value?: string) => {
  queryParams.groupOrNature = value || undefined;
  await refreshReviewData();
};
const selectReviewScope = async (scopeKey: string) => {
  const scope = reviewScopeOptions.value.find((item) => String(item.scopeKey || '') === scopeKey);
  if (!scope || Number(scope.totalCount || 0) <= 0) return;
  await clearReviewSelection();
  queryParams.pageNum = 1;
  queryParams.scopeKey = scopeKey;
  persistReviewScopeKey(scopeKey);
  await refreshReviewData();
};
const getList = async () => {
  loading.value = true;
  try {
    const res: any = await listMyReviewTask(queryParams);
    taskList.value = res.rows || [];
    total.value = res.total || 0;
    await syncPageReviewSelection();
  } finally {
    loading.value = false;
  }
};

const handleScoreStatusChange = async () => {
  await refreshReviewData();
};

const selectScoreStatus = async (value?: string) => {
  queryParams.scoreStatus = value || '';
  await handleScoreStatusChange();
};

const applyRouteReviewTarget = async () => {
  if (route.path !== '/crehn/review') return;
  const nextScoreStatus = routeScoreStatus(route.query.scoreStatus);
  const shouldReload = queryParams.scoreStatus !== nextScoreStatus;
  queryParams.scoreStatus = nextScoreStatus;
  const assignmentId = routeQueryText(route.query.assignmentId);
  const projectId = routeQueryText(route.query.projectId);
  if (!assignmentId || !projectId) {
    clearRouteDetailOpenTimer();
  }
  const detailPromise = assignmentId && projectId ? openDetailAfterRouteSettles({ assignmentId, projectId } as ReviewTaskVO) : Promise.resolve();
  const listPromise = shouldReload ? refreshReviewData() : Promise.resolve();
  await Promise.all([detailPromise, listPromise]);
};

const resetQuery = async () => {
  await clearReviewSelection();
  queryParams.pageNum = 1;
  queryParams.projectName = '';
  queryParams.scoreStatus = '';
  categoryOptions.value = [];
  queryParams.activityId = resolveManagedActivityId();
  queryParams.categoryId = undefined;
  queryParams.categoryIds = undefined;
  queryParams.scopeKey = undefined;
  queryParams.programForm = undefined;
  queryParams.groupOrNature = undefined;
  selectedReviewCategoryKey.value = '';
  if (queryParams.activityId) {
    await loadCategoryOptions();
  }
  await refreshReviewData();
};

const openCategorySignature = async () => {
  if (!queryParams.activityId || !selectedSubmissionCategoryId.value || !selectedReviewScopeKey.value) {
    proxy?.$modal.msgWarning('请先选择一个有分配作品的评审范围');
    return;
  }
  exportLoading.value = true;
  try {
    await scoreSheetExportDialogRef.value?.open(
      queryParams.activityId,
      selectedSubmissionCategoryId.value,
      selectedReviewScopeKey.value,
      selectedReviewScopeLabel.value
    );
  } finally {
    exportLoading.value = false;
  }
};

const openUnifiedSubmitPrecheck = async () => {
  if (!queryParams.activityId || !selectedSubmissionCategoryId.value || !hasSelectedReviewScope.value) {
    proxy?.$modal.msgWarning('请先选择一个有分配作品的评审范围');
    return;
  }
  submitPrecheck.preview = undefined;
  submitPrecheck.visible = true;
  submitPrecheck.loading = true;
  queryParams.scoreStatus = 'draft';
  try {
    await refreshReviewData();
    await selectEntireReviewCategory();
    const { data } = await previewReviewScoreSheetSignedSheet({
      activityId: queryParams.activityId,
      categoryId: selectedSubmissionCategoryId.value,
      scopeKey: selectedReviewScopeKey.value
    });
    submitPrecheck.preview = data;
  } catch {
    submitPrecheck.visible = false;
  } finally {
    submitPrecheck.loading = false;
  }
};

const continueToScoreSheet = async () => {
  if (!submitPrecheckReady.value) return;
  submitPrecheck.visible = false;
  await openCategorySignature();
};

const showPendingReviewTasks = async () => {
  submitPrecheck.visible = false;
  await clearReviewSelection();
  queryParams.groupOrNature = undefined;
  queryParams.projectName = '';
  queryParams.scoreStatus = 'none';
  await refreshReviewData();
};

const openDetail = async (row: ReviewTaskVO) => {
  if (!row.assignmentId || !row.projectId) return;
  await loadArtDetailDisplayConfig();
  const requestSeq = ++detailRequestSeq;
  const cacheKey = detailTaskCacheKey(row.assignmentId, row.projectId);
  reviewDetailTab.value = configuredReviewDefaultTab(row);
  detailVisible.value = true;
  detailLoading.value = true;
  detailPreviewReady.value = false;
  const cached = detailTaskCache.get(cacheKey);
  applyDetailTask(cached || createDetailPlaceholder(row));
  try {
    const { data } = await getMyReviewTask(row.assignmentId, row.projectId);
    if (requestSeq !== detailRequestSeq) return;
    cacheDetailTask(data);
    applyDetailTask(data);
    reviewDetailTab.value = resolveAvailableReviewTab(reviewDetailTab.value, data);
  } finally {
    if (requestSeq === detailRequestSeq) {
      detailLoading.value = false;
    }
  }
};

const ensureScoreComplete = () => {
  if (!currentTask.value) return false;
  if (currentTask.value.scoreMode === 'comment_only' && !scoreForm.commentText?.trim()) {
    proxy?.$modal.msgError('请输入评语');
    return false;
  }
  if (currentTask.value.scoreMode === 'grade' && !scoreForm.gradeValue) {
    proxy?.$modal.msgError('请选择评分等级');
    return false;
  }
  if (currentTask.value.scoreMode !== 'grade' && currentTask.value.scoreMode !== 'comment_only' && (scoreForm.scoreValue === undefined || scoreForm.scoreValue === null)) {
    proxy?.$modal.msgError('请输入分数');
    return false;
  }
  return true;
};

const saveScore = async () => {
  if (!currentTask.value) return;
  if (!ensureScoreComplete()) return;
  await saveReviewScoreDraft(buildScorePayload());
  proxy?.$modal.msgSuccess('评分已保存，类别签字前仍可修改');
  await refreshCurrentTask();
  await refreshReviewData();
};

const saveScoreAndNext = async () => {
  if (!ensureScoreComplete()) return;
  await saveReviewScoreDraft(buildScorePayload());
  proxy?.$modal.msgSuccess('评分已保存');
  await refreshCurrentTask();
  await refreshReviewData();
  await openAdjacentTask(1);
};

const openAdjacentTask = async (direction: -1 | 1) => {
  if (!(await confirmDiscardBeforeSwitch())) return;
  const index = currentTaskIndex.value;
  const localTarget = index >= 0 ? taskList.value[index + direction] : undefined;
  if (localTarget) {
    await openDetail(localTarget);
    return;
  }
  if (direction > 0 && Number(queryParams.pageNum || 1) * Number(queryParams.pageSize || 10) < total.value) {
    queryParams.pageNum = Number(queryParams.pageNum || 1) + 1;
    await getList();
    if (taskList.value[0]) await openDetail(taskList.value[0]);
    return;
  }
  if (direction < 0 && Number(queryParams.pageNum || 1) > 1) {
    queryParams.pageNum = Number(queryParams.pageNum || 1) - 1;
    await getList();
    const target = taskList.value[taskList.value.length - 1];
    if (target) await openDetail(target);
  }
};

const refreshCurrentTask = async () => {
  if (!currentTask.value) return;
  const { data } = await getMyReviewTask(currentTask.value.assignmentId!, currentTask.value.projectId!);
  cacheDetailTask(data);
  applyDetailTask(data, { preserveSelectedFile: true, resetViewed: false });
};

const buildScorePayload = () => ({
  assignmentId: currentTask.value?.assignmentId,
  projectId: currentTask.value?.projectId,
  scoreValue: scoreForm.scoreValue,
  gradeValue: scoreForm.gradeValue,
  commentText: scoreForm.commentText
});

const scoreVisibilityLabel = (_value?: string) => '评委间始终互盲';
const scoreStatusLabel = (row: ReviewTaskVO) => {
  if (row.lockedByOther) return '已被评分';
  if (row.scoreStatus === 'submitted') return '已签字';
  if (row.scoreStatus === 'draft') return '已评分';
  return '未评分';
};
const scoreStatusType = (row: ReviewTaskVO) => {
  if (row.lockedByOther) return 'warning';
  if (row.scoreStatus === 'submitted') return 'success';
  if (row.scoreStatus === 'draft') return 'info';
  return 'danger';
};
const scoreStatusSemantic = (row: ReviewTaskVO): ArtListStatusSemantic => {
  if (row.lockedByOther) return 'locked';
  if (row.scoreStatus === 'submitted') return 'scored';
  if (row.scoreStatus === 'draft') return 'scoreDraft';
  return 'unscored';
};
const applyQuickScore = (value: number) => {
  if (scoreLocked.value) return;
  scoreForm.scoreValue = value;
};

const currentScoreSnapshot = () =>
  JSON.stringify({
    scoreValue: scoreForm.scoreValue ?? null,
    gradeValue: scoreForm.gradeValue || '',
    commentText: scoreForm.commentText || ''
  });
const resetScoreSnapshot = () => {
  scoreSnapshot.value = currentScoreSnapshot();
};
const handleDetailBeforeClose = (done: () => void) => {
  requestCloseDetail(done);
};
const requestCloseDetail = (done?: () => void) => {
  if (hasUnsavedScoreChange.value) {
    pendingDetailCloseDone = done;
    closeGuard.visible = true;
    return;
  }
  pendingDetailCloseDone = done;
  closeDetailDirect();
};
const closeDetailDirect = () => {
  detailRequestSeq += 1;
  detailLoading.value = false;
  detailPreviewReady.value = false;
  closeGuard.visible = false;
  const done = pendingDetailCloseDone;
  pendingDetailCloseDone = undefined;
  if (done) {
    done();
    return;
  }
  detailVisible.value = false;
};
const discardAndCloseDetail = () => {
  resetScoreSnapshot();
  closeDetailDirect();
};
const cancelCloseGuard = () => {
  pendingDetailCloseDone = undefined;
  closeGuard.visible = false;
};
const saveScoreAndClose = async () => {
  if (!currentTask.value) return;
  if (!ensureScoreComplete()) return;
  closeGuard.saving = true;
  try {
    await saveReviewScoreDraft(buildScorePayload());
    proxy?.$modal.msgSuccess('评分已保存');
    await refreshCurrentTask();
    await refreshReviewData();
    closeDetailDirect();
  } finally {
    closeGuard.saving = false;
  }
};
const confirmDiscardBeforeSwitch = async () => {
  if (!hasUnsavedScoreChange.value) return true;
  try {
    await proxy?.$modal.confirm('当前评分内容未保存，切换作品后不会保存，确认继续？');
    resetScoreSnapshot();
    return true;
  } catch {
    return false;
  }
};

const canPreview = (file: ProjectFileVO) => {
  const ext = (file.fileExt || '').toLowerCase();
  if (['jpg', 'jpeg', 'png', 'gif', 'webp', 'mp4', 'mov', 'mpeg', 'mpg', 'pdf'].includes(ext)) return true;
  return file.previewStatus === 'converted' && !!file.previewPath;
};
const previewTypeOf = (file?: ProjectFileVO) => {
  if (!file) return '';
  const ext = (file.fileExt || '').toLowerCase();
  if (file.previewStatus === 'converted' || ext === 'pdf') return 'pdf';
  if (['mp4', 'mov', 'mpeg', 'mpg'].includes(ext)) return 'video';
  if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext)) return 'image';
  return canPreview(file) ? 'pdf' : '';
};
const selectPreviewFile = (file: ProjectFileVO) => {
  selectedFileId.value = fileKey(file);
  markFileViewed(file);
  if (!availableReviewTabs.value.includes('preview')) {
    void openPreview(file);
    return;
  }
  reviewDetailTab.value = 'preview';
  scheduleDetailPreviewReady();
};
const openSelectedPreview = () => {
  if (selectedFile.value) openPreview(selectedFile.value);
};
const openSelectedOriginalFile = () => {
  if (selectedFile.value) openOriginalFile(selectedFile.value);
};
const refreshCurrentTaskFile = async (file: ProjectFileVO) => {
  if (!currentTask.value?.assignmentId || !currentTask.value?.projectId || !file?.id) return file;
  try {
    const { data } = await getMyReviewTask(currentTask.value.assignmentId, currentTask.value.projectId);
    cacheDetailTask(data);
    currentTask.value = data;
    return data.project?.files?.find((item) => String(item.id) === String(file.id)) || file;
  } catch {
    return file;
  }
};
const openPreview = async (file: ProjectFileVO) => {
  const freshFile = await refreshCurrentTaskFile(file);
  markFileViewed(freshFile);
  const ext = (freshFile.fileExt || '').toLowerCase();
  const previewUrl = freshFile.previewStatus === 'converted' && freshFile.previewPath ? freshFile.previewPath : freshFile.storagePath || '';
  previewDialog.title = freshFile.originalName || '文件预览';
  previewDialog.url = previewUrl;
  if (freshFile.previewStatus === 'converted' || ext === 'pdf') previewDialog.type = 'pdf';
  else if (['mp4', 'mov', 'mpeg', 'mpg'].includes(ext)) previewDialog.type = 'video';
  else if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext)) previewDialog.type = 'image';
  else previewDialog.type = 'other';
  previewDialog.visible = true;
};
const openOriginalFile = async (file: ProjectFileVO) => {
  const freshFile = await refreshCurrentTaskFile(file);
  if (freshFile.storagePath) {
    windowOpen(freshFile.storagePath);
  }
};
const previewTagType = (status?: string) => (status === 'converted' ? 'success' : status === 'failed' ? 'danger' : 'warning');
const previewMessage = (file: ProjectFileVO) => {
  return normalizePreviewMessage(file.previewMessage, file.previewStatus);
};
const formatSize = (size?: number) => {
  if (!size) return '-';
  if (size >= 1024 * 1024 * 1024) return `${(size / 1024 / 1024 / 1024).toFixed(2)} GB`;
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(2)} MB`;
  return `${(size / 1024).toFixed(1)} KB`;
};
const parseJsonObject = (text?: string) => {
  if (!text) return {};
  try {
    const parsed = JSON.parse(text);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
};
const parseStringArray = (text?: string) => {
  if (!text) return [];
  try {
    const parsed = JSON.parse(text);
    return Array.isArray(parsed) ? parsed.map(String) : [];
  } catch {
    return [];
  }
};
const formatValue = (value: any) => {
  if (value === undefined || value === null || value === '') return '-';
  if (Array.isArray(value))
    return (
      value
        .map((item) => formatValue(item))
        .filter((item) => item && item !== '-')
        .join('、') || '-'
    );
  if (typeof value === 'object') {
    const entries = Object.entries(value)
      .filter(([, item]) => item !== undefined && item !== null && item !== '')
      .map(([key, item]) => `${objectFieldLabel(key)}：${formatValue(item)}`);
    return entries.length ? entries.join('，') : JSON.stringify(value);
  }
  return String(value);
};
const shortText = (text: string) => (shouldExpandText(text) ? `${text.replace(/\s+/g, ' ').slice(0, 64)}...` : text);
const shouldExpandText = (text: string) => text.length > 64 || text.includes('\n');
const objectFieldLabel = (key: string) =>
  (
    ({
      name: '姓名',
      teacherName: '姓名',
      teacher_name: '姓名',
      instructorName: '姓名',
      instructor_name: '姓名',
      roleName: '角色',
      role_name: '角色',
      title: '职称',
      phone: '电话',
      mobile: '手机',
      unit: '单位',
      school: '学校',
      department: '院系',
      major: '专业'
    }) as Record<string, string>
  )[key] || key;
const openFieldDialog = (row: { label: string; value: string }) => {
  fieldDialog.title = row.label || '内容详情';
  fieldDialog.content = row.value || '-';
  fieldDialog.visible = true;
};
const pdfViewerUrl = (url?: string) => {
  if (!url) return '';
  return `${url.split('#')[0]}#toolbar=1&navpanes=0&scrollbar=1`;
};
const windowOpen = (url: string) => window.open(url, '_blank', 'noopener,noreferrer');
const printPdfPreview = () => {
  const iframe = document.querySelector<HTMLIFrameElement>('.pdf-preview-dialog iframe');
  iframe?.contentWindow?.print();
};
const downloadPdfPreview = () => windowOpen(previewDialog.url);

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload);
  await loadArtDetailDisplayConfig(true);
  queryParams.scoreStatus = routeScoreStatus(route.query.scoreStatus) || queryParams.scoreStatus;
  await loadActivityOptions();
  const assignmentId = routeQueryText(route.query.assignmentId);
  const projectId = routeQueryText(route.query.projectId);
  const detailPromise = assignmentId && projectId ? openDetailAfterRouteSettles({ assignmentId, projectId } as ReviewTaskVO) : Promise.resolve();
  await Promise.all([refreshReviewData(), detailPromise]);
});

watch(
  () => route.fullPath,
  async () => {
    await applyRouteReviewTarget();
  }
);

onBeforeUnmount(() => {
  clearRouteDetailOpenTimer();
  window.removeEventListener('beforeunload', handleBeforeUnload);
  if (detailPreviewFrame) {
    window.cancelAnimationFrame(detailPreviewFrame);
  }
});
</script>

<style scoped>
.review-task-list-card {
  min-width: 0;
}

.review-header-search {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}

.review-category-select-all,
.review-category-submit {
  color: var(--art-button-text-color);
  background-color: var(--art-button-background-color);
  border-color: var(--art-button-border-color);
  border-width: var(--art-button-border-width);
  border-radius: var(--art-button-border-radius);
}

.review-category-select-all:not(.is-disabled):hover,
.review-category-select-all:not(.is-disabled):focus-visible,
.review-category-submit:not(.is-disabled):hover,
.review-category-submit:not(.is-disabled):focus-visible {
  color: var(--art-button-hover-text-color);
  background-color: var(--art-button-hover-background-color);
  border-color: var(--art-button-hover-border-color);
}

.review-category-select-all.is-selected {
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--el-color-primary) 14%, transparent);
}

.review-task-page :deep(.review-task-table-row) {
  cursor: pointer;
}

.review-task-page :deep(.review-task-table-row:hover > td.el-table__cell) {
  background: #f8fbff;
}

.review-category-progress {
  display: grid;
  width: max-content;
  min-width: min(100%, 220px);
  max-width: 100%;
  gap: 6px;
  color: var(--review-progress-text-color);
  font-size: var(--review-progress-font-size);
}

.review-category-progress__summary {
  display: flex;
  width: 100%;
  min-width: var(--review-progress-width);
  max-width: 100%;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}

.review-progress-row {
  display: flex;
  min-width: 0;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin: 8px 0 10px;
  padding: 10px 12px;
  border: 1px solid #dbe7f5;
  border-radius: 6px;
  background: #f8fbff;
}

.review-progress-row__filter {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  gap: 6px;
}

.review-progress-row__filter > span {
  flex: 0 0 auto;
  color: #16324f;
  font-size: 13px;
  font-weight: 700;
}

.review-progress-row__filter :deep(.el-select),
.review-progress-row__filter :deep(.art-category-tree-dropdown) {
  width: auto;
  min-width: 0;
  flex: 1 1 auto;
}

.review-hidden-activity-switch {
  min-width: 0;
}

.review-activity-options {
  display: grid;
  gap: 4px;
}

.review-activity-options button {
  width: 100%;
  padding: 8px 10px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #29445f;
  cursor: pointer;
  text-align: left;
}

.review-activity-options button:hover,
.review-activity-options button.is-active {
  background: #eff6ff;
  color: #2563eb;
}

.review-progress-row__signature {
  flex: 0 0 auto;
  margin-left: auto;
  color: var(--review-signature-button-text-color) !important;
  font-size: var(--review-signature-button-font-size);
  font-weight: var(--review-signature-button-font-weight);
}

.review-progress-row__hint {
  flex: 0 1 auto;
  padding: 5px 8px;
  color: var(--review-signature-hint-color);
  font-size: var(--review-signature-hint-font-size);
  font-weight: var(--review-signature-hint-font-weight);
  line-height: 1.45;
  background: var(--review-signature-hint-background);
  border: 1px solid var(--review-signature-hint-border);
  border-radius: 4px;
}

.review-category-progress :deep(.el-progress) {
  width: 100%;
  min-width: 0;
}

.review-category-progress :deep(.el-progress-bar__outer) {
  background: var(--review-progress-track-color);
}

.review-category-progress__text,
.review-category-progress__percentage,
.review-category-progress__remaining {
  min-width: 0;
  overflow: hidden;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-category-progress__text {
  flex: 1 1 auto;
  color: #16324f;
  font-weight: 600;
}

.review-category-progress__percentage {
  flex: 0 0 auto;
  color: var(--review-progress-text-color);
  font-variant-numeric: tabular-nums;
}

.review-category-progress__remaining {
  flex: 0 1 auto;
  max-width: 45%;
  color: var(--review-progress-text-color);
}

.review-task-table :deep(.review-wrap-cell .cell) {
  line-height: 1.45;
}

.review-side-pane {
  display: flex;
  flex-direction: column;
  color: #25364b;
  font-size: 14px;
  overflow: hidden;
}

.review-score-panel {
  padding: 12px;
  border-bottom: 1px solid #e4edf7;
  background: #ffffff;
  box-shadow: none;
}

.review-file-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 10px 12px 12px;
  border-bottom: 0;
  background: #fbfdff;
}

.review-score-control,
.review-comment-control {
  min-width: 0;
  margin-bottom: 12px;
}

.review-score-control label,
.review-comment-control label {
  display: block;
  margin-bottom: 6px;
  color: #082f49;
  font-size: 14px;
  font-weight: 900;
}

.review-score-control :deep(.el-select),
.review-score-control :deep(.el-input-number) {
  width: 100%;
}

.review-grade-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.review-grade-group :deep(.el-radio-button__inner) {
  border-left: 1px solid var(--el-border-color);
  border-radius: 6px;
}

.review-quick-scores {
  display: grid;
  gap: 6px;
  margin-top: 8px;
}

.review-quick-scores :deep(.el-button) {
  width: 100%;
  min-width: 0;
  margin-left: 0;
  padding-inline: 6px;
}

.review-score-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.review-score-locked-hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.review-form-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.review-form-cell span {
  min-width: 0;
  overflow: hidden;
  color: #25364b;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-detail-summary {
  margin-bottom: 10px;
}

.review-preview-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}

.review-dialog-preview-frame {
  height: 72vh;
  display: grid;
  place-items: center;
  border: 1px solid #d8e6f5;
  border-radius: 8px;
  background: #f4f8ff;
  overflow: hidden;
}

.review-dialog-preview-frame video,
.review-dialog-preview-frame img {
  width: 100%;
  height: 100%;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.review-dialog-preview-frame video {
  background: #0f172a;
}

.pdf-preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border-bottom: 1px solid #e4edf7;
  background: #ffffff;
}

.pdf-preview-dialog {
  width: 100%;
  height: 100%;
  display: grid;
  grid-template-rows: auto 1fr;
  background: #ffffff;
}

.pdf-preview-dialog iframe {
  width: 100%;
  height: 100%;
  border: 0;
  background: #ffffff;
}

.review-field-full-text {
  max-height: 60vh;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border: 1px solid #e4edf7;
  border-radius: 6px;
  background: #f8fbff;
  color: #25364b;
  font-family: inherit;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.review-submit-precheck {
  display: grid;
  gap: 14px;
}

.review-submit-precheck__groups {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.review-submit-precheck__groups strong {
  margin-right: 4px;
  color: var(--el-text-color-primary);
}

.review-close-guard-text {
  margin: 0;
  color: #25364b;
  line-height: 1.7;
}

@media (max-height: 760px) and (min-width: 1181px) {
  .review-score-panel {
    padding: 10px;
  }

  .review-file-panel {
    padding: 8px 10px 10px;
  }
}

@media (max-width: 768px) {
  .review-progress-row {
    align-items: stretch;
    flex-direction: column;
  }

  .review-progress-row__signature {
    align-self: flex-end;
    margin-left: 0;
  }

  .review-progress-row__filter {
    width: 100%;
  }

  .review-category-progress {
    width: 100%;
    min-width: 0;
  }

  .review-category-progress__summary {
    min-width: 0;
  }

  .review-category-progress :deep(.el-progress) {
    width: 100%;
  }

  .review-status-form-item {
    width: 100%;
  }

  .review-status-menu {
    display: flex;
    width: 100%;
  }

  .review-status-menu :deep(.el-radio-button) {
    flex: 1 1 84px;
  }

  .review-status-menu :deep(.el-radio-button__inner) {
    width: 100%;
    min-width: 0;
    padding-right: 10px;
    padding-left: 10px;
  }
}

@media (max-width: 1180px) {
  .review-side-pane {
    height: auto;
    min-height: 0;
  }

  .review-score-actions {
    justify-content: flex-start;
  }
}
</style>
