package org.dromara.web.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.RegistrationCode;
import org.dromara.crehn.config.service.IArtActivityScopeService;
import org.dromara.crehn.config.service.IArtSchoolInfoService;
import org.dromara.crehn.registration.service.IArtRegistrationCodeService;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.constant.SystemConstants;
import org.dromara.common.core.domain.model.RegisterBody;
import org.dromara.common.core.enums.UserType;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.exception.user.CaptchaException;
import org.dromara.common.core.exception.user.CaptchaExpireException;
import org.dromara.common.core.exception.user.UserException;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.log.event.LogininforEvent;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.common.web.config.properties.CaptchaProperties;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.SysUserRole;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.mapper.SysUserRoleMapper;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * 注册校验方法
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysRegisterService {

    private final ISysUserService userService;
    private final SysUserMapper userMapper;
    private final CaptchaProperties captchaProperties;
    private final IArtRegistrationCodeService registrationCodeService;
    private final IArtSchoolInfoService schoolInfoService;
    private final IArtActivityScopeService activityScopeService;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    /**
     * 注册
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterBody registerBody) {
        String tenantId = registerBody.getTenantId();
        String username = registerBody.getUsername();
        String password = registerBody.getPassword();
        // 校验用户类型是否存在
        String userType = UserType.getUserType(registerBody.getUserType()).getUserType();

        boolean captchaEnabled = captchaProperties.getEnable();
        // 验证码开关
        if (captchaEnabled) {
            validateCaptcha(tenantId, username, registerBody.getCode(), registerBody.getUuid());
        }
        TenantHelper.dynamic(tenantId, () -> registerInTenant(registerBody, tenantId, username, password, userType));
    }

    private void registerInTenant(RegisterBody registerBody, String tenantId, String username, String password, String userType) {
        RegistrationCode registrationCode = registrationCodeService.validateForRegister(registerBody);
        SysRole boundRole = resolveBoundRole(registrationCode, userType);
        boolean effectiveAutoApprove = effectiveAutoApprove(registrationCode, userType);

        SysUserBo sysUser = new SysUserBo();
        sysUser.setUserName(username);
        sysUser.setNickName(username);
        sysUser.setPassword(BCrypt.hashpw(password));
        sysUser.setUserType(userType);
        sysUser.setPhonenumber(registerBody.getPhonenumber());
        sysUser.setStatus(effectiveAutoApprove ? "0" : "1");
        if ("school".equals(userType)) {
            Long schoolId = registrationCode.getSchoolId() == null
                ? schoolInfoService.saveOrGetSchool(registerBody.getSchoolId(), registerBody.getSchoolName(), registerBody.getSchoolProfileJson())
                : registrationCode.getSchoolId();
            sysUser.setSchoolId(schoolId);
            sysUser.setSchoolReviewStatus(effectiveAutoApprove ? "enabled" : "pending_review");
            if (effectiveAutoApprove && registrationCode.getActivityId() != null) {
                activityScopeService.ensureAssigned(registrationCode.getActivityId(), schoolId, "注册码自动审核通过");
            }
        }

        boolean exist = userMapper.exists(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUserName, sysUser.getUserName()));
        if (exist) {
            throw new UserException("user.register.save.error", username);
        }
        boolean regFlag = userService.registerUser(sysUser, tenantId);
        if (!regFlag) {
            throw new UserException("user.register.error");
        }
        SysUser registered = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getUserName, sysUser.getUserName()));
        if (registered == null) {
            throw new ServiceException("注册用户不存在");
        }
        bindRole(registered.getUserId(), boundRole);
        registrationCodeService.markUsed(registrationCode, registered.getUserId(), username, ServletUtils.getRequest());
        if (effectiveAutoApprove) {
            recordLogininfor(tenantId, username, Constants.REGISTER, "注册码自动审核通过");
        }
        recordLogininfor(tenantId, username, Constants.REGISTER, MessageUtils.message("user.register.success"));
    }

    public boolean isCodeRegisterRequest(RegisterBody registerBody) {
        return registrationCodeService.isCodeRegisterRequest(registerBody);
    }

    private boolean effectiveAutoApprove(RegistrationCode registrationCode, String userType) {
        if (!Boolean.TRUE.equals(registrationCode.getAutoApprove())) {
            return false;
        }
        // 通用学校码允许注册，但必须人工审核后才能启用，避免绑定到任意学校后自动生效。
        return !"school".equals(userType) || registrationCode.getSchoolId() != null;
    }

    private SysRole resolveBoundRole(RegistrationCode registrationCode, String userType) {
        String roleKey = registrationCode.getRoleKey();
        if (StringUtils.isBlank(roleKey)) {
            return null;
        }
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
            .eq(SysRole::getRoleKey, roleKey.trim())
            .eq(SysRole::getStatus, SystemConstants.NORMAL));
        if (role == null) {
            throw new ServiceException("注册角色不存在或已停用");
        }
        validateBusinessRole(role, userType);
        return role;
    }

    private void bindRole(Long userId, SysRole role) {
        if (role == null) {
            return;
        }
        Long exists = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
            .eq(SysUserRole::getUserId, userId)
            .eq(SysUserRole::getRoleId, role.getRoleId()));
        if (exists != null && exists > 0) {
            return;
        }
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getRoleId());
        userRoleMapper.insert(userRole);
    }

    private void validateBusinessRole(SysRole role, String userType) {
        String roleKey = StringUtils.blankToDefault(role.getRoleKey(), "").trim().toLowerCase(Locale.ROOT);
        if (Long.valueOf(1L).equals(role.getRoleId()) || "*".equals(roleKey) || "superadmin".equals(roleKey)) {
            throw new ServiceException("不允许使用该注册角色");
        }
        if ("school".equals(userType) && !isRoleKeyForType(roleKey, "crehn_school", "school")) {
            throw new ServiceException("注册角色与学校账号类型不匹配");
        }
        if ("expert".equals(userType) && !isRoleKeyForType(roleKey, "crehn_expert", "expert")) {
            throw new ServiceException("注册角色与专家账号类型不匹配");
        }
    }

    private boolean isRoleKeyForType(String roleKey, String primary, String alias) {
        return primary.equals(roleKey) || alias.equals(roleKey)
            || roleKey.startsWith(primary + "_") || roleKey.startsWith(alias + "_");
    }

    /**
     * 校验验证码
     *
     * @param username 用户名
     * @param code     验证码
     * @param uuid     唯一标识
     */
    public void validateCaptcha(String tenantId, String username, String code, String uuid) {
        String verifyKey = GlobalConstants.CAPTCHA_CODE_KEY + StringUtils.blankToDefault(uuid, "");
        String captcha = RedisUtils.getCacheObject(verifyKey);
        RedisUtils.deleteObject(verifyKey);
        if (captcha == null) {
            recordLogininfor(tenantId, username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        if (!StringUtils.equalsIgnoreCase(code, captcha)) {
            recordLogininfor(tenantId, username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error"));
            throw new CaptchaException();
        }
    }

    /**
     * 记录登录信息
     *
     * @param tenantId 用户ID
     * @param username 用户名
     * @param status   状态
     * @param message  消息内容
     * @return
     */
    private void recordLogininfor(String tenantId, String username, String status, String message) {
        LogininforEvent logininforEvent = new LogininforEvent();
        logininforEvent.setTenantId(tenantId);
        logininforEvent.setUsername(username);
        logininforEvent.setStatus(status);
        logininforEvent.setMessage(message);
        logininforEvent.setRequest(ServletUtils.getRequest());
        SpringUtils.context().publishEvent(logininforEvent);
    }

}
