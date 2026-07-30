package org.dromara.crehn.domain.bo;

import lombok.Data;

/**
 * Client input for the one-way save of a signed score-sheet batch. Score IDs
 * are deliberately absent: the server derives the complete draft scores from
 * the current reviewer's opaque assignment scope.
 */
@Data
public class ReviewScoreSheetSignedSheetCreateBo {
    private Long activityId;
    private Long categoryId;
    private String scopeKey;
    private Long templateId;
    private ReviewScoreSheetTemplateBo template;
    private Long signatureId;
    private ReviewScoreSheetSignaturePlacementVo placement;
}
