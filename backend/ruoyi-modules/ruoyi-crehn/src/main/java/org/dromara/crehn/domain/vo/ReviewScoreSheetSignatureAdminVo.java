package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** Privileged view of a reviewer's reusable handwritten signature. */
@Data
public class ReviewScoreSheetSignatureAdminVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long reviewerUserId;
    private String reviewerName;
    private String reviewerUserName;
    private String signatureName;
    private Long ossId;
    private String imageUrl;
    /** Compatibility alias for imageUrl. */
    private String url;
    private Boolean defaultSignature;
    private String libraryStatus;
    private Long version;
    private Date createTime;
    private Date updateTime;
    private Date statusChangedAt;
    private Long statusChangedByUserId;
    private String statusChangedByName;
    private String statusReason;
}
