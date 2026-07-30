-- Stable business roles. Migration runner must execute serially.
-- IDs are allocated from current maxima; application code uses role_key only.

set @next_role_id = (select coalesce(max(role_id), 0) from sys_role);

insert into sys_role
(role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly,
 dept_check_strictly, status, del_flag, create_dept, create_by, create_time, remark)
select @next_role_id := @next_role_id + 1, '000000', x.role_name, x.role_key, x.role_sort,
       '1', 1, 1, '0', '0', null, 1, sysdate(), x.remark
from (
    select '活动管理员' role_name, 'crehn_admin' role_key, 20 role_sort, '活动全流程管理' remark
    union all select '次活动管理员', 'crehn_sub_admin', 21, '按授权减少权限'
    union all select '节目审核员', 'crehn_auditor', 30, '形式审核'
    union all select '评分员', 'crehn_reviewer', 40, '单评委评分'
    union all select '评分汇总员', 'crehn_score_summary', 50, '评分汇总只读与导出'
    union all select '学校联络员', 'crehn_school', 60, '本校名单审核推荐和最终提交'
    union all select '参赛者', 'crehn_participant', 70, '本人资料和作品'
    union all select 'CMS编辑', 'crehn_cms_editor', 80, '门户内容草稿'
    union all select 'CMS发布', 'crehn_cms_publisher', 81, '门户审核发布和回滚'
) x
where not exists (select 1 from sys_role r where r.tenant_id = '000000' and r.role_key = x.role_key);
