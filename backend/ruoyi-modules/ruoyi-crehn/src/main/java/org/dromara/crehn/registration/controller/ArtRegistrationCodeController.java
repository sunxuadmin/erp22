package org.dromara.crehn.registration.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.RegistrationCode;
import org.dromara.crehn.domain.bo.RegistrationCodeBatchBo;
import org.dromara.crehn.domain.vo.RegistrationCodeUsageVo;
import org.dromara.crehn.domain.vo.RegistrationCodeVo;
import org.dromara.crehn.registration.service.IArtRegistrationCodeService;
import org.dromara.common.core.domain.R;
import org.dromara.common.excel.utils.ExcelUtil;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/registration-code")
public class ArtRegistrationCodeController extends BaseController {

    private final IArtRegistrationCodeService registrationCodeService;

    @SaCheckPermission("crehn:registration:list")
    @GetMapping("/list")
    public TableDataInfo<RegistrationCodeVo> list(RegistrationCode query, PageQuery pageQuery) {
        return registrationCodeService.queryPage(query, pageQuery);
    }

    @SaCheckPermission("crehn:registration:query")
    @GetMapping("/{id}")
    public R<RegistrationCodeVo> getInfo(@PathVariable Long id) {
        return R.ok(registrationCodeService.getInfo(id));
    }

    @SaCheckPermission("crehn:registration:add")
    @Log(title = "art registration code generate", businessType = BusinessType.INSERT)
    @PostMapping
    public R<RegistrationCodeVo> generate(@RequestBody RegistrationCode bo) {
        return R.ok(registrationCodeService.generate(bo));
    }

    @SaCheckPermission("crehn:registration:add")
    @Log(title = "art registration code batch generate", businessType = BusinessType.INSERT)
    @PostMapping("/batch")
    public R<List<RegistrationCodeVo>> generateBatch(@RequestBody RegistrationCodeBatchBo bo) {
        return R.ok(registrationCodeService.generateBatch(bo));
    }

    @SaCheckPermission("crehn:registration:edit")
    @Log(title = "art registration code binding", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody RegistrationCode bo) {
        registrationCodeService.updateBinding(bo);
        return R.ok();
    }

    @SaCheckPermission("crehn:registration:void")
    @Log(title = "art registration code void", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/void")
    public R<Void> voidCode(@PathVariable Long id) {
        registrationCodeService.voidCode(id);
        return R.ok();
    }

    @SaCheckPermission("crehn:registration:resend")
    @Log(title = "art registration code resend", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/resend")
    public R<RegistrationCodeVo> resend(@PathVariable Long id) {
        return R.ok(registrationCodeService.resend(id));
    }

    @SaCheckPermission("crehn:registration:usage")
    @GetMapping("/{id}/usage")
    public R<List<RegistrationCodeUsageVo>> usage(@PathVariable Long id) {
        return R.ok(registrationCodeService.listUsage(id));
    }

    @SaCheckPermission("crehn:registration:export")
    @Log(title = "art registration code export", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(RegistrationCode query, HttpServletResponse response) {
        ExcelUtil.exportExcel(registrationCodeService.queryList(query), "注册码", RegistrationCodeVo.class, response);
    }
}
