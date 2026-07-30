-- CREHN independent schema extension.
-- Apply after RuoYi-Vue-Plus base schema and the structural business scripts
-- listed in ../migration-manifest.json. This file contains no activity data.

alter table sys_user
    add column if not exists login_alias varchar(64) default null comment '英文登录别名' after user_name,
    add column if not exists school_id bigint default null comment '所属学校' after user_type,
    add column if not exists school_review_status varchar(32) default null comment '学校账号审核状态' after school_id,
    add column if not exists school_review_by bigint default null after school_review_status,
    add column if not exists school_review_time datetime default null after school_review_by,
    add column if not exists school_review_opinion varchar(500) default null after school_review_time;

create unique index if not exists uk_sys_user_login_alias on sys_user(login_alias);

alter table project
    add column if not exists participant_user_id bigint default null comment '参赛者账号' after school_id,
    add column if not exists participant_submitted_at datetime default null after submitted_at,
    add column if not exists school_review_status varchar(32) default null after participant_submitted_at,
    add column if not exists school_reviewed_at datetime default null after school_review_status,
    add column if not exists school_final_batch_id bigint default null after school_reviewed_at,
    add column if not exists config_version_id bigint default null after school_final_batch_id,
    add column if not exists snapshot_version_id bigint default null after config_version_id,
    add column if not exists row_version bigint not null default 1 after snapshot_version_id;

create index if not exists idx_project_participant_status
    on project(participant_user_id, status, del_flag);

create table if not exists participant_profile (
    id bigint not null,
    tenant_id varchar(20) not null default '000000',
    user_id bigint not null,
    activity_id bigint not null,
    school_id bigint not null,
    participant_name varchar(64) not null,
    identity_type varchar(32) not null,
    identity_no_hash char(64) not null,
    identity_no_masked varchar(64) not null,
    phonenumber varchar(32) default null,
    email varchar(128) default null,
    profile_json text default null,
    status varchar(32) not null default 'pending_confirmation',
    confirmed_at datetime default null,
    row_version bigint not null default 1,
    create_dept bigint default null,
    create_by bigint default null,
    create_time datetime default null,
    update_by bigint default null,
    update_time datetime default null,
    del_flag char(1) not null default '0',
    primary key (id),
    unique key uk_participant_user_activity (tenant_id, user_id, activity_id),
    unique key uk_participant_identity_activity (tenant_id, identity_no_hash, activity_id),
    key idx_participant_school_status (tenant_id, school_id, activity_id, status, del_flag)
) engine=innodb comment='参赛者当前资料';

create table if not exists activation_batch (
    id bigint not null,
    tenant_id varchar(20) not null default '000000',
    batch_no varchar(64) not null,
    activity_id bigint not null,
    school_id bigint not null,
    total_count int not null,
    activated_count int not null default 0,
    expires_at datetime not null,
    status varchar(32) not null,
    remark varchar(500) default null,
    create_dept bigint default null,
    create_by bigint default null,
    create_time datetime default null,
    update_by bigint default null,
    update_time datetime default null,
    del_flag char(1) not null default '0',
    primary key (id),
    unique key uk_activation_batch_no (tenant_id, batch_no),
    key idx_activation_batch_scope (tenant_id, activity_id, school_id, status)
) engine=innodb comment='参赛者激活批次';

create table if not exists participant_activation_code (
    id bigint not null,
    tenant_id varchar(20) not null default '000000',
    batch_id bigint not null,
    participant_id bigint not null,
    code_hash char(64) not null,
    code_hint varchar(8) not null,
    expires_at datetime not null,
    activated_at datetime default null,
    failed_attempts int not null default 0,
    status varchar(32) not null default 'issued',
    create_dept bigint default null,
    create_by bigint default null,
    create_time datetime default null,
    update_by bigint default null,
    update_time datetime default null,
    del_flag char(1) not null default '0',
    primary key (id),
    unique key uk_participant_activation_hash (tenant_id, code_hash),
    key idx_participant_activation_state (tenant_id, participant_id, status, expires_at)
) engine=innodb comment='一次性参赛者激活码';

