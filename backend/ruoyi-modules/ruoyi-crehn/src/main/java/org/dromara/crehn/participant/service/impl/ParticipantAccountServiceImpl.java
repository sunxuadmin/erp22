package org.dromara.crehn.participant.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.crehn.domain.AccountAudit;
import org.dromara.crehn.domain.ActivationBatch;
import org.dromara.crehn.domain.ParticipantActivationCode;
import org.dromara.crehn.domain.ParticipantProfile;
import org.dromara.crehn.domain.bo.ParticipantActivateBo;
import org.dromara.crehn.domain.bo.ParticipantBatchImportBo;
import org.dromara.crehn.domain.bo.ParticipantCodeReissueBo;
import org.dromara.crehn.domain.bo.ParticipantImportRow;
import org.dromara.crehn.domain.vo.ParticipantActivationIssueVo;
import org.dromara.crehn.mapper.AccountAuditMapper;
import org.dromara.crehn.mapper.ActivationBatchMapper;
import org.dromara.crehn.mapper.ParticipantActivationCodeMapper;
import org.dromara.crehn.mapper.ParticipantProfileMapper;
import org.dromara.crehn.mapper.SchoolInfoMapper;
import org.dromara.crehn.participant.service.IParticipantAccountService;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ParticipantAccountServiceImpl implements IParticipantAccountService {
    private static final String USER_TYPE_PARTICIPANT = "participant";
    private static final String ROLE_KEY_PARTICIPANT = "crehn_participant";
    private static final String PROFILE_PENDING = "pending_confirmation";
    private static final String PROFILE_CONFIRMED = "confirmed";
    private static final Pattern PASSWORD_LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern PASSWORD_NUMBER = Pattern.compile(".*\\d.*");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ParticipantProfileMapper profileMapper;
    private final ActivationBatchMapper batchMapper;
    private final ParticipantActivationCodeMapper codeMapper;
    private final AccountAuditMapper auditMapper;
    private final SchoolInfoMapper schoolInfoMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final ISysUserService sysUserService;

    @Override
    public TableDataInfo<ParticipantProfile> queryPage(ParticipantProfile query, PageQuery pageQuery) {
        Long schoolId = resolveSchoolScope(query == null ? null : query.getSchoolId());
        LambdaQueryWrapper<ParticipantProfile> wrapper = Wrappers.lambdaQuery(ParticipantProfile.class)
            .eq(query != null && query.getActivityId() != null, ParticipantProfile::getActivityId, query == null ? null : query.getActivityId())
            .eq(schoolId != null, ParticipantProfile::getSchoolId, schoolId)
            .like(query != null && StringUtils.isNotBlank(query.getParticipantName()), ParticipantProfile::getParticipantName,
                query == null ? null : query.getParticipantName())
            .eq(query != null && StringUtils.isNotBlank(query.getStatus()), ParticipantProfile::getStatus,
                query == null ? null : query.getStatus())
            .orderByDesc(ParticipantProfile::getCreateTime);
        Page<ParticipantProfile> page = profileMapper.selectPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ParticipantActivationIssueVo> importParticipants(ParticipantBatchImportBo bo) {
        if (bo == null || bo.getActivityId() == null || bo.getRows() == null || bo.getRows().isEmpty()) {
            throw new ServiceException("活动和参赛者名单不能为空");
        }
        Long schoolId = resolveSchoolScope(bo.getSchoolId());
        if (schoolId == null || schoolInfoMapper.selectById(schoolId) == null) {
            throw new ServiceException("学校不存在");
        }
        Date expiresAt = requireFutureExpiry(bo.getExpiresAt());
        ActivationBatch batch = new ActivationBatch();
        batch.setBatchNo("AB" + System.currentTimeMillis() + randomText(4));
        batch.setActivityId(bo.getActivityId());
        batch.setSchoolId(schoolId);
        batch.setTotalCount(bo.getRows().size());
        batch.setActivatedCount(0);
        batch.setExpiresAt(expiresAt);
        batch.setStatus("issued");
        batch.setRemark(StringUtils.substring(bo.getRemark(), 0, 500));
        batchMapper.insert(batch);

        Long participantRoleId = requireParticipantRole().getRoleId();
        List<ParticipantActivationIssueVo> result = new ArrayList<>();
        Set<String> usernames = new java.util.HashSet<>();
        for (ParticipantImportRow row : bo.getRows()) {
            validateImportRow(row, usernames);
            SysUserBo user = new SysUserBo();
            user.setUserName(row.getUserName().trim());
            user.setNickName(row.getParticipantName().trim());
            user.setPassword(BCrypt.hashpw(randomText(32)));
            user.setUserType(USER_TYPE_PARTICIPANT);
            user.setSchoolId(schoolId);
            user.setPhonenumber(trimToNull(row.getPhonenumber()));
            user.setEmail(trimToNull(row.getEmail()));
            user.setStatus("1");
            user.setRemark(StringUtils.substring(row.getRemark(), 0, 500));
            user.setRoleIds(new Long[]{participantRoleId});
            user.setPostIds(new Long[0]);
            ensureUserUnique(user);
            sysUserService.insertUser(user);

            ParticipantProfile profile = new ParticipantProfile();
            profile.setUserId(user.getUserId());
            profile.setActivityId(bo.getActivityId());
            profile.setSchoolId(schoolId);
            profile.setParticipantName(row.getParticipantName().trim());
            profile.setIdentityType(StringUtils.blankToDefault(trimToNull(row.getIdentityType()), "id_card"));
            profile.setIdentityNoHash(hash(normalizeIdentity(row.getIdentityNo())));
            profile.setIdentityNoMasked(maskIdentity(row.getIdentityNo()));
            profile.setPhonenumber(trimToNull(row.getPhonenumber()));
            profile.setEmail(trimToNull(row.getEmail()));
            profile.setStatus(PROFILE_PENDING);
            profile.setRowVersion(1L);
            profileMapper.insert(profile);

            String plainCode = randomText(24);
            insertCode(batch, profile, plainCode, expiresAt);
            ParticipantActivationIssueVo issue = new ParticipantActivationIssueVo();
            issue.setUserName(user.getUserName());
            issue.setParticipantName(profile.getParticipantName());
            issue.setPhonenumber(profile.getPhonenumber());
            issue.setActivationCode(plainCode);
            issue.setExpiresAt(expiresAt);
            result.add(issue);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activate(ParticipantActivateBo bo, HttpServletRequest request) {
        validatePassword(bo.getPassword());
        if (!"confirmed".equalsIgnoreCase(bo.getProfileConfirmation())) {
            throw new ServiceException("必须由参赛者本人确认资料");
        }
        ParticipantActivationCode code = codeMapper.selectOne(Wrappers.lambdaQuery(ParticipantActivationCode.class)
            .eq(ParticipantActivationCode::getCodeHash, hash(normalizeCode(bo.getActivationCode())))
            .last("for update"));
        if (code == null) {
            throw new ServiceException("激活码无效");
        }
        if (!"issued".equals(code.getStatus())) {
            throw new ServiceException("激活码已使用或已失效");
        }
        if (code.getExpiresAt() == null || !code.getExpiresAt().after(new Date())) {
            code.setStatus("expired");
            codeMapper.updateById(code);
            throw new ServiceException("激活码已过期");
        }
        ParticipantProfile profile = profileMapper.selectById(code.getParticipantId());
        if (profile == null || !PROFILE_PENDING.equals(profile.getStatus())) {
            throw new ServiceException("参赛者资料状态不允许激活");
        }
        SysUser user = sysUserMapper.selectById(profile.getUserId());
        if (user == null || !USER_TYPE_PARTICIPANT.equals(user.getUserType())) {
            throw new ServiceException("参赛者账号不存在");
        }
        Date now = new Date();
        user.setPassword(BCrypt.hashpw(bo.getPassword()));
        user.setStatus("0");
        sysUserMapper.updateById(user);
        profile.setStatus(PROFILE_CONFIRMED);
        profile.setConfirmedAt(now);
        profile.setRowVersion((profile.getRowVersion() == null ? 0L : profile.getRowVersion()) + 1);
        profileMapper.updateById(profile);
        code.setStatus("used");
        code.setActivatedAt(now);
        codeMapper.updateById(code);
        ActivationBatch batch = batchMapper.selectById(code.getBatchId());
        if (batch != null) {
            batch.setActivatedCount((batch.getActivatedCount() == null ? 0 : batch.getActivatedCount()) + 1);
            if (Objects.equals(batch.getActivatedCount(), batch.getTotalCount())) {
                batch.setStatus("completed");
            }
            batchMapper.updateById(batch);
        }
        writeAudit(null, user.getUserId(), "participant_activate", "参赛者本人激活", request, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParticipantActivationIssueVo reissue(ParticipantCodeReissueBo bo, HttpServletRequest request) {
        ParticipantProfile profile = profileMapper.selectById(bo.getParticipantId());
        if (profile == null) {
            throw new ServiceException("参赛者不存在");
        }
        Long schoolId = resolveSchoolScope(profile.getSchoolId());
        if (!Objects.equals(schoolId, profile.getSchoolId())) {
            throw new ServiceException("无权处理其他学校参赛者");
        }
        codeMapper.update(null, Wrappers.lambdaUpdate(ParticipantActivationCode.class)
            .eq(ParticipantActivationCode::getParticipantId, profile.getId())
            .eq(ParticipantActivationCode::getStatus, "issued")
            .set(ParticipantActivationCode::getStatus, "revoked"));
        Date expiresAt = requireFutureExpiry(bo.getExpiresAt());
        ActivationBatch batch = new ActivationBatch();
        batch.setBatchNo("AR" + System.currentTimeMillis() + randomText(4));
        batch.setActivityId(profile.getActivityId());
        batch.setSchoolId(profile.getSchoolId());
        batch.setTotalCount(1);
        batch.setActivatedCount(0);
        batch.setExpiresAt(expiresAt);
        batch.setStatus("issued");
        batch.setRemark(StringUtils.substring(bo.getReason(), 0, 500));
        batchMapper.insert(batch);
        String plainCode = randomText(24);
        insertCode(batch, profile, plainCode, expiresAt);
        SysUser user = sysUserMapper.selectById(profile.getUserId());
        writeAudit(LoginHelper.getUserId(), profile.getUserId(), "participant_code_reissue", bo.getReason(), request, null);
        ParticipantActivationIssueVo issue = new ParticipantActivationIssueVo();
        issue.setUserName(user == null ? null : user.getUserName());
        issue.setParticipantName(profile.getParticipantName());
        issue.setPhonenumber(profile.getPhonenumber());
        issue.setActivationCode(plainCode);
        issue.setExpiresAt(expiresAt);
        return issue;
    }

    private Long resolveSchoolScope(Long requestedSchoolId) {
        SysUser current = sysUserMapper.selectById(LoginHelper.getUserId());
        if (current != null && "school".equals(current.getUserType())) {
            if (current.getSchoolId() == null) {
                throw new ServiceException("学校账号未绑定学校");
            }
            return current.getSchoolId();
        }
        return requestedSchoolId;
    }

    private void validateImportRow(ParticipantImportRow row, Set<String> usernames) {
        if (row == null || StringUtils.isBlank(row.getUserName()) || StringUtils.isBlank(row.getParticipantName())
            || StringUtils.isBlank(row.getIdentityNo())) {
            throw new ServiceException("登录账号、姓名和证件号码不能为空");
        }
        String key = row.getUserName().trim().toLowerCase(Locale.ROOT);
        if (!usernames.add(key)) {
            throw new ServiceException("导入名单中登录账号重复：" + row.getUserName());
        }
    }

    private void ensureUserUnique(SysUserBo user) {
        if (!sysUserService.checkUserNameUnique(user)) {
            throw new ServiceException("登录账号已存在：" + user.getUserName());
        }
        if (StringUtils.isNotBlank(user.getPhonenumber()) && !sysUserService.checkPhoneUnique(user)) {
            throw new ServiceException("手机号已被其他账号使用");
        }
        if (StringUtils.isNotBlank(user.getEmail()) && !sysUserService.checkEmailUnique(user)) {
            throw new ServiceException("邮箱已被其他账号使用");
        }
    }

    private SysRole requireParticipantRole() {
        SysRole role = sysRoleMapper.selectOne(Wrappers.lambdaQuery(SysRole.class)
            .eq(SysRole::getRoleKey, ROLE_KEY_PARTICIPANT)
            .eq(SysRole::getStatus, "0")
            .last("limit 1"));
        if (role == null) {
            throw new ServiceException("未找到启用的参赛者角色");
        }
        return role;
    }

    private void insertCode(ActivationBatch batch, ParticipantProfile profile, String plainCode, Date expiresAt) {
        ParticipantActivationCode code = new ParticipantActivationCode();
        code.setBatchId(batch.getId());
        code.setParticipantId(profile.getId());
        code.setCodeHash(hash(plainCode));
        code.setCodeHint(plainCode.substring(plainCode.length() - 4));
        code.setExpiresAt(expiresAt);
        code.setFailedAttempts(0);
        code.setStatus("issued");
        codeMapper.insert(code);
    }

    private void validatePassword(String password) {
        if (StringUtils.isBlank(password) || password.length() < 8 || password.length() > 30
            || !PASSWORD_LETTER.matcher(password).matches() || !PASSWORD_NUMBER.matcher(password).matches()) {
            throw new ServiceException("密码必须为8至30位，并同时包含字母和数字");
        }
    }

    private Date requireFutureExpiry(Date expiresAt) {
        if (expiresAt == null || !expiresAt.after(new Date())) {
            throw new ServiceException("激活码有效期必须晚于当前时间");
        }
        return expiresAt;
    }

    private String normalizeIdentity(String identityNo) {
        return identityNo == null ? "" : identityNo.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private String maskIdentity(String identityNo) {
        String value = normalizeIdentity(identityNo);
        if (value.length() <= 7) {
            return "****";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256不可用", e);
        }
    }

    private String randomText(int length) {
        byte[] bytes = new byte[(int) Math.ceil(length * 0.75)];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).substring(0, length).toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private void writeAudit(Long actorUserId, Long targetUserId, String action, String reason,
                            HttpServletRequest request, String metadataJson) {
        AccountAudit audit = new AccountAudit();
        audit.setActorUserId(actorUserId);
        audit.setTargetUserId(targetUserId);
        audit.setAction(action);
        audit.setReason(StringUtils.substring(reason, 0, 500));
        audit.setIpAddress(request == null ? null : StringUtils.substring(request.getRemoteAddr(), 0, 64));
        audit.setMetadataJson(metadataJson);
        auditMapper.insert(audit);
    }
}
