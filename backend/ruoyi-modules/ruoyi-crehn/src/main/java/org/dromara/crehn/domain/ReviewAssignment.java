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
@TableName("review_assignment")
public class ReviewAssignment extends TenantEntity {
    @TableId(value = "id")
    private Long id;
    private Long activityId;
    private Long categoryId;
    private String projectIdsJson;
    private String excludedProjectIdsJson;
    private Long reviewerUserId;
    private String schoolScopeMode;
    private String schoolIdsJson;
    private String projectFilterJson;
    private String scoreMode;
    private String scoreRuleJson;
    private String exclusiveMode;
    private String scoreVisibilityPolicy;
    private Boolean hideSchoolInfo;
    private Boolean hideMemberInfo;
    private String hiddenFieldKeysJson;
    private String status;
    private Long assignedBy;
    private Date assignedAt;
    @TableLogic
    private String delFlag;
}
