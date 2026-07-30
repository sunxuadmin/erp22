package org.dromara.crehn.project.service.assembler;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.CategoryFieldSchema;
import org.dromara.crehn.domain.CategoryFileRequirement;
import org.dromara.crehn.domain.ProjectFile;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ProjectMember;
import org.dromara.crehn.domain.SchoolInfo;
import org.dromara.crehn.domain.vo.CategoryFieldSchemaVo;
import org.dromara.crehn.domain.vo.CategoryFileRequirementVo;
import org.dromara.crehn.domain.vo.ProjectAuditRecordVo;
import org.dromara.crehn.domain.vo.ProjectFileVo;
import org.dromara.crehn.domain.vo.ProjectMemberVo;
import org.dromara.crehn.domain.vo.ProjectVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ActivityMapper;
import org.dromara.crehn.mapper.CategoryFieldSchemaMapper;
import org.dromara.crehn.mapper.CategoryFileRequirementMapper;
import org.dromara.crehn.mapper.ProjectAuditRecordMapper;
import org.dromara.crehn.mapper.ProjectFileMapper;
import org.dromara.crehn.mapper.ProjectMemberMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.SchoolInfoMapper;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysOssService;
import org.dromara.system.service.ISysConfigService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ART-OWNER: BE.PROJECT.DETAIL_ASSEMBLER
 * 项目详情和列表展示数据组装器，只补齐 VO 展示字段，不改变接口返回字段结构。
 */
@RequiredArgsConstructor
@Service
public class ProjectDetailAssembler {

    private static final String GROUP_NAME_FORM_KEYS_CONFIG_KEY = "crehn.review.project.groupName.formKeys";
    private static final List<String> DEFAULT_GROUP_NAME_FORM_KEYS = List.of(
        "groupName", "group_name", "group", "groupCode", "group_code",
        "displayGroup", "display_group", "designGroup", "design_group", "projectNature", "project_nature"
    );

    private final ProjectFileMapper projectFileMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectAuditRecordMapper auditRecordMapper;
    private final ProjectMapper projectMapper;
    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final CategoryFieldSchemaMapper fieldMapper;
    private final CategoryFileRequirementMapper fileRequirementMapper;
    private final SchoolInfoMapper schoolInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final ISysOssService ossService;
    private final ISysConfigService sysConfigService;

