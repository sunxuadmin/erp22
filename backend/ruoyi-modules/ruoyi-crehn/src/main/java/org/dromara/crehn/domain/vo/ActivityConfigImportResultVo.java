package org.dromara.crehn.domain.vo;

import lombok.Data;

@Data
public class ActivityConfigImportResultVo {

    private Long activityId;

    private String activityName;

    private Integer categoryCount;

    private Integer fieldCount;

    private Integer fileRequirementCount;
}
