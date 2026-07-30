-- M85: 门户首页登录态文案配置。
-- Safe to rerun after M79 首页配置中心初始化。

alter table home_site_config
    add column if not exists login_state_config_json text null comment 'login state text json' after header_buttons_json;

update home_site_config
set login_state_config_json = '{"adminText":"管理账户已登录","schoolText":"学校账户已登录","expertText":"专家账户已登录","defaultText":"已登录"}'
where config_code = 'portal_home'
  and (login_state_config_json is null or trim(login_state_config_json) = '');
