# DYZ SQL 文件说明与部署规则

本目录里的项目 SQL 必须先分清用途，再决定是否进入普通部署。

## SQL 分类

- `RequiredSchemaSql`：后端运行前必需的结构 SQL。只允许幂等、向后兼容地新增表、字段、索引或约束；禁止写入业务数据、菜单、角色、权限和系统配置。菜单 `8` 会先在本地执行，并在阿里云备份后按文件路径和 SHA-256 幂等核对/执行，成功后才切换后端 jar。
- `ProgramMigrationSql`：非结构程序迁移 SQL。用于菜单、权限、角色授权、系统配置等随代码版本升级需要的幂等变更。菜单 `6` 会与 `RequiredSchemaSql` 一起执行；阿里云只自动处理其中 manifest 明确标记 `aliyunAutoApply=true` 的受控迁移。
- `ProgramRuntimeConfigSql`：程序运行配置/模板 SQL。用于活动模板、上传规则、通知模板、申报模板、工作台默认配置等。不能随普通上传更新自动执行。
- `ProgramInitializationSql`：程序初始化 SQL。用于新服务器或空库初始化。不能随普通上传更新执行。
- `BusinessRuntimeSql`：业务运行数据 SQL。会修改活动、类别、项目、附件、评审、账号、工作台布局等运行期数据或业务配置。必须单独确认、先备份再执行。
- `TestSeedSql`：测试种子 SQL。只用于测试数据，不得进入生产普通部署。
- `FrameworkVendorSql`：框架原始/升级 SQL。按框架升级流程单独审查。

## 必填说明

新增或修改项目 SQL 时，必须同步更新 `sql-manifest.json`。至少填写：

- `path`：SQL 相对仓库根目录路径。
- `category`：上面的分类之一。
- `summary`：这个 SQL 做什么。
- `touches`：会影响哪些表。
- `deployPolicy`：是否允许普通部署，还是需要单独确认。
- `risk`：可能影响什么业务数据或配置。

历史 SQL 如果没有采用 `art_review_m<版本>_*.sql` 命名，但必须插入既有版本迁移之间，可额外填写数值型 `migrationOrder`。它只控制执行顺序，不改变迁移路径或 SHA-256；普通迁移继续按文件名版本自动排序。

紧凑文件名在双位主版本时存在天然歧义，例如 `m110` 可能被误解为 M1.10 或 M11.0。因此 M10.0 及之后的迁移必须在 manifest 显式填写 `migrationOrder`，数值采用 `主版本 * 100000 + 次版本`，例如 M10.8 为 `1000008`、M11.3 为 `1100003`。分类器会拒绝缺少该字段的 M10+ 紧凑版本文件，避免后续菜单或权限迁移被错误重排。

`ProgramMigrationSql` 可以额外填写布尔值 `aliyunAutoApply`。默认不填写或填写 `false`，仍由人工单独处理；只有同时满足以下条件时才能设为 `true`：

- SQL 使用明确的 `START TRANSACTION` / `COMMIT` 边界，并且可以安全重复执行。
- 文件必须位于 `framework-ruoyi-vue-plus/script/sql/`，文件名只能使用安全的字母、数字、点、下划线和连字符。
- 只做随程序版本必需的增量菜单、权限或角色授权写入，不包含结构 DDL。
- 不包含 `DELETE`、`TRUNCATE`、`DROP`、`ALTER`、`RENAME`、`CREATE`、`REPLACE`、`GRANT`、`REVOKE`、`CALL` 或外部数据导入。
- 不修改活动、类别、项目、评审、账号、附件、运行配置、初始化数据或测试数据。

该标记是阿里云全量发布的显式白名单，不是整个 `ProgramMigrationSql` 分类的自动开关。部署脚本还会再次执行分类、manifest、事务边界、危险语句和业务表写入检查。

如果新增功能 SQL 没有写入 `sql-manifest.json`，部署脚本在自动发现或显式执行时会拦截，避免提示里只显示文件名却不知道影响范围。

## 执行原则

- 普通程序迁移：只允许 `RequiredSchemaSql` 和 `ProgramMigrationSql`。
- 阿里云后端发布：必须传入 `-ApplyRequiredSchemaSql`，在数据库备份成功后核对并幂等执行全部已登记的 `RequiredSchemaSql`，再切换 jar；同路径不同 SHA-256 会阻断并要求新建迁移文件。
- 受控程序迁移：传入 `-ApplyProgramMigrationSql` 后，只处理 `aliyunAutoApply=true` 的 `ProgramMigrationSql`。未记录项执行成功后写入 `art_deploy_program_migration_history`；同路径同哈希以后跳过执行并刷新验证时间，同路径不同哈希阻断并要求新建迁移文件。
- 未标记的 `ProgramMigrationSql`、运行配置、业务数据、初始化或测试种子 SQL 都不会自动执行；检测到这些变更时仍必须确认生产库已单独处理或本次无需处理。
- 运行配置、业务数据、初始化、测试种子 SQL：不要混入普通部署清单。
- 执行业务运行数据或运行配置 SQL 前，必须备份目标数据库。
