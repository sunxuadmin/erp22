package org.dromara.crehn.domain.bo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** Request body for an activity/category score warning rule. */
@Data
public class ReviewScoreWarningConfigBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "活动不能为空")
    private Long activityId;
    @NotNull(message = "类别不能为空")
    private Long categoryId;
    private Boolean enabled;
    private String formulaType;
    private String formulaExpression;
    @DecimalMin(value = "0", message = "阈值不能小于0")
    @DecimalMax(value = "10000", message = "阈值不能超过10000")
    private BigDecimal thresholdPercent;
    private String normalText;
    private String warningText;
    private String insufficientText;
    private String unsupportedText;
    private String columnsJson;
    private Integer scoreColumnCount;
    private String remark;
}
