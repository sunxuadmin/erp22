package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.ActivityCategory;

import java.io.Serial;
import java.io.Serializable;

@Data
@AutoMapper(target = ActivityCategory.class)
public class ActivityCategoryVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long activityId;
    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private Integer quotaLimit;
    private String categoryGroup;
    private String ruleJson;
    private String tipText;
    private String checkMode;
    private Integer sortOrder;
    private Boolean enabled;
    private String delFlag;
}
