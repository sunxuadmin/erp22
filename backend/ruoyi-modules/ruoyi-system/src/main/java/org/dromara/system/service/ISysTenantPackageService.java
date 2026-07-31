package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysTenantPackageBo;
import org.dromara.system.domain.vo.SysTenantPackageVo;

import java.util.Collection;
import java.util.List;

/**
 * 用户套餐Service接口
 *
 * @author Michelle.Chung
 */
public interface ISysTenantPackageService {

    /**
     * 查询用户套餐
     */
    SysTenantPackageVo queryById(Long packageId);

    /**
     * 查询用户套餐列表
     */
    TableDataInfo<SysTenantPackageVo> queryPageList(SysTenantPackageBo bo, PageQuery pageQuery);

    /**
     * 查询用户套餐已启用列表
     */
    List<SysTenantPackageVo> selectList();

    /**
     * 查询用户套餐列表
     */
    List<SysTenantPackageVo> queryList(SysTenantPackageBo bo);

    /**
     * 新增用户套餐
     */
    Boolean insertByBo(SysTenantPackageBo bo);

    /**
     * 修改用户套餐
     */
    Boolean updateByBo(SysTenantPackageBo bo);

    /**
     * 校验套餐名称是否唯一
     */
    boolean checkPackageNameUnique(SysTenantPackageBo bo);

    /**
     * 修改套餐状态
     */
    int updatePackageStatus(SysTenantPackageBo bo);

    /**
     * 校验并批量删除用户套餐信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
