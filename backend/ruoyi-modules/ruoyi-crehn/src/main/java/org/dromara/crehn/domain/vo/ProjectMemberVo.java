package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.ProjectMember;

import java.io.Serial;
import java.io.Serializable;

@Data
@AutoMapper(target = ProjectMember.class)
public class ProjectMemberVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long projectId;
    private Long activityId;
    private Long schoolId;
    private String memberType;
    private String name;
    private String studentNo;
    private String department;
    private String major;
    private String roleName;
    private String extraJson;
    private Long photoOssId;
    private String photoPath;
    private Long studentReportOssId;
    private String studentReportPath;
    private Integer sortOrder;
    private String status;
}
