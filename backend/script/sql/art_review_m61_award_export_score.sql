-- --------------------------------------------------
-- Art Review M6.1 award rules, export, score reopen/adjust logs.
-- Safe to rerun after art_review_m60_result_publish.sql.
-- Chinese menu labels are written as UTF-8 hex to avoid Windows codepage damage.
-- --------------------------------------------------

alter table review_result
    add column if not exists award_level varchar(64) default null comment 'award level' after final_grade,
    add column if not exists award_remark varchar(500) default null comment 'award remark' after award_level;

create table if not exists review_award_rule
(
    id            bigint(20)    not null comment 'award rule id',
    tenant_id     varchar(20)   default '000000' comment 'tenant id',
    activity_id   bigint(20)    not null comment 'activity id',
    category_id   bigint(20)    not null comment 'category id',
    award_level   varchar(64)   not null comment 'award level',
    rule_type     varchar(32)   default 'rank_range' comment 'rank_range/score_range',
    min_rank      int           default null comment 'min rank',
    max_rank      int           default null comment 'max rank',
    min_score     decimal(10,2) default null comment 'min score',
    max_score     decimal(10,2) default null comment 'max score',
    sort_order    int           default 0 comment 'sort order',
    enabled       tinyint(1)    default 1 comment 'enabled',
    remark        varchar(500)  default null comment 'remark',
    create_dept   bigint(20)    default null comment 'create dept',
    create_by     bigint(20)    default null comment 'create by',
    create_time   datetime      default null comment 'create time',
    update_by     bigint(20)    default null comment 'update by',
    update_time   datetime      default null comment 'update time',
    del_flag      char(1)       default '0' comment 'delete flag',
    primary key (id),
    key idx_review_award_rule_scope (activity_id, category_id, enabled, sort_order)
) engine=innodb comment='art review award rule';

create table if not exists review_result_log
(
    id            bigint(20)   not null comment 'log id',
    tenant_id     varchar(20)  default '000000' comment 'tenant id',
    activity_id   bigint(20)   default null comment 'activity id',
    category_id   bigint(20)   default null comment 'category id',
    project_id    bigint(20)   default null comment 'project id',
    score_id      bigint(20)   default null comment 'score id',
    target_type   varchar(32)  default null comment 'result/score',
    action_type   varchar(64)  default null comment 'action type',
    before_json   text         null comment 'before json',
    after_json    text         null comment 'after json',
    reason        varchar(500) default null comment 'reason',
    operated_by   bigint(20)   default null comment 'operated by',
    operated_at   datetime     default null comment 'operated time',
    create_dept   bigint(20)   default null comment 'create dept',
    create_by     bigint(20)   default null comment 'create by',
    create_time   datetime     default null comment 'create time',
    update_by     bigint(20)   default null comment 'update by',
    update_time   datetime     default null comment 'update time',
    del_flag      char(1)      default '0' comment 'delete flag',
    primary key (id),
    key idx_review_result_log_scope (activity_id, category_id, project_id),
    key idx_review_result_log_score (score_id),
    key idx_review_result_log_operated (operated_at)
) engine=innodb comment='art review result operation log';

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17946, convert(0xE5A596E9A1B9E8A784E58899 using utf8mb4), 17940, 6, '', '', '', 1, 0, 'F', '0', '0', 'crehn:result:rule', '#', 103, 1, sysdate(), null, null, ''),
(17947, convert(0xE5AFBCE587BAE7BB93E69E9C using utf8mb4), 17940, 7, '', '', '', 1, 0, 'F', '0', '0', 'crehn:result:export', '#', 103, 1, sysdate(), null, null, ''),
(17948, convert(0xE8AF84E58886E7AEA1E79086 using utf8mb4), 17940, 8, '', '', '', 1, 0, 'F', '0', '0', 'crehn:result:score', '#', 103, 1, sysdate(), null, null, ''),
(17949, convert(0xE6938DE4BD9CE697A5E5BF97 using utf8mb4), 17940, 9, '', '', '', 1, 0, 'F', '0', '0', 'crehn:result:log', '#', 103, 1, sysdate(), null, null, '');

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17946, 17947, 17948, 17949)
where r.role_key = 'crehn_admin';
