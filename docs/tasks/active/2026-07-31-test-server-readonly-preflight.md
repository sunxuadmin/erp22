# 2026-07-31 TEST服务器只读预检

## 状态

- 需求：`REQ-006`、`REQ-007`、`REQ-010`
- 当前状态：`VERIFIED / COMMITTED / SWAP_BLOCKER_RESOLVED / ARCHIVE_READY`
- 192.168.2.229只读连接与检查：已授权
- 当前SSH账号加入 `docker` 组：已于2026-07-31单独授权
- 其他服务器写操作、资产暂存、构建、容器和SQL：未授权
- 本任务记录的Git提交与推送：已于2026-07-31获得授权

## 目标

在不改变192.168.2.229和项目A状态的前提下，采集TEST主机资源、Docker能力、端口占用和项目A容器基线，为后续是否允许CREHN资产暂存与构建提供证据。

## 实施前基线

- TEST主机：`192.168.2.229:22`，端口可达；
- SSH服务：Debian OpenSSH 9.2；
- 已由用户确认ED25519指纹：`SHA256:jGk3tV2+Wr+W/ulTlqmcORcUSjeIvAYoAq7b9fuhbdM`；
- 认证方式：密码；用户名和密码只存在于被Git忽略的 `deploy/.env`，不写入任务卡或日志；
- `deploy/.evn` 曾因文件名错误未被Git忽略，连接前已精确更名为 `deploy/.env`，必填字段验证通过且未输出密码；
- 本地系统OpenSSH `ssh-keyscan`与目标KEX不兼容；本机Git for Windows OpenSSH扫描验证通过。

## 范围

- 读取主机名、系统、架构、内核、CPU、内存、Swap和磁盘；
- 读取Docker、Compose版本和Docker daemon可访问性；
- 读取Compose项目、容器ID、镜像、健康、重启、OOM、端口、挂载和网络；
- 确认项目A Compose项目及18181、19000入口健康；
- 检查候选端口28181、29000、29001是否空闲；
- 读取Docker磁盘占用和主机监听端口，不执行清理；
- 核对 `/srv/crehn-test` 是否已存在，但不创建或修改。

## 执行约束

- 除一次精确执行 `sudo usermod -aG docker <当前SSH账号>` 外，远端只执行读取命令且不使用 `sudo`；
- 不运行当前 `PreflightLocal` 包装入口，因为其现有实现会在远端使用 `mktemp`；本任务改用等价的内联只读检查，避免临时写入；
- 不执行 `docker build/pull/run/compose up/down/restart/stop/rm/prune`；
- 不创建目录、文件、网络、卷、容器或Swap；
- 不读取容器环境变量、数据库数据、密码、密钥或业务内容；
- 不连接ECS 1或ECS 2，不修改DYZ。

## 验收

- [x] SSH严格校验确认的主机指纹并成功登录；
- [x] 主机资源、Docker客户端和Compose客户端有当前证据；
- [x] 项目A容器清单、健康、端口、挂载、网络、重启和OOM状态有完整基线；
- [x] 项目A Web和MinIO健康入口正常；
- [x] 28181、29000、29001占用状态明确；
- [x] `/srv/crehn-test`存在状态明确；
- [x] 除已授权的Docker组成员关系外，未执行其他服务器写操作、构建、容器变更或SQL；
- [x] 给出资源、Swap和后续资产暂存/构建建议。
- [x] 当前SSH账号加入 `docker` 组并在新SSH会话中回读生效；

## 变更与验证证据

### SSH与凭据边界

- 用户确认ED25519指纹后，使用严格主机密钥校验和密码认证登录成功；
- `deploy/.evn` 因拼写错误未被Git忽略，连接前仅将其精确更名为 `deploy/.env`；
- `.env` 主机、端口、认证方式、指纹和必填字段检查通过，密码未输出；
- 本地临时known-hosts和SSH_ASKPASS文件已清理，密码环境变量已恢复；
- 系统OpenSSH `ssh-keyscan`因目标选择 `sntrup761x25519-sha512@openssh.com` 而失败；Git for Windows自带 `ssh-keyscan` 可正确取得RSA、ECDSA和ED25519主机密钥。本轮未修改部署脚本。

### 当前主机资源

- 主机名：`Develop`；
- 系统：Debian GNU/Linux 12，`x86_64`，内核 `6.1.0-49-cloud-amd64`；
- CPU：4核；
- 内存：6063 MiB，总可用内存快照为4362 MiB；
- Swap：0 MiB；
- 根分区：99575 MiB，总剩余48322 MiB，使用率50%；
- 负载：`0.04 / 0.18 / 0.36`；
- Docker客户端：29.6.2；Docker Compose客户端：5.3.1；
- Docker socket：`root:docker`、`srw-rw----`。

### 当前账号与阻断

- SSH账号UID为1000，调整前组为 `dyz,sudo,users`；
- 首次完整预检因无Docker socket权限，以 `BLOCKED docker_daemon_unavailable_to_user` 退出；
- 用户单独授权后，精确执行一次 `sudo usermod -aG docker <当前SSH账号>`；
- 第一次命令因空的sudo提示参数在SSH参数序列化中丢失，只输出sudo用法，`usermod`未执行；移除非必要参数后重试成功；
- 新SSH会话回读组为 `dyz,sudo,users,docker`，`docker info`、Docker Compose和全部只读Docker检查通过；
- 本地known-hosts、SSH_ASKPASS文件和密码环境变量均在每次连接后清理。

