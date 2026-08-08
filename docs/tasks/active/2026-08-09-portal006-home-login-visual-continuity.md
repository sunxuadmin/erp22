# PORTAL-006 首页登录直达与登录页视觉连续性

## 状态

- 需求：用户 2026-08-09 确认“首页点击登录直接进入登录界面”，并要求登录界面延续首页风格。
- 当前状态：`IMPLEMENTED / STATIC_VERIFIED / BUILD_BLOCKED_LOCAL_DEPS / NEEDS_BROWSER / NOT_DEPLOYED`
- Git 提交：未授权
- Git 推送：未授权
- 数据库执行：未授权
- 部署/服务器连接：未授权

## 目标

公共首页顶部“登录”在当前窗口直接进入管理端登录页；登录页默认展示统一账号表单，并根据来源首页版本保持 V1/V2 视觉连续性。登录成功后的角色工作台和权限仍由既有账号权限决定。

## 允许修改范围

- `portal/src/components/competition-home/CompetitionHomeCanvas.vue`
- `frontend/src/views/login.vue`
- `frontend/src/config/loginPage.ts`
- `backend/ruoyi-modules/ruoyi-crehn/src/test/contract/portal006-home-login-contract.ps1`
- 本任务卡及 `TASKS.md` 活动索引（索引已建立；本次仅回填本卡）

## 已确认交互与视觉契约

- 首页顶部登录按钮同窗口跳转至 `/admin/login?redirect=/index&homeVersion=v1|v2`，保留当前首页版本。
- 登录页默认直接展示统一登录表单，不再要求先选择“上报/管理/专家”入口；登录后仍由角色权限决定菜单和工作台。
- `homeVersion` 仅接受 `v1`/`v2` 白名单值，非法或缺失值回退 V1；内部 `redirect` 仅接受安全的站内路径，拒绝协议相对 URL 和外部地址。
- 从 V1 首页进入时使用液态玻璃、紫蓝珊瑚渐变和玻璃卡片语言；从 V2 首页进入时使用明亮网格、蓝紫艺术科技语言。
- 直接访问 `/admin/login` 默认采用 V1；品牌或返回首页回到 `/crehn/?version=v1|v2` 并保留来源版本。
- 登录页品牌文案统一为“创意河南”及已确认赛事副标题，使用代码原生 `CH` 标识；旧图片 Logo、旧 `.brand-logo` 样式和旧 `html.dark` 覆盖已移除。验证码、记住账号、注册、登录 API 与权限逻辑保持不变。

## 禁止范围与边界

- 不修改登录 API、权限模型、角色菜单、数据库、迁移、配置密钥或部署资产。
- 不新增第三方依赖、公共抽象或未确认的兼容分支。
- 不执行 SQL、服务器连接、构建部署、Git 提交/推送或影响 `dyz-current-shadow` 的操作。
- 保护所有既有及并行脏改动；禁止 `git reset`、`git checkout`、`git clean` 和批量覆盖。

## 验收契约

- [x] V1/V2 首页顶部登录同窗口直达管理端登录页并直接显示统一表单。
- [x] `homeVersion` 白名单、`/crehn/?version=...` 回跳、安全内部 `redirect`、代码原生 `CH` 标识和 V1/V2 主题契约已实现；旧 dark 覆盖已移除。
- [x] 定向静态契约 `portal006-home-login-contract.ps1`：`PASS`。
- [x] 目标文件 `git diff --check`：`PASS`（仅 CRLF 提示，无空白错误）。
- [ ] 1440、1024、390 视口布局及 `prefers-reduced-motion`：`NEEDS_BROWSER`。
- [ ] 真实账号登录、角色权限和登录态行为：`NEEDS_BROWSER`。

## 验证计划与证据

| 证据层级 | 状态 | 说明 |
| --- | --- | --- |
| 静态/定向检查 | `PASS` | `portal006-home-login-contract.ps1` 通过；`git diff --check` 通过（仅 CRLF 提示）。 |
| 构建 | `BLOCKED_LOCAL_DEPS / NOT_RUN` | `frontend/node_modules` 与 `portal/node_modules` 缺失；尝试通过 pnpm 补装时遭遇网络/EACCES 阻断，未安装成功且未重试，因此 typecheck/build 未运行。 |
| 浏览器 | `NEEDS_BROWSER` | 需在登录态浏览器中验证 V1/V2、1440/1024/390、返回首页和真实账号登录。 |
| 数据库/SQL | `NOT_APPLICABLE / NOT_AUTHORIZED` | 本需求不改数据库，未执行 SQL。 |
| 服务器/部署 | `NOT_DEPLOYED / NOT_AUTHORIZED` | 未连接 229，未执行构建部署或服务器验收。 |
| 生产/DYZ | `NOT_RUN / NOT_APPLICABLE` | 未执行生产操作，未影响 `dyz-current-shadow`。 |

## 实际变更与交付记录

### 实际修改文件

- `portal/src/components/competition-home/CompetitionHomeCanvas.vue`：首页顶部登录按钮改为同窗口直达 `/admin/login`，携带受控 `homeVersion`。
- `frontend/src/views/login.vue`：统一登录表单初始态、V1/V2 主题跟随、安全内部重定向、`/crehn` 回跳、`CH` 原生标识、reduced-motion 适配，并移除旧 Logo/dark 覆盖。
- `frontend/src/config/loginPage.ts`：统一“创意河南”品牌、赛事副标题和登录页文案，移除旧入口卡片配置。
- `backend/ruoyi-modules/ruoyi-crehn/src/test/contract/portal006-home-login-contract.ps1`：新增首页登录直达、版本白名单、登录表单、回跳与品牌视觉静态契约。

### 依赖、API、配置、数据库与授权

- 无新增依赖、API、数据库字段、迁移、配置密钥或部署资产；既有登录 API、验证码、记住账号、注册和权限逻辑未改。
- 未执行 SQL、服务器连接、构建部署、Git 提交/推送、生产操作或 DYZ 操作。
- 浏览器场景（1440/1024/390、`prefers-reduced-motion`、真实账号登录）仍为 `NEEDS_BROWSER`；typecheck/build 因本地依赖缺失和 pnpm 网络/EACCES 阻断保持 `NOT_RUN/BLOCKED`。
