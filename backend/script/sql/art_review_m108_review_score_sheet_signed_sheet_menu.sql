-- --------------------------------------------------
-- M10.8 signed score-sheet summary/my-history menus and withdrawal permissions.
-- Safe to rerun after the crehn menu and signed-score-sheet schema are initialized.
-- super administrators receive all permissions automatically.
-- --------------------------------------------------

start transaction;

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17990, '签名表汇总', 17000, 16, 'signed-sheets', 'crehn/review-score-sheet/signed-sheets/index', null, 1, 0, 'C', '0', '0', 'crehn:reviewSheet:manage', 'collection', 103, 1, sysdate(), null, null, '仅艺术评审管理员查看全部评审老师签名表'),
(17991, '签名表汇总查询', 17990, 1, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewSheet:manage', '#', 103, 1, sysdate(), null, null, ''),
(17992, '签名表强制撤回', 17990, 2, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewSheet:withdraw', '#', 103, 1, sysdate(), null, null, '管理员可撤回任意有效签名表，服务端保留撤回审计'),
(17993, '我的签名表', 17000, 17, 'my-signed-sheets', 'crehn/review-score-sheet/my-signed-sheets/index', null, 1, 0, 'C', '0', '0', 'crehn:reviewSheet:export', 'document', 103, 1, sysdate(), null, null, '评审老师查看本人签名表和撤回历史'),
(17994, '我的签名表查询', 17993, 1, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewSheet:export', '#', 103, 1, sysdate(), null, null, ''),
(17995, '我的签名表撤回', 17993, 2, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewSheet:withdraw', '#', 103, 1, sysdate(), null, null, '评审老师只可撤回本人有效签名表，服务端校验归属')
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

-- Global summary and forced withdrawal are intentionally not granted to crehn_ops.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17990, 17991, 17992)
where r.role_key = 'crehn_admin'
  and r.del_flag = '0';

-- Review experts receive only their own history plus the common withdrawal action.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17993, 17994, 17995)
where r.role_key = 'crehn_expert'
  and r.del_flag = '0';

commit;
