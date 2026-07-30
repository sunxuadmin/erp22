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
@TableName("review_result_log")
public class ReviewResultLog extends TenantEntity {
    @TableId(value = "id")
    private Long id;
    private Long activityId;
    private Long categoryId;
    private Long projectId;
    private Long scoreId;
    private String targetType;
    private String actionType;
    private String beforeJson;
    private String afterJson;
    private String reason;
    private Long operatedBy;
    private Date operatedAt;
    @TableLogic
    private String delFlag;
}
