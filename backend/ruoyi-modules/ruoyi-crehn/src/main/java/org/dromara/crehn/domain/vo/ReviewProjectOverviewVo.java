package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class ReviewProjectOverviewVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long activityId;
    private String activityName;
    private Long categoryId;
    private String categoryName;
    private Long projectId;
    private String projectNo;
    private String projectName;
    private Long schoolId;
    private String schoolName;
    private String groupName;
    private String projectType;
    private String programForm;
    private String performanceOrder;
    private String status;
    private Date submittedAt;
    private Date createTime;
    private Long assignmentCount;
    private Long pendingScoreCount;
    private Long submittedScoreCount;
    private Long draftScoreCount;
    private Long requiredReviewerCount;
    private Long shortageCount;
    private String assignmentStatus;
    private String reviewerNames;
    private BigDecimal averageScore;
    private String finalGrade;
    private String awardLevel;
    private Integer rankNo;
    private String resultStatus;
}
