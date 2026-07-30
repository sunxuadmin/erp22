package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.NoticeTemplate;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@AutoMapper(target = NoticeTemplate.class)
public class NoticeTemplateVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String templateCode;
    private String templateName;
    private String noticeTitle;
    private String noticeContent;
    private String customCss;
    private Long coverOssId;
    private String coverUrl;
    private String attachmentOssIds;
    private String noticeGroup;
    private String targetMode;
    private String targetUserType;
    private Long activityId;
    private Long schoolId;
    private String schoolType;
    private Boolean enabled;
    private Integer sortOrder;
    private String remark;
    private Date createTime;
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
