-- --------------------------------------------------
-- Art Review M7.1 project member upload fields
-- Scope: member photo and student report attachment references
-- Safe to rerun on MariaDB 10.11+
-- --------------------------------------------------

alter table project_member
    add column if not exists photo_oss_id bigint(20) default null comment 'member photo oss id' after extra_json,
    add column if not exists photo_path varchar(500) default null comment 'member photo path' after photo_oss_id,
    add column if not exists student_report_oss_id bigint(20) default null comment 'student report oss id' after photo_path,
    add column if not exists student_report_path varchar(500) default null comment 'student report path' after student_report_oss_id;
