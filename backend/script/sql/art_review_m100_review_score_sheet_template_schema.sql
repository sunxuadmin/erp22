-- --------------------------------------------------
-- M10.0 shared review score-sheet template schema.
-- Additive schema only, no runtime template or review-score rows are changed.
-- --------------------------------------------------

create table if not exists review_score_sheet_template
(
    id                    bigint        not null,
    tenant_id             varchar(20)   default '000000',
    template_key          varchar(64)   not null,
    title                 varchar(100)  not null,
    column_config_json    text          not null,
    footer_time_text      varchar(100)  null,
    footer_signature_text varchar(100)  null,
    template_version      bigint        not null default 1,
    create_dept           bigint        null,
    create_by             bigint        null,
    create_time           datetime      null,
    update_by             bigint        null,
    update_time           datetime      null,
    remark                varchar(500)  null,
    del_flag              char(1)       not null default '0',
    primary key (id),
    unique key uk_review_score_sheet_template_key (tenant_id, template_key, del_flag)
) comment = '评审打分表共享模板';
