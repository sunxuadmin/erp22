package org.dromara.crehn.cms.service.impl;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.crehn.domain.PortalHomeComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 门户首页布局、组件和主题的服务端权威校验。
 */
final class PortalHomeConfigurationValidator {

    private static final int MAX_LAYOUT_COMPONENTS = 100;

    private static final Set<String> COMPONENT_TYPES = Set.of(
        "hero", "news", "notice", "activity", "schedule", "media", "stat", "showcase", "links",
        "competition-nav", "competition-hero", "competition-key-facts", "competition-tracks",
        "competition-art-tech", "competition-journey", "competition-file-specs", "competition-timeline",
        "competition-notice-downloads", "competition-contact", "competition-footer"
    );

    private static final Map<String, Set<String>> COMPETITION_CONFIG_KEYS = Map.ofEntries(
        Map.entry("competition-nav", Set.of("brand", "edition", "actionLabel", "sticky")),
        Map.entry("competition-hero", Set.of("eyebrow", "title", "summary", "primaryActionLabel",
            "secondaryActionLabel", "status", "visualStyle", "organizer", "groups", "fee", "uploadDeadline")),
        Map.entry("competition-key-facts", Set.of("title", "item1", "item2", "item3", "item4")),
        Map.entry("competition-tracks", Set.of("eyebrow", "title", "summary", "showLimits", "trackATitle",
            "trackADescription", "trackADirections", "trackBTitle", "trackBDescription", "trackBDirections")),
        Map.entry("competition-art-tech", Set.of("eyebrow", "title", "summary", "items")),
        Map.entry("competition-journey", Set.of("eyebrow", "title", "summary", "items")),
        Map.entry("competition-file-specs", Set.of("eyebrow", "title", "summary", "actionLabel", "items")),
        Map.entry("competition-timeline", Set.of("eyebrow", "title", "summary", "items")),
        Map.entry("competition-notice-downloads", Set.of("eyebrow", "title", "summary", "actionLabel",
            "noticeDate", "noticeTitle", "noticeSummary", "items")),
        Map.entry("competition-contact", Set.of("eyebrow", "title", "summary", "showOrganizers",
            "contacts", "organizers")),
        Map.entry("competition-footer", Set.of("brand", "slogan", "copyright"))
    );

    private static final Set<String> DATA_SOURCES = Set.of(
        "manual", "public_articles", "public_notices", "public_activity", "public_results", "public_stats"
    );

    private static final Set<String> RENDER_VERSIONS = Set.of("v1", "v2");

    private static final Set<String> COLOR_THEME_KEYS = Set.of(
        "pageBgStart", "pageBgMiddle", "pageBgEnd", "brandText", "brandSubtext", "heroTitle",
        "heroGradientMiddle", "heroAccent", "heroSubtitle", "bodyText", "cardTitle", "cardText",
        "sectionTitle", "footerText", "footerLine", "noticeText"
    );

    private static final Map<String, double[]> NUMBER_THEME_RANGES = Map.ofEntries(
        Map.entry("footerFontSize", new double[]{8, 18}),
        Map.entry("glassCardOpacity", new double[]{0.08, 0.96}),
        Map.entry("glassCardMobileOpacity", new double[]{0.08, 0.98}),
        Map.entry("glassBorderOpacity", new double[]{0.1, 1}),
        Map.entry("glassBlur", new double[]{6, 32}),
        Map.entry("glassContrast", new double[]{0.85, 1.3}),
        Map.entry("glassSaturation", new double[]{0.75, 1.6}),
        Map.entry("glassTextOpacity", new double[]{0.5, 0.96}),
        Map.entry("glassHighlightOpacity", new double[]{0.2, 1}),
        Map.entry("glassShadowOpacity", new double[]{0, 0.4}),
        Map.entry("particleOpacity", new double[]{0, 0.6}),
        Map.entry("particleSize", new double[]{1, 6}),
        Map.entry("particleGlow", new double[]{0, 24}),
        Map.entry("topbarOpacity", new double[]{0, 0.96}),
        Map.entry("topbarBorderOpacity", new double[]{0, 1}),
        Map.entry("topbarShadowOpacity", new double[]{0, 0.4}),
        Map.entry("logoTileOpacity", new double[]{0, 0.96}),
        Map.entry("noticeOpacity", new double[]{0, 0.96}),
        Map.entry("noticeBorderOpacity", new double[]{0, 1})
    );

