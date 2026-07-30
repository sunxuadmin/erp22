package org.dromara.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysWorkbenchComponentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String componentKey;

    private String componentName;

    private String componentType;

    private String defaultTitle;

    private String defaultWidth;

    private String permission;

    private List<String> roleKeys;

}
