package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 学校首页数据看板聚合结果，仅包含当前学校可见的数据。 */
@Data
public class SchoolDashboardVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long totalCount = 0L;
    private Long draftCount = 0L;
    private Long submittedCount = 0L;
    private Long returnedCount = 0L;
    private Long passedCount = 0L;
    private Long publishedResultCount = 0L;
    private List<SchoolDashboardProjectVo> recentEdited = new ArrayList<>();
    private List<SchoolDashboardProjectVo> recentSubmitted = new ArrayList<>();
    private List<SchoolDashboardProjectVo> recentPassed = new ArrayList<>();
    private List<SchoolDashboardResultVo> recentResults = new ArrayList<>();
}
