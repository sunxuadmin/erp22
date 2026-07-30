package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ProjectFileDirectUploadInitVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String uploadUrl;
    private String uploadToken;
    private String objectKey;
    private String fileName;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private Long expiresInSeconds;
}
