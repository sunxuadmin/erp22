-- Safe to rerun after RuoYi base tables and art review roles are initialized.

create table if not exists sys_workbench_layout
(
    id            bigint       not null,
    tenant_id     varchar(20)  default '000000',
    role_id       bigint       not null,
    component_key varchar(80)  not null,
    title         varchar(100) not null,
    width         varchar(20)  default '1/1',
    sort_order    int          default 1,
    visible       char(1)      default '0',
    config_json   text         null,
    create_dept   bigint       null,
    create_by     bigint       null,
    create_time   datetime     null,
    update_by     bigint       null,
    update_time   datetime     null,
    primary key (id),
    unique key uk_sys_workbench_layout_role_component (tenant_id, role_id, component_key),
    key idx_sys_workbench_layout_role (tenant_id, role_id, sort_order)
);

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(18000, '工作台配置', 1, 90, 'workbench', 'system/workbench/index', null, 1, 0, 'C', '0', '0', 'system:workbench:list', 'dashboard', 103, 1, sysdate(), null, null, '工作台布局配置菜单')
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
    remark = values(remark);

insert into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(18001, '工作台查询', 18000, 1, '#', '', null, 1, 0, 'F', '0', '0', 'system:workbench:query', '#', 103, 1, sysdate(), null, null, ''),
(18002, '工作台编辑', 18000, 2, '#', '', null, 1, 0, 'F', '0', '0', 'system:workbench:edit', '#', 103, 1, sysdate(), null, null, '')
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
    remark = values(remark);

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (1, 18000, 18001, 18002)
where r.role_key in ('admin', 'crehn_admin');

insert into sys_workbench_layout
(id, tenant_id, role_id, component_key, title, width, sort_order, visible, config_json, create_dept, create_by, create_time, update_by, update_time)
select 180400000 + r.role_id,
       r.tenant_id,
       r.role_id,
       'school_stage_notice',
       '学校活动公告',
       '1/1',
       1,
       '0',
       null,
       103,
       1,
       sysdate(),
       null,
       null
from sys_role r
where r.role_key in ('school', 'crehn_school')
  and not exists (
    select 1
    from sys_workbench_layout w
    where w.tenant_id = r.tenant_id
      and w.role_id = r.role_id
      and w.component_key = 'school_stage_notice'
  );

insert into sys_workbench_layout
(id, tenant_id, role_id, component_key, title, width, sort_order, visible, config_json, create_dept, create_by, create_time, update_by, update_time)
select 180800000 + r.role_id,
       r.tenant_id,
       r.role_id,
       'admin_stage_notice',
       '管理员公告',
       '1/1',
       1,
       '0',
       null,
       103,
       1,
       sysdate(),
       null,
       null
from sys_role r
where r.role_key in ('admin', 'crehn_admin')
  and not exists (
    select 1
    from sys_workbench_layout w
    where w.tenant_id = r.tenant_id
      and w.role_id = r.role_id
      and w.component_key = 'admin_stage_notice'
  );

insert into sys_workbench_layout
(id, tenant_id, role_id, component_key, title, width, sort_order, visible, config_json, create_dept, create_by, create_time, update_by, update_time)
select 180900000 + r.role_id,
       r.tenant_id,
       r.role_id,
       'audit_stage_notice',
       '审核公告',
       '1/1',
       1,
       '0',
       null,
       103,
       1,
       sysdate(),
       null,
       null
from sys_role r
where r.role_key in ('crehn_auditor', 'auditor')
  and not exists (
    select 1
    from sys_workbench_layout w
    where w.tenant_id = r.tenant_id
      and w.role_id = r.role_id
      and w.component_key = 'audit_stage_notice'
  );

insert into sys_workbench_layout
(id, tenant_id, role_id, component_key, title, width, sort_order, visible, config_json, create_dept, create_by, create_time, update_by, update_time)
select 180700000 + r.role_id,
       r.tenant_id,
       r.role_id,
       'project_upload_overview',
       '作品上传总览',
       '1/1',
       2,
       '0',
       null,
       103,
       1,
       sysdate(),
       null,
       null
from sys_role r
where r.role_key in ('admin', 'crehn_admin', 'crehn_auditor', 'auditor', 'crehn_project_viewer')
  and not exists (
    select 1
    from sys_workbench_layout w
    where w.tenant_id = r.tenant_id
      and w.role_id = r.role_id
      and w.component_key = 'project_upload_overview'
  );

insert into sys_workbench_layout
(id, tenant_id, role_id, component_key, title, width, sort_order, visible, config_json, create_dept, create_by, create_time, update_by, update_time)
select 181000000 + r.role_id,
       r.tenant_id,
       r.role_id,
       'quota_ratio_overview',
       '名额比例监控',
       '1/2',
       3,
       '0',
       null,
       103,
       1,
       sysdate(),
       null,
       null
from sys_role r
where r.role_key in ('admin', 'crehn_admin')
  and not exists (
    select 1
    from sys_workbench_layout w
    where w.tenant_id = r.tenant_id
      and w.role_id = r.role_id
      and w.component_key = 'quota_ratio_overview'
  );
