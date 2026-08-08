# PORTAL-005 双视觉首页组件化与后台自由编排

## 状态

- 需求：用户 2026-08-07 确认将 PORTAL-004 两版首页改造成后台可维护的可视化组件页面
- 当前状态：`IMPLEMENTED / CONTRACT_VERIFIED / VUE_TSC_VERIFIED / PORTAL_VITE_BUILD_VERIFIED / LOCAL_BROWSER_VERIFIED / TEST_0.1.12_DEPLOYED / TEST_PUBLIC_BROWSER_VERIFIED / FRONTEND_VITEST_VERIFIED_IN_IMAGE_BUILD / JAVA_NARROW_CHECK_NOT_RUN / CMS_ROLE_FLOW_NEEDS_BROWSER`
- 唯一主任务编号：`PORTAL-005`
- 2026-08-07 专项授权：TEST V006、迁移前备份和数据库回读已完成；TEST 部署与公共浏览器验收也已授权并完成。
- 本地 Git提交/推送：已授权并执行；仍未授权：生产连接/验收、其他 SQL、外部发送。2026-08-08 本次精确清理已按授权完成，不得泛化为其他镜像、缓存、容器、卷或项目清理。
- 2026-08-07 本轮实施：以 `.tmp/creative-henan-both-final-reference/creative-henan-both-final/` 中 `01-liquid-glass` 与 `02-art-tech` 最终确认稿为唯一视觉基准，收敛发布快照和内置回退的视觉分叉；扩充可编辑白名单字段、补齐 v1/v2 独立预设与跨端契约。仅修改 PORTAL-005 指定文件，不执行数据库、部署、Git 提交/推送或删除操作。

### 2026-08-07 本轮本地实现与验证

- 已实施：11 个白名单组件保持不变；v1/v2 采用独立字段预设。公共门户仅在**不存在**发布快照时生成回退；已发布但全部 `disabled` 的空渲染不再误回退。共享 Canvas 同时消费顺序、显隐和 `gridX/gridY/gridW/gridH`。
- 已实施：导航、主视觉、ART/年份/届次、浮卡、受控截止时间动态倒计时/数据轨、赛道、流程、文件规范、时间线、通知、联系、页脚和两种弹窗的可见文案均进入受控标量字段；v1 的艺术×科技组件默认禁用，v2 默认启用；显式空字符串保留为空，仅缺失字段使用版本默认值。
- 已实施：`creative-henan-hero.png` 从最终参考包复制（1448×1086）；`arttech-hero.png` 从最终参考包复制并在模板中按真实 1584×990 声明。
- PASS：`portal005-componentized-home-contract.ps1`；`git diff --check`（仅 Git CRLF 提示，无空白错误）。
- 初始阻塞（后续已由本卡末尾隔离副本验证部分更新/取代）：本轮定向 Vitest 触发 pnpm 补依赖，受网络/权限限制失败（未更改 lockfile）；当时门户无 `node_modules`，typecheck/build、浏览器复测尚未执行。Vitest、Java/Maven/wrapper 与后端 Java 窄编译、CMS 真实保存—启用—发布闭环仍未完成；服务器、部署、数据库、Git 提交/推送均未执行。

## 目标

复用现有 Vue 3、Element Plus、GridStack、CMS 布局快照和发布链路，把液态玻璃版与艺术科技明亮版实现为两套受控模板。运营人员可在后台调整组件顺序、栅格、显隐及白名单字段，发布后公共门户只读取不可变发布快照。

## 允许修改范围

- `frontend/src/views/crehn/cms/`、`frontend/src/api/crehn/cms.ts`
- `portal/src/components/competition-home/`、`portal/src/config/competitionHome.ts`、`portal/src/main.ts`
- `backend/ruoyi-modules/ruoyi-crehn/src/main/java/org/dromara/crehn/cms/service/impl/PortalHomeConfigurationValidator.java`
- 本任务卡、`TASKS.md`、`docs/codex/TASK_BOARD.md`、`docs/codex/PROJECT_STATUS.md`、`docs/codex/HANDOFF.md`

## 保护与禁止范围

- 保留 PORTAL-003、PORTAL-004、GridStack、V006、任务卡和测试产物的全部未提交修改；不覆盖 `portal/src/components/PortalContent.vue`。
- 不新增迁移或执行 V006 之外的 SQL；PORTAL-005 实施本身不自动授权部署，本次 TEST 部署由后续 OPS-003 专项授权完成。仍不得生产、其他 SQL 或外部发送；Git 提交/推送已按当前授权完成。精确清理仅限本次已批准对象，不得泛化。
- 禁止 `git reset`、`git checkout`、`git clean`。
- 不开放任意 HTML、CSS、JavaScript、iframe 或未登记的跳转动作；组件类型、字段和主题令牌均使用服务端白名单。

