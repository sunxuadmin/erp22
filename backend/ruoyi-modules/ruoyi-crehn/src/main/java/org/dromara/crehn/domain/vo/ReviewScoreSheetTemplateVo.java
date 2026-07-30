package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class ReviewScoreSheetTemplateVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String templateName;
    private String title;
    private List<ReviewScoreSheetColumnVo> columns;
    private List<ReviewScoreSheetFooterRowVo> footerRows;
    private String footerTimeText;
    private String footerSignatureText;
    private Boolean defaultTemplate;
    private Long version;
    private Long updateBy;
    private String updatedByName;
    private Date updateTime;
}
