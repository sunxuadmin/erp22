package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.AuditAssignment;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@AutoMapper(target = AuditAssignment.class)
public class AuditAssignmentVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long activityId;
    private String activityName;
    private Long categoryId;
    private List<Long> categoryIds;
    private String categoryName;
    private Long auditorUserId;
    private List<Long> auditorUserIds;
    private String auditorUserName;
    private String auditorNickName;
    private String batchOperation;
    private String batchTarget;
    private String schoolScopeMode;
    private String schoolIdsJson;
    private Boolean allowQuery;
    private Boolean allowPass;
    private Boolean allowReturn;
    private Boolean allowWithdrawPass;
    private Boolean allowWithdrawReturn;
    private Boolean allowDownload;
    private String status;
    private Long assignedBy;
    private Date assignedAt;
    private Date createTime;
}
