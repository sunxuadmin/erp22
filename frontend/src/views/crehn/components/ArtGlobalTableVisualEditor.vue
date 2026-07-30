<template>
  <section v-loading="loading" class="global-table-editor" :class="{ 'is-compact': compact }">
    <header v-if="!compact" class="global-table-editor__toolbar">
      <div>
        <strong>{{ editorTitle }}</strong>
        <small>{{ editorDescription }}</small>
      </div>
      <el-segmented v-if="mode !== 'appearance' && showPageNavigation" v-model="pageKey" :options="pageOptions" />
    </header>

    <el-alert
      v-if="!compact"
      type="info"
      :closable="false"
      show-icon
      :title="
        mode === 'appearance'
          ? '颜色、状态标签和操作样式全局共享；这里只修改公共外观，不改变页面列方案。'
          : '列标题、显隐、固定和顺序按当前页面及类别范围生效；用户拖拽宽度仍是个人偏好。'
      "
    />

    <el-tabs v-if="draft" v-model="section" type="border-card" :class="{ 'is-single-section': mode !== 'all' }">
      <el-tab-pane v-if="mode !== 'appearance'" label="列与文字" name="columns">
        <div class="global-table-scope">
          <el-radio-group v-model="scope">
            <el-radio-button label="default">页面默认</el-radio-button>
            <el-radio-button label="category">按类别覆盖</el-radio-button>
          </el-radio-group>
          <template v-if="scope === 'category'">
            <el-select v-model="activityId" filterable clearable placeholder="选择活动" @change="loadCategories">
              <el-option v-for="item in activities" :key="String(item.id)" :label="item.activityName" :value="String(item.id)" />
            </el-select>
            <el-select v-model="categoryId" filterable clearable placeholder="选择末级小类别" @change="loadCategoryFields">
              <el-option
                v-for="item in categories"
                :key="String(item.id)"
                :label="item.displayLabel"
                :value="String(item.id)"
                :disabled="item.disabled"
              />
            </el-select>
            <el-button v-if="categoryId && currentPage.categoryColumns[categoryId]" type="danger" plain @click="removeCategoryOverride">
              删除类别覆盖
            </el-button>
            <el-button v-else-if="categoryId" type="primary" plain icon="Plus" @click="createCategoryOverride"> 创建类别覆盖 </el-button>
          </template>
        </div>

        <el-alert
          v-if="scope === 'category' && (!categoryId || !currentPage.categoryColumns[categoryId])"
          type="info"
          :closable="false"
          show-icon
          :title="
            categoryId
              ? '当前末级类别尚未创建覆盖，运行页面继续使用页面默认；点击“创建类别覆盖”后再单独修改。'
              : '请先选择活动和末级类别；未选择时不会创建或修改类别覆盖。'
          "
        />
        <div v-else class="global-table-workspace">
          <aside class="global-table-panel">
            <header><strong>表格列</strong><small>拖动调整顺序</small></header>
            <div
              v-for="(column, index) in editableColumns"
              :key="column.key"
              class="global-table-column-item"
              :class="{
                'is-selected': selectedKey === column.key,
                'is-hidden': !column.visible,
                'is-protected': isDeletionLocked(column)
              }"
              draggable="true"
              role="button"
              tabindex="0"
              :title="columnFixedHint(column)"
              @click="selectedKey = column.key"
              @keydown.enter.prevent="selectedKey = column.key"
              @keydown.space.prevent="selectedKey = column.key"
              @dragstart="dragIndex = index"
              @dragend="dragIndex = undefined"
              @dragover.prevent
              @drop="moveColumn(index)"
            >
              <el-icon><Rank /></el-icon>
              <span class="global-table-column-item__copy"
                ><strong>{{ column.label }}</strong
                ><small>{{ columnVariable(column) }} · {{ columnFixedLabel(column.fixed) }}</small></span
              >
              <span class="global-table-column-item__move" @click.stop>
                <el-button
                  plain
                  size="small"
                  aria-label="上移"
                  title="在当前固定分区内上移"
                  :disabled="!canMoveColumn(index, -1)"
                  @click="moveColumnByOffset(index, -1)"
                >
                  ↑
                </el-button>
                <el-button
                  plain
                  size="small"
                  aria-label="下移"
                  title="在当前固定分区内下移"
                  :disabled="!canMoveColumn(index, 1)"
                  @click="moveColumnByOffset(index, 1)"
                >
                  ↓
                </el-button>
              </span>
              <em>{{ column.width }}px</em>
            </div>
            <el-select v-model="columnToAdd" class="w-full" filterable clearable placeholder="增加系统字段或类别字段" @change="addColumn">
              <el-option-group v-if="availableBuiltinColumns.length" label="系统字段">
                <el-option v-for="item in availableBuiltinColumns" :key="item.key" :label="item.label" :value="item.key" />
              </el-option-group>
              <el-option-group v-if="availableFormFields.length" label="当前类别上报字段">
                <el-option
                  v-for="item in availableFormFields"
                  :key="String(item.fieldKey)"
                  :label="item.fieldLabel || item.fieldKey"
                  :value="`field:${item.fieldKey}`"
                />
              </el-option-group>
            </el-select>
          </aside>

          <main class="global-table-preview">
            <header>
              <span
                ><strong>{{ currentPageLabel }}实时预览</strong><small>单击选择列，双击表头直接修改文字</small></span
              >
              <el-segmented v-model="previewMode" :options="previewModeOptions" size="small" />
            </header>
            <div
              ref="previewTableShellRef"
              class="global-table-preview__shell art-resizable-table-shell"
              :class="{
                'art-resizable-table-shell--fit': previewMode === 'actual',
                'is-inspection': previewMode === 'inspection',
                'is-dragging': previewTableDragging
              }"
              tabindex="0"
              @pointerdown="startPreviewTableDrag"
              @pointermove="movePreviewTableDrag"
              @pointerup="stopPreviewTableDrag"
              @pointercancel="stopPreviewTableDrag"
              @wheel="handlePreviewTableWheel"
            >
              <el-table
                class="global-table-preview__table art-list-table art-resizable-table art-resizable-table--borderless"
                :class="previewAppearance.className"
                :style="previewTableStyle"
                :data="previewRows"
                border
              >
                <el-table-column
                  v-if="previewSelectionColumn"
                  type="selection"
                  column-key="selection"
                  :width="previewResolvedColumnWidth('selection')"
                  :fixed="previewSelectionColumn.fixed"
                  align="center"
                  :selectable="() => false"
                />
                <el-table-column
                  v-if="previewSerialColumn"
                  column-key="serial"
                  :width="previewResolvedColumnWidth('serial')"
                  :fixed="previewSerialColumn.fixed"
                  align="center"
                  class-name="art-table-nowrap-cell"
                >
                  <template #header>
                    <span v-if="previewSerialColumn.source === 'structural'">序号</span>
                    <template v-else>
                      <el-input
                        v-if="editingHeaderKey === previewSerialColumn.key"
                        :ref="setHeaderInputRef"
                        v-model="editingHeaderValue"
                        class="global-table-preview__header-input"
                        size="small"
                        maxlength="30"
                        @keydown.enter.prevent="commitHeaderEdit"
                        @keydown.esc.prevent="cancelHeaderEdit"
                        @blur="commitHeaderEdit"
                      />
                      <button
                        v-else
                        type="button"
                        class="global-table-preview__header-button"
                        :class="{ 'is-selected': selectedKey === previewSerialColumn.key }"
                        @click="selectedKey = previewSerialColumn.key"
                        @dblclick.stop="startHeaderEdit(previewSerialColumn)"
                      >
                        {{ previewSerialColumn.label }}
                      </button>
                    </template>
                  </template>
                  <template #default>1</template>
                </el-table-column>
                <el-table-column
                  v-for="column in previewDataColumns"
                  :key="column.key"
                  :column-key="column.key"
                  :label="column.label"
                  :width="previewResolvedColumnWidth(column.key)"
                  :min-width="column.minWidth"
                  :fixed="column.fixed"
                  :resizable="false"
                  :align="column.key === 'status' || column.key === 'actions' ? 'center' : 'left'"
                  :class-name="column.key === 'actions' ? 'art-table-nowrap-cell' : 'art-table-configurable-wrap-cell'"
                  :label-class-name="column.key === 'actions' ? 'art-fixed-action-header' : undefined"
                >
                  <template #header>
                    <el-input
                      v-if="editingHeaderKey === column.key"
                      :ref="setHeaderInputRef"
                      v-model="editingHeaderValue"
                      class="global-table-preview__header-input"
                      size="small"
                      maxlength="30"
                      @keydown.enter.prevent="commitHeaderEdit"
                      @keydown.esc.prevent="cancelHeaderEdit"
                      @blur="commitHeaderEdit"
                    />
                    <button
                      v-else
                      type="button"
                      class="global-table-preview__header-button"
                      :class="{ 'is-selected': selectedKey === column.key }"
                      @click="selectedKey = column.key"
                      @dblclick.stop="startHeaderEdit(column)"
                    >
                      {{ column.label }}
                    </button>
                  </template>
                  <template #default>
                    <el-tag
                      v-if="column.key === 'status' || column.key === 'scoreStatus'"
                      class="art-list-status-tag art-list-status-tag--pending"
                      type="warning"
                    >
                      <el-icon v-if="draft?.appearance.statusAppearance.iconVisible" class="art-list-status-tag__icon"><Clock /></el-icon>
                      <span>{{ previewStatusLabel }}</span>
                    </el-tag>
                    <div v-else-if="column.key === 'actions'" class="art-list-row-actions">
                      <el-button
                        v-for="action in previewActions"
                        :key="action.semantic"
                        class="art-list-row-action"
                        :class="`art-list-row-action--${action.semantic}`"
                        link
                        :icon="previewActionIcon(action.icon)"
                      >
                        {{ action.label }}
                      </el-button>
                    </div>
                    <span v-else>{{ sampleValue(column) }}</span>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </main>

          <aside class="global-table-panel global-table-properties">
            <header>
              <strong>属性设置</strong><small>{{ selectedColumn?.label || '尚未选择列' }}</small>
            </header>
            <el-form v-if="selectedColumn" label-position="top" class="global-table-property-grid">
              <el-form-item label="列标题"><el-input v-model="selectedColumn.label" maxlength="30" /></el-form-item>
              <el-form-item label="显示列">
                <el-switch v-model="selectedColumn.visible" :disabled="selectedColumnVisibilityLocked" />
              </el-form-item>
              <el-form-item label="固定位置">
                <el-select v-model="selectedColumn.fixed" @change="normalizeActiveColumnOrder">
                  <el-option v-for="item in fixedOptions(selectedColumn)" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
              <el-form-item label="默认列宽">
                <el-slider v-model="selectedColumn.width" :min="selectedColumnDefaultWidthMinimum" :max="800" show-input />
              </el-form-item>
              <el-form-item label="最小宽度">
                <el-slider
                  v-model="selectedColumn.minWidth"
                  :min="selectedColumnMinimumWidthMinimum"
                  :max="400"
                  show-input
                  :disabled="selectedColumn.key === 'actions'"
                />
              </el-form-item>
              <div v-if="isReviewGroupColumn(selectedColumn)" class="global-table-group-options">
                <el-divider content-position="left">组别取值</el-divider>
                <el-alert type="info" :closable="false" show-icon :title="groupOptionSourceText" />
                <div class="global-table-group-options__tags">
                  <el-tag v-for="label in groupOptionLabels" :key="label" effect="plain">{{ label }}</el-tag>
                </div>
                <small>组别只在活动报送规则中维护；这里负责预览，不保存第二套组别数据。历史“个人项目”仍兼容显示为“个人”。</small>
              </div>
              <div class="global-table-property-actions">
                <el-button plain icon="RefreshLeft" @click="restoreSelectedColumnDefaults">恢复此列默认</el-button>
                <el-button v-if="!isDeletionLocked(selectedColumn)" type="danger" plain icon="Delete" @click="deleteSelectedColumn">
                  隐藏并移入回收站
                </el-button>
                <el-button v-else disabled plain icon="Lock">
                  {{ selectedColumn.key === 'actions' ? '操作列可隐藏，但不可移入回收站' : '该列不可隐藏或删除' }}
                </el-button>
              </div>
            </el-form>
            <template>
              <el-divider content-position="left">结构列</el-divider>
              <el-form label-position="top" class="global-table-property-grid">
                <el-form-item v-if="pageKey === 'review'" label="选择列固定位置">
                  <el-radio-group v-model="currentPage.selectionFixed">
                    <el-radio-button label="left">固定左侧</el-radio-button>
                    <el-radio-button label="none">不固定</el-radio-button>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="显示序号列">
                  <el-switch v-model="currentPage.serialVisible" />
                </el-form-item>
                <el-form-item label="序号列宽">
                  <el-slider v-model="currentPage.serialWidth" :min="48" :max="240" show-input :disabled="!currentPage.serialVisible" />
                </el-form-item>
                <el-form-item label="序号列固定位置">
                  <el-radio-group v-model="currentPage.serialFixed">
                    <el-radio-button label="left">固定左侧</el-radio-button>
                    <el-radio-button label="none">不固定</el-radio-button>
                  </el-radio-group>
                </el-form-item>
              </el-form>
            </template>
            <el-divider content-position="left">表格固定文字</el-divider>
            <el-form label-position="top" class="global-table-fixed-text-grid">
              <el-form-item label="空数据提示"><el-input v-model="currentPage.emptyText" maxlength="60" /></el-form-item>
              <el-form-item v-for="item in statusTextOptions" :key="item.key" :label="item.label">
                <el-input v-model="currentPage.statusLabels[item.key]" maxlength="30" />
              </el-form-item>
              <el-form-item v-for="item in actionTextOptions" :key="item.key" :label="item.label">
                <el-input v-model="currentPage.actionLabels[item.key]" maxlength="30" />
              </el-form-item>
            </el-form>
          </aside>
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="mode !== 'layout'" label="颜色与公共样式" name="appearance">
        <div class="global-appearance-grid">
          <div class="global-appearance-preview">
            <header class="global-appearance-preview__heading">
              <span><strong>全局颜色实时预览</strong><small>六组场景同步响应右侧设置；拖动空白区域可横向查看</small></span>
              <el-tag size="small" effect="plain" type="info">其它表格仅作全量预览</el-tag>
            </header>
            <div
              ref="appearancePreviewViewportRef"
              class="global-appearance-preview__viewport"
              :class="{ 'is-dragging': appearancePreviewDragging }"
              tabindex="0"
              aria-label="全局颜色实时预览画布，可横向拖动查看"
              @pointerdown="startAppearancePreviewDrag"
              @pointermove="moveAppearancePreviewDrag"
              @pointerup="stopAppearancePreviewDrag"
              @pointercancel="stopAppearancePreviewDrag"
              @wheel="handleAppearancePreviewWheel"
            >
              <div class="global-appearance-preview__board">
                <section v-for="scene in appearancePreviewScenes" :key="scene.key" class="global-appearance-preview__scene">
                  <header>
                    <span
                      ><strong>{{ scene.label }}</strong
                      ><small>{{ scene.description }}</small></span
                    >
                    <em>{{ scene.rows.length }} 种状态</em>
                  </header>
                  <el-table
                    class="global-appearance-preview__table art-list-table art-resizable-table art-resizable-table--borderless"
                    :class="appearanceOverview.className"
                    :style="appearanceOverview.style"
                    :data="scene.rows"
                    border
                  >
                    <el-table-column label="项目名称" min-width="132">
                      <template #default="{ row }">{{ row.projectName }}</template>
                    </el-table-column>
                    <el-table-column label="状态" width="112" align="center">
                      <template #default="{ row }">
                        <el-tag
                          class="art-list-status-tag"
                          :class="`art-list-status-tag--${previewStatusClass(row.status)}`"
                          :type="previewStatusTagType(row.status)"
                        >
                          <el-icon v-if="draft?.appearance.statusAppearance.iconVisible" class="art-list-status-tag__icon">
                            <component :is="previewStatusIcon(row.status)" />
                          </el-icon>
                          <span>{{ row.statusLabel }}</span>
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" min-width="228" align="center" class-name="art-table-nowrap-cell">
                      <template #default="{ row }">
                        <div class="art-list-row-actions">
                          <el-button
                            v-for="action in row.actions"
                            :key="`${row.status}-${action.semantic}`"
                            class="art-list-row-action"
                            :class="`art-list-row-action--${action.semantic}`"
                            link
                            :disabled="action.disabled"
                            :icon="previewActionIcon(action.icon)"
                          >
                            {{ action.label }}
                          </el-button>
                        </div>
                      </template>
                    </el-table-column>
                  </el-table>
                </section>

                <section class="global-appearance-preview__scene global-appearance-preview__scene--overview">
                  <header>
                    <span><strong>其它表格 / 全量样式总览</strong><small>集中查看全部状态、操作及禁用透明度，不扩大实际覆盖范围</small></span>
                    <em>8 状态 · 7 操作</em>
                  </header>
                  <el-table
                    class="global-appearance-preview__table art-list-table art-resizable-table art-resizable-table--borderless"
                    :class="appearanceOverview.className"
                    :style="appearanceOverview.style"
                    :data="appearanceSemanticOverviewRows"
                    border
                  >
                    <el-table-column label="项目" width="112">
                      <template #default="{ row }">{{ row.label }}</template>
                    </el-table-column>
                    <el-table-column label="实时样式" min-width="340">
                      <template #default="{ row }">
                        <div v-if="row.type === 'status'" class="global-appearance-preview__semantic-list">
                          <el-tag
                            v-for="status in appearanceOverviewStatuses"
                            :key="status.semantic"
                            class="art-list-status-tag"
                            :class="`art-list-status-tag--${previewStatusClass(status.semantic)}`"
                            :type="previewStatusTagType(status.semantic)"
                          >
                            <el-icon v-if="draft?.appearance.statusAppearance.iconVisible" class="art-list-status-tag__icon">
                              <component :is="previewStatusIcon(status.semantic)" />
                            </el-icon>
                            <span>{{ status.label }}</span>
                          </el-tag>
                        </div>
                        <div v-else class="art-list-row-actions global-appearance-preview__semantic-list">
                          <el-button
                            v-for="action in appearanceOverviewActions"
                            :key="action.key"
                            class="art-list-row-action"
                            :class="`art-list-row-action--${action.semantic}`"
                            link
                            :disabled="action.disabled"
                            :icon="previewActionIcon(action.icon)"
                          >
                            {{ action.label }}
                          </el-button>
                        </div>
                      </template>
                    </el-table-column>
                  </el-table>
                </section>
              </div>
            </div>
          </div>
          <aside class="global-table-panel global-appearance-form">
            <el-form label-position="top">
              <el-form-item label="启用全局颜色覆盖">
                <el-switch v-model="draft.appearance.globalColorOverride" active-text="覆盖五类表格" inactive-text="使用页面默认颜色" />
              </el-form-item>
              <el-alert
                v-if="draft.appearance.globalColorOverride"
                type="warning"
                :closable="false"
                show-icon
                title="保存后将覆盖类别上报、学校统一提交、项目审核、专家评分和上报进度的表格颜色。"
              />
              <div class="global-color-grid">
                <label>表头背景 <el-color-picker v-model="draft.appearance.headerBackground" /></label>
                <label>表头文字 <el-color-picker v-model="draft.appearance.headerTextColor" /></label>
                <label>正文文字 <el-color-picker v-model="draft.appearance.textColor" /></label>
                <label>悬停背景 <el-color-picker v-model="draft.appearance.hoverBackground" /></label>
                <label>边框颜色 <el-color-picker v-model="draft.appearance.borderColor" /></label>
                <label>横线颜色 <el-color-picker v-model="draft.appearance.horizontalBorderColor" /></label>
                <label>竖线颜色 <el-color-picker v-model="draft.appearance.verticalBorderColor" /></label>
                <label>外框颜色 <el-color-picker v-model="draft.appearance.outerBorderColor" /></label>
              </div>
              <el-form-item label="行高"><el-slider v-model="draft.appearance.rowHeight" :min="34" :max="72" show-input /></el-form-item>
              <el-form-item label="圆角"><el-slider v-model="draft.appearance.borderRadius" :min="0" :max="24" show-input /></el-form-item>
              <el-form-item label="表头字号"><el-slider v-model="draft.appearance.headerFontSize" :min="12" :max="20" show-input /></el-form-item>
              <el-form-item label="正文字号"><el-slider v-model="draft.appearance.bodyFontSize" :min="12" :max="20" show-input /></el-form-item>
              <el-form-item label="按钮与状态字号">
                <el-slider v-model="draft.appearance.buttonFontSize" :min="12" :max="20" show-input />
              </el-form-item>
              <el-form-item label="内容换行">
                <el-radio-group v-model="draft.appearance.wrapMode">
                  <el-radio-button label="nowrap">不换行</el-radio-button>
                  <el-radio-button label="wrap">自动换行</el-radio-button>
                  <el-radio-button label="two-line">最多两行</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="横向线"><el-switch v-model="draft.appearance.horizontalBorderVisible" /></el-form-item>
              <el-form-item label="竖向线"><el-switch v-model="draft.appearance.verticalBorderVisible" /></el-form-item>
              <el-form-item label="显示外框"><el-switch v-model="draft.appearance.outerBorderVisible" /></el-form-item>
              <el-form-item label="表头换行"><el-switch v-model="draft.appearance.headerWrap" /></el-form-item>
              <el-form-item label="斑马纹"><el-switch v-model="draft.appearance.striped" /></el-form-item>
              <el-form-item label="操作呈现">
                <el-radio-group v-model="draft.appearance.actionStyle">
                  <el-radio-button label="link">文字链接</el-radio-button>
                  <el-radio-button label="button">按钮</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <div class="global-recommended-colors">
                <div class="global-recommended-colors__heading">
                  <span><strong>推荐配色</strong><small>清新明快、低饱和，保留状态和操作的语义差异</small></span>
                  <el-tag size="small" effect="plain" :type="activeColorPreset ? 'primary' : 'info'">
                    {{ activeColorPreset?.label || '自定义' }}
                  </el-tag>
                </div>
                <div class="global-color-presets" role="radiogroup" aria-label="推荐配色">
                  <button
                    v-for="preset in colorPresets"
                    :key="preset.key"
                    type="button"
                    class="global-color-preset"
                    :class="{ 'is-active': activeColorPreset?.key === preset.key }"
                    :aria-pressed="activeColorPreset?.key === preset.key"
                    @click="applyColorPreset(preset.key)"
                  >
                    <span class="global-color-preset__copy">
                      <strong>{{ preset.label }}</strong>
                      <small>{{ preset.description }}</small>
                    </span>
                    <span class="global-color-preset__swatches" aria-hidden="true">
                      <i v-for="color in presetSwatches(preset)" :key="color" :style="{ backgroundColor: color }"></i>
                    </span>
                  </button>
                </div>
                <small class="global-recommended-colors__hint">点击即更新草稿预览；只修改颜色，保存后才全局生效。</small>
              </div>
              <el-divider content-position="left">状态标签</el-divider>
              <el-form-item label="显示边框"><el-switch v-model="draft.appearance.statusAppearance.borderVisible" /></el-form-item>
              <el-form-item label="显示小图标"><el-switch v-model="draft.appearance.statusAppearance.iconVisible" /></el-form-item>
              <el-form-item label="同列自动统一宽度"><el-switch v-model="draft.appearance.statusAppearance.uniformWidth" /></el-form-item>
              <div class="global-color-grid">
                <label v-for="item in statusColorOptions" :key="item.key">
                  {{ item.label }} <el-color-picker v-model="draft.appearance.statusAppearance.colors[item.key]" />
                </label>
              </div>
              <el-divider content-position="left">操作按钮</el-divider>
              <el-form-item label="显示边框"><el-switch v-model="draft.appearance.rowActionAppearance.borderVisible" /></el-form-item>
              <el-form-item label="显示小图标"><el-switch v-model="draft.appearance.rowActionAppearance.iconVisible" /></el-form-item>
              <el-form-item label="同列自动统一宽度"><el-switch v-model="draft.appearance.rowActionAppearance.uniformWidth" /></el-form-item>
              <el-form-item label="禁用透明度">
                <el-slider v-model="draft.appearance.rowActionAppearance.disabledOpacity" :min="10" :max="100" show-input />
              </el-form-item>
              <div class="global-color-grid">
                <label v-for="item in actionColorOptions" :key="item.key">
                  {{ item.label }} <el-color-picker v-model="draft.appearance.rowActionAppearance.colors[item.key]" />
                </label>
              </div>
            </el-form>
          </aside>
        </div>
      </el-tab-pane>
    </el-tabs>

    <footer v-if="showFooter" class="global-table-editor__footer">
      <span>{{ isDirty ? '当前存在未保存修改' : '当前全局表格配置已保存' }}</span>
      <div>
        <el-button :disabled="saving" @click="restoreDefaults()">恢复系统默认</el-button>
        <el-button :disabled="!isDirty || saving" @click="cancelChanges">取消本次修改</el-button>
        <el-button type="primary" :disabled="!isDirty" :loading="saving" @click="saveAll">
          {{ saveActionLabel }}
        </el-button>
      </div>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { Back, CircleCheck, Clock, Document, EditPen, Lock } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type InputInstance } from 'element-plus';
