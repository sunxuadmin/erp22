package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 学校工作台近期项目的轻量只读投影。 */
@Data
public class SchoolDashboardProjectVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long projectId;
    private String projectName;
    private String categoryName;
    private String status;
    private Date time;
}
