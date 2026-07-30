-- --------------------------------------------------
-- Art Review configurable rules menus
-- Safe to rerun after art_review_menu.sql
-- --------------------------------------------------

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17600, '上传规则模板', 17000, 6, 'upload-template', 'crehn/upload-template/index', '', 1, 0, 'C', '0', '0', 'crehn:uploadTemplate:list', 'upload', 103, 1, sysdate(), null, null, '可维护上传规则模板'),
(17610, '单位管理', 17000, 7, 'school-field', 'crehn/school-field/index', '', 1, 0, 'C', '0', '0', 'crehn:schoolInfo:list', 'company', 103, 1, sysdate(), null, null, '单位基础资料维护'),
(17620, '活动学校授权', 17000, 8, 'activity-scope', 'crehn/activity-scope/index', '', 1, 0, 'C', '0', '0', 'crehn:activityScope:list', 'peoples', 103, 1, sysdate(), null, null, '按 school_id 授权活动'),
(17640, '公告设置', 1, 12, 'notice-template', 'crehn/notice-template/index', '', 1, 0, 'C', '0', '0', 'crehn:noticeTemplate:list', 'message', 103, 1, sysdate(), null, null, '公告模板分组推送');

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17601, '模板查询', 17600, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:uploadTemplate:query', '#', 103, 1, sysdate(), null, null, ''),
(17602, '模板新增', 17600, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:uploadTemplate:add', '#', 103, 1, sysdate(), null, null, ''),
(17603, '模板修改', 17600, 3, '', '', '', 1, 0, 'F', '0', '0', 'crehn:uploadTemplate:edit', '#', 103, 1, sysdate(), null, null, ''),
(17604, '模板复制', 17600, 4, '', '', '', 1, 0, 'F', '0', '0', 'crehn:uploadTemplate:copy', '#', 103, 1, sysdate(), null, null, ''),
(17605, '模板应用', 17600, 5, '', '', '', 1, 0, 'F', '0', '0', 'crehn:uploadTemplate:apply', '#', 103, 1, sysdate(), null, null, ''),
(17606, '模板删除', 17600, 6, '', '', '', 1, 0, 'F', '0', '0', 'crehn:uploadTemplate:remove', '#', 103, 1, sysdate(), null, null, ''),
(17611, '学校查询', 17610, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:schoolInfo:query', '#', 103, 1, sysdate(), null, null, ''),
(17612, '学校保存', 17610, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:schoolInfo:edit', '#', 103, 1, sysdate(), null, null, ''),
(17613, '学校删除', 17610, 3, '', '', '', 1, 0, 'F', '0', '0', 'crehn:schoolInfo:remove', '#', 103, 1, sysdate(), null, null, ''),
(17621, '活动授权修改', 17620, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:activityScope:edit', '#', 103, 1, sysdate(), null, null, ''),
(17622, '活动授权删除', 17620, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:activityScope:remove', '#', 103, 1, sysdate(), null, null, ''),
(17641, '公告模板修改', 17640, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:noticeTemplate:edit', '#', 103, 1, sysdate(), null, null, ''),
(17642, '公告模板删除', 17640, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:noticeTemplate:remove', '#', 103, 1, sysdate(), null, null, '');

update sys_menu
set visible = '1',
    status = '1',
    perms = '',
    component = '',
    remark = '已退役：旧名额规则入口已合并到当前活动规则'
where menu_id = 17630;

update sys_menu
set visible = '1',
    status = '1',
    perms = '',
    remark = '已退役：旧名额规则按钮权限不再使用'
where menu_id in (17631, 17632);

delete from sys_role_menu where menu_id in (17630, 17631, 17632);

-- Grant config menus to the seeded crehn_admin role when the test seed exists.
insert ignore into sys_role_menu(role_id, menu_id)
select 900101, menu_id from sys_menu where menu_id in (17600, 17601, 17602, 17603, 17604, 17605, 17606, 17610, 17611, 17612, 17613, 17620, 17621, 17622, 17640, 17641, 17642)
  and exists (select 1 from sys_role where role_id = 900101 and role_key = 'crehn_admin');

-- Some test databases imported the config menu before the seeded role was created.
-- Keep notice permissions explicitly idempotent so crehn_admin can maintain register notices.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17640, 17641, 17642)
where r.role_key = 'crehn_admin';

-- Upgrade previous "school field" route into school base info management.
update sys_menu
set menu_name = '单位管理',
    perms = 'crehn:schoolInfo:list',
    icon = 'company',
    remark = '单位基础资料维护'
where menu_id = 17610;

update sys_menu
set menu_name = '学校查询',
    perms = 'crehn:schoolInfo:query'
where menu_id = 17611;

update sys_menu
set menu_name = '单位保存',
    perms = 'crehn:schoolInfo:edit'
where menu_id = 17612;

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17613, '单位删除', 17610, 3, '', '', '', 1, 0, 'F', '0', '0', 'crehn:schoolInfo:remove', '#', 103, 1, sysdate(), null, null, '');

update sys_menu
set menu_name = '单位查询'
where menu_id = 17611;

update sys_menu
set menu_name = '单位删除'
where menu_id = 17613;

update sys_menu
set menu_name = '公告设置',
    parent_id = 1,
    order_num = 12,
    remark = '公告模板分组推送'
where menu_id = 17640;

-- Keep old schema permissions available for backend compatibility, but grant the new school info permissions to config admins.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17610, 17611, 17612, 17613)
where r.role_key = 'crehn_admin';

-- Merge school account review and registration code management into account management.
update sys_menu
set menu_name = '账号管理',
    remark = '账号审核、注册码邀请和直接创建账号'
where menu_id = 17002;

update sys_menu
set visible = '1',
    remark = '已合并到账号管理，保留路由和按钮权限'
where menu_id = 17005;

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17206, '账号创建', 17002, 6, '', '', '', 1, 0, 'F', '0', '0', 'crehn:schoolAccount:add', '#', 103, 1, sysdate(), null, null, '');

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17206)
where r.role_key = 'crehn_admin';

-- New reusable report rule center. Old quota menu is retired because the backend and frontend entry were removed.
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

-- Ensure art review units are visible as a clean organization tree in System > User.
insert ignore into sys_dept
(dept_id, tenant_id, parent_id, ancestors, dept_name, dept_category, order_num, leader, phone, email, status, del_flag, create_dept, create_by, create_time, update_by, update_time)
values
(900100, '000000', 0, '0', '艺术评审组织', 'crehn_root', 90, null, '15800000000', 'art-test@example.com', '0', '0', 103, 1, sysdate(), null, null);

update sys_dept
set dept_name = '艺术评审组织',
    dept_category = 'crehn_root',
    parent_id = 0,
    ancestors = '0',
    status = '0'
where dept_id = 900100;

update sys_dept d
join school_info s on s.id = 900101
set d.dept_name = s.school_name,
    d.dept_category = concat('crehn_school_', s.id),
    d.parent_id = 900100,
    d.ancestors = '0,900100',
    d.phone = s.contact_phone,
    d.email = s.contact_email,
    d.status = if(s.status = 'enabled', '0', '1')
where d.dept_id = 900101;