import { listActivityOptions, listCategory } from '@/api/crehn/activity';
import { listActivityReportRuleSchoolOptions } from '@/api/crehn/config';
import { listArtDetailDisplayCategoryFields } from '@/api/crehn/detailDisplay';
import type {
  ArtListActionSemantic,
  ArtListColumnFixed,
  ArtListStatusSemantic,
  ArtListTableAppearanceConfig,
  ArtListTableConfigVO,
  ArtListTablePageKey,
  ArtReviewListColumnConfig
} from '@/api/crehn/detailDisplay';
import type { ActivityCategoryVO, ActivityRuleGroupOptionVO, ActivityVO, CategoryFieldSchemaVO } from '@/api/crehn/types';
import { resolveArtListTableAppearance } from '@/composables/useArtListTableAppearance';
import {
  artListActionColumnMinimumWidth,
  artListActionSlots,
  artListStatusColumnMinimumWidth,
  sortArtListColumnsByFixedZone,
  toArtListRuntimeColumns,
  type ArtListRuntimeColumn
} from '@/composables/artListTableRuntime';
import { useArtTableColumnWidths, type ArtResizableColumn } from '@/composables/useArtTableColumnWidths';
import {
  artListTablePageOptions,
  defaultArtListTableLayout,
  defaultListTableAppearance,
  loadArtListTableConfig,
  saveArtListTableConfig
} from './artDetailDisplayConfig';

