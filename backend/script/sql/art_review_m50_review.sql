-- --------------------------------------------------
-- Art Review M5.0 reviewer assignment and scoring.
-- Safe to rerun after art_review_menu.sql.
-- Chinese menu labels are written as UTF-8 hex to avoid Windows codepage damage.
-- --------------------------------------------------

create table if not exists review_assignment
(
    id                     bigint(20)   not null comment 'assignment id',
    tenant_id              varchar(20)  default '000000' comment 'tenant id',
    activity_id             bigint(20)   not null comment 'activity id',
    category_id             bigint(20)   not null comment 'category id',
    project_ids_json        text         null comment 'optional assigned project ids json array',
    reviewer_user_id        bigint(20)   not null comment 'reviewer user id',
    score_mode              varchar(32)  default 'numeric_100' comment 'numeric_100/grade',
    score_rule_json         text         null comment 'score rule json',
    exclusive_mode          varchar(32)  default 'single' comment 'single/multi',
    score_visibility_policy varchar(32)  default 'after_submit' comment 'peer score visibility: hidden/after_submit/always',
    hide_school_info        tinyint(1)   default 0 comment 'hide school and submitter info',
    hide_member_info        tinyint(1)   default 0 comment 'hide member table',
    hidden_field_keys_json  text         null comment 'hidden form field keys json array',
    status                  varchar(32)  default 'active' comment 'active/disabled',
    assigned_by             bigint(20)   default null comment 'assigned by',
    assigned_at             datetime     default null comment 'assigned time',
    create_dept             bigint(20)   default null comment 'create dept',
    create_by               bigint(20)   default null comment 'create by',
    create_time             datetime     default null comment 'create time',
    update_by               bigint(20)   default null comment 'update by',
    update_time             datetime     default null comment 'update time',
    del_flag                char(1)      default '0' comment 'delete flag',
    primary key (id),
    unique key uk_review_assignment_scope_user (activity_id, category_id, reviewer_user_id, del_flag),
    key idx_review_assignment_user (reviewer_user_id, status),
    key idx_review_assignment_scope (activity_id, category_id, status)
) engine=innodb comment='art review assignment';

alter table review_assignment
    add column if not exists project_ids_json text null comment 'optional assigned project ids json array' after category_id;

alter table review_assignment
    add column if not exists score_visibility_policy varchar(32) default 'after_submit' comment 'peer score visibility: hidden/after_submit/always' after exclusive_mode;

create table if not exists review_score
(
    id               bigint(20)    not null comment 'score id',
    tenant_id        varchar(20)   default '000000' comment 'tenant id',
    assignment_id    bigint(20)    not null comment 'assignment id',
    project_id       bigint(20)    not null comment 'project id',
    reviewer_user_id bigint(20)    not null comment 'reviewer user id',
    score_value      decimal(8,2)  default null comment 'numeric score',
    grade_value      varchar(32)   default null comment 'grade score',
    comment_text     text          null comment 'review comment',
    status           varchar(32)   default 'draft' comment 'draft/submitted',
    submitted_at     datetime      default null comment 'submitted time',
    create_dept      bigint(20)    default null comment 'create dept',
    create_by        bigint(20)    default null comment 'create by',
    create_time      datetime      default null comment 'create time',
    update_by        bigint(20)    default null comment 'update by',
    update_time      datetime      default null comment 'update time',
    del_flag         char(1)       default '0' comment 'delete flag',
    primary key (id),
    unique key uk_review_score_assignment_project (assignment_id, project_id, reviewer_user_id, del_flag),
    key idx_review_score_project (project_id, status),
    key idx_review_score_reviewer (reviewer_user_id, status)
) engine=innodb comment='art review score';

insert ignore into sys_role
(role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_dept, create_by, create_time, update_by, update_time, remark)
values
(900104, '000000', convert(0xE889BAE69CAFE8AF84E5AEA1E4B893E5AEB6 using utf8mb4), 'crehn_expert', 93, '5', 1, 1, '0', '0', 103, 1, sysdate(), null, null, 'Art review expert role');

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17900, convert(0xE8AF84E5AEA1E58886E9858D using utf8mb4), 17000, 12, 'review-assignment', 'crehn/review-assignment/index', '', 1, 0, 'C', '0', '0', 'crehn:reviewAssignment:list', 'peoples', 103, 1, sysdate(), null, null, 'Review assignment by activity/category'),
(17920, convert(0xE8AF84E5AEA1E5B7A5E4BD9CE58FB0 using utf8mb4), 17000, 13, 'review', 'crehn/review/index', '', 1, 0, 'C', '0', '0', 'crehn:review:task', 'editPen', 103, 1, sysdate(), null, null, 'Review workbench');

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17901, convert(0xE8AF84E5AEA1E58886E9858DE69FA5E8AFA2 using utf8mb4), 17900, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:reviewAssignment:list', '#', 103, 1, sysdate(), null, null, ''),
(17902, convert(0xE8AF84E5AEA1E58886E9858DE8AFA6E68385 using utf8mb4), 17900, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:reviewAssignment:query', '#', 103, 1, sysdate(), null, null, ''),
(17903, convert(0xE8AF84E5AEA1E58886E9858DE696B0E5A29E using utf8mb4), 17900, 3, '', '', '', 1, 0, 'F', '0', '0', 'crehn:reviewAssignment:add', '#', 103, 1, sysdate(), null, null, ''),
(17904, convert(0xE8AF84E5AEA1E58886E9858DE4BFAEE694B9 using utf8mb4), 17900, 4, '', '', '', 1, 0, 'F', '0', '0', 'crehn:reviewAssignment:edit', '#', 103, 1, sysdate(), null, null, ''),
(17905, convert(0xE8AF84E5AEA1E58886E9858DE588A0E999A4 using utf8mb4), 17900, 5, '', '', '', 1, 0, 'F', '0', '0', 'crehn:reviewAssignment:remove', '#', 103, 1, sysdate(), null, null, ''),
(17921, convert(0xE8AF84E5AEA1E5B7A5E4BD9CE58FB0E69FA5E8AFA2 using utf8mb4), 17920, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:review:task', '#', 103, 1, sysdate(), null, null, ''),
(17922, convert(0xE8AF84E58886E68F90E4BAA4 using utf8mb4), 17920, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:review:score', '#', 103, 1, sysdate(), null, null, '');

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17900, 17901, 17902, 17903, 17904, 17905, 17920, 17921, 17922)
where r.role_key = 'crehn_admin';

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17920, 17921, 17922)
where r.role_key = 'crehn_expert';
