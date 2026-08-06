package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("portal_page_layout")
public class PortalPageLayout extends TenantEntity {
    @TableId
    private Long id;
    private Long siteId;
    private String pageCode;
    private String layoutCode;
    private String layoutName;
    private String renderVersion;
    private String themeJson;
    private String componentJson;
    private Integer sortOrder;
    private Boolean enabled;
    private Boolean active;
    @TableLogic
    private String delFlag;
}
