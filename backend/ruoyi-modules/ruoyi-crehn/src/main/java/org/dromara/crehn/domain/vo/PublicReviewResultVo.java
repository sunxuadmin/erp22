package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PublicReviewResultVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String activityName;
    private String categoryName;
    private String projectNo;
    private String projectName;
    private String schoolName;
    private BigDecimal averageScore;
    private String finalGrade;
    private String awardLevel;
    private Integer rankNo;
    private Date publishedAt;
}
