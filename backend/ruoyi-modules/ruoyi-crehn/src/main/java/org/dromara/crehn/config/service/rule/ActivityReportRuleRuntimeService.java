package org.dromara.crehn.config.service.rule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.ActivityReportRule;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.SchoolInfo;
import org.dromara.crehn.domain.vo.ActivityRuleGroupOptionVo;
import org.dromara.crehn.mapper.ActivityReportRuleMapper;
import org.dromara.crehn.mapper.SchoolInfoMapper;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ActivityReportRuleRuntimeService {

    private final ActivityReportRuleMapper activityReportRuleMapper;
    private final SchoolInfoMapper schoolInfoMapper;
    private final ISysUserService userService;

    public List<ActivityReportRuleRuntime> listSubmitRules(Project project, String schoolType) {
        List<ActivityReportRule> rules = activityReportRuleMapper.selectList(Wrappers.lambdaQuery(ActivityReportRule.class)
            .eq(ActivityReportRule::getActivityId, project.getActivityId())
            .eq(ActivityReportRule::getEnabled, true)
            .and(w -> w.isNull(ActivityReportRule::getCategoryId).or().eq(ActivityReportRule::getCategoryId, project.getCategoryId()))
            .and(w -> w.isNull(ActivityReportRule::getSchoolId).or().eq(ActivityReportRule::getSchoolId, project.getSchoolId()))
            .and(w -> w.isNull(ActivityReportRule::getSchoolType).or().eq(ActivityReportRule::getSchoolType, schoolType))
            .orderByDesc(ActivityReportRule::getSchoolId)
            .orderByDesc(ActivityReportRule::getCategoryId)
            .orderByAsc(ActivityReportRule::getSortOrder)
            .orderByAsc(ActivityReportRule::getId));
        return rules.stream().map(this::toRuntime).toList();
    }

    public List<ActivityReportRuleRuntime> listOverviewRules(Long activityId, Long schoolId, String schoolType) {
        if (activityId == null) {
            return List.of();
        }
        LambdaQueryWrapper<ActivityReportRule> wrapper = Wrappers.lambdaQuery(ActivityReportRule.class)
            .eq(ActivityReportRule::getActivityId, activityId)
            .eq(ActivityReportRule::getEnabled, true)
            .orderByDesc(ActivityReportRule::getSchoolId)
            .orderByDesc(ActivityReportRule::getCategoryId)
            .orderByAsc(ActivityReportRule::getSortOrder)
            .orderByAsc(ActivityReportRule::getId);
        applySchoolScope(wrapper, schoolId, schoolType);
        return activityReportRuleMapper.selectList(wrapper).stream().map(this::toRuntime).toList();
    }

    public List<ActivityReportRuleRuntime> listRatioRules(Long activityId, Long schoolId, String schoolType) {
        if (activityId == null) {
            return List.of();
        }
        LambdaQueryWrapper<ActivityReportRule> wrapper = Wrappers.lambdaQuery(ActivityReportRule.class)
            .eq(ActivityReportRule::getActivityId, activityId)
            .eq(ActivityReportRule::getEnabled, true)
            .isNotNull(ActivityReportRule::getRatioValue)
            .and(w -> w.eq(ActivityReportRule::getRuleType, "ratio").or().eq(ActivityReportRule::getRuleType, "mixed"))
            .orderByAsc(ActivityReportRule::getSortOrder)
            .orderByAsc(ActivityReportRule::getId);
        if (schoolId != null || StringUtils.isNotBlank(schoolType)) {
            applySchoolScope(wrapper, schoolId, schoolType);
        }
        return activityReportRuleMapper.selectList(wrapper).stream().map(this::toRuntime).toList();
    }

    public List<ActivityRuleGroupOptionVo> listSchoolGroupOptions(ActivityReportRule query) {
        Long schoolId = currentSchoolId();
        String schoolType = currentSchoolType(schoolId);
        return activityReportRuleMapper.selectList(Wrappers.lambdaQuery(ActivityReportRule.class)
            .eq(query.getActivityId() != null, ActivityReportRule::getActivityId, query.getActivityId())
            .eq(ActivityReportRule::getEnabled, true)
            .and(query.getCategoryId() != null, w -> w.isNull(ActivityReportRule::getCategoryId).or().eq(ActivityReportRule::getCategoryId, query.getCategoryId()))
            .and(schoolId == null, w -> w.isNull(ActivityReportRule::getSchoolId))
            .and(schoolId != null, w -> w.isNull(ActivityReportRule::getSchoolId).or().eq(ActivityReportRule::getSchoolId, schoolId))
            .and(StringUtils.isBlank(schoolType), w -> w.isNull(ActivityReportRule::getSchoolType))
            .and(StringUtils.isNotBlank(schoolType), w -> w.isNull(ActivityReportRule::getSchoolType).or().eq(ActivityReportRule::getSchoolType, schoolType))
            .orderByAsc(ActivityReportRule::getCategoryId)
            .orderByAsc(ActivityReportRule::getSortOrder)
            .orderByAsc(ActivityReportRule::getId))
            .stream()
            .map(this::toRuntime)
            .map(this::toGroupOption)
            .toList();
    }

    public String currentSchoolType(Long schoolId) {
        if (schoolId == null) {
            return null;
        }
        SchoolInfo school = schoolInfoMapper.selectById(schoolId);
        return school == null ? null : school.getSchoolType();
    }

    private void applySchoolScope(LambdaQueryWrapper<ActivityReportRule> wrapper, Long schoolId, String schoolType) {
        if (schoolId == null) {
            wrapper.isNull(ActivityReportRule::getSchoolId);
        } else {
            wrapper.and(w -> w.isNull(ActivityReportRule::getSchoolId).or().eq(ActivityReportRule::getSchoolId, schoolId));
        }
        if (StringUtils.isBlank(schoolType)) {
            wrapper.isNull(ActivityReportRule::getSchoolType);
        } else {
            wrapper.and(w -> w.isNull(ActivityReportRule::getSchoolType).or().eq(ActivityReportRule::getSchoolType, schoolType));
        }
    }

    private ActivityReportRuleRuntime toRuntime(ActivityReportRule rule) {
        ActivityReportRuleRuntime runtime = new ActivityReportRuleRuntime();
        runtime.setId(rule.getId());
        runtime.setActivityId(rule.getActivityId());
        runtime.setCategoryId(rule.getCategoryId());
        runtime.setSchoolId(rule.getSchoolId());
        runtime.setSchoolType(rule.getSchoolType());
        runtime.setRuleName(rule.getRuleName());
        runtime.setRuleGroup(rule.getRuleGroup());
        runtime.setRuleType(rule.getRuleType());
        runtime.setScopeType(rule.getScopeType());
        runtime.setScopeCategoryGroup(rule.getScopeCategoryGroup());
        runtime.setCategoryCode(rule.getCategoryCode());
        runtime.setTargetFieldKey(rule.getTargetFieldKey());
        runtime.setTargetValue(rule.getTargetValue());
        runtime.setDisplayName(StringUtils.blankToDefault(rule.getRuleName(), rule.getTargetValue()));
        runtime.setOperator(rule.getOperator());
        runtime.setLimitCount(rule.getLimitCount());
        runtime.setRatioValue(rule.getRatioValue());
        runtime.setEnforceMode(rule.getEnforceMode());
        runtime.setMessage(rule.getMessage());
        runtime.setRuleJson(runtimeRuleJson(rule));
        runtime.setEnabled(rule.getEnabled());
        runtime.setSortOrder(rule.getSortOrder());
        runtime.setRemark(rule.getRemark());
        runtime.setCreateTime(rule.getCreateTime());
        return runtime;
    }

    private ActivityRuleGroupOptionVo toGroupOption(ActivityReportRuleRuntime rule) {
        ActivityRuleGroupOptionVo vo = new ActivityRuleGroupOptionVo();
        vo.setId(rule.getId());
        vo.setActivityId(rule.getActivityId());
        vo.setCategoryId(rule.getCategoryId());
        vo.setSchoolId(rule.getSchoolId());
        vo.setSchoolType(rule.getSchoolType());
        vo.setGroupCode(rule.getTargetValue());
        vo.setGroupName(StringUtils.blankToDefault(rule.getDisplayName(), rule.getTargetValue()));
        vo.setRuleType(rule.normalizedRuleType());
        vo.setLimitCount(rule.getLimitCount());
        vo.setRatioValue(rule.getRatioValue());
        vo.setRuleJson(rule.getRuleJson());
        vo.setEnabled(rule.getEnabled());
        vo.setRemark(rule.displayMessage());
        vo.setCreateTime(rule.getCreateTime());
        return vo;
    }

    private Long currentSchoolId() {
        SysUserVo user = userService.selectUserById(LoginHelper.getUserId());
        return user == null ? null : user.getSchoolId();
    }

    private String runtimeRuleJson(ActivityReportRule reportRule) {
        Map<String, Object> rule = new LinkedHashMap<>(parseRuleObject(reportRule.getRuleJson()));
        putIfNotBlank(rule, "targetFieldKey", reportRule.getTargetFieldKey());
        putIfNotBlank(rule, "scopeCategoryGroup", reportRule.getScopeCategoryGroup());
        putIfNotBlank(rule, "categoryCode", reportRule.getCategoryCode());
        putIfNotBlank(rule, "ratioOperator", reportRule.getOperator());
        putIfNotBlank(rule, "enforceMode", reportRule.getEnforceMode());
        putIfNotBlank(rule, "message", reportRule.getMessage());
        return rule.isEmpty() ? null : JsonUtils.toJsonString(rule);
    }

    private Map<String, Object> parseRuleObject(String text) {
        if (StringUtils.isBlank(text) || !JsonUtils.isJsonObject(text)) {
            return Map.of();
        }
        Map<String, Object> parsed = JsonUtils.parseObject(text, new TypeReference<Map<String, Object>>() {
        });
        return parsed == null ? Map.of() : parsed;
    }

    private void putIfNotBlank(Map<String, Object> rule, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            rule.put(key, value);
        }
    }
}
