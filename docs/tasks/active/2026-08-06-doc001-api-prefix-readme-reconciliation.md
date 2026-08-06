# DOC-001 统一 API 前缀和模块 README

## 状态

- 当前状态：`IMPLEMENTED / STATIC_VERIFIED`
- Git提交：未授权
- 数据库执行：未授权
- 服务器连接：未授权
- 部署：未授权

## 目标

以实际 CREHN 控制器、前端环境配置、Web 反向代理和工程规范为依据，消除 `/api/v1`、`/crehn/*`、`/prod-api/`、`/admin/` 与门户 `/crehn/` 在正式文档中的语义混用，不修改任何实际 API 或部署实现。

## 范围

- `docs/CREHN-DOCS-V1.0-MASTER/04-后端/CREHN_BACKEND_ARCH_V1.0.md`：修正与实际控制器冲突的 API 前缀。
- `backend/ruoyi-modules/ruoyi-crehn/README.md`、`frontend/src/api/crehn/README.md`：补充后端路由与浏览器代理前缀的区别。
- `frontend/README.md`：按当前 `package.json`、锁文件、分支约定和生产环境配置修正 pnpm、main、测试/构建及 `/admin/`、`/prod-api/` 口径。
- `portal/README.md`：明确门户 `/crehn/` 是公共页面上下文，不是后端 API 前缀，并说明相对静态产物的部署约束。
- 数据/API/配置：仅修订说明；不新增或修改接口、依赖、环境变量、数据库结构与部署路径。

## 禁止范围

- 不修改 `TASKS.md`、`docs/codex/*`、其他任务卡。
- 不修改 `backend/`、`frontend/`、`portal/`、`deploy/` 下的业务代码、环境配置、构建脚本或迁移资产；仅允许上述模块 README。
- 不执行 SQL、服务器连接、部署、Git提交/推送、外部发送、删除、清理、reset、checkout 或 clean。
- `PORTAL-001`、`DB-003`、V006、回滚、清单与 Dockerfile 仅做只读发布前审查。

## 验收

- [x] MASTER 不再把当前 CREHN 业务 API 写成 `/api/v1`。
- [x] 文档分别说明后端控制器路径 `/crehn/<module>/...`、浏览器代理前缀 `/prod-api/`、管理端页面上下文 `/admin/` 和公共门户页面上下文 `/crehn/`。
- [x] 管理端 README 使用仓库实际 `pnpm` 命令、`main + feature/REQ-* + 发布标签` 约定和现有测试/构建脚本。
- [x] 文档一致性检索、Markdown 差异检查和 `git diff --check` 通过。
- [x] PORTAL-001 与 V006 只读审查按严重级别输出；数据库、浏览器、服务器、生产证据分别标记。

## 实际变更与证据

### 文档修改

- 后端架构 MASTER：将当前 API 口径由错误的 `/api/v1` 改为实际 `/crehn/<module>/...`，保留未来大版本必须独立迁移的门禁。
- `ruoyi-crehn` 与管理端 API README：区分 Controller 路径、浏览器 `/prod-api/` 代理前缀、管理端 `/admin/` 和门户 `/crehn/` 页面上下文。
- 管理端 README：将上游 `npm`、`ts/dev` 分支和旧构建说明修正为当前 `pnpm`、`main + feature/REQ-* + 发布标签`、测试/类型检查/生产构建命令。
- 门户 README：明确 `/crehn/` 为目标页面上下文，并记录相对静态资源必须部署到同一上下文的约束。
- 未新增依赖、接口、配置、数据库或部署变更；未修改实际 API。

### PORTAL-001 只读审查发现

- `P1 / RELEASE_BLOCKER`：`portal` 以 `--base ./` 生成相对资源，构建产物有4处 `./assets`/`./config.js` 引用；`deploy/web/Dockerfile` 却将门户 `dist` 复制到 Nginx 根目录，Nginx 没有 `/crehn/` 静态映射。按目标 `/crehn/` 访问时，`/crehn/assets/*` 和 `/crehn/config.js` 不能由现有资产位置直接命中。业务/部署修复不在 DOC-001 写入范围内。
- `P2`：布局“先全部取消 active、再启用目标行”只依赖应用事务，V006 没有保证同一租户/站点/页面只有一个 active 布局；并发切换可能形成多个 active，未指定布局发布时使用无确定顺序的 `limit 1`。
- 定向布局草稿测试使用本地 `vitest.cmd` 运行，1个文件、4个测试全部通过；门户 `pnpm build` 通过，Vite 保留 `config.js` 非 module 提示。

### DB-003 / V006 只读审查发现

- `P1 / RELEASE_BLOCKER`：rollback 仅执行 `drop table if exists portal_page_layout`，会直接丢失全部布局草稿；与 MASTER“兼容新增结构通常保留、应用回滚不自动回滚数据库”冲突。未取得备份、恢复演练和 SQL 授权前不得执行。
- `P2`：V006 只记录了文件头注释和 manifest 路径，未在资产中提供 MASTER 要求的分类、前置版本、锁表/数据量风险、验证 SQL、生产自动执行策略及前向修复说明；DB-003 仍需在 DB-001 后补齐迁移计划。
- 清单静态计数可自洽：`businessStructure=43`、framework baseline 1、bootstrap 2，对应 Docker 初始化 COPY 46；43个业务结构路径均存在。V006 与 rollback 均为 LF-only，并记录本轮 SHA-256，但未执行数据库语法校验或真实迁移。

### 验证与授权边界

- `STATIC_VERIFIED`：21个 CREHN Controller 使用 `/crehn` 类级路径，0个使用 `/api/v1`；生产环境配置为 `/admin/` 与 `/prod-api`；文档定向 `git diff --check` 通过。
- `BUILD_VERIFIED`：门户 `pnpm build` 通过；这不证明 `/crehn/` 部署映射、浏览器行为或服务器运行。
- `NEEDS_BROWSER`：管理端登录态保存/切换/发布，以及门户桌面/平板/手机和 `/crehn/` 实际加载均未执行。
- `NOT_RUN / SQL_NOT_AUTHORIZED`：V006、rollback、数据库回读、迁移历史和验证 SQL均未执行。
- `NEEDS_SERVER / PROD_NOT_AUTHORIZED`：未连接 TEST/ECS 2，未部署，未做生产验收。
- Git提交/推送、外部发送、删除与清理均未授权、未执行；中央任务板与 `HANDOFF.md` 由 `00-项目总控调度` 回写。

## 00 总控后续回写

- 上述 `/crehn/` 静态映射阻断已在 `deploy/web/Dockerfile` 和 Nginx 配置中修复；仍未做容器/服务器运行验证。
- V006 已增加活动布局唯一性、稳定查询顺序、迁移元数据和非破坏性回滚说明；SQL 未执行。
- 加入 V007 后当前口径更新为44项 `businessStructure` / 47个 init COPY；原43/46只作为本卡审查时点的历史证据。
