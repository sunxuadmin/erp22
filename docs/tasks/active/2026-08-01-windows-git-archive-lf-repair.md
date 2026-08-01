# 2026-08-01 Windows Git归档LF修复

## 状态

- 需求：`REQ-007`、`REQ-010`
- 当前状态：`VERIFIED_LOCAL / COMMIT_APPROVED / BUILD_NOT_AUTHORIZED`
- Git提交：已授权（本地分类提交，不推送）
- 数据库执行：未授权
- 部署：未授权

## 目标

修复Windows本机在`core.autocrlf=true`时通过正式入口生成源码归档会把Git对象中的LF Shell脚本转换为CRLF的问题，确保发送到229的源码归档保持已提交Git对象的原始字节。

## 实测依据

- `0.1.1-test`数据库初始化在`002_bootstrap_admin.sh`失败，远端暂存源文件和镜像内文件均包含CRLF；
- `003_bootstrap_test_oss.sh`同样包含CRLF；
- 修订`5f543a06d861d5bb2afca9734c7c2a8d4e80aed3`中的两个Git对象和本地工作文件均为LF；
- 默认`git archive`导出哈希与Git对象不同并包含CRLF；使用`git -c core.autocrlf=false archive`后导出哈希与Git对象完全一致。

## 范围

- `deploy/scripts/invoke-deployment.ps1`：仅在唯一源码归档命令上固定`core.autocrlf=false`；
- `TASKS.md`及本任务卡：记录授权范围、故障证据和验证结果；
- 不增加`.gitattributes`，不触发全仓库换行重规范化，不修改数据库引导脚本内容。

## 禁止范围

- 不连接服务器，不暂存TEST资产，不构建新镜像；
- 不停止、清理、恢复或修改当前部分初始化数据库；
- 不执行SQL，不部署其他服务；
- 仅按用户授权执行本地分类提交，不推送Git，不覆盖已有未提交文档修改。

## 验收

- [x] PowerShell脚本AST解析通过；
- [x] 正式归档命令显式使用`core.autocrlf=false`；
- [x] 两个数据库Shell脚本的归档内容均为LF且SHA-256与Git对象一致；
- [x] 默认`core.autocrlf=true`环境仍保持不变；
- [x] `git diff --check`和敏感信息扫描通过；
- [x] 服务器、构建、SQL、数据库清理和Git推送均为`NOT_RUN`；本地分类提交已获单独授权。

## 实际变更与证据

### 修改文件与原因

- `deploy/scripts/invoke-deployment.ps1`：在唯一源码归档命令增加命令级`core.autocrlf=false`，使归档保持Git对象字节，不改变用户全局或仓库Git配置；
- `TASKS.md`、本任务卡：登记修复边界、实测根因和验证证据。

### 验证结果

- Windows PowerShell 5.1 AST解析：`PASS`；
- 当前PowerShell运行时AST解析：`PASS`；
- 静态断言：唯一源码归档执行显式包含`git -c core.autocrlf=false -C $projectRoot archive`：`PASS`；
- `deploy/db/bootstrap-admin.sh`：归档SHA-256与Git对象同为`2d80e505be536b7d4d196f496e421146d03e5dad0ae7d9363415061403cd9de3`，`LF-only`；
- `deploy/db/bootstrap-test-oss.sh`：归档SHA-256与Git对象同为`f2971c2a87689b82812b224be7e77ced8b0ec8cbd210bed51589bdb707926835`，`LF-only`；
- 本机`core.autocrlf`在检查前后均为`true`，修复未修改用户配置：`PASS`；
- `git diff --check`：`PASS`；
- 本次差异私钥头、访问密钥和秘密字面量扫描：`PASS`；
- 修改范围检查：除既有未提交文档外，唯一代码文件为`deploy/scripts/invoke-deployment.ps1`，Git索引未变化：`PASS`。

### 远端状态与后续门禁

- 本任务实施阶段未连接229、未构建、未执行SQL或数据库清理；
- 此前失败的`0.1.1-test`数据库容器仍运行且健康，但只有25张框架表、`sys_user=0`、无完成标记；
- 已校验备份`20260801T092450Z-0.1.0-test-pre-reinitialize`保持可恢复；
- `0.1.1-test`属于不可变但数据库初始化不可用的产物，不得原地修补；
- 本修复提交后，使用新版本`0.1.2-test`重新暂存和构建仍需单独授权；数据库隔离与初始化SQL也需再次授权。

### 授权状态

- 脚本和任务文档修改：已授权并完成；
- Git提交：已授权（本地分类提交）；推送：未授权、未执行；
- 服务器连接、构建、数据库清理、SQL和部署：未授权、未执行。
