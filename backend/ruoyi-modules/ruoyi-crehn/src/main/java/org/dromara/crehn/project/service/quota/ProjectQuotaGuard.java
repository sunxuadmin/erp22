package org.dromara.crehn.project.service.quota;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.config.service.rule.ActivityReportRuleRuntime;
import org.dromara.crehn.config.service.rule.ActivityReportRuleRuntimeService;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.SchoolInfo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.SchoolInfoMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ART-OWNER: BE.PROJECT.QUOTA_GUARD
 * 项目提交前名额和比例规则保护器，只做校验、加锁和预警计算，不保存项目状态。
 */
@RequiredArgsConstructor
@Service
public class ProjectQuotaGuard {

    private final ProjectMapper projectMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final SchoolInfoMapper schoolInfoMapper;
    private final ActivityReportRuleRuntimeService reportRuleRuntimeService;

    /**
     * 只锁学校行来串行化同校提交名额校验，保持原事务边界：由 ArtProjectServiceImpl#submit 开事务。
     */
    public void lockSchoolSubmissionScope(Long schoolId) {
        SchoolInfo school = schoolInfoMapper.selectOne(Wrappers.lambdaQuery(SchoolInfo.class)
            .eq(SchoolInfo::getId, schoolId)
            .last("for update"));
        if (school == null) {
            throw new ServiceException("单位不存在");
        }
    }

    /**
     * 提交前名额校验保持旧语义：count 默认阻断，ratio 默认只写预警，除非规则 JSON 显式要求 block。
     */
    public List<String> validateQuotaRules(Project project) {
        String schoolType = currentSchoolType(project.getSchoolId());
        if (StringUtils.isBlank(schoolType)) {
            throw new ServiceException("当前单位未设置院校类型，请先在单位管理中维护本科院校或高职高专等院校类型");
        }
        List<ActivityReportRuleRuntime> rules = loadEffectiveQuotaRules(project, schoolType);
        if (rules.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> warnings = new ArrayList<>();
        boolean groupRequired = rules.stream().anyMatch(rule -> StringUtils.isNotBlank(rule.getTargetValue())
            && quotaUsesTopLevelGroup(rule)
            && quotaRuleHasCountLimit(rule));
        if (groupRequired && StringUtils.isBlank(project.getGroupCode())) {
            throw new ServiceException("名额规则要求填写组别");
        }
        for (ActivityReportRuleRuntime rule : rules) {
            if (quotaRuleHasCountLimit(rule)) {
                if (!quotaRuleMatchesProject(project, rule)) {
                    continue;
                }
                long count = countSubmittedProjects(project, rule, true);
                if (count + 1 > rule.getLimitCount()) {
                    String message = "名额预警: " + quotaRuleName(rule) + " 当前提交后为 " + (count + 1) + "，上限 " + rule.getLimitCount();
                    if (quotaRuleBlocks(rule, "count")) {
                        throw new ServiceException(message.replace("名额预警", "名额限制"));
                    }
                    warnings.add(message);
                }
            }
            if (rule.hasRatioLimit()) {
                if (!quotaScopeMatchesProject(project, rule)) {
                    continue;
                }
                String warning = checkRatioRuleWarning(project, rule);
                if (StringUtils.isNotBlank(warning)) {
                    if (quotaRuleBlocks(rule, "ratio")) {
                        throw new ServiceException(warning.replace("比例预警", "比例限制"));
                    }
                    warnings.add(warning);
                }
            }
        }
        return warnings;
    }

    private List<ActivityReportRuleRuntime> loadEffectiveQuotaRules(Project project, String schoolType) {
        return reportRuleRuntimeService.listSubmitRules(project, schoolType);
    }

    private boolean quotaRuleHasCountLimit(ActivityReportRuleRuntime rule) {
        return rule.hasCountLimit();
    }

    private String checkRatioRuleWarning(Project project, ActivityReportRuleRuntime rule) {
        long groupCount = countSubmittedProjects(project, rule, true) + 1;
        long totalCount = countSubmittedProjects(project, rule, false) + 1;
        if (totalCount <= 0) {
            return null;
        }
        double ratio = groupCount * 100.0 / totalCount;
        String operator = ratioOperator(rule.getRuleJson());
        double configured = rule.getRatioValue().doubleValue();
        if ("min".equals(operator) && ratio < configured) {
            return "比例预警: " + quotaRuleName(rule) + " 当前 " + String.format(Locale.ROOT, "%.2f", ratio) + "%，低于 " + configured + "%";
        }
        if (!"min".equals(operator) && ratio > configured) {
            return "比例预警: " + quotaRuleName(rule) + " 当前 " + String.format(Locale.ROOT, "%.2f", ratio) + "%，超过 " + configured + "%";
        }
        return null;
    }

    private long countSubmittedProjects(Project project, ActivityReportRuleRuntime rule, boolean includeGroup) {
        String targetFieldKey = quotaTargetFieldKey(rule);
        if ((includeGroup && StringUtils.isNotBlank(targetFieldKey)) || quotaHasDynamicScope(rule)) {
            List<Project> projects = projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
                .eq(Project::getActivityId, project.getActivityId())
                .eq(Project::getSchoolId, project.getSchoolId())
                .in(Project::getStatus, ArtReviewConstants.PROJECT_SUBMITTED, ArtReviewConstants.PROJECT_AUDIT_PASSED)
                .ne(project.getId() != null, Project::getId, project.getId())
                .eq(rule.getCategoryId() != null, Project::getCategoryId, project.getCategoryId()));
            return projects.stream().filter(row -> quotaRuleMatchesProject(row, rule, includeGroup)).count();
        }
        return projectMapper.selectCount(Wrappers.lambdaQuery(Project.class)
            .eq(Project::getActivityId, project.getActivityId())
            .eq(Project::getSchoolId, project.getSchoolId())
            .in(Project::getStatus, ArtReviewConstants.PROJECT_SUBMITTED, ArtReviewConstants.PROJECT_AUDIT_PASSED)
            .ne(project.getId() != null, Project::getId, project.getId())
            .eq(rule.getCategoryId() != null, Project::getCategoryId, project.getCategoryId())
            .eq(includeGroup && StringUtils.isNotBlank(rule.getTargetValue()), Project::getGroupCode, rule.getTargetValue()));
    }

