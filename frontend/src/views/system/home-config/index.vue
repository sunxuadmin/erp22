<template>
  <div class="p-2 home-config-page">
    <el-card shadow="hover" class="mb-3">
      <div class="page-head">
        <div>
          <span class="page-kicker">PORTAL HOME</span>
          <h2>门户页面配置</h2>
          <p>活动首页保持固定模板，Header / Footer 全站通用；其他页面可新增、隐藏、删除并使用自由画布编辑内容。</p>
        </div>
        <div class="page-actions">
          <el-button icon="Refresh" :loading="loading || moduleLoading || cardLoading" @click="refreshAll">刷新</el-button>
          <el-button v-hasPermi="['system:homeConfig:edit']" type="primary" icon="Check" :loading="saving" @click="submitSiteConfig">
            保存 Header / Footer
          </el-button>
        </div>
      </div>
    </el-card>

    <el-tabs v-model="activeMainTab" class="home-config-tabs">
      <el-tab-pane label="页面画布" name="builder">
        <div class="builder-toolbar">
          <div>
            <span class="page-kicker">PAGE CANVAS</span>
            <h3>页面级自由画布</h3>
            <p>先在左侧选择页面，再拖动当前页面的内容块；活动首页保持固定 Hero 模板，不参与任意定位。</p>
          </div>
          <div class="builder-toolbar-actions">
            <el-select v-model="activeModuleCode" style="width: 180px" @change="handleModuleChange">
              <el-option v-for="item in modules" :key="item.moduleCode" :label="item.moduleTitle || item.moduleCode" :value="item.moduleCode" />
            </el-select>
            <el-select v-model="selectedModuleBuilder.cardLayout" :disabled="selectedPageIsFixedHome" style="width: 132px">
              <el-option label="流式页面" value="flow" />
              <el-option label="自由画布" value="free" />
            </el-select>
            <el-input-number v-model="selectedModuleBuilder.h" :disabled="selectedPageIsFixedHome" :min="360" :max="3600" :step="80" controls-position="right" />
            <el-button icon="Refresh" :loading="loading || moduleLoading || cardLoading" @click="refreshAll">刷新</el-button>
            <el-button v-hasPermi="['system:homeConfig:edit']" type="primary" icon="Check" :loading="saving || moduleSaving || cardSaving" @click="saveBuilderLayout">保存当前页面</el-button>
          </div>
        </div>

        <div class="builder-shell">
          <aside class="builder-palette">
            <div class="builder-panel-title">
              <strong>页面列表</strong>
              <span>点击切换编辑对象</span>
            </div>
            <div
              v-for="item in builderModules"
              :key="item.moduleCode"
              class="builder-palette-item"
              :class="{ 'is-selected': activeModuleCode === item.moduleCode, 'is-hidden': !item.enabled }"
              @click="selectBuilderModule(item)"
            >
              <div>
                <strong>{{ item.moduleTitle || item.moduleCode }}</strong>
                <span>{{ item.moduleCode }} · {{ moduleTypeLabel(item.moduleType) }}</span>
              </div>
              <el-tag size="small" :type="item.enabled ? 'success' : 'info'">{{ item.enabled ? '显示' : '隐藏' }}</el-tag>
            </div>
            <el-button v-hasPermi="['system:homeConfig:edit']" class="w-full" type="primary" plain icon="Plus" @click="addModule">新增自定义页面</el-button>
          </aside>

          <section class="builder-stage-wrap">
            <div class="builder-stage-head">
              <span>{{ activeModule?.moduleTitle || '页面画布' }}</span>
              <small>{{ selectedPageIsFixedHome ? 'Hero 固定保留；这里管理 Hero 下方的中间内容块。' : '自由画布下可拖动卡片；流式页面按排序自动排版。' }}</small>
            </div>
            <div
              ref="builderCanvasRef"
              class="builder-stage"
              :class="{ 'is-free': activePageCardLayout === 'free' && !selectedPageIsFixedHome, 'is-fixed-home': selectedPageIsFixedHome }"
              :style="pageCanvasStyle"
            >
              <div v-if="selectedPageIsFixedHome" class="builder-fixed-home-preview">
                <span>固定活动首页</span>
                <strong>Hero 首屏 + 中间内容区</strong>
                <p>Hero 首屏继续使用当前清爽蓝绿艺术展布局。下方可按需添加图文、HTML、轮播、通知、时间线等内容块。</p>
              </div>
              <template v-if="!selectedPageIsFixedHome || builderCanvasCards.length">
                <div
                  v-for="card in builderCanvasCards"
                  :key="card.id || card.cardCode"
                  class="builder-card-node"
                  :class="{ 'is-selected': selectedBuilderKind === 'card' && selectedBuilderId === cardKey(card), 'is-hidden': !card.enabled, 'is-flow-dragging': isFlowCardDragging(card) }"
                  :data-builder-card-key="cardKey(card)"
                  :style="builderCardStyle(card)"
                  @pointerdown.left.stop="startCardPointerDrag($event, card)"
                  @click.stop="selectBuilderCard(card)"
                >
                  <strong>{{ card.cardTitle || card.cardCode }}</strong>
                  <span>{{ cardTypeLabel(card.cardType || card.templateCode) }}</span>
                  <div class="builder-node-actions" @pointerdown.stop>
                    <el-button v-if="activePageUsesFlowLayout" size="small" link type="primary" icon="Top" :disabled="!canMoveBuilderCard(card, -1)" @click.stop="moveBuilderCard(card, -1)">上移</el-button>
                    <el-button v-if="activePageUsesFlowLayout" size="small" link type="primary" icon="Bottom" :disabled="!canMoveBuilderCard(card, 1)" @click.stop="moveBuilderCard(card, 1)">下移</el-button>
                    <el-button size="small" link type="primary" @click.stop="openCardForm(card)">编辑</el-button>
                    <el-button size="small" link type="primary" @click.stop="duplicateBuilderCard(card)">复制</el-button>
                    <el-button size="small" link type="danger" @click.stop="handleCardDelete(card)">删除</el-button>
                  </div>
                </div>
                <el-empty v-if="!builderCanvasCards.length && !selectedPageIsFixedHome" description="当前页面还没有内容块">
                  <div class="builder-empty-actions">
                    <el-button size="small" type="primary" @click.stop="addPresetCard(componentPresets[0])">添加富文本</el-button>
                    <el-button size="small" plain @click.stop="addPresetCard(componentPresets[1])">添加图文卡</el-button>
                    <el-button size="small" plain @click.stop="addPresetCard(componentPresets[4])">添加按钮组</el-button>
                  </div>
                </el-empty>
              </template>
            </div>
          </section>

          <aside class="builder-inspector">
            <template v-if="activeModule">
              <div class="builder-panel-title">
                <strong>页面属性</strong>
                <span>{{ activeModule.moduleCode }}</span>
              </div>
              <el-form :model="activeModule" label-width="82px" class="builder-inspector-form">
                <el-form-item label="标题"><el-input v-model="activeModule.moduleTitle" /></el-form-item>
                <el-form-item label="类型">
                  <el-select v-model="activeModule.moduleType" class="w-full" :disabled="selectedPageIsFixedHome" @change="syncModuleTemplate(activeModule)">
                    <el-option v-for="item in moduleTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="锚点"><el-input v-model="activeModule.anchor" :disabled="selectedPageIsFixedHome" /></el-form-item>
                <el-row :gutter="10">
                  <el-col :span="12"><el-form-item label="显示"><el-switch v-model="activeModule.enabled" :disabled="selectedPageIsFixedHome" /></el-form-item></el-col>
                  <el-col :span="12"><el-form-item label="导航"><el-switch v-model="activeModule.navEnabled" /></el-form-item></el-col>
                  <el-col :span="12"><el-form-item label="头部"><el-switch v-model="activeModule.showPageHeader" /></el-form-item></el-col>
                  <el-col :span="12"><el-form-item label="更多"><el-switch v-model="activeModule.showPageMore" /></el-form-item></el-col>
                  <el-col :span="12"><el-form-item label="标识"><el-switch v-model="activeModule.showPageEyebrow" /></el-form-item></el-col>
                  <el-col :span="12"><el-form-item label="标题"><el-switch v-model="activeModule.showPageTitle" /></el-form-item></el-col>
                </el-row>
                <el-row :gutter="10">
                  <el-col :span="12"><el-form-item label="页面高"><el-input-number v-model="selectedModuleBuilder.h" class="w-full" :disabled="selectedPageIsFixedHome" :min="360" :max="3600" /></el-form-item></el-col>
                  <el-col :span="12"><el-form-item label="卡片布局"><el-select v-model="selectedModuleBuilder.cardLayout" class="w-full" :disabled="selectedPageIsFixedHome"><el-option label="流式" value="flow" /><el-option label="自由" value="free" /></el-select></el-form-item></el-col>
                </el-row>
              </el-form>

              <div class="builder-component-library">
                <div class="builder-card-board-head">
                  <strong>组件库</strong>
                  <span>点击添加到当前页面</span>
                </div>
                <div class="component-preset-grid">
                  <button
                    v-for="item in componentPresets"
                    :key="item.key"
                    type="button"
                    class="component-preset-card"
                    @click="addPresetCard(item)"
                  >
                    <span class="component-preset-icon">{{ item.icon }}</span>
                    <strong>{{ item.label }}</strong>
                    <em>{{ item.desc }}</em>
                  </button>
                </div>
              </div>

              <template v-if="selectedBuilderCard">
                <div class="builder-panel-title mt-3">
                  <strong>卡片属性</strong>
                  <span>{{ selectedBuilderCard.cardCode || selectedBuilderCard.id }}</span>
                </div>
                <el-form :model="selectedBuilderCard" label-width="70px" class="builder-inspector-form">
                  <el-form-item label="标题"><el-input v-model="selectedBuilderCard.cardTitle" /></el-form-item>
                  <el-form-item label="类型">
                    <el-select v-model="selectedBuilderCard.cardType" class="w-full">
                      <el-option v-for="item in cardTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-form-item>
                  <el-row v-if="!activePageUsesFlowLayout" :gutter="10">
                    <el-col :span="12"><el-form-item label="X"><el-input-number v-model="selectedCardBuilder.x" class="w-full" :min="0" :max="1200" /></el-form-item></el-col>
                    <el-col :span="12"><el-form-item label="Y"><el-input-number v-model="selectedCardBuilder.y" class="w-full" :min="0" :max="900" /></el-form-item></el-col>
                    <el-col :span="12"><el-form-item label="宽"><el-input-number v-model="selectedCardBuilder.w" class="w-full" :min="120" :max="980" /></el-form-item></el-col>
                    <el-col :span="12"><el-form-item label="高"><el-input-number v-model="selectedCardBuilder.h" class="w-full" :min="80" :max="720" /></el-form-item></el-col>
                    <el-col :span="12"><el-form-item label="层级"><el-input-number v-model="selectedCardBuilder.z" class="w-full" :min="0" :max="99" /></el-form-item></el-col>
                    <el-col :span="12"><el-form-item label="显示"><el-switch v-model="selectedBuilderCard.enabled" /></el-form-item></el-col>
                  </el-row>
                  <template v-else>
                    <el-alert class="builder-flow-alert" type="info" :closable="false" title="流式页面按上下顺序排版，可拖动卡片排序，也可以使用上移 / 下移。" />
                    <el-form-item label="排序">
                      <el-button-group>
                        <el-button size="small" icon="Top" :disabled="!canMoveBuilderCard(selectedBuilderCard, -1)" @click="moveBuilderCard(selectedBuilderCard, -1)">上移</el-button>
                        <el-button size="small" icon="Bottom" :disabled="!canMoveBuilderCard(selectedBuilderCard, 1)" @click="moveBuilderCard(selectedBuilderCard, 1)">下移</el-button>
                      </el-button-group>
                    </el-form-item>
                    <el-form-item label="显示"><el-switch v-model="selectedBuilderCard.enabled" /></el-form-item>
                  </template>
                </el-form>
                <div class="builder-inspector-actions">
                  <el-button type="primary" plain size="small" @click="openCardForm(selectedBuilderCard)">完整编辑</el-button>
                  <el-button plain size="small" @click="duplicateBuilderCard(selectedBuilderCard)">复制</el-button>
                  <el-button type="danger" plain size="small" @click="handleCardDelete(selectedBuilderCard)">删除</el-button>
                </div>
              </template>
            </template>
            <el-empty v-else description="选择一个模块开始编辑" />
          </aside>
        </div>
      </el-tab-pane>

      <el-tab-pane label="页面管理" name="modules">
        <el-card shadow="hover">
          <div class="sub-head">
            <div>
              <h3>页面列表</h3>
              <p>活动首页固定保留；其他页面可新增、删除、隐藏、修改标题，并自动同步顶部 Header 导航。</p>
            </div>
            <el-button v-hasPermi="['system:homeConfig:edit']" type="primary" plain icon="Plus" @click="addModule">新增页面</el-button>
          </div>

          <el-tabs v-model="activeModuleCode" type="card" class="module-tabs" @tab-change="handleModuleChange">
            <el-tab-pane v-for="item in modules" :key="item.moduleCode" :name="item.moduleCode">
              <template #label>
                <span class="module-tab-label">
                  <span>{{ item.moduleTitle || item.moduleCode }}</span>
                  <el-tag v-if="!item.enabled" size="small" type="info">隐藏</el-tag>
                </span>
              </template>
            </el-tab-pane>
          </el-tabs>

          <template v-if="activeModule">
            <el-form :model="activeModule" label-width="112px" class="module-form">
              <el-row :gutter="16">
                <el-col :xs="24" :md="8">
                  <el-form-item label="页面标题" required>
                    <el-input v-model="activeModule.moduleTitle" maxlength="80" show-word-limit />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="页面编码" required>
                    <el-input v-model="activeModule.moduleCode" :disabled="!!activeModule.id || selectedPageIsFixedHome" maxlength="64" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="页面类型">
                    <el-select v-model="activeModule.moduleType" class="w-full" :disabled="selectedPageIsFixedHome" @change="syncModuleTemplate(activeModule)">
                      <el-option v-for="item in moduleTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="导航锚点">
                    <el-input v-model="activeModule.anchor" :disabled="selectedPageIsFixedHome" placeholder="home / guide" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="英文标识">
                    <el-input v-model="activeModule.eyebrow" placeholder="SUBMISSION GUIDE" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="排序">
                    <el-input-number v-model="activeModule.sortOrder" class="w-full" :min="0" :max="999" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="显示页面">
                    <el-switch v-model="activeModule.enabled" :disabled="selectedPageIsFixedHome" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="显示到导航">
                    <el-switch v-model="activeModule.navEnabled" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="模板编码">
                    <el-input v-model="activeModule.templateCode" placeholder="默认跟随组件页面" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="导航文字">
                    <el-input v-model="activeModule.navTitle" placeholder="默认使用页面标题" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="导航排序">
                    <el-input-number v-model="activeModule.navSortOrder" class="w-full" :min="0" :max="999" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="页面地址">
                    <el-input v-model="activeModule.pageUrl" placeholder="#guide / /login / https://..." />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="默认打开">
                    <el-select v-model="activeModule.defaultOpenMode" class="w-full">
                      <el-option label="当前页打开" value="current" />
                      <el-option label="新标签页" value="blank" />
                      <el-option label="页面内弹窗" value="modal" />
                      <el-option label="浏览器小窗口" value="browserWindow" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="小窗宽度">
                    <el-input-number v-model="activeModule.popupWidth" class="w-full" :min="480" :max="1280" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="小窗高度">
                    <el-input-number v-model="activeModule.popupHeight" class="w-full" :min="360" :max="900" controls-position="right" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="12">
                  <el-form-item label="页面简介">
                    <el-input v-model="activeModule.intro" maxlength="300" show-word-limit />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="6">
                  <el-form-item label="更多文字">
                    <el-input v-model="activeModule.moreText" placeholder="通知公告可用" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="6">
                  <el-form-item label="更多链接">
                    <el-input v-model="activeModule.moreLink" placeholder="#notice" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24"><el-divider content-position="left">页面头部模块</el-divider></el-col>
                <el-col :xs="24" :md="4"><el-form-item label="显示头部"><el-switch v-model="activeModule.showPageHeader" /></el-form-item></el-col>
                <el-col :xs="24" :md="4"><el-form-item label="显示标识"><el-switch v-model="activeModule.showPageEyebrow" /></el-form-item></el-col>
                <el-col :xs="24" :md="4"><el-form-item label="显示标题"><el-switch v-model="activeModule.showPageTitle" /></el-form-item></el-col>
                <el-col :xs="24" :md="4"><el-form-item label="显示简介"><el-switch v-model="activeModule.showPageIntro" /></el-form-item></el-col>
                <el-col :xs="24" :md="4"><el-form-item label="更多按钮"><el-switch v-model="activeModule.showPageMore" /></el-form-item></el-col>
                <el-col :xs="24" :md="4"><el-form-item label="手机隐藏"><el-switch v-model="activeModule.hidePageHeaderMobile" /></el-form-item></el-col>
                <el-col :xs="24" :md="6">
                  <el-form-item label="头部对齐">
                    <el-select v-model="activeModule.pageHeaderAlign" class="w-full">
                      <el-option label="左对齐" value="left" />
                      <el-option label="居中" value="center" />
                      <el-option label="左右分栏" value="split" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="6">
                  <el-form-item label="头部尺寸">
                    <el-select v-model="activeModule.pageHeaderSize" class="w-full">
                      <el-option label="紧凑" value="compact" />
                      <el-option label="常规" value="normal" />
                      <el-option label="突出" value="large" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="12">
                  <el-form-item label="更多打开">
                    <el-select v-model="activeModule.pageMoreOpenMode" class="w-full">
                      <el-option label="跟随页面默认" value="pageDefault" />
                      <el-option label="当前页打开" value="current" />
                      <el-option label="新标签页" value="blank" />
                      <el-option label="页面内弹窗" value="modal" />
                      <el-option label="浏览器小窗口" value="browserWindow" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="12">
                  <el-form-item label="标识 CSS">
                    <el-input v-model="activeModule.pageEyebrowCss" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="color: #006fb8; letter-spacing: .2em;" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="12">
                  <el-form-item label="标题 CSS">
                    <el-input v-model="activeModule.pageTitleCss" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="font-size: 42px; color: #102a43;" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="12">
                  <el-form-item label="简介 CSS">
                    <el-input v-model="activeModule.pageIntroCss" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="max-width: 620px; color: #31596d;" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="12">
                  <el-form-item label="更多 CSS">
                    <el-input v-model="activeModule.pageMoreCss" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="color: #006fb8; font-weight: 900;" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24">
                  <el-form-item label="头部 CSS">
                    <el-input v-model="activeModule.pageHeaderCss" type="textarea" :rows="3" maxlength="3000" show-word-limit placeholder="margin-bottom: 34px;" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>

            <el-collapse class="css-config-collapse">
              <el-collapse-item title="当前页面自定义 CSS" name="module-css">
                <el-alert
                  class="mb-3"
                  type="info"
                  :closable="false"
                  show-icon
                  title="前台会自动作用到当前页面范围，例如 .portal-module-guide；移动端 CSS 会包裹在 760px 以下。"
                />
                <el-form :model="activeModule" label-width="112px">
                  <el-row :gutter="16">
                    <el-col :xs="24" :md="8">
                      <el-form-item label="自定义类名">
                        <el-input v-model="activeModule.style.customClass" maxlength="120" placeholder="如 hero-art-polished" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24">
                      <el-form-item label="模块 CSS">
                        <el-input v-model="activeModule.style.customCss" type="textarea" :rows="6" maxlength="6000" show-word-limit :placeholder="moduleCssPlaceholder" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24">
                      <el-form-item label="移动端 CSS">
                        <el-input v-model="activeModule.style.customMobileCss" type="textarea" :rows="4" maxlength="4000" show-word-limit :placeholder="mobileCssPlaceholder" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                </el-form>
              </el-collapse-item>
            </el-collapse>

            <template v-if="activeModule.moduleType === 'article' || activeModule.templateCode === 'article'">
              <el-divider content-position="left">文章页内容</el-divider>
              <el-form :model="activeModule.article" label-width="112px">
                <el-row :gutter="16">
                  <el-col :xs="24" :md="8">
                    <el-form-item label="正文格式">
                      <el-select v-model="activeModule.article.contentMode" class="w-full">
                        <el-option label="Markdown" value="markdown" />
                        <el-option label="HTML" value="html" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :md="8"><el-form-item label="返回上页"><el-switch v-model="activeModule.article.showBackButton" /></el-form-item></el-col>
                  <el-col :xs="24" :md="8"><el-form-item label="回到首页"><el-switch v-model="activeModule.article.showHomeButton" /></el-form-item></el-col>
                  <el-col :xs="24">
                    <el-form-item label="正文内容">
                      <el-input v-model="activeModule.article.content" type="textarea" :rows="10" maxlength="30000" show-word-limit placeholder="支持 Markdown 标题、链接、图片；选择 HTML 时可直接写 HTML 片段。" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :md="12"><el-form-item label="返回文字"><el-input v-model="activeModule.article.backText" placeholder="返回上页" /></el-form-item></el-col>
                  <el-col :xs="24" :md="12"><el-form-item label="首页文字"><el-input v-model="activeModule.article.homeText" placeholder="回到首页" /></el-form-item></el-col>
                  <el-col :xs="24">
                    <el-form-item label="自定义按钮">
                      <el-input v-model="activeModule.article.buttonsText" type="textarea" :rows="3" placeholder="每行一个：按钮文字 | 链接地址 | 打开方式(current/blank/modal/article)" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form>
            </template>

            <div class="module-actions">
              <el-button v-hasPermi="['system:homeConfig:edit']" type="primary" icon="Check" :loading="moduleSaving" @click="submitModule">保存当前页面</el-button>
              <el-button v-hasPermi="['system:homeConfig:remove']" type="danger" plain icon="Delete" :disabled="selectedPageIsFixedHome || modules.length <= 1" @click="handleModuleDelete">删除当前页面</el-button>
            </div>

            <div class="sub-head card-head">
              <div>
                <h3>页面内容块</h3>
                <p>选择预置组件模板并填写配置项；自定义画布页面可在“页面画布”中拖动定位。</p>
              </div>
              <el-button v-hasPermi="['system:homeConfig:edit']" type="primary" plain icon="Plus" @click="openCardForm()">新增卡片</el-button>
            </div>

            <el-table v-loading="cardLoading" border :data="cards" row-key="id">
              <el-table-column label="排序" prop="sortOrder" width="80" align="center" />
              <el-table-column label="组件" prop="cardType" width="130">
                <template #default="{ row }">{{ cardTypeLabel(row.cardType || row.templateCode) }}</template>
              </el-table-column>
              <el-table-column label="标题" prop="cardTitle" min-width="180">
                <template #default="{ row }">
                  <strong>{{ row.cardTitle }}</strong>
                  <el-tag v-if="row.highlight" class="ml-2" type="primary">重点</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="内容" prop="cardContent" min-width="280">
                <template #default="{ row }"><span class="line-clamp">{{ row.cardContent || row.cardSubtitle }}</span></template>
              </el-table-column>
              <el-table-column label="按钮/链接" min-width="160">
                <template #default="{ row }">{{ row.linkText || '-' }} <span class="muted">{{ row.linkUrl }}</span></template>
              </el-table-column>
              <el-table-column label="启用" prop="enabled" width="90" align="center">
                <template #default="{ row }"><el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag></template>
              </el-table-column>
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <el-button v-hasPermi="['system:homeConfig:edit']" link type="primary" icon="Edit" @click="openCardForm(row)">编辑</el-button>
                  <el-button v-hasPermi="['system:homeConfig:remove']" link type="danger" icon="Delete" :disabled="isProtectedHomeHeroCard(row)" @click="handleCardDelete(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
            <pagination v-show="cardTotal > 0" v-model:page="cardQuery.pageNum" v-model:limit="cardQuery.pageSize" :total="cardTotal" @pagination="getCardList" />
          </template>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="Header 配置" name="header">
        <el-card shadow="hover">
          <el-form :model="siteForm" label-width="110px">
            <el-row :gutter="16">
              <el-col :xs="24" :md="12">
                <el-form-item label="站点名称" required>
                  <el-input v-model="siteForm.siteName" maxlength="128" show-word-limit />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="首页链接">
                  <el-input v-model="siteForm.homeLink" placeholder="#home" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="Logo 上传">
                  <image-upload v-model="siteForm.logoOssId" :limit="1" :file-size="5" :file-type="['png', 'jpg', 'jpeg', 'webp']" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="Logo 地址">
                  <el-input v-model="siteForm.logoUrl" placeholder="assets/logo-icon.png 或图片 URL" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>

          <div class="sub-head mt-4">
            <div>
              <h3>登录态文案</h3>
              <p>登录后首页右上角按账号类型显示对应文案。</p>
            </div>
          </div>
          <el-form :model="loginStateForm" label-width="112px">
            <el-row :gutter="16">
              <el-col :xs="24" :md="12">
                <el-form-item label="管理账户">
                  <el-input v-model="loginStateForm.adminText" maxlength="40" placeholder="管理账户已登录" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="学校账户">
                  <el-input v-model="loginStateForm.schoolText" maxlength="40" placeholder="学校账户已登录" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="专家账户">
                  <el-input v-model="loginStateForm.expertText" maxlength="40" placeholder="专家账户已登录" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="兜底文案">
                  <el-input v-model="loginStateForm.defaultText" maxlength="40" placeholder="已登录" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>

          <div class="sub-head mt-4">
            <div>
              <h3>Header 按钮</h3>
              <p>顶部导航由启用且显示到导航的页面自动生成；这里维护登录入口、注册、工作台等右侧按钮。</p>
            </div>
            <el-button v-hasPermi="['system:homeConfig:edit']" icon="Plus" @click="addHeaderButton">新增按钮</el-button>
          </div>
          <el-table border :data="headerButtons" row-key="sortOrder">
            <el-table-column label="显示" width="80" align="center">
              <template #default="{ row }"><el-switch v-model="row.enabled" /></template>
            </el-table-column>
            <el-table-column label="文字" min-width="140">
              <template #default="{ row }"><el-input v-model="row.text" maxlength="40" /></template>
            </el-table-column>
            <el-table-column label="链接" min-width="170">
              <template #default="{ row }"><el-input v-model="row.link" /></template>
            </el-table-column>
            <el-table-column label="样式" width="140">
              <template #default="{ row }">
                <el-select v-model="row.style" class="w-full">
                  <el-option label="浅色" value="ghost" />
                  <el-option label="实色" value="solid" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="PC" width="72" align="center">
              <template #default="{ row }"><el-switch v-model="row.showDesktop" /></template>
            </el-table-column>
            <el-table-column label="手机" width="72" align="center">
              <template #default="{ row }"><el-switch v-model="row.showMobile" /></template>
            </el-table-column>
            <el-table-column label="排序" width="130">
              <template #default="{ row }"><el-input-number v-model="row.sortOrder" :min="0" :max="999" controls-position="right" /></template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ $index }"><el-button link type="danger" icon="Delete" @click="removeHeaderButton($index)">删除</el-button></template>
            </el-table-column>
          </el-table>

          <el-collapse class="css-config-collapse mt-4">
            <el-collapse-item title="Header 响应式显示 / 自定义 HTML" name="header-display">
              <el-alert class="mb-3" type="info" :closable="false" show-icon title="桌面端与手机端可分别控制显示项；自定义 HTML 会按端独立渲染。" />
              <el-form :model="siteStyleForm" label-width="132px">
                <el-divider content-position="left">桌面端 Header</el-divider>
                <el-row :gutter="16">
                  <el-col :xs="24" :md="6"><el-form-item label="显示 Header"><el-switch v-model="siteStyleForm.headerDesktop.showHeader" /></el-form-item></el-col>
                  <el-col :xs="24" :md="6"><el-form-item label="显示 Logo"><el-switch v-model="siteStyleForm.headerDesktop.showLogo" /></el-form-item></el-col>
                  <el-col :xs="24" :md="6"><el-form-item label="显示站点名"><el-switch v-model="siteStyleForm.headerDesktop.showBrandName" /></el-form-item></el-col>
                  <el-col :xs="24" :md="6"><el-form-item label="显示导航"><el-switch v-model="siteStyleForm.headerDesktop.showNav" /></el-form-item></el-col>
                  <el-col :xs="24" :md="6"><el-form-item label="登录按钮"><el-switch v-model="siteStyleForm.headerDesktop.showLoginButtons" /></el-form-item></el-col>
                  <el-col :xs="24" :md="6"><el-form-item label="自定义按钮"><el-switch v-model="siteStyleForm.headerDesktop.showCustomButtons" /></el-form-item></el-col>
                  <el-col :xs="24">
                    <el-form-item label="桌面 HTML">
                      <el-input v-model="siteStyleForm.headerDesktop.customHtml" type="textarea" :rows="4" maxlength="6000" show-word-limit placeholder="<div class='header-extra'>...</div>" />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-divider content-position="left">手机端 Header</el-divider>
                <el-row :gutter="16">
                  <el-col :xs="24" :md="6"><el-form-item label="隐藏 Header"><el-switch v-model="siteStyleForm.headerMobile.hideHeader" /></el-form-item></el-col>
                  <el-col :xs="24" :md="6"><el-form-item label="显示 Logo"><el-switch v-model="siteStyleForm.headerMobile.showLogo" /></el-form-item></el-col>
                  <el-col :xs="24" :md="6"><el-form-item label="显示站点名"><el-switch v-model="siteStyleForm.headerMobile.showBrandName" /></el-form-item></el-col>
                  <el-col :xs="24" :md="6"><el-form-item label="显示导航菜单"><el-switch v-model="siteStyleForm.headerMobile.showNavMenu" /></el-form-item></el-col>
                  <el-col :xs="24" :md="6"><el-form-item label="登录按钮"><el-switch v-model="siteStyleForm.headerMobile.showLoginButtons" /></el-form-item></el-col>
                  <el-col :xs="24" :md="6"><el-form-item label="自定义按钮"><el-switch v-model="siteStyleForm.headerMobile.showCustomButtons" /></el-form-item></el-col>
                  <el-col :xs="24">
                    <el-form-item label="手机 HTML">
                      <el-input v-model="siteStyleForm.headerMobile.customHtml" type="textarea" :rows="4" maxlength="6000" show-word-limit placeholder="<div class='mobile-header-extra'>...</div>" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form>
            </el-collapse-item>
          </el-collapse>

          <el-collapse class="css-config-collapse mt-4">
            <el-collapse-item title="Header 自定义 CSS" name="header-css">
              <el-alert class="mb-3" type="info" :closable="false" show-icon title="前台会自动作用到 .site-header 范围，可调整导航、Logo、右侧按钮和登录态样式。" />
              <el-form :model="siteStyleForm" label-width="120px">
                <el-row :gutter="16">
                  <el-col :xs="24">
                    <el-form-item label="Header CSS">
                      <el-input v-model="siteStyleForm.headerCss" type="textarea" :rows="6" maxlength="6000" show-word-limit :placeholder="headerCssPlaceholder" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24">
                    <el-form-item label="移动端 CSS">
                      <el-input v-model="siteStyleForm.headerMobileCss" type="textarea" :rows="4" maxlength="4000" show-word-limit :placeholder="mobileCssPlaceholder" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="Footer 配置" name="footer">
        <el-card shadow="hover">
          <el-form :model="siteForm" label-width="120px">
            <el-row :gutter="16">
              <el-col :xs="24" :md="12"><el-form-item label="页脚标题"><el-input v-model="siteForm.footerTitle" /></el-form-item></el-col>
              <el-col :xs="24" :md="12"><el-form-item label="版权文字"><el-input v-model="siteForm.footerText" /></el-form-item></el-col>
              <el-col :xs="24" :md="12"><el-form-item label="主办单位"><el-input v-model="siteForm.organizer" /></el-form-item></el-col>
              <el-col :xs="24" :md="12"><el-form-item label="承办/协办"><el-input v-model="siteForm.sponsor" /></el-form-item></el-col>
              <el-col :xs="24" :md="12"><el-form-item label="技术支持"><el-input v-model="siteForm.technicalSupport" /></el-form-item></el-col>
              <el-col :xs="24" :md="12"><el-form-item label="联系电话"><el-input v-model="siteForm.contactPhone" /></el-form-item></el-col>
              <el-col :xs="24" :md="12"><el-form-item label="联系邮箱"><el-input v-model="siteForm.contactEmail" /></el-form-item></el-col>
              <el-col :xs="24" :md="12"><el-form-item label="联系地址"><el-input v-model="siteForm.contactAddress" /></el-form-item></el-col>
              <el-col :xs="24" :md="12"><el-form-item label="ICP备案"><el-input v-model="siteForm.icpText" /></el-form-item></el-col>
              <el-col :xs="24" :md="12"><el-form-item label="ICP备案链接"><el-input v-model="siteForm.icpLink" /></el-form-item></el-col>
              <el-col :xs="24" :md="12"><el-form-item label="公安备案"><el-input v-model="siteForm.policeText" /></el-form-item></el-col>
              <el-col :xs="24" :md="12"><el-form-item label="公安备案链接"><el-input v-model="siteForm.policeLink" /></el-form-item></el-col>
              <el-col :xs="24" :md="12"><el-form-item label="公告默认条数"><el-input-number v-model="siteForm.noticeLimit" :min="1" :max="12" controls-position="right" /></el-form-item></el-col>
              <el-col :xs="24"><el-divider content-position="left">Footer 响应式显示 / 自定义 HTML</el-divider></el-col>
              <el-col :xs="24" :md="6"><el-form-item label="桌面显示 Footer"><el-switch v-model="siteStyleForm.footerDesktop.showFooter" /></el-form-item></el-col>
              <el-col :xs="24" :md="6"><el-form-item label="桌面 Logo"><el-switch v-model="siteStyleForm.footerDesktop.showLogo" /></el-form-item></el-col>
              <el-col :xs="24" :md="6"><el-form-item label="桌面标题"><el-switch v-model="siteStyleForm.footerDesktop.showTitle" /></el-form-item></el-col>
              <el-col :xs="24" :md="6"><el-form-item label="桌面联系方式"><el-switch v-model="siteStyleForm.footerDesktop.showContact" /></el-form-item></el-col>
              <el-col :xs="24" :md="6"><el-form-item label="桌面备案"><el-switch v-model="siteStyleForm.footerDesktop.showIcp" /></el-form-item></el-col>
              <el-col :xs="24">
                <el-form-item label="桌面 HTML">
                  <el-input v-model="siteStyleForm.footerDesktop.customHtml" type="textarea" :rows="4" maxlength="6000" show-word-limit placeholder="<div class='footer-extra'>...</div>" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="6"><el-form-item label="手机隐藏 Footer"><el-switch v-model="siteStyleForm.footerMobile.hideFooter" /></el-form-item></el-col>
              <el-col :xs="24" :md="6"><el-form-item label="手机 Logo"><el-switch v-model="siteStyleForm.footerMobile.showLogo" /></el-form-item></el-col>
              <el-col :xs="24" :md="6"><el-form-item label="手机标题"><el-switch v-model="siteStyleForm.footerMobile.showTitle" /></el-form-item></el-col>
              <el-col :xs="24" :md="6"><el-form-item label="手机联系方式"><el-switch v-model="siteStyleForm.footerMobile.showContact" /></el-form-item></el-col>
              <el-col :xs="24" :md="6"><el-form-item label="手机备案"><el-switch v-model="siteStyleForm.footerMobile.showIcp" /></el-form-item></el-col>
              <el-col :xs="24">
                <el-form-item label="手机 HTML">
                  <el-input v-model="siteStyleForm.footerMobile.customHtml" type="textarea" :rows="4" maxlength="6000" show-word-limit placeholder="<div class='mobile-footer-extra'>...</div>" />
                </el-form-item>
              </el-col>
              <el-col :xs="24"><el-divider content-position="left">全站 / Footer 自定义 CSS</el-divider></el-col>
              <el-col :xs="24">
                <el-form-item label="全站 CSS">
                  <el-input v-model="siteForm.customCss" type="textarea" :rows="6" maxlength="6000" show-word-limit :placeholder="globalCssPlaceholder" />
                </el-form-item>
              </el-col>
              <el-col :xs="24">
                <el-form-item label="全站移动端">
                  <el-input v-model="siteStyleForm.globalMobileCss" type="textarea" :rows="4" maxlength="4000" show-word-limit :placeholder="mobileCssPlaceholder" />
                </el-form-item>
              </el-col>
              <el-col :xs="24">
                <el-form-item label="Footer CSS">
                  <el-input v-model="siteStyleForm.footerCss" type="textarea" :rows="6" maxlength="6000" show-word-limit :placeholder="footerCssPlaceholder" />
                </el-form-item>
              </el-col>
              <el-col :xs="24">
                <el-form-item label="Footer 移动端">
                  <el-input v-model="siteStyleForm.footerMobileCss" type="textarea" :rows="4" maxlength="4000" show-word-limit :placeholder="mobileCssPlaceholder" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="cardDialog.visible" :title="cardDialog.title" width="840px" append-to-body>
      <el-form ref="cardFormRef" :model="cardForm" :rules="cardRules" label-width="112px">
        <el-row :gutter="16">
          <el-col :xs="24" :md="12">
            <el-form-item label="所属模块" prop="sectionKey">
              <el-select v-model="cardForm.sectionKey" class="w-full">
                <el-option v-for="item in moduleOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="组件类型" prop="cardType">
              <el-select v-model="cardForm.cardType" class="w-full" @change="handleCardTypeChange">
                <el-option v-for="item in cardTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="排序" prop="sortOrder">
              <el-input-number v-model="cardForm.sortOrder" class="w-full" :min="0" :max="999" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="启用">
              <el-switch v-model="cardForm.enabled" />
            </el-form-item>
          </el-col>
        </el-row>

        <template v-if="cardForm.cardType !== 'spacer'">
          <el-form-item label="卡片标题" prop="cardTitle">
            <el-input v-model="cardForm.cardTitle" maxlength="128" show-word-limit placeholder="按钮、HTML 等组件可留空，前台可隐藏标题" />
          </el-form-item>
          <el-form-item label="副标题">
            <el-input v-model="cardForm.cardSubtitle" maxlength="255" show-word-limit />
          </el-form-item>
          <el-form-item label="摘要内容">
            <el-input
              v-model="cardForm.cardContent"
              type="textarea"
              :rows="4"
              maxlength="8000"
              show-word-limit
              placeholder="卡片上优先显示摘要；长正文、图文和 PDF 请放到下方“详情与附件”。"
            />
          </el-form-item>

          <el-row :gutter="16">
            <el-col :xs="24" :md="12"><el-form-item label="图标文字"><el-input v-model="cardForm.icon" maxlength="16" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="按钮文字"><el-input v-model="cardForm.linkText" maxlength="64" /></el-form-item></el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="链接地址">
                <el-select v-model="cardForm.linkUrl" class="w-full" filterable allow-create placeholder="选择页面、系统页或手填链接">
                  <el-option v-for="item in pageTargetOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="链接类型">
                <el-select v-model="cardForm.linkType" class="w-full">
                  <el-option label="当前页打开" value="current" />
                  <el-option label="跟随目标页面默认" value="pageDefault" />
                  <el-option label="新窗口打开" value="blank" />
                  <el-option label="小窗弹出" value="modal" />
                  <el-option label="浏览器小窗口" value="browserWindow" />
                  <el-option label="打开文章页" value="article" />
                  <el-option label="页内锚点" value="anchor" />
                  <el-option label="后台路由" value="route" />
                  <el-option label="外部链接" value="external" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <template v-if="false && cardForm.linkType === 'article'">
          <el-row :gutter="16">
            <el-col :xs="24" :md="12">
              <el-form-item label="目标文章页">
                <el-select v-model="cardForm.linkUrl" class="w-full" filterable allow-create placeholder="选择文章页或手填 #anchor">
                  <el-option
                    v-for="item in pageTargetOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <template v-if="['modal', 'browserWindow'].includes(cardForm.linkType)">
          <el-row :gutter="16">
            <el-col :xs="24" :md="12"><el-form-item label="小窗标题"><el-input v-model="cardConfig.modalTitle" placeholder="默认使用卡片标题" /></el-form-item></el-col>
            <el-col :xs="24" :md="6"><el-form-item label="小窗宽度"><el-input-number v-model="cardConfig.modalWidth" class="w-full" :min="480" :max="1280" /></el-form-item></el-col>
            <el-col :xs="24" :md="6"><el-form-item label="小窗高度"><el-input-number v-model="cardConfig.modalHeight" class="w-full" :min="360" :max="900" /></el-form-item></el-col>
          </el-row>
        </template>

        <template v-if="cardForm.cardType !== 'spacer' && cardForm.cardType !== 'hero'">
          <el-divider content-position="left">详情与附件</el-divider>
          <el-alert
            class="mb-3"
            type="info"
            :closable="false"
            show-icon
            title="卡片先显示摘要；详情可用 Markdown/HTML 写图文，也可上传 PDF。PDF 前台只使用受控预览地址，不直接暴露 OSS 原始链接。"
          />
          <el-row :gutter="16">
            <el-col :xs="24" :md="8">
              <el-form-item label="详情类型">
                <el-select v-model="cardConfig.detailSource" class="w-full">
                  <el-option label="无详情" value="none" />
                  <el-option label="长正文/图文" value="article" />
                  <el-option label="PDF 文档" value="pdf" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="打开方式">
                <el-select v-model="cardConfig.detailOpenMode" class="w-full" :disabled="cardConfig.detailSource !== 'pdf'">
                  <el-option label="弹窗预览" value="modal" />
                  <el-option label="新标签页" value="blank" />
                  <el-option label="浏览器小窗口" value="browserWindow" />
                  <el-option label="当前页" value="current" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8"><el-form-item label="按钮文字"><el-input v-model="cardConfig.detailButtonText" placeholder="查看详情" /></el-form-item></el-col>
            <el-col v-if="cardConfig.detailSource === 'article'" :xs="24" :md="8">
              <el-form-item label="正文格式">
                <el-select v-model="cardConfig.detailContentMode" class="w-full">
                  <el-option label="Markdown 图文" value="markdown" />
                  <el-option label="HTML 图文" value="html" />
                  <el-option label="普通文本" value="text" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col v-if="cardConfig.detailSource === 'article'" :xs="24" :md="16"><el-form-item label="详情标题"><el-input v-model="cardConfig.detailTitle" placeholder="默认使用卡片标题" /></el-form-item></el-col>
            <el-col v-if="cardConfig.detailSource === 'article'" :xs="24">
              <el-form-item label="详情正文">
                <el-input
                  v-model="cardConfig.detailContent"
                  type="textarea"
                  :rows="10"
                  maxlength="50000"
                  show-word-limit
                  placeholder="支持 Markdown 链接、图片、加粗、列表；也可切换为 HTML 图文片段。"
                />
              </el-form-item>
            </el-col>
            <el-col v-if="cardConfig.detailSource === 'pdf'" :xs="24" :md="12">
              <el-form-item label="PDF 文件">
                <file-upload v-model="cardConfig.detailPdfOssId" :limit="1" :file-size="50" :file-type="['pdf']" />
              </el-form-item>
            </el-col>
            <el-col v-if="cardConfig.detailSource === 'pdf'" :xs="24" :md="12">
              <el-form-item label="PDF 标题"><el-input v-model="cardConfig.detailPdfTitle" placeholder="默认使用卡片标题或文件名" /></el-form-item>
              <el-form-item label="登录查看"><el-switch v-model="cardConfig.detailRequireLogin" /></el-form-item>
            </el-col>
          </el-row>
        </template>

        <template v-if="cardForm.cardType === 'hero'">
          <el-divider content-position="left">Hero 首屏配置</el-divider>
          <el-row :gutter="16">
            <el-col :xs="24" :md="12"><el-form-item label="英文标识"><el-input v-model="cardConfig.kicker" placeholder="DYZ · ART EXHIBITION" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="徽标标签"><el-input v-model="cardConfig.dynamicLabel" placeholder="ART" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="粒子文字"><el-input v-model="cardConfig.dynamicWord" placeholder="艺术展演" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="标题分行"><el-input v-model="cardConfig.sloganText" placeholder="向美而行 / 逐梦未来" /></el-form-item></el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="主视觉动画">
                <el-select v-model="cardConfig.visualAnimation" class="w-full">
                  <el-option label="轻微浮动" value="float" />
                  <el-option label="跟随鼠标倾斜" value="tilt" />
                  <el-option label="缓慢自转" value="slowRotate" />
                  <el-option label="呼吸发光" value="glow" />
                  <el-option label="静态" value="none" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8"><el-form-item label="鼠标跟随"><el-switch v-model="cardConfig.visualFollowMouse" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="跟随强度"><el-input-number v-model="cardConfig.visualTiltIntensity" class="w-full" :min="2" :max="18" :step="1" /></el-form-item></el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="背景类型">
                <el-select v-model="cardConfig.backgroundType" class="w-full">
                  <el-option label="默认浅色" value="default" />
                  <el-option label="图片背景" value="image" />
                  <el-option label="动态渐变" value="gradient" />
                  <el-option label="自定义 HTML" value="html" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="16"><el-form-item label="背景图地址"><el-input v-model="cardConfig.backgroundImage" placeholder="可填图片 URL；背景类型选图片时使用" /></el-form-item></el-col>
            <el-col :xs="24"><el-form-item label="背景 HTML"><el-input v-model="cardConfig.backgroundHtml" type="textarea" :rows="3" maxlength="6000" show-word-limit placeholder="<div class='hero-bg-extra'>...</div>" /></el-form-item></el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="粒子蓝色">
                <div class="color-control">
                  <el-color-picker v-model="particleConfig.blueColor" show-alpha color-format="rgb" />
                  <el-input v-model="particleConfig.blueColor" />
                </div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="粒子强调色">
                <div class="color-control">
                  <el-color-picker v-model="particleConfig.accentColor" show-alpha color-format="rgb" />
                  <el-input v-model="particleConfig.accentColor" />
                </div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12"><el-form-item label="蓝色透明度"><el-slider v-model="particleBlueAlpha" :min="0" :max="1" :step="0.01" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="强调透明度"><el-slider v-model="particleAccentAlpha" :min="0" :max="1" :step="0.01" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="粒子密度"><el-input-number v-model="particleConfig.density" class="w-full" :min="0.4" :max="2" :step="0.1" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="文字大小"><el-input-number v-model="particleConfig.fontScale" class="w-full" :min="0.6" :max="1.4" :step="0.05" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="横向位置"><el-input-number v-model="particleConfig.xRatio" class="w-full" :min="0.2" :max="0.9" :step="0.01" /></el-form-item></el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="鼠标互动">
                <el-select v-model="particleConfig.interactionMode" class="w-full">
                  <el-option label="触碰扩散" value="repel" />
                  <el-option label="触碰聚合" value="attract" />
                  <el-option label="轻微扰动" value="drift" />
                  <el-option label="关闭互动" value="none" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8"><el-form-item label="互动半径"><el-input-number v-model="particleConfig.interactionRadius" class="w-full" :min="40" :max="220" :step="10" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="互动力度"><el-input-number v-model="particleConfig.interactionForce" class="w-full" :min="0.2" :max="9" :step="0.2" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="点击聚合时长"><el-input-number v-model="particleConfig.clickRegroupMs" class="w-full" :min="0" :max="3000" :step="100" /></el-form-item></el-col>
          </el-row>
          <div class="sub-head mini-head">
            <h3>Hero 按钮</h3>
            <el-button icon="Plus" @click="addHeroButton">新增按钮</el-button>
          </div>
          <el-table border :data="heroButtons" row-key="sortOrder">
            <el-table-column label="显示" width="80" align="center"><template #default="{ row }"><el-switch v-model="row.enabled" /></template></el-table-column>
            <el-table-column label="文字" min-width="130"><template #default="{ row }"><el-input v-model="row.text" /></template></el-table-column>
            <el-table-column label="链接" min-width="160"><template #default="{ row }"><el-input v-model="row.link" /></template></el-table-column>
            <el-table-column label="样式" width="130">
              <template #default="{ row }">
                <el-select v-model="row.style" class="w-full">
                  <el-option label="主按钮" value="primary" />
                  <el-option label="次按钮" value="secondary" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="排序" width="120"><template #default="{ row }"><el-input-number v-model="row.sortOrder" :min="0" :max="999" /></template></el-table-column>
            <el-table-column label="操作" width="80" align="center"><template #default="{ $index }"><el-button link type="danger" icon="Delete" @click="removeHeroButton($index)">删除</el-button></template></el-table-column>
          </el-table>

          <el-collapse class="css-config-collapse mt-4">
            <el-collapse-item title="Hero 元素级自定义 CSS" name="hero-element-css">
              <el-alert class="mb-3" type="info" :closable="false" show-icon title="这些 CSS 会分别作用到 Hero 的固定元素范围，适合标题光影、按钮磨砂、粒子透明度等精细样式。" />
              <el-row :gutter="16">
                <el-col :xs="24" :md="12"><el-form-item label="整体面板"><el-input v-model="heroElementCss.panelCss" type="textarea" :rows="4" maxlength="3000" show-word-limit placeholder="background: rgba(255,255,255,.32);" /></el-form-item></el-col>
                <el-col :xs="24" :md="12"><el-form-item label="英文标识"><el-input v-model="heroElementCss.kickerCss" type="textarea" :rows="4" maxlength="3000" show-word-limit placeholder="color: #006fb8;" /></el-form-item></el-col>
                <el-col :xs="24" :md="12"><el-form-item label="主标题"><el-input v-model="heroElementCss.titleCss" type="textarea" :rows="4" maxlength="3000" show-word-limit :placeholder="heroTitleCssPlaceholder" /></el-form-item></el-col>
                <el-col :xs="24" :md="12"><el-form-item label="副标题"><el-input v-model="heroElementCss.subtitleCss" type="textarea" :rows="4" maxlength="3000" show-word-limit placeholder="letter-spacing: .2em;" /></el-form-item></el-col>
                <el-col :xs="24" :md="12"><el-form-item label="按钮组"><el-input v-model="heroElementCss.actionsCss" type="textarea" :rows="4" maxlength="3000" show-word-limit placeholder="gap: 16px;" /></el-form-item></el-col>
                <el-col :xs="24" :md="12"><el-form-item label="主按钮"><el-input v-model="heroElementCss.primaryButtonCss" type="textarea" :rows="4" maxlength="3000" show-word-limit placeholder="box-shadow: 0 18px 36px rgba(0,128,200,.28);" /></el-form-item></el-col>
                <el-col :xs="24" :md="12"><el-form-item label="次按钮"><el-input v-model="heroElementCss.secondaryButtonCss" type="textarea" :rows="4" maxlength="3000" show-word-limit placeholder="backdrop-filter: blur(12px);" /></el-form-item></el-col>
                <el-col :xs="24" :md="12"><el-form-item label="右侧主视觉"><el-input v-model="heroElementCss.visualCss" type="textarea" :rows="4" maxlength="3000" show-word-limit placeholder="filter: drop-shadow(0 20px 36px rgba(0,94,168,.2));" /></el-form-item></el-col>
                <el-col :xs="24"><el-form-item label="粒子层"><el-input v-model="heroElementCss.particleCss" type="textarea" :rows="4" maxlength="3000" show-word-limit placeholder="opacity: .9; mix-blend-mode: multiply;" /></el-form-item></el-col>
              </el-row>
            </el-collapse-item>
          </el-collapse>
        </template>

        <template v-if="cardForm.cardType !== 'hero'">
          <el-divider content-position="left">布局与内容</el-divider>
          <el-row :gutter="16">
            <el-col :xs="24" :md="8">
              <el-form-item label="宽度">
                <el-select v-model="cardConfig.widthMode" class="w-full">
                  <el-option label="普通宽度" value="normal" />
                  <el-option label="半宽卡片" value="half" />
                  <el-option label="全宽显示" value="full" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="正文格式">
                <el-select v-model="cardConfig.contentMode" class="w-full">
                  <el-option label="普通文本" value="text" />
                  <el-option label="Markdown" value="markdown" />
                  <el-option label="HTML" value="html" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8"><el-form-item label="手机隐藏"><el-switch v-model="cardConfig.mobileHidden" /></el-form-item></el-col>
          </el-row>
          <el-divider content-position="left">通用显示与外观</el-divider>
          <el-row :gutter="16">
            <el-col :xs="24" :md="6"><el-form-item label="显示标题"><el-switch v-model="cardConfig.showCardTitle" /></el-form-item></el-col>
            <el-col :xs="24" :md="6"><el-form-item label="显示正文"><el-switch v-model="cardConfig.showCardContent" /></el-form-item></el-col>
            <el-col :xs="24" :md="6"><el-form-item label="显示图片"><el-switch v-model="cardConfig.showCardImage" /></el-form-item></el-col>
            <el-col :xs="24" :md="6"><el-form-item label="显示按钮"><el-switch v-model="cardConfig.showCardAction" /></el-form-item></el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="外观模式">
                <el-select v-model="cardConfig.cardChrome" class="w-full">
                  <el-option label="默认卡片" value="default" />
                  <el-option label="纯内容" value="plain" />
                  <el-option label="透明无框" value="transparent" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="4"><el-form-item label="背景"><el-switch v-model="cardConfig.showCardBackground" /></el-form-item></el-col>
            <el-col :xs="24" :md="4"><el-form-item label="边框"><el-switch v-model="cardConfig.showCardBorder" /></el-form-item></el-col>
            <el-col :xs="24" :md="4"><el-form-item label="阴影"><el-switch v-model="cardConfig.showCardShadow" /></el-form-item></el-col>
            <el-col :xs="24" :md="4"><el-form-item label="图标"><el-switch v-model="cardConfig.showCardIcon" /></el-form-item></el-col>
            <el-col :xs="24" :md="6"><el-form-item label="圆角"><el-input-number v-model="cardConfig.cardRadius" class="w-full" :min="0" :max="80" /></el-form-item></el-col>
            <el-col :xs="24" :md="6"><el-form-item label="内边距"><el-input-number v-model="cardConfig.cardPadding" class="w-full" :min="0" :max="80" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="背景色"><el-input v-model="cardConfig.cardBgColor" placeholder="rgba(255,255,255,.9) 或 transparent" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="边框色"><el-input v-model="cardConfig.cardBorderColor" placeholder="rgba(10,142,219,.12)" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="标题色"><el-input v-model="cardConfig.cardTitleColor" placeholder="#0c2540" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="正文色"><el-input v-model="cardConfig.cardTextColor" placeholder="#3f596d" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="强调色"><el-input v-model="cardConfig.cardAccentColor" placeholder="#0878d8" /></el-form-item></el-col>
          </el-row>
        </template>

        <template v-if="cardForm.cardType === 'notice'">
          <el-divider content-position="left">通知公告列表配置</el-divider>
          <el-row :gutter="16">
            <el-col :xs="24" :md="8"><el-form-item label="展示条数"><el-input-number v-model="cardConfig.noticeLimit" class="w-full" :min="1" :max="12" /></el-form-item></el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="列表样式">
                <el-select v-model="cardConfig.noticeVariant" class="w-full">
                  <el-option label="普通列表" value="list" />
                  <el-option label="卡片列表" value="card" />
                  <el-option label="图文卡片" value="media" />
                  <el-option label="重点公告大卡" value="featured" />
                  <el-option label="长条通知" value="strip" />
                  <el-option label="长条弹出卡" value="popupStrip" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8"><el-form-item label="显示日期"><el-switch v-model="cardConfig.showDate" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="显示标签"><el-switch v-model="cardConfig.showTag" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="显示标识"><el-switch v-model="cardConfig.showNoticeKicker" /></el-form-item></el-col>
          </el-row>
        </template>

        <template v-if="cardForm.cardType === 'timeline'">
          <el-divider content-position="left">时间线卡片配置</el-divider>
          <el-row :gutter="16">
            <el-col :xs="24" :md="8"><el-form-item label="英文标识"><el-input v-model="cardConfig.timelineKicker" placeholder="EXHIBITION STAGES" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="显示标识"><el-switch v-model="cardConfig.showTimelineKicker" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="显示标题"><el-switch v-model="cardConfig.showTimelineTitle" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="时间线标题"><el-input v-model="cardConfig.timelineTitle" placeholder="默认使用卡片标题" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="显示简介"><el-switch v-model="cardConfig.showTimelineIntro" /></el-form-item></el-col>
            <el-col :xs="24"><el-form-item label="简介"><el-input v-model="cardConfig.timelineIntro" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item></el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="时间线样式">
                <el-select v-model="cardConfig.timelineVariant" class="w-full">
                  <el-option label="横向线性" value="horizontal" />
                  <el-option label="竖向线性" value="vertical" />
                  <el-option label="卡片式" value="card" />
                  <el-option label="紧凑式" value="compact" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="手机布局">
                <el-select v-model="cardConfig.timelineMobileMode" class="w-full">
                  <el-option label="竖向堆叠" value="stack" />
                  <el-option label="紧凑堆叠" value="compact" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="6"><el-form-item label="背景"><el-switch v-model="cardConfig.showTimelineBackground" /></el-form-item></el-col>
            <el-col :xs="24" :md="6"><el-form-item label="边框"><el-switch v-model="cardConfig.showTimelineBorder" /></el-form-item></el-col>
            <el-col :xs="24" :md="6"><el-form-item label="阴影"><el-switch v-model="cardConfig.showTimelineShadow" /></el-form-item></el-col>
            <el-col :xs="24" :md="6"><el-form-item label="连线"><el-switch v-model="cardConfig.showTimelineLine" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="背景色"><el-input v-model="cardConfig.timelineBgColor" placeholder="rgba(255,255,255,.95) 或 transparent" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="边框色"><el-input v-model="cardConfig.timelineBorderColor" placeholder="rgba(10,142,219,.12)" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="连线色"><el-input v-model="cardConfig.timelineLineColor" placeholder="linear-gradient(...) 或 #0a8edb" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="圆点色"><el-input v-model="cardConfig.timelineDotColor" placeholder="linear-gradient(135deg,#0a8edb,#00bfa6)" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="强调色"><el-input v-model="cardConfig.timelineAccentColor" placeholder="高亮节点圆点颜色" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="标题色"><el-input v-model="cardConfig.timelineTitleColor" placeholder="#0c2540" /></el-form-item></el-col>
            <el-col :xs="24" :md="12"><el-form-item label="正文色"><el-input v-model="cardConfig.timelineTextColor" placeholder="#3f596d" /></el-form-item></el-col>
            <el-col :xs="24">
              <el-form-item label="时间节点">
                <el-input
                  v-model="cardConfig.timelineItemsText"
                  type="textarea"
                  :rows="7"
                  show-word-limit
                  maxlength="16000"
                  placeholder="每行一项：标题 | 日期 | 说明 | 链接 | 按钮文字 | 高亮"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <template v-if="cardForm.cardType === 'spacer'">
          <el-divider content-position="left">分割留白配置</el-divider>
          <el-row :gutter="16">
            <el-col :xs="24" :md="8">
              <el-form-item label="留白类型">
                <el-select v-model="cardConfig.spacerMode" class="w-full">
                  <el-option label="纯空白" value="blank" />
                  <el-option label="分割线" value="line" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8"><el-form-item label="桌面高度"><el-input-number v-model="cardConfig.spacerHeight" class="w-full" :min="0" :max="600" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="手机高度"><el-input-number v-model="cardConfig.spacerMobileHeight" class="w-full" :min="0" :max="360" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="手机隐藏"><el-switch v-model="cardConfig.mobileHidden" /></el-form-item></el-col>
            <el-col v-if="cardConfig.spacerMode === 'line'" :xs="24" :md="8">
              <el-form-item label="线条样式">
                <el-select v-model="cardConfig.spacerLineStyle" class="w-full">
                  <el-option label="渐变线" value="gradient" />
                  <el-option label="实线" value="solid" />
                  <el-option label="虚线" value="dashed" />
                  <el-option label="点线" value="dotted" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col v-if="cardConfig.spacerMode === 'line'" :xs="24" :md="8"><el-form-item label="线条粗细"><el-input-number v-model="cardConfig.spacerLineSize" class="w-full" :min="1" :max="12" /></el-form-item></el-col>
            <el-col v-if="cardConfig.spacerMode === 'line'" :xs="24" :md="12"><el-form-item label="线条宽度"><el-input v-model="cardConfig.spacerLineWidth" placeholder="100% / 720px / 60%" /></el-form-item></el-col>
            <el-col v-if="cardConfig.spacerMode === 'line'" :xs="24" :md="12"><el-form-item label="线条颜色"><el-input v-model="cardConfig.spacerLineColor" placeholder="rgba(10,142,219,.22)" /></el-form-item></el-col>
          </el-row>
        </template>

        <template v-if="['image', 'info', 'button', 'custom'].includes(cardForm.cardType)">
          <el-divider content-position="left">页面卡片样式</el-divider>
          <el-row :gutter="16">
            <el-col :xs="24" :md="8">
              <el-form-item label="卡片模板">
                <el-select v-model="cardConfig.cardVariant" class="w-full">
                  <el-option label="图片在上" value="imageTop" />
                  <el-option label="左图右文" value="imageLeft" />
                  <el-option label="背景图卡片" value="imageCover" />
                  <el-option label="全宽横幅" value="fullBleed" />
                  <el-option label="简洁文字" value="plain" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8"><el-form-item label="全宽显示"><el-switch v-model="cardConfig.fullWidth" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="手机隐藏"><el-switch v-model="cardConfig.mobileHidden" /></el-form-item></el-col>
          </el-row>
        </template>

        <template v-if="cardForm.cardType === 'button'">
          <el-divider content-position="left">按钮入口配置</el-divider>
          <el-row :gutter="16">
            <el-col :xs="24" :md="8">
              <el-form-item label="显示方式">
                <el-select v-model="cardConfig.buttonDisplayMode" class="w-full">
                  <el-option label="卡片内按钮" value="card" />
                  <el-option label="单独按钮" value="single" />
                  <el-option label="按钮组" value="group" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="按钮样式">
                <el-select v-model="cardConfig.buttonVariant" class="w-full">
                  <el-option label="实心按钮" value="solid" />
                  <el-option label="描边按钮" value="outline" />
                  <el-option label="浅底按钮" value="ghost" />
                  <el-option label="文字按钮" value="text" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="对齐方式">
                <el-select v-model="cardConfig.buttonAlign" class="w-full">
                  <el-option label="左对齐" value="left" />
                  <el-option label="居中" value="center" />
                  <el-option label="右对齐" value="right" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8"><el-form-item label="按钮背景"><el-input v-model="cardConfig.buttonBgColor" placeholder="linear-gradient(...) 或 #0878d8" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="按钮文字"><el-input v-model="cardConfig.buttonTextColor" placeholder="#ffffff" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="按钮边框"><el-input v-model="cardConfig.buttonBorderColor" placeholder="rgba(10,142,219,.32)" /></el-form-item></el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="组布局">
                <el-select v-model="cardConfig.buttonLayout" class="w-full">
                  <el-option label="横向排列" value="row" />
                  <el-option label="自动换行" value="wrap" />
                  <el-option label="纵向排列" value="column" />
                  <el-option label="网格排列" value="grid" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="手机布局">
                <el-select v-model="cardConfig.buttonMobileLayout" class="w-full">
                  <el-option label="跟随桌面" value="same" />
                  <el-option label="纵向堆叠" value="column" />
                  <el-option label="自动换行" value="wrap" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="4"><el-form-item label="桌面间距"><el-input-number v-model="cardConfig.buttonGap" class="w-full" :min="0" :max="48" /></el-form-item></el-col>
            <el-col :xs="24" :md="4"><el-form-item label="手机间距"><el-input-number v-model="cardConfig.buttonMobileGap" class="w-full" :min="0" :max="40" /></el-form-item></el-col>
            <el-col :xs="24">
              <el-form-item label="按钮列表">
                <el-input
                  v-model="cardConfig.buttonsText"
                  type="textarea"
                  :rows="5"
                  maxlength="12000"
                  show-word-limit
                  placeholder="每行一个：按钮文字 | 链接地址 | 样式(solid/outline/ghost/text) | 打开方式(current/blank/modal/browserWindow/pageDefault) | 显示端(desktop/mobile/all)"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <template v-if="cardForm.cardType === 'html'">
          <el-divider content-position="left">自定义 HTML 模块</el-divider>
          <el-row :gutter="16">
            <el-col :xs="24" :md="8">
              <el-form-item label="布局">
                <el-select v-model="cardConfig.htmlLayout" class="w-full">
                  <el-option label="居中内容" value="contained" />
                  <el-option label="全宽内容" value="full" />
                  <el-option label="卡片容器" value="card" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24"><el-form-item label="桌面 HTML"><el-input v-model="cardConfig.htmlDesktop" type="textarea" :rows="5" maxlength="12000" show-word-limit /></el-form-item></el-col>
            <el-col :xs="24"><el-form-item label="手机 HTML"><el-input v-model="cardConfig.htmlMobile" type="textarea" :rows="5" maxlength="12000" show-word-limit /></el-form-item></el-col>
          </el-row>
        </template>

        <template v-if="['carousel', 'linkedCarousel'].includes(cardForm.cardType)">
          <el-divider content-position="left">图片轮播配置</el-divider>
          <el-row :gutter="16">
            <el-col :xs="24" :md="8">
              <el-form-item label="轮播样式">
                <el-select v-model="cardConfig.carouselVariant" class="w-full">
                  <el-option label="单图轮播" value="single" />
                  <el-option label="多图横滑" value="multi" />
                  <el-option label="图文联动" value="linked" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8"><el-form-item label="自动播放"><el-switch v-model="cardConfig.autoplay" /></el-form-item></el-col>
            <el-col :xs="24" :md="8"><el-form-item label="间隔秒"><el-input-number v-model="cardConfig.interval" class="w-full" :min="2" :max="10" /></el-form-item></el-col>
            <el-col :xs="24">
              <el-form-item label="轮播数据">
                <el-input v-model="cardConfig.carouselItemsText" type="textarea" :rows="6" show-word-limit maxlength="12000" placeholder="每行一项：标题 | 描述 | 图片URL | 链接" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <el-form-item v-if="cardForm.cardType !== 'spacer'" :label="cardImageUploadLabel">
          <image-upload v-model="cardForm.imageOssId" :limit="1" :file-size="5" :file-type="['png', 'jpg', 'jpeg', 'webp']" />
        </el-form-item>
        <el-form-item v-if="cardForm.cardType !== 'spacer'" :label="cardImageUrlLabel">
          <el-input v-model="cardForm.imageUrl" :placeholder="cardImageUrlPlaceholder" />
        </el-form-item>
        <el-row v-if="cardForm.cardType !== 'spacer'" :gutter="16">
          <el-col :xs="24" :md="12"><el-form-item label="重点高亮"><el-switch v-model="cardForm.highlight" /></el-form-item></el-col>
          <el-col :xs="24" :md="12"><el-form-item label="替换位"><el-input v-model="cardForm.slotKey" /></el-form-item></el-col>
        </el-row>

        <el-collapse class="css-config-collapse">
          <el-collapse-item title="当前卡片自定义 CSS" name="card-css">
            <el-alert class="mb-3" type="info" :closable="false" show-icon title="前台会自动作用到当前卡片范围，例如 .portal-card-home_hero；适合单卡片强调和按钮细节。" />
            <el-row :gutter="16">
              <el-col :xs="24" :md="8">
                <el-form-item label="自定义类名">
                  <el-input v-model="cardStyleConfig.customClass" maxlength="120" placeholder="如 hero-title-glow" />
                </el-form-item>
              </el-col>
              <el-col :xs="24">
                <el-form-item label="卡片 CSS">
                  <el-input v-model="cardStyleConfig.customCss" type="textarea" :rows="6" maxlength="6000" show-word-limit :placeholder="cardCssPlaceholder" />
                </el-form-item>
              </el-col>
              <el-col :xs="24">
                <el-form-item label="移动端 CSS">
                  <el-input v-model="cardStyleConfig.customMobileCss" type="textarea" :rows="4" maxlength="4000" show-word-limit :placeholder="mobileCssPlaceholder" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-collapse-item>
        </el-collapse>

        <el-collapse>
          <el-collapse-item title="高级 JSON 配置" name="advanced">
            <el-input v-model="cardForm.configJson" type="textarea" :rows="6" placeholder='{"key":"value"}' />
          </el-collapse-item>
        </el-collapse>
        <el-form-item label="备注" class="mt-4"><el-input v-model="cardForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cardDialog.visible = false">取消</el-button>
        <el-button v-hasPermi="['system:homeConfig:edit']" type="primary" :loading="cardSaving" @click="submitCardForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="SystemHomeConfig" lang="ts">
