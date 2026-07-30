package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.HomeSiteConfig;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = HomeSiteConfig.class)
public class HomeSiteConfigVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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
    private Date createTime;
    private Date updateTime;
}