create table if not exists account_audit (
    id bigint not null,
    tenant_id varchar(20) not null default '000000',
    actor_user_id bigint default null,
    target_user_id bigint not null,
    action varchar(64) not null,
    reason varchar(500) not null,
    ip_address varchar(64) default null,
    metadata_json text default null,
    create_dept bigint default null,
    create_by bigint default null,
    create_time datetime default null,
    update_by bigint default null,
    update_time datetime default null,
    primary key (id),
    key idx_account_audit_target (tenant_id, target_user_id, create_time),
    key idx_account_audit_action (tenant_id, action, create_time)
) engine=innodb comment='账号高风险操作审计';

create table if not exists project_snapshot (
    id bigint not null,
    tenant_id varchar(20) not null default '000000',
    project_id bigint not null,
    version_no int not null,
    snapshot_json longtext not null,
    checksum char(64) not null,
    submitted_by bigint not null,
    submitted_at datetime not null,
    source_snapshot_id bigint default null,
    create_dept bigint default null,
    create_by bigint default null,
    create_time datetime default null,
    update_by bigint default null,
    update_time datetime default null,
    primary key (id),
    unique key uk_project_snapshot_version (tenant_id, project_id, version_no),
    key idx_project_snapshot_checksum (tenant_id, checksum)
) engine=innodb comment='作品不可变提交快照';

create table if not exists school_submission_batch (
    id bigint not null,
    tenant_id varchar(20) not null default '000000',
    batch_no varchar(64) not null,
    activity_id bigint not null,
    school_id bigint not null,
    item_count int not null,
    status varchar(32) not null,
    submitted_by bigint default null,
    submitted_at datetime default null,
    withdrawn_by bigint default null,
    withdrawn_at datetime default null,
    reason varchar(500) default null,
    row_version bigint not null default 1,
    create_dept bigint default null,
    create_by bigint default null,
    create_time datetime default null,
    update_by bigint default null,
    update_time datetime default null,
    del_flag char(1) not null default '0',
    primary key (id),
    unique key uk_school_submission_batch_no (tenant_id, batch_no),
    key idx_school_submission_scope (tenant_id, activity_id, school_id, status, del_flag)
) engine=innodb comment='学校最终提交批次';

create table if not exists school_submission_item (
    id bigint not null,
    tenant_id varchar(20) not null default '000000',
    batch_id bigint not null,
    project_id bigint not null,
    project_snapshot_id bigint not null,
    status varchar(32) not null,
    create_dept bigint default null,
    create_by bigint default null,
    create_time datetime default null,
    update_by bigint default null,
    update_time datetime default null,
    primary key (id),
    unique key uk_school_submission_project (tenant_id, batch_id, project_id),
    key idx_school_submission_item_project (tenant_id, project_id, status)
) engine=innodb comment='学校最终提交明细';

create table if not exists portal_site (
    id bigint not null,
    tenant_id varchar(20) not null default '000000',
    site_code varchar(64) not null,
    site_name varchar(128) not null,
    domain_name varchar(255) default null,
    logo_oss_id bigint default null,
    theme_json text default null,
    seo_json text default null,
    filing_no varchar(128) default null,
    status varchar(32) not null default 'enabled',
    create_dept bigint default null, create_by bigint default null, create_time datetime default null,
    update_by bigint default null, update_time datetime default null, del_flag char(1) not null default '0',
    primary key (id),
    unique key uk_portal_site_code (tenant_id, site_code)
) engine=innodb comment='门户站点';

create table if not exists portal_channel (
    id bigint not null, tenant_id varchar(20) not null default '000000',
    site_id bigint not null, parent_id bigint not null default 0,
    channel_code varchar(64) not null, channel_name varchar(128) not null,
    navigation_position varchar(32) default 'main', sort_order int not null default 0,
    visibility varchar(32) not null default 'public', status varchar(32) not null default 'enabled',
    create_dept bigint default null, create_by bigint default null, create_time datetime default null,
    update_by bigint default null, update_time datetime default null, del_flag char(1) not null default '0',
    primary key (id),
    unique key uk_portal_channel_code (tenant_id, site_id, channel_code),
    key idx_portal_channel_tree (tenant_id, site_id, parent_id, sort_order)
) engine=innodb comment='门户栏目';

