-- M11.1 score-summary export permission.
-- The child administrator role is not re-expanded after its initial baseline.

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17955, '评分汇总导出', 17952, 3, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewScoreSummary:export', '#', 103, 1, sysdate(), null, null, '导出全部或当前页分数汇总数据')
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

insert ignore into sys_role_menu(role_id, menu_id)
select role_id, 17955
from sys_role
where role_key = 'crehn_admin'
  and del_flag = '0';

delete role_menu
from sys_role_menu role_menu
join sys_role role_row on role_row.role_id = role_menu.role_id
where role_row.role_key = 'crehn_ops'
  and role_menu.menu_id = 17955;
