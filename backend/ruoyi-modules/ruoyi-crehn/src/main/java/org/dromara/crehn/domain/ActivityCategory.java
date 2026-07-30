package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity_category")
public class ActivityCategory extends TenantEntity {
    @TableId(value = "id")
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
    @TableLogic
    private String delFlag;
}
