package org.dromara.crehn.domain.bo;

import lombok.Data;

import java.util.List;

@Data
public class ReviewAwardRuleBo {
    private Long activityId;
    private Long categoryId;
    private List<ReviewAwardRuleItemBo> rules;
}
