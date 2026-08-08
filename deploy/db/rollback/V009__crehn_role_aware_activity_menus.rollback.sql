-- V009 rollback is intentionally non-destructive and must not re-grant legacy fixed category menus automatically.
-- An automatic re-grant would restore category submission entries to administrators, schools, reviewers and other roles,
-- which conflicts with the confirmed role-aware menu boundary and FLOW-001.
--
-- Before V009 execution, export the affected sys_role_menu rows as the rollback snapshot. If a rollback is approved,
-- restore only the exact snapshot rows through a separately reviewed SQL change, then re-run V009 as the forward repair
-- once the cause is corrected. This file deliberately performs no persistent database write.

select 'V009 rollback requires the execution-time sys_role_menu snapshot; no legacy category grants were restored automatically.' as rollback_notice;
