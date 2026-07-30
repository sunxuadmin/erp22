package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("portal_site")
public class PortalSite extends TenantEntity {
    @TableId
    private Long id;
    private String siteCode;
    private String siteName;
    private String domainName;
    private Long logoOssId;
    private String themeJson;
    private String seoJson;
    private String filingNo;
    private String status;
    @TableLogic
    private String delFlag;
}
