-- --------------------------------------------------
-- M10.2 idempotent metadata backfill for the legacy shared score-sheet template.
-- Template title, columns, footer text, review assignments, and scores are unchanged.
-- --------------------------------------------------

start transaction;

update review_score_sheet_template
set template_name = '默认评审打分表'
where template_key = 'review_score_sheet_default'
  and del_flag = '0'
  and (template_name is null or trim(template_name) = '');

update review_score_sheet_template legacy_template
join (
    select tenant_id
    from review_score_sheet_template
    where del_flag = '0'
    group by tenant_id
    having sum(case when default_flag = '1' then 1 else 0 end) = 0
) missing_default on missing_default.tenant_id = legacy_template.tenant_id
set legacy_template.default_flag = '1'
where legacy_template.template_key = 'review_score_sheet_default'
  and legacy_template.del_flag = '0';

commit;
