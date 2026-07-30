package org.dromara.crehn.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ProjectMemberImportVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("序号")
    private String orderNo;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("身份证号 / 护照号")
    private String identityNo;

    @ExcelProperty("民族")
    private String nation;

    @ExcelProperty("年龄")
    private String age;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("所在院系")
    private String department;

    @ExcelProperty("年级")
    private String grade;

    @ExcelProperty("专业代码")
    private String majorCode;

    @ExcelProperty("专业名称")
    private String major;

    @ExcelProperty("学号 / 工作证号")
    private String studentNo;

    @ExcelProperty("联系方式")
    private String phone;

    @ExcelProperty("备注")
    private String remark;
}