type CategoryOption = ActivityCategoryVO & { displayLabel: string; disabled: boolean };
type PreviewStructuralColumn = ArtResizableColumn & {
  label: string;
  source: 'structural';
};
type ArtListColorPresetKey = 'sky' | 'mint' | 'lime' | 'lavender';
type ArtListBaseColorConfig = Pick<
  ArtListTableAppearanceConfig,
  | 'headerBackground'
  | 'headerTextColor'
  | 'textColor'
  | 'hoverBackground'
  | 'borderColor'
  | 'horizontalBorderColor'
  | 'verticalBorderColor'
  | 'outerBorderColor'
>;
interface ArtListColorPreset {
  key: ArtListColorPresetKey;
  label: string;
  description: string;
  colors: ArtListBaseColorConfig;
  statusColors: Record<ArtListStatusSemantic, string>;
  actionColors: Record<ArtListActionSemantic, string>;
}
type PreviewStatusTagType = 'primary' | 'success' | 'warning' | 'danger' | 'info';
interface AppearancePreviewAction {
  semantic: ArtListActionSemantic;
  label: string;
  icon: string;
  disabled?: boolean;
}
interface AppearancePreviewRow {
  projectName: string;
  status: ArtListStatusSemantic;
  statusLabel: string;
  actions: AppearancePreviewAction[];
}
interface AppearancePreviewScene {
  key: ArtListTablePageKey;
  label: string;
  description: string;
  rows: AppearancePreviewRow[];
}

