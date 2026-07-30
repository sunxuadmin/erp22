package org.dromara.crehn.config.service.rule;

import lombok.Data;
import org.dromara.common.core.utils.StringUtils;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ActivityReportRuleRuntime {
    private Long id;
    private Long activityId;
    private Long categoryId;
    private Long schoolId;
    private String schoolType;
    private String ruleName;
    private String ruleGroup;
    private String ruleType;
    private String scopeType;
    private String scopeCategoryGroup;
    private String categoryCode;
    private String targetFieldKey;
    private String targetValue;
    private String displayName;
    private String operator;
    private Integer limitCount;
    private BigDecimal ratioValue;
    private String enforceMode;
    private String message;
    private String ruleJson;
    private Boolean enabled;
    private Integer sortOrder;
    private String remark;
    private Date createTime;

    public String normalizedRuleType() {
        if (StringUtils.isBlank(ruleType)) {
            return "count";
        }
        if ("quota_count".equals(ruleType) || "category_count".equals(ruleType) || "total_count".equals(ruleType)) {
            return "count";
        }
        return ruleType;
    }

    public boolean hasCountLimit() {
        String normalized = normalizedRuleType();
        return limitCount != null && limitCount >= 0
            && ("count".equals(normalized) || "mixed".equals(normalized) || StringUtils.isBlank(normalized));
    }

    public boolean hasRatioLimit() {
        String normalized = normalizedRuleType();
        return ratioValue != null && ("ratio".equals(normalized) || "mixed".equals(normalized));
    }

    public String displayMessage() {
        return StringUtils.blankToDefault(message, remark);
    }

    public String label() {
        if (StringUtils.isNotBlank(displayMessage())) {
            return displayMessage();
        }
        if (StringUtils.isNotBlank(displayName)) {
            return displayName;
        }
        if (StringUtils.isNotBlank(targetValue)) {
            return targetValue;
        }
        return "activity report rule";
    }
}
