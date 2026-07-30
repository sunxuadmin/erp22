package org.dromara.crehn.project.service.scope;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.common.ArtReviewSecurity;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ProjectFile;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ART-OWNER: BE.PROJECT.SCOPE_GUARD
 * 项目数据范围保护器，集中学校端收窄、查看状态权限和项目可见性判断。
 */
@RequiredArgsConstructor
@Service
public class ProjectScopeGuard {

    private static final String PERM_PROJECT_VIEW_DRAFT = "crehn:projectView:draft";
    private static final String PERM_PROJECT_VIEW_SUBMITTED = "crehn:projectView:submitted";
    private static final String PERM_PROJECT_VIEW_AUDITED = "crehn:projectView:audited";

    private final ProjectMapper projectMapper;
    private final ArtReviewSecurity artReviewSecurity;

    /**
     * 学校端范围判断保持“只在学校账号下收窄”的旧行为，管理员、审核员和查看员继续走原查询条件。
     */
    public void applySchoolScopeIfNeeded(LambdaQueryWrapper<Project> lqw) {
        if (isCurrentUserSchool()) {
            lqw.eq(Project::getSchoolId, artReviewSecurity.requireEnabledSchoolId());
        } else if (isCurrentUserParticipant()) {
            lqw.eq(Project::getParticipantUserId, LoginHelper.getUserId());
        }
    }

    /**
     * 文件表没有直接的学校字段，沿用原逻辑先查当前学校项目，再用项目 ID 收窄已删除文件列表。
     */
    public void applySchoolScopeToFileQueryIfNeeded(LambdaQueryWrapper<ProjectFile> lqw) {
        if (!isCurrentUserSchool() && !isCurrentUserParticipant()) {
            return;
        }
        LambdaQueryWrapper<Project> projectQuery = Wrappers.lambdaQuery(Project.class)
            .select(Project::getId);
        if (isCurrentUserParticipant()) {
            projectQuery.eq(Project::getParticipantUserId, LoginHelper.getUserId());
        } else {
            projectQuery.eq(Project::getSchoolId, artReviewSecurity.requireEnabledSchoolId());
        }
        List<Long> projectIds = projectMapper.selectList(projectQuery)
            .stream()
            .map(Project::getId)
            .toList();
        if (projectIds.isEmpty()) {
            lqw.eq(ProjectFile::getProjectId, -1L);
            return;
        }
        lqw.in(ProjectFile::getProjectId, projectIds);
    }

    public void checkProjectAccess(Project project) {
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        if (isCurrentUserSchool()) {
            artReviewSecurity.checkSchoolProject(project.getSchoolId());
        } else if (isCurrentUserParticipant()
            && !LoginHelper.getUserId().equals(project.getParticipantUserId())) {
            throw new ServiceException("无权访问其他参赛者作品");
        }
    }

    /**
     * 项目查看页的状态可见性仍只由原权限标识决定，避免本次拆分改变菜单授权语义。
     */
    public List<String> visibleProjectViewStatuses() {
        if (LoginHelper.isSuperAdmin()) {
            return List.of(
                ArtReviewConstants.PROJECT_DRAFT,
                ArtReviewConstants.PROJECT_PARTICIPANT_SUBMITTED,
                ArtReviewConstants.PROJECT_PARTICIPANT_RETURNED,
                ArtReviewConstants.PROJECT_SCHOOL_APPROVED,
                ArtReviewConstants.PROJECT_SUBMITTED,
                ArtReviewConstants.PROJECT_RETURNED,
                ArtReviewConstants.PROJECT_AUDIT_PASSED
            );
        }
        List<String> statuses = new ArrayList<>();
        if (StpUtil.hasPermission(PERM_PROJECT_VIEW_DRAFT)) {
            statuses.add(ArtReviewConstants.PROJECT_DRAFT);
            statuses.add(ArtReviewConstants.PROJECT_PARTICIPANT_RETURNED);
        }
        if (StpUtil.hasPermission(PERM_PROJECT_VIEW_SUBMITTED)) {
            statuses.add(ArtReviewConstants.PROJECT_PARTICIPANT_SUBMITTED);
            statuses.add(ArtReviewConstants.PROJECT_SCHOOL_APPROVED);
            statuses.add(ArtReviewConstants.PROJECT_SUBMITTED);
        }
        if (StpUtil.hasPermission(PERM_PROJECT_VIEW_AUDITED)) {
            statuses.add(ArtReviewConstants.PROJECT_RETURNED);
            statuses.add(ArtReviewConstants.PROJECT_AUDIT_PASSED);
        }
        return statuses;
    }

    public boolean canViewProjectStatus(String status) {
        if (LoginHelper.isSuperAdmin()) {
            return true;
        }
        return switch (StringUtils.blankToDefault(status, "")) {
            case ArtReviewConstants.PROJECT_DRAFT, ArtReviewConstants.PROJECT_PARTICIPANT_RETURNED ->
                StpUtil.hasPermission(PERM_PROJECT_VIEW_DRAFT);
            case ArtReviewConstants.PROJECT_PARTICIPANT_SUBMITTED, ArtReviewConstants.PROJECT_SCHOOL_APPROVED,
                 ArtReviewConstants.PROJECT_SUBMITTED -> StpUtil.hasPermission(PERM_PROJECT_VIEW_SUBMITTED);
            case ArtReviewConstants.PROJECT_RETURNED, ArtReviewConstants.PROJECT_AUDIT_PASSED -> StpUtil.hasPermission(PERM_PROJECT_VIEW_AUDITED);
            default -> false;
        };
    }

    public void checkProjectViewStatusPermission(Project project) {
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        if (!canViewProjectStatus(project.getStatus())) {
            throw new ServiceException("当前账号无权查看该状态作品");
        }
    }

    public boolean isCurrentUserSchool() {
        return ArtReviewConstants.USER_TYPE_SCHOOL.equals(artReviewSecurity.currentUser().getUserType());
    }

    public boolean isCurrentUserParticipant() {
        return ArtReviewConstants.USER_TYPE_PARTICIPANT.equals(artReviewSecurity.currentUser().getUserType());
    }
}
