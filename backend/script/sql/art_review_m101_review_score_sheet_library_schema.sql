-- --------------------------------------------------
-- M10.1 additive score-sheet template library schema.
-- Existing title, columns, footer text, review assignments, and scores are unchanged.
-- --------------------------------------------------

alter table review_score_sheet_template
    add column if not exists template_name varchar(100) null;

alter table review_score_sheet_template
    add column if not exists footer_config_json text null;

alter table review_score_sheet_template
    add column if not exists default_flag char(1) not null default '0';

create index if not exists idx_review_score_sheet_template_default
    on review_score_sheet_template (tenant_id, default_flag, del_flag);
