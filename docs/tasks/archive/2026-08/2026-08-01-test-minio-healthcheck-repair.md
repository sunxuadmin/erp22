# 2026-08-01 TEST MinIO健康检查修复

## 状态

- 需求：`REQ-007`、`REQ-010`
- 当前状态：`ARCHIVED / VERIFIED_THROUGH_0.1.9`
- 代码/部署资产修改：已获授权
- 构建、资产暂存、TEST部署：已获授权
- Git提交：本轮已授权，仅本地提交
- Git推送、标签、数据库初始化：未授权且本任务禁止

## 目标

修复TEST使用的MinIO镜像健康检查与实际镜像工具集不匹配的问题，使完整`crehn-test`部署能够可靠判断MinIO服务已监听9000端口，并继续执行服务健康和浏览器验收。

## 证据

- `pgsty/minio`实际启动成功，但镜像内不存在`curl`、`wget`、`nc`、`grep`；原healthcheck返回exit 127。
- 失败部署已创建并保留项目级回滚备份：`20260801T125320Z-0.1.2-test-pre-deploy`。
- 项目A五个容器在失败部署前后均保持healthy，未被操作。

## 范围

- 修改：`deploy/compose.test.yml` MinIO healthcheck。
- 新建：本任务卡及`TASKS.md`活动索引。
- 后续需要新的完整提交，才能重新生成不可变`0.1.2-test`归档并暂存部署。

## 禁止范围

- 不执行数据库初始化或迁移；
- 不修改项目A；
- 不连接ECS 2；
- 不推送Git或发布标签；本轮仅允许本地提交。

## 验收

- [ ] Compose配置解析通过，healthcheck不依赖MinIO镜像不存在的外部工具；
- [ ] 新提交归档后MinIO healthcheck通过；
- [ ] Backend、Web、Redis、MinIO和MinIO-init healthy/completed；
- [ ] 项目A五容器基线一致；
- [ ] 登录态、角色权限、配置保存回读和无权限提示浏览器验收通过。

## 实际变更与证据

- 已将MinIO健康检查从不存在的`curl`改为镜像内可用的`/proc/net/tcp`端口探针；
- `git diff --check`通过；Bash语法检查待重新构建入口执行时在229核验；
- 失败部署回滚备份保留，等待新提交后的重新构建、暂存、部署和浏览器验收。
