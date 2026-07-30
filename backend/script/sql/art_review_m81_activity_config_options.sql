-- M81: strengthen DYZ activity configuration options.
-- - Keep the design exhibition as a disabled extension module.
-- - Keep principal calligraphy/painting category options including seal carving.
-- - Add default performance ratio quota rules to every non-deleted activity
--   that contains performance categories.
-- - Keep legacy quota/count rules intact; duplicate ratio defaults are skipped.
-- - Fix a few configuration consistency issues found in the runtime readback.

set @activity_id = 910001;
set @design_category_id = 910150;

-- Backfill untouched seed rows only. Operator edits set update_time.

update category_field_schema f
join activity_category c on c.id = f.category_id
set f.options_json = '["绘画","书法","摄影","篆刻"]',
    f.update_time = now()
where c.activity_id = @activity_id
  and c.category_code = 'artwork_principal'
  and f.field_key = 'workType'
  and c.del_flag = '0'
  and f.del_flag = '0'
  and f.update_time is null;

update category_file_requirement r
join activity_category c on c.id = r.category_id
set r.required = 1,
    r.min_count = greatest(coalesce(r.min_count, 0), 1),
    r.update_time = now()
where c.activity_id = @activity_id
  and c.category_code = 'achievement_paper'
  and r.file_type_code = 'body_doc'
  and c.del_flag = '0'
  and r.del_flag = '0'
  and r.update_time is null;

update category_field_schema f
join activity_category c on c.id = f.category_id
set f.sort_order = 3,
    f.update_time = now()
where c.activity_id = @activity_id
  and c.category_code = 'artwork_film'
  and f.field_key = 'work_duration'
  and c.del_flag = '0'
  and f.del_flag = '0'
  and f.update_time is null;

update category_field_schema f
join activity_category c on c.id = f.category_id
set f.sort_order = 3,
    f.update_time = now()
where c.activity_id = @activity_id
  and c.category_code = 'achievement_case'
  and f.field_key = 'adviser_teachers'
  and c.del_flag = '0'
  and f.del_flag = '0'
  and f.update_time is null;

insert into activity_category (
    id, tenant_id, activity_id, parent_id, category_code, category_name, quota_limit,
    category_group, rule_json, tip_text, check_mode, sort_order, enabled,
    create_dept, create_by, create_time, update_by, update_time, del_flag
)
select @design_category_id, '000000', @activity_id, 0, 'extension_design_exhibition', '设计展', null,
       '扩展设计展',
       '{"templateCode":"tpl_extension_design_exhibition","validatorType":"artwork","extensionModule":true,"projectName":{"label":"作品名称","required":true},"mediaTechnicalCheckMode":"manual","maxAuthorCount":3,"teacherCount":1}',
       '扩展模块，默认隐藏不用。若启用，请确认设计展来源、教师/学生组、作品类别和作品形式联动规则。',
       'mixed', 901, 0, 103, 1, now(), null, now(), '0'
where exists (select 1 from activity where id = @activity_id and del_flag = '0')
  and not exists (
    select 1 from activity_category
    where activity_id = @activity_id and category_code = 'extension_design_exhibition' and del_flag = '0'
  );

insert into category_field_schema (
    id, tenant_id, category_id, field_key, field_label, field_type, required,
    options_json, validation_json, sensitive_flag, sort_order,
    create_dept, create_by, create_time, update_by, update_time, del_flag
)
select 91015001, '000000', @design_category_id, 'designGroup', '组别', 'radio', 1,
       '["教师组","学生组"]', null, 0, 1, 103, 1, now(), null, null, '0'
where exists (select 1 from activity_category where id = @design_category_id and del_flag = '0')
  and not exists (select 1 from category_field_schema where id = 91015001);

insert into category_field_schema (
    id, tenant_id, category_id, field_key, field_label, field_type, required,
    options_json, validation_json, sensitive_flag, sort_order,
    create_dept, create_by, create_time, update_by, update_time, del_flag
)
select 91015002, '000000', @design_category_id, 'workType', '作品类别', 'select', 1,
       '["平面设计","景观设计","产品设计","室内设计","服装设计","建筑设计","现代手工艺","传媒"]',
       null, 0, 2, 103, 1, now(), null, null, '0'
where exists (select 1 from activity_category where id = @design_category_id and del_flag = '0')
  and not exists (select 1 from category_field_schema where id = 91015002);

insert into category_field_schema (
    id, tenant_id, category_id, field_key, field_label, field_type, required,
    options_json, validation_json, sensitive_flag, sort_order,
    create_dept, create_by, create_time, update_by, update_time, del_flag
)
select 91015003, '000000', @design_category_id, 'workForm', '作品形式', 'input', 1,
       null, null, 0, 3, 103, 1, now(), null, null, '0'
where exists (select 1 from activity_category where id = @design_category_id and del_flag = '0')
  and not exists (select 1 from category_field_schema where id = 91015003);

insert into category_field_schema (
    id, tenant_id, category_id, field_key, field_label, field_type, required,
    options_json, validation_json, sensitive_flag, sort_order,
    create_dept, create_by, create_time, update_by, update_time, del_flag
)
select 91015004, '000000', @design_category_id, 'sizeText', '尺寸/规格', 'input', 0,
       null, null, 0, 4, 103, 1, now(), null, null, '0'
where exists (select 1 from activity_category where id = @design_category_id and del_flag = '0')
  and not exists (select 1 from category_field_schema where id = 91015004);

insert into category_field_schema (
    id, tenant_id, category_id, field_key, field_label, field_type, required,
    options_json, validation_json, sensitive_flag, sort_order,
    create_dept, create_by, create_time, update_by, update_time, del_flag
)
select 91015005, '000000', @design_category_id, 'creationDescription', '创作说明', 'textarea', 1,
       null, '{"maxLength":400}', 0, 5, 103, 1, now(), null, null, '0'
where exists (select 1 from activity_category where id = @design_category_id and del_flag = '0')
  and not exists (select 1 from category_field_schema where id = 91015005);

insert into category_file_requirement (
    id, tenant_id, category_id, file_type_code, file_type_name, allowed_ext,
    max_size_mb, min_count, max_count, required, rule_json, tip_text, sort_order,
    create_dept, create_by, create_time, update_by, update_time, del_flag
)
select 91015051, '000000', @design_category_id, 'artwork_file', '作品文件',
       'jpg,jpeg,png,pdf', 200, 1, 1, 1,
       '{"stage":"initial","technicalCheckMode":"format_only","manualCheckTips":["扩展设计展启用前，请确认作品类别和形式来源。"]}',
       '上传设计作品文件。扩展模块默认隐藏，启用前请确认来源规则。', 1,
       103, 1, now(), null, null, '0'
where exists (select 1 from activity_category where id = @design_category_id and del_flag = '0')
  and not exists (select 1 from category_file_requirement where id = 91015051);

-- Legacy activity_quota_rule seeds retired.
-- Ratio and quota requirements are maintained in activity_report_rule by M94/M98 and the current activity rule UI.
