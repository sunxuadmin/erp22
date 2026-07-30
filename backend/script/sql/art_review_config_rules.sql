-- --------------------------------------------------
-- Art Review configurable rules extension
-- Scope: upload templates, registration auto approve, school binding, school fields, group/quota rules
-- Safe to rerun on MariaDB 10.11+
-- --------------------------------------------------

insert into sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
select 18701, '000000', '艺术评审-侧栏品牌是否显示', 'crehn.sidebar.logo.enabled', 'false', 'Y', 103, 1, sysdate(), null, null, '默认不显示左上角 Logo/工作台；true 时显示'
where not exists (select 1 from sys_config where config_key = 'crehn.sidebar.logo.enabled');

insert into sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
select 18702, '000000', '艺术评审-侧栏品牌文字', 'crehn.sidebar.logo.text', '工作台', 'Y', 103, 1, sysdate(), null, null, '左上角品牌文字，默认工作台'
where not exists (select 1 from sys_config where config_key = 'crehn.sidebar.logo.text');

insert into sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
select 18703, '000000', '艺术评审-侧栏品牌文字颜色', 'crehn.sidebar.logo.textColor', '', 'Y', 103, 1, sysdate(), null, null, '为空则跟随系统主题，可填写 #ffffff 等颜色'
where not exists (select 1 from sys_config where config_key = 'crehn.sidebar.logo.textColor');

insert into sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
select 18704, '000000', '艺术评审-侧栏品牌背景色', 'crehn.sidebar.logo.backgroundColor', '', 'Y', 103, 1, sysdate(), null, null, '为空则跟随系统主题，可填写 #ffffff 等颜色'
where not exists (select 1 from sys_config where config_key = 'crehn.sidebar.logo.backgroundColor');

insert into sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
select 18705, '000000', '艺术评审-工作台顶部单位名称', 'crehn.workbench.navbar.title', '{"visible":true,"title":""}', 'Y', 103, 1, sysdate(), null, null, '工作台顶部居中单位名称配置，title 为空时显示当前学校名称'
where not exists (select 1 from sys_config where config_key = 'crehn.workbench.navbar.title');

insert into sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
select 18706, '000000', 'Art review workbench style', 'crehn.workbench.style', '{"enabled":true,"preset":"sky","pageBackground":"linear-gradient(180deg,#f5f9ff 0%,#f8fbff 44%,#ffffff 100%)","cardBackground":"#ffffff","cardBorder":"#d8e6f5","accentColor":"#2563eb","headingColor":"#0f2f5f","calendarBackground":"linear-gradient(180deg,#2563eb 0%,#0f62b9 100%)","shadow":"0 4px 14px rgba(37,99,235,0.05)","sBg":"#f4f8ff","sBl":0,"sIt":"transparent","sHv":"#e7f0ff","sBd":"#d9e6f6"}', 'Y', 103, 1, sysdate(), null, null, 'Workbench home background/card/sidebar style config JSON'
where not exists (select 1 from sys_config where config_key = 'crehn.workbench.style');

insert into sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
select 18707, '000000', 'Art review workbench security reminder', 'crehn.workbench.security.reminder', '{"enabled":true,"checkDefaultPassword":true,"checkMissingPhone":true,"checkMissingEmail":true}', 'Y', 103, 1, sysdate(), null, null, 'Workbench account security reminder config JSON'
where not exists (select 1 from sys_config where config_key = 'crehn.workbench.security.reminder');

alter table activity
    add column if not exists scope_type varchar(32) default 'all' comment 'all/assigned' after signup_end_at;

alter table activity_category
    add column if not exists category_group varchar(128) default null comment 'template/category group' after quota_limit,
    add column if not exists rule_json text null comment 'category rule json' after category_group,
    add column if not exists tip_text text null comment 'school side tips' after rule_json,
    add column if not exists check_mode varchar(32) default null comment 'manual/auto/mixed' after tip_text;

alter table project
    add column if not exists group_code varchar(64) default null comment 'configurable group code' after project_name,
    add column if not exists group_name varchar(128) default null comment 'configurable group name' after group_code,
    add column if not exists validation_result_json text null comment 'submit validation result' after form_data_json;

alter table category_file_requirement
    add column if not exists rule_json longtext null comment 'extended upload rule json' after required,
    add column if not exists tip_text text null comment 'file requirement upload tip text' after rule_json;

alter table project_file
    add column if not exists preview_oss_id bigint(20) default null comment 'pdf preview oss id' after oss_id,
    add column if not exists preview_path varchar(500) default null comment 'pdf preview url/path' after storage_path,
    add column if not exists preview_ext varchar(32) default null comment 'preview file extension' after preview_path,
    add column if not exists preview_status varchar(32) default null comment 'not_required/converted/failed' after preview_ext,
    add column if not exists preview_message varchar(1000) default null comment 'preview convert message' after preview_status,
    add column if not exists preview_generated_at datetime default null comment 'preview generated time' after preview_message,
    add column if not exists preview_retry_count int default 0 comment 'preview convert retry count' after preview_generated_at,
    add column if not exists preview_started_at datetime default null comment 'preview convert start time' after preview_retry_count,
    add column if not exists media_type varchar(32) default null comment 'image/video/audio/document' after mime_type,
    add column if not exists duration_seconds decimal(12,3) default null comment 'media duration seconds' after media_type,
    add column if not exists width int default null comment 'media width' after duration_seconds,
    add column if not exists height int default null comment 'media height' after width,
    add column if not exists fps decimal(8,3) default null comment 'video fps' after height,
    add column if not exists bitrate bigint(20) default null comment 'video bitrate bps' after fps,
    add column if not exists dpi int default null comment 'image dpi' after bitrate,
    add column if not exists metadata_json longtext null comment 'raw media metadata json' after dpi,
    add column if not exists check_status varchar(32) default null comment 'passed/warning/failed' after metadata_json,
    add column if not exists check_message varchar(1000) default null comment 'technical check message' after check_status;

create index if not exists idx_project_file_preview_status on project_file (preview_status, status);

create table if not exists project_member
(
    id          bigint(20)   not null comment 'ID',
    tenant_id   varchar(20)  default '000000' comment 'tenant id',
    project_id  bigint(20)   not null comment 'project id',
    activity_id bigint(20)   not null comment 'activity id',
    school_id   bigint(20)   not null comment 'school id',
    member_type varchar(32)  not null comment 'student/author/teacher/completer',
    name        varchar(64)  default null comment 'member name',
    student_no  varchar(64)  default null comment 'student no',
    department  varchar(128) default null comment 'department',
    major       varchar(128) default null comment 'major',
    role_name   varchar(128) default null comment 'role name',
    extra_json  longtext     null comment 'dynamic member fields json',
    sort_order  int          default 0 comment 'sort order',
    status      varchar(32)  default 'active' comment 'active/inactive',
    create_dept bigint(20)   default null comment 'create dept',
    create_by   bigint(20)   default null comment 'create by',
    create_time datetime     default null comment 'create time',
    update_by   bigint(20)   default null comment 'update by',
    update_time datetime     default null comment 'update time',
    del_flag    char(1)      default '0' comment 'delete flag',
    primary key (id),
    key idx_project_member_project (project_id),
    key idx_project_member_activity_school (activity_id, school_id),
    key idx_project_member_person (activity_id, school_id, member_type, student_no, name)
) engine=innodb comment='project member';

alter table project_member
    add column if not exists extra_json longtext null comment 'dynamic member fields json' after role_name;

alter table registration_code
    add column if not exists auto_approve tinyint(1) default 0 comment 'auto approve registered account' after role_key;

create table if not exists school_info
(
    id            bigint(20)   not null comment 'ID',
    tenant_id     varchar(20)  default '000000' comment 'tenant id',
    school_name   varchar(128) not null comment 'school name',
    school_code   varchar(64)  default null comment 'school code',
    school_type   varchar(64)  default null comment 'school type',
    region        varchar(128) default null comment 'region',
    address       varchar(255) default null comment 'address',
    contact_name  varchar(64)  default null comment 'contact name',
    contact_phone varchar(32)  default null comment 'contact phone',
    contact_email varchar(128) default null comment 'contact email',
    profile_json  text         null comment 'dynamic school profile json',
    status        varchar(32)  default 'enabled' comment 'enabled/disabled/recycled',
    remark        varchar(500) default null comment 'remark',
    create_dept   bigint(20)   default null comment 'create dept',
    create_by     bigint(20)   default null comment 'create by',
    create_time   datetime     default null comment 'create time',
    update_by     bigint(20)   default null comment 'update by',
    update_time   datetime     default null comment 'update time',
    del_flag      char(1)      default '0' comment 'delete flag',
    primary key (id),
    key idx_school_info_name (school_name),
    key idx_school_info_code (school_code)
) engine=innodb comment='school info';

