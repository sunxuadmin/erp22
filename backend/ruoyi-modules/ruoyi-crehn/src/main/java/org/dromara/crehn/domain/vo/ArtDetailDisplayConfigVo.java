package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class ArtDetailDisplayConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private TabConfig tabs;
    private AuditConfig audit;
    private FileConfig files;
    private ScoreConfig score;
    private NavigatorConfig navigator;
    private AuditProgressConfig auditProgress;
    private WorkspaceHeaderConfig workspaceHeader;
    private ListTableAppearanceConfig listTableAppearance;
    private ListTableLayoutConfig listTableLayout;
    private ReviewWorkbenchConfig reviewWorkbench;

    @Data
    public static class TabConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String basicInfoLabel;
        private String projectFilesLabel;
        private String auditDefaultTab;
        private String scoreDefaultTab;
        private PopupTabConfig audit;
        private PopupTabConfig score;
    }

    @Data
    public static class PopupTabConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private List<String> order;
        private List<String> visibleTabs;
        private Map<String, String> labels;
        private String defaultTab;
    }

    @Data
    public static class AuditConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String title;
        private List<String> summaryFields;
        private Map<String, String> summaryLabels;
        private Boolean actionsVisible;
        private AuditContentConfig content;
    }

    @Data
    public static class AuditContentConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private List<String> basicSections;
        private List<String> fileColumns;
        private List<String> recordColumns;
    }

    @Data
    public static class FileConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private List<String> fields;
        private Map<String, String> labels;
    }

    @Data
    public static class ScoreConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String title;
        private String scoreLabel;
        private String gradeLabel;
        private String commentLabel;
        private Boolean statusVisible;
        private Boolean commentVisible;
        private Boolean saveDraftVisible;
        private Boolean fileListVisible;
        private ScoreContentConfig content;
    }

    @Data
    public static class ScoreContentConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private List<String> basicSections;
        private List<String> fileColumns;
        private List<String> recordColumns;
    }

    @Data
    public static class NavigatorConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Integer titleFontSize;
        private Integer itemFontSize;
        private Integer countFontSize;
        private Integer rowHeight;
        private Integer itemGap;
        private Integer panelPadding;
        private String backgroundColor;
        private String textColor;
        private String activeColor;
        private String hoverBackground;
        private Integer hoverBackgroundOpacity;
        private String selectedBackground;
        private Integer selectedBackgroundOpacity;
        private String selectedTextColor;
        private String borderColor;
        private Integer backgroundOpacity;
        private Boolean dividerVisible;
        private String dividerColor;
        private Integer dividerOpacity;
        private Integer dividerWidth;
        private Integer borderRadius;
        private Integer sidebarWidth;
        private Integer sidebarMaxHeight;
        private Integer topHeight;
        private Boolean borderVisible;
        private Boolean shadowVisible;
        private Map<String, NavigatorPageConfig> pages;
    }

    @Data
    public static class NavigatorPageConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String layout;
        private List<String> fields;
        private Map<String, String> labels;
        private NavigatorActivityTitleConfig activityTitle;
    }

    @Data
    public static class NavigatorActivityTitleConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String displayMode;
        private String template;
        private Integer fontSize;
        private String color;
        private String fontWeight;
        private String align;
        private String switchText;
        private Boolean allowWrap;
    }

    @Data
    public static class AuditProgressConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Boolean visible;
        private String title;
        private String template;
        private Integer height;
        private Integer width;
        private String activeColor;
        private String completeColor;
        private String trackColor;
        private String textColor;
        private Integer fontSize;
    }

    @Data
    public static class WorkspaceHeaderConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Integer layoutVersion;
        private String managedActivityId;
        private Map<String, WorkspaceHeaderPageConfig> pages;
    }

    @Data
    public static class WorkspaceHeaderPageConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String titleTemplate;
        private Integer titleFontSize;
        private Integer titleFontWeight;
        private String titleColor;
        private Boolean sidebarEnabled;
        private Boolean sidebarDefaultExpanded;
        private String statusAlign;
        private Boolean statusCollapseOnOverflow;
        private List<WorkspaceHeaderStatusItemConfig> statusItems;
        private Integer componentGap;
        private Integer rowMinHeight;
        private Integer paddingTop;
        private Integer paddingBottom;
        private Integer paddingInline;
        private Integer marginTop;
        private Integer marginBottom;
        private WorkspaceHeaderButtonConfig navigatorToggleButton;
        private WorkspaceHeaderButtonConfig columnWidthResetButton;
        private WorkspaceHeaderButtonConfig selectAllButton;
        private WorkspaceHeaderButtonConfig unifiedSubmitButton;
        private WorkspaceHeaderButtonConfig navigationButton;
        private WorkspaceHeaderButtonConfig homeButton;
        private WorkspaceHeaderButtonStyleConfig buttonStyle;
        private Map<String, WorkspaceHeaderItemConfig> itemConfigs;
        private Map<String, WorkspaceHeaderSpacerInstanceConfig> spacerInstances;
        private Map<String, WorkspaceHeaderCompactGroupInstanceConfig> compactGroupInstances;
        private List<WorkspaceHeaderCardConfig> cards;
    }

    @Data
    public static class WorkspaceHeaderItemConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String align;
        private String widthMode;
        private Integer span;
        private Map<String, String> texts;
    }

    @Data
    public static class WorkspaceHeaderStatusItemConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String key;
        private Boolean visible;
        private Integer order;
    }

    @Data
    public static class WorkspaceHeaderSpacerInstanceConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String type;
        private Integer span;
        private Boolean allowStatusBorrow;
    }

    @Data
    public static class WorkspaceHeaderCompactGroupInstanceConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String widthMode;
        private Integer span;
        private String align;
        private Integer gap;
        private List<String> items;
        private String collapseMode;
        private List<String> pinnedItems;
        private String collapseText;
    }

    @Data
    public static class WorkspaceHeaderButtonConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String text;
        private String alternateText;
        private String tooltip;
        private String alternateTooltip;
        private String align;
        private Boolean iconVisible;
        private String variant;
        private Boolean showCount;
        private WorkspaceHeaderButtonAppearanceConfig appearance;
        private String targetMode;
        private String presetKey;
        private String customUrl;
        private String openMode;
    }

    @Data
    public static class WorkspaceHeaderButtonAppearanceConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String backgroundColor;
        private Integer backgroundOpacity;
        private String textColor;
        private Boolean borderVisible;
        private String borderColor;
        private Integer borderOpacity;
        private Integer borderWidth;
        private Integer borderRadius;
        private String hoverBackgroundColor;
        private Integer hoverBackgroundOpacity;
        private String hoverTextColor;
        private String hoverBorderColor;
        private Integer hoverBorderOpacity;
    }

    @Data
    public static class WorkspaceHeaderButtonStyleConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Integer fontSize;
        private Integer fontWeight;
        private Integer iconSize;
        private Integer height;
    }

    @Data
    public static class WorkspaceHeaderCardConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String key;
        private List<List<String>> rows;
        private String backgroundColor;
        private Integer backgroundOpacity;
        private Integer borderRadius;
        private Boolean borderVisible;
    }

    @Data
    public static class ListTableAppearanceConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Boolean globalColorOverride;
        private String headerBackground;
        private String headerTextColor;
        private String textColor;
        private String hoverBackground;
        private Integer hoverBackgroundOpacity;
        private String borderColor;
        private Boolean horizontalBorderVisible;
        private String horizontalBorderColor;
        private Integer horizontalBorderOpacity;
        private Boolean verticalBorderVisible;
        private String verticalBorderColor;
        private Integer verticalBorderOpacity;
        private Boolean outerBorderVisible;
        private String outerBorderColor;
        private Integer outerBorderOpacity;
        private Integer headerFontSize;
        private Integer bodyFontSize;
        private Integer buttonFontSize;
        private Integer rowHeight;
        private Integer borderRadius;
        private String borderMode;
        private String wrapMode;
        private Boolean headerWrap;
        private Boolean striped;
        private String actionStyle;
        private ListStatusAppearanceConfig statusAppearance;
        private ListActionAppearanceConfig rowActionAppearance;
    }

    @Data
    public static class ListTableConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private ListTableAppearanceConfig appearance;
        private ListTableLayoutConfig layout;
    }

    @Data
    public static class ListTableLayoutConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Integer version;
        private Map<String, ListTablePageConfig> pages;
    }

    @Data
    public static class ListTablePageConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String emptyText;
        private String selectionFixed;
        private Boolean serialVisible;
        private Integer serialWidth;
        private String serialFixed;
        private List<ReviewListColumnConfig> columns;
        private Map<String, List<ReviewListColumnConfig>> categoryColumns;
        private Map<String, String> statusLabels;
        private Map<String, String> actionLabels;
    }

    @Data
    public static class ListStatusAppearanceConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Boolean borderVisible;
        private Boolean iconVisible;
        private Boolean uniformWidth;
        private Map<String, String> colors;
    }

    @Data
    public static class ListActionAppearanceConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Boolean borderVisible;
        private Boolean iconVisible;
        private Boolean uniformWidth;
        private Integer disabledOpacity;
        private Map<String, String> colors;
    }

    @Data
    public static class ReviewWorkbenchConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private ReviewActivityTitleConfig activityTitle;
        private ReviewScopeNavigatorConfig scopeNavigator;
        private ReviewProgressConfig progress;
        private ReviewSignatureConfig signature;
        private List<ReviewListColumnConfig> columns;
        private Map<String, List<ReviewListColumnConfig>> categoryColumns;
    }

    @Data
    public static class ReviewActivityTitleConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Boolean visible;
        private String template;
        private Integer fontSize;
        private String color;
        private String fontWeight;
        private String align;
        private String switchText;
        private Boolean allowWrap;
    }

    @Data
    public static class ReviewScopeNavigatorConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Boolean visible;
        private String displayMode;
        private Boolean showCount;
        private Boolean progressVisible;
        private String progressPlacement;
    }

    @Data
    public static class ReviewProgressConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Boolean visible;
        private String template;
        private Boolean showPercentage;
        private Boolean showRemaining;
        private Integer height;
        private Integer width;
        private String activeColor;
        private String successColor;
        private String signedColor;
        private String trackColor;
        private String textColor;
        private Integer fontSize;
    }

    @Data
    public static class ReviewSignatureConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Boolean hintVisible;
        private String hintText;
        private Integer hintFontSize;
        private String hintFontWeight;
        private String hintColor;
        private String hintBackgroundColor;
        private String hintBorderColor;
        private String buttonText;
        private Integer buttonFontSize;
        private String buttonFontWeight;
        private String buttonTextColor;
    }

    @Data
    public static class ReviewListColumnConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String key;
        private String source;
        private String fieldKey;
        private String label;
        private Boolean visible;
        private Boolean deleted;
        private Integer width;
        private Integer minWidth;
        private String fixed;
    }
}
