package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

/**
 * Activity/category scoped score-discrepancy warning configuration.
 *
 * <p>This is deliberately separate from reviewer assignments. A category can
 * have several assignments, while one warning rule must be unambiguous for
 * the result summary.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review_score_warning_config")
public class ReviewScoreWarningConfig extends TenantEntity {
    @TableId
    private Long id;
    private Long activityId;
    private Long categoryId;
    private Boolean enabled;
    /** Supported values are range_average, range_max and range_min. */
    private String formulaType;
    /** Restricted arithmetic expression used when formulaType is custom. */
    private String formulaExpression;
    private BigDecimal thresholdPercent;
    private String normalText;
    private String warningText;
    private String insufficientText;
    private String unsupportedText;
    private String columnsJson;
    private Integer scoreColumnCount;
    private String remark;
    @TableLogic
    private String delFlag;
}
