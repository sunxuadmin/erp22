-- V008: expand CREHN user-type capacity for participant accounts.
-- 分类: RequiredSchema；前置版本: V006 已执行，V007 与本迁移无关且不得随本迁移执行。
-- 影响对象: 仅 sys_user.user_type，从 varchar(10) 向后兼容地扩大至 varchar(32)。
-- 幂等: 是。仅当 information_schema 显示字符长度小于 32 时执行 ALTER；已为 32 或更大时不改动。
-- 锁表/数据量风险: ALTER TABLE 可能获取 metadata lock；在 TEST 低峰执行，执行前确认无长事务。
-- 回滚/前向修复: 非破坏性。禁止收窄该列；如应用回滚，保留 varchar(32)，后续通过兼容代码前向修复。
-- 验证: 回读 information_schema.columns 的 data_type='varchar' 且 character_maximum_length=32。
-- 生产自动执行: 否。仅可在已备份、明确授权的目标 TEST 库通过受控入口单独执行。

set @crehn_user_type_needs_expand := (
    select case when count(*) = 1 and max(character_maximum_length) < 32 then 1 else 0 end
    from information_schema.columns
    where table_schema = database()
      and table_name = 'sys_user'
      and column_name = 'user_type'
      and data_type = 'varchar'
);

set @crehn_user_type_sql := if(
    @crehn_user_type_needs_expand = 1,
    'alter table sys_user modify column user_type varchar(32) default ''sys_user'' comment ''用户类型（sys_user系统用户）''',
    'do 0'
);

prepare crehn_user_type_stmt from @crehn_user_type_sql;
execute crehn_user_type_stmt;
deallocate prepare crehn_user_type_stmt;

select table_schema, table_name, column_name, data_type, character_maximum_length, column_default
from information_schema.columns
where table_schema = database()
  and table_name = 'sys_user'
  and column_name = 'user_type';
