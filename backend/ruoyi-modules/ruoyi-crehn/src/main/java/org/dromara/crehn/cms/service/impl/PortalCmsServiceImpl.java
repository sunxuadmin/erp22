package org.dromara.crehn.cms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.crehn.cms.service.IPortalCmsService;
import org.dromara.crehn.domain.PortalArticle;
import org.dromara.crehn.domain.PortalArticleVersion;
import org.dromara.crehn.domain.PortalChannel;
import org.dromara.crehn.domain.PortalHomeComponent;
import org.dromara.crehn.domain.PortalMediaAsset;
import org.dromara.crehn.domain.PortalPageLayout;
import org.dromara.crehn.domain.PortalRelease;
import org.dromara.crehn.domain.PortalSite;
import org.dromara.crehn.domain.bo.PortalArticleActionBo;
import org.dromara.crehn.mapper.PortalArticleMapper;
import org.dromara.crehn.mapper.PortalArticleVersionMapper;
import org.dromara.crehn.mapper.PortalChannelMapper;
import org.dromara.crehn.mapper.PortalHomeComponentMapper;
import org.dromara.crehn.mapper.PortalMediaAssetMapper;
import org.dromara.crehn.mapper.PortalPageLayoutMapper;
import org.dromara.crehn.mapper.PortalReleaseMapper;
import org.dromara.crehn.mapper.PortalSiteMapper;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.service.ISysOssService;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortalCmsServiceImpl implements IPortalCmsService {
    private static final Set<String> ARTICLE_VISIBILITY = Set.of("public", "login", "school");
    private static final Set<String> SITE_STATUS = Set.of("enabled", "disabled");

    private final PortalArticleMapper articleMapper;
    private final PortalArticleVersionMapper versionMapper;
    private final PortalSiteMapper siteMapper;
    private final PortalChannelMapper channelMapper;
    private final PortalMediaAssetMapper mediaMapper;
    private final PortalHomeComponentMapper componentMapper;
    private final PortalPageLayoutMapper pageLayoutMapper;
    private final PortalReleaseMapper releaseMapper;
    private final ISysOssService ossService;

    @Override
    public List<PortalSite> listSites() {
        return siteMapper.selectList(Wrappers.lambdaQuery(PortalSite.class)
            .orderByAsc(PortalSite::getSiteName));
    }

    @Override
    public PortalSite saveSite(PortalSite site) {
        if (site == null || StringUtils.isBlank(site.getSiteCode()) || StringUtils.isBlank(site.getSiteName())) {
            throw new ServiceException("站点编码和站点名称不能为空");
        }
        site.setSiteCode(site.getSiteCode().trim());
        site.setSiteName(site.getSiteName().trim());
        site.setStatus(StringUtils.blankToDefault(site.getStatus(), "enabled"));
        if (!SITE_STATUS.contains(site.getStatus())) {
            throw new ServiceException("站点状态不正确");
        }
        if (site.getId() == null) {
            long duplicate = siteMapper.selectCount(Wrappers.lambdaQuery(PortalSite.class)
                .eq(PortalSite::getSiteCode, site.getSiteCode()));
            if (duplicate > 0) {
                throw new ServiceException("站点编码已存在");
            }
            siteMapper.insert(site);
        } else {
            PortalSite existing = requireExistingSite(site.getId());
            long duplicate = siteMapper.selectCount(Wrappers.lambdaQuery(PortalSite.class)
                .eq(PortalSite::getSiteCode, site.getSiteCode())
                .ne(PortalSite::getId, existing.getId()));
            if (duplicate > 0) {
                throw new ServiceException("站点编码已存在");
            }
            siteMapper.updateById(site);
        }
        return site;
    }

    @Override
    public List<PortalChannel> listChannels(Long siteId) {
        requireSite(siteId);
        return channelMapper.selectList(Wrappers.lambdaQuery(PortalChannel.class)
            .eq(PortalChannel::getSiteId, siteId)
            .orderByAsc(PortalChannel::getSortOrder)
            .orderByAsc(PortalChannel::getId));
    }

    @Override
    public PortalChannel saveChannel(PortalChannel channel) {
        if (channel == null || channel.getSiteId() == null || StringUtils.isBlank(channel.getChannelCode())
            || StringUtils.isBlank(channel.getChannelName())) {
            throw new ServiceException("站点、栏目编码和栏目名称不能为空");
        }
        requireSite(channel.getSiteId());
        channel.setParentId(channel.getParentId() == null ? 0L : channel.getParentId());
        channel.setNavigationPosition(StringUtils.blankToDefault(channel.getNavigationPosition(), "main"));
        channel.setVisibility(StringUtils.blankToDefault(channel.getVisibility(), "public"));
        channel.setStatus(StringUtils.blankToDefault(channel.getStatus(), "enabled"));
        channel.setSortOrder(channel.getSortOrder() == null ? 0 : channel.getSortOrder());
        if (channel.getParentId() != 0L) {
            PortalChannel parent = channelMapper.selectById(channel.getParentId());
            if (parent == null || !channel.getSiteId().equals(parent.getSiteId())) {
                throw new ServiceException("上级栏目不属于当前站点");
            }
        }
        if (channel.getId() == null) {
            channelMapper.insert(channel);
        } else {
            PortalChannel existing = channelMapper.selectById(channel.getId());
            if (existing == null || !existing.getSiteId().equals(channel.getSiteId())) {
                throw new ServiceException("栏目不存在或不属于当前站点");
            }
            channelMapper.updateById(channel);
        }
        return channel;
    }

    @Override
    public List<PortalMediaAsset> listMedia(Long siteId) {
        requireSite(siteId);
        return mediaMapper.selectList(Wrappers.lambdaQuery(PortalMediaAsset.class)
            .eq(PortalMediaAsset::getSiteId, siteId)
            .orderByDesc(PortalMediaAsset::getCreateTime));
    }

    @Override
    public PortalMediaAsset saveMedia(PortalMediaAsset media) {
        if (media == null || media.getSiteId() == null || media.getOssId() == null
            || StringUtils.isBlank(media.getMediaType())) {
            throw new ServiceException("站点、OSS文件和媒体类型不能为空");
        }
        requireSite(media.getSiteId());
        if (media.getId() == null) {
            SysOssVo oss = ossService.getById(media.getOssId());
            if (oss == null || StringUtils.isBlank(oss.getFileName())) {
                throw new ServiceException("OSS文件不存在");
            }
            media.setContentSha256(hashOssFile(oss.getFileName()));
            media.setScanStatus("pending");
            media.setStatus("draft");
            mediaMapper.insert(media);
        } else {
            PortalMediaAsset existing = mediaMapper.selectById(media.getId());
            if (existing == null || !existing.getSiteId().equals(media.getSiteId())) {
                throw new ServiceException("媒体不存在或不属于当前站点");
            }
            media.setScanStatus(existing.getScanStatus());
            media.setStatus(existing.getStatus());
            mediaMapper.updateById(media);
        }
        return media;
    }

    @Override
    public void approveMedia(Long mediaId, String reason) {
        if (StringUtils.isBlank(reason)) {
            throw new ServiceException("媒体人工复核原因不能为空");
        }
        PortalMediaAsset media = mediaMapper.selectById(mediaId);
        if (media == null) {
            throw new ServiceException("媒体不存在");
        }
        if (!"pending".equals(media.getScanStatus()) && !"rejected".equals(media.getScanStatus())) {
            throw new ServiceException("只有待复核或已驳回媒体可以重新复核");
        }
        media.setScanStatus("approved");
        media.setStatus("active");
        mediaMapper.updateById(media);
    }

    @Override
    public List<PortalHomeComponent> listHomeComponents(Long siteId) {
        requireSite(siteId);
        return componentMapper.selectList(Wrappers.lambdaQuery(PortalHomeComponent.class)
            .eq(PortalHomeComponent::getSiteId, siteId)
            .orderByAsc(PortalHomeComponent::getSortOrder)
            .orderByAsc(PortalHomeComponent::getId));
    }

    @Override
    public PortalHomeComponent saveHomeComponent(PortalHomeComponent component) {
        if (component == null || component.getSiteId() == null || StringUtils.isBlank(component.getComponentKey())) {
            throw new ServiceException("站点和组件标识不能为空");
        }
        requireSite(component.getSiteId());
        PortalHomeConfigurationValidator.normalizeComponent(component, component.getSiteId());
        if (component.getId() == null) {
            componentMapper.insert(component);
        } else {
            PortalHomeComponent existing = componentMapper.selectById(component.getId());
            if (existing == null || !existing.getSiteId().equals(component.getSiteId())) {
                throw new ServiceException("首页组件不存在或不属于当前站点");
            }
            componentMapper.updateById(component);
        }
        return component;
    }

    @Override
    public List<PortalPageLayout> listPageLayouts(Long siteId, String pageCode) {
        requireSite(siteId);
        String normalizedPageCode = StringUtils.blankToDefault(pageCode, "home");
        return pageLayoutMapper.selectList(Wrappers.lambdaQuery(PortalPageLayout.class)
            .eq(PortalPageLayout::getSiteId, siteId)
            .eq(PortalPageLayout::getPageCode, normalizedPageCode)
            .orderByAsc(PortalPageLayout::getSortOrder)
            .orderByAsc(PortalPageLayout::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalPageLayout savePageLayout(PortalPageLayout layout) {
        if (layout == null || layout.getSiteId() == null || StringUtils.isBlank(layout.getLayoutCode())
            || StringUtils.isBlank(layout.getLayoutName())) {
            throw new ServiceException("站点、布局编码和布局名称不能为空");
        }
        requireSite(layout.getSiteId());
        layout.setPageCode(StringUtils.blankToDefault(layout.getPageCode(), "home"));
        layout.setLayoutCode(layout.getLayoutCode().trim());
        layout.setLayoutName(layout.getLayoutName().trim());
        layout.setRenderVersion(PortalHomeConfigurationValidator.normalizeRenderVersion(layout.getRenderVersion()));
        layout.setSortOrder(layout.getSortOrder() == null ? 0 : layout.getSortOrder());
        layout.setEnabled(layout.getEnabled() == null || layout.getEnabled());
        boolean activateRequested = Boolean.TRUE.equals(layout.getActive());
        layout.setActive(false);
        if (!layout.getLayoutCode().matches("[A-Za-z0-9_-]{1,64}")) {
            throw new ServiceException("布局编码只能包含字母、数字、下划线和短横线");
        }
        PortalHomeConfigurationValidator.validateThemeJson(layout.getThemeJson());
        if (StringUtils.isBlank(layout.getComponentJson())) {
            layout.setComponentJson("[]");
        }
        layout.setComponentJson(PortalHomeConfigurationValidator.normalizeComponentSnapshotJson(
            layout.getComponentJson(), layout.getSiteId()
        ));
        if (layout.getId() == null) {
            long duplicate = pageLayoutMapper.selectCount(Wrappers.lambdaQuery(PortalPageLayout.class)
                .eq(PortalPageLayout::getSiteId, layout.getSiteId())
                .eq(PortalPageLayout::getPageCode, layout.getPageCode())
                .eq(PortalPageLayout::getLayoutCode, layout.getLayoutCode()));
            if (duplicate > 0) {
                throw new ServiceException("布局编码已存在");
            }
            pageLayoutMapper.insert(layout);
        } else {
            PortalPageLayout existing = pageLayoutMapper.selectById(layout.getId());
            if (existing == null || !existing.getSiteId().equals(layout.getSiteId())) {
                throw new ServiceException("布局不存在或不属于当前站点");
            }
            long duplicate = pageLayoutMapper.selectCount(Wrappers.lambdaQuery(PortalPageLayout.class)
                .eq(PortalPageLayout::getSiteId, layout.getSiteId())
                .eq(PortalPageLayout::getPageCode, layout.getPageCode())
                .eq(PortalPageLayout::getLayoutCode, layout.getLayoutCode())
                .ne(PortalPageLayout::getId, layout.getId()));
            if (duplicate > 0) {
                throw new ServiceException("布局编码已存在");
            }
            layout.setActive(existing.getActive());
            pageLayoutMapper.updateById(layout);
        }
        if (activateRequested) {
            activatePageLayout(layout.getSiteId(), layout.getPageCode(), layout.getLayoutCode());
            layout.setActive(true);
        }
        return layout;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activatePageLayout(Long siteId, String pageCode, String layoutCode) {
        requireSite(siteId);
        String normalizedPageCode = StringUtils.blankToDefault(pageCode, "home");
        PortalPageLayout target = pageLayoutMapper.selectOne(Wrappers.lambdaQuery(PortalPageLayout.class)
            .eq(PortalPageLayout::getSiteId, siteId)
            .eq(PortalPageLayout::getPageCode, normalizedPageCode)
            .eq(PortalPageLayout::getLayoutCode, layoutCode)
            .last("limit 1"));
        if (target == null || !Boolean.TRUE.equals(target.getEnabled())) {
            throw new ServiceException("布局不存在或未启用");
        }
        pageLayoutMapper.update(null, Wrappers.lambdaUpdate(PortalPageLayout.class)
            .eq(PortalPageLayout::getSiteId, siteId)
            .eq(PortalPageLayout::getPageCode, normalizedPageCode)
            .set(PortalPageLayout::getActive, false));
        target.setActive(true);
        pageLayoutMapper.updateById(target);
    }

    @Override
    public TableDataInfo<PortalArticle> queryArticles(PortalArticle query, PageQuery pageQuery) {
        Page<PortalArticle> page = articleMapper.selectPage(pageQuery.build(), Wrappers.lambdaQuery(PortalArticle.class)
            .eq(query.getSiteId() != null, PortalArticle::getSiteId, query.getSiteId())
            .eq(query.getChannelId() != null, PortalArticle::getChannelId, query.getChannelId())
            .eq(query.getActivityId() != null, PortalArticle::getActivityId, query.getActivityId())
            .eq(StringUtils.isNotBlank(query.getStatus()), PortalArticle::getStatus, query.getStatus())
            .like(StringUtils.isNotBlank(query.getTitle()), PortalArticle::getTitle, query.getTitle())
            .orderByDesc(PortalArticle::getCreateTime));
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalArticle saveDraft(PortalArticle article) {
        validateDraft(article);
        if (article.getId() == null) {
            article.setArticleCode(StringUtils.blankToDefault(article.getArticleCode(), "A" + System.currentTimeMillis()));
            article.setStatus("DRAFT");
            article.setRowVersion(1L);
            articleMapper.insert(article);
            return article;
        }
        PortalArticle existing = requireArticle(article.getId());
        if (!Set.of("DRAFT", "OFFLINE").contains(existing.getStatus())) {
            throw new ServiceException("只有草稿或已下线文章可以修改");
        }
        Long expectedVersion = article.getRowVersion();
        if (expectedVersion == null || !expectedVersion.equals(existing.getRowVersion())) {
            throw new ServiceException("文章已被其他用户修改，请刷新后重试");
        }
        article.setStatus("DRAFT");
        article.setRowVersion(expectedVersion + 1);
        articleMapper.updateById(article);
        return article;
    }

    @Override
    public void submitReview(Long articleId) {
        PortalArticle article = requireArticle(articleId);
        if (!"DRAFT".equals(article.getStatus())) {
            throw new ServiceException("只有草稿可以提交审核");
        }
        article.setStatus("IN_REVIEW");
        article.setRowVersion(article.getRowVersion() + 1);
        articleMapper.updateById(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalArticle publish(PortalArticleActionBo bo) {
        PortalArticle article = requireArticle(bo.getArticleId());
        if (!"IN_REVIEW".equals(article.getStatus())) {
            throw new ServiceException("只有审核中的文章可以发布或定时发布");
        }
        requireApprovedCover(article.getCoverMediaId());
        Date now = new Date();
        boolean scheduled = bo.getScheduledAt() != null && bo.getScheduledAt().after(now);
        article.setStatus(scheduled ? "SCHEDULED" : "PUBLISHED");
        article.setScheduledAt(scheduled ? bo.getScheduledAt() : null);
        article.setPublishedAt(scheduled ? null : now);
        PortalArticleVersion version = createArticleVersion(article, null, scheduled ? bo.getScheduledAt() : now);
        article.setCurrentVersionId(version.getId());
        article.setRowVersion(article.getRowVersion() + 1);
        articleMapper.updateById(article);
        return article;
    }

    @Override
    public void offline(PortalArticleActionBo bo) {
        PortalArticle article = requireArticle(bo.getArticleId());
        if (!Set.of("PUBLISHED", "SCHEDULED").contains(article.getStatus())) {
            throw new ServiceException("只有已发布或定时发布文章可以下线");
        }
        article.setStatus("OFFLINE");
        article.setScheduledAt(null);
        article.setRowVersion(article.getRowVersion() + 1);
        articleMapper.updateById(article);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalArticle rollback(PortalArticleActionBo bo) {
        if (bo.getSourceVersionId() == null) {
            throw new ServiceException("回滚来源版本不能为空");
        }
        PortalArticle article = requireArticle(bo.getArticleId());
        PortalArticleVersion source = versionMapper.selectById(bo.getSourceVersionId());
        if (source == null || !article.getId().equals(source.getArticleId())) {
            throw new ServiceException("回滚版本不存在");
        }
        PortalArticle snapshot = JsonUtils.parseObject(source.getSnapshotJson(), PortalArticle.class);
        if (snapshot == null) {
            throw new ServiceException("历史版本快照无效");
        }
        snapshot.setId(article.getId());
        snapshot.setStatus("PUBLISHED");
        snapshot.setPublishedAt(new Date());
        snapshot.setScheduledAt(null);
        snapshot.setRowVersion(article.getRowVersion() + 1);
        PortalArticleVersion newVersion = createArticleVersion(snapshot, source.getId(), snapshot.getPublishedAt());
        snapshot.setCurrentVersionId(newVersion.getId());
        articleMapper.updateById(snapshot);
        return snapshot;
    }

    @Override
    public List<PortalArticle> publicArticles(Long siteId, Long channelId) {
        return articleMapper.selectList(Wrappers.lambdaQuery(PortalArticle.class)
            .eq(PortalArticle::getSiteId, siteId)
            .eq(channelId != null, PortalArticle::getChannelId, channelId)
            .eq(PortalArticle::getStatus, "PUBLISHED")
            .eq(PortalArticle::getVisibility, "public")
            .le(PortalArticle::getPublishedAt, new Date())
            .orderByDesc(PortalArticle::getPinned)
            .orderByDesc(PortalArticle::getPublishedAt));
    }

    @Override
    public PortalArticle publicArticle(String articleCode) {
        PortalArticle article = articleMapper.selectOne(Wrappers.lambdaQuery(PortalArticle.class)
            .eq(PortalArticle::getArticleCode, articleCode)
            .eq(PortalArticle::getStatus, "PUBLISHED")
            .eq(PortalArticle::getVisibility, "public")
            .le(PortalArticle::getPublishedAt, new Date())
            .last("limit 1"));
        if (article == null) {
            throw new ServiceException("文章不存在或未发布");
        }
        return article;
    }

    @Override
    public PortalSite publicSite(String siteCode) {
        if (StringUtils.isBlank(siteCode)) {
            throw new ServiceException("站点编码不能为空");
        }
        PortalSite site = siteMapper.selectOne(Wrappers.lambdaQuery(PortalSite.class)
            .eq(PortalSite::getSiteCode, siteCode.trim())
            .eq(PortalSite::getStatus, "enabled")
            .last("limit 1"));
        if (site == null) {
            throw new ServiceException("门户站点不存在或未启用");
        }
        return site;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalRelease publishHome(Long siteId, String reason) {
        return publishHome(siteId, null, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortalRelease publishHome(Long siteId, String layoutCode, String reason) {
        requireSite(siteId);
        if (StringUtils.isBlank(reason)) {
            throw new ServiceException("首页发布原因不能为空");
        }
        PortalPageLayout selectedLayout = StringUtils.isBlank(layoutCode)
            ? pageLayoutMapper.selectOne(Wrappers.lambdaQuery(PortalPageLayout.class)
                .eq(PortalPageLayout::getSiteId, siteId)
                .eq(PortalPageLayout::getPageCode, "home")
                .eq(PortalPageLayout::getActive, true)
                .eq(PortalPageLayout::getEnabled, true)
                .orderByAsc(PortalPageLayout::getSortOrder)
                .orderByAsc(PortalPageLayout::getId)
                .last("limit 1"))
            : pageLayoutMapper.selectOne(Wrappers.lambdaQuery(PortalPageLayout.class)
                .eq(PortalPageLayout::getSiteId, siteId)
                .eq(PortalPageLayout::getPageCode, "home")
                .eq(PortalPageLayout::getLayoutCode, layoutCode)
                .eq(PortalPageLayout::getEnabled, true)
                .last("limit 1"));
        if (StringUtils.isNotBlank(layoutCode) && selectedLayout == null) {
            throw new ServiceException("指定的首页布局不存在或未启用");
        }
        List<PortalHomeComponent> components;
        String themeJson = null;
        String renderVersion = "v1";
        String selectedLayoutCode = null;
        if (selectedLayout == null) {
            components = componentMapper.selectList(Wrappers.lambdaQuery(PortalHomeComponent.class)
                .eq(PortalHomeComponent::getSiteId, siteId)
                .eq(PortalHomeComponent::getEnabled, true)
                .orderByAsc(PortalHomeComponent::getSortOrder));
            components.forEach(component -> PortalHomeConfigurationValidator.normalizeComponent(component, siteId));
        } else {
            components = PortalHomeConfigurationValidator.parseAndNormalizeComponentSnapshot(
                selectedLayout.getComponentJson(), siteId
            ).stream().filter(component -> Boolean.TRUE.equals(component.getEnabled())).toList();
            themeJson = selectedLayout.getThemeJson();
            PortalHomeConfigurationValidator.validateThemeJson(themeJson);
            renderVersion = PortalHomeConfigurationValidator.normalizeRenderVersion(selectedLayout.getRenderVersion());
            selectedLayoutCode = selectedLayout.getLayoutCode();
        }
        Map<String, Object> releaseSnapshot = new LinkedHashMap<>();
        releaseSnapshot.put("layoutCode", selectedLayoutCode);
        releaseSnapshot.put("renderVersion", renderVersion);
        releaseSnapshot.put("theme", StringUtils.isBlank(themeJson)
            ? Map.of()
            : JsonUtils.parseObject(themeJson, Map.class));
        releaseSnapshot.put("components", components);
        String snapshot = JsonUtils.toJsonString(releaseSnapshot);
        PortalRelease previousRelease = currentRelease(siteId);
        Integer latest = releaseMapper.selectList(Wrappers.lambdaQuery(PortalRelease.class)
            .eq(PortalRelease::getSiteId, siteId)
            .orderByDesc(PortalRelease::getVersionNo)
            .last("limit 1")).stream().findFirst().map(PortalRelease::getVersionNo).orElse(0);
        releaseMapper.update(null, Wrappers.lambdaUpdate(PortalRelease.class)
            .eq(PortalRelease::getSiteId, siteId)
            .eq(PortalRelease::getStatus, "PUBLISHED")
            .set(PortalRelease::getStatus, "SUPERSEDED"));
        PortalRelease release = new PortalRelease();
        release.setSiteId(siteId);
        release.setVersionNo(latest + 1);
        release.setSnapshotJson(snapshot);
        release.setChecksum(hash(snapshot));
        release.setStatus("PUBLISHED");
        release.setPublishedAt(new Date());
        release.setPublishedBy(LoginHelper.getUserId());
        release.setReleaseReason(StringUtils.substring(reason, 0, 500));
        release.setSourceReleaseId(previousRelease == null ? null : previousRelease.getId());
        releaseMapper.insert(release);
        return release;
    }

    @Override
    public PortalRelease currentRelease(Long siteId) {
        return releaseMapper.selectOne(Wrappers.lambdaQuery(PortalRelease.class)
            .eq(PortalRelease::getSiteId, siteId)
            .eq(PortalRelease::getStatus, "PUBLISHED")
            .orderByDesc(PortalRelease::getVersionNo)
            .last("limit 1"));
    }

    @Override
    public List<PortalHomeComponent> publicHomeComponents(Long siteId) {
        PortalRelease release = currentRelease(siteId);
        if (release == null) {
            return List.of();
        }
        if (JsonUtils.isJsonArray(release.getSnapshotJson())) {
            return JsonUtils.parseArray(release.getSnapshotJson(), PortalHomeComponent.class);
        }
        Map<String, Object> snapshot = JsonUtils.parseObject(release.getSnapshotJson(), Map.class);
        Object components = snapshot.get("components");
        return components == null
            ? List.of()
            : JsonUtils.parseArray(JsonUtils.toJsonString(components), PortalHomeComponent.class);
    }

    @Override
    public Map<String, Object> publicHomeSnapshot(Long siteId) {
        PortalRelease release = currentRelease(siteId);
        if (release == null || StringUtils.isBlank(release.getSnapshotJson())) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("layoutCode", null);
            empty.put("renderVersion", "v1");
            empty.put("theme", Map.of());
            empty.put("components", List.of());
            return empty;
        }
        if (JsonUtils.isJsonArray(release.getSnapshotJson())) {
            Map<String, Object> legacy = new LinkedHashMap<>();
            legacy.put("layoutCode", null);
            legacy.put("renderVersion", "v1");
            legacy.put("theme", Map.of());
            legacy.put("components", JsonUtils.parseArray(release.getSnapshotJson(), PortalHomeComponent.class));
            return legacy;
        }
        return JsonUtils.parseObject(release.getSnapshotJson(), Map.class);
    }

    @Scheduled(fixedDelayString = "${crehn.cms.publish-poll-ms:60000}")
    @Transactional(rollbackFor = Exception.class)
    public void publishDueArticles() {
        TenantHelper.ignore(() -> {
            Date now = new Date();
            List<PortalArticle> due = articleMapper.selectList(Wrappers.lambdaQuery(PortalArticle.class)
                .eq(PortalArticle::getStatus, "SCHEDULED")
                .le(PortalArticle::getScheduledAt, now));
            for (PortalArticle article : due) {
                article.setStatus("PUBLISHED");
                article.setPublishedAt(article.getScheduledAt());
                article.setScheduledAt(null);
                article.setRowVersion(article.getRowVersion() + 1);
                articleMapper.updateById(article);
            }
        });
    }

    private PortalArticle requireArticle(Long id) {
        PortalArticle article = articleMapper.selectById(id);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        return article;
    }

    private PortalSite requireSite(Long id) {
        PortalSite site = requireExistingSite(id);
        if (site == null || !"enabled".equals(site.getStatus())) {
            throw new ServiceException("门户站点不存在或未启用");
        }
        return site;
    }

    private PortalSite requireExistingSite(Long id) {
        PortalSite site = siteMapper.selectById(id);
        if (site == null) {
            throw new ServiceException("门户站点不存在");
        }
        return site;
    }

    private void requireApprovedCover(Long mediaId) {
        if (mediaId == null) {
            return;
        }
        PortalMediaAsset media = mediaMapper.selectById(mediaId);
        if (media == null || !"active".equals(media.getStatus()) || !"approved".equals(media.getScanStatus())) {
            throw new ServiceException("封面媒体尚未完成复核");
        }
    }

    private void validateDraft(PortalArticle article) {
        if (article == null || article.getSiteId() == null || article.getChannelId() == null
            || StringUtils.isBlank(article.getTitle())) {
            throw new ServiceException("站点、栏目和标题不能为空");
        }
        article.setVisibility(StringUtils.blankToDefault(article.getVisibility(), "public"));
        if (!ARTICLE_VISIBILITY.contains(article.getVisibility())) {
            throw new ServiceException("文章可见范围不正确");
        }
        if (article.getContentMarkdown() != null && article.getContentMarkdown().length() > 2_000_000) {
            throw new ServiceException("文章正文超过大小限制");
        }
    }

    private PortalArticleVersion createArticleVersion(PortalArticle article, Long sourceVersionId, Date publishedAt) {
        String snapshot = JsonUtils.toJsonString(article);
        Integer latest = versionMapper.selectList(Wrappers.lambdaQuery(PortalArticleVersion.class)
            .eq(PortalArticleVersion::getArticleId, article.getId())
            .orderByDesc(PortalArticleVersion::getVersionNo)
            .last("limit 1")).stream().findFirst().map(PortalArticleVersion::getVersionNo).orElse(0);
        PortalArticleVersion version = new PortalArticleVersion();
        version.setArticleId(article.getId());
        version.setVersionNo(latest + 1);
        version.setSnapshotJson(snapshot);
        version.setChecksum(hash(snapshot));
        version.setPublishedAt(publishedAt);
        version.setPublishedBy(LoginHelper.getUserId());
        version.setSourceVersionId(sourceVersionId);
        versionMapper.insert(version);
        return version;
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256不可用", e);
        }
    }

    private String hashOssFile(String objectKey) {
        Path tempFile = ossService.downloadToTemp(objectKey);
        try (InputStream input = Files.newInputStream(tempFile)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                digest.update(buffer, 0, count);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new ServiceException("媒体内容校验失败，请重新上传");
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn("门户媒体临时文件清理失败: {}", tempFile, e);
            }
        }
    }
}
