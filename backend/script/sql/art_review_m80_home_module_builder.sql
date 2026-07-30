-- M80: 门户首页模块搭建器。
-- 依赖 M79 已创建 home_site_config 与 home_content_card。

create table if not exists home_page_module
(
    id             bigint(20)   not null comment 'ID',
    tenant_id      varchar(20)  default '000000' comment 'tenant id',
    module_code    varchar(64)  not null comment 'module code',
    module_title   varchar(128) not null comment 'module title',
    module_type    varchar(32)  default 'custom' comment 'hero/guide/category/stage/notice/custom',
    anchor         varchar(64)  default null comment 'front anchor',
    eyebrow        varchar(128) default null comment 'section eyebrow',
    intro          varchar(500) default null comment 'section intro',
    more_text      varchar(128) default null comment 'more text',
    more_link      varchar(255) default null comment 'more link',
    nav_enabled    tinyint(1)   default 1 comment 'show in header nav',
    enabled        tinyint(1)   default 1 comment 'enabled',
    sort_order     int          default 0 comment 'sort order',
    layout_type    varchar(32)  default 'default' comment 'layout type',
    template_code  varchar(64)  default null comment 'template code',
    config_json    text         null comment 'module config json',
    remark         varchar(500) default null comment 'remark',
    create_dept    bigint(20)   default null comment 'create dept',
    create_by      bigint(20)   default null comment 'create by',
    create_time    datetime     default null comment 'create time',
    update_by      bigint(20)   default null comment 'update by',
    update_time    datetime     default null comment 'update time',
    del_flag       char(1)      default '0' comment 'delete flag',
    primary key (id),
    unique key uk_home_page_module_code (tenant_id, module_code),
    key idx_home_page_module_sort (tenant_id, enabled, sort_order)
) engine=innodb comment='home page module';

set @schema_name = database();