import {
  deleteHomeCard,
  deleteHomeModule,
  getHomeSiteConfig,
  listHomeCard,
  listHomeModule,
  saveHomeCard,
  saveHomeModule,
  saveHomeSiteConfig,
  sortHomeCard
} from '@/api/system/homeConfig';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const activeMainTab = ref('modules');
const activeModuleCode = ref('');
const loading = ref(false);
const saving = ref(false);
const moduleLoading = ref(false);
const moduleSaving = ref(false);
const cardLoading = ref(false);
const cardSaving = ref(false);
const cardFormRef = ref<any>();
const builderCanvasRef = ref<HTMLElement>();
const selectedBuilderKind = ref<'module' | 'card'>('module');
const selectedBuilderId = ref('');
const cardDragState = ref<any>();
const flowDraggingCardKey = ref('');

const moduleTypeOptions = [
  { label: '文章详情页', value: 'article' },
  { label: '系统页链接', value: 'system' },
  { label: '外部链接页', value: 'external' },
  { label: '活动首页固定模板', value: 'hero' },
  { label: '上报指南模板', value: 'guide' },
  { label: '展演项目模板', value: 'category' },
  { label: '时间轴模板', value: 'stage' },
  { label: '通知列表模板', value: 'notice' },
  { label: '自定义画布页', value: 'custom' }
];

