-- --------------------------------------------------
-- Art Review M6.8 project recycle bin and audit scope.
-- Safe to rerun after art_review_menu.sql and M4.6/M5/M6 scripts.
-- --------------------------------------------------

alter table project
    add column if not exists recycled_from_status varchar(32) default null comment 'status before recycle' after status,
    add column if not exists recycled_by bigint(20) default null comment 'recycled by' after recycled_from_status,
    add column if not exists recycled_at datetime default null comment 'recycled time' after recycled_by,
    add column if not exists recycle_reason varchar(500) default null comment 'recycle reason' after recycled_at;

create table if not exists audit_assignment
(
    id                    bigint(20)   not null comment 'assignment id',
    tenant_id             varchar(20)  default '000000' comment 'tenant id',
    activity_id            bigint(20)   not null comment 'activity id',
    category_id            bigint(20)   default null comment 'category id; null means all categories under activity',
    auditor_user_id        bigint(20)   not null comment 'auditor user id',
    allow_query            tinyint(1)   default 1 comment 'can view projects',
    allow_pass             tinyint(1)   default 0 comment 'can pass audit',
    allow_return           tinyint(1)   default 0 comment 'can return audit',
    allow_withdraw_pass    tinyint(1)   default 0 comment 'can withdraw pass',
    allow_withdraw_return  tinyint(1)   default 0 comment 'can withdraw return',
    allow_download         tinyint(1)   default 1 comment 'can download/preview files',
    status                 varchar(32)  default 'active' comment 'active/disabled',
    assigned_by            bigint(20)   default null comment 'assigned by',
    assigned_at            datetime     default null comment 'assigned time',
    create_dept            bigint(20)   default null comment 'create dept',
    create_by              bigint(20)   default null comment 'create by',
    create_time            datetime     default null comment 'create time',
    update_by              bigint(20)   default null comment 'update by',
    update_time            datetime     default null comment 'update time',
    del_flag               char(1)      default '0' comment 'delete flag',
    primary key (id),
    unique key uk_audit_assignment_scope_user (activity_id, category_id, auditor_user_id, del_flag),
    key idx_audit_assignment_user (auditor_user_id, status),
    key idx_audit_assignment_scope (activity_id, category_id, status)
) engine=innodb comment='art audit assignment';

update sys_menu
set menu_name = convert(0xE4BD9CE593812FE99984E4BBB6E59B9EE694B6E7AB99 using utf8mb4),
    remark = convert(0xE4BD9CE59381E5928CE99984E4BBB6E59B9EE694B6E7AB99EFBC8CE694AFE68C81E681A2E5A48DE5928CE6B885E79086 using utf8mb4)
where menu_id = 17700;

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17305, convert(0xE9A1B9E79BAEE588A0E999A4 using utf8mb4), 17003, 5, '', '', '', 1, 0, 'F', '0', '0', 'crehn:project:remove', '#', 103, 1, sysdate(), null, null, ''),
(17420, convert(0xE5AEA1E6A0B8E68E88E69D83 using utf8mb4), 17004, 20, 'audit-assignment', 'crehn/audit-assignment/index', '', 1, 0, 'C', '0', '0', 'crehn:auditScope:list', 'peoples', 103, 1, sysdate(), null, null, ''),
(17421, convert(0xE5AEA1E6A0B8E68E88E69D83E69FA5E8AFA2 using utf8mb4), 17420, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:auditScope:list', '#', 103, 1, sysdate(), null, null, ''),
(17422, convert(0xE5AEA1E6A0B8E68E88E69D83E8AFA6E68385 using utf8mb4), 17420, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:auditScope:query', '#', 103, 1, sysdate(), null, null, ''),
(17423, convert(0xE5AEA1E6A0B8E68E88E69D83E696B0E5A29E using utf8mb4), 17420, 3, '', '', '', 1, 0, 'F', '0', '0', 'crehn:auditScope:add', '#', 103, 1, sysdate(), null, null, ''),
(17424, convert(0xE5AEA1E6A0B8E68E88E69D83E4BFAEE694B9 using utf8mb4), 17420, 4, '', '', '', 1, 0, 'F', '0', '0', 'crehn:auditScope:edit', '#', 103, 1, sysdate(), null, null, ''),
(17425, convert(0xE5AEA1E6A0B8E68E88E69D83E588A0E999A4 using utf8mb4), 17420, 5, '', '', '', 1, 0, 'F', '0', '0', 'crehn:auditScope:remove', '#', 103, 1, sysdate(), null, null, ''),
(17426, convert(0xE5AEA1E6A0B8E585A8E983A8E88C83E59BB4 using utf8mb4), 17420, 6, '', '', '', 1, 0, 'F', '0', '0', 'crehn:auditScope:all', '#', 103, 1, sysdate(), null, null, '');

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17305)
where r.role_key = 'crehn_school';

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17305, 17420, 17421, 17422, 17423, 17424, 17425, 17426, 17700, 17701, 17702, 17703)
where r.role_key = 'crehn_admin';

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17004, 17401, 17402, 17403, 17404)
where r.role_key = 'crehn_auditor';

insert into audit_assignment
(id, tenant_id, activity_id, category_id, auditor_user_id, allow_query, allow_pass, allow_return, allow_withdraw_pass, allow_withdraw_return, allow_download, status, assigned_by, assigned_at, create_dept, create_by, create_time, del_flag)
select 9680102, '000000', 900101, null, u.user_id, 1, 1, 1, 1, 1, 1, 'active', 1, sysdate(), 103, 1, sysdate(), '0'
from sys_user u
where u.user_id = 900102
  and u.user_name = 'crehn_auditor'
  and exists (select 1 from activity a where a.id = 900101)
on duplicate key update
    activity_id = values(activity_id),
    category_id = values(category_id),
    auditor_user_id = values(auditor_user_id),
    allow_query = values(allow_query),
    allow_pass = values(allow_pass),
    allow_return = values(allow_return),
    allow_withdraw_pass = values(allow_withdraw_pass),
    allow_withdraw_return = values(allow_withdraw_return),
    allow_download = values(allow_download),
    status = values(status),
    update_by = 1,
    update_time = sysdate(),
    del_flag = '0';
