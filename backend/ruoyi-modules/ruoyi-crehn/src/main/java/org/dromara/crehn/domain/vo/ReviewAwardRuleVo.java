package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.ReviewAwardRule;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@AutoMapper(target = ReviewAwardRule.class)
public class ReviewAwardRuleVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long activityId;
    private Long categoryId;
    private String awardLevel;
    private String ruleType;
    private Integer minRank;
    private Integer maxRank;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private Integer sortOrder;
    private Boolean enabled;
    private String remark;
    private Date createTime;
}
