-- --------------------------------------------------
-- Art Review M6.9 review and audit permission scope refinement.
-- Safe to rerun after art_review_menu.sql and M5/M6.8 scripts.
-- --------------------------------------------------

alter table review_assignment
    add column if not exists project_ids_json text null comment 'optional assigned project ids json array' after category_id;

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17405, convert(0xE692A4E99480E98080E59B9E using utf8mb4), 17004, 5, '', '', '', 1, 0, 'F', '0', '0', 'crehn:audit:withdrawReturn', '#', 103, 1, sysdate(), null, null, 'audit withdraw return permission');

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id = 17405
where r.role_key in ('crehn_admin', 'crehn_auditor');