const cardTypeOptions = [
  { label: 'Hero 首屏', value: 'hero' },
  { label: '指南信息卡', value: 'guide' },
  { label: '项目图标卡', value: 'category' },
  { label: '兼容旧阶段卡', value: 'stage' },
  { label: '独立时间线卡', value: 'timeline' },
  { label: '通知列表', value: 'notice' },
  { label: '普通信息卡', value: 'info' },
  { label: '图片卡片', value: 'image' },
  { label: '入口按钮卡', value: 'button' },
  { label: '自定义 HTML', value: 'html' },
  { label: '分割留白', value: 'spacer' },
  { label: '图片轮播', value: 'carousel' },
  { label: '联动轮播', value: 'linkedCarousel' },
  { label: '自定义配置卡', value: 'custom' }
];

const componentPresets = [
  {
    key: 'richText',
    icon: 'TXT',
    label: '富文本',
    desc: '标题、段落、Markdown',
    cardType: 'info',
    cardTitle: '页面说明',
    cardSubtitle: '可编辑 Markdown 或普通文本',
    cardContent: '## 页面标题\n在这里填写正文内容，可插入链接、加粗文字或分段说明。',
    config: {
      cardVariant: 'plain',
      widthMode: 'full',
      fullWidth: true,
      contentMode: 'markdown',
      builder: { w: 760, h: 180 }
    }
  },
  {
    key: 'imageText',
    icon: 'IMG',
    label: '图文卡',
    desc: '图片、说明和按钮',
    cardType: 'image',
    cardTitle: '图文介绍',
    cardSubtitle: '适合上报指南、展演项目介绍',
    cardContent: '填写图文卡片正文，支持配置图片、按钮和跳转方式。',
    linkText: '查看详情',
    linkUrl: '#',
    config: {
      cardVariant: 'imageLeft',
      widthMode: 'full',
      fullWidth: true,
      contentMode: 'text',
      builder: { w: 760, h: 220 }
    }
  },
  {
    key: 'noticeList',
    icon: 'LIST',
    label: '通知列表',
    desc: '公告列表或卡片',
    cardType: 'notice',
    cardTitle: '通知公告',
    cardSubtitle: 'NOTICE',
    cardContent: '展示通知公告，可在组件属性里切换列表样式。',
    config: {
      noticeLimit: 4,
      noticeVariant: 'card',
      showDate: true,
      showTag: true,
      widthMode: 'full',
      fullWidth: true,
      builder: { w: 760, h: 260 }
    }
  },
  {
    key: 'noticeStrip',
    icon: 'POP',
    label: '长条通知',
    desc: '横向通知、点击弹窗',
    cardType: 'notice',
    cardTitle: '重要通知',
    cardSubtitle: '2026-06-26',
    cardContent: '这里填写通知摘要。点击右侧按钮后，可在弹窗里查看完整内容。',
    linkText: '查看详情',
    linkUrl: '#',
    linkType: 'modal',
    config: {
      noticeLimit: 4,
      noticeVariant: 'popupStrip',
      showDate: true,
      showTag: true,
      widthMode: 'full',
      fullWidth: true,
      contentMode: 'text',
      modalWidth: 880,
      modalHeight: 620,
      builder: { w: 760, h: 120 }
    }
  },
  {
    key: 'buttonGroup',
    icon: 'BTN',
    label: '按钮入口',
    desc: '登录、下载、跳转',
    cardType: 'button',
    cardTitle: '快捷入口',
    cardSubtitle: '快速进入常用页面',
    cardContent: '配置按钮文字、链接和打开方式。',
    linkText: '立即查看',
    linkUrl: '#home',
    linkType: 'pageDefault',
    config: {
      cardVariant: 'plain',
      widthMode: 'normal',
      contentMode: 'text',
      buttonDisplayMode: 'group',
      buttonLayout: 'row',
      buttonMobileLayout: 'wrap',
      buttonGap: 12,
      buttonMobileGap: 10,
      buttonsText: '立即查看 | #home | solid | pageDefault | all\n上报指南 | #guide | outline | pageDefault | all',
      builder: { w: 320, h: 160 }
    }
  },
  {
    key: 'timeline',
    icon: 'TIME',
    label: '时间轴',
    desc: '一组阶段、日期、说明',
    cardType: 'timeline',
    cardTitle: '展演阶段',
    cardSubtitle: 'EXHIBITION STAGES',
    cardContent: '活动启动、作品报送、省级遴选、现场展演等关键节点。',
    config: {
      timelineKicker: 'EXHIBITION STAGES',
      timelineTitle: '展演阶段',
      timelineIntro: '活动启动、作品报送、省级遴选、现场展演等关键节点。',
      timelineVariant: 'horizontal',
      timelineMobileMode: 'stack',
      showTimelineKicker: true,
      showTimelineTitle: true,
      showTimelineIntro: true,
      timelineItemsText: '活动启动 | 2026.04-08 | 活动发布与校级组织启动 | #stage | 查看阶段 | 高亮\n作品报送 | 2026.09 | 学校完成作品报送与材料提交 | #stage | 查看阶段 |\n省级遴选 | 2026.10 | 组织省级推荐与遴选 | #stage | 查看阶段 |\n现场展演 | 2026.11-12 | 展演活动与成果展示 | #stage | 查看阶段 |',
      widthMode: 'full',
      fullWidth: true,
      contentMode: 'text',
      builder: { w: 760, h: 300 }
    }
  },
  {
    key: 'customHtml',
    icon: 'HTML',
    label: '自定义 HTML',
    desc: '高级自定义内容',
    cardType: 'html',
    cardTitle: '自定义内容',
    cardSubtitle: 'HTML',
    config: {
      htmlLayout: 'contained',
      htmlDesktop: '<section class="portal-custom-block"><h3>自定义标题</h3><p>在这里编写桌面端 HTML 内容。</p></section>',
      htmlMobile: '<section class="portal-custom-block"><h3>自定义标题</h3><p>在这里编写手机端 HTML 内容。</p></section>',
      widthMode: 'full',
      fullWidth: true,
      builder: { w: 760, h: 220 }
    }
  },
  {
    key: 'carousel',
    icon: 'SWP',
    label: '图片轮播',
    desc: '多图展示和跳转',
    cardType: 'carousel',
    cardTitle: '精彩瞬间',
    cardSubtitle: 'GALLERY',
    config: {
      carouselVariant: 'single',
      autoplay: true,
      interval: 4,
      carouselItemsText: '轮播标题 | 轮播说明 | 图片URL | #home',
      widthMode: 'full',
      fullWidth: true,
      builder: { w: 760, h: 300 }
    }
  },
  {
    key: 'spacer',
    icon: 'GAP',
    label: '分割留白',
    desc: '纯空白占位',
    cardType: 'spacer',
    cardTitle: '分割留白',
    cardSubtitle: '',
    cardContent: '',
    config: {
      spacerHeight: 56,
      spacerMobileHeight: 32,
      spacerLine: false,
      widthMode: 'full',
      fullWidth: true,
      builder: { w: 760, h: 90 }
    }
  }
];

