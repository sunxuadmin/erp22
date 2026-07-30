package org.dromara.crehn.project.service.query;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.audit.service.IArtAuditAssignmentService;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.common.ArtReviewSecurity;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ReviewResult;
import org.dromara.crehn.domain.vo.ProjectCategoryStatsVo;
import org.dromara.crehn.domain.vo.ProjectVo;
import org.dromara.crehn.domain.vo.SchoolDashboardProjectVo;
import org.dromara.crehn.domain.vo.SchoolDashboardResultVo;
import org.dromara.crehn.domain.vo.SchoolDashboardVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.ReviewResultMapper;
import org.dromara.crehn.project.service.assembler.ProjectDetailAssembler;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ART-OWNER: BE.PROJECT.QUERY
 *
 * Read-only project list/statistics queries. The original service remains the
 * public facade so controllers, permissions and response contracts stay stable.
 */
@RequiredArgsConstructor
@Service
public class ProjectQueryService {

    private static final String AUDIT_STATUS_ALL = "all";
    private static final String AUDIT_STATUS_AUDITED = "audited";

    private final ProjectMapper projectMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final ReviewResultMapper reviewResultMapper;
    private final ArtReviewSecurity artReviewSecurity;
    private final IArtAuditAssignmentService auditAssignmentService;
    private final ProjectDetailAssembler projectDetailAssembler;

    public TableDataInfo<ProjectVo> queryMyProjectPage(ProjectVo query, PageQuery pageQuery) {
        Long schoolId = artReviewSecurity.requireEnabledSchoolId();
        LambdaQueryWrapper<Project> lqw = buildProjectQuery(query);
        lqw.eq(Project::getSchoolId, schoolId);
        if (artReviewSecurity.isCurrentParticipant()) {
            lqw.eq(Project::getParticipantUserId, org.dromara.common.satoken.utils.LoginHelper.getUserId());
        }
        lqw.ne(Project::getStatus, ArtReviewConstants.PROJECT_RECYCLED);
        Page<ProjectVo> page = projectMapper.selectVoPage(pageQuery.build(), lqw);
        projectDetailAssembler.fillProjectSchoolNames(page.getRecords());
        projectDetailAssembler.maskSensitiveFormData(page.getRecords());
        return TableDataInfo.build(page);
    }

    public List<ProjectCategoryStatsVo> queryMyCategoryStats(Long activityId, List<Long> categoryIds) {
        Long schoolId = artReviewSecurity.requireEnabledSchoolId();
        LambdaQueryWrapper<Project> lqw = Wrappers.lambdaQuery(Project.class)
            .select(Project::getCategoryId, Project::getStatus)
            .eq(Project::getSchoolId, schoolId)
            .eq(artReviewSecurity.isCurrentParticipant(), Project::getParticipantUserId,
                artReviewSecurity.isCurrentParticipant() ? org.dromara.common.satoken.utils.LoginHelper.getUserId() : null)
            .eq(activityId != null, Project::getActivityId, activityId)
            .in(CollUtil.isNotEmpty(categoryIds), Project::getCategoryId, categoryIds)
            .ne(Project::getStatus, ArtReviewConstants.PROJECT_RECYCLED);
        Map<Long, ProjectCategoryStatsVo> statsMap = new LinkedHashMap<>();
        projectMapper.selectList(lqw).forEach(project -> {
            if (project.getCategoryId() == null) {
                return;
            }
            ProjectCategoryStatsVo stats = statsMap.computeIfAbsent(project.getCategoryId(), categoryId -> {
                ProjectCategoryStatsVo item = new ProjectCategoryStatsVo();
                item.setCategoryId(categoryId);
                item.setTotalCount(0L);
                item.setDraftCount(0L);
                item.setSubmittedCount(0L);
                item.setCompletedCount(0L);
                return item;
            });
            stats.setTotalCount(stats.getTotalCount() + 1);
            if (ArtReviewConstants.PROJECT_DRAFT.equals(project.getStatus())) {
                stats.setDraftCount(stats.getDraftCount() + 1);
            }
            if (isSubmissionPipelineStatus(project.getStatus())) {
                stats.setSubmittedCount(stats.getSubmittedCount() + 1);
            }
            if (ArtReviewConstants.PROJECT_AUDIT_PASSED.equals(project.getStatus())) {
                stats.setCompletedCount(stats.getCompletedCount() + 1);
            }
        });
        return new ArrayList<>(statsMap.values());
    }

