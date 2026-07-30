package org.dromara.crehn.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.RegistrationCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = RegistrationCode.class)
public class RegistrationCodeVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @ExcelProperty("ID")
    private Long id;
    @ExcelProperty("注册码")
    private String code;
    @ExcelProperty("类型")
    private String codeType;
    @ExcelProperty("活动ID")
    private Long activityId;
    @ExcelProperty("学校ID")
    private Long schoolId;
    @ExcelProperty("绑定手机号")
    private String boundPhone;
    @ExcelProperty("绑定角色")
    private String roleKey;
    @ExcelProperty("autoApprove")
    private Boolean autoApprove;
    @ExcelProperty("最大使用次数")
    private Integer maxUseCount;
    @ExcelProperty("已使用次数")
    private Integer usedCount;
    @ExcelProperty("有效期")
    private Date expireAt;
    @ExcelProperty("状态")
    private String status;
    @ExcelProperty("备注")
    private String remark;
}
