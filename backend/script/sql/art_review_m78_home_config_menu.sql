-- Home page configuration menu.
-- Safe to rerun after RuoYi base tables and art review roles are initialized.

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(18010, '首页配置', 1, 91, 'home-config', 'system/home-config/index', null, 1, 0, 'C', '0', '0', 'system:homeConfig:list', 'dashboard', 103, 1, sysdate(), null, null, '门户首页展演阶段配置')
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
    remark = values(remark);

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(18011, '首页配置查询', 18010, 1, '#', '', null, 1, 0, 'F', '0', '0', 'system:homeConfig:query', '#', 103, 1, sysdate(), null, null, ''),
(18012, '首页配置编辑', 18010, 2, '#', '', null, 1, 0, 'F', '0', '0', 'system:homeConfig:edit', '#', 103, 1, sysdate(), null, null, ''),
(18013, '首页配置删除', 18010, 3, '#', '', null, 1, 0, 'F', '0', '0', 'system:homeConfig:remove', '#', 103, 1, sysdate(), null, null, '')
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
    remark = values(remark);

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (1, 18010, 18011, 18012, 18013)
where r.role_key in ('admin', 'superadmin', 'super_admin');
