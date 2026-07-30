package org.dromara.crehn.domain.vo;

import lombok.Data;
import org.dromara.crehn.domain.bo.ReviewScoreSheetSignaturePlacementVo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Read model for a saved signed sheet. All display/export data comes from its
 * snapshots rather than live review-score rows.
 */
@Data
public class ReviewScoreSheetSignedSheetVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long activityId;
    private String activityName;
    private Long categoryId;
    private String categoryName;
    private Long reviewerUserId;
    private String reviewerName;
    private Long templateId;
    private Long templateVersion;
    private ReviewScoreSheetTemplateVo template;
    private Long signatureId;
    private Long signatureOssId;
    private String signatureName;
    private String signatureUrl;
    private ReviewScoreSheetSignaturePlacementVo placement;
    /** SIGNED or UNSIGNED. Existing sheets without the column are treated as SIGNED. */
    private String submissionMode;
    private Long total;
    private List<ReviewTaskVo> rows;
    private Date signedAt;
    private String exportTime;
    private String status;
    private Date withdrawnAt;
    private Long withdrawnByUserId;
    private String withdrawnByName;
    private String withdrawnByRole;
    private String withdrawalMode;
    private String withdrawReason;
}
