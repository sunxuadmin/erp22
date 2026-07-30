package org.dromara.crehn.result.service.warning;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.ReviewResultLog;
import org.dromara.crehn.domain.ReviewScoreWarningConfig;
import org.dromara.crehn.domain.bo.ReviewScoreWarningConfigBo;
import org.dromara.crehn.domain.vo.ReviewScoreWarningConfigVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ActivityMapper;
import org.dromara.crehn.mapper.ReviewResultLogMapper;
import org.dromara.crehn.mapper.ReviewScoreWarningConfigMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Locale;

/**
 * Activity/category scoped score-discrepancy configuration and calculation.
 *
 * <p>Formula names are a deliberately closed set. Arbitrary expression
 * evaluation would turn an admin setting into a code-execution surface and
 * would make historical results impossible to reproduce.</p>
 */
@RequiredArgsConstructor
@Service
public class ReviewScoreWarningService {

    public static final String FORMULA_RANGE_AVERAGE = "range_average";
    public static final String FORMULA_RANGE_MAX = "range_max";
    public static final String FORMULA_RANGE_MIN = "range_min";
    public static final String FORMULA_CUSTOM = "custom";
    private static final String DEFAULT_RANGE_AVERAGE_EXPRESSION = "(RANGE / AVG) * 100";
    private static final String DEFAULT_RANGE_MAX_EXPRESSION = "(RANGE / MAX) * 100";
    private static final String DEFAULT_RANGE_MIN_EXPRESSION = "(RANGE / MIN) * 100";
    private static final String DEFAULT_CUSTOM_EXPRESSION = DEFAULT_RANGE_AVERAGE_EXPRESSION;
    private static final Set<String> FORMULAS = Set.of(FORMULA_RANGE_AVERAGE, FORMULA_RANGE_MAX, FORMULA_RANGE_MIN, FORMULA_CUSTOM);
    private static final BigDecimal DEFAULT_THRESHOLD = BigDecimal.TEN;
    private static final String DEFAULT_NORMAL_TEXT = "正常";
    private static final String DEFAULT_WARNING_TEXT = "需复核";
    private static final String DEFAULT_INSUFFICIENT_TEXT = "数据不足";
    private static final String DEFAULT_UNSUPPORTED_TEXT = "无法计算";
    private static final String STATUS_DISABLED = "disabled";
    private static final String STATUS_INSUFFICIENT = "insufficient";
    private static final String STATUS_UNSUPPORTED = "unsupported";
    private static final String STATUS_WARNING = "warning";
    private static final String STATUS_NORMAL = "normal";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int TEXT_MAX_LENGTH = 100;
    private static final int DEFAULT_SCORE_COLUMN_COUNT = 3;
    private static final int MAX_SCORE_COLUMN_COUNT = 20;
    private static final int MAX_COLUMNS_JSON_LENGTH = 20000;
    private static final int MAX_FORMULA_EXPRESSION_LENGTH = 200;
    private static final Set<String> SUMMARY_COLUMN_KEYS = Set.of(
        "rowNo", "projectName", "categoryName", "programForm", "groupOrNature", "schoolName",
        "currentAverageScore", "warningText", "actions");

    private final ReviewScoreWarningConfigMapper configMapper;
    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final ReviewResultLogMapper resultLogMapper;

    /** Returns the saved rule, or the stable default rule when none exists. */
    public ScoreWarningRule resolve(Long activityId, Long categoryId) {
        if (activityId == null || categoryId == null) {
            return defaultRule(null, null);
        }
        ReviewScoreWarningConfig config = findConfig(activityId, categoryId);
        return config == null
            ? defaultRule(activityId, categoryId)
            : toRule(config);
    }

