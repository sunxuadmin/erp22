package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.UploadRuleTemplate;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = UploadRuleTemplate.class)
public class UploadRuleTemplateVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
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
    private Date createTime;
}
