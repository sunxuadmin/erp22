-- --------------------------------------------------
-- Guide-style submission templates
-- Source: docs/06-报送配置梳理表.md
-- Run after:
--   art_review_core.sql
--   art_review_config_rules.sql
--
-- Safe to rerun. This script seeds one enabled activity and fixed
-- guide-style categories, fields, and material requirements.
-- --------------------------------------------------

set names utf8mb4;

set @activity_id = 910001;

insert into activity
(id, tenant_id, activity_name, edition, year, organizer, undertaker, signup_start_at, signup_end_at, description, status, create_dept, create_by, create_time, update_by, update_time, del_flag)
values
(@activity_id, '000000', '2026 大学生艺术展演节目和作品报送', '2026', 2026, '通识教育学院', '通识教育学院',
 date_sub(sysdate(), interval 7 day), date_add(sysdate(), interval 120 day),
 '按报送配置梳理表预置的校内报送活动。', 'enabled', 103, 1, sysdate(), null, null, '0')
on duplicate key update
    id = id;

-- Preserve operator-maintained category config before reseeding fixed categories.
drop temporary table if exists tmp_art_review_category_runtime;
create temporary table tmp_art_review_category_runtime as
select id, tenant_id, activity_id, parent_id, category_code, category_name, quota_limit,
       category_group, rule_json, tip_text, check_mode, sort_order, enabled,
       create_dept, create_by, create_time, update_by, update_time, del_flag
from activity_category
where activity_id = @activity_id
  and id between 910101 and 910199;

-- Preserve operator-maintained field and attachment config before reseeding.
drop temporary table if exists tmp_art_review_field_schema_runtime;
create temporary table tmp_art_review_field_schema_runtime as
select id, tenant_id, category_id, field_key, field_label, field_type, required,
       options_json, validation_json, sensitive_flag, sort_order,
       create_dept, create_by, create_time, update_by, update_time, del_flag
from category_field_schema
where category_id between 910101 and 910199;

drop temporary table if exists tmp_art_review_file_requirement_runtime;
create temporary table tmp_art_review_file_requirement_runtime as
select *
from category_file_requirement
where category_id between 910101 and 910199;

delete from category_file_requirement where category_id between 910101 and 910199;
delete from category_field_schema where category_id between 910101 and 910199;
delete from activity_category where activity_id = @activity_id and id between 910101 and 910199;

-- --------------------------------------------------
-- Categories
-- --------------------------------------------------

insert into activity_category
(id, tenant_id, activity_id, parent_id, category_code, category_name, quota_limit, category_group, rule_json, tip_text, check_mode, sort_order, enabled, create_dept, create_by, create_time, update_by, update_time, del_flag)
values
(910101, '000000', @activity_id, 0, 'performance_vocal', '声乐', null, '艺术表演类', '{"templateCode":"tpl_performance_vocal","validatorType":"performance","projectName":{"label":"节目名称","required":true},"maxTeacherCount":3,"maxDurationSecondsByProjectNature":{"合唱":480,"小合唱":360,"表演唱":360,"个人项目":300}}', '合唱可填两首曲目，小合唱/表演唱只填一首。', 'mixed', 101, 1, 103, 1, sysdate(), null, null, '0'),
(910102, '000000', @activity_id, 0, 'performance_instrumental', '器乐', null, '艺术表演类', '{"templateCode":"tpl_performance_instrumental","validatorType":"performance","projectName":{"label":"节目名称","required":true},"maxTeacherCount":3}', '伴奏、指挥等身份请在人员备注中注明。', 'mixed', 102, 1, 103, 1, sysdate(), null, null, '0'),
(910103, '000000', @activity_id, 0, 'performance_dance', '舞蹈', null, '艺术表演类', '{"templateCode":"tpl_performance_dance","validatorType":"performance","projectName":{"label":"节目名称","required":true},"maxTeacherCount":3}', '填写节目时长、作品简述和指导老师，上传节目视频。', 'mixed', 103, 1, 103, 1, sysdate(), null, null, '0'),
(910104, '000000', @activity_id, 0, 'performance_drama', '戏剧', null, '艺术表演类', '{"templateCode":"tpl_performance_drama","validatorType":"performance","projectName":{"label":"节目名称","required":true},"maxTeacherCount":3}', '填写节目时长、作品简述和指导老师，上传节目视频。', 'mixed', 104, 1, 103, 1, sysdate(), null, null, '0'),
(910105, '000000', @activity_id, 0, 'performance_recitation', '朗诵', null, '艺术表演类', '{"templateCode":"tpl_performance_recitation","validatorType":"performance","projectName":{"label":"节目名称","required":true},"maxTeacherCount":3}', '填写节目时长、作品简述和指导老师，上传节目视频。', 'mixed', 105, 1, 103, 1, sysdate(), null, null, '0'),
(910106, '000000', @activity_id, 0, 'performance_personal', '个人项目', null, '艺术表演类', '{"templateCode":"tpl_performance_personal","validatorType":"performance","projectName":{"label":"节目名称","required":true},"maxTeacherCount":1}', '备注中注明演唱、演奏、伴奏等身份。', 'mixed', 106, 1, 103, 1, sysdate(), null, null, '0'),
(910111, '000000', @activity_id, 0, 'artwork_fine_art', '美术类', null, '艺术作品类', '{"templateCode":"tpl_artwork_fine_art","validatorType":"artwork","projectName":{"label":"作品名称","required":true},"maxAuthorCount":3,"teacherCount":1}', '填写类别、组别、尺寸、创作时间、创作说明并上传作品。', 'mixed', 201, 1, 103, 1, sysdate(), null, null, '0'),
(910112, '000000', @activity_id, 0, 'artwork_grand_design', '大艺展设计类', null, '艺术作品类', '{"templateCode":"tpl_artwork_grand_design","validatorType":"artwork","projectName":{"label":"作品名称","required":true},"maxAuthorCount":3,"teacherCount":1}', '不分组别，额外上传 2-5 张过程性作品。', 'mixed', 202, 1, 103, 1, sysdate(), null, null, '0'),
(910113, '000000', @activity_id, 0, 'artwork_design', '设计展', null, '艺术作品类', '{"templateCode":"tpl_artwork_design","validatorType":"artwork","projectName":{"label":"作品名称","required":true},"maxAuthorCount":3,"teacherCount":1}', '作者信息直接在表单中填写，并额外上传 2-5 张过程性作品。', 'mixed', 203, 1, 103, 1, sysdate(), null, null, '0'),
(910114, '000000', @activity_id, 0, 'artwork_film', '影视类', null, '艺术作品类', '{"templateCode":"tpl_artwork_film","validatorType":"artwork","projectName":{"label":"作品名称","required":true},"filmMaxAuthorCount":6,"filmMaxTeacherCount":3,"filmVideoRules":{"短片":{"maxDurationSeconds":900},"动画":{"maxDurationSeconds":900},"纪录片":{"maxDurationSeconds":1800}}}', '按影视作品类别校验时长。', 'mixed', 204, 1, 103, 1, sysdate(), null, null, '0'),
(910115, '000000', @activity_id, 0, 'artwork_principal', '高校校长书画作品', null, '高校校长书画作品', '{"templateCode":"tpl_artwork_principal_calligraphy_painting","validatorType":"artwork","projectName":{"label":"作品名称","required":true},"maxAuthorCount":1,"teacherCount":0}', '高校校长书画作品作者信息直接在表单中填写，作品类别限定书法或绘画。', 'mixed', 205, 1, 103, 1, sysdate(), null, null, '0'),
(910121, '000000', @activity_id, 0, 'workshop', '艺术实践工作坊', null, '艺术实践工作坊', '{"templateCode":"tpl_workshop_practice","validatorType":"workshop","projectName":{"label":"工作坊名称","required":true},"minTeacherCount":1,"maxTeacherCount":3,"minStudentCount":7,"maxStudentCount":9,"maxTotalMemberCount":12,"maxVideoDurationSeconds":480}', '上传工作坊视频和项目介绍文档；教师 1-3 人，学生 7-9 人。', 'mixed', 301, 1, 103, 1, sysdate(), null, null, '0'),
(910131, '000000', @activity_id, 0, 'achievement_paper', '学术论文', null, '美育改革创新优秀成果', '{"templateCode":"tpl_achievement_paper","validatorType":"aesthetic_achievement","projectName":{"label":"论文标题","required":true},"paperMaxAuthorCount":2}', '填写标题、简介、作者信息并上传 Word 正文。', 'mixed', 401, 1, 103, 1, sysdate(), null, null, '0'),
(910132, '000000', @activity_id, 0, 'achievement_case', '教学改革案例', null, '美育改革创新优秀成果', '{"templateCode":"tpl_achievement_case","validatorType":"aesthetic_achievement","projectName":{"label":"案例标题","required":true},"caseMaxCompleterCount":3,"caseMaxVideoCount":1,"caseMaxImageCount":5}', '填写标题、简介、完成人信息，上传 Word 正文，可选视频和图片。', 'mixed', 402, 1, 103, 1, sysdate(), null, null, '0');

