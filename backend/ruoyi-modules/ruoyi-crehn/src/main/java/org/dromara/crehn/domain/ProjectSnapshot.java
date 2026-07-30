package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_snapshot")
public class ProjectSnapshot extends TenantEntity {
    @TableId
    private Long id;
    private Long projectId;
    private Integer versionNo;
    private String snapshotJson;
    private String checksum;
    private Long submittedBy;
    private Date submittedAt;
    private Long sourceSnapshotId;
}
