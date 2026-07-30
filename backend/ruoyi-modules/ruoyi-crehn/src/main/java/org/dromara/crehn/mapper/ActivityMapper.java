package org.dromara.crehn.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dromara.crehn.domain.Activity;
import org.dromara.crehn.domain.vo.ActivityDeleteCheckVo;
import org.dromara.crehn.domain.vo.ActivityVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

public interface ActivityMapper extends BaseMapperPlus<Activity, ActivityVo> {

    int DELETE_CHECK_DETAIL_LIMIT = 200;

    @Select({
        "<script>",
        "select id, activity_name, edition, year, organizer, undertaker, signup_start_at, signup_end_at,",
        "scope_type, description, status, create_time, del_flag",
        "from activity",
        "where del_flag = #{delFlag}",
        "<if test='query.activityName != null and query.activityName != \"\"'>",
        "and activity_name like concat('%', #{query.activityName}, '%')",
        "</if>",
        "<if test='query.status != null and query.status != \"\"'>",
        "and status = #{query.status}",
        "</if>",
        "<if test='query.year != null'>",
        "and year = #{query.year}",
        "</if>",
        "order by create_time desc",
        "</script>"
    })
    Page<ActivityVo> selectDeletedPage(@Param("page") Page<ActivityVo> page, @Param("query") Activity query, @Param("delFlag") String delFlag);

