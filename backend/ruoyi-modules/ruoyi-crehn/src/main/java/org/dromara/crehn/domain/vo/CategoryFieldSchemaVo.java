package org.dromara.crehn.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.CategoryFieldSchema;

import java.io.Serial;
import java.io.Serializable;

@Data
@AutoMapper(target = CategoryFieldSchema.class)
public class CategoryFieldSchemaVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long categoryId;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private Boolean required;
    private String optionsJson;
    private String validationJson;
    @JsonProperty("sensitive")
    private Boolean sensitiveFlag;
    private Integer sortOrder;
    private String delFlag;
}
