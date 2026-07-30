package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.ReviewAssignment;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@AutoMapper(target = ReviewAssignment.class)
public class ReviewAssignmentVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long activityId;
    private String activityName;
    private Long categoryId;
    private List<Long> categoryIds;
    private String categoryName;
    private String projectIdsJson;
    private String excludedProjectIdsJson;
    private Long reviewerUserId;
    private List<Long> reviewerUserIds;
    private String reviewerUserName;
    private String reviewerNickName;
    private String batchOperation;
    private String batchTarget;
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
    private Date createTime;
    private Long projectCount;
    private Long submittedCount;
}