    @Update({
        "<script>",
        "update activity set del_flag = '0', update_time = sysdate()",
        "where del_flag = '1' and id in",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    int restoreByIds(@Param("ids") List<Long> ids);

    @Select({
        "select id, activity_name, menu_name, edition, year, organizer, undertaker,",
        "signup_start_at, signup_end_at, scope_type, description, status, create_time, del_flag",
        "from activity",
        "where id = #{id}"
    })
    Activity selectAnyById(@Param("id") Long id);

    @Select("select count(*) from activity_category where activity_id = #{activityId}")
    long countAllCategories(@Param("activityId") Long activityId);

    @Select({
        "select c.id, c.parent_id, c.category_code, c.category_name, c.category_group, c.del_flag,",
        "(select count(*) from category_field_schema f where f.category_id = c.id) as field_schema_count,",
        "(select count(*) from category_file_requirement r where r.category_id = c.id) as file_requirement_count",
        "from activity_category c",
        "where c.activity_id = #{activityId}",
        "order by c.del_flag asc, c.sort_order asc, c.id asc",
        "limit 200"
    })
    List<ActivityDeleteCheckVo.CategoryItem> selectAllCategoriesForDeleteCheck(@Param("activityId") Long activityId);

    @Select({
        "select count(*)",
        "from category_field_schema f",
        "where f.category_id in (select id from activity_category where activity_id = #{activityId})"
    })
    long countAllFieldSchemas(@Param("activityId") Long activityId);

    @Select({
        "select count(*)",
        "from category_file_requirement r",
        "where r.category_id in (select id from activity_category where activity_id = #{activityId})"
    })
    long countAllFileRequirements(@Param("activityId") Long activityId);

    @Select("select count(*) from activity_school_scope where activity_id = #{activityId}")
    long countAllSchoolScopes(@Param("activityId") Long activityId);

    @Select("select count(*) from project where activity_id = #{activityId}")
    long countAllProjects(@Param("activityId") Long activityId);

    @Select({
        "select p.id, p.project_no, p.project_name, p.school_id, s.school_name,",
        "p.status, p.del_flag, p.create_time, p.update_time,",
        "(select count(*) from project_file pf where pf.project_id = p.id) as file_count",
        "from project p",
        "left join school_info s on s.id = p.school_id",
        "where p.activity_id = #{activityId}",
        "order by p.del_flag asc, p.create_time desc, p.id desc",
        "limit 200"
    })
    List<ActivityDeleteCheckVo.ProjectItem> selectAllProjectsForDeleteCheck(@Param("activityId") Long activityId);

    @Select({
        "select count(*)",
        "from project_file pf",
        "left join project p on p.id = pf.project_id",
        "where p.activity_id = #{activityId}",
        "or pf.requirement_id in (",
        "select r.id from category_file_requirement r",
        "join activity_category c on c.id = r.category_id",
        "where c.activity_id = #{activityId})"
    })
    long countAllProjectFiles(@Param("activityId") Long activityId);

    @Select({
        "select pf.id, pf.project_id, p.project_no, p.project_name,",
        "p.status as project_status, p.del_flag as project_del_flag,",
        "pf.oss_id, pf.original_name, pf.status, pf.del_flag, pf.uploaded_at",
        "from project_file pf",
        "left join project p on p.id = pf.project_id",
        "where p.activity_id = #{activityId}",
        "or pf.requirement_id in (",
        "select r.id from category_file_requirement r",
        "join activity_category c on c.id = r.category_id",
        "where c.activity_id = #{activityId})",
        "order by pf.del_flag asc, pf.uploaded_at desc, pf.id desc",
        "limit 200"
    })
    List<ActivityDeleteCheckVo.FileItem> selectAllProjectFilesForDeleteCheck(@Param("activityId") Long activityId);

    @Select({
        "select reference_type, reference_label, reference_count, record_ids",
        "from (",
        "select 'project_member' reference_type, '项目成员' reference_label, count(*) reference_count,",
        "group_concat(id order by id separator ',') record_ids",
        "from project_member where activity_id = #{activityId}",
        "union all",
        "select 'project_audit_record', '项目审核记录', count(*), group_concat(id order by id separator ',')",
        "from project_audit_record where project_id in (select id from project where activity_id = #{activityId})",
        "union all",
        "select 'activity_quota_rule', '名额规则', count(*), group_concat(id order by id separator ',')",
        "from activity_quota_rule where activity_id = #{activityId}",
        "union all",
        "select 'report_rule_item', '报送规则包条目', count(*), group_concat(id order by id separator ',')",
        "from report_rule_item where category_id in (select id from activity_category where activity_id = #{activityId})",
        "union all",
        "select 'activity_report_rule', '活动报送规则', count(*), group_concat(id order by id separator ',')",
        "from activity_report_rule where activity_id = #{activityId}",
        "union all",
        "select 'audit_assignment', '审核授权', count(*), group_concat(id order by id separator ',')",
        "from audit_assignment where activity_id = #{activityId}",
        "union all",
        "select 'review_assignment', '评审任务', count(*), group_concat(id order by id separator ',')",
        "from review_assignment where activity_id = #{activityId}",
        "union all",
        "select 'review_score', '评审评分', count(*), group_concat(id order by id separator ',')",
        "from review_score where project_id in (select id from project where activity_id = #{activityId})",
        "or assignment_id in (select id from review_assignment where activity_id = #{activityId})",
        "union all",
        "select 'review_award_rule', '奖项规则', count(*), group_concat(id order by id separator ',')",
        "from review_award_rule where activity_id = #{activityId}",
        "union all",
        "select 'review_result', '评审结果', count(*), group_concat(id order by id separator ',')",
        "from review_result where activity_id = #{activityId}",
        "union all",
        "select 'review_result_log', '结果操作日志', count(*), group_concat(id order by id separator ',')",
        "from review_result_log where activity_id = #{activityId}",
        "union all",
        "select 'registration_code', '注册码', count(*), group_concat(id order by id separator ',')",
        "from registration_code where activity_id = #{activityId}",
        "union all",
        "select 'registration_code_usage', '注册码使用记录', count(*), group_concat(u.id order by u.id separator ',')",
        "from registration_code_usage u",
        "join registration_code c on c.id = u.code_id",
        "where c.activity_id = #{activityId}",
        "union all",
        "select 'notice_template', '活动公告模板', count(*), group_concat(id order by id separator ',')",
        "from notice_template where activity_id = #{activityId}",
        "union all",
        "select 'art_user_message', '站内消息', count(*), group_concat(id order by id separator ',')",
        "from art_user_message where activity_id = #{activityId}",
        ") activity_reference",
        "where reference_count > 0",
        "order by reference_label asc"
    })
    List<ActivityDeleteCheckVo.ReferenceItem> selectAllReferencesForDeleteCheck(@Param("activityId") Long activityId);

    @Delete({
        "delete from category_field_schema",
        "where category_id in (select id from activity_category where activity_id = #{activityId})"
    })
    int purgeFieldSchemas(@Param("activityId") Long activityId);

    @Delete({
        "delete from category_file_requirement",
        "where category_id in (select id from activity_category where activity_id = #{activityId})"
    })
    int purgeFileRequirements(@Param("activityId") Long activityId);

    @Delete("delete from activity_school_scope where activity_id = #{activityId}")
    int purgeSchoolScopes(@Param("activityId") Long activityId);

    @Delete("delete from activity_category where activity_id = #{activityId}")
    int purgeCategories(@Param("activityId") Long activityId);

    @Delete("delete from activity where id = #{activityId} and del_flag = '1'")
    int purgeDeletedActivity(@Param("activityId") Long activityId);
}
