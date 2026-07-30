package org.dromara.crehn.result.service.generate;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.ReviewAssignment;
import org.dromara.crehn.domain.ReviewAwardRule;
import org.dromara.crehn.domain.ReviewResult;
import org.dromara.crehn.domain.ReviewScore;
import org.dromara.crehn.domain.ReviewScoreSheetSignedSheet;
import org.dromara.crehn.domain.ReviewScoreSheetSignedSheetItem;
import org.dromara.crehn.domain.bo.ReviewResultBo;
import org.dromara.crehn.domain.vo.ReviewResultReadinessVo;
import org.dromara.crehn.mapper.ActivityCategoryMapper;
import org.dromara.crehn.mapper.ActivityMapper;
import org.dromara.crehn.mapper.ProjectMapper;
import org.dromara.crehn.mapper.ReviewAssignmentMapper;
import org.dromara.crehn.mapper.ReviewAwardRuleMapper;
import org.dromara.crehn.mapper.ReviewResultMapper;
import org.dromara.crehn.mapper.ReviewScoreMapper;
import org.dromara.crehn.mapper.ReviewScoreSheetSignedSheetItemMapper;
import org.dromara.crehn.mapper.ReviewScoreSheetSignedSheetMapper;
import org.dromara.crehn.result.service.warning.ReviewScoreWarningService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ART-OWNER: BE.RESULT.GENERATE
 *
 * Owns result aggregation, rank refresh and award matching. Transaction
 * ownership stays on the public result-service facade during this migration.
 */
@RequiredArgsConstructor
@Service
public class ReviewResultGenerateService {

    private static final int ACTIVE_MARKER = 1;
    private static final String SIGNED_SHEET_ACTIVE = "active";
    private static final String SIGNED_SHEET_WITHDRAWN = "withdrawn";
    private static final String WARNING_STATUS_WARNING = "warning";

    private static final Map<String, BigDecimal> DEFAULT_GRADE_POINTS = Map.ofEntries(
        Map.entry("A", BigDecimal.valueOf(100)),
        Map.entry("B", BigDecimal.valueOf(90)),
        Map.entry("C", BigDecimal.valueOf(80)),
        Map.entry("D", BigDecimal.valueOf(70)),
        Map.entry("E", BigDecimal.valueOf(60)),
        Map.entry("F", BigDecimal.valueOf(50)),
        Map.entry("G", BigDecimal.valueOf(40)),
        Map.entry("优秀", BigDecimal.valueOf(100)),
        Map.entry("良好", BigDecimal.valueOf(85)),
        Map.entry("合格", BigDecimal.valueOf(70)),
        Map.entry("不合格", BigDecimal.valueOf(50))
    );

    private final ReviewResultMapper resultMapper;
    private final ReviewAwardRuleMapper awardRuleMapper;
    private final ReviewScoreMapper scoreMapper;
    private final ReviewScoreSheetSignedSheetMapper signedSheetMapper;
    private final ReviewScoreSheetSignedSheetItemMapper signedSheetItemMapper;
    private final ReviewAssignmentMapper assignmentMapper;
    private final ProjectMapper projectMapper;
    private final ActivityMapper activityMapper;
    private final ActivityCategoryMapper categoryMapper;
    private final ReviewScoreWarningService scoreWarningService;

    public void generate(ReviewResultBo bo) {
        lockGenerationScope(bo);
        ReviewResultReadinessVo readiness = checkReadiness(bo);
        if (!readiness.isReady()) {
            String reason = readiness.getBlockers().isEmpty() ? "当前活动类别尚未满足结果生成条件" : readiness.getBlockers().get(0);
            throw new ServiceException("无法生成评审结果：" + reason);
        }
        List<Project> projects = projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
            .eq(Project::getActivityId, bo.getActivityId())
            .eq(Project::getCategoryId, bo.getCategoryId())
            .eq(Project::getStatus, ArtReviewConstants.PROJECT_AUDIT_PASSED)
            .orderByAsc(Project::getProjectNo)
            .orderByAsc(Project::getId));
        if (projects.isEmpty()) {
            throw new ServiceException("当前活动类别没有审核通过的项目，无法生成结果");
        }

