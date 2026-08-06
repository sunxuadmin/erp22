-- Security-regressing rollback to the V004 default role grants.
-- Use only with an execution-time role-menu snapshot and explicit SQL authorization.

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.perms in (
    'crehn:project:add',
    'crehn:project:submit',
    'crehn:project:upload'
)
where r.role_key = 'crehn_school';

delete rm
from sys_role_menu rm
join sys_role r on r.role_id = rm.role_id
join sys_menu m on m.menu_id = rm.menu_id
where r.role_key = 'crehn_score_summary'
  and m.perms in (
      'crehn:reviewScoreSummary:detail',
      'crehn:reviewScoreSummary:export',
      'crehn:reviewSheet:manage'
  );

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on (
    (m.parent_id = 0 and m.path = 'crehn')
    or m.path in ('result', 'score-summary')
    or m.perms like 'crehn:result:%'
)
where r.role_key = 'crehn_score_summary';
