package org.dromara.crehn.project.service.submit;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.activity.service.ArtCategoryHierarchyService;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.common.ArtReviewSecurity;
import org.dromara.crehn.config.service.IArtActivityScopeService;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.CategoryFieldSchema;
import org.dromara.crehn.domain.CategoryFileRequirement;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ProjectFile;
import org.dromara.crehn.domain.ProjectMember;
import org.dromara.crehn.domain.bo.ProjectMemberBo;
import org.dromara.crehn.domain.bo.ProjectSaveBo;
import org.dromara.crehn.domain.vo.ProjectVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ActivityMapper;
import org.dromara.crehn.mapper.CategoryFieldSchemaMapper;
import org.dromara.crehn.mapper.CategoryFileRequirementMapper;
import org.dromara.crehn.mapper.ProjectFileMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.ProjectMemberMapper;
import org.dromara.crehn.project.service.ArtProjectSubmitValidator;
import org.dromara.crehn.project.service.FileRequirementRuleChecker;
import org.dromara.crehn.project.service.SubmitValidationResult;
import org.dromara.crehn.project.service.assembler.ProjectDetailAssembler;
import org.dromara.crehn.project.service.quota.ProjectQuotaGuard;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.service.ISysOssService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ART-OWNER: BE.PROJECT.DRAFT_SUBMIT
 * 草稿、提交、撤回流程服务，维持原门面事务边界和对外接口行为不变。
 */
@RequiredArgsConstructor
@Service
public class ProjectDraftSubmitService {

    private static final int FORM_DATA_JSON_MAX_BYTES = 60 * 1024;

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectFileMapper projectFileMapper;
    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final ArtCategoryHierarchyService categoryHierarchyService;
    private final CategoryFieldSchemaMapper fieldMapper;
    private final CategoryFileRequirementMapper fileRequirementMapper;
    private final ISysOssService ossService;
    private final ArtReviewSecurity artReviewSecurity;
    private final IArtActivityScopeService activityScopeService;
    private final FileRequirementRuleChecker fileRequirementRuleChecker;
    private final ArtProjectSubmitValidator projectSubmitValidator;
    private final ProjectDetailAssembler projectDetailAssembler;
    private final ProjectQuotaGuard projectQuotaGuard;

    public ProjectVo saveDraft(ProjectSaveBo bo) {
        Long schoolId = artReviewSecurity.requireEnabledSchoolId();
        if (StringUtils.isBlank(bo.getFormDataJson()) || !JsonUtils.isJsonObject(bo.getFormDataJson())) {
            throw new ServiceException("申报表单数据必须是 JSON 对象");
        }
        if (bo.getFormDataJson().getBytes(StandardCharsets.UTF_8).length > FORM_DATA_JSON_MAX_BYTES) {
            throw new ServiceException("填报数据过大，请精简通用表格内容或拆分申报");
        }

        Project project;
        if (bo.getId() == null) {
            requireCategoryAvailable(bo.getActivityId(), bo.getCategoryId());
            project = new Project();
            project.setActivityId(bo.getActivityId());
            project.setSchoolId(schoolId);
            if (artReviewSecurity.isCurrentParticipant()) {
                project.setParticipantUserId(LoginHelper.getUserId());
            }
            project.setCategoryId(bo.getCategoryId());
            project.setProjectNo("AR" + System.currentTimeMillis());
            project.setStatus(ArtReviewConstants.PROJECT_DRAFT);
        } else {
            project = requireProjectForUpdate(bo.getId());
            artReviewSecurity.checkProjectAccess(project);
            requireEditable(project);
            if (!Objects.equals(project.getActivityId(), bo.getActivityId()) || !Objects.equals(project.getCategoryId(), bo.getCategoryId())) {
                throw new ServiceException("项目保存后不可修改活动或类别");
            }
            requireCategoryAvailable(project.getActivityId(), project.getCategoryId());
        }
        project.setProjectName(bo.getProjectName());
        project.setGroupCode(bo.getGroupCode());
        project.setGroupName(bo.getGroupName());
        project.setFormDataJson(bo.getFormDataJson());
        if (bo.getId() == null) {
            projectMapper.insert(project);
        } else {
            projectMapper.updateById(project);
        }
        replaceMembers(project, bo.getMembers());
        return getProjectDetail(project.getId());
    }

