package org.dromara.crehn.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectUploadOverviewVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long activityId;
    private String activityName;
    private List<ActivityItem> activities = new ArrayList<>();
    private CountItem summary = new CountItem();
    private List<CategoryItem> groupItems = new ArrayList<>();
    private List<CategoryItem> categoryItems = new ArrayList<>();

    @Data
    public static class ActivityItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;
        private String activityName;
        private Integer year;
    }

    @Data
    public static class CountItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private long totalCount;
        private long draftCount;
        private long submittedCount;
        private long auditedCount;
        private long returnedCount;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CategoryItem extends CountItem {
        @Serial
        private static final long serialVersionUID = 1L;

        private String id;
        private Long categoryId;
        private String categoryCode;
        private String categoryName;
        private String groupKey;
        private String groupName;
        private Integer groupSortOrder;
        private Integer sortOrder;
    }
}
