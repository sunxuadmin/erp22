package org.dromara.crehn.review.sheet;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.ReviewScoreSheetSignature;
import org.dromara.crehn.domain.ReviewScoreSheetSignedSheet;
import org.dromara.crehn.domain.ReviewScoreSheetSignedSheetItem;
import org.dromara.crehn.domain.ReviewScore;
import org.dromara.crehn.domain.ReviewResult;
import org.dromara.crehn.domain.bo.ReviewScoreSheetExportBo;
import org.dromara.crehn.domain.bo.ReviewScoreSheetSignatureAdminQueryBo;
import org.dromara.crehn.domain.bo.ReviewScoreSheetSignatureAdminStatusBo;
import org.dromara.crehn.domain.bo.ReviewScoreSheetSignaturePlacementVo;
import org.dromara.crehn.domain.bo.ReviewScoreSheetSignedSheetCreateBo;
import org.dromara.crehn.domain.bo.ReviewScoreSheetSignedSheetQueryBo;
import org.dromara.crehn.domain.bo.ReviewScoreSheetSignedSheetWithdrawBo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetExportPreviewVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetFooterFieldVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetFooterRowVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetReviewerOptionVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetSignatureAdminVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetSignatureVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetSignedSheetVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetTemplateVo;
import org.dromara.crehn.domain.vo.ReviewTaskVo;
import org.dromara.crehn.mapper.ReviewScoreSheetSignatureMapper;
import org.dromara.crehn.mapper.ReviewScoreSheetSignedSheetItemMapper;
import org.dromara.crehn.mapper.ReviewScoreSheetSignedSheetMapper;
import org.dromara.crehn.mapper.ReviewResultMapper;
import org.dromara.crehn.mapper.ReviewScoreMapper;
import org.dromara.crehn.review.service.IArtReviewService;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.vo.SysOssVo;
import org.dromara.system.domain.SysUser;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysOssService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Personal signature library plus immutable signed-sheet snapshots. This class
 * deliberately never accepts score IDs from the browser: the current user's
 * complete draft score records are derived from an opaque assignment scope on
 * the server.
 */
@Service
@RequiredArgsConstructor
public class ReviewScoreSheetSigningService {

    private static final String FLAG_TRUE = "1";
    private static final String FLAG_FALSE = "0";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_WITHDRAWN = "withdrawn";
    private static final String SUBMISSION_MODE_SIGNED = "SIGNED";
    private static final String SUBMISSION_MODE_UNSIGNED = "UNSIGNED";
    private static final String WITHDRAWAL_MODE_SELF = "SELF";
    private static final String WITHDRAWAL_MODE_ADMIN_FORCE = "ADMIN_FORCE";
    private static final String PERM_SIGNED_SHEET_MANAGE = "crehn:reviewSheet:manage";
    private static final String PERM_SIGNED_SHEET_WITHDRAW = "crehn:reviewSheet:withdraw";
    private static final String LIBRARY_STATUS_ACTIVE = "active";
    private static final String LIBRARY_STATUS_DISABLED = "disabled";
    private static final String LIBRARY_STATUS_ARCHIVED = "archived";
    private static final int ACTIVE_MARKER = 1;
    private static final int MAX_WITHDRAW_REASON_LENGTH = 500;
    private static final long MAX_SIGNATURE_SIZE = 2L * 1024 * 1024;
    private static final int MAX_SIGNATURE_DIMENSION = 4096;
    private static final BigDecimal DEFAULT_X = new BigDecimal("0.42");
    private static final BigDecimal DEFAULT_Y = new BigDecimal("0.05");
    private static final BigDecimal DEFAULT_WIDTH = new BigDecimal("0.54");
    private static final BigDecimal DEFAULT_HEIGHT = new BigDecimal("0.90");
    private static final DateTimeFormatter SIGNED_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReviewScoreSheetSignatureMapper signatureMapper;
    private final ReviewScoreSheetSignedSheetMapper signedSheetMapper;
    private final ReviewScoreSheetSignedSheetItemMapper signedItemMapper;
    private final ReviewResultMapper reviewResultMapper;
    private final ReviewScoreMapper scoreMapper;
    private final ReviewScoreSheetService scoreSheetService;
    private final ReviewScoreSheetExcelWriter excelWriter;
    private final IArtReviewService reviewService;
    private final ISysOssService ossService;
    private final SysUserMapper sysUserMapper;

