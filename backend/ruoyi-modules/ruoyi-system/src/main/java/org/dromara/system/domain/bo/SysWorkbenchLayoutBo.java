package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.system.domain.SysWorkbenchLayout;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysWorkbenchLayout.class, reverseConvertGenerate = false)
public class SysWorkbenchLayoutBo extends BaseEntity {

    private Long id;

    private Long roleId;

    private String componentKey;

    private String title;

    private String width;

    private Integer sortOrder;

    private String visible;

    private String configJson;

}
