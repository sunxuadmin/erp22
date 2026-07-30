-- --------------------------------------------------
-- M9.6 ops role and school account login alias.
-- Safe to rerun.
-- --------------------------------------------------

alter table sys_user
    add column if not exists login_alias varchar(64) default null comment 'English login alias' after user_name;

create unique index if not exists uk_sys_user_login_alias on sys_user(login_alias);

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17207, '学校账号导出', 17002, 7, '', '', '', 1, 0, 'F', '0', '0', 'crehn:schoolAccount:export', '#', 103, 1, sysdate(), null, null, '');

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17208, '学校账号恢复', 17002, 8, '', '', '', 1, 0, 'F', '0', '0', 'crehn:schoolAccount:restore', '#', 103, 1, sysdate(), null, null, '');

update sys_menu
set menu_name = '学校账号导出',
    parent_id = 17002,
    order_num = 7,
    menu_type = 'F',
    visible = '0',
    status = '0',
    perms = 'crehn:schoolAccount:export',
    update_time = sysdate()
where menu_id = 17207;

update sys_menu
set menu_name = '学校账号恢复',
    parent_id = 17002,
    order_num = 8,
    menu_type = 'F',
    visible = '0',
    status = '0',
    perms = 'crehn:schoolAccount:restore',
    update_time = sysdate()
where menu_id = 17208;

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17207, 17208)
where r.role_key = 'crehn_admin';

set @crehn_ops_m96_new_role := if(
    exists (
        select 1
        from sys_role
        where tenant_id = '000000'
          and role_key = 'crehn_ops'
    ),
    0,
    1
);

insert into sys_role
(role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_dept, create_by, create_time, update_by, update_time, remark)
select 900107, '000000', '运维管理员', 'crehn_ops', 90, '1', 1, 1, '0', '0', 103, 1, sysdate(), null, null, '业务管理员权限基线与系统运维能力，可在角色管理中自由调整'
where not exists (
    select 1
    from sys_role
    where tenant_id = '000000'
      and role_key = 'crehn_ops'
);

update sys_role
set role_name = '运维管理员',
    role_sort = 90,
    data_scope = '1',
    menu_check_strictly = 1,
    dept_check_strictly = 1,
    status = '0',
    del_flag = '0',
    remark = '业务管理员权限基线与系统运维能力，可在角色管理中自由调整',
    update_time = sysdate()
where tenant_id = '000000'
  and role_key = 'crehn_ops';

insert ignore into sys_role_menu(role_id, menu_id)
select ops.role_id, grants.menu_id
from sys_role ops
join (
    select admin_menu.menu_id
    from sys_role admin_role
    join sys_role_menu admin_menu on admin_menu.role_id = admin_role.role_id
    where admin_role.tenant_id = '000000'
      and admin_role.role_key = 'crehn_admin'
    union
    select 1 union
    select 108 union
    select 500 union
    select 501 union
    select 1040 union
    select 1043 union
    select 1050 union
    select 2 union
    select 109 union
    select 1046 union
    select 1047 union
    select 1048
) grants
join sys_menu m on m.menu_id = grants.menu_id and m.status = '0'
where ops.tenant_id = '000000'
  and ops.role_key = 'crehn_ops'
  and @crehn_ops_m96_new_role = 1;

insert into sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
select next_config.config_id,
       ops.tenant_id,
       '运维管理员权限基线版本',
       'crehn.ops.permissionBaseline.b2',
       'admin-clone-v1',
       'Y',
       103,
       1,
       sysdate(),
       null,
       null,
       '标记 crehn_ops 已完成 B-2 首次权限基线，后续人工授权不得被初始化脚本覆盖'
from sys_role ops
cross join (
    select coalesce(max(config_id), 0) + 1 as config_id
    from sys_config
) next_config
where ops.tenant_id = '000000'
  and ops.role_key = 'crehn_ops'
  and @crehn_ops_m96_new_role = 1
  and exists (
      select 1
      from sys_role admin_role
      join sys_role_menu admin_menu on admin_menu.role_id = admin_role.role_id
      where admin_role.tenant_id = ops.tenant_id
        and admin_role.role_key = 'crehn_admin'
  )
  and not exists (
      select 1
      from sys_config existing
      where existing.tenant_id = ops.tenant_id
        and existing.config_key = 'crehn.ops.permissionBaseline.b2'
  )
limit 1;
