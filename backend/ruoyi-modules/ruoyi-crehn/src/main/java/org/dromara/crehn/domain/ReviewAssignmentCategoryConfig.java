package org.dromara.crehn.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Category-level assignment configuration stored inside activity-category
 * rule_json under a dedicated reviewAssignmentConfig key.
 */
@Data
public class ReviewAssignmentCategoryConfig {
    private List<String> scopeFieldKeys = new ArrayList<>();
    private Boolean visibilityConfigured = false;
    private Boolean hideSchoolInfo = true;
    private Boolean hideMemberInfo = false;
    private String hiddenFieldKeysJson;
}
