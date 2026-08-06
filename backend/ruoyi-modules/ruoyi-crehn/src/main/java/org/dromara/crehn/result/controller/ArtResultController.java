package org.dromara.crehn.result.controller;

import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.bo.ReviewAwardRuleBo;
import org.dromara.crehn.domain.bo.ReviewResultBo;
import org.dromara.crehn.domain.bo.ShowcaseOrderBo;
import org.dromara.crehn.domain.bo.ReviewScoreWarningConfigBo;
import org.dromara.crehn.domain.vo.ProjectUploadExportVo;
import org.dromara.crehn.domain.vo.PublicReviewResultVo;
import org.dromara.crehn.domain.vo.ProjectUploadOverviewVo;
import org.dromara.crehn.domain.vo.ProjectQuotaRatioSummaryVo;
import org.dromara.crehn.domain.vo.ProjectUploadSummaryVo;
import org.dromara.crehn.domain.vo.ReviewAwardRuleVo;
import org.dromara.crehn.domain.vo.ReviewProjectOverviewVo;
import org.dromara.crehn.domain.vo.ReviewResultLogVo;
import org.dromara.crehn.domain.vo.ReviewResultReadinessVo;
import org.dromara.crehn.domain.vo.ReviewResultVo;
import org.dromara.crehn.domain.vo.ReviewScoreSummaryDetailVo;
import org.dromara.crehn.domain.vo.ReviewScoreSummaryFilterOptionsVo;
import org.dromara.crehn.domain.vo.ReviewScoreSummaryQueryVo;
import org.dromara.crehn.domain.vo.ReviewScoreSummaryVo;
import org.dromara.crehn.domain.vo.ShowcaseOrderVo;
import org.dromara.crehn.domain.vo.ReviewScoreWarningConfigVo;
import org.dromara.crehn.result.service.IArtResultService;
import org.dromara.crehn.result.service.warning.ReviewScoreWarningService;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.ratelimiter.enums.LimitType;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/result")
public class ArtResultController extends BaseController {

    private final IArtResultService resultService;
    private final ReviewScoreWarningService scoreWarningService;

