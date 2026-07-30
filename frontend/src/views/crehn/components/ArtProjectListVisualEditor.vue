<template>
  <section class="art-project-list-visual" :class="{ 'is-compact': compact }">
    <div v-if="!compact" class="art-visual-toolbar">
      <div>
        <strong>{{ schoolSubmitMode ? schoolSubmitSceneTitle : '项目列表可视化编辑器' }}</strong>
        <small>{{ schoolSubmitMode ? schoolSubmitSceneDescription : '拖动组件调整位置，点击组件或卡片后在右侧修改参数' }}</small>
      </div>
      <el-segmented v-if="!schoolSubmitMode && !pageKey" v-model="activePage" :options="pageSegmentOptions" />
    </div>
    <div v-if="schoolSubmitMode" class="school-submit-scene-guide" aria-label="统一提交配置作用范围">
      <span :class="{ 'is-current': schoolSubmitScene === 'home' }"><strong>首页组件</strong><small>只作用于普通首页卡片</small></span>
      <span :class="{ 'is-current': schoolSubmitScene === 'maximized' }"><strong>首页最大化</strong><small>只作用于组件最大化页面</small></span>
      <span :class="{ 'is-current': schoolSubmitScene === 'standalone' }"><strong>独立提交页</strong><small>由页面类别中的学校提交维护</small></span>
    </div>

    <el-tabs
      v-model="activeSection"
      type="border-card"
      class="art-project-list-visual__sections"
      :class="{ 'is-single-section': schoolSubmitMode || activePage === 'project' }"
    >
      <el-tab-pane label="布局画布" name="layout">
        <div class="art-visual-workspace">
          <aside class="art-visual-panel art-component-palette">
            <header><strong>组件</strong></header>
            <section v-for="group in paletteGroups" :key="group.label">
              <span class="art-component-palette__group">{{ group.label }}</span>
              <button
                v-for="item in group.items"
                :key="item.key"
                type="button"
                class="art-component-palette__item"
                :class="{ 'is-used': !isRepeatableItem(item.key) && configuredItems.has(item.key) }"
                :title="`${item.label}：${item.description}`"
                :draggable="isRepeatableItem(item.key) || !configuredItems.has(item.key)"
                @dragstart="startPaletteDrag($event, item.key)"
                @dragend="clearDragState"
                @click="selectConfiguredItem(item.key)"
              >
                <el-icon><Rank /></el-icon>
                <span>{{ item.label }}</span>
              </button>
            </section>
          </aside>

          <main class="art-layout-canvas">
            <div v-if="activePage !== 'project'" class="art-layout-canvas__context">
              <el-switch v-model="pageConfig.sidebarEnabled" active-text="允许展开左侧筛选" />
            </div>

            <div class="art-layout-canvas__preview" :style="headerPreviewStyle">
              <div class="art-layout-canvas__surface">
                <article
                  v-for="(card, cardIndex) in pageConfig.cards"
                  :key="card.key"
                  class="art-layout-card"
                  :class="{ 'is-selected': selection.kind === 'card' && selection.cardKey === card.key }"
                  :style="cardPreviewStyle(card)"
                  @dragover.prevent
                  @drop="dropCard($event, cardIndex)"
                  @click.self="selectCard(card.key)"
                >
                  <header class="art-layout-card__head" @click="selectCard(card.key)">
                    <span draggable="true" @dragstart.stop="startCardDrag($event, cardIndex)" @dragend="clearCardDrag"
                      ><el-icon><Rank /></el-icon>长条卡片 {{ cardIndex + 1 }}</span
                    >
                    <div>
                      <el-button text icon="ArrowUp" :disabled="cardIndex === 0" @click.stop="moveCard(cardIndex, -1)" />
                      <el-button text icon="ArrowDown" :disabled="cardIndex === pageConfig.cards.length - 1" @click.stop="moveCard(cardIndex, 1)" />
                      <el-button
                        text
                        type="danger"
                        icon="Delete"
                        :disabled="pageConfig.cards.length <= 1 || cardContainsRequiredItem(card)"
                        @click.stop="removeCard(cardIndex)"
                      />
                    </div>
                  </header>
                  <div
                    v-for="rowIndex in [0, 1]"
                    :key="rowIndex"
                    class="art-layout-row"
                    :class="{
                      'is-empty': !card.rows[rowIndex]?.length,
                      'is-drop-target': isItemDropTarget(cardIndex, rowIndex)
                    }"
                    :style="rowPreviewStyle"
                    @dragover="markItemDropTarget($event, cardIndex, rowIndex)"
                    @drop.stop="dropItem($event, cardIndex, rowIndex)"
                  >
                    <span class="art-layout-row__label">
                      {{ rowIndex === 0 ? '第一行' : '第二行' }}
                      <small>
                        配置 {{ rowConfiguredUsage(card.rows[rowIndex]) }} 格
                        <template v-if="rowConfiguredUsage(card.rows[rowIndex]) !== rowGridUsage(card.rows[rowIndex])">
                          → 实际 {{ rowGridUsage(card.rows[rowIndex]) }} 格
                        </template>
                        <template v-if="rowHasOnlySpacers(card.rows[rowIndex])"> · 仅空白不生成高度</template>
                      </small>
                    </span>
                    <template v-for="item in previewItems(card.rows[rowIndex])" :key="item.id">
                      <div
                        v-if="item.key === 'compactGroup'"
                        class="art-layout-item art-layout-compact-group"
                        :class="[
                          {
                            'is-selected': isSelectedItem(card.key, item.id),
                            'is-group-drop-target': compactGroupInnerDropTarget === item.id
                          },
                          outerDropEdgeClasses(cardIndex, rowIndex, item.id)
                        ]"
                        :style="itemPreviewStyle(item.id, item.key, card.rows[rowIndex])"
                        draggable="true"
                        @click.stop="selectItem(card.key, item.id, item.key)"
                        @dragstart.stop="startItemDrag($event, cardIndex, rowIndex, item.index, item.id, item.key)"
                        @dragend="clearDragState"
                        @dragover.stop="markCompactGroupDropTarget($event, cardIndex, rowIndex, item.index, item.id)"
                        @drop.stop="dropItemOnCompactGroup($event, cardIndex, rowIndex, item.index, item.id)"
                      >
                        <span class="art-layout-compact-group__title"
                          ><el-icon><Rank /></el-icon>紧凑组</span
                        >
                        <button
                          v-for="(childKey, childIndex) in pageConfig.compactGroupInstances[item.id]?.items || []"
                          :key="childKey"
                          type="button"
                          class="art-layout-compact-group__item"
                          draggable="true"
                          @click.stop="selectItem(card.key, childKey, childKey)"
                          @dragstart.stop="startGroupItemDrag($event, item.id, childIndex, childKey)"
                          @dragend="clearDragState"
                        >
                          {{ itemPreviewText(childKey, childKey) }}
                        </button>
                        <span v-if="!pageConfig.compactGroupInstances[item.id]?.items.length" class="art-layout-compact-group__empty">
                          拖入组件
                        </span>
                      </div>
                      <button
                        v-else
                        type="button"
                        class="art-layout-item"
                        :class="[
                          `is-${item.key}`,
                          { 'is-selected': isSelectedItem(card.key, item.id) },
                          outerDropEdgeClasses(cardIndex, rowIndex, item.id)
                        ]"
                        :style="itemPreviewStyle(item.id, item.key, card.rows[rowIndex])"
                        :aria-label="itemPreviewFullText(item.id, item.key)"
                        :title="itemPreviewFullText(item.id, item.key)"
                        draggable="true"
                        @click.stop="selectItem(card.key, item.id, item.key)"
                        @dragstart.stop="startItemDrag($event, cardIndex, rowIndex, item.index, item.id, item.key)"
                        @dragend="clearDragState"
                        @dragover.stop="markOuterItemDropTarget($event, cardIndex, rowIndex, item.index, item.id)"
                        @drop.stop="dropItem($event, cardIndex, rowIndex, outerDropIndex($event, item.index))"
                      >
                        <el-icon><Rank /></el-icon>
                        <span>{{ itemPreviewText(item.id, item.key) }}</span>
                      </button>
                    </template>
                    <span v-if="!card.rows[rowIndex]?.length" class="art-layout-row__empty">拖动组件到此行</span>
                  </div>
                </article>
                <button type="button" class="art-layout-card-add" :disabled="pageConfig.cards.length >= 6" @click="addCard">
                  <el-icon><Plus /></el-icon>添加长条卡片
                </button>
              </div>
              <div class="art-layout-table-reference" aria-hidden="true">
                <span>序号</span><span>项目名称</span><span>状态</span><span>操作</span>
              </div>
            </div>
          </main>

          <aside class="art-visual-panel art-property-panel">
            <header>
              <strong>属性</strong><small>{{ selectionTitle }}</small>
            </header>
            <el-collapse class="art-header-metrics" :model-value="['headerMetrics']">
              <el-collapse-item title="表头尺寸与边距" name="headerMetrics">
                <el-form label-position="top">
                  <el-form-item label="单行最小高度">
                    <el-slider v-model="pageConfig.rowMinHeight" :min="28" :max="96" show-input />
                  </el-form-item>
                  <div class="art-header-metrics__grid">
                    <el-form-item label="按钮区上边距"><el-input-number v-model="pageConfig.paddingTop" :min="0" :max="32" /></el-form-item>
                    <el-form-item label="背景条内下边距">
                      <el-input-number v-model="pageConfig.paddingBottom" :min="0" :max="32" />
                      <small class="art-property-hint art-header-metric-hint">按钮底部到背景条底边的距离</small>
                    </el-form-item>
                    <el-form-item label="左右安全边距"><el-input-number v-model="pageConfig.paddingInline" :min="0" :max="40" /></el-form-item>
                    <el-form-item label="与上方距离"><el-input-number v-model="pageConfig.marginTop" :min="0" :max="40" /></el-form-item>
                    <el-form-item label="背景条与表格间距">
                      <el-input-number v-model="pageConfig.marginBottom" :min="0" :max="40" />
                      <small class="art-property-hint art-header-metric-hint">背景条底边到表格表头的距离</small>
                    </el-form-item>
                  </div>
                </el-form>
              </el-collapse-item>
            </el-collapse>
            <template v-if="selectedCard">
              <el-form label-position="top">
                <el-form-item label="卡片背景"><el-color-picker v-model="selectedCard.backgroundColor" /></el-form-item>
                <el-form-item label="背景透明度">
                  <el-slider v-model="selectedCard.backgroundOpacity" :min="0" :max="100" show-input />
                </el-form-item>
                <el-form-item label="圆角">
                  <el-slider v-model="selectedCard.borderRadius" :min="0" :max="24" show-input />
                </el-form-item>
                <el-form-item label="卡片边框"><el-switch v-model="selectedCard.borderVisible" /></el-form-item>
              </el-form>
            </template>
            <template v-else-if="selectedItemKey">
              <el-form label-position="top">
                <template v-if="selectedCompactGroup">
                  <el-alert
                    class="mb-3"
                    type="info"
                    :closable="false"
                    title="按钮按内容宽度排列；筛选、状态等内容组件会在组内收缩。可拖动普通组件到组内，或把组内组件拖回外层栅格。"
                  />
                  <el-form-item label="组内对齐">
                    <el-radio-group v-model="selectedCompactGroup.align">
                      <el-radio-button label="left">居左</el-radio-button>
                      <el-radio-button label="center">居中</el-radio-button>
                      <el-radio-button label="right">居右</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item label="外层宽度模式">
                    <el-radio-group v-model="selectedCompactGroup.widthMode">
                      <el-radio-button label="fixed">固定格数</el-radio-button>
                      <el-radio-button label="auto">自动伸缩</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item v-if="selectedCompactGroup.widthMode === 'fixed'" label="外层占用格数">
                    <el-slider v-model="selectedCompactGroup.span" :min="1" :max="12" show-input />
                  </el-form-item>
                  <el-form-item label="组内组件间隔">
                    <el-slider v-model="selectedCompactGroup.gap" :min="0" :max="32" show-input />
                  </el-form-item>
                  <el-form-item label="窄屏按钮折叠">
                    <el-radio-group v-model="selectedCompactGroup.collapseMode">
                      <el-radio-button label="none">不折叠</el-radio-button>
                      <el-radio-button label="overflow">自动分级</el-radio-button>
                      <el-radio-button label="all">全部合并</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                  <template v-if="selectedCompactGroup.collapseMode !== 'none'">
                    <el-form-item label="下拉按钮文字">
                      <el-input v-model="selectedCompactGroup.collapseText" maxlength="10" placeholder="更多" />
                    </el-form-item>
                    <el-form-item v-if="selectedCompactGroup.collapseMode === 'overflow'" label="优先常驻按钮">
                      <el-checkbox-group v-model="selectedCompactGroup.pinnedItems">
                        <el-checkbox v-for="item in selectedCompactCollapsibleItems" :key="item.key" :label="item.key">
                          {{ item.label }}
                        </el-checkbox>
                      </el-checkbox-group>
                    </el-form-item>
                    <small class="art-property-hint">
                      {{
                        selectedCompactGroup.collapseMode === 'overflow'
                          ? '实际空间不足时，非固定按钮先进入下拉；仍放不下时全部按钮合并。'
                          : '实际空间不足时，组内按钮全部合并为一个下拉；筛选、状态和文字组件不参与折叠。'
                      }}
                    </small>
                  </template>
                  <div class="art-compact-group-property-list">
                    <span v-for="childKey in selectedCompactGroup.items" :key="childKey">
                      {{ workspaceHeaderItemOptions.find((item) => item.key === childKey)?.label || childKey }}
                    </span>
                    <small v-if="!selectedCompactGroup.items.length">尚未放入组件</small>
                  </div>
                </template>
                <el-form-item v-if="selectedItemConfig && !selectedSpacerConfig" label="组件位置">
                  <el-radio-group v-model="selectedItemConfig.align">
                    <el-radio-button label="left">居左</el-radio-button>
                    <el-radio-button label="center">居中</el-radio-button>
                    <el-radio-button label="right">居右</el-radio-button>
                  </el-radio-group>
                </el-form-item>
                <template v-if="selectedItemConfig && !selectedSpacerConfig">
                  <el-form-item label="宽度模式">
                    <el-radio-group v-model="selectedItemConfig.widthMode">
                      <el-radio-button label="fixed">固定格数</el-radio-button>
                      <el-radio-button label="auto">自动伸缩</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item v-if="selectedItemConfig.widthMode === 'fixed'" label="占用格数">
                    <el-slider v-model="selectedItemConfig.span" :min="1" :max="12" show-input />
                  </el-form-item>
                  <small class="art-property-hint">每行共 12 格；自动伸缩组件平分固定组件之外的剩余格数。</small>
                </template>
                <template v-else-if="selectedSpacerConfig">
                  <el-form-item label="空白模式">
                    <el-tag :type="selectedSpacerConfig.type === 'fixedSpacer' ? 'warning' : 'info'">
                      {{ selectedSpacerConfig.type === 'fixedSpacer' ? '固定格数' : '自动伸缩' }}
                    </el-tag>
                  </el-form-item>
                  <el-form-item v-if="selectedSpacerConfig.type === 'fixedSpacer'" label="占用格数">
                    <el-slider v-model="selectedSpacerConfig.span" :min="1" :max="12" show-input />
                  </el-form-item>
                  <el-form-item v-else label="状态宽度保护">
                    <el-switch v-model="selectedSpacerConfig.allowStatusBorrow" active-text="允许左侧状态 Tab 借用" />
                  </el-form-item>
                  <small class="art-property-hint">
                    {{
                      selectedSpacerConfig.type === 'fixedSpacer'
                        ? '该格数只作用于当前固定空白实例。'
                        : selectedSpacerConfig.allowStatusBorrow
                          ? '仅当紧邻左侧是状态 Tab 时生效；运行页把该空白格数并入状态区域，右侧组件位置不变。'
                          : '多个自动空白与其他自动组件平分固定组件之外的剩余格数；当前空白不会被状态 Tab 借用。'
                    }}
                  </small>
                </template>
                <el-form-item label="同一行组件间隔">
                  <el-slider v-model="pageConfig.componentGap" :min="0" :max="32" show-input />
                </el-form-item>
                <template v-if="selectedItemKey === 'title'">
                  <el-form-item label="大标题模板">
                    <el-input v-model="pageConfig.titleTemplate" maxlength="120" show-word-limit />
                    <small class="art-property-hint">
                      支持变量：{activityName}<template v-if="activePage === 'project'">、{categoryName}</template>
                    </small>
                  </el-form-item>
                  <div class="art-header-metrics__grid">
                    <el-form-item label="标题字号">
                      <el-input-number v-model="pageConfig.titleFontSize" :min="14" :max="42" />
                    </el-form-item>
                    <el-form-item label="标题字重">
                      <el-select v-model="pageConfig.titleFontWeight">
                        <el-option label="常规 400" :value="400" />
                        <el-option label="中等 500" :value="500" />
                        <el-option label="半粗 600" :value="600" />
                        <el-option label="加粗 700" :value="700" />
                        <el-option label="特粗 800" :value="800" />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="标题颜色"><el-color-picker v-model="pageConfig.titleColor" /></el-form-item>
                  </div>
                </template>
                <template v-else-if="selectedItemKey === 'status'">
                  <el-alert
                    v-if="schoolSubmitMode"
                    class="mb-3"
                    type="info"
                    :closable="false"
                    title="统一提交的状态业务名称与表格共用全局配置；当前场景只调整状态组件的布局、显示方式和折叠策略。"
                  />
                  <el-form-item label="空间不足自动切换下拉">
                    <el-switch v-model="pageConfig.statusCollapseOnOverflow" active-text="已启用" />
                  </el-form-item>
                  <small class="art-property-hint">状态 Tab 使用当前实际宽度判断；放不下时显示当前状态下拉，变宽后自动恢复 Tab。</small>
                  <el-divider content-position="left">状态显示与顺序</el-divider>
                  <el-table :data="pageConfig.statusItems" size="small" border>
                    <el-table-column label="显示" width="72" align="center">
                      <template #default="{ row }"><el-switch v-model="row.visible" /></template>
                    </el-table-column>
                    <el-table-column label="状态" min-width="120">
                      <template #default="{ row }">{{ statusItemLabel(row.key) }}</template>
                    </el-table-column>
                    <el-table-column label="顺序" width="110" align="center">
                      <template #default="{ row }"><el-input-number v-model="row.order" :min="1" :max="99" controls-position="right" /></template>
                    </el-table-column>
                  </el-table>
                </template>
                <template v-else-if="selectedItemKey === 'navigatorToggle'">
                  <el-form-item label="左侧筛选"><el-switch v-model="pageConfig.sidebarEnabled" active-text="允许展开" /></el-form-item>
                  <el-form-item v-if="pageConfig.sidebarEnabled" label="默认展开">
                    <el-switch v-model="pageConfig.sidebarDefaultExpanded" />
                  </el-form-item>
                  <el-form-item label="展开时文字（可留空）"
                    ><el-input v-model="pageConfig.navigatorToggleButton.text" maxlength="30"
                  /></el-form-item>
                  <el-form-item label="收起时文字（可留空）"
                    ><el-input v-model="pageConfig.navigatorToggleButton.alternateText" maxlength="30"
                  /></el-form-item>
                  <el-form-item label="展开提示语"><el-input v-model="pageConfig.navigatorToggleButton.tooltip" maxlength="50" /></el-form-item>
                  <el-form-item label="收起提示语"
                    ><el-input v-model="pageConfig.navigatorToggleButton.alternateTooltip" maxlength="50"
                  /></el-form-item>
                </template>
                <template v-else-if="selectedItemKey === 'columnWidthReset'">
                  <el-form-item label="按钮文字（可留空）"><el-input v-model="pageConfig.columnWidthResetButton.text" maxlength="30" /></el-form-item>
                  <el-form-item label="鼠标悬停提示语"><el-input v-model="pageConfig.columnWidthResetButton.tooltip" maxlength="50" /></el-form-item>
                </template>
                <template v-else-if="selectedItemKey === 'selectAll'">
                  <el-alert
                    class="mb-3"
                    type="info"
                    :closable="false"
                    show-icon
                    title="这是同一个双状态按钮：未全选时显示“全选”，全部选中后显示“全取消”；下方可继续设置边框和悬停外观。"
                  />
                  <el-form-item label="未全选文字（可留空）"><el-input v-model="pageConfig.selectAllButton.text" maxlength="30" /></el-form-item>
                  <el-form-item label="已全选文字（可留空）"
                    ><el-input v-model="pageConfig.selectAllButton.alternateText" maxlength="30"
                  /></el-form-item>
                  <el-form-item label="未全选提示语"><el-input v-model="pageConfig.selectAllButton.tooltip" maxlength="50" /></el-form-item>
                  <el-form-item label="已全选提示语"><el-input v-model="pageConfig.selectAllButton.alternateTooltip" maxlength="50" /></el-form-item>
                </template>
                <template v-else-if="selectedItemKey === 'unifiedSubmit'">
                  <el-form-item label="按钮文字（可留空）"><el-input v-model="pageConfig.unifiedSubmitButton.text" maxlength="30" /></el-form-item>
                  <el-form-item label="鼠标悬停提示语"><el-input v-model="pageConfig.unifiedSubmitButton.tooltip" maxlength="50" /></el-form-item>
                </template>
                <template v-else-if="selectedItemKey === 'navigationButton'">
                  <el-form-item label="按钮文字（可留空）"><el-input v-model="pageConfig.navigationButton.text" maxlength="30" /></el-form-item>
                  <el-form-item label="鼠标悬停提示语"><el-input v-model="pageConfig.navigationButton.tooltip" maxlength="50" /></el-form-item>
                  <el-form-item label="跳转模式">
                    <el-radio-group v-model="pageConfig.navigationButton.targetMode">
                      <el-radio-button label="preset">角色预设</el-radio-button>
                      <el-radio-button label="custom">自定义链接</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item v-if="pageConfig.navigationButton.targetMode === 'preset'" label="预设目标">
                    <el-select v-model="pageConfig.navigationButton.presetKey">
                      <el-option v-for="item in navigationPresetOptions" :key="item.key" :label="item.label" :value="item.key" />
                    </el-select>
                  </el-form-item>
                  <el-form-item v-else label="自定义链接">
                    <el-input v-model="pageConfig.navigationButton.customUrl" placeholder="/站内路径，或 https:// 外部地址" maxlength="500" />
                    <small class="art-property-hint">只允许以 / 开头的站内路径，以及 http/https 外部链接。</small>
                  </el-form-item>
                  <el-form-item label="打开方式">
                    <el-radio-group v-model="pageConfig.navigationButton.openMode">
                      <el-radio-button label="current">当前页</el-radio-button>
                      <el-radio-button label="new">新窗口</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                </template>
                <template v-else-if="selectedItemKey === 'home'">
                  <el-form-item label="按钮名称"><el-input v-model="pageConfig.homeButton.text" maxlength="30" /></el-form-item>
                  <el-form-item label="鼠标悬停提示语"><el-input v-model="pageConfig.homeButton.tooltip" maxlength="50" /></el-form-item>
                </template>
                <template v-else-if="selectedItemKey === 'reviewScope' && activePage === 'review'">
                  <el-form-item label="显示评审范围">
                    <el-switch v-model="businessModel.reviewWorkbench.scopeNavigator.visible" />
                  </el-form-item>
                  <el-form-item label="显示形式">
                    <el-radio-group v-model="businessModel.reviewWorkbench.scopeNavigator.displayMode">
                      <el-radio-button label="buttons">切换按钮</el-radio-button>
                      <el-radio-button label="select">下拉选择</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item label="显示完成数量">
                    <el-switch v-model="businessModel.reviewWorkbench.scopeNavigator.showCount" />
                  </el-form-item>
                  <el-form-item label="显示进度条">
                    <el-switch v-model="businessModel.reviewWorkbench.scopeNavigator.progressVisible" />
                  </el-form-item>
                  <el-form-item v-if="businessModel.reviewWorkbench.scopeNavigator.progressVisible" label="进度条位置">
                    <el-radio-group v-model="businessModel.reviewWorkbench.scopeNavigator.progressPlacement">
                      <el-radio-button label="inside">按钮内部</el-radio-button>
                      <el-radio-button label="below">组件下方</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                  <el-alert
                    type="info"
                    :closable="false"
                    title="评审范围、类别和组别是三个独立筛选，可同时使用；如需并排显示，可拖入同一个紧凑组件组。进度颜色和高度复用评分进度配置。"
                  />
                </template>
                <template v-else-if="selectedItemKey === 'progress' && schoolSubmitMode && schoolSubmitProgress">
                  <el-form-item label="显示审核总进度"><el-switch v-model="schoolSubmitProgress.visible" /></el-form-item>
                  <el-form-item label="进度标题"><el-input v-model="schoolSubmitProgress.title" maxlength="20" /></el-form-item>
                  <el-form-item label="统计模板"><el-input v-model="schoolSubmitProgress.template" maxlength="80" /></el-form-item>
                  <el-form-item label="进度条宽度">
                    <el-slider v-model="schoolSubmitProgress.width" :min="120" :max="480" show-input />
                  </el-form-item>
                  <el-form-item label="进度条高度">
                    <el-slider v-model="schoolSubmitProgress.height" :min="4" :max="20" show-input />
                  </el-form-item>
                  <el-form-item label="进行中颜色"><el-color-picker v-model="schoolSubmitProgress.activeColor" /></el-form-item>
                  <el-form-item label="完成颜色"><el-color-picker v-model="schoolSubmitProgress.completeColor" /></el-form-item>
                  <el-form-item label="轨道颜色"><el-color-picker v-model="schoolSubmitProgress.trackColor" /></el-form-item>
                  <el-form-item label="文字颜色"><el-color-picker v-model="schoolSubmitProgress.textColor" /></el-form-item>
                  <el-form-item label="文字大小">
                    <el-slider v-model="schoolSubmitProgress.fontSize" :min="12" :max="20" show-input />
                  </el-form-item>
                </template>
                <template v-else-if="selectedItemKey === 'progress' && schoolSubmitMode">
                  <el-alert
                    type="info"
                    :closable="false"
                    title="这里仅调整审核进度组件的位置、宽度和对齐；文字、颜色和尺寸请在统一提交配置的“共享审核进度”中维护。"
                  />
                </template>
                <template v-else-if="selectedItemKey === 'progress' && activePage === 'audit'">
                  <el-form-item label="显示审核进度"><el-switch v-model="businessModel.auditProgress.visible" /></el-form-item>
                  <el-form-item label="进度标题"><el-input v-model="businessModel.auditProgress.title" maxlength="20" /></el-form-item>
                  <el-form-item label="统计模板"><el-input v-model="businessModel.auditProgress.template" maxlength="80" /></el-form-item>
                  <el-form-item label="进度条宽度">
                    <el-slider v-model="businessModel.auditProgress.width" :min="120" :max="480" show-input />
                  </el-form-item>
                  <el-form-item label="进行中颜色"><el-color-picker v-model="businessModel.auditProgress.activeColor" /></el-form-item>
                  <el-form-item label="完成颜色"><el-color-picker v-model="businessModel.auditProgress.completeColor" /></el-form-item>
                </template>
                <template v-else-if="selectedItemKey === 'progress' && activePage === 'review'">
                  <el-form-item label="显示评分进度"><el-switch v-model="businessModel.reviewWorkbench.progress.visible" /></el-form-item>
                  <el-form-item label="统计模板"><el-input v-model="businessModel.reviewWorkbench.progress.template" maxlength="120" /></el-form-item>
                  <el-form-item label="显示百分比"><el-switch v-model="businessModel.reviewWorkbench.progress.showPercentage" /></el-form-item>
                  <el-form-item label="显示剩余数量"><el-switch v-model="businessModel.reviewWorkbench.progress.showRemaining" /></el-form-item>
                  <el-form-item label="进度区域最小宽度">
                    <el-slider v-model="businessModel.reviewWorkbench.progress.width" :min="120" :max="480" show-input />
                  </el-form-item>
                  <el-form-item label="进度条高度">
                    <el-slider v-model="businessModel.reviewWorkbench.progress.height" :min="4" :max="20" show-input />
                  </el-form-item>
                  <el-form-item label="进行中颜色"><el-color-picker v-model="businessModel.reviewWorkbench.progress.activeColor" /></el-form-item>
                  <el-form-item label="完成颜色"><el-color-picker v-model="businessModel.reviewWorkbench.progress.successColor" /></el-form-item>
                </template>
                <template v-if="selectedTextFields.length && selectedItemConfig">
                  <el-divider content-position="left">显示文字</el-divider>
                  <el-form-item v-for="field in selectedTextFields" :key="field.key" :label="field.label">
                    <el-input v-model="selectedItemConfig.texts[field.key]" :maxlength="field.maxlength || 80" show-word-limit />
                  </el-form-item>
                </template>
                <template v-if="selectedItemKey === 'actions' && activePage === 'review'">
                  <el-form-item label="未选择类别提示文字">
                    <el-input v-model="businessModel.reviewWorkbench.signature.hintText" maxlength="80" show-word-limit />
                  </el-form-item>
                  <el-form-item label="签名按钮文字">
                    <el-input v-model="businessModel.reviewWorkbench.signature.buttonText" maxlength="30" show-word-limit />
                  </el-form-item>
                </template>
                <template v-if="selectedButtonConfig">
                  <el-divider content-position="left">按钮布局</el-divider>
                  <template v-if="['selectAll', 'unifiedSubmit', 'navigationButton', 'home'].includes(selectedItemKey || '')">
                    <el-form-item label="显示图标"><el-switch v-model="selectedButtonConfig.iconVisible" /></el-form-item>
                  </template>
                  <template v-if="selectedItemKey === 'selectAll' || selectedItemKey === 'unifiedSubmit' || selectedItemKey === 'navigationButton'">
                    <el-form-item label="按钮样式">
                      <el-radio-group v-model="selectedButtonConfig.variant">
                        <el-radio-button label="default">普通</el-radio-button>
                        <el-radio-button label="plain">朴素</el-radio-button>
                        <el-radio-button label="text">文字</el-radio-button>
                      </el-radio-group>
                    </el-form-item>
                    <el-form-item
                      v-if="selectedItemKey !== 'navigationButton' && (selectedItemKey !== 'unifiedSubmit' || schoolSubmitScene !== 'home')"
                      label="显示已选数量"
                    >
                      <el-switch v-model="selectedButtonConfig.showCount" />
                    </el-form-item>
                    <el-alert
                      v-else-if="selectedItemKey === 'navigationButton'"
                      type="info"
                      :closable="false"
                      title="跳转按钮只负责导航，不携带当前列表选择，因此不显示已选数量。"
                    />
                    <el-alert v-else type="info" :closable="false" title="首页不直接执行批量提交，因此不显示已选数量。" />
                  </template>
                  <template
                    v-if="
                      ['selectAll', 'unifiedSubmit', 'navigationButton', 'home'].includes(selectedItemKey || '') && selectedButtonConfig.appearance
                    "
                  >
                    <el-divider content-position="left">按钮外观</el-divider>
                    <el-form-item label="背景颜色"><el-color-picker v-model="selectedButtonConfig.appearance.backgroundColor" /></el-form-item>
                    <el-form-item label="背景透明度">
                      <el-slider v-model="selectedButtonConfig.appearance.backgroundOpacity" :min="0" :max="100" show-input />
                    </el-form-item>
                    <el-form-item label="文字与图标颜色"><el-color-picker v-model="selectedButtonConfig.appearance.textColor" /></el-form-item>
                    <el-form-item label="显示边框"><el-switch v-model="selectedButtonConfig.appearance.borderVisible" /></el-form-item>
                    <template v-if="selectedButtonConfig.appearance.borderVisible">
                      <el-form-item label="边框颜色"><el-color-picker v-model="selectedButtonConfig.appearance.borderColor" /></el-form-item>
                      <el-form-item label="边框透明度">
                        <el-slider v-model="selectedButtonConfig.appearance.borderOpacity" :min="0" :max="100" show-input />
                      </el-form-item>
                      <el-form-item label="边框宽度">
                        <el-slider v-model="selectedButtonConfig.appearance.borderWidth" :min="0" :max="6" show-input />
                      </el-form-item>
                    </template>
                    <el-form-item label="圆角">
                      <el-slider v-model="selectedButtonConfig.appearance.borderRadius" :min="0" :max="24" show-input />
                    </el-form-item>
                    <el-form-item label="悬停背景"><el-color-picker v-model="selectedButtonConfig.appearance.hoverBackgroundColor" /></el-form-item>
                    <el-form-item label="悬停背景透明度">
                      <el-slider v-model="selectedButtonConfig.appearance.hoverBackgroundOpacity" :min="0" :max="100" show-input />
                    </el-form-item>
                    <el-form-item label="悬停文字颜色"><el-color-picker v-model="selectedButtonConfig.appearance.hoverTextColor" /></el-form-item>
                    <el-form-item v-if="selectedButtonConfig.appearance.borderVisible" label="悬停边框颜色">
                      <el-color-picker v-model="selectedButtonConfig.appearance.hoverBorderColor" />
                    </el-form-item>
                    <el-form-item v-if="selectedButtonConfig.appearance.borderVisible" label="悬停边框透明度">
                      <el-slider v-model="selectedButtonConfig.appearance.hoverBorderOpacity" :min="0" :max="100" show-input />
                    </el-form-item>
                  </template>
                  <el-form-item label="统一字体大小">
                    <el-slider v-model="pageConfig.buttonStyle.fontSize" :min="12" :max="18" show-input />
                  </el-form-item>
                  <el-form-item label="统一字重">
                    <el-select v-model="pageConfig.buttonStyle.fontWeight">
                      <el-option label="常规 400" :value="400" />
                      <el-option label="中等 500" :value="500" />
                      <el-option label="半粗 600" :value="600" />
                      <el-option label="加粗 700" :value="700" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="统一图标大小">
                    <el-slider v-model="pageConfig.buttonStyle.iconSize" :min="12" :max="24" show-input />
                  </el-form-item>
                  <el-form-item label="统一按钮高度">
                    <el-slider v-model="pageConfig.buttonStyle.height" :min="24" :max="44" show-input />
                  </el-form-item>
                  <small class="art-property-hint">文字留空时运行页仅显示图标，并使用提示语作为悬停说明和无障碍名称。</small>
                </template>
                <el-alert
                  v-else-if="
                    !selectedSpacerConfig &&
                    !selectedCompactGroup &&
                    !selectedTextFields.length &&
                    !['title', 'status', 'progress'].includes(selectedItemKey)
                  "
                  type="info"
                  :closable="false"
                  title="该组件的按钮、查询和权限由对应业务页面提供；这里仅控制显示位置和整体显隐。"
                />
                <el-button plain icon="RefreshLeft" @click="restoreSelectedItemDefaults">恢复本组件默认</el-button>
                <el-button v-if="selectedItemRemovable" type="danger" plain icon="Delete" @click="removeSelectedItem">从画布移除组件</el-button>
                <el-button v-else disabled plain icon="Lock">固定组件不可删除</el-button>
              </el-form>
            </template>
            <el-empty v-else :image-size="72" description="点击画布中的组件或卡片" />
          </aside>
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="!schoolSubmitMode && activePage !== 'project'" label="筛选导航" name="navigator">
        <div class="art-preview-settings">
          <div class="art-preview-stage">
            <header><strong>筛选导航预览</strong><small>拖动字段调整顺序，点击字段编辑名称</small></header>
            <template v-if="navigatorPageConfig">
              <div class="art-navigator-preview" :class="`is-${navigatorPageConfig.layout}`" :style="navigatorPreviewStyle">
                <strong>筛选项目</strong>
                <button
                  v-for="(fieldKey, index) in navigatorPageConfig.fields"
                  :key="fieldKey"
                  type="button"
                  class="art-navigator-preview__item"
                  draggable="true"
                  @dragstart="navDragIndex = index"
                  @dragover.prevent
                  @drop="moveNavigatorField(index)"
                >
                  <el-icon><Rank /></el-icon>
                  <span>{{ navigatorPageConfig.labels[fieldKey] }}</span>
                  <small>12</small>
                </button>
              </div>
            </template>
            <el-alert v-else type="info" :closable="false" title="学校统一提交沿用学校范围内的类别筛选，不开放跨学校导航字段。" />
          </div>
          <aside class="art-visual-panel art-property-panel">
            <header><strong>筛选参数</strong><small>当前页面</small></header>
            <template v-if="navigatorPageConfig">
              <el-form label-position="top">
                <el-form-item label="展示方式">
                  <el-radio-group v-model="navigatorPageConfig.layout">
                    <el-radio-button label="sidebar">左侧卡片</el-radio-button>
                    <el-radio-button label="top">顶部长条</el-radio-button>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="筛选字段">
                  <el-checkbox-group v-model="navigatorPageConfig.fields" class="art-property-checkboxes">
                    <el-checkbox v-for="field in allowedNavigatorFields" :key="field.key" :label="field.key">{{ field.label }}</el-checkbox>
                  </el-checkbox-group>
                </el-form-item>
                <el-form-item v-for="fieldKey in navigatorPageConfig.fields" :key="fieldKey" :label="`${fieldLabel(fieldKey)}名称`">
                  <el-input v-model="navigatorPageConfig.labels[fieldKey]" maxlength="20" />
                </el-form-item>
                <el-divider>公共外观</el-divider>
                <el-form-item label="侧栏宽度"
                  ><el-slider v-model="businessModel.navigator.sidebarWidth" :min="260" :max="460" show-input
                /></el-form-item>
                <el-form-item label="侧栏最大高度">
                  <el-slider v-model="businessModel.navigator.sidebarMaxHeight" :min="320" :max="960" show-input />
                </el-form-item>
                <el-form-item label="顶部长条高度"
                  ><el-slider v-model="businessModel.navigator.topHeight" :min="56" :max="180" show-input
                /></el-form-item>
                <el-form-item label="列表行高"><el-slider v-model="businessModel.navigator.rowHeight" :min="30" :max="60" show-input /></el-form-item>
                <el-form-item label="列表间距"><el-slider v-model="businessModel.navigator.itemGap" :min="0" :max="16" show-input /></el-form-item>
                <el-form-item label="卡片内边距"
                  ><el-slider v-model="businessModel.navigator.panelPadding" :min="8" :max="24" show-input
                /></el-form-item>
                <el-form-item label="列表字号"
                  ><el-slider v-model="businessModel.navigator.itemFontSize" :min="12" :max="20" show-input
                /></el-form-item>
                <el-form-item label="背景"><el-color-picker v-model="businessModel.navigator.backgroundColor" /></el-form-item>
                <el-form-item label="背景透明度">
                  <el-slider v-model="businessModel.navigator.backgroundOpacity" :min="0" :max="100" show-input />
                </el-form-item>
                <el-form-item label="悬停颜色"><el-color-picker v-model="businessModel.navigator.hoverBackground" /></el-form-item>
                <el-form-item label="悬停透明度">
                  <el-slider v-model="businessModel.navigator.hoverBackgroundOpacity" :min="0" :max="100" show-input />
                </el-form-item>
                <el-form-item label="选中颜色"><el-color-picker v-model="businessModel.navigator.selectedBackground" /></el-form-item>
                <el-form-item label="选中透明度">
                  <el-slider v-model="businessModel.navigator.selectedBackgroundOpacity" :min="0" :max="100" show-input />
                </el-form-item>
                <el-form-item label="显示行间横线"><el-switch v-model="businessModel.navigator.dividerVisible" /></el-form-item>
                <el-form-item label="横线颜色"><el-color-picker v-model="businessModel.navigator.dividerColor" /></el-form-item>
                <el-form-item label="横线透明度">
                  <el-slider v-model="businessModel.navigator.dividerOpacity" :min="0" :max="100" show-input />
                </el-form-item>
                <el-form-item label="显示外框"><el-switch v-model="businessModel.navigator.borderVisible" /></el-form-item>
                <el-form-item label="显示阴影"><el-switch v-model="businessModel.navigator.shadowVisible" /></el-form-item>
              </el-form>
            </template>
          </aside>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup lang="ts">
