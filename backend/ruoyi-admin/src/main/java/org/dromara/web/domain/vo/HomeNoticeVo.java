package org.dromara.web.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class HomeNoticeVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String content;
    private String coverUrl;
    private Date createTime;
    private Integer sortOrder;
    private List<AttachmentVo> attachments;

    @Data
    public static class AttachmentVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private Long ossId;
        private String fileName;
        private String originalName;
        private String url;
    }
}
