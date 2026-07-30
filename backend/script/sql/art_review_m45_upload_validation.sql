-- --------------------------------------------------
-- Art Review M4.5 upload rules, media metadata, and submit validation
-- Safe to rerun on MariaDB 10.11+
-- --------------------------------------------------

alter table category_file_requirement
    add column if not exists rule_json longtext null comment 'extended upload rule json' after required,
    add column if not exists tip_text text null comment 'file requirement upload tip text' after rule_json;

alter table project_file
    add column if not exists preview_oss_id bigint(20) default null comment 'pdf preview oss id' after oss_id,
    add column if not exists preview_path varchar(500) default null comment 'pdf preview url/path' after storage_path,
    add column if not exists preview_ext varchar(32) default null comment 'preview file extension' after preview_path,
    add column if not exists preview_status varchar(32) default null comment 'not_required/converted/failed' after preview_ext,
    add column if not exists preview_message varchar(1000) default null comment 'preview convert message' after preview_status,
    add column if not exists preview_generated_at datetime default null comment 'preview generated time' after preview_message,
    add column if not exists preview_retry_count int default 0 comment 'preview convert retry count' after preview_generated_at,
    add column if not exists preview_started_at datetime default null comment 'preview convert start time' after preview_retry_count,
    add column if not exists media_type varchar(32) default null comment 'image/video/audio/document' after mime_type,
    add column if not exists duration_seconds decimal(12,3) default null comment 'media duration seconds' after media_type,
    add column if not exists width int default null comment 'media width' after duration_seconds,
    add column if not exists height int default null comment 'media height' after width,
    add column if not exists fps decimal(8,3) default null comment 'video fps' after height,
    add column if not exists bitrate bigint(20) default null comment 'video bitrate bps' after fps,
    add column if not exists dpi int default null comment 'image dpi' after bitrate,
    add column if not exists metadata_json longtext null comment 'raw media metadata json' after dpi,
    add column if not exists check_status varchar(32) default null comment 'passed/warning/failed' after metadata_json,
    add column if not exists check_message varchar(1000) default null comment 'technical check message' after check_status;

create index if not exists idx_project_file_preview_status on project_file (preview_status, status);

create table if not exists project_member
(
    id          bigint(20)   not null comment 'ID',
    tenant_id   varchar(20)  default '000000' comment 'tenant id',
    project_id  bigint(20)   not null comment 'project id',
    activity_id bigint(20)   not null comment 'activity id',
    school_id   bigint(20)   not null comment 'school id',
    member_type varchar(32)  not null comment 'student/author/teacher/completer',
    name        varchar(64)  default null comment 'member name',
    student_no  varchar(64)  default null comment 'student no',
    department  varchar(128) default null comment 'department',
    major       varchar(128) default null comment 'major',
    role_name   varchar(128) default null comment 'role name',
    extra_json  longtext     null comment 'dynamic member fields json',
    sort_order  int          default 0 comment 'sort order',
    status      varchar(32)  default 'active' comment 'active/inactive',
    create_dept bigint(20)   default null comment 'create dept',
    create_by   bigint(20)   default null comment 'create by',
    create_time datetime     default null comment 'create time',
    update_by   bigint(20)   default null comment 'update by',
    update_time datetime     default null comment 'update time',
    del_flag    char(1)      default '0' comment 'delete flag',
    primary key (id),
    key idx_project_member_project (project_id),
    key idx_project_member_activity_school (activity_id, school_id),
    key idx_project_member_person (activity_id, school_id, member_type, student_no, name)
) engine=innodb comment='project member';

alter table project_member
    add column if not exists extra_json longtext null comment 'dynamic member fields json' after role_name;

update upload_rule_template
set rule_json = '{"validatorType":"performance","fps":25,"fpsTolerance":0.5,"minBitrateMbps":10,"maxDurationSecondsByProjectNature":{"个人":300,"集体":480,"individual":300,"collective":480},"manualCheckTips":["视频中不得出现省份、学校、姓名、指导教师姓名。","系统仅校验承诺勾选，原创/授权真实性需人工复核。"],"rejectReasonTemplates":["视频技术参数不符合要求。","视频出现学校或人员身份信息。","原创或授权承诺材料不足。"]}',
    tip_text = '请上传 MP4/MOV 节目视频，系统将校验分辨率、帧率、码率、时长和大小；视频身份信息、原创授权由承诺与人工审核结合判断。',
    check_mode = 'mixed',
    update_time = sysdate()
where template_code = 'tpl_art_performance';

update upload_rule_template
set rule_json = '{"validatorType":"artwork","filmVideoRules":{"纪录短片":{"maxDurationSeconds":480,"minBitrateMbps":10},"剧情短片":{"maxDurationSeconds":480,"minBitrateMbps":10},"AIGC动画短片":{"maxDurationSeconds":300,"minBitrateMbps":10},"documentary":{"maxDurationSeconds":480,"minBitrateMbps":10},"feature":{"maxDurationSeconds":480,"minBitrateMbps":10},"aigc":{"maxDurationSeconds":300,"minBitrateMbps":10}},"manualCheckTips":["普通作品作者不超过3人，影视作者不超过6人。","AIGC动画短片需核验AI工具使用比例和创作流程。"],"rejectReasonTemplates":["作品图片格式、大小或DPI不符合要求。","作者或指导教师人数超限。","AIGC创作过程说明不完整。"]}',
    tip_text = '非影视作品请上传 JPG/JPEG 图片；影视类按短片类别上传视频，时长和码率可通过 filmVideoRules 调整。',
    check_mode = 'mixed',
    update_time = sysdate()
where template_code = 'tpl_art_work';
