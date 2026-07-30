package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("art_user_message")
public class UserMessage extends TenantEntity {
    @TableId(value = "id")
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
    @TableLogic
    private String delFlag;
}