import type {
  ArtAuditProgressConfig,
  ArtBrowseNavigatorFieldKey,
  ArtDetailDisplayConfigVO,
  ArtWorkspaceHeaderCardConfig,
  ArtWorkspaceHeaderCompactGroupInstanceConfig,
  ArtWorkspaceHeaderItemKey,
  ArtWorkspaceHeaderPageConfig,
  ArtWorkspaceHeaderPageKey,
  ArtWorkspaceHeaderRepeatableKey,
  ArtWorkspaceHeaderSpacerKey
} from '@/api/crehn/detailDisplay';
import {
  createWorkspaceHeaderSpacerInstance,
  createWorkspaceHeaderCompactGroupInstance,
  defaultSchoolSubmitHomeHeader,
  defaultSchoolSubmitMaximizedHeader,
  defaultWorkspaceHeaderPage,
  defaultSchoolSubmitProgress,
  isWorkspaceHeaderSpacerKey,
  isWorkspaceHeaderRepeatableKey,
  navigatorFieldOptions,
  normalizeArtDetailDisplayConfig,
  resolveWorkspaceHeaderGridSpans,
  workspaceHeaderCollapsibleButtonKeys,
  workspaceHeaderLayoutItemKey,
  workspaceHeaderItemOptions,
  workspaceHeaderPageOptions
} from './artDetailDisplayConfig';
import {
  adjustWorkspaceHeaderInsertIndex,
  resolveWorkspaceHeaderOuterDropTarget,
  shouldDropWorkspaceHeaderItemOutsideCompactGroup,
  type WorkspaceHeaderOuterDropEdge
} from './artWorkspaceHeaderDrag';
import { defaultWorkspaceNavigationPreset, workspaceNavigationPresetsForRole } from './artWorkspaceNavigation';

