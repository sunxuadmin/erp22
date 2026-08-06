-- V006 is an additive, backward-compatible table migration.
-- Application rollback keeps portal_page_layout and its drafts; no destructive SQL is executed.
-- If the table must ever be retired, use a separately reviewed archive/purge migration after data export.
select 'V006 rollback is intentionally non-destructive; portal_page_layout is retained' as rollback_notice;
