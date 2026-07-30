package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class ReviewScoreSheetSignatureVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String signatureName;
    private Long ossId;
    /** PNG access URL used by the signature library and preview. */
    private String imageUrl;
    /** Retained as a compatibility alias for earlier consumers. */
    private String url;
    /** True only when a create request reused an identical active signature. */
    private Boolean reused;
    private Boolean defaultSignature;
    private Long version;
    private Date createTime;
    private Date updateTime;
}