const props = withDefaults(
  defineProps<{
    mode?: 'business' | 'schoolSubmit';
    pageKey?: ArtWorkspaceHeaderPageKey;
    compact?: boolean;
    schoolSubmitScene?: 'home' | 'maximized' | 'standalone';
    roleKey?: string;
  }>(),
  {
    mode: 'business',
    compact: false,
    schoolSubmitScene: 'standalone'
  }
);
const model = defineModel<ArtDetailDisplayConfigVO>();
const standalonePageConfig = defineModel<ArtWorkspaceHeaderPageConfig>('pageConfig');
const schoolSubmitProgress = defineModel<ArtAuditProgressConfig>('schoolSubmitProgress');
const schoolSubmitMode = computed(() => props.mode === 'schoolSubmit');
const schoolSubmitScene = computed(() => props.schoolSubmitScene);
const schoolSubmitSceneTitle = computed(() =>
  schoolSubmitScene.value === 'home' ? '统一提交首页组件表头' : schoolSubmitScene.value === 'maximized' ? '统一提交最大化表头' : '统一提交独立页表头'
);
const schoolSubmitSceneDescription = computed(() =>
  schoolSubmitScene.value === 'home'
    ? '配置普通首页组件的按钮、筛选与排列'
    : schoolSubmitScene.value === 'maximized'
      ? '配置首页组件最大化后的按钮、筛选与排列'
      : '配置独立统一提交页面的按钮、筛选与排列'
);
const compact = computed(() => props.compact);
const pageKey = computed(() => props.pageKey);
const businessModel = computed(() => model.value as ArtDetailDisplayConfigVO);
const activePage = ref<ArtWorkspaceHeaderPageKey>(schoolSubmitMode.value ? 'schoolSubmit' : props.pageKey || 'project');
const activeSection = ref<'layout' | 'navigator'>('layout');
const navDragIndex = ref<number>();
const cardDragIndex = ref<number>();

