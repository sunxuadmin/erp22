-- --------------------------------------------------
-- Art Review M2/M3/M4 menu and button permissions
-- Run after RuoYi base sys_menu has been initialized.
-- --------------------------------------------------

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17000, '创意河南', 0, 6, 'crehn', null, '', 1, 0, 'M', '0', '0', '', 'education', 103, 1, sysdate(), null, null, '创意河南艺术设计大赛管理目录'),
(17001, '活动配置', 17000, 1, 'activity', 'crehn/activity/index', '', 1, 0, 'C', '0', '0', 'crehn:activity:list', 'calendar', 103, 1, sysdate(), null, null, '活动、类别、动态字段和附件要求配置'),
(17002, '账号管理', 17000, 2, 'school-account', 'crehn/school-account/index', '', 1, 0, 'C', '0', '0', 'crehn:schoolAccount:list', 'user', 103, 1, sysdate(), null, null, '账号审核、注册码邀请和直接创建账号'),
(17003, '我的项目', 17000, 3, 'project', 'crehn/project/index', '', 1, 0, 'C', '0', '0', 'crehn:project:list', 'form', 103, 1, sysdate(), null, null, '学校项目填报'),
(17004, '项目审核', 17000, 4, 'audit', 'crehn/audit/index', '', 1, 0, 'C', '0', '0', 'crehn:audit:list', 'clipboard', 103, 1, sysdate(), null, null, '管理老师材料审核'),
(17005, '注册码管理', 17000, 5, 'registration', 'crehn/registration/index', '', 1, 0, 'C', '1', '0', 'crehn:registration:list', 'validCode', 103, 1, sysdate(), null, null, '已合并到账号管理，保留路由和按钮权限');

