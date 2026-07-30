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
@TableName("portal_home_component")
public class PortalHomeComponent extends TenantEntity {
    @TableId
    private Long id;
    private Long siteId;
    private String pageCode;
    private String componentKey;
    private String componentType;
    private String dataSourceCode;
    private String configJson;
    private Integer gridX;
    private Integer gridY;
    private Integer gridW;
    private Integer gridH;
    private Integer sortOrder;
    private Date visibleFrom;
    private Date visibleUntil;
    private Boolean enabled;
    @TableLogic
    private String delFlag;
}