    private static final Set<String> BOOLEAN_THEME_KEYS = Set.of("noticeFollowTopbar");
    private static final Set<String> RGB_CHANNEL_THEME_KEYS = Set.of("blueGlowRgb");
    private static final Set<String> THEME_KEYS;
    private static final Pattern HEX_COLOR = Pattern.compile(
        "^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$"
    );

    static {
        Set<String> keys = new HashSet<>();
        keys.addAll(COLOR_THEME_KEYS);
        keys.addAll(NUMBER_THEME_RANGES.keySet());
        keys.addAll(BOOLEAN_THEME_KEYS);
        keys.addAll(RGB_CHANNEL_THEME_KEYS);
        THEME_KEYS = Set.copyOf(keys);
    }

    private PortalHomeConfigurationValidator() {
    }

    static String normalizeRenderVersion(String renderVersion) {
        String normalized = StringUtils.blankToDefault(renderVersion, "v1").trim();
        if (!RENDER_VERSIONS.contains(normalized)) {
            throw new ServiceException("门户渲染版本未登记：" + normalized);
        }
        return normalized;
    }

    static void normalizeComponent(PortalHomeComponent component, Long expectedSiteId) {
        if (component == null || StringUtils.isBlank(component.getComponentKey())) {
            throw new ServiceException("首页组件标识不能为空");
        }
        if (expectedSiteId != null && component.getSiteId() != null && !expectedSiteId.equals(component.getSiteId())) {
            throw new ServiceException("布局组件不属于当前站点：" + component.getComponentKey());
        }
        if (expectedSiteId != null) {
            component.setSiteId(expectedSiteId);
        }
        component.setPageCode(StringUtils.blankToDefault(component.getPageCode(), "home"));
        component.setSortOrder(component.getSortOrder() == null ? 0 : component.getSortOrder());
        component.setGridX(component.getGridX() == null ? 0 : component.getGridX());
        component.setGridY(component.getGridY() == null ? 0 : component.getGridY());
        component.setGridW(component.getGridW() == null ? 12 : component.getGridW());
        component.setGridH(component.getGridH() == null ? 1 : component.getGridH());
        component.setEnabled(component.getEnabled() == null || component.getEnabled());
        if (component.getGridX() < 0 || component.getGridX() > 11 || component.getGridY() < 0
            || component.getGridW() < 1 || component.getGridW() > 12
            || component.getGridX() + component.getGridW() > 12 || component.getGridH() < 1) {
            throw new ServiceException("首页组件栅格位置不正确：" + component.getComponentKey());
        }
        if (StringUtils.isNotBlank(component.getConfigJson()) && !JsonUtils.isJsonObject(component.getConfigJson())) {
            throw new ServiceException("首页组件配置必须是JSON对象：" + component.getComponentKey());
        }
        if (!COMPONENT_TYPES.contains(component.getComponentType())) {
            throw new ServiceException("首页组件类型不在白名单：" + component.getComponentType());
        }
        if (!DATA_SOURCES.contains(component.getDataSourceCode())) {
            throw new ServiceException("首页组件数据源不在白名单：" + component.getDataSourceCode());
        }
        validateComponentConfig(component);
    }

    static String normalizeComponentSnapshotJson(String componentJson, Long siteId) {
        return JsonUtils.toJsonString(parseAndNormalizeComponentSnapshot(componentJson, siteId));
    }

    static List<PortalHomeComponent> parseAndNormalizeComponentSnapshot(String componentJson, Long siteId) {
        if (!JsonUtils.isJsonArray(componentJson)) {
            throw new ServiceException("布局组件必须是JSON数组");
        }
        List<PortalHomeComponent> parsed = JsonUtils.parseArray(componentJson, PortalHomeComponent.class);
        if (parsed == null || parsed.size() > MAX_LAYOUT_COMPONENTS) {
            throw new ServiceException("布局组件数量超出限制");
        }
        List<PortalHomeComponent> components = new ArrayList<>(parsed);
        components.forEach(component -> normalizeComponent(component, siteId));
        Set<String> componentKeys = new HashSet<>();
        for (PortalHomeComponent component : components) {
            if (!componentKeys.add(component.getComponentKey())) {
                throw new ServiceException("布局组件标识重复：" + component.getComponentKey());
            }
        }
        return components;
    }