const props = withDefaults(
  defineProps<{
    mode?: 'all' | 'layout' | 'appearance';
    compact?: boolean;
    showPageNavigation?: boolean;
    showFooter?: boolean;
  }>(),
  {
    mode: 'all',
    compact: false,
    showPageNavigation: true,
    showFooter: true
  }
);
const mode = computed(() => props.mode);
const compact = computed(() => props.compact);
const showPageNavigation = computed(() => props.showPageNavigation);
const showFooter = computed(() => props.showFooter);
const pageOptions = artListTablePageOptions.map((item) => ({ label: item.label, value: item.key }));
const previewModeOptions = [
  { label: '页面实际效果', value: 'actual' },
  { label: '检查全部列', value: 'inspection' }
];
const statusTextOptions: Array<{ key: ArtListStatusSemantic; label: string }> = [
  { key: 'draft', label: '草稿文字' },
  { key: 'pending', label: '待审核文字' },
  { key: 'approved', label: '已通过文字' },
  { key: 'returned', label: '已退回文字' },
  { key: 'unscored', label: '未评分文字' },
  { key: 'scored', label: '已评分文字' },
  { key: 'scoreDraft', label: '评分草稿文字' },
  { key: 'locked', label: '锁定文字' }
];
const actionTextOptions: Array<{ key: ArtListActionSemantic; label: string }> = [
  { key: 'view', label: '查看文字' },
  { key: 'score', label: '评分文字' },
  { key: 'edit', label: '编辑文字' },
  { key: 'return', label: '退回文字' },
  { key: 'withdraw', label: '撤回文字' },
  { key: 'submit', label: '提交文字' },
  { key: 'delete', label: '删除文字' }
];
const statusColorOptions = statusTextOptions.map((item) => ({ key: item.key, label: item.label.replace('文字', '') }));
const actionColorOptions = actionTextOptions.map((item) => ({ key: item.key, label: item.label.replace('文字', '') }));
const previewStatusTagTypes: Record<ArtListStatusSemantic, PreviewStatusTagType> = {
  draft: 'info',
  pending: 'warning',
  approved: 'success',
  returned: 'danger',
  unscored: 'danger',
  scored: 'success',
  scoreDraft: 'info',
  locked: 'warning'
};
const previewStatusIcons = {
  draft: Document,
  pending: Clock,
  approved: CircleCheck,
  returned: Back,
  unscored: EditPen,
  scored: CircleCheck,
  scoreDraft: EditPen,
  locked: Lock
};
const previewActionIcons: Record<ArtListActionSemantic, string> = {
  view: 'View',
  score: 'EditPen',
  edit: 'Edit',
  return: 'Back',
  withdraw: 'RefreshLeft',
  submit: 'Upload',
  delete: 'Delete'
};
const commonPreviewStatuses: ArtListStatusSemantic[] = ['draft', 'pending', 'returned', 'approved'];
const reviewPreviewStatuses: ArtListStatusSemantic[] = ['unscored', 'scoreDraft', 'scored', 'locked'];
const baseColorFields: Array<keyof ArtListBaseColorConfig> = [
  'headerBackground',
  'headerTextColor',
  'textColor',
  'hoverBackground',
  'borderColor',
  'horizontalBorderColor',
  'verticalBorderColor',
  'outerBorderColor'
];
const colorPresets: ArtListColorPreset[] = [
  {
    key: 'sky',
    label: '晴空蓝',
    description: '清爽稳妥',
    colors: {
      headerBackground: '#EEF4FF',
      headerTextColor: '#1F3B64',
      textColor: '#334155',
      hoverBackground: '#F8FBFF',
      borderColor: '#E8EEF6',
      horizontalBorderColor: '#E8EEF6',
      verticalBorderColor: '#E8EEF6',
      outerBorderColor: '#E8EEF6'
    },
    statusColors: {
      draft: '#6B778C',
      pending: '#B88736',
      approved: '#4D8E62',
      returned: '#BC626B',
      unscored: '#B88736',
      scored: '#4D8E62',
      scoreDraft: '#6B778C',
      locked: '#9A7844'
    },
    actionColors: {
      view: '#4F73B8',
      score: '#4F73B8',
      edit: '#4F73B8',
      return: '#BC626B',
      withdraw: '#B88736',
      submit: '#4D8E62',
      delete: '#BC626B'
    }
  },
  {
    key: 'mint',
    label: '薄荷青',
    description: '轻盈柔和',
    colors: {
      headerBackground: '#EAF9F4',
      headerTextColor: '#246457',
      textColor: '#344A45',
      hoverBackground: '#F4FCF9',
      borderColor: '#D8EEE7',
      horizontalBorderColor: '#D8EEE7',
      verticalBorderColor: '#D8EEE7',
      outerBorderColor: '#D8EEE7'
    },
    statusColors: {
      draft: '#607672',
      pending: '#956526',
      approved: '#33795C',
      returned: '#AA525C',
      unscored: '#956526',
      scored: '#33795C',
      scoreDraft: '#607672',
      locked: '#786644'
    },
    actionColors: {
      view: '#3E7F8C',
      score: '#3E7F8C',
      edit: '#3E7F8C',
      return: '#AA525C',
      withdraw: '#956526',
      submit: '#33795C',
      delete: '#AA525C'
    }
  },
  {
    key: 'lime',
    label: '青柠绿',
    description: '自然明亮',
    colors: {
      headerBackground: '#F1F9E8',
      headerTextColor: '#4B6534',
      textColor: '#43513B',
      hoverBackground: '#F8FCF4',
      borderColor: '#DFEDD2',
      horizontalBorderColor: '#DFEDD2',
      verticalBorderColor: '#DFEDD2',
      outerBorderColor: '#DFEDD2'
    },
    statusColors: {
      draft: '#68755E',
      pending: '#936424',
      approved: '#4A7C36',
      returned: '#AA5158',
      unscored: '#936424',
      scored: '#4A7C36',
      scoreDraft: '#68755E',
      locked: '#76643F'
    },
    actionColors: {
      view: '#5476A7',
      score: '#5476A7',
      edit: '#5476A7',
      return: '#AA5158',
      withdraw: '#936424',
      submit: '#4A7C36',
      delete: '#AA5158'
    }
  },
  {
    key: 'lavender',
    label: '丁香紫',
    description: '清雅活泼',
    colors: {
      headerBackground: '#F3EFFF',
      headerTextColor: '#5B4F82',
      textColor: '#484458',
      hoverBackground: '#FAF8FF',
      borderColor: '#E4DDF5',
      horizontalBorderColor: '#E4DDF5',
      verticalBorderColor: '#E4DDF5',
      outerBorderColor: '#E4DDF5'
    },
    statusColors: {
      draft: '#6F6980',
      pending: '#966727',
      approved: '#437A5F',
      returned: '#AA5062',
      unscored: '#966727',
      scored: '#437A5F',
      scoreDraft: '#6F6980',
      locked: '#786345'
    },
    actionColors: {
      view: '#7664B0',
      score: '#7664B0',
      edit: '#7664B0',
      return: '#AA5062',
      withdraw: '#966727',
      submit: '#437A5F',
      delete: '#AA5062'
    }
  }
];

const loading = ref(false);
const saving = ref(false);
const section = ref<'columns' | 'appearance'>(props.mode === 'appearance' ? 'appearance' : 'columns');
const pageKey = defineModel<ArtListTablePageKey>('pageKey', { default: 'project' });
const scope = ref<'default' | 'category'>('default');
const activityId = ref('');
const categoryId = ref('');
const activities = ref<ActivityVO[]>([]);
const rawCategories = ref<ActivityCategoryVO[]>([]);
const fields = ref<CategoryFieldSchemaVO[]>([]);
const draft = ref<ArtListTableConfigVO>();
const originalSnapshot = ref('');
const selectedKey = ref('');
const dragIndex = ref<number>();
const columnToAdd = ref('');
const groupRuleOptions = ref<ActivityRuleGroupOptionVO[]>([]);
const groupOptionsLoading = ref(false);
const groupOptionsLoadFailed = ref(false);
const defaultGroupOptionLabels = ['甲组', '乙组', '个人'];
const editingHeaderKey = ref('');
const editingHeaderValue = ref('');
const headerInputRef = ref<InputInstance>();
const previewMode = ref<'actual' | 'inspection'>('actual');
const previewTableDragging = ref(false);
let previewTablePointerId: number | undefined;
let previewTablePointerStartX = 0;
let previewTableScrollStart = 0;
const appearancePreviewViewportRef = ref<HTMLElement>();
const appearancePreviewDragging = ref(false);
let appearancePreviewPointerId: number | undefined;
let appearancePreviewPointerStartX = 0;
let appearancePreviewScrollStart = 0;
const setHeaderInputRef = (instance: unknown) => {
  headerInputRef.value = (instance || undefined) as InputInstance | undefined;
};
const editorTitle = computed(() => (mode.value === 'appearance' ? '颜色与公共样式' : '五类页面与类别表格'));
const editorDescription = computed(() =>
  mode.value === 'appearance' ? '表格颜色、状态与操作样式全角色共享，保存后统一生效' : '按页面默认或活动末级类别维护列、文字、固定位置和预览效果'
);

