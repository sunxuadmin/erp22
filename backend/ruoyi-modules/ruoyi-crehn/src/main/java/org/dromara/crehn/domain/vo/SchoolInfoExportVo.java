package org.dromara.crehn.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
public class SchoolInfoExportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "单位ID")
    private Long id;

    @ExcelProperty(value = "学校名称")
    private String schoolName;

    @ExcelProperty(value = "学校代码")
    private String schoolCode;

    @ExcelProperty(value = "学校类型")
    private String schoolType;

    @ExcelProperty(value = "地区")
    private String region;

    @ExcelProperty(value = "地址")
    private String address;

    @ExcelProperty(value = "负责人")
    private String contactName;

    @ExcelProperty(value = "联系电话")
    private String contactPhone;

    @ExcelProperty(value = "邮箱")
    private String contactEmail;

    @ExcelProperty(value = "状态")
    private String status;

    @ExcelProperty(value = "备注")
    private String remark;

    @ExcelProperty(value = "创建时间")
    private Date createTime;
}
