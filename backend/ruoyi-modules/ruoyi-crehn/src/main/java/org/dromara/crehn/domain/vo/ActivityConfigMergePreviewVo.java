package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ActivityConfigMergePreviewVo {

    private Long activityId;

    private String activityName;

    private String sourceActivityName;

    private Boolean applied = false;

    private Boolean hasAdditions = false;

    private Integer addedCategoryCount = 0;

    private Integer addedFieldCount = 0;

    private Integer addedFileRequirementCount = 0;

    private Integer skippedCategoryCount = 0;

    private Integer skippedFieldCount = 0;

    private Integer skippedFileRequirementCount = 0;

    private List<Item> additions = new ArrayList<>();

    private List<Item> skipped = new ArrayList<>();

    @Data
    public static class Item {

        private String itemType;

        private String itemTypeLabel;

        private String categoryCode;

        private String categoryName;

        private String itemCode;

        private String itemName;

        private String reason;
    }
}
