-- Backfill the default project-upload overview for eligible workbench roles.
-- Safe to rerun: existing role/component layouts are never overwritten.

start transaction;

insert into sys_workbench_layout
(id, tenant_id, role_id, component_key, title, width, sort_order, visible, config_json, create_dept, create_by, create_time, update_by, update_time)
select 180700000 + r.role_id,
       r.tenant_id,
       r.role_id,
       'project_upload_overview',
       '作品上传总览',
       '1/1',
       2,
       '0',
       null,
       103,
       1,
       sysdate(),
       null,
       null
from sys_role r
where r.role_key in ('admin', 'crehn_admin', 'crehn_auditor', 'auditor', 'crehn_project_viewer')
  and r.status = '0'
  and r.del_flag = '0'
  and not exists (
    select 1
    from sys_workbench_layout w
    where w.tenant_id = r.tenant_id
      and w.role_id = r.role_id
      and w.component_key = 'project_upload_overview'
  );

commit;
