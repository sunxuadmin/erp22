package org.dromara.crehn.cms.service;

import org.dromara.crehn.domain.PortalArticle;
import org.dromara.crehn.domain.PortalChannel;
import org.dromara.crehn.domain.PortalHomeComponent;
import org.dromara.crehn.domain.PortalMediaAsset;
import org.dromara.crehn.domain.PortalRelease;
import org.dromara.crehn.domain.PortalSite;
import org.dromara.crehn.domain.bo.PortalArticleActionBo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

public interface IPortalCmsService {
    List<PortalSite> listSites();
    PortalSite saveSite(PortalSite site);
    List<PortalChannel> listChannels(Long siteId);
    PortalChannel saveChannel(PortalChannel channel);
    List<PortalMediaAsset> listMedia(Long siteId);
    PortalMediaAsset saveMedia(PortalMediaAsset media);
    void approveMedia(Long mediaId, String reason);
    List<PortalHomeComponent> listHomeComponents(Long siteId);
    PortalHomeComponent saveHomeComponent(PortalHomeComponent component);
    TableDataInfo<PortalArticle> queryArticles(PortalArticle query, PageQuery pageQuery);
    PortalArticle saveDraft(PortalArticle article);
    void submitReview(Long articleId);
    PortalArticle publish(PortalArticleActionBo bo);
    void offline(PortalArticleActionBo bo);
    PortalArticle rollback(PortalArticleActionBo bo);
    List<PortalArticle> publicArticles(Long siteId, Long channelId);
    PortalArticle publicArticle(String articleCode);
    PortalSite publicSite(String siteCode);
    PortalRelease publishHome(Long siteId, String reason);
    PortalRelease currentRelease(Long siteId);
    List<PortalHomeComponent> publicHomeComponents(Long siteId);
}
