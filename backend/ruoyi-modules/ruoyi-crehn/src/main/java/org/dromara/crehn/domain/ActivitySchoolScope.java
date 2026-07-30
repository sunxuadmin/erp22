package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activity_school_scope")
public class ActivitySchoolScope extends TenantEntity {
    @TableId
    private Long id;
    private Long activityId;
    private Long schoolId;
    private Boolean enabled;
    private Long assignedBy;
    private Date assignedAt;
    private String remark;
    @TableLogic
    private String delFlag;
}
