package org.dromara.crehn.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
public class SchoolAccountExportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "登录账号")
    private String userName;

    @ExcelProperty(value = "英文登录账号")
    private String loginAlias;

    @ExcelProperty(value = "账号昵称")
    private String nickName;

    @ExcelProperty(value = "学校名称")
    private String schoolName;

    @ExcelProperty(value = "手机号")
    private String phonenumber;

    @ExcelProperty(value = "邮箱")
    private String email;

    @ExcelProperty(value = "审核状态")
    private String schoolReviewStatus;

    @ExcelProperty(value = "系统状态")
    private String status;

    @ExcelProperty(value = "备注")
    private String remark;

    @ExcelProperty(value = "创建时间")
    private Date createTime;
}
