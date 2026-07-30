package org.dromara.crehn.domain.bo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReviewAwardRuleItemBo {
    private Long id;
    private String awardLevel;
    private String ruleType;
    private Integer minRank;
    private Integer maxRank;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private Integer sortOrder;
    private Boolean enabled;
    private String remark;
}
