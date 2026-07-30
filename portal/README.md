# CREHN 独立门户

独立 Vue 3/Vite 门户。它通过公开 CMS 接口按 `VITE_PORTAL_SITE_CODE`
查找启用站点，不依赖数据库自增 ID，也不依赖 DYZ 的运行目录。

## 页面拆分

- `src/App.vue`：页面骨架、版本/动效控制、入口跳转与维护提示。
- `src/components/BackgroundLayer.vue`：圆环、光球、三角形、立方体、气泡、星点、扫光等独立背景元素。
- `src/components/HeroTitle.vue`：左侧品牌主题与说明文案。
- `src/components/EntryCard.vue`：右侧活动入口卡片。
- `src/components/VersionTwoLayout.vue`：图三风格的居中标题和三列竖卡页面。
- `src/components/PortraitEntryCard.vue`：v2 竖向入口卡片。
- `src/components/EntryIllustration.vue`：学士帽、书本、钢笔尖三套独立 SVG 插画。
- `src/config/entries.ts`：运行时配置合并、版本选择、默认值与安全校验。
- `src/styles/global.css`：两套版本样式、慢速动效和响应式布局。
- `public/config.js`：部署后仍可直接修改的文案、颜色、入口、版本和动效配置。

页面不使用整张参考图作背景。所有背景装饰均为独立 DOM/CSS 元素：远景几何体使用分级高斯模糊，玻璃圆球保留清晰边缘和折射，只沿垂直方向缓慢浮动。动效会响应暂停按钮和系统的“减少动态效果”设置。

## 两个版本

默认版本在 `public/config.js` 中设置：

```js
version: {
  active: "v1",
  allowQueryOverride: true,
  userToggle: false,
  options: [
    { id: "v1", label: "参考稿版" },
    { id: "v2", label: "竖卡展厅版" }
  ]
}
```

- `/crehn/`：打开配置中的默认版本。
- `/crehn/?version=v1`：临时预览参考稿版。
- `/crehn/?version=v2`：临时预览图三风格竖卡展厅版。
- 将 `userToggle` 改为 `true`：在顶部显示版本切换按钮，适合现场对比。

`hero`、`entries`、入口链接和提示文案由两个版本共用；`variants.v1` 和 `variants.v2` 只写颜色和动效差异。修改服务器 `/crehn/config.js` 后刷新页面即可生效，不需要重新构建。

## 配置内容

`public/config.js` 支持：

- `hero`：网页标题、品牌名、主标题、副标题、说明、页脚。
- `entries`：三个入口的编号、标题、说明、链接、打开方式、提示与主题色。
- `theme`：背景、文字、卡片、玻璃透明度等颜色参数。
- `motion`：总开关、速度、强度、气泡/尘点数量、入场与卡片扫光。
- `variants`：每个版本独立覆盖上述配置。

入口链接为空、为 `#` 或使用 `javascript:` / `data:` 等不安全协议时，不会跳转，只显示维护提示。

## 本地运行与构建

```powershell
cd C:\Users\A\Documents\CREHN\portal
pnpm install
pnpm dev
```

生成可发布静态包：

```powershell
pnpm run build
```

产物：

```text
dist/index.html
dist/config.js
dist/assets/
```

正式发布由根目录 `deploy/web/Dockerfile` 将 `dist` 写入 Web 镜像，不直接上传源码目录。