### 项目A容器基线

- Compose项目：`dyz-current-shadow`，状态 `running(5)`，配置文件 `/srv/dyz-current-shadow/context/scripts/deploy/docker-shadow/compose.shadow.yml`；
- 共5个容器，全部 `running/healthy`、重启次数0、`OOMKilled=false`，网络均为 `dyz-current-shadow_shadow-internal`；
- Web：容器 `e6a815934ae3285a8d4c83e6eaaa0ef4f38548b7b8794579e4d5c55c4f599261`，镜像 `dyz/art-review-web:current-w20dbab14626df062`，镜像ID `sha256:6949070054023773b45aa3333e961b0e159aa2d44067ab8f08bbc6ef62521b2d`，占用约5.18 MiB，端口 `192.168.2.229:18181 → 8080`，无挂载；
- Backend：容器 `06597796f2974ad61016999a5e41b7874fa17e419260ffe137e21f3bdf851278`，镜像 `dyz/art-review-backend:current-b22f818488bf7991a`，镜像ID `sha256:e7350b7f3d19761f03d9bb23c1e4ce90a100fe438bd46792599373f60c868c93`，占用约903 MiB，挂载 `/srv/dyz-current-shadow/backend/{temp,upload,workbench-assets,logs}`；
- MinIO：容器 `14a635d5e89764a43e0f47845878cc9df9f8599ddd9b9c781fd99660288291b6`，镜像摘要 `sha256:00c404465c4e7c4fd716d98caec2928e9e0f82c3548615662fdd3866ea9a28c0`，占用约116.7 MiB，挂载 `/srv/dyz-current-shadow/minio`，端口19000和回环19001；
- Redis：容器 `718f3873cc39ea5bb730fe46bb71365a17ad1a8c160c87800948b56e47a1f9dd`，镜像摘要 `sha256:c9d92d840fd011c908f040592857c724ae6d877f2aba5c40ad963276507386b2`，占用约13.16 MiB，挂载 `/srv/dyz-current-shadow/redis`；
- MariaDB：容器 `1a28ba6540f3db7611a4b714a3621787bc4c143db26a85b9e5241ef8d4ac57b6`，镜像摘要 `sha256:be981e4113326ada8d6004174dd09eeaefc03094037f811182a52d4f2e737350`，占用约169 MiB，挂载 `/srv/dyz-current-shadow/mariadb`；
- 项目A当前容器内存合计约1.18 GiB；本轮未读取容器环境变量、数据库内容、密钥或业务数据。

### 端口、入口和目录

- 项目A Web `http://192.168.2.229:18181/`：健康；
- 项目A MinIO `http://192.168.2.229:19000/minio/health/live`：健康；
- 18181、19000正在 `192.168.2.229` 监听，19001仅在 `127.0.0.1` 监听；
- CREHN候选端口28181、29000、29001：当前均空闲；
- `/srv/crehn-test`：当前不存在；
- Docker当前共有20个镜像、5个运行容器和56项构建缓存；镜像占5.143 GB，构建缓存占4.566 GB；
- 除已授权的一次Docker组成员关系调整外，未创建目录、临时远端文件、容器、网络、卷或Swap，未执行构建、SQL或Docker变更命令。

### 结论与后续门禁

- 磁盘余量满足“至少20 GiB”构建门槛；
- 当前约6 GiB内存但无Swap，不满足已确认的“约6 GiB内存+4 GiB Swap”构建门槛，构建必须继续阻断；
- 项目A完整容器基线已经形成，两个入口健康，候选端口空闲；
- 项目B按当前内存限制常态运行时可与项目A并存，但项目A当前约1.18 GiB、项目B容器限制合计约3.63 GiB，再计入系统和构建峰值后余量偏紧；必须禁止两项目同时构建，并在部署后监控内存、重启和OOM；
- 本地测试暂不需要增加100 GB独立虚拟磁盘；当前约48 GiB空闲足够进入测试，但不得执行Docker全局清理；
- 在单独授权并配置4 GiB Swap前，不得执行TEST构建；Swap配置是服务器写操作，不能由本次授权推导；
- 资产暂存仍是独立写操作，尚未授权；
- 即使加入 `docker` 组，后续现有部署脚本的写操作仍要求UID 0，正式部署账号和提权方式需要另行确认。

### Docker组权限变更授权

- 用户已单独授权将当前SSH账号加入 `docker` 组；
- 允许的唯一服务器写命令为 `sudo usermod -aG docker <当前SSH账号>`；
- 权限调整已在新SSH会话中回读生效，Docker只读访问通过；
- `docker` 组具有接近root的主机控制能力，后续需要按部署账号安全方案管理；本次授权未扩展到Swap、目录、构建、容器或其他系统配置。

### 依赖、接口、配置和数据库

- 新增依赖、业务接口、环境配置项和数据库变更：无；
- 本地敏感文件仅纠正文件名并保持Git忽略；
- 服务器写操作：仅执行已授权的当前SSH账号Docker组成员关系调整；
- SQL、构建、资产暂存、目录创建和容器变更：`NOT_RUN`。
