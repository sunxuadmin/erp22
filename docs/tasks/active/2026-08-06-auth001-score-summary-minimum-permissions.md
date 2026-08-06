# 2026-08-06 分数汇总角色最小权限白名单

## 状态

- 任务编号：`AUTH-001`
- 当前状态：`IMPLEMENTED / STATIC_VERIFIED / SQL_NOT_AUTHORIZED`
- 实现授权：已确认
- SQL执行、服务器连接、部署、Git提交/推送、删除：未授权

## 冻结契约

- `crehn_score_summary` 默认只读取评分汇总、汇总明细、签名表汇总并导出评分汇总。
- 默认不得获得结果生成、撤回生成、发布/下线、奖项规则、评分调整、汇总配置或结果导出权限。
- 本次同时用同一增量权限边界迁移移除 `crehn_school` 的参赛者草稿、上传和单项目提交权限；业务服务端拒绝由 `FLOW-001` 持有。

## 允许修改范围

- `deploy/db/migrations/V007__crehn_role_permission_boundaries.sql`
- `deploy/db/rollback/V007__crehn_role_permission_boundaries.rollback.sql`
- `deploy/db/migration-manifest.json`
- `deploy/db/Dockerfile`
- 本任务卡

## 禁止范围

- 不修改已存在的 V004/V005/V006 内容。
- 不执行 SQL、数据库回读、服务器连接、部署、Git提交或推送。
- 不扩大汇总员的数据范围；真实活动范围仍由服务端和角色数据范围共同约束。

## 允许/拒绝矩阵

| 范围 | 权限 | 默认结论 |
| --- | --- | --- |
| 评分汇总 | `crehn:reviewScoreSummary:list` | 允许 |
| 汇总明细 | `crehn:reviewScoreSummary:detail` | 允许 |
| 汇总导出 | `crehn:reviewScoreSummary:export` | 允许 |
| 签名表汇总只读 | `crehn:reviewSheet:manage` | 允许 |
| 汇总配置 | `crehn:reviewScoreSummary:config` | 拒绝 |
| 结果模块 | `crehn:result:*` | 拒绝 |
| 评分写入/调整 | `crehn:review:score`、`crehn:result:score:edit` | 拒绝 |
| 签名表撤回/签名管理 | `crehn:reviewSheet:withdraw`、`crehn:reviewSheet:signatureManage*` | 拒绝 |

## 验收

- [x] V007 按 `role_key` 和权限标识收紧默认授权，不依赖数字角色 ID。
- [x] 汇总员新增白名单仅含 `list/detail/export/reviewSheet:manage`，迁移同时移除已知结果、评分、配置、撤回和签名管理权限。
- [x] V007 移除学校角色 `crehn:project:add/submit/upload/remove`。
- [x] manifest JSON可解析；当前44项业务结构/47个 init COPY，44个路径存在；迁移与非破坏性回滚资产存在。
- [x] SQL执行与真实角色接口矩阵为 `NOT_RUN / NEEDS_SERVER`，等待专项授权。

## 风险与回滚

- 迁移会收敛目标角色在 CREHN 菜单树内的现有授权；执行前必须导出角色菜单快照并确认没有经审批的例外授权。
- 回滚仅恢复 V004 的历史默认授权形状，不代表安全验收通过；若曾有环境特有授权，必须从执行前快照恢复。
