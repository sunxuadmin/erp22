# CREHN 需求登记

## 状态

`PROPOSED → CONFIRMED → IMPLEMENTING → IMPLEMENTED → VERIFIED → DEFERRED/DEPRECATED`

需求确认只授权进入实现，不自动授权 Git提交、推送、数据库执行或服务器部署。

## 已登记需求

| ID | 需求 | 状态 | 静态测试 | 构建 | 浏览器 | 数据库 | 服务器 | 生产 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-001 | 独立 CREHN 工程 | IMPLEMENTED | 规范/前端检查 PASS | 前端 PASS；后端 NOT_RUN | TEST局部通过 | NOT_RUN | NEEDS_SERVER | NOT_RUN |
| REQ-002 | 独立公共门户及组件化 CMS | IMPLEMENTED | 定向测试 PASS | 门户/管理端 PASS；后端 NOT_RUN | NEEDS_BROWSER | V006 NOT_RUN | NEEDS_SERVER | NOT_RUN |
| REQ-003 | 参赛者正式账号、一次性激活码与本人填报 | IMPLEMENTED | FLOW/ACTIVATE/UX 静态 PASS | 前端 PASS；后端 NOT_RUN | NEEDS_BROWSER | NOT_RUN | NEEDS_SERVER | NOT_RUN |
| REQ-004 | 学校审核、专家评审、汇总与结果发布流程 | IMPLEMENTED | 前端测试 PASS；后端仅静态 | 前端 PASS；后端 NOT_RUN | NEEDS_BROWSER（12角色未完成） | NOT_RUN | NEEDS_SERVER | NOT_RUN |
| REQ-005 | DYZ 选择性兼容，不共库/双写/复用运行标识 | IMPLEMENTED | 版本化契约存在 | 前端 PASS；固定样例包 NOT_RUN | NEEDS_BROWSER | NOT_RUN | NEEDS_SERVER | NOT_RUN |
| REQ-006 | 容量、上传、保留、备份和消息分层配置 | IMPLEMENTED | 配置/脚本静态存在 | 前端 PASS；部署产物 NOT_RUN | NEEDS_BROWSER | 恢复演练 NOT_RUN | NEEDS_SERVER | NOT_RUN |
| REQ-007 | LOCAL/TEST/STAGE/PROD 隔离 | IMPLEMENTED | Compose静态历史通过 | TEST历史构建通过 | TEST局部通过 | TEST DB隔离回读未完成 | TEST局部通过 | NOT_RUN |
| REQ-008 | `main + feature/REQ-* + 发布标签` | IMPLEMENTED | 本地 Git 结构已核验 | 不适用 | 不适用 | 不适用 | 远端运行验证未完成 | 发布标签 NOT_RUN |
| REQ-009 | Synology 裸仓库 | VERIFIED | `origin/main`与首次推送有历史证据 | 不适用 | 不适用 | 不适用 | 已验证 | 不适用 |
| REQ-010 | 229 TEST 验证，ECS 2 不可变发布 | IMPLEMENTING | 预检/资产历史 PASS | TEST历史 PASS | 管理员/临时审核局部通过 | 备份/恢复/初始化 NOT_RUN | TEST局部通过；ECS 2 NEEDS_SERVER | NOT_RUN |
| REQ-011 | 初期只启用站内消息 | IMPLEMENTED | 三类代码静态存在 | 后端 NOT_RUN | NEEDS_BROWSER | NOT_RUN | NEEDS_SERVER | NOT_RUN |
| REQ-012 | 1Panel 不作为运行依赖 | CONFIRMED | ADR-0001 | 不适用 | 不适用 | 不适用 | NEEDS_SERVER | NOT_RUN |
| REQ-013 | 全页面受控配置与角色工作台 | IMPLEMENTING | QA-001 typecheck/94测试 PASS | 管理端4GB构建 PASS；后端 NOT_RUN | TEST两角色局部通过；12角色 NEEDS_BROWSER | V005/V006 NOT_RUN | NEEDS_SERVER | NOT_RUN |
| REQ-014 | 百分制、等级制和仅评语全链路 | IMPLEMENTING | 第一批基础链路存在 | 前端 PASS；后端 NOT_RUN | NEEDS_BROWSER | NOT_RUN | NEEDS_SERVER | NOT_RUN |

## 新需求登记规则

新增需求先增加稳定 `REQ` 编号，再分析影响并拆分活动任务。需求不得直接以活动 ID、角色 ID 或临时代码分支落入实现。
