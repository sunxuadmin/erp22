package org.dromara.crehn.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class PortalArticleActionBo {
    @NotNull(message = "文章ID不能为空")
    private Long articleId;
    private Date scheduledAt;
    private Long sourceVersionId;
    @NotBlank(message = "操作原因不能为空")
    private String reason;
}
