package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AssignmentBatchPreviewVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String operation;
    private String target;
    private Long createCount = 0L;
    private Long updateCount = 0L;
    private Long disableCount = 0L;
    private Long unchangedCount = 0L;
    private Long preservedSubmittedScoreCount = 0L;
    private Long preservedDraftScoreCount = 0L;
    /** Draft scores that would fall outside the effective assignment scope. */
    private Long orphanDraftScoreCount = 0L;
    /** Scores currently protected by an active signed sheet. */
    private Long lockedScoreCount = 0L;
}
