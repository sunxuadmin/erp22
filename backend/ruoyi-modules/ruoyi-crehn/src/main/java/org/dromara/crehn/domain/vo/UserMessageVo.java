package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.UserMessage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = UserMessage.class)
public class UserMessageVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long receiverUserId;
    private Long senderUserId;
    private Long schoolId;
    private Long activityId;
    private Long categoryId;
    private Long projectId;
    private String messageType;
    private String sourceType;
    private Long sourceId;
    private String title;
    private String content;
    private String readStatus;
    private Date readTime;
    private Date sentAt;
    private Date createTime;
}
