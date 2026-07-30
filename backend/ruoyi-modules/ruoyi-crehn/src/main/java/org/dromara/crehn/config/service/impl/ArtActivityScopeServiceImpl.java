package org.dromara.crehn.config.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.crehn.config.service.IArtActivityScopeService;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.ActivitySchoolScope;
import org.dromara.crehn.domain.SchoolInfo;
import org.dromara.crehn.domain.bo.ActivityScopeAssignBo;
import org.dromara.crehn.domain.vo.ActivitySchoolScopeVo;
import org.dromara.crehn.mapper.ActivityMapper;
import org.dromara.crehn.mapper.ActivitySchoolScopeMapper;
import org.dromara.crehn.mapper.SchoolInfoMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class ArtActivityScopeServiceImpl implements IArtActivityScopeService {

    private static final String SCHOOL_STATUS_ENABLED = "enabled";

    private final ActivitySchoolScopeMapper activitySchoolScopeMapper;
    private final ActivityMapper activityMapper;
    private final SchoolInfoMapper schoolInfoMapper;

    @Override
    public List<ActivitySchoolScopeVo> list(ActivitySchoolScope query) {
        return activitySchoolScopeMapper.selectVoList(Wrappers.lambdaQuery(ActivitySchoolScope.class)
            .eq(query.getActivityId() != null, ActivitySchoolScope::getActivityId, query.getActivityId())
            .eq(query.getSchoolId() != null, ActivitySchoolScope::getSchoolId, query.getSchoolId())
            .eq(query.getEnabled() != null, ActivitySchoolScope::getEnabled, query.getEnabled())
            .orderByDesc(ActivitySchoolScope::getCreateTime));
    }

    @Override
    public void assign(ActivityScopeAssignBo bo) {
        if (bo.getActivityId() == null || bo.getSchoolIds() == null || bo.getSchoolIds().isEmpty()) {
            throw new ServiceException("活动ID和学校ID不能为空");
        }
        for (Long schoolId : bo.getSchoolIds()) {
            ensureAssigned(bo.getActivityId(), schoolId, bo.getRemark());
        }
    }

    @Override
    public void ensureAssigned(Long activityId, Long schoolId, String remark) {
        if (activityId == null || schoolId == null) {
            return;
        }
        requireExistingActivity(activityId);
        requireEnabledSchool(schoolId);
        ActivitySchoolScope existing = activitySchoolScopeMapper.selectOne(Wrappers.lambdaQuery(ActivitySchoolScope.class)
            .eq(ActivitySchoolScope::getActivityId, activityId)
            .eq(ActivitySchoolScope::getSchoolId, schoolId));
        if (existing == null) {
            ActivitySchoolScope scope = new ActivitySchoolScope();
            scope.setActivityId(activityId);
            scope.setSchoolId(schoolId);
            scope.setEnabled(true);
            scope.setAssignedBy(currentUserIdOrNull());
            scope.setAssignedAt(new Date());
            scope.setRemark(remark);
            activitySchoolScopeMapper.insert(scope);
            auditScope("活动授权新增", scope, null, true);
            return;
        }
        Boolean oldEnabled = existing.getEnabled();
        existing.setEnabled(true);
        existing.setAssignedBy(currentUserIdOrNull());
        existing.setAssignedAt(new Date());
        existing.setRemark(remark);
        activitySchoolScopeMapper.updateById(existing);
        auditScope("活动授权新增", existing, oldEnabled, true);
    }

    @Override
    public boolean isSchoolAllowed(Long activityId, Long schoolId) {
        Long count = activitySchoolScopeMapper.selectCount(Wrappers.lambdaQuery(ActivitySchoolScope.class)
            .eq(ActivitySchoolScope::getActivityId, activityId)
            .eq(ActivitySchoolScope::getSchoolId, schoolId)
            .eq(ActivitySchoolScope::getEnabled, true));
        return count != null && count > 0;
    }

    @Override
    public int delete(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        List<ActivitySchoolScope> scopes = activitySchoolScopeMapper.selectBatchIds(Arrays.asList(ids));
        int rows = activitySchoolScopeMapper.deleteByIds(Arrays.asList(ids));
        for (ActivitySchoolScope scope : scopes) {
            auditScope("活动授权取消", scope, scope.getEnabled(), false);
        }
        return rows;
    }

    private Long currentUserIdOrNull() {
        try {
            return LoginHelper.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private void requireExistingActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new ServiceException("活动不存在");
        }
    }

    private void requireEnabledSchool(Long schoolId) {
        SchoolInfo school = schoolInfoMapper.selectById(schoolId);
        if (school == null) {
            throw new ServiceException("单位不存在");
        }
        if (!SCHOOL_STATUS_ENABLED.equals(school.getStatus())) {
            throw new ServiceException("单位已停用或在回收站，不能授权活动");
        }
    }

    private void auditScope(String action, ActivitySchoolScope scope, Boolean fromEnabled, Boolean toEnabled) {
        SchoolInfo school = scope.getSchoolId() == null ? null : schoolInfoMapper.selectById(scope.getSchoolId());
        log.info(
            "[ART_AUDIT] action={}, operator={}, time={}, activityId={}, unitId={}, unitName={}, fromStatus={}, toStatus={}, scopeId={}",
            action,
            currentUsername(),
            new Date(),
            scope.getActivityId(),
            scope.getSchoolId(),
            school == null ? null : school.getSchoolName(),
            fromEnabled,
            toEnabled,
            scope.getId()
        );
    }

    private String currentUsername() {
        try {
            return LoginHelper.getUsername();
        } catch (Exception e) {
            return "anonymous";
        }
    }
}
