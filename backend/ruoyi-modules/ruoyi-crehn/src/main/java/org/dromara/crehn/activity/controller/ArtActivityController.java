package org.dromara.crehn.activity.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.activity.service.IArtActivityService;
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
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.vo.SysOssUploadVo;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/activity")
public class ArtActivityController extends BaseController {

    private final IArtActivityService activityService;

    @SaCheckPermission("crehn:activity:list")
    @GetMapping("/list")
    public TableDataInfo<ActivityVo> list(Activity activity, PageQuery pageQuery) {
        return activityService.queryActivityPage(activity, pageQuery);
    }

    /**
     * 各业务管理页面共用的活动下拉选项，不授予活动配置管理权限。
     */
    @SaCheckPermission(value = {
        "crehn:activity:list", "crehn:auditScope:list", "crehn:activityScope:list",
        "crehn:uploadTemplate:list", "crehn:registration:list", "crehn:reviewAssignment:list",
        "crehn:reportRule:list", "crehn:result:list", "crehn:participant:list",
        "crehn:participant:import", "crehn:recycle:list", "system:workbench:edit"
    }, mode = SaMode.OR)
    @GetMapping("/options")
    public R<List<ActivityVo>> activityOptions() {
        return R.ok(activityService.listManagementActivityOptions());
    }

    /**
     * 各业务管理页面共用的类别下拉选项，不授予类别配置管理权限。
     */
    @SaCheckPermission(value = {
        "crehn:category:list", "crehn:auditScope:list", "crehn:uploadTemplate:list",
        "crehn:reviewAssignment:list", "crehn:reportRule:list", "crehn:result:list"
    }, mode = SaMode.OR)
    @GetMapping("/category/options/{activityId}")
    public R<List<ActivityCategoryVo>> categoryOptions(@PathVariable Long activityId) {
        return R.ok(activityService.listManagementCategoryOptions(activityId));
    }

    @SaCheckPermission("crehn:activity:query")
    @GetMapping("/{id}")
    public R<ActivityVo> getInfo(@PathVariable Long id) {
        return R.ok(activityService.getActivity(id));
    }

    @SaCheckPermission("crehn:activity:add")
    @SaCheckRole(value = {"crehn_admin", "superadmin"}, mode = SaMode.OR)
    @Log(title = "art activity", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody Activity activity) {
        return toAjax(activityService.addActivity(activity));
    }

    @SaCheckPermission("crehn:activity:edit")
    @SaCheckRole(value = {"crehn_admin", "superadmin"}, mode = SaMode.OR)
    @Log(title = "art activity", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody Activity activity) {
        return toAjax(activityService.updateActivity(activity));
    }

    @SaCheckPermission("crehn:activity:remove")
    @SaCheckRole(value = {"crehn_admin", "superadmin"}, mode = SaMode.OR)
    @Log(title = "art activity", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(activityService.deleteActivities(ids));
    }

    @SaCheckPermission("crehn:activity:edit")
    @SaCheckRole(value = {"crehn_admin", "superadmin"}, mode = SaMode.OR)
    @Log(title = "art activity restore", businessType = BusinessType.UPDATE)
    @PutMapping("/restore/{ids}")
    public R<Void> restore(@PathVariable Long[] ids) {
        return toAjax(activityService.restoreActivities(ids));
    }

    @SaCheckPermission("crehn:activity:remove")
    @SaCheckRole(value = {"crehn_admin", "superadmin"}, mode = SaMode.OR)
    @GetMapping("/{id}/delete-check")
    public R<ActivityDeleteCheckVo> checkActivityDelete(@PathVariable Long id) {
        return R.ok(activityService.checkActivityDelete(id));
    }

    @SaCheckPermission("crehn:activity:remove")
    @SaCheckRole(value = {"crehn_admin", "superadmin"}, mode = SaMode.OR)
    @GetMapping("/{id}/purge-check")
    public R<ActivityDeleteCheckVo> checkActivityPurge(@PathVariable Long id) {
        return R.ok(activityService.checkActivityPurge(id));
    }

    @SaCheckPermission("crehn:activity:remove")
    @SaCheckRole(value = {"crehn_admin", "superadmin"}, mode = SaMode.OR)
    @Log(title = "art activity permanent delete", businessType = BusinessType.CLEAN)
    @DeleteMapping("/{id}/purge")
    public R<Void> purgeActivity(@PathVariable Long id) {
        return toAjax(activityService.purgeActivity(id));
    }

