# OPS-003 TEST root 部署门禁

## 状态

- 需求：2026-08-07 已确认，仅为 TEST 部署建立 root 受限执行门禁。
- 当前状态：`IMPLEMENTED / STATIC_VERIFIED / SERVER_NOT_RUN`
- Git提交：已授权（本地；推送仍未授权）
- 数据库执行：未授权
- 服务器连接、TEST 门禁 bootstrap/验证与 `crehn-test` 部署：已授权；认证方式尚未确认，不假定 SSH 密码等于 sudo 密码。

## 目标

在 TEST 主机将需要 root 权限的 stage、build、deploy、verify 固定为四个无参数门禁动作；Windows 控制器仅在门禁已安装时以 `sudo -n /usr/local/sbin/crehn-test-deploy-gate <action>` 请求这些动作。门禁只可操作 `/srv/crehn-test` 的 `crehn-test` Compose 项目，且不能以 root 执行可由调用者修改的源码脚本。

## 允许修改范围

- `deploy/scripts/invoke-deployment.ps1`
- `deploy/linux/install-assets.sh`
- `deploy/linux/crehn-deploy.sh`（仅确有必要）
- `deploy/linux/` 下本任务新增的门禁和安装脚本
- `deploy/sudoers/` 下本任务新增模板
- `deploy/tests/` 下本任务新增定向测试
- 本任务卡

## 禁止范围

- 不执行生产、数据库或其他 SQL；门禁 bootstrap、验证和 `crehn-test` 部署仅在认证方式明确并由主线程继续派发时执行。
- 不修改生产、数据库、回滚、清理路径或 `dyz-current-shadow`。
- sudoers 不授予 `bash`、`bash -s`、`docker`、`docker compose`、`ALL` 或通配参数的 `NOPASSWD`。
- 未安装门禁不得自举；首次安装仅可由独立管理员 bootstrap 完成，且不能假定 SSH 密码等于 sudo 密码。
- 不新增第三方依赖、Git推送或文件删除。

## 安全验收

- [ ] sudoers 仅包含 root:root 且调用用户不可写的 gate 绝对路径，以及 stage/build/deploy/verify 四条精确无通配参数命令。
- [ ] gate 拒绝 prod、database、rollback、cleanup、任意路径、额外参数和非 TEST Compose 项目。
- [ ] gate 仅调用 `/usr/local/libexec/crehn-test/` 下受信 root-owned 脚本，不以 root 执行 `/srv/source` 内容。
- [ ] stage 候选归档拒绝符号链接、硬链接、非普通文件、owner/mode 不符、绝对路径、`..`、范围外条目，并校验版本、完整 revision 和 SHA-256。
- [ ] 保留 Apply + 精确确认串、不可变版本、`dyz-current-shadow` 前后基线和无 SQL 语义。
- [ ] PowerShell 和静态安全测试通过；服务器/运行时验证明确标注 `NOT_RUN`。

## 实际变更与证据

- `deploy/scripts/invoke-deployment.ps1`：TEST stage 把固定三份候选文件上传至调用用户的固定 inbox 并调用 `sudo -n /usr/local/sbin/crehn-test-deploy-gate stage`；build/deploy/verify 只映射到 gate 的同名精确动作。原有数据库、回滚和清理入口未被本任务重写或赋权。
- `deploy/linux/crehn-test-deploy-gate.sh`：仅接受一个 `stage|build|deploy|verify` 参数，绑定 `/srv/crehn-test`、`crehn-test`、配置的 sudo 调用用户和 root-owned libexec 脚本；拒绝额外参数及生产/数据库/回滚/清理。
- `deploy/linux/crehn-test-stage-candidate.sh`：在 operator inbox 先检查目录和三份候选文件的非链接、类型、owner、mode；复制到 root-owned `0700` 固定临时目录后才校验 metadata、完整 revision、SHA-256 与 tar 路径/链接类型，随后将 root-owned副本交给可信安装器并清理该固定临时目录。
- `deploy/linux/install-assets.sh`：普通 stage 不再更新 `/usr/local/libexec/crehn-test`；可信 helper/Compose 仅能由管理员 bootstrap 安装或更新。stage 只更新 `/srv/crehn-test` 候选和 root-owned version/revision 状态。
- `deploy/linux/crehn-deploy.sh`：当作为可信 helper 安装时，Compose 文件仍从 root-owned libexec 读取，并以显式 `/srv/crehn-test/source` project directory 提供构建上下文；不加载 source 下的 Compose 文件或执行 source 下脚本。
- `deploy/linux/install-crehn-test-deploy-gate.sh`、`deploy/sudoers/crehn-test-deploy-gate`：独立管理员 bootstrap 先把审查资产复制至 root-owned 快照，再安装 helper、配置、inbox 和只列四条精确 gate 命令的 sudoers；sudoers 使用 root-owned 临时文件，`visudo -cf` 成功后原子替换最终规则。
- `deploy/tests/ops003-test-root-deployment-gate-static.ps1`：验证 sudoers 禁止项、gate 边界、root copy-before-validate、普通 stage 不改 trusted assets、可信 Compose/source 路径和 PowerShell AST/映射。