    public ReviewScoreWarningConfigVo getConfig(Long activityId, Long categoryId) {
        requireScope(activityId, categoryId);
        ReviewScoreWarningConfig config = findConfig(activityId, categoryId);
        return toVo(config == null ? defaultConfig(activityId, categoryId) : config);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreWarningConfigVo saveConfig(ReviewScoreWarningConfigBo bo) {
        if (bo == null) {
            throw new ServiceException("评分提示规则不能为空");
        }
        requireScope(bo.getActivityId(), bo.getCategoryId());
        String formulaType = normalizeFormula(bo.getFormulaType());
        String formulaExpression = normalizeFormulaExpression(formulaType, bo.getFormulaExpression());
        BigDecimal threshold = normalizeThreshold(bo.getThresholdPercent());
        ReviewScoreWarningConfig before = findConfig(bo.getActivityId(), bo.getCategoryId());
        ReviewScoreWarningConfig config = before == null ? new ReviewScoreWarningConfig() : before;
        config.setActivityId(bo.getActivityId());
        config.setCategoryId(bo.getCategoryId());
        config.setEnabled(bo.getEnabled() == null || bo.getEnabled());
        config.setFormulaType(formulaType);
        config.setFormulaExpression(formulaExpression);
        config.setThresholdPercent(threshold);
        config.setNormalText(normalizeText(bo.getNormalText(), DEFAULT_NORMAL_TEXT));
        config.setWarningText(normalizeText(bo.getWarningText(), DEFAULT_WARNING_TEXT));
        config.setInsufficientText(normalizeText(bo.getInsufficientText(), DEFAULT_INSUFFICIENT_TEXT));
        config.setUnsupportedText(normalizeText(bo.getUnsupportedText(), DEFAULT_UNSUPPORTED_TEXT));
        config.setColumnsJson(normalizeColumnsJson(bo.getColumnsJson()));
        config.setScoreColumnCount(normalizeScoreColumnCount(bo.getScoreColumnCount()));
        config.setRemark(StringUtils.trim(bo.getRemark()));
        if (config.getId() == null) {
            try {
                configMapper.insert(config);
            } catch (DuplicateKeyException ex) {
                // A concurrent first save is retried as an update so the
                // endpoint remains idempotent for two administrator windows.
                config = findConfig(bo.getActivityId(), bo.getCategoryId());
                if (config == null) {
                    throw new ServiceException("评分提示规则保存失败，请刷新后重试");
                }
                config.setEnabled(bo.getEnabled() == null || bo.getEnabled());
                config.setFormulaType(formulaType);
                config.setFormulaExpression(formulaExpression);
                config.setThresholdPercent(threshold);
                config.setNormalText(normalizeText(bo.getNormalText(), DEFAULT_NORMAL_TEXT));
                config.setWarningText(normalizeText(bo.getWarningText(), DEFAULT_WARNING_TEXT));
                config.setInsufficientText(normalizeText(bo.getInsufficientText(), DEFAULT_INSUFFICIENT_TEXT));
                config.setUnsupportedText(normalizeText(bo.getUnsupportedText(), DEFAULT_UNSUPPORTED_TEXT));
                config.setColumnsJson(normalizeColumnsJson(bo.getColumnsJson()));
                config.setScoreColumnCount(normalizeScoreColumnCount(bo.getScoreColumnCount()));
                config.setRemark(StringUtils.trim(bo.getRemark()));
                configMapper.updateById(config);
            }
        } else {
            configMapper.updateById(config);
        }
        ReviewScoreWarningConfig latest = findConfig(bo.getActivityId(), bo.getCategoryId());
        logConfigChange(bo.getActivityId(), bo.getCategoryId(), before, latest);
        return toVo(latest == null ? config : latest);
    }

    /**
     * Computes a warning from all submitted numeric values. The caller passes
     * submittedCount separately because grade-only or malformed scores are
     * still submitted records but cannot be used by a numeric formula.
     */
    public ScoreWarningEvaluation evaluate(List<BigDecimal> values, int submittedCount, ScoreWarningRule rule) {
        ScoreWarningRule effective = rule == null ? defaultRule(null, null) : rule;
        if (!effective.enabled()) {
            return evaluation(STATUS_DISABLED, effective.normalText(), null, null, null, null, values, effective);
        }
        List<BigDecimal> numeric = values == null ? List.of() : values.stream().filter(Objects::nonNull).toList();
        if (numeric.size() < 2) {
            String status = submittedCount >= 2 ? STATUS_UNSUPPORTED : STATUS_INSUFFICIENT;
            String text = submittedCount >= 2 ? effective.unsupportedText() : effective.insufficientText();
            return evaluation(status, text, null, null, null, null, numeric, effective);
        }
        BigDecimal max = numeric.stream().max(BigDecimal::compareTo).orElse(null);
        BigDecimal min = numeric.stream().min(BigDecimal::compareTo).orElse(null);
        BigDecimal average = numeric.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(numeric.size()), 10, RoundingMode.HALF_UP);
        BigDecimal denominator = switch (effective.formulaType()) {
            case FORMULA_RANGE_MAX -> max;
            case FORMULA_RANGE_MIN -> min;
            default -> average;
        };
        BigDecimal difference = FORMULA_CUSTOM.equals(effective.formulaType())
            ? evaluateCustomExpression(effective.formulaExpression(), max, min, average, numeric.size())
            : percentageDifference(max, min, denominator);
        String status = difference == null ? STATUS_UNSUPPORTED
            : difference.compareTo(effective.thresholdPercent()) > 0 ? STATUS_WARNING : STATUS_NORMAL;
        String text = STATUS_UNSUPPORTED.equals(status) ? effective.unsupportedText()
            : STATUS_WARNING.equals(status) ? effective.warningText() : effective.normalText();
        return evaluation(status, text, max, min, average, difference, numeric, effective);
    }

