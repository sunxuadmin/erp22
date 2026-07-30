-- M79: 门户首页配置中心。
-- 运行前确保 RuoYi 基础表和 art_review_config_rules.sql 已初始化。

create table if not exists home_site_config
(
    id                  bigint(20)   not null comment 'ID',
    tenant_id           varchar(20)  default '000000' comment 'tenant id',
    config_code         varchar(64)  not null comment 'config code',
    site_name           varchar(128) not null comment 'site name',
    logo_oss_id         bigint(20)   default null comment 'logo oss id',
    logo_url            varchar(500) default null comment 'logo url',
    home_link           varchar(255) default '#home' comment 'home link',
    nav_items_json      text         null comment 'header nav json',
    header_buttons_json text         null comment 'header buttons json',
    login_state_config_json text     null comment 'login state text json',
    section_config_json text         null comment 'section meta json',
    notice_limit        int          default 6 comment 'home notice count',
    footer_title        varchar(128) default null comment 'footer title',
    footer_text         varchar(500) default null comment 'footer text',
    organizer           varchar(255) default null comment 'organizer',
    sponsor             varchar(255) default null comment 'sponsor',
    technical_support   varchar(255) default null comment 'technical support',
    contact_phone       varchar(64)  default null comment 'contact phone',
    contact_email       varchar(128) default null comment 'contact email',
    contact_address     varchar(255) default null comment 'contact address',
    icp_text            varchar(128) default null comment 'icp text',
    icp_link            varchar(255) default null comment 'icp link',
    police_text         varchar(128) default null comment 'police text',
    police_link         varchar(255) default null comment 'police link',
    custom_css          text         null comment 'custom css',
    enabled             tinyint(1)   default 1 comment 'enabled',
    remark              varchar(500) default null comment 'remark',
    create_dept         bigint(20)   default null comment 'create dept',
    create_by           bigint(20)   default null comment 'create by',
    create_time         datetime     default null comment 'create time',
    update_by           bigint(20)   default null comment 'update by',
    update_time         datetime     default null comment 'update time',
    del_flag            char(1)      default '0' comment 'delete flag',
    primary key (id),
    unique key uk_home_site_config_code (config_code)
) engine=innodb comment='home site config';

create table if not exists home_content_card
(
    id             bigint(20)   not null comment 'ID',
    tenant_id      varchar(20)  default '000000' comment 'tenant id',
    section_key    varchar(32)  not null comment 'guide/category/stage/notice',
    card_code      varchar(64)  not null comment 'card code',
    card_title     varchar(128) not null comment 'card title',
    card_subtitle  varchar(255) default null comment 'card subtitle',
    card_content   text         null comment 'card content',
    icon           varchar(64)  default null comment 'icon text',
    image_oss_id   bigint(20)   default null comment 'image oss id',
    image_url      varchar(500) default null comment 'image url',
    link_text      varchar(64)  default null comment 'link text',
    link_url       varchar(255) default null comment 'link url',
    link_type      varchar(32)  default 'anchor' comment 'anchor/route/external',
    highlight      tinyint(1)   default 0 comment 'highlight',
    enabled        tinyint(1)   default 1 comment 'enabled',
    sort_order     int          default 0 comment 'sort order',
    layout_type    varchar(32)  default 'default' comment 'reserved layout type',
    template_code  varchar(64)  default null comment 'reserved template code',
    slot_key       varchar(64)  default null comment 'reserved slot key',
    remark         varchar(500) default null comment 'remark',
    create_dept    bigint(20)   default null comment 'create dept',
    create_by      bigint(20)   default null comment 'create by',
    create_time    datetime     default null comment 'create time',
    update_by      bigint(20)   default null comment 'update by',
    update_time    datetime     default null comment 'update time',
    del_flag       char(1)      default '0' comment 'delete flag',
    primary key (id),
    unique key uk_home_content_card_code (card_code),
    key idx_home_content_card_section (section_key, enabled, sort_order)
) engine=innodb comment='home content card';

alter table home_site_config
    add column if not exists login_state_config_json text null comment 'login state text json' after header_buttons_json;

alter table home_site_config
    add column if not exists custom_css text null comment 'custom css' after police_link;

insert into home_site_config
(id, tenant_id, config_code, site_name, logo_url, home_link, nav_items_json, header_buttons_json, login_state_config_json, section_config_json,
 notice_limit, footer_title, footer_text, organizer, sponsor, technical_support, contact_phone, contact_email,
 contact_address, icp_text, icp_link, police_text, police_link, custom_css, enabled, remark, create_dept, create_by, create_time, del_flag)
