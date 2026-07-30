package org.dromara.crehn.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.crehn.domain.HomePageModule;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = HomePageModule.class)
public class HomePageModuleVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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
    private Date createTime;
    private Date updateTime;
}
