package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class HomePageConfigVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private SiteVo site;
    private List<NavItemVo> nav = new ArrayList<>();
    private List<ButtonVo> headerButtons = new ArrayList<>();
    private LoginStateVo loginState;
    private List<ModuleVo> modules = new ArrayList<>();
    private HeroVo hero;
    private SectionVo guide;
    private SectionVo category;
    private SectionVo stage;
    private NoticeSectionVo notice;
    private FooterVo footer;

    @Data
    public static class SiteVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String brandName;
        private String logo;
        private String homeLink;
        private String customCss;
        private String customMobileCss;
        private String headerCss;
        private String headerMobileCss;
        private String homeLayoutMode;
        private Integer homeCanvasHeight;
        private String headerPlacement;
        private String footerPlacement;
        private Map<String, Object> headerConfig = new LinkedHashMap<>();
    }

    @Data
    public static class NavItemVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String text;
        private String link;
        private String linkType;
        private Boolean enabled;
        private Boolean showDesktop;
        private Boolean showMobile;
        private Integer sortOrder;
        private Integer popupWidth;
        private Integer popupHeight;
    }

    @Data
    public static class ButtonVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String text;
        private String link;
        private String style;
        private Boolean enabled;
        private Boolean showDesktop;
        private Boolean showMobile;
        private Integer sortOrder;
    }

    @Data
    public static class LoginStateVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String adminText;
        private String schoolText;
        private String expertText;
        private String defaultText;
    }

    @Data
    public static class SectionMetaVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String sectionKey;
        private String eyebrow;
        private String title;
        private String intro;
        private String moreText;
        private String moreLink;
    }

    @Data
    public static class HeroVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String moduleCode;
        private String anchor;
        private String kicker;
        private List<String> sloganLines = new ArrayList<>();
        private String subtitle;
        private String desc;
        private String image;
        private String dynamicLabel;
        private String dynamicWord;
        private String visualAnimation;
        private Boolean visualFollowMouse;
        private Integer visualTiltIntensity;
        private String backgroundType;
        private String backgroundImage;
        private String backgroundHtml;
        private List<ButtonVo> buttons = new ArrayList<>();
        private List<String> meta = new ArrayList<>();
        private Map<String, Object> particleConfig = new LinkedHashMap<>();
        private String customClass;
        private String customCss;
        private String customMobileCss;
        private Map<String, Object> elementCss = new LinkedHashMap<>();
    }

    @Data
    public static class ModuleVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String moduleCode;
        private String moduleType;
        private String anchor;
        private String eyebrow;
        private String title;
        private String intro;
        private String moreText;
        private String moreLink;
        private Boolean navEnabled;
        private Boolean enabled;
        private Integer sortOrder;
        private String layoutType;
        private String templateCode;
        private Map<String, Object> config = new LinkedHashMap<>();
        private String customClass;
        private String customCss;
        private String customMobileCss;
        private List<CardVo> cards = new ArrayList<>();
        private List<CardVo> items = new ArrayList<>();
        private List<NoticeItemVo> noticeItems = new ArrayList<>();
    }

    @Data
    public static class SectionVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String moduleCode;
        private String moduleType;
        private String anchor;
        private String eyebrow;
        private String title;
        private String intro;
        private String moreText;
        private String moreLink;
        private Map<String, Object> config = new LinkedHashMap<>();
        private String customClass;
        private String customCss;
        private String customMobileCss;
        private List<CardVo> cards = new ArrayList<>();
        private List<CardVo> items = new ArrayList<>();
    }

    @Data
    public static class NoticeSectionVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String eyebrow;
        private String title;
        private String intro;
        private String moreText;
        private String moreLink;
        private Boolean showPageHeader;
        private Boolean showPageEyebrow;
        private Boolean showPageTitle;
        private Boolean showPageIntro;
        private Boolean showPageMore;
        private Boolean hidePageHeaderMobile;
        private String pageHeaderAlign;
        private String pageHeaderSize;
        private String pageHeaderCss;
        private String pageEyebrowCss;
        private String pageTitleCss;
        private String pageIntroCss;
        private String pageMoreCss;
        private Boolean showMoreLink;
        private String noticeVariant;
        private Boolean showNoticeEyebrow;
        private Boolean showNoticeTitle;
        private String noticeTitleAlign;
        private String noticeEyebrowCss;
        private String noticeTitleCss;
        private String noticeMoreCss;
        private List<NoticeItemVo> items = new ArrayList<>();
    }

    @Data
    public static class CardVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String sectionKey;
        private String code;
        private String cardType;
        private String icon;
        private String title;
        private String subtitle;
        private String desc;
        private String content;
        private String image;
        private String linkText;
        private String actionText;
        private String link;
        private String linkType;
        private Boolean highlight;
        private String layoutType;
        private String templateCode;
        private String slotKey;
        private Map<String, Object> config = new LinkedHashMap<>();
        private String customClass;
        private String customCss;
        private String customMobileCss;
        private Integer sortOrder;
    }

    @Data
    public static class NoticeItemVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private Long id;
        private String title;
        private String content;
        private String date;
        private String tag;
        private String coverUrl;
        private String image;
        private String link;
        private String linkText;
        private String actionText;
        private String linkType;
        private Map<String, Object> config = new LinkedHashMap<>();
        private String customClass;
        private String customCss;
        private String customMobileCss;
        private Date createTime;
        private Integer sortOrder;
    }

    @Data
    public static class FooterVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String title;
        private String text;
        private String organizer;
        private String sponsor;
        private String technicalSupport;
        private String contactPhone;
        private String contactEmail;
        private String contactAddress;
        private String icpText;
        private String icpLink;
        private String policeText;
        private String policeLink;
        private String customCss;
        private String customMobileCss;
        private Map<String, Object> displayConfig = new LinkedHashMap<>();
    }
}
