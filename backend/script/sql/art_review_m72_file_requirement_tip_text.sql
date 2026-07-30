-- --------------------------------------------------
-- Art Review M7.2 file requirement upload tip text
-- Scope: configurable school-side upload hints per file requirement
-- Safe to rerun on MariaDB 10.11+
-- --------------------------------------------------

alter table category_file_requirement
    add column if not exists tip_text text null comment 'file requirement upload tip text' after rule_json;