create table if not exists school_field_schema
(
    id              bigint(20)   not null comment 'ID',
    tenant_id       varchar(20)  default '000000' comment 'tenant id',
    field_key       varchar(64)  not null comment 'field key',
    field_label     varchar(128) not null comment 'field label',
    field_type      varchar(32)  not null comment 'input/textarea/select/radio/checkbox/date/number/phone/email/file',
    required        tinyint(1)   default 0 comment 'required',
    options_json    text         null comment 'options json',
    validation_json text         null comment 'validation json',
    sensitive_flag  tinyint(1)   default 0 comment 'sensitive',
    enabled         tinyint(1)   default 1 comment 'enabled',
    sort_order      int          default 0 comment 'sort order',
    create_dept     bigint(20)   default null comment 'create dept',
    create_by       bigint(20)   default null comment 'create by',
    create_time     datetime     default null comment 'create time',
    update_by       bigint(20)   default null comment 'update by',
    update_time     datetime     default null comment 'update time',
    del_flag        char(1)      default '0' comment 'delete flag',
    primary key (id),
    unique key uk_school_field_key (field_key)
) engine=innodb comment='school field schema';

create table if not exists activity_school_scope
(
    id          bigint(20)   not null comment 'ID',
    tenant_id   varchar(20)  default '000000' comment 'tenant id',
    activity_id bigint(20)   not null comment 'activity id',
    school_id   bigint(20)   not null comment 'school id',
    enabled     tinyint(1)   default 1 comment 'enabled',
    assigned_by bigint(20)   default null comment 'assigned by',
    assigned_at datetime     default null comment 'assigned at',
    remark      varchar(500) default null comment 'remark',
    create_dept bigint(20)   default null comment 'create dept',
    create_by   bigint(20)   default null comment 'create by',
    create_time datetime     default null comment 'create time',
    update_by   bigint(20)   default null comment 'update by',
    update_time datetime     default null comment 'update time',
    del_flag    char(1)      default '0' comment 'delete flag',
    primary key (id),
    unique key uk_activity_school_scope (activity_id, school_id),
    key idx_activity_school_scope_school (school_id)
) engine=innodb comment='activity school authorization';

create table if not exists upload_rule_template
(
    id                    bigint(20)   not null comment 'ID',
    tenant_id             varchar(20)  default '000000' comment 'tenant id',
    template_code         varchar(64)  not null comment 'template code',
    template_name         varchar(128) not null comment 'template name',
    category_group        varchar(128) default null comment 'category group',
    field_schema_json     longtext     null comment 'field schema json',
    file_requirement_json longtext     null comment 'file requirement json',
    rule_json             longtext     null comment 'rule json',
    tip_text              text         null comment 'tip text',
    check_mode            varchar(32)  default null comment 'check mode',
    enabled               tinyint(1)   default 1 comment 'enabled',
    sort_order            int          default 0 comment 'sort order',
    remark                varchar(500) default null comment 'remark',
    create_dept           bigint(20)   default null comment 'create dept',
    create_by             bigint(20)   default null comment 'create by',
    create_time           datetime     default null comment 'create time',
    update_by             bigint(20)   default null comment 'update by',
    update_time           datetime     default null comment 'update time',
    del_flag              char(1)      default '0' comment 'delete flag',
    primary key (id),
    unique key uk_upload_rule_template_code (template_code),
    key idx_upload_rule_template_group (category_group)
) engine=innodb comment='upload rule template';

create table if not exists activity_quota_rule
(
    id          bigint(20)   not null comment 'ID',
    tenant_id   varchar(20)  default '000000' comment 'tenant id',
    activity_id bigint(20)   not null comment 'activity id',
    category_id bigint(20)   default null comment 'category id',
    school_id   bigint(20)   default null comment 'school id',
    school_type varchar(64)  default null comment 'school type',
    group_code  varchar(64)  default null comment 'group code',
    group_name  varchar(128) default null comment 'group name',
    rule_type   varchar(32)  default 'count' comment 'count/ratio/mixed/custom',
    limit_count int          default null comment 'limit count',
    ratio_value decimal(8,2) default null comment 'ratio value percent',
    rule_json   longtext     null comment 'extended rule json',
    enabled     tinyint(1)   default 1 comment 'enabled',
    remark      varchar(500) default null comment 'remark',
    create_dept bigint(20)   default null comment 'create dept',
    create_by   bigint(20)   default null comment 'create by',
    create_time datetime     default null comment 'create time',
    update_by   bigint(20)   default null comment 'update by',
    update_time datetime     default null comment 'update time',
    del_flag    char(1)      default '0' comment 'delete flag',
    primary key (id),
    key idx_activity_quota_rule_activity (activity_id),
    key idx_activity_quota_rule_category (category_id),
    key idx_activity_quota_rule_school (school_id),
    key idx_activity_quota_rule_group (group_code)
) engine=innodb comment='activity quota rule';

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

insert ignore into report_rule_package
(id, tenant_id, package_code, package_name, activity_type, version_no, status, enabled, remark, create_dept, create_by, create_time, del_flag)
values
(190001, '000000', 'henan_art_show_default_2026', '河南高校艺术展演报送规则默认包', 'college_art_show', '2026.06', 'published', 1,
 '当前活动默认规则包；默认提醒不强制，单条规则 enforce_mode 改为 block 后强制拦截。', 103, 1, now(), '0');

