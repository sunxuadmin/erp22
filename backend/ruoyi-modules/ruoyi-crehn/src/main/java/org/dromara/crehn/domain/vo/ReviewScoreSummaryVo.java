package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Read-only project-level score-summary row.
 */
@Data
public class ReviewScoreSummaryVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer rowNo;
    private Long activityId;
    private String activityName;
    private Long categoryId;
    private String categoryName;
    private Long projectId;
    private String projectNo;
    private String projectName;
    private Long schoolId;
    private String schoolName;
    private String groupOrNature;
    private String programForm;
    private List<ReviewScoreSummaryScoreVo> scoreItems;
    private BigDecimal currentAverageScore;
    private BigDecimal resultAverageScore;
    private Date resultGeneratedAt;
    private ReviewScoreSummaryWarningVo warning;
    private String resultStatus;
}
