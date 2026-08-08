-- V008 rollback is intentionally non-destructive.
-- sys_user.user_type remains varchar(32); shrinking it could truncate participant values.
-- Use application rollback and a separately reviewed forward-compatible correction if required.
select 'V008 rollback is intentionally non-destructive; sys_user.user_type remains varchar(32)' as rollback_notice;