    private ScoreWarningEvaluation evaluation(String status, String displayText, BigDecimal max, BigDecimal min,
                                              BigDecimal average, BigDecimal difference, List<BigDecimal> values,
                                              ScoreWarningRule rule) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("enabled", rule.enabled());
        snapshot.put("configId", rule.configId());
        snapshot.put("activityId", rule.activityId());
        snapshot.put("categoryId", rule.categoryId());
        snapshot.put("formulaType", rule.formulaType());
        snapshot.put("formulaExpression", rule.formulaExpression());
        snapshot.put("thresholdPercent", rule.thresholdPercent());
        snapshot.put("normalText", rule.normalText());
        snapshot.put("warningText", rule.warningText());
        snapshot.put("insufficientText", rule.insufficientText());
        snapshot.put("unsupportedText", rule.unsupportedText());
        snapshot.put("status", status);
        snapshot.put("displayText", displayText);
        // Keep the short names used by the score-summary read model while
        // retaining explicit names for audit consumers.
        snapshot.put("text", displayText);
        snapshot.put("maxScore", scale(max));
        snapshot.put("minScore", scale(min));
        snapshot.put("averageScore", scale(average));
        snapshot.put("differencePercent", scale(difference));
        snapshot.put("spreadPercent", scale(difference));
        snapshot.put("evaluatedCount", values == null ? 0 : values.size());
        return new ScoreWarningEvaluation(status, displayText, scale(max), scale(min), scale(average), scale(difference),
            values == null ? 0 : values.size(), snapshot);
    }

    private BigDecimal percentageDifference(BigDecimal max, BigDecimal min, BigDecimal denominator) {
        if (max == null || min == null) {
            return null;
        }
        BigDecimal range = max.subtract(min).abs();
        if (range.signum() == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (denominator == null || denominator.signum() == 0) {
            return HUNDRED.setScale(2, RoundingMode.HALF_UP);
        }
        return range.multiply(HUNDRED).divide(denominator.abs(), 10, RoundingMode.HALF_UP);
    }

    private ReviewScoreWarningConfig findConfig(Long activityId, Long categoryId) {
        return configMapper.selectOne(Wrappers.lambdaQuery(ReviewScoreWarningConfig.class)
            .eq(ReviewScoreWarningConfig::getActivityId, activityId)
            .eq(ReviewScoreWarningConfig::getCategoryId, categoryId)
            .last("limit 1"));
    }

    private void requireScope(Long activityId, Long categoryId) {
        if (activityId == null || categoryId == null) {
            throw new ServiceException("请选择活动和类别");
        }
        Activity activity = activityMapper.selectById(activityId);
        ActivityCategory category = categoryMapper.selectById(categoryId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        if (category == null || !Objects.equals(category.getActivityId(), activityId)) {
            throw new ServiceException("类别不属于当前活动");
        }
    }

    private ReviewScoreWarningConfig defaultConfig(Long activityId, Long categoryId) {
        ReviewScoreWarningConfig config = new ReviewScoreWarningConfig();
        config.setActivityId(activityId);
        config.setCategoryId(categoryId);
        config.setEnabled(true);
        config.setFormulaType(FORMULA_RANGE_AVERAGE);
        config.setFormulaExpression(DEFAULT_RANGE_AVERAGE_EXPRESSION);
        config.setThresholdPercent(DEFAULT_THRESHOLD);
        config.setNormalText(DEFAULT_NORMAL_TEXT);
        config.setWarningText(DEFAULT_WARNING_TEXT);
        config.setInsufficientText(DEFAULT_INSUFFICIENT_TEXT);
        config.setUnsupportedText(DEFAULT_UNSUPPORTED_TEXT);
        config.setScoreColumnCount(DEFAULT_SCORE_COLUMN_COUNT);
        return config;
    }

    private ScoreWarningRule defaultRule(Long activityId, Long categoryId) {
        ReviewScoreWarningConfig config = defaultConfig(activityId, categoryId);
        return toRule(config);
    }

    private ScoreWarningRule toRule(ReviewScoreWarningConfig config) {
        return new ScoreWarningRule(
            Boolean.TRUE.equals(config.getEnabled()),
            normalizeFormula(config.getFormulaType()),
            normalizeFormulaExpression(normalizeFormula(config.getFormulaType()), config.getFormulaExpression()),
            normalizeThreshold(config.getThresholdPercent()),
            normalizeText(config.getNormalText(), DEFAULT_NORMAL_TEXT),
            normalizeText(config.getWarningText(), DEFAULT_WARNING_TEXT),
            normalizeText(config.getInsufficientText(), DEFAULT_INSUFFICIENT_TEXT),
            normalizeText(config.getUnsupportedText(), DEFAULT_UNSUPPORTED_TEXT),
            config.getId(), config.getActivityId(), config.getCategoryId());
    }

    private ReviewScoreWarningConfigVo toVo(ReviewScoreWarningConfig config) {
        ReviewScoreWarningConfigVo vo = new ReviewScoreWarningConfigVo();
        vo.setId(config.getId());
        vo.setActivityId(config.getActivityId());
        vo.setCategoryId(config.getCategoryId());
        vo.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
        vo.setFormulaType(normalizeFormula(config.getFormulaType()));
        vo.setFormulaExpression(normalizeFormulaExpression(vo.getFormulaType(), config.getFormulaExpression()));
        vo.setThresholdPercent(normalizeThreshold(config.getThresholdPercent()));
        vo.setNormalText(normalizeText(config.getNormalText(), DEFAULT_NORMAL_TEXT));
        vo.setWarningText(normalizeText(config.getWarningText(), DEFAULT_WARNING_TEXT));
        vo.setInsufficientText(normalizeText(config.getInsufficientText(), DEFAULT_INSUFFICIENT_TEXT));
        vo.setUnsupportedText(normalizeText(config.getUnsupportedText(), DEFAULT_UNSUPPORTED_TEXT));
        vo.setColumnsJson(normalizeColumnsJson(config.getColumnsJson()));
        vo.setScoreColumnCount(normalizeScoreColumnCount(config.getScoreColumnCount()));
        vo.setRemark(config.getRemark());
        vo.setCreateTime(config.getCreateTime());
        vo.setUpdateTime(config.getUpdateTime());
        return vo;
    }

    private String normalizeFormula(String formulaType) {
        String value = StringUtils.trim(formulaType);
        if (StringUtils.isBlank(value)) {
            return FORMULA_RANGE_AVERAGE;
        }
        value = value.toLowerCase(Locale.ROOT);
        if (!FORMULAS.contains(value)) {
            throw new ServiceException("评分提示公式不支持：" + value);
        }
        return value;
    }

    private String normalizeFormulaExpression(String formulaType, String expression) {
        String normalizedType = normalizeFormula(formulaType);
        if (!FORMULA_CUSTOM.equals(normalizedType)) {
            return switch (normalizedType) {
                case FORMULA_RANGE_MAX -> DEFAULT_RANGE_MAX_EXPRESSION;
                case FORMULA_RANGE_MIN -> DEFAULT_RANGE_MIN_EXPRESSION;
                default -> DEFAULT_RANGE_AVERAGE_EXPRESSION;
            };
        }
        String normalized = StringUtils.trim(expression);
        if (StringUtils.isBlank(normalized)) {
            normalized = DEFAULT_CUSTOM_EXPRESSION;
        }
        if (normalized.length() > MAX_FORMULA_EXPRESSION_LENGTH) {
            throw new ServiceException("自定义评分差异公式不能超过" + MAX_FORMULA_EXPRESSION_LENGTH + "个字符");
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        try {
            new RestrictedFormulaParser(normalized, Map.of(
                "MAX", BigDecimal.valueOf(90),
                "MIN", BigDecimal.valueOf(70),
                "AVG", BigDecimal.valueOf(80),
                "COUNT", BigDecimal.valueOf(3),
                "RANGE", BigDecimal.valueOf(20))).parse();
        } catch (RuntimeException ex) {
            throw new ServiceException("自定义评分差异公式无效，仅支持 MAX、MIN、AVG、COUNT、RANGE、数字、四则运算和括号");
        }
        return normalized;
    }

    private BigDecimal evaluateCustomExpression(String expression, BigDecimal max, BigDecimal min,
                                                BigDecimal average, int count) {
        try {
            return new RestrictedFormulaParser(expression, Map.of(
                "MAX", max,
                "MIN", min,
                "AVG", average,
                "COUNT", BigDecimal.valueOf(count),
                "RANGE", max.subtract(min).abs())).parse();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private BigDecimal normalizeThreshold(BigDecimal value) {
        BigDecimal normalized = value == null ? DEFAULT_THRESHOLD : value;
        if (normalized.compareTo(BigDecimal.ZERO) < 0 || normalized.compareTo(BigDecimal.valueOf(10000)) > 0) {
            throw new ServiceException("评分提示阈值必须在0到10000之间");
        }
        return normalized.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeText(String value, String fallback) {
        String normalized = StringUtils.trim(value);
        if (StringUtils.isBlank(normalized)) {
            return fallback;
        }
        if (normalized.length() > TEXT_MAX_LENGTH) {
            throw new ServiceException("评分提示文案不能超过" + TEXT_MAX_LENGTH + "个字符");
        }
        return normalized;
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private Integer normalizeScoreColumnCount(Integer value) {
        int count = value == null ? DEFAULT_SCORE_COLUMN_COUNT : value;
        if (count < 1 || count > MAX_SCORE_COLUMN_COUNT) {
            throw new ServiceException("评分列数量必须在1到" + MAX_SCORE_COLUMN_COUNT + "之间");
        }
        return count;
    }

    private String normalizeColumnsJson(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (value.length() > MAX_COLUMNS_JSON_LENGTH) {
            throw new ServiceException("汇总字段配置过长");
        }
        try {
            List<Map<String, Object>> columns = JsonUtils.parseObject(value,
                new TypeReference<List<Map<String, Object>>>() { });
            if (columns == null || columns.size() > 30) {
                throw new ServiceException("汇总字段配置数量无效");
            }
            List<Map<String, Object>> normalized = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (Map<String, Object> column : columns) {
                if (column == null) {
                    continue;
                }
                String fieldKey = StringUtils.trim(String.valueOf(column.get("fieldKey")));
                if ((!SUMMARY_COLUMN_KEYS.contains(fieldKey) && !fieldKey.matches("score(?:[1-9]|1[0-9]|20)")) || !seen.add(fieldKey)) {
                    throw new ServiceException("汇总字段不支持或重复：" + fieldKey);
                }
                String label = StringUtils.trim(String.valueOf(column.get("label")));
                if (StringUtils.isBlank(label) || label.length() > 30) {
                    throw new ServiceException("汇总字段表头不能为空且不能超过30个字符");
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("fieldKey", fieldKey);
                item.put("label", label);
                item.put("visible", !Boolean.FALSE.equals(column.get("visible")));
                item.put("sortOrder", toInt(column.get("sortOrder"), normalized.size() + 1, 1, 99));
                item.put("width", toInt(column.get("width"), 120, 70, 420));
                normalized.add(item);
            }
            return normalized.isEmpty() ? null : JsonUtils.toJsonString(normalized);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("汇总字段配置格式无效");
        }
    }

    private int toInt(Object value, int fallback, int min, int max) {
        try {
            int parsed = value == null ? fallback : new BigDecimal(String.valueOf(value)).intValueExact();
            if (parsed < min || parsed > max) {
                throw new ServiceException("汇总字段数值超出范围");
            }
            return parsed;
        } catch (ArithmeticException | NumberFormatException ex) {
            throw new ServiceException("汇总字段数值无效");
        }
    }

    private void logConfigChange(Long activityId, Long categoryId, ReviewScoreWarningConfig before, ReviewScoreWarningConfig after) {
        ReviewResultLog log = new ReviewResultLog();
        log.setActivityId(activityId);
        log.setCategoryId(categoryId);
        log.setTargetType(ArtReviewConstants.RESULT_LOG_TARGET_RESULT);
        log.setActionType(ArtReviewConstants.RESULT_LOG_ACTION_SCORE_WARNING_RULE_SAVE);
        log.setBeforeJson(before == null ? null : JsonUtils.toJsonString(toVo(before)));
        log.setAfterJson(after == null ? null : JsonUtils.toJsonString(toVo(after)));
        log.setReason("保存评分差异预警规则");
        log.setOperatedBy(LoginHelper.getUserId());
        log.setOperatedAt(new Date());
        resultLogMapper.insert(log);
    }

    public record ScoreWarningRule(boolean enabled, String formulaType, String formulaExpression,
                                   BigDecimal thresholdPercent,
                                   String normalText, String warningText, String insufficientText,
                                   String unsupportedText, Long configId, Long activityId, Long categoryId) {
    }

    public record ScoreWarningEvaluation(String status, String displayText, BigDecimal maxScore, BigDecimal minScore,
                                         BigDecimal averageScore, BigDecimal differencePercent, int evaluatedCount,
                                          Map<String, Object> snapshot) {
    }

    /** Small, non-evaluating expression parser for the administrator formula field. */
    private static final class RestrictedFormulaParser {
        private final String expression;
        private final Map<String, BigDecimal> variables;
        private int position;

        private RestrictedFormulaParser(String expression, Map<String, BigDecimal> variables) {
            this.expression = expression == null ? "" : expression;
            this.variables = variables;
        }

        private BigDecimal parse() {
            BigDecimal value = parseExpression();
            skipWhitespace();
            if (position != expression.length()) {
                throw new IllegalArgumentException("unexpected formula token");
            }
            return value;
        }

        private BigDecimal parseExpression() {
            BigDecimal value = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    value = value.add(parseTerm());
                } else if (match('-')) {
                    value = value.subtract(parseTerm());
                } else {
                    return value;
                }
            }
        }

        private BigDecimal parseTerm() {
            BigDecimal value = parseFactor();
            while (true) {
                skipWhitespace();
                if (match('*')) {
                    value = value.multiply(parseFactor());
                } else if (match('/')) {
                    BigDecimal divisor = parseFactor();
                    if (divisor.signum() == 0) {
                        throw new ArithmeticException("division by zero");
                    }
                    value = value.divide(divisor, 10, RoundingMode.HALF_UP);
                } else {
                    return value;
                }
            }
        }

        private BigDecimal parseFactor() {
            skipWhitespace();
            if (match('+')) {
                return parseFactor();
            }
            if (match('-')) {
                return parseFactor().negate();
            }
            if (match('(')) {
                BigDecimal value = parseExpression();
                skipWhitespace();
                if (!match(')')) {
                    throw new IllegalArgumentException("missing closing parenthesis");
                }
                return value;
            }
            if (position >= expression.length()) {
                throw new IllegalArgumentException("missing formula value");
            }
            char current = expression.charAt(position);
            if (Character.isLetter(current)) {
                int start = position++;
                while (position < expression.length() && Character.isLetter(expression.charAt(position))) {
                    position++;
                }
                String name = expression.substring(start, position);
                BigDecimal value = variables.get(name);
                if (value == null) {
                    throw new IllegalArgumentException("unsupported formula variable");
                }
                return value;
            }
            int start = position;
            boolean decimalPoint = false;
            while (position < expression.length()) {
                char digit = expression.charAt(position);
                if (Character.isDigit(digit)) {
                    position++;
                } else if (digit == '.' && !decimalPoint) {
                    decimalPoint = true;
                    position++;
                } else {
                    break;
                }
            }
            if (start == position) {
                throw new IllegalArgumentException("unsupported formula character");
            }
            return new BigDecimal(expression.substring(start, position));
        }

        private boolean match(char expected) {
            if (position < expression.length() && expression.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (position < expression.length() && Character.isWhitespace(expression.charAt(position))) {
                position++;
            }
        }
    }
}
