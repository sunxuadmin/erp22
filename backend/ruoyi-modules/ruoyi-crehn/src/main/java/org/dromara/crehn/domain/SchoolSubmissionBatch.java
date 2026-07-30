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
@TableName("school_submission_batch")
public class SchoolSubmissionBatch extends TenantEntity {
    @TableId
    private Long id;
    private String batchNo;
    private Long activityId;
    private Long schoolId;
    private Integer itemCount;
    private String status;
    private Long submittedBy;
    private Date submittedAt;
    private Long withdrawnBy;
    private Date withdrawnAt;
    private String reason;
    private Long rowVersion;
    @TableLogic
    private String delFlag;
}
