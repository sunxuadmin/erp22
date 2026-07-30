# CREHN 业务状态机

## 1. 通用规则

- 状态码为稳定协议常量，显示文字可以配置。
- 所有状态转换由后端服务执行，前端不能直接提交目标状态绕过动作。
- 每次转换校验身份、权限、数据范围、当前状态、完整性和时间窗口。
- 重复请求必须幂等；并发请求使用版本号、唯一约束或锁保护。
- 转换记录操作人、原状态、目标状态、原因、时间和关联版本。

## 2. 活动状态

`DRAFT → CONFIGURED → REGISTRATION_OPEN → REGISTRATION_CLOSED → SCHOOL_REVIEW → PROVINCIAL_REVIEW → EXPERT_REVIEW → RESULT_PENDING → PUBLISHED → ARCHIVED`

规则：

- 时间到达可以触发阶段提示，但阶段切换仍需服务端确认。
- 已有作品后禁止无检查地修改关键类别编码、字段编码和附件编码。
- `PUBLISHED` 仅表示活动结果版本已发布，不允许重新解释历史提交。
- 归档后默认只读；恢复运行必须形成新的配置版本和审计记录。

## 3. 作品状态

`DRAFT → PARTICIPANT_SUBMITTED → SCHOOL_RECOMMENDED → SCHOOL_FINAL_SUBMITTED → PROVINCE_APPROVED → REVIEWING → RESULT_GENERATED → RESULT_PUBLISHED`

允许的退回和旁路：

- `PARTICIPANT_SUBMITTED → SCHOOL_RETURNED → DRAFT`
- `SCHOOL_RECOMMENDED → SCHOOL_RETURNED → DRAFT`
- `SCHOOL_FINAL_SUBMITTED → PROVINCE_RETURNED → DRAFT`
- 可编辑阶段允许撤回到 `DRAFT`，学校最终提交后仅授权人员可按原因退回。
- 任何允许状态都可进入 `RECYCLED`；恢复时回到记录的合法来源状态。

门禁：

- 参赛者提交前校验动态字段、成员、通用表格、附件、承诺和账号归属。
- 学校推荐前校验学校数据范围和校内审核意见。
- 学校最终提交通过批次完成，不逐条偷偷改变参赛者身份。
- 省级审核通过后才能分配专家。
- 结果生成后普通评分和签名表不可撤回；需先撤回结果版本。

## 4. 学校最终提交批次

`DRAFT → SUBMITTED → ACCEPTED`

异常状态：

- `RETURNED`：省级退回，学校处理后创建新批次；
- `SUPERSEDED`：被后续正式批次取代；
- `CANCELLED`：提交前取消。

已提交批次必须保存作品 ID、作品快照版本、学校序号、提交人、承诺版本和校验摘要。

## 5. 审核任务和结论

- 审核任务：`ACTIVE ↔ DISABLED`，删除默认逻辑删除。
- 审核结论：`PASS`、`RETURN`，撤销动作分别记录 `WITHDRAW_PASS`、`WITHDRAW_RETURN`。
- 每次结论保留独立历史，不覆盖旧结论。
- 撤销审核通过前先检查是否已存在专家分配、评分或结果。

## 6. 评审任务和评分

评审任务：`ACTIVE ↔ DISABLED`。

评分：

`DRAFT → SUBMITTED → LOCKED`

异常动作：

- 管理员或规则允许时 `SUBMITTED → RETURNED → DRAFT`；
- 类别签名评分表创建后，所含评分进入 `LOCKED`；
- 签名表撤回成功后，且不存在结果版本时，评分恢复为可编辑状态。

批量分配、同步、移除和范围计划必须先预览新增、移除、冲突、回避和已评分保护，再确认应用。

## 7. 签名评分表

`ACTIVE → WITHDRAWN`

- 创建时原子冻结评分、模板、签名、签名位置、专家、活动、类别和服务端时间。
- `ACTIVE` 同一专家、活动和类别只能存在一个有效版本。
- 替换个人签名资源不能改写已签历史快照。
- 撤回必须填写原因；结果已生成时禁止直接撤回。

## 8. 结果版本

`DRAFT → LOCKED → PUBLISHED`

辅助动作：

- `DRAFT` 可撤回生成并重新生成；
- `PUBLISHED → OFFLINE` 只停止公开展示，不删除发布历史；
- 新计算产生新版本，旧版本标记 `SUPERSEDED`，不能原地覆盖。

生成前执行就绪检查：作品范围、审核状态、专家数量、评分完成度、签名要求、规则版本和异常预警。发布前再次检查锁定版本、权限和门户展示范围。

## 9. CMS 内容

`DRAFT → REVIEWED → PUBLISHED → OFFLINE → ARCHIVED`

编辑已发布内容形成新草稿版本；发布和回滚均指向明确版本。定时发布由服务端任务执行并记录实际时间和结果。

## 10. DYZ 状态映射

历史数据迁移时使用显式映射，不修改 CREHN 状态机：

| DYZ 状态 | CREHN 建议映射 |
|---|---|
| `draft` | `DRAFT`，标记来源为学校代填历史 |
| `submitted` | `SCHOOL_FINAL_SUBMITTED` |
| `returned` | `PROVINCE_RETURNED` |
| `audit_passed` | `PROVINCE_APPROVED` |
| `recycled` | `RECYCLED` |
| 评分 `draft/submitted` | `DRAFT/SUBMITTED` |
| 结果 `draft/published` | `DRAFT/PUBLISHED` |

无法无损映射时导入为只读历史记录并产生阻断性警告，不猜测状态。
