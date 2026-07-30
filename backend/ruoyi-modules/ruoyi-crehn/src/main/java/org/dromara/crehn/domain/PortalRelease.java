package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("portal_release")
public class PortalRelease extends TenantEntity {
    @TableId
    private Long id;
    private Long siteId;
    private Integer versionNo;
    private String snapshotJson;
    private String checksum;
    private String status;
    private Date publishedAt;
    private Long publishedBy;
    private String releaseReason;
    private Long sourceReleaseId;
}
