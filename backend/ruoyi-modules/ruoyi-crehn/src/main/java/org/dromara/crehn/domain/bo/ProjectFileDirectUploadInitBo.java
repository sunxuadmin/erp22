package org.dromara.crehn.domain.bo;

import lombok.Data;

@Data
public class ProjectFileDirectUploadInitBo {
    private String originalName;
    private Long fileSize;
    private String contentType;
}
