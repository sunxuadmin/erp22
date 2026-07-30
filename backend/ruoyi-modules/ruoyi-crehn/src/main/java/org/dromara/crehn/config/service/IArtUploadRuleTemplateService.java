package org.dromara.crehn.config.service;

import org.dromara.crehn.domain.UploadRuleTemplate;
import org.dromara.crehn.domain.bo.UploadRuleTemplateApplyBo;
import org.dromara.crehn.domain.vo.UploadRuleTemplateVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

public interface IArtUploadRuleTemplateService {
    TableDataInfo<UploadRuleTemplateVo> queryPage(UploadRuleTemplate query, PageQuery pageQuery);

    List<UploadRuleTemplateVo> queryList(UploadRuleTemplate query);

    UploadRuleTemplateVo getInfo(Long id);

    int save(UploadRuleTemplate template);

    UploadRuleTemplateVo copy(Long id);

    UploadRuleTemplateVo saveFromCategory(Long categoryId, String templateName);

    List<UploadRuleTemplateVo> syncFromActivityCategories(Long activityId);

    void applyToCategory(UploadRuleTemplateApplyBo bo);

    int delete(Long[] ids);
}
