package org.dromara.crehn.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.domain.CategoryFieldSchema;
import org.dromara.crehn.domain.CategoryFileRequirement;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ProjectFile;
import org.dromara.crehn.domain.ProjectMember;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.ProjectMemberMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

@RequiredArgsConstructor
public abstract class ProjectSubmitValidationSupport implements ProjectCategorySubmitValidator {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;

    protected long memberCount(ProjectSubmitContext context, String type) {
        return context.getMembers().stream().filter(member -> type.equalsIgnoreCase(StringUtils.blankToDefault(member.getMemberType(), ""))).count();
    }

    protected List<ProjectMember> members(ProjectSubmitContext context, String type) {
        return context.getMembers().stream().filter(member -> type.equalsIgnoreCase(StringUtils.blankToDefault(member.getMemberType(), ""))).toList();
    }

    protected long roleMemberCount(ProjectSubmitContext context, String role, String... fallbackLabelKeywords) {
        boolean matchedDynamicField = false;
        long count = 0;
        if (context.getFieldSchemas() != null && context.getFormData() != null) {
            for (CategoryFieldSchema field : context.getFieldSchemas()) {
                if (field == null || !"teacher_group".equals(StringUtils.blankToDefault(field.getFieldType(), ""))) {
                    continue;
                }
                Map<String, Object> rule = ProjectSubmitContext.parseJsonObject(field.getValidationJson());
                if (!matchesMemberRole(field, rule, role, fallbackLabelKeywords)) {
                    continue;
                }
                matchedDynamicField = true;
                count += ProjectSubmitContext.countFilledRows(context.getFormData().get(field.getFieldKey()));
            }
        }
        return matchedDynamicField ? count : memberCount(context, role);
    }

    protected long roleMemberCountRule(ProjectSubmitContext context, String role, String ruleKey, String... fallbackLabelKeywords) {
        List<String> fieldKeys = new ArrayList<>(stringListRule(context, ruleKey));
        fieldKeys.addAll(parseStringList(mapRule(context, "memberCountFields").get(role)));
        if (!fieldKeys.isEmpty()) {
            return dynamicFieldRowCount(context, fieldKeys);
        }
        return roleMemberCount(context, role, fallbackLabelKeywords);
    }

    protected long dynamicFieldRowCount(ProjectSubmitContext context, List<String> fieldKeys) {
        if (context.getFormData() == null || fieldKeys == null || fieldKeys.isEmpty()) {
            return 0;
        }
        long count = 0;
        for (String fieldKey : fieldKeys) {
            if (StringUtils.isBlank(fieldKey)) {
                continue;
            }
            count += ProjectSubmitContext.countFilledRows(context.getFormData().get(fieldKey));
        }
        return count;
    }

    private boolean matchesMemberRole(CategoryFieldSchema field, Map<String, Object> rule, String expectedRole, String... fallbackLabelKeywords) {
        String role = firstRuleText(rule, "memberRole", "businessRole", "role");
        if (StringUtils.isNotBlank(role)) {
            return normalizeMemberRole(role).equals(normalizeMemberRole(expectedRole));
        }
        String text = String.join(" ",
            StringUtils.blankToDefault(field.getFieldKey(), ""),
            StringUtils.blankToDefault(field.getFieldLabel(), "")
        );
        return containsAny(text, fallbackLabelKeywords);
    }

