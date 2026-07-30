package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notice_template")
public class NoticeTemplate extends TenantEntity {
    @TableId
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
    @TableLogic
    private String delFlag;
}
