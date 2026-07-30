-- --------------------------------------------------
-- Art Review M2/M3/M4 core workflow
-- Scope: activity/category/schema/file requirement/project/project file/audit record
-- --------------------------------------------------

alter table sys_user
    add column if not exists login_alias varchar(64) default null comment 'English login alias' after user_name,
    add column if not exists school_id bigint(20) default null comment '学校ID，学校用户使用' after user_type,
    add column if not exists school_review_status varchar(32) default null comment '学校账号审核状态 pending_review/enabled/rejected/disabled/locked' after school_id,
    add column if not exists school_review_by bigint(20) default null comment '学校账号审核人' after school_review_status,
    add column if not exists school_review_time datetime default null comment '学校账号审核时间' after school_review_by,
    add column if not exists school_review_opinion varchar(500) default null comment '学校账号审核意见' after school_review_time;

create unique index if not exists uk_sys_user_login_alias on sys_user(login_alias);

insert into sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
select 17001, '000000', '艺术评审-是否允许自由注册', 'crehn.register.allow_free_register', 'false', 'Y', 103, 1, sysdate(), null, null, '默认 false：学校/专家需注册码，管理员不允许前台注册'
where not exists (select 1 from sys_config where config_key = 'crehn.register.allow_free_register');

insert into sys_config
(config_id, tenant_id, config_name, config_key, config_value, config_type, create_dept, create_by, create_time, update_by, update_time, remark)
select 17002, '000000', '艺术评审-是否必须注册码', 'crehn.register.register_code_required', 'true', 'Y', 103, 1, sysdate(), null, null, '默认 true：学校账号和专家账号必须使用注册码注册'
where not exists (select 1 from sys_config where config_key = 'crehn.register.register_code_required');

create table if not exists registration_code
(
    id            bigint(20)   not null comment 'ID',
    tenant_id     varchar(20)  default '000000' comment '租户编号',
    code          varchar(64)  not null comment '注册码',
    code_type     varchar(32)  not null comment '类型 school/expert/admin',
    activity_id   bigint(20)   default null comment '绑定活动',
    school_id     bigint(20)   default null comment '绑定学校',
    bound_phone   varchar(32)  default null comment '绑定手机号',
    role_key      varchar(64)  default null comment '绑定角色',
    max_use_count int          default 1 comment '最大使用次数',
    used_count    int          default 0 comment '已使用次数',
    expire_at     datetime     default null comment '有效期',
    status        varchar(32)  default 'unused' comment '状态 unused/active/used/voided',
    remark        varchar(500) default null comment '备注',
    create_dept   bigint(20)   default null comment '创建部门',
    create_by     bigint(20)   default null comment '创建者',
    create_time   datetime     default null comment '创建时间',
    update_by     bigint(20)   default null comment '更新者',
    update_time   datetime     default null comment '更新时间',
    del_flag      char(1)      default '0' comment '删除标志',
    primary key (id),
    unique key uk_registration_code_code (code),
    key idx_registration_code_type (code_type),
    key idx_registration_code_status (status),
    key idx_registration_code_school (school_id),
    key idx_registration_code_activity (activity_id)
) engine = innodb comment = '注册码表';

create table if not exists registration_code_usage
(
    id            bigint(20)   not null comment 'ID',
    tenant_id     varchar(20)  default '000000' comment '租户编号',
    code_id       bigint(20)   not null comment '注册码ID',
    user_id       bigint(20)   default null comment '绑定账号ID',
    used_at       datetime     not null comment '使用时间',
    ip_address    varchar(64)  default null comment 'IP',
    user_agent    varchar(512) default null comment 'UserAgent',
    bound_account varchar(64)  default null comment '绑定账号',
    create_dept   bigint(20)   default null comment '创建部门',
    create_by     bigint(20)   default null comment '创建者',
    create_time   datetime     default null comment '创建时间',
    update_by     bigint(20)   default null comment '更新者',
    update_time   datetime     default null comment '更新时间',
    primary key (id),
    key idx_registration_code_usage_code (code_id),
    key idx_registration_code_usage_user (user_id)
) engine = innodb comment = '注册码使用记录表';

