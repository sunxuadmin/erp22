package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity_report_rule")
public class ActivityReportRule extends TenantEntity {
    @TableId
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
    @TableLogic
    private String delFlag;
}
