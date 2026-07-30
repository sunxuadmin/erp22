package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.Activity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = Activity.class)
public class ActivityVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private String activityName;
    private String menuName;
    private String edition;
    private Integer year;
    private String organizer;
    private String undertaker;
    private Date signupStartAt;
    private Date signupEndAt;
    private String scopeType;
    private String description;
    private String status;
    private Date createTime;
    private String delFlag;
}
