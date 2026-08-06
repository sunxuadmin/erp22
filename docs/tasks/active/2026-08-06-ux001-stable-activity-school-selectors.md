# 2026-08-06 移除活动/学校数字 ID 手工输入

## 状态

- 任务编号：`UX-001`
- 当前状态：`IMPLEMENTED / STATIC_VERIFIED / NEEDS_BACKEND_COMPILE / NEEDS_BROWSER`
- 实现授权：已确认
- SQL执行、服务器连接、部署、Git提交/推送、删除：未授权

## 范围与契约

- 学校提交、参赛者名单和回收站不再要求用户输入活动数字ID。
- 参赛者名单导入不再输入学校数字ID，改用有权限的学校选项或当前学校上下文。
- 前端仍向现有API传递内部ID，但ID只由服务端授权选项返回，不由用户猜测。

## 允许修改范围

- `frontend/src/views/crehn/school-submission/index.vue`
- `frontend/src/views/crehn/participant-account/index.vue`
- `frontend/src/views/crehn/recycle/index.vue`
- `backend/ruoyi-modules/ruoyi-crehn/src/main/java/org/dromara/crehn/activity/controller/ArtActivityController.java`
- `backend/ruoyi-modules/ruoyi-crehn/src/main/java/org/dromara/crehn/config/controller/ArtSchoolInfoController.java`
- 本任务卡、总控索引和交接文档

## 验收

- [x] 学校提交、参赛者名单和回收站三个目标页不再出现活动ID手工输入。
- [x] 参赛者名单导入不再出现学校ID手工输入，改为可搜索学校选项或当前学校上下文。
- [x] 活动/学校选项只增加查询权限的 OR 入口，没有授予配置写权限。
- [x] 管理端全量 typecheck、16文件/94测试和目标 ESLint 通过；浏览器 `NEEDS_BROWSER`，数据库 `NOT_RUN`，服务器 `NEEDS_SERVER`，生产 `NOT_RUN`。