const currentPage = computed(() => draft.value!.layout.pages[pageKey.value]);
const currentPageLabel = computed(() => artListTablePageOptions.find((item) => item.key === pageKey.value)?.label || '业务表格');
const categories = computed<CategoryOption[]>(() => {
  const source = rawCategories.value.filter((item) => item.delFlag !== '1');
  const parentIds = new Set(source.map((item) => String(item.parentId || '')).filter((id) => id && id !== '0'));
  const names = new Map(source.map((item) => [String(item.id), item.categoryName || '']));
  return source.map((item) => {
    const parentId = String(item.parentId || '');
    const parentName = parentId && parentId !== '0' ? names.get(parentId) : '';
    return {
      ...item,
      displayLabel: parentName ? `${parentName} / ${item.categoryName || ''}` : String(item.categoryName || ''),
      disabled: item.enabled === false || parentIds.has(String(item.id || ''))
    };
  });
});
const activeColumns = computed<ArtReviewListColumnConfig[]>(() => {
  if (scope.value === 'category') {
    return categoryId.value ? currentPage.value.categoryColumns[categoryId.value] || [] : [];
  }
  return currentPage.value.columns;
});
const editableColumns = computed(() => activeColumns.value.filter((column) => !column.deleted));
const visibleColumns = computed(() => editableColumns.value.filter((column) => column.visible));
const selectedColumn = computed(() => activeColumns.value.find((column) => column.key === selectedKey.value && !column.deleted));
const selectedColumnRuntimeMinimum = computed(() => {
  const column = selectedColumn.value;
  if (!column || !draft.value) return 48;
  if (column.key === 'actions') {
    return artListActionColumnMinimumWidth(pageKey.value, currentPage.value, draft.value.appearance);
  }
  if (['status', 'scoreStatus'].includes(column.key)) {
    return artListStatusColumnMinimumWidth(currentPage.value, draft.value.appearance);
  }
  return 48;
});
const selectedColumnDefaultWidthMinimum = computed(() =>
  Math.max(selectedColumn.value?.key === 'actions' ? Number(selectedColumn.value.minWidth) || 48 : 58, selectedColumnRuntimeMinimum.value)
);
const selectedColumnMinimumWidthMinimum = computed(() =>
  ['status', 'scoreStatus'].includes(selectedColumn.value?.key || '') ? selectedColumnRuntimeMinimum.value : 48
);
const isReviewGroupColumn = (column?: ArtReviewListColumnConfig) => pageKey.value === 'review' && column?.key === 'groupOrNature';
const isVisibilityLocked = (column?: ArtReviewListColumnConfig) => isReviewGroupColumn(column);
const isDeletionLocked = (column?: ArtReviewListColumnConfig) => column?.key === 'actions' || isReviewGroupColumn(column);
const selectedColumnVisibilityLocked = computed(() => isVisibilityLocked(selectedColumn.value));
const groupOptionLabels = computed(() => {
  if (scope.value !== 'category' || !activityId.value || groupRuleOptions.value.length === 0) return defaultGroupOptionLabels;
  const labels = groupRuleOptions.value.map((item) => String(item.groupName || item.groupCode || '').trim()).filter(Boolean);
  return [...new Set(labels)];
});
const groupOptionSourceText = computed(() => {
  if (scope.value !== 'category') return '系统预览：切换到“按类别覆盖”并选择活动后，可查看该活动的真实组别。';
  if (!activityId.value) return '请选择活动；未选择时展示系统建议组别。';
  if (groupOptionsLoading.value) return '正在读取当前活动报送规则中的组别…';
  if (groupOptionsLoadFailed.value) return '当前活动组别读取失败，暂时展示系统建议组别。';
  if (groupRuleOptions.value.length > 0) return categoryId.value ? '来自当前活动及末级类别的报送规则。' : '来自当前活动的报送规则。';
  return '当前范围没有配置顶层组别，暂时展示系统建议组别。';
});
const defaultsForPage = computed(() => defaultArtListTableLayout().pages[pageKey.value].columns);
const availableBuiltinColumns = computed(() =>
  defaultsForPage.value.filter((item) => !activeColumns.value.some((column) => column.key === item.key && !column.deleted))
);
const availableFormFields = computed(() =>
  scope.value === 'category' && categoryId.value
    ? fields.value.filter(
        (field) =>
          field.fieldKey &&
          !activeColumns.value.some((column) => column.source === 'form' && String(column.fieldKey) === String(field.fieldKey) && !column.deleted)
      )
    : []
);
const visibleRuntimeColumns = computed<ArtListRuntimeColumn[]>(() => {
  if (!draft.value) return [];
  return toArtListRuntimeColumns(pageKey.value, visibleColumns.value, currentPage.value, draft.value.appearance);
});
const previewSelectionColumn = computed<PreviewStructuralColumn | ArtListRuntimeColumn | undefined>(() => {
  if (pageKey.value === 'review') {
    return {
      key: 'selection',
      label: '选择',
      source: 'structural',
      width: 50,
      minWidth: 50,
      visible: true,
      fixed: currentPage.value.selectionFixed === 'left' ? 'left' : false,
      resizable: false
    };
  }
  return pageKey.value === 'schoolSubmit' ? visibleRuntimeColumns.value.find((column) => column.key === 'selection') : undefined;
});
const previewSerialColumn = computed<PreviewStructuralColumn | ArtListRuntimeColumn | undefined>(() => {
  if (!currentPage.value.serialVisible) return undefined;
  return {
    key: 'serial',
    label: '序号',
    source: 'structural',
    width: currentPage.value.serialWidth,
    minWidth: 48,
    visible: true,
    fixed: currentPage.value.serialFixed === 'left' ? 'left' : false,
    resizable: false
  };
});
const previewDataColumns = computed(() => {
  return visibleRuntimeColumns.value.filter((column) => !['selection', 'serial'].includes(column.key));
});
const previewWidthColumns = computed<ArtResizableColumn[]>(() => [
  ...(previewSelectionColumn.value ? [previewSelectionColumn.value] : []),
  ...(previewSerialColumn.value ? [previewSerialColumn.value] : []),
  ...previewDataColumns.value
]);
const previewWidthStorageKey = computed(() => '__art-global-table-preview-widths__');
const { tableShellRef: previewTableShellRef, columnWidth: previewColumnWidth } = useArtTableColumnWidths({
  columns: previewWidthColumns,
  storageKey: previewWidthStorageKey,
  fitContainer: true,
  persistLocalWidths: false
});
const previewResolvedColumnWidth = (key: string) => {
  if (previewMode.value === 'actual') return previewColumnWidth(key);
  return previewWidthColumns.value.find((column) => column.key === key)?.width;
};
const previewStatusLabel = computed(() =>
  pageKey.value === 'review' ? currentPage.value.statusLabels.scored : currentPage.value.statusLabels.pending
);
const previewActionDefinitions: Record<
  ArtListTablePageKey,
  Array<{ semantic: ArtListActionSemantic; icon: string; alternate?: ArtListActionSemantic }>
> = {
  project: [
    { semantic: 'edit', alternate: 'view', icon: 'Edit' },
    { semantic: 'submit', alternate: 'withdraw', icon: 'Upload' },
    { semantic: 'delete', icon: 'Delete' }
  ],
  schoolSubmit: [
    { semantic: 'view', icon: 'View' },
    { semantic: 'submit', alternate: 'withdraw', icon: 'Upload' },
    { semantic: 'delete', icon: 'Delete' }
  ],
  audit: [
    { semantic: 'view', icon: 'View' },
    { semantic: 'return', icon: 'Back' },
    { semantic: 'withdraw', icon: 'RefreshLeft' }
  ],
  review: [{ semantic: 'view', alternate: 'score', icon: 'View' }],
  projectView: [
    { semantic: 'view', icon: 'View' },
    { semantic: 'delete', icon: 'Delete' }
  ]
};
const previewActions = computed(() =>
  previewActionDefinitions[pageKey.value].map((action) => ({
    ...action,
    label: currentPage.value.actionLabels[action.semantic]
  }))
);
const previewAppearance = computed(() => {
  const appearance = draft.value?.appearance || defaultListTableAppearance();
  const page = draft.value?.layout.pages[pageKey.value];
  return resolveArtListTableAppearance(appearance, {
    statusLabels: page ? Object.values(page.statusLabels) : [],
    actionLabels: page ? artListActionSlots[pageKey.value].flat().map((semantic) => page.actionLabels[semantic]) : []
  });
});
const previewTableStyle = computed(() => {
  if (previewMode.value === 'actual') return previewAppearance.value.style;
  const width = previewWidthColumns.value.reduce((sum, column) => sum + Number(column.width || column.minWidth || 0), 0);
  return {
    ...previewAppearance.value.style,
    width: `${Math.max(width, 680)}px`
  };
});
const previewRows = [{}];
const previewActionIcon = (icon: string) => (draft.value?.appearance.rowActionAppearance.iconVisible ? icon : undefined);
const previewStatusClass = (semantic: ArtListStatusSemantic) => (semantic === 'scoreDraft' ? 'score-draft' : semantic);
const previewStatusTagType = (semantic: ArtListStatusSemantic) => previewStatusTagTypes[semantic];
const previewStatusIcon = (semantic: ArtListStatusSemantic) => previewStatusIcons[semantic];
const buildAppearancePreviewAction = (page: ArtListTablePageKey, semantic: ArtListActionSemantic, disabled = false): AppearancePreviewAction => ({
  semantic,
  label: draft.value!.layout.pages[page].actionLabels[semantic],
  icon: previewActionIcons[semantic],
  disabled
});
const buildAppearancePreviewRows = (
  page: ArtListTablePageKey,
  statuses: ArtListStatusSemantic[],
  actions: (semantic: ArtListStatusSemantic) => AppearancePreviewAction[]
): AppearancePreviewRow[] =>
  statuses.map((status, index) => ({
    projectName: `项目名称示例 ${index + 1}`,
    status,
    statusLabel: draft.value!.layout.pages[page].statusLabels[status],
    actions: actions(status)
  }));
