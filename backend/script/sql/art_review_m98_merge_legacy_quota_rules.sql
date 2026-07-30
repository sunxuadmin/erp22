-- M98: merge legacy activity_quota_rule rows into activity_report_rule.
-- Run manually after database backup. This script is intentionally not part of normal deployment.

set @legacy_report_rule_seed = (select greatest(coalesce(max(id), 0), 91098000) from activity_report_rule);

insert into activity_report_rule (
    id, tenant_id, activity_id, package_id, package_item_id,
    rule_code, rule_name, rule_group, rule_type, scope_type,
    scope_category_group, category_code, category_id, school_type, school_id,
    target_field_key, target_value, operator, limit_count, ratio_value,
    min_value, max_value, enforce_mode, message, rule_json,
    enabled, sort_order, remark, create_dept, create_by,
    create_time, update_by, update_time, del_flag
)
select
    @legacy_report_rule_seed := @legacy_report_rule_seed + 1,
    coalesce(q.tenant_id, '000000'),
    q.activity_id,
    null,
    null,
    concat('legacy_quota_', q.id),
    coalesce(nullif(q.remark, ''), nullif(q.group_name, ''), nullif(q.group_code, ''), concat('legacy quota ', q.id)),
    case
        when q.ratio_value is not null and (q.limit_count is null or q.rule_type = 'ratio') then 'ratio'
        else 'quota'
    end,
    case
        when q.rule_type in ('quota_count', 'category_count', 'total_count') then 'count'
        when q.rule_type is null or q.rule_type = '' then 'count'
        else q.rule_type
    end,
    case
        when q.school_id is not null then 'school'
        when q.category_id is not null then 'category'
        else 'activity'
    end,
    c.category_group,
    c.category_code,
    q.category_id,
    q.school_type,
    q.school_id,
    null,
    q.group_code,
    case
        when q.ratio_value is not null and q.rule_json like '%"ratioOperator":"min"%' then 'min'
        when q.ratio_value is not null then 'max'
        else 'max'
    end,
    q.limit_count,
    q.ratio_value,
    null,
    null,
    null,
    q.remark,
    q.rule_json,
    q.enabled,
    9000,
    concat('Migrated from legacy activity_quota_rule id=', q.id),
    q.create_dept,
    q.create_by,
    coalesce(q.create_time, now()),
    q.update_by,
    q.update_time,
    '0'
from activity_quota_rule q
left join activity_category c on c.id = q.category_id and c.del_flag = '0'
where q.del_flag = '0'
  and not exists (
      select 1
      from activity_report_rule r
      where r.del_flag = '0'
        and r.rule_code = concat('legacy_quota_', q.id)
  )
order by q.activity_id, q.id;

update activity_quota_rule q
set q.enabled = 0,
    q.del_flag = '1',
    q.update_time = now()
where q.del_flag = '0'
  and exists (
      select 1
      from activity_report_rule r
      where r.del_flag = '0'
        and r.rule_code = concat('legacy_quota_', q.id)
  );

update sys_menu
set menu_name = '规则模板包',
    remark = '维护可复用的规则模板包'
where menu_id = 18101;

update sys_menu
set menu_name = '当前活动规则',
    remark = '维护当前活动实际生效规则'
where menu_id = 18102;

update sys_menu
set visible = '1',
    status = '1',
    perms = '',
    component = '',
    remark = '历史兼容入口已退役：旧 activity_quota_rule 已合并到当前活动规则。'
where menu_id = 17630;

delete from sys_role_menu where menu_id in (17630, 17631, 17632);
