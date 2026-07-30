package org.dromara.crehn.domain.bo;

import lombok.Data;

@Data
public class ReportRuleApplyBo {
    private Long packageId;
    private Long activityId;
    private Boolean overwrite;
}
