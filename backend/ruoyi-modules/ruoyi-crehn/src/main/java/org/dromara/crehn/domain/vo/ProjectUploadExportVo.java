package org.dromara.crehn.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@ExcelIgnoreUnannotated
public class ProjectUploadExportVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("活动")
    private String activityName;
    @ExcelProperty("类别")
    private String categoryName;
    @ExcelProperty("项目编号")
    private String projectNo;
    @ExcelProperty("作品名称")
    private String projectName;
    @ExcelProperty("单位")
    private String schoolName;
    @ExcelProperty("组别")
    private String groupName;
    @ExcelProperty("\u9879\u76ee\u7c7b\u578b")
    private String projectType;
    @ExcelProperty("\u8282\u76ee\u5f62\u5f0f")
    private String programForm;
    @ExcelProperty("\u5c55\u6f14\u987a\u5e8f")
    private String performanceOrder;
    @ExcelProperty("状态")
    private String status;
    @ExcelProperty("提交时间")
    private Date submittedAt;
    @ExcelProperty("创建时间")
    private Date createTime;
}
