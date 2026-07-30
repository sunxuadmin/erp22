package org.dromara.crehn.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One exact review-unit condition. Values inside a field are OR; fields in the
 * same group are AND; multiple groups in an assignment are OR.
 */
@Data
public class ReviewAssignmentProjectScopeGroup {
    private List<String> schoolTypes = new ArrayList<>();
    private Map<String, List<String>> fieldValues = new LinkedHashMap<>();
}