    private boolean quotaRuleMatchesProject(Project project, ActivityReportRuleRuntime rule) {
        return quotaRuleMatchesProject(project, rule, true);
    }

    private boolean quotaRuleMatchesProject(Project project, ActivityReportRuleRuntime rule, boolean includeGroup) {
        if (!quotaScopeMatchesProject(project, rule)) {
            return false;
        }
        if (!includeGroup) {
            return true;
        }
        String targetFieldKey = quotaTargetFieldKey(rule);
        if (StringUtils.isBlank(targetFieldKey)) {
            return StringUtils.isBlank(rule.getTargetValue()) || rule.getTargetValue().equals(project.getGroupCode());
        }
        String actual = quotaTargetValue(project, targetFieldKey);
        if (StringUtils.isBlank(actual)) {
            return false;
        }
        Map<String, Object> options = parseQuotaRuleJson(rule.getRuleJson());
        String excludeTargetValue = stringOption(options, "excludeTargetValue", "excludeValue");
        if (StringUtils.isNotBlank(excludeTargetValue)
            && Arrays.stream(excludeTargetValue.split("[,，\\s]+")).filter(StringUtils::isNotBlank).anyMatch(actual::equals)) {
            return false;
        }
        if (StringUtils.isBlank(rule.getTargetValue())) {
            return true;
        }
        if (rule.getTargetValue().equals(actual)) {
            return true;
        }
        return actual.contains(rule.getTargetValue()) || rule.getTargetValue().contains(actual);
    }

    private boolean quotaScopeMatchesProject(Project project, ActivityReportRuleRuntime rule) {
        Map<String, Object> options = parseQuotaRuleJson(rule.getRuleJson());
        String categoryGroupScope = stringOption(options, "scopeCategoryGroup", "categoryGroupScope");
        String categoryCodeScope = stringOption(options, "scopeCategoryCodes", "categoryCodeScope");
        if (StringUtils.isBlank(categoryGroupScope) && StringUtils.isBlank(categoryCodeScope)) {
            return true;
        }
        ActivityCategory category = categoryMapper.selectById(project.getCategoryId());
        if (category == null) {
            return false;
        }
        if (StringUtils.isNotBlank(categoryGroupScope) && !categoryGroupScope.equals(category.getCategoryGroup())) {
            return false;
        }
        if (StringUtils.isBlank(categoryCodeScope)) {
            return true;
        }
        return Arrays.stream(categoryCodeScope.split("[,，\\s]+"))
            .filter(StringUtils::isNotBlank)
            .anyMatch(code -> code.equals(category.getCategoryCode()));
    }

