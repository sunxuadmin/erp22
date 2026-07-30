# CREHN 项目开发文档体系

文档版本：V1.1（审查稿）  
适用项目：`C:\Users\A\Documents\CREHN`  
当前阶段：设计约束已确认，尚未表示代码已实现或已验收。

## 1. 文档目标

本目录用于约束 CREHN 的长期产品设计、工程实现、DYZ 兼容、数据迁移和验收。CREHN 采用与 DYZ 相同的 RuoYi-Vue-Plus 技术栈重新建立，但必须拥有独立代码、数据库、对象存储、运行配置、租户与账号体系。

## 2. 唯一事实来源与优先级

发生冲突时按以下顺序处理：

1. 用户在当前任务中明确确认的业务决定；
2. [CREHN_MASTER_AUTHORITY.md](CREHN_MASTER_AUTHORITY.md) 中的文档治理规则；
3. 本 MASTER 中各专题规范；
4. 上一级目录中的 `CREHN-功能结构与页面字段规格说明书-审查稿V0.2.docx`；
5. 已验证的 CREHN 当前代码、数据库迁移和运行时配置；
6. DYZ 当前实现，仅作为功能参考和兼容来源，不直接成为 CREHN 运行时真相。

分析提案、界面草图、测试数据和 DYZ 历史实现不能覆盖已确认规则。

## 3. 推荐阅读顺序

1. [CREHN_AI_DEVELOPMENT_CONTEXT.md](CREHN_AI_DEVELOPMENT_CONTEXT.md)
2. [CREHN_MASTER_AUTHORITY.md](CREHN_MASTER_AUTHORITY.md)
3. [CREHN_DOMAIN_GLOSSARY.md](CREHN_DOMAIN_GLOSSARY.md)
4. [CREHN_PRODUCT_RULE.md](../01-产品设计/CREHN_PRODUCT_RULE.md)
5. [CREHN_ROLE_DATA_SCOPE.md](../01-产品设计/CREHN_ROLE_DATA_SCOPE.md)
6. [CREHN_STATE_MACHINE.md](../01-产品设计/CREHN_STATE_MACHINE.md)
7. [CREHN_DYZ_FUNCTION_COVERAGE.md](../01-产品设计/CREHN_DYZ_FUNCTION_COVERAGE.md)
8. [CREHN_PROJECT_RULE.md](../02-工程规范/CREHN_PROJECT_RULE.md)
9. [CREHN_DYZ_COMPATIBILITY_V1.md](../02-工程规范/CREHN_DYZ_COMPATIBILITY_V1.md)
10. [CREHN_VERSION_SNAPSHOT_POLICY.md](../02-工程规范/CREHN_VERSION_SNAPSHOT_POLICY.md)
11. 数据库、后端、前端、动态引擎、主题与文件、CMS 专题；
12. 安全运维与迁移发布规范；
13. Codex 开发流程；
14. 最终验收清单和验收矩阵。

## 4. 目录职责

| 目录 | 负责内容 |
|---|---|
| `00-项目总览` | 权威来源、固定决策、术语和阅读入口 |
| `01-产品设计` | 产品能力、角色、数据范围、状态机和 DYZ 功能覆盖 |
| `02-工程规范` | 技术边界、DYZ 兼容、版本与快照 |
| `03-数据库` | 领域数据、唯一来源、迁移与发布 |
| `04-后端` | 模块职责、API、事务、权限和异步任务 |
| `05-前端` | 门户、管理端、角色工作区与组件归属 |
| `06-动态引擎` | 动态表单、成员表、通用表格和 Excel |
| `07-主题与文件` | 主题令牌、文件生命周期和安全访问 |
| `08-CMS` | 独立门户内容、媒体和发布版本 |
| `09-安全运维` | 安全、审计、备份、监控和环境隔离 |
| `10-Codex开发` | 分析、确认、实现、验证和授权门禁 |
| `11-验收` | 功能、权限、兼容、数据和上线验收 |

## 5. 兼容边界

“兼容 DYZ 活动”默认表示：

- CREHN 能导入 DYZ 活动配置并转换为 CREHN 标准模型；
- 历史作品、文件、审核、评分和结果可通过独立迁移批次导入；
- 不共享数据库，不实时双写，不复用 DYZ 数字主键、用户密码、租户 ID、角色 ID、OSS ID 或临时访问 URL；
- DYZ 与 CREHN 分别独立部署、独立升级和独立回滚。

功能覆盖见 [CREHN_DYZ_FUNCTION_COVERAGE.md](../01-产品设计/CREHN_DYZ_FUNCTION_COVERAGE.md)，协议规则见 [CREHN_DYZ_COMPATIBILITY_V1.md](../02-工程规范/CREHN_DYZ_COMPATIBILITY_V1.md)。

## 6. 状态声明

文档中使用以下状态：

- `CONFIRMED`：方案已经用户确认，可以进入文档或实现阶段；
- `IMPLEMENTED`：代码和必要迁移已完成；
- `VERIFIED`：已记录真实测试或运行证据；
- `NOT_VERIFIED`：存在实现或设计，但缺少对应验证；
- `DEFERRED`：明确延后，不得被误报为完成。

文档完整不等于代码完成；静态检查通过不等于浏览器、后端、数据库或生产环境验证通过。
