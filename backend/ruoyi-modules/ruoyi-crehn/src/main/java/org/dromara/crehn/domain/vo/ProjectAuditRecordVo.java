package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.ProjectAuditRecord;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = ProjectAuditRecord.class)
public class ProjectAuditRecordVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long projectId;
    private String fromStatus;
    private String toStatus;
    private String auditResult;
    private String opinion;
    private Long auditedBy;
    private String auditedByName;
    private Date auditedAt;
}
