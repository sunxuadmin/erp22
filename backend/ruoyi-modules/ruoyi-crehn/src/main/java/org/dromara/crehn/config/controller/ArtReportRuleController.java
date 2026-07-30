package org.dromara.crehn.config.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.config.service.IArtReportRuleService;
import org.dromara.crehn.domain.ActivityReportRule;
import org.dromara.crehn.domain.ReportRuleItem;
import org.dromara.crehn.domain.ReportRulePackage;
import org.dromara.crehn.domain.bo.ReportRuleApplyBo;
import org.dromara.crehn.domain.vo.ActivityReportRuleVo;
import org.dromara.crehn.domain.vo.ActivityRuleGroupOptionVo;
import org.dromara.crehn.domain.vo.ReportRuleItemVo;
import org.dromara.crehn.domain.vo.ReportRulePackageVo;
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
@RequestMapping("/crehn/report-rule")
public class ArtReportRuleController extends BaseController {

    private final IArtReportRuleService reportRuleService;

    @SaCheckPermission("crehn:reportRule:list")
    @GetMapping("/package/list")
    public TableDataInfo<ReportRulePackageVo> packageList(ReportRulePackage query, PageQuery pageQuery) {
        return reportRuleService.queryPackagePage(query, pageQuery);
    }

    @SaCheckPermission("crehn:reportRule:edit")
    @Log(title = "art report rule package", businessType = BusinessType.UPDATE)
    @PostMapping("/package")
    public R<Void> savePackage(@RequestBody ReportRulePackage rulePackage) {
        return toAjax(reportRuleService.savePackage(rulePackage));
    }

    @SaCheckPermission("crehn:reportRule:edit")
    @Log(title = "restore default art report rule package", businessType = BusinessType.UPDATE)
    @PostMapping("/package/restore-default")
    public R<Long> restoreDefaultPackage() {
        return R.ok(reportRuleService.restoreDefaultPackage());
    }

    @SaCheckPermission("crehn:reportRule:remove")
    @Log(title = "art report rule package", businessType = BusinessType.DELETE)
    @DeleteMapping("/package/{ids}")
    public R<Void> deletePackage(@PathVariable Long[] ids) {
        return toAjax(reportRuleService.deletePackage(ids));
    }

    @SaCheckPermission("crehn:reportRule:list")
    @GetMapping("/item/list")
    public TableDataInfo<ReportRuleItemVo> itemList(ReportRuleItem query, PageQuery pageQuery) {
        return reportRuleService.queryItemPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:reportRule:list")
    @GetMapping("/item/options")
    public R<List<ReportRuleItemVo>> itemOptions(ReportRuleItem query) {
        query.setEnabled(true);
        return R.ok(reportRuleService.queryItemList(query));
    }

    @SaCheckPermission("crehn:reportRule:edit")
    @Log(title = "art report rule item", businessType = BusinessType.UPDATE)
    @PostMapping("/item")
    public R<Void> saveItem(@RequestBody ReportRuleItem item) {
        return toAjax(reportRuleService.saveItem(item));
    }

    @SaCheckPermission("crehn:reportRule:remove")
    @Log(title = "art report rule item", businessType = BusinessType.DELETE)
    @DeleteMapping("/item/{ids}")
    public R<Void> deleteItem(@PathVariable Long[] ids) {
        return toAjax(reportRuleService.deleteItem(ids));
    }

    @SaCheckPermission("crehn:reportRule:list")
    @GetMapping("/activity/list")
    public TableDataInfo<ActivityReportRuleVo> activityRuleList(ActivityReportRule query, PageQuery pageQuery) {
        return reportRuleService.queryActivityRulePage(query, pageQuery);
    }

    @SaCheckPermission("crehn:reportRule:list")
    @GetMapping("/activity/options")
    public R<List<ActivityReportRuleVo>> activityRuleOptions(ActivityReportRule query) {
        query.setEnabled(true);
        return R.ok(reportRuleService.queryActivityRuleList(query));
    }

    @SaCheckPermission(value = {"crehn:project:add", "crehn:reportRule:list"}, mode = SaMode.OR)
    @GetMapping("/activity/school-options")
    public R<List<ActivityRuleGroupOptionVo>> activityRuleSchoolOptions(ActivityReportRule query) {
        query.setEnabled(true);
        return R.ok(reportRuleService.queryActivityRuleSchoolOptions(query));
    }

    @SaCheckPermission("crehn:reportRule:edit")
    @Log(title = "art activity report rule", businessType = BusinessType.UPDATE)
    @PostMapping("/activity")
    public R<Void> saveActivityRule(@RequestBody ActivityReportRule rule) {
        return toAjax(reportRuleService.saveActivityRule(rule));
    }

    @SaCheckPermission("crehn:reportRule:remove")
    @Log(title = "art activity report rule", businessType = BusinessType.DELETE)
    @DeleteMapping("/activity/{ids}")
    public R<Void> deleteActivityRule(@PathVariable Long[] ids) {
        return toAjax(reportRuleService.deleteActivityRule(ids));
    }

    @SaCheckPermission("crehn:reportRule:edit")
    @Log(title = "apply art report rule package", businessType = BusinessType.INSERT)
    @PostMapping("/activity/apply")
    public R<Void> applyPackage(@RequestBody ReportRuleApplyBo bo) {
        return toAjax(reportRuleService.applyPackageToActivity(bo));
    }

    @SaCheckPermission("crehn:reportRule:edit")
    @Log(title = "restore default art activity report rules", businessType = BusinessType.UPDATE)
    @PostMapping("/activity/restore-default")
    public R<Integer> restoreDefaultActivityRules(@RequestBody ReportRuleApplyBo bo) {
        return R.ok(reportRuleService.restoreDefaultActivityRules(bo));
    }
}
