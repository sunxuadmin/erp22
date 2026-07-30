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
@TableName("project_audit_record")
public class ProjectAuditRecord extends TenantEntity {
    @TableId(value = "id")
    private Long id;
    private Long projectId;
    private String fromStatus;
    private String toStatus;
    private String auditResult;
    private String opinion;
    private Long auditedBy;
    private Date auditedAt;
    @TableLogic
    private String delFlag;
}
