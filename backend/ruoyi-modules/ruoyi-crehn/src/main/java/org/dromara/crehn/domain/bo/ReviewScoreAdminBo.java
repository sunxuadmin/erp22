package org.dromara.crehn.domain.bo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReviewScoreAdminBo {
    private Long scoreId;
    private BigDecimal scoreValue;
    private String gradeValue;
    private String commentText;
    private String reason;
}
