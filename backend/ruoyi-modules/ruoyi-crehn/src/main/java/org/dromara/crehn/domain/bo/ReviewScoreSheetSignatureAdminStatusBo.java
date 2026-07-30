package org.dromara.crehn.domain.bo;

import lombok.Data;

/** Required audit reason for a privileged signature-library state change. */
@Data
public class ReviewScoreSheetSignatureAdminStatusBo {

    private String reason;
}