    private static void validateComponentConfig(PortalHomeComponent component) {
        Set<String> allowedKeys = COMPETITION_CONFIG_KEYS.get(component.getComponentType());
        if (allowedKeys == null || StringUtils.isBlank(component.getConfigJson())) {
            return;
        }
        Map<String, Object> config = JsonUtils.parseObject(component.getConfigJson(), Map.class);
        if (config == null || !allowedKeys.containsAll(config.keySet())) {
            throw new ServiceException("首页组件配置包含未登记字段：" + component.getComponentKey());
        }
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof String) && !(value instanceof Boolean) && !(value instanceof Number)) {
                throw new ServiceException("首页组件配置字段类型不正确：" + entry.getKey());
            }
            if (value instanceof String text) {
                String normalized = text.toLowerCase(Locale.ROOT);
                if (text.length() > 2000 || normalized.contains("<script")
                    || normalized.contains("javascript:") || normalized.contains("data:text/html")) {
                    throw new ServiceException("首页组件配置文本不安全或过长：" + entry.getKey());
                }
            }
        }
    }

    static void validateThemeJson(String themeJson) {
        if (StringUtils.isBlank(themeJson)) {
            return;
        }
        if (!JsonUtils.isJsonObject(themeJson)) {
            throw new ServiceException("主题配置必须是JSON对象");
        }
        Map<String, Object> theme = JsonUtils.parseObject(themeJson, Map.class);
        if (theme == null || !THEME_KEYS.containsAll(theme.keySet())) {
            throw new ServiceException("主题配置包含未登记的样式令牌");
        }
        theme.forEach(PortalHomeConfigurationValidator::validateThemeValue);
    }

    private static void validateThemeValue(String key, Object value) {
        if (COLOR_THEME_KEYS.contains(key)) {
            if (!(value instanceof String color) || !isSafeCssColor(color)) {
                throw new ServiceException("主题颜色格式不正确：" + key);
            }
            return;
        }
        if (RGB_CHANNEL_THEME_KEYS.contains(key)) {
            if (!(value instanceof String channels) || !areRgbChannels(channels)) {
                throw new ServiceException("主题RGB通道格式不正确：" + key);
            }
            return;
        }
        double[] range = NUMBER_THEME_RANGES.get(key);
        if (range != null) {
            if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())
                || number.doubleValue() < range[0] || number.doubleValue() > range[1]) {
                throw new ServiceException("主题数值超出范围：" + key);
            }
            return;
        }
        if (BOOLEAN_THEME_KEYS.contains(key) && !(value instanceof Boolean)) {
            throw new ServiceException("主题开关必须是布尔值：" + key);
        }
    }

    private static boolean isSafeCssColor(String value) {
        String normalized = value.trim();
        if (HEX_COLOR.matcher(normalized).matches() || "transparent".equalsIgnoreCase(normalized)) {
            return true;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        boolean rgba = lower.startsWith("rgba(") && lower.endsWith(")");
        boolean rgb = lower.startsWith("rgb(") && lower.endsWith(")");
        if (!rgba && !rgb) {
            return false;
        }
        String[] parts = normalized.substring(normalized.indexOf('(') + 1, normalized.length() - 1).split(",", -1);
        if (parts.length != (rgba ? 4 : 3) || !areRgbChannels(String.join(",", parts[0], parts[1], parts[2]))) {
            return false;
        }
        if (!rgba) {
            return true;
        }
        try {
            double alpha = Double.parseDouble(parts[3].trim());
            return Double.isFinite(alpha) && alpha >= 0 && alpha <= 1;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean areRgbChannels(String value) {
        String[] channels = value.split(",", -1);
        if (channels.length != 3) {
            return false;
        }
        try {
            for (String channel : channels) {
                int parsed = Integer.parseInt(channel.trim());
                if (parsed < 0 || parsed > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
