-- M71: auditor accounts can review uploads across all schools and view/export upload summaries.
-- Keep this idempotent so it is safe to rerun during deployment.

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (
    17426, -- audit all scope
    17940, -- review result page
    17941, -- review result query
    17947, -- export
    17950  -- upload summary
)
where r.role_key = 'crehn_auditor';
