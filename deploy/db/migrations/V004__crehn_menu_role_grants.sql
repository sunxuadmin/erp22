-- CREHN-specific menu entries and stable role grants.
-- Role and menu primary keys are discovered or allocated at migration time.

set @crehn_root_menu_id = (
    select menu_id from sys_menu
    where parent_id = 0 and path = 'crehn'
    order by menu_id
    limit 1
);

set @next_menu_id = (select coalesce(max(menu_id), 0) from sys_menu);

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
 menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
select @next_menu_id := @next_menu_id + 1, '参赛者账号', @crehn_root_menu_id, 3, 'participant-account',
       'crehn/participant-account/index', '', 1, 0, 'C', '0', '0', 'crehn:participant:list',
       'peoples', null, 1, sysdate(), '名单导入、一次性激活码和激活状态'
where not exists (
    select 1 from sys_menu where parent_id = @crehn_root_menu_id and path = 'participant-account'
);

set @participant_menu_id = (
    select menu_id from sys_menu
    where parent_id = @crehn_root_menu_id and path = 'participant-account'
    order by menu_id limit 1
);

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
 menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
select @next_menu_id := @next_menu_id + 1, x.menu_name, @participant_menu_id, x.order_num, '', '', '',
       1, 0, 'F', '0', '0', x.perms, '#', null, 1, sysdate(), ''
from (
    select '参赛者查询' menu_name, 1 order_num, 'crehn:participant:list' perms
    union all select '名单与激活码导入', 2, 'crehn:participant:import'
    union all select '重新签发激活码', 3, 'crehn:participant:reissue'
) x
where not exists (select 1 from sys_menu m where m.perms = x.perms);

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
 menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
select @next_menu_id := @next_menu_id + 1, '学校审核终报', @crehn_root_menu_id, 4, 'school-submission',
       'crehn/school-submission/index', '', 1, 0, 'C', '0', '0', 'crehn:schoolSubmission:list',
       'clipboard', null, 1, sysdate(), '学校审核推荐并形成不可变最终提交批次'
where not exists (
    select 1 from sys_menu where parent_id = @crehn_root_menu_id and path = 'school-submission'
);

set @school_submission_menu_id = (
    select menu_id from sys_menu
    where parent_id = @crehn_root_menu_id and path = 'school-submission'
    order by menu_id limit 1
);

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
 menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
select @next_menu_id := @next_menu_id + 1, x.menu_name, @school_submission_menu_id, x.order_num, '', '', '',
       1, 0, 'F', '0', '0', x.perms, '#', null, 1, sysdate(), ''
from (
    select '学校待审查询' menu_name, 1 order_num, 'crehn:schoolSubmission:list' perms
    union all select '学校审核推荐', 2, 'crehn:schoolSubmission:review'
    union all select '学校最终提交', 3, 'crehn:schoolSubmission:submit'
) x
where not exists (select 1 from sys_menu m where m.perms = x.perms);

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
 menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
select @next_menu_id := @next_menu_id + 1, '独立门户管理', @crehn_root_menu_id, 90, 'cms',
       'crehn/cms/index', '', 1, 0, 'C', '0', '0', 'crehn:cms:article:list',
       'documentation', null, 1, sysdate(), '站点、栏目、文章、媒体和首页组件发布'
where not exists (
    select 1 from sys_menu where parent_id = @crehn_root_menu_id and path = 'cms'
);

set @cms_menu_id = (
    select menu_id from sys_menu
    where parent_id = @crehn_root_menu_id and path = 'cms'
    order by menu_id limit 1
);

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
 menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
select @next_menu_id := @next_menu_id + 1, x.menu_name, @cms_menu_id, x.order_num, '', '', '',
       1, 0, 'F', '0', '0', x.perms, '#', null, 1, sysdate(), ''
