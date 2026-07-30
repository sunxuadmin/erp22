-- --------------------------------------------------
-- M70 school project grouped menus
-- Keep the school-side menu fixed as five reporting entries, while
-- concrete categories, fields and file requirements remain configurable.
-- --------------------------------------------------

update sys_menu
set menu_name = '我的项目',
    path = 'project',
    component = null,
    query_param = '',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'M',
    visible = '1',
    status = '0',
    perms = '',
    icon = 'form',
    remark = '已隐藏，仅保留项目按钮权限；报送大类直接挂在艺术展演下'
where menu_id = 17003;

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17310, '全部项目', 17000, 3, 'all', 'crehn/project/index', '{"group":"all"}', 1, 0, 'C', '0', '0', 'crehn:project:list', 'list', 103, 1, sysdate(), null, null, '学校全部报送项目'),
(17311, '艺术表演类', 17000, 4, 'performance', null, '', 1, 0, 'M', '0', '0', '', 'star', 103, 1, sysdate(), null, null, '艺术表演类报送分项'),
(17312, '艺术作品类', 17000, 5, 'artwork', null, '', 1, 0, 'M', '0', '0', '', 'color', 103, 1, sysdate(), null, null, '艺术作品类报送分项'),
(17313, '艺术实践工作坊', 17000, 6, 'workshop', null, '', 1, 0, 'M', '0', '0', '', 'guide', 103, 1, sysdate(), null, null, '艺术实践工作坊报送分项'),
(17314, '高校美育改革创新优秀成果', 17000, 7, 'achievement', null, '', 1, 0, 'M', '0', '0', '', 'finish', 103, 1, sysdate(), null, null, '高校美育改革创新优秀成果报送分项'),
(17334, '高校校长书画作品', 17000, 8, 'principal', 'crehn/project/index', '{"group":"principal","categoryCode":"artwork_principal"}', 1, 0, 'C', '0', '0', 'crehn:project:list', 'education', 103, 1, sysdate(), null, null, '高校校长书画作品报送入口')
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
(17320, '声乐', 17311, 1, 'vocal', 'crehn/project/index', '{"group":"performance","categoryCode":"performance_vocal"}', 1, 0, 'C', '0', '0', 'crehn:project:list', 'message', 103, 1, sysdate(), null, null, '艺术表演类-声乐'),
(17321, '器乐', 17311, 2, 'instrumental', 'crehn/project/index', '{"group":"performance","categoryCode":"performance_instrumental"}', 1, 0, 'C', '0', '0', 'crehn:project:list', 'skill', 103, 1, sysdate(), null, null, '艺术表演类-器乐'),
(17322, '舞蹈', 17311, 3, 'dance', 'crehn/project/index', '{"group":"performance","categoryCode":"performance_dance"}', 1, 0, 'C', '0', '0', 'crehn:project:list', 'star', 103, 1, sysdate(), null, null, '艺术表演类-舞蹈'),
(17323, '戏剧', 17311, 4, 'drama', 'crehn/project/index', '{"group":"performance","categoryCode":"performance_drama"}', 1, 0, 'C', '0', '0', 'crehn:project:list', 'component', 103, 1, sysdate(), null, null, '艺术表演类-戏剧'),
(17324, '朗诵', 17311, 5, 'recitation', 'crehn/project/index', '{"group":"performance","categoryCode":"performance_recitation"}', 1, 0, 'C', '0', '0', 'crehn:project:list', 'documentation', 103, 1, sysdate(), null, null, '艺术表演类-朗诵'),
(17325, '个人项目', 17311, 6, 'personal', 'crehn/project/index', '{"group":"performance","categoryCode":"performance_personal"}', 1, 0, 'C', '0', '0', 'crehn:project:list', 'user', 103, 1, sysdate(), null, null, '艺术表演类-个人项目'),
(17330, '美术类', 17312, 1, 'fine-art', 'crehn/project/index', '{"group":"artwork","categoryCode":"artwork_fine_art"}', 1, 0, 'C', '0', '0', 'crehn:project:list', 'color', 103, 1, sysdate(), null, null, '艺术作品类-美术类'),
(17331, '大艺展设计类', 17312, 2, 'design-exhibition', 'crehn/project/index', '{"group":"artwork","categoryCode":"artwork_grand_design"}', 1, 0, 'C', '0', '0', 'crehn:project:list', 'component', 103, 1, sysdate(), null, null, '艺术作品类-大艺展设计类'),
(17332, '设计展', 17312, 3, 'design', 'crehn/project/index', '{"group":"artwork","categoryCode":"artwork_design"}', 1, 0, 'C', '0', '0', 'crehn:project:list', 'component', 103, 1, sysdate(), null, null, '艺术作品类-设计展'),
(17333, '影视类', 17312, 4, 'video', 'crehn/project/index', '{"group":"artwork","categoryCode":"artwork_film"}', 1, 0, 'C', '0', '0', 'crehn:project:list', 'monitor', 103, 1, sysdate(), null, null, '艺术作品类-影视类'),
(17340, '艺术实践工作坊', 17313, 1, 'workshop-item', 'crehn/project/index', '{"group":"workshop","categoryCode":"workshop"}', 1, 0, 'C', '0', '0', 'crehn:project:list', 'guide', 103, 1, sysdate(), null, null, '艺术实践工作坊'),
(17350, '学术论文', 17314, 1, 'paper', 'crehn/project/index', '{"group":"achievement","categoryCode":"achievement_paper"}', 1, 0, 'C', '0', '0', 'crehn:project:list', 'documentation', 103, 1, sysdate(), null, null, '高校美育改革创新优秀成果-学术论文'),
(17351, '教学改革案例', 17314, 2, 'teaching-case', 'crehn/project/index', '{"group":"achievement","categoryCode":"achievement_case"}', 1, 0, 'C', '0', '0', 'crehn:project:list', 'education', 103, 1, sysdate(), null, null, '高校美育改革创新优秀成果-教学改革案例')
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
select roles.role_id, menus.menu_id
from (
    select distinct role_id
    from sys_role_menu
    where menu_id = 17003
) roles
join (
    select 17310 as menu_id union all
    select 17311 union all
    select 17312 union all
    select 17313 union all
    select 17314 union all
    select 17320 union all
    select 17321 union all
    select 17322 union all
    select 17323 union all
    select 17324 union all
    select 17325 union all
    select 17330 union all
    select 17331 union all
    select 17332 union all
    select 17333 union all
    select 17334 union all
    select 17340 union all
    select 17350 union all
    select 17351
) menus;