type ItemDragSource =
  | { kind: 'palette'; itemKey: ArtWorkspaceHeaderItemKey }
  | { kind: 'canvas'; cardIndex: number; rowIndex: number; itemIndex: number; itemId: string; itemKey: ArtWorkspaceHeaderItemKey }
  | { kind: 'groupChild'; groupId: string; itemIndex: number; itemId: string; itemKey: ArtWorkspaceHeaderItemKey };
type ItemDropTarget = {
  cardIndex: number;
  rowIndex: number;
  itemIndex?: number;
  itemId?: string;
  edge?: WorkspaceHeaderOuterDropEdge;
};
type CanvasSelection =
  | { kind: 'none' }
  | { kind: 'card'; cardKey: string }
  | { kind: 'item'; cardKey: string; itemId: string; itemKey: ArtWorkspaceHeaderItemKey };

const dragSource = ref<ItemDragSource>();
const itemDropTarget = ref<ItemDropTarget>();
const compactGroupInnerDropTarget = ref<string>();
const selection = ref<CanvasSelection>({ kind: 'none' });
const WORKSPACE_ITEM_DRAG_MIME = 'application/x-crehn-workspace-header-item';
const WORKSPACE_CARD_DRAG_MIME = 'application/x-crehn-workspace-header-card';
const pageSegmentOptions = workspaceHeaderPageOptions
  .filter((item) => item.key !== 'schoolSubmit')
  .map((item) => ({ label: item.label, value: item.key }));
const pageConfig = computed(() =>
  schoolSubmitMode.value ? (standalonePageConfig.value as ArtWorkspaceHeaderPageConfig) : businessModel.value.workspaceHeader.pages[activePage.value]
);
const navigatorPageConfig = computed(() => {
  if (activePage.value === 'schoolSubmit') return undefined;
  const key = activePage.value === 'review' ? 'review' : activePage.value === 'projectView' ? 'projectView' : 'audit';
  return businessModel.value.navigator.pages[key];
});
const allowedNavigatorFields = computed(() =>
  navigatorFieldOptions.filter(
    (field) =>
      (activePage.value !== 'review' || field.key !== 'school') &&
      (activePage.value !== 'audit' || (field.key !== 'activity' && field.key !== 'programForm'))
  )
);
const isRepeatableItem = (itemKey: ArtWorkspaceHeaderItemKey): itemKey is ArtWorkspaceHeaderRepeatableKey => isWorkspaceHeaderRepeatableKey(itemKey);
const configuredItems = computed(
  () =>
    new Set(
      pageConfig.value.cards.flatMap((card) =>
        card.rows.flatMap((row) =>
          row.flatMap((itemId) => {
            const itemKey = workspaceHeaderLayoutItemKey(itemId, pageConfig.value);
            return itemKey === 'compactGroup' ? pageConfig.value.compactGroupInstances[itemId]?.items || [] : [itemKey];
          })
        )
      )
    )
);
const selectedCard = computed(() => {
  const currentSelection = selection.value;
  return currentSelection.kind === 'card' ? pageConfig.value.cards.find((card) => card.key === currentSelection.cardKey) : undefined;
});
const selectedItemKey = computed(() => (selection.value.kind === 'item' ? selection.value.itemKey : undefined));
const selectedItemConfig = computed(() =>
  selectedItemKey.value && selectedItemKey.value !== 'compactGroup' ? pageConfig.value.itemConfigs[selectedItemKey.value] : undefined
);
const selectedSpacerConfig = computed(() => (selection.value.kind === 'item' ? pageConfig.value.spacerInstances[selection.value.itemId] : undefined));
const selectedCompactGroup = computed(() =>
  selection.value.kind === 'item' && selection.value.itemKey === 'compactGroup'
    ? pageConfig.value.compactGroupInstances[selection.value.itemId]
    : undefined
);
const selectedCompactCollapsibleItems = computed(() =>
  workspaceHeaderItemOptions.filter(
    (item) => selectedCompactGroup.value?.items.includes(item.key) && workspaceHeaderCollapsibleButtonKeys.includes(item.key)
  )
);
const selectedItemRemovable = computed(() => {
  const requiredHome =
    ((schoolSubmitMode.value && schoolSubmitScene.value !== 'home') || (!schoolSubmitMode.value && activePage.value === 'schoolSubmit')) &&
    (selectedItemKey.value === 'home' || Boolean(selectedCompactGroup.value?.items.includes('home')));
  return !requiredHome && !(activePage.value === 'review' && selectedItemKey.value === 'selectAll');
});
const selectedButtonConfig = computed(() =>
  selectedItemKey.value === 'navigatorToggle'
    ? pageConfig.value.navigatorToggleButton
    : selectedItemKey.value === 'columnWidthReset'
      ? pageConfig.value.columnWidthResetButton
      : selectedItemKey.value === 'selectAll'
        ? pageConfig.value.selectAllButton
        : selectedItemKey.value === 'unifiedSubmit'
          ? pageConfig.value.unifiedSubmitButton
          : selectedItemKey.value === 'navigationButton'
            ? pageConfig.value.navigationButton
            : selectedItemKey.value === 'home'
              ? pageConfig.value.homeButton
              : undefined
);
const navigationPresetOptions = computed(() => workspaceNavigationPresetsForRole(props.roleKey || 'school'));
watch(
  navigationPresetOptions,
  (items) => {
    if (!items.some((item) => item.key === pageConfig.value.navigationButton.presetKey)) {
      pageConfig.value.navigationButton.presetKey = defaultWorkspaceNavigationPreset(props.roleKey || 'school');
    }
  },
  { immediate: true }
);
const statusItemLabel = (key: string) => pageConfig.value.itemConfigs.status.texts[key] || key;
const selectionTitle = computed(() => {
  if (selectedCard.value) return '长条卡片';
  if (selectedItemKey.value) {
    const label = workspaceHeaderItemOptions.find((item) => item.key === selectedItemKey.value)?.label || '组件';
    if (selection.value.kind === 'item' && selectedSpacerConfig.value) {
      const siblingIds = pageConfig.value.cards
        .flatMap((card) => card.rows.flat())
        .filter((itemId) => pageConfig.value.spacerInstances[itemId]?.type === selectedSpacerConfig.value?.type);
      return `${label} #${Math.max(1, siblingIds.indexOf(selection.value.itemId) + 1)}`;
    }
    if (selection.value.kind === 'item' && selectedCompactGroup.value) {
      const siblingIds = pageConfig.value.cards
        .flatMap((card) => card.rows.flat())
        .filter((itemId) => Boolean(pageConfig.value.compactGroupInstances[itemId]));
      return `${label} #${Math.max(1, siblingIds.indexOf(selection.value.itemId) + 1)}`;
    }
    return label;
  }
  return '尚未选择';
});

