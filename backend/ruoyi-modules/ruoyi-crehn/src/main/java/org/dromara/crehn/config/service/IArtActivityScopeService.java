package org.dromara.crehn.config.service;

import org.dromara.crehn.domain.ActivitySchoolScope;
import org.dromara.crehn.domain.bo.ActivityScopeAssignBo;
import org.dromara.crehn.domain.vo.ActivitySchoolScopeVo;

import java.util.List;

public interface IArtActivityScopeService {
    List<ActivitySchoolScopeVo> list(ActivitySchoolScope query);

    void assign(ActivityScopeAssignBo bo);

    void ensureAssigned(Long activityId, Long schoolId, String remark);

    boolean isSchoolAllowed(Long activityId, Long schoolId);

    int delete(Long[] ids);
}
