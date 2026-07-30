package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/** 已发布结果的轻量只读投影；分数与排名遵守结果发布可见性。 */
@Data
public class SchoolDashboardResultVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long projectId;
    private String projectName;
    private String categoryName;
    private String awardLevel;
    private Integer rankNo;
    private BigDecimal averageScore;
    private String finalGrade;
    private Date publishedAt;
}
