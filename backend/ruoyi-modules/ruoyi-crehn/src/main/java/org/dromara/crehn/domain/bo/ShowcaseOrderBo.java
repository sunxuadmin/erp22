package org.dromara.crehn.domain.bo;

import lombok.Data;

import java.util.List;

@Data
public class ShowcaseOrderBo {
    private Long activityId;
    private Long categoryId;
    private String remark;
    private List<Item> items;

    @Data
    public static class Item {
        private Long projectId;
        private String performanceOrder;
    }
}
