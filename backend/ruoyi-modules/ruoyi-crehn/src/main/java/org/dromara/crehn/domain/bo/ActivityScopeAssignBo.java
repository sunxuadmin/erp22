package org.dromara.crehn.domain.bo;

import lombok.Data;

import java.util.List;

@Data
public class ActivityScopeAssignBo {
    private Long activityId;
    private List<Long> schoolIds;
    private Boolean enabled;
    private String remark;
}
