package org.dromara.crehn.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dromara.crehn.domain.CategoryFieldSchema;
import org.dromara.crehn.domain.vo.CategoryFieldSchemaVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

public interface CategoryFieldSchemaMapper extends BaseMapperPlus<CategoryFieldSchema, CategoryFieldSchemaVo> {

    @Select({
        "<script>",
        "select id, category_id, field_key, field_label, field_type, required, options_json,",
        "validation_json, sensitive_flag, sort_order, del_flag",
        "from category_field_schema",
        "where category_id = #{categoryId} and del_flag = #{delFlag}",
        "order by sort_order asc, id asc",
        "</script>"
    })
    List<CategoryFieldSchemaVo> selectByCategoryAndDelFlag(@Param("categoryId") Long categoryId, @Param("delFlag") String delFlag);

    @Update({
        "<script>",
        "update category_field_schema set del_flag = '0', update_time = sysdate()",
        "where del_flag = '1' and id in",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    int restoreByIds(@Param("ids") List<Long> ids);
}
