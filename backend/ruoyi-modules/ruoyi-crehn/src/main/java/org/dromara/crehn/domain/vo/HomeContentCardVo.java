package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.HomeContentCard;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = HomeContentCard.class)
public class HomeContentCardVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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
    private Date createTime;
    private Date updateTime;
}
