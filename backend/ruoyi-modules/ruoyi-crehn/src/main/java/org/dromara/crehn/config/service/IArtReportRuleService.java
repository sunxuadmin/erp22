package org.dromara.crehn.config.service;

import org.dromara.crehn.domain.ActivityReportRule;
import org.dromara.crehn.domain.ReportRuleItem;
import org.dromara.crehn.domain.ReportRulePackage;
import org.dromara.crehn.domain.bo.ReportRuleApplyBo;
import org.dromara.crehn.domain.vo.ActivityReportRuleVo;
import org.dromara.crehn.domain.vo.ActivityRuleGroupOptionVo;
import org.dromara.crehn.domain.vo.ReportRuleItemVo;
import org.dromara.crehn.domain.vo.ReportRulePackageVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

public interface IArtReportRuleService {
    TableDataInfo<ReportRulePackageVo> queryPackagePage(ReportRulePackage query, PageQuery pageQuery);

    int savePackage(ReportRulePackage rulePackage);

    Long restoreDefaultPackage();

    int deletePackage(Long[] ids);

    TableDataInfo<ReportRuleItemVo> queryItemPage(ReportRuleItem query, PageQuery pageQuery);

    List<ReportRuleItemVo> queryItemList(ReportRuleItem query);

    int saveItem(ReportRuleItem item);

    int deleteItem(Long[] ids);

    TableDataInfo<ActivityReportRuleVo> queryActivityRulePage(ActivityReportRule query, PageQuery pageQuery);

    List<ActivityReportRuleVo> queryActivityRuleList(ActivityReportRule query);

    List<ActivityRuleGroupOptionVo> queryActivityRuleSchoolOptions(ActivityReportRule query);

    int saveActivityRule(ActivityReportRule rule);

    int deleteActivityRule(Long[] ids);

    int applyPackageToActivity(ReportRuleApplyBo bo);

    int restoreDefaultActivityRules(ReportRuleApplyBo bo);
}
