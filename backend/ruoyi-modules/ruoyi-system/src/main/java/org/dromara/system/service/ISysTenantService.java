package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysTenantBo;
import org.dromara.system.domain.vo.SysTenantVo;

import java.util.Collection;
import java.util.List;

/**
 * 用户Service接口
 *
 * @author Michelle.Chung
 */
public interface ISysTenantService {

    /**
     * 查询用户
     */
    SysTenantVo queryById(Long id);

    /**
     * 基于用户ID查询用户
     */
    SysTenantVo queryByTenantId(String tenantId);

    /**
     * 查询用户列表
     */
    TableDataInfo<SysTenantVo> queryPageList(SysTenantBo bo, PageQuery pageQuery);

    /**
     * 查询用户列表
     */
    List<SysTenantVo> queryList(SysTenantBo bo);

    /**
     * 新增用户
     */
    Boolean insertByBo(SysTenantBo bo);

    /**
     * 修改用户
     */
    Boolean updateByBo(SysTenantBo bo);

    /**
     * 修改用户状态
     */
    int updateTenantStatus(SysTenantBo bo);

    /**
     * 校验用户是否允许操作
     */
    void checkTenantAllowed(String tenantId);

    /**
     * 校验并批量删除用户信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 校验企业名称是否唯一
     */
    boolean checkCompanyNameUnique(SysTenantBo bo);

    /**
     * 校验账号余额
     */
    boolean checkAccountBalance(String tenantId);

    /**
     * 校验有效期
     */
    boolean checkExpireTime(String tenantId);

    /**
     * 同步用户套餐
     */
    Boolean syncTenantPackage(String tenantId, Long packageId);

    /**
     * 同步用户字典
     */
    void syncTenantDict();

    /**
     * 同步用户参数配置
     */
    void syncTenantConfig();
}
