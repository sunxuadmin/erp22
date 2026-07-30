-- M86: 门户首页自定义 CSS 配置。
-- Safe to rerun after M79 首页配置中心初始化。

alter table home_site_config
    add column if not exists custom_css text null comment 'custom css' after police_link;
