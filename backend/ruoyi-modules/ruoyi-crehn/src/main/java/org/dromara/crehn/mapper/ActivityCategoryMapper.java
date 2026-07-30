package org.dromara.crehn.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dromara.crehn.domain.ActivityCategory;
import org.dromara.crehn.domain.vo.ActivityCategoryPurgeCheckVo;
import org.dromara.crehn.domain.vo.ActivityCategoryVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

public interface ActivityCategoryMapper extends BaseMapperPlus<ActivityCategory, ActivityCategoryVo> {

    int PURGE_DETAIL_LIMIT = 200;

    @Select({
        "<script>",
        "select id, activity_id, parent_id, category_code, category_name, quota_limit, category_group,",
        "rule_json, tip_text, check_mode, sort_order, enabled, del_flag",
        "from activity_category",
        "where activity_id = #{activityId} and del_flag = #{delFlag}",
        "order by sort_order asc, id asc",
        "</script>"
    })
    List<ActivityCategoryVo> selectByActivityAndDelFlag(@Param("activityId") Long activityId, @Param("delFlag") String delFlag);

    @Update({
        "<script>",
        "update activity_category set del_flag = '0', update_time = sysdate()",
        "where del_flag = '1' and id in",
        "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    int restoreByIds(@Param("ids") List<Long> ids);

    @Select({
        "select id, activity_id, parent_id, category_code, category_name, quota_limit, category_group,",
        "rule_json, tip_text, check_mode, sort_order, enabled, del_flag",
        "from activity_category",
        "where id = #{id} and del_flag = '1'"
    })
    ActivityCategory selectDeletedById(@Param("id") Long id);

    @Select({
        "select id, activity_id, parent_id, category_code, category_name, quota_limit, category_group,",
        "rule_json, tip_text, check_mode, sort_order, enabled, del_flag",
        "from activity_category",
        "where id = #{id}"
    })
    ActivityCategory selectAnyById(@Param("id") Long id);

    @Select({
        "select count(*)",
        "from activity_category",
        "where activity_id = #{activityId}",
        "and id <> #{categoryId}",
        "and (parent_id = #{categoryId}",
        "or ((parent_id is null or parent_id = 0)",
        "and category_group = #{categoryName}",
        "and category_name <> #{categoryName}))"
    })
    long countAllChildren(@Param("activityId") Long activityId,
                          @Param("categoryId") Long categoryId,
                          @Param("categoryName") String categoryName);

    @Select({
        "select id, category_code, category_name, del_flag",
        "from activity_category",
        "where activity_id = #{activityId}",
        "and id <> #{categoryId}",
        "and (parent_id = #{categoryId}",
        "or ((parent_id is null or parent_id = 0)",
        "and category_group = #{categoryName}",
        "and category_name <> #{categoryName}))",
        "order by del_flag asc, sort_order asc, id asc",
        "limit 200"
    })
    List<ActivityCategoryPurgeCheckVo.ChildItem> selectAllChildrenForPurge(
        @Param("activityId") Long activityId,
        @Param("categoryId") Long categoryId,
        @Param("categoryName") String categoryName);

    @Select("select count(*) from project where category_id = #{categoryId}")
    long countAllProjects(@Param("categoryId") Long categoryId);

    @Select({
        "select p.id, p.project_no, p.project_name, p.school_id, s.school_name,",
        "p.status, p.del_flag, p.create_time, p.update_time,",
        "(select count(*) from project_file pf where pf.project_id = p.id) as file_count",
        "from project p",
        "left join school_info s on s.id = p.school_id",
        "where p.category_id = #{categoryId}",
        "order by p.del_flag asc, p.create_time desc, p.id desc",
        "limit 200"
    })
    List<ActivityCategoryPurgeCheckVo.ProjectItem> selectAllProjectsForPurge(@Param("categoryId") Long categoryId);

    @Select({
        "select count(*)",
        "from project_file pf",
        "left join project p on p.id = pf.project_id",
        "where p.category_id = #{categoryId}",
        "or pf.requirement_id in (select id from category_file_requirement where category_id = #{categoryId})"
    })
    long countAllProjectFiles(@Param("categoryId") Long categoryId);

    @Select({
        "select pf.id, pf.project_id, p.project_no, p.project_name,",
        "p.status as project_status, p.del_flag as project_del_flag,",
        "pf.oss_id, pf.original_name, pf.status, pf.del_flag, pf.uploaded_at",
        "from project_file pf",
        "left join project p on p.id = pf.project_id",
        "where p.category_id = #{categoryId}",
        "or pf.requirement_id in (select id from category_file_requirement where category_id = #{categoryId})",
        "order by pf.del_flag asc, pf.uploaded_at desc, pf.id desc",
        "limit 200"
    })
    List<ActivityCategoryPurgeCheckVo.FileItem> selectAllProjectFilesForPurge(@Param("categoryId") Long categoryId);

    @Select({
        "select reference_type, reference_label, reference_count, record_ids",
        "from (",
        "select 'project_member' reference_type, '项目成员' reference_label, count(*) reference_count,",
        "group_concat(id order by id separator ',') record_ids",
        "from project_member where project_id in (select id from project where category_id = #{categoryId})",
        "union all",
        "select 'project_audit_record', '项目审核记录', count(*), group_concat(id order by id separator ',')",
        "from project_audit_record where project_id in (select id from project where category_id = #{categoryId})",
        "union all",
        "select 'activity_quota_rule' reference_type, '名额规则' reference_label, count(*) reference_count,",
        "group_concat(id order by id separator ',') record_ids",
        "from activity_quota_rule where category_id = #{categoryId}",
        "union all",
        "select 'report_rule_item', '报送规则包条目', count(*), group_concat(id order by id separator ',')",
        "from report_rule_item where category_id = #{categoryId}",
        "union all",
        "select 'activity_report_rule', '活动报送规则', count(*), group_concat(id order by id separator ',')",
        "from activity_report_rule where category_id = #{categoryId}",
        "union all",
        "select 'audit_assignment', '审核授权', count(*), group_concat(id order by id separator ',')",
        "from audit_assignment where category_id = #{categoryId}",
        "union all",
        "select 'review_assignment', '评审任务', count(*), group_concat(id order by id separator ',')",
        "from review_assignment where category_id = #{categoryId}",
        "union all",
        "select 'review_score', '评审评分', count(*), group_concat(id order by id separator ',')",
        "from review_score where project_id in (select id from project where category_id = #{categoryId})",
        "or assignment_id in (select id from review_assignment where category_id = #{categoryId})",
        "union all",
        "select 'review_award_rule', '奖项规则', count(*), group_concat(id order by id separator ',')",
        "from review_award_rule where category_id = #{categoryId}",
        "union all",
        "select 'review_result', '评审结果', count(*), group_concat(id order by id separator ',')",
        "from review_result where category_id = #{categoryId}",
        "union all",
        "select 'review_result_log', '结果操作日志', count(*), group_concat(id order by id separator ',')",
        "from review_result_log where category_id = #{categoryId}",
        "union all",
        "select 'art_user_message', '站内消息', count(*), group_concat(id order by id separator ',')",
        "from art_user_message where category_id = #{categoryId}",
        ") category_reference",
        "where reference_count > 0",
        "order by reference_label asc"
    })
    List<ActivityCategoryPurgeCheckVo.ReferenceItem> selectAllReferencesForPurge(@Param("categoryId") Long categoryId);

    @Select("select count(*) from category_field_schema where category_id = #{categoryId}")
    long countAllFieldSchemas(@Param("categoryId") Long categoryId);

    @Select("select count(*) from category_file_requirement where category_id = #{categoryId}")
    long countAllFileRequirements(@Param("categoryId") Long categoryId);

    @Delete("delete from category_field_schema where category_id = #{categoryId}")
    int purgeFieldSchemas(@Param("categoryId") Long categoryId);

    @Delete("delete from category_file_requirement where category_id = #{categoryId}")
    int purgeFileRequirements(@Param("categoryId") Long categoryId);

    @Delete("delete from activity_category where id = #{categoryId} and del_flag = '1'")
    int purgeDeletedCategory(@Param("categoryId") Long categoryId);
}
