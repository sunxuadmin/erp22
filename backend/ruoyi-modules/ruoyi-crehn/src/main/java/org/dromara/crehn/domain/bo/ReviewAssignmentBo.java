package org.dromara.crehn.domain.bo;

import lombok.Data;

import java.util.List;

@Data
public class ReviewAssignmentBo {
    private Long id;
    private Long activityId;
    private Long categoryId;
    private List<Long> categoryIds;
    private String projectIdsJson;
    private String excludedProjectIdsJson;
    private Long reviewerUserId;
    private List<Long> reviewerUserIds;
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
}
