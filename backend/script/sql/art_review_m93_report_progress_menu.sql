-- --------------------------------------------------
-- M9.3 restore standalone report progress menu.
-- Safe to rerun after art_review_m74_project_view_menu.sql and art_review_m83_hide_project_view_menu.sql.
-- --------------------------------------------------

insert ignore into sys_role
(role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_dept, create_by, create_time, update_by, update_time, remark)
values
(900106, '000000', '上报进度查看员', 'crehn_project_viewer', 95, '5', 1, 1, '0', '0', 103, 1, sysdate(), null, null, '只读查看学校单位上报进度，可由超级管理员分配状态、全部单位和活动类别范围');

update sys_role
set role_name = '上报进度查看员',
    remark = '只读查看学校单位上报进度，可由超级管理员分配状态、全部单位和活动类别范围',
    update_time = sysdate()
where role_key = 'crehn_project_viewer';

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17980, '上报进度', 17000, 9, 'project-view', 'crehn/project-view/index', '', 1, 0, 'C', '0', '0', 'crehn:projectView:list', 'eye-open', 103, 1, sysdate(), null, null, '按类别或学校单位查看草稿、待审核、审核通过、已退回上报进度'),
(17981, '查看详情', 17980, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:projectView:query', '#', 103, 1, sysdate(), null, null, '允许查看上报详情'),
(17982, '查看草稿', 17980, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:projectView:draft', '#', 103, 1, sysdate(), null, null, '允许查看学校单位未提交草稿'),
(17983, '查看待审核', 17980, 3, '', '', '', 1, 0, 'F', '0', '0', 'crehn:projectView:submitted', '#', 103, 1, sysdate(), null, null, '允许查看已提交待审核上报'),
(17984, '查看审核通过/已退回', 17980, 4, '', '', '', 1, 0, 'F', '0', '0', 'crehn:projectView:audited', '#', 103, 1, sysdate(), null, null, '允许查看审核通过和已退回上报'),
(17985, '查看全部单位', 17980, 5, '', '', '', 1, 0, 'F', '0', '0', 'crehn:projectView:all', '#', 103, 1, sysdate(), null, null, '允许上报进度页面跨学校单位查看全部上报'),
(17986, '删除上报', 17980, 6, '', '', '', 1, 0, 'F', '0', '0', 'crehn:projectView:remove', '#', 103, 1, sysdate(), null, null, '允许在上报进度页面将上报移入回收站')
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
    remark = values(remark),
    update_time = sysdate();

update sys_menu
set menu_name = '上报进度',
    parent_id = 17000,
    order_num = 9,
    path = 'project-view',
    component = 'crehn/project-view/index',
    query_param = '',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'C',
    visible = '0',
    status = '0',
    perms = 'crehn:projectView:list',
    icon = 'eye-open',
    update_time = sysdate(),
    remark = '按类别或学校单位查看草稿、待审核、审核通过、已退回上报进度'
where menu_id = 17980
   or path = 'project-view'
   or component = 'crehn/project-view/index';

update sys_menu
set menu_name = '查看详情',
    visible = '0',
    status = '0',
    perms = 'crehn:projectView:query',
    update_time = sysdate()
where menu_id = 17981;

update sys_menu
set menu_name = '查看草稿',
    visible = '0',
    status = '0',
    perms = 'crehn:projectView:draft',
    remark = '允许查看学校单位未提交草稿',
    update_time = sysdate()
where menu_id = 17982;

update sys_menu
set menu_name = '查看待审核',
    visible = '0',
    status = '0',
    perms = 'crehn:projectView:submitted',
    remark = '允许查看已提交待审核上报',
    update_time = sysdate()
where menu_id = 17983;

update sys_menu
set menu_name = '查看审核通过/已退回',
    visible = '0',
    status = '0',
    perms = 'crehn:projectView:audited',
    remark = '允许查看审核通过和已退回上报',
    update_time = sysdate()
where menu_id = 17984;

update sys_menu
set menu_name = '查看全部单位',
    visible = '0',
    status = '0',
    perms = 'crehn:projectView:all',
    remark = '允许上报进度页面跨学校单位查看全部上报',
    update_time = sysdate()
where menu_id = 17985;

update sys_menu
set menu_name = '删除上报',
    visible = '0',
    status = '0',
    perms = 'crehn:projectView:remove',
    remark = '允许在上报进度页面将上报移入回收站',
    update_time = sysdate()
where menu_id = 17986;

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17000, 17980, 17981)
where r.role_key in ('crehn_admin', 'crehn_project_viewer');

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17982, 17983, 17984, 17985)
where r.role_key = 'crehn_admin';

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id = 17983
where r.role_key = 'crehn_project_viewer';

delete rm
from sys_role_menu rm
join sys_role r on r.role_id = rm.role_id
where r.role_key = 'crehn_project_viewer'
  and rm.menu_id = 17986;
