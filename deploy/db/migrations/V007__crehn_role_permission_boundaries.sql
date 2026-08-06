-- V007: freeze participant/school write ownership and score-summary least privilege.
-- ProgramMigration. Execute only after role-menu snapshot, backup and explicit SQL authorization.

-- Schools keep project read access plus the dedicated school review/final-submit workflow,
-- but no longer receive participant-owned draft, upload or single-project submission actions.
delete rm
from sys_role_menu rm
join sys_role r on r.role_id = rm.role_id
join sys_menu m on m.menu_id = rm.menu_id
where r.role_key = 'crehn_school'
  and m.perms in (
      'crehn:project:add',
      'crehn:project:submit',
      'crehn:project:upload',
      'crehn:project:remove'
  );

-- Remove result-generation/publication and other write capabilities from the score-summary role.
delete rm
from sys_role_menu rm
join sys_role r on r.role_id = rm.role_id
join sys_menu m on m.menu_id = rm.menu_id
where r.role_key = 'crehn_score_summary'
  and (
      m.path = 'result'
      or m.perms like 'crehn:result:%'
      or m.perms in (
          'crehn:reviewScoreSummary:config',
          'crehn:review:score',
          'crehn:reviewSheet:withdraw'
      )
      or m.perms like 'crehn:reviewSheet:signatureManage%'
  );

-- The default score-summary role can read summary/detail/signed-sheet data and export summaries.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on (
    (m.parent_id = 0 and m.path = 'crehn')
    or m.path in ('review-management', 'score-summary', 'signed-sheets')
    or m.perms in (
        'crehn:reviewScoreSummary:list',
        'crehn:reviewScoreSummary:detail',
        'crehn:reviewScoreSummary:export',
        'crehn:reviewSheet:manage'
    )
)
where r.role_key = 'crehn_score_summary';

-- Readback diagnostics. Any returned row is a release blocker.
select r.tenant_id, r.role_key, m.path, m.perms
from sys_role r
join sys_role_menu rm on rm.role_id = r.role_id
join sys_menu m on m.menu_id = rm.menu_id
where r.role_key = 'crehn_school'
  and m.perms in ('crehn:project:add', 'crehn:project:submit', 'crehn:project:upload', 'crehn:project:remove');

select r.tenant_id, r.role_key, m.path, m.perms
from sys_role r
join sys_role_menu rm on rm.role_id = r.role_id
join sys_menu m on m.menu_id = rm.menu_id
where r.role_key = 'crehn_score_summary'
  and (
      m.path = 'result'
      or m.perms like 'crehn:result:%'
      or m.perms in ('crehn:reviewScoreSummary:config', 'crehn:review:score', 'crehn:reviewSheet:withdraw')
      or m.perms like 'crehn:reviewSheet:signatureManage%'
  );
