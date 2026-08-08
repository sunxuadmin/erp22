-- V009: remove legacy fixed activity-category menu grants from CREHN roles.
-- 分类: ProgramMigration；前置版本: V006 已执行。V007 与本迁移相互独立，不得由本文件执行。
-- 影响对象: sys_role_menu。保留所有 sys_menu 行；不删除活动、类别、作品或角色数据。
-- 幂等: 是。重复执行只会再次确认目标绑定为零。
-- 锁表/数据量风险: 仅删除 CREHN 角色与 18 个旧固定类别菜单的关联；在事务内执行，风险低。
-- 回滚/前向修复: 不自动恢复可能违反 FLOW-001 的旧授权。执行前导出 role-menu 快照；如需恢复，按该快照受控回写。
-- 验证: 预检必须发现唯一 CREHN 根菜单、完整且无额外叶子的 M70 18 项路径清单；提交后回读 CREHN 角色的目标绑定数为 0。
-- 生产自动执行: 否。仅在已备份、明确 SQL 授权的目标库通过受控入口单独执行。

-- 旧固定类别菜单仅通过稳定根路径、父子层级和完整路径清单定位，禁止使用 DYZ/历史数字主键。
create temporary table crehn_v009_expected_legacy_category_paths (
    parent_path varchar(64) not null,
    menu_path varchar(64) not null,
    primary key (parent_path, menu_path)
);

insert into crehn_v009_expected_legacy_category_paths(parent_path, menu_path) values
    ('crehn', 'performance'),
    ('crehn', 'artwork'),
    ('crehn', 'workshop'),
    ('crehn', 'achievement'),
    ('crehn', 'principal'),
    ('performance', 'vocal'),
    ('performance', 'instrumental'),
    ('performance', 'dance'),
    ('performance', 'drama'),
    ('performance', 'recitation'),
    ('performance', 'personal'),
    ('artwork', 'fine-art'),
    ('artwork', 'design-exhibition'),
    ('artwork', 'design'),
    ('artwork', 'video'),
    ('workshop', 'workshop-item'),
    ('achievement', 'paper'),
    ('achievement', 'teaching-case');

create temporary table crehn_v009_matched_legacy_category_menus (
    menu_id bigint not null primary key,
    parent_path varchar(64) not null,
    menu_path varchar(64) not null
);

insert into crehn_v009_matched_legacy_category_menus(menu_id, parent_path, menu_path)
select menu.menu_id, expected.parent_path, expected.menu_path
from crehn_v009_expected_legacy_category_paths expected
join sys_menu parent on parent.path = expected.parent_path
join sys_menu menu on menu.parent_id = parent.menu_id and menu.path = expected.menu_path
join sys_menu root on (
    (expected.parent_path = 'crehn' and root.menu_id = parent.menu_id)
    or (expected.parent_path <> 'crehn' and root.menu_id = parent.parent_id)
)
where root.parent_id = 0
  and root.path = 'crehn';

set @crehn_v009_root_count := (
    select count(*)
    from sys_menu
    where parent_id = 0
      and path = 'crehn'
);
set @crehn_v009_expected_menu_count := (select count(*) from crehn_v009_expected_legacy_category_paths);
set @crehn_v009_matched_menu_count := (select count(*) from crehn_v009_matched_legacy_category_menus);
set @crehn_v009_unexpected_leaf_count := (
    select count(*)
    from sys_menu leaf
    join sys_menu category_group on category_group.menu_id = leaf.parent_id
    join sys_menu root on root.menu_id = category_group.parent_id
    left join crehn_v009_expected_legacy_category_paths expected
        on expected.parent_path = category_group.path
       and expected.menu_path = leaf.path
    where root.parent_id = 0
      and root.path = 'crehn'
      and category_group.path in ('performance', 'artwork', 'workshop', 'achievement')
      and expected.menu_path is null
);
set @crehn_v009_before_role_menu_count := (
    select count(*)
    from sys_role_menu role_menu
    join sys_role role on role.role_id = role_menu.role_id
    join crehn_v009_matched_legacy_category_menus target on target.menu_id = role_menu.menu_id
    where role.role_key regexp '^crehn_'
);

select
    @crehn_v009_root_count as crehn_root_count,
    @crehn_v009_expected_menu_count as expected_legacy_category_menu_count,
    @crehn_v009_matched_menu_count as matched_legacy_category_menu_count,
    @crehn_v009_unexpected_leaf_count as unexpected_legacy_group_leaf_count,
    @crehn_v009_before_role_menu_count as crehn_role_legacy_category_binding_count;

-- Fail closed before any persistent write if the source hierarchy is not exactly the known M70 fixed-category tree.
create temporary table crehn_v009_preflight_guard (
    guard_value tinyint not null primary key
);
insert into crehn_v009_preflight_guard(guard_value)
select if(
    @crehn_v009_root_count = 1
    and @crehn_v009_expected_menu_count = 18
    and @crehn_v009_matched_menu_count = 18
    and @crehn_v009_unexpected_leaf_count = 0,
    1,
    0
);
insert into crehn_v009_preflight_guard(guard_value) values (0);

start transaction;

delete role_menu
from sys_role_menu role_menu
join sys_role role on role.role_id = role_menu.role_id
join crehn_v009_matched_legacy_category_menus target on target.menu_id = role_menu.menu_id
where role.role_key regexp '^crehn_';

set @crehn_v009_after_role_menu_count := (
    select count(*)
    from sys_role_menu role_menu
    join sys_role role on role.role_id = role_menu.role_id
    join crehn_v009_matched_legacy_category_menus target on target.menu_id = role_menu.menu_id
    where role.role_key regexp '^crehn_'
);

select
    @crehn_v009_after_role_menu_count as remaining_crehn_role_legacy_category_binding_count,
    (select count(*) from sys_menu menu join crehn_v009_matched_legacy_category_menus target on target.menu_id = menu.menu_id) as retained_legacy_menu_row_count;

commit;

drop temporary table crehn_v009_preflight_guard;
drop temporary table crehn_v009_matched_legacy_category_menus;
drop temporary table crehn_v009_expected_legacy_category_paths;
