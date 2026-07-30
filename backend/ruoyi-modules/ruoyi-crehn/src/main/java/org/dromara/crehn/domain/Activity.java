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
@TableName("activity")
public class Activity extends TenantEntity {
    @TableId(value = "id")
    private Long id;
    private String activityName;
    private String menuName;
    private String edition;
    private Integer year;
    private String organizer;
    private String undertaker;
    private Date signupStartAt;
    private Date signupEndAt;
    private String scopeType;
    private String description;
    private String status;
    @TableLogic
    private String delFlag;
}
