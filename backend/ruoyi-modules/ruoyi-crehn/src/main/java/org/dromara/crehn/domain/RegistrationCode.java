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
@TableName("registration_code")
public class RegistrationCode extends TenantEntity {
    @TableId
    private Long id;
    private String code;
    private String codeType;
    private Long activityId;
    private Long schoolId;
    private String boundPhone;
    private String roleKey;
    private Boolean autoApprove;
    private Integer maxUseCount;
    private Integer usedCount;
    private Date expireAt;
    private String status;
    private String remark;
    @TableLogic
    private String delFlag;
}
