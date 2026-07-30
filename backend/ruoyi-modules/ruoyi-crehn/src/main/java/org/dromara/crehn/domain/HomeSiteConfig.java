package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("home_site_config")
public class HomeSiteConfig extends TenantEntity {
    @TableId
    private Long id;
    private String configCode;
    private String siteName;
    private Long logoOssId;
    private String logoUrl;
    private String homeLink;
    private String navItemsJson;
    private String headerButtonsJson;
    private String loginStateConfigJson;
    private String sectionConfigJson;
    private Integer noticeLimit;
    private String footerTitle;
    private String footerText;
    private String organizer;
    private String sponsor;
    private String technicalSupport;
    private String contactPhone;
    private String contactEmail;
    private String contactAddress;
    private String icpText;
    private String icpLink;
    private String policeText;
    private String policeLink;
    private String customCss;
    private Boolean enabled;
    private String remark;
    @TableLogic
    private String delFlag;
}