const siteForm = reactive<any>({
  configCode: 'portal_home',
  siteName: '河南省第八届大学生艺术展演',
  logoOssId: undefined,
  logoUrl: 'assets/logo-icon.png',
  homeLink: '#home',
  noticeLimit: 6,
  footerTitle: '河南省第八届大学生艺术展演',
  footerText: 'Copyright © 2026 ',
  customCss: ''
});

const siteStyleForm = reactive<any>({
  globalMobileCss: '',
  headerCss: '',
  headerMobileCss: '',
  footerCss: '',
  footerMobileCss: '',
  homeLayoutMode: 'flow',
  homeCanvasHeight: 1280,
  headerPlacement: 'independent',
  footerPlacement: 'independent',
  headerDesktop: {},
  headerMobile: {},
  footerDesktop: {},
  footerMobile: {}
});

const modules = ref<any[]>([]);
const headerButtons = ref<any[]>([]);
const loginStateForm = reactive<any>({
  adminText: '管理账户已登录',
  schoolText: '学校账户已登录',
  expertText: '专家账户已登录',
  defaultText: '已登录'
});
const cards = ref<any[]>([]);
const cardTotal = ref(0);
const cardQuery = reactive<any>({
  pageNum: 1,
  pageSize: 10,
  sectionKey: undefined,
  cardTitle: ''
});
const cardDialog = reactive({ visible: false, title: '' });
const cardForm = ref<any>({});
const cardConfig = reactive<any>({});
const cardStyleConfig = reactive<any>({
  customClass: '',
  customCss: '',
  customMobileCss: ''
});
const particleConfig = reactive<any>({});
const heroElementCss = reactive<any>({
  panelCss: '',
  kickerCss: '',
  titleCss: '',
  subtitleCss: '',
  actionsCss: '',
  primaryButtonCss: '',
  secondaryButtonCss: '',
  visualCss: '',
  particleCss: ''
});
const heroButtons = ref<any[]>([]);

