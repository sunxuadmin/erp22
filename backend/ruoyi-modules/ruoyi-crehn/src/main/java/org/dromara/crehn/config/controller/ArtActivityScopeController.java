package org.dromara.crehn.config.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.config.service.IArtActivityScopeService;
import org.dromara.crehn.domain.ActivitySchoolScope;
import org.dromara.crehn.domain.bo.ActivityScopeAssignBo;
import org.dromara.crehn.domain.vo.ActivitySchoolScopeVo;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/activity-scope")
public class ArtActivityScopeController extends BaseController {
    private final IArtActivityScopeService activityScopeService;

    @SaCheckPermission("crehn:activityScope:list")
    @GetMapping("/list")
    public R<List<ActivitySchoolScopeVo>> list(ActivitySchoolScope query) {
        return R.ok(activityScopeService.list(query));
    }

    @SaCheckPermission("crehn:activityScope:edit")
    @Log(title = "活动单位授权新增", businessType = BusinessType.UPDATE)
    @PostMapping("/assign")
    public R<Void> assign(@RequestBody ActivityScopeAssignBo bo) {
        activityScopeService.assign(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:activityScope:remove")
    @Log(title = "活动单位授权取消", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(activityScopeService.delete(ids));
    }
}