update activity_category c
join tmp_art_review_category_runtime t
  on t.activity_id = c.activity_id
 and t.id = c.id
 and t.category_code = c.category_code
set c.tenant_id = t.tenant_id,
    c.parent_id = t.parent_id,
    c.category_name = t.category_name,
    c.quota_limit = t.quota_limit,
    c.category_group = t.category_group,
    c.rule_json = t.rule_json,
    c.tip_text = t.tip_text,
    c.check_mode = t.check_mode,
    c.sort_order = t.sort_order,
    c.enabled = t.enabled,
    c.create_dept = t.create_dept,
    c.create_by = t.create_by,
    c.create_time = t.create_time,
    c.update_by = t.update_by,
    c.update_time = t.update_time,
    c.del_flag = t.del_flag
where c.activity_id = @activity_id
  and c.id between 910101 and 910199;

insert into activity_category
(id, tenant_id, activity_id, parent_id, category_code, category_name, quota_limit, category_group, rule_json, tip_text, check_mode, sort_order, enabled, create_dept, create_by, create_time, update_by, update_time, del_flag)
select t.id, t.tenant_id, t.activity_id, t.parent_id, t.category_code, t.category_name,
       t.quota_limit, t.category_group, t.rule_json, t.tip_text, t.check_mode,
       t.sort_order, t.enabled, t.create_dept, t.create_by, t.create_time,
       t.update_by, t.update_time, t.del_flag
from tmp_art_review_category_runtime t
left join activity_category c_id on c_id.id = t.id
left join activity_category c_code
  on c_code.activity_id = t.activity_id
 and c_code.category_code = t.category_code
where c_id.id is null
  and c_code.id is null;

drop temporary table if exists tmp_art_review_category_runtime;

-- --------------------------------------------------
-- Fields
-- --------------------------------------------------