insert ignore into report_rule_item
(id, tenant_id, package_id, rule_code, rule_name, rule_group, rule_type, scope_type, scope_category_group, category_code, category_id, school_type, school_id, target_field_key, target_value, operator, limit_count, ratio_value, min_value, max_value, enforce_mode, message, rule_json, enabled, sort_order, remark, create_dept, create_by, create_time, del_flag)
values
(190101,'000000',190001,'performance_total_undergraduate','本科院校艺术表演类总数不超过10','quota','count','category_group','艺术表演类',null,null,'本科院校',null,null,null,'max',10,null,null,null,'warn','本科院校艺术表演类报送总数建议不超过10个','{"countScope":"same_school_category_group","scopeCategoryGroup":"艺术表演类","enforceMode":"warn"}',1,101,'本科/高职高专差异名额',103,1,now(),'0'),
(190102,'000000',190001,'performance_total_vocational','高职高专院校艺术表演类总数不超过6','quota','count','category_group','艺术表演类',null,null,'高职高专',null,null,null,'max',6,null,null,null,'warn','高职高专院校艺术表演类报送总数建议不超过6个','{"countScope":"same_school_category_group","scopeCategoryGroup":"艺术表演类","enforceMode":"warn"}',1,102,'本科/高职高专差异名额',103,1,now(),'0'),
(190103,'000000',190001,'artwork_total_undergraduate','本科院校艺术作品类总数不超过20','quota','count','category_group','艺术作品类',null,null,'本科院校',null,null,null,'max',20,null,null,null,'warn','本科院校艺术作品类报送总数建议不超过20件','{"countScope":"same_school_category_group","scopeCategoryGroup":"艺术作品类","enforceMode":"warn"}',1,103,'本科/高职高专差异名额',103,1,now(),'0'),
(190104,'000000',190001,'artwork_total_vocational','高职高专院校艺术作品类总数不超过20','quota','count','category_group','艺术作品类',null,null,'高职高专',null,null,null,'max',20,null,null,null,'warn','高职高专院校艺术作品类报送总数建议不超过20件','{"countScope":"same_school_category_group","scopeCategoryGroup":"艺术作品类","enforceMode":"warn"}',1,104,'本科/高职高专差异名额',103,1,now(),'0'),
(190105,'000000',190001,'workshop_total_undergraduate','本科院校艺术实践工作坊不超过2','quota','count','category_group','艺术实践工作坊',null,null,'本科院校',null,null,null,'max',2,null,null,null,'warn','本科院校艺术实践工作坊建议不超过2个','{"countScope":"same_school_category_group","scopeCategoryGroup":"艺术实践工作坊","enforceMode":"warn"}',1,105,'本科/高职高专差异名额',103,1,now(),'0'),
(190106,'000000',190001,'workshop_total_vocational','高职高专院校艺术实践工作坊不超过1','quota','count','category_group','艺术实践工作坊',null,null,'高职高专',null,null,null,'max',1,null,null,null,'warn','高职高专院校艺术实践工作坊建议不超过1个','{"countScope":"same_school_category_group","scopeCategoryGroup":"艺术实践工作坊","enforceMode":"warn"}',1,106,'本科/高职高专差异名额',103,1,now(),'0'),
(190107,'000000',190001,'achievement_total_undergraduate','本科院校美育成果不超过4','quota','count','category_group','美育改革创新优秀成果',null,null,'本科院校',null,null,null,'max',4,null,null,null,'warn','本科院校美育改革创新优秀成果建议不超过4项','{"countScope":"same_school_category_group","scopeCategoryGroup":"美育改革创新优秀成果","enforceMode":"warn"}',1,107,'本科/高职高专差异名额',103,1,now(),'0'),
(190108,'000000',190001,'achievement_total_vocational','高职高专院校美育成果不超过2','quota','count','category_group','美育改革创新优秀成果',null,null,'高职高专',null,null,null,'max',2,null,null,null,'warn','高职高专院校美育改革创新优秀成果建议不超过2项','{"countScope":"same_school_category_group","scopeCategoryGroup":"美育改革创新优秀成果","enforceMode":"warn"}',1,108,'本科/高职高专差异名额',103,1,now(),'0'),
(190111,'000000',190001,'performance_vocal_same_school','同校声乐不超过2个','quota','count','category','艺术表演类','performance_vocal',null,null,null,null,null,'max',2,null,null,null,'warn','同一学校同一表演类别节目建议不超过2个','{"countScope":"same_school_category","targetFieldKey":"categoryCode","enforceMode":"warn"}',1,111,'同校同类限制',103,1,now(),'0'),
(190112,'000000',190001,'performance_instrumental_same_school','同校器乐不超过2个','quota','count','category','艺术表演类','performance_instrumental',null,null,null,null,null,'max',2,null,null,null,'warn','同一学校同一表演类别节目建议不超过2个','{"countScope":"same_school_category","targetFieldKey":"categoryCode","enforceMode":"warn"}',1,112,'同校同类限制',103,1,now(),'0'),
(190113,'000000',190001,'performance_dance_same_school','同校舞蹈不超过2个','quota','count','category','艺术表演类','performance_dance',null,null,null,null,null,'max',2,null,null,null,'warn','同一学校同一表演类别节目建议不超过2个','{"countScope":"same_school_category","targetFieldKey":"categoryCode","enforceMode":"warn"}',1,113,'同校同类限制',103,1,now(),'0'),
(190114,'000000',190001,'performance_drama_same_school','同校戏剧/戏曲不超过2个','quota','count','category','艺术表演类','performance_drama',null,null,null,null,null,'max',2,null,null,null,'warn','同一学校同一表演类别节目建议不超过2个','{"countScope":"same_school_category","targetFieldKey":"categoryCode","enforceMode":"warn"}',1,114,'同校同类限制',103,1,now(),'0'),
(190115,'000000',190001,'performance_recitation_same_school','同校朗诵不超过2个','quota','count','category','艺术表演类','performance_recitation',null,null,null,null,null,'max',2,null,null,null,'warn','同一学校同一表演类别节目建议不超过2个','{"countScope":"same_school_category","targetFieldKey":"categoryCode","enforceMode":"warn"}',1,115,'同校同类限制',103,1,now(),'0'),
(190116,'000000',190001,'performance_personal_same_school','同校个人项目不超过2个','quota','count','category','艺术表演类','performance_personal',null,null,null,null,null,'max',2,null,null,null,'warn','同一学校同一表演类别节目建议不超过2个','{"countScope":"same_school_category","targetFieldKey":"categoryCode","enforceMode":"warn"}',1,116,'同校同类限制',103,1,now(),'0'),
(190121,'000000',190001,'performance_group_a_min_60','艺术表演甲组不低于60%','ratio','ratio','category_group','艺术表演类',null,null,null,null,'displayGroup','甲组','min',null,60.00,null,null,'warn','艺术表演类甲组比例建议不低于60%','{"targetFieldKey":"displayGroup","scopeCategoryGroup":"艺术表演类","ratioOperator":"min","enforceMode":"warn"}',1,121,'表演比例',103,1,now(),'0'),
(190122,'000000',190001,'performance_group_b_max_40','艺术表演乙组不超过40%','ratio','ratio','category_group','艺术表演类',null,null,null,null,'displayGroup','乙组','max',null,40.00,null,null,'warn','艺术表演类乙组比例建议不超过40%','{"targetFieldKey":"displayGroup","scopeCategoryGroup":"艺术表演类","ratioOperator":"max","enforceMode":"warn"}',1,122,'表演比例',103,1,now(),'0'),
(190123,'000000',190001,'performance_collective_min_80','艺术表演集体项目不低于80%','ratio','ratio','category_group','艺术表演类',null,null,null,null,'categoryCode',null,'min',null,80.00,null,null,'warn','艺术表演类集体项目比例建议不低于80%','{"targetFieldKey":"categoryCode","scopeCategoryGroup":"艺术表演类","excludeTargetValue":"performance_personal","ratioOperator":"min","enforceMode":"warn"}',1,123,'表演比例',103,1,now(),'0'),
(190124,'000000',190001,'performance_personal_max_20','艺术表演个人项目不超过20%','ratio','ratio','category_group','艺术表演类','performance_personal',null,null,null,'categoryCode','performance_personal','max',null,20.00,null,null,'warn','艺术表演类个人项目比例建议不超过20%','{"targetFieldKey":"categoryCode","scopeCategoryGroup":"艺术表演类","ratioOperator":"max","enforceMode":"warn"}',1,124,'表演比例',103,1,now(),'0'),
(190131,'000000',190001,'performance_vocal_max_30','声乐不超过表演总数30%','ratio','ratio','category_group','艺术表演类','performance_vocal',null,null,null,'categoryCode','performance_vocal','max',null,30.00,null,null,'warn','艺术表演类单类别比例建议不超过30%','{"targetFieldKey":"categoryCode","scopeCategoryGroup":"艺术表演类","ratioOperator":"max","enforceMode":"warn"}',1,131,'单类别比例',103,1,now(),'0'),
(190132,'000000',190001,'performance_instrumental_max_30','器乐不超过表演总数30%','ratio','ratio','category_group','艺术表演类','performance_instrumental',null,null,null,'categoryCode','performance_instrumental','max',null,30.00,null,null,'warn','艺术表演类单类别比例建议不超过30%','{"targetFieldKey":"categoryCode","scopeCategoryGroup":"艺术表演类","ratioOperator":"max","enforceMode":"warn"}',1,132,'单类别比例',103,1,now(),'0'),
(190133,'000000',190001,'performance_dance_max_30','舞蹈不超过表演总数30%','ratio','ratio','category_group','艺术表演类','performance_dance',null,null,null,'categoryCode','performance_dance','max',null,30.00,null,null,'warn','艺术表演类单类别比例建议不超过30%','{"targetFieldKey":"categoryCode","scopeCategoryGroup":"艺术表演类","ratioOperator":"max","enforceMode":"warn"}',1,133,'单类别比例',103,1,now(),'0'),
(190134,'000000',190001,'performance_drama_max_30','戏剧/戏曲不超过表演总数30%','ratio','ratio','category_group','艺术表演类','performance_drama',null,null,null,'categoryCode','performance_drama','max',null,30.00,null,null,'warn','艺术表演类单类别比例建议不超过30%','{"targetFieldKey":"categoryCode","scopeCategoryGroup":"艺术表演类","ratioOperator":"max","enforceMode":"warn"}',1,134,'单类别比例',103,1,now(),'0'),
(190135,'000000',190001,'performance_recitation_max_30','朗诵不超过表演总数30%','ratio','ratio','category_group','艺术表演类','performance_recitation',null,null,null,'categoryCode','performance_recitation','max',null,30.00,null,null,'warn','艺术表演类单类别比例建议不超过30%','{"targetFieldKey":"categoryCode","scopeCategoryGroup":"艺术表演类","ratioOperator":"max","enforceMode":"warn"}',1,135,'单类别比例',103,1,now(),'0'),
(190136,'000000',190001,'performance_personal_max_30','个人项目不超过表演总数30%','ratio','ratio','category_group','艺术表演类','performance_personal',null,null,null,'categoryCode','performance_personal','max',null,30.00,null,null,'warn','艺术表演类单类别比例建议不超过30%','{"targetFieldKey":"categoryCode","scopeCategoryGroup":"艺术表演类","ratioOperator":"max","enforceMode":"warn"}',1,136,'单类别比例',103,1,now(),'0'),
(190141,'000000',190001,'artwork_photography_max_20','摄影不超过艺术作品总数20%','ratio','ratio','category_group','艺术作品类',null,null,null,null,'workType','摄影','max',null,20.00,null,null,'warn','摄影作品比例建议不超过艺术作品总数20%','{"targetFieldKey":"workType","scopeCategoryGroup":"艺术作品类","ratioOperator":"max","enforceMode":"warn"}',1,141,'艺术作品比例',103,1,now(),'0'),
(190151,'000000',190001,'artwork_author_max_3','普通艺术作品作者不超过3人','member','member','category_group','艺术作品类',null,null,null,null,'authorCount',null,'max',null,null,null,3,'warn','普通艺术作品作者不超过3人；影视作品不超过6人','{"memberRole":"author","maxAuthorCount":3,"excludeCategoryCodes":"artwork_film","enforceMode":"warn"}',1,151,'成员规则由分类校验器执行',103,1,now(),'0'),
(190152,'000000',190001,'artwork_film_author_max_6','影视作品作者不超过6人','member','member','category','艺术作品类','artwork_film',null,null,null,'authorCount',null,'max',null,null,null,6,'warn','影视作品作者不超过6人','{"memberRole":"author","maxAuthorCount":6,"enforceMode":"warn"}',1,152,'成员规则由分类校验器执行',103,1,now(),'0'),
(190153,'000000',190001,'artwork_teacher_limits','艺术作品指导教师人数要求','member','member','category_group','艺术作品类',null,null,null,null,'teacherCount',null,'max',null,null,null,3,'warn','普通艺术作品指导教师1人，影视作品不超过3人','{"teacherCount":1,"filmMaxTeacherCount":3,"enforceMode":"warn"}',1,153,'成员规则由分类校验器执行',103,1,now(),'0'),
(190154,'000000',190001,'artwork_creation_desc_400','艺术作品创作说明400字以内','special','custom','category_group','艺术作品类',null,null,null,null,'creationDescription',null,'max',null,null,null,400,'warn','所有艺术作品须附400字以内创作说明','{"maxLength":400,"fieldKeys":["creationDescription","creation_description"],"enforceMode":"warn"}',1,154,'字段规则由分类模板执行',103,1,now(),'0'),
(190161,'000000',190001,'workshop_member_count','工作坊学生7-9人教师1-3人总数不超过12','member','member','category','艺术实践工作坊','workshop',null,null,null,'memberCount',null,'range',null,null,7,12,'warn','工作坊每队不超过12人，其中学生7-9人、指导教师1-3人','{"minStudentCount":7,"maxStudentCount":9,"minTeacherCount":1,"maxTeacherCount":3,"maxTotalMemberCount":12,"enforceMode":"warn"}',1,161,'工作坊成员规则由分类校验器执行',103,1,now(),'0'),
(190162,'000000',190001,'workshop_no_previous_award','历届已获奖工作坊不得重复申报','special','custom','category','艺术实践工作坊','workshop',null,null,null,'noPreviousAwardCommitment',null,'eq',null,null,null,null,'warn','历届已获奖工作坊不得重复申报','{"requiredCommitment":true,"fieldKeys":["noPreviousAwardCommitment","no_previous_award_commitment"],"enforceMode":"warn"}',1,162,'承诺规则由分类模板执行',103,1,now(),'0'),
(190171,'000000',190001,'achievement_paper_author_max_2','学术论文作者不超过2人','member','member','category','美育改革创新优秀成果','achievement_paper',null,null,null,'authorCount',null,'max',null,null,null,2,'warn','学术论文作者不超过2人','{"memberRole":"author","maxAuthorCount":2,"enforceMode":"warn"}',1,171,'成员规则由分类校验器执行',103,1,now(),'0'),
(190172,'000000',190001,'achievement_case_completer_max_3','教学改革案例完成人不超过3人','member','member','category','美育改革创新优秀成果','achievement_case',null,null,null,'completerCount',null,'max',null,null,null,3,'warn','教学改革案例以单位名义提交，完成人不超过3人','{"memberRole":"participant","maxCompleterCount":3,"enforceMode":"warn"}',1,172,'成员规则由分类校验器执行',103,1,now(),'0'),
(190173,'000000',190001,'achievement_case_materials','教学改革案例材料限制','file','file','category','美育改革创新优秀成果','achievement_case',null,null,null,'caseMaterials',null,'max',null,null,null,null,'warn','教学改革案例文字5000字以内，可选1个5分钟以内视频和5张以内图片','{"textMaxLength":5000,"maxVideoCount":1,"videoMaxDurationSeconds":300,"videoMaxMb":1024,"maxImageCount":5,"imageMinMb":10,"imageDpi":300,"enforceMode":"warn"}',1,173,'材料规则由分类模板执行',103,1,now(),'0');

