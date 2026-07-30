package org.dromara.crehn.config.service;

import org.dromara.crehn.domain.SchoolInfo;
import org.dromara.crehn.domain.vo.SchoolInfoExportVo;
import org.dromara.crehn.domain.vo.SchoolInfoImportVo;
import org.dromara.crehn.domain.vo.SchoolInfoVo;
import org.dromara.crehn.domain.vo.SchoolImpactVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.List;

public interface IArtSchoolInfoService {
    TableDataInfo<SchoolInfoVo> queryPage(SchoolInfo query, PageQuery pageQuery);

    List<SchoolInfoVo> queryList(SchoolInfo query);

    SchoolInfoVo getInfo(Long id);

    Long saveOrGetSchool(Long schoolId, String schoolName, String profileJson);

    int save(SchoolInfo school);

    int delete(Long[] ids, boolean confirmed);

    int restore(Long[] ids);

    List<SchoolImpactVo> impact(Long[] ids);

    List<SchoolInfoExportVo> exportList(SchoolInfo query);

    String importData(List<SchoolInfoImportVo> list, boolean updateSupport);
}
