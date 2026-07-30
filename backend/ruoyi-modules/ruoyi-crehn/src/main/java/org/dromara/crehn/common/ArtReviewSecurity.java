package org.dromara.crehn.common;

import lombok.RequiredArgsConstructor;
import org.dromara.crehn.domain.SchoolInfo;
import org.dromara.crehn.domain.Project;
import org.dromara.crehn.mapper.SchoolInfoMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysUser;
import org.dromara.system.mapper.SysUserMapper;
import org.springframework.stereotype.Component;

import static org.dromara.crehn.common.ArtReviewConstants.SCHOOL_REVIEW_ENABLED;
import static org.dromara.crehn.common.ArtReviewConstants.USER_TYPE_SCHOOL;
import static org.dromara.crehn.common.ArtReviewConstants.USER_TYPE_PARTICIPANT;

/**
 * 艺术展演业务服务端权限辅助。
 */
@Component
@RequiredArgsConstructor
public class ArtReviewSecurity {

    private final SysUserMapper sysUserMapper;
    private final SchoolInfoMapper schoolInfoMapper;

    public SysUser currentUser() {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("用户未登录");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("当前用户不存在");
        }
        return user;
    }

    public Long requireEnabledSchoolId() {
        SysUser user = currentUser();
        boolean schoolUser = USER_TYPE_SCHOOL.equals(user.getUserType());
        boolean participantUser = USER_TYPE_PARTICIPANT.equals(user.getUserType());
        if (!schoolUser && !participantUser) {
            throw new ServiceException("当前用户不是学校或参赛者账号");
        }
        if (schoolUser && !SCHOOL_REVIEW_ENABLED.equals(user.getSchoolReviewStatus())) {
            throw new ServiceException("学校账号未审核通过，暂不能申报");
        }
        if (!"0".equals(user.getStatus())) {
            throw new ServiceException("当前账号未启用");
        }
        if (user.getSchoolId() == null) {
            throw new ServiceException("学校账号未绑定学校");
        }
        SchoolInfo school = schoolInfoMapper.selectById(user.getSchoolId());
        if (school == null) {
            throw new ServiceException("绑定单位不存在，请联系管理员");
        }
        if (!"enabled".equals(school.getStatus())) {
            throw new ServiceException("绑定单位已停用或在回收站，暂不能申报，请联系管理员");
        }
        return user.getSchoolId();
    }

    public void checkSchoolProject(Long projectSchoolId) {
        Long schoolId = requireEnabledSchoolId();
        if (!schoolId.equals(projectSchoolId)) {
            throw new ServiceException("无权访问其他学校项目");
        }
    }

    public boolean isCurrentParticipant() {
        return USER_TYPE_PARTICIPANT.equals(currentUser().getUserType());
    }

    public void checkProjectAccess(Project project) {
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        SysUser user = currentUser();
        if (USER_TYPE_PARTICIPANT.equals(user.getUserType())) {
            if (!user.getUserId().equals(project.getParticipantUserId())) {
                throw new ServiceException("无权访问他人作品");
            }
            return;
        }
        checkSchoolProject(project.getSchoolId());
    }
}
