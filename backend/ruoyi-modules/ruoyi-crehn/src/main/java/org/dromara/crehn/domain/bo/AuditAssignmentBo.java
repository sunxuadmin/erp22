package org.dromara.crehn.domain.bo;

import lombok.Data;

import java.util.List;

@Data
public class AuditAssignmentBo {
    private Long id;
    private Long activityId;
    private Long categoryId;
    private List<Long> categoryIds;
    private Long auditorUserId;
    private List<Long> auditorUserIds;
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
}
