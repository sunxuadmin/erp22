package org.dromara.crehn.activity.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.activity.service.ArtCategoryHierarchyService;
import org.dromara.crehn.activity.service.IArtActivityService;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.common.ArtReviewSecurity;
import org.dromara.crehn.config.service.IArtActivityScopeService;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.CategoryFieldSchema;
import org.dromara.crehn.domain.CategoryFileRequirement;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ProjectFile;
import org.dromara.crehn.domain.vo.ActivityCategoryVo;
import org.dromara.crehn.domain.vo.ActivityCategoryPurgeCheckVo;
import org.dromara.crehn.domain.vo.ActivityConfigCategoryPackageVo;
import org.dromara.crehn.domain.vo.ActivityConfigExcelRowVo;
import org.dromara.crehn.domain.vo.ActivityConfigImportResultVo;
import org.dromara.crehn.domain.vo.ActivityConfigMergePreviewVo;
import org.dromara.crehn.domain.vo.ActivityConfigPackageVo;
import org.dromara.crehn.domain.vo.ActivityDeleteCheckVo;
import org.dromara.crehn.domain.vo.ActivityVo;
import org.dromara.crehn.domain.vo.CategoryFieldSchemaVo;
import org.dromara.crehn.domain.vo.CategoryFileRequirementVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ActivityMapper;
import org.dromara.crehn.mapper.CategoryFieldSchemaMapper;
import org.dromara.crehn.mapper.CategoryFileRequirementMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.ProjectFileMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.tenant.core.TenantEntity;
import org.dromara.system.domain.SysOss;
import org.dromara.system.domain.SysOssExt;
import org.dromara.system.domain.vo.SysOssUploadVo;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.mapper.SysOssMapper;
import org.dromara.system.service.ISysOssService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.StringJoiner;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class ArtActivityServiceImpl implements IArtActivityService {

    private static final Pattern FIELD_KEY_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{1,63}$");
    private static final long FILE_REQUIREMENT_TEMPLATE_MAX_SIZE = 50L * 1024 * 1024;
    private static final Set<String> FILE_REQUIREMENT_TEMPLATE_EXTENSIONS = Set.of("xls", "xlsx", "doc", "docx", "pdf");
    private static final String FILE_REQUIREMENT_TEMPLATE_BIZ_TYPE = "art_file_requirement_template";
    private static final String FILE_REQUIREMENT_TEMPLATE_REF_TYPE = "art_category";

    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final CategoryFieldSchemaMapper fieldMapper;
    private final CategoryFileRequirementMapper fileRequirementMapper;
    private final ProjectMapper projectMapper;
    private final ProjectFileMapper projectFileMapper;
    private final ArtReviewSecurity artReviewSecurity;
    private final IArtActivityScopeService activityScopeService;
    private final ArtCategoryHierarchyService categoryHierarchyService;
    private final ISysOssService ossService;
    private final SysOssMapper ossMapper;

    @Override
    public TableDataInfo<ActivityVo> queryActivityPage(Activity activity, PageQuery pageQuery) {
        if ("1".equals(activity.getDelFlag())) {
            Page<ActivityVo> page = activityMapper.selectDeletedPage(pageQuery.build(), activity, "1");
            return TableDataInfo.build(page);
        }
        LambdaQueryWrapper<Activity> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(activity.getActivityName()), Activity::getActivityName, activity.getActivityName());
        lqw.eq(StringUtils.isNotBlank(activity.getStatus()), Activity::getStatus, activity.getStatus());
        lqw.eq(activity.getYear() != null, Activity::getYear, activity.getYear());
        lqw.orderByDesc(Activity::getCreateTime);
        Page<ActivityVo> page = activityMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public List<ActivityVo> listManagementActivityOptions() {
        return activityMapper.selectVoList(Wrappers.lambdaQuery(Activity.class)
            .select(Activity::getId, Activity::getActivityName, Activity::getMenuName,
                Activity::getYear, Activity::getStatus, Activity::getCreateTime)
            .orderByDesc(Activity::getCreateTime)
            .orderByDesc(Activity::getId));
    }

    @Override
    public List<ActivityCategoryVo> listManagementCategoryOptions(Long activityId) {
        return categoryMapper.selectVoList(Wrappers.lambdaQuery(ActivityCategory.class)
            .select(ActivityCategory::getId, ActivityCategory::getActivityId,
                ActivityCategory::getParentId, ActivityCategory::getCategoryCode,
                ActivityCategory::getCategoryName, ActivityCategory::getCategoryGroup,
                ActivityCategory::getRuleJson, ActivityCategory::getSortOrder, ActivityCategory::getEnabled)
            .eq(ActivityCategory::getActivityId, activityId)
            .orderByAsc(ActivityCategory::getSortOrder)
            .orderByAsc(ActivityCategory::getId))
            .stream()
            .filter(category -> !categoryHierarchyService.isGroup(category))
            .toList();
    }

    @Override
    public ActivityVo getActivity(Long id) {
        return activityMapper.selectVoById(id);
    }

    @Override
    public int addActivity(Activity activity) {
        if (StringUtils.isBlank(activity.getScopeType())) {
            activity.setScopeType(ArtReviewConstants.ACTIVITY_SCOPE_ALL);
        }
        return activityMapper.insert(activity);
    }

    @Override
    public int updateActivity(Activity activity) {
        return activityMapper.updateById(activity);
    }

    @Override
    public int deleteActivities(Long[] ids) {
        List<Long> idList = Arrays.asList(ids);
        for (Long id : idList) {
            ActivityDeleteCheckVo check = checkActivityDelete(id);
            if (!Boolean.TRUE.equals(check.getCanDelete())) {
                throw new ServiceException(buildActivityDeleteBlockMessage(check, false));
            }
        }
        return activityMapper.deleteByIds(idList);
    }

    @Override
    public int restoreActivities(Long[] ids) {
        return activityMapper.restoreByIds(Arrays.asList(ids));
    }

    @Override
    public ActivityDeleteCheckVo checkActivityDelete(Long id) {
        Activity activity = requireActivityForDeleteCheck(id, false);
        return buildActivityDeleteCheck(activity, false);
    }

    @Override
    public ActivityDeleteCheckVo checkActivityPurge(Long id) {
        Activity activity = requireActivityForDeleteCheck(id, true);
        return buildActivityDeleteCheck(activity, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int purgeActivity(Long id) {
        ActivityDeleteCheckVo check = checkActivityPurge(id);
        if (!Boolean.TRUE.equals(check.getCanDelete())) {
            throw new ServiceException(buildActivityDeleteBlockMessage(check, true));
        }
        activityMapper.purgeFieldSchemas(id);
        activityMapper.purgeFileRequirements(id);
        activityMapper.purgeSchoolScopes(id);
        activityMapper.purgeCategories(id);
        int rows = activityMapper.purgeDeletedActivity(id);
        if (rows < 1) {
            throw new ServiceException("活动不存在、已恢复或已被永久删除，请刷新后重试");
        }
        return rows;
    }

    private Activity requireActivityForDeleteCheck(Long id, boolean deleted) {
        if (id == null) {
            throw new ServiceException("活动ID不能为空");
        }
        Activity activity = activityMapper.selectAnyById(id);
        if (activity == null) {
            throw new ServiceException("活动不存在或已被永久删除");
        }
        if (deleted && !"1".equals(activity.getDelFlag())) {
            throw new ServiceException("仅已删除活动可以执行永久删除检查");
        }
        if (!deleted && "1".equals(activity.getDelFlag())) {
            throw new ServiceException("活动已删除，请在“已删活动”中恢复或永久删除");
        }
        return activity;
    }

    private ActivityDeleteCheckVo buildActivityDeleteCheck(Activity activity, boolean permanent) {
        Long activityId = activity.getId();
        ActivityDeleteCheckVo check = new ActivityDeleteCheckVo();
        check.setActivityId(activityId);
        check.setActivityName(activity.getActivityName());
        check.setDelFlag(activity.getDelFlag());

        long categoryCount = activityMapper.countAllCategories(activityId);
        long projectCount = activityMapper.countAllProjects(activityId);
        long fileCount = activityMapper.countAllProjectFiles(activityId);
        List<ActivityDeleteCheckVo.ReferenceItem> references =
            activityMapper.selectAllReferencesForDeleteCheck(activityId);
        long referenceCount = references.stream()
            .map(ActivityDeleteCheckVo.ReferenceItem::getReferenceCount)
            .filter(Objects::nonNull)
            .mapToLong(Long::longValue)
            .sum();

        check.setCategoryCount(categoryCount);
        check.setFieldSchemaCount(activityMapper.countAllFieldSchemas(activityId));
        check.setFileRequirementCount(activityMapper.countAllFileRequirements(activityId));
        check.setSchoolScopeCount(activityMapper.countAllSchoolScopes(activityId));
        check.setProjectCount(projectCount);
        check.setFileCount(fileCount);
        check.setReferenceCount(referenceCount);
        check.setCategories(activityMapper.selectAllCategoriesForDeleteCheck(activityId));
        check.setProjects(activityMapper.selectAllProjectsForDeleteCheck(activityId));
        check.setFiles(activityMapper.selectAllProjectFilesForDeleteCheck(activityId));
        check.setReferences(references);
        check.setCategoryDetailsTruncated(categoryCount > ActivityMapper.DELETE_CHECK_DETAIL_LIMIT);
        check.setProjectDetailsTruncated(projectCount > ActivityMapper.DELETE_CHECK_DETAIL_LIMIT);
        check.setFileDetailsTruncated(fileCount > ActivityMapper.DELETE_CHECK_DETAIL_LIMIT);

        boolean canDelete = projectCount == 0L && fileCount == 0L && referenceCount == 0L;
        check.setCanDelete(canDelete);
        check.setBlockMessage(canDelete ? "" : buildActivityDeleteBlockMessage(check, permanent));
        return check;
    }

    private String buildActivityDeleteBlockMessage(ActivityDeleteCheckVo check, boolean permanent) {
        StringJoiner reasons = new StringJoiner("、");
        if (positive(check.getProjectCount())) {
            reasons.add(check.getProjectCount() + " 个历史项目" + formatItemIds(check.getProjects().stream()
                .map(ActivityDeleteCheckVo.ProjectItem::getId).toList()));
        }
        if (positive(check.getFileCount())) {
            reasons.add(check.getFileCount() + " 个上传文件" + formatItemIds(check.getFiles().stream()
                .map(ActivityDeleteCheckVo.FileItem::getId).toList()));
        }
        if (positive(check.getReferenceCount())) {
            StringJoiner referenceReasons = new StringJoiner("，");
            check.getReferences().forEach(reference ->
                referenceReasons.add(reference.getReferenceLabel() + " " + reference.getReferenceCount()
                    + " 条" + formatRecordIds(reference.getRecordIds())));
            reasons.add("业务引用 " + check.getReferenceCount() + " 条（" + referenceReasons + "）");
        }
        return "活动“" + check.getActivityName() + "”（ID " + check.getActivityId() + "）存在："
            + reasons + "，不能" + (permanent ? "永久删除" : "删除")
            + "。请按检查明细处理后重新检查。";
    }

    @Override
    public ActivityConfigPackageVo exportActivityConfigPackage(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        ActivityConfigPackageVo configPackage = new ActivityConfigPackageVo();
        clearActivityForPackage(activity);
        configPackage.setActivity(activity);

        List<ActivityCategory> categories = categoryMapper.selectList(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getActivityId, activityId)
            .orderByAsc(ActivityCategory::getSortOrder)
            .orderByAsc(ActivityCategory::getId));
        for (ActivityCategory category : categories) {
            Long categoryId = category.getId();
            ActivityConfigCategoryPackageVo categoryPackage = new ActivityConfigCategoryPackageVo();
            clearCategoryForPackage(category);
            categoryPackage.setCategory(category);

            List<CategoryFieldSchema> fields = fieldMapper.selectList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
                .eq(CategoryFieldSchema::getCategoryId, categoryId)
                .orderByAsc(CategoryFieldSchema::getSortOrder)
                .orderByAsc(CategoryFieldSchema::getId));
            fields.forEach(this::clearFieldForPackage);
            categoryPackage.setFields(fields);

            List<CategoryFileRequirement> requirements = fileRequirementMapper.selectList(Wrappers.lambdaQuery(CategoryFileRequirement.class)
                .eq(CategoryFileRequirement::getCategoryId, categoryId)
                .orderByAsc(CategoryFileRequirement::getSortOrder)
                .orderByAsc(CategoryFileRequirement::getId));
            requirements.forEach(this::clearFileRequirementForPackage);
            categoryPackage.setFileRequirements(requirements);

            configPackage.getCategories().add(categoryPackage);
        }
        return configPackage;
    }

    @Override
    public List<ActivityConfigExcelRowVo> exportActivityConfigExcelRows(Long activityId) {
        ActivityConfigPackageVo configPackage = exportActivityConfigPackage(activityId);
        List<ActivityConfigExcelRowVo> rows = new ArrayList<>();
        Activity activity = configPackage.getActivity();
        ActivityConfigExcelRowVo activityRow = new ActivityConfigExcelRowVo();
        activityRow.setSection("activity");
        activityRow.setItemName(activity.getActivityName());
        activityRow.setSortOrder(0);
        activityRow.setPayloadJson(JsonUtils.toJsonString(activity));
        rows.add(activityRow);

        for (ActivityConfigCategoryPackageVo categoryPackage : configPackage.getCategories()) {
            ActivityCategory category = categoryPackage.getCategory();
            String categoryCode = category.getCategoryCode();
            ActivityConfigExcelRowVo categoryRow = new ActivityConfigExcelRowVo();
            categoryRow.setSection("category");
            categoryRow.setCategoryCode(categoryCode);
            categoryRow.setItemCode(categoryCode);
            categoryRow.setItemName(category.getCategoryName());
            categoryRow.setSortOrder(category.getSortOrder());
            categoryRow.setPayloadJson(JsonUtils.toJsonString(category));
            rows.add(categoryRow);

            for (CategoryFieldSchema field : categoryPackage.getFields()) {
                ActivityConfigExcelRowVo fieldRow = new ActivityConfigExcelRowVo();
                fieldRow.setSection("field");
                fieldRow.setCategoryCode(categoryCode);
                fieldRow.setItemCode(field.getFieldKey());
                fieldRow.setItemName(field.getFieldLabel());
                fieldRow.setSortOrder(field.getSortOrder());
                fieldRow.setPayloadJson(JsonUtils.toJsonString(field));
                rows.add(fieldRow);
            }

            for (CategoryFileRequirement requirement : categoryPackage.getFileRequirements()) {
                ActivityConfigExcelRowVo requirementRow = new ActivityConfigExcelRowVo();
                requirementRow.setSection("fileRequirement");
                requirementRow.setCategoryCode(categoryCode);
                requirementRow.setItemCode(requirement.getFileTypeCode());
                requirementRow.setItemName(requirement.getFileTypeName());
                requirementRow.setSortOrder(requirement.getSortOrder());
                requirementRow.setPayloadJson(JsonUtils.toJsonString(requirement));
                rows.add(requirementRow);
            }
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityConfigImportResultVo importActivityConfigPackage(ActivityConfigPackageVo configPackage) {
        validateActivityConfigPackage(configPackage);
        Activity activity = configPackage.getActivity();
        clearActivityForImport(activity);
        addActivity(activity);
        Long newActivityId = activity.getId();

        int fieldCount = 0;
        int fileRequirementCount = 0;
        for (ActivityConfigCategoryPackageVo categoryPackage : configPackage.getCategories()) {
            ActivityCategory category = categoryPackage.getCategory();
            clearCategoryForImport(category, newActivityId);
            addCategory(category);
            Long newCategoryId = category.getId();

            for (CategoryFieldSchema field : safeList(categoryPackage.getFields())) {
                clearFieldForImport(field, newCategoryId);
                addField(field);
                fieldCount++;
            }

            for (CategoryFileRequirement requirement : safeList(categoryPackage.getFileRequirements())) {
                clearFileRequirementForImport(requirement, newCategoryId);
                addFileRequirement(requirement);
                fileRequirementCount++;
            }
        }

        ActivityConfigImportResultVo result = new ActivityConfigImportResultVo();
        result.setActivityId(newActivityId);
        result.setActivityName(activity.getActivityName());
        result.setCategoryCount(configPackage.getCategories().size());
        result.setFieldCount(fieldCount);
        result.setFileRequirementCount(fileRequirementCount);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityConfigImportResultVo importActivityConfigExcelRows(List<ActivityConfigExcelRowVo> rows) {
        return importActivityConfigPackage(parseExcelRows(rows));
    }

    @Override
    public ActivityConfigMergePreviewVo previewActivityConfigMerge(Long activityId, ActivityConfigPackageVo configPackage) {
        return mergeActivityConfigInternal(activityId, configPackage, false);
    }

    @Override
    public ActivityConfigMergePreviewVo previewActivityConfigExcelMerge(Long activityId, List<ActivityConfigExcelRowVo> rows) {
        return previewActivityConfigMerge(activityId, parseExcelRows(rows));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityConfigMergePreviewVo mergeActivityConfig(Long activityId, ActivityConfigPackageVo configPackage) {
        return mergeActivityConfigInternal(activityId, configPackage, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityConfigMergePreviewVo mergeActivityConfigExcelRows(Long activityId, List<ActivityConfigExcelRowVo> rows) {
        return mergeActivityConfig(activityId, parseExcelRows(rows));
    }

    private ActivityConfigMergePreviewVo mergeActivityConfigInternal(Long activityId, ActivityConfigPackageVo configPackage, boolean apply) {
        if (activityId == null) {
            throw new ServiceException("当前活动ID不能为空");
        }
        Activity targetActivity = apply
            ? activityMapper.selectOne(Wrappers.lambdaQuery(Activity.class).eq(Activity::getId, activityId).last("FOR UPDATE"))
            : activityMapper.selectById(activityId);
        if (targetActivity == null) {
            throw new ServiceException("当前活动不存在或已删除，不能导入配置");
        }
        validateActivityConfigPackage(configPackage);

        ActivityConfigMergePreviewVo result = new ActivityConfigMergePreviewVo();
        result.setActivityId(activityId);
        result.setActivityName(targetActivity.getActivityName());
        result.setSourceActivityName(configPackage.getActivity().getActivityName());
        result.setApplied(apply);

        Map<String, ActivityCategoryVo> activeCategoryMap = new LinkedHashMap<>();
        for (ActivityCategoryVo category : listCategories(activityId, false)) {
            activeCategoryMap.put(normalizeConfigKey(category.getCategoryCode()), category);
        }
        Set<String> deletedCategoryCodes = new HashSet<>();
        for (ActivityCategoryVo category : listCategories(activityId, true)) {
            deletedCategoryCodes.add(normalizeConfigKey(category.getCategoryCode()));
        }

        for (ActivityConfigCategoryPackageVo categoryPackage : configPackage.getCategories()) {
            ActivityCategory sourceCategory = categoryPackage.getCategory();
            String categoryCode = normalizeConfigKey(sourceCategory.getCategoryCode());
            ActivityCategoryVo existingCategory = activeCategoryMap.get(categoryCode);

            if (deletedCategoryCodes.contains(categoryCode)) {
                addSkippedItem(result, "category", "类别", sourceCategory.getCategoryCode(), sourceCategory.getCategoryName(),
                    sourceCategory.getCategoryCode(), sourceCategory.getCategoryName(), "目标活动中存在已删除的同编码类别，请先恢复或永久删除");
                for (CategoryFieldSchema field : safeList(categoryPackage.getFields())) {
                    addSkippedItem(result, "field", "字段", sourceCategory.getCategoryCode(), sourceCategory.getCategoryName(),
                        field.getFieldKey(), field.getFieldLabel(), "所属类别已删除，本次不导入");
                }
                for (CategoryFileRequirement requirement : safeList(categoryPackage.getFileRequirements())) {
                    addSkippedItem(result, "fileRequirement", "附件要求", sourceCategory.getCategoryCode(), sourceCategory.getCategoryName(),
                        normalizedRequirementCode(requirement.getFileTypeCode()), requirement.getFileTypeName(), "所属类别已删除，本次不导入");
                }
                continue;
            }

            Long targetCategoryId;
            Set<String> fieldKeys = new HashSet<>();
            Set<String> requirementCodes = new HashSet<>();
            if (existingCategory == null) {
                ActivityCategory categoryToInsert = BeanUtil.copyProperties(sourceCategory, ActivityCategory.class);
                if (apply) {
                    clearCategoryForImport(categoryToInsert, activityId);
                    addCategory(categoryToInsert);
                    targetCategoryId = categoryToInsert.getId();
                } else {
                    targetCategoryId = null;
                }
                addAdditionItem(result, "category", "类别", sourceCategory.getCategoryCode(), sourceCategory.getCategoryName(),
                    sourceCategory.getCategoryCode(), sourceCategory.getCategoryName(), "类别编码不存在，将新增类别");
            } else {
                targetCategoryId = existingCategory.getId();
                addSkippedItem(result, "category", "类别", sourceCategory.getCategoryCode(), sourceCategory.getCategoryName(),
                    sourceCategory.getCategoryCode(), sourceCategory.getCategoryName(), "类别编码已存在，仅比较其字段和附件要求");
                for (CategoryFieldSchemaVo field : listFields(targetCategoryId, false)) {
                    fieldKeys.add(normalizeConfigKey(field.getFieldKey()));
                }
                for (CategoryFieldSchemaVo field : listFields(targetCategoryId, true)) {
                    fieldKeys.add(normalizeConfigKey(field.getFieldKey()));
                }
                for (CategoryFileRequirementVo requirement : listFileRequirements(targetCategoryId, false)) {
                    requirementCodes.add(normalizedRequirementCode(requirement.getFileTypeCode()));
                }
                for (CategoryFileRequirementVo requirement : listFileRequirements(targetCategoryId, true)) {
                    requirementCodes.add(normalizedRequirementCode(requirement.getFileTypeCode()));
                }
            }

            for (CategoryFieldSchema sourceField : safeList(categoryPackage.getFields())) {
                String fieldKey = normalizeConfigKey(sourceField.getFieldKey());
                if (!fieldKeys.add(fieldKey)) {
                    addSkippedItem(result, "field", "字段", sourceCategory.getCategoryCode(), sourceCategory.getCategoryName(),
                        sourceField.getFieldKey(), sourceField.getFieldLabel(), "同类别字段编码已存在");
                    continue;
                }
                if (apply) {
                    CategoryFieldSchema fieldToInsert = BeanUtil.copyProperties(sourceField, CategoryFieldSchema.class);
                    clearFieldForImport(fieldToInsert, targetCategoryId);
                    addField(fieldToInsert);
                }
                addAdditionItem(result, "field", "字段", sourceCategory.getCategoryCode(), sourceCategory.getCategoryName(),
                    sourceField.getFieldKey(), sourceField.getFieldLabel(), "字段编码不存在，将新增字段");
            }

            for (CategoryFileRequirement sourceRequirement : safeList(categoryPackage.getFileRequirements())) {
                String requirementCode = normalizedRequirementCode(sourceRequirement.getFileTypeCode());
                if (!requirementCodes.add(requirementCode)) {
                    addSkippedItem(result, "fileRequirement", "附件要求", sourceCategory.getCategoryCode(), sourceCategory.getCategoryName(),
                        requirementCode, sourceRequirement.getFileTypeName(), "同类别附件类型编码已存在");
                    continue;
                }
                if (apply) {
                    CategoryFileRequirement requirementToInsert = BeanUtil.copyProperties(sourceRequirement, CategoryFileRequirement.class);
                    clearFileRequirementForImport(requirementToInsert, targetCategoryId);
                    addFileRequirement(requirementToInsert);
                }
                addAdditionItem(result, "fileRequirement", "附件要求", sourceCategory.getCategoryCode(), sourceCategory.getCategoryName(),
                    requirementCode, sourceRequirement.getFileTypeName(), "附件类型编码不存在，将新增附件要求");
            }
        }

        result.setHasAdditions(result.getAddedCategoryCount() + result.getAddedFieldCount()
            + result.getAddedFileRequirementCount() > 0);
        return result;
    }

    private void addAdditionItem(ActivityConfigMergePreviewVo result, String itemType, String itemTypeLabel,
                                  String categoryCode, String categoryName, String itemCode, String itemName, String reason) {
        result.getAdditions().add(configMergeItem(itemType, itemTypeLabel, categoryCode, categoryName, itemCode, itemName, reason));
        switch (itemType) {
            case "category" -> result.setAddedCategoryCount(result.getAddedCategoryCount() + 1);
            case "field" -> result.setAddedFieldCount(result.getAddedFieldCount() + 1);
            case "fileRequirement" -> result.setAddedFileRequirementCount(result.getAddedFileRequirementCount() + 1);
            default -> throw new ServiceException("不支持的配置项类型: " + itemType);
        }
    }

    private void addSkippedItem(ActivityConfigMergePreviewVo result, String itemType, String itemTypeLabel,
                                 String categoryCode, String categoryName, String itemCode, String itemName, String reason) {
        result.getSkipped().add(configMergeItem(itemType, itemTypeLabel, categoryCode, categoryName, itemCode, itemName, reason));
        switch (itemType) {
            case "category" -> result.setSkippedCategoryCount(result.getSkippedCategoryCount() + 1);
            case "field" -> result.setSkippedFieldCount(result.getSkippedFieldCount() + 1);
            case "fileRequirement" -> result.setSkippedFileRequirementCount(result.getSkippedFileRequirementCount() + 1);
            default -> throw new ServiceException("不支持的配置项类型: " + itemType);
        }
    }

    private ActivityConfigMergePreviewVo.Item configMergeItem(String itemType, String itemTypeLabel,
                                                                String categoryCode, String categoryName,
                                                                String itemCode, String itemName, String reason) {
        ActivityConfigMergePreviewVo.Item item = new ActivityConfigMergePreviewVo.Item();
        item.setItemType(itemType);
        item.setItemTypeLabel(itemTypeLabel);
        item.setCategoryCode(categoryCode);
        item.setCategoryName(categoryName);
        item.setItemCode(itemCode);
        item.setItemName(itemName);
        item.setReason(reason);
        return item;
    }

    private String normalizeConfigKey(String value) {
        return StringUtils.blankToDefault(value, "").trim().toLowerCase(Locale.ROOT);
    }

    private String normalizedRequirementCode(String value) {
        return StringUtils.isBlank(value) ? "all" : value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public List<ActivityCategoryVo> listCategories(Long activityId, Boolean deleted) {
        if (Boolean.TRUE.equals(deleted)) {
            return categoryMapper.selectByActivityAndDelFlag(activityId, "1");
        }
        return categoryMapper.selectVoList(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getActivityId, activityId)
            .orderByAsc(ActivityCategory::getSortOrder)
            .orderByAsc(ActivityCategory::getId));
    }

    @Override
    public ActivityCategoryVo getCategory(Long id) {
        return categoryMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addCategory(ActivityCategory category) {
        categoryHierarchyService.normalizeForSave(category);
        return categoryMapper.insert(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityCategoryVo copyCategory(Long id) {
        if (id == null) {
            throw new ServiceException("类别ID不能为空");
        }
        ActivityCategory source = categoryMapper.selectById(id);
        if (source == null) {
            throw new ServiceException("类别不存在或已删除");
        }

        List<ActivityCategory> activityCategories = categoryMapper.selectList(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getActivityId, source.getActivityId())
            .orderByAsc(ActivityCategory::getSortOrder)
            .orderByAsc(ActivityCategory::getId));
        Set<String> categoryCodes = activityCategories.stream()
            .map(ActivityCategory::getCategoryCode)
            .filter(StringUtils::isNotBlank)
            .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        Set<String> categoryNames = activityCategories.stream()
            .map(ActivityCategory::getCategoryName)
            .filter(StringUtils::isNotBlank)
            .collect(java.util.stream.Collectors.toCollection(HashSet::new));

        boolean sourceIsGroup = categoryHierarchyService.isGroup(source);
        ActivityCategory copiedRoot = insertCategoryCopy(source, sourceIsGroup ? 0L : source.getParentId(), categoryCodes, categoryNames);
        if (sourceIsGroup) {
            activityCategories.stream()
                .filter(category -> isCopyChildOfGroup(category, source))
                .forEach(child -> {
                    ActivityCategory copiedChild = insertCategoryCopy(child, copiedRoot.getId(), categoryCodes, categoryNames);
                    copyCategoryConfiguration(child.getId(), copiedChild.getId());
                });
        } else {
            copyCategoryConfiguration(source.getId(), copiedRoot.getId());
        }

        ActivityCategoryVo copied = categoryMapper.selectVoById(copiedRoot.getId());
        if (copied == null) {
            throw new ServiceException("复制类别后读取副本失败");
        }
        return copied;
    }

    private ActivityCategory insertCategoryCopy(ActivityCategory source, Long parentId,
                                                Set<String> categoryCodes, Set<String> categoryNames) {
        ActivityCategory copy = BeanUtil.copyProperties(source, ActivityCategory.class);
        clearCategoryForImport(copy, source.getActivityId());
        copy.setParentId(parentId == null ? 0L : parentId);
        copy.setCategoryCode(nextCopyCategoryCode(source.getCategoryCode(), categoryCodes));
        copy.setCategoryName(nextCopyCategoryName(source.getCategoryName(), categoryNames));
        categoryHierarchyService.normalizeForSave(copy);
        if (categoryMapper.insert(copy) < 1 || copy.getId() == null) {
            throw new ServiceException("复制类别失败");
        }
        return copy;
    }

    private boolean isCopyChildOfGroup(ActivityCategory category, ActivityCategory group) {
        if (category == null || group == null || Objects.equals(category.getId(), group.getId()) || categoryHierarchyService.isGroup(category)) {
            return false;
        }
        if (Objects.equals(category.getParentId(), group.getId())) {
            return true;
        }
        return (category.getParentId() == null || category.getParentId() == 0L)
            && StringUtils.isNotBlank(category.getCategoryGroup())
            && !Objects.equals(category.getCategoryName(), group.getCategoryName())
            && Objects.equals(category.getCategoryGroup(), group.getCategoryName());
    }

    private void copyCategoryConfiguration(Long sourceCategoryId, Long targetCategoryId) {
        List<CategoryFieldSchema> fields = fieldMapper.selectList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
            .eq(CategoryFieldSchema::getCategoryId, sourceCategoryId)
            .orderByAsc(CategoryFieldSchema::getSortOrder)
            .orderByAsc(CategoryFieldSchema::getId));
        for (CategoryFieldSchema field : fields) {
            CategoryFieldSchema copy = BeanUtil.copyProperties(field, CategoryFieldSchema.class);
            clearFieldForImport(copy, targetCategoryId);
            addField(copy);
        }

        List<CategoryFileRequirement> requirements = fileRequirementMapper.selectList(Wrappers.lambdaQuery(CategoryFileRequirement.class)
            .eq(CategoryFileRequirement::getCategoryId, sourceCategoryId)
            .orderByAsc(CategoryFileRequirement::getSortOrder)
            .orderByAsc(CategoryFileRequirement::getId));
        for (CategoryFileRequirement requirement : requirements) {
            CategoryFileRequirement copy = BeanUtil.copyProperties(requirement, CategoryFileRequirement.class);
            clearFileRequirementForImport(copy, targetCategoryId);
            addFileRequirement(copy);
        }
    }

    private String nextCopyCategoryCode(String sourceCode, Set<String> existingCodes) {
        String base = trimToLength(StringUtils.blankToDefault(StringUtils.trim(sourceCode), "category") + "_copy", 64);
        String candidate = base;
        for (int index = 2; existingCodes.contains(candidate); index++) {
            String suffix = "_" + index;
            candidate = trimToLength(base, 64 - suffix.length()) + suffix;
        }
        existingCodes.add(candidate);
        return candidate;
    }

    private String nextCopyCategoryName(String sourceName, Set<String> existingNames) {
        String base = trimToLength(StringUtils.blankToDefault(StringUtils.trim(sourceName), "类别") + " 副本", 128);
        String candidate = base;
        for (int index = 2; existingNames.contains(candidate); index++) {
            String suffix = String.valueOf(index);
            candidate = trimToLength(base, 128 - suffix.length()) + suffix;
        }
        existingNames.add(candidate);
        return candidate;
    }

    private String trimToLength(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateCategory(ActivityCategory category) {
        if (category == null || category.getId() == null) {
            throw new ServiceException("类别ID不能为空");
        }
        ActivityCategory existing = categoryMapper.selectById(category.getId());
        if (existing == null) {
            throw new ServiceException("类别不存在或已删除");
        }
        boolean existingGroup = categoryHierarchyService.isGroup(existing);
        categoryHierarchyService.normalizeForSave(category);
        boolean nextGroup = categoryHierarchyService.isGroup(category);
        if (existingGroup != nextGroup && categoryHierarchyService.hasChildren(existing)) {
            throw new ServiceException("大类下还有子类别，不能变更节点类型");
        }
        int rows = categoryMapper.updateById(category);
        if (existingGroup && nextGroup && !Objects.equals(existing.getCategoryName(), category.getCategoryName())) {
            categoryMapper.update(null, Wrappers.lambdaUpdate(ActivityCategory.class)
                .set(ActivityCategory::getCategoryGroup, category.getCategoryName())
                .eq(ActivityCategory::getActivityId, existing.getActivityId())
                .and(wrapper -> wrapper.eq(ActivityCategory::getParentId, existing.getId())
                    .or()
                    .eq(ActivityCategory::getCategoryGroup, existing.getCategoryName())));
        }
        return rows;
    }

    @Override
    public int deleteCategories(Long[] ids) {
        List<Long> idList = Arrays.asList(ids);
        for (Long id : idList) {
            ActivityCategoryPurgeCheckVo check = checkCategoryDelete(id);
            if (!Boolean.TRUE.equals(check.getCanPurge())) {
                throw new ServiceException(buildCategoryDeleteBlockMessage(check, false));
            }
        }
        return categoryMapper.deleteByIds(idList);
    }

    @Override
    public int restoreCategories(Long[] ids) {
        return categoryMapper.restoreByIds(Arrays.asList(ids));
    }

    @Override
    public ActivityCategoryPurgeCheckVo checkCategoryDelete(Long id) {
        if (id == null) {
            throw new ServiceException("类别ID不能为空");
        }
        ActivityCategory category = categoryMapper.selectAnyById(id);
        if (category == null) {
            throw new ServiceException("类别不存在或已被永久删除");
        }
        if ("1".equals(category.getDelFlag())) {
            throw new ServiceException("类别已删除，请在“已删类”中恢复或永久删除");
        }
        return buildCategoryDeleteCheck(category, false);
    }

    @Override
    public ActivityCategoryPurgeCheckVo checkCategoryPurge(Long id) {
        if (id == null) {
            throw new ServiceException("类别ID不能为空");
        }
        ActivityCategory category = categoryMapper.selectDeletedById(id);
        if (category == null) {
            throw new ServiceException("仅已删除类别可以执行永久删除检查");
        }
        return buildCategoryDeleteCheck(category, true);
    }

    private ActivityCategoryPurgeCheckVo buildCategoryDeleteCheck(ActivityCategory category, boolean permanent) {
        ActivityCategoryPurgeCheckVo check = new ActivityCategoryPurgeCheckVo();
        check.setCategoryId(category.getId());
        check.setCategoryCode(category.getCategoryCode());
        check.setCategoryName(category.getCategoryName());

        long childCount = 0L;
        if (categoryHierarchyService.isGroup(category)) {
            childCount = categoryMapper.countAllChildren(category.getActivityId(), category.getId(), category.getCategoryName());
            check.setChildren(categoryMapper.selectAllChildrenForPurge(
                category.getActivityId(), category.getId(), category.getCategoryName()));
        }
        long projectCount = categoryMapper.countAllProjects(category.getId());
        long fileCount = categoryMapper.countAllProjectFiles(category.getId());
        List<ActivityCategoryPurgeCheckVo.ReferenceItem> references =
            categoryMapper.selectAllReferencesForPurge(category.getId());
        long referenceCount = references.stream()
            .map(ActivityCategoryPurgeCheckVo.ReferenceItem::getReferenceCount)
            .filter(Objects::nonNull)
            .mapToLong(Long::longValue)
            .sum();

        check.setChildCount(childCount);
        check.setProjectCount(projectCount);
        check.setFileCount(fileCount);
        check.setReferenceCount(referenceCount);
        check.setFieldSchemaCount(categoryMapper.countAllFieldSchemas(category.getId()));
        check.setFileRequirementCount(categoryMapper.countAllFileRequirements(category.getId()));
        check.setProjects(categoryMapper.selectAllProjectsForPurge(category.getId()));
        check.setFiles(categoryMapper.selectAllProjectFilesForPurge(category.getId()));
        check.setReferences(references);
        check.setChildDetailsTruncated(childCount > ActivityCategoryMapper.PURGE_DETAIL_LIMIT);
        check.setProjectDetailsTruncated(projectCount > ActivityCategoryMapper.PURGE_DETAIL_LIMIT);
        check.setFileDetailsTruncated(fileCount > ActivityCategoryMapper.PURGE_DETAIL_LIMIT);

        boolean canPurge = childCount == 0L && projectCount == 0L && fileCount == 0L && referenceCount == 0L;
        check.setCanPurge(canPurge);
        check.setBlockMessage(canPurge ? "" : buildCategoryDeleteBlockMessage(check, permanent));
        return check;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int purgeCategory(Long id) {
        ActivityCategoryPurgeCheckVo check = checkCategoryPurge(id);
        if (!Boolean.TRUE.equals(check.getCanPurge())) {
            throw new ServiceException(buildCategoryDeleteBlockMessage(check, true));
        }
        categoryMapper.purgeFieldSchemas(id);
        categoryMapper.purgeFileRequirements(id);
        int rows = categoryMapper.purgeDeletedCategory(id);
        if (rows < 1) {
            throw new ServiceException("类别不存在、已恢复或已被永久删除，请刷新后重试");
        }
        return rows;
    }

    private String buildCategoryDeleteBlockMessage(ActivityCategoryPurgeCheckVo check, boolean permanent) {
        StringJoiner reasons = new StringJoiner("、");
        if (positive(check.getChildCount())) {
            reasons.add(check.getChildCount() + " 个子类别" + formatItemIds(check.getChildren().stream()
                .map(ActivityCategoryPurgeCheckVo.ChildItem::getId).toList()));
        }
        if (positive(check.getProjectCount())) {
            reasons.add(check.getProjectCount() + " 个历史项目" + formatItemIds(check.getProjects().stream()
                .map(ActivityCategoryPurgeCheckVo.ProjectItem::getId).toList()));
        }
        if (positive(check.getFileCount())) {
            reasons.add(check.getFileCount() + " 个上传文件" + formatItemIds(check.getFiles().stream()
                .map(ActivityCategoryPurgeCheckVo.FileItem::getId).toList()));
        }
        if (positive(check.getReferenceCount())) {
            StringJoiner referenceReasons = new StringJoiner("，");
            check.getReferences().forEach(reference ->
                referenceReasons.add(reference.getReferenceLabel() + " " + reference.getReferenceCount()
                    + " 条" + formatRecordIds(reference.getRecordIds())));
            reasons.add("外部业务引用 " + check.getReferenceCount() + " 条（" + referenceReasons + "）");
        }
        return "类别“" + check.getCategoryName() + "”（ID " + check.getCategoryId() + "）存在："
            + reasons + "，不能" + (permanent ? "永久删除" : "删除")
            + "。请按检查明细处理后重新检查。";
    }

    private boolean positive(Long value) {
        return value != null && value > 0L;
    }

    private String formatItemIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return "（ID：" + ids.stream().filter(Objects::nonNull).limit(20)
            .map(String::valueOf).reduce((left, right) -> left + "," + right).orElse("") + "）";
    }

    private String formatRecordIds(String recordIds) {
        if (StringUtils.isBlank(recordIds)) {
            return "";
        }
        String[] ids = recordIds.split(",");
        return "（ID：" + Arrays.stream(ids).limit(20)
            .reduce((left, right) -> left + "," + right).orElse("") + "）";
    }

    @Override
    public List<CategoryFieldSchemaVo> listFields(Long categoryId, Boolean deleted) {
        if (Boolean.TRUE.equals(deleted)) {
            return fieldMapper.selectByCategoryAndDelFlag(categoryId, "1");
        }
        return fieldMapper.selectVoList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
            .eq(CategoryFieldSchema::getCategoryId, categoryId)
            .orderByAsc(CategoryFieldSchema::getSortOrder)
            .orderByAsc(CategoryFieldSchema::getId));
    }

    @Override
    public int addField(CategoryFieldSchema field) {
        normalizeAndValidateField(field);
        return fieldMapper.insert(field);
    }

    @Override
    public int updateField(CategoryFieldSchema field) {
        normalizeAndValidateField(field);
        return fieldMapper.updateById(field);
    }

    @Override
    public int deleteFields(Long[] ids) {
        return fieldMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int restoreFields(Long[] ids) {
        return fieldMapper.restoreByIds(Arrays.asList(ids));
    }

    @Override
    public long countFieldData(Long id) {
        CategoryFieldSchema field = fieldMapper.selectById(id);
        if (field == null || StringUtils.isBlank(field.getFieldKey())) {
            return 0L;
        }
        String fieldKeyNeedle = "\"" + field.getFieldKey().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        Long count = projectMapper.selectCount(Wrappers.lambdaQuery(Project.class)
            .eq(Project::getCategoryId, field.getCategoryId())
            .isNotNull(Project::getFormDataJson)
            .ne(Project::getFormDataJson, "")
            .like(Project::getFormDataJson, fieldKeyNeedle));
        return count == null ? 0L : count;
    }

    @Override
    public List<CategoryFileRequirementVo> listFileRequirements(Long categoryId, Boolean deleted) {
        if (Boolean.TRUE.equals(deleted)) {
            return fileRequirementMapper.selectByCategoryAndDelFlag(categoryId, "1");
        }
        return fileRequirementMapper.selectVoList(Wrappers.lambdaQuery(CategoryFileRequirement.class)
            .eq(CategoryFileRequirement::getCategoryId, categoryId)
            .orderByAsc(CategoryFileRequirement::getSortOrder)
            .orderByAsc(CategoryFileRequirement::getId));
    }

    @Override
    public int addFileRequirement(CategoryFileRequirement requirement) {
        normalizeFileRequirement(requirement);
        return fileRequirementMapper.insert(requirement);
    }

    @Override
    public int updateFileRequirement(CategoryFileRequirement requirement) {
        normalizeFileRequirement(requirement);
        return fileRequirementMapper.updateById(requirement);
    }

    @Override
    public int deleteFileRequirements(Long[] ids) {
        return fileRequirementMapper.deleteByIds(Arrays.asList(ids));
    }

    @Override
    public int restoreFileRequirements(Long[] ids) {
        return fileRequirementMapper.restoreByIds(Arrays.asList(ids));
    }

    @Override
    public long countFileRequirementData(Long id) {
        Long count = projectFileMapper.selectCount(Wrappers.lambdaQuery(ProjectFile.class)
            .eq(ProjectFile::getRequirementId, id));
        return count == null ? 0L : count;
    }

    @Override
    public SysOssUploadVo uploadCategoryFileRequirementTemplate(Long categoryId, MultipartFile file) {
        requireTemplateConfigCategory(categoryId);
        validateFileRequirementTemplate(file);
        SysOssVo oss = ossService.upload(file);
        bindFileRequirementTemplate(oss, categoryId);
        SysOssUploadVo uploadVo = new SysOssUploadVo();
        uploadVo.setFileName(oss.getOriginalName());
        uploadVo.setOssId(oss.getOssId().toString());
        return uploadVo;
    }

    @Override
    public void downloadCategoryFileRequirementTemplate(Long categoryId, Long requirementId, HttpServletResponse response) throws IOException {
        requireAvailableSchoolCategory(categoryId);
        CategoryFileRequirement requirement = fileRequirementMapper.selectById(requirementId);
        if (requirement == null || !Objects.equals(categoryId, requirement.getCategoryId())) {
            throw new ServiceException("附件要求不存在或不属于当前类别");
        }
        Map<String, Object> rule = fileRequirementRule(requirement.getRuleJson());
        Long ossId = positiveLong(rule.get("templateOssId"));
        if (ossId == null || Boolean.FALSE.equals(rule.get("templateEnabled"))) {
            throw new ServiceException("下载模板不存在或未启用");
        }
        SysOssVo oss = ossService.getById(ossId);
        if (!isBoundFileRequirementTemplate(oss, categoryId)) {
            throw new ServiceException("下载模板不存在或未启用");
        }
        ossService.download(ossId, response);
    }

    @Override
    public List<ActivityVo> listSchoolAvailableActivities() {
        Long schoolId = artReviewSecurity.requireEnabledSchoolId();
        Date now = new Date();
        return activityMapper.selectVoList(Wrappers.lambdaQuery(Activity.class)
            .eq(Activity::getStatus, ArtReviewConstants.ACTIVITY_ENABLED)
            .and(w -> w.isNull(Activity::getSignupStartAt).or().le(Activity::getSignupStartAt, now))
            .and(w -> w.isNull(Activity::getSignupEndAt).or().ge(Activity::getSignupEndAt, now))
            .orderByDesc(Activity::getCreateTime))
            .stream()
            .filter(activity -> isActivityAllowedForSchool(activity, schoolId))
            .toList();
    }

    @Override
    public List<ActivityCategoryVo> listSchoolAvailableCategories(Long activityId) {
        Long schoolId = artReviewSecurity.requireEnabledSchoolId();
        requireAvailableActivity(activityId);
        requireActivityScope(activityId, schoolId);
        List<ActivityCategoryVo> catalog = categoryMapper.selectVoList(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getActivityId, activityId)
            .orderByAsc(ActivityCategory::getSortOrder)
            .orderByAsc(ActivityCategory::getId));
        return catalog.stream()
            .filter(category -> categoryHierarchyService.isBusinessAvailableLeaf(category, catalog))
            .toList();
    }

    @Override
    public List<ActivityCategoryVo> listSchoolCategoryCatalog(Long activityId) {
        Long schoolId = artReviewSecurity.requireEnabledSchoolId();
        requireAvailableActivity(activityId);
        requireActivityScope(activityId, schoolId);
        return categoryMapper.selectVoList(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getActivityId, activityId)
            .orderByAsc(ActivityCategory::getSortOrder)
            .orderByAsc(ActivityCategory::getId));
    }

    @Override
    public List<CategoryFieldSchemaVo> listSchoolCategoryFields(Long categoryId) {
        requireAvailableSchoolCategory(categoryId);
        return listFields(categoryId, false);
    }

    @Override
    public List<CategoryFileRequirementVo> listSchoolCategoryFileRequirements(Long categoryId) {
        requireAvailableSchoolCategory(categoryId);
        return listFileRequirements(categoryId, false);
    }

    private void requireAvailableSchoolCategory(Long categoryId) {
        Long schoolId = artReviewSecurity.requireEnabledSchoolId();
        ActivityCategory category = categoryMapper.selectById(categoryId);
        categoryHierarchyService.requireBusinessAvailableLeaf(category);
        requireAvailableActivity(category.getActivityId());
        requireActivityScope(category.getActivityId(), schoolId);
    }

    private void requireTemplateConfigCategory(Long categoryId) {
        if (categoryId == null) {
            throw new ServiceException("类别ID不能为空");
        }
        ActivityCategory category = categoryMapper.selectById(categoryId);
        if (category == null || categoryHierarchyService.isGroup(category)) {
            throw new ServiceException("类别不存在或不是可配置附件的业务类别");
        }
    }

    private void validateFileRequirementTemplate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("模板文件不能为空");
        }
        if (file.getSize() > FILE_REQUIREMENT_TEMPLATE_MAX_SIZE) {
            throw new ServiceException("模板文件不能超过50MB");
        }
        String extension = normalizeFileExtension(file.getOriginalFilename());
        if (!FILE_REQUIREMENT_TEMPLATE_EXTENSIONS.contains(extension)) {
            throw new ServiceException("模板文件仅支持 xls、xlsx、doc、docx、pdf 格式");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(8);
            if (!matchesTemplateSignature(extension, header)) {
                throw new ServiceException("模板文件内容与扩展名不匹配");
            }
        } catch (IOException e) {
            throw new ServiceException("模板文件读取失败");
        }
    }

    private void bindFileRequirementTemplate(SysOssVo oss, Long categoryId) {
        if (oss == null || oss.getOssId() == null) {
            throw new ServiceException("模板文件保存失败");
        }
        SysOssExt ext = parseOssExt(oss.getExt1());
        ext.setBizType(FILE_REQUIREMENT_TEMPLATE_BIZ_TYPE);
        ext.setRefType(FILE_REQUIREMENT_TEMPLATE_REF_TYPE);
        ext.setRefId(String.valueOf(categoryId));
        SysOss update = new SysOss();
        update.setOssId(oss.getOssId());
        update.setExt1(JsonUtils.toJsonString(ext));
        if (ossMapper.updateById(update) < 1) {
            throw new ServiceException("模板文件绑定类别失败");
        }
    }

    private boolean isBoundFileRequirementTemplate(SysOssVo oss, Long categoryId) {
        if (oss == null) {
            return false;
        }
        SysOssExt ext = parseOssExt(oss.getExt1());
        return FILE_REQUIREMENT_TEMPLATE_BIZ_TYPE.equals(ext.getBizType())
            && FILE_REQUIREMENT_TEMPLATE_REF_TYPE.equals(ext.getRefType())
            && Objects.equals(String.valueOf(categoryId), ext.getRefId());
    }

    private SysOssExt parseOssExt(String extJson) {
        if (StringUtils.isBlank(extJson) || !JsonUtils.isJsonObject(extJson)) {
            return new SysOssExt();
        }
        try {
            SysOssExt ext = JsonUtils.parseObject(extJson, SysOssExt.class);
            return ext == null ? new SysOssExt() : ext;
        } catch (Exception ignored) {
            return new SysOssExt();
        }
    }

    private boolean matchesTemplateSignature(String extension, byte[] header) {
        return switch (extension) {
            case "pdf" -> startsWith(header, 0x25, 0x50, 0x44, 0x46);
            case "xls", "doc" -> startsWith(header, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            case "xlsx", "docx" -> startsWith(header, 0x50, 0x4B, 0x03, 0x04);
            default -> false;
        };
    }

    private boolean startsWith(byte[] value, int... expected) {
        if (value.length < expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if ((value[index] & 0xFF) != expected[index]) {
                return false;
            }
        }
        return true;
    }

    private String normalizeFileExtension(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }
        String value = filename.trim().toLowerCase(Locale.ROOT);
        int dot = value.lastIndexOf('.');
        return dot >= 0 && dot < value.length() - 1 ? value.substring(dot + 1) : "";
    }

    private Map<String, Object> fileRequirementRule(String ruleJson) {
        if (StringUtils.isBlank(ruleJson) || !JsonUtils.isJsonObject(ruleJson)) {
            return Map.of();
        }
        Map<String, Object> rule = JsonUtils.parseObject(ruleJson, Map.class);
        return rule == null ? Map.of() : rule;
    }

    private Long positiveLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            long id = Long.parseLong(String.valueOf(value).trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void requireAvailableActivity(Long activityId) {
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
    }

    private boolean isActivityAllowedForSchool(ActivityVo activity, Long schoolId) {
        String scopeType = StringUtils.blankToDefault(activity.getScopeType(), ArtReviewConstants.ACTIVITY_SCOPE_ALL);
        return ArtReviewConstants.ACTIVITY_SCOPE_ALL.equals(scopeType)
            || activityScopeService.isSchoolAllowed(activity.getId(), schoolId);
    }

    private void requireActivityScope(Long activityId, Long schoolId) {
        Activity activity = activityMapper.selectById(activityId);
        String scopeType = activity == null ? ArtReviewConstants.ACTIVITY_SCOPE_ALL : StringUtils.blankToDefault(activity.getScopeType(), ArtReviewConstants.ACTIVITY_SCOPE_ALL);
        if (ArtReviewConstants.ACTIVITY_SCOPE_ASSIGNED.equals(scopeType) && !activityScopeService.isSchoolAllowed(activityId, schoolId)) {
            throw new ServiceException("当前学校未获得该活动授权");
        }
    }

    private void normalizeAndValidateField(CategoryFieldSchema field) {
        if (field.getCategoryId() == null) {
            throw new ServiceException("类别ID不能为空");
        }
        if (StringUtils.isBlank(field.getFieldLabel())) {
            throw new ServiceException("字段名称不能为空");
        }
        if (StringUtils.isBlank(field.getFieldKey())) {
            field.setFieldKey("custom_" + System.currentTimeMillis());
        }
        field.setFieldKey(field.getFieldKey().trim());
        if (!FIELD_KEY_PATTERN.matcher(field.getFieldKey()).matches()) {
            throw new ServiceException("字段编码只能使用英文、数字、下划线，并且必须以英文开头");
        }
        Long duplicateCount = fieldMapper.selectCount(Wrappers.lambdaQuery(CategoryFieldSchema.class)
            .eq(CategoryFieldSchema::getCategoryId, field.getCategoryId())
            .eq(CategoryFieldSchema::getFieldKey, field.getFieldKey())
            .ne(field.getId() != null, CategoryFieldSchema::getId, field.getId()));
        if (duplicateCount != null && duplicateCount > 0) {
            throw new ServiceException("该类别下字段编码已存在");
        }
    }

    private ActivityConfigPackageVo parseExcelRows(List<ActivityConfigExcelRowVo> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new ServiceException("Excel 配置包为空");
        }
        ActivityConfigPackageVo configPackage = new ActivityConfigPackageVo();
        Map<String, ActivityConfigCategoryPackageVo> categoryMap = new LinkedHashMap<>();
        for (ActivityConfigExcelRowVo row : rows) {
            if (row == null || StringUtils.isBlank(row.getSection())) {
                continue;
            }
            String section = row.getSection().trim();
            String payloadJson = row.getPayloadJson();
            if (StringUtils.isBlank(payloadJson)) {
                throw new ServiceException("Excel 配置包存在空的配置JSON");
            }
            switch (section) {
                case "activity" -> {
                    if (configPackage.getActivity() != null) {
                throw new ServiceException("Excel 配置包只能包含一行活动基础信息");
                    }
                    configPackage.setActivity(JsonUtils.parseObject(payloadJson, Activity.class));
                }
                case "category" -> {
                    ActivityCategory category = JsonUtils.parseObject(payloadJson, ActivityCategory.class);
                    String categoryCode = StringUtils.blankToDefault(category.getCategoryCode(), row.getCategoryCode());
                    category.setCategoryCode(categoryCode);
                    if (StringUtils.isBlank(categoryCode)) {
                        throw new ServiceException("Excel 类别行缺少类别编码");
                    }
                    ActivityConfigCategoryPackageVo categoryPackage = new ActivityConfigCategoryPackageVo();
                    categoryPackage.setCategory(category);
                    categoryMap.put(categoryCode, categoryPackage);
                    configPackage.getCategories().add(categoryPackage);
                }
                case "field" -> {
                    ActivityConfigCategoryPackageVo categoryPackage = requireExcelCategory(categoryMap, row.getCategoryCode());
                    categoryPackage.getFields().add(JsonUtils.parseObject(payloadJson, CategoryFieldSchema.class));
                }
                case "fileRequirement" -> {
                    ActivityConfigCategoryPackageVo categoryPackage = requireExcelCategory(categoryMap, row.getCategoryCode());
                    categoryPackage.getFileRequirements().add(JsonUtils.parseObject(payloadJson, CategoryFileRequirement.class));
                }
                default -> throw new ServiceException("Excel 配置类型不支持: " + section);
            }
        }
        return configPackage;
    }

    private ActivityConfigCategoryPackageVo requireExcelCategory(Map<String, ActivityConfigCategoryPackageVo> categoryMap, String categoryCode) {
        if (StringUtils.isBlank(categoryCode) || !categoryMap.containsKey(categoryCode)) {
            throw new ServiceException("Excel 字段/附件要求必须先提供对应类别行: " + StringUtils.blankToDefault(categoryCode, "空类别编码"));
        }
        return categoryMap.get(categoryCode);
    }

    private void validateActivityConfigPackage(ActivityConfigPackageVo configPackage) {
        if (configPackage == null || configPackage.getActivity() == null) {
            throw new ServiceException("配置包缺少活动基础信息");
        }
        if (!"1.0".equals(configPackage.getFormatVersion())) {
            throw new ServiceException("不支持的活动配置包版本：" + StringUtils.blankToDefault(
                configPackage.getFormatVersion(), "空版本"));
        }
        Activity activity = configPackage.getActivity();
        if (StringUtils.isBlank(activity.getActivityName())) {
            throw new ServiceException("活动名称不能为空");
        }
        if (StringUtils.isBlank(activity.getStatus())) {
            activity.setStatus("draft");
        }
        if (configPackage.getCategories() == null) {
            configPackage.setCategories(new ArrayList<>());
        }
        Set<String> categoryCodes = new HashSet<>();
        for (ActivityConfigCategoryPackageVo categoryPackage : configPackage.getCategories()) {
            if (categoryPackage == null || categoryPackage.getCategory() == null) {
                throw new ServiceException("配置包存在空类别");
            }
            ActivityCategory category = categoryPackage.getCategory();
            if (StringUtils.isBlank(category.getCategoryCode()) || !category.getCategoryCode().matches("^[a-z][a-z0-9_]{1,63}$")) {
                throw new ServiceException("类别编码不合法: " + StringUtils.blankToDefault(category.getCategoryCode(), "空"));
            }
            if (!categoryCodes.add(category.getCategoryCode())) {
                throw new ServiceException("类别编码重复: " + category.getCategoryCode());
            }
            if (StringUtils.isBlank(category.getCategoryName())) {
                throw new ServiceException("类别名称不能为空: " + category.getCategoryCode());
            }
            if (category.getEnabled() == null) {
                category.setEnabled(true);
            }
            if (category.getSortOrder() == null) {
                category.setSortOrder(0);
            }
            validateJsonObject(category.getRuleJson(), "类别规则JSON");
            validateFields(categoryPackage);
            validateFileRequirements(categoryPackage);
        }
    }

    private void validateFields(ActivityConfigCategoryPackageVo categoryPackage) {
        Set<String> fieldKeys = new HashSet<>();
        for (CategoryFieldSchema field : safeList(categoryPackage.getFields())) {
            if (field == null) {
                throw new ServiceException("配置包存在空字段");
            }
            if (StringUtils.isBlank(field.getFieldLabel())) {
                throw new ServiceException("字段名称不能为空");
            }
            if (StringUtils.isBlank(field.getFieldKey()) || !FIELD_KEY_PATTERN.matcher(field.getFieldKey()).matches()) {
                throw new ServiceException("字段编码不合法: " + StringUtils.blankToDefault(field.getFieldKey(), "空"));
            }
            if (!fieldKeys.add(field.getFieldKey())) {
                throw new ServiceException("同一类别字段编码重复: " + field.getFieldKey());
            }
            if (StringUtils.isBlank(field.getFieldType())) {
                field.setFieldType("input");
            }
            if (field.getRequired() == null) {
                field.setRequired(false);
            }
            if (field.getSensitiveFlag() == null) {
                field.setSensitiveFlag(false);
            }
            if (field.getSortOrder() == null) {
                field.setSortOrder(0);
            }
            validateJsonArray(field.getOptionsJson(), "字段选项JSON");
            validateJsonObject(field.getValidationJson(), "字段校验JSON");
        }
    }

    private void validateFileRequirements(ActivityConfigCategoryPackageVo categoryPackage) {
        for (CategoryFileRequirement requirement : safeList(categoryPackage.getFileRequirements())) {
            if (requirement == null) {
                throw new ServiceException("配置包存在空附件要求");
            }
            validateJsonObject(requirement.getRuleJson(), "附件规则JSON");
            if (requirement.getMinCount() != null && requirement.getMinCount() < 0) {
                throw new ServiceException("附件最少数量不能小于 0");
            }
            if (requirement.getMaxCount() != null && requirement.getMaxCount() < 0) {
                throw new ServiceException("附件最多数量不能小于 0");
            }
        }
    }

    private void validateJsonObject(String json, String label) {
        if (StringUtils.isNotBlank(json) && !JsonUtils.isJsonObject(json)) {
            throw new ServiceException(label + "必须是合法 JSON 对象");
        }
    }

    private void validateJsonArray(String json, String label) {
        if (StringUtils.isNotBlank(json) && !JsonUtils.isJsonArray(json)) {
            throw new ServiceException(label + "必须是合法 JSON 数组");
        }
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list.stream().filter(Objects::nonNull).toList();
    }

    private void clearActivityForPackage(Activity activity) {
        activity.setId(null);
        activity.setDelFlag(null);
        clearTenantEntity(activity);
    }

    private void clearCategoryForPackage(ActivityCategory category) {
        category.setId(null);
        category.setActivityId(null);
        category.setParentId(0L);
        category.setDelFlag(null);
        clearTenantEntity(category);
    }

    private void clearFieldForPackage(CategoryFieldSchema field) {
        field.setId(null);
        field.setCategoryId(null);
        field.setDelFlag(null);
        clearTenantEntity(field);
    }

    private void clearFileRequirementForPackage(CategoryFileRequirement requirement) {
        requirement.setId(null);
        requirement.setCategoryId(null);
        requirement.setDelFlag(null);
        clearTenantEntity(requirement);
    }

    private void clearActivityForImport(Activity activity) {
        clearActivityForPackage(activity);
    }

    private void clearCategoryForImport(ActivityCategory category, Long activityId) {
        clearCategoryForPackage(category);
        category.setActivityId(activityId);
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
    }

    private void clearFieldForImport(CategoryFieldSchema field, Long categoryId) {
        clearFieldForPackage(field);
        field.setCategoryId(categoryId);
    }

    private void clearFileRequirementForImport(CategoryFileRequirement requirement, Long categoryId) {
        clearFileRequirementForPackage(requirement);
        requirement.setCategoryId(categoryId);
    }

    private void clearTenantEntity(TenantEntity entity) {
        entity.setTenantId(null);
        entity.setCreateDept(null);
        entity.setCreateBy(null);
        entity.setCreateTime(null);
        entity.setUpdateBy(null);
        entity.setUpdateTime(null);
        entity.setParams(null);
    }

    private void normalizeFileRequirement(CategoryFileRequirement requirement) {
        if (requirement.getCategoryId() == null) {
            throw new ServiceException("类别ID不能为空");
        }
        if (StringUtils.isBlank(requirement.getFileTypeCode())) {
            requirement.setFileTypeCode("all");
        }
        requirement.setFileTypeCode(requirement.getFileTypeCode().trim().toLowerCase(Locale.ROOT));
        if (StringUtils.isBlank(requirement.getFileTypeName())) {
            requirement.setFileTypeName(defaultFileTypeName(requirement.getFileTypeCode()));
        }
        if (StringUtils.isNotBlank(requirement.getAllowedExt())) {
            String allowedExt = requirement.getAllowedExt().trim().toLowerCase(Locale.ROOT);
            if ("*".equals(allowedExt) || "all".equals(allowedExt)) {
                requirement.setAllowedExt(null);
            } else {
                requirement.setAllowedExt(Arrays.stream(allowedExt.split("[,，、;；\\s]+"))
                    .map(String::trim)
                    .map(ext -> ext.startsWith(".") ? ext.substring(1) : ext)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .reduce((left, right) -> left + "," + right)
                .orElse(null));
            }
        }
        if (requirement.getMaxSizeMb() != null && requirement.getMaxSizeMb() <= 0) {
            requirement.setMaxSizeMb(null);
        }
        Map<String, Object> rule = new LinkedHashMap<>(fileRequirementRule(requirement.getRuleJson()));
        Double minMb = positiveNumber(rule.get("minMb"));
        if (minMb == null) {
            rule.remove("minMb");
        } else {
            rule.put("minMb", minMb);
        }
        Double ruleMaxMb = positiveNumber(rule.get("maxMb"));
        Double maxMb = requirement.getMaxSizeMb() == null ? ruleMaxMb : requirement.getMaxSizeMb().doubleValue();
        if (minMb != null && maxMb != null && minMb > maxMb) {
            throw new ServiceException("附件最大文件大小不能小于最小文件大小");
        }
        requirement.setRuleJson(rule.isEmpty() ? null : JsonUtils.toJsonString(rule));
        if (requirement.getMinCount() == null || requirement.getMinCount() < 0) {
            requirement.setMinCount(0);
        }
        if (requirement.getMaxCount() != null && requirement.getMaxCount() <= 0) {
            requirement.setMaxCount(null);
        }
        if (requirement.getMaxCount() != null && requirement.getMaxCount() < requirement.getMinCount()) {
            throw new ServiceException("附件最多数量不能小于最少数量");
        }
        if (requirement.getRequired() == null) {
            requirement.setRequired(false);
        }
        if (requirement.getSortOrder() == null) {
            requirement.setSortOrder(0);
        }
    }

    private String defaultFileTypeName(String fileTypeCode) {
        return switch (fileTypeCode) {
            case "document" -> "文档";
            case "image" -> "图片";
            case "video" -> "视频";
            case "audio" -> "音频";
            case "archive" -> "压缩包";
            default -> "所有类型";
        };
    }

    private Double positiveNumber(Object value) {
        if (value == null) {
            return null;
        }
        try {
            double number = Double.parseDouble(String.valueOf(value).trim());
            return Double.isFinite(number) && number > 0 ? number : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
