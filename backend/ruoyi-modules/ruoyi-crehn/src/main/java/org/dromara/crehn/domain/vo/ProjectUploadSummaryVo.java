package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectUploadSummaryVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private long totalCount;
    private long schoolCount;
    private List<Item> statusItems = new ArrayList<>();
    private List<Item> categoryItems = new ArrayList<>();
    private List<SchoolItem> schoolItems = new ArrayList<>();
    private List<Item> groupItems = new ArrayList<>();
    private List<Item> projectTypeItems = new ArrayList<>();
    private List<Item> programFormItems = new ArrayList<>();
    private List<Item> performanceOrderItems = new ArrayList<>();

    @Data
    public static class Item implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String key;
        private String label;
        private long count;
        private double percent;
    }

    @Data
    public static class SchoolItem extends Item {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long schoolId;
        private long draftCount;
        private long submittedCount;
        private long returnedCount;
        private long passedCount;
    }
}
