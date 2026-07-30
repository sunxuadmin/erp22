package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Data
public class ReviewTaskVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long assignmentId;
    /** Server-issued assignment scope key used for exact task filtering. */
    private String scopeKey;
    private Long projectId;
    private Long activityId;
    private String activityName;
    private Long categoryId;
    private String categoryName;
    private String categoryIds;
    private Long schoolId;
    private String programForm;
    private String groupOrNature;
    private String projectNo;
    private String projectName;
    private String projectStatus;
    private Date submittedAt;
    private String scoreMode;
    private String scoreRuleJson;
    private String exclusiveMode;
    private String scoreVisibilityPolicy;
    private Boolean hideSchoolInfo;
    private Boolean hideMemberInfo;
    private String hiddenFieldKeysJson;
    private Long scoreId;
    private BigDecimal scoreValue;
    private String gradeValue;
    private String commentText;
    private String scoreStatus;
    private Date scoreSubmittedAt;
    private Boolean lockedByOther;
    private String lockedMessage;
    private Map<String, String> displayFields;
    private ProjectVo project;
    private java.util.List<ReviewScoreVo> peerScores;
}
