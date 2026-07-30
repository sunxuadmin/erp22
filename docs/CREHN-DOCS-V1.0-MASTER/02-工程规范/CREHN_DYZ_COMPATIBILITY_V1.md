# CREHN 与 DYZ 活动兼容规范 V1

## 1. 兼容目标

CREHN 必须能够：

1. 导入 DYZ 活动配置并转换为 CREHN 标准模型；
2. 对无法转换的模块给出明确警告或阻断，不静默丢弃；
3. 在 CREHN 中独立运行导入后的活动；
4. 分批迁移 DYZ 历史作品、文件、审核、评分和结果；
5. 保留来源追踪，支持审计、回读和失败回滚。

兼容不包括共享数据库、共享业务表、实时双写或让 DYZ 前端直接调用 CREHN 内部接口。

## 2. 当前 DYZ 配置包基线

当前 DYZ `art_activity_config`、`formatVersion=1.0` 明确包含：

- 活动基础信息；
- 类别；
- 类别动态字段；
- 类别附件要求。

部分成员表、通用表格和布局可能嵌在 `ruleJson` 中，属于需要专门解析的半结构化内容。以下内容不应假定已完整包含：

- 学校范围；
- 报送规则和名额包；
- 上传规则模板和模板二进制文件；
- 业务角色和账号；
- 页面工作台、公共样式和角色外壳；
- 评审分配规则、评分表模板和签名资源；
- 奖项规则、评分汇总配置和结果；
- 门户 CMS 内容和媒体。

因此 DYZ 1.0 包只能作为输入格式之一，不能直接成为 CREHN 核心模型。

## 3. CREHN 标准活动包

包类型：`crehn_activity_bundle`。  
建议容器：ZIP，清单和数据使用 UTF-8 JSON，资源放入 `resources/`。

```text
manifest.json
activity.json
categories.json
schemas/
  fields.json
  members.json
  tables.json
  files.json
rules/
  submission.json
  quota.json
  audit.json
  review.json
  result.json
templates/
  upload-rules.json
  score-sheet.json
notifications/
  templates.json
ui/
  workbench.json
  public-style.json
portal/
  content.json
resources/
  <sha256>.<ext>
```

`manifest.json` 至少包含：

- `packageType`
- `formatVersion`
- `sourceSystem`
- `sourceVersion`
- `exportedAt`
- `packageId`
- `activityExternalKey`
- `modules`
- `capabilities`
- `resources`
- `checksums`
- `minReaderVersion`
- `maxReaderVersion`

模块能力清单至少可表达：活动范围、名额/报送规则、上传规则模板、成员与通用表格、审核/评审范围、评分预警、评分表模板、结果/奖项规则、通知模板、工作台/页面样式和门户内容。包未声明的模块视为“不包含”，不能被导入器解释为“清空目标模块”。

## 4. 稳定编码

跨系统关联使用业务编码，不使用数字主键：

- `competition_code`
- `edition_code`
- `activity_code`
- `category_code`
- `field_key`
- `member_type_code`
- `table_key`
- `file_requirement_code`
- `school_code`
- `role_key`
- `rule_code`
- `resource_key`
- `template_code`
- `notification_template_code`

每个编码说明大小写、长度、允许字符和活动内唯一范围。编码一旦被正式提交数据引用，默认不可修改，只能通过别名映射迁移。

## 5. 适配器边界

兼容模块结构：

```text
compatibility
  adapter
    DyzActivityPackageV1Adapter
  contract
    ActivityBundleReader
    ActivityBundleWriter
  mapping
    SchoolMappingService
    RoleMappingService
    ResourceMappingService
  importjob
    PreviewService
    ApplyService
    ReadbackService
```

适配器输出 CREHN 标准 DTO，不能返回 DYZ 实体。核心业务模块不能引用 `Dyz*` 类、DYZ 表名或 DYZ API 路径。

## 6. 导入模式

