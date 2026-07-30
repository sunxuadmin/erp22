package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.SchoolInfo;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = SchoolInfo.class)
public class SchoolInfoVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private String schoolName;
    private String schoolCode;
    private String schoolType;
    private String region;
    private String address;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String profileJson;
    private String status;
    private String remark;
    private Date createTime;
    private Long accountCount;
    private Long activeAccountCount;
    private Long projectCount;
    private Long visibleProjectCount;
    private Long recycledProjectCount;
    private Long fileCount;
    private Long memberCount;
}
