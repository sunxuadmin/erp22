package org.dromara.common.tenant.core;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户基类
 *
 * @author Michelle.Chung
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TenantEntity extends BaseEntity {

    /**
     * 用户编号
     */
    private String tenantId;

}