    public void submit(Long id) {
        Project project = requireProjectForUpdate(id);
        artReviewSecurity.checkProjectAccess(project);
        projectQuotaGuard.lockSchoolSubmissionScope(project.getSchoolId());
        requireEditable(project);
        requireCategoryAvailable(project.getActivityId(), project.getCategoryId());
        validateSystemFields(project);
        validateRequiredFields(project);
        validateRequiredMemberFields(project);
        validateRequiredFiles(project);
        validateUploadedFileChecks(project);
        List<String> quotaWarnings = projectQuotaGuard.validateQuotaRules(project);
        SubmitValidationResult validation = projectSubmitValidator.validate(project);
        quotaWarnings.forEach(validation::warn);
        project.setValidationResultJson(JsonUtils.toJsonString(validation));
        boolean participant = artReviewSecurity.isCurrentParticipant();
        project.setStatus(participant
            ? ArtReviewConstants.PROJECT_PARTICIPANT_SUBMITTED
            : ArtReviewConstants.PROJECT_SUBMITTED);
        project.setSubmittedAt(new Date());
        project.setSubmittedBy(LoginHelper.getUserId());
        project.setParticipantSubmittedAt(participant ? project.getSubmittedAt() : null);
        if (participant) {
            project.setSchoolReviewStatus("pending");
            project.setSchoolReviewedAt(null);
        }
        project.setCurrentAuditOpinion(null);
        projectMapper.updateById(project);
    }

    public void withdrawSubmit(Long id) {
        Project project = requireProjectForUpdate(id);
        artReviewSecurity.checkProjectAccess(project);
        boolean participant = artReviewSecurity.isCurrentParticipant();
        if (participant && !ArtReviewConstants.PROJECT_PARTICIPANT_SUBMITTED.equals(project.getStatus())) {
            throw new ServiceException("只有本人已提交且学校尚未审核的作品可以撤回");
        }
        if (!participant && (!ArtReviewConstants.PROJECT_SUBMITTED.equals(project.getStatus())
            || project.getSchoolFinalBatchId() != null)) {
            throw new ServiceException("学校最终提交批次不可通过单个作品撤回");
        }
        project.setStatus(ArtReviewConstants.PROJECT_DRAFT);
        project.setSubmittedAt(null);
        project.setSubmittedBy(null);
        project.setParticipantSubmittedAt(null);
        project.setCurrentAuditOpinion(null);
        projectMapper.updateById(project);
    }

    private ProjectVo getProjectDetail(Long id) {
        ProjectVo vo = projectMapper.selectVoById(id);
        projectDetailAssembler.attachFilesAndAuditRecords(vo);
        return vo;
    }

    private void replaceMembers(Project project, List<ProjectMemberBo> members) {
        if (members == null) {
            return;
        }
        Map<Long, String> trustedMemberAttachmentPaths = trustedMemberAttachmentPaths(project.getId());
        projectMemberMapper.delete(Wrappers.lambdaQuery(ProjectMember.class)
            .eq(ProjectMember::getProjectId, project.getId()));
        int index = 1;
        for (ProjectMemberBo bo : members) {
            if (bo == null || isBlankMember(bo)) {
                continue;
            }
            ProjectMember member = new ProjectMember();
            member.setProjectId(project.getId());
            member.setActivityId(project.getActivityId());
            member.setSchoolId(project.getSchoolId());
            member.setMemberType(StringUtils.blankToDefault(bo.getMemberType(), "student"));
            member.setName(bo.getName());
            member.setStudentNo(bo.getStudentNo());
            member.setDepartment(bo.getDepartment());
            member.setMajor(bo.getMajor());
            member.setRoleName(bo.getRoleName());
            member.setExtraJson(normalizeExtraJson(bo.getExtraJson()));
            copyTrustedMemberAttachment(member, bo.getPhotoOssId(), true, trustedMemberAttachmentPaths);
            copyTrustedMemberAttachment(member, bo.getStudentReportOssId(), false, trustedMemberAttachmentPaths);
            member.setSortOrder(bo.getSortOrder() == null ? index : bo.getSortOrder());
            member.setStatus(StringUtils.blankToDefault(bo.getStatus(), "active"));
            projectMemberMapper.insert(member);
            index++;
        }
    }

