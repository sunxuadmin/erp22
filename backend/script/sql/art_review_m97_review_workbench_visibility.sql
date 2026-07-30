-- --------------------------------------------------
-- M9.7 review workbench, peer score visibility, and ops read-only score access.
-- Safe to rerun.
-- --------------------------------------------------

update review_assignment
set score_visibility_policy = 'after_submit'
where score_visibility_policy is null or score_visibility_policy = '';

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17951, '评分调整', 17940, 11, '', '', '', 1, 0, 'F', '0', '0', 'crehn:result:score:edit', '#', 103, 1, sysdate(), null, null, '管理员调整或退回评委评分');

update sys_menu
set menu_name = '评分调整',
    parent_id = 17940,
    order_num = 11,
    menu_type = 'F',
    visible = '0',
    status = '0',
    perms = 'crehn:result:score:edit',
    update_time = sysdate()
where menu_id = 17951;

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17951)
where r.role_key = 'crehn_admin';

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (
    17000,
    17940,
    17941,
    17948,
    17949,
    17950
)
where r.role_key = 'crehn_ops'
  and not exists (
      select 1
      from sys_config baseline
      where baseline.tenant_id = r.tenant_id
        and baseline.config_key = 'crehn.ops.permissionBaseline.b2'
  );

insert into sys_workbench_layout
(id, tenant_id, role_id, component_key, title, width, sort_order, visible, config_json, create_dept, create_by, create_time, update_by, update_time)
select 181800000 + r.role_id,
       r.tenant_id,
       r.role_id,
       'admin_stage_notice',
       '管理员公告',
       '1/1',
       1,
       '0',
       null,
       103,
       1,
       sysdate(),
       null,
       null
from sys_role r
where r.role_key = 'crehn_ops'
  and not exists (
    select 1
    from sys_workbench_layout w
    where w.tenant_id = r.tenant_id
      and w.role_id = r.role_id
      and w.component_key = 'admin_stage_notice'
  );
