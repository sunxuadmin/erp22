package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SchoolImpactVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long schoolId;
    private String schoolName;
    private String schoolStatus;
    private Long accountCount;
    private Long activeAccountCount;
    private Long projectCount;
    private Long visibleProjectCount;
    private Long recycledProjectCount;
    private Long fileCount;
    private Long memberCount;
}
