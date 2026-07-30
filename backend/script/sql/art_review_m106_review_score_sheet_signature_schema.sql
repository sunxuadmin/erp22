-- --------------------------------------------------
-- M10.6 additive handwritten-signature library and signed score-sheet audit snapshots.
-- A signed score can appear in only one active signed sheet per tenant.
-- --------------------------------------------------

create table if not exists review_score_sheet_signature
(
    id                bigint        not null,
    tenant_id         varchar(20)   default '000000',
    reviewer_user_id  bigint        not null comment 'signature owner / review expert',
    signature_name    varchar(100)  not null comment 'personal signature label',
    oss_id            bigint        not null comment 'immutable current PNG object id',
    content_sha256    char(64)      not null comment 'PNG content hash for duplicate prevention',
    default_flag      char(1)       not null default '0',
    active_marker     tinyint       null default 1 comment '1 active and null after logical delete so hashes can be reused',
    signature_version bigint        not null default 1,
    create_dept       bigint        null,
    create_by         bigint        null,
    create_time       datetime      null,
    update_by         bigint        null,
    update_time       datetime      null,
    remark            varchar(500)  null,
    del_flag          char(1)       not null default '0',
    primary key (id),
    unique key uk_review_score_sheet_signature_hash (tenant_id, reviewer_user_id, content_sha256, active_marker),
    key idx_review_score_sheet_signature_owner (tenant_id, reviewer_user_id, default_flag, active_marker, del_flag)
) comment = '评审老师个人手写签名库';

create table if not exists review_score_sheet_signed
(
    id                       bigint        not null,
    tenant_id                varchar(20)   default '000000',
    reviewer_user_id         bigint        not null comment 'signed-sheet owner / review expert',
    reviewer_name_snapshot   varchar(100)  not null comment 'reviewer display-name snapshot',
    activity_id              bigint        not null,
    activity_name_snapshot   varchar(200)  null,
    category_id              bigint        not null,
    category_name_snapshot   varchar(200)  null,
    template_id              bigint        null,
    template_version         bigint        null,
    template_snapshot_json   longtext      not null comment 'immutable export template snapshot',
    signature_id             bigint        not null comment 'personal signature record at save time',
    signature_oss_id         bigint        not null comment 'immutable PNG OSS snapshot',
    signature_name_snapshot  varchar(100)  null,
    signature_placement_json text          not null comment 'slot-relative x/y/width/height snapshot',
    score_count              bigint        not null default 0,
    signed_at                datetime      not null comment 'server signing time',
    status                   varchar(32)   not null default 'active',
    active_marker            tinyint       null default 1 comment '1 active and null reserved for future void-and-resign',
    create_dept              bigint        null,
    create_by                bigint        null,
    create_time              datetime      null,
    update_by                bigint        null,
    update_time              datetime      null,
    remark                   varchar(500)  null,
    primary key (id),
    key idx_review_score_sheet_signed_owner (tenant_id, reviewer_user_id, activity_id, category_id, signed_at),
    key idx_review_score_sheet_signed_signature (signature_oss_id)
) comment = '已保存的评审签名打分表快照';

create table if not exists review_score_sheet_signed_item
(
    id                  bigint        not null,
    tenant_id           varchar(20)   default '000000',
    signed_sheet_id     bigint        not null,
    review_score_id     bigint        not null comment 'review_score atomic identity',
    assignment_id       bigint        not null,
    project_id          bigint        not null,
    score_snapshot_json longtext      not null comment 'immutable score-table row snapshot',
    active_marker       tinyint       null default 1 comment '1 active and null reserved for future void-and-resign',
    create_dept         bigint        null,
    create_by           bigint        null,
    create_time         datetime      null,
    update_by           bigint        null,
    update_time         datetime      null,
    remark              varchar(500)  null,
    primary key (id),
    unique key uk_review_score_sheet_signed_item_active (tenant_id, review_score_id, active_marker),
    key idx_review_score_sheet_signed_item_sheet (tenant_id, signed_sheet_id),
    key idx_review_score_sheet_signed_item_project (tenant_id, project_id)
) comment = '评审签名打分表评分明细快照';
