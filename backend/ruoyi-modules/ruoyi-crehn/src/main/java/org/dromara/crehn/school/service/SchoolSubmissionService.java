package org.dromara.crehn.school.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.common.ArtReviewSecurity;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ProjectSnapshot;
import org.dromara.crehn.domain.SchoolSubmissionBatch;
import org.dromara.crehn.domain.SchoolSubmissionItem;
import org.dromara.crehn.domain.bo.SchoolFinalSubmitBo;
import org.dromara.crehn.domain.bo.SchoolProjectReviewBo;
import org.dromara.crehn.mapper.ProjectFileMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.ProjectMemberMapper;
import org.dromara.crehn.mapper.ProjectSnapshotMapper;
import org.dromara.crehn.mapper.SchoolSubmissionBatchMapper;
import org.dromara.crehn.mapper.SchoolSubmissionItemMapper;
import org.dromara.crehn.notice.service.IArtUserMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class SchoolSubmissionService {
    private final ArtReviewSecurity security;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final ProjectFileMapper fileMapper;
    private final ProjectSnapshotMapper snapshotMapper;
    private final SchoolSubmissionBatchMapper batchMapper;
    private final SchoolSubmissionItemMapper itemMapper;
    private final IArtUserMessageService userMessageService;

    public List<Project> pending(Long activityId) {
        Long schoolId = requireSchoolContact();
        return projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
            .eq(Project::getSchoolId, schoolId)
            .eq(activityId != null, Project::getActivityId, activityId)
            .in(Project::getStatus, ArtReviewConstants.PROJECT_PARTICIPANT_SUBMITTED,
                ArtReviewConstants.PROJECT_SCHOOL_APPROVED, ArtReviewConstants.PROJECT_PARTICIPANT_RETURNED)
            .orderByDesc(Project::getParticipantSubmittedAt));
    }

    @Transactional(rollbackFor = Exception.class)
    public void review(SchoolProjectReviewBo bo) {
        Long schoolId = requireSchoolContact();
        Project project = lockProject(bo.getProjectId());
        if (!Objects.equals(schoolId, project.getSchoolId())) {
            throw new ServiceException("无权审核其他学校作品");
        }
        if (!ArtReviewConstants.PROJECT_PARTICIPANT_SUBMITTED.equals(project.getStatus())) {
            throw new ServiceException("只有参赛者已提交作品可以进行学校审核");
        }
        if ("pass".equals(bo.getResult())) {
            project.setStatus(ArtReviewConstants.PROJECT_SCHOOL_APPROVED);
            project.setSchoolReviewStatus("approved");
        } else if ("return".equals(bo.getResult())) {
            if (org.dromara.common.core.utils.StringUtils.isBlank(bo.getOpinion())) {
                throw new ServiceException("退回原因不能为空");
            }
            project.setStatus(ArtReviewConstants.PROJECT_PARTICIPANT_RETURNED);
            project.setSchoolReviewStatus("returned");
        } else {
            throw new ServiceException("学校审核结果不正确");
        }
        project.setSchoolReviewedAt(new Date());
        project.setCurrentAuditOpinion(bo.getOpinion());
        project.setRowVersion((project.getRowVersion() == null ? 0L : project.getRowVersion()) + 1);
        projectMapper.updateById(project);
        userMessageService.sendSchoolReviewMessage(project, bo.getResult(), bo.getOpinion(), LoginHelper.getUserId());
    }

    @Transactional(rollbackFor = Exception.class)
    public SchoolSubmissionBatch finalSubmit(SchoolFinalSubmitBo bo) {
        Long schoolId = requireSchoolContact();
        if (new HashSet<>(bo.getProjectIds()).size() != bo.getProjectIds().size()) {
            throw new ServiceException("最终提交列表中存在重复作品");
        }
        List<Project> projects = bo.getProjectIds().stream().map(this::lockProject).toList();
        if (projects.stream().anyMatch(project -> !Objects.equals(project.getSchoolId(), schoolId)
            || !Objects.equals(project.getActivityId(), bo.getActivityId())
            || !ArtReviewConstants.PROJECT_SCHOOL_APPROVED.equals(project.getStatus()))) {
            throw new ServiceException("批次中存在非本校、非本活动或未推荐作品");
        }
        SchoolSubmissionBatch batch = new SchoolSubmissionBatch();
        batch.setBatchNo("SB" + System.currentTimeMillis());
        batch.setActivityId(bo.getActivityId());
        batch.setSchoolId(schoolId);
        batch.setItemCount(projects.size());
        batch.setStatus("submitted");
        batch.setSubmittedBy(LoginHelper.getUserId());
        batch.setSubmittedAt(new Date());
        batch.setRowVersion(1L);
        batchMapper.insert(batch);
        for (Project project : projects) {
            ProjectSnapshot snapshot = createSnapshot(project);
            SchoolSubmissionItem item = new SchoolSubmissionItem();
            item.setBatchId(batch.getId());
            item.setProjectId(project.getId());
            item.setProjectSnapshotId(snapshot.getId());
            item.setStatus("submitted");
            itemMapper.insert(item);
            project.setStatus(ArtReviewConstants.PROJECT_SUBMITTED);
            project.setSchoolFinalBatchId(batch.getId());
            project.setSnapshotVersionId(snapshot.getId());
            project.setSubmittedAt(batch.getSubmittedAt());
            project.setSubmittedBy(batch.getSubmittedBy());
            project.setRowVersion((project.getRowVersion() == null ? 0L : project.getRowVersion()) + 1);
            projectMapper.updateById(project);
            userMessageService.sendSchoolFinalSubmitMessage(project, batch.getId(), batch.getSubmittedBy());
        }
        return batch;
    }

    private ProjectSnapshot createSnapshot(Project project) {
        Map<String, Object> snapshotData = new LinkedHashMap<>();
        snapshotData.put("project", project);
        snapshotData.put("members", memberMapper.selectList(Wrappers.lambdaQuery(org.dromara.crehn.domain.ProjectMember.class)
            .eq(org.dromara.crehn.domain.ProjectMember::getProjectId, project.getId())));
        snapshotData.put("files", fileMapper.selectList(Wrappers.lambdaQuery(org.dromara.crehn.domain.ProjectFile.class)
            .eq(org.dromara.crehn.domain.ProjectFile::getProjectId, project.getId())));
        String json = JsonUtils.toJsonString(snapshotData);
        Integer latest = snapshotMapper.selectList(Wrappers.lambdaQuery(ProjectSnapshot.class)
            .eq(ProjectSnapshot::getProjectId, project.getId())
            .orderByDesc(ProjectSnapshot::getVersionNo)
            .last("limit 1")).stream().findFirst().map(ProjectSnapshot::getVersionNo).orElse(0);
        ProjectSnapshot snapshot = new ProjectSnapshot();
        snapshot.setProjectId(project.getId());
        snapshot.setVersionNo(latest + 1);
        snapshot.setSnapshotJson(json);
        snapshot.setChecksum(hash(json));
        snapshot.setSubmittedBy(LoginHelper.getUserId());
        snapshot.setSubmittedAt(new Date());
        snapshot.setSourceSnapshotId(project.getSnapshotVersionId());
        snapshotMapper.insert(snapshot);
        return snapshot;
    }

    private Project lockProject(Long id) {
        Project project = projectMapper.selectOne(Wrappers.lambdaQuery(Project.class)
            .eq(Project::getId, id).last("for update"));
        if (project == null) {
            throw new ServiceException("作品不存在");
        }
        return project;
    }

    private Long requireSchoolContact() {
        if (!ArtReviewConstants.USER_TYPE_SCHOOL.equals(security.currentUser().getUserType())) {
            throw new ServiceException("只有学校联络员可以执行该操作");
        }
        return security.requireEnabledSchoolId();
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256不可用", e);
        }
    }
}
