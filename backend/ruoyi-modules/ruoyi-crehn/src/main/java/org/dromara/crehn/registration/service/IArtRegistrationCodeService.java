package org.dromara.crehn.registration.service;

import jakarta.servlet.http.HttpServletRequest;
import org.dromara.crehn.domain.RegistrationCode;
import org.dromara.crehn.domain.bo.RegistrationCodeBatchBo;
import org.dromara.crehn.domain.vo.RegistrationCodeUsageVo;
import org.dromara.crehn.domain.vo.RegistrationCodeVo;
import org.dromara.common.core.domain.model.RegisterBody;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

public interface IArtRegistrationCodeService {
    TableDataInfo<RegistrationCodeVo> queryPage(RegistrationCode query, PageQuery pageQuery);

    List<RegistrationCodeVo> queryList(RegistrationCode query);

    RegistrationCodeVo getInfo(Long id);

    RegistrationCodeVo generate(RegistrationCode bo);

    List<RegistrationCodeVo> generateBatch(RegistrationCodeBatchBo bo);

    void updateBinding(RegistrationCode bo);

    void voidCode(Long id);

    RegistrationCodeVo resend(Long id);

    List<RegistrationCodeUsageVo> listUsage(Long codeId);

    boolean isCodeRegisterRequest(RegisterBody body);

    RegistrationCode validateForRegister(RegisterBody body);

    void markUsed(RegistrationCode code, Long userId, String username, HttpServletRequest request);
}
