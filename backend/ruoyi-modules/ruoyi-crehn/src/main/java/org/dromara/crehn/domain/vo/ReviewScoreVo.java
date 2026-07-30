package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.ReviewScore;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@AutoMapper(target = ReviewScore.class)
public class ReviewScoreVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long assignmentId;
    private Long projectId;
    private String projectNo;
    private String projectName;
    private Long activityId;
    private String activityName;
    private Long categoryId;
    private String categoryName;
    private Long reviewerUserId;
    private String reviewerUserName;
    private String reviewerNickName;
    private BigDecimal scoreValue;
    private String gradeValue;
    private String commentText;
    private String status;
    private Date submittedAt;
    private Date createTime;
}
