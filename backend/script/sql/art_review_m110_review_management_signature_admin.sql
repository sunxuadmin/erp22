-- --------------------------------------------------
-- M11.0 reviewer-management menu reorganization, privileged signature
-- library controls, and the shrinkable crehn_admin_signature child role.
--
-- Safe to rerun. The child role uses crehn_admin as its permission ceiling, so
-- its menu set can be reduced in role management but can never exceed the
-- project-administrator menu scope.
-- --------------------------------------------------

start transaction;

-- Administrative status is independent of active_marker and del_flag.
-- Therefore disabled/archived handwritten signatures remain auditable and
-- existing signed-sheet snapshots keep referring to their exact OSS object.
alter table review_score_sheet_signature
    add column if not exists library_status varchar(16) not null default 'active' comment 'active / disabled / archived';

alter table review_score_sheet_signature
    add column if not exists status_changed_at datetime null comment 'signature library state change time';

alter table review_score_sheet_signature
    add column if not exists status_changed_by_user_id bigint null comment 'signature library state change operator';

alter table review_score_sheet_signature
    add column if not exists status_reason varchar(500) null comment 'required privileged state-change reason';

create index if not exists idx_review_score_sheet_signature_manage
    on review_score_sheet_signature (tenant_id, library_status, reviewer_user_id, active_marker, del_flag);

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17960, '评审管理', 17000, 12, 'review-management', null, null, 1, 0, 'M', '0', '0', '', 'management', 103, 1, sysdate(), null, null, '评审分配、评分汇总、签名表与结果管理目录'),
(17952, '评分汇总', 17960, 3, 'score-summary', 'crehn/result/score-summary/index', null, 1, 0, 'C', '0', '0', 'crehn:reviewScoreSummary:list', 'histogram', 103, 1, sysdate(), null, null, '按项目汇总查看评委评分和签名状态'),
(17953, '评分汇总详情', 17952, 1, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewScoreSummary:detail', '#', 103, 1, sysdate(), null, null, ''),
(17954, '评分汇总配置', 17952, 2, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:reviewScoreSummary:config', '#', 103, 1, sysdate(), null, null, ''),
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

-- Keep existing IDs and permissions intact while collecting administrator
-- functions under one directory. Personal reviewer pages remain at the root.
update sys_menu
set parent_id = 17960,
    menu_name = case when menu_id = 17990 then '签名评分表' else menu_name end,
    order_num = case menu_id
        when 17900 then 1
        when 17930 then 2
        when 17990 then 4
        when 17940 then 6
        else order_num
    end,
    update_time = sysdate()
where menu_id in (17900, 17930, 17990, 17940);

-- Every existing role that already owns a moved child also receives the new
-- directory node, avoiding orphaned routes after the hierarchy change.
insert ignore into sys_role_menu(role_id, menu_id)
select distinct rm.role_id, 17960
from sys_role_menu rm
where rm.menu_id in (17900, 17930, 17940, 17990);

-- Project administrators receive the new score-summary and signature-library
-- controls. crehn_ops is intentionally not granted either signed-sheet summary
-- or signature-library access.
insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17000, 17960, 17952, 17953, 17954, 17961, 17962, 17963, 17964, 17990, 17991, 17992)
where r.role_key = 'crehn_admin'
  and r.del_flag = '0';

-- crehn_ops may have inherited the old signed-sheet menu from the historical
-- crehn_admin baseline. Remove only the new summary/signature controls; its
-- other review-management access remains unchanged.
delete rm
from sys_role_menu rm
join sys_role r on r.role_id = rm.role_id
where r.role_key = 'crehn_ops'
  and rm.menu_id in (17990, 17991, 17992, 17952, 17953, 17954, 17961, 17962, 17963, 17964);

-- Create a dedicated child administrator. The role is restricted by the
-- generic permission-scope enforcement in SysRoleService/SysMenuMapper; its
-- scope role ID is server-owned and cannot be elevated by the role-edit form.
insert into sys_role
(role_id, tenant_id, role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, permission_scope_role_id, status, del_flag, create_dept, create_by, create_time, update_by, update_time, remark)
select 900108, admin_role.tenant_id, '评审签字管理员', 'crehn_admin_signature', 91, '1', 1, 1, admin_role.role_id, '0', '0', 103, 1, sysdate(), null, null,
       '初始复制艺术评审配置管理员权限；以其为权限上限，由超级管理员缩减为最小签名管理权限'
from sys_role admin_role
where admin_role.tenant_id = '000000'
  and admin_role.role_key = 'crehn_admin'
  and admin_role.del_flag = '0'
  and not exists (
      select 1 from sys_role role_row
      where role_row.tenant_id = admin_role.tenant_id
        and role_row.role_key = 'crehn_admin_signature'
  );

update sys_role signature_admin
join sys_role admin_role
  on admin_role.tenant_id = signature_admin.tenant_id
 and admin_role.role_key = 'crehn_admin'
 and admin_role.del_flag = '0'
set signature_admin.role_name = '评审签字管理员',
    signature_admin.role_sort = 91,
    signature_admin.data_scope = '1',
    signature_admin.menu_check_strictly = 1,
    signature_admin.dept_check_strictly = 1,
    signature_admin.permission_scope_role_id = admin_role.role_id,
    signature_admin.status = '0',
    signature_admin.del_flag = '0',
    signature_admin.remark = '初始复制艺术评审配置管理员权限；以其为权限上限，由超级管理员缩减为最小签名管理权限',
    signature_admin.update_time = sysdate()
where signature_admin.tenant_id = '000000'
  and signature_admin.role_key = 'crehn_admin_signature';

-- Initialize the new child role once with the same current menu baseline as
-- crehn_admin. A persistent marker means later manual reductions are never
-- restored by a migration rerun; the super administrator can then reduce it
-- to the minimum signature-management scope.
set @art_signature_admin_m110_apply := if(
    exists (
        select 1 from sys_role role_row
        where role_row.tenant_id = '000000'
          and role_row.role_key = 'crehn_admin_signature'
          and role_row.del_flag = '0'
    ) and not exists (
        select 1 from sys_config
        where tenant_id = '000000'
          and config_key = 'crehn.signatureAdmin.permissionBaseline.m110'
    ),
    1,
    0
);

delete signature_menu
from sys_role_menu signature_menu
join sys_role signature_admin on signature_admin.role_id = signature_menu.role_id
where signature_admin.tenant_id = '000000'
  and signature_admin.role_key = 'crehn_admin_signature'
  and @art_signature_admin_m110_apply = 1;

-- Initial-copy the complete current crehn_admin menu baseline. Subsequent
-- shrinkage is a deliberate super-administrator action and is preserved.
insert ignore into sys_role_menu(role_id, menu_id)
select signature_admin.role_id, admin_menu.menu_id
from sys_role signature_admin
join sys_role admin_role
  on admin_role.role_id = signature_admin.permission_scope_role_id
 and admin_role.role_key = 'crehn_admin'
 and admin_role.del_flag = '0'
join sys_role_menu admin_menu on admin_menu.role_id = admin_role.role_id
where signature_admin.tenant_id = '000000'
  and signature_admin.role_key = 'crehn_admin_signature'
  and @art_signature_admin_m110_apply = 1;

insert into sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
select next_config.config_id, signature_admin.tenant_id,
       '评审签字管理员权限基线版本',
       'crehn.signatureAdmin.permissionBaseline.m110',
       'signature-admin-v1', 'Y', 103, 1, sysdate(), null, null,
       '标记 crehn_admin_signature 已完成初始签字管理权限复制，后续人工缩权不会被迁移脚本覆盖'
from sys_role signature_admin
cross join (
    select coalesce(max(config_id), 0) + 1 as config_id from sys_config
) next_config
where signature_admin.tenant_id = '000000'
  and signature_admin.role_key = 'crehn_admin_signature'
  and @art_signature_admin_m110_apply = 1
  and not exists (
      select 1 from sys_config existing
      where existing.tenant_id = signature_admin.tenant_id
        and existing.config_key = 'crehn.signatureAdmin.permissionBaseline.m110'
  )
limit 1;

commit;
