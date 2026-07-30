package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review_result")
public class ReviewResult extends TenantEntity {
    @TableId(value = "id")
    private Long id;
    private Long activityId;
    private Long categoryId;
    private Long projectId;
    private Long schoolId;
    private Integer scoreCount;
    private BigDecimal totalScore;
    private BigDecimal averageScore;
    private String finalGrade;
    private String awardLevel;
    private String awardRemark;
    private Integer rankNo;
    private String scoreSummaryJson;
    private String resultStatus;
    private Boolean showScore;
    private Boolean showRank;
    private Boolean showComment;
    private Long publishedBy;
    private Date publishedAt;
    private String remark;
    @TableLogic
    private String delFlag;
}
