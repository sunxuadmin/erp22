package org.dromara.crehn.result.service;

import org.dromara.crehn.domain.bo.ReviewResultBo;
import org.dromara.crehn.domain.bo.ShowcaseOrderBo;
import org.dromara.crehn.domain.vo.ProjectUploadExportVo;
import org.dromara.crehn.domain.vo.ProjectUploadOverviewVo;
import org.dromara.crehn.domain.vo.ProjectQuotaRatioSummaryVo;
import org.dromara.crehn.domain.vo.ProjectUploadSummaryVo;
import org.dromara.crehn.domain.bo.ReviewAwardRuleBo;
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
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface IArtResultService {
    TableDataInfo<ReviewResultVo> queryResultPage(ReviewResultVo query, PageQuery pageQuery);

    TableDataInfo<ReviewResultVo> queryMyPublishedResultPage(ReviewResultVo query, PageQuery pageQuery);

    ReviewResultReadinessVo checkReadiness(ReviewResultBo bo);

    void generate(ReviewResultBo bo);

    void withdrawGenerated(ReviewResultBo bo);

    void publish(ReviewResultBo bo);

    void unpublish(ReviewResultBo bo);

    List<ReviewAwardRuleVo> queryAwardRules(Long activityId, Long categoryId);

    void saveAwardRules(ReviewAwardRuleBo bo);

    List<ReviewResultLogVo> queryLogs(ReviewResultLogVo query);

    List<ReviewResultVo> queryExportList(ReviewResultVo query);

    ProjectUploadSummaryVo queryUploadSummary(ReviewResultVo query);

    ProjectUploadOverviewVo queryUploadOverview(ReviewResultVo query);

    ProjectQuotaRatioSummaryVo queryQuotaRatioSummary(ReviewResultVo query);

    List<ProjectUploadExportVo> queryUploadExportList(ReviewResultVo query);

    TableDataInfo<ReviewProjectOverviewVo> queryProjectOverviewPage(ReviewProjectOverviewVo query, PageQuery pageQuery);

    List<ShowcaseOrderVo> queryShowcaseOrders(ShowcaseOrderVo query);

    void saveShowcaseOrders(ShowcaseOrderBo bo);

    /** Read-only administrator score summary, including current score rows. */
    TableDataInfo<ReviewScoreSummaryVo> queryScoreSummaryPage(ReviewScoreSummaryQueryVo query, PageQuery pageQuery);

    ReviewScoreSummaryFilterOptionsVo queryScoreSummaryFilterOptions(Long activityId, Long categoryId);

    void exportScoreSummary(ReviewScoreSummaryQueryVo query, PageQuery pageQuery, String scope,
                            String columnsJson, HttpServletResponse response);

    /** Read-only project score summary detail. */
    ReviewScoreSummaryDetailVo getScoreSummaryDetail(Long projectId);
}