set @sql = (
    select if(count(*) = 0,
        'alter table home_content_card add column card_type varchar(32) default ''info'' comment ''card type'' after card_code',
        'select 1')
    from information_schema.columns
    where table_schema = @schema_name and table_name = 'home_content_card' and column_name = 'card_type'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql = (
    select if(count(*) = 0,
        'alter table home_content_card add column config_json text null comment ''card config json'' after slot_key',
        'select 1')
    from information_schema.columns
    where table_schema = @schema_name and table_name = 'home_content_card' and column_name = 'config_json'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

insert into home_page_module
(id, tenant_id, module_code, module_title, module_type, anchor, eyebrow, intro, more_text, more_link,
 nav_enabled, enabled, sort_order, layout_type, template_code, config_json, remark, create_dept, create_by, create_time, del_flag)
select 18401, '000000', 'home', '活动首页', 'hero', 'home', 'DYZ · ART EXHIBITION', '', '', '',
       1, 1, 10, 'default', 'hero', '{}', '首页首屏模块', 103, 1, sysdate(), '0'
where not exists (select 1 from home_page_module where tenant_id = '000000' and module_code = 'home');

insert into home_page_module
(id, tenant_id, module_code, module_title, module_type, anchor, eyebrow, intro, more_text, more_link,
 nav_enabled, enabled, sort_order, layout_type, template_code, config_json, remark, create_dept, create_by, create_time, del_flag)
select 18402, '000000', 'guide', '上报指南', 'guide', 'guide', 'SUBMISSION GUIDE', '', '', '',
       1, 1, 20, 'default', 'guide', '{}', '上报指南模块', 103, 1, sysdate(), '0'
where not exists (select 1 from home_page_module where tenant_id = '000000' and module_code = 'guide');

insert into home_page_module
(id, tenant_id, module_code, module_title, module_type, anchor, eyebrow, intro, more_text, more_link,
 nav_enabled, enabled, sort_order, layout_type, template_code, config_json, remark, create_dept, create_by, create_time, del_flag)
select 18403, '000000', 'category', '展演项目', 'category', 'category', 'ART CATEGORIES', '', '', '',
       1, 1, 30, 'default', 'category', '{}', '展演项目模块', 103, 1, sysdate(), '0'
where not exists (select 1 from home_page_module where tenant_id = '000000' and module_code = 'category');

insert into home_page_module
(id, tenant_id, module_code, module_title, module_type, anchor, eyebrow, intro, more_text, more_link,
 nav_enabled, enabled, sort_order, layout_type, template_code, config_json, remark, create_dept, create_by, create_time, del_flag)
select 18404, '000000', 'stage', '展演阶段', 'stage', 'stage', 'EXHIBITION STAGES', '', '', '',
       1, 1, 40, 'default', 'stage', '{}', '展演阶段模块', 103, 1, sysdate(), '0'
where not exists (select 1 from home_page_module where tenant_id = '000000' and module_code = 'stage');

insert into home_page_module
(id, tenant_id, module_code, module_title, module_type, anchor, eyebrow, intro, more_text, more_link,
 nav_enabled, enabled, sort_order, layout_type, template_code, config_json, remark, create_dept, create_by, create_time, del_flag)
select 18405, '000000', 'notice', '通知公告', 'notice', 'notice', 'NOTICE', '', '查看更多 →', '#notice',
       1, 1, 50, 'default', 'notice', '{"noticeLimit":6}', '通知公告模块', 103, 1, sysdate(), '0'
where not exists (select 1 from home_page_module where tenant_id = '000000' and module_code = 'notice');

update home_content_card
set card_type = case section_key
    when 'guide' then 'guide'
    when 'category' then 'category'
    when 'stage' then 'stage'
    when 'notice' then 'notice'
    else 'info'
end
where card_type is null or card_type = '';

update home_content_card
set template_code = card_type
where template_code is null or template_code = '';

update home_content_card
set config_json = '{}'
where config_json is null;

insert into home_content_card
(id, tenant_id, section_key, card_code, card_type, card_title, card_subtitle, card_content, icon,
 link_text, link_url, link_type, highlight, enabled, sort_order, layout_type, template_code, slot_key,
 config_json, create_dept, create_by, create_time, del_flag)
select 18409, '000000', 'home', 'home_hero', 'hero', concat('向美而行', char(10), '逐梦未来'),
       '河南省第八届大学生艺术展演', '', '', '', '#home', 'anchor', 0, 1, 10, 'default', 'hero', 'hero',
       '{"kicker":"DYZ · ART EXHIBITION","sloganLines":["向美而行","逐梦未来"],"dynamicLabel":"ART","dynamicWord":"艺术展演","buttons":[{"text":"登录入口","link":"/login","style":"primary","enabled":true,"sortOrder":10},{"text":"上报指南","link":"#guide","style":"secondary","enabled":true,"sortOrder":20}],"particle":{"blueColor":"rgba(0, 140, 255, 0.58)","accentColor":"rgba(245, 100, 100, 0.5)","floatBlueColor":"rgba(0,112,185,","floatGreenColor":"rgba(0,180,150,","density":1,"fontScale":1,"xRatio":0.72,"yRatio":0.48}}',
       103, 1, sysdate(), '0'
where not exists (select 1 from home_content_card where card_code = 'home_hero');

insert into home_content_card
(id, tenant_id, section_key, card_code, card_type, card_title, card_content, icon,
 link_text, link_url, link_type, highlight, enabled, sort_order, layout_type, template_code, slot_key,
 config_json, create_dept, create_by, create_time, del_flag)
select 18440, '000000', 'notice', 'notice_list', 'notice', '通知公告列表', '', '',
       '', '#notice', 'anchor', 0, 1, 10, 'default', 'notice', 'notice',
       '{"noticeLimit":6,"showDate":true,"showTag":true}',
       103, 1, sysdate(), '0'
where not exists (select 1 from home_content_card where card_code = 'notice_list');
