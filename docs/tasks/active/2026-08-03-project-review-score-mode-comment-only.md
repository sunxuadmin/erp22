# 2026-08-03 项目评分模式与仅评语

## 状态

- 需求：`REQ-014`
- 当前状态：`IMPLEMENTING`
- 代码修改：第一批已实施，待完整验证
- 数据库执行：未授权、未执行
- 部署：未授权、未执行
- Git提交/推送：未授权、未执行

## 目标

项目评审除现有百分制和等级制外，支持不产生数值分的等级评审，以及只保存评语的“仅评语”模式；模式必须贯穿评分提交、签名评分表、评分汇总、结果生成、排名/奖项规则、导出和学校端展示。

## 当前 CREHN 事实

- `ArtReviewConstants` 当前只有 `numeric_100` 和 `grade`。
- `ReviewScore` 已有可空的 `scoreValue`、`gradeValue`、`commentText`，但提交校验目前要求分数或等级。
- 评分分配页面目前只提供百分制和等级制。
- 结果发布已有 `showScore`、`showRank`、`showComment`，但不能据此推断“仅评语”评分流程已经完成。
- 当前评分、签名表和结果接口未使用 `reviewSessionId`；本卡需与迁移总卡的会话隔离设计保持兼容。

## 待确定的业务契约

- “不评分”和“仅评语”是否是同一模式；建议暂定为 `grade` 与 `comment_only` 两种非百分制模式。
- 仅评语是否要求评语必填。
- 模式作用范围是活动、类别还是单个评审分配。
- 仅评语时是否允许生成排名、奖项和结果发布。
- 评分表、汇总表、导出和学校端分别显示哪些列。
- 已有百分制/等级制数据的兼容和模式变更锁定规则。

## 预计范围

- 前端评分模式选择、评分输入、评分表和评分汇总列。
- 后端评分模式常量、提交/调整校验、结果汇总、排名/奖项保护、导出和权限边界。
- 数据库：先检查现有 `score_mode`、结果和评分表结构；是否需要 SQL 变更保持 `NOT_DECIDED`，不得直接执行。
- 测试：模式校验、草稿/提交、签名锁定、结果生成、导出和角色化浏览器验收。

## 禁止范围

- 不直接复制 DYZ 的评分 SQL、角色名称、权限 ID 或业务数据。
- 不删除或改变既有百分制/等级制数据。
- 不在未取得单独授权前执行 SQL、连接服务器、部署、提交或推送。

## 验收

- [ ] 百分制行为保持兼容。
- [ ] 等级制不要求数值分。
- [ ] 仅评语不生成数值分/等级，并按确定的必填规则提交。
- [ ] 签名表、汇总、结果、导出和学校端展示符合模式契约。
- [ ] 管理员、评分员、汇总员和学校用户权限/数据范围通过浏览器验收。
- [ ] 静态、数据库、浏览器、部署证据分别记录。

## 实际变更与证据

- 第一批已实现：新增 `comment_only` 模式常量；后端提交/调整校验要求评语；前端分配、批量分配和专家评分支持仅评语；评分表和汇总结果显示评语。
- 当前仍未完成：结果发布规则、奖项/排名策略的业务确认，数据库回读、浏览器验收、服务端验收和部署验收。
- 第一批文件：`ArtReviewConstants.java`、`ArtReviewServiceImpl.java`、`ReviewScoreSheetExcelWriter.java`、`ReviewScoreSheetService.java`、`ArtDetailDisplayConfigService.java`、`review-assignment/index.vue`、`review/index.vue`、`review-score-sheet/scoreSheet.ts`、`result/score-summary/index.vue`、`artListTableDefaults.ts`、`artDetailDisplayConfig.ts`。
- 结构优化：将评分模式标签、评分结果和评分记录展示回退逻辑集中到 `frontend/src/views/crehn/review/scorePresentation.ts`，评分分配、专家评分和评分表预览共用，避免继续复制分支；新增对应 3 项单元测试。
- 页面拆分：将专家评分输入、评语和保存操作区移至 `frontend/src/views/crehn/review/components/ReviewScorePanel.vue`；父页面继续唯一持有评分状态、校验和保存编排，子组件不新增 API 或业务规则。
- 分配页拆分：将单条分配和混合范围分配重复的评分模式、互斥、可见性和规则 JSON 字段移至 `frontend/src/views/crehn/review-assignment/components/ReviewScoreConfigurationFields.vue`；两种布局共用受控字段组件，父页面继续持有提交和预览逻辑。
- 静态证据：`git diff --check` 通过；定向 Vitest 通过 3 个文件、39 个测试；前端全量 `vue-tsc` 被既有未修改的 `src/views/system/workbench/index.vue:460` 类型错误阻断；后端编译因当前环境无 `mvn`/`mvnw.cmd` 未执行；定向 ESLint 仍受仓库既有 CRLF/Prettier 基线问题影响，未形成通过证据。
- 最新验证：定向 Vitest 3 个文件、39 个测试通过；`vue-tsc` 仍仅报告上述未修改的既有错误。
- 当前假设：仅评语模式评语必填；不产生数值分和等级；没有可计算平均分时不生成排名/分数区间奖项。该假设需在后续业务确认中固化或调整。
