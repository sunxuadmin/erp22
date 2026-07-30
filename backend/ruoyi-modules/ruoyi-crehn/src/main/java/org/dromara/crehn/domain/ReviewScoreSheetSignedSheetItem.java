package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

/**
 * One immutable score row included by a signed score sheet.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review_score_sheet_signed_item")
public class ReviewScoreSheetSignedSheetItem extends TenantEntity {

    @TableId
    private Long id;
    private Long signedSheetId;
    private Long reviewScoreId;
    private Long assignmentId;
    private Long projectId;
    private String scoreSnapshotJson;
    /** Kept in the unique key to reserve future void-and-re-sign support. */
    private Integer activeMarker;
}
