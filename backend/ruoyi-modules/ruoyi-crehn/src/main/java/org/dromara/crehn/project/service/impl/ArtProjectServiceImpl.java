package org.dromara.crehn.project.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dromara.crehn.audit.service.IArtAuditAssignmentService;
import org.dromara.crehn.activity.service.ArtCategoryHierarchyService;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.common.ArtReviewSecurity;
import org.dromara.crehn.config.service.rule.ActivityReportRuleRuntime;
import org.dromara.crehn.config.service.rule.ActivityReportRuleRuntimeService;
import org.dromara.crehn.config.service.IArtActivityScopeService;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ProjectFile;
import org.dromara.crehn.domain.ProjectMember;
import org.dromara.crehn.domain.ReviewResult;
import org.dromara.crehn.domain.ReviewScore;
import org.dromara.crehn.domain.SchoolInfo;
import org.dromara.crehn.domain.bo.ProjectFileDirectUploadCompleteBo;
import org.dromara.crehn.domain.bo.ProjectFileDirectUploadInitBo;
import org.dromara.crehn.domain.bo.ProjectSaveBo;
import org.dromara.crehn.domain.vo.ProjectCategoryStatsVo;
import org.dromara.crehn.domain.vo.GenericTableImportResultVo;
import org.dromara.crehn.domain.vo.MemberAttachmentBatchResultVo;
import org.dromara.crehn.domain.vo.ProjectFileDirectUploadInitVo;
import org.dromara.crehn.domain.vo.ProjectFileVo;
import org.dromara.crehn.domain.vo.ProjectQuotaOverviewVo;
import org.dromara.crehn.domain.vo.SchoolDashboardVo;
import org.dromara.crehn.domain.vo.ProjectViewStatsVo;
import org.dromara.crehn.domain.vo.ProjectVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ActivityMapper;
import org.dromara.crehn.mapper.ProjectAuditRecordMapper;
import org.dromara.crehn.mapper.ProjectFileMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.ProjectMemberMapper;
import org.dromara.crehn.mapper.ReviewResultMapper;
import org.dromara.crehn.mapper.ReviewScoreMapper;
import org.dromara.crehn.mapper.SchoolInfoMapper;
import org.dromara.crehn.project.service.IArtProjectService;
import org.dromara.crehn.project.service.assembler.ProjectDetailAssembler;
import org.dromara.crehn.project.service.file.ProjectFileService;
import org.dromara.crehn.project.service.query.ProjectQueryService;
import org.dromara.crehn.project.service.quota.ProjectQuotaGuard;
import org.dromara.crehn.project.service.scope.ProjectScopeGuard;
import org.dromara.crehn.project.service.submit.ProjectDraftSubmitService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.domain.SysUser;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysOssService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ArtProjectServiceImpl implements IArtProjectService {

    private static final long MEMBER_PHOTO_MAX_SIZE = 5L * 1024L * 1024L;
    private static final long MEMBER_REPORT_MAX_SIZE = 30L * 1024L * 1024L;
    private static final long MEMBER_ATTACHMENT_MAX_SIZE = 30L * 1024L * 1024L;
    private static final long GENERIC_TABLE_IMPORT_MAX_SIZE = 5L * 1024L * 1024L;
    private static final int GENERIC_TABLE_MAX_ROWS = 500;
    private static final Set<String> MEMBER_PHOTO_EXTS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> MEMBER_REPORT_EXTS = Set.of("pdf");
    private static final Set<String> MEMBER_ATTACHMENT_EXTS = Set.of("jpg", "jpeg", "png", "webp", "pdf", "doc", "docx", "xls", "xlsx");
    private static final String MEMBER_TEMPLATE_FILE_NAME = "师生信息采集表.xlsx";
    private static final String MEMBER_TEMPLATE_SHEET_NAME = "信息导入";
    private static final int MEMBER_TEMPLATE_BLANK_DATA_ROWS = 19;
    private static final Set<String> MEMBER_TEMPLATE_EXCLUDED_FIELD_TYPES = Set.of("image_upload", "pdf_upload", "attachment_upload");
    private static final Set<String> MEMBER_FIXED_FIELD_KEYS = Set.of("memberType", "name", "studentNo", "department", "major", "roleName", "status", "photo", "studentReport");
    private static final Map<String, String> MEMBER_IMPORT_HEADER_ALIASES = Map.ofEntries(
        Map.entry("序号", "orderNo"),
        Map.entry("类型", "memberType"),
        Map.entry("成员类型", "memberType"),
        Map.entry("姓名", "name"),
        Map.entry("身份证号护照号", "identityNo"),
        Map.entry("身份证号码", "identityNo"),
        Map.entry("身份证号", "identityNo"),
        Map.entry("护照号", "identityNo"),
        Map.entry("民族", "nation"),
        Map.entry("年龄", "age"),
        Map.entry("性别", "gender"),
        Map.entry("所在院系", "department"),
        Map.entry("院系", "department"),
        Map.entry("年级", "grade"),
        Map.entry("专业代码", "majorCode"),
        Map.entry("专业名称", "major"),
        Map.entry("专业", "major"),
        Map.entry("学号工作证号", "studentNo"),
        Map.entry("学号", "studentNo"),
        Map.entry("工作证号", "studentNo"),
        Map.entry("联系方式", "phone"),
        Map.entry("联系电话", "phone"),
        Map.entry("手机号", "phone"),
        Map.entry("电话", "phone"),
        Map.entry("备注", "roleName"),
        Map.entry("备注身份", "roleName"),
        Map.entry("身份", "roleName"),
        Map.entry("职务", "roleName"),
        Map.entry("角色", "roleName"),
        Map.entry("成员角色", "roleName"),
        Map.entry("角色声部岗位", "roleName"),
        Map.entry("分工", "roleName")
    );
    private static final String AUDIT_STATUS_ALL = "all";
    private static final String AUDIT_STATUS_AUDITED = "audited";

    private final ProjectMapper projectMapper;
    private final ProjectFileMapper projectFileMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectAuditRecordMapper auditRecordMapper;
    private final ReviewScoreMapper reviewScoreMapper;
    private final ReviewResultMapper reviewResultMapper;
    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final ArtCategoryHierarchyService categoryHierarchyService;
    private final SchoolInfoMapper schoolInfoMapper;
    private final ActivityReportRuleRuntimeService reportRuleRuntimeService;
    private final ISysOssService ossService;
    private final SysUserMapper sysUserMapper;
    private final ArtReviewSecurity artReviewSecurity;
    private final IArtActivityScopeService activityScopeService;
    private final IArtAuditAssignmentService auditAssignmentService;
    private final ProjectDetailAssembler projectDetailAssembler;
    private final ProjectFileService projectFileService;
    private final ProjectQuotaGuard projectQuotaGuard;
    private final ProjectScopeGuard projectScopeGuard;
    private final ProjectDraftSubmitService projectDraftSubmitService;
    private final ProjectQueryService projectQueryService;

    @Override
    // ART-REF: BE.PROJECT.QUERY -> project/service/query/ProjectQueryService.java
    public TableDataInfo<ProjectVo> queryMyProjectPage(ProjectVo query, PageQuery pageQuery) {
        return projectQueryService.queryMyProjectPage(query, pageQuery);
    }

    @Override
    public List<ProjectCategoryStatsVo> queryMyCategoryStats(Long activityId, List<Long> categoryIds) {
        return projectQueryService.queryMyCategoryStats(activityId, categoryIds);
    }

    @Override
    public ProjectQuotaOverviewVo queryMyQuotaOverview(Long activityId, List<Long> categoryIds) {
        Long schoolId = artReviewSecurity.requireEnabledSchoolId();
        SchoolInfo school = requireSchoolInfo(schoolId);
        List<Project> projects = selectQuotaOverviewProjects(activityId, schoolId, categoryIds);
        return buildQuotaOverview(activityId, school, categoryIds, projects);
    }

    @Override
    public SchoolDashboardVo queryMyDashboard(Long activityId) {
        return projectQueryService.queryMyDashboard(activityId);
    }

    @Override
    public TableDataInfo<ProjectVo> queryAuditProjectPage(ProjectVo query, PageQuery pageQuery) {
        return projectQueryService.queryAuditProjectPage(query, pageQuery);
    }

    @Override
    public ProjectQuotaOverviewVo queryAuditQuotaOverview(ProjectVo query) {
        ProjectVo safeQuery = query == null ? new ProjectVo() : query;
        String auditStatus = safeQuery.getStatus();
        safeQuery.setStatus(baseAuditStatus(auditStatus));
        LambdaQueryWrapper<Project> lqw = buildProjectQuery(safeQuery);
        applyAuditStatusFilter(lqw, auditStatus);
        lqw.ne(Project::getStatus, ArtReviewConstants.PROJECT_RECYCLED);
        List<Project> visibleProjects = auditAssignmentService.filterVisibleProjects(projectMapper.selectList(lqw));
        SchoolInfo school = safeQuery.getSchoolId() == null ? null : schoolInfoMapper.selectById(safeQuery.getSchoolId());
        return buildQuotaOverview(safeQuery.getActivityId(), school, safeQuery.getCategoryIds(), visibleProjects);
    }

    private String baseAuditStatus(String status) {
        String normalized = StringUtils.trimToEmpty(status);
        if (StringUtils.isBlank(normalized) || AUDIT_STATUS_ALL.equals(normalized) || AUDIT_STATUS_AUDITED.equals(normalized)) {
            return null;
        }
        if (ArtReviewConstants.PROJECT_SUBMITTED.equals(normalized)
            || ArtReviewConstants.PROJECT_AUDIT_PASSED.equals(normalized)
            || ArtReviewConstants.PROJECT_RETURNED.equals(normalized)) {
            return normalized;
        }
        return "__audit_status_not_allowed__";
    }

    private void applyAuditStatusFilter(LambdaQueryWrapper<Project> lqw, String status) {
        String normalized = StringUtils.trimToEmpty(status);
        if (AUDIT_STATUS_ALL.equals(normalized)) {
            lqw.in(Project::getStatus, ArtReviewConstants.PROJECT_SUBMITTED,
                ArtReviewConstants.PROJECT_AUDIT_PASSED, ArtReviewConstants.PROJECT_RETURNED);
            return;
        }
        if (AUDIT_STATUS_AUDITED.equals(normalized)) {
            lqw.in(Project::getStatus, ArtReviewConstants.PROJECT_AUDIT_PASSED, ArtReviewConstants.PROJECT_RETURNED);
            return;
        }
        if (StringUtils.isBlank(normalized)) {
            lqw.eq(Project::getStatus, ArtReviewConstants.PROJECT_SUBMITTED);
        }
    }

    @Override
    public TableDataInfo<ProjectVo> queryProjectViewPage(ProjectVo query, PageQuery pageQuery) {
        List<Project> visibleProjects = selectProjectViewVisibleProjects(query);
        List<ProjectVo> rows = visibleProjects.stream()
            .map(project -> projectMapper.selectVoById(project.getId()))
            .filter(Objects::nonNull)
            .toList();
        projectDetailAssembler.fillProjectSchoolNames(rows);
        projectDetailAssembler.maskSensitiveFormData(rows);
        return TableDataInfo.build(rows, pageQuery.build());
    }

    @Override
    public ProjectViewStatsVo queryProjectViewStats(ProjectVo query) {
        List<Project> visibleProjects = selectProjectViewVisibleProjects(query);
        ProjectViewStatsVo stats = new ProjectViewStatsVo();
        stats.setCategoryCounts(toProjectViewCountItems(visibleProjects.stream()
            .map(Project::getCategoryId)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(id -> id, LinkedHashMap::new, Collectors.counting()))));
        stats.setSchoolCounts(toProjectViewCountItems(visibleProjects.stream()
            .map(Project::getSchoolId)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(id -> id, LinkedHashMap::new, Collectors.counting()))));
        return stats;
    }

    @Override
    public ProjectQuotaOverviewVo queryProjectViewQuotaOverview(ProjectVo query) {
        ProjectVo safeQuery = query == null ? new ProjectVo() : query;
        List<Project> visibleProjects = selectProjectViewVisibleProjects(safeQuery);
        SchoolInfo school = safeQuery.getSchoolId() == null ? null : schoolInfoMapper.selectById(safeQuery.getSchoolId());
        return buildQuotaOverview(safeQuery.getActivityId(), school, safeQuery.getCategoryIds(), visibleProjects);
    }

    private ProjectQuotaOverviewVo buildQuotaOverview(Long activityId, SchoolInfo school, List<Long> categoryIds, List<Project> projects) {
        List<Project> safeProjects = projects == null ? List.of() : projects;
        Long resolvedActivityId = activityId != null ? activityId : safeProjects.stream().map(Project::getActivityId).filter(Objects::nonNull).findFirst().orElse(null);
        ProjectQuotaOverviewVo overview = new ProjectQuotaOverviewVo();
        overview.setActivityId(resolvedActivityId);
        if (school != null) {
            overview.setSchoolId(school.getId());
            overview.setSchoolName(school.getSchoolName());
            overview.setSchoolType(school.getSchoolType());
        }
        Activity activity = resolvedActivityId == null ? null : activityMapper.selectById(resolvedActivityId);
        if (activity != null) {
            overview.setActivityName(activity.getActivityName());
        }
        List<ActivityCategory> categories = selectQuotaOverviewCategories(resolvedActivityId, categoryIds);
        Map<Long, ActivityCategory> categoryMap = categories.stream()
            .filter(category -> category.getId() != null)
            .collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<ActivityReportRuleRuntime> rules = loadOverviewQuotaRules(resolvedActivityId, school);
        Map<Long, ProjectQuotaOverviewVo.Item> categoryItems = new LinkedHashMap<>();
        Map<String, ProjectQuotaOverviewVo.Item> groupItems = new LinkedHashMap<>();
        for (ActivityCategory category : categories) {
            ProjectQuotaOverviewVo.Item categoryItem = toQuotaCategoryItem(category, rules);
            categoryItems.put(category.getId(), categoryItem);
            ProjectQuotaOverviewVo.Item groupItem = ensureQuotaGroupItem(groupItems, category);
            mergeQuotaLimit(groupItem, categoryItem.getQuotaLimit());
        }
        applyGroupQuotaRuleLimits(groupItems, rules);
        for (Project project : safeProjects) {
            ActivityCategory category = categoryMap.get(project.getCategoryId());
            if (category == null) {
                continue;
            }
            ProjectQuotaOverviewVo.Item categoryItem = categoryItems.get(project.getCategoryId());
            ProjectQuotaOverviewVo.Item groupItem = ensureQuotaGroupItem(groupItems, category);
            incrementQuotaItem(categoryItem, project);
            incrementQuotaItem(groupItem, project);
            incrementRatioItem(categoryItem, project);
            incrementRatioItem(groupItem, project);
        }
        applyRuleRatioHints(categoryItems, groupItems, categoryMap, rules);
        overview.setCategoryItems(categoryItems.values().stream()
            .peek(this::finalizeQuotaItem)
            .sorted(Comparator.comparing(ProjectQuotaOverviewVo.Item::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ProjectQuotaOverviewVo.Item::getCategoryName, Comparator.nullsLast(String::compareTo)))
            .toList());
        overview.setGroupItems(groupItems.values().stream()
            .peek(this::finalizeQuotaItem)
            .sorted(Comparator.comparing(ProjectQuotaOverviewVo.Item::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ProjectQuotaOverviewVo.Item::getGroupName, Comparator.nullsLast(String::compareTo)))
            .toList());
        summarizeQuotaOverview(overview);
        return overview;
    }

    private SchoolInfo requireSchoolInfo(Long schoolId) {
        SchoolInfo school = schoolInfoMapper.selectById(schoolId);
        if (school == null) {
            throw new ServiceException("当前单位不存在");
        }
        return school;
    }

    private List<Project> selectQuotaOverviewProjects(Long activityId, Long schoolId, List<Long> categoryIds) {
        if (activityId == null) {
            return List.of();
        }
        return projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
            .select(Project::getActivityId, Project::getSchoolId, Project::getCategoryId, Project::getStatus, Project::getGroupCode, Project::getGroupName)
            .eq(Project::getActivityId, activityId)
            .eq(Project::getSchoolId, schoolId)
            .in(CollUtil.isNotEmpty(categoryIds), Project::getCategoryId, categoryIds)
            .ne(Project::getStatus, ArtReviewConstants.PROJECT_RECYCLED));
    }

    private List<ActivityCategory> selectQuotaOverviewCategories(Long activityId, List<Long> categoryIds) {
        if (activityId == null) {
            if (CollUtil.isEmpty(categoryIds)) {
                return List.of();
            }
            return categoryMapper.selectBatchIds(categoryIds).stream()
                .filter(category -> Boolean.TRUE.equals(category.getEnabled()))
                .sorted(Comparator.comparing(ActivityCategory::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(ActivityCategory::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        }
        return categoryMapper.selectList(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getActivityId, activityId)
            .eq(ActivityCategory::getEnabled, true)
            .in(CollUtil.isNotEmpty(categoryIds), ActivityCategory::getId, categoryIds)
            .orderByAsc(ActivityCategory::getSortOrder)
            .orderByAsc(ActivityCategory::getId));
    }

    private List<ActivityReportRuleRuntime> loadOverviewQuotaRules(Long activityId, SchoolInfo school) {
        String schoolType = school == null ? null : school.getSchoolType();
        return reportRuleRuntimeService.listOverviewRules(activityId, school == null ? null : school.getId(), schoolType);
    }

    private ProjectQuotaOverviewVo.Item toQuotaCategoryItem(ActivityCategory category, List<ActivityReportRuleRuntime> rules) {
        ProjectQuotaOverviewVo.Item item = new ProjectQuotaOverviewVo.Item();
        item.setId(category.getId() == null ? null : String.valueOf(category.getId()));
        item.setCategoryId(category.getId());
        item.setCategoryCode(category.getCategoryCode());
        item.setCategoryName(category.getCategoryName());
        item.setGroupKey(categoryGroupKey(category));
        item.setGroupName(categoryGroupName(category));
        item.setSortOrder(category.getSortOrder());
        item.setQuotaLimit(resolveCategoryQuotaLimit(category, rules));
        return item;
    }

    private ProjectQuotaOverviewVo.Item ensureQuotaGroupItem(Map<String, ProjectQuotaOverviewVo.Item> groupItems, ActivityCategory category) {
        String key = categoryGroupKey(category);
        return groupItems.computeIfAbsent(key, groupKey -> {
            ProjectQuotaOverviewVo.Item item = new ProjectQuotaOverviewVo.Item();
            item.setId(groupKey);
            item.setGroupKey(groupKey);
            item.setGroupName(categoryGroupName(category));
            item.setSortOrder(categoryGroupSortOrder(groupKey, category.getSortOrder()));
            return item;
        });
    }

    private void mergeQuotaLimit(ProjectQuotaOverviewVo.Item item, Integer quotaLimit) {
        if (quotaLimit == null) {
            return;
        }
        item.setQuotaLimit(zeroIfNull(item.getQuotaLimit()) + quotaLimit);
    }

    private int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer resolveCategoryQuotaLimit(ActivityCategory category, List<ActivityReportRuleRuntime> rules) {
        return rules.stream()
            .filter(this::quotaRuleHasCountLimit)
            .filter(rule -> !quotaRuleTargetsProjectGroup(rule))
            .filter(rule -> rule.getCategoryId() != null)
            .filter(rule -> Objects.equals(rule.getCategoryId(), category.getId()))
            .filter(rule -> quotaRuleMatchesCategory(rule, category))
            .map(ActivityReportRuleRuntime::getLimitCount)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(category.getQuotaLimit());
    }

    private void applyGroupQuotaRuleLimits(Map<String, ProjectQuotaOverviewVo.Item> groupItems, List<ActivityReportRuleRuntime> rules) {
        for (ProjectQuotaOverviewVo.Item item : groupItems.values()) {
            rules.stream()
                .filter(this::quotaRuleHasCountLimit)
                .filter(rule -> !quotaRuleTargetsProjectGroup(rule))
                .filter(rule -> rule.getCategoryId() == null)
                .filter(rule -> quotaRuleMatchesGroup(rule, item.getGroupKey(), item.getGroupName()))
                .map(ActivityReportRuleRuntime::getLimitCount)
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(item::setQuotaLimit);
        }
    }

    private void incrementQuotaItem(ProjectQuotaOverviewVo.Item item, Project project) {
        item.setTotalCount(item.getTotalCount() + 1);
        if (ArtReviewConstants.PROJECT_DRAFT.equals(project.getStatus())) {
            item.setDraftCount(item.getDraftCount() + 1);
        }
        if (ArtReviewConstants.PROJECT_SUBMITTED.equals(project.getStatus())) {
            item.setSubmittedCount(item.getSubmittedCount() + 1);
        }
        if (ArtReviewConstants.PROJECT_AUDIT_PASSED.equals(project.getStatus())) {
            item.setCompletedCount(item.getCompletedCount() + 1);
        }
    }

    private void incrementRatioItem(ProjectQuotaOverviewVo.Item item, Project project) {
        if (!isQuotaUsedStatus(project.getStatus())) {
            return;
        }
        String groupCode = StringUtils.trimToEmpty(project.getGroupCode());
        String groupName = StringUtils.trimToEmpty(project.getGroupName());
        if (StringUtils.isBlank(groupCode) && StringUtils.isBlank(groupName)) {
            return;
        }
        ProjectQuotaOverviewVo.RatioItem ratioItem = ensureRatioItem(item, StringUtils.blankToDefault(groupCode, groupName), groupCode, groupName);
        ratioItem.setCount(ratioItem.getCount() + 1);
    }

    private void applyRuleRatioHints(Map<Long, ProjectQuotaOverviewVo.Item> categoryItems,
                                     Map<String, ProjectQuotaOverviewVo.Item> groupItems,
                                     Map<Long, ActivityCategory> categoryMap,
                                     List<ActivityReportRuleRuntime> rules) {
        for (ActivityReportRuleRuntime rule : rules) {
            if (!quotaRuleTargetsProjectGroup(rule)) {
                continue;
            }
            for (ActivityCategory category : categoryMap.values()) {
                if (!quotaRuleMatchesCategory(rule, category)) {
                    continue;
                }
                ProjectQuotaOverviewVo.Item categoryItem = categoryItems.get(category.getId());
                ProjectQuotaOverviewVo.Item groupItem = groupItems.get(categoryGroupKey(category));
                applyRuleHint(categoryItem, rule);
                applyRuleHint(groupItem, rule);
            }
        }
    }

    private void applyRuleHint(ProjectQuotaOverviewVo.Item item, ActivityReportRuleRuntime rule) {
        if (item == null) {
            return;
        }
        String groupCode = StringUtils.trimToEmpty(rule.getTargetValue());
        String groupName = StringUtils.trimToEmpty(rule.getDisplayName());
        if (StringUtils.isBlank(groupCode) && StringUtils.isBlank(groupName)) {
            return;
        }
        ProjectQuotaOverviewVo.RatioItem ratioItem = ensureRatioItem(item, StringUtils.blankToDefault(groupCode, groupName), groupCode, groupName);
        if (quotaRuleHasCountLimit(rule)) {
            ratioItem.setLimitCount(rule.getLimitCount());
        }
        if (rule.getRatioValue() != null) {
            ratioItem.setRatioValue(rule.getRatioValue());
            ratioItem.setOperator(ratioOperator(rule.getRuleJson()));
        }
        ratioItem.setMessage(StringUtils.blankToDefault(rule.displayMessage(), ratioItem.getMessage()));
    }

    private ProjectQuotaOverviewVo.RatioItem ensureRatioItem(ProjectQuotaOverviewVo.Item item, String key, String groupCode, String groupName) {
        String safeKey = StringUtils.blankToDefault(key, "未分组");
        return item.getRatioItems().stream()
            .filter(ratio -> safeKey.equals(ratio.getKey()))
            .findFirst()
            .orElseGet(() -> {
                ProjectQuotaOverviewVo.RatioItem ratioItem = new ProjectQuotaOverviewVo.RatioItem();
                ratioItem.setKey(safeKey);
                ratioItem.setGroupCode(groupCode);
                ratioItem.setGroupName(StringUtils.blankToDefault(groupName, groupCode));
                item.getRatioItems().add(ratioItem);
                return ratioItem;
            });
    }

    private void finalizeQuotaItem(ProjectQuotaOverviewVo.Item item) {
        item.setUsedCount(item.getSubmittedCount() + item.getCompletedCount());
        if (item.getQuotaLimit() != null) {
            item.setRemainingCount(Math.max(0, item.getQuotaLimit() - Math.toIntExact(Math.min(Integer.MAX_VALUE, item.getUsedCount()))));
        }
        long denominator = item.getUsedCount();
        item.getRatioItems().forEach(ratio -> {
            if (denominator <= 0) {
                ratio.setPercent(BigDecimal.ZERO);
            } else {
                ratio.setPercent(BigDecimal.valueOf(ratio.getCount() * 100).divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP));
            }
        });
        item.setRatioItems(item.getRatioItems().stream()
            .sorted(Comparator.comparing(ProjectQuotaOverviewVo.RatioItem::getGroupName, Comparator.nullsLast(String::compareTo)))
            .toList());
    }

    private void summarizeQuotaOverview(ProjectQuotaOverviewVo overview) {
        List<ProjectQuotaOverviewVo.Item> items = overview.getGroupItems().isEmpty() ? overview.getCategoryItems() : overview.getGroupItems();
        overview.setTotalCount(items.stream().mapToLong(ProjectQuotaOverviewVo.Item::getTotalCount).sum());
        overview.setDraftCount(items.stream().mapToLong(ProjectQuotaOverviewVo.Item::getDraftCount).sum());
        overview.setSubmittedCount(items.stream().mapToLong(ProjectQuotaOverviewVo.Item::getSubmittedCount).sum());
        overview.setCompletedCount(items.stream().mapToLong(ProjectQuotaOverviewVo.Item::getCompletedCount).sum());
        overview.setUsedCount(items.stream().mapToLong(ProjectQuotaOverviewVo.Item::getUsedCount).sum());
        List<Integer> quotas = items.stream().map(ProjectQuotaOverviewVo.Item::getQuotaLimit).filter(Objects::nonNull).toList();
        if (!quotas.isEmpty()) {
            int quotaLimit = quotas.stream().mapToInt(Integer::intValue).sum();
            overview.setQuotaLimit(quotaLimit);
            overview.setRemainingCount(Math.max(0, quotaLimit - Math.toIntExact(Math.min(Integer.MAX_VALUE, overview.getUsedCount()))));
        }
    }

    private boolean isQuotaUsedStatus(String status) {
        return ArtReviewConstants.PROJECT_SUBMITTED.equals(status) || ArtReviewConstants.PROJECT_AUDIT_PASSED.equals(status);
    }

    private boolean quotaRuleTargetsProjectGroup(ActivityReportRuleRuntime rule) {
        String targetFieldKey = quotaTargetFieldKey(rule);
        return StringUtils.isNotBlank(rule.getTargetValue())
            || "groupCode".equals(targetFieldKey)
            || "__group_code".equals(targetFieldKey)
            || "groupName".equals(targetFieldKey)
            || "displayGroup".equals(targetFieldKey);
    }

    private boolean quotaRuleMatchesCategory(ActivityReportRuleRuntime rule, ActivityCategory category) {
        if (rule.getCategoryId() != null && !Objects.equals(rule.getCategoryId(), category.getId())) {
            return false;
        }
        Map<String, Object> options = parseQuotaRuleJson(rule.getRuleJson());
        String categoryGroupScope = stringOption(options, "scopeCategoryGroup", "categoryGroupScope");
        if (StringUtils.isNotBlank(categoryGroupScope) && !matchesCategoryGroup(categoryGroupScope, category)) {
            return false;
        }
        String categoryCodeScope = stringOption(options, "scopeCategoryCodes", "categoryCodeScope");
        if (StringUtils.isBlank(categoryCodeScope)) {
            return true;
        }
        return Arrays.stream(categoryCodeScope.split("[,，\\s]+"))
            .filter(StringUtils::isNotBlank)
            .anyMatch(code -> code.equals(category.getCategoryCode()));
    }

    private boolean quotaRuleMatchesGroup(ActivityReportRuleRuntime rule, String groupKey, String groupName) {
        Map<String, Object> options = parseQuotaRuleJson(rule.getRuleJson());
        String categoryGroupScope = stringOption(options, "scopeCategoryGroup", "categoryGroupScope");
        if (StringUtils.isBlank(categoryGroupScope)) {
            return false;
        }
        return Arrays.stream(categoryGroupScope.split("[,，\\s]+"))
            .filter(StringUtils::isNotBlank)
            .anyMatch(scope -> scope.equals(groupKey) || scope.equals(groupName));
    }

    private boolean matchesCategoryGroup(String categoryGroupScope, ActivityCategory category) {
        String groupKey = categoryGroupKey(category);
        String groupName = categoryGroupName(category);
        return Arrays.stream(categoryGroupScope.split("[,，\\s]+"))
            .filter(StringUtils::isNotBlank)
            .anyMatch(scope -> scope.equals(category.getCategoryGroup()) || scope.equals(groupKey) || scope.equals(groupName));
    }

    private String categoryGroupKey(ActivityCategory category) {
        String code = StringUtils.trimToEmpty(category.getCategoryCode()).toLowerCase(Locale.ROOT);
        String group = StringUtils.trimToEmpty(category.getCategoryGroup()).toLowerCase(Locale.ROOT);
        String text = StringUtils.blankToDefault(category.getCategoryGroup(), category.getCategoryName());
        if ("principal".equals(group) || "artwork_principal".equals(code) || text.contains("校长书画")) {
            return "principal";
        }
        if ("performance".equals(group) || code.startsWith("performance_") || text.contains("艺术表演")) {
            return "performance";
        }
        if ("artwork".equals(group) || code.startsWith("artwork_") || text.contains("艺术作品")) {
            return "artwork";
        }
        if ("workshop".equals(group) || code.startsWith("workshop_") || text.contains("工作坊")) {
            return "workshop";
        }
        if ("achievement".equals(group) || code.startsWith("achievement_") || text.contains("美育") || text.contains("成果")) {
            return "achievement";
        }
        return StringUtils.blankToDefault(group, "other");
    }

    private String categoryGroupName(ActivityCategory category) {
        String key = categoryGroupKey(category);
        return switch (key) {
            case "performance" -> "艺术表演类";
            case "artwork" -> "艺术作品类";
            case "workshop" -> "艺术实践工作坊";
            case "achievement" -> "高校美育改革创新优秀成果";
            case "principal" -> "高校校长书画作品";
            default -> StringUtils.blankToDefault(category.getCategoryGroup(), "其他类别");
        };
    }

    private Integer categoryGroupSortOrder(String groupKey, Integer categorySortOrder) {
        return switch (StringUtils.blankToDefault(groupKey, "other")) {
            case "performance" -> 1;
            case "artwork" -> 2;
            case "workshop" -> 3;
            case "achievement" -> 4;
            case "principal" -> 5;
            default -> categorySortOrder == null ? 99 : categorySortOrder;
        };
    }

    @Override
    public void checkProjectViewStatusPermission(Project project) {
        projectScopeGuard.checkProjectViewStatusPermission(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recycleProjectViewProject(Long projectId, String reason) {
        if (projectId == null) {
            throw new ServiceException("项目ID不能为空");
        }
        Project project = requireProjectForUpdate(projectId);
        if (ArtReviewConstants.PROJECT_RECYCLED.equals(project.getStatus())) {
            throw new ServiceException("项目已在回收站");
        }
        checkProjectViewStatusPermission(project);
        auditAssignmentService.checkProjectViewAction(project, "remove");
        project.setRecycledFromStatus(project.getStatus());
        project.setStatus(ArtReviewConstants.PROJECT_RECYCLED);
        project.setRecycledBy(LoginHelper.getUserId());
        project.setRecycledAt(new Date());
        project.setRecycleReason(StringUtils.trim(reason));
        projectMapper.updateById(project);
    }

    @Override
    // ART-REF: BE.PROJECT.DETAIL_ASSEMBLER -> project/service/assembler/ProjectDetailAssembler.java
    public ProjectVo getProjectDetail(Long id) {
        Project project = requireProject(id);
        projectScopeGuard.checkProjectAccess(project);
        ProjectVo vo = projectMapper.selectVoById(id);
        projectDetailAssembler.attachFilesAndAuditRecords(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // ART-REF: BE.PROJECT.DRAFT_SUBMIT -> project/service/submit/ProjectDraftSubmitService.java
    public ProjectVo saveDraft(ProjectSaveBo bo) {
        return projectDraftSubmitService.saveDraft(bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        projectDraftSubmitService.submit(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawSubmit(Long id) {
        projectDraftSubmitService.withdrawSubmit(id);
    }

    @Override
    // ART-REF: BE.PROJECT.MEMBER_TEMPLATE -> project/service/member/ProjectMemberTemplateService.java
    public void exportMemberTemplate(HttpServletResponse response, Long projectId, Long categoryId) {
        ActivityCategory category = resolveMemberTemplateCategory(projectId, categoryId);
        Map<String, Object> rule = parseRuleObject(category == null ? null : category.getRuleJson());
        writeMemberTemplate(response, memberTemplateFields(rule), memberTemplateFooterText(rule));
    }

    private ActivityCategory resolveMemberTemplateCategory(Long projectId, Long categoryId) {
        if (projectId != null) {
            Project project = requireProject(projectId);
            projectScopeGuard.checkProjectAccess(project);
            return categoryMapper.selectById(project.getCategoryId());
        }
        if (categoryId == null) {
            return null;
        }
        ActivityCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new ServiceException("活动类别不存在或已被删除");
        }
        if (projectScopeGuard.isCurrentUserSchool()) {
            requireCategoryAvailable(category.getActivityId(), category.getId());
        }
        return category;
    }

    private void writeMemberTemplate(HttpServletResponse response, List<MemberTemplateField> fields, String footerText) {
        FileUtils.setAttachmentResponseHeader(response, MEMBER_TEMPLATE_FILE_NAME);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(MEMBER_TEMPLATE_SHEET_NAME);
            CellStyle headerStyle = memberTemplateHeaderStyle(workbook);
            CellStyle dataStyle = memberTemplateDataStyle(workbook);
            CellStyle footerStyle = memberTemplateFooterStyle(workbook);

            Row header = sheet.createRow(0);
            header.setHeightInPoints(24);
            for (int index = 0; index < fields.size(); index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(fields.get(index).fieldLabel());
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(index, memberTemplateColumnWidth(fields.get(index)));
            }
            for (int rowIndex = 1; rowIndex <= MEMBER_TEMPLATE_BLANK_DATA_ROWS; rowIndex++) {
                Row row = sheet.createRow(rowIndex);
                row.setHeightInPoints(22);
                for (int columnIndex = 0; columnIndex < fields.size(); columnIndex++) {
                    row.createCell(columnIndex).setCellStyle(dataStyle);
                }
            }
            if (StringUtils.isNotBlank(footerText) && !fields.isEmpty()) {
                int footerRowIndex = MEMBER_TEMPLATE_BLANK_DATA_ROWS + 1;
                Row footer = sheet.createRow(footerRowIndex);
                footer.setHeightInPoints(Math.min(240, Math.max(42, footerText.split("\\R", -1).length * 18)));
                Cell cell = footer.createCell(0);
                cell.setCellValue(memberTemplateFooterCellText(footerText));
                cell.setCellStyle(footerStyle);
                for (int columnIndex = 1; columnIndex < fields.size(); columnIndex++) {
                    footer.createCell(columnIndex).setCellStyle(footerStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(footerRowIndex, footerRowIndex, 0, fields.size() - 1));
            }
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException("导出成员采集表异常", e);
        }
    }

    private CellStyle memberTemplateHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setThinBorder(style);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private String memberTemplateFooterCellText(String footerText) {
        String text = footerText.trim();
        return text.startsWith("说明") ? text : "说明：" + text;
    }

    private CellStyle memberTemplateDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("@"));
        setThinBorder(style);
        return style;
    }

    private CellStyle memberTemplateFooterStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        setThinBorder(style);
        return style;
    }

    private void setThinBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

    private int memberTemplateColumnWidth(MemberTemplateField field) {
        String label = StringUtils.blankToDefault(field.fieldLabel(), field.fieldKey());
        int width = Math.max(10, Math.min(24, label.length() + 6));
        if (Set.of("identityNo", "department", "major", "phone", "roleName").contains(field.fieldKey())) {
            width = Math.max(width, 18);
        }
        return width * 256;
    }

    private List<MemberImportRow> readMemberImportRows(Project project, InputStream inputStream) throws IOException {
        ActivityCategory category = categoryMapper.selectById(project.getCategoryId());
        Map<String, Object> rule = parseRuleObject(category == null ? null : category.getRuleJson());
        Map<String, String> headerKeyMap = memberTemplateHeaderKeyMap(memberTemplateFields(rule));
        Map<String, String> memberTypeAliases = memberTypeAliases(rule);
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return List.of();
            }
            HeaderMapping headerMapping = resolveMemberTemplateHeader(sheet, formatter, headerKeyMap);
            if (headerMapping.columnKeys().isEmpty()) {
                throw new ServiceException("成员采集表缺少表头，请使用系统下载的模板填写");
            }
            List<MemberImportRow> rows = new ArrayList<>();
            for (int rowIndex = headerMapping.headerRowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                MemberImportRow memberRow = readMemberImportRow(row, formatter, headerMapping.columnKeys());
                if (memberRow.isBlank()) {
                    continue;
                }
                if (memberRow.isFooter()) {
                    break;
                }
                String rawMemberType = memberRow.value("memberType");
                if (StringUtils.isNotBlank(rawMemberType)) {
                    String memberType = memberTypeAliases.get(normalizeMemberAttachmentMatch(rawMemberType));
                    if (StringUtils.isBlank(memberType)) {
                        throw new ServiceException("成员采集表中的类型“" + rawMemberType + "”未在当前类别配置中启用");
                    }
                    memberRow.values().put("memberType", memberType);
                }
                rows.add(memberRow);
            }
            return rows;
        }
    }

    private HeaderMapping resolveMemberTemplateHeader(Sheet sheet, DataFormatter formatter, Map<String, String> headerKeyMap) {
        int maxHeaderRow = Math.min(sheet.getLastRowNum(), 5);
        for (int rowIndex = 0; rowIndex <= maxHeaderRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<Integer, String> columnKeys = new LinkedHashMap<>();
            for (int columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++) {
                String header = normalizeMemberTemplateHeader(formatter.formatCellValue(row.getCell(columnIndex)));
                String fieldKey = headerKeyMap.get(header);
                if (StringUtils.isNotBlank(fieldKey)) {
                    columnKeys.put(columnIndex, fieldKey);
                }
            }
            if (columnKeys.containsValue("name")) {
                return new HeaderMapping(rowIndex, columnKeys);
            }
        }
        return new HeaderMapping(0, Map.of());
    }

    private MemberImportRow readMemberImportRow(Row row, DataFormatter formatter, Map<Integer, String> columnKeys) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : columnKeys.entrySet()) {
            Cell cell = row.getCell(entry.getKey());
            String value = formatter.formatCellValue(cell);
            if (value != null) {
                value = value.trim();
            }
            if (StringUtils.isNotBlank(value)) {
                values.put(canonicalMemberFieldKey(entry.getValue()), value);
            }
        }
        return new MemberImportRow(values);
    }

    private Map<String, String> memberTemplateHeaderKeyMap(List<MemberTemplateField> fields) {
        Map<String, String> map = new LinkedHashMap<>(MEMBER_IMPORT_HEADER_ALIASES);
        for (MemberTemplateField field : fields) {
            String fieldKey = canonicalMemberFieldKey(field.fieldKey());
            map.put(normalizeMemberTemplateHeader(field.fieldLabel()), fieldKey);
            map.put(normalizeMemberTemplateHeader(field.fieldKey()), fieldKey);
        }
        return map;
    }

    private String canonicalMemberFieldKey(String fieldKey) {
        return switch (StringUtils.blankToDefault(fieldKey, "")) {
            case "identity_no", "identityNo", "idCard", "id_card" -> "identityNo";
            case "student_no", "studentNo", "workNo", "work_no" -> "studentNo";
            case "major_code", "majorCode" -> "majorCode";
            case "role_name", "roleName" -> "roleName";
            default -> fieldKey;
        };
    }

    private String normalizeMemberTemplateHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\s　/／\\\\|｜·・:：()（）\\[\\]【】]+", "").trim();
    }

    private String memberTemplateFooterText(Map<String, Object> rule) {
        String text = stringValue(rule.get("memberTemplateFooterText")).trim();
        if (StringUtils.isNotBlank(text)) {
            return text;
        }
        Object template = rule.get("memberTemplate");
        if (template instanceof Map<?, ?> templateMap) {
            return stringValue(templateMap.get("footerText")).trim();
        }
        return "";
    }

    private List<MemberTemplateField> memberTemplateFields(Map<String, Object> rule) {
        List<MemberTemplateField> configured = new ArrayList<>();
        Object groups = rule.get("memberFieldGroups");
        if (groups instanceof Map<?, ?> groupMap) {
            configured.addAll(parseMemberTemplateGroup(groupMap.get("participant"), "participant", 100));
            configured.addAll(parseMemberTemplateGroup(groupMap.get("author"), "author", 200));
        }
        if (configured.isEmpty()) {
            configured.addAll(defaultMemberTemplateFields());
        }
        Map<String, MemberTemplateField> unique = new LinkedHashMap<>();
        unique.put("orderNo", new MemberTemplateField("orderNo", "序号", "system", "input", false, 0, 0));
        unique.put("memberType", new MemberTemplateField("memberType", "类型", "system", "input", false, 0, 1));
        configured.stream()
            .filter(field -> !MEMBER_TEMPLATE_EXCLUDED_FIELD_TYPES.contains(StringUtils.blankToDefault(field.fieldType(), "").toLowerCase(Locale.ROOT)))
            .filter(field -> StringUtils.isNotBlank(field.fieldKey()) && StringUtils.isNotBlank(field.fieldLabel()))
            .sorted(Comparator.comparingInt(MemberTemplateField::sortOrder).thenComparingInt(MemberTemplateField::sequence))
            .forEach(field -> unique.merge(field.fieldKey(), field, this::mergeMemberTemplateField));
        return new ArrayList<>(unique.values());
    }

    private Map<String, String> memberTypeAliases(Map<String, Object> rule) {
        Map<String, String> aliases = new LinkedHashMap<>();
        Object configured = rule.get("memberTypeOptions");
        if (!(configured instanceof Iterable<?> rows)) {
            addBuiltinMemberTypeAliases(aliases);
            return aliases;
        }
        boolean hasConfiguredType = false;
        for (Object item : rows) {
            if (!(item instanceof Map<?, ?> row)) {
                continue;
            }
            String code = StringUtils.trimToEmpty(stringValue(row.get("code")));
            String label = StringUtils.trimToEmpty(stringValue(row.get("label")));
            if (!code.matches("[a-z][a-z0-9_]{0,31}")) {
                continue;
            }
            hasConfiguredType = true;
            if (Boolean.FALSE.equals(row.get("enabled"))) {
                continue;
            }
            aliases.put(normalizeMemberAttachmentMatch(code), code);
            if (StringUtils.isNotBlank(label)) {
                aliases.put(normalizeMemberAttachmentMatch(label), code);
            }
        }
        if (!hasConfiguredType) {
            addBuiltinMemberTypeAliases(aliases);
        }
        return aliases;
    }

    private void addBuiltinMemberTypeAliases(Map<String, String> aliases) {
        aliases.put(normalizeMemberAttachmentMatch("学生"), "student");
        aliases.put(normalizeMemberAttachmentMatch("student"), "student");
        aliases.put(normalizeMemberAttachmentMatch("作者"), "author");
        aliases.put(normalizeMemberAttachmentMatch("author"), "author");
        aliases.put(normalizeMemberAttachmentMatch("指导教师"), "teacher");
        aliases.put(normalizeMemberAttachmentMatch("teacher"), "teacher");
        aliases.put(normalizeMemberAttachmentMatch("完成人"), "completer");
        aliases.put(normalizeMemberAttachmentMatch("completer"), "completer");
    }

    private List<MemberTemplateField> parseMemberTemplateGroup(Object value, String group, int sequenceOffset) {
        if (!(value instanceof Iterable<?> schemas)) {
            return List.of();
        }
        List<MemberTemplateField> fields = new ArrayList<>();
        int index = 0;
        for (Object item : schemas) {
            if (!(item instanceof Map<?, ?> schema)) {
                continue;
            }
            String fieldKey = canonicalMemberFieldKey(stringValue(schema.get("fieldKey")));
            String fieldLabel = StringUtils.blankToDefault(stringValue(schema.get("fieldLabel")), fieldKey);
            String fieldType = StringUtils.blankToDefault(stringValue(schema.get("fieldType")), "input");
            boolean required = Boolean.parseBoolean(String.valueOf(schema.get("required")));
            int sortOrder = intValue(schema.get("sortOrder"), index + 1);
            fields.add(new MemberTemplateField(fieldKey, fieldLabel, group, fieldType, required, sortOrder, sequenceOffset + index));
            index++;
        }
        return fields;
    }

    private MemberTemplateField mergeMemberTemplateField(MemberTemplateField existing, MemberTemplateField incoming) {
        return new MemberTemplateField(
            existing.fieldKey(),
            StringUtils.blankToDefault(existing.fieldLabel(), incoming.fieldLabel()),
            existing.group() + "," + incoming.group(),
            StringUtils.blankToDefault(existing.fieldType(), incoming.fieldType()),
            existing.required() || incoming.required(),
            Math.min(existing.sortOrder(), incoming.sortOrder()),
            Math.min(existing.sequence(), incoming.sequence())
        );
    }

    private List<MemberTemplateField> defaultMemberTemplateFields() {
        return List.of(
            new MemberTemplateField("name", "姓名", "participant", "input", true, 1, 1),
            new MemberTemplateField("identityNo", "身份证号/护照号", "participant", "input", false, 2, 2),
            new MemberTemplateField("nation", "民族", "participant", "input", false, 3, 3),
            new MemberTemplateField("gender", "性别", "participant", "input", false, 4, 4),
            new MemberTemplateField("grade", "年级", "participant", "input", false, 5, 5),
            new MemberTemplateField("studentNo", "学号/工作证号", "participant", "input", false, 6, 6),
            new MemberTemplateField("department", "所在院系", "participant", "input", false, 7, 7),
            new MemberTemplateField("majorCode", "专业代码", "participant", "input", false, 8, 8),
            new MemberTemplateField("major", "专业名称", "participant", "input", false, 9, 9),
            new MemberTemplateField("phone", "联系方式", "participant", "input", false, 10, 10),
            new MemberTemplateField("roleName", "备注/身份", "participant", "input", false, 11, 11)
        );
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int intValue(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // ART-REF: BE.PROJECT.MEMBER_IMPORT -> project/service/member/ProjectMemberImportService.java
    public ProjectVo importMembers(Long projectId, MultipartFile file, Boolean replace) {
        Project project = requireProjectForUpdate(projectId);
        artReviewSecurity.checkProjectAccess(project);
        requireEditable(project);
        List<MemberImportRow> rows;
        try {
            rows = readMemberImportRows(project, file.getInputStream());
        } catch (ServiceException e) {
            throw e;
        } catch (IOException e) {
            throw new ServiceException("读取成员采集表失败，请上传正确的 Excel 文件");
        } catch (RuntimeException e) {
            throw new ServiceException("读取成员采集表失败，请上传正确的 Excel 文件");
        }
        if (rows == null || rows.isEmpty()) {
            throw new ServiceException("成员采集表内容为空，请填写后重新上传");
        }
        if (Boolean.TRUE.equals(replace)) {
            projectMemberMapper.delete(Wrappers.lambdaQuery(ProjectMember.class)
                .eq(ProjectMember::getProjectId, projectId));
        }
        int index = nextMemberSortOrder(projectId);
        for (MemberImportRow row : rows) {
            if (StringUtils.isBlank(row.value("name")) && StringUtils.isBlank(row.value("studentNo"))) {
                continue;
            }
            ProjectMember member = findExistingMember(projectId, row);
            if (member == null) {
                member = new ProjectMember();
                member.setProjectId(projectId);
                member.setActivityId(project.getActivityId());
                member.setSchoolId(project.getSchoolId());
                member.setSortOrder(index++);
            }
            applyImportedMember(member, row);
            if (member.getId() == null) {
                projectMemberMapper.insert(member);
            } else {
                projectMemberMapper.updateById(member);
            }
        }
        return getProjectDetail(projectId);
    }

    @Override
    // ART-REF: BE.PROJECT.GENERIC_TABLE_TEMPLATE -> project/service/table/ProjectGenericTableService.java
    public void exportGenericTableTemplate(HttpServletResponse response, Long projectId, Long categoryId, String tableKey) {
        ActivityCategory category = resolveMemberTemplateCategory(projectId, categoryId);
        GenericTableTemplateSpec template = requireGenericTableTemplate(category, tableKey);
        writeGenericTableTemplate(response, template);
    }

    @Override
    // ART-REF: BE.PROJECT.GENERIC_TABLE_IMPORT -> project/service/table/ProjectGenericTableService.java
    public GenericTableImportResultVo importGenericTable(Long projectId, String tableKey, MultipartFile file) {
        Project project = requireProjectForUpdate(projectId);
        artReviewSecurity.checkProjectAccess(project);
        requireEditable(project);
        validateGenericTableImportFile(file);
        ActivityCategory category = categoryMapper.selectById(project.getCategoryId());
        GenericTableTemplateSpec template = requireGenericTableTemplate(category, tableKey);
        List<Map<String, Object>> rows;
        try (InputStream inputStream = file.getInputStream()) {
            rows = readGenericTableRows(inputStream, template);
        } catch (ServiceException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new ServiceException("读取表格失败，请上传系统下载的 Excel 模板");
        }
        if (rows.isEmpty()) {
            throw new ServiceException("表格内容为空，请填写后重新上传");
        }
        GenericTableImportResultVo result = new GenericTableImportResultVo();
        result.setTableKey(template.templateKey());
        result.setRows(rows);
        result.setImportedCount(rows.size());
        return result;
    }

    private GenericTableTemplateSpec requireGenericTableTemplate(ActivityCategory category, String tableKey) {
        if (category == null) {
            throw new ServiceException("活动类别不存在或已被删除");
        }
        String normalizedKey = StringUtils.trim(tableKey);
        Map<String, Object> rule = parseRuleObject(category.getRuleJson());
        Object templatesValue = rule.get("genericTableTemplates");
        if (templatesValue instanceof Iterable<?> templates) {
            int templateIndex = 0;
            for (Object value : templates) {
                if (!(value instanceof Map<?, ?> template)) {
                    continue;
                }
                if (!normalizedKey.equals(StringUtils.trim(stringValue(template.get("templateKey"))))) {
                    templateIndex++;
                    continue;
                }
                if (Boolean.FALSE.equals(template.get("enabled"))) {
                    throw new ServiceException("该表格模板已停用");
                }
                List<GenericTableColumnSpec> fields = parseGenericTableColumns(template.get("fields"));
                if (fields.isEmpty()) {
                    throw new ServiceException("表格模板未配置字段");
                }
                String templateName = StringUtils.blankToDefault(stringValue(template.get("templateName")), normalizedKey);
                return new GenericTableTemplateSpec(
                    normalizedKey,
                    templateName,
                    stringValue(template.get("tipText")),
                    Math.max(0, intValue(template.get("minRows"), 0)),
                    genericTableRowLimit(template.get("maxRows")),
                    stringValue(template.get("excelTitle")),
                    genericTableFileName(template.get("fileName"), templateName),
                    genericTableSheetName(template.get("sheetName"), templateName),
                    Math.min(GENERIC_TABLE_MAX_ROWS, Math.max(0, intValue(template.get("blankRows"), 20))),
                    stringValue(template.get("footerText")),
                    intValue(template.get("sortOrder"), templateIndex + 1),
                    fields
                );
            }
        }
        throw new ServiceException("未找到对应的通用表格模板");
    }

    private List<GenericTableColumnSpec> parseGenericTableColumns(Object fieldsValue) {
        if (!(fieldsValue instanceof Iterable<?> fields)) {
            return List.of();
        }
        List<GenericTableColumnSpec> result = new ArrayList<>();
        int index = 0;
        for (Object value : fields) {
            if (!(value instanceof Map<?, ?> field) || result.size() >= 30) {
                continue;
            }
            String fieldKey = StringUtils.trim(stringValue(field.get("fieldKey")));
            String fieldLabel = StringUtils.trim(stringValue(field.get("fieldLabel")));
            if (StringUtils.isBlank(fieldKey) || StringUtils.isBlank(fieldLabel)) {
                continue;
            }
            String fieldType = StringUtils.blankToDefault(stringValue(field.get("fieldType")), "input").toLowerCase(Locale.ROOT);
            Map<String, Object> validation = parseRuleObject(stringValue(field.get("validationJson")));
            List<String> options = parseGenericTableOptions(field.get("optionsJson"));
            result.add(new GenericTableColumnSpec(
                fieldKey,
                fieldLabel,
                fieldType,
                Boolean.TRUE.equals(field.get("required")),
                StringUtils.blankToDefault(stringValue(validation.get("type")), ""),
                options,
                intValue(field.get("sortOrder"), index + 1),
                Math.min(600, Math.max(100, intValue(field.get("width"), 160)))
            ));
            index++;
        }
        return result.stream()
            .sorted(Comparator.comparingInt(GenericTableColumnSpec::sortOrder))
            .toList();
    }

    private List<String> parseGenericTableOptions(Object value) {
        if (value instanceof Iterable<?> iterable) {
            List<String> values = new ArrayList<>();
            iterable.forEach(item -> {
                if (item != null && StringUtils.isNotBlank(String.valueOf(item))) {
                    values.add(String.valueOf(item).trim());
                }
            });
            return values.stream().distinct().toList();
        }
        String text = stringValue(value).trim();
        if (StringUtils.isBlank(text)) {
            return List.of();
        }
        try {
            List<String> values = JsonUtils.parseObject(text, new TypeReference<List<String>>() {
            });
            return values == null ? List.of() : values.stream().filter(StringUtils::isNotBlank).map(String::trim).distinct().toList();
        } catch (RuntimeException e) {
            return Arrays.stream(text.split("[,，、;；\\n]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        }
    }

    private int genericTableRowLimit(Object value) {
        int configured = intValue(value, 100);
        return configured <= 0 ? GENERIC_TABLE_MAX_ROWS : Math.min(GENERIC_TABLE_MAX_ROWS, configured);
    }

    private String genericTableFileName(Object configured, String templateName) {
        String fileName = StringUtils.blankToDefault(stringValue(configured), templateName + ".xlsx")
            .replaceAll("[\\\\/:*?\"<>|]", "_")
            .trim();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            fileName = fileName.replaceAll("(?i)\\.xls$", "") + ".xlsx";
        }
        return StringUtils.blankToDefault(fileName, "通用表格.xlsx");
    }

    private String genericTableSheetName(Object configured, String templateName) {
        String sheetName = StringUtils.blankToDefault(stringValue(configured), templateName);
        sheetName = WorkbookUtil.createSafeSheetName(sheetName);
        if (sheetName.length() > 31) {
            sheetName = sheetName.substring(0, 31);
        }
        return StringUtils.blankToDefault(sheetName, "数据采集表");
    }

    private void writeGenericTableTemplate(HttpServletResponse response, GenericTableTemplateSpec template) {
        FileUtils.setAttachmentResponseHeader(response, template.fileName());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(template.sheetName());
            CellStyle headerStyle = memberTemplateHeaderStyle(workbook);
            CellStyle dataStyle = memberTemplateDataStyle(workbook);
            CellStyle footerStyle = memberTemplateFooterStyle(workbook);
            int headerRowIndex = 0;
            if (StringUtils.isNotBlank(template.excelTitle())) {
                Row title = sheet.createRow(0);
                title.setHeightInPoints(30);
                Cell titleCell = title.createCell(0);
                titleCell.setCellValue(template.excelTitle().trim());
                titleCell.setCellStyle(headerStyle);
                for (int columnIndex = 1; columnIndex < template.fields().size(); columnIndex++) {
                    title.createCell(columnIndex).setCellStyle(headerStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, template.fields().size() - 1));
                headerRowIndex = 1;
            }
            Row header = sheet.createRow(headerRowIndex);
            header.setHeightInPoints(24);
            for (int columnIndex = 0; columnIndex < template.fields().size(); columnIndex++) {
                GenericTableColumnSpec field = template.fields().get(columnIndex);
                Cell cell = header.createCell(columnIndex);
                cell.setCellValue(field.fieldLabel());
                cell.setCellStyle(headerStyle);
                int characterWidth = Math.max(10, Math.min(80, field.width() / 7));
                sheet.setColumnWidth(columnIndex, characterWidth * 256);
            }
            for (int offset = 1; offset <= template.blankRows(); offset++) {
                Row row = sheet.createRow(headerRowIndex + offset);
                row.setHeightInPoints(22);
                for (int columnIndex = 0; columnIndex < template.fields().size(); columnIndex++) {
                    row.createCell(columnIndex).setCellStyle(dataStyle);
                }
            }
            if (StringUtils.isNotBlank(template.footerText())) {
                int footerRowIndex = headerRowIndex + template.blankRows() + 1;
                Row footer = sheet.createRow(footerRowIndex);
                footer.setHeightInPoints(Math.min(240, Math.max(42, template.footerText().split("\\R", -1).length * 18)));
                Cell footerCell = footer.createCell(0);
                footerCell.setCellValue(memberTemplateFooterCellText(template.footerText()));
                footerCell.setCellStyle(footerStyle);
                for (int columnIndex = 1; columnIndex < template.fields().size(); columnIndex++) {
                    footer.createCell(columnIndex).setCellStyle(footerStyle);
                }
                sheet.addMergedRegion(new CellRangeAddress(footerRowIndex, footerRowIndex, 0, template.fields().size() - 1));
            }
            sheet.createFreezePane(0, headerRowIndex + 1);
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException("导出通用表格模板异常", e);
        }
    }

    private void validateGenericTableImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("Excel 文件不能为空");
        }
        if (file.getSize() > GENERIC_TABLE_IMPORT_MAX_SIZE) {
            throw new ServiceException("Excel 文件不能超过 5MB");
        }
        if (!Set.of("xls", "xlsx").contains(normalizeExt(file.getOriginalFilename()))) {
            throw new ServiceException("请上传 xls 或 xlsx 格式文件");
        }
    }

    private List<Map<String, Object>> readGenericTableRows(InputStream inputStream, GenericTableTemplateSpec template) throws IOException {
        DataFormatter formatter = new DataFormatter(Locale.CHINA);
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheet(template.sheetName());
            if (sheet == null && workbook.getNumberOfSheets() > 0) {
                sheet = workbook.getSheetAt(0);
            }
            if (sheet == null) {
                return List.of();
            }
            HeaderMapping header = resolveGenericTableHeader(sheet, formatter, template);
            if (header.columnKeys().isEmpty()) {
                throw new ServiceException("未识别到表头，请使用系统下载的模板填写");
            }
            Set<String> mappedKeys = Set.copyOf(header.columnKeys().values());
            List<String> missingRequiredHeaders = template.fields().stream()
                .filter(GenericTableColumnSpec::required)
                .filter(field -> !mappedKeys.contains(field.fieldKey()))
                .map(GenericTableColumnSpec::fieldLabel)
                .toList();
            if (!missingRequiredHeaders.isEmpty()) {
                throw new ServiceException("缺少必填列：" + String.join("、", missingRequiredHeaders));
            }
            Map<String, GenericTableColumnSpec> fieldMap = template.fields().stream()
                .collect(Collectors.toMap(GenericTableColumnSpec::fieldKey, field -> field, (left, right) -> left, LinkedHashMap::new));
            List<Map<String, Object>> rows = new ArrayList<>();
            for (int rowIndex = header.headerRowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                if (isGenericTableFooter(row, formatter)) {
                    break;
                }
                Map<String, Object> values = new LinkedHashMap<>();
                boolean hasValue = false;
                for (Map.Entry<Integer, String> entry : header.columnKeys().entrySet()) {
                    GenericTableColumnSpec field = fieldMap.get(entry.getValue());
                    if (field == null) {
                        continue;
                    }
                    String text = StringUtils.trim(formatter.formatCellValue(row.getCell(entry.getKey())));
                    if (StringUtils.isBlank(text)) {
                        continue;
                    }
                    hasValue = true;
                    values.put(field.fieldKey(), parseGenericTableCellValue(template, field, text, rowIndex + 1));
                }
                if (!hasValue) {
                    continue;
                }
                validateGenericTableRequiredCells(template, values, rowIndex + 1);
                rows.add(values);
                if (rows.size() > template.maxRows()) {
                    throw new ServiceException("“" + template.templateName() + "”最多导入 " + template.maxRows() + " 行");
                }
            }
            return rows;
        }
    }

    private HeaderMapping resolveGenericTableHeader(Sheet sheet, DataFormatter formatter, GenericTableTemplateSpec template) {
        Map<String, String> headerKeys = new LinkedHashMap<>();
        template.fields().forEach(field -> {
            headerKeys.put(normalizeMemberTemplateHeader(field.fieldLabel()), field.fieldKey());
            headerKeys.put(normalizeMemberTemplateHeader(field.fieldKey()), field.fieldKey());
        });
        int maxHeaderRow = Math.min(sheet.getLastRowNum(), 5);
        for (int rowIndex = 0; rowIndex <= maxHeaderRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<Integer, String> columnKeys = new LinkedHashMap<>();
            for (int columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++) {
                String key = headerKeys.get(normalizeMemberTemplateHeader(formatter.formatCellValue(row.getCell(columnIndex))));
                if (StringUtils.isNotBlank(key)) {
                    columnKeys.put(columnIndex, key);
                }
            }
            if (!columnKeys.isEmpty()) {
                return new HeaderMapping(rowIndex, columnKeys);
            }
        }
        return new HeaderMapping(0, Map.of());
    }

    private Object parseGenericTableCellValue(GenericTableTemplateSpec template, GenericTableColumnSpec field, String text, int rowNumber) {
        String value = text.trim();
        if ("number".equals(field.fieldType())) {
            try {
                BigDecimal number = new BigDecimal(value.replace(",", ""));
                validateGenericTableNumber(field, number, template, rowNumber);
                return number.stripTrailingZeros();
            } catch (NumberFormatException e) {
                throw new ServiceException(genericTableCellLabel(template, field, rowNumber) + "须为数字");
            }
        }
        if ("checkbox".equals(field.fieldType())) {
            List<String> values = Arrays.stream(value.split("[,，、;；]+"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
            validateGenericTableOptions(template, field, values, rowNumber);
            return values;
        }
        validateGenericTableOptions(template, field, List.of(value), rowNumber);
        validateGenericTableText(field, value, template, rowNumber);
        return value;
    }

    private void validateGenericTableRequiredCells(GenericTableTemplateSpec template, Map<String, Object> values, int rowNumber) {
        template.fields().stream()
            .filter(GenericTableColumnSpec::required)
            .filter(field -> values.get(field.fieldKey()) == null || StringUtils.isBlank(String.valueOf(values.get(field.fieldKey()))))
            .findFirst()
            .ifPresent(field -> {
                throw new ServiceException(genericTableCellLabel(template, field, rowNumber) + "不能为空");
            });
    }

    private void validateGenericTableOptions(GenericTableTemplateSpec template, GenericTableColumnSpec field, List<String> values, int rowNumber) {
        if (field.options().isEmpty()) {
            return;
        }
        String invalid = values.stream().filter(value -> !field.options().contains(value)).findFirst().orElse(null);
        if (invalid != null) {
            throw new ServiceException(genericTableCellLabel(template, field, rowNumber) + "不在可选范围内");
        }
    }

    private void validateGenericTableNumber(GenericTableColumnSpec field, BigDecimal number, GenericTableTemplateSpec template, int rowNumber) {
        if ("integer".equals(field.validationType()) && number.stripTrailingZeros().scale() > 0) {
            throw new ServiceException(genericTableCellLabel(template, field, rowNumber) + "须为整数");
        }
        if ("nonnegative".equals(field.validationType()) && number.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException(genericTableCellLabel(template, field, rowNumber) + "不能小于 0");
        }
    }

    private void validateGenericTableText(GenericTableColumnSpec field, String value, GenericTableTemplateSpec template, int rowNumber) {
        boolean valid = switch (field.validationType()) {
            case "phone" -> value.matches("^1\\d{10}$");
            case "email" -> value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
            case "idcard", "id_card" -> value.matches("(^\\d{15}$)|(^\\d{17}[0-9Xx]$)");
            default -> true;
        };
        if (!valid) {
            throw new ServiceException(genericTableCellLabel(template, field, rowNumber) + "格式不正确");
        }
    }

    private String genericTableCellLabel(GenericTableTemplateSpec template, GenericTableColumnSpec field, int rowNumber) {
        return "“" + template.templateName() + "”第 " + rowNumber + " 行“" + field.fieldLabel() + "”";
    }

    private boolean isGenericTableFooter(Row row, DataFormatter formatter) {
        for (int columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++) {
            String value = StringUtils.trim(formatter.formatCellValue(row.getCell(columnIndex)));
            if (StringUtils.isBlank(value)) {
                continue;
            }
            return value.startsWith("说明") || value.startsWith("备注说明");
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVo uploadMemberPhoto(Long projectId, Long memberId, MultipartFile file) {
        ProjectMember member = requireEditableMember(projectId, memberId);
        validateMemberUploadFile(file, MEMBER_PHOTO_EXTS, MEMBER_PHOTO_MAX_SIZE, "证件照");
        SysOssVo oss = ossService.upload(file);
        member.setPhotoOssId(oss.getOssId());
        member.setPhotoPath(oss.getUrl());
        projectMemberMapper.updateById(member);
        return getProjectDetail(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVo uploadMemberStudentReport(Long projectId, Long memberId, MultipartFile file) {
        ProjectMember member = requireEditableMember(projectId, memberId);
        validateMemberUploadFile(file, MEMBER_REPORT_EXTS, MEMBER_REPORT_MAX_SIZE, "学籍在线验证报告");
        SysOssVo oss = ossService.upload(file);
        member.setStudentReportOssId(oss.getOssId());
        member.setStudentReportPath(oss.getUrl());
        projectMemberMapper.updateById(member);
        return getProjectDetail(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectVo uploadMemberAttachment(Long projectId, Long memberId, String fieldKey, MultipartFile file) {
        Project project = requireProjectForUpdate(projectId);
        artReviewSecurity.checkProjectAccess(project);
        requireEditable(project);
        ProjectMember member = projectMemberMapper.selectById(memberId);
        if (member == null || !Objects.equals(member.getProjectId(), projectId)) {
            throw new ServiceException("项目成员不存在");
        }
        MemberAttachmentField field = resolveMemberAttachmentField(project, fieldKey);
        validateMemberUploadFile(file, field.allowedExts(), field.maxSize(), field.label());
        SysOssVo oss = ossService.upload(file);
        applyMemberAttachment(member, field, oss, file.getOriginalFilename());
        projectMemberMapper.updateById(member);
        return getProjectDetail(projectId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberAttachmentBatchResultVo batchUploadMemberAttachment(Long projectId, String fieldKey, List<MultipartFile> files) {
        Project project = requireProjectForUpdate(projectId);
        artReviewSecurity.checkProjectAccess(project);
        requireEditable(project);
        if (files == null || files.isEmpty()) {
            throw new ServiceException("请选择需要批量上传的文件");
        }
        if (files.size() > 100) {
            throw new ServiceException("一次最多批量上传100个成员附件");
        }
        MemberAttachmentField field = resolveMemberAttachmentField(project, fieldKey);
        MemberAttachmentBatchResultVo result = new MemberAttachmentBatchResultVo();
        result.setFieldKey(field.fieldKey());
        Map<String, List<ProjectMember>> membersByMatchValue = projectMemberMapper.selectList(Wrappers.lambdaQuery(ProjectMember.class)
                .eq(ProjectMember::getProjectId, projectId)
                .orderByAsc(ProjectMember::getSortOrder))
            .stream()
            .collect(Collectors.groupingBy(member -> memberAttachmentMatchKey(memberAttachmentMatchValue(member, field.matchFieldKey())), LinkedHashMap::new, Collectors.toList()));
        List<MemberAttachmentBatchPlan> plans = new ArrayList<>();
        Set<Long> plannedMemberIds = new java.util.HashSet<>();
        for (MultipartFile file : files) {
            String fileName = StringUtils.blankToDefault(file == null ? null : file.getOriginalFilename(), "未命名文件");
            String validationMessage = memberUploadValidationMessage(file, field.allowedExts(), field.maxSize(), field.label());
            if (StringUtils.isNotBlank(validationMessage)) {
                addMemberAttachmentBatchItem(result, fileName, null, null, "skipped", validationMessage);
                continue;
            }
            String matchedValue = memberAttachmentMatchKey(fileNameWithoutExtension(fileName));
            if (StringUtils.isBlank(matchedValue)) {
                addMemberAttachmentBatchItem(result, fileName, null, null, "skipped", "文件名不能为空");
                continue;
            }
            List<ProjectMember> matchedMembers = membersByMatchValue.getOrDefault(matchedValue, List.of());
            if (matchedMembers.isEmpty()) {
                addMemberAttachmentBatchItem(result, fileName, null, fileNameWithoutExtension(fileName), "skipped", "未找到匹配的成员");
                continue;
            }
            if (matchedMembers.size() > 1) {
                addMemberAttachmentBatchItem(result, fileName, null, fileNameWithoutExtension(fileName), "skipped", "匹配字段存在重复值，无法自动绑定");
                continue;
            }
            ProjectMember member = matchedMembers.get(0);
            if (memberHasAttachment(member, field)) {
                addMemberAttachmentBatchItem(result, fileName, member.getId(), fileNameWithoutExtension(fileName), "skipped", "该成员已有附件，未覆盖");
                continue;
            }
            if (!plannedMemberIds.add(member.getId())) {
                addMemberAttachmentBatchItem(result, fileName, member.getId(), fileNameWithoutExtension(fileName), "skipped", "多个文件匹配到同一成员，未自动覆盖");
                continue;
            }
            plans.add(new MemberAttachmentBatchPlan(file, member, fileNameWithoutExtension(fileName)));
        }
        for (MemberAttachmentBatchPlan plan : plans) {
            SysOssVo oss = ossService.upload(plan.file());
            applyMemberAttachment(plan.member(), field, oss, plan.file().getOriginalFilename());
            projectMemberMapper.updateById(plan.member());
            addMemberAttachmentBatchItem(result, plan.file().getOriginalFilename(), plan.member().getId(), plan.matchedValue(), "success", "已匹配并上传");
            result.setMatchedCount(result.getMatchedCount() + 1);
        }
        result.setSkippedCount((int) result.getItems().stream().filter(item -> !"success".equals(item.getStatus())).count());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // ART-REF: BE.PROJECT.FILE_UPLOAD -> project/service/file/ProjectFileService.java
    public ProjectFileVo uploadFile(Long projectId, Long requirementId, MultipartFile file) {
        return projectFileService.uploadFile(projectId, requirementId, file);
    }

    @Override
    public ProjectFileDirectUploadInitVo initDirectUpload(Long projectId, Long requirementId, ProjectFileDirectUploadInitBo bo) {
        return projectFileService.initDirectUpload(projectId, requirementId, bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectFileVo completeDirectUpload(Long projectId, Long requirementId, ProjectFileDirectUploadCompleteBo bo) {
        return projectFileService.completeDirectUpload(projectId, requirementId, bo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProjectFileVo requestFilePreview(Long projectId, Long fileId) {
        return projectFileService.requestFilePreview(projectId, fileId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long projectId, Long fileId) {
        projectFileService.deleteFile(projectId, fileId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    // ART-REF: BE.PROJECT.RECYCLE -> project/service/recycle/ProjectRecycleService.java
    public void recycleProject(Long projectId, String reason) {
        if (projectId == null) {
            throw new ServiceException("项目ID不能为空");
        }
        Project project = requireProjectForUpdate(projectId);
        projectScopeGuard.checkProjectAccess(project);
        requireEditable(project);
        project.setRecycledFromStatus(project.getStatus());
        project.setStatus(ArtReviewConstants.PROJECT_RECYCLED);
        project.setRecycledBy(LoginHelper.getUserId());
        project.setRecycledAt(new Date());
        project.setRecycleReason(StringUtils.trim(reason));
        projectMapper.updateById(project);
    }

    @Override
    public TableDataInfo<ProjectVo> queryRecycledProjectPage(ProjectVo query, PageQuery pageQuery) {
        LambdaQueryWrapper<Project> lqw = buildProjectQuery(query);
        lqw.eq(Project::getStatus, ArtReviewConstants.PROJECT_RECYCLED);
        projectScopeGuard.applySchoolScopeIfNeeded(lqw);
        Page<ProjectVo> page = projectMapper.selectVoPage(pageQuery.build(), lqw);
        fillRecycledByName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreRecycledProject(Long projectId) {
        Project project = requireProjectForUpdate(projectId);
        projectScopeGuard.checkProjectAccess(project);
        if (!ArtReviewConstants.PROJECT_RECYCLED.equals(project.getStatus())) {
            throw new ServiceException("只有回收站中的项目可以恢复");
        }
        String restoreStatus = StringUtils.blankToDefault(project.getRecycledFromStatus(), ArtReviewConstants.PROJECT_DRAFT);
        if (!Set.of(
            ArtReviewConstants.PROJECT_DRAFT,
            ArtReviewConstants.PROJECT_SUBMITTED,
            ArtReviewConstants.PROJECT_RETURNED,
            ArtReviewConstants.PROJECT_AUDIT_PASSED
        ).contains(restoreStatus)) {
            restoreStatus = ArtReviewConstants.PROJECT_DRAFT;
        }
        project.setStatus(restoreStatus);
        project.setRecycledFromStatus(null);
        project.setRecycledBy(null);
        project.setRecycledAt(null);
        project.setRecycleReason(null);
        projectMapper.updateById(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purgeRecycledProjects(Long[] projectIds) {
        if (projectIds == null || projectIds.length == 0) {
            throw new ServiceException("项目ID不能为空");
        }
        for (Long projectId : projectIds) {
            Project project = projectMapper.selectById(projectId);
            if (project == null) {
                continue;
            }
            if (!ArtReviewConstants.PROJECT_RECYCLED.equals(project.getStatus())) {
                throw new ServiceException("只有回收站中的项目可以永久清理");
            }
            projectScopeGuard.checkProjectAccess(project);
            ensureProjectCanBePurged(projectId);
            projectFileMapper.delete(Wrappers.lambdaQuery(ProjectFile.class).eq(ProjectFile::getProjectId, projectId));
            projectMemberMapper.delete(Wrappers.lambdaQuery(ProjectMember.class).eq(ProjectMember::getProjectId, projectId));
            auditRecordMapper.delete(Wrappers.lambdaQuery(org.dromara.crehn.domain.ProjectAuditRecord.class)
                .eq(org.dromara.crehn.domain.ProjectAuditRecord::getProjectId, projectId));
            projectMapper.deleteById(projectId);
        }
    }

    @Override
    public TableDataInfo<ProjectFileVo> queryDeletedFilePage(ProjectFileVo query, PageQuery pageQuery) {
        LambdaQueryWrapper<ProjectFile> lqw = Wrappers.lambdaQuery(ProjectFile.class)
            .eq(ProjectFile::getStatus, ArtReviewConstants.FILE_DELETED)
            .eq(query.getProjectId() != null, ProjectFile::getProjectId, query.getProjectId())
            .eq(query.getRequirementId() != null, ProjectFile::getRequirementId, query.getRequirementId())
            .eq(StringUtils.isNotBlank(query.getMediaType()), ProjectFile::getMediaType, query.getMediaType())
            .like(StringUtils.isNotBlank(query.getOriginalName()), ProjectFile::getOriginalName, query.getOriginalName())
            .orderByDesc(ProjectFile::getUpdateTime)
            .orderByDesc(ProjectFile::getUploadedAt);
        projectScopeGuard.applySchoolScopeToFileQueryIfNeeded(lqw);
        Page<ProjectFileVo> page = projectFileMapper.selectVoPage(pageQuery.build(), lqw);
        fillDeletedByName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restoreDeletedFile(Long fileId) {
        ProjectFile file = projectFileMapper.selectById(fileId);
        if (file == null || !ArtReviewConstants.FILE_DELETED.equals(file.getStatus())) {
            throw new ServiceException("已删除文件不存在");
        }
        Project project = requireProject(file.getProjectId());
        projectScopeGuard.checkProjectAccess(project);
        file.setStatus(ArtReviewConstants.FILE_ACTIVE);
        projectFileMapper.updateById(file);
    }

    @Override
    public void downloadDeletedFile(Long fileId, HttpServletResponse response) throws IOException {
        ProjectFile file = projectFileMapper.selectById(fileId);
        if (file == null || !ArtReviewConstants.FILE_DELETED.equals(file.getStatus())) {
            throw new ServiceException("已删除文件不存在");
        }
        Project project = requireProject(file.getProjectId());
        projectScopeGuard.checkProjectAccess(project);
        if (file.getOssId() == null) {
            throw new ServiceException("已删除文件没有对应的 OSS 对象");
        }
        ossService.download(file.getOssId(), response);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purgeDeletedFiles(Long[] fileIds) {
        if (fileIds == null || fileIds.length == 0) {
            throw new ServiceException("文件ID不能为空");
        }
        for (Long fileId : fileIds) {
            ProjectFile file = projectFileMapper.selectById(fileId);
            if (file == null) {
                continue;
            }
            if (!ArtReviewConstants.FILE_DELETED.equals(file.getStatus())) {
                throw new ServiceException("只有已删除文件可以永久清理");
            }
            Project project = requireProject(file.getProjectId());
            projectScopeGuard.checkProjectAccess(project);
            projectFileMapper.deleteById(fileId);
        }
    }

    private void fillDeletedByName(List<ProjectFileVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> userIds = rows.stream()
            .map(ProjectFileVo::getUpdateBy)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> userNameMap = sysUserMapper.selectBatchIds(userIds).stream()
            .collect(java.util.stream.Collectors.toMap(
                SysUser::getUserId,
                user -> StringUtils.blankToDefault(user.getUserName(), String.valueOf(user.getUserId())),
                (left, right) -> left
            ));
        rows.forEach(row -> row.setDeletedByName(userNameMap.get(row.getUpdateBy())));
    }

    private void fillRecycledByName(List<ProjectVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> userIds = rows.stream()
            .map(ProjectVo::getRecycledBy)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, String> userNameMap = sysUserMapper.selectBatchIds(userIds).stream()
            .collect(java.util.stream.Collectors.toMap(
                SysUser::getUserId,
                user -> StringUtils.blankToDefault(user.getUserName(), String.valueOf(user.getUserId())),
                (left, right) -> left
            ));
        rows.forEach(row -> row.setRecycledByName(userNameMap.get(row.getRecycledBy())));
    }

    private void ensureProjectCanBePurged(Long projectId) {
        long scoreCount = reviewScoreMapper.selectCount(Wrappers.lambdaQuery(ReviewScore.class)
            .eq(ReviewScore::getProjectId, projectId));
        if (scoreCount > 0) {
            throw new ServiceException("项目已有评审评分，不能永久清理");
        }
        long resultCount = reviewResultMapper.selectCount(Wrappers.lambdaQuery(ReviewResult.class)
            .eq(ReviewResult::getProjectId, projectId));
        if (resultCount > 0) {
            throw new ServiceException("项目已有评审结果，不能永久清理");
        }
    }

    private int nextMemberSortOrder(Long projectId) {
        List<ProjectMember> members = projectMemberMapper.selectList(Wrappers.lambdaQuery(ProjectMember.class)
            .eq(ProjectMember::getProjectId, projectId)
            .orderByDesc(ProjectMember::getSortOrder)
            .last("limit 1"));
        if (members.isEmpty() || members.get(0).getSortOrder() == null) {
            return 1;
        }
        return members.get(0).getSortOrder() + 1;
    }

    private ProjectMember findExistingMember(Long projectId, MemberImportRow row) {
        LambdaQueryWrapper<ProjectMember> wrapper = Wrappers.lambdaQuery(ProjectMember.class)
            .eq(ProjectMember::getProjectId, projectId);
        if (StringUtils.isNotBlank(row.value("studentNo"))) {
            wrapper.eq(ProjectMember::getStudentNo, row.value("studentNo"));
        } else {
            wrapper.eq(ProjectMember::getName, row.value("name"));
        }
        return projectMemberMapper.selectOne(wrapper.last("limit 1"));
    }

    private void applyImportedMember(ProjectMember member, MemberImportRow row) {
        member.setMemberType(resolveImportedMemberType(row));
        member.setName(row.value("name"));
        member.setStudentNo(row.value("studentNo"));
        member.setDepartment(row.value("department"));
        member.setMajor(row.value("major"));
        member.setRoleName(row.value("roleName"));
        member.setExtraJson(JsonUtils.toJsonString(importedMemberExtra(row)));
        member.setStatus(ArtReviewConstants.FILE_ACTIVE);
    }

    private String resolveImportedMemberType(MemberImportRow row) {
        String explicitType = StringUtils.trimToEmpty(row.value("memberType"));
        if (StringUtils.isNotBlank(explicitType)) {
            return explicitType;
        }
        String grade = StringUtils.blankToDefault(row.value("grade"), "").trim();
        String roleName = StringUtils.blankToDefault(row.value("roleName"), "");
        String roleNameLower = roleName.toLowerCase(Locale.ROOT);
        if ("7".equals(grade) || grade.contains("教师") || grade.toLowerCase(Locale.ROOT).contains("teacher")
            || roleName.contains("教师") || roleName.contains("老师") || roleNameLower.contains("teacher")) {
            return "teacher";
        }
        if (roleName.contains("作者") || roleNameLower.contains("author")) {
            return "author";
        }
        return "student";
    }

    private Map<String, Object> importedMemberExtra(MemberImportRow row) {
        Map<String, Object> extra = new LinkedHashMap<>();
        row.values().forEach((key, value) -> {
            if (StringUtils.isBlank(value)) {
                return;
            }
            if (!MEMBER_FIXED_FIELD_KEYS.contains(key)) {
                extra.put(key, value);
            }
        });
        if (StringUtils.isNotBlank(row.value("roleName"))) {
            extra.put("remark", row.value("roleName"));
        }
        return extra;
    }

    private MemberAttachmentField resolveMemberAttachmentField(Project project, String fieldKey) {
        String key = StringUtils.trimToEmpty(fieldKey);
        if (!key.matches("[A-Za-z][A-Za-z0-9_]{0,63}")) {
            throw new ServiceException("成员附件字段不合法");
        }
        ActivityCategory category = categoryMapper.selectById(project.getCategoryId());
        Map<String, Object> rule = category == null ? Map.of() : parseRuleObject(category.getRuleJson());
        Object groupValue = rule.get("memberFieldGroups");
        boolean hasConfiguredFields = false;
        if (groupValue instanceof Map<?, ?> groups) {
            for (String group : List.of("author", "participant")) {
                Object fields = groups.get(group);
                if (!(fields instanceof Iterable<?> iterable)) {
                    continue;
                }
                hasConfiguredFields = true;
                for (Object item : iterable) {
                    if (!(item instanceof Map<?, ?> field)) {
                        continue;
                    }
                    String configuredKey = stringValue(field.get("fieldKey"));
                    String fieldType = stringValue(field.get("fieldType"));
                    if (!key.equals(configuredKey) || !isMemberAttachmentFieldType(fieldType)) {
                        continue;
                    }
                    Map<String, Object> validation = parseRuleObject(stringValue(field.get("validationJson")));
                    String matchFieldKey = stringOption(validation, "matchFieldKey");
                    Set<String> allowedExts = configuredMemberAttachmentExts(fieldType, validation);
                    return new MemberAttachmentField(
                        key,
                        StringUtils.blankToDefault(stringValue(field.get("fieldLabel")), key),
                        fieldType,
                        StringUtils.blankToDefault(matchFieldKey, "name"),
                        allowedExts,
                        memberAttachmentMaxSize(fieldType)
                    );
                }
            }
        }
        if (!hasConfiguredFields && "photo".equals(key)) {
            return new MemberAttachmentField("photo", "证件照", "image_upload", "name", MEMBER_PHOTO_EXTS, MEMBER_PHOTO_MAX_SIZE);
        }
        if (!hasConfiguredFields && "studentReport".equals(key)) {
            return new MemberAttachmentField("studentReport", "学籍报告", "pdf_upload", "studentNo", MEMBER_REPORT_EXTS, MEMBER_REPORT_MAX_SIZE);
        }
        throw new ServiceException("当前类别未配置该成员附件字段");
    }

    private boolean isMemberAttachmentFieldType(String fieldType) {
        return Set.of("image_upload", "pdf_upload", "attachment_upload").contains(fieldType);
    }

    private Set<String> configuredMemberAttachmentExts(String fieldType, Map<String, Object> validation) {
        if ("image_upload".equals(fieldType)) {
            return MEMBER_PHOTO_EXTS;
        }
        if ("pdf_upload".equals(fieldType)) {
            return MEMBER_REPORT_EXTS;
        }
        Object raw = validation.get("attachmentAllowedExts");
        if (!(raw instanceof Iterable<?> values)) {
            return MEMBER_ATTACHMENT_EXTS;
        }
        Set<String> result = new java.util.LinkedHashSet<>();
        for (Object value : values) {
            String ext = normalizeExt(String.valueOf(value));
            if (MEMBER_ATTACHMENT_EXTS.contains(ext)) {
                result.add(ext);
            }
        }
        return result.isEmpty() ? MEMBER_ATTACHMENT_EXTS : result;
    }

    private long memberAttachmentMaxSize(String fieldType) {
        if ("image_upload".equals(fieldType)) {
            return MEMBER_PHOTO_MAX_SIZE;
        }
        if ("pdf_upload".equals(fieldType)) {
            return MEMBER_REPORT_MAX_SIZE;
        }
        return MEMBER_ATTACHMENT_MAX_SIZE;
    }

    private String memberAttachmentMatchValue(ProjectMember member, String fieldKey) {
        return switch (fieldKey) {
            case "memberType" -> member.getMemberType();
            case "name" -> member.getName();
            case "studentNo" -> member.getStudentNo();
            case "department" -> member.getDepartment();
            case "major" -> member.getMajor();
            case "roleName" -> member.getRoleName();
            default -> stringValue(memberExtra(member).get(fieldKey));
        };
    }

    private Map<String, Object> memberExtra(ProjectMember member) {
        if (member == null || StringUtils.isBlank(member.getExtraJson()) || !JsonUtils.isJsonObject(member.getExtraJson())) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> value = JsonUtils.parseObject(member.getExtraJson(), new TypeReference<Map<String, Object>>() {
        });
        return value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value);
    }

    private boolean memberHasAttachment(ProjectMember member, MemberAttachmentField field) {
        if ("photo".equals(field.fieldKey())) {
            return member.getPhotoOssId() != null || StringUtils.isNotBlank(member.getPhotoPath());
        }
        if ("studentReport".equals(field.fieldKey())) {
            return member.getStudentReportOssId() != null || StringUtils.isNotBlank(member.getStudentReportPath());
        }
        Object value = memberExtra(member).get(field.fieldKey());
        if (value instanceof Map<?, ?> attachment) {
            return attachment.get("ossId") != null || StringUtils.isNotBlank(stringValue(attachment.get("url")));
        }
        return value != null && StringUtils.isNotBlank(String.valueOf(value));
    }

    private void applyMemberAttachment(ProjectMember member, MemberAttachmentField field, SysOssVo oss, String fileName) {
        if ("photo".equals(field.fieldKey())) {
            member.setPhotoOssId(oss.getOssId());
            member.setPhotoPath(oss.getUrl());
            return;
        }
        if ("studentReport".equals(field.fieldKey())) {
            member.setStudentReportOssId(oss.getOssId());
            member.setStudentReportPath(oss.getUrl());
            return;
        }
        Map<String, Object> extra = memberExtra(member);
        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("ossId", oss.getOssId());
        attachment.put("url", oss.getUrl());
        attachment.put("fileName", StringUtils.blankToDefault(fileName, oss.getOriginalName()));
        extra.put(field.fieldKey(), attachment);
        member.setExtraJson(JsonUtils.toJsonString(extra));
    }

    private String normalizeMemberAttachmentMatch(String value) {
        return StringUtils.isBlank(value) ? "" : value.trim().replace('\u3000', ' ').replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private String memberAttachmentMatchKey(String value) {
        return StringUtils.trimToEmpty(value);
    }

    private String fileNameWithoutExtension(String fileName) {
        String text = StringUtils.trimToEmpty(fileName);
        int index = text.lastIndexOf('.');
        return index > 0 ? text.substring(0, index) : text;
    }

    private void addMemberAttachmentBatchItem(MemberAttachmentBatchResultVo result, String fileName, Long memberId,
                                              String matchedValue, String status, String message) {
        MemberAttachmentBatchResultVo.Item item = new MemberAttachmentBatchResultVo.Item();
        item.setFileName(fileName);
        item.setMemberId(memberId);
        item.setMatchedValue(matchedValue);
        item.setStatus(status);
        item.setMessage(message);
        result.getItems().add(item);
    }

    private record MemberAttachmentField(String fieldKey, String label, String fieldType, String matchFieldKey,
                                         Set<String> allowedExts, long maxSize) {
    }

    private record MemberAttachmentBatchPlan(MultipartFile file, ProjectMember member, String matchedValue) {
    }

    private ProjectMember requireEditableMember(Long projectId, Long memberId) {
        Project project = requireProjectForUpdate(projectId);
        artReviewSecurity.checkProjectAccess(project);
        requireEditable(project);
        ProjectMember member = projectMemberMapper.selectById(memberId);
        if (member == null || !Objects.equals(member.getProjectId(), projectId)) {
            throw new ServiceException("项目成员不存在");
        }
        return member;
    }

    private List<Project> selectProjectViewVisibleProjects(ProjectVo query) {
        ProjectVo safeQuery = query == null ? new ProjectVo() : query;
        LambdaQueryWrapper<Project> lqw = buildProjectQuery(safeQuery);
        if (StringUtils.isNotBlank(safeQuery.getStatus())) {
            if (!projectScopeGuard.canViewProjectStatus(safeQuery.getStatus())) {
                return List.of();
            }
        } else {
            List<String> visibleStatuses = projectScopeGuard.visibleProjectViewStatuses();
            if (visibleStatuses.isEmpty()) {
                return List.of();
            }
            lqw.in(Project::getStatus, visibleStatuses);
        }
        lqw.ne(Project::getStatus, ArtReviewConstants.PROJECT_RECYCLED);
        return auditAssignmentService.filterProjectViewVisibleProjects(projectMapper.selectList(lqw));
    }

    private List<ProjectViewStatsVo.CountItem> toProjectViewCountItems(Map<Long, Long> counts) {
        return counts.entrySet().stream().map(entry -> {
            ProjectViewStatsVo.CountItem item = new ProjectViewStatsVo.CountItem();
            item.setId(entry.getKey());
            item.setTotalCount(entry.getValue());
            return item;
        }).toList();
    }

    private LambdaQueryWrapper<Project> buildProjectQuery(ProjectVo query) {
        LambdaQueryWrapper<Project> lqw = Wrappers.lambdaQuery();
        lqw.eq(query.getId() != null, Project::getId, query.getId());
        lqw.eq(query.getActivityId() != null, Project::getActivityId, query.getActivityId());
        lqw.eq(query.getCategoryId() != null, Project::getCategoryId, query.getCategoryId());
        lqw.eq(query.getSchoolId() != null, Project::getSchoolId, query.getSchoolId());
        lqw.in(CollUtil.isNotEmpty(query.getCategoryIds()), Project::getCategoryId, query.getCategoryIds());
        lqw.and(StringUtils.isNotBlank(query.getGroupCode()), wrapper -> wrapper
            .eq(Project::getGroupCode, query.getGroupCode())
            .or()
            .eq(Project::getGroupName, query.getGroupCode()));
        lqw.eq(StringUtils.isNotBlank(query.getStatus()), Project::getStatus, query.getStatus());
        lqw.like(StringUtils.isNotBlank(query.getProjectName()), Project::getProjectName, query.getProjectName());
        lqw.orderByDesc(Project::getCreateTime);
        return lqw;
    }

    private Project requireProject(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        return project;
    }

    private Project requireProjectForUpdate(Long id) {
        Project project = projectMapper.selectOne(Wrappers.lambdaQuery(Project.class)
            .eq(Project::getId, id)
            .last("for update"));
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        return project;
    }

    private void requireEditable(Project project) {
        if (!ArtReviewConstants.PROJECT_DRAFT.equals(project.getStatus())
            && !ArtReviewConstants.PROJECT_RETURNED.equals(project.getStatus())
            && !ArtReviewConstants.PROJECT_PARTICIPANT_RETURNED.equals(project.getStatus())) {
            throw new ServiceException("项目当前状态不可编辑");
        }
    }

    private void requireCategoryAvailable(Long activityId, Long categoryId) {
        Date now = new Date();
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || !ArtReviewConstants.ACTIVITY_ENABLED.equals(activity.getStatus())) {
            throw new ServiceException("活动不存在或未启用");
        }
        if (activity.getSignupStartAt() != null && activity.getSignupStartAt().after(now)) {
            throw new ServiceException("活动报名尚未开始");
        }
        if (activity.getSignupEndAt() != null && activity.getSignupEndAt().before(now)) {
            throw new ServiceException("活动报名已结束");
        }
        Long schoolId = artReviewSecurity.requireEnabledSchoolId();
        String scopeType = StringUtils.blankToDefault(activity.getScopeType(), ArtReviewConstants.ACTIVITY_SCOPE_ALL);
        if (ArtReviewConstants.ACTIVITY_SCOPE_ASSIGNED.equals(scopeType) && !activityScopeService.isSchoolAllowed(activityId, schoolId)) {
            throw new ServiceException("当前学校未获得该活动授权");
        }
        ActivityCategory category = categoryMapper.selectById(categoryId);
        if (category == null || !Objects.equals(category.getActivityId(), activityId)) {
            throw new ServiceException("类别不存在或不属于当前活动");
        }
        categoryHierarchyService.requireBusinessAvailableLeaf(category);
    }

    private boolean quotaRuleHasCountLimit(ActivityReportRuleRuntime rule) {
        return rule.hasCountLimit();
    }

    private String quotaTargetFieldKey(ActivityReportRuleRuntime rule) {
        Map<String, Object> options = parseQuotaRuleJson(rule.getRuleJson());
        return stringOption(options, "targetFieldKey", "groupFieldKey");
    }

    private String ratioOperator(String ruleJson) {
        Map<String, Object> rule = parseQuotaRuleJson(ruleJson);
        Object operator = rule == null ? null : rule.get("ratioOperator");
        return operator == null ? "max" : String.valueOf(operator);
    }

    private Map<String, Object> parseQuotaRuleJson(String ruleJson) {
        if (StringUtils.isBlank(ruleJson) || !JsonUtils.isJsonObject(ruleJson)) {
            return Map.of();
        }
        Map<String, Object> rule = JsonUtils.parseObject(ruleJson, new TypeReference<Map<String, Object>>() {
        });
        return rule == null ? Map.of() : rule;
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

    private Map<String, Object> parseRuleObject(String text) {
        if (StringUtils.isBlank(text) || !JsonUtils.isJsonObject(text)) {
            return Map.of();
        }
        Map<String, Object> parsed = JsonUtils.parseObject(text, new TypeReference<Map<String, Object>>() {
        });
        return parsed == null ? Map.of() : parsed;
    }

    private void validateMemberUploadFile(MultipartFile file, Set<String> allowedExts, long maxSize, String label) {
        String message = memberUploadValidationMessage(file, allowedExts, maxSize, label);
        if (StringUtils.isNotBlank(message)) {
            throw new ServiceException(message);
        }
    }

    private String memberUploadValidationMessage(MultipartFile file, Set<String> allowedExts, long maxSize, String label) {
        if (file == null || file.isEmpty()) {
            return label + "不能为空";
        }
        String ext = normalizeExt(file.getOriginalFilename());
        if (StringUtils.isBlank(ext) || !allowedExts.contains(ext)) {
            return label + "格式不正确，请上传 " + formatAllowedExt(allowedExts) + " 格式文件";
        }
        if (file.getSize() > maxSize) {
            return label + "大小不能超过 " + formatFileSize(maxSize);
        }
        return "";
    }

    private String formatAllowedExt(Set<String> allowedExts) {
        String text = allowedExts.stream()
            .filter(StringUtils::isNotBlank)
            .sorted()
            .collect(Collectors.joining("/"));
        return StringUtils.isBlank(text) ? "指定" : text;
    }

    private String formatFileSize(long bytes) {
        long mb = bytes / 1024L / 1024L;
        return mb > 0 ? mb + "MB" : bytes + "B";
    }

    private String normalizeExt(String nameOrExt) {
        if (StringUtils.isBlank(nameOrExt)) {
            return "";
        }
        String value = nameOrExt.trim().toLowerCase();
        int index = value.lastIndexOf('.');
        if (index >= 0) {
            value = value.substring(index + 1);
        }
        return value;
    }

    private record MemberTemplateField(String fieldKey, String fieldLabel, String group, String fieldType, boolean required, int sortOrder, int sequence) {
    }

    private record HeaderMapping(int headerRowIndex, Map<Integer, String> columnKeys) {
    }

    private record MemberImportRow(Map<String, String> values) {
        private String value(String fieldKey) {
            return values.getOrDefault(fieldKey, "");
        }

        private boolean isBlank() {
            return values.values().stream().allMatch(value -> StringUtils.isBlank(value));
        }

        private boolean isFooter() {
            return values.values().stream().anyMatch(value -> StringUtils.isNotBlank(value) && value.trim().startsWith("说明"));
        }
    }

    private record GenericTableTemplateSpec(
        String templateKey,
        String templateName,
        String tipText,
        int minRows,
        int maxRows,
        String excelTitle,
        String fileName,
        String sheetName,
        int blankRows,
        String footerText,
        int sortOrder,
        List<GenericTableColumnSpec> fields
    ) {
    }

    private record GenericTableColumnSpec(
        String fieldKey,
        String fieldLabel,
        String fieldType,
        boolean required,
        String validationType,
        List<String> options,
        int sortOrder,
        int width
    ) {
    }

}
