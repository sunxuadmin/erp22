package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_member")
public class ProjectMember extends TenantEntity {
    @TableId(value = "id")
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
    @TableLogic
    private String delFlag;
}
