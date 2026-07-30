package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("portal_channel")
public class PortalChannel extends TenantEntity {
    @TableId
    private Long id;
    private Long siteId;
    private Long parentId;
    private String channelCode;
    private String channelName;
    private String navigationPosition;
    private Integer sortOrder;
    private String visibility;
    private String status;
    @TableLogic
    private String delFlag;
}
