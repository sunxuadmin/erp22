package org.dromara.crehn.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.SchoolFieldSchema;

import java.io.Serial;
import java.io.Serializable;

@Data
@AutoMapper(target = SchoolFieldSchema.class)
public class SchoolFieldSchemaVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private Boolean required;
    private String optionsJson;
    private String validationJson;
    @JsonProperty("sensitive")
    private Boolean sensitiveFlag;
    private Boolean enabled;
    private Integer sortOrder;
}