type RgbaColor = {
  r: number;
  g: number;
  b: number;
  a: number;
};

const DEFAULT_BLUE_PARTICLE: RgbaColor = { r: 0, g: 143, b: 216, a: 0.56 };
const DEFAULT_ACCENT_PARTICLE: RgbaColor = { r: 222, g: 114, b: 95, a: 0.32 };

const clampRgb = (value: number) => Math.max(0, Math.min(255, Math.round(Number(value) || 0)));
const clampAlpha = (value: number) => Math.max(0, Math.min(1, Number(value) || 0));

const toRgbaString = (color: RgbaColor) => `rgba(${clampRgb(color.r)}, ${clampRgb(color.g)}, ${clampRgb(color.b)}, ${Number(clampAlpha(color.a).toFixed(2))})`;

const parseParticleColor = (value: any, fallback: RgbaColor): RgbaColor => {
  const color = String(value || '').trim();
  const rgbMatch = color.match(/^rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)(?:\s*,\s*([\d.]+))?\s*\)$/i);
  if (rgbMatch) {
    return {
      r: clampRgb(Number(rgbMatch[1])),
      g: clampRgb(Number(rgbMatch[2])),
      b: clampRgb(Number(rgbMatch[3])),
      a: clampAlpha(rgbMatch[4] === undefined ? fallback.a : Number(rgbMatch[4]))
    };
  }
  if (/^#([0-9a-f]{3}|[0-9a-f]{6})$/i.test(color)) {
    let hex = color.slice(1);
    if (hex.length === 3) {
      hex = hex
        .split('')
        .map((item) => item + item)
        .join('');
    }
    const parsed = Number.parseInt(hex, 16);
    return {
      r: (parsed >> 16) & 255,
      g: (parsed >> 8) & 255,
      b: parsed & 255,
      a: fallback.a
    };
  }
  return fallback;
};

const setParticleAlpha = (key: 'blueColor' | 'accentColor', value: number, fallback: RgbaColor) => {
  particleConfig[key] = toRgbaString({ ...parseParticleColor(particleConfig[key], fallback), a: value });
};

const normalizedParticleColor = (key: 'blueColor' | 'accentColor', fallback: RgbaColor) => toRgbaString(parseParticleColor(particleConfig[key], fallback));

const particleBlueAlpha = computed({
  get: () => parseParticleColor(particleConfig.blueColor, DEFAULT_BLUE_PARTICLE).a,
  set: (value: number) => setParticleAlpha('blueColor', value, DEFAULT_BLUE_PARTICLE)
});

const particleAccentAlpha = computed({
  get: () => parseParticleColor(particleConfig.accentColor, DEFAULT_ACCENT_PARTICLE).a,
  set: (value: number) => setParticleAlpha('accentColor', value, DEFAULT_ACCENT_PARTICLE)
});

const cardImageUploadLabel = computed(() => (cardForm.value?.cardType === 'hero' ? '浮动图片' : '卡片图片'));
const cardImageUrlLabel = computed(() => (cardForm.value?.cardType === 'hero' ? '浮动图地址' : '图片地址'));
const cardImageUrlPlaceholder = computed(() => (cardForm.value?.cardType === 'hero' ? '用于首屏右侧粒子上方浮动图；上传图片优先使用 OSS' : '可填外链；上传图片优先使用 OSS'));

const isCardTitleRequired = (type?: string) => !['button', 'html', 'spacer'].includes(String(type || ''));

const cardRules = {
  sectionKey: [{ required: true, message: '请选择所属模块', trigger: 'change' }],
  cardType: [{ required: true, message: '请选择组件类型', trigger: 'change' }],
  cardTitle: [
    {
      validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
        if (isCardTitleRequired(cardForm.value?.cardType) && !String(value || '').trim()) {
          callback(new Error('请输入卡片标题'));
          return;
        }
        callback();
      },
      trigger: 'blur'
    }
  ],
  sortOrder: [{ required: true, message: '请输入排序', trigger: 'change' }]
};

const activeModule = computed(() => modules.value.find((item) => item.moduleCode === activeModuleCode.value));
const moduleOptions = computed(() => modules.value.map((item) => ({ label: item.moduleTitle || item.moduleCode, value: item.moduleCode })));
const systemPageOptions = [
  { label: '系统页 / 登录页', value: '/login' },
  { label: '系统页 / 注册页', value: '/register' },
  { label: '系统页 / 工作台', value: '/index' }
];
const pageTargetOptions = computed(() => [
  ...modules.value.map((item) => ({
    label: `${item.moduleTitle || item.moduleCode} / ${moduleTypeLabel(item.moduleType)}`,
    value: item.pageUrl || `#${item.anchor || item.moduleCode}`
  })),
  ...systemPageOptions
]);
const builderModules = computed(() => [...modules.value].sort((a, b) => Number(a.sortOrder || 0) - Number(b.sortOrder || 0)));
const selectedBuilderModule = computed(() => modules.value.find((item) => item.moduleCode === selectedBuilderId.value) || activeModule.value);
const selectedModuleBuilder = computed(() => selectedBuilderModule.value?.builder || defaultModuleBuilder());
const selectedBuilderCard = computed(() => cards.value.find((item) => cardKey(item) === selectedBuilderId.value));
const selectedCardBuilder = computed(() => selectedBuilderCard.value?.builder || defaultCardBuilder());
const selectedPageIsFixedHome = computed(() => isHomeModule(activeModule.value));
const builderCanvasCards = computed(() => (selectedPageIsFixedHome.value ? cards.value.filter((item) => !isHeroCard(item)) : cards.value));
const activePageCardLayout = computed(() => String(selectedModuleBuilder.value?.cardLayout || 'flow'));
const activePageUsesFlowLayout = computed(() => selectedPageIsFixedHome.value || activePageCardLayout.value !== 'free');
const pageCanvasStyle = computed(() => ({
  height: `${selectedPageIsFixedHome.value ? 420 : Number(selectedModuleBuilder.value?.h || 720)}px`
}));

const cardKey = (card: any) => String(card?.id || card?.cardCode || card?.cardTitle || '');
const isHomeModule = (module?: any) => String(module?.moduleCode || '') === 'home' || String(module?.anchor || '') === 'home';
const isHeroCard = (card?: any) => {
  const type = String(card?.cardType || card?.templateCode || '').toLowerCase();
  const code = String(card?.cardCode || card?.code || card?.slotKey || '').toLowerCase();
  return type === 'hero' || code === 'home_hero' || code === 'hero';
};
const isProtectedHomeHeroCard = (card?: any) => isHomeModule(activeModule.value) && isHeroCard(card);

const builderCardStyle = (card: any) => {
  if (selectedPageIsFixedHome.value || activePageCardLayout.value !== 'free') {
    return {};
  }
  const builder = card.builder || defaultCardBuilder();
  return {
    left: `${builder.x}px`,
    top: `${builder.y}px`,
    width: `${builder.w}px`,
    height: `${builder.h}px`,
    zIndex: builder.z
  };
};

const defaultHeaderButtons = () => [
  { text: '登录入口', link: '/login', style: 'ghost', enabled: true, showDesktop: true, showMobile: true, sortOrder: 10 },
  { text: '注册', link: '/register', style: 'solid', enabled: true, showDesktop: true, showMobile: true, sortOrder: 20 }
];

const defaultLoginState = () => ({
  adminText: '管理账户已登录',
  schoolText: '学校账户已登录',
  expertText: '专家账户已登录',
  defaultText: '已登录'
});

const normalizeHeaderButton = (item: any, index = 0) => ({
  text: '',
  link: '#',
  style: 'ghost',
  enabled: true,
  showDesktop: true,
  showMobile: true,
  sortOrder: (index + 1) * 10,
  ...(item || {})
});

const globalCssPlaceholder = `:root {
  --portal-accent: #0a8edb;
}`;
const headerCssPlaceholder = `.header-actions .solid {
  box-shadow: 0 14px 30px rgba(0, 94, 168, .24);
}`;
const footerCssPlaceholder = `.footer-shell {
  justify-content: flex-start;
}
.footer-text {
  text-align: left;
}`;
const moduleCssPlaceholder = `.section-title {
  color: #123456;
}`;
const cardCssPlaceholder = `box-shadow: 0 18px 36px rgba(0, 94, 168, .12);`;
const mobileCssPlaceholder = `padding-left: 24px;
padding-right: 24px;`;
const heroTitleCssPlaceholder = `background: linear-gradient(90deg, #0878d8, #16b6d9, #74bf54, #f1b33a, #e66657);
-webkit-background-clip: text;
color: transparent;`;

const defaultParticle = () => ({
  blueColor: toRgbaString(DEFAULT_BLUE_PARTICLE),
  accentColor: toRgbaString(DEFAULT_ACCENT_PARTICLE),
  floatBlueColor: 'rgba(0,118,188,',
  floatGreenColor: 'rgba(34,185,154,',
  density: 1,
  fontScale: 1,
  xRatio: 0.72,
  yRatio: 0.48,
  interactionMode: 'repel',
  interactionRadius: 100,
  interactionForce: 4.8,
  clickRegroupMs: 900
});

const defaultHeroButtons = () => [
  { text: '登录入口', link: '/login', style: 'primary', enabled: true, sortOrder: 10 },
  { text: '上报指南', link: '#guide', style: 'secondary', enabled: true, sortOrder: 20 }
];

const parseJson = (text: string, fallback: any) => {
  try {
    const parsed = JSON.parse(text || '');
    return parsed && typeof parsed === 'object' ? parsed : fallback;
  } catch {
    return fallback;
  }
};

const prettyJson = (value: any) => JSON.stringify(value || {}, null, 2);

const safeArray = (value: any, fallback: any[]) => (Array.isArray(value) ? value : fallback);
const safeObject = (value: any, fallback: any) => (value && typeof value === 'object' && !Array.isArray(value) ? value : fallback);
const trimStyleObject = (value: any) => {
  const result: any = {};
  Object.entries(value || {}).forEach(([key, item]) => {
    if (typeof item === 'string') {
      const text = item.trim();
      if (text) result[key] = text;
    } else if (item !== undefined && item !== null && item !== '') {
      result[key] = item;
    }
  });
  return result;
};

const resetReactive = (target: any, source: any) => {
  Object.keys(target).forEach((key) => delete target[key]);
  Object.assign(target, source || {});
};

const parseCarouselItems = (text: string) =>
  String(text || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [title = '', desc = '', image = '', link = ''] = line.split('|').map((item) => item.trim());
      return { title, desc, image, link };
    });

const carouselItemsToText = (items: any[]) =>
  safeArray(items, [])
    .map((item) => [item.title, item.desc || item.content, item.image || item.imageUrl, item.link].map((value) => String(value || '').trim()).join(' | '))
    .join('\n');

const parseTimelineItems = (text: string) =>
  String(text || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [title = '', date = '', desc = '', link = '', linkText = '', highlight = ''] = line.split('|').map((item) => item.trim());
      return {
        title,
        date,
        desc,
        link,
        linkText,
        highlight: /^(1|true|yes|y|高亮|重点)$/i.test(highlight)
      };
    });

const timelineItemsToText = (items: any[]) =>
  safeArray(items, [])
    .map((item) =>
      [item.title, item.date, item.desc || item.content, item.link, item.linkText, item.highlight ? '高亮' : ''].map((value) => String(value || '').trim()).join(' | ')
    )
    .join('\n');

