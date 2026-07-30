package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.ActivitySchoolScope;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = ActivitySchoolScope.class)
public class ActivitySchoolScopeVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long activityId;
    private Long schoolId;
    private Boolean enabled;
    private Long assignedBy;
    private Date assignedAt;
    private String remark;
    private Date createTime;
}