create table activity
(
    id           bigint(20)   not null comment '活动ID',
    tenant_id    varchar(20)  default '000000' comment '租户编号',
    activity_name varchar(128) not null comment '活动名称',
    menu_name    varchar(64)  default null comment '学校端菜单显示名称',
    edition      varchar(64)  default null comment '届次',
    year         int          default null comment '年度',
    organizer    varchar(255) default null comment '主办单位',
    undertaker   varchar(255) default null comment '承办单位',
    signup_start_at datetime  default null comment '报名开始时间',
    signup_end_at   datetime  default null comment '报名结束时间',
    description  text         comment '活动说明',
    status       varchar(32)  default 'draft' comment '状态 draft/enabled/paused/ended/archived',
    create_dept  bigint(20)   default null comment '创建部门',
    create_by    bigint(20)   default null comment '创建者',
    create_time  datetime     default null comment '创建时间',
    update_by    bigint(20)   default null comment '更新者',
    update_time  datetime     default null comment '更新时间',
    del_flag     char(1)      default '0' comment '删除标志 0存在 1删除',
    primary key (id)
) engine=innodb comment='活动表';

create table activity_category
(
    id            bigint(20)   not null comment '类别ID',
    tenant_id     varchar(20)  default '000000' comment '租户编号',
    activity_id   bigint(20)   not null comment '活动ID',
    parent_id     bigint(20)   default 0 comment '父类别ID',
    category_code varchar(64)  not null comment '类别编码',
    category_name varchar(128) not null comment '类别名称',
    quota_limit   int          default null comment '名额限制',
    sort_order    int          default 0 comment '排序',
    enabled       tinyint(1)   default 1 comment '是否启用',
    create_dept   bigint(20)   default null comment '创建部门',
    create_by     bigint(20)   default null comment '创建者',
    create_time   datetime     default null comment '创建时间',
    update_by     bigint(20)   default null comment '更新者',
    update_time   datetime     default null comment '更新时间',
    del_flag      char(1)      default '0' comment '删除标志 0存在 1删除',
    primary key (id),
    key idx_activity_category_activity (activity_id)
) engine=innodb comment='活动类别表';

create table category_field_schema
(
    id              bigint(20)   not null comment '字段配置ID',
    tenant_id       varchar(20)  default '000000' comment '租户编号',
    category_id     bigint(20)   not null comment '类别ID',
    field_key       varchar(64)  not null comment '字段键，英文编码',
    field_label     varchar(128) not null comment '字段名称',
    field_type      varchar(32)  not null comment '字段类型 input/textarea/select/date/number/id_card',
    required        tinyint(1)   default 0 comment '是否必填',
    options_json    text         comment '选项JSON',
    validation_json text         comment '校验规则JSON',
    sensitive_flag  tinyint(1)   default 0 comment '列表默认脱敏',
    sort_order      int          default 0 comment '排序',
    create_dept     bigint(20)   default null comment '创建部门',
    create_by       bigint(20)   default null comment '创建者',
    create_time     datetime     default null comment '创建时间',
    update_by       bigint(20)   default null comment '更新者',
    update_time     datetime     default null comment '更新时间',
    del_flag        char(1)      default '0' comment '删除标志 0存在 1删除',
    primary key (id),
    key idx_category_field_category (category_id)
) engine=innodb comment='类别表单字段配置表';

create table category_file_requirement
(
    id              bigint(20)   not null comment '附件要求ID',
    tenant_id       varchar(20)  default '000000' comment '租户编号',
    category_id     bigint(20)   not null comment '类别ID',
    file_type_code  varchar(64)  not null comment '附件类型编码',
    file_type_name  varchar(128) not null comment '附件类型名称',
    allowed_ext     varchar(255) default null comment '允许扩展名，逗号分隔',
    max_size_mb     int          default null comment '单文件大小限制MB',
    min_count       int          default 0 comment '最少数量',
    max_count       int          default 1 comment '最多数量',
    required        tinyint(1)   default 0 comment '是否必传',
    sort_order      int          default 0 comment '排序',
    create_dept     bigint(20)   default null comment '创建部门',
    create_by       bigint(20)   default null comment '创建者',
    create_time     datetime     default null comment '创建时间',
    update_by       bigint(20)   default null comment '更新者',
    update_time     datetime     default null comment '更新时间',
    del_flag        char(1)      default '0' comment '删除标志 0存在 1删除',
    primary key (id),
    key idx_category_file_req_category (category_id)
) engine=innodb comment='类别附件要求表';

