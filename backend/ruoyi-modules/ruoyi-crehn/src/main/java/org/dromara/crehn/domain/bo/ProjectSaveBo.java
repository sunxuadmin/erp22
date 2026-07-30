package org.dromara.crehn.domain.bo;

import lombok.Data;

import java.util.List;

/**
 * School project draft/save request.
 */
@Data
public class ProjectSaveBo {
    private Long id;
    private Long activityId;
    private Long categoryId;
    private String projectName;
    private String groupCode;
    private String groupName;
    private String formDataJson;
    private List<ProjectMemberBo> members;
}