## 实施设计

- 组件注册表：登记导航、主视觉、关键数据、赛道、艺术科技、流程、文件规范、时间线、通知附件、联系机构和页脚组件。
- 模板：`v1` 液态玻璃与 `v2` 艺术科技各自生成可继续编辑的组件快照。
- 后台：GridStack 负责拖拽/缩放，字段表单负责配置，不要求运营人员直接编写 JSON。
- 门户：按已发布快照的组件顺序和栅格渲染；无有效组件快照时回退 PORTAL-004 内置页面。
- 安全：服务端继续校验组件类型、数据源和 JSON 对象；运行时只识别注册组件与受控动作。

## 验收与证据

| 证据层级 | 状态 | 说明 |
| --- | --- | --- |
| 静态 | PASS | PORTAL-005 静态契约通过；11 个组件在服务端、CMS 注册表和门户渲染器一致；`git diff --check` 通过。 |
| 构建 | PASS | 管理端全量 typecheck；2 文件 6 测试；新注册表完整 ESLint，CMS 页面关闭既有纯格式规则后 ESLint 通过；门户 typecheck/build 通过。 |
| 浏览器 | PASS（门户本地/TEST 公共首页）/ NEEDS_BROWSER（CMS角色流） | 部署前历史证据曾为 TEST 静态资源返回 HTML、控制台 3 个 MIME 错误；已由 `0.1.10-test` 公共首页最新证据取代：CMS API/静态请求均 200，MIME 与 nosniff 正确，控制台 0 errors/0 warnings，V1/V2 与各目标视口通过。真实 CMS 登录保存—启用—发布及角色登录态仍未执行。 |
| 数据库 | PASS（TEST V006） | 迁移前全库逻辑备份 74 张表、252752 bytes、SHA-256 `d4504a4b...5a5bbf`；V005 前置列/索引/角色回读通过；V006 SHA-256 `545666ec...c785202` 执行成功，`portal_page_layout`、生成列和三项索引回读通过，当前 0 行。 |
| 服务器 | PASS（TEST部署/健康）/ NEEDS_BROWSER（CMS闭环） | `0.1.9-test` 路由 404 是部署前历史证据，已由 `0.1.10-test` BuildLocal、DeployLocal、VerifyLocal、应用健康和公共首页浏览器最新证据取代；真实 CMS 登录保存—启用—发布闭环仍未执行。 |
| 生产 | NOT_RUN | 未授权部署或生产验收 |

## 风险

- V006 已在 TEST 落库；`0.1.9-test` 不含 PORTAL-005 路由及静态资源返回 HTML 是部署前历史证据，已由 `0.1.10-test` 部署、MIME/nosniff 和公共浏览器最新证据取代。数据库和匿名公共首页通过不能替代登录态 CMS 发布验收。
- PORTAL-004 固定页面与新快照渲染需保持兼容；无快照时必须安全回退。
- 自由编辑限定为已登记组件和字段，不等同于任意代码级页面生成器。

## 实际变更

- `frontend/src/views/crehn/cms/competitionHomeRegistry.ts`：11 类组件注册表、字段模型和 v1/v2 模板。
- `frontend/src/views/crehn/cms/index.vue`：模板套用、GridStack 组件键映射、字段化编辑和“保存当前画布为布局”。
- `frontend/src/views/crehn/cms/competitionHomeRegistry.test.ts`：组件类型、模板键、默认值合并契约测试。
- `backend/.../PortalHomeConfigurationValidator.java`：组件/字段白名单、文本安全边界和组件键去重。
- `portal/src/main.ts`、`portal/src/config/entries.ts`：把公开发布快照的组件和布局编码接入运行时配置。
- `portal/src/config/competitionHomeModules.ts`：匿名运行时快照归一化、过滤、排序和栅格限制。
- `portal/src/components/competition-home/CompetitionHomeCanvas.vue`：11 类模块的响应式受控渲染器。
- `portal/src/components/competition-home/CompetitionHomePage.vue`：优先渲染有效发布快照，无快照回退 PORTAL-004 固定页面。
- `backend/.../src/test/contract/portal005-componentized-home-contract.ps1`：跨后端/CMS/门户静态契约测试。

