-- --------------------------------------------------
-- M9.4 safe init for report rule center.
-- Scope: create new report_rule tables and menu permissions only.
-- It intentionally does not update activity/category/template runtime config.
-- --------------------------------------------------

create table if not exists report_rule_package
(
    id            bigint(20)   not null comment 'ID',
    tenant_id     varchar(20)  default '000000' comment 'tenant id',
    package_code  varchar(128) not null comment 'package code',
    package_name  varchar(200) not null comment 'package name',
    activity_type varchar(64)  default null comment 'activity type',
    version_no    varchar(64)  default null comment 'version no',
    status        varchar(32)  default 'draft' comment 'draft/published/disabled',
    enabled       tinyint(1)   default 1 comment 'enabled',
    remark        varchar(500) default null comment 'remark',
    create_dept   bigint(20)   default null comment 'create dept',
    create_by     bigint(20)   default null comment 'create by',
    create_time   datetime     default null comment 'create time',
    update_by     bigint(20)   default null comment 'update by',
    update_time   datetime     default null comment 'update time',
    del_flag      char(1)      default '0' comment 'delete flag',
    primary key (id),
    unique key uk_report_rule_package_code (package_code),
    key idx_report_rule_package_status (status, enabled)
) engine=innodb comment='report rule package';

create table if not exists report_rule_item
(
    id                   bigint(20)    not null comment 'ID',
    tenant_id            varchar(20)   default '000000' comment 'tenant id',
    package_id           bigint(20)    not null comment 'package id',
    rule_code            varchar(128)  default null comment 'rule code',
    rule_name            varchar(200)  not null comment 'rule name',
    rule_group           varchar(64)   default 'quota' comment 'quota/ratio/member/file/special',
    rule_type            varchar(64)   default 'count' comment 'count/ratio/member/file/custom',
    scope_type           varchar(64)   default 'activity' comment 'activity/category_group/category/school',
    scope_category_group varchar(128)  default null comment 'scope category group',
    category_code        varchar(128)  default null comment 'category code for reusable mapping',
    category_id          bigint(20)    default null comment 'optional category id',
    school_type          varchar(64)   default null comment 'school type',
    school_id            bigint(20)    default null comment 'school id',
    target_field_key     varchar(128)  default null comment 'target field key',
    target_value         varchar(256)  default null comment 'target value',
    operator             varchar(32)   default null comment 'max/min/eq',
    limit_count          int           default null comment 'limit count',
    ratio_value          decimal(8,2)  default null comment 'ratio value percent',
    min_value            int           default null comment 'min value',
    max_value            int           default null comment 'max value',
    enforce_mode         varchar(32)   default 'warn' comment 'warn/block/manual',
    message              varchar(500)  default null comment 'message',
    rule_json            longtext      null comment 'extended rule json',
    enabled              tinyint(1)    default 1 comment 'enabled',
    sort_order           int           default 0 comment 'sort order',
    remark               varchar(500)  default null comment 'remark',
    create_dept          bigint(20)    default null comment 'create dept',
    create_by            bigint(20)    default null comment 'create by',
    create_time          datetime      default null comment 'create time',
    update_by            bigint(20)    default null comment 'update by',
    update_time          datetime      default null comment 'update time',
    del_flag             char(1)       default '0' comment 'delete flag',
    primary key (id),
    key idx_report_rule_item_package (package_id),
    key idx_report_rule_item_scope (scope_category_group, category_code),
    key idx_report_rule_item_type (rule_group, rule_type)
) engine=innodb comment='report rule item';

create table if not exists activity_report_rule
(
    id                   bigint(20)    not null comment 'ID',
    tenant_id            varchar(20)   default '000000' comment 'tenant id',
    activity_id          bigint(20)    not null comment 'activity id',
    package_id           bigint(20)    default null comment 'source package id',
    package_item_id      bigint(20)    default null comment 'source package item id',
    rule_code            varchar(128)  default null comment 'rule code',
    rule_name            varchar(200)  not null comment 'rule name',
    rule_group           varchar(64)   default 'quota' comment 'quota/ratio/member/file/special',
    rule_type            varchar(64)   default 'count' comment 'count/ratio/member/file/custom',
    scope_type           varchar(64)   default 'activity' comment 'activity/category_group/category/school',
    scope_category_group varchar(128)  default null comment 'scope category group',
    category_code        varchar(128)  default null comment 'category code for reusable mapping',
    category_id          bigint(20)    default null comment 'activity category id',
    school_type          varchar(64)   default null comment 'school type',
    school_id            bigint(20)    default null comment 'school id',
    target_field_key     varchar(128)  default null comment 'target field key',
    target_value         varchar(256)  default null comment 'target value',
    operator             varchar(32)   default null comment 'max/min/eq',
    limit_count          int           default null comment 'limit count',
    ratio_value          decimal(8,2)  default null comment 'ratio value percent',
    min_value            int           default null comment 'min value',
    max_value            int           default null comment 'max value',
    enforce_mode         varchar(32)   default 'warn' comment 'warn/block/manual',
    message              varchar(500)  default null comment 'message',
    rule_json            longtext      null comment 'extended rule json',
    enabled              tinyint(1)    default 1 comment 'enabled',
    sort_order           int           default 0 comment 'sort order',
    remark               varchar(500)  default null comment 'remark',
    create_dept          bigint(20)    default null comment 'create dept',
    create_by            bigint(20)    default null comment 'create by',
    create_time          datetime      default null comment 'create time',
    update_by            bigint(20)    default null comment 'update by',
    update_time          datetime      default null comment 'update time',
    del_flag             char(1)       default '0' comment 'delete flag',
    primary key (id),
    key idx_activity_report_rule_activity (activity_id),
    key idx_activity_report_rule_package (package_id),
    key idx_activity_report_rule_category (category_id, category_code),
    key idx_activity_report_rule_school (school_id, school_type),
    key idx_activity_report_rule_type (rule_group, rule_type)
) engine=innodb comment='activity report rule';

update sys_menu
set visible = '1',
    status = '1',
    perms = '',
    component = '',
    remark = 'Retired quota-rule route. Use report rule center instead.'
where menu_id = 17630;

delete from sys_role_menu where menu_id in (17630, 17631, 17632);

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(18100, '规则配置', 17000, 10, 'report-rule', null, '', 1, 0, 'M', '0', '0', '', 'setting', 103, 1, sysdate(), null, null, '通用报送规则、规则包与活动规则应用'),
(18101, '规则模板包', 18100, 1, 'report-rule-package', 'crehn/report-rule/package/index', '', 1, 0, 'C', '0', '0', 'crehn:reportRule:list', 'collection', 103, 1, sysdate(), null, null, '维护可复用的规则模板包'),
(18102, '当前活动规则', 18100, 2, 'activity-report-rule', 'crehn/report-rule/activity/index', '', 1, 0, 'C', '0', '0', 'crehn:reportRule:list', 'connection', 103, 1, sysdate(), null, null, '维护当前活动实际生效规则'),
(18111, '报送规则保存', 18100, 11, '', '', '', 1, 0, 'F', '0', '0', 'crehn:reportRule:edit', '#', 103, 1, sysdate(), null, null, ''),
(18112, '报送规则删除', 18100, 12, '', '', '', 1, 0, 'F', '0', '0', 'crehn:reportRule:remove', '#', 103, 1, sysdate(), null, null, '');

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (18100, 18101, 18102, 18111, 18112)
where r.role_key = 'crehn_admin';
