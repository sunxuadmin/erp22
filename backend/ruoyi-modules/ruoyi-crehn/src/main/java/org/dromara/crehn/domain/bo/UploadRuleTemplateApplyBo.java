package org.dromara.crehn.domain.bo;

import lombok.Data;

@Data
public class UploadRuleTemplateApplyBo {
    private Long templateId;
    private Long categoryId;
    /**
     * append / overwrite / fields_only / files_only / rules_only
     */
    private String applyMode;
}
