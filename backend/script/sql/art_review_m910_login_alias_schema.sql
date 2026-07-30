-- --------------------------------------------------
-- M9.10 required schema: school account login alias.
-- RequiredSchemaSql: additive and safe to rerun before backend publication.
-- --------------------------------------------------

alter table sys_user
    add column if not exists login_alias varchar(64) default null comment 'English login alias' after user_name;

create unique index if not exists uk_sys_user_login_alias on sys_user(login_alias);
