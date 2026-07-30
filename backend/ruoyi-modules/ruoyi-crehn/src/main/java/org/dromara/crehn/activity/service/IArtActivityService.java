package org.dromara.crehn.activity.service;

import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.CategoryFieldSchema;
import org.dromara.crehn.domain.CategoryFileRequirement;
import org.dromara.crehn.domain.vo.ActivityCategoryVo;
import org.dromara.crehn.domain.vo.ActivityCategoryPurgeCheckVo;
import org.dromara.crehn.domain.vo.ActivityConfigExcelRowVo;
import org.dromara.crehn.domain.vo.ActivityConfigImportResultVo;
import org.dromara.crehn.domain.vo.ActivityConfigMergePreviewVo;
import org.dromara.crehn.domain.vo.ActivityConfigPackageVo;
import org.dromara.crehn.domain.vo.ActivityDeleteCheckVo;
import org.dromara.crehn.domain.vo.ActivityVo;
import org.dromara.crehn.domain.vo.CategoryFieldSchemaVo;
import org.dromara.crehn.domain.vo.CategoryFileRequirementVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.vo.SysOssUploadVo;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface IArtActivityService {
    TableDataInfo<ActivityVo> queryActivityPage(Activity activity, PageQuery pageQuery);

    List<ActivityVo> listManagementActivityOptions();

    List<ActivityCategoryVo> listManagementCategoryOptions(Long activityId);

    ActivityVo getActivity(Long id);

    int addActivity(Activity activity);

    int updateActivity(Activity activity);

    int deleteActivities(Long[] ids);

    int restoreActivities(Long[] ids);

    ActivityDeleteCheckVo checkActivityDelete(Long id);

    ActivityDeleteCheckVo checkActivityPurge(Long id);

    int purgeActivity(Long id);

    ActivityConfigPackageVo exportActivityConfigPackage(Long activityId);

    List<ActivityConfigExcelRowVo> exportActivityConfigExcelRows(Long activityId);

    ActivityConfigImportResultVo importActivityConfigPackage(ActivityConfigPackageVo configPackage);

    ActivityConfigImportResultVo importActivityConfigExcelRows(List<ActivityConfigExcelRowVo> rows);

    ActivityConfigMergePreviewVo previewActivityConfigMerge(Long activityId, ActivityConfigPackageVo configPackage);

    ActivityConfigMergePreviewVo previewActivityConfigExcelMerge(Long activityId, List<ActivityConfigExcelRowVo> rows);

    ActivityConfigMergePreviewVo mergeActivityConfig(Long activityId, ActivityConfigPackageVo configPackage);

    ActivityConfigMergePreviewVo mergeActivityConfigExcelRows(Long activityId, List<ActivityConfigExcelRowVo> rows);

    List<ActivityCategoryVo> listCategories(Long activityId, Boolean deleted);

    ActivityCategoryVo getCategory(Long id);

    int addCategory(ActivityCategory category);

    ActivityCategoryVo copyCategory(Long id);

    int updateCategory(ActivityCategory category);

    int deleteCategories(Long[] ids);

    int restoreCategories(Long[] ids);

    ActivityCategoryPurgeCheckVo checkCategoryDelete(Long id);

    ActivityCategoryPurgeCheckVo checkCategoryPurge(Long id);

    int purgeCategory(Long id);

    List<CategoryFieldSchemaVo> listFields(Long categoryId, Boolean deleted);

    int addField(CategoryFieldSchema field);

    int updateField(CategoryFieldSchema field);

    int deleteFields(Long[] ids);

    int restoreFields(Long[] ids);

    long countFieldData(Long id);

    List<CategoryFileRequirementVo> listFileRequirements(Long categoryId, Boolean deleted);

    int addFileRequirement(CategoryFileRequirement requirement);

    int updateFileRequirement(CategoryFileRequirement requirement);

    int deleteFileRequirements(Long[] ids);

    int restoreFileRequirements(Long[] ids);

    long countFileRequirementData(Long id);

    SysOssUploadVo uploadCategoryFileRequirementTemplate(Long categoryId, MultipartFile file);

    void downloadCategoryFileRequirementTemplate(Long categoryId, Long requirementId, HttpServletResponse response) throws IOException;

    List<ActivityVo> listSchoolAvailableActivities();

    List<ActivityCategoryVo> listSchoolAvailableCategories(Long activityId);

    List<ActivityCategoryVo> listSchoolCategoryCatalog(Long activityId);

    List<CategoryFieldSchemaVo> listSchoolCategoryFields(Long categoryId);

    List<CategoryFileRequirementVo> listSchoolCategoryFileRequirements(Long categoryId);
}