    private String quotaTargetValue(Project project, String targetFieldKey) {
        if ("groupCode".equals(targetFieldKey) || "__group_code".equals(targetFieldKey)) {
            return StringUtils.blankToDefault(project.getGroupCode(), "");
        }
        if ("categoryCode".equals(targetFieldKey) || "__category_code".equals(targetFieldKey)
            || "categoryGroup".equals(targetFieldKey) || "__category_group".equals(targetFieldKey)) {
            ActivityCategory category = categoryMapper.selectById(project.getCategoryId());
            if (category == null) {
                return "";
            }
            if ("categoryGroup".equals(targetFieldKey) || "__category_group".equals(targetFieldKey)) {
                return StringUtils.blankToDefault(category.getCategoryGroup(), "");
            }
            return StringUtils.blankToDefault(category.getCategoryCode(), "");
        }
        return String.valueOf(parseRuleObject(project.getFormDataJson()).getOrDefault(targetFieldKey, ""));
    }

    private boolean quotaHasDynamicScope(ActivityReportRuleRuntime rule) {
        Map<String, Object> options = parseQuotaRuleJson(rule.getRuleJson());
        return StringUtils.isNotBlank(quotaTargetFieldKey(rule))
            || StringUtils.isNotBlank(stringOption(options, "scopeCategoryGroup", "categoryGroupScope"))
            || StringUtils.isNotBlank(stringOption(options, "scopeCategoryCodes", "categoryCodeScope"));
    }

    private String quotaTargetFieldKey(ActivityReportRuleRuntime rule) {
        Map<String, Object> options = parseQuotaRuleJson(rule.getRuleJson());
        return stringOption(options, "targetFieldKey", "groupFieldKey");
    }

    private boolean quotaUsesTopLevelGroup(ActivityReportRuleRuntime rule) {
        String targetFieldKey = quotaTargetFieldKey(rule);
        return StringUtils.isBlank(targetFieldKey)
            || "groupCode".equals(targetFieldKey)
            || "__group_code".equals(targetFieldKey);
    }

    private String ratioOperator(String ruleJson) {
        Map<String, Object> rule = parseQuotaRuleJson(ruleJson);
        Object operator = rule.get("ratioOperator");
        return operator == null ? "max" : String.valueOf(operator);
    }

    private boolean quotaRuleBlocks(ActivityReportRuleRuntime rule, String checkType) {
        Map<String, Object> options = parseQuotaRuleJson(rule.getRuleJson());
        String enforceMode = stringOption(options, "enforceMode", "enforcement", "mode");
        if (StringUtils.isNotBlank(enforceMode)) {
            return "block".equalsIgnoreCase(enforceMode) || "force".equalsIgnoreCase(enforceMode);
        }
        return "count".equals(checkType);
    }

    private Map<String, Object> parseRuleObject(String text) {
        if (StringUtils.isBlank(text) || !JsonUtils.isJsonObject(text)) {
            return Map.of();
        }
        Map<String, Object> parsed = JsonUtils.parseObject(text, new TypeReference<Map<String, Object>>() {
        });
        return parsed == null ? Map.of() : parsed;
    }

    private Map<String, Object> parseQuotaRuleJson(String ruleJson) {
        return parseRuleObject(ruleJson);
    }

    private String stringOption(Map<String, Object> options, String... keys) {
        for (String key : keys) {
            Object value = options.get(key);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private String quotaRuleName(ActivityReportRuleRuntime rule) {
        return rule.label();
    }

    private String currentSchoolType(Long schoolId) {
        if (schoolId == null) {
            return null;
        }
        SchoolInfo school = schoolInfoMapper.selectById(schoolId);
        return school == null ? null : school.getSchoolType();
    }
}
