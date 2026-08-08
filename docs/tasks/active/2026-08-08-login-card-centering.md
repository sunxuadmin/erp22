# 登录页登录卡片错位最小 CSS 修复

## 状态

- 当前状态：`IMPLEMENTED / STATIC_VERIFIED / BROWSER_NEEDS_LOCAL_DEPS`
- 当前权限：仅本地代码实施与验证
- Git 提交、推送、部署、账号操作、SQL：未授权

## 问题

弹出态使用 `top/left: 50%`，但居中依赖动画 `transform`；减少动画时可能出现右下裁切。

## 范围

- 允许实现文件：`frontend/src/views/login.vue`
- 仅调整登录卡片居中相关 CSS，不改变接口、数据库、依赖或部署配置。

## 验收目标

- [x] `git diff --check` 与静态 CSS/`prefers-reduced-motion` 断言：`PASS`
- [ ] 浏览器场景（直达管理登录、入口打开、返回入口；1440/1472/1024/390；真实 reduced-motion）：`BROWSER_NEEDS_LOCAL_DEPS`
  - 未运行原因：本工作树 `frontend/node_modules` 缺少可用 Vite、Playwright、Prettier 二进制，且 `pnpm` 补装受权限/网络阻断。

## 变更与授权

- 无 API、配置、数据库、部署或 Git 变更。
