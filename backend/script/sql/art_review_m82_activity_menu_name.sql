-- Safe to rerun. Adds a configurable short label for school-side dynamic activity menus.

alter table activity
    add column if not exists menu_name varchar(64) default null comment '学校端菜单显示名称' after activity_name;

update activity
set menu_name = '2026 大艺展'
where id = 910001
  and (menu_name is null or menu_name = '');

update activity
set menu_name = '2026 大艺展'
where activity_name = '2026 大学生艺术展演节目和作品报送'
  and (menu_name is null or menu_name = '');