    /**
     * 草稿重存会全量替换成员，只允许沿用本项目已有成员附件，防止前端把外部 OSS ID 混入项目。
     */
    private Map<Long, String> trustedMemberAttachmentPaths(Long projectId) {
        Set<Long> ids = projectMemberMapper.selectList(Wrappers.lambdaQuery(ProjectMember.class)
                .eq(ProjectMember::getProjectId, projectId))
            .stream()
            .flatMap(member -> Stream.of(member.getPhotoOssId(), member.getStudentReportOssId()))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> paths = new LinkedHashMap<>();
        ossService.listByIds(new ArrayList<>(ids)).forEach(oss -> paths.put(oss.getOssId(), oss.getUrl()));
        return paths;
    }

    private void copyTrustedMemberAttachment(ProjectMember member, Long ossId, boolean photo, Map<Long, String> trustedAttachmentPaths) {
        String path = ossId == null ? null : trustedAttachmentPaths.get(ossId);
        if (StringUtils.isBlank(path)) {
            return;
        }
        if (photo) {
            member.setPhotoOssId(ossId);
            member.setPhotoPath(path);
        } else {
            member.setStudentReportOssId(ossId);
            member.setStudentReportPath(path);
        }
    }

    private boolean isBlankMember(ProjectMemberBo bo) {
        if (StringUtils.isNotBlank(bo.getName()) || StringUtils.isNotBlank(bo.getStudentNo())
            || StringUtils.isNotBlank(bo.getDepartment()) || StringUtils.isNotBlank(bo.getMajor())
            || StringUtils.isNotBlank(bo.getRoleName())) {
            return false;
        }
        return !hasExtraJsonValue(bo.getExtraJson());
    }

