package org.dromara.crehn.domain.bo;

import lombok.Data;

/**
 * Filter for signed-score-sheet history. Scope is never trusted from the
 * browser: non-managers are always restricted to their own sheets.
 */
@Data
public class ReviewScoreSheetSignedSheetQueryBo {
    private Long activityId;
    private Long categoryId;
    private Long reviewerUserId;
    private String status;
    /** yyyy-MM-dd or yyyy-MM-dd HH:mm:ss, inclusive. */
    private String signedAtStart;
    /** yyyy-MM-dd or yyyy-MM-dd HH:mm:ss, inclusive. */
    private String signedAtEnd;
    /** Administrators can opt into their own history instead of the summary. */
    private Boolean ownerOnly;
}
