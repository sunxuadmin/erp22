package org.dromara.crehn.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class ParticipantActivationIssueVo {
    @ExcelProperty("登录账号")
    private String userName;
    @ExcelProperty("姓名")
    private String participantName;
    @ExcelProperty("手机号")
    private String phonenumber;
    @ExcelProperty("一次性激活码")
    private String activationCode;
    @ExcelProperty("有效期")
    private Date expiresAt;
}
