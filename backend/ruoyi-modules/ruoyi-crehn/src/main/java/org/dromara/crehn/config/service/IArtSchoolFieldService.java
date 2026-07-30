package org.dromara.crehn.config.service;

import org.dromara.crehn.domain.SchoolFieldSchema;
import org.dromara.crehn.domain.vo.SchoolFieldSchemaVo;

import java.util.List;

public interface IArtSchoolFieldService {
    List<SchoolFieldSchemaVo> list(SchoolFieldSchema query);

    int save(SchoolFieldSchema field);

    int delete(Long[] ids);
}
