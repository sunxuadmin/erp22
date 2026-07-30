package org.dromara.crehn.domain.bo;

import lombok.Data;

/**
 * Audit pass/return request.
 */
@Data
public class ProjectAuditBo {
    private Long projectId;
    private String opinion;
    private Boolean notifyEnabled;
}
