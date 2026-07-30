package org.dromara.crehn.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ActivityConfigExcelRowVo {

    @ExcelProperty("配置类型")
    private String section;

    @ExcelProperty("类别编码")
    private String categoryCode;

    @ExcelProperty("项目编码")
    private String itemCode;

    @ExcelProperty("项目名称")
    private String itemName;

    @ExcelProperty("排序")
    private Integer sortOrder;

    @ExcelProperty("配置JSON")
    private String payloadJson;
}
