package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("portal_article_version")
public class PortalArticleVersion extends TenantEntity {
    @TableId
    private Long id;
    private Long articleId;
    private Integer versionNo;
    private String snapshotJson;
    private String checksum;
    private Date publishedAt;
    private Long publishedBy;
    private Long sourceVersionId;
}
