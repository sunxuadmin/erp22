-- --------------------------------------------------
-- Art Review M7.6 menu icon and Chinese label repair.
-- Safe to rerun after art_review_menu.sql and M7.5 scripts.
-- --------------------------------------------------

update sys_menu
set icon = case menu_id
    when 17000 then 'education'
    when 17001 then 'date'
    when 17002 then 'user'
    when 17003 then 'form'
    when 17004 then 'clipboard'
    when 17005 then 'validCode'
    when 17310 then 'list'
    when 17311 then 'star'
    when 17312 then 'color'
    when 17313 then 'guide'
    when 17314 then 'finish'
    when 17320 then 'message'
    when 17321 then 'skill'
    when 17322 then 'star'
    when 17323 then 'component'
    when 17324 then 'documentation'
    when 17325 then 'user'
    when 17330 then 'color'
    when 17331 then 'component'
    when 17332 then 'component'
    when 17333 then 'monitor'
    when 17334 then 'education'
    when 17340 then 'guide'
    when 17350 then 'documentation'
    when 17351 then 'education'
    when 17420 then 'peoples'
    when 17600 then 'upload'
    when 17610 then 'company'
    when 17620 then 'peoples'
    when 17640 then 'message'
    when 17700 then 'redis-list'
    when 17800 then 'message'
    when 17900 then 'my-task'
    when 17920 then 'finish'
    when 17940 then 'star'
    when 17980 then 'eye-open'
    when 18000 then 'dashboard'
    else icon
end
where menu_id in (
    17000, 17001, 17002, 17003, 17004, 17005,
    17310, 17311, 17312, 17313, 17314, 17320, 17321,
    17322, 17323, 17324, 17325, 17330, 17331, 17332,
    17333, 17334, 17340, 17350, 17351,
    17420, 17600, 17610, 17620, 17640,
    17700, 17800, 17900, 17920, 17940, 17980, 18000
);

update sys_menu
set menu_name = '工作台配置',
    remark = '工作台布局配置菜单',
    icon = 'dashboard'
where menu_id = 18000;

update sys_menu
set menu_name = '工作台查询'
where menu_id = 18001;

update sys_menu
set menu_name = '工作台编辑'
where menu_id = 18002;