### 本地验证

- `powershell -ExecutionPolicy Bypass -File deploy/tests/ops003-test-root-deployment-gate-static.ps1`：`PASS`。
- `git diff --check`：`PASS`。
- Shell 语法：`NOT_RUN`，本机没有可用 `bash`；服务器安装前应以目标主机 Bash 再执行 `bash -n` 与 `visudo -cf`。
- 服务器、sudoers 安装、真实 gate 运行、Docker/Compose、`dyz-current-shadow` 前后基线和浏览器：`NOT_RUN`。

### 2026-08-07 服务器门禁验证与 Stage 重试状态

- gate 安装、`sudoers` 解析、gate `verify` 与 TEST preflight：`PASS`（服务器证据由主线程保留）。
- 首次 `StageTestAssets`：`BLOCKED`，控制器对已存在的固定 `/home/dyz/crehn-test-inbox` 使用非幂等 `mkdir -m 700`，在进入 gate 前以 `File exists` 退出；未执行 stage、build、deploy 或 SQL。
- 本次修复：改为固定路径 `mkdir -p -- <inbox>` 后紧接 `chmod 700 -- <inbox>`，不含通配、删除或路径参数；静态验证通过，待主线程授权后重试 stage。

### 管理员 bootstrap（后续已授权服务器步骤）

1. 使用独立管理员认证进入 TEST；SSH 认证不推定为 sudo 认证。
2. 从已审查的部署资产目录以 root 执行：`install-crehn-test-deploy-gate.sh --operator <TEST SSH operator> --source-deploy-dir <reviewed deploy dir>`。
3. 管理员回读 `/usr/local/sbin/crehn-test-deploy-gate`、`/usr/local/libexec/crehn-test/`、`/etc/crehn-test-deploy-gate.conf` 的 `root:root` 和权限，并运行 `visudo -cf /etc/sudoers.d/crehn-test-deploy-gate`。
4. 以配置 operator 使用 `sudo -n /usr/local/sbin/crehn-test-deploy-gate verify` 先验证拒绝/允许边界；只有后续明确派发时才 stage/build/deploy。部署前后均回读 `dyz-current-shadow` 基线，且不执行 SQL。

## 风险与未验证项

- Bootstrap 接受的 `<TEST SSH operator>` 必须由主线程在连接前确认；本任务没有假定 SSH 密码可用于 sudo。
- 可信 helper/Compose 的更新需要独立管理员 bootstrap，普通候选 stage 故意不会更新它们；因此门禁资产升级与应用候选发布是分开的审查步骤。
- bootstrap 会要求 `/usr/local`、`/usr/local/sbin`、`/usr/local/libexec`、`/srv`、`/srv/crehn-test`、`runtime` 和 `state` 均为非链接、`root:root` 且不可组/全局写；若 TEST 现有目录由部署用户拥有或权限宽松，bootstrap 将安全拒绝，需管理员先盘点并单独确认收紧影响。
- 本机无 Bash，Shell 语法和真实 sudoers 解析尚未在 Linux 验证；静态检查不能替代服务器、Docker、protected-project 或浏览器证据。