const appearancePreviewScenes = computed<AppearancePreviewScene[]>(() => {
  if (!draft.value) return [];
  return [
    {
      key: 'project',
      label: '类别上报',
      description: '编辑 / 查看、提交 / 撤回、删除',
      rows: buildAppearancePreviewRows('project', commonPreviewStatuses, (status) => {
        const editable = status === 'draft' || status === 'returned';
        const withdraw = status === 'pending' || status === 'approved';
        return [
          buildAppearancePreviewAction('project', editable ? 'edit' : 'view'),
          buildAppearancePreviewAction('project', withdraw ? 'withdraw' : 'submit', status === 'approved'),
          buildAppearancePreviewAction('project', 'delete', !editable)
        ];
      })
    },
    {
      key: 'schoolSubmit',
      label: '学校统一提交',
      description: '查看、提交 / 撤回、删除与禁用状态',
      rows: buildAppearancePreviewRows('schoolSubmit', commonPreviewStatuses, (status) => {
        const editable = status === 'draft' || status === 'returned';
        const withdraw = status === 'pending' || status === 'approved';
        return [
          buildAppearancePreviewAction('schoolSubmit', 'view'),
          buildAppearancePreviewAction('schoolSubmit', withdraw ? 'withdraw' : 'submit', status === 'approved'),
          buildAppearancePreviewAction('schoolSubmit', 'delete', !editable)
        ];
      })
    },
    {
      key: 'audit',
      label: '项目审核',
      description: '查看、退回、撤回保持固定槽位',
      rows: buildAppearancePreviewRows('audit', commonPreviewStatuses, (status) => [
        buildAppearancePreviewAction('audit', 'view'),
        buildAppearancePreviewAction('audit', 'return', status !== 'pending'),
        buildAppearancePreviewAction('audit', 'withdraw', !['returned', 'approved'].includes(status))
      ])
    },
    {
      key: 'review',
      label: '专家评分',
      description: '开始评分、修改评分、查看评分',
      rows: buildAppearancePreviewRows('review', reviewPreviewStatuses, (status) => {
        if (status === 'unscored') return [buildAppearancePreviewAction('review', 'score')];
        if (status === 'scoreDraft') return [buildAppearancePreviewAction('review', 'edit')];
        return [buildAppearancePreviewAction('review', 'view', status === 'locked')];
      })
    },
    {
      key: 'projectView',
      label: '上报进度',
      description: '查看、删除',
      rows: buildAppearancePreviewRows('projectView', commonPreviewStatuses, () => [
        buildAppearancePreviewAction('projectView', 'view'),
        buildAppearancePreviewAction('projectView', 'delete')
      ])
    }
  ];
});
const appearanceOverviewStatuses = computed(() =>
  statusColorOptions.map((item) => ({
    semantic: item.key,
    label:
      (reviewPreviewStatuses.includes(item.key) ? draft.value?.layout.pages.review : draft.value?.layout.pages.project)?.statusLabels[item.key] ||
      item.label
  }))
);
const appearanceOverviewActions = computed(() => [
  ...actionColorOptions.map((item) => ({
    key: item.key,
    semantic: item.key,
    label: item.label,
    icon: previewActionIcons[item.key],
    disabled: false
  })),
  {
    key: 'disabled-view',
    semantic: 'view' as const,
    label: '禁用示例',
    icon: previewActionIcons.view,
    disabled: true
  }
]);
const appearanceSemanticOverviewRows = [
  { type: 'status' as const, label: '全部状态' },
  { type: 'action' as const, label: '全部操作' }
];
const appearanceOverview = computed(() =>
  resolveArtListTableAppearance(draft.value?.appearance || defaultListTableAppearance(), {
    statusLabels: appearanceOverviewStatuses.value.map((item) => item.label),
    actionLabels: appearanceOverviewActions.value.map((item) => item.label)
  })
);
const startPreviewTableDrag = (event: PointerEvent) => {
  if (previewMode.value !== 'inspection' || event.button !== 0) return;
  const target = event.target as HTMLElement;
  if (target.closest('button, input, textarea, select, .el-input, .el-select, .el-checkbox')) return;
  const viewport = event.currentTarget as HTMLElement;
  if (viewport.scrollWidth <= viewport.clientWidth + 1) return;
  previewTablePointerId = event.pointerId;
  previewTablePointerStartX = event.clientX;
  previewTableScrollStart = viewport.scrollLeft;
  previewTableDragging.value = true;
  viewport.setPointerCapture?.(event.pointerId);
};
const movePreviewTableDrag = (event: PointerEvent) => {
  if (!previewTableDragging.value || previewTablePointerId !== event.pointerId) return;
  const viewport = event.currentTarget as HTMLElement;
  viewport.scrollLeft = previewTableScrollStart - (event.clientX - previewTablePointerStartX);
  event.preventDefault();
};
const stopPreviewTableDrag = (event: PointerEvent) => {
  if (previewTablePointerId !== event.pointerId) return;
  const viewport = event.currentTarget as HTMLElement;
  if (viewport.hasPointerCapture?.(event.pointerId)) viewport.releasePointerCapture(event.pointerId);
  previewTablePointerId = undefined;
  previewTableDragging.value = false;
};
const handlePreviewTableWheel = (event: WheelEvent) => {
  if (previewMode.value !== 'inspection' || !event.shiftKey) return;
  const viewport = event.currentTarget as HTMLElement;
  viewport.scrollLeft += event.deltaY || event.deltaX;
  event.preventDefault();
};
const startAppearancePreviewDrag = (event: PointerEvent) => {
  if (event.button !== 0) return;
  const viewport = event.currentTarget as HTMLElement;
  if (viewport.scrollWidth <= viewport.clientWidth + 1) return;
  appearancePreviewPointerId = event.pointerId;
  appearancePreviewPointerStartX = event.clientX;
  appearancePreviewScrollStart = viewport.scrollLeft;
  appearancePreviewDragging.value = true;
  viewport.setPointerCapture?.(event.pointerId);
};
const moveAppearancePreviewDrag = (event: PointerEvent) => {
  if (!appearancePreviewDragging.value || appearancePreviewPointerId !== event.pointerId) return;
  const viewport = event.currentTarget as HTMLElement;
  viewport.scrollLeft = appearancePreviewScrollStart - (event.clientX - appearancePreviewPointerStartX);
  event.preventDefault();
};
const stopAppearancePreviewDrag = (event: PointerEvent) => {
  if (appearancePreviewPointerId !== event.pointerId) return;
  const viewport = event.currentTarget as HTMLElement;
  if (viewport.hasPointerCapture?.(event.pointerId)) viewport.releasePointerCapture(event.pointerId);
  appearancePreviewPointerId = undefined;
  appearancePreviewDragging.value = false;
};
const handleAppearancePreviewWheel = (event: WheelEvent) => {
  if (!event.shiftKey) return;
  const viewport = event.currentTarget as HTMLElement;
  viewport.scrollLeft += event.deltaY || event.deltaX;
  event.preventDefault();
};
const currentSnapshot = computed(() => (draft.value ? JSON.stringify(draft.value) : ''));
const isDirty = computed(() => Boolean(draft.value) && currentSnapshot.value !== originalSnapshot.value);
const saveActionLabel = computed(() => (draft.value?.appearance.globalColorOverride ? '保存并全局覆盖颜色' : '保存全局表格设置'));
const normalizeColor = (value: string) => value.trim().toLowerCase();
const matchesColorPreset = (appearance: ArtListTableAppearanceConfig, preset: ArtListColorPreset) =>
  baseColorFields.every((field) => normalizeColor(appearance[field]) === normalizeColor(preset.colors[field])) &&
  statusColorOptions.every(({ key }) => normalizeColor(appearance.statusAppearance.colors[key]) === normalizeColor(preset.statusColors[key])) &&
  actionColorOptions.every(({ key }) => normalizeColor(appearance.rowActionAppearance.colors[key]) === normalizeColor(preset.actionColors[key]));
const activeColorPreset = computed(() => {
  if (!draft.value) return undefined;
  return colorPresets.find((preset) => matchesColorPreset(draft.value!.appearance, preset));
});
const presetSwatches = (preset: ArtListColorPreset) => [
  preset.colors.headerBackground,
  preset.actionColors.view,
  preset.statusColors.approved,
  preset.statusColors.pending
];

const columnVariable = (column: ArtReviewListColumnConfig) => (column.source === 'form' ? `{form.${column.fieldKey}}` : `{${column.key}}`);
const columnFixedLabel = (fixed: ArtReviewListColumnConfig['fixed']) => ({ left: '固定左侧', right: '固定右侧', none: '中间区域' })[fixed || 'none'];
const columnFixedHint = (column: ArtReviewListColumnConfig) =>
  column.key === 'actions'
    ? '操作列可显示或隐藏，可选择固定右侧或放入中间区域'
    : isReviewGroupColumn(column)
      ? '组别列必须显示，可排序并可设置固定位置'
      : '可在当前固定分区内拖拽或使用上下按钮排序';
const fixedOptions = (column: ArtReviewListColumnConfig): Array<{ label: string; value: ArtListColumnFixed }> => {
  if (column.key === 'selection') {
    return [
      { label: '不固定', value: 'none' },
      { label: '固定左侧', value: 'left' }
    ];
  }
  if (column.key === 'actions') {
    return [
      { label: '不固定', value: 'none' },
      { label: '固定右侧', value: 'right' }
    ];
  }
  return [
    { label: '不固定', value: 'none' },
    { label: '固定左侧', value: 'left' },
    { label: '固定右侧', value: 'right' }
  ];
};
const sampleValue = (column: ArtReviewListColumnConfig) =>
  ({
    selection: '□',
    serial: '1',
    projectNo: 'AR20260001',
    projectName: '项目名称示例',
    activityName: '艺术展演',
    categoryName: '声乐',
    groupName: '甲组',
    groupOrNature: '甲组',
    programForm: '小合唱',
    schoolName: '示例学校',
    submittedAt: '2026-07-23',
    updateTime: '2026-07-23',
    currentAuditOpinion: '审核意见',
    scoreMode: '百分制',
    scoreResult: '90',
    scoreSubmittedAt: '2026-07-23'
  })[column.key] || (column.source === 'form' ? '类别字段示例' : '示例');

const startHeaderEdit = (column: ArtReviewListColumnConfig | ArtListRuntimeColumn | PreviewStructuralColumn) => {
  if (column.source === 'structural') return;
  selectedKey.value = column.key;
  editingHeaderKey.value = column.key;
  editingHeaderValue.value = column.label;
  nextTick(() => {
    headerInputRef.value?.focus();
    headerInputRef.value?.select();
  });
};
const cancelHeaderEdit = () => {
  editingHeaderKey.value = '';
  editingHeaderValue.value = '';
};
const commitHeaderEdit = () => {
  if (!editingHeaderKey.value) return;
  const column = activeColumns.value.find((item) => item.key === editingHeaderKey.value && !item.deleted);
  const label = editingHeaderValue.value.trim();
  if (column && label) column.label = label;
  cancelHeaderEdit();
};

