-- --------------------------------------------------
-- Art Review M6.2 result visibility and upload summary.
-- Safe to rerun after art_review_m61_award_export_score.sql.
-- --------------------------------------------------

alter table review_result
    add column if not exists show_score tinyint(1) default 0 comment 'school can view score' after result_status,
    add column if not exists show_rank tinyint(1) default 1 comment 'school can view rank' after show_score,
    add column if not exists show_comment tinyint(1) default 0 comment 'school can view review comments' after show_rank;

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17950, convert(0xE4BD9CE59381E6B187E680BB using utf8mb4), 17940, 10, '', '', '', 1, 0, 'F', '0', '0', 'crehn:result:summary', '#', 103, 1, sysdate(), null, null, '');

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17950)
where r.role_key = 'crehn_admin';
