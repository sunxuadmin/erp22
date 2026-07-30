package org.dromara.crehn.common;

import com.fasterxml.jackson.core.type.TypeReference;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Included/excluded project semantics for review assignments.
 */
public final class ReviewAssignmentProjectScopeSupport {

    private ReviewAssignmentProjectScopeSupport() {
    }

    public static Set<Long> parseProjectIds(String text) {
        if (StringUtils.isBlank(text)) {
            return new LinkedHashSet<>();
        }
        if (!JsonUtils.isJsonArray(text)) {
            throw new ServiceException("作品范围必须是 JSON 数组");
        }
        List<Object> rawIds = JsonUtils.parseObject(text, new TypeReference<List<Object>>() {
        });
        if (rawIds == null || rawIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        if (rawIds.size() > 5000) {
            throw new ServiceException("作品范围一次最多包含5000个项目");
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (Object rawId : rawIds) {
            if (rawId == null) {
                continue;
            }
            try {
                String idText = String.valueOf(rawId).trim();
                if (!idText.matches("[0-9]+")) {
                    throw new NumberFormatException();
                }
                Long id = Long.valueOf(idText);
                if (id <= 0) {
                    throw new NumberFormatException();
                }
                ids.add(id);
            } catch (NumberFormatException e) {
                throw new ServiceException("作品范围包含无效项目ID");
            }
        }
        return ids;
    }

    public static String normalizeProjectIdsJson(String text) {
        Set<Long> ids = parseProjectIds(text);
        return ids.isEmpty() ? null : JsonUtils.toJsonString(new ArrayList<>(ids));
    }

    public static boolean matches(Long projectId, String includedProjectIdsJson, String excludedProjectIdsJson) {
        if (projectId == null) {
            return false;
        }
        Set<Long> includedIds = parseProjectIds(includedProjectIdsJson);
        if (!includedIds.isEmpty() && !includedIds.contains(projectId)) {
            return false;
        }
        return !parseProjectIds(excludedProjectIdsJson).contains(projectId);
    }
}
