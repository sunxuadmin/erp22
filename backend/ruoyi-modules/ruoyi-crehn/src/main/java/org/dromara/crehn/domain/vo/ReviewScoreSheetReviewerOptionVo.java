package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** Reviewer selector built from signed-sheet history, including former reviewers. */
@Data
public class ReviewScoreSheetReviewerOptionVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long reviewerUserId;
    private String reviewerName;
}
