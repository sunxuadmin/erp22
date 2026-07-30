package org.dromara.crehn.school.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.domain.SchoolSubmissionBatch;
import org.dromara.crehn.domain.bo.SchoolFinalSubmitBo;
import org.dromara.crehn.domain.bo.SchoolProjectReviewBo;
import org.dromara.crehn.school.service.SchoolSubmissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/crehn/school-submission")
public class SchoolSubmissionController {
    private final SchoolSubmissionService service;

    @SaCheckPermission("crehn:schoolSubmission:list")
    @GetMapping("/pending")
    public R<List<Project>> pending(@RequestParam(required = false) Long activityId) {
        return R.ok(service.pending(activityId));
    }

    @SaCheckPermission("crehn:schoolSubmission:review")
    @Log(title = "学校审核参赛作品", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/review")
    public R<Void> review(@Valid @RequestBody SchoolProjectReviewBo bo) {
        service.review(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:schoolSubmission:submit")
    @Log(title = "学校最终提交", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/final-submit")
    public R<SchoolSubmissionBatch> finalSubmit(@Valid @RequestBody SchoolFinalSubmitBo bo) {
        return R.ok(service.finalSubmit(bo));
    }
}
