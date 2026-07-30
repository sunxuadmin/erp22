-- --------------------------------------------------
-- Art Review M2/M3/M4 test seed data
-- Run after:
--   1. ry_vue_5.X.sql
--   2. ry_job.sql
--   3. ry_workflow.sql
--   4. art_review_core.sql
--   5. art_review_menu.sql
--
-- Purpose:
--   Create test roles, users, one enabled activity, two categories,
--   dynamic field schemas, and file requirements for end-to-end testing.
--
-- Test password for all seeded users: 666666
-- --------------------------------------------------

set names utf8mb4;

-- Test departments / school organization.
insert into sys_dept
(dept_id, tenant_id, parent_id, ancestors, dept_name, dept_category, order_num, leader, phone, email, status, del_flag, create_dept, create_by, create_time, update_by, update_time)
values
(900100, '000000', 0, '0', '艺术评审组织', 'crehn_root', 90, null, '15800000000', 'art-test@example.com', '0', '0', 103, 1, sysdate(), null, null),
(900101, '000000', 900100, '0,900100', '测试学校一', 'crehn_school_900101', 1, null, '15800000001', 'school-test@example.com', '0', '0', 103, 1, sysdate(), null, null)
on duplicate key update
    dept_name = values(dept_name),
    dept_category = values(dept_category),
    parent_id = values(parent_id),
    ancestors = values(ancestors),
    status = values(status),
    del_flag = values(del_flag),
    update_by = 1,
    update_time = sysdate();

-- Test roles.
insert into sys_role
(role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_dept, create_by, create_time, update_by, update_time, remark)
values
(900101, '000000', '艺术评审配置管理员', 'crehn_admin', 90, '1', 1, 1, '0', '0', 103, 1, sysdate(), null, null, '测试角色：活动、类别、字段、附件要求、学校账号审核'),
(900102, '000000', '艺术评审审核老师', 'crehn_auditor', 91, '1', 1, 1, '0', '0', 103, 1, sysdate(), null, null, '测试角色：项目材料审核'),
(900103, '000000', '艺术评审学校用户', 'crehn_school', 92, '5', 1, 1, '0', '0', 103, 1, sysdate(), null, null, '测试角色：学校项目填报')
on duplicate key update
    role_name = values(role_name),
    role_key = values(role_key),
    data_scope = values(data_scope),
    status = values(status),
    del_flag = values(del_flag),
    update_by = 1,
    update_time = sysdate();

-- Role menu permissions for M2/M3/M4.
insert ignore into sys_role_menu (role_id, menu_id)
select 900101, menu_id
from sys_menu
where menu_id in (
    1, 100, 1001, 1003, 1007,
    17000, 17001, 17002, 17005,
    17101, 17102, 17103, 17104, 17105, 17106, 17107, 17108, 17109,
    17110, 17111, 17112, 17113, 17114, 17115, 17116, 17117,
    17201, 17202, 17203, 17204, 17205, 17206, 17207, 17208,
    17501, 17502, 17503, 17504, 17505, 17506, 17507
);

insert ignore into sys_role_menu (role_id, menu_id)
select 900102, menu_id
from sys_menu
where menu_id in (
    17000, 17004,
    17401, 17402, 17403
);

insert ignore into sys_role_menu (role_id, menu_id)
select 900103, menu_id
from sys_menu
where menu_id in (
    17000, 17003,
    17301, 17302, 17303, 17304
);

