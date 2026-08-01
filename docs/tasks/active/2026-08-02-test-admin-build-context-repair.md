# 2026-08-02 TEST管理端构建上下文修复

## 状态

- 需求：`REQ-010`
- 当前状态：`VERIFIED / COMMITTED / DEPLOYED_0.1.6 / NEEDS_RESTRICTED_ROLE_BROWSER`
- 代码修改：已获授权
- 构建、资产暂存、TEST部署和浏览器验收：已获授权
- Git提交：已完成；本任务不推送
- 数据库初始化：禁止

## 目标

让TEST镜像构建包含管理端和公共门户的受控Vite生产构建配置，修复管理端资源路径错误、白屏和标题占位符问题。

## 范围

- 修改：`.dockerignore`，仅放行已纳入Git且不含运行时密钥的`frontend/.env.production`和`portal/.env.production`；
- 新建：本任务卡并更新`TASKS.md`活动索引；
- 后续：以新的不可变`0.1.6-test`版本重新构建、暂存、部署和浏览器验收。

## 禁止范围

- 不执行数据库初始化、迁移或SQL；
- 不删除或覆盖既有不可变归档；
- 不修改项目A；
- 不连接ECS 2；
- 不推送Git或发布标签。

## 验收

- [ ] Docker构建上下文仅包含两个明确放行的Vite生产配置文件；
- [ ] 静态差异检查通过；
- [x] `0.1.6-test`三张应用镜像和归档修订一致；
- [ ] 完整TEST服务健康，MinIO-init完成，项目A五容器基线一致；
- [x] 管理端登录页资源加载正常，不再出现白屏和HTML资源MIME错误；
- [ ] 登录态、受限角色权限、工作台配置保存回读和无权限提示浏览器验收通过（当前仅超级管理员、无启用活动数据）。

## 实际变更与证据

- `VERIFIED`：`.dockerignore`仅放行已纳入Git的两个Vite生产配置文件；
- `VERIFIED`：0.1.6-test BuildLocal成功，归档SHA-256为`f7d9dd43101679d19da8cf57d99411f5c494b1de0d5c1dcd092163fab399a3d0`；
- `VERIFIED`：229 `deploy-local`和`verify-local`通过，Backend/Web/DB/Redis/MinIO健康，MinIO-init完成；
- `VERIFIED`：浏览器门户、管理端登录入口和登录态页面加载通过，控制台0 errors；
- `NEEDS_RESTRICTED_ROLE_BROWSER`：尚无受限角色账号，且当前没有启用活动，保存回读和无权限提示未验证。
