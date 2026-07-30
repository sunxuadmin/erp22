package org.dromara.crehn.domain.bo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed assignment plan. The server resolves unit keys from category
 * configuration and never accepts a client-built project filter.
 */
@Data
public class ReviewAssignmentScopePlanBo {
    private Long activityId;
    private List<Long> reviewerUserIds = new ArrayList<>();
    private String operation;
    private List<ReviewAssignmentScopePlanItemBo> scopes = new ArrayList<>();
    private String schoolScopeMode;
    private String schoolIdsJson;
    private String scoreMode;
    private String scoreRuleJson;
    private String exclusiveMode;
    private String scoreVisibilityPolicy;
    private Boolean hideSchoolInfo;
    private Boolean hideMemberInfo;
    private String hiddenFieldKeysJson;
    private String status;
}
