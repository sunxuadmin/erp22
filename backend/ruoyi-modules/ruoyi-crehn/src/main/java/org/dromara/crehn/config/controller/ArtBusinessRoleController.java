package org.dromara.crehn.config.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.bo.BusinessRoleCreateBo;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysRole;
import org.dromara.system.domain.vo.SysRoleVo;
import org.dromara.system.mapper.SysRoleMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@RestController
@RequestMapping("/crehn/business-role")
public class ArtBusinessRoleController {

    private static final Pattern ROLE_KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{2,63}$");
    private final SysRoleMapper roleMapper;

    @SaCheckPermission(value = {"crehn:registration:list", "crehn:schoolAccount:add"}, mode = SaMode.OR)
    @GetMapping("/options")
    public R<List<SysRoleVo>> options(String roleName) {
        return R.ok(roleMapper.selectVoList(Wrappers.lambdaQuery(SysRole.class)
            .ne(SysRole::getRoleId, 1L)
            .ne(SysRole::getRoleKey, "crehn_admin_signature")
            .eq(SysRole::getStatus, "0")
            .and(w -> w.eq(SysRole::getRoleKey, "crehn_school")
                .or().likeRight(SysRole::getRoleKey, "crehn_school_")
                .or().eq(SysRole::getRoleKey, "crehn_expert")
                .or().likeRight(SysRole::getRoleKey, "crehn_expert_")
                .or().eq(SysRole::getRoleKey, "crehn_auditor")
                .or().likeRight(SysRole::getRoleKey, "crehn_auditor_")
                .or().eq(SysRole::getRoleKey, "crehn_admin")
                .or().likeRight(SysRole::getRoleKey, "crehn_admin_"))
            .like(StringUtils.isNotBlank(roleName), SysRole::getRoleName, roleName)
            .orderByAsc(SysRole::getRoleSort)
            .orderByAsc(SysRole::getRoleId)));
    }

    @SaCheckPermission("crehn:registration:add")
    @Log(title = "art business role quick create", businessType = BusinessType.INSERT)
    @PostMapping
    public R<SysRoleVo> create(@RequestBody BusinessRoleCreateBo bo) {
        if (StringUtils.isBlank(bo.getRoleName())) {
            throw new ServiceException("角色名称不能为空");
        }
        String roleKey = normalizeRoleKey(bo);
        if ("crehn_admin_signature".equals(roleKey) && !LoginHelper.isSuperAdmin()) {
            throw new ServiceException("只有超级管理员可以创建评审签字管理员角色");
        }
        SysRole exists = roleMapper.selectOne(Wrappers.lambdaQuery(SysRole.class)
            .eq(SysRole::getRoleKey, roleKey));
        if (exists != null) {
            return R.ok(roleMapper.selectVoById(exists.getRoleId()));
        }
        SysRole role = new SysRole();
        role.setRoleName(bo.getRoleName().trim());
        role.setRoleKey(roleKey);
        role.setRoleSort(99);
        role.setDataScope("5");
        role.setMenuCheckStrictly(true);
        role.setDeptCheckStrictly(true);
        role.setStatus("0");
        role.setRemark("art_business_role:" + normalizeRoleType(bo.getRoleType()));
        roleMapper.insert(role);
        return R.ok(roleMapper.selectVoById(role.getRoleId()));
    }

    private String normalizeRoleKey(BusinessRoleCreateBo bo) {
        String roleType = normalizeRoleType(bo.getRoleType());
        String expectedPrefix = "art_" + roleType;
        String roleKey = StringUtils.isNotBlank(bo.getRoleKey())
            ? bo.getRoleKey()
            : expectedPrefix + "_" + System.currentTimeMillis();
        roleKey = roleKey.trim().toLowerCase(Locale.ROOT);
        if (!ROLE_KEY_PATTERN.matcher(roleKey).matches()) {
            throw new ServiceException("角色权限标识只能包含小写英文、数字和下划线");
        }
        if (!roleKey.equals(expectedPrefix) && !roleKey.startsWith(expectedPrefix + "_")) {
            throw new ServiceException("业务角色权限标识必须以 " + expectedPrefix + " 开头");
        }
        return roleKey;
    }

    private String normalizeRoleType(String roleType) {
        String value = StringUtils.blankToDefault(roleType, "").trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "school", "expert", "auditor", "admin" -> value;
            default -> throw new ServiceException("角色类型只能是学校、专家、审核员或管理员");
        };
    }
}
