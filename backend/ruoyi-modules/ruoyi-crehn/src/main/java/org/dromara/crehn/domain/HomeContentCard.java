package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("home_content_card")
public class HomeContentCard extends TenantEntity {
    @TableId
    private Long id;
    private String sectionKey;
    private String cardCode;
    private String cardType;
    private String cardTitle;
    private String cardSubtitle;
    private String cardContent;
    private String icon;
    private Long imageOssId;
    private String imageUrl;
    private String linkText;
    private String linkUrl;
    private String linkType;
    private Boolean highlight;
    private Boolean enabled;
    private Integer sortOrder;
    private String layoutType;
    private String templateCode;
    private String slotKey;
    private String configJson;
    private String remark;
    @TableLogic
    private String delFlag;
}
