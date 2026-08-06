# 2026-08-06 超大文件分拆风险排序

## 状态

- 任务编号：`REF-001`
- 当前状态：`BLOCKED / AUDIT_COMPLETE / REFACTOR_NOT_STARTED`
- 阻断原因：最高风险文件与当前未提交的QA、工作台、门户、FLOW和结果改动重叠；后端本机又无 Java/Maven 编译门禁。在当前脏工作区直接做大规模移动会放大覆盖和无法归因风险。

## 静态盘点

### 前端优先级

1. `frontend/src/views/system/workbench/index.vue`：6336行；当前 QA-001 已修改，先完成复审/提交边界再拆。
2. `frontend/src/views/crehn/activity/index.vue`：4893行；优先抽离配置包、规则和删除影响对话框，每批必须有定向测试。
3. `frontend/src/views/crehn/project/edit.vue`：4067行；涉及参赛者本人填报主链路，必须在 FLOW-001 运行矩阵后再拆。
4. `frontend/src/views/system/home-config/index.vue`：3325行；与受控配置契约紧密，等 REQ-013 页面清单冻结。
5. `review-assignment/index.vue`（2991）、`audit/index.vue`（2184）、`ArtGlobalTableVisualEditor.vue`（2134）、`SchoolProjectSubmitWidget.vue`（2096）、`review/index.vue`（2091）、`ArtProjectListVisualEditor.vue`（2082）、`SchoolStageNoticeWidget.vue`（2023）、`artDetailDisplayConfig.ts`（1994）、`login.vue`（1884）依次处理。

### 后端优先级

1. `ArtReviewServiceImpl.java`：3013行。
2. `ArtDetailDisplayConfigService.java`：2467行。
3. `ArtProjectServiceImpl.java`：2177行；当前 FLOW-001 有重叠改动。
4. `ArtResultServiceImpl.java`：2126行；当前 PORTAL-003 有重叠改动。
5. `ArtActivityServiceImpl.java`：1419行、`ArtHomeConfigServiceImpl.java`：1302行、`ReviewScoreSheetSigningService.java`：1211行。

## 实施门禁

- 每次只拆一个明确所有者、一个业务切片，先固定行为测试，不同时做功能变更。
- 后端分拆必须在可执行模块编译/测试的环境中进行；前端分拆必须保持 typecheck、目标测试和构建通过。
- 本轮不移动上述文件，不以大规模格式化代替分拆，不覆盖当前未提交修改。

## 授权状态

- 静态盘点/任务卡：已完成。
- 代码分拆：待重叠任务收口并为首个切片新建唯一子任务卡。
- SQL、服务器、部署、Git、外部发送、删除：未授权。
