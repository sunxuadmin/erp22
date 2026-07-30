package org.dromara.crehn.review.service;

import org.dromara.crehn.domain.bo.ReviewAssignmentBo;
import org.dromara.crehn.domain.bo.ReviewAssignmentScopePlanBo;
import org.dromara.crehn.domain.bo.ReviewScoreAdminBo;
import org.dromara.crehn.domain.bo.ReviewScoreBo;
import org.dromara.crehn.domain.vo.ActivityCategoryVo;
import org.dromara.crehn.domain.vo.ActivityVo;
import org.dromara.crehn.domain.vo.AssignmentBatchPreviewVo;
import org.dromara.crehn.domain.vo.CategoryFieldSchemaVo;
import org.dromara.crehn.domain.vo.ProjectVo;
import org.dromara.crehn.domain.vo.ReviewAssignmentVo;
import org.dromara.crehn.domain.vo.ReviewAssignmentCategoryConfigVo;
import org.dromara.crehn.domain.vo.ReviewProjectOverviewVo;
import org.dromara.crehn.domain.vo.ReviewScoreVo;
import org.dromara.crehn.domain.vo.ReviewTaskVo;
import org.dromara.crehn.domain.vo.ReviewTaskNavigatorVo;
import org.dromara.crehn.domain.vo.ReviewWorkbenchVo;
import org.dromara.crehn.domain.vo.SchoolInfoVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.vo.SysUserVo;

import java.util.List;

public interface IArtReviewService {
    TableDataInfo<ReviewAssignmentVo> queryAssignmentPage(ReviewAssignmentVo query, PageQuery pageQuery);

    ReviewAssignmentVo getAssignment(Long id);

    void addAssignment(ReviewAssignmentBo bo);

    void updateAssignment(ReviewAssignmentBo bo);

    void deleteAssignments(Long[] ids);

    AssignmentBatchPreviewVo previewAssignmentBatch(ReviewAssignmentBo bo);

    AssignmentBatchPreviewVo maintainAssignmentBatch(ReviewAssignmentBo bo);

    AssignmentBatchPreviewVo previewAssignmentScopePlan(ReviewAssignmentScopePlanBo bo);

    AssignmentBatchPreviewVo maintainAssignmentScopePlan(ReviewAssignmentScopePlanBo bo);

    TableDataInfo<ReviewProjectOverviewVo> queryAssignmentProjectPage(ReviewProjectOverviewVo query, PageQuery pageQuery);

    List<SysUserVo> reviewerOptions(String keyword, Long activityId, Long categoryId, Long includeUserId);

    List<ProjectVo> approvedProjectOptions(Long activityId, Long categoryId, String keyword);

    List<SchoolInfoVo> assignmentSchoolOptions(String keyword);

    List<CategoryFieldSchemaVo> assignmentFilterFields(Long categoryId);

    ReviewAssignmentCategoryConfigVo getAssignmentCategoryConfig(Long categoryId);

    ReviewAssignmentCategoryConfigVo saveAssignmentCategoryConfig(Long categoryId, ReviewAssignmentCategoryConfigVo config);

    TableDataInfo<ReviewTaskVo> queryMyTaskPage(ReviewTaskVo query, PageQuery pageQuery);

    ReviewTaskNavigatorVo queryMyTaskNavigator(ReviewTaskVo query);

    List<ReviewTaskVo> queryMySubmittedTasksForExport(Long activityId, Long categoryId);

    List<ReviewTaskVo> queryMyScopeTasksForSignature(Long activityId, Long categoryId, String scopeKey);

    ReviewWorkbenchVo workbench();

    List<ActivityVo> activityOptions();

    List<ActivityCategoryVo> categoryOptions(Long activityId);

    ReviewTaskVo getMyTaskDetail(Long assignmentId, Long projectId);

    ReviewScoreVo saveScoreDraft(ReviewScoreBo bo);

    ReviewScoreVo submitScore(ReviewScoreBo bo);

    List<ReviewScoreVo> listProjectScores(Long projectId);

    ReviewScoreVo adjustScore(ReviewScoreAdminBo bo);

    ReviewScoreVo returnScore(ReviewScoreAdminBo bo);
}
