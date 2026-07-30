package org.dromara.crehn.domain.bo;

import lombok.Data;

@Data
public class SchoolAccountReviewBo {
    private Long userId;
    private String schoolReviewStatus;
    private String opinion;
}