    /**
     * 详情接口一次性补齐子对象，字段名和排序沿用原 ArtProjectServiceImpl 行为。
     */
    public void attachFilesAndAuditRecords(ProjectVo vo) {
        if (vo == null) {
            return;
        }
        List<ProjectFileVo> files = projectFileMapper.selectVoList(Wrappers.lambdaQuery(ProjectFile.class)
            .eq(ProjectFile::getProjectId, vo.getId())
            .eq(ProjectFile::getStatus, ArtReviewConstants.FILE_ACTIVE)
            .orderByAsc(ProjectFile::getRequirementId)
            .orderByAsc(ProjectFile::getVersionNo));
        List<ProjectAuditRecordVo> records = auditRecordMapper.selectVoList(Wrappers.lambdaQuery(org.dromara.crehn.domain.ProjectAuditRecord.class)
            .eq(org.dromara.crehn.domain.ProjectAuditRecord::getProjectId, vo.getId())
            .orderByDesc(org.dromara.crehn.domain.ProjectAuditRecord::getAuditedAt));
        fillAuditRecordOperatorNames(records);
        List<ProjectMemberVo> members = projectMemberMapper.selectVoList(Wrappers.lambdaQuery(ProjectMember.class)
            .eq(ProjectMember::getProjectId, vo.getId())
            .orderByAsc(ProjectMember::getMemberType)
            .orderByAsc(ProjectMember::getSortOrder)
            .orderByAsc(ProjectMember::getId));
        Activity activity = activityMapper.selectById(vo.getActivityId());
        ActivityCategory category = categoryMapper.selectById(vo.getCategoryId());
        List<CategoryFieldSchemaVo> fields = fieldMapper.selectVoList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
            .eq(CategoryFieldSchema::getCategoryId, vo.getCategoryId())
            .orderByAsc(CategoryFieldSchema::getSortOrder)
            .orderByAsc(CategoryFieldSchema::getId));
        List<CategoryFileRequirementVo> requirements = fileRequirementMapper.selectVoList(Wrappers.lambdaQuery(CategoryFileRequirement.class)
            .eq(CategoryFileRequirement::getCategoryId, vo.getCategoryId())
            .orderByAsc(CategoryFileRequirement::getSortOrder)
            .orderByAsc(CategoryFileRequirement::getId));
        if (activity != null) {
            vo.setActivityName(activity.getActivityName());
        }
        SchoolInfo school = schoolInfoMapper.selectById(vo.getSchoolId());
        if (school != null) {
            vo.setSchoolName(school.getSchoolName());
        }
        if (category != null) {
            vo.setCategoryName(category.getCategoryName());
            vo.setCategoryRuleJson(category.getRuleJson());
            vo.setCategoryTipText(category.getTipText());
            vo.setCategoryCheckMode(category.getCheckMode());
        }
        refreshProjectFileAccessUrls(files);
        vo.setFiles(files);
        vo.setMembers(members);
        vo.setAuditRecords(records);
        vo.setFieldSchemas(fields);
        vo.setFileRequirements(requirements);
    }

    /**
     * 上传和预览返回前刷新签名访问地址，避免调用方直接依赖 OSS 表中的旧 URL。
     */
    public ProjectFileVo refreshProjectFileAccessUrl(ProjectFileVo file) {
        if (file == null) {
            return null;
        }
        refreshProjectFileAccessUrls(List.of(file));
        return file;
    }

    /**
     * 列表页补充活动、类别、学校与组别名称，避免把详情子表查询带入分页接口。
     */
    public void fillProjectSchoolNames(List<ProjectVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<String> groupNameFormKeys = resolvedGroupNameKeys();
        Map<Long, Project> groupSourceProjectMap = loadGroupSourceProjects(rows);
        List<Long> activityIds = rows.stream().map(ProjectVo::getActivityId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Activity> activityMap = activityIds.isEmpty()
            ? Map.of()
            : activityMapper.selectBatchIds(activityIds).stream().collect(Collectors.toMap(Activity::getId, item -> item, (left, right) -> left));
        List<Long> categoryIds = rows.stream().map(ProjectVo::getCategoryId).filter(Objects::nonNull).distinct().toList();
        Map<Long, ActivityCategory> categoryMap = categoryIds.isEmpty()
            ? Map.of()
            : categoryMapper.selectBatchIds(categoryIds).stream().collect(Collectors.toMap(ActivityCategory::getId, item -> item, (left, right) -> left));
        List<Long> schoolIds = rows.stream().map(ProjectVo::getSchoolId).filter(Objects::nonNull).distinct().toList();
        Map<Long, SchoolInfo> schoolMap = schoolIds.isEmpty()
            ? Map.of()
            : schoolInfoMapper.selectBatchIds(schoolIds).stream().collect(Collectors.toMap(SchoolInfo::getId, item -> item, (left, right) -> left));
        rows.forEach(row -> {
            Activity activity = activityMap.get(row.getActivityId());
            if (activity != null) {
                row.setActivityName(activity.getActivityName());
            }
            ActivityCategory category = categoryMap.get(row.getCategoryId());
            if (category != null) {
                row.setCategoryName(category.getCategoryName());
            }
            SchoolInfo school = schoolMap.get(row.getSchoolId());
            if (school != null) {
                row.setSchoolName(school.getSchoolName());
            }
            String groupDisplayName = resolveProjectGroupName(row, groupNameFormKeys);
            if (isBlankGroupName(groupDisplayName)) {
                groupDisplayName = resolveProjectGroupName(groupSourceProjectMap.get(row.getId()), groupNameFormKeys);
            }
            row.setGroupDisplayName(groupDisplayName);
            if (isBlankGroupName(row.getGroupName())) {
                row.setGroupName(groupDisplayName);
            }
        });
    }

    /**
     * 分页 VO 在个别查询链路中可能没有完整映射 formDataJson；仅对缺少该来源的记录批量回读，避免学校列表与审核列表的展示结果不一致。
     */
    private Map<Long, Project> loadGroupSourceProjects(List<ProjectVo> rows) {
        List<Long> projectIds = rows.stream()
            .filter(row -> row.getId() != null)
            .filter(row -> isBlankGroupName(row.getGroupName()))
            .filter(row -> StringUtils.isBlank(row.getFormDataJson()))
            .map(ProjectVo::getId)
            .distinct()
            .toList();
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        return projectMapper.selectBatchIds(projectIds).stream()
            .filter(Objects::nonNull)
            .filter(project -> project.getId() != null)
            .collect(Collectors.toMap(Project::getId, project -> project, (left, right) -> left));
    }

    private boolean isBlankGroupName(String value) {
        String normalized = StringUtils.trimToEmpty(value);
        return StringUtils.isBlank(normalized) || "-".equals(normalized);
    }

    private List<String> resolvedGroupNameKeys() {
        String rawConfig = StringUtils.trimToEmpty(sysConfigService.selectConfigByKey(GROUP_NAME_FORM_KEYS_CONFIG_KEY));
        if (StringUtils.isBlank(rawConfig)) {
            return DEFAULT_GROUP_NAME_FORM_KEYS;
        }
        List<String> configuredKeys = parseConfiguredFormKeys(rawConfig);
        return configuredKeys.isEmpty() ? DEFAULT_GROUP_NAME_FORM_KEYS : configuredKeys;
    }

    private List<String> parseConfiguredFormKeys(String rawConfig) {
        List<String> parsed = parseStringListFromJson(rawConfig);
        if (parsed.isEmpty()) {
            parsed = parseStringListByDelimiter(rawConfig);
        }
        return distinctNonBlank(parsed);
    }

    private List<String> parseStringListFromJson(String rawConfig) {
        try {
            List<String> parsed = JsonUtils.parseObject(rawConfig, new TypeReference<List<String>>() {
            });
            return parsed == null ? List.of() : parsed;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private List<String> parseStringListByDelimiter(String rawConfig) {
        if (StringUtils.isBlank(rawConfig)) {
            return List.of();
        }
        String[] split = rawConfig.split("[,，;；\\n\\r\\t ]+");
        List<String> parsed = new ArrayList<>();
        for (String item : split) {
            if (StringUtils.isNotBlank(item)) {
                parsed.add(item.trim());
            }
        }
        return parsed;
    }

    private List<String> distinctNonBlank(List<String> source) {
        return source == null || source.isEmpty()
            ? List.of()
            : source.stream()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    private String resolveProjectGroupName(ProjectVo row, List<String> groupNameFormKeys) {
        return resolveProjectGroupName(
            row == null ? null : row.getGroupName(),
            row == null ? null : row.getFormDataJson(),
            row == null ? null : row.getGroupCode(),
            groupNameFormKeys
        );
    }

    private String resolveProjectGroupName(Project row, List<String> groupNameFormKeys) {
        return resolveProjectGroupName(
            row == null ? null : row.getGroupName(),
            row == null ? null : row.getFormDataJson(),
            row == null ? null : row.getGroupCode(),
            groupNameFormKeys
        );
    }

    private String resolveProjectGroupName(String groupName, String formDataJson, String groupCode, List<String> groupNameFormKeys) {
        String explicitGroupName = StringUtils.trimToEmpty(groupName);
        if (!isBlankGroupName(explicitGroupName)) {
            return explicitGroupName;
        }
        Map<String, Object> formData = parseFormData(formDataJson);
        if (!formData.isEmpty() && groupNameFormKeys != null) {
            for (String key : groupNameFormKeys) {
                if (StringUtils.isBlank(key)) {
                    continue;
                }
                Object rawValue = formData.get(key);
                if (rawValue != null && !isBlankGroupName(String.valueOf(rawValue))) {
                    return StringUtils.trimToEmpty(String.valueOf(rawValue));
                }
            }
        }
        String normalizedGroupCode = StringUtils.trimToEmpty(groupCode);
        return isBlankGroupName(normalizedGroupCode) ? "" : normalizedGroupCode;
    }

    private Map<String, Object> parseFormData(String formDataJson) {
        if (StringUtils.isBlank(formDataJson) || !JsonUtils.isJsonObject(formDataJson)) {
            return Map.of();
        }
        Map<String, Object> parsed = JsonUtils.parseObject(formDataJson, new TypeReference<Map<String, Object>>() {
        });
        return parsed == null ? Map.of() : parsed;
    }

    /**
     * 列表脱敏只处理配置为 sensitive 的动态字段，保留原 JSON key 和整体结构。
     */
    public void maskSensitiveFormData(List<ProjectVo> records) {
        for (ProjectVo record : records) {
            Map<String, Object> formData = JsonUtils.parseObject(record.getFormDataJson(), new TypeReference<Map<String, Object>>() {
            });
            if (formData == null || formData.isEmpty()) {
                continue;
            }
            List<CategoryFieldSchema> fields = fieldMapper.selectList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
                .eq(CategoryFieldSchema::getCategoryId, record.getCategoryId())
                .eq(CategoryFieldSchema::getSensitiveFlag, true));
            if (fields.isEmpty()) {
                continue;
            }
            Map<String, Object> masked = new LinkedHashMap<>(formData);
            for (CategoryFieldSchema field : fields) {
                if (masked.containsKey(field.getFieldKey())) {
                    masked.put(field.getFieldKey(), maskValue(masked.get(field.getFieldKey())));
                }
            }
            record.setFormDataJson(JsonUtils.toJsonString(masked));
        }
    }

    private void refreshProjectFileAccessUrls(List<ProjectFileVo> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        Set<Long> ossIds = new LinkedHashSet<>();
        for (ProjectFileVo file : files) {
            if (file.getOssId() != null) {
                ossIds.add(file.getOssId());
            }
            if (file.getPreviewOssId() != null) {
                ossIds.add(file.getPreviewOssId());
            }
        }
        if (ossIds.isEmpty()) {
            return;
        }
        Map<Long, String> signedUrls = ossService.listByIds(ossIds).stream()
            .filter(oss -> oss.getOssId() != null && StringUtils.isNotBlank(oss.getUrl()))
            .collect(Collectors.toMap(SysOssVo::getOssId, SysOssVo::getUrl, (left, right) -> left));
        for (ProjectFileVo file : files) {
            if (file.getOssId() != null && signedUrls.containsKey(file.getOssId())) {
                file.setStoragePath(signedUrls.get(file.getOssId()));
            }
            if (file.getPreviewOssId() != null && signedUrls.containsKey(file.getPreviewOssId())) {
                file.setPreviewPath(signedUrls.get(file.getPreviewOssId()));
            }
        }
    }

    private void fillAuditRecordOperatorNames(List<ProjectAuditRecordVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream()
            .map(ProjectAuditRecordVo::getAuditedBy)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(userIds).stream()
            .collect(Collectors.toMap(SysUser::getUserId, item -> item, (left, right) -> left));
        for (ProjectAuditRecordVo record : records) {
            SysUser user = userMap.get(record.getAuditedBy());
            if (user != null) {
                record.setAuditedByName(StringUtils.blankToDefault(user.getNickName(), user.getUserName()));
            }
        }
    }

    private String maskValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        if (text.length() <= 4) {
            return "****";
        }
        return text.substring(0, 2) + "****" + text.substring(text.length() - 2);
    }
}
