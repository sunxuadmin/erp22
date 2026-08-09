# PORTAL-007 V2 默认首页与生产晋级前置

## 状态

- 需求：用户 2026-08-09 明确要求“v2 样式作为首页”，并计划经 229 TEST 验证后部署至 `cre.blog56.top` 生产环境。
- 当前状态：`TEST_VERIFIED / NEEDS_BROWSER / NEEDS_PROD_PROMOTION`
- Git 提交：`0954db30e488ca7cf0a60977b366fc49bd7f10da`
- Git 推送：`origin/github 非强制推送 PASS`
- 数据库执行：未授权
- 229 TEST 构建/部署：`0.1.15-test PASS`
- 阿里云生产部署：需独立授权

## 目标

将公共门户首页的默认视觉版本调整为 V2：无版本查询参数时进入 V2；保留显式 V1 预览入口；输入非法版本时安全回退到 V2。实现完成后，先构建新的 TEST 不可变版本，在 192.168.2.229 的 `crehn-test` 完成构建、部署与验证，再以完全相同的 revision 与镜像摘要晋级阿里云生产，不在生产端重新构建源码。

## 已确认行为契约

- 无 query（例如 `/crehn/`）默认渲染 V2 首页。
- 显式 `?version=v1` 保留 V1 预览能力。
- `?version=v2` 显式选择 V2。
- 其他或非法 `version` 值按 V2 处理，不引入新的视觉版本或兼容分支。
- 生产发布必须使用已在 229 TEST 构建、部署、验证通过的同一 revision/镜像归档；生产侧只执行受控加载与启动，不执行源码构建或重新下载已随归档传递的 CREHN 镜像。

## 范围

- 涉及模块：`portal` 公共门户首页版本选择及其定向契约测试；必要的 TEST/生产发布证据回填。
- 预计代码文件：由 Terra 先检查并确定唯一写入入口，原则上只改首页版本归一化逻辑及对应定向测试。
- 预计发布产物：新的 TEST 不可变版本（版本号由 Sol/Terra 依据当前发布序列确定），包含 portal/web/backend/db 既有构建产物。
- 数据/API/配置：不新增数据库字段、SQL、API、密钥或业务配置；生产运行配置、OSS、TLS、反向代理和安全组另行按门禁提供。

## 部署与数据边界

- 先在 229 `crehn-test` 形成新的不可变 TEST 版本并完成静态、构建、容器健康与浏览器验收，再允许同 revision 镜像晋级生产。
- 不复制 TEST 数据卷、数据库内容、Redis/MinIO 数据或账号资料到阿里云；生产数据库初始化/恢复是独立授权和独立任务。
- 生产域名 `cre.blog56.top`、TLS 证书、反向代理、80/443 安全组与防火墙为独立门禁；本任务卡不隐式授权其配置。
- 生产仅允许受控 `docker load` 与 Compose 启动既有镜像；不得执行 Docker build、pull、prune、任意 SQL 或影响 `dyz-current-shadow` 的操作。

## 禁止范围

- 不修改 DYZ 代码、服务器、容器、数据、卷或 `dyz-current-shadow`。
- 不执行生产数据库 SQL、迁移、数据复制或账号资料传输。
- 不在未完成 229 TEST 验证前直接部署生产。
- 不在生产端重新构建、拉取或清理 Docker 资源；不执行 DNS API、证书申请或无关主机软件安装。
- Git 提交、推送、229 连接、生产连接和浏览器验收均保持独立授权；禁止 `git reset`、`git checkout`、`git clean` 和批量覆盖并行修改。

## 验收契约

- [x] 无 query 首页为 V2。
- [x] `?version=v1` 仍可预览 V1。
- [x] `?version=v2` 为 V2；非法 `version` 回退 V2。
- [x] 定向静态三断言通过；目标文件无空白错误。
- [x] 新 TEST 不可变版本 `0.1.15-test` 在 229 完成 Preflight、Stage、Build、Deploy、Verify；backend/db/minio/redis/web 健康，`minio-init` 完成，DYZ 保护端点前后健康，HTTP/MIME 检查通过。
- [ ] 新 TEST 版本完成浏览器验收；当前仍为 `NEEDS_BROWSER`。
- [ ] 生产晋级仅使用同 revision/同镜像摘要的 TEST 产物；未复制 TEST 数据卷。
- [ ] 生产 DNS/TLS/反向代理/安全组、生产数据库初始化及真实运行配置均有独立门禁证据。

## 验证计划与证据

| 证据层级 | 状态 | 说明 |
| --- | --- | --- |
| 静态/定向检查 | `PASS` | 三项版本断言通过；目标文件 `git diff --check` 通过。 |
| 构建 | `PASS` | 229 `0.1.15-test` Build PASS；产物 manifest SHA-256 为 `d02fa9dbe08973d12bc7b53569b863306cc9cf04e77590addda46581ff5a5a06`。 |
| 浏览器 | `NEEDS_BROWSER` | 需验证无 query、V1/V2 显式参数及登录入口视觉连续性。 |
| 229 TEST | `PASS` | `0.1.15-test` Preflight/Stage/Build/Deploy/Verify PASS；回滚点：`/srv/crehn-test/backups/20260809T060927Z-0.1.14-test-pre-deploy`。 |
| 阿里云生产 | `NOT_PROD_DEPLOYED` | 尚未连接或部署；需确认主机、指纹、运行配置、门禁与晋级授权。 |
| 数据库/SQL | `NOT_RUN / NOT_AUTHORIZED` | 本任务不执行 SQL，不复制 TEST 数据卷。 |
| DNS/TLS/安全组 | `NOT_RUN / NEEDS_GATE` | 由生产运维门禁单独验证。 |

## 实际变更与证据

### 实际修改文件

- `portal/src/config/entries.ts`：将 `defaultVersionSettings.active` 从 `"v1"` 调整为 `"v2"`，使无 query 首页默认使用 V2；合法 query override 保持启用，CMS 显式 `renderVersion` 行为不变。

### 证据与授权

- 静态版本三断言及目标文件 `git diff --check`：`PASS`。
- Git 提交 `0954db30e488ca7cf0a60977b366fc49bd7f10da`，并向 `origin` 与 `github` 完成非强制推送：`PASS`。
- 229 `crehn-test` `0.1.15-test`：Preflight、Stage、Build、Deploy、Verify 均 `PASS`；manifest SHA-256：`d02fa9dbe08973d12bc7b53569b863306cc9cf04e77590addda46581ff5a5a06`。
- 229 运行回读：backend、db、minio、redis、web 健康，`minio-init` 完成；DYZ 保护端点部署前后均健康；HTTP/MIME 检查 `PASS`。
- 229 回滚点：`/srv/crehn-test/backups/20260809T060927Z-0.1.14-test-pre-deploy`。
- 浏览器验收：`NEEDS_BROWSER`；尚未完成无 query、V1/V2 显式参数及登录入口视觉连续性回读。
- 阿里云生产部署、SQL/数据库操作：未执行；生产晋级仍需独立门禁。