const parseButtonItems = (text: string) =>
  String(text || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line, index) => {
      const [text = '', link = '', style = '', linkType = '', visibility = ''] = line.split('|').map((item) => item.trim());
      const visible = visibility.toLowerCase();
      return {
        text,
        link,
        style: style || 'solid',
        linkType: linkType || 'pageDefault',
        enabled: true,
        showDesktop: !['mobile', '手机'].includes(visible),
        showMobile: !['desktop', 'pc', '电脑'].includes(visible),
        sortOrder: (index + 1) * 10
      };
    })
    .filter((item) => item.text && item.link);

const buttonItemsToText = (items: any[]) =>
  safeArray(items, [])
    .map((item) => {
      const visibility = item.showDesktop === false ? 'mobile' : item.showMobile === false ? 'desktop' : 'all';
      return [item.text, item.link, item.style, item.linkType, visibility].map((value) => String(value || '').trim()).join(' | ');
    })
    .join('\n');

const cardTypeLabel = (value?: string) => cardTypeOptions.find((item) => item.value === value)?.label || value || '-';

const logLoadFailure = (label: string, reason: unknown) => {
  console.warn(`[home-config] ${label} load failed`, reason);
};

const defaultSiteStyle = () => ({
  globalMobileCss: '',
  headerCss: '',
  headerMobileCss: '',
  footerCss: '',
  footerMobileCss: '',
  homeLayoutMode: 'flow',
  homeCanvasHeight: 1280,
  headerPlacement: 'independent',
  footerPlacement: 'independent',
  headerDesktop: {
    showHeader: true,
    showLogo: true,
    showBrandName: true,
    showNav: true,
    showLoginButtons: true,
    showCustomButtons: true,
    customHtml: ''
  },
  headerMobile: {
    hideHeader: false,
    showLogo: true,
    showBrandName: true,
    showNavMenu: true,
    showLoginButtons: false,
    showCustomButtons: false,
    customHtml: ''
  },
  footerDesktop: {
    showFooter: true,
    showLogo: true,
    showTitle: true,
    showText: true,
    showContact: true,
    showIcp: true,
    customHtml: ''
  },
  footerMobile: {
    hideFooter: false,
    showLogo: false,
    showTitle: true,
    showText: true,
    showContact: false,
    showIcp: true,
    customHtml: ''
  }
});

const defaultItemStyle = () => ({
  customClass: '',
  customCss: '',
  customMobileCss: ''
});

const defaultHeroElementCss = () => ({
  panelCss: '',
  kickerCss: '',
  titleCss: '',
  subtitleCss: '',
  actionsCss: '',
  primaryButtonCss: '',
  secondaryButtonCss: '',
  visualCss: '',
  particleCss: ''
});

const defaultHeroAdvanced = () => ({
  visualAnimation: 'float',
  visualFollowMouse: true,
  visualTiltIntensity: 8,
  backgroundType: 'default',
  backgroundImage: '',
  backgroundHtml: ''
});

const defaultCardAdvanced = () => ({
  cardVariant: 'imageTop',
  fullWidth: false,
  widthMode: 'normal',
  mobileHidden: false,
  contentMode: 'text',
  showCardTitle: true,
  showCardContent: true,
  showCardImage: true,
  showCardIcon: true,
  showCardAction: true,
  cardChrome: 'default',
  showCardBackground: true,
  showCardBorder: true,
  showCardShadow: true,
  cardBgColor: '',
  cardBorderColor: '',
  cardTitleColor: '',
  cardTextColor: '',
  cardAccentColor: '',
  cardRadius: 18,
  cardPadding: 24,
  modalTitle: '',
  modalWidth: 880,
  modalHeight: 620,
  detailSource: 'none',
  detailOpenMode: 'modal',
  detailButtonText: '查看详情',
  detailTitle: '',
  detailContentMode: 'markdown',
  detailContent: '',
  detailPdfOssId: '',
  detailPdfTitle: '',
  detailRequireLogin: false,
  noticeVariant: 'list',
  showNoticeKicker: true,
  htmlLayout: 'contained',
  htmlDesktop: '',
  htmlMobile: '',
  carouselVariant: 'single',
  autoplay: true,
  interval: 4,
  carouselItemsText: '',
  timelineKicker: 'TIMELINE',
  timelineTitle: '',
  timelineIntro: '',
  timelineVariant: 'horizontal',
  timelineMobileMode: 'stack',
  showTimelineKicker: true,
  showTimelineTitle: true,
  showTimelineIntro: true,
  showTimelineBackground: true,
  showTimelineBorder: true,
  showTimelineShadow: true,
  showTimelineLine: true,
  timelineBgColor: '',
  timelineBorderColor: '',
  timelineLineColor: '',
  timelineDotColor: '',
  timelineAccentColor: '',
  timelineTitleColor: '',
  timelineTextColor: '',
  timelineItemsText: '',
  spacerHeight: 56,
  spacerMobileHeight: 32,
  spacerMode: 'blank',
  spacerLine: false,
  spacerLineStyle: 'gradient',
  spacerLineColor: '',
  spacerLineWidth: '100%',
  spacerLineSize: 1,
  buttonDisplayMode: 'card',
  buttonVariant: 'solid',
  buttonAlign: 'left',
  buttonLayout: 'row',
  buttonMobileLayout: 'same',
  buttonGap: 12,
  buttonMobileGap: 10,
  buttonBgColor: '',
  buttonTextColor: '',
  buttonBorderColor: '',
  buttonsText: ''
});

const defaultArticleConfig = () => ({
  contentMode: 'markdown',
  content: '',
  showBackButton: true,
  showHomeButton: true,
  backText: '返回上页',
  homeText: '回到首页',
  buttonsText: ''
});

const isNoticeModuleLike = (module?: any) => String(module?.moduleType || '') === 'notice' || String(module?.moduleCode || '') === 'notice';

const defaultPageHeaderConfig = (module?: any) => ({
  showPageHeader: true,
  showPageEyebrow: true,
  showPageTitle: true,
  showPageIntro: true,
  showPageMore: isNoticeModuleLike(module),
  hidePageHeaderMobile: false,
  pageHeaderAlign: 'left',
  pageHeaderSize: 'normal',
  pageMoreOpenMode: 'pageDefault',
  pageHeaderCss: '',
  pageEyebrowCss: '',
  pageTitleCss: '',
  pageIntroCss: '',
  pageMoreCss: ''
});

const pageHeaderFromConfig = (module: any, config: any) => {
  const defaults = defaultPageHeaderConfig(module);
  return {
    showPageHeader: config.showPageHeader !== false,
    showPageEyebrow: config.showPageEyebrow ?? config.showNoticeEyebrow ?? defaults.showPageEyebrow,
    showPageTitle: config.showPageTitle ?? config.showNoticeTitle ?? defaults.showPageTitle,
    showPageIntro: config.showPageIntro ?? defaults.showPageIntro,
    showPageMore: config.showPageMore ?? config.showMoreLink ?? defaults.showPageMore,
    hidePageHeaderMobile: !!config.hidePageHeaderMobile,
    pageHeaderAlign: config.pageHeaderAlign || config.noticeTitleAlign || defaults.pageHeaderAlign,
    pageHeaderSize: config.pageHeaderSize || defaults.pageHeaderSize,
    pageMoreOpenMode: config.pageMoreOpenMode || defaults.pageMoreOpenMode,
    pageHeaderCss: config.pageHeaderCss || '',
    pageEyebrowCss: config.pageEyebrowCss || config.noticeEyebrowCss || '',
    pageTitleCss: config.pageTitleCss || config.noticeTitleCss || '',
    pageIntroCss: config.pageIntroCss || '',
    pageMoreCss: config.pageMoreCss || config.noticeMoreCss || ''
  };
};

const styleFromConfig = (config: any) => ({
  customClass: config.customClass || '',
  customCss: config.customCss || '',
  customMobileCss: config.customMobileCss || config.mobileCss || ''
});

const mergeStyleToConfig = (config: any, style: any) => {
  const next = { ...(config || {}) };
  const normalized = {
    customClass: style?.customClass || '',
    customCss: style?.customCss || '',
    customMobileCss: style?.customMobileCss || ''
  };
  Object.entries(normalized).forEach(([key, value]) => {
    const text = String(value || '').trim();
    if (text) next[key] = text;
    else delete next[key];
  });
  delete next.mobileCss;
  return next;
};

const clampBuilderNumber = (value: any, min: number, max: number, fallback: number) => {
  const num = Number(value);
  if (!Number.isFinite(num)) return fallback;
  return Math.max(min, Math.min(max, Math.round(num)));
};

const defaultModuleBuilder = (index = 0, type = 'custom') => {
  const fullHeight = type === 'hero' ? 560 : type === 'stage' ? 420 : 720;
  return {
    x: 40,
    y: 40 + index * 460,
    w: 1120,
    h: fullHeight,
    z: index + 1,
    cardLayout: type === 'custom' ? 'free' : 'flow'
  };
};

const defaultCardBuilder = (index = 0) => ({
  x: 24 + (index % 2) * 220,
  y: 24 + Math.floor(index / 2) * 150,
  w: 200,
  h: 120,
  z: index + 1
});

const normalizeBuilder = (source: any, fallback: any) => ({
  ...fallback,
  ...(source || {}),
  x: clampBuilderNumber(source?.x, 0, 2000, fallback.x),
  y: clampBuilderNumber(source?.y, 0, 4000, fallback.y),
  w: clampBuilderNumber(source?.w, 80, 1800, fallback.w),
  h: clampBuilderNumber(source?.h, 60, 1600, fallback.h),
  z: clampBuilderNumber(source?.z, 0, 99, fallback.z)
});

const moduleTypeLabel = (value?: string) => moduleTypeOptions.find((item) => item.value === value)?.label || value || '-';

const normalizeModule = (item: any, index = 0) => {
  const config = parseJson(item?.configJson, {});
  const type = item?.moduleType || 'custom';
  const pageHeader = pageHeaderFromConfig(item, config);
  const normalized = {
    moduleType: 'custom',
    anchor: item?.moduleCode,
    navEnabled: true,
    enabled: true,
    sortOrder: 0,
    layoutType: 'default',
    templateCode: item?.moduleType || 'custom',
    configJson: '{}',
    ...item,
    style: styleFromConfig(config),
    navTitle: config.navTitle || item?.moduleTitle || '',
    navSortOrder: Number(config.navSortOrder ?? item?.sortOrder ?? 0),
    pageUrl: config.pageUrl || `#${item?.anchor || item?.moduleCode || ''}`,
    defaultOpenMode: config.defaultOpenMode || 'current',
    popupWidth: Number(config.popupWidth || 900),
    popupHeight: Number(config.popupHeight || 700),
    ...pageHeader,
    showMoreLink: config.showMoreLink !== false,
    showNoticeEyebrow: config.showNoticeEyebrow !== false,
    showNoticeTitle: config.showNoticeTitle !== false,
    noticeTitleAlign: config.noticeTitleAlign || 'left',
    noticeEyebrowCss: config.noticeEyebrowCss || '',
    noticeTitleCss: config.noticeTitleCss || '',
    noticeMoreCss: config.noticeMoreCss || '',
    article: {
      ...defaultArticleConfig(),
      ...safeObject(config.article, {})
    },
    builder: normalizeBuilder(config.builder, defaultModuleBuilder(index, type))
  };
  if (String(normalized.moduleCode || '') === 'home') {
    normalized.moduleType = 'hero';
    normalized.anchor = 'home';
    normalized.enabled = true;
    normalized.templateCode = 'hero';
    normalized.builder.cardLayout = 'flow';
  }
  return normalized;
};

const buildModulePayload = (module: any) => {
  const config = mergeStyleToConfig(parseJson(module.configJson, {}), module.style || defaultItemStyle());
  config.builder = normalizeBuilder(module.builder, defaultModuleBuilder(0, module.moduleType));
  config.navTitle = module.navTitle || module.moduleTitle || '';
  config.navSortOrder = Number(module.navSortOrder ?? module.sortOrder ?? 0);
  config.pageUrl = module.pageUrl || `#${module.anchor || module.moduleCode}`;
  config.defaultOpenMode = module.defaultOpenMode || 'current';
  config.popupWidth = Number(module.popupWidth || 900);
  config.popupHeight = Number(module.popupHeight || 700);
  config.showPageHeader = module.showPageHeader !== false;
  config.showPageEyebrow = module.showPageEyebrow !== false;
  config.showPageTitle = module.showPageTitle !== false;
  config.showPageIntro = module.showPageIntro !== false;
  config.showPageMore = module.showPageMore === true;
  config.hidePageHeaderMobile = !!module.hidePageHeaderMobile;
  config.pageHeaderAlign = module.pageHeaderAlign || 'left';
  config.pageHeaderSize = module.pageHeaderSize || 'normal';
  config.pageMoreOpenMode = module.pageMoreOpenMode || 'pageDefault';
  config.pageHeaderCss = module.pageHeaderCss || '';
  config.pageEyebrowCss = module.pageEyebrowCss || '';
  config.pageTitleCss = module.pageTitleCss || '';
  config.pageIntroCss = module.pageIntroCss || '';
  config.pageMoreCss = module.pageMoreCss || '';
  config.showMoreLink = config.showPageMore;
  config.showNoticeEyebrow = config.showPageEyebrow;
  config.showNoticeTitle = config.showPageTitle;
  config.noticeTitleAlign = config.pageHeaderAlign === 'center' ? 'center' : 'left';
  config.noticeEyebrowCss = config.pageEyebrowCss;
  config.noticeTitleCss = config.pageTitleCss;
  config.noticeMoreCss = config.pageMoreCss;
  if (module.moduleType === 'article' || module.templateCode === 'article') {
    config.article = {
      ...defaultArticleConfig(),
      ...safeObject(module.article, {})
    };
  } else {
    delete config.article;
  }
  const {
    style,
    builder,
    article,
    navTitle,
    navSortOrder,
    pageUrl,
    defaultOpenMode,
    popupWidth,
    popupHeight,
    showPageHeader,
    showPageEyebrow,
    showPageTitle,
    showPageIntro,
    showPageMore,
    hidePageHeaderMobile,
    pageHeaderAlign,
    pageHeaderSize,
    pageMoreOpenMode,
    pageHeaderCss,
    pageEyebrowCss,
    pageTitleCss,
    pageIntroCss,
    pageMoreCss,
    showMoreLink,
    showNoticeEyebrow,
    showNoticeTitle,
    noticeTitleAlign,
    noticeEyebrowCss,
    noticeTitleCss,
    noticeMoreCss,
    ...payload
  } = module;
  return {
    ...payload,
    configJson: prettyJson(config)
  };
};

const normalizeSite = (data: any) => {
  Object.assign(siteForm, data || {});
  siteForm.noticeLimit = siteForm.noticeLimit || 6;
  const parsedStyle = safeObject(parseJson(siteForm.sectionConfigJson, defaultSiteStyle()), defaultSiteStyle());
  Object.assign(siteStyleForm, defaultSiteStyle(), parsedStyle);
  siteStyleForm.headerDesktop = { ...defaultSiteStyle().headerDesktop, ...safeObject(siteStyleForm.headerDesktop, {}) };
  siteStyleForm.headerMobile = { ...defaultSiteStyle().headerMobile, ...safeObject(siteStyleForm.headerMobile, {}) };
  siteStyleForm.footerDesktop = { ...defaultSiteStyle().footerDesktop, ...safeObject(siteStyleForm.footerDesktop, {}) };
  siteStyleForm.footerMobile = { ...defaultSiteStyle().footerMobile, ...safeObject(siteStyleForm.footerMobile, {}) };
  headerButtons.value = safeArray(parseJson(siteForm.headerButtonsJson, []), defaultHeaderButtons()).map(normalizeHeaderButton);
  Object.assign(
    loginStateForm,
    defaultLoginState(),
    safeObject(parseJson(siteForm.loginStateConfigJson, defaultLoginState()), defaultLoginState())
  );
};

const getSiteConfig = async () => {
  loading.value = true;
  try {
    const res: any = await getHomeSiteConfig();
    normalizeSite(res.data || {});
  } finally {
    loading.value = false;
  }
};

const getModuleList = async () => {
  moduleLoading.value = true;
  try {
    const res: any = await listHomeModule();
    modules.value = (res.data || []).map(normalizeModule);
    if (!activeModuleCode.value || !modules.value.some((item) => item.moduleCode === activeModuleCode.value)) {
      activeModuleCode.value = modules.value[0]?.moduleCode || '';
    }
    if (!selectedBuilderId.value && activeModuleCode.value) {
      selectedBuilderKind.value = 'module';
      selectedBuilderId.value = activeModuleCode.value;
    }
  } finally {
    moduleLoading.value = false;
  }
};

const submitSiteConfig = async () => {
  saving.value = true;
  try {
    await saveHomeSiteConfig({
      ...siteForm,
      headerButtonsJson: JSON.stringify(headerButtons.value),
      loginStateConfigJson: JSON.stringify(loginStateForm),
      sectionConfigJson: JSON.stringify(trimStyleObject(siteStyleForm))
    });
    proxy?.$modal.msgSuccess('页头 / 页脚配置已保存');
    await getSiteConfig();
  } finally {
    saving.value = false;
  }
};

const syncModuleTemplate = (module: any) => {
  const builtInTypes = ['hero', 'guide', 'category', 'stage', 'timeline', 'notice', 'article', 'custom'];
  if (!module.templateCode || builtInTypes.includes(module.templateCode)) {
    module.templateCode = module.moduleType;
  }
  if (!module.anchor) {
    module.anchor = module.moduleCode;
  }
  if (!module.pageUrl) {
    module.pageUrl = `#${module.anchor || module.moduleCode}`;
  }
  if (!module.navTitle) {
    module.navTitle = module.moduleTitle;
  }
  if (module.navSortOrder === undefined || module.navSortOrder === null || module.navSortOrder === '') {
    module.navSortOrder = module.sortOrder || 0;
  }
  if (module.moduleType === 'article' && !module.article) {
    module.article = defaultArticleConfig();
  }
  Object.assign(module, {
    ...defaultPageHeaderConfig(module),
    ...pageHeaderFromConfig(module, {
      showPageHeader: module.showPageHeader,
      showPageEyebrow: module.showPageEyebrow,
      showPageTitle: module.showPageTitle,
      showPageIntro: module.showPageIntro,
      showPageMore: module.showPageMore,
      hidePageHeaderMobile: module.hidePageHeaderMobile,
      pageHeaderAlign: module.pageHeaderAlign,
      pageHeaderSize: module.pageHeaderSize,
      pageMoreOpenMode: module.pageMoreOpenMode,
      pageHeaderCss: module.pageHeaderCss,
      pageEyebrowCss: module.pageEyebrowCss,
      pageTitleCss: module.pageTitleCss,
      pageIntroCss: module.pageIntroCss,
      pageMoreCss: module.pageMoreCss
    })
  });
};

