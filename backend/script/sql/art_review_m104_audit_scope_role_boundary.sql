-- --------------------------------------------------
-- M10.4 audit-scope role boundary and clearer menu label.
-- Personal auditors must use audit_assignment; project viewers use project-view scope.
-- --------------------------------------------------

start transaction;

update sys_menu
set menu_name = convert(0xE5AEA1E6A0B8E69D83E99990E58886E9858D using utf8mb4),
    path = '',
    component = '',
    menu_type = 'F',
    update_time = sysdate()
where menu_id = 17420;

delete rm
from sys_role_menu rm
join sys_role r on r.role_id = rm.role_id
where rm.menu_id = 17426
  and r.role_key in ('crehn_auditor', 'crehn_project_viewer');

commit;
