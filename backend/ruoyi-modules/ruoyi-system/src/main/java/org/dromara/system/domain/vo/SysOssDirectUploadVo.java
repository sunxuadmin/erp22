package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SysOssDirectUploadVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String uploadUrl;
    private String objectKey;
    private String fileName;
    private String service;
    private Long expiresInSeconds;
}
