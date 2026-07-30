package org.dromara.crehn.school.service;

import org.dromara.crehn.domain.bo.SchoolAccountReviewBo;
import org.dromara.crehn.domain.bo.ArtAccountCreateBo;
import org.dromara.crehn.domain.vo.SchoolAccountExportVo;
import org.dromara.crehn.domain.vo.SchoolAccountImportVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.bo.SysUserBo;
import org.dromara.system.domain.vo.SysUserVo;

import java.util.List;

public interface IArtSchoolAccountService {
    TableDataInfo<SysUserVo> querySchoolAccountPage(SysUserVo query, PageQuery pageQuery);

    TableDataInfo<SysUserVo> queryAvailableSchoolAccountPage(SysUserVo query, PageQuery pageQuery);

    SysUserVo getSchoolAccount(Long userId);

    void createAccount(ArtAccountCreateBo bo);

    void updateReviewStatus(SchoolAccountReviewBo bo);

    void updateSchoolAccount(SysUserBo bo);

    void bindSchoolAccount(Long userId, Long schoolId);

    void unbindSchoolAccount(Long userId);

    void resetPassword(SysUserBo bo);

    void deleteSchoolAccounts(Long[] userIds);

    void restoreSchoolAccounts(Long[] userIds);

    List<SchoolAccountExportVo> exportList(SysUserVo query);

    String importData(List<SchoolAccountImportVo> list, boolean updateSupport);
}
