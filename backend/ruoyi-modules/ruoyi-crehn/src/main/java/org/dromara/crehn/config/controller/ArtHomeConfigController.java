package org.dromara.crehn.config.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.config.service.IArtHomeConfigService;
import org.dromara.crehn.config.service.IArtNoticeTemplateService;
import org.dromara.crehn.domain.HomeContentCard;
import org.dromara.crehn.domain.HomePageModule;
import org.dromara.crehn.domain.HomeSiteConfig;
import org.dromara.crehn.domain.NoticeTemplate;
import org.dromara.crehn.domain.vo.HomeContentCardVo;
import org.dromara.crehn.domain.vo.HomeLocalPdfUploadVo;
import org.dromara.crehn.domain.vo.HomePageModuleVo;
import org.dromara.crehn.domain.vo.HomeSiteConfigVo;
import org.dromara.crehn.domain.vo.NoticeTemplateVo;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/system/home-config")
public class ArtHomeConfigController extends BaseController {
    private final IArtHomeConfigService homeConfigService;
    private final IArtNoticeTemplateService noticeTemplateService;

    @SaCheckPermission("system:homeConfig:query")
    @GetMapping("/site")
    public R<HomeSiteConfigVo> siteConfig() {
        return R.ok(homeConfigService.getSiteConfig());
    }

    @SaCheckPermission("system:homeConfig:edit")
    @Log(title = "home site config", businessType = BusinessType.UPDATE)
    @PutMapping("/site")
    public R<Void> saveSiteConfig(@RequestBody HomeSiteConfig config) {
        return toAjax(homeConfigService.saveSiteConfig(config));
    }

    @SaCheckPermission("system:homeConfig:query")
    @GetMapping("/module/list")
    public R<List<HomePageModuleVo>> moduleList() {
        return R.ok(homeConfigService.listModules(null));
    }

    @SaCheckPermission("system:homeConfig:edit")
    @Log(title = "home page module", businessType = BusinessType.UPDATE)
    @PostMapping("/module")
    public R<Void> saveModule(@RequestBody HomePageModule module) {
        return toAjax(homeConfigService.saveModule(module));
    }

    @SaCheckPermission("system:homeConfig:edit")
    @Log(title = "home page module sort", businessType = BusinessType.UPDATE)
    @PutMapping("/module/sort")
    public R<Void> sortModules(@RequestBody List<HomePageModule> modules) {
        return toAjax(homeConfigService.sortModules(modules));
    }

    @SaCheckPermission("system:homeConfig:remove")
    @Log(title = "home page module", businessType = BusinessType.DELETE)
    @DeleteMapping("/module/{ids}")
    public R<Void> removeModule(@PathVariable Long[] ids) {
        return toAjax(homeConfigService.deleteModules(ids));
    }

    @SaCheckPermission("system:homeConfig:query")
    @GetMapping("/card/list")
    public TableDataInfo<HomeContentCardVo> cardList(HomeContentCard query, PageQuery pageQuery) {
        return homeConfigService.queryCardPage(query, pageQuery);
    }

    @SaCheckPermission("system:homeConfig:edit")
    @Log(title = "home content card", businessType = BusinessType.UPDATE)
    @PostMapping("/card")
    public R<Void> saveCard(@RequestBody HomeContentCard card) {
        return toAjax(homeConfigService.saveCard(card));
    }

    @SaCheckPermission("system:homeConfig:edit")
    @Log(title = "home content card sort", businessType = BusinessType.UPDATE)
    @PutMapping("/card/sort")
    public R<Void> sortCards(@RequestBody List<HomeContentCard> cards) {
        return toAjax(homeConfigService.sortCards(cards));
    }

    @SaCheckPermission("system:homeConfig:edit")
    @Log(title = "home content card pdf", businessType = BusinessType.INSERT)
    @PostMapping(value = "/card/pdf/local-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<HomeLocalPdfUploadVo> uploadLocalPdf(@RequestPart("file") MultipartFile file) {
        return R.ok(homeConfigService.uploadLocalPdf(file));
    }

    @SaCheckPermission("system:homeConfig:remove")
    @Log(title = "home content card", businessType = BusinessType.DELETE)
    @DeleteMapping("/card/{ids}")
    public R<Void> removeCard(@PathVariable Long[] ids) {
        return toAjax(homeConfigService.deleteCards(ids));
    }

    @SaCheckPermission("system:homeConfig:query")
    @GetMapping("/stage/list")
    public TableDataInfo<NoticeTemplateVo> stageList(PageQuery pageQuery) {
        return noticeTemplateService.queryHomePage(pageQuery);
    }

    @SaCheckPermission("system:homeConfig:edit")
    @Log(title = "home stage config", businessType = BusinessType.UPDATE)
    @PostMapping("/stage")
    public R<Void> saveStage(@RequestBody NoticeTemplate template) {
        return toAjax(noticeTemplateService.saveHomePage(template));
    }

    @SaCheckPermission("system:homeConfig:remove")
    @Log(title = "home stage config", businessType = BusinessType.DELETE)
    @DeleteMapping("/stage/{ids}")
    public R<Void> removeStage(@PathVariable Long[] ids) {
        return toAjax(noticeTemplateService.deleteHomePage(ids));
    }
}
