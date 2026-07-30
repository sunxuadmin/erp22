package org.dromara.crehn.audit.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.audit.service.IArtAuditAssignmentService;
import org.dromara.crehn.common.AssignmentSchoolScopeSupport;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.AuditAssignment;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.bo.AuditAssignmentBo;
import org.dromara.crehn.domain.vo.ActivityCategoryVo;
import org.dromara.crehn.domain.vo.ActivityVo;
import org.dromara.crehn.domain.vo.AssignmentBatchPreviewVo;
import org.dromara.crehn.domain.vo.AuditAssignmentVo;
import org.dromara.crehn.domain.vo.SchoolInfoVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ActivityMapper;
import org.dromara.crehn.mapper.AuditAssignmentMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.SchoolInfoMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ArtAuditAssignmentServiceImpl implements IArtAuditAssignmentService {

    private static final String ROLE_KEY_AUDITOR = "crehn_auditor";
    private static final String ROLE_KEY_PROJECT_ADMIN = "project_admin";
    private static final String PERM_PROJECT_VIEW_ALL = "crehn:projectView:all";

    private final AuditAssignmentMapper assignmentMapper;
    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final ProjectMapper projectMapper;
    private final SchoolInfoMapper schoolInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public TableDataInfo<AuditAssignmentVo> queryPage(AuditAssignmentVo query, PageQuery pageQuery) {
        LambdaQueryWrapper<AuditAssignment> lqw = Wrappers.lambdaQuery(AuditAssignment.class)
            .eq(query.getActivityId() != null, AuditAssignment::getActivityId, query.getActivityId())
            .eq(query.getCategoryId() != null, AuditAssignment::getCategoryId, query.getCategoryId())
            .eq(query.getAuditorUserId() != null, AuditAssignment::getAuditorUserId, query.getAuditorUserId())
            .eq(StringUtils.isNotBlank(query.getStatus()), AuditAssignment::getStatus, query.getStatus())
            .orderByDesc(AuditAssignment::getCreateTime)
            .orderByDesc(AuditAssignment::getId);
        Page<AuditAssignmentVo> page = assignmentMapper.selectVoPage(pageQuery.build(), lqw);
        fillNames(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public AuditAssignmentVo getById(Long id) {
        AuditAssignmentVo vo = assignmentMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("审核授权不存在");
        }
        fillNames(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(AuditAssignmentBo bo) {
        if (bo == null) {
            throw new ServiceException("审核授权不能为空");
        }
        if (bo.getAuditorUserIds() != null || bo.getCategoryIds() != null) {
            bo.setBatchOperation(StringUtils.blankToDefault(bo.getBatchOperation(), ArtReviewConstants.ASSIGNMENT_BATCH_MERGE));
            maintainBatch(bo);
            return;
        }
        validateBo(bo);
        ensureNoDuplicate(null, bo.getActivityId(), bo.getCategoryId(), bo.getAuditorUserId());
        AuditAssignment assignment = new AuditAssignment();
        copy(assignment, bo, bo.getCategoryId(), bo.getAuditorUserId());
        assignment.setAssignedBy(LoginHelper.getUserId());
        assignment.setAssignedAt(new Date());
        assignmentMapper.insert(assignment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AuditAssignmentBo bo) {
        if (bo == null || bo.getId() == null) {
            throw new ServiceException("审核授权ID不能为空");
        }
        validateBo(bo);
        AuditAssignment assignment = assignmentMapper.selectById(bo.getId());
        if (assignment == null) {
            throw new ServiceException("审核授权不存在");
        }
        ensureNoDuplicate(bo.getId(), bo.getActivityId(), bo.getCategoryId(), bo.getAuditorUserId());
        copy(assignment, bo, bo.getCategoryId(), bo.getAuditorUserId());
        assignmentMapper.updateById(assignment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new ServiceException("审核授权ID不能为空");
        }
        for (Long id : ids) {
            AuditAssignment assignment = assignmentMapper.selectById(id);
            if (assignment != null && !ArtReviewConstants.AUDIT_ASSIGNMENT_DISABLED.equals(assignment.getStatus())) {
                assignment.setStatus(ArtReviewConstants.AUDIT_ASSIGNMENT_DISABLED);
                assignmentMapper.updateById(assignment);
            }
        }
    }

    @Override
    public AssignmentBatchPreviewVo previewBatch(AuditAssignmentBo bo) {
        return executeBatch(bo, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignmentBatchPreviewVo maintainBatch(AuditAssignmentBo bo) {
        String operation = normalizeBatchOperation(bo == null ? null : bo.getBatchOperation());
        checkBatchPermissions(operation);
        return executeBatch(bo, true);
    }

    @Override
    public List<ActivityVo> activityOptions() {
        LambdaQueryWrapper<Activity> wrapper = Wrappers.lambdaQuery(Activity.class)
            .eq(Activity::getStatus, ArtReviewConstants.ACTIVITY_ENABLED)
            .orderByDesc(Activity::getCreateTime)
            .orderByDesc(Activity::getId);
        if (!hasFullAuditScope()) {
            List<Long> activityIds = currentActiveScopes().stream()
                .map(AuditAssignment::getActivityId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
            if (activityIds.isEmpty()) {
                return List.of();
            }
            wrapper.in(Activity::getId, activityIds);
        }
        return activityMapper.selectVoList(wrapper);
    }

    @Override
    public List<ActivityCategoryVo> categoryOptions(Long activityId) {
        if (activityId == null) {
            return List.of();
        }
        List<ActivityCategoryVo> categories = categoryMapper.selectVoList(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getActivityId, activityId)
            .eq(ActivityCategory::getEnabled, true)
            .orderByAsc(ActivityCategory::getSortOrder)
            .orderByAsc(ActivityCategory::getId));
        if (categories.isEmpty() || hasFullAuditScope()) {
            return categories;
        }
        List<AuditAssignment> scopes = currentActiveScopes().stream()
            .filter(scope -> Objects.equals(scope.getActivityId(), activityId))
            .toList();
        if (scopes.isEmpty()) {
            return List.of();
        }
        if (scopes.stream().anyMatch(scope -> scope.getCategoryId() == null)) {
            return categories;
        }
        Set<Long> visibleIds = new LinkedHashSet<>(scopes.stream()
            .map(AuditAssignment::getCategoryId)
            .filter(Objects::nonNull)
            .toList());
        Map<Long, ActivityCategoryVo> categoryMap = categories.stream()
            .filter(category -> category.getId() != null)
            .collect(Collectors.toMap(ActivityCategoryVo::getId, item -> item, (left, right) -> left));
        for (Long categoryId : List.copyOf(visibleIds)) {
            ActivityCategoryVo current = categoryMap.get(categoryId);
            while (current != null && current.getParentId() != null) {
                visibleIds.add(current.getParentId());
                current = categoryMap.get(current.getParentId());
            }
        }
        return categories.stream()
            .filter(category -> category.getId() != null && visibleIds.contains(category.getId()))
            .toList();
    }

    @Override
    public List<SchoolInfoVo> schoolOptions(Long activityId, List<Long> categoryIds, String groupCode, String status, String projectName) {
        LambdaQueryWrapper<Project> projectWrapper = Wrappers.lambdaQuery(Project.class)
            .eq(activityId != null, Project::getActivityId, activityId)
            .in(categoryIds != null && !categoryIds.isEmpty(), Project::getCategoryId, categoryIds)
            .and(StringUtils.isNotBlank(groupCode), wrapper -> wrapper
                .eq(Project::getGroupCode, groupCode)
                .or()
                .eq(Project::getGroupName, groupCode))
            .like(StringUtils.isNotBlank(projectName), Project::getProjectName, projectName)
            .ne(Project::getStatus, ArtReviewConstants.PROJECT_RECYCLED)
            .orderByDesc(Project::getSubmittedAt)
            .orderByDesc(Project::getId);
        applyAuditStatus(projectWrapper, status);
        List<Project> visibleProjects = filterVisibleProjects(projectMapper.selectList(projectWrapper));
        Map<Long, Long> schoolCounts = visibleProjects.stream()
            .map(Project::getSchoolId)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        if (schoolCounts.isEmpty()) {
            return List.of();
        }
        List<SchoolInfoVo> schools = schoolInfoMapper.selectVoList(Wrappers.lambdaQuery(org.dromara.crehn.domain.SchoolInfo.class)
            .in(org.dromara.crehn.domain.SchoolInfo::getId, schoolCounts.keySet())
            .eq(org.dromara.crehn.domain.SchoolInfo::getStatus, "enabled")
            .orderByAsc(org.dromara.crehn.domain.SchoolInfo::getSchoolCode)
            .orderByAsc(org.dromara.crehn.domain.SchoolInfo::getId));
        schools.forEach(school -> {
            long visibleProjectCount = schoolCounts.getOrDefault(school.getId(), 0L);
            school.setVisibleProjectCount(visibleProjectCount);
        });
        return schools;
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
    public List<SysUserVo> auditorOptions(String keyword) {
        Set<Long> userIds = new HashSet<>();
        addRoleUsers(userIds, ROLE_KEY_AUDITOR);
        addRoleUsers(userIds, ROLE_KEY_PROJECT_ADMIN);
        List<SysUser> typedUsers = sysUserMapper.selectList(Wrappers.lambdaQuery(SysUser.class)
            .select(SysUser::getUserId)
            .in(SysUser::getUserType, "project_admin", "province"));
        typedUsers.forEach(user -> userIds.add(user.getUserId()));
        if (userIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<SysUser> lqw = Wrappers.lambdaQuery(SysUser.class)
            .in(SysUser::getUserId, userIds)
            .eq(SysUser::getStatus, "0")
            .and(StringUtils.isNotBlank(keyword), w -> w.like(SysUser::getUserName, keyword).or().like(SysUser::getNickName, keyword))
            .orderByAsc(SysUser::getUserName)
            .last("limit 500");
        return sysUserMapper.selectVoList(lqw);
    }

    @Override
    public List<Project> filterVisibleProjects(List<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            return List.of();
        }
        List<Project> auditableProjects = projects.stream()
            .filter(this::isAuditVisibleProject)
            .toList();
        if (hasFullAuditScope()) {
            return auditableProjects;
        }
        return filterProjectsByCurrentScopes(auditableProjects, "query");
    }

    @Override
    public List<Project> filterProjectViewVisibleProjects(List<Project> projects) {
        if (projects == null || projects.isEmpty() || hasFullProjectViewScope()) {
            return projects == null ? List.of() : projects;
        }
        return filterProjectsByCurrentScopes(projects, "query");
    }

    private List<Project> filterProjectsByCurrentScopes(List<Project> projects, String action) {
        List<AuditAssignment> scopes = currentActiveScopes();
        if (scopes.isEmpty()) {
            return List.of();
        }
        return projects.stream().filter(project -> isAllowed(project, scopes, action)).toList();
    }

    @Override
    public void checkProjectAction(Project project, String action) {
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        if (!isAuditVisibleProject(project)) {
            throw new ServiceException("草稿或非审核状态项目不能在项目审核中查看或操作");
        }
        if (hasFullAuditScope()) {
            return;
        }
        List<AuditAssignment> scopes = currentActiveScopes();
        if (scopes.isEmpty() || !isAllowed(project, scopes, action)) {
            throw new ServiceException("当前账号无权执行该审核操作");
        }
    }

    @Override
    public void checkProjectViewAction(Project project, String action) {
        if (project == null || hasFullProjectViewScope()) {
            return;
        }
        List<AuditAssignment> scopes = currentActiveScopes();
        if (scopes.isEmpty() || !isAllowed(project, scopes, action)) {
            throw new ServiceException("当前账号无权查看该申报");
        }
    }

    private boolean hasFullAuditScope() {
        return LoginHelper.isSuperAdmin() || StpUtil.hasPermission("crehn:auditScope:all");
    }

    private boolean hasFullProjectViewScope() {
        return hasFullAuditScope() || StpUtil.hasPermission(PERM_PROJECT_VIEW_ALL);
    }

    private List<AuditAssignment> currentActiveScopes() {
        return assignmentMapper.selectList(Wrappers.lambdaQuery(AuditAssignment.class)
            .eq(AuditAssignment::getAuditorUserId, LoginHelper.getUserId())
            .eq(AuditAssignment::getStatus, ArtReviewConstants.AUDIT_ASSIGNMENT_ACTIVE));
    }

    private boolean isAllowed(Project project, List<AuditAssignment> scopes, String action) {
        return scopes.stream().anyMatch(scope -> Objects.equals(scope.getActivityId(), project.getActivityId())
            && (scope.getCategoryId() == null || Objects.equals(scope.getCategoryId(), project.getCategoryId()))
            && AssignmentSchoolScopeSupport.matches(project.getSchoolId(), scope.getSchoolScopeMode(), scope.getSchoolIdsJson())
            && actionAllowed(scope, action));
    }

    private boolean isAuditVisibleProject(Project project) {
        return project != null && (ArtReviewConstants.PROJECT_SUBMITTED.equals(project.getStatus())
            || ArtReviewConstants.PROJECT_AUDIT_PASSED.equals(project.getStatus())
            || ArtReviewConstants.PROJECT_RETURNED.equals(project.getStatus()));
    }

    private boolean actionAllowed(AuditAssignment scope, String action) {
        return switch (StringUtils.blankToDefault(action, "query")) {
            case "pass" -> Boolean.TRUE.equals(scope.getAllowPass());
            case "return" -> Boolean.TRUE.equals(scope.getAllowReturn());
            case "withdraw_pass" -> Boolean.TRUE.equals(scope.getAllowWithdrawPass());
            case "withdraw_return" -> Boolean.TRUE.equals(scope.getAllowWithdrawReturn());
            case "download" -> Boolean.TRUE.equals(scope.getAllowDownload());
            default -> Boolean.TRUE.equals(scope.getAllowQuery());
        };
    }

    private void applyAuditStatus(LambdaQueryWrapper<Project> wrapper, String status) {
        String normalized = StringUtils.trimToEmpty(status);
        if ("all".equals(normalized)) {
            wrapper.in(Project::getStatus, ArtReviewConstants.PROJECT_SUBMITTED,
                ArtReviewConstants.PROJECT_AUDIT_PASSED, ArtReviewConstants.PROJECT_RETURNED);
            return;
        }
        if ("audited".equals(normalized)) {
            wrapper.in(Project::getStatus, ArtReviewConstants.PROJECT_AUDIT_PASSED, ArtReviewConstants.PROJECT_RETURNED);
            return;
        }
        if (StringUtils.isBlank(normalized)) {
            wrapper.eq(Project::getStatus, ArtReviewConstants.PROJECT_SUBMITTED);
            return;
        }
        if (ArtReviewConstants.PROJECT_SUBMITTED.equals(normalized)
            || ArtReviewConstants.PROJECT_AUDIT_PASSED.equals(normalized)
            || ArtReviewConstants.PROJECT_RETURNED.equals(normalized)) {
            wrapper.eq(Project::getStatus, normalized);
        } else {
            wrapper.eq(Project::getStatus, "__audit_status_not_allowed__");
        }
    }

    private void validateBo(AuditAssignmentBo bo) {
        if (bo == null) {
            throw new ServiceException("审核授权不能为空");
        }
        if (bo.getActivityId() == null || bo.getAuditorUserId() == null) {
            throw new ServiceException("活动和审核员不能为空");
        }
        validateActivity(bo.getActivityId());
        validateCategory(bo.getActivityId(), bo.getCategoryId());
        validateAuditor(bo.getAuditorUserId());
        validateSchoolScope(bo.getSchoolScopeMode(), bo.getSchoolIdsJson());
    }

    private void validateActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
    }

    private void validateCategory(Long activityId, Long categoryId) {
        if (categoryId == null) {
            return;
        }
        ActivityCategory category = categoryMapper.selectById(categoryId);
        if (category == null || !Objects.equals(category.getActivityId(), activityId)) {
            throw new ServiceException("类别不属于当前活动");
        }
    }

    private void validateAuditor(Long auditorUserId) {
        SysUser user = sysUserMapper.selectById(auditorUserId);
        if (user == null || !"0".equals(user.getStatus())) {
            throw new ServiceException("审核员账号不存在或已停用");
        }
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

    private AssignmentBatchPreviewVo executeBatch(AuditAssignmentBo bo, boolean apply) {
        if (bo == null) {
            throw new ServiceException("批量维护参数不能为空");
        }
        String operation = normalizeBatchOperation(bo.getBatchOperation());
        String target = StringUtils.blankToDefault(bo.getBatchTarget(), ArtReviewConstants.ASSIGNMENT_BATCH_TARGET_ASSIGNMENT);
        if (!ArtReviewConstants.ASSIGNMENT_BATCH_TARGET_ASSIGNMENT.equals(target)) {
            throw new ServiceException("审核权限只支持按账号和类别批量维护");
        }
        List<Long> auditorUserIds = normalizeIds(bo.getAuditorUserIds());
        List<Long> categoryIds = normalizeIds(bo.getCategoryIds());
        if (bo.getActivityId() == null || auditorUserIds.isEmpty() || categoryIds.isEmpty()) {
            throw new ServiceException("活动、至少一个类别和至少一个审核账号不能为空");
        }
        if (auditorUserIds.size() > 100 || categoryIds.size() > 100 || (long) auditorUserIds.size() * categoryIds.size() > 1000) {
            throw new ServiceException("一次最多生成1000条审核权限，请缩小账号或类别范围");
        }
        validateActivity(bo.getActivityId());
        if (!ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(operation)) {
            validateSchoolScope(bo.getSchoolScopeMode(), bo.getSchoolIdsJson());
        }
        categoryIds.forEach(categoryId -> validateCategory(bo.getActivityId(), categoryId));
        auditorUserIds.forEach(this::validateAuditor);

        List<AuditAssignment> existingRows = assignmentMapper.selectList(Wrappers.lambdaQuery(AuditAssignment.class)
            .eq(AuditAssignment::getActivityId, bo.getActivityId())
            .in(AuditAssignment::getAuditorUserId, auditorUserIds));
        Map<String, AuditAssignment> existingMap = existingRows.stream()
            .collect(Collectors.toMap(
                row -> assignmentKey(row.getAuditorUserId(), row.getCategoryId()),
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
            for (AuditAssignment row : existingRows) {
                if (!selectedCategoryIds.contains(row.getCategoryId())
                    && ArtReviewConstants.AUDIT_ASSIGNMENT_ACTIVE.equals(row.getStatus())) {
                    preview.setDisableCount(preview.getDisableCount() + 1);
                    if (apply) {
                        row.setStatus(ArtReviewConstants.AUDIT_ASSIGNMENT_DISABLED);
                        assignmentMapper.updateById(row);
                    }
                }
            }
        }

        for (Long auditorUserId : auditorUserIds) {
            for (Long categoryId : categoryIds) {
                AuditAssignment assignment = existingMap.get(assignmentKey(auditorUserId, categoryId));
                if (ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(operation)) {
                    if (assignment != null && ArtReviewConstants.AUDIT_ASSIGNMENT_ACTIVE.equals(assignment.getStatus())) {
                        preview.setDisableCount(preview.getDisableCount() + 1);
                        if (apply) {
                            assignment.setStatus(ArtReviewConstants.AUDIT_ASSIGNMENT_DISABLED);
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
                        assignment = new AuditAssignment();
                        copy(assignment, bo, categoryId, auditorUserId);
                        assignment.setStatus(ArtReviewConstants.AUDIT_ASSIGNMENT_ACTIVE);
                        assignment.setAssignedBy(assignedBy);
                        assignment.setAssignedAt(assignedAt);
                        assignmentMapper.insert(assignment);
                    }
                } else if (auditConfigMatches(assignment, bo)) {
                    preview.setUnchangedCount(preview.getUnchangedCount() + 1);
                } else {
                    preview.setUpdateCount(preview.getUpdateCount() + 1);
                    if (apply) {
                        copy(assignment, bo, categoryId, auditorUserId);
                        assignment.setStatus(ArtReviewConstants.AUDIT_ASSIGNMENT_ACTIVE);
                        assignment.setAssignedBy(assignedBy);
                        assignment.setAssignedAt(assignedAt);
                        assignmentMapper.updateById(assignment);
                    }
                }
            }
        }
        return preview;
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

    private void checkBatchPermissions(String operation) {
        if (ArtReviewConstants.ASSIGNMENT_BATCH_REMOVE.equals(operation)) {
            StpUtil.checkPermission("crehn:auditScope:remove");
            return;
        }
        StpUtil.checkPermission("crehn:auditScope:add");
        StpUtil.checkPermission("crehn:auditScope:edit");
        if (ArtReviewConstants.ASSIGNMENT_BATCH_SYNC.equals(operation)) {
            StpUtil.checkPermission("crehn:auditScope:remove");
        }
    }

    private boolean auditConfigMatches(AuditAssignment assignment, AuditAssignmentBo bo) {
        String normalizedMode = AssignmentSchoolScopeSupport.normalizeMode(bo.getSchoolScopeMode());
        Set<Long> expectedSchoolIds = AssignmentSchoolScopeSupport.parseSchoolIds(
            AssignmentSchoolScopeSupport.normalizeSchoolIdsJson(normalizedMode, bo.getSchoolIdsJson())
        );
        return ArtReviewConstants.AUDIT_ASSIGNMENT_ACTIVE.equals(assignment.getStatus())
            && Objects.equals(normalizedMode, AssignmentSchoolScopeSupport.normalizeMode(assignment.getSchoolScopeMode()))
            && Objects.equals(expectedSchoolIds, AssignmentSchoolScopeSupport.parseSchoolIds(assignment.getSchoolIdsJson()))
            && Objects.equals(!Boolean.FALSE.equals(bo.getAllowQuery()), assignment.getAllowQuery())
            && Objects.equals(Boolean.TRUE.equals(bo.getAllowPass()), assignment.getAllowPass())
            && Objects.equals(Boolean.TRUE.equals(bo.getAllowReturn()), assignment.getAllowReturn())
            && Objects.equals(Boolean.TRUE.equals(bo.getAllowWithdrawPass()), assignment.getAllowWithdrawPass())
            && Objects.equals(Boolean.TRUE.equals(bo.getAllowWithdrawReturn()), assignment.getAllowWithdrawReturn())
            && Objects.equals(Boolean.TRUE.equals(bo.getAllowDownload()), assignment.getAllowDownload());
    }

    private String assignmentKey(Long userId, Long categoryId) {
        return String.valueOf(userId) + ":" + String.valueOf(categoryId);
    }

    private AuditAssignment findAssignment(Long activityId, Long categoryId, Long auditorUserId) {
        return assignmentMapper.selectOne(Wrappers.lambdaQuery(AuditAssignment.class)
            .eq(AuditAssignment::getActivityId, activityId)
            .eq(AuditAssignment::getCategoryId, categoryId)
            .eq(AuditAssignment::getAuditorUserId, auditorUserId)
            .last("limit 1"));
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).filter(id -> id > 0).distinct().toList();
    }

    private void ensureNoDuplicate(Long id, Long activityId, Long categoryId, Long auditorUserId) {
        long count = assignmentMapper.selectCount(Wrappers.lambdaQuery(AuditAssignment.class)
            .ne(id != null, AuditAssignment::getId, id)
            .eq(AuditAssignment::getActivityId, activityId)
            .eq(categoryId != null, AuditAssignment::getCategoryId, categoryId)
            .isNull(categoryId == null, AuditAssignment::getCategoryId)
            .eq(AuditAssignment::getAuditorUserId, auditorUserId));
        if (count > 0) {
            throw new ServiceException("该审核员已分配到当前活动/类别");
        }
    }

    private void copy(AuditAssignment assignment, AuditAssignmentBo bo, Long categoryId, Long auditorUserId) {
        assignment.setActivityId(bo.getActivityId());
        assignment.setCategoryId(categoryId);
        assignment.setAuditorUserId(auditorUserId);
        assignment.setSchoolScopeMode(AssignmentSchoolScopeSupport.normalizeMode(bo.getSchoolScopeMode()));
        assignment.setSchoolIdsJson(AssignmentSchoolScopeSupport.normalizeSchoolIdsJson(bo.getSchoolScopeMode(), bo.getSchoolIdsJson()));
        assignment.setAllowQuery(!Boolean.FALSE.equals(bo.getAllowQuery()));
        assignment.setAllowPass(Boolean.TRUE.equals(bo.getAllowPass()));
        assignment.setAllowReturn(Boolean.TRUE.equals(bo.getAllowReturn()));
        assignment.setAllowWithdrawPass(Boolean.TRUE.equals(bo.getAllowWithdrawPass()));
        assignment.setAllowWithdrawReturn(Boolean.TRUE.equals(bo.getAllowWithdrawReturn()));
        assignment.setAllowDownload(Boolean.TRUE.equals(bo.getAllowDownload()));
        assignment.setStatus(StringUtils.blankToDefault(bo.getStatus(), ArtReviewConstants.AUDIT_ASSIGNMENT_ACTIVE));
    }

    private void addRoleUsers(Set<Long> userIds, String roleKey) {
        SysRole role = sysRoleMapper.selectOne(Wrappers.lambdaQuery(SysRole.class)
            .eq(SysRole::getRoleKey, roleKey)
            .last("limit 1"));
        if (role != null) {
            userIds.addAll(sysUserRoleMapper.selectUserIdsByRoleId(role.getRoleId()));
        }
    }

    private void fillNames(List<AuditAssignmentVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, Activity> activityMap = activityMapper.selectBatchIds(rows.stream().map(AuditAssignmentVo::getActivityId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(Activity::getId, item -> item, (left, right) -> left));
        List<Long> categoryIds = rows.stream().map(AuditAssignmentVo::getCategoryId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ActivityCategory> categoryMap = categoryIds.isEmpty()
            ? Map.of()
            : categoryMapper.selectBatchIds(categoryIds).stream().collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left));
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(rows.stream().map(AuditAssignmentVo::getAuditorUserId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(SysUser::getUserId, item -> item, (left, right) -> left));
        for (AuditAssignmentVo row : rows) {
            Activity activity = activityMap.get(row.getActivityId());
            ActivityCategory category = categoryMap.get(row.getCategoryId());
            SysUser user = userMap.get(row.getAuditorUserId());
            row.setActivityName(activity == null ? null : activity.getActivityName());
            row.setCategoryName(category == null ? "全部类别" : category.getCategoryName());
            row.setAuditorUserName(user == null ? null : user.getUserName());
            row.setAuditorNickName(user == null ? null : user.getNickName());
        }
    }
}