    @SaCheckPermission("crehn:activity:query")
    @Log(title = "art activity config package export json", businessType = BusinessType.EXPORT)
    @GetMapping("/{id}/config-package/export-json")
    public void exportConfigJson(@PathVariable Long id, HttpServletResponse response) throws Exception {
        ActivityConfigPackageVo configPackage = activityService.exportActivityConfigPackage(id);
        String activityName = configPackage.getActivity() == null ? "活动配置包" : configPackage.getActivity().getActivityName();
        FileUtils.setAttachmentResponseHeader(response, activityName + "-活动配置包.json");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getOutputStream().write(JsonUtils.toJsonString(configPackage).getBytes(StandardCharsets.UTF_8));
    }

    @SaCheckPermission("crehn:activity:query")
    @Log(title = "art activity config package export excel", businessType = BusinessType.EXPORT)
    @GetMapping("/{id}/config-package/export-excel")
    public void exportConfigExcel(@PathVariable Long id, HttpServletResponse response) {
        List<ActivityConfigExcelRowVo> rows = activityService.exportActivityConfigExcelRows(id);
        ExcelUtil.exportExcel(rows, "活动配置包", ActivityConfigExcelRowVo.class, response);
    }

    @SaCheckPermission("crehn:activity:add")
    @SaCheckRole(value = {"crehn_admin", "superadmin"}, mode = SaMode.OR)
    @Log(title = "art activity config package import", businessType = BusinessType.IMPORT)
    @PostMapping(value = "/config-package/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ActivityConfigImportResultVo> importConfigPackage(@RequestPart("file") MultipartFile file) throws Exception {
        try {
            String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
            ActivityConfigImportResultVo result;
            if (filename.endsWith(".json")) {
                ActivityConfigPackageVo configPackage = JsonUtils.parseObject(file.getBytes(), ActivityConfigPackageVo.class);
                result = activityService.importActivityConfigPackage(configPackage);
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                List<ActivityConfigExcelRowVo> rows = ExcelUtil.importExcel(file.getInputStream(), ActivityConfigExcelRowVo.class);
                result = activityService.importActivityConfigExcelRows(rows);
            } else {
                throw new ServiceException("仅支持 JSON、Excel(.xlsx/.xls) 活动配置包");
            }
            return R.ok(result);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("活动配置包导入失败：" + rootCauseMessage(e));
        }
    }

    @SaCheckPermission(value = {"crehn:category:add", "crehn:field:add", "crehn:fileRequirement:add"}, mode = SaMode.AND)
    @PostMapping(value = "/{id}/config-package/merge-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ActivityConfigMergePreviewVo> previewConfigMerge(@PathVariable Long id,
                                                               @RequestPart("file") MultipartFile file) throws Exception {
        try {
            String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
            ActivityConfigMergePreviewVo result;
            if (filename.endsWith(".json")) {
                ActivityConfigPackageVo configPackage = JsonUtils.parseObject(file.getBytes(), ActivityConfigPackageVo.class);
                result = activityService.previewActivityConfigMerge(id, configPackage);
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                List<ActivityConfigExcelRowVo> rows = ExcelUtil.importExcel(file.getInputStream(), ActivityConfigExcelRowVo.class);
                result = activityService.previewActivityConfigExcelMerge(id, rows);
            } else {
                throw new ServiceException("仅支持 JSON、Excel(.xlsx/.xls) 活动配置包");
            }
            return R.ok(result);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("活动配置包预览失败：" + rootCauseMessage(e));
        }
    }

