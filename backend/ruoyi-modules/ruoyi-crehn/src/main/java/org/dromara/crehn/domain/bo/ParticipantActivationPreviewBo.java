package org.dromara.crehn.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParticipantActivationPreviewBo {
    @NotBlank(message = "激活码不能为空")
    private String activationCode;
}
