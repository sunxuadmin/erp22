-- --------------------------------------------------
-- M9.7 schema-only migration for review score visibility.
-- This file belongs to RequiredSchemaSql and must be safe for pre-backend updates.
-- Runtime backfill, menu grants, and workbench layout remain in the optional runtime SQL.
-- --------------------------------------------------

alter table review_assignment
    add column if not exists score_visibility_policy varchar(32)
        default 'after_submit'
        comment 'peer score visibility: hidden/after_submit/always'
        after exclusive_mode;
