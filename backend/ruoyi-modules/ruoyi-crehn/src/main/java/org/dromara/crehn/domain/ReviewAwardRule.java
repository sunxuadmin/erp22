package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review_award_rule")
public class ReviewAwardRule extends TenantEntity {
    @TableId(value = "id")
    private Long id;
    private Long activityId;
    private Long categoryId;
    private String awardLevel;
    private String ruleType;
    private Integer minRank;
    private Integer maxRank;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private Integer sortOrder;
    private Boolean enabled;
    private String remark;
    @TableLogic
    private String delFlag;
}
