# CREHN 需求登记

## 状态

`PROPOSED → CONFIRMED → IMPLEMENTING → IMPLEMENTED → VERIFIED → DEFERRED/DEPRECATED`

需求确认只授权进入实现，不自动授权 Git提交、推送、数据库执行或服务器部署。

## 已登记需求

| ID | 需求 | 状态 | 权威规范/验收 |
| --- | --- | --- | --- |
| REQ-001 | 使用与 DYZ 同源的 RuoYi-Vue-Plus 技术栈建立完全独立的 CREHN 项目 | IMPLEMENTED | 工程规范、后端/前端架构；待服务器编译验证 |
| REQ-002 | 建设独立公共门户及组件化 CMS | IMPLEMENTED | 门户CMS规范；管理端/门户本地构建通过 |
| REQ-003 | 参赛者正式账号；学校批量名单/一次性激活码；本人激活填报 | IMPLEMENTED | 产品规范、角色权限；待数据库流程验证 |
| REQ-004 | 学校提交、审核、多专家单评委评分、汇总和结果发布完整流程 | IMPLEMENTED | 状态机、验收矩阵；待角色化集成验证 |
| REQ-005 | 兼容 DYZ 活动和历史数据，但不共库、不双写、不复用运行标识 | IMPLEMENTED | DYZ兼容规范、活动配置包版本门禁；待固定样例包验证 |
| REQ-006 | 学校、并发、上传、保留、浏览器、备份目标和消息渠道分层配置 | IMPLEMENTED | 安全运维、动态引擎；容量/恢复等待服务器验证 |
| REQ-007 | LOCAL、TEST、STAGE、PROD 环境严格隔离 | IMPLEMENTED | 工程规范、安全运维、Compose 分层文件 |
| REQ-008 | Git 使用 `main + feature/REQ-* + 发布标签`，不设长期 `develop` | IMPLEMENTED | `main` 已建立本地提交历史；无远端、标签或上游；后续提交与推送仍需独立授权 |
| REQ-009 | Synology 裸仓库目标为 `C:\Users\A\Documents\SynologyDrive\GIT\CREHN\CREHN.git` | CONFIRMED | 工程规范；创建/推送另行授权 |
| REQ-010 | 192.168.2.229 构建测试，专用 ECS 2 只接收同一不可变产物 | IMPLEMENTING | 独立Compose、镜像归档、SHA-256、主机指纹和发布门禁；等待服务器预检与运行验证 |
| REQ-011 | 初期只启用站内消息；短信和邮件在真实供应商配置前关闭 | IMPLEMENTED | 安全运维、学校审核/项目审核/结果发布站内消息 |
| REQ-012 | 1Panel 不作为运行依赖；若使用只装宿主机，不装入 CREHN 容器 | CONFIRMED | ADR-0001 |
| REQ-013 | 所有用户可见功能页面纳入受控配置覆盖，管理员可在白名单和兼容契约内维护主题、系统骨架、角色首页、页面/类别布局、组件、表格和显示文案 | IMPLEMENTING | 角色工作台与稳定 `role_key` 基础已实施；全页面覆盖矩阵要求、组件权限提示、兼容替换、表格与文案边界规范已补齐，逐页登记、运行时版本和浏览器验收仍需分阶段实施 |

## 新需求登记规则

新增需求先增加稳定 `REQ` 编号，再分析影响并拆分活动任务。需求不得直接以活动 ID、角色 ID 或临时代码分支落入实现。
