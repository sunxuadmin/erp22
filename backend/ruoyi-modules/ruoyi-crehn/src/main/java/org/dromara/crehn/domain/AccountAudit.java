package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("account_audit")
public class AccountAudit extends TenantEntity {
    @TableId
    private Long id;
    private Long actorUserId;
    private Long targetUserId;
    private String action;
    private String reason;
    private String ipAddress;
    private String metadataJson;
}
