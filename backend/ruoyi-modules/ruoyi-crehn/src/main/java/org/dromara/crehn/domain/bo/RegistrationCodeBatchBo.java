package org.dromara.crehn.domain.bo;

import lombok.Data;
import org.dromara.crehn.domain.RegistrationCode;

import java.util.Date;
import java.util.List;

@Data
public class RegistrationCodeBatchBo {
    private String codeType;
    private Long activityId;
    private String roleKey;
    private Boolean autoApprove;
    private Integer maxUseCount;
    private Date expireAt;
    private String remark;
    private List<RegistrationCode> rows;
}
