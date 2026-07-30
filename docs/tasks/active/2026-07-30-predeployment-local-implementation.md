# 2026-07-30 部署前本地工程实现

## 状态

- 需求：`REQ-001` 至 `REQ-012`
- 当前状态：`IMPLEMENTED / SERVER_VERIFICATION_PENDING`
- 负责人：Codex
- Git仓库：2026-07-31提交前只读审计时，`main` 已有5个本地提交（非本任务执行）
- 新增Git提交：未授权
- Git推送：未授权
- 数据库执行：未授权
- 服务器部署：未授权

## 目标

在 `C:\Users\A\Documents\CREHN` 建立独立、可构建、可测试和可容器化的 CREHN 项目，完成部署前本地实现与验证材料。

## 范围

- 完善已确认的限制、环境、Git、消息和发布规范；
- 建立后端、管理端、独立门户和部署目录；
- 复用 RuoYi-Vue-Plus 公共能力；
- 迁入并重构 CREHN 所需的活动、单位账号、报名、项目、审核、评审、结果、文件、CMS 和兼容能力；
- 建立结构迁移清单、Compose、构建和不可变产物流程；
- 执行当前环境可完成的静态检查、测试和构建。

## 禁止范围

- 不修改 `C:\Users\A\Documents\DYZ`；
- 不连接或修改 `192.168.2.229`、原项目A生产机或新建ECS 2；
- 不安装 1Panel；
- 不执行数据库迁移或导入业务数据；
- 不创建或推送 Synology 裸仓库；
- 不提交 Git，除非取得单独授权；
- 不把 DYZ 生产配置、账号、密钥、数据或 OSS 标识复制到 CREHN。

## 验收

- [x] MASTER 包含已确认的限制、环境、Git/CI、消息和Docker产物规则；
- [x] 源码目录和模块边界清楚，CREHN 不依赖 DYZ 运行目录；
- [ ] 后端模块可编译并通过目标测试；
- [x] 管理端和门户可构建；
- [x] 数据库迁移分类和清单可校验；
- [ ] Compose 配置可解析，测试/生产配置隔离；
- [ ] 关键角色、状态、文件和兼容用例有测试或明确未验证证据；
- [x] 未执行服务器部署。

## 实际变更与证据

### 工程与功能

- `backend/`：独立 RuoYi-Vue-Plus 后端，业务模块为 `ruoyi-crehn`，接口前缀 `/crehn`，权限前缀 `crehn:`。
- `frontend/`：独立管理端，包含活动/类别/字段/附件模板、单位与账号、参赛者激活、学校审核与最终提交、审核、专家评分、签名表、汇总结果、CMS、工作台和页面配置。
- `portal/`：独立门户，按 `VITE_PORTAL_SITE_CODE` 查询启用站点，展示已发布文章和首页发布快照。
- `deploy/`：MariaDB、Redis、后端、管理端和门户 Compose；TEST/PROD 覆盖文件；镜像归档和 SHA-256 校验脚本。
- `deploy/db/migrations/V001..V004`：参赛者身份、学校流程快照、CMS、角色、兼容结构和菜单授权。
- 参赛者账号使用一次性激活码；只保存哈希和末四位提示。学校审核与最终提交发送站内消息，通知失败不再被静默吞掉。
- 学校账号新建/重置/导入不使用固定默认密码；密码须为 8 至 30 位并同时包含字母和数字。
- 门户媒体人工复核、文章发布/下线/回滚、首页发布和学校状态转换均保留权限、原因或操作日志。

### 已执行验证

- `frontend: pnpm run typecheck`：PASS。
- `frontend: pnpm test`：PASS，10 个测试文件、65 项测试。
- `frontend: pnpm run build:prod`：PASS，3427 个模块；仅有大分块体积警告。
- `portal: pnpm run build`：PASS；`vue-tsc --noEmit` 与 Vite 构建成功；`config.js` 保留非模块运行时配置提示。
- 迁移清单 JSON 可解析；42 个 SQL 路径全部存在，和数据库 Dockerfile 一一对应，无缺项、无额外项、无重复目标文件名。
- 两个发布 PowerShell 脚本语法解析均为 0 错误。
- 运行范围内未发现 `org.dromara.art`、`ruoyi-art-review`、`/art-review`、`@/api/art`、`@/views/art`、`art:`、固定站点 ID 或学校固定默认密码残留。
- 本机未发现 Java、Maven、Docker，因此未伪造后端编译、Compose 解析和镜像构建结果。

### 未执行及原因

- `backend Maven compile/test`：`NOT_RUN`，本机没有 Java/Maven；在 TEST 构建环境执行。
- `docker compose config/build/up`：`NOT_RUN`，本机没有 Docker；等待测试服务器部署方案。
- 数据库初始化、迁移、回滚：`NOT_RUN`，未获数据库执行授权。
- 管理员、学校、参赛者、审核员、专家、汇总员浏览器端到端流程：`NEEDS_SERVER`。
- DYZ 固定活动包 dry-run、导入、资源对账：`NEEDS_SERVER`，且不得读取 DYZ 生产数据。
- 并发、上传容量、备份恢复、浏览器矩阵：`NEEDS_SERVER`。
- Git现状只读复核：2026-07-31提交前，`main` 已有5个本地提交，提交为 `08d3ba25bebbfab2949ad9873614d36db15cd0d5`；无远端、标签或上游。上述既有提交非本任务执行；后续提交、Synology裸仓库创建和推送仍需独立授权。

### 当前风险

- 后端和 Compose 尚未在具备 Java/Maven/Docker 的环境实际编译解析，不能判定为可部署版本。
- 复制保留了 RuoYi 的 demo、generator、workflow、job 可选模块；生产是否禁用或移除需结合 TEST 运行依赖确认，不能在未确认时擅自删除。
- `backend/script/sql/postgres`、`oracle`、`sqlserver` 和被清单排除的测试种子属于上游参考资产，不进入 MariaDB 镜像；其中仍有上游示例账号/口令文本，禁止纳入发布清单或执行。
- 管理端构建存在大分块警告，当前不阻断功能，但应在真实网络与浏览器矩阵中测量首屏和按需加载。
