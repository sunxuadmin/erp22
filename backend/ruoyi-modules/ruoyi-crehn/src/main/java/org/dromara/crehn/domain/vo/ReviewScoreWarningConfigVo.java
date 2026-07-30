package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.ReviewScoreWarningConfig;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@AutoMapper(target = ReviewScoreWarningConfig.class)
public class ReviewScoreWarningConfigVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long activityId;
    private Long categoryId;
    private Boolean enabled;
    private String formulaType;
    private String formulaExpression;
    private BigDecimal thresholdPercent;
    private String normalText;
    private String warningText;
    private String insufficientText;
    private String unsupportedText;
    private String columnsJson;
    private Integer scoreColumnCount;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
