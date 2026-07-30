package org.dromara.crehn.audit.service.impl;

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.dromara.crehn.audit.service.IArtAuditAssignmentService;
import org.dromara.crehn.audit.service.IArtAuditService;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ProjectAuditRecord;
import org.dromara.crehn.domain.SchoolInfo;
import org.dromara.crehn.domain.vo.AuditWorkbenchVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ProjectAuditRecordMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.SchoolInfoMapper;
import org.dromara.crehn.notice.service.IArtUserMessageService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ArtAuditServiceImpl implements IArtAuditService {

    private final ProjectMapper projectMapper;
    private final ProjectAuditRecordMapper auditRecordMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final SchoolInfoMapper schoolInfoMapper;
    private final IArtUserMessageService userMessageService;
    private final IArtAuditAssignmentService auditAssignmentService;

    @Override
    public AuditWorkbenchVo workbench() {
        AuditWorkbenchVo vo = new AuditWorkbenchVo();
        List<Project> pendingProjects = auditAssignmentService.filterVisibleProjects(projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
                .eq(Project::getStatus, ArtReviewConstants.PROJECT_SUBMITTED)
                .ne(Project::getStatus, ArtReviewConstants.PROJECT_RECYCLED)
                .orderByDesc(Project::getSubmittedAt)
                .orderByDesc(Project::getId)))
            .stream()
            .limit(10)
            .toList();

        List<ProjectAuditRecord> myRecords = auditRecordMapper.selectList(Wrappers.lambdaQuery(ProjectAuditRecord.class)
            .eq(ProjectAuditRecord::getAuditedBy, LoginHelper.getUserId())
            .in(ProjectAuditRecord::getAuditResult, ArtReviewConstants.AUDIT_PASS, ArtReviewConstants.AUDIT_RETURN)
            .orderByDesc(ProjectAuditRecord::getAuditedAt)
            .orderByDesc(ProjectAuditRecord::getId));

        Map<Long, Project> initialProjectMap = loadProjectMap(pendingProjects, myRecords);
        myRecords = filterVisibleAuditRecords(myRecords, initialProjectMap);
        Map<Long, Project> visibleProjectMap = loadProjectMap(pendingProjects, myRecords);
        Map<Long, ActivityCategory> categoryMap = loadCategoryMap(visibleProjectMap.values().stream().toList());
        Map<Long, SchoolInfo> schoolMap = loadSchoolMap(visibleProjectMap.values().stream().toList());

        vo.setPendingProjects(pendingProjects.stream()
            .map(project -> toWorkbenchProjectItem(project, categoryMap, schoolMap))
            .toList());
        vo.setMyStats(toWorkbenchStats(myRecords));
        vo.setMyGroupItems(toWorkbenchGroupItems(myRecords, visibleProjectMap, categoryMap));
        vo.setMyRecords(myRecords.stream()
            .limit(10)
            .map(record -> toWorkbenchRecordItem(record, visibleProjectMap, categoryMap, schoolMap))
            .toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pass(Long projectId, String opinion, Boolean notifyEnabled) {
        audit(projectId, ArtReviewConstants.PROJECT_AUDIT_PASSED, ArtReviewConstants.AUDIT_PASS,
            StringUtils.blankToDefault(opinion, "审核通过"), notifyEnabled);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnProject(Long projectId, String opinion, Boolean notifyEnabled) {
        if (StringUtils.isBlank(opinion)) {
            throw new ServiceException("退回原因不能为空");
        }
        audit(projectId, ArtReviewConstants.PROJECT_RETURNED, ArtReviewConstants.AUDIT_RETURN, opinion, notifyEnabled);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawReturn(Long projectId, String opinion, Boolean notifyEnabled) {
        withdraw(projectId, ArtReviewConstants.PROJECT_RETURNED, ArtReviewConstants.AUDIT_WITHDRAW_RETURN,
            StringUtils.blankToDefault(opinion, "撤销退回，恢复待审核"), notifyEnabled);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawPass(Long projectId, String opinion, Boolean notifyEnabled) {
        withdraw(projectId, ArtReviewConstants.PROJECT_AUDIT_PASSED, ArtReviewConstants.AUDIT_WITHDRAW_PASS,
            StringUtils.blankToDefault(opinion, "撤销通过，恢复待审核"), notifyEnabled);
    }

    private void audit(Long projectId, String toStatus, String result, String opinion, Boolean notifyEnabled) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        if (!ArtReviewConstants.PROJECT_SUBMITTED.equals(project.getStatus())) {
            throw new ServiceException("只有待审核项目可以审核");
        }
        auditAssignmentService.checkProjectAction(project, result);

        String fromStatus = project.getStatus();
        project.setStatus(toStatus);
        project.setCurrentAuditOpinion(opinion);
        projectMapper.updateById(project);

        insertAuditRecord(projectId, fromStatus, toStatus, result, opinion);
        handleAuditNotify(project, result, opinion, notifyEnabled);
    }

    private void withdraw(Long projectId, String requiredStatus, String result, String opinion, Boolean notifyEnabled) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        if (!requiredStatus.equals(project.getStatus())) {
            throw new ServiceException("当前项目状态不允许撤销该审核操作");
        }
        auditAssignmentService.checkProjectAction(project, result);

        String fromStatus = project.getStatus();
        project.setStatus(ArtReviewConstants.PROJECT_SUBMITTED);
        project.setCurrentAuditOpinion(opinion);
        projectMapper.updateById(project);

        insertAuditRecord(projectId, fromStatus, ArtReviewConstants.PROJECT_SUBMITTED, result, opinion);
        handleAuditNotify(project, result, opinion, notifyEnabled);
    }

    private void insertAuditRecord(Long projectId, String fromStatus, String toStatus, String result, String opinion) {
        ProjectAuditRecord record = new ProjectAuditRecord();
        record.setProjectId(projectId);
        record.setFromStatus(fromStatus);
        record.setToStatus(toStatus);
        record.setAuditResult(result);
        record.setOpinion(opinion);
        record.setAuditedBy(LoginHelper.getUserId());
        record.setAuditedAt(new Date());
        auditRecordMapper.insert(record);
    }

    private void handleAuditNotify(Project project, String result, String opinion, Boolean notifyEnabled) {
        if (!Boolean.TRUE.equals(notifyEnabled)) {
            return;
        }
        userMessageService.sendAuditMessage(project, result, opinion, LoginHelper.getUserId());
    }

    private Map<Long, Project> loadProjectMap(List<Project> pendingProjects, List<ProjectAuditRecord> records) {
        List<Long> projectIds = records.stream()
            .map(ProjectAuditRecord::getProjectId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        pendingProjects.stream()
            .map(Project::getId)
            .filter(Objects::nonNull)
            .forEach(projectIds::add);
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        return projectMapper.selectBatchIds(projectIds.stream().distinct().toList())
            .stream()
            .collect(Collectors.toMap(Project::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private List<ProjectAuditRecord> filterVisibleAuditRecords(List<ProjectAuditRecord> records, Map<Long, Project> projectMap) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<Project> recordProjects = records.stream()
            .map(record -> projectMap.get(record.getProjectId()))
            .filter(Objects::nonNull)
            .toList();
        if (recordProjects.isEmpty()) {
            return List.of();
        }
        Set<Long> visibleProjectIds = auditAssignmentService.filterVisibleProjects(recordProjects)
            .stream()
            .map(Project::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (visibleProjectIds.isEmpty()) {
            return List.of();
        }
        return records.stream()
            .filter(record -> visibleProjectIds.contains(record.getProjectId()))
            .toList();
    }

    private Map<Long, ActivityCategory> loadCategoryMap(List<Project> projects) {
        List<Long> categoryIds = projects.stream()
            .map(Project::getCategoryId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return categoryMapper.selectBatchIds(categoryIds)
            .stream()
            .collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<Long, SchoolInfo> loadSchoolMap(List<Project> projects) {
        List<Long> schoolIds = projects.stream()
            .map(Project::getSchoolId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (schoolIds.isEmpty()) {
            return Map.of();
        }
        return schoolInfoMapper.selectBatchIds(schoolIds)
            .stream()
            .collect(Collectors.toMap(SchoolInfo::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private AuditWorkbenchVo.ProjectItem toWorkbenchProjectItem(Project project, Map<Long, ActivityCategory> categoryMap,
                                                               Map<Long, SchoolInfo> schoolMap) {
        AuditWorkbenchVo.ProjectItem item = new AuditWorkbenchVo.ProjectItem();
        ActivityCategory category = categoryMap.get(project.getCategoryId());
        SchoolInfo school = schoolMap.get(project.getSchoolId());
        item.setId(project.getId());
        item.setProjectName(project.getProjectName());
        item.setSchoolName(school == null ? null : school.getSchoolName());
        item.setCategoryName(category == null ? null : category.getCategoryName());
        item.setStatus(project.getStatus());
        item.setSubmittedAt(project.getSubmittedAt());
        return item;
    }

    private AuditWorkbenchVo.AuditStats toWorkbenchStats(List<ProjectAuditRecord> records) {
        AuditWorkbenchVo.AuditStats stats = new AuditWorkbenchVo.AuditStats();
        stats.setTotalCount(records.size());
        stats.setPassCount(records.stream().filter(record -> ArtReviewConstants.AUDIT_PASS.equals(record.getAuditResult())).count());
        stats.setReturnCount(records.stream().filter(record -> ArtReviewConstants.AUDIT_RETURN.equals(record.getAuditResult())).count());
        return stats;
    }

    private List<AuditWorkbenchVo.GroupItem> toWorkbenchGroupItems(List<ProjectAuditRecord> records, Map<Long, Project> projectMap,
                                                                   Map<Long, ActivityCategory> categoryMap) {
        Map<String, AuditWorkbenchVo.GroupItem> groupItems = new LinkedHashMap<>();
        for (ProjectAuditRecord record : records) {
            Project project = projectMap.get(record.getProjectId());
            if (project == null) {
                continue;
            }
            ActivityCategory category = categoryMap.get(project.getCategoryId());
            String groupKey = categoryGroupKey(category);
            AuditWorkbenchVo.GroupItem item = groupItems.computeIfAbsent(groupKey, key -> {
                AuditWorkbenchVo.GroupItem created = new AuditWorkbenchVo.GroupItem();
                created.setGroupKey(key);
                created.setGroupName(categoryGroupName(key, category));
                created.setSortOrder(categoryGroupSortOrder(key));
                return created;
            });
            item.setTotalCount(item.getTotalCount() + 1);
            if (ArtReviewConstants.AUDIT_PASS.equals(record.getAuditResult())) {
                item.setPassCount(item.getPassCount() + 1);
            }
            if (ArtReviewConstants.AUDIT_RETURN.equals(record.getAuditResult())) {
                item.setReturnCount(item.getReturnCount() + 1);
            }
        }
        return groupItems.values().stream()
            .sorted(Comparator.comparing(AuditWorkbenchVo.GroupItem::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
            .toList();
    }

    private AuditWorkbenchVo.RecordItem toWorkbenchRecordItem(ProjectAuditRecord record, Map<Long, Project> projectMap,
                                                             Map<Long, ActivityCategory> categoryMap, Map<Long, SchoolInfo> schoolMap) {
        AuditWorkbenchVo.RecordItem item = new AuditWorkbenchVo.RecordItem();
        Project project = projectMap.get(record.getProjectId());
        ActivityCategory category = project == null ? null : categoryMap.get(project.getCategoryId());
        SchoolInfo school = project == null ? null : schoolMap.get(project.getSchoolId());
        item.setRecordId(record.getId());
        item.setProjectId(record.getProjectId());
        item.setProjectName(project == null ? null : project.getProjectName());
        item.setSchoolName(school == null ? null : school.getSchoolName());
        item.setCategoryName(category == null ? null : category.getCategoryName());
        item.setAuditResult(record.getAuditResult());
        item.setOpinion(record.getOpinion());
        item.setStatus(project == null ? null : project.getStatus());
        item.setAuditedAt(record.getAuditedAt());
        return item;
    }

    private String categoryGroupKey(ActivityCategory category) {
        if (category == null) {
            return "other";
        }
        String group = normalizeGroupValue(category.getCategoryGroup());
        String code = normalizeGroupValue(category.getCategoryCode());
        String text = StringUtils.blankToDefault(category.getCategoryGroup(), category.getCategoryName());
        if ("principal".equals(group) || "artwork_principal".equals(code) || "principal".equals(code) || text.contains("校长书画")) {
            return "principal";
        }
        if ("performance".equals(group) || code.startsWith("performance_") || text.contains("艺术表演")) {
            return "performance";
        }
        if ("artwork".equals(group) || code.startsWith("artwork_") || text.contains("艺术作品")) {
            return "artwork";
        }
        if ("workshop".equals(group) || "workshop".equals(code) || code.startsWith("workshop_") || text.contains("工作坊")) {
            return "workshop";
        }
        if ("achievement".equals(group) || code.startsWith("achievement_") || text.contains("美育") || text.contains("成果")) {
            return "achievement";
        }
        return StringUtils.isBlank(group) ? "other" : group;
    }

    private String categoryGroupName(String groupKey, ActivityCategory category) {
        return switch (StringUtils.blankToDefault(groupKey, "other")) {
            case "performance" -> "艺术表演类";
            case "artwork" -> "艺术作品类";
            case "principal" -> "高校校长书画作品";
            case "workshop" -> "艺术实践工作坊";
            case "achievement" -> "高校美育改革创新优秀成果";
            case "other" -> "其他类别";
            default -> category == null ? groupKey : StringUtils.blankToDefault(category.getCategoryGroup(), StringUtils.blankToDefault(category.getCategoryName(), groupKey));
        };
    }

    private int categoryGroupSortOrder(String groupKey) {
        return switch (StringUtils.blankToDefault(groupKey, "other")) {
            case "performance" -> 10;
            case "artwork" -> 20;
            case "principal" -> 30;
            case "workshop" -> 40;
            case "achievement" -> 50;
            default -> 90;
        };
    }

    private String normalizeGroupValue(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
