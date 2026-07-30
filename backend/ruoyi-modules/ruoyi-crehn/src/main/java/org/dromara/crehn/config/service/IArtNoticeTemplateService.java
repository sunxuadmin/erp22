package org.dromara.crehn.config.service;

import org.dromara.crehn.domain.NoticeTemplate;
import org.dromara.crehn.domain.vo.NoticeTemplateVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface IArtNoticeTemplateService {
    TableDataInfo<NoticeTemplateVo> queryPage(NoticeTemplate query, PageQuery pageQuery);

    List<NoticeTemplateVo> listForRegister(String userType, Long activityId, Long schoolId, String schoolType);

    List<NoticeTemplateVo> listForHome();

    TableDataInfo<NoticeTemplateVo> queryHomePage(PageQuery pageQuery);

    int save(NoticeTemplate template);

    int saveHomePage(NoticeTemplate template);

    int deleteHomePage(Long[] ids);

    void downloadAttachment(Long ossId, HttpServletResponse response) throws IOException;

    int delete(Long[] ids);
}
