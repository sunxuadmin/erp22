package org.dromara.crehn.school.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.bo.ArtAccountCreateBo;
import org.dromara.crehn.domain.bo.SchoolAccountReviewBo;
import org.dromara.crehn.domain.vo.SchoolAccountExportVo;
import org.dromara.crehn.domain.vo.SchoolAccountImportVo;
import org.dromara.crehn.school.service.IArtSchoolAccountService;
import org.dromara.common.core.domain.R;
import org.dromara.common.encrypt.annotation.ApiEncrypt;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import jakarta.servlet.http.HttpServletResponse;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysUserVo;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/school-account")
public class ArtSchoolAccountController extends BaseController {

    private final IArtSchoolAccountService schoolAccountService;

    @SaCheckPermission("crehn:schoolAccount:list")
    @GetMapping("/list")
    public TableDataInfo<SysUserVo> list(SysUserVo query, PageQuery pageQuery) {
        return schoolAccountService.querySchoolAccountPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:schoolAccount:list")
    @GetMapping("/available")
    public TableDataInfo<SysUserVo> available(SysUserVo query, PageQuery pageQuery) {
        return schoolAccountService.queryAvailableSchoolAccountPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:schoolAccount:query")
    @GetMapping("/{userId}")
    public R<SysUserVo> getInfo(@PathVariable Long userId) {
        return R.ok(schoolAccountService.getSchoolAccount(userId));
    }

    @SaCheckPermission("crehn:schoolAccount:add")
    @Log(title = "业务账号创建", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@RequestBody ArtAccountCreateBo bo) {
        schoolAccountService.createAccount(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:schoolAccount:review")
    @Log(title = "学校账号审核", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/review")
    public R<Void> review(@RequestBody SchoolAccountReviewBo bo) {
        schoolAccountService.updateReviewStatus(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:schoolAccount:edit")
    @Log(title = "学校账号修改", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@RequestBody SysUserBo bo) {
        schoolAccountService.updateSchoolAccount(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:schoolAccount:edit")
    @Log(title = "学校账号绑定单位", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{userId}/bind/{schoolId}")
    public R<Void> bind(@PathVariable Long userId, @PathVariable Long schoolId) {
        schoolAccountService.bindSchoolAccount(userId, schoolId);
        return R.ok();
    }

    @SaCheckPermission("crehn:schoolAccount:edit")
    @Log(title = "学校账号解绑单位", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{userId}/unbind")
    public R<Void> unbind(@PathVariable Long userId) {
        schoolAccountService.unbindSchoolAccount(userId);
        return R.ok();
    }

    @ApiEncrypt
    @SaCheckPermission("crehn:schoolAccount:resetPwd")
    @Log(title = "学校账号重置密码", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/resetPwd")
    public R<Void> resetPwd(@RequestBody SysUserBo bo) {
        schoolAccountService.resetPassword(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:schoolAccount:remove")
    @Log(title = "学校账号删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public R<Void> remove(@PathVariable Long[] userIds) {
        schoolAccountService.deleteSchoolAccounts(userIds);
        return R.ok();
    }

    @SaCheckPermission("crehn:schoolAccount:restore")
    @Log(title = "瀛︽牎璐﹀彿鎭㈠", businessType = BusinessType.UPDATE)
    @PutMapping("/restore/{userIds}")
    public R<Void> restore(@PathVariable Long[] userIds) {
        schoolAccountService.restoreSchoolAccounts(userIds);
        return R.ok();
    }

    @SaCheckPermission("crehn:schoolAccount:export")
    @Log(title = "学校账号导出", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SysUserVo query, HttpServletResponse response) {
        List<SchoolAccountExportVo> list = schoolAccountService.exportList(query);
        ExcelUtil.exportExcel(list, "学校账号", SchoolAccountExportVo.class, response);
    }

    @SaCheckPermission("crehn:schoolAccount:add")
    @Log(title = "学校账号导入", businessType = BusinessType.IMPORT)
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        List<SchoolAccountImportVo> list = ExcelUtil.importExcel(file.getInputStream(), SchoolAccountImportVo.class);
        return R.ok(schoolAccountService.importData(list, updateSupport));
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil.exportExcel(new ArrayList<>(), "学校账号导入模板", SchoolAccountImportVo.class, response);
    }
}
