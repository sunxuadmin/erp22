package org.dromara.crehn.review.sheet;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.bo.ReviewScoreSheetExportBo;
import org.dromara.crehn.domain.bo.ReviewScoreSheetSignatureAdminQueryBo;
import org.dromara.crehn.domain.bo.ReviewScoreSheetSignatureAdminStatusBo;
import org.dromara.crehn.domain.bo.ReviewScoreSheetSignedSheetCreateBo;
import org.dromara.crehn.domain.bo.ReviewScoreSheetSignedSheetQueryBo;
import org.dromara.crehn.domain.bo.ReviewScoreSheetSignedSheetWithdrawBo;
import org.dromara.crehn.domain.bo.ReviewScoreSheetTemplateBo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetExportPreviewVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetReviewerOptionVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetSignatureVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetSignatureAdminVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetSignedSheetVo;
import org.dromara.crehn.domain.vo.ReviewScoreSheetTemplateVo;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/review-score-sheet")
public class ReviewScoreSheetController {

    private final ReviewScoreSheetService scoreSheetService;
    private final ReviewScoreSheetSigningService signingService;

    @SaCheckPermission("crehn:reviewSheetTemplate:list")
    @GetMapping("/templates")
    public R<List<ReviewScoreSheetTemplateVo>> templates() {
        return R.ok(scoreSheetService.listTemplates());
    }

    @SaCheckPermission("crehn:reviewSheetTemplate:list")
    @GetMapping("/template")
    public R<ReviewScoreSheetTemplateVo> template() {
        return R.ok(scoreSheetService.getTemplate());
    }

    @SaCheckPermission("crehn:reviewSheetTemplate:list")
    @GetMapping("/template/{templateId}")
    public R<ReviewScoreSheetTemplateVo> template(@PathVariable Long templateId) {
        return R.ok(scoreSheetService.getTemplate(templateId));
    }

    @SaCheckPermission("crehn:reviewSheetTemplate:edit")
    @Log(title = "评审打分表模板新增", businessType = BusinessType.INSERT)
    @PostMapping("/template")
    public R<ReviewScoreSheetTemplateVo> createTemplate(@RequestBody ReviewScoreSheetTemplateBo bo) {
        return R.ok(scoreSheetService.createTemplate(bo));
    }

    @SaCheckPermission("crehn:reviewSheetTemplate:edit")
    @Log(title = "评审打分表模板修改", businessType = BusinessType.UPDATE)
    @PutMapping("/template")
    public R<ReviewScoreSheetTemplateVo> saveTemplate(@RequestBody ReviewScoreSheetTemplateBo bo) {
        return R.ok(scoreSheetService.saveTemplate(bo));
    }

    @SaCheckPermission("crehn:reviewSheetTemplate:edit")
    @Log(title = "评审打分表模板修改", businessType = BusinessType.UPDATE)
    @PutMapping("/template/{templateId}")
    public R<ReviewScoreSheetTemplateVo> updateTemplate(@PathVariable Long templateId,
                                                         @RequestBody ReviewScoreSheetTemplateBo bo) {
        return R.ok(scoreSheetService.updateTemplate(templateId, bo));
    }

    @SaCheckPermission("crehn:reviewSheetTemplate:edit")
    @Log(title = "评审打分表模板设为默认", businessType = BusinessType.UPDATE)
    @PostMapping("/template/{templateId}/default")
    public R<ReviewScoreSheetTemplateVo> setDefaultTemplate(@PathVariable Long templateId) {
        return R.ok(scoreSheetService.setDefaultTemplate(templateId));
    }

    @SaCheckPermission("crehn:reviewSheetTemplate:edit")
    @Log(title = "评审打分表模板删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/template/{templateId}")
    public R<Void> deleteTemplate(@PathVariable Long templateId) {
        scoreSheetService.deleteTemplate(templateId);
        return R.ok();
    }

    @SaCheckPermission("crehn:reviewSheetTemplate:edit")
    @Log(title = "评审打分表模板恢复默认", businessType = BusinessType.UPDATE)
    @PostMapping("/template/reset")
    public R<ReviewScoreSheetTemplateVo> resetTemplate(@RequestParam Long version) {
        return R.ok(scoreSheetService.resetTemplate(version));
    }

    @SaCheckPermission("crehn:reviewSheetTemplate:edit")
    @Log(title = "评审打分表模板恢复默认", businessType = BusinessType.UPDATE)
    @PostMapping("/template/{templateId}/reset")
    public R<ReviewScoreSheetTemplateVo> resetTemplate(@PathVariable Long templateId, @RequestParam Long version) {
        return R.ok(scoreSheetService.resetTemplate(templateId, version));
    }