## 依赖、API、配置和数据库变化

- 依赖：无新增；复用 GridStack、Element Plus、Vue 3。
- API：无新增端点；扩展既有公开首页配置响应的前端消费范围，读取已有 `components` 和 `layoutCode`。
- 配置：新增运行时 `homeComponents`、`publishedLayoutCode`；保留 `renderVersion=v1/v2`。
- 数据库：实现本身未新增迁移；按专项授权已将既有 V006 落到 TEST，新增 `portal_page_layout`，未执行其他 SQL。数据库内没有 Flyway/Liquibase/CREHN 迁移历史表，执行历史以服务器侧校验文件留存。

## 2026-08-07 TEST 专项执行记录

- 目标：`192.168.2.229` / Compose `crehn-test` / 数据库 `crehn_test`；受保护项目 `dyz-current-shadow` 预检通过且未被修改。
- 迁移前回读：74 张表；V005 的 `sys_workbench_layout.role_key` 为 `NOT NULL`，`idx_sys_workbench_layout_role_key`、`uk_sys_workbench_layout_role_key_component` 和两个稳定角色均存在；V006 表不存在。
- 备份：`/home/dyz/crehn-v006-backups/20260807T022331Z-crehn_test-pre-v006.sql`，74 个 `CREATE TABLE`，252752 bytes，SHA-256 `d4504a4b863a8d23cfa75e2f48b14f265b8337175863ccb5a03e7882605a5bbf`。这是全库逻辑备份，未声称完成恢复演练或物理备份。
- V006：文件 SHA-256 `545666ec4d7b447cad78c75a4a9fd91689936cb9cec2427914bf6fda2c785202`；执行时间 `2026-08-07T02:24:15Z`；服务器证据 `/home/dyz/crehn-v006-backups/V006__crehn_portal_page_layout.applied` 校验通过。
- 结构回读：InnoDB；`active_page_code` 为 `STORED GENERATED`；主键、`uk_portal_page_layout`、`uk_portal_page_layout_active`、`idx_portal_page_layout_active` 均存在；当前 `row_count=0`、`active_rows=0`。
- 运行时：`crehn-backend:0.1.9-test`、`crehn-web:0.1.9-test`；`/crehn/cms/layout/list` 与 `/crehn/cms/public/site/1/home/config` 均为应用层 404，公开站点查询显示站点不存在。

## 未验证项与下一步

- 历史阻塞：`BLOCKED / DEPLOY_NOT_AUTHORIZED` 与“部署后 TEST 验收 `NOT_RUN`”已由 `0.1.10-test` TEST 部署、Verify 和公共浏览器证据解除/取代。
- 当前下一步：真实 CMS 管理员登录后的套模板、字段修改、拖拽、保存、启用、发布与匿名回读闭环，以及角色登录态 smoke，均为 `NEEDS_BROWSER`。
- `NOT_RUN`：后端 Java/Maven 编译（本机命令不可用）与生产验收。
- 后续后台接口：统一登录、作品报送、正式通知详情、受控附件下载仍保持 TODO，本任务未伪造接口。

## 2026-08-07 TEST 0.1.10-test 部署与公共首页浏览器验收

- 部署版本：`0.1.10-test`，revision `94cbb8b94b1e9736276dd2d890ba9b23aa8c4be9`。`BuildLocal`、`DeployLocal` 与 `VerifyLocal` 均 `PASS`；构建未执行 SQL，未执行生产、其他 SQL 或 Git push，且未影响 `dyz-current-shadow`。
- 发布资产：manifest `/srv/crehn-test/releases/0.1.10-test/crehn-images-0.1.10-test.json`，SHA-256 `5fea1038024cdd501b9b701f05bfbe4bbf162a03b4d4898df41f794c5a3ed8e1`；部署前备份 `/srv/crehn-test/backups/20260807T093844Z-0.1.9-test-pre-deploy`。backend/db/web healthy，`web_admin_api_health` healthy；`protected_web_entry`/`protected_storage_entry` healthy；TEST MinIO live-data 不复制警告保留。
- 公共首页浏览器：`PASS`，URL 为 `http://192.168.2.229:28181/crehn/`。公共 CMS API 返回 200，全部静态请求返回 200；JS 为 `application/javascript`、CSS 为 `text/css`、`config.js` 为 `application/javascript`，且均有 `nosniff`。控制台为 `0 errors / 0 warnings`。
- 视觉与版本切换：V1/V2 切换 `PASS`；V1 在 `1440x1000`、`1280x800`、`768x1024`、`390x844` 视觉 `PASS`；V2 在 `1440x1000`、`390x844` 视觉 `PASS`。截图：`output/playwright/portal005-test-0.1.10-*.png`。
- 仍未完成：真实 CMS 登录后的保存—启用—发布闭环为 `NEEDS_BROWSER`/未执行；`VerifyLocal` 的角色登录态 Browser smoke tests 也仍为 `NEEDS_BROWSER`，不得以公共匿名首页验收替代。

