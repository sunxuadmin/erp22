package org.dromara.crehn.registration.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.RegistrationCode;
import org.dromara.crehn.domain.RegistrationCodeUsage;
import org.dromara.crehn.domain.bo.RegistrationCodeBatchBo;
import org.dromara.crehn.domain.vo.RegistrationCodeUsageVo;
import org.dromara.crehn.domain.vo.RegistrationCodeVo;
import org.dromara.crehn.mapper.RegistrationCodeMapper;
import org.dromara.crehn.mapper.RegistrationCodeUsageMapper;
import org.dromara.crehn.registration.service.IArtRegistrationCodeService;
import org.dromara.common.core.domain.model.RegisterBody;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class ArtRegistrationCodeServiceImpl implements IArtRegistrationCodeService {

    private static final Set<String> CODE_TYPES = Set.of("school", "expert", "admin");
    private static final Set<String> ACTIVE_STATUS = Set.of("unused", "active");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RegistrationCodeMapper registrationCodeMapper;
    private final RegistrationCodeUsageMapper registrationCodeUsageMapper;

    @Override
    public TableDataInfo<RegistrationCodeVo> queryPage(RegistrationCode query, PageQuery pageQuery) {
        LambdaQueryWrapper<RegistrationCode> lqw = buildQuery(query);
        Page<RegistrationCodeVo> page = registrationCodeMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public List<RegistrationCodeVo> queryList(RegistrationCode query) {
        return registrationCodeMapper.selectVoList(buildQuery(query));
    }

    @Override
    public RegistrationCodeVo getInfo(Long id) {
        return registrationCodeMapper.selectVoById(id);
    }

    @Override
    public RegistrationCodeVo generate(RegistrationCode bo) {
        validateCodeType(bo.getCodeType());
        bo.setCode(nextCode(bo.getCodeType()));
        bo.setStatus("unused");
        bo.setAutoApprove(Boolean.TRUE.equals(bo.getAutoApprove()));
        bo.setUsedCount(0);
        bo.setMaxUseCount(bo.getMaxUseCount() == null || bo.getMaxUseCount() <= 0 ? 1 : bo.getMaxUseCount());
        registrationCodeMapper.insert(bo);
        return registrationCodeMapper.selectVoById(bo.getId());
    }

    @Override
    public List<RegistrationCodeVo> generateBatch(RegistrationCodeBatchBo bo) {
        if (bo.getRows() == null || bo.getRows().isEmpty()) {
            throw new ServiceException("批量注册码行不能为空");
        }
        String codeType = StringUtils.isBlank(bo.getCodeType()) ? "school" : bo.getCodeType();
        validateCodeType(codeType);
        return bo.getRows().stream()
            .map(row -> {
                row.setCodeType(StringUtils.isBlank(row.getCodeType()) ? codeType : row.getCodeType());
                row.setActivityId(row.getActivityId() == null ? bo.getActivityId() : row.getActivityId());
                row.setRoleKey(StringUtils.isBlank(row.getRoleKey()) ? bo.getRoleKey() : row.getRoleKey());
                row.setAutoApprove(row.getAutoApprove() == null ? bo.getAutoApprove() : row.getAutoApprove());
                row.setMaxUseCount(row.getMaxUseCount() == null ? bo.getMaxUseCount() : row.getMaxUseCount());
                row.setExpireAt(row.getExpireAt() == null ? bo.getExpireAt() : row.getExpireAt());
                row.setRemark(StringUtils.isBlank(row.getRemark()) ? bo.getRemark() : row.getRemark());
                return generate(row);
            })
            .toList();
    }

    @Override
    public void updateBinding(RegistrationCode bo) {
        RegistrationCode existing = requireCode(bo.getId());
        existing.setActivityId(bo.getActivityId());
        existing.setSchoolId(bo.getSchoolId());
        existing.setBoundPhone(bo.getBoundPhone());
        existing.setRoleKey(bo.getRoleKey());
        existing.setAutoApprove(bo.getAutoApprove());
        existing.setMaxUseCount(bo.getMaxUseCount());
        existing.setExpireAt(bo.getExpireAt());
        existing.setRemark(bo.getRemark());
        registrationCodeMapper.updateById(existing);
    }

    @Override
    public void voidCode(Long id) {
        RegistrationCode code = requireCode(id);
        code.setStatus("voided");
        registrationCodeMapper.updateById(code);
    }

    @Override
    public RegistrationCodeVo resend(Long id) {
        RegistrationCode code = requireCode(id);
        code.setCode(nextCode(code.getCodeType()));
        code.setStatus("unused");
        code.setUsedCount(0);
        registrationCodeMapper.updateById(code);
        return registrationCodeMapper.selectVoById(id);
    }

    @Override
    public List<RegistrationCodeUsageVo> listUsage(Long codeId) {
        return registrationCodeUsageMapper.selectVoList(Wrappers.lambdaQuery(RegistrationCodeUsage.class)
            .eq(RegistrationCodeUsage::getCodeId, codeId)
            .orderByDesc(RegistrationCodeUsage::getUsedAt));
    }

    @Override
    public boolean isCodeRegisterRequest(RegisterBody body) {
        return body != null
            && ("school".equals(body.getUserType()) || "expert".equals(body.getUserType()))
            && StringUtils.isNotBlank(body.getRegistrationCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegistrationCode validateForRegister(RegisterBody body) {
        if ("sys_user".equals(body.getUserType()) || "admin".equals(body.getUserType())) {
            throw new ServiceException("管理员账号不能从公开页面注册");
        }
        if (!"school".equals(body.getUserType()) && !"expert".equals(body.getUserType())) {
            throw new ServiceException("只允许学校账号或专家账号注册");
        }
        if (StringUtils.isBlank(body.getRegistrationCode())) {
            throw new ServiceException("注册码不能为空");
        }
        RegistrationCode code = registrationCodeMapper.selectOne(Wrappers.lambdaQuery(RegistrationCode.class)
            .eq(RegistrationCode::getCode, body.getRegistrationCode())
            .last("for update"));
        if (code == null) {
            throw new ServiceException("注册码不存在");
        }
        if (!body.getUserType().equals(code.getCodeType())) {
            throw new ServiceException("注册码类型与账号类型不匹配");
        }
        if (!ACTIVE_STATUS.contains(code.getStatus())) {
            throw new ServiceException("注册码未启用");
        }
        if (code.getExpireAt() != null && code.getExpireAt().before(new Date())) {
            throw new ServiceException("注册码已过期");
        }
        int usedCount = code.getUsedCount() == null ? 0 : code.getUsedCount();
        int maxUseCount = code.getMaxUseCount() == null ? 1 : code.getMaxUseCount();
        if (usedCount >= maxUseCount) {
            throw new ServiceException("注册码使用次数已达上限");
        }
        if (StringUtils.isNotBlank(code.getBoundPhone()) && !code.getBoundPhone().equals(body.getPhonenumber())) {
            throw new ServiceException("注册手机号与注册码绑定手机号不一致");
        }
        if (code.getSchoolId() != null && body.getSchoolId() != null && !code.getSchoolId().equals(body.getSchoolId())) {
            throw new ServiceException("注册码已绑定其他学校");
        }
        return code;
    }

    @Override
    public void markUsed(RegistrationCode code, Long userId, String username, HttpServletRequest request) {
        code.setUsedCount((code.getUsedCount() == null ? 0 : code.getUsedCount()) + 1);
        if (code.getUsedCount() >= (code.getMaxUseCount() == null ? 1 : code.getMaxUseCount())) {
            code.setStatus("used");
        }
        registrationCodeMapper.updateById(code);

        RegistrationCodeUsage usage = new RegistrationCodeUsage();
        usage.setCodeId(code.getId());
        usage.setUserId(userId);
        usage.setUsedAt(new Date());
        usage.setBoundAccount(username);
        if (request != null) {
            usage.setIpAddress(request.getRemoteAddr());
            usage.setUserAgent(StringUtils.substring(request.getHeader("User-Agent"), 0, 512));
        }
        registrationCodeUsageMapper.insert(usage);
    }

    private LambdaQueryWrapper<RegistrationCode> buildQuery(RegistrationCode query) {
        return Wrappers.lambdaQuery(RegistrationCode.class)
            .like(StringUtils.isNotBlank(query.getCode()), RegistrationCode::getCode, query.getCode())
            .eq(StringUtils.isNotBlank(query.getCodeType()), RegistrationCode::getCodeType, query.getCodeType())
            .eq(StringUtils.isNotBlank(query.getStatus()), RegistrationCode::getStatus, query.getStatus())
            .eq(query.getActivityId() != null, RegistrationCode::getActivityId, query.getActivityId())
            .eq(query.getSchoolId() != null, RegistrationCode::getSchoolId, query.getSchoolId())
            .orderByDesc(RegistrationCode::getCreateTime);
    }

    private RegistrationCode requireCode(Long id) {
        RegistrationCode code = registrationCodeMapper.selectById(id);
        if (code == null) {
            throw new ServiceException("注册码不存在");
        }
        return code;
    }

    private void validateCodeType(String codeType) {
        if (!CODE_TYPES.contains(codeType)) {
            throw new ServiceException("注册码类型不正确");
        }
    }

    private String nextCode(String codeType) {
        String prefix = switch (codeType) {
            case "school" -> "SCH";
            case "expert" -> "EXP";
            default -> "ADM";
        };
        return prefix + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + randomText(8);
    }

    private String randomText(int len) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