const textFieldLabels: Record<string, string> = {
  scopeLabel: '组件名称',
  allScopeLabel: '全部范围文字',
  selectPlaceholder: '下拉占位语',
  categoryLabel: '类别名称',
  categoryPlaceholder: '类别占位语',
  groupLabel: '组别名称',
  groupPlaceholder: '组别占位语',
  schoolLabel: '学校名称',
  schoolPlaceholder: '学校占位语',
  allCategories: '全部类别文字',
  allUnits: '全部单位文字',
  progressSuffix: '路径末尾文字',
  remainingTemplate: '剩余数量提示模板',
  all: '全部状态文字',
  draft: '草稿状态文字',
  none: '未评分状态文字',
  submitted: '提交/签字状态文字',
  audit_passed: '通过状态文字',
  returned: '退回状态文字',
  placeholder: '输入框占位语',
  search: '搜索按钮文字',
  reset: '重置按钮文字',
  settings: '设置按钮文字',
  restoreColumns: '恢复列设置文字',
  auditAssignment: '审核权限按钮文字',
  lockedSignature: '已签评分表按钮文字',
  add: '添加按钮文字',
  tooltip: '鼠标悬停提示语'
};
const selectedTextFields = computed(() => {
  if (schoolSubmitMode.value && selectedItemKey.value === 'status') return [];
  return Object.keys(selectedItemConfig.value?.texts || {})
    .filter((key) => !(selectedItemKey.value === 'reviewScope' && key === 'allScopeLabel'))
    .map((key) => ({
      key,
      label: textFieldLabels[key] || key,
      maxlength: key === 'progressSuffix' ? 30 : 50
    }));
});

const paletteDescriptions: Record<ArtWorkspaceHeaderItemKey, string> = {
  title: '活动标题',
  navigatorToggle: '展开筛选',
  filters: '上报路径摘要',
  reviewScope: '按评委分配范围切换，支持按钮或下拉',
  categoryFilter: '类别下拉筛选',
  groupFilter: '组别下拉筛选',
  schoolFilter: '学校下拉筛选',
  progress: '审核/评分进度',
  status: '项目状态',
  search: '名称搜索',
  actions: '业务按钮',
  columnWidthReset: '恢复列宽',
  selectAll: '选择项目',
  unifiedSubmit: '批量提交',
  navigationButton: '跳转到角色页面或自定义链接',
  home: schoolSubmitScene.value === 'home' ? '最大化组件' : schoolSubmitScene.value === 'maximized' ? '退出最大化' : '返回首页',
  compactGroup: '组内组件保持内容宽度和固定间隔',
  fixedSpacer: '占用指定栅格的透明空白',
  autoSpacer: '自动吸收当前行剩余空间'
};
const allPaletteGroups = [
  { label: '内容', keys: ['title'] },
  { label: '导航筛选', keys: ['navigatorToggle', 'filters', 'reviewScope', 'categoryFilter', 'groupFilter', 'schoolFilter', 'status'] },
  { label: '数据反馈', keys: ['progress'] },
  { label: '列表操作', keys: ['selectAll', 'unifiedSubmit', 'navigationButton', 'search', 'actions', 'columnWidthReset', 'home'] },
  { label: '布局容器', keys: ['compactGroup', 'fixedSpacer', 'autoSpacer'] }
];
const paletteGroups = computed(() =>
  allPaletteGroups
    .map((group) => ({
      label: group.label,
      items: group.keys
        .map((key) => key as ArtWorkspaceHeaderItemKey)
        .filter((itemKey) => {
          if (isWorkspaceHeaderRepeatableKey(itemKey)) return true;
          if (schoolSubmitMode.value) {
            if (itemKey === 'filters' || itemKey === 'schoolFilter' || itemKey === 'actions') return false;
            if (schoolSubmitScene.value === 'home' && ['navigatorToggle', 'selectAll', 'unifiedSubmit'].includes(itemKey)) return false;
            return true;
          }
          if (activePage.value === 'project') return ['title', 'actions'].includes(itemKey);
          if (itemKey === 'filters') return activePage.value === 'projectView';
          if (itemKey === 'reviewScope') return activePage.value === 'review';
          if (itemKey === 'categoryFilter' || itemKey === 'groupFilter') {
            return ['audit', 'review', 'schoolSubmit'].includes(activePage.value);
          }
          if (itemKey === 'schoolFilter') return activePage.value === 'audit';
          if (itemKey === 'home') return activePage.value === 'schoolSubmit';
          if (itemKey === 'navigationButton') return activePage.value === 'schoolSubmit';
          if (['selectAll', 'unifiedSubmit'].includes(itemKey) && !['review', 'schoolSubmit'].includes(activePage.value)) return false;
          return activePage.value !== 'projectView' || itemKey !== 'progress';
        })
        .map((itemKey) => ({
          key: itemKey,
          label: workspaceHeaderItemOptions.find((item) => item.key === itemKey)?.label || itemKey,
          description: paletteDescriptions[itemKey]
        }))
    }))
    .filter((group) => group.items.length)
);

const ensureRows = () => {
  pageConfig.value.cards.forEach((card) => {
    card.rows = [Array.isArray(card.rows[0]) ? card.rows[0] : [], Array.isArray(card.rows[1]) ? card.rows[1] : []];
  });
};
watch(activePage, () => {
  ensureRows();
  selection.value = { kind: 'none' };
  if (activePage.value === 'project') activeSection.value = 'layout';
});
watch(
  () => props.pageKey,
  (value) => {
    if (value && value !== activePage.value) activePage.value = value;
  }
);

const itemPreviewFullText = (itemId: string, key: ArtWorkspaceHeaderItemKey) => {
  const texts = pageConfig.value.itemConfigs[key].texts;
  const labels: Record<ArtWorkspaceHeaderItemKey, string> = {
    title: pageConfig.value.titleTemplate.replace('{activityName}', '当前活动').replace('{categoryName}', '当前类别'),
    navigatorToggle: pageConfig.value.navigatorToggleButton.text || '仅图标',
    filters: Object.values(texts).filter(Boolean).join('　') || '筛选',
    reviewScope:
      businessModel.value.reviewWorkbench.scopeNavigator.displayMode === 'select'
        ? '评审范围　器乐全部组 0/10 ▼'
        : '评审范围　器乐 0/10　声乐 > 甲组 0/20',
    categoryFilter: Object.values(texts).filter(Boolean).join('　') || '类别',
    groupFilter: Object.values(texts).filter(Boolean).join('　') || '组别',
    schoolFilter: Object.values(texts).filter(Boolean).join('　') || '学校',
    progress:
      schoolSubmitMode.value && schoolSubmitProgress.value
        ? `${schoolSubmitProgress.value.title} ${schoolSubmitProgress.value.template
            .replaceAll('{completed}', '8')
            .replaceAll('{total}', '47')
            .replaceAll('{pending}', '39')
            .replaceAll('{percentage}', '17')}`
        : activePage.value === 'review'
          ? '本类别 7/7 已评分 100%'
          : '审核进度 7/9 78%',
    status: Object.values(texts).filter(Boolean).join('　') || '状态',
    search: Object.values(texts).filter(Boolean).join('　') || '搜索',
    actions: Object.values(texts).filter(Boolean).join('　') || (activePage.value === 'review' ? '签名操作' : '页面操作'),
    columnWidthReset: pageConfig.value.columnWidthResetButton.text || '仅图标',
    selectAll: `${pageConfig.value.selectAllButton.text || '仅图标'}${pageConfig.value.selectAllButton.showCount ? '（3）' : ''}`,
    unifiedSubmit: `${pageConfig.value.unifiedSubmitButton.text || '仅图标'}${pageConfig.value.unifiedSubmitButton.showCount ? '（3）' : ''}`,
    navigationButton: pageConfig.value.navigationButton.text || '仅图标',
    home: pageConfig.value.homeButton.text || '首页',
    compactGroup:
      pageConfig.value.compactGroupInstances[itemId]?.items
        .map((childKey) => workspaceHeaderItemOptions.find((item) => item.key === childKey)?.label || childKey)
        .join(' / ') || '空紧凑组',
    fixedSpacer: `固定空白 ${pageConfig.value.spacerInstances[itemId]?.span || 1} 格`,
    autoSpacer: pageConfig.value.spacerInstances[itemId]?.allowStatusBorrow ? '自动空白（允许状态借用）' : '自动空白'
  };
  return labels[key];
};
const compactPreviewSegment = (value: string) => Array.from(value.trim()).slice(0, 2).join('');
const compactTextValues = (texts: Record<string, string>) =>
  Object.values(texts)
    .map((value) => compactPreviewSegment(value))
    .filter(Boolean)
    .join('　');
const compactFilterText = (texts: Record<string, string>) => {
  const labels = [
    Object.keys(texts).some((key) => key.startsWith('category')) ? texts.categoryLabel || '类别' : '',
    Object.keys(texts).some((key) => key.startsWith('group')) ? texts.groupLabel || '组别' : '',
    Object.keys(texts).some((key) => key.startsWith('school')) ? texts.schoolLabel || '学校' : ''
  ].filter(Boolean);
  return labels.join('　') || '筛选';
};
const itemPreviewText = (itemId: string, key: ArtWorkspaceHeaderItemKey) => {
  const texts = pageConfig.value.itemConfigs[key].texts;
  if (key === 'filters') return compactFilterText(texts);
  if (key === 'reviewScope') return texts.scopeLabel || '评审范围';
  if (key === 'categoryFilter') return texts.categoryLabel || '类别';
  if (key === 'groupFilter') return texts.groupLabel || '组别';
  if (key === 'schoolFilter') return texts.schoolLabel || '学校';
  if (key === 'fixedSpacer') return `固定空白 ${pageConfig.value.spacerInstances[itemId]?.span || 1} 格`;
  if (key === 'autoSpacer') return pageConfig.value.spacerInstances[itemId]?.allowStatusBorrow ? '自动空白·状态可借用' : '自动空白';
  if (key === 'compactGroup') return `紧凑组（${pageConfig.value.compactGroupInstances[itemId]?.items.length || 0}）`;
  if (key === 'status' || key === 'search' || key === 'actions') return compactTextValues(texts) || itemPreviewFullText(itemId, key);
  if (key === 'columnWidthReset') return '列宽';
  return itemPreviewFullText(itemId, key);
};
const configurableButtonKeys = new Set<ArtWorkspaceHeaderItemKey>(['navigatorToggle', 'columnWidthReset', 'selectAll', 'unifiedSubmit', 'home']);
const rowPreviewStyle = computed(() => ({
  gap: `${pageConfig.value.componentGap}px`,
  minHeight: `${pageConfig.value.rowMinHeight}px`
}));
const headerPreviewStyle = computed(() => ({
  marginTop: `${pageConfig.value.marginTop}px`,
  gap: `${pageConfig.value.marginBottom}px`
}));
const previewItems = (row: string[] | undefined) =>
  (row || []).map((id, index) => ({ id, key: workspaceHeaderLayoutItemKey(id, pageConfig.value), index }));
