package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category_file_requirement")
public class CategoryFileRequirement extends TenantEntity {
    @TableId(value = "id")
    private Long id;
    private Long categoryId;
    private String fileTypeCode;
    private String fileTypeName;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String allowedExt;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer maxSizeMb;
    private Integer minCount;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer maxCount;
    private Boolean required;
    private String ruleJson;
    private String tipText;
    private Integer sortOrder;
    @TableLogic
    private String delFlag;
}