const normalizeActiveColumnOrder = () => {
  const normalized = sortArtListColumnsByFixedZone(activeColumns.value);
  activeColumns.value.splice(0, activeColumns.value.length, ...normalized);
};
const canMoveColumn = (index: number, offset: number) => {
  const column = editableColumns.value[index];
  const target = editableColumns.value[index + offset];
  return Boolean(column && target && column.fixed === target.fixed);
};
const moveColumnByOffset = (index: number, offset: number) => {
  if (!canMoveColumn(index, offset)) return;
  dragIndex.value = index;
  moveColumn(index + offset);
};
const moveColumn = (targetIndex: number) => {
  if (dragIndex.value === undefined || dragIndex.value === targetIndex) return;
  const sourceColumn = editableColumns.value[dragIndex.value];
  const targetColumn = editableColumns.value[targetIndex];
  if (!sourceColumn || !targetColumn || sourceColumn.fixed !== targetColumn.fixed) {
    dragIndex.value = undefined;
    return;
  }
  const sourceIndex = activeColumns.value.findIndex((column) => column.key === sourceColumn.key);
  const actualTarget = activeColumns.value.findIndex((column) => column.key === targetColumn.key);
  const [column] = activeColumns.value.splice(sourceIndex, 1);
  activeColumns.value.splice(actualTarget, 0, column);
  dragIndex.value = undefined;
};
const deleteSelectedColumn = () => {
  if (!selectedColumn.value || isDeletionLocked(selectedColumn.value)) return;
  selectedColumn.value.visible = false;
  selectedColumn.value.deleted = true;
  selectedKey.value = '';
};
const restoreSelectedColumnDefaults = () => {
  const selected = selectedColumn.value;
  if (!selected) return;
  const pageDefaults = scope.value === 'category' ? currentPage.value.columns : defaultsForPage.value;
  let restored = pageDefaults.find((column) => column.key === selected.key);
  if (!restored && selected.source === 'form') {
    const field = fields.value.find((item) => String(item.fieldKey) === String(selected.fieldKey));
    restored = {
      key: selected.key,
      source: 'form',
      fieldKey: selected.fieldKey,
      label: field?.fieldLabel || selected.fieldKey || selected.label,
      visible: true,
      deleted: false,
      width: 160,
      minWidth: 90,
      fixed: 'none'
    };
  }
  if (!restored) return;
  const replacement = JSON.parse(JSON.stringify(restored)) as ArtReviewListColumnConfig;
  const currentIndex = activeColumns.value.findIndex((column) => column.key === selected.key);
  if (currentIndex >= 0) activeColumns.value.splice(currentIndex, 1);
  const defaultOrder = pageDefaults.findIndex((column) => column.key === replacement.key);
  let insertAt = activeColumns.value.findIndex((column) => {
    if (replacement.source === 'form') return column.key === 'actions';
    const order = pageDefaults.findIndex((item) => item.key === column.key);
    return order >= 0 && defaultOrder >= 0 && order > defaultOrder;
  });
  if (insertAt < 0) {
    const actionsIndex = activeColumns.value.findIndex((column) => column.key === 'actions');
    insertAt = replacement.key === 'actions' || actionsIndex < 0 ? activeColumns.value.length : actionsIndex;
  }
  activeColumns.value.splice(insertAt, 0, replacement);
  selectedKey.value = replacement.key;
  ElMessage.success(scope.value === 'category' ? '该列已恢复页面默认草稿，保存后生效' : '该列已恢复系统默认草稿，保存后生效');
};
const addColumn = (rawKey: unknown) => {
  const key = String(rawKey || '');
  if (!key) return;
  const restored = activeColumns.value.find((column) => column.key === key);
  if (restored) {
    restored.deleted = false;
    restored.visible = true;
    selectedKey.value = restored.key;
    columnToAdd.value = '';
    return;
  }
  const builtin = defaultsForPage.value.find((column) => column.key === key);
  let column: ArtReviewListColumnConfig | undefined;
  if (builtin) column = JSON.parse(JSON.stringify(builtin));
  if (key.startsWith('field:')) {
    const fieldKey = key.slice(6);
    const field = fields.value.find((item) => String(item.fieldKey) === fieldKey);
    if (field) {
      column = {
        key,
        source: 'form',
        fieldKey,
        label: field.fieldLabel || fieldKey,
        visible: true,
        deleted: false,
        width: 160,
        minWidth: 90,
        fixed: 'none'
      };
    }
  }
  if (column) {
    const actionsIndex = activeColumns.value.findIndex((item) => item.key === 'actions');
    activeColumns.value.splice(actionsIndex < 0 ? activeColumns.value.length : actionsIndex, 0, column);
    selectedKey.value = column.key;
  }
  columnToAdd.value = '';
};
const createCategoryOverride = () => {
  if (!categoryId.value || currentPage.value.categoryColumns[categoryId.value]) return;
  currentPage.value.categoryColumns[categoryId.value] = JSON.parse(JSON.stringify(currentPage.value.columns));
  selectedKey.value = currentPage.value.categoryColumns[categoryId.value][0]?.key || '';
  ElMessage.success('已从页面默认创建类别覆盖草稿，保存后生效');
};
const removeCategoryOverride = async () => {
  if (!categoryId.value) return;
  delete currentPage.value.categoryColumns[categoryId.value];
  selectedKey.value = '';
  ElMessage.success('该类别已恢复页面默认列方案，保存后生效');
};
const loadCategories = async () => {
  categoryId.value = '';
  fields.value = [];
  groupRuleOptions.value = [];
  rawCategories.value = [];
  if (!activityId.value) return;
  const [{ data }] = await Promise.all([listCategory(activityId.value, { deleted: false }), loadGroupRuleOptions()]);
  rawCategories.value = data || [];
};
const loadCategoryFields = async () => {
  fields.value = [];
  selectedKey.value = '';
  if (!categoryId.value) {
    await loadGroupRuleOptions();
    return;
  }
  const selected = categories.value.find((item) => String(item.id) === categoryId.value);
  if (!selected || selected.disabled) {
    categoryId.value = '';
    ElMessage.warning('请选择末级可报送小类别');
    return;
  }
  void activeColumns.value;
  const [{ data }] = await Promise.all([listArtDetailDisplayCategoryFields(categoryId.value), loadGroupRuleOptions(categoryId.value)]);
  fields.value = (data || []).filter((field) => field.fieldKey && field.delFlag !== '1');
};
const loadGroupRuleOptions = async (selectedCategoryId?: string) => {
  groupRuleOptions.value = [];
  groupOptionsLoadFailed.value = false;
  if (!activityId.value) return;
  groupOptionsLoading.value = true;
  try {
    const { data } = await listActivityReportRuleSchoolOptions({
      activityId: activityId.value,
      categoryId: selectedCategoryId || undefined,
      enabled: true
    });
    groupRuleOptions.value = (data || []).filter((item: ActivityRuleGroupOptionVO) => {
      const rule = (() => {
        try {
          return item.ruleJson ? JSON.parse(item.ruleJson) : {};
        } catch {
          return {};
        }
      })() as Record<string, unknown>;
      const targetFieldKey = String(rule.targetFieldKey || rule.groupFieldKey || '').trim();
      return Boolean(item.groupCode || item.groupName) && (!targetFieldKey || targetFieldKey === 'groupCode' || targetFieldKey === '__group_code');
    });
  } catch {
    groupOptionsLoadFailed.value = true;
  } finally {
    groupOptionsLoading.value = false;
  }
};
const initialize = async () => {
  loading.value = true;
  try {
    const [tableConfig, activityRes] = await Promise.all([loadArtListTableConfig(), listActivityOptions()]);
    draft.value = JSON.parse(JSON.stringify(tableConfig));
    originalSnapshot.value = JSON.stringify(draft.value);
    activities.value = activityRes.data || [];
    selectedKey.value = currentPage.value.columns.find((column) => column.visible)?.key || '';
  } finally {
    loading.value = false;
  }
};
const restoreDefaults = async (confirm = true) => {
  const confirmation =
    mode.value === 'appearance'
      ? '确认把全局颜色与公共样式恢复为系统默认草稿？页面布局不会改变，保存后才会生效。'
      : mode.value === 'layout'
        ? '确认把五类表格列方案恢复为系统默认草稿？全局颜色不会改变，保存后才会生效。'
        : '确认把全局颜色和五类表格列方案恢复为系统默认草稿？保存后才会生效。';
  if (confirm) {
    try {
      await ElMessageBox.confirm(confirmation, '恢复系统默认', {
        confirmButtonText: '恢复默认',
        cancelButtonText: '取消',
        type: 'warning'
      });
    } catch {
      return;
    }
  }
  if (!draft.value) return;
  if (mode.value === 'appearance') {
    draft.value.appearance = defaultListTableAppearance();
  } else if (mode.value === 'layout') {
    draft.value.layout.pages[pageKey.value] = defaultArtListTableLayout().pages[pageKey.value];
  } else {
    draft.value = {
      appearance: defaultListTableAppearance(),
      layout: defaultArtListTableLayout()
    };
  }
  selectedKey.value = '';
};
const applyColorPreset = (key: ArtListColorPresetKey) => {
  if (!draft.value) return;
  const preset = colorPresets.find((item) => item.key === key);
  if (!preset) return;
  Object.assign(draft.value.appearance, preset.colors);
  draft.value.appearance.statusAppearance.colors = { ...preset.statusColors };
  draft.value.appearance.rowActionAppearance.colors = { ...preset.actionColors };
  draft.value.appearance.globalColorOverride = true;
};
const cancelChanges = () => {
  if (!draft.value) return;
  draft.value = JSON.parse(originalSnapshot.value) as ArtListTableConfigVO;
};
const saveAll = async () => {
  if (!draft.value || !isDirty.value) return;
  if (draft.value.appearance.globalColorOverride) {
    try {
      await ElMessageBox.confirm('保存后将全局覆盖五类业务表格的颜色样式，确认继续？', '全局覆盖颜色', {
        confirmButtonText: '保存并覆盖',
        cancelButtonText: '取消',
        type: 'warning'
      });
    } catch {
      return;
    }
  }
  saving.value = true;
  try {
    const saved = await saveArtListTableConfig(draft.value);
    draft.value = JSON.parse(JSON.stringify(saved));
    originalSnapshot.value = JSON.stringify(draft.value);
    ElMessage.success(draft.value.appearance.globalColorOverride ? '全局表格设置已保存，颜色已覆盖五类表格' : '全局表格设置已保存');
  } finally {
    saving.value = false;
  }
};

