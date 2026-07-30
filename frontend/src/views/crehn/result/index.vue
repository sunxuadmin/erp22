<template>
  <div class="p-2 review-result-page">
    <el-card shadow="hover" class="mb-[10px]">
      <el-form :model="queryParams" :inline="true">
        <el-form-item label="活动">
          <el-select v-model="queryParams.activityId" clearable filterable placeholder="全部活动" style="width: 240px" @change="handleActivityChange">
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
          >
            <el-option v-for="item in categoryOptions" :key="item.id" :label="item.categoryName" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isAdminMode" label="学校">
          <el-select v-model="queryParams.schoolId" clearable filterable placeholder="全部学校" style="width: 220px">
            <el-option v-for="item in schoolOptions" :key="item.id" :label="schoolLabel(item)" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="isAdminMode" label="发布状态">
          <el-select v-model="queryParams.resultStatus" clearable placeholder="全部" style="width: 130px">
            <el-option label="未发布" value="draft" />
            <el-option label="已发布" value="published" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleSearch">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button v-if="isAdminMode" v-hasPermi="['crehn:result:generate']" type="success" icon="RefreshRight" @click="handleGenerate"
            >生成汇总</el-button
          >
          <el-button v-if="isAdminMode" v-hasPermi="['crehn:result:generate']" type="info" plain icon="RefreshLeft" @click="handleWithdrawGenerated"
            >撤回生成</el-button
          >
          <el-button v-if="isAdminMode" v-hasPermi="['crehn:result:publish']" type="warning" icon="Promotion" @click="handlePublish"
            >发布结果</el-button
          >
          <el-button v-if="isAdminMode" v-hasPermi="['crehn:result:rule']" icon="Trophy" @click="openRuleDialog">奖项规则</el-button>
          <el-button v-if="isAdminMode" v-hasPermi="['crehn:result:export']" icon="Download" @click="handleExport">导出结果</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-tabs v-model="activeMainTab" class="result-main-tabs">
      <el-tab-pane label="评审结果" name="result">
        <el-card shadow="hover">
          <el-alert
            v-if="isAdminMode"
            class="mb-2"
            type="info"
            :closable="false"
            show-icon
            title="生成会读取已签署评分表中的正式评分；发布只使用当前已生成结果，不会再次计算。"
          />
          <el-alert
            v-else
            class="mb-2"
            type="info"
            :closable="false"
            show-icon
            title="这里只展示管理员已发布的评审结果；未发布时不会显示评分和排名。"
          />
          <el-table v-loading="loading" border :data="resultList">
            <el-table-column label="排名" prop="rankNo" width="80" align="center">
              <template #default="scope">{{ scope.row.rankNo || '-' }}</template>
            </el-table-column>
            <el-table-column label="活动" prop="activityName" min-width="170" show-overflow-tooltip />
            <el-table-column label="类别" prop="categoryName" min-width="130" show-overflow-tooltip />
            <el-table-column label="项目编号" prop="projectNo" width="130" show-overflow-tooltip />
            <el-table-column label="项目名称" prop="projectName" min-width="180" show-overflow-tooltip />
            <el-table-column v-if="isAdminMode" label="单位" prop="schoolName" min-width="160" show-overflow-tooltip />
            <el-table-column label="评分数" prop="scoreCount" width="90" align="center" />
            <el-table-column label="平均分" prop="averageScore" width="100" align="center">
              <template #default="scope">{{ formatScore(scope.row.averageScore) }}</template>
            </el-table-column>
            <el-table-column label="等级" prop="finalGrade" width="90" align="center">
              <template #default="scope">{{ scope.row.finalGrade || '-' }}</template>
            </el-table-column>
            <el-table-column label="奖项" prop="awardLevel" width="120" align="center">
              <template #default="scope">
                <el-tag v-if="scope.row.awardLevel" type="warning">{{ scope.row.awardLevel }}</el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="scope">
                <el-tag :type="scope.row.resultStatus === 'published' ? 'success' : 'info'">{{
                  scope.row.resultStatus === 'published' ? '已发布' : '未发布'
                }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="发布时间" prop="publishedAt" width="170" />
            <el-table-column label="备注" prop="remark" min-width="150" show-overflow-tooltip />
            <el-table-column v-if="isAdminMode" label="操作" width="170" fixed="right" align="center">
              <template #default="scope">
                <el-button v-hasPermi="['crehn:result:score']" link type="primary" icon="EditPen" @click="openScoreDrawer(scope.row)"
                  >评分记录</el-button
                >
                <el-button v-hasPermi="['crehn:result:log']" link type="info" icon="Document" @click="openLogDrawer(scope.row)">日志</el-button>
              </template>
            </el-table-column>
          </el-table>
          <pagination
            v-show="total > 0"
            v-model:page="queryParams.pageNum"
            v-model:limit="queryParams.pageSize"
            :total="total"
            @pagination="getList"
          />
        </el-card>
      </el-tab-pane>

      <el-tab-pane v-if="isAdminMode" label="作品汇总" name="summary">
        <el-card shadow="hover">
          <div class="summary-toolbar">
            <el-button v-hasPermi="['crehn:result:summary']" type="primary" icon="Refresh" @click="loadUploadSummary">刷新汇总</el-button>
            <el-button v-hasPermi="['crehn:result:export']" icon="Download" @click="handleUploadExport">导出上传列表</el-button>
          </div>
          <el-alert
            class="mb-2"
            type="info"
            :closable="false"
            show-icon
            title="作品汇总按当前筛选的活动、类别统计全部上传作品，包含草稿、待审核、退回和已通过项目。"
          />
          <el-row :gutter="12" class="mb-3">
            <el-col :xs="24" :md="6">
              <div class="summary-total">
                <div class="summary-total-label">上传总数</div>
                <div class="summary-total-value">{{ uploadSummary.totalCount || 0 }}</div>
              </div>
            </el-col>
            <el-col :xs="24" :md="6">
              <div class="summary-total">
                <div class="summary-total-label">上传学校总数</div>
                <div class="summary-total-value">{{ uploadSummary.schoolCount || 0 }}</div>
              </div>
            </el-col>
          </el-row>
          <el-tabs v-model="summaryTab">
            <el-tab-pane label="状态比例" name="status"><SummaryTable :rows="uploadSummary.statusItems || []" /></el-tab-pane>
            <el-tab-pane label="类别比例" name="category"><SummaryTable :rows="uploadSummary.categoryItems || []" /></el-tab-pane>
            <el-tab-pane label="学校上传" name="school"><SchoolSummaryTable :rows="uploadSummary.schoolItems || []" /></el-tab-pane>
            <el-tab-pane label="组别比例" name="group"><SummaryTable :rows="uploadSummary.groupItems || []" /></el-tab-pane>
            <el-tab-pane label="项目类型" name="projectType"><SummaryTable :rows="uploadSummary.projectTypeItems || []" /></el-tab-pane>
            <el-tab-pane label="节目形式" name="programForm"><SummaryTable :rows="uploadSummary.programFormItems || []" /></el-tab-pane>
            <el-tab-pane label="展演顺序" name="performanceOrder"><SummaryTable :rows="uploadSummary.performanceOrderItems || []" /></el-tab-pane>
          </el-tabs>
        </el-card>
      </el-tab-pane>

      <el-tab-pane v-if="isAdminMode" label="全部作品" name="projects">
        <el-card shadow="hover">
          <div class="summary-toolbar">
            <el-input
              v-model="projectQuery.projectName"
              clearable
              placeholder="搜索作品名称"
              style="width: 220px"
              @keyup.enter="loadProjectOverview"
            />
            <el-select v-model="projectQuery.status" clearable placeholder="全部状态" style="width: 150px">
              <el-option label="草稿" value="draft" />
              <el-option label="待审核" value="submitted" />
              <el-option label="已退回" value="returned" />
              <el-option label="已通过" value="audit_passed" />
            </el-select>
            <el-button v-hasPermi="['crehn:result:summary']" type="primary" icon="Refresh" @click="loadProjectOverview">刷新列表</el-button>
            <el-button v-hasPermi="['crehn:result:export']" icon="Download" @click="handleUploadExport">导出上传列表</el-button>
          </div>
          <el-alert
            class="mb-2"
            type="info"
            :closable="false"
            show-icon
            title="这里展示全部上传作品，不要求先生成评审结果；评分详情可点每行的评分记录查看。"
          />
          <el-table v-loading="projectLoading" border :data="projectOverviewList">
            <el-table-column label="活动" prop="activityName" min-width="170" show-overflow-tooltip />
            <el-table-column label="类别" prop="categoryName" min-width="130" show-overflow-tooltip />
            <el-table-column label="项目编号" prop="projectNo" width="130" show-overflow-tooltip />
            <el-table-column label="作品名称" prop="projectName" min-width="180" show-overflow-tooltip />
            <el-table-column label="单位" prop="schoolName" min-width="160" show-overflow-tooltip />
            <el-table-column label="状态" prop="status" width="100" align="center" />
            <el-table-column label="组别" prop="groupName" width="110" show-overflow-tooltip />
            <el-table-column label="项目类型" prop="projectType" width="140" show-overflow-tooltip />
            <el-table-column label="节目形式" prop="programForm" width="120" show-overflow-tooltip />
            <el-table-column label="评分进度" width="120" align="center">
              <template #default="scope">{{ scoreProgress(scope.row) }}</template>
            </el-table-column>
            <el-table-column label="平均分" prop="averageScore" width="100" align="center">
              <template #default="scope">{{ formatScore(scope.row.averageScore) }}</template>
            </el-table-column>
            <el-table-column label="排名" prop="rankNo" width="80" align="center">
              <template #default="scope">{{ scope.row.rankNo || '-' }}</template>
            </el-table-column>
            <el-table-column label="奖项" prop="awardLevel" width="110" align="center">
              <template #default="scope">{{ scope.row.awardLevel || '-' }}</template>
            </el-table-column>
            <el-table-column label="提交时间" prop="submittedAt" width="170" />
            <el-table-column label="操作" width="110" fixed="right" align="center">
              <template #default="scope">
                <el-button v-hasPermi="['crehn:result:score']" link type="primary" icon="EditPen" @click="openScoreDrawer(scope.row)"
                  >评分记录</el-button
                >
              </template>
            </el-table-column>
          </el-table>
          <pagination
            v-show="projectTotal > 0"
            v-model:page="projectQuery.pageNum"
            v-model:limit="projectQuery.pageSize"
            :total="projectTotal"
            @pagination="loadProjectOverview"
          />
        </el-card>
      </el-tab-pane>

      <el-tab-pane v-if="isAdminMode" label="展演顺序" name="showcaseOrder">
        <el-card shadow="hover">
          <div class="summary-toolbar">
            <el-input v-model="orderFilters.groupName" clearable placeholder="筛选组别" style="width: 160px" @keyup.enter="loadShowcaseOrders" />
            <el-input
              v-model="orderFilters.programForm"
              clearable
              placeholder="筛选节目形式"
              style="width: 180px"
              @keyup.enter="loadShowcaseOrders"
            />
            <el-button v-hasPermi="['crehn:result:summary']" type="primary" icon="Refresh" @click="loadShowcaseOrders">刷新列表</el-button>
            <el-button v-hasPermi="['crehn:result:rule']" type="success" icon="Check" @click="saveShowcaseOrderList">保存顺序</el-button>
            <el-button v-hasPermi="['crehn:result:export']" icon="Download" @click="handleShowcaseOrderExport">导出顺序表</el-button>
          </div>
          <el-alert
            class="mb-2"
            type="info"
            :closable="false"
            show-icon
            title="展演顺序写入项目动态表单字段 performanceOrder，用于抽签/排序展示，不改变项目审核或评分状态。"
          />
          <el-table v-loading="orderLoading" border :data="showcaseOrders">
            <el-table-column label="展演顺序" width="120" fixed="left" align="center">
              <template #default="scope">
                <el-input v-model="scope.row.performanceOrder" placeholder="如 1" />
              </template>
            </el-table-column>
            <el-table-column label="组别" prop="groupName" width="120" show-overflow-tooltip />
            <el-table-column label="项目类型" prop="projectType" width="150" show-overflow-tooltip />
            <el-table-column label="节目形式" prop="programForm" width="130" show-overflow-tooltip />
            <el-table-column label="项目编号" prop="projectNo" width="130" show-overflow-tooltip />
            <el-table-column label="作品名称" prop="projectName" min-width="180" show-overflow-tooltip />
            <el-table-column label="单位" prop="schoolName" min-width="170" show-overflow-tooltip />
            <el-table-column label="状态" prop="status" width="100" align="center" />
            <el-table-column label="提交时间" prop="submittedAt" width="170" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <ArtPopupDialog v-model="readinessDialog.visible" title="结果生成预检" width="720px" append-to-body>
      <div v-loading="readinessDialog.loading">
        <el-descriptions v-if="readiness" :column="3" border class="mb-3">
          <el-descriptions-item label="通过作品">{{ readiness.projectCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="有效评分分配">{{ readiness.activeAssignmentCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="每件要求评委">{{ readiness.requiredReviewerCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="应有评分">{{ readiness.expectedScoreCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="已提交评分">{{ readiness.submittedScoreCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="已签署评分">{{ readiness.signedScoreCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="有效签署表">{{ readiness.activeSignedSheetCount || 0 }}</el-descriptions-item>
        </el-descriptions>

        <el-alert v-if="readiness?.blockers?.length" class="mb-3" type="error" :closable="false" show-icon title="存在阻断项，暂不能生成结果">
          <ul class="readiness-message-list">
            <li v-for="item in readiness.blockers" :key="item">{{ item }}</li>
          </ul>
        </el-alert>

        <el-alert v-if="readiness?.warnings?.length" class="mb-3" type="warning" :closable="false" show-icon title="存在风险提示，请确认后再生成">
          <ul class="readiness-message-list">
            <li v-for="item in readiness.warnings" :key="item">{{ item }}</li>
          </ul>
        </el-alert>

        <el-alert
          v-if="readiness?.ready && !readiness?.warnings?.length"
          class="mb-3"
          type="success"
          :closable="false"
          show-icon
          title="评分分配、提交和签署状态完整，可以生成评审结果。"
        />

        <el-checkbox v-if="readiness?.ready && readiness?.warnings?.length" v-model="readinessDialog.warningsConfirmed" class="readiness-confirm">
          我已核对上述风险提示，仍确认生成本次结果
        </el-checkbox>
      </div>
      <template #footer>
        <el-button type="primary" icon="RefreshRight" :loading="readinessDialog.generating" :disabled="!canSubmitGenerate" @click="submitGenerate"
          >确认生成</el-button
        >
        <el-button @click="readinessDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="publishDialog.visible" title="发布结果可见性确认" width="560px" append-to-body>
      <el-alert
        class="mb-3"
        type="warning"
        :closable="false"
        show-icon
        title="学校端只有在发布后才能看到结果；分数和评语需要管理员明确勾选才会展示。"
      />
      <el-form label-width="120px">
        <el-form-item label="学校端可见">
          <el-checkbox v-model="publishDialog.showRank">显示排名</el-checkbox>
          <el-checkbox v-model="publishDialog.showScore">显示评分/平均分</el-checkbox>
          <el-checkbox v-model="publishDialog.showComment">显示评委评语</el-checkbox>
        </el-form-item>
        <el-form-item label="学校消息">
          <el-switch v-model="publishDialog.notifySchoolAccounts" active-text="发送" inactive-text="不发送" />
          <div class="publish-help">关闭后不发送站内消息，不影响学校账号登录后查看已发布结果。</div>
        </el-form-item>
        <el-form-item label="发布备注">
          <el-input v-model="publishDialog.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" icon="Promotion" @click="submitPublish">确认发布</el-button>
        <el-button @click="publishDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDialog v-model="ruleDialog.visible" title="奖项等级/名次规则" width="980px" append-to-body>
      <el-alert
        class="mb-2"
        type="info"
        :closable="false"
        title="规则按顺序匹配，并在生成结果时形成奖项快照。已生成结果存在时不能修改；如需调整，请先撤回未发布结果。"
      />
      <el-table border :data="awardRules">
        <el-table-column label="奖项" min-width="130">
          <template #default="scope"><el-input v-model="scope.row.awardLevel" placeholder="如 一等奖" /></template>
        </el-table-column>
        <el-table-column label="规则类型" width="130">
          <template #default="scope">
            <el-select v-model="scope.row.ruleType">
              <el-option label="按名次" value="rank_range" />
              <el-option label="按分数" value="score_range" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="起始名次" width="110">
          <template #default="scope"
            ><el-input-number v-model="scope.row.minRank" :min="1" :disabled="scope.row.ruleType !== 'rank_range'" controls-position="right"
          /></template>
        </el-table-column>
        <el-table-column label="截止名次" width="110">
          <template #default="scope"
            ><el-input-number v-model="scope.row.maxRank" :min="1" :disabled="scope.row.ruleType !== 'rank_range'" controls-position="right"
          /></template>
        </el-table-column>
        <el-table-column label="最低分" width="120">
          <template #default="scope"
            ><el-input-number
              v-model="scope.row.minScore"
              :min="0"
              :max="100"
              :precision="2"
              :disabled="scope.row.ruleType !== 'score_range'"
              controls-position="right"
          /></template>
        </el-table-column>
        <el-table-column label="最高分" width="120">
          <template #default="scope"
            ><el-input-number
              v-model="scope.row.maxScore"
              :min="0"
              :max="100"
              :precision="2"
              :disabled="scope.row.ruleType !== 'score_range'"
              controls-position="right"
          /></template>
        </el-table-column>
        <el-table-column label="排序" width="90">
          <template #default="scope"><el-input-number v-model="scope.row.sortOrder" :min="0" controls-position="right" /></template>
        </el-table-column>
        <el-table-column label="启用" width="80" align="center">
          <template #default="scope"><el-switch v-model="scope.row.enabled" /></template>
        </el-table-column>
        <el-table-column label="备注" min-width="150">
          <template #default="scope"><el-input v-model="scope.row.remark" /></template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
          <template #default="scope"
            ><el-button link type="danger" icon="Delete" @click="awardRules.splice(scope.$index, 1)">删除</el-button></template
          >
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button icon="Plus" @click="addAwardRule">新增规则</el-button>
        <el-button type="primary" @click="saveAwardRuleList">保存规则</el-button>
        <el-button @click="ruleDialog.visible = false">关闭</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDrawer v-model="scoreDrawer.visible" title="评分记录" size="720px">
      <el-alert
        v-if="currentResult && canEditScore"
        class="mb-2"
        type="warning"
        :closable="false"
        title="评分进入有效签署表后不可直接退回；请先撤回未发布结果，再撤回对应评分表，由评委修改并重新签署。页面仅保留历史未签署评分的退回能力。"
      />
      <el-alert v-else-if="currentResult" class="mb-2" type="info" :closable="false" title="当前账号仅可查看评分记录，不能退回评分。" />
      <el-table v-loading="scoreLoading" border :data="scoreList">
        <el-table-column label="评委" min-width="150">
          <template #default="scope">{{ scope.row.reviewerNickName || scope.row.reviewerUserName || scope.row.reviewerUserId }}</template>
        </el-table-column>
        <el-table-column label="分数" prop="scoreValue" width="90" />
        <el-table-column label="等级" prop="gradeValue" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="scope"
            ><el-tag :type="scope.row.status === 'submitted' ? 'success' : 'info'">{{
              scope.row.status === 'submitted' ? '已提交' : '草稿'
            }}</el-tag></template
          >
        </el-table-column>
        <el-table-column label="评分提交时间" prop="submittedAt" width="170">
          <template #default="scope">{{ scope.row.submittedAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="意见" prop="commentText" min-width="150" show-overflow-tooltip />
        <el-table-column v-if="canEditScore" label="操作" width="110" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="warning" icon="RefreshLeft" @click="openReturnDialog(scope.row)">退回重评</el-button>
          </template>
        </el-table-column>
      </el-table>
    </ArtPopupDrawer>

    <ArtPopupDialog v-model="scoreDialog.visible" title="退回重评" width="520px" append-to-body>
      <el-form :model="scoreForm" label-width="90px">
        <el-form-item label="原因" required>
          <el-input v-model="scoreForm.reason" type="textarea" :rows="3" placeholder="请填写退回原因，系统会记录留痕" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitScoreAction">确认</el-button>
        <el-button @click="scoreDialog.visible = false">取消</el-button>
      </template>
    </ArtPopupDialog>

    <ArtPopupDrawer v-model="logDrawer.visible" title="操作日志" size="820px">
      <el-table v-loading="logLoading" border :data="logList">
        <el-table-column label="时间" prop="operatedAt" width="170" />
        <el-table-column label="操作人" prop="operatedByName" width="130" />
        <el-table-column label="动作" width="120">
          <template #default="scope">{{ actionLabel(scope.row.actionType) }}</template>
        </el-table-column>
        <el-table-column label="原因" prop="reason" min-width="180" show-overflow-tooltip />
        <el-table-column label="对象" width="130">
          <template #default="scope">{{ scope.row.targetType === 'score' ? `评分 ${scope.row.scoreId || ''}` : '结果/规则' }}</template>
        </el-table-column>
      </el-table>
    </ArtPopupDrawer>
  </div>
</template>

<script setup name="ArtReviewResult" lang="ts">
import { ElMessageBox, ElProgress, ElTable, ElTableColumn } from 'element-plus';
import { defineComponent, h } from 'vue';
import type { PropType } from 'vue';
import { listActivityOptions, listCategoryOptions } from '@/api/crehn/activity';
import { listAuditSchoolOptions } from '@/api/crehn/audit';
import {
  generateReviewResult,
  getReviewResultReadiness,
  listMyReviewResult,
  listProjectReviewOverview,
  listReviewAwardRules,
  listReviewResult,
  listReviewResultLogs,
  getUploadSummary,
  listShowcaseOrders,
  publishReviewResult,
  saveReviewAwardRules,
  saveShowcaseOrders,
  withdrawGeneratedReviewResult
} from '@/api/crehn/result';
import { listProjectReviewScores, returnReviewScore } from '@/api/crehn/review';
import {
  ActivityCategoryVO,
  ActivityVO,
  ProjectUploadSummaryItemVO,
  ProjectUploadSchoolSummaryItemVO,
  ProjectUploadSummaryVO,
  ReviewAwardRuleVO,
  ReviewProjectOverviewVO,
  ReviewResultLogVO,
  ReviewResultReadinessVO,
  ReviewResultVO,
  ReviewScoreAdminForm,
  ReviewScoreVO,
  ShowcaseOrderVO
} from '@/api/crehn/types';
import { useUserStore } from '@/store/modules/user';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const userStore = useUserStore();

const loading = ref(false);
const total = ref(0);
const projectTotal = ref(0);
const activeMainTab = ref('result');
const summaryTab = ref('status');
const resultList = ref<ReviewResultVO[]>([]);
const projectOverviewList = ref<ReviewProjectOverviewVO[]>([]);
const uploadSummary = ref<ProjectUploadSummaryVO>({});
const activityOptions = ref<ActivityVO[]>([]);
const categoryOptions = ref<ActivityCategoryVO[]>([]);
const schoolOptions = ref<any[]>([]);
const awardRules = ref<ReviewAwardRuleVO[]>([]);
const scoreList = ref<ReviewScoreVO[]>([]);
const logList = ref<ReviewResultLogVO[]>([]);
const showcaseOrders = ref<ShowcaseOrderVO[]>([]);
const readiness = ref<ReviewResultReadinessVO>();
const currentResult = ref<{ projectId?: string | number; projectName?: string }>();
const scoreLoading = ref(false);
const logLoading = ref(false);
const orderLoading = ref(false);
const projectLoading = ref(false);

const SummaryTable = defineComponent({
  props: {
    rows: { type: Array as PropType<ProjectUploadSummaryItemVO[]>, default: () => [] }
  },
  setup(props) {
    return () =>
      h(
        ElTable,
        { data: props.rows, border: true },
        {
          default: () => [
            h(ElTableColumn, { label: '名称', prop: 'label', minWidth: 180 }),
            h(ElTableColumn, { label: '数量', prop: 'count', width: 120, align: 'center' }),
            h(
              ElTableColumn,
              { label: '占比', width: 220, align: 'center' },
              {
                default: ({ row }: any) =>
                  h('div', { class: 'summary-percent-cell' }, [
                    h(ElProgress, { percentage: Number(row.percent || 0), strokeWidth: 10 }),
                    h('span', { class: 'summary-percent-text' }, `${Number(row.percent || 0).toFixed(2)}%`)
                  ])
              }
            )
          ]
        }
      );
  }
});

const SchoolSummaryTable = defineComponent({
  props: {
    rows: { type: Array as PropType<ProjectUploadSchoolSummaryItemVO[]>, default: () => [] }
  },
  setup(props) {
    return () =>
      h(
        ElTable,
        { data: props.rows, border: true },
        {
          default: () => [
            h(ElTableColumn, { label: '学校', prop: 'label', minWidth: 200, showOverflowTooltip: true }),
            h(ElTableColumn, { label: '上传数量', prop: 'count', width: 110, align: 'center' }),
            h(ElTableColumn, { label: '草稿', prop: 'draftCount', width: 90, align: 'center' }),
            h(ElTableColumn, { label: '待审核', prop: 'submittedCount', width: 90, align: 'center' }),
            h(ElTableColumn, { label: '已退回', prop: 'returnedCount', width: 90, align: 'center' }),
            h(ElTableColumn, { label: '已通过', prop: 'passedCount', width: 90, align: 'center' }),
            h(
              ElTableColumn,
              { label: '占比', width: 220, align: 'center' },
              {
                default: ({ row }: any) =>
                  h('div', { class: 'summary-percent-cell' }, [
                    h(ElProgress, { percentage: Number(row.percent || 0), strokeWidth: 10 }),
                    h('span', { class: 'summary-percent-text' }, `${Number(row.percent || 0).toFixed(2)}%`)
                  ])
              }
            )
          ]
        }
      );
  }
});

const queryParams = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  activityId: undefined,
  categoryId: undefined,
  schoolId: undefined,
  resultStatus: undefined
});
const projectQuery = reactive<any>({ pageNum: 1, pageSize: 10, projectName: '', status: '' });
const orderFilters = reactive({ groupName: '', programForm: '' });
const ruleDialog = reactive({ visible: false });
const readinessDialog = reactive({ visible: false, loading: false, generating: false, warningsConfirmed: false });
const publishDialog = reactive({
  visible: false,
  showScore: false,
  showRank: true,
  showComment: false,
  notifySchoolAccounts: true,
  remark: ''
});
const scoreDrawer = reactive({ visible: false });
const logDrawer = reactive({ visible: false });
const scoreDialog = reactive({ visible: false });
const scoreForm = reactive<ReviewScoreAdminForm>({});

const hasPermi = (permi: string) => userStore.permissions.includes('*:*:*') || userStore.permissions.includes(permi);
const isAdminMode = computed(() => hasPermi('crehn:result:list'));
const canEditScore = computed(() => hasPermi('crehn:result:score:edit'));
const canSubmitGenerate = computed(() => {
  if (readinessDialog.loading || readinessDialog.generating || !readiness.value?.ready) return false;
  return !readiness.value.warnings?.length || readinessDialog.warningsConfirmed;
});

const loadActivityOptions = async () => {
  const { data } = await listActivityOptions();
  activityOptions.value = data || [];
  if (!queryParams.activityId && activityOptions.value.length === 1) {
    queryParams.activityId = activityOptions.value[0].id;
    await loadCategoryOptions();
  }
};

const loadSchoolOptions = async () => {
  if (!isAdminMode.value) return;
  const res: any = await listAuditSchoolOptions({ status: 'enabled' });
  schoolOptions.value = res.rows || res.data || [];
};

const schoolLabel = (item: any) => [item.schoolName, item.schoolCode].filter(Boolean).join(' / ');

const loadCategoryOptions = async () => {
  queryParams.categoryId = undefined;
  categoryOptions.value = [];
  if (queryParams.activityId) {
    const { data } = await listCategoryOptions(queryParams.activityId);
    categoryOptions.value = data || [];
    queryParams.categoryId = categoryOptions.value[0]?.id;
  }
};

const handleActivityChange = async () => {
  await loadCategoryOptions();
};

const getList = async () => {
  loading.value = true;
  try {
    const res: any = isAdminMode.value ? await listReviewResult(queryParams) : await listMyReviewResult(queryParams);
    resultList.value = res.rows || [];
    total.value = res.total || 0;
    if (activeMainTab.value === 'summary' && isAdminMode.value) {
      await loadUploadSummary();
    }
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => {
  if (activeMainTab.value === 'summary' && isAdminMode.value) {
    await loadUploadSummary();
    return;
  }
  if (activeMainTab.value === 'projects' && isAdminMode.value) {
    projectQuery.pageNum = 1;
    await loadProjectOverview();
    return;
  }
  if (activeMainTab.value === 'showcaseOrder' && isAdminMode.value) {
    await loadShowcaseOrders();
    return;
  }
  await getList();
};

const loadUploadSummary = async () => {
  const { data } = await getUploadSummary({ activityId: queryParams.activityId, categoryId: queryParams.categoryId, schoolId: queryParams.schoolId });
  uploadSummary.value = data || {};
};

const loadProjectOverview = async () => {
  projectLoading.value = true;
  try {
    const res: any = await listProjectReviewOverview({
      ...projectQuery,
      activityId: queryParams.activityId,
      categoryId: queryParams.categoryId,
      schoolId: queryParams.schoolId
    });
    projectOverviewList.value = res.rows || [];
    projectTotal.value = res.total || 0;
  } finally {
    projectLoading.value = false;
  }
};

const loadShowcaseOrders = async () => {
  if (!requireScope()) {
    showcaseOrders.value = [];
    return;
  }
  orderLoading.value = true;
  try {
    const { data } = await listShowcaseOrders({
      activityId: queryParams.activityId,
      categoryId: queryParams.categoryId,
      groupName: orderFilters.groupName,
      programForm: orderFilters.programForm
    });
    showcaseOrders.value = data || [];
  } finally {
    orderLoading.value = false;
  }
};

const resetQuery = async () => {
  queryParams.pageNum = 1;
  queryParams.schoolId = undefined;
  queryParams.resultStatus = undefined;
  projectQuery.pageNum = 1;
  projectQuery.projectName = '';
  projectQuery.status = '';
  orderFilters.groupName = '';
  orderFilters.programForm = '';
  categoryOptions.value = [];
  queryParams.activityId = activityOptions.value.length === 1 ? activityOptions.value[0].id : undefined;
  queryParams.categoryId = undefined;
  if (queryParams.activityId) {
    await loadCategoryOptions();
  }
  await handleSearch();
};

const requireScope = () => {
  if (!queryParams.activityId || !queryParams.categoryId) {
    proxy?.$modal.msgWarning('请先选择活动和类别');
    return false;
  }
  return true;
};

const handleGenerate = async () => {
  if (!requireScope()) return;
  readiness.value = undefined;
  Object.assign(readinessDialog, { visible: true, loading: true, generating: false, warningsConfirmed: false });
  try {
    const { data } = await getReviewResultReadiness(queryParams.activityId, queryParams.categoryId);
    readiness.value = data || {};
  } finally {
    readinessDialog.loading = false;
  }
};

const submitGenerate = async () => {
  if (!canSubmitGenerate.value) return;
  readinessDialog.generating = true;
  try {
    await generateReviewResult({ activityId: queryParams.activityId, categoryId: queryParams.categoryId });
    proxy?.$modal.msgSuccess('已生成评审结果汇总');
    readinessDialog.visible = false;
    queryParams.resultStatus = 'draft';
    await getList();
  } finally {
    readinessDialog.generating = false;
  }
};

const handleWithdrawGenerated = async () => {
  if (!requireScope()) return;
  const { value } = await ElMessageBox.prompt(
    '撤回后会删除当前未发布结果，但不会修改或解锁评分。之后可撤回签署评分表并重新评分。请输入撤回原因。',
    '撤回生成结果',
    {
      confirmButtonText: '确认撤回',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputPlaceholder: '请填写撤回原因',
      inputValidator: (text) => Boolean(text?.trim()) || '请填写撤回原因'
    }
  );
  await withdrawGeneratedReviewResult({
    activityId: queryParams.activityId,
    categoryId: queryParams.categoryId,
    remark: value.trim()
  });
  proxy?.$modal.msgSuccess('已撤回未发布的生成结果');
  queryParams.resultStatus = undefined;
  await getList();
};

const handlePublish = async () => {
  if (!requireScope()) return;
  Object.assign(publishDialog, {
    visible: true,
    showScore: false,
    showRank: true,
    showComment: false,
    notifySchoolAccounts: true,
    remark: ''
  });
};

const submitPublish = async () => {
  await publishReviewResult({
    activityId: queryParams.activityId,
    categoryId: queryParams.categoryId,
    showScore: publishDialog.showScore,
    showRank: publishDialog.showRank,
    showComment: publishDialog.showComment,
    notifySchoolAccounts: publishDialog.notifySchoolAccounts,
    remark: publishDialog.remark
  });
  proxy?.$modal.msgSuccess('已发布评审结果');
  publishDialog.visible = false;
  queryParams.resultStatus = 'published';
  await getList();
};

/*
 * 已发布结果的撤回应是极少数超级管理员应急操作，普通结果页不再提供入口。
 * 后端保留带原因和超级管理员校验的兼容接口。
 */

const openRuleDialog = async () => {
  if (!requireScope()) return;
  const { data } = await listReviewAwardRules(queryParams.activityId, queryParams.categoryId);
  awardRules.value = (data || []).length
    ? data || []
    : [
        { awardLevel: '一等奖', ruleType: 'rank_range', minRank: 1, maxRank: 3, sortOrder: 10, enabled: true },
        { awardLevel: '二等奖', ruleType: 'rank_range', minRank: 4, maxRank: 8, sortOrder: 20, enabled: true },
        { awardLevel: '三等奖', ruleType: 'rank_range', minRank: 9, maxRank: 15, sortOrder: 30, enabled: true }
      ];
  ruleDialog.visible = true;
};

const addAwardRule = () => {
  awardRules.value.push({
    awardLevel: '',
    ruleType: 'rank_range',
    minRank: undefined,
    maxRank: undefined,
    minScore: undefined,
    maxScore: undefined,
    sortOrder: (awardRules.value.length + 1) * 10,
    enabled: true,
    remark: ''
  });
};

const saveAwardRuleList = async () => {
  if (!requireScope()) return;
  await saveReviewAwardRules({ activityId: queryParams.activityId, categoryId: queryParams.categoryId, rules: awardRules.value });
  proxy?.$modal.msgSuccess('奖项规则已保存');
  ruleDialog.visible = false;
  await getList();
};

const handleExport = () => {
  proxy?.download('/crehn/result/export', queryParams, `review_result_${new Date().getTime()}.xlsx`);
};

const handleUploadExport = () => {
  proxy?.download(
    '/crehn/result/upload-export',
    { activityId: queryParams.activityId, categoryId: queryParams.categoryId, schoolId: queryParams.schoolId },
    `project_upload_${new Date().getTime()}.xlsx`
  );
};

const saveShowcaseOrderList = async () => {
  if (!requireScope()) return;
  await saveShowcaseOrders({
    activityId: queryParams.activityId,
    categoryId: queryParams.categoryId,
    remark: '后台维护展演顺序',
    items: showcaseOrders.value.map((item) => ({ projectId: item.projectId, performanceOrder: item.performanceOrder }))
  });
  proxy?.$modal.msgSuccess('展演顺序已保存');
  await loadShowcaseOrders();
};

const handleShowcaseOrderExport = () => {
  proxy?.download(
    '/crehn/result/showcase-order/export',
    {
      activityId: queryParams.activityId,
      categoryId: queryParams.categoryId,
      groupName: orderFilters.groupName,
      programForm: orderFilters.programForm
    },
    `showcase_order_${new Date().getTime()}.xlsx`
  );
};

const openScoreDrawer = async (row: { projectId?: string | number; projectName?: string }) => {
  currentResult.value = row;
  scoreDrawer.visible = true;
  await loadScores(row.projectId);
};

const loadScores = async (projectId?: string | number) => {
  if (!projectId) return;
  scoreLoading.value = true;
  try {
    const { data } = await listProjectReviewScores(projectId);
    scoreList.value = data || [];
  } finally {
    scoreLoading.value = false;
  }
};

const resetScoreForm = () => {
  Object.assign(scoreForm, { scoreId: undefined, scoreValue: undefined, gradeValue: '', commentText: '', reason: '' });
};

const openReturnDialog = (row: ReviewScoreVO) => {
  if (!canEditScore.value) return;
  resetScoreForm();
  Object.assign(scoreForm, { scoreId: row.id, reason: '' });
  scoreDialog.visible = true;
};

const submitScoreAction = async () => {
  if (!canEditScore.value) return;
  if (!scoreForm.reason) {
    proxy?.$modal.msgWarning('请填写原因');
    return;
  }
  await returnReviewScore(scoreForm);
  proxy?.$modal.msgSuccess('已退回重评');
  scoreDialog.visible = false;
  await loadScores(currentResult.value?.projectId);
  await getList();
};

const openLogDrawer = async (row: ReviewResultVO) => {
  currentResult.value = row;
  logDrawer.visible = true;
  logLoading.value = true;
  try {
    const { data } = await listReviewResultLogs({ projectId: row.projectId });
    logList.value = data || [];
  } finally {
    logLoading.value = false;
  }
};

const actionLabel = (action?: string) => {
  const map: Record<string, string> = {
    award_rule_save: '保存奖项规则',
    score_adjust: '调整评分',
    score_return: '退回重评',
    result_generate_withdraw: '撤回生成结果',
    showcase_order_save: '保存展演顺序'
  };
  return action ? map[action] || action : '-';
};

const formatScore = (value?: number) => {
  if (value === null || value === undefined) return '-';
  return Number(value).toFixed(2);
};

const scoreProgress = (row: ReviewProjectOverviewVO) => {
  const done = row.submittedScoreCount || 0;
  const total = row.assignmentCount || 0;
  return total > 0 ? `${done}/${total}` : `${done}/0`;
};

onMounted(async () => {
  await loadActivityOptions();
  await loadSchoolOptions();
  await getList();
});

watch(activeMainTab, async (value) => {
  if (value === 'summary' && isAdminMode.value) {
    await loadUploadSummary();
  }
  if (value === 'projects' && isAdminMode.value) {
    await loadProjectOverview();
  }
  if (value === 'showcaseOrder' && isAdminMode.value) {
    await loadShowcaseOrders();
  }
});
</script>

<style scoped>
.result-main-tabs {
  --el-tabs-header-height: 42px;
}

.summary-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.summary-total {
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
}

.summary-total-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.summary-total-value {
  margin-top: 4px;
  font-size: 28px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.summary-percent-cell {
  display: grid;
  grid-template-columns: 1fr 64px;
  align-items: center;
  gap: 8px;
}

.summary-percent-text {
  color: var(--el-text-color-secondary);
  font-variant-numeric: tabular-nums;
}

.readiness-message-list {
  margin: 8px 0 0;
  padding-left: 20px;
  line-height: 1.7;
}

.readiness-confirm {
  align-items: flex-start;
  white-space: normal;
}

.publish-help {
  width: 100%;
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}
</style>
