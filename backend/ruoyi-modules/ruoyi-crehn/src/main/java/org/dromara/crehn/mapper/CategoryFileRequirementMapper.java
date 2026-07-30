package org.dromara.crehn.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dromara.crehn.domain.CategoryFileRequirement;
import org.dromara.crehn.domain.vo.CategoryFileRequirementVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

public interface CategoryFileRequirementMapper extends BaseMapperPlus<CategoryFileRequirement, CategoryFileRequirementVo> {

    @Select({
        "<script>",
        "select id, category_id, file_type_code, file_type_name, allowed_ext, max_size_mb,",
        "min_count, max_count, required, rule_json, tip_text, sort_order, del_flag",
        "from category_file_requirement",
        "where category_id = #{categoryId} and del_flag = #{delFlag}",
        "order by sort_order asc, id asc",
        "</script>"
    })
    List<CategoryFileRequirementVo> selectByCategoryAndDelFlag(@Param("categoryId") Long categoryId, @Param("delFlag") String delFlag);

    @Update({
        "<script>",
        "update category_file_requirement set del_flag = '0', update_time = sysdate()",
        "where del_flag = '1' and id in",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    int restoreByIds(@Param("ids") List<Long> ids);
}