create table if not exists portal_article (
    id bigint not null, tenant_id varchar(20) not null default '000000',
    site_id bigint not null, channel_id bigint not null, activity_id bigint default null,
    article_code varchar(64) not null, title varchar(255) not null, summary varchar(1000) default null,
    content_markdown longtext default null, cover_media_id bigint default null,
    author_name varchar(128) default null, source_name varchar(255) default null, tags varchar(1000) default null,
    pinned tinyint(1) not null default 0, visibility varchar(32) not null default 'public',
    status varchar(32) not null default 'DRAFT', scheduled_at datetime default null,
    published_at datetime default null, current_version_id bigint default null, row_version bigint not null default 1,
    create_dept bigint default null, create_by bigint default null, create_time datetime default null,
    update_by bigint default null, update_time datetime default null, del_flag char(1) not null default '0',
    primary key (id),
    unique key uk_portal_article_code (tenant_id, site_id, article_code),
    key idx_portal_article_public (tenant_id, site_id, channel_id, status, visibility, published_at, del_flag)
) engine=innodb comment='门户文章当前草稿';

create table if not exists portal_article_version (
    id bigint not null, tenant_id varchar(20) not null default '000000',
    article_id bigint not null, version_no int not null, snapshot_json longtext not null,
    checksum char(64) not null, published_at datetime not null, published_by bigint not null,
    source_version_id bigint default null,
    create_dept bigint default null, create_by bigint default null, create_time datetime default null,
    update_by bigint default null, update_time datetime default null,
    primary key (id),
    unique key uk_portal_article_version (tenant_id, article_id, version_no)
) engine=innodb comment='门户文章不可变发布版本';

create table if not exists portal_media_asset (
    id bigint not null, tenant_id varchar(20) not null default '000000', site_id bigint not null,
    oss_id bigint not null, media_type varchar(32) not null, title varchar(255) default null,
    alt_text varchar(500) default null, copyright_owner varchar(255) default null,
    source_name varchar(255) default null, content_sha256 char(64) not null,
    scan_status varchar(32) not null default 'pending', status varchar(32) not null default 'draft',
    create_dept bigint default null, create_by bigint default null, create_time datetime default null,
    update_by bigint default null, update_time datetime default null, del_flag char(1) not null default '0',
    primary key (id),
    unique key uk_portal_media_sha (tenant_id, site_id, content_sha256),
    key idx_portal_media_status (tenant_id, site_id, status, scan_status, del_flag)
) engine=innodb comment='门户媒体';

create table if not exists portal_home_component (
    id bigint not null, tenant_id varchar(20) not null default '000000', site_id bigint not null,
    page_code varchar(64) not null default 'home', component_key varchar(64) not null,
    component_type varchar(32) not null, data_source_code varchar(64) not null,
    config_json text default null, grid_x int not null default 0, grid_y int not null default 0,
    grid_w int not null default 12, grid_h int not null default 1, sort_order int not null default 0,
    visible_from datetime default null, visible_until datetime default null, enabled tinyint(1) not null default 1,
    create_dept bigint default null, create_by bigint default null, create_time datetime default null,
    update_by bigint default null, update_time datetime default null, del_flag char(1) not null default '0',
    primary key (id),
    unique key uk_portal_component_key (tenant_id, site_id, page_code, component_key),
    key idx_portal_component_order (tenant_id, site_id, page_code, enabled, sort_order, del_flag)
) engine=innodb comment='门户首页组件草稿';

create table if not exists portal_release (
    id bigint not null, tenant_id varchar(20) not null default '000000', site_id bigint not null,
    version_no int not null, snapshot_json longtext not null, checksum char(64) not null,
    status varchar(32) not null, published_at datetime not null, published_by bigint not null,
    release_reason varchar(500) not null,
    source_release_id bigint default null,
    create_dept bigint default null, create_by bigint default null, create_time datetime default null,
    update_by bigint default null, update_time datetime default null,
    primary key (id),
    unique key uk_portal_release_version (tenant_id, site_id, version_no),
    key idx_portal_release_current (tenant_id, site_id, status, version_no)
) engine=innodb comment='门户不可变发布版本';