insert ignore into report_rule_item
(id, tenant_id, package_id, rule_code, rule_name, rule_group, rule_type, scope_type, scope_category_group, category_code, category_id, school_type, school_id, target_field_key, target_value, operator, limit_count, ratio_value, min_value, max_value, enforce_mode, message, rule_json, enabled, sort_order, remark, create_dept, create_by, create_time, del_flag)
values
(190181,'000000',190001,'performance_cover_five_categories','艺术表演应覆盖5类','special','custom','category_group','艺术表演类',null,null,null,null,'categoryCode',null,'contains_all',null,null,null,null,'warn','艺术表演类建议覆盖声乐、器乐、舞蹈、戏剧/戏曲、朗诵5类','{"requiredCategoryCodes":["performance_vocal","performance_instrumental","performance_dance","performance_drama","performance_recitation"],"enforceMode":"warn"}',1,181,'覆盖类规则，暂作提醒/复核',103,1,now(),'0'),
(190182,'000000',190001,'performance_personal_student_exclusive','个人项目学生不得与集体项目兼报','special','custom','category_group','艺术表演类','performance_personal',null,null,null,'studentIdentity',null,'unique',null,null,null,null,'warn','每名学生只能报1个个人项目，且不得与集体项目兼报','{"targetCategoryCode":"performance_personal","exclusiveWithCategoryCodes":["performance_vocal","performance_instrumental","performance_dance","performance_drama","performance_recitation"],"enforceMode":"warn"}',1,182,'人员跨项目规则，暂作提醒/复核',103,1,now(),'0'),
(190183,'000000',190001,'artwork_one_work_per_person','艺术作品每人限报1件','special','custom','category_group','艺术作品类',null,null,null,null,'studentIdentity',null,'unique',1,null,null,null,'warn','艺术作品每人限报1件','{"uniquePerPerson":true,"scopeCategoryGroup":"艺术作品类","enforceMode":"warn"}',1,183,'人员跨作品规则，暂作提醒/复核',103,1,now(),'0'),
(190184,'000000',190001,'artwork_same_school_authors','多人创作须为同一学校学生','special','custom','category_group','艺术作品类',null,null,null,null,'authorSchoolId',null,'same_school',null,null,null,null,'warn','多人创作艺术作品的创作者必须为同一学校学生','{"requireSameSchoolAuthors":true,"scopeCategoryGroup":"艺术作品类","enforceMode":"warn"}',1,184,'人员归属规则，暂作提醒/复核',103,1,now(),'0');

set @report_rule_activity_id = 910001;

insert ignore into activity_report_rule
(id, tenant_id, activity_id, package_id, package_item_id, rule_code, rule_name, rule_group, rule_type, scope_type, scope_category_group, category_code, category_id, school_type, school_id, target_field_key, target_value, operator, limit_count, ratio_value, min_value, max_value, enforce_mode, message, rule_json, enabled, sort_order, remark, create_dept, create_by, create_time, del_flag)
select i.id + 10000, i.tenant_id, @report_rule_activity_id, i.package_id, i.id,
       i.rule_code, i.rule_name, i.rule_group, i.rule_type, i.scope_type, i.scope_category_group,
       i.category_code,
       case
           when i.rule_type = 'ratio' and i.scope_category_group is not null and i.target_field_key = 'categoryCode' then null
           else c.id
       end,
       i.school_type, i.school_id, i.target_field_key, i.target_value,
       i.operator, i.limit_count, i.ratio_value, i.min_value, i.max_value, i.enforce_mode,
       i.message, i.rule_json, i.enabled, i.sort_order, i.remark, 103, 1, now(), '0'
from report_rule_item i
left join activity_category c on c.activity_id = @report_rule_activity_id
    and c.category_code = i.category_code
    and c.del_flag = '0'
where i.package_id = 190001
  and i.del_flag = '0'
  and exists (select 1 from activity a where a.id = @report_rule_activity_id and a.del_flag = '0');

create table if not exists notice_template
(
    id               bigint(20)   not null comment 'ID',
    tenant_id        varchar(20)  default '000000' comment 'tenant id',
    template_code    varchar(64)  not null comment 'template code',
    template_name    varchar(128) not null comment 'template name',
    notice_title     varchar(255) not null comment 'notice title',
    notice_content   longtext     not null comment 'notice content',
    custom_css       text         null comment 'custom css',
    cover_oss_id     bigint(20)   default null comment 'cover oss id',
    cover_url        varchar(500) default null comment 'cover url',
    attachment_oss_ids varchar(1000) default null comment 'attachment oss ids',
    notice_group     varchar(128) default null comment 'notice group',
    target_mode      varchar(32)  default 'all' comment 'all/user_type/activity',
    target_user_type varchar(32)  default null comment 'school/expert/null all',
    activity_id      bigint(20)   default null comment 'activity id',
    school_id        bigint(20)   default null comment 'school id',
    school_type      varchar(64)  default null comment 'school type',
    enabled          tinyint(1)   default 1 comment 'enabled',
    sort_order       int          default 0 comment 'sort order',
    remark           varchar(500) default null comment 'remark',
    create_dept      bigint(20)   default null comment 'create dept',
    create_by        bigint(20)   default null comment 'create by',
    create_time      datetime     default null comment 'create time',
    update_by        bigint(20)   default null comment 'update by',
    update_time      datetime     default null comment 'update time',
    del_flag         char(1)      default '0' comment 'delete flag',
    primary key (id),
    unique key uk_notice_template_code (template_code),
    key idx_notice_template_target (target_user_type, activity_id, school_id, school_type)
) engine=innodb comment='notice template';

