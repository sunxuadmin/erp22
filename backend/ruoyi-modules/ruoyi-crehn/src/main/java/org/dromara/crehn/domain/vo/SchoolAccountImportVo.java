package org.dromara.crehn.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
public class SchoolAccountImportVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "登录账号")
    private String userName;

    @ExcelProperty(value = "英文登录账号")
    private String loginAlias;

    @ExcelProperty(value = "账号昵称")
    private String nickName;

    @ExcelProperty(value = "新建账号初始密码")
    private String initialPassword;

    @ExcelProperty(value = "学校名称")
    private String schoolName;

    @ExcelProperty(value = "手机号")
    private String phonenumber;

    @ExcelProperty(value = "邮箱")
    private String email;

    @ExcelProperty(value = "审核状态")
    private String schoolReviewStatus;

    @ExcelProperty(value = "允许改绑")
    private String allowRebind;

    @ExcelProperty(value = "备注")
    private String remark;
}
