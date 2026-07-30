package org.dromara.crehn.config.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.config.service.IArtHomeConfigService;
import org.dromara.crehn.domain.HomeContentCard;
import org.dromara.crehn.domain.HomePageModule;
import org.dromara.crehn.domain.HomeSiteConfig;
import org.dromara.crehn.domain.vo.HomeContentCardVo;
import org.dromara.crehn.domain.vo.HomeLocalPdfUploadVo;
import org.dromara.crehn.domain.vo.HomePageConfigVo;
import org.dromara.crehn.domain.vo.HomePageModuleVo;
import org.dromara.crehn.domain.vo.HomeSiteConfigVo;
import org.dromara.crehn.mapper.HomeContentCardMapper;
import org.dromara.crehn.mapper.HomePageModuleMapper;
import org.dromara.crehn.mapper.HomeSiteConfigMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysNotice;
import org.dromara.system.domain.SysOssExt;
import org.dromara.system.domain.vo.SysNoticeVo;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.mapper.SysNoticeMapper;
import org.dromara.system.service.ISysOssService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ArtHomeConfigServiceImpl implements IArtHomeConfigService {
    private static final String CONFIG_CODE = "portal_home";
    private static final int DEFAULT_NOTICE_LIMIT = 6;
    private static final String DEFAULT_LOGO = "assets/logo-icon.png";
    private static final String DEFAULT_LOGIN_ADMIN_TEXT = "管理账户已登录";
    private static final String DEFAULT_LOGIN_SCHOOL_TEXT = "学校账户已登录";
    private static final String DEFAULT_LOGIN_EXPERT_TEXT = "专家账户已登录";
    private static final String DEFAULT_LOGIN_DEFAULT_TEXT = "已登录";
    private static final long MAX_PORTAL_LOCAL_PDF_SIZE = 50L * 1024 * 1024;
    private static final String DEFAULT_HEADER_BUTTONS_JSON = """
        [
          {"text":"登录入口","link":"/login","style":"ghost","enabled":true,"showDesktop":true,"showMobile":true,"sortOrder":10},
          {"text":"注册","link":"/register","style":"solid","enabled":true,"showDesktop":true,"showMobile":true,"sortOrder":20}
        ]
        """;
    private static final String DEFAULT_LOGIN_STATE_CONFIG_JSON = """
        {
          "adminText":"管理账户已登录",
          "schoolText":"学校账户已登录",
          "expertText":"专家账户已登录",
          "defaultText":"已登录"
        }
        """;

    private final HomeSiteConfigMapper siteConfigMapper;
    private final HomePageModuleMapper pageModuleMapper;
    private final HomeContentCardMapper contentCardMapper;
    private final SysNoticeMapper sysNoticeMapper;
    private final ISysOssService ossService;

    @Value("${crehn.portal.local-pdf-dir:}")
    private String portalLocalPdfDir;

    @Override
    public HomePageConfigVo getPublicConfig() {
        HomeSiteConfigVo config = getSiteConfig();
        List<HomePageModuleVo> modules = enabledAndSortedModules(listModules(true));
        List<HomeContentCardVo> cards = listCards(null, true);
        boolean useDefaultCards = CollUtil.isEmpty(cards);
        Map<String, List<HomeContentCardVo>> groupedCards = cards.stream()
            .collect(Collectors.groupingBy(HomeContentCardVo::getSectionKey, LinkedHashMap::new, Collectors.toList()));

        HomePageConfigVo result = new HomePageConfigVo();
        result.setSite(buildSite(config));
        result.setHeaderButtons(enabledAndSorted(parseList(config.getHeaderButtonsJson(), HomePageConfigVo.ButtonVo.class)));
        result.setLoginState(buildLoginState(config));
        result.setModules(modules.stream()
            .map(module -> buildModule(module, cardsForModule(module.getModuleCode(), groupedCards, useDefaultCards), config))
            .toList());
        result.setNav(buildNavItems(config, modules));

        HomePageModuleVo heroModule = moduleByCode(modules, "home");
        HomePageModuleVo guideModule = moduleByCode(modules, "guide");
        HomePageModuleVo categoryModule = moduleByCode(modules, "category");
        HomePageModuleVo stageModule = moduleByCode(modules, "stage");
        HomePageModuleVo noticeModule = moduleByCode(modules, "notice");

        if (heroModule != null) {
            result.setHero(buildHero(heroModule, cardsForModule("home", groupedCards, useDefaultCards), config));
        }
        if (guideModule != null) {
            result.setGuide(buildSection(guideModule, cardsForModule("guide", groupedCards, useDefaultCards)));
        }
        if (categoryModule != null) {
            result.setCategory(buildSection(categoryModule, cardsForModule("category", groupedCards, useDefaultCards)));
        }
        if (stageModule != null) {
            result.setStage(buildStageSection(stageModule, cardsForModule("stage", groupedCards, useDefaultCards)));
        }
        if (noticeModule != null) {
            result.setNotice(buildNoticeSection(noticeModule, cardsForModule("notice", groupedCards, useDefaultCards), config));
        }
        result.setFooter(buildFooter(config));
        return result;
    }

    @Override
    public HomeSiteConfigVo getSiteConfig() {
        HomeSiteConfigVo config = siteConfigMapper.selectVoOne(Wrappers.lambdaQuery(HomeSiteConfig.class)
            .eq(HomeSiteConfig::getConfigCode, CONFIG_CODE)
            .last("limit 1"), false);
        return config == null ? defaultSiteConfig() : fillSiteDefaults(config);
    }

    @Override
    public int saveSiteConfig(HomeSiteConfig config) {
        if (config == null) {
            throw new ServiceException("首页配置不能为空");
        }
        HomeSiteConfig exist = siteConfigMapper.selectOne(Wrappers.lambdaQuery(HomeSiteConfig.class)
            .eq(HomeSiteConfig::getConfigCode, CONFIG_CODE)
            .last("limit 1"), false);
        config.setConfigCode(CONFIG_CODE);
        if (StringUtils.isBlank(config.getSiteName())) {
            throw new ServiceException("站点名称不能为空");
        }
        if (config.getNoticeLimit() == null || config.getNoticeLimit() < 1) {
            config.setNoticeLimit(DEFAULT_NOTICE_LIMIT);
        }
        if (StringUtils.isBlank(config.getHeaderButtonsJson())) {
            config.setHeaderButtonsJson(DEFAULT_HEADER_BUTTONS_JSON);
        }
        if (StringUtils.isBlank(config.getLoginStateConfigJson())) {
            config.setLoginStateConfigJson(DEFAULT_LOGIN_STATE_CONFIG_JSON);
        }
        if (config.getEnabled() == null) {
            config.setEnabled(true);
        }
        if (exist != null) {
            config.setId(exist.getId());
            return siteConfigMapper.updateById(config);
        }
        return siteConfigMapper.insert(config);
    }

    @Override
    public List<HomePageModuleVo> listModules(Boolean enabled) {
        LambdaQueryWrapper<HomePageModule> wrapper = Wrappers.lambdaQuery(HomePageModule.class)
            .eq(enabled != null, HomePageModule::getEnabled, enabled)
            .orderByAsc(HomePageModule::getSortOrder)
            .orderByDesc(HomePageModule::getCreateTime);
        List<HomePageModuleVo> modules = pageModuleMapper.selectVoList(wrapper);
        return CollUtil.isEmpty(modules) ? defaultModules() : modules;
    }

    @Override
    public int saveModule(HomePageModule module) {
        if (module == null) {
            throw new ServiceException("页面配置不能为空");
        }
        if (StringUtils.isBlank(module.getModuleTitle())) {
            throw new ServiceException("页面标题不能为空");
        }
        if (StringUtils.isBlank(module.getModuleCode())) {
            module.setModuleCode("module_" + System.currentTimeMillis());
        }
        module.setModuleCode(normalizeCode(module.getModuleCode()));
        if (StringUtils.isBlank(module.getAnchor())) {
            module.setAnchor(module.getModuleCode());
        }
        module.setAnchor(normalizeAnchor(module.getAnchor()));
        if (StringUtils.isBlank(module.getModuleType())) {
            module.setModuleType("custom");
        }
        if ("home".equals(module.getModuleCode())) {
            module.setModuleType("hero");
            module.setAnchor("home");
            module.setEnabled(true);
            module.setTemplateCode("hero");
        }
        if (StringUtils.isBlank(module.getTemplateCode())) {
            module.setTemplateCode(module.getModuleType());
        }
        if (StringUtils.isBlank(module.getLayoutType())) {
            module.setLayoutType("default");
        }
        if (module.getNavEnabled() == null) {
            module.setNavEnabled(true);
        }
        if (module.getEnabled() == null) {
            module.setEnabled(true);
        }
        if (module.getSortOrder() == null) {
            module.setSortOrder(0);
        }
        HomePageModule exist = pageModuleMapper.selectOne(Wrappers.lambdaQuery(HomePageModule.class)
            .eq(HomePageModule::getModuleCode, module.getModuleCode())
            .last("limit 1"), false);
        if (exist != null) {
            module.setId(exist.getId());
            return pageModuleMapper.updateById(module);
        }
        return pageModuleMapper.insert(module);
    }

    @Override
    public int sortModules(List<HomePageModule> modules) {
        if (CollUtil.isEmpty(modules)) {
            return 0;
        }
        int count = 0;
        for (HomePageModule module : modules) {
            if (module == null || module.getId() == null) {
                continue;
            }
            HomePageModule update = new HomePageModule();
            update.setId(module.getId());
            update.setSortOrder(module.getSortOrder() == null ? 0 : module.getSortOrder());
            count += pageModuleMapper.updateById(update);
        }
        return count;
    }

    @Override
    public int deleteModules(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        List<HomePageModule> modules = pageModuleMapper.selectList(Wrappers.lambdaQuery(HomePageModule.class)
            .in(HomePageModule::getId, Arrays.asList(ids)));
        boolean hasHome = CollUtil.emptyIfNull(modules).stream()
            .anyMatch(module -> "home".equals(module.getModuleCode()) || "home".equals(module.getAnchor()));
        if (hasHome) {
            throw new ServiceException("活动首页为固定页面，不能删除");
        }
        List<String> sectionKeys = CollUtil.emptyIfNull(modules).stream()
            .map(HomePageModule::getModuleCode)
            .filter(StringUtils::isNotBlank)
            .toList();
        int count = pageModuleMapper.deleteByIds(Arrays.asList(ids));
        if (CollUtil.isNotEmpty(sectionKeys)) {
            contentCardMapper.delete(Wrappers.lambdaQuery(HomeContentCard.class)
                .in(HomeContentCard::getSectionKey, sectionKeys));
        }
        return count;
    }

    @Override
    public TableDataInfo<HomeContentCardVo> queryCardPage(HomeContentCard query, PageQuery pageQuery) {
        Page<HomeContentCardVo> page = contentCardMapper.selectVoPage(pageQuery.build(), buildCardQuery(query));
        return TableDataInfo.build(page);
    }

    @Override
    public List<HomeContentCardVo> listCards(String sectionKey, Boolean enabled) {
        HomeContentCard query = new HomeContentCard();
        query.setSectionKey(sectionKey);
        query.setEnabled(enabled);
        return contentCardMapper.selectVoList(buildCardQuery(query));
    }

    @Override
    public HomeContentCardVo getPublicCard(Long id) {
        if (id == null) {
            return null;
        }
        HomeContentCardVo card = contentCardMapper.selectVoById(id);
        if (card == null || !Boolean.TRUE.equals(card.getEnabled()) || StringUtils.isBlank(card.getSectionKey())) {
            return null;
        }
        HomePageModule module = pageModuleMapper.selectOne(Wrappers.lambdaQuery(HomePageModule.class)
            .eq(HomePageModule::getModuleCode, card.getSectionKey())
            .last("limit 1"));
        return module != null && Boolean.TRUE.equals(module.getEnabled()) ? card : null;
    }

    @Override
    public HomeLocalPdfUploadVo uploadLocalPdf(MultipartFile file) {
        validateLocalPdf(file);
        Path root = localPdfRoot();
        String month = new SimpleDateFormat("yyyy/MM").format(new Date());
        String safeName = sanitizePdfName(file.getOriginalFilename());
        String generatedName = UUID.randomUUID().toString().replace("-", "") + ".pdf";
        String fileKey = month + "/" + generatedName;
        Path target = resolveLocalPdfPath(fileKey, false);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ServiceException("PDF 文件保存失败");
        }
        if (!target.startsWith(root)) {
            throw new ServiceException("PDF 文件路径不合法");
        }
        HomeLocalPdfUploadVo vo = new HomeLocalPdfUploadVo();
        vo.setFileKey(fileKey);
        vo.setOriginalName(safeName);
        vo.setFileName(generatedName);
        vo.setSize(file.getSize());
        return vo;
    }

    @Override
    public Path resolveLocalPdfPath(String fileKey) {
        if (StringUtils.isBlank(fileKey)) {
            return null;
        }
        try {
            Path path = resolveLocalPdfPath(fileKey, true);
            return Files.isRegularFile(path) ? path : null;
        } catch (ServiceException ignored) {
            return null;
        }
    }

    @Override
    public int saveCard(HomeContentCard card) {
        if (card == null) {
            throw new ServiceException("页面内容块不能为空");
        }
        if (StringUtils.isBlank(card.getSectionKey())) {
            throw new ServiceException("请选择内容块所属页面");
        }
        if (StringUtils.isBlank(card.getCardType())) {
            card.setCardType(StringUtils.isNotBlank(card.getTemplateCode()) ? card.getTemplateCode() : "info");
        }
        if (StringUtils.isBlank(card.getCardTitle())) {
            if (isTitleOptionalCardType(card.getCardType())) {
                card.setCardTitle(defaultCardTitle(card.getCardType()));
            } else {
                throw new ServiceException("内容块标题不能为空");
            }
        }
        if (StringUtils.isBlank(card.getTemplateCode())) {
            card.setTemplateCode(card.getCardType());
        }
        if (StringUtils.isBlank(card.getLayoutType())) {
            card.setLayoutType("default");
        }
        if (StringUtils.isBlank(card.getCardCode())) {
            card.setCardCode(card.getSectionKey() + "_" + System.currentTimeMillis());
        }
        if (card.getEnabled() == null) {
            card.setEnabled(true);
        }
        if (card.getHighlight() == null) {
            card.setHighlight(false);
        }
        if (card.getSortOrder() == null) {
            card.setSortOrder(0);
        }
        if (card.getId() == null) {
            return contentCardMapper.insert(card);
        }
        return contentCardMapper.updateById(card);
    }

    @Override
    public int sortCards(List<HomeContentCard> cards) {
        if (CollUtil.isEmpty(cards)) {
            return 0;
        }
        int count = 0;
        for (HomeContentCard card : cards) {
            if (card == null || card.getId() == null) {
                continue;
            }
            HomeContentCard update = new HomeContentCard();
            update.setId(card.getId());
            update.setSortOrder(card.getSortOrder() == null ? 0 : card.getSortOrder());
            count += contentCardMapper.updateById(update);
        }
        return count;
    }

    @Override
    public int deleteCards(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        List<HomeContentCard> cards = contentCardMapper.selectList(Wrappers.lambdaQuery(HomeContentCard.class)
            .in(HomeContentCard::getId, Arrays.asList(ids)));
        boolean hasHomeHero = CollUtil.emptyIfNull(cards).stream()
            .anyMatch(card -> "home".equals(card.getSectionKey())
                && ("hero".equals(card.getCardType()) || "hero".equals(card.getTemplateCode()) || "home_hero".equals(card.getCardCode())));
        if (hasHomeHero) {
            throw new ServiceException("Hero 首屏为活动首页固定内容，不能删除");
        }
        return contentCardMapper.deleteByIds(Arrays.asList(ids));
    }

    private LambdaQueryWrapper<HomeContentCard> buildCardQuery(HomeContentCard query) {
        return Wrappers.lambdaQuery(HomeContentCard.class)
            .eq(StringUtils.isNotBlank(query.getSectionKey()), HomeContentCard::getSectionKey, query.getSectionKey())
            .like(StringUtils.isNotBlank(query.getCardTitle()), HomeContentCard::getCardTitle, query.getCardTitle())
            .eq(query.getEnabled() != null, HomeContentCard::getEnabled, query.getEnabled())
            .orderByAsc(HomeContentCard::getSortOrder)
            .orderByDesc(HomeContentCard::getCreateTime);
    }

    private boolean isTitleOptionalCardType(String cardType) {
        return "spacer".equals(cardType) || "button".equals(cardType) || "html".equals(cardType);
    }

    private String defaultCardTitle(String cardType) {
        if ("spacer".equals(cardType)) {
            return "分割留白";
        }
        if ("button".equals(cardType)) {
            return "按钮入口";
        }
        if ("html".equals(cardType)) {
            return "自定义 HTML";
        }
        return "内容块";
    }

    private HomeSiteConfigVo defaultSiteConfig() {
        HomeSiteConfigVo config = new HomeSiteConfigVo();
        config.setConfigCode(CONFIG_CODE);
        config.setSiteName("河南省第八届大学生艺术展演");
        config.setLogoUrl(DEFAULT_LOGO);
        config.setHomeLink("#home");
        config.setHeaderButtonsJson(DEFAULT_HEADER_BUTTONS_JSON);
        config.setLoginStateConfigJson(DEFAULT_LOGIN_STATE_CONFIG_JSON);
        config.setNoticeLimit(DEFAULT_NOTICE_LIMIT);
        config.setFooterTitle("河南省第八届大学生艺术展演");
        config.setFooterText("Copyright © 2026 ");
        config.setEnabled(true);
        return config;
    }

    private HomeSiteConfigVo fillSiteDefaults(HomeSiteConfigVo config) {
        HomeSiteConfigVo defaults = defaultSiteConfig();
        if (StringUtils.isBlank(config.getSiteName())) {
            config.setSiteName(defaults.getSiteName());
        }
        if (StringUtils.isBlank(config.getLogoUrl())) {
            config.setLogoUrl(defaults.getLogoUrl());
        }
        if (StringUtils.isBlank(config.getHomeLink())) {
            config.setHomeLink(defaults.getHomeLink());
        }
        if (StringUtils.isBlank(config.getHeaderButtonsJson())) {
            config.setHeaderButtonsJson(defaults.getHeaderButtonsJson());
        }
        if (StringUtils.isBlank(config.getLoginStateConfigJson())) {
            config.setLoginStateConfigJson(defaults.getLoginStateConfigJson());
        }
        if (config.getNoticeLimit() == null || config.getNoticeLimit() < 1) {
            config.setNoticeLimit(defaults.getNoticeLimit());
        }
        if (StringUtils.isBlank(config.getFooterTitle())) {
            config.setFooterTitle(defaults.getFooterTitle());
        }
        if (StringUtils.isBlank(config.getFooterText())) {
            config.setFooterText(defaults.getFooterText());
        }
        return config;
    }

    private HomePageConfigVo.SiteVo buildSite(HomeSiteConfigVo config) {
        Map<String, Object> styleConfig = parseConfig(config.getSectionConfigJson());
        HomePageConfigVo.SiteVo site = new HomePageConfigVo.SiteVo();
        site.setBrandName(config.getSiteName());
        site.setLogo(resolveOssUrl(config.getLogoOssId(), config.getLogoUrl(), DEFAULT_LOGO));
        site.setHomeLink(config.getHomeLink());
        site.setCustomCss(config.getCustomCss());
        site.setCustomMobileCss(firstText(styleConfig.get("globalMobileCss")));
        site.setHeaderCss(firstText(styleConfig.get("headerCss")));
        site.setHeaderMobileCss(firstText(styleConfig.get("headerMobileCss")));
        site.setHomeLayoutMode(firstText(styleConfig.get("homeLayoutMode"), "flow"));
        Integer canvasHeight = intValue(styleConfig.get("homeCanvasHeight"));
        site.setHomeCanvasHeight(canvasHeight == null ? 1280 : canvasHeight);
        site.setHeaderPlacement(firstText(styleConfig.get("headerPlacement"), "independent"));
        site.setFooterPlacement(firstText(styleConfig.get("footerPlacement"), "independent"));
        Map<String, Object> headerConfig = new LinkedHashMap<>();
        Object headerDesktop = styleConfig.get("headerDesktop");
        Object headerMobile = styleConfig.get("headerMobile");
        headerConfig.put("desktop", headerDesktop instanceof Map<?, ?> map ? normalizeMap(map) : new LinkedHashMap<>());
        headerConfig.put("mobile", headerMobile instanceof Map<?, ?> map ? normalizeMap(map) : new LinkedHashMap<>());
        site.setHeaderConfig(headerConfig);
        return site;
    }

    private HomePageConfigVo.LoginStateVo buildLoginState(HomeSiteConfigVo config) {
        Map<String, Object> loginStateConfig = parseConfig(config.getLoginStateConfigJson());
        HomePageConfigVo.LoginStateVo loginState = new HomePageConfigVo.LoginStateVo();
        loginState.setAdminText(firstText(loginStateConfig.get("adminText"), DEFAULT_LOGIN_ADMIN_TEXT));
        loginState.setSchoolText(firstText(loginStateConfig.get("schoolText"), DEFAULT_LOGIN_SCHOOL_TEXT));
        loginState.setExpertText(firstText(loginStateConfig.get("expertText"), DEFAULT_LOGIN_EXPERT_TEXT));
        loginState.setDefaultText(firstText(loginStateConfig.get("defaultText"), DEFAULT_LOGIN_DEFAULT_TEXT));
        return loginState;
    }

    private HomePageConfigVo.FooterVo buildFooter(HomeSiteConfigVo config) {
        Map<String, Object> styleConfig = parseConfig(config.getSectionConfigJson());
        HomePageConfigVo.FooterVo footer = new HomePageConfigVo.FooterVo();
        footer.setTitle(config.getFooterTitle());
        footer.setText(config.getFooterText());
        footer.setOrganizer(config.getOrganizer());
        footer.setSponsor(config.getSponsor());
        footer.setTechnicalSupport(config.getTechnicalSupport());
        footer.setContactPhone(config.getContactPhone());
        footer.setContactEmail(config.getContactEmail());
        footer.setContactAddress(config.getContactAddress());
        footer.setIcpText(config.getIcpText());
        footer.setIcpLink(config.getIcpLink());
        footer.setPoliceText(config.getPoliceText());
        footer.setPoliceLink(config.getPoliceLink());
        footer.setCustomCss(firstText(styleConfig.get("footerCss")));
        footer.setCustomMobileCss(firstText(styleConfig.get("footerMobileCss")));
        Map<String, Object> displayConfig = new LinkedHashMap<>();
        Object footerDesktop = styleConfig.get("footerDesktop");
        Object footerMobile = styleConfig.get("footerMobile");
        displayConfig.put("desktop", footerDesktop instanceof Map<?, ?> map ? normalizeMap(map) : new LinkedHashMap<>());
        displayConfig.put("mobile", footerMobile instanceof Map<?, ?> map ? normalizeMap(map) : new LinkedHashMap<>());
        footer.setDisplayConfig(displayConfig);
        return footer;
    }

    private List<HomePageConfigVo.NavItemVo> buildNavItems(HomeSiteConfigVo config, List<HomePageModuleVo> modules) {
        Map<String, Object> styleConfig = parseConfig(config.getSectionConfigJson());
        if ("custom".equalsIgnoreCase(firstText(styleConfig.get("navMode")))) {
            List<HomePageConfigVo.NavItemVo> customItems = navItemList(styleConfig.get("navItems"));
            if (CollUtil.isNotEmpty(customItems)) {
                return customItems;
            }
        }
        List<HomePageConfigVo.NavItemVo> autoItems = new ArrayList<>(modules.stream()
            .filter(module -> module.getNavEnabled() == null || module.getNavEnabled())
            .map(this::buildNavItem)
            .filter(item -> item.getEnabled() == null || item.getEnabled())
            .toList());
        autoItems.addAll(navItemList(styleConfig.get("navExtraItems")));
        return autoItems.stream()
            .filter(item -> item.getEnabled() == null || item.getEnabled())
            .sorted(Comparator.comparing(item -> item.getSortOrder() == null ? 0 : item.getSortOrder()))
            .toList();
    }

    private HomePageConfigVo.NavItemVo buildNavItem(HomePageModuleVo module) {
        Map<String, Object> moduleConfig = parseConfig(module.getConfigJson());
        HomePageConfigVo.NavItemVo item = new HomePageConfigVo.NavItemVo();
        item.setText(firstText(moduleConfig.get("navTitle"), module.getModuleTitle()));
        item.setLink(firstText(moduleConfig.get("navLink"), moduleConfig.get("pageUrl"), "#" + normalizeAnchor(module.getAnchor())));
        item.setLinkType(firstText(moduleConfig.get("navOpenMode"), moduleConfig.get("defaultOpenMode"), "anchor"));
        item.setEnabled(module.getEnabled());
        item.setShowDesktop(true);
        item.setShowMobile(true);
        item.setSortOrder(intValue(firstObject(moduleConfig.get("navSortOrder"), module.getSortOrder())));
        item.setPopupWidth(intValue(firstObject(moduleConfig.get("popupWidth"))));
        item.setPopupHeight(intValue(firstObject(moduleConfig.get("popupHeight"))));
        return item;
    }

    private HomePageConfigVo.ModuleVo buildModule(HomePageModuleVo module, List<HomeContentCardVo> cards, HomeSiteConfigVo config) {
        Map<String, Object> moduleConfig = parseConfig(module.getConfigJson());
        HomePageConfigVo.ModuleVo vo = new HomePageConfigVo.ModuleVo();
        vo.setId(module.getId());
        vo.setModuleCode(module.getModuleCode());
        vo.setModuleType(module.getModuleType());
        vo.setAnchor(normalizeAnchor(module.getAnchor()));
        vo.setEyebrow(module.getEyebrow());
        vo.setTitle(module.getModuleTitle());
        vo.setIntro(module.getIntro());
        vo.setMoreText(module.getMoreText());
        vo.setMoreLink(module.getMoreLink());
        vo.setNavEnabled(module.getNavEnabled());
        vo.setEnabled(module.getEnabled());
        vo.setSortOrder(module.getSortOrder());
        vo.setLayoutType(module.getLayoutType());
        vo.setTemplateCode(module.getTemplateCode());
        vo.setConfig(moduleConfig);
        vo.setCustomClass(firstText(moduleConfig.get("customClass")));
        vo.setCustomCss(firstText(moduleConfig.get("customCss")));
        vo.setCustomMobileCss(firstText(moduleConfig.get("customMobileCss"), moduleConfig.get("mobileCss")));
        List<HomePageConfigVo.CardVo> cardVos = CollUtil.emptyIfNull(cards).stream().map(this::buildCard).toList();
        vo.setCards(cardVos);
        vo.setItems(cardVos);
        if ("notice".equals(module.getModuleType()) || "notice".equals(module.getModuleCode())) {
            vo.setNoticeItems(buildNoticeItems(module, cards, config));
        }
        return vo;
    }

    private HomePageConfigVo.HeroVo buildHero(HomePageModuleVo module, List<HomeContentCardVo> cards, HomeSiteConfigVo siteConfig) {
        HomeContentCardVo card = CollUtil.emptyIfNull(cards).stream()
            .filter(item -> "hero".equals(item.getCardType()) || "hero".equals(item.getTemplateCode()))
            .findFirst()
            .orElse(CollUtil.emptyIfNull(cards).stream().findFirst().orElse(null));
        Map<String, Object> moduleConfig = parseConfig(module.getConfigJson());
        Map<String, Object> cardConfig = card == null ? new LinkedHashMap<>() : parseConfig(card.getConfigJson());
        HomePageConfigVo.HeroVo hero = new HomePageConfigVo.HeroVo();
        hero.setModuleCode(module.getModuleCode());
        hero.setAnchor(normalizeAnchor(module.getAnchor()));
        hero.setKicker(firstText(cardConfig.get("kicker"), moduleConfig.get("kicker"), module.getEyebrow(), "DYZ · ART EXHIBITION"));
        hero.setSloganLines(stringList(cardConfig.get("sloganLines")));
        if (hero.getSloganLines().isEmpty() && card != null) {
            hero.setSloganLines(splitLines(card.getCardTitle()));
        }
        if (hero.getSloganLines().isEmpty()) {
            hero.setSloganLines(List.of("向美而行", "逐梦未来"));
        }
        hero.setSubtitle(firstText(card == null ? null : card.getCardSubtitle(), moduleConfig.get("subtitle"), siteConfig.getSiteName()));
        hero.setDesc(firstText(card == null ? null : card.getCardContent(), moduleConfig.get("desc"), ""));
        if (card != null) {
            hero.setImage(resolveOssUrl(card.getImageOssId(), card.getImageUrl(), null));
        }
        hero.setDynamicLabel(firstText(cardConfig.get("dynamicLabel"), moduleConfig.get("dynamicLabel"), "ART"));
        hero.setDynamicWord(firstText(cardConfig.get("dynamicWord"), moduleConfig.get("dynamicWord"), "艺术展演"));
        hero.setVisualAnimation(firstText(cardConfig.get("visualAnimation"), moduleConfig.get("visualAnimation"), "float"));
        hero.setVisualFollowMouse(booleanValue(firstObject(cardConfig.get("visualFollowMouse"), moduleConfig.get("visualFollowMouse")), true));
        Integer tiltIntensity = intValue(firstObject(cardConfig.get("visualTiltIntensity"), moduleConfig.get("visualTiltIntensity")));
        hero.setVisualTiltIntensity(tiltIntensity == null ? 8 : tiltIntensity);
        hero.setBackgroundType(firstText(cardConfig.get("backgroundType"), moduleConfig.get("backgroundType"), "default"));
        hero.setBackgroundImage(firstText(cardConfig.get("backgroundImage"), moduleConfig.get("backgroundImage")));
        hero.setBackgroundHtml(firstText(cardConfig.get("backgroundHtml"), moduleConfig.get("backgroundHtml")));
        hero.setButtons(buttonList(cardConfig.get("buttons")));
        if (hero.getButtons().isEmpty()) {
            hero.setButtons(defaultHeroButtons());
        }
        hero.setMeta(stringList(cardConfig.get("meta")));
        Object particle = firstObject(cardConfig.get("particle"), moduleConfig.get("particle"));
        hero.setParticleConfig(particle instanceof Map<?, ?> map ? normalizeMap(map) : defaultParticleConfig());
        hero.setCustomClass(firstText(cardConfig.get("customClass"), moduleConfig.get("customClass")));
        hero.setCustomCss(firstText(cardConfig.get("customCss"), moduleConfig.get("customCss")));
        hero.setCustomMobileCss(firstText(cardConfig.get("customMobileCss"), cardConfig.get("mobileCss"), moduleConfig.get("customMobileCss"), moduleConfig.get("mobileCss")));
        Object elementCss = firstObject(cardConfig.get("elementCss"), moduleConfig.get("elementCss"));
        hero.setElementCss(elementCss instanceof Map<?, ?> map ? normalizeMap(map) : new LinkedHashMap<>());
        return hero;
    }

    private HomePageConfigVo.SectionVo buildSection(HomePageModuleVo module, List<HomeContentCardVo> cards) {
        Map<String, Object> moduleConfig = parseConfig(module.getConfigJson());
        HomePageConfigVo.SectionVo section = new HomePageConfigVo.SectionVo();
        section.setModuleCode(module.getModuleCode());
        section.setModuleType(module.getModuleType());
        section.setAnchor(normalizeAnchor(module.getAnchor()));
        section.setEyebrow(module.getEyebrow());
        section.setTitle(module.getModuleTitle());
        section.setIntro(module.getIntro());
        section.setMoreText(module.getMoreText());
        section.setMoreLink(module.getMoreLink());
        section.setConfig(moduleConfig);
        section.setCustomClass(firstText(moduleConfig.get("customClass")));
        section.setCustomCss(firstText(moduleConfig.get("customCss")));
        section.setCustomMobileCss(firstText(moduleConfig.get("customMobileCss"), moduleConfig.get("mobileCss")));
        List<HomePageConfigVo.CardVo> items = CollUtil.emptyIfNull(cards).stream()
            .map(this::buildCard)
            .filter(Objects::nonNull)
            .toList();
        section.setCards(items);
        section.setItems(items);
        return section;
    }

    private HomePageConfigVo.SectionVo buildStageSection(HomePageModuleVo module, List<HomeContentCardVo> cards) {
        HomePageConfigVo.SectionVo section = buildSection(module, cards);
        section.setItems(CollUtil.emptyIfNull(section.getCards()).stream()
            .filter(this::isStageTimelineCard)
            .toList());
        return section;
    }

    private boolean isStageTimelineCard(HomePageConfigVo.CardVo card) {
        if (card == null) {
            return false;
        }
        String cardType = firstText(card.getCardType(), card.getTemplateCode());
        return StringUtils.isBlank(cardType) || "stage".equalsIgnoreCase(cardType) || "timeline".equalsIgnoreCase(cardType);
    }

    private HomePageConfigVo.CardVo buildCard(HomeContentCardVo card) {
        Map<String, Object> cardConfig = parseConfig(card.getConfigJson());
        HomePageConfigVo.CardVo vo = new HomePageConfigVo.CardVo();
        vo.setId(card.getId());
        vo.setSectionKey(card.getSectionKey());
        vo.setCode(card.getCardCode());
        vo.setCardType(card.getCardType());
        vo.setIcon(card.getIcon());
        vo.setTitle(card.getCardTitle());
        vo.setSubtitle(card.getCardSubtitle());
        vo.setDesc(card.getCardContent());
        vo.setContent(card.getCardContent());
        vo.setImage(resolveOssUrl(card.getImageOssId(), card.getImageUrl(), null));
        vo.setLinkText(card.getLinkText());
        vo.setActionText(card.getLinkText());
        vo.setLink(card.getLinkUrl());
        vo.setLinkType(card.getLinkType());
        vo.setHighlight(card.getHighlight());
        vo.setLayoutType(card.getLayoutType());
        vo.setTemplateCode(card.getTemplateCode());
        vo.setSlotKey(card.getSlotKey());
        enrichCardDetailAttachment(vo, cardConfig);
        vo.setConfig(cardConfig);
        vo.setCustomClass(firstText(cardConfig.get("customClass")));
        vo.setCustomCss(firstText(cardConfig.get("customCss")));
        vo.setCustomMobileCss(firstText(cardConfig.get("customMobileCss"), cardConfig.get("mobileCss")));
        vo.setSortOrder(card.getSortOrder());
        return vo;
    }

    private void enrichCardDetailAttachment(HomePageConfigVo.CardVo card, Map<String, Object> cardConfig) {
        if (card == null || card.getId() == null || cardConfig == null) {
            return;
        }
        if (!"pdf".equalsIgnoreCase(firstText(cardConfig.get("detailSource")))) {
            return;
        }
        String storage = firstText(cardConfig.get("detailPdfStorage"), "oss");
        if ("local".equalsIgnoreCase(storage)) {
            enrichLocalPdfAttachment(card, cardConfig);
            return;
        }
        Long ossId = longValue(cardConfig.get("detailPdfOssId"));
        if (ossId == null) {
            cardConfig.remove("detailPdfPreviewUrl");
            return;
        }
        SysOssVo oss = ossService.getById(ossId);
        if (oss == null || !isPdfOss(oss)) {
            cardConfig.remove("detailPdfPreviewUrl");
            cardConfig.remove("detailPdfSize");
            return;
        }
        cardConfig.put("detailPdfOssId", String.valueOf(ossId));
        cardConfig.put("detailPdfName", firstText(cardConfig.get("detailPdfTitle"), oss.getOriginalName(), oss.getFileName(), "PDF 文档"));
        cardConfig.put("detailPdfPreviewUrl", "/auth/home/card/" + card.getId() + "/pdf");
        Long size = ossFileSize(oss);
        if (size != null) {
            cardConfig.put("detailPdfSize", size);
        }
        cardConfig.remove("detailPdfUrl");
        cardConfig.remove("detailPdfRawUrl");
    }

    private void enrichLocalPdfAttachment(HomePageConfigVo.CardVo card, Map<String, Object> cardConfig) {
        String fileKey = firstText(cardConfig.get("detailPdfLocalKey"));
        Path pdfPath = resolveLocalPdfPath(fileKey);
        if (pdfPath == null) {
            cardConfig.remove("detailPdfPreviewUrl");
            cardConfig.remove("detailPdfSize");
            return;
        }
        cardConfig.put("detailPdfStorage", "local");
        cardConfig.put("detailPdfName", firstText(cardConfig.get("detailPdfTitle"), cardConfig.get("detailPdfLocalName"), "PDF 文档"));
        cardConfig.put("detailPdfPreviewUrl", "/auth/home/card/" + card.getId() + "/pdf");
        try {
            cardConfig.put("detailPdfSize", Files.size(pdfPath));
        } catch (IOException ignored) {
            cardConfig.remove("detailPdfSize");
        }
        cardConfig.remove("detailPdfUrl");
        cardConfig.remove("detailPdfRawUrl");
        cardConfig.remove("detailPdfOssId");
    }

    private boolean isPdfOss(SysOssVo oss) {
        if (oss == null) {
            return false;
        }
        String suffix = firstText(oss.getFileSuffix()).replace(".", "");
        if ("pdf".equalsIgnoreCase(suffix)) {
            return true;
        }
        SysOssExt ext = parseOssExt(oss);
        return ext != null && "application/pdf".equalsIgnoreCase(firstText(ext.getContentType()));
    }

    private Long ossFileSize(SysOssVo oss) {
        SysOssExt ext = parseOssExt(oss);
        return ext == null ? null : ext.getFileSize();
    }

    private SysOssExt parseOssExt(SysOssVo oss) {
        if (oss == null || StringUtils.isBlank(oss.getExt1())) {
            return null;
        }
        try {
            return JsonUtils.parseObject(oss.getExt1(), SysOssExt.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void validateLocalPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择 PDF 文件");
        }
        if (file.getSize() > MAX_PORTAL_LOCAL_PDF_SIZE) {
            throw new ServiceException("PDF 文件大小不能超过 50MB");
        }
        String originalName = firstText(file.getOriginalFilename(), "document.pdf");
        if (!StringUtils.endsWithIgnoreCase(originalName, ".pdf")) {
            throw new ServiceException("只允许上传 PDF 文件");
        }
        String contentType = firstText(file.getContentType());
        if (StringUtils.isNotBlank(contentType)
            && !"application/pdf".equalsIgnoreCase(contentType)
            && !"application/x-pdf".equalsIgnoreCase(contentType)
            && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            throw new ServiceException("PDF 文件类型不合法");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(5);
            if (header.length < 5 || header[0] != '%' || header[1] != 'P' || header[2] != 'D' || header[3] != 'F' || header[4] != '-') {
                throw new ServiceException("PDF 文件内容不合法");
            }
        } catch (IOException e) {
            throw new ServiceException("PDF 文件读取失败");
        }
    }

    private Path localPdfRoot() {
        Path root = StringUtils.isBlank(portalLocalPdfDir)
            ? Path.of(System.getProperty("user.dir"), "data", "portal-pdf")
            : Path.of(portalLocalPdfDir);
        return root.toAbsolutePath().normalize();
    }

    private Path resolveLocalPdfPath(String fileKey, boolean existingOnly) {
        String normalizedKey = firstText(fileKey).replace('\\', '/');
        if (StringUtils.isBlank(normalizedKey)
            || normalizedKey.startsWith("/")
            || normalizedKey.contains("../")
            || normalizedKey.contains("/..")
            || !StringUtils.endsWithIgnoreCase(normalizedKey, ".pdf")) {
            throw new ServiceException("PDF 文件路径不合法");
        }
        Path root = localPdfRoot();
        Path path = root.resolve(normalizedKey).toAbsolutePath().normalize();
        if (!path.startsWith(root)) {
            throw new ServiceException("PDF 文件路径不合法");
        }
        if (existingOnly && !Files.isRegularFile(path)) {
            return null;
        }
        return path;
    }

    private String sanitizePdfName(String originalName) {
        String name = firstText(originalName, "document.pdf").replaceAll("[\\\\/:*?\"<>|,]+", "_").trim();
        if (StringUtils.isBlank(name)) {
            name = "document.pdf";
        }
        if (!StringUtils.endsWithIgnoreCase(name, ".pdf")) {
            name += ".pdf";
        }
        return name.length() > 160 ? name.substring(0, 154) + ".pdf" : name;
    }

    private HomePageConfigVo.NoticeSectionVo buildNoticeSection(HomePageModuleVo module, List<HomeContentCardVo> cards, HomeSiteConfigVo siteConfig) {
        Map<String, Object> moduleConfig = parseConfig(module.getConfigJson());
        Map<String, Object> cardConfig = CollUtil.emptyIfNull(cards).stream()
            .findFirst()
            .map(card -> parseConfig(card.getConfigJson()))
            .orElseGet(LinkedHashMap::new);
        HomePageConfigVo.NoticeSectionVo section = new HomePageConfigVo.NoticeSectionVo();
        section.setEyebrow(module.getEyebrow());
        section.setTitle(module.getModuleTitle());
        section.setIntro(module.getIntro());
        section.setMoreText(StringUtils.isBlank(module.getMoreText()) ? "查看更多 →" : module.getMoreText());
        section.setMoreLink(StringUtils.isBlank(module.getMoreLink()) ? "#notice" : module.getMoreLink());
        section.setShowPageHeader(booleanValue(moduleConfig.get("showPageHeader"), true));
        section.setShowPageEyebrow(booleanValue(firstObject(moduleConfig.get("showPageEyebrow"), moduleConfig.get("showNoticeEyebrow")), true));
        section.setShowPageTitle(booleanValue(firstObject(moduleConfig.get("showPageTitle"), moduleConfig.get("showNoticeTitle")), true));
        section.setShowPageIntro(booleanValue(moduleConfig.get("showPageIntro"), true));
        section.setShowPageMore(booleanValue(firstObject(moduleConfig.get("showPageMore"), moduleConfig.get("showMoreLink"), moduleConfig.get("showMore"), cardConfig.get("showMoreLink")), true));
        section.setHidePageHeaderMobile(booleanValue(moduleConfig.get("hidePageHeaderMobile"), false));
        section.setPageHeaderAlign(firstText(moduleConfig.get("pageHeaderAlign"), moduleConfig.get("noticeTitleAlign"), "left"));
        section.setPageHeaderSize(firstText(moduleConfig.get("pageHeaderSize"), "normal"));
        section.setPageHeaderCss(firstText(moduleConfig.get("pageHeaderCss")));
        section.setPageEyebrowCss(firstText(moduleConfig.get("pageEyebrowCss"), moduleConfig.get("noticeEyebrowCss")));
        section.setPageTitleCss(firstText(moduleConfig.get("pageTitleCss"), moduleConfig.get("noticeTitleCss")));
        section.setPageIntroCss(firstText(moduleConfig.get("pageIntroCss")));
        section.setPageMoreCss(firstText(moduleConfig.get("pageMoreCss"), moduleConfig.get("noticeMoreCss")));
        section.setShowMoreLink(section.getShowPageMore());
        section.setNoticeVariant(firstText(moduleConfig.get("noticeVariant"), cardConfig.get("noticeVariant"), "card"));
        section.setShowNoticeEyebrow(section.getShowPageEyebrow());
        section.setShowNoticeTitle(section.getShowPageTitle());
        section.setNoticeTitleAlign(section.getPageHeaderAlign());
        section.setNoticeEyebrowCss(section.getPageEyebrowCss());
        section.setNoticeTitleCss(section.getPageTitleCss());
        section.setNoticeMoreCss(section.getPageMoreCss());
        section.setItems(buildNoticeItems(module, cards, siteConfig));
        return section;
    }

    private List<HomePageConfigVo.NoticeItemVo> buildNoticeItems(HomePageModuleVo module, List<HomeContentCardVo> cards, HomeSiteConfigVo siteConfig) {
        List<HomePageConfigVo.NoticeItemVo> systemItems = listLatestNotices(noticeLimit(module, cards, siteConfig));
        if (CollUtil.isNotEmpty(systemItems)) {
            return systemItems;
        }
        return CollUtil.emptyIfNull(cards).stream()
            .filter(this::hasNoticeCardContent)
            .map(this::buildNoticeItem)
            .filter(Objects::nonNull)
            .toList();
    }

    private boolean hasNoticeCardContent(HomeContentCardVo card) {
        if (card == null) {
            return false;
        }
        String cardType = firstText(card.getCardType(), card.getTemplateCode());
        if (StringUtils.isNotBlank(cardType) && !"notice".equals(cardType)) {
            return false;
        }
        boolean hasBody = StringUtils.isNotBlank(card.getCardSubtitle())
            || StringUtils.isNotBlank(card.getCardContent())
            || StringUtils.isNotBlank(card.getImageUrl())
            || card.getImageOssId() != null;
        if (hasBody) {
            return true;
        }
        return StringUtils.isNotBlank(card.getCardTitle()) && !"notice_list".equals(card.getCardCode());
    }

    private int noticeLimit(HomePageModuleVo module, List<HomeContentCardVo> cards, HomeSiteConfigVo siteConfig) {
        Map<String, Object> moduleConfig = parseConfig(module.getConfigJson());
        Map<String, Object> cardConfig = CollUtil.emptyIfNull(cards).stream()
            .findFirst()
            .map(card -> parseConfig(card.getConfigJson()))
            .orElseGet(LinkedHashMap::new);
        Integer limit = intValue(firstObject(moduleConfig.get("noticeLimit"), cardConfig.get("noticeLimit"), siteConfig.getNoticeLimit()));
        return limit == null || limit < 1 ? DEFAULT_NOTICE_LIMIT : Math.min(limit, 12);
    }

    private List<HomePageConfigVo.NoticeItemVo> listLatestNotices(Integer noticeLimit) {
        int limit = noticeLimit == null || noticeLimit < 1 ? DEFAULT_NOTICE_LIMIT : Math.min(noticeLimit, 12);
        Page<SysNotice> pageQuery = new Page<>(1, limit);
        Page<SysNoticeVo> page = sysNoticeMapper.selectVoPage(pageQuery, Wrappers.lambdaQuery(SysNotice.class)
            .eq(SysNotice::getStatus, "0")
            .orderByDesc(SysNotice::getCreateTime)
            .orderByDesc(SysNotice::getNoticeId));
        return page.getRecords().stream().map(this::buildNoticeItem).toList();
    }

    private HomePageConfigVo.NoticeItemVo buildNoticeItem(SysNoticeVo notice) {
        HomePageConfigVo.NoticeItemVo item = new HomePageConfigVo.NoticeItemVo();
        item.setId(notice.getNoticeId());
        item.setTitle(notice.getNoticeTitle());
        item.setContent(notice.getNoticeContent());
        item.setDate(formatDate(notice.getCreateTime()));
        item.setTag("1".equals(notice.getNoticeType()) ? "通知" : "公告");
        item.setCreateTime(notice.getCreateTime());
        item.setSortOrder(0);
        return item;
    }

    private HomePageConfigVo.NoticeItemVo buildNoticeItem(HomeContentCardVo card) {
        Map<String, Object> cardConfig = parseConfig(card.getConfigJson());
        HomePageConfigVo.NoticeItemVo item = new HomePageConfigVo.NoticeItemVo();
        item.setId(card.getId());
        item.setTitle(card.getCardTitle());
        item.setContent(firstText(card.getCardContent(), card.getCardSubtitle(), ""));
        item.setDate(formatDate(card.getCreateTime()));
        item.setTag(firstText(card.getIcon(), "公告"));
        item.setCoverUrl(resolveOssUrl(card.getImageOssId(), card.getImageUrl(), null));
        item.setImage(item.getCoverUrl());
        item.setLink(card.getLinkUrl());
        item.setLinkText(card.getLinkText());
        item.setActionText(card.getLinkText());
        item.setLinkType(card.getLinkType());
        item.setConfig(cardConfig);
        item.setCustomClass(firstText(cardConfig.get("customClass")));
        item.setCustomCss(firstText(cardConfig.get("customCss")));
        item.setCustomMobileCss(firstText(cardConfig.get("customMobileCss"), cardConfig.get("mobileCss")));
        item.setCreateTime(card.getCreateTime());
        item.setSortOrder(card.getSortOrder() == null ? 0 : card.getSortOrder());
        return item;
    }

    private String formatDate(java.util.Date date) {
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    private String resolveOssUrl(Long ossId, String explicitUrl, String fallback) {
        if (StringUtils.isNotBlank(explicitUrl)) {
            return explicitUrl;
        }
        if (ossId != null) {
            SysOssVo oss = ossService.getById(ossId);
            if (oss != null && StringUtils.isNotBlank(oss.getUrl())) {
                return oss.getUrl();
            }
        }
        return fallback;
    }

    private List<HomePageModuleVo> defaultModules() {
        return List.of(
            module("home", "活动首页", "hero", "home", "DYZ · ART EXHIBITION", "", "", "", 10),
            module("guide", "上报指南", "guide", "guide", "SUBMISSION GUIDE", "", "", "", 20),
            module("category", "展演项目", "category", "category", "ART CATEGORIES", "", "", "", 30),
            module("stage", "展演阶段", "stage", "stage", "EXHIBITION STAGES", "", "", "", 40),
            module("notice", "通知公告", "notice", "notice", "NOTICE", "", "查看更多 →", "#notice", 50)
        );
    }

    private HomePageModuleVo module(String code, String title, String type, String anchor, String eyebrow,
                                    String intro, String moreText, String moreLink, Integer sortOrder) {
        HomePageModuleVo module = new HomePageModuleVo();
        module.setModuleCode(code);
        module.setModuleTitle(title);
        module.setModuleType(type);
        module.setAnchor(anchor);
        module.setEyebrow(eyebrow);
        module.setIntro(intro);
        module.setMoreText(moreText);
        module.setMoreLink(moreLink);
        module.setNavEnabled(true);
        module.setEnabled(true);
        module.setSortOrder(sortOrder);
        module.setLayoutType("default");
        module.setTemplateCode(type);
        module.setConfigJson("{}");
        return module;
    }

    private List<HomeContentCardVo> cardsForModule(String moduleCode, Map<String, List<HomeContentCardVo>> groupedCards, boolean useDefaultCards) {
        List<HomeContentCardVo> cards = groupedCards.get(moduleCode);
        if (CollUtil.isNotEmpty(cards)) {
            if ("home".equals(moduleCode) && cards.stream().noneMatch(this::isHeroCard)) {
                List<HomeContentCardVo> merged = new ArrayList<>();
                merged.add(defaultHeroCard());
                merged.addAll(cards);
                return merged;
            }
            return cards;
        }
        return useDefaultCards ? defaultCards(moduleCode) : List.of();
    }

    private boolean isHeroCard(HomeContentCardVo card) {
        if (card == null) {
            return false;
        }
        return "hero".equals(card.getCardType()) || "hero".equals(card.getTemplateCode()) || "home_hero".equals(card.getCardCode());
    }

    private List<HomeContentCardVo> defaultCards(String moduleCode) {
        return switch (moduleCode) {
            case "home" -> List.of(defaultHeroCard());
            case "guide" -> List.of(
                card("guide", "guide_workshop", "guide", "艺术实践工作坊", "上传工作坊视频和项目介绍文档。", "", "操作指南", "#guide", false, 10),
                card("guide", "guide_reform_result", "guide", "美育改革创新优秀成果", "填写标题、简介、作者信息并上传正文。", "", "上报说明", "#guide", true, 20),
                card("guide", "guide_performance", "guide", "艺术表演类", "声乐、器乐、舞蹈、戏剧、朗诵及个人项目。", "", "操作指南", "#guide", false, 30),
                card("guide", "guide_artwork", "guide", "艺术作品类", "美术、设计、大艺展设计及影像作品报送。", "", "上报说明", "#guide", false, 40),
                card("guide", "guide_principal_calligraphy", "guide", "高校校长书画作品", "校长书画作品作者信息直接在表单中填写。", "", "操作指南", "#guide", false, 50)
            );
            case "category" -> List.of(
                card("category", "category_vocal", "category", "声乐", "合唱 / 独唱", "声", "", "#category", false, 10),
                card("category", "category_instrumental", "category", "器乐", "民乐 / 西乐", "器", "", "#category", false, 20),
                card("category", "category_dance", "category", "舞蹈", "群舞 / 独舞", "舞", "", "#category", false, 30),
                card("category", "category_drama", "category", "戏剧", "短剧 / 戏曲", "戏", "", "#category", false, 40),
                card("category", "category_recitation", "category", "朗诵", "朗诵 / 语言", "诵", "", "#category", false, 50),
                card("category", "category_workshop", "category", "艺术实践工作坊", "实践 / 展示", "坊", "", "#category", false, 60)
            );
            case "stage" -> List.of();
            case "notice" -> List.of(card("notice", "notice_list", "notice", "通知公告列表", "", "", "", "#notice", false, 10));
            default -> List.of();
        };
    }

    private HomeContentCardVo defaultHeroCard() {
        HomeContentCardVo card = card("home", "home_hero", "hero", "向美而行\n逐梦未来", "", "", "", "#home", false, 10);
        card.setCardSubtitle("河南省第八届大学生艺术展演");
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("kicker", "DYZ · ART EXHIBITION");
        config.put("sloganLines", List.of("向美而行", "逐梦未来"));
        config.put("dynamicLabel", "ART");
        config.put("dynamicWord", "艺术展演");
        config.put("buttons", List.of(
            buttonMap("登录入口", "/login", "primary"),
            buttonMap("上报指南", "#guide", "secondary")
        ));
        config.put("particle", defaultParticleConfig());
        card.setConfigJson(JsonUtils.toJsonString(config));
        return card;
    }

    private HomeContentCardVo card(String sectionKey, String code, String type, String title, String content, String icon,
                                   String linkText, String linkUrl, boolean highlight, Integer sortOrder) {
        HomeContentCardVo card = new HomeContentCardVo();
        card.setSectionKey(sectionKey);
        card.setCardCode(code);
        card.setCardType(type);
        card.setTemplateCode(type);
        card.setLayoutType("default");
        card.setCardTitle(title);
        card.setCardContent(content);
        card.setIcon(icon);
        card.setLinkText(linkText);
        card.setLinkUrl(linkUrl);
        card.setLinkType("anchor");
        card.setHighlight(highlight);
        card.setEnabled(true);
        card.setSortOrder(sortOrder);
        card.setConfigJson("{}");
        return card;
    }

    private List<HomePageConfigVo.ButtonVo> defaultHeroButtons() {
        HomePageConfigVo.ButtonVo login = new HomePageConfigVo.ButtonVo();
        login.setText("登录入口");
        login.setLink("/login");
        login.setStyle("primary");
        login.setEnabled(true);
        login.setShowDesktop(true);
        login.setShowMobile(true);
        login.setSortOrder(10);
        HomePageConfigVo.ButtonVo guide = new HomePageConfigVo.ButtonVo();
        guide.setText("上报指南");
        guide.setLink("#guide");
        guide.setStyle("secondary");
        guide.setEnabled(true);
        guide.setShowDesktop(true);
        guide.setShowMobile(true);
        guide.setSortOrder(20);
        return List.of(login, guide);
    }

    private Map<String, Object> defaultParticleConfig() {
        Map<String, Object> particle = new LinkedHashMap<>();
        particle.put("blueColor", "rgba(0, 143, 216, 0.56)");
        particle.put("accentColor", "rgba(222, 114, 95, 0.32)");
        particle.put("floatBlueColor", "rgba(0,118,188,");
        particle.put("floatGreenColor", "rgba(34,185,154,");
        particle.put("density", 1);
        particle.put("fontScale", 1);
        particle.put("xRatio", 0.72);
        particle.put("yRatio", 0.48);
        return particle;
    }

    private Map<String, Object> buttonMap(String text, String link, String style) {
        Map<String, Object> button = new LinkedHashMap<>();
        button.put("text", text);
        button.put("link", link);
        button.put("style", style);
        button.put("enabled", true);
        button.put("showDesktop", true);
        button.put("showMobile", true);
        return button;
    }

    private Map<String, Object> parseConfig(String text) {
        try {
            Map<String, Object> parsed = JsonUtils.parseObject(text, new TypeReference<Map<String, Object>>() {
            });
            return parsed == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parsed);
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    private <T> List<T> parseList(String text, Class<T> clazz) {
        try {
            return JsonUtils.parseArray(text, clazz);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private <T> List<T> enabledAndSorted(List<T> items) {
        return items.stream()
            .filter(this::isEnabled)
            .sorted(Comparator.comparingInt(this::sortOrder))
            .toList();
    }

    private List<HomePageModuleVo> enabledAndSortedModules(List<HomePageModuleVo> modules) {
        return modules.stream()
            .filter(module -> module.getEnabled() == null || module.getEnabled())
            .sorted(Comparator.comparingInt(module -> module.getSortOrder() == null ? 0 : module.getSortOrder()))
            .toList();
    }

    private boolean isEnabled(Object item) {
        if (item instanceof HomePageConfigVo.ButtonVo button) {
            return button.getEnabled() == null || button.getEnabled();
        }
        return true;
    }

    private int sortOrder(Object item) {
        if (item instanceof HomePageConfigVo.ButtonVo button && button.getSortOrder() != null) {
            return button.getSortOrder();
        }
        return 0;
    }

    private HomePageModuleVo moduleByCode(List<HomePageModuleVo> modules, String code) {
        return modules.stream().filter(module -> code.equals(module.getModuleCode())).findFirst().orElse(null);
    }

    private String normalizeCode(String value) {
        String normalized = StringUtils.isBlank(value) ? "module_" + System.currentTimeMillis() : value.trim();
        normalized = normalized.replaceAll("[^A-Za-z0-9_-]", "_");
        return StringUtils.isBlank(normalized) ? "module_" + System.currentTimeMillis() : normalized;
    }

    private String normalizeAnchor(String value) {
        String anchor = StringUtils.isBlank(value) ? "home" : value.trim().replaceFirst("^#", "");
        anchor = anchor.replaceAll("[^A-Za-z0-9_-]", "_");
        return StringUtils.isBlank(anchor) ? "home" : anchor;
    }

    private String firstText(Object... values) {
        Object value = firstObject(values);
        return value == null ? "" : String.valueOf(value);
    }

    private Object firstObject(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof String text && StringUtils.isBlank(text)) {
                continue;
            }
            return value;
        }
        return null;
    }

    private List<String> splitLines(String text) {
        if (StringUtils.isBlank(text)) {
            return new ArrayList<>();
        }
        return Arrays.stream(text.split("\\r?\\n"))
            .map(String::trim)
            .filter(item -> StringUtils.isNotBlank(item))
            .toList();
    }

    private List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(item -> StringUtils.isNotBlank(item)).toList();
        }
        if (value instanceof String text) {
            return splitLines(text);
        }
        return new ArrayList<>();
    }

    private List<HomePageConfigVo.ButtonVo> buttonList(Object value) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<HomePageConfigVo.ButtonVo> buttons = new ArrayList<>();
        int index = 0;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            HomePageConfigVo.ButtonVo button = new HomePageConfigVo.ButtonVo();
            button.setText(firstText(map.get("text")));
            button.setLink(firstText(map.get("link"), "#"));
            button.setStyle(firstText(map.get("style"), "secondary"));
            button.setEnabled(booleanValue(map.get("enabled"), true));
            button.setShowDesktop(booleanValue(firstObject(map.get("showDesktop"), map.get("desktopVisible")), true));
            button.setShowMobile(booleanValue(firstObject(map.get("showMobile"), map.get("mobileVisible")), true));
            Integer sortOrder = intValue(map.get("sortOrder"));
            button.setSortOrder(sortOrder == null ? (index + 1) * 10 : sortOrder);
            if (StringUtils.isNotBlank(button.getText())) {
                buttons.add(button);
            }
            index++;
        }
        return enabledAndSorted(buttons);
    }

    private List<HomePageConfigVo.NavItemVo> navItemList(Object value) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<HomePageConfigVo.NavItemVo> navItems = new ArrayList<>();
        int index = 0;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            HomePageConfigVo.NavItemVo nav = new HomePageConfigVo.NavItemVo();
            nav.setText(firstText(map.get("text")));
            nav.setLink(firstText(map.get("link"), "#home"));
            nav.setLinkType(firstText(map.get("linkType"), map.get("openMode"), "current"));
            nav.setEnabled(booleanValue(map.get("enabled"), true));
            nav.setShowDesktop(booleanValue(firstObject(map.get("showDesktop"), map.get("desktopVisible")), true));
            nav.setShowMobile(booleanValue(firstObject(map.get("showMobile"), map.get("mobileVisible")), true));
            Integer sortOrder = intValue(map.get("sortOrder"));
            nav.setSortOrder(sortOrder == null ? (index + 1) * 10 : sortOrder);
            nav.setPopupWidth(intValue(map.get("popupWidth")));
            nav.setPopupHeight(intValue(map.get("popupHeight")));
            if (StringUtils.isNotBlank(nav.getText()) && StringUtils.isNotBlank(nav.getLink()) && (nav.getEnabled() == null || nav.getEnabled())) {
                navItems.add(nav);
            }
            index++;
        }
        return navItems.stream()
            .sorted(Comparator.comparing(item -> item.getSortOrder() == null ? 0 : item.getSortOrder()))
            .toList();
    }

    private Map<String, Object> normalizeMap(Map<?, ?> input) {
        Map<String, Object> result = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value);
            }
        });
        return result;
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            String normalized = text.trim();
            int commaIndex = normalized.indexOf(',');
            if (commaIndex >= 0) {
                normalized = normalized.substring(0, commaIndex).trim();
            }
            try {
                return Long.parseLong(normalized);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Boolean booleanValue(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && StringUtils.isNotBlank(text)) {
            return Boolean.parseBoolean(text);
        }
        return fallback;
    }
}
