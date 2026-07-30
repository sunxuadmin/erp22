package org.dromara.crehn.project.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.audit.service.IArtAuditAssignmentService;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.config.service.IArtSchoolInfoService;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.SchoolInfo;
import org.dromara.crehn.domain.bo.ProjectRecycleBo;
import org.dromara.crehn.domain.vo.ActivityCategoryVo;
import org.dromara.crehn.domain.vo.ActivityVo;
import org.dromara.crehn.domain.vo.ProjectQuotaOverviewVo;
import org.dromara.crehn.domain.vo.ProjectViewStatsVo;
import org.dromara.crehn.domain.vo.ProjectVo;
import org.dromara.crehn.domain.vo.SchoolInfoVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ActivityMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.project.service.IArtProjectService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/project-view")
public class ArtProjectViewController extends BaseController {

    private final IArtProjectService projectService;
    private final IArtAuditAssignmentService auditAssignmentService;
    private final IArtSchoolInfoService schoolInfoService;
    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final ProjectMapper projectMapper;

    @SaCheckPermission("crehn:projectView:list")
    @GetMapping("/list")
    public TableDataInfo<ProjectVo> list(ProjectVo query,
                                         @RequestParam(value = "categoryIds", required = false) String categoryIds,
                                         PageQuery pageQuery) {
        query.setCategoryIds(parseIdList(categoryIds));
        return projectService.queryProjectViewPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:projectView:list")
    @GetMapping("/stats")
    public R<ProjectViewStatsVo> stats(ProjectVo query,
                                       @RequestParam(value = "categoryIds", required = false) String categoryIds) {
        query.setCategoryIds(parseIdList(categoryIds));
        return R.ok(projectService.queryProjectViewStats(query));
    }

    @SaCheckPermission("crehn:projectView:list")
    @GetMapping("/quota-overview")
    public R<ProjectQuotaOverviewVo> quotaOverview(ProjectVo query,
                                                   @RequestParam(value = "categoryIds", required = false) String categoryIds) {
        query.setCategoryIds(parseIdList(categoryIds));
        return R.ok(projectService.queryProjectViewQuotaOverview(query));
    }

    @SaCheckPermission("crehn:projectView:remove")
    @Log(title = "art project view recycle", businessType = BusinessType.DELETE)
    @PostMapping("/recycle")
    public R<Void> recycle(@RequestBody ProjectRecycleBo bo) {
        projectService.recycleProjectViewProject(bo.getProjectId(), bo.getReason());
        return R.ok();
    }

    @SaCheckPermission("crehn:projectView:list")
    @GetMapping("/activity/options")
    public R<List<ActivityVo>> activityOptions() {
        return R.ok(activityMapper.selectVoList(Wrappers.lambdaQuery(Activity.class)
            .eq(Activity::getStatus, ArtReviewConstants.ACTIVITY_ENABLED)
            .orderByDesc(Activity::getYear)
            .orderByDesc(Activity::getCreateTime)
            .orderByDesc(Activity::getId)));
    }

    @SaCheckPermission("crehn:projectView:list")
    @GetMapping("/category/options/{activityId}")
    public R<List<ActivityCategoryVo>> categoryOptions(@PathVariable Long activityId) {
        return R.ok(categoryMapper.selectVoList(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getActivityId, activityId)
            .eq(ActivityCategory::getEnabled, true)
            .orderByAsc(ActivityCategory::getSortOrder)
            .orderByAsc(ActivityCategory::getId)));
    }

    @SaCheckPermission("crehn:projectView:list")
    @GetMapping("/school/options")
    public R<List<SchoolInfoVo>> schoolOptions(SchoolInfo query) {
        return R.ok(schoolInfoService.queryList(query));
    }

    @SaCheckPermission("crehn:projectView:query")
    @GetMapping("/{id}")
    public R<ProjectVo> detail(@PathVariable Long id, @RequestParam(value = "withFile", required = false, defaultValue = "true") Boolean withFile) {
        Project project = projectMapper.selectById(id);
        projectService.checkProjectViewStatusPermission(project);
        auditAssignmentService.checkProjectViewAction(project, "query");
        ProjectVo vo = projectService.getProjectDetail(id);
        if (Boolean.TRUE.equals(withFile)) {
            auditAssignmentService.checkProjectViewAction(project, "download");
        } else if (vo.getFiles() != null) {
            vo.getFiles().forEach(file -> {
                file.setStoragePath(null);
                file.setPreviewPath(null);
            });
        }
        return R.ok(vo);
    }

    private List<Long> parseIdList(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .map(Long::valueOf)
            .toList();
    }
}
