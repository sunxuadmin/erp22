package org.dromara.crehn.result.service.publish;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.ReviewResult;
import org.dromara.crehn.domain.bo.ReviewResultBo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ActivityMapper;
import org.dromara.crehn.mapper.ReviewResultMapper;
import org.dromara.crehn.notice.service.IArtUserMessageService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * ART-OWNER: BE.RESULT.PUBLISH
 *
 * Keeps publish/unpublish mutations beside their notification side effect while
 * result calculation remains in the independent generation service.
 */
@RequiredArgsConstructor
@Service
public class ReviewResultPublishService {

    private final ReviewResultMapper resultMapper;
    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final IArtUserMessageService userMessageService;

    public void publish(ReviewResultBo bo) {
        lockScope(bo);
        List<ReviewResult> results = resultMapper.selectList(Wrappers.lambdaQuery(ReviewResult.class)
            .eq(ReviewResult::getActivityId, bo.getActivityId())
            .eq(ReviewResult::getCategoryId, bo.getCategoryId())
            .last("for update"));
        if (results.isEmpty()) {
            throw new ServiceException("请先生成评审结果");
        }
        if (results.stream().anyMatch(result -> ArtReviewConstants.RESULT_PUBLISHED.equals(result.getResultStatus()))) {
            throw new ServiceException("当前活动类别结果已发布，不允许重复发布或修改已发布内容");
        }
        if (results.stream().anyMatch(result -> result.getScoreCount() == null || result.getScoreCount() <= 0
            || (result.getAverageScore() == null && StringUtils.isBlank(result.getFinalGrade())))) {
            throw new ServiceException("当前结果草案存在未完成汇总的作品，请重新生成并核对结果");
        }
        Date now = new Date();
        for (ReviewResult result : results) {
            result.setResultStatus(ArtReviewConstants.RESULT_PUBLISHED);
            result.setShowScore(Boolean.TRUE.equals(bo.getShowScore()));
            result.setShowRank(!Boolean.FALSE.equals(bo.getShowRank()));
            result.setShowComment(Boolean.TRUE.equals(bo.getShowComment()));
            result.setPublishedBy(LoginHelper.getUserId());
            result.setPublishedAt(now);
            result.setRemark(StringUtils.trim(bo.getRemark()));
            resultMapper.updateById(result);
        }
        if (!Boolean.FALSE.equals(bo.getNotifySchoolAccounts())) {
            userMessageService.sendResultPublishedMessages(results, LoginHelper.getUserId());
        }
    }

    public void unpublish(ReviewResultBo bo) {
        lockScope(bo);
        if (!LoginHelper.isSuperAdmin()) {
            throw new ServiceException("评审结果发布后原则上不可撤回，仅超级管理员可执行紧急下线");
        }
        if (StringUtils.isBlank(bo.getRemark())) {
            throw new ServiceException("紧急撤回发布必须填写原因");
        }
        List<ReviewResult> results = resultMapper.selectList(Wrappers.lambdaQuery(ReviewResult.class)
            .eq(ReviewResult::getActivityId, bo.getActivityId())
            .eq(ReviewResult::getCategoryId, bo.getCategoryId())
            .last("for update"));
        for (ReviewResult result : results) {
            result.setResultStatus(ArtReviewConstants.RESULT_DRAFT);
            result.setShowScore(false);
            result.setShowRank(true);
            result.setShowComment(false);
            result.setPublishedBy(null);
            result.setPublishedAt(null);
            result.setRemark(StringUtils.trim(bo.getRemark()));
            resultMapper.updateById(result);
        }
    }

    private void lockScope(ReviewResultBo bo) {
        requireScope(bo);
        categoryMapper.selectOne(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getId, bo.getCategoryId())
            .eq(ActivityCategory::getActivityId, bo.getActivityId())
            .last("for update"));
    }

    private void requireScope(ReviewResultBo bo) {
        if (bo.getActivityId() == null || bo.getCategoryId() == null) {
            throw new ServiceException("请选择活动和类别");
        }
        Activity activity = activityMapper.selectById(bo.getActivityId());
        ActivityCategory category = categoryMapper.selectById(bo.getCategoryId());
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        if (category == null || !Objects.equals(category.getActivityId(), bo.getActivityId())) {
            throw new ServiceException("类别不属于当前活动");
        }
    }
}