## 2026-08-07 参考稿返工收口

- 基准：重新按 `.tmp/creative-henan-both-final-reference/creative-henan-both-final/01-liquid-glass` 与 `02-art-tech` 的源码校正 V1/V2 默认文案、方向编码、版本专属模块和动作目标；两张主视觉仍使用同源原图。
- CMS：切换组件类型时根据当前布局/组件键使用 `versionDefaultConfigs.v1/v2`；组件编辑框增加“门户显示”开关。服务端字段白名单同步 `documentTitle`、受控截止时间、报送提示、通知卡动作和页脚签名。
- 门户：V1 模板默认禁用、V2 默认启用艺术×科技宣言；显式启用的 V1 快照可渲染；`deadlineAt` 计算 `Math.ceil((deadline - Date.now()) / 86_400_000)`，无效值显示受控占位；`document.title` 仅取受控纯文本字段。已发布空/全禁用快照保持空渲染，未发布快照才使用内置回退。
- PASS：`powershell -ExecutionPolicy Bypass -File .\backend\ruoyi-modules\ruoyi-crehn\src\test\contract\portal005-componentized-home-contract.ps1`；`git diff --check`（仅 CRLF 提示，无空白错误）。
- BLOCKED/NOT_RUN：`pnpm test competitionHomeRegistry.test.ts` 尝试补依赖时网络/权限下载失败；门户 typecheck/build 因无本地依赖未运行；Java 窄编译、浏览器、服务器、数据库、部署、Git 提交/推送均未执行。本轮不得引用历史构建或浏览器记录替代当前改动验收。

## 2026-08-07 最终定点修复

- 交互：V1 主视觉双按钮分别滚动至报送流程/通知；V2 保持报送流程/赛道；关键数据箭头在 V1 指向时间线、V2 指向赛道。页脚恢复 V1/V2 可访问版本切换按钮。
- 回退与显隐：`value`、`flag`、`rows` 支持显式组件类型回退，导航组件未出现时弹窗仍读取当前版本受控文案；V1 默认及回退不出现艺术×科技，但 CMS 显式启用的 V1 快照仍可渲染并有独立样式。
- 配置与无障碍：`DAYS`、步骤前缀、V2 通知图案文字、页脚品牌缩写均入注册表、回退与服务端白名单；通知图案保留换行。弹窗补齐打开焦点、Escape、Tab 焦点陷阱、关闭焦点恢复；报送弹窗主按钮关闭后滚动至联系区，通知按钮仍滚动时间线。
- PASS：最终静态契约与 `git diff --check`；已复用主检出已有 `@vue/compiler-sfc` 3.5.30，对 Canvas/Page 执行 parse、compileScript、compileTemplate 均 `PASS`。完整 typecheck/build 仍为 `BLOCKED_NO_LOCAL_DEPS`；依赖型 Vitest、Java 编译及浏览器验收保持未执行/阻断，不以历史证据代替。
- 收口补正：页脚说明、赛事签名和版本切换使用三列栅格（移动端仍单列）；页脚品牌缩写与 V2 通知图案文字在显式空字符串时不渲染对应装饰节点。

## 2026-08-07 390×844 浏览器发现与 CSS 修复

- 浏览器发现（待 Sol 复测）：真实 Playwright 390×844 截图 `C:\Users\A\AppData\Local\Temp\crehn-portal005-verify-019fda97\.playwright-cli\page-2026-08-07T13-24-56-547Z.png`（V2）及同目录 `page-2026-08-07T13-25-01-869Z.png`（V1）显示首屏从约 x=170 向右裁切；不得据此标记 `BROWSER_PASS`。
- 修复：仅在 `max-width: 520px` 下为 `hero-section` 与 `hero-copy` 设置 `min-width: 0`，并将 `hero-visual` 的布局宽度限制为 `100%/max-width:100%`、`min-width:0`；保留绝对定位主视觉与 `.7` 缩放/居中，避免 620px 未缩放布局宽度撑开移动端单列。桌面规则未改。
- 历史记录：该时点仅完成静态契约和差异检查，移动端浏览器复测尚未执行。

