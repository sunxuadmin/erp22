-- --------------------------------------------------
-- M11.4 keep score-sheet administration pages under reviewer management.
--
-- M10.0/M10.8 are initialization migrations. They retain their original
-- root-menu defaults for reviewed manual initialization, so this controlled
-- finalizer is the automatic, repeatable source of the live hierarchy.
-- It intentionally does not revoke role access; M11.3 remains the reviewed
-- manual migration that removes expert template-page/edit grants.
-- --------------------------------------------------

start transaction;

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17960, '评审管理', 17000, 12, 'review-management', null, null, 1, 0, 'M', '0', '0', '', 'management', 103, 1, sysdate(), null, null, '评审分配、评分表模板、评分汇总、签名表汇总和评审结果管理目录')
on duplicate key update
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    path = values(path),
    component = values(component),
    query_param = values(query_param),
    is_frame = values(is_frame),
    is_cache = values(is_cache),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    icon = values(icon),
    remark = values(remark),
    update_time = sysdate();

update sys_menu
set parent_id = 17960,
    menu_name = case when menu_id = 17990 then '签名表汇总' else '评分表模板' end,
    order_num = case menu_id when 17930 then 2 when 17990 then 4 else order_num end,
    update_time = sysdate()
where menu_id in (17930, 17990);

-- Preserve a complete dynamic menu tree for every non-expert role that can
-- open either administrative page. Access revocation stays in M11.3.
insert ignore into sys_role_menu(role_id, menu_id)
select distinct rm.role_id, 17960
from sys_role_menu rm
join sys_role r on r.role_id = rm.role_id
where rm.menu_id in (17930, 17990)
  and r.role_key <> 'crehn_expert'
  and r.del_flag = '0';

commit;
