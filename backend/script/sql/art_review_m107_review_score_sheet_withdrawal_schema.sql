-- --------------------------------------------------
-- M10.7 additive signed score-sheet withdrawal audit fields.
-- The original score, template, signature, and signing snapshot remain immutable.
-- --------------------------------------------------

alter table review_score_sheet_signed
    add column if not exists withdrawn_at datetime null comment 'server withdrawal time';

alter table review_score_sheet_signed
    add column if not exists withdrawn_by_user_id bigint null comment 'withdrawal actor user id';

alter table review_score_sheet_signed
    add column if not exists withdrawn_by_name_snapshot varchar(100) null comment 'withdrawal actor display-name snapshot';

alter table review_score_sheet_signed
    add column if not exists withdrawn_by_role_snapshot varchar(100) null comment 'withdrawal actor role-key snapshot';

alter table review_score_sheet_signed
    add column if not exists withdrawal_mode varchar(32) null comment 'SELF or ADMIN_FORCE';

alter table review_score_sheet_signed
    add column if not exists withdraw_reason varchar(500) null comment 'required withdrawal reason';

create index if not exists idx_review_score_sheet_signed_manage
    on review_score_sheet_signed (tenant_id, activity_id, category_id, status, signed_at);
