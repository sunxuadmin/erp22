package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SysWorkbenchAssetUploadVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String fileKey;

    private String url;

    private String originalName;

    private String fileName;

    private Long size;

    private String contentType;

}
