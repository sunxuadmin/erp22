package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ProjectCategoryStatsVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long categoryId;
    private Long totalCount;
    private Long draftCount;
    private Long submittedCount;
    private Long completedCount;
}
