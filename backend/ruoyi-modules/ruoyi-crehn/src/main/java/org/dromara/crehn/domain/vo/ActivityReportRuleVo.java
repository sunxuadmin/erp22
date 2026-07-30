package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.ActivityReportRule;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@AutoMapper(target = ActivityReportRule.class)
public class ActivityReportRuleVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long activityId;
    private Long packageId;
    private Long packageItemId;
    private String ruleCode;
    private String ruleName;
    private String ruleGroup;
    private String ruleType;
    private String scopeType;
    private String scopeCategoryGroup;
    private String categoryCode;
    private Long categoryId;
    private String schoolType;
    private Long schoolId;
    private String targetFieldKey;
    private String targetValue;
    private String operator;
    private Integer limitCount;
    private BigDecimal ratioValue;
    private Integer minValue;
    private Integer maxValue;
    private String enforceMode;
    private String message;
    private String ruleJson;
    private Boolean enabled;
    private Integer sortOrder;
    private String remark;
    private Date createTime;
}
