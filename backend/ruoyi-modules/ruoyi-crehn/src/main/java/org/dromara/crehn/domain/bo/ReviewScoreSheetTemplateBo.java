package org.dromara.crehn.domain.bo;

import lombok.Data;
import org.dromara.crehn.domain.vo.ReviewScoreSheetColumnVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetFooterRowVo;

import java.util.List;

@Data
public class ReviewScoreSheetTemplateBo {
    private Long id;
    private String templateName;
    private String title;
    private List<ReviewScoreSheetColumnVo> columns;
    private List<ReviewScoreSheetFooterRowVo> footerRows;
    private String footerTimeText;
    private String footerSignatureText;
    private Boolean defaultTemplate;
    private Long version;
}
