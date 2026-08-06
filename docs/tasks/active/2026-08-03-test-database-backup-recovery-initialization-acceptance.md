# 2026-08-03 TEST数据库备份、隔离恢复与初始化验收

## 状态

- 当前状态：`READONLY_PREFLIGHT_PASSED / ASSETS_RESTAGED / DATABASE_PLAN_PASSED / INITIALIZATION_SCOPE_AUDIT_COMPLETE / INITIALIZATION_DECISION_REQUIRED / BACKUP_NOT_AUTHORIZED / SQL_NOT_AUTHORIZED / DEPLOY_NOT_AUTHORIZED / PUSH_NOT_AUTHORIZED`
- 文档规划：已授权并完成
- 229只读预检：未执行
- 数据库备份、隔离重建、恢复演练和初始化：未授权、未执行
- ECS 2连接、生产操作、Git提交和推送：未授权、未执行

## 目标

在不触碰项目A、不覆盖现有证据的前提下，重新规划当前 TEST 部分初始化数据库的备份、隔离恢复、空库初始化和只读回读验收。数据库验收成功前，不扩展到应用重新部署、业务 SQL 或 ECS 2。

## 当前事实与边界

- TEST `0.1.9-test` 已部署并运行正常，运行时源码修订为 `4a7eb2a9c18396a6933e441967a6f065b6236fcb`。
- 管理员和 TEST 临时审核浏览器验收已完成；这些证据只证明应用、权限入口和页面行为，不证明数据库初始化或恢复可用。
- 数据库初始化、业务 SQL、ECS 2连接、OSS配置和Git推送均未执行。
- 当前本地静态重算：`migration-manifest.json` 为44项 `businessStructure`，`deploy/db/Dockerfile` 为47个 `/opt/crehn/init` 入口（1个框架基线 + 2个 bootstrap + 44项业务结构）；48条全部 `COPY` 另含 manifest 本身。44个 manifest 路径全部存在。
- 全量 SQL manifest 不能直接等同于初始化清单；初始化入口中包含 `V005` 和运行时配置内容，需在远端写操作前固定其分类、顺序和 SHA-256。

## 阶段A已执行证据

- 229 `PreflightLocal` 已通过，且脚本报告未改变主机：内存 `6755 MiB`、Swap `4095 MiB`、根分区剩余 `24847 MiB`、Docker `29.6.2`、Compose `5.3.1`。
- TEST 候选端口 `28181/29000/29001` 均为可用或已由 `crehn-test` 占用；项目A `dyz-current-shadow` 5 个容器均 `running/healthy`，重启次数为0且未 OOM，保护 Web 和存储入口健康。
- 历史 45 项入口（加入 V006/V007 前）分类对照结果：`ProgramInitializationSql=4`、`RequiredSchemaSql=10`、`ProgramMigrationSql=22`、`ProgramRuntimeConfigSql=2`、`BusinessRuntimeSql=1`、未登记入口=6。
- 未登记入口为两个 TEST 引导 Shell 和 `V001` 至 `V004`；需要在初始化专用 manifest 中登记来源、顺序、脚本类型和 SHA-256。
- 需要人工确认的高风险分类包括 `V005`、`art_review_config_rules.sql`、`art_review_m68_recycle_audit_scope.sql`、`art_review_m82_activity_menu_name.sql` 及多个手动 ProgramMigration；在分类决策完成前不执行初始化。
- 首次 `DatabasePlan` 未进入远端计划逻辑，因 TEST 远端入口以当前用户执行时返回退出码 `126 Permission denied`；未执行 SQL、未启动容器、未修改数据库。
- 已在本地 `deploy/scripts/invoke-deployment.ps1` 将 TEST `database` 入口纳入 `sudo -n` 包装；PowerShell AST 解析和 `git diff --check` 通过，修复已随 TEST 资产暂存。
- 当前 `deploy/env/test.env` 固定 `0.1.9-test` 与已提交修订 `4a7eb2a9c18396a6933e441967a6f065b6236fcb`；本地权限修复属于调用端提交 `aacd1a6`，已完成本地提交但未推送。
- 已获仅本地提交授权并完成提交 `aacd1a6`，只包含 `deploy/scripts/invoke-deployment.ps1`，未推送。
- TEST 资产已按 `STAGE:crehn-test:4a7eb2a9c18396a6933e441967a6f065b6236fcb` 暂存成功；入口报告未执行 build、容器或 SQL。
- `DatabasePlan` 已重试通过：Compose 配置通过，初始化来源为 `/opt/crehn/init`，入口明确报告未启动容器、未执行 SQL；后续迁移仍需审查 checksum manifest 和独立实现。

## 初始化入口本地审查结果

- 历史 45 项入口对照为：`ProgramInitializationSql=4`、`RequiredSchemaSql=10`、`ProgramMigrationSql=22`、`ProgramRuntimeConfigSql=2`、`BusinessRuntimeSql=1`、未登记入口=6。
- 未登记入口：`bootstrap-admin.sh`、`bootstrap-test-oss.sh`、`V001`、`V002`、`V003`、`V004`；必须先补充来源、顺序、脚本类型和 SHA-256，不能依赖 Dockerfile 顺序作为唯一审计来源。
- `BusinessRuntimeSql` 的 `art_review_m68_recycle_audit_scope.sql` 含运行时审核分配数据风险，不得默认纳入空库初始化。
- `ProgramRuntimeConfigSql` 的 `art_review_config_rules.sql` 与 `art_review_m82_activity_menu_name.sql` 必须作为显式运行时配置动作单独决策，不能随普通结构初始化自动执行。
- `V005__crehn_workbench_role_key.sql`、`V006__crehn_portal_page_layout.sql` 和 `V007__crehn_role_permission_boundaries.sql` 虽位于初始化镜像，仍需备份、隔离恢复、执行前快照/回读和明确 SQL 授权；本轮均未执行。
- 其余 ProgramMigration 需要逐项确认“空库初始化允许”还是“后续手动迁移”，不能因为 `DatabasePlan` 通过就自动执行。

