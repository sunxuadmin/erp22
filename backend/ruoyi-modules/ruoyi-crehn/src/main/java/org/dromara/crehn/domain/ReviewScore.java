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
@TableName("review_score")
public class ReviewScore extends TenantEntity {
    @TableId(value = "id")
    private Long id;
    private Long assignmentId;
    private Long projectId;
    private Long reviewerUserId;
    private BigDecimal scoreValue;
    private String gradeValue;
    private String commentText;
    private String status;
    private Date submittedAt;
    @TableLogic
    private String delFlag;
}
