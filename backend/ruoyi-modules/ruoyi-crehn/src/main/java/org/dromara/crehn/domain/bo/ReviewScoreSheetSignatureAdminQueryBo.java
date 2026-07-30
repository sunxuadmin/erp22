package org.dromara.crehn.domain.bo;

import lombok.Data;

/** Filters for the privileged reviewer-signature library console. */
@Data
public class ReviewScoreSheetSignatureAdminQueryBo {

    private Long reviewerUserId;
    /** Matches the reviewer's account or display name. */
    private String reviewerKeyword;
    private String signatureName;
    /** active, disabled, or archived. Empty means all non-deleted records. */
    private String libraryStatus;
}
