# 2026-07-31 TEST资产暂存sudo提权

## 状态

- 需求：`REQ-007`、`REQ-010`
- 当前状态：`VERIFIED / COMMIT_NOT_AUTHORIZED / BUILD_NOT_AUTHORIZED`
- 用户已授权：TEST资产暂存，以及仅在TEST资产安装步骤使用已验证的 `sudo -n`
- 构建、容器、SQL、生产操作、Git提交和推送：未授权

## 目标

使 TEST 资产暂存入口能够以当前 SSH 用户完成临时目录和上传，再以无交互 `sudo -n` 执行需要 root 的资产安装脚本。

## 实施依据

- 资产暂存已在本地归档检查通过后进入远端安装脚本，但该脚本要求 UID 0；
- 229 已实测 `sudo -n true` 返回0，无需传递或记录 sudo 密码；
- 远端临时目录、上传和清理不需要 root，应继续由当前 SSH 用户负责。

## 范围与限制

- 仅 TEST 的 `install-assets.sh` 标准输入调用前增加 `sudo -n`；
- 不改变生产路径、SSH认证、主机密钥校验、归档内容、Compose、构建、容器、数据库或部署动作；
- 仅重试已授权的 `StageTestAssets`，不推导为构建或部署授权。

## 验收

- [x] TEST资产安装脚本以 `sudo -n` 调用；
- [x] 资产暂存成功，且未执行构建、容器或SQL；
- [x] 远端临时目录在结束后清理。

## 验证

- 本机 PowerShell AST、TEST分支 `sudo -n` 字面断言和 `git diff --check`：`PASS`；
- `pwsh 7.6.3` 运行 `StageTestAssets`，确认串为 `STAGE:crehn-test:2de841e680ab0b76fef104c1cec0c21032b5f4d3`：`PASS`；
- 入口输出资产已以 `test-source` 模式暂存到 `/srv/crehn-test`，修订为 `2de841e680ab0b76fef104c1cec0c21032b5f4d3`；未运行构建、容器或SQL；
- 229只读回检：部署根、源码和运行时目录均为 `root:root:700`，`runtime/test-0.1.0-test.env` 为 `root:root:600`；CREHN容器、网络、卷数量均为0，`/tmp/crehn-stage-*` 临时目录数量为0；
- Git提交和推送、构建、容器、SQL和生产操作：`NOT_RUN`。
