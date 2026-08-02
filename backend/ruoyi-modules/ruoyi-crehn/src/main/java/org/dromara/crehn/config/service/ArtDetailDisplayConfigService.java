package org.dromara.crehn.config.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.CategoryFieldSchema;
import org.dromara.crehn.domain.vo.ArtDetailDisplayConfigVo;
import org.dromara.crehn.domain.vo.CategoryFieldSchemaVo;
import org.dromara.crehn.mapper.ActivityMapper;
import org.dromara.crehn.mapper.CategoryFieldSchemaMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.system.domain.bo.SysConfigBo;
import org.dromara.system.domain.vo.SysConfigVo;
import org.dromara.system.service.ISysConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtDetailDisplayConfigService {

    private static final String TABS_KEY = "crehn.review.detail.tabs";
    private static final String TABS_AUDIT_KEY = "crehn.review.detail.tabs.audit";
    private static final String TABS_SCORE_KEY = "crehn.review.detail.tabs.score";
    private static final String AUDIT_KEY = "crehn.review.detail.audit";
    private static final String AUDIT_CONTENT_KEY = "crehn.review.detail.audit.content";
    private static final String FILES_KEY = "crehn.review.detail.files";
    private static final String SCORE_KEY = "crehn.review.detail.score";
    private static final String SCORE_CONTENT_KEY = "crehn.review.detail.score.content";
    private static final String NAVIGATOR_KEY = "crehn.review.browse.navigator";
    private static final String NAVIGATOR_AUDIT_KEY = "crehn.review.browse.navigator.audit";
    private static final String NAVIGATOR_PROJECT_VIEW_KEY = "crehn.review.browse.navigator.projectView";
    private static final String NAVIGATOR_REVIEW_KEY = "crehn.review.browse.navigator.review";
    private static final String AUDIT_PROGRESS_KEY = "crehn.review.browse.audit.progress";
    private static final String WORKSPACE_HEADER_KEY_PREFIX = "crehn.review.workspace.header.";
    private static final String WORKSPACE_HEADER_META_KEY = WORKSPACE_HEADER_KEY_PREFIX + "meta";
    private static final String LIST_TABLE_APPEARANCE_KEY = "crehn.review.list.table.appearance";
    private static final String LIST_TABLE_LAYOUT_KEY = "crehn.review.list.table.layout";
    private static final String REVIEW_WORKBENCH_KEY = "crehn.review.workbench.display";
    private static final int MAX_CONFIG_VALUE_LENGTH = 500;
    private static final int MAX_NAVIGATOR_STYLE_VALUE_LENGTH = 5_000;
    private static final int MAX_WORKSPACE_PAGE_CONFIG_VALUE_LENGTH = 20_000;
    private static final int MAX_TABLE_APPEARANCE_VALUE_LENGTH = 5_000;
    private static final int MAX_TABLE_LAYOUT_VALUE_LENGTH = 100_000;
    private static final int MAX_REVIEW_WORKBENCH_VALUE_LENGTH = 50_000;

    private static final List<String> AUDIT_FIELD_KEYS = List.of("activity", "category", "school", "submittedAt", "auditOpinion");
    private static final List<String> AUDIT_TAB_KEYS = List.of("preview", "form", "members", "files", "records");
    private static final List<String> SCORE_TAB_KEYS = List.of("preview", "form", "members", "files", "scores");
    private static final List<String> AUDIT_BASIC_SECTION_KEYS = List.of("formFields", "genericTables", "downloadWorkbook");
    private static final List<String> AUDIT_FILE_COLUMN_KEYS = List.of("fileType", "fileName", "fileSize", "technicalCheck", "mediaInfo", "actions");
    private static final List<String> AUDIT_RECORD_COLUMN_KEYS = List.of("operator", "result", "transition", "opinion", "time");
    private static final List<String> SCORE_BASIC_SECTION_KEYS = List.of("submittedAt", "scoreVisibility", "formFields", "genericTables");
    private static final List<String> SCORE_FILE_COLUMN_KEYS = List.of("fileType", "fileName", "fileSize", "previewStatus", "actions");
    private static final List<String> SCORE_RECORD_COLUMN_KEYS = List.of("reviewer", "result", "comment");
    private static final List<String> FILE_FIELD_KEYS = List.of(
        "fileName", "fileType", "fileExt", "mimeType", "fileSize", "mediaType", "format",
        "resolution", "duration", "fps", "bitrate", "dpi", "checkStatus", "technicalRequirements"
    );
    private static final List<String> NAVIGATOR_FIELD_KEYS = List.of("activity", "category", "groupOrNature", "programForm", "school");
    private static final List<String> NAVIGATOR_LAYOUT_KEYS = List.of("sidebar", "top");
    private static final List<String> NAVIGATOR_ACTIVITY_DISPLAY_KEYS = List.of("title", "hidden");
    private static final List<String> REVIEW_LIST_BUILTIN_KEYS = List.of(
        "projectNo", "projectName", "activityName", "categoryName", "groupOrNature", "programForm",
        "scoreMode", "scoreResult", "scoreSubmittedAt", "scoreStatus", "actions"
    );
    private static final List<String> REVIEW_TITLE_WEIGHT_KEYS = List.of("normal", "medium", "bold");
    private static final List<String> REVIEW_TITLE_ALIGN_KEYS = List.of("left", "center", "right");
    private static final List<String> REVIEW_SCOPE_DISPLAY_MODE_KEYS = List.of("buttons", "select");
    private static final List<String> REVIEW_SCOPE_PROGRESS_PLACEMENT_KEYS = List.of("inside", "below");
    private static final List<String> WORKSPACE_HEADER_PAGE_KEYS = List.of("project", "audit", "review", "schoolSubmit", "projectView");
    private static final int WORKSPACE_HEADER_LAYOUT_VERSION = 8;
    private static final List<String> WORKSPACE_HEADER_ITEM_KEYS = List.of(
        "title", "navigatorToggle", "filters", "reviewScope", "categoryFilter", "groupFilter", "schoolFilter",
        "progress", "status", "search", "actions", "columnWidthReset", "selectAll", "unifiedSubmit",
        "navigationButton", "home", "compactGroup", "fixedSpacer", "autoSpacer"
    );
    private static final List<String> WORKSPACE_HEADER_BUSINESS_ITEM_KEYS = WORKSPACE_HEADER_ITEM_KEYS.stream()
        .filter(item -> !"compactGroup".equals(item) && !"fixedSpacer".equals(item) && !"autoSpacer".equals(item))
        .toList();
    private static final List<String> WORKSPACE_HEADER_BUTTON_VARIANT_KEYS = List.of("default", "plain", "text");
    private static final List<String> WORKSPACE_HEADER_COMPACT_COLLAPSE_MODE_KEYS = List.of("none", "overflow", "all");
    private static final List<String> WORKSPACE_HEADER_COLLAPSIBLE_BUTTON_KEYS = List.of(
        "navigatorToggle", "actions", "columnWidthReset", "selectAll", "unifiedSubmit", "navigationButton", "home"
    );
    private static final List<String> WORKSPACE_NAVIGATION_PRESET_KEYS = List.of(
        "schoolSubmit", "schoolProjects", "auditProjects", "reviewTasks", "projectOverview", "resultManagement", "home"
    );
    private static final List<String> WORKSPACE_HEADER_SPACER_KEYS = List.of("fixedSpacer", "autoSpacer");
    private static final List<String> WORKSPACE_HEADER_STATUS_ALIGN_KEYS = List.of("left", "center", "right");
    private static final List<String> WORKSPACE_HEADER_WIDTH_MODE_KEYS = List.of("fixed", "auto");
    private static final List<String> TABLE_BORDER_MODE_KEYS = List.of("horizontal", "grid", "none");
    private static final List<String> TABLE_ACTION_STYLE_KEYS = List.of("link", "button");
    private static final List<String> TABLE_WRAP_MODE_KEYS = List.of("nowrap", "wrap", "two-line");
    private static final List<String> TABLE_COLUMN_FIXED_KEYS = List.of("none", "left", "right");
    private static final List<String> TABLE_STRUCTURAL_FIXED_KEYS = List.of("none", "left");
    private static final List<String> TABLE_STATUS_SEMANTIC_KEYS = List.of(
        "draft", "pending", "approved", "returned", "unscored", "scored", "scoreDraft", "locked"
    );
    private static final List<String> TABLE_ACTION_SEMANTIC_KEYS = List.of(
        "view", "score", "edit", "return", "withdraw", "submit", "delete"
    );
    private static final List<String> LIST_TABLE_PAGE_KEYS = List.of(
        "project", "schoolSubmit", "audit", "review", "projectView",
        "reviewAssignment", "scoreSummary", "signedSheets"
    );

    private static final Map<String, String> AUDIT_DEFAULT_LABELS = orderedMap(
        "activity", "活动",
        "category", "类别",
        "school", "单位",
        "submittedAt", "提交时间",
        "auditOpinion", "审核意见"
    );

    private static final Map<String, String> AUDIT_TAB_DEFAULT_LABELS = orderedMap(
        "preview", "作品预览",
        "form", "基础信息",
        "members", "成员信息",
        "files", "作品文件",
        "records", "审核记录"
    );

    private static final Map<String, String> SCORE_TAB_DEFAULT_LABELS = orderedMap(
        "preview", "作品预览",
        "form", "基础信息",
        "members", "成员信息",
        "files", "作品文件",
        "scores", "评分记录"
    );

    private static final Map<String, String> FILE_DEFAULT_LABELS = orderedMap(
        "fileName", "文件名称",
        "fileType", "材料类型",
        "fileExt", "扩展名",
        "mimeType", "MIME 类型",
        "fileSize", "文件大小",
        "mediaType", "媒体类型",
        "format", "媒体格式",
        "resolution", "分辨率",
        "duration", "时长",
        "fps", "帧率",
        "bitrate", "码率",
        "dpi", "DPI",
        "checkStatus", "技术校验",
        "technicalRequirements", "技术校验要求"
    );

    private static final Map<String, String> NAVIGATOR_DEFAULT_LABELS = orderedMap(
        "activity", "活动",
        "category", "大类 / 子类",
        "groupOrNature", "组别",
        "programForm", "类别细分",
        "school", "学校"
    );

    private final ISysConfigService sysConfigService;
    private final CategoryFieldSchemaMapper fieldSchemaMapper;
    private final ActivityMapper activityMapper;

    public List<CategoryFieldSchemaVo> categoryFields(Long categoryId) {
        if (categoryId == null) {
            return List.of();
        }
        return fieldSchemaMapper.selectVoList(Wrappers.lambdaQuery(CategoryFieldSchema.class)
            .eq(CategoryFieldSchema::getCategoryId, categoryId)
            .orderByAsc(CategoryFieldSchema::getSortOrder)
            .orderByAsc(CategoryFieldSchema::getId));
    }

    public ArtDetailDisplayConfigVo getConfig() {
        ArtDetailDisplayConfigVo config = defaults();
        ArtDetailDisplayConfigVo.TabConfig tabs = readPart(TABS_KEY, ArtDetailDisplayConfigVo.TabConfig.class, config.getTabs());
        tabs.setAudit(readPart(TABS_AUDIT_KEY, ArtDetailDisplayConfigVo.PopupTabConfig.class, tabs.getAudit()));
        tabs.setScore(readPart(TABS_SCORE_KEY, ArtDetailDisplayConfigVo.PopupTabConfig.class, tabs.getScore()));
        config.setTabs(tabs);
        ArtDetailDisplayConfigVo.AuditConfig audit = readPart(AUDIT_KEY, ArtDetailDisplayConfigVo.AuditConfig.class, config.getAudit());
        audit.setContent(readPart(AUDIT_CONTENT_KEY, ArtDetailDisplayConfigVo.AuditContentConfig.class, audit.getContent()));
        config.setAudit(audit);
        config.setFiles(readPart(FILES_KEY, ArtDetailDisplayConfigVo.FileConfig.class, config.getFiles()));
        ArtDetailDisplayConfigVo.ScoreConfig score = readPart(SCORE_KEY, ArtDetailDisplayConfigVo.ScoreConfig.class, config.getScore());
        score.setContent(readPart(SCORE_CONTENT_KEY, ArtDetailDisplayConfigVo.ScoreContentConfig.class, score.getContent()));
        config.setScore(score);
        ArtDetailDisplayConfigVo.NavigatorConfig navigator = readPart(NAVIGATOR_KEY, ArtDetailDisplayConfigVo.NavigatorConfig.class, config.getNavigator());
        Map<String, ArtDetailDisplayConfigVo.NavigatorPageConfig> defaultPages = config.getNavigator().getPages();
        Map<String, ArtDetailDisplayConfigVo.NavigatorPageConfig> pages = new LinkedHashMap<>();
        pages.put("audit", readPart(NAVIGATOR_AUDIT_KEY, ArtDetailDisplayConfigVo.NavigatorPageConfig.class, defaultPages.get("audit")));
        pages.put("projectView", readPart(NAVIGATOR_PROJECT_VIEW_KEY, ArtDetailDisplayConfigVo.NavigatorPageConfig.class, defaultPages.get("projectView")));
        pages.put("review", readPart(NAVIGATOR_REVIEW_KEY, ArtDetailDisplayConfigVo.NavigatorPageConfig.class, defaultPages.get("review")));
        navigator.setPages(pages);
        config.setNavigator(navigator);
        config.setAuditProgress(readPart(AUDIT_PROGRESS_KEY, ArtDetailDisplayConfigVo.AuditProgressConfig.class, config.getAuditProgress()));
        config.setWorkspaceHeader(readWorkspaceHeader(config.getWorkspaceHeader()));
        config.setListTableAppearance(readPart(LIST_TABLE_APPEARANCE_KEY, ArtDetailDisplayConfigVo.ListTableAppearanceConfig.class, config.getListTableAppearance()));
        config.setReviewWorkbench(readPart(REVIEW_WORKBENCH_KEY, ArtDetailDisplayConfigVo.ReviewWorkbenchConfig.class, config.getReviewWorkbench()));
        config.setListTableLayout(readPart(
            LIST_TABLE_LAYOUT_KEY,
            ArtDetailDisplayConfigVo.ListTableLayoutConfig.class,
            defaultListTableLayout(config.getReviewWorkbench())
        ));
        return normalize(config);
    }

    public ArtDetailDisplayConfigVo.ListTableConfig getTableConfig() {
        ArtDetailDisplayConfigVo config = getConfig();
        ArtDetailDisplayConfigVo.ListTableConfig tableConfig = new ArtDetailDisplayConfigVo.ListTableConfig();
        tableConfig.setAppearance(config.getListTableAppearance());
        tableConfig.setLayout(config.getListTableLayout());
        return tableConfig;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(ArtDetailDisplayConfigVo request) {
        ArtDetailDisplayConfigVo config = normalize(request);
        writePart(TABS_KEY, "艺术评审详情标签兼容配置", legacyTabs(config.getTabs()));
        writePart(TABS_AUDIT_KEY, "艺术评审审核弹窗标签", config.getTabs().getAudit());
        writePart(TABS_SCORE_KEY, "艺术评审评分弹窗标签", config.getTabs().getScore());
        writePart(AUDIT_KEY, "艺术评审审核处理显示", auditStyle(config.getAudit()));
        writePart(AUDIT_CONTENT_KEY, "艺术评审审核弹窗固定信息", config.getAudit().getContent());
        writePart(FILES_KEY, "艺术评审作品文件显示", config.getFiles());
        writePart(SCORE_KEY, "艺术评审评分卡片显示", scoreStyle(config.getScore()));
        writePart(SCORE_CONTENT_KEY, "艺术评审评分弹窗固定信息", config.getScore().getContent());
        writePart(NAVIGATOR_KEY, "艺术评审浏览导航外观", navigatorStyle(config.getNavigator()), MAX_NAVIGATOR_STYLE_VALUE_LENGTH);
        writePart(NAVIGATOR_AUDIT_KEY, "艺术评审项目审核导航", config.getNavigator().getPages().get("audit"));
        writePart(NAVIGATOR_PROJECT_VIEW_KEY, "艺术评审上报进度导航", config.getNavigator().getPages().get("projectView"));
        writePart(NAVIGATOR_REVIEW_KEY, "艺术评审专家评分导航", config.getNavigator().getPages().get("review"));
        writePart(AUDIT_PROGRESS_KEY, "艺术评审项目审核进度", config.getAuditProgress());
        writeWorkspaceHeader(config.getWorkspaceHeader());
        writePart(REVIEW_WORKBENCH_KEY, "艺术评审工作台显示配置", config.getReviewWorkbench(), MAX_REVIEW_WORKBENCH_VALUE_LENGTH);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveTableConfig(ArtDetailDisplayConfigVo.ListTableConfig request) {
        ArtDetailDisplayConfigVo defaults = defaults();
        ArtDetailDisplayConfigVo.ListTableAppearanceConfig appearance = normalizeListTableAppearance(
            request == null ? null : request.getAppearance(),
            defaults.getListTableAppearance()
        );
        ArtDetailDisplayConfigVo.ListTableLayoutConfig layout = normalizeListTableLayout(
            request == null ? null : request.getLayout(),
            defaults.getListTableLayout()
        );
        writePart(LIST_TABLE_APPEARANCE_KEY, "艺术评审项目列表表格外观", appearance, MAX_TABLE_APPEARANCE_VALUE_LENGTH);
        writePart(LIST_TABLE_LAYOUT_KEY, "艺术评审项目列表全局列方案", layout, MAX_TABLE_LAYOUT_VALUE_LENGTH);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveManagedActivity(Long activityId) {
        if (activityId == null) {
            throw new ServiceException("当前业务活动不能为空");
        }
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null || !ArtReviewConstants.ACTIVITY_ENABLED.equals(activity.getStatus())) {
            throw new ServiceException("只能选择活动配置中状态为启用的当前活动");
        }
        ArtDetailDisplayConfigVo.WorkspaceHeaderConfig defaults = defaultWorkspaceHeader();
        ArtDetailDisplayConfigVo.WorkspaceHeaderConfig workspaceHeader = normalizeWorkspaceHeader(
            readWorkspaceHeader(defaults),
            defaults
        );
        workspaceHeader.setManagedActivityId(String.valueOf(activityId));
        writeWorkspaceHeader(workspaceHeader);
    }

    private ArtDetailDisplayConfigVo normalize(ArtDetailDisplayConfigVo source) {
        ArtDetailDisplayConfigVo defaults = defaults();
        ArtDetailDisplayConfigVo target = new ArtDetailDisplayConfigVo();

        ArtDetailDisplayConfigVo.TabConfig sourceTabs = source == null ? null : source.getTabs();
        ArtDetailDisplayConfigVo.TabConfig tabs = new ArtDetailDisplayConfigVo.TabConfig();
        tabs.setBasicInfoLabel(text(sourceTabs == null ? null : sourceTabs.getBasicInfoLabel(), defaults.getTabs().getBasicInfoLabel(), 20));
        tabs.setProjectFilesLabel(text(sourceTabs == null ? null : sourceTabs.getProjectFilesLabel(), defaults.getTabs().getProjectFilesLabel(), 20));
        tabs.setAuditDefaultTab(allowedValue(sourceTabs == null ? null : sourceTabs.getAuditDefaultTab(), AUDIT_TAB_KEYS, defaults.getTabs().getAuditDefaultTab()));
        tabs.setScoreDefaultTab(allowedValue(sourceTabs == null ? null : sourceTabs.getScoreDefaultTab(), SCORE_TAB_KEYS, defaults.getTabs().getScoreDefaultTab()));
        tabs.setAudit(normalizePopupTabs(
            sourceTabs == null ? null : sourceTabs.getAudit(),
            AUDIT_TAB_KEYS,
            AUDIT_TAB_DEFAULT_LABELS,
            tabs.getAuditDefaultTab(),
            tabs.getBasicInfoLabel(),
            tabs.getProjectFilesLabel()
        ));
        tabs.setScore(normalizePopupTabs(
            sourceTabs == null ? null : sourceTabs.getScore(),
            SCORE_TAB_KEYS,
            SCORE_TAB_DEFAULT_LABELS,
            tabs.getScoreDefaultTab(),
            tabs.getBasicInfoLabel(),
            tabs.getProjectFilesLabel()
        ));
        tabs.setAuditDefaultTab(tabs.getAudit().getDefaultTab());
        tabs.setScoreDefaultTab(tabs.getScore().getDefaultTab());
        target.setTabs(tabs);

        ArtDetailDisplayConfigVo.AuditConfig sourceAudit = source == null ? null : source.getAudit();
        ArtDetailDisplayConfigVo.AuditConfig audit = new ArtDetailDisplayConfigVo.AuditConfig();
        audit.setTitle(text(sourceAudit == null ? null : sourceAudit.getTitle(), defaults.getAudit().getTitle(), 20));
        audit.setSummaryFields(normalizeFields(sourceAudit == null ? null : sourceAudit.getSummaryFields(), AUDIT_FIELD_KEYS, defaults.getAudit().getSummaryFields()));
        audit.setSummaryLabels(normalizeLabels(sourceAudit == null ? null : sourceAudit.getSummaryLabels(), AUDIT_DEFAULT_LABELS));
        audit.setActionsVisible(sourceAudit == null || sourceAudit.getActionsVisible() == null ? defaults.getAudit().getActionsVisible() : sourceAudit.getActionsVisible());
        ArtDetailDisplayConfigVo.AuditContentConfig sourceAuditContent = sourceAudit == null ? null : sourceAudit.getContent();
        ArtDetailDisplayConfigVo.AuditContentConfig auditContent = new ArtDetailDisplayConfigVo.AuditContentConfig();
        auditContent.setBasicSections(normalizeFields(sourceAuditContent == null ? null : sourceAuditContent.getBasicSections(), AUDIT_BASIC_SECTION_KEYS, defaults.getAudit().getContent().getBasicSections()));
        auditContent.setFileColumns(normalizeFields(sourceAuditContent == null ? null : sourceAuditContent.getFileColumns(), AUDIT_FILE_COLUMN_KEYS, defaults.getAudit().getContent().getFileColumns()));
        auditContent.setRecordColumns(normalizeFields(sourceAuditContent == null ? null : sourceAuditContent.getRecordColumns(), AUDIT_RECORD_COLUMN_KEYS, defaults.getAudit().getContent().getRecordColumns()));
        audit.setContent(auditContent);
        target.setAudit(audit);

        ArtDetailDisplayConfigVo.FileConfig sourceFiles = source == null ? null : source.getFiles();
        ArtDetailDisplayConfigVo.FileConfig files = new ArtDetailDisplayConfigVo.FileConfig();
        files.setFields(normalizeFileFields(sourceFiles == null ? null : sourceFiles.getFields(), defaults.getFiles().getFields()));
        files.setLabels(normalizeLabels(sourceFiles == null ? null : sourceFiles.getLabels(), FILE_DEFAULT_LABELS));
        target.setFiles(files);

        ArtDetailDisplayConfigVo.ScoreConfig sourceScore = source == null ? null : source.getScore();
        ArtDetailDisplayConfigVo.ScoreConfig score = new ArtDetailDisplayConfigVo.ScoreConfig();
        score.setTitle(text(sourceScore == null ? null : sourceScore.getTitle(), defaults.getScore().getTitle(), 20));
        score.setScoreLabel(text(sourceScore == null ? null : sourceScore.getScoreLabel(), defaults.getScore().getScoreLabel(), 20));
        score.setGradeLabel(text(sourceScore == null ? null : sourceScore.getGradeLabel(), defaults.getScore().getGradeLabel(), 20));
        score.setCommentLabel(text(sourceScore == null ? null : sourceScore.getCommentLabel(), defaults.getScore().getCommentLabel(), 20));
        score.setStatusVisible(bool(sourceScore == null ? null : sourceScore.getStatusVisible(), defaults.getScore().getStatusVisible()));
        score.setCommentVisible(bool(sourceScore == null ? null : sourceScore.getCommentVisible(), defaults.getScore().getCommentVisible()));
        score.setSaveDraftVisible(bool(sourceScore == null ? null : sourceScore.getSaveDraftVisible(), defaults.getScore().getSaveDraftVisible()));
        score.setFileListVisible(bool(sourceScore == null ? null : sourceScore.getFileListVisible(), defaults.getScore().getFileListVisible()));
        ArtDetailDisplayConfigVo.ScoreContentConfig sourceScoreContent = sourceScore == null ? null : sourceScore.getContent();
        ArtDetailDisplayConfigVo.ScoreContentConfig scoreContent = new ArtDetailDisplayConfigVo.ScoreContentConfig();
        scoreContent.setBasicSections(normalizeFields(sourceScoreContent == null ? null : sourceScoreContent.getBasicSections(), SCORE_BASIC_SECTION_KEYS, defaults.getScore().getContent().getBasicSections()));
        scoreContent.setFileColumns(normalizeFields(sourceScoreContent == null ? null : sourceScoreContent.getFileColumns(), SCORE_FILE_COLUMN_KEYS, defaults.getScore().getContent().getFileColumns()));
        scoreContent.setRecordColumns(normalizeFields(sourceScoreContent == null ? null : sourceScoreContent.getRecordColumns(), SCORE_RECORD_COLUMN_KEYS, defaults.getScore().getContent().getRecordColumns()));
        score.setContent(scoreContent);
        target.setScore(score);

        ArtDetailDisplayConfigVo.NavigatorConfig sourceNavigator = source == null ? null : source.getNavigator();
        ArtDetailDisplayConfigVo.NavigatorConfig navigator = new ArtDetailDisplayConfigVo.NavigatorConfig();
        navigator.setTitleFontSize(number(sourceNavigator == null ? null : sourceNavigator.getTitleFontSize(), defaults.getNavigator().getTitleFontSize(), 12, 22));
        navigator.setItemFontSize(number(sourceNavigator == null ? null : sourceNavigator.getItemFontSize(), defaults.getNavigator().getItemFontSize(), 12, 20));
        navigator.setCountFontSize(number(sourceNavigator == null ? null : sourceNavigator.getCountFontSize(), defaults.getNavigator().getCountFontSize(), 10, 18));
        navigator.setRowHeight(number(sourceNavigator == null ? null : sourceNavigator.getRowHeight(), defaults.getNavigator().getRowHeight(), 30, 60));
        navigator.setItemGap(number(sourceNavigator == null ? null : sourceNavigator.getItemGap(), defaults.getNavigator().getItemGap(), 0, 16));
        navigator.setPanelPadding(number(sourceNavigator == null ? null : sourceNavigator.getPanelPadding(), defaults.getNavigator().getPanelPadding(), 8, 24));
        navigator.setBackgroundColor(color(sourceNavigator == null ? null : sourceNavigator.getBackgroundColor(), defaults.getNavigator().getBackgroundColor()));
        navigator.setTextColor(color(sourceNavigator == null ? null : sourceNavigator.getTextColor(), defaults.getNavigator().getTextColor()));
        navigator.setActiveColor(color(sourceNavigator == null ? null : sourceNavigator.getActiveColor(), defaults.getNavigator().getActiveColor()));
        navigator.setHoverBackground(color(
            sourceNavigator == null ? null : sourceNavigator.getHoverBackground(),
            sourceNavigator == null ? defaults.getNavigator().getHoverBackground() : color(sourceNavigator.getActiveColor(), defaults.getNavigator().getHoverBackground())
        ));
        navigator.setHoverBackgroundOpacity(number(sourceNavigator == null ? null : sourceNavigator.getHoverBackgroundOpacity(), defaults.getNavigator().getHoverBackgroundOpacity(), 0, 100));
        navigator.setSelectedBackground(color(
            sourceNavigator == null ? null : sourceNavigator.getSelectedBackground(),
            sourceNavigator == null ? defaults.getNavigator().getSelectedBackground() : color(sourceNavigator.getActiveColor(), defaults.getNavigator().getSelectedBackground())
        ));
        navigator.setSelectedBackgroundOpacity(number(sourceNavigator == null ? null : sourceNavigator.getSelectedBackgroundOpacity(), defaults.getNavigator().getSelectedBackgroundOpacity(), 0, 100));
        navigator.setSelectedTextColor(color(
            sourceNavigator == null ? null : sourceNavigator.getSelectedTextColor(),
            sourceNavigator == null ? defaults.getNavigator().getSelectedTextColor() : color(sourceNavigator.getActiveColor(), defaults.getNavigator().getSelectedTextColor())
        ));
        navigator.setBorderColor(color(sourceNavigator == null ? null : sourceNavigator.getBorderColor(), defaults.getNavigator().getBorderColor()));
        navigator.setBackgroundOpacity(number(sourceNavigator == null ? null : sourceNavigator.getBackgroundOpacity(), defaults.getNavigator().getBackgroundOpacity(), 0, 100));
        navigator.setDividerVisible(bool(sourceNavigator == null ? null : sourceNavigator.getDividerVisible(), defaults.getNavigator().getDividerVisible()));
        navigator.setDividerColor(color(
            sourceNavigator == null ? null : sourceNavigator.getDividerColor(),
            sourceNavigator == null ? defaults.getNavigator().getDividerColor() : color(sourceNavigator.getBorderColor(), defaults.getNavigator().getDividerColor())
        ));
        navigator.setDividerOpacity(number(sourceNavigator == null ? null : sourceNavigator.getDividerOpacity(), defaults.getNavigator().getDividerOpacity(), 0, 100));
        navigator.setDividerWidth(number(sourceNavigator == null ? null : sourceNavigator.getDividerWidth(), defaults.getNavigator().getDividerWidth(), 1, 3));
        navigator.setBorderRadius(number(sourceNavigator == null ? null : sourceNavigator.getBorderRadius(), defaults.getNavigator().getBorderRadius(), 0, 24));
        navigator.setSidebarWidth(number(sourceNavigator == null ? null : sourceNavigator.getSidebarWidth(), defaults.getNavigator().getSidebarWidth(), 260, 460));
        navigator.setSidebarMaxHeight(number(sourceNavigator == null ? null : sourceNavigator.getSidebarMaxHeight(), defaults.getNavigator().getSidebarMaxHeight(), 320, 960));
        navigator.setTopHeight(number(sourceNavigator == null ? null : sourceNavigator.getTopHeight(), defaults.getNavigator().getTopHeight(), 56, 180));
        navigator.setBorderVisible(bool(sourceNavigator == null ? null : sourceNavigator.getBorderVisible(), defaults.getNavigator().getBorderVisible()));
        navigator.setShadowVisible(bool(sourceNavigator == null ? null : sourceNavigator.getShadowVisible(), defaults.getNavigator().getShadowVisible()));
        Map<String, ArtDetailDisplayConfigVo.NavigatorPageConfig> sourcePages = sourceNavigator == null ? null : sourceNavigator.getPages();
        Map<String, ArtDetailDisplayConfigVo.NavigatorPageConfig> pages = new LinkedHashMap<>();
        pages.put("audit", normalizeNavigatorPage("audit", sourcePages == null ? null : sourcePages.get("audit"), defaults.getNavigator().getPages().get("audit")));
        pages.put("projectView", normalizeNavigatorPage("projectView", sourcePages == null ? null : sourcePages.get("projectView"), defaults.getNavigator().getPages().get("projectView")));
        pages.put("review", normalizeNavigatorPage("review", sourcePages == null ? null : sourcePages.get("review"), defaults.getNavigator().getPages().get("review")));
        navigator.setPages(pages);
        target.setNavigator(navigator);
        target.setAuditProgress(normalizeAuditProgress(
            source == null ? null : source.getAuditProgress(),
            defaults.getAuditProgress()
        ));
        target.setWorkspaceHeader(normalizeWorkspaceHeader(
            source == null ? null : source.getWorkspaceHeader(),
            defaults.getWorkspaceHeader()
        ));
        target.setListTableAppearance(normalizeListTableAppearance(
            source == null ? null : source.getListTableAppearance(),
            defaults.getListTableAppearance()
        ));
        target.setReviewWorkbench(normalizeReviewWorkbench(
            source == null ? null : source.getReviewWorkbench(),
            defaults.getReviewWorkbench()
        ));
        target.setListTableLayout(normalizeListTableLayout(
            source == null ? null : source.getListTableLayout(),
            defaultListTableLayout(target.getReviewWorkbench())
        ));
        return target;
    }

    private ArtDetailDisplayConfigVo defaults() {
        ArtDetailDisplayConfigVo config = new ArtDetailDisplayConfigVo();

        ArtDetailDisplayConfigVo.TabConfig tabs = new ArtDetailDisplayConfigVo.TabConfig();
        tabs.setBasicInfoLabel("基础信息");
        tabs.setProjectFilesLabel("作品文件");
        tabs.setAuditDefaultTab("preview");
        tabs.setScoreDefaultTab("preview");
        tabs.setAudit(defaultPopupTabs(AUDIT_TAB_KEYS, AUDIT_TAB_DEFAULT_LABELS));
        tabs.setScore(defaultPopupTabs(SCORE_TAB_KEYS, SCORE_TAB_DEFAULT_LABELS));
        config.setTabs(tabs);

        ArtDetailDisplayConfigVo.AuditConfig audit = new ArtDetailDisplayConfigVo.AuditConfig();
        audit.setTitle("审核处理");
        audit.setSummaryFields(new ArrayList<>(AUDIT_FIELD_KEYS));
        audit.setSummaryLabels(new LinkedHashMap<>(AUDIT_DEFAULT_LABELS));
        audit.setActionsVisible(true);
        ArtDetailDisplayConfigVo.AuditContentConfig auditContent = new ArtDetailDisplayConfigVo.AuditContentConfig();
        auditContent.setBasicSections(new ArrayList<>(AUDIT_BASIC_SECTION_KEYS));
        auditContent.setFileColumns(new ArrayList<>(AUDIT_FILE_COLUMN_KEYS));
        auditContent.setRecordColumns(new ArrayList<>(AUDIT_RECORD_COLUMN_KEYS));
        audit.setContent(auditContent);
        config.setAudit(audit);

        ArtDetailDisplayConfigVo.FileConfig files = new ArtDetailDisplayConfigVo.FileConfig();
        files.setFields(new ArrayList<>(List.of(
            "fileName", "fileType", "fileSize", "format", "resolution", "duration", "fps", "bitrate", "dpi", "checkStatus", "technicalRequirements"
        )));
        files.setLabels(new LinkedHashMap<>(FILE_DEFAULT_LABELS));
        config.setFiles(files);

        ArtDetailDisplayConfigVo.ScoreConfig score = new ArtDetailDisplayConfigVo.ScoreConfig();
        score.setTitle("评审评分");
        score.setScoreLabel("分数");
        score.setGradeLabel("等级");
        score.setCommentLabel("意见");
        score.setStatusVisible(true);
        score.setCommentVisible(true);
        score.setSaveDraftVisible(true);
        score.setFileListVisible(true);
        ArtDetailDisplayConfigVo.ScoreContentConfig scoreContent = new ArtDetailDisplayConfigVo.ScoreContentConfig();
        scoreContent.setBasicSections(new ArrayList<>(SCORE_BASIC_SECTION_KEYS));
        scoreContent.setFileColumns(new ArrayList<>(SCORE_FILE_COLUMN_KEYS));
        scoreContent.setRecordColumns(new ArrayList<>(SCORE_RECORD_COLUMN_KEYS));
        score.setContent(scoreContent);
        config.setScore(score);

        ArtDetailDisplayConfigVo.NavigatorConfig navigator = new ArtDetailDisplayConfigVo.NavigatorConfig();
        navigator.setTitleFontSize(15);
        navigator.setItemFontSize(14);
        navigator.setCountFontSize(13);
        navigator.setRowHeight(38);
        navigator.setItemGap(2);
        navigator.setPanelPadding(12);
        navigator.setBackgroundColor("#ffffff");
        navigator.setTextColor("#29445f");
        navigator.setActiveColor("#2563eb");
        navigator.setHoverBackground("#2563eb");
        navigator.setHoverBackgroundOpacity(7);
        navigator.setSelectedBackground("#2563eb");
        navigator.setSelectedBackgroundOpacity(10);
        navigator.setSelectedTextColor("#2563eb");
        navigator.setBorderColor("#e8edf5");
        navigator.setBackgroundOpacity(100);
        navigator.setDividerVisible(true);
        navigator.setDividerColor("#e8edf5");
        navigator.setDividerOpacity(72);
        navigator.setDividerWidth(1);
        navigator.setBorderRadius(10);
        navigator.setSidebarWidth(340);
        navigator.setSidebarMaxHeight(720);
        navigator.setTopHeight(76);
        navigator.setBorderVisible(true);
        navigator.setShadowVisible(true);
        Map<String, ArtDetailDisplayConfigVo.NavigatorPageConfig> pages = new LinkedHashMap<>();
        pages.put("audit", defaultNavigatorPage("audit"));
        pages.put("projectView", defaultNavigatorPage("projectView"));
        pages.put("review", defaultNavigatorPage("review"));
        navigator.setPages(pages);
        config.setNavigator(navigator);
        config.setAuditProgress(defaultAuditProgress());
        config.setWorkspaceHeader(defaultWorkspaceHeader());
        config.setListTableAppearance(defaultListTableAppearance());
        config.setReviewWorkbench(defaultReviewWorkbench());
        config.setListTableLayout(defaultListTableLayout(config.getReviewWorkbench()));
        return config;
    }

    private <T> T readPart(String key, Class<T> type, T fallback) {
        String value = sysConfigService.selectConfigByKey(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            T parsed = JsonUtils.parseObject(value, type);
            return parsed == null ? fallback : parsed;
        } catch (RuntimeException ex) {
            log.warn("Failed to parse art detail display config, key={}", key, ex);
            return fallback;
        }
    }

    private ArtDetailDisplayConfigVo.WorkspaceHeaderConfig readWorkspaceHeader(
        ArtDetailDisplayConfigVo.WorkspaceHeaderConfig defaults
    ) {
        ArtDetailDisplayConfigVo.WorkspaceHeaderConfig emptyMeta = new ArtDetailDisplayConfigVo.WorkspaceHeaderConfig();
        emptyMeta.setLayoutVersion(0);
        ArtDetailDisplayConfigVo.WorkspaceHeaderConfig meta = readPart(
            WORKSPACE_HEADER_META_KEY,
            ArtDetailDisplayConfigVo.WorkspaceHeaderConfig.class,
            emptyMeta
        );
        if (!Objects.equals(meta.getLayoutVersion(), WORKSPACE_HEADER_LAYOUT_VERSION)) {
            return defaults;
        }
        ArtDetailDisplayConfigVo.WorkspaceHeaderConfig result = new ArtDetailDisplayConfigVo.WorkspaceHeaderConfig();
        result.setLayoutVersion(WORKSPACE_HEADER_LAYOUT_VERSION);
        result.setManagedActivityId(meta.getManagedActivityId());
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig> pages = new LinkedHashMap<>();
        for (String pageKey : WORKSPACE_HEADER_PAGE_KEYS) {
            pages.put(pageKey, readPart(
                workspaceHeaderPageConfigKey(pageKey),
                ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig.class,
                defaults.getPages().get(pageKey)
            ));
        }
        result.setPages(pages);
        return result;
    }

    private Map<String, String> readWorkspaceHeaderPartsFresh() {
        SysConfigBo query = new SysConfigBo();
        query.setConfigKey(WORKSPACE_HEADER_KEY_PREFIX);
        Map<String, String> values = new LinkedHashMap<>();
        sysConfigService.selectConfigList(query).forEach(item -> {
            String key = item.getConfigKey();
            boolean pageConfigKey = WORKSPACE_HEADER_PAGE_KEYS.stream()
                .anyMatch(pageKey -> workspaceHeaderPageConfigKey(pageKey).equals(key));
            if (WORKSPACE_HEADER_META_KEY.equals(key) || pageConfigKey) {
                values.put(key, item.getConfigValue());
            }
        });
        return values;
    }

    private void writeWorkspaceHeader(ArtDetailDisplayConfigVo.WorkspaceHeaderConfig value) {
        for (String pageKey : WORKSPACE_HEADER_PAGE_KEYS) {
            writePart(
                workspaceHeaderPageConfigKey(pageKey),
                workspaceHeaderPageConfigName(pageKey),
                value.getPages().get(pageKey),
                MAX_WORKSPACE_PAGE_CONFIG_VALUE_LENGTH
            );
        }
        ArtDetailDisplayConfigVo.WorkspaceHeaderConfig meta = new ArtDetailDisplayConfigVo.WorkspaceHeaderConfig();
        meta.setLayoutVersion(WORKSPACE_HEADER_LAYOUT_VERSION);
        meta.setManagedActivityId(value.getManagedActivityId());
        writePart(WORKSPACE_HEADER_META_KEY, "艺术评审项目列表头部编排版本", meta);

        // 系统参数缓存会在事务提交后才更新；保存事务内必须绕过缓存读取当前数据库值，
        // 否则会把提交前的旧缓存误判为持久化失败并触发事务回滚。
        Map<String, String> persisted = readWorkspaceHeaderPartsFresh();
        if (!Objects.equals(JsonUtils.toJsonString(meta), persisted.get(WORKSPACE_HEADER_META_KEY))) {
            throw new ServiceException("项目列表头部版本或当前业务活动保存回读不一致，请重试");
        }
        for (String pageKey : WORKSPACE_HEADER_PAGE_KEYS) {
            if (!Objects.equals(
                JsonUtils.toJsonString(value.getPages().get(pageKey)),
                persisted.get(workspaceHeaderPageConfigKey(pageKey))
            )) {
                throw new ServiceException(workspaceHeaderPageConfigName(pageKey) + "保存回读不一致，请重试");
            }
        }
    }

    private static String workspaceHeaderPageConfigKey(String pageKey) {
        return WORKSPACE_HEADER_KEY_PREFIX + "page." + pageKey;
    }

    private static String workspaceHeaderPageConfigName(String pageKey) {
        return switch (pageKey) {
            case "project" -> "艺术评审类别上报表头";
            case "audit" -> "艺术评审项目审核表头";
            case "review" -> "艺术评审专家评分表头";
            case "schoolSubmit" -> "艺术评审学校统一提交表头";
            case "projectView" -> "艺术评审上报进度表头";
            default -> "艺术评审项目列表表头";
        };
    }

    private void writePart(String key, String name, Object value) {
        writePart(key, name, value, MAX_CONFIG_VALUE_LENGTH);
    }

    private void writePart(String key, String name, Object value, int maxLength) {
        String json = JsonUtils.toJsonString(value);
        if (json == null || json.length() > maxLength) {
            throw new ServiceException(name + "过长（最大 " + maxLength + " 字符），请缩短自定义文字或减少组件");
        }
        SysConfigBo query = new SysConfigBo();
        query.setConfigKey(key);
        SysConfigVo existing = sysConfigService.selectConfigList(query).stream()
            .filter(item -> key.equals(item.getConfigKey()))
            .findFirst()
            .orElse(null);
        SysConfigBo config = new SysConfigBo();
        config.setConfigId(existing == null ? null : existing.getConfigId());
        config.setConfigName(name);
        config.setConfigKey(key);
        config.setConfigValue(json);
        config.setConfigType("Y");
        config.setRemark("艺术评审审核与评分详情界面显示配置");
        if (existing == null) {
            sysConfigService.insertConfig(config);
        } else {
            sysConfigService.updateConfig(config);
        }
    }

    private static ArtDetailDisplayConfigVo.WorkspaceHeaderConfig defaultWorkspaceHeader() {
        ArtDetailDisplayConfigVo.WorkspaceHeaderConfig config = new ArtDetailDisplayConfigVo.WorkspaceHeaderConfig();
        config.setLayoutVersion(WORKSPACE_HEADER_LAYOUT_VERSION);
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig> pages = new LinkedHashMap<>();
        pages.put("project", defaultWorkspaceHeaderPage("project", "{categoryName}上报列表"));
        pages.put("audit", defaultWorkspaceHeaderPage("audit", "{activityName}项目审核"));
        pages.put("review", defaultWorkspaceHeaderPage("review", "{activityName}评审工作台"));
        pages.put("schoolSubmit", defaultWorkspaceHeaderPage("schoolSubmit", "{activityName}学校统一上报"));
        pages.put("projectView", defaultWorkspaceHeaderPage("projectView", "{activityName}上报进度"));
        config.setPages(pages);
        return config;
    }

    private static ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig defaultWorkspaceHeaderPage(String pageKey, String title) {
        ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig page = new ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig();
        page.setTitleTemplate(title);
        page.setTitleFontSize(20);
        page.setTitleFontWeight(800);
        page.setTitleColor("#102a43");
        page.setSidebarEnabled(true);
        page.setSidebarDefaultExpanded(true);
        String statusAlign = "audit".equals(pageKey) ? "left" : "center";
        page.setStatusAlign(statusAlign);
        page.setStatusCollapseOnOverflow(true);
        page.setComponentGap(10);
        page.setRowMinHeight(36);
        page.setPaddingTop(10);
        page.setPaddingBottom(10);
        page.setPaddingInline(14);
        page.setMarginTop(0);
        page.setMarginBottom(0);
        page.setNavigatorToggleButton(workspaceHeaderButton("展开左侧筛选", "收起左侧筛选", "展开左侧筛选", "收起左侧筛选", "left"));
        page.setColumnWidthResetButton(workspaceHeaderButton("恢复默认列宽", "", "恢复默认列宽", "", "right"));
        page.setSelectAllButton(workspaceHeaderActionButton(
            "全选",
            "review".equals(pageKey) ? "全取消" : "取消全选",
            "review".equals(pageKey) ? "选择当前类别全部评分项目" : "选择当前页可提交项目",
            "review".equals(pageKey) ? "取消选择当前类别全部评分项目" : "取消选择当前页项目",
            "left",
            true,
            false
        ));
        page.setUnifiedSubmitButton(workspaceHeaderActionButton(
            "review".equals(pageKey) ? "统一提交签字" : "统一提交",
            "",
            "review".equals(pageKey) ? "检查当前类别评分并进入评分表签字" : "提交当前选中的项目",
            "",
            "right",
            true,
            true
        ));
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonConfig navigationButton =
            workspaceHeaderActionButton("跳转", "", "打开目标页面", "", "right", false, false);
        navigationButton.setTargetMode("preset");
        navigationButton.setPresetKey("schoolSubmit".equals(pageKey) ? "schoolSubmit" : "home");
        navigationButton.setCustomUrl("");
        navigationButton.setOpenMode("current");
        page.setNavigationButton(navigationButton);
        page.setHomeButton(workspaceHeaderActionButton("首页", "", "返回首页", "", "right", false, false));
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonStyleConfig buttonStyle = new ArtDetailDisplayConfigVo.WorkspaceHeaderButtonStyleConfig();
        buttonStyle.setFontSize(14);
        buttonStyle.setFontWeight(400);
        buttonStyle.setIconSize(14);
        buttonStyle.setHeight(32);
        page.setButtonStyle(buttonStyle);
        page.setItemConfigs(defaultWorkspaceHeaderItemConfigs(pageKey, statusAlign));
        List<ArtDetailDisplayConfigVo.WorkspaceHeaderStatusItemConfig> statusItems = new ArrayList<>();
        int statusOrder = 1;
        for (String statusKey : page.getItemConfigs().get("status").getTexts().keySet()) {
            ArtDetailDisplayConfigVo.WorkspaceHeaderStatusItemConfig item =
                new ArtDetailDisplayConfigVo.WorkspaceHeaderStatusItemConfig();
            item.setKey(statusKey);
            item.setVisible(true);
            item.setOrder(statusOrder++);
            statusItems.add(item);
        }
        page.setStatusItems(statusItems);
        page.setSpacerInstances(new LinkedHashMap<>());
        page.setCompactGroupInstances(new LinkedHashMap<>());
        List<String> filterItems = "audit".equals(pageKey)
            ? List.of("status", "categoryFilter", "groupFilter", "schoolFilter", "progress")
            : List.of("categoryFilter", "groupFilter", "progress", "status");
        page.setCards("project".equals(pageKey)
            ? new ArrayList<>(List.of(workspaceHeaderCard("primary", List.of(List.of("title", "actions")))))
            : "projectView".equals(pageKey)
                ? new ArrayList<>(List.of(workspaceHeaderCard("filters", List.of(List.of("navigatorToggle", "filters", "status", "search", "actions", "columnWidthReset")))))
                : "schoolSubmit".equals(pageKey)
                    ? new ArrayList<>(List.of(
                        workspaceHeaderCard("batch", List.of(List.of(
                            "selectAll", "title", "navigatorToggle", "search", "columnWidthReset", "unifiedSubmit", "home"
                        ))),
                        workspaceHeaderCard("filters", List.of(List.of("categoryFilter", "groupFilter", "status")))
                    ))
                    : "review".equals(pageKey)
                        ? new ArrayList<>(List.of(
                            workspaceHeaderCard("primary", List.of(List.of(
                                "title", "navigatorToggle", "search", "selectAll", "actions", "unifiedSubmit", "columnWidthReset"
                            ))),
                            workspaceHeaderCard("filters", List.of(List.of("reviewScope"), filterItems))
                        ))
                        : new ArrayList<>(List.of(
                            workspaceHeaderCard("primary", List.of(List.of("title", "navigatorToggle", "search", "actions", "columnWidthReset"))),
                            workspaceHeaderCard("filters", List.of(filterItems))
                        )));
        if ("schoolSubmit".equals(pageKey)) {
            String groupId = createWorkspaceHeaderCompactGroupInstance(
                page.getCompactGroupInstances(),
                "fixed",
                4,
                "right",
                10,
                List.of("columnWidthReset", "unifiedSubmit", "home"),
                "none",
                List.of("unifiedSubmit"),
                "更多"
            );
            page.getCards().get(0).getRows().set(0, new ArrayList<>(List.of(
                "selectAll", "title", "navigatorToggle", "search", groupId
            )));
        }
        return page;
    }

    private static ArtDetailDisplayConfigVo.WorkspaceHeaderButtonConfig workspaceHeaderButton(
        String text,
        String alternateText,
        String tooltip,
        String alternateTooltip,
        String align
    ) {
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonConfig button = new ArtDetailDisplayConfigVo.WorkspaceHeaderButtonConfig();
        button.setText(text);
        button.setAlternateText(alternateText);
        button.setTooltip(tooltip);
        button.setAlternateTooltip(alternateTooltip);
        button.setAlign(align);
        return button;
    }

    private static ArtDetailDisplayConfigVo.WorkspaceHeaderButtonConfig workspaceHeaderActionButton(
        String text,
        String alternateText,
        String tooltip,
        String alternateTooltip,
        String align,
        boolean showCount,
        boolean primary
    ) {
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonConfig button =
            workspaceHeaderButton(text, alternateText, tooltip, alternateTooltip, align);
        button.setIconVisible(true);
        button.setVariant("default");
        button.setShowCount(showCount);
        button.setAppearance(workspaceHeaderButtonAppearance(primary));
        return button;
    }

    private static ArtDetailDisplayConfigVo.WorkspaceHeaderButtonAppearanceConfig workspaceHeaderButtonAppearance(boolean primary) {
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonAppearanceConfig appearance =
            new ArtDetailDisplayConfigVo.WorkspaceHeaderButtonAppearanceConfig();
        appearance.setBackgroundColor(primary ? "#2563eb" : "#ffffff");
        appearance.setBackgroundOpacity(100);
        appearance.setTextColor(primary ? "#ffffff" : "#334e68");
        appearance.setBorderVisible(!primary);
        appearance.setBorderColor(primary ? "#2563eb" : "#d8e2ee");
        appearance.setBorderOpacity(100);
        appearance.setBorderWidth(1);
        appearance.setBorderRadius(8);
        appearance.setHoverBackgroundColor(primary ? "#1d4ed8" : "#f4f8ff");
        appearance.setHoverBackgroundOpacity(100);
        appearance.setHoverTextColor(primary ? "#ffffff" : "#2563eb");
        appearance.setHoverBorderColor(primary ? "#1d4ed8" : "#93b4f8");
        appearance.setHoverBorderOpacity(100);
        return appearance;
    }

    private static Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderItemConfig> defaultWorkspaceHeaderItemConfigs(
        String pageKey,
        String statusAlign
    ) {
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderItemConfig> items = new LinkedHashMap<>();
        items.put("title", workspaceHeaderItem("left", "auto", 4, Map.of()));
        items.put("navigatorToggle", workspaceHeaderItem("left", "fixed", 1, Map.of()));
        items.put("filters", workspaceHeaderItem(
            "left",
            "auto",
            3,
            "projectView".equals(pageKey) ? orderedMap(
                "categoryPlaceholder", "筛选分类",
                "allCategories", "全部类别",
                "allUnits", "全部单位",
                "progressSuffix", "上报进度"
            ) : Map.of()
        ));
        items.put("reviewScope", workspaceHeaderItem(
            "left",
            "auto",
            12,
            orderedMap(
                "scopeLabel", "评审范围",
                "allScopeLabel", "全部评审范围",
                "selectPlaceholder", "请选择评审范围"
            )
        ));
        items.put("categoryFilter", workspaceHeaderItem(
            "left",
            "schoolSubmit".equals(pageKey) ? "fixed" : "auto",
            "schoolSubmit".equals(pageKey) ? 2 : 3,
            "schoolSubmit".equals(pageKey)
                ? orderedMap("categoryPlaceholder", "全部类别")
                : orderedMap("categoryLabel", "类别", "categoryPlaceholder", "全部类别")
        ));
        items.put("groupFilter", workspaceHeaderItem(
            "left",
            "auto",
            3,
            "audit".equals(pageKey)
                ? orderedMap("groupLabel", "组别", "groupPlaceholder", "所有组别")
                : "schoolSubmit".equals(pageKey)
                    ? orderedMap("groupPlaceholder", "全部组别")
                    : orderedMap("groupLabel", "组别", "groupPlaceholder", "全部组别")
        ));
        items.put("schoolFilter", workspaceHeaderItem(
            "left", "auto", 3, orderedMap("schoolLabel", "学校", "schoolPlaceholder", "搜索学校")
        ));
        items.put("progress", workspaceHeaderItem(
            "right",
            "fixed",
            3,
            "review".equals(pageKey) ? orderedMap("remainingTemplate", "还有 {pending} 个作品未评分") : Map.of()
        ));
        Map<String, String> statusTexts = "review".equals(pageKey)
            ? orderedMap("all", "全部", "none", "未评分", "draft", "已评分", "submitted", "已签字")
            : "projectView".equals(pageKey)
                ? orderedMap("all", "全部", "draft", "草稿", "school_submitted", "待审核", "audit_passed", "已通过", "returned", "已退回")
                : "schoolSubmit".equals(pageKey)
                    ? orderedMap("all", "全部", "draft", "草稿", "school_submitted", "提交待审核", "audit_passed", "通过", "returned", "退回")
                    : orderedMap("all", "全部", "draft", "草稿", "school_submitted", "待审核", "audit_passed", "通过", "returned", "退回");
        items.put("status", workspaceHeaderItem(statusAlign, "fixed", 3, statusTexts));
        items.put("search", workspaceHeaderItem(
            "left", "fixed", 4, orderedMap("placeholder", "请输入项目名称", "search", "搜索", "reset", "重置")
        ));
        Map<String, String> actionTexts = "project".equals(pageKey)
            ? orderedMap("add", "添加")
            : "audit".equals(pageKey)
                ? orderedMap("settings", "设置", "restoreColumns", "恢复默认", "auditAssignment", "审核权限分配")
            : "review".equals(pageKey)
                ? orderedMap("lockedSignature", "查看已签评分表")
                : Map.of();
        items.put("actions", workspaceHeaderItem("right", "fixed", 2, actionTexts));
        items.put("columnWidthReset", workspaceHeaderItem("right", "fixed", 1, Map.of()));
        items.put("selectAll", workspaceHeaderItem("left", "fixed", 1, Map.of()));
        items.put("unifiedSubmit", workspaceHeaderItem("right", "fixed", 2, Map.of()));
        items.put("navigationButton", workspaceHeaderItem("right", "fixed", 2, Map.of()));
        items.put("home", workspaceHeaderItem("right", "fixed", 1, Map.of()));
        items.put("compactGroup", workspaceHeaderItem("right", "fixed", 4, Map.of()));
        items.put("fixedSpacer", workspaceHeaderItem("left", "fixed", 1, Map.of()));
        items.put("autoSpacer", workspaceHeaderItem("left", "auto", 1, Map.of()));
        return items;
    }

    private static ArtDetailDisplayConfigVo.WorkspaceHeaderItemConfig workspaceHeaderItem(
        String align,
        String widthMode,
        int span,
        Map<String, String> texts
    ) {
        ArtDetailDisplayConfigVo.WorkspaceHeaderItemConfig item = new ArtDetailDisplayConfigVo.WorkspaceHeaderItemConfig();
        item.setAlign(align);
        item.setWidthMode(widthMode);
        item.setSpan(span);
        item.setTexts(new LinkedHashMap<>(texts));
        return item;
    }

    private static ArtDetailDisplayConfigVo.WorkspaceHeaderCardConfig workspaceHeaderCard(String key, List<List<String>> rows) {
        ArtDetailDisplayConfigVo.WorkspaceHeaderCardConfig card = new ArtDetailDisplayConfigVo.WorkspaceHeaderCardConfig();
        card.setKey(key);
        List<List<String>> copiedRows = new ArrayList<>();
        rows.forEach(row -> copiedRows.add(new ArrayList<>(row)));
        card.setRows(copiedRows);
        card.setBackgroundColor("#ffffff");
        card.setBackgroundOpacity(100);
        card.setBorderRadius(8);
        card.setBorderVisible(true);
        return card;
    }

    private static ArtDetailDisplayConfigVo.WorkspaceHeaderConfig normalizeWorkspaceHeader(
        ArtDetailDisplayConfigVo.WorkspaceHeaderConfig source,
        ArtDetailDisplayConfigVo.WorkspaceHeaderConfig defaults
    ) {
        if (source == null || !Objects.equals(source.getLayoutVersion(), WORKSPACE_HEADER_LAYOUT_VERSION)) {
            source = defaults;
        }
        ArtDetailDisplayConfigVo.WorkspaceHeaderConfig target = new ArtDetailDisplayConfigVo.WorkspaceHeaderConfig();
        target.setLayoutVersion(WORKSPACE_HEADER_LAYOUT_VERSION);
        String activityId = source.getManagedActivityId();
        target.setManagedActivityId(activityId == null || activityId.isBlank() ? null : activityId.trim());
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig> pages = new LinkedHashMap<>();
        for (String pageKey : WORKSPACE_HEADER_PAGE_KEYS) {
            ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig sourcePage =
                source.getPages() == null ? null : source.getPages().get(pageKey);
            pages.put(pageKey, normalizeWorkspaceHeaderPage(pageKey, sourcePage, defaults.getPages().get(pageKey)));
        }
        target.setPages(pages);
        return target;
    }

    private static ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig normalizeWorkspaceHeaderPage(
        String pageKey,
        ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig source,
        ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig defaults
    ) {
        ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig target = new ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig();
        target.setTitleTemplate(template(source == null ? null : source.getTitleTemplate(), defaults.getTitleTemplate(), 120));
        target.setTitleFontSize(number(source == null ? null : source.getTitleFontSize(), defaults.getTitleFontSize(), 14, 42));
        Integer titleFontWeight = source == null ? null : source.getTitleFontWeight();
        target.setTitleFontWeight(titleFontWeight != null && List.of(400, 500, 600, 700, 800, 900).contains(titleFontWeight)
            ? titleFontWeight
            : defaults.getTitleFontWeight());
        target.setTitleColor(color(source == null ? null : source.getTitleColor(), defaults.getTitleColor()));
        target.setSidebarEnabled(bool(source == null ? null : source.getSidebarEnabled(), defaults.getSidebarEnabled()));
        target.setSidebarDefaultExpanded(bool(
            source == null ? null : source.getSidebarDefaultExpanded(),
            defaults.getSidebarDefaultExpanded()
        ));
        target.setStatusCollapseOnOverflow(bool(
            source == null ? null : source.getStatusCollapseOnOverflow(),
            defaults.getStatusCollapseOnOverflow()
        ));
        target.setStatusItems(normalizeWorkspaceHeaderStatusItems(source, defaults));
        target.setComponentGap(number(source == null ? null : source.getComponentGap(), defaults.getComponentGap(), 0, 32));
        target.setRowMinHeight(number(source == null ? null : source.getRowMinHeight(), defaults.getRowMinHeight(), 28, 96));
        target.setPaddingTop(number(source == null ? null : source.getPaddingTop(), defaults.getPaddingTop(), 0, 32));
        target.setPaddingBottom(number(source == null ? null : source.getPaddingBottom(), defaults.getPaddingBottom(), 0, 32));
        target.setPaddingInline(number(source == null ? null : source.getPaddingInline(), defaults.getPaddingInline(), 0, 40));
        target.setMarginTop(number(source == null ? null : source.getMarginTop(), defaults.getMarginTop(), 0, 40));
        target.setMarginBottom(number(source == null ? null : source.getMarginBottom(), defaults.getMarginBottom(), 0, 40));
        target.setNavigatorToggleButton(normalizeWorkspaceHeaderButton(
            source == null ? null : source.getNavigatorToggleButton(), defaults.getNavigatorToggleButton(), true));
        target.setColumnWidthResetButton(normalizeWorkspaceHeaderButton(
            source == null ? null : source.getColumnWidthResetButton(), defaults.getColumnWidthResetButton(), false));
        target.setSelectAllButton(normalizeWorkspaceHeaderButton(
            source == null ? null : source.getSelectAllButton(), defaults.getSelectAllButton(), true));
        target.setUnifiedSubmitButton(normalizeWorkspaceHeaderButton(
            source == null ? null : source.getUnifiedSubmitButton(), defaults.getUnifiedSubmitButton(), false));
        target.setNavigationButton(normalizeWorkspaceHeaderNavigationButton(
            source == null ? null : source.getNavigationButton(), defaults.getNavigationButton()));
        target.setHomeButton(normalizeWorkspaceHeaderButton(
            source == null ? null : source.getHomeButton(), defaults.getHomeButton(), false));
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonStyleConfig sourceButtonStyle = source == null ? null : source.getButtonStyle();
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonStyleConfig buttonStyle = new ArtDetailDisplayConfigVo.WorkspaceHeaderButtonStyleConfig();
        buttonStyle.setFontSize(number(sourceButtonStyle == null ? null : sourceButtonStyle.getFontSize(), defaults.getButtonStyle().getFontSize(), 12, 18));
        Integer fontWeight = sourceButtonStyle == null ? null : sourceButtonStyle.getFontWeight();
        buttonStyle.setFontWeight(fontWeight != null && List.of(400, 500, 600, 700).contains(fontWeight)
            ? fontWeight
            : defaults.getButtonStyle().getFontWeight());
        buttonStyle.setIconSize(number(sourceButtonStyle == null ? null : sourceButtonStyle.getIconSize(), defaults.getButtonStyle().getIconSize(), 12, 24));
        buttonStyle.setHeight(number(sourceButtonStyle == null ? null : sourceButtonStyle.getHeight(), defaults.getButtonStyle().getHeight(), 24, 44));
        target.setButtonStyle(buttonStyle);
        String normalizedStatusAlign = allowedValue(
            source == null ? null : source.getStatusAlign(),
            WORKSPACE_HEADER_STATUS_ALIGN_KEYS,
            defaults.getStatusAlign()
        );
        boolean legacyReviewScopeLayout = "review".equals(pageKey)
            && (source == null || source.getItemConfigs() == null || !source.getItemConfigs().containsKey("reviewScope"));
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderItemConfig> itemConfigs =
            normalizeWorkspaceHeaderItemConfigs(source, defaults, normalizedStatusAlign);
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderSpacerInstanceConfig> spacerInstances =
            normalizeWorkspaceHeaderSpacerInstances(source);
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig> compactGroupInstances =
            normalizeWorkspaceHeaderCompactGroupInstances(source);
        defaults.getCompactGroupInstances().forEach((instanceId, config) ->
            compactGroupInstances.putIfAbsent(instanceId, copyWorkspaceHeaderCompactGroup(config)));
        List<ArtDetailDisplayConfigVo.WorkspaceHeaderCardConfig> sourceCards = source == null ? null : source.getCards();
        if (sourceCards == null || sourceCards.isEmpty()) {
            sourceCards = defaults.getCards();
        }
        List<ArtDetailDisplayConfigVo.WorkspaceHeaderCardConfig> cards = new ArrayList<>();
        for (int index = 0; index < Math.min(sourceCards.size(), 6); index++) {
            ArtDetailDisplayConfigVo.WorkspaceHeaderCardConfig fallback = defaults.getCards().get(Math.min(index, defaults.getCards().size() - 1));
            cards.add(normalizeWorkspaceHeaderCard(
                sourceCards.get(index),
                fallback,
                index,
                spacerInstances,
                compactGroupInstances
            ));
        }
        Set<String> usedItems = new LinkedHashSet<>();
        Set<String> usedSpacerInstances = new LinkedHashSet<>();
        Set<String> usedCompactGroupInstances = new LinkedHashSet<>();
        for (ArtDetailDisplayConfigVo.WorkspaceHeaderCardConfig card : cards) {
            List<List<String>> uniqueRows = new ArrayList<>();
            for (List<String> row : card.getRows()) {
                List<String> uniqueRow = new ArrayList<>();
                for (String itemId : row) {
                    String itemKey = workspaceHeaderItemKey(itemId, spacerInstances, compactGroupInstances);
                    if (WORKSPACE_HEADER_SPACER_KEYS.contains(itemKey)) {
                        if (usedSpacerInstances.add(itemId)) {
                            uniqueRow.add(itemId);
                        } else {
                            ArtDetailDisplayConfigVo.WorkspaceHeaderSpacerInstanceConfig sourceSpacer = spacerInstances.get(itemId);
                            String cloneId = createWorkspaceHeaderSpacerInstance(
                                spacerInstances,
                                sourceSpacer.getType(),
                                sourceSpacer.getSpan(),
                                sourceSpacer.getAllowStatusBorrow()
                            );
                            usedSpacerInstances.add(cloneId);
                            uniqueRow.add(cloneId);
                        }
                    } else if ("compactGroup".equals(itemKey)) {
                        ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig sourceGroup = compactGroupInstances.get(itemId);
                        if (sourceGroup == null) {
                            continue;
                        }
                        String groupId = itemId;
                        if (usedCompactGroupInstances.contains(groupId)) {
                            groupId = createWorkspaceHeaderCompactGroupInstance(
                                compactGroupInstances,
                                sourceGroup.getWidthMode(),
                                sourceGroup.getSpan(),
                                sourceGroup.getAlign(),
                                sourceGroup.getGap(),
                                sourceGroup.getItems(),
                                sourceGroup.getCollapseMode(),
                                sourceGroup.getPinnedItems(),
                                sourceGroup.getCollapseText()
                            );
                        }
                        ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig group = compactGroupInstances.get(groupId);
                        List<String> groupItems = new ArrayList<>();
                        for (String childKey : group.getItems()) {
                            if (WORKSPACE_HEADER_BUSINESS_ITEM_KEYS.contains(childKey) && usedItems.add(childKey)) {
                                groupItems.add(childKey);
                            }
                        }
                        group.setItems(groupItems);
                        group.setPinnedItems(group.getPinnedItems().stream()
                            .filter(groupItems::contains)
                            .filter(WORKSPACE_HEADER_COLLAPSIBLE_BUTTON_KEYS::contains)
                            .distinct()
                            .toList());
                        if (!groupItems.isEmpty()) {
                            usedCompactGroupInstances.add(groupId);
                            uniqueRow.add(groupId);
                        }
                    } else if (usedItems.add(itemKey)) {
                        uniqueRow.add(itemId);
                    }
                }
                uniqueRows.add(uniqueRow);
            }
            card.setRows(uniqueRows);
        }
        if (legacyReviewScopeLayout && usedItems.add("reviewScope") && !cards.isEmpty()) {
            ArtDetailDisplayConfigVo.WorkspaceHeaderCardConfig filterCard = cards.stream()
                .filter(card -> "filters".equals(card.getKey()))
                .findFirst()
                .orElse(cards.get(cards.size() - 1));
            List<List<String>> rows = filterCard.getRows();
            if (rows.size() < 2) {
                rows.add(new ArrayList<>());
            }
            if (rows.get(1).isEmpty()) {
                rows.set(1, new ArrayList<>(rows.get(0)));
                rows.set(0, new ArrayList<>(List.of("reviewScope")));
            } else {
                rows.get(0).add(0, "reviewScope");
            }
        }
        spacerInstances.keySet().removeIf(instanceId -> !usedSpacerInstances.contains(instanceId));
        compactGroupInstances.keySet().removeIf(instanceId -> !usedCompactGroupInstances.contains(instanceId));
        target.setStatusAlign(normalizedStatusAlign);
        target.setItemConfigs(itemConfigs);
        target.setSpacerInstances(spacerInstances);
        target.setCompactGroupInstances(compactGroupInstances);
        target.setCards(cards);
        return target;
    }

    private static List<ArtDetailDisplayConfigVo.WorkspaceHeaderStatusItemConfig> normalizeWorkspaceHeaderStatusItems(
        ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig source,
        ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig defaults
    ) {
        List<ArtDetailDisplayConfigVo.WorkspaceHeaderStatusItemConfig> sourceItems =
            source == null || source.getStatusItems() == null ? defaults.getStatusItems() : source.getStatusItems();
        Set<String> knownKeys = defaults.getStatusItems().stream()
            .map(ArtDetailDisplayConfigVo.WorkspaceHeaderStatusItemConfig::getKey)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<ArtDetailDisplayConfigVo.WorkspaceHeaderStatusItemConfig> result = new ArrayList<>();
        Set<String> usedKeys = new LinkedHashSet<>();
        int index = 0;
        for (ArtDetailDisplayConfigVo.WorkspaceHeaderStatusItemConfig sourceItem : sourceItems) {
            if (sourceItem == null || !knownKeys.contains(sourceItem.getKey()) || !usedKeys.add(sourceItem.getKey())) {
                continue;
            }
            ArtDetailDisplayConfigVo.WorkspaceHeaderStatusItemConfig item =
                new ArtDetailDisplayConfigVo.WorkspaceHeaderStatusItemConfig();
            item.setKey(sourceItem.getKey());
            item.setVisible(!Boolean.FALSE.equals(sourceItem.getVisible()));
            item.setOrder(number(sourceItem.getOrder(), ++index, 1, 99));
            result.add(item);
        }
        for (ArtDetailDisplayConfigVo.WorkspaceHeaderStatusItemConfig fallback : defaults.getStatusItems()) {
            if (usedKeys.add(fallback.getKey())) {
                ArtDetailDisplayConfigVo.WorkspaceHeaderStatusItemConfig item =
                    new ArtDetailDisplayConfigVo.WorkspaceHeaderStatusItemConfig();
                item.setKey(fallback.getKey());
                item.setVisible(fallback.getVisible());
                item.setOrder(fallback.getOrder());
                result.add(item);
            }
        }
        return result;
    }

    private static Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderItemConfig> normalizeWorkspaceHeaderItemConfigs(
        ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig source,
        ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig defaults,
        String normalizedStatusAlign
    ) {
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderItemConfig> result = new LinkedHashMap<>();
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderItemConfig> sourceItems = source == null ? null : source.getItemConfigs();
        for (String itemKey : WORKSPACE_HEADER_ITEM_KEYS) {
            ArtDetailDisplayConfigVo.WorkspaceHeaderItemConfig fallback = defaults.getItemConfigs().get(itemKey);
            ArtDetailDisplayConfigVo.WorkspaceHeaderItemConfig sourceItem = sourceItems == null ? null : sourceItems.get(itemKey);
            String sourceAlign = sourceItem == null
                ? ("status".equals(itemKey) ? normalizedStatusAlign : null)
                : sourceItem.getAlign();
            ArtDetailDisplayConfigVo.WorkspaceHeaderItemConfig targetItem = new ArtDetailDisplayConfigVo.WorkspaceHeaderItemConfig();
            targetItem.setAlign(allowedValue(
                sourceAlign,
                WORKSPACE_HEADER_STATUS_ALIGN_KEYS,
                fallback.getAlign()
            ));
            targetItem.setWidthMode(allowedValue(
                sourceItem == null ? null : sourceItem.getWidthMode(),
                WORKSPACE_HEADER_WIDTH_MODE_KEYS,
                fallback.getWidthMode()
            ));
            targetItem.setSpan(number(sourceItem == null ? null : sourceItem.getSpan(), fallback.getSpan(), 1, 12));
            Map<String, String> texts = new LinkedHashMap<>();
            Map<String, String> sourceTexts = sourceItem == null ? null : sourceItem.getTexts();
            fallback.getTexts().forEach((textKey, defaultText) -> texts.put(
                textKey,
                optionalText(
                    sourceTexts == null ? null : sourceTexts.get(textKey),
                    defaultText,
                    80
                )
            ));
            targetItem.setTexts(texts);
            result.put(itemKey, targetItem);
        }
        return result;
    }

    private static Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderSpacerInstanceConfig> normalizeWorkspaceHeaderSpacerInstances(
        ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig source
    ) {
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderSpacerInstanceConfig> result = new LinkedHashMap<>();
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderSpacerInstanceConfig> sourceInstances =
            source == null ? null : source.getSpacerInstances();
        if (sourceInstances == null) {
            return result;
        }
        sourceInstances.forEach((instanceId, sourceInstance) -> {
            if (instanceId == null || !instanceId.matches("[A-Za-z][A-Za-z0-9_-]{0,47}")
                || sourceInstance == null || !WORKSPACE_HEADER_SPACER_KEYS.contains(sourceInstance.getType())) {
                return;
            }
            ArtDetailDisplayConfigVo.WorkspaceHeaderSpacerInstanceConfig targetInstance =
                new ArtDetailDisplayConfigVo.WorkspaceHeaderSpacerInstanceConfig();
            targetInstance.setType(sourceInstance.getType());
            targetInstance.setSpan("fixedSpacer".equals(sourceInstance.getType())
                ? number(sourceInstance.getSpan(), 1, 1, 12)
                : 1);
            targetInstance.setAllowStatusBorrow(
                "autoSpacer".equals(sourceInstance.getType())
                    && Boolean.TRUE.equals(sourceInstance.getAllowStatusBorrow())
            );
            result.put(instanceId, targetInstance);
        });
        return result;
    }

    private static Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig> normalizeWorkspaceHeaderCompactGroupInstances(
        ArtDetailDisplayConfigVo.WorkspaceHeaderPageConfig source
    ) {
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig> result = new LinkedHashMap<>();
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig> sourceInstances =
            source == null ? null : source.getCompactGroupInstances();
        if (sourceInstances == null) {
            return result;
        }
        sourceInstances.forEach((instanceId, sourceInstance) -> {
            if (instanceId == null || !instanceId.matches("[A-Za-z][A-Za-z0-9_-]{0,47}") || sourceInstance == null) {
                return;
            }
            ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig target =
                new ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig();
            target.setWidthMode(allowedValue(sourceInstance.getWidthMode(), WORKSPACE_HEADER_WIDTH_MODE_KEYS, "fixed"));
            target.setSpan(number(sourceInstance.getSpan(), 4, 1, 12));
            target.setAlign(allowedValue(sourceInstance.getAlign(), WORKSPACE_HEADER_STATUS_ALIGN_KEYS, "right"));
            target.setGap(number(sourceInstance.getGap(), 10, 0, 32));
            List<String> items = sourceInstance.getItems() == null
                ? List.of()
                : sourceInstance.getItems().stream()
                    .filter(WORKSPACE_HEADER_BUSINESS_ITEM_KEYS::contains)
                    .distinct()
                    .limit(8)
                    .toList();
            target.setItems(new ArrayList<>(items));
            target.setCollapseMode(allowedValue(
                sourceInstance.getCollapseMode(),
                WORKSPACE_HEADER_COMPACT_COLLAPSE_MODE_KEYS,
                "none"
            ));
            List<String> sourcePinnedItems = sourceInstance.getPinnedItems() == null
                ? (items.contains("unifiedSubmit") ? List.of("unifiedSubmit") : List.of())
                : sourceInstance.getPinnedItems();
            target.setPinnedItems(sourcePinnedItems.stream()
                .filter(items::contains)
                .filter(WORKSPACE_HEADER_COLLAPSIBLE_BUTTON_KEYS::contains)
                .distinct()
                .toList());
            target.setCollapseText(text(sourceInstance.getCollapseText(), "更多", 10));
            result.put(instanceId, target);
        });
        return result;
    }

    private static ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig copyWorkspaceHeaderCompactGroup(
        ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig source
    ) {
        ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig target =
            new ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig();
        target.setWidthMode(source.getWidthMode());
        target.setSpan(source.getSpan());
        target.setAlign(source.getAlign());
        target.setGap(source.getGap());
        target.setItems(new ArrayList<>(source.getItems()));
        target.setCollapseMode(source.getCollapseMode());
        target.setPinnedItems(new ArrayList<>(source.getPinnedItems()));
        target.setCollapseText(source.getCollapseText());
        return target;
    }

    private static String createWorkspaceHeaderCompactGroupInstance(
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig> compactGroupInstances,
        String widthMode,
        Integer span,
        String align,
        Integer gap,
        List<String> items,
        String collapseMode,
        List<String> pinnedItems,
        String collapseText
    ) {
        int sequence = 1;
        String instanceId = "compactGroup-" + sequence;
        while (compactGroupInstances.containsKey(instanceId)) {
            sequence++;
            instanceId = "compactGroup-" + sequence;
        }
        ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig group =
            new ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig();
        group.setWidthMode(allowedValue(widthMode, WORKSPACE_HEADER_WIDTH_MODE_KEYS, "fixed"));
        group.setSpan(number(span, 4, 1, 12));
        group.setAlign(allowedValue(align, WORKSPACE_HEADER_STATUS_ALIGN_KEYS, "right"));
        group.setGap(number(gap, 10, 0, 32));
        group.setItems(new ArrayList<>(items == null ? List.of() : items));
        group.setCollapseMode(allowedValue(collapseMode, WORKSPACE_HEADER_COMPACT_COLLAPSE_MODE_KEYS, "none"));
        group.setPinnedItems(new ArrayList<>(pinnedItems == null ? List.of() : pinnedItems));
        group.setCollapseText(text(collapseText, "更多", 10));
        compactGroupInstances.put(instanceId, group);
        return instanceId;
    }

    private static String createWorkspaceHeaderSpacerInstance(
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderSpacerInstanceConfig> spacerInstances,
        String type,
        Integer span,
        Boolean allowStatusBorrow
    ) {
        int sequence = 1;
        String instanceId = type + "-" + sequence;
        while (spacerInstances.containsKey(instanceId)) {
            sequence++;
            instanceId = type + "-" + sequence;
        }
        ArtDetailDisplayConfigVo.WorkspaceHeaderSpacerInstanceConfig instance =
            new ArtDetailDisplayConfigVo.WorkspaceHeaderSpacerInstanceConfig();
        instance.setType(type);
        instance.setSpan("fixedSpacer".equals(type) ? number(span, 1, 1, 12) : 1);
        instance.setAllowStatusBorrow(
            "autoSpacer".equals(type) && Boolean.TRUE.equals(allowStatusBorrow)
        );
        spacerInstances.put(instanceId, instance);
        return instanceId;
    }

    private static String workspaceHeaderItemKey(
        String itemId,
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderSpacerInstanceConfig> spacerInstances,
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig> compactGroupInstances
    ) {
        ArtDetailDisplayConfigVo.WorkspaceHeaderSpacerInstanceConfig spacer = spacerInstances.get(itemId);
        if (spacer != null) {
            return spacer.getType();
        }
        return compactGroupInstances.containsKey(itemId) ? "compactGroup" : itemId;
    }

    private static ArtDetailDisplayConfigVo.WorkspaceHeaderButtonConfig normalizeWorkspaceHeaderButton(
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonConfig source,
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonConfig defaults,
        boolean alternate
    ) {
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonConfig target = new ArtDetailDisplayConfigVo.WorkspaceHeaderButtonConfig();
        target.setText(optionalText(source == null ? null : source.getText(), defaults.getText(), 30));
        target.setAlternateText(alternate ? optionalText(source == null ? null : source.getAlternateText(), defaults.getAlternateText(), 30) : "");
        target.setTooltip(text(source == null ? null : source.getTooltip(), defaults.getTooltip(), 50));
        target.setAlternateTooltip(alternate ? text(source == null ? null : source.getAlternateTooltip(), defaults.getAlternateTooltip(), 50) : "");
        target.setAlign(allowedValue(source == null ? null : source.getAlign(), WORKSPACE_HEADER_STATUS_ALIGN_KEYS, defaults.getAlign()));
        target.setIconVisible(bool(source == null ? null : source.getIconVisible(), defaults.getIconVisible()));
        target.setVariant(allowedValue(
            source == null ? null : source.getVariant(),
            WORKSPACE_HEADER_BUTTON_VARIANT_KEYS,
            defaults.getVariant()
        ));
        target.setShowCount(bool(source == null ? null : source.getShowCount(), defaults.getShowCount()));
        if (defaults.getAppearance() != null) {
            target.setAppearance(normalizeWorkspaceHeaderButtonAppearance(
                source == null ? null : source.getAppearance(),
                defaults.getAppearance()
            ));
        }
        return target;
    }

    private static ArtDetailDisplayConfigVo.WorkspaceHeaderButtonConfig normalizeWorkspaceHeaderNavigationButton(
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonConfig source,
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonConfig defaults
    ) {
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonConfig target =
            normalizeWorkspaceHeaderButton(source, defaults, false);
        target.setTargetMode("custom".equals(source == null ? null : source.getTargetMode()) ? "custom" : "preset");
        target.setPresetKey(allowedValue(
            source == null ? null : source.getPresetKey(),
            WORKSPACE_NAVIGATION_PRESET_KEYS,
            defaults.getPresetKey()
        ));
        target.setCustomUrl(normalizeWorkspaceNavigationUrl(source == null ? null : source.getCustomUrl()));
        target.setOpenMode("new".equals(source == null ? null : source.getOpenMode()) ? "new" : "current");
        return target;
    }

    private static String normalizeWorkspaceNavigationUrl(String value) {
        String url = value == null ? "" : value.trim();
        if (url.isEmpty() || url.chars().anyMatch(Character::isWhitespace)) {
            return "";
        }
        if (url.startsWith("/") && !url.startsWith("//")) {
            return url.length() <= 500 ? url : "";
        }
        try {
            URI parsed = URI.create(url);
            String scheme = parsed.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && url.length() <= 500
                ? url
                : "";
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    private static ArtDetailDisplayConfigVo.WorkspaceHeaderButtonAppearanceConfig normalizeWorkspaceHeaderButtonAppearance(
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonAppearanceConfig source,
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonAppearanceConfig defaults
    ) {
        ArtDetailDisplayConfigVo.WorkspaceHeaderButtonAppearanceConfig target =
            new ArtDetailDisplayConfigVo.WorkspaceHeaderButtonAppearanceConfig();
        target.setBackgroundColor(color(source == null ? null : source.getBackgroundColor(), defaults.getBackgroundColor()));
        target.setBackgroundOpacity(number(
            source == null ? null : source.getBackgroundOpacity(), defaults.getBackgroundOpacity(), 0, 100));
        target.setTextColor(color(source == null ? null : source.getTextColor(), defaults.getTextColor()));
        target.setBorderVisible(bool(source == null ? null : source.getBorderVisible(), defaults.getBorderVisible()));
        target.setBorderColor(color(source == null ? null : source.getBorderColor(), defaults.getBorderColor()));
        target.setBorderOpacity(number(source == null ? null : source.getBorderOpacity(), defaults.getBorderOpacity(), 0, 100));
        target.setBorderWidth(number(source == null ? null : source.getBorderWidth(), defaults.getBorderWidth(), 0, 6));
        target.setBorderRadius(number(source == null ? null : source.getBorderRadius(), defaults.getBorderRadius(), 0, 24));
        target.setHoverBackgroundColor(color(
            source == null ? null : source.getHoverBackgroundColor(), defaults.getHoverBackgroundColor()));
        target.setHoverBackgroundOpacity(number(
            source == null ? null : source.getHoverBackgroundOpacity(), defaults.getHoverBackgroundOpacity(), 0, 100));
        target.setHoverTextColor(color(source == null ? null : source.getHoverTextColor(), defaults.getHoverTextColor()));
        target.setHoverBorderColor(color(source == null ? null : source.getHoverBorderColor(), defaults.getHoverBorderColor()));
        target.setHoverBorderOpacity(number(
            source == null ? null : source.getHoverBorderOpacity(), defaults.getHoverBorderOpacity(), 0, 100));
        return target;
    }

    private static ArtDetailDisplayConfigVo.WorkspaceHeaderCardConfig normalizeWorkspaceHeaderCard(
        ArtDetailDisplayConfigVo.WorkspaceHeaderCardConfig source,
        ArtDetailDisplayConfigVo.WorkspaceHeaderCardConfig defaults,
        int index,
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderSpacerInstanceConfig> spacerInstances,
        Map<String, ArtDetailDisplayConfigVo.WorkspaceHeaderCompactGroupInstanceConfig> compactGroupInstances
    ) {
        ArtDetailDisplayConfigVo.WorkspaceHeaderCardConfig target = new ArtDetailDisplayConfigVo.WorkspaceHeaderCardConfig();
        target.setKey(text(source == null ? null : source.getKey(), defaults.getKey() == null ? "card-" + (index + 1) : defaults.getKey(), 40));
        List<List<String>> rows = new ArrayList<>();
        List<List<String>> sourceRows = source == null ? null : source.getRows();
        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            List<String> row = sourceRows != null && sourceRows.size() > rowIndex ? sourceRows.get(rowIndex) : List.of();
            List<String> normalizedRow = new ArrayList<>();
            for (String itemId : row) {
                if (normalizedRow.size() >= 12) {
                    break;
                }
                if (WORKSPACE_HEADER_BUSINESS_ITEM_KEYS.contains(itemId)) {
                    normalizedRow.add(itemId);
                } else if (spacerInstances.containsKey(itemId)) {
                    normalizedRow.add(itemId);
                } else if (compactGroupInstances.containsKey(itemId)) {
                    normalizedRow.add(itemId);
                }
            }
            rows.add(normalizedRow);
        }
        if (rows.stream().allMatch(List::isEmpty)) {
            rows.clear();
            for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
                List<String> row = defaults.getRows().size() > rowIndex ? defaults.getRows().get(rowIndex) : List.of();
                rows.add(new ArrayList<>(row));
            }
        }
        target.setRows(rows);
        target.setBackgroundColor(color(source == null ? null : source.getBackgroundColor(), defaults.getBackgroundColor()));
        target.setBackgroundOpacity(number(source == null ? null : source.getBackgroundOpacity(), defaults.getBackgroundOpacity(), 0, 100));
        target.setBorderRadius(number(source == null ? null : source.getBorderRadius(), defaults.getBorderRadius(), 0, 24));
        target.setBorderVisible(bool(source == null ? null : source.getBorderVisible(), defaults.getBorderVisible()));
        return target;
    }

    private static ArtDetailDisplayConfigVo.ListTableAppearanceConfig defaultListTableAppearance() {
        ArtDetailDisplayConfigVo.ListTableAppearanceConfig config = new ArtDetailDisplayConfigVo.ListTableAppearanceConfig();
        config.setGlobalColorOverride(true);
        config.setHeaderBackground("#eef4ff");
        config.setHeaderTextColor("#1f3b64");
        config.setTextColor("#334155");
        config.setHoverBackground("#f8fbff");
        config.setHoverBackgroundOpacity(100);
        config.setBorderColor("#e8eef6");
        config.setHorizontalBorderVisible(true);
        config.setHorizontalBorderColor("#e8eef6");
        config.setHorizontalBorderOpacity(100);
        config.setVerticalBorderVisible(false);
        config.setVerticalBorderColor("#e8eef6");
        config.setVerticalBorderOpacity(0);
        config.setOuterBorderVisible(true);
        config.setOuterBorderColor("#e8eef6");
        config.setOuterBorderOpacity(60);
        config.setHeaderFontSize(14);
        config.setBodyFontSize(14);
        config.setButtonFontSize(14);
        config.setRowHeight(44);
        config.setBorderRadius(8);
        config.setBorderMode("horizontal");
        config.setWrapMode("wrap");
        config.setHeaderWrap(false);
        config.setStriped(false);
        config.setActionStyle("link");
        config.setStatusAppearance(defaultListStatusAppearance());
        config.setRowActionAppearance(defaultListActionAppearance());
        return config;
    }

    private static ArtDetailDisplayConfigVo.ListStatusAppearanceConfig defaultListStatusAppearance() {
        ArtDetailDisplayConfigVo.ListStatusAppearanceConfig config = new ArtDetailDisplayConfigVo.ListStatusAppearanceConfig();
        config.setBorderVisible(true);
        config.setIconVisible(false);
        config.setUniformWidth(true);
        config.setColors(orderedMap(
            "draft", "#909399",
            "pending", "#e6a23c",
            "approved", "#67c23a",
            "returned", "#f56c6c",
            "unscored", "#f56c6c",
            "scored", "#67c23a",
            "scoreDraft", "#909399",
            "locked", "#e6a23c"
        ));
        return config;
    }

    private static ArtDetailDisplayConfigVo.ListActionAppearanceConfig defaultListActionAppearance() {
        ArtDetailDisplayConfigVo.ListActionAppearanceConfig config = new ArtDetailDisplayConfigVo.ListActionAppearanceConfig();
        config.setBorderVisible(false);
        config.setIconVisible(true);
        config.setUniformWidth(true);
        config.setDisabledOpacity(36);
        config.setColors(orderedMap(
            "view", "#2563eb",
            "score", "#2563eb",
            "edit", "#2563eb",
            "return", "#f56c6c",
            "withdraw", "#e6a23c",
            "submit", "#67c23a",
            "delete", "#f56c6c"
        ));
        return config;
    }

    private static ArtDetailDisplayConfigVo.ListTableAppearanceConfig normalizeListTableAppearance(
        ArtDetailDisplayConfigVo.ListTableAppearanceConfig source,
        ArtDetailDisplayConfigVo.ListTableAppearanceConfig defaults
    ) {
        ArtDetailDisplayConfigVo.ListTableAppearanceConfig target = new ArtDetailDisplayConfigVo.ListTableAppearanceConfig();
        target.setGlobalColorOverride(bool(
            source == null ? null : source.getGlobalColorOverride(),
            defaults.getGlobalColorOverride()
        ));
        String legacyBorderMode = allowedValue(source == null ? null : source.getBorderMode(), TABLE_BORDER_MODE_KEYS, defaults.getBorderMode());
        Boolean horizontalBorderVisible = bool(
            source == null ? null : source.getHorizontalBorderVisible(),
            !"none".equals(legacyBorderMode)
        );
        Boolean verticalBorderVisible = bool(
            source == null ? null : source.getVerticalBorderVisible(),
            "grid".equals(legacyBorderMode)
        );
        target.setHeaderBackground(color(source == null ? null : source.getHeaderBackground(), defaults.getHeaderBackground()));
        target.setHeaderTextColor(color(source == null ? null : source.getHeaderTextColor(), defaults.getHeaderTextColor()));
        target.setTextColor(color(source == null ? null : source.getTextColor(), defaults.getTextColor()));
        target.setHoverBackground(color(source == null ? null : source.getHoverBackground(), defaults.getHoverBackground()));
        target.setHoverBackgroundOpacity(number(source == null ? null : source.getHoverBackgroundOpacity(), defaults.getHoverBackgroundOpacity(), 0, 100));
        target.setBorderColor(color(source == null ? null : source.getBorderColor(), defaults.getBorderColor()));
        target.setHorizontalBorderVisible(horizontalBorderVisible);
        target.setHorizontalBorderColor(color(
            source == null ? null : source.getHorizontalBorderColor(),
            source == null ? defaults.getHorizontalBorderColor() : color(source.getBorderColor(), defaults.getHorizontalBorderColor())
        ));
        target.setHorizontalBorderOpacity(number(source == null ? null : source.getHorizontalBorderOpacity(), defaults.getHorizontalBorderOpacity(), 0, 100));
        target.setVerticalBorderVisible(verticalBorderVisible);
        target.setVerticalBorderColor(color(
            source == null ? null : source.getVerticalBorderColor(),
            source == null ? defaults.getVerticalBorderColor() : color(source.getBorderColor(), defaults.getVerticalBorderColor())
        ));
        target.setVerticalBorderOpacity(number(
            source == null ? null : source.getVerticalBorderOpacity(),
            verticalBorderVisible ? 100 : defaults.getVerticalBorderOpacity(),
            0,
            100
        ));
        target.setOuterBorderVisible(bool(source == null ? null : source.getOuterBorderVisible(), !"none".equals(legacyBorderMode)));
        target.setOuterBorderColor(color(
            source == null ? null : source.getOuterBorderColor(),
            source == null ? defaults.getOuterBorderColor() : color(source.getBorderColor(), defaults.getOuterBorderColor())
        ));
        target.setOuterBorderOpacity(number(source == null ? null : source.getOuterBorderOpacity(), defaults.getOuterBorderOpacity(), 0, 100));
        target.setHeaderFontSize(number(source == null ? null : source.getHeaderFontSize(), defaults.getHeaderFontSize(), 12, 20));
        target.setBodyFontSize(number(source == null ? null : source.getBodyFontSize(), defaults.getBodyFontSize(), 12, 20));
        target.setButtonFontSize(number(source == null ? null : source.getButtonFontSize(), defaults.getButtonFontSize(), 12, 20));
        target.setRowHeight(number(source == null ? null : source.getRowHeight(), defaults.getRowHeight(), 32, 64));
        target.setBorderRadius(number(source == null ? null : source.getBorderRadius(), defaults.getBorderRadius(), 0, 24));
        target.setBorderMode(verticalBorderVisible ? "grid" : horizontalBorderVisible ? "horizontal" : "none");
        target.setWrapMode(allowedValue(source == null ? null : source.getWrapMode(), TABLE_WRAP_MODE_KEYS, defaults.getWrapMode()));
        target.setHeaderWrap(bool(source == null ? null : source.getHeaderWrap(), defaults.getHeaderWrap()));
        target.setStriped(bool(source == null ? null : source.getStriped(), defaults.getStriped()));
        target.setActionStyle(allowedValue(source == null ? null : source.getActionStyle(), TABLE_ACTION_STYLE_KEYS, defaults.getActionStyle()));
        target.setStatusAppearance(normalizeListStatusAppearance(
            source == null ? null : source.getStatusAppearance(),
            defaults.getStatusAppearance()
        ));
        target.setRowActionAppearance(normalizeListActionAppearance(
            source == null ? null : source.getRowActionAppearance(),
            defaults.getRowActionAppearance()
        ));
        return target;
    }

    private static ArtDetailDisplayConfigVo.ListStatusAppearanceConfig normalizeListStatusAppearance(
        ArtDetailDisplayConfigVo.ListStatusAppearanceConfig source,
        ArtDetailDisplayConfigVo.ListStatusAppearanceConfig defaults
    ) {
        ArtDetailDisplayConfigVo.ListStatusAppearanceConfig target = new ArtDetailDisplayConfigVo.ListStatusAppearanceConfig();
        target.setBorderVisible(bool(source == null ? null : source.getBorderVisible(), defaults.getBorderVisible()));
        target.setIconVisible(bool(source == null ? null : source.getIconVisible(), defaults.getIconVisible()));
        target.setUniformWidth(bool(source == null ? null : source.getUniformWidth(), defaults.getUniformWidth()));
        target.setColors(normalizeSemanticColors(source == null ? null : source.getColors(), defaults.getColors(), TABLE_STATUS_SEMANTIC_KEYS));
        return target;
    }

    private static ArtDetailDisplayConfigVo.ListActionAppearanceConfig normalizeListActionAppearance(
        ArtDetailDisplayConfigVo.ListActionAppearanceConfig source,
        ArtDetailDisplayConfigVo.ListActionAppearanceConfig defaults
    ) {
        ArtDetailDisplayConfigVo.ListActionAppearanceConfig target = new ArtDetailDisplayConfigVo.ListActionAppearanceConfig();
        target.setBorderVisible(bool(source == null ? null : source.getBorderVisible(), defaults.getBorderVisible()));
        target.setIconVisible(bool(source == null ? null : source.getIconVisible(), defaults.getIconVisible()));
        target.setUniformWidth(bool(source == null ? null : source.getUniformWidth(), defaults.getUniformWidth()));
        target.setDisabledOpacity(number(source == null ? null : source.getDisabledOpacity(), defaults.getDisabledOpacity(), 10, 100));
        target.setColors(normalizeSemanticColors(source == null ? null : source.getColors(), defaults.getColors(), TABLE_ACTION_SEMANTIC_KEYS));
        return target;
    }

    private static Map<String, String> normalizeSemanticColors(
        Map<String, String> source,
        Map<String, String> defaults,
        List<String> keys
    ) {
        Map<String, String> target = new LinkedHashMap<>();
        for (String key : keys) {
            target.put(key, color(source == null ? null : source.get(key), defaults.get(key)));
        }
        return target;
    }

    private static ArtDetailDisplayConfigVo.ListTableLayoutConfig defaultListTableLayout(
        ArtDetailDisplayConfigVo.ReviewWorkbenchConfig reviewWorkbench
    ) {
        ArtDetailDisplayConfigVo.ListTableLayoutConfig layout = new ArtDetailDisplayConfigVo.ListTableLayoutConfig();
        layout.setVersion(4);
        Map<String, ArtDetailDisplayConfigVo.ListTablePageConfig> pages = new LinkedHashMap<>();
        ArtDetailDisplayConfigVo.ListTablePageConfig project = defaultListTablePage(
            "暂无上报项目",
            List.of(
                tableColumn("projectNo", "项目编号", 150, 90, true),
                tableColumn("projectName", "项目名称", 220, 120, true),
                tableColumn("groupName", "组别", 120, 80, false),
                tableColumn("categoryName", "小类别", 150, 90, false),
                tableColumn("status", "状态", 110, 76, true),
                tableColumn("submittedAt", "提交时间", 170, 110, true),
                tableColumn("currentAuditOpinion", "意见", 180, 110, true),
                tableColumn("actions", "操作", 220, 220, true)
            )
        );
        project.getStatusLabels().put("pending", "已提交");
        project.getStatusLabels().put("approved", "审核通过");
        project.getStatusLabels().put("returned", "退回修改");
        pages.put("project", project);
        ArtDetailDisplayConfigVo.ListTablePageConfig schoolSubmit = defaultListTablePage(
            "暂无报送项目",
            List.of(
                tableColumn("selection", "选择", 58, 52, true),
                tableColumn("categoryName", "小类别", 150, 90, true),
                tableColumn("groupName", "组别", 120, 80, false),
                tableColumn("projectName", "项目名称", 360, 160, true),
                tableColumn("status", "状态", 130, 76, true),
                tableColumn("updateTime", "修改时间", 190, 110, true),
                tableColumn("submittedAt", "提交时间", 190, 110, true),
                tableColumn("currentAuditOpinion", "意见", 260, 120, false),
                tableColumn("actions", "操作", 220, 220, true)
            )
        );
        schoolSubmit.getStatusLabels().put("pending", "提交待审核");
        schoolSubmit.getStatusLabels().put("approved", "通过");
        schoolSubmit.getStatusLabels().put("returned", "退回");
        pages.put("schoolSubmit", schoolSubmit);
        pages.put("audit", defaultListTablePage(
            "暂无待审核项目",
            List.of(
                tableColumn("projectNo", "作品编号", 180, 90, true),
                tableColumn("projectName", "项目名称", 190, 120, true),
                tableColumn("categoryName", "小类别", 120, 80, true),
                tableColumn("groupName", "组别", 110, 80, false),
                tableColumn("schoolName", "单位", 140, 90, true),
                tableColumn("submittedAt", "提交时间", 146, 100, true),
                tableColumn("currentAuditOpinion", "审核意见", 160, 100, false),
                tableColumn("status", "状态", 84, 70, true),
                tableColumn("actions", "操作", 200, 200, true)
            )
        ));
        ArtDetailDisplayConfigVo.ListTablePageConfig review = defaultListTablePage(
            "暂无评分任务",
            reviewWorkbench == null ? defaultReviewListColumns() : reviewWorkbench.getColumns()
        );
        review.getStatusLabels().put("locked", "已被评分");
        review.getStatusLabels().put("scored", "已签字");
        review.getStatusLabels().put("scoreDraft", "已评分");
        review.getActionLabels().put("view", "查看评分");
        review.getActionLabels().put("edit", "修改评分");
        review.getActionLabels().put("score", "开始评分");
        review.setSelectionFixed("left");
        review.setCategoryColumns(
            reviewWorkbench == null || reviewWorkbench.getCategoryColumns() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(reviewWorkbench.getCategoryColumns())
        );
        pages.put("review", review);
        pages.put("projectView", defaultListTablePage(
            "暂无上报项目",
            List.of(
                tableColumn("projectNo", "项目编号", 150, 90, true),
                tableColumn("projectName", "项目名称", 190, 120, true),
                tableColumn("categoryName", "小类别", 140, 80, true),
                tableColumn("groupName", "组别", 110, 80, false),
                tableColumn("schoolName", "单位", 160, 100, true),
                tableColumn("submittedAt", "提交时间", 170, 110, true),
                tableColumn("status", "状态", 110, 76, true),
                tableColumn("actions", "操作", 130, 130, true)
            )
        ));
        pages.put("reviewAssignment", defaultListTablePage(
            "暂无评审分配记录",
            List.of(
                tableColumn("selection", "选择", 58, 52, true),
                tableColumn("activityName", "活动", 180, 120, true),
                tableColumn("categoryName", "类别", 150, 90, true),
                tableColumn("reviewerNames", "评审人员", 220, 120, true),
                tableColumn("schoolScopeMode", "学校范围", 160, 100, true),
                tableColumn("scoreMode", "评分模式", 110, 86, true),
                tableColumn("exclusiveMode", "互斥模式", 120, 90, true),
                tableColumn("scoreVisibilityPolicy", "评分可见", 120, 90, true),
                tableColumn("taskStats", "任务/历史评分", 180, 120, true),
                tableColumn("visibility", "可见性", 190, 120, true),
                tableColumn("status", "状态", 100, 76, true),
                tableColumn("actions", "操作", 180, 180, true)
            )
        ));
        pages.put("scoreSummary", defaultListTablePage(
            "暂无评分汇总记录",
            List.of(
                tableColumn("projectName", "节目名称", 240, 140, true),
                tableColumn("categoryName", "类别", 150, 90, true),
                tableColumn("programForm", "形式", 120, 80, true),
                tableColumn("groupOrNature", "甲乙组/个人", 140, 90, true),
                tableColumn("schoolName", "学校", 180, 100, true),
                tableColumn("currentAverageScore", "当前平均分", 120, 90, true),
                tableColumn("warningText", "评分提示", 120, 90, true),
                tableColumn("actions", "操作", 140, 140, true)
            )
        ));
        pages.put("signedSheets", defaultListTablePage(
            "当前筛选下还没有签名表",
            List.of(
                tableColumn("activityName", "活动", 160, 110, true),
                tableColumn("categoryName", "类别", 110, 80, true),
                tableColumn("reviewerName", "签字老师", 120, 90, true),
                tableColumn("total", "评分数", 88, 70, true),
                tableColumn("submissionMode", "提交方式", 100, 82, true),
                tableColumn("signedAt", "提交时间", 158, 110, true),
                tableColumn("status", "状态", 92, 76, true),
                tableColumn("withdrawalAudit", "撤回审计", 245, 140, true),
                tableColumn("actions", "操作", 252, 220, true)
            )
        ));
        layout.setPages(pages);
        return layout;
    }

    private static ArtDetailDisplayConfigVo.ListTablePageConfig defaultListTablePage(
        String emptyText,
        List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> columns
    ) {
        ArtDetailDisplayConfigVo.ListTablePageConfig page = new ArtDetailDisplayConfigVo.ListTablePageConfig();
        page.setEmptyText(emptyText);
        page.setSelectionFixed("none");
        page.setSerialVisible(true);
        page.setSerialWidth(66);
        page.setSerialFixed("none");
        page.setColumns(new ArrayList<>(columns));
        page.setCategoryColumns(new LinkedHashMap<>());
        page.setStatusLabels(orderedMap(
            "draft", "草稿",
            "pending", "待审核",
            "approved", "已通过",
            "returned", "已退回",
            "unscored", "未评分",
            "scored", "已评分",
            "scoreDraft", "评分草稿",
            "locked", "他人评分"
        ));
        page.setActionLabels(orderedMap(
            "view", "查看",
            "score", "评分",
            "edit", "编辑",
            "return", "退回",
            "withdraw", "撤回",
            "submit", "提交",
            "delete", "删除"
        ));
        return page;
    }

    private static ArtDetailDisplayConfigVo.ReviewListColumnConfig tableColumn(
        String key,
        String label,
        int width,
        int minWidth,
        boolean visible
    ) {
        return reviewColumn(key, label, width, minWidth, visible);
    }

    private static ArtDetailDisplayConfigVo.ListTableLayoutConfig normalizeListTableLayout(
        ArtDetailDisplayConfigVo.ListTableLayoutConfig source,
        ArtDetailDisplayConfigVo.ListTableLayoutConfig defaults
    ) {
        ArtDetailDisplayConfigVo.ListTableLayoutConfig target = new ArtDetailDisplayConfigVo.ListTableLayoutConfig();
        target.setVersion(4);
        int sourceVersion = number(source == null ? null : source.getVersion(), 1, 1, 100);
        Map<String, ArtDetailDisplayConfigVo.ListTablePageConfig> pages = new LinkedHashMap<>();
        Map<String, ArtDetailDisplayConfigVo.ListTablePageConfig> sourcePages = source == null ? null : source.getPages();
        for (String pageKey : LIST_TABLE_PAGE_KEYS) {
            ArtDetailDisplayConfigVo.ListTablePageConfig fallback = defaults.getPages().get(pageKey);
            ArtDetailDisplayConfigVo.ListTablePageConfig value = sourcePages == null ? null : sourcePages.get(pageKey);
            pages.put(pageKey, normalizeListTablePage(pageKey, value, fallback, sourceVersion));
        }
        target.setPages(pages);
        return target;
    }

    private static ArtDetailDisplayConfigVo.ListTablePageConfig normalizeListTablePage(
        String pageKey,
        ArtDetailDisplayConfigVo.ListTablePageConfig source,
        ArtDetailDisplayConfigVo.ListTablePageConfig defaults,
        int sourceVersion
    ) {
        ArtDetailDisplayConfigVo.ListTablePageConfig target = new ArtDetailDisplayConfigVo.ListTablePageConfig();
        target.setEmptyText(text(source == null ? null : source.getEmptyText(), defaults.getEmptyText(), 60));
        target.setSelectionFixed(allowedValue(
            source == null ? null : source.getSelectionFixed(),
            TABLE_STRUCTURAL_FIXED_KEYS,
            defaults.getSelectionFixed()
        ));
        target.setSerialVisible(bool(
            source == null ? null : source.getSerialVisible(),
            defaults.getSerialVisible()
        ));
        target.setSerialWidth(number(
            source == null ? null : source.getSerialWidth(),
            defaults.getSerialWidth(),
            48,
            240
        ));
        target.setSerialFixed(allowedValue(
            source == null ? null : source.getSerialFixed(),
            TABLE_STRUCTURAL_FIXED_KEYS,
            defaults.getSerialFixed()
        ));
        target.setColumns(normalizeListTableColumns(
            pageKey,
            source == null ? null : source.getColumns(),
            defaults.getColumns(),
            false
        ));
        migrateLegacyActionFixedRight(target.getColumns(), sourceVersion);
        Map<String, List<ArtDetailDisplayConfigVo.ReviewListColumnConfig>> categoryColumns = new LinkedHashMap<>();
        if (source != null && source.getCategoryColumns() != null) {
            source.getCategoryColumns().entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().matches("\\d+"))
                .limit(200)
                .forEach(entry -> {
                    List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> columns =
                        normalizeListTableColumns(pageKey, entry.getValue(), target.getColumns(), true);
                    migrateLegacyActionFixedRight(columns, sourceVersion);
                    categoryColumns.put(entry.getKey(), columns);
                });
        } else if (defaults.getCategoryColumns() != null) {
            defaults.getCategoryColumns().entrySet().stream()
                .limit(200)
                .forEach(entry -> categoryColumns.put(
                    entry.getKey(),
                    normalizeListTableColumns(pageKey, entry.getValue(), target.getColumns(), true)
                ));
        }
        target.setCategoryColumns(categoryColumns);
        target.setStatusLabels(normalizeTextMap(
            source == null ? null : source.getStatusLabels(),
            defaults.getStatusLabels(),
            TABLE_STATUS_SEMANTIC_KEYS,
            30
        ));
        target.setActionLabels(normalizeTextMap(
            source == null ? null : source.getActionLabels(),
            defaults.getActionLabels(),
            TABLE_ACTION_SEMANTIC_KEYS,
            30
        ));
        return target;
    }

    private static void migrateLegacyActionFixedRight(
        List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> columns,
        int sourceVersion
    ) {
        if (sourceVersion >= 4 || columns == null) {
            return;
        }
        columns.stream()
            .filter(column -> "actions".equals(column.getKey()))
            .forEach(column -> column.setFixed("right"));
    }

    private static List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> normalizeListTableColumns(
        String pageKey,
        List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> source,
        List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> defaults,
        boolean allowForm
    ) {
        Map<String, ArtDetailDisplayConfigVo.ReviewListColumnConfig> defaultMap = defaults.stream()
            .collect(java.util.stream.Collectors.toMap(
                ArtDetailDisplayConfigVo.ReviewListColumnConfig::getKey,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
            ));
        Set<String> allowed = pageBuiltinKeys(pageKey);
        List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> input = source == null ? defaults : source;
        for (ArtDetailDisplayConfigVo.ReviewListColumnConfig item : input) {
            if (item == null) continue;
            boolean form = allowForm && "form".equals(item.getSource());
            String fieldKey = item.getFieldKey() == null ? "" : item.getFieldKey().trim();
            String key = form ? "field:" + fieldKey : item.getKey();
            if (!form && "review".equals(pageKey) && "status".equals(key)) {
                key = "scoreStatus";
            }
            if (key == null || !seen.add(key)
                || (!form && !allowed.contains(key))
                || (form && !fieldKey.matches("[A-Za-z0-9_.-]{1,80}"))) {
                continue;
            }
            ArtDetailDisplayConfigVo.ReviewListColumnConfig fallback = defaultMap.get(key);
            ArtDetailDisplayConfigVo.ReviewListColumnConfig column = new ArtDetailDisplayConfigVo.ReviewListColumnConfig();
            column.setKey(key);
            column.setSource(form ? "form" : "builtin");
            column.setFieldKey(form ? fieldKey : null);
            column.setLabel(text(item.getLabel(), fallback == null ? fieldKey : fallback.getLabel(), 30));
            boolean visibilityLocked = "review".equals(pageKey) && "groupOrNature".equals(key);
            boolean deletionLocked = "actions".equals(key) || visibilityLocked;
            boolean deleted = !deletionLocked && bool(item.getDeleted(), false);
            column.setVisible(visibilityLocked || (!deleted && bool(item.getVisible(), fallback == null || Boolean.TRUE.equals(fallback.getVisible()))));
            column.setDeleted(deleted);
            int minimum = "actions".equals(key) ? Math.max(96, fallback == null ? 96 : fallback.getMinWidth()) : 48;
            column.setWidth(number(item.getWidth(), fallback == null ? 140 : fallback.getWidth(), minimum, 800));
            column.setMinWidth(number(item.getMinWidth(), fallback == null ? 90 : fallback.getMinWidth(), minimum, 400));
            column.setFixed(normalizeColumnFixed(item.getFixed(), key, fallback == null ? "none" : fallback.getFixed()));
            result.add(column);
        }
        for (ArtDetailDisplayConfigVo.ReviewListColumnConfig item : defaults) {
            if (!seen.contains(item.getKey())) result.add(item);
        }
        result.sort((left, right) -> {
            int zone = Integer.compare(columnFixedWeight(left.getFixed()), columnFixedWeight(right.getFixed()));
            if (zone != 0) return zone;
            if ("actions".equals(left.getKey())) return 1;
            if ("actions".equals(right.getKey())) return -1;
            return 0;
        });
        return result;
    }

    private static String normalizeColumnFixed(String value, String key, String fallback) {
        String normalized = allowedValue(value, TABLE_COLUMN_FIXED_KEYS, allowedValue(fallback, TABLE_COLUMN_FIXED_KEYS, "none"));
        if ("selection".equals(key)) {
            return "left".equals(normalized) ? "left" : "none";
        }
        if ("actions".equals(key)) {
            return "right".equals(normalized) ? "right" : "none";
        }
        return normalized;
    }

    private static int columnFixedWeight(String fixed) {
        if ("left".equals(fixed)) return 0;
        if ("right".equals(fixed)) return 2;
        return 1;
    }

    private static Set<String> pageBuiltinKeys(String pageKey) {
        return switch (pageKey) {
            case "project" -> Set.of(
                "projectNo", "projectName", "groupName", "categoryName", "status",
                "submittedAt", "currentAuditOpinion", "actions"
            );
            case "schoolSubmit" -> Set.of(
                "selection", "serial", "categoryName", "groupName", "projectName", "status",
                "updateTime", "submittedAt", "currentAuditOpinion", "actions"
            );
            case "audit" -> Set.of(
                "projectNo", "projectName", "categoryName", "groupName", "schoolName",
                "submittedAt", "currentAuditOpinion", "status", "actions"
            );
            case "review" -> new LinkedHashSet<>(REVIEW_LIST_BUILTIN_KEYS);
            case "projectView" -> Set.of(
                "projectNo", "projectName", "categoryName", "groupName", "schoolName",
                "submittedAt", "status", "actions"
            );
            case "reviewAssignment" -> new LinkedHashSet<>(List.of(
                "selection", "activityName", "categoryName", "reviewerNames", "schoolScopeMode", "scoreMode",
                "exclusiveMode", "scoreVisibilityPolicy", "taskStats", "visibility", "status", "actions"
            ));
            case "scoreSummary" -> Set.of(
                "projectName", "categoryName", "programForm", "groupOrNature", "schoolName",
                "currentAverageScore", "warningText", "actions"
            );
            case "signedSheets" -> Set.of(
                "activityName", "categoryName", "reviewerName", "total", "submissionMode",
                "signedAt", "status", "withdrawalAudit", "actions"
            );
            default -> Set.of();
        };
    }

    private static Map<String, String> normalizeTextMap(
        Map<String, String> source,
        Map<String, String> defaults,
        List<String> keys,
        int maxLength
    ) {
        Map<String, String> target = new LinkedHashMap<>();
        for (String key : keys) {
            target.put(key, text(source == null ? null : source.get(key), defaults.get(key), maxLength));
        }
        return target;
    }

    private static ArtDetailDisplayConfigVo.ReviewWorkbenchConfig defaultReviewWorkbench() {
        ArtDetailDisplayConfigVo.ReviewWorkbenchConfig workbench = new ArtDetailDisplayConfigVo.ReviewWorkbenchConfig();
        ArtDetailDisplayConfigVo.ReviewActivityTitleConfig title = new ArtDetailDisplayConfigVo.ReviewActivityTitleConfig();
        title.setVisible(true);
        title.setTemplate("{activityName}");
        title.setFontSize(15);
        title.setColor("#29445f");
        title.setFontWeight("bold");
        title.setAlign("left");
        title.setSwitchText("切换活动");
        title.setAllowWrap(true);
        workbench.setActivityTitle(title);

        ArtDetailDisplayConfigVo.ReviewScopeNavigatorConfig scopeNavigator =
            new ArtDetailDisplayConfigVo.ReviewScopeNavigatorConfig();
        scopeNavigator.setVisible(true);
        scopeNavigator.setDisplayMode("buttons");
        scopeNavigator.setShowCount(true);
        scopeNavigator.setProgressVisible(false);
        scopeNavigator.setProgressPlacement("inside");
        workbench.setScopeNavigator(scopeNavigator);

        ArtDetailDisplayConfigVo.ReviewProgressConfig progress = new ArtDetailDisplayConfigVo.ReviewProgressConfig();
        progress.setVisible(true);
        progress.setTemplate("本类别 {completed}/{total} 个作品已评分");
        progress.setShowPercentage(false);
        progress.setShowRemaining(true);
        progress.setHeight(8);
        progress.setWidth(260);
        progress.setActiveColor("#2563eb");
        progress.setSuccessColor("#67c23a");
        progress.setSignedColor("#16a34a");
        progress.setTrackColor("#e5e7eb");
        progress.setTextColor("#64748b");
        progress.setFontSize(13);
        workbench.setProgress(progress);

        ArtDetailDisplayConfigVo.ReviewSignatureConfig signature = new ArtDetailDisplayConfigVo.ReviewSignatureConfig();
        signature.setHintVisible(true);
        signature.setHintText("请选择具体类别后提交签字");
        signature.setHintFontSize(13);
        signature.setHintFontWeight("medium");
        signature.setHintColor("#8a5b16");
        signature.setHintBackgroundColor("#fff8e8");
        signature.setHintBorderColor("#f3d19e");
        signature.setButtonText("统一提交签字");
        signature.setButtonFontSize(14);
        signature.setButtonFontWeight("medium");
        signature.setButtonTextColor("#ffffff");
        workbench.setSignature(signature);
        workbench.setColumns(defaultReviewListColumns());
        workbench.setCategoryColumns(new LinkedHashMap<>());
        return workbench;
    }

    private static List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> defaultReviewListColumns() {
        List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> columns = new ArrayList<>();
        columns.add(reviewColumn("projectNo", "项目编号", 135, 90, true));
        columns.add(reviewColumn("projectName", "项目名称", 180, 120, true));
        columns.add(reviewColumn("activityName", "活动", 180, 120, true));
        columns.add(reviewColumn("categoryName", "类别", 110, 80, true));
        columns.add(reviewColumn("groupOrNature", "组别", 100, 70, false));
        columns.add(reviewColumn("programForm", "类别细分", 110, 80, false));
        columns.add(reviewColumn("scoreMode", "评分模式", 90, 70, true));
        columns.add(reviewColumn("scoreResult", "得分/等级", 96, 80, true));
        columns.add(reviewColumn("scoreSubmittedAt", "签字时间", 146, 100, true));
        columns.add(reviewColumn("scoreStatus", "状态", 84, 70, true));
        columns.add(reviewColumn("actions", "操作", 110, 110, true));
        return columns;
    }

    private static ArtDetailDisplayConfigVo.ReviewListColumnConfig reviewColumn(
        String key, String label, int width, int minWidth, boolean visible
    ) {
        ArtDetailDisplayConfigVo.ReviewListColumnConfig column = new ArtDetailDisplayConfigVo.ReviewListColumnConfig();
        column.setKey(key);
        column.setSource("builtin");
        column.setLabel(label);
        column.setVisible(visible);
        column.setDeleted(false);
        column.setWidth(width);
        column.setMinWidth(minWidth);
        column.setFixed("actions".equals(key) ? "right" : "none");
        return column;
    }

    private static ArtDetailDisplayConfigVo.ReviewWorkbenchConfig normalizeReviewWorkbench(
        ArtDetailDisplayConfigVo.ReviewWorkbenchConfig source,
        ArtDetailDisplayConfigVo.ReviewWorkbenchConfig defaults
    ) {
        ArtDetailDisplayConfigVo.ReviewWorkbenchConfig target = new ArtDetailDisplayConfigVo.ReviewWorkbenchConfig();
        ArtDetailDisplayConfigVo.ReviewActivityTitleConfig sourceTitle = source == null ? null : source.getActivityTitle();
        ArtDetailDisplayConfigVo.ReviewActivityTitleConfig defaultTitle = defaults.getActivityTitle();
        ArtDetailDisplayConfigVo.ReviewActivityTitleConfig title = new ArtDetailDisplayConfigVo.ReviewActivityTitleConfig();
        title.setVisible(bool(sourceTitle == null ? null : sourceTitle.getVisible(), defaultTitle.getVisible()));
        title.setTemplate(template(sourceTitle == null ? null : sourceTitle.getTemplate(), defaultTitle.getTemplate(), 120));
        title.setFontSize(number(sourceTitle == null ? null : sourceTitle.getFontSize(), defaultTitle.getFontSize(), 12, 28));
        title.setColor(color(sourceTitle == null ? null : sourceTitle.getColor(), defaultTitle.getColor()));
        title.setFontWeight(allowedValue(sourceTitle == null ? null : sourceTitle.getFontWeight(), REVIEW_TITLE_WEIGHT_KEYS, defaultTitle.getFontWeight()));
        title.setAlign(allowedValue(sourceTitle == null ? null : sourceTitle.getAlign(), REVIEW_TITLE_ALIGN_KEYS, defaultTitle.getAlign()));
        title.setSwitchText(text(sourceTitle == null ? null : sourceTitle.getSwitchText(), defaultTitle.getSwitchText(), 20));
        title.setAllowWrap(bool(sourceTitle == null ? null : sourceTitle.getAllowWrap(), defaultTitle.getAllowWrap()));
        target.setActivityTitle(title);

        ArtDetailDisplayConfigVo.ReviewScopeNavigatorConfig sourceScope =
            source == null ? null : source.getScopeNavigator();
        ArtDetailDisplayConfigVo.ReviewScopeNavigatorConfig defaultScope = defaults.getScopeNavigator();
        ArtDetailDisplayConfigVo.ReviewScopeNavigatorConfig scopeNavigator =
            new ArtDetailDisplayConfigVo.ReviewScopeNavigatorConfig();
        scopeNavigator.setVisible(bool(sourceScope == null ? null : sourceScope.getVisible(), defaultScope.getVisible()));
        scopeNavigator.setDisplayMode(allowedValue(
            sourceScope == null ? null : sourceScope.getDisplayMode(),
            REVIEW_SCOPE_DISPLAY_MODE_KEYS,
            defaultScope.getDisplayMode()
        ));
        scopeNavigator.setShowCount(bool(sourceScope == null ? null : sourceScope.getShowCount(), defaultScope.getShowCount()));
        scopeNavigator.setProgressVisible(bool(
            sourceScope == null ? null : sourceScope.getProgressVisible(),
            defaultScope.getProgressVisible()
        ));
        scopeNavigator.setProgressPlacement(allowedValue(
            sourceScope == null ? null : sourceScope.getProgressPlacement(),
            REVIEW_SCOPE_PROGRESS_PLACEMENT_KEYS,
            defaultScope.getProgressPlacement()
        ));
        target.setScopeNavigator(scopeNavigator);

        ArtDetailDisplayConfigVo.ReviewProgressConfig sourceProgress = source == null ? null : source.getProgress();
        ArtDetailDisplayConfigVo.ReviewProgressConfig defaultProgress = defaults.getProgress();
        ArtDetailDisplayConfigVo.ReviewProgressConfig progress = new ArtDetailDisplayConfigVo.ReviewProgressConfig();
        progress.setVisible(bool(sourceProgress == null ? null : sourceProgress.getVisible(), defaultProgress.getVisible()));
        progress.setTemplate(template(sourceProgress == null ? null : sourceProgress.getTemplate(), defaultProgress.getTemplate(), 120));
        progress.setShowPercentage(bool(sourceProgress == null ? null : sourceProgress.getShowPercentage(), defaultProgress.getShowPercentage()));
        progress.setShowRemaining(bool(sourceProgress == null ? null : sourceProgress.getShowRemaining(), defaultProgress.getShowRemaining()));
        progress.setHeight(number(sourceProgress == null ? null : sourceProgress.getHeight(), defaultProgress.getHeight(), 4, 20));
        progress.setWidth(number(sourceProgress == null ? null : sourceProgress.getWidth(), defaultProgress.getWidth(), 120, 600));
        progress.setActiveColor(color(sourceProgress == null ? null : sourceProgress.getActiveColor(), defaultProgress.getActiveColor()));
        progress.setSuccessColor(color(sourceProgress == null ? null : sourceProgress.getSuccessColor(), defaultProgress.getSuccessColor()));
        progress.setSignedColor(color(sourceProgress == null ? null : sourceProgress.getSignedColor(), defaultProgress.getSignedColor()));
        progress.setTrackColor(color(sourceProgress == null ? null : sourceProgress.getTrackColor(), defaultProgress.getTrackColor()));
        progress.setTextColor(color(sourceProgress == null ? null : sourceProgress.getTextColor(), defaultProgress.getTextColor()));
        progress.setFontSize(number(sourceProgress == null ? null : sourceProgress.getFontSize(), defaultProgress.getFontSize(), 12, 20));
        target.setProgress(progress);

        ArtDetailDisplayConfigVo.ReviewSignatureConfig sourceSignature = source == null ? null : source.getSignature();
        ArtDetailDisplayConfigVo.ReviewSignatureConfig defaultSignature = defaults.getSignature();
        ArtDetailDisplayConfigVo.ReviewSignatureConfig signature = new ArtDetailDisplayConfigVo.ReviewSignatureConfig();
        signature.setHintVisible(bool(sourceSignature == null ? null : sourceSignature.getHintVisible(), defaultSignature.getHintVisible()));
        signature.setHintText(text(sourceSignature == null ? null : sourceSignature.getHintText(), defaultSignature.getHintText(), 60));
        signature.setHintFontSize(number(sourceSignature == null ? null : sourceSignature.getHintFontSize(), defaultSignature.getHintFontSize(), 12, 20));
        signature.setHintFontWeight(allowedValue(sourceSignature == null ? null : sourceSignature.getHintFontWeight(), REVIEW_TITLE_WEIGHT_KEYS, defaultSignature.getHintFontWeight()));
        signature.setHintColor(color(sourceSignature == null ? null : sourceSignature.getHintColor(), defaultSignature.getHintColor()));
        signature.setHintBackgroundColor(color(sourceSignature == null ? null : sourceSignature.getHintBackgroundColor(), defaultSignature.getHintBackgroundColor()));
        signature.setHintBorderColor(color(sourceSignature == null ? null : sourceSignature.getHintBorderColor(), defaultSignature.getHintBorderColor()));
        signature.setButtonText(text(sourceSignature == null ? null : sourceSignature.getButtonText(), defaultSignature.getButtonText(), 30));
        signature.setButtonFontSize(number(sourceSignature == null ? null : sourceSignature.getButtonFontSize(), defaultSignature.getButtonFontSize(), 12, 20));
        signature.setButtonFontWeight(allowedValue(sourceSignature == null ? null : sourceSignature.getButtonFontWeight(), REVIEW_TITLE_WEIGHT_KEYS, defaultSignature.getButtonFontWeight()));
        signature.setButtonTextColor(color(sourceSignature == null ? null : sourceSignature.getButtonTextColor(), defaultSignature.getButtonTextColor()));
        target.setSignature(signature);
        target.setColumns(normalizeReviewListColumns(source == null ? null : source.getColumns(), defaults.getColumns()));

        Map<String, List<ArtDetailDisplayConfigVo.ReviewListColumnConfig>> categoryColumns = new LinkedHashMap<>();
        if (source != null && source.getCategoryColumns() != null) {
            source.getCategoryColumns().entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().matches("\\d+"))
                .limit(200)
                .forEach(entry -> categoryColumns.put(entry.getKey(), normalizeReviewListColumns(entry.getValue(), defaults.getColumns())));
        }
        target.setCategoryColumns(categoryColumns);
        return target;
    }

    private static List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> normalizeReviewListColumns(
        List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> source,
        List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> defaults
    ) {
        Map<String, ArtDetailDisplayConfigVo.ReviewListColumnConfig> defaultMap = defaults.stream()
            .collect(java.util.stream.Collectors.toMap(
                ArtDetailDisplayConfigVo.ReviewListColumnConfig::getKey,
                item -> item,
                (left, right) -> left,
                LinkedHashMap::new
            ));
        List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        List<ArtDetailDisplayConfigVo.ReviewListColumnConfig> input = source == null ? defaults : source;
        for (ArtDetailDisplayConfigVo.ReviewListColumnConfig item : input) {
            if (item == null) {
                continue;
            }
            boolean form = "form".equals(item.getSource());
            String fieldKey = item.getFieldKey() == null ? "" : item.getFieldKey().trim();
            String key = form ? "field:" + fieldKey : item.getKey();
            if (!form && "status".equals(key)) {
                key = "scoreStatus";
            }
            ArtDetailDisplayConfigVo.ReviewListColumnConfig fallback = defaultMap.get(key);
            if (key == null || !seen.add(key)
                || (!form && !REVIEW_LIST_BUILTIN_KEYS.contains(key))
                || (form && !fieldKey.matches("[A-Za-z0-9_.-]{1,80}"))) {
                continue;
            }
            ArtDetailDisplayConfigVo.ReviewListColumnConfig target = new ArtDetailDisplayConfigVo.ReviewListColumnConfig();
            target.setKey(key);
            target.setSource(form ? "form" : "builtin");
            target.setFieldKey(form ? fieldKey : null);
            target.setLabel(text(item.getLabel(), fallback == null ? fieldKey : fallback.getLabel(), 30));
            boolean visibilityLocked = "groupOrNature".equals(key);
            boolean deletionLocked = "actions".equals(key) || visibilityLocked;
            boolean deleted = !deletionLocked && bool(item.getDeleted(), false);
            target.setVisible(visibilityLocked
                || (!deleted && bool(item.getVisible(), fallback == null || Boolean.TRUE.equals(fallback.getVisible()))));
            target.setDeleted(deleted);
            int minWidth = "actions".equals(key) ? 96 : 48;
            target.setWidth(number(item.getWidth(), fallback == null ? 140 : fallback.getWidth(), minWidth, 600));
            target.setMinWidth(number(item.getMinWidth(), fallback == null ? 90 : fallback.getMinWidth(), minWidth, 300));
            target.setFixed(normalizeColumnFixed(item.getFixed(), key, fallback == null ? "none" : fallback.getFixed()));
            result.add(target);
        }
        for (ArtDetailDisplayConfigVo.ReviewListColumnConfig item : defaults) {
            if (!seen.contains(item.getKey())) {
                result.add(item);
            }
        }
        result.sort((left, right) -> {
            int zone = Integer.compare(columnFixedWeight(left.getFixed()), columnFixedWeight(right.getFixed()));
            if (zone != 0) return zone;
            if ("actions".equals(left.getKey())) return 1;
            if ("actions".equals(right.getKey())) return -1;
            return 0;
        });
        return result;
    }

    private static String template(String value, String fallback, int maxLength) {
        return text(value == null ? null : value.replace("<", "").replace(">", ""), fallback, maxLength);
    }

    private static List<String> normalizeFields(List<String> source, List<String> allowed, List<String> defaults) {
        if (source == null) {
            return new ArrayList<>(defaults);
        }
        Set<String> allowedSet = new LinkedHashSet<>(allowed);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        source.forEach(key -> {
            if (allowedSet.contains(key)) {
                result.add(key);
            }
        });
        return new ArrayList<>(result);
    }

    private static List<String> normalizeFileFields(List<String> source, List<String> defaults) {
        if (source == null) {
            return new ArrayList<>(defaults);
        }
        List<String> migrated = source.stream()
            .map(key -> "checkMessage".equals(key) ? "technicalRequirements" : key)
            .toList();
        return normalizeFields(migrated, FILE_FIELD_KEYS, defaults);
    }

    private static String allowedValue(String source, List<String> allowed, String fallback) {
        return source != null && allowed.contains(source) ? source : fallback;
    }

    private static ArtDetailDisplayConfigVo.PopupTabConfig normalizePopupTabs(
        ArtDetailDisplayConfigVo.PopupTabConfig source,
        List<String> allowed,
        Map<String, String> defaultLabels,
        String legacyDefault,
        String basicInfoLabel,
        String projectFilesLabel
    ) {
        List<String> order = normalizeFields(source == null ? null : source.getOrder(), allowed, allowed);
        allowed.forEach(key -> {
            if (!order.contains(key)) {
                order.add(key);
            }
        });
        List<String> visibleTabs = normalizeFields(source == null ? null : source.getVisibleTabs(), allowed, allowed);
        if (visibleTabs.isEmpty()) {
            visibleTabs.addAll(allowed);
        }
        if (!visibleTabs.contains("preview") && !visibleTabs.contains("files")) {
            visibleTabs.add("preview");
        }
        List<String> orderedVisibleTabs = new ArrayList<>();
        for (String key : order) {
            if (visibleTabs.contains(key)) {
                orderedVisibleTabs.add(key);
            }
        }
        Map<String, String> migratedLabels = new LinkedHashMap<>(defaultLabels);
        migratedLabels.put("form", basicInfoLabel);
        migratedLabels.put("files", projectFilesLabel);
        Map<String, String> labels = normalizeLabels(source == null ? null : source.getLabels(), migratedLabels);
        String requestedDefault = allowedValue(source == null ? null : source.getDefaultTab(), allowed, legacyDefault);
        String defaultTab = orderedVisibleTabs.contains(requestedDefault) ? requestedDefault : orderedVisibleTabs.get(0);

        ArtDetailDisplayConfigVo.PopupTabConfig target = new ArtDetailDisplayConfigVo.PopupTabConfig();
        target.setOrder(new ArrayList<>(order));
        target.setVisibleTabs(orderedVisibleTabs);
        target.setLabels(labels);
        target.setDefaultTab(defaultTab);
        return target;
    }

    private static ArtDetailDisplayConfigVo.PopupTabConfig defaultPopupTabs(List<String> keys, Map<String, String> labels) {
        ArtDetailDisplayConfigVo.PopupTabConfig tabs = new ArtDetailDisplayConfigVo.PopupTabConfig();
        tabs.setOrder(new ArrayList<>(keys));
        tabs.setVisibleTabs(new ArrayList<>(keys));
        tabs.setLabels(new LinkedHashMap<>(labels));
        tabs.setDefaultTab(keys.get(0));
        return tabs;
    }

    private static Map<String, String> normalizeLabels(Map<String, String> source, Map<String, String> defaults) {
        Map<String, String> result = new LinkedHashMap<>();
        defaults.forEach((key, defaultLabel) -> result.put(key, text(source == null ? null : source.get(key), defaultLabel, 20)));
        return result;
    }

    private static ArtDetailDisplayConfigVo.NavigatorPageConfig normalizeNavigatorPage(
        String pageKey,
        ArtDetailDisplayConfigVo.NavigatorPageConfig source,
        ArtDetailDisplayConfigVo.NavigatorPageConfig defaults
    ) {
        ArtDetailDisplayConfigVo.NavigatorPageConfig target = new ArtDetailDisplayConfigVo.NavigatorPageConfig();
        target.setLayout(allowedValue(source == null ? null : source.getLayout(), NAVIGATOR_LAYOUT_KEYS, defaults.getLayout()));
        List<String> fields = normalizeFields(source == null ? null : source.getFields(), NAVIGATOR_FIELD_KEYS, defaults.getFields());
        if ("review".equals(pageKey)) {
            fields.remove("school");
        } else if ("audit".equals(pageKey)) {
            fields.remove("activity");
            fields.remove("programForm");
        }
        target.setFields(fields);
        target.setLabels(normalizeLabels(source == null ? null : source.getLabels(), defaults.getLabels()));
        target.setActivityTitle("audit".equals(pageKey)
            ? normalizeNavigatorActivityTitle(source == null ? null : source.getActivityTitle(), defaults.getActivityTitle())
            : null);
        return target;
    }

    private static ArtDetailDisplayConfigVo.NavigatorActivityTitleConfig normalizeNavigatorActivityTitle(
        ArtDetailDisplayConfigVo.NavigatorActivityTitleConfig source,
        ArtDetailDisplayConfigVo.NavigatorActivityTitleConfig defaults
    ) {
        ArtDetailDisplayConfigVo.NavigatorActivityTitleConfig target = new ArtDetailDisplayConfigVo.NavigatorActivityTitleConfig();
        target.setDisplayMode(allowedValue(source == null ? null : source.getDisplayMode(), NAVIGATOR_ACTIVITY_DISPLAY_KEYS, defaults.getDisplayMode()));
        target.setTemplate(template(source == null ? null : source.getTemplate(), defaults.getTemplate(), 120));
        target.setFontSize(number(source == null ? null : source.getFontSize(), defaults.getFontSize(), 12, 30));
        target.setColor(color(source == null ? null : source.getColor(), defaults.getColor()));
        target.setFontWeight(allowedValue(source == null ? null : source.getFontWeight(), REVIEW_TITLE_WEIGHT_KEYS, defaults.getFontWeight()));
        target.setAlign(allowedValue(source == null ? null : source.getAlign(), REVIEW_TITLE_ALIGN_KEYS, defaults.getAlign()));
        target.setSwitchText(text(source == null ? null : source.getSwitchText(), defaults.getSwitchText(), 20));
        target.setAllowWrap(bool(source == null ? null : source.getAllowWrap(), defaults.getAllowWrap()));
        return target;
    }

    private static ArtDetailDisplayConfigVo.AuditProgressConfig normalizeAuditProgress(
        ArtDetailDisplayConfigVo.AuditProgressConfig source,
        ArtDetailDisplayConfigVo.AuditProgressConfig defaults
    ) {
        ArtDetailDisplayConfigVo.AuditProgressConfig target = new ArtDetailDisplayConfigVo.AuditProgressConfig();
        target.setVisible(bool(source == null ? null : source.getVisible(), defaults.getVisible()));
        target.setTitle(text(source == null ? null : source.getTitle(), defaults.getTitle(), 20));
        target.setTemplate(template(source == null ? null : source.getTemplate(), defaults.getTemplate(), 80));
        target.setHeight(number(source == null ? null : source.getHeight(), defaults.getHeight(), 4, 20));
        target.setWidth(number(source == null ? null : source.getWidth(), defaults.getWidth(), 120, 480));
        target.setActiveColor(color(source == null ? null : source.getActiveColor(), defaults.getActiveColor()));
        target.setCompleteColor(color(source == null ? null : source.getCompleteColor(), defaults.getCompleteColor()));
        target.setTrackColor(color(source == null ? null : source.getTrackColor(), defaults.getTrackColor()));
        target.setTextColor(color(source == null ? null : source.getTextColor(), defaults.getTextColor()));
        target.setFontSize(number(source == null ? null : source.getFontSize(), defaults.getFontSize(), 12, 20));
        return target;
    }

    private static ArtDetailDisplayConfigVo.AuditProgressConfig defaultAuditProgress() {
        ArtDetailDisplayConfigVo.AuditProgressConfig progress = new ArtDetailDisplayConfigVo.AuditProgressConfig();
        progress.setVisible(true);
        progress.setTitle("审核进度");
        progress.setTemplate("已审核 {completed}/{total}");
        progress.setHeight(8);
        progress.setWidth(240);
        progress.setActiveColor("#2563eb");
        progress.setCompleteColor("#16a34a");
        progress.setTrackColor("#e5e7eb");
        progress.setTextColor("#64748b");
        progress.setFontSize(13);
        return progress;
    }

    private static ArtDetailDisplayConfigVo.NavigatorPageConfig defaultNavigatorPage(String pageKey) {
        ArtDetailDisplayConfigVo.NavigatorPageConfig page = new ArtDetailDisplayConfigVo.NavigatorPageConfig();
        page.setLayout("sidebar");
        List<String> fields = new ArrayList<>(NAVIGATOR_FIELD_KEYS);
        if ("review".equals(pageKey)) {
            fields.remove("school");
        } else if ("audit".equals(pageKey)) {
            fields.remove("activity");
            fields.remove("programForm");
        }
        page.setFields(fields);
        page.setLabels(new LinkedHashMap<>(NAVIGATOR_DEFAULT_LABELS));
        if ("audit".equals(pageKey)) {
            page.getLabels().put("category", "类别");
            page.getLabels().put("groupOrNature", "组别");
            page.getLabels().put("programForm", "子类");
            ArtDetailDisplayConfigVo.NavigatorActivityTitleConfig title = new ArtDetailDisplayConfigVo.NavigatorActivityTitleConfig();
            title.setDisplayMode("title");
            title.setTemplate("{activityName}");
            title.setFontSize(15);
            title.setColor("#29445f");
            title.setFontWeight("bold");
            title.setAlign("left");
            title.setSwitchText("切换活动");
            title.setAllowWrap(true);
            page.setActivityTitle(title);
        }
        return page;
    }

    private static ArtDetailDisplayConfigVo.NavigatorConfig navigatorStyle(ArtDetailDisplayConfigVo.NavigatorConfig source) {
        ArtDetailDisplayConfigVo.NavigatorConfig style = new ArtDetailDisplayConfigVo.NavigatorConfig();
        style.setTitleFontSize(source.getTitleFontSize());
        style.setItemFontSize(source.getItemFontSize());
        style.setCountFontSize(source.getCountFontSize());
        style.setRowHeight(source.getRowHeight());
        style.setItemGap(source.getItemGap());
        style.setPanelPadding(source.getPanelPadding());
        style.setBackgroundColor(source.getBackgroundColor());
        style.setTextColor(source.getTextColor());
        style.setActiveColor(source.getActiveColor());
        style.setHoverBackground(source.getHoverBackground());
        style.setHoverBackgroundOpacity(source.getHoverBackgroundOpacity());
        style.setSelectedBackground(source.getSelectedBackground());
        style.setSelectedBackgroundOpacity(source.getSelectedBackgroundOpacity());
        style.setSelectedTextColor(source.getSelectedTextColor());
        style.setBorderColor(source.getBorderColor());
        style.setBackgroundOpacity(source.getBackgroundOpacity());
        style.setDividerVisible(source.getDividerVisible());
        style.setDividerColor(source.getDividerColor());
        style.setDividerOpacity(source.getDividerOpacity());
        style.setDividerWidth(source.getDividerWidth());
        style.setBorderRadius(source.getBorderRadius());
        style.setSidebarWidth(source.getSidebarWidth());
        style.setSidebarMaxHeight(source.getSidebarMaxHeight());
        style.setTopHeight(source.getTopHeight());
        style.setBorderVisible(source.getBorderVisible());
        style.setShadowVisible(source.getShadowVisible());
        return style;
    }

    private static ArtDetailDisplayConfigVo.TabConfig legacyTabs(ArtDetailDisplayConfigVo.TabConfig source) {
        ArtDetailDisplayConfigVo.TabConfig legacy = new ArtDetailDisplayConfigVo.TabConfig();
        legacy.setBasicInfoLabel(source.getAudit().getLabels().get("form"));
        legacy.setProjectFilesLabel(source.getAudit().getLabels().get("files"));
        legacy.setAuditDefaultTab(source.getAudit().getDefaultTab());
        legacy.setScoreDefaultTab(source.getScore().getDefaultTab());
        return legacy;
    }

    private static ArtDetailDisplayConfigVo.AuditConfig auditStyle(ArtDetailDisplayConfigVo.AuditConfig source) {
        ArtDetailDisplayConfigVo.AuditConfig style = new ArtDetailDisplayConfigVo.AuditConfig();
        style.setTitle(source.getTitle());
        style.setSummaryFields(source.getSummaryFields());
        style.setSummaryLabels(source.getSummaryLabels());
        style.setActionsVisible(source.getActionsVisible());
        return style;
    }

    private static ArtDetailDisplayConfigVo.ScoreConfig scoreStyle(ArtDetailDisplayConfigVo.ScoreConfig source) {
        ArtDetailDisplayConfigVo.ScoreConfig style = new ArtDetailDisplayConfigVo.ScoreConfig();
        style.setTitle(source.getTitle());
        style.setScoreLabel(source.getScoreLabel());
        style.setGradeLabel(source.getGradeLabel());
        style.setCommentLabel(source.getCommentLabel());
        style.setStatusVisible(source.getStatusVisible());
        style.setCommentVisible(source.getCommentVisible());
        style.setSaveDraftVisible(source.getSaveDraftVisible());
        style.setFileListVisible(source.getFileListVisible());
        return style;
    }

    private static String text(String value, String fallback, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            normalized = fallback;
        }
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private static String optionalText(String value, String fallback, int maxLength) {
        String normalized = value == null ? fallback : value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private static Boolean bool(Boolean value, Boolean fallback) {
        return value == null ? fallback : value;
    }

    private static String color(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.matches("#[0-9a-fA-F]{6}") ? normalized : fallback;
    }

    private static Integer number(Integer value, Integer fallback, int min, int max) {
        int resolved = value == null ? fallback : value;
        return Math.max(min, Math.min(max, resolved));
    }

    private static Map<String, String> orderedMap(String... entries) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            result.put(entries[index], entries[index + 1]);
        }
        return result;
    }
}
