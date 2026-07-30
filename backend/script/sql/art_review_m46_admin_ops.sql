-- --------------------------------------------------
-- Art Review M4.6 admin operations.
-- Unit/account merged management and upload recycle bin.
-- Safe to rerun after art_review_menu.sql and art_review_config_menu.sql.
-- Chinese labels are written as UTF-8 hex to avoid Windows codepage damage.
-- --------------------------------------------------

-- Repoint the existing unit management menu to the merged unit/account page.
update sys_menu
set menu_name = convert(0xE58D95E4BD8DE4B88EE8B4A6E58FB7 using utf8mb4),
    path = 'school',
    component = 'crehn/school/index',
    perms = 'crehn:schoolInfo:list',
    icon = 'company',
    visible = '0',
    status = '0',
    remark = convert(0xE58D95E4BD8DE6A1A3E6A188E7BBB4E68AA4E5928CE58D95E4BD8DE8B4A6E58FB7E7BB91E5AE9AE7BBB4E68AA4 using utf8mb4)
where menu_id = 17610;

-- Hide the old standalone account management menu. Its permissions remain available for the merged page.
update sys_menu
set menu_name = convert(0xE8B4A6E58FB7E7AEA1E79086 using utf8mb4),
    visible = '1',
    remark = convert(0xE8B4A6E58FB7E5AEA1E6A0B8E38081E6B3A8E5868CE7A081E98280E8AFB7E5928CE79BB4E68EA5E5889BE5BBBAE8B4A6E58FB7 using utf8mb4)
where menu_id = 17002;

-- Upload recycle bin menu and permissions.
insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17700, convert(0xE99984E4BBB6E59B9EE694B6E7AB99 using utf8mb4), 17000, 10, 'recycle', 'crehn/recycle/index', '', 1, 0, 'C', '0', '0', 'crehn:recycle:list', 'redis-list', 103, 1, sysdate(), null, null, convert(0xE5908EE58FB0E5B7B2E588A0E999A4E4B88AE4BCA0E99984E4BBB6E681A2E5A48DE38081E4B88BE8BDBDE5928CE6B885E79086 using utf8mb4)),
(17701, convert(0xE99984E4BBB6E59B9EE694B6E7AB99E69FA5E8AFA2 using utf8mb4), 17700, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:recycle:list', '#', 103, 1, sysdate(), null, null, ''),
(17702, convert(0xE99984E4BBB6E681A2E5A48D using utf8mb4), 17700, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:recycle:restore', '#', 103, 1, sysdate(), null, null, ''),
(17703, convert(0xE99984E4BBB6E6B885E79086 using utf8mb4), 17700, 3, '', '', '', 1, 0, 'F', '0', '0', 'crehn:recycle:remove', '#', 103, 1, sysdate(), null, null, '');

-- Audit closure permission added after M4.6: allow audited pass records to be withdrawn back to submitted.
insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17404, convert(0xE692A4E99480E9809AE8BF87 using utf8mb4), 17004, 4, '', '', '', 1, 0, 'F', '0', '0', 'crehn:audit:withdrawPass', '#', 103, 1, sysdate(), null, null, '');

-- Grant to seeded art admin role when present. Keep account permissions because the merged page embeds account operations.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17610, 17611, 17612, 17613, 17002, 17201, 17202, 17203, 17204, 17205, 17206, 17404, 17700, 17701, 17702, 17703)
where r.role_key = 'crehn_admin';
