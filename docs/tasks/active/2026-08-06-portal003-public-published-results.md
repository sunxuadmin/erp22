# 2026-08-06 公共结果发布与查询链路

## 状态

- 任务编号：`PORTAL-003`
- 当前状态：`IMPLEMENTED / STATIC_VERIFIED / NEEDS_BACKEND_COMPILE / NEEDS_BROWSER / NEEDS_DATABASE`
- 实现授权：已确认
- SQL执行、服务器连接、部署、Git提交/推送、删除：未授权

## 冻结契约

- 公开接口只读取 `result_status=published` 的结果，不读取评分汇总草案或生成中结果。
- 公开响应不返回结果、项目、学校、活动、发布人等内部主键，也不返回总分、评委明细或评语汇总 JSON。
- 平均分/等级仅在 `show_score=true` 时公开，排名仅在 `show_rank=true` 时公开；奖项、项目名称、单位和发布时间随已发布结果公开。
- 门户只展示服务端返回的公开VO，不在浏览器中从完整结果对象做脱敏。

## 允许修改范围

- `backend/ruoyi-modules/ruoyi-crehn/src/main/java/org/dromara/crehn/domain/vo/PublicReviewResultVo.java`
- `backend/ruoyi-modules/ruoyi-crehn/src/main/java/org/dromara/crehn/result/`
- `portal/src/components/PortalContent.vue`
- 本任务卡、总控索引和交接文档

## 验收

- [x] 公开查询在服务端固定 `result_status=published`；真实数据拒绝证据 `NEEDS_DATABASE`。
- [x] 专用公开VO不含内部主键、发布人、总分、评委明细和评语JSON。
- [x] 公开VO仅在 `show_score=true` / `show_rank=true` 时映射分数/等级与排名。
- [x] 门户已增加结果列表、空状态与独立错误降级，结果API失败不遮蔽CMS内容。
- [x] 门户 typecheck/构建 `PASS`；后端编译 `NOT_RUN`；浏览器 `NEEDS_BROWSER`；数据库 `NOT_RUN`；服务器 `NEEDS_SERVER`；生产 `NOT_RUN`。
