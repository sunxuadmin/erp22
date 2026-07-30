-- --------------------------------------------------
-- M9.9 one-time B-2 permission baseline for crehn_ops.
--
-- Existing environments:
--   1. Copy all currently enabled crehn_admin menu grants to crehn_ops.
--   2. Bind crehn_ops to crehn_admin as its permanent menu-permission upper bound.
--   3. Write a marker so reruns never overwrite later manual role changes.
--
-- This file intentionally does not create admin2 or store any password.
-- ProgramMigration deployment is allowed after the normal backup/confirmation.
-- The first effective run replaces crehn_ops grants once; later runs are marker-protected.
-- --------------------------------------------------

start transaction;

insert into sys_role
(role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, permission_scope_role_id, status, del_flag, create_dept, create_by, create_time, update_by, update_time, remark)
select 900107, '000000', '运维管理员', 'crehn_ops', 90, '1', 1, 1, admin_role.role_id, '0', '0', 103, 1, sysdate(), null, null, '以项目管理员为权限上限，可在角色管理中自由选择其权限子集'
from sys_role admin_role
where admin_role.tenant_id = '000000'
  and admin_role.role_key = 'crehn_admin'
  and admin_role.del_flag = '0'
  and not exists (
    select 1
    from sys_role
    where tenant_id = '000000'
      and role_key = 'crehn_ops'
);

update sys_role
join sys_role admin_role
  on admin_role.tenant_id = sys_role.tenant_id
 and admin_role.role_key = 'crehn_admin'
 and admin_role.del_flag = '0'
set sys_role.role_name = '运维管理员',
    sys_role.role_sort = 90,
    sys_role.data_scope = '1',
    sys_role.menu_check_strictly = 1,
    sys_role.dept_check_strictly = 1,
    sys_role.permission_scope_role_id = admin_role.role_id,
    sys_role.status = '0',
    sys_role.del_flag = '0',
    sys_role.remark = '以项目管理员为权限上限，可在角色管理中自由选择其权限子集',
    sys_role.update_time = sysdate()
where sys_role.tenant_id = '000000'
  and sys_role.role_key = 'crehn_ops';

set @crehn_ops_m99_apply := if(
    exists (
        select 1
        from sys_role admin_role
        join sys_role_menu admin_menu on admin_menu.role_id = admin_role.role_id
        where admin_role.tenant_id = '000000'
          and admin_role.role_key = 'crehn_admin'
    )
    and not exists (
        select 1
        from sys_config
        where tenant_id = '000000'
          and config_key = 'crehn.ops.permissionBaseline.b2'
    ),
    1,
    0
);

delete role_menu
from sys_role_menu role_menu
join sys_role ops on ops.role_id = role_menu.role_id
where ops.tenant_id = '000000'
  and ops.role_key = 'crehn_ops'
  and @crehn_ops_m99_apply = 1;

insert ignore into sys_role_menu(role_id, menu_id)
select ops.role_id, grants.menu_id
from sys_role ops
join (
    select admin_menu.menu_id
    from sys_role admin_role
    join sys_role_menu admin_menu on admin_menu.role_id = admin_role.role_id
    where admin_role.tenant_id = '000000'
      and admin_role.role_key = 'crehn_admin'
) grants
join sys_menu m on m.menu_id = grants.menu_id and m.status = '0'
where ops.tenant_id = '000000'
  and ops.role_key = 'crehn_ops'
  and @crehn_ops_m99_apply = 1;

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
       '标记 crehn_ops 已完成 B-2 首次权限基线，后续人工授权不得被迁移脚本覆盖'
from sys_role ops
cross join (
    select coalesce(max(config_id), 0) + 1 as config_id
    from sys_config
) next_config
where ops.tenant_id = '000000'
  and ops.role_key = 'crehn_ops'
  and @crehn_ops_m99_apply = 1
  and not exists (
      select 1
      from sys_config existing
      where existing.tenant_id = ops.tenant_id
        and existing.config_key = 'crehn.ops.permissionBaseline.b2'
  )
limit 1;

commit;