const rowGridSpans = (row: string[] | undefined) => resolveWorkspaceHeaderGridSpans(row || [], pageConfig.value);
const rowGridUsage = (row: string[] | undefined) => Object.values(rowGridSpans(row)).reduce((total, span) => total + span, 0);
const rowConfiguredUsage = (row: string[] | undefined) =>
  (row || []).reduce((total, itemId) => {
    const itemKey = workspaceHeaderLayoutItemKey(itemId, pageConfig.value);
    if (itemKey === 'fixedSpacer') return total + (pageConfig.value.spacerInstances[itemId]?.span || 1);
    if (itemKey === 'compactGroup') {
      const group = pageConfig.value.compactGroupInstances[itemId];
      return total + (group?.widthMode === 'auto' ? 1 : group?.span || 4);
    }
    if (itemKey === 'autoSpacer' || pageConfig.value.itemConfigs[itemKey].widthMode === 'auto') return total + 1;
    return total + pageConfig.value.itemConfigs[itemKey].span;
  }, 0);
const rowHasOnlySpacers = (row: string[] | undefined) =>
  Boolean(row?.length) && row!.every((itemId) => isWorkspaceHeaderSpacerKey(workspaceHeaderLayoutItemKey(itemId, pageConfig.value)));
const itemPreviewStyle = (itemId: string, key: ArtWorkspaceHeaderItemKey, row: string[] | undefined) => {
  const group = key === 'compactGroup' ? pageConfig.value.compactGroupInstances[itemId] : undefined;
  const baseStyle = {
    gridColumn: `span ${rowGridSpans(row)[itemId] || 1}`,
    justifyContent:
      (group?.align || pageConfig.value.itemConfigs[key].align) === 'right'
        ? 'flex-end'
        : (group?.align || pageConfig.value.itemConfigs[key].align) === 'center'
          ? 'center'
          : 'flex-start'
  };
  return configurableButtonKeys.has(key)
    ? {
        ...baseStyle,
        ...(() => {
          const appearance =
            key === 'selectAll'
              ? pageConfig.value.selectAllButton.appearance
              : key === 'unifiedSubmit'
                ? pageConfig.value.unifiedSubmitButton.appearance
                : key === 'home'
                  ? pageConfig.value.homeButton.appearance
                  : undefined;
          return {
            minHeight: `${pageConfig.value.buttonStyle.height}px`,
            fontSize: `${pageConfig.value.buttonStyle.fontSize}px`,
            fontWeight: pageConfig.value.buttonStyle.fontWeight,
            ...(appearance
              ? {
                  color: appearance.textColor,
                  backgroundColor: `color-mix(in srgb, ${appearance.backgroundColor} ${appearance.backgroundOpacity}%, transparent)`,
                  borderColor: appearance.borderVisible
                    ? `color-mix(in srgb, ${appearance.borderColor} ${appearance.borderOpacity}%, transparent)`
                    : 'transparent',
                  borderWidth: `${appearance.borderVisible ? appearance.borderWidth : 0}px`,
                  borderRadius: `${appearance.borderRadius}px`,
                  '--preview-button-hover-background': `color-mix(in srgb, ${appearance.hoverBackgroundColor} ${appearance.hoverBackgroundOpacity}%, transparent)`,
                  '--preview-button-hover-text': appearance.hoverTextColor,
                  '--preview-button-hover-border': appearance.borderVisible
                    ? `color-mix(in srgb, ${appearance.hoverBorderColor} ${appearance.hoverBorderOpacity}%, transparent)`
                    : 'transparent'
                }
              : {})
          };
        })()
      }
    : baseStyle;
};
const fieldLabel = (key: ArtBrowseNavigatorFieldKey) => navigatorFieldOptions.find((item) => item.key === key)?.label || key;
const cardPreviewStyle = (card: ArtWorkspaceHeaderCardConfig) => ({
  background: `color-mix(in srgb, ${card.backgroundColor} ${card.backgroundOpacity}%, transparent)`,
  border: card.borderVisible ? '1px solid var(--el-border-color-lighter)' : '1px dashed transparent',
  borderRadius: `${card.borderRadius}px`,
  padding: `${pageConfig.value.paddingTop}px ${pageConfig.value.paddingInline}px ${pageConfig.value.paddingBottom}px`
});
const navigatorPreviewStyle = computed(() => ({
  '--preview-bg': `color-mix(in srgb, ${businessModel.value.navigator.backgroundColor} ${businessModel.value.navigator.backgroundOpacity}%, transparent)`,
  '--preview-hover': `color-mix(in srgb, ${businessModel.value.navigator.hoverBackground} ${businessModel.value.navigator.hoverBackgroundOpacity}%, transparent)`,
  '--preview-selected': `color-mix(in srgb, ${businessModel.value.navigator.selectedBackground} ${businessModel.value.navigator.selectedBackgroundOpacity}%, transparent)`,
  '--preview-row-height': `${businessModel.value.navigator.rowHeight}px`,
  '--preview-width': `${businessModel.value.navigator.sidebarWidth}px`
}));
const selectCard = (cardKey: string) => (selection.value = { kind: 'card', cardKey });
const selectItem = (cardKey: string, itemId: string, itemKey: ArtWorkspaceHeaderItemKey) =>
  (selection.value = { kind: 'item', cardKey, itemId, itemKey });
const isSelectedItem = (cardKey: string, itemId: string) =>
  selection.value.kind === 'item' && selection.value.cardKey === cardKey && selection.value.itemId === itemId;
const selectConfiguredItem = (itemKey: ArtWorkspaceHeaderItemKey) => {
  if (isWorkspaceHeaderRepeatableKey(itemKey)) return;
  const card = pageConfig.value.cards.find((item) =>
    item.rows.some((row) =>
      row.some((itemId) => {
        const layoutKey = workspaceHeaderLayoutItemKey(itemId, pageConfig.value);
        return layoutKey === itemKey || (layoutKey === 'compactGroup' && pageConfig.value.compactGroupInstances[itemId]?.items.includes(itemKey));
      })
    )
  );
  if (card) selectItem(card.key, itemKey, itemKey);
};

const isWorkspaceItemKey = (value: unknown): value is ArtWorkspaceHeaderItemKey =>
  typeof value === 'string' && workspaceHeaderItemOptions.some((item) => item.key === value);
const isItemDragSource = (value: unknown): value is ItemDragSource => {
  if (!value || typeof value !== 'object') return false;
  const source = value as {
    kind?: string;
    itemKey?: unknown;
    cardIndex?: unknown;
    rowIndex?: unknown;
    itemIndex?: unknown;
    itemId?: unknown;
    groupId?: unknown;
  };
  if (!isWorkspaceItemKey(source.itemKey)) return false;
  if (source.kind === 'palette') return true;
  const isIndex = (candidate: unknown): candidate is number => typeof candidate === 'number' && Number.isInteger(candidate) && candidate >= 0;
  if (source.kind === 'canvas') {
    return isIndex(source.cardIndex) && isIndex(source.rowIndex) && isIndex(source.itemIndex) && typeof source.itemId === 'string';
  }
  return source.kind === 'groupChild' && typeof source.groupId === 'string' && isIndex(source.itemIndex) && typeof source.itemId === 'string';
};
const writeItemDragPayload = (event: DragEvent, source: ItemDragSource) => {
  dragSource.value = source;
  itemDropTarget.value = undefined;
  if (!event.dataTransfer) return;
  const payload = JSON.stringify(source);
  event.dataTransfer.effectAllowed = 'move';
  event.dataTransfer.setData(WORKSPACE_ITEM_DRAG_MIME, payload);
  event.dataTransfer.setData('text/plain', payload);
};
const readItemDragPayload = (event: DragEvent) => {
  if (dragSource.value) return dragSource.value;
  const payload = event.dataTransfer?.getData(WORKSPACE_ITEM_DRAG_MIME) || event.dataTransfer?.getData('text/plain');
  if (!payload) return undefined;
  try {
    const parsed = JSON.parse(payload) as unknown;
    return isItemDragSource(parsed) ? parsed : undefined;
  } catch {
    return undefined;
  }
};
const clearDragState = () => {
  dragSource.value = undefined;
  itemDropTarget.value = undefined;
  compactGroupInnerDropTarget.value = undefined;
};
const isItemDropTarget = (cardIndex: number, rowIndex: number) =>
  itemDropTarget.value?.cardIndex === cardIndex && itemDropTarget.value?.rowIndex === rowIndex;
const hasItemDragPayload = (event: DragEvent) =>
  Boolean(dragSource.value) || Array.from(event.dataTransfer?.types || []).some((type) => type === WORKSPACE_ITEM_DRAG_MIME || type === 'text/plain');
const markItemDropTarget = (event: DragEvent, cardIndex: number, rowIndex: number, itemIndex?: number) => {
  if (!hasItemDragPayload(event)) return;
  event.preventDefault();
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move';
  itemDropTarget.value = { cardIndex, rowIndex, itemIndex };
  compactGroupInnerDropTarget.value = undefined;
};
const outerDropTarget = (event: DragEvent, targetItemIndex: number) => {
  const element = event.currentTarget instanceof HTMLElement ? event.currentTarget : undefined;
  const bounds = element?.getBoundingClientRect();
  return resolveWorkspaceHeaderOuterDropTarget(targetItemIndex, event.clientX, bounds?.left || 0, bounds?.width || 0);
};
const outerDropIndex = (event: DragEvent, targetItemIndex: number) => outerDropTarget(event, targetItemIndex).itemIndex;
const markOuterItemDropTarget = (event: DragEvent, cardIndex: number, rowIndex: number, targetItemIndex: number, targetItemId: string) => {
  if (!hasItemDragPayload(event)) return;
  const target = outerDropTarget(event, targetItemIndex);
  event.preventDefault();
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move';
  itemDropTarget.value = {
    cardIndex,
    rowIndex,
    itemIndex: target.itemIndex,
    itemId: targetItemId,
    edge: target.edge
  };
  compactGroupInnerDropTarget.value = undefined;
};
const outerDropEdgeClasses = (cardIndex: number, rowIndex: number, itemId: string) => {
  const target = itemDropTarget.value;
  if (target?.cardIndex !== cardIndex || target.rowIndex !== rowIndex || target.itemId !== itemId) return {};
  return {
    'is-drop-before': target.edge === 'before',
    'is-drop-after': target.edge === 'after'
  };
};
const startPaletteDrag = (event: DragEvent, itemKey: ArtWorkspaceHeaderItemKey) => {
  if (isWorkspaceHeaderRepeatableKey(itemKey) || !configuredItems.value.has(itemKey)) {
    writeItemDragPayload(event, { kind: 'palette', itemKey });
  }
};
const startItemDrag = (
  event: DragEvent,
  cardIndex: number,
  rowIndex: number,
  itemIndex: number,
  itemId: string,
  itemKey: ArtWorkspaceHeaderItemKey
) => writeItemDragPayload(event, { kind: 'canvas', cardIndex, rowIndex, itemIndex, itemId, itemKey });
const startGroupItemDrag = (event: DragEvent, groupId: string, itemIndex: number, itemKey: ArtWorkspaceHeaderItemKey) =>
  writeItemDragPayload(event, { kind: 'groupChild', groupId, itemIndex, itemId: itemKey, itemKey });
