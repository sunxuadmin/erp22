package org.dromara.crehn.domain.bo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class ParticipantImportRow {
    @ExcelProperty("登录账号")
    private String userName;
    @ExcelProperty("姓名")
    private String participantName;
    @ExcelProperty("证件类型")
    private String identityType;
    @ExcelProperty("证件号码")
    private String identityNo;
    @ExcelProperty("手机号")
    private String phonenumber;
    @ExcelProperty("邮箱")
    private String email;
    @ExcelProperty("备注")
    private String remark;
}
