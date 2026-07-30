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
@TableName("audit_assignment")
public class AuditAssignment extends TenantEntity {
    @TableId(value = "id")
    private Long id;
    private Long activityId;
    private Long categoryId;
    private Long auditorUserId;
    private String schoolScopeMode;
    private String schoolIdsJson;
    private Boolean allowQuery;
    private Boolean allowPass;
    private Boolean allowReturn;
    private Boolean allowWithdrawPass;
    private Boolean allowWithdrawReturn;
    private Boolean allowDownload;
    private String status;
    private Long assignedBy;
    private Date assignedAt;
    @TableLogic
    private String delFlag;
}
