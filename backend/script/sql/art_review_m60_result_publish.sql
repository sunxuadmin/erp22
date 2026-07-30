-- --------------------------------------------------
-- Art Review M6.0 result aggregation and publishing.
-- Safe to rerun after art_review_menu.sql and art_review_m50_review.sql.
-- Chinese menu labels are written as UTF-8 hex to avoid Windows codepage damage.
-- --------------------------------------------------

create table if not exists review_result
(
    id                 bigint(20)    not null comment 'result id',
    tenant_id          varchar(20)   default '000000' comment 'tenant id',
    activity_id         bigint(20)    not null comment 'activity id',
    category_id         bigint(20)    not null comment 'category id',
    project_id          bigint(20)    not null comment 'project id',
    school_id           bigint(20)    default null comment 'school id',
    score_count         int          default 0 comment 'submitted score count',
    total_score         decimal(10,2) default null comment 'total score',
    average_score       decimal(10,2) default null comment 'average score',
    final_grade         varchar(32)   default null comment 'final grade',
    rank_no             int          default null comment 'rank number',
    score_summary_json  text         null comment 'score summary snapshot json',
    result_status       varchar(32)  default 'draft' comment 'draft/published',
    published_by        bigint(20)   default null comment 'published by',
    published_at        datetime     default null comment 'published time',
    remark              varchar(500) default null comment 'remark',
    create_dept         bigint(20)   default null comment 'create dept',
    create_by           bigint(20)   default null comment 'create by',
    create_time         datetime     default null comment 'create time',
    update_by           bigint(20)   default null comment 'update by',
    update_time         datetime     default null comment 'update time',
    del_flag            char(1)      default '0' comment 'delete flag',
    primary key (id),
    unique key uk_review_result_project (activity_id, category_id, project_id, del_flag),
    key idx_review_result_scope (activity_id, category_id, result_status),
    key idx_review_result_school (school_id, result_status),
    key idx_review_result_rank (activity_id, category_id, rank_no)
) engine=innodb comment='art review result';

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17940, convert(0xE8AF84E5AEA1E7BB93E69E9C using utf8mb4), 17000, 14, 'result', 'crehn/result/index', '', 1, 0, 'C', '0', '0', 'crehn:result:list', 'trophy', 103, 1, sysdate(), null, null, convert(0xE7BB93E69E9CE58F91E5B883E5908EE5ADA6E6A0A1E7ABAFE58FAFE69FA5E79C8B using utf8mb4));

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17941, convert(0xE8AF84E5AEA1E7BB93E69E9CE69FA5E8AFA2 using utf8mb4), 17940, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:result:list', '#', 103, 1, sysdate(), null, null, ''),
(17942, convert(0xE7949FE68890E6B187E680BB using utf8mb4), 17940, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:result:generate', '#', 103, 1, sysdate(), null, null, ''),
(17943, convert(0xE58F91E5B883E7BB93E69E9C using utf8mb4), 17940, 3, '', '', '', 1, 0, 'F', '0', '0', 'crehn:result:publish', '#', 103, 1, sysdate(), null, null, ''),
(17944, convert(0xE692A4E59B9EE58F91E5B883 using utf8mb4), 17940, 4, '', '', '', 1, 0, 'F', '0', '0', 'crehn:result:publish', '#', 103, 1, sysdate(), null, null, ''),
(17945, convert(0xE68891E79A84E7BB93E69E9C using utf8mb4), 17940, 5, '', '', '', 1, 0, 'F', '0', '0', 'crehn:result:school', '#', 103, 1, sysdate(), null, null, '');

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17940, 17941, 17942, 17943, 17944)
where r.role_key = 'crehn_admin';
