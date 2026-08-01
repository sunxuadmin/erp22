# 2026-07-31 部署预检脚本修复

## 状态

- 需求：`REQ-007`、`REQ-010`
- 当前状态：`VERIFIED / COMMITTED / ARCHIVE_READY / PUSH_NOT_AUTHORIZED`
- 脚本修改：已于2026-07-31确认
- 192.168.2.229修复后只读预检：已于2026-07-31授权并完成
- Swap配置、资产暂存、构建、容器和SQL：未授权
- 本次修复和任务记录已进入本地提交；新的Git推送未授权

## 目标

修复已由TEST服务器只读预检证实的两个问题，使Windows部署入口能够通过普通SSH握手采集并严格校验已确认的主机指纹，同时保证Linux预检不在远端创建临时文件。

## 实施依据

- Windows系统OpenSSH的 `ssh-keyscan` 与目标Debian OpenSSH 9.2协商失败，普通Windows `ssh` 握手可取得主机密钥；
- Git for Windows的 `ssh-keyscan` 虽可工作，但不应让部署入口依赖另一套可执行文件；
- 修复前 `deploy/linux/crehn-deploy.sh` 的预检使用 `mktemp` 和删除临时文件，不符合只读预检约束；
- 部署阶段仍需要保存项目A部署前后基线文件并执行逐字节比较，不能删除现有证据能力。
- 使用真实 `test.env` 重跑预检时，远端 Bash 将 UTF-8 BOM 误作为首行命令；本次虽未影响返回码，但标准输入脚本必须在发送前移除 BOM。

## 范围

- `deploy/scripts/invoke-deployment.ps1`：
  - 使用现有 `ssh` 进行禁止认证的主机密钥握手；
  - 将握手得到的主机密钥写入本地临时known-hosts；
  - 继续使用 `ssh-keygen` 核对用户已确认的SHA256指纹；
  - 实际SSH/SCP会话继续强制 `StrictHostKeyChecking=yes`。
  - 标准输入脚本在 LF 规范化后移除开头 UTF-8 BOM，并保留安全结尾。
- `deploy/linux/crehn-deploy.sh`：
  - 提取唯一的容器基线输出实现；
  - 只读预检在内存中统计和显示基线，不创建临时文件；
  - 部署阶段原有前后基线文件及比较行为保持不变。

## 禁止范围

- 除本任务已授权的192.168.2.229只读预检外，不连接ECS 1或ECS 2；
- 不配置Swap，不安装软件，不暂存远端资产；
- 不构建镜像，不创建、启动、停止或修改容器；
- 不执行SQL，不修改防火墙、DNS、80/443；
- 不修改业务代码、Compose服务、环境变量字段或数据库结构；
- 不提交或推送Git。

## 验收

- [x] Windows部署入口不再依赖 `ssh-keyscan`；
- [x] 未匹配已确认指纹时仍然阻断，正式会话仍启用严格主机密钥校验；
- [x] Linux `preflight` 路径不使用 `mktemp`、删除或普通文件重定向；
- [x] 部署阶段基线文件写入和部署前后比较保持不变；
- [x] PowerShell语法解析无错误；
- [x] Bash语法检查通过；
- [x] Git差异检查和敏感信息检查通过；
- [x] 仅执行已授权的192.168.2.229只读预检，未执行服务器写操作、构建、容器变更、SQL、Git提交或推送。
- [x] 使用真实 `test.env` 的标准输入预检不再出现 UTF-8 BOM 命令诊断。

## 实际变更与证据

### 修改文件及原因

- `deploy/scripts/invoke-deployment.ps1`：移除 `ssh-keyscan` 依赖，改为通过禁止所有认证方式的普通SSH握手把服务端主机密钥写入本地临时known-hosts；随后继续用 `ssh-keygen -E sha256` 与已确认指纹匹配，正式SSH/SCP参数继续使用 `StrictHostKeyChecking=yes`；兼容Windows PowerShell 5.1对原生程序标准错误的处理；标准输入脚本先规范为LF、移除源内容的开头BOM并追加安全结尾，随后以无BOM UTF-8本地临时文件重定向到 `ssh`，避免PowerShell管道向原生命令写入BOM，临时文件在会话结束时精确清理；
- `deploy/linux/crehn-deploy.sh`：新增唯一的 `inspect_project_containers` 标准输出实现；只读预检在内存中统计与显示容器基线，部署阶段的 `capture_project_baseline` 继续写入前后证据文件；允许 `bash -s` 标准输入运行时在 `BASH_SOURCE[0]` 未定义的情况下完成只读预检；
- `TASKS.md`：登记本活动任务；
- 本任务卡：记录授权边界、实现范围、验证证据和剩余风险。

