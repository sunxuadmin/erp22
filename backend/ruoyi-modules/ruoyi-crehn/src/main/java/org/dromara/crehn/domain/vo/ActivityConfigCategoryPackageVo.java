package org.dromara.crehn.domain.vo;

import lombok.Data;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.CategoryFieldSchema;
import org.dromara.crehn.domain.CategoryFileRequirement;

import java.util.ArrayList;
import java.util.List;

@Data
public class ActivityConfigCategoryPackageVo {

    private ActivityCategory category;

    private List<CategoryFieldSchema> fields = new ArrayList<>();

    private List<CategoryFileRequirement> fileRequirements = new ArrayList<>();
}
