package org.dromara.crehn.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Read-only detail projection for one score-summary project.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewScoreSummaryDetailVo extends ReviewScoreSummaryVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private ProjectVo project;
    private List<ProjectFileVo> attachments;
}
