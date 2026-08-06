# QA-001 修复管理端工作台页面键类型边界

## 状态

- 任务：`QA-001`
- 关联需求：`REQ-013`
- 当前状态：`IMPLEMENTED / STATIC_VERIFIED / PORTAL_NEEDS_BROWSER`
- Git提交：未授权
- 数据库执行：未授权
- 服务器连接：未授权
- 部署：未授权

## 目标

修复管理端工作台页面配置中的 `page_key` 类型边界，使全量 TypeScript 类型检查不再被该问题阻断，同时保持既有页面配置行为和稳定键契约不变。

## 范围

- 涉及模块：管理端工作台配置页面。
- 允许文件：`frontend/src/views/system/workbench/` 及其直接类型/测试。
- 数据/API/配置：不改变 HTTP API、运行时配置、数据库结构或稳定页面键；仅收紧前端类型边界。
- 联调：对 `PORTAL-001` 仅做本地真实浏览器桌面、平板、手机和 `prefers-reduced-motion` 验收，证据写入唯一新目录，不修改门户/CMS业务代码。

## 禁止范围

- 不使用 `any`、`ts-ignore`、删除检查或放宽类型来绕过错误。
- 不修改 `TASKS.md`、`docs/codex/*`、门户/CMS业务代码、`deploy/db/*` 或其他任务文件。
- 不执行 SQL，不连接服务器，不部署，不提交/推送 Git，不外部发送，不删除或清理现有文件。
- 不覆盖 `.playwright-cli/`、`output/` 或现有未提交改动。

## 验收

- [x] `frontend` 全量 `pnpm typecheck` 通过。
- [x] 工作台相关定向测试和非格式 ESLint 规则通过；默认 Prettier 仍受既有混合换行基线阻断。
- [x] 管理端生产构建使用 4 GB Node 堆通过。
- [ ] `PORTAL-001` 本地门户完成桌面、平板、手机和减少动态效果浏览器验收并保存唯一证据。
- [ ] 管理端登录态缺少本地环境或账号时标记 `NEEDS_BROWSER`，不以构建或匿名页面代替。

## 实际变更与证据

### 修改文件与原因

- `frontend/src/views/system/workbench/index.vue`：把 8 个表格页面键与 5 个工作台表头页面键显式分界；不支持表头配置的评审分配、评分汇总、签名表汇总页面禁用表头页签并自动进入表格列页签。
- `frontend/src/views/system/workbench/workbenchPageKeyBoundary.ts`：新增单一类型守卫，禁止把宽页面键联合直接传给窄表头契约。
- `frontend/src/views/system/workbench/workbenchPageKeyBoundary.test.ts`：覆盖 5 个允许表头页面与 3 个仅表格页面。
- `output/playwright/qa001-portal-20260806T1520/`：保存 PORTAL-001 本地浏览器启动失败日志和证据说明；未覆盖既有产物。

### 依赖、接口、配置和数据库

- 新增第三方依赖：无。
- HTTP API、环境变量、运行时配置、稳定页面键：无变化。
- 数据库字段、迁移 SQL、SQL 执行：无；未执行数据库操作。

### 验证结果

- `PASS`：`frontend/pnpm typecheck`，全量 `vue-tsc --noEmit` 通过；修复前唯一错误为 `index.vue:460 TS2322`。
- `PASS`：`pnpm test -- ...` 实际运行全量前端测试，16 个测试文件、94 项通过；新增边界测试 1/1 通过。
- `PASS`：新增类型守卫和测试文件默认 ESLint 通过。
- `PASS`：`index.vue` 关闭既有 `prettier/prettier` 格式规则后，其余 ESLint 规则通过。
- `FAIL（既有格式基线）`：`index.vue` 默认 ESLint 报 54 个 Prettier 项，均为混合 LF/CRLF 或既有长标签格式；未整页格式化，避免扩大并行未提交差异。
- `PASS`：定向 `git diff --check` 通过，仅提示后续 Git 触碰时 LF 将转换为 CRLF。
- `PASS`：管理端生产构建使用 `node --max-old-space-size=4096 ... vite build --mode production --configLoader runner` 通过，3449 个模块；仅有大 chunk 提示。
- `NEEDS_BROWSER`：门户 Vite 在沙箱内因配置路径访问受限未启动；本机启动审批又因 Codex 使用额度触顶被拒绝，故桌面、平板、手机和 `prefers-reduced-motion` 均未运行。详细记录见独立浏览器产物目录。
- `NEEDS_BROWSER`：管理端缺少已启动的本地服务和可用登录账号，未伪造登录态验收。
- `NOT_RUN`：数据库、服务器、部署、生产验收。

### 风险与未验证项

- 新增的边界守卫依赖 `ArtWorkspaceHeaderPageKey` 的权威联合类型；未来新增页面表头契约时需同步该守卫及测试。
- 浏览器尚未验证页面切换后的实际页签状态、门户响应式视觉和减少动态效果降级。
- 当前工作区存在其他会话并行未提交改动；本任务未覆盖、清理、重置或提交这些改动。

### 授权状态

- QA-001 代码和任务卡：已授权并已实施。
- PORTAL-001：仅浏览器验收授权；因环境审批阻断未完成，未修改门户/CMS业务代码。
- Git提交/推送、SQL、服务器连接、部署、外部发送、删除：均未授权且未执行。