const detachDraggedItem = (source: ItemDragSource) => {
  if (source.kind === 'canvas') {
    pageConfig.value.cards[source.cardIndex].rows[source.rowIndex].splice(source.itemIndex, 1);
    return;
  }
  if (source.kind === 'groupChild') {
    const sourceGroup = pageConfig.value.compactGroupInstances[source.groupId];
    sourceGroup?.items.splice(source.itemIndex, 1);
    if (sourceGroup) sourceGroup.pinnedItems = sourceGroup.pinnedItems.filter((item) => sourceGroup.items.includes(item));
  }
};
const dropItem = (event: DragEvent, targetCardIndex: number, targetRowIndex: number, targetItemIndex?: number) => {
  event.preventDefault();
  const source = readItemDragPayload(event);
  if (!source) {
    clearDragState();
    return;
  }
  try {
    const targetRow = pageConfig.value.cards[targetCardIndex].rows[targetRowIndex];
    const movingWithinTarget = source.kind === 'canvas' && pageConfig.value.cards[source.cardIndex].rows[source.rowIndex] === targetRow;
    if (!movingWithinTarget && targetRow.length >= 12) {
      ElMessage.warning('每行最多放置 12 个组件，请先移除或移动现有组件');
      return;
    }
    let insertIndex = targetItemIndex ?? targetRow.length;
    let itemId = source.kind === 'palette' ? source.itemKey : source.itemId;
    if (source.kind === 'canvas') {
      detachDraggedItem(source);
      insertIndex = adjustWorkspaceHeaderInsertIndex(source.itemIndex, insertIndex, movingWithinTarget);
    } else if (source.kind === 'groupChild') {
      detachDraggedItem(source);
    } else if (isWorkspaceHeaderSpacerKey(source.itemKey)) {
      itemId = createWorkspaceHeaderSpacerInstance(pageConfig.value, source.itemKey);
    } else if (source.itemKey === 'compactGroup') {
      itemId = createWorkspaceHeaderCompactGroupInstance(pageConfig.value);
    } else if (configuredItems.value.has(source.itemKey)) {
      return;
    }
    targetRow.splice(Math.max(0, insertIndex), 0, itemId);
    selectItem(pageConfig.value.cards[targetCardIndex].key, itemId, source.itemKey);
  } finally {
    clearDragState();
  }
};
const markCompactGroupDropTarget = (event: DragEvent, cardIndex: number, rowIndex: number, targetItemIndex: number, groupId: string) => {
  if (!hasItemDragPayload(event)) return;
  if (dragSource.value && shouldDropWorkspaceHeaderItemOutsideCompactGroup(dragSource.value.itemKey)) {
    markOuterItemDropTarget(event, cardIndex, rowIndex, targetItemIndex, groupId);
    return;
  }
  event.preventDefault();
  if (event.dataTransfer) event.dataTransfer.dropEffect = 'move';
  itemDropTarget.value = undefined;
  compactGroupInnerDropTarget.value = groupId;
};
const dropItemOnCompactGroup = (event: DragEvent, cardIndex: number, rowIndex: number, targetItemIndex: number, groupId: string) => {
  const source = readItemDragPayload(event);
  if (source && shouldDropWorkspaceHeaderItemOutsideCompactGroup(source.itemKey)) {
    dropItem(event, cardIndex, rowIndex, outerDropIndex(event, targetItemIndex));
    return;
  }
  dropItemIntoGroup(event, groupId);
};
const dropItemIntoGroup = (event: DragEvent, groupId: string) => {
  event.preventDefault();
  const source = readItemDragPayload(event);
  const targetGroup = pageConfig.value.compactGroupInstances[groupId];
  try {
    if (!source || !targetGroup || shouldDropWorkspaceHeaderItemOutsideCompactGroup(source.itemKey)) return;
    if (targetGroup.items.includes(source.itemKey)) return;
    if (targetGroup.items.length >= 8) {
      ElMessage.warning('紧凑组最多放置 8 个组件');
      return;
    }
    if (source.kind === 'palette' && configuredItems.value.has(source.itemKey)) return;
    detachDraggedItem(source);
    targetGroup.items.push(source.itemKey);
    const card = pageConfig.value.cards.find((item) => item.rows.some((row) => row.includes(groupId)));
    if (card) selectItem(card.key, source.itemKey, source.itemKey);
  } finally {
    clearDragState();
  }
};
const removeSelectedItem = () => {
  if (selection.value.kind !== 'item' || !selectedItemRemovable.value) return;
  const selectedItemId = selection.value.itemId;
  pageConfig.value.cards.forEach((card) => {
    card.rows.forEach((row) => {
      const index = row.indexOf(selectedItemId);
      if (index >= 0) row.splice(index, 1);
    });
  });
  Object.values(pageConfig.value.compactGroupInstances).forEach((group) => {
    const index = group.items.indexOf(selectedItemId as ArtWorkspaceHeaderItemKey);
    if (index >= 0) group.items.splice(index, 1);
    group.pinnedItems = group.pinnedItems.filter((item) => group.items.includes(item));
  });
  if (pageConfig.value.spacerInstances[selectedItemId]) delete pageConfig.value.spacerInstances[selectedItemId];
  if (pageConfig.value.compactGroupInstances[selectedItemId]) delete pageConfig.value.compactGroupInstances[selectedItemId];
  selection.value = { kind: 'none' };
};

const restoreSelectedItemDefaults = () => {
  const itemKey = selectedItemKey.value;
  if (!itemKey) return;
  if (selectedSpacerConfig.value) {
    selectedSpacerConfig.value.span = 1;
    selectedSpacerConfig.value.allowStatusBorrow = false;
    return;
  }
  if (selectedCompactGroup.value) {
    selectedCompactGroup.value.widthMode = 'fixed';
    selectedCompactGroup.value.span = 4;
    selectedCompactGroup.value.align = 'right';
    selectedCompactGroup.value.gap = 10;
    selectedCompactGroup.value.collapseMode = 'none';
    selectedCompactGroup.value.pinnedItems = selectedCompactGroup.value.items.includes('unifiedSubmit')
      ? ['unifiedSubmit']
      : selectedCompactGroup.value.items.includes('navigationButton')
        ? ['navigationButton']
        : [];
    selectedCompactGroup.value.collapseText = '更多';
    return;
  }
  const defaults = normalizeArtDetailDisplayConfig();
  const defaultPage = schoolSubmitMode.value
    ? schoolSubmitScene.value === 'home'
      ? defaultSchoolSubmitHomeHeader()
      : schoolSubmitScene.value === 'maximized'
        ? defaultSchoolSubmitMaximizedHeader()
        : defaultWorkspaceHeaderPage('schoolSubmit')
    : defaults.workspaceHeader.pages[activePage.value];
  pageConfig.value.itemConfigs[itemKey] = JSON.parse(JSON.stringify(defaultPage.itemConfigs[itemKey]));
  if (itemKey === 'title') {
    pageConfig.value.titleTemplate = defaultPage.titleTemplate;
    pageConfig.value.titleFontSize = defaultPage.titleFontSize;
    pageConfig.value.titleFontWeight = defaultPage.titleFontWeight;
    pageConfig.value.titleColor = defaultPage.titleColor;
  }
  if (itemKey === 'navigatorToggle') Object.assign(pageConfig.value.navigatorToggleButton, defaultPage.navigatorToggleButton);
  if (itemKey === 'columnWidthReset') Object.assign(pageConfig.value.columnWidthResetButton, defaultPage.columnWidthResetButton);
  if (itemKey === 'selectAll') {
    Object.assign(pageConfig.value.selectAllButton, defaultPage.selectAllButton);
  }
  if (itemKey === 'unifiedSubmit') Object.assign(pageConfig.value.unifiedSubmitButton, defaultPage.unifiedSubmitButton);
  if (itemKey === 'navigationButton') Object.assign(pageConfig.value.navigationButton, defaultPage.navigationButton);
  if (itemKey === 'home') Object.assign(pageConfig.value.homeButton, defaultPage.homeButton);
  if (itemKey === 'status') pageConfig.value.statusItems = defaultPage.statusItems.map((item) => ({ ...item }));
  if (schoolSubmitMode.value && itemKey === 'progress' && schoolSubmitProgress.value) {
    Object.assign(schoolSubmitProgress.value, defaultSchoolSubmitProgress());
  }
  if (!schoolSubmitMode.value && itemKey === 'progress') {
    if (activePage.value === 'audit') Object.assign(businessModel.value.auditProgress, defaults.auditProgress);
    if (activePage.value === 'review') Object.assign(businessModel.value.reviewWorkbench.progress, defaults.reviewWorkbench.progress);
  }
  if (!schoolSubmitMode.value && itemKey === 'reviewScope' && activePage.value === 'review') {
    Object.assign(businessModel.value.reviewWorkbench.scopeNavigator, defaults.reviewWorkbench.scopeNavigator);
  }
  if (!schoolSubmitMode.value && itemKey === 'actions' && activePage.value === 'review') {
    businessModel.value.reviewWorkbench.signature.hintText = defaults.reviewWorkbench.signature.hintText;
    businessModel.value.reviewWorkbench.signature.buttonText = defaults.reviewWorkbench.signature.buttonText;
  }
};
const addCard = () => {
  if (pageConfig.value.cards.length >= 6) return;
  pageConfig.value.cards.push({
    key: `custom-${Date.now()}`,
    rows: [[], []],
    backgroundColor: '#ffffff',
    backgroundOpacity: 100,
    borderRadius: 8,
    borderVisible: true
  });
};
const removeCard = (index: number) => {
  if (pageConfig.value.cards.length <= 1 || cardContainsRequiredItem(pageConfig.value.cards[index])) return;
  pageConfig.value.cards.splice(index, 1);
  selection.value = { kind: 'none' };
};
const cardContainsRequiredItem = (card: ArtWorkspaceHeaderCardConfig) =>
  (schoolSubmitMode.value || activePage.value === 'schoolSubmit') &&
  card.rows.some((row) => row.some((itemId) => itemId === 'home' || pageConfig.value.compactGroupInstances[itemId]?.items.includes('home')));
const moveCard = (index: number, offset: number) => {
  const target = index + offset;
  if (target < 0 || target >= pageConfig.value.cards.length) return;
  const [card] = pageConfig.value.cards.splice(index, 1);
  pageConfig.value.cards.splice(target, 0, card);
};
const startCardDrag = (event: DragEvent, index: number) => {
  clearDragState();
  cardDragIndex.value = index;
  if (!event.dataTransfer) return;
  event.dataTransfer.effectAllowed = 'move';
  event.dataTransfer.setData(WORKSPACE_CARD_DRAG_MIME, String(index));
};
const clearCardDrag = () => {
  cardDragIndex.value = undefined;
};
const dropCard = (event: DragEvent, targetIndex: number) => {
  event.preventDefault();
  if (dragSource.value || cardDragIndex.value === undefined || cardDragIndex.value === targetIndex) return;
  const [card] = pageConfig.value.cards.splice(cardDragIndex.value, 1);
  pageConfig.value.cards.splice(targetIndex, 0, card);
  clearCardDrag();
};
const moveNavigatorField = (targetIndex: number) => {
  if (!navigatorPageConfig.value || navDragIndex.value === undefined || navDragIndex.value === targetIndex) return;
  const [field] = navigatorPageConfig.value.fields.splice(navDragIndex.value, 1);
  navigatorPageConfig.value.fields.splice(targetIndex, 0, field);
  navDragIndex.value = undefined;
};

onMounted(ensureRows);
</script>

<style scoped>
.art-project-list-visual,
.art-layout-canvas,
.art-visual-panel,
.art-preview-stage {
  min-width: 0;
}

.art-project-list-visual {
  display: grid;
  gap: 10px;
}

.art-project-list-visual.is-compact :deep(.el-tabs__header) {
  margin-bottom: 8px;
}

.art-project-list-visual.is-compact :deep(.el-tabs__item) {
  height: 34px;
  padding: 0 12px;
}

.art-project-list-visual.is-compact :deep(.el-button) {
  --el-component-size: 28px;

  padding: 5px 9px;
}

