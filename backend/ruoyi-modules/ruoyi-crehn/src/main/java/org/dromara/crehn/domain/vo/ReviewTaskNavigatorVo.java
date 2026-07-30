package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Current-reviewer-only navigation choices. Unit rows never include assignments that hide school information. */
@Data
public class ReviewTaskNavigatorVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<CountOption> categoryOptions = new ArrayList<>();
    private List<CountOption> unitOptions = new ArrayList<>();
    private List<ScopeOption> scopeOptions = new ArrayList<>();
    private List<CountOption> programFormOptions = new ArrayList<>();
    private List<CountOption> groupOrNatureOptions = new ArrayList<>();
    private Long totalCount = 0L;
    private Long completedCount = 0L;
    private Long lockedCount = 0L;
    private Long categoryTotalCount = 0L;
    private Long categoryCompletedCount = 0L;
    private Long categoryLockedCount = 0L;
    private List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> displayColumns = new ArrayList<>();

    @Data
    public static class CountOption implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String id;
        private String label;
        private Long count;

        public CountOption(String id, String label, Long count) {
            this.id = id;
            this.label = label;
            this.count = count;
        }
    }

    @Data
    public static class ScopeOption implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String scopeKey;
        private Long assignmentId;
        private Long categoryId;
        private String categoryName;
        private String scopeLabel;
        private Boolean wholeCategory;
        private Long totalCount = 0L;
        private Long completedCount = 0L;
        private Long lockedCount = 0L;
        private Long pendingCount = 0L;
    }
}
