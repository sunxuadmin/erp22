package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Read-only filters for the administrator score-summary view.
 */
@Data
public class ReviewScoreSummaryQueryVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long activityId;
    private Long categoryId;
    private Long schoolId;
    private Long reviewerUserId;
    private String projectName;
    private String programForm;
    private String groupOrNature;
    /** desc (default), asc or none; sorting is performed server-side. */
    private String averageSort;
    private String resultStatus;
}
