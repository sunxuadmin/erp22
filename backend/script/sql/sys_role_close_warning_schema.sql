-- --------------------------------------------------
-- Required schema migration for role-level browser close warnings.
-- This file belongs to RequiredSchemaSql and must stay additive and idempotent.
-- Existing and newly created roles default to enabling the close/refresh warning.
-- --------------------------------------------------

alter table sys_role
    add column if not exists close_warning_enabled tinyint(1)
        not null
        default 1
        comment 'whether browser close or refresh warning is enabled for this role'
        after dept_check_strictly;