create table project
(
    id                    bigint(20)   not null comment '项目ID',
    tenant_id             varchar(20)  default '000000' comment '租户编号',
    activity_id           bigint(20)   not null comment '活动ID',
    school_id             bigint(20)   not null comment '学校ID',
    category_id           bigint(20)   not null comment '类别ID',
    project_no            varchar(64)  default null comment '项目编号',
    project_name          varchar(255) default null comment '项目名称',
    form_data_json        text         comment '动态表单数据JSON',
    status                varchar(32)  default 'draft' comment '状态 draft/submitted/returned/audit_passed',
    submitted_at          datetime     default null comment '提交时间',
    submitted_by          bigint(20)   default null comment '提交人',
    current_audit_opinion text         comment '最近审核意见',
    create_dept           bigint(20)   default null comment '创建部门',
    create_by             bigint(20)   default null comment '创建者',
    create_time           datetime     default null comment '创建时间',
    update_by             bigint(20)   default null comment '更新者',
    update_time           datetime     default null comment '更新时间',
    del_flag              char(1)      default '0' comment '删除标志 0存在 1删除',
    primary key (id),
    key idx_project_school (school_id),
    key idx_project_activity_category (activity_id, category_id),
    key idx_project_status (status)
) engine=innodb comment='项目表';

create table project_file
(
    id             bigint(20)   not null comment '项目附件ID',
    tenant_id      varchar(20)  default '000000' comment '租户编号',
    project_id     bigint(20)   not null comment '项目ID',
    requirement_id bigint(20)   default null comment '附件要求ID',
    oss_id         bigint(20)   default null comment 'OSS文件ID',
    file_type_code varchar(64)  not null comment '附件类型编码',
    original_name  varchar(255) not null comment '原文件名',
    storage_path   varchar(512) not null comment '存储路径或OSS URL',
    file_ext       varchar(32)  default null comment '扩展名',
    file_size      bigint(20)   default null comment '文件大小，字节',
    mime_type      varchar(128) default null comment 'MIME类型',
    version_no     int          default 1 comment '版本号',
    uploaded_by    bigint(20)   default null comment '上传人',
    uploaded_at    datetime     default null comment '上传时间',
    status         varchar(32)  default 'active' comment '状态 active/replaced/deleted',
    create_dept    bigint(20)   default null comment '创建部门',
    create_by      bigint(20)   default null comment '创建者',
    create_time    datetime     default null comment '创建时间',
    update_by      bigint(20)   default null comment '更新者',
    update_time    datetime     default null comment '更新时间',
    del_flag       char(1)      default '0' comment '删除标志 0存在 1删除',
    primary key (id),
    key idx_project_file_project (project_id),
    key idx_project_file_requirement (requirement_id)
) engine=innodb comment='项目附件表';

create table project_audit_record
(
    id           bigint(20)  not null comment '审核记录ID',
    tenant_id    varchar(20) default '000000' comment '租户编号',
    project_id   bigint(20)  not null comment '项目ID',
    from_status  varchar(32) not null comment '原状态',
    to_status    varchar(32) not null comment '新状态',
    audit_result varchar(32) not null comment '审核结果 pass/return',
    opinion      text        comment '审核意见',
    audited_by   bigint(20)  not null comment '审核人',
    audited_at   datetime    not null comment '审核时间',
    create_dept  bigint(20)  default null comment '创建部门',
    create_by    bigint(20)  default null comment '创建者',
    create_time  datetime    default null comment '创建时间',
    update_by    bigint(20)  default null comment '更新者',
    update_time  datetime    default null comment '更新时间',
    del_flag     char(1)     default '0' comment '删除标志 0存在 1删除',
    primary key (id),
    key idx_project_audit_project (project_id)
) engine=innodb comment='项目审核记录表';
