package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectQuotaRatioSummaryVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long activityId;
    private String activityName;
    private long totalCount;
    private long exceededCount;
    private List<ActivityItem> activities = new ArrayList<>();
    private List<Item> items = new ArrayList<>();

    @Data
    public static class ActivityItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String activityName;
        private Integer year;
    }

    @Data
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long ruleId;
        private Long categoryId;
        private String categoryName;
        private String groupCode;
        private String groupName;
        private String targetFieldKey;
        private String scopeCategoryGroup;
        private String scopeCategoryCodes;
        private String operator;
        private BigDecimal ratioValue;
        private BigDecimal ratio;
        private long numerator;
        private long denominator;
        private boolean exceeded;
        private String statusText;
        private String message;
        private String remark;
    }
}