alter table notice_template
    add column if not exists custom_css text null comment 'custom css' after notice_content,
    add column if not exists cover_oss_id bigint(20) default null comment 'cover oss id' after custom_css,
    add column if not exists cover_url varchar(500) default null comment 'cover url' after cover_oss_id,
    add column if not exists attachment_oss_ids varchar(1000) default null comment 'attachment oss ids' after cover_url,
    add column if not exists target_mode varchar(32) default 'all' comment 'all/user_type/activity' after notice_group;

create unique index if not exists uk_school_info_name_unique on school_info(school_name);

insert into school_field_schema
(id, tenant_id, field_key, field_label, field_type, required, options_json, sensitive_flag, enabled, sort_order, create_dept, create_by, create_time, del_flag)
select 18001, '000000', 'school_name', '学校名称', 'input', 1, null, 0, 1, 10, 103, 1, sysdate(), '0'
where not exists (select 1 from school_field_schema where field_key = 'school_name');

insert into school_field_schema
(id, tenant_id, field_key, field_label, field_type, required, options_json, sensitive_flag, enabled, sort_order, create_dept, create_by, create_time, del_flag)
select 18002, '000000', 'school_code', '统一社会信用代码/学校代码', 'input', 0, null, 0, 1, 20, 103, 1, sysdate(), '0'
where not exists (select 1 from school_field_schema where field_key = 'school_code');

insert into school_field_schema
(id, tenant_id, field_key, field_label, field_type, required, options_json, sensitive_flag, enabled, sort_order, create_dept, create_by, create_time, del_flag)
select 18003, '000000', 'school_type', '学校类型', 'select', 0, '["本科院校","高职高专","其他"]', 0, 1, 30, 103, 1, sysdate(), '0'
where not exists (select 1 from school_field_schema where field_key = 'school_type');

update school_field_schema
set enabled = 0, update_time = sysdate()
where field_key = 'school_type';

insert into school_field_schema
(id, tenant_id, field_key, field_label, field_type, required, options_json, sensitive_flag, enabled, sort_order, create_dept, create_by, create_time, del_flag)
select 18004, '000000', 'contact_name', '联系人姓名', 'input', 1, null, 0, 1, 40, 103, 1, sysdate(), '0'
where not exists (select 1 from school_field_schema where field_key = 'contact_name');

insert into school_field_schema
(id, tenant_id, field_key, field_label, field_type, required, options_json, sensitive_flag, enabled, sort_order, create_dept, create_by, create_time, del_flag)
select 18005, '000000', 'contact_phone', '联系人电话', 'phone', 1, null, 1, 1, 50, 103, 1, sysdate(), '0'
where not exists (select 1 from school_field_schema where field_key = 'contact_phone');

update school_field_schema
set enabled = 0, update_time = sysdate()
where field_key in ('school_code', 'contact_name', 'contact_phone');

insert into school_field_schema
(id, tenant_id, field_key, field_label, field_type, required, options_json, sensitive_flag, enabled, sort_order, create_dept, create_by, create_time, del_flag)
select 18006, '000000', 'province', '省', 'input', 0, null, 0, 1, 20, 103, 1, sysdate(), '0'
where not exists (select 1 from school_field_schema where field_key = 'province');

insert into school_field_schema
(id, tenant_id, field_key, field_label, field_type, required, options_json, sensitive_flag, enabled, sort_order, create_dept, create_by, create_time, del_flag)
select 18007, '000000', 'city', '市', 'input', 0, null, 0, 1, 30, 103, 1, sysdate(), '0'
where not exists (select 1 from school_field_schema where field_key = 'city');

insert into school_field_schema
(id, tenant_id, field_key, field_label, field_type, required, options_json, sensitive_flag, enabled, sort_order, create_dept, create_by, create_time, del_flag)
select 18008, '000000', 'district', '区', 'input', 0, null, 0, 1, 40, 103, 1, sysdate(), '0'
where not exists (select 1 from school_field_schema where field_key = 'district');

insert into school_field_schema
(id, tenant_id, field_key, field_label, field_type, required, options_json, sensitive_flag, enabled, sort_order, create_dept, create_by, create_time, del_flag)
select 18009, '000000', 'leader_name', '负责人', 'input', 0, null, 0, 1, 50, 103, 1, sysdate(), '0'
where not exists (select 1 from school_field_schema where field_key = 'leader_name');

insert into school_field_schema
(id, tenant_id, field_key, field_label, field_type, required, options_json, sensitive_flag, enabled, sort_order, create_dept, create_by, create_time, del_flag)
select 18010, '000000', 'leader_phone', '电话', 'phone', 0, null, 1, 1, 60, 103, 1, sysdate(), '0'
where not exists (select 1 from school_field_schema where field_key = 'leader_phone');

insert into school_field_schema
(id, tenant_id, field_key, field_label, field_type, required, options_json, sensitive_flag, enabled, sort_order, create_dept, create_by, create_time, del_flag)
select 18011, '000000', 'remark', '备注', 'textarea', 0, null, 0, 1, 70, 103, 1, sysdate(), '0'
where not exists (select 1 from school_field_schema where field_key = 'remark');

insert into notice_template
(id, tenant_id, template_code, template_name, notice_title, notice_content, custom_css, notice_group, target_mode, target_user_type, enabled, sort_order, create_dept, create_by, create_time, del_flag)
select 18101, '000000', 'school_register_notice', '学校注册成功公告', '注册成功，请等待审核或直接登录测试',
       '学校账号注册成功后，请关注账号状态。若注册码开启自动审核，可直接登录并选择已授权活动申报；若未开启自动审核，请等待管理员审核通过后再申报。',
       '.notice-title{color:#1f4f82}.notice-content{line-height:1.8}',
       '注册公告', 'user_type', 'school', 1, 10, 103, 1, sysdate(), '0'
where not exists (select 1 from notice_template where template_code = 'school_register_notice');

update notice_template
set target_mode = 'user_type',
    target_user_type = 'school',
    custom_css = '.notice-title{color:#1f4f82}.notice-content{line-height:1.8}',
    update_time = sysdate()
where template_code = 'school_register_notice';

insert into notice_template
(id, tenant_id, template_code, template_name, notice_title, notice_content, custom_css, notice_group, target_mode, target_user_type, enabled, sort_order, create_dept, create_by, create_time, del_flag)
select 18102, '000000', 'home_default_notice', '默认展演阶段', '展演阶段安排',
       '请关注活动启动、作品报送、省级推荐、线上展演与成果展示等阶段安排。具体内容可由后台管理员维护更新。',
       '.notice-title{font-weight:600;color:#1f4f82}.notice-content{line-height:1.8;color:#303133}',
       '展演阶段', 'all', null, 1, 1, 103, 1, sysdate(), '0'
where not exists (select 1 from notice_template where template_code = 'home_default_notice');

update notice_template
set template_name = '默认展演阶段',
    notice_title = '展演阶段安排',
    notice_content = '请关注活动启动、作品报送、省级推荐、线上展演与成果展示等阶段安排。具体内容可由后台管理员维护更新。',
    notice_group = '展演阶段',
    update_time = sysdate()
where template_code = 'home_default_notice'
  and notice_group = '首页公告';

update notice_template
set notice_group = '展演阶段',
    update_time = sysdate()
where notice_group = '首页公告';

insert into upload_rule_template
(id, tenant_id, template_code, template_name, category_group, field_schema_json, file_requirement_json, rule_json, tip_text, check_mode, enabled, sort_order, remark, create_dept, create_by, create_time, del_flag)
select 18201, '000000', 'tpl_art_performance', '艺术表演节目申报模板', '艺术表演节目',
       '[{"fieldKey":"work_name","fieldLabel":"节目名称","fieldType":"input","required":true,"sortOrder":1},{"fieldKey":"team_members","fieldLabel":"团队成员","fieldType":"textarea","required":true,"sortOrder":2},{"fieldKey":"teacher_name","fieldLabel":"指导教师","fieldType":"input","required":false,"sortOrder":3},{"fieldKey":"is_original","fieldLabel":"是否原创","fieldType":"radio","required":true,"optionsJson":"[\"是\",\"否\"]","sortOrder":4},{"fieldKey":"authorization_commitment","fieldLabel":"授权承诺","fieldType":"checkbox","required":true,"optionsJson":"[\"确认拥有表演、展示和传播所需授权\"]","sortOrder":5}]',
       '[{"fileTypeCode":"video","fileTypeName":"节目视频","allowedExt":"mp4,mov","maxSizeMb":1024,"minCount":1,"maxCount":1,"required":true,"sortOrder":1},{"fileTypeCode":"document","fileTypeName":"申报表","allowedExt":"doc,docx,pdf","maxSizeMb":50,"minCount":1,"maxCount":1,"required":true,"sortOrder":2}]',
       '{"groupEnabled":true,"ratioDisabled":false,"requiresOriginality":true}', '请按活动规则选择组别并上传节目视频。', 'mixed', 1, 10, '系统预置模板', 103, 1, sysdate(), '0'
where not exists (select 1 from upload_rule_template where template_code = 'tpl_art_performance');

