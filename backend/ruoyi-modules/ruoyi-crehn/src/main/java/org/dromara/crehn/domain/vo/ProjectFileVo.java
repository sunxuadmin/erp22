package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.ProjectFile;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = ProjectFile.class)
public class ProjectFileVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long projectId;
    private Long requirementId;
    private Long ossId;
    private Long previewOssId;
    private String fileTypeCode;
    private String originalName;
    private String storagePath;
    private String previewPath;
    private String previewExt;
    private String previewStatus;
    private String previewMessage;
    private Date previewGeneratedAt;
    private Integer previewRetryCount;
    private Date previewStartedAt;
    private String fileExt;
    private Long fileSize;
    private String mimeType;
    private String mediaType;
    private Double durationSeconds;
    private Integer width;
    private Integer height;
    private Double fps;
    private Long bitrate;
    private Integer dpi;
    private String metadataJson;
    private String checkStatus;
    private String checkMessage;
    private Integer versionNo;
    private Long uploadedBy;
    private Date uploadedAt;
    private String status;
    private Long updateBy;
    private Date updateTime;
    private String deletedByName;
}