    public List<ReviewScoreSheetSignatureVo> listSignatures() {
        List<ReviewScoreSheetSignature> rows = signatureMapper.selectList(Wrappers.lambdaQuery(ReviewScoreSheetSignature.class)
            .eq(ReviewScoreSheetSignature::getReviewerUserId, currentUserId())
            .eq(ReviewScoreSheetSignature::getActiveMarker, ACTIVE_MARKER)
            .eq(ReviewScoreSheetSignature::getLibraryStatus, LIBRARY_STATUS_ACTIVE)
            .orderByDesc(ReviewScoreSheetSignature::getDefaultFlag)
            .orderByDesc(ReviewScoreSheetSignature::getUpdateTime)
            .orderByDesc(ReviewScoreSheetSignature::getId));
        return rows.stream().map(this::toSignatureVo).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreSheetSignatureVo createSignature(MultipartFile file, String signatureName, Boolean defaultSignature) {
        SignatureUpload upload = validateSignatureImage(file);
        Long userId = currentUserId();
        ReviewScoreSheetSignature duplicate = findSignatureByHash(userId, upload.contentSha256(), null);
        if (duplicate != null) {
            ReviewScoreSheetSignatureVo reused = toSignatureVo(duplicate);
            reused.setReused(true);
            return reused;
        }
        SysOssVo oss = ossService.upload(file);
        ReviewScoreSheetSignature signature = new ReviewScoreSheetSignature();
        signature.setReviewerUserId(userId);
        signature.setSignatureName(normalizeSignatureName(signatureName));
        signature.setOssId(oss.getOssId());
        signature.setContentSha256(upload.contentSha256());
        boolean makeDefault = Boolean.TRUE.equals(defaultSignature)
            || signatureMapper.selectCount(Wrappers.lambdaQuery(ReviewScoreSheetSignature.class)
            .eq(ReviewScoreSheetSignature::getReviewerUserId, userId)
            .eq(ReviewScoreSheetSignature::getActiveMarker, ACTIVE_MARKER)
            .eq(ReviewScoreSheetSignature::getLibraryStatus, LIBRARY_STATUS_ACTIVE)) == 0;
        if (makeDefault) {
            clearDefaultFlags(userId, null);
        }
        signature.setDefaultFlag(makeDefault ? FLAG_TRUE : FLAG_FALSE);
        signature.setVersion(1L);
        signature.setActiveMarker(ACTIVE_MARKER);
        signature.setLibraryStatus(LIBRARY_STATUS_ACTIVE);
        signature.setDelFlag("0");
        try {
            signatureMapper.insert(signature);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("该手写签名已在签名列表中");
        }
        return toSignatureVo(requireOwnedSignature(signature.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreSheetSignatureVo replaceSignature(Long signatureId, MultipartFile file, String signatureName) {
        ReviewScoreSheetSignature signature = requireOwnedSignature(signatureId);
        SignatureUpload upload = validateSignatureImage(file);
        if (upload.contentSha256().equals(signature.getContentSha256())) {
            if (StringUtils.isNotBlank(signatureName)) {
                signature.setSignatureName(normalizeSignatureName(signatureName));
                signatureMapper.updateById(signature);
            }
            return toSignatureVo(signature);
        }
        ReviewScoreSheetSignature duplicate = findSignatureByHash(currentUserId(), upload.contentSha256(), signatureId);
        if (duplicate != null) {
            throw new ServiceException("该手写签名已在签名列表中，请直接选择复用");
        }
        // Do not delete the old OSS object: signed-sheet headers retain its ID
        // as their immutable historical image snapshot.
        SysOssVo oss = ossService.upload(file);
        signature.setOssId(oss.getOssId());
        signature.setContentSha256(upload.contentSha256());
        signature.setVersion((signature.getVersion() == null ? 0L : signature.getVersion()) + 1);
        if (StringUtils.isNotBlank(signatureName)) {
            signature.setSignatureName(normalizeSignatureName(signatureName));
        }
        signatureMapper.updateById(signature);
        return toSignatureVo(requireOwnedSignature(signatureId));
    }

    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreSheetSignatureVo setDefaultSignature(Long signatureId) {
        ReviewScoreSheetSignature signature = requireOwnedSignature(signatureId);
        clearDefaultFlags(currentUserId(), signatureId);
        signatureMapper.update(null, Wrappers.<ReviewScoreSheetSignature>lambdaUpdate()
            .set(ReviewScoreSheetSignature::getDefaultFlag, FLAG_TRUE)
            .eq(ReviewScoreSheetSignature::getId, signatureId)
            .eq(ReviewScoreSheetSignature::getReviewerUserId, currentUserId())
            .eq(ReviewScoreSheetSignature::getActiveMarker, ACTIVE_MARKER)
            .eq(ReviewScoreSheetSignature::getLibraryStatus, LIBRARY_STATUS_ACTIVE));
        return toSignatureVo(requireOwnedSignature(signatureId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteSignature(Long signatureId) {
        ReviewScoreSheetSignature signature = requireOwnedSignature(signatureId);
        // MySQL permits multiple NULL values in a unique key. Clear the active
        // marker before the logical delete so a delete -> re-upload -> delete
        // cycle never collides with an older deleted hash.
        int activeMarkerCleared = signatureMapper.update(null, Wrappers.<ReviewScoreSheetSignature>lambdaUpdate()
            .set(ReviewScoreSheetSignature::getActiveMarker, null)
            .eq(ReviewScoreSheetSignature::getId, signatureId)
            .eq(ReviewScoreSheetSignature::getReviewerUserId, currentUserId())
            .eq(ReviewScoreSheetSignature::getActiveMarker, ACTIVE_MARKER));
        if (activeMarkerCleared != 1) {
            throw new ServiceException("手写签名状态已变更，请刷新后重试");
        }
        signatureMapper.deleteById(signatureId);
        // The underlying object is intentionally retained. Existing signed
        // sheets reference that precise object and must stay reproducible.
        if (FLAG_TRUE.equals(signature.getDefaultFlag())) {
            ReviewScoreSheetSignature next = signatureMapper.selectOne(Wrappers.lambdaQuery(ReviewScoreSheetSignature.class)
                .eq(ReviewScoreSheetSignature::getReviewerUserId, currentUserId())
                .eq(ReviewScoreSheetSignature::getActiveMarker, ACTIVE_MARKER)
                .eq(ReviewScoreSheetSignature::getLibraryStatus, LIBRARY_STATUS_ACTIVE)
                .orderByDesc(ReviewScoreSheetSignature::getUpdateTime)
                .last("limit 1"));
            if (next != null) {
                clearDefaultFlags(currentUserId(), next.getId());
                signatureMapper.update(null, Wrappers.<ReviewScoreSheetSignature>lambdaUpdate()
                    .set(ReviewScoreSheetSignature::getDefaultFlag, FLAG_TRUE)
                    .eq(ReviewScoreSheetSignature::getId, next.getId())
                    .eq(ReviewScoreSheetSignature::getReviewerUserId, currentUserId())
                    .eq(ReviewScoreSheetSignature::getActiveMarker, ACTIVE_MARKER));
            }
        }
    }

    /**
     * Privileged library console. It intentionally exposes asset metadata and
     * the immutable PNG only; managers never replace or delete a teacher's
     * handwritten signature on the teacher's behalf.
     */
    public TableDataInfo<ReviewScoreSheetSignatureAdminVo> querySignatureAdminPage(ReviewScoreSheetSignatureAdminQueryBo query,
                                                                                      PageQuery pageQuery) {
        ReviewScoreSheetSignatureAdminQueryBo effectiveQuery = query == null ? new ReviewScoreSheetSignatureAdminQueryBo() : query;
        String libraryStatus = normalizeLibraryStatus(effectiveQuery.getLibraryStatus(), true);
        List<Long> reviewerIds = null;
        if (StringUtils.isNotBlank(effectiveQuery.getReviewerKeyword())) {
            reviewerIds = sysUserMapper.selectList(Wrappers.lambdaQuery(SysUser.class)
                    .select(SysUser::getUserId)
                    .eq(SysUser::getDelFlag, FLAG_FALSE)
                    .and(wrapper -> wrapper.like(SysUser::getUserName, effectiveQuery.getReviewerKeyword())
                        .or().like(SysUser::getNickName, effectiveQuery.getReviewerKeyword())))
                .stream().map(SysUser::getUserId).filter(Objects::nonNull).toList();
            if (reviewerIds.isEmpty()) {
                return new TableDataInfo<>(List.of(), 0);
            }
        }
        Page<ReviewScoreSheetSignature> page = signatureMapper.selectPage(pageQuery.build(),
            Wrappers.lambdaQuery(ReviewScoreSheetSignature.class)
                .eq(effectiveQuery.getReviewerUserId() != null, ReviewScoreSheetSignature::getReviewerUserId,
                    effectiveQuery.getReviewerUserId())
                .in(reviewerIds != null, ReviewScoreSheetSignature::getReviewerUserId, reviewerIds)
                .like(StringUtils.isNotBlank(effectiveQuery.getSignatureName()), ReviewScoreSheetSignature::getSignatureName,
                    effectiveQuery.getSignatureName())
                .eq(libraryStatus != null, ReviewScoreSheetSignature::getLibraryStatus, libraryStatus)
                .eq(ReviewScoreSheetSignature::getActiveMarker, ACTIVE_MARKER)
                .orderByDesc(ReviewScoreSheetSignature::getUpdateTime)
                .orderByDesc(ReviewScoreSheetSignature::getId));
        return new TableDataInfo<>(toSignatureAdminVos(page.getRecords()), page.getTotal());
    }

    public ReviewScoreSheetSignatureAdminVo getSignatureAdmin(Long signatureId) {
        ReviewScoreSheetSignature signature = requireManagedSignature(signatureId);
        return toSignatureAdminVo(signature, loadUsersForSignatureRows(List.of(signature)));
    }

    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreSheetSignatureAdminVo disableSignatureAdmin(Long signatureId, ReviewScoreSheetSignatureAdminStatusBo bo) {
        return updateManagedSignatureStatus(signatureId, LIBRARY_STATUS_DISABLED, bo == null ? null : bo.getReason());
    }

    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreSheetSignatureAdminVo restoreSignatureAdmin(Long signatureId, ReviewScoreSheetSignatureAdminStatusBo bo) {
        return updateManagedSignatureStatus(signatureId, LIBRARY_STATUS_ACTIVE, bo == null ? null : bo.getReason());
    }

    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreSheetSignatureAdminVo archiveSignatureAdmin(Long signatureId, ReviewScoreSheetSignatureAdminStatusBo bo) {
        return updateManagedSignatureStatus(signatureId, LIBRARY_STATUS_ARCHIVED, bo == null ? null : bo.getReason());
    }

    /** Returns the complete, still-draft scores eligible for the next scope batch. */
    public ReviewScoreSheetExportPreviewVo previewUnsignedSubmittedScores(ReviewScoreSheetExportBo bo) {
        assertScope(bo == null ? null : bo.getActivityId(), bo == null ? null : bo.getCategoryId());
        assertReviewScopeKey(bo == null ? null : bo.getScopeKey());
        ReviewScoreSheetExportPreviewVo preview = new ReviewScoreSheetExportPreviewVo();
        preview.setTemplate(scoreSheetService.resolveTemplateForExport(bo));
        ScopeSubmissionBatch batch = queryCurrentSignableScopeBatch(bo.getActivityId(), bo.getCategoryId(), bo.getScopeKey());
        List<ReviewTaskVo> tasks = batch.eligibleTasks();
        preview.setRows(tasks);
        preview.setTotal((long) tasks.size());
        preview.setAssignedTotal((long) batch.allTasks().size());
        preview.setCompletedCount(batch.completedCount());
        preview.setSubmittedCount(batch.submittedCount());
        preview.setPendingCount(batch.pendingCount());
        preview.setRemainingCount(Math.max(0L, batch.allTasks().size() - batch.submittedCount() - tasks.size()));
        preview.setCompleteAfterSubmit(!batch.allTasks().isEmpty()
            && batch.submittedCount() + tasks.size() == batch.allTasks().size());
        preview.setReviewerName(scoreSheetService.currentReviewerDisplayName());
        preview.setCategoryName(batch.allTasks().isEmpty() ? null : batch.allTasks().get(0).getCategoryName());
        preview.setExportTime(formatSignedTime(new Date()));
        preview.setSignable(!tasks.isEmpty());
        if (tasks.isEmpty()) {
            preview.setMessage(!batch.allTasks().isEmpty() && batch.submittedCount() == batch.allTasks().size()
                ? "当前评审范围已全部提交"
                : "当前评审范围没有待提交的已评分作品");
        } else if (batch.pendingCount() > 0) {
            preview.setMessage("当前评审范围尚有" + batch.pendingCount() + "个作品未评分，可先提交本批已评分作品");
        } else {
            preview.setMessage("本批提交后，当前评审范围将全部完成");
        }
        return preview;
    }

    /**
     * Persists the server-derived score, template, signature, placement and
     * time snapshots in one transaction. The database unique key protects
     * against two browser windows signing the same score concurrently.
     */
    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreSheetSignedSheetVo createSignedSheet(ReviewScoreSheetSignedSheetCreateBo bo) {
        if (bo == null) {
            throw new ServiceException("签名表内容不能为空");
        }
        assertScope(bo.getActivityId(), bo.getCategoryId());
        assertReviewScopeKey(bo.getScopeKey());
        ReviewScoreSheetExportBo exportBo = new ReviewScoreSheetExportBo();
        exportBo.setActivityId(bo.getActivityId());
        exportBo.setCategoryId(bo.getCategoryId());
        exportBo.setScopeKey(bo.getScopeKey());
        exportBo.setTemplateId(bo.getTemplateId());
        exportBo.setTemplate(bo.getTemplate());
        ReviewScoreSheetTemplateVo template = scoreSheetService.resolveTemplateForExport(exportBo);
        // Signature policy is server-owned. A browser may supply a transient
        // export layout, but it cannot turn a persisted required signature
        // into an optional one or hide a persisted signature timestamp.
        ReviewScoreSheetTemplateVo policyTemplate = resolveSignaturePolicyTemplate(bo);
        applySignaturePolicy(template, policyTemplate);
        ReviewScoreSheetFooterFieldVo policySignatureField = findSignatureField(policyTemplate);
        boolean signatureRequired = isSignatureRequired(policySignatureField);
        boolean signatureProvided = bo.getSignatureId() != null || bo.getPlacement() != null;
        ReviewScoreSheetSignature signature = null;
        ReviewScoreSheetSignaturePlacementVo placement = null;
        String submissionMode;
        if (!signatureProvided) {
            if (signatureRequired) {
                throw new ServiceException("当前评分表模板要求手写签名，请先签名后保存");
            }
            submissionMode = SUBMISSION_MODE_UNSIGNED;
        } else {
            if (bo.getSignatureId() == null || bo.getPlacement() == null) {
                throw new ServiceException("手写签名和签字位置必须同时填写");
            }
            signature = requireOwnedSignature(bo.getSignatureId());
            placement = normalizePlacement(bo.getPlacement());
            requireSignatureSlot(template, placement.getSlotKey());
            submissionMode = SUBMISSION_MODE_SIGNED;
        }

        ScopeSubmissionBatch batch = queryCurrentSignableScopeBatch(bo.getActivityId(), bo.getCategoryId(), bo.getScopeKey());
        List<ReviewTaskVo> tasks = batch.eligibleTasks();
        if (tasks.isEmpty()) {
            throw new ServiceException("当前评审范围没有待提交的已评分作品");
        }
        Date signedAt = new Date();
        ReviewScoreSheetSignedSheet signedSheet = new ReviewScoreSheetSignedSheet();
        signedSheet.setReviewerUserId(currentUserId());
        signedSheet.setReviewerNameSnapshot(scoreSheetService.currentReviewerDisplayName());
        signedSheet.setActivityId(bo.getActivityId());
        signedSheet.setActivityNameSnapshot(tasks.get(0).getActivityName());
        signedSheet.setCategoryId(bo.getCategoryId());
        signedSheet.setCategoryNameSnapshot(tasks.get(0).getCategoryName());
        signedSheet.setTemplateId(template.getId());
        signedSheet.setTemplateVersion(template.getVersion());
        signedSheet.setTemplateSnapshotJson(JsonUtils.toJsonString(template));
        signedSheet.setSignatureId(signature == null ? null : signature.getId());
        signedSheet.setSignatureOssId(signature == null ? null : signature.getOssId());
        signedSheet.setSignatureNameSnapshot(signature == null ? null : signature.getSignatureName());
        signedSheet.setSignaturePlacementJson(placement == null ? null : JsonUtils.toJsonString(placement));
        signedSheet.setSubmissionMode(submissionMode);
        signedSheet.setScoreCount((long) tasks.size());
        signedSheet.setSignedAt(signedAt);
        signedSheet.setStatus(STATUS_ACTIVE);
        signedSheet.setActiveMarker(ACTIVE_MARKER);
        signedSheetMapper.insert(signedSheet);
        try {
            for (ReviewTaskVo task : tasks) {
                if (task.getScoreId() == null) {
                    throw new ServiceException("当前类别存在未评分作品，请完成后再签字");
                }
                int locked = scoreMapper.update(null, Wrappers.<ReviewScore>lambdaUpdate()
                    .set(ReviewScore::getStatus, ArtReviewConstants.REVIEW_SCORE_SUBMITTED)
                    .set(ReviewScore::getSubmittedAt, signedAt)
                    .eq(ReviewScore::getId, task.getScoreId())
                    .eq(ReviewScore::getReviewerUserId, currentUserId())
                    .eq(ReviewScore::getStatus, ArtReviewConstants.REVIEW_SCORE_DRAFT));
                if (locked != 1) {
                    throw new ServiceException("评分状态已变化，请刷新后重新提交签字");
                }
                task.setScoreStatus(ArtReviewConstants.REVIEW_SCORE_SUBMITTED);
                task.setScoreSubmittedAt(signedAt);
                ReviewScoreSheetSignedSheetItem item = new ReviewScoreSheetSignedSheetItem();
                item.setSignedSheetId(signedSheet.getId());
                item.setReviewScoreId(task.getScoreId());
                item.setAssignmentId(task.getAssignmentId());
                item.setProjectId(task.getProjectId());
                item.setScoreSnapshotJson(JsonUtils.toJsonString(task));
                item.setActiveMarker(ACTIVE_MARKER);
                signedItemMapper.insert(item);
            }
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("部分评分已在有效签名表中，请刷新列表后重试");
        }
        return getSignedSheet(signedSheet.getId());
    }

    public List<ReviewScoreSheetSignedSheetVo> listSignedSheets(Long activityId, Long categoryId) {
        List<ReviewScoreSheetSignedSheet> rows = signedSheetMapper.selectList(Wrappers.lambdaQuery(ReviewScoreSheetSignedSheet.class)
            .eq(ReviewScoreSheetSignedSheet::getReviewerUserId, currentUserId())
            .eq(activityId != null, ReviewScoreSheetSignedSheet::getActivityId, activityId)
            .eq(categoryId != null, ReviewScoreSheetSignedSheet::getCategoryId, categoryId)
            .orderByDesc(ReviewScoreSheetSignedSheet::getSignedAt)
            .orderByDesc(ReviewScoreSheetSignedSheet::getId));
        return rows.stream().map(sheet -> toSignedSheetVo(sheet, false)).toList();
    }

    /**
     * Management summary and "my signed sheets" page. Browser-provided owner
     * filters never expand a reviewer's scope.
     */
    public TableDataInfo<ReviewScoreSheetSignedSheetVo> querySignedSheetPage(ReviewScoreSheetSignedSheetQueryBo query,
                                                                               PageQuery pageQuery) {
        ReviewScoreSheetSignedSheetQueryBo effectiveQuery = query == null ? new ReviewScoreSheetSignedSheetQueryBo() : query;
        boolean managementScope = canManageSignedSheets();
        Long currentUserId = currentUserId();
        if (!managementScope && effectiveQuery.getReviewerUserId() != null
            && !Objects.equals(effectiveQuery.getReviewerUserId(), currentUserId)) {
            throw new ServiceException("无权查看其他评审老师的签名表");
        }
        if (!managementScope && Boolean.FALSE.equals(effectiveQuery.getOwnerOnly())) {
            throw new ServiceException("无权查看签名表管理汇总");
        }

        boolean ownerOnly = !managementScope || Boolean.TRUE.equals(effectiveQuery.getOwnerOnly());
        Long reviewerUserId = ownerOnly ? currentUserId : effectiveQuery.getReviewerUserId();
        String status = normalizeSignedSheetStatus(effectiveQuery.getStatus());
        Date signedAtStart = parseSignedAtFilter(effectiveQuery.getSignedAtStart(), false);
        Date signedAtEnd = parseSignedAtFilter(effectiveQuery.getSignedAtEnd(), true);
        if (signedAtStart != null && signedAtEnd != null && signedAtStart.after(signedAtEnd)) {
            throw new ServiceException("签名时间起始不能晚于结束时间");
        }

        Page<ReviewScoreSheetSignedSheet> page = signedSheetMapper.selectPage(pageQuery.build(),
            Wrappers.lambdaQuery(ReviewScoreSheetSignedSheet.class)
                .eq(effectiveQuery.getActivityId() != null, ReviewScoreSheetSignedSheet::getActivityId, effectiveQuery.getActivityId())
                .eq(effectiveQuery.getCategoryId() != null, ReviewScoreSheetSignedSheet::getCategoryId, effectiveQuery.getCategoryId())
                .eq(reviewerUserId != null, ReviewScoreSheetSignedSheet::getReviewerUserId, reviewerUserId)
                .eq(status != null, ReviewScoreSheetSignedSheet::getStatus, status)
                .ge(signedAtStart != null, ReviewScoreSheetSignedSheet::getSignedAt, signedAtStart)
                .le(signedAtEnd != null, ReviewScoreSheetSignedSheet::getSignedAt, signedAtEnd)
                .orderByDesc(ReviewScoreSheetSignedSheet::getSignedAt)
                .orderByDesc(ReviewScoreSheetSignedSheet::getId));
        return new TableDataInfo<>(page.getRecords().stream().map(sheet -> toSignedSheetVo(sheet, false)).toList(), page.getTotal());
    }

    /** Management selector based on history, so an unassigned former reviewer remains searchable. */
    public List<ReviewScoreSheetReviewerOptionVo> listSignedSheetReviewerOptions(Long activityId, Long categoryId, String keyword) {
        assertManageSignedSheets();
        String normalizedKeyword = StringUtils.trim(keyword);
        List<ReviewScoreSheetSignedSheet> rows = signedSheetMapper.selectList(Wrappers.lambdaQuery(ReviewScoreSheetSignedSheet.class)
            .select(ReviewScoreSheetSignedSheet::getReviewerUserId, ReviewScoreSheetSignedSheet::getReviewerNameSnapshot)
            .eq(activityId != null, ReviewScoreSheetSignedSheet::getActivityId, activityId)
            .eq(categoryId != null, ReviewScoreSheetSignedSheet::getCategoryId, categoryId)
            .like(StringUtils.isNotBlank(normalizedKeyword), ReviewScoreSheetSignedSheet::getReviewerNameSnapshot, normalizedKeyword)
            .orderByDesc(ReviewScoreSheetSignedSheet::getSignedAt)
            .orderByDesc(ReviewScoreSheetSignedSheet::getId));
        Map<Long, ReviewScoreSheetReviewerOptionVo> options = new LinkedHashMap<>();
        for (ReviewScoreSheetSignedSheet row : rows) {
            if (row.getReviewerUserId() == null || options.containsKey(row.getReviewerUserId())) {
                continue;
            }
            ReviewScoreSheetReviewerOptionVo option = new ReviewScoreSheetReviewerOptionVo();
            option.setReviewerUserId(row.getReviewerUserId());
            option.setReviewerName(StringUtils.blankToDefault(row.getReviewerNameSnapshot(), String.valueOf(row.getReviewerUserId())));
            options.put(option.getReviewerUserId(), option);
        }
        return new ArrayList<>(options.values());
    }

    public ReviewScoreSheetSignedSheetVo getSignedSheet(Long signedSheetId) {
        return toSignedSheetVo(requireReadableSignedSheet(signedSheetId), true);
    }

    public void exportSignedSheet(Long signedSheetId, HttpServletResponse response) {
        ReviewScoreSheetSignedSheet signedSheet = requireReadableSignedSheet(signedSheetId);
        if (isWithdrawn(signedSheet)) {
            throw new ServiceException("该签名表已撤回，仅保留审计记录，不能下载或打印");
        }
        ReviewScoreSheetSignedSheetVo snapshot = toSignedSheetVo(signedSheet, true);
        ReviewScoreSheetSignatureImage signatureImage = null;
        if (hasSignatureSnapshot(signedSheet)) {
            byte[] pngBytes = loadSignaturePng(signedSheet.getSignatureOssId());
            signatureImage = new ReviewScoreSheetSignatureImage(snapshot.getPlacement().getSlotKey(), pngBytes,
                snapshot.getPlacement(), snapshot.getExportTime(), isSignatureTimeVisible(snapshot.getTemplate()));
        }
        String filename = safeFilename(snapshot.getCategoryName()) + "-已签名评审打分表.xlsx";
        FileUtils.setAttachmentResponseHeader(response, filename);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        try {
            excelWriter.write(snapshot.getRows(), snapshot.getTemplate(), snapshot.getReviewerName(), snapshot.getCategoryName(),
                snapshot.getExportTime(), signatureImage, response.getOutputStream());
        } catch (IOException ex) {
            throw new ServiceException("导出已签名评审打分表失败");
        }
    }

    /**
     * Makes the sheet an immutable withdrawn audit record and restores all
     * included scores to the editable state in the same transaction.
     */
    @Transactional(rollbackFor = Exception.class)
    public ReviewScoreSheetSignedSheetVo withdrawSignedSheet(Long signedSheetId, ReviewScoreSheetSignedSheetWithdrawBo bo) {
        // A locking read makes repeated concurrent withdrawal requests observe
        // the first committed withdrawal and return its same audit snapshot.
        ReviewScoreSheetSignedSheet signedSheet = requireReadableSignedSheetForUpdate(signedSheetId);
        Long operatorUserId = currentUserId();
        boolean selfWithdrawal = Objects.equals(signedSheet.getReviewerUserId(), operatorUserId);
        if (!selfWithdrawal && (!canManageSignedSheets() || !StpUtil.hasPermission(PERM_SIGNED_SHEET_WITHDRAW))) {
            throw new ServiceException("无权撤回其他评审老师的签名表");
        }
        if (isWithdrawn(signedSheet)) {
            return toSignedSheetVo(signedSheet, false);
        }

        String reason = normalizeWithdrawReason(bo == null ? null : bo.getReason());
        assertNoGeneratedResult(signedSheet);
        Date withdrawnAt = new Date();
        String mode = selfWithdrawal ? WITHDRAWAL_MODE_SELF : WITHDRAWAL_MODE_ADMIN_FORCE;
        int changed = signedSheetMapper.update(null, Wrappers.<ReviewScoreSheetSignedSheet>lambdaUpdate()
            .set(ReviewScoreSheetSignedSheet::getStatus, STATUS_WITHDRAWN)
            .set(ReviewScoreSheetSignedSheet::getActiveMarker, null)
            .set(ReviewScoreSheetSignedSheet::getWithdrawnAt, withdrawnAt)
            .set(ReviewScoreSheetSignedSheet::getWithdrawnByUserId, operatorUserId)
            .set(ReviewScoreSheetSignedSheet::getWithdrawnByNameSnapshot, currentOperatorDisplayName())
            .set(ReviewScoreSheetSignedSheet::getWithdrawnByRoleSnapshot, currentOperatorRoleSnapshot())
            .set(ReviewScoreSheetSignedSheet::getWithdrawalMode, mode)
            .set(ReviewScoreSheetSignedSheet::getWithdrawReason, reason)
            .eq(ReviewScoreSheetSignedSheet::getId, signedSheetId)
            .eq(ReviewScoreSheetSignedSheet::getStatus, STATUS_ACTIVE)
            .eq(ReviewScoreSheetSignedSheet::getActiveMarker, ACTIVE_MARKER));
        if (changed == 0) {
            ReviewScoreSheetSignedSheet latest = signedSheetMapper.selectById(signedSheetId);
            if (latest != null && isWithdrawn(latest)) {
                return toSignedSheetVo(latest, false);
            }
            throw new ServiceException("签名表状态已变更，请刷新后重试");
        }

        List<ReviewScoreSheetSignedSheetItem> activeItems = signedItemMapper.selectList(
            Wrappers.lambdaQuery(ReviewScoreSheetSignedSheetItem.class)
                .eq(ReviewScoreSheetSignedSheetItem::getSignedSheetId, signedSheetId)
                .eq(ReviewScoreSheetSignedSheetItem::getActiveMarker, ACTIVE_MARKER));
        if (signedSheet.getScoreCount() == null || !Objects.equals(signedSheet.getScoreCount(), (long) activeItems.size())) {
            throw new ServiceException("签名表评分明细状态异常，无法撤回");
        }
        int released = signedItemMapper.update(null, Wrappers.<ReviewScoreSheetSignedSheetItem>lambdaUpdate()
            .set(ReviewScoreSheetSignedSheetItem::getActiveMarker, null)
            .eq(ReviewScoreSheetSignedSheetItem::getSignedSheetId, signedSheetId)
            .eq(ReviewScoreSheetSignedSheetItem::getActiveMarker, ACTIVE_MARKER));
        if (signedSheet.getScoreCount() == null || !Objects.equals(signedSheet.getScoreCount(), Long.valueOf(released))) {
            throw new ServiceException("签名表评分明细状态异常，无法撤回");
        }
        List<Long> scoreIds = activeItems.stream().map(ReviewScoreSheetSignedSheetItem::getReviewScoreId)
            .filter(Objects::nonNull).toList();
        if (!scoreIds.isEmpty()) {
            int unlocked = scoreMapper.update(null, Wrappers.<ReviewScore>lambdaUpdate()
                .set(ReviewScore::getStatus, ArtReviewConstants.REVIEW_SCORE_DRAFT)
                .set(ReviewScore::getSubmittedAt, null)
                .in(ReviewScore::getId, scoreIds)
                .eq(ReviewScore::getReviewerUserId, signedSheet.getReviewerUserId()));
            if (unlocked != scoreIds.size()) {
                throw new ServiceException("签名表评分解锁数量异常，无法撤回");
            }
        }
        return toSignedSheetVo(requireReadableSignedSheet(signedSheetId), false);
    }

    private ScopeSubmissionBatch queryCurrentSignableScopeBatch(Long activityId, Long categoryId, String scopeKey) {
        List<ReviewTaskVo> tasks = reviewService.queryMyScopeTasksForSignature(activityId, categoryId, scopeKey);
        Set<Long> scoreIds = tasks.stream().map(ReviewTaskVo::getScoreId)
            .filter(id -> id != null).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<Long> alreadySigned = scoreIds.isEmpty() ? Set.of() : signedItemMapper.selectList(
            Wrappers.lambdaQuery(ReviewScoreSheetSignedSheetItem.class)
                .select(ReviewScoreSheetSignedSheetItem::getReviewScoreId)
                .in(ReviewScoreSheetSignedSheetItem::getReviewScoreId, scoreIds)
                .eq(ReviewScoreSheetSignedSheetItem::getActiveMarker, ACTIVE_MARKER))
            .stream().map(ReviewScoreSheetSignedSheetItem::getReviewScoreId).collect(java.util.stream.Collectors.toSet());
        long completedCount = tasks.stream().filter(this::isTaskScoreComplete).count();
        long submittedCount = 0L;
        List<ReviewTaskVo> eligibleTasks = new ArrayList<>();
        for (ReviewTaskVo task : tasks) {
            boolean signed = task.getScoreId() != null && alreadySigned.contains(task.getScoreId());
            boolean submitted = ArtReviewConstants.REVIEW_SCORE_SUBMITTED.equals(task.getScoreStatus());
            if (signed != submitted) {
                throw new ServiceException("当前评审范围的评分与有效评分表状态不一致，请联系管理员处理");
            }
            if (submitted) {
                submittedCount++;
                continue;
            }
            if (isTaskScoreComplete(task)
                && ArtReviewConstants.REVIEW_SCORE_DRAFT.equals(task.getScoreStatus())) {
                eligibleTasks.add(task);
            }
        }
        return new ScopeSubmissionBatch(
            tasks,
            eligibleTasks,
            completedCount,
            submittedCount,
            Math.max(0L, tasks.size() - completedCount)
        );
    }

    private void assertReviewScopeKey(String scopeKey) {
        if (StringUtils.isBlank(scopeKey)) {
            throw new ServiceException("请选择一个具体评审范围");
        }
    }

    private boolean isTaskScoreComplete(ReviewTaskVo task) {
        if (task == null || task.getScoreId() == null) {
            return false;
        }
        return ArtReviewConstants.REVIEW_SCORE_MODE_GRADE.equals(task.getScoreMode())
            ? StringUtils.isNotBlank(task.getGradeValue())
            : task.getScoreValue() != null;
    }

    private record ScopeSubmissionBatch(
        List<ReviewTaskVo> allTasks,
        List<ReviewTaskVo> eligibleTasks,
        long completedCount,
        long submittedCount,
        long pendingCount
    ) {
    }

    private ReviewScoreSheetSignedSheetVo toSignedSheetVo(ReviewScoreSheetSignedSheet signedSheet, boolean includeRows) {
        ReviewScoreSheetSignedSheetVo vo = new ReviewScoreSheetSignedSheetVo();
        vo.setId(signedSheet.getId());
        vo.setActivityId(signedSheet.getActivityId());
        vo.setActivityName(signedSheet.getActivityNameSnapshot());
        vo.setCategoryId(signedSheet.getCategoryId());
        vo.setCategoryName(signedSheet.getCategoryNameSnapshot());
        vo.setReviewerUserId(signedSheet.getReviewerUserId());
        vo.setReviewerName(signedSheet.getReviewerNameSnapshot());
        vo.setTemplateId(signedSheet.getTemplateId());
        vo.setTemplateVersion(signedSheet.getTemplateVersion());
        vo.setSignatureId(signedSheet.getSignatureId());
        vo.setSignatureOssId(signedSheet.getSignatureOssId());
        vo.setSignatureName(signedSheet.getSignatureNameSnapshot());
        vo.setSubmissionMode(normalizeSubmissionMode(signedSheet.getSubmissionMode()));
        if (hasSignatureSnapshot(signedSheet)) {
            String signatureUrl = resolveOssUrl(signedSheet.getSignatureOssId());
            vo.setSignatureUrl(signatureUrl);
            ReviewScoreSheetSignaturePlacementVo placement = parsePlacement(signedSheet.getSignaturePlacementJson());
            placement.setSignatureId(signedSheet.getSignatureId());
            placement.setSignatureName(signedSheet.getSignatureNameSnapshot());
            placement.setSignatureUrl(signatureUrl);
            placement.setSignedAt(signedSheet.getSignedAt());
            vo.setPlacement(placement);
        }
        vo.setTotal(signedSheet.getScoreCount());
        vo.setSignedAt(signedSheet.getSignedAt());
        vo.setExportTime(formatSignedTime(signedSheet.getSignedAt()));
        vo.setStatus(signedSheet.getStatus());
        vo.setWithdrawnAt(signedSheet.getWithdrawnAt());
        vo.setWithdrawnByUserId(signedSheet.getWithdrawnByUserId());
        vo.setWithdrawnByName(signedSheet.getWithdrawnByNameSnapshot());
        vo.setWithdrawnByRole(signedSheet.getWithdrawnByRoleSnapshot());
        vo.setWithdrawalMode(signedSheet.getWithdrawalMode());
        vo.setWithdrawReason(signedSheet.getWithdrawReason());
        if (includeRows) {
            vo.setTemplate(parseTemplate(signedSheet.getTemplateSnapshotJson()));
            vo.setRows(parseItemSnapshots(signedSheet.getId()));
        }
        return vo;
    }

    /**
     * Resolves only the persisted signature-policy switches. The rest of a
     * temporary export layout can still be snapshotted as before, but a
     * reviewer cannot relax a mandatory signature by changing request JSON.
     */
    private ReviewScoreSheetTemplateVo resolveSignaturePolicyTemplate(ReviewScoreSheetSignedSheetCreateBo bo) {
        Long templateId = bo.getTemplateId();
        if (templateId == null && bo.getTemplate() != null) {
            templateId = bo.getTemplate().getId();
        }
        return templateId == null ? scoreSheetService.getTemplate() : scoreSheetService.getTemplate(templateId);
    }

    private void applySignaturePolicy(ReviewScoreSheetTemplateVo template, ReviewScoreSheetTemplateVo policyTemplate) {
        ReviewScoreSheetFooterFieldVo effectiveSignatureField = findSignatureField(template);
        if (effectiveSignatureField == null) {
            return;
        }
        ReviewScoreSheetFooterFieldVo policySignatureField = findSignatureField(policyTemplate);
        effectiveSignatureField.setSignatureRequired(isSignatureRequired(policySignatureField));
        effectiveSignatureField.setSignatureTimeVisible(isSignatureTimeVisible(policySignatureField));
    }

    private ReviewScoreSheetFooterFieldVo findSignatureField(ReviewScoreSheetTemplateVo template) {
        if (template == null || template.getFooterRows() == null) {
            return null;
        }
        for (ReviewScoreSheetFooterRowVo row : template.getFooterRows()) {
            if (row == null) {
                continue;
            }
            if (isSignatureField(row.getLeft())) {
                return row.getLeft();
            }
            if (isSignatureField(row.getRight())) {
                return row.getRight();
            }
        }
        return null;
    }

    private boolean isSignatureField(ReviewScoreSheetFooterFieldVo field) {
        return field != null && "signature".equals(field.getType());
    }

    /** Legacy signature fields omit the switch and remain required; templates without a signature field submit unsigned. */
    private boolean isSignatureRequired(ReviewScoreSheetFooterFieldVo signatureField) {
        return signatureField != null && !Boolean.FALSE.equals(signatureField.getSignatureRequired());
    }

    /** Legacy JSON has no switch, so historic signed snapshots keep their time visible. */
    private boolean isSignatureTimeVisible(ReviewScoreSheetFooterFieldVo signatureField) {
        return signatureField == null || !Boolean.FALSE.equals(signatureField.getSignatureTimeVisible());
    }

    private boolean isSignatureTimeVisible(ReviewScoreSheetTemplateVo template) {
        return isSignatureTimeVisible(findSignatureField(template));
    }

    private boolean hasSignatureSnapshot(ReviewScoreSheetSignedSheet signedSheet) {
        return signedSheet != null
            && SUBMISSION_MODE_SIGNED.equals(normalizeSubmissionMode(signedSheet.getSubmissionMode()))
            && signedSheet.getSignatureId() != null
            && signedSheet.getSignatureOssId() != null
            && StringUtils.isNotBlank(signedSheet.getSignaturePlacementJson());
    }

    private String normalizeSubmissionMode(String submissionMode) {
        return SUBMISSION_MODE_UNSIGNED.equalsIgnoreCase(StringUtils.trim(submissionMode))
            ? SUBMISSION_MODE_UNSIGNED : SUBMISSION_MODE_SIGNED;
    }

    private List<ReviewTaskVo> parseItemSnapshots(Long signedSheetId) {
        List<ReviewScoreSheetSignedSheetItem> items = signedItemMapper.selectList(Wrappers.lambdaQuery(ReviewScoreSheetSignedSheetItem.class)
            .eq(ReviewScoreSheetSignedSheetItem::getSignedSheetId, signedSheetId)
            .orderByAsc(ReviewScoreSheetSignedSheetItem::getId));
        List<ReviewTaskVo> rows = new ArrayList<>();
        for (ReviewScoreSheetSignedSheetItem item : items) {
            try {
                ReviewTaskVo row = JsonUtils.parseObject(item.getScoreSnapshotJson(), ReviewTaskVo.class);
                if (row != null) {
                    rows.add(row);
                }
            } catch (Exception ex) {
                throw new ServiceException("签名表评分快照损坏，无法读取");
            }
        }
        return rows;
    }

    private ReviewScoreSheetTemplateVo parseTemplate(String json) {
        try {
            ReviewScoreSheetTemplateVo template = JsonUtils.parseObject(json, ReviewScoreSheetTemplateVo.class);
            if (template == null || template.getColumns() == null || template.getFooterRows() == null) {
                throw new ServiceException("签名表模板快照损坏，无法导出");
            }
            return template;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("签名表模板快照损坏，无法导出");
        }
    }

    private ReviewScoreSheetSignaturePlacementVo parsePlacement(String json) {
        try {
            ReviewScoreSheetSignaturePlacementVo placement = JsonUtils.parseObject(json, ReviewScoreSheetSignaturePlacementVo.class);
            return normalizePlacement(placement);
        } catch (Exception ex) {
            throw new ServiceException("签名位置快照损坏，无法导出");
        }
    }

    private ReviewScoreSheetSignaturePlacementVo normalizePlacement(ReviewScoreSheetSignaturePlacementVo input) {
        if (input == null || StringUtils.isBlank(input.getSlotKey())) {
            throw new ServiceException("请选择评分老师签字区域");
        }
        String slotKey = input.getSlotKey().trim();
        if (!slotKey.matches("[a-z][a-z0-9_]{0,63}")) {
            throw new ServiceException("签字槽位标识无效");
        }
        BigDecimal x = input.getX() == null ? DEFAULT_X : input.getX();
        BigDecimal y = input.getY() == null ? DEFAULT_Y : input.getY();
        BigDecimal width = input.getWidth() == null ? DEFAULT_WIDTH : input.getWidth();
        BigDecimal height = input.getHeight() == null ? DEFAULT_HEIGHT : input.getHeight();
        if (x.compareTo(BigDecimal.ZERO) < 0 || y.compareTo(BigDecimal.ZERO) < 0
            || width.compareTo(BigDecimal.ZERO) <= 0 || height.compareTo(BigDecimal.ZERO) <= 0
            || x.add(width).compareTo(BigDecimal.ONE) > 0 || y.add(height).compareTo(BigDecimal.ONE) > 0) {
            throw new ServiceException("签名位置必须位于签字区域内");
        }
        ReviewScoreSheetSignaturePlacementVo normalized = new ReviewScoreSheetSignaturePlacementVo();
        normalized.setSlotKey(slotKey);
        normalized.setX(x);
        normalized.setY(y);
        normalized.setWidth(width);
        normalized.setHeight(height);
        return normalized;
    }

    private void requireSignatureSlot(ReviewScoreSheetTemplateVo template, String slotKey) {
        if (template == null || template.getFooterRows() == null) {
            throw new ServiceException("评分表模板未配置评分老师签字区域");
        }
        boolean matched = template.getFooterRows().stream().anyMatch(row -> hasSignatureSlot(row == null ? null : row.getLeft(), slotKey)
            || hasSignatureSlot(row == null ? null : row.getRight(), slotKey));
        if (!matched) {
            throw new ServiceException("当前模板未配置所选的评分老师签字区域");
        }
    }

    private boolean hasSignatureSlot(ReviewScoreSheetFooterFieldVo field, String slotKey) {
        return field != null && "signature".equals(field.getType()) && slotKey.equals(field.getSlotKey());
    }

    private ReviewScoreSheetSignatureAdminVo updateManagedSignatureStatus(Long signatureId, String targetStatus, String reason) {
        ReviewScoreSheetSignature signature = requireManagedSignature(signatureId);
        String normalizedReason = normalizeSignatureAdminReason(reason);
        String currentStatus = normalizeLibraryStatus(signature.getLibraryStatus(), false);
        if (targetStatus.equals(currentStatus)) {
            return toSignatureAdminVo(signature, loadUsersForSignatureRows(List.of(signature)));
        }
        int changed = signatureMapper.update(null, Wrappers.<ReviewScoreSheetSignature>lambdaUpdate()
            .set(ReviewScoreSheetSignature::getLibraryStatus, targetStatus)
            .set(!LIBRARY_STATUS_ACTIVE.equals(targetStatus), ReviewScoreSheetSignature::getDefaultFlag, FLAG_FALSE)
            .set(ReviewScoreSheetSignature::getStatusChangedAt, new Date())
            .set(ReviewScoreSheetSignature::getStatusChangedByUserId, currentUserId())
            .set(ReviewScoreSheetSignature::getStatusReason, normalizedReason)
            .eq(ReviewScoreSheetSignature::getId, signatureId)
            .eq(ReviewScoreSheetSignature::getActiveMarker, ACTIVE_MARKER)
            .eq(ReviewScoreSheetSignature::getLibraryStatus, currentStatus));
        if (changed != 1) {
            throw new ServiceException("签名库状态已变更，请刷新后重试");
        }
        ReviewScoreSheetSignature latest = requireManagedSignature(signatureId);
        return toSignatureAdminVo(latest, loadUsersForSignatureRows(List.of(latest)));
    }

    private ReviewScoreSheetSignature requireManagedSignature(Long signatureId) {
        if (signatureId == null) {
            throw new ServiceException("手写签名不存在");
        }
        ReviewScoreSheetSignature signature = signatureMapper.selectOne(Wrappers.lambdaQuery(ReviewScoreSheetSignature.class)
            .eq(ReviewScoreSheetSignature::getId, signatureId)
            .eq(ReviewScoreSheetSignature::getActiveMarker, ACTIVE_MARKER)
            .last("limit 1"));
        if (signature == null) {
            throw new ServiceException("手写签名不存在或已删除");
        }
        return signature;
    }

    private ReviewScoreSheetSignature requireOwnedSignature(Long signatureId) {
        if (signatureId == null) {
            throw new ServiceException("请先选择手写签名");
        }
        ReviewScoreSheetSignature signature = signatureMapper.selectOne(Wrappers.lambdaQuery(ReviewScoreSheetSignature.class)
            .eq(ReviewScoreSheetSignature::getId, signatureId)
            .eq(ReviewScoreSheetSignature::getReviewerUserId, currentUserId())
            .eq(ReviewScoreSheetSignature::getActiveMarker, ACTIVE_MARKER)
            .eq(ReviewScoreSheetSignature::getLibraryStatus, LIBRARY_STATUS_ACTIVE)
            .last("limit 1"));
        if (signature == null) {
            throw new ServiceException("手写签名不存在或无权使用");
        }
        return signature;
    }

    /** Reads a sheet only for its owner or an explicitly authorized manager. */
    private ReviewScoreSheetSignedSheet requireReadableSignedSheet(Long signedSheetId) {
        return requireReadableSignedSheet(signedSheetId, false);
    }

    /** Used only inside a transaction that changes the sheet status. */
    private ReviewScoreSheetSignedSheet requireReadableSignedSheetForUpdate(Long signedSheetId) {
        return requireReadableSignedSheet(signedSheetId, true);
    }

    private ReviewScoreSheetSignedSheet requireReadableSignedSheet(Long signedSheetId, boolean forUpdate) {
        if (signedSheetId == null) {
            throw new ServiceException("签名表不存在或无权查看");
        }
        ReviewScoreSheetSignedSheet sheet = signedSheetMapper.selectOne(Wrappers.lambdaQuery(ReviewScoreSheetSignedSheet.class)
            .eq(ReviewScoreSheetSignedSheet::getId, signedSheetId)
            .last(forUpdate, "for update"));
        if (sheet == null || (!Objects.equals(sheet.getReviewerUserId(), currentUserId()) && !canManageSignedSheets())) {
            throw new ServiceException("签名表不存在或无权查看");
        }
        return sheet;
    }

    private boolean canManageSignedSheets() {
        return LoginHelper.isSuperAdmin() || StpUtil.hasPermission(PERM_SIGNED_SHEET_MANAGE);
    }

    private void assertManageSignedSheets() {
        if (!canManageSignedSheets()) {
            throw new ServiceException("无权查看签名表管理汇总");
        }
    }

    private void assertNoGeneratedResult(ReviewScoreSheetSignedSheet signedSheet) {
        Long generatedCount = reviewResultMapper.selectCount(Wrappers.lambdaQuery(ReviewResult.class)
            .eq(ReviewResult::getActivityId, signedSheet.getActivityId())
            .eq(ReviewResult::getCategoryId, signedSheet.getCategoryId()));
        if (generatedCount != null && generatedCount > 0) {
            throw new ServiceException("该活动和类别已生成或发布结果，不能撤回签名表");
        }
    }

    private boolean isWithdrawn(ReviewScoreSheetSignedSheet signedSheet) {
        return signedSheet != null && (STATUS_WITHDRAWN.equalsIgnoreCase(signedSheet.getStatus())
            || !Objects.equals(signedSheet.getActiveMarker(), ACTIVE_MARKER));
    }

    private String normalizeSignedSheetStatus(String value) {
        String status = StringUtils.trim(value);
        if (StringUtils.isBlank(status)) {
            return null;
        }
        if (STATUS_ACTIVE.equalsIgnoreCase(status)) {
            return STATUS_ACTIVE;
        }
        if (STATUS_WITHDRAWN.equalsIgnoreCase(status)) {
            return STATUS_WITHDRAWN;
        }
        throw new ServiceException("签名表状态筛选无效");
    }

    private Date parseSignedAtFilter(String value, boolean endOfDay) {
        String text = StringUtils.trim(value);
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            LocalDateTime dateTime;
            if (text.length() == 10) {
                dateTime = LocalDate.parse(text).atStartOfDay();
                if (endOfDay) {
                    dateTime = dateTime.plusDays(1).minusNanos(1);
                }
            } else {
                dateTime = LocalDateTime.parse(text, SIGNED_TIME_FORMATTER);
            }
            return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ex) {
            throw new ServiceException("签名时间格式应为 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss");
        }
    }

    private String normalizeWithdrawReason(String value) {
        String reason = StringUtils.trim(value);
        if (StringUtils.isBlank(reason)) {
            throw new ServiceException("撤回原因不能为空");
        }
        if (reason.length() > MAX_WITHDRAW_REASON_LENGTH) {
            throw new ServiceException("撤回原因不能超过500个字符");
        }
        return reason;
    }

    private String currentOperatorDisplayName() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        String displayName = loginUser == null ? null : loginUser.getNickname();
        if (StringUtils.isBlank(displayName)) {
            displayName = loginUser == null ? LoginHelper.getUsername() : loginUser.getUsername();
        }
        return StringUtils.blankToDefault(displayName, String.valueOf(currentUserId()));
    }

    private String currentOperatorRoleSnapshot() {
        Set<String> roleKeys = new TreeSet<>();
        if (LoginHelper.isSuperAdmin()) {
            roleKeys.add("superadmin");
        }
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser != null && loginUser.getRolePermission() != null) {
            loginUser.getRolePermission().stream()
                .filter(StringUtils::isNotBlank)
                .forEach(roleKeys::add);
        }
        if (roleKeys.isEmpty()) {
            return null;
        }
        String snapshot = String.join(",", roleKeys);
        return snapshot.length() <= 100 ? snapshot : snapshot.substring(0, 100);
    }

    private ReviewScoreSheetSignature findSignatureByHash(Long userId, String sha256, Long excludeId) {
        return signatureMapper.selectOne(Wrappers.lambdaQuery(ReviewScoreSheetSignature.class)
            .eq(ReviewScoreSheetSignature::getReviewerUserId, userId)
            .eq(ReviewScoreSheetSignature::getContentSha256, sha256)
            .eq(ReviewScoreSheetSignature::getActiveMarker, ACTIVE_MARKER)
            .ne(excludeId != null, ReviewScoreSheetSignature::getId, excludeId)
            .last("limit 1"));
    }

    private void clearDefaultFlags(Long userId, Long excludeId) {
        signatureMapper.update(null, Wrappers.<ReviewScoreSheetSignature>lambdaUpdate()
            .set(ReviewScoreSheetSignature::getDefaultFlag, FLAG_FALSE)
            .eq(ReviewScoreSheetSignature::getReviewerUserId, userId)
            .eq(ReviewScoreSheetSignature::getDefaultFlag, FLAG_TRUE)
            .eq(ReviewScoreSheetSignature::getActiveMarker, ACTIVE_MARKER)
            .eq(ReviewScoreSheetSignature::getLibraryStatus, LIBRARY_STATUS_ACTIVE)
            .ne(excludeId != null, ReviewScoreSheetSignature::getId, excludeId));
    }

    private List<ReviewScoreSheetSignatureAdminVo> toSignatureAdminVos(List<ReviewScoreSheetSignature> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return toSignatureAdminVos(rows, loadUsersForSignatureRows(rows));
    }

    private List<ReviewScoreSheetSignatureAdminVo> toSignatureAdminVos(List<ReviewScoreSheetSignature> rows, Map<Long, SysUser> users) {
        return rows.stream().map(row -> toSignatureAdminVo(row, users)).toList();
    }

    private Map<Long, SysUser> loadUsersForSignatureRows(List<ReviewScoreSheetSignature> rows) {
        List<Long> userIds = new ArrayList<>();
        for (ReviewScoreSheetSignature row : rows) {
            if (row == null) {
                continue;
            }
            if (row.getReviewerUserId() != null) {
                userIds.add(row.getReviewerUserId());
            }
            if (row.getStatusChangedByUserId() != null) {
                userIds.add(row.getStatusChangedByUserId());
            }
        }
        return loadUsersById(userIds);
    }

    private ReviewScoreSheetSignatureAdminVo toSignatureAdminVo(ReviewScoreSheetSignature signature, Map<Long, SysUser> users) {
        ReviewScoreSheetSignatureAdminVo vo = new ReviewScoreSheetSignatureAdminVo();
        vo.setId(signature.getId());
        vo.setReviewerUserId(signature.getReviewerUserId());
        SysUser reviewer = users.get(signature.getReviewerUserId());
        vo.setReviewerName(userDisplayName(reviewer, signature.getReviewerUserId()));
        vo.setReviewerUserName(reviewer == null ? null : reviewer.getUserName());
        vo.setSignatureName(signature.getSignatureName());
        vo.setOssId(signature.getOssId());
        String imageUrl = resolveOssUrl(signature.getOssId());
        vo.setImageUrl(imageUrl);
        vo.setUrl(imageUrl);
        vo.setDefaultSignature(FLAG_TRUE.equals(signature.getDefaultFlag()));
        vo.setLibraryStatus(normalizeLibraryStatus(signature.getLibraryStatus(), false));
        vo.setVersion(signature.getVersion());
        vo.setCreateTime(signature.getCreateTime());
        vo.setUpdateTime(signature.getUpdateTime());
        vo.setStatusChangedAt(signature.getStatusChangedAt());
        vo.setStatusChangedByUserId(signature.getStatusChangedByUserId());
        SysUser operator = users.get(signature.getStatusChangedByUserId());
        vo.setStatusChangedByName(userDisplayName(operator, signature.getStatusChangedByUserId()));
        vo.setStatusReason(signature.getStatusReason());
        return vo;
    }

    private Map<Long, SysUser> loadUsersById(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, SysUser> users = new LinkedHashMap<>();
        for (SysUser user : sysUserMapper.selectBatchIds(userIds.stream().filter(Objects::nonNull).distinct().toList())) {
            if (user != null && user.getUserId() != null) {
                users.put(user.getUserId(), user);
            }
        }
        return users;
    }

    private String userDisplayName(SysUser user, Long fallbackUserId) {
        if (user == null) {
            return fallbackUserId == null ? "-" : String.valueOf(fallbackUserId);
        }
        return StringUtils.blankToDefault(user.getNickName(), StringUtils.blankToDefault(user.getUserName(), String.valueOf(user.getUserId())));
    }

    private String normalizeLibraryStatus(String value, boolean emptyAsNull) {
        String status = StringUtils.trim(value);
        if (StringUtils.isBlank(status)) {
            return emptyAsNull ? null : LIBRARY_STATUS_ACTIVE;
        }
        if (LIBRARY_STATUS_ACTIVE.equalsIgnoreCase(status)) {
            return LIBRARY_STATUS_ACTIVE;
        }
        if (LIBRARY_STATUS_DISABLED.equalsIgnoreCase(status)) {
            return LIBRARY_STATUS_DISABLED;
        }
        if (LIBRARY_STATUS_ARCHIVED.equalsIgnoreCase(status)) {
            return LIBRARY_STATUS_ARCHIVED;
        }
        throw new ServiceException("签名库状态筛选无效");
    }

    private String normalizeSignatureAdminReason(String value) {
        String reason = StringUtils.trim(value);
        if (StringUtils.isBlank(reason)) {
            throw new ServiceException("签名库操作原因不能为空");
        }
        if (reason.length() > MAX_WITHDRAW_REASON_LENGTH) {
            throw new ServiceException("签名库操作原因不能超过500个字符");
        }
        return reason;
    }

    private ReviewScoreSheetSignatureVo toSignatureVo(ReviewScoreSheetSignature signature) {
        ReviewScoreSheetSignatureVo vo = new ReviewScoreSheetSignatureVo();
        vo.setId(signature.getId());
        vo.setSignatureName(signature.getSignatureName());
        vo.setOssId(signature.getOssId());
        String imageUrl = resolveOssUrl(signature.getOssId());
        vo.setImageUrl(imageUrl);
        vo.setUrl(imageUrl);
        vo.setReused(false);
        vo.setDefaultSignature(FLAG_TRUE.equals(signature.getDefaultFlag()));
        vo.setVersion(signature.getVersion());
        vo.setCreateTime(signature.getCreateTime());
        vo.setUpdateTime(signature.getUpdateTime());
        return vo;
    }

    private SignatureUpload validateSignatureImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请上传手写签名 PNG 图片");
        }
        if (file.getSize() > MAX_SIGNATURE_SIZE) {
            throw new ServiceException("手写签名图片不能超过 2MB");
        }
        String originalName = StringUtils.blankToDefault(file.getOriginalFilename(), "").toLowerCase();
        if (!originalName.endsWith(".png")) {
            throw new ServiceException("手写签名仅支持 PNG 图片");
        }
        try {
            byte[] bytes = file.getBytes();
            if (!isPng(bytes)) {
                throw new ServiceException("手写签名图片格式无效");
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0
                || image.getWidth() > MAX_SIGNATURE_DIMENSION || image.getHeight() > MAX_SIGNATURE_DIMENSION) {
                throw new ServiceException("手写签名图片尺寸无效");
            }
            return new SignatureUpload(sha256(bytes));
        } catch (ServiceException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ServiceException("读取手写签名图片失败");
        }
    }

    private byte[] loadSignaturePng(Long ossId) {
        SysOssVo oss = ossId == null ? null : ossService.getById(ossId);
        if (oss == null || StringUtils.isBlank(oss.getFileName()) || StringUtils.isBlank(oss.getService())) {
            throw new ServiceException("签名图片快照不存在，无法导出");
        }
        Path tempFile = null;
        try {
            tempFile = OssFactory.instance(oss.getService()).fileDownload(oss.getFileName());
            if (tempFile == null) {
                throw new ServiceException("签名图片快照不存在，无法导出");
            }
            byte[] bytes = Files.readAllBytes(tempFile);
            if (!isPng(bytes)) {
                throw new ServiceException("签名图片快照格式无效，无法导出");
            }
            return bytes;
        } catch (IOException ex) {
            throw new ServiceException("读取签名图片快照失败，无法导出");
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {
                    // Temp cleanup is best effort; the OSS client owns the source file.
                }
            }
        }
    }

    private String resolveOssUrl(Long ossId) {
        if (ossId == null) {
            return null;
        }
        // listByIds applies the shared storage access policy. In particular,
        // private OSS objects receive a short-lived presigned GET URL, while
        // local/public storage keeps its normal address.
        return ossService.listByIds(List.of(ossId)).stream()
            .findFirst()
            .map(SysOssVo::getUrl)
            .orElse(null);
    }

    private void assertScope(Long activityId, Long categoryId) {
        if (activityId == null || categoryId == null) {
            throw new ServiceException("请先选择具体活动和类别");
        }
    }

    private Long currentUserId() {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("当前登录用户不存在");
        }
        return userId;
    }

    private String normalizeSignatureName(String value) {
        String name = StringUtils.trim(value);
        if (StringUtils.isBlank(name)) {
            name = "手写签名 " + formatSignedTime(new Date());
        }
        if (name.length() > 100) {
            throw new ServiceException("签名名称不能超过100个字符");
        }
        return name;
    }

    private String safeFilename(String value) {
        String name = StringUtils.blankToDefault(value, "当前类别");
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String formatSignedTime(Date signedAt) {
        if (signedAt == null) {
            return null;
        }
        return signedAt.toInstant().atZone(ZoneId.systemDefault()).format(SIGNED_TIME_FORMATTER);
    }

    private boolean isPng(byte[] bytes) {
        return bytes != null && bytes.length >= 8
            && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
            && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
    }

    private String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new ServiceException("无法校验手写签名图片");
        }
    }

    private record SignatureUpload(String contentSha256) {
    }
}
