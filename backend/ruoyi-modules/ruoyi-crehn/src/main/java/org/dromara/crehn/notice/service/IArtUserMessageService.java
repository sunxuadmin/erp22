package org.dromara.crehn.notice.service;

import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ReviewResult;
import org.dromara.crehn.domain.UserMessage;
import org.dromara.crehn.domain.vo.UserMessageVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

public interface IArtUserMessageService {
    TableDataInfo<UserMessageVo> queryMyPage(UserMessage query, PageQuery pageQuery);

    long unreadCount();

    void markRead(Long id);

    void markAllRead();

    void sendAuditMessage(Project project, String result, String opinion, Long senderUserId);

    void sendSchoolReviewMessage(Project project, String result, String opinion, Long senderUserId);

    void sendSchoolFinalSubmitMessage(Project project, Long batchId, Long senderUserId);

    void sendResultPublishedMessages(java.util.List<ReviewResult> results, Long senderUserId);
}
