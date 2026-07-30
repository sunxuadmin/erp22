package org.dromara.crehn.review.sheet;

import org.dromara.crehn.domain.bo.ReviewScoreSheetSignaturePlacementVo;

/** In-memory PNG, normalized placement and signed-time text used only while creating XLSX. */
public record ReviewScoreSheetSignatureImage(String slotKey, byte[] pngBytes,
                                              ReviewScoreSheetSignaturePlacementVo placement,
                                              String signedTime,
                                              boolean timeVisible) {

    public ReviewScoreSheetSignatureImage(String slotKey, byte[] pngBytes,
                                          ReviewScoreSheetSignaturePlacementVo placement, String signedTime) {
        this(slotKey, pngBytes, placement, signedTime, true);
    }
}
