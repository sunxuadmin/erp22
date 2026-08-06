# CREHN 新会话交接

> 快照：2026-08-06。开始工作前重新运行下列只读命令；本文件不是实时锁。

## 当前 Git 状态

- 分支：`main`
- 上游：`origin/main`（本地 Synology 裸仓库）
- 相对上游：当前 `HEAD` 领先 35 个提交、落后 0 个（本轮只读核对）
- 远端：`origin` 为本地 Synology 裸仓库；`github` 为 GitHub HTTPS 远端。
- 工作区：有未提交修改和未跟踪文件。不得 reset、checkout、clean、覆盖或删除。

### 已修改的跟踪文件

- `TASKS.md`（原有门户任务索引 + 本次总控任务索引）
- `backend/.../cms/controller/PortalCmsController.java`
- `backend/.../cms/service/IPortalCmsService.java`
- `backend/.../cms/service/impl/PortalCmsServiceImpl.java`
- `deploy/db/Dockerfile`
- `deploy/db/migration-manifest.json`
- `frontend/package.json`、`frontend/pnpm-lock.yaml`
- `frontend/src/api/crehn/cms.ts`
- `frontend/src/views/crehn/cms/index.vue`
- `portal/src/App.vue`、`portal/src/components/PortalContent.vue`
- `portal/src/config/entries.ts`、`portal/src/main.ts`、`portal/src/styles/global.css`
- FLOW-001：参赛者所有者校验相关后端服务，学校提交/工作台页面。
- ACTIVATE-001/UX-001：参赛者激活API/页面、活动与学校选项权限及三个业务页。
- PORTAL-003：结果公开查询服务/控制器与门户展示。
- DOC-001/DOC-002：模块 README、后端架构 MASTER 和需求证据分列。

### 未跟踪文件/目录

- `.playwright-cli/`（大量浏览器测试 YAML）
- `output/`（Playwright 截图/产物）
- `backend/.../cms/domain/PortalPageLayout.java`
- `backend/.../cms/mapper/PortalPageLayoutMapper.java`
- `backend/.../cms/service/impl/PortalHomeConfigurationValidator.java`
- `deploy/db/migrations/V006__crehn_portal_page_layout.sql`
- `deploy/db/rollback/V006__crehn_portal_page_layout.rollback.sql`
- `docs/requirements/CREHN_PORTAL_HOME_DESIGN_QUESTIONNAIRE.md`
- 两张 2026-08-05 门户活动任务卡
- 本次 `docs/codex/` 与 2026-08-06 总控任务卡
- `frontend/src/views/crehn/cms/portalLayoutDraft.ts` 及其定向测试
- `deploy/db/migrations/V007__crehn_role_permission_boundaries.sql` 及回滚资产
- 激活预览 BO/VO、公开结果 VO
- QA-001、FLOW-001、AUTH-001、ACTIVATE-001、UX-001、PORTAL-003、DOC-001/DOC-002 任务卡
- `frontend/src/views/system/workbench/workbenchPageKeyBoundary.ts` 及测试

## 最近完成且有边界的内容

- CREHN 独立工程、MASTER 规范、需求/任务卡治理基础已建立。
- Synology 裸仓库 `origin/main` 已建立；GitHub 为独立远端。后续推送仍需授权。
- TEST `0.1.9-test` 已部署运行，管理员和临时审核角色完成局部浏览器验收。
- 业务页面表格配置运行时、稳定 `role_key` 代码基础、选择性评分模式第一批已实现。
- 门户主题/布局和液态玻璃/粒子增强已在当前工作区实现；布局快照串写、无效布局静默发布和服务端配置校验问题已修复并完成静态验证，但仍是 REVIEW，不是浏览器/数据库/服务器验收。
- QA-001 已恢复管理端全量 typecheck 基线；16个测试文件/94项通过，4GB生产构建已有通过证据。
- FLOW-001、ACTIVATE-001、UX-001、PORTAL-003 已实施并通过前端/门户静态验证；后端本机编译与真实数据/浏览器仍未验。
- AUTH-001 已准备 V007，当前数据库口径为44项业务结构/47个 init COPY；SQL未授权、未执行。
- DOC-001 已统一API/页面前缀文档；DOC-002 已将REQ-001至REQ-014的静态/构建、浏览器、数据库、服务器和生产证据分列。