        List<ReviewAssignment> activeAssignments = assignmentMapper.selectList(Wrappers.lambdaQuery(ReviewAssignment.class)
            .eq(ReviewAssignment::getActivityId, bo.getActivityId())
            .eq(ReviewAssignment::getCategoryId, bo.getCategoryId())
            .eq(ReviewAssignment::getStatus, ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE));
        ScoreAggregateRule aggregateRule = resolveAggregateRule(activeAssignments);
        ReviewScoreWarningService.ScoreWarningRule warningRule = scoreWarningService.resolve(bo.getActivityId(), bo.getCategoryId());
        List<Long> assignmentIds = activeAssignments.stream().map(ReviewAssignment::getId).toList();
        Map<Long, List<ReviewScore>> scoreMap = assignmentIds.isEmpty()
            ? Map.of()
            : scoreMapper.selectList(Wrappers.lambdaQuery(ReviewScore.class)
                .in(ReviewScore::getAssignmentId, assignmentIds)
                .eq(ReviewScore::getStatus, ArtReviewConstants.REVIEW_SCORE_SUBMITTED))
            .stream()
            .collect(Collectors.groupingBy(ReviewScore::getProjectId));

        List<ReviewResult> generated = new ArrayList<>();
        for (Project project : projects) {
            ReviewResult result = getOrCreateResult(project);
            applyAggregate(result, scoreMap.getOrDefault(project.getId(), List.of()), aggregateRule, warningRule, project);
            result.setRemark(StringUtils.trim(bo.getRemark()));
            if (result.getId() == null) {
                resultMapper.insert(result);
            } else {
                resultMapper.updateById(result);
            }
            generated.add(result);
        }
        refreshRanks(bo.getActivityId(), bo.getCategoryId(), generated);
        applyAwardRules(bo.getActivityId(), bo.getCategoryId());
    }

    private void lockGenerationScope(ReviewResultBo bo) {
        requireScope(bo);
        categoryMapper.selectOne(Wrappers.lambdaQuery(ActivityCategory.class)
            .eq(ActivityCategory::getId, bo.getCategoryId())
            .eq(ActivityCategory::getActivityId, bo.getActivityId())
            .last("for update"));
        signedSheetMapper.selectList(Wrappers.lambdaQuery(ReviewScoreSheetSignedSheet.class)
            .eq(ReviewScoreSheetSignedSheet::getActivityId, bo.getActivityId())
            .eq(ReviewScoreSheetSignedSheet::getCategoryId, bo.getCategoryId())
            .eq(ReviewScoreSheetSignedSheet::getStatus, SIGNED_SHEET_ACTIVE)
            .eq(ReviewScoreSheetSignedSheet::getActiveMarker, ACTIVE_MARKER)
            .last("for update"));
    }

    public ReviewResultReadinessVo checkReadiness(ReviewResultBo bo) {
        requireScope(bo);
        ReviewResultReadinessVo readiness = new ReviewResultReadinessVo();
        List<Project> projects = projectMapper.selectList(Wrappers.lambdaQuery(Project.class)
            .eq(Project::getActivityId, bo.getActivityId())
            .eq(Project::getCategoryId, bo.getCategoryId())
            .eq(Project::getStatus, ArtReviewConstants.PROJECT_AUDIT_PASSED)
            .orderByAsc(Project::getProjectNo)
            .orderByAsc(Project::getId));
        List<ReviewAssignment> activeAssignments = assignmentMapper.selectList(Wrappers.lambdaQuery(ReviewAssignment.class)
            .eq(ReviewAssignment::getActivityId, bo.getActivityId())
            .eq(ReviewAssignment::getCategoryId, bo.getCategoryId())
            .eq(ReviewAssignment::getStatus, ArtReviewConstants.REVIEW_ASSIGNMENT_ACTIVE));
        readiness.setProjectCount(projects.size());
        readiness.setActiveAssignmentCount(activeAssignments.size());
        if (projects.isEmpty()) {
            readiness.getBlockers().add("当前活动类别没有审核通过的作品");
        }
        if (activeAssignments.isEmpty()) {
            readiness.getBlockers().add("当前活动类别没有有效评审分配");
        }
        long publishedResultCount = resultMapper.selectCount(Wrappers.lambdaQuery(ReviewResult.class)
            .eq(ReviewResult::getActivityId, bo.getActivityId())
            .eq(ReviewResult::getCategoryId, bo.getCategoryId())
            .eq(ReviewResult::getResultStatus, ArtReviewConstants.RESULT_PUBLISHED));
        if (publishedResultCount > 0) {
            readiness.getBlockers().add("当前活动类别结果已经发布，不能重新生成或覆盖");
        }

        Set<String> exclusiveModes = activeAssignments.stream()
            .map(ReviewAssignment::getExclusiveMode)
            .map(mode -> StringUtils.blankToDefault(mode, ArtReviewConstants.REVIEW_EXCLUSIVE_SINGLE))
            .collect(Collectors.toSet());
        if (exclusiveModes.size() > 1) {
            readiness.getBlockers().add("当前类别混用了单评委和多评委模式，请先统一评审分配规则");
        }
        boolean singleReviewer = exclusiveModes.contains(ArtReviewConstants.REVIEW_EXCLUSIVE_SINGLE);
        ScoreAggregateRule aggregateRule = resolveAggregateRule(activeAssignments);
        if (singleReviewer && aggregateRule.minReviewers() > 1) {
            readiness.getBlockers().add("单评委模式不能要求至少 " + aggregateRule.minReviewers() + " 位评委，请先修正评分规则");
        }
        int requiredReviewerCount = singleReviewer ? 1 : Math.max(1, aggregateRule.minReviewers());
        readiness.setRequiredReviewerCount(activeAssignments.isEmpty() ? 0 : requiredReviewerCount);
        readiness.setExpectedScoreCount((long) projects.size() * readiness.getRequiredReviewerCount());

        if (projects.isEmpty() || activeAssignments.isEmpty()) {
            readiness.setReady(false);
            return readiness;
        }

        Set<Long> projectIds = projects.stream().map(Project::getId).collect(Collectors.toSet());
        List<Long> assignmentIds = activeAssignments.stream().map(ReviewAssignment::getId).filter(Objects::nonNull).toList();
        List<ReviewScore> submittedScores = scoreMapper.selectList(Wrappers.lambdaQuery(ReviewScore.class)
            .in(ReviewScore::getAssignmentId, assignmentIds)
            .in(ReviewScore::getProjectId, projectIds)
            .eq(ReviewScore::getStatus, ArtReviewConstants.REVIEW_SCORE_SUBMITTED));
        readiness.setSubmittedScoreCount(submittedScores.size());
        Map<Long, ReviewScore> submittedScoreById = submittedScores.stream()
            .filter(score -> score.getId() != null)
            .collect(Collectors.toMap(ReviewScore::getId, score -> score, (left, right) -> left));

        List<ReviewScoreSheetSignedSheet> categorySheets = signedSheetMapper.selectList(
            Wrappers.lambdaQuery(ReviewScoreSheetSignedSheet.class)
                .eq(ReviewScoreSheetSignedSheet::getActivityId, bo.getActivityId())
                .eq(ReviewScoreSheetSignedSheet::getCategoryId, bo.getCategoryId()));
        List<ReviewScoreSheetSignedSheet> activeSheets = categorySheets.stream()
            .filter(sheet -> SIGNED_SHEET_ACTIVE.equalsIgnoreCase(sheet.getStatus()))
            .filter(sheet -> Objects.equals(sheet.getActiveMarker(), ACTIVE_MARKER))
            .toList();
        readiness.setActiveSignedSheetCount(activeSheets.size());
        Set<Long> activeSheetIds = activeSheets.stream().map(ReviewScoreSheetSignedSheet::getId)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> activeSheetReviewerIds = activeSheets.stream().map(ReviewScoreSheetSignedSheet::getReviewerUserId)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> expectedReviewerIds = activeAssignments.stream().map(ReviewAssignment::getReviewerUserId)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        long missingReviewerSheets = expectedReviewerIds.stream().filter(id -> !activeSheetReviewerIds.contains(id)).count();
        if (missingReviewerSheets > 0) {
            readiness.getBlockers().add(missingReviewerSheets + " 位评委尚未提交当前类别评分表");
        }

        List<ReviewScoreSheetSignedSheetItem> activeItems = activeSheetIds.isEmpty()
            ? List.of()
            : signedSheetItemMapper.selectList(Wrappers.lambdaQuery(ReviewScoreSheetSignedSheetItem.class)
                .in(ReviewScoreSheetSignedSheetItem::getSignedSheetId, activeSheetIds)
                .in(ReviewScoreSheetSignedSheetItem::getProjectId, projectIds)
                .eq(ReviewScoreSheetSignedSheetItem::getActiveMarker, ACTIVE_MARKER));
        Set<Long> validSignedScoreIds = activeItems.stream()
            .filter(item -> {
                ReviewScore score = submittedScoreById.get(item.getReviewScoreId());
                return score != null
                    && Objects.equals(score.getProjectId(), item.getProjectId())
                    && activeSheetIds.contains(item.getSignedSheetId());
            })
            .map(ReviewScoreSheetSignedSheetItem::getReviewScoreId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        readiness.setSignedScoreCount(validSignedScoreIds.size());
        long unsignedSubmittedCount = submittedScores.stream()
            .map(ReviewScore::getId)
            .filter(Objects::nonNull)
            .filter(id -> !validSignedScoreIds.contains(id))
            .count();
        if (unsignedSubmittedCount > 0) {
            readiness.getBlockers().add(unsignedSubmittedCount + " 条已提交评分未进入有效类别评分表");
        }
        long inconsistentSignedItemCount = activeItems.stream()
            .filter(item -> !validSignedScoreIds.contains(item.getReviewScoreId()))
            .count();
        if (inconsistentSignedItemCount > 0) {
            readiness.getBlockers().add(inconsistentSignedItemCount + " 条有效签名表明细与当前评分状态不一致");
        }

        Map<Long, List<ReviewScore>> projectScoreMap = submittedScores.stream()
            .collect(Collectors.groupingBy(ReviewScore::getProjectId));
        List<Project> insufficientProjects = new ArrayList<>();
        List<Project> excessiveProjects = new ArrayList<>();
        List<Project> singleModeConflictProjects = new ArrayList<>();
        List<Project> warningProjects = new ArrayList<>();
        ReviewScoreWarningService.ScoreWarningRule warningRule = scoreWarningService.resolve(bo.getActivityId(), bo.getCategoryId());
        for (Project project : projects) {
            List<ReviewScore> scores = projectScoreMap.getOrDefault(project.getId(), List.of());
            if (scores.size() < requiredReviewerCount) {
                insufficientProjects.add(project);
            }
            if (aggregateRule.maxReviewers() > 0 && scores.size() > aggregateRule.maxReviewers()) {
                excessiveProjects.add(project);
            }
            if (singleReviewer && scores.size() > 1) {
                singleModeConflictProjects.add(project);
            }
            List<BigDecimal> numericValues = scores.stream()
                .map(score -> score.getScoreValue() != null ? score.getScoreValue() : gradePoint(score.getGradeValue()))
                .filter(Objects::nonNull)
                .toList();
            ReviewScoreWarningService.ScoreWarningEvaluation warning = scoreWarningService.evaluate(
                numericValues, scores.size(), warningRule);
            if (WARNING_STATUS_WARNING.equals(warning.status())) {
                warningProjects.add(project);
            }
        }
        if (!insufficientProjects.isEmpty()) {
            readiness.getBlockers().add(insufficientProjects.size() + " 件作品评分人数不足（如：" + sampleProjectNames(insufficientProjects) + "）");
        }
        if (!excessiveProjects.isEmpty()) {
            readiness.getBlockers().add(excessiveProjects.size() + " 件作品评分人数超过上限（如：" + sampleProjectNames(excessiveProjects) + "）");
        }
        if (!singleModeConflictProjects.isEmpty()) {
            readiness.getBlockers().add(singleModeConflictProjects.size() + " 件作品在单评委模式下存在多条已提交评分（如："
                + sampleProjectNames(singleModeConflictProjects) + "）");
        }
        if (!warningProjects.isEmpty()) {
            readiness.getWarnings().add(warningProjects.size() + " 件作品命中评分差异预警（如：" + sampleProjectNames(warningProjects) + "）");
        }

        long resubmittedSheetCount = categorySheets.stream()
            .filter(sheet -> SIGNED_SHEET_WITHDRAWN.equalsIgnoreCase(sheet.getStatus())
                || !Objects.equals(sheet.getActiveMarker(), ACTIVE_MARKER))
            .filter(sheet -> sheet.getReviewerUserId() != null && activeSheetReviewerIds.contains(sheet.getReviewerUserId()))
            .count();
        if (resubmittedSheetCount > 0) {
            readiness.getWarnings().add("存在 " + resubmittedSheetCount + " 张已撤回评分表，相关评委已重新提交，请核对撤回审计");
        }
        readiness.setReady(readiness.getBlockers().isEmpty());
        return readiness;
    }

    private String sampleProjectNames(List<Project> projects) {
        String names = projects.stream()
            .limit(5)
            .map(project -> StringUtils.blankToDefault(project.getProjectName(), project.getProjectNo()))
            .map(name -> StringUtils.blankToDefault(name, "未命名作品"))
            .collect(Collectors.joining("、"));
        return projects.size() > 5 ? names + "等" : names;
    }

    private void requireScope(ReviewResultBo bo) {
        if (bo.getActivityId() == null || bo.getCategoryId() == null) {
            throw new ServiceException("请选择活动和类别");
        }
        Activity activity = activityMapper.selectById(bo.getActivityId());
        ActivityCategory category = categoryMapper.selectById(bo.getCategoryId());
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
        if (category == null || !Objects.equals(category.getActivityId(), bo.getActivityId())) {
            throw new ServiceException("类别不属于当前活动");
        }
    }

    private ReviewResult getOrCreateResult(Project project) {
        ReviewResult result = resultMapper.selectOne(Wrappers.lambdaQuery(ReviewResult.class)
            .eq(ReviewResult::getActivityId, project.getActivityId())
            .eq(ReviewResult::getCategoryId, project.getCategoryId())
            .eq(ReviewResult::getProjectId, project.getId())
            .last("limit 1"));
        if (result == null) {
            result = new ReviewResult();
            result.setActivityId(project.getActivityId());
            result.setCategoryId(project.getCategoryId());
            result.setProjectId(project.getId());
            result.setSchoolId(project.getSchoolId());
            result.setResultStatus(ArtReviewConstants.RESULT_DRAFT);
        }
        return result;
    }

    private void applyAggregate(ReviewResult result, List<ReviewScore> scores, ScoreAggregateRule rule,
                                ReviewScoreWarningService.ScoreWarningRule warningRule, Project project) {
        List<ReviewScore> submitted = scores.stream()
            .filter(score -> ArtReviewConstants.REVIEW_SCORE_SUBMITTED.equals(score.getStatus()))
            .toList();
        List<BigDecimal> numericValues = submitted.stream()
            .map(score -> score.getScoreValue() != null ? score.getScoreValue() : gradePoint(score.getGradeValue()))
            .filter(Objects::nonNull)
            .sorted()
            .toList();
        ReviewScoreWarningService.ScoreWarningEvaluation warning = scoreWarningService.evaluate(
            numericValues, submitted.size(), warningRule);
        result.setScoreCount(submitted.size());
        if (submitted.isEmpty() || (rule.minReviewers() > 0 && submitted.size() < rule.minReviewers())) {
            result.setTotalScore(null);
            result.setAverageScore(null);
            result.setFinalGrade(null);
            result.setScoreSummaryJson(buildScoreSummary(submitted, rule,
                submitted.isEmpty() ? null : "评分人数不足，至少需要 " + rule.minReviewers() + " 位评委", warning));
            return;
        }
        if (rule.maxReviewers() > 0 && submitted.size() > rule.maxReviewers()) {
            throw new ServiceException("项目“" + StringUtils.blankToDefault(project.getProjectName(), project.getProjectNo()) + "”评分人数超过上限 " + rule.maxReviewers() + " 位");
        }

        if (numericValues.isEmpty()) {
            result.setTotalScore(null);
            result.setAverageScore(null);
        } else {
            List<BigDecimal> valuesForAverage = aggregateValues(numericValues, rule);
            BigDecimal total = valuesForAverage.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            result.setTotalScore(total.setScale(2, RoundingMode.HALF_UP));
            result.setAverageScore(total.divide(BigDecimal.valueOf(valuesForAverage.size()), rule.precision(), RoundingMode.HALF_UP));
        }
        result.setFinalGrade(resolveFinalGrade(submitted));
        result.setScoreSummaryJson(buildScoreSummary(submitted, rule, null, warning));
    }

    private BigDecimal gradePoint(String grade) {
        if (StringUtils.isBlank(grade)) {
            return null;
        }
        return DEFAULT_GRADE_POINTS.get(StringUtils.trim(grade).toUpperCase());
    }

    private String resolveFinalGrade(List<ReviewScore> scores) {
        Map<String, Long> countMap = scores.stream()
            .map(ReviewScore::getGradeValue)
            .filter(StringUtils::isNotBlank)
            .map(StringUtils::trim)
            .collect(Collectors.groupingBy(item -> item, LinkedHashMap::new, Collectors.counting()));
        if (countMap.isEmpty()) {
            return null;
        }
        return countMap.entrySet().stream()
            .sorted((left, right) -> {
                int byCount = Long.compare(right.getValue(), left.getValue());
                if (byCount != 0) {
                    return byCount;
                }
                BigDecimal leftPoint = gradePoint(left.getKey());
                BigDecimal rightPoint = gradePoint(right.getKey());
                if (leftPoint == null && rightPoint == null) {
                    return left.getKey().compareTo(right.getKey());
                }
                if (leftPoint == null) {
                    return 1;
                }
                if (rightPoint == null) {
                    return -1;
                }
                return rightPoint.compareTo(leftPoint);
            })
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }

    private List<BigDecimal> aggregateValues(List<BigDecimal> values, ScoreAggregateRule rule) {
        if ("trim_extreme".equals(rule.aggregateMethod()) && values.size() >= 5) {
            return values.subList(1, values.size() - 1);
        }
        return values;
    }

    private ScoreAggregateRule resolveAggregateRule(List<ReviewAssignment> assignments) {
        for (ReviewAssignment assignment : assignments) {
            Map<String, Object> rule = parseObject(assignment.getScoreRuleJson());
            if (!rule.isEmpty()) {
                return new ScoreAggregateRule(
                    intRule(rule, "minReviewers", 0),
                    intRule(rule, "maxReviewers", 0),
                    StringUtils.blankToDefault(stringRule(rule, "aggregateMethod"), "average"),
                    Math.max(0, intRule(rule, "precision", 2))
                );
            }
        }
        return new ScoreAggregateRule(0, 0, "average", 2);
    }

    private int intRule(Map<String, Object> rule, String key, int defaultValue) {
        Object value = rule.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String stringRule(Map<String, Object> rule, String key) {
        Object value = rule.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String buildScoreSummary(List<ReviewScore> scores, ScoreAggregateRule rule, String invalidReason,
                                     ReviewScoreWarningService.ScoreWarningEvaluation warning) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("minReviewers", rule.minReviewers());
        summary.put("maxReviewers", rule.maxReviewers());
        summary.put("aggregateMethod", rule.aggregateMethod());
        summary.put("precision", rule.precision());
        summary.put("invalidReason", invalidReason);
        summary.put("warning", warning == null ? null : warning.snapshot());
        List<Map<String, Object>> items = scores.stream().map(score -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("reviewerUserId", score.getReviewerUserId());
            item.put("scoreValue", score.getScoreValue());
            item.put("gradeValue", score.getGradeValue());
            item.put("commentText", score.getCommentText());
            item.put("submittedAt", score.getSubmittedAt());
            return item;
        }).toList();
        summary.put("scores", items);
        return JsonUtils.toJsonString(summary);
    }

    private void refreshRanks(Long activityId, Long categoryId, List<ReviewResult> generated) {
        List<ReviewResult> ranked = generated.stream()
            .filter(result -> result.getAverageScore() != null)
            .sorted(Comparator.comparing(ReviewResult::getAverageScore).reversed()
                .thenComparing(ReviewResult::getId, Comparator.nullsLast(Long::compareTo)))
            .toList();
        BigDecimal previousScore = null;
        int previousRank = 0;
        for (int i = 0; i < ranked.size(); i++) {
            ReviewResult result = ranked.get(i);
            int rank = previousScore != null && previousScore.compareTo(result.getAverageScore()) == 0 ? previousRank : i + 1;
            result.setRankNo(rank);
            resultMapper.updateById(result);
            previousScore = result.getAverageScore();
            previousRank = rank;
        }
        resultMapper.selectList(Wrappers.lambdaQuery(ReviewResult.class)
                .eq(ReviewResult::getActivityId, activityId)
                .eq(ReviewResult::getCategoryId, categoryId)
                .isNull(ReviewResult::getAverageScore))
            .forEach(result -> {
                result.setRankNo(null);
                resultMapper.updateById(result);
            });
    }

    private void applyAwardRules(Long activityId, Long categoryId) {
        List<ReviewAwardRule> rules = awardRuleMapper.selectList(Wrappers.lambdaQuery(ReviewAwardRule.class)
            .eq(ReviewAwardRule::getActivityId, activityId)
            .eq(ReviewAwardRule::getCategoryId, categoryId)
            .eq(ReviewAwardRule::getEnabled, true)
            .orderByAsc(ReviewAwardRule::getSortOrder)
            .orderByAsc(ReviewAwardRule::getId));
        List<ReviewResult> results = resultMapper.selectList(Wrappers.lambdaQuery(ReviewResult.class)
            .eq(ReviewResult::getActivityId, activityId)
            .eq(ReviewResult::getCategoryId, categoryId));
        for (ReviewResult result : results) {
            ReviewAwardRule matched = matchAwardRule(result, rules);
            result.setAwardLevel(matched == null ? null : matched.getAwardLevel());
            result.setAwardRemark(matched == null ? null : matched.getRemark());
            resultMapper.updateById(result);
        }
    }

    private ReviewAwardRule matchAwardRule(ReviewResult result, List<ReviewAwardRule> rules) {
        for (ReviewAwardRule rule : rules) {
            if (ArtReviewConstants.AWARD_RULE_RANK_RANGE.equals(rule.getRuleType()) && result.getRankNo() != null) {
                boolean minOk = rule.getMinRank() == null || result.getRankNo() >= rule.getMinRank();
                boolean maxOk = rule.getMaxRank() == null || result.getRankNo() <= rule.getMaxRank();
                if (minOk && maxOk) {
                    return rule;
                }
            }
            if (ArtReviewConstants.AWARD_RULE_SCORE_RANGE.equals(rule.getRuleType()) && result.getAverageScore() != null) {
                boolean minOk = rule.getMinScore() == null || result.getAverageScore().compareTo(rule.getMinScore()) >= 0;
                boolean maxOk = rule.getMaxScore() == null || result.getAverageScore().compareTo(rule.getMaxScore()) <= 0;
                if (minOk && maxOk) {
                    return rule;
                }
            }
        }
        return null;
    }

    private Map<String, Object> parseObject(String text) {
        if (StringUtils.isBlank(text) || !JsonUtils.isJsonObject(text)) {
            return Map.of();
        }
        Map<String, Object> parsed = JsonUtils.parseObject(text, new TypeReference<Map<String, Object>>() {
        });
        return parsed == null ? Map.of() : parsed;
    }

    private record ScoreAggregateRule(int minReviewers, int maxReviewers, String aggregateMethod, int precision) {
    }
}