from (
    select '门户内容查询' menu_name, 1 order_num, 'crehn:cms:article:list' perms
    union all select '门户内容编辑', 2, 'crehn:cms:article:edit'
    union all select '门户内容送审', 3, 'crehn:cms:article:review'
    union all select '门户内容发布', 4, 'crehn:cms:article:publish'
    union all select '门户站点查询', 5, 'crehn:cms:site:list'
    union all select '门户站点编辑', 6, 'crehn:cms:site:edit'
    union all select '门户栏目查询', 7, 'crehn:cms:channel:list'
    union all select '门户栏目编辑', 8, 'crehn:cms:channel:edit'
    union all select '门户媒体查询', 9, 'crehn:cms:media:list'
    union all select '门户媒体编辑', 10, 'crehn:cms:media:edit'
    union all select '首页组件查询', 11, 'crehn:cms:home:list'
    union all select '首页组件编辑', 12, 'crehn:cms:home:edit'
    union all select '首页组件发布', 13, 'crehn:cms:home:publish'
) x
where not exists (select 1 from sys_menu m where m.perms = x.perms);

-- Activity administrators initially receive the same full CREHN tree.
-- A secondary administrator can later be reduced through normal role-menu configuration.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id = @crehn_root_menu_id
    or m.parent_id = @crehn_root_menu_id
    or m.parent_id in (select c.menu_id from sys_menu c where c.parent_id = @crehn_root_menu_id)
    or m.parent_id in (
        select c2.menu_id from sys_menu c2
        where c2.parent_id in (select c1.menu_id from sys_menu c1 where c1.parent_id = @crehn_root_menu_id)
    )
where r.role_key in ('crehn_admin', 'crehn_sub_admin');

-- School contacts: own projects, participant batches, school review/final submit and messages.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id = @crehn_root_menu_id
    or m.menu_id in (@participant_menu_id, @school_submission_menu_id)
    or m.parent_id in (@participant_menu_id, @school_submission_menu_id)
    or m.path in ('project', 'message')
    or m.perms in (
        'crehn:project:list', 'crehn:project:query', 'crehn:project:add',
        'crehn:project:submit', 'crehn:project:upload',
        'crehn:projectView:draft', 'crehn:projectView:submitted', 'crehn:projectView:audited'
    )
where r.role_key = 'crehn_school';

-- Participants: only their own project workflow and messages.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id = @crehn_root_menu_id
    or m.path in ('project', 'message')
    or m.perms in (
        'crehn:project:list', 'crehn:project:query', 'crehn:project:add',
        'crehn:project:submit', 'crehn:project:upload',
        'crehn:projectView:draft', 'crehn:projectView:submitted', 'crehn:projectView:audited'
    )
where r.role_key = 'crehn_participant';

-- Review and result roles reuse the inherited DYZ-compatible menus by permission prefix.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id = @crehn_root_menu_id
    or m.path in ('review', 'review-score-sheet')
    or m.perms like 'crehn:review:%'
where r.role_key in ('crehn_reviewer', 'crehn_expert');

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id = @crehn_root_menu_id
    or m.path = 'audit'
    or m.perms like 'crehn:audit:%'
where r.role_key = 'crehn_auditor';

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id = @crehn_root_menu_id
    or m.path in ('result', 'score-summary')
    or m.perms like 'crehn:result:%'
where r.role_key = 'crehn_score_summary';

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (@crehn_root_menu_id, @cms_menu_id)
    or m.parent_id = @cms_menu_id
where r.role_key = 'crehn_cms_editor'
  and (m.perms is null or m.perms = '' or m.perms in (
      'crehn:cms:article:list', 'crehn:cms:article:edit',
      'crehn:cms:site:list', 'crehn:cms:channel:list',
      'crehn:cms:media:list', 'crehn:cms:media:edit',
      'crehn:cms:home:list', 'crehn:cms:home:edit'
  ));

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (@crehn_root_menu_id, @cms_menu_id)
    or m.parent_id = @cms_menu_id
where r.role_key = 'crehn_cms_publisher';
