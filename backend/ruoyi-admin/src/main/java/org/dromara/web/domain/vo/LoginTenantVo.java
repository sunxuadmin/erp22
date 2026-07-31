package org.dromara.web.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 登录用户对象
 *
 * @author Michelle.Chung
 */
@Data
public class LoginTenantVo {

    /**
     * 用户开关
     */
    private Boolean tenantEnabled;

    /**
     * 用户对象列表
     */
    private List<TenantListVo> voList;

}