    @SaCheckPermission(value = {"crehn:category:add", "crehn:field:add", "crehn:fileRequirement:add"}, mode = SaMode.AND)
    @Log(title = "art activity config package merge import", businessType = BusinessType.IMPORT)
    @PostMapping(value = "/{id}/config-package/merge-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ActivityConfigMergePreviewVo> mergeConfigPackage(@PathVariable Long id,
                                                               @RequestPart("file") MultipartFile file) throws Exception {
        try {
            String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
            ActivityConfigMergePreviewVo result;
            if (filename.endsWith(".json")) {
                ActivityConfigPackageVo configPackage = JsonUtils.parseObject(file.getBytes(), ActivityConfigPackageVo.class);
                result = activityService.mergeActivityConfig(id, configPackage);
            } else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                List<ActivityConfigExcelRowVo> rows = ExcelUtil.importExcel(file.getInputStream(), ActivityConfigExcelRowVo.class);
                result = activityService.mergeActivityConfigExcelRows(id, rows);
            } else {
                throw new ServiceException("仅支持 JSON、Excel(.xlsx/.xls) 活动配置包");
            }
            return R.ok(result);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("活动配置包增量导入失败：" + rootCauseMessage(e));
        }
    }

    private String rootCauseMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            message = error.getMessage();
        }
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    @SaCheckPermission(value = {"crehn:category:list", "system:workbench:edit"}, mode = SaMode.OR)
    @GetMapping("/category/list/{activityId}")
    public R<List<ActivityCategoryVo>> categoryList(@PathVariable Long activityId, @RequestParam(required = false) Boolean deleted) {
        return R.ok(activityService.listCategories(activityId, deleted));
    }

    @SaCheckPermission("crehn:category:query")
    @GetMapping("/category/{id}")
    public R<ActivityCategoryVo> getCategory(@PathVariable Long id) {
        return R.ok(activityService.getCategory(id));
    }

    @SaCheckPermission("crehn:category:add")
    @Log(title = "art category", businessType = BusinessType.INSERT)
    @PostMapping("/category")
    public R<Void> addCategory(@RequestBody ActivityCategory category) {
        return toAjax(activityService.addCategory(category));
    }

    @SaCheckPermission("crehn:category:add")
    @Log(title = "art category copy", businessType = BusinessType.INSERT)
    @PostMapping("/category/{id}/copy")
    public R<ActivityCategoryVo> copyCategory(@PathVariable Long id) {
        return R.ok(activityService.copyCategory(id));
    }

    @SaCheckPermission("crehn:category:edit")
    @Log(title = "art category", businessType = BusinessType.UPDATE)
    @PutMapping("/category")
    public R<Void> editCategory(@RequestBody ActivityCategory category) {
        return toAjax(activityService.updateCategory(category));
    }

    @SaCheckPermission("crehn:category:remove")
    @Log(title = "art category", businessType = BusinessType.DELETE)
    @DeleteMapping("/category/{ids}")
    public R<Void> removeCategory(@PathVariable Long[] ids) {
        return toAjax(activityService.deleteCategories(ids));
    }

    @SaCheckPermission("crehn:category:edit")
    @Log(title = "art category restore", businessType = BusinessType.UPDATE)
    @PutMapping("/category/restore/{ids}")
    public R<Void> restoreCategory(@PathVariable Long[] ids) {
        return toAjax(activityService.restoreCategories(ids));
    }

    @SaCheckPermission("crehn:category:remove")
    @GetMapping("/category/{id}/delete-check")
    public R<ActivityCategoryPurgeCheckVo> checkCategoryDelete(@PathVariable Long id) {
        return R.ok(activityService.checkCategoryDelete(id));
    }

    @SaCheckPermission("crehn:category:remove")
    @GetMapping("/category/{id}/purge-check")
    public R<ActivityCategoryPurgeCheckVo> checkCategoryPurge(@PathVariable Long id) {
        return R.ok(activityService.checkCategoryPurge(id));
    }

    @SaCheckPermission("crehn:category:remove")
    @Log(title = "art category permanent delete", businessType = BusinessType.CLEAN)
    @DeleteMapping("/category/{id}/purge")
    public R<Void> purgeCategory(@PathVariable Long id) {
        return toAjax(activityService.purgeCategory(id));
    }

    @SaCheckPermission("crehn:field:list")
    @GetMapping("/field/list/{categoryId}")
    public R<List<CategoryFieldSchemaVo>> fieldList(@PathVariable Long categoryId, @RequestParam(required = false) Boolean deleted) {
        return R.ok(activityService.listFields(categoryId, deleted));
    }

    @SaCheckPermission("crehn:field:add")
    @Log(title = "art field schema", businessType = BusinessType.INSERT)
    @PostMapping("/field")
    public R<Void> addField(@RequestBody CategoryFieldSchema field) {
        return toAjax(activityService.addField(field));
    }

    @SaCheckPermission("crehn:field:edit")
    @Log(title = "art field schema", businessType = BusinessType.UPDATE)
    @PutMapping("/field")
    public R<Void> editField(@RequestBody CategoryFieldSchema field) {
        return toAjax(activityService.updateField(field));
    }

    @SaCheckPermission("crehn:field:remove")
    @Log(title = "art field schema", businessType = BusinessType.DELETE)
    @DeleteMapping("/field/{ids}")
    public R<Void> removeField(@PathVariable Long[] ids) {
        return toAjax(activityService.deleteFields(ids));
    }

    @SaCheckPermission("crehn:field:edit")
    @Log(title = "art field schema restore", businessType = BusinessType.UPDATE)
    @PutMapping("/field/restore/{ids}")
    public R<Void> restoreField(@PathVariable Long[] ids) {
        return toAjax(activityService.restoreFields(ids));
    }

    @SaCheckPermission("crehn:field:list")
    @GetMapping("/field/{id}/usage-count")
    public R<Long> fieldUsageCount(@PathVariable Long id) {
        return R.ok(activityService.countFieldData(id));
    }

    @SaCheckPermission("crehn:fileRequirement:list")
    @GetMapping("/file-requirement/list/{categoryId}")
    public R<List<CategoryFileRequirementVo>> fileRequirementList(@PathVariable Long categoryId, @RequestParam(required = false) Boolean deleted) {
        return R.ok(activityService.listFileRequirements(categoryId, deleted));
    }

    @SaCheckPermission("crehn:fileRequirement:add")
    @Log(title = "art file requirement", businessType = BusinessType.INSERT)
    @PostMapping("/file-requirement")
    public R<Void> addFileRequirement(@RequestBody CategoryFileRequirement requirement) {
        return toAjax(activityService.addFileRequirement(requirement));
    }

    @SaCheckPermission("crehn:fileRequirement:edit")
    @Log(title = "art file requirement", businessType = BusinessType.UPDATE)
    @PutMapping("/file-requirement")
    public R<Void> editFileRequirement(@RequestBody CategoryFileRequirement requirement) {
        return toAjax(activityService.updateFileRequirement(requirement));
    }

    @SaCheckPermission("crehn:fileRequirement:remove")
    @Log(title = "art file requirement", businessType = BusinessType.DELETE)
    @DeleteMapping("/file-requirement/{ids}")
    public R<Void> removeFileRequirement(@PathVariable Long[] ids) {
        return toAjax(activityService.deleteFileRequirements(ids));
    }

    @SaCheckPermission("crehn:fileRequirement:edit")
    @Log(title = "art file requirement restore", businessType = BusinessType.UPDATE)
    @PutMapping("/file-requirement/restore/{ids}")
    public R<Void> restoreFileRequirement(@PathVariable Long[] ids) {
        return toAjax(activityService.restoreFileRequirements(ids));
    }

    @SaCheckPermission("crehn:fileRequirement:list")
    @GetMapping("/file-requirement/{id}/usage-count")
    public R<Long> fileRequirementUsageCount(@PathVariable Long id) {
        return R.ok(activityService.countFileRequirementData(id));
    }

    @SaCheckPermission("crehn:fileRequirement:edit")
    @Log(title = "art file requirement template", businessType = BusinessType.INSERT)
    @PostMapping(value = "/file-requirement/template/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<SysOssUploadVo> uploadFileRequirementTemplate(@RequestParam Long categoryId, @RequestPart("file") MultipartFile file) {
        return R.ok(activityService.uploadCategoryFileRequirementTemplate(categoryId, file));
    }

    @SaCheckPermission("crehn:project:add")
    @GetMapping("/school/available")
    public R<List<ActivityVo>> schoolAvailableActivities() {
        return R.ok(activityService.listSchoolAvailableActivities());
    }

    @SaCheckPermission("crehn:project:add")
    @GetMapping("/school/{activityId}/category")
    public R<List<ActivityCategoryVo>> schoolAvailableCategories(@PathVariable Long activityId) {
        return R.ok(activityService.listSchoolAvailableCategories(activityId));
    }

    @SaCheckPermission("crehn:project:add")
    @GetMapping("/school/{activityId}/category/catalog")
    public R<List<ActivityCategoryVo>> schoolCategoryCatalog(@PathVariable Long activityId) {
        return R.ok(activityService.listSchoolCategoryCatalog(activityId));
    }

    @SaCheckPermission("crehn:project:add")
    @GetMapping("/school/category/{categoryId}/field")
    public R<List<CategoryFieldSchemaVo>> schoolCategoryFields(@PathVariable Long categoryId) {
        return R.ok(activityService.listSchoolCategoryFields(categoryId));
    }

    @SaCheckPermission("crehn:project:add")
    @GetMapping("/school/category/{categoryId}/file-requirement")
    public R<List<CategoryFileRequirementVo>> schoolCategoryFileRequirements(@PathVariable Long categoryId) {
        return R.ok(activityService.listSchoolCategoryFileRequirements(categoryId));
    }

    @SaCheckPermission("crehn:project:add")
    @Log(title = "art file requirement template", businessType = BusinessType.EXPORT)
    @GetMapping("/school/category/{categoryId}/file-requirement/{requirementId}/template/download")
    public void downloadSchoolCategoryFileRequirementTemplate(
        @PathVariable Long categoryId, @PathVariable Long requirementId, HttpServletResponse response) throws IOException {
        activityService.downloadCategoryFileRequirementTemplate(categoryId, requirementId, response);
    }
}
