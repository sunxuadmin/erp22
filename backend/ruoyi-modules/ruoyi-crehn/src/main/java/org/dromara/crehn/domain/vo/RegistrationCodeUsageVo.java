package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.RegistrationCodeUsage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = RegistrationCodeUsage.class)
public class RegistrationCodeUsageVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long codeId;
    private Long userId;
    private Date usedAt;
    private String ipAddress;
    private String userAgent;
    private String boundAccount;
}
