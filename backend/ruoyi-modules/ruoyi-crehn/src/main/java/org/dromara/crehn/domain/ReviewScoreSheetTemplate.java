package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review_score_sheet_template")
public class ReviewScoreSheetTemplate extends TenantEntity {
    @TableId
    private Long id;
    private String templateKey;
    private String templateName;
    private String title;
    private String columnConfigJson;
    private String footerTimeText;
    private String footerSignatureText;
    private String footerConfigJson;
    private String defaultFlag;
    @TableField("template_version")
    private Long version;
    @TableLogic
    private String delFlag;
}
