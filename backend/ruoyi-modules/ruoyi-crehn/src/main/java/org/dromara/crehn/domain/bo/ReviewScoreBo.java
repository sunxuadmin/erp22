package org.dromara.crehn.domain.bo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReviewScoreBo {
    private Long assignmentId;
    private Long projectId;
    private BigDecimal scoreValue;
    private String gradeValue;
    private String commentText;
}
