package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category_field_schema")
public class CategoryFieldSchema extends TenantEntity {
    @TableId(value = "id")
    private Long id;
    private Long categoryId;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private Boolean required;
    private String optionsJson;
    private String validationJson;
    @TableField("sensitive_flag")
    @JsonProperty("sensitive")
    private Boolean sensitiveFlag;
    private Integer sortOrder;
    @TableLogic
    private String delFlag;
}