-- 活动配置按钮权限
insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17101, '活动查询', 17001, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:activity:query', '#', 103, 1, sysdate(), null, null, ''),
(17102, '活动新增', 17001, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:activity:add', '#', 103, 1, sysdate(), null, null, ''),
(17103, '活动修改', 17001, 3, '', '', '', 1, 0, 'F', '0', '0', 'crehn:activity:edit', '#', 103, 1, sysdate(), null, null, ''),
(17104, '活动删除', 17001, 4, '', '', '', 1, 0, 'F', '0', '0', 'crehn:activity:remove', '#', 103, 1, sysdate(), null, null, ''),
(17105, '类别列表', 17001, 5, '', '', '', 1, 0, 'F', '0', '0', 'crehn:category:list', '#', 103, 1, sysdate(), null, null, ''),
(17106, '类别查询', 17001, 6, '', '', '', 1, 0, 'F', '0', '0', 'crehn:category:query', '#', 103, 1, sysdate(), null, null, ''),
(17107, '类别新增', 17001, 7, '', '', '', 1, 0, 'F', '0', '0', 'crehn:category:add', '#', 103, 1, sysdate(), null, null, ''),
(17108, '类别修改', 17001, 8, '', '', '', 1, 0, 'F', '0', '0', 'crehn:category:edit', '#', 103, 1, sysdate(), null, null, ''),
(17109, '类别删除', 17001, 9, '', '', '', 1, 0, 'F', '0', '0', 'crehn:category:remove', '#', 103, 1, sysdate(), null, null, ''),
(17110, '字段列表', 17001, 10, '', '', '', 1, 0, 'F', '0', '0', 'crehn:field:list', '#', 103, 1, sysdate(), null, null, ''),
(17111, '字段新增', 17001, 11, '', '', '', 1, 0, 'F', '0', '0', 'crehn:field:add', '#', 103, 1, sysdate(), null, null, ''),
(17112, '字段修改', 17001, 12, '', '', '', 1, 0, 'F', '0', '0', 'crehn:field:edit', '#', 103, 1, sysdate(), null, null, ''),
(17113, '字段删除', 17001, 13, '', '', '', 1, 0, 'F', '0', '0', 'crehn:field:remove', '#', 103, 1, sysdate(), null, null, ''),
(17114, '附件要求列表', 17001, 14, '', '', '', 1, 0, 'F', '0', '0', 'crehn:fileRequirement:list', '#', 103, 1, sysdate(), null, null, ''),
(17115, '附件要求新增', 17001, 15, '', '', '', 1, 0, 'F', '0', '0', 'crehn:fileRequirement:add', '#', 103, 1, sysdate(), null, null, ''),
(17116, '附件要求修改', 17001, 16, '', '', '', 1, 0, 'F', '0', '0', 'crehn:fileRequirement:edit', '#', 103, 1, sysdate(), null, null, ''),
(17117, '附件要求删除', 17001, 17, '', '', '', 1, 0, 'F', '0', '0', 'crehn:fileRequirement:remove', '#', 103, 1, sysdate(), null, null, '');

-- 学校账号审核按钮权限
insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17201, '学校账号审核', 17002, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:schoolAccount:review', '#', 103, 1, sysdate(), null, null, ''),
(17202, '学校账号查询', 17002, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:schoolAccount:query', '#', 103, 1, sysdate(), null, null, ''),
(17203, '学校账号修改', 17002, 3, '', '', '', 1, 0, 'F', '0', '0', 'crehn:schoolAccount:edit', '#', 103, 1, sysdate(), null, null, ''),
(17204, '学校账号重置密码', 17002, 4, '', '', '', 1, 0, 'F', '0', '0', 'crehn:schoolAccount:resetPwd', '#', 103, 1, sysdate(), null, null, ''),
(17205, '学校账号删除', 17002, 5, '', '', '', 1, 0, 'F', '0', '0', 'crehn:schoolAccount:remove', '#', 103, 1, sysdate(), null, null, ''),
(17206, '账号创建', 17002, 6, '', '', '', 1, 0, 'F', '0', '0', 'crehn:schoolAccount:add', '#', 103, 1, sysdate(), null, null, ''),
(17207, '学校账号导出', 17002, 7, '', '', '', 1, 0, 'F', '0', '0', 'crehn:schoolAccount:export', '#', 103, 1, sysdate(), null, null, ''),
(17208, '学校账号恢复', 17002, 8, '', '', '', 1, 0, 'F', '0', '0', 'crehn:schoolAccount:restore', '#', 103, 1, sysdate(), null, null, '');

update sys_menu
set menu_name = '账号管理',
    remark = '账号审核、注册码邀请和直接创建账号'
where menu_id = 17002;

update sys_menu
set visible = '1',
    remark = '已合并到账号管理，保留路由和按钮权限'
where menu_id = 17005;

-- 学校项目按钮权限
insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17301, '项目查询', 17003, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:project:query', '#', 103, 1, sysdate(), null, null, ''),
(17302, '项目新增/草稿', 17003, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:project:add', '#', 103, 1, sysdate(), null, null, ''),
(17303, '项目提交', 17003, 3, '', '', '', 1, 0, 'F', '0', '0', 'crehn:project:submit', '#', 103, 1, sysdate(), null, null, ''),
(17304, '材料上传/删除', 17003, 4, '', '', '', 1, 0, 'F', '0', '0', 'crehn:project:upload', '#', 103, 1, sysdate(), null, null, '');

-- 管理老师审核按钮权限
insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17401, '审核查询', 17004, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:audit:query', '#', 103, 1, sysdate(), null, null, ''),
(17402, '审核通过', 17004, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:audit:pass', '#', 103, 1, sysdate(), null, null, ''),
(17403, '审核驳回', 17004, 3, '', '', '', 1, 0, 'F', '0', '0', 'crehn:audit:return', '#', 103, 1, sysdate(), null, null, ''),
(17404, '撤销通过', 17004, 4, '', '', '', 1, 0, 'F', '0', '0', 'crehn:audit:withdrawPass', '#', 103, 1, sysdate(), null, null, '');

-- 注册码管理按钮权限
insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17501, '注册码查询', 17005, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:registration:query', '#', 103, 1, sysdate(), null, null, ''),
(17502, '注册码生成', 17005, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:registration:add', '#', 103, 1, sysdate(), null, null, ''),
(17503, '注册码修改绑定', 17005, 3, '', '', '', 1, 0, 'F', '0', '0', 'crehn:registration:edit', '#', 103, 1, sysdate(), null, null, ''),
(17504, '注册码作废', 17005, 4, '', '', '', 1, 0, 'F', '0', '0', 'crehn:registration:void', '#', 103, 1, sysdate(), null, null, ''),
(17505, '注册码重发', 17005, 5, '', '', '', 1, 0, 'F', '0', '0', 'crehn:registration:resend', '#', 103, 1, sysdate(), null, null, ''),
(17506, '注册码导出', 17005, 6, '', '', '', 1, 0, 'F', '0', '0', 'crehn:registration:export', '#', 103, 1, sysdate(), null, null, ''),
(17507, '注册码使用记录', 17005, 7, '', '', '', 1, 0, 'F', '0', '0', 'crehn:registration:usage', '#', 103, 1, sysdate(), null, null, '');
