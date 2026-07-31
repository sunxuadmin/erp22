-- --------------------------------------------------
-- M10.0 review score-sheet menu, export permission, and default role grants.
-- Safe to rerun after the crehn menu and roles are initialized.
-- admin (super administrator) receives all permissions automatically.
-- --------------------------------------------------

start transaction;

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17930, '评分表模板', 17000, 14, 'review-score-sheet', 'crehn/review-score-sheet/index', null, 1, 0, 'C', '0', '0', 'crehn:reviewSheetTemplate:list', 'document', 103, 1, sysdate(), null, null, '全用户共用的评审打分表模板')
on duplicate key update
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    path = values(path),
    component = values(component),
    query_param = values(query_param),
    is_frame = values(is_frame),
    is_cache = values(is_cache),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    icon = values(icon),
    remark = values(remark),
    update_time = sysdate();

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17931, '评分表模板查询', 17930, 1, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewSheetTemplate:list', '#', 103, 1, sysdate(), null, null, ''),
(17932, '评分表模板修改', 17930, 2, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewSheetTemplate:edit', '#', 103, 1, sysdate(), null, null, ''),
(17923, '评审打分表导出', 17920, 3, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewSheet:export', '#', 103, 1, sysdate(), null, null, '按活动和类别导出当前评审老师已提交评分')
on duplicate key update
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    path = values(path),
    component = values(component),
    query_param = values(query_param),
    is_frame = values(is_frame),
    is_cache = values(is_cache),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    icon = values(icon),
    remark = values(remark),
    update_time = sysdate();

update sys_menu
set order_num = 15,
    update_time = sysdate()
where menu_id = 17940
  and order_num = 14;

-- Template page is shared and editable by project admin, ops admin, and reviewers.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17000, 17930, 17931, 17932)
where r.role_key in ('crehn_admin', 'crehn_ops', 'crehn_expert')
  and r.del_flag = '0';

-- Export remains reviewer-scoped; the backend also enforces the logged-in review assignment.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17000, 17920, 17921, 17922, 17923)
where r.role_key = 'crehn_expert'
  and r.del_flag = '0';

commit;

