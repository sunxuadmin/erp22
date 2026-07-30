package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("upload_rule_template")
public class UploadRuleTemplate extends TenantEntity {
    @TableId
    private Long id;
    private String templateCode;
    private String templateName;
    private String categoryGroup;
    private String fieldSchemaJson;
    private String fileRequirementJson;
    private String ruleJson;
    private String tipText;
    private String checkMode;
    private Boolean enabled;
    private Integer sortOrder;
    private String remark;
    @TableLogic
    private String delFlag;
}