select 18400, '000000', 'portal_home', '河南省第八届大学生艺术展演', 'assets/logo-icon.png', '#home',
       '[{"text":"活动首页","link":"#home","enabled":true,"sortOrder":10},{"text":"上报指南","link":"#guide","enabled":true,"sortOrder":20},{"text":"展演项目","link":"#category","enabled":true,"sortOrder":30},{"text":"展演阶段","link":"#stage","enabled":true,"sortOrder":40},{"text":"通知公告","link":"#notice","enabled":true,"sortOrder":50}]',
       '[{"text":"登录入口","link":"/login","style":"ghost","enabled":true,"sortOrder":10},{"text":"注册","link":"/register","style":"solid","enabled":true,"sortOrder":20}]',
       '{"adminText":"管理账户已登录","schoolText":"学校账户已登录","expertText":"专家账户已登录","defaultText":"已登录"}',
       '[{"sectionKey":"guide","eyebrow":"SUBMISSION GUIDE","title":"上报指南","intro":""},{"sectionKey":"category","eyebrow":"ART CATEGORIES","title":"展演项目","intro":""},{"sectionKey":"stage","eyebrow":"EXHIBITION STAGES","title":"展演阶段","intro":""},{"sectionKey":"notice","eyebrow":"NOTICE","title":"通知公告","intro":"","moreText":"查看更多 →","moreLink":"#notice"}]',
       6, '河南省第八届大学生艺术展演', 'Copyright © 2026 ', '', '', '', '', '', '', '', '', '', '', '', 1,
       '门户首页默认配置', 103, 1, sysdate(), '0'
where not exists (select 1 from home_site_config where config_code = 'portal_home');

insert into home_content_card
(id, tenant_id, section_key, card_code, card_title, card_content, icon, link_text, link_url, link_type, highlight, enabled, sort_order, layout_type, create_dept, create_by, create_time, del_flag)
select 18410, '000000', 'guide', 'guide_workshop', '艺术实践工作坊', '上传工作坊视频和项目介绍文档。', '坊', '操作指南', '#guide', 'anchor', 0, 1, 10, 'default', 103, 1, sysdate(), '0'
where not exists (select 1 from home_content_card where card_code = 'guide_workshop');

insert into home_content_card
(id, tenant_id, section_key, card_code, card_title, card_content, icon, link_text, link_url, link_type, highlight, enabled, sort_order, layout_type, create_dept, create_by, create_time, del_flag)
select 18411, '000000', 'guide', 'guide_reform_result', '美育改革创新优秀成果', '填写标题、简介、作者信息并上传正文。', '改', '上报说明', '#guide', 'anchor', 1, 1, 20, 'default', 103, 1, sysdate(), '0'
where not exists (select 1 from home_content_card where card_code = 'guide_reform_result');

insert into home_content_card
(id, tenant_id, section_key, card_code, card_title, card_content, icon, link_text, link_url, link_type, highlight, enabled, sort_order, layout_type, create_dept, create_by, create_time, del_flag)
select 18412, '000000', 'guide', 'guide_performance', '艺术表演类', '声乐、器乐、舞蹈、戏剧、朗诵及个人项目。', '演', '操作指南', '#guide', 'anchor', 0, 1, 30, 'default', 103, 1, sysdate(), '0'
where not exists (select 1 from home_content_card where card_code = 'guide_performance');

insert into home_content_card
(id, tenant_id, section_key, card_code, card_title, card_content, icon, link_text, link_url, link_type, highlight, enabled, sort_order, layout_type, create_dept, create_by, create_time, del_flag)
select 18413, '000000', 'guide', 'guide_artwork', '艺术作品类', '美术、设计、大艺展设计及影像作品报送。', '作', '上报说明', '#guide', 'anchor', 0, 1, 40, 'default', 103, 1, sysdate(), '0'
where not exists (select 1 from home_content_card where card_code = 'guide_artwork');

insert into home_content_card
(id, tenant_id, section_key, card_code, card_title, card_content, icon, link_text, link_url, link_type, highlight, enabled, sort_order, layout_type, create_dept, create_by, create_time, del_flag)
select 18414, '000000', 'guide', 'guide_principal_calligraphy', '高校校长书画作品', '校长书画作品作者信息直接在表单中填写。', '书', '操作指南', '#guide', 'anchor', 0, 1, 50, 'default', 103, 1, sysdate(), '0'
where not exists (select 1 from home_content_card where card_code = 'guide_principal_calligraphy');

