package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectQuotaOverviewVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long activityId;
    private String activityName;
    private Long schoolId;
    private String schoolName;
    private String schoolType;
    private long totalCount;
    private long draftCount;
    private long submittedCount;
    private long completedCount;
    private long usedCount;
    private Integer quotaLimit;
    private Integer remainingCount;
    private List<Item> groupItems = new ArrayList<>();
    private List<Item> categoryItems = new ArrayList<>();

    @Data
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String id;
        private Long categoryId;
        private String categoryCode;
        private String categoryName;
        private String groupKey;
        private String groupName;
        private Integer sortOrder;
        private Integer quotaLimit;
        private Integer remainingCount;
        private long totalCount;
        private long draftCount;
        private long submittedCount;
        private long completedCount;
        private long usedCount;
        private List<RatioItem> ratioItems = new ArrayList<>();
    }

    @Data
    public static class RatioItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String key;
        private String groupCode;
        private String groupName;
        private long count;
        private BigDecimal percent;
        private Integer limitCount;
        private BigDecimal ratioValue;
        private String operator;
        private String message;
    }
}