    @SaCheckPermission("crehn:reviewSheet:export")
    @PostMapping("/export/preview")
    public R<ReviewScoreSheetExportPreviewVo> exportPreview(@RequestBody ReviewScoreSheetExportBo bo) {
        return R.ok(scoreSheetService.previewSubmittedScores(bo));
    }

    @SaCheckPermission("crehn:reviewSheet:export")
    @Log(title = "评审打分表导出", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@RequestBody(required = false) ReviewScoreSheetExportBo bo,
                       @RequestParam(required = false) Long activityId,
                       @RequestParam(required = false) Long categoryId,
                       HttpServletResponse response) {
        if (bo == null) {
            bo = new ReviewScoreSheetExportBo();
            bo.setActivityId(activityId);
            bo.setCategoryId(categoryId);
        }
        scoreSheetService.exportSubmittedScores(bo, response);
    }

    @SaCheckPermission("crehn:reviewSheet:export")
    @GetMapping("/signatures")
    public R<List<ReviewScoreSheetSignatureVo>> signatures() {
        return R.ok(signingService.listSignatures());
    }

    @SaCheckPermission("crehn:reviewSheet:export")
    @Log(title = "评审手写签名新增", businessType = BusinessType.INSERT)
    @PostMapping(value = "/signatures", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ReviewScoreSheetSignatureVo> createSignature(@RequestPart("file") MultipartFile file,
                                                           @RequestParam(required = false) String signatureName,
                                                           @RequestParam(required = false) Boolean defaultSignature) {
        return R.ok(signingService.createSignature(file, signatureName, defaultSignature));
    }

    @SaCheckPermission("crehn:reviewSheet:export")
    @Log(title = "评审手写签名更换", businessType = BusinessType.UPDATE)
    @PostMapping(value = "/signatures/{signatureId}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ReviewScoreSheetSignatureVo> replaceSignature(@PathVariable Long signatureId,
                                                            @RequestPart("file") MultipartFile file,
                                                            @RequestParam(required = false) String signatureName) {
        return R.ok(signingService.replaceSignature(signatureId, file, signatureName));
    }

    @SaCheckPermission("crehn:reviewSheet:export")
    @Log(title = "评审手写签名设为默认", businessType = BusinessType.UPDATE)
    @PostMapping("/signatures/{signatureId}/default")
    public R<ReviewScoreSheetSignatureVo> setDefaultSignature(@PathVariable Long signatureId) {
        return R.ok(signingService.setDefaultSignature(signatureId));
    }

    @SaCheckPermission("crehn:reviewSheet:export")
    @Log(title = "评审手写签名删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/signatures/{signatureId}")
    public R<Void> deleteSignature(@PathVariable Long signatureId) {
        signingService.deleteSignature(signatureId);
        return R.ok();
    }

    @SaCheckPermission("crehn:reviewSheet:signatureManage")
    @GetMapping("/signature-admin/signatures/page")
    public TableDataInfo<ReviewScoreSheetSignatureAdminVo> signatureAdminPage(ReviewScoreSheetSignatureAdminQueryBo query,
                                                                                PageQuery pageQuery) {
        return signingService.querySignatureAdminPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:reviewSheet:signatureManage")
    @GetMapping("/signature-admin/signatures/{signatureId}")
    public R<ReviewScoreSheetSignatureAdminVo> signatureAdminDetail(@PathVariable Long signatureId) {
        return R.ok(signingService.getSignatureAdmin(signatureId));
    }

    @SaCheckPermission("crehn:reviewSheet:signatureManage:edit")
    @Log(title = "评审手写签名库停用", businessType = BusinessType.UPDATE)
    @PostMapping("/signature-admin/signatures/{signatureId}/disable")
    public R<ReviewScoreSheetSignatureAdminVo> disableSignatureAdmin(@PathVariable Long signatureId,
                                                                       @RequestBody ReviewScoreSheetSignatureAdminStatusBo bo) {
        return R.ok(signingService.disableSignatureAdmin(signatureId, bo));
    }

    @SaCheckPermission("crehn:reviewSheet:signatureManage:edit")
    @Log(title = "评审手写签名库恢复", businessType = BusinessType.UPDATE)
    @PostMapping("/signature-admin/signatures/{signatureId}/restore")
    public R<ReviewScoreSheetSignatureAdminVo> restoreSignatureAdmin(@PathVariable Long signatureId,
                                                                       @RequestBody ReviewScoreSheetSignatureAdminStatusBo bo) {
        return R.ok(signingService.restoreSignatureAdmin(signatureId, bo));
    }

    @SaCheckPermission("crehn:reviewSheet:signatureManage:archive")
    @Log(title = "评审手写签名库归档", businessType = BusinessType.UPDATE)
    @PostMapping("/signature-admin/signatures/{signatureId}/archive")
    public R<ReviewScoreSheetSignatureAdminVo> archiveSignatureAdmin(@PathVariable Long signatureId,
                                                                       @RequestBody ReviewScoreSheetSignatureAdminStatusBo bo) {
        return R.ok(signingService.archiveSignatureAdmin(signatureId, bo));
    }

    @SaCheckPermission("crehn:reviewSheet:export")
    @PostMapping({"/signed-sheets/preview", "/signed-sheet/preview"})
    public R<ReviewScoreSheetExportPreviewVo> signedSheetPreview(@RequestBody ReviewScoreSheetExportBo bo) {
        return R.ok(signingService.previewUnsignedSubmittedScores(bo));
    }

    @SaCheckPermission("crehn:reviewSheet:export")
    @Log(title = "评审签名表保存", businessType = BusinessType.INSERT)
    @PostMapping({"/signed-sheets", "/signed-sheet"})
    public R<ReviewScoreSheetSignedSheetVo> createSignedSheet(@RequestBody ReviewScoreSheetSignedSheetCreateBo bo) {
        return R.ok(signingService.createSignedSheet(bo));
    }

    @SaCheckPermission("crehn:reviewSheet:export")
    @GetMapping("/signed-sheets")
    public R<List<ReviewScoreSheetSignedSheetVo>> signedSheets(@RequestParam(required = false) Long activityId,
                                                                @RequestParam(required = false) Long categoryId) {
        return R.ok(signingService.listSignedSheets(activityId, categoryId));
    }

    /**
     * Paginated history for both roles. The service forces reviewers to their
     * own rows and only allows crehn_admin/superadmin to read the global summary.
     */
    @SaCheckPermission(value = {"crehn:reviewSheet:export", "crehn:reviewSheet:manage"}, mode = SaMode.OR)
    @GetMapping("/signed-sheets/page")
    public TableDataInfo<ReviewScoreSheetSignedSheetVo> signedSheetPage(ReviewScoreSheetSignedSheetQueryBo query,
                                                                          PageQuery pageQuery) {
        return signingService.querySignedSheetPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:reviewSheet:manage")
    @GetMapping("/signed-sheets/reviewer-options")
    public R<List<ReviewScoreSheetReviewerOptionVo>> signedSheetReviewerOptions(@RequestParam(required = false) Long activityId,
                                                                                   @RequestParam(required = false) Long categoryId,
                                                                                   @RequestParam(required = false) String keyword) {
        return R.ok(signingService.listSignedSheetReviewerOptions(activityId, categoryId, keyword));
    }

    @SaCheckPermission(value = {"crehn:reviewSheet:export", "crehn:reviewSheet:manage"}, mode = SaMode.OR)
    @GetMapping("/signed-sheets/{signedSheetId}")
    public R<ReviewScoreSheetSignedSheetVo> signedSheet(@PathVariable Long signedSheetId) {
        return R.ok(signingService.getSignedSheet(signedSheetId));
    }

    @SaCheckPermission(value = {"crehn:reviewSheet:export", "crehn:reviewSheet:manage"}, mode = SaMode.OR)
    @Log(title = "已签名评审打分表导出", businessType = BusinessType.EXPORT)
    @GetMapping("/signed-sheets/{signedSheetId}/export")
    public void exportSignedSheet(@PathVariable Long signedSheetId, HttpServletResponse response) {
        signingService.exportSignedSheet(signedSheetId, response);
    }

    @SaCheckPermission(value = {"crehn:reviewSheet:export", "crehn:reviewSheet:withdraw"}, mode = SaMode.OR)
    @Log(title = "评审签名表撤回", businessType = BusinessType.UPDATE)
    @PostMapping("/signed-sheets/{signedSheetId}/withdraw")
    public R<ReviewScoreSheetSignedSheetVo> withdrawSignedSheet(@PathVariable Long signedSheetId,
                                                                  @RequestBody ReviewScoreSheetSignedSheetWithdrawBo bo) {
        return R.ok(signingService.withdrawSignedSheet(signedSheetId, bo));
    }
}
