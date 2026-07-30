package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.util.Date;

/**
 * Immutable header snapshot for one saved and signed review score sheet.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review_score_sheet_signed")
public class ReviewScoreSheetSignedSheet extends TenantEntity {

    @TableId
    private Long id;
    private Long reviewerUserId;
    private String reviewerNameSnapshot;
    private Long activityId;
    private String activityNameSnapshot;
    private Long categoryId;
    private String categoryNameSnapshot;
    private Long templateId;
    private Long templateVersion;
    private String templateSnapshotJson;
    private Long signatureId;
    private Long signatureOssId;
    private String signatureNameSnapshot;
    private String signaturePlacementJson;
    /** SIGNED for a handwritten-signature snapshot; UNSIGNED for an optional-signature submission. */
    private String submissionMode;
    private Long scoreCount;
    private Date signedAt;
    private String status;
    /** Server-side withdrawal audit snapshot; the original signature remains immutable. */
    private Date withdrawnAt;
    private Long withdrawnByUserId;
    private String withdrawnByNameSnapshot;
    private String withdrawnByRoleSnapshot;
    private String withdrawalMode;
    private String withdrawReason;
    /** 1 while active; nullable when a future correction flow voids this sheet. */
    private Integer activeMarker;
}