insert into category_field_schema
(id, tenant_id, category_id, field_key, field_label, field_type, required, options_json, validation_json, sensitive_flag, sort_order, create_dept, create_by, create_time, update_by, update_time, del_flag)
values
-- Performance fields. These are repeated per subtype for simple auditing.
(91010101, '000000', 910101, 'programForm', '节目形式', 'select', 1, '["合唱","小合唱","表演唱","独唱"]', null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(91010102, '000000', 910101, 'displayGroup', '展演组别', 'select', 1, '["甲组","乙组"]', null, 0, 2, 103, 1, sysdate(), null, null, '0'),
(91010103, '000000', 910101, 'isOriginal', '是否原创', 'radio', 1, '["是","否"]', null, 0, 3, 103, 1, sysdate(), null, null, '0'),
(91010104, '000000', 910101, 'applyOriginalAward', '是否申报灼华奖', 'radio', 1, '["是","否"]', null, 0, 4, 103, 1, sysdate(), null, null, '0'),
(91010105, '000000', 910101, 'durationText', '节目时长', 'input', 1, null, null, 0, 5, 103, 1, sysdate(), null, null, '0'),
(91010106, '000000', 910101, 'track1Name', '曲目1名称', 'input', 1, null, null, 0, 6, 103, 1, sysdate(), null, null, '0'),
(91010107, '000000', 910101, 'track1Author', '曲目1词曲作者', 'input', 1, null, null, 0, 7, 103, 1, sysdate(), null, null, '0'),
(91010108, '000000', 910101, 'track2Name', '曲目2名称', 'input', 0, null, null, 0, 8, 103, 1, sysdate(), null, null, '0'),
(91010109, '000000', 910101, 'track2Author', '曲目2词曲作者', 'input', 0, null, null, 0, 9, 103, 1, sysdate(), null, null, '0'),
(91010110, '000000', 910101, 'workDescription', '作品简述', 'textarea', 1, null, '{"maxLength":200}', 0, 10, 103, 1, sysdate(), null, null, '0'),
(91010111, '000000', 910101, 'adviserNames', '指导老师', 'input', 0, null, null, 0, 11, 103, 1, sysdate(), null, null, '0'),
(91010201, '000000', 910102, 'programForm', '节目形式', 'select', 1, '["合奏","小合奏","独奏"]', null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(91010202, '000000', 910102, 'displayGroup', '展演组别', 'select', 1, '["甲组","乙组"]', null, 0, 2, 103, 1, sysdate(), null, null, '0'),
(91010203, '000000', 910102, 'isOriginal', '是否原创', 'radio', 1, '["是","否"]', null, 0, 3, 103, 1, sysdate(), null, null, '0'),
(91010204, '000000', 910102, 'durationText', '节目时长', 'input', 1, null, null, 0, 4, 103, 1, sysdate(), null, null, '0'),
(91010205, '000000', 910102, 'workDescription', '作品简述', 'textarea', 1, null, '{"maxLength":200}', 0, 5, 103, 1, sysdate(), null, null, '0'),
(91010206, '000000', 910102, 'adviserNames', '指导老师', 'input', 0, null, null, 0, 6, 103, 1, sysdate(), null, null, '0'),
(91010301, '000000', 910103, 'programForm', '节目形式', 'select', 1, '["群舞","双人舞","三人舞","独舞"]', null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(91010302, '000000', 910103, 'displayGroup', '展演组别', 'select', 1, '["甲组","乙组"]', null, 0, 2, 103, 1, sysdate(), null, null, '0'),
(91010303, '000000', 910103, 'isOriginal', '是否原创', 'radio', 1, '["是","否"]', null, 0, 3, 103, 1, sysdate(), null, null, '0'),
(91010304, '000000', 910103, 'durationText', '节目时长', 'input', 1, null, null, 0, 4, 103, 1, sysdate(), null, null, '0'),
(91010305, '000000', 910103, 'workDescription', '作品简述', 'textarea', 1, null, '{"maxLength":200}', 0, 5, 103, 1, sysdate(), null, null, '0'),
(91010306, '000000', 910103, 'adviserNames', '指导老师', 'input', 0, null, null, 0, 6, 103, 1, sysdate(), null, null, '0'),
(91010401, '000000', 910104, 'programForm', '节目形式', 'select', 1, '["戏剧","小品","音乐剧"]', null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(91010402, '000000', 910104, 'displayGroup', '展演组别', 'select', 1, '["甲组","乙组"]', null, 0, 2, 103, 1, sysdate(), null, null, '0'),
(91010403, '000000', 910104, 'isOriginal', '是否原创', 'radio', 1, '["是","否"]', null, 0, 3, 103, 1, sysdate(), null, null, '0'),
(91010404, '000000', 910104, 'durationText', '节目时长', 'input', 1, null, null, 0, 4, 103, 1, sysdate(), null, null, '0'),
(91010405, '000000', 910104, 'workDescription', '作品简述', 'textarea', 1, null, '{"maxLength":200}', 0, 5, 103, 1, sysdate(), null, null, '0'),
(91010406, '000000', 910104, 'adviserNames', '指导老师', 'input', 0, null, null, 0, 6, 103, 1, sysdate(), null, null, '0'),
(91010501, '000000', 910105, 'programForm', '节目形式', 'select', 1, '["朗诵","集体朗诵","个人朗诵"]', null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(91010502, '000000', 910105, 'displayGroup', '展演组别', 'select', 1, '["甲组","乙组"]', null, 0, 2, 103, 1, sysdate(), null, null, '0'),
(91010503, '000000', 910105, 'isOriginal', '是否原创', 'radio', 1, '["是","否"]', null, 0, 3, 103, 1, sysdate(), null, null, '0'),
(91010504, '000000', 910105, 'durationText', '节目时长', 'input', 1, null, null, 0, 4, 103, 1, sysdate(), null, null, '0'),
(91010505, '000000', 910105, 'workDescription', '作品简述', 'textarea', 1, null, '{"maxLength":200}', 0, 5, 103, 1, sysdate(), null, null, '0'),
(91010506, '000000', 910105, 'adviserNames', '指导老师', 'input', 0, null, null, 0, 6, 103, 1, sysdate(), null, null, '0'),
(91010601, '000000', 910106, 'programForm', '节目形式', 'select', 1, '["独唱","独奏","个人朗诵","其他"]', null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(91010602, '000000', 910106, 'displayGroup', '展演组别', 'select', 1, '["甲组","乙组"]', null, 0, 2, 103, 1, sysdate(), null, null, '0'),
(91010603, '000000', 910106, 'isOriginal', '是否原创', 'radio', 1, '["是","否"]', null, 0, 3, 103, 1, sysdate(), null, null, '0'),
(91010604, '000000', 910106, 'durationText', '节目时长', 'input', 1, null, null, 0, 4, 103, 1, sysdate(), null, null, '0'),
(91010605, '000000', 910106, 'workDescription', '作品简述', 'textarea', 1, null, '{"maxLength":200}', 0, 5, 103, 1, sysdate(), null, null, '0'),
(91010606, '000000', 910106, 'adviserNames', '指导老师', 'input', 0, null, null, 0, 6, 103, 1, sysdate(), null, null, '0'),
-- Artwork fields
(91011101, '000000', 910111, 'workType', '作品类别', 'select', 1, '["绘画","书法","篆刻","摄影"]', null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(91011102, '000000', 910111, 'displayGroup', '组别', 'select', 1, '["甲组","乙组"]', null, 0, 2, 103, 1, sysdate(), null, null, '0'),
(91011103, '000000', 910111, 'sizeText', '尺寸', 'input', 0, null, null, 0, 3, 103, 1, sysdate(), null, null, '0'),
(91011104, '000000', 910111, 'createdAtText', '创作时间', 'input', 0, null, null, 0, 4, 103, 1, sysdate(), null, null, '0'),
(91011105, '000000', 910111, 'creationDescription', '创作说明', 'textarea', 1, null, '{"maxLength":400}', 0, 5, 103, 1, sysdate(), null, null, '0'),
(91011106, '000000', 910111, 'adviserNames', '指导教师', 'input', 0, null, null, 0, 6, 103, 1, sysdate(), null, null, '0'),
(91011201, '000000', 910112, 'workType', '作品类别', 'select', 1, '["视觉传达","环境设计","产品设计","数字媒体设计","其他"]', null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(91011202, '000000', 910112, 'sizeText', '尺寸', 'input', 0, null, null, 0, 2, 103, 1, sysdate(), null, null, '0'),
(91011203, '000000', 910112, 'createdAtText', '创作时间', 'input', 0, null, null, 0, 3, 103, 1, sysdate(), null, null, '0'),
(91011204, '000000', 910112, 'creationDescription', '创作说明', 'textarea', 1, null, '{"maxLength":400}', 0, 4, 103, 1, sysdate(), null, null, '0'),
(91011205, '000000', 910112, 'adviserNames', '指导教师', 'input', 0, null, null, 0, 5, 103, 1, sysdate(), null, null, '0'),
(91011301, '000000', 910113, 'workType', '作品类别', 'select', 1, '["视觉传达","环境设计","产品设计","数字媒体设计","其他"]', null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(91011302, '000000', 910113, 'sizeText', '尺寸', 'input', 0, null, null, 0, 2, 103, 1, sysdate(), null, null, '0'),
(91011303, '000000', 910113, 'createdAtText', '创作时间', 'input', 0, null, null, 0, 3, 103, 1, sysdate(), null, null, '0'),
(91011304, '000000', 910113, 'authorNames', '作者信息', 'textarea', 1, null, null, 0, 4, 103, 1, sysdate(), null, null, '0'),
(91011305, '000000', 910113, 'creationDescription', '创作说明', 'textarea', 1, null, '{"maxLength":400}', 0, 5, 103, 1, sysdate(), null, null, '0'),
(91011401, '000000', 910114, 'workType', '作品类别', 'select', 1, '["短片","动画","纪录片"]', null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(91011402, '000000', 910114, 'sizeText', '尺寸/规格', 'input', 0, null, null, 0, 2, 103, 1, sysdate(), null, null, '0'),
(91011403, '000000', 910114, 'createdAtText', '创作时间', 'input', 0, null, null, 0, 3, 103, 1, sysdate(), null, null, '0'),
(91011404, '000000', 910114, 'creationDescription', '创作说明', 'textarea', 1, null, '{"maxLength":400}', 0, 4, 103, 1, sysdate(), null, null, '0'),
(91011405, '000000', 910114, 'adviserNames', '指导教师', 'input', 0, null, null, 0, 5, 103, 1, sysdate(), null, null, '0'),
(91011501, '000000', 910115, 'workType', '作品类别', 'select', 1, '["书法","绘画"]', null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(91011502, '000000', 910115, 'sizeText', '尺寸', 'input', 0, null, null, 0, 2, 103, 1, sysdate(), null, null, '0'),
(91011503, '000000', 910115, 'createdAtText', '创作时间', 'input', 0, null, null, 0, 3, 103, 1, sysdate(), null, null, '0'),
(91011504, '000000', 910115, 'authorNames', '校长作者信息', 'textarea', 1, null, null, 0, 4, 103, 1, sysdate(), null, null, '0'),
(91011505, '000000', 910115, 'creationDescription', '创作说明', 'textarea', 1, null, '{"maxLength":400}', 0, 5, 103, 1, sysdate(), null, null, '0'),
-- Workshop
(91012101, '000000', 910121, 'projectIntro', '项目介绍', 'textarea', 1, null, null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(91012102, '000000', 910121, 'designIdea', '设计思路', 'textarea', 1, null, null, 0, 2, 103, 1, sysdate(), null, null, '0'),
(91012103, '000000', 910121, 'featureDescription', '特色说明', 'textarea', 1, null, null, 0, 3, 103, 1, sysdate(), null, null, '0'),
(91012104, '000000', 910121, 'exhibitionDesignPlan', '展示设计方案', 'textarea', 1, null, null, 0, 4, 103, 1, sysdate(), null, null, '0'),
(91012105, '000000', 910121, 'noPreviousAwardCommitment', '未获往届奖项承诺', 'checkbox', 1, '["确认未获往届同类奖项"]', null, 0, 5, 103, 1, sysdate(), null, null, '0'),
-- Achievements
(91013101, '000000', 910131, 'topicCategory', '选题类别', 'radio', 1, '["01 学校美育的内涵与价值功能","02 中华美育精神的内在意蕴与时代价值","03 学校美育浸润行动的实践路径","04 高校教师美育素养提升路径","05 高校学科美育与创新型人才培养","06 “五育并举”视域下高校美育育人模式创新","07 新时代高校美育评价改革","08 中华优秀传统文化融入高校美育的策略与路径","09 高校数字美育新生态构建","10 艺术师范教育改革发展","11 高校艺术教育中外比较","12 高校美育赋能经济社会发展实施路径"]', null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(91013102, '000000', 910131, 'paperAbstract', '简介/摘要', 'textarea', 1, null, '{"minLength":250,"maxLength":350}', 0, 2, 103, 1, sysdate(), null, null, '0'),
(91013103, '000000', 910131, 'keywords', '关键词', 'input', 1, null, null, 0, 3, 103, 1, sysdate(), null, null, '0'),
(91013104, '000000', 910131, 'bodyText', '正文文本', 'textarea', 1, null, '{"minLength":5000}', 0, 4, 103, 1, sysdate(), null, null, '0'),
(91013105, '000000', 910131, 'references', '参考文献', 'textarea', 1, null, null, 0, 5, 103, 1, sysdate(), null, null, '0'),
(91013106, '000000', 910131, 'unpublishedCommitment', '未公开发表承诺', 'checkbox', 1, '["确认未公开发表"]', null, 0, 6, 103, 1, sysdate(), null, null, '0'),
(91013201, '000000', 910132, 'topicCategory', '选题类别', 'radio', 1, '["01 高校公共艺术教育教学体系建设","02 高校美育课程与教材建设","03 高校学生艺术社团及实践工作坊建设","04 高校跨学科美育实践","05 高校美育专门机构和教师队伍建设","06 高校美育评价制度建设","07 美育名师工作室建设","08 中华优秀传统文化艺术传承基地建设","09 艺术展演育人实践","10 艺术师范教育基本功展示引领人才培养","11 高校助力乡村学校美育提质发展","12 高校美育资源与社会艺术资源共建共享"]', null, 0, 1, 103, 1, sysdate(), null, null, '0'),
(91013202, '000000', 910132, 'unitName', '申报单位', 'input', 1, null, null, 0, 2, 103, 1, sysdate(), null, null, '0'),
(91013203, '000000', 910132, 'caseText', '案例正文', 'textarea', 1, null, '{"maxLength":5000}', 0, 3, 103, 1, sysdate(), null, null, '0'),
(91013204, '000000', 910132, 'completerNames', '完成人信息', 'textarea', 1, null, null, 0, 4, 103, 1, sysdate(), null, null, '0');

update category_field_schema f
join tmp_art_review_field_schema_runtime t
  on t.category_id = f.category_id
 and t.field_key = f.field_key
set f.tenant_id = t.tenant_id,
    f.field_label = t.field_label,
    f.field_type = t.field_type,
    f.required = t.required,
    f.options_json = t.options_json,
    f.validation_json = t.validation_json,
    f.sensitive_flag = t.sensitive_flag,
    f.sort_order = t.sort_order,
    f.create_dept = t.create_dept,
    f.create_by = t.create_by,
    f.create_time = t.create_time,
    f.update_by = t.update_by,
    f.update_time = t.update_time,
    f.del_flag = t.del_flag
where f.category_id between 910101 and 910199;

insert into category_field_schema
(id, tenant_id, category_id, field_key, field_label, field_type, required, options_json, validation_json, sensitive_flag, sort_order, create_dept, create_by, create_time, update_by, update_time, del_flag)
select t.id, t.tenant_id, t.category_id, t.field_key, t.field_label, t.field_type,
       t.required, t.options_json, t.validation_json, t.sensitive_flag, t.sort_order,
       t.create_dept, t.create_by, t.create_time, t.update_by, t.update_time, t.del_flag
from tmp_art_review_field_schema_runtime t
left join category_field_schema f_id on f_id.id = t.id
left join category_field_schema f_key
  on f_key.category_id = t.category_id
 and f_key.field_key = t.field_key
where f_id.id is null
  and f_key.id is null;

-- Default performance program form to radio unless the field was edited online.
update category_field_schema
set field_type = 'radio'
where tenant_id = '000000'
  and category_id between 910101 and 910106
  and field_key = 'programForm'
  and update_time is null;

-- --------------------------------------------------
-- Material requirements
-- stage in rule_json: initial / post_selection / per_member
-- --------------------------------------------------

insert into category_file_requirement
(id, tenant_id, category_id, file_type_code, file_type_name, allowed_ext, max_size_mb, min_count, max_count, required, rule_json, sort_order, create_dept, create_by, create_time, update_by, update_time, del_flag)
values
-- Performance materials
(91010101, '000000', 910101, 'performance_video', '节目视频', 'mp4,mov', 1024, 1, 1, 1, '{"stage":"initial","mediaType":"video","minWidth":1920,"minHeight":1080,"fps":25,"fpsTolerance":0.5,"minBitrateMbps":10,"manualCheckTips":["视频不得出现省份、学校、姓名、指导老师等信息。"]}', 1, 103, 1, sysdate(), null, null, '0'),
(91010102, '000000', 910101, 'sealed_program_form', '节目盖章表', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection","manualCheckTips":["选送省平台后补传，核验盖章。"]}', 2, 103, 1, sysdate(), null, null, '0'),
(91010103, '000000', 910101, 'review_opinion', '审查意见书', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection","manualCheckTips":["选送省平台后补传，使用通用模板并盖章扫描。"]}', 3, 103, 1, sysdate(), null, null, '0'),
(91010104, '000000', 910101, 'copyright_statement', '版权声明书', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection","condition":"applyOriginalAward=是","manualCheckTips":["申报原创奖时补传，核验版权声明。"]}', 4, 103, 1, sysdate(), null, null, '0'),
(91010201, '000000', 910102, 'performance_video', '节目视频', 'mp4,mov', 1024, 1, 1, 1, '{"stage":"initial","mediaType":"video","manualCheckTips":["视频不得出现省份、学校、姓名、指导老师等信息。"]}', 1, 103, 1, sysdate(), null, null, '0'),
(91010202, '000000', 910102, 'sealed_program_form', '节目盖章表', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection"}', 2, 103, 1, sysdate(), null, null, '0'),
(91010203, '000000', 910102, 'review_opinion', '审查意见书', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection"}', 3, 103, 1, sysdate(), null, null, '0'),
(91010204, '000000', 910102, 'copyright_statement', '版权声明书', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection","condition":"isOriginal=是"}', 4, 103, 1, sysdate(), null, null, '0'),
(91010301, '000000', 910103, 'performance_video', '节目视频', 'mp4,mov', 1024, 1, 1, 1, '{"stage":"initial","mediaType":"video"}', 1, 103, 1, sysdate(), null, null, '0'),
(91010302, '000000', 910103, 'sealed_program_form', '节目盖章表', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection"}', 2, 103, 1, sysdate(), null, null, '0'),
(91010303, '000000', 910103, 'review_opinion', '审查意见书', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection"}', 3, 103, 1, sysdate(), null, null, '0'),
(91010304, '000000', 910103, 'copyright_statement', '版权声明书', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection","condition":"isOriginal=是"}', 4, 103, 1, sysdate(), null, null, '0'),
(91010401, '000000', 910104, 'performance_video', '节目视频', 'mp4,mov', 1024, 1, 1, 1, '{"stage":"initial","mediaType":"video"}', 1, 103, 1, sysdate(), null, null, '0'),
(91010402, '000000', 910104, 'sealed_program_form', '节目盖章表', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection"}', 2, 103, 1, sysdate(), null, null, '0'),
(91010403, '000000', 910104, 'review_opinion', '审查意见书', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection"}', 3, 103, 1, sysdate(), null, null, '0'),
(91010404, '000000', 910104, 'copyright_statement', '版权声明书', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection","condition":"isOriginal=是"}', 4, 103, 1, sysdate(), null, null, '0'),
(91010501, '000000', 910105, 'performance_video', '节目视频', 'mp4,mov', 1024, 1, 1, 1, '{"stage":"initial","mediaType":"video"}', 1, 103, 1, sysdate(), null, null, '0'),
(91010502, '000000', 910105, 'sealed_program_form', '节目盖章表', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection"}', 2, 103, 1, sysdate(), null, null, '0'),
(91010503, '000000', 910105, 'review_opinion', '审查意见书', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection"}', 3, 103, 1, sysdate(), null, null, '0'),
(91010504, '000000', 910105, 'copyright_statement', '版权声明书', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection","condition":"isOriginal=是"}', 4, 103, 1, sysdate(), null, null, '0'),
(91010601, '000000', 910106, 'performance_video', '节目视频', 'mp4,mov', 1024, 1, 1, 1, '{"stage":"initial","mediaType":"video"}', 1, 103, 1, sysdate(), null, null, '0'),
(91010602, '000000', 910106, 'sealed_program_form', '节目盖章表', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection"}', 2, 103, 1, sysdate(), null, null, '0'),
(91010603, '000000', 910106, 'review_opinion', '审查意见书', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection"}', 3, 103, 1, sysdate(), null, null, '0'),
(91010604, '000000', 910106, 'copyright_statement', '版权声明书', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection","condition":"isOriginal=是"}', 4, 103, 1, sysdate(), null, null, '0'),
-- Artwork materials
(91011101, '000000', 910111, 'artwork_file', '作品文件', 'jpg,jpeg,png,pdf,mp4,mov', 1024, 1, 1, 1, '{"stage":"initial","manualCheckTips":["核验作品清晰度、内容和尺寸。"]}', 1, 103, 1, sysdate(), null, null, '0'),
(91011201, '000000', 910112, 'artwork_file', '作品文件', 'jpg,jpeg,png,pdf', 200, 1, 1, 1, '{"stage":"initial"}', 1, 103, 1, sysdate(), null, null, '0'),
(91011202, '000000', 910112, 'process_works', '过程性作品', 'jpg,jpeg,png,pdf', 50, 2, 5, 1, '{"stage":"initial","mediaType":"image","manualCheckTips":["核验过程性材料真实性。"]}', 2, 103, 1, sysdate(), null, null, '0'),
(91011301, '000000', 910113, 'artwork_file', '作品文件', 'jpg,jpeg,png,pdf', 200, 1, 1, 1, '{"stage":"initial"}', 1, 103, 1, sysdate(), null, null, '0'),
(91011302, '000000', 910113, 'process_works', '过程性作品', 'jpg,jpeg,png,pdf', 50, 2, 5, 1, '{"stage":"initial","mediaType":"image"}', 2, 103, 1, sysdate(), null, null, '0'),
(91011401, '000000', 910114, 'artwork_video', '影视作品', 'mp4,mov', 1024, 1, 1, 1, '{"stage":"initial","mediaType":"video","manualCheckTips":["按作品类别核验时长。"]}', 1, 103, 1, sysdate(), null, null, '0'),
(91011402, '000000', 910114, 'review_opinion', '审查意见书', 'pdf', 20, 0, 1, 0, '{"stage":"post_selection"}', 2, 103, 1, sysdate(), null, null, '0'),
(91011501, '000000', 910115, 'artwork_file', '书画作品文件', 'jpg,jpeg,png,pdf', 200, 1, 1, 1, '{"stage":"initial","manualCheckTips":["核验作品为高校校长书法或绘画作品，作者信息已在表单中填写。"]}', 1, 103, 1, sysdate(), null, null, '0'),
-- Workshop materials
(91012101, '000000', 910121, 'workshop_video', '工作坊视频', 'mp4,mpg,mpeg', 1024, 1, 1, 1, '{"stage":"initial","mediaType":"video","maxDurationSeconds":480}', 1, 103, 1, sysdate(), null, null, '0'),
(91012102, '000000', 910121, 'project_intro_doc', '项目介绍文档', 'doc,docx,pdf', 50, 1, 1, 1, '{"stage":"initial"}', 2, 103, 1, sysdate(), null, null, '0'),
-- Achievement materials
(91013101, '000000', 910131, 'body_doc', 'Word 正文', 'doc,docx', 50, 1, 1, 1, '{"stage":"initial"}', 1, 103, 1, sysdate(), null, null, '0'),
(91013201, '000000', 910132, 'body_doc', 'Word 正文', 'doc,docx', 50, 1, 1, 1, '{"stage":"initial"}', 1, 103, 1, sysdate(), null, null, '0'),
(91013202, '000000', 910132, 'case_video', '案例视频', 'mp4,mov', 1024, 0, 1, 0, '{"stage":"initial","mediaType":"video","maxDurationSeconds":300}', 2, 103, 1, sysdate(), null, null, '0'),
(91013203, '000000', 910132, 'case_images', '案例图片', 'jpg,jpeg,png', 50, 0, 5, 0, '{"stage":"initial","mediaType":"image"}', 3, 103, 1, sysdate(), null, null, '0');

update category_file_requirement r
join tmp_art_review_file_requirement_runtime t
  on t.category_id = r.category_id
 and t.file_type_code = r.file_type_code
set r.tenant_id = t.tenant_id,
    r.file_type_name = t.file_type_name,
    r.allowed_ext = t.allowed_ext,
    r.max_size_mb = t.max_size_mb,
    r.min_count = t.min_count,
    r.max_count = t.max_count,
    r.required = t.required,
    r.rule_json = t.rule_json,
    r.sort_order = t.sort_order,
    r.create_dept = t.create_dept,
    r.create_by = t.create_by,
    r.create_time = t.create_time,
    r.update_by = t.update_by,
    r.update_time = t.update_time,
    r.del_flag = t.del_flag
where r.category_id between 910101 and 910199;

insert into category_file_requirement
(id, tenant_id, category_id, file_type_code, file_type_name, allowed_ext, max_size_mb, min_count, max_count, required, rule_json, sort_order, create_dept, create_by, create_time, update_by, update_time, del_flag)
select t.id, t.tenant_id, t.category_id, t.file_type_code, t.file_type_name,
       t.allowed_ext, t.max_size_mb, t.min_count, t.max_count, t.required,
       t.rule_json, t.sort_order, t.create_dept, t.create_by, t.create_time,
       t.update_by, t.update_time, t.del_flag
from tmp_art_review_file_requirement_runtime t
left join category_file_requirement r_id on r_id.id = t.id
left join category_file_requirement r_code
  on r_code.category_id = t.category_id
 and r_code.file_type_code = t.file_type_code
where r_id.id is null
  and r_code.id is null;

set @art_review_file_req_has_tip_text = (
    select count(*)
    from information_schema.columns
    where table_schema = database()
      and table_name = 'category_file_requirement'
      and column_name = 'tip_text'
);
set @art_review_restore_file_req_tip_text = if(
    @art_review_file_req_has_tip_text > 0,
    'update category_file_requirement r join tmp_art_review_file_requirement_runtime t on t.category_id = r.category_id and t.file_type_code = r.file_type_code set r.tip_text = t.tip_text where r.category_id between 910101 and 910199',
    'select 1'
);
prepare art_review_restore_file_req_tip_text_stmt from @art_review_restore_file_req_tip_text;
execute art_review_restore_file_req_tip_text_stmt;
deallocate prepare art_review_restore_file_req_tip_text_stmt;

drop temporary table if exists tmp_art_review_field_schema_runtime;
drop temporary table if exists tmp_art_review_file_requirement_runtime;

-- --------------------------------------------------
-- Upload rule templates
-- The template_code values are referenced by activity_category.rule_json.
-- Applying a template in the admin UI overwrites fields/files/rules for
-- the corresponding category, while category_code remains the menu binding.
-- --------------------------------------------------

insert into upload_rule_template
(id, tenant_id, template_code, template_name, category_group, field_schema_json, file_requirement_json, rule_json, tip_text, check_mode, enabled, sort_order, remark, create_dept, create_by, create_time, del_flag)
values
(18301, '000000', 'tpl_performance_vocal', '声乐节目上传规则包', '艺术表演类',
 '[{"fieldKey":"programForm","fieldLabel":"节目形式","fieldType":"select","required":true,"optionsJson":"[\"合唱\",\"小合唱\",\"表演唱\",\"独唱\"]","sortOrder":1},{"fieldKey":"displayGroup","fieldLabel":"展演组别","fieldType":"select","required":true,"optionsJson":"[\"甲组\",\"乙组\"]","sortOrder":2},{"fieldKey":"isOriginal","fieldLabel":"是否原创","fieldType":"radio","required":true,"optionsJson":"[\"是\",\"否\"]","sortOrder":3},{"fieldKey":"applyOriginalAward","fieldLabel":"是否申报灼华奖","fieldType":"radio","required":true,"optionsJson":"[\"是\",\"否\"]","sortOrder":4},{"fieldKey":"durationText","fieldLabel":"节目时长","fieldType":"input","required":true,"sortOrder":5},{"fieldKey":"track1Name","fieldLabel":"曲目1名称","fieldType":"input","required":true,"sortOrder":6},{"fieldKey":"track1Author","fieldLabel":"曲目1词曲作者","fieldType":"input","required":true,"sortOrder":7},{"fieldKey":"track2Name","fieldLabel":"曲目2名称","fieldType":"input","required":false,"sortOrder":8},{"fieldKey":"track2Author","fieldLabel":"曲目2词曲作者","fieldType":"input","required":false,"sortOrder":9},{"fieldKey":"workDescription","fieldLabel":"作品简述","fieldType":"textarea","required":true,"validationJson":"{\"maxLength\":200}","sortOrder":10},{"fieldKey":"adviserNames","fieldLabel":"指导老师","fieldType":"input","required":false,"sortOrder":11}]',
 '[{"fileTypeCode":"performance_video","fileTypeName":"节目视频","allowedExt":"mp4,mov","maxSizeMb":1024,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"stage\":\"initial\",\"mediaType\":\"video\",\"minWidth\":1920,\"minHeight\":1080,\"fps\":25,\"fpsTolerance\":0.5,\"minBitrateMbps\":10,\"manualCheckTips\":[\"视频不得出现省份、学校、姓名、指导老师等信息。\"]}","sortOrder":1},{"fileTypeCode":"sealed_program_form","fileTypeName":"节目盖章表","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\"}","sortOrder":2},{"fileTypeCode":"review_opinion","fileTypeName":"审查意见书","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\"}","sortOrder":3},{"fileTypeCode":"copyright_statement","fileTypeName":"版权声明书","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\",\"condition\":\"applyOriginalAward=是\"}","sortOrder":4}]',
 '{"templateCode":"tpl_performance_vocal","validatorType":"performance","projectName":{"label":"节目名称","required":true},"maxTeacherCount":3,"maxDurationSecondsByProjectNature":{"合唱":480,"小合唱":360,"表演唱":360,"个人项目":300}}', '合唱可填两首曲目，小合唱/表演唱只填一首。', 'mixed', 1, 101, '指南固定模板：声乐', 103, 1, sysdate(), '0'),
(18302, '000000', 'tpl_performance_instrumental', '器乐节目上传规则包', '艺术表演类',
 '[{"fieldKey":"programForm","fieldLabel":"节目形式","fieldType":"select","required":true,"optionsJson":"[\"合奏\",\"小合奏\",\"独奏\"]","sortOrder":1},{"fieldKey":"displayGroup","fieldLabel":"展演组别","fieldType":"select","required":true,"optionsJson":"[\"甲组\",\"乙组\"]","sortOrder":2},{"fieldKey":"isOriginal","fieldLabel":"是否原创","fieldType":"radio","required":true,"optionsJson":"[\"是\",\"否\"]","sortOrder":3},{"fieldKey":"durationText","fieldLabel":"节目时长","fieldType":"input","required":true,"sortOrder":4},{"fieldKey":"workDescription","fieldLabel":"作品简述","fieldType":"textarea","required":true,"validationJson":"{\"maxLength\":200}","sortOrder":5},{"fieldKey":"adviserNames","fieldLabel":"指导老师","fieldType":"input","required":false,"sortOrder":6}]',
 '[{"fileTypeCode":"performance_video","fileTypeName":"节目视频","allowedExt":"mp4,mov","maxSizeMb":1024,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"stage\":\"initial\",\"mediaType\":\"video\",\"manualCheckTips\":[\"视频不得出现省份、学校、姓名、指导老师等信息。\"]}","sortOrder":1},{"fileTypeCode":"sealed_program_form","fileTypeName":"节目盖章表","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\"}","sortOrder":2},{"fileTypeCode":"review_opinion","fileTypeName":"审查意见书","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\"}","sortOrder":3},{"fileTypeCode":"copyright_statement","fileTypeName":"版权声明书","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\",\"condition\":\"isOriginal=是\"}","sortOrder":4}]',
 '{"templateCode":"tpl_performance_instrumental","validatorType":"performance","projectName":{"label":"节目名称","required":true},"maxTeacherCount":3}', '伴奏、指挥等身份请在人员备注中注明。', 'mixed', 1, 102, '指南固定模板：器乐', 103, 1, sysdate(), '0'),
(18303, '000000', 'tpl_performance_dance', '舞蹈节目上传规则包', '艺术表演类',
 '[{"fieldKey":"programForm","fieldLabel":"节目形式","fieldType":"select","required":true,"optionsJson":"[\"群舞\",\"双人舞\",\"三人舞\",\"独舞\"]","sortOrder":1},{"fieldKey":"displayGroup","fieldLabel":"展演组别","fieldType":"select","required":true,"optionsJson":"[\"甲组\",\"乙组\"]","sortOrder":2},{"fieldKey":"isOriginal","fieldLabel":"是否原创","fieldType":"radio","required":true,"optionsJson":"[\"是\",\"否\"]","sortOrder":3},{"fieldKey":"durationText","fieldLabel":"节目时长","fieldType":"input","required":true,"sortOrder":4},{"fieldKey":"workDescription","fieldLabel":"作品简述","fieldType":"textarea","required":true,"validationJson":"{\"maxLength\":200}","sortOrder":5},{"fieldKey":"adviserNames","fieldLabel":"指导老师","fieldType":"input","required":false,"sortOrder":6}]',
 '[{"fileTypeCode":"performance_video","fileTypeName":"节目视频","allowedExt":"mp4,mov","maxSizeMb":1024,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"stage\":\"initial\",\"mediaType\":\"video\"}","sortOrder":1},{"fileTypeCode":"sealed_program_form","fileTypeName":"节目盖章表","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\"}","sortOrder":2},{"fileTypeCode":"review_opinion","fileTypeName":"审查意见书","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\"}","sortOrder":3},{"fileTypeCode":"copyright_statement","fileTypeName":"版权声明书","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\",\"condition\":\"isOriginal=是\"}","sortOrder":4}]',
 '{"templateCode":"tpl_performance_dance","validatorType":"performance","projectName":{"label":"节目名称","required":true},"maxTeacherCount":3}', '填写节目时长、作品简述和指导老师，上传节目视频。', 'mixed', 1, 103, '指南固定模板：舞蹈', 103, 1, sysdate(), '0'),
(18304, '000000', 'tpl_performance_drama', '戏剧节目上传规则包', '艺术表演类',
 '[{"fieldKey":"programForm","fieldLabel":"节目形式","fieldType":"select","required":true,"optionsJson":"[\"戏剧\",\"小品\",\"音乐剧\"]","sortOrder":1},{"fieldKey":"displayGroup","fieldLabel":"展演组别","fieldType":"select","required":true,"optionsJson":"[\"甲组\",\"乙组\"]","sortOrder":2},{"fieldKey":"isOriginal","fieldLabel":"是否原创","fieldType":"radio","required":true,"optionsJson":"[\"是\",\"否\"]","sortOrder":3},{"fieldKey":"durationText","fieldLabel":"节目时长","fieldType":"input","required":true,"sortOrder":4},{"fieldKey":"workDescription","fieldLabel":"作品简述","fieldType":"textarea","required":true,"validationJson":"{\"maxLength\":200}","sortOrder":5},{"fieldKey":"adviserNames","fieldLabel":"指导老师","fieldType":"input","required":false,"sortOrder":6}]',
 '[{"fileTypeCode":"performance_video","fileTypeName":"节目视频","allowedExt":"mp4,mov","maxSizeMb":1024,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"stage\":\"initial\",\"mediaType\":\"video\"}","sortOrder":1},{"fileTypeCode":"sealed_program_form","fileTypeName":"节目盖章表","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\"}","sortOrder":2},{"fileTypeCode":"review_opinion","fileTypeName":"审查意见书","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\"}","sortOrder":3},{"fileTypeCode":"copyright_statement","fileTypeName":"版权声明书","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\",\"condition\":\"isOriginal=是\"}","sortOrder":4}]',
 '{"templateCode":"tpl_performance_drama","validatorType":"performance","projectName":{"label":"节目名称","required":true},"maxTeacherCount":3}', '填写节目时长、作品简述和指导老师，上传节目视频。', 'mixed', 1, 104, '指南固定模板：戏剧', 103, 1, sysdate(), '0'),
(18305, '000000', 'tpl_performance_recitation', '朗诵节目上传规则包', '艺术表演类',
 '[{"fieldKey":"programForm","fieldLabel":"节目形式","fieldType":"select","required":true,"optionsJson":"[\"朗诵\",\"集体朗诵\",\"个人朗诵\"]","sortOrder":1},{"fieldKey":"displayGroup","fieldLabel":"展演组别","fieldType":"select","required":true,"optionsJson":"[\"甲组\",\"乙组\"]","sortOrder":2},{"fieldKey":"isOriginal","fieldLabel":"是否原创","fieldType":"radio","required":true,"optionsJson":"[\"是\",\"否\"]","sortOrder":3},{"fieldKey":"durationText","fieldLabel":"节目时长","fieldType":"input","required":true,"sortOrder":4},{"fieldKey":"workDescription","fieldLabel":"作品简述","fieldType":"textarea","required":true,"validationJson":"{\"maxLength\":200}","sortOrder":5},{"fieldKey":"adviserNames","fieldLabel":"指导老师","fieldType":"input","required":false,"sortOrder":6}]',
 '[{"fileTypeCode":"performance_video","fileTypeName":"节目视频","allowedExt":"mp4,mov","maxSizeMb":1024,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"stage\":\"initial\",\"mediaType\":\"video\"}","sortOrder":1},{"fileTypeCode":"sealed_program_form","fileTypeName":"节目盖章表","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\"}","sortOrder":2},{"fileTypeCode":"review_opinion","fileTypeName":"审查意见书","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\"}","sortOrder":3},{"fileTypeCode":"copyright_statement","fileTypeName":"版权声明书","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\",\"condition\":\"isOriginal=是\"}","sortOrder":4}]',
 '{"templateCode":"tpl_performance_recitation","validatorType":"performance","projectName":{"label":"节目名称","required":true},"maxTeacherCount":3}', '填写节目时长、作品简述和指导老师，上传节目视频。', 'mixed', 1, 105, '指南固定模板：朗诵', 103, 1, sysdate(), '0'),
(18306, '000000', 'tpl_performance_personal', '个人项目上传规则包', '艺术表演类',
 '[{"fieldKey":"programForm","fieldLabel":"节目形式","fieldType":"select","required":true,"optionsJson":"[\"独唱\",\"独奏\",\"个人朗诵\",\"其他\"]","sortOrder":1},{"fieldKey":"displayGroup","fieldLabel":"展演组别","fieldType":"select","required":true,"optionsJson":"[\"甲组\",\"乙组\"]","sortOrder":2},{"fieldKey":"isOriginal","fieldLabel":"是否原创","fieldType":"radio","required":true,"optionsJson":"[\"是\",\"否\"]","sortOrder":3},{"fieldKey":"durationText","fieldLabel":"节目时长","fieldType":"input","required":true,"sortOrder":4},{"fieldKey":"workDescription","fieldLabel":"作品简述","fieldType":"textarea","required":true,"validationJson":"{\"maxLength\":200}","sortOrder":5},{"fieldKey":"adviserNames","fieldLabel":"指导老师","fieldType":"input","required":false,"sortOrder":6}]',
 '[{"fileTypeCode":"performance_video","fileTypeName":"节目视频","allowedExt":"mp4,mov","maxSizeMb":1024,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"stage\":\"initial\",\"mediaType\":\"video\"}","sortOrder":1},{"fileTypeCode":"sealed_program_form","fileTypeName":"节目盖章表","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\"}","sortOrder":2},{"fileTypeCode":"review_opinion","fileTypeName":"审查意见书","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\"}","sortOrder":3},{"fileTypeCode":"copyright_statement","fileTypeName":"版权声明书","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\",\"condition\":\"isOriginal=是\"}","sortOrder":4}]',
 '{"templateCode":"tpl_performance_personal","validatorType":"performance","projectName":{"label":"节目名称","required":true},"maxTeacherCount":1}', '备注中注明演唱、演奏、伴奏等身份。', 'mixed', 1, 106, '指南固定模板：个人项目', 103, 1, sysdate(), '0'),
(18311, '000000', 'tpl_artwork_fine_art', '美术类作品上传规则包', '艺术作品类',
 '[{"fieldKey":"workType","fieldLabel":"作品类别","fieldType":"select","required":true,"optionsJson":"[\"绘画\",\"书法\",\"篆刻\",\"摄影\"]","sortOrder":1},{"fieldKey":"displayGroup","fieldLabel":"组别","fieldType":"select","required":true,"optionsJson":"[\"甲组\",\"乙组\"]","sortOrder":2},{"fieldKey":"sizeText","fieldLabel":"尺寸","fieldType":"input","required":false,"sortOrder":3},{"fieldKey":"createdAtText","fieldLabel":"创作时间","fieldType":"input","required":false,"sortOrder":4},{"fieldKey":"creationDescription","fieldLabel":"创作说明","fieldType":"textarea","required":true,"validationJson":"{\"maxLength\":400}","sortOrder":5},{"fieldKey":"adviserNames","fieldLabel":"指导教师","fieldType":"input","required":false,"sortOrder":6}]',
 '[{"fileTypeCode":"artwork_file","fileTypeName":"作品文件","allowedExt":"jpg,jpeg,png,pdf,mp4,mov","maxSizeMb":1024,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"stage\":\"initial\",\"manualCheckTips\":[\"核验作品清晰度、内容和尺寸。\"]}","sortOrder":1}]',
 '{"templateCode":"tpl_artwork_fine_art","validatorType":"artwork","projectName":{"label":"作品名称","required":true},"maxAuthorCount":3,"teacherCount":1}', '填写类别、组别、尺寸、创作时间、创作说明并上传作品。', 'mixed', 1, 201, '指南固定模板：美术类', 103, 1, sysdate(), '0'),
(18312, '000000', 'tpl_artwork_grand_design', '大艺展设计类上传规则包', '艺术作品类',
 '[{"fieldKey":"workType","fieldLabel":"作品类别","fieldType":"select","required":true,"optionsJson":"[\"视觉传达\",\"环境设计\",\"产品设计\",\"数字媒体设计\",\"其他\"]","sortOrder":1},{"fieldKey":"sizeText","fieldLabel":"尺寸","fieldType":"input","required":false,"sortOrder":2},{"fieldKey":"createdAtText","fieldLabel":"创作时间","fieldType":"input","required":false,"sortOrder":3},{"fieldKey":"creationDescription","fieldLabel":"创作说明","fieldType":"textarea","required":true,"validationJson":"{\"maxLength\":400}","sortOrder":4},{"fieldKey":"adviserNames","fieldLabel":"指导教师","fieldType":"input","required":false,"sortOrder":5}]',
 '[{"fileTypeCode":"artwork_file","fileTypeName":"作品文件","allowedExt":"jpg,jpeg,png,pdf","maxSizeMb":200,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"stage\":\"initial\"}","sortOrder":1},{"fileTypeCode":"process_works","fileTypeName":"过程性作品","allowedExt":"jpg,jpeg,png,pdf","maxSizeMb":50,"minCount":2,"maxCount":5,"required":true,"ruleJson":"{\"stage\":\"initial\",\"mediaType\":\"image\"}","sortOrder":2}]',
 '{"templateCode":"tpl_artwork_grand_design","validatorType":"artwork","projectName":{"label":"作品名称","required":true},"maxAuthorCount":3,"teacherCount":1}', '不分组别，额外上传 2-5 张过程性作品。', 'mixed', 1, 202, '指南固定模板：大艺展设计类', 103, 1, sysdate(), '0'),
(18313, '000000', 'tpl_artwork_design', '设计展上传规则包', '艺术作品类',
 '[{"fieldKey":"workType","fieldLabel":"作品类别","fieldType":"select","required":true,"optionsJson":"[\"视觉传达\",\"环境设计\",\"产品设计\",\"数字媒体设计\",\"其他\"]","sortOrder":1},{"fieldKey":"sizeText","fieldLabel":"尺寸","fieldType":"input","required":false,"sortOrder":2},{"fieldKey":"createdAtText","fieldLabel":"创作时间","fieldType":"input","required":false,"sortOrder":3},{"fieldKey":"authorNames","fieldLabel":"作者信息","fieldType":"textarea","required":true,"sortOrder":4},{"fieldKey":"creationDescription","fieldLabel":"创作说明","fieldType":"textarea","required":true,"validationJson":"{\"maxLength\":400}","sortOrder":5}]',
 '[{"fileTypeCode":"artwork_file","fileTypeName":"作品文件","allowedExt":"jpg,jpeg,png,pdf","maxSizeMb":200,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"stage\":\"initial\"}","sortOrder":1},{"fileTypeCode":"process_works","fileTypeName":"过程性作品","allowedExt":"jpg,jpeg,png,pdf","maxSizeMb":50,"minCount":2,"maxCount":5,"required":true,"ruleJson":"{\"stage\":\"initial\",\"mediaType\":\"image\"}","sortOrder":2}]',
 '{"templateCode":"tpl_artwork_design","validatorType":"artwork","projectName":{"label":"作品名称","required":true},"maxAuthorCount":3,"teacherCount":1}', '作者信息直接在表单中填写，并额外上传 2-5 张过程性作品。', 'mixed', 1, 203, '指南固定模板：设计展', 103, 1, sysdate(), '0'),
(18314, '000000', 'tpl_artwork_film', '影视类上传规则包', '艺术作品类',
 '[{"fieldKey":"workType","fieldLabel":"作品类别","fieldType":"select","required":true,"optionsJson":"[\"短片\",\"动画\",\"纪录片\"]","sortOrder":1},{"fieldKey":"sizeText","fieldLabel":"尺寸/规格","fieldType":"input","required":false,"sortOrder":2},{"fieldKey":"createdAtText","fieldLabel":"创作时间","fieldType":"input","required":false,"sortOrder":3},{"fieldKey":"creationDescription","fieldLabel":"创作说明","fieldType":"textarea","required":true,"validationJson":"{\"maxLength\":400}","sortOrder":4},{"fieldKey":"adviserNames","fieldLabel":"指导教师","fieldType":"input","required":false,"sortOrder":5}]',
 '[{"fileTypeCode":"artwork_video","fileTypeName":"影视作品","allowedExt":"mp4,mov","maxSizeMb":1024,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"stage\":\"initial\",\"mediaType\":\"video\",\"manualCheckTips\":[\"按作品类别核验时长。\"]}","sortOrder":1},{"fileTypeCode":"review_opinion","fileTypeName":"审查意见书","allowedExt":"pdf","maxSizeMb":20,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"post_selection\"}","sortOrder":2}]',
 '{"templateCode":"tpl_artwork_film","validatorType":"artwork","projectName":{"label":"作品名称","required":true},"filmMaxAuthorCount":6,"filmMaxTeacherCount":3,"filmVideoRules":{"短片":{"maxDurationSeconds":900},"动画":{"maxDurationSeconds":900},"纪录片":{"maxDurationSeconds":1800}}}', '按影视作品类别校验时长。', 'mixed', 1, 204, '指南固定模板：影视类', 103, 1, sysdate(), '0'),
(18315, '000000', 'tpl_artwork_principal_calligraphy_painting', '高校校长书画作品上传规则包', '高校校长书画作品',
 '[{"fieldKey":"workType","fieldLabel":"作品类别","fieldType":"select","required":true,"optionsJson":"[\"书法\",\"绘画\"]","sortOrder":1},{"fieldKey":"sizeText","fieldLabel":"尺寸","fieldType":"input","required":false,"sortOrder":2},{"fieldKey":"createdAtText","fieldLabel":"创作时间","fieldType":"input","required":false,"sortOrder":3},{"fieldKey":"authorNames","fieldLabel":"校长作者信息","fieldType":"textarea","required":true,"sortOrder":4},{"fieldKey":"creationDescription","fieldLabel":"创作说明","fieldType":"textarea","required":true,"validationJson":"{\"maxLength\":400}","sortOrder":5}]',
 '[{"fileTypeCode":"artwork_file","fileTypeName":"书画作品文件","allowedExt":"jpg,jpeg,png,pdf","maxSizeMb":200,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"stage\":\"initial\",\"manualCheckTips\":[\"核验作品为高校校长书法或绘画作品，作者信息已在表单中填写。\"]}","sortOrder":1}]',
 '{"templateCode":"tpl_artwork_principal_calligraphy_painting","validatorType":"artwork","projectName":{"label":"作品名称","required":true},"maxAuthorCount":1,"teacherCount":0}', '高校校长书画作品作者信息直接在表单中填写，作品类别限定书法或绘画。', 'mixed', 1, 205, '指南固定模板：高校校长书画作品', 103, 1, sysdate(), '0'),
(18321, '000000', 'tpl_workshop_practice', '艺术实践工作坊上传规则包', '艺术实践工作坊',
 '[{"fieldKey":"projectIntro","fieldLabel":"项目介绍","fieldType":"textarea","required":true,"sortOrder":1},{"fieldKey":"designIdea","fieldLabel":"设计思路","fieldType":"textarea","required":true,"sortOrder":2},{"fieldKey":"featureDescription","fieldLabel":"特色说明","fieldType":"textarea","required":true,"sortOrder":3},{"fieldKey":"exhibitionDesignPlan","fieldLabel":"展示设计方案","fieldType":"textarea","required":true,"sortOrder":4},{"fieldKey":"noPreviousAwardCommitment","fieldLabel":"未获往届奖项承诺","fieldType":"checkbox","required":true,"optionsJson":"[\"确认未获往届同类奖项\"]","sortOrder":5}]',
 '[{"fileTypeCode":"workshop_video","fileTypeName":"工作坊视频","allowedExt":"mp4,mpg,mpeg","maxSizeMb":1024,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"stage\":\"initial\",\"mediaType\":\"video\",\"maxDurationSeconds\":480}","sortOrder":1},{"fileTypeCode":"project_intro_doc","fileTypeName":"项目介绍文档","allowedExt":"doc,docx,pdf","maxSizeMb":50,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"stage\":\"initial\"}","sortOrder":2}]',
 '{"templateCode":"tpl_workshop_practice","validatorType":"workshop","projectName":{"label":"工作坊名称","required":true},"minTeacherCount":1,"maxTeacherCount":3,"minStudentCount":7,"maxStudentCount":9,"maxTotalMemberCount":12,"maxVideoDurationSeconds":480}', '上传工作坊视频和项目介绍文档；教师 1-3 人，学生 7-9 人。', 'mixed', 1, 301, '指南固定模板：艺术实践工作坊', 103, 1, sysdate(), '0'),
(18331, '000000', 'tpl_achievement_paper', '学术论文上传规则包', '美育改革创新优秀成果',
 '[{"fieldKey":"topicCategory","fieldLabel":"选题类别","fieldType":"radio","required":true,"optionsJson":"[\"01 学校美育的内涵与价值功能\",\"02 中华美育精神的内在意蕴与时代价值\",\"03 学校美育浸润行动的实践路径\",\"04 高校教师美育素养提升路径\",\"05 高校学科美育与创新型人才培养\",\"06 “五育并举”视域下高校美育育人模式创新\",\"07 新时代高校美育评价改革\",\"08 中华优秀传统文化融入高校美育的策略与路径\",\"09 高校数字美育新生态构建\",\"10 艺术师范教育改革发展\",\"11 高校艺术教育中外比较\",\"12 高校美育赋能经济社会发展实施路径\"]","sortOrder":1},{"fieldKey":"paperAbstract","fieldLabel":"简介/摘要","fieldType":"textarea","required":true,"validationJson":"{\"minLength\":250,\"maxLength\":350}","sortOrder":2},{"fieldKey":"keywords","fieldLabel":"关键词","fieldType":"input","required":true,"sortOrder":3},{"fieldKey":"bodyText","fieldLabel":"正文文本","fieldType":"textarea","required":true,"validationJson":"{\"minLength\":5000}","sortOrder":4},{"fieldKey":"references","fieldLabel":"参考文献","fieldType":"textarea","required":true,"sortOrder":5},{"fieldKey":"unpublishedCommitment","fieldLabel":"未公开发表承诺","fieldType":"checkbox","required":true,"optionsJson":"[\"确认未公开发表\"]","sortOrder":6}]',
 '[{"fileTypeCode":"body_doc","fileTypeName":"Word 正文","allowedExt":"doc,docx","maxSizeMb":50,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"stage\":\"initial\"}","sortOrder":1}]',
 '{"templateCode":"tpl_achievement_paper","validatorType":"aesthetic_achievement","projectName":{"label":"论文标题","required":true},"paperMaxAuthorCount":2}', '填写标题、简介、作者信息并上传 Word 正文。', 'mixed', 1, 401, '指南固定模板：学术论文', 103, 1, sysdate(), '0'),
(18332, '000000', 'tpl_achievement_case', '教学改革案例上传规则包', '美育改革创新优秀成果',
 '[{"fieldKey":"topicCategory","fieldLabel":"选题类别","fieldType":"radio","required":true,"optionsJson":"[\"01 高校公共艺术教育教学体系建设\",\"02 高校美育课程与教材建设\",\"03 高校学生艺术社团及实践工作坊建设\",\"04 高校跨学科美育实践\",\"05 高校美育专门机构和教师队伍建设\",\"06 高校美育评价制度建设\",\"07 美育名师工作室建设\",\"08 中华优秀传统文化艺术传承基地建设\",\"09 艺术展演育人实践\",\"10 艺术师范教育基本功展示引领人才培养\",\"11 高校助力乡村学校美育提质发展\",\"12 高校美育资源与社会艺术资源共建共享\"]","sortOrder":1},{"fieldKey":"unitName","fieldLabel":"申报单位","fieldType":"input","required":true,"sortOrder":2},{"fieldKey":"caseText","fieldLabel":"案例正文","fieldType":"textarea","required":true,"validationJson":"{\"maxLength\":5000}","sortOrder":3},{"fieldKey":"completerNames","fieldLabel":"完成人信息","fieldType":"textarea","required":true,"sortOrder":4}]',
 '[{"fileTypeCode":"body_doc","fileTypeName":"Word 正文","allowedExt":"doc,docx","maxSizeMb":50,"minCount":1,"maxCount":1,"required":true,"ruleJson":"{\"stage\":\"initial\"}","sortOrder":1},{"fileTypeCode":"case_video","fileTypeName":"案例视频","allowedExt":"mp4,mov","maxSizeMb":1024,"minCount":0,"maxCount":1,"required":false,"ruleJson":"{\"stage\":\"initial\",\"mediaType\":\"video\",\"maxDurationSeconds\":300}","sortOrder":2},{"fileTypeCode":"case_images","fileTypeName":"案例图片","allowedExt":"jpg,jpeg,png","maxSizeMb":50,"minCount":0,"maxCount":5,"required":false,"ruleJson":"{\"stage\":\"initial\",\"mediaType\":\"image\"}","sortOrder":3}]',
 '{"templateCode":"tpl_achievement_case","validatorType":"aesthetic_achievement","projectName":{"label":"案例标题","required":true},"caseMaxCompleterCount":3,"caseMaxVideoCount":1,"caseMaxImageCount":5}', '填写标题、简介、完成人信息，上传 Word 正文，可选视频和图片。', 'mixed', 1, 402, '指南固定模板：教学改革案例', 103, 1, sysdate(), '0')
on duplicate key update
    template_name = values(template_name),
    category_group = values(category_group),
    field_schema_json = values(field_schema_json),
    file_requirement_json = values(file_requirement_json),
    rule_json = values(rule_json),
    tip_text = values(tip_text),
    check_mode = values(check_mode),
    enabled = values(enabled),
    sort_order = values(sort_order),
    remark = values(remark),
    update_time = sysdate();

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
