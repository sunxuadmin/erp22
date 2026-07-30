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
@TableName("portal_article")
public class PortalArticle extends TenantEntity {
    @TableId
    private Long id;
    private Long siteId;
    private Long channelId;
    private Long activityId;
    private String articleCode;
    private String title;
    private String summary;
    private String contentMarkdown;
    private Long coverMediaId;
    private String authorName;
    private String sourceName;
    private String tags;
    private Boolean pinned;
    private String visibility;
    private String status;
    private Date scheduledAt;
    private Date publishedAt;
    private Long currentVersionId;
    private Long rowVersion;
    @TableLogic
    private String delFlag;
}
