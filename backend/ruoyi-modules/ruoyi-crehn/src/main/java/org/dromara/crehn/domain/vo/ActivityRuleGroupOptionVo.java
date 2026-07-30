package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class ActivityRuleGroupOptionVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long activityId;
    private Long categoryId;
    private Long schoolId;
    private String schoolType;
    private String groupCode;
    private String groupName;
    private String ruleType;
    private Integer limitCount;
    private BigDecimal ratioValue;
    private String ruleJson;
    private Boolean enabled;
    private String remark;
    private Date createTime;
}
