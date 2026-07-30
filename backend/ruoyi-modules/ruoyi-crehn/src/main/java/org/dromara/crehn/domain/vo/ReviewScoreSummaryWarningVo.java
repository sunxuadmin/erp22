package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Server-calculated score spread warning. The score summary endpoint is
 * intentionally read-only; configuration is applied by the result service.
 */
@Data
public class ReviewScoreSummaryWarningVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String status;
    private String text;
    private BigDecimal spreadPercent;
    private BigDecimal thresholdPercent;
    private String formulaType;
    private String formulaExpression;
}