    /**
     * 首页学校数据看板的单次只读聚合。项目和结果均由当前学校范围约束，
     * 已发布结果的分数、等级、排名继续使用发布时的可见性开关。
     */
    public SchoolDashboardVo queryMyDashboard(Long activityId) {
        Long schoolId = artReviewSecurity.requireEnabledSchoolId();
        List<Project> projects = projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
            .eq(Project::getSchoolId, schoolId)
            .eq(artReviewSecurity.isCurrentParticipant(), Project::getParticipantUserId,
                artReviewSecurity.isCurrentParticipant() ? org.dromara.common.satoken.utils.LoginHelper.getUserId() : null)
            .eq(activityId != null, Project::getActivityId, activityId)
            .ne(Project::getStatus, ArtReviewConstants.PROJECT_RECYCLED));
        Map<Long, Project> projectMap = projects.stream()
            .filter(project -> project.getId() != null)
            .collect(Collectors.toMap(Project::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<Long, String> categoryNames = resolveCategoryNames(projects);

        SchoolDashboardVo dashboard = new SchoolDashboardVo();
        dashboard.setTotalCount((long) projects.size());
        dashboard.setDraftCount(countProjectStatus(projects, ArtReviewConstants.PROJECT_DRAFT));
        dashboard.setSubmittedCount(projects.stream().filter(project -> isSubmissionPipelineStatus(project.getStatus())).count());
        dashboard.setReturnedCount(projects.stream().filter(project ->
            ArtReviewConstants.PROJECT_RETURNED.equals(project.getStatus())
                || ArtReviewConstants.PROJECT_PARTICIPANT_RETURNED.equals(project.getStatus())).count());
        dashboard.setPassedCount(countProjectStatus(projects, ArtReviewConstants.PROJECT_AUDIT_PASSED));
        dashboard.setRecentEdited(toDashboardProjectItems(projects, Project::getUpdateTime, categoryNames));
        dashboard.setRecentSubmitted(toDashboardProjectItems(projects.stream()
            .filter(project -> project.getSubmittedAt() != null)
            .toList(), Project::getSubmittedAt, categoryNames));
        dashboard.setRecentPassed(toDashboardProjectItems(projects.stream()
            .filter(project -> ArtReviewConstants.PROJECT_AUDIT_PASSED.equals(project.getStatus()))
            .toList(), Project::getUpdateTime, categoryNames));

        List<ReviewResult> publishedResults = reviewResultMapper.selectList(Wrappers.lambdaQuery(ReviewResult.class)
            .eq(ReviewResult::getSchoolId, schoolId)
            .eq(activityId != null, ReviewResult::getActivityId, activityId)
            .eq(ReviewResult::getResultStatus, ArtReviewConstants.RESULT_PUBLISHED)
            .orderByDesc(ReviewResult::getPublishedAt));
        dashboard.setPublishedResultCount((long) publishedResults.size());
        dashboard.setRecentResults(publishedResults.stream().limit(6)
            .map(result -> toDashboardResultItem(result, projectMap, categoryNames))
            .toList());
        return dashboard;
    }

    private long countProjectStatus(List<Project> projects, String status) {
        return projects.stream().filter(project -> Objects.equals(status, project.getStatus())).count();
    }

    private boolean isSubmissionPipelineStatus(String status) {
        return ArtReviewConstants.PROJECT_PARTICIPANT_SUBMITTED.equals(status)
            || ArtReviewConstants.PROJECT_SCHOOL_APPROVED.equals(status)
            || ArtReviewConstants.PROJECT_SUBMITTED.equals(status);
    }

    private Map<Long, String> resolveCategoryNames(List<Project> projects) {
        List<Long> categoryIds = projects.stream().map(Project::getCategoryId).filter(Objects::nonNull).distinct().toList();
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectBatchIds(categoryIds).stream()
            .filter(category -> category.getId() != null)
            .collect(Collectors.toMap(ActivityCategory::getId, category -> StringUtils.blankToDefault(category.getCategoryName(), "未分类"),
                (left, right) -> left, HashMap::new));
    }

    private List<SchoolDashboardProjectVo> toDashboardProjectItems(List<Project> projects, Function<Project, Date> timeGetter,
                                                                     Map<Long, String> categoryNames) {
        return projects.stream()
            .sorted(Comparator.comparing(timeGetter, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(6)
            .map(project -> {
                SchoolDashboardProjectVo item = new SchoolDashboardProjectVo();
                item.setProjectId(project.getId());
                item.setProjectName(StringUtils.blankToDefault(project.getProjectName(), "未命名项目"));
                item.setCategoryName(categoryNames.getOrDefault(project.getCategoryId(), "未分类"));
                item.setStatus(project.getStatus());
                item.setTime(timeGetter.apply(project));
                return item;
            })
            .toList();
    }

    private SchoolDashboardResultVo toDashboardResultItem(ReviewResult result, Map<Long, Project> projectMap,
                                                            Map<Long, String> categoryNames) {
        Project project = projectMap.get(result.getProjectId());
        SchoolDashboardResultVo item = new SchoolDashboardResultVo();
        item.setProjectId(result.getProjectId());
        item.setProjectName(project == null ? "项目结果" : StringUtils.blankToDefault(project.getProjectName(), "未命名项目"));
        item.setCategoryName(project == null ? "未分类" : categoryNames.getOrDefault(project.getCategoryId(), "未分类"));
        item.setAwardLevel(result.getAwardLevel());
        item.setPublishedAt(result.getPublishedAt());
        if (Boolean.TRUE.equals(result.getShowRank())) {
            item.setRankNo(result.getRankNo());
        }
        if (Boolean.TRUE.equals(result.getShowScore())) {
            item.setAverageScore(result.getAverageScore());
            item.setFinalGrade(result.getFinalGrade());
        }
        return item;
    }

    public TableDataInfo<ProjectVo> queryAuditProjectPage(ProjectVo query, PageQuery pageQuery) {
        ProjectVo safeQuery = query == null ? new ProjectVo() : query;
        String auditStatus = safeQuery.getStatus();
        safeQuery.setStatus(baseAuditStatus(auditStatus));
        LambdaQueryWrapper<Project> lqw = buildProjectQuery(safeQuery);
        applyAuditStatusFilter(lqw, auditStatus);
        lqw.ne(Project::getStatus, ArtReviewConstants.PROJECT_RECYCLED);
        List<Project> visibleProjects = auditAssignmentService.filterVisibleProjects(projectMapper.selectList(lqw));
        List<ProjectVo> rows = visibleProjects.stream()
            .map(project -> projectMapper.selectVoById(project.getId()))
            .filter(Objects::nonNull)
            .toList();
        projectDetailAssembler.fillProjectSchoolNames(rows);
        projectDetailAssembler.maskSensitiveFormData(rows);
        return TableDataInfo.build(rows, pageQuery.build());
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

    private LambdaQueryWrapper<Project> buildProjectQuery(ProjectVo query) {
        ProjectVo safeQuery = query == null ? new ProjectVo() : query;
        LambdaQueryWrapper<Project> lqw = Wrappers.lambdaQuery();
        lqw.eq(safeQuery.getId() != null, Project::getId, safeQuery.getId());
        lqw.eq(safeQuery.getActivityId() != null, Project::getActivityId, safeQuery.getActivityId());
        lqw.eq(safeQuery.getCategoryId() != null, Project::getCategoryId, safeQuery.getCategoryId());
        lqw.eq(safeQuery.getSchoolId() != null, Project::getSchoolId, safeQuery.getSchoolId());
        lqw.in(CollUtil.isNotEmpty(safeQuery.getCategoryIds()), Project::getCategoryId, safeQuery.getCategoryIds());
        lqw.and(StringUtils.isNotBlank(safeQuery.getGroupCode()), wrapper -> wrapper
            .eq(Project::getGroupCode, safeQuery.getGroupCode())
            .or()
            .eq(Project::getGroupName, safeQuery.getGroupCode()));
        lqw.eq(StringUtils.isNotBlank(safeQuery.getStatus()), Project::getStatus, safeQuery.getStatus());
        lqw.like(StringUtils.isNotBlank(safeQuery.getProjectName()), Project::getProjectName, safeQuery.getProjectName());
        lqw.orderByDesc(Project::getCreateTime);
        return lqw;
    }
}