const submitModule = async () => {
  if (!activeModule.value) return;
  moduleSaving.value = true;
  try {
    syncModuleTemplate(activeModule.value);
    await saveHomeModule(buildModulePayload(activeModule.value));
    proxy?.$modal.msgSuccess('页面已保存');
    const current = activeModule.value.moduleCode;
    await getModuleList();
    activeModuleCode.value = current;
  } finally {
    moduleSaving.value = false;
  }
};

const addModule = () => {
  const code = `module_${Date.now()}`;
  modules.value.push({
    id: undefined,
    moduleCode: code,
    moduleTitle: '自定义页面',
    moduleType: 'custom',
    anchor: code,
    eyebrow: 'CUSTOM',
    intro: '',
    moreText: '',
    moreLink: '',
    navEnabled: true,
    enabled: true,
    sortOrder: (modules.value.length + 1) * 10,
    layoutType: 'default',
    templateCode: 'custom',
    configJson: '{}',
    style: defaultItemStyle(),
    navTitle: '自定义页面',
    navSortOrder: (modules.value.length + 1) * 10,
    pageUrl: `#${code}`,
    defaultOpenMode: 'current',
    popupWidth: 900,
    popupHeight: 700,
    ...defaultPageHeaderConfig({ moduleType: 'custom' }),
    showMoreLink: true,
    showNoticeEyebrow: true,
    showNoticeTitle: true,
    noticeTitleAlign: 'left',
    noticeEyebrowCss: '',
    noticeTitleCss: '',
    noticeMoreCss: '',
    article: defaultArticleConfig(),
    builder: defaultModuleBuilder(modules.value.length, 'custom')
  });
  activeModuleCode.value = code;
  selectedBuilderKind.value = 'module';
  selectedBuilderId.value = code;
  cards.value = [];
  cardTotal.value = 0;
};

const selectBuilderModule = async (module: any) => {
  selectedBuilderKind.value = 'module';
  selectedBuilderId.value = module.moduleCode;
  if (activeModuleCode.value !== module.moduleCode) {
    activeModuleCode.value = module.moduleCode;
    await getCardList();
  }
};

const selectBuilderCard = (card: any) => {
  selectedBuilderKind.value = 'card';
  selectedBuilderId.value = cardKey(card);
};

const updateVisibleCardSortOrders = () => {
  builderCanvasCards.value.forEach((item, index) => {
    item.sortOrder = (index + 1) * 10;
  });
};

const visibleCardIndex = (card: any) => builderCanvasCards.value.findIndex((item) => cardKey(item) === cardKey(card));

const canMoveBuilderCard = (card: any, direction: -1 | 1) => {
  const index = visibleCardIndex(card);
  return index >= 0 && index + direction >= 0 && index + direction < builderCanvasCards.value.length;
};

const applyVisibleCardOrder = (orderedVisibleCards: any[]) => {
  const visibleKeys = new Set(builderCanvasCards.value.map((item) => cardKey(item)));
  let visibleIndex = 0;
  cards.value = cards.value.map((item) => {
    if (!visibleKeys.has(cardKey(item))) {
      return item;
    }
    return orderedVisibleCards[visibleIndex++] || item;
  });
  updateVisibleCardSortOrders();
};

const reorderBuilderCard = (card: any, targetCard: any, placement: 'before' | 'after' = 'before') => {
  const sourceKey = cardKey(card);
  const targetKey = cardKey(targetCard);
  if (!sourceKey || !targetKey || sourceKey === targetKey) return;
  const ordered = builderCanvasCards.value.filter((item) => cardKey(item) !== sourceKey);
  const targetIndex = ordered.findIndex((item) => cardKey(item) === targetKey);
  if (targetIndex < 0) return;
  ordered.splice(placement === 'after' ? targetIndex + 1 : targetIndex, 0, card);
  applyVisibleCardOrder(ordered);
};

const moveBuilderCard = (card: any, direction: -1 | 1) => {
  const index = visibleCardIndex(card);
  if (index < 0) return;
  const target = builderCanvasCards.value[index + direction];
  if (!target) return;
  reorderBuilderCard(card, target, direction > 0 ? 'after' : 'before');
  selectBuilderCard(card);
};

const isFlowCardDragging = (card: any) => flowDraggingCardKey.value === cardKey(card);

const stopCardPointerDrag = () => {
  if (!cardDragState.value) return;
  window.removeEventListener('pointermove', handleCardPointerMove);
  window.removeEventListener('pointerup', stopCardPointerDrag);
  if (cardDragState.value.mode === 'flow') {
    updateVisibleCardSortOrders();
  }
  cardDragState.value = undefined;
  flowDraggingCardKey.value = '';
};

const handleCardPointerMove = (event: PointerEvent) => {
  const state = cardDragState.value;
  if (!state) return;
  if (state.mode === 'flow') {
    const targetNode = document.elementFromPoint(event.clientX, event.clientY)?.closest?.('[data-builder-card-key]') as HTMLElement | null;
    const targetKey = targetNode?.dataset?.builderCardKey;
    const targetCard = targetKey ? builderCanvasCards.value.find((item) => cardKey(item) === targetKey) : undefined;
    if (!targetCard || targetKey === cardKey(state.card)) return;
    const rect = targetNode.getBoundingClientRect();
    reorderBuilderCard(state.card, targetCard, event.clientY > rect.top + rect.height / 2 ? 'after' : 'before');
    return;
  }
  state.builder.x = Math.max(0, Math.round(state.startX + event.clientX - state.pointerX));
  state.builder.y = Math.max(0, Math.round(state.startY + event.clientY - state.pointerY));
};

const startCardPointerDrag = (event: PointerEvent, card: any) => {
  selectBuilderCard(card);
  if (activePageUsesFlowLayout.value) {
    flowDraggingCardKey.value = cardKey(card);
    cardDragState.value = {
      mode: 'flow',
      card
    };
    window.addEventListener('pointermove', handleCardPointerMove);
    window.addEventListener('pointerup', stopCardPointerDrag);
    return;
  }
  const builder = card.builder || defaultCardBuilder();
  card.builder = builder;
  cardDragState.value = {
    mode: 'free',
    builder,
    pointerX: event.clientX,
    pointerY: event.clientY,
    startX: builder.x,
    startY: builder.y
  };
  window.addEventListener('pointermove', handleCardPointerMove);
  window.addEventListener('pointerup', stopCardPointerDrag);
};

const cardPayloadWithBuilder = (card: any) => {
  const config = parseJson(card.configJson, {});
  config.builder = normalizeBuilder(card.builder, defaultCardBuilder());
  const { builder, ...payload } = card;
  return {
    ...payload,
    templateCode: payload.templateCode || payload.cardType,
    configJson: prettyJson(config)
  };
};

const saveBuilderLayout = async () => {
  if (!activeModule.value) return;
  saving.value = true;
  moduleSaving.value = true;
  cardSaving.value = true;
  try {
    syncModuleTemplate(activeModule.value);
    await saveHomeModule(buildModulePayload(activeModule.value));

    const orderedCards = activePageUsesFlowLayout.value
      ? [...builderCanvasCards.value]
      : [...builderCanvasCards.value].sort((a, b) => Number(a.builder?.y || 0) - Number(b.builder?.y || 0) || Number(a.builder?.x || 0) - Number(b.builder?.x || 0));
    orderedCards.forEach((item, index) => {
      item.sortOrder = (index + 1) * 10;
    });
    for (const card of orderedCards) {
      if (card.id) {
        await saveHomeCard(cardPayloadWithBuilder(card));
      }
    }
    await sortHomeCard(orderedCards.filter((item) => item.id).map((item) => ({ id: item.id, sortOrder: item.sortOrder })));
    proxy?.$modal.msgSuccess('页面画布已保存');
    const current = activeModuleCode.value;
    await getModuleList();
    activeModuleCode.value = current;
    await getCardList();
  } finally {
    saving.value = false;
    moduleSaving.value = false;
    cardSaving.value = false;
  }
};

const handleModuleDelete = async () => {
  if (!activeModule.value) return;
  if (isHomeModule(activeModule.value)) {
    proxy?.$modal.msgWarning('活动首页为固定页面，不能删除。');
    return;
  }
  await proxy?.$modal.confirm(`确认删除页面“${activeModule.value.moduleTitle}”吗？页面下的内容块也会一并清理。`);
  if (activeModule.value.id) {
    await deleteHomeModule(activeModule.value.id);
  } else {
    modules.value = modules.value.filter((item) => item.moduleCode !== activeModuleCode.value);
  }
  proxy?.$modal.msgSuccess('删除成功');
  activeModuleCode.value = modules.value[0]?.moduleCode || '';
  await getModuleList();
  await getCardList();
};

const addHeaderButton = () => {
  headerButtons.value.push(normalizeHeaderButton({ text: '新按钮', link: '#', style: 'ghost' }, headerButtons.value.length));
};
const removeHeaderButton = (index: number) => headerButtons.value.splice(index, 1);

const normalizeCard = (item: any, index = 0) => {
  const config = parseJson(item?.configJson, {});
  return {
    ...item,
    builder: normalizeBuilder(config.builder, defaultCardBuilder(index))
  };
};

const getCardList = async () => {
  if (!activeModuleCode.value) return;
  cardLoading.value = true;
  try {
    cardQuery.sectionKey = activeModuleCode.value;
    const res: any = await listHomeCard(cardQuery);
    cards.value = (res.rows || []).map(normalizeCard);
    cardTotal.value = res.total || 0;
  } finally {
    cardLoading.value = false;
  }
};

const defaultCardTypeForModule = () => {
  if (isHomeModule(activeModule.value)) return 'info';
  const type = activeModule.value?.moduleType || 'info';
  if (type === 'stage') return 'timeline';
  return ['hero', 'guide', 'category', 'notice'].includes(type) ? type : 'info';
};

const buildDefaultCard = () => ({
  id: undefined,
  sectionKey: activeModuleCode.value || 'guide',
  cardCode: undefined,
  cardType: defaultCardTypeForModule(),
  cardTitle: '',
  cardSubtitle: '',
  cardContent: '',
  icon: '',
  imageOssId: undefined,
  imageUrl: '',
  linkText: '',
  linkUrl: '#',
  linkType: 'pageDefault',
  highlight: false,
  enabled: true,
  sortOrder: (cards.value.length + 1) * 10,
  layoutType: 'default',
  templateCode: defaultCardTypeForModule(),
  slotKey: '',
  configJson: '{}',
  builder: defaultCardBuilder(cards.value.length),
  remark: ''
});

const prepareCardDialog = (card: any, title: string) => {
  cardForm.value = { ...card };
  cardForm.value.cardType = cardForm.value.cardType || cardForm.value.templateCode || defaultCardTypeForModule();
  cardForm.value.templateCode = cardForm.value.templateCode || cardForm.value.cardType;
  cardForm.value.configJson = cardForm.value.configJson || '{}';
  resetCardConfig(cardForm.value);
  cardDialog.title = title;
  cardDialog.visible = true;
  nextTick(() => cardFormRef.value?.clearValidate?.());
};

const buildPresetCard = (preset: any) => {
  const cardType = preset.cardType || 'info';
  const fallbackBuilder = defaultCardBuilder(cards.value.length);
  const presetConfig = safeObject(preset.config, {});
  const builder = normalizeBuilder({ ...fallbackBuilder, ...safeObject(presetConfig.builder, {}) }, fallbackBuilder);
  const config = {
    ...presetConfig,
    builder
  };
  return {
    ...buildDefaultCard(),
    cardType,
    templateCode: cardType,
    cardTitle: preset.cardTitle || preset.label || '新内容块',
    cardSubtitle: preset.cardSubtitle || '',
    cardContent: preset.cardContent || '',
    icon: preset.cardIcon || '',
    imageUrl: preset.imageUrl || '',
    linkText: preset.linkText || '',
    linkUrl: preset.linkUrl || '#',
    linkType: preset.linkType || 'pageDefault',
    sortOrder: (cards.value.length + 1) * 10,
    builder,
    configJson: prettyJson(config)
  };
};

const addPresetCard = (preset: any) => {
  if (!activeModule.value) {
    proxy?.$modal.msgWarning('请先选择一个页面');
    return;
  }
  prepareCardDialog(buildPresetCard(preset), `添加${preset.label || '内容块'}`);
};

const duplicateBuilderCard = (row: any) => {
  const parsed = parseJson(row.configJson, {});
  const fallbackBuilder = defaultCardBuilder(cards.value.length);
  const sourceBuilder = row.builder || parsed.builder || fallbackBuilder;
  const builder = normalizeBuilder(
    {
      ...sourceBuilder,
      x: Number(sourceBuilder.x || 0) + 24,
      y: Number(sourceBuilder.y || 0) + 24,
      z: Number(sourceBuilder.z || 0) + 1
    },
    fallbackBuilder
  );
  parsed.builder = builder;
  prepareCardDialog(
    {
      ...row,
      id: undefined,
      cardCode: undefined,
      cardTitle: `${row.cardTitle || '内容块'} 副本`,
      sortOrder: (cards.value.length + 1) * 10,
      builder,
      configJson: prettyJson(parsed)
    },
    '复制页面内容块'
  );
};

const resetCardConfig = (row: any) => {
  Object.keys(cardConfig).forEach((key) => delete cardConfig[key]);
  Object.keys(particleConfig).forEach((key) => delete particleConfig[key]);
  const parsed = parseJson(row.configJson, {});
  resetReactive(cardStyleConfig, {
    ...defaultItemStyle(),
    ...styleFromConfig(parsed)
  });
  resetReactive(heroElementCss, {
    ...defaultHeroElementCss(),
    ...safeObject(parsed.elementCss, {})
  });
  Object.assign(cardConfig, defaultCardAdvanced(), parsed);
  cardConfig.widthMode = cardConfig.widthMode || (cardConfig.fullWidth ? 'full' : 'normal');
  if (row.cardType === 'hero') {
    Object.assign(cardConfig, defaultHeroAdvanced(), parsed);
  }
  if (!cardConfig.carouselItemsText && Array.isArray(parsed.carouselItems)) {
    cardConfig.carouselItemsText = carouselItemsToText(parsed.carouselItems);
  }
  if (!cardConfig.timelineItemsText && Array.isArray(parsed.timelineItems)) {
    cardConfig.timelineItemsText = timelineItemsToText(parsed.timelineItems);
  }
  if (!cardConfig.buttonsText && Array.isArray(parsed.buttons)) {
    cardConfig.buttonsText = buttonItemsToText(parsed.buttons);
  }
  Object.assign(particleConfig, defaultParticle(), parsed.particle || {});
  cardConfig.sloganText = Array.isArray(parsed.sloganLines) ? parsed.sloganLines.join(' / ') : cardConfig.sloganText || '';
  heroButtons.value = safeArray(parsed.buttons, defaultHeroButtons());
  if (row.cardType === 'notice') {
    cardConfig.noticeLimit = cardConfig.noticeLimit || siteForm.noticeLimit || 6;
    cardConfig.showDate = cardConfig.showDate !== false;
    cardConfig.showTag = cardConfig.showTag !== false;
  }
};

const openCardForm = (row?: any) => {
  prepareCardDialog(row ? { ...row } : buildDefaultCard(), row ? '编辑页面内容块' : '新增页面内容块');
};

const handleCardTypeChange = () => {
  cardForm.value.templateCode = cardForm.value.cardType;
  Object.assign(cardConfig, defaultCardAdvanced());
  if (cardForm.value.cardType === 'hero') {
    Object.assign(cardConfig, defaultHeroAdvanced());
    Object.assign(particleConfig, defaultParticle());
    resetReactive(heroElementCss, defaultHeroElementCss());
    heroButtons.value = defaultHeroButtons();
  }
};

const addHeroButton = () => {
  heroButtons.value.push({ text: '新按钮', link: '#', style: 'secondary', enabled: true, sortOrder: (heroButtons.value.length + 1) * 10 });
};
const removeHeroButton = (index: number) => heroButtons.value.splice(index, 1);

