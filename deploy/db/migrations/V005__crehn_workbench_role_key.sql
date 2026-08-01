-- Stabilize workbench ownership on role_key and complete the CREHN role catalog.
-- Execute after V004. This migration is idempotent but must still be backed up and reviewed before production execution.

set @next_role_id = (select coalesce(max(role_id), 0) from sys_role);

insert into sys_role
(role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly,
 dept_check_strictly, status, del_flag, create_dept, create_by, create_time, remark)
select @next_role_id := @next_role_id + 1, '000000', x.role_name, x.role_key, x.role_sort,
       '1', 1, 1, '0', '0', null, 1, sysdate(), x.remark
from (
    select '结果管理员' role_name, 'crehn_result_admin' role_key, 51 role_sort, '结果生成、复核和发布管理' remark
    union all select '监督审计员', 'crehn_audit_supervisor', 90, '跨流程只读监督与审计追踪'
) x
where not exists (
    select 1 from sys_role r where r.tenant_id = '000000' and r.role_key = x.role_key
);

alter table sys_workbench_layout
    add column if not exists role_key varchar(100) null after role_id;

update sys_workbench_layout w
join sys_role r
  on r.tenant_id = w.tenant_id
 and r.role_id = w.role_id
set w.role_key = r.role_key
where w.role_key is null
   or trim(w.role_key) = ''
   or w.role_key <> r.role_key;

-- This statement intentionally fails if an orphan layout cannot be mapped to a role.
-- Repair the orphan explicitly instead of assigning a guessed role.
alter table sys_workbench_layout
    modify column role_key varchar(100) not null;

create unique index if not exists uk_sys_workbench_layout_role_key_component
    on sys_workbench_layout (tenant_id, role_key, component_key);

create index if not exists idx_sys_workbench_layout_role_key
    on sys_workbench_layout (tenant_id, role_key, sort_order);

-- Copy numeric role-shell configs to stable keys. Numeric rows are retained only as a rollback point for the old backend.
-- The new backend reads stable keys exclusively; no runtime compatibility branch is added.
set @next_config_id = (select coalesce(max(config_id), 0) from sys_config);

insert into sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type,
 create_dept, create_by, create_time, update_by, update_time, remark)
select @next_config_id := @next_config_id + 1,
       c.tenant_id,
       concat('工作台-角色系统布局-', r.role_name),
       concat('crehn.workbench.role.shell.', r.role_key),
       c.config_value,
       c.config_type,
       c.create_dept,
       c.create_by,
       c.create_time,
       c.update_by,
       sysdate(),
       concat(coalesce(c.remark, ''), '；由数字角色键迁移，旧键保留用于版本回滚')
from sys_config c
join sys_role r
  on r.tenant_id = c.tenant_id
 and c.config_key = concat('crehn.workbench.role.shell.', r.role_id)
left join sys_config stable
  on stable.tenant_id = c.tenant_id
 and stable.config_key = concat('crehn.workbench.role.shell.', r.role_key)
where stable.config_id is null;

set @crehn_root_menu_id = (
    select menu_id from sys_menu
    where parent_id = 0 and path = 'crehn'
    order by menu_id
    limit 1
);

-- Result administrators receive result and score-summary capabilities.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id = @crehn_root_menu_id
    or m.path in ('result', 'score-summary')
    or m.perms like 'crehn:result:%'
where r.tenant_id = '000000'
  and r.role_key = 'crehn_result_admin';

-- Supervisors are read-only: query/list/view permissions only.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id = @crehn_root_menu_id
    or m.perms in (
        'crehn:audit:list',
        'crehn:review:task',
        'crehn:result:summary',
        'crehn:project:list',
        'crehn:project:query'
    )
where r.tenant_id = '000000'
  and r.role_key = 'crehn_audit_supervisor';

-- Readback diagnostics. Any returned row needs explicit operator review.
select w.id, w.tenant_id, w.role_id, w.role_key, w.component_key
from sys_workbench_layout w
left join sys_role r
  on r.tenant_id = w.tenant_id
 and r.role_key = w.role_key
where r.role_id is null;

select legacy.config_id, legacy.tenant_id, legacy.config_key, r.role_key
from sys_config legacy
join sys_role r
  on r.tenant_id = legacy.tenant_id
 and legacy.config_key = concat('crehn.workbench.role.shell.', r.role_id)
left join sys_config stable
  on stable.tenant_id = legacy.tenant_id
 and stable.config_key = concat('crehn.workbench.role.shell.', r.role_key)
where stable.config_id is null;
