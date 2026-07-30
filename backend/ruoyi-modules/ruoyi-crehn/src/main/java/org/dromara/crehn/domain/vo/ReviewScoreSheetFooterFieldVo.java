package org.dromara.crehn.domain.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
public class ReviewScoreSheetFooterFieldVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String label;
    private String type;
    private String lineLength;
    /**
     * Stable identifier for a signature placement area. It is only used when
     * {@link #type} is {@code signature}; templates from before this field was
     * introduced remain valid through the server-side legacy-slot upgrade.
     */
    private String slotKey;
    /** Editing-only wording for the empty-slot action button. */
    private String signatureButtonText;
    /** Editing-only wording shown with the empty-slot action button. */
    private String signatureHintText;
    /** Formal timestamp prefix retained by signed-sheet preview, print and Excel. */
    private String signatureTimeText;
    /**
     * Whether this reviewer-signature slot is mandatory when the current
     * category's submitted scores are archived. Legacy templates omit this
     * property and are normalized to {@code true} by the service.
     */
    private Boolean signatureRequired;
    /**
     * Whether the signed-sheet snapshot renders its signature timestamp.
     * The server always retains {@code signedAt} for audit and withdrawal.
     */
    private Boolean signatureTimeVisible;

    public ReviewScoreSheetFooterFieldVo(String label, String type, String lineLength, String slotKey,
                                         String signatureButtonText, String signatureHintText, String signatureTimeText,
                                         Boolean signatureRequired, Boolean signatureTimeVisible) {
        this.label = label;
        this.type = type;
        this.lineLength = lineLength;
        this.slotKey = slotKey;
        this.signatureButtonText = signatureButtonText;
        this.signatureHintText = signatureHintText;
        this.signatureTimeText = signatureTimeText;
        this.signatureRequired = signatureRequired;
        this.signatureTimeVisible = signatureTimeVisible;
    }

    public ReviewScoreSheetFooterFieldVo(String label, String type, String lineLength, String slotKey,
                                         String signatureButtonText, String signatureHintText, String signatureTimeText) {
        this(label, type, lineLength, slotKey, signatureButtonText, signatureHintText, signatureTimeText,
            Boolean.TRUE, Boolean.TRUE);
    }

    public ReviewScoreSheetFooterFieldVo(String label, String type, String lineLength, String slotKey) {
        this(label, type, lineLength, slotKey, null, null, null);
    }

    public ReviewScoreSheetFooterFieldVo(String label, String type, String lineLength) {
        this(label, type, lineLength, null);
    }
}
