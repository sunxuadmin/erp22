package org.dromara.crehn.review.sheet;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.ReviewScoreSheetTemplate;
import org.dromara.crehn.domain.bo.ReviewScoreSheetExportBo;
import org.dromara.crehn.domain.bo.ReviewScoreSheetTemplateBo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetColumnVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetExportPreviewVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetFooterFieldVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetFooterRowVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetTemplateVo;
import org.dromara.crehn.domain.vo.ReviewTaskVo;
import org.dromara.crehn.mapper.ReviewScoreSheetTemplateMapper;
import org.dromara.crehn.review.service.IArtReviewService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysUserMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewScoreSheetService {

    private static final String DEFAULT_TEMPLATE_KEY = "review_score_sheet_default";
    private static final String DEFAULT_TEMPLATE_NAME = "默认评审打分表";
    private static final String DEFAULT_TITLE = "评审打分表";
    private static final String DEFAULT_FOOTER_TIME = "时间：";
    private static final String DEFAULT_FOOTER_SIGNATURE = "评分老师签字：";
    private static final String DEFAULT_SIGNATURE_BUTTON_TEXT = "点击签名";
    private static final String DEFAULT_SIGNATURE_HINT_TEXT = "或拖入默认签名";
    private static final String DEFAULT_SIGNATURE_TIME_TEXT = "签名时间：";
    static final String DEFAULT_REVIEWER_SIGNATURE_SLOT = "reviewer_signature";
    private static final String FLAG_TRUE = "1";
    private static final String FLAG_FALSE = "0";
    private static final int MAX_TEMPLATE_COUNT = 20;
    private static final int MAX_FOOTER_ROWS = 6;
    private static final DateTimeFormatter EXPORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReviewScoreSheetTemplateMapper templateMapper;
    private final SysUserMapper sysUserMapper;
    private final IArtReviewService reviewService;
    private final ReviewScoreSheetExcelWriter excelWriter;

    public List<ReviewScoreSheetTemplateVo> listTemplates() {
        List<ReviewScoreSheetTemplate> templates = queryTemplates();
        if (templates.isEmpty()) {
            return List.of(defaultTemplateVo());
        }
        Long defaultId = effectiveDefaultId(templates);
        return templates.stream()
            .map(template -> toVo(template, Objects.equals(defaultId, template.getId())))
            .toList();
    }

    public ReviewScoreSheetTemplateVo getTemplate() {
        ReviewScoreSheetTemplate template = findDefaultTemplate();
        return template == null ? defaultTemplateVo() : toVo(template, true);
    }

    public ReviewScoreSheetTemplateVo getTemplate(Long templateId) {
        return toVo(requireTemplate(templateId), isEffectiveDefault(templateId));
    }

    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreSheetTemplateVo saveTemplate(ReviewScoreSheetTemplateBo bo) {
        if (bo == null) {
            throw new ServiceException("模板内容不能为空");
        }
        if (bo.getId() != null) {
            return updateTemplate(bo.getId(), bo);
        }
        ReviewScoreSheetTemplate existing = findDefaultTemplate();
        if (existing == null) {
            bo.setTemplateName(StringUtils.blankToDefault(bo.getTemplateName(), DEFAULT_TEMPLATE_NAME));
            bo.setDefaultTemplate(true);
            return createTemplate(bo);
        }
        bo.setTemplateName(StringUtils.blankToDefault(bo.getTemplateName(), templateName(existing)));
        bo.setDefaultTemplate(true);
        return updateTemplate(existing.getId(), bo);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreSheetTemplateVo createTemplate(ReviewScoreSheetTemplateBo bo) {
        long count = templateMapper.selectCount(Wrappers.lambdaQuery(ReviewScoreSheetTemplate.class));
        if (count >= MAX_TEMPLATE_COUNT) {
            throw new ServiceException("评分表模板最多保存" + MAX_TEMPLATE_COUNT + "个");
        }
        ReviewScoreSheetTemplate normalized = normalize(bo, true);
        checkTemplateNameUnique(normalized.getTemplateName(), null);
        boolean makeDefault = count == 0 || Boolean.TRUE.equals(bo.getDefaultTemplate());
        if (makeDefault) {
            clearDefaultFlags(null);
        }
        normalized.setTemplateKey("review_score_sheet_" + UUID.randomUUID().toString().replace("-", ""));
        normalized.setDefaultFlag(makeDefault ? FLAG_TRUE : FLAG_FALSE);
        normalized.setVersion(1L);
        normalized.setDelFlag("0");
        try {
            templateMapper.insert(normalized);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("模板名称已存在，请更换名称");
        }
        return toVo(requireTemplate(normalized.getId()), makeDefault);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreSheetTemplateVo updateTemplate(Long templateId, ReviewScoreSheetTemplateBo bo) {
        ReviewScoreSheetTemplate existing = requireTemplate(templateId);
        if (bo == null || bo.getVersion() == null || !bo.getVersion().equals(existing.getVersion())) {
            throw conflictException();
        }
        ReviewScoreSheetTemplate normalized = normalize(bo, true);
        checkTemplateNameUnique(normalized.getTemplateName(), templateId);
        boolean makeDefault = FLAG_TRUE.equals(existing.getDefaultFlag()) || Boolean.TRUE.equals(bo.getDefaultTemplate());
        if (Boolean.TRUE.equals(bo.getDefaultTemplate())) {
            clearDefaultFlags(templateId);
        }
        normalized.setId(existing.getId());
        normalized.setTemplateKey(existing.getTemplateKey());
        normalized.setDefaultFlag(makeDefault ? FLAG_TRUE : FLAG_FALSE);
        normalized.setDelFlag(existing.getDelFlag());
        normalized.setVersion(existing.getVersion() + 1);
        int rows = templateMapper.update(normalized, Wrappers.<ReviewScoreSheetTemplate>lambdaUpdate()
            .eq(ReviewScoreSheetTemplate::getId, existing.getId())
            .eq(ReviewScoreSheetTemplate::getVersion, existing.getVersion()));
        if (rows != 1) {
            throw conflictException();
        }
        return toVo(requireTemplate(templateId), makeDefault);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreSheetTemplateVo setDefaultTemplate(Long templateId) {
        ReviewScoreSheetTemplate template = requireTemplate(templateId);
        clearDefaultFlags(templateId);
        templateMapper.update(null, Wrappers.<ReviewScoreSheetTemplate>lambdaUpdate()
            .set(ReviewScoreSheetTemplate::getDefaultFlag, FLAG_TRUE)
            .eq(ReviewScoreSheetTemplate::getId, templateId));
        return toVo(requireTemplate(templateId), true);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTemplate(Long templateId) {
        ReviewScoreSheetTemplate template = requireTemplate(templateId);
        if (templateMapper.selectCount(Wrappers.lambdaQuery(ReviewScoreSheetTemplate.class)) <= 1) {
            throw new ServiceException("至少需要保留一个评分表模板");
        }
        if (isEffectiveDefault(template.getId())) {
            throw new ServiceException("默认模板不能删除，请先将其他模板设为默认");
        }
        templateMapper.deleteById(templateId);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreSheetTemplateVo resetTemplate(Long version) {
        ReviewScoreSheetTemplate template = findDefaultTemplate();
        if (template == null) {
            ReviewScoreSheetTemplateBo createBo = defaultTemplateBo(DEFAULT_TEMPLATE_NAME);
            createBo.setVersion(0L);
            createBo.setDefaultTemplate(true);
            return createTemplate(createBo);
        }
        return resetTemplate(template.getId(), version);
    }

    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreSheetTemplateVo resetTemplate(Long templateId, Long version) {
        ReviewScoreSheetTemplate existing = requireTemplate(templateId);
        ReviewScoreSheetTemplateBo bo = new ReviewScoreSheetTemplateBo();
        bo.setId(templateId);
        bo.setTemplateName(templateName(existing));
        bo.setTitle(DEFAULT_TITLE);
        bo.setColumns(defaultColumns());
        bo.setFooterRows(defaultFooterRows());
        bo.setDefaultTemplate(isEffectiveDefault(templateId));
        bo.setVersion(version);
        return updateTemplate(templateId, bo);
    }

    public void exportSubmittedScores(Long activityId, Long categoryId, HttpServletResponse response) {
        ReviewScoreSheetExportBo bo = new ReviewScoreSheetExportBo();
        bo.setActivityId(activityId);
        bo.setCategoryId(categoryId);
        exportSubmittedScores(bo, response);
    }

    public ReviewScoreSheetExportPreviewVo previewSubmittedScores(ReviewScoreSheetExportBo bo) {
        List<ReviewTaskVo> tasks = submittedTasks(bo);
        ReviewScoreSheetExportPreviewVo preview = new ReviewScoreSheetExportPreviewVo();
        preview.setTemplate(resolveExportTemplate(bo));
        preview.setRows(tasks);
        preview.setTotal((long) tasks.size());
        preview.setReviewerName(currentReviewerName());
        preview.setCategoryName(tasks.get(0).getCategoryName());
        preview.setExportTime(normalizeExportTime(bo.getExportTime()));
        return preview;
    }

    public void exportSubmittedScores(ReviewScoreSheetExportBo bo, HttpServletResponse response) {
        List<ReviewTaskVo> tasks = submittedTasks(bo);
        ReviewScoreSheetTemplateVo template = resolveExportTemplate(bo);
        String exportTime = normalizeExportTime(bo.getExportTime());
        String categoryName = tasks.get(0).getCategoryName();
        String reviewerName = currentReviewerName();
        String filename = safeFilename(categoryName) + "-评审打分表.xlsx";
        FileUtils.setAttachmentResponseHeader(response, filename);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        try {
            excelWriter.write(tasks, template, reviewerName, categoryName, exportTime, response.getOutputStream());
        } catch (IOException ex) {
            throw new ServiceException("导出评审打分表失败");
        }
    }

    /**
     * Resolves a template exactly as an export would, without persisting it.
     * The signed-sheet workflow uses this so a per-export layout is snapshotted
     * with the same validation and legacy compatibility as the ordinary export.
     */
    public ReviewScoreSheetTemplateVo resolveTemplateForExport(ReviewScoreSheetExportBo bo) {
        return resolveExportTemplate(bo);
    }

    public String currentReviewerDisplayName() {
        return currentReviewerName();
    }

    private List<ReviewTaskVo> submittedTasks(ReviewScoreSheetExportBo bo) {
        if (bo == null || bo.getActivityId() == null || bo.getCategoryId() == null) {
            throw new ServiceException("请先选择具体活动和类别");
        }
        List<ReviewTaskVo> tasks = reviewService.queryMySubmittedTasksForExport(bo.getActivityId(), bo.getCategoryId());
        if (tasks.isEmpty()) {
            throw new ServiceException("该类别暂无已提交评分");
        }
        return tasks;
    }

    private ReviewScoreSheetTemplateVo resolveExportTemplate(ReviewScoreSheetExportBo bo) {
        if (bo != null && bo.getTemplate() != null) {
            ReviewScoreSheetTemplate normalized = normalize(bo.getTemplate(), false);
            ReviewScoreSheetTemplateVo vo = toVo(normalized, Boolean.TRUE.equals(bo.getTemplate().getDefaultTemplate()));
            vo.setId(bo.getTemplate().getId());
            vo.setVersion(bo.getTemplate().getVersion());
            return vo;
        }
        if (bo != null && bo.getTemplateId() != null) {
            return getTemplate(bo.getTemplateId());
        }
        return getTemplate();
    }

    private ReviewScoreSheetTemplate normalize(ReviewScoreSheetTemplateBo bo, boolean requireTemplateName) {
        if (bo == null) {
            throw new ServiceException("模板内容不能为空");
        }
        String templateName = trimToNull(bo.getTemplateName());
        if (requireTemplateName && templateName == null) {
            throw new ServiceException("模板名称不能为空");
        }
        if (templateName != null && templateName.length() > 100) {
            throw new ServiceException("模板名称不能超过100个字符");
        }
        String title = trimToNull(bo.getTitle());
        if (title == null) {
            throw new ServiceException("表格标题不能为空");
        }
        if (title.length() > 100) {
            throw new ServiceException("表格标题不能超过100个字符");
        }
        List<ReviewScoreSheetColumnVo> columns = normalizeColumns(bo.getColumns());
        List<ReviewScoreSheetFooterRowVo> footerRows = bo.getFooterRows() == null
            ? legacyFooterRows(bo.getFooterTimeText(), bo.getFooterSignatureText())
            : normalizeFooterRows(bo.getFooterRows());
        ReviewScoreSheetTemplate template = new ReviewScoreSheetTemplate();
        template.setTemplateName(templateName);
        template.setTitle(title);
        template.setColumnConfigJson(JsonUtils.toJsonString(columns));
        template.setFooterConfigJson(JsonUtils.toJsonString(footerRows));
        ReviewScoreSheetFooterRowVo firstRow = footerRows.isEmpty() ? null : footerRows.get(0);
        template.setFooterTimeText(firstRow == null || firstRow.getLeft() == null ? "" : firstRow.getLeft().getLabel());
        template.setFooterSignatureText(firstRow == null || firstRow.getRight() == null ? "" : firstRow.getRight().getLabel());
        return template;
    }

    private List<ReviewScoreSheetColumnVo> normalizeColumns(List<ReviewScoreSheetColumnVo> columns) {
        Map<String, String> allowed = allowedColumns();
        if (columns == null || columns.size() != allowed.size()) {
            throw new ServiceException("模板列配置不完整，请刷新后重试");
        }
        Set<String> seen = new HashSet<>();
        List<ReviewScoreSheetColumnVo> normalized = new ArrayList<>();
        int visibleCount = 0;
        for (ReviewScoreSheetColumnVo column : columns) {
            String key = column == null ? null : trimToNull(column.getKey());
            if (key == null || !allowed.containsKey(key) || !seen.add(key)) {
                throw new ServiceException("模板列配置包含无效或重复字段");
            }
            String label = trimToNull(column.getLabel());
            if (label == null || label.length() > 30) {
                throw new ServiceException("列名称不能为空且不能超过30个字符");
            }
            boolean visible = Boolean.TRUE.equals(column.getVisible());
            if (visible) {
                visibleCount++;
            }
            normalized.add(new ReviewScoreSheetColumnVo(key, label, visible));
        }
        if (!seen.equals(allowed.keySet())) {
            throw new ServiceException("模板列配置不完整，请刷新后重试");
        }
        if (visibleCount < 2) {
            throw new ServiceException("至少需要显示两列");
        }
        return normalized;
    }

    private List<ReviewScoreSheetFooterRowVo> normalizeFooterRows(List<ReviewScoreSheetFooterRowVo> rows) {
        if (rows.size() > MAX_FOOTER_ROWS) {
            throw new ServiceException("底部签字区最多配置" + MAX_FOOTER_ROWS + "行");
        }
        List<ReviewScoreSheetFooterRowVo> normalized = new ArrayList<>();
        for (int index = 0; index < rows.size(); index++) {
            ReviewScoreSheetFooterRowVo row = rows.get(index);
            if (row == null) {
                throw new ServiceException("底部签字区第" + (index + 1) + "行不能为空");
            }
            ReviewScoreSheetFooterFieldVo left = normalizeFooterField(row.getLeft());
            ReviewScoreSheetFooterFieldVo right = normalizeFooterField(row.getRight());
            if (left == null && right == null) {
                throw new ServiceException("底部签字区第" + (index + 1) + "行至少配置一个字段");
            }
            normalized.add(new ReviewScoreSheetFooterRowVo(left, right));
        }
        List<ReviewScoreSheetFooterRowVo> upgraded = upgradeLegacyReviewerSignatureSlot(normalized);
        long signatureCount = upgraded.stream()
            .flatMap(row -> java.util.stream.Stream.of(row == null ? null : row.getLeft(), row == null ? null : row.getRight()))
            .filter(this::isSignatureField)
            .count();
        if (signatureCount > 1) {
            throw new ServiceException("评分表最多配置一个评分老师签名槽");
        }
        return upgraded;
    }

    private ReviewScoreSheetFooterFieldVo normalizeFooterField(ReviewScoreSheetFooterFieldVo field) {
        if (field == null) {
            return null;
        }
        String label = trimToNull(field.getLabel());
        if (label == null) {
            return null;
        }
        if (label.length() > 30) {
            throw new ServiceException("底部字段名称不能超过30个字符");
        }
        String type = StringUtils.blankToDefault(field.getType(), "text");
        if (!Set.of("text", "exportTime", "signature").contains(type)) {
            throw new ServiceException("底部字段类型无效");
        }
        String lineLength = "exportTime".equals(type)
            ? "none"
            : StringUtils.blankToDefault(field.getLineLength(), "none");
        if (!Set.of("none", "short", "medium", "long").contains(lineLength)) {
            throw new ServiceException("签字横线长度无效");
        }
        String slotKey = trimToNull(field.getSlotKey());
        String signatureButtonText = null;
        String signatureHintText = null;
        String signatureTimeText = null;
        Boolean signatureRequired = null;
        Boolean signatureTimeVisible = null;
        if ("signature".equals(type)) {
            if (label.contains("复核")) {
                throw new ServiceException("当前仅支持评分老师签名，不支持复核老师签名槽");
            }
            if (slotKey == null) {
                slotKey = DEFAULT_REVIEWER_SIGNATURE_SLOT;
            }
            if (!slotKey.matches("[a-z][a-z0-9_]{0,63}")) {
                throw new ServiceException("签字槽位标识无效");
            }
            signatureButtonText = normalizeSignatureCopy(field.getSignatureButtonText(), DEFAULT_SIGNATURE_BUTTON_TEXT, 20, "签名按钮文字");
            signatureHintText = normalizeSignatureCopy(field.getSignatureHintText(), DEFAULT_SIGNATURE_HINT_TEXT, 30, "签名提示文字");
            signatureTimeText = normalizeSignatureCopy(field.getSignatureTimeText(), DEFAULT_SIGNATURE_TIME_TEXT, 20, "签名时间标题");
            // Older footer JSON does not include either switch. Preserve the
            // historical signed-sheet behavior by treating both as enabled.
            signatureRequired = !Boolean.FALSE.equals(field.getSignatureRequired());
            signatureTimeVisible = !Boolean.FALSE.equals(field.getSignatureTimeVisible());
        } else {
            slotKey = null;
        }
        return new ReviewScoreSheetFooterFieldVo(label, type, lineLength, slotKey, signatureButtonText, signatureHintText,
            signatureTimeText, signatureRequired, signatureTimeVisible);
    }

    private String normalizeSignatureCopy(String value, String defaultValue, int maxLength, String fieldName) {
        if (value == null) {
            return defaultValue;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new ServiceException(fieldName + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }

    /**
     * Older templates used a plain text field such as “评分老师签字：”,
     * “评审老师签名：”, or “评委老师签字：”. Upgrade the first matching
     * field in memory so old saved templates get one stable slot without
     * forcing a data migration or changing unrelated footer text.
     */
    private List<ReviewScoreSheetFooterRowVo> upgradeLegacyReviewerSignatureSlot(List<ReviewScoreSheetFooterRowVo> rows) {
        boolean configured = rows.stream().anyMatch(row -> isSignatureField(row == null ? null : row.getLeft())
            || isSignatureField(row == null ? null : row.getRight()));
        if (configured) {
            return rows;
        }
        for (ReviewScoreSheetFooterRowVo row : rows) {
            if (row == null) {
                continue;
            }
            if (isLegacyReviewerSignatureLabel(row.getLeft())) {
                row.setLeft(asReviewerSignatureField(row.getLeft()));
                return rows;
            }
            if (isLegacyReviewerSignatureLabel(row.getRight())) {
                row.setRight(asReviewerSignatureField(row.getRight()));
                return rows;
            }
        }
        return rows;
    }

    private boolean isSignatureField(ReviewScoreSheetFooterFieldVo field) {
        return field != null && "signature".equals(field.getType());
    }

    private boolean isLegacyReviewerSignatureLabel(ReviewScoreSheetFooterFieldVo field) {
        if (field == null || StringUtils.isBlank(field.getLabel())) {
            return false;
        }
        String label = field.getLabel();
        return (label.contains("评分老师") || label.contains("评审老师") || label.contains("评委老师"))
            && (label.contains("签字") || label.contains("签名"));
    }

    private ReviewScoreSheetFooterFieldVo asReviewerSignatureField(ReviewScoreSheetFooterFieldVo field) {
        return new ReviewScoreSheetFooterFieldVo(field.getLabel(), "signature",
            StringUtils.blankToDefault(field.getLineLength(), "long"), DEFAULT_REVIEWER_SIGNATURE_SLOT,
            DEFAULT_SIGNATURE_BUTTON_TEXT, DEFAULT_SIGNATURE_HINT_TEXT, DEFAULT_SIGNATURE_TIME_TEXT);
    }

    private ReviewScoreSheetTemplateVo toVo(ReviewScoreSheetTemplate template, boolean defaultTemplate) {
        ReviewScoreSheetTemplateVo vo = new ReviewScoreSheetTemplateVo();
        vo.setId(template.getId());
        vo.setTemplateName(templateName(template));
        vo.setTitle(template.getTitle());
        vo.setColumns(parseColumns(template.getColumnConfigJson()));
        vo.setFooterRows(parseFooterRows(template));
        vo.setFooterTimeText(template.getFooterTimeText());
        vo.setFooterSignatureText(template.getFooterSignatureText());
        vo.setDefaultTemplate(defaultTemplate);
        vo.setVersion(template.getVersion());
        vo.setUpdateBy(template.getUpdateBy());
        vo.setUpdateTime(template.getUpdateTime());
        if (template.getUpdateBy() != null) {
            SysUserVo user = sysUserMapper.selectVoById(template.getUpdateBy());
            vo.setUpdatedByName(user == null
                ? String.valueOf(template.getUpdateBy())
                : StringUtils.blankToDefault(user.getNickName(), user.getUserName()));
        }
        return vo;
    }

    private ReviewScoreSheetTemplateVo defaultTemplateVo() {
        ReviewScoreSheetTemplateVo vo = new ReviewScoreSheetTemplateVo();
        vo.setTemplateName(DEFAULT_TEMPLATE_NAME);
        vo.setTitle(DEFAULT_TITLE);
        vo.setColumns(defaultColumns());
        vo.setFooterRows(defaultFooterRows());
        vo.setFooterTimeText(DEFAULT_FOOTER_TIME);
        vo.setFooterSignatureText(DEFAULT_FOOTER_SIGNATURE);
        vo.setDefaultTemplate(true);
        vo.setVersion(0L);
        return vo;
    }

    private ReviewScoreSheetTemplateBo defaultTemplateBo(String templateName) {
        ReviewScoreSheetTemplateBo bo = new ReviewScoreSheetTemplateBo();
        bo.setTemplateName(templateName);
        bo.setTitle(DEFAULT_TITLE);
        bo.setColumns(defaultColumns());
        bo.setFooterRows(defaultFooterRows());
        return bo;
    }

    private List<ReviewScoreSheetColumnVo> parseColumns(String json) {
        try {
            List<ReviewScoreSheetColumnVo> columns = JsonUtils.parseObject(json,
                new TypeReference<List<ReviewScoreSheetColumnVo>>() {
                });
            return normalizeColumns(columns);
        } catch (Exception ex) {
            return defaultColumns();
        }
    }

    private List<ReviewScoreSheetFooterRowVo> parseFooterRows(ReviewScoreSheetTemplate template) {
        if (StringUtils.isBlank(template.getFooterConfigJson())) {
            return legacyFooterRows(template.getFooterTimeText(), template.getFooterSignatureText());
        }
        try {
            List<ReviewScoreSheetFooterRowVo> rows = JsonUtils.parseObject(template.getFooterConfigJson(),
                new TypeReference<List<ReviewScoreSheetFooterRowVo>>() {
                });
            return normalizeFooterRows(rows == null ? List.of() : rows);
        } catch (Exception ex) {
            return legacyFooterRows(template.getFooterTimeText(), template.getFooterSignatureText());
        }
    }

    private List<ReviewScoreSheetColumnVo> defaultColumns() {
        return List.of(
            new ReviewScoreSheetColumnVo("projectNo", "项目编号", true),
            new ReviewScoreSheetColumnVo("projectName", "项目名称", true),
            new ReviewScoreSheetColumnVo("categoryName", "类别", true),
            new ReviewScoreSheetColumnVo("scoreMode", "评分模式", true),
            new ReviewScoreSheetColumnVo("status", "状态", false),
            new ReviewScoreSheetColumnVo("scoreResult", "评审结果", true),
            new ReviewScoreSheetColumnVo("scoreTime", "评分时间", true),
            new ReviewScoreSheetColumnVo("reviewerName", "评审老师", true)
        );
    }

    private Map<String, String> allowedColumns() {
        Map<String, String> columns = new LinkedHashMap<>();
        for (ReviewScoreSheetColumnVo column : defaultColumns()) {
            columns.put(column.getKey(), column.getLabel());
        }
        return columns;
    }

    private List<ReviewScoreSheetFooterRowVo> defaultFooterRows() {
        return List.of(
            new ReviewScoreSheetFooterRowVo(
                new ReviewScoreSheetFooterFieldVo(DEFAULT_FOOTER_TIME, "text", "medium"),
                new ReviewScoreSheetFooterFieldVo(DEFAULT_FOOTER_SIGNATURE, "signature", "long", DEFAULT_REVIEWER_SIGNATURE_SLOT,
                    DEFAULT_SIGNATURE_BUTTON_TEXT, DEFAULT_SIGNATURE_HINT_TEXT, DEFAULT_SIGNATURE_TIME_TEXT)
            )
        );
    }

    private List<ReviewScoreSheetFooterRowVo> legacyFooterRows(String timeText, String signatureText) {
        String time = trimToNull(timeText);
        String signature = trimToNull(signatureText);
        List<ReviewScoreSheetFooterRowVo> rows = new ArrayList<>();
        if (time != null || signature != null) {
            ReviewScoreSheetFooterFieldVo signatureField = signature == null ? null
                : new ReviewScoreSheetFooterFieldVo(signature, "text", "long");
            if (isLegacyReviewerSignatureLabel(signatureField)) {
                signatureField = asReviewerSignatureField(signatureField);
            }
            rows.add(new ReviewScoreSheetFooterRowVo(
                time == null ? null : new ReviewScoreSheetFooterFieldVo(time, "text", "medium"),
                signatureField
            ));
        }
        rows.add(new ReviewScoreSheetFooterRowVo(
            new ReviewScoreSheetFooterFieldVo("导出时间：", "exportTime", "none"),
            null
        ));
        return rows;
    }

    private List<ReviewScoreSheetTemplate> queryTemplates() {
        List<ReviewScoreSheetTemplate> templates = templateMapper.selectList(
            Wrappers.lambdaQuery(ReviewScoreSheetTemplate.class));
        templates.sort(Comparator
            .comparing((ReviewScoreSheetTemplate item) -> !FLAG_TRUE.equals(item.getDefaultFlag()))
            .thenComparing(ReviewScoreSheetTemplate::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(ReviewScoreSheetTemplate::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        return templates;
    }

    private ReviewScoreSheetTemplate findDefaultTemplate() {
        List<ReviewScoreSheetTemplate> templates = queryTemplates();
        if (templates.isEmpty()) {
            return null;
        }
        Long defaultId = effectiveDefaultId(templates);
        return templates.stream()
            .filter(template -> Objects.equals(defaultId, template.getId()))
            .findFirst()
            .orElse(templates.get(0));
    }

    private Long effectiveDefaultId(List<ReviewScoreSheetTemplate> templates) {
        return templates.stream()
            .filter(template -> FLAG_TRUE.equals(template.getDefaultFlag()))
            .map(ReviewScoreSheetTemplate::getId)
            .findFirst()
            .orElseGet(() -> templates.stream()
                .filter(template -> DEFAULT_TEMPLATE_KEY.equals(template.getTemplateKey()))
                .map(ReviewScoreSheetTemplate::getId)
                .findFirst()
                .orElse(templates.get(0).getId()));
    }

    private boolean isEffectiveDefault(Long templateId) {
        List<ReviewScoreSheetTemplate> templates = queryTemplates();
        return !templates.isEmpty() && Objects.equals(effectiveDefaultId(templates), templateId);
    }

    private ReviewScoreSheetTemplate requireTemplate(Long templateId) {
        ReviewScoreSheetTemplate template = templateId == null ? null : templateMapper.selectById(templateId);
        if (template == null) {
            throw new ServiceException("评分表模板不存在或已被删除");
        }
        return template;
    }

    private void checkTemplateNameUnique(String templateName, Long excludeId) {
        long count = templateMapper.selectCount(Wrappers.lambdaQuery(ReviewScoreSheetTemplate.class)
            .eq(ReviewScoreSheetTemplate::getTemplateName, templateName)
            .ne(excludeId != null, ReviewScoreSheetTemplate::getId, excludeId));
        if (count > 0) {
            throw new ServiceException("模板名称已存在，请更换名称");
        }
    }

    private void clearDefaultFlags(Long excludeId) {
        templateMapper.update(null, Wrappers.<ReviewScoreSheetTemplate>lambdaUpdate()
            .set(ReviewScoreSheetTemplate::getDefaultFlag, FLAG_FALSE)
            .eq(ReviewScoreSheetTemplate::getDefaultFlag, FLAG_TRUE)
            .ne(excludeId != null, ReviewScoreSheetTemplate::getId, excludeId));
    }

    private String templateName(ReviewScoreSheetTemplate template) {
        return StringUtils.blankToDefault(template.getTemplateName(), DEFAULT_TEMPLATE_NAME);
    }

    private String normalizeExportTime(String value) {
        if (StringUtils.isBlank(value)) {
            return LocalDateTime.now().format(EXPORT_TIME_FORMATTER);
        }
        try {
            return LocalDateTime.parse(value.trim(), EXPORT_TIME_FORMATTER).format(EXPORT_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new ServiceException("导出时间格式应为 yyyy-MM-dd HH:mm");
        }
    }

    private String currentReviewerName() {
        if (LoginHelper.getLoginUser() == null) {
            return LoginHelper.getUsername();
        }
        return StringUtils.blankToDefault(LoginHelper.getLoginUser().getNickname(), LoginHelper.getUsername());
    }

    private String safeFilename(String value) {
        String name = StringUtils.blankToDefault(value, "当前类别");
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private ServiceException conflictException() {
        return new ServiceException("模板已被其他用户修改，请刷新后重新编辑");
    }
}
