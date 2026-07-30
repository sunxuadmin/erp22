package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class ReviewScoreSheetExportPreviewVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private ReviewScoreSheetTemplateVo template;
    /** Scores eligible for the next batch. */
    private List<ReviewTaskVo> rows;
    /** Number of rows eligible for the next batch. */
    private Long total;
    /** All assigned works in the selected review scope. */
    private Long assignedTotal;
    /** Complete scores, including submitted and still-draft scores. */
    private Long completedCount;
    /** Scores already covered by effective submitted batches. */
    private Long submittedCount;
    /** Assigned works without a complete score. */
    private Long pendingCount;
    /** Works that remain outside submitted batches after saving this preview. */
    private Long remainingCount;
    /** True when saving this preview would complete the selected review scope. */
    private Boolean completeAfterSubmit;
    private String reviewerName;
    private String categoryName;
    private String exportTime;
    private ReviewScoreSheetSignedSheetVo existingSheet;
    private Boolean signable;
    private String message;
}
