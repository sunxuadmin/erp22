# CREHN 文档权威与治理规则

## 1. 目的

本文件规定 CREHN 文档之间的责任边界、冲突处理、变更流程和状态表达，防止同一规则在多处复制后产生不同有效版本。

## 2. 权威层级

1. 用户在当前任务中的明确确认；
2. MASTER 的专题所有者文档；
3. 详细功能与页面规格；
4. CREHN 当前已验证代码、迁移和运行配置；
5. DYZ 当前实现与历史文档；
6. 草图、示例、测试数据和未确认提案。

代码与文档冲突时，不自动判定代码正确。先区分：

- 文档已确认、代码未实现；
- 代码已经实现、文档未更新；
- 两者都未验证；
- 生产运行配置覆盖了仓库默认值。

## 3. 专题唯一所有权

| 规则类型 | 唯一所有者 |
|---|---|
| 产品功能和业务边界 | `CREHN_PRODUCT_RULE.md` |
| 角色、权限和数据范围 | `CREHN_ROLE_DATA_SCOPE.md` |
| 状态和转换门禁 | `CREHN_STATE_MACHINE.md` |
| DYZ 兼容和导入 | `CREHN_DYZ_COMPATIBILITY_V1.md` |
| 版本与快照 | `CREHN_VERSION_SNAPSHOT_POLICY.md` |
| 数据实体和唯一来源 | `CREHN_DATABASE_DESIGN_V1.0.md` |
| 文件生命周期 | `CREHN_FILE_LIFECYCLE.md` |
| 数据库迁移和发布 | `CREHN_MIGRATION_RELEASE_RULE.md` |
| 开发与授权流程 | `CREHN_CODEX_WORKFLOW.md` |
| 验收证据 | `CREHN_ACCEPTANCE_MATRIX.md` |

其他文件只能摘要并链接，不复制整段规则。

## 4. 决策状态

- `PROPOSED`：分析建议，尚未确认；
- `CONFIRMED`：用户已确认，可进入实现；
- `IMPLEMENTING`：正在修改文件；
- `IMPLEMENTED`：实现完成但可能尚未验证；
- `VERIFIED`：验证证据满足要求；
- `DEPRECATED`：保留兼容但不再用于新增功能；
- `DEFERRED`：明确延后。

“已写入文档”只能证明文档已更新，不能标为 `IMPLEMENTED` 或 `VERIFIED`。

## 5. 变更流程

1. 定位唯一所有者文档；
2. 写明变更原因、影响模块、兼容性、迁移和验收；
3. 用户确认后更新所有者文档；
4. 其他文档只更新链接或一句摘要；
5. 实现后回填真实验证状态；
6. Git 提交、数据库执行和部署分别获取授权。

## 6. DYZ 的地位

DYZ 是功能参考、兼容来源和已发生问题的证据库，不是 CREHN 的运行时依赖。CREHN 可以复用设计经验和 RuoYi 公共模式，但不得：

- 直接连接 DYZ 生产数据库；
- 复用 DYZ 账号密码、密钥和 OSS 访问配置；
- 依赖 DYZ 数字主键或菜单 ID；
- 通过复制整个历史模块制造重复实现；
- 把 DYZ 当前缺陷固化成 CREHN 兼容要求。
