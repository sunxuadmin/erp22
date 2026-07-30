package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("school_submission_item")
public class SchoolSubmissionItem extends TenantEntity {
    @TableId
    private Long id;
    private Long batchId;
    private Long projectId;
    private Long projectSnapshotId;
    private String status;
}
