package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_workbench_layout")
public class SysWorkbenchLayout extends TenantEntity {

    @TableId(value = "id")
    private Long id;

    private Long roleId;

    private String componentKey;

    private String title;

    private String width;

    private Integer sortOrder;

    private String visible;

    private String configJson;

}
