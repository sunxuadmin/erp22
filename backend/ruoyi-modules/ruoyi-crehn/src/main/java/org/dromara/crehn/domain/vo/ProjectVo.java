package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.Project;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@AutoMapper(target = Project.class)
public class ProjectVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long activityId;
    private String activityName;
    private Long schoolId;
    private Long participantUserId;
    private String schoolName;
    private Long categoryId;
    private List<Long> categoryIds;
    private String categoryName;
    private String categoryRuleJson;
    private String categoryTipText;
    private String categoryCheckMode;
    private String projectNo;
    private String projectName;
    private String groupCode;
    private String groupName;
    /**
     * 供各列表统一展示的组别名称，由列表组装器按固定字段、配置字段和历史数据回退规则解析。
     */
    private String groupDisplayName;
    private String formDataJson;
    private String validationResultJson;
    private String status;
    private String recycledFromStatus;
    private Long recycledBy;
    private String recycledByName;
    private Date recycledAt;
    private String recycleReason;
    private Date submittedAt;
    private Long submittedBy;
    private Date participantSubmittedAt;
    private String schoolReviewStatus;
    private Date schoolReviewedAt;
    private Long schoolFinalBatchId;
    private Long rowVersion;
    private String currentAuditOpinion;
    private Date createTime;
    private Date updateTime;
    private List<ProjectFileVo> files;
    private List<ProjectMemberVo> members;
    private List<ProjectAuditRecordVo> auditRecords;
    private List<CategoryFieldSchemaVo> fieldSchemas;
    private List<CategoryFileRequirementVo> fileRequirements;
}
