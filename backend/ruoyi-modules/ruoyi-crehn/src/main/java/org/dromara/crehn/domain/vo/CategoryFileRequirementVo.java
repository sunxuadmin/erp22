package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.CategoryFileRequirement;

import java.io.Serial;
import java.io.Serializable;

@Data
@AutoMapper(target = CategoryFileRequirement.class)
public class CategoryFileRequirementVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long categoryId;
    private String fileTypeCode;
    private String fileTypeName;
    private String allowedExt;
    private Integer maxSizeMb;
    private Integer minCount;
    private Integer maxCount;
    private Boolean required;
    private String ruleJson;
    private String tipText;
    private Integer sortOrder;
    private String delFlag;
}
