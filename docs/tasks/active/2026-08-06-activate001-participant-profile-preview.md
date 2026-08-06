# 2026-08-06 激活前脱敏资料预览与本人核对

## 状态

- 任务编号：`ACTIVATE-001`
- 当前状态：`IMPLEMENTED / STATIC_VERIFIED / NEEDS_BACKEND_COMPILE / NEEDS_BROWSER`
- 实现授权：已确认
- SQL执行、服务器连接、部署、Git提交/推送、删除：未授权

## 冻结契约

- 未登录参赛者只能凭高熵一次性激活码获取对应待激活资料的脱敏预览，不接受参赛者ID、用户ID、学校ID等可枚主键。
- 预览和激活共用“已签发、未过期、资料待确认”校验；接口按IP限流，响应不返回内部主键、账号、原始证件号、完整手机或完整邮箱。
- 前端必须先用当前激活码完成资料核对，修改激活码后清空预览和确认状态，随后才能确认激活。

## 允许修改范围

- `backend/ruoyi-modules/ruoyi-crehn/src/main/java/org/dromara/crehn/domain/bo/ParticipantActivationPreviewBo.java`
- `backend/ruoyi-modules/ruoyi-crehn/src/main/java/org/dromara/crehn/domain/vo/ParticipantActivationPreviewVo.java`
- `backend/ruoyi-modules/ruoyi-crehn/src/main/java/org/dromara/crehn/participant/`
- `frontend/src/api/crehn/participant.ts`
- `frontend/src/views/activate.vue`
- 本任务卡、总控索引和交接文档

## 验收证据

- 静态/构建：管理端全量 typecheck、16文件/94测试、目标 ESLint 通过；后端本机无 Java/Maven，模块编译 `NOT_RUN`。
- 浏览器：`NEEDS_BROWSER`，需验证无效、过期、已使用及有效激活码的交互。
- 数据库：`NOT_RUN`，本任务无迁移；真实数据读取未验证。
- 服务器/生产：`NEEDS_SERVER` / `NOT_RUN`。

## 风险

- IP限流不能替代网关风控；生产仍需结合访问日志、反自动化策略与异常告警验收。
- 姓名、学校和活动仍属于个人资料；当前依赖24位随机激活码和限流控制访问，禁止加入任何按数字主键查询的兼容接口。
