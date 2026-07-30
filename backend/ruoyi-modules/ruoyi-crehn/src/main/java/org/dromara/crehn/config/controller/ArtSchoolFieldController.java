package org.dromara.crehn.config.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.config.service.IArtSchoolFieldService;
import org.dromara.crehn.domain.SchoolFieldSchema;
import org.dromara.crehn.domain.vo.SchoolFieldSchemaVo;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/school-field")
public class ArtSchoolFieldController extends BaseController {
    private final IArtSchoolFieldService schoolFieldService;

    @SaCheckPermission("crehn:schoolField:list")
    @GetMapping("/list")
    public R<List<SchoolFieldSchemaVo>> list(SchoolFieldSchema query) {
        return R.ok(schoolFieldService.list(query));
    }

    @SaCheckPermission("crehn:schoolField:edit")
    @Log(title = "art school field", businessType = BusinessType.UPDATE)
    @PostMapping
    public R<Void> save(@RequestBody SchoolFieldSchema field) {
        return toAjax(schoolFieldService.save(field));
    }

    @SaCheckPermission("crehn:schoolField:remove")
    @Log(title = "art school field", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(schoolFieldService.delete(ids));
    }
}
