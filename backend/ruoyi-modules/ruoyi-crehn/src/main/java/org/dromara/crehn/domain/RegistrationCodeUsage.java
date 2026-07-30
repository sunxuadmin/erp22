package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("registration_code_usage")
public class RegistrationCodeUsage extends TenantEntity {
    @TableId
    private Long id;
    private Long codeId;
    private Long userId;
    private Date usedAt;
    private String ipAddress;
    private String userAgent;
    private String boundAccount;
}
