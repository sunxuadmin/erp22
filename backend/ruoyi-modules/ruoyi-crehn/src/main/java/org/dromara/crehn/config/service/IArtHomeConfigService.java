package org.dromara.crehn.config.service;

import org.dromara.crehn.domain.HomeContentCard;
import org.dromara.crehn.domain.HomePageModule;
import org.dromara.crehn.domain.HomeSiteConfig;
import org.dromara.crehn.domain.vo.HomeContentCardVo;
import org.dromara.crehn.domain.vo.HomeLocalPdfUploadVo;
import org.dromara.crehn.domain.vo.HomePageConfigVo;
import org.dromara.crehn.domain.vo.HomePageModuleVo;
import org.dromara.crehn.domain.vo.HomeSiteConfigVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface IArtHomeConfigService {
    HomePageConfigVo getPublicConfig();

    HomeSiteConfigVo getSiteConfig();

    int saveSiteConfig(HomeSiteConfig config);

    List<HomePageModuleVo> listModules(Boolean enabled);

    int saveModule(HomePageModule module);

    int sortModules(List<HomePageModule> modules);

    int deleteModules(Long[] ids);

    TableDataInfo<HomeContentCardVo> queryCardPage(HomeContentCard query, PageQuery pageQuery);

    List<HomeContentCardVo> listCards(String sectionKey, Boolean enabled);

    HomeContentCardVo getPublicCard(Long id);

    HomeLocalPdfUploadVo uploadLocalPdf(MultipartFile file);

    Path resolveLocalPdfPath(String fileKey);

    int saveCard(HomeContentCard card);

    int sortCards(List<HomeContentCard> cards);

    int deleteCards(Long[] ids);
}
