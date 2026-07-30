package org.dromara.crehn.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class ParticipantCodeReissueBo {
    @NotNull(message = "参赛者ID不能为空")
    private Long participantId;
    private Date expiresAt;
    @NotBlank(message = "重新签发原因不能为空")
    private String reason;
}
