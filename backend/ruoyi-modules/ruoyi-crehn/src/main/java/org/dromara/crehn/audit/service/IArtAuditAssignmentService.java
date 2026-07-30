package org.dromara.crehn.audit.service;

import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.bo.AuditAssignmentBo;
import org.dromara.crehn.domain.vo.ActivityCategoryVo;
import org.dromara.crehn.domain.vo.ActivityVo;
import org.dromara.crehn.domain.vo.AssignmentBatchPreviewVo;
import org.dromara.crehn.domain.vo.AuditAssignmentVo;
import org.dromara.crehn.domain.vo.SchoolInfoVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.vo.SysUserVo;

import java.util.List;

public interface IArtAuditAssignmentService {
    TableDataInfo<AuditAssignmentVo> queryPage(AuditAssignmentVo query, PageQuery pageQuery);

    AuditAssignmentVo getById(Long id);

    void add(AuditAssignmentBo bo);

    void update(AuditAssignmentBo bo);

    void delete(Long[] ids);

    AssignmentBatchPreviewVo previewBatch(AuditAssignmentBo bo);

    AssignmentBatchPreviewVo maintainBatch(AuditAssignmentBo bo);

    List<ActivityVo> activityOptions();

    List<ActivityCategoryVo> categoryOptions(Long activityId);

    List<SchoolInfoVo> schoolOptions(Long activityId, List<Long> categoryIds, String groupCode, String status, String projectName);

    List<SchoolInfoVo> assignmentSchoolOptions(String keyword);

    List<SysUserVo> auditorOptions(String keyword);

    List<Project> filterVisibleProjects(List<Project> projects);

    List<Project> filterProjectViewVisibleProjects(List<Project> projects);

    void checkProjectAction(Project project, String action);

    void checkProjectViewAction(Project project, String action);
}
