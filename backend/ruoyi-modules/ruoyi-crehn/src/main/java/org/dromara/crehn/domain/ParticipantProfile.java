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
@TableName("participant_profile")
public class ParticipantProfile extends TenantEntity {
    @TableId
    private Long id;
    private Long userId;
    private Long activityId;
    private Long schoolId;
    private String participantName;
    private String identityType;
    private String identityNoHash;
    private String identityNoMasked;
    private String phonenumber;
    private String email;
    private String profileJson;
    private String status;
    private Date confirmedAt;
    private Long rowVersion;
    @TableLogic
    private String delFlag;
}
