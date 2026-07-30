package org.dromara.crehn.result.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.audit.service.IArtAuditAssignmentService;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.common.ArtReviewSecurity;
import org.dromara.crehn.config.service.rule.ActivityReportRuleRuntime;
import org.dromara.crehn.config.service.rule.ActivityReportRuleRuntimeService;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ReviewAwardRule;
import org.dromara.crehn.domain.ReviewAssignment;
import org.dromara.crehn.domain.ReviewResult;
import org.dromara.crehn.domain.ReviewResultLog;
import org.dromara.crehn.domain.ReviewScore;
import org.dromara.crehn.domain.ReviewScoreSheetSignedSheet;
import org.dromara.crehn.domain.ReviewScoreSheetSignedSheetItem;
import org.dromara.crehn.domain.SchoolInfo;
import org.dromara.crehn.domain.bo.ReviewAwardRuleBo;
import org.dromara.crehn.domain.bo.ReviewAwardRuleItemBo;
import org.dromara.crehn.domain.bo.ReviewResultBo;
import org.dromara.crehn.domain.bo.ShowcaseOrderBo;
import org.dromara.crehn.domain.vo.ActivityCategoryVo;
import org.dromara.crehn.domain.vo.ActivityVo;
import org.dromara.crehn.domain.vo.ProjectUploadExportVo;
import org.dromara.crehn.domain.vo.ProjectUploadOverviewVo;
import org.dromara.crehn.domain.vo.ProjectQuotaRatioSummaryVo;
import org.dromara.crehn.domain.vo.ProjectUploadSummaryVo;
import org.dromara.crehn.domain.vo.ProjectFileVo;
import org.dromara.crehn.domain.vo.ProjectVo;
import org.dromara.crehn.domain.vo.ReviewAwardRuleVo;
import org.dromara.crehn.domain.vo.ReviewProjectOverviewVo;
import org.dromara.crehn.domain.vo.ReviewResultLogVo;
import org.dromara.crehn.domain.vo.ReviewResultReadinessVo;
import org.dromara.crehn.domain.vo.ReviewResultVo;
import org.dromara.crehn.domain.vo.ReviewScoreSummaryDetailVo;
import org.dromara.crehn.domain.vo.ReviewScoreSummaryFilterOptionsVo;
import org.dromara.crehn.domain.vo.ReviewScoreSummaryQueryVo;
import org.dromara.crehn.domain.vo.ReviewScoreSummaryScoreVo;
import org.dromara.crehn.domain.vo.ReviewScoreSummarySignatureVo;
import org.dromara.crehn.domain.vo.ReviewScoreSummaryVo;
import org.dromara.crehn.domain.vo.ReviewScoreSummaryWarningVo;
import org.dromara.crehn.domain.vo.ShowcaseOrderVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ActivityMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.ReviewAwardRuleMapper;
import org.dromara.crehn.mapper.ReviewAssignmentMapper;
import org.dromara.crehn.mapper.ReviewResultLogMapper;
import org.dromara.crehn.mapper.ReviewResultMapper;
import org.dromara.crehn.mapper.ReviewScoreMapper;
import org.dromara.crehn.mapper.ReviewScoreSheetSignedSheetItemMapper;
import org.dromara.crehn.mapper.ReviewScoreSheetSignedSheetMapper;
import org.dromara.crehn.mapper.SchoolInfoMapper;
import org.dromara.crehn.notice.service.IArtUserMessageService;
import org.dromara.crehn.result.service.IArtResultService;
import org.dromara.crehn.result.service.generate.ReviewResultGenerateService;
import org.dromara.crehn.result.service.publish.ReviewResultPublishService;
import org.dromara.crehn.result.service.warning.ReviewScoreWarningService;
import org.dromara.crehn.project.service.IArtProjectService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.excel.convert.ExcelBigNumberConvert;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysUser;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ArtResultServiceImpl implements IArtResultService {

    private static final String AVERAGE_SORT_ASC = "asc";
    private static final String AVERAGE_SORT_NONE = "none";
    private static final Set<String> SUMMARY_EXPORT_KEYS = Set.of(
        "rowNo", "projectName", "categoryName", "programForm", "groupOrNature", "schoolName",
        "currentAverageScore", "resultAverageScore", "warningText", "resultStatus");

    private static final String UNFILLED_LABEL = "\u672a\u586b\u5199";
    private static final String SIGNED_SHEET_ACTIVE = "active";
    private static final String[] GROUP_NAME_KEYS = {"groupName", "group_name", "group", "groupCode", "group_code"};
    private static final String[] PROJECT_TYPE_KEYS = {"projectType", "project_type", "itemType", "item_type", "workType", "work_type", "programType", "program_type"};
    private static final String[] PROGRAM_FORM_KEYS = {"programForm", "program_form", "workForm", "work_form", "showForm", "show_form", "performanceForm", "performance_form"};
    private static final String[] PERFORMANCE_ORDER_KEYS = {"performanceOrder", "performance_order", "drawNo", "draw_no", "drawNumber", "draw_number", "orderNo", "order_no", "showOrder", "show_order"};
    private static final String PERM_RESULT_SUMMARY = "crehn:result:summary";
    private static final String PERM_AUDIT_LIST = "crehn:audit:list";
    private static final String PERM_AUDIT_SCOPE_ALL = "crehn:auditScope:all";
    private static final String PERM_PROJECT_VIEW_ALL = "crehn:projectView:all";
    private static final String PERM_PROJECT_VIEW_DRAFT = "crehn:projectView:draft";
    private static final String PERM_PROJECT_VIEW_SUBMITTED = "crehn:projectView:submitted";
    private static final String PERM_PROJECT_VIEW_AUDITED = "crehn:projectView:audited";

    private static final Map<String, BigDecimal> DEFAULT_GRADE_POINTS = Map.ofEntries(
        Map.entry("A", BigDecimal.valueOf(100)),
        Map.entry("B", BigDecimal.valueOf(90)),
        Map.entry("C", BigDecimal.valueOf(80)),
        Map.entry("D", BigDecimal.valueOf(70)),
        Map.entry("E", BigDecimal.valueOf(60)),
        Map.entry("F", BigDecimal.valueOf(50)),
        Map.entry("G", BigDecimal.valueOf(40)),
        Map.entry("优秀", BigDecimal.valueOf(100)),
        Map.entry("良好", BigDecimal.valueOf(85)),
        Map.entry("合格", BigDecimal.valueOf(70)),
        Map.entry("不合格", BigDecimal.valueOf(50))
    );

    private final ReviewResultMapper resultMapper;
    private final ReviewAwardRuleMapper awardRuleMapper;
    private final ReviewResultLogMapper resultLogMapper;
    private final ReviewScoreMapper scoreMapper;
    private final ReviewScoreSheetSignedSheetMapper signedSheetMapper;
    private final ReviewScoreSheetSignedSheetItemMapper signedSheetItemMapper;
    private final ReviewAssignmentMapper assignmentMapper;
    private final ProjectMapper projectMapper;
    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final SchoolInfoMapper schoolInfoMapper;
    private final ActivityReportRuleRuntimeService reportRuleRuntimeService;
    private final SysUserMapper sysUserMapper;
    private final ArtReviewSecurity artReviewSecurity;
    private final IArtAuditAssignmentService auditAssignmentService;
    private final IArtUserMessageService userMessageService;
    private final ReviewResultGenerateService reviewResultGenerateService;
    private final ReviewResultPublishService reviewResultPublishService;
    private final IArtProjectService projectService;
    private final ISysOssService ossService;
    private final ReviewScoreWarningService scoreWarningService;

    @Override
    // ART-REF: BE.RESULT.QUERY -> result/service/query/ReviewResultQueryService.java
    public TableDataInfo<ReviewResultVo> queryResultPage(ReviewResultVo query, PageQuery pageQuery) {
        LambdaQueryWrapper<ReviewResult> lqw = baseResultQuery(query)
            .orderByDesc(ReviewResult::getAverageScore)
            .orderByAsc(ReviewResult::getRankNo)
            .orderByDesc(ReviewResult::getCreateTime);
        Page<ReviewResultVo> page = resultMapper.selectVoPage(pageQuery.build(), lqw);
        fillNames(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<ReviewResultVo> queryExportList(ReviewResultVo query) {
        List<ReviewResultVo> rows = resultMapper.selectVoList(baseResultQuery(query)
            .orderByAsc(ReviewResult::getRankNo)
            .orderByDesc(ReviewResult::getAverageScore)
            .orderByDesc(ReviewResult::getCreateTime));
        fillNames(rows);
        return rows;
    }

    @Override
    public TableDataInfo<ReviewResultVo> queryMyPublishedResultPage(ReviewResultVo query, PageQuery pageQuery) {
        Long schoolId = artReviewSecurity.requireEnabledSchoolId();
        query.setSchoolId(schoolId);
        query.setResultStatus(ArtReviewConstants.RESULT_PUBLISHED);
        LambdaQueryWrapper<ReviewResult> lqw = baseResultQuery(query)
            .orderByAsc(ReviewResult::getRankNo)
            .orderByDesc(ReviewResult::getPublishedAt);
        Page<ReviewResultVo> page = resultMapper.selectVoPage(pageQuery.build(), lqw);
        fillNames(page.getRecords());
        maskSchoolResultVisibility(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public ReviewResultReadinessVo checkReadiness(ReviewResultBo bo) {
        return reviewResultGenerateService.checkReadiness(bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // ART-REF: BE.RESULT.GENERATE -> result/service/generate/ReviewResultGenerateService.java
    public void generate(ReviewResultBo bo) {
        reviewResultGenerateService.generate(bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawGenerated(ReviewResultBo bo) {
        lockResultScope(bo);
        if (StringUtils.isBlank(bo.getRemark())) {
            throw new ServiceException("撤回生成结果必须填写原因");
        }
        List<ReviewResult> results = resultMapper.selectList(Wrappers.lambdaQuery(ReviewResult.class)
            .eq(ReviewResult::getActivityId, bo.getActivityId())
            .eq(ReviewResult::getCategoryId, bo.getCategoryId())
            .last("for update"));
        if (results.isEmpty()) {
            throw new ServiceException("当前活动类别没有可撤回的生成结果");
        }
        if (results.stream().anyMatch(result -> ArtReviewConstants.RESULT_PUBLISHED.equals(result.getResultStatus()))) {
            throw new ServiceException("已发布结果不能撤回生成，请使用后续更正版本流程");
        }
        Map<String, Object> beforeSnapshot = new LinkedHashMap<>();
        beforeSnapshot.put("activityId", bo.getActivityId());
        beforeSnapshot.put("categoryId", bo.getCategoryId());
        beforeSnapshot.put("resultCount", results.size());
        beforeSnapshot.put("sampleResultIds", results.stream().map(ReviewResult::getId).limit(50).toList());
        String beforeJson = JsonUtils.toJsonString(beforeSnapshot);
        for (ReviewResult result : results) {
            resultMapper.deleteById(result.getId());
        }
        logAction(bo.getActivityId(), bo.getCategoryId(), null, null, ArtReviewConstants.RESULT_LOG_TARGET_RESULT,
            ArtReviewConstants.RESULT_LOG_ACTION_GENERATE_WITHDRAW, beforeJson, null, bo.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // ART-REF: BE.RESULT.PUBLISH -> result/service/publish/ReviewResultPublishService.java
    public void publish(ReviewResultBo bo) {
        reviewResultPublishService.publish(bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublish(ReviewResultBo bo) {
        reviewResultPublishService.unpublish(bo);
    }

    @Override
    public List<ReviewAwardRuleVo> queryAwardRules(Long activityId, Long categoryId) {
        if (activityId == null || categoryId == null) {
            return List.of();
        }
        return awardRuleMapper.selectVoList(Wrappers.lambdaQuery(ReviewAwardRule.class)
            .eq(ReviewAwardRule::getActivityId, activityId)
            .eq(ReviewAwardRule::getCategoryId, categoryId)
            .orderByAsc(ReviewAwardRule::getSortOrder)
            .orderByAsc(ReviewAwardRule::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // ART-REF: BE.RESULT.AWARD_RULE -> result/service/award/ReviewAwardRuleService.java
    public void saveAwardRules(ReviewAwardRuleBo bo) {
        requireScope(bo);
        categoryMapper.selectOne(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getId, bo.getCategoryId())
            .eq(ActivityCategory::getActivityId, bo.getActivityId())
            .last("for update"));
        long generatedResultCount = resultMapper.selectCount(Wrappers.lambdaQuery(ReviewResult.class)
            .eq(ReviewResult::getActivityId, bo.getActivityId())
            .eq(ReviewResult::getCategoryId, bo.getCategoryId()));
        if (generatedResultCount > 0) {
            throw new ServiceException("当前活动类别已生成结果；如需修改奖项规则，请先撤回未发布结果后重新生成");
        }
        awardRuleMapper.delete(Wrappers.lambdaQuery(ReviewAwardRule.class)
            .eq(ReviewAwardRule::getActivityId, bo.getActivityId())
            .eq(ReviewAwardRule::getCategoryId, bo.getCategoryId()));
        List<ReviewAwardRuleItemBo> rules = bo.getRules() == null ? List.of() : bo.getRules();
        int index = 1;
        for (ReviewAwardRuleItemBo item : rules) {
            if (StringUtils.isBlank(item.getAwardLevel())) {
                continue;
            }
            validateAwardRule(item);
            ReviewAwardRule rule = new ReviewAwardRule();
            rule.setActivityId(bo.getActivityId());
            rule.setCategoryId(bo.getCategoryId());
            rule.setAwardLevel(StringUtils.trim(item.getAwardLevel()));
            rule.setRuleType(StringUtils.blankToDefault(item.getRuleType(), ArtReviewConstants.AWARD_RULE_RANK_RANGE));
            rule.setMinRank(item.getMinRank());
            rule.setMaxRank(item.getMaxRank());
            rule.setMinScore(item.getMinScore());
            rule.setMaxScore(item.getMaxScore());
            rule.setSortOrder(item.getSortOrder() == null ? index * 10 : item.getSortOrder());
            rule.setEnabled(!Boolean.FALSE.equals(item.getEnabled()));
            rule.setRemark(StringUtils.trim(item.getRemark()));
            awardRuleMapper.insert(rule);
            index++;
        }
        logAction(bo.getActivityId(), bo.getCategoryId(), null, null, ArtReviewConstants.RESULT_LOG_TARGET_RESULT,
            ArtReviewConstants.RESULT_LOG_ACTION_AWARD_RULE_SAVE, null, JsonUtils.toJsonString(rules), "保存奖项规则");
        applyAwardRules(bo.getActivityId(), bo.getCategoryId());
    }

    @Override
    public List<ReviewResultLogVo> queryLogs(ReviewResultLogVo query) {
        LambdaQueryWrapper<ReviewResultLog> lqw = Wrappers.lambdaQuery(ReviewResultLog.class)
            .eq(query.getActivityId() != null, ReviewResultLog::getActivityId, query.getActivityId())
            .eq(query.getCategoryId() != null, ReviewResultLog::getCategoryId, query.getCategoryId())
            .eq(query.getProjectId() != null, ReviewResultLog::getProjectId, query.getProjectId())
            .eq(query.getScoreId() != null, ReviewResultLog::getScoreId, query.getScoreId())
            .eq(StringUtils.isNotBlank(query.getTargetType()), ReviewResultLog::getTargetType, query.getTargetType())
            .orderByDesc(ReviewResultLog::getOperatedAt)
            .orderByDesc(ReviewResultLog::getId)
            .last("limit 500");
        List<ReviewResultLogVo> rows = resultLogMapper.selectVoList(lqw);
        fillLogUsers(rows);
        return rows;
    }

    @Override
    // ART-REF: BE.RESULT.UPLOAD_SUMMARY -> result/service/stats/ProjectUploadStatsService.java
    public ProjectUploadSummaryVo queryUploadSummary(ReviewResultVo query) {
        LambdaQueryWrapper<Project> lqw = Wrappers.lambdaQuery(Project.class)
            .eq(query.getActivityId() != null, Project::getActivityId, query.getActivityId())
            .eq(query.getCategoryId() != null, Project::getCategoryId, query.getCategoryId())
            .eq(query.getSchoolId() != null, Project::getSchoolId, query.getSchoolId())
            .orderByDesc(Project::getCreateTime);
        List<Project> projects = projectMapper.selectList(lqw);
        ProjectUploadSummaryVo summary = new ProjectUploadSummaryVo();
        summary.setTotalCount(projects.size());
        Map<Long, ActivityCategory> categoryMap = projects.isEmpty()
            ? Map.of()
            : categoryMapper.selectBatchIds(projects.stream().map(Project::getCategoryId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left));
        Map<Long, SchoolInfo> schoolMap = projectSchoolMap(projects);
        summary.setSchoolCount(projects.stream().map(Project::getSchoolId).filter(Objects::nonNull).distinct().count());
        summary.setStatusItems(buildSummaryItems(projects.stream()
            .collect(Collectors.groupingBy(project -> StringUtils.blankToDefault(project.getStatus(), "-"), LinkedHashMap::new, Collectors.counting())), projects.size()));
        summary.setCategoryItems(buildSummaryItems(projects.stream()
            .collect(Collectors.groupingBy(project -> {
                ActivityCategory category = categoryMap.get(project.getCategoryId());
                return category == null ? String.valueOf(project.getCategoryId()) : category.getCategoryName();
            }, LinkedHashMap::new, Collectors.counting())), projects.size()));
        summary.setSchoolItems(buildSchoolSummaryItems(projects, schoolMap));
        summary.setGroupItems(buildSummaryItems(projects.stream()
            .collect(Collectors.groupingBy(project -> {
                ActivityCategory category = categoryMap.get(project.getCategoryId());
                return groupName(project, category);
            }, LinkedHashMap::new, Collectors.counting())), projects.size()));
        summary.setProjectTypeItems(buildSummaryItems(projects.stream()
            .collect(Collectors.groupingBy(project -> formValue(project, categoryMap.get(project.getCategoryId()), "projectType", PROJECT_TYPE_KEYS), LinkedHashMap::new, Collectors.counting())), projects.size()));
        summary.setProgramFormItems(buildSummaryItems(projects.stream()
            .collect(Collectors.groupingBy(project -> formValue(project, categoryMap.get(project.getCategoryId()), "programForm", PROGRAM_FORM_KEYS), LinkedHashMap::new, Collectors.counting())), projects.size()));
        summary.setPerformanceOrderItems(buildSummaryItems(projects.stream()
            .collect(Collectors.groupingBy(project -> formValue(project, categoryMap.get(project.getCategoryId()), "performanceOrder", PERFORMANCE_ORDER_KEYS), LinkedHashMap::new, Collectors.counting())), projects.size()));
        return summary;
    }

    @Override
    public ProjectUploadOverviewVo queryUploadOverview(ReviewResultVo query) {
        ReviewResultVo safeQuery = query == null ? new ReviewResultVo() : query;
        List<Activity> activities = uploadOverviewVisibleActivities(activityMapper.selectList(Wrappers.lambdaQuery(Activity.class)
            .eq(Activity::getStatus, ArtReviewConstants.ACTIVITY_ENABLED)
            .orderByDesc(Activity::getYear)
            .orderByDesc(Activity::getCreateTime)
            .orderByDesc(Activity::getId)));
        Activity currentActivity = selectUploadOverviewActivity(activities, safeQuery.getActivityId());

        ProjectUploadOverviewVo overview = new ProjectUploadOverviewVo();
        overview.setActivities(activities.stream().map(this::toUploadOverviewActivityItem).toList());
        if (currentActivity == null) {
            return overview;
        }
        overview.setActivityId(currentActivity.getId());
        overview.setActivityName(currentActivity.getActivityName());

        List<ActivityCategory> categories = uploadOverviewVisibleCategories(categoryMapper.selectList(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getActivityId, currentActivity.getId())
            .orderByAsc(ActivityCategory::getSortOrder)
            .orderByAsc(ActivityCategory::getId)), currentActivity.getId());
        Map<Long, ActivityCategory> categoryMap = categories.stream()
            .collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<Long, ProjectUploadOverviewVo.CategoryItem> categoryItems = new LinkedHashMap<>();
        Map<String, ProjectUploadOverviewVo.CategoryItem> groupItems = defaultUploadOverviewGroupItems();
        for (ActivityCategory category : categories) {
            ProjectUploadOverviewVo.CategoryItem item = toUploadOverviewCategoryItem(category);
            categoryItems.put(category.getId(), item);
            ensureUploadOverviewGroupItem(groupItems, item.getGroupKey(), item.getGroupName(), item.getGroupSortOrder());
        }

        List<String> visibleStatuses = uploadOverviewVisibleStatuses();
        if (!visibleStatuses.isEmpty()) {
            List<Project> projects = projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
                .eq(Project::getActivityId, currentActivity.getId())
                .in(Project::getStatus, visibleStatuses)
                .ne(Project::getStatus, ArtReviewConstants.PROJECT_RECYCLED)
                .orderByAsc(Project::getCategoryId)
                .orderByDesc(Project::getCreateTime));
            if (!hasUploadOverviewFullScope()) {
                projects = auditAssignmentService.filterProjectViewVisibleProjects(projects);
            }
            for (Project project : projects) {
                ActivityCategory category = categoryMap.get(project.getCategoryId());
                ProjectUploadOverviewVo.CategoryItem categoryItem = categoryItems.computeIfAbsent(
                    project.getCategoryId() == null ? 0L : project.getCategoryId(),
                    ignored -> toUploadOverviewCategoryItem(project, category));
                ProjectUploadOverviewVo.CategoryItem groupItem = ensureUploadOverviewGroupItem(groupItems,
                    categoryItem.getGroupKey(), categoryItem.getGroupName(), categoryItem.getGroupSortOrder());
                incrementUploadOverviewCount(overview.getSummary(), project.getStatus());
                incrementUploadOverviewCount(categoryItem, project.getStatus());
                incrementUploadOverviewCount(groupItem, project.getStatus());
            }
        }

        overview.setGroupItems(groupItems.values().stream()
            .filter(item -> !"other".equals(item.getGroupKey()) || item.getTotalCount() > 0)
            .sorted(Comparator.comparing(ProjectUploadOverviewVo.CategoryItem::getGroupSortOrder, Comparator.nullsLast(Integer::compareTo)))
            .toList());
        overview.setCategoryItems(categoryItems.values().stream()
            .sorted(Comparator.comparing(ProjectUploadOverviewVo.CategoryItem::getGroupSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ProjectUploadOverviewVo.CategoryItem::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ProjectUploadOverviewVo.CategoryItem::getCategoryName, Comparator.nullsLast(String::compareTo)))
            .toList());
        return overview;
    }

    @Override
    public ProjectQuotaRatioSummaryVo queryQuotaRatioSummary(ReviewResultVo query) {
        ReviewResultVo safeQuery = query == null ? new ReviewResultVo() : query;
        List<Activity> activities = activityMapper.selectList(Wrappers.lambdaQuery(Activity.class)
            .eq(Activity::getStatus, ArtReviewConstants.ACTIVITY_ENABLED)
            .orderByDesc(Activity::getYear)
            .orderByDesc(Activity::getCreateTime)
            .orderByDesc(Activity::getId));
        Activity currentActivity = selectUploadOverviewActivity(activities, safeQuery.getActivityId());

        ProjectQuotaRatioSummaryVo summary = new ProjectQuotaRatioSummaryVo();
        summary.setActivities(activities.stream().map(this::toQuotaRatioActivityItem).toList());
        if (currentActivity == null) {
            return summary;
        }
        summary.setActivityId(currentActivity.getId());
        summary.setActivityName(currentActivity.getActivityName());

        List<Project> projects = projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
            .eq(Project::getActivityId, currentActivity.getId())
            .in(Project::getStatus, ArtReviewConstants.PROJECT_SUBMITTED, ArtReviewConstants.PROJECT_AUDIT_PASSED)
            .orderByAsc(Project::getCategoryId)
            .orderByDesc(Project::getCreateTime));
        Long schoolId = currentSchoolIdIfSchoolUser();
        String schoolType = currentSchoolType(schoolId);
        if (!hasUploadOverviewFullScope()) {
            projects = auditAssignmentService.filterProjectViewVisibleProjects(projects);
        }
        List<Project> ratioProjects = projects;
        summary.setTotalCount(projects.size());

        List<ActivityReportRuleRuntime> rules = loadEffectiveRatioRules(currentActivity.getId(), schoolId, schoolType);
        if (rules.isEmpty()) {
            return summary;
        }

        List<ActivityCategory> categories = categoryMapper.selectList(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getActivityId, currentActivity.getId()));
        Map<Long, ActivityCategory> categoryMap = categories.stream()
            .collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<ProjectQuotaRatioSummaryVo.Item> items = rules.stream()
            .map(rule -> buildQuotaRatioSummaryItem(rule, ratioProjects, categoryMap))
            .toList();
        summary.setItems(items);
        summary.setExceededCount(items.stream().filter(ProjectQuotaRatioSummaryVo.Item::isExceeded).count());
        return summary;
    }

    private Activity selectUploadOverviewActivity(List<Activity> activities, Long activityId) {
        if (activities == null || activities.isEmpty()) {
            return null;
        }
        if (activityId != null) {
            for (Activity activity : activities) {
                if (Objects.equals(activity.getId(), activityId)) {
                    return activity;
                }
            }
        }
        return activities.get(0);
    }

    private List<Activity> uploadOverviewVisibleActivities(List<Activity> activities) {
        if (activities == null || activities.isEmpty() || hasUploadOverviewFullScope()) {
            return activities == null ? List.of() : activities;
        }
        Set<Long> visibleIds = auditAssignmentService.activityOptions().stream()
            .map(ActivityVo::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (visibleIds.isEmpty()) {
            return List.of();
        }
        return activities.stream()
            .filter(activity -> activity.getId() != null && visibleIds.contains(activity.getId()))
            .toList();
    }

    private List<ActivityCategory> uploadOverviewVisibleCategories(List<ActivityCategory> categories, Long activityId) {
        if (categories == null || categories.isEmpty() || hasUploadOverviewFullScope()) {
            return categories == null ? List.of() : categories;
        }
        if (activityId == null) {
            return List.of();
        }
        Set<Long> visibleIds = auditAssignmentService.categoryOptions(activityId).stream()
            .map(ActivityCategoryVo::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (visibleIds.isEmpty()) {
            return List.of();
        }
        return categories.stream()
            .filter(category -> category.getId() != null && visibleIds.contains(category.getId()))
            .toList();
    }

    private ProjectQuotaRatioSummaryVo.ActivityItem toQuotaRatioActivityItem(Activity activity) {
        ProjectQuotaRatioSummaryVo.ActivityItem item = new ProjectQuotaRatioSummaryVo.ActivityItem();
        item.setId(activity.getId());
        item.setActivityName(activity.getActivityName());
        item.setYear(activity.getYear());
        return item;
    }

    private List<ActivityReportRuleRuntime> loadEffectiveRatioRules(Long activityId, Long schoolId, String schoolType) {
        return reportRuleRuntimeService.listRatioRules(activityId, schoolId, schoolType);
    }

    private ProjectUploadOverviewVo.ActivityItem toUploadOverviewActivityItem(Activity activity) {
        ProjectUploadOverviewVo.ActivityItem item = new ProjectUploadOverviewVo.ActivityItem();
        item.setId(activity.getId());
        item.setActivityName(activity.getActivityName());
        item.setYear(activity.getYear());
        return item;
    }

    private ProjectQuotaRatioSummaryVo.Item buildQuotaRatioSummaryItem(ActivityReportRuleRuntime rule, List<Project> projects,
                                                                       Map<Long, ActivityCategory> categoryMap) {
        Map<String, Object> options = parseObject(rule.getRuleJson());
        String operator = StringUtils.blankToDefault(stringOption(options, "ratioOperator"), "max");
        String targetFieldKey = stringOption(options, "targetFieldKey", "groupFieldKey");
        List<Project> scopedProjects = projects.stream()
            .filter(project -> quotaRatioScopeMatches(project, rule, categoryMap, options))
            .toList();
        long denominator = scopedProjects.size();
        long numerator = scopedProjects.stream()
            .filter(project -> quotaRatioTargetMatches(project, rule, categoryMap.get(project.getCategoryId()), targetFieldKey))
            .count();
        BigDecimal ratio = denominator == 0
            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
        boolean exceeded = denominator > 0 && rule.getRatioValue() != null
            && ("min".equals(operator)
            ? ratio.compareTo(rule.getRatioValue()) < 0
            : ratio.compareTo(rule.getRatioValue()) > 0);
        ActivityCategory category = rule.getCategoryId() == null ? null : categoryMap.get(rule.getCategoryId());

        ProjectQuotaRatioSummaryVo.Item item = new ProjectQuotaRatioSummaryVo.Item();
        item.setRuleId(rule.getId());
        item.setCategoryId(rule.getCategoryId());
        item.setCategoryName(category == null ? null : category.getCategoryName());
        item.setGroupCode(rule.getTargetValue());
        item.setGroupName(StringUtils.blankToDefault(rule.getDisplayName(), rule.getTargetValue()));
        item.setTargetFieldKey(StringUtils.blankToDefault(targetFieldKey, "groupCode"));
        item.setScopeCategoryGroup(stringOption(options, "scopeCategoryGroup", "categoryGroupScope"));
        item.setScopeCategoryCodes(stringOption(options, "scopeCategoryCodes", "categoryCodeScope"));
        item.setOperator(operator);
        item.setRatioValue(rule.getRatioValue());
        item.setRatio(ratio);
        item.setNumerator(numerator);
        item.setDenominator(denominator);
        item.setExceeded(exceeded);
        item.setStatusText(exceeded ? ("min".equals(operator) ? "低于预警线" : "超过预警线") : "达标");
        item.setMessage(stringOption(options, "message"));
        item.setRemark(rule.displayMessage());
        return item;
    }

    private Long currentSchoolIdIfSchoolUser() {
        try {
            SysUser user = artReviewSecurity.currentUser();
            return ArtReviewConstants.USER_TYPE_SCHOOL.equals(user.getUserType()) ? user.getSchoolId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String currentSchoolType(Long schoolId) {
        if (schoolId == null) {
            return null;
        }
        SchoolInfo school = schoolInfoMapper.selectById(schoolId);
        return school == null ? null : school.getSchoolType();
    }

    private boolean quotaRatioScopeMatches(Project project, ActivityReportRuleRuntime rule, Map<Long, ActivityCategory> categoryMap,
                                           Map<String, Object> options) {
        if (rule.getCategoryId() != null && !Objects.equals(rule.getCategoryId(), project.getCategoryId())) {
            return false;
        }
        String categoryGroupScope = stringOption(options, "scopeCategoryGroup", "categoryGroupScope");
        String categoryCodeScope = stringOption(options, "scopeCategoryCodes", "categoryCodeScope");
        if (StringUtils.isBlank(categoryGroupScope) && StringUtils.isBlank(categoryCodeScope)) {
            return true;
        }
        ActivityCategory category = categoryMap.get(project.getCategoryId());
        if (category == null) {
            return false;
        }
        if (StringUtils.isNotBlank(categoryGroupScope) && !categoryGroupScope.equals(category.getCategoryGroup())) {
            return false;
        }
        if (StringUtils.isBlank(categoryCodeScope)) {
            return true;
        }
        for (String code : categoryCodeScope.split("[,，\\s]+")) {
            if (StringUtils.isNotBlank(code) && code.equals(category.getCategoryCode())) {
                return true;
            }
        }
        return false;
    }

    private boolean quotaRatioTargetMatches(Project project, ActivityReportRuleRuntime rule, ActivityCategory category,
                                            String targetFieldKey) {
        String expected = blankToNull(rule.getTargetValue());
        String actual = quotaRatioTargetValue(project, category, targetFieldKey);
        if (StringUtils.isBlank(actual)) {
            return false;
        }
        Map<String, Object> options = parseObject(rule.getRuleJson());
        String excludeTargetValue = stringOption(options, "excludeTargetValue", "excludeValue");
        if (StringUtils.isNotBlank(excludeTargetValue)) {
            for (String value : actual.split("[,，、\\s]+")) {
                if (containsToken(excludeTargetValue, value)) {
                    return false;
                }
            }
            if (containsToken(excludeTargetValue, actual)) {
                return false;
            }
        }
        if (expected == null) {
            return true;
        }
        if (expected.equals(actual)) {
            return true;
        }
        for (String value : actual.split("[,，、\\s]+")) {
            if (expected.equals(value)) {
                return true;
            }
        }
        return actual.contains(expected) || expected.contains(actual);
    }

    private boolean containsToken(String tokens, String value) {
        if (StringUtils.isBlank(value)) {
            return false;
        }
        for (String token : tokens.split("[,，、\\s]+")) {
            if (value.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private String quotaRatioTargetValue(Project project, ActivityCategory category, String targetFieldKey) {
        String key = blankToNull(targetFieldKey);
        if (key == null || "groupCode".equals(key) || "__group_code".equals(key)) {
            return StringUtils.blankToDefault(project.getGroupCode(), "");
        }
        if ("groupName".equals(key) || "__group_name".equals(key)) {
            return StringUtils.blankToDefault(project.getGroupName(), "");
        }
        if ("categoryCode".equals(key) || "__category_code".equals(key)) {
            return category == null ? "" : StringUtils.blankToDefault(category.getCategoryCode(), "");
        }
        if ("categoryGroup".equals(key) || "__category_group".equals(key)) {
            return category == null ? "" : StringUtils.blankToDefault(category.getCategoryGroup(), "");
        }
        Object value = parseObject(project.getFormDataJson()).get(key);
        if (value instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(","));
        }
        return value == null ? "" : String.valueOf(value);
    }

    private List<String> uploadOverviewVisibleStatuses() {
        if (hasUploadOverviewFullScope()) {
            return List.of(
                ArtReviewConstants.PROJECT_DRAFT,
                ArtReviewConstants.PROJECT_SUBMITTED,
                ArtReviewConstants.PROJECT_AUDIT_PASSED,
                ArtReviewConstants.PROJECT_RETURNED
            );
        }
        List<String> statuses = new ArrayList<>();
        if (StpUtil.hasPermission(PERM_PROJECT_VIEW_DRAFT)) {
            statuses.add(ArtReviewConstants.PROJECT_DRAFT);
        }
        if (StpUtil.hasPermission(PERM_PROJECT_VIEW_SUBMITTED) || StpUtil.hasPermission(PERM_AUDIT_LIST)) {
            statuses.add(ArtReviewConstants.PROJECT_SUBMITTED);
        }
        if (StpUtil.hasPermission(PERM_PROJECT_VIEW_AUDITED) || StpUtil.hasPermission(PERM_AUDIT_LIST)) {
            statuses.add(ArtReviewConstants.PROJECT_AUDIT_PASSED);
            statuses.add(ArtReviewConstants.PROJECT_RETURNED);
        }
        return statuses.stream().distinct().toList();
    }

    private boolean hasUploadOverviewFullScope() {
        return LoginHelper.isSuperAdmin()
            || StpUtil.hasPermission(PERM_RESULT_SUMMARY)
            || StpUtil.hasPermission(PERM_AUDIT_SCOPE_ALL)
            || StpUtil.hasPermission(PERM_PROJECT_VIEW_ALL);
    }

    private Map<String, ProjectUploadOverviewVo.CategoryItem> defaultUploadOverviewGroupItems() {
        Map<String, ProjectUploadOverviewVo.CategoryItem> items = new LinkedHashMap<>();
        putUploadOverviewGroupItem(items, "performance", "艺术表演类", 10);
        putUploadOverviewGroupItem(items, "artwork", "艺术作品类", 20);
        putUploadOverviewGroupItem(items, "principal", "高校校长书画作品", 30);
        putUploadOverviewGroupItem(items, "workshop", "艺术实践工作坊", 40);
        putUploadOverviewGroupItem(items, "achievement", "高校美育改革创新优秀成果", 50);
        putUploadOverviewGroupItem(items, "other", "其他类别", 90);
        return items;
    }

    private void putUploadOverviewGroupItem(Map<String, ProjectUploadOverviewVo.CategoryItem> items, String key, String name, Integer sortOrder) {
        ProjectUploadOverviewVo.CategoryItem item = new ProjectUploadOverviewVo.CategoryItem();
        item.setId("group-" + key);
        item.setCategoryName(name);
        item.setGroupKey(key);
        item.setGroupName(name);
        item.setGroupSortOrder(sortOrder);
        item.setSortOrder(sortOrder);
        items.put(key, item);
    }

    private ProjectUploadOverviewVo.CategoryItem ensureUploadOverviewGroupItem(Map<String, ProjectUploadOverviewVo.CategoryItem> items,
                                                                               String groupKey, String groupName, Integer sortOrder) {
        String key = StringUtils.blankToDefault(groupKey, "other");
        return items.computeIfAbsent(key, ignored -> {
            ProjectUploadOverviewVo.CategoryItem item = new ProjectUploadOverviewVo.CategoryItem();
            item.setId("group-" + key);
            item.setCategoryName(StringUtils.blankToDefault(groupName, "其他类别"));
            item.setGroupKey(key);
            item.setGroupName(StringUtils.blankToDefault(groupName, "其他类别"));
            item.setGroupSortOrder(sortOrder == null ? uploadOverviewGroupSortOrder(key) : sortOrder);
            item.setSortOrder(item.getGroupSortOrder());
            return item;
        });
    }

    private ProjectUploadOverviewVo.CategoryItem toUploadOverviewCategoryItem(ActivityCategory category) {
        ProjectUploadOverviewVo.CategoryItem item = new ProjectUploadOverviewVo.CategoryItem();
        String groupKey = uploadOverviewGroupKey(category);
        String groupName = uploadOverviewGroupName(groupKey, category);
        item.setId("category-" + category.getId());
        item.setCategoryId(category.getId());
        item.setCategoryCode(category.getCategoryCode());
        item.setCategoryName(category.getCategoryName());
        item.setGroupKey(groupKey);
        item.setGroupName(groupName);
        item.setGroupSortOrder(uploadOverviewGroupSortOrder(groupKey));
        item.setSortOrder(category.getSortOrder());
        return item;
    }

    private ProjectUploadOverviewVo.CategoryItem toUploadOverviewCategoryItem(Project project, ActivityCategory category) {
        if (category != null) {
            return toUploadOverviewCategoryItem(category);
        }
        ProjectUploadOverviewVo.CategoryItem item = new ProjectUploadOverviewVo.CategoryItem();
        item.setId(project.getCategoryId() == null ? "category-unknown" : "category-" + project.getCategoryId());
        item.setCategoryId(project.getCategoryId());
        item.setCategoryName(project.getCategoryId() == null ? "未分类" : "未知类别 " + project.getCategoryId());
        item.setGroupKey("other");
        item.setGroupName("其他类别");
        item.setGroupSortOrder(uploadOverviewGroupSortOrder("other"));
        item.setSortOrder(Integer.MAX_VALUE);
        return item;
    }

    private String uploadOverviewGroupKey(ActivityCategory category) {
        String group = normalizeGroupValue(category == null ? null : category.getCategoryGroup());
        String code = normalizeGroupValue(category == null ? null : category.getCategoryCode());
        if ("performance".equals(group) || code.startsWith("performance_")) {
            return "performance";
        }
        if ("principal".equals(group) || "artwork_principal".equals(code) || "principal".equals(code)) {
            return "principal";
        }
        if ("artwork".equals(group) || code.startsWith("artwork_")) {
            return "artwork";
        }
        if ("workshop".equals(group) || "workshop".equals(code) || code.startsWith("workshop_")) {
            return "workshop";
        }
        if ("achievement".equals(group) || code.startsWith("achievement_")) {
            return "achievement";
        }
        return StringUtils.isBlank(group) ? "other" : group;
    }

    private String uploadOverviewGroupName(String groupKey, ActivityCategory category) {
        return switch (StringUtils.blankToDefault(groupKey, "other")) {
            case "performance" -> "艺术表演类";
            case "artwork" -> "艺术作品类";
            case "principal" -> "高校校长书画作品";
            case "workshop" -> "艺术实践工作坊";
            case "achievement" -> "高校美育改革创新优秀成果";
            case "other" -> "其他类别";
            default -> StringUtils.blankToDefault(category == null ? null : category.getCategoryGroup(), groupKey);
        };
    }

    private int uploadOverviewGroupSortOrder(String groupKey) {
        return switch (StringUtils.blankToDefault(groupKey, "other")) {
            case "performance" -> 10;
            case "artwork" -> 20;
            case "principal" -> 30;
            case "workshop" -> 40;
            case "achievement" -> 50;
            default -> 90;
        };
    }

    private String normalizeGroupValue(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void incrementUploadOverviewCount(ProjectUploadOverviewVo.CountItem item, String status) {
        item.setTotalCount(item.getTotalCount() + 1);
        switch (StringUtils.blankToDefault(status, "")) {
            case ArtReviewConstants.PROJECT_DRAFT -> item.setDraftCount(item.getDraftCount() + 1);
            case ArtReviewConstants.PROJECT_SUBMITTED -> item.setSubmittedCount(item.getSubmittedCount() + 1);
            case ArtReviewConstants.PROJECT_AUDIT_PASSED -> item.setAuditedCount(item.getAuditedCount() + 1);
            case ArtReviewConstants.PROJECT_RETURNED -> item.setReturnedCount(item.getReturnedCount() + 1);
            default -> {
            }
        }
    }

    private List<ProjectUploadSummaryVo.SchoolItem> buildSchoolSummaryItems(List<Project> projects, Map<Long, SchoolInfo> schoolMap) {
        if (projects == null || projects.isEmpty()) {
            return List.of();
        }
        Map<Long, List<Project>> grouped = projects.stream()
            .collect(Collectors.groupingBy(project -> project.getSchoolId() == null ? 0L : project.getSchoolId(), LinkedHashMap::new, Collectors.toList()));
        long total = projects.size();
        return grouped.entrySet().stream()
            .map(entry -> {
                Long schoolId = entry.getKey();
                List<Project> rows = entry.getValue();
                SchoolInfo school = schoolMap.get(schoolId);
                ProjectUploadSummaryVo.SchoolItem item = new ProjectUploadSummaryVo.SchoolItem();
                item.setSchoolId(schoolId == 0L ? null : schoolId);
                item.setKey(schoolId == 0L ? "-" : String.valueOf(schoolId));
                item.setLabel(school == null ? "\u672a\u77e5\u5b66\u6821" : school.getSchoolName());
                item.setCount(rows.size());
                item.setPercent(total == 0 ? 0D : BigDecimal.valueOf(rows.size() * 100D / total).setScale(2, RoundingMode.HALF_UP).doubleValue());
                item.setDraftCount(countStatus(rows, ArtReviewConstants.PROJECT_DRAFT));
                item.setSubmittedCount(countStatus(rows, ArtReviewConstants.PROJECT_SUBMITTED));
                item.setReturnedCount(countStatus(rows, ArtReviewConstants.PROJECT_RETURNED));
                item.setPassedCount(countStatus(rows, ArtReviewConstants.PROJECT_AUDIT_PASSED));
                return item;
            })
            .sorted(Comparator.comparing(ProjectUploadSummaryVo.SchoolItem::getCount).reversed()
                .thenComparing(ProjectUploadSummaryVo.SchoolItem::getLabel, Comparator.nullsLast(String::compareTo)))
            .toList();
    }

    private long countStatus(List<Project> projects, String status) {
        return projects.stream().filter(project -> Objects.equals(project.getStatus(), status)).count();
    }

    @Override
    public List<ProjectUploadExportVo> queryUploadExportList(ReviewResultVo query) {
        List<Project> projects = projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
            .eq(query.getActivityId() != null, Project::getActivityId, query.getActivityId())
            .eq(query.getCategoryId() != null, Project::getCategoryId, query.getCategoryId())
            .eq(query.getSchoolId() != null, Project::getSchoolId, query.getSchoolId())
            .orderByAsc(Project::getActivityId)
            .orderByAsc(Project::getCategoryId)
            .orderByDesc(Project::getCreateTime));
        if (projects.isEmpty()) {
            return List.of();
        }
        Map<Long, Activity> activityMap = activityMapper.selectBatchIds(projects.stream().map(Project::getActivityId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(Activity::getId, item -> item, (left, right) -> left));
        Map<Long, ActivityCategory> categoryMap = categoryMapper.selectBatchIds(projects.stream().map(Project::getCategoryId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left));
        Map<Long, SchoolInfo> schoolMap = schoolInfoMapper.selectBatchIds(projects.stream().map(Project::getSchoolId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(SchoolInfo::getId, item -> item, (left, right) -> left));
        return projects.stream().map(project -> {
            ProjectUploadExportVo row = new ProjectUploadExportVo();
            Activity activity = activityMap.get(project.getActivityId());
            ActivityCategory category = categoryMap.get(project.getCategoryId());
            SchoolInfo school = schoolMap.get(project.getSchoolId());
            row.setActivityName(activity == null ? null : activity.getActivityName());
            row.setCategoryName(category == null ? null : category.getCategoryName());
            row.setProjectNo(project.getProjectNo());
            row.setProjectName(project.getProjectName());
            row.setSchoolName(school == null ? null : school.getSchoolName());
            row.setGroupName(groupName(project, category));
            row.setProjectType(blankToNull(formValue(project, category, "projectType", PROJECT_TYPE_KEYS)));
            row.setProgramForm(blankToNull(formValue(project, category, "programForm", PROGRAM_FORM_KEYS)));
            row.setPerformanceOrder(blankToNull(formValue(project, category, "performanceOrder", PERFORMANCE_ORDER_KEYS)));
            row.setStatus(summaryLabel(project.getStatus()));
            row.setSubmittedAt(project.getSubmittedAt());
            row.setCreateTime(project.getCreateTime());
            return row;
        }).toList();
    }

    @Override
    // ART-REF: BE.RESULT.PROJECT_OVERVIEW -> result/service/overview/ReviewProjectOverviewService.java
    public TableDataInfo<ReviewProjectOverviewVo> queryProjectOverviewPage(ReviewProjectOverviewVo query, PageQuery pageQuery) {
        LambdaQueryWrapper<Project> lqw = Wrappers.lambdaQuery(Project.class)
            .eq(query.getActivityId() != null, Project::getActivityId, query.getActivityId())
            .eq(query.getCategoryId() != null, Project::getCategoryId, query.getCategoryId())
            .eq(query.getSchoolId() != null, Project::getSchoolId, query.getSchoolId())
            .eq(StringUtils.isNotBlank(query.getStatus()), Project::getStatus, query.getStatus())
            .like(StringUtils.isNotBlank(query.getProjectName()), Project::getProjectName, query.getProjectName())
            .orderByDesc(Project::getSubmittedAt)
            .orderByDesc(Project::getCreateTime)
            .orderByDesc(Project::getId);
        Page<Project> page = projectMapper.selectPage(pageQuery.build(), lqw);
        List<Project> projects = page.getRecords();
        if (projects.isEmpty()) {
            return new TableDataInfo<>(List.of(), page.getTotal());
        }
        Map<Long, Activity> activityMap = activityMapper.selectBatchIds(projects.stream().map(Project::getActivityId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(Activity::getId, item -> item, (left, right) -> left));
        Map<Long, ActivityCategory> categoryMap = categoryMapper.selectBatchIds(projects.stream().map(Project::getCategoryId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left));
        Map<Long, SchoolInfo> schoolMap = projectSchoolMap(projects);
        Map<Long, ReviewResult> resultMap = resultByProjectMap(projects);
        List<Long> categoryIds = projects.stream().map(Project::getCategoryId).filter(Objects::nonNull).distinct().toList();
        List<ReviewAssignment> allAssignments = categoryIds.isEmpty()
            ? List.of()
            : assignmentMapper.selectList(Wrappers.lambdaQuery(ReviewAssignment.class)
                .in(ReviewAssignment::getCategoryId, categoryIds));
        List<ReviewAssignment> assignments = allAssignments.stream()
            .filter(assignment -> ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE.equals(assignment.getStatus()))
            .toList();
        Map<Long, Long> assignmentCountByCategory = assignments.stream()
            .collect(Collectors.groupingBy(ReviewAssignment::getCategoryId, Collectors.counting()));
        Map<Long, Long> submittedScoreCount = scoreCountByProject(projects, allAssignments, ArtReviewConstants.REVIEW_SCORE_SUBMITTED);
        Map<Long, Long> draftScoreCount = scoreCountByProject(projects, assignments, ArtReviewConstants.REVIEW_SCORE_DRAFT);
        List<ReviewProjectOverviewVo> rows = projects.stream()
            .map(project -> toProjectOverviewVo(project, activityMap, categoryMap, schoolMap, resultMap,
                assignmentCountByCategory, submittedScoreCount, draftScoreCount))
            .toList();
        return new TableDataInfo<>(rows, page.getTotal());
    }

    /**
     * Project-level score summary for the review-management view. This is a
     * read-only projection: score rows are read from the current submitted
     * records, while resultAverageScore/resultGeneratedAt come from the last
     * generated result snapshot.
     */
    @Override
    public TableDataInfo<ReviewScoreSummaryVo> queryScoreSummaryPage(ReviewScoreSummaryQueryVo query, PageQuery pageQuery) {
        List<ReviewScoreSummaryVo> allRows = queryScoreSummaryRows(query, false);
        List<ReviewScoreSummaryVo> pageRows = pageRows(allRows, pageQuery);
        return new TableDataInfo<>(pageRows, (long) allRows.size());
    }

    @Override
    public ReviewScoreSummaryFilterOptionsVo queryScoreSummaryFilterOptions(Long activityId, Long categoryId) {
        LambdaQueryWrapper<Project> wrapper = Wrappers.lambdaQuery(Project.class)
            .select(Project::getCategoryId, Project::getFormDataJson, Project::getGroupName, Project::getGroupCode)
            .eq(activityId != null, Project::getActivityId, activityId)
            .eq(categoryId != null, Project::getCategoryId, categoryId)
            .eq(Project::getStatus, ArtReviewConstants.PROJECT_AUDIT_PASSED)
            .orderByAsc(Project::getId);
        List<Project> projects = projectMapper.selectList(wrapper);
        Map<Long, ActivityCategory> categories = projects.isEmpty() ? Map.of() : categoryMapper.selectBatchIds(
                projects.stream().map(Project::getCategoryId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left));
        Set<String> forms = new LinkedHashSet<>();
        Set<String> groups = new LinkedHashSet<>();
        for (Project project : projects) {
            ActivityCategory category = categories.get(project.getCategoryId());
            forms.add(StringUtils.blankToDefault(formValue(project, category, "programForm", PROGRAM_FORM_KEYS), UNFILLED_LABEL));
            groups.add(StringUtils.blankToDefault(groupName(project, category), UNFILLED_LABEL));
        }
        ReviewScoreSummaryFilterOptionsVo options = new ReviewScoreSummaryFilterOptionsVo();
        options.setProgramForms(forms.stream().sorted().toList());
        options.setGroupOrNatures(groups.stream().sorted().toList());
        return options;
    }

    @Override
    public void exportScoreSummary(ReviewScoreSummaryQueryVo query, PageQuery pageQuery, String scope,
                                   String columnsJson, HttpServletResponse response) {
        List<ReviewScoreSummaryVo> allRows = queryScoreSummaryRows(query, false);
        List<ReviewScoreSummaryVo> exportRows = "all".equalsIgnoreCase(StringUtils.trim(scope))
            ? allRows : pageRows(allRows, pageQuery);
        List<SummaryExportColumn> columns = exportColumns(columnsJson, maxScoreSlot(allRows));
        writeScoreSummaryExcel(exportRows, columns, response);
    }

    private List<ReviewScoreSummaryVo> queryScoreSummaryRows(ReviewScoreSummaryQueryVo query, boolean includeSignatures) {
        ReviewScoreSummaryQueryVo safeQuery = query == null ? new ReviewScoreSummaryQueryVo() : query;
        LambdaQueryWrapper<Project> wrapper = buildScoreSummaryProjectQuery(safeQuery);
        List<Project> projects = projectMapper.selectList(wrapper);
        if (projects.isEmpty()) {
            return List.of();
        }
        ScoreSummaryContext context = loadScoreSummaryContext(projects, includeSignatures);
        List<ReviewScoreSummaryVo> rows = projects.stream()
            .map(project -> toScoreSummaryRow(project, context, 0))
            .filter(row -> matchesScoreSummaryFilter(row, safeQuery))
            .collect(Collectors.toCollection(ArrayList::new));
        sortScoreSummaryRows(rows, safeQuery.getAverageSort());
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setRowNo(i + 1);
        }
        return rows;
    }

    private LambdaQueryWrapper<Project> buildScoreSummaryProjectQuery(ReviewScoreSummaryQueryVo query) {
        LambdaQueryWrapper<Project> wrapper = Wrappers.lambdaQuery(Project.class)
            .eq(query.getActivityId() != null, Project::getActivityId, query.getActivityId())
            .eq(query.getCategoryId() != null, Project::getCategoryId, query.getCategoryId())
            .eq(query.getSchoolId() != null, Project::getSchoolId, query.getSchoolId())
            .eq(Project::getStatus, ArtReviewConstants.PROJECT_AUDIT_PASSED)
            .like(StringUtils.isNotBlank(query.getProjectName()), Project::getProjectName, query.getProjectName())
            .orderByAsc(Project::getActivityId)
            .orderByAsc(Project::getCategoryId)
            .orderByAsc(Project::getProjectNo)
            .orderByAsc(Project::getId);
        if (query.getReviewerUserId() != null) {
            List<Long> projectIds = scoreMapper.selectList(Wrappers.lambdaQuery(ReviewScore.class)
                    .select(ReviewScore::getProjectId)
                    .eq(ReviewScore::getReviewerUserId, query.getReviewerUserId())
                    .eq(ReviewScore::getStatus, ArtReviewConstants.REVIEW_SCORE_SUBMITTED))
                .stream().map(ReviewScore::getProjectId).filter(Objects::nonNull).distinct().toList();
            if (projectIds.isEmpty()) {
                wrapper.in(Project::getId, List.of(-1L));
            } else {
                wrapper.in(Project::getId, projectIds);
            }
        }
        if (StringUtils.isNotBlank(query.getResultStatus())) {
            List<Long> resultProjectIds = resultMapper.selectList(Wrappers.lambdaQuery(ReviewResult.class)
                    .select(ReviewResult::getProjectId)
                    .eq(ReviewResult::getResultStatus, query.getResultStatus())
                    .eq(query.getActivityId() != null, ReviewResult::getActivityId, query.getActivityId())
                    .eq(query.getCategoryId() != null, ReviewResult::getCategoryId, query.getCategoryId()))
                .stream().map(ReviewResult::getProjectId).filter(Objects::nonNull).distinct().toList();
            if (resultProjectIds.isEmpty()) {
                wrapper.in(Project::getId, List.of(-1L));
            } else {
                wrapper.in(Project::getId, resultProjectIds);
            }
        }
        return wrapper;
    }

    private boolean matchesScoreSummaryFilter(ReviewScoreSummaryVo row, ReviewScoreSummaryQueryVo query) {
        if (StringUtils.isNotBlank(query.getProgramForm()) && !matchesFilterValue(row.getProgramForm(), query.getProgramForm())) {
            return false;
        }
        return StringUtils.isBlank(query.getGroupOrNature()) || matchesFilterValue(row.getGroupOrNature(), query.getGroupOrNature());
    }

    private boolean matchesFilterValue(String actual, String expected) {
        String expectedValue = StringUtils.trim(expected);
        String actualValue = StringUtils.isBlank(actual) ? UNFILLED_LABEL : actual;
        return Objects.equals(actualValue, expectedValue);
    }

    private void sortScoreSummaryRows(List<ReviewScoreSummaryVo> rows, String averageSort) {
        String sort = StringUtils.blankToDefault(StringUtils.trim(averageSort), "desc").toLowerCase(Locale.ROOT);
        if (AVERAGE_SORT_NONE.equals(sort)) {
            return;
        }
        boolean descending = !AVERAGE_SORT_ASC.equals(sort);
        rows.sort((left, right) -> {
            int averageCompare = compareNullable(left.getCurrentAverageScore(), right.getCurrentAverageScore());
            if (averageCompare != 0) {
                if (left.getCurrentAverageScore() != null && right.getCurrentAverageScore() != null && descending) {
                    return -averageCompare;
                }
                return averageCompare;
            }
            int projectNoCompare = compareNullable(left.getProjectNo(), right.getProjectNo());
            return projectNoCompare != 0 ? projectNoCompare : compareNullable(left.getProjectId(), right.getProjectId());
        });
    }

    private <T extends Comparable<? super T>> int compareNullable(T left, T right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private List<ReviewScoreSummaryVo> pageRows(List<ReviewScoreSummaryVo> rows, PageQuery pageQuery) {
        int pageNum = pageQuery == null || pageQuery.getPageNum() == null || pageQuery.getPageNum() < 1
            ? PageQuery.DEFAULT_PAGE_NUM : pageQuery.getPageNum();
        int pageSize = pageQuery == null || pageQuery.getPageSize() == null || pageQuery.getPageSize() < 1
            ? 10 : pageQuery.getPageSize();
        long fromLong = (long) (pageNum - 1) * pageSize;
        if (fromLong >= rows.size()) {
            return List.of();
        }
        int from = (int) fromLong;
        int to = Math.min(rows.size(), from + pageSize);
        return new ArrayList<>(rows.subList(from, to));
    }

    private int maxScoreSlot(List<ReviewScoreSummaryVo> rows) {
        return Math.max(3, rows.stream().filter(Objects::nonNull)
            .flatMap(row -> row.getScoreItems() == null ? java.util.stream.Stream.empty() : row.getScoreItems().stream())
            .map(ReviewScoreSummaryScoreVo::getSlot).filter(Objects::nonNull).max(Integer::compareTo).orElse(0));
    }

    private List<SummaryExportColumn> exportColumns(String columnsJson, int maxScoreSlot) {
        Map<String, SummaryExportColumn> configured = new LinkedHashMap<>();
        Set<String> configuredFieldKeys = new LinkedHashSet<>();
        boolean hasConfig = false;
        if (StringUtils.isNotBlank(columnsJson) && columnsJson.length() <= 20000) {
            try {
                List<Map<String, Object>> values = JsonUtils.parseObject(columnsJson,
                    new TypeReference<List<Map<String, Object>>>() { });
                if (values != null) {
                    for (Map<String, Object> value : values) {
                        String fieldKey = StringUtils.trim(String.valueOf(value.get("fieldKey")));
                        if ("actions".equals(fieldKey) || (!SUMMARY_EXPORT_KEYS.contains(fieldKey)
                            && !fieldKey.matches("score(?:[1-9]|1[0-9]|20)"))) {
                            continue;
                        }
                        hasConfig = true;
                        configuredFieldKeys.add(fieldKey);
                        if (Boolean.FALSE.equals(value.get("visible"))) {
                            continue;
                        }
                        String label = StringUtils.trim(String.valueOf(value.get("label")));
                        if (StringUtils.isBlank(label)) {
                            label = fieldKey;
                        }
                        if (label.length() > 30) {
                            label = label.substring(0, 30);
                        }
                        int sortOrder = toExportInt(value.get("sortOrder"), configured.size() + 1);
                        configured.putIfAbsent(fieldKey, new SummaryExportColumn(fieldKey, label, sortOrder));
                    }
                }
            } catch (RuntimeException ignored) {
                configured.clear();
                configuredFieldKeys.clear();
                hasConfig = false;
            }
        }
        if (configured.isEmpty()) {
            configured.put("rowNo", new SummaryExportColumn("rowNo", "序号", 1));
            configured.put("projectName", new SummaryExportColumn("projectName", "节目名称", 2));
            configured.put("categoryName", new SummaryExportColumn("categoryName", "类别", 3));
            configured.put("programForm", new SummaryExportColumn("programForm", "形式", 4));
            configured.put("groupOrNature", new SummaryExportColumn("groupOrNature", "甲乙组/个人", 5));
            configured.put("schoolName", new SummaryExportColumn("schoolName", "学校", 6));
            configured.put("currentAverageScore", new SummaryExportColumn("currentAverageScore", "平均分", 100));
            configured.put("warningText", new SummaryExportColumn("warningText", "评分提示", 101));
        }
        for (int slot = 1; slot <= maxScoreSlot; slot++) {
            String fieldKey = "score" + slot;
            if (configuredFieldKeys.contains(fieldKey)) {
                continue;
            }
            if (hasConfig && slot <= 3) {
                continue;
            }
            final int scoreSlot = slot;
            configured.computeIfAbsent(fieldKey,
                key -> new SummaryExportColumn(key, "评分" + scoreSlot, 6 + scoreSlot));
        }
        return configured.values().stream()
            .sorted(Comparator.comparingInt(SummaryExportColumn::sortOrder))
            .toList();
    }

    private int toExportInt(Object value, int fallback) {
        try {
            return value == null ? fallback : new BigDecimal(String.valueOf(value)).intValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            return fallback;
        }
    }

    private void writeScoreSummaryExcel(List<ReviewScoreSummaryVo> rows, List<SummaryExportColumn> columns,
                                        HttpServletResponse response) {
        try {
            String filename = URLEncoder.encode("分数汇总.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + filename);
            List<List<String>> head = columns.stream().map(column -> List.of(column.label())).toList();
            List<List<Object>> data = rows.stream().map(row -> columns.stream()
                .map(column -> exportCellValue(row, column.fieldKey())).toList()).toList();
            try (ExcelWriter writer = FastExcel.write(response.getOutputStream())
                .head(head)
                .autoCloseStream(false)
                .registerConverter(new ExcelBigNumberConvert())
                .build()) {
                writer.write(data, FastExcel.writerSheet("分数汇总").build());
            }
        } catch (IOException ex) {
            throw new RuntimeException("分数汇总导出失败", ex);
        }
    }

    private Object exportCellValue(ReviewScoreSummaryVo row, String fieldKey) {
        if (fieldKey.matches("score(?:[1-9]|1[0-9]|20)")) {
            int slot = Integer.parseInt(fieldKey.substring("score".length()));
            return row.getScoreItems() == null ? null : row.getScoreItems().stream()
                .filter(item -> Objects.equals(item.getSlot(), slot)).map(item -> item.getScoreValue() == null
                    ? item.getGradeValue() : item.getScoreValue()).findFirst().orElse(null);
        }
        return switch (fieldKey) {
            case "rowNo" -> row.getRowNo();
            case "projectName" -> row.getProjectName();
            case "categoryName" -> row.getCategoryName();
            case "programForm" -> row.getProgramForm();
            case "groupOrNature" -> row.getGroupOrNature();
            case "schoolName" -> row.getSchoolName();
            case "currentAverageScore" -> row.getCurrentAverageScore();
            case "resultAverageScore" -> row.getResultAverageScore();
            case "warningText" -> row.getWarning() == null ? null : row.getWarning().getText();
            case "resultStatus" -> row.getResultStatus();
            default -> null;
        };
    }

    private record SummaryExportColumn(String fieldKey, String label, int sortOrder) {
    }

    /**
     * Returns project data, attachments, current scores and historical
     * signature status for one project. No mutation or score re-calculation is
     * persisted by this endpoint.
     */
    @Override
    public ReviewScoreSummaryDetailVo getScoreSummaryDetail(Long projectId) {
        if (projectId == null) {
            throw new ServiceException("项目ID不能为空");
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null || !ArtReviewConstants.PROJECT_AUDIT_PASSED.equals(project.getStatus())) {
            throw new ServiceException("项目不存在或当前不可查看评分汇总");
        }
        ScoreSummaryContext context = loadScoreSummaryContext(List.of(project));
        ReviewScoreSummaryVo row = toScoreSummaryRow(project, context, 1);
        ReviewScoreSummaryDetailVo detail = copyScoreSummaryDetail(row);
        ProjectVo projectDetail = projectService.getProjectDetail(projectId);
        detail.setProject(projectDetail);
        detail.setAttachments(projectDetail == null || projectDetail.getFiles() == null
            ? List.of() : projectDetail.getFiles());
        return detail;
    }

    private ScoreSummaryContext loadScoreSummaryContext(List<Project> projects) {
        return loadScoreSummaryContext(projects, true);
    }

    private ScoreSummaryContext loadScoreSummaryContext(List<Project> projects, boolean includeSignatures) {
        List<Long> projectIds = projects.stream().map(Project::getId).filter(Objects::nonNull).distinct().toList();
        if (projectIds.isEmpty()) {
            return new ScoreSummaryContext(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }
        Map<Long, Activity> activityMap = activityMapper.selectBatchIds(projects.stream()
                .map(Project::getActivityId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(Activity::getId, item -> item, (left, right) -> left));
        Map<Long, ActivityCategory> categoryMap = categoryMapper.selectBatchIds(projects.stream()
                .map(Project::getCategoryId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left));
        Map<Long, SchoolInfo> schoolMap = projectSchoolMap(projects);
        Map<Long, ReviewResult> resultMap = resultMapper.selectList(Wrappers.lambdaQuery(ReviewResult.class)
                .in(ReviewResult::getProjectId, projectIds))
            .stream().collect(Collectors.toMap(ReviewResult::getProjectId, item -> item, (left, right) ->
                latestResult(left, right)));
        List<ReviewScore> scores = scoreMapper.selectList(Wrappers.lambdaQuery(ReviewScore.class)
            .in(ReviewScore::getProjectId, projectIds)
            .eq(ReviewScore::getStatus, ArtReviewConstants.REVIEW_SCORE_SUBMITTED)
            .orderByAsc(ReviewScore::getSubmittedAt)
            .orderByAsc(ReviewScore::getCreateTime)
            .orderByAsc(ReviewScore::getId));
        Map<Long, List<ReviewScore>> scoresByProject = scores.stream()
            .collect(Collectors.groupingBy(ReviewScore::getProjectId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, SysUser> userMap = scores.stream().map(ReviewScore::getReviewerUserId).filter(Objects::nonNull).distinct().toList()
            .isEmpty() ? Map.of() : sysUserMapper.selectBatchIds(scores.stream().map(ReviewScore::getReviewerUserId)
                .filter(Objects::nonNull).distinct().toList()).stream()
            .collect(Collectors.toMap(SysUser::getUserId, item -> item, (left, right) -> left));

        if (!includeSignatures) {
            return new ScoreSummaryContext(activityMap, categoryMap, schoolMap, resultMap, scoresByProject, userMap,
                Map.of(), Map.of(), Map.of());
        }

        List<ReviewScoreSheetSignedSheetItem> items = signedSheetItemMapper.selectList(Wrappers.lambdaQuery(ReviewScoreSheetSignedSheetItem.class)
            .in(ReviewScoreSheetSignedSheetItem::getProjectId, projectIds));
        Set<Long> signedSheetIds = items.stream().map(ReviewScoreSheetSignedSheetItem::getSignedSheetId)
            .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ReviewScoreSheetSignedSheet> sheetMap = signedSheetIds.isEmpty() ? Map.of() : signedSheetMapper.selectBatchIds(signedSheetIds)
            .stream().collect(Collectors.toMap(ReviewScoreSheetSignedSheet::getId, item -> item, (left, right) -> latestSignedSheet(left, right)));
        Map<Long, List<ReviewScoreSheetSignedSheet>> sheetsByScore = new LinkedHashMap<>();
        Map<Long, String> scoreSnapshots = new LinkedHashMap<>();
        for (ReviewScoreSheetSignedSheetItem item : items) {
            if (item.getReviewScoreId() != null && StringUtils.isNotBlank(item.getScoreSnapshotJson())) {
                scoreSnapshots.putIfAbsent(item.getReviewScoreId(), item.getScoreSnapshotJson());
            }
            ReviewScoreSheetSignedSheet sheet = sheetMap.get(item.getSignedSheetId());
            if (sheet != null && item.getReviewScoreId() != null) {
                sheetsByScore.computeIfAbsent(item.getReviewScoreId(), ignored -> new ArrayList<>()).add(sheet);
            }
        }
        Set<Long> signatureOssIds = sheetMap.values().stream().map(ReviewScoreSheetSignedSheet::getSignatureOssId)
            .filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> signatureUrls = signatureOssIds.isEmpty() ? Map.of() : ossService.listByIds(signatureOssIds).stream()
            .filter(item -> item.getOssId() != null && StringUtils.isNotBlank(item.getUrl()))
            .collect(Collectors.toMap(SysOssVo::getOssId, SysOssVo::getUrl, (left, right) -> left));
        return new ScoreSummaryContext(activityMap, categoryMap, schoolMap, resultMap, scoresByProject, userMap,
            sheetsByScore, signatureUrls, scoreSnapshots);
    }

    private ReviewResult latestResult(ReviewResult left, ReviewResult right) {
        Date leftTime = left.getUpdateTime() == null ? left.getCreateTime() : left.getUpdateTime();
        Date rightTime = right.getUpdateTime() == null ? right.getCreateTime() : right.getUpdateTime();
        return rightTime != null && (leftTime == null || rightTime.after(leftTime)) ? right : left;
    }

    private ReviewScoreSheetSignedSheet latestSignedSheet(ReviewScoreSheetSignedSheet left, ReviewScoreSheetSignedSheet right) {
        Date leftTime = left.getSignedAt();
        Date rightTime = right.getSignedAt();
        return rightTime != null && (leftTime == null || rightTime.after(leftTime)) ? right : left;
    }

    private ReviewScoreSummaryVo toScoreSummaryRow(Project project, ScoreSummaryContext context, int rowNo) {
        Activity activity = context.activityMap().get(project.getActivityId());
        ActivityCategory category = context.categoryMap().get(project.getCategoryId());
        SchoolInfo school = context.schoolMap().get(project.getSchoolId());
        ReviewResult result = context.resultMap().get(project.getId());
        ReviewScoreSummaryVo row = new ReviewScoreSummaryVo();
        row.setRowNo(rowNo);
        row.setActivityId(project.getActivityId());
        row.setActivityName(activity == null ? null : activity.getActivityName());
        row.setCategoryId(project.getCategoryId());
        row.setCategoryName(category == null ? null : category.getCategoryName());
        row.setProjectId(project.getId());
        row.setProjectNo(project.getProjectNo());
        row.setProjectName(project.getProjectName());
        row.setSchoolId(project.getSchoolId());
        row.setSchoolName(school == null ? null : school.getSchoolName());
        row.setGroupOrNature(groupName(project, category));
        row.setProgramForm(blankToNull(formValue(project, category, "programForm", PROGRAM_FORM_KEYS)));
        List<ReviewScore> scores = new ArrayList<>(context.scoresByProject().getOrDefault(project.getId(), List.of()));
        scores.sort(Comparator.comparing(ReviewScore::getSubmittedAt, Comparator.nullsLast(Date::compareTo))
            .thenComparing(ReviewScore::getCreateTime, Comparator.nullsLast(Date::compareTo))
            .thenComparing(ReviewScore::getId, Comparator.nullsLast(Long::compareTo)));
        List<ReviewScoreSummaryScoreVo> scoreItems = new ArrayList<>();
        List<BigDecimal> numericValues = new ArrayList<>();
        List<BigDecimal> warningValues = new ArrayList<>();
        int slot = 1;
        for (ReviewScore score : scores) {
            ReviewScoreSummaryScoreVo item = new ReviewScoreSummaryScoreVo();
            item.setSlot(slot++);
            item.setScoreId(score.getId());
            item.setAssignmentId(score.getAssignmentId());
            item.setReviewerUserId(score.getReviewerUserId());
            SysUser reviewer = context.userMap().get(score.getReviewerUserId());
            item.setReviewerName(reviewer == null ? String.valueOf(score.getReviewerUserId())
                : StringUtils.blankToDefault(reviewer.getNickName(), reviewer.getUserName()));
            item.setScoreValue(score.getScoreValue());
            item.setGradeValue(score.getGradeValue());
            item.setCommentText(score.getCommentText());
            item.setStatus(score.getStatus());
            item.setSubmittedAt(score.getSubmittedAt());
            BigDecimal numeric = score.getScoreValue() == null ? gradePoint(score.getGradeValue()) : score.getScoreValue();
            if (numeric != null) {
                numericValues.add(numeric);
                warningValues.add(numeric);
            }
            List<ReviewScoreSheetSignedSheet> sheets = context.sheetsByScore().get(score.getId());
            ReviewScoreSheetSignedSheet sheet = selectPreferredSignedSheet(sheets);
            if (sheet != null) {
                ReviewScoreSummarySignatureVo signature = toSignatureVo(sheet, context.signatureUrls());
                BigDecimal signedScore = snapshotScore(context.scoreSnapshots().get(score.getId()));
                boolean mismatch = numeric != null && signedScore != null && numeric.compareTo(signedScore) != 0;
                signature.setScoreMismatch(mismatch);
                signature.setSignedScoreValue(signedScore);
                if (mismatch && SIGNED_SHEET_ACTIVE.equalsIgnoreCase(signature.getSignatureStatus())) {
                    signature.setSignatureStatus("discrepancy");
                }
                item.setSignature(signature);
                item.setSignatureUrl(signature.getSignatureUrl());
                item.setSignatureName(signature.getSignatureName());
                item.setSignatureSignedAt(signature.getSignatureSignedAt());
                item.setSignatureStatus(signature.getSignatureStatus());
                item.setSubmissionMode(signature.getSubmissionMode());
            }
            scoreItems.add(item);
        }
        row.setScoreItems(scoreItems);
        row.setCurrentAverageScore(average(numericValues));
        if (result != null) {
            row.setResultAverageScore(result.getAverageScore());
            row.setResultGeneratedAt(result.getUpdateTime() == null ? result.getCreateTime() : result.getUpdateTime());
            row.setResultStatus(result.getResultStatus());
        }
        ReviewScoreWarningService.ScoreWarningRule rule = scoreWarningService.resolve(project.getActivityId(), project.getCategoryId());
        ReviewScoreWarningService.ScoreWarningEvaluation evaluation = scoreWarningService.evaluate(warningValues, scores.size(), rule);
        ReviewScoreSummaryWarningVo warning = new ReviewScoreSummaryWarningVo();
        warning.setStatus(evaluation.status());
        warning.setText(evaluation.displayText());
        warning.setSpreadPercent(evaluation.differencePercent());
        warning.setThresholdPercent(rule.thresholdPercent());
        warning.setFormulaType(rule.formulaType());
        warning.setFormulaExpression(rule.formulaExpression());
        row.setWarning(warning);
        return row;
    }

    private ReviewScoreSheetSignedSheet selectPreferredSignedSheet(List<ReviewScoreSheetSignedSheet> sheets) {
        if (sheets == null || sheets.isEmpty()) {
            return null;
        }
        return sheets.stream().sorted((left, right) -> {
                int active = Boolean.compare(isActiveSignedSheet(right), isActiveSignedSheet(left));
                if (active != 0) {
                    return active;
                }
                int signedAt = Comparator.nullsLast(Date::compareTo).compare(right.getSignedAt(), left.getSignedAt());
                if (signedAt != 0) {
                    return signedAt;
                }
                return Comparator.nullsLast(Long::compareTo).compare(right.getId(), left.getId());
            })
            .findFirst().orElse(null);
    }

    private boolean isActiveSignedSheet(ReviewScoreSheetSignedSheet sheet) {
        return sheet != null && SIGNED_SHEET_ACTIVE.equalsIgnoreCase(sheet.getStatus())
            && Objects.equals(sheet.getActiveMarker(), 1);
    }

    private ReviewScoreSummarySignatureVo toSignatureVo(ReviewScoreSheetSignedSheet sheet, Map<Long, String> signatureUrls) {
        ReviewScoreSummarySignatureVo vo = new ReviewScoreSummarySignatureVo();
        vo.setSignedSheetId(sheet.getId());
        vo.setSignatureId(sheet.getSignatureId());
        vo.setSignatureUrl(sheet.getSignatureOssId() == null ? null : signatureUrls.get(sheet.getSignatureOssId()));
        vo.setSignatureName(sheet.getSignatureNameSnapshot());
        vo.setSignatureSignedAt(sheet.getSignedAt());
        vo.setSubmissionMode("UNSIGNED".equalsIgnoreCase(sheet.getSubmissionMode()) ? "UNSIGNED" : "SIGNED");
        vo.setSignatureStatus(SIGNED_SHEET_ACTIVE.equalsIgnoreCase(sheet.getStatus())
            && Objects.equals(sheet.getActiveMarker(), 1) ? SIGNED_SHEET_ACTIVE : "withdrawn");
        vo.setWithdrawnAt(sheet.getWithdrawnAt());
        vo.setWithdrawnByName(sheet.getWithdrawnByNameSnapshot());
        vo.setWithdrawalMode(sheet.getWithdrawalMode());
        vo.setWithdrawReason(sheet.getWithdrawReason());
        return vo;
    }

    private BigDecimal snapshotScore(String snapshotJson) {
        if (StringUtils.isBlank(snapshotJson)) {
            return null;
        }
        Map<String, Object> snapshot = parseObject(snapshotJson);
        Object scoreValue = snapshot.get("scoreValue");
        if (scoreValue == null) {
            return gradePoint(snapshot.get("gradeValue") == null ? null : String.valueOf(snapshot.get("gradeValue")));
        }
        try {
            return new BigDecimal(String.valueOf(scoreValue));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private ReviewScoreSummaryDetailVo copyScoreSummaryDetail(ReviewScoreSummaryVo source) {
        ReviewScoreSummaryDetailVo target = new ReviewScoreSummaryDetailVo();
        target.setRowNo(source.getRowNo());
        target.setActivityId(source.getActivityId());
        target.setActivityName(source.getActivityName());
        target.setCategoryId(source.getCategoryId());
        target.setCategoryName(source.getCategoryName());
        target.setProjectId(source.getProjectId());
        target.setProjectNo(source.getProjectNo());
        target.setProjectName(source.getProjectName());
        target.setSchoolId(source.getSchoolId());
        target.setSchoolName(source.getSchoolName());
        target.setGroupOrNature(source.getGroupOrNature());
        target.setProgramForm(source.getProgramForm());
        target.setScoreItems(source.getScoreItems());
        target.setCurrentAverageScore(source.getCurrentAverageScore());
        target.setResultAverageScore(source.getResultAverageScore());
        target.setResultGeneratedAt(source.getResultGeneratedAt());
        target.setWarning(source.getWarning());
        target.setResultStatus(source.getResultStatus());
        return target;
    }

    private record ScoreSummaryContext(Map<Long, Activity> activityMap,
                                       Map<Long, ActivityCategory> categoryMap,
                                       Map<Long, SchoolInfo> schoolMap,
                                       Map<Long, ReviewResult> resultMap,
                                       Map<Long, List<ReviewScore>> scoresByProject,
                                       Map<Long, SysUser> userMap,
                                       Map<Long, List<ReviewScoreSheetSignedSheet>> sheetsByScore,
                                       Map<Long, String> signatureUrls,
                                       Map<Long, String> scoreSnapshots) {
    }

    @Override
    public List<ShowcaseOrderVo> queryShowcaseOrders(ShowcaseOrderVo query) {
        List<Project> projects = projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
            .eq(query.getActivityId() != null, Project::getActivityId, query.getActivityId())
            .eq(query.getCategoryId() != null, Project::getCategoryId, query.getCategoryId())
            .orderByAsc(Project::getActivityId)
            .orderByAsc(Project::getCategoryId)
            .orderByAsc(Project::getProjectNo)
            .orderByAsc(Project::getId));
        if (projects.isEmpty()) {
            return List.of();
        }
        Map<Long, Activity> activityMap = activityMapper.selectBatchIds(projects.stream().map(Project::getActivityId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(Activity::getId, item -> item, (left, right) -> left));
        Map<Long, ActivityCategory> categoryMap = categoryMapper.selectBatchIds(projects.stream().map(Project::getCategoryId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left));
        Map<Long, SchoolInfo> schoolMap = schoolInfoMapper.selectBatchIds(projects.stream().map(Project::getSchoolId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(SchoolInfo::getId, item -> item, (left, right) -> left));
        return projects.stream()
            .map(project -> toShowcaseOrderVo(project, activityMap, categoryMap, schoolMap))
            .filter(row -> matchesShowcaseFilter(row, query))
            .sorted(Comparator.comparing((ShowcaseOrderVo row) -> orderSortValue(row.getPerformanceOrder()))
                .thenComparing(row -> StringUtils.blankToDefault(row.getProjectNo(), ""))
                .thenComparing(ShowcaseOrderVo::getProjectId, Comparator.nullsLast(Long::compareTo)))
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveShowcaseOrders(ShowcaseOrderBo bo) {
        requireShowcaseScope(bo.getActivityId(), bo.getCategoryId());
        List<ShowcaseOrderBo.Item> items = bo.getItems() == null ? List.of() : bo.getItems();
        if (items.isEmpty()) {
            return;
        }
        for (ShowcaseOrderBo.Item item : items) {
            if (item.getProjectId() == null) {
                continue;
            }
            Project project = projectMapper.selectById(item.getProjectId());
            if (project == null || !Objects.equals(project.getActivityId(), bo.getActivityId()) || !Objects.equals(project.getCategoryId(), bo.getCategoryId())) {
                throw new ServiceException("项目不属于当前活动类别");
            }
            ActivityCategory category = categoryMapper.selectById(project.getCategoryId());
            String before = formValue(project, category, "performanceOrder", PERFORMANCE_ORDER_KEYS);
            String after = StringUtils.trim(item.getPerformanceOrder());
            if (Objects.equals(blankToNull(before), blankToNull(after))) {
                continue;
            }
            Map<String, Object> form = new LinkedHashMap<>(parseObject(project.getFormDataJson()));
            List<String> orderKeys = canonicalKeys(category, "performanceOrder", PERFORMANCE_ORDER_KEYS);
            orderKeys.forEach(form::remove);
            if (StringUtils.isBlank(after)) {
                // 已清理所有顺序字段别名。
            } else {
                form.put(orderKeys.isEmpty() ? "performanceOrder" : orderKeys.get(0), after);
            }
            project.setFormDataJson(JsonUtils.toJsonString(form));
            projectMapper.updateById(project);
            logAction(bo.getActivityId(), bo.getCategoryId(), project.getId(), null, ArtReviewConstants.RESULT_LOG_TARGET_PROJECT,
                ArtReviewConstants.RESULT_LOG_ACTION_SHOWCASE_ORDER_SAVE,
                orderChangeJson(blankToNull(before)),
                orderChangeJson(StringUtils.blankToDefault(after, "")),
                StringUtils.blankToDefault(bo.getRemark(), "\u4fdd\u5b58\u5c55\u6f14\u987a\u5e8f"));
        }
    }

    // ART-REF: BE.RESULT.QUERY -> result/service/query/ReviewResultQueryService.java
    private LambdaQueryWrapper<ReviewResult> baseResultQuery(ReviewResultVo query) {
        return Wrappers.lambdaQuery(ReviewResult.class)
            .eq(query.getActivityId() != null, ReviewResult::getActivityId, query.getActivityId())
            .eq(query.getCategoryId() != null, ReviewResult::getCategoryId, query.getCategoryId())
            .eq(query.getProjectId() != null, ReviewResult::getProjectId, query.getProjectId())
            .eq(query.getSchoolId() != null, ReviewResult::getSchoolId, query.getSchoolId())
            .eq(StringUtils.isNotBlank(query.getResultStatus()), ReviewResult::getResultStatus, query.getResultStatus());
    }

    private ShowcaseOrderVo toShowcaseOrderVo(Project project, Map<Long, Activity> activityMap,
                                              Map<Long, ActivityCategory> categoryMap,
                                              Map<Long, SchoolInfo> schoolMap) {
        ShowcaseOrderVo row = new ShowcaseOrderVo();
        Activity activity = activityMap.get(project.getActivityId());
        ActivityCategory category = categoryMap.get(project.getCategoryId());
        SchoolInfo school = schoolMap.get(project.getSchoolId());
        row.setActivityId(project.getActivityId());
        row.setCategoryId(project.getCategoryId());
        row.setProjectId(project.getId());
        row.setActivityName(activity == null ? null : activity.getActivityName());
        row.setCategoryName(category == null ? null : category.getCategoryName());
        row.setProjectNo(project.getProjectNo());
        row.setProjectName(project.getProjectName());
        row.setSchoolName(school == null ? null : school.getSchoolName());
        row.setGroupName(groupName(project, category));
        row.setProjectType(blankToNull(formValue(project, category, "projectType", PROJECT_TYPE_KEYS)));
        row.setProgramForm(blankToNull(formValue(project, category, "programForm", PROGRAM_FORM_KEYS)));
        row.setPerformanceOrder(blankToNull(formValue(project, category, "performanceOrder", PERFORMANCE_ORDER_KEYS)));
        row.setStatus(summaryLabel(project.getStatus()));
        row.setSubmittedAt(project.getSubmittedAt());
        row.setCreateTime(project.getCreateTime());
        return row;
    }

    private ReviewProjectOverviewVo toProjectOverviewVo(Project project, Map<Long, Activity> activityMap,
                                                        Map<Long, ActivityCategory> categoryMap,
                                                        Map<Long, SchoolInfo> schoolMap,
                                                        Map<Long, ReviewResult> resultMap,
                                                        Map<Long, Long> assignmentCountByCategory,
                                                        Map<Long, Long> submittedScoreCount,
                                                        Map<Long, Long> draftScoreCount) {
        ReviewProjectOverviewVo row = new ReviewProjectOverviewVo();
        Activity activity = activityMap.get(project.getActivityId());
        ActivityCategory category = categoryMap.get(project.getCategoryId());
        SchoolInfo school = schoolMap.get(project.getSchoolId());
        ReviewResult result = resultMap.get(project.getId());
        row.setActivityId(project.getActivityId());
        row.setActivityName(activity == null ? null : activity.getActivityName());
        row.setCategoryId(project.getCategoryId());
        row.setCategoryName(category == null ? null : category.getCategoryName());
        row.setProjectId(project.getId());
        row.setProjectNo(project.getProjectNo());
        row.setProjectName(project.getProjectName());
        row.setSchoolId(project.getSchoolId());
        row.setSchoolName(school == null ? null : school.getSchoolName());
        row.setGroupName(groupName(project, category));
        row.setProjectType(blankToNull(formValue(project, category, "projectType", PROJECT_TYPE_KEYS)));
        row.setProgramForm(blankToNull(formValue(project, category, "programForm", PROGRAM_FORM_KEYS)));
        row.setPerformanceOrder(blankToNull(formValue(project, category, "performanceOrder", PERFORMANCE_ORDER_KEYS)));
        row.setStatus(summaryLabel(project.getStatus()));
        row.setSubmittedAt(project.getSubmittedAt());
        row.setCreateTime(project.getCreateTime());
        row.setAssignmentCount(assignmentCountByCategory.getOrDefault(project.getCategoryId(), 0L));
        row.setSubmittedScoreCount(submittedScoreCount.getOrDefault(project.getId(), 0L));
        row.setDraftScoreCount(draftScoreCount.getOrDefault(project.getId(), 0L));
        if (result != null) {
            row.setAverageScore(result.getAverageScore());
            row.setFinalGrade(result.getFinalGrade());
            row.setAwardLevel(result.getAwardLevel());
            row.setRankNo(result.getRankNo());
            row.setResultStatus(result.getResultStatus());
        }
        return row;
    }

    private Map<Long, SchoolInfo> projectSchoolMap(List<Project> projects) {
        List<Long> schoolIds = projects.stream().map(Project::getSchoolId).filter(Objects::nonNull).distinct().toList();
        if (schoolIds.isEmpty()) {
            return Map.of();
        }
        return schoolInfoMapper.selectBatchIds(schoolIds)
            .stream().collect(Collectors.toMap(SchoolInfo::getId, item -> item, (left, right) -> left));
    }

    private Map<Long, ReviewResult> resultByProjectMap(List<Project> projects) {
        List<Long> projectIds = projects.stream().map(Project::getId).filter(Objects::nonNull).distinct().toList();
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        return resultMapper.selectList(Wrappers.lambdaQuery(ReviewResult.class)
                .in(ReviewResult::getProjectId, projectIds))
            .stream().collect(Collectors.toMap(ReviewResult::getProjectId, item -> item, (left, right) -> left));
    }

    private Map<Long, Long> scoreCountByProject(List<Project> projects, List<ReviewAssignment> assignments, String status) {
        List<Long> projectIds = projects.stream().map(Project::getId).filter(Objects::nonNull).distinct().toList();
        List<Long> assignmentIds = assignments.stream().map(ReviewAssignment::getId).filter(Objects::nonNull).distinct().toList();
        if (projectIds.isEmpty() || assignmentIds.isEmpty()) {
            return Map.of();
        }
        return scoreMapper.selectList(Wrappers.lambdaQuery(ReviewScore.class)
                .in(ReviewScore::getProjectId, projectIds)
                .in(ReviewScore::getAssignmentId, assignmentIds)
                .eq(ReviewScore::getStatus, status))
            .stream().collect(Collectors.groupingBy(ReviewScore::getProjectId, Collectors.counting()));
    }

    private boolean matchesShowcaseFilter(ShowcaseOrderVo row, ShowcaseOrderVo query) {
        if (StringUtils.isNotBlank(query.getGroupName()) && !containsText(row.getGroupName(), query.getGroupName())) {
            return false;
        }
        if (StringUtils.isNotBlank(query.getProgramForm()) && !containsText(row.getProgramForm(), query.getProgramForm())) {
            return false;
        }
        return true;
    }

    private boolean containsText(String source, String keyword) {
        return StringUtils.isBlank(keyword) || StringUtils.blankToDefault(source, "").contains(keyword);
    }

    private int orderSortValue(String value) {
        if (StringUtils.isBlank(value)) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE - 1;
        }
    }

    private String orderChangeJson(String performanceOrder) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("performanceOrder", performanceOrder);
        return JsonUtils.toJsonString(payload);
    }

    private List<ProjectUploadSummaryVo.Item> buildSummaryItems(Map<String, Long> counts, long total) {
        return counts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .map(entry -> {
                ProjectUploadSummaryVo.Item item = new ProjectUploadSummaryVo.Item();
                item.setKey(entry.getKey());
                item.setLabel(summaryLabel(entry.getKey()));
                item.setCount(entry.getValue());
                item.setPercent(total <= 0 ? 0D : BigDecimal.valueOf(entry.getValue() * 100D / total).setScale(2, RoundingMode.HALF_UP).doubleValue());
                return item;
            }).toList();
    }

    private String summaryLabel(String key) {
        if (StringUtils.isBlank(key)) {
            return UNFILLED_LABEL;
        }
        return switch (key) {
            case ArtReviewConstants.PROJECT_DRAFT -> "\u8349\u7a3f";
            case ArtReviewConstants.PROJECT_SUBMITTED -> "\u5f85\u5ba1\u6838";
            case ArtReviewConstants.PROJECT_RETURNED -> "\u5df2\u9000\u56de";
            case ArtReviewConstants.PROJECT_AUDIT_PASSED -> "\u5df2\u901a\u8fc7";
            default -> key;
        };
    }

    private String formValue(Project project, String... keys) {
        Map<String, Object> form = parseObject(project.getFormDataJson());
        for (String key : keys) {
            Object value = form.get(key);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return UNFILLED_LABEL;
    }

    private String formValue(Project project, ActivityCategory category, String canonicalKey, String... fallbackKeys) {
        Map<String, Object> form = parseObject(project.getFormDataJson());
        for (String key : canonicalKeys(category, canonicalKey, fallbackKeys)) {
            Object value = form.get(key);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return UNFILLED_LABEL;
    }

    private String groupName(Project project, ActivityCategory category) {
        return StringUtils.blankToDefault(blankToNull(formValue(project, category, "groupName", GROUP_NAME_KEYS)),
            StringUtils.blankToDefault(project.getGroupName(), StringUtils.blankToDefault(project.getGroupCode(), UNFILLED_LABEL)));
    }

    private List<String> canonicalKeys(ActivityCategory category, String canonicalKey, String... fallbackKeys) {
        List<String> keys = new ArrayList<>();
        Map<String, Object> rule = parseObject(category == null ? null : category.getRuleJson());
        Object mappings = rule.get("canonicalFieldKeys");
        if (mappings instanceof Map<?, ?> map) {
            Object configured = map.get(canonicalKey);
            if (configured instanceof List<?> list) {
                list.stream().filter(Objects::nonNull).map(String::valueOf).filter(StringUtils::isNotBlank).forEach(keys::add);
            } else if (configured != null && StringUtils.isNotBlank(String.valueOf(configured))) {
                keys.add(String.valueOf(configured));
            }
        }
        for (String key : fallbackKeys) {
            if (StringUtils.isNotBlank(key) && !keys.contains(key)) {
                keys.add(key);
            }
        }
        return keys;
    }

    private String blankToNull(String value) {
        return StringUtils.isBlank(value) || UNFILLED_LABEL.equals(value) ? null : value;
    }

    private Map<String, Object> parseObject(String text) {
        if (StringUtils.isBlank(text) || !JsonUtils.isJsonObject(text)) {
            return Map.of();
        }
        Map<String, Object> parsed = JsonUtils.parseObject(text, new TypeReference<Map<String, Object>>() {
        });
        return parsed == null ? Map.of() : parsed;
    }

    private String stringOption(Map<String, Object> options, String... keys) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        for (String key : keys) {
            Object value = options.get(key);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private void requireScope(ReviewResultBo bo) {
        if (bo.getActivityId() == null || bo.getCategoryId() == null) {
            throw new ServiceException("请选择活动和类别");
        }
        Activity activity = activityMapper.selectById(bo.getActivityId());
        ActivityCategory category = categoryMapper.selectById(bo.getCategoryId());
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        if (category == null || !Objects.equals(category.getActivityId(), bo.getActivityId())) {
            throw new ServiceException("类别不属于当前活动");
        }
    }

    private void lockResultScope(ReviewResultBo bo) {
        requireScope(bo);
        categoryMapper.selectOne(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getId, bo.getCategoryId())
            .eq(ActivityCategory::getActivityId, bo.getActivityId())
            .last("for update"));
    }

    private void requireScope(ReviewAwardRuleBo bo) {
        ReviewResultBo scope = new ReviewResultBo();
        scope.setActivityId(bo.getActivityId());
        scope.setCategoryId(bo.getCategoryId());
        requireScope(scope);
    }

    private void requireShowcaseScope(Long activityId, Long categoryId) {
        ReviewResultBo scope = new ReviewResultBo();
        scope.setActivityId(activityId);
        scope.setCategoryId(categoryId);
        requireScope(scope);
    }

    private void validateAwardRule(ReviewAwardRuleItemBo item) {
        String ruleType = StringUtils.blankToDefault(item.getRuleType(), ArtReviewConstants.AWARD_RULE_RANK_RANGE);
        if (ArtReviewConstants.AWARD_RULE_RANK_RANGE.equals(ruleType)) {
            if (item.getMinRank() == null || item.getMaxRank() == null || item.getMinRank() <= 0 || item.getMaxRank() < item.getMinRank()) {
                throw new ServiceException("名次规则需要填写有效的起止名次");
            }
            return;
        }
        if (ArtReviewConstants.AWARD_RULE_SCORE_RANGE.equals(ruleType)) {
            if (item.getMinScore() == null && item.getMaxScore() == null) {
                throw new ServiceException("分数规则至少填写最低分或最高分");
            }
            if (item.getMinScore() != null && item.getMaxScore() != null && item.getMaxScore().compareTo(item.getMinScore()) < 0) {
                throw new ServiceException("最高分不能低于最低分");
            }
            return;
        }
        throw new ServiceException("未知奖项规则类型");
    }

    private ReviewResult getOrCreateResult(Project project) {
        ReviewResult result = resultMapper.selectOne(Wrappers.lambdaQuery(ReviewResult.class)
            .eq(ReviewResult::getActivityId, project.getActivityId())
            .eq(ReviewResult::getCategoryId, project.getCategoryId())
            .eq(ReviewResult::getProjectId, project.getId())
            .last("limit 1"));
        if (result == null) {
            result = new ReviewResult();
            result.setActivityId(project.getActivityId());
            result.setCategoryId(project.getCategoryId());
            result.setProjectId(project.getId());
            result.setSchoolId(project.getSchoolId());
            result.setResultStatus(ArtReviewConstants.RESULT_DRAFT);
        }
        return result;
    }

    // ART-REF: BE.RESULT.GENERATE -> result/service/generate/ReviewResultGenerateService.java
    private void applyAggregate(ReviewResult result, List<ReviewScore> scores, ScoreAggregateRule rule, Project project) {
        List<ReviewScore> submitted = scores.stream()
            .filter(score -> ArtReviewConstants.REVIEW_SCORE_SUBMITTED.equals(score.getStatus()))
            .toList();
        result.setScoreCount(submitted.size());
        if (submitted.isEmpty() || (rule.minReviewers() > 0 && submitted.size() < rule.minReviewers())) {
            result.setTotalScore(null);
            result.setAverageScore(null);
            result.setFinalGrade(null);
            result.setScoreSummaryJson(buildScoreSummary(submitted, rule, submitted.isEmpty() ? null : "评分人数不足，至少需要 " + rule.minReviewers() + " 位评委"));
            return;
        }
        if (rule.maxReviewers() > 0 && submitted.size() > rule.maxReviewers()) {
            throw new ServiceException("项目“" + StringUtils.blankToDefault(project.getProjectName(), project.getProjectNo()) + "”评分人数超过上限 " + rule.maxReviewers() + " 位");
        }

        List<BigDecimal> numericValues = submitted.stream()
            .map(score -> score.getScoreValue() != null ? score.getScoreValue() : gradePoint(score.getGradeValue()))
            .filter(Objects::nonNull)
            .sorted()
            .toList();
        if (numericValues.isEmpty()) {
            result.setTotalScore(null);
            result.setAverageScore(null);
        } else {
            List<BigDecimal> valuesForAverage = aggregateValues(numericValues, rule);
            BigDecimal total = valuesForAverage.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            result.setTotalScore(total.setScale(2, RoundingMode.HALF_UP));
            result.setAverageScore(total.divide(BigDecimal.valueOf(valuesForAverage.size()), rule.precision(), RoundingMode.HALF_UP));
        }
        result.setFinalGrade(resolveFinalGrade(submitted));
        result.setScoreSummaryJson(buildScoreSummary(submitted, rule, null));
    }

    private BigDecimal gradePoint(String grade) {
        if (StringUtils.isBlank(grade)) {
            return null;
        }
        return DEFAULT_GRADE_POINTS.get(StringUtils.trim(grade).toUpperCase());
    }

    private String resolveFinalGrade(List<ReviewScore> scores) {
        Map<String, Long> countMap = scores.stream()
            .map(ReviewScore::getGradeValue)
            .filter(StringUtils::isNotBlank)
            .map(StringUtils::trim)
            .collect(Collectors.groupingBy(item -> item, LinkedHashMap::new, Collectors.counting()));
        if (countMap.isEmpty()) {
            return null;
        }
        return countMap.entrySet().stream()
            .sorted((left, right) -> {
                int byCount = Long.compare(right.getValue(), left.getValue());
                if (byCount != 0) {
                    return byCount;
                }
                BigDecimal leftPoint = gradePoint(left.getKey());
                BigDecimal rightPoint = gradePoint(right.getKey());
                if (leftPoint == null && rightPoint == null) {
                    return left.getKey().compareTo(right.getKey());
                }
                if (leftPoint == null) {
                    return 1;
                }
                if (rightPoint == null) {
                    return -1;
                }
                return rightPoint.compareTo(leftPoint);
            })
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }

    private List<BigDecimal> aggregateValues(List<BigDecimal> values, ScoreAggregateRule rule) {
        if ("trim_extreme".equals(rule.aggregateMethod()) && values.size() >= 5) {
            return values.subList(1, values.size() - 1);
        }
        return values;
    }

    private ScoreAggregateRule resolveAggregateRule(List<ReviewAssignment> assignments) {
        for (ReviewAssignment assignment : assignments) {
            Map<String, Object> rule = parseObject(assignment.getScoreRuleJson());
            if (!rule.isEmpty()) {
                return new ScoreAggregateRule(
                    intRule(rule, "minReviewers", 0),
                    intRule(rule, "maxReviewers", 0),
                    StringUtils.blankToDefault(stringRule(rule, "aggregateMethod"), "average"),
                    Math.max(0, intRule(rule, "precision", 2))
                );
            }
        }
        return new ScoreAggregateRule(0, 0, "average", 2);
    }

    private int intRule(Map<String, Object> rule, String key, int defaultValue) {
        Object value = rule.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String stringRule(Map<String, Object> rule, String key) {
        Object value = rule.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String buildScoreSummary(List<ReviewScore> scores, ScoreAggregateRule rule, String invalidReason) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("minReviewers", rule.minReviewers());
        summary.put("maxReviewers", rule.maxReviewers());
        summary.put("aggregateMethod", rule.aggregateMethod());
        summary.put("precision", rule.precision());
        summary.put("invalidReason", invalidReason);
        List<Map<String, Object>> items = scores.stream().map(score -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("reviewerUserId", score.getReviewerUserId());
            item.put("scoreValue", score.getScoreValue());
            item.put("gradeValue", score.getGradeValue());
            item.put("commentText", score.getCommentText());
            item.put("submittedAt", score.getSubmittedAt());
            return item;
        }).toList();
        summary.put("scores", items);
        return JsonUtils.toJsonString(summary);
    }

    private record ScoreAggregateRule(int minReviewers, int maxReviewers, String aggregateMethod, int precision) {
    }

    private void refreshRanks(Long activityId, Long categoryId, List<ReviewResult> generated) {
        List<ReviewResult> ranked = generated.stream()
            .filter(result -> result.getAverageScore() != null)
            .sorted(Comparator.comparing(ReviewResult::getAverageScore).reversed()
                .thenComparing(ReviewResult::getId, Comparator.nullsLast(Long::compareTo)))
            .toList();
        BigDecimal previousScore = null;
        int previousRank = 0;
        for (int i = 0; i < ranked.size(); i++) {
            ReviewResult result = ranked.get(i);
            int rank = previousScore != null && previousScore.compareTo(result.getAverageScore()) == 0 ? previousRank : i + 1;
            result.setRankNo(rank);
            resultMapper.updateById(result);
            previousScore = result.getAverageScore();
            previousRank = rank;
        }
        resultMapper.selectList(Wrappers.lambdaQuery(ReviewResult.class)
                .eq(ReviewResult::getActivityId, activityId)
                .eq(ReviewResult::getCategoryId, categoryId)
                .isNull(ReviewResult::getAverageScore))
            .forEach(result -> {
                result.setRankNo(null);
                resultMapper.updateById(result);
            });
    }

    private void applyAwardRules(Long activityId, Long categoryId) {
        List<ReviewAwardRule> rules = awardRuleMapper.selectList(Wrappers.lambdaQuery(ReviewAwardRule.class)
            .eq(ReviewAwardRule::getActivityId, activityId)
            .eq(ReviewAwardRule::getCategoryId, categoryId)
            .eq(ReviewAwardRule::getEnabled, true)
            .orderByAsc(ReviewAwardRule::getSortOrder)
            .orderByAsc(ReviewAwardRule::getId));
        List<ReviewResult> results = resultMapper.selectList(Wrappers.lambdaQuery(ReviewResult.class)
            .eq(ReviewResult::getActivityId, activityId)
            .eq(ReviewResult::getCategoryId, categoryId));
        for (ReviewResult result : results) {
            ReviewAwardRule matched = matchAwardRule(result, rules);
            result.setAwardLevel(matched == null ? null : matched.getAwardLevel());
            result.setAwardRemark(matched == null ? null : matched.getRemark());
            resultMapper.updateById(result);
        }
    }

    private ReviewAwardRule matchAwardRule(ReviewResult result, List<ReviewAwardRule> rules) {
        for (ReviewAwardRule rule : rules) {
            if (ArtReviewConstants.AWARD_RULE_RANK_RANGE.equals(rule.getRuleType()) && result.getRankNo() != null) {
                boolean minOk = rule.getMinRank() == null || result.getRankNo() >= rule.getMinRank();
                boolean maxOk = rule.getMaxRank() == null || result.getRankNo() <= rule.getMaxRank();
                if (minOk && maxOk) {
                    return rule;
                }
            }
            if (ArtReviewConstants.AWARD_RULE_SCORE_RANGE.equals(rule.getRuleType()) && result.getAverageScore() != null) {
                boolean minOk = rule.getMinScore() == null || result.getAverageScore().compareTo(rule.getMinScore()) >= 0;
                boolean maxOk = rule.getMaxScore() == null || result.getAverageScore().compareTo(rule.getMaxScore()) <= 0;
                if (minOk && maxOk) {
                    return rule;
                }
            }
        }
        return null;
    }

    private void logAction(Long activityId, Long categoryId, Long projectId, Long scoreId, String targetType,
                           String actionType, String beforeJson, String afterJson, String reason) {
        ReviewResultLog log = new ReviewResultLog();
        log.setActivityId(activityId);
        log.setCategoryId(categoryId);
        log.setProjectId(projectId);
        log.setScoreId(scoreId);
        log.setTargetType(targetType);
        log.setActionType(actionType);
        log.setBeforeJson(beforeJson);
        log.setAfterJson(afterJson);
        log.setReason(StringUtils.trim(reason));
        log.setOperatedBy(LoginHelper.getUserId());
        log.setOperatedAt(new Date());
        resultLogMapper.insert(log);
    }

    // ART-REF: BE.RESULT.LOG -> result/service/log/ReviewResultLogService.java
    private void fillNames(List<ReviewResultVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, Activity> activityMap = activityMapper.selectBatchIds(rows.stream().map(ReviewResultVo::getActivityId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(Activity::getId, item -> item, (left, right) -> left));
        Map<Long, ActivityCategory> categoryMap = categoryMapper.selectBatchIds(rows.stream().map(ReviewResultVo::getCategoryId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left));
        Map<Long, Project> projectMap = projectMapper.selectBatchIds(rows.stream().map(ReviewResultVo::getProjectId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(Project::getId, item -> item, (left, right) -> left));
        Map<Long, SchoolInfo> schoolMap = schoolInfoMapper.selectBatchIds(rows.stream().map(ReviewResultVo::getSchoolId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(SchoolInfo::getId, item -> item, (left, right) -> left));
        for (ReviewResultVo row : rows) {
            Activity activity = activityMap.get(row.getActivityId());
            ActivityCategory category = categoryMap.get(row.getCategoryId());
            Project project = projectMap.get(row.getProjectId());
            SchoolInfo school = schoolMap.get(row.getSchoolId());
            row.setActivityName(activity == null ? null : activity.getActivityName());
            row.setCategoryName(category == null ? null : category.getCategoryName());
            row.setProjectNo(project == null ? null : project.getProjectNo());
            row.setProjectName(project == null ? null : project.getProjectName());
            row.setSchoolName(school == null ? null : school.getSchoolName());
        }
    }

    // ART-REF: BE.RESULT.VISIBILITY -> result/service/visibility/ResultVisibilityService.java
    private void maskSchoolResultVisibility(List<ReviewResultVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (ReviewResultVo row : rows) {
            if (!Boolean.TRUE.equals(row.getShowScore())) {
                row.setScoreCount(null);
                row.setTotalScore(null);
                row.setAverageScore(null);
                row.setFinalGrade(null);
            }
            if (Boolean.FALSE.equals(row.getShowRank())) {
                row.setRankNo(null);
            }
            if (!Boolean.TRUE.equals(row.getShowComment())) {
                row.setScoreSummaryJson(null);
            } else if (!Boolean.TRUE.equals(row.getShowScore())) {
                row.setScoreSummaryJson(maskScoreSummaryCommentsOnly(row.getScoreSummaryJson()));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String maskScoreSummaryCommentsOnly(String scoreSummaryJson) {
        if (StringUtils.isBlank(scoreSummaryJson) || !JsonUtils.isJsonObject(scoreSummaryJson)) {
            return null;
        }
        Map<String, Object> summary = JsonUtils.parseObject(scoreSummaryJson, new TypeReference<Map<String, Object>>() {
        });
        if (summary == null) {
            return null;
        }
        Object scoresObj = summary.get("scores");
        List<Map<String, Object>> comments = new ArrayList<>();
        if (scoresObj instanceof Iterable<?> iterable) {
            for (Object itemObj : iterable) {
                if (!(itemObj instanceof Map<?, ?> raw)) {
                    continue;
                }
                Object comment = raw.get("commentText");
                if (comment == null || StringUtils.isBlank(String.valueOf(comment))) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("commentText", comment);
                item.put("submittedAt", raw.get("submittedAt"));
                comments.add(item);
            }
        }
        return JsonUtils.toJsonString(Map.of("comments", comments));
    }

    private void fillLogUsers(List<ReviewResultLogVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(rows.stream().map(ReviewResultLogVo::getOperatedBy).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(SysUser::getUserId, item -> item, (left, right) -> left));
        for (ReviewResultLogVo row : rows) {
            SysUser user = userMap.get(row.getOperatedBy());
            row.setOperatedByName(user == null ? null : StringUtils.blankToDefault(user.getNickName(), user.getUserName()));
        }
    }
}
