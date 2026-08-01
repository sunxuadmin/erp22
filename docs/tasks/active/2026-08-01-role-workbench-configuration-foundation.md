# 2026-08-01 角色工作台配置基础实现

## 状态

- 需求：`REQ-013`
- 当前状态：`IMPLEMENTED`
- Git提交：未授权
- 数据库执行：未授权
- 部署：未授权

## 目标

复用现有工作台可视化编辑器，补齐 CREHN 全部业务角色的首页配置入口，并将角色工作台布局与系统骨架的权威持久化标识从数字 `role_id` 调整为稳定 `role_key`，为后续活动、页面和类别版本化配置提供可靠基础。

## 范围

- 涉及模块：RuoYi 系统工作台服务、管理端工作台配置、CREHN 角色迁移、数据库镜像迁移清单；
- 预计文件：工作台领域对象与服务、角色识别与组件注册、前端角色映射和测试、增量迁移 SQL、迁移清单与数据库 Dockerfile；
- 数据/API/配置：保留现有 API 路径；内部持久化改用稳定 `role_key`，迁移既有 `role_id` 数据。

## 禁止范围

- 不重做已经存在的可视化编辑器、表格列设计器和主题编辑器；
- 不修改 DYZ；不复制 DYZ 数字主键、配置数据或生产参数；
- 本任务不完成活动/页面/类别草稿发布表和公共门户编辑器；这些按 `REQ-013` 后续垂直切片实施；
- 不执行迁移 SQL，不连接服务器，不提交或推送 Git，不部署。

## 验收

- [ ] 12 个业务角色均可在管理员工作台中选择并获得合法的默认首页组件（代码与迁移已覆盖，待数据库和浏览器验证）；
- [x] `crehn_reviewer` 使用评分员工作台能力，不再依赖旧 `crehn_expert` 命名；
- [ ] 角色系统骨架和首页布局以稳定 `role_key` 持久化，数字 `role_id` 只作为当前角色查询入口（代码已实现，待迁移和回读）；
- [ ] 增量迁移可回填既有布局、迁移角色外壳配置，并补建结果管理员和监督审计员；
- [ ] 前端测试、类型检查和可执行的后端检查具有明确结果。

## 实际变更与证据

### 修改文件与原因

- `backend/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/domain/SysWorkbenchLayout.java`：增加稳定角色编码字段映射；
- `backend/ruoyi-modules/ruoyi-system/src/main/java/org/dromara/system/service/impl/SysWorkbenchLayoutServiceImpl.java`：布局和系统骨架改按 `role_key` 读写，补齐 12 角色组件覆盖及 `crehn_reviewer`，配置 JSON 损坏时返回真实错误；
- `frontend/src/views/system/workbench/index.vue`：为 12 个正式角色提供明确的紧凑标签；
- `frontend/src/views/system/workbench/components/RoleShellVisualEditor.vue`：移除预览中的 DYZ 品牌字母残留；
- `frontend/src/views/workbench/universalDashboard.ts`：增加无数据权限角色的安全通用类型，补齐正式角色映射；
- `frontend/src/views/workbench/components/UniversalDashboardWidget.vue`：通用类型不再误调用管理员统计接口；
- `frontend/src/views/workbench/universalDashboard.test.ts`：覆盖 12 角色、学校上下文和多角色优先级；
- `deploy/db/migrations/V005__crehn_workbench_role_key.sql`：增加结果管理员和监督审计员，回填布局 `role_key`，复制稳定系统骨架配置并补权限；
- `deploy/db/migration-manifest.json`、`deploy/db/Dockerfile`、`backend/script/sql/sql-manifest.json`：登记迁移顺序、数据库镜像入口、影响表、执行政策和风险；
- `TASKS.md`、`docs/requirements/CREHN_REQUIREMENTS.md`：登记实现任务并保持 `REQ-013` 为分阶段实施状态。

### 依赖、接口、配置和数据库

- 新增第三方依赖：无；
- 新增或修改 HTTP API：无，继续使用现有工作台 API；
- 新增环境变量或部署端口：无；
- 配置键变化：角色系统骨架的权威键由 `crehn.workbench.role.shell.<role_id>` 改为 `crehn.workbench.role.shell.<role_key>`；迁移保留数字键作为旧版本回滚点，新代码不读取旧键；
- 数据库变更：待执行的 V005 增加 `sys_workbench_layout.role_key` 和两个索引，补建两个角色及权限，并复制系统骨架配置；未执行 SQL。

### 检查结果

- `VERIFIED`：新增角色映射测试 15 项通过；
- `VERIFIED`：前端完整测试 11 个文件、80 项通过；
- `VERIFIED`：`vue-tsc --noEmit` 通过；
- `VERIFIED`：直接修改的通用看板、测试、组件和角色骨架编辑器 ESLint 通过；工作台主页面仅剩基线已有的第 441 行模板格式错误，未为本需求改动该无关区域；
- `VERIFIED`：两个迁移清单 JSON 可解析，V005 在数据库镜像和两个清单中各登记一次；
- `VERIFIED`：`git diff --check` 通过；
- `NOT_RUN`：本机未发现 Java 和 Maven，后端未编译；
- `NOT_RUN`：未执行 SQL、未构建镜像、未启动容器、未连接服务器；
- `NEEDS_BROWSER`：12 角色真实登录、保存回读、刷新重登和越权验证；
- `NEEDS_SERVER`：后端编译、迁移预演、事务/索引影响和真实数据库回读。

### 风险与回滚

- 新后端查询 `sys_workbench_layout.role_key`，因此 V005 是发布前置迁移；未执行迁移时不得发布该后端；
- V005 的 `NOT NULL` 和唯一索引会在存在孤儿布局或重复稳定角色布局时显式失败，必须先读回异常数据，不得自动猜测修复；
- 迁移包含 DDL，MariaDB 会隐式提交，执行前必须备份；失败时保持旧应用不发布并按实际完成语句逐项处置；
- 数字角色系统骨架配置保留，旧版本后端可以回读；稳定键配置是新版本唯一权威实现；
- 结果管理员和监督审计员的菜单权限尚未在真实数据库和浏览器中验证；
- 活动/页面/类别的草稿、发布版本和回滚机制不在本垂直切片内，`REQ-013` 仍为 `IMPLEMENTING`。

### 授权状态

- 代码和部署文件修改：已授权并已实施；
- Git提交/推送：未授权；
- SQL执行：未授权；
- 本地或生产部署：未授权；
- 服务器连接：未授权。
