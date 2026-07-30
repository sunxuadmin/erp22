# 2026-07-30 项目B独立 Compose 部署资产

## 状态

- 需求：`REQ-006`、`REQ-007`、`REQ-010`、`REQ-012`
- 当前状态：`IMPLEMENTED / SERVER_VERIFICATION_PENDING`
- 文件修改：已授权
- 本地构建：未授权
- 本地容器部署：未授权
- 数据库执行：未授权
- 192.168.2.229 写操作：未授权
- ECS 2 连接与部署：未授权
- DNS、80/443、安全组和防火墙：未授权
- 现有Git仓库：2026-07-31提交前只读审计时，`main` 已有5个本地提交（非本任务执行）
- 新增Git提交与推送：未授权

## 目标

为 CREHN 项目B建立可审计、可重复执行且与项目A完全隔离的 Docker Compose 部署资产，使后续能够分别执行只读预检、本地构建、TEST部署、健康验收、生产预演、生产发布、数据库操作、回滚和备份维护。

## 已确认配置

- TEST Compose 项目名：`crehn-test`；
- TEST 部署目录：`/srv/crehn-test`；
- TEST Web：`192.168.2.229:28181`；
- TEST MinIO API：`192.168.2.229:29000`；
- TEST MinIO Console：`127.0.0.1:29001`；
- TEST 使用独立 MariaDB、Redis、MinIO、网络、目录和凭据；
- TEST 不增加独立虚拟磁盘，仅用于程序测试；
- 后端运行镜像安装 LibreOffice、中文字体和 ffmpeg；
- 生产对象存储使用阿里云 OSS；
- 生产 ECS 2 地址、SSH用户、端口和凭据由受限环境文件提供，当前不得连接；
- 本地暂按 6 GiB 内存条件检查，4 GiB Swap 仅作为后续服务器变更建议，不在本任务执行。

## 范围

- 调整 Compose、环境模板、Dockerfile和构建上下文；
- 解除数据库 SQL 与 MariaDB 首次启动的自动耦合；
- 增加 TEST MinIO和必要持久化目录；
- 为TEST设置内存限制、线程池覆盖和局域网端口；
- 为生产设置阿里云 OSS、不可变产物和ECS 2占位配置；
- 建立九类独立部署入口及项目A前后基线对比；
- 建立仅统计成功完整回滚点的备份保留规则；
- 清理长期规范中把旧IP直接视为CREHN生产机的表述。

## 禁止范围

- 不修改 `C:\Users\A\Documents\DYZ`；
- 不创建、停止、重启或删除任何容器；
- 不执行 Docker build、pull、save、load 或 Compose up；
- 不执行数据库初始化、迁移、导入、回滚或验证 SQL；
- 不写入或调整 192.168.2.229；
- 不连接或修改 ECS 1、ECS 2；
- 不安装 Swap、Docker、1Panel或其他宿主机软件；
- 不修改防火墙、Docker daemon、DNS、安全组、80/443；
- 不创建真实 `.env`，不写入真实密码、密钥或OSS凭据；
- 不提交或推送 Git。

## 验收

- [x] Compose 静态结构体现项目名、目录、网络、端口、服务和资源隔离；
- [x] MariaDB首次启动不再自动执行CREHN业务SQL；
- [x] TEST MinIO、工作台资源和门户PDF具备独立持久化目录；
- [x] Backend镜像包含已确认的Office和媒体处理运行包；
- [x] 九类入口默认安全，数据库和生产动作必须显式Apply；
- [x] 项目A基线包含容器ID、镜像、健康、端口、挂载、重启和OOM状态；
- [x] 备份仅将校验通过且带SUCCESS标记的目录计入最近3次；
- [x] PowerShell、Bash、JSON和YAML完成当前环境可执行的静态检查；
- [x] 本任务未执行构建、容器、SQL、服务器、ECS 2、Git提交或推送。

## 实际变更与证据

### 部署结构

- `deploy/compose.yml`：MariaDB、Redis、Backend和Web共用基础层；不固定容器名，按项目名生成独立网络，全部数据使用项目目录下的绑定挂载和可调内存限制。
- `deploy/compose.test.yml`：仅TEST增加独立MinIO和一次性桶初始化；API使用29000，Console使用29001，Web使用28181。
- `deploy/compose.prod.yml`：不包含MinIO，Backend必须读取阿里云OSS环境配置，Web默认由生产环境文件约束为回环地址。
- `deploy/.env.example`：只保存TEST/ECS 2 SSH控制字段，包含IP、用户、密钥/密码方式和必须确认的主机指纹。
- `deploy/env/test.env.example`、`prod.env.example`：TEST/PROD运行配置分离；真实文件分别部署为`root:root:600`，不进入Git。
- `backend/Dockerfile`：构建阶段执行Maven verify；运行阶段安装LibreOffice Writer/Calc/Impress、中文字体、fontconfig和ffmpeg。
- `deploy/web/Dockerfile`、`nginx.conf.template`：管理端和门户顺序构建；Nginx上传请求上限从环境变量生成。
- `deploy/db/Dockerfile`：初始化清单移至`/opt/crehn/init`，不再放入MariaDB自动初始化目录。
- `deploy/db/bootstrap-admin.sh`、`bootstrap-test-oss.sh`：首次初始化入口显式创建管理员并在TEST配置独立MinIO；均不随普通容器启动自动执行。

