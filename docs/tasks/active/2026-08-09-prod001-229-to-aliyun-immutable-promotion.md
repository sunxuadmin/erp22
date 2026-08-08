# PROD-001 229 TEST 不可变产物晋级阿里云生产

## 状态

- 需求：用户要求建立 CREHN 生产部署脚本；由 192.168.2.229 完成构建与测试后，经受控 SSH 跳板将镜像及构建产物晋级至 `118.190.150.161`，域名为 `cre.blog56.top`。
- 当前状态：`IMPLEMENTED / STATIC_VERIFIED / NEEDS_GATE_BOOTSTRAP / NEEDS_SERVER / NEEDS_DNS_TLS / NOT_DEPLOYED`
- Git 提交：未授权
- Git 推送：未授权
- 数据库执行：未授权
- 229 服务器连接/写入：本轮未授权、未执行
- 生产服务器连接/部署：本轮未授权、未执行
- DNS、证书、安全组与防火墙：本轮未授权、未执行

执行入口默认使用 `PLAN`；任何写操作都要求显式 `Apply` 与精确 `PROMOTE:crehn-prod:<version>` 确认串。

## 目标

建立可审计、可重复复用的 CREHN 生产晋级入口：仅接受 192.168.2.229 上已完成 TEST 验证、带 `SUCCESS` 标记且 manifest 与镜像归档校验通过的同版本、同 revision、`x86_64` Docker 产物；通过受控 SSH 跳板将镜像归档、manifest 与必要部署资产传递到 `118.190.150.161`，生产端只加载并运行该不可变产物，不从源码构建。后续版本沿用同一晋级流程。

## 范围

- 涉及模块：`deploy/` 生产晋级脚本、SSH 跳板配置模板、产物/manifest 校验、生产 Compose/运行配置模板、静态检查与回滚前置检查。
- 预计文件：现有 `deploy/scripts/`、`deploy/linux/` 中的晋级入口或其受控辅助脚本；`deploy/.env.example`、生产运行配置/反向代理模板；必要的定向静态检查或本任务卡证据回填。具体文件由 Terra 依据现有唯一写入入口确定。
- 产物与协议：229 TEST 发布目录中的 `SUCCESS`、manifest、tar 及 SHA-256；生产仅接收同版本同 revision 的 `x86_64` 产物，不接收源码构建请求。
- 域名验收：A 记录期望 `cre.blog56.top -> 118.190.150.161`；HTTPS 由生产侧独立配置的反向代理承接，脚本只进行受控配置/健康检查所需的契约校验，不在本轮安装或申请证书。

## 已确认门禁与约束

- 本轮只实施脚本、模板和静态测试；不连接 192.168.2.229 或生产，不实际部署，不改 DNS、安全组、防火墙或证书，不执行 SQL，不进行 Git 提交或推送。
- 跳板主机和生产主机的地址、端口、用户、认证方式及主机指纹必须来自受限 `deploy/.env` 或等价受控运行配置；不得在脚本、模板、镜像或任务卡中硬编码凭据、密钥或真实密码。
- 229 仅作为已验证产物来源与 SSH 跳板；生产不得执行 Docker build、源码打包或任意构建命令。
- 首次生产数据库初始化、生产真实 runtime 配置、TLS、80/443、安全组和防火墙均为独立授权门禁；本任务不得隐式执行。
- 不影响 `dyz-current-shadow`、DYZ 服务器/容器/数据/卷，不访问生产数据库，不删除任何镜像、缓存、容器或卷。
- 生产晋级应保留可验证的上一成功版本与回滚点；失败时停止并报告，不自动扩大清理或修改范围。

## 禁止范围

- 不在本任务执行 229 或 `118.190.150.161` 的 SSH 连接、文件上传、Docker/Compose 操作或浏览器验收。
- 不执行 DNS API、证书申请/续期、云安全组、防火墙、端口暴露或宿主机软件安装。
- 不执行数据库初始化、迁移、备份、回滚或任何任务外 SQL。
- 不把生产密钥、密码、运行配置内容或账号资料写入仓库、日志或输出。
- 不修改业务前端/后端、数据库结构、DYZ 资产或并行任务文件；不使用 `git reset`、`git checkout`、`git clean` 或批量覆盖。

## 验收契约