    private String firstRuleText(Map<String, Object> rule, String... keys) {
        if (rule == null) {
            return "";
        }
        for (String key : keys) {
            Object value = rule.get(key);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    private String normalizeMemberRole(String value) {
        String text = StringUtils.blankToDefault(value, "").toLowerCase(Locale.ROOT);
        if (containsAny(text, "author", "作者")) {
            return "author";
        }
        if (containsAny(text, "teacher", "adviser", "advisor", "instructor", "指导", "教师", "老师")) {
            return "teacher";
        }
        if (containsAny(text, "student", "学生")) {
            return "student";
        }
        return text;
    }

    protected void require(boolean expression, String message) {
        if (!expression) {
            throw new ServiceException(message);
        }
    }

    protected boolean containsAny(String text, String... values) {
        String normalized = StringUtils.blankToDefault(text, "").toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (normalized.contains(value.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    protected String lowerField(ProjectSubmitContext context, String key) {
        return context.field(key).toLowerCase(Locale.ROOT);
    }

    protected String firstField(ProjectSubmitContext context, String... keys) {
        for (String key : keys) {
            String value = context.field(key);
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    protected boolean truthyAny(ProjectSubmitContext context, String... keys) {
        for (String key : keys) {
            if (context.truthy(key)) {
                return true;
            }
        }
        return false;
    }

    protected boolean truthyAnyRule(ProjectSubmitContext context, String ruleKey, String... defaultKeys) {
        return truthyAny(context, stringListRule(context, ruleKey, defaultKeys).toArray(String[]::new));
    }

    protected String firstFieldRule(ProjectSubmitContext context, String ruleKey, String... defaultKeys) {
        return firstField(context, stringListRule(context, ruleKey, defaultKeys).toArray(String[]::new));
    }

    protected List<String> stringListRule(ProjectSubmitContext context, String key, String... defaultValues) {
        Map<String, Object> rule = context.getCategoryRule();
        Object value = rule == null ? null : rule.get(key);
        List<String> parsed = parseStringList(value);
        return parsed.isEmpty() ? Arrays.stream(defaultValues).filter(StringUtils::isNotBlank).toList() : parsed;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> mapRule(ProjectSubmitContext context, String key) {
        Map<String, Object> rule = context.getCategoryRule();
        Object value = rule == null ? null : rule.get(key);
        if (value instanceof Map<?, ?> raw) {
            return (Map<String, Object>) raw;
        }
        return Map.of();
    }

    protected boolean boolRule(ProjectSubmitContext context, String key, boolean defaultValue) {
        Map<String, Object> rule = context.getCategoryRule();
        Object value = rule == null ? null : rule.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = String.valueOf(value).trim();
        if (StringUtils.isBlank(text)) {
            return defaultValue;
        }
        if ("1".equals(text) || "true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text) || "on".equalsIgnoreCase(text)) {
            return true;
        }
        if ("0".equals(text) || "false".equalsIgnoreCase(text) || "no".equalsIgnoreCase(text) || "off".equalsIgnoreCase(text)) {
            return false;
        }
        return defaultValue;
    }

    protected List<String> parseStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                String text = String.valueOf(item == null ? "" : item).trim();
                if (StringUtils.isNotBlank(text)) {
                    result.add(text);
                }
            }
            return result;
        }
        String text = String.valueOf(value).trim();
        if (StringUtils.isBlank(text)) {
            return List.of();
        }
        for (String item : text.split("[,，;；、\\s]+")) {
            if (StringUtils.isNotBlank(item)) {
                result.add(item.trim());
            }
        }
        return result;
    }

    protected boolean mediaTechnicalTipsOnly(ProjectSubmitContext context) {
        return mediaTechnicalTipsOnly(context, null);
    }

    protected boolean mediaTechnicalTipsOnly(ProjectSubmitContext context, String mediaType) {
        Map<String, Object> rule = context.getCategoryRule();
        if (technicalTipsOnlyMode(rule == null ? null : rule.get("mediaTechnicalCheckMode"))
            || technicalTipsOnlyMode(rule == null ? null : rule.get("technicalCheckMode"))) {
            return true;
        }
        if (context.getFileRequirements() == null) {
            return false;
        }
        for (CategoryFileRequirement requirement : context.getFileRequirements()) {
            if (fileRequirementTechnicalTipsOnly(requirement, mediaType)) {
                return true;
            }
        }
        return false;
    }

    private boolean fileRequirementTechnicalTipsOnly(CategoryFileRequirement requirement, String mediaType) {
        if (requirement == null) {
            return false;
        }
        Map<String, Object> rule = ProjectSubmitContext.parseJsonObject(requirement.getRuleJson());
        if (!technicalTipsOnlyMode(rule.get("technicalCheckMode")) && !technicalTipsOnlyMode(rule.get("mediaTechnicalCheckMode"))) {
            return false;
        }
        Object requirementMediaType = rule.get("mediaType");
        if (requirementMediaType != null && StringUtils.isNotBlank(String.valueOf(requirementMediaType))) {
            return mediaTypeMatches(String.valueOf(requirementMediaType), mediaType);
        }
        String text = String.join(" ",
            StringUtils.blankToDefault(requirement.getFileTypeCode(), ""),
            StringUtils.blankToDefault(requirement.getFileTypeName(), ""),
            StringUtils.blankToDefault(requirement.getAllowedExt(), "")
        );
        return mediaTypeMatches(text, mediaType);
    }

    private boolean technicalTipsOnlyMode(Object mode) {
        return containsAny(StringUtils.blankToDefault(mode == null ? null : String.valueOf(mode), ""),
            "tip", "tips", "manual", "warn", "format_only", "提示");
    }

    private boolean mediaTypeMatches(String text, String mediaType) {
        if (StringUtils.isBlank(mediaType)) {
            return containsAny(text, "video", "image", "audio", "视频", "图片", "音频", "mp4", "mov", "avi", "mkv", "jpg", "jpeg", "png", "mp3", "wav");
        }
        return switch (StringUtils.blankToDefault(mediaType, "").toLowerCase(Locale.ROOT)) {
            case "video" -> containsAny(text, "video", "视频", "mp4", "mov", "avi", "mkv", "mpeg", "mpg", "webm");
            case "image" -> containsAny(text, "image", "图片", "图像", "jpg", "jpeg", "png", "gif", "webp");
            case "audio" -> containsAny(text, "audio", "音频", "mp3", "wav", "m4a", "aac");
            default -> containsAny(text, mediaType);
        };
    }

    protected boolean hasValidator(ProjectSubmitContext context, String validatorType, String... categoryKeywords) {
        String configuredValidator = context.validatorType();
        if (containsAny(configuredValidator, "none", "disabled", "disable", "off", "no", "false")) {
            return false;
        }
        if (StringUtils.isNotBlank(configuredValidator)) {
            return validatorType.equalsIgnoreCase(configuredValidator);
        }
        return containsAny(context.categoryText(), categoryKeywords);
    }

    protected List<ProjectFile> activeFiles(ProjectSubmitContext context, Predicate<ProjectFile> predicate) {
        return context.getFiles().stream()
            .filter(file -> ArtReviewConstants.FILE_ACTIVE.equals(file.getStatus()))
            .filter(predicate)
            .toList();
    }

    protected boolean isExt(ProjectFile file, String... extValues) {
        String ext = StringUtils.blankToDefault(file.getFileExt(), "").toLowerCase(Locale.ROOT);
        for (String value : extValues) {
            if (value.equals(ext)) {
                return true;
            }
        }
        return false;
    }

    protected void requireFileExt(ProjectSubmitContext context, Predicate<ProjectFile> predicate, String message, String... extValues) {
        for (ProjectFile file : activeFiles(context, predicate)) {
            require(isExt(file, extValues), message + ": " + file.getOriginalName());
        }
    }

    protected void requireMaxDuration(ProjectSubmitContext context, Predicate<ProjectFile> predicate, double maxSeconds, String message) {
        for (ProjectFile file : activeFiles(context, predicate)) {
            require(file.getDurationSeconds() == null || file.getDurationSeconds() <= maxSeconds, message + ": " + file.getOriginalName());
        }
    }

    protected void requireMaxSize(ProjectSubmitContext context, Predicate<ProjectFile> predicate, long maxBytes, String message) {
        for (ProjectFile file : activeFiles(context, predicate)) {
            require(file.getFileSize() == null || file.getFileSize() <= maxBytes, message + ": " + file.getOriginalName());
        }
    }

    protected void requireMinSize(ProjectSubmitContext context, Predicate<ProjectFile> predicate, long minBytes, String message) {
        for (ProjectFile file : activeFiles(context, predicate)) {
            require(file.getFileSize() != null && file.getFileSize() >= minBytes, message + ": " + file.getOriginalName());
        }
    }

    protected void requireSingleProjectPerSchool(ProjectSubmitContext context, String message) {
        Long count = projectMapper.selectCount(Wrappers.lambdaQuery(Project.class)
            .eq(Project::getActivityId, context.getProject().getActivityId())
            .eq(Project::getSchoolId, context.getProject().getSchoolId())
            .eq(Project::getCategoryId, context.getProject().getCategoryId())
            .in(Project::getStatus, ArtReviewConstants.PROJECT_SUBMITTED, ArtReviewConstants.PROJECT_AUDIT_PASSED)
            .ne(context.getProject().getId() != null, Project::getId, context.getProject().getId()));
        require(count == null || count == 0, message);
    }

    protected void requirePersonLimit(ProjectSubmitContext context, String memberType, int maxTimes, String message) {
        for (ProjectMember member : members(context, memberType)) {
            String identity = StringUtils.blankToDefault(member.getStudentNo(), member.getName());
            if (StringUtils.isBlank(identity)) {
                continue;
            }
            List<ProjectMember> samePeople = memberMapper.selectList(Wrappers.lambdaQuery(ProjectMember.class)
                .eq(ProjectMember::getActivityId, context.getProject().getActivityId())
                .eq(ProjectMember::getSchoolId, context.getProject().getSchoolId())
                .eq(ProjectMember::getMemberType, memberType)
                .and(w -> {
                    if (StringUtils.isNotBlank(member.getStudentNo())) {
                        w.eq(ProjectMember::getStudentNo, member.getStudentNo());
                    } else {
                        w.eq(ProjectMember::getName, member.getName());
                    }
                })
                .ne(ProjectMember::getProjectId, context.getProject().getId()));
            int times = 0;
            for (ProjectMember same : samePeople) {
                Project project = projectMapper.selectById(same.getProjectId());
                if (project != null && (ArtReviewConstants.PROJECT_SUBMITTED.equals(project.getStatus())
                    || ArtReviewConstants.PROJECT_AUDIT_PASSED.equals(project.getStatus()))) {
                    times++;
                }
            }
            require(times < maxTimes, message + ": " + identity);
        }
    }

    protected int textLength(ProjectSubmitContext context, String key) {
        return context.field(key).length();
    }

    protected double numberRule(ProjectSubmitContext context, String key, double defaultValue) {
        Map<String, Object> rule = context.getCategoryRule();
        Object value = rule == null ? null : rule.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected int intRule(ProjectSubmitContext context, String key, int defaultValue) {
        return (int) Math.round(numberRule(context, key, defaultValue));
    }

    protected long mb(long mb) {
        return mb * 1024L * 1024L;
    }
}
