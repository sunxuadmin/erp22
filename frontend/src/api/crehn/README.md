# Art Review API

本目录存放省级大学生艺术展演评审系统的业务 API 封装。

## 目录规划

- `registration`: 注册码接口。
- `activity`: 活动与配置接口。
- `school`: 学校接口。
- `project`: 项目申报接口。
- `audit`: 材料审核接口。
- `expert`: 专家管理接口。
- `review`: 评审任务与评分接口。
- `result`: 结果管理接口。
- `report`: 统计报表接口。

接口封装中的路径对应后端 `/crehn/<module>/...`。生产浏览器请求会由请求客户端在前面加上 `VITE_APP_BASE_API=/prod-api`，形成 `/prod-api/crehn/<module>/...`；Nginx 去除代理前缀后转发到后端 `/crehn/<module>/...`。

`/admin/` 是管理端页面上下文，公共门户 `/crehn/` 是独立前端页面上下文，二者都不是本目录接口封装需要重复拼接的 API 前缀。
