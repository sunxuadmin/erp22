# CREHN 数据库设计 V1.0

## 1. 设计原则

- 每类权威数据只有一个写入所有者。
- 内部使用独立主键，跨系统兼容使用稳定业务编码。
- 核心状态和查询字段使用结构化列；扩展配置可使用有版本的 JSON。
- 历史提交、签名和发布数据使用不可变快照。
- 逻辑删除、永久删除和业务状态分离。
- 所有业务表继承统一用户、审计、版本和逻辑删除规范。
- CREHN 不复用 DYZ 表、数字 ID、用户 ID 或 OSS ID。

## 2. 核心领域

| 领域 | 主要实体 |
|---|---|
| 身份 | user、participant_profile、activation_batch、activation_code、account_audit |
| 学校 | school、school_contact、school_account、school_field_schema |
| 赛事 | competition、competition_edition、activity、activity_stage |
| 配置 | activity_config_version、category、field_schema、member_schema、table_schema、file_requirement |
| 范围与规则 | activity_school_scope、submission_rule、quota_rule、review_rule、award_rule |
| 作品 | project、project_snapshot、project_member、project_table_data、project_declaration |
| 文件 | file_object、file_version、file_binding、file_preview、file_security_scan |
| 学校提交 | school_submission_batch、school_submission_item |
| 审核 | audit_assignment、formal_review_record |
| 评审 | expert_profile、review_assignment、review_score、score_sheet_template、signed_score_sheet |
| 结果 | result_version、result_item、result_log、showcase_order |
| CMS | portal_site、channel、article、article_version、media_asset、home_component、portal_release |
| 支撑 | message、notice_template、export_job、import_batch、source_mapping、operation_audit |
| 主题与工作台 | theme_version、role_shell_config、workbench_layout_version、page_display_config |

## 3. 通用字段

核心表按需包含：

- `id`
- `tenant_id`
- `created_by`
- `created_at`
- `updated_by`
- `updated_at`
- `row_version`
- `del_flag`
- `status`

兼容或迁移实体按需包含：

- `source_system`
- `source_record_key`
- `source_format_version`
- `source_checksum`
- `import_batch_id`
- `legacy_extension_json`

## 4. 唯一来源

| 数据 | 权威来源 | 禁止做法 |
|---|---|---|
| 学校名称和代码 | `school` | 在账号、作品表维护可编辑学校名称 |
| 参赛者当前资料 | `participant_profile` | 学校代填后不经本人确认即作为当前资料 |
| 历史作品署名 | `project_snapshot` | 参赛者改名后覆盖历史署名 |
| 活动配置 | 已发布 `activity_config_version` | 前后端和 SQL 各存一份不同规则 |
| 正式评分 | 有效已提交 `review_score` 版本 | 从页面显示值或临时草稿生成结果 |
| 签名评分表 | `signed_score_sheet` 快照 | 替换签名后重写历史表 |
| 已发布结果 | 锁定 `result_version` | 发布时再次计算并产生漂移 |
| 文件元数据 | `file_object/file_version` | 在多个业务表保存可变 URL |

## 5. JSON 使用规则

适合 JSON：

- 动态字段值；
- 有明确 Schema 版本的配置；
- 不直接用于高频关联的扩展属性；
- 不可变快照。

不适合 JSON：

- 学校、活动、类别、作品、账号的核心关联；
- 权限和数据范围；
- 高频状态筛选；
- 必须唯一或建立外键的稳定编码；
- 需要精确统计和索引的核心数值。

每个 JSON 必须有版本、服务端校验和大小上限。禁止用字符串模糊查询代替长期稳定索引。

## 6. 约束和索引

- 稳定编码在定义范围内唯一。
- 激活码仅保存哈希，建立状态和有效期索引。
- 同一参赛者、活动和类别的作品唯一规则按活动配置建立业务约束。
- 同一专家、作品和有效分配的正式评分使用唯一约束。
- 同一专家、活动和类别只允许一个有效签名评分表。
- 导入包和导入批次具有幂等唯一约束。
- 学校、活动、类别、状态、时间和删除标记建立组合索引。

## 7. 并发和事务

- 草稿保存使用乐观锁，冲突返回当前服务端版本。
- 最终提交、签名、结果生成和发布使用事务及唯一约束。
- 异步文件转换和导出任务通过状态机恢复，不与数据库事务假装原子。
- 批量操作记录逐项结果和整体批次状态。

## 8. 删除策略

- 业务删除默认逻辑删除并进入回收站。
- 历史引用存在时禁止永久删除。
- 文件逻辑删除与对象物理清理分离。
- 永久清理使用后台任务，检查引用、保留期和审计要求。

## 9. 数据库实现前置产物

建表前必须先完成：

- 领域关系图；
- 状态机；
- 稳定编码规范；
- 唯一约束和索引清单；
- 配置/快照边界；
- DYZ 字段映射；
- 数据保留和删除策略；
- 迁移、回滚和验收方案。
