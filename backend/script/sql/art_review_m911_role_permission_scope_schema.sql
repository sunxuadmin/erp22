-- --------------------------------------------------
-- Required schema migration for role menu-permission upper bounds.
-- This file belongs to RequiredSchemaSql and must stay additive and idempotent.
-- A null value means that the role has no parent-role menu restriction.
-- --------------------------------------------------

alter table sys_role
    add column if not exists permission_scope_role_id bigint(20)
        default null
        comment 'menu permission upper-bound role id'
        after close_warning_enabled;

create index if not exists idx_sys_role_permission_scope
    on sys_role(permission_scope_role_id);
