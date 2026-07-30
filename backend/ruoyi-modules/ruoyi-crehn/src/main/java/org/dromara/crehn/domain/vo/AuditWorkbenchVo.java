package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class AuditWorkbenchVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<ProjectItem> pendingProjects = List.of();
    private AuditStats myStats = new AuditStats();
    private List<GroupItem> myGroupItems = List.of();
    private List<RecordItem> myRecords = List.of();

    @Data
    public static class ProjectItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;
        private String projectName;
        private String schoolName;
        private String categoryName;
        private String status;
        private Date submittedAt;
    }

    @Data
    public static class AuditStats implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private long totalCount;
        private long passCount;
        private long returnCount;
    }

    @Data
    public static class GroupItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String groupKey;
        private String groupName;
        private Integer sortOrder;
        private long totalCount;
        private long passCount;
        private long returnCount;
    }

    @Data
    public static class RecordItem implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Long recordId;
        private Long projectId;
        private String projectName;
        private String schoolName;
        private String categoryName;
        private String auditResult;
        private String opinion;
        private String status;
        private Date auditedAt;
    }
}
