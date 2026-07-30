package org.dromara.crehn.project.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.bo.ProjectFileDirectUploadCompleteBo;
import org.dromara.crehn.domain.bo.ProjectFileDirectUploadInitBo;
import org.dromara.crehn.domain.bo.ProjectRecycleBo;
import org.dromara.crehn.domain.bo.ProjectSaveBo;
import org.dromara.crehn.domain.vo.ProjectCategoryStatsVo;
import org.dromara.crehn.domain.vo.GenericTableImportResultVo;
import org.dromara.crehn.domain.vo.MemberAttachmentBatchResultVo;
import org.dromara.crehn.domain.vo.ProjectFileDirectUploadInitVo;
import org.dromara.crehn.domain.vo.ProjectFileVo;
import org.dromara.crehn.domain.vo.ProjectQuotaOverviewVo;
import org.dromara.crehn.domain.vo.ProjectVo;
import org.dromara.crehn.domain.vo.SchoolDashboardVo;
import org.dromara.crehn.project.service.IArtProjectService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/project")
public class ArtProjectController extends BaseController {

    private static final int SAVE_DRAFT_MAX_ATTEMPTS = 3;

    private final IArtProjectService projectService;

    @SaCheckPermission("crehn:project:list")
    @GetMapping("/my-list")
    public TableDataInfo<ProjectVo> myList(ProjectVo query, PageQuery pageQuery) {
        return projectService.queryMyProjectPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:project:list")
    @GetMapping("/my-category-stats")
    public R<List<ProjectCategoryStatsVo>> myCategoryStats(@RequestParam(value = "activityId", required = false) Long activityId,
                                                           @RequestParam(value = "categoryIds", required = false) String categoryIds) {
        return R.ok(projectService.queryMyCategoryStats(activityId, parseIdList(categoryIds)));
    }

    @SaCheckPermission("crehn:project:list")
    @GetMapping("/my-quota-overview")
    public R<ProjectQuotaOverviewVo> myQuotaOverview(@RequestParam(value = "activityId", required = false) Long activityId,
                                                     @RequestParam(value = "categoryIds", required = false) String categoryIds) {
        return R.ok(projectService.queryMyQuotaOverview(activityId, parseIdList(categoryIds)));
    }

    @SaCheckPermission("crehn:project:list")
    @GetMapping("/my-dashboard")
    public R<SchoolDashboardVo> myDashboard(@RequestParam(value = "activityId", required = false) Long activityId) {
        return R.ok(projectService.queryMyDashboard(activityId));
    }

    @SaCheckPermission("crehn:project:query")
    @GetMapping("/{id}")
    public R<ProjectVo> detail(@PathVariable Long id) {
        return R.ok(projectService.getProjectDetail(id));
    }

    @SaCheckPermission("crehn:project:add")
    @Log(title = "art project draft", businessType = BusinessType.INSERT)
    @PostMapping("/draft")
    public R<ProjectVo> saveDraft(@RequestBody ProjectSaveBo bo) {
        return R.ok(saveDraftWithDeadlockRetry(bo));
    }

    @SaCheckPermission("crehn:project:submit")
    @Log(title = "art project submit", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/submit")
    public R<Void> submit(@PathVariable Long id) {
        projectService.submit(id);
        return R.ok();
    }

    @SaCheckPermission("crehn:project:submit")
    @Log(title = "art project withdraw submit", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/withdraw-submit")
    public R<Void> withdrawSubmit(@PathVariable Long id) {
        projectService.withdrawSubmit(id);
        return R.ok();
    }

    @SaCheckPermission("crehn:project:upload")
    @RequestMapping(value = "/member/template", method = {RequestMethod.GET, RequestMethod.POST})
    public void downloadMemberTemplate(@RequestParam(value = "projectId", required = false) Long projectId,
                                       @RequestParam(value = "categoryId", required = false) Long categoryId,
                                       HttpServletResponse response) {
        projectService.exportMemberTemplate(response, projectId, categoryId);
    }

    @SaCheckPermission("crehn:project:upload")
    @Log(title = "art project member import", businessType = BusinessType.IMPORT)
    @PostMapping("/{projectId}/member/import")
    public R<ProjectVo> importMembers(@PathVariable Long projectId,
                                      @RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "replace", required = false) Boolean replace) {
        return R.ok(projectService.importMembers(projectId, file, replace));
    }

    @SaCheckPermission("crehn:project:upload")
    @Log(title = "art project generic table template", businessType = BusinessType.EXPORT)
    @GetMapping("/table/template")
    public void downloadGenericTableTemplate(@RequestParam(value = "projectId", required = false) Long projectId,
                                             @RequestParam(value = "categoryId", required = false) Long categoryId,
                                             @RequestParam("tableKey") String tableKey,
                                             HttpServletResponse response) {
        projectService.exportGenericTableTemplate(response, projectId, categoryId, tableKey);
    }

    @SaCheckPermission("crehn:project:upload")
    @Log(title = "art project generic table import", businessType = BusinessType.IMPORT)
    @PostMapping("/{projectId}/table/{tableKey}/import")
    public R<GenericTableImportResultVo> importGenericTable(@PathVariable Long projectId,
                                                            @PathVariable String tableKey,
                                                            @RequestParam("file") MultipartFile file) {
        return R.ok(projectService.importGenericTable(projectId, tableKey, file));
    }

    @SaCheckPermission("crehn:project:upload")
    @Log(title = "art project member photo", businessType = BusinessType.UPDATE)
    @PostMapping("/{projectId}/member/{memberId}/photo")
    public R<ProjectVo> uploadMemberPhoto(@PathVariable Long projectId, @PathVariable Long memberId,
                                          @RequestParam("file") MultipartFile file) {
        return R.ok(projectService.uploadMemberPhoto(projectId, memberId, file));
    }

    @SaCheckPermission("crehn:project:upload")
    @Log(title = "art project member student report", businessType = BusinessType.UPDATE)
    @PostMapping("/{projectId}/member/{memberId}/student-report")
    public R<ProjectVo> uploadMemberStudentReport(@PathVariable Long projectId, @PathVariable Long memberId,
                                                  @RequestParam("file") MultipartFile file) {
        return R.ok(projectService.uploadMemberStudentReport(projectId, memberId, file));
    }

    @SaCheckPermission("crehn:project:upload")
    @Log(title = "art project member attachment", businessType = BusinessType.UPDATE)
    @PostMapping("/{projectId}/member/{memberId}/attachment/{fieldKey}")
    public R<ProjectVo> uploadMemberAttachment(@PathVariable Long projectId, @PathVariable Long memberId,
                                                @PathVariable String fieldKey, @RequestParam("file") MultipartFile file) {
        return R.ok(projectService.uploadMemberAttachment(projectId, memberId, fieldKey, file));
    }

    @SaCheckPermission("crehn:project:upload")
    @Log(title = "art project member attachment batch", businessType = BusinessType.UPDATE)
    @PostMapping("/{projectId}/member/attachment/{fieldKey}/batch")
    public R<MemberAttachmentBatchResultVo> batchUploadMemberAttachment(@PathVariable Long projectId,
                                                                         @PathVariable String fieldKey,
                                                                         @RequestParam("files") List<MultipartFile> files) {
        return R.ok(projectService.batchUploadMemberAttachment(projectId, fieldKey, files));
    }

    @SaCheckPermission("crehn:project:upload")
    @Log(title = "art project file upload", businessType = BusinessType.INSERT)
    @PostMapping("/{projectId}/file/{requirementId}")
    public R<ProjectFileVo> uploadFile(@PathVariable Long projectId, @PathVariable Long requirementId,
                                       @RequestParam("file") MultipartFile file) {
        return R.ok(projectService.uploadFile(projectId, requirementId, file));
    }

    @SaCheckPermission("crehn:project:upload")
    @Log(title = "art project file direct upload init", businessType = BusinessType.INSERT)
    @PostMapping("/{projectId}/file/{requirementId}/direct-upload/init")
    public R<ProjectFileDirectUploadInitVo> initDirectUpload(@PathVariable Long projectId,
                                                            @PathVariable Long requirementId,
                                                            @RequestBody ProjectFileDirectUploadInitBo bo) {
        return R.ok(projectService.initDirectUpload(projectId, requirementId, bo));
    }

    @SaCheckPermission("crehn:project:upload")
    @Log(title = "art project file direct upload complete", businessType = BusinessType.INSERT)
    @PostMapping("/{projectId}/file/{requirementId}/direct-upload/complete")
    public R<ProjectFileVo> completeDirectUpload(@PathVariable Long projectId,
                                                 @PathVariable Long requirementId,
                                                 @RequestBody ProjectFileDirectUploadCompleteBo bo) {
        return R.ok(projectService.completeDirectUpload(projectId, requirementId, bo));
    }

    @SaCheckPermission("crehn:project:upload")
    @Log(title = "art project file preview", businessType = BusinessType.UPDATE)
    @PostMapping("/{projectId}/file/{fileId}/preview")
    public R<ProjectFileVo> requestPreview(@PathVariable Long projectId, @PathVariable Long fileId) {
        return R.ok(projectService.requestFilePreview(projectId, fileId));
    }

    @SaCheckPermission("crehn:project:upload")
    @Log(title = "art project file delete", businessType = BusinessType.DELETE)
    @DeleteMapping("/{projectId}/file/{fileId}")
    public R<Void> deleteFile(@PathVariable Long projectId, @PathVariable Long fileId) {
        projectService.deleteFile(projectId, fileId);
        return R.ok();
    }

    @SaCheckPermission("crehn:project:remove")
    @Log(title = "art project recycle", businessType = BusinessType.DELETE)
    @PostMapping("/recycle")
    public R<Void> recycleProject(@RequestBody ProjectRecycleBo bo) {
        projectService.recycleProject(bo.getProjectId(), bo.getReason());
        return R.ok();
    }

    @SaCheckPermission("crehn:recycle:list")
    @GetMapping("/recycle/project/list")
    public TableDataInfo<ProjectVo> recycleProjectList(ProjectVo query, PageQuery pageQuery) {
        return projectService.queryRecycledProjectPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:recycle:restore")
    @Log(title = "art project restore", businessType = BusinessType.UPDATE)
    @PutMapping("/recycle/project/{projectId}/restore")
    public R<Void> restoreProject(@PathVariable Long projectId) {
        projectService.restoreRecycledProject(projectId);
        return R.ok();
    }

    @SaCheckPermission("crehn:recycle:remove")
    @Log(title = "art project purge", businessType = BusinessType.DELETE)
    @DeleteMapping("/recycle/project/{projectIds}")
    public R<Void> purgeProjects(@PathVariable Long[] projectIds) {
        projectService.purgeRecycledProjects(projectIds);
        return R.ok();
    }

    @SaCheckPermission("crehn:recycle:list")
    @GetMapping("/recycle/file/list")
    public TableDataInfo<ProjectFileVo> recycleFileList(ProjectFileVo query, PageQuery pageQuery) {
        return projectService.queryDeletedFilePage(query, pageQuery);
    }

    @SaCheckPermission("crehn:recycle:restore")
    @Log(title = "art project file restore", businessType = BusinessType.UPDATE)
    @PutMapping("/recycle/file/{fileId}/restore")
    public R<Void> restoreFile(@PathVariable Long fileId) {
        projectService.restoreDeletedFile(fileId);
        return R.ok();
    }

    @SaCheckPermission("crehn:recycle:list")
    @GetMapping("/recycle/file/{fileId}/download")
    public void downloadDeletedFile(@PathVariable Long fileId, HttpServletResponse response) throws IOException {
        projectService.downloadDeletedFile(fileId, response);
    }

    @SaCheckPermission("crehn:recycle:remove")
    @Log(title = "art project file purge", businessType = BusinessType.DELETE)
    @DeleteMapping("/recycle/file/{fileIds}")
    public R<Void> purgeFiles(@PathVariable Long[] fileIds) {
        projectService.purgeDeletedFiles(fileIds);
        return R.ok();
    }

    private ProjectVo saveDraftWithDeadlockRetry(ProjectSaveBo bo) {
        RuntimeException latest = null;
        for (int attempt = 1; attempt <= SAVE_DRAFT_MAX_ATTEMPTS; attempt++) {
            try {
                return projectService.saveDraft(bo);
            } catch (DeadlockLoserDataAccessException | CannotAcquireLockException e) {
                latest = e;
                if (attempt == SAVE_DRAFT_MAX_ATTEMPTS) {
                    break;
                }
                sleepBeforeRetry(attempt);
            }
        }
        throw latest;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(50L * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while retrying project draft save", e);
        }
    }

    private List<Long> parseIdList(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.split(","))
            .map(String::trim)
            .filter(item -> !item.isBlank())
            .map(Long::valueOf)
            .toList();
    }
}
