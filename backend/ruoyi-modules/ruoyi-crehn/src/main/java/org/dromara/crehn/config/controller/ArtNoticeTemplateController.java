package org.dromara.crehn.config.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.config.service.IArtNoticeTemplateService;
import org.dromara.crehn.domain.NoticeTemplate;
import org.dromara.crehn.domain.vo.NoticeTemplateVo;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/notice-template")
public class ArtNoticeTemplateController extends BaseController {
    private final IArtNoticeTemplateService noticeTemplateService;

    @SaCheckPermission("crehn:noticeTemplate:list")
    @GetMapping("/list")
    public TableDataInfo<NoticeTemplateVo> list(NoticeTemplate query, PageQuery pageQuery) {
        return noticeTemplateService.queryPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:noticeTemplate:edit")
    @Log(title = "art notice template", businessType = BusinessType.UPDATE)
    @PostMapping
    public R<Void> save(@RequestBody NoticeTemplate template) {
        return toAjax(noticeTemplateService.save(template));
    }

    @SaCheckPermission("crehn:noticeTemplate:remove")
    @Log(title = "art notice template", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(noticeTemplateService.delete(ids));
    }

    @GetMapping("/attachment/{ossId}/download")
    public void downloadAttachment(@PathVariable Long ossId, HttpServletResponse response) throws IOException {
        noticeTemplateService.downloadAttachment(ossId, response);
    }
}