- [x] 晋级入口能拒绝缺失 `SUCCESS`、manifest/tar SHA-256 不匹配、版本/revision 不一致或非 `x86_64` 的候选产物；同时核对 image IDs。
- [x] 晋级入口只引用受限环境配置中的跳板/生产连接参数；两端强制 key-only、独立 `known_hosts` 与指纹校验，生产全程使用 `ProxyJump`，未配置真实环境时安全阻断。
- [x] 静态检查覆盖实际 PowerShell/Bash/配置文件，并确认生产路径不执行 build、pull、prune、SQL 或 DNS API；生产仅 `docker load`、`--no-build`，保留既有回滚路径。
- [x] 产物传递契约明确：229 TEST 产物 → 受控跳板 → 生产加载；active/candidate/release 的 version 与 revision 必须一致，后续版本可复用同一入口。
- [ ] 生产域名与 HTTPS、首次数据库初始化、真实 runtime、TLS/安全组门禁均明确标记为独立未执行项。

## 验证计划与证据

| 证据层级 | 状态 | 说明 |
| --- | --- | --- |
| 静态/定向检查 | `PASS` | PROD-001 fixture/static PASS；Sol PowerShell Parser PASS；Git Bash `bash -n` PASS；任务范围 `git diff --check` PASS。 |
| 构建 | `NOT_RUN` | 本轮不构建镜像；需后续在 229 通过独立授权执行。 |
| 浏览器 | `NOT_RUN` | 本轮不做域名或生产浏览器验收。 |
| 229/生产服务器 | `NOT_RUN` | 本轮不连接、不上传、不部署。 |
| DNS/TLS/安全组 | `NOT_RUN` | 均为独立门禁，未授权。 |
| 数据库/SQL | `NOT_RUN / NOT_AUTHORIZED` | 本任务不执行 SQL。 |
| Git | `NOT_RUN / NOT_AUTHORIZED` | 本任务不提交、不推送。 |

## 实际变更与证据

### 实际修改文件

- `deploy/scripts/promote-test-release-via-229.ps1`：默认 PLAN 的本地编排入口；Apply 仅接受精确 PROMOTE 串，通过 229 跳板晋级已验证产物。
- TEST 导出门禁、安装器与 sudoers：`deploy/linux/crehn-test-release-export-gate.sh`、`deploy/linux/install-crehn-test-release-export-gate.sh`、`deploy/sudoers/crehn-test-release-export-gate`。
- PROD 暂存/部署门禁、安装器与 sudoers：`deploy/linux/crehn-prod-stage-release.sh`、`deploy/linux/crehn-prod-deploy-gate.sh`、`deploy/linux/install-crehn-prod-deploy-gate.sh`、`deploy/sudoers/crehn-prod-deploy-gate`。
- `deploy/linux/validate-crehn-prod-runtime-env.sh`：生产 runtime 配置校验，防止 shell 注入和越界配置。
- `deploy/linux/crehn-deploy.sh`：接入受控生产晋级动作与既有回滚边界。
- `deploy/.env.example`、`deploy/env/prod.env.example`：仅提供受限控制字段和生产 runtime 模板，不含真实凭据。
- `deploy/tests/prod001-immutable-promotion-static.ps1`：PROD-001 静态/fixture 契约检查。

### 实现契约与安全边界

- TEST 只导出带 `SUCCESS` 的完整 release；active/candidate/release 的 version 与 source revision 必须一致。
- manifest、tar、部署资产和 image IDs 均校验 SHA-256/架构（`x86_64`）；产物缺失、摘要不匹配或同版本元数据冲突即阻断。
- 两端连接均为 key-only，使用独立 `known_hosts` 与主机指纹；生产连接全程通过 `ProxyJump`，配置不硬编码地址凭据。
- 生产路径禁止 build、pull、prune、SQL 和 DNS API；只执行 `docker load` 与 `docker compose --no-build`，失败保留并可回滚到既有成功版本。
- runtime 校验拒绝 shell 注入和不安全路径；导出/晋级使用 `flock` 防并发，成功后清理专用 inbox；同版本重试必须先经过受控恢复流程。
- 域名只验证 A 记录与 HTTPS 可达性，不在本任务申请或配置 TLS、DNS、安全组。

### 未验证项与风险

- `NEEDS_GATE_BOOTSTRAP`：TEST/PROD root 门禁、sudoers 与受信脚本尚未在服务器安装或验证。
- `NEEDS_SERVER / NOT_DEPLOYED`：未连接 229 或 `118.190.150.161`，未上传、构建、加载、启动或浏览器验收。
- `NEEDS_DNS_TLS`：未执行 DNS、证书、反代、80/443 或安全组检查。
- 未执行 SQL、生产数据库初始化、Git 提交或推送。
- 当前 `0.1.13` 不是带 SUCCESS 且校验完整的可晋级成功 release，禁止据此进行生产晋级。
