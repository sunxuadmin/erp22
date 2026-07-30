package org.dromara.crehn.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ParticipantActivateBo {
    @NotBlank(message = "激活码不能为空")
    private String activationCode;
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 30, message = "密码长度必须为8至30位")
    private String password;
    @NotBlank(message = "请确认本人资料")
    private String profileConfirmation;
}
