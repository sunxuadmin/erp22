package org.dromara.crehn.config.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.config.service.IArtUploadRuleTemplateService;
import org.dromara.crehn.domain.UploadRuleTemplate;
import org.dromara.crehn.domain.bo.UploadRuleTemplateApplyBo;
import org.dromara.crehn.domain.vo.UploadRuleTemplateVo;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/upload-template")
public class ArtUploadRuleTemplateController extends BaseController {
    private final IArtUploadRuleTemplateService uploadRuleTemplateService;

    @SaCheckPermission("crehn:uploadTemplate:list")
    @GetMapping("/list")
    public TableDataInfo<UploadRuleTemplateVo> list(UploadRuleTemplate query, PageQuery pageQuery) {
        return uploadRuleTemplateService.queryPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:uploadTemplate:query")
    @GetMapping("/{id}")
    public R<UploadRuleTemplateVo> getInfo(@PathVariable Long id) {
        return R.ok(uploadRuleTemplateService.getInfo(id));
    }

    @SaCheckPermission("crehn:uploadTemplate:edit")
    @Log(title = "art upload rule template", businessType = BusinessType.UPDATE)
    @PostMapping
    public R<Void> save(@RequestBody UploadRuleTemplate template) {
        return toAjax(uploadRuleTemplateService.save(template));
    }

    @SaCheckPermission("crehn:uploadTemplate:copy")
    @Log(title = "art upload rule template copy", businessType = BusinessType.INSERT)
    @PostMapping("/copy/{id}")
    public R<UploadRuleTemplateVo> copy(@PathVariable Long id) {
        return R.ok(uploadRuleTemplateService.copy(id));
    }

    @SaCheckPermission("crehn:uploadTemplate:add")
    @Log(title = "art upload rule template save as", businessType = BusinessType.INSERT)
    @PostMapping("/from-category/{categoryId}")
    public R<UploadRuleTemplateVo> saveFromCategory(@PathVariable Long categoryId, String templateName) {
        return R.ok(uploadRuleTemplateService.saveFromCategory(categoryId, templateName));
    }

    @SaCheckPermission("crehn:uploadTemplate:add")
    @Log(title = "art upload rule template sync categories", businessType = BusinessType.INSERT)
    @PostMapping("/sync-from-activity/{activityId}")
    public R<List<UploadRuleTemplateVo>> syncFromActivityCategories(@PathVariable Long activityId) {
        return R.ok(uploadRuleTemplateService.syncFromActivityCategories(activityId));
    }

    @SaCheckPermission("crehn:uploadTemplate:apply")
    @Log(title = "art upload rule template apply", businessType = BusinessType.UPDATE)
    @PostMapping("/apply")
    public R<Void> apply(@RequestBody UploadRuleTemplateApplyBo bo) {
        uploadRuleTemplateService.applyToCategory(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:uploadTemplate:remove")
    @Log(title = "art upload rule template", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(uploadRuleTemplateService.delete(ids));
    }
}
