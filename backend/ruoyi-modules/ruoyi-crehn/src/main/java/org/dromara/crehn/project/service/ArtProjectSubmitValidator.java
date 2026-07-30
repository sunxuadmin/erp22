package org.dromara.crehn.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.CategoryFieldSchema;
import org.dromara.crehn.domain.CategoryFileRequirement;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ProjectFile;
import org.dromara.crehn.domain.ProjectMember;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.CategoryFieldSchemaMapper;
import org.dromara.crehn.mapper.CategoryFileRequirementMapper;
import org.dromara.crehn.mapper.ProjectFileMapper;
import org.dromara.crehn.mapper.ProjectMemberMapper;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ArtProjectSubmitValidator {

    private final ActivityCategoryMapper categoryMapper;
    private final CategoryFieldSchemaMapper fieldSchemaMapper;
    private final CategoryFileRequirementMapper fileRequirementMapper;
    private final ProjectFileMapper projectFileMapper;
    private final ProjectMemberMapper memberMapper;
    private final FileRequirementRuleChecker fileRequirementRuleChecker;
    private final List<ProjectCategorySubmitValidator> validators;

    public SubmitValidationResult validate(Project project) {
        ProjectSubmitContext context = buildContext(project);
        addManualTips(context);
        validateFieldSchemas(context);
        validateGenericTables(context);
        validateMemberCountRules(context);
        for (ProjectCategorySubmitValidator validator : validators) {
            if (validator.supports(context)) {
                validator.validate(context);
            }
        }
        return context.getResult();
    }

    private ProjectSubmitContext buildContext(Project project) {
        ProjectSubmitContext context = new ProjectSubmitContext();
        ActivityCategory category = categoryMapper.selectById(project.getCategoryId());
        context.setProject(project);
        context.setCategory(category);
        context.setMembers(memberMapper.selectList(Wrappers.lambdaQuery(ProjectMember.class)
            .eq(ProjectMember::getProjectId, project.getId())
            .orderByAsc(ProjectMember::getMemberType)
            .orderByAsc(ProjectMember::getSortOrder)
            .orderByAsc(ProjectMember::getId)));
        context.setFieldSchemas(fieldSchemaMapper.selectList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
            .eq(CategoryFieldSchema::getCategoryId, project.getCategoryId())
            .eq(CategoryFieldSchema::getDelFlag, SystemConstants.NORMAL)
            .orderByAsc(CategoryFieldSchema::getSortOrder)
            .orderByAsc(CategoryFieldSchema::getId)));
        context.setFiles(projectFileMapper.selectList(Wrappers.lambdaQuery(ProjectFile.class)
            .eq(ProjectFile::getProjectId, project.getId())
            .eq(ProjectFile::getStatus, ArtReviewConstants.FILE_ACTIVE)));
        context.setFileRequirements(fileRequirementMapper.selectList(Wrappers.lambdaQuery(CategoryFileRequirement.class)
            .eq(CategoryFileRequirement::getCategoryId, project.getCategoryId())));
        context.setFormData(ProjectSubmitContext.parseJsonObject(project.getFormDataJson()));
        context.setCategoryRule(category == null ? Map.of() : ProjectSubmitContext.parseJsonObject(category.getRuleJson()));
        context.setResult(new SubmitValidationResult());
        return context;
    }

    private void validateFieldSchemas(ProjectSubmitContext context) {
        if (context.getFieldSchemas() == null || context.getFieldSchemas().isEmpty()) {
            return;
        }
        Map<String, Object> formData = context.getFormData() == null ? Map.of() : context.getFormData();
        for (CategoryFieldSchema field : context.getFieldSchemas()) {
            if (field == null || StringUtils.isBlank(field.getFieldKey())) {
                continue;
            }
            Object value = formData.get(field.getFieldKey());
            String label = StringUtils.blankToDefault(field.getFieldLabel(), field.getFieldKey());
            Map<String, Object> rule = ProjectSubmitContext.parseJsonObject(field.getValidationJson());
            if (Boolean.TRUE.equals(field.getRequired()) && isBlankValue(value)) {
                throw new ServiceException(label + "不能为空");
            }
            if ("teacher_group".equals(StringUtils.blankToDefault(field.getFieldType(), ""))) {
                validateTeacherGroupField(label, value, rule, Boolean.TRUE.equals(field.getRequired()));
            }
            if (isBlankValue(value)) {
                continue;
            }
            validateTextField(field, label, value, rule);
            validateKeywordCountField(label, value, rule);
            validateNumberField(field, label, value, rule);
        }
    }

    private void validateMemberCountRules(ProjectSubmitContext context) {
        Map<String, Object> categoryRule = context.getCategoryRule() == null ? Map.of() : context.getCategoryRule();
        if (Boolean.FALSE.equals(categoryRule.get("memberTableEnabled"))) {
            return;
        }
        Object rulesValue = categoryRule.get("memberCountRules");
        if (!(rulesValue instanceof Map<?, ?> rules)) {
            return;
        }
        long total = context.getMembers() == null ? 0 : context.getMembers().size();
        validateMemberCountRule("成员", total, rules.get("total"));
        validateMemberCountRule("作者", countMemberType(context, "author"), rules.get("author"));
        validateMemberCountRule("学生", countMemberType(context, "student"), rules.get("student"));
        validateMemberCountRule("指导教师", countMemberType(context, "teacher"), rules.get("teacher"));
        validateMemberCountRule("完成人", countMemberType(context, "completer"), rules.get("completer"));
    }

    private void validateGenericTables(ProjectSubmitContext context) {
        Map<String, Object> categoryRule = context.getCategoryRule() == null ? Map.of() : context.getCategoryRule();
        Object templatesValue = categoryRule.get("genericTableTemplates");
        if (!(templatesValue instanceof Iterable<?> templates)) {
            return;
        }
        Map<?, ?> tableData = Map.of();
        Object genericDataValue = context.getFormData() == null ? null : context.getFormData().get("__genericTables");
        if (genericDataValue instanceof Map<?, ?> map) {
            tableData = map;
        }
        for (Object templateValue : templates) {
            if (!(templateValue instanceof Map<?, ?> template) || Boolean.FALSE.equals(template.get("enabled"))) {
                continue;
            }
            String tableKey = StringUtils.trim(genericText(template.get("templateKey")));
            String tableName = StringUtils.blankToDefault(StringUtils.trim(genericText(template.get("templateName"))), tableKey);
            if (StringUtils.isBlank(tableKey)) {
                continue;
            }
            List<Map<?, ?>> rows = genericTableRows(tableData.get(tableKey), template.get("fields"));
            if (rows.size() > 500) {
                throw new ServiceException("“" + tableName + "”不能超过 500 行");
            }
            Integer minRows = nonNegativeInteger(template.get("minRows"));
            Integer maxRows = positiveInteger(template.get("maxRows"));
            if (minRows != null && rows.size() < minRows) {
                throw new ServiceException("“" + tableName + "”至少填写 " + minRows + " 行");
            }
            if (maxRows != null && rows.size() > maxRows) {
                throw new ServiceException("“" + tableName + "”最多填写 " + maxRows + " 行");
            }
            validateGenericTableRows(tableName, rows, template.get("fields"));
        }
    }

    private List<Map<?, ?>> genericTableRows(Object rowsValue, Object fieldsValue) {
        if (!(rowsValue instanceof Iterable<?> rows)) {
            return List.of();
        }
        List<String> fieldKeys = genericTableFields(fieldsValue).stream()
            .map(field -> StringUtils.trim(genericText(field.get("fieldKey"))))
            .filter(StringUtils::isNotBlank)
            .toList();
        java.util.ArrayList<Map<?, ?>> result = new java.util.ArrayList<>();
        for (Object rowValue : rows) {
            if (!(rowValue instanceof Map<?, ?> row)) {
                continue;
            }
            boolean filled = fieldKeys.stream().anyMatch(key -> !isBlankValue(row.get(key)));
            if (filled) {
                result.add(row);
            }
        }
        return result;
    }

    private void validateGenericTableRows(String tableName, List<Map<?, ?>> rows, Object fieldsValue) {
        List<Map<?, ?>> fields = genericTableFields(fieldsValue);
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Map<?, ?> row = rows.get(rowIndex);
            for (Map<?, ?> field : fields) {
                String fieldKey = StringUtils.trim(genericText(field.get("fieldKey")));
                if (StringUtils.isBlank(fieldKey)) {
                    continue;
                }
                String fieldLabel = StringUtils.blankToDefault(StringUtils.trim(genericText(field.get("fieldLabel"))), fieldKey);
                Object value = row.get(fieldKey);
                String label = "“" + tableName + "”第 " + (rowIndex + 1) + " 行“" + fieldLabel + "”";
                if (Boolean.TRUE.equals(field.get("required")) && isBlankValue(value)) {
                    throw new ServiceException(label + "不能为空");
                }
                if (isBlankValue(value)) {
                    continue;
                }
                String fieldType = StringUtils.blankToDefault(genericText(field.get("fieldType")), "input");
                if ("number".equals(fieldType)) {
                    BigDecimal number = decimalValue(value);
                    if (number == null) {
                        throw new ServiceException(label + "必须为数字");
                    }
                    validateGenericNumber(label, number, genericValidationType(field.get("validationJson")));
                } else {
                    validateGenericText(label, String.valueOf(value), genericValidationType(field.get("validationJson")));
                }
                validateGenericOptions(label, value, field.get("optionsJson"));
            }
        }
    }

    private List<Map<?, ?>> genericTableFields(Object fieldsValue) {
        if (!(fieldsValue instanceof Iterable<?> fields)) {
            return List.of();
        }
        java.util.ArrayList<Map<?, ?>> result = new java.util.ArrayList<>();
        for (Object value : fields) {
            if (value instanceof Map<?, ?> field) {
                result.add(field);
            }
        }
        return result;
    }

    private String genericValidationType(Object validationValue) {
        if (validationValue instanceof Map<?, ?> validation) {
            return genericText(validation.get("type"));
        }
        String text = validationValue == null ? "" : String.valueOf(validationValue);
        if (StringUtils.isBlank(text)) {
            return "";
        }
        Map<String, Object> validation = ProjectSubmitContext.parseJsonObject(text);
        return StringUtils.blankToDefault(genericText(validation.get("type")), text);
    }

    private String genericText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void validateGenericNumber(String label, BigDecimal number, String validationType) {
        if ("integer".equals(validationType) && number.stripTrailingZeros().scale() > 0) {
            throw new ServiceException(label + "必须为整数");
        }
        if ("nonnegative".equals(validationType) && number.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException(label + "不能小于 0");
        }
    }

    private void validateGenericText(String label, String value, String validationType) {
        boolean valid = switch (validationType) {
            case "phone" -> value.matches("^1\\d{10}$");
            case "email" -> value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
            case "idcard", "id_card" -> value.matches("(^\\d{15}$)|(^\\d{17}[0-9Xx]$)");
            default -> true;
        };
        if (!valid) {
            throw new ServiceException(label + "格式不正确");
        }
    }

    private void validateGenericOptions(String label, Object value, Object optionsValue) {
        List<String> options = genericOptions(optionsValue);
        if (options.isEmpty()) {
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (item != null && !options.contains(String.valueOf(item))) {
                    throw new ServiceException(label + "不在可选范围内");
                }
            }
            return;
        }
        if (!options.contains(String.valueOf(value))) {
            throw new ServiceException(label + "不在可选范围内");
        }
    }

    private List<String> genericOptions(Object optionsValue) {
        if (optionsValue instanceof Iterable<?> iterable) {
            java.util.ArrayList<String> result = new java.util.ArrayList<>();
            iterable.forEach(item -> {
                if (item != null && StringUtils.isNotBlank(String.valueOf(item))) {
                    result.add(String.valueOf(item).trim());
                }
            });
            return result;
        }
        String text = optionsValue == null ? "" : String.valueOf(optionsValue);
        if (StringUtils.isBlank(text)) {
            return List.of();
        }
        try {
            List<String> result = JsonUtils.parseObject(text, new TypeReference<List<String>>() {
            });
            return result == null ? List.of() : result;
        } catch (RuntimeException e) {
            return Arrays.stream(text.split("[,，、;；\\n]+")).map(String::trim).filter(StringUtils::isNotBlank).toList();
        }
    }

    private long countMemberType(ProjectSubmitContext context, String memberType) {
        if (context.getMembers() == null || context.getMembers().isEmpty()) {
            return 0;
        }
        return context.getMembers().stream()
            .filter(member -> member != null && memberType.equals(StringUtils.blankToDefault(member.getMemberType(), "student")))
            .count();
    }

    private void validateMemberCountRule(String label, long count, Object ruleValue) {
        if (!(ruleValue instanceof Map<?, ?> rule)) {
            return;
        }
        Integer min = nonNegativeInteger(rule.get("min"));
        Integer max = nonNegativeInteger(rule.get("max"));
        if (min != null && count < min) {
            throw new ServiceException(label + "人数应不少于 " + min + " 人");
        }
        if (max != null && count > max) {
            throw new ServiceException(label + "人数应不超过 " + max + " 人");
        }
    }

    private void validateTeacherGroupField(String label, Object value, Map<String, Object> rule, boolean required) {
        long count = ProjectSubmitContext.countFilledRows(value);
        Integer minItems = nonNegativeInteger(rule.get("minItems"));
        if (minItems == null && required) {
            minItems = 1;
        }
        Object maxItemsValue = rule.get("maxItems") == null ? rule.get("maxTeacherCount") : rule.get("maxItems");
        Integer maxItems = positiveInteger(maxItemsValue);
        if (minItems != null && count < minItems) {
            throw new ServiceException(label + "至少需要填写 " + minItems + " 人");
        }
        if (maxItems != null && count > maxItems) {
            throw new ServiceException(label + "不能超过 " + maxItems + " 人");
        }
    }

    private void validateTextField(CategoryFieldSchema field, String label, Object value, Map<String, Object> rule) {
        String fieldType = StringUtils.blankToDefault(field.getFieldType(), "");
        if (!List.of("input", "textarea", "id_card").contains(fieldType)) {
            return;
        }
        String text = String.valueOf(value).trim();
        Integer minLength = positiveInteger(rule.get("minLength"));
        Integer maxLength = positiveInteger(rule.get("maxLength"));
        if (minLength != null && text.length() < minLength) {
            throw new ServiceException(label + "不能少于 " + minLength + " 个字符");
        }
        if (maxLength != null && text.length() > maxLength) {
            throw new ServiceException(label + "不能超过 " + maxLength + " 个字符");
        }
    }

    private void validateKeywordCountField(String label, Object value, Map<String, Object> rule) {
        if (!"keyword_count".equals(StringUtils.blankToDefault(String.valueOf(rule.get("type")), ""))) {
            return;
        }
        int count = keywordCount(String.valueOf(value));
        Integer minCount = nonNegativeInteger(rule.get("minKeywordCount"));
        Integer maxCount = nonNegativeInteger(rule.get("maxKeywordCount"));
        if (minCount == null) {
            minCount = 3;
        }
        if (maxCount == null) {
            maxCount = 5;
        }
        if (count < minCount || (maxCount > 0 && count > maxCount)) {
            throw new ServiceException(label + "应为 " + minCount + " 至 " + maxCount + " 个");
        }
    }

    private int keywordCount(String text) {
        if (StringUtils.isBlank(text)) {
            return 0;
        }
        return (int) Arrays.stream(text.split("[,，;；、\\s]+")).filter(StringUtils::isNotBlank).count();
    }

    private void validateNumberField(CategoryFieldSchema field, String label, Object value, Map<String, Object> rule) {
        if (!"number".equals(StringUtils.blankToDefault(field.getFieldType(), ""))) {
            return;
        }
        BigDecimal number = decimalValue(value);
        if (number == null) {
            throw new ServiceException(label + "必须为数字");
        }
        BigDecimal min = decimalValue(rule.get("min"));
        BigDecimal max = decimalValue(rule.get("max"));
        if (min != null && number.compareTo(min) < 0) {
            throw new ServiceException(label + "不能小于 " + min.stripTrailingZeros().toPlainString());
        }
        if (max != null && number.compareTo(max) > 0) {
            throw new ServiceException(label + "不能大于 " + max.stripTrailingZeros().toPlainString());
        }
        boolean integerOnly = Boolean.TRUE.equals(rule.get("integerOnly"));
        Integer precision = nonNegativeInteger(rule.get("precision"));
        if (integerOnly && number.stripTrailingZeros().scale() > 0) {
            throw new ServiceException(label + "必须为整数");
        }
        if (!integerOnly && precision != null && number.stripTrailingZeros().scale() > precision) {
            throw new ServiceException(label + "最多保留 " + precision + " 位小数");
        }
    }

    private boolean isBlankValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof Iterable<?> iterable) {
            return !iterable.iterator().hasNext();
        }
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        return StringUtils.isBlank(String.valueOf(value));
    }

    private Integer positiveInteger(Object value) {
        Integer number = nonNegativeInteger(value);
        return number != null && number > 0 ? number : null;
    }

    private Integer nonNegativeInteger(Object value) {
        BigDecimal number = decimalValue(value);
        if (number == null || number.compareTo(BigDecimal.ZERO) < 0) {
            return null;
        }
        return number.intValue();
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void addManualTips(ProjectSubmitContext context) {
        Object tips = context.getCategoryRule() == null ? null : context.getCategoryRule().get("manualCheckTips");
        addTips(context.getResult(), tips);
        Object rejectTemplates = context.getCategoryRule() == null ? null : context.getCategoryRule().get("rejectReasonTemplates");
        addRejectReasonTemplates(context.getResult(), rejectTemplates);
        for (CategoryFileRequirement requirement : context.getFileRequirements()) {
            fileRequirementRuleChecker.manualTips(requirement).forEach(context.getResult()::manualTip);
        }
    }

    private void addTips(SubmitValidationResult result, Object tips) {
        if (tips instanceof Iterable<?> iterable) {
            for (Object tip : iterable) {
                if (tip != null && StringUtils.isNotBlank(String.valueOf(tip))) {
                    result.manualTip(String.valueOf(tip));
                }
            }
            return;
        }
        if (tips instanceof String text && StringUtils.isNotBlank(text)) {
            if (JsonUtils.isJsonArray(text)) {
                List<String> values = JsonUtils.parseObject(text, new TypeReference<List<String>>() {
                });
                if (values != null) {
                    values.forEach(result::manualTip);
                }
            } else {
                result.manualTip(text);
            }
        }
    }

    private void addRejectReasonTemplates(SubmitValidationResult result, Object templates) {
        if (templates instanceof Iterable<?> iterable) {
            for (Object template : iterable) {
                if (template != null && StringUtils.isNotBlank(String.valueOf(template))) {
                    result.rejectReasonTemplate(String.valueOf(template));
                }
            }
            return;
        }
        if (templates instanceof String text && StringUtils.isNotBlank(text)) {
            if (JsonUtils.isJsonArray(text)) {
                List<String> values = JsonUtils.parseObject(text, new TypeReference<List<String>>() {
                });
                if (values != null) {
                    values.forEach(result::rejectReasonTemplate);
                }
            } else {
                result.rejectReasonTemplate(text);
            }
        }
    }
}
