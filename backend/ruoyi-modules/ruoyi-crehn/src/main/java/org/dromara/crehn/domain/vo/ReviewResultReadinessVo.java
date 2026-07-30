package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only generation readiness for one activity/category scope.
 */
@Data
public class ReviewResultReadinessVo {

    private boolean ready;
    private long projectCount;
    private long activeAssignmentCount;
    private int requiredReviewerCount;
    private long expectedScoreCount;
    private long submittedScoreCount;
    private long signedScoreCount;
    private long activeSignedSheetCount;
    private List<String> blockers = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
}
