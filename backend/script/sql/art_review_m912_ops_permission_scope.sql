-- --------------------------------------------------
-- M9.12 bind crehn_ops to the crehn_admin menu-permission upper bound.
--
-- Idempotent behavior:
--   1. Bind each tenant's crehn_ops role to its crehn_admin role.
--   2. Remove only crehn_ops menu rows that are outside crehn_admin's current grants.
--   3. Never copy newly added crehn_admin grants into crehn_ops.
-- --------------------------------------------------

start transaction;

update sys_role ops
join sys_role admin_role
  on admin_role.tenant_id = ops.tenant_id
 and admin_role.role_key = 'crehn_admin'
 and admin_role.del_flag = '0'
set ops.permission_scope_role_id = admin_role.role_id,
    ops.update_time = sysdate()
where ops.role_key = 'crehn_ops'
  and ops.del_flag = '0'
  and (ops.permission_scope_role_id is null
       or ops.permission_scope_role_id <> admin_role.role_id);

delete ops_menu
from sys_role_menu ops_menu
join sys_role ops
  on ops.role_id = ops_menu.role_id
 and ops.role_key = 'crehn_ops'
 and ops.del_flag = '0'
join sys_role admin_role
  on admin_role.role_id = ops.permission_scope_role_id
 and admin_role.tenant_id = ops.tenant_id
 and admin_role.role_key = 'crehn_admin'
 and admin_role.del_flag = '0'
left join sys_role_menu admin_menu
  on admin_menu.role_id = admin_role.role_id
 and admin_menu.menu_id = ops_menu.menu_id
where admin_menu.menu_id is null;

commit;
