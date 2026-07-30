package org.dromara.crehn.domain.bo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReviewAssignmentScopePlanItemBo {
    private Long categoryId;
    /** all/units */
    private String scopeMode;
    private List<String> unitKeys = new ArrayList<>();
}
