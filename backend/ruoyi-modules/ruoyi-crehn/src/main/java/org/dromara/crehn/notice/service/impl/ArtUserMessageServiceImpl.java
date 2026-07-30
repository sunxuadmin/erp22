package org.dromara.crehn.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ReviewResult;
import org.dromara.crehn.domain.UserMessage;
import org.dromara.crehn.domain.vo.UserMessageVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ActivityMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.UserMessageMapper;
import org.dromara.crehn.notice.service.IArtUserMessageService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysUser;
import org.dromara.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ArtUserMessageServiceImpl implements IArtUserMessageService {

    private final UserMessageMapper userMessageMapper;
    private final SysUserMapper sysUserMapper;
    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final ProjectMapper projectMapper;

    @Override
    public TableDataInfo<UserMessageVo> queryMyPage(UserMessage query, PageQuery pageQuery) {
        LambdaQueryWrapper<UserMessage> lqw = Wrappers.lambdaQuery(UserMessage.class)
            .eq(UserMessage::getReceiverUserId, LoginHelper.getUserId())
            .eq(StringUtils.isNotBlank(query.getReadStatus()), UserMessage::getReadStatus, query.getReadStatus())
            .eq(StringUtils.isNotBlank(query.getMessageType()), UserMessage::getMessageType, query.getMessageType())
            .like(StringUtils.isNotBlank(query.getTitle()), UserMessage::getTitle, query.getTitle())
            .orderByDesc(UserMessage::getReadStatus)
            .orderByDesc(UserMessage::getSentAt)
            .orderByDesc(UserMessage::getCreateTime);
        Page<UserMessageVo> page = userMessageMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public long unreadCount() {
        return userMessageMapper.selectCount(Wrappers.lambdaQuery(UserMessage.class)
            .eq(UserMessage::getReceiverUserId, LoginHelper.getUserId())
            .eq(UserMessage::getReadStatus, ArtReviewConstants.MESSAGE_READ_UNREAD));
    }

    @Override
    public void markRead(Long id) {
        UserMessage message = requireMyMessage(id);
        if (ArtReviewConstants.MESSAGE_READ_READ.equals(message.getReadStatus())) {
            return;
        }
        message.setReadStatus(ArtReviewConstants.MESSAGE_READ_READ);
        message.setReadTime(new Date());
        userMessageMapper.updateById(message);
    }

    @Override
    public void markAllRead() {
        UserMessage update = new UserMessage();
        update.setReadStatus(ArtReviewConstants.MESSAGE_READ_READ);
        update.setReadTime(new Date());
        userMessageMapper.update(update, Wrappers.lambdaUpdate(UserMessage.class)
            .eq(UserMessage::getReceiverUserId, LoginHelper.getUserId())
            .eq(UserMessage::getReadStatus, ArtReviewConstants.MESSAGE_READ_UNREAD));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendAuditMessage(Project project, String result, String opinion, Long senderUserId) {
        if (project == null || project.getSchoolId() == null) {
            return;
        }
        List<SysUser> receivers = sysUserMapper.selectList(Wrappers.lambdaQuery(SysUser.class)
            .eq(SysUser::getUserType, ArtReviewConstants.USER_TYPE_SCHOOL)
            .eq(SysUser::getSchoolId, project.getSchoolId())
            .eq(SysUser::getSchoolReviewStatus, ArtReviewConstants.SCHOOL_REVIEW_ENABLED)
            .eq(SysUser::getStatus, "0"));
        if (receivers.isEmpty()) {
            return;
        }

        Activity activity = project.getActivityId() == null ? null : activityMapper.selectById(project.getActivityId());
        ActivityCategory category = project.getCategoryId() == null ? null : categoryMapper.selectById(project.getCategoryId());
        String title = auditTitle(result);
        String content = buildAuditContent(project, activity, category, result, opinion);
        Date now = new Date();
        for (SysUser receiver : receivers) {
            UserMessage message = new UserMessage();
            message.setReceiverUserId(receiver.getUserId());
            message.setSenderUserId(senderUserId);
            message.setSchoolId(project.getSchoolId());
            message.setActivityId(project.getActivityId());
            message.setCategoryId(project.getCategoryId());
            message.setProjectId(project.getId());
            message.setMessageType(ArtReviewConstants.MESSAGE_TYPE_AUDIT);
            message.setSourceType("project_audit");
            message.setSourceId(project.getId());
            message.setTitle(title);
            message.setContent(content);
            message.setReadStatus(ArtReviewConstants.MESSAGE_READ_UNREAD);
            message.setSentAt(now);
            userMessageMapper.insert(message);
        }
    }

    @Override
    public void sendSchoolReviewMessage(Project project, String result, String opinion, Long senderUserId) {
        if (project == null || project.getParticipantUserId() == null) {
            return;
        }
        String title = "pass".equals(result) ? "学校审核已推荐" : "学校审核已退回";
        StringBuilder content = new StringBuilder()
            .append("作品名称：").append(StringUtils.blankToDefault(project.getProjectName(), "-")).append('\n')
            .append("学校审核结果：").append(title).append('\n');
        if (StringUtils.isNotBlank(opinion)) {
            content.append("审核意见：").append(opinion.trim()).append('\n');
        }
        content.append("请进入“我的项目”查看作品状态。");
        insertParticipantMessage(project, senderUserId, "school_review", project.getId(), title, content.toString());
    }

    @Override
    public void sendSchoolFinalSubmitMessage(Project project, Long batchId, Long senderUserId) {
        if (project == null || project.getParticipantUserId() == null || batchId == null) {
            return;
        }
        String title = "学校已完成最终提交";
        String content = "作品名称：" + StringUtils.blankToDefault(project.getProjectName(), "-") + '\n'
            + "学校已将该作品纳入最终提交批次，提交后数据以系统快照为准。";
        insertParticipantMessage(project, senderUserId, "school_final_submit", batchId, title, content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendResultPublishedMessages(List<ReviewResult> results, Long senderUserId) {
        if (results == null || results.isEmpty()) {
            return;
        }
        Date now = new Date();
        for (ReviewResult result : results) {
            if (result == null || result.getId() == null || result.getSchoolId() == null) {
                continue;
            }
            List<SysUser> receivers = sysUserMapper.selectList(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getUserType, ArtReviewConstants.USER_TYPE_SCHOOL)
                .eq(SysUser::getSchoolId, result.getSchoolId())
                .eq(SysUser::getSchoolReviewStatus, ArtReviewConstants.SCHOOL_REVIEW_ENABLED)
                .eq(SysUser::getStatus, "0"));
            if (receivers.isEmpty()) {
                continue;
            }
            Activity activity = result.getActivityId() == null ? null : activityMapper.selectById(result.getActivityId());
            ActivityCategory category = result.getCategoryId() == null ? null : categoryMapper.selectById(result.getCategoryId());
            Project project = result.getProjectId() == null ? null : projectMapper.selectById(result.getProjectId());
            String title = "评审结果已发布";
            String content = buildResultPublishContent(result, project, activity, category);
            for (SysUser receiver : receivers) {
                if (hasResultPublishMessage(receiver.getUserId(), result.getId())) {
                    continue;
                }
                UserMessage message = new UserMessage();
                message.setReceiverUserId(receiver.getUserId());
                message.setSenderUserId(senderUserId);
                message.setSchoolId(result.getSchoolId());
                message.setActivityId(result.getActivityId());
                message.setCategoryId(result.getCategoryId());
                message.setProjectId(result.getProjectId());
                message.setMessageType(ArtReviewConstants.MESSAGE_TYPE_RESULT);
                message.setSourceType("result_publish");
                message.setSourceId(result.getId());
                message.setTitle(title);
                message.setContent(content);
                message.setReadStatus(ArtReviewConstants.MESSAGE_READ_UNREAD);
                message.setSentAt(now);
                userMessageMapper.insert(message);
            }
        }
    }

    private UserMessage requireMyMessage(Long id) {
        UserMessage message = userMessageMapper.selectOne(Wrappers.lambdaQuery(UserMessage.class)
            .eq(UserMessage::getId, id)
            .eq(UserMessage::getReceiverUserId, LoginHelper.getUserId()));
        if (message == null) {
            throw new ServiceException("消息不存在或无权访问");
        }
        return message;
    }

    private void insertParticipantMessage(
        Project project,
        Long senderUserId,
        String sourceType,
        Long sourceId,
        String title,
        String content
    ) {
        UserMessage message = new UserMessage();
        message.setReceiverUserId(project.getParticipantUserId());
        message.setSenderUserId(senderUserId);
        message.setSchoolId(project.getSchoolId());
        message.setActivityId(project.getActivityId());
        message.setCategoryId(project.getCategoryId());
        message.setProjectId(project.getId());
        message.setMessageType(ArtReviewConstants.MESSAGE_TYPE_SCHOOL_REVIEW);
        message.setSourceType(sourceType);
        message.setSourceId(sourceId);
        message.setTitle(title);
        message.setContent(content);
        message.setReadStatus(ArtReviewConstants.MESSAGE_READ_UNREAD);
        message.setSentAt(new Date());
        userMessageMapper.insert(message);
    }

    private String auditTitle(String result) {
        return switch (result) {
            case ArtReviewConstants.AUDIT_PASS -> "项目审核通过";
            case ArtReviewConstants.AUDIT_RETURN -> "项目审核退回";
            case ArtReviewConstants.AUDIT_WITHDRAW_RETURN -> "项目撤销退回";
            case ArtReviewConstants.AUDIT_WITHDRAW_PASS -> "项目撤销通过";
            default -> "项目审核通知";
        };
    }

    private boolean hasResultPublishMessage(Long receiverUserId, Long resultId) {
        return userMessageMapper.selectCount(Wrappers.lambdaQuery(UserMessage.class)
            .eq(UserMessage::getReceiverUserId, receiverUserId)
            .eq(UserMessage::getSourceType, "result_publish")
            .eq(UserMessage::getSourceId, resultId)) > 0;
    }

    private String buildResultPublishContent(ReviewResult result, Project project, Activity activity, ActivityCategory category) {
        StringBuilder content = new StringBuilder();
        content.append("作品名称：").append(project == null ? "-" : StringUtils.blankToDefault(project.getProjectName(), "-")).append('\n');
        content.append("活动：").append(activity == null ? "-" : StringUtils.blankToDefault(activity.getActivityName(), "-")).append('\n');
        content.append("类别：").append(category == null ? "-" : StringUtils.blankToDefault(category.getCategoryName(), "-")).append('\n');
        if (result.getRankNo() != null) {
            content.append("排名：").append(result.getRankNo()).append('\n');
        }
        if (StringUtils.isNotBlank(result.getFinalGrade())) {
            content.append("结果：").append(result.getFinalGrade()).append('\n');
        }
        content.append("评审结果已由管理员发布，请以本通知为准。");
        return content.toString();
    }

    private String buildAuditContent(Project project, Activity activity, ActivityCategory category, String result, String opinion) {
        StringBuilder content = new StringBuilder();
        content.append("项目名称：").append(StringUtils.blankToDefault(project.getProjectName(), "-")).append('\n');
        content.append("活动：").append(activity == null ? "-" : StringUtils.blankToDefault(activity.getActivityName(), "-")).append('\n');
        content.append("类别：").append(category == null ? "-" : StringUtils.blankToDefault(category.getCategoryName(), "-")).append('\n');
        content.append("审核结果：").append(auditTitle(result)).append('\n');
        if (StringUtils.isNotBlank(opinion)) {
            content.append("审核意见：").append(opinion.trim()).append('\n');
        }
        content.append("请进入“我的项目”查看项目状态。");
        return content.toString();
    }
}
