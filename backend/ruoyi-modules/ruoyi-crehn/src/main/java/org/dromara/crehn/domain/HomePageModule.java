package org.dromara.crehn.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("home_page_module")
public class HomePageModule extends TenantEntity {
    @TableId
    private Long id;
    private String moduleCode;
    private String moduleTitle;
    private String moduleType;
    private String anchor;
    private String eyebrow;
    private String intro;
    private String moreText;
    private String moreLink;
    private Boolean navEnabled;
    private Boolean enabled;
    private Integer sortOrder;
    private String layoutType;
    private String templateCode;
    private String configJson;
    private String remark;
    @TableLogic
    private String delFlag;
}
