package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.ReportRulePackage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = ReportRulePackage.class)
public class ReportRulePackageVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private String packageCode;
    private String packageName;
    private String activityType;
    private String versionNo;
    private String status;
    private Boolean enabled;
    private String remark;
    private Date createTime;
}
