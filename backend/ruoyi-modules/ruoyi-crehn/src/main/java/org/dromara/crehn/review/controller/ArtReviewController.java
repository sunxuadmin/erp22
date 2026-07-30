package org.dromara.crehn.review.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.bo.ReviewScoreAdminBo;
import org.dromara.crehn.domain.bo.ReviewAssignmentBo;
import org.dromara.crehn.domain.bo.ReviewAssignmentScopePlanBo;
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
import org.dromara.crehn.review.service.IArtReviewService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.vo.SysUserVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/review")
public class ArtReviewController extends BaseController {

    private final IArtReviewService reviewService;

    @SaCheckPermission("crehn:reviewAssignment:list")
    @GetMapping("/assignment/list")
    public TableDataInfo<ReviewAssignmentVo> assignmentList(ReviewAssignmentVo query, PageQuery pageQuery) {
        return reviewService.queryAssignmentPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:reviewAssignment:query")
    @GetMapping("/assignment/{id}")
    public R<ReviewAssignmentVo> assignmentDetail(@PathVariable Long id) {
        return R.ok(reviewService.getAssignment(id));
    }

    @SaCheckPermission("crehn:reviewAssignment:add")
    @Log(title = "art review assignment add", businessType = BusinessType.INSERT)
    @PostMapping("/assignment")
    public R<Void> addAssignment(@RequestBody ReviewAssignmentBo bo) {
        reviewService.addAssignment(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:reviewAssignment:list")
    @PostMapping("/assignment/batch/preview")
    public R<AssignmentBatchPreviewVo> previewAssignmentBatch(@RequestBody ReviewAssignmentBo bo) {
        return R.ok(reviewService.previewAssignmentBatch(bo));
    }

    @SaCheckPermission("crehn:reviewAssignment:list")
    @Log(title = "art review assignment batch maintain", businessType = BusinessType.UPDATE)
    @PostMapping("/assignment/batch")
    public R<AssignmentBatchPreviewVo> maintainAssignmentBatch(@RequestBody ReviewAssignmentBo bo) {
        return R.ok(reviewService.maintainAssignmentBatch(bo));
    }

    @SaCheckPermission("crehn:reviewAssignment:list")
    @PostMapping("/assignment/scope-plan/preview")
    public R<AssignmentBatchPreviewVo> previewAssignmentScopePlan(@RequestBody ReviewAssignmentScopePlanBo bo) {
        return R.ok(reviewService.previewAssignmentScopePlan(bo));
    }

    @SaCheckPermission("crehn:reviewAssignment:list")
    @Log(title = "art review assignment scope plan", businessType = BusinessType.UPDATE)
    @PostMapping("/assignment/scope-plan")
    public R<AssignmentBatchPreviewVo> maintainAssignmentScopePlan(@RequestBody ReviewAssignmentScopePlanBo bo) {
        return R.ok(reviewService.maintainAssignmentScopePlan(bo));
    }

    @SaCheckPermission("crehn:reviewAssignment:edit")
    @Log(title = "art review assignment edit", businessType = BusinessType.UPDATE)
    @PutMapping("/assignment")
    public R<Void> updateAssignment(@RequestBody ReviewAssignmentBo bo) {
        reviewService.updateAssignment(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:reviewAssignment:remove")
    @Log(title = "art review assignment remove", businessType = BusinessType.DELETE)
    @DeleteMapping("/assignment/{ids}")
    public R<Void> deleteAssignment(@PathVariable Long[] ids) {
        reviewService.deleteAssignments(ids);
        return R.ok();
    }

    @SaCheckPermission("crehn:reviewAssignment:list")
    @GetMapping("/reviewer/options")
    public R<List<SysUserVo>> reviewerOptions(String keyword, Long activityId, Long categoryId, Long includeUserId) {
        return R.ok(reviewService.reviewerOptions(keyword, activityId, categoryId, includeUserId));
    }

    @SaCheckPermission("crehn:reviewAssignment:list")
    @GetMapping("/project/options")
    public R<List<ProjectVo>> projectOptions(Long activityId, Long categoryId, String keyword) {
        return R.ok(reviewService.approvedProjectOptions(activityId, categoryId, keyword));
    }

    @SaCheckPermission("crehn:reviewAssignment:list")
    @GetMapping("/assignment/school/options")
    public R<List<SchoolInfoVo>> assignmentSchoolOptions(String keyword) {
        return R.ok(reviewService.assignmentSchoolOptions(keyword));
    }

    @SaCheckPermission("crehn:reviewAssignment:list")
    @GetMapping("/assignment/filter-fields/{categoryId}")
    public R<List<CategoryFieldSchemaVo>> assignmentFilterFields(@PathVariable Long categoryId) {
        return R.ok(reviewService.assignmentFilterFields(categoryId));
    }

    @SaCheckPermission("crehn:reviewAssignment:list")
    @GetMapping("/assignment/category-config/{categoryId}")
    public R<ReviewAssignmentCategoryConfigVo> assignmentCategoryConfig(@PathVariable Long categoryId) {
        return R.ok(reviewService.getAssignmentCategoryConfig(categoryId));
    }

    @SaCheckPermission("crehn:reviewAssignment:edit")
    @Log(title = "art review assignment category config", businessType = BusinessType.UPDATE)
    @PutMapping("/assignment/category-config/{categoryId}")
    public R<ReviewAssignmentCategoryConfigVo> saveAssignmentCategoryConfig(
        @PathVariable Long categoryId,
        @RequestBody ReviewAssignmentCategoryConfigVo config
    ) {
        return R.ok(reviewService.saveAssignmentCategoryConfig(categoryId, config));
    }

    @SaCheckPermission("crehn:reviewAssignment:list")
    @GetMapping("/assignment/project/list")
    public TableDataInfo<ReviewProjectOverviewVo> assignmentProjectList(ReviewProjectOverviewVo query, PageQuery pageQuery) {
        return reviewService.queryAssignmentProjectPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:review:task")
    @GetMapping("/task/my-list")
    public TableDataInfo<ReviewTaskVo> myTasks(ReviewTaskVo query, PageQuery pageQuery) {
        return reviewService.queryMyTaskPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:review:task")
    @GetMapping("/task/navigator")
    public R<ReviewTaskNavigatorVo> myTaskNavigator(ReviewTaskVo query) {
        return R.ok(reviewService.queryMyTaskNavigator(query));
    }

    @SaCheckPermission("crehn:review:task")
    @GetMapping("/workbench")
    public R<ReviewWorkbenchVo> workbench() {
        return R.ok(reviewService.workbench());
    }

    @SaCheckPermission("crehn:review:task")
    @GetMapping("/activity/options")
    public R<List<ActivityVo>> activityOptions() {
        return R.ok(reviewService.activityOptions());
    }

    @SaCheckPermission("crehn:review:task")
    @GetMapping("/category/options/{activityId}")
    public R<List<ActivityCategoryVo>> categoryOptions(@PathVariable Long activityId) {
        return R.ok(reviewService.categoryOptions(activityId));
    }

    @SaCheckPermission("crehn:review:task")
    @GetMapping("/task/{assignmentId}/{projectId}")
    public R<ReviewTaskVo> taskDetail(@PathVariable Long assignmentId, @PathVariable Long projectId) {
        return R.ok(reviewService.getMyTaskDetail(assignmentId, projectId));
    }

    @SaCheckPermission("crehn:review:score")
    @Log(title = "art review score draft", businessType = BusinessType.UPDATE)
    @PostMapping("/score/draft")
    public R<ReviewScoreVo> saveScoreDraft(@RequestBody ReviewScoreBo bo) {
        return R.ok(reviewService.saveScoreDraft(bo));
    }

    @SaCheckPermission("crehn:review:score")
    @Log(title = "art review score submit", businessType = BusinessType.UPDATE)
    @PostMapping("/score/submit")
    public R<ReviewScoreVo> submitScore(@RequestBody ReviewScoreBo bo) {
        return R.ok(reviewService.submitScore(bo));
    }

    @SaCheckPermission("crehn:result:score")
    @GetMapping("/score/project/{projectId}")
    public R<List<ReviewScoreVo>> projectScores(@PathVariable Long projectId) {
        return R.ok(reviewService.listProjectScores(projectId));
    }

    @SaCheckPermission("crehn:result:score:edit")
    @Log(title = "art review score adjust", businessType = BusinessType.UPDATE)
    @PostMapping("/score/adjust")
    public R<ReviewScoreVo> adjustScore(@RequestBody ReviewScoreAdminBo bo) {
        return R.ok(reviewService.adjustScore(bo));
    }

    @SaCheckPermission("crehn:result:score:edit")
    @Log(title = "art review score return", businessType = BusinessType.UPDATE)
    @PostMapping("/score/return")
    public R<ReviewScoreVo> returnScore(@RequestBody ReviewScoreAdminBo bo) {
        return R.ok(reviewService.returnScore(bo));
    }
}
