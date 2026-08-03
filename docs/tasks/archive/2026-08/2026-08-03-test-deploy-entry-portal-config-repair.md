# REQ-010/REQ-013 TEST DeployLocal 入口与门户运行时配置修复

## 状态

- 需求：`REQ-010`、`REQ-013`
- 当前状态：`ARCHIVED / VERIFIED / DEPLOYED_0.1.9 / BROWSER_VERIFIED / RESTRICTED_ROLE_VERIFIED`
- Git提交：已授权并完成，`ed2c85d`、`4a7eb2a`、`cb55919`
- 数据库执行：未授权
- 部署：已授权并完成 TEST `0.1.9-test`

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
- [x] 门户构建产物包含根目录 `config.js`，TEST 返回 `200 application/javascript`。
- [x] `git diff --check`、构建、暂存和部署通过；未执行数据库初始化或 SQL。
- [x] 受限审核角色浏览器验收通过：仅显示项目审核；直接访问工作台配置返回 404。

## 实际变更与证据

实施证据：

- `deploy/scripts/invoke-deployment.ps1` 将 TEST `build-local` 与 `deploy-local` 统一包装为 `sudo -n`，与既有暂存环境权限边界一致。
- 暂存前的秘密文件追踪检查改为读取 `git ls-files` 结果，避免 Windows PowerShell 将“未匹配”诊断误判为终止异常。
- 新增 `portal/public/config.js`，仅初始化 `window.CREHN_PORTAL_CONFIG`，不含密码、令牌或服务器地址；Vite 会将 `public/` 原样复制到门户根目录。
- PowerShell AST、Node `--check` 和 `git diff --check` 通过；后续已按独立授权完成 TEST 构建、暂存、部署和浏览器验收。
- `0.1.9-test` 资产已暂存到 `/srv/crehn-test`；BuildLocal、DeployLocal、健康检查和项目A保护基线验收均已完成。
- 清理后项目 A 五个容器 ID、健康状态和重启次数保持不变；未删除镜像、容器、卷、项目 A 数据或 TEST 备份。
- 后续按授权删除未使用的 CREHN TEST `0.1.0-test` 至 `0.1.7-test` backend/db/web 镜像后，空间约 33.2 GiB；项目 A 五个容器仍为 `running/healthy`。
- `BuildLocal` 生成 manifest SHA-256 `c99f81a4b59c3f6d57aa2361a6c957695d67fe9b7e43fd5a14cd265d98590fb9`；`DeployLocal` 使用备份 `/srv/crehn-test/backups/20260803T073145Z-0.1.8-test-pre-deploy`，DB/backend/web/redis/minio 健康，minio-init 完成。
- 管理员浏览器验收通过：工作台配置、8 个页面标签、页面表头与按钮、表格列编辑字段均显示；无启用活动时保存按钮禁用，未写入业务配置。门户页面显示正常，`config.js` 返回 `200 application/javascript`，控制台无错误。
- `TEST临时审核` 浏览器验收通过：菜单仅有首页和项目审核；项目审核页显示“当前没有可用的启用活动”；直接访问 `/admin/system/workbench` 显示 404；审核页控制台无错误。
