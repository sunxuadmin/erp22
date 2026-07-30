package org.dromara.crehn.project.service;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.CategoryFileRequirement;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class FileRequirementRuleChecker {

    public FileRuleCheckResult check(CategoryFileRequirement requirement, MediaMetadata metadata, String originalName, String ext, long size) {
        return check(requirement, Map.of(), metadata, originalName, ext, size);
    }

    public FileRuleCheckResult check(CategoryFileRequirement requirement, Map<String, Object> categoryRule, MediaMetadata metadata, String originalName, String ext, long size) {
        FileRuleCheckResult result = new FileRuleCheckResult();
        Map<String, Object> rule = parseRule(requirement.getRuleJson());
        if (rule.isEmpty()) {
            return result;
        }
        String expectedMediaType = string(rule.get("mediaType"));
        if (StringUtils.isNotBlank(expectedMediaType) && !expectedMediaType.equalsIgnoreCase(metadata.getMediaType())) {
            fail("文件类型不符合要求，请上传" + mediaTypeLabel(expectedMediaType) + "文件");
        }
        checkSize(rule, size);
        checkFileName(rule, originalName);
        if ((technicalTipsOnly(rule) || technicalTipsOnly(categoryRule)) && hasTechnicalParameterRule(rule)) {
            result.warn("技术参数已配置为人工复核，请提交后等待审核");
            return result;
        }
        if ("image".equals(metadata.getMediaType())) {
            checkImage(rule, metadata, result);
        }
        if ("video".equals(metadata.getMediaType())) {
            checkVideo(rule, metadata);
        }
        return result;
    }

    public boolean technicalTipsOnly(CategoryFileRequirement requirement) {
        return technicalTipsOnly(parseRule(requirement == null ? null : requirement.getRuleJson()));
    }

    public boolean technicalTipsOnly(Map<String, Object> rule) {
        if (rule == null || rule.isEmpty()) {
            return false;
        }
        String mode = string(rule.get("technicalCheckMode"));
        if (StringUtils.isBlank(mode)) {
            mode = string(rule.get("mediaTechnicalCheckMode"));
        }
        if (StringUtils.isBlank(mode)) {
            return false;
        }
        String normalized = mode.toLowerCase(Locale.ROOT);
        return normalized.contains("tip") || normalized.contains("manual") || normalized.contains("warn") || normalized.contains("format_only") || normalized.contains("提示");
    }

    public List<String> manualTips(CategoryFileRequirement requirement) {
        Map<String, Object> rule = parseRule(requirement.getRuleJson());
        Object tips = rule.get("manualCheckTips");
        List<String> values = new ArrayList<>();
        if (tips instanceof Iterable<?> iterable) {
            for (Object tip : iterable) {
                if (tip != null && StringUtils.isNotBlank(String.valueOf(tip))) {
                    values.add(String.valueOf(tip));
                }
            }
        } else if (tips != null && StringUtils.isNotBlank(String.valueOf(tips))) {
            values.add(String.valueOf(tips));
        }
        return values;
    }

    private void checkSize(Map<String, Object> rule, long size) {
        Double minMb = number(rule.get("minMb"));
        Double maxMb = number(rule.get("maxMb"));
        double mb = size / 1024.0 / 1024.0;
        if (minMb != null && mb < minMb) {
            fail("文件大小不能小于 " + formatMb(minMb));
        }
        if (maxMb != null && mb > maxMb) {
            fail("文件大小不能超过 " + formatMb(maxMb));
        }
    }

    private void checkFileName(Map<String, Object> rule, String originalName) {
        String pattern = string(rule.get("filenamePattern"));
        if (StringUtils.isNotBlank(pattern) && !Pattern.compile(pattern).matcher(StringUtils.blankToDefault(originalName, "")).find()) {
            fail("文件名不符合要求，请按提示重新命名后上传");
        }
        Object parts = rule.get("filenameRequiredParts");
        if (parts instanceof Iterable<?> iterable) {
            for (Object part : iterable) {
                if (part != null && !StringUtils.blankToDefault(originalName, "").contains(String.valueOf(part))) {
                    fail("文件名必须包含：" + part);
                }
            }
        }
    }

    private void checkImage(Map<String, Object> rule, MediaMetadata metadata, FileRuleCheckResult result) {
        Integer minWidth = integer(rule.get("minWidth"));
        Integer minHeight = integer(rule.get("minHeight"));
        Integer dpi = integer(rule.get("dpi"));
        if (minWidth != null && (metadata.getWidth() == null || metadata.getWidth() < minWidth)) {
            fail("图片宽度不能低于 " + minWidth + " 像素");
        }
        if (minHeight != null && (metadata.getHeight() == null || metadata.getHeight() < minHeight)) {
            fail("图片高度不能低于 " + minHeight + " 像素");
        }
        if (dpi != null) {
            if (metadata.getDpi() == null) {
                result.warn("图片 DPI 无法识别，需人工复核");
            } else if (metadata.getDpi() < dpi) {
                fail("图片 DPI 不能低于 " + dpi);
            }
        }
    }

    private void checkVideo(Map<String, Object> rule, MediaMetadata metadata) {
        if (!metadata.hasTechnicalValues()) {
            fail("视频参数无法识别，请上传有效视频文件或联系管理员确认 ffprobe 服务");
        }
        Integer minWidth = integer(rule.get("minWidth"));
        Integer minHeight = integer(rule.get("minHeight"));
        Double fps = number(rule.get("fps"));
        Double fpsTolerance = number(rule.get("fpsTolerance"));
        Double minBitrateMbps = number(rule.get("minBitrateMbps"));
        Double minDuration = number(rule.get("minDurationSeconds"));
        Double maxDuration = number(rule.get("maxDurationSeconds"));
        if (minWidth != null && (metadata.getWidth() == null || metadata.getWidth() < minWidth)
            || minHeight != null && (metadata.getHeight() == null || metadata.getHeight() < minHeight)) {
            fail("视频分辨率不能低于 " + StringUtils.blankToDefault(String.valueOf(minWidth), "?") + "x" + StringUtils.blankToDefault(String.valueOf(minHeight), "?"));
        }
        if (fps != null) {
            double tolerance = fpsTolerance == null ? 0.5D : fpsTolerance;
            if (metadata.getFps() == null || Math.abs(metadata.getFps() - fps) > tolerance) {
                fail("视频帧率需约为 " + fps + "fps");
            }
        }
        if (minBitrateMbps != null) {
            long minBitrate = Math.round(minBitrateMbps * 1024 * 1024);
            if (metadata.getBitrate() == null || metadata.getBitrate() < minBitrate) {
                fail("视频码率不能低于 " + minBitrateMbps + "Mbps");
            }
        }
        if (minDuration != null && (metadata.getDurationSeconds() == null || metadata.getDurationSeconds() < minDuration)) {
            fail("视频时长不能少于 " + minDuration.longValue() + " 秒");
        }
        if (maxDuration != null && (metadata.getDurationSeconds() == null || metadata.getDurationSeconds() > maxDuration)) {
            fail("视频时长不能超过 " + maxDuration.longValue() + " 秒");
        }
    }

    private String mediaTypeLabel(String mediaType) {
        return switch (StringUtils.blankToDefault(mediaType, "").toLowerCase(Locale.ROOT)) {
            case "image" -> "图片";
            case "video" -> "视频";
            case "audio" -> "音频";
            case "document", "office" -> "文档";
            default -> mediaType;
        };
    }

    private Map<String, Object> parseRule(String ruleJson) {
        if (StringUtils.isBlank(ruleJson) || !JsonUtils.isJsonObject(ruleJson)) {
            return Map.of();
        }
        Map<String, Object> rule = JsonUtils.parseObject(ruleJson, new TypeReference<Map<String, Object>>() {
        });
        return rule == null ? Map.of() : rule;
    }

    private boolean hasTechnicalParameterRule(Map<String, Object> rule) {
        return List.of("mediaType", "minWidth", "minHeight", "dpi", "fps", "fpsTolerance", "minBitrateMbps", "minDurationSeconds", "maxDurationSeconds")
            .stream()
            .anyMatch(rule::containsKey);
    }

    private String formatMb(Double value) {
        return value % 1 == 0 ? value.longValue() + "MB" : String.format(Locale.ROOT, "%.2fMB", value);
    }

    private Integer integer(Object value) {
        Double number = number(value);
        return number == null ? null : number.intValue();
    }

    private Double number(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private void fail(String message) {
        throw new ServiceException(message);
    }
}
