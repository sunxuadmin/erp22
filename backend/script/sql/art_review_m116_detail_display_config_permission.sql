-- --------------------------------------------------
-- M11.6 dedicated audit/review detail-display configuration permission.
--
-- The initial grant is applied once. A persistent marker prevents later
-- role-management reductions from being restored when this migration is
-- inspected or replayed in a controlled recovery.
-- --------------------------------------------------

start transaction;

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17406, '审核与评分弹窗设置', 17004, 6, '#', '', null, 1, 0, 'F', '0', '0', 'crehn:detailDisplayConfig:edit', '#', 103, 1, sysdate(), null, null,
 '允许维护审核与评分详情的默认标签、摘要字段、作品文件和评分卡片显示配置')
on duplicate key update
    menu_name = values(menu_name),
    parent_id = values(parent_id),
    order_num = values(order_num),
    menu_type = values(menu_type),
    visible = values(visible),
    status = values(status),
    perms = values(perms),
    remark = values(remark),
    update_time = sysdate();

set @art_detail_display_permission_apply := if(
    exists (
        select 1 from sys_role
        where tenant_id = '000000'
          and role_key = 'crehn_admin'
          and del_flag = '0'
    ) and not exists (
        select 1 from sys_config
        where tenant_id = '000000'
          and config_key = 'crehn.detailDisplayConfig.permissionBaseline.m116'
    ),
    1,
    0
);

insert ignore into sys_role_menu(role_id, menu_id)
select role_row.role_id, 17406
from sys_role role_row
where role_row.role_key in ('crehn_admin', 'crehn_ops', 'crehn_admin_signature')
  and role_row.tenant_id = '000000'
  and role_row.del_flag = '0'
  and @art_detail_display_permission_apply = 1;

insert into sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
select next_config.config_id, admin_role.tenant_id,
       '审核与评分弹窗设置权限基线',
       'crehn.detailDisplayConfig.permissionBaseline.m116',
       'art-admin-ops-signature-v1', 'Y', 103, 1, sysdate(), null, null,
       '标记审核与评分弹窗设置专用权限已完成一次性初始授权，后续角色管理缩权不会被迁移重放覆盖'
from sys_role admin_role
cross join (
    select coalesce(max(config_id), 0) + 1 as config_id from sys_config
) next_config
where admin_role.role_key = 'crehn_admin'
  and admin_role.tenant_id = '000000'
  and admin_role.del_flag = '0'
  and @art_detail_display_permission_apply = 1
  and not exists (
      select 1 from sys_config existing
      where existing.tenant_id = admin_role.tenant_id
        and existing.config_key = 'crehn.detailDisplayConfig.permissionBaseline.m116'
  );

commit;
