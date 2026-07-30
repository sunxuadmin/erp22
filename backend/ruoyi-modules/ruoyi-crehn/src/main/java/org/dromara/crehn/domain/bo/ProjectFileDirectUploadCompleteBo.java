package org.dromara.crehn.domain.bo;

import lombok.Data;

@Data
public class ProjectFileDirectUploadCompleteBo {
    private String uploadToken;
    private String objectKey;
    private String originalName;
    private Long fileSize;
    private String contentType;
}
