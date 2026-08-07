# PORTAL-005 双视觉首页组件化与后台自由编排

## 状态

- 需求：用户 2026-08-07 确认将 PORTAL-004 两版首页改造成后台可维护的可视化组件页面
- 当前状态：`IMPLEMENTED / STATIC_VERIFIED / LOCAL_BROWSER_VERIFIED / TEST_DB_VERIFIED / TEST_DEPLOYED / TEST_PUBLIC_BROWSER_VERIFIED / CMS_ROLE_FLOW_NEEDS_BROWSER`
- 唯一主任务编号：`PORTAL-005`
- 2026-08-07 专项授权：TEST V006、迁移前备份和数据库回读已完成；TEST 部署与公共浏览器验收也已授权并完成。
- 本地 Git提交：已授权；仍未授权：Git push、生产连接/验收、其他 SQL、外部发送和文件删除。

## 目标

复用现有 Vue 3、Element Plus、GridStack、CMS 布局快照和发布链路，把液态玻璃版与艺术科技明亮版实现为两套受控模板。运营人员可在后台调整组件顺序、栅格、显隐及白名单字段，发布后公共门户只读取不可变发布快照。

## 允许修改范围

- `frontend/src/views/crehn/cms/`、`frontend/src/api/crehn/cms.ts`
- `portal/src/components/competition-home/`、`portal/src/config/competitionHome.ts`、`portal/src/main.ts`
- `backend/ruoyi-modules/ruoyi-crehn/src/main/java/org/dromara/crehn/cms/service/impl/PortalHomeConfigurationValidator.java`
- 本任务卡、`TASKS.md`、`docs/codex/TASK_BOARD.md`、`docs/codex/PROJECT_STATUS.md`、`docs/codex/HANDOFF.md`

## 保护与禁止范围

- 保留 PORTAL-003、PORTAL-004、GridStack、V006、任务卡和测试产物的全部未提交修改；不覆盖 `portal/src/components/PortalContent.vue`。
- 不新增迁移或执行 V006 之外的 SQL；PORTAL-005 实施本身不自动授权部署，本次 TEST 部署由后续 OPS-003 专项授权完成。仍不得生产、其他 SQL、Git push、外部发送或文件删除。
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