    @SaCheckPermission("crehn:result:list")
    @GetMapping("/list")
    public TableDataInfo<ReviewResultVo> list(ReviewResultVo query, PageQuery pageQuery) {
        return resultService.queryResultPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:result:school")
    @GetMapping("/my-list")
    public TableDataInfo<ReviewResultVo> myList(ReviewResultVo query, PageQuery pageQuery) {
        return resultService.queryMyPublishedResultPage(query, pageQuery);
    }

    @SaIgnore
    @RateLimiter(time = 60, count = 60, limitType = LimitType.IP)
    @GetMapping("/public/published")
    public TableDataInfo<PublicReviewResultVo> publicPublished(PageQuery pageQuery) {
        return resultService.queryPublicPublishedResultPage(pageQuery);
    }

    @SaCheckPermission("crehn:result:generate")
    @GetMapping("/readiness")
    public R<ReviewResultReadinessVo> readiness(Long activityId, Long categoryId) {
        ReviewResultBo bo = new ReviewResultBo();
        bo.setActivityId(activityId);
        bo.setCategoryId(categoryId);
        return R.ok(resultService.checkReadiness(bo));
    }

    @SaCheckPermission("crehn:result:generate")
    @Log(title = "art review result generate", businessType = BusinessType.UPDATE)
    @PostMapping("/generate")
    public R<Void> generate(@RequestBody ReviewResultBo bo) {
        resultService.generate(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:result:generate")
    @Log(title = "art review generated result withdraw", businessType = BusinessType.UPDATE)
    @PostMapping("/generate/withdraw")
    public R<Void> withdrawGenerated(@RequestBody ReviewResultBo bo) {
        resultService.withdrawGenerated(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:result:publish")
    @Log(title = "art review result publish", businessType = BusinessType.UPDATE)
    @PostMapping("/publish")
    public R<Void> publish(@RequestBody ReviewResultBo bo) {
        resultService.publish(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:result:publish")
    @Log(title = "art review result unpublish", businessType = BusinessType.UPDATE)
    @PostMapping("/unpublish")
    public R<Void> unpublish(@RequestBody ReviewResultBo bo) {
        resultService.unpublish(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:result:rule")
    @GetMapping("/award-rule/list")
    public R<java.util.List<ReviewAwardRuleVo>> awardRuleList(Long activityId, Long categoryId) {
        return R.ok(resultService.queryAwardRules(activityId, categoryId));
    }

    @SaCheckPermission("crehn:result:rule")
    @Log(title = "art review award rule save", businessType = BusinessType.UPDATE)
    @PostMapping("/award-rule/save")
    public R<Void> saveAwardRules(@RequestBody ReviewAwardRuleBo bo) {
        resultService.saveAwardRules(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:result:log")
    @GetMapping("/log/list")
    public R<java.util.List<ReviewResultLogVo>> logs(ReviewResultLogVo query) {
        return R.ok(resultService.queryLogs(query));
    }

    @SaCheckPermission("crehn:result:export")
    @Log(title = "art review result export", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ReviewResultVo query, HttpServletResponse response) {
        ExcelUtil.exportExcel(resultService.queryExportList(query), "评审结果", ReviewResultVo.class, response);
    }

    @SaCheckPermission("crehn:result:summary")
    @GetMapping("/upload-summary")
    public R<ProjectUploadSummaryVo> uploadSummary(ReviewResultVo query) {
        return R.ok(resultService.queryUploadSummary(query));
    }

    @SaCheckPermission(value = {"crehn:result:summary", "crehn:audit:list", "crehn:projectView:list"}, mode = SaMode.OR)
    @GetMapping("/upload-overview")
    public R<ProjectUploadOverviewVo> uploadOverview(ReviewResultVo query) {
        return R.ok(resultService.queryUploadOverview(query));
    }

    @SaCheckPermission("crehn:result:summary")
    @GetMapping("/quota-ratio-summary")
    public R<ProjectQuotaRatioSummaryVo> quotaRatioSummary(ReviewResultVo query) {
        return R.ok(resultService.queryQuotaRatioSummary(query));
    }

    /** Activity/category score-discrepancy warning configuration. */
    @SaCheckPermission("crehn:reviewScoreSummary:config")
    @GetMapping("/score-warning-config")
    public R<ReviewScoreWarningConfigVo> scoreWarningConfig(Long activityId, Long categoryId) {
        return R.ok(scoreWarningService.getConfig(activityId, categoryId));
    }

    @SaCheckPermission("crehn:reviewScoreSummary:config")
    @Log(title = "art review score warning rule save", businessType = BusinessType.UPDATE)
    @PostMapping("/score-warning-config")
    public R<ReviewScoreWarningConfigVo> saveScoreWarningConfig(@RequestBody @Validated ReviewScoreWarningConfigBo bo) {
        return R.ok(scoreWarningService.saveConfig(bo));
    }

    @SaCheckPermission("crehn:result:export")
    @Log(title = "art project upload export", businessType = BusinessType.EXPORT)
    @PostMapping("/upload-export")
    public void uploadExport(ReviewResultVo query, HttpServletResponse response) {
        ExcelUtil.exportExcel(resultService.queryUploadExportList(query), "作品上传列表", ProjectUploadExportVo.class, response);
    }

    @SaCheckPermission("crehn:result:summary")
    @GetMapping("/project-overview/list")
    public TableDataInfo<ReviewProjectOverviewVo> projectOverviewList(ReviewProjectOverviewVo query, PageQuery pageQuery) {
        return resultService.queryProjectOverviewPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:result:summary")
    @GetMapping("/showcase-order/list")
    public R<java.util.List<ShowcaseOrderVo>> showcaseOrderList(ShowcaseOrderVo query) {
        return R.ok(resultService.queryShowcaseOrders(query));
    }

    @SaCheckPermission("crehn:result:rule")
    @Log(title = "art showcase order save", businessType = BusinessType.UPDATE)
    @PostMapping("/showcase-order/save")
    public R<Void> saveShowcaseOrders(@RequestBody ShowcaseOrderBo bo) {
        resultService.saveShowcaseOrders(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:result:export")
    @Log(title = "art showcase order export", businessType = BusinessType.EXPORT)
    @PostMapping("/showcase-order/export")
    public void showcaseOrderExport(ShowcaseOrderVo query, HttpServletResponse response) {
        ExcelUtil.exportExcel(resultService.queryShowcaseOrders(query), "\u5c55\u6f14\u987a\u5e8f\u8868", ShowcaseOrderVo.class, response);
    }

    @SaCheckPermission("crehn:reviewScoreSummary:list")
    @GetMapping("/score-summary/page")
    public TableDataInfo<ReviewScoreSummaryVo> scoreSummaryPage(ReviewScoreSummaryQueryVo query, PageQuery pageQuery) {
        return resultService.queryScoreSummaryPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:reviewScoreSummary:list")
    @GetMapping("/score-summary/options")
    public R<ReviewScoreSummaryFilterOptionsVo> scoreSummaryOptions(Long activityId, Long categoryId) {
        return R.ok(resultService.queryScoreSummaryFilterOptions(activityId, categoryId));
    }

    @SaCheckPermission("crehn:reviewScoreSummary:export")
    @Log(title = "art review score summary export", businessType = BusinessType.EXPORT)
    @PostMapping("/score-summary/export")
    public void scoreSummaryExport(ReviewScoreSummaryQueryVo query, PageQuery pageQuery, String scope,
                                   String columnsJson, HttpServletResponse response) {
        resultService.exportScoreSummary(query, pageQuery, scope, columnsJson, response);
    }

    @SaCheckPermission("crehn:reviewScoreSummary:detail")
    @GetMapping("/score-summary/{projectId}")
    public R<ReviewScoreSummaryDetailVo> scoreSummaryDetail(@PathVariable Long projectId) {
        return R.ok(resultService.getScoreSummaryDetail(projectId));
    }
}
