package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("school_info")
public class SchoolInfo extends TenantEntity {
    @TableId
    private Long id;
    private String schoolName;
    private String schoolCode;
    private String schoolType;
    private String region;
    private String address;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String profileJson;
    private String status;
    private String remark;
    @TableLogic
    private String delFlag;
}