insert into home_content_card
(id, tenant_id, section_key, card_code, card_title, card_content, icon, link_text, link_url, link_type, highlight, enabled, sort_order, layout_type, create_dept, create_by, create_time, del_flag)
select 18420, '000000', 'category', 'category_vocal', '声乐', '合唱 / 独唱', '声', '', '#category', 'anchor', 0, 1, 10, 'default', 103, 1, sysdate(), '0'
where not exists (select 1 from home_content_card where card_code = 'category_vocal');

insert into home_content_card
(id, tenant_id, section_key, card_code, card_title, card_content, icon, link_text, link_url, link_type, highlight, enabled, sort_order, layout_type, create_dept, create_by, create_time, del_flag)
select 18421, '000000', 'category', 'category_instrumental', '器乐', '民乐 / 西乐', '器', '', '#category', 'anchor', 0, 1, 20, 'default', 103, 1, sysdate(), '0'
where not exists (select 1 from home_content_card where card_code = 'category_instrumental');

insert into home_content_card
(id, tenant_id, section_key, card_code, card_title, card_content, icon, link_text, link_url, link_type, highlight, enabled, sort_order, layout_type, create_dept, create_by, create_time, del_flag)
select 18422, '000000', 'category', 'category_dance', '舞蹈', '群舞 / 独舞', '舞', '', '#category', 'anchor', 0, 1, 30, 'default', 103, 1, sysdate(), '0'
where not exists (select 1 from home_content_card where card_code = 'category_dance');

insert into home_content_card
(id, tenant_id, section_key, card_code, card_title, card_content, icon, link_text, link_url, link_type, highlight, enabled, sort_order, layout_type, create_dept, create_by, create_time, del_flag)
select 18423, '000000', 'category', 'category_drama', '戏剧', '短剧 / 戏曲', '戏', '', '#category', 'anchor', 0, 1, 40, 'default', 103, 1, sysdate(), '0'
where not exists (select 1 from home_content_card where card_code = 'category_drama');

insert into home_content_card
(id, tenant_id, section_key, card_code, card_title, card_content, icon, link_text, link_url, link_type, highlight, enabled, sort_order, layout_type, create_dept, create_by, create_time, del_flag)
select 18424, '000000', 'category', 'category_recitation', '朗诵', '朗诵 / 语言', '诵', '', '#category', 'anchor', 0, 1, 50, 'default', 103, 1, sysdate(), '0'
where not exists (select 1 from home_content_card where card_code = 'category_recitation');

insert into home_content_card
(id, tenant_id, section_key, card_code, card_title, card_content, icon, link_text, link_url, link_type, highlight, enabled, sort_order, layout_type, create_dept, create_by, create_time, del_flag)
select 18425, '000000', 'category', 'category_workshop', '艺术实践工作坊', '实践 / 展示', '坊', '', '#category', 'anchor', 0, 1, 60, 'default', 103, 1, sysdate(), '0'
where not exists (select 1 from home_content_card where card_code = 'category_workshop');

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(18010, '首页配置', 1, 91, 'home-config', 'system/home-config/index', null, 1, 0, 'C', '0', '0', 'system:homeConfig:list', 'dashboard', 103, 1, sysdate(), null, null, '门户首页内容卡片、Header、Footer 配置')
on duplicate key update
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    path = values(path),
    component = values(component),
    query_param = values(query_param),
    is_frame = values(is_frame),
    is_cache = values(is_cache),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    icon = values(icon),
    remark = values(remark);

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(18011, '首页配置查询', 18010, 1, '#', '', null, 1, 0, 'F', '0', '0', 'system:homeConfig:query', '#', 103, 1, sysdate(), null, null, ''),
(18012, '首页配置编辑', 18010, 2, '#', '', null, 1, 0, 'F', '0', '0', 'system:homeConfig:edit', '#', 103, 1, sysdate(), null, null, ''),
(18013, '首页配置删除', 18010, 3, '#', '', null, 1, 0, 'F', '0', '0', 'system:homeConfig:remove', '#', 103, 1, sysdate(), null, null, '')
on duplicate key update
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    path = values(path),
    component = values(component),
    query_param = values(query_param),
    is_frame = values(is_frame),
    is_cache = values(is_cache),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    icon = values(icon),
    remark = values(remark);

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (1, 18010, 18011, 18012, 18013)
where r.role_key in ('admin', 'superadmin', 'super_admin');
