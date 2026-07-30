-- --------------------------------------------------
-- M10.9 activity/category scoped score-discrepancy warning configuration.
-- Additive only, existing assignments, scores and generated results remain unchanged.
-- --------------------------------------------------

create table if not exists review_score_warning_config
(
    id                 bigint        not null,
    tenant_id          varchar(20)   default '000000' comment 'tenant id',
    activity_id        bigint        not null comment 'activity id',
    category_id        bigint        not null comment 'activity category id',
    enabled            tinyint(1)    not null default 1 comment 'whether discrepancy warning is enabled',
    formula_type       varchar(32)   not null default 'range_average' comment 'range_average/range_max/range_min',
    threshold_percent  decimal(10,2) not null default 10.00 comment 'warning threshold percentage',
    normal_text        varchar(100)  not null default '正常' comment 'normal display text',
    warning_text       varchar(100)  not null default '需复核' comment 'over-threshold display text',
    insufficient_text  varchar(100)  not null default '数据不足' comment 'fewer than two submitted scores',
    unsupported_text   varchar(100)  not null default '无法计算' comment 'submitted scores cannot be converted to numbers',
    columns_json       text          null comment '分数汇总显示字段配置JSON',
    score_column_count int           not null default 3 comment '默认显示评分列数量',
    create_dept        bigint        default null,
    create_by          bigint        default null,
    create_time        datetime      default null,
    update_by          bigint        default null,
    update_time         datetime      default null,
    remark             varchar(500)  default null,
    del_flag           char(1)       not null default '0',
    primary key (id),
    unique key uk_review_score_warning_scope (tenant_id, activity_id, category_id, del_flag),
    key idx_review_score_warning_activity (tenant_id, activity_id, category_id, enabled)
) engine=innodb comment='评审分数差异预警规则';

alter table review_score_warning_config
    add column if not exists columns_json text null comment '分数汇总显示字段配置JSON';

alter table review_score_warning_config
    add column if not exists score_column_count int not null default 3 comment '默认显示评分列数量';
