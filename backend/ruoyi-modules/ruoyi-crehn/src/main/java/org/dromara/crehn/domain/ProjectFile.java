package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("project_file")
public class ProjectFile extends TenantEntity {
    @TableId(value = "id")
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
    @TableLogic
    private String delFlag;
}
