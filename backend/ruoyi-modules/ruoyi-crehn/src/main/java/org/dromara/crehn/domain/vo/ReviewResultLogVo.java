package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.ReviewResultLog;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = ReviewResultLog.class)
public class ReviewResultLogVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long activityId;
    private Long categoryId;
    private Long projectId;
    private Long scoreId;
    private String targetType;
    private String actionType;
    private String beforeJson;
    private String afterJson;
    private String reason;
    private Long operatedBy;
    private String operatedByName;
    private Date operatedAt;
    private Date createTime;
}
