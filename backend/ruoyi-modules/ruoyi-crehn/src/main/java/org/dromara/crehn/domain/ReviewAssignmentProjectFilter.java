package org.dromara.crehn.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persisted, server-enforced project tags for a review assignment.
 */
@Data
public class ReviewAssignmentProjectFilter {
    private List<String> schoolTypes = new ArrayList<>();
    private Map<String, List<String>> fieldValues = new LinkedHashMap<>();
    private List<ReviewAssignmentProjectScopeGroup> scopeGroups = new ArrayList<>();
}