| 模式 | 行为 | 默认 |
|---|---|---|
| `CREATE_NEW` | 新建活动并建立全部映射 | 首选 |
| `MERGE_ADDITIVE` | 只新增不存在的稳定编码 | 可用，必须预览 |
| `UPDATE_SAFE` | 更新尚未被历史数据引用的配置 | 受限 |
| `FULL_SYNC` | 更新、删除并与来源一致 | V1 禁用 |

任何覆盖、删除或编码重映射都不能由普通增量导入完成。

## 7. 导入流水线

1. 上传文件并检查大小、扩展名、MIME、ZIP 路径、压缩比和病毒风险；
2. 校验清单、包类型、格式版本、模块能力和所有 SHA-256；
3. 解析到来源 DTO；
4. 通过版本适配器转换为 CREHN 标准 DTO；
5. 校验稳定编码、引用关系、JSON Schema 和资源；
6. 解析学校、角色和模板资源映射；
7. 生成新增、更新、跳过、冲突、未知和阻断差异；
8. 用户确认导入模式和差异；
9. 在导入批次事务中写入；
10. 服务端重新读取并比较关键指纹；
11. 写入导入审计和来源映射；
12. 失败时回滚数据库并清理本批次未引用资源。

## 8. 幂等和来源追踪

所有导入实体至少保存：

- `source_system`
- `source_record_key`
- `source_format_version`
- `source_checksum`
- `import_batch_id`
- `imported_at`
- `legacy_extension_json`

`package_id + checksum + target_activity_id + import_mode` 应形成幂等约束。重复导入同一包返回已有结果，不生成重复活动或资源。

## 9. 未知内容处理

- 未知可选字段保存在 `legacy_extension_json` 并产生警告；
- 未知必需模块、状态、规则或资源产生阻断；
- 不支持的 DYZ 字段类型不能自动降级为普通文本；
- 无法映射的学校、角色和资源必须由用户选择映射或跳过整个依赖对象；
- 导入报告必须可下载和追踪。

## 10. 资源迁移

- 包中携带模板和媒体实体，使用内容 SHA-256 标识；
- 禁止导入 DYZ OSS ID、bucket 密钥或临时签名 URL；
- 相同内容可以去重，但必须保留逻辑资源版本；
- 每个资源在业务对象写入前先通过文件安全校验；
- 包导入失败时只清理由该批次创建且未被其他对象引用的资源。

## 11. 历史数据迁移

历史迁移与活动配置导入分开执行，顺序为：

1. 学校编码映射；
2. 账号和参与者映射；
3. 活动和配置版本；
4. 作品、成员、表格和附件；
5. 学校提交与审核记录；
6. 审核和评审分配；
7. 评分、签名评分表；
8. 结果版本、奖项和发布记录；
9. 消息和审计附件。

规则：

- 不迁移密码、令牌、验证码、密钥或会话；
- DYZ 学校代填历史作品标记为 `legacy_school_owned`，不得伪造参赛者本人确认；
- 已签评分表和已发布结果默认只读迁移；
- 无法证明状态或来源的数据进入隔离区，不直接进入正式流程；
- 文件实体和元数据都验证成功后，业务记录才标记迁移完成。

## 12. 合同测试

至少维护以下固定样例：

- DYZ 1.0 最小活动包；
- 完整类别、字段和附件活动包；
- 包含成员表和通用表格 `ruleJson` 的活动包；
- 重复编码、非法 JSON、缺失资源和校验值错误包；
- 新版本未知字段包；
- 同一包重复导入；
- 增量预览不写入；
- 中途失败事务回滚；
- 导入后全流程样例：填报、学校提交、审核、评分和结果。

每次兼容适配器或标准活动包变更必须运行合同测试。

## 13. 明确禁止

- CREHN 直接查询或更新 DYZ 数据库；
- CREHN 业务表保存 DYZ 数字主键作为唯一身份；
- DYZ 与 CREHN 共用 Redis key、对象存储前缀或生产密钥；
- 导入时静默覆盖目标活动；
- 未经预览执行全量同步；
- 把数据库 dump 当作日常活动兼容包；
- 将未知字段直接丢弃后仍报告完全兼容。
