package org.dromara.crehn.audit.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.audit.service.IArtAuditAssignmentService;
import org.dromara.crehn.audit.service.IArtAuditService;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.bo.AuditAssignmentBo;
import org.dromara.crehn.domain.bo.ProjectAuditBo;
import org.dromara.crehn.domain.vo.AssignmentBatchPreviewVo;
import org.dromara.crehn.domain.vo.AuditAssignmentVo;
import org.dromara.crehn.domain.vo.AuditWorkbenchVo;
import org.dromara.crehn.domain.vo.ProjectQuotaOverviewVo;
import org.dromara.crehn.domain.vo.ProjectVo;
import org.dromara.crehn.domain.vo.SchoolInfoVo;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/audit")
public class ArtAuditController extends BaseController {

    private final IArtProjectService projectService;
    private final IArtAuditService auditService;
    private final IArtAuditAssignmentService auditAssignmentService;
    private final ProjectMapper projectMapper;

    @SaCheckPermission("crehn:audit:list")
    @GetMapping("/workbench")
    public R<AuditWorkbenchVo> workbench() {
        return R.ok(auditService.workbench());
    }

    @SaCheckPermission("crehn:audit:list")
    @GetMapping("/list")
    public TableDataInfo<ProjectVo> list(ProjectVo query,
                                         @RequestParam(value = "categoryIds", required = false) String categoryIds,
                                         PageQuery pageQuery) {
        query.setCategoryIds(parseIdList(categoryIds));
        return projectService.queryAuditProjectPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:audit:list")
    @GetMapping("/quota-overview")
    public R<ProjectQuotaOverviewVo> quotaOverview(ProjectVo query,
                                                   @RequestParam(value = "categoryIds", required = false) String categoryIds) {
        query.setCategoryIds(parseIdList(categoryIds));
        return R.ok(projectService.queryAuditQuotaOverview(query));
    }

    @SaCheckPermission("crehn:audit:list")
    @GetMapping("/school/options")
    public R<java.util.List<SchoolInfoVo>> schoolOptions(ProjectVo query,
                                                         @RequestParam(value = "categoryIds", required = false) String categoryIds) {
        return R.ok(auditAssignmentService.schoolOptions(
            query.getActivityId(), parseIdList(categoryIds), query.getGroupCode(), query.getStatus(), query.getProjectName()
        ));
    }

    @SaCheckPermission("crehn:audit:list")
    @GetMapping("/activity/options")
    public R<?> activityOptions() {
        return R.ok(auditAssignmentService.activityOptions());
    }

    @SaCheckPermission("crehn:audit:list")
    @GetMapping("/category/options/{activityId}")
    public R<?> categoryOptions(@PathVariable Long activityId) {
        return R.ok(auditAssignmentService.categoryOptions(activityId));
    }

    @SaCheckPermission("crehn:audit:query")
    @GetMapping("/{id}")
    public R<ProjectVo> detail(@PathVariable Long id) {
        Project project = projectMapper.selectById(id);
        auditAssignmentService.checkProjectAction(project, "query");
        ProjectVo vo = projectService.getProjectDetail(id);
        try {
            auditAssignmentService.checkProjectAction(project, "download");
        } catch (Exception ignored) {
            if (vo.getFiles() != null) {
                vo.getFiles().forEach(file -> {
                    file.setStoragePath(null);
                    file.setPreviewPath(null);
                });
            }
        }
        return R.ok(vo);
    }

    @SaCheckPermission("crehn:audit:pass")
    @Log(title = "art project audit pass", businessType = BusinessType.UPDATE)
    @PostMapping("/pass")
    public R<Void> pass(@RequestBody ProjectAuditBo bo) {
        auditService.pass(bo.getProjectId(), bo.getOpinion(), bo.getNotifyEnabled());
        return R.ok();
    }

    @SaCheckPermission("crehn:audit:return")
    @Log(title = "art project audit return", businessType = BusinessType.UPDATE)
    @PostMapping("/return")
    public R<Void> returnProject(@RequestBody ProjectAuditBo bo) {
        auditService.returnProject(bo.getProjectId(), bo.getOpinion(), bo.getNotifyEnabled());
        return R.ok();
    }

    @SaCheckPermission("crehn:audit:withdrawReturn")
    @Log(title = "art project audit return withdraw", businessType = BusinessType.UPDATE)
    @PostMapping("/withdraw-return")
    public R<Void> withdrawReturn(@RequestBody ProjectAuditBo bo) {
        auditService.withdrawReturn(bo.getProjectId(), bo.getOpinion(), bo.getNotifyEnabled());
        return R.ok();
    }

    @SaCheckPermission("crehn:audit:withdrawPass")
    @Log(title = "art project audit pass withdraw", businessType = BusinessType.UPDATE)
    @PostMapping("/withdraw-pass")
    public R<Void> withdrawPass(@RequestBody ProjectAuditBo bo) {
        auditService.withdrawPass(bo.getProjectId(), bo.getOpinion(), bo.getNotifyEnabled());
        return R.ok();
    }

    @SaCheckPermission("crehn:auditScope:list")
    @GetMapping("/assignment/list")
    public TableDataInfo<AuditAssignmentVo> assignmentList(AuditAssignmentVo query, PageQuery pageQuery) {
        return auditAssignmentService.queryPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:auditScope:query")
    @GetMapping("/assignment/{id}")
    public R<AuditAssignmentVo> assignmentDetail(@PathVariable Long id) {
        return R.ok(auditAssignmentService.getById(id));
    }

    @SaCheckPermission("crehn:auditScope:list")
    @GetMapping("/auditor/options")
    public R<?> auditorOptions(String keyword) {
        return R.ok(auditAssignmentService.auditorOptions(keyword));
    }

    @SaCheckPermission("crehn:auditScope:list")
    @GetMapping("/assignment/school/options")
    public R<List<SchoolInfoVo>> assignmentSchoolOptions(String keyword) {
        return R.ok(auditAssignmentService.assignmentSchoolOptions(keyword));
    }

    @SaCheckPermission("crehn:auditScope:add")
    @Log(title = "art audit assignment add", businessType = BusinessType.INSERT)
    @PostMapping("/assignment")
    public R<Void> addAssignment(@RequestBody AuditAssignmentBo bo) {
        auditAssignmentService.add(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:auditScope:list")
    @PostMapping("/assignment/batch/preview")
    public R<AssignmentBatchPreviewVo> previewAssignmentBatch(@RequestBody AuditAssignmentBo bo) {
        return R.ok(auditAssignmentService.previewBatch(bo));
    }

    @SaCheckPermission("crehn:auditScope:list")
    @Log(title = "art audit assignment batch maintain", businessType = BusinessType.UPDATE)
    @PostMapping("/assignment/batch")
    public R<AssignmentBatchPreviewVo> maintainAssignmentBatch(@RequestBody AuditAssignmentBo bo) {
        return R.ok(auditAssignmentService.maintainBatch(bo));
    }

    @SaCheckPermission("crehn:auditScope:edit")
    @Log(title = "art audit assignment edit", businessType = BusinessType.UPDATE)
    @PutMapping("/assignment")
    public R<Void> updateAssignment(@RequestBody AuditAssignmentBo bo) {
        auditAssignmentService.update(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:auditScope:remove")
    @Log(title = "art audit assignment remove", businessType = BusinessType.DELETE)
    @DeleteMapping("/assignment/{ids}")
    public R<Void> deleteAssignment(@PathVariable Long[] ids) {
        auditAssignmentService.delete(ids);
        return R.ok();
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
