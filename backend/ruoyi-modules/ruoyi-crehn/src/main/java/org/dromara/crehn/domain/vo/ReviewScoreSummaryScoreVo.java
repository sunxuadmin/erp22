package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * One submitted review in the score-summary row. Slots are one-based and are
 * assigned by the server in a deterministic submitted-time/order sequence.
 */
@Data
public class ReviewScoreSummaryScoreVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer slot;
    private Long scoreId;
    private Long assignmentId;
    private Long reviewerUserId;
    private String reviewerName;
    private BigDecimal scoreValue;
    private String gradeValue;
    private String commentText;
    private String status;
    private Date submittedAt;

    /**
     * Flattened signature fields are kept on the score item so the summary
     * table can render the signed reviewer entry without having to know the
     * nested signature DTO shape.  The nested object remains available for
     * consumers that need the full audit metadata (withdrawal reason, actor,
     * etc.).
     */
    private String signatureUrl;
    private String signatureName;
    private Date signatureSignedAt;
    private String signatureStatus;
    private String submissionMode;
    private ReviewScoreSummarySignatureVo signature;
}
