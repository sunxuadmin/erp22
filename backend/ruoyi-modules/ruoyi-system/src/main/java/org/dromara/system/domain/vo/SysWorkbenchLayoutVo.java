package org.dromara.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.system.domain.SysWorkbenchLayout;

import java.io.Serial;
import java.io.Serializable;

@Data
@AutoMapper(target = SysWorkbenchLayout.class)
public class SysWorkbenchLayoutVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long roleId;

    private String roleKey;

    private String componentKey;

    private String componentName;

    private String componentType;

    private String title;

    private String width;

    private Integer sortOrder;

    private String visible;

    private String configJson;

    private String permission;

}
