package org.dromara.crehn.domain.bo;

import lombok.Data;

@Data
public class ReviewScoreSheetExportBo {
    private Long activityId;
    private Long categoryId;
    /** Opaque current-reviewer assignment scope key; required for batch submission preview. */
    private String scopeKey;
    private Long templateId;
    private ReviewScoreSheetTemplateBo template;
    private String exportTime;
}
