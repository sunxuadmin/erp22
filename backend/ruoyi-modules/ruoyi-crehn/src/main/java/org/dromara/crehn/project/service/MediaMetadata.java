package org.dromara.crehn.project.service;

import lombok.Data;

@Data
public class MediaMetadata {
    private String mediaType;
    private Double durationSeconds;
    private Integer width;
    private Integer height;
    private Double fps;
    private Long bitrate;
    private Integer dpi;
    private String metadataJson;

    public boolean hasTechnicalValues() {
        return durationSeconds != null || width != null || height != null || fps != null || bitrate != null || dpi != null;
    }
}