.art-visual-toolbar,
.art-visual-toolbar > div,
.art-visual-panel > header,
.art-preview-stage > header,
.art-layout-card__head,
.art-layout-canvas__context {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.art-visual-toolbar > div,
.art-visual-panel > header,
.art-preview-stage > header {
  align-items: flex-start;
  flex-direction: column;
  gap: 3px;
}

.art-visual-toolbar small,
.art-visual-panel header small,
.art-preview-stage header small,
.art-component-palette__item small,
.art-property-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.school-submit-scene-guide {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.school-submit-scene-guide > span {
  display: grid;
  gap: 3px;
  padding: 10px 12px;
  color: var(--el-text-color-regular);
  background: var(--el-fill-color-extra-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.school-submit-scene-guide > span.is-current {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
}

.school-submit-scene-guide small {
  color: var(--el-text-color-secondary);
}

.art-project-list-visual__sections :deep(.el-tabs__content) {
  padding: 10px;
}

.art-project-list-visual__sections.is-single-section :deep(> .el-tabs__header) {
  display: none;
}

.art-visual-workspace {
  display: grid;
  grid-template-areas:
    'preview preview'
    'components properties';
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
}

.art-visual-panel,
.art-preview-stage,
.art-layout-canvas__surface {
  padding: 9px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--app-radius-md, var(--el-border-radius-base));
}

.art-header-metrics {
  margin-bottom: 10px;
}

.art-header-metrics__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
}

.art-header-metrics__grid :deep(.el-input-number) {
  width: 100%;
}

.art-header-metric-hint {
  display: block;
  width: 100%;
  margin-top: 3px;
  line-height: 1.35;
}

.art-compact-group-property-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.art-compact-group-property-list > span {
  padding: 4px 8px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: 6px;
}

.art-component-palette {
  grid-area: components;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 420px;
  overflow: auto;
}

.art-component-palette section {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 5px;
}

.art-component-palette__group {
  grid-column: 1 / -1;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 700;
}

.art-component-palette__item {
  display: flex;
  gap: 6px;
  align-items: center;
  width: 100%;
  min-height: 32px;
  padding: 5px 8px;
  color: var(--el-text-color-primary);
  text-align: left;
  background: var(--el-fill-color-extra-light);
  border: 1px solid transparent;
  border-radius: var(--app-radius-sm, var(--el-border-radius-small));
  cursor: grab;
}

.art-component-palette__item span {
  display: grid;
  min-width: 0;
}

.art-component-palette__item:hover {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-5);
}

.art-component-palette__item.is-used {
  cursor: pointer;
  opacity: 0.72;
}

.art-layout-canvas {
  grid-area: preview;
  display: grid;
  align-content: start;
  gap: 10px;
}

.art-layout-canvas__context {
  padding: 7px 9px;
  background: var(--el-fill-color-extra-light);
  border-radius: var(--app-radius-sm, var(--el-border-radius-small));
}

.art-layout-canvas__context label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 700;
}

.art-layout-canvas__context .el-select {
  width: 260px;
}

.art-layout-canvas__surface {
  display: grid;
  align-content: start;
  gap: 10px;
  min-height: 150px;
  max-height: clamp(170px, 20vh, 220px);
  overflow: auto;
  background-color: #f5f8fc;
  background-image: radial-gradient(var(--el-border-color-light) 0.7px, transparent 0.7px);
  background-size: 14px 14px;
}

.art-layout-canvas__preview {
  display: grid;
  min-width: 0;
}

.art-layout-table-reference {
  display: grid;
  grid-template-columns: 80px minmax(180px, 1fr) 100px 140px;
  min-width: 0;
  min-height: 36px;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 700;
  background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--app-radius-sm, var(--el-border-radius-small));
}

.art-layout-table-reference span {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: center;
  padding: 0 8px;
}

.art-layout-card {
  display: grid;
  gap: 8px;
  padding: 7px;
  box-shadow: 0 6px 16px rgb(15 23 42 / 4%);
}

.art-layout-card.is-selected {
  outline: 2px solid var(--el-color-primary-light-5);
}

.art-layout-card__head {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.art-layout-card__head > span {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: grab;
}

.art-layout-row {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  min-height: 54px;
  align-items: center;
  gap: 8px;
  padding: 8px;
  overflow: hidden;
  background: color-mix(in srgb, var(--el-fill-color-lighter) 72%, transparent);
  border: 1px dashed var(--el-border-color);
  border-radius: var(--app-radius-sm, var(--el-border-radius-small));
}

.art-layout-row.is-drop-target {
  background: color-mix(in srgb, var(--el-color-primary-light-9) 82%, transparent);
  border-color: var(--el-color-primary);
  box-shadow: inset 0 0 0 1px var(--el-color-primary-light-5);
}

.art-layout-row__label {
  display: flex;
  grid-column: 1 / -1;
  align-items: center;
  justify-content: space-between;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.art-layout-row__empty {
  grid-column: 1 / -1;
  color: var(--el-text-color-placeholder);
  text-align: center;
}

.art-layout-item {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  min-height: 34px;
  padding: 7px 10px;
  color: #24415f;
  background: #fff;
  border: 1px solid #dbe6f3;
  border-radius: var(--app-radius-sm, var(--el-border-radius-small));
  box-shadow: 0 2px 5px rgb(15 23 42 / 5%);
  cursor: grab;
}

.art-layout-compact-group {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  overflow: visible;
  border-style: dashed;
}

.art-layout-compact-group__title {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  white-space: nowrap;
}

.art-layout-compact-group__item {
  min-width: 0;
  padding: 4px 7px;
  overflow: hidden;
  color: #24415f;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: #fff;
  border: 1px solid #dbe6f3;
  border-radius: 6px;
  cursor: grab;
}

.art-layout-compact-group__empty {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.art-layout-item > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.art-layout-item.is-title {
  font-size: 15px;
  font-weight: 800;
}

.art-layout-item:is(.is-fixedSpacer, .is-autoSpacer) {
  color: var(--el-text-color-placeholder);
  background: repeating-linear-gradient(135deg, transparent, transparent 6px, var(--el-fill-color-light) 6px, var(--el-fill-color-light) 12px);
  border-style: dashed;
}

.art-layout-item.is-selected {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
}

.art-layout-item.is-drop-before {
  box-shadow:
    inset 4px 0 0 var(--el-color-primary),
    0 2px 5px rgb(15 23 42 / 5%);
}

.art-layout-item.is-drop-after {
  box-shadow:
    inset -4px 0 0 var(--el-color-primary),
    0 2px 5px rgb(15 23 42 / 5%);
}

.art-layout-compact-group.is-group-drop-target {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary);
}

.art-layout-item:is(.is-unifiedSubmit, .is-home):hover {
  color: var(--preview-button-hover-text);
  background-color: var(--preview-button-hover-background);
  border-color: var(--preview-button-hover-border);
}

.art-layout-card-add {
  min-height: 36px;
  color: var(--el-color-primary);
  background: rgb(255 255 255 / 76%);
  border: 1px dashed var(--el-color-primary-light-5);
  border-radius: var(--app-radius-md, var(--el-border-radius-base));
}

.art-property-panel {
  grid-area: properties;
  max-height: 420px;
  overflow: auto;
}

.art-property-panel :deep(.el-form-item) {
  margin-bottom: 10px;
}

.art-property-checkboxes {
  display: grid;
}

.art-preview-settings {
  display: grid;
  grid-template-columns: minmax(480px, 1fr) 320px;
  gap: 12px;
}

.art-preview-stage {
  display: grid;
  align-content: start;
  gap: 18px;
  min-height: 530px;
  background: var(--el-fill-color-extra-light);
}

.art-navigator-preview {
  display: grid;
  gap: 7px;
  width: min(100%, var(--preview-width));
  padding: 14px;
  background: var(--preview-bg);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--app-radius-md, var(--el-border-radius-base));
}

.art-navigator-preview.is-top {
  display: flex;
  width: 100%;
  align-items: center;
  overflow-x: auto;
}

.art-navigator-preview__item {
  display: flex;
  min-height: var(--preview-row-height);
  align-items: center;
  gap: 8px;
  padding: 0 10px;
  color: var(--el-text-color-primary);
  background: transparent;
  border: 0;
  border-radius: var(--app-radius-sm, var(--el-border-radius-small));
  cursor: grab;
}

.art-navigator-preview__item:nth-of-type(2) {
  background: var(--preview-selected);
}

.art-navigator-preview__item:hover {
  background: var(--preview-hover);
}

.art-navigator-preview__item small {
  margin-left: auto;
}

.art-table-preview {
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--preview-table-horizontal);
  border-radius: var(--preview-table-radius);
}

.art-table-preview__row {
  display: grid;
  grid-template-columns: 1fr 2fr 1fr 1fr 0.8fr;
  min-height: var(--preview-table-row-height);
  color: var(--preview-table-text);
  font-size: var(--preview-table-body-size);
  border-bottom: 1px solid var(--preview-table-horizontal);
}

.art-table-preview__row > span {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  white-space: var(--preview-table-wrap);
  border-right: 1px solid var(--preview-table-vertical);
}

.art-table-preview__status,
.art-table-preview__action {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  justify-content: center;
  gap: 4px;
  box-sizing: border-box;
  font: inherit;
  white-space: nowrap;
}

.art-table-preview__status {
  padding: 2px 8px;
  border: 1px solid transparent;
  border-radius: 4px;
  background: color-mix(in srgb, var(--preview-semantic-color) 10%, transparent);
  color: var(--preview-semantic-color);
}

.art-table-preview.has-status-border .art-table-preview__status {
  border-color: color-mix(in srgb, var(--preview-semantic-color) 32%, transparent);
}

.art-table-preview.has-uniform-status-width .art-table-preview__status {
  width: calc(var(--preview-status-width) + 18px);
}

.art-table-preview.has-uniform-status-width.has-status-icon .art-table-preview__status {
  width: calc(var(--preview-status-width) + 36px);
}

.art-table-preview__status.is-pending {
  --preview-semantic-color: var(--preview-status-pending);
}

.art-table-preview__status.is-returned {
  --preview-semantic-color: var(--preview-status-returned);
}

.art-table-preview__action {
  --preview-semantic-color: var(--preview-action-view);
  padding: 3px 4px;
  border: 1px solid transparent;
  border-radius: 6px;
  color: var(--preview-semantic-color);
}

.art-table-preview.has-action-border .art-table-preview__action {
  border-color: color-mix(in srgb, var(--preview-semantic-color) 42%, transparent);
}

.art-table-preview.is-action-button .art-table-preview__action {
  background: color-mix(in srgb, var(--preview-semantic-color) 9%, transparent);
}

.art-table-preview.has-uniform-action-width .art-table-preview__action {
  width: calc(var(--preview-action-width) + 8px);
}

.art-table-preview.has-uniform-action-width.has-action-icon .art-table-preview__action {
  width: calc(var(--preview-action-width) + 26px);
}

.art-table-preview__action.is-score {
  --preview-semantic-color: var(--preview-action-score);
}

.art-semantic-color-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 16px;
}

.art-semantic-color-grid label {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.art-semantic-color-grid label > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.art-table-preview__row.is-head {
  color: var(--preview-table-head-text);
  font-size: var(--preview-table-head-size);
  font-weight: 700;
  background: var(--preview-table-head);
}

.art-table-preview__row.is-hover {
  background: var(--preview-table-hover);
}

@media (max-width: 800px) {
  .school-submit-scene-guide {
    grid-template-columns: 1fr;
  }

  .art-visual-toolbar,
  .art-layout-canvas__context {
    align-items: stretch;
    flex-direction: column;
  }

  .art-visual-workspace {
    grid-template-areas:
      'preview'
      'components'
      'properties';
  }

  .art-visual-workspace,
  .art-preview-settings {
    grid-template-columns: 1fr;
  }

  .art-component-palette,
  .art-property-panel {
    max-height: none;
  }

  .art-layout-canvas__context label {
    align-items: stretch;
    flex-direction: column;
  }

  .art-layout-canvas__context .el-select {
    width: 100%;
  }
}

@media (max-width: 520px) {
  .art-component-palette section {
    grid-template-columns: 1fr;
  }
}
</style>
