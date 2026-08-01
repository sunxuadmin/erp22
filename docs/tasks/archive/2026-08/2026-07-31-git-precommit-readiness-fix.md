# 2026-07-31 Git 提交前就绪修复

## 状态

- 需求：`REQ-008`、`REQ-010`
- 当前状态：`VERIFIED / COMMITTED / ARCHIVED`
- 文件修改：已授权
- Git精准暂存与本地提交：已于2026-07-31获得授权
- Synology裸仓库和远端配置已由后续独立任务完成；新的推送未授权
- 构建、容器、SQL和服务器操作：未授权

## 目标

修复当前已提交源码无法完整复现管理端依赖安装的问题，并将项目文档中的 Git 状态校正为只读审计确认的当前事实。

## 实施前基线

- 当前分支：`main`；
- 已有本地提交：5个；
- 当前提交：`08d3ba25bebbfab2949ad9873614d36db15cd0d5`；
- 工作区：实施前干净；
- 远端、标签、上游分支：均未配置；
- 目标 Synology 裸仓库 `C:\Users\A\Documents\SynologyDrive\GIT\CREHN\CREHN.git`：尚不存在；
- `frontend/pnpm-lock.yaml` 存在于本地但被忽略，当前提交不包含该文件；
- `deploy/web/Dockerfile` 使用 `pnpm install --frozen-lockfile`，并明确复制管理端锁文件。

## 范围

- 取消忽略并纳入后续提交范围的 `frontend/pnpm-lock.yaml`，不改写锁文件内容；
- 为管理端和门户固定实际使用的 `pnpm@11.9.0`；
- 校正需求索引和既有任务卡中“尚未初始化 Git”等已过时表述；
- 记录本次变更和静态检查证据。

## 禁止范围

- 不打标签或推送；Git暂存和本地提交仅限本任务列出的9个文件；
- 不创建 Synology 裸仓库，不配置远端；
- 不构建前后端或镜像，不启动容器；
- 不执行 SQL，不连接 TEST 或 ECS 2；
- 不修改 DYZ，不处理与本次就绪修复无关的代码。

## 验收

- [x] `frontend/pnpm-lock.yaml` 不再被 Git 忽略，且文件内容哈希保持不变；
- [x] 管理端与门户 `package.json` 均固定 `pnpm@11.9.0`；
- [x] 两个 `package.json` 可解析，锁文件根导入项与依赖声明一致；
- [x] 需求索引和既有任务卡准确反映当前 Git 状态；
- [x] 提交授权前Git暂存区保持为空，且未配置远端、未创建提交或推送；
- [x] 精准暂存后的文件清单与本任务范围一致；
- [x] `git diff --check` 通过。

## 变更与验证证据

### 修改文件及原因

- `frontend/.gitignore`：停止忽略管理端 pnpm 锁文件，使后续完整提交能够满足 Docker 的冻结安装要求；
- `frontend/pnpm-lock.yaml`：文件内容未改写，仅由“已忽略”变为本次精准提交范围；
- `frontend/package.json`、`portal/package.json`：固定实际使用的 `pnpm@11.9.0`；
- `docs/requirements/CREHN_REQUIREMENTS.md`：校正 `REQ-008` 的 Git 现状；
- `docs/tasks/active/2026-07-30-predeployment-local-implementation.md`、`2026-07-30-project-b-isolated-compose-assets.md`：移除已经失效的“尚未建立Git工作树”表述，并保留新增提交、远端和部署的独立门禁；
- `TASKS.md`、本任务卡：登记本次实施边界、状态和证据。

### 依赖、接口、配置和数据库

- 新增第三方依赖：无；
- 依赖版本变化：无；
- 包管理器元数据：管理端和门户新增 `packageManager: pnpm@11.9.0`；
- 业务接口、环境配置和数据库变更：无；
- SQL执行：无。

### 已执行检查

- `frontend/pnpm-lock.yaml` 实施前后 SHA-256 均为 `177c9b1c11bfedaad8460e395c3d7b80ac0a0bfa89bc9ca2195217bb34516cbe`：`PASS`；
- `git check-ignore` 确认管理端锁文件不再被忽略：`PASS`；
- 管理端和门户 `package.json` JSON解析及 `packageManager` 精确值检查：`PASS`；
- 管理端和门户锁文件根导入项与 `dependencies`、`devDependencies`、`optionalDependencies` 逐项核对：`PASS`；
- 过时 Git 状态文本检索：`PASS`；
- `git diff --check`：`PASS`；
- Git暂存区、远端、标签均为空；分支仍为 `main`，HEAD和提交数未变化：`PASS`；
- 前后端构建、容器、SQL、服务器与浏览器验证：`NOT_RUN`，不在本次授权范围。

### 尚未处理的风险

- 实施前提交 `08d3ba25bebbfab2949ad9873614d36db15cd0d5` 不包含管理端锁文件；本次提交完成后，后续构建必须使用包含该锁文件的新提交，不能继续使用实施前提交；
- Git提示部分工作区文本文件在未来由 Git处理时可能按本机设置从 LF 转为 CRLF；本次未新增 `.gitattributes`，因为不在已确认的最小修复范围内；
- 尚未运行构建；本次仅修复并验证依赖输入的可复现性元数据。