### 执行入口和保护

- `deploy/linux/crehn-deploy.sh`：统一实现只读预检、TEST顺序构建、TEST部署、TEST健康验收、生产预检、生产发布、数据库计划/首次初始化、回滚和备份维护。
- `deploy/linux/install-assets.sh`：只负责已授权的源码/部署资产暂存；TEST传源码，PROD只传部署资产和已验证镜像归档。
- `deploy/scripts/invoke-deployment.ps1`：Windows统一调用；SSH密钥或`SSH_ASKPASS`密码认证、主机指纹固定、TEST/ECS 2目标分离。
- 资产暂存使用 `git archive` 从明确提交生成，不打包当前未提交工作区；提交号不匹配或提交未包含完整构建输入时直接阻断。
- 新版本运行环境先保存为带版本文件，发布成功后才晋级为`test.env`/`prod.env`；发布前备份读取旧生效环境、旧容器镜像和旧Compose，避免把待发布配置误当回滚点。
- `deploy/scripts/verify-release.ps1`：校验清单版本、提交号、架构、镜像ID和归档SHA-256。
- 项目A基线比较使用Compose项目标签记录容器ID、镜像、健康、端口、挂载、重启和OOM状态；找不到项目A时直接阻断。
- 构建使用全机锁并顺序执行；资源必须满足约8GiB+2GiB Swap或约6GiB+4GiB Swap，磁盘至少20GiB可用。
- 所有写操作要求独立`Apply`和精确确认串；SQL只支持计划和空库首次初始化，未提供受审迁移清单时拒绝推测性迁移。
- 发布前创建项目B专属备份；仅校验完成并带`SUCCESS`的备份计数，清理只处理第4个及更早的成功备份，禁止Docker全局prune。

### 规范同步

- 工程规范、迁移发布规范、安全运维规范、ADR和REQ-010已把生产目标改为受限环境指定的专用ECS 2，删除把旧项目A生产IP作为CREHN生产机的现行表述。
- TEST固定`crehn-test`、`/srv/crehn-test`和28181/29000/29001；不增加虚拟磁盘。生产对象存储固定为阿里云OSS。

### 依赖、API、配置和数据库

- 新增第三方项目依赖：无。
- 新增或修改业务API：无。
- 新增数据库表/字段/索引：无。
- 部署配置：新增SSH控制模板、TEST/PROD运行模板、资源/端口/存储配置。
- 数据库执行行为：CREHN SQL从MariaDB自动首次启动中移除，改为独立、显式、一次性初始化入口；本轮未执行任何SQL。

### 已执行静态检查

- PowerShell Parser：`invoke-deployment.ps1`、`verify-release.ps1`均0个语法错误。
- Git Bash `bash -n`：两个Linux脚本通过。
- Bash帮助入口：PASS；无环境/无Apply的构建入口以`BLOCKED`退出。
- YAML解析：基础、TEST、PROD三个Compose文件通过；TEST/PROD所需变量均存在于对应模板。
- 迁移清单JSON解析和数据库Dockerfile全部COPY源文件存在：PASS。
- 部署目录未发现固定`container_name`、MariaDB自动initdb路径、`docker system/volume/network prune`：PASS。
- 未创建`deploy/.env`、`test.env`或`prod.env`；未写入真实凭据。

### 未验证和后续门禁

- `docker compose config/build/up`：`NOT_RUN`，本机无Docker；必须先在192.168.2.229执行只读预检。
- TEST资源、端口和项目A基线：`NEEDS_SERVER`；当前服务器快照不得视为最新事实。
- Backend运行镜像内LibreOffice、字体和ffmpeg实际调用：`NEEDS_SERVER`。
- 数据库首次初始化、迁移、备份和恢复：`NOT_RUN`，需要各自授权。
- 管理员和普通角色浏览器冒烟：`NEEDS_BROWSER`。
- ECS 2 IP、规格、系统、Docker/Compose、端口、安全组、域名和证书：`NEEDS_SERVER`；未连接ECS 2。
- 2026-07-31本次就绪修复提交前，`main` 已有5个本地提交，提交为 `08d3ba25bebbfab2949ad9873614d36db15cd0d5`，但无远端；后续构建必须使用经确认且包含完整构建输入的新提交。
- 新增Git提交、推送、TEST资产暂存、TEST构建、TEST部署、SQL、ECS 2资产暂存和生产发布均未获本轮授权。
