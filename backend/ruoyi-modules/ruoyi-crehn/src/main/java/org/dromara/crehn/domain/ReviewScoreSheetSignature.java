package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.util.Date;

/**
 * A reusable handwritten signature owned by one review expert.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review_score_sheet_signature")
public class ReviewScoreSheetSignature extends TenantEntity {

    @TableId
    private Long id;
    private Long reviewerUserId;
    private String signatureName;
    private Long ossId;
    private String contentSha256;
    private String defaultFlag;
    /**
     * Administrative lifecycle of a reusable signature. It is deliberately
     * separate from {@link #activeMarker}: a manager can disable or archive a
     * signature without releasing its duplicate-content guard or damaging any
     * already-signed sheet snapshot.
     */
    private String libraryStatus;
    private Date statusChangedAt;
    private Long statusChangedByUserId;
    private String statusReason;
    /**
     * Allows a deleted signature hash to be uploaded again. Active rows use 1;
     * deleted rows are changed to null before their logical delete.
     */
    private Integer activeMarker;
    @TableField("signature_version")
    private Long version;
    @TableLogic
    private String delFlag;
}