insert into upload_rule_template
(id, tenant_id, template_code, template_name, category_group, field_schema_json, file_requirement_json, rule_json, tip_text, check_mode, enabled, sort_order, remark, create_dept, create_by, create_time, del_flag)
select 18202, '000000', 'tpl_art_work', '艺术作品申报模板', '艺术作品',
       '[{"fieldKey":"work_name","fieldLabel":"作品名称","fieldType":"input","required":true,"sortOrder":1},{"fieldKey":"work_type","fieldLabel":"作品类别","fieldType":"input","required":false,"sortOrder":2},{"fieldKey":"work_size","fieldLabel":"作品尺寸","fieldType":"input","required":false,"sortOrder":3},{"fieldKey":"is_original","fieldLabel":"是否原创","fieldType":"radio","required":true,"optionsJson":"[\"是\",\"否\"]","sortOrder":4},{"fieldKey":"original_commitment","fieldLabel":"原创承诺","fieldType":"checkbox","required":true,"optionsJson":"[\"本人承诺作品原创或已获得合法授权\"]","sortOrder":5}]',
       '[{"fileTypeCode":"image","fileTypeName":"作品图片","allowedExt":"jpg,jpeg,png","maxSizeMb":50,"minCount":1,"maxCount":5,"required":true,"sortOrder":1},{"fileTypeCode":"document","fileTypeName":"申报说明","allowedExt":"doc,docx,pdf","maxSizeMb":50,"minCount":0,"maxCount":1,"required":false,"sortOrder":2}]',
       '{"groupEnabled":true,"ratioDisabled":false,"requiresOriginality":true}', '请上传清晰作品图片，格式和数量以当前活动类别要求为准。', 'mixed', 1, 20, '系统预置模板', 103, 1, sysdate(), '0'
where not exists (select 1 from upload_rule_template where template_code = 'tpl_art_work');

insert into upload_rule_template
(id, tenant_id, template_code, template_name, category_group, field_schema_json, file_requirement_json, rule_json, tip_text, check_mode, enabled, sort_order, remark, create_dept, create_by, create_time, del_flag)
select 18203, '000000', 'tpl_workshop', '艺术实践工作坊申报模板', '艺术实践工作坊',
       '[{"fieldKey":"project_name","fieldLabel":"工作坊名称","fieldType":"input","required":true,"sortOrder":1},{"fieldKey":"leader_name","fieldLabel":"项目负责人","fieldType":"input","required":true,"sortOrder":2},{"fieldKey":"leader_phone","fieldLabel":"负责人电话","fieldType":"phone","required":true,"sensitive":true,"sortOrder":3},{"fieldKey":"work_desc","fieldLabel":"工作坊说明","fieldType":"textarea","required":true,"sortOrder":4}]',
       '[{"fileTypeCode":"document","fileTypeName":"方案文档","allowedExt":"doc,docx,pdf","maxSizeMb":100,"minCount":1,"maxCount":2,"required":true,"sortOrder":1},{"fileTypeCode":"archive","fileTypeName":"支撑材料压缩包","allowedExt":"zip,rar,7z","maxSizeMb":500,"minCount":0,"maxCount":1,"required":false,"sortOrder":2}]',
       '{"groupEnabled":false,"ratioDisabled":true,"requiresOriginality":false}', '请上传工作坊方案和支撑材料。', 'manual', 1, 30, '系统预置模板', 103, 1, sysdate(), '0'
where not exists (select 1 from upload_rule_template where template_code = 'tpl_workshop');

insert into upload_rule_template
(id, tenant_id, template_code, template_name, category_group, field_schema_json, file_requirement_json, rule_json, tip_text, check_mode, enabled, sort_order, remark, create_dept, create_by, create_time, del_flag)
select 18204, '000000', 'tpl_reform_result', '高校美育改革创新成果模板', '高校美育改革创新成果',
       '[{"fieldKey":"project_name","fieldLabel":"成果名称","fieldType":"input","required":true,"sortOrder":1},{"fieldKey":"leader_name","fieldLabel":"项目负责人","fieldType":"input","required":true,"sortOrder":2},{"fieldKey":"teacher_unit","fieldLabel":"负责人单位","fieldType":"input","required":false,"sortOrder":3},{"fieldKey":"work_desc","fieldLabel":"成果说明","fieldType":"textarea","required":true,"sortOrder":4},{"fieldKey":"display_authorization","fieldLabel":"获奖展示授权确认","fieldType":"checkbox","required":true,"optionsJson":"[\"同意用于本活动获奖展示和宣传\"]","sortOrder":5}]',
       '[{"fileTypeCode":"document","fileTypeName":"成果报告","allowedExt":"doc,docx,pdf","maxSizeMb":100,"minCount":1,"maxCount":1,"required":true,"sortOrder":1},{"fileTypeCode":"material","fileTypeName":"佐证材料","allowedExt":"所有格式","maxSizeMb":1024,"minCount":0,"maxCount":5,"required":false,"sortOrder":2}]',
       '{"groupEnabled":false,"ratioDisabled":true,"requiresOriginality":false}', '请上传成果报告和必要佐证材料。', 'manual', 1, 40, '系统预置模板', 103, 1, sysdate(), '0'
where not exists (select 1 from upload_rule_template where template_code = 'tpl_reform_result');

update upload_rule_template
set template_name = '艺术表演节目上传规则包',
    category_group = '艺术表演节目',
    field_schema_json = '[{"fieldKey":"groupName","fieldLabel":"组别","fieldType":"select","required":true,"optionsJson":"[\"甲组\",\"乙组\",\"高校组\",\"中小学组\"]","sortOrder":1},{"fieldKey":"projectType","fieldLabel":"项目类型","fieldType":"select","required":true,"optionsJson":"[\"声乐\",\"器乐\",\"舞蹈\",\"戏剧\",\"朗诵\"]","sortOrder":2},{"fieldKey":"programForm","fieldLabel":"节目形式","fieldType":"select","required":true,"optionsJson":"[\"独唱\",\"重唱\",\"合唱\",\"独奏\",\"合奏\",\"群舞\",\"小品\",\"戏曲\",\"朗诵\"]","sortOrder":3},{"fieldKey":"projectNature","fieldLabel":"项目性质","fieldType":"select","required":true,"optionsJson":"[\"个人\",\"集体\"]","sortOrder":4},{"fieldKey":"performanceOrder","fieldLabel":"展演顺序","fieldType":"input","required":false,"validationJson":"{\"type\":\"number\"}","sortOrder":5},{"fieldKey":"original_commitment","fieldLabel":"原创承诺","fieldType":"checkbox","required":true,"optionsJson":"[\"已确认原创或具备授权\"]","sortOrder":6},{"fieldKey":"authorization_commitment","fieldLabel":"授权承诺","fieldType":"checkbox","required":true,"optionsJson":"[\"已获得展演传播所需授权\"]","sortOrder":7},{"fieldKey":"no_identity_in_video","fieldLabel":"视频无身份信息确认","fieldType":"checkbox","required":true,"optionsJson":"[\"视频中不出现省份、学校、姓名、指导教师姓名\"]","sortOrder":8}]',
    file_requirement_json = '[{"fileTypeCode":"video","fileTypeName":"节目视频","allowedExt":"mp4,mov","maxSizeMb":1024,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"mediaType\":\"video\",\"maxMb\":1024,\"minWidth\":1920,\"minHeight\":1080,\"fps\":25,\"fpsTolerance\":0.5,\"minBitrateMbps\":10,\"minDurationSeconds\":30,\"maxDurationSeconds\":480,\"manualCheckTips\":[\"视频中不得出现省份、学校、姓名、指导教师姓名。\",\"核验原创或授权承诺。\"]}","sortOrder":1},{"fileTypeCode":"document","fileTypeName":"节目申报材料","allowedExt":"doc,docx,pdf","maxSizeMb":50,"minCount":0,"maxCount":2,"required":false,"ruleJson":"{\"maxMb\":50,\"filenamePattern\":\"(?i).*(申报|报名|program|application).*\",\"manualCheckTips\":[\"申报材料需与系统字段、成员名单保持一致。\"]}","sortOrder":2}]',
    rule_json = '{"validatorType":"performance","maxTeacherCount":3,"maxVideoMb":1024,"minVideoWidth":1920,"minVideoHeight":1080,"fps":25,"fpsTolerance":0.5,"minBitrateMbps":10,"maxDurationSecondsByProjectNature":{"个人":300,"集体":480,"individual":300,"collective":480},"canonicalFieldKeys":{"groupName":["groupName"],"projectType":["projectType"],"programForm":["programForm"],"performanceOrder":["performanceOrder"],"originalityCommitment":["original_commitment"]},"memberFieldGroups":{"participant":[{"fieldKey":"name","fieldLabel":"姓名","fieldType":"input","required":true,"sortOrder":1},{"fieldKey":"studentNo","fieldLabel":"学号","fieldType":"input","required":false,"sortOrder":2},{"fieldKey":"department","fieldLabel":"院系","fieldType":"input","required":false,"sortOrder":3},{"fieldKey":"major","fieldLabel":"专业","fieldType":"input","required":false,"sortOrder":4},{"fieldKey":"roleName","fieldLabel":"角色/声部/岗位","fieldType":"input","required":false,"sortOrder":5}]},"manualCheckTips":["视频中不得出现省份、学校、姓名、指导教师姓名。","系统仅校验承诺勾选，原创/授权真实性需人工复核。","展演顺序用于抽签或后台排序展示，需由管理员复核。"],"rejectReasonTemplates":["视频技术参数不符合要求。","视频出现学校或人员身份信息。","原创或授权承诺材料不足。","节目类型、节目形式、组别或成员名单填写不完整。"]}',
    tip_text = '请完整填写组别、项目类型、节目形式、项目性质和展演顺序，并上传 MP4/MOV 节目视频。系统将校验分辨率、帧率、码率、时长和大小；身份信息、原创授权由承诺与人工审核结合判断。',
    check_mode = 'mixed',
    update_time = sysdate()
