package org.dromara.crehn.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@ExcelIgnoreUnannotated
public class ShowcaseOrderVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long activityId;
    private Long categoryId;
    private Long projectId;

    @ExcelProperty("\u5c55\u6f14\u987a\u5e8f")
    private String performanceOrder;
    @ExcelProperty("\u6d3b\u52a8")
    private String activityName;
    @ExcelProperty("\u7c7b\u522b")
    private String categoryName;
    @ExcelProperty("\u7ec4\u522b")
    private String groupName;
    @ExcelProperty("\u9879\u76ee\u7c7b\u578b")
    private String projectType;
    @ExcelProperty("\u8282\u76ee\u5f62\u5f0f")
    private String programForm;
    @ExcelProperty("\u9879\u76ee\u7f16\u53f7")
    private String projectNo;
    @ExcelProperty("\u4f5c\u54c1\u540d\u79f0")
    private String projectName;
    @ExcelProperty("\u5355\u4f4d")
    private String schoolName;
    @ExcelProperty("\u72b6\u6001")
    private String status;
    @ExcelProperty("\u63d0\u4ea4\u65f6\u95f4")
    private Date submittedAt;
    @ExcelProperty("\u521b\u5efa\u65f6\u95f4")
    private Date createTime;
}