    private boolean hasExtraJsonValue(String extraJson) {
        Map<String, Object> extra = parseRuleObject(extraJson);
        if (extra.isEmpty()) {
            return false;
        }
        for (Object value : extra.values()) {
            if (!isBlankValue(value)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeExtraJson(String extraJson) {
        if (StringUtils.isBlank(extraJson)) {
            return null;
        }
        if (!JsonUtils.isJsonObject(extraJson)) {
            throw new ServiceException("成员扩展信息必须是 JSON 对象");
        }
        return extraJson;
    }

    private Project requireProjectForUpdate(Long id) {
        Project project = projectMapper.selectOne(Wrappers.lambdaQuery(Project.class)
            .eq(Project::getId, id)
            .last("for update"));
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        return project;
    }

    private void requireEditable(Project project) {
        if (!ArtReviewConstants.PROJECT_DRAFT.equals(project.getStatus())
            && !ArtReviewConstants.PROJECT_RETURNED.equals(project.getStatus())
            && !ArtReviewConstants.PROJECT_PARTICIPANT_RETURNED.equals(project.getStatus())) {
            throw new ServiceException("项目当前状态不可编辑");
        }
    }

    private void requireCategoryAvailable(Long activityId, Long categoryId) {
        Date now = new Date();
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || !ArtReviewConstants.ACTIVITY_ENABLED.equals(activity.getStatus())) {
            throw new ServiceException("活动不存在或未启用");
        }
        if (activity.getSignupStartAt() != null && activity.getSignupStartAt().after(now)) {
            throw new ServiceException("活动报名尚未开始");
        }
        if (activity.getSignupEndAt() != null && activity.getSignupEndAt().before(now)) {
            throw new ServiceException("活动报名已结束");
        }
        Long schoolId = artReviewSecurity.requireEnabledSchoolId();
        String scopeType = StringUtils.blankToDefault(activity.getScopeType(), ArtReviewConstants.ACTIVITY_SCOPE_ALL);
        if (ArtReviewConstants.ACTIVITY_SCOPE_ASSIGNED.equals(scopeType) && !activityScopeService.isSchoolAllowed(activityId, schoolId)) {
            throw new ServiceException("当前学校未获得该活动授权");
        }
        ActivityCategory category = categoryMapper.selectById(categoryId);
        if (category == null || !Objects.equals(category.getActivityId(), activityId)) {
            throw new ServiceException("类别不存在或不属于当前活动");
        }
        categoryHierarchyService.requireBusinessAvailableLeaf(category);
    }

    // ART-REF: BE.PROJECT.SUBMIT_VALIDATION -> project/service/ArtProjectSubmitValidator.java
    private void validateRequiredFields(Project project) {
        Map<String, Object> formData = JsonUtils.parseObject(project.getFormDataJson(), new TypeReference<Map<String, Object>>() {
        });
        if (formData == null) {
            throw new ServiceException("申报表单数据必须是 JSON 对象");
        }
        List<CategoryFieldSchema> fields = fieldMapper.selectList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
            .eq(CategoryFieldSchema::getCategoryId, project.getCategoryId()));
        for (CategoryFieldSchema field : fields) {
            if (Boolean.TRUE.equals(field.getRequired()) && isBlankValue(formData.get(field.getFieldKey()))) {
                throw new ServiceException("必填字段缺失: " + field.getFieldKey());
            }
        }
    }

    private void validateSystemFields(Project project) {
        ActivityCategory category = categoryMapper.selectById(project.getCategoryId());
        Map<String, Object> rule = parseRuleObject(category == null ? null : category.getRuleJson());
        Object config = rule.get("projectName");
        boolean required = false;
        String label = "项目名称";
        if (config instanceof Map<?, ?> map) {
            required = Boolean.parseBoolean(String.valueOf(map.get("required")));
            label = StringUtils.blankToDefault(stringValue(map.get("label")), label);
        }
        if (required && StringUtils.isBlank(project.getProjectName())) {
            throw new ServiceException("必填字段缺失: " + label);
        }
    }

    private void validateRequiredMemberFields(Project project) {
        ActivityCategory category = categoryMapper.selectById(project.getCategoryId());
        Map<String, Object> rule = parseRuleObject(category == null ? null : category.getRuleJson());
        Object groups = rule.get("memberFieldGroups");
        if (!(groups instanceof Map<?, ?> groupMap)) {
            return;
        }
        List<ProjectMember> members = projectMemberMapper.selectList(Wrappers.lambdaQuery(ProjectMember.class)
            .eq(ProjectMember::getProjectId, project.getId())
            .eq(ProjectMember::getStatus, "active")
            .orderByAsc(ProjectMember::getSortOrder)
            .orderByAsc(ProjectMember::getId));
        for (ProjectMember member : members) {
            String groupKey = "author".equals(member.getMemberType()) ? "author" : "participant";
            Object schemaValue = groupMap.get(groupKey);
            if (!(schemaValue instanceof Iterable<?> schemas)) {
                continue;
            }
            Map<String, Object> extra = parseRuleObject(member.getExtraJson());
            for (Object schemaValueItem : schemas) {
                if (!(schemaValueItem instanceof Map<?, ?> schema)) {
                    continue;
                }
                boolean required = Boolean.parseBoolean(String.valueOf(schema.get("required")));
                if (!required) {
                    continue;
                }
                String fieldKey = stringValue(schema.get("fieldKey"));
                if (StringUtils.isBlank(fieldKey)) {
                    continue;
                }
                Object value = fixedMemberFieldValue(member, fieldKey);
                if (value == null) {
                    value = extra.get(fieldKey);
                }
                if (isBlankValue(value)) {
                    String label = StringUtils.blankToDefault(stringValue(schema.get("fieldLabel")), fieldKey);
                    throw new ServiceException("成员必填字段缺失: " + label);
                }
            }
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Object fixedMemberFieldValue(ProjectMember member, String fieldKey) {
        return switch (fieldKey) {
            case "memberType" -> member.getMemberType();
            case "name" -> member.getName();
            case "studentNo" -> member.getStudentNo();
            case "department" -> member.getDepartment();
            case "major" -> member.getMajor();
            case "roleName" -> member.getRoleName();
            case "photo" -> member.getPhotoOssId() != null ? member.getPhotoOssId() : member.getPhotoPath();
            case "studentReport" -> member.getStudentReportOssId() != null ? member.getStudentReportOssId() : member.getStudentReportPath();
            case "status" -> member.getStatus();
            default -> null;
        };
    }

    private Map<String, Object> parseRuleObject(String text) {
        if (StringUtils.isBlank(text) || !JsonUtils.isJsonObject(text)) {
            return Map.of();
        }
        Map<String, Object> parsed = JsonUtils.parseObject(text, new TypeReference<Map<String, Object>>() {
        });
        return parsed == null ? Map.of() : parsed;
    }

    private void validateRequiredFiles(Project project) {
        List<CategoryFileRequirement> requirements = fileRequirementMapper.selectList(Wrappers.lambdaQuery(CategoryFileRequirement.class)
            .eq(CategoryFileRequirement::getCategoryId, project.getCategoryId()));
        for (CategoryFileRequirement requirement : requirements) {
            long activeCount = countActiveFiles(project.getId(), requirement.getId());
            Integer minCount = requirement.getMinCount() == null ? 0 : requirement.getMinCount();
            if (Boolean.TRUE.equals(requirement.getRequired()) && activeCount < Math.max(1, minCount)) {
                throw new ServiceException("缺少必传材料：" + requirementDisplayName(requirement));
            }
            if (activeCount < minCount) {
                throw new ServiceException("材料数量不足：" + requirementDisplayName(requirement) + "，至少需要上传 " + minCount + " 个");
            }
        }
    }

    private void validateUploadedFileChecks(Project project) {
        List<ProjectFile> failedFiles = projectFileMapper.selectList(Wrappers.lambdaQuery(ProjectFile.class)
            .eq(ProjectFile::getProjectId, project.getId())
            .eq(ProjectFile::getStatus, ArtReviewConstants.FILE_ACTIVE)
            .eq(ProjectFile::getCheckStatus, "failed")
            .orderByAsc(ProjectFile::getRequirementId)
            .orderByAsc(ProjectFile::getVersionNo));
        if (failedFiles.isEmpty()) {
            return;
        }
        ActivityCategory category = categoryMapper.selectById(project.getCategoryId());
        Map<String, Object> categoryRule = parseRuleObject(category == null ? null : category.getRuleJson());
        List<Long> requirementIds = failedFiles.stream()
            .map(ProjectFile::getRequirementId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, CategoryFileRequirement> requirementMap = requirementIds.isEmpty() ? Map.of() : fileRequirementMapper.selectList(Wrappers.lambdaQuery(CategoryFileRequirement.class)
                .in(CategoryFileRequirement::getId, requirementIds))
            .stream()
            .collect(Collectors.toMap(CategoryFileRequirement::getId, item -> item, (left, right) -> left));
        for (ProjectFile failedFile : failedFiles) {
            CategoryFileRequirement requirement = requirementMap.get(failedFile.getRequirementId());
            if (technicalTipsOnly(requirement, categoryRule)) {
                continue;
            }
            throw new ServiceException("存在校验未通过的附件，请删除或替换后再提交：" + StringUtils.blankToDefault(failedFile.getOriginalName(), "未命名文件"));
        }
    }

    private boolean technicalTipsOnly(CategoryFileRequirement requirement, Map<String, Object> categoryRule) {
        return fileRequirementRuleChecker.technicalTipsOnly(requirement) || fileRequirementRuleChecker.technicalTipsOnly(categoryRule);
    }

    private String requirementDisplayName(CategoryFileRequirement requirement) {
        return StringUtils.blankToDefault(requirement.getFileTypeName(), StringUtils.blankToDefault(requirement.getFileTypeCode(), "材料"));
    }

    private long countActiveFiles(Long projectId, Long requirementId) {
        return projectFileMapper.selectCount(Wrappers.lambdaQuery(ProjectFile.class)
            .eq(ProjectFile::getProjectId, projectId)
            .eq(ProjectFile::getRequirementId, requirementId)
            .eq(ProjectFile::getStatus, ArtReviewConstants.FILE_ACTIVE));
    }

    private boolean isBlankValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String str) {
            return StringUtils.isBlank(str);
        }
        if (value instanceof List<?> list) {
            return list.isEmpty();
        }
        return false;
    }
}
