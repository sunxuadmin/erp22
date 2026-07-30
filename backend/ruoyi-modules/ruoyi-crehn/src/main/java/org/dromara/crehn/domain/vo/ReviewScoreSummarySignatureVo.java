package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Historical signature-table snapshot associated with one score.
 */
@Data
public class ReviewScoreSummarySignatureVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long signedSheetId;
    private Long signatureId;
    private String signatureUrl;
    private String signatureName;
    private Date signatureSignedAt;
    private String signatureStatus;
    /** SIGNED carries a handwritten image snapshot; UNSIGNED is an approved optional-signature submission. */
    private String submissionMode;
    private Date withdrawnAt;
    private String withdrawnByName;
    private String withdrawalMode;
    private String withdrawReason;
    /** True when the current score differs from the immutable signed snapshot. */
    private Boolean scoreMismatch;
    private BigDecimal signedScoreValue;
}
