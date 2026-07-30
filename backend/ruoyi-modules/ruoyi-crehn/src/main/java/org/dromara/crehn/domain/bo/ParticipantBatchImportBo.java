package org.dromara.crehn.domain.bo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ParticipantBatchImportBo {
    private Long activityId;
    private Long schoolId;
    private Date expiresAt;
    private String remark;
    private List<ParticipantImportRow> rows;
}
