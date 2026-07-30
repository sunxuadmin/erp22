package org.dromara.crehn.config.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.config.service.IArtSchoolInfoService;
import org.dromara.crehn.domain.SchoolInfo;
import org.dromara.crehn.domain.vo.SchoolInfoExportVo;
import org.dromara.crehn.domain.vo.SchoolInfoImportVo;
import org.dromara.crehn.domain.vo.SchoolInfoVo;
import org.dromara.crehn.domain.vo.SchoolImpactVo;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/school-info")
public class ArtSchoolInfoController extends BaseController {
    private final IArtSchoolInfoService schoolInfoService;

    @SaCheckPermission(value = {"crehn:schoolInfo:list", "crehn:schoolAccount:list"}, mode = SaMode.OR)
    @GetMapping("/list")
    public TableDataInfo<SchoolInfoVo> list(SchoolInfo query, PageQuery pageQuery) {
        return schoolInfoService.queryPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:schoolInfo:list")
    @GetMapping("/options")
    public R<List<SchoolInfoVo>> options(SchoolInfo query) {
        return R.ok(schoolInfoService.queryList(query));
    }

    @SaCheckPermission("crehn:schoolInfo:query")
    @GetMapping("/{id}")
    public R<SchoolInfoVo> getInfo(@PathVariable Long id) {
        return R.ok(schoolInfoService.getInfo(id));
    }

    @SaCheckPermission(value = {"crehn:schoolInfo:list", "crehn:schoolAccount:list"}, mode = SaMode.OR)
    @GetMapping("/impact/{ids}")
    public R<List<SchoolImpactVo>> impact(@PathVariable Long[] ids) {
        return R.ok(schoolInfoService.impact(ids));
    }

    @SaCheckPermission("crehn:schoolInfo:edit")
    @Log(title = "单位资料新增/修改", businessType = BusinessType.UPDATE)
    @PostMapping
    public R<Void> save(@RequestBody SchoolInfo school) {
        return toAjax(schoolInfoService.save(school));
    }

    @SaCheckPermission("crehn:schoolInfo:remove")
    @Log(title = "单位回收", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids,
                          @RequestParam(value = "confirmed", defaultValue = "false") Boolean confirmed) {
        return toAjax(schoolInfoService.delete(ids, Boolean.TRUE.equals(confirmed)));
    }

    @SaCheckPermission("crehn:schoolInfo:edit")
    @Log(title = "单位恢复", businessType = BusinessType.UPDATE)
    @PutMapping("/restore/{ids}")
    public R<Void> restore(@PathVariable Long[] ids) {
        return toAjax(schoolInfoService.restore(ids));
    }

    @SaCheckPermission("crehn:schoolInfo:list")
    @Log(title = "单位资料导出", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(SchoolInfo query, HttpServletResponse response) {
        List<SchoolInfoExportVo> list = schoolInfoService.exportList(query);
        ExcelUtil.exportExcel(list, "单位资料", SchoolInfoExportVo.class, response);
    }

    @SaCheckPermission("crehn:schoolInfo:edit")
    @Log(title = "单位资料导入", businessType = BusinessType.IMPORT)
    @PostMapping(value = "/importData", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Void> importData(@RequestPart("file") MultipartFile file,
                              @RequestParam(value = "updateSupport", defaultValue = "false") Boolean updateSupport) throws Exception {
        List<SchoolInfoImportVo> list = ExcelUtil.importExcel(file.getInputStream(), SchoolInfoImportVo.class);
        return R.ok(schoolInfoService.importData(list, Boolean.TRUE.equals(updateSupport)));
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) {
        ExcelUtil.exportExcel(new ArrayList<>(), "单位资料导入模板", SchoolInfoImportVo.class, response);
    }
}