## 现有脚本能力限制

- `backup_create` 当前主要生成应用回滚点，包含运行中数据库的逻辑 dump，但不等同于停库后的物理数据库备份，也不包含恢复演练成功标记。
- `database initialize` 只允许空库首次初始化；已有表或 `database-initialized` 标记存在时必须阻断。
- 应用回滚不自动恢复数据库或对象存储；TEST MinIO 与数据库必须分别记录恢复范围。

## 阶段A：本地计划与远端只读预检

1. 固定 `0.1.9-test` 发布清单、数据库镜像、源码修订、镜像摘要和初始化入口清单哈希。
2. 对照 SQL manifest 解决初始化入口与 `ProgramMigrationSql` / `ProgramRuntimeConfigSql` 分类差异；未解决前不得执行初始化。
3. [x] 获得单独授权后完成 229 只读预检：项目A基线、TEST端口、主机资源和保护入口均已回读；数据库容器、卷/挂载、表数量、错误标记和已有备份尚未读取。
4. [x] 使用本地权限包装修复重新暂存并执行 `DatabasePlan`；未启动容器、未执行 SQL。

## 阶段B：当前部分数据库备份

仅在取得数据库写操作和备份授权后执行：

- 生成逻辑 dump，并记录数据库、版本、时间和 SHA-256；
- 停止数据库后生成物理数据目录/卷备份；
- 保存项目专属 Compose、运行时文件、版本清单、状态文件和镜像引用；
- 旧数据库目录和错误完成标记只移动到项目专属隔离目录，不永久删除；
- 备份完成与恢复验证分开标记，不能仅凭 `SUCCESS` 和校验文件声称可恢复。

## 阶段C：隔离恢复演练

- 在独立恢复目录/实例中验证逻辑 dump 恢复；
- 在独立实例中验证物理数据恢复；
- 回读表数量、关键表、管理员账号、角色权限和审计数据；
- 确认项目A容器、网络、卷、数据库和入口未发生变化；
- 恢复演练通过后保留原始备份和隔离目录，不覆盖当前运行环境。

## 阶段D：TEST 空库初始化

仅在备份和隔离恢复证据完成、并取得明确 SQL 授权后执行：

1. 只停止/重建 `crehn-test` 数据库及必要的 CREHN 应用连接，不操作项目A。
2. 确认目标库表数量为0、无完成标记、数据库镜像与 `0.1.9-test` 清单一致。
3. 按执行前最终签字的47项初始化白名单逐项执行，记录文件名、顺序、SHA-256、退出码和实际执行数量。
4. 任一入口失败时不得写入完成标记；保留日志、容器和隔离证据，停止后续应用启动。

## 阶段E：初始化只读回读验收

- 47/47 入口执行成功，顺序和清单哈希一致；
- 完成标记包含项目、版本、源码修订、入口数量和完成时间；
- `sys_user`、管理员角色、菜单权限和必要框架表可回读；
- `sys_workbench_layout.role_key`、V005 索引、结果管理员和监督审计员配置符合预期；
- TEST 对象存储配置仅指向独立 MinIO 和 `crehn-test` 前缀，不读取或写入生产 OSS；
- 未执行测试种子、运行时业务快照或其他未列入初始化白名单的业务 SQL；
- 项目A保护基线前后稳定字段一致。

## 阶段F：后续应用验收（另行授权）

数据库阶段通过后，才重新启动同一 `0.1.9-test` 应用栈，执行 `VerifyLocal`、登录态持久化回读、管理员/临时审核浏览器复验和角色工作台数据库回读。该阶段不自动授权业务 SQL、ECS 2或Git推送。

## 验收状态

- [x] 本地初始化入口数量、路径唯一性和 Shell LF 静态核对。
- [x] 历史45项入口与 SQL manifest 分类和危险操作本地审查；当前44项业务结构/47个初始化入口已重算，但新增 V006/V007 尚未纳入完整分类和哈希签字。
- [x] 229 主机、TEST端口和项目A保护基线只读预检。
- [ ] 229 TEST 数据库容器、卷/挂载、表数量、错误标记和已有备份只读回读。
- [ ] 当前部分数据库逻辑与物理备份。
- [ ] 隔离逻辑恢复与物理恢复演练。
- [ ] 初始化白名单分类、顺序和 SHA-256 固定；当前因 manifest 分类冲突阻断。
- [ ] 人工确认空库初始化白名单与后续 ProgramMigration 分组。
- [ ] 空库47项初始化和完成标记。
- [ ] 初始化后的数据库结构、角色、配置和项目A隔离回读。
- [ ] 后续应用重新启动和浏览器复验。

## 禁止范围

- 不连接 ECS 2；
- 不执行未单独批准的业务 SQL、迁移 SQL 或测试种子；
- 不永久删除旧数据库、备份、错误标记或项目A数据；
- 不提交、推送或创建发布标签。