-- Test users.
-- The password hash below is copied from the base RuoYi test user and corresponds to 666666.
insert into sys_user
(user_id, tenant_id, dept_id, user_name, nick_name, user_type, school_id, school_review_status, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, create_dept, create_by, create_time, update_by, update_time, remark)
values
(900101, '000000', 103, 'crehn_admin', '艺术评审配置管理员', 'sys_user', null, null, 'crehn_admin@example.com', '15800000101', '0', null, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', sysdate(), 103, 1, sysdate(), null, null, '测试账号，密码 666666'),
(900102, '000000', 103, 'crehn_auditor', '艺术评审审核老师', 'sys_user', null, null, 'crehn_auditor@example.com', '15800000102', '0', null, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', sysdate(), 103, 1, sysdate(), null, null, '测试账号，密码 666666'),
(900103, '000000', 900101, 'school_enabled', '测试学校一申报员', 'school', 900101, 'enabled', 'school_enabled@example.com', '15800000103', '0', null, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', sysdate(), 103, 1, sysdate(), null, null, '已审核通过学校账号，密码 666666'),
(900104, '000000', 900101, 'school_pending', '测试学校待审核账号', 'school', 900101, 'pending_review', 'school_pending@example.com', '15800000104', '0', null, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', '127.0.0.1', sysdate(), 103, 1, sysdate(), null, null, '待审核学校账号，密码 666666')
on duplicate key update
    dept_id = values(dept_id),
    nick_name = values(nick_name),
    user_type = values(user_type),
    school_id = values(school_id),
    school_review_status = values(school_review_status),
    email = values(email),
    phonenumber = values(phonenumber),
    password = values(password),
    status = values(status),
    del_flag = values(del_flag),
    update_by = 1,
    update_time = sysdate(),
    remark = values(remark);

insert ignore into sys_user_role (user_id, role_id) values
(900101, 900101),
(900102, 900102),
(900103, 900103),
(900104, 900103);

-- Test activity and categories.
insert into activity
(id, tenant_id, activity_name, edition, year, organizer, undertaker, signup_start_at, signup_end_at, description, status, create_dept, create_by, create_time, update_by, update_time, del_flag)
values
(900101, '000000', '2026 艺术评审测试活动', '测试届', 2026, '测试主办单位', '测试承办单位', date_sub(sysdate(), interval 7 day), date_add(sysdate(), interval 60 day), '用于 M2/M3/M4 最小闭环验收：活动配置、学校填报、材料审核。', 'enabled', 103, 1, sysdate(), null, null, '0')
on duplicate key update
    activity_name = values(activity_name),
    signup_start_at = values(signup_start_at),
    signup_end_at = values(signup_end_at),
    status = values(status),
    update_by = 1,
    update_time = sysdate(),
    del_flag = '0';

update activity
set scope_type = 'assigned',
    edition = '2026美展测试',
    description = '测试活动：已配置学校授权、甲组/乙组、数量限制和比例限制。',
    update_time = sysdate()
where id = 900101;

insert into activity_category
(id, tenant_id, activity_id, parent_id, category_code, category_name, quota_limit, sort_order, enabled, create_dept, create_by, create_time, update_by, update_time, del_flag)
values
(900101, '000000', 900101, 0, 'painting', '绘画作品', 100, 1, 1, 103, 1, sysdate(), null, null, '0'),
(900102, '000000', 900101, 0, 'calligraphy', '书法作品', 100, 2, 1, 103, 1, sysdate(), null, null, '0')
on duplicate key update
    category_name = values(category_name),
    quota_limit = values(quota_limit),
    sort_order = values(sort_order),
    enabled = values(enabled),
    update_by = 1,
    update_time = sysdate(),
    del_flag = '0';

update activity_category
set category_group = '艺术作品',
    check_mode = 'mixed',
    tip_text = '请先选择组别。甲组/乙组和名额比例均由后台规则配置，超额时提交会被后端拦截。',
    rule_json = '{"requiresOriginality":true,"quotaMode":"configured"}',
    update_time = sysdate()
where id in (900101, 900102);

-- Dynamic form fields: painting.
insert into category_field_schema
(id, tenant_id, category_id, field_key, field_label, field_type, required, options_json, validation_json, sensitive_flag, sort_order, create_dept, create_by, create_time, update_by, update_time, del_flag)
values
(900101, '000000', 900101, 'student_name', '姓名', 'input', 1, null, null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(900102, '000000', 900101, 'class_name', '班级', 'input', 1, null, null, 0, 2, 103, 1, sysdate(), null, null, '0'),
(900103, '000000', 900101, 'id_card', '身份证号', 'id_card', 1, null, null, 1, 3, 103, 1, sysdate(), null, null, '0'),
(900104, '000000', 900101, 'work_name', '作品名称', 'input', 1, null, null, 0, 4, 103, 1, sysdate(), null, null, '0'),
(900105, '000000', 900101, 'teacher_name', '指导教师', 'input', 0, null, null, 0, 5, 103, 1, sysdate(), null, null, '0'),
(900106, '000000', 900101, 'work_desc', '作品说明', 'textarea', 0, null, null, 0, 6, 103, 1, sysdate(), null, null, '0'),
(900107, '000000', 900101, 'is_original', '是否原创', 'radio', 1, '["是","否"]', null, 0, 7, 103, 1, sysdate(), null, null, '0'),
(900108, '000000', 900101, 'original_commitment', '原创承诺', 'checkbox', 1, '["本人承诺申报作品为原创或已获得合法授权"]', null, 0, 8, 103, 1, sysdate(), null, null, '0'),
(900109, '000000', 900101, 'display_authorization', '获奖展示授权确认', 'checkbox', 1, '["同意用于本活动获奖展示和宣传"]', null, 0, 9, 103, 1, sysdate(), null, null, '0')
on duplicate key update
    field_label = values(field_label),
    field_type = values(field_type),
    required = values(required),
    options_json = values(options_json),
    sensitive_flag = values(sensitive_flag),
    sort_order = values(sort_order),
    update_by = 1,
    update_time = sysdate(),
    del_flag = '0';

-- Dynamic form fields: calligraphy.
insert into category_field_schema
(id, tenant_id, category_id, field_key, field_label, field_type, required, options_json, validation_json, sensitive_flag, sort_order, create_dept, create_by, create_time, update_by, update_time, del_flag)
values
(900201, '000000', 900102, 'student_name', '姓名', 'input', 1, null, null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(900202, '000000', 900102, 'class_name', '班级', 'input', 1, null, null, 0, 2, 103, 1, sysdate(), null, null, '0'),
(900203, '000000', 900102, 'id_card', '身份证号', 'id_card', 1, null, null, 1, 3, 103, 1, sysdate(), null, null, '0'),
(900204, '000000', 900102, 'work_name', '作品名称', 'input', 1, null, null, 0, 4, 103, 1, sysdate(), null, null, '0'),
(900205, '000000', 900102, 'calligraphy_type', '书体', 'select', 1, '["楷书","行书","隶书","篆书"]', null, 0, 5, 103, 1, sysdate(), null, null, '0'),
(900206, '000000', 900102, 'teacher_name', '指导教师', 'input', 0, null, null, 0, 6, 103, 1, sysdate(), null, null, '0'),
(900207, '000000', 900102, 'is_original', '是否原创', 'radio', 1, '["是","否"]', null, 0, 7, 103, 1, sysdate(), null, null, '0'),
(900208, '000000', 900102, 'authorization_commitment', '授权承诺', 'checkbox', 1, '["确认拥有作品展示、评审和传播所需授权"]', null, 0, 8, 103, 1, sysdate(), null, null, '0')
on duplicate key update
    field_label = values(field_label),
    field_type = values(field_type),
    required = values(required),
    options_json = values(options_json),
    sensitive_flag = values(sensitive_flag),
    sort_order = values(sort_order),
    update_by = 1,
    update_time = sysdate(),
    del_flag = '0';

-- File requirements.
insert into category_file_requirement
(id, tenant_id, category_id, file_type_code, file_type_name, allowed_ext, max_size_mb, min_count, max_count, required, sort_order, create_dept, create_by, create_time, update_by, update_time, del_flag)
values
(900101, '000000', 900101, 'photo', '作品照片', 'jpg,jpeg,png', 20, 1, 3, 1, 1, 103, 1, sysdate(), null, null, '0'),
(900102, '000000', 900101, 'document', '申报表文档', 'doc,docx,pdf', 20, 1, 1, 1, 2, 103, 1, sysdate(), null, null, '0'),
(900103, '000000', 900101, 'video', '展示视频', 'mp4,mov', 500, 0, 1, 0, 3, 103, 1, sysdate(), null, null, '0'),
(900201, '000000', 900102, 'photo', '作品照片', 'jpg,jpeg,png', 20, 1, 3, 1, 1, 103, 1, sysdate(), null, null, '0'),
(900202, '000000', 900102, 'document', '申报表文档', 'doc,docx,pdf', 20, 1, 1, 1, 2, 103, 1, sysdate(), null, null, '0'),
(900203, '000000', 900102, 'archive', '过程材料压缩包', 'zip,rar,7z', 200, 0, 1, 0, 3, 103, 1, sysdate(), null, null, '0')
on duplicate key update
    file_type_name = values(file_type_name),
    allowed_ext = values(allowed_ext),
    max_size_mb = values(max_size_mb),
    min_count = values(min_count),
    max_count = values(max_count),
    required = values(required),
    sort_order = values(sort_order),
    update_by = 1,
    update_time = sysdate(),
    del_flag = '0';

-- Config-rule supplement: schools, activity authorization, groups and quota ratios.
insert into school_info
(id, tenant_id, school_name, school_code, school_type, region, address, contact_name, contact_phone, contact_email, profile_json, status, remark, create_dept, create_by, create_time, del_flag)
values
(900101, '000000', '测试学校一', 'TEST-SCHOOL-001', '本科院校', '测试地区', '测试地址一号', '测试联系人', '15800000103', 'school-test@example.com', '{"school_type":"本科院校"}', 'enabled', '测试本科院校', 103, 1, sysdate(), '0'),
(900102, '000000', '测试专科学校', 'TEST-SCHOOL-002', '高职高专', '测试地区', '测试地址二号', '专科联系人', '15800000105', 'school-college@example.com', '{"school_type":"高职高专"}', 'enabled', '测试专科院校', 103, 1, sysdate(), '0'),
(900103, '000000', '测试自定义学校', 'TEST-SCHOOL-003', '自定义', '测试地区', '测试地址三号', '自定义联系人', '15800000106', 'school-custom@example.com', '{"school_type":"自定义"}', 'enabled', '测试自定义学校类型', 103, 1, sysdate(), '0')
on duplicate key update
    school_name = values(school_name),
    school_type = values(school_type),
    contact_name = values(contact_name),
    contact_phone = values(contact_phone),
    profile_json = values(profile_json),
    status = values(status),
    update_by = 1,
    update_time = sysdate(),
    del_flag = '0';

insert into activity_school_scope
(id, tenant_id, activity_id, school_id, enabled, assigned_by, assigned_at, remark, create_dept, create_by, create_time, del_flag)
values
(900101, '000000', 900101, 900101, 1, 1, sysdate(), '测试本科院校授权', 103, 1, sysdate(), '0'),
(900102, '000000', 900101, 900102, 1, 1, sysdate(), '测试专科院校授权', 103, 1, sysdate(), '0'),
(900103, '000000', 900101, 900103, 1, 1, sysdate(), '测试自定义学校授权', 103, 1, sysdate(), '0')
on duplicate key update
    enabled = values(enabled),
    assigned_by = values(assigned_by),
    assigned_at = values(assigned_at),
    remark = values(remark),
    update_by = 1,
    update_time = sysdate(),
    del_flag = '0';

-- Legacy activity_quota_rule test seeds retired.
-- Use activity_report_rule/current activity rules for quota and ratio test data.

-- Submitted project for auditor smoke test.
insert into project
(id, tenant_id, activity_id, school_id, category_id, project_no, project_name, form_data_json, status, submitted_at, submitted_by, current_audit_opinion, create_dept, create_by, create_time, update_by, update_time, del_flag)
values
(900101, '000000', 900101, 900101, 900101, 'TEST-SUBMITTED-001', '审核老师待审测试项目', '{"student_name":"测试学生","class_name":"一班","id_card":"110101200001019999","work_name":"测试绘画作品","teacher_name":"测试教师","work_desc":"用于 crehn_auditor 待审核列表验证"}', 'submitted', sysdate(), 900103, null, 103, 900103, sysdate(), null, null, '0')
on duplicate key update
    project_name = values(project_name),
    form_data_json = values(form_data_json),
    status = values(status),
    submitted_at = values(submitted_at),
    submitted_by = values(submitted_by),
    current_audit_opinion = null,
    update_by = 1,
    update_time = sysdate(),
    del_flag = '0';

-- Registration codes for public registration smoke test.
insert into registration_code
(id, tenant_id, code, code_type, activity_id, school_id, bound_phone, role_key, auto_approve, max_use_count, used_count, expire_at, status, remark, create_dept, create_by, create_time, update_by, update_time, del_flag)
values
(900101, '000000', 'SCH2026TEST001', 'school', 900101, 900101, null, 'crehn_school', 1, 5, 0, date_add(sysdate(), interval 30 day), 'unused', '测试学校注册码，自动审核并授权测试活动', 103, 1, sysdate(), null, null, '0'),
(900102, '000000', 'EXP2026TEST001', 'expert', 900101, null, null, 'crehn_expert', 0, 5, 0, date_add(sysdate(), interval 30 day), 'unused', '测试专家注册码，先进入待审核', 103, 1, sysdate(), null, null, '0'),
(900103, '000000', 'ADM2026TEST001', 'admin', 900101, null, null, 'crehn_admin', 0, 1, 0, date_add(sysdate(), interval 30 day), 'unused', '后台管理员注册码元数据测试，不允许前台注册管理员', 103, 1, sysdate(), null, null, '0')
on duplicate key update
    code = values(code),
    code_type = values(code_type),
    activity_id = values(activity_id),
    school_id = values(school_id),
    role_key = values(role_key),
    auto_approve = values(auto_approve),
    max_use_count = values(max_use_count),
    expire_at = values(expire_at),
    status = values(status),
    remark = values(remark),
    update_by = 1,
    update_time = sysdate(),
    del_flag = '0';

select 'art_review_seed_test.sql imported' as result;