where template_code = 'tpl_art_performance';

update upload_rule_template
set template_name = '艺术作品上传规则包',
    category_group = '艺术作品',
    field_schema_json = '[{"fieldKey":"groupName","fieldLabel":"组别","fieldType":"select","required":true,"optionsJson":"[\"甲组\",\"乙组\",\"高校组\",\"中小学组\"]","sortOrder":1},{"fieldKey":"projectType","fieldLabel":"项目类型","fieldType":"select","required":true,"optionsJson":"[\"绘画\",\"书法\",\"篆刻\",\"摄影\",\"设计\",\"微电影\",\"短视频\",\"AIGC动画\"]","sortOrder":2},{"fieldKey":"programForm","fieldLabel":"作品形式","fieldType":"select","required":true,"optionsJson":"[\"普通作品\",\"纪录短片\",\"剧情短片\",\"AIGC动画短片\"]","sortOrder":3},{"fieldKey":"work_type","fieldLabel":"作品类型","fieldType":"select","required":true,"optionsJson":"[\"普通作品\",\"纪录短片\",\"剧情短片\",\"AIGC动画短片\"]","sortOrder":4},{"fieldKey":"work_size","fieldLabel":"作品尺寸/规格","fieldType":"input","required":false,"sortOrder":5},{"fieldKey":"creation_description","fieldLabel":"创作说明","fieldType":"textarea","required":true,"validationJson":"{\"maxLength\":400}","sortOrder":6},{"fieldKey":"ai_usage_ratio","fieldLabel":"AI工具使用比例","fieldType":"input","required":false,"sortOrder":7},{"fieldKey":"ai_process_description","fieldLabel":"AI创作流程简述","fieldType":"textarea","required":false,"sortOrder":8},{"fieldKey":"original_commitment","fieldLabel":"原创承诺","fieldType":"checkbox","required":true,"optionsJson":"[\"本人承诺作品原创或已获得合法授权\"]","sortOrder":9}]',
    file_requirement_json = '[{"fileTypeCode":"image","fileTypeName":"非影视作品图片","allowedExt":"jpg,jpeg","maxSizeMb":100,"minCount":1,"maxCount":5,"required":false,"ruleJson":"{\"mediaType\":\"image\",\"minMb\":5,\"maxMb\":100,\"dpi\":300,\"manualCheckTips\":[\"DPI缺失时需人工确认是否达到300dpi。\",\"图片需能清晰呈现作品整体和细节。\"]}","sortOrder":1},{"fileTypeCode":"video","fileTypeName":"影视作品视频","allowedExt":"mp4,mov","maxSizeMb":1024,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"mediaType\":\"video\",\"maxMb\":1024,\"minBitrateMbps\":10,\"minDurationSeconds\":30,\"maxDurationSeconds\":480,\"manualCheckTips\":[\"短片类别、时长和AIGC说明需人工复核。\"]}","sortOrder":2},{"fileTypeCode":"document","fileTypeName":"作品说明或授权材料","allowedExt":"doc,docx,pdf","maxSizeMb":50,"minCount":0,"maxCount":2,"required":false,"ruleJson":"{\"maxMb\":50,\"manualCheckTips\":[\"授权、创作说明、AIGC过程材料需与申报字段一致。\"]}","sortOrder":3}]',
    rule_json = '{"validatorType":"artwork","maxAuthorCount":3,"filmMaxAuthorCount":6,"teacherCount":1,"filmMaxTeacherCount":3,"creationDescriptionMaxLength":400,"imageMinMb":5,"imageMinDpi":300,"videoMaxMb":1024,"filmVideoRules":{"纪录短片":{"maxDurationSeconds":480,"minBitrateMbps":10},"剧情短片":{"maxDurationSeconds":480,"minBitrateMbps":10},"AIGC动画短片":{"maxDurationSeconds":300,"minBitrateMbps":10},"documentary":{"maxDurationSeconds":480,"minBitrateMbps":10},"feature":{"maxDurationSeconds":480,"minBitrateMbps":10},"aigc":{"maxDurationSeconds":300,"minBitrateMbps":10}},"canonicalFieldKeys":{"groupName":["groupName"],"projectType":["projectType"],"programForm":["programForm"],"originalityCommitment":["original_commitment"]},"memberFieldGroups":{"author":[{"fieldKey":"name","fieldLabel":"作者姓名","fieldType":"input","required":true,"sortOrder":1},{"fieldKey":"studentNo","fieldLabel":"学号","fieldType":"input","required":false,"sortOrder":2},{"fieldKey":"department","fieldLabel":"院系","fieldType":"input","required":false,"sortOrder":3},{"fieldKey":"major","fieldLabel":"专业","fieldType":"input","required":false,"sortOrder":4},{"fieldKey":"roleName","fieldLabel":"分工","fieldType":"input","required":false,"sortOrder":5}],"participant":[{"fieldKey":"name","fieldLabel":"指导教师姓名","fieldType":"input","required":true,"sortOrder":1},{"fieldKey":"department","fieldLabel":"所在单位/院系","fieldType":"input","required":false,"sortOrder":2}]},"manualCheckTips":["普通作品作者不超过3人，影视作者不超过6人。","AIGC动画短片需核验AI工具使用比例和创作流程。","非影视图片DPI缺失时需人工确认。"],"rejectReasonTemplates":["作品图片格式、大小或DPI不符合要求。","作者或指导教师人数超限。","AIGC创作过程说明不完整。","作品类型、作品形式、创作说明或授权材料填写不完整。"]}',
    tip_text = '请按作品类型填写组别、项目类型、作品形式和创作说明。非影视作品上传 JPG/JPEG 图片；影视类按短片类别上传视频。作者、指导教师和每人限报将在提交时校验。',
    check_mode = 'mixed',
    update_time = sysdate()
where template_code = 'tpl_art_work';

