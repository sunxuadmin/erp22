package org.dromara.crehn.domain.bo;

import lombok.Data;

/** Required audit reason for withdrawing an effective signed score sheet. */
@Data
public class ReviewScoreSheetSignedSheetWithdrawBo {
    private String reason;
}
