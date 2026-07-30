package org.dromara.crehn.domain.bo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Coordinates normalized to the configured signature slot, never to browser
 * pixels. This makes print and Excel generation deterministic.
 */
@Data
public class ReviewScoreSheetSignaturePlacementVo {
    private String slotKey;
    /** Read-only response metadata; it is not trusted or persisted from the browser. */
    private Long signatureId;
    private String signatureName;
    private String signatureUrl;
    private BigDecimal x;
    private BigDecimal y;
    private BigDecimal width;
    private BigDecimal height;
    /** Server-side time of the immutable signed-sheet snapshot. */
    private Date signedAt;
}
