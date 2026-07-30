package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectViewStatsVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<CountItem> categoryCounts = new ArrayList<>();
    private List<CountItem> schoolCounts = new ArrayList<>();

    @Data
    public static class CountItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;
        private Long totalCount;
    }
}