## 当前正在做什么

1. 当前代码审查任务：`PORTAL-001`、`FLOW-001`、`AUTH-001`、`ACTIVATE-001`、`UX-001`、`PORTAL-003`；静态实现已完成，后端/浏览器/数据库证据按任务卡继续。
2. 当前未提交业务任务：门户主题/布局/GridStack/CMS快照/V006，权限边界/V007，激活核对、稳定选择器和公开结果。
3. 门禁未完成任务：TEST数据库备份/隔离恢复/初始化，V005/V006/V007回读，REQ-014产品契约，REQ-013全页面/12角色验收，多角色当前上下文，DYZ选择性迁移。

## 下一步（按顺序）

1. `10-集成联调` 和 `90-审查与发布` 已建立；第二轮因 Codex 额度截止至 2026-08-08 11:44 未执行。恢复后先做后端编译/拒绝矩阵与发布前复审。
2. 需用户产品裁决 `SCORE-001`（comment_only 排名/奖项/发布/导出规则）、`AUTH-002`（同账号多角色当前上下文）和 `PORTAL-002`（门户视觉问卷）。
3. 只有获得数据库专项授权后，才执行备份、隔离恢复、47项初始化及V005/V006/V007回读。服务器、部署、Git仍各自另行授权。

## 必须先阅读

1. `AGENTS.md`
2. `TASKS.md`
3. `docs/codex/PROJECT_STATUS.md`
4. `docs/codex/TASK_BOARD.md`
5. 与所选任务对应的 `docs/tasks/active/` 任务卡
6. 与所选任务直接相关的 `docs/CREHN-DOCS-V1.0-MASTER/` 专题规范
7. `docs/codex/DECISIONS.md`

## 必须先运行的只读命令

```powershell
git status --short
git branch --show-current
git log --oneline -20
git diff --stat
```

如果任务涉及当前未提交文件，再运行定向 `git diff -- <path>`；不要用会改写工作区的 Git 命令。

## 已知高风险

- FLOW-001/AUTH-001 只有代码/资产证据；后端未编译，V007未执行，真实角色越权风险尚未用运行证据关闭。
- 数据库当前为44项业务结构/47个 init COPY；执行前仍须分类、SHA-256、备份与隔离恢复。
- V005/V006/V007 均未执行，脚本/构建通过不能作为数据库证据。
- 管理端全量 typecheck 已通过，但不代表12角色浏览器、服务端权限或数据库验收。
- 当前门户改动未提交且含新增运行时依赖 `gridstack`；相关历史会话暂不能归档。
- 公开结果查询链路、多角色当前角色切换未在静态检索中确认。

## 禁止事项与授权状态

- 业务代码：用户已授权推进统一任务板的未完成任务；本轮已实施 PORTAL-001/FLOW-001/AUTH-001/ACTIVATE-001/UX-001/PORTAL-003，具体范围以任务卡为准。
- 数据库/SQL：未授权。
- 服务器连接、部署、生产验收：未授权。
- Git提交、推送、标签：未授权。
- 删除/清理文件或会话：未授权。
- 外部发送：未授权。
- `git reset`、`git checkout`、`git clean`：禁止。

## 尚未验证

- `NEEDS_BROWSER`：当前门户主题/布局/性能、完整 12 角色、激活/填报/审核/评审/结果全链路。
- `NEEDS_SERVER`：完整 TEST 容量、数据库恢复、定时备份、ECS 2 和 PROD。
- `STATIC_VERIFIED`：管理端全量 typecheck、16文件/94测试、目标 ESLint、门户 typecheck/构建、manifest JSON/44路径、`git diff --check` 通过；管理端4GB构建已有通过证据。
- `NOT_RUN`：后端本机缺少 Java/Maven，未执行模块编译；SQL、部署和生产验收未运行。

## 历史会话可见范围

已读取最近 50 个任务索引及 5 个 CREHN 相关任务的近期摘要或部分回合；未读取到全部历史会话。任何未进入仓库文档、任务卡或 Git 的旧结论都必须重新核实。
