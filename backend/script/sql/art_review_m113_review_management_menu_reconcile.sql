-- --------------------------------------------------
-- M11.3 reconcile reviewer-management menu hierarchy and expert template access.
--
-- M11.0 introduced the directory, but databases that had already recorded an
-- earlier partial menu migration can still retain these two pages at the
-- crehn root. Reapply only their final parent/name/order state here.
-- --------------------------------------------------

start transaction;

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17960, '评审管理', 17000, 12, 'review-management', null, null, 1, 0, 'M', '0', '0', '', 'management', 103, 1, sysdate(), null, null, '评审分配、评分表模板、评分汇总、签名评分表和评审结果管理目录')
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
    menu_name = case when menu_id = 17990 then '签名评分表' else '评分表模板' end,
    order_num = case menu_id when 17930 then 2 when 17990 then 4 else order_num end,
    update_time = sysdate()
where menu_id in (17930, 17990);

-- Any role that can still open either administrative page needs the parent
-- directory so the dynamic menu tree remains complete after the move.
insert ignore into sys_role_menu(role_id, menu_id)
select distinct rm.role_id, 17960
from sys_role_menu rm
join sys_role r on r.role_id = rm.role_id
where rm.menu_id in (17930, 17990)
  and r.role_key <> 'crehn_expert'
  and r.del_flag = '0';

-- Review experts continue to receive only the hidden query function. It is
-- required by the score-submission dialog to read administrator-published
-- templates, but no longer grants a template page route or any write action.
delete rm
from sys_role_menu rm
join sys_role r on r.role_id = rm.role_id
where r.role_key = 'crehn_expert'
  and rm.menu_id in (17930, 17932, 17960);

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, 17931
from sys_role r
where r.role_key = 'crehn_expert'
  and r.del_flag = '0';

commit;