const buildCardConfig = () => {
  let config = mergeStyleToConfig(parseJson(cardForm.value.configJson, {}), cardStyleConfig);
  if (cardForm.value.cardType === 'hero') {
    config.kicker = cardConfig.kicker || 'DYZ · ART EXHIBITION';
    config.sloganLines = String(cardConfig.sloganText || cardForm.value.cardTitle || '')
      .split(/[\/\n]/)
      .map((item) => item.trim())
      .filter(Boolean);
    config.dynamicLabel = cardConfig.dynamicLabel || 'ART';
    config.dynamicWord = cardConfig.dynamicWord || '艺术展演';
    config.visualAnimation = cardConfig.visualAnimation || 'float';
    config.visualFollowMouse = cardConfig.visualFollowMouse !== false;
    config.visualTiltIntensity = cardConfig.visualTiltIntensity || 8;
    config.backgroundType = cardConfig.backgroundType || 'default';
    config.backgroundImage = cardConfig.backgroundImage || '';
    config.backgroundHtml = cardConfig.backgroundHtml || '';
    config.buttons = heroButtons.value;
    config.particle = {
      ...defaultParticle(),
      ...particleConfig,
      blueColor: normalizedParticleColor('blueColor', DEFAULT_BLUE_PARTICLE),
      accentColor: normalizedParticleColor('accentColor', DEFAULT_ACCENT_PARTICLE)
    };
    const elementCss = trimStyleObject(heroElementCss);
    if (Object.keys(elementCss).length) config.elementCss = elementCss;
    else delete config.elementCss;
  }
  if (cardForm.value.cardType === 'notice') {
    config.noticeLimit = cardConfig.noticeLimit || siteForm.noticeLimit || 6;
    config.noticeVariant = cardConfig.noticeVariant || 'list';
    config.showDate = cardConfig.showDate !== false;
    config.showTag = cardConfig.showTag !== false;
    config.showNoticeKicker = cardConfig.showNoticeKicker !== false;
  }
  if (cardForm.value.cardType !== 'hero') {
    config.widthMode = cardConfig.widthMode || (cardConfig.fullWidth ? 'full' : 'normal');
    config.fullWidth = config.widthMode === 'full' || !!cardConfig.fullWidth;
    config.mobileHidden = !!cardConfig.mobileHidden;
    config.contentMode = cardConfig.contentMode || 'text';
    config.modalTitle = cardConfig.modalTitle || '';
    config.modalWidth = cardConfig.modalWidth || 880;
    config.modalHeight = cardConfig.modalHeight || 620;
    config.detailSource = cardConfig.detailSource || 'none';
    config.detailOpenMode = cardConfig.detailOpenMode || 'modal';
    config.detailButtonText = cardConfig.detailButtonText || '查看详情';
    config.detailTitle = cardConfig.detailTitle || '';
    config.detailContentMode = cardConfig.detailContentMode || 'markdown';
    config.detailContent = cardConfig.detailContent || '';
    config.detailPdfOssId = cardConfig.detailPdfOssId || '';
    config.detailPdfTitle = cardConfig.detailPdfTitle || '';
    config.detailRequireLogin = !!cardConfig.detailRequireLogin;
    config.showCardTitle = cardConfig.showCardTitle !== false;
    config.showCardContent = cardConfig.showCardContent !== false;
    config.showCardImage = cardConfig.showCardImage !== false;
    config.showCardIcon = cardConfig.showCardIcon !== false;
    config.showCardAction = cardConfig.showCardAction !== false;
    config.cardChrome = cardConfig.cardChrome || 'default';
    config.showCardBackground = cardConfig.showCardBackground !== false;
    config.showCardBorder = cardConfig.showCardBorder !== false;
    config.showCardShadow = cardConfig.showCardShadow !== false;
    config.cardBgColor = cardConfig.cardBgColor || '';
    config.cardBorderColor = cardConfig.cardBorderColor || '';
    config.cardTitleColor = cardConfig.cardTitleColor || '';
    config.cardTextColor = cardConfig.cardTextColor || '';
    config.cardAccentColor = cardConfig.cardAccentColor || '';
    config.cardRadius = Number(cardConfig.cardRadius ?? 18);
    config.cardPadding = Number(cardConfig.cardPadding ?? 24);
  }
  if (cardForm.value.cardType === 'spacer') {
    const spacerMode = cardConfig.spacerMode || (cardConfig.spacerLine ? 'line' : 'blank');
    config.spacerHeight = Number(cardConfig.spacerHeight ?? 56);
    config.spacerMobileHeight = Number(cardConfig.spacerMobileHeight ?? 32);
    config.spacerMode = spacerMode;
    config.spacerLine = spacerMode === 'line';
    config.spacerLineStyle = cardConfig.spacerLineStyle || 'gradient';
    config.spacerLineColor = cardConfig.spacerLineColor || '';
    config.spacerLineWidth = cardConfig.spacerLineWidth || '100%';
    config.spacerLineSize = Number(cardConfig.spacerLineSize ?? 1);
    config.widthMode = 'full';
    config.fullWidth = true;
  }
  if (['image', 'info', 'button', 'custom'].includes(cardForm.value.cardType)) {
    config.cardVariant = cardConfig.cardVariant || 'imageTop';
  }
  if (cardForm.value.cardType === 'button') {
    config.buttonDisplayMode = cardConfig.buttonDisplayMode || 'card';
    config.buttonVariant = cardConfig.buttonVariant || 'solid';
    config.buttonAlign = cardConfig.buttonAlign || 'left';
    config.buttonLayout = cardConfig.buttonLayout || 'row';
    config.buttonMobileLayout = cardConfig.buttonMobileLayout || 'same';
    config.buttonGap = Number(cardConfig.buttonGap ?? 12);
    config.buttonMobileGap = Number(cardConfig.buttonMobileGap ?? 10);
    config.buttonBgColor = cardConfig.buttonBgColor || '';
    config.buttonTextColor = cardConfig.buttonTextColor || '';
    config.buttonBorderColor = cardConfig.buttonBorderColor || '';
    config.buttonsText = cardConfig.buttonsText || '';
    config.buttons = parseButtonItems(cardConfig.buttonsText);
  }
  if (cardForm.value.cardType === 'html') {
    config.htmlLayout = cardConfig.htmlLayout || 'contained';
    config.htmlDesktop = cardConfig.htmlDesktop || '';
    config.htmlMobile = cardConfig.htmlMobile || '';
  }
  if (['carousel', 'linkedCarousel'].includes(cardForm.value.cardType)) {
    config.carouselVariant = cardForm.value.cardType === 'linkedCarousel' ? 'linked' : cardConfig.carouselVariant || 'single';
    config.autoplay = cardConfig.autoplay !== false;
    config.interval = cardConfig.interval || 4;
    config.carouselItemsText = cardConfig.carouselItemsText || '';
    config.carouselItems = parseCarouselItems(cardConfig.carouselItemsText);
  }
  if (cardForm.value.cardType === 'timeline') {
    config.timelineKicker = cardConfig.timelineKicker || 'TIMELINE';
    config.timelineTitle = cardConfig.timelineTitle || cardForm.value.cardTitle || '';
    config.timelineIntro = cardConfig.timelineIntro || cardForm.value.cardContent || '';
    config.timelineVariant = cardConfig.timelineVariant || 'horizontal';
    config.timelineMobileMode = cardConfig.timelineMobileMode || 'stack';
    config.showTimelineKicker = cardConfig.showTimelineKicker !== false;
    config.showTimelineTitle = cardConfig.showTimelineTitle !== false;
    config.showTimelineIntro = cardConfig.showTimelineIntro !== false;
    config.showTimelineBackground = cardConfig.showTimelineBackground !== false;
    config.showTimelineBorder = cardConfig.showTimelineBorder !== false;
    config.showTimelineShadow = cardConfig.showTimelineShadow !== false;
    config.showTimelineLine = cardConfig.showTimelineLine !== false;
    config.timelineBgColor = cardConfig.timelineBgColor || '';
    config.timelineBorderColor = cardConfig.timelineBorderColor || '';
    config.timelineLineColor = cardConfig.timelineLineColor || '';
    config.timelineDotColor = cardConfig.timelineDotColor || '';
    config.timelineAccentColor = cardConfig.timelineAccentColor || '';
    config.timelineTitleColor = cardConfig.timelineTitleColor || '';
    config.timelineTextColor = cardConfig.timelineTextColor || '';
    config.timelineItemsText = cardConfig.timelineItemsText || '';
    config.timelineItems = parseTimelineItems(cardConfig.timelineItemsText);
    config.widthMode = config.widthMode || 'full';
    config.fullWidth = true;
  }
  config.builder = normalizeBuilder(cardForm.value.builder || cardConfig.builder, defaultCardBuilder());
  return config;
};

const submitCardForm = async () => {
  await cardFormRef.value?.validate?.();
  cardSaving.value = true;
  try {
    const { builder, ...cardPayload } = cardForm.value;
    const payload = {
      ...cardPayload,
      templateCode: cardForm.value.templateCode || cardForm.value.cardType,
      configJson: prettyJson(buildCardConfig())
    };
    await saveHomeCard(payload);
    proxy?.$modal.msgSuccess('页面内容块保存成功');
    cardDialog.visible = false;
    await getCardList();
  } finally {
    cardSaving.value = false;
  }
};

const handleCardDelete = async (row: any) => {
  if (isProtectedHomeHeroCard(row)) {
    proxy?.$modal.msgWarning('Hero 首屏为活动首页固定内容，不能删除。');
    return;
  }
  if (!row.id) {
    cards.value = cards.value.filter((item) => item !== row);
    if (selectedBuilderId.value === cardKey(row)) {
      selectedBuilderKind.value = 'module';
      selectedBuilderId.value = activeModuleCode.value;
    }
    return;
  }
  await proxy?.$modal.confirm(`确认删除页面内容块“${row.cardTitle}”吗？`);
  await deleteHomeCard(row.id);
  proxy?.$modal.msgSuccess('删除成功');
  await getCardList();
};

const handleModuleChange = async () => {
  cardQuery.pageNum = 1;
  selectedBuilderKind.value = 'module';
  selectedBuilderId.value = activeModuleCode.value;
  await getCardList();
};

const refreshAll = async () => {
  const [siteResult, moduleResult] = await Promise.allSettled([getSiteConfig(), getModuleList()]);
  if (siteResult.status === 'rejected') {
    logLoadFailure('site config', siteResult.reason);
  }
  if (moduleResult.status === 'rejected') {
    logLoadFailure('module list', moduleResult.reason);
    return;
  }
  try {
    await getCardList();
  } catch (error) {
    logLoadFailure('card list', error);
  }
};

onMounted(() => {
  refreshAll();
});
</script>

<style scoped lang="scss">
.home-config-page {
  .page-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

  .page-kicker {
    color: var(--el-color-primary);
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.14em;
  }

  h2 {
    margin: 6px 0 8px;
    color: var(--el-text-color-primary);
    font-size: 24px;
    font-weight: 800;
  }

  h3 {
    margin: 0 0 6px;
    color: var(--el-text-color-primary);
    font-size: 16px;
    font-weight: 800;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
  }

  .page-actions,
  .sub-head,
  .module-actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .sub-head {
    margin: 8px 0 14px;
  }

  .mini-head {
    margin-top: 6px;
  }

  .module-tabs {
    margin-bottom: 16px;
  }

  .module-tab-label {
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }

  .module-form {
    padding-top: 2px;
  }

  .module-actions {
    justify-content: flex-start;
    margin: 2px 0 18px;
  }

  .css-config-collapse {
    margin: 4px 0 18px;
  }

  .css-config-collapse :deep(.el-collapse-item__content) {
    padding-bottom: 4px;
  }

  .card-head {
    padding-top: 14px;
    border-top: 1px solid var(--el-border-color-lighter);
  }

  .line-clamp {
    display: -webkit-box;
    line-height: 1.6;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .color-control {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .color-control .el-input {
    flex: 1;
    min-width: 0;
  }

  .muted {
    color: var(--el-text-color-secondary);
  }

  .builder-toolbar {
    margin-bottom: 12px;
    padding: 18px 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    background: linear-gradient(135deg, rgba(240, 249, 255, .95), rgba(255, 255, 255, .98));
    box-shadow: var(--el-box-shadow-light);
  }

  .builder-toolbar-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 10px;
  }

  .builder-shell {
    min-height: 720px;
    display: grid;
    grid-template-columns: 260px minmax(520px, 1fr) 360px;
    gap: 12px;
  }

  .builder-palette,
  .builder-stage-wrap,
  .builder-inspector {
    min-width: 0;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    background: rgba(255, 255, 255, .96);
    box-shadow: var(--el-box-shadow-light);
  }

  .builder-palette,
  .builder-inspector {
    padding: 14px;
  }

  .builder-panel-title,
  .builder-stage-head,
  .builder-card-board-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 12px;
  }

  .builder-panel-title span,
  .builder-stage-head small {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  .builder-component-library {
    margin: 4px 0 16px;
  }

  .builder-card-board-head span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  .component-preset-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .component-preset-card {
    min-height: 86px;
    padding: 10px;
    display: grid;
    align-content: start;
    gap: 5px;
    border: 1px solid rgba(10, 142, 219, .16);
    border-radius: 8px;
    background: linear-gradient(180deg, rgba(248, 252, 255, .98), rgba(239, 249, 255, .92));
    color: var(--el-text-color-primary);
    text-align: left;
    cursor: pointer;
    transition: border-color .18s ease, box-shadow .18s ease, transform .18s ease;
  }

  .component-preset-card:hover {
    border-color: rgba(10, 142, 219, .48);
    box-shadow: 0 10px 22px rgba(0, 94, 168, .12);
    transform: translateY(-1px);
  }

  .component-preset-card strong,
  .component-preset-card em {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .component-preset-card em {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    font-style: normal;
  }

  .component-preset-icon {
    width: fit-content;
    max-width: 100%;
    padding: 2px 6px;
    border-radius: 6px;
    background: rgba(10, 142, 219, .10);
    color: var(--el-color-primary);
    font-size: 11px;
    font-weight: 800;
    letter-spacing: .04em;
  }

  .builder-palette-item {
    min-height: 72px;
    margin-bottom: 10px;
    padding: 12px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    border: 1px solid rgba(10, 142, 219, .14);
    border-radius: 8px;
    background: rgba(245, 251, 255, .92);
    cursor: grab;
    transition: border-color .18s ease, box-shadow .18s ease, transform .18s ease;
  }

  .builder-palette-item:hover {
    border-color: rgba(10, 142, 219, .45);
    box-shadow: 0 10px 24px rgba(0, 94, 168, .10);
    transform: translateY(-1px);
  }

  .builder-palette-item.is-selected {
    border-color: var(--el-color-primary);
    background: linear-gradient(135deg, rgba(231, 245, 255, .96), rgba(255, 255, 255, .98));
    box-shadow: 0 0 0 2px rgba(64, 158, 255, .14), 0 12px 26px rgba(0, 94, 168, .12);
  }

  .builder-palette-item.is-hidden {
    opacity: .58;
  }

  .builder-palette-item strong,
  .builder-card-node strong {
    display: block;
    color: var(--el-text-color-primary);
  }

  .builder-palette-item span,
  .builder-card-node span {
    display: block;
    margin-top: 4px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.45;
  }

  .builder-stage-wrap {
    padding: 14px;
    overflow: auto;
  }

  .builder-stage {
    position: relative;
    min-width: 960px;
    padding: 18px;
    display: grid;
    gap: 14px;
    align-content: start;
    border: 1px dashed rgba(10, 142, 219, .30);
    border-radius: 8px;
    background:
      linear-gradient(rgba(10, 142, 219, .04) 1px, transparent 1px),
      linear-gradient(90deg, rgba(10, 142, 219, .04) 1px, transparent 1px),
      linear-gradient(180deg, #f9fdff, #ffffff);
    background-size: 32px 32px, 32px 32px, auto;
  }

  .builder-stage.is-free {
    display: block;
  }

  .builder-stage.is-fixed-home {
    min-width: 0;
    display: grid;
    grid-template-columns: 1fr;
    align-content: start;
    justify-items: center;
  }

  .builder-fixed-home-preview {
    width: min(520px, 92%);
    padding: 28px;
    border: 1px solid rgba(10, 142, 219, .16);
    border-radius: 8px;
    background:
      radial-gradient(circle at 80% 16%, rgba(69, 203, 198, .18), transparent 34%),
      linear-gradient(135deg, rgba(255, 255, 255, .96), rgba(238, 249, 255, .92));
    box-shadow: 0 18px 40px rgba(0, 94, 168, .12);
    text-align: center;
  }

  .builder-stage.is-fixed-home .builder-card-node {
    width: min(520px, 92%);
    margin-top: 10px;
  }

  .builder-fixed-home-preview span {
    color: var(--el-color-primary);
    font-size: 12px;
    font-weight: 800;
    letter-spacing: .16em;
  }

  .builder-fixed-home-preview strong {
    display: block;
    margin: 10px 0;
    color: var(--el-text-color-primary);
    font-size: 22px;
  }

  .builder-module-node {
    min-height: 150px;
    padding: 14px;
    border: 1px solid rgba(10, 142, 219, .18);
    border-radius: 8px;
    background: rgba(255, 255, 255, .92);
    box-shadow: 0 12px 28px rgba(0, 94, 168, .10);
    cursor: grab;
    user-select: none;
  }

  .builder-stage.is-free .builder-module-node {
    position: absolute;
  }

  .builder-module-node.is-selected,
  .builder-card-node.is-selected {
    border-color: var(--el-color-primary);
    box-shadow: 0 0 0 2px rgba(64, 158, 255, .18), 0 16px 32px rgba(0, 94, 168, .16);
  }

  .builder-module-node.is-hidden,
  .builder-card-node.is-hidden {
    opacity: .52;
  }

  .builder-node-top,
  .builder-node-meta {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .builder-node-top {
    justify-content: space-between;
  }

  .builder-node-top strong {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .builder-node-body {
    margin-top: 18px;
    display: grid;
    gap: 6px;
    color: var(--el-text-color-secondary);
  }

  .builder-node-body em {
    font-style: normal;
    color: var(--el-color-primary);
  }

  .builder-node-meta {
    margin-top: 18px;
    justify-content: space-between;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  .builder-inspector {
    overflow: auto;
  }

  .builder-inspector-form :deep(.el-form-item) {
    margin-bottom: 12px;
  }

  .builder-empty-actions {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  .builder-card-node {
    position: relative;
    min-width: 120px;
    min-height: 80px;
    padding: 10px;
    border: 1px solid rgba(10, 142, 219, .16);
    border-radius: 8px;
    background: rgba(255, 255, 255, .94);
    box-shadow: 0 8px 18px rgba(0, 94, 168, .10);
    cursor: grab;
    user-select: none;
  }

  .builder-card-node.is-flow-dragging {
    opacity: .72;
    cursor: grabbing;
    outline: 2px solid rgba(64, 158, 255, .26);
  }

  .builder-flow-alert {
    margin-bottom: 12px;
  }

  .builder-node-actions {
    margin-top: 10px;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  .builder-inspector-actions {
    margin-top: 10px;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  .builder-stage.is-free .builder-card-node {
    position: absolute;
  }
}

@media (max-width: 760px) {
  .home-config-page {
    .page-head,
    .sub-head,
    .module-actions {
      align-items: flex-start;
      flex-direction: column;
    }

    .page-actions {
      flex-wrap: wrap;
    }

    .builder-toolbar,
    .builder-shell {
      display: flex;
      flex-direction: column;
    }

    .builder-stage {
      min-width: 720px;
    }
  }
}
</style>
