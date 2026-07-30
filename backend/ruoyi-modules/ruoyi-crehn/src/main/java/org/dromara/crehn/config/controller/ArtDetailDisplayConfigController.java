package org.dromara.crehn.config.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.config.service.ArtDetailDisplayConfigService;
import org.dromara.crehn.domain.vo.ArtDetailDisplayConfigVo;
import org.dromara.crehn.domain.vo.CategoryFieldSchemaVo;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crehn/detail-display-config")
public class ArtDetailDisplayConfigController {

    private final ArtDetailDisplayConfigService displayConfigService;

    @GetMapping
    public R<ArtDetailDisplayConfigVo> getConfig() {
        ArtDetailDisplayConfigVo config = displayConfigService.getConfig();
        if (!StpUtil.hasPermission("crehn:detailDisplayConfig:edit") && config.getReviewWorkbench() != null) {
            config.getReviewWorkbench().setColumns(config.getReviewWorkbench().getColumns().stream()
                .filter(column -> !"form".equals(column.getSource()))
                .toList());
            config.getReviewWorkbench().setCategoryColumns(java.util.Map.of());
        }
        return R.ok(config);
    }

    @GetMapping("/table")
    @SaCheckPermission("crehn:detailDisplayConfig:edit")
    public R<ArtDetailDisplayConfigVo.ListTableConfig> getTableConfig() {
        return R.ok(displayConfigService.getTableConfig());
    }

    @GetMapping("/category-fields/{categoryId}")
    @SaCheckPermission("crehn:detailDisplayConfig:edit")
    public R<List<CategoryFieldSchemaVo>> categoryFields(@PathVariable Long categoryId) {
        return R.ok(displayConfigService.categoryFields(categoryId));
    }

    @PutMapping
    @RepeatSubmit
    @SaCheckPermission("crehn:detailDisplayConfig:edit")
    @Log(title = "艺术评审显示配置", businessType = BusinessType.UPDATE)
    public R<Void> saveConfig(@RequestBody ArtDetailDisplayConfigVo config) {
        displayConfigService.saveConfig(config);
        return R.ok();
    }

    @PutMapping("/table")
    @RepeatSubmit
    @SaCheckPermission("crehn:detailDisplayConfig:edit")
    @Log(title = "全局业务表格配置", businessType = BusinessType.UPDATE)
    public R<Void> saveTableConfig(@RequestBody ArtDetailDisplayConfigVo.ListTableConfig config) {
        displayConfigService.saveTableConfig(config);
        return R.ok();
    }

    @PutMapping("/managed-activity/{activityId}")
    @RepeatSubmit
    @SaCheckPermission("crehn:detailDisplayConfig:edit")
    @Log(title = "当前业务活动", businessType = BusinessType.UPDATE)
    public R<Void> saveManagedActivity(@PathVariable Long activityId) {
        displayConfigService.saveManagedActivity(activityId);
        return R.ok();
    }
}
