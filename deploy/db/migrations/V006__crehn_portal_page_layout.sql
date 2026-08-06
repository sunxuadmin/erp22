-- V006: 门户页面多布局与主题草稿。
-- 分类: RequiredSchema；前置版本: V005。
-- 影响: 新增独立表和索引，不改写既有门户发布快照或业务数据。
-- 锁表/数据量: 仅创建新表；执行前仍需备份并记录迁移 SHA-256。
-- 发布策略: 先在 TEST 预演和回读，再发布依赖本表的后端；不允许随普通部署猜测执行。
-- 回滚/前向修复: 应用回滚时保留兼容新增表，禁止通过 DROP TABLE 丢弃布局草稿。
-- 验证: 回读表、唯一键 uk_portal_page_layout、uk_portal_page_layout_active 和 active_page_code 生成列。
-- 仅登记结构；执行需独立获得数据库授权。
create table if not exists portal_page_layout (
    id bigint not null,
    tenant_id varchar(20) not null default '000000',
    site_id bigint not null,
    page_code varchar(64) not null default 'home',
    layout_code varchar(64) not null,
    layout_name varchar(128) not null,
    render_version varchar(32) not null default 'v1',
    theme_json text default null,
    component_json longtext not null,
    sort_order int not null default 0,
    enabled tinyint(1) not null default 1,
    active tinyint(1) not null default 0,
    create_dept bigint default null, create_by bigint default null, create_time datetime default null,
    update_by bigint default null, update_time datetime default null,
    del_flag char(1) not null default '0',
    active_page_code varchar(64) as (
        case when active = 1 and enabled = 1 and del_flag = '0' then page_code else null end
    ) persistent,
    primary key (id),
    unique key uk_portal_page_layout (tenant_id, site_id, page_code, layout_code),
    unique key uk_portal_page_layout_active (tenant_id, site_id, active_page_code),
    key idx_portal_page_layout_active (tenant_id, site_id, page_code, active, enabled, sort_order, del_flag)
) engine=innodb comment='门户页面多布局与主题草稿';
