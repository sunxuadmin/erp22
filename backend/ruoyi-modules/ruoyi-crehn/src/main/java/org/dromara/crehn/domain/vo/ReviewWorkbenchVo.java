package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class ReviewWorkbenchVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String activityName;
    private Map<String, Long> stats;
    private List<ReviewTaskVo> pendingTasks;
    private List<ReviewScoreVo> recentScores;
    private List<GroupItem> groupItems;

    @Data
    public static class GroupItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String groupKey;
        private String groupName;
        private Long totalCount;
        private Long pendingCount;
        private Long draftCount;
        private Long submittedCount;
    }
}
