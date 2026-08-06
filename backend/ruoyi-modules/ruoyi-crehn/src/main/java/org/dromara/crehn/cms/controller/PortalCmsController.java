package org.dromara.crehn.cms.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.crehn.cms.service.IPortalCmsService;
import org.dromara.crehn.domain.PortalArticle;
import org.dromara.crehn.domain.PortalChannel;
import org.dromara.crehn.domain.PortalHomeComponent;
import org.dromara.crehn.domain.PortalMediaAsset;
import org.dromara.crehn.domain.PortalPageLayout;
import org.dromara.crehn.domain.PortalRelease;
import org.dromara.crehn.domain.PortalSite;
import org.dromara.crehn.domain.bo.PortalArticleActionBo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crehn/cms")
public class PortalCmsController {
    private final IPortalCmsService cmsService;

    @SaCheckPermission("crehn:cms:site:list")
    @GetMapping("/site/list")
    public R<List<PortalSite>> sites() {
        return R.ok(cmsService.listSites());
    }

    @SaCheckPermission("crehn:cms:site:edit")
    @Log(title = "门户站点保存", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/site")
    public R<PortalSite> saveSite(@RequestBody PortalSite site) {
        return R.ok(cmsService.saveSite(site));
    }

    @SaCheckPermission("crehn:cms:channel:list")
    @GetMapping("/channel/list")
    public R<List<PortalChannel>> channels(@RequestParam Long siteId) {
        return R.ok(cmsService.listChannels(siteId));
    }

    @SaCheckPermission("crehn:cms:channel:edit")
    @Log(title = "门户栏目保存", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/channel")
    public R<PortalChannel> saveChannel(@RequestBody PortalChannel channel) {
        return R.ok(cmsService.saveChannel(channel));
    }

    @SaCheckPermission("crehn:cms:media:list")
    @GetMapping("/media/list")
    public R<List<PortalMediaAsset>> media(@RequestParam Long siteId) {
        return R.ok(cmsService.listMedia(siteId));
    }

    @SaCheckPermission("crehn:cms:media:edit")
    @Log(title = "门户媒体保存", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/media")
    public R<PortalMediaAsset> saveMedia(@RequestBody PortalMediaAsset media) {
        return R.ok(cmsService.saveMedia(media));
    }

    @SaCheckPermission("crehn:cms:article:publish")
    @Log(title = "门户媒体人工复核", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/media/{mediaId}/approve")
    public R<Void> approveMedia(@PathVariable Long mediaId, @RequestParam String reason) {
        cmsService.approveMedia(mediaId, reason);
        return R.ok();
    }

    @SaCheckPermission("crehn:cms:home:list")
    @GetMapping("/home/list")
    public R<List<PortalHomeComponent>> homeComponents(@RequestParam Long siteId) {
        return R.ok(cmsService.listHomeComponents(siteId));
    }

    @SaCheckPermission("crehn:cms:home:edit")
    @Log(title = "门户首页组件保存", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/home")
    public R<PortalHomeComponent> saveHomeComponent(@RequestBody PortalHomeComponent component) {
        return R.ok(cmsService.saveHomeComponent(component));
    }

    @SaCheckPermission("crehn:cms:home:list")
    @GetMapping("/layout/list")
    public R<List<PortalPageLayout>> pageLayouts(
        @RequestParam Long siteId,
        @RequestParam(required = false) String pageCode
    ) {
        return R.ok(cmsService.listPageLayouts(siteId, pageCode));
    }

    @SaCheckPermission("crehn:cms:home:edit")
    @Log(title = "门户页面布局保存", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/layout")
    public R<PortalPageLayout> savePageLayout(@RequestBody PortalPageLayout layout) {
        return R.ok(cmsService.savePageLayout(layout));
    }

    @SaCheckPermission("crehn:cms:home:edit")
    @Log(title = "门户页面布局切换", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/layout/{siteId}/activate")
    public R<Void> activatePageLayout(
        @PathVariable Long siteId,
        @RequestParam(required = false) String pageCode,
        @RequestParam String layoutCode
    ) {
        cmsService.activatePageLayout(siteId, pageCode, layoutCode);
        return R.ok();
    }

    @SaCheckPermission("crehn:cms:article:list")
    @GetMapping("/article/list")
    public TableDataInfo<PortalArticle> list(PortalArticle query, PageQuery pageQuery) {
        return cmsService.queryArticles(query, pageQuery);
    }

    @SaCheckPermission("crehn:cms:article:edit")
    @Log(title = "门户文章草稿保存", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/article/draft")
    public R<PortalArticle> saveDraft(@RequestBody PortalArticle article) {
        return R.ok(cmsService.saveDraft(article));
    }

    @SaCheckPermission("crehn:cms:article:review")
    @Log(title = "门户文章提交审核", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/article/{articleId}/review")
    public R<Void> submitReview(@PathVariable Long articleId) {
        cmsService.submitReview(articleId);
        return R.ok();
    }

    @SaCheckPermission("crehn:cms:article:publish")
    @Log(title = "门户文章发布", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/article/publish")
    public R<PortalArticle> publish(@Valid @RequestBody PortalArticleActionBo bo) {
        return R.ok(cmsService.publish(bo));
    }

    @SaCheckPermission("crehn:cms:article:publish")
    @Log(title = "门户文章下线", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/article/offline")
    public R<Void> offline(@Valid @RequestBody PortalArticleActionBo bo) {
        cmsService.offline(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:cms:article:publish")
    @Log(title = "门户文章回滚", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/article/rollback")
    public R<PortalArticle> rollback(@Valid @RequestBody PortalArticleActionBo bo) {
        return R.ok(cmsService.rollback(bo));
    }

    @SaCheckPermission("crehn:cms:home:publish")
    @Log(title = "门户首页发布", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/home/{siteId}/publish")
    public R<PortalRelease> publishHome(
        @PathVariable Long siteId,
        @RequestParam String reason,
        @RequestParam(required = false) String layoutCode
    ) {
        return R.ok(cmsService.publishHome(siteId, layoutCode, reason));
    }

    @SaIgnore
    @GetMapping("/public/site/code/{siteCode}")
    public R<PortalSite> publicSite(@PathVariable String siteCode) {
        return R.ok(cmsService.publicSite(siteCode));
    }

    @SaIgnore
    @GetMapping("/public/site/{siteId}/articles")
    public R<List<PortalArticle>> publicArticles(
        @PathVariable Long siteId,
        @RequestParam(required = false) Long channelId
    ) {
        return R.ok(cmsService.publicArticles(siteId, channelId));
    }

    @SaIgnore
    @GetMapping("/public/article/{articleCode}")
    public R<PortalArticle> publicArticle(@PathVariable String articleCode) {
        return R.ok(cmsService.publicArticle(articleCode));
    }

    @SaIgnore
    @GetMapping("/public/site/{siteId}/home")
    public R<List<PortalHomeComponent>> publicHome(@PathVariable Long siteId) {
        return R.ok(cmsService.publicHomeComponents(siteId));
    }

    @SaIgnore
    @GetMapping("/public/site/{siteId}/home/config")
    public R<Map<String, Object>> publicHomeConfig(@PathVariable Long siteId) {
        return R.ok(cmsService.publicHomeSnapshot(siteId));
    }
}
