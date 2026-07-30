package org.dromara.crehn.project.service;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.CategoryFieldSchema;
import org.dromara.crehn.domain.CategoryFileRequirement;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ProjectFile;
import org.dromara.crehn.domain.ProjectMember;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;

import java.util.List;
import java.util.Map;

@Data
public class ProjectSubmitContext {
    private Project project;
    private ActivityCategory category;
    private List<ProjectMember> members;
    private List<CategoryFieldSchema> fieldSchemas;
    private List<ProjectFile> files;
    private List<CategoryFileRequirement> fileRequirements;
    private Map<String, Object> formData;
    private Map<String, Object> categoryRule;
    private SubmitValidationResult result;

    public String field(String key) {
        Object value = formData == null ? null : formData.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    public boolean truthy(String key) {
        Object value = formData == null ? null : formData.get(key);
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Iterable<?> iterable) {
            return iterable.iterator().hasNext();
        }
        String text = String.valueOf(value).trim();
        return StringUtils.isNotBlank(text) && !"false".equalsIgnoreCase(text) && !"0".equals(text) && !"no".equalsIgnoreCase(text);
    }

    public String categoryText() {
        return String.join(" ",
            StringUtils.blankToDefault(category.getCategoryCode(), ""),
            StringUtils.blankToDefault(category.getCategoryGroup(), ""),
            StringUtils.blankToDefault(category.getCategoryName(), "")
        ).toLowerCase();
    }

    public String validatorType() {
        Object value = categoryRule == null ? null : categoryRule.get("validatorType");
        return value == null ? "" : String.valueOf(value).toLowerCase();
    }

    public static Map<String, Object> parseJsonObject(String json) {
        if (StringUtils.isBlank(json) || !JsonUtils.isJsonObject(json)) {
            return Map.of();
        }
        Map<String, Object> parsed = JsonUtils.parseObject(json, new TypeReference<Map<String, Object>>() {
        });
        return parsed == null ? Map.of() : parsed;
    }

    public static long countFilledRows(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Iterable<?> iterable) {
            long count = 0;
            for (Object item : iterable) {
                if (!isBlankStructuredValue(item)) {
                    count++;
                }
            }
            return count;
        }
        if (value instanceof Map<?, ?> map) {
            return isBlankStructuredValue(map) ? 0 : 1;
        }
        String text = String.valueOf(value).trim();
        if (StringUtils.isBlank(text)) {
            return 0;
        }
        long count = 0;
        for (String item : text.split("[,，;；、\\n]+")) {
            if (StringUtils.isNotBlank(item)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isBlankStructuredValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                return true;
            }
            for (Object item : map.values()) {
                if (!isBlankStructuredValue(item)) {
                    return false;
                }
            }
            return true;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (!isBlankStructuredValue(item)) {
                    return false;
                }
            }
            return true;
        }
        return StringUtils.isBlank(String.valueOf(value));
    }
}
