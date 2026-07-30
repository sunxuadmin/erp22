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
 * Shared school-scope semantics for audit and score assignments.
 */
public final class AssignmentSchoolScopeSupport {

    public static final String SCOPE_ALL = "all";
    public static final String SCOPE_WHITELIST = "whitelist";
    public static final String SCOPE_BLACKLIST = "blacklist";

    private AssignmentSchoolScopeSupport() {
    }

    public static String normalizeMode(String mode) {
        String normalized = StringUtils.blankToDefault(StringUtils.trim(mode), SCOPE_ALL);
        if (!SCOPE_ALL.equals(normalized) && !SCOPE_WHITELIST.equals(normalized) && !SCOPE_BLACKLIST.equals(normalized)) {
            throw new ServiceException("学校范围模式不正确");
        }
        return normalized;
    }

    public static String normalizeSchoolIdsJson(String mode, String schoolIdsJson) {
        String normalizedMode = normalizeMode(mode);
        if (SCOPE_ALL.equals(normalizedMode)) {
            return null;
        }
        Set<Long> schoolIds = parseSchoolIds(schoolIdsJson);
        if (schoolIds.isEmpty()) {
            throw new ServiceException("学校白名单或黑名单至少选择一所学校");
        }
        return JsonUtils.toJsonString(new ArrayList<>(schoolIds));
    }

    public static Set<Long> parseSchoolIds(String schoolIdsJson) {
        if (StringUtils.isBlank(schoolIdsJson)) {
            return Set.of();
        }
        if (!JsonUtils.isJsonArray(schoolIdsJson)) {
            throw new ServiceException("学校范围必须是 JSON 数组");
        }
        List<Object> rawIds = JsonUtils.parseObject(schoolIdsJson, new TypeReference<List<Object>>() {
        });
        if (rawIds == null || rawIds.isEmpty()) {
            return Set.of();
        }
        if (rawIds.size() > 2000) {
            throw new ServiceException("学校范围一次最多选择2000所学校");
        }
        Set<Long> schoolIds = new LinkedHashSet<>();
        for (Object rawId : rawIds) {
            if (rawId == null) {
                continue;
            }
            try {
                String schoolIdText = String.valueOf(rawId).trim();
                if (!schoolIdText.matches("[0-9]+")) {
                    throw new NumberFormatException();
                }
                Long schoolId = Long.valueOf(schoolIdText);
                if (schoolId <= 0) {
                    throw new NumberFormatException();
                }
                schoolIds.add(schoolId);
            } catch (NumberFormatException e) {
                throw new ServiceException("学校范围包含无效学校ID");
            }
        }
        return schoolIds;
    }

    public static boolean matches(Long schoolId, String mode, String schoolIdsJson) {
        String normalizedMode = normalizeMode(mode);
        if (SCOPE_ALL.equals(normalizedMode)) {
            return true;
        }
        if (schoolId == null) {
            return false;
        }
        Set<Long> schoolIds = parseSchoolIds(schoolIdsJson);
        return SCOPE_WHITELIST.equals(normalizedMode) ? schoolIds.contains(schoolId) : !schoolIds.contains(schoolId);
    }
}
