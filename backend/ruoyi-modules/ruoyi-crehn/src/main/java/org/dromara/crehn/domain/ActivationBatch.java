package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("activation_batch")
public class ActivationBatch extends TenantEntity {
    @TableId
    private Long id;
    private String batchNo;
    private Long activityId;
    private Long schoolId;
    private Integer totalCount;
    private Integer activatedCount;
    private Date expiresAt;
    private String status;
    private String remark;
    @TableLogic
    private String delFlag;
}
