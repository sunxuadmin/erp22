package org.dromara.system.controller.system;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.bo.SysWorkbenchLayoutBo;
import org.dromara.system.domain.vo.SysWorkbenchAssetUploadVo;
import org.dromara.system.domain.vo.SysWorkbenchComponentVo;
import org.dromara.system.domain.vo.SysWorkbenchConfigImportPreviewVo;
import org.dromara.system.domain.vo.SysWorkbenchLayoutVo;
import org.dromara.system.service.ISysRoleService;
import org.dromara.system.service.ISysWorkbenchLayoutService;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/workbench")
public class SysWorkbenchLayoutController extends BaseController {

    private final ISysWorkbenchLayoutService workbenchLayoutService;
    private final ISysRoleService roleService;

    @SaCheckPermission("system:workbench:query")
    @GetMapping("/components")
    public R<List<SysWorkbenchComponentVo>> components(@RequestParam(required = false) Long roleId) {
        return R.ok(workbenchLayoutService.componentOptions(roleId));
    }

    @SaCheckPermission("system:workbench:query")
    @GetMapping("/role-options")
    public R<List<WorkbenchRoleOptionVo>> roleOptions() {
        List<WorkbenchRoleOptionVo> options = roleService.selectRoleByIds(null).stream()
            .map(role -> new WorkbenchRoleOptionVo(role.getRoleId(), role.getRoleName(), role.getRoleKey()))
            .toList();
        return R.ok(options);
    }

    @SaCheckLogin
    @GetMapping("/current")
    public R<List<SysWorkbenchLayoutVo>> current() {
        return R.ok(workbenchLayoutService.queryCurrentLayout());
    }

    @SaCheckLogin
    @GetMapping("/current/component/{componentKey}")
    public R<SysWorkbenchLayoutVo> currentComponent(@PathVariable String componentKey) {
        return R.ok(workbenchLayoutService.queryCurrentComponent(componentKey));
    }

    @SaCheckPermission("system:workbench:query")
    @GetMapping("/role/{roleId}")
    public R<List<SysWorkbenchLayoutVo>> roleLayout(@PathVariable Long roleId) {
        return R.ok(workbenchLayoutService.queryRoleLayout(roleId));
    }

    @SaCheckLogin
    @GetMapping("/role-shell/current")
    public R<Map<String, Object>> currentRoleShellConfig() {
        return R.ok(workbenchLayoutService.queryCurrentRoleShellConfig());
    }

    @SaCheckPermission("system:workbench:query")
    @GetMapping("/role/{roleId}/shell")
    public R<Map<String, Object>> roleShellConfig(@PathVariable Long roleId) {
        return R.ok(workbenchLayoutService.queryRoleShellConfig(roleId));
    }

