# 2026-08-06 参赛者本人填报与学校审核边界修正

## 状态

- 任务编号：`FLOW-001`
- 当前状态：`IMPLEMENTED / STATIC_VERIFIED / NEEDS_BACKEND_COMPILE / NEEDS_BROWSER`
- 实现授权：已确认
- SQL执行、服务器连接、部署、Git提交/推送、删除：未授权

## 冻结契约

- 参赛者本人激活正式账号后，才可创建、编辑、上传材料、提交或撤回本人作品。
- 学校联络员只可查看本校作品，执行学校审核推荐/退回，并通过学校最终提交批次形成不可变快照。
- 学校不得调用参赛者草稿、上传、单项目提交或单项目撤回写入链路；即使数据库中残留旧权限，服务端仍必须拒绝。

## 允许修改范围

- `backend/ruoyi-modules/ruoyi-crehn/src/main/java/org/dromara/crehn/common/ArtReviewSecurity.java`
- `backend/ruoyi-modules/ruoyi-crehn/src/main/java/org/dromara/crehn/project/service/submit/ProjectDraftSubmitService.java`
- `backend/ruoyi-modules/ruoyi-crehn/src/main/java/org/dromara/crehn/project/service/file/ProjectFileService.java`
- `backend/ruoyi-modules/ruoyi-crehn/src/main/java/org/dromara/crehn/project/service/impl/ArtProjectServiceImpl.java`
- `frontend/src/views/crehn/project-submit/index.vue`
- `frontend/src/views/workbench/components/SchoolProjectSubmitWidget.vue`
- 本任务卡；学校菜单权限的增量迁移由 `AUTH-001` 唯一持有。

## 禁止范围

- 不改变学校审核/推荐/最终提交服务的状态机和数据范围。
- 不修改已存在的 V004；不执行任何迁移 SQL。
- 不连接服务器、不部署、不提交或推送 Git、不清理现有工作区。

## 验收

- [x] 服务层对草稿、编辑、上传、提交、撤回和成员导入写路径要求当前参赛者且校验项目所有者；待后端编译/运行矩阵证明。
- [x] 参赛者所有者校验已实施；真实账号拒绝矩阵 `NEEDS_SERVER`。
- [x] 学校统一提交入口复用学校审核/最终提交页面，不再调用参赛者单项目提交 API。
- [x] 学校工作台项目列表的选择、直接提交/撤回/删除模板与死代码已移除。
- [x] 管理端全量 typecheck、16个测试文件/94项测试、目标 ESLint 通过。
- [x] 证据分列：静态 `PASS`；后端编译 `NOT_RUN`；浏览器 `NEEDS_BROWSER`；数据库 `NOT_RUN`；服务器 `NEEDS_SERVER`；生产 `NOT_RUN`。

## 风险

- 真实数据库可能仍保留 V004 产生的旧菜单授权；需由 `AUTH-001` 增量迁移收敛并在 SQL 获批后回读。
- 多角色当前角色上下文仍由 `AUTH-002` 处理，本任务不以“第一个角色”替代服务端用户类型与数据范围。
