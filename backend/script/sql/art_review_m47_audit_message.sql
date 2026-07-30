-- --------------------------------------------------
-- Art Review M4.7 audit in-site message.
-- Safe to rerun after art_review_menu.sql and art_review_m46_admin_ops.sql.
-- Chinese labels are written as UTF-8 hex to avoid Windows codepage damage.
-- --------------------------------------------------

create table if not exists art_user_message
(
    id               bigint(20)   not null comment 'message id',
    tenant_id        varchar(20)  default '000000' comment 'tenant id',
    receiver_user_id bigint(20)   not null comment 'receiver user id',
    sender_user_id   bigint(20)   default null comment 'sender user id',
    school_id        bigint(20)   default null comment 'school id',
    activity_id      bigint(20)   default null comment 'activity id',
    category_id      bigint(20)   default null comment 'category id',
    project_id       bigint(20)   default null comment 'project id',
    message_type     varchar(32)  not null comment 'audit/result/system',
    source_type      varchar(64)  default null comment 'source type',
    source_id        bigint(20)   default null comment 'source id',
    title            varchar(255) not null comment 'message title',
    content          text         null comment 'message content',
    read_status      varchar(16)  default 'unread' comment 'unread/read',
    read_time        datetime     default null comment 'read time',
    sent_at          datetime     default null comment 'sent time',
    create_dept      bigint(20)   default null comment 'create dept',
    create_by        bigint(20)   default null comment 'create by',
    create_time      datetime     default null comment 'create time',
    update_by        bigint(20)   default null comment 'update by',
    update_time      datetime     default null comment 'update time',
    del_flag         char(1)      default '0' comment 'delete flag',
    primary key (id),
    key idx_art_user_message_receiver (receiver_user_id, read_status, sent_at),
    key idx_art_user_message_project (project_id),
    key idx_art_user_message_school (school_id)
) engine=innodb comment='art user in-site message';

insert ignore into sys_menu
(menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, update_by, update_time, remark)
values
(17800, convert(0xE68891E79A84E6B688E681AF using utf8mb4), 17000, 11, 'message', 'crehn/message/index', '', 1, 0, 'C', '0', '0', 'crehn:message:list', 'message', 103, 1, sysdate(), null, null, convert(0xE5ADA6E6A0A1E7ABAFE69FA5E79C8BE5AEA1E6A0B8E9809AE79FA5E38081E98080E59B9EE58E9FE59BA0E5928CE9809AE8BF87E7BB93E69E9C using utf8mb4)),
(17801, convert(0xE68891E79A84E6B688E681AFE69FA5E8AFA2 using utf8mb4), 17800, 1, '', '', '', 1, 0, 'F', '0', '0', 'crehn:message:list', '#', 103, 1, sysdate(), null, null, ''),
(17802, convert(0xE6A087E8AEB0E5B7B2E8AFBB using utf8mb4), 17800, 2, '', '', '', 1, 0, 'F', '0', '0', 'crehn:message:read', '#', 103, 1, sysdate(), null, null, '');

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id in (17800, 17801, 17802)
where r.role_key in ('crehn_school', 'crehn_admin');
