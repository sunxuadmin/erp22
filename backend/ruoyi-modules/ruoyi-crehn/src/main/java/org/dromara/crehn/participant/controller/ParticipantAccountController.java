package org.dromara.crehn.participant.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.ratelimiter.enums.LimitType;
import org.dromara.crehn.domain.ParticipantProfile;
import org.dromara.crehn.domain.bo.ParticipantActivateBo;
import org.dromara.crehn.domain.bo.ParticipantActivationPreviewBo;
import org.dromara.crehn.domain.bo.ParticipantBatchImportBo;
import org.dromara.crehn.domain.bo.ParticipantCodeReissueBo;
import org.dromara.crehn.domain.bo.ParticipantImportRow;
import org.dromara.crehn.domain.vo.ParticipantActivationIssueVo;
import org.dromara.crehn.domain.vo.ParticipantActivationPreviewVo;
import org.dromara.crehn.participant.service.IParticipantAccountService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crehn/participant-account")
public class ParticipantAccountController {
    private final IParticipantAccountService participantAccountService;

    @SaCheckPermission("crehn:participant:list")
    @GetMapping("/list")
    public TableDataInfo<ParticipantProfile> list(ParticipantProfile query, PageQuery pageQuery) {
        return participantAccountService.queryPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:participant:import")
    @Log(title = "参赛者名单导入", businessType = BusinessType.IMPORT)
    @RepeatSubmit
    @PostMapping("/batch")
    public R<List<ParticipantActivationIssueVo>> batch(@RequestBody ParticipantBatchImportBo bo) {
        return R.ok(participantAccountService.importParticipants(bo));
    }

    @SaCheckPermission("crehn:participant:import")
    @Log(title = "参赛者名单Excel导入", businessType = BusinessType.IMPORT)
    @RepeatSubmit
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<List<ParticipantActivationIssueVo>> importExcel(
        @RequestPart("file") MultipartFile file,
        @RequestParam Long activityId,
        @RequestParam(required = false) Long schoolId,
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date expiresAt,
        @RequestParam(required = false) String remark
    ) throws Exception {
        ParticipantBatchImportBo bo = new ParticipantBatchImportBo();
        bo.setActivityId(activityId);
        bo.setSchoolId(schoolId);
        bo.setExpiresAt(expiresAt);
        bo.setRemark(remark);
        bo.setRows(ExcelUtil.importExcel(file.getInputStream(), ParticipantImportRow.class));
        return R.ok(participantAccountService.importParticipants(bo));
    }

    @SaCheckPermission("crehn:participant:import")
    @PostMapping("/template")
    public void template(HttpServletResponse response) {
        ExcelUtil.exportExcel(new ArrayList<>(), "参赛者名单导入模板", ParticipantImportRow.class, response);
    }

    @SaIgnore
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @RepeatSubmit(interval = 1000)
    @PostMapping("/activation-preview")
    public R<ParticipantActivationPreviewVo> previewActivation(@Valid @RequestBody ParticipantActivationPreviewBo bo) {
        return R.ok(participantAccountService.previewActivation(bo));
    }

    @SaIgnore
    @RateLimiter(time = 60, count = 10, limitType = LimitType.IP)
    @RepeatSubmit(interval = 3000)
    @PostMapping("/activate")
    public R<Void> activate(@Valid @RequestBody ParticipantActivateBo bo, HttpServletRequest request) {
        participantAccountService.activate(bo, request);
        return R.ok();
    }

    @SaCheckPermission("crehn:participant:reissue")
    @Log(title = "参赛者激活码重新签发", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/reissue")
    public R<ParticipantActivationIssueVo> reissue(
        @Valid @RequestBody ParticipantCodeReissueBo bo,
        HttpServletRequest request
    ) {
        return R.ok(participantAccountService.reissue(bo, request));
    }
}