update upload_rule_template
set template_name = '艺术实践工作坊上传规则包',
    category_group = '艺术实践工作坊',
    field_schema_json = '[{"fieldKey":"groupName","fieldLabel":"组别","fieldType":"select","required":true,"optionsJson":"[\"高校组\",\"中小学组\"]","sortOrder":1},{"fieldKey":"projectType","fieldLabel":"项目类型","fieldType":"select","required":true,"optionsJson":"[\"艺术实践工作坊\"]","sortOrder":2},{"fieldKey":"programForm","fieldLabel":"工作坊形式","fieldType":"select","required":true,"optionsJson":"[\"美术工作坊\",\"设计工作坊\",\"音乐工作坊\",\"舞蹈工作坊\",\"戏剧工作坊\",\"综合实践工作坊\"]","sortOrder":3},{"fieldKey":"project_intro","fieldLabel":"项目简介","fieldType":"textarea","required":true,"sortOrder":4},{"fieldKey":"design_idea","fieldLabel":"设计思路","fieldType":"textarea","required":true,"sortOrder":5},{"fieldKey":"feature_desc","fieldLabel":"特色描述","fieldType":"textarea","required":true,"sortOrder":6},{"fieldKey":"exhibition_design_plan","fieldLabel":"展区设计方案","fieldType":"textarea","required":true,"sortOrder":7},{"fieldKey":"no_previous_award_commitment","fieldLabel":"未获奖工作坊确认","fieldType":"checkbox","required":true,"optionsJson":"[\"确认不是历届已获奖工作坊\"]","sortOrder":8}]',
    file_requirement_json = '[{"fileTypeCode":"video","fileTypeName":"工作坊视频","allowedExt":"mp4,mpg,mpeg","maxSizeMb":1024,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"mediaType\":\"video\",\"maxMb\":1024,\"minDurationSeconds\":60,\"maxDurationSeconds\":480,\"manualCheckTips\":[\"核验工作坊不是历届已获奖工作坊。\",\"视频需呈现工作坊实施过程和成果。\"]}","sortOrder":1},{"fileTypeCode":"document","fileTypeName":"工作坊方案","allowedExt":"doc,docx,pdf","maxSizeMb":100,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"maxMb\":100,\"filenamePattern\":\"(?i).*(方案|plan|workshop).*\",\"manualCheckTips\":[\"方案需包含项目简介、设计思路、特色描述和展区设计。\"]}","sortOrder":2},{"fileTypeCode":"archive","fileTypeName":"支撑材料压缩包","allowedExt":"zip,rar,7z","maxSizeMb":500,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"maxMb\":500,\"manualCheckTips\":[\"压缩包仅作为辅助佐证材料，需人工确认目录和内容。\"]}","sortOrder":3}]',
    rule_json = '{"validatorType":"workshop","minStudentCount":7,"maxStudentCount":9,"minTeacherCount":1,"maxTeacherCount":3,"maxTotalMemberCount":12,"maxVideoDurationSeconds":480,"canonicalFieldKeys":{"groupName":["groupName"],"projectType":["projectType"],"programForm":["programForm"]},"memberFieldGroups":{"participant":[{"fieldKey":"name","fieldLabel":"姓名","fieldType":"input","required":true,"sortOrder":1},{"fieldKey":"studentNo","fieldLabel":"学号","fieldType":"input","required":false,"sortOrder":2},{"fieldKey":"department","fieldLabel":"院系","fieldType":"input","required":false,"sortOrder":3},{"fieldKey":"major","fieldLabel":"专业","fieldType":"input","required":false,"sortOrder":4},{"fieldKey":"roleName","fieldLabel":"成员角色","fieldType":"select","required":true,"optionsJson":"[\"学生\",\"指导教师\",\"负责人\"]","sortOrder":5}]},"manualCheckTips":["学生7-9人、指导教师1-3人，总人数不超过12人。","一校一队一坊，历届已获奖工作坊不得重复申报。","展区设计方案和视频内容需人工核对一致性。"],"rejectReasonTemplates":["成员人数不符合工作坊要求。","视频时长或格式不符合要求。","项目简介、设计思路、特色描述或展区设计方案缺失。","工作坊方案或支撑材料与填报信息不一致。"]}',
    tip_text = '请维护学生和指导教师成员列表，填写工作坊形式、项目简介、设计思路、特色描述和展区设计方案，并上传不超过8分钟的工作坊视频。',
    check_mode = 'mixed',
    update_time = sysdate()
where template_code = 'tpl_workshop';

update upload_rule_template
set template_name = '高校美育改革创新成果上传规则包',
    category_group = '高校美育改革创新成果',
    field_schema_json = '[{"fieldKey":"groupName","fieldLabel":"组别","fieldType":"select","required":false,"optionsJson":"[\"高校组\",\"本科院校\",\"高职高专\"]","sortOrder":1},{"fieldKey":"projectType","fieldLabel":"项目类型","fieldType":"select","required":true,"optionsJson":"[\"高校美育改革创新成果\"]","sortOrder":2},{"fieldKey":"programForm","fieldLabel":"成果形式","fieldType":"select","required":true,"optionsJson":"[\"学术论文\",\"教学改革案例\"]","sortOrder":3},{"fieldKey":"achievement_type","fieldLabel":"成果类型","fieldType":"select","required":true,"optionsJson":"[\"学术论文\",\"教学改革案例\"]","sortOrder":4},{"fieldKey":"abstract","fieldLabel":"摘要","fieldType":"textarea","required":false,"sortOrder":5},{"fieldKey":"keywords","fieldLabel":"关键词","fieldType":"input","required":false,"sortOrder":6},{"fieldKey":"body_text","fieldLabel":"正文/案例正文","fieldType":"textarea","required":true,"sortOrder":7},{"fieldKey":"references","fieldLabel":"参考文献","fieldType":"textarea","required":false,"sortOrder":8},{"fieldKey":"unpublished_commitment","fieldLabel":"未公开发表承诺","fieldType":"checkbox","required":false,"optionsJson":"[\"承诺未公开发表\"]","sortOrder":9},{"fieldKey":"unit_name","fieldLabel":"提交单位","fieldType":"input","required":false,"sortOrder":10}]',
    file_requirement_json = '[{"fileTypeCode":"document","fileTypeName":"成果正文或附件","allowedExt":"doc,docx,pdf","maxSizeMb":100,"minCount":1,"maxCount":3,"required":true,"ruleJson":"{\"maxMb\":100,\"filenamePattern\":\"(?i).*(成果|论文|案例|改革|achievement|paper|case).*\",\"manualCheckTips\":[\"正文附件需与系统填写的摘要、关键词、正文和参考文献一致。\"]}","sortOrder":1},{"fileTypeCode":"video","fileTypeName":"案例视频","allowedExt":"mp4,mov","maxSizeMb":1024,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"mediaType\":\"video\",\"maxMb\":1024,\"maxDurationSeconds\":300,\"manualCheckTips\":[\"案例视频不超过5分钟，需人工确认内容与案例正文一致。\"]}","sortOrder":2},{"fileTypeCode":"image","fileTypeName":"案例图片","allowedExt":"jpg,jpeg","maxSizeMb":100,"minCount":0,"maxCount":5,"required":false,"ruleJson":"{\"mediaType\":\"image\",\"minMb\":10,\"maxMb\":100,\"dpi\":300,\"manualCheckTips\":[\"DPI缺失时需人工确认是否达到300dpi。\"]}","sortOrder":3}]',
    rule_json = '{"validatorType":"aesthetic_achievement","paperMaxAuthorCount":2,"paperAbstractMinLength":250,"paperAbstractMaxLength":350,"paperMinKeywordCount":3,"paperMaxKeywordCount":5,"paperMinBodyLength":5000,"caseMaxCompleterCount":3,"caseMaxTextLength":5000,"caseMaxVideoCount":1,"caseVideoMaxMb":1024,"caseVideoMaxDurationSeconds":300,"caseMaxImageCount":5,"caseImageMinMb":10,"caseImageMinDpi":300,"canonicalFieldKeys":{"groupName":["groupName"],"projectType":["projectType"],"programForm":["programForm"]},"memberFieldGroups":{"author":[{"fieldKey":"name","fieldLabel":"作者姓名","fieldType":"input","required":true,"sortOrder":1},{"fieldKey":"department","fieldLabel":"所在单位/院系","fieldType":"input","required":false,"sortOrder":2},{"fieldKey":"roleName","fieldLabel":"作者排序/贡献","fieldType":"input","required":false,"sortOrder":3}],"participant":[{"fieldKey":"name","fieldLabel":"完成人姓名","fieldType":"input","required":true,"sortOrder":1},{"fieldKey":"department","fieldLabel":"所在单位/院系","fieldType":"input","required":false,"sortOrder":2},{"fieldKey":"roleName","fieldLabel":"完成分工","fieldType":"input","required":false,"sortOrder":3}]},"manualCheckTips":["学术论文需核验未公开发表承诺和参考文献。","教学改革案例需以单位名义提交，并包含背景、做法、成效、建议。","论文作者不超过2人，教学改革案例完成人不超过3人。"],"rejectReasonTemplates":["论文摘要、关键词、正文或参考文献不符合要求。","未勾选未公开发表承诺。","教学改革案例结构或附件不符合要求。","作者、完成人、提交单位或成果形式填写不完整。"]}',
    tip_text = '学术论文和教学改革案例按成果类型分别校验。请完整填写成果形式、正文、摘要、关键词、参考文献或案例结构，并上传成果正文或附件。',
    check_mode = 'mixed',
    update_time = sysdate()
where template_code = 'tpl_reform_result';

-- Normalize configurable field types used by the school reporting form.
update category_field_schema
set field_type = 'duration',
    update_time = sysdate()
where tenant_id = '000000'
  and field_key in ('durationText', 'work_duration');

update category_field_schema
set field_key = 'adviserTeachers',
    field_type = 'teacher_group',
    validation_json = '{"maxItems":3}',
    update_time = sysdate()
where tenant_id = '000000'
  and field_key = 'adviserNames';

update upload_rule_template
set field_schema_json = replace(
        replace(
            replace(
                replace(field_schema_json,
                    '"fieldKey":"durationText","fieldLabel":"节目时长","fieldType":"input"',
                    '"fieldKey":"durationText","fieldLabel":"节目时长","fieldType":"duration"'
                ),
                '"fieldKey":"adviserNames","fieldLabel":"指导老师","fieldType":"input"',
                '"fieldKey":"adviserTeachers","fieldLabel":"指导老师","fieldType":"teacher_group","validationJson":"{\"maxItems\":3}"'
            ),
            '"fieldKey":"adviserNames","fieldLabel":"指导教师","fieldType":"input"',
            '"fieldKey":"adviserTeachers","fieldLabel":"指导教师","fieldType":"teacher_group","validationJson":"{\"maxItems\":3}"'
        ),
        '"fieldKey":"work_duration","fieldLabel":"作品时长","fieldType":"input"',
        '"fieldKey":"work_duration","fieldLabel":"作品时长","fieldType":"duration"'
    ),
    update_time = sysdate()
where tenant_id = '000000'
  and field_schema_json is not null;
