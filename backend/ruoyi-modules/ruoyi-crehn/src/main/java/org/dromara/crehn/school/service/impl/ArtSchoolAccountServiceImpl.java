package org.dromara.crehn.school.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.crehn.common.ArtReviewConstants;
import org.dromara.crehn.config.service.IArtActivityScopeService;
import org.dromara.crehn.domain.RegistrationCode;
import org.dromara.crehn.domain.RegistrationCodeUsage;
import org.dromara.crehn.domain.SchoolInfo;
import org.dromara.crehn.domain.bo.ArtAccountCreateBo;
import org.dromara.crehn.domain.bo.SchoolAccountReviewBo;
import org.dromara.crehn.domain.vo.SchoolAccountExportVo;
import org.dromara.crehn.domain.vo.SchoolAccountImportVo;
import org.dromara.crehn.mapper.RegistrationCodeMapper;
import org.dromara.crehn.mapper.RegistrationCodeUsageMapper;
import org.dromara.crehn.mapper.SchoolInfoMapper;
import org.dromara.crehn.school.service.IArtSchoolAccountService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysDept;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.SysUser;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysUserVo;
import org.dromara.system.mapper.SysDeptMapper;
import org.dromara.system.mapper.SysRoleMapper;
import org.dromara.system.mapper.SysUserMapper;
import org.dromara.system.service.ISysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
@Slf4j
public class ArtSchoolAccountServiceImpl implements IArtSchoolAccountService {

