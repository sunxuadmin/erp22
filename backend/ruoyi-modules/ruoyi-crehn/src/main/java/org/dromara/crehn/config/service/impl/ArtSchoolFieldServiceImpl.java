package org.dromara.crehn.config.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.crehn.config.service.IArtSchoolFieldService;
import org.dromara.crehn.domain.SchoolFieldSchema;
import org.dromara.crehn.domain.vo.SchoolFieldSchemaVo;
import org.dromara.crehn.mapper.SchoolFieldSchemaMapper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Service
public class ArtSchoolFieldServiceImpl implements IArtSchoolFieldService {

    private static final Pattern FIELD_KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");

    private final SchoolFieldSchemaMapper schoolFieldSchemaMapper;

    @Override
    public List<SchoolFieldSchemaVo> list(SchoolFieldSchema query) {
        return schoolFieldSchemaMapper.selectVoList(Wrappers.lambdaQuery(SchoolFieldSchema.class)
            .like(StringUtils.isNotBlank(query.getFieldLabel()), SchoolFieldSchema::getFieldLabel, query.getFieldLabel())
            .eq(query.getEnabled() != null, SchoolFieldSchema::getEnabled, query.getEnabled())
            .orderByAsc(SchoolFieldSchema::getSortOrder)
            .orderByAsc(SchoolFieldSchema::getId));
    }

    @Override
    public int save(SchoolFieldSchema field) {
        normalize(field);
        if (field.getId() == null) {
            SchoolFieldSchema existing = schoolFieldSchemaMapper.selectOne(Wrappers.lambdaQuery(SchoolFieldSchema.class)
                .eq(SchoolFieldSchema::getFieldKey, field.getFieldKey()));
            if (existing != null) {
                field.setId(existing.getId());
                return schoolFieldSchemaMapper.updateById(field);
            }
            return schoolFieldSchemaMapper.insert(field);
        }
        return schoolFieldSchemaMapper.updateById(field);
    }

    @Override
    public int delete(Long[] ids) {
        return schoolFieldSchemaMapper.deleteByIds(Arrays.asList(ids));
    }

    private void normalize(SchoolFieldSchema field) {
        if (StringUtils.isBlank(field.getFieldLabel())) {
            throw new ServiceException("字段名称不能为空");
        }
        if (StringUtils.isBlank(field.getFieldKey())) {
            field.setFieldKey("school_" + System.currentTimeMillis());
        }
        field.setFieldKey(field.getFieldKey().trim().toLowerCase(Locale.ROOT));
        if (!FIELD_KEY_PATTERN.matcher(field.getFieldKey()).matches()) {
            throw new ServiceException("字段编码只能使用小写英文、数字和下划线");
        }
        if (field.getEnabled() == null) {
            field.setEnabled(true);
        }
        if (field.getRequired() == null) {
            field.setRequired(false);
        }
        if (field.getSensitiveFlag() == null) {
            field.setSensitiveFlag(false);
        }
        if (field.getSortOrder() == null) {
            field.setSortOrder(0);
        }
    }
}
