# CREHN 协作说明

## 默认读取顺序

开始实施前只读取：

1. `AGENTS.md`
2. `TASKS.md`
3. 当前活动任务卡
4. 与需求直接相关的 MASTER 专题规范

部署、数据库执行、服务器连接和 Git 推送分别需要明确授权，不从代码实现授权自动推导。

## 状态门禁

`ANALYZE → CONFIRMED → IMPLEMENTING → IMPLEMENTED → VERIFIED → COMMIT_APPROVED → DEPLOY_APPROVED`

- 纯分析不创建任务卡、不修改文件。
- 用户确认实施后创建活动任务卡，再修改文件。
- 未执行的检查标记为 `NOT_RUN`、`NEEDS_BROWSER` 或 `NEEDS_SERVER`。
- 文档完成、代码完成、验证通过、Git提交和部署是不同状态。

## 最小改动

- 遵守 [CREHN工程规范](docs/CREHN-DOCS-V1.0-MASTER/02-工程规范/CREHN_PROJECT_RULE.md)。
- 先定位已有实现和唯一写入所有者，再修改。
- 不复制 DYZ 数字主键、生产配置、密钥、数据或部署路径。
- DYZ 只作为功能参考和兼容输入；CREHN 源码、数据库、存储和部署保持独立。
- 不增加没有明确需求依据的依赖、抽象、兼容分支、配置或文档。

## 目录职责

- `docs/CREHN-DOCS-V1.0-MASTER/`：长期规范。
- `docs/requirements/CREHN_REQUIREMENTS.md`：需求状态索引。
- `docs/tasks/active/`：正在实施的任务边界和证据。
- `docs/tasks/archive/YYYY-MM/`：已完成任务卡。
- `docs/decisions/`：仅记录跨模块且难以回退的重大决策。
- `backend/`：RuoYi-Vue-Plus 后端与 CREHN 业务模块。
- `frontend/`：管理端与角色工作台。
- `portal/`：独立公共门户。
- `deploy/`：容器、构建、迁移清单和产物发布工具。

## 验证与交付

- 前端构建不能代替登录态浏览器验证。
- 后端单文件编译不能代替模块测试和服务端权限验证。
- Docker 配置解析不能代替真实容器运行。
- 每次交付列出修改文件及原因、依赖/API/配置/数据库变化、验证、风险和授权状态。

