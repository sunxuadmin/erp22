package org.dromara.crehn.domain.vo;

import lombok.Data;

@Data
public class ParticipantActivationPreviewVo {
    private String participantName;
    private String schoolName;
    private String activityName;
    private String identityNoMasked;
    private String phonenumberMasked;
    private String emailMasked;
}
