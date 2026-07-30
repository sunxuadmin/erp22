package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("portal_media_asset")
public class PortalMediaAsset extends TenantEntity {
    @TableId
    private Long id;
    private Long siteId;
    private Long ossId;
    private String mediaType;
    private String title;
    private String altText;
    private String copyrightOwner;
    private String sourceName;
    private String contentSha256;
    private String scanStatus;
    private String status;
    @TableLogic
    private String delFlag;
}
