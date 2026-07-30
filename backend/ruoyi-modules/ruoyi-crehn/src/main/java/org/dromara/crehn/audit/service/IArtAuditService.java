package org.dromara.crehn.audit.service;

import org.dromara.crehn.domain.vo.AuditWorkbenchVo;

public interface IArtAuditService {
    AuditWorkbenchVo workbench();

    void pass(Long projectId, String opinion, Boolean notifyEnabled);

    void returnProject(Long projectId, String opinion, Boolean notifyEnabled);

    void withdrawReturn(Long projectId, String opinion, Boolean notifyEnabled);

    void withdrawPass(Long projectId, String opinion, Boolean notifyEnabled);
}
