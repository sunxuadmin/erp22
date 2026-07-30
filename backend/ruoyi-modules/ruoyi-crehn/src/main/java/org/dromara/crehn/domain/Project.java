package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project")
public class Project extends TenantEntity {
    @TableId(value = "id")
    private Long id;
    private Long activityId;
    private Long schoolId;
    private Long participantUserId;
    private Long categoryId;
    private String projectNo;
    private String projectName;
    private String groupCode;
    private String groupName;
    private String formDataJson;
    private String validationResultJson;
    private String status;
    private String recycledFromStatus;
    private Long recycledBy;
    private Date recycledAt;
    private String recycleReason;
    private Date submittedAt;
    private Long submittedBy;
    private Date participantSubmittedAt;
    private String schoolReviewStatus;
    private Date schoolReviewedAt;
    private Long schoolFinalBatchId;
    private Long configVersionId;
    private Long snapshotVersionId;
    private Long rowVersion;
    private String currentAuditOpinion;
    @TableLogic
    private String delFlag;
}
