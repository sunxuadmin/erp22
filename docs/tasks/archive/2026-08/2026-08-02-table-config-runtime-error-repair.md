# REQ-014 表格配置编辑器初始化运行时错误修复

## 状态

- 需求：`REQ-014`
- 当前状态：`ARCHIVED / VERIFIED / SUPERSEDED_BY_0.1.9`
- Git提交：已授权并完成，`345c3c86ac2f451013c726cd04492dbe410161db`
- 数据库执行：未执行（本任务无 SQL）
- 部署：已授权并完成 TEST `0.1.7-test`

## 目标

修复管理端表格配置编辑器在配置接口返回前和页面键同步瞬态期间出现的 `undefined.layout` 与 `undefined.map` 运行时错误，保证配置页可以安全加载并继续受控浏览器验收。

## 范围

- `frontend/src/views/crehn/components/ArtGlobalTableVisualEditor.vue`
- `frontend/src/composables/artListTableRuntime.ts`
- 本任务卡及 `TASKS.md` 索引

## 禁止范围

- 不修改后端接口、权限、数据库、部署资产或运行时配置数据；
- 不保存或改变任何表格配置；
- 不执行本任务范围外的 SQL 或业务数据修改；
- 不推送 Git。

## 验收

- [x] 本地 TypeScript/Vue 静态检查通过；
- [x] 表格编辑器初始化期间不再读取未定义的布局或动作槽位；
- [x] 浏览器重新打开“页面类别 → 表格列”无上述两个运行时错误；控制台 0 errors，仅有 Element Plus radio label/value 弃用警告；
- [x] 受限角色浏览器验收：临时账号 `crehn_auditor_temp` 可登录并进入项目审核工作台；仅显示审核菜单；直接访问 `/admin/system/workbench` 返回前端 404，审核页无组件添加、布局配置或保存入口；未保存业务配置。

## 授权状态

- 代码修改：本轮已授权；
- 服务器、构建、部署：已按授权执行 TEST `0.1.7-test`；
- 数据库：未初始化、未执行 SQL；
- Git提交：已完成上述修复提交；推送：未执行。

## 本地验证证据

- `frontend`: `pnpm typecheck` 通过；
- `frontend`: `pnpm test` 通过，11 个测试文件、80 个测试全部通过；
- 仓库：`git diff --check` 通过；
- 229 构建/部署：`0.1.7-test` 镜像构建、暂存、部署、VerifyLocal 均通过；所有 TEST 服务健康，项目 A 保护基线健康；
- 229 浏览器回归：管理员“页面类别 → 表格列”加载无 `undefined.layout`/`undefined.map`；控制台 0 errors、4 个 Element Plus 弃用警告；
- 受限角色：通过管理端创建 `crehn_auditor_temp`（昵称“TEST临时审核”、角色“节目审核员”），登录后进入项目审核工作台；工作台配置路由不可访问（404），无配置保存行为；当前审核页控制台 0 errors、0 warnings。