## 2026-08-07 隔离副本 typecheck/build 与本地静态预览复测

- 隔离方式：Sol 在 `C:\Users\A\AppData\Local\Temp\crehn-portal005-verify-019fda97` 建立隔离副本，并通过只读 junction 复用主检出 `portal/node_modules`；未修改主检出依赖或其他工作树。
- 静态检查：`vue-tsc --noEmit` `PASS`；Vite `6.4.3` build `PASS`（40 modules）。
- Playwright 仅针对本地静态预览：1440×1000 V1/V2 首屏视觉与页脚版本切换 `PASS`；390×844 CSS 修复后 V1 截图 `C:\Users\A\AppData\Local\Temp\crehn-portal005-verify-019fda97\.playwright-cli\page-2026-08-07T13-28-52-666Z.png`、V2 截图 `C:\Users\A\AppData\Local\Temp\crehn-portal005-verify-019fda97\.playwright-cli\page-2026-08-07T13-29-00-619Z.png`，两版 `document/body.scrollWidth = innerWidth = 390`，`PASS`。
- 本地静态预览交互：版本切换、登录弹窗打开焦点落在关闭按钮、Escape 关闭并恢复登录按钮焦点、移动菜单、赛道 B 显示 5 个方向均 `PASS`。控制台唯一错误为静态预览无后端导致 `/prod-api/crehn/cms/public/site/code/crehn` 返回 404；回退页正常，不计为集成、TEST 或部署通过。
- 仍未完成：Vitest 受依赖获取阻断；Java 窄编译 `NOT_RUN`；真实 CMS 登录后的保存—启用—发布闭环 `NEEDS_BROWSER`。上述本地静态预览不替代服务端、部署或角色登录态验收。

## 2026-08-08 TEST 0.1.12-test 部署与公共首页浏览器验收

- 部署版本：`0.1.12-test`，revision `40e4fe1ee9a849141e8add90ed29a6394d3aa3b8`。`BuildLocal`、`DeployLocal` 与 `VerifyLocal` 均 `PASS`；构建过程未执行 SQL、生产操作或 DYZ 写入，且未影响 `dyz-current-shadow`。
- 发布资产：manifest `/srv/crehn-test/releases/0.1.12-test/crehn-images-0.1.12-test.json`，SHA-256 `72fb1a19c19d04caaa943fb138ff0843dd04364196b3252b47b1ddca8893b031`；部署前备份 `/srv/crehn-test/backups/20260807T235419Z-0.1.10-test-pre-deploy`。
- 镜像回读：backend `6241849f...`、db `9696c7a...`、web `405fffcd...`；CREHN 服务、应用 health、`protected_web_entry` 与 `protected_storage_entry` 均 `PASS`。TEST MinIO live-data 不复制 warning 保留并已记录。
- 构建证据：Web 镜像构建内 `frontend pnpm test` `PASS`；管理端 Vite `PASS`；portal `vue-tsc` 与 Vite `PASS`。chunk/config 提示仅为 warning。此前 `0.1.11-test` 首次构建因空间门槛与依赖测试上下文阻塞，修复提交 `40e4fe1` 仅补齐 Validator 测试输入，未将失败版本宣称为已部署。
- 公共首页浏览器：URL `http://192.168.2.229:28181/crehn/`；CMS 公共 API `200`；JS `application/javascript`、CSS `text/css`、两张 PNG `image/png`，均带 `nosniff`；控制台 `0 error / 0 warning`。
- 视觉与响应式：V1/V2 在 `1440x1000` 与 `390x844` 均 `PASS`；移动端 `scrollWidth=390`；版本切换、移动菜单、登录弹窗初始焦点、Escape 关闭并恢复登录焦点均 `PASS`。稳定截图：`output/playwright/portal005-test-0.1.12-v1-1440x1000.png`、`output/playwright/portal005-test-0.1.12-v2-1440x1000.png`、`output/playwright/portal005-test-0.1.12-v2-390x844.png`、`output/playwright/portal005-test-0.1.12-v1-390x844.png`。
- 未完成项：真实 CMS 登录后的保存—启用—发布—匿名回读闭环仍为 `NEEDS_BROWSER`，不能由公共首页替代；后端 Java 窄编译仍为 `NOT_RUN`。
- Git：代码提交 `1ac25d4` 与修复提交 `40e4fe1` 均已非强制推送到 `origin` 与 `github` 同名分支，并通过远端回读验证。
