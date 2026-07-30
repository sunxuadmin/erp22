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
@TableName("participant_activation_code")
public class ParticipantActivationCode extends TenantEntity {
    @TableId
    private Long id;
    private Long batchId;
    private Long participantId;
    private String codeHash;
    private String codeHint;
    private Date expiresAt;
    private Date activatedAt;
    private Integer failedAttempts;
    private String status;
    @TableLogic
    private String delFlag;
}
