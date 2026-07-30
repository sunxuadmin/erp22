package org.dromara.crehn.domain.bo;

import lombok.Data;

@Data
public class ProjectMemberBo {
    private Long id;
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
