package org.dromara.crehn.project.service;

import org.dromara.crehn.domain.bo.ProjectSaveBo;
import org.dromara.crehn.domain.bo.ProjectFileDirectUploadCompleteBo;
import org.dromara.crehn.domain.bo.ProjectFileDirectUploadInitBo;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.vo.ProjectCategoryStatsVo;
import org.dromara.crehn.domain.vo.GenericTableImportResultVo;
import org.dromara.crehn.domain.vo.MemberAttachmentBatchResultVo;
import org.dromara.crehn.domain.vo.ProjectFileDirectUploadInitVo;
import org.dromara.crehn.domain.vo.ProjectFileVo;
import org.dromara.crehn.domain.vo.ProjectQuotaOverviewVo;
import org.dromara.crehn.domain.vo.SchoolDashboardVo;
import org.dromara.crehn.domain.vo.ProjectViewStatsVo;
import org.dromara.crehn.domain.vo.ProjectVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IArtProjectService {
    TableDataInfo<ProjectVo> queryMyProjectPage(ProjectVo query, PageQuery pageQuery);

    List<ProjectCategoryStatsVo> queryMyCategoryStats(Long activityId, List<Long> categoryIds);

    ProjectQuotaOverviewVo queryMyQuotaOverview(Long activityId, List<Long> categoryIds);

    SchoolDashboardVo queryMyDashboard(Long activityId);

    TableDataInfo<ProjectVo> queryAuditProjectPage(ProjectVo query, PageQuery pageQuery);

    ProjectQuotaOverviewVo queryAuditQuotaOverview(ProjectVo query);

    TableDataInfo<ProjectVo> queryProjectViewPage(ProjectVo query, PageQuery pageQuery);

    ProjectViewStatsVo queryProjectViewStats(ProjectVo query);

    ProjectQuotaOverviewVo queryProjectViewQuotaOverview(ProjectVo query);

    void checkProjectViewStatusPermission(Project project);

    void recycleProjectViewProject(Long projectId, String reason);

    ProjectVo getProjectDetail(Long id);

    ProjectVo saveDraft(ProjectSaveBo bo);

    void submit(Long id);

    void withdrawSubmit(Long id);

    void exportMemberTemplate(HttpServletResponse response, Long projectId, Long categoryId);

    ProjectVo importMembers(Long projectId, MultipartFile file, Boolean replace);

    void exportGenericTableTemplate(HttpServletResponse response, Long projectId, Long categoryId, String tableKey);

    GenericTableImportResultVo importGenericTable(Long projectId, String tableKey, MultipartFile file);

    ProjectVo uploadMemberPhoto(Long projectId, Long memberId, MultipartFile file);

    ProjectVo uploadMemberStudentReport(Long projectId, Long memberId, MultipartFile file);

    ProjectVo uploadMemberAttachment(Long projectId, Long memberId, String fieldKey, MultipartFile file);

    MemberAttachmentBatchResultVo batchUploadMemberAttachment(Long projectId, String fieldKey, List<MultipartFile> files);

    ProjectFileVo uploadFile(Long projectId, Long requirementId, MultipartFile file);

    ProjectFileDirectUploadInitVo initDirectUpload(Long projectId, Long requirementId, ProjectFileDirectUploadInitBo bo);

    ProjectFileVo completeDirectUpload(Long projectId, Long requirementId, ProjectFileDirectUploadCompleteBo bo);

    ProjectFileVo requestFilePreview(Long projectId, Long fileId);

    void deleteFile(Long projectId, Long fileId);

    void recycleProject(Long projectId, String reason);

    TableDataInfo<ProjectVo> queryRecycledProjectPage(ProjectVo query, PageQuery pageQuery);

    void restoreRecycledProject(Long projectId);

    void purgeRecycledProjects(Long[] projectIds);

    TableDataInfo<ProjectFileVo> queryDeletedFilePage(ProjectFileVo query, PageQuery pageQuery);

    void restoreDeletedFile(Long fileId);

    void downloadDeletedFile(Long fileId, HttpServletResponse response) throws IOException;

    void purgeDeletedFiles(Long[] fileIds);
}
