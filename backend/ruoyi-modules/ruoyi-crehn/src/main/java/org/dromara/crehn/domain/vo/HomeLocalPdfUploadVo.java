package org.dromara.crehn.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class HomeLocalPdfUploadVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String fileKey;
    private String originalName;
    private String fileName;
    private Long size;
}
