package org.dromara.crehn.domain.bo;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
@Data
public class SchoolFinalSubmitBo {
    @NotNull private Long activityId;
    @NotEmpty private List<Long> projectIds;
}
