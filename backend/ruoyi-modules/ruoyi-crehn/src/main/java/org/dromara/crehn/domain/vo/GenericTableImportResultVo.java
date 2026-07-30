package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class GenericTableImportResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String tableKey;
    private List<Map<String, Object>> rows;
    private Integer importedCount;
}
