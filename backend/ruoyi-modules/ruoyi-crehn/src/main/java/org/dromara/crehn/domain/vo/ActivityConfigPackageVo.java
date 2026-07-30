package org.dromara.crehn.domain.vo;

import lombok.Data;
import org.dromara.crehn.domain.Activity;

import java.util.ArrayList;
import java.util.List;

@Data
public class ActivityConfigPackageVo {

    private String formatVersion = "1.0";

    private String packageType = "art_activity_config";

    private Activity activity;

    private List<ActivityConfigCategoryPackageVo> categories = new ArrayList<>();
}
