package org.dromara.crehn.domain.bo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
@Data
public class SchoolProjectReviewBo {
    @NotNull private Long projectId;
    @NotBlank private String result;
    private String opinion;
}
