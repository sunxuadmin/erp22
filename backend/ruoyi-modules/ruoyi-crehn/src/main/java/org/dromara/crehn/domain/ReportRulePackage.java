package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_rule_package")
public class ReportRulePackage extends TenantEntity {
    @TableId
    private Long id;
    private String packageCode;
    private String packageName;
    private String activityType;
    private String versionNo;
    private String status;
    private Boolean enabled;
    private String remark;
    @TableLogic
    private String delFlag;
}