    @SaCheckPermission("system:workbench:edit")
    @Log(title = "workbench role shell", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/role/{roleId}/shell")
    public R<Void> saveRoleShellConfig(@PathVariable Long roleId, @RequestBody Map<String, Object> config) {
        return toAjax(workbenchLayoutService.saveRoleShellConfig(roleId, config));
    }

    @SaCheckPermission("system:workbench:edit")
    @Log(title = "workbench role shell", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/role/{roleId}/shell/restore-default")
    public R<Void> restoreRoleShellConfig(@PathVariable Long roleId) {
        return toAjax(workbenchLayoutService.restoreRoleShellConfig(roleId));
    }

    @SaCheckLogin
    @GetMapping("/navbar-title")
    public R<String> navbarTitleConfig() {
        return R.ok(workbenchLayoutService.queryNavbarTitleConfig());
    }

    @SaCheckPermission("system:workbench:edit")
    @Log(title = "workbench navbar title", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/navbar-title")
    public R<Void> saveNavbarTitleConfig(@RequestBody Map<String, Object> config) {
        return toAjax(workbenchLayoutService.saveNavbarTitleConfig(JsonUtils.toJsonString(config)));
    }

    @SaCheckLogin
    @GetMapping("/style")
    public R<String> styleConfig() {
        return R.ok(workbenchLayoutService.queryStyleConfig());
    }

    @SaCheckPermission("system:workbench:edit")
    @Log(title = "workbench style", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/style")
    public R<Map<String, Object>> saveStyleConfig(@RequestBody Map<String, Object> config) {
        return R.ok(workbenchLayoutService.saveStyleConfig(JsonUtils.toJsonString(config)));
    }

    @SaCheckLogin
    @GetMapping("/security-reminder")
    public R<String> securityReminderConfig() {
        return R.ok(workbenchLayoutService.querySecurityReminderConfig());
    }

    @SaCheckPermission("system:workbench:edit")
    @Log(title = "workbench security reminder", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/security-reminder")
    public R<Void> saveSecurityReminderConfig(@RequestBody Map<String, Object> config) {
        return toAjax(workbenchLayoutService.saveSecurityReminderConfig(JsonUtils.toJsonString(config)));
    }

    @SaCheckPermission("system:workbench:edit")
    @Log(title = "workbench asset", businessType = BusinessType.INSERT)
    @PostMapping(value = "/asset/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<SysWorkbenchAssetUploadVo> uploadWorkbenchAsset(@RequestPart("file") MultipartFile file) {
        return R.ok(workbenchLayoutService.uploadWorkbenchAsset(file));
    }

    @GetMapping("/asset")
    public void workbenchAsset(@RequestParam String key, HttpServletResponse response) throws IOException {
        Path path = workbenchLayoutService.resolveWorkbenchAssetPath(key);
        if (path == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(workbenchLayoutService.resolveWorkbenchAssetContentType(key));
        response.setContentLengthLong(Files.size(path));
        response.setHeader("Cache-Control", "public, max-age=604800, immutable");
        response.setHeader("X-Content-Type-Options", "nosniff");
        Files.copy(path, response.getOutputStream());
    }

    @SaCheckPermission("system:workbench:edit")
    @Log(title = "workbench asset", businessType = BusinessType.DELETE)
    @DeleteMapping("/asset/delete")
    public R<Void> deleteWorkbenchAsset(@RequestParam String key) {
        return toAjax(workbenchLayoutService.deleteWorkbenchAsset(key));
    }

    @SaCheckPermission("system:workbench:query")
    @Log(title = "工作台配置包", businessType = BusinessType.EXPORT)
    @GetMapping("/config-package/export")
    public void exportConfigPackage(HttpServletResponse response) throws IOException {
        byte[] content = workbenchLayoutService.exportConfigPackage();
        response.setContentType("application/zip");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=workbench-config.zip");
        response.setContentLengthLong(content.length);
        response.getOutputStream().write(content);
    }

    @SaCheckPermission("system:workbench:edit")
    @PostMapping(value = "/config-package/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<SysWorkbenchConfigImportPreviewVo> previewConfigPackage(@RequestPart("file") MultipartFile file) {
        return R.ok(workbenchLayoutService.previewConfigPackage(file));
    }

    @SaCheckPermission("system:workbench:edit")
    @Log(title = "工作台配置包", businessType = BusinessType.IMPORT)
    @RepeatSubmit()
    @PostMapping(value = "/config-package/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importConfigPackage(@RequestPart("file") MultipartFile file) {
        return toAjax(workbenchLayoutService.importConfigPackage(file));
    }

    @SaCheckPermission("system:workbench:edit")
    @Log(title = "workbench layout", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/role/{roleId}")
    public R<Void> saveRoleLayout(@PathVariable Long roleId, @RequestBody List<SysWorkbenchLayoutBo> layouts) {
        return toAjax(workbenchLayoutService.saveRoleLayout(roleId, layouts));
    }

    @SaCheckPermission("system:workbench:edit")
    @Log(title = "workbench component config", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping("/role/{roleId}/component/{componentKey}/restore-default")
    public R<Void> restoreRoleComponentDefault(@PathVariable Long roleId, @PathVariable String componentKey) {
        return toAjax(workbenchLayoutService.restoreRoleComponentDefault(roleId, componentKey));
    }

    public record WorkbenchRoleOptionVo(Long roleId, String roleName, String roleKey) {
    }

}
