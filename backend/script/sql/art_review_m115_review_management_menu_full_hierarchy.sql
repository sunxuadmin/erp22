-- --------------------------------------------------
-- M11.5 controlled finalizer for the complete reviewer-management tree.
--
-- M11.0 remains the reviewed migration for signature-library schema and the
-- restricted child role. This file contains only idempotent menu and menu-role
-- records so it is safe for the menu 8 automatic migration allow-list.
-- --------------------------------------------------

start transaction;

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17960, '评审管理', 17000, 12, 'review-management', null, null, 1, 0, 'M', '0', '0', '', 'management', 103, 1, sysdate(), null, null, '评审分配、评分表模板、评分汇总、签名表汇总、评分老师签名库和评审结果管理目录'),
(17952, '评分汇总', 17960, 3, 'score-summary', 'crehn/result/score-summary/index', null, 1, 0, 'C', '0', '0', 'crehn:reviewScoreSummary:list', 'histogram', 103, 1, sysdate(), null, null, '按项目汇总查看评委评分和签名状态'),
(17953, '评分汇总详情', 17952, 1, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewScoreSummary:detail', '#', 103, 1, sysdate(), null, null, ''),
(17954, '评分汇总配置', 17952, 2, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewScoreSummary:config', '#', 103, 1, sysdate(), null, null, ''),
(17990, '签名表汇总', 17960, 4, 'signed-sheets', 'crehn/review-score-sheet/signed-sheets/index', null, 1, 0, 'C', '0', '0', 'crehn:reviewSheet:manage', 'collection', 103, 1, sysdate(), null, null, '仅艺术评审管理员查看全部评审老师签名表'),
(17991, '签名表汇总查询', 17990, 1, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewSheet:manage', '#', 103, 1, sysdate(), null, null, ''),
(17992, '签名表强制撤回', 17990, 2, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewSheet:withdraw', '#', 103, 1, sysdate(), null, null, '管理员可撤回任意有效签名表，服务端保留撤回审计'),
(17961, '评分老师签名库', 17960, 5, 'signature-library', 'crehn/review-score-sheet/signature-library/index', null, 1, 0, 'C', '0', '0', 'crehn:reviewSheet:signatureManage', 'signature', 103, 1, sysdate(), null, null, '查看、停用、恢复或归档评分老师个人手写签名；不会修改历史签名表快照'),
(17962, '评分老师签名库查询', 17961, 1, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewSheet:signatureManage', '#', 103, 1, sysdate(), null, null, ''),
(17963, '评分老师签名库停用恢复', 17961, 2, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewSheet:signatureManage:edit', '#', 103, 1, sysdate(), null, null, '停用或恢复必须记录操作原因'),
(17964, '评分老师签名库归档', 17961, 3, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewSheet:signatureManage:archive', '#', 103, 1, sysdate(), null, null, '归档必须记录操作原因')
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
    menu_name = case when menu_id = 17990 then '签名表汇总' else menu_name end,
    order_num = case menu_id
        when 17900 then 1
        when 17930 then 2
        when 17990 then 4
        when 17940 then 6
        else order_num
    end,
    update_time = sysdate()
where menu_id in (17900, 17930, 17940, 17990);

insert ignore into sys_role_menu(role_id, menu_id)
select distinct rm.role_id, 17960
from sys_role_menu rm
join sys_role r on r.role_id = rm.role_id
where rm.menu_id in (17900, 17930, 17940, 17990)
  and r.role_key <> 'crehn_expert'
  and r.del_flag = '0';

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17960, 17952, 17953, 17954, 17961, 17962, 17963, 17964, 17990, 17991, 17992)
where r.role_key = 'crehn_admin'
  and r.del_flag = '0';

commit;
