package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class ReviewAssignmentScopeUnitVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String unitKey;
    private String pathLabel;
    private Map<String, String> fieldValues = new LinkedHashMap<>();
    private Long projectCount;
    private Integer requiredReviewerCount;
    private Integer minimumAssignedReviewerCount;
    private Integer maximumAssignedReviewerCount;
    private Long shortageProjectCount;
    private String assignmentStatus;
}
