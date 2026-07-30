-- --------------------------------------------------
-- Art Review M7.4 read-only project submission view.
-- Safe to rerun after art_review_menu.sql and M6.8 scripts.
-- --------------------------------------------------

insert ignore into sys_role
(role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_dept, create_by, create_time, update_by, update_time, remark)
values
(900106, '000000', '作品上报查看员', 'crehn_project_viewer', 95, '5', 1, 1, '0', '0', 103, 1, sysdate(), null, null, '只读查看所有学校作品上报');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17980, '作品上报查看', 17000, 9, 'project-view', 'crehn/project-view/index', '', 1, 0, 'C', '0', '0', 'crehn:projectView:list', 'eye-open', 103, 1, sysdate(), null, null, '按类别或学校只读查看作品上报'),
(17981, '作品上报查看查询', 17980, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:projectView:query', '#', 103, 1, sysdate(), null, null, ''),
(17982, '查看未提交', 17980, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:projectView:draft', '#', 103, 1, sysdate(), null, null, '允许查看学校未提交草稿'),
(17983, '查看未审核', 17980, 3, '', '', '', 1, 0, 'F', '0', '0', 'crehn:projectView:submitted', '#', 103, 1, sysdate(), null, null, '允许查看已提交待审核作品'),
(17984, '查看已审核', 17980, 4, '', '', '', 1, 0, 'F', '0', '0', 'crehn:projectView:audited', '#', 103, 1, sysdate(), null, null, '允许查看已退回和已通过作品')
on duplicate key update
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    path = values(path),
    component = values(component),
    query_param = values(query_param),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    icon = values(icon),
    remark = values(remark);

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17985, '查看全部学校', 17980, 5, '', '', '', 1, 0, 'F', '0', '0', 'crehn:projectView:all', '#', 103, 1, sysdate(), null, null, '允许作品上报查看页面跨学校查看全部作品'),
(17986, '作品上报查看删除', 17980, 6, '', '', '', 1, 0, 'F', '0', '0', 'crehn:projectView:remove', '#', 103, 1, sysdate(), null, null, '允许在作品上报查看页面将作品移入回收站')
on duplicate key update
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    remark = values(remark);

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17000, 17980, 17981, 17426)
where r.role_key in ('crehn_admin', 'crehn_project_viewer');

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17985)
where r.role_key in ('crehn_admin', 'crehn_project_viewer');

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17982, 17983, 17984)
where r.role_key = 'crehn_admin';

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17986)
where r.role_key = 'crehn_admin';

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17983)
where r.role_key = 'crehn_project_viewer';
