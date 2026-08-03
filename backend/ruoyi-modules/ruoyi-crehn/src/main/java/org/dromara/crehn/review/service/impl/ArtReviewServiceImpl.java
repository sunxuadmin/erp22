package org.dromara.crehn.review.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.common.AssignmentSchoolScopeSupport;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.common.ReviewAssignmentProjectScopeSupport;
import org.dromara.crehn.config.service.ArtDetailDisplayConfigService;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.CategoryFieldSchema;
import org.dromara.crehn.domain.CategoryFileRequirement;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ProjectAuditRecord;
import org.dromara.crehn.domain.ProjectFile;
import org.dromara.crehn.domain.ProjectMember;
import org.dromara.crehn.domain.ReviewAssignment;
import org.dromara.crehn.domain.ReviewAssignmentCategoryConfig;
import org.dromara.crehn.domain.ReviewAssignmentProjectFilter;
import org.dromara.crehn.domain.ReviewAssignmentProjectScopeGroup;
import org.dromara.crehn.domain.ReviewResult;
import org.dromara.crehn.domain.ReviewResultLog;
import org.dromara.crehn.domain.ReviewScore;
import org.dromara.crehn.domain.ReviewScoreSheetSignedSheet;
import org.dromara.crehn.domain.ReviewScoreSheetSignedSheetItem;
import org.dromara.crehn.domain.bo.ReviewAssignmentScopePlanBo;
import org.dromara.crehn.domain.bo.ReviewAssignmentScopePlanItemBo;
import org.dromara.crehn.domain.bo.ReviewScoreAdminBo;
import org.dromara.crehn.domain.bo.ReviewAssignmentBo;
import org.dromara.crehn.domain.bo.ReviewScoreBo;
import org.dromara.crehn.domain.vo.ActivityCategoryVo;
import org.dromara.crehn.domain.vo.ActivityVo;
import org.dromara.crehn.domain.vo.ArtDetailDisplayConfigVo;
import org.dromara.crehn.domain.vo.AssignmentBatchPreviewVo;
import org.dromara.crehn.domain.vo.CategoryFieldSchemaVo;
import org.dromara.crehn.domain.vo.CategoryFileRequirementVo;
import org.dromara.crehn.domain.vo.ProjectAuditRecordVo;
import org.dromara.crehn.domain.vo.ProjectFileVo;
import org.dromara.crehn.domain.vo.ProjectMemberVo;
import org.dromara.crehn.domain.vo.ProjectVo;
import org.dromara.crehn.domain.vo.ReviewAssignmentVo;
import org.dromara.crehn.domain.vo.ReviewAssignmentCategoryConfigVo;
import org.dromara.crehn.domain.vo.ReviewAssignmentScopeUnitVo;
import org.dromara.crehn.domain.vo.ReviewProjectOverviewVo;
import org.dromara.crehn.domain.vo.ReviewScoreVo;
import org.dromara.crehn.domain.vo.ReviewTaskVo;
import org.dromara.crehn.domain.vo.ReviewTaskNavigatorVo;
import org.dromara.crehn.domain.vo.ReviewWorkbenchVo;
import org.dromara.crehn.domain.vo.SchoolInfoVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ActivityMapper;
import org.dromara.crehn.mapper.CategoryFieldSchemaMapper;
import org.dromara.crehn.mapper.CategoryFileRequirementMapper;
import org.dromara.crehn.mapper.ProjectAuditRecordMapper;
import org.dromara.crehn.mapper.ProjectFileMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.ProjectMemberMapper;
import org.dromara.crehn.mapper.ReviewAssignmentMapper;
import org.dromara.crehn.mapper.ReviewResultLogMapper;
import org.dromara.crehn.mapper.ReviewResultMapper;
import org.dromara.crehn.mapper.ReviewScoreMapper;
import org.dromara.crehn.mapper.ReviewScoreSheetSignedSheetMapper;
import org.dromara.crehn.mapper.ReviewScoreSheetSignedSheetItemMapper;
import org.dromara.crehn.mapper.SchoolInfoMapper;
import org.dromara.crehn.review.service.IArtReviewService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysMenu;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysRoleMenu;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysMenuMapper;
import org.dromara.system.mapper.SysRoleMenuMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.dromara.system.service.ISysOssService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ArtReviewServiceImpl implements IArtReviewService {

    private static final String PERM_REVIEW_TASK = "crehn:review:task";
    private static final String PERM_REVIEW_SCORE = "crehn:review:score";
    private static final String HIDE_ALL_FORM_FIELDS = "*";
    private static final String HIDDEN_FILE_REQUIREMENT_PREFIX = "__file_requirement__:";
    private static final String REVIEW_ASSIGNMENT_CONFIG_KEY = "reviewAssignmentConfig";
    private static final String FILTER_UNFILLED_VALUE = "__unfilled__";
    private static final String UNGROUPED_LABEL = "未分组";
    private static final String SCOPE_MODE_ALL = "all";
    private static final String SCOPE_MODE_UNITS = "units";
    private static final String SCOPE_PLAN_REPLACE = "replace";
    private static final String SIGNED_SHEET_ACTIVE = "active";
    private static final Integer SIGNED_SHEET_ACTIVE_MARKER = 1;
    private static final String[] PROGRAM_FORM_KEYS = {"programForm", "program_form", "workForm", "work_form", "showForm", "show_form", "performanceForm", "performance_form"};
    private static final String[] GROUP_NAME_KEYS = {
        "groupName", "group_name", "group", "groupCode", "group_code",
        "displayGroup", "display_group", "designGroup", "design_group", "projectNature", "project_nature"
    };
    private static final Set<String> TEACHER_FIELD_ALIASES = Set.of(
        "teacher", "teacherName", "teacher_name", "instructor", "instructorName", "instructor_name", "adviserTeachers"
    );
    private static final Set<String> LEGACY_HIDE_ALL_FORM_FIELD_KEYS = Set.of(
        "school_name", "schoolName", "student_name", "studentName", "name", "id_card", "idCard",
        "teacher_name", "teacherName", "teacher", "instructor_name", "phone", "mobile", "email",
        "department", "major", "class_name", "description", "remark"
    );
    private static final Set<String> REQUIRED_REVIEW_PERMISSIONS = Set.of(PERM_REVIEW_TASK, PERM_REVIEW_SCORE);

    private final ReviewAssignmentMapper assignmentMapper;
    private final ReviewScoreMapper scoreMapper;
    private final ReviewScoreSheetSignedSheetMapper signedSheetMapper;
    private final ReviewScoreSheetSignedSheetItemMapper signedSheetItemMapper;
    private final ReviewResultMapper resultMapper;
    private final ReviewResultLogMapper resultLogMapper;
    private final ProjectMapper projectMapper;
    private final ProjectFileMapper projectFileMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectAuditRecordMapper auditRecordMapper;
    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final CategoryFieldSchemaMapper fieldMapper;
    private final CategoryFileRequirementMapper fileRequirementMapper;
    private final SchoolInfoMapper schoolInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final ISysOssService ossService;
    private final ArtDetailDisplayConfigService displayConfigService;

    @Override
    public TableDataInfo<ReviewAssignmentVo> queryAssignmentPage(ReviewAssignmentVo query, PageQuery pageQuery) {
        LambdaQueryWrapper<ReviewAssignment> lqw = Wrappers.lambdaQuery(ReviewAssignment.class)
            .eq(query.getActivityId() != null, ReviewAssignment::getActivityId, query.getActivityId())
            .eq(query.getCategoryId() != null, ReviewAssignment::getCategoryId, query.getCategoryId())
            .eq(query.getReviewerUserId() != null, ReviewAssignment::getReviewerUserId, query.getReviewerUserId())
            .eq(StringUtils.isNotBlank(query.getStatus()), ReviewAssignment::getStatus, query.getStatus())
            .orderByDesc(ReviewAssignment::getCreateTime)
            .orderByDesc(ReviewAssignment::getId);
        Page<ReviewAssignmentVo> page = assignmentMapper.selectVoPage(pageQuery.build(), lqw);
        fillAssignmentNames(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public ReviewAssignmentVo getAssignment(Long id) {
        ReviewAssignmentVo vo = assignmentMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("评审分配不存在");
        }
        fillAssignmentNames(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAssignment(ReviewAssignmentBo bo) {
        if (bo == null) {
            throw new ServiceException("评审分配不能为空");
        }
        if (bo.getReviewerUserIds() != null || bo.getCategoryIds() != null) {
            bo.setBatchOperation(StringUtils.blankToDefault(bo.getBatchOperation(), ArtReviewConstants.ASSIGNMENT_BATCH_MERGE));
            maintainAssignmentBatch(bo);
            return;
        }
        validateAssignmentBo(bo);
        ensureNoDuplicateAssignment(null, bo.getActivityId(), bo.getCategoryId(), bo.getReviewerUserId());
        ReviewAssignment assignment = new ReviewAssignment();
        copyAssignment(assignment, bo, bo.getCategoryId(), bo.getReviewerUserId());
        assignment.setAssignedBy(LoginHelper.getUserId());
        assignment.setAssignedAt(new Date());
        assignmentMapper.insert(assignment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAssignment(ReviewAssignmentBo bo) {
        if (bo == null || bo.getId() == null) {
            throw new ServiceException("评审分配ID不能为空");
        }
        validateAssignmentBo(bo);
        ReviewAssignment assignment = requireAssignment(bo.getId());
        validateScoreConfigMutable(assignment, bo);
        ensureNoDuplicateAssignment(bo.getId(), bo.getActivityId(), bo.getCategoryId(), bo.getReviewerUserId());
        copyAssignment(assignment, bo, bo.getCategoryId(), bo.getReviewerUserId());
        assignmentMapper.updateById(assignment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAssignments(Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new ServiceException("评审分配ID不能为空");
        }
        for (Long id : ids) {
            ReviewAssignment assignment = assignmentMapper.selectById(id);
            if (assignment != null && !ArtReviewConstants.REVIEW_ASSIGNMENT_DISABLED.equals(assignment.getStatus())) {
                assignment.setStatus(ArtReviewConstants.REVIEW_ASSIGNMENT_DISABLED);
                assignmentMapper.updateById(assignment);
            }
        }
    }

    @Override
    public AssignmentBatchPreviewVo previewAssignmentBatch(ReviewAssignmentBo bo) {
        return executeAssignmentBatch(bo, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignmentBatchPreviewVo maintainAssignmentBatch(ReviewAssignmentBo bo) {
        String operation = normalizeBatchOperation(bo == null ? null : bo.getBatchOperation());
        checkBatchPermissions(operation);
        return executeAssignmentBatch(bo, true);
    }

    @Override
    public AssignmentBatchPreviewVo previewAssignmentScopePlan(ReviewAssignmentScopePlanBo bo) {
        return executeAssignmentScopePlan(bo, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignmentBatchPreviewVo maintainAssignmentScopePlan(ReviewAssignmentScopePlanBo bo) {
        String operation = normalizeScopePlanOperation(bo == null ? null : bo.getOperation());
        checkScopePlanPermissions(operation);
        return executeAssignmentScopePlan(bo, true);
    }

    @Override
    public List<SysUserVo> reviewerOptions(String keyword, Long activityId, Long categoryId, Long includeUserId) {
        Set<Long> userIds = reviewerUserIdsWithPermissions();
        excludeAssignedReviewerIds(userIds, activityId, categoryId, includeUserId);
        if (userIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<SysUser> lqw = Wrappers.lambdaQuery(SysUser.class)
            .in(SysUser::getUserId, userIds)
            .eq(SysUser::getStatus, "0")
            .eq(SysUser::getDelFlag, "0")
            .and(StringUtils.isNotBlank(keyword), w -> w.like(SysUser::getUserName, keyword).or().like(SysUser::getNickName, keyword))
            .orderByAsc(SysUser::getUserName)
            .last("limit 500");
        return sysUserMapper.selectVoList(lqw);
    }

    @Override
    public List<ProjectVo> approvedProjectOptions(Long activityId, Long categoryId, String keyword) {
        if (activityId == null || categoryId == null) {
            return List.of();
        }
        return projectMapper.selectVoList(Wrappers.lambdaQuery(Project.class)
            .eq(Project::getActivityId, activityId)
            .eq(Project::getCategoryId, categoryId)
            .eq(Project::getStatus, ArtReviewConstants.PROJECT_AUDIT_PASSED)
            .like(StringUtils.isNotBlank(keyword), Project::getProjectName, keyword)
            .orderByDesc(Project::getSubmittedAt)
            .orderByDesc(Project::getCreateTime)
            .last("limit 500"));
    }

    @Override
    public List<SchoolInfoVo> assignmentSchoolOptions(String keyword) {
        return schoolInfoMapper.selectVoList(Wrappers.lambdaQuery(org.dromara.crehn.domain.SchoolInfo.class)
            .eq(org.dromara.crehn.domain.SchoolInfo::getStatus, "enabled")
            .and(StringUtils.isNotBlank(keyword), wrapper -> wrapper
                .like(org.dromara.crehn.domain.SchoolInfo::getSchoolName, keyword)
                .or()
                .like(org.dromara.crehn.domain.SchoolInfo::getSchoolCode, keyword))
            .orderByAsc(org.dromara.crehn.domain.SchoolInfo::getSchoolCode)
            .orderByAsc(org.dromara.crehn.domain.SchoolInfo::getId)
            .last("limit 1000"));
    }

    @Override
    public List<CategoryFieldSchemaVo> assignmentFilterFields(Long categoryId) {
        if (categoryId == null || categoryMapper.selectById(categoryId) == null) {
            throw new ServiceException("类别不存在");
        }
        return fieldMapper.selectVoList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
            .eq(CategoryFieldSchema::getCategoryId, categoryId)
            .orderByAsc(CategoryFieldSchema::getSortOrder)
            .orderByAsc(CategoryFieldSchema::getId));
    }

    @Override
    public ReviewAssignmentCategoryConfigVo getAssignmentCategoryConfig(Long categoryId) {
        ActivityCategory category = requireCategory(categoryId);
        return buildAssignmentCategoryConfigVo(category, assignmentCategoryConfig(category));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewAssignmentCategoryConfigVo saveAssignmentCategoryConfig(Long categoryId, ReviewAssignmentCategoryConfigVo input) {
        ActivityCategory category = requireCategory(categoryId);
        ReviewAssignmentCategoryConfig config = new ReviewAssignmentCategoryConfig();
        config.setScopeFieldKeys(normalizeScopeFieldKeys(categoryId, input == null ? null : input.getScopeFieldKeys()));
        config.setVisibilityConfigured(input != null && Boolean.TRUE.equals(input.getVisibilityConfigured()));
        config.setHideSchoolInfo(input == null || !Boolean.FALSE.equals(input.getHideSchoolInfo()));
        config.setHideMemberInfo(input != null && Boolean.TRUE.equals(input.getHideMemberInfo()));
        config.setHiddenFieldKeysJson(normalizeHiddenFieldKeysJson(input == null ? null : input.getHiddenFieldKeysJson(), categoryId));
        Map<String, Object> rule = new LinkedHashMap<>(parseObject(category.getRuleJson()));
        rule.put(REVIEW_ASSIGNMENT_CONFIG_KEY, config);
        category.setRuleJson(JsonUtils.toJsonString(rule));
        categoryMapper.updateById(category);
        return buildAssignmentCategoryConfigVo(category, config);
    }

    @Override
    public TableDataInfo<ReviewProjectOverviewVo> queryAssignmentProjectPage(ReviewProjectOverviewVo query, PageQuery pageQuery) {
        ReviewProjectOverviewVo safeQuery = query == null ? new ReviewProjectOverviewVo() : query;
        List<Project> projects = projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
            .eq(safeQuery.getActivityId() != null, Project::getActivityId, safeQuery.getActivityId())
            .eq(safeQuery.getCategoryId() != null, Project::getCategoryId, safeQuery.getCategoryId())
            .eq(safeQuery.getSchoolId() != null, Project::getSchoolId, safeQuery.getSchoolId())
            .like(StringUtils.isNotBlank(safeQuery.getProjectName()), Project::getProjectName, safeQuery.getProjectName())
            .eq(Project::getStatus, ArtReviewConstants.PROJECT_AUDIT_PASSED)
            .orderByDesc(Project::getSubmittedAt)
            .orderByDesc(Project::getCreateTime)
            .orderByDesc(Project::getId));
        if (projects.isEmpty()) {
            return TableDataInfo.build(List.of(), pageQuery.build());
        }

        List<Long> activityIds = projects.stream().map(Project::getActivityId).filter(Objects::nonNull).distinct().toList();
        List<Long> categoryIds = projects.stream().map(Project::getCategoryId).filter(Objects::nonNull).distinct().toList();
        List<Long> schoolIds = projects.stream().map(Project::getSchoolId).filter(Objects::nonNull).distinct().toList();
        List<Long> projectIds = projects.stream().map(Project::getId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Activity> activityMap = activityIds.isEmpty()
            ? Map.of()
            : activityMapper.selectBatchIds(activityIds).stream()
                .collect(Collectors.toMap(Activity::getId, item -> item, (left, right) -> left));
        Map<Long, ActivityCategory> categoryMap = categoryIds.isEmpty()
            ? Map.of()
            : categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left));
        Map<Long, org.dromara.crehn.domain.SchoolInfo> schoolMap = schoolIds.isEmpty()
            ? Map.of()
            : schoolInfoMapper.selectBatchIds(schoolIds).stream()
                .collect(Collectors.toMap(org.dromara.crehn.domain.SchoolInfo::getId, item -> item, (left, right) -> left));
        List<ReviewAssignment> assignments = categoryIds.isEmpty()
            ? List.of()
            : assignmentMapper.selectList(Wrappers.lambdaQuery(ReviewAssignment.class)
                .in(ReviewAssignment::getActivityId, activityIds)
                .in(ReviewAssignment::getCategoryId, categoryIds));
        List<ReviewScore> scores = projectIds.isEmpty()
            ? List.of()
            : scoreMapper.selectList(Wrappers.lambdaQuery(ReviewScore.class)
                .in(ReviewScore::getProjectId, projectIds));
        Map<Long, List<ReviewScore>> scoreMap = scores.stream()
            .collect(Collectors.groupingBy(ReviewScore::getProjectId));
        List<Long> reviewerUserIds = assignments.stream()
            .map(ReviewAssignment::getReviewerUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, SysUser> reviewerMap = reviewerUserIds.isEmpty()
            ? Map.of()
            : sysUserMapper.selectBatchIds(reviewerUserIds).stream()
                .collect(Collectors.toMap(SysUser::getUserId, item -> item, (left, right) -> left));

        List<ReviewProjectOverviewVo> rows = new ArrayList<>();
        for (Project project : projects) {
            List<ReviewAssignment> activeAssignments = assignments.stream()
                .filter(assignment -> ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE.equals(assignment.getStatus()))
                .filter(assignment -> assignmentMatchesProject(assignment, project, schoolMap.get(project.getSchoolId())))
                .toList();
            Set<Long> activeAssignmentIds = activeAssignments.stream()
                .map(ReviewAssignment::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            List<ReviewScore> projectScores = scoreMap.getOrDefault(project.getId(), List.of());
            long submittedCount = projectScores.stream()
                .filter(score -> ArtReviewConstants.REVIEW_SCORE_SUBMITTED.equals(score.getStatus()))
                .count();
            long draftCount = projectScores.stream()
                .filter(score -> activeAssignmentIds.contains(score.getAssignmentId()))
                .filter(score -> ArtReviewConstants.REVIEW_SCORE_DRAFT.equals(score.getStatus()))
                .count();
            long activeScoredCount = projectScores.stream()
                .filter(score -> activeAssignmentIds.contains(score.getAssignmentId()))
                .filter(score -> ArtReviewConstants.REVIEW_SCORE_DRAFT.equals(score.getStatus())
                    || ArtReviewConstants.REVIEW_SCORE_SUBMITTED.equals(score.getStatus()))
                .map(ReviewScore::getAssignmentId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
            long requiredReviewerCount = activeAssignments.stream()
                .map(ReviewAssignment::getScoreRuleJson)
                .map(this::minimumReviewerCount)
                .max(Integer::compareTo)
                .orElse(0);
            long shortageCount = Math.max(0L, requiredReviewerCount - activeAssignments.size());
            String assignmentStatus = activeAssignments.isEmpty() ? "unassigned" : shortageCount > 0 ? "shortage" : "assigned";
            if (StringUtils.isNotBlank(safeQuery.getAssignmentStatus())
                && !Objects.equals(safeQuery.getAssignmentStatus(), assignmentStatus)) {
                continue;
            }

            ReviewProjectOverviewVo row = new ReviewProjectOverviewVo();
            Activity activity = activityMap.get(project.getActivityId());
            ActivityCategory category = categoryMap.get(project.getCategoryId());
            org.dromara.crehn.domain.SchoolInfo school = schoolMap.get(project.getSchoolId());
            row.setActivityId(project.getActivityId());
            row.setActivityName(activity == null ? null : activity.getActivityName());
            row.setCategoryId(project.getCategoryId());
            row.setCategoryName(category == null ? null : category.getCategoryName());
            row.setProjectId(project.getId());
            row.setProjectNo(project.getProjectNo());
            row.setProjectName(project.getProjectName());
            row.setSchoolId(project.getSchoolId());
            row.setSchoolName(school == null ? null : school.getSchoolName());
            row.setStatus(project.getStatus());
            row.setSubmittedAt(project.getSubmittedAt());
            row.setCreateTime(project.getCreateTime());
            row.setAssignmentCount((long) activeAssignments.size());
            row.setPendingScoreCount(Math.max(0L, activeAssignments.size() - activeScoredCount));
            row.setDraftScoreCount(draftCount);
            row.setSubmittedScoreCount(submittedCount);
            row.setRequiredReviewerCount(requiredReviewerCount);
            row.setShortageCount(shortageCount);
            row.setAssignmentStatus(assignmentStatus);
            row.setReviewerNames(activeAssignments.stream()
                .map(ReviewAssignment::getReviewerUserId)
                .map(reviewerMap::get)
                .filter(Objects::nonNull)
                .map(user -> StringUtils.blankToDefault(user.getNickName(), user.getUserName()))
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining("、")));
            rows.add(row);
        }
        return TableDataInfo.build(rows, pageQuery.build());
    }

    @Override
    public TableDataInfo<ReviewTaskVo> queryMyTaskPage(ReviewTaskVo query, PageQuery pageQuery) {
        List<ReviewTaskVo> tasks = collectCurrentUserTasks(query);
        TableDataInfo<ReviewTaskVo> page = TableDataInfo.build(tasks, pageQuery.build());
        fillReviewListDisplayFields(page.getRows());
        return page;
    }

    @Override
    public ReviewTaskNavigatorVo queryMyTaskNavigator(ReviewTaskVo query) {
        ReviewTaskVo source = query == null ? new ReviewTaskVo() : query;
        // Category badges are an activity-level baseline. They must not be
        // rebuilt from the currently selected category, otherwise every sibling
        // disappears from the response and the shared navigator displays 0.
        ReviewTaskVo categoryCountQuery = new ReviewTaskVo();
        categoryCountQuery.setActivityId(source.getActivityId());
        List<ReviewTaskVo> activityTasks = collectCurrentUserTasks(categoryCountQuery);

        // Progress describes the selected category and group scope but is independent of
        // score status and project search. Category signing
        // always covers the reviewer's whole category assignment.
        ReviewTaskVo progressQuery = new ReviewTaskVo();
        progressQuery.setActivityId(source.getActivityId());
        progressQuery.setCategoryId(source.getCategoryId());
        progressQuery.setCategoryIds(source.getCategoryIds());
        List<ReviewTaskVo> categoryScopeTasks = collectCurrentUserTasks(progressQuery);
        progressQuery.setGroupOrNature(source.getGroupOrNature());
        progressQuery.setScopeKey(source.getScopeKey());
        List<ReviewTaskVo> scopeTasks = collectCurrentUserTasks(progressQuery);
        ReviewTaskNavigatorVo result = new ReviewTaskNavigatorVo();
        result.setCategoryOptions(countOptions(activityTasks, ReviewTaskVo::getCategoryId, ReviewTaskVo::getCategoryName));
        result.setScopeOptions(buildCurrentReviewerScopeOptions(source.getActivityId(), activityTasks));
        result.setProgramFormOptions(countOptions(scopeTasks, ReviewTaskVo::getProgramForm, ReviewTaskVo::getProgramForm));
        result.setGroupOrNatureOptions(countOptions(categoryScopeTasks, ReviewTaskVo::getGroupOrNature, ReviewTaskVo::getGroupOrNature));
        result.setTotalCount((long) scopeTasks.size());
        result.setCompletedCount(scopeTasks.stream().filter(this::isTaskScoreComplete).count());
        result.setLockedCount(scopeTasks.stream()
            .filter(task -> ArtReviewConstants.REVIEW_SCORE_SUBMITTED.equals(task.getScoreStatus())).count());
        result.setCategoryTotalCount((long) categoryScopeTasks.size());
        result.setCategoryCompletedCount(categoryScopeTasks.stream().filter(this::isTaskScoreComplete).count());
        result.setCategoryLockedCount(categoryScopeTasks.stream()
            .filter(task -> ArtReviewConstants.REVIEW_SCORE_SUBMITTED.equals(task.getScoreStatus())).count());
        result.setDisplayColumns(reviewListDisplayColumns(scopeTasks, source.getCategoryId()));

        return result;
    }

    private boolean isTaskScoreComplete(ReviewTaskVo task) {
        if (task == null || task.getScoreId() == null) {
            return false;
        }
        return ArtReviewConstants.REVIEW_SCORE_MODE_GRADE.equals(task.getScoreMode())
            ? StringUtils.isNotBlank(task.getGradeValue())
            : task.getScoreValue() != null;
    }

    @Override
    // ART-REF: BE.REVIEW.SCORE_SHEET
    public List<ReviewTaskVo> queryMySubmittedTasksForExport(Long activityId, Long categoryId) {
        ReviewTaskVo query = new ReviewTaskVo();
        query.setActivityId(activityId);
        query.setCategoryId(categoryId);
        query.setScoreStatus(ArtReviewConstants.REVIEW_SCORE_SUBMITTED);
        return collectCurrentUserTasks(query);
    }

    @Override
    // ART-REF: BE.REVIEW.SCORE_SHEET
    public List<ReviewTaskVo> queryMyScopeTasksForSignature(Long activityId, Long categoryId, String scopeKey) {
        ReviewTaskVo query = new ReviewTaskVo();
        query.setActivityId(activityId);
        query.setCategoryId(categoryId);
        query.setScopeKey(scopeKey);
        return collectCurrentUserTasks(query);
    }

    @Override
    public ReviewWorkbenchVo workbench() {
        ReviewTaskVo query = new ReviewTaskVo();
        List<ReviewTaskVo> tasks = collectCurrentUserTasks(query);
        ReviewWorkbenchVo vo = new ReviewWorkbenchVo();
        vo.setActivityName(tasks.stream().map(ReviewTaskVo::getActivityName).filter(StringUtils::isNotBlank).findFirst().orElse(null));
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("assigned_total", (long) tasks.size());
        stats.put("pending_score", tasks.stream().filter(task -> StringUtils.isBlank(task.getScoreStatus())).count());
        stats.put("draft_score", tasks.stream().filter(task -> ArtReviewConstants.REVIEW_SCORE_DRAFT.equals(task.getScoreStatus())).count());
        stats.put("submitted_score", tasks.stream().filter(task -> ArtReviewConstants.REVIEW_SCORE_SUBMITTED.equals(task.getScoreStatus())).count());
        stats.put("locked_by_other", tasks.stream().filter(task -> Boolean.TRUE.equals(task.getLockedByOther())).count());
        vo.setStats(stats);
        vo.setPendingTasks(tasks.stream()
            .filter(task -> StringUtils.isBlank(task.getScoreStatus()) && !Boolean.TRUE.equals(task.getLockedByOther()))
            .limit(10)
            .toList());
        vo.setGroupItems(buildReviewWorkbenchGroups(tasks));
        List<ReviewScoreVo> recentScores = scoreMapper.selectVoList(Wrappers.lambdaQuery(ReviewScore.class)
            .eq(ReviewScore::getReviewerUserId, LoginHelper.getUserId())
            .orderByDesc(ReviewScore::getSubmittedAt)
            .orderByDesc(ReviewScore::getCreateTime)
            .last("limit 10"));
        fillScoreReviewerNames(recentScores);
        vo.setRecentScores(recentScores);
        return vo;
    }

    @Override
    public List<ActivityVo> activityOptions() {
        List<Long> activityIds = currentUserAssignments(null, null).stream()
            .map(ReviewAssignment::getActivityId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (activityIds.isEmpty()) {
            return List.of();
        }
        return activityMapper.selectVoList(Wrappers.lambdaQuery(Activity.class)
            .in(Activity::getId, activityIds)
            .eq(Activity::getStatus, ArtReviewConstants.ACTIVITY_ENABLED)
            .orderByDesc(Activity::getCreateTime)
            .orderByDesc(Activity::getId));
    }

    @Override
    public List<ActivityCategoryVo> categoryOptions(Long activityId) {
        if (activityId == null) {
            return List.of();
        }
        Set<Long> visibleCategoryIds = currentUserAssignments(activityId, null).stream()
            .map(ReviewAssignment::getCategoryId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (visibleCategoryIds.isEmpty()) {
            return List.of();
        }
        List<ActivityCategoryVo> categories = categoryMapper.selectVoList(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getActivityId, activityId)
            .eq(ActivityCategory::getEnabled, true)
            .orderByAsc(ActivityCategory::getSortOrder)
            .orderByAsc(ActivityCategory::getId));
        Map<Long, ActivityCategoryVo> categoryMap = categories.stream()
            .filter(category -> category.getId() != null)
            .collect(Collectors.toMap(ActivityCategoryVo::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        for (Long categoryId : List.copyOf(visibleCategoryIds)) {
            ActivityCategoryVo current = categoryMap.get(categoryId);
            Set<Long> visited = new HashSet<>();
            while (current != null && current.getParentId() != null && visited.add(current.getParentId())) {
                visibleCategoryIds.add(current.getParentId());
                current = categoryMap.get(current.getParentId());
            }
        }
        return categories.stream()
            .filter(category -> category.getId() != null && visibleCategoryIds.contains(category.getId()))
            .toList();
    }

    private List<ReviewTaskVo> collectCurrentUserTasks(ReviewTaskVo query) {
        ReviewTaskVo safeQuery = query == null ? new ReviewTaskVo() : query;
        AssignmentScopeKey requestedScope = parseAssignmentScopeKey(safeQuery.getScopeKey());
        Set<Long> requestedCategoryIds = parseIdSet(safeQuery.getCategoryIds());
        List<ReviewAssignment> assignments = currentUserAssignments(safeQuery.getActivityId(), safeQuery.getCategoryId()).stream()
            .filter(assignment -> requestedCategoryIds.isEmpty() || requestedCategoryIds.contains(assignment.getCategoryId()))
            .filter(assignment -> requestedScope == null || Objects.equals(requestedScope.assignmentId(), assignment.getId()))
            .toList();
        if (StringUtils.isNotBlank(safeQuery.getScopeKey()) && requestedScope == null) {
            return List.of();
        }
        if (assignments.isEmpty()) {
            return List.of();
        }
        List<ReviewTaskVo> tasks = new ArrayList<>();
        for (ReviewAssignment assignment : assignments) {
            ReviewAssignmentScopeUnitVo requestedUnit = requestedScope != null && requestedScope.unitKey() != null
                ? findAssignmentScopeUnit(assignment, requestedScope.unitKey())
                : null;
            if (requestedScope != null && requestedScope.unitKey() != null && requestedUnit == null) {
                continue;
            }
            List<Project> projects = projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
                .eq(Project::getActivityId, assignment.getActivityId())
                .eq(Project::getCategoryId, assignment.getCategoryId())
                .eq(Project::getStatus, ArtReviewConstants.PROJECT_AUDIT_PASSED)
                .like(StringUtils.isNotBlank(safeQuery.getProjectName()), Project::getProjectName, safeQuery.getProjectName())
                .orderByDesc(Project::getSubmittedAt)
                .orderByDesc(Project::getCreateTime));
            for (Project project : projects) {
                if (!assignmentMatchesProject(assignment, project)) {
                    continue;
                }
                if (requestedUnit != null && !projectMatchesScopeUnit(project, requestedUnit)) {
                    continue;
                }
                ReviewTaskVo task = buildTaskVo(assignment, project, false);
                if (!matchesNavigatorFilter(task, safeQuery)) {
                    continue;
                }
                if ("none".equals(safeQuery.getScoreStatus()) && StringUtils.isNotBlank(task.getScoreStatus())) {
                    continue;
                }
                if (StringUtils.isNotBlank(safeQuery.getScoreStatus())
                    && !"none".equals(safeQuery.getScoreStatus())
                    && !Objects.equals(safeQuery.getScoreStatus(), task.getScoreStatus())) {
                    continue;
                }
                tasks.add(task);
            }
        }
        tasks.sort(Comparator.comparing(ReviewTaskVo::getSubmittedAt, Comparator.nullsLast(Date::compareTo)).reversed());
        return tasks;
    }

    private void fillReviewListDisplayFields(List<ReviewTaskVo> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        ArtDetailDisplayConfigVo.ListTablePageConfig tablePage = reviewListTablePage();
        if (tablePage == null || tablePage.getColumns() == null) {
            return;
        }
        Set<Long> projectIds = tasks.stream().map(ReviewTaskVo::getProjectId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> assignmentIds = tasks.stream().map(ReviewTaskVo::getAssignmentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> categoryIds = tasks.stream().map(ReviewTaskVo::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Project> projects = projectIds.isEmpty() ? Map.of() : projectMapper.selectBatchIds(projectIds).stream()
            .collect(Collectors.toMap(Project::getId, item -> item));
        Map<Long, ReviewAssignment> assignments = assignmentIds.isEmpty() ? Map.of() : assignmentMapper.selectBatchIds(assignmentIds).stream()
            .collect(Collectors.toMap(ReviewAssignment::getId, item -> item));
        List<CategoryFieldSchema> schemaRows = categoryIds.isEmpty() ? List.of() : fieldMapper.selectList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
            .in(CategoryFieldSchema::getCategoryId, categoryIds)
            .orderByAsc(CategoryFieldSchema::getSortOrder)
            .orderByAsc(CategoryFieldSchema::getId));
        Map<Long, Map<String, CategoryFieldSchema>> schemas = schemaRows.stream()
            .filter(item -> item.getCategoryId() != null && StringUtils.isNotBlank(item.getFieldKey()))
            .collect(Collectors.groupingBy(
                CategoryFieldSchema::getCategoryId,
                LinkedHashMap::new,
                Collectors.toMap(CategoryFieldSchema::getFieldKey, item -> item, (left, right) -> left, LinkedHashMap::new)
            ));

        for (ReviewTaskVo task : tasks) {
            List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> columns = reviewListColumnsForCategory(tablePage, task.getCategoryId());
            List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> formColumns = columns.stream()
                .filter(column -> !Boolean.TRUE.equals(column.getDeleted()))
                .filter(column -> Boolean.TRUE.equals(column.getVisible()))
                .filter(column -> "form".equals(column.getSource()) && StringUtils.isNotBlank(column.getFieldKey()))
                .toList();
            if (formColumns.isEmpty()) {
                task.setDisplayFields(Map.of());
                continue;
            }
            ReviewAssignment assignment = assignments.get(task.getAssignmentId());
            Project project = projects.get(task.getProjectId());
            if (assignment == null || project == null) {
                task.setDisplayFields(Map.of());
                continue;
            }
            Set<String> hiddenFields = parseStringSet(assignment.getHiddenFieldKeysJson());
            if (hiddenFields.contains(HIDE_ALL_FORM_FIELDS) || hiddenFields.containsAll(LEGACY_HIDE_ALL_FORM_FIELD_KEYS)) {
                task.setDisplayFields(Map.of());
                continue;
            }
            Map<String, CategoryFieldSchema> categorySchemas = schemas.getOrDefault(task.getCategoryId(), Map.of());
            Set<String> effectiveHiddenFields = new HashSet<>(hiddenFields);
            categorySchemas.values().stream()
                .filter(field -> Boolean.TRUE.equals(field.getSensitiveFlag()) || isSchoolIdentityField(field.getFieldKey(), field.getFieldLabel()))
                .map(CategoryFieldSchema::getFieldKey)
                .forEach(effectiveHiddenFields::add);
            if (hiddenFields.stream().anyMatch(TEACHER_FIELD_ALIASES::contains)) {
                categorySchemas.values().stream()
                    .filter(field -> "teacher_group".equals(field.getFieldType()))
                    .map(CategoryFieldSchema::getFieldKey)
                    .forEach(effectiveHiddenFields::add);
            }
            Map<String, Object> formData = parseObject(project.getFormDataJson());
            Map<String, String> displayFields = new LinkedHashMap<>();
            for (ArtDetailDisplayConfigVo.ReviewListColumnConfig column : formColumns) {
                String fieldKey = column.getFieldKey();
                if (!categorySchemas.containsKey(fieldKey) || effectiveHiddenFields.contains(fieldKey)) {
                    continue;
                }
                displayFields.put(fieldKey, reviewListValue(formData.get(fieldKey)));
            }
            task.setDisplayFields(displayFields);
        }
    }

    private List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> reviewListDisplayColumns(List<ReviewTaskVo> tasks, Long categoryId) {
        ArtDetailDisplayConfigVo.ListTablePageConfig tablePage = reviewListTablePage();
        List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> configured = reviewListColumnsForCategory(tablePage, categoryId).stream()
            .filter(column -> !Boolean.TRUE.equals(column.getDeleted()))
            .toList();
        if (categoryId == null) {
            return configured.stream().filter(column -> !"form".equals(column.getSource())).toList();
        }
        Map<String, CategoryFieldSchema> schemas = fieldMapper.selectList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
                .eq(CategoryFieldSchema::getCategoryId, categoryId)
                .orderByAsc(CategoryFieldSchema::getSortOrder)
                .orderByAsc(CategoryFieldSchema::getId)).stream()
            .filter(item -> StringUtils.isNotBlank(item.getFieldKey()))
            .collect(Collectors.toMap(CategoryFieldSchema::getFieldKey, item -> item, (left, right) -> left, LinkedHashMap::new));
        Set<Long> assignmentIds = tasks.stream().map(ReviewTaskVo::getAssignmentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ReviewAssignment> assignments = assignmentIds.isEmpty() ? Map.of() : assignmentMapper.selectBatchIds(assignmentIds).stream()
            .collect(Collectors.toMap(ReviewAssignment::getId, item -> item));
        return configured.stream().filter(column -> {
            if (!"form".equals(column.getSource())) {
                return true;
            }
            CategoryFieldSchema schema = schemas.get(column.getFieldKey());
            if (schema == null || Boolean.TRUE.equals(schema.getSensitiveFlag()) || isSchoolIdentityField(schema.getFieldKey(), schema.getFieldLabel())) {
                return false;
            }
            return tasks.stream().map(task -> assignments.get(task.getAssignmentId())).filter(Objects::nonNull).anyMatch(assignment -> {
                Set<String> hidden = parseStringSet(assignment.getHiddenFieldKeysJson());
                if (hidden.contains(HIDE_ALL_FORM_FIELDS) || hidden.containsAll(LEGACY_HIDE_ALL_FORM_FIELD_KEYS) || hidden.contains(column.getFieldKey())) {
                    return false;
                }
                return !("teacher_group".equals(schema.getFieldType()) && hidden.stream().anyMatch(TEACHER_FIELD_ALIASES::contains));
            });
        }).toList();
    }

    private List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> reviewListColumnsForCategory(
        ArtDetailDisplayConfigVo.ListTablePageConfig tablePage,
        Long categoryId
    ) {
        if (tablePage == null) {
            return List.of();
        }
        if (categoryId != null && tablePage.getCategoryColumns() != null) {
            List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> override = tablePage.getCategoryColumns().get(String.valueOf(categoryId));
            if (override != null) {
                return override;
            }
        }
        return tablePage.getColumns() == null ? List.of() : tablePage.getColumns();
    }

    private ArtDetailDisplayConfigVo.ListTablePageConfig reviewListTablePage() {
        ArtDetailDisplayConfigVo config = displayConfigService.getConfig();
        if (config.getListTableLayout() == null || config.getListTableLayout().getPages() == null) {
            return null;
        }
        return config.getListTableLayout().getPages().get("review");
    }

    private String reviewListValue(Object value) {
        if (value == null) {
            return "-";
        }
        String textValue = value instanceof String ? (String) value : JsonUtils.toJsonString(value);
        if (StringUtils.isBlank(textValue)) {
            return "-";
        }
        String normalized = textValue.trim();
        return normalized.length() <= 300 ? normalized : normalized.substring(0, 300) + "…";
    }

    private List<ReviewTaskNavigatorVo.CountOption> countOptions(List<ReviewTaskVo> tasks,
                                                                   java.util.function.Function<ReviewTaskVo, Object> id,
                                                                   java.util.function.Function<ReviewTaskVo, String> label) {
        Map<String, Long> counts = new LinkedHashMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        for (ReviewTaskVo task : tasks) {
            Object rawId = id.apply(task);
            String rawLabel = label.apply(task);
            if (rawId == null || StringUtils.isBlank(rawLabel)) {
                continue;
            }
            String key = String.valueOf(rawId);
            counts.merge(key, 1L, Long::sum);
            labels.putIfAbsent(key, rawLabel);
        }
        return counts.entrySet().stream()
            .map(entry -> new ReviewTaskNavigatorVo.CountOption(entry.getKey(), labels.get(entry.getKey()), entry.getValue()))
            .sorted(Comparator.comparing(ReviewTaskNavigatorVo.CountOption::getLabel, Comparator.nullsLast(String::compareTo)))
            .toList();
    }

    private List<ReviewTaskNavigatorVo.ScopeOption> buildCurrentReviewerScopeOptions(
        Long activityId,
        List<ReviewTaskVo> activityTasks
    ) {
        List<ReviewAssignment> assignments = currentUserAssignments(activityId, null);
        if (assignments.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ReviewTaskVo>> tasksByAssignment = activityTasks.stream()
            .filter(task -> task.getAssignmentId() != null)
            .collect(Collectors.groupingBy(ReviewTaskVo::getAssignmentId));
        Map<Long, ActivityCategory> categories = categoryMapper.selectBatchIds(assignments.stream()
                .map(ReviewAssignment::getCategoryId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left));
        List<ReviewTaskNavigatorVo.ScopeOption> result = new ArrayList<>();
        for (ReviewAssignment assignment : assignments) {
            ActivityCategory category = categories.get(assignment.getCategoryId());
            List<ReviewTaskVo> tasks = tasksByAssignment.getOrDefault(assignment.getId(), List.of());
            if (isWholeCategoryAssignment(assignment) || category == null) {
                result.add(buildScopeOption(assignment, category, null, assignmentScopeLabel(assignment, category), true, tasks));
                continue;
            }
            ReviewAssignmentCategoryConfig config = assignmentCategoryConfig(category);
            Map<Long, Project> projects = projectMapper.selectBatchIds(tasks.stream()
                    .map(ReviewTaskVo::getProjectId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Project::getId, item -> item, (left, right) -> left));
            List<ReviewAssignmentScopeUnitVo> units = buildAssignmentScopeUnits(category, config.getScopeFieldKeys());
            boolean emitted = false;
            for (ReviewAssignmentScopeUnitVo unit : units) {
                if (!assignmentSelectsScopeUnit(assignment, unit, config.getScopeFieldKeys())) {
                    continue;
                }
                List<ReviewTaskVo> unitTasks = tasks.stream()
                    .filter(task -> projectMatchesScopeUnit(projects.get(task.getProjectId()), unit))
                    .toList();
                result.add(buildScopeOption(
                    assignment,
                    category,
                    unit.getUnitKey(),
                    StringUtils.blankToDefault(category.getCategoryName(), "未命名类别") + " > " + unitScopeLabel(unit),
                    false,
                    unitTasks
                ));
                emitted = true;
            }
            if (!emitted) {
                result.add(buildScopeOption(assignment, category, null, assignmentScopeLabel(assignment, category), false, tasks));
            }
        }
        result.sort(Comparator
            .comparing(ReviewTaskNavigatorVo.ScopeOption::getCategoryName, Comparator.nullsLast(String::compareTo))
            .thenComparing(ReviewTaskNavigatorVo.ScopeOption::getAssignmentId, Comparator.nullsLast(Long::compareTo)));
        return result;
    }

    private ReviewTaskNavigatorVo.ScopeOption buildScopeOption(
        ReviewAssignment assignment,
        ActivityCategory category,
        String unitKey,
        String label,
        boolean wholeCategory,
        List<ReviewTaskVo> tasks
    ) {
        long completed = tasks.stream().filter(this::isTaskScoreComplete).count();
        long locked = tasks.stream()
            .filter(task -> ArtReviewConstants.REVIEW_SCORE_SUBMITTED.equals(task.getScoreStatus())).count();
        ReviewTaskNavigatorVo.ScopeOption option = new ReviewTaskNavigatorVo.ScopeOption();
        option.setScopeKey(assignmentScopeKey(assignment.getId(), unitKey));
        option.setAssignmentId(assignment.getId());
        option.setCategoryId(assignment.getCategoryId());
        option.setCategoryName(category == null ? null : category.getCategoryName());
        option.setScopeLabel(label);
        option.setWholeCategory(wholeCategory);
        option.setTotalCount((long) tasks.size());
        option.setCompletedCount(completed);
        option.setLockedCount(locked);
        option.setPendingCount(Math.max(0L, tasks.size() - completed));
        return option;
    }

    private String assignmentScopeKey(Long assignmentId, String unitKey) {
        if (assignmentId == null) {
            return null;
        }
        if (unitKey == null) {
            return "assignment:" + assignmentId + ":all";
        }
        String encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(unitKey.getBytes(StandardCharsets.UTF_8));
        return "assignment:" + assignmentId + ":unit:" + encoded;
    }

    private AssignmentScopeKey parseAssignmentScopeKey(String scopeKey) {
        if (StringUtils.isBlank(scopeKey)) {
            return null;
        }
        String[] parts = scopeKey.split(":", 4);
        if (parts.length < 3 || !"assignment".equals(parts[0])) {
            return null;
        }
        try {
            Long assignmentId = Long.valueOf(parts[1]);
            if ("all".equals(parts[2]) && parts.length == 3) {
                return new AssignmentScopeKey(assignmentId, null);
            }
            if ("unit".equals(parts[2]) && parts.length == 4) {
                String unitKey = new String(Base64.getUrlDecoder().decode(parts[3]), StandardCharsets.UTF_8);
                return new AssignmentScopeKey(assignmentId, unitKey);
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid opaque scope keys must narrow to no tasks.
        }
        return null;
    }

    private ReviewAssignmentScopeUnitVo findAssignmentScopeUnit(ReviewAssignment assignment, String unitKey) {
        if (assignment == null || StringUtils.isBlank(unitKey)) {
            return null;
        }
        ActivityCategory category = categoryMapper.selectById(assignment.getCategoryId());
        if (category == null) {
            return null;
        }
        ReviewAssignmentCategoryConfig config = assignmentCategoryConfig(category);
        return buildAssignmentScopeUnits(category, config.getScopeFieldKeys()).stream()
            .filter(item -> Objects.equals(item.getUnitKey(), unitKey))
            .findFirst().orElse(null);
    }

    private boolean projectMatchesScopeUnit(Project project, ReviewAssignmentScopeUnitVo unit) {
        if (project == null || unit == null) {
            return false;
        }
        return unit.getFieldValues().entrySet().stream()
            .allMatch(entry -> matchesAnyFilterValue(projectFieldValue(project, entry.getKey()), List.of(entry.getValue())));
    }

    private boolean assignmentSelectsScopeUnit(
        ReviewAssignment assignment,
        ReviewAssignmentScopeUnitVo unit,
        List<String> scopeFieldKeys
    ) {
        ReviewAssignmentProjectFilter filter = parseProjectFilter(assignment.getProjectFilterJson());
        if (!filter.getScopeGroups().isEmpty()) {
            return filter.getScopeGroups().stream()
                .anyMatch(group -> scopeConditionSelectsUnit(group.getFieldValues(), unit, scopeFieldKeys));
        }
        return scopeConditionSelectsUnit(filter.getFieldValues(), unit, scopeFieldKeys);
    }

    private boolean scopeConditionSelectsUnit(
        Map<String, List<String>> condition,
        ReviewAssignmentScopeUnitVo unit,
        List<String> scopeFieldKeys
    ) {
        if (condition == null || condition.isEmpty() || scopeFieldKeys == null || scopeFieldKeys.isEmpty()) {
            return true;
        }
        for (String fieldKey : scopeFieldKeys) {
            List<String> acceptedValues = condition.get(fieldKey);
            if (acceptedValues == null || acceptedValues.isEmpty()) {
                continue;
            }
            String unitValue = unit.getFieldValues().get(fieldKey);
            if (!acceptedValues.contains(unitValue)) {
                return false;
            }
        }
        return true;
    }

    private String unitScopeLabel(ReviewAssignmentScopeUnitVo unit) {
        if (unit == null || unit.getFieldValues() == null || unit.getFieldValues().isEmpty()) {
            return "全部项目";
        }
        if (unit.getFieldValues().size() == 1) {
            return scopeDisplayValues(List.of(unit.getFieldValues().values().iterator().next()));
        }
        return StringUtils.blankToDefault(unit.getPathLabel(), "部分项目");
    }

    private boolean isWholeCategoryAssignment(ReviewAssignment assignment) {
        ReviewAssignmentProjectFilter filter = parseProjectFilter(assignment.getProjectFilterJson());
        return AssignmentSchoolScopeSupport.parseSchoolIds(assignment.getSchoolIdsJson()).isEmpty()
            && filter.getSchoolTypes().isEmpty()
            && filter.getFieldValues().isEmpty()
            && filter.getScopeGroups().isEmpty()
            && parseProjectIdSet(assignment.getProjectIdsJson()).isEmpty()
            && parseProjectIdSet(assignment.getExcludedProjectIdsJson()).isEmpty();
    }

    private String assignmentScopeLabel(ReviewAssignment assignment, ActivityCategory category) {
        String categoryName = category == null
            ? "未命名类别"
            : StringUtils.blankToDefault(category.getCategoryName(), "未命名类别");
        if (isWholeCategoryAssignment(assignment)) {
            boolean grouped = category != null && !assignmentCategoryConfig(category).getScopeFieldKeys().isEmpty();
            return categoryName + (grouped ? " 全部组" : " 全部项目");
        }
        ReviewAssignmentProjectFilter filter = parseProjectFilter(assignment.getProjectFilterJson());
        List<Map<String, List<String>>> conditions = new ArrayList<>();
        if (!filter.getFieldValues().isEmpty()) {
            conditions.add(filter.getFieldValues());
        }
        filter.getScopeGroups().stream()
            .map(ReviewAssignmentProjectScopeGroup::getFieldValues)
            .filter(values -> values != null && !values.isEmpty())
            .forEach(conditions::add);
        if (conditions.isEmpty()) {
            return categoryName + " 部分项目";
        }
        List<String> scopeFieldKeys = category == null
            ? List.of()
            : assignmentCategoryConfig(category).getScopeFieldKeys();
        List<String> labels = conditions.stream()
            .map(values -> assignmentConditionLabel(values, scopeFieldKeys))
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        return labels.isEmpty() ? categoryName + " 部分项目" : categoryName + " > " + String.join("、", labels);
    }

    private String assignmentConditionLabel(Map<String, List<String>> values, List<String> scopeFieldKeys) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (scopeFieldKeys != null && scopeFieldKeys.size() == 1 && values.size() == 1
            && values.containsKey(scopeFieldKeys.get(0))) {
            return scopeDisplayValues(values.get(scopeFieldKeys.get(0)));
        }
        return values.entrySet().stream()
            .map(entry -> entry.getKey() + "：" + scopeDisplayValues(entry.getValue()))
            .collect(Collectors.joining(" / "));
    }

    private String scopeDisplayValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "未填写";
        }
        return values.stream()
            .map(value -> FILTER_UNFILLED_VALUE.equals(value) ? "未填写" : value)
            .collect(Collectors.joining("、"));
    }

    private boolean matchesNavigatorFilter(ReviewTaskVo task, ReviewTaskVo query) {
        return (StringUtils.isBlank(query.getProgramForm()) || Objects.equals(query.getProgramForm(), task.getProgramForm()))
            && (StringUtils.isBlank(query.getGroupOrNature()) || Objects.equals(query.getGroupOrNature(), task.getGroupOrNature()));
    }

    private Set<Long> parseIdSet(String ids) {
        if (StringUtils.isBlank(ids)) {
            return Set.of();
        }
        Set<Long> result = new LinkedHashSet<>();
        for (String text : ids.split(",")) {
            try {
                result.add(Long.parseLong(text.trim()));
            } catch (NumberFormatException ignored) {
                // Ignore malformed route/query values instead of broadening the result set.
            }
        }
        return result;
    }

    private List<ReviewWorkbenchVo.GroupItem> buildReviewWorkbenchGroups(List<ReviewTaskVo> tasks) {
        Map<String, List<ReviewTaskVo>> grouped = tasks.stream()
            .collect(Collectors.groupingBy(task -> String.valueOf(task.getCategoryId()), LinkedHashMap::new, Collectors.toList()));
        List<ReviewWorkbenchVo.GroupItem> rows = new ArrayList<>();
        for (Map.Entry<String, List<ReviewTaskVo>> entry : grouped.entrySet()) {
            List<ReviewTaskVo> groupTasks = entry.getValue();
            ReviewWorkbenchVo.GroupItem item = new ReviewWorkbenchVo.GroupItem();
            item.setGroupKey(entry.getKey());
            item.setGroupName(groupTasks.stream().map(ReviewTaskVo::getCategoryName).filter(StringUtils::isNotBlank).findFirst().orElse("其他类别"));
            item.setTotalCount((long) groupTasks.size());
            item.setPendingCount(groupTasks.stream().filter(task -> StringUtils.isBlank(task.getScoreStatus())).count());
            item.setDraftCount(groupTasks.stream().filter(task -> ArtReviewConstants.REVIEW_SCORE_DRAFT.equals(task.getScoreStatus())).count());
            item.setSubmittedCount(groupTasks.stream().filter(task -> ArtReviewConstants.REVIEW_SCORE_SUBMITTED.equals(task.getScoreStatus())).count());
            rows.add(item);
        }
        return rows;
    }

    @Override
    public ReviewTaskVo getMyTaskDetail(Long assignmentId, Long projectId) {
        ReviewAssignment assignment = requireCurrentUserAssignment(assignmentId);
        Project project = requireAssignableProject(assignment, projectId);
        return buildTaskVo(assignment, project, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreVo saveScoreDraft(ReviewScoreBo bo) {
        return saveScore(bo, ArtReviewConstants.REVIEW_SCORE_DRAFT, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreVo submitScore(ReviewScoreBo bo) {
        // Compatibility endpoint: the category signature is now the only final
        // submission boundary. Saving one work must remain editable until then.
        return saveScore(bo, ArtReviewConstants.REVIEW_SCORE_DRAFT, true);
    }

    @Override
    public List<ReviewScoreVo> listProjectScores(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        List<ReviewScoreVo> rows = scoreMapper.selectVoList(Wrappers.lambdaQuery(ReviewScore.class)
            .eq(ReviewScore::getProjectId, projectId)
            .orderByDesc(ReviewScore::getSubmittedAt)
            .orderByDesc(ReviewScore::getCreateTime));
        fillScoreReviewerNames(rows);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreVo adjustScore(ReviewScoreAdminBo bo) {
        if (bo.getScoreId() == null) {
            throw new ServiceException("评分ID不能为空");
        }
        if (StringUtils.isBlank(bo.getReason())) {
            throw new ServiceException("调整原因必填");
        }
        ReviewScore score = requireScore(bo.getScoreId());
        ensureScoreNotSigned(score);
        ReviewScore before = copyScoreSnapshot(score);
        ReviewAssignment assignment = requireAssignment(score.getAssignmentId());
        ReviewScoreBo validateBo = new ReviewScoreBo();
        validateBo.setAssignmentId(score.getAssignmentId());
        validateBo.setProjectId(score.getProjectId());
        validateBo.setScoreValue(bo.getScoreValue());
        validateBo.setGradeValue(bo.getGradeValue());
        validateBo.setCommentText(bo.getCommentText());
        validateScoreValue(assignment, validateBo, ArtReviewConstants.REVIEW_SCORE_SUBMITTED);
        score.setScoreValue(scaleScore(bo.getScoreValue()));
        score.setGradeValue(StringUtils.trim(bo.getGradeValue()));
        score.setCommentText(StringUtils.trim(bo.getCommentText()));
        score.setStatus(ArtReviewConstants.REVIEW_SCORE_SUBMITTED);
        if (score.getSubmittedAt() == null) {
            score.setSubmittedAt(new Date());
        }
        scoreMapper.updateById(score);
        markResultDraft(score.getProjectId());
        logScoreAction(score, ArtReviewConstants.RESULT_LOG_ACTION_SCORE_ADJUST, before, score, bo.getReason());
        ReviewScoreVo vo = scoreMapper.selectVoById(score.getId());
        fillScoreReviewerNames(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreVo returnScore(ReviewScoreAdminBo bo) {
        if (bo.getScoreId() == null) {
            throw new ServiceException("评分ID不能为空");
        }
        if (StringUtils.isBlank(bo.getReason())) {
            throw new ServiceException("退回原因必填");
        }
        ReviewScore score = requireScore(bo.getScoreId());
        ensureScoreNotSigned(score);
        ReviewScore before = copyScoreSnapshot(score);
        score.setStatus(ArtReviewConstants.REVIEW_SCORE_DRAFT);
        score.setSubmittedAt(null);
        scoreMapper.updateById(score);
        markResultDraft(score.getProjectId());
        logScoreAction(score, ArtReviewConstants.RESULT_LOG_ACTION_SCORE_RETURN, before, score, bo.getReason());
        ReviewScoreVo vo = scoreMapper.selectVoById(score.getId());
        fillScoreReviewerNames(List.of(vo));
        return vo;
    }

    private ReviewScoreVo saveScore(ReviewScoreBo bo, String targetStatus, boolean requireCompleteScore) {
        if (bo.getAssignmentId() == null || bo.getProjectId() == null) {
            throw new ServiceException("评审分配ID和项目ID不能为空");
        }
        ReviewAssignment assignment = requireCurrentUserAssignment(bo.getAssignmentId());
        Project project = requireAssignableProject(assignment, bo.getProjectId());
        validateScoreValue(assignment, bo,
            requireCompleteScore ? ArtReviewConstants.REVIEW_SCORE_SUBMITTED : targetStatus);
        ReviewScore score = scoreMapper.selectOne(Wrappers.lambdaQuery(ReviewScore.class)
            .eq(ReviewScore::getAssignmentId, assignment.getId())
            .eq(ReviewScore::getProjectId, project.getId())
            .eq(ReviewScore::getReviewerUserId, LoginHelper.getUserId())
            .last("limit 1"));
        if (score != null && hasActiveSignedSheetItem(score.getId())) {
            throw new ServiceException("该类别已提交签字，评分已锁定");
        }
        if (ArtReviewConstants.REVIEW_SCORE_SUBMITTED.equals(targetStatus)
            && ArtReviewConstants.REVIEW_EXCLUSIVE_SINGLE.equals(assignment.getExclusiveMode())) {
            ensureNotLockedByOther(assignment, project.getId());
        }
        if (score == null) {
            score = new ReviewScore();
            score.setAssignmentId(assignment.getId());
            score.setProjectId(project.getId());
            score.setReviewerUserId(LoginHelper.getUserId());
        }
        score.setScoreValue(scaleScore(bo.getScoreValue()));
        score.setGradeValue(StringUtils.trim(bo.getGradeValue()));
        score.setCommentText(StringUtils.trim(bo.getCommentText()));
        score.setStatus(targetStatus);
        if (ArtReviewConstants.REVIEW_SCORE_SUBMITTED.equals(targetStatus)) {
            score.setSubmittedAt(new Date());
        } else {
            score.setSubmittedAt(null);
        }
        if (score.getId() == null) {
            scoreMapper.insert(score);
        } else {
            scoreMapper.updateById(score);
        }
        ReviewScoreVo vo = scoreMapper.selectVoById(score.getId());
        fillScoreReviewerNames(List.of(vo));
        return vo;
    }

    private void ensureScoreNotSigned(ReviewScore score) {
        if (score != null && hasActiveSignedSheetItem(score.getId())) {
            throw new ServiceException("该评分已进入有效签字表，请先撤回类别签字");
        }
    }

    private boolean hasActiveSignedSheetItem(Long scoreId) {
        return scoreId != null && signedSheetItemMapper.selectCount(Wrappers.lambdaQuery(ReviewScoreSheetSignedSheetItem.class)
            .eq(ReviewScoreSheetSignedSheetItem::getReviewScoreId, scoreId)
            .eq(ReviewScoreSheetSignedSheetItem::getActiveMarker, 1)) > 0;
    }

    private ReviewScore requireScore(Long scoreId) {
        ReviewScore score = scoreMapper.selectById(scoreId);
        if (score == null) {
            throw new ServiceException("评分记录不存在");
        }
        return score;
    }

    private ReviewScore copyScoreSnapshot(ReviewScore source) {
        ReviewScore snapshot = new ReviewScore();
        snapshot.setId(source.getId());
        snapshot.setAssignmentId(source.getAssignmentId());
        snapshot.setProjectId(source.getProjectId());
        snapshot.setReviewerUserId(source.getReviewerUserId());
        snapshot.setScoreValue(source.getScoreValue());
        snapshot.setGradeValue(source.getGradeValue());
        snapshot.setCommentText(source.getCommentText());
        snapshot.setStatus(source.getStatus());
        snapshot.setSubmittedAt(source.getSubmittedAt());
        return snapshot;
    }

    private BigDecimal scaleScore(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private void markResultDraft(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }
        List<ReviewResult> results = resultMapper.selectList(Wrappers.lambdaQuery(ReviewResult.class)
            .eq(ReviewResult::getActivityId, project.getActivityId())
            .eq(ReviewResult::getCategoryId, project.getCategoryId()));
        for (ReviewResult result : results) {
            if (ArtReviewConstants.RESULT_PUBLISHED.equals(result.getResultStatus())) {
                result.setResultStatus(ArtReviewConstants.RESULT_DRAFT);
                result.setPublishedBy(null);
                result.setPublishedAt(null);
                resultMapper.updateById(result);
            }
        }
    }

    private void logScoreAction(ReviewScore score, String actionType, ReviewScore before, ReviewScore after, String reason) {
        ReviewAssignment assignment = assignmentMapper.selectById(score.getAssignmentId());
        Project project = projectMapper.selectById(score.getProjectId());
        ReviewResultLog log = new ReviewResultLog();
        log.setActivityId(project == null ? (assignment == null ? null : assignment.getActivityId()) : project.getActivityId());
        log.setCategoryId(project == null ? (assignment == null ? null : assignment.getCategoryId()) : project.getCategoryId());
        log.setProjectId(score.getProjectId());
        log.setScoreId(score.getId());
        log.setTargetType(ArtReviewConstants.RESULT_LOG_TARGET_SCORE);
        log.setActionType(actionType);
        log.setBeforeJson(JsonUtils.toJsonString(before));
        log.setAfterJson(JsonUtils.toJsonString(after));
        log.setReason(StringUtils.trim(reason));
        log.setOperatedBy(LoginHelper.getUserId());
        log.setOperatedAt(new Date());
        resultLogMapper.insert(log);
    }

    private void validateAssignmentBo(ReviewAssignmentBo bo) {
        if (bo == null) {
            throw new ServiceException("评审分配不能为空");
        }
        validateAssignmentBo(bo, bo.getCategoryId(), bo.getReviewerUserId());
    }

    private void validateAssignmentBo(ReviewAssignmentBo bo, Long categoryId, Long reviewerUserId) {
        if (bo.getActivityId() == null || categoryId == null || reviewerUserId == null) {
            throw new ServiceException("活动、类别和专家不能为空");
        }
        Activity activity = activityMapper.selectById(bo.getActivityId());
        ActivityCategory category = categoryMapper.selectById(categoryId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        if (category == null || !Objects.equals(category.getActivityId(), bo.getActivityId())) {
            throw new ServiceException("类别不属于当前活动");
        }
        SysUser reviewer = sysUserMapper.selectById(reviewerUserId);
        if (reviewer == null || !"0".equals(reviewer.getStatus())) {
            throw new ServiceException("专家账号不存在或已停用");
        }
        if (!hasReviewerPermissions(reviewer.getUserId())) {
            throw new ServiceException("专家账号必须具备评审任务和评分权限");
        }
        if (StringUtils.isNotBlank(bo.getScoreRuleJson()) && !JsonUtils.isJsonObject(bo.getScoreRuleJson())) {
            throw new ServiceException("评分规则必须是 JSON 对象");
        }
        if (StringUtils.isNotBlank(bo.getHiddenFieldKeysJson()) && !JsonUtils.isJsonArray(bo.getHiddenFieldKeysJson())) {
            throw new ServiceException("隐藏字段必须是 JSON 数组");
        }
        ReviewAssignmentProjectFilter projectFilter = parseProjectFilter(bo.getProjectFilterJson());
        validateProjectFilter(projectFilter, categoryId);
        validateHiddenFileRequirements(bo, categoryId);
        if (StringUtils.isNotBlank(bo.getScoreVisibilityPolicy())) {
            String policy = bo.getScoreVisibilityPolicy();
            if (!ArtReviewConstants.REVIEW_SCORE_VISIBILITY_AFTER_SUBMIT.equals(policy)
                && !ArtReviewConstants.REVIEW_SCORE_VISIBILITY_HIDDEN.equals(policy)
                && !ArtReviewConstants.REVIEW_SCORE_VISIBILITY_ALWAYS.equals(policy)) {
                throw new ServiceException("他人评分可见策略不正确");
            }
        }
        Set<Long> projectIds = parseProjectIdSet(bo.getProjectIdsJson());
        Set<Long> excludedProjectIds = parseProjectIdSet(bo.getExcludedProjectIdsJson());
        if (!projectIds.isEmpty() && !excludedProjectIds.isEmpty()) {
            throw new ServiceException("指定作品和排除作品不能同时配置");
        }
        if (!projectIds.isEmpty()) {
            List<Project> projects = projectMapper.selectBatchIds(projectIds);
            if (projects.size() != projectIds.size()) {
                throw new ServiceException("指定项目中包含不可用项目");
            }
            boolean mismatchedScope = projects.stream().anyMatch(project ->
                !Objects.equals(project.getActivityId(), bo.getActivityId())
                    || !Objects.equals(project.getCategoryId(), categoryId)
                    || !ArtReviewConstants.PROJECT_AUDIT_PASSED.equals(project.getStatus())
                    || !AssignmentSchoolScopeSupport.matches(project.getSchoolId(), bo.getSchoolScopeMode(), bo.getSchoolIdsJson())
                    || !projectMatchesFilter(project, projectFilter, null));
            if (mismatchedScope) {
                throw new ServiceException("指定项目必须是所选活动、类别和学校范围内的已审核通过项目");
            }
        }
        if (!excludedProjectIds.isEmpty()) {
            List<Project> excludedProjects = projectMapper.selectBatchIds(excludedProjectIds);
            if (excludedProjects.size() != excludedProjectIds.size()
                || excludedProjects.stream().anyMatch(project ->
                    !Objects.equals(project.getActivityId(), bo.getActivityId())
                        || !Objects.equals(project.getCategoryId(), categoryId))) {
                throw new ServiceException("排除作品必须属于所选活动和类别");
            }
        }
        validateSchoolScope(bo.getSchoolScopeMode(), bo.getSchoolIdsJson());
    }

    private void validateSchoolScope(String mode, String schoolIdsJson) {
        String normalizedMode = AssignmentSchoolScopeSupport.normalizeMode(mode);
        String normalizedSchoolIdsJson = AssignmentSchoolScopeSupport.normalizeSchoolIdsJson(normalizedMode, schoolIdsJson);
        Set<Long> schoolIds = AssignmentSchoolScopeSupport.parseSchoolIds(normalizedSchoolIdsJson);
        if (schoolIds.isEmpty()) {
            return;
        }
        long enabledCount = schoolInfoMapper.selectCount(Wrappers.lambdaQuery(org.dromara.crehn.domain.SchoolInfo.class)
            .in(org.dromara.crehn.domain.SchoolInfo::getId, schoolIds)
            .eq(org.dromara.crehn.domain.SchoolInfo::getStatus, "enabled"));
        if (enabledCount != schoolIds.size()) {
            throw new ServiceException("学校范围包含不存在或已停用的学校");
        }
    }

    private void validateProjectFilter(ReviewAssignmentProjectFilter filter, Long categoryId) {
        if (filter.getFieldValues().isEmpty() && filter.getScopeGroups().isEmpty()) {
            return;
        }
        Map<String, CategoryFieldSchema> schemaMap = fieldMapper.selectList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
                .eq(CategoryFieldSchema::getCategoryId, categoryId))
            .stream()
            .filter(field -> StringUtils.isNotBlank(field.getFieldKey()))
            .collect(Collectors.toMap(CategoryFieldSchema::getFieldKey, field -> field, (left, right) -> left));
        validateProjectFilterFields(filter.getFieldValues(), schemaMap);
        for (ReviewAssignmentProjectScopeGroup group : filter.getScopeGroups()) {
            validateProjectFilterFields(group.getFieldValues(), schemaMap);
        }
    }

    private void validateProjectFilterFields(
        Map<String, List<String>> fieldValues,
        Map<String, CategoryFieldSchema> schemaMap
    ) {
        for (Map.Entry<String, List<String>> entry : fieldValues.entrySet()) {
            CategoryFieldSchema schema = schemaMap.get(entry.getKey());
            if (schema == null || !isProjectFilterField(schema)) {
                throw new ServiceException("项目筛选标签不属于当前类别：" + entry.getKey());
            }
            Set<String> allowedValues = parseStringSet(schema.getOptionsJson());
            Set<String> expectedValues = entry.getValue().stream()
                .filter(value -> !FILTER_UNFILLED_VALUE.equals(value))
                .collect(Collectors.toSet());
            if (allowedValues.isEmpty() || !allowedValues.containsAll(expectedValues)) {
                throw new ServiceException("项目筛选标签值不正确：" + schema.getFieldLabel());
            }
        }
    }

    private boolean isProjectFilterField(CategoryFieldSchema field) {
        return field != null && Set.of("select", "radio", "checkbox").contains(StringUtils.blankToDefault(field.getFieldType(), ""));
    }

    private void validateHiddenFileRequirements(ReviewAssignmentBo bo, Long categoryId) {
        Set<String> keys = parseStringSet(bo.getHiddenFieldKeysJson());
        Set<Long> hiddenRequirementIds = hiddenFileRequirementIds(keys);
        long prefixedKeyCount = keys.stream().filter(key -> key.startsWith(HIDDEN_FILE_REQUIREMENT_PREFIX)).count();
        if (prefixedKeyCount != hiddenRequirementIds.size()) {
            throw new ServiceException("隐藏附件配置不正确");
        }
        if (hiddenRequirementIds.isEmpty()) {
            return;
        }
        Set<Long> allowedCategoryIds = bo.getCategoryIds() == null || bo.getCategoryIds().isEmpty()
            ? Set.of(categoryId)
            : new HashSet<>(bo.getCategoryIds());
        List<CategoryFileRequirement> requirements = fileRequirementMapper.selectBatchIds(hiddenRequirementIds);
        if (requirements.size() != hiddenRequirementIds.size()
            || requirements.stream().anyMatch(item -> !allowedCategoryIds.contains(item.getCategoryId()))) {
            throw new ServiceException("隐藏附件必须属于当前评分分配类别");
        }
    }

    private String normalizeHiddenFieldKeysJson(String text, Long categoryId) {
        ReviewAssignmentBo validation = new ReviewAssignmentBo();
        validation.setCategoryId(categoryId);
        validation.setCategoryIds(List.of(categoryId));
        validation.setHiddenFieldKeysJson(text);
        validateHiddenFileRequirements(validation, categoryId);
        Set<String> keys = parseStringSet(text);
        return keys.isEmpty() ? null : JsonUtils.toJsonString(new ArrayList<>(keys));
    }

    private String hiddenFieldKeysForCategory(ReviewAssignmentBo bo, Long categoryId) {
        Set<String> keys = parseStringSet(bo.getHiddenFieldKeysJson());
        List<String> result = keys.stream()
            .filter(key -> !key.startsWith(HIDDEN_FILE_REQUIREMENT_PREFIX)
                || hiddenFileRequirementIds(Set.of(key)).stream().anyMatch(id -> requirementBelongsToCategory(id, categoryId)))
            .toList();
        return result.isEmpty() ? null : JsonUtils.toJsonString(result);
    }

    private boolean requirementBelongsToCategory(Long requirementId, Long categoryId) {
        CategoryFileRequirement requirement = fileRequirementMapper.selectById(requirementId);
        return requirement != null && Objects.equals(requirement.getCategoryId(), categoryId);
    }

    private Set<Long> hiddenFileRequirementIds(Set<String> keys) {
        Set<Long> result = new LinkedHashSet<>();
        for (String key : keys) {
            if (!key.startsWith(HIDDEN_FILE_REQUIREMENT_PREFIX)) {
                continue;
            }
            try {
                result.add(Long.parseLong(key.substring(HIDDEN_FILE_REQUIREMENT_PREFIX.length())));
            } catch (NumberFormatException ignored) {
                // validation compares the number of prefixed keys and rejects malformed values
            }
        }
        return result;
    }

    private AssignmentBatchPreviewVo executeAssignmentBatch(ReviewAssignmentBo bo, boolean apply) {
        if (bo == null) {
            throw new ServiceException("批量维护参数不能为空");
        }
        String operation = normalizeBatchOperation(bo.getBatchOperation());
        String target = normalizeBatchTarget(bo.getBatchTarget());
        List<Long> reviewerUserIds = normalizeIds(bo.getReviewerUserIds());
        List<Long> categoryIds = normalizeIds(bo.getCategoryIds());
        if (bo.getActivityId() == null || reviewerUserIds.isEmpty() || categoryIds.isEmpty()) {
            throw new ServiceException("活动、至少一个类别和至少一个评审账号不能为空");
        }
        if (reviewerUserIds.size() > 100 || categoryIds.size() > 100 || (long) reviewerUserIds.size() * categoryIds.size() > 1000) {
            throw new ServiceException("一次最多生成1000条评分任务，请缩小账号或类别范围");
        }
        Set<Long> targetProjectIds = parseProjectIdSet(bo.getProjectIdsJson());
        if (categoryIds.size() > 1 && !targetProjectIds.isEmpty()) {
            throw new ServiceException("指定具体作品时只能选择一个类别");
        }
        if (ArtReviewConstants.ASSIGNMENT_BATCH_TARGET_PROJECT.equals(target)) {
            if (categoryIds.size() != 1 || targetProjectIds.isEmpty()) {
                throw new ServiceException("按项目维护时必须选择同一类别下的至少一个项目");
            }
            if (ArtReviewConstants.ASSIGNMENT_BATCH_SYNC.equals(operation)) {
                throw new ServiceException("按项目维护不支持同步类别，请使用添加或移除");
            }
            validateTargetProjects(bo.getActivityId(), categoryIds.get(0), targetProjectIds);
        }
        validateActivityAndCategories(bo.getActivityId(), categoryIds);
        reviewerUserIds.forEach(this::validateReviewer);
        if (!ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(operation)) {
            for (Long reviewerUserId : reviewerUserIds) {
                for (Long categoryId : categoryIds) {
                    validateAssignmentBo(bo, categoryId, reviewerUserId);
                }
            }
        }

        List<ReviewAssignment> existingRows = assignmentMapper.selectList(Wrappers.lambdaQuery(ReviewAssignment.class)
            .eq(ReviewAssignment::getActivityId, bo.getActivityId())
            .in(ReviewAssignment::getReviewerUserId, reviewerUserIds));
        Map<String, ReviewAssignment> existingMap = existingRows.stream()
            .collect(Collectors.toMap(
                row -> assignmentKey(row.getReviewerUserId(), row.getCategoryId()),
                row -> row,
                (left, right) -> left
            ));
        AssignmentBatchPreviewVo preview = new AssignmentBatchPreviewVo();
        preview.setOperation(operation);
        preview.setTarget(target);
        Date assignedAt = new Date();
        Long assignedBy = LoginHelper.getUserId();

        if (ArtReviewConstants.ASSIGNMENT_BATCH_SYNC.equals(operation)) {
            Set<Long> selectedCategoryIds = new HashSet<>(categoryIds);
            for (ReviewAssignment row : existingRows) {
                if (!selectedCategoryIds.contains(row.getCategoryId())
                    && ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE.equals(row.getStatus())) {
                    preview.setDisableCount(preview.getDisableCount() + 1);
                    addPreservedScoreCounts(preview, row.getId(), Set.of());
                    if (apply) {
                        row.setStatus(ArtReviewConstants.REVIEW_ASSIGNMENT_DISABLED);
                        assignmentMapper.updateById(row);
                    }
                }
            }
        }

        for (Long reviewerUserId : reviewerUserIds) {
            for (Long categoryId : categoryIds) {
                ReviewAssignment assignment = existingMap.get(assignmentKey(reviewerUserId, categoryId));
                if (ArtReviewConstants.ASSIGNMENT_BATCH_TARGET_PROJECT.equals(target)) {
                    maintainProjectScope(preview, bo, assignment, categoryId, reviewerUserId, targetProjectIds, operation, assignedBy, assignedAt, apply);
                    continue;
                }
                if (ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(operation)) {
                    if (assignment != null && ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE.equals(assignment.getStatus())) {
                        preview.setDisableCount(preview.getDisableCount() + 1);
                        addPreservedScoreCounts(preview, assignment.getId(), Set.of());
                        if (apply) {
                            assignment.setStatus(ArtReviewConstants.REVIEW_ASSIGNMENT_DISABLED);
                            assignmentMapper.updateById(assignment);
                        }
                    } else {
                        preview.setUnchangedCount(preview.getUnchangedCount() + 1);
                    }
                    continue;
                }
                if (assignment == null) {
                    preview.setCreateCount(preview.getCreateCount() + 1);
                    if (apply) {
                        assignment = new ReviewAssignment();
                        copyAssignment(assignment, bo, categoryId, reviewerUserId);
                        assignment.setExcludedProjectIdsJson(null);
                        assignment.setStatus(ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE);
                        assignment.setAssignedBy(assignedBy);
                        assignment.setAssignedAt(assignedAt);
                        assignmentMapper.insert(assignment);
                    }
                } else {
                    String effectiveProjectFilterJson = mergeBatchProjectFilterJson(assignment.getProjectFilterJson(), bo.getProjectFilterJson(), operation);
                    if (reviewConfigMatches(assignment, bo, categoryId, effectiveProjectFilterJson)) {
                        preview.setUnchangedCount(preview.getUnchangedCount() + 1);
                        continue;
                    }
                    validateScoreConfigMutable(assignment, bo);
                    preview.setUpdateCount(preview.getUpdateCount() + 1);
                    if (apply) {
                        copyAssignment(assignment, bo, categoryId, reviewerUserId, effectiveProjectFilterJson);
                        assignment.setExcludedProjectIdsJson(null);
                        assignment.setStatus(ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE);
                        assignment.setAssignedBy(assignedBy);
                        assignment.setAssignedAt(assignedAt);
                        assignmentMapper.updateById(assignment);
                    }
                }
            }
        }
        return preview;
    }

    private AssignmentBatchPreviewVo executeAssignmentScopePlan(ReviewAssignmentScopePlanBo plan, boolean apply) {
        if (plan == null || plan.getActivityId() == null) {
            throw new ServiceException("活动不能为空");
        }
        String operation = normalizeScopePlanOperation(plan.getOperation());
        List<Long> reviewerIds = normalizeIds(plan.getReviewerUserIds());
        List<ResolvedAssignmentScope> scopes = resolveAssignmentScopes(plan);
        if (reviewerIds.isEmpty() || scopes.isEmpty()) {
            throw new ServiceException("至少选择一个评审账号和一个分配范围");
        }
        if ((long) reviewerIds.size() * scopes.size() > 1000) {
            throw new ServiceException("一次最多维护1000条评分分配");
        }
        reviewerIds.forEach(this::validateReviewer);
        validateActivityAndCategories(plan.getActivityId(), scopes.stream()
            .map(ResolvedAssignmentScope::categoryId).toList());
        ReviewAssignmentBo template = scopePlanAssignmentBo(plan);
        if (!ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(operation)) {
            for (Long reviewerId : reviewerIds) {
                for (ResolvedAssignmentScope scope : scopes) {
                    template.setCategoryId(scope.categoryId());
                    template.setReviewerUserId(reviewerId);
                    template.setProjectFilterJson(scope.projectFilterJson());
                    validateAssignmentBo(template, scope.categoryId(), reviewerId);
                }
            }
        }

        List<ReviewAssignment> existingRows = assignmentMapper.selectList(Wrappers.lambdaQuery(ReviewAssignment.class)
            .eq(ReviewAssignment::getActivityId, plan.getActivityId())
            .in(ReviewAssignment::getReviewerUserId, reviewerIds));
        Map<String, ReviewAssignment> existingMap = existingRows.stream().collect(Collectors.toMap(
            row -> assignmentKey(row.getReviewerUserId(), row.getCategoryId()),
            row -> row,
            (left, right) -> left
        ));
        AssignmentBatchPreviewVo preview = new AssignmentBatchPreviewVo();
        preview.setOperation(operation);
        preview.setTarget("scope_plan");
        Long assignedBy = LoginHelper.getUserId();
        Date assignedAt = new Date();
        Set<Long> selectedCategoryIds = scopes.stream().map(ResolvedAssignmentScope::categoryId).collect(Collectors.toSet());

        if (SCOPE_PLAN_REPLACE.equals(operation)) {
            for (ReviewAssignment existing : existingRows) {
                if (!selectedCategoryIds.contains(existing.getCategoryId())
                    && ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE.equals(existing.getStatus())) {
                    registerScopeChange(preview, existing, null, true, apply);
                    preview.setDisableCount(preview.getDisableCount() + 1);
                    if (apply) {
                        existing.setStatus(ArtReviewConstants.REVIEW_ASSIGNMENT_DISABLED);
                        existing.setAssignedBy(assignedBy);
                        existing.setAssignedAt(assignedAt);
                        assignmentMapper.updateById(existing);
                    }
                }
            }
        }

        for (Long reviewerId : reviewerIds) {
            for (ResolvedAssignmentScope scope : scopes) {
                ReviewAssignment existing = existingMap.get(assignmentKey(reviewerId, scope.categoryId()));
                template.setCategoryId(scope.categoryId());
                template.setReviewerUserId(reviewerId);
                template.setProjectFilterJson(scope.projectFilterJson());
                if (existing == null) {
                    if (ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(operation)) {
                        preview.setUnchangedCount(preview.getUnchangedCount() + 1);
                        continue;
                    }
                    preview.setCreateCount(preview.getCreateCount() + 1);
                    if (apply) {
                        ReviewAssignment created = new ReviewAssignment();
                        copyAssignment(created, template, scope.categoryId(), reviewerId, scope.projectFilterJson());
                        created.setStatus(ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE);
                        created.setAssignedBy(assignedBy);
                        created.setAssignedAt(assignedAt);
                        assignmentMapper.insert(created);
                    }
                    continue;
                }

                ScopeMutation mutation = resolveScopeMutation(existing, scope, operation, plan);
                if (!mutation.changed()) {
                    preview.setUnchangedCount(preview.getUnchangedCount() + 1);
                    continue;
                }
                registerScopeChange(preview, existing, mutation.target(), mutation.disable(), apply);
                if (mutation.disable()) {
                    preview.setDisableCount(preview.getDisableCount() + 1);
                } else {
                    validateScoreConfigMutable(existing, template);
                    preview.setUpdateCount(preview.getUpdateCount() + 1);
                }
                if (apply) {
                    if (mutation.disable()) {
                        existing.setStatus(ArtReviewConstants.REVIEW_ASSIGNMENT_DISABLED);
                    } else if (ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(operation)) {
                        existing.setProjectFilterJson(mutation.target().getProjectFilterJson());
                        existing.setStatus(ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE);
                    } else {
                        copyAssignment(existing, template, scope.categoryId(), reviewerId, mutation.target().getProjectFilterJson());
                        existing.setProjectIdsJson(null);
                        existing.setExcludedProjectIdsJson(null);
                        existing.setStatus(ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE);
                    }
                    existing.setAssignedBy(assignedBy);
                    existing.setAssignedAt(assignedAt);
                    assignmentMapper.updateById(existing);
                }
            }
        }
        return preview;
    }

    private List<ResolvedAssignmentScope> resolveAssignmentScopes(ReviewAssignmentScopePlanBo plan) {
        List<ReviewAssignmentScopePlanItemBo> items = plan.getScopes() == null ? List.of() : plan.getScopes();
        if (items.size() > 100) {
            throw new ServiceException("一次最多配置100个类别范围");
        }
        Set<Long> categoryIds = new HashSet<>();
        List<ResolvedAssignmentScope> result = new ArrayList<>();
        for (ReviewAssignmentScopePlanItemBo item : items) {
            if (item == null || item.getCategoryId() == null || !categoryIds.add(item.getCategoryId())) {
                throw new ServiceException("分配范围中的类别不能为空且不能重复");
            }
            ActivityCategory category = requireCategory(item.getCategoryId());
            if (!Objects.equals(category.getActivityId(), plan.getActivityId())) {
                throw new ServiceException("类别不属于当前活动");
            }
            String mode = StringUtils.blankToDefault(StringUtils.trim(item.getScopeMode()), SCOPE_MODE_ALL);
            if (SCOPE_MODE_ALL.equals(mode)) {
                result.add(new ResolvedAssignmentScope(item.getCategoryId(), null, List.of()));
                continue;
            }
            if (!SCOPE_MODE_UNITS.equals(mode)) {
                throw new ServiceException("分配范围模式不正确");
            }
            List<String> unitKeys = normalizeFilterValues(item.getUnitKeys());
            if (unitKeys.isEmpty()) {
                throw new ServiceException("按评审单元分配时至少选择一个单元");
            }
            ReviewAssignmentCategoryConfig config = assignmentCategoryConfig(category);
            if (config.getScopeFieldKeys().isEmpty()) {
                throw new ServiceException("当前类别未配置评审单元拆分字段");
            }
            Map<String, ReviewAssignmentScopeUnitVo> units = buildAssignmentScopeUnits(category, config.getScopeFieldKeys())
                .stream().collect(Collectors.toMap(ReviewAssignmentScopeUnitVo::getUnitKey, unit -> unit, (left, right) -> left));
            List<ReviewAssignmentProjectScopeGroup> groups = new ArrayList<>();
            for (String unitKey : unitKeys) {
                ReviewAssignmentScopeUnitVo unit = units.get(unitKey);
                if (unit == null) {
                    throw new ServiceException("评审单元已变化，请刷新后重新选择：" + unitKey);
                }
                ReviewAssignmentProjectScopeGroup group = new ReviewAssignmentProjectScopeGroup();
                Map<String, List<String>> values = new LinkedHashMap<>();
                unit.getFieldValues().forEach((fieldKey, value) -> values.put(fieldKey, List.of(value)));
                group.setFieldValues(values);
                groups.add(group);
            }
            ReviewAssignmentProjectFilter filter = new ReviewAssignmentProjectFilter();
            filter.setScopeGroups(groups.stream().distinct().toList());
            result.add(new ResolvedAssignmentScope(
                item.getCategoryId(),
                normalizeProjectFilterJson(JsonUtils.toJsonString(filter)),
                unitKeys
            ));
        }
        return result;
    }

    private ScopeMutation resolveScopeMutation(
        ReviewAssignment existing,
        ResolvedAssignmentScope incoming,
        String operation,
        ReviewAssignmentScopePlanBo plan
    ) {
        if (ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(operation)) {
            if (!ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE.equals(existing.getStatus())) {
                return new ScopeMutation(false, false, existing);
            }
            if (incoming.projectFilterJson() == null) {
                return new ScopeMutation(true, true, null);
            }
            String remaining = subtractAssignmentUnits(existing, incoming);
            if (remaining == null) {
                return new ScopeMutation(true, true, null);
            }
            ReviewAssignment target = assignmentScopeTarget(existing, remaining);
            boolean changed = !Objects.equals(parseProjectFilter(existing.getProjectFilterJson()), parseProjectFilter(remaining));
            return new ScopeMutation(changed, false, target);
        }
        boolean reactivating = !ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE.equals(existing.getStatus());
        String effectiveFilter = SCOPE_PLAN_REPLACE.equals(operation) || reactivating
            ? incoming.projectFilterJson()
            : mergeBatchProjectFilterJson(existing.getProjectFilterJson(), incoming.projectFilterJson(),
                ArtReviewConstants.ASSIGNMENT_BATCH_MERGE);
        ReviewAssignmentBo template = scopePlanAssignmentBo(plan);
        template.setCategoryId(existing.getCategoryId());
        template.setReviewerUserId(existing.getReviewerUserId());
        template.setProjectFilterJson(effectiveFilter);
        boolean changed = reactivating || !reviewConfigMatches(existing, template, existing.getCategoryId(), effectiveFilter);
        ReviewAssignment target = assignmentScopeTarget(existing, effectiveFilter);
        target.setSchoolScopeMode(AssignmentSchoolScopeSupport.normalizeMode(plan.getSchoolScopeMode()));
        target.setSchoolIdsJson(AssignmentSchoolScopeSupport.normalizeSchoolIdsJson(
            plan.getSchoolScopeMode(), plan.getSchoolIdsJson()));
        return new ScopeMutation(changed, false, target);
    }

    private String subtractAssignmentUnits(ReviewAssignment existing, ResolvedAssignmentScope incoming) {
        ReviewAssignmentProjectFilter current = parseProjectFilter(existing.getProjectFilterJson());
        if (!current.getSchoolTypes().isEmpty() || !current.getFieldValues().isEmpty()
            || current.getScopeGroups().stream().anyMatch(group -> !group.getSchoolTypes().isEmpty())) {
            throw new ServiceException("历史复合筛选不能直接按评审单元移除，请使用替换计划");
        }
        List<ReviewAssignmentProjectScopeGroup> currentGroups;
        if (current.getScopeGroups().isEmpty()) {
            ActivityCategory category = requireCategory(existing.getCategoryId());
            ReviewAssignmentCategoryConfig config = assignmentCategoryConfig(category);
            currentGroups = buildAssignmentScopeUnits(category, config.getScopeFieldKeys()).stream().map(unit -> {
                ReviewAssignmentProjectScopeGroup group = new ReviewAssignmentProjectScopeGroup();
                Map<String, List<String>> values = new LinkedHashMap<>();
                unit.getFieldValues().forEach((key, value) -> values.put(key, List.of(value)));
                group.setFieldValues(values);
                return group;
            }).distinct().collect(Collectors.toCollection(ArrayList::new));
        } else {
            currentGroups = new ArrayList<>(current.getScopeGroups());
        }
        currentGroups.removeAll(parseProjectFilter(incoming.projectFilterJson()).getScopeGroups());
        if (currentGroups.isEmpty()) {
            return null;
        }
        ReviewAssignmentProjectFilter remaining = new ReviewAssignmentProjectFilter();
        remaining.setScopeGroups(currentGroups);
        return normalizeProjectFilterJson(JsonUtils.toJsonString(remaining));
    }

    private ReviewAssignment assignmentScopeTarget(ReviewAssignment source, String filterJson) {
        ReviewAssignment target = new ReviewAssignment();
        target.setId(source.getId());
        target.setActivityId(source.getActivityId());
        target.setCategoryId(source.getCategoryId());
        target.setReviewerUserId(source.getReviewerUserId());
        target.setSchoolScopeMode(source.getSchoolScopeMode());
        target.setSchoolIdsJson(source.getSchoolIdsJson());
        target.setProjectFilterJson(filterJson);
        target.setProjectIdsJson(null);
        target.setExcludedProjectIdsJson(null);
        target.setStatus(ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE);
        return target;
    }

    private void registerScopeChange(
        AssignmentBatchPreviewVo preview,
        ReviewAssignment existing,
        ReviewAssignment target,
        boolean disable,
        boolean apply
    ) {
        List<ReviewScore> scores = scoreMapper.selectList(Wrappers.lambdaQuery(ReviewScore.class)
            .eq(ReviewScore::getAssignmentId, existing.getId()));
        if (!scores.isEmpty()) {
            Set<Long> retainedProjectIds;
            if (disable || target == null) {
                retainedProjectIds = Set.of();
            } else {
                retainedProjectIds = projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
                        .eq(Project::getActivityId, existing.getActivityId())
                        .eq(Project::getCategoryId, existing.getCategoryId())
                        .eq(Project::getStatus, ArtReviewConstants.PROJECT_AUDIT_PASSED))
                    .stream().filter(project -> assignmentMatchesProject(target, project))
                    .map(Project::getId).collect(Collectors.toSet());
            }
            long orphanDrafts = scores.stream()
                .filter(score -> ArtReviewConstants.REVIEW_SCORE_DRAFT.equals(score.getStatus()))
                .filter(score -> !retainedProjectIds.contains(score.getProjectId()))
                .count();
            preview.setOrphanDraftScoreCount(preview.getOrphanDraftScoreCount() + orphanDrafts);
        }
        if (hasActiveSignedSheet(existing)) {
            long locked = signedSheetItemMapper.selectCount(Wrappers.lambdaQuery(ReviewScoreSheetSignedSheetItem.class)
                .eq(ReviewScoreSheetSignedSheetItem::getAssignmentId, existing.getId())
                .eq(ReviewScoreSheetSignedSheetItem::getActiveMarker, SIGNED_SHEET_ACTIVE_MARKER));
            preview.setLockedScoreCount(preview.getLockedScoreCount() + locked);
            if (apply) {
                throw new ServiceException("该评委当前类别已有有效签字评分表，请先撤回签字后再调整分配范围");
            }
        }
    }

    private boolean hasActiveSignedSheet(ReviewAssignment assignment) {
        return signedSheetMapper.selectCount(Wrappers.lambdaQuery(ReviewScoreSheetSignedSheet.class)
            .eq(ReviewScoreSheetSignedSheet::getReviewerUserId, assignment.getReviewerUserId())
            .eq(ReviewScoreSheetSignedSheet::getActivityId, assignment.getActivityId())
            .eq(ReviewScoreSheetSignedSheet::getCategoryId, assignment.getCategoryId())
            .eq(ReviewScoreSheetSignedSheet::getStatus, SIGNED_SHEET_ACTIVE)
            .eq(ReviewScoreSheetSignedSheet::getActiveMarker, SIGNED_SHEET_ACTIVE_MARKER)) > 0;
    }

    private ReviewAssignmentBo scopePlanAssignmentBo(ReviewAssignmentScopePlanBo plan) {
        ReviewAssignmentBo bo = new ReviewAssignmentBo();
        bo.setActivityId(plan.getActivityId());
        bo.setSchoolScopeMode(plan.getSchoolScopeMode());
        bo.setSchoolIdsJson(plan.getSchoolIdsJson());
        bo.setScoreMode(plan.getScoreMode());
        bo.setScoreRuleJson(plan.getScoreRuleJson());
        bo.setExclusiveMode(plan.getExclusiveMode());
        bo.setScoreVisibilityPolicy(plan.getScoreVisibilityPolicy());
        bo.setHideSchoolInfo(plan.getHideSchoolInfo());
        bo.setHideMemberInfo(plan.getHideMemberInfo());
        bo.setHiddenFieldKeysJson(plan.getHiddenFieldKeysJson());
        bo.setStatus(plan.getStatus());
        return bo;
    }

    private String normalizeScopePlanOperation(String operation) {
        String normalized = StringUtils.blankToDefault(StringUtils.trim(operation), ArtReviewConstants.ASSIGNMENT_BATCH_MERGE);
        if (!ArtReviewConstants.ASSIGNMENT_BATCH_MERGE.equals(normalized)
            && !SCOPE_PLAN_REPLACE.equals(normalized)
            && !ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(normalized)) {
            throw new ServiceException("范围计划操作不正确");
        }
        return normalized;
    }

    private void checkScopePlanPermissions(String operation) {
        if (ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(operation)) {
            StpUtil.checkPermission("crehn:reviewAssignment:remove");
            return;
        }
        StpUtil.checkPermission("crehn:reviewAssignment:add");
        StpUtil.checkPermission("crehn:reviewAssignment:edit");
        if (SCOPE_PLAN_REPLACE.equals(operation)) {
            StpUtil.checkPermission("crehn:reviewAssignment:remove");
        }
    }

    private record ResolvedAssignmentScope(Long categoryId, String projectFilterJson, List<String> unitKeys) {
    }

    private record ScopeMutation(boolean changed, boolean disable, ReviewAssignment target) {
    }

    private record AssignmentScopeKey(Long assignmentId, String unitKey) {
    }

    private void maintainProjectScope(AssignmentBatchPreviewVo preview, ReviewAssignmentBo bo,
                                      ReviewAssignment assignment, Long categoryId, Long reviewerUserId,
                                      Set<Long> targetProjectIds, String operation, Long assignedBy,
                                      Date assignedAt, boolean apply) {
        if (assignment == null) {
            if (ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(operation)) {
                preview.setUnchangedCount(preview.getUnchangedCount() + 1);
                return;
            }
            preview.setCreateCount(preview.getCreateCount() + 1);
            if (apply) {
                ReviewAssignment created = new ReviewAssignment();
                copyAssignment(created, bo, categoryId, reviewerUserId);
                created.setProjectIdsJson(JsonUtils.toJsonString(new ArrayList<>(targetProjectIds)));
                created.setExcludedProjectIdsJson(null);
                created.setStatus(ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE);
                created.setAssignedBy(assignedBy);
                created.setAssignedAt(assignedAt);
                assignmentMapper.insert(created);
            }
            return;
        }
        if (ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(operation)
            && !ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE.equals(assignment.getStatus())) {
            preview.setUnchangedCount(preview.getUnchangedCount() + 1);
            return;
        }

        Set<Long> includedIds = parseProjectIdSet(assignment.getProjectIdsJson());
        Set<Long> excludedIds = parseProjectIdSet(assignment.getExcludedProjectIdsJson());
        boolean disable = false;
        boolean changed;
        if (ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(operation)) {
            addPreservedScoreCounts(preview, assignment.getId(), targetProjectIds);
            if (includedIds.isEmpty()) {
                changed = excludedIds.addAll(targetProjectIds);
            } else {
                changed = includedIds.removeAll(targetProjectIds);
                disable = changed && includedIds.isEmpty();
            }
        } else if (!ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE.equals(assignment.getStatus())) {
            includedIds = new LinkedHashSet<>(targetProjectIds);
            excludedIds.clear();
            changed = true;
        } else {
            if (includedIds.isEmpty()) {
                changed = excludedIds.removeAll(targetProjectIds);
            } else {
                changed = includedIds.addAll(targetProjectIds);
            }
        }
        if (!changed) {
            preview.setUnchangedCount(preview.getUnchangedCount() + 1);
            return;
        }
        if (disable) {
            preview.setDisableCount(preview.getDisableCount() + 1);
        } else {
            preview.setUpdateCount(preview.getUpdateCount() + 1);
        }
        if (apply) {
            if (!disable) {
                assignment.setProjectIdsJson(includedIds.isEmpty() ? null : JsonUtils.toJsonString(new ArrayList<>(includedIds)));
                assignment.setExcludedProjectIdsJson(excludedIds.isEmpty() ? null : JsonUtils.toJsonString(new ArrayList<>(excludedIds)));
            }
            assignment.setStatus(disable ? ArtReviewConstants.REVIEW_ASSIGNMENT_DISABLED : ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE);
            assignment.setAssignedBy(assignedBy);
            assignment.setAssignedAt(assignedAt);
            assignmentMapper.updateById(assignment);
        }
    }

    private void validateActivityAndCategories(Long activityId, List<Long> categoryIds) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        for (Long categoryId : categoryIds) {
            ActivityCategory category = categoryMapper.selectById(categoryId);
            if (category == null || !Objects.equals(category.getActivityId(), activityId)) {
                throw new ServiceException("类别不属于当前活动");
            }
        }
    }

    private void validateReviewer(Long reviewerUserId) {
        SysUser reviewer = sysUserMapper.selectById(reviewerUserId);
        if (reviewer == null || !"0".equals(reviewer.getStatus())) {
            throw new ServiceException("专家账号不存在或已停用");
        }
        if (!hasReviewerPermissions(reviewerUserId)) {
            throw new ServiceException("专家账号必须具备评审任务和评分权限");
        }
    }

    private void validateTargetProjects(Long activityId, Long categoryId, Set<Long> projectIds) {
        List<Project> projects = projectMapper.selectBatchIds(projectIds);
        if (projects.size() != projectIds.size() || projects.stream().anyMatch(project ->
            !Objects.equals(project.getActivityId(), activityId)
                || !Objects.equals(project.getCategoryId(), categoryId)
                || !ArtReviewConstants.PROJECT_AUDIT_PASSED.equals(project.getStatus()))) {
            throw new ServiceException("所选项目必须是同一活动类别下已审核通过的项目");
        }
    }

    private String normalizeBatchOperation(String operation) {
        String normalized = StringUtils.blankToDefault(StringUtils.trim(operation), ArtReviewConstants.ASSIGNMENT_BATCH_MERGE);
        if (!ArtReviewConstants.ASSIGNMENT_BATCH_MERGE.equals(normalized)
            && !ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(normalized)
            && !ArtReviewConstants.ASSIGNMENT_BATCH_SYNC.equals(normalized)) {
            throw new ServiceException("批量维护方式不正确");
        }
        return normalized;
    }

    private String normalizeBatchTarget(String target) {
        String normalized = StringUtils.blankToDefault(StringUtils.trim(target), ArtReviewConstants.ASSIGNMENT_BATCH_TARGET_ASSIGNMENT);
        if (!ArtReviewConstants.ASSIGNMENT_BATCH_TARGET_ASSIGNMENT.equals(normalized)
            && !ArtReviewConstants.ASSIGNMENT_BATCH_TARGET_PROJECT.equals(normalized)) {
            throw new ServiceException("批量维护目标不正确");
        }
        return normalized;
    }

    private void checkBatchPermissions(String operation) {
        if (ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(operation)) {
            StpUtil.checkPermission("crehn:reviewAssignment:remove");
            return;
        }
        StpUtil.checkPermission("crehn:reviewAssignment:add");
        StpUtil.checkPermission("crehn:reviewAssignment:edit");
        if (ArtReviewConstants.ASSIGNMENT_BATCH_SYNC.equals(operation)) {
            StpUtil.checkPermission("crehn:reviewAssignment:remove");
        }
    }

    private void addPreservedScoreCounts(AssignmentBatchPreviewVo preview, Long assignmentId, Set<Long> projectIds) {
        if (assignmentId == null) {
            return;
        }
        LambdaQueryWrapper<ReviewScore> wrapper = Wrappers.lambdaQuery(ReviewScore.class)
            .eq(ReviewScore::getAssignmentId, assignmentId)
            .in(projectIds != null && !projectIds.isEmpty(), ReviewScore::getProjectId, projectIds);
        List<ReviewScore> scores = scoreMapper.selectList(wrapper);
        preview.setPreservedSubmittedScoreCount(preview.getPreservedSubmittedScoreCount()
            + scores.stream().filter(score -> ArtReviewConstants.REVIEW_SCORE_SUBMITTED.equals(score.getStatus())).count());
        preview.setPreservedDraftScoreCount(preview.getPreservedDraftScoreCount()
            + scores.stream().filter(score -> ArtReviewConstants.REVIEW_SCORE_DRAFT.equals(score.getStatus())).count());
    }

    private String assignmentKey(Long userId, Long categoryId) {
        return String.valueOf(userId) + ":" + String.valueOf(categoryId);
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
    }

    private boolean hasReviewerPermissions(Long userId) {
        return userId != null && reviewerUserIdsWithPermissions().contains(userId);
    }

    private Set<Long> reviewerUserIdsWithPermissions() {
        List<SysMenu> permissionMenus = sysMenuMapper.selectList(Wrappers.lambdaQuery(SysMenu.class)
            .select(SysMenu::getMenuId, SysMenu::getPerms)
            .in(SysMenu::getPerms, REQUIRED_REVIEW_PERMISSIONS)
            .eq(SysMenu::getStatus, "0"));
        Map<Long, String> menuPermMap = permissionMenus.stream()
            .filter(menu -> menu.getMenuId() != null && StringUtils.isNotBlank(menu.getPerms()))
            .collect(Collectors.toMap(SysMenu::getMenuId, SysMenu::getPerms, (left, right) -> left));
        if (menuPermMap.isEmpty()) {
            return Set.of();
        }
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(Wrappers.lambdaQuery(SysRoleMenu.class)
            .select(SysRoleMenu::getRoleId, SysRoleMenu::getMenuId)
            .in(SysRoleMenu::getMenuId, menuPermMap.keySet()));
        Map<Long, Set<String>> rolePermMap = new LinkedHashMap<>();
        for (SysRoleMenu roleMenu : roleMenus) {
            if (roleMenu.getRoleId() == null || roleMenu.getMenuId() == null) {
                continue;
            }
            String perm = menuPermMap.get(roleMenu.getMenuId());
            if (StringUtils.isBlank(perm)) {
                continue;
            }
            rolePermMap.computeIfAbsent(roleMenu.getRoleId(), key -> new HashSet<>()).add(perm);
        }
        Set<Long> roleIds = new HashSet<>(rolePermMap.keySet());
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        roleIds = sysRoleMapper.selectList(Wrappers.lambdaQuery(SysRole.class)
                .select(SysRole::getRoleId)
                .in(SysRole::getRoleId, roleIds)
                .eq(SysRole::getStatus, "0")
                .eq(SysRole::getDelFlag, "0"))
            .stream()
            .map(SysRole::getRoleId)
            .collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(Wrappers.lambdaQuery(SysUserRole.class)
            .select(SysUserRole::getUserId, SysUserRole::getRoleId)
            .in(SysUserRole::getRoleId, roleIds));
        Map<Long, Set<String>> userPermMap = new LinkedHashMap<>();
        for (SysUserRole userRole : userRoles) {
            if (userRole.getUserId() == null || userRole.getRoleId() == null) {
                continue;
            }
            Set<String> rolePerms = rolePermMap.get(userRole.getRoleId());
            if (rolePerms == null || rolePerms.isEmpty()) {
                continue;
            }
            userPermMap.computeIfAbsent(userRole.getUserId(), key -> new HashSet<>()).addAll(rolePerms);
        }
        return userPermMap.entrySet().stream()
            .filter(entry -> entry.getValue().containsAll(REQUIRED_REVIEW_PERMISSIONS))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    }

    private void excludeAssignedReviewerIds(Set<Long> userIds, Long activityId, Long categoryId, Long includeUserId) {
        if (userIds.isEmpty() || activityId == null || categoryId == null) {
            return;
        }
        Set<Long> assignedUserIds = assignmentMapper.selectList(Wrappers.lambdaQuery(ReviewAssignment.class)
                .select(ReviewAssignment::getReviewerUserId)
                .eq(ReviewAssignment::getActivityId, activityId)
                .eq(ReviewAssignment::getCategoryId, categoryId))
            .stream()
            .map(ReviewAssignment::getReviewerUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (includeUserId != null) {
            assignedUserIds.remove(includeUserId);
        }
        userIds.removeAll(assignedUserIds);
    }

    private void ensureNoDuplicateAssignment(Long id, Long activityId, Long categoryId, Long reviewerUserId) {
        long count = assignmentMapper.selectCount(Wrappers.lambdaQuery(ReviewAssignment.class)
            .ne(id != null, ReviewAssignment::getId, id)
            .eq(ReviewAssignment::getActivityId, activityId)
            .eq(ReviewAssignment::getCategoryId, categoryId)
            .eq(ReviewAssignment::getReviewerUserId, reviewerUserId));
        if (count > 0) {
            throw new ServiceException("该专家已分配到当前类别");
        }
    }

    private boolean reviewConfigMatches(
        ReviewAssignment assignment,
        ReviewAssignmentBo bo,
        Long categoryId,
        String effectiveProjectFilterJson
    ) {
        String normalizedMode = AssignmentSchoolScopeSupport.normalizeMode(bo.getSchoolScopeMode());
        Set<Long> expectedSchoolIds = AssignmentSchoolScopeSupport.parseSchoolIds(
            AssignmentSchoolScopeSupport.normalizeSchoolIdsJson(normalizedMode, bo.getSchoolIdsJson())
        );
        return ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE.equals(assignment.getStatus())
            && Objects.equals(normalizedMode, AssignmentSchoolScopeSupport.normalizeMode(assignment.getSchoolScopeMode()))
            && Objects.equals(expectedSchoolIds, AssignmentSchoolScopeSupport.parseSchoolIds(assignment.getSchoolIdsJson()))
            && Objects.equals(parseProjectFilter(effectiveProjectFilterJson), parseProjectFilter(assignment.getProjectFilterJson()))
            && Objects.equals(StringUtils.blankToDefault(bo.getScoreMode(), ArtReviewConstants.REVIEW_SCORE_MODE_NUMERIC), assignment.getScoreMode())
            && jsonObjectEquals(
                StringUtils.blankToDefault(bo.getScoreRuleJson(), "{\"min\":0,\"max\":100,\"step\":1}"),
                assignment.getScoreRuleJson()
            )
            && Objects.equals(StringUtils.blankToDefault(bo.getExclusiveMode(), ArtReviewConstants.REVIEW_EXCLUSIVE_SINGLE), assignment.getExclusiveMode())
            && Objects.equals(normalizeScoreVisibilityPolicy(bo.getScoreVisibilityPolicy()), assignment.getScoreVisibilityPolicy())
            && Objects.equals(Boolean.TRUE.equals(bo.getHideSchoolInfo()), assignment.getHideSchoolInfo())
            && Objects.equals(Boolean.TRUE.equals(bo.getHideMemberInfo()), assignment.getHideMemberInfo())
            && Objects.equals(parseProjectIdSet(bo.getProjectIdsJson()), parseProjectIdSet(assignment.getProjectIdsJson()))
            && parseProjectIdSet(assignment.getExcludedProjectIdsJson()).isEmpty()
            && Objects.equals(parseStringSet(hiddenFieldKeysForCategory(bo, categoryId)), parseStringSet(assignment.getHiddenFieldKeysJson()));
    }

    private void validateScoreConfigMutable(ReviewAssignment assignment, ReviewAssignmentBo bo) {
        if (assignment == null || assignment.getId() == null) {
            return;
        }
        boolean scoreConfigChanged = !Objects.equals(
            StringUtils.blankToDefault(bo.getScoreMode(), ArtReviewConstants.REVIEW_SCORE_MODE_NUMERIC),
            assignment.getScoreMode()
        ) || !jsonObjectEquals(
            StringUtils.blankToDefault(bo.getScoreRuleJson(), "{\"min\":0,\"max\":100,\"step\":1}"),
            assignment.getScoreRuleJson()
        );
        if (!scoreConfigChanged) {
            return;
        }
        long scoreCount = scoreMapper.selectCount(Wrappers.lambdaQuery(ReviewScore.class)
            .eq(ReviewScore::getAssignmentId, assignment.getId()));
        if (scoreCount > 0) {
            throw new ServiceException("该评审分配已经产生评分，不能直接修改评分模式或评分规则");
        }
    }

    private void copyAssignment(ReviewAssignment assignment, ReviewAssignmentBo bo, Long categoryId, Long reviewerUserId) {
        copyAssignment(assignment, bo, categoryId, reviewerUserId, bo.getProjectFilterJson());
    }

    private void copyAssignment(
        ReviewAssignment assignment,
        ReviewAssignmentBo bo,
        Long categoryId,
        Long reviewerUserId,
        String effectiveProjectFilterJson
    ) {
        assignment.setActivityId(bo.getActivityId());
        assignment.setCategoryId(categoryId);
        assignment.setReviewerUserId(reviewerUserId);
        assignment.setSchoolScopeMode(AssignmentSchoolScopeSupport.normalizeMode(bo.getSchoolScopeMode()));
        assignment.setSchoolIdsJson(AssignmentSchoolScopeSupport.normalizeSchoolIdsJson(bo.getSchoolScopeMode(), bo.getSchoolIdsJson()));
        assignment.setProjectFilterJson(normalizeProjectFilterJson(effectiveProjectFilterJson));
        assignment.setScoreMode(StringUtils.blankToDefault(bo.getScoreMode(), ArtReviewConstants.REVIEW_SCORE_MODE_NUMERIC));
        assignment.setScoreRuleJson(StringUtils.blankToDefault(bo.getScoreRuleJson(), "{\"min\":0,\"max\":100,\"step\":1}"));
        assignment.setExclusiveMode(StringUtils.blankToDefault(bo.getExclusiveMode(), ArtReviewConstants.REVIEW_EXCLUSIVE_SINGLE));
        assignment.setScoreVisibilityPolicy(normalizeScoreVisibilityPolicy(bo.getScoreVisibilityPolicy()));
        ReviewAssignmentCategoryConfig categoryConfig = assignmentCategoryConfig(categoryMapper.selectById(categoryId));
        boolean categoryVisibility = Boolean.TRUE.equals(categoryConfig.getVisibilityConfigured());
        assignment.setHideSchoolInfo(categoryVisibility ? categoryConfig.getHideSchoolInfo() : Boolean.TRUE.equals(bo.getHideSchoolInfo()));
        assignment.setHideMemberInfo(categoryVisibility ? categoryConfig.getHideMemberInfo() : Boolean.TRUE.equals(bo.getHideMemberInfo()));
        assignment.setProjectIdsJson(ReviewAssignmentProjectScopeSupport.normalizeProjectIdsJson(bo.getProjectIdsJson()));
        assignment.setExcludedProjectIdsJson(ReviewAssignmentProjectScopeSupport.normalizeProjectIdsJson(bo.getExcludedProjectIdsJson()));
        assignment.setHiddenFieldKeysJson(categoryVisibility ? categoryConfig.getHiddenFieldKeysJson() : hiddenFieldKeysForCategory(bo, categoryId));
        assignment.setStatus(StringUtils.blankToDefault(bo.getStatus(), ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE));
    }

    private ReviewAssignment requireAssignment(Long id) {
        ReviewAssignment assignment = assignmentMapper.selectById(id);
        if (assignment == null) {
            throw new ServiceException("评审分配不存在");
        }
        return assignment;
    }

    private boolean assignmentMatchesProject(ReviewAssignment assignment, Project project) {
        return assignmentMatchesProject(assignment, project, null);
    }

    private boolean assignmentMatchesProject(ReviewAssignmentVo assignment, Project project) {
        return assignment != null
            && project != null
            && Objects.equals(assignment.getActivityId(), project.getActivityId())
            && Objects.equals(assignment.getCategoryId(), project.getCategoryId())
            && AssignmentSchoolScopeSupport.matches(project.getSchoolId(), assignment.getSchoolScopeMode(), assignment.getSchoolIdsJson())
            && projectMatchesFilter(project, parseProjectFilter(assignment.getProjectFilterJson()), null)
            && ReviewAssignmentProjectScopeSupport.matches(
                project.getId(),
                assignment.getProjectIdsJson(),
                assignment.getExcludedProjectIdsJson()
            );
    }

    private boolean assignmentMatchesProject(ReviewAssignment assignment, Project project, org.dromara.crehn.domain.SchoolInfo school) {
        return assignment != null
            && project != null
            && Objects.equals(assignment.getActivityId(), project.getActivityId())
            && Objects.equals(assignment.getCategoryId(), project.getCategoryId())
            && AssignmentSchoolScopeSupport.matches(project.getSchoolId(), assignment.getSchoolScopeMode(), assignment.getSchoolIdsJson())
            && projectMatchesFilter(project, parseProjectFilter(assignment.getProjectFilterJson()), school)
            && ReviewAssignmentProjectScopeSupport.matches(
                project.getId(),
                assignment.getProjectIdsJson(),
                assignment.getExcludedProjectIdsJson()
            );
    }

    private List<ReviewAssignment> currentUserAssignments(Long activityId, Long categoryId) {
        return assignmentMapper.selectList(Wrappers.lambdaQuery(ReviewAssignment.class)
            .eq(ReviewAssignment::getReviewerUserId, LoginHelper.getUserId())
            .eq(ReviewAssignment::getStatus, ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE)
            .eq(activityId != null, ReviewAssignment::getActivityId, activityId)
            .eq(categoryId != null, ReviewAssignment::getCategoryId, categoryId)
            .orderByDesc(ReviewAssignment::getCreateTime));
    }

    private ReviewAssignment requireCurrentUserAssignment(Long assignmentId) {
        ReviewAssignment assignment = assignmentMapper.selectOne(Wrappers.lambdaQuery(ReviewAssignment.class)
            .eq(ReviewAssignment::getId, assignmentId)
            .eq(ReviewAssignment::getReviewerUserId, LoginHelper.getUserId())
            .eq(ReviewAssignment::getStatus, ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE)
            .last("limit 1"));
        if (assignment == null) {
            throw new ServiceException("当前用户没有可用的评审分配");
        }
        return assignment;
    }

    private Project requireAssignableProject(ReviewAssignment assignment, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null
            || !Objects.equals(project.getActivityId(), assignment.getActivityId())
            || !Objects.equals(project.getCategoryId(), assignment.getCategoryId())
            || !assignmentMatchesProject(assignment, project)
            || !ArtReviewConstants.PROJECT_AUDIT_PASSED.equals(project.getStatus())) {
            throw new ServiceException("项目当前不可评审");
        }
        return project;
    }

    private ReviewTaskVo buildTaskVo(ReviewAssignment assignment, Project project, boolean detail) {
        ReviewTaskVo task = new ReviewTaskVo();
        task.setAssignmentId(assignment.getId());
        task.setScopeKey(assignmentScopeKey(assignment.getId(), null));
        task.setProjectId(project.getId());
        task.setActivityId(project.getActivityId());
        task.setCategoryId(project.getCategoryId());
        task.setProjectNo(project.getProjectNo());
        task.setProjectName(project.getProjectName());
        task.setProjectStatus(project.getStatus());
        task.setSubmittedAt(project.getSubmittedAt());
        task.setScoreMode(assignment.getScoreMode());
        task.setScoreRuleJson(assignment.getScoreRuleJson());
        task.setExclusiveMode(assignment.getExclusiveMode());
        // Expert scoring is mutually blind. Management summaries use separate
        // privileged APIs and never rely on the reviewer task payload.
        task.setScoreVisibilityPolicy(ArtReviewConstants.REVIEW_SCORE_VISIBILITY_HIDDEN);
        Activity activity = activityMapper.selectById(project.getActivityId());
        ActivityCategory category = categoryMapper.selectById(project.getCategoryId());
        ReviewAssignmentCategoryConfig categoryConfig = assignmentCategoryConfig(category);
        boolean categoryVisibility = Boolean.TRUE.equals(categoryConfig.getVisibilityConfigured());
        task.setHideSchoolInfo(categoryVisibility ? categoryConfig.getHideSchoolInfo() : assignment.getHideSchoolInfo());
        task.setHideMemberInfo(categoryVisibility ? categoryConfig.getHideMemberInfo() : assignment.getHideMemberInfo());
        task.setHiddenFieldKeysJson(categoryVisibility ? categoryConfig.getHiddenFieldKeysJson() : assignment.getHiddenFieldKeysJson());
        task.setActivityName(activity == null ? null : activity.getActivityName());
        task.setCategoryName(category == null ? null : category.getCategoryName());
        task.setProgramForm(classificationValue(project, PROGRAM_FORM_KEYS));
        task.setGroupOrNature(groupOrNature(project, category));
        ReviewScore score = currentUserScore(assignment.getId(), project.getId());
        if (score != null) {
            task.setScoreId(score.getId());
            task.setScoreValue(score.getScoreValue());
            task.setGradeValue(score.getGradeValue());
            task.setCommentText(score.getCommentText());
            boolean signed = hasActiveSignedSheetItem(score.getId());
            task.setScoreStatus(signed ? ArtReviewConstants.REVIEW_SCORE_SUBMITTED : ArtReviewConstants.REVIEW_SCORE_DRAFT);
            task.setScoreSubmittedAt(signed ? score.getSubmittedAt() : null);
        }
        boolean lockedByOther = ArtReviewConstants.REVIEW_EXCLUSIVE_SINGLE.equals(assignment.getExclusiveMode())
            && hasSubmittedScoreByOther(assignment, project.getId());
        task.setLockedByOther(lockedByOther);
        task.setLockedMessage(lockedByOther ? "单评模式下，其他专家已提交该项目评分" : null);
        if (detail) {
            task.setProject(buildProjectDetail(project, assignment));
            task.setPeerScores(List.of());
        }
        return task;
    }

    private String classificationValue(Project project, String... keys) {
        Map<String, Object> formData = parseObject(project.getFormDataJson());
        for (String key : keys) {
            Object value = formData.get(key);
            if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
                return StringUtils.trim(String.valueOf(value));
            }
        }
        return null;
    }

    private String groupOrNature(Project project, ActivityCategory category) {
        if (StringUtils.isNotBlank(project.getGroupName())) {
            return StringUtils.trim(project.getGroupName());
        }
        String group = classificationValue(project, GROUP_NAME_KEYS);
        if (StringUtils.isNotBlank(group)) {
            return group;
        }
        if (StringUtils.isNotBlank(project.getGroupCode())) {
            return StringUtils.trim(project.getGroupCode());
        }
        String categoryCode = category == null ? "" : StringUtils.blankToDefault(category.getCategoryCode(), "");
        String categoryName = category == null ? "" : StringUtils.blankToDefault(category.getCategoryName(), "");
        return categoryCode.toLowerCase().contains("personal") || categoryName.contains("个人") ? "个人" : UNGROUPED_LABEL;
    }

    private String normalizeScoreVisibilityPolicy(String policy) {
        if (ArtReviewConstants.REVIEW_SCORE_VISIBILITY_HIDDEN.equals(policy)
            || ArtReviewConstants.REVIEW_SCORE_VISIBILITY_ALWAYS.equals(policy)
            || ArtReviewConstants.REVIEW_SCORE_VISIBILITY_AFTER_SUBMIT.equals(policy)) {
            return policy;
        }
        return ArtReviewConstants.REVIEW_SCORE_VISIBILITY_AFTER_SUBMIT;
    }

    private boolean canViewPeerScores(ReviewAssignment assignment, ReviewScore currentScore) {
        String policy = normalizeScoreVisibilityPolicy(assignment.getScoreVisibilityPolicy());
        if (ArtReviewConstants.REVIEW_SCORE_VISIBILITY_ALWAYS.equals(policy)) {
            return true;
        }
        if (ArtReviewConstants.REVIEW_SCORE_VISIBILITY_HIDDEN.equals(policy)) {
            return false;
        }
        return currentScore != null && ArtReviewConstants.REVIEW_SCORE_SUBMITTED.equals(currentScore.getStatus());
    }

    private List<ReviewScoreVo> listSubmittedScoresForProject(Long projectId) {
        List<ReviewScoreVo> rows = scoreMapper.selectVoList(Wrappers.lambdaQuery(ReviewScore.class)
            .eq(ReviewScore::getProjectId, projectId)
            .eq(ReviewScore::getStatus, ArtReviewConstants.REVIEW_SCORE_SUBMITTED)
            .orderByDesc(ReviewScore::getSubmittedAt)
            .orderByDesc(ReviewScore::getCreateTime));
        fillScoreReviewerNames(rows);
        return rows;
    }

    private ReviewScore currentUserScore(Long assignmentId, Long projectId) {
        return scoreMapper.selectOne(Wrappers.lambdaQuery(ReviewScore.class)
            .eq(ReviewScore::getAssignmentId, assignmentId)
            .eq(ReviewScore::getProjectId, projectId)
            .eq(ReviewScore::getReviewerUserId, LoginHelper.getUserId())
            .last("limit 1"));
    }

    private boolean hasSubmittedScoreByOther(ReviewAssignment assignment, Long projectId) {
        List<Long> assignmentIds = assignmentMapper.selectList(Wrappers.lambdaQuery(ReviewAssignment.class)
                .select(ReviewAssignment::getId)
                .eq(ReviewAssignment::getActivityId, assignment.getActivityId())
                .eq(ReviewAssignment::getCategoryId, assignment.getCategoryId())
                .eq(ReviewAssignment::getStatus, ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE))
            .stream()
            .map(ReviewAssignment::getId)
            .toList();
        if (assignmentIds.isEmpty()) {
            return false;
        }
        return scoreMapper.selectCount(Wrappers.lambdaQuery(ReviewScore.class)
            .in(ReviewScore::getAssignmentId, assignmentIds)
            .eq(ReviewScore::getProjectId, projectId)
            .eq(ReviewScore::getStatus, ArtReviewConstants.REVIEW_SCORE_SUBMITTED)
            .ne(ReviewScore::getReviewerUserId, LoginHelper.getUserId())) > 0;
    }

    private void ensureNotLockedByOther(ReviewAssignment assignment, Long projectId) {
        if (hasSubmittedScoreByOther(assignment, projectId)) {
            throw new ServiceException("其他专家已提交该项目评分");
        }
    }

    private void validateScoreValue(ReviewAssignment assignment, ReviewScoreBo bo, String targetStatus) {
        if (!ArtReviewConstants.REVIEW_SCORE_SUBMITTED.equals(targetStatus)) {
            return;
        }
        if (ArtReviewConstants.REVIEW_SCORE_MODE_GRADE.equals(assignment.getScoreMode())) {
            if (StringUtils.isBlank(bo.getGradeValue())) {
                throw new ServiceException("请选择评分等级");
            }
            return;
        }
        if (ArtReviewConstants.REVIEW_SCORE_MODE_COMMENT_ONLY.equals(assignment.getScoreMode())) {
            if (StringUtils.isBlank(bo.getCommentText())) {
                throw new ServiceException("请输入评语");
            }
            return;
        }
        if (bo.getScoreValue() == null) {
            throw new ServiceException("请输入分数");
        }
        BigDecimal min = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.valueOf(100);
        Map<String, Object> rule = parseObject(assignment.getScoreRuleJson());
        if (rule.get("min") != null) {
            min = new BigDecimal(String.valueOf(rule.get("min")));
        }
        if (rule.get("max") != null) {
            max = new BigDecimal(String.valueOf(rule.get("max")));
        }
        if (bo.getScoreValue().compareTo(min) < 0 || bo.getScoreValue().compareTo(max) > 0) {
            throw new ServiceException("分数超出允许范围");
        }
    }

    private ProjectVo buildProjectDetail(Project project, ReviewAssignment assignment) {
        ProjectVo vo = projectMapper.selectVoById(project.getId());
        if (vo == null) {
            throw new ServiceException("项目不存在");
        }
        Activity activity = activityMapper.selectById(vo.getActivityId());
        ActivityCategory category = categoryMapper.selectById(vo.getCategoryId());
        if (activity != null) {
            vo.setActivityName(activity.getActivityName());
        }
        if (category != null) {
            vo.setCategoryName(category.getCategoryName());
            vo.setCategoryRuleJson(category.getRuleJson());
            vo.setCategoryTipText(category.getTipText());
            vo.setCategoryCheckMode(category.getCheckMode());
        }
        List<ProjectFileVo> files = projectFileMapper.selectVoList(Wrappers.lambdaQuery(ProjectFile.class)
            .eq(ProjectFile::getProjectId, vo.getId())
            .eq(ProjectFile::getStatus, ArtReviewConstants.FILE_ACTIVE)
            .orderByAsc(ProjectFile::getRequirementId)
            .orderByAsc(ProjectFile::getVersionNo));
        ReviewAssignmentCategoryConfig categoryConfig = assignmentCategoryConfig(category);
        boolean categoryVisibility = Boolean.TRUE.equals(categoryConfig.getVisibilityConfigured());
        String effectiveHiddenFieldKeysJson = categoryVisibility
            ? categoryConfig.getHiddenFieldKeysJson()
            : assignment.getHiddenFieldKeysJson();
        Set<Long> hiddenRequirementIds = hiddenFileRequirementIds(parseStringSet(effectiveHiddenFieldKeysJson));
        if (!hiddenRequirementIds.isEmpty()) {
            files = files.stream()
                .filter(file -> file.getRequirementId() == null || !hiddenRequirementIds.contains(file.getRequirementId()))
                .toList();
        }
        refreshProjectFileAccessUrls(files);
        vo.setFiles(files);
        vo.setAuditRecords(auditRecordMapper.selectVoList(Wrappers.lambdaQuery(ProjectAuditRecord.class)
            .eq(ProjectAuditRecord::getProjectId, vo.getId())
            .orderByDesc(ProjectAuditRecord::getAuditedAt)));
        vo.setFieldSchemas(fieldMapper.selectVoList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
            .eq(CategoryFieldSchema::getCategoryId, vo.getCategoryId())
            .orderByAsc(CategoryFieldSchema::getSortOrder)
            .orderByAsc(CategoryFieldSchema::getId)));
        List<CategoryFileRequirementVo> fileRequirements = fileRequirementMapper.selectVoList(Wrappers.lambdaQuery(CategoryFileRequirement.class)
            .eq(CategoryFileRequirement::getCategoryId, vo.getCategoryId())
            .orderByAsc(CategoryFileRequirement::getSortOrder)
            .orderByAsc(CategoryFileRequirement::getId));
        if (!hiddenRequirementIds.isEmpty()) {
            fileRequirements = fileRequirements.stream().filter(item -> !hiddenRequirementIds.contains(item.getId())).toList();
        }
        vo.setFileRequirements(fileRequirements);
        boolean hideMemberInfo = categoryVisibility
            ? Boolean.TRUE.equals(categoryConfig.getHideMemberInfo())
            : Boolean.TRUE.equals(assignment.getHideMemberInfo());
        if (hideMemberInfo) {
            vo.setMembers(List.of());
        } else {
            vo.setMembers(projectMemberMapper.selectVoList(Wrappers.lambdaQuery(ProjectMember.class)
                .eq(ProjectMember::getProjectId, vo.getId())
                .orderByAsc(ProjectMember::getMemberType)
                .orderByAsc(ProjectMember::getSortOrder)
                .orderByAsc(ProjectMember::getId)));
        }
        applyVisibility(vo, effectiveHiddenFieldKeysJson);
        return vo;
    }

    private void refreshProjectFileAccessUrls(List<ProjectFileVo> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        Set<Long> ossIds = new java.util.LinkedHashSet<>();
        for (ProjectFileVo file : files) {
            if (file.getOssId() != null) {
                ossIds.add(file.getOssId());
            }
            if (file.getPreviewOssId() != null) {
                ossIds.add(file.getPreviewOssId());
            }
        }
        if (ossIds.isEmpty()) {
            return;
        }
        Map<Long, String> signedUrls = ossService.listByIds(ossIds).stream()
            .filter(oss -> oss.getOssId() != null && StringUtils.isNotBlank(oss.getUrl()))
            .collect(Collectors.toMap(SysOssVo::getOssId, SysOssVo::getUrl, (left, right) -> left));
        for (ProjectFileVo file : files) {
            if (file.getOssId() != null && signedUrls.containsKey(file.getOssId())) {
                file.setStoragePath(signedUrls.get(file.getOssId()));
            }
            if (file.getPreviewOssId() != null && signedUrls.containsKey(file.getPreviewOssId())) {
                file.setPreviewPath(signedUrls.get(file.getPreviewOssId()));
            }
        }
    }

    private void applyVisibility(ProjectVo vo, String hiddenFieldKeysJson) {
        // 评分任务统一隐藏学校身份；分配中的 hideSchoolInfo 保留给历史配置兼容，不能放宽该默认匿名边界。
        vo.setSchoolId(null);
        vo.setSchoolName(null);
        vo.setSubmittedBy(null);
        Set<String> hiddenFields = parseStringSet(hiddenFieldKeysJson);
        Set<String> effectiveHiddenFields = new HashSet<>(hiddenFields);
        if (vo.getFieldSchemas() != null) {
            vo.getFieldSchemas().stream()
                .filter(field -> isSchoolIdentityField(field.getFieldKey(), field.getFieldLabel()))
                .map(CategoryFieldSchemaVo::getFieldKey)
                .filter(StringUtils::isNotBlank)
                .forEach(effectiveHiddenFields::add);
        }
        if (hiddenFields.contains(HIDE_ALL_FORM_FIELDS) || hiddenFields.containsAll(LEGACY_HIDE_ALL_FORM_FIELD_KEYS)) {
            vo.setFormDataJson(JsonUtils.toJsonString(Map.of()));
            vo.setFieldSchemas(List.of());
            return;
        }
        if (StringUtils.isBlank(vo.getFormDataJson()) || !JsonUtils.isJsonObject(vo.getFormDataJson())) {
            vo.setFieldSchemas(vo.getFieldSchemas() == null ? List.of() : vo.getFieldSchemas().stream()
                .filter(field -> !effectiveHiddenFields.contains(field.getFieldKey()))
                .toList());
            return;
        }
        Map<String, Object> form = JsonUtils.parseObject(vo.getFormDataJson(), new TypeReference<Map<String, Object>>() {
        });
        if (form == null || form.isEmpty()) {
            vo.setFieldSchemas(vo.getFieldSchemas() == null ? List.of() : vo.getFieldSchemas().stream()
                .filter(field -> !effectiveHiddenFields.contains(field.getFieldKey()))
                .toList());
            return;
        }
        if (hiddenFields.stream().anyMatch(TEACHER_FIELD_ALIASES::contains)) {
            effectiveHiddenFields.add("adviserTeachers");
            if (vo.getFieldSchemas() != null) {
                vo.getFieldSchemas().stream()
                    .filter(field -> "teacher_group".equals(field.getFieldType()))
                    .map(CategoryFieldSchemaVo::getFieldKey)
                    .filter(StringUtils::isNotBlank)
                    .forEach(effectiveHiddenFields::add);
            }
        }
        Map<String, Object> masked = new LinkedHashMap<>(form);
        effectiveHiddenFields.forEach(masked::remove);
        vo.setFormDataJson(JsonUtils.toJsonString(masked));
        List<CategoryFieldSchemaVo> visibleSchemas = vo.getFieldSchemas() == null ? List.of() : vo.getFieldSchemas().stream()
            .filter(field -> !effectiveHiddenFields.contains(field.getFieldKey()))
            .toList();
        vo.setFieldSchemas(visibleSchemas);
    }

    private boolean isSchoolIdentityField(String fieldKey, String fieldLabel) {
        String key = StringUtils.lowerCase(StringUtils.trimToEmpty(fieldKey));
        String label = StringUtils.trimToEmpty(fieldLabel);
        return Set.of("school", "schoolid", "school_id", "schoolname", "school_name", "schoolcode", "school_code", "unit", "unitname", "unit_name")
            .contains(key)
            || label.contains("学校")
            || label.contains("单位")
            || label.contains("院校");
    }

    private ActivityCategory requireCategory(Long categoryId) {
        ActivityCategory category = categoryId == null ? null : categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new ServiceException("类别不存在");
        }
        return category;
    }

    private ReviewAssignmentCategoryConfig assignmentCategoryConfig(ActivityCategory category) {
        Object raw = parseObject(category == null ? null : category.getRuleJson()).get(REVIEW_ASSIGNMENT_CONFIG_KEY);
        if (raw == null) {
            return new ReviewAssignmentCategoryConfig();
        }
        ReviewAssignmentCategoryConfig parsed = JsonUtils.parseObject(
            JsonUtils.toJsonString(raw),
            new TypeReference<ReviewAssignmentCategoryConfig>() {
            }
        );
        ReviewAssignmentCategoryConfig result = parsed == null ? new ReviewAssignmentCategoryConfig() : parsed;
        result.setScopeFieldKeys(normalizeFilterValues(result.getScopeFieldKeys()));
        return result;
    }

    private List<String> normalizeScopeFieldKeys(Long categoryId, List<String> fieldKeys) {
        List<String> normalized = normalizeFilterValues(fieldKeys);
        if (normalized.size() > 8) {
            throw new ServiceException("评审单元拆分字段最多选择8个");
        }
        if (normalized.isEmpty()) {
            return List.of();
        }
        Map<String, CategoryFieldSchema> allowed = fieldMapper.selectList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
                .eq(CategoryFieldSchema::getCategoryId, categoryId))
            .stream()
            .filter(this::isProjectFilterField)
            .filter(field -> StringUtils.isNotBlank(field.getFieldKey()))
            .collect(Collectors.toMap(CategoryFieldSchema::getFieldKey, field -> field, (left, right) -> left));
        for (String fieldKey : normalized) {
            if (!allowed.containsKey(fieldKey)) {
                throw new ServiceException("拆分字段不属于当前类别或没有固定选项：" + fieldKey);
            }
        }
        return normalized;
    }

    private ReviewAssignmentCategoryConfigVo buildAssignmentCategoryConfigVo(
        ActivityCategory category,
        ReviewAssignmentCategoryConfig config
    ) {
        ReviewAssignmentCategoryConfigVo vo = new ReviewAssignmentCategoryConfigVo();
        vo.setActivityId(category.getActivityId());
        vo.setCategoryId(category.getId());
        vo.setCategoryName(category.getCategoryName());
        vo.setScopeFieldKeys(config.getScopeFieldKeys());
        vo.setVisibilityConfigured(Boolean.TRUE.equals(config.getVisibilityConfigured()));
        vo.setHideSchoolInfo(Boolean.TRUE.equals(config.getHideSchoolInfo()));
        vo.setHideMemberInfo(Boolean.TRUE.equals(config.getHideMemberInfo()));
        vo.setHiddenFieldKeysJson(config.getHiddenFieldKeysJson());
        vo.setScopeUnits(buildAssignmentScopeUnits(category, config.getScopeFieldKeys()));
        return vo;
    }

    private List<ReviewAssignmentScopeUnitVo> buildAssignmentScopeUnits(ActivityCategory category, List<String> scopeFieldKeys) {
        List<Project> projects = projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
            .eq(Project::getActivityId, category.getActivityId())
            .eq(Project::getCategoryId, category.getId())
            .eq(Project::getStatus, ArtReviewConstants.PROJECT_AUDIT_PASSED)
            .orderByAsc(Project::getProjectNo)
            .orderByAsc(Project::getId));
        if (projects.isEmpty()) {
            return List.of();
        }
        List<String> fieldKeys = scopeFieldKeys == null ? List.of() : scopeFieldKeys;
        Map<String, CategoryFieldSchema> fieldMap = fieldMapper.selectList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
                .eq(CategoryFieldSchema::getCategoryId, category.getId()))
            .stream()
            .filter(field -> StringUtils.isNotBlank(field.getFieldKey()))
            .collect(Collectors.toMap(CategoryFieldSchema::getFieldKey, field -> field, (left, right) -> left));
        Map<String, Map<String, String>> valuesByUnit = new LinkedHashMap<>();
        Map<String, List<Project>> projectsByUnit = new LinkedHashMap<>();
        for (Project project : projects) {
            for (Map<String, String> values : projectScopePaths(project, fieldKeys)) {
                String unitKey = JsonUtils.toJsonString(values);
                valuesByUnit.putIfAbsent(unitKey, values);
                projectsByUnit.computeIfAbsent(unitKey, ignored -> new ArrayList<>()).add(project);
            }
        }
        List<ReviewAssignment> assignments = assignmentMapper.selectList(Wrappers.lambdaQuery(ReviewAssignment.class)
            .eq(ReviewAssignment::getActivityId, category.getActivityId())
            .eq(ReviewAssignment::getCategoryId, category.getId())
            .eq(ReviewAssignment::getStatus, ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE));
        int requiredReviewerCount = assignments.stream()
            .mapToInt(assignment -> minimumReviewerCount(assignment.getScoreRuleJson()))
            .max()
            .orElse(0);
        List<ReviewAssignmentScopeUnitVo> rows = new ArrayList<>();
        for (Map.Entry<String, List<Project>> entry : projectsByUnit.entrySet()) {
            Map<String, String> values = valuesByUnit.get(entry.getKey());
            List<Integer> coverages = entry.getValue().stream()
                .map(project -> (int) assignments.stream()
                    .filter(assignment -> assignmentMatchesProject(assignment, project))
                    .map(ReviewAssignment::getReviewerUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count())
                .toList();
            int minimumCoverage = coverages.stream().min(Integer::compareTo).orElse(0);
            int maximumCoverage = coverages.stream().max(Integer::compareTo).orElse(0);
            long shortageCount = requiredReviewerCount <= 0
                ? 0
                : coverages.stream().filter(count -> count < requiredReviewerCount).count();
            ReviewAssignmentScopeUnitVo row = new ReviewAssignmentScopeUnitVo();
            row.setUnitKey(entry.getKey());
            row.setFieldValues(values);
            row.setPathLabel(scopePathLabel(values, fieldKeys, fieldMap));
            row.setProjectCount((long) entry.getValue().size());
            row.setRequiredReviewerCount(requiredReviewerCount);
            row.setMinimumAssignedReviewerCount(minimumCoverage);
            row.setMaximumAssignedReviewerCount(maximumCoverage);
            row.setShortageProjectCount(shortageCount);
            row.setAssignmentStatus(minimumCoverage == 0 ? "unassigned" : shortageCount > 0 ? "shortage" : "assigned");
            rows.add(row);
        }
        rows.sort(Comparator.comparing(ReviewAssignmentScopeUnitVo::getPathLabel, Comparator.nullsLast(String::compareTo)));
        return rows;
    }

    private List<Map<String, String>> projectScopePaths(Project project, List<String> fieldKeys) {
        List<Map<String, String>> paths = new ArrayList<>();
        paths.add(new LinkedHashMap<>());
        for (String fieldKey : fieldKeys) {
            List<String> values = scopeValues(projectFieldValue(project, fieldKey));
            List<Map<String, String>> expanded = new ArrayList<>();
            for (Map<String, String> path : paths) {
                for (String value : values) {
                    Map<String, String> next = new LinkedHashMap<>(path);
                    next.put(fieldKey, value);
                    expanded.add(next);
                }
            }
            paths = expanded;
        }
        return paths;
    }

    private List<String> scopeValues(Object value) {
        if (value instanceof Iterable<?> iterable) {
            List<String> values = new ArrayList<>();
            for (Object item : iterable) {
                if (item != null && StringUtils.isNotBlank(String.valueOf(item))) {
                    values.add(StringUtils.trim(String.valueOf(item)));
                }
            }
            return values.isEmpty() ? List.of(FILTER_UNFILLED_VALUE) : values.stream().distinct().toList();
        }
        return value == null || StringUtils.isBlank(String.valueOf(value))
            ? List.of(FILTER_UNFILLED_VALUE)
            : List.of(StringUtils.trim(String.valueOf(value)));
    }

    private String scopePathLabel(
        Map<String, String> values,
        List<String> fieldKeys,
        Map<String, CategoryFieldSchema> fieldMap
    ) {
        if (fieldKeys.isEmpty()) {
            return "全部项目";
        }
        return fieldKeys.stream().map(fieldKey -> {
            CategoryFieldSchema field = fieldMap.get(fieldKey);
            String label = field == null ? fieldKey : StringUtils.blankToDefault(field.getFieldLabel(), fieldKey);
            String value = FILTER_UNFILLED_VALUE.equals(values.get(fieldKey)) ? "未填写" : values.get(fieldKey);
            return label + "：" + StringUtils.blankToDefault(value, "未填写");
        }).collect(Collectors.joining(" / "));
    }

    private Map<String, Object> parseObject(String text) {
        if (StringUtils.isBlank(text) || !JsonUtils.isJsonObject(text)) {
            return Map.of();
        }
        Map<String, Object> parsed = JsonUtils.parseObject(text, new TypeReference<Map<String, Object>>() {
        });
        return parsed == null ? Map.of() : parsed;
    }

    private boolean jsonObjectEquals(String left, String right) {
        return Objects.equals(parseObject(left), parseObject(right));
    }

    private ReviewAssignmentProjectFilter parseProjectFilter(String text) {
        if (StringUtils.isBlank(text)) {
            return new ReviewAssignmentProjectFilter();
        }
        if (!JsonUtils.isJsonObject(text)) {
            throw new ServiceException("项目筛选标签必须是 JSON 对象");
        }
        ReviewAssignmentProjectFilter parsed = JsonUtils.parseObject(text, new TypeReference<ReviewAssignmentProjectFilter>() {
        });
        ReviewAssignmentProjectFilter result = parsed == null ? new ReviewAssignmentProjectFilter() : parsed;
        result.setSchoolTypes(normalizeFilterValues(result.getSchoolTypes()));
        Map<String, List<String>> normalizedFieldValues = new LinkedHashMap<>();
        if (result.getFieldValues() != null) {
            result.getFieldValues().forEach((fieldKey, values) -> {
                List<String> normalizedValues = normalizeFilterValues(values);
                if (StringUtils.isNotBlank(fieldKey) && !normalizedValues.isEmpty()) {
                    normalizedFieldValues.put(StringUtils.trim(fieldKey), normalizedValues);
                }
            });
        }
        result.setFieldValues(normalizedFieldValues);
        List<ReviewAssignmentProjectScopeGroup> normalizedGroups = new ArrayList<>();
        if (result.getScopeGroups() != null) {
            if (result.getScopeGroups().size() > 200) {
                throw new ServiceException("精确评审单元一次最多配置200组");
            }
            for (ReviewAssignmentProjectScopeGroup group : result.getScopeGroups()) {
                if (group == null) {
                    continue;
                }
                ReviewAssignmentProjectScopeGroup normalized = new ReviewAssignmentProjectScopeGroup();
                normalized.setSchoolTypes(normalizeFilterValues(group.getSchoolTypes()));
                Map<String, List<String>> groupFieldValues = new LinkedHashMap<>();
                if (group.getFieldValues() != null) {
                    group.getFieldValues().forEach((fieldKey, values) -> {
                        List<String> normalizedValues = normalizeFilterValues(values);
                        if (StringUtils.isNotBlank(fieldKey) && !normalizedValues.isEmpty()) {
                            groupFieldValues.put(StringUtils.trim(fieldKey), normalizedValues);
                        }
                    });
                }
                normalized.setFieldValues(groupFieldValues);
                if (!normalized.getSchoolTypes().isEmpty() || !normalized.getFieldValues().isEmpty()) {
                    normalizedGroups.add(normalized);
                }
            }
        }
        result.setScopeGroups(normalizedGroups);
        return result;
    }

    private String normalizeProjectFilterJson(String text) {
        ReviewAssignmentProjectFilter filter = parseProjectFilter(text);
        return filter.getSchoolTypes().isEmpty() && filter.getFieldValues().isEmpty() && filter.getScopeGroups().isEmpty()
            ? null
            : JsonUtils.toJsonString(filter);
    }

    private String mergeBatchProjectFilterJson(String existingText, String incomingText, String operation) {
        if (!ArtReviewConstants.ASSIGNMENT_BATCH_MERGE.equals(operation)) {
            return incomingText;
        }
        ReviewAssignmentProjectFilter incoming = parseProjectFilter(incomingText);
        if (incoming.getScopeGroups().isEmpty()) {
            return incomingText;
        }
        ReviewAssignmentProjectFilter existing = parseProjectFilter(existingText);
        if (existing.getSchoolTypes().isEmpty() && existing.getFieldValues().isEmpty() && existing.getScopeGroups().isEmpty()) {
            // The reviewer already covers the whole category, so adding a narrower unit must not reduce that scope.
            return existingText;
        }
        if (!existing.getSchoolTypes().isEmpty() || !existing.getFieldValues().isEmpty()
            || !incoming.getSchoolTypes().isEmpty() || !incoming.getFieldValues().isEmpty()
            || existing.getScopeGroups().isEmpty()) {
            // Legacy compound filters keep their established replace-on-update semantics.
            return incomingText;
        }
        ReviewAssignmentProjectFilter merged = new ReviewAssignmentProjectFilter();
        LinkedHashSet<ReviewAssignmentProjectScopeGroup> groups = new LinkedHashSet<>(existing.getScopeGroups());
        groups.addAll(incoming.getScopeGroups());
        merged.setScopeGroups(new ArrayList<>(groups));
        return JsonUtils.toJsonString(merged);
    }

    private List<String> normalizeFilterValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
            .filter(StringUtils::isNotBlank)
            .map(StringUtils::trim)
            .distinct()
            .toList();
    }

    private boolean projectMatchesFilter(Project project, ReviewAssignmentProjectFilter filter, org.dromara.crehn.domain.SchoolInfo school) {
        if (filter.getSchoolTypes().isEmpty() && filter.getFieldValues().isEmpty() && filter.getScopeGroups().isEmpty()) {
            return true;
        }
        if (!matchesProjectFilterCondition(project, filter.getSchoolTypes(), filter.getFieldValues(), school)) {
            return false;
        }
        return filter.getScopeGroups().isEmpty() || filter.getScopeGroups().stream()
            .anyMatch(group -> matchesProjectFilterCondition(project, group.getSchoolTypes(), group.getFieldValues(), school));
    }

    private boolean matchesProjectFilterCondition(
        Project project,
        List<String> schoolTypes,
        Map<String, List<String>> fieldValues,
        org.dromara.crehn.domain.SchoolInfo school
    ) {
        if (schoolTypes != null && !schoolTypes.isEmpty()) {
            org.dromara.crehn.domain.SchoolInfo targetSchool = school == null && project.getSchoolId() != null
                ? schoolInfoMapper.selectById(project.getSchoolId())
                : school;
            if (targetSchool == null || !schoolTypes.contains(StringUtils.trim(targetSchool.getSchoolType()))) {
                return false;
            }
        }
        if (fieldValues == null || fieldValues.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, List<String>> entry : fieldValues.entrySet()) {
            if (!matchesAnyFilterValue(projectFieldValue(project, entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private Object projectFieldValue(Project project, String fieldKey) {
        Object formValue = parseObject(project == null ? null : project.getFormDataJson()).get(fieldKey);
        if (formValue != null && StringUtils.isNotBlank(String.valueOf(formValue))) {
            return formValue;
        }
        if (project == null) {
            return null;
        }
        if (Set.of("groupName", "group_name", "group", "displayGroup", "display_group", "designGroup", "design_group",
            "projectNature", "project_nature").contains(fieldKey)) {
            return StringUtils.isNotBlank(project.getGroupName()) ? project.getGroupName() : project.getGroupCode();
        }
        if (Set.of("groupCode", "group_code").contains(fieldKey)) {
            return project.getGroupCode();
        }
        return null;
    }

    private boolean matchesAnyFilterValue(Object actualValue, List<String> expectedValues) {
        if (expectedValues == null || expectedValues.isEmpty()) {
            return false;
        }
        if (actualValue == null || StringUtils.isBlank(String.valueOf(actualValue))) {
            return expectedValues.contains(FILTER_UNFILLED_VALUE);
        }
        if (actualValue instanceof Iterable<?> values) {
            for (Object value : values) {
                if (expectedValues.contains(String.valueOf(value))) {
                    return true;
                }
            }
            return false;
        }
        return expectedValues.contains(String.valueOf(actualValue));
    }

    private int minimumReviewerCount(String scoreRuleJson) {
        Object value = parseObject(scoreRuleJson).get("minReviewers");
        if (value == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(String.valueOf(value)));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Set<String> parseStringSet(String text) {
        if (StringUtils.isBlank(text) || !JsonUtils.isJsonArray(text)) {
            return Set.of();
        }
        List<String> list = JsonUtils.parseObject(text, new TypeReference<List<String>>() {
        });
        return list == null ? Set.of() : list.stream().filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    }

    private Set<Long> parseProjectIdSet(String text) {
        return ReviewAssignmentProjectScopeSupport.parseProjectIds(text);
    }

    private void fillAssignmentNames(List<ReviewAssignmentVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, Activity> activityMap = activityMapper.selectBatchIds(rows.stream().map(ReviewAssignmentVo::getActivityId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(Activity::getId, item -> item, (left, right) -> left));
        Map<Long, ActivityCategory> categoryMap = categoryMapper.selectBatchIds(rows.stream().map(ReviewAssignmentVo::getCategoryId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left));
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(rows.stream().map(ReviewAssignmentVo::getReviewerUserId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(SysUser::getUserId, item -> item, (left, right) -> left));
        for (ReviewAssignmentVo row : rows) {
            Activity activity = activityMap.get(row.getActivityId());
            ActivityCategory category = categoryMap.get(row.getCategoryId());
            SysUser user = userMap.get(row.getReviewerUserId());
            row.setActivityName(activity == null ? null : activity.getActivityName());
            row.setCategoryName(category == null ? null : category.getCategoryName());
            ReviewAssignmentCategoryConfig categoryConfig = assignmentCategoryConfig(category);
            if (Boolean.TRUE.equals(categoryConfig.getVisibilityConfigured())) {
                row.setHideSchoolInfo(categoryConfig.getHideSchoolInfo());
                row.setHideMemberInfo(categoryConfig.getHideMemberInfo());
                row.setHiddenFieldKeysJson(categoryConfig.getHiddenFieldKeysJson());
            }
            row.setReviewerUserName(user == null ? null : user.getUserName());
            row.setReviewerNickName(user == null ? null : user.getNickName());
            long projectCount = projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
                .eq(Project::getActivityId, row.getActivityId())
                .eq(Project::getCategoryId, row.getCategoryId())
                .eq(Project::getStatus, ArtReviewConstants.PROJECT_AUDIT_PASSED))
                .stream()
                .filter(project -> assignmentMatchesProject(row, project))
                .count();
            row.setProjectCount(projectCount);
            row.setSubmittedCount(scoreMapper.selectCount(Wrappers.lambdaQuery(ReviewScore.class)
                .eq(ReviewScore::getAssignmentId, row.getId())
                .eq(ReviewScore::getStatus, ArtReviewConstants.REVIEW_SCORE_SUBMITTED)));
        }
    }

    private void fillScoreReviewerNames(List<ReviewScoreVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> reviewerUserIds = rows.stream().map(ReviewScoreVo::getReviewerUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, SysUser> userMap = reviewerUserIds.isEmpty()
            ? Map.of()
            : sysUserMapper.selectBatchIds(reviewerUserIds).stream().collect(Collectors.toMap(SysUser::getUserId, item -> item, (left, right) -> left));
        List<Long> projectIds = rows.stream().map(ReviewScoreVo::getProjectId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Project> projectMap = projectIds.isEmpty()
            ? Map.of()
            : projectMapper.selectBatchIds(projectIds).stream().collect(Collectors.toMap(Project::getId, item -> item, (left, right) -> left));
        List<Long> activityIds = projectMap.values().stream().map(Project::getActivityId).filter(Objects::nonNull).distinct().toList();
        List<Long> categoryIds = projectMap.values().stream().map(Project::getCategoryId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Activity> activityMap = activityIds.isEmpty()
            ? Map.of()
            : activityMapper.selectBatchIds(activityIds).stream().collect(Collectors.toMap(Activity::getId, item -> item, (left, right) -> left));
        Map<Long, ActivityCategory> categoryMap = categoryIds.isEmpty()
            ? Map.of()
            : categoryMapper.selectBatchIds(categoryIds).stream().collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left));
        for (ReviewScoreVo row : rows) {
            SysUser user = userMap.get(row.getReviewerUserId());
            row.setReviewerUserName(user == null ? null : user.getUserName());
            row.setReviewerNickName(user == null ? null : user.getNickName());
            Project project = projectMap.get(row.getProjectId());
            if (project != null) {
                row.setProjectNo(project.getProjectNo());
                row.setProjectName(project.getProjectName());
                row.setActivityId(project.getActivityId());
                row.setCategoryId(project.getCategoryId());
                Activity activity = activityMap.get(project.getActivityId());
                ActivityCategory category = categoryMap.get(project.getCategoryId());
                row.setActivityName(activity == null ? null : activity.getActivityName());
                row.setCategoryName(category == null ? null : category.getCategoryName());
            }
        }
    }
}