    private static final Set<String> ALLOWED_STATUS = Set.of("pending_review", "enabled", "rejected", "disabled", "locked");
    private static final Set<String> ALLOWED_USER_TYPE = Set.of("school", "expert");
    private static final String ART_ROOT_CATEGORY = "crehn_root";
    private static final String ART_SCHOOL_CATEGORY_PREFIX = "crehn_school_";
    private static final String SCHOOL_STATUS_ENABLED = "enabled";
    private static final Pattern LOGIN_ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{2,64}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,30}$");

    private final SysUserMapper sysUserMapper;
    private final ISysUserService sysUserService;
    private final SysRoleMapper sysRoleMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SchoolInfoMapper schoolInfoMapper;
    private final RegistrationCodeUsageMapper registrationCodeUsageMapper;
    private final RegistrationCodeMapper registrationCodeMapper;
    private final IArtActivityScopeService activityScopeService;

    @Override
    public TableDataInfo<SysUserVo> querySchoolAccountPage(SysUserVo query, PageQuery pageQuery) {
        if ("1".equals(query.getDelFlag())) {
            Page<SysUserVo> page = sysUserMapper.selectDeletedBusinessUserPage(pageQuery.build(), query, ArtReviewConstants.USER_TYPE_SCHOOL, "1");
            return TableDataInfo.build(page);
        }
        LambdaQueryWrapper<SysUser> lqw = Wrappers.lambdaQuery(SysUser.class)
            .eq(SysUser::getUserType, ArtReviewConstants.USER_TYPE_SCHOOL)
            .eq(query.getSchoolId() != null, SysUser::getSchoolId, query.getSchoolId())
            .and(StringUtils.isNotBlank(query.getUserName()),
                w -> w.like(SysUser::getUserName, query.getUserName()).or().like(SysUser::getLoginAlias, query.getUserName()))
            .like(StringUtils.isNotBlank(query.getNickName()), SysUser::getNickName, query.getNickName())
            .eq(StringUtils.isNotBlank(query.getSchoolReviewStatus()), SysUser::getSchoolReviewStatus, query.getSchoolReviewStatus())
            .orderByDesc(SysUser::getCreateTime);
        Page<SysUserVo> page = sysUserMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public TableDataInfo<SysUserVo> queryAvailableSchoolAccountPage(SysUserVo query, PageQuery pageQuery) {
        LambdaQueryWrapper<SysUser> lqw = Wrappers.lambdaQuery(SysUser.class)
            .eq(SysUser::getUserType, ArtReviewConstants.USER_TYPE_SCHOOL)
            .isNull(SysUser::getSchoolId)
            .and(StringUtils.isNotBlank(query.getUserName()),
                w -> w.like(SysUser::getUserName, query.getUserName()).or().like(SysUser::getLoginAlias, query.getUserName()))
            .like(StringUtils.isNotBlank(query.getNickName()), SysUser::getNickName, query.getNickName())
            .like(StringUtils.isNotBlank(query.getPhonenumber()), SysUser::getPhonenumber, query.getPhonenumber())
            .orderByDesc(SysUser::getCreateTime);
        Page<SysUserVo> page = sysUserMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(page);
    }

    @Override
    public SysUserVo getSchoolAccount(Long userId) {
        SysUser user = requireSchoolAccount(userId);
        return sysUserMapper.selectVoById(user.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAccount(ArtAccountCreateBo bo) {
        if (bo == null || StringUtils.isBlank(bo.getUserName()) || StringUtils.isBlank(bo.getPassword())) {
            throw new ServiceException("账号和初始密码不能为空");
        }
        validatePassword(bo.getPassword());
        String userType = normalizeUserType(bo.getUserType());
        if (!ALLOWED_USER_TYPE.contains(userType)) {
            throw new ServiceException("不支持的账号类型");
        }
        if ("sys_user".equals(userType)) {
            validateSystemBusinessRoleIds(bo.getRoleIds());
        }
        SysUserBo user = new SysUserBo();
        user.setUserName(bo.getUserName().trim());
        user.setLoginAlias(ArtReviewConstants.USER_TYPE_SCHOOL.equals(userType) ? normalizeLoginAlias(bo.getLoginAlias()) : null);
        user.setNickName(StringUtils.blankToDefault(bo.getNickName(), user.getUserName()).trim());
        user.setPassword(BCrypt.hashpw(bo.getPassword()));
        user.setUserType(userType);
        user.setEmail(trimToNull(bo.getEmail()));
        user.setPhonenumber(trimToNull(bo.getPhonenumber()));
        user.setRemark(bo.getRemark());
        user.setRoleIds(validateRoleIds(bo.getRoleIds(), userType));
        user.setPostIds(new Long[0]);
        user.setStatus("0");
        user.setDeptId(ensureArtRootDept().getDeptId());

        if (ArtReviewConstants.USER_TYPE_SCHOOL.equals(userType)) {
            if (bo.getSchoolId() == null) {
                throw new ServiceException("学校账号必须绑定单位");
            }
            SchoolInfo school = requireEnabledSchool(bo.getSchoolId());
            user.setSchoolId(school.getId());
            user.setDeptId(ensureSchoolDept(school).getDeptId());
            String reviewStatus = StringUtils.blankToDefault(bo.getSchoolReviewStatus(), "enabled");
            if (!ALLOWED_STATUS.contains(reviewStatus)) {
                throw new ServiceException("学校账号审核状态不正确");
            }
            user.setSchoolReviewStatus(reviewStatus);
            user.setSchoolReviewBy(LoginHelper.getUserId());
            user.setSchoolReviewTime(new Date());
            user.setSchoolReviewOpinion("后台直接创建账号");
            user.setStatus("enabled".equals(reviewStatus) ? "0" : "1");
        }

        validateLoginNamesUnique(user.getUserId(), user.getUserName(), user.getLoginAlias());
        if (!sysUserService.checkUserNameUnique(user)) {
            throw new ServiceException("登录账号已存在");
        }
        if (StringUtils.isNotBlank(user.getPhonenumber()) && !sysUserService.checkPhoneUnique(user)) {
            throw new ServiceException("手机号码已存在");
        }
        if (StringUtils.isNotBlank(user.getEmail()) && !sysUserService.checkEmailUnique(user)) {
            throw new ServiceException("邮箱账号已存在");
        }
        sysUserService.insertUser(user);
    }

    @Override
    public void updateReviewStatus(SchoolAccountReviewBo bo) {
        if (bo.getUserId() == null || StringUtils.isBlank(bo.getSchoolReviewStatus())) {
            throw new ServiceException("用户ID和审核状态不能为空");
        }
        if (!ALLOWED_STATUS.contains(bo.getSchoolReviewStatus())) {
            throw new ServiceException("学校账号审核状态不正确");
        }
        if ("rejected".equals(bo.getSchoolReviewStatus()) && StringUtils.isBlank(bo.getOpinion())) {
            throw new ServiceException("驳回原因不能为空");
        }
        SysUser user = requireSchoolAccount(bo.getUserId());
        user.setSchoolReviewStatus(bo.getSchoolReviewStatus());
        user.setSchoolReviewBy(LoginHelper.getUserId());
        user.setSchoolReviewTime(new Date());
        user.setSchoolReviewOpinion(StringUtils.trimToEmpty(bo.getOpinion()));
        user.setStatus("enabled".equals(bo.getSchoolReviewStatus()) ? "0" : "1");
        sysUserMapper.updateById(user);
        if ("enabled".equals(bo.getSchoolReviewStatus())) {
            if (user.getSchoolId() != null) {
                ensureSchoolDept(requireSchool(user.getSchoolId()));
            }
            assignActivityFromRegistrationCode(user);
        }
    }

    @Override
    public void updateSchoolAccount(SysUserBo bo) {
        if (bo.getUserId() == null) {
            throw new ServiceException("用户ID不能为空");
        }
        SysUser user = requireSchoolAccount(bo.getUserId());
        String newUserName = StringUtils.isNotBlank(bo.getUserName()) ? bo.getUserName().trim() : user.getUserName();
        String newLoginAlias = normalizeLoginAlias(bo.getLoginAlias());
        validateLoginNamesUnique(user.getUserId(), newUserName, newLoginAlias);
        bo.setUserName(newUserName);
        user.setLoginAlias(newLoginAlias);
        if (StringUtils.isNotBlank(bo.getUserName())) {
            Long duplicateUserName = sysUserMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getUserName, bo.getUserName())
                .ne(SysUser::getUserId, user.getUserId()));
            if (duplicateUserName != null && duplicateUserName > 0) {
                throw new ServiceException("登录账号已存在");
            }
            user.setUserName(bo.getUserName());
        }
        if (StringUtils.isNotBlank(bo.getPhonenumber())) {
            Long duplicatePhone = sysUserMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getPhonenumber, bo.getPhonenumber())
                .ne(SysUser::getUserId, user.getUserId()));
            if (duplicatePhone != null && duplicatePhone > 0) {
                throw new ServiceException("手机号码已存在");
            }
            user.setPhonenumber(bo.getPhonenumber());
        } else {
            user.setPhonenumber(null);
        }
        if (StringUtils.isNotBlank(bo.getEmail())) {
            Long duplicateEmail = sysUserMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getEmail, bo.getEmail())
                .ne(SysUser::getUserId, user.getUserId()));
            if (duplicateEmail != null && duplicateEmail > 0) {
                throw new ServiceException("邮箱账号已存在");
            }
            user.setEmail(bo.getEmail());
        } else {
            user.setEmail(null);
        }
        if (StringUtils.isNotBlank(bo.getNickName())) {
            user.setNickName(bo.getNickName());
        }
        if (bo.getSchoolId() == null) {
            throw new ServiceException("学校账号必须绑定启用单位");
        }
        Long oldSchoolId = user.getSchoolId();
        SchoolInfo oldSchool = oldSchoolId == null ? null : schoolInfoMapper.selectById(oldSchoolId);
        SchoolInfo school = requireEnabledSchool(bo.getSchoolId());
        user.setSchoolId(school.getId());
        user.setDeptId(ensureSchoolDept(school).getDeptId());
        user.setRemark(bo.getRemark());
        sysUserMapper.updateById(user);
        auditSchoolAccountBinding(user, oldSchool, school);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindSchoolAccount(Long userId, Long schoolId) {
        SysUser user = requireSchoolAccount(userId);
        Long oldSchoolId = user.getSchoolId();
        SchoolInfo oldSchool = oldSchoolId == null ? null : schoolInfoMapper.selectById(oldSchoolId);
        SchoolInfo school = requireEnabledSchool(schoolId);
        user.setSchoolId(school.getId());
        user.setDeptId(ensureSchoolDept(school).getDeptId());
        user.setSchoolReviewStatus("enabled");
        user.setSchoolReviewBy(LoginHelper.getUserId());
        user.setSchoolReviewTime(new Date());
        user.setSchoolReviewOpinion("后台绑定已有账号到单位");
        user.setStatus("0");
        sysUserMapper.updateById(user);
        auditSchoolAccountBinding(user, oldSchool, school);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindSchoolAccount(Long userId) {
        SysUser user = requireSchoolAccount(userId);
        SchoolInfo oldSchool = user.getSchoolId() == null ? null : schoolInfoMapper.selectById(user.getSchoolId());
        user.setSchoolId(null);
        user.setDeptId(ensureArtRootDept().getDeptId());
        user.setSchoolReviewStatus("disabled");
        user.setSchoolReviewBy(LoginHelper.getUserId());
        user.setSchoolReviewTime(new Date());
        user.setSchoolReviewOpinion("后台解除单位绑定并停用账号");
        user.setStatus("1");
        sysUserMapper.updateById(user);
        auditSchoolAccountBinding(user, oldSchool, null);
    }

    @Override
    public void resetPassword(SysUserBo bo) {
        if (bo.getUserId() == null || StringUtils.isBlank(bo.getPassword())) {
            throw new ServiceException("用户ID和密码不能为空");
        }
        validatePassword(bo.getPassword());
        SysUser user = requireSchoolAccount(bo.getUserId());
        user.setPassword(BCrypt.hashpw(bo.getPassword()));
        sysUserMapper.updateById(user);
    }

    @Override
    public void deleteSchoolAccounts(Long[] userIds) {
        if (userIds == null || userIds.length == 0) {
            throw new ServiceException("用户ID不能为空");
        }
        for (Long userId : userIds) {
            requireSchoolAccount(userId);
        }
        sysUserMapper.deleteByIds(Arrays.asList(userIds));
    }

    @Override
    public void restoreSchoolAccounts(Long[] userIds) {
        if (userIds == null || userIds.length == 0) {
            throw new ServiceException("用户ID不能为空");
        }
        sysUserMapper.restoreBusinessUsers(Arrays.asList(userIds), ArtReviewConstants.USER_TYPE_SCHOOL);
    }

    @Override
    public List<SchoolAccountExportVo> exportList(SysUserVo query) {
        LambdaQueryWrapper<SysUser> lqw = Wrappers.lambdaQuery(SysUser.class)
            .eq(SysUser::getUserType, ArtReviewConstants.USER_TYPE_SCHOOL)
            .eq(query.getSchoolId() != null, SysUser::getSchoolId, query.getSchoolId())
            .and(StringUtils.isNotBlank(query.getUserName()),
                w -> w.like(SysUser::getUserName, query.getUserName()).or().like(SysUser::getLoginAlias, query.getUserName()))
            .like(StringUtils.isNotBlank(query.getNickName()), SysUser::getNickName, query.getNickName())
            .eq(StringUtils.isNotBlank(query.getSchoolReviewStatus()), SysUser::getSchoolReviewStatus, query.getSchoolReviewStatus())
            .orderByDesc(SysUser::getCreateTime);
        return sysUserMapper.selectList(lqw).stream().map(user -> {
            SchoolInfo school = user.getSchoolId() == null ? null : schoolInfoMapper.selectById(user.getSchoolId());
            SchoolAccountExportVo vo = new SchoolAccountExportVo();
            vo.setUserName(user.getUserName());
            vo.setLoginAlias(user.getLoginAlias());
            vo.setNickName(user.getNickName());
            vo.setSchoolName(school == null ? null : school.getSchoolName());
            vo.setPhonenumber(user.getPhonenumber());
            vo.setEmail(user.getEmail());
            vo.setSchoolReviewStatus(user.getSchoolReviewStatus());
            vo.setStatus(user.getStatus());
            vo.setRemark(user.getRemark());
            vo.setCreateTime(user.getCreateTime());
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String importData(List<SchoolAccountImportVo> list, boolean updateSupport) {
        if (list == null || list.isEmpty()) {
            throw new ServiceException("导入数据不能为空");
        }
        int createNum = 0;
        int updateNum = 0;
        int skipNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        Set<String> seenUserNames = new HashSet<>();
        Set<String> seenLoginAliases = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            SchoolAccountImportVo row = list.get(i);
            String userName = trimToNull(row.getUserName());
            String loginAlias = trimToNull(row.getLoginAlias());
            try {
                if (StringUtils.isBlank(userName)) {
                    throw new ServiceException("登录账号不能为空");
                }
                loginAlias = normalizeLoginAlias(loginAlias);
                String userNameKey = userName.toLowerCase(Locale.ROOT);
                if (seenLoginAliases.contains(userNameKey)) {
                    skipNum++;
                    successMsg.append("<br/>第").append(i + 2).append(" 行：登录账号“").append(userName).append("”与本次文件中的英文登录账号重复，已跳过");
                    continue;
                }
                if (StringUtils.isNotBlank(loginAlias) && (seenUserNames.contains(loginAlias) || seenLoginAliases.contains(loginAlias))) {
                    skipNum++;
                    successMsg.append("<br/>第").append(i + 2).append(" 行：英文登录账号“").append(loginAlias).append("”在本次文件中重复，已跳过");
                    continue;
                }
                if (seenUserNames.contains(userNameKey)) {
                    skipNum++;
                    successMsg.append("<br/>第 ").append(i + 2).append(" 行：登录账号“").append(userName).append("”在本次文件中重复，已跳过");
                    continue;
                }
                seenUserNames.add(userNameKey);
                if (StringUtils.isNotBlank(loginAlias)) {
                    seenLoginAliases.add(loginAlias);
                }
                SysUser existing = selectByUserName(userName);
                SysUser userNameAliasOwner = selectByLoginAlias(userName);
                if (userNameAliasOwner != null && (existing == null || !Objects.equals(userNameAliasOwner.getUserId(), existing.getUserId()))) {
                    throw new ServiceException("登录账号已作为其他账号的英文登录账号使用");
                }
                SysUser aliasOwner = selectByLoginAlias(loginAlias);
                if (aliasOwner != null && (existing == null || !Objects.equals(aliasOwner.getUserId(), existing.getUserId()))) {
                    throw new ServiceException("英文登录账号已绑定其他账号");
                }
                if (existing == null) {
                    createAccountFromImport(row, userName);
                    createNum++;
                    successMsg.append("<br/>第 ").append(i + 2).append(" 行：登录账号“").append(userName).append("”新增成功");
                    continue;
                }
                if (!ArtReviewConstants.USER_TYPE_SCHOOL.equals(existing.getUserType())) {
                    throw new ServiceException("登录账号已被非学校账号占用，不能导入");
                }
                if (!updateSupport) {
                    skipNum++;
                    successMsg.append("<br/>第 ").append(i + 2).append(" 行：登录账号“").append(userName).append("”已存在，已跳过");
                    continue;
                }
                updateAccountFromImport(existing, row);
                updateNum++;
                successMsg.append("<br/>第 ").append(i + 2).append(" 行：登录账号“").append(userName).append("”覆盖更新成功");
            } catch (Exception e) {
                failureNum++;
                failureMsg.append("<br/>第 ").append(i + 2).append(" 行：登录账号“")
                    .append(StringUtils.blankToDefault(userName, "空")).append("”导入失败：").append(e.getMessage());
            }
        }
        if (failureNum > 0) {
            throw new ServiceException("导入失败，共 " + failureNum + " 条错误：" + failureMsg);
        }
        return "导入完成：新增 " + createNum + " 条，覆盖更新 " + updateNum + " 条，跳过 " + skipNum + " 条。" + successMsg;
    }

    private SysUser requireSchoolAccount(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !ArtReviewConstants.USER_TYPE_SCHOOL.equals(user.getUserType())) {
            throw new ServiceException("学校账号不存在");
        }
        return user;
    }

    private void validateSystemBusinessRoleIds(Long[] roleIds) {
        if (roleIds == null || roleIds.length == 0) {
            throw new ServiceException("角色ID不能为空");
        }
        for (Long roleId : roleIds) {
            SysRole role = sysRoleMapper.selectById(roleId);
            String roleKey = role == null ? "" : StringUtils.blankToDefault(role.getRoleKey(), "").trim().toLowerCase(Locale.ROOT);
            boolean auditorRole = roleKey.equals("crehn_auditor") || roleKey.startsWith("crehn_auditor_");
            boolean adminRole = roleKey.equals("crehn_admin")
                || (roleKey.startsWith("crehn_admin_") && !roleKey.equals("crehn_admin_signature"));
            if (role == null || "1".equals(role.getStatus())
                || (!auditorRole && !adminRole)) {
                throw new ServiceException("后台业务账号只能使用审核员或管理员业务角色");
            }
        }
    }

    private String normalizeUserType(String userType) {
        if (StringUtils.isBlank(userType)) {
            return ArtReviewConstants.USER_TYPE_SCHOOL;
        }
        return switch (userType.trim().toLowerCase(Locale.ROOT)) {
            case "auditor", "admin", "sys_user" -> "sys_user";
            case "school" -> "school";
            case "expert" -> "expert";
            default -> userType.trim().toLowerCase(Locale.ROOT);
        };
    }

    private String trimToNull(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeLoginAlias(String loginAlias) {
        String value = trimToNull(loginAlias);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        if (!LOGIN_ALIAS_PATTERN.matcher(value).matches()) {
            throw new ServiceException("英文登录账号仅支持 2-64 位字母、数字、下划线或中横线");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private void validateLoginNamesUnique(Long userId, String userName, String loginAlias) {
        String normalizedUserName = trimToNull(userName);
        if (StringUtils.isBlank(normalizedUserName)) {
            throw new ServiceException("登录账号不能为空");
        }
        String normalizedAlias = normalizeLoginAlias(loginAlias);
        if (StringUtils.isNotBlank(normalizedAlias) && StringUtils.equalsIgnoreCase(normalizedUserName, normalizedAlias)) {
            throw new ServiceException("英文登录账号不能和登录账号相同");
        }
        validateLoginNameValueUnique(userId, normalizedUserName, "登录账号已存在或已作为英文登录账号使用");
        if (StringUtils.isNotBlank(normalizedAlias)) {
            validateLoginNameValueUnique(userId, normalizedAlias, "英文登录账号已存在或已作为登录账号使用");
        }
    }

    private void validateLoginNameValueUnique(Long userId, String value, String message) {
        Long count = sysUserMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
            .ne(userId != null, SysUser::getUserId, userId)
            .and(w -> w.eq(SysUser::getUserName, value).or().eq(SysUser::getLoginAlias, value)));
        if (count != null && count > 0) {
            throw new ServiceException(message);
        }
    }

    private SysUser selectByUserName(String userName) {
        return sysUserMapper.selectOne(Wrappers.lambdaQuery(SysUser.class)
            .eq(SysUser::getUserName, userName)
            .last("limit 1"));
    }

    private SysUser selectByLoginAlias(String loginAlias) {
        if (StringUtils.isBlank(loginAlias)) {
            return null;
        }
        return sysUserMapper.selectOne(Wrappers.lambdaQuery(SysUser.class)
            .eq(SysUser::getLoginAlias, loginAlias)
            .last("limit 1"));
    }

    private void createAccountFromImport(SchoolAccountImportVo row, String userName) {
        SchoolInfo school = requireEnabledSchoolByName(row.getSchoolName());
        ArtAccountCreateBo bo = new ArtAccountCreateBo();
        bo.setUserName(userName);
        bo.setLoginAlias(row.getLoginAlias());
        bo.setNickName(StringUtils.blankToDefault(trimToNull(row.getNickName()), userName));
        if (StringUtils.isBlank(row.getInitialPassword())) {
            throw new ServiceException("新建账号初始密码不能为空");
        }
        bo.setPassword(row.getInitialPassword());
        bo.setUserType(ArtReviewConstants.USER_TYPE_SCHOOL);
        bo.setSchoolId(school.getId());
        bo.setEmail(trimToNull(row.getEmail()));
        bo.setPhonenumber(trimToNull(row.getPhonenumber()));
        bo.setRemark(trimToNull(row.getRemark()));
        bo.setSchoolReviewStatus(normalizeReviewStatus(row.getSchoolReviewStatus()));
        bo.setRoleIds(defaultSchoolRoleIds());
        createAccount(bo);
    }

    private void validatePassword(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ServiceException("密码须为8至30位，并同时包含字母和数字");
        }
    }

    private void updateAccountFromImport(SysUser user, SchoolAccountImportVo row) {
        String phone = trimToNull(row.getPhonenumber());
        if (StringUtils.isNotBlank(phone)) {
            Long duplicatePhone = sysUserMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getPhonenumber, phone)
                .ne(SysUser::getUserId, user.getUserId()));
            if (duplicatePhone != null && duplicatePhone > 0) {
                throw new ServiceException("手机号已被其他账号使用");
            }
            user.setPhonenumber(phone);
        }
        String email = trimToNull(row.getEmail());
        if (StringUtils.isNotBlank(email)) {
            Long duplicateEmail = sysUserMapper.selectCount(Wrappers.lambdaQuery(SysUser.class)
                .eq(SysUser::getEmail, email)
                .ne(SysUser::getUserId, user.getUserId()));
            if (duplicateEmail != null && duplicateEmail > 0) {
                throw new ServiceException("邮箱已被其他账号使用");
            }
            user.setEmail(email);
        }
        String loginAlias = trimToNull(row.getLoginAlias());
        if (StringUtils.isNotBlank(loginAlias)) {
            String normalizedAlias = normalizeLoginAlias(loginAlias);
            validateLoginNamesUnique(user.getUserId(), user.getUserName(), normalizedAlias);
            user.setLoginAlias(normalizedAlias);
        }
        String nickName = trimToNull(row.getNickName());
        if (StringUtils.isNotBlank(nickName)) {
            user.setNickName(nickName);
        }
        String reviewStatus = trimToNull(row.getSchoolReviewStatus());
        if (StringUtils.isNotBlank(reviewStatus)) {
            user.setSchoolReviewStatus(normalizeReviewStatus(reviewStatus));
            user.setStatus("enabled".equals(user.getSchoolReviewStatus()) ? "0" : "1");
            user.setSchoolReviewBy(LoginHelper.getUserId());
            user.setSchoolReviewTime(new Date());
            user.setSchoolReviewOpinion("表格覆盖导入更新审核状态");
        }
        String schoolName = trimToNull(row.getSchoolName());
        if (StringUtils.isNotBlank(schoolName)) {
            SchoolInfo school = requireEnabledSchoolByName(schoolName);
            if (!Objects.equals(user.getSchoolId(), school.getId())) {
                if (!isYes(row.getAllowRebind())) {
                    throw new ServiceException("学校绑定不一致，如需改绑请将“允许改绑”填写为“是”");
                }
                SchoolInfo oldSchool = user.getSchoolId() == null ? null : schoolInfoMapper.selectById(user.getSchoolId());
                user.setSchoolId(school.getId());
                user.setDeptId(ensureSchoolDept(school).getDeptId());
                auditSchoolAccountBinding(user, oldSchool, school);
            }
        }
        String remark = trimToNull(row.getRemark());
        if (StringUtils.isNotBlank(remark)) {
            user.setRemark(remark);
        }
        sysUserMapper.updateById(user);
    }

    private String normalizeReviewStatus(String status) {
        String value = trimToNull(status);
        if (StringUtils.isBlank(value)) {
            return "enabled";
        }
        value = switch (value) {
            case "待审核", "待审" -> "pending_review";
            case "启用", "已启用", "通过" -> "enabled";
            case "驳回", "已驳回" -> "rejected";
            case "停用", "已停用" -> "disabled";
            case "锁定", "已锁定" -> "locked";
            default -> value;
        };
        if (!ALLOWED_STATUS.contains(value)) {
            throw new ServiceException("审核状态只支持待审核、启用、驳回、停用、锁定");
        }
        return value;
    }

    private SchoolInfo requireEnabledSchoolByName(String schoolName) {
        String name = trimToNull(schoolName);
        if (StringUtils.isBlank(name)) {
            throw new ServiceException("学校名称不能为空");
        }
        SchoolInfo school = schoolInfoMapper.selectOne(Wrappers.lambdaQuery(SchoolInfo.class)
            .eq(SchoolInfo::getSchoolName, name)
            .last("limit 1"));
        if (school == null) {
            throw new ServiceException("学校名称不存在");
        }
        if (!SCHOOL_STATUS_ENABLED.equals(school.getStatus())) {
            throw new ServiceException("单位已停用或在回收站，不能绑定账号");
        }
        return school;
    }

    private Long[] defaultSchoolRoleIds() {
        SysRole role = sysRoleMapper.selectOne(Wrappers.lambdaQuery(SysRole.class)
            .in(SysRole::getRoleKey, "crehn_school", "school")
            .eq(SysRole::getStatus, "0")
            .last("limit 1"));
        if (role == null) {
            throw new ServiceException("未找到启用的学校业务角色");
        }
        return new Long[]{role.getRoleId()};
    }

    private boolean isYes(String value) {
        String text = StringUtils.blankToDefault(value, "").trim().toLowerCase(Locale.ROOT);
        return "是".equals(text) || "true".equals(text) || "1".equals(text) || "yes".equals(text) || "y".equals(text);
    }

    private void auditSchoolAccountBinding(SysUser user, SchoolInfo oldSchool, SchoolInfo newSchool) {
        Long oldSchoolId = oldSchool == null ? null : oldSchool.getId();
        Long newSchoolId = newSchool == null ? null : newSchool.getId();
        if (Objects.equals(oldSchoolId, newSchoolId)) {
            return;
        }
        log.info(
            "[ART_AUDIT] action=学校账号更换绑定单位, operator={}, time={}, userId={}, userName={}, oldUnitId={}, oldUnitName={}, oldUnitStatus={}, newUnitId={}, newUnitName={}, newUnitStatus={}",
            currentUsername(),
            new Date(),
            user.getUserId(),
            user.getUserName(),
            oldSchoolId,
            oldSchool == null ? null : oldSchool.getSchoolName(),
            oldSchool == null ? null : oldSchool.getStatus(),
            newSchoolId,
            newSchool == null ? null : newSchool.getSchoolName(),
            newSchool == null ? null : newSchool.getStatus()
        );
    }

    private String currentUsername() {
        try {
            return LoginHelper.getUsername();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private Long[] validateRoleIds(Long[] roleIds, String userType) {
        if (roleIds == null || roleIds.length == 0) {
            throw new ServiceException("请选择账号角色");
        }
        for (Long roleId : roleIds) {
            SysRole role = sysRoleMapper.selectById(roleId);
            if (role == null || "1".equals(role.getStatus())) {
                throw new ServiceException("账号角色不存在或已停用");
            }
            if (Long.valueOf(1L).equals(role.getRoleId()) || "*".equals(role.getRoleKey()) || "superadmin".equals(role.getRoleKey())) {
                throw new ServiceException("不能在业务账号创建入口授予超级管理员角色");
            }
            String roleKey = StringUtils.blankToDefault(role.getRoleKey(), "").trim().toLowerCase(Locale.ROOT);
            if (ArtReviewConstants.USER_TYPE_SCHOOL.equals(userType) && !isRoleKeyForType(roleKey, "crehn_school", "school")) {
                throw new ServiceException("学校账号只能选择学校业务角色");
            }
            if ("expert".equals(userType) && !isRoleKeyForType(roleKey, "crehn_expert", "expert")) {
                throw new ServiceException("专家账号只能选择专家业务角色");
            }
        }
        return roleIds;
    }

    private boolean isRoleKeyForType(String roleKey, String primary, String alias) {
        return primary.equals(roleKey) || alias.equals(roleKey)
            || roleKey.startsWith(primary + "_") || roleKey.startsWith(alias + "_");
    }

    private SchoolInfo requireSchool(Long schoolId) {
        SchoolInfo school = schoolInfoMapper.selectById(schoolId);
        if (school == null) {
            throw new ServiceException("单位不存在");
        }
        return school;
    }

    private SchoolInfo requireEnabledSchool(Long schoolId) {
        SchoolInfo school = requireSchool(schoolId);
        if (!SCHOOL_STATUS_ENABLED.equals(school.getStatus())) {
            throw new ServiceException("单位已停用或在回收站，不能绑定账号");
        }
        return school;
    }

    private SysDept ensureArtRootDept() {
        SysDept root = sysDeptMapper.selectOne(Wrappers.lambdaQuery(SysDept.class)
            .eq(SysDept::getDeptCategory, ART_ROOT_CATEGORY)
            .last("limit 1"));
        if (root != null) {
            if (!"艺术评审组织".equals(root.getDeptName()) || !"0".equals(root.getStatus())) {
                root.setDeptName("艺术评审组织");
                root.setStatus("0");
                sysDeptMapper.updateById(root);
            }
            return root;
        }
        root = new SysDept();
        root.setParentId(0L);
        root.setAncestors("0");
        root.setDeptName("艺术评审组织");
        root.setDeptCategory(ART_ROOT_CATEGORY);
        root.setOrderNum(90);
        root.setStatus("0");
        root.setDelFlag("0");
        sysDeptMapper.insert(root);
        return root;
    }

    private SysDept ensureSchoolDept(SchoolInfo school) {
        SysDept root = ensureArtRootDept();
        String category = ART_SCHOOL_CATEGORY_PREFIX + school.getId();
        SysDept dept = sysDeptMapper.selectOne(Wrappers.lambdaQuery(SysDept.class)
            .eq(SysDept::getDeptCategory, category)
            .last("limit 1"));
        if (dept == null) {
            dept = new SysDept();
            dept.setParentId(root.getDeptId());
            dept.setAncestors(root.getAncestors() + "," + root.getDeptId());
            dept.setDeptCategory(category);
            dept.setOrderNum(1);
            dept.setDelFlag("0");
        }
        dept.setDeptName(school.getSchoolName());
        dept.setPhone(school.getContactPhone());
        dept.setEmail(school.getContactEmail());
        dept.setStatus(SCHOOL_STATUS_ENABLED.equals(school.getStatus()) ? "0" : "1");
        if (dept.getDeptId() == null) {
            sysDeptMapper.insert(dept);
        } else {
            sysDeptMapper.updateById(dept);
        }
        return dept;
    }

    private void assignActivityFromRegistrationCode(SysUser user) {
        if (user.getSchoolId() == null) {
            return;
        }
        RegistrationCodeUsage usage = registrationCodeUsageMapper.selectOne(Wrappers.lambdaQuery(RegistrationCodeUsage.class)
            .eq(RegistrationCodeUsage::getUserId, user.getUserId())
            .orderByDesc(RegistrationCodeUsage::getUsedAt)
            .last("limit 1"));
        if (usage == null) {
            return;
        }
        RegistrationCode code = registrationCodeMapper.selectById(usage.getCodeId());
        if (code != null && code.getActivityId() != null) {
            activityScopeService.ensureAssigned(code.getActivityId(), user.getSchoolId(), "学校账号审核通过后自动授权");
        }
    }
}
