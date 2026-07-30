package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ReviewAssignmentCategoryConfigVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long activityId;
    private Long categoryId;
    private String categoryName;
    private List<String> scopeFieldKeys = new ArrayList<>();
    private Boolean visibilityConfigured;
    private Boolean hideSchoolInfo;
    private Boolean hideMemberInfo;
    private String hiddenFieldKeysJson;
    private List<ReviewAssignmentScopeUnitVo> scopeUnits = new ArrayList<>();
}
