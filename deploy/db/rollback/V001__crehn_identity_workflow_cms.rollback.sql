-- Destructive rollback is intentionally not automatic.
-- Before any approved rollback:
-- 1. export participant, project snapshot, school batch and CMS tables;
-- 2. verify no published result, signed sheet or portal release references them;
-- 3. stop application writes;
-- 4. execute a separately reviewed environment-specific rollback.
select 'MANUAL_ROLLBACK_REQUIRED' as rollback_status;