### 验证

- PowerShell AST语法解析：`PASS`，错误数0；
- `bash -n deploy/linux/crehn-deploy.sh`：`PASS`；
- Bash `--help` 冒烟：前两次因Codex PowerShell调用环境未把Git Bash的 `/usr/bin` 加入其内部 `PATH`，`dirname`、`cat` 不可用而失败；显式使用 `PATH=/usr/bin:/mingw64/bin:$PATH` 后：`PASS`。该失败属于本地调用环境，不是脚本语法或业务分支失败；
- 静态边界断言：`PASS`，PowerShell无 `ssh-keyscan`、探测阶段禁用认证、SHA256指纹校验和正式会话严格校验均保留；
- 静态边界断言：`PASS`，`preflight` 不含 `mktemp`、临时删除或文件型基线调用，部署前后基线捕获及 `cmp -s` 比较均保留；
- `git diff --check`：`PASS`；
- 修改差异中的私钥头和AWS样式访问密钥扫描：`PASS`；
- 第一次运行因显式指定的 `deploy/env/test.env` 不存在，在本地参数解析阶段阻断，未连接服务器；随后仅为只读预检使用仓库中的 `test.env.example` 提供非敏感运行边界参数；
- 第一次真实握手成功取得229的ED25519主机密钥，但Windows PowerShell 5.1把SSH诊断标准错误转为终止异常；未执行远端命令，遗留的一个本地临时known-hosts已精确清理；
- 修复标准错误兼容后，远端预检的全部读取项已通过，但标准输入模式暴露 `BASH_SOURCE[0]` 未定义和PowerShell结尾CRLF问题，最终返回127；该次已完成的读取结果显示项目A正常；
- 修复标准输入兼容后再次执行真实入口：`PASS`，返回码0，输出 `preflight-local completed without changing the host` 和 `action=PreflightLocal completed`；
- TEST主机：4核、6063 MiB内存、0 MiB Swap、根分区剩余47955 MiB、Docker 29.6.2、Compose 5.3.1；
- 候选端口28181、29000、29001：均空闲或仅允许归属 `crehn-test`；
- 项目A `dyz-current-shadow`：5个容器ID与前一次完整读取及历史基线一致，全部 `running/healthy`、重启0、`OOMKilled=false`；
- 项目A Web 18181和MinIO 19000健康入口：`PASS`；
- 本地临时known-hosts和ASKPASS文件：最终检查无遗留；
- 远端预检通过标准输入运行，修复后的 `preflight` 不调用 `mktemp`、删除或普通文件写入；未创建CREHN目录、容器、网络、卷或Swap；
- 使用真实 `deploy/env/test.env` 的首次预检虽返回0，但发现PowerShell标准输入传输附加UTF-8 BOM，远端Bash在脚本初始化前输出一次无效命令诊断；随后确认源脚本原始字节不含BOM，改用无BOM UTF-8临时标准输入文件重定向；
- BOM修复本地验证：PowerShell AST通过；开头BOM移除模拟通过；无BOM临时输入文件原始前缀为 `#!/`；原生进程重定向冒烟通过；`git diff --check`通过；
- BOM修复后使用真实 `test.env` 重跑229 `PreflightLocal`：返回码0，无BOM诊断；主机为4核、6063 MiB内存、4095 MiB Swap、根分区剩余41173 MiB；28181、29000、29001符合 `crehn-test` 归属约束；项目A当前5个容器均 `running/healthy`、重启0、`OOMKilled=false`，Web与MinIO入口健康；入口输出 `preflight-local completed without changing the host`；
- 构建、资产暂存、容器变更和SQL：`NOT_RUN`；
- Git本地提交：`AUTHORIZED`；Git推送：`NOT_RUN`。

### 依赖、接口、配置和数据库

- 新增第三方依赖：无；
- 新增业务接口、环境变量、Compose服务和数据库变更：无；
- 远端文件、服务器配置和运行状态变更：无。

### 尚未处理的风险

- Swap和真实 `deploy/env/test.env` 前置条件现已具备，但资产暂存与构建仍需分别获得授权；
- BOM修复、TEST环境生成任务记录和索引尚未提交；Git提交与推送需要用户单独授权。