watch(pageKey, () => {
  cancelHeaderEdit();
  selectedKey.value = currentPage.value.columns.find((column) => column.visible)?.key || '';
});
watch(scope, () => {
  cancelHeaderEdit();
  selectedKey.value = '';
});
onMounted(() => void initialize());
defineExpose({ cancelChanges, initialize, isDirty, loading, restoreDefaults, saveActionLabel, saveAll, saving });
</script>

<style scoped>
.global-table-editor {
  display: grid;
  gap: 10px;
}

.global-table-editor.is-compact :deep(.el-tabs__content) {
  padding: 10px;
}

.global-table-editor.is-compact :deep(.el-button) {
  --el-component-size: 28px;

  padding: 5px 9px;
}

.global-table-editor :deep(.el-tabs.is-single-section > .el-tabs__header) {
  display: none;
}

.global-table-editor__toolbar,
.global-table-editor__footer,
.global-table-panel > header,
.global-table-preview > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.global-table-editor__toolbar > div,
.global-table-panel > header,
.global-table-preview > header > span {
  display: grid;
  gap: 3px;
}

.global-table-editor small,
.global-table-panel small,
.global-table-preview small {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.global-table-scope {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.global-table-scope .el-select {
  width: 230px;
}

.global-table-workspace {
  display: grid;
  grid-template-areas:
    'preview preview'
    'columns properties';
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
}

.global-table-panel,
.global-table-preview,
.global-appearance-preview {
  min-width: 0;
  padding: 9px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
}

.global-table-workspace > .global-table-panel:first-child {
  grid-area: columns;
  max-height: 430px;
  overflow: auto;
}

.global-table-preview {
  grid-area: preview;
}

.global-table-column-item.is-protected {
  border-style: dashed;
}

.global-table-group-options {
  margin-bottom: 14px;
}

.global-table-group-options__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0 8px;
}

.global-table-group-options > small {
  display: block;
  line-height: 1.6;
}

.global-table-column-item {
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr) 60px auto;
  align-items: center;
  gap: 6px;
  width: 100%;
  min-height: 5mm;
  margin-top: 4px;
  padding: 2px 6px;
  text-align: left;
  color: var(--el-text-color-primary);
  background: var(--el-fill-color-extra-light);
  border: 1px solid transparent;
  border-radius: 8px;
  cursor: pointer;
}

.global-table-column-item__move {
  display: inline-grid;
  grid-template-columns: repeat(2, 28px);
  gap: 4px;
}

.global-table-column-item__move :deep(.el-button + .el-button) {
  margin-left: 0;
}

.global-table-column-item__move :deep(.el-button) {
  width: 28px;
  height: 24px;
  min-height: 24px;
  padding: 0;
  font-size: 14px;
  font-weight: 700;
  border-color: var(--el-border-color);
  border-radius: 5px;
}

.global-table-column-item__copy {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
}

.global-table-column-item__copy strong {
  flex: none;
  font-size: 13px;
}

.global-table-column-item__copy small {
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  text-overflow: ellipsis;
}

.global-table-column-item em {
  min-width: 38px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  font-style: normal;
  text-align: right;
  white-space: nowrap;
}

.global-table-column-item.is-selected {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.global-table-column-item.is-hidden {
  opacity: 0.55;
}

.global-table-panel > .el-select {
  margin-top: 12px;
}

.global-table-preview__shell {
  max-height: clamp(170px, 20vh, 220px);
  margin-top: 8px;
  overflow: auto;
  border-radius: 8px;
}

.global-table-preview__shell.is-inspection {
  overflow-x: auto;
  cursor: grab;
  touch-action: pan-y;
}

.global-table-preview__shell.is-inspection.is-dragging {
  cursor: grabbing;
  user-select: none;
}

.global-table-preview__table,
.global-appearance-preview__table {
  width: 100%;
}

.global-table-preview__header-button {
  width: 100%;
  min-width: 0;
  padding: 6px 8px;
  overflow: hidden;
  color: inherit;
  font: inherit;
  font-weight: inherit;
  text-align: inherit;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: transparent;
  border: 0;
  border-radius: 4px;
  cursor: text;
}

.global-table-preview__header-button:hover,
.global-table-preview__header-button.is-selected {
  color: var(--el-color-primary);
  background: color-mix(in srgb, var(--el-color-primary) 10%, transparent);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--el-color-primary) 45%, transparent);
}

.global-table-preview__header-input {
  width: 100%;
}

.global-table-preview :deep(.el-table__header .cell) {
  padding: 0 4px;
}

.global-table-preview__shell.is-inspection :deep(.el-table__body-wrapper .el-scrollbar__bar) {
  opacity: 1;
}

.global-appearance-preview__table {
  margin-top: 0;
}

.global-appearance-preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 700px;
  overflow: hidden;
}

.global-appearance-preview__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.global-appearance-preview__heading > span {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.global-appearance-preview__viewport {
  flex: 1;
  min-width: 0;
  overflow: auto;
  cursor: grab;
  touch-action: pan-y;
  overscroll-behavior-inline: contain;
  scrollbar-gutter: stable;
}

.global-appearance-preview__viewport.is-dragging {
  cursor: grabbing;
  user-select: none;
}

.global-appearance-preview__board {
  display: grid;
  grid-template-columns: repeat(2, minmax(440px, 1fr));
  gap: 12px;
  min-width: 920px;
  padding: 0 2px 4px;
}

.global-appearance-preview__scene {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.global-appearance-preview__scene > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-height: 52px;
  padding: 9px 11px;
  background: var(--el-fill-color-extra-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.global-appearance-preview__scene > header > span {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.global-appearance-preview__scene > header em {
  flex: none;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  font-style: normal;
  white-space: nowrap;
}

.global-appearance-preview__semantic-list {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 10px;
  min-height: 32px;
  padding: 4px 0;
}

.global-appearance-preview__scene :deep(.el-table__cell) {
  padding-top: 5px;
  padding-bottom: 5px;
}

.global-appearance-preview__scene :deep(.art-list-row-actions) {
  justify-content: center;
}

.global-table-properties {
  grid-area: properties;
  max-height: 430px;
  overflow: auto;
  container-type: inline-size;
}

.global-table-property-grid,
.global-table-fixed-text-grid {
  display: grid;
  gap: 0 12px;
}

.global-table-property-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.global-table-fixed-text-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.global-table-property-grid :deep(.el-form-item),
.global-table-fixed-text-grid :deep(.el-form-item) {
  min-width: 0;
  margin-bottom: 10px;
}

.global-table-property-grid :deep(.el-select),
.global-table-property-grid :deep(.el-slider),
.global-table-fixed-text-grid :deep(.el-input) {
  width: 100%;
}

.global-table-property-grid > .global-table-group-options,
.global-table-property-actions {
  grid-column: 1 / -1;
}

.global-table-property-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.global-table-property-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.global-appearance-grid {
  display: grid;
  grid-template-columns: minmax(460px, 1fr) 360px;
  gap: 14px;
}

.global-appearance-form {
  max-height: 700px;
  overflow: auto;
}

.global-color-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 10px 0 16px;
}

.global-color-grid label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.global-recommended-colors {
  display: grid;
  gap: 10px;
  margin: 8px 0 4px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}

.global-recommended-colors__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.global-recommended-colors__heading > span {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.global-color-presets {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.global-color-preset {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 9px 10px;
  color: var(--el-text-color-primary);
  text-align: left;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 7px;
  cursor: pointer;
  transition:
    border-color 0.18s ease,
    background-color 0.18s ease,
    box-shadow 0.18s ease;
}

.global-color-preset:hover {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.global-color-preset.is-active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  box-shadow: inset 0 0 0 1px var(--el-color-primary-light-7);
}

.global-color-preset__copy {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.global-color-preset__copy small,
.global-recommended-colors__hint {
  color: var(--el-text-color-secondary);
}

.global-color-preset__swatches {
  display: flex;
  gap: 5px;
}

.global-color-preset__swatches i {
  width: 16px;
  height: 16px;
  border: 1px solid rgb(15 23 42 / 10%);
  border-radius: 50%;
  box-shadow: 0 0 0 1px rgb(255 255 255 / 75%);
}

.global-recommended-colors__hint {
  line-height: 1.5;
}

.global-table-editor__footer {
  position: sticky;
  z-index: 8;
  bottom: 0;
  padding: 12px 14px;
  color: var(--el-text-color-secondary);
  background: color-mix(in srgb, var(--el-bg-color) 96%, transparent);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  box-shadow: 0 -8px 24px rgb(15 23 42 / 6%);
  backdrop-filter: blur(8px);
}

.global-table-editor__footer > div {
  display: flex;
  gap: 10px;
}

@media (max-width: 860px) {
  .global-table-editor__toolbar,
  .global-table-editor__footer {
    align-items: stretch;
    flex-direction: column;
  }

  .global-table-workspace {
    grid-template-areas:
      'preview'
      'columns'
      'properties';
  }

  .global-table-workspace,
  .global-appearance-grid {
    grid-template-columns: 1fr;
  }

  .global-table-workspace > .global-table-panel:first-child,
  .global-table-properties,
  .global-appearance-preview,
  .global-appearance-form {
    max-height: none;
  }
}

@container (min-width: 640px) {
  .global-table-fixed-text-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@container (max-width: 460px) {
  .global-table-property-grid,
  .global-table-fixed-text-grid {
    grid-template-columns: 1fr;
  }
}
</style>
