package org.dromara.crehn.notice.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.UserMessage;
import org.dromara.crehn.domain.vo.UserMessageVo;
import org.dromara.crehn.notice.service.IArtUserMessageService;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/message")
public class ArtUserMessageController extends BaseController {
    private final IArtUserMessageService userMessageService;

    @SaCheckPermission("crehn:message:list")
    @GetMapping("/my-list")
    public TableDataInfo<UserMessageVo> myList(UserMessage query, PageQuery pageQuery) {
        return userMessageService.queryMyPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:message:list")
    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(userMessageService.unreadCount());
    }

    @SaCheckPermission("crehn:message:read")
    @PutMapping("/{id}/read")
    public R<Void> markRead(@PathVariable Long id) {
        userMessageService.markRead(id);
        return R.ok();
    }

    @SaCheckPermission("crehn:message:read")
    @PutMapping("/read-all")
    public R<Void> markAllRead() {
        userMessageService.markAllRead();
        return R.ok();
    }
}
