-- Rename the reviewer task page without changing its route, permission, or grants.
start transaction;

update sys_menu
set menu_name = convert(0xE8AF84E5AEA1E5B7A5E4BD9CE58FB0 using utf8mb4),
    remark = 'Review workbench',
    update_time = sysdate()
where menu_id = 17920
  and component = 'crehn/review/index';

update sys_menu
set menu_name = convert(0xE8AF84E5AEA1E5B7A5E4BD9CE58FB0E69FA5E8AFA2 using utf8mb4),
    update_time = sysdate()
where menu_id = 17921
  and parent_id = 17920
  and perms = 'crehn:review:task';

commit;
