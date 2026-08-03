# REQ-010/REQ-013 TEST DeployLocal 入口与门户运行时配置修复

## 状态

- 需求：`REQ-010`、`REQ-013`
- 当前状态：`IMPLEMENTED / VERIFIED_LOCAL_STATIC / BUILD_BLOCKED_BY_WINDOWS_GIT_DIAGNOSTIC`
- Git提交：未授权
- 数据库执行：未授权
- 部署：未授权

## 目标

修复 TEST 正式 `DeployLocal` 入口对受保护暂存资产的权限适配，并确保独立门户的运行时 `config.js` 以 JavaScript 静态资源提供，消除浏览器 MIME 错误。

## 范围

- `deploy/scripts/invoke-deployment.ps1`：TEST `deploy-local` 与 `build-local` 统一使用已授权的 `sudo -n` 远端执行方式。
- `portal/public/config.js`：提供无密钥、可被部署后覆盖的最小运行时配置入口，保留代码内默认配置。

## 禁止范围

- 不执行数据库初始化或业务 SQL。
- 不连接生产 ECS 2，不修改项目 A，不创建或修改运行时密码。
- 本任务不包含构建、暂存、部署、Git 提交或推送。

## 验收

- [x] PowerShell AST 解析通过，入口参数仅为 TEST `deploy-local` 增加 `sudo -n`。
- [ ] 门户构建产物包含唯一根目录 `config.js`，内容为非模块 JavaScript（本轮未构建，待获构建授权）。
- [x] `git diff --check` 通过；静态检查未执行数据库、服务器和部署操作。
- [ ] 服务器部署和浏览器复验另行授权后执行。

## 实际变更与证据

实施证据：

- `deploy/scripts/invoke-deployment.ps1` 将 TEST `build-local` 与 `deploy-local` 统一包装为 `sudo -n`，与既有暂存环境权限边界一致。
- 暂存前的秘密文件追踪检查改为读取 `git ls-files` 结果，避免 Windows PowerShell 将“未匹配”诊断误判为终止异常。
- 新增 `portal/public/config.js`，仅初始化 `window.CREHN_PORTAL_CONFIG`，不含密码、令牌或服务器地址；Vite 会将 `public/` 原样复制到门户根目录。
- PowerShell AST、Node `--check` 和 `git diff --check` 通过；构建、服务器和浏览器验证标记为 `NOT_RUN`，需分别授权。
