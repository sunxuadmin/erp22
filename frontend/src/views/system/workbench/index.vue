<template>
  <div class="p-2 workbench-config-page" :style="{ '--workbench-config-sticky-offset': `${workbenchControlHeight + 10}px` }">
    <section ref="workbenchControlPanelRef" class="workbench-config-control" aria-label="工作台配置控制台">
      <header class="workbench-config-toolbar">
        <strong class="workbench-config-toolbar__title">工作台配置</strong>
        <label
          v-hasPermi="['crehn:detailDisplayConfig:edit']"
          class="workbench-config-toolbar__activity"
          title="未单独选择时，默认跟随“报送管理 → 活动配置”中状态为启用的当前活动"
        >
          <span>业务活动</span>
          <el-select
            v-model="toolbarManagedActivityId"
            size="small"
            filterable
            placeholder="请选择活动"
            :loading="managedActivitySaving"
            :disabled="!managedActivityOptions.length || managedActivitySaving || currentScopeDirty || currentScopeSaving || currentScopeRestoring"
            @change="saveToolbarManagedActivity"
          >
            <el-option
              v-for="activity in managedActivityOptions"
              :key="String(activity.id)"
              :label="activity.activityName || '活动名称未配置'"
              :value="String(activity.id)"
            />
          </el-select>
        </label>
        <div class="workbench-config-toolbar__actions">
          <span class="workbench-config-save-state" :class="{ 'is-dirty': currentScopeDirty }">
            <el-icon><WarningFilled v-if="currentScopeDirty" /><CircleCheckFilled v-else /></el-icon>
            {{ currentScopeDirty ? '有未保存修改' : activeWorkbenchTab === 'business' ? '当前页签已保存' : '当前范围已保存' }}
          </span>
          <el-button
            size="small"
            icon="Refresh"
            :loading="currentScopeLoading"
            :disabled="!canRefreshCurrentScope"
            :title="canRefreshCurrentScope ? '重新加载当前范围' : '当前范围不可刷新'"
            @click="refreshActiveWorkbenchConfig"
          >
            刷新
          </el-button>
          <el-button
            v-hasPermi="currentScopeSavePermission"
            size="small"
            type="warning"
            plain
            icon="RefreshLeft"
            :loading="currentScopeRestoring"
            :disabled="!canRestoreCurrentScope"
            :title="canRestoreCurrentScope ? '恢复当前精确范围的系统默认草稿' : '当前范围不支持恢复默认'"
            @click="restoreCurrentScope"
          >
            恢复系统默认
          </el-button>
          <el-button
            v-hasPermi="currentScopeSavePermission"
            size="small"
            :disabled="!canCancelCurrentScope"
            title="放弃当前精确范围尚未保存的修改"
            @click="cancelCurrentScope"
          >
            取消本次修改
          </el-button>
          <el-button
            v-hasPermi="currentScopeSavePermission"
            size="small"
            type="primary"
            icon="Check"
            :loading="currentScopeSaving"
            :disabled="!canSaveCurrentScope"
            :title="canSaveCurrentScope ? '保存当前精确范围' : '当前范围没有可保存的修改'"
            @click="saveCurrentScope"
          >
            {{ currentSaveActionLabel }}
          </el-button>
        </div>
      </header>

      <section class="workbench-config-selectors" aria-label="当前配置范围">
        <section class="workbench-config-selector-group">
          <header class="workbench-config-selector-group__header">
            <span>配置范围</span>
            <span class="workbench-config-selector-group__current"
              >当前：<strong>{{ currentSectionLabel }}</strong></span
            >
            <el-button
              text
              size="small"
              :icon="sectionOptionsExpanded ? 'ArrowUp' : 'ArrowDown'"
              :aria-expanded="sectionOptionsExpanded"
              @click="sectionOptionsExpanded = !sectionOptionsExpanded"
            >
              {{ sectionOptionsExpanded ? '折叠' : '展开' }}
            </el-button>
          </header>
          <el-collapse-transition>
            <nav v-show="sectionOptionsExpanded" class="workbench-config-selector-options" aria-label="工作台配置范围">
              <el-tooltip v-for="section in workbenchSections" :key="section.key" :content="section.label" placement="top" :show-after="350">
                <button
                  type="button"
                  :class="{ 'is-active': activeWorkbenchTab === section.key }"
                  :disabled="currentScopeSaving || currentScopeRestoring"
                  :title="hasSectionPermission(section) ? section.label : CONFIG_PERMISSION_HINT"
                  @click="switchWorkbenchSectionGuarded(section)"
                >
                  <span>{{ section.shortLabel }}</span>
                  <el-icon v-if="sectionDirtyMap[section.key]" class="workbench-config-nav__dirty"><WarningFilled /></el-icon>
                </button>
              </el-tooltip>
            </nav>
          </el-collapse-transition>
        </section>

        <section class="workbench-config-selector-group">
          <header class="workbench-config-selector-group__header">
            <span>{{ currentContextHeading }}</span>
            <span class="workbench-config-selector-group__current"
              >当前：<strong>{{ currentContextLabel }}</strong></span
            >
            <el-button
              text
              size="small"
              :icon="contextOptionsExpanded ? 'ArrowUp' : 'ArrowDown'"
              :aria-expanded="contextOptionsExpanded"
              @click="contextOptionsExpanded = !contextOptionsExpanded"
            >
              {{ contextOptionsExpanded ? '折叠' : '展开' }}
            </el-button>
          </header>
          <el-collapse-transition>
            <div v-show="contextOptionsExpanded" class="workbench-config-selector-options">
              <template v-if="isRoleScopedSection">
                <el-tooltip v-for="role in roleOptions" :key="role.roleId" :content="role.roleName" placement="top" :show-after="350">
                  <button
                    type="button"
                    role="radio"
                    :class="{ 'is-active': String(role.roleId) === String(selectedRoleId) }"
                    :aria-checked="String(role.roleId) === String(selectedRoleId)"
                    :title="role.roleName"
                    :disabled="loading || currentScopeSaving || currentScopeRestoring"
                    @click="handleRoleSelection(role.roleId)"
                  >
                    {{ compactRoleLabel(role) }}
                  </button>
                </el-tooltip>
              </template>
              <template v-else-if="activeWorkbenchTab === 'business'">
                <el-tooltip v-for="page in businessPageOptions" :key="page.value" :content="page.label" placement="top" :show-after="350">
                  <button
                    type="button"
                    role="tab"
                    :aria-selected="businessPageKey === page.value"
                    :class="{ 'is-active': businessPageKey === page.value }"
                    :title="page.label"
                    :disabled="currentScopeSaving"
                    @click="switchBusinessPage(page.value)"
                  >
                    {{ page.shortLabel }}
                  </button>
                </el-tooltip>
              </template>
              <template v-else-if="activeWorkbenchTab === 'style'">
                <el-tooltip v-for="item in publicStyleSectionOptions" :key="item.value" :content="item.label" placement="top" :show-after="350">
                  <button
                    type="button"
                    role="tab"
                    :aria-selected="publicStyleSection === item.value"
                    :class="{ 'is-active': publicStyleSection === item.value }"
                    :title="item.label"
                    :disabled="currentScopeSaving"
                    @click="publicStyleSection = item.value"
                  >
                    {{ item.shortLabel }}
                    <el-icon v-if="publicStyleDirtyMap[item.value]" class="workbench-config-nav__dirty"><WarningFilled /></el-icon>
                  </button>
                </el-tooltip>
              </template>
              <button v-else type="button" class="is-active" disabled title="配置包迁移">迁移设置</button>
            </div>
          </el-collapse-transition>
        </section>
      </section>
    </section>

    <div class="workbench-config-shell">
      <main class="workbench-config-main">
        <el-card v-if="activeWorkbenchTab === 'shell'" v-loading="roleShellLoading" shadow="never" class="workbench-config-panel">
          <template #header>
            <div class="panel-header">
              <div>
                <span>角色系统外壳</span>
                <small>点击预览区域展开对应配置；由管理员按角色统一下发，登录后优先于浏览器本地设置</small>
              </div>
            </div>
          </template>
          <el-alert
            class="mb-4"
            :title="
              roleShellForm.configured
                ? '当前角色已启用后台统一布局配置'
                : '当前角色尚未保存后台配置；除首页导航默认值外，继续沿用用户本地或系统默认布局'
            "
            :type="roleShellForm.configured ? 'success' : 'info'"
            :closable="false"
            show-icon
          />
          <RoleShellVisualEditor :model="roleShellForm" :activity-options="brandActivityOptions" :role-name="selectedRoleName" />
        </el-card>

        <template v-if="activeWorkbenchTab === 'style'">
          <el-card shadow="never" class="workbench-config-panel">
            <template #header>
              <div class="panel-header">
                <div>
                  <span>公共样式</span>
                  <small>颜色与全局样式共享给全部角色</small>
                </div>
              </div>
            </template>
            <section v-show="publicStyleSection === 'workbench'" class="workbench-style-section">
              <div class="panel-header">
                <div>
                  <span>全局工作台样式</span>
                  <small>后台首页与左侧菜单全局生效，支持预设和自定义渐变</small>
                </div>
                <div class="style-actions">
                  <el-button text :icon="workbenchStyleExpanded ? 'ArrowUp' : 'ArrowDown'" @click="workbenchStyleExpanded = !workbenchStyleExpanded">
                    {{ workbenchStyleExpanded ? '收起' : '展开' }}
                  </el-button>
                </div>
              </div>
              <el-row v-if="workbenchStyleExpanded" :gutter="16" class="style-config-row">
                <el-col :xs="24" :md="16">
                  <el-form :model="styleForm" label-width="106px">
                    <el-row :gutter="14">
                      <el-col :xs="24" :md="24">
                        <el-divider content-position="left">全局显示比例</el-divider>
                      </el-col>
                      <el-col :xs="24" :md="12">
                        <el-form-item label="布局模式">
                          <el-select v-model="styleForm.layoutScaleMode" class="w-full">
                            <el-option label="自动适配（推荐）" value="auto" />
                            <el-option label="固定默认（100%）" value="default" />
                            <el-option label="固定稍小（90%）" value="small" />
                            <el-option label="固定较大（110%）" value="large" />
                            <el-option label="自定义" value="custom" />
                          </el-select>
                        </el-form-item>
                      </el-col>
                      <el-col v-if="styleForm.layoutScaleMode === 'custom'" :xs="24" :md="12">
                        <el-form-item label="显示比例">
                          <el-input-number v-model="styleForm.customScalePercent" :min="75" :max="110" :step="5" controls-position="right" />
                          <span class="ml-2">%</span>
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="24">
                        <el-alert
                          v-if="styleForm.layoutScaleMode === 'auto'"
                          type="info"
                          :closable="false"
                          show-icon
                          title="自动模式根据未缩放前的有效内容宽度，在 75%～100% 之间按 5% 档位调整；移动端保持 100%。"
                        />
                        <el-alert
                          v-else-if="styleForm.layoutScaleMode === 'custom' && styleForm.customScalePercent < 85"
                          type="warning"
                          :closable="false"
                          show-icon
                          title="低于 85% 可能导致文字偏小，请重点检查弹窗、下拉框、固定表头和抽屉。"
                        />
                      </el-col>
                      <el-col :xs="24" :md="24">
                        <el-divider content-position="left">背景与配色</el-divider>
                      </el-col>
                      <el-col :xs="24" :md="12">
                        <el-form-item label="启用背景">
                          <el-switch v-model="styleForm.enabled" active-text="启用" inactive-text="关闭" />
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="12">
                        <el-form-item label="预设样式">
                          <el-select v-model="styleForm.preset" class="w-full" @change="applyStylePreset">
                            <el-option
                              v-for="item in workbenchStylePresets"
                              :key="item.value.preset"
                              :label="item.label"
                              :value="item.value.preset"
                            />
                            <el-option label="自定义" value="custom" />
                          </el-select>
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="24">
                        <el-form-item label="页面背景">
                          <el-input
                            v-model="styleForm.pageBackground"
                            type="textarea"
                            :rows="2"
                            maxlength="160"
                            show-word-limit
                            placeholder="linear-gradient(180deg,#f5f9ff 0%,#f8fbff 44%,#ffffff 100%)"
                          />
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="12">
                        <el-form-item label="卡片底色">
                          <div class="color-control">
                            <el-color-picker v-model="styleForm.cardBackground" show-alpha />
                            <el-input v-model="styleForm.cardBackground" maxlength="40" />
                          </div>
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="12">
                        <el-form-item label="卡片边框">
                          <div class="color-control">
                            <el-color-picker v-model="styleForm.cardBorder" show-alpha />
                            <el-input v-model="styleForm.cardBorder" maxlength="40" />
                          </div>
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="12">
                        <el-form-item label="强调色">
                          <div class="color-control">
                            <el-color-picker v-model="styleForm.accentColor" show-alpha />
                            <el-input v-model="styleForm.accentColor" maxlength="40" />
                          </div>
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="12">
                        <el-form-item label="标题色">
                          <div class="color-control">
                            <el-color-picker v-model="styleForm.headingColor" show-alpha />
                            <el-input v-model="styleForm.headingColor" maxlength="40" />
                          </div>
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="24">
                        <el-form-item label="日历渐变">
                          <el-input
                            v-model="styleForm.calendarBackground"
                            maxlength="120"
                            show-word-limit
                            placeholder="linear-gradient(180deg,#2563eb 0%,#0f62b9 100%)"
                          />
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="24">
                        <el-form-item label="卡片阴影">
                          <el-input v-model="styleForm.shadow" maxlength="90" show-word-limit placeholder="0 4px 14px rgba(37,99,235,0.05)" />
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="24">
                        <el-divider content-position="left">左侧菜单样式</el-divider>
                      </el-col>
                      <el-col :xs="24" :md="12">
                        <el-form-item label="菜单背景">
                          <div class="color-control">
                            <el-color-picker v-model="styleForm.sidebarBackground" show-alpha />
                            <el-input v-model="styleForm.sidebarBackground" maxlength="140" />
                          </div>
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="12">
                        <el-form-item label="菜单边框">
                          <div class="color-control">
                            <el-color-picker v-model="styleForm.sidebarBorderColor" show-alpha />
                            <el-input v-model="styleForm.sidebarBorderColor" maxlength="40" />
                          </div>
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="12">
                        <el-form-item label="菜单项底色">
                          <div class="color-control">
                            <el-color-picker v-model="styleForm.sidebarItemBackground" show-alpha />
                            <el-input v-model="styleForm.sidebarItemBackground" maxlength="40" />
                          </div>
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="12">
                        <el-form-item label="悬停底色">
                          <div class="color-control">
                            <el-color-picker v-model="styleForm.sidebarItemHoverBackground" show-alpha />
                            <el-input v-model="styleForm.sidebarItemHoverBackground" maxlength="40" />
                          </div>
                        </el-form-item>
                      </el-col>
                      <el-col :xs="24" :md="24">
                        <el-form-item label="磨砂强度">
                          <el-slider v-model="styleForm.sidebarBackdropBlur" :min="0" :max="30" :step="1" show-input />
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </el-form>
                </el-col>
                <el-col :xs="24" :md="8">
                  <div class="style-preview" :style="stylePreviewVars">
                    <aside class="style-preview-sidebar" aria-hidden="true">
                      <span class="style-preview-sidebar__brand">DYZ</span>
                      <div class="style-preview-sidebar__item is-active">首页</div>
                      <div class="style-preview-sidebar__item">项目审核</div>
                      <div class="style-preview-sidebar__item">工作台配置</div>
                    </aside>
                    <div class="style-preview-main">
                      <div class="style-preview-card">
                        <span>WORKBENCH</span>
                        <strong>首页样式预览</strong>
                        <p>公告、统计卡片与日历组件会使用这组颜色。</p>
                        <div class="style-preview-calendar">21</div>
                      </div>
                    </div>
                  </div>
                </el-col>
              </el-row>
            </section>

            <div v-show="publicStyleSection === 'tableAppearance'" v-hasPermi="['crehn:detailDisplayConfig:edit']" class="workbench-public-editor">
              <div class="panel-header">
                <div>
                  <span>颜色与公共样式</span>
                  <small>八类业务表格共享颜色、状态和操作样式</small>
                </div>
              </div>
              <ArtGlobalTableVisualEditor key="global-table-appearance" ref="globalTableAppearanceEditorRef" mode="appearance" :show-footer="false" />
            </div>

            <div v-show="publicStyleSection === 'reviewDisplay'" v-hasPermi="['crehn:detailDisplayConfig:edit']" class="workbench-public-editor">
              <div class="panel-header">
                <div>
                  <span>艺术评审可视化</span>
                  <small>审核与评分详情共享的显示配置</small>
                </div>
              </div>
              <ArtBusinessDisplayVisualEditor ref="reviewDisplayEditorRef" mode="reviewDisplay" :show-navigation="false" :show-footer="false" />
            </div>
          </el-card>
        </template>

        <el-card v-if="activeWorkbenchTab === 'business'" v-hasPermi="['crehn:detailDisplayConfig:edit']" shadow="never" class="workbench-config-panel">
          <template #header>
            <div class="panel-header">
              <div>
                <span>页面与类别布局</span>
                <small>类别上报、学校统一提交、项目审核、专家评分和上报进度统一按页面默认或末级类别维护</small>
              </div>
            </div>
          </template>
          <el-tabs v-model="businessConfigTab" class="business-config-tabs">
            <el-tab-pane name="header" :disabled="!businessHeaderPageKey">
              <template #label>
                <span class="business-config-tab-label">
                  页面表头与按钮
                  <el-icon v-if="pageHeaderDirty" class="workbench-config-nav__dirty"><WarningFilled /></el-icon>
                </span>
              </template>
              <ArtBusinessDisplayVisualEditor
                v-if="businessHeaderPageKey"
                ref="pageHeaderEditorRef"
                mode="projectList"
                :page-key="businessHeaderPageKey"
                compact
                :show-navigation="false"
                :show-footer="false"
              />
            </el-tab-pane>
            <el-tab-pane name="table">
              <template #label>
                <span class="business-config-tab-label">
                  表格列
                  <el-icon v-if="tableLayoutDirty" class="workbench-config-nav__dirty"><WarningFilled /></el-icon>
                </span>
              </template>
              <ArtGlobalTableVisualEditor
                key="global-table-layout"
                ref="globalTableLayoutEditorRef"
                v-model:page-key="businessPageKey"
                mode="layout"
                compact
                :show-page-navigation="false"
                :show-footer="false"
              />
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <section v-if="activeWorkbenchTab === 'layout'" class="home-layout-designer">
          <el-card shadow="never" class="home-layout-designer__library">
            <template #header>
              <div class="panel-header">
                <div>
                  <span>组件库</span>
                  <small>添加后进入当前角色草稿</small>
                </div>
                <el-button icon="Refresh" text :loading="componentLoading" @click="loadComponents" />
              </div>
            </template>
            <el-empty v-if="!componentOptions.length" description="请选择角色后加载组件" />
            <div v-else class="component-palette">
              <div v-for="item in componentOptions" :key="item.componentKey" class="component-option">
                <div>
                  <strong>{{ item.componentName }}</strong>
                  <span>{{ componentTypeLabel(item.componentType) }} · 默认{{ widthLabel(item.defaultWidth) }}</span>
                </div>
                <el-button size="small" type="primary" plain :disabled="hasLayout(item.componentKey)" @click="addComponent(item)">添加</el-button>
              </div>
            </div>
          </el-card>

          <el-card shadow="never" class="home-layout-designer__canvas">
            <template #header>
              <div class="panel-header">
                <div>
                  <span>首页布局画布</span>
                  <small>拖动排序，点击组件后在右侧编辑属性</small>
                </div>
                <el-tag type="info" effect="plain">{{ layoutRows.length }} 个组件</el-tag>
              </div>
            </template>
            <el-empty v-if="!layoutRows.length" description="从左侧组件库添加首页组件" />
            <div v-else class="layout-canvas">
              <article
                v-for="(row, index) in layoutRows"
                :key="row.componentKey"
                class="layout-canvas__item"
                :class="{
                  'is-selected': selectedLayoutIndex === index,
                  'is-hidden': row.visible === '1',
                  [`is-width-${widthClass(row.width)}`]: true
                }"
                draggable="true"
                tabindex="0"
                @click="selectedLayoutIndex = index"
                @focus="selectedLayoutIndex = index"
                @dragstart="dragIndex = index"
                @dragover.prevent
                @drop="moveRow(index)"
              >
                <div class="layout-canvas__head">
                  <span class="drag-handle"
                    ><el-icon><Rank /></el-icon
                  ></span>
                  <div>
                    <strong>{{ row.title || row.componentName || row.componentKey }}</strong>
                    <small>{{ row.componentName || row.componentKey }} · {{ widthLabel(row.width) }}</small>
                  </div>
                  <el-tag v-if="row.visible === '1'" size="small" type="info">已隐藏</el-tag>
                </div>
                <div class="layout-canvas__preview">
                  <span v-for="previewIndex in componentPreviewBlocks(row.componentKey)" :key="previewIndex" />
                </div>
              </article>
            </div>
            <el-alert
              class="layout-draft-tip"
              type="warning"
              :closable="false"
              show-icon
              title="右侧属性和组件配置先应用到当前草稿；点击顶部“保存当前范围”后，才会写入并影响实际首页。"
            />
          </el-card>

          <el-card shadow="never" class="home-layout-designer__properties">
            <template #header>
              <div class="panel-header">
                <div>
                  <span>组件属性</span>
                  <small>当前选中项</small>
                </div>
              </div>
            </template>
            <el-empty v-if="!selectedLayoutRow" description="请在画布中选择组件" />
            <el-form v-else :model="selectedLayoutRow" label-position="top">
              <div class="selected-component-heading">
                <strong>{{ selectedLayoutRow.componentName || selectedLayoutRow.componentKey }}</strong>
                <el-tag size="small" effect="plain">{{ selectedLayoutRow.componentKey }}</el-tag>
              </div>
              <el-form-item label="显示标题">
                <el-input v-model="selectedLayoutRow.title" placeholder="显示标题" />
              </el-form-item>
              <el-form-item label="占用宽度">
                <el-select v-model="selectedLayoutRow.width" class="w-full">
                  <el-option label="整行" value="1/1" />
                  <el-option label="半宽" value="1/2" />
                  <el-option label="三分之一" value="1/3" />
                  <el-option label="三分之二" value="2/3" />
                </el-select>
              </el-form-item>
              <el-form-item label="显示状态">
                <el-switch v-model="selectedLayoutRow.visible" active-value="0" inactive-value="1" active-text="显示" inactive-text="隐藏" />
              </el-form-item>
              <el-button
                v-if="isConfigurableLayoutRow(selectedLayoutRow)"
                class="w-full"
                type="primary"
                plain
                icon="Setting"
                @click="openSelectedLayoutConfig(selectedLayoutRow)"
              >
                编辑组件详细配置
              </el-button>
              <el-button class="w-full" type="danger" plain icon="Delete" @click="removeSelectedComponent">从首页移除</el-button>
            </el-form>
          </el-card>
        </section>

        <el-card v-if="activeWorkbenchTab === 'migration'" shadow="never" class="workbench-config-panel migration-panel">
          <template #header>
            <div class="panel-header">
              <div>
                <span>迁移与高级设置</span>
                <small>配置包包含首页布局、角色系统外壳和全局工作台样式；业务显示与全局表格仍独立保存</small>
              </div>
            </div>
          </template>
          <el-alert type="info" :closable="false" show-icon title="导入前先生成差异预览；只有确认导入后才覆盖目标环境配置。" />
          <div class="migration-actions">
            <el-button v-hasPermi="['system:workbench:query']" icon="Download" :loading="configPackageExporting" @click="exportConfigPackage">
              导出工作台配置包
            </el-button>
            <el-button
              v-hasPermi="['system:workbench:edit']"
              type="primary"
              icon="Upload"
              :loading="configPackageImporting"
              @click="selectConfigPackage"
            >
              导入并预览差异
            </el-button>
            <input ref="configPackageInput" class="config-package-input" type="file" accept=".zip,application/zip" @change="previewConfigPackage" />
          </div>
        </el-card>
      </main>
    </div>

    <el-dialog v-model="configDialog.visible" title="统一提交列表配置" width="min(1380px, 96vw)" append-to-body>
      <el-alert
        class="mb-3"
        type="info"
        :closable="false"
        show-icon
        title="这里只配置三种页面场景的表头、筛选、进度与外观；表格列、状态文字和操作按钮继续在“页面类别 → 学校提交 → 表格列”统一维护。"
      />
      <el-tabs v-model="configDialog.activeTab">
        <el-tab-pane label="外观与页面" name="appearance">
          <el-form :model="configForm" label-width="110px">
            <el-row :gutter="14">
              <el-col :span="24"><el-divider content-position="left">首页组件外观</el-divider></el-col>
              <el-col :xs="24" :sm="6">
                <el-form-item label="组件背景"><el-color-picker v-model="configForm.componentBackground" /></el-form-item>
              </el-col>
              <el-col :xs="24" :sm="6">
                <el-form-item label="背景透明度">
                  <el-input-number v-model="configForm.componentBackgroundOpacity" :min="0" :max="100" :step="5" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="6">
                <el-form-item label="显示边框"><el-switch v-model="configForm.componentBorderVisible" /></el-form-item>
              </el-col>
              <el-col :xs="24" :sm="6">
                <el-form-item label="边框颜色"><el-color-picker v-model="configForm.componentBorderColor" /></el-form-item>
              </el-col>
              <el-col :xs="24" :sm="6">
                <el-form-item label="圆角">
                  <el-input-number v-model="configForm.componentBorderRadius" :min="0" :max="32" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :span="24"><el-divider content-position="left">首页最大化页面</el-divider></el-col>
              <el-col :xs="24" :sm="6">
                <el-form-item label="页面内边距">
                  <el-input-number v-model="configForm.expandedPadding" :min="0" :max="32" :step="2" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="6">
                <el-form-item label="筛选区间距">
                  <el-input-number v-model="configForm.expandedGap" :min="0" :max="32" :step="2" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="6">
                <el-form-item label="页面背景"><el-color-picker v-model="configForm.expandedBackground" /></el-form-item>
              </el-col>
              <el-col :xs="24" :sm="6">
                <el-form-item label="背景透明度">
                  <el-input-number v-model="configForm.expandedBackgroundOpacity" :min="0" :max="100" :step="5" controls-position="right" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="首页组件表头" name="homeHeader">
          <el-alert
            class="mb-3"
            type="info"
            :closable="false"
            show-icon
            title="只控制普通首页组件的按钮、筛选与排列；表格列继续与独立提交页统一设置。"
          />
          <ArtProjectListVisualEditor
            v-model:page-config="configForm.homeHeader"
            v-model:school-submit-progress="configForm.progress"
            mode="schoolSubmit"
            school-submit-scene="home"
            :role-key="selectedRoleKey"
          />
        </el-tab-pane>
        <el-tab-pane label="首页最大化表头" name="maximizedHeader">
          <el-alert
            class="mb-3"
            type="info"
            :closable="false"
            show-icon
            title="只控制首页组件最大化后的表头；独立提交页表头请在“页面类别 → 学校提交”中维护。"
          />
          <ArtProjectListVisualEditor
            v-model:page-config="configForm.maximizedHeader"
            v-model:school-submit-progress="configForm.progress"
            mode="schoolSubmit"
            school-submit-scene="maximized"
            :role-key="selectedRoleKey"
          />
        </el-tab-pane>
        <el-tab-pane label="筛选类别" name="tabs">
          <div class="mb-3 flex flex-wrap items-center gap-2">
            <el-alert class="min-w-0 flex-1" :title="projectSubmitCategorySyncTitle" type="info" :closable="false" show-icon />
            <el-button :loading="projectSubmitCategoryLoading" icon="Refresh" @click="syncProjectSubmitCategoryTabs(true)">重新同步</el-button>
          </div>
          <el-table v-loading="projectSubmitCategoryLoading" :data="configForm.tabs" border>
            <el-table-column label="显示" width="76" align="center">
              <template #default="{ row }"><el-switch v-model="row.visible" /></template>
            </el-table-column>
            <el-table-column label="默认" width="76" align="center">
              <template #default="{ row }">
                <el-radio v-model="configForm.defaultTabKey" :label="row.key">
                  <span></span>
                </el-radio>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="76" align="center">
              <template #default="{ row }"><el-switch v-model="row.showCount" /></template>
            </el-table-column>
            <el-table-column label="层级" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.key === 'all'" type="info">汇总</el-tag>
                <el-tag v-else-if="row.sourceLevel === 1">一级类别</el-tag>
                <el-tag v-else type="success">二级类别</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="顺序" width="100" align="center">
              <template #default="{ row }"><el-input-number v-model="row.order" :min="1" :step="1" controls-position="right" /></template>
            </el-table-column>
            <el-table-column label="显示名称" min-width="180">
              <template #default="{ row }"><el-input v-model="row.label" /></template>
            </el-table-column>
            <el-table-column label="当前活动类别" min-width="180">
              <template #default="{ row }">{{ row.key === 'all' ? '全部项目' : row.sourceName || row.label }}</template>
            </el-table-column>
            <el-table-column label="类别编码" min-width="180">
              <template #default="{ row }">{{ row.key === 'all' ? 'all' : row.sourceCode || '-' }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button v-hasPermi="['system:workbench:edit']" type="warning" plain icon="RefreshLeft" @click="restoreProjectSubmitConfig"
          >恢复默认配置</el-button
        >
        <el-button @click="configDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveProjectSubmitConfig">应用到当前草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stageConfigDialog.visible" :title="stageConfigTitle" width="1180px" append-to-body>
      <el-tabs v-model="stageConfigDialog.activeTab">
        <el-tab-pane label="顶部与时间" name="top">
          <el-row :gutter="18" class="stage-layout-config">
            <el-col :xs="24" :md="15">
              <el-form :model="stageConfigForm" label-width="116px">
                <el-divider content-position="left">顶部公告区</el-divider>
                <el-row :gutter="12">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="区域高度">
                      <el-input-number v-model="stageConfigForm.topHeight" :min="160" :max="360" :step="10" controls-position="right" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="区块间距">
                      <el-input-number v-model="stageConfigForm.topGap" :min="8" :max="40" :step="2" controls-position="right" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="公告宽度">
                      <el-input-number
                        v-model="stageConfigForm.noticeRatio"
                        :min="0.5"
                        :max="2"
                        :step="0.1"
                        :precision="1"
                        controls-position="right"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="活动宽度">
                      <el-input-number
                        v-model="stageConfigForm.activityRatio"
                        :min="0.5"
                        :max="2.5"
                        :step="0.1"
                        :precision="1"
                        controls-position="right"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
                <template v-if="stageConfigKind === 'school'">
                  <el-divider content-position="left">公告名额信息</el-divider>
                  <el-row :gutter="12">
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="显示名额条">
                        <el-switch v-model="stageConfigForm.quotaSummaryEnabled" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24">
                      <el-form-item label="显示内容">
                        <el-checkbox-group v-model="stageConfigForm.quotaSummaryFields">
                          <el-checkbox label="schoolType">院校类型</el-checkbox>
                          <el-checkbox label="schoolName">学校名称</el-checkbox>
                          <el-checkbox label="totalCount">总上报</el-checkbox>
                          <el-checkbox label="usedCount">已用名额</el-checkbox>
                          <el-checkbox label="quotaLimit">总限额</el-checkbox>
                          <el-checkbox label="remainingCount">剩余</el-checkbox>
                          <el-checkbox label="draftCount">草稿</el-checkbox>
                          <el-checkbox label="submittedCount">已提交</el-checkbox>
                          <el-checkbox label="completedCount">已通过</el-checkbox>
                        </el-checkbox-group>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24">
                      <el-form-item label="显示模板">
                        <el-input
                          v-model="stageConfigForm.quotaSummaryTemplate"
                          type="textarea"
                          :rows="2"
                          maxlength="300"
                          show-word-limit
                          placeholder="{schoolType} 总上报 {totalCount} 总限额 {quotaLimit} 剩余 {remainingCount}"
                        />
                        <div class="stage-config-help">
                          留空时按“显示内容”生成标签；支持 {schoolType}、{totalCount}、{usedCount}、{quotaLimit}、{remainingCount}，也支持
                          {学校类别变量}、{总上报变量}。
                        </div>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24">
                      <el-form-item label="院校类型替换">
                        <div class="school-type-replacements">
                          <div
                            v-for="(item, index) in stageConfigForm.schoolTypeLabelReplacements"
                            :key="index"
                            class="school-type-replacements__row"
                          >
                            <el-input v-model="item.source" clearable placeholder="原文，如：高职" />
                            <span>替换为</span>
                            <el-input v-model="item.target" clearable placeholder="显示为，如：高职高专" />
                            <el-button type="danger" plain icon="Delete" @click="removeSchoolTypeLabelReplacement(index)">删除</el-button>
                          </div>
                          <el-button type="primary" plain icon="Plus" @click="addSchoolTypeLabelReplacement">新增替换</el-button>
                          <div class="stage-config-help">只影响首页公告组件里的院校类型展示，不修改学校真实类型和限额匹配规则。</div>
                        </div>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="名额字号">
                        <el-input-number v-model="stageConfigForm.quotaSummaryFontSize" :min="11" :max="24" :step="1" controls-position="right" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="名额圆角">
                        <el-input-number v-model="stageConfigForm.quotaSummaryRadius" :min="0" :max="24" :step="1" controls-position="right" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="文字颜色">
                        <div class="color-control">
                          <el-color-picker v-model="stageConfigForm.quotaSummaryTextColor" show-alpha />
                          <el-input v-model="stageConfigForm.quotaSummaryTextColor" maxlength="40" />
                        </div>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="数字颜色">
                        <div class="color-control">
                          <el-color-picker v-model="stageConfigForm.quotaSummaryNumberColor" show-alpha />
                          <el-input v-model="stageConfigForm.quotaSummaryNumberColor" maxlength="40" />
                        </div>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="名额背景">
                        <div class="color-control">
                          <el-color-picker v-model="stageConfigForm.quotaSummaryBackground" show-alpha />
                          <el-input v-model="stageConfigForm.quotaSummaryBackground" maxlength="80" />
                        </div>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="名额边框">
                        <div class="color-control">
                          <el-color-picker v-model="stageConfigForm.quotaSummaryBorderColor" show-alpha />
                          <el-input v-model="stageConfigForm.quotaSummaryBorderColor" maxlength="40" />
                        </div>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="标签间距">
                        <el-input-number v-model="stageConfigForm.quotaSummaryGap" :min="0" :max="24" :step="1" controls-position="right" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24">
                      <el-form-item label="名额 CSS">
                        <el-input v-model="stageConfigForm.quotaSummaryCss" type="textarea" :rows="3" placeholder="可填 CSS 声明或完整选择器" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-divider content-position="left">比例规则</el-divider>
                  <el-row :gutter="12">
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="公告显示比例">
                        <el-switch v-model="stageConfigForm.quotaRatioEnabled" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="最多显示">
                        <el-input-number v-model="stageConfigForm.quotaRatioMaxItems" :min="1" :max="12" :step="1" controls-position="right" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="比例字号">
                        <el-input-number v-model="stageConfigForm.quotaRatioFontSize" :min="10" :max="20" :step="1" controls-position="right" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="比例圆角">
                        <el-input-number v-model="stageConfigForm.quotaRatioRadius" :min="0" :max="24" :step="1" controls-position="right" />
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="比例文字">
                        <div class="color-control">
                          <el-color-picker v-model="stageConfigForm.quotaRatioTextColor" show-alpha />
                          <el-input v-model="stageConfigForm.quotaRatioTextColor" maxlength="40" />
                        </div>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="比例数字">
                        <div class="color-control">
                          <el-color-picker v-model="stageConfigForm.quotaRatioNumberColor" show-alpha />
                          <el-input v-model="stageConfigForm.quotaRatioNumberColor" maxlength="40" />
                        </div>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="比例背景">
                        <div class="color-control">
                          <el-color-picker v-model="stageConfigForm.quotaRatioBackground" show-alpha />
                          <el-input v-model="stageConfigForm.quotaRatioBackground" maxlength="80" />
                        </div>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24" :sm="12">
                      <el-form-item label="比例边框">
                        <div class="color-control">
                          <el-color-picker v-model="stageConfigForm.quotaRatioBorderColor" show-alpha />
                          <el-input v-model="stageConfigForm.quotaRatioBorderColor" maxlength="40" />
                        </div>
                      </el-form-item>
                    </el-col>
                    <el-col :xs="24">
                      <el-form-item label="比例 CSS">
                        <el-input v-model="stageConfigForm.quotaRatioCss" type="textarea" :rows="3" placeholder="可填 CSS 声明或完整选择器" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                </template>
                <el-divider content-position="left">时间卡片</el-divider>
                <el-row :gutter="12">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="卡片宽度">
                      <el-input-number v-model="stageConfigForm.calendarWidth" :min="120" :max="320" :step="10" controls-position="right" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="圆角">
                      <el-input-number v-model="stageConfigForm.calendarRadius" :min="0" :max="24" :step="1" controls-position="right" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="文字色">
                      <div class="color-control">
                        <el-color-picker v-model="stageConfigForm.calendarTextColor" show-alpha />
                        <el-input v-model="stageConfigForm.calendarTextColor" maxlength="40" />
                      </div>
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24">
                    <el-form-item label="背景">
                      <el-input v-model="stageConfigForm.calendarBackground" maxlength="120" show-word-limit />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="月份字号">
                      <el-input-number v-model="stageConfigForm.calendarMonthFontSize" :min="10" :max="24" :step="1" controls-position="right" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="日期字号">
                      <el-input-number v-model="stageConfigForm.calendarDayFontSize" :min="28" :max="84" :step="2" controls-position="right" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="星期字号">
                      <el-input-number v-model="stageConfigForm.calendarWeekdayFontSize" :min="12" :max="28" :step="1" controls-position="right" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="时间字号">
                      <el-input-number v-model="stageConfigForm.calendarTimeFontSize" :min="10" :max="22" :step="1" controls-position="right" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="显示时间">
                      <el-switch v-model="stageConfigForm.calendarShowTime" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="显示秒">
                      <el-switch v-model="stageConfigForm.calendarShowSeconds" :disabled="stageConfigForm.calendarShowTime === false" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24">
                    <el-form-item label="时间底色">
                      <el-input v-model="stageConfigForm.calendarTimeBackground" maxlength="80" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form>
            </el-col>
            <el-col :xs="24" :md="9">
              <div class="stage-config-preview" :style="stageConfigPreviewVars">
                <div class="stage-config-preview__top">
                  <div class="stage-config-preview__notice">
                    <span>公告</span>
                    <strong>您好！</strong>
                    <p>选择活动申报，请等待管理员审核。</p>
                    <div
                      v-if="stageConfigKind === 'school' && stageConfigForm.quotaSummaryEnabled !== false"
                      class="stage-config-preview__quota"
                      :style="stagePreviewQuotaStyle"
                    >
                      <span>高等教育</span>
                      <strong>总上报 8</strong>
                      <strong>已用名额 5</strong>
                      <strong>总限额 2</strong>
                      <strong>剩余 0</strong>
                    </div>
                    <div
                      v-if="stageConfigKind === 'school' && stageConfigForm.quotaRatioEnabled === true"
                      class="stage-config-preview__ratios"
                      :style="stagePreviewRatioStyle"
                    >
                      <span><em>个人项目比例不超过 20%</em><strong>0</strong><small>0.0%</small></span>
                      <span><em>乙组比例不超过 40%</em><strong>0</strong><small>0.0%</small></span>
                    </div>
                  </div>
                  <div class="stage-config-preview__activity">
                    <span>当前活动</span>
                    <strong>河南省第八届大学生艺术展演报送</strong>
                  </div>
                  <aside class="stage-config-preview__calendar">
                    <div>{{ stagePreviewCalendarInfo.monthText }}</div>
                    <strong>{{ stagePreviewCalendarInfo.day }}</strong>
                    <span>{{ stagePreviewCalendarInfo.weekday }}</span>
                    <small v-if="stageConfigForm.calendarShowTime !== false">{{ stagePreviewCalendarInfo.timeText }}</small>
                  </aside>
                </div>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>
        <el-tab-pane :label="stageConfigKind === 'school' ? '分类统计' : '统计卡片'" name="categories">
          <el-form :inline="true" :model="stageConfigForm" label-width="96px" class="mb-3">
            <template v-if="stageConfigKind === 'school'">
              <el-form-item label="配置活动">
                <el-select
                  v-model="stageConfigActivityId"
                  style="width: 300px"
                  filterable
                  placeholder="请选择活动"
                  :loading="stageActivityLoading"
                  @change="handleStageConfigActivityChange"
                >
                  <el-option
                    v-for="item in stageActivityOptions"
                    :key="String(item.id)"
                    :label="`${item.activityName || item.id}${item.status === 'enabled' ? '' : '（未启用）'}`"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="添加卡片">
                <el-select
                  v-model="stageCardSourceKey"
                  style="width: 260px"
                  filterable
                  clearable
                  placeholder="选择大类或一级类别"
                  :loading="stageCategoryLoading"
                >
                  <el-option
                    v-for="item in stageAddableSourceOptions"
                    :key="item.optionKey"
                    :label="`${item.sourceType === 'group' ? '大类' : '一级类别'}｜${item.label}`"
                    :value="item.optionKey"
                  />
                </el-select>
                <el-button type="primary" plain icon="Plus" :disabled="!stageCardSourceKey" @click="addStageCategoryCard">添加</el-button>
                <el-button plain icon="Refresh" :disabled="!stageConfigActivityId || stageCategoryLoading" @click="syncStageCategoryCards">
                  同步活动根类别
                </el-button>
              </el-form-item>
            </template>
            <el-form-item label="每行列数">
              <el-input-number v-model="stageConfigForm.categoryColumns" :min="1" :max="8" :step="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="卡片间距">
              <el-input-number v-model="stageConfigForm.categoryGap" :min="0" :max="40" :step="2" controls-position="right" />
            </el-form-item>
            <el-form-item label="圆角">
              <el-input-number v-model="stageConfigForm.categoryRadius" :min="0" :max="24" :step="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="悬停效果">
              <el-radio-group v-model="stageConfigForm.hoverEffectMode">
                <el-radio-button label="none">关闭</el-radio-button>
                <el-radio-button label="text">放大数字</el-radio-button>
                <el-radio-button label="pattern">放大背景</el-radio-button>
                <el-radio-button label="textPattern">数字+背景</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="放大比例">
              <el-input-number v-model="stageConfigForm.hoverScale" :min="1" :max="1.3" :step="0.01" :precision="2" controls-position="right" />
            </el-form-item>
            <el-form-item label="区域底色">
              <el-color-picker v-model="stageConfigForm.categoryAreaBackground" show-alpha />
              <el-input v-model="stageConfigForm.categoryAreaBackground" style="width: 150px; margin-left: 8px" />
            </el-form-item>
            <el-form-item label="默认样式">
              <el-button type="warning" plain icon="Refresh" @click="resetAllCategoryVisualToDefault">全部恢复默认样式</el-button>
            </el-form-item>
          </el-form>
          <el-alert
            v-if="stageConfigKind === 'school'"
            class="mb-3"
            type="info"
            :closable="false"
            show-icon
            title="卡片仅绑定当前活动的大类或不归属大类的一级类别；新增类别不会自动显示，可手工添加或同步。无有效限额时仅隐藏限额信息。"
          />
          <el-table v-loading="stageConfigKind === 'school' && stageCategoryLoading" :data="stageConfigCategoryRows" border>
            <el-table-column label="显示" width="66" align="center">
              <template #default="{ row }"><el-switch v-model="row.visible" /></template>
            </el-table-column>
            <el-table-column label="顺序" width="86" align="center">
              <template #default="{ row }"><el-input-number v-model="row.order" :min="1" :step="1" controls-position="right" /></template>
            </el-table-column>
            <el-table-column label="名称" min-width="140">
              <template #default="{ row }"><el-input v-model="row.label" /></template>
            </el-table-column>
            <el-table-column v-if="stageConfigKind === 'school'" label="绑定来源" min-width="190">
              <template #default="{ row }">
                <el-tag :type="!row.sourceType ? 'danger' : row.sourceType === 'group' ? 'warning' : 'primary'" effect="plain">
                  {{ !row.sourceType ? '未绑定' : row.sourceType === 'group' ? '大类' : '一级类别' }}
                </el-tag>
                <span class="ml-2">{{ stageCardSourceLabel(row) }}</span>
              </template>
            </el-table-column>
            <el-table-column v-if="stageConfigKind === 'audit'" label="副指标" width="72" align="center">
              <template #default="{ row }"><el-switch v-model="row.secondaryVisible" /></template>
            </el-table-column>
            <el-table-column v-if="stageConfigKind === 'audit'" label="副文案" min-width="140">
              <template #default="{ row }"><el-input v-model="row.secondaryLabel" placeholder="如：我已审核" /></template>
            </el-table-column>
            <el-table-column v-if="stageConfigKind === 'audit'" label="副字段" min-width="150">
              <template #default="{ row }">
                <el-select v-model="row.secondaryValueKey" clearable placeholder="选择字段">
                  <el-option v-for="item in auditSecondaryFieldOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column v-if="stageConfigKind === 'audit'" label="副字号" width="92" align="center">
              <template #default="{ row }"
                ><el-input-number v-model="row.secondaryFontSize" :min="10" :max="20" :step="1" controls-position="right"
              /></template>
            </el-table-column>
            <el-table-column v-if="stageConfigKind === 'audit'" label="副文字色" width="82" align="center">
              <template #default="{ row }"><el-color-picker v-model="row.secondaryTextColor" show-alpha /></template>
            </el-table-column>
            <el-table-column v-if="stageConfigKind === 'audit'" label="副数字色" width="82" align="center">
              <template #default="{ row }"><el-color-picker v-model="row.secondaryNumberColor" show-alpha /></template>
            </el-table-column>
            <el-table-column v-if="stageConfigKind === 'school'" label="限额显示" min-width="135">
              <template #default="{ row }">
                <el-select v-model="row.quotaDisplayMode">
                  <el-option label="不显示" value="none" />
                  <el-option label="卡片内常显" value="always" />
                  <el-option label="鼠标悬停" value="hover" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column v-if="stageConfigKind === 'school'" label="限额变量" min-width="210">
              <template #default="{ row }">
                <el-select v-model="row.quotaFields" multiple collapse-tags collapse-tags-tooltip :disabled="row.quotaDisplayMode === 'none'">
                  <el-option v-for="item in quotaFieldOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column v-if="stageConfigKind === 'school'" label="常显位置" min-width="130">
              <template #default="{ row }">
                <el-select v-model="row.quotaPosition" :disabled="row.quotaDisplayMode !== 'always'">
                  <el-option v-for="item in quotaPositionOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="字号" width="92" align="center">
              <template #default="{ row }"
                ><el-input-number v-model="row.fontSize" :min="12" :max="24" :step="1" controls-position="right"
              /></template>
            </el-table-column>
            <el-table-column label="视觉" width="82" align="center">
              <template #default="{ row }">
                <el-button type="primary" plain size="small" icon="Picture" @click="openCategoryVisualConfig(row)">视觉</el-button>
              </template>
            </el-table-column>
            <el-table-column label="标题色" width="76" align="center">
              <template #default="{ row }"><el-color-picker v-model="row.titleColor" show-alpha /></template>
            </el-table-column>
            <el-table-column label="数字色" width="76" align="center">
              <template #default="{ row }"><el-color-picker v-model="row.numberColor" show-alpha /></template>
            </el-table-column>
            <el-table-column label="背景色" width="76" align="center">
              <template #default="{ row }"><el-color-picker v-model="row.backgroundColor" show-alpha /></template>
            </el-table-column>
            <el-table-column label="边框色" width="76" align="center">
              <template #default="{ row }"><el-color-picker v-model="row.borderColor" show-alpha /></template>
            </el-table-column>
            <el-table-column label="强调色" width="76" align="center">
              <template #default="{ row }"><el-color-picker v-model="row.accentColor" show-alpha /></template>
            </el-table-column>
            <el-table-column v-if="stageConfigKind === 'school'" label="提交" width="66" align="center">
              <template #default="{ row }"><el-switch v-model="row.showSubmitted" /></template>
            </el-table-column>
            <el-table-column v-if="stageConfigKind === 'school'" label="跳转" width="66" align="center">
              <template #default="{ row }"><el-switch v-model="row.linkEnabled" /></template>
            </el-table-column>
            <el-table-column v-if="stageConfigKind === 'school'" label="操作" width="76" fixed="right" align="center">
              <template #default="{ row }">
                <el-button type="danger" link icon="Delete" @click="removeStageCategoryCard(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="stageConfigDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveStageNoticeConfig">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="categoryVisualDialog.visible" :title="categoryVisualTitle" width="960px" append-to-body>
      <div class="category-visual-layout">
        <el-form :model="categoryVisualForm" label-width="110px" class="category-visual-form">
          <el-row :gutter="14">
            <el-col :xs="24">
              <el-form-item label="背景方式">
                <el-radio-group v-model="categoryVisualForm.patternMode" @change="handleCategoryVisualPatternModeChange">
                  <el-radio-button label="preset">默认预设</el-radio-button>
                  <el-radio-button label="icon">内置图标</el-radio-button>
                  <el-radio-button label="text">文字符号</el-radio-button>
                  <el-radio-button label="image">本地图片</el-radio-button>
                  <el-radio-button label="none">不显示纹理</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col v-if="categoryVisualForm.patternMode === 'preset'" :xs="24" :md="12">
              <el-form-item label="背景预设">
                <el-select v-model="categoryVisualForm.patternPreset">
                  <el-option v-for="item in visualPatternOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col v-if="categoryVisualForm.patternMode === 'icon'" :xs="24" :md="12">
              <el-form-item label="内置图标">
                <icon-select v-model="categoryVisualForm.patternIcon" width="100%" />
              </el-form-item>
            </el-col>
            <el-col v-if="categoryVisualForm.patternMode === 'text'" :xs="24" :md="12">
              <el-form-item label="文字符号">
                <el-input v-model="categoryVisualForm.patternText" maxlength="8" show-word-limit clearable placeholder="艺 / ★ / 2026" />
              </el-form-item>
            </el-col>
            <el-col v-if="categoryVisualForm.patternMode !== 'none'" :xs="24" :md="12">
              <el-form-item label="背景位置">
                <el-select v-model="categoryVisualForm.patternPosition">
                  <el-option v-for="item in visualPositionOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col v-if="categoryVisualForm.patternMode === 'image'" :xs="24">
              <el-form-item label="本地图片">
                <div class="category-visual-upload">
                  <el-upload
                    action="#"
                    :auto-upload="false"
                    :show-file-list="false"
                    accept=".png,.jpg,.jpeg,.webp,image/png,image/jpeg,image/webp"
                    :on-change="handleCategoryVisualAssetChange"
                  >
                    <el-button type="primary" plain icon="Upload" :loading="categoryVisualAssetUploading">上传图片</el-button>
                  </el-upload>
                  <el-button plain :disabled="!categoryVisualForm.patternImageKey" @click="clearCategoryVisualImage(false)">清空引用</el-button>
                  <el-button
                    type="danger"
                    plain
                    :disabled="!categoryVisualForm.patternImageKey"
                    :loading="categoryVisualAssetDeleting"
                    @click="clearCategoryVisualImage(true)"
                  >
                    删除文件
                  </el-button>
                  <span v-if="categoryVisualForm.patternImageName" class="category-visual-upload__name">{{
                    categoryVisualForm.patternImageName
                  }}</span>
                </div>
              </el-form-item>
            </el-col>
            <el-col v-if="categoryVisualForm.patternMode === 'image'" :xs="24" :md="12">
              <el-form-item label="图片尺寸">
                <el-select v-model="categoryVisualForm.patternImageSize">
                  <el-option v-for="item in visualImageSizeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col v-if="categoryVisualForm.patternMode === 'image'" :xs="24" :md="12">
              <el-form-item label="是否平铺">
                <el-select v-model="categoryVisualForm.patternImageRepeat">
                  <el-option v-for="item in visualImageRepeatOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col v-if="categoryVisualForm.patternMode === 'preset'" :xs="24" :md="12">
              <el-form-item label="图案颜色">
                <el-color-picker v-model="categoryVisualForm.patternColor" show-alpha />
                <el-input v-model="categoryVisualForm.patternColor" maxlength="40" style="width: 170px; margin-left: 8px" />
              </el-form-item>
            </el-col>
            <el-col v-if="['icon', 'text'].includes(String(categoryVisualForm.patternMode || ''))" :xs="24" :md="12">
              <el-form-item :label="categoryVisualForm.patternMode === 'text' ? '文字颜色' : '图标颜色'">
                <el-color-picker v-model="categoryVisualForm.patternColor" show-alpha />
                <el-input
                  v-model="categoryVisualForm.patternColor"
                  maxlength="40"
                  placeholder="可在强调色基础上微调"
                  style="width: 150px; margin-left: 8px"
                />
                <el-button plain style="margin-left: 8px" @click="applyAccentColorToPattern">跟随强调色</el-button>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="玻璃颜色">
                <el-color-picker v-model="categoryVisualForm.glassColor" show-alpha />
                <el-input v-model="categoryVisualForm.glassColor" maxlength="60" style="width: 170px; margin-left: 8px" />
              </el-form-item>
            </el-col>
            <el-col v-if="['preset', 'icon', 'text'].includes(String(categoryVisualForm.patternMode || ''))" :xs="24" :md="12">
              <el-form-item label="图案大小">
                <el-input-number
                  v-model="categoryVisualForm.patternScale"
                  :min="0.8"
                  :max="3"
                  :step="0.05"
                  :precision="2"
                  controls-position="right"
                />
              </el-form-item>
            </el-col>
            <el-col v-if="categoryVisualForm.patternMode !== 'none'" :xs="24" :md="12">
              <el-form-item label="纹理透明度">
                <el-input-number
                  v-model="categoryVisualForm.patternOpacity"
                  :min="0"
                  :max="0.75"
                  :step="0.01"
                  :precision="2"
                  controls-position="right"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="玻璃透明度">
                <el-input-number v-model="categoryVisualForm.glassOpacity" :min="0" :max="1" :step="0.01" :precision="2" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="磨砂强度">
                <el-input-number v-model="categoryVisualForm.glassBlur" :min="0" :max="28" :step="1" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="细纹强度">
                <el-input-number
                  v-model="categoryVisualForm.textureOpacity"
                  :min="0"
                  :max="0.65"
                  :step="0.01"
                  :precision="2"
                  controls-position="right"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="高光强度">
                <el-input-number
                  v-model="categoryVisualForm.highlightOpacity"
                  :min="0"
                  :max="1"
                  :step="0.01"
                  :precision="2"
                  controls-position="right"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <div class="category-visual-preview-shell">
          <article
            class="category-visual-preview stage-notice-stat"
            :class="categoryVisualPreviewClasses"
            :style="categoryVisualPreviewStyle"
            aria-hidden="true"
          >
            <span v-if="categoryVisualForm.patternMode !== 'none'" class="stage-notice-stat__pattern" :style="categoryVisualPreviewPatternStyle">
              <svg-icon
                v-if="
                  normalizePreviewPatternModeValue(categoryVisualForm.patternMode) === 'icon' &&
                  normalizePatternIconValue(categoryVisualForm.patternIcon)
                "
                class-name="stage-notice-stat__svg-icon"
                :icon-class="normalizePatternIconValue(categoryVisualForm.patternIcon)"
              />
              <span v-else-if="normalizePreviewPatternModeValue(categoryVisualForm.patternMode) === 'text'" class="stage-notice-stat__text-symbol">
                {{ normalizePatternTextValue(categoryVisualForm.patternText) }}
              </span>
            </span>
            <span class="stage-notice-stat__glass" :style="categoryVisualPreviewGlassStyle"></span>
            <i class="stage-notice-stat__accent" :style="{ backgroundColor: categoryVisualForm.accentColor || '#2563eb' }"></i>
            <strong :style="categoryVisualPreviewTitleStyle">{{ categoryVisualForm.label || '分类统计' }}</strong>
            <span class="stage-notice-stat__numbers" :style="categoryVisualPreviewNumberStyle"> <b>0</b><em>/</em><b>10</b><em>/</em><b>0</b> </span>
            <small class="stage-notice-stat__legend">草稿 / 提交 / 通过</small>
          </article>
        </div>
      </div>
      <template #footer>
        <div class="category-visual-footer">
          <el-button plain @click="resetCategoryVisualToDefault">恢复默认</el-button>
          <div>
            <el-button @click="categoryVisualDialog.visible = false">取消</el-button>
            <el-button type="primary" @click="saveCategoryVisualConfig">确定</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="auditWorkbenchConfigDialog.visible" title="审核工作台组件配置" width="1100px" append-to-body>
      <el-tabs v-model="auditWorkbenchConfigDialog.activeTab">
        <el-tab-pane label="标题" name="base">
          <el-form :model="auditWorkbenchConfigForm" label-width="118px">
            <el-row :gutter="14">
              <el-col :xs="24" :md="12">
                <el-form-item label="显示英文标识">
                  <el-switch v-model="auditWorkbenchConfigForm.kickerVisible" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="显示主标题">
                  <el-switch v-model="auditWorkbenchConfigForm.titleVisible" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="英文标识">
                  <el-input v-model="auditWorkbenchConfigForm.kickerText" maxlength="30" placeholder="AUDIT" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="标识字号">
                  <el-input-number v-model="auditWorkbenchConfigForm.kickerFontSize" :min="10" :max="24" :step="1" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="主标题">
                  <el-input v-model="auditWorkbenchConfigForm.titleText" maxlength="40" placeholder="留空使用布局标题" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="标题字号">
                  <el-input-number v-model="auditWorkbenchConfigForm.titleFontSize" :min="14" :max="32" :step="1" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="刷新按钮">
                  <el-switch v-model="auditWorkbenchConfigForm.refreshVisible" active-text="显示" inactive-text="隐藏" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="默认 Tab">
                  <el-radio-group v-model="auditWorkbenchConfigForm.defaultTab">
                    <el-radio-button label="pending">待审核</el-radio-button>
                    <el-radio-button label="mine">我的审核</el-radio-button>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="内容区块" name="content">
          <el-form :model="auditWorkbenchConfigForm" label-width="118px">
            <el-divider content-position="left">待审核 Tab</el-divider>
            <el-row :gutter="14">
              <el-col :xs="24" :md="8">
                <el-form-item label="显示 Tab">
                  <el-switch v-model="auditWorkbenchConfigForm.pendingTabVisible" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="Tab 名称">
                  <el-input v-model="auditWorkbenchConfigForm.pendingTabLabel" maxlength="20" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="显示列表按钮">
                  <el-switch v-model="auditWorkbenchConfigForm.auditListButtonVisible" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="按钮文字">
                  <el-input v-model="auditWorkbenchConfigForm.auditListButtonText" maxlength="20" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="区块标题">
                  <el-input v-model="auditWorkbenchConfigForm.pendingSectionTitle" maxlength="30" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-divider content-position="left">我的审核 Tab</el-divider>
            <el-row :gutter="14">
              <el-col :xs="24" :md="8">
                <el-form-item label="显示 Tab">
                  <el-switch v-model="auditWorkbenchConfigForm.mineTabVisible" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="Tab 名称">
                  <el-input v-model="auditWorkbenchConfigForm.mineTabLabel" maxlength="20" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="显示头部统计">
                  <el-switch v-model="auditWorkbenchConfigForm.metricsVisible" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="统计文字字号">
                  <el-input-number v-model="auditWorkbenchConfigForm.metricFontSize" :min="10" :max="20" :step="1" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="统计数字字号">
                  <el-input-number v-model="auditWorkbenchConfigForm.metricNumberFontSize" :min="12" :max="28" :step="1" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="总数名称">
                  <el-input v-model="auditWorkbenchConfigForm.metricTotalLabel" maxlength="20" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="通过名称">
                  <el-input v-model="auditWorkbenchConfigForm.metricPassLabel" maxlength="20" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="退回名称">
                  <el-input v-model="auditWorkbenchConfigForm.metricReturnLabel" maxlength="20" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="文字颜色">
                  <div class="color-control">
                    <el-color-picker v-model="auditWorkbenchConfigForm.metricLabelColor" show-alpha />
                    <el-input v-model="auditWorkbenchConfigForm.metricLabelColor" maxlength="40" />
                  </div>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="总数颜色">
                  <div class="color-control">
                    <el-color-picker v-model="auditWorkbenchConfigForm.metricTotalColor" show-alpha />
                    <el-input v-model="auditWorkbenchConfigForm.metricTotalColor" maxlength="40" />
                  </div>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="通过颜色">
                  <div class="color-control">
                    <el-color-picker v-model="auditWorkbenchConfigForm.metricPassColor" show-alpha />
                    <el-input v-model="auditWorkbenchConfigForm.metricPassColor" maxlength="40" />
                  </div>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="退回颜色">
                  <div class="color-control">
                    <el-color-picker v-model="auditWorkbenchConfigForm.metricReturnColor" show-alpha />
                    <el-input v-model="auditWorkbenchConfigForm.metricReturnColor" maxlength="40" />
                  </div>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="显示大类统计">
                  <el-switch v-model="auditWorkbenchConfigForm.groupVisible" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="大类列数">
                  <el-input-number v-model="auditWorkbenchConfigForm.groupColumns" :min="1" :max="6" :step="1" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="大类标题">
                  <el-input v-model="auditWorkbenchConfigForm.groupSectionTitle" maxlength="30" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="大类说明">
                  <el-input v-model="auditWorkbenchConfigForm.groupSectionSubtitle" maxlength="60" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="显示审核记录">
                  <el-switch v-model="auditWorkbenchConfigForm.recordsVisible" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="记录标题">
                  <el-input v-model="auditWorkbenchConfigForm.recordsSectionTitle" maxlength="30" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="记录说明">
                  <el-input v-model="auditWorkbenchConfigForm.recordsSectionSubtitle" maxlength="60" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="自定义 CSS" name="css">
          <el-input
            v-model="auditWorkbenchConfigForm.componentCss"
            type="textarea"
            :rows="8"
            maxlength="1200"
            show-word-limit
            placeholder="可填 CSS 声明或完整选择器"
          />
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="auditWorkbenchConfigDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveAuditWorkbenchConfig">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewWorkbenchConfigDialog.visible" title="评审工作台组件配置" width="1160px" append-to-body>
      <el-tabs v-model="reviewWorkbenchConfigDialog.activeTab">
        <el-tab-pane label="标题" name="base">
          <el-form :model="reviewWorkbenchConfigForm" label-width="118px">
            <el-row :gutter="14">
              <el-col :xs="24" :md="12">
                <el-form-item label="显示英文标识">
                  <el-switch v-model="reviewWorkbenchConfigForm.kickerVisible" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="显示主标题">
                  <el-switch v-model="reviewWorkbenchConfigForm.titleVisible" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="英文标识">
                  <el-input v-model="reviewWorkbenchConfigForm.kickerText" maxlength="30" placeholder="SCORE" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="标识字号">
                  <el-input-number v-model="reviewWorkbenchConfigForm.kickerFontSize" :min="10" :max="24" :step="1" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="主标题">
                  <el-input v-model="reviewWorkbenchConfigForm.titleText" maxlength="40" placeholder="留空使用布局标题" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="标题字号">
                  <el-input-number v-model="reviewWorkbenchConfigForm.titleFontSize" :min="14" :max="32" :step="1" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="刷新按钮">
                  <el-switch v-model="reviewWorkbenchConfigForm.refreshVisible" active-text="显示" inactive-text="隐藏" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="列表按钮">
                  <el-switch v-model="reviewWorkbenchConfigForm.listButtonVisible" active-text="显示" inactive-text="隐藏" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="按钮文字">
                  <el-input v-model="reviewWorkbenchConfigForm.listButtonText" maxlength="20" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="卡片列数">
                  <el-input-number v-model="reviewWorkbenchConfigForm.cardColumns" :min="1" :max="6" :step="1" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="待评分 Tab">
                  <el-input v-model="reviewWorkbenchConfigForm.pendingTabLabel" maxlength="20" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-form-item label="最近评分 Tab">
                  <el-input v-model="reviewWorkbenchConfigForm.recentTabLabel" maxlength="20" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="统计卡片" name="cards">
          <el-table :data="reviewWorkbenchConfigForm.cards" border>
            <el-table-column label="显示" width="76" align="center">
              <template #default="{ row }"><el-switch v-model="row.visible" /></template>
            </el-table-column>
            <el-table-column label="顺序" width="96" align="center">
              <template #default="{ row }"><el-input-number v-model="row.order" :min="1" :step="1" controls-position="right" /></template>
            </el-table-column>
            <el-table-column label="标题" min-width="140">
              <template #default="{ row }"><el-input v-model="row.label" maxlength="24" /></template>
            </el-table-column>
            <el-table-column label="变量字段" min-width="170">
              <template #default="{ row }">
                <el-select v-model="row.valueKey" filterable allow-create default-first-option>
                  <el-option label="分配总数" value="assigned_total" />
                  <el-option label="待评分" value="pending_score" />
                  <el-option label="草稿" value="draft_score" />
                  <el-option label="已提交" value="submitted_score" />
                  <el-option label="已锁定" value="locked_by_other" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="图片 URL" min-width="220">
              <template #default="{ row }"><el-input v-model="row.imageUrl" clearable placeholder="可选，留空显示色条" /></template>
            </el-table-column>
            <el-table-column label="点击筛选" min-width="140">
              <template #default="{ row }">
                <el-select v-model="row.linkStatus" clearable>
                  <el-option label="不跳转" value="" />
                  <el-option label="全部" value="all" />
                  <el-option label="待评分" value="none" />
                  <el-option label="草稿" value="draft" />
                  <el-option label="已提交" value="submitted" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="标题色" width="120">
              <template #default="{ row }"><el-color-picker v-model="row.titleColor" show-alpha /></template>
            </el-table-column>
            <el-table-column label="数字色" width="120">
              <template #default="{ row }"><el-color-picker v-model="row.numberColor" show-alpha /></template>
            </el-table-column>
            <el-table-column label="背景" width="120">
              <template #default="{ row }"><el-color-picker v-model="row.backgroundColor" show-alpha /></template>
            </el-table-column>
            <el-table-column label="边框" width="120">
              <template #default="{ row }"><el-color-picker v-model="row.borderColor" show-alpha /></template>
            </el-table-column>
            <el-table-column label="强调" width="120">
              <template #default="{ row }"><el-color-picker v-model="row.accentColor" show-alpha /></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="自定义 CSS" name="css">
          <el-input
            v-model="reviewWorkbenchConfigForm.componentCss"
            type="textarea"
            :rows="8"
            maxlength="1200"
            show-word-limit
            placeholder="可填 CSS 声明或完整选择器"
          />
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="reviewWorkbenchConfigDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveReviewWorkbenchConfig">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="configImportDialog.visible"
      title="导入工作台配置"
      width="min(1100px, calc(100vw - 32px))"
      append-to-body
      destroy-on-close
      @closed="clearConfigImportPreview"
    >
      <div v-if="configImportPreview" class="config-import-preview">
        <el-alert
          :closable="false"
          show-icon
          type="warning"
          title="以下差异仅覆盖首页布局、角色系统布局和全局工作台样式；业务页面显示配置不会导入。"
        />
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="配置包版本">{{ configImportPreview.packageVersion }}</el-descriptions-item>
          <el-descriptions-item label="导出时间">{{ configImportPreview.exportedAt || '未记录' }}</el-descriptions-item>
          <el-descriptions-item label="将发生变化">
            <el-tag type="warning">{{ configImportPreview.changedCount }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="无法匹配">
            <el-tag :type="configImportPreview.skippedCount ? 'danger' : 'success'">{{ configImportPreview.skippedCount }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <el-table :data="configImportPreview.differences" border max-height="520">
          <el-table-column label="范围" width="120">
            <template #default="{ row }">{{ configDiffScopeLabel(row.scope) }}</template>
          </el-table-column>
          <el-table-column label="角色" min-width="150">
            <template #default="{ row }">
              <span v-if="row.roleKey">{{ row.roleName || row.roleKey }}（{{ row.roleKey }}）</span>
              <span v-else>全局</span>
            </template>
          </el-table-column>
          <el-table-column label="配置项" prop="path" min-width="230" show-overflow-tooltip />
          <el-table-column label="变更" width="92" align="center">
            <template #default="{ row }">
              <el-tag :type="configDiffTagType(row.changeType)">{{ configDiffTypeLabel(row.changeType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="当前值" min-width="190">
            <template #default="{ row }"
              ><span class="config-diff-value">{{ row.currentValue ?? '—' }}</span></template
            >
          </el-table-column>
          <el-table-column label="导入值" min-width="190">
            <template #default="{ row }"
              ><span class="config-diff-value">{{ row.importedValue ?? '—' }}</span></template
            >
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="configImportDialog.visible = false">取消</el-button>
        <el-button
          v-hasPermi="['system:workbench:edit']"
          type="primary"
          :loading="configPackageImporting"
          :disabled="!configImportFile || !configImportPreview?.changedCount"
          @click="confirmConfigPackageImport"
        >
          确认导入
        </el-button>
      </template>
    </el-dialog>

    <UniversalDashboardConfigDialog
      v-model="universalDashboardConfigDialog.visible"
      :config-json="universalDashboardConfigTargetRow?.configJson"
      :role-key="selectedRoleKey"
      :title="universalDashboardConfigTargetRow?.title"
      @save="saveUniversalDashboardConfig"
    />
  </div>
</template>

<script setup name="WorkbenchConfig" lang="ts">
import { listActivityOptions, listCategory } from '@/api/crehn/activity';
import {
  saveManagedActivity,
  type ArtAuditProgressConfig,
  type ArtListTablePageKey,
  type ArtWorkspaceHeaderPageConfig
} from '@/api/crehn/detailDisplay';
import { ActivityCategoryVO, ActivityVO } from '@/api/crehn/types';
import { checkPermi } from '@/utils/permission';
import { useAppStore } from '@/store/modules/app';
import { ElMessageBox } from 'element-plus';
import { onBeforeRouteLeave } from 'vue-router';
import {
  WORKBENCH_STYLE_SAVE_API_VERSION,
  deleteWorkbenchAsset,
  exportWorkbenchConfigPackage,
  getRoleShellConfig,
  getWorkbenchStyleConfig,
  importWorkbenchConfigPackage,
  listWorkbenchComponents,
  listWorkbenchRoleOptions,
  listRoleWorkbench,
  previewWorkbenchConfigPackage,
  restoreRoleShellConfig,
  saveRoleWorkbench,
  saveRoleShellConfig,
  saveWorkbenchStyleConfig,
  uploadWorkbenchAsset,
  workbenchAssetUrl
} from '@/api/system/workbench';
import {
  WorkbenchComponentVO,
  WorkbenchConfigChangeType,
  WorkbenchConfigImportPreviewVO,
  WorkbenchLayoutVO,
  WorkbenchRoleOptionVO,
  WorkbenchRoleShellConfig
} from '@/api/system/workbench/types';
import {
  applyWorkbenchStyleVars,
  cloneWorkbenchStyle,
  compactWorkbenchStyleConfig,
  defaultWorkbenchStyleConfig,
  isWorkbenchStyleConfigPayload,
  normalizeWorkbenchStyleConfig,
  parseWorkbenchStyleConfig,
  workbenchStyleConfigsEqual,
  workbenchStylePreviewVars,
  workbenchStylePresets
} from '@/utils/workbenchStyle';
import {
  ActivityCategoryTreeNode,
  buildActivityCategoryTree,
  buildSchoolCategoryMenuTree,
  isCategoryGroup,
  isCategoryMenuAvailable,
  isCategoryMenuVisible,
  sortCategoryNodes
} from '@/utils/artCategory';
import { UNIVERSAL_DASHBOARD_COMPONENT_KEY, defaultUniversalDashboardConfig, resolveDashboardRole } from '@/views/workbench/universalDashboard';
import { normalizeSchoolProjectCategoryAllLabel } from '@/views/workbench/schoolProjectSubmitConfig';
import { isWorkspaceHeaderPageKey } from './workbenchPageKeyBoundary';
import ArtBusinessDisplayVisualEditor from '@/views/crehn/components/ArtBusinessDisplayVisualEditor.vue';
import ArtGlobalTableVisualEditor from '@/views/crehn/components/ArtGlobalTableVisualEditor.vue';
import ArtProjectListVisualEditor from '@/views/crehn/components/ArtProjectListVisualEditor.vue';
import {
  WORKSPACE_HEADER_LAYOUT_VERSION,
  artListTablePageOptions,
  defaultSchoolSubmitHomeHeader,
  defaultSchoolSubmitMaximizedHeader,
  defaultSchoolSubmitProgress,
  defaultWorkspaceHeaderPage,
  loadArtDetailDisplayConfig,
  normalizeWorkspaceHeaderPage,
  useArtDetailDisplayConfig
} from '@/views/crehn/components/artDetailDisplayConfig';
import {
  WORKBENCH_COMPONENT_KEYS,
  WORKBENCH_STAGE_NOTICE_COMPONENT_KEYS,
  workbenchComponentConfigKind,
  workbenchComponentPreviewBlocks,
  workbenchWidthLabel
} from '@/views/workbench/workbenchComponentRegistry';
import RoleShellVisualEditor from './components/RoleShellVisualEditor.vue';
import UniversalDashboardConfigDialog from './components/UniversalDashboardConfigDialog.vue';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const appStore = useAppStore();
const loading = ref(false);
const saving = ref(false);
const resettingRoleLayout = ref(false);
const componentLoading = ref(false);
const styleLoading = ref(false);
const styleSaving = ref(false);
const workbenchStyleExpanded = ref(true);
const sectionOptionsExpanded = ref(true);
const contextOptionsExpanded = ref(true);
const workbenchControlPanelRef = ref<HTMLElement>();
const workbenchControlHeight = ref(0);
let workbenchControlObserver: ResizeObserver | undefined;
const roleShellLoading = ref(false);
const roleShellSaving = ref(false);
const roleShellRestoring = ref(false);
const configPackageExporting = ref(false);
const configPackageImporting = ref(false);
const configPackageInput = ref<HTMLInputElement>();
const configImportFile = ref<File>();
const configImportPreview = ref<WorkbenchConfigImportPreviewVO>();
const configImportDialog = reactive({ visible: false });
type WorkbenchSectionKey = 'layout' | 'shell' | 'style' | 'business' | 'migration';
const workbenchSections: Array<{
  key: WorkbenchSectionKey;
  label: string;
  shortLabel: string;
  description: string;
  icon: string;
  permission: string[];
}> = [
  {
    key: 'layout',
    label: '角色首页设置',
    shortLabel: '角色首页',
    description: '组件、顺序与首页占位',
    icon: 'Grid',
    permission: ['system:workbench:query']
  },
  {
    key: 'business',
    label: '页面与类别布局',
    shortLabel: '页面类别',
    description: '八类页面默认与末级类别覆盖',
    icon: 'Document',
    permission: ['crehn:detailDisplayConfig:edit']
  },
  {
    key: 'shell',
    label: '角色工作台外观',
    shortLabel: '角色工作台',
    description: '导航、标题与角色品牌',
    icon: 'Monitor',
    permission: ['system:workbench:query']
  },
  {
    key: 'style',
    label: '公共样式',
    shortLabel: '公共样式',
    description: '颜色与全局工作台样式',
    icon: 'Brush',
    permission: ['system:workbench:query']
  },
  {
    key: 'migration',
    label: '迁移与高级设置',
    shortLabel: '迁移设置',
    description: '差异预览、导入与导出',
    icon: 'Switch',
    permission: ['system:workbench:query']
  }
];
const CONFIG_PERMISSION_HINT = '请先在角色权限管理中增加权限';
const hasSectionPermission = (section: (typeof workbenchSections)[number]) => checkPermi(section.permission);
type PublicStyleSectionKey = 'workbench' | 'tableAppearance' | 'reviewDisplay';
const businessPageShortLabels: Record<ArtListTablePageKey, string> = {
  project: '类别上报',
  schoolSubmit: '学校提交',
  audit: '项目审核',
  review: '专家评分',
  projectView: '上报进度',
  reviewAssignment: '评审分配',
  scoreSummary: '评分汇总',
  signedSheets: '签名表汇总'
};
const businessPageOptions = artListTablePageOptions.map((item) => ({
  label: item.label,
  shortLabel: businessPageShortLabels[item.key],
  value: item.key
}));
const businessPageKey = ref<ArtListTablePageKey>('project');
const businessHeaderPageKey = computed(() => (isWorkspaceHeaderPageKey(businessPageKey.value) ? businessPageKey.value : undefined));
type BusinessConfigTabKey = 'header' | 'table';
const businessConfigTab = ref<BusinessConfigTabKey>('header');
const publicStyleSectionOptions: Array<{ label: string; shortLabel: string; value: PublicStyleSectionKey }> = [
  { label: '工作台与菜单', shortLabel: '工作台菜单', value: 'workbench' },
  { label: '颜色与公共样式', shortLabel: '颜色样式', value: 'tableAppearance' },
  { label: '艺术评审可视化', shortLabel: '艺术评审', value: 'reviewDisplay' }
];
const publicStyleSection = ref<PublicStyleSectionKey>('workbench');
const activeWorkbenchTab = ref<WorkbenchSectionKey>('layout');
const selectedRoleId = ref<string | number>();
const roleOptions = ref<WorkbenchRoleOptionVO[]>([]);
const componentOptions = ref<WorkbenchComponentVO[]>([]);
const layoutRows = ref<WorkbenchLayoutVO[]>([]);
const dragIndex = ref<number>();
const selectedLayoutIndex = ref(0);
const layoutOriginalSnapshot = ref('');
const styleOriginalSnapshot = ref('');
const globalTableLayoutEditorRef = ref<any>();
const globalTableAppearanceEditorRef = ref<any>();
const pageHeaderEditorRef = ref<any>();
const reviewDisplayEditorRef = ref<any>();
const layoutSnapshot = () => JSON.stringify(layoutRows.value.map((item, index) => ({ ...item, sortOrder: index + 1 })));
const layoutDirty = computed(
  () => Boolean(selectedRoleId.value) && Boolean(layoutOriginalSnapshot.value) && layoutOriginalSnapshot.value !== layoutSnapshot()
);
const selectedLayoutRow = computed(() => layoutRows.value[selectedLayoutIndex.value]);
const selectedRoleName = computed(
  () => roleOptions.value.find((item) => String(item.roleId) === String(selectedRoleId.value))?.roleName || '当前角色'
);
const selectedRoleKey = computed(() => String(roleOptions.value.find((item) => String(item.roleId) === String(selectedRoleId.value))?.roleKey || ''));
const compactRoleLabels: Record<string, string> = {
  superadmin: '超管',
  crehn_admin: '管理',
  crehn_sub_admin: '次管',
  crehn_auditor: '审核',
  crehn_reviewer: '评分',
  crehn_score_summary: '汇总',
  crehn_school: '学校',
  crehn_participant: '参赛',
  crehn_result_admin: '结果',
  crehn_cms_editor: '编辑',
  crehn_cms_publisher: '发布',
  crehn_audit_supervisor: '监督'
};
const compactRoleLabel = (role: WorkbenchRoleOptionVO) => {
  const normalizedRoleKey = String(role.roleKey || '')
    .trim()
    .toLowerCase();
  if (compactRoleLabels[normalizedRoleKey]) return compactRoleLabels[normalizedRoleKey];
  const fullLabel = `${role.roleName} ${normalizedRoleKey}`.toLowerCase();
  if (fullLabel.includes('超级') || fullLabel.includes('super')) return '超管';
  if (fullLabel.includes('签字') || fullLabel.includes('sign')) return '签字';
  if (fullLabel.includes('运维') || fullLabel.includes('operation') || fullLabel.includes('ops')) return '运维';
  if (fullLabel.includes('上报') || fullLabel.includes('progress')) return '上报';
  if (fullLabel.includes('评审') || fullLabel.includes('专家') || fullLabel.includes('review') || fullLabel.includes('expert')) return '评审';
  if (fullLabel.includes('审核') || fullLabel.includes('audit')) return '审核';
  if (fullLabel.includes('学校') || fullLabel.includes('school')) return '学校';
  if (fullLabel.includes('管理') || fullLabel.includes('admin')) return '管理';
  return role.roleName.length > 4 ? role.roleName.slice(0, 4) : role.roleName;
};
const defaultRoleShellConfig = (roleKey = ''): WorkbenchRoleShellConfig => {
  const normalizedRole = String(roleKey).trim().toLowerCase();
  const schoolRole =
    normalizedRole === 'school' ||
    normalizedRole.startsWith('school_') ||
    normalizedRole === 'crehn_school' ||
    normalizedRole.startsWith('crehn_school_');
  return {
    configured: false,
    navType: 'left',
    theme: '#2563EB',
    radiusBase: 8,
    tagsView: true,
    tagsIcon: false,
    fixedHeader: true,
    sidebarLogo: true,
    dynamicTitle: false,
    breadcrumbVisible: true,
    hideHomeBreadcrumb: schoolRole,
    hideHomeTagsView: schoolRole,
    navbarTitleVisible: true,
    navbarTitle: '',
    navbarTitleAlign: 'left',
    navbarTitleFontSize: 15,
    navbarTitleColor: '#29445f',
    navbarTitleFontWeight: 'bold',
    navbarTitleFontFamily: 'system',
    navbarTitleLogoMode: 'hidden',
    navbarTitleLogoAssetKey: '',
    navbarTitleLogoHeight: 28,
    showUserNickname: true,
    showScreenfull: true,
    showSizeSelect: true,
    showUserAvatar: true,
    userDisplayMode: 'nickname',
    brandTitleMode: 'role',
    brandTitleKeepVisibleOnCollapse: false,
    brandTitle: '工作台',
    brandActivityId: undefined
  };
};
const roleShellForm = reactive<WorkbenchRoleShellConfig>(defaultRoleShellConfig());
const { detailDisplayConfig } = useArtDetailDisplayConfig();
const brandActivityOptions = ref<ActivityVO[]>([]);
const managedActivityOptions = computed(() => brandActivityOptions.value.filter((item) => item.status === 'enabled'));
const toolbarManagedActivityId = ref('');
const managedActivitySaving = ref(false);
const roleShellOriginalSnapshot = ref('');
const roleShellLoadedRoleId = ref<string>();
const roleShellSnapshot = () => JSON.stringify({ ...roleShellForm, configured: undefined });
const roleShellDirty = computed(
  () => Boolean(selectedRoleId.value) && Boolean(roleShellOriginalSnapshot.value) && roleShellOriginalSnapshot.value !== roleShellSnapshot()
);
const WORKBENCH_STYLE_CONFIG_MAX_LENGTH = 4000;
const SCHOOL_DASHBOARD_COMPONENT_KEY = WORKBENCH_COMPONENT_KEYS.schoolDashboard;
const PROJECT_SUBMIT_COMPONENT_KEY = WORKBENCH_COMPONENT_KEYS.schoolProjectList;
const STAGE_NOTICE_COMPONENT_KEY = WORKBENCH_COMPONENT_KEYS.schoolStageNotice;
const ADMIN_NOTICE_COMPONENT_KEY = WORKBENCH_COMPONENT_KEYS.adminStageNotice;
const AUDIT_NOTICE_COMPONENT_KEY = WORKBENCH_COMPONENT_KEYS.auditStageNotice;
const REVIEW_NOTICE_COMPONENT_KEY = WORKBENCH_COMPONENT_KEYS.reviewStageNotice;
const AUDIT_WORKBENCH_COMPONENT_KEY = WORKBENCH_COMPONENT_KEYS.auditWorkbench;
const REVIEW_WORKBENCH_COMPONENT_KEY = WORKBENCH_COMPONENT_KEYS.reviewWorkbench;
const NOTICE_COMPONENT_KEYS = WORKBENCH_STAGE_NOTICE_COMPONENT_KEYS;
const styleForm = reactive(cloneWorkbenchStyle(defaultWorkbenchStyleConfig));
const styleSnapshot = () => JSON.stringify(compactWorkbenchStyleConfig(styleForm));
const styleDirty = computed(() => Boolean(styleOriginalSnapshot.value) && styleOriginalSnapshot.value !== styleSnapshot());
const stylePreviewVars = computed(() => workbenchStylePreviewVars(styleForm));
const editorDirty = (editor: any) => Boolean(unref(editor?.isDirty));
const editorState = (editor: any, key: 'loading' | 'saving') => Boolean(unref(editor?.[key]));
const editorLabel = (editor: any, fallback: string) => String(unref(editor?.saveActionLabel) || fallback);
const tableLayoutDirty = computed(() => editorDirty(globalTableLayoutEditorRef.value));
const tableAppearanceDirty = computed(() => editorDirty(globalTableAppearanceEditorRef.value));
const pageHeaderDirty = computed(() => editorDirty(pageHeaderEditorRef.value));
const reviewDisplayDirty = computed(() => editorDirty(reviewDisplayEditorRef.value));
const activeBusinessConfigEditor = computed(() =>
  businessConfigTab.value === 'header' ? pageHeaderEditorRef.value : globalTableLayoutEditorRef.value
);
const activeBusinessConfigDirty = computed(() => (businessConfigTab.value === 'header' ? pageHeaderDirty.value : tableLayoutDirty.value));
const styleSectionDirty = computed(() => styleDirty.value || tableAppearanceDirty.value || reviewDisplayDirty.value);
const publicStyleDirtyMap = computed<Record<PublicStyleSectionKey, boolean>>(() => ({
  workbench: styleDirty.value,
  tableAppearance: tableAppearanceDirty.value,
  reviewDisplay: reviewDisplayDirty.value
}));
const sectionDirtyMap = computed<Record<WorkbenchSectionKey, boolean>>(() => ({
  layout: layoutDirty.value,
  shell: roleShellDirty.value,
  style: styleSectionDirty.value,
  business: tableLayoutDirty.value || pageHeaderDirty.value,
  migration: false
}));
const hasAnyWorkbenchDirty = computed(() => Object.values(sectionDirtyMap.value).some(Boolean));
const currentScopeDirty = computed(() => {
  if (activeWorkbenchTab.value === 'style') return publicStyleDirtyMap.value[publicStyleSection.value];
  if (activeWorkbenchTab.value === 'business') return activeBusinessConfigDirty.value;
  return sectionDirtyMap.value[activeWorkbenchTab.value];
});
const currentSectionLabel = computed(() => workbenchSections.find((section) => section.key === activeWorkbenchTab.value)?.label || '工作台配置');
const isRoleScopedSection = computed(() => activeWorkbenchTab.value === 'layout' || activeWorkbenchTab.value === 'shell');
const currentBusinessPageLabel = computed(() => businessPageOptions.find((item) => item.value === businessPageKey.value)?.label || '类别上报');
const currentPublicStyleLabel = computed(
  () => publicStyleSectionOptions.find((item) => item.value === publicStyleSection.value)?.label || '工作台与菜单'
);
const currentContextHeading = computed(() => {
  if (isRoleScopedSection.value) return '当前角色';
  if (activeWorkbenchTab.value === 'business') return '当前页面';
  if (activeWorkbenchTab.value === 'style') return '当前公共样式';
  return '当前工具';
});
const currentContextLabel = computed(() => {
  if (isRoleScopedSection.value) return selectedRoleName.value;
  if (activeWorkbenchTab.value === 'business') return currentBusinessPageLabel.value;
  if (activeWorkbenchTab.value === 'style') return currentPublicStyleLabel.value;
  return '配置包迁移';
});
const currentScopeLoading = computed(() => {
  if (activeWorkbenchTab.value === 'style') {
    if (publicStyleSection.value === 'workbench') return styleLoading.value;
    if (publicStyleSection.value === 'tableAppearance') return editorState(globalTableAppearanceEditorRef.value, 'loading');
    return editorState(reviewDisplayEditorRef.value, 'loading');
  }
  if (activeWorkbenchTab.value === 'shell') return roleShellLoading.value;
  if (activeWorkbenchTab.value === 'business') {
    return editorState(activeBusinessConfigEditor.value, 'loading');
  }
  return activeWorkbenchTab.value === 'layout' ? loading.value || componentLoading.value : false;
});
const currentScopeSaving = computed(() => {
  if (activeWorkbenchTab.value === 'layout') return saving.value;
  if (activeWorkbenchTab.value === 'shell') return roleShellSaving.value;
  if (activeWorkbenchTab.value === 'style') {
    if (publicStyleSection.value === 'workbench') return styleSaving.value;
    if (publicStyleSection.value === 'tableAppearance') return editorState(globalTableAppearanceEditorRef.value, 'saving');
    return editorState(reviewDisplayEditorRef.value, 'saving');
  }
  if (activeWorkbenchTab.value === 'business') {
    return editorState(activeBusinessConfigEditor.value, 'saving');
  }
  return false;
});
const currentScopeRestoring = computed(() =>
  activeWorkbenchTab.value === 'layout' ? resettingRoleLayout.value : activeWorkbenchTab.value === 'shell' ? roleShellRestoring.value : false
);
const currentScopeSavePermission = computed(() => {
  if (activeWorkbenchTab.value === 'business') return ['crehn:detailDisplayConfig:edit'];
  if (activeWorkbenchTab.value === 'style' && publicStyleSection.value !== 'workbench') return ['crehn:detailDisplayConfig:edit'];
  return ['system:workbench:edit'];
});
const canRefreshCurrentScope = computed(() => activeWorkbenchTab.value !== 'migration' && !currentScopeSaving.value && !currentScopeRestoring.value);
const canRestoreCurrentScope = computed(() => {
  if (currentScopeLoading.value || currentScopeSaving.value || currentScopeRestoring.value) return false;
  if (isRoleScopedSection.value) return Boolean(selectedRoleId.value);
  if (activeWorkbenchTab.value === 'business') return Boolean(activeBusinessConfigEditor.value);
  if (activeWorkbenchTab.value === 'style' && publicStyleSection.value === 'tableAppearance') {
    return Boolean(globalTableAppearanceEditorRef.value);
  }
  if (activeWorkbenchTab.value === 'style' && publicStyleSection.value === 'reviewDisplay') {
    return Boolean(reviewDisplayEditorRef.value);
  }
  return false;
});
const canCancelCurrentScope = computed(
  () => currentScopeDirty.value && !currentScopeLoading.value && !currentScopeSaving.value && !currentScopeRestoring.value
);
const canSaveCurrentScope = computed(() => {
  if (isRoleScopedSection.value && !selectedRoleId.value) return false;
  return activeWorkbenchTab.value !== 'migration' && currentScopeDirty.value && !currentScopeRestoring.value;
});
const currentSaveActionLabel = computed(() => {
  if (activeWorkbenchTab.value === 'business') {
    return businessConfigTab.value === 'header' ? '保存表头与按钮' : '保存表格列';
  }
  if (activeWorkbenchTab.value === 'style' && publicStyleSection.value === 'tableAppearance') {
    return editorLabel(globalTableAppearanceEditorRef.value, '保存全局表格设置');
  }
  if (activeWorkbenchTab.value === 'style' && publicStyleSection.value === 'reviewDisplay') {
    return editorLabel(reviewDisplayEditorRef.value, '保存艺术评审设置');
  }
  return '保存当前范围';
});
type WorkbenchConfigItem = {
  key: string;
  label: string;
  activityId?: string | number;
  sourceType?: 'group' | 'category';
  sourceId?: string | number;
  sourceCode?: string;
  sourceName?: string;
  sourceLevel?: 1 | 2;
  visible?: boolean;
  order?: number;
  width?: number;
  aliases?: string[];
  aliasesText?: string;
  showCount?: boolean;
  fontSize?: number;
  titleColor?: string;
  numberColor?: string;
  backgroundColor?: string;
  borderColor?: string;
  accentColor?: string;
  customCss?: string;
  patternMode?: string;
  patternPreset?: string;
  patternIcon?: string;
  patternText?: string;
  patternColor?: string;
  patternOpacity?: number;
  patternScale?: number;
  patternPosition?: string;
  patternImageKey?: string;
  patternImageName?: string;
  patternImageSize?: string;
  patternImageRepeat?: string;
  glassColor?: string;
  glassOpacity?: number;
  glassBlur?: number;
  textureOpacity?: number;
  highlightOpacity?: number;
  showQuota?: boolean;
  showSubmitted?: boolean;
  linkEnabled?: boolean;
  quotaDisplayMode?: 'none' | 'always' | 'hover';
  quotaFields?: string[];
  quotaPosition?: 'topRight' | 'belowStats' | 'bottom';
  secondary?: WorkbenchSecondaryConfig;
  secondaryVisible?: boolean;
  secondaryLabel?: string;
  secondaryValueKey?: string;
  secondaryFontSize?: number;
  secondaryTextColor?: string;
  secondaryNumberColor?: string;
};

type WorkbenchSecondaryConfig = {
  visible?: boolean;
  label?: string;
  valueKey?: string;
  fontSize?: number;
  textColor?: string;
  numberColor?: string;
};

type SchoolTypeLabelReplacement = {
  source: string;
  target: string;
};

type HoverEffectMode = 'none' | 'text' | 'pattern' | 'textPattern';

type ProjectSubmitConfig = {
  componentBackground: string;
  componentBackgroundOpacity: number;
  componentBorderVisible: boolean;
  componentBorderColor: string;
  componentBorderRadius: number;
  defaultTabKey?: string;
  expandedPadding: number;
  expandedGap: number;
  expandedBackground: string;
  expandedBackgroundOpacity: number;
  expandedLayoutVersion: number;
  sceneHeadersVersion: number;
  homeHeader: ArtWorkspaceHeaderPageConfig;
  maximizedHeader: ArtWorkspaceHeaderPageConfig;
  progress: ArtAuditProgressConfig;
  tabs: WorkbenchConfigItem[];
};

type StageNoticeConfig = {
  categoryConfigMode?: 'legacy' | 'activity';
  topHeight: number;
  topGap: number;
  noticeRatio: number;
  activityRatio: number;
  calendarWidth: number;
  calendarHeight: number;
  calendarRadius: number;
  calendarBackground: string;
  calendarTextColor: string;
  calendarMonthFontSize: number;
  calendarDayFontSize: number;
  calendarWeekdayFontSize: number;
  calendarTimeFontSize: number;
  calendarTimeBackground: string;
  calendarShowTime: boolean;
  calendarShowSeconds: boolean;
  quotaSummaryEnabled: boolean;
  quotaSummaryFields: string[];
  quotaSummaryTemplate: string;
  quotaSummaryFontSize: number;
  quotaSummaryTextColor: string;
  quotaSummaryNumberColor: string;
  quotaSummaryBackground: string;
  quotaSummaryBorderColor: string;
  quotaSummaryRadius: number;
  quotaSummaryGap: number;
  quotaSummaryCss: string;
  quotaRatioEnabled: boolean;
  quotaRatioMaxItems: number;
  quotaRatioFontSize: number;
  quotaRatioTextColor: string;
  quotaRatioNumberColor: string;
  quotaRatioBackground: string;
  quotaRatioBorderColor: string;
  quotaRatioRadius: number;
  quotaRatioCss: string;
  cardRatioEnabled: boolean;
  schoolTypeLabelReplacements: SchoolTypeLabelReplacement[];
  categoryColumns: number;
  categoryGap: number;
  categoryRadius: number;
  hoverEffectMode: HoverEffectMode;
  hoverScaleEnabled: boolean;
  hoverScale: number;
  categoryAreaBackground: string;
  componentCss: string;
  dateCss: string;
  categories: WorkbenchConfigItem[];
};

type AuditWorkbenchConfig = {
  kickerVisible: boolean;
  kickerText: string;
  kickerFontSize: number;
  titleVisible: boolean;
  titleText: string;
  titleFontSize: number;
  refreshVisible: boolean;
  defaultTab: string;
  pendingTabVisible: boolean;
  pendingTabLabel: string;
  pendingSectionTitle: string;
  auditListButtonVisible: boolean;
  auditListButtonText: string;
  mineTabVisible: boolean;
  mineTabLabel: string;
  metricsVisible: boolean;
  metricColumns: number;
  metricTotalLabel: string;
  metricPassLabel: string;
  metricReturnLabel: string;
  metricLabelColor: string;
  metricTotalColor: string;
  metricPassColor: string;
  metricReturnColor: string;
  metricFontSize: number;
  metricNumberFontSize: number;
  groupVisible: boolean;
  groupSectionTitle: string;
  groupSectionSubtitle: string;
  groupColumns: number;
  recordsVisible: boolean;
  recordsSectionTitle: string;
  recordsSectionSubtitle: string;
  componentCss: string;
};

type ReviewWorkbenchCardConfig = {
  key: string;
  label: string;
  valueKey?: string;
  visible?: boolean;
  order?: number;
  imageUrl?: string;
  titleColor?: string;
  numberColor?: string;
  backgroundColor?: string;
  borderColor?: string;
  accentColor?: string;
  linkStatus?: string;
};

type ReviewWorkbenchConfig = {
  kickerVisible: boolean;
  kickerText: string;
  kickerFontSize: number;
  titleVisible: boolean;
  titleText: string;
  titleFontSize: number;
  refreshVisible: boolean;
  listButtonVisible: boolean;
  listButtonText: string;
  cardColumns: number;
  pendingTabLabel: string;
  recentTabLabel: string;
  componentCss: string;
  cards: ReviewWorkbenchCardConfig[];
};

const defaultNoticeLayoutConfig = {
  topHeight: 220,
  topGap: 18,
  noticeRatio: 0.9,
  activityRatio: 1.4,
  calendarWidth: 190,
  calendarHeight: 220,
  calendarRadius: 8,
  calendarBackground: 'linear-gradient(180deg,#2563eb 0%,#0f62b9 100%)',
  calendarTextColor: '#ffffff',
  calendarMonthFontSize: 14,
  calendarDayFontSize: 54,
  calendarWeekdayFontSize: 17,
  calendarTimeFontSize: 12,
  calendarTimeBackground: 'rgba(255,255,255,0.18)',
  calendarShowTime: true,
  calendarShowSeconds: true,
  quotaSummaryEnabled: true,
  quotaSummaryFields: ['schoolType', 'totalCount', 'usedCount', 'quotaLimit', 'remainingCount'],
  quotaSummaryTemplate: '',
  quotaSummaryFontSize: 13,
  quotaSummaryTextColor: '#214567',
  quotaSummaryNumberColor: '#1d4ed8',
  quotaSummaryBackground: '#f4f9ff',
  quotaSummaryBorderColor: '#bfdbfe',
  quotaSummaryRadius: 8,
  quotaSummaryGap: 8,
  quotaSummaryCss: '',
  quotaRatioEnabled: false,
  quotaRatioMaxItems: 4,
  quotaRatioFontSize: 12,
  quotaRatioTextColor: '#40566f',
  quotaRatioNumberColor: '#2563eb',
  quotaRatioBackground: '#ffffff',
  quotaRatioBorderColor: '#d8e6f5',
  quotaRatioRadius: 8,
  quotaRatioCss: '',
  cardRatioEnabled: false,
  schoolTypeLabelReplacements: [{ source: '高职', target: '高职高专' }]
};

const defaultProjectSubmitConfig: ProjectSubmitConfig = {
  componentBackground: '#ffffff',
  componentBackgroundOpacity: 0,
  componentBorderVisible: false,
  componentBorderColor: '#e8edf5',
  componentBorderRadius: 8,
  defaultTabKey: 'all',
  expandedPadding: 0,
  expandedGap: 16,
  expandedBackground: '#f6f8fc',
  expandedBackgroundOpacity: 0,
  expandedLayoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
  sceneHeadersVersion: 2,
  homeHeader: defaultSchoolSubmitHomeHeader(),
  maximizedHeader: defaultSchoolSubmitMaximizedHeader(),
  progress: defaultSchoolSubmitProgress(),
  tabs: [
    { key: 'all', label: '全部类别', aliases: [], visible: true, order: 1, showCount: true },
    { key: 'vocal', label: '声乐', aliases: ['声乐', 'performance_vocal'], visible: true, order: 2, showCount: true },
    { key: 'instrumental', label: '器乐', aliases: ['器乐', 'performance_instrumental'], visible: true, order: 3, showCount: true },
    { key: 'dance', label: '舞蹈', aliases: ['舞蹈', 'performance_dance'], visible: true, order: 4, showCount: true },
    { key: 'drama', label: '戏剧', aliases: ['戏剧', 'performance_drama'], visible: true, order: 5, showCount: true },
    { key: 'recitation', label: '朗诵', aliases: ['朗诵', 'performance_recitation'], visible: true, order: 6, showCount: true },
    { key: 'fine_art', label: '美术类', aliases: ['美术', 'artwork_fine_art'], visible: true, order: 7, showCount: true },
    { key: 'grand_design', label: '大艺展设计类', aliases: ['大艺展设计', 'artwork_grand_design'], visible: true, order: 8, showCount: true },
    { key: 'design', label: '设计展', aliases: ['设计展', 'artwork_design'], visible: true, order: 9, showCount: true },
    { key: 'film', label: '影视类', aliases: ['影视', '影像', 'artwork_film'], visible: true, order: 10, showCount: true },
    { key: 'principal', label: '高校校长书画作品', aliases: ['校长书画', 'artwork_principal'], visible: true, order: 11, showCount: true }
  ]
};

const defaultStageNoticeConfig: StageNoticeConfig = {
  ...defaultNoticeLayoutConfig,
  categoryColumns: 5,
  categoryGap: 12,
  categoryRadius: 8,
  hoverEffectMode: 'textPattern',
  hoverScaleEnabled: true,
  hoverScale: 1.04,
  categoryAreaBackground: '#f8fbff',
  componentCss: '',
  dateCss: '',
  categories: [
    {
      key: 'performance',
      label: '艺术表演类',
      aliases: ['performance', 'performance_', '声乐', '器乐', '舞蹈', '戏剧', '朗诵'],
      visible: true,
      order: 1,
      fontSize: 15,
      titleColor: '#082f49',
      numberColor: '#2563eb',
      backgroundColor: '#f8fbff',
      borderColor: '#bfd7ff',
      accentColor: '#2563eb',
      customCss: '',
      showQuota: true,
      showSubmitted: true,
      linkEnabled: true
    },
    {
      key: 'artwork',
      label: '艺术作品类',
      aliases: ['artwork', 'artwork_', '美术', '设计', '影视'],
      visible: true,
      order: 2,
      fontSize: 15,
      titleColor: '#083344',
      numberColor: '#0891b2',
      backgroundColor: '#f7fcff',
      borderColor: '#c7e6f4',
      accentColor: '#0284c7',
      customCss: '',
      showQuota: true,
      showSubmitted: true,
      linkEnabled: true
    },
    {
      key: 'workshop',
      label: '艺术实践工作坊',
      aliases: ['workshop', '工作坊'],
      visible: true,
      order: 3,
      fontSize: 15,
      titleColor: '#064e3b',
      numberColor: '#059669',
      backgroundColor: '#f6fdfa',
      borderColor: '#cdeee1',
      accentColor: '#0d9488',
      customCss: '',
      showQuota: true,
      showSubmitted: true,
      linkEnabled: true
    },
    {
      key: 'achievement',
      label: '高校美育改革创新优秀成果',
      aliases: ['achievement', 'achievement_', '论文', '案例', '成果', '美育'],
      visible: true,
      order: 4,
      fontSize: 15,
      titleColor: '#422006',
      numberColor: '#d97706',
      backgroundColor: '#fffaf0',
      borderColor: '#f4d99d',
      accentColor: '#d97706',
      customCss: '',
      showQuota: true,
      showSubmitted: true,
      linkEnabled: true
    },
    {
      key: 'principal',
      label: '高校校长书画作品',
      aliases: ['principal', 'artwork_principal', '校长书画'],
      visible: true,
      order: 5,
      fontSize: 15,
      titleColor: '#312e81',
      numberColor: '#4f46e5',
      backgroundColor: '#f5f7ff',
      borderColor: '#cbd5ff',
      accentColor: '#4f46e5',
      customCss: '',
      showQuota: true,
      showSubmitted: true,
      linkEnabled: true
    }
  ]
};

const defaultAdminNoticeConfig: StageNoticeConfig = {
  ...defaultNoticeLayoutConfig,
  categoryColumns: 6,
  categoryGap: 12,
  categoryRadius: 8,
  hoverEffectMode: 'textPattern',
  hoverScaleEnabled: true,
  hoverScale: 1.04,
  categoryAreaBackground: '#f8fbff',
  componentCss: '',
  dateCss: '',
  categories: [
    {
      key: 'school_total',
      label: '学校总数',
      visible: true,
      order: 1,
      fontSize: 15,
      titleColor: '#082f49',
      numberColor: '#2563eb',
      backgroundColor: '#f8fbff',
      borderColor: '#bfd7ff',
      accentColor: '#2563eb',
      customCss: ''
    },
    {
      key: 'submitted_schools',
      label: '已报送学校',
      visible: true,
      order: 2,
      fontSize: 15,
      titleColor: '#083344',
      numberColor: '#0891b2',
      backgroundColor: '#f7fcff',
      borderColor: '#c7e6f4',
      accentColor: '#0284c7',
      customCss: ''
    },
    {
      key: 'unsubmitted_schools',
      label: '未报送学校',
      visible: true,
      order: 3,
      fontSize: 15,
      titleColor: '#422006',
      numberColor: '#d97706',
      backgroundColor: '#fffaf0',
      borderColor: '#f4d99d',
      accentColor: '#d97706',
      customCss: ''
    },
    {
      key: 'project_total',
      label: '项目总数',
      visible: true,
      order: 4,
      fontSize: 15,
      titleColor: '#064e3b',
      numberColor: '#059669',
      backgroundColor: '#f6fdfa',
      borderColor: '#cdeee1',
      accentColor: '#0d9488',
      customCss: ''
    },
    {
      key: 'pending_audit',
      label: '待审核项目',
      visible: true,
      order: 5,
      fontSize: 15,
      titleColor: '#312e81',
      numberColor: '#4f46e5',
      backgroundColor: '#f5f7ff',
      borderColor: '#cbd5ff',
      accentColor: '#4f46e5',
      customCss: ''
    },
    {
      key: 'audit_passed',
      label: '已通过项目',
      visible: true,
      order: 6,
      fontSize: 15,
      titleColor: '#14532d',
      numberColor: '#16a34a',
      backgroundColor: '#f0fdf4',
      borderColor: '#bbf7d0',
      accentColor: '#22c55e',
      customCss: ''
    }
  ]
};

const defaultAuditNoticeConfig: StageNoticeConfig = {
  ...defaultNoticeLayoutConfig,
  categoryColumns: 5,
  categoryGap: 12,
  categoryRadius: 8,
  hoverEffectMode: 'textPattern',
  hoverScaleEnabled: true,
  hoverScale: 1.04,
  categoryAreaBackground: '#f8fbff',
  componentCss: '',
  dateCss: '',
  categories: [
    {
      key: 'assigned_total',
      label: '分配总数',
      visible: true,
      order: 1,
      fontSize: 15,
      titleColor: '#082f49',
      numberColor: '#2563eb',
      backgroundColor: '#f8fbff',
      borderColor: '#bfd7ff',
      accentColor: '#2563eb',
      customCss: ''
    },
    {
      key: 'pending_audit',
      label: '待审核',
      visible: true,
      order: 2,
      fontSize: 15,
      titleColor: '#422006',
      numberColor: '#d97706',
      backgroundColor: '#fffaf0',
      borderColor: '#f4d99d',
      accentColor: '#d97706',
      customCss: ''
    },
    {
      key: 'audited_total',
      label: '已审核',
      visible: true,
      order: 3,
      fontSize: 15,
      titleColor: '#083344',
      numberColor: '#0891b2',
      backgroundColor: '#f7fcff',
      borderColor: '#c7e6f4',
      accentColor: '#0284c7',
      customCss: '',
      secondary: { visible: true, label: '我已审核', valueKey: 'my_audited_total', fontSize: 13, textColor: '#5c7087', numberColor: '#0891b2' }
    },
    {
      key: 'audit_passed',
      label: '审核通过',
      visible: true,
      order: 4,
      fontSize: 15,
      titleColor: '#14532d',
      numberColor: '#16a34a',
      backgroundColor: '#f0fdf4',
      borderColor: '#bbf7d0',
      accentColor: '#22c55e',
      customCss: '',
      secondary: { visible: true, label: '我已审核通过', valueKey: 'my_audit_passed', fontSize: 13, textColor: '#527162', numberColor: '#16a34a' }
    },
    {
      key: 'returned',
      label: '审核退回',
      visible: true,
      order: 5,
      fontSize: 15,
      titleColor: '#7f1d1d',
      numberColor: '#dc2626',
      backgroundColor: '#fef2f2',
      borderColor: '#fecaca',
      accentColor: '#ef4444',
      customCss: '',
      secondary: { visible: true, label: '我已审核退回', valueKey: 'my_returned', fontSize: 13, textColor: '#7f4d4d', numberColor: '#dc2626' }
    }
  ]
};

const defaultReviewNoticeConfig: StageNoticeConfig = {
  ...defaultNoticeLayoutConfig,
  categoryColumns: 5,
  categoryGap: 12,
  categoryRadius: 8,
  hoverEffectMode: 'textPattern',
  hoverScaleEnabled: true,
  hoverScale: 1.04,
  categoryAreaBackground: '#f8fbff',
  componentCss: '',
  dateCss: '',
  categories: [
    {
      key: 'assigned_total',
      label: '分配总数',
      visible: true,
      order: 1,
      fontSize: 15,
      titleColor: '#082f49',
      numberColor: '#2563eb',
      backgroundColor: '#f8fbff',
      borderColor: '#bfd7ff',
      accentColor: '#2563eb',
      customCss: ''
    },
    {
      key: 'pending_score',
      label: '待评分',
      visible: true,
      order: 2,
      fontSize: 15,
      titleColor: '#422006',
      numberColor: '#d97706',
      backgroundColor: '#fffaf0',
      borderColor: '#f4d99d',
      accentColor: '#d97706',
      customCss: ''
    },
    {
      key: 'draft_score',
      label: '草稿',
      visible: true,
      order: 3,
      fontSize: 15,
      titleColor: '#083344',
      numberColor: '#0891b2',
      backgroundColor: '#f7fcff',
      borderColor: '#c7e6f4',
      accentColor: '#0284c7',
      customCss: ''
    },
    {
      key: 'submitted_score',
      label: '已提交',
      visible: true,
      order: 4,
      fontSize: 15,
      titleColor: '#14532d',
      numberColor: '#16a34a',
      backgroundColor: '#f0fdf4',
      borderColor: '#bbf7d0',
      accentColor: '#22c55e',
      customCss: ''
    },
    {
      key: 'locked_by_other',
      label: '已被锁定',
      visible: true,
      order: 5,
      fontSize: 15,
      titleColor: '#7f1d1d',
      numberColor: '#dc2626',
      backgroundColor: '#fef2f2',
      borderColor: '#fecaca',
      accentColor: '#ef4444',
      customCss: ''
    }
  ]
};

const defaultAuditWorkbenchConfig: AuditWorkbenchConfig = {
  kickerVisible: true,
  kickerText: 'AUDIT',
  kickerFontSize: 12,
  titleVisible: true,
  titleText: '',
  titleFontSize: 20,
  refreshVisible: true,
  defaultTab: 'pending',
  pendingTabVisible: true,
  pendingTabLabel: '待审核',
  pendingSectionTitle: '最近待审核',
  auditListButtonVisible: true,
  auditListButtonText: '进入审核列表',
  mineTabVisible: true,
  mineTabLabel: '我的审核',
  metricsVisible: true,
  metricColumns: 3,
  metricTotalLabel: '我审核总数',
  metricPassLabel: '通过',
  metricReturnLabel: '退回',
  metricLabelColor: '#5c7087',
  metricTotalColor: '#2563eb',
  metricPassColor: '#16a34a',
  metricReturnColor: '#dc2626',
  metricFontSize: 13,
  metricNumberFontSize: 18,
  groupVisible: true,
  groupSectionTitle: '大类简版统计',
  groupSectionSubtitle: '按本人通过/退回记录汇总',
  groupColumns: 5,
  recordsVisible: true,
  recordsSectionTitle: '最近审核记录',
  recordsSectionSubtitle: '仅统计本人点击通过/退回的项目',
  componentCss: ''
};

const defaultReviewWorkbenchConfig: ReviewWorkbenchConfig = {
  kickerVisible: true,
  kickerText: 'SCORE',
  kickerFontSize: 12,
  titleVisible: true,
  titleText: '',
  titleFontSize: 20,
  refreshVisible: true,
  listButtonVisible: true,
  listButtonText: '进入评分列表',
  cardColumns: 5,
  pendingTabLabel: '待评分',
  recentTabLabel: '最近评分',
  componentCss: '',
  cards: [
    {
      key: 'assigned_total',
      label: '分配总数',
      valueKey: 'assigned_total',
      visible: true,
      order: 1,
      imageUrl: '',
      titleColor: '#082f49',
      numberColor: '#2563eb',
      backgroundColor: '#f8fbff',
      borderColor: '#bfd7ff',
      accentColor: '#2563eb',
      linkStatus: 'all'
    },
    {
      key: 'pending_score',
      label: '待评分',
      valueKey: 'pending_score',
      visible: true,
      order: 2,
      imageUrl: '',
      titleColor: '#422006',
      numberColor: '#d97706',
      backgroundColor: '#fffaf0',
      borderColor: '#f4d99d',
      accentColor: '#d97706',
      linkStatus: 'none'
    },
    {
      key: 'draft_score',
      label: '草稿',
      valueKey: 'draft_score',
      visible: true,
      order: 3,
      imageUrl: '',
      titleColor: '#083344',
      numberColor: '#0891b2',
      backgroundColor: '#f7fcff',
      borderColor: '#c7e6f4',
      accentColor: '#0284c7',
      linkStatus: 'draft'
    },
    {
      key: 'submitted_score',
      label: '已提交',
      valueKey: 'submitted_score',
      visible: true,
      order: 4,
      imageUrl: '',
      titleColor: '#14532d',
      numberColor: '#16a34a',
      backgroundColor: '#f0fdf4',
      borderColor: '#bbf7d0',
      accentColor: '#22c55e',
      linkStatus: 'submitted'
    },
    {
      key: 'locked_by_other',
      label: '已锁定',
      valueKey: 'locked_by_other',
      visible: true,
      order: 5,
      imageUrl: '',
      titleColor: '#7f1d1d',
      numberColor: '#dc2626',
      backgroundColor: '#fef2f2',
      borderColor: '#fecaca',
      accentColor: '#ef4444',
      linkStatus: ''
    }
  ]
};

const configDialog = reactive({
  visible: false,
  activeTab: 'appearance'
});
const configTargetRow = ref<WorkbenchLayoutVO>();
const configForm = reactive<ProjectSubmitConfig>(cloneProjectSubmitConfig(defaultProjectSubmitConfig));
const projectSubmitCategoryLoading = ref(false);
const projectSubmitCurrentActivity = ref<ActivityVO>();
const projectSubmitCategoryCatalog = ref<ActivityCategoryVO[]>([]);
const projectSubmitCategorySyncTitle = computed(() => {
  const activityName = String(projectSubmitCurrentActivity.value?.activityName || '').trim();
  if (!activityName) return '未找到当前启用活动，暂时保留现有筛选配置。';
  const firstLevelCount = configForm.tabs.filter((item) => item.key !== 'all' && item.sourceLevel === 1).length;
  const secondLevelCount = configForm.tabs.filter((item) => item.sourceLevel === 2).length;
  return `已同步当前活动“${activityName}”：一级类别 ${firstLevelCount} 个，二级类别 ${secondLevelCount} 个；显示名称仍可自定义。`;
});
const stageConfigDialog = reactive({
  visible: false,
  activeTab: 'categories'
});
const stageConfigTargetRow = ref<WorkbenchLayoutVO>();
const stageConfigForm = reactive<StageNoticeConfig>(cloneStageNoticeConfig(defaultStageNoticeConfig));
const stageConfigKind = ref<'school' | 'admin' | 'audit' | 'review'>('school');
const stageActivityOptions = ref<ActivityVO[]>([]);
const stageCategoryCatalog = ref<ActivityCategoryVO[]>([]);
const stageConfigActivityId = ref<string | number>();
const stageCardSourceKey = ref('');
const stageActivityLoading = ref(false);
const stageCategoryLoading = ref(false);
const categoryVisualDialog = reactive({
  visible: false,
  targetIndex: -1
});
const categoryVisualForm = reactive<WorkbenchConfigItem>({
  key: '',
  label: ''
});
const categoryVisualAssetUploading = ref(false);
const categoryVisualAssetDeleting = ref(false);
const quotaFieldOptions = [
  { label: '总限额', value: 'quotaLimit' },
  { label: '已用数量', value: 'usedCount' },
  { label: '剩余数量', value: 'remainingCount' }
];
const quotaPositionOptions = [
  { label: '右上角角标', value: 'topRight' },
  { label: '统计数字下方', value: 'belowStats' },
  { label: '卡片底部', value: 'bottom' }
];
const auditWorkbenchConfigDialog = reactive({
  visible: false,
  activeTab: 'base'
});
const auditWorkbenchConfigTargetRow = ref<WorkbenchLayoutVO>();
const auditWorkbenchConfigForm = reactive<AuditWorkbenchConfig>(cloneAuditWorkbenchConfig(defaultAuditWorkbenchConfig));
const reviewWorkbenchConfigDialog = reactive({
  visible: false,
  activeTab: 'base'
});
const reviewWorkbenchConfigTargetRow = ref<WorkbenchLayoutVO>();
const reviewWorkbenchConfigForm = reactive<ReviewWorkbenchConfig>(cloneReviewWorkbenchConfig(defaultReviewWorkbenchConfig));
const universalDashboardConfigDialog = reactive({ visible: false });
const universalDashboardConfigTargetRow = ref<WorkbenchLayoutVO>();
const auditSecondaryFieldOptions = [
  { label: '我已审核总数', value: 'my_audited_total' },
  { label: '我已审核通过', value: 'my_audit_passed' },
  { label: '我已审核退回', value: 'my_returned' }
];
const visualPatternOptions = [
  { label: '柔光流线', value: 'softFlow' },
  { label: '舞台光束', value: 'stageBeam' },
  { label: '几何网格', value: 'geoGrid' },
  { label: '宣纸墨韵', value: 'inkPaper' },
  { label: '奖章浮雕', value: 'medalRelief' },
  { label: '书画水印', value: 'calligraphyMark' },
  { label: '纯玻璃', value: 'pureGlass' }
];
const visualPositionOptions = [
  { label: '居中铺满', value: 'center' },
  { label: '右侧铺满', value: 'right' },
  { label: '左侧铺满', value: 'left' },
  { label: '底部铺满', value: 'bottom' }
];
const visualImageSizeOptions = [
  { label: '铺满裁切', value: 'cover' },
  { label: '完整显示', value: 'contain' },
  { label: '原始尺寸', value: 'auto' },
  { label: '80%', value: '80%' },
  { label: '120%', value: '120%' }
];
const visualImageRepeatOptions = [
  { label: '不平铺', value: 'no-repeat' },
  { label: '平铺', value: 'repeat' }
];
type StageCategorySourceOption = {
  optionKey: string;
  sourceType: 'group' | 'category';
  sourceId: string | number;
  sourceCode: string;
  label: string;
  searchText: string;
  node: ActivityCategoryTreeNode;
};
const defaultQuotaFields = ['quotaLimit', 'usedCount', 'remainingCount'];
const stageConfigCategoryRows = computed(() => {
  if (stageConfigKind.value !== 'school') return stageConfigForm.categories;
  const activityId = stageConfigActivityId.value;
  if (activityId === undefined || activityId === null || activityId === '') return [];
  return stageConfigForm.categories.filter(
    (item) =>
      String(item.activityId ?? '') === String(activityId) ||
      (!item.activityId && stageConfigForm.categoryConfigMode !== 'activity' && !item.sourceType)
  );
});
const stageAvailableRootNodes = computed<ActivityCategoryTreeNode[]>(() =>
  buildActivityCategoryTree(stageCategoryCatalog.value, true)
    .map((node) => {
      if (!isCategoryGroup(node)) {
        return isCategoryMenuAvailable(node, stageCategoryCatalog.value) ? node : undefined;
      }
      if (node.enabled === false || !isCategoryMenuVisible(node)) return undefined;
      const children = sortCategoryNodes((node.children || []).filter((item) => isCategoryMenuAvailable(item, stageCategoryCatalog.value)));
      return children.length ? { ...node, children } : undefined;
    })
    .filter((item): item is ActivityCategoryTreeNode => Boolean(item))
);
const stageCategorySourceOptions = computed<StageCategorySourceOption[]>(() =>
  stageAvailableRootNodes.value.map((node) => {
    const sourceType = isCategoryGroup(node) ? 'group' : 'category';
    const sourceId = node.id ?? node.categoryCode ?? node.categoryName ?? `${sourceType}-${node.sortOrder || 0}`;
    const sourceCode = String(node.categoryCode || '');
    const childText = (node.children || [])
      .flatMap((item) => [item.categoryCode, item.categoryName, item.categoryGroup])
      .filter(Boolean)
      .join(' ');
    return {
      optionKey: `${sourceType}:${String(sourceId)}`,
      sourceType,
      sourceId,
      sourceCode,
      label: String(node.categoryName || sourceCode || '未命名类别'),
      searchText: [sourceId, sourceCode, node.categoryName, node.categoryGroup, childText].filter(Boolean).join(' ').toLowerCase(),
      node
    };
  })
);
const stageAddableSourceOptions = computed(() => {
  const used = new Set(
    stageConfigCategoryRows.value
      .filter((item) => item.sourceType && item.sourceId !== undefined && item.sourceId !== null)
      .map((item) => `${item.sourceType}:${String(item.sourceId)}`)
  );
  return stageCategorySourceOptions.value.filter((item) => !used.has(item.optionKey));
});
const stageCardSourceOption = (row: WorkbenchConfigItem) =>
  stageCategorySourceOptions.value.find(
    (item) =>
      (row.sourceType === item.sourceType && row.sourceId !== undefined && String(row.sourceId) === String(item.sourceId)) ||
      (!!row.sourceCode && row.sourceCode === item.sourceCode)
  );
const stageCardSourceLabel = (row: WorkbenchConfigItem) => {
  const option = stageCardSourceOption(row);
  if (option) return option.label;
  return `${row.sourceName || row.label || row.sourceCode || '未绑定来源'}（来源已失效）`;
};
const normalizeQuotaFields = (value?: string[]) => {
  const allowed = new Set(defaultQuotaFields);
  const fields = (Array.isArray(value) ? value : defaultQuotaFields).map((item) => String(item || '')).filter((item) => allowed.has(item));
  return fields.length ? fields : [...defaultQuotaFields];
};
const normalizeQuotaDisplayMode = (value?: string, showQuota?: boolean): 'none' | 'always' | 'hover' => {
  if (value === 'none' || value === 'always' || value === 'hover') return value;
  return showQuota === false ? 'none' : 'hover';
};
const normalizeQuotaPosition = (value?: string): 'topRight' | 'belowStats' | 'bottom' =>
  value === 'belowStats' || value === 'bottom' || value === 'topRight' ? value : 'topRight';
const stageCategoryCardKey = (activityId: string | number, source: StageCategorySourceOption) =>
  `activity_${String(activityId).replace(/[^a-zA-Z0-9_-]/g, '_')}_${source.sourceType}_${String(source.sourceId).replace(/[^a-zA-Z0-9_-]/g, '_')}`;
const createStageCategoryCard = (source: StageCategorySourceOption): WorkbenchConfigItem => {
  const activityId = stageConfigActivityId.value!;
  const styleIndex = stageConfigCategoryRows.value.length % defaultStageNoticeConfig.categories.length;
  const styleTemplate = cloneStageNoticeConfig(defaultStageNoticeConfig).categories[styleIndex] || defaultStageNoticeConfig.categories[0];
  return {
    ...styleTemplate,
    key: stageCategoryCardKey(activityId, source),
    label: source.label,
    activityId,
    sourceType: source.sourceType,
    sourceId: source.sourceId,
    sourceCode: source.sourceCode,
    sourceName: source.label,
    aliases: [],
    aliasesText: '',
    visible: true,
    order: stageConfigCategoryRows.value.length + 1,
    quotaDisplayMode: 'hover',
    quotaFields: [...defaultQuotaFields],
    quotaPosition: 'topRight',
    showQuota: true,
    showSubmitted: true,
    linkEnabled: true
  };
};
const addStageCategoryCardFromSource = (source: StageCategorySourceOption) => {
  stageConfigForm.categories.push(createStageCategoryCard(source));
};
const addStageCategoryCard = () => {
  const source = stageAddableSourceOptions.value.find((item) => item.optionKey === stageCardSourceKey.value);
  if (!source) return;
  addStageCategoryCardFromSource(source);
  stageCardSourceKey.value = '';
};
const syncStageCategoryCards = () => {
  const missing = [...stageAddableSourceOptions.value];
  missing.forEach(addStageCategoryCardFromSource);
  proxy?.$modal.msgSuccess(missing.length ? `已补充 ${missing.length} 张分类卡片` : '当前活动根类别已全部配置');
};
const removeStageCategoryCard = (row: WorkbenchConfigItem) => {
  const index = stageConfigForm.categories.indexOf(row);
  if (index >= 0) {
    stageConfigForm.categories.splice(index, 1);
  }
};
const legacyCardMatchScore = (row: WorkbenchConfigItem, source: StageCategorySourceOption) => {
  const label = String(row.label || '')
    .trim()
    .toLowerCase();
  const key = String(row.key || '')
    .trim()
    .toLowerCase();
  const aliases = (row.aliases || [])
    .map((item) =>
      String(item || '')
        .trim()
        .toLowerCase()
    )
    .filter(Boolean);
  if (label && source.label.toLowerCase() === label) return 100;
  if (source.sourceCode && aliases.includes(source.sourceCode.toLowerCase())) return 90;
  if (key && (source.sourceCode.toLowerCase() === key || source.searchText.includes(key))) return 80;
  if (aliases.some((alias) => source.searchText.includes(alias))) return 70;
  if (label && source.searchText.includes(label)) return 60;
  return 0;
};
const bindLegacyStageCategoryCards = () => {
  if (stageConfigForm.categoryConfigMode === 'activity' || !stageConfigActivityId.value) return;
  const used = new Set<string>();
  stageConfigForm.categories.forEach((row) => {
    if (row.activityId || row.sourceType) return;
    const matched = stageCategorySourceOptions.value
      .filter((source) => !used.has(source.optionKey))
      .map((source) => ({ source, score: legacyCardMatchScore(row, source) }))
      .filter((item) => item.score > 0)
      .sort((a, b) => b.score - a.score)[0]?.source;
    if (!matched) return;
    used.add(matched.optionKey);
    row.key = stageCategoryCardKey(stageConfigActivityId.value!, matched);
    row.activityId = stageConfigActivityId.value;
    row.sourceType = matched.sourceType;
    row.sourceId = matched.sourceId;
    row.sourceCode = matched.sourceCode;
    row.sourceName = matched.label;
    row.quotaDisplayMode = normalizeQuotaDisplayMode(row.quotaDisplayMode, row.showQuota);
    row.quotaFields = normalizeQuotaFields(row.quotaFields);
    row.quotaPosition = normalizeQuotaPosition(row.quotaPosition);
  });
};
const loadStageCategoryCatalog = async () => {
  stageCardSourceKey.value = '';
  stageCategoryCatalog.value = [];
  if (!stageConfigActivityId.value) return;
  stageCategoryLoading.value = true;
  try {
    const res = await listCategory(stageConfigActivityId.value, { deleted: false });
    stageCategoryCatalog.value = res.data || [];
    bindLegacyStageCategoryCards();
  } finally {
    stageCategoryLoading.value = false;
  }
};
const handleStageConfigActivityChange = async () => {
  await loadStageCategoryCatalog();
};
const loadStageActivityOptions = async () => {
  stageActivityLoading.value = true;
  try {
    const res = await listActivityOptions();
    stageActivityOptions.value = res.data || [];
    const configuredActivityId = stageConfigForm.categories.find((item) => item.activityId !== undefined && item.activityId !== null)?.activityId;
    const preferredId =
      configuredActivityId !== undefined && stageActivityOptions.value.some((item) => String(item.id) === String(configuredActivityId))
        ? configuredActivityId
        : (stageActivityOptions.value.find((item) => item.status === 'enabled')?.id ?? stageActivityOptions.value[0]?.id);
    stageConfigActivityId.value = preferredId;
    await loadStageCategoryCatalog();
  } finally {
    stageActivityLoading.value = false;
  }
};
const stageConfigTitle = computed(() => {
  const map = {
    school: '学校活动公告组件配置',
    admin: '管理员公告组件配置',
    audit: '审核公告组件配置',
    review: '评审公告组件配置'
  };
  return map[stageConfigKind.value];
});
const categoryVisualTitle = computed(() => `分类卡视觉配置${categoryVisualForm.label ? ` - ${categoryVisualForm.label}` : ''}`);

const clampNumber = (value: unknown, fallback: number, min: number, max: number) => {
  const numeric = Number(value ?? fallback);
  if (!Number.isFinite(numeric)) return fallback;
  return Math.min(max, Math.max(min, numeric));
};

const normalizeHoverScale = (value?: number) => Math.min(1.3, Math.max(1, Number(value || 1.08)));
const hoverEffectModes: HoverEffectMode[] = ['none', 'text', 'pattern', 'textPattern'];
const normalizeHoverEffectMode = (value?: string, enabled?: boolean): HoverEffectMode => {
  const mode = String(value || '') as HoverEffectMode;
  if (hoverEffectModes.includes(mode)) return mode;
  return enabled === false ? 'none' : 'text';
};
const hoverTextEnabled = (mode?: string) => {
  const normalized = normalizeHoverEffectMode(mode, false);
  return normalized === 'text' || normalized === 'textPattern';
};
const hoverPatternEnabled = (mode?: string) => {
  const normalized = normalizeHoverEffectMode(mode, false);
  return normalized === 'pattern' || normalized === 'textPattern';
};

const categoryVisualImageUrl = computed(() => workbenchAssetUrl(categoryVisualForm.patternImageKey));

const visualPositionCss = (value?: string) => {
  switch (value) {
    case 'right':
      return 'right center';
    case 'left':
      return 'left center';
    case 'bottom':
      return 'center bottom';
    default:
      return 'center center';
  }
};

const normalizePreviewPatternModeValue = (value?: string) => {
  const mode = String(value || '');
  return ['preset', 'icon', 'text', 'image', 'none'].includes(mode) ? mode : 'preset';
};

const normalizePreviewPatternPositionValue = (value?: string) => {
  const position = String(value || '');
  return ['center', 'right', 'left', 'bottom'].includes(position) ? position : 'center';
};

const categoryPreviewHoverEffectMode = computed(() => normalizeHoverEffectMode(stageConfigForm.hoverEffectMode, stageConfigForm.hoverScaleEnabled));

const categoryVisualPreviewClasses = computed(() => [
  `stage-notice-stat__pattern-mode--${normalizePreviewPatternModeValue(categoryVisualForm.patternMode)}`,
  `stage-notice-stat__pattern--${normalizePatternPresetValue(categoryVisualForm.patternPreset)}`,
  `stage-notice-stat__pattern-position--${normalizePreviewPatternPositionValue(categoryVisualForm.patternPosition)}`,
  {
    'is-hover-text-enabled': hoverTextEnabled(categoryPreviewHoverEffectMode.value),
    'is-hover-pattern-enabled': hoverPatternEnabled(categoryPreviewHoverEffectMode.value)
  }
]);

const categoryVisualPreviewStyle = computed(() => ({
  background: categoryVisualForm.backgroundColor || '#f8fbff',
  borderColor: categoryVisualForm.borderColor || '#dbeafe',
  '--stage-stat-hover-scale': String(normalizeHoverScale(stageConfigForm.hoverScale))
}));

const categoryVisualPreviewPatternStyle = computed(() => {
  const opacity = String(clampNumber(categoryVisualForm.patternOpacity, 0.07, 0, 0.75));
  const hoverScale = normalizeHoverScale(stageConfigForm.hoverScale);
  if (categoryVisualForm.patternMode === 'image') {
    return {
      opacity,
      backgroundImage: categoryVisualImageUrl.value ? `url("${categoryVisualImageUrl.value}")` : 'none',
      backgroundSize: categoryVisualForm.patternImageSize || 'cover',
      backgroundRepeat: categoryVisualForm.patternImageRepeat || 'no-repeat',
      backgroundPosition: visualPositionCss(categoryVisualForm.patternPosition),
      '--stage-stat-pattern-scale': '1',
      '--stage-stat-pattern-hover-scale': String(hoverScale)
    };
  }
  const patternScale = clampNumber(categoryVisualForm.patternScale, 0.8, 0.8, 3);
  return {
    opacity,
    color: categoryVisualForm.patternColor || categoryVisualForm.accentColor || '#2563eb',
    '--stage-stat-pattern-scale': String(patternScale),
    '--stage-stat-pattern-hover-scale': String(patternScale * hoverScale)
  };
});

const categoryVisualPreviewGlassStyle = computed(() => ({
  '--stage-stat-glass-color': categoryVisualForm.glassColor || 'rgba(255,255,255,1)',
  '--stage-stat-glass-opacity': String(clampNumber(categoryVisualForm.glassOpacity, 0.9, 0, 1)),
  '--stage-stat-glass-blur': `${clampNumber(categoryVisualForm.glassBlur, 28, 0, 28)}px`,
  '--stage-stat-texture-opacity':
    normalizePreviewPatternModeValue(categoryVisualForm.patternMode) === 'none'
      ? '0'
      : String(clampNumber(categoryVisualForm.textureOpacity, 0.21, 0, 0.65)),
  '--stage-stat-highlight-opacity': String(clampNumber(categoryVisualForm.highlightOpacity, 0.5, 0, 1))
}));

const categoryVisualPreviewTitleStyle = computed(() => ({
  color: categoryVisualForm.titleColor || '#102a43',
  fontSize: `${clampNumber(categoryVisualForm.fontSize, 15, 12, 22)}px`
}));

const categoryVisualPreviewNumberStyle = computed(() => ({
  color: categoryVisualForm.numberColor || '#2563eb'
}));

const stagePreviewCalendarInfo = computed(() => {
  const date = new Date();
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
  const timeParts = [String(date.getHours()).padStart(2, '0'), String(date.getMinutes()).padStart(2, '0')];
  if (stageConfigForm.calendarShowSeconds !== false) {
    timeParts.push(String(date.getSeconds()).padStart(2, '0'));
  }
  return {
    monthText: `${date.getFullYear()}年${date.getMonth() + 1}月`,
    day: String(date.getDate()).padStart(2, '0'),
    weekday: weekdays[date.getDay()],
    timeText: timeParts.join(':')
  };
});

const stageConfigPreviewVars = computed(
  () =>
    ({
      '--stage-preview-top-height': `${clampNumber(stageConfigForm.topHeight, 220, 160, 360)}px`,
      '--stage-preview-row-height': `${clampNumber(stageConfigForm.topHeight, 220, 160, 360)}px`,
      '--stage-preview-gap': `${clampNumber(stageConfigForm.topGap, 18, 8, 40)}px`,
      '--stage-preview-notice-ratio': `${clampNumber(stageConfigForm.noticeRatio, 0.9, 0.5, 2)}fr`,
      '--stage-preview-activity-ratio': `${clampNumber(stageConfigForm.activityRatio, 1.4, 0.5, 2.5)}fr`,
      '--stage-preview-calendar-width': `${clampNumber(stageConfigForm.calendarWidth, 190, 120, 320)}px`,
      '--stage-preview-calendar-height': `${clampNumber(stageConfigForm.topHeight, 220, 160, 360)}px`,
      '--stage-preview-calendar-radius': `${clampNumber(stageConfigForm.calendarRadius, 8, 0, 24)}px`,
      '--stage-preview-calendar-bg': stageConfigForm.calendarBackground || defaultNoticeLayoutConfig.calendarBackground,
      '--stage-preview-calendar-color': stageConfigForm.calendarTextColor || '#ffffff',
      '--stage-preview-month-size': `${clampNumber(stageConfigForm.calendarMonthFontSize, 14, 10, 24)}px`,
      '--stage-preview-day-size': `${clampNumber(stageConfigForm.calendarDayFontSize, 54, 28, 84)}px`,
      '--stage-preview-weekday-size': `${clampNumber(stageConfigForm.calendarWeekdayFontSize, 17, 12, 28)}px`,
      '--stage-preview-time-size': `${clampNumber(stageConfigForm.calendarTimeFontSize, 12, 10, 22)}px`,
      '--stage-preview-time-bg': stageConfigForm.calendarTimeBackground || 'rgba(255,255,255,0.18)'
    }) as Record<string, string>
);

const stagePreviewQuotaStyle = computed(
  () =>
    ({
      fontSize: `${clampNumber(stageConfigForm.quotaSummaryFontSize, 13, 11, 24)}px`,
      color: stageConfigForm.quotaSummaryTextColor || '#214567',
      background: stageConfigForm.quotaSummaryBackground || '#f4f9ff',
      borderColor: stageConfigForm.quotaSummaryBorderColor || '#bfdbfe',
      borderRadius: `${clampNumber(stageConfigForm.quotaSummaryRadius, 8, 0, 24)}px`,
      gap: `${clampNumber(stageConfigForm.quotaSummaryGap, 8, 0, 24)}px`,
      '--stage-preview-quota-number-color': stageConfigForm.quotaSummaryNumberColor || '#1d4ed8'
    }) as Record<string, string>
);

const stagePreviewRatioStyle = computed(
  () =>
    ({
      fontSize: `${clampNumber(stageConfigForm.quotaRatioFontSize, 12, 10, 20)}px`,
      color: stageConfigForm.quotaRatioTextColor || '#40566f',
      background: stageConfigForm.quotaRatioBackground || '#ffffff',
      borderColor: stageConfigForm.quotaRatioBorderColor || '#d8e6f5',
      borderRadius: `${clampNumber(stageConfigForm.quotaRatioRadius, 8, 0, 24)}px`,
      '--stage-preview-ratio-number-color': stageConfigForm.quotaRatioNumberColor || '#2563eb'
    }) as Record<string, string>
);

const confirmDiscardCurrentScope = async (action: string, dirty = currentScopeDirty.value) => {
  if (!dirty) return true;
  try {
    await ElMessageBox.confirm(`“${currentSectionLabel.value}”存在未保存修改，${action}将放弃当前草稿。是否继续？`, '未保存修改', {
      type: 'warning',
      confirmButtonText: `放弃并${action}`,
      cancelButtonText: '继续编辑'
    });
    return true;
  } catch {
    return false;
  }
};

const switchWorkbenchSection = async (nextSection: WorkbenchSectionKey) => {
  if (nextSection === activeWorkbenchTab.value) return;
  if (!(await confirmDiscardCurrentScope('切换', sectionDirtyMap.value[activeWorkbenchTab.value]))) return;
  activeWorkbenchTab.value = nextSection;
  if (nextSection === 'style' && !styleOriginalSnapshot.value) {
    await loadWorkbenchStyle();
  } else if (nextSection === 'shell' && selectedRoleId.value && roleShellLoadedRoleId.value !== String(selectedRoleId.value)) {
    await loadRoleShell();
  }
};

const switchWorkbenchSectionGuarded = async (section: (typeof workbenchSections)[number]) => {
  if (!hasSectionPermission(section)) {
    proxy?.$modal.msgWarning(CONFIG_PERMISSION_HINT);
    return;
  }
  await switchWorkbenchSection(section.key);
};

const switchBusinessPage = async (nextPage: ArtListTablePageKey) => {
  if (nextPage === businessPageKey.value) return;
  if (!(await confirmDiscardCurrentScope('切换页面', sectionDirtyMap.value.business))) return;
  globalTableLayoutEditorRef.value?.cancelChanges?.();
  await pageHeaderEditorRef.value?.cancelChanges?.(false);
  businessPageKey.value = nextPage;
  if (!isWorkspaceHeaderPageKey(nextPage)) businessConfigTab.value = 'table';
};

const saveCurrentScope = async () => {
  if (activeWorkbenchTab.value === 'layout') {
    await saveLayout();
    return;
  }
  if (activeWorkbenchTab.value === 'shell') {
    await saveSelectedRoleShell();
    return;
  }
  if (activeWorkbenchTab.value === 'style') {
    if (publicStyleSection.value === 'workbench') {
      await saveWorkbenchStyle();
    } else if (publicStyleSection.value === 'tableAppearance') {
      await globalTableAppearanceEditorRef.value?.saveAll?.();
    } else {
      await reviewDisplayEditorRef.value?.saveAll?.();
    }
    return;
  }
  if (activeWorkbenchTab.value === 'business') {
    await activeBusinessConfigEditor.value?.saveAll?.();
  }
};

const restoreCurrentScope = async () => {
  if (activeWorkbenchTab.value === 'layout') {
    await restoreRoleDefault();
  } else if (activeWorkbenchTab.value === 'shell') {
    await restoreSelectedRoleShell();
  } else if (activeWorkbenchTab.value === 'business') {
    await activeBusinessConfigEditor.value?.restoreDefaults?.();
  } else if (activeWorkbenchTab.value === 'style' && publicStyleSection.value === 'tableAppearance') {
    await globalTableAppearanceEditorRef.value?.restoreDefaults?.();
  } else if (activeWorkbenchTab.value === 'style' && publicStyleSection.value === 'reviewDisplay') {
    await reviewDisplayEditorRef.value?.restoreDefaults?.();
  }
};

const cancelCurrentScope = async () => {
  if (!(await confirmDiscardCurrentScope('取消本次修改'))) return;
  if (activeWorkbenchTab.value === 'layout') {
    await loadRoleLayout();
  } else if (activeWorkbenchTab.value === 'shell') {
    await loadRoleShell();
  } else if (activeWorkbenchTab.value === 'business') {
    await activeBusinessConfigEditor.value?.cancelChanges?.(false);
  } else if (activeWorkbenchTab.value === 'style' && publicStyleSection.value === 'workbench') {
    await loadWorkbenchStyle();
  } else if (activeWorkbenchTab.value === 'style' && publicStyleSection.value === 'tableAppearance') {
    globalTableAppearanceEditorRef.value?.cancelChanges?.();
  } else if (activeWorkbenchTab.value === 'style' && publicStyleSection.value === 'reviewDisplay') {
    await reviewDisplayEditorRef.value?.cancelChanges?.(false);
  }
};

const loadRoles = async () => {
  const res = await listWorkbenchRoleOptions();
  roleOptions.value = res.data || [];
  if (!selectedRoleId.value && roleOptions.value.length) {
    selectedRoleId.value = roleOptions.value[0].roleId;
    await loadRoleLayout();
  }
};

const handleRoleSelection = async (roleId: string | number) => {
  if (String(roleId) === String(selectedRoleId.value)) return;
  if (!(await confirmDiscardCurrentScope('切换角色'))) return;
  selectedRoleId.value = roleId;
  roleShellOriginalSnapshot.value = '';
  roleShellLoadedRoleId.value = undefined;
  if (activeWorkbenchTab.value === 'shell') {
    await loadRoleShell();
  } else {
    await loadRoleLayout();
  }
};

const assignWorkbenchStyle = (config: any) => {
  Object.assign(styleForm, cloneWorkbenchStyle(normalizeWorkbenchStyleConfig(config)));
};

const fetchWorkbenchStyle = async (forceFresh = false) => {
  const res = await getWorkbenchStyleConfig(forceFresh);
  return {
    config: parseWorkbenchStyleConfig(res.data),
    valid: isWorkbenchStyleConfigPayload(res.data)
  };
};

const preserveWorkbenchStyleDraft = (config: any) => {
  assignWorkbenchStyle(config);
  applyWorkbenchStyleVars(config);
  appStore.configureLayoutDisplayScale(config.layoutScaleMode, config.customScalePercent);
};

const applyLoadedWorkbenchStyle = (config: any) => {
  assignWorkbenchStyle(config);
  applyWorkbenchStyleVars(config);
  appStore.configureLayoutDisplayScale(config.layoutScaleMode, config.customScalePercent);
  styleOriginalSnapshot.value = styleSnapshot();
};

const loadWorkbenchStyle = async () => {
  styleLoading.value = true;
  try {
    const { config } = await fetchWorkbenchStyle();
    applyLoadedWorkbenchStyle(config);
  } finally {
    styleLoading.value = false;
  }
};

const applyStylePreset = (preset: string) => {
  const match = workbenchStylePresets.find((item) => item.value.preset === preset);
  if (match) {
    const layoutScaleMode = styleForm.layoutScaleMode;
    const customScalePercent = styleForm.customScalePercent;
    assignWorkbenchStyle(match.value);
    styleForm.layoutScaleMode = layoutScaleMode;
    styleForm.customScalePercent = customScalePercent;
  } else {
    styleForm.preset = 'custom';
  }
};

const saveWorkbenchStyle = async () => {
  styleSaving.value = true;
  try {
    const payload = compactWorkbenchStyleConfig(styleForm);
    const payloadLength = JSON.stringify(payload).length;
    if (payloadLength > WORKBENCH_STYLE_CONFIG_MAX_LENGTH) {
      proxy?.$modal.msgError(`样式配置过长，请控制在 ${WORKBENCH_STYLE_CONFIG_MAX_LENGTH} 字符以内`);
      return;
    }
    const saveResponse = await saveWorkbenchStyleConfig(payload);
    const saveResult = saveResponse.data;
    const savedConfigValid = Number(saveResult?.apiVersion) === WORKBENCH_STYLE_SAVE_API_VERSION && isWorkbenchStyleConfigPayload(saveResult?.config);
    if (!savedConfigValid) {
      preserveWorkbenchStyleDraft(payload);
      proxy?.$modal.msgError('工作台样式保存接口版本不匹配，已保留当前草稿，请同步部署当前后端后重试');
      return;
    }
    const savedConfig = parseWorkbenchStyleConfig(saveResult.config);
    if (!workbenchStyleConfigsEqual(payload, savedConfig)) {
      preserveWorkbenchStyleDraft(payload);
      proxy?.$modal.msgError('后端返回的已保存工作台样式与本次提交内容不一致，已保留当前草稿，请检查配置存储');
      return;
    }
    const persisted = await fetchWorkbenchStyle(true);
    if (!persisted.valid || !workbenchStyleConfigsEqual(payload, persisted.config)) {
      preserveWorkbenchStyleDraft(payload);
      proxy?.$modal.msgError('工作台样式保存节点与读取节点返回不一致，已保留当前草稿，请检查服务节点版本或缓存');
      return;
    }
    applyLoadedWorkbenchStyle(persisted.config);
    proxy?.$modal.msgSuccess('工作台样式已保存');
  } finally {
    styleSaving.value = false;
  }
};

const loadComponents = async () => {
  if (!selectedRoleId.value) return;
  componentLoading.value = true;
  try {
    const res = await listWorkbenchComponents(selectedRoleId.value);
    componentOptions.value = res.data || [];
  } finally {
    componentLoading.value = false;
  }
};

const loadRoleLayout = async () => {
  if (!selectedRoleId.value) return;
  loading.value = true;
  try {
    await loadComponents();
    const res = await listRoleWorkbench(selectedRoleId.value);
    layoutRows.value = (res.data || []).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0));
    selectedLayoutIndex.value = Math.min(selectedLayoutIndex.value, Math.max(layoutRows.value.length - 1, 0));
    layoutOriginalSnapshot.value = layoutSnapshot();
  } finally {
    loading.value = false;
  }
};

const refreshActiveWorkbenchConfig = async () => {
  if (!(await confirmDiscardCurrentScope('刷新'))) return;
  if (activeWorkbenchTab.value === 'style') {
    if (publicStyleSection.value === 'workbench') {
      await loadWorkbenchStyle();
    } else if (publicStyleSection.value === 'tableAppearance') {
      await globalTableAppearanceEditorRef.value?.initialize?.();
    } else {
      await reviewDisplayEditorRef.value?.initialize?.();
    }
    return;
  }
  if (activeWorkbenchTab.value === 'business') {
    await activeBusinessConfigEditor.value?.initialize?.();
    return;
  }
  if (activeWorkbenchTab.value === 'migration') {
    proxy?.$modal.msg('迁移工具没有需要刷新的运行态数据');
    return;
  }
  if (activeWorkbenchTab.value === 'shell') {
    await loadRoleShell();
  } else {
    await loadRoleLayout();
  }
};

const exportConfigPackage = async () => {
  configPackageExporting.value = true;
  try {
    const response = await exportWorkbenchConfigPackage();
    const data = response instanceof Blob ? response : response.data;
    const blob = data instanceof Blob ? data : new Blob([data], { type: 'application/zip' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    const date = new Date().toISOString().slice(0, 10);
    link.href = url;
    link.download = `workbench-config-${date}.zip`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
    proxy?.$modal.msgSuccess('工作台配置包已导出');
  } finally {
    configPackageExporting.value = false;
  }
};

const selectConfigPackage = () => {
  configPackageInput.value?.click();
};

const previewConfigPackage = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (!file) return;
  if (!file.name.toLowerCase().endsWith('.zip')) {
    proxy?.$modal.msgWarning('请选择 ZIP 工作台配置包');
    return;
  }
  configPackageImporting.value = true;
  try {
    const { data } = await previewWorkbenchConfigPackage(file);
    configImportFile.value = file;
    configImportPreview.value = data;
    configImportDialog.visible = true;
  } finally {
    configPackageImporting.value = false;
  }
};

const clearConfigImportPreview = () => {
  configImportFile.value = undefined;
  configImportPreview.value = undefined;
};

const confirmConfigPackageImport = async () => {
  if (!configImportFile.value || !configImportPreview.value) return;
  configPackageImporting.value = true;
  try {
    await importWorkbenchConfigPackage(configImportFile.value);
    configImportDialog.visible = false;
    await Promise.all([loadWorkbenchStyle(), loadRoleLayout()]);
    proxy?.$modal.msgSuccess('工作台配置已导入，相关角色刷新后生效');
  } finally {
    configPackageImporting.value = false;
  }
};

const configDiffScopeLabel = (scope: string) =>
  ({ style: '全局样式', shell: '系统布局', layout: '首页布局', role: '角色匹配', asset: '图片资源' })[scope] || scope;

const configDiffTypeLabel = (type: WorkbenchConfigChangeType) =>
  ({ add: '新增', modify: '修改', remove: '删除', unchanged: '无变化', skipped: '跳过' })[type];

const configDiffTagType = (type: WorkbenchConfigChangeType): 'success' | 'warning' | 'danger' | 'info' | 'primary' =>
  ({ add: 'success', modify: 'warning', remove: 'danger', unchanged: 'info', skipped: 'danger' })[type] as 'success' | 'warning' | 'danger' | 'info';

const hasLayout = (componentKey: string) => layoutRows.value.some((item) => item.componentKey === componentKey);
const widthLabel = workbenchWidthLabel;
const widthClass = (width?: string) => String(width || '1/1').replace('/', '-');
const componentTypeLabel = (type?: string) =>
  ({ card: '卡片', chart: '图表', list: '列表', notice: '提醒', dashboard: '看板' })[String(type || '').toLowerCase()] || type || '首页组件';
const componentPreviewBlocks = workbenchComponentPreviewBlocks;
const isConfigurableLayoutRow = (row?: WorkbenchLayoutVO) => Boolean(row && workbenchComponentConfigKind(row.componentKey));
const openSelectedLayoutConfig = (row: WorkbenchLayoutVO) => {
  const configKind = workbenchComponentConfigKind(row.componentKey);
  if (configKind === 'projectSubmit') {
    openProjectSubmitConfig(row);
  } else if (configKind === 'stageNotice') {
    openStageNoticeConfig(row);
  } else if (configKind === 'auditWorkbench') {
    openAuditWorkbenchConfig(row);
  } else if (configKind === 'reviewWorkbench') {
    openReviewWorkbenchConfig(row);
  } else if (configKind === 'dashboard') {
    openUniversalDashboardConfig(row);
  }
};

const defaultComponentConfigJson = (componentKey: string) => {
  const configKind = workbenchComponentConfigKind(componentKey);
  if (configKind === 'projectSubmit') return JSON.stringify(defaultProjectSubmitConfig);
  if (configKind === 'stageNotice') return JSON.stringify(defaultNoticeConfig(componentKey));
  if (configKind === 'auditWorkbench') return JSON.stringify(defaultAuditWorkbenchConfig);
  if (configKind === 'reviewWorkbench') return JSON.stringify(defaultReviewWorkbenchConfig);
  if (configKind === 'dashboard') {
    const dashboardRole = componentKey === SCHOOL_DASHBOARD_COMPONENT_KEY ? 'school' : resolveDashboardRole(selectedRoleKey.value);
    return JSON.stringify(defaultUniversalDashboardConfig(dashboardRole));
  }
  return undefined;
};

const addComponent = (component: WorkbenchComponentVO) => {
  if (component.permission?.length && !checkPermi(component.permission)) {
    proxy?.$modal.msgWarning(CONFIG_PERMISSION_HINT);
    return;
  }
  layoutRows.value.push({
    roleId: selectedRoleId.value,
    componentKey: component.componentKey,
    componentName: component.componentName,
    componentType: component.componentType,
    title: component.defaultTitle,
    width: component.defaultWidth,
    sortOrder: layoutRows.value.length + 1,
    visible: '0',
    permission: component.permission,
    configJson: defaultComponentConfigJson(component.componentKey)
  });
  selectedLayoutIndex.value = layoutRows.value.length - 1;
};

const removeComponent = (index: number) => {
  layoutRows.value.splice(index, 1);
  selectedLayoutIndex.value = Math.min(selectedLayoutIndex.value, Math.max(layoutRows.value.length - 1, 0));
};

const removeSelectedComponent = () => {
  if (!selectedLayoutRow.value) return;
  removeComponent(selectedLayoutIndex.value);
};

const moveRow = (targetIndex: number) => {
  if (dragIndex.value === undefined || dragIndex.value === targetIndex) return;
  const selectedRow = selectedLayoutRow.value;
  const [row] = layoutRows.value.splice(dragIndex.value, 1);
  layoutRows.value.splice(targetIndex, 0, row);
  selectedLayoutIndex.value = Math.max(
    0,
    layoutRows.value.findIndex((item) => item === selectedRow)
  );
  dragIndex.value = undefined;
};

const restoreRoleDefault = async () => {
  if (!selectedRoleId.value) return;
  try {
    const roleName = roleOptions.value.find((item) => String(item.roleId) === String(selectedRoleId.value))?.roleName || '当前角色';
    await ElMessageBox.confirm(`将清除“${roleName}”的全部首页自定义布局，并恢复系统角色默认。是否继续？`, '恢复角色默认', {
      type: 'warning',
      confirmButtonText: '恢复默认',
      cancelButtonText: '取消'
    });
    resettingRoleLayout.value = true;
    await saveRoleWorkbench(selectedRoleId.value, []);
    await loadRoleLayout();
    proxy?.$modal.msgSuccess('角色工作台已恢复系统默认');
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      proxy?.$modal.msgError(error?.msg || error?.message || '恢复角色默认失败');
    }
  } finally {
    resettingRoleLayout.value = false;
  }
};

function cloneProjectSubmitConfig(config: ProjectSubmitConfig): ProjectSubmitConfig {
  return JSON.parse(JSON.stringify(config));
}

function cloneStageNoticeConfig(config: StageNoticeConfig): StageNoticeConfig {
  return JSON.parse(JSON.stringify(config));
}

function cloneAuditWorkbenchConfig(config: AuditWorkbenchConfig): AuditWorkbenchConfig {
  return JSON.parse(JSON.stringify(config));
}

function cloneReviewWorkbenchConfig(config: ReviewWorkbenchConfig): ReviewWorkbenchConfig {
  return JSON.parse(JSON.stringify(config));
}

const mergeItems = (base: WorkbenchConfigItem[], custom?: WorkbenchConfigItem[]) => {
  const customMap = new Map((custom || []).map((item) => [item.key, item]));
  const merged = base.map((item) => {
    const customItem = customMap.get(item.key);
    if (!customItem) return { ...item };
    const secondary = item.secondary || customItem.secondary ? { ...(item.secondary || {}), ...(customItem.secondary || {}) } : undefined;
    return { ...item, ...customItem, ...(secondary ? { secondary } : {}) };
  });
  (custom || []).forEach((item) => {
    if (!merged.some((baseItem) => baseItem.key === item.key)) {
      merged.push(item);
    }
  });
  return merged;
};

const normalizeSecondaryConfig = (value?: WorkbenchSecondaryConfig): WorkbenchSecondaryConfig => ({
  visible: value?.visible === true,
  label: value?.label || '',
  valueKey: value?.valueKey || '',
  fontSize: clampNumber(value?.fontSize, 13, 10, 20),
  textColor: value?.textColor || '#5c7087',
  numberColor: value?.numberColor || '#2563eb'
});

const visualModeValues = ['preset', 'icon', 'text', 'image', 'none'];
const visualPatternValues = visualPatternOptions.map((item) => item.value);
const visualPositionValues = visualPositionOptions.map((item) => item.value);
const visualImageSizeValues = visualImageSizeOptions.map((item) => item.value);
const visualImageRepeatValues = visualImageRepeatOptions.map((item) => item.value);

const legacyPatternPresetMap: Record<string, string> = {
  performance: 'stageBeam',
  artwork: 'inkPaper',
  workshop: 'geoGrid',
  achievement: 'medalRelief',
  calligraphy: 'calligraphyMark',
  school: 'geoGrid',
  review: 'softFlow',
  score: 'stageBeam',
  ribbon: 'softFlow'
};
const legacyPatternIconMap: Record<string, string> = {
  performance: 'guide',
  artwork: 'example',
  workshop: 'build',
  achievement: 'star',
  calligraphy: 'edit',
  school: 'education',
  review: 'dict',
  score: 'chart',
  ribbon: 'message'
};

const normalizePatternPresetValue = (value?: string) => {
  const raw = String(value || '');
  const mapped = legacyPatternPresetMap[raw] || raw;
  return visualPatternValues.includes(mapped) ? mapped : 'softFlow';
};

const normalizePatternIconValue = (value?: string) => {
  const raw = String(value || '').trim();
  return legacyPatternIconMap[raw] || raw || 'date';
};

const normalizePatternTextValue = (value?: string) => {
  const text = Array.from(String(value || '').trim())
    .slice(0, 8)
    .join('');
  return text || '艺';
};

const normalizePatternColorValue = (item: Pick<WorkbenchConfigItem, 'patternColor' | 'accentColor'>, fallback: { patternColor: string }) => {
  const color = String(item.patternColor || '').trim();
  return color || item.accentColor || fallback.patternColor;
};

const defaultVisualConfig = (item: Pick<WorkbenchConfigItem, 'key' | 'label' | 'accentColor' | 'backgroundColor'>) => {
  return {
    patternMode: 'icon',
    patternPreset: 'softFlow',
    patternIcon: 'chart',
    patternText: '艺',
    patternColor: item.accentColor || '#2563eb',
    patternOpacity: 0.07,
    patternScale: 0.8,
    patternPosition: 'center',
    patternImageKey: '',
    patternImageName: '',
    patternImageSize: 'cover',
    patternImageRepeat: 'no-repeat',
    glassColor: 'rgba(255,255,255,1)',
    glassOpacity: 0.9,
    glassBlur: 28,
    textureOpacity: 0.21,
    highlightOpacity: 0.5
  };
};

const nearlyEqualNumber = (value: unknown, expected: number) => Math.abs(Number(value) - expected) < 0.001;

const isPreviousFlatGlassDefault = (
  item: Pick<
    WorkbenchConfigItem,
    | 'patternMode'
    | 'patternPreset'
    | 'patternOpacity'
    | 'patternScale'
    | 'patternImageKey'
    | 'glassColor'
    | 'glassOpacity'
    | 'glassBlur'
    | 'textureOpacity'
    | 'highlightOpacity'
  >
) => {
  return (
    String(item.patternMode || 'preset') === 'preset' &&
    String(item.patternPreset || '') === 'pureGlass' &&
    !item.patternImageKey &&
    nearlyEqualNumber(item.patternOpacity, 0.08) &&
    nearlyEqualNumber(item.patternScale, 1.25) &&
    String(item.glassColor || '').replace(/\s/g, '') === 'rgba(255,255,255,0.62)' &&
    nearlyEqualNumber(item.glassOpacity, 0.78) &&
    nearlyEqualNumber(item.glassBlur, 16) &&
    nearlyEqualNumber(item.textureOpacity, 0.06) &&
    nearlyEqualNumber(item.highlightOpacity, 0.38)
  );
};

const isPreviousLayeredGlassDefault = (
  item: Pick<
    WorkbenchConfigItem,
    | 'patternMode'
    | 'patternPreset'
    | 'patternOpacity'
    | 'patternScale'
    | 'patternImageKey'
    | 'glassColor'
    | 'glassOpacity'
    | 'glassBlur'
    | 'textureOpacity'
    | 'highlightOpacity'
  >
) => {
  return (
    String(item.patternMode || 'preset') === 'preset' &&
    String(item.patternPreset || '') === 'softFlow' &&
    !item.patternImageKey &&
    nearlyEqualNumber(item.patternOpacity, 0.18) &&
    nearlyEqualNumber(item.patternScale, 1.45) &&
    String(item.glassColor || '').replace(/\s/g, '') === 'rgba(255,255,255,0.46)' &&
    nearlyEqualNumber(item.glassOpacity, 0.58) &&
    nearlyEqualNumber(item.glassBlur, 18) &&
    nearlyEqualNumber(item.textureOpacity, 0.12) &&
    nearlyEqualNumber(item.highlightOpacity, 0.5)
  );
};

const isLegacyDefaultVisualConfig = (
  item: Pick<
    WorkbenchConfigItem,
    | 'patternMode'
    | 'patternPreset'
    | 'patternOpacity'
    | 'patternScale'
    | 'patternImageKey'
    | 'glassColor'
    | 'glassOpacity'
    | 'glassBlur'
    | 'textureOpacity'
    | 'highlightOpacity'
  >
) => isPreviousFlatGlassDefault(item) || isPreviousLayeredGlassDefault(item);

const normalizeVisualConfig = (
  item: WorkbenchConfigItem
): Pick<
  WorkbenchConfigItem,
  | 'patternMode'
  | 'patternPreset'
  | 'patternIcon'
  | 'patternText'
  | 'patternColor'
  | 'patternOpacity'
  | 'patternScale'
  | 'patternPosition'
  | 'patternImageKey'
  | 'patternImageName'
  | 'patternImageSize'
  | 'patternImageRepeat'
  | 'glassColor'
  | 'glassOpacity'
  | 'glassBlur'
  | 'textureOpacity'
  | 'highlightOpacity'
> => {
  const fallback = defaultVisualConfig(item);
  const visualSource = isLegacyDefaultVisualConfig(item) ? { ...item, ...fallback } : item;
  const patternMode = visualModeValues.includes(String(visualSource.patternMode || ''))
    ? String(visualSource.patternMode)
    : visualSource.patternImageKey
      ? 'image'
      : fallback.patternMode;
  const patternPreset = normalizePatternPresetValue(visualSource.patternPreset || fallback.patternPreset);
  const patternIcon = normalizePatternIconValue(visualSource.patternIcon || fallback.patternIcon);
  const patternText = normalizePatternTextValue(visualSource.patternText || fallback.patternText);
  const patternPosition = visualPositionValues.includes(String(visualSource.patternPosition || ''))
    ? String(visualSource.patternPosition)
    : fallback.patternPosition;
  const patternImageSize = visualImageSizeValues.includes(String(visualSource.patternImageSize || ''))
    ? String(visualSource.patternImageSize)
    : fallback.patternImageSize;
  const patternImageRepeat = visualImageRepeatValues.includes(String(visualSource.patternImageRepeat || ''))
    ? String(visualSource.patternImageRepeat)
    : fallback.patternImageRepeat;
  return {
    patternMode,
    patternPreset,
    patternIcon,
    patternText,
    patternColor: normalizePatternColorValue(visualSource, fallback),
    patternOpacity: clampNumber(visualSource.patternOpacity, fallback.patternOpacity, 0, 0.75),
    patternScale: clampNumber(visualSource.patternScale, fallback.patternScale, 0.8, 3),
    patternPosition,
    patternImageKey: visualSource.patternImageKey || '',
    patternImageName: visualSource.patternImageName || '',
    patternImageSize,
    patternImageRepeat,
    glassColor: visualSource.glassColor || fallback.glassColor,
    glassOpacity: clampNumber(visualSource.glassOpacity, fallback.glassOpacity, 0, 1),
    glassBlur: clampNumber(visualSource.glassBlur, fallback.glassBlur, 0, 28),
    textureOpacity: clampNumber(visualSource.textureOpacity, fallback.textureOpacity, 0, 0.65),
    highlightOpacity: clampNumber(visualSource.highlightOpacity, fallback.highlightOpacity, 0, 1)
  };
};

const defaultNoticeConfig = (componentKey?: string): StageNoticeConfig => {
  if (componentKey === ADMIN_NOTICE_COMPONENT_KEY) return defaultAdminNoticeConfig;
  if (componentKey === AUDIT_NOTICE_COMPONENT_KEY) return defaultAuditNoticeConfig;
  if (componentKey === REVIEW_NOTICE_COMPONENT_KEY) return defaultReviewNoticeConfig;
  return defaultStageNoticeConfig;
};

const normalizeQuotaSummaryFields = (value: unknown, fallback: string[]) => {
  const allowed = new Set([
    'schoolType',
    'schoolName',
    'totalCount',
    'usedCount',
    'quotaLimit',
    'remainingCount',
    'draftCount',
    'submittedCount',
    'completedCount'
  ]);
  const source = Array.isArray(value) ? value : fallback;
  const fields = source.map((item) => String(item || '')).filter((item) => allowed.has(item));
  return fields.length ? fields : fallback;
};

const normalizeSchoolTypeLabelReplacements = (value: unknown, fallback: SchoolTypeLabelReplacement[] = []) => {
  const source = Array.isArray(value) ? value : fallback;
  return source
    .map((item) => ({
      source: String(item?.source || '').trim(),
      target: String(item?.target || '').trim()
    }))
    .filter((item) => item.source && item.target);
};

const normalizeNoticeLayout = (custom: Partial<StageNoticeConfig>, defaults: StageNoticeConfig) => ({
  topHeight: clampNumber(custom.topHeight, defaults.topHeight, 160, 360),
  topGap: clampNumber(custom.topGap, defaults.topGap, 8, 40),
  noticeRatio: clampNumber(custom.noticeRatio, defaults.noticeRatio, 0.5, 2),
  activityRatio: clampNumber(custom.activityRatio, defaults.activityRatio, 0.5, 2.5),
  calendarWidth: clampNumber(custom.calendarWidth, defaults.calendarWidth, 120, 320),
  calendarHeight: clampNumber(custom.calendarHeight, defaults.calendarHeight, 140, 360),
  calendarRadius: clampNumber(custom.calendarRadius, defaults.calendarRadius, 0, 24),
  calendarBackground: custom.calendarBackground || defaults.calendarBackground,
  calendarTextColor: custom.calendarTextColor || defaults.calendarTextColor,
  calendarMonthFontSize: clampNumber(custom.calendarMonthFontSize, defaults.calendarMonthFontSize, 10, 24),
  calendarDayFontSize: clampNumber(custom.calendarDayFontSize, defaults.calendarDayFontSize, 28, 84),
  calendarWeekdayFontSize: clampNumber(custom.calendarWeekdayFontSize, defaults.calendarWeekdayFontSize, 12, 28),
  calendarTimeFontSize: clampNumber(custom.calendarTimeFontSize, defaults.calendarTimeFontSize, 10, 22),
  calendarTimeBackground: custom.calendarTimeBackground || defaults.calendarTimeBackground,
  calendarShowTime: custom.calendarShowTime !== false,
  calendarShowSeconds: custom.calendarShowSeconds !== false,
  quotaSummaryEnabled: custom.quotaSummaryEnabled !== false,
  quotaSummaryFields: normalizeQuotaSummaryFields(custom.quotaSummaryFields, defaults.quotaSummaryFields),
  quotaSummaryTemplate: custom.quotaSummaryTemplate || '',
  quotaSummaryFontSize: clampNumber(custom.quotaSummaryFontSize, defaults.quotaSummaryFontSize, 11, 24),
  quotaSummaryTextColor: custom.quotaSummaryTextColor || defaults.quotaSummaryTextColor,
  quotaSummaryNumberColor: custom.quotaSummaryNumberColor || defaults.quotaSummaryNumberColor,
  quotaSummaryBackground: custom.quotaSummaryBackground || defaults.quotaSummaryBackground,
  quotaSummaryBorderColor: custom.quotaSummaryBorderColor || defaults.quotaSummaryBorderColor,
  quotaSummaryRadius: clampNumber(custom.quotaSummaryRadius, defaults.quotaSummaryRadius, 0, 24),
  quotaSummaryGap: clampNumber(custom.quotaSummaryGap, defaults.quotaSummaryGap, 0, 24),
  quotaSummaryCss: custom.quotaSummaryCss || '',
  quotaRatioEnabled: custom.quotaRatioEnabled === true,
  quotaRatioMaxItems: clampNumber(custom.quotaRatioMaxItems, defaults.quotaRatioMaxItems, 1, 12),
  quotaRatioFontSize: clampNumber(custom.quotaRatioFontSize, defaults.quotaRatioFontSize, 10, 20),
  quotaRatioTextColor: custom.quotaRatioTextColor || defaults.quotaRatioTextColor,
  quotaRatioNumberColor: custom.quotaRatioNumberColor || defaults.quotaRatioNumberColor,
  quotaRatioBackground: custom.quotaRatioBackground || defaults.quotaRatioBackground,
  quotaRatioBorderColor: custom.quotaRatioBorderColor || defaults.quotaRatioBorderColor,
  quotaRatioRadius: clampNumber(custom.quotaRatioRadius, defaults.quotaRatioRadius, 0, 24),
  quotaRatioCss: custom.quotaRatioCss || '',
  cardRatioEnabled: custom.cardRatioEnabled === true,
  schoolTypeLabelReplacements: normalizeSchoolTypeLabelReplacements(custom.schoolTypeLabelReplacements, defaults.schoolTypeLabelReplacements)
});

const parseStageNoticeConfig = (value?: string, componentKey?: string): StageNoticeConfig => {
  const defaults = defaultNoticeConfig(componentKey);
  let custom: Partial<StageNoticeConfig> = {};
  if (value) {
    try {
      custom = JSON.parse(value);
    } catch {
      custom = {};
    }
  }
  const hoverEffectMode = normalizeHoverEffectMode(custom.hoverEffectMode, custom.hoverScaleEnabled ?? defaults.hoverScaleEnabled);
  const activityCategoryMode = componentKey === STAGE_NOTICE_COMPONENT_KEY && custom.categoryConfigMode === 'activity';
  const categorySource = activityCategoryMode ? custom.categories || [] : mergeItems(defaults.categories, custom.categories);
  return {
    categoryConfigMode: activityCategoryMode ? 'activity' : 'legacy',
    ...normalizeNoticeLayout(custom, defaults),
    categoryColumns: Number(custom.categoryColumns || defaults.categoryColumns),
    categoryGap: Number(custom.categoryGap ?? defaults.categoryGap),
    categoryRadius: Number(custom.categoryRadius ?? defaults.categoryRadius),
    hoverEffectMode,
    hoverScaleEnabled: hoverEffectMode !== 'none',
    hoverScale: normalizeHoverScale(custom.hoverScale ?? defaults.hoverScale),
    categoryAreaBackground: custom.categoryAreaBackground || defaults.categoryAreaBackground,
    componentCss: custom.componentCss || '',
    dateCss: custom.dateCss || '',
    categories: categorySource.map((item) => {
      const secondary = normalizeSecondaryConfig(item.secondary);
      const visual = normalizeVisualConfig(item);
      return {
        ...item,
        ...visual,
        aliasesText: (item.aliases || []).join(','),
        secondary,
        secondaryVisible: secondary.visible === true,
        secondaryLabel: secondary.label,
        secondaryValueKey: secondary.valueKey,
        secondaryFontSize: secondary.fontSize,
        secondaryTextColor: secondary.textColor,
        secondaryNumberColor: secondary.numberColor,
        quotaDisplayMode: normalizeQuotaDisplayMode(item.quotaDisplayMode, item.showQuota),
        quotaFields: normalizeQuotaFields(item.quotaFields),
        quotaPosition: normalizeQuotaPosition(item.quotaPosition)
      };
    })
  };
};

const normalizeProjectSubmitProgress = (
  value: Partial<ArtAuditProgressConfig> | undefined,
  fallback = defaultSchoolSubmitProgress()
): ArtAuditProgressConfig => ({
  visible: typeof value?.visible === 'boolean' ? value.visible : fallback.visible,
  title: String(value?.title || fallback.title)
    .trim()
    .slice(0, 20),
  template: String(value?.template || fallback.template)
    .trim()
    .slice(0, 80),
  height: clampNumber(value?.height, fallback.height, 4, 20),
  width: clampNumber(value?.width, fallback.width, 120, 480),
  activeColor: value?.activeColor || fallback.activeColor,
  completeColor: value?.completeColor || fallback.completeColor,
  trackColor: value?.trackColor || fallback.trackColor,
  textColor: value?.textColor || fallback.textColor,
  fontSize: clampNumber(value?.fontSize, fallback.fontSize, 12, 20)
});

const parseProjectSubmitConfig = (value?: string): ProjectSubmitConfig => {
  let custom: Partial<ProjectSubmitConfig> = {};
  if (value) {
    try {
      custom = JSON.parse(value);
    } catch {
      custom = {};
    }
  }
  const useSavedSceneHeaders = custom.sceneHeadersVersion === 2;
  return {
    componentBackground: custom.componentBackground || defaultProjectSubmitConfig.componentBackground,
    componentBackgroundOpacity: clampNumber(custom.componentBackgroundOpacity, defaultProjectSubmitConfig.componentBackgroundOpacity, 0, 100),
    componentBorderVisible: custom.componentBorderVisible === true,
    componentBorderColor: custom.componentBorderColor || defaultProjectSubmitConfig.componentBorderColor,
    componentBorderRadius: clampNumber(custom.componentBorderRadius, defaultProjectSubmitConfig.componentBorderRadius, 0, 32),
    defaultTabKey: custom.defaultTabKey || defaultProjectSubmitConfig.defaultTabKey,
    expandedPadding: clampNumber(custom.expandedPadding, defaultProjectSubmitConfig.expandedPadding, 0, 32),
    expandedGap: clampNumber(custom.expandedGap, defaultProjectSubmitConfig.expandedGap, 0, 32),
    expandedBackground: custom.expandedBackground || defaultProjectSubmitConfig.expandedBackground,
    expandedBackgroundOpacity: clampNumber(custom.expandedBackgroundOpacity, defaultProjectSubmitConfig.expandedBackgroundOpacity, 0, 100),
    expandedLayoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
    sceneHeadersVersion: 2,
    homeHeader: normalizeWorkspaceHeaderPage(
      'schoolSubmit',
      useSavedSceneHeaders ? custom.homeHeader || defaultSchoolSubmitHomeHeader() : defaultSchoolSubmitHomeHeader(),
      WORKSPACE_HEADER_LAYOUT_VERSION
    ),
    maximizedHeader: normalizeWorkspaceHeaderPage(
      'schoolSubmit',
      useSavedSceneHeaders ? custom.maximizedHeader || defaultSchoolSubmitMaximizedHeader() : defaultSchoolSubmitMaximizedHeader(),
      WORKSPACE_HEADER_LAYOUT_VERSION
    ),
    progress: normalizeProjectSubmitProgress(custom.progress, defaultProjectSubmitConfig.progress),
    tabs: mergeItems(defaultProjectSubmitConfig.tabs, custom.tabs).map((item) => ({
      ...item,
      label: item.key === 'all' ? normalizeSchoolProjectCategoryAllLabel(item.label) : item.label,
      aliasesText: (item.aliases || []).join(',')
    }))
  };
};

const normalizeAuditWorkbenchDefaultTab = (config: AuditWorkbenchConfig) => {
  const tabs: string[] = [];
  if (config.pendingTabVisible !== false) tabs.push('pending');
  if (config.mineTabVisible !== false) tabs.push('mine');
  if (!tabs.length) return defaultAuditWorkbenchConfig.defaultTab;
  return tabs.includes(config.defaultTab) ? config.defaultTab : tabs[0];
};

const parseAuditWorkbenchConfig = (value?: string): AuditWorkbenchConfig => {
  let custom: Partial<AuditWorkbenchConfig> = {};
  if (value) {
    try {
      custom = JSON.parse(value);
    } catch {
      custom = {};
    }
  }
  const config: AuditWorkbenchConfig = {
    kickerVisible: custom.kickerVisible !== false,
    kickerText: custom.kickerText || defaultAuditWorkbenchConfig.kickerText,
    kickerFontSize: clampNumber(custom.kickerFontSize, defaultAuditWorkbenchConfig.kickerFontSize, 10, 24),
    titleVisible: custom.titleVisible !== false,
    titleText: custom.titleText || '',
    titleFontSize: clampNumber(custom.titleFontSize, defaultAuditWorkbenchConfig.titleFontSize, 14, 32),
    refreshVisible: custom.refreshVisible !== false,
    defaultTab: custom.defaultTab || defaultAuditWorkbenchConfig.defaultTab,
    pendingTabVisible: custom.pendingTabVisible !== false,
    pendingTabLabel: custom.pendingTabLabel || defaultAuditWorkbenchConfig.pendingTabLabel,
    pendingSectionTitle: custom.pendingSectionTitle || defaultAuditWorkbenchConfig.pendingSectionTitle,
    auditListButtonVisible: custom.auditListButtonVisible !== false,
    auditListButtonText: custom.auditListButtonText || defaultAuditWorkbenchConfig.auditListButtonText,
    mineTabVisible: custom.mineTabVisible !== false,
    mineTabLabel: custom.mineTabLabel || defaultAuditWorkbenchConfig.mineTabLabel,
    metricsVisible: custom.metricsVisible !== false,
    metricColumns: clampNumber(custom.metricColumns, defaultAuditWorkbenchConfig.metricColumns, 1, 4),
    metricTotalLabel: custom.metricTotalLabel || defaultAuditWorkbenchConfig.metricTotalLabel,
    metricPassLabel: custom.metricPassLabel || defaultAuditWorkbenchConfig.metricPassLabel,
    metricReturnLabel: custom.metricReturnLabel || defaultAuditWorkbenchConfig.metricReturnLabel,
    metricLabelColor: custom.metricLabelColor || defaultAuditWorkbenchConfig.metricLabelColor,
    metricTotalColor: custom.metricTotalColor || defaultAuditWorkbenchConfig.metricTotalColor,
    metricPassColor: custom.metricPassColor || defaultAuditWorkbenchConfig.metricPassColor,
    metricReturnColor: custom.metricReturnColor || defaultAuditWorkbenchConfig.metricReturnColor,
    metricFontSize: clampNumber(custom.metricFontSize, defaultAuditWorkbenchConfig.metricFontSize, 10, 20),
    metricNumberFontSize: clampNumber(custom.metricNumberFontSize, defaultAuditWorkbenchConfig.metricNumberFontSize, 12, 28),
    groupVisible: custom.groupVisible !== false,
    groupSectionTitle: custom.groupSectionTitle || defaultAuditWorkbenchConfig.groupSectionTitle,
    groupSectionSubtitle: custom.groupSectionSubtitle || defaultAuditWorkbenchConfig.groupSectionSubtitle,
    groupColumns: clampNumber(custom.groupColumns, defaultAuditWorkbenchConfig.groupColumns, 1, 6),
    recordsVisible: custom.recordsVisible !== false,
    recordsSectionTitle: custom.recordsSectionTitle || defaultAuditWorkbenchConfig.recordsSectionTitle,
    recordsSectionSubtitle: custom.recordsSectionSubtitle || defaultAuditWorkbenchConfig.recordsSectionSubtitle,
    componentCss: custom.componentCss || ''
  };
  config.defaultTab = normalizeAuditWorkbenchDefaultTab(config);
  return config;
};

const mergeReviewCards = (base: ReviewWorkbenchCardConfig[], custom?: ReviewWorkbenchCardConfig[]) => {
  const customMap = new Map((custom || []).map((item) => [item.key, item]));
  const merged = base.map((item) => ({ ...item, ...(customMap.get(item.key) || {}) }));
  (custom || []).forEach((item) => {
    if (!merged.some((baseItem) => baseItem.key === item.key)) {
      merged.push(item);
    }
  });
  return merged;
};

const parseReviewWorkbenchConfig = (value?: string): ReviewWorkbenchConfig => {
  let custom: Partial<ReviewWorkbenchConfig> = {};
  if (value) {
    try {
      custom = JSON.parse(value);
    } catch {
      custom = {};
    }
  }
  return {
    kickerVisible: custom.kickerVisible !== false,
    kickerText: custom.kickerText || defaultReviewWorkbenchConfig.kickerText,
    kickerFontSize: clampNumber(custom.kickerFontSize, defaultReviewWorkbenchConfig.kickerFontSize, 10, 24),
    titleVisible: custom.titleVisible !== false,
    titleText: custom.titleText || '',
    titleFontSize: clampNumber(custom.titleFontSize, defaultReviewWorkbenchConfig.titleFontSize, 14, 32),
    refreshVisible: custom.refreshVisible !== false,
    listButtonVisible: custom.listButtonVisible !== false,
    listButtonText: custom.listButtonText || defaultReviewWorkbenchConfig.listButtonText,
    cardColumns: clampNumber(custom.cardColumns, defaultReviewWorkbenchConfig.cardColumns, 1, 6),
    pendingTabLabel: custom.pendingTabLabel || defaultReviewWorkbenchConfig.pendingTabLabel,
    recentTabLabel: custom.recentTabLabel || defaultReviewWorkbenchConfig.recentTabLabel,
    componentCss: custom.componentCss || '',
    cards: mergeReviewCards(defaultReviewWorkbenchConfig.cards, custom.cards)
  };
};

const assignProjectSubmitConfig = (target: ProjectSubmitConfig, source: ProjectSubmitConfig) => {
  target.componentBackground = source.componentBackground;
  target.componentBackgroundOpacity = source.componentBackgroundOpacity;
  target.componentBorderVisible = source.componentBorderVisible;
  target.componentBorderColor = source.componentBorderColor;
  target.componentBorderRadius = source.componentBorderRadius;
  target.defaultTabKey = source.defaultTabKey;
  target.expandedPadding = source.expandedPadding;
  target.expandedGap = source.expandedGap;
  target.expandedBackground = source.expandedBackground;
  target.expandedBackgroundOpacity = source.expandedBackgroundOpacity;
  target.expandedLayoutVersion = source.expandedLayoutVersion;
  target.sceneHeadersVersion = source.sceneHeadersVersion;
  target.homeHeader = JSON.parse(JSON.stringify(source.homeHeader));
  target.maximizedHeader = JSON.parse(JSON.stringify(source.maximizedHeader));
  target.progress = { ...source.progress };
  target.tabs = source.tabs;
};

const assignRoleShellConfig = (config?: Partial<WorkbenchRoleShellConfig>) => {
  Object.assign(roleShellForm, defaultRoleShellConfig(selectedRoleKey.value), config || {});
  roleShellOriginalSnapshot.value = roleShellSnapshot();
};

const loadRoleShell = async () => {
  if (!selectedRoleId.value) return;
  roleShellLoading.value = true;
  try {
    const res = await getRoleShellConfig(selectedRoleId.value);
    assignRoleShellConfig(res.data);
  } catch {
    assignRoleShellConfig();
  } finally {
    roleShellLoadedRoleId.value = String(selectedRoleId.value);
    roleShellLoading.value = false;
  }
};

const saveSelectedRoleShell = async () => {
  if (!selectedRoleId.value) return;
  if (!roleShellForm.showUserAvatar && roleShellForm.userDisplayMode === 'hidden') {
    proxy?.$modal.msgWarning('右上角头像和账号名称至少需要显示一个');
    return;
  }
  if (roleShellForm.brandTitleMode === 'activity' && !roleShellForm.brandActivityId) {
    proxy?.$modal.msgWarning('请选择活动，或改为自定义品牌文字');
    return;
  }
  if (roleShellForm.navbarTitleLogoMode === 'asset' && !roleShellForm.navbarTitleLogoAssetKey) {
    proxy?.$modal.msgWarning('请先上传 Header Logo，或改为系统 Logo/不显示');
    return;
  }
  roleShellSaving.value = true;
  try {
    await saveRoleShellConfig(selectedRoleId.value, { ...roleShellForm, configured: true });
    await loadRoleShell();
    proxy?.$modal.msgSuccess('角色系统布局已保存，角色用户刷新后生效');
  } finally {
    roleShellSaving.value = false;
  }
};

const loadBrandActivityOptions = async () => {
  try {
    const res = await listActivityOptions();
    brandActivityOptions.value = res.data || [];
    await loadArtDetailDisplayConfig(true);
    const configuredId = String(detailDisplayConfig.value.workspaceHeader.managedActivityId || '').trim();
    toolbarManagedActivityId.value = configuredId || String(managedActivityOptions.value[0]?.id || '');
  } catch {
    brandActivityOptions.value = [];
    toolbarManagedActivityId.value = '';
  }
};

const saveToolbarManagedActivity = async (activityId: string | number) => {
  const normalizedId = String(activityId || '').trim();
  if (!normalizedId) return;
  managedActivitySaving.value = true;
  try {
    await saveManagedActivity(normalizedId);
    await loadArtDetailDisplayConfig(true);
    toolbarManagedActivityId.value = String(detailDisplayConfig.value.workspaceHeader.managedActivityId || normalizedId);
    pageHeaderEditorRef.value?.syncPersistedManagedActivityId?.(normalizedId);
    if (reviewDisplayEditorRef.value) await reviewDisplayEditorRef.value.initialize?.();
    proxy?.$modal.msgSuccess('当前业务活动已保存，用户页面刷新后生效');
  } catch (error: any) {
    toolbarManagedActivityId.value = String(detailDisplayConfig.value.workspaceHeader.managedActivityId || managedActivityOptions.value[0]?.id || '');
    proxy?.$modal.msgError(error?.msg || error?.message || '保存当前业务活动失败');
  } finally {
    managedActivitySaving.value = false;
  }
};

const restoreSelectedRoleShell = async () => {
  if (!selectedRoleId.value) return;
  const roleName = roleOptions.value.find((item) => String(item.roleId) === String(selectedRoleId.value))?.roleName || '当前角色';
  try {
    await ElMessageBox.confirm(`将“${roleName}”的系统布局恢复为角色默认值，角色用户刷新后生效。是否继续？`, '恢复角色系统布局', {
      type: 'warning',
      confirmButtonText: '恢复默认',
      cancelButtonText: '取消'
    });
    roleShellRestoring.value = true;
    await restoreRoleShellConfig(selectedRoleId.value);
    await loadRoleShell();
    proxy?.$modal.msgSuccess('角色系统布局已恢复默认');
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      proxy?.$modal.msgError(error?.msg || error?.message || '恢复角色系统布局失败');
    }
  } finally {
    roleShellRestoring.value = false;
  }
};

type ProjectSubmitCategorySource = WorkbenchConfigItem & {
  sourceType: 'group' | 'category';
  sourceLevel: 1 | 2;
};

const projectSubmitCategorySourceKey = (sourceType: 'group' | 'category', sourceId?: string | number, sourceCode?: string, sourceName?: string) =>
  `${sourceType}:${String(sourceId ?? sourceCode ?? sourceName ?? '')}`;

const buildProjectSubmitCategorySources = (catalog: ActivityCategoryVO[]): ProjectSubmitCategorySource[] => {
  const rows: ProjectSubmitCategorySource[] = [];
  let order = 2;
  buildSchoolCategoryMenuTree(catalog).forEach((root) => {
    if (isCategoryGroup(root)) {
      const children = root.children || [];
      if (!children.length) return;
      rows.push({
        key: projectSubmitCategorySourceKey('group', root.id, root.categoryCode, root.categoryName),
        label: String(root.categoryName || root.categoryCode || '未命名一级类别'),
        sourceType: 'group',
        sourceId: root.id,
        sourceCode: String(root.categoryCode || ''),
        sourceName: String(root.categoryName || ''),
        sourceLevel: 1,
        visible: true,
        order: order++,
        showCount: true,
        aliases: [root.categoryName, root.categoryCode].filter(Boolean).map(String),
        aliasesText: [root.categoryName, root.categoryCode].filter(Boolean).map(String).join(',')
      });
      children.forEach((child) => {
        if (child.id === undefined || child.id === null) return;
        rows.push({
          key: projectSubmitCategorySourceKey('category', child.id, child.categoryCode, child.categoryName),
          label: String(child.categoryName || child.categoryCode || '未命名二级类别'),
          sourceType: 'category',
          sourceId: child.id,
          sourceCode: String(child.categoryCode || ''),
          sourceName: String(child.categoryName || ''),
          sourceLevel: 2,
          visible: true,
          order: order++,
          showCount: true,
          aliases: [child.categoryName, child.categoryCode].filter(Boolean).map(String),
          aliasesText: [child.categoryName, child.categoryCode].filter(Boolean).map(String).join(',')
        });
      });
      return;
    }
    if (root.id === undefined || root.id === null) return;
    rows.push({
      key: projectSubmitCategorySourceKey('category', root.id, root.categoryCode, root.categoryName),
      label: String(root.categoryName || root.categoryCode || '未命名一级类别'),
      sourceType: 'category',
      sourceId: root.id,
      sourceCode: String(root.categoryCode || ''),
      sourceName: String(root.categoryName || ''),
      sourceLevel: 1,
      visible: true,
      order: order++,
      showCount: true,
      aliases: [root.categoryName, root.categoryCode].filter(Boolean).map(String),
      aliasesText: [root.categoryName, root.categoryCode].filter(Boolean).map(String).join(',')
    });
  });
  return rows;
};

const projectSubmitTabMatchesSource = (tab: WorkbenchConfigItem, source: ProjectSubmitCategorySource) => {
  if (tab.key === source.key) return true;
  if (tab.sourceType === source.sourceType && tab.sourceId !== undefined && String(tab.sourceId) === String(source.sourceId)) return true;
  if (tab.sourceCode && source.sourceCode && tab.sourceCode === source.sourceCode) return true;
  const aliases = [
    ...(tab.aliases || []),
    ...String(tab.aliasesText || '')
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  ];
  const sourceCode = String(source.sourceCode || '');
  const sourceName = String(source.sourceName || '');
  return aliases.some((alias) => {
    const value = String(alias || '').trim();
    return Boolean(value && (sourceCode === value || sourceName === value || sourceCode.includes(value) || sourceName.includes(value)));
  });
};

const mergeProjectSubmitCategoryTabs = (catalog: ActivityCategoryVO[]) => {
  const existingTabs = [...configForm.tabs];
  const previousDefault = existingTabs.find((item) => item.key === configForm.defaultTabKey);
  const allExisting = existingTabs.find((item) => item.key === 'all');
  const allRow: WorkbenchConfigItem = {
    ...(allExisting || defaultProjectSubmitConfig.tabs[0]),
    key: 'all',
    label: allExisting?.label || defaultProjectSubmitConfig.tabs[0].label,
    visible: allExisting?.visible !== false,
    order: Number(allExisting?.order || 1),
    showCount: allExisting?.showCount !== false,
    aliases: [],
    aliasesText: ''
  };
  const sourceRows = buildProjectSubmitCategorySources(catalog).map((source) => {
    const existing =
      existingTabs.find((item) => item.key === source.key) ||
      existingTabs.find(
        (item) => item.sourceType === source.sourceType && item.sourceId !== undefined && String(item.sourceId) === String(source.sourceId)
      ) ||
      existingTabs.find((item) => projectSubmitTabMatchesSource(item, source));
    const hasCustomLabel = Boolean(existing?.label && existing.sourceName && String(existing.label) !== String(existing.sourceName));
    return {
      ...source,
      label: hasCustomLabel || (existing && !existing.sourceName) ? String(existing?.label || source.label) : source.label,
      visible: existing?.visible !== false,
      order: Number(existing?.order || source.order),
      showCount: existing?.showCount !== false
    };
  });
  configForm.tabs = [allRow, ...sourceRows];
  const mappedDefault =
    previousDefault?.key === 'all'
      ? 'all'
      : sourceRows.find((source) => previousDefault && projectSubmitTabMatchesSource(previousDefault, source))?.key;
  const visibleDefault = configForm.tabs.find((item) => item.key === mappedDefault && item.visible !== false);
  configForm.defaultTabKey = visibleDefault?.key || configForm.tabs.find((item) => item.visible !== false)?.key || 'all';
};

const syncProjectSubmitCategoryTabs = async (showMessage = false) => {
  projectSubmitCategoryLoading.value = true;
  try {
    const activityRes = await listActivityOptions();
    const currentActivity = (activityRes.data || []).find((item) => item.status === 'enabled');
    projectSubmitCurrentActivity.value = currentActivity;
    projectSubmitCategoryCatalog.value = [];
    if (!currentActivity?.id) {
      if (showMessage) proxy?.$modal.msgWarning('未找到当前启用活动，已保留现有筛选配置');
      return;
    }
    const categoryRes = await listCategory(currentActivity.id, { deleted: false });
    projectSubmitCategoryCatalog.value = categoryRes.data || [];
    mergeProjectSubmitCategoryTabs(projectSubmitCategoryCatalog.value);
    if (showMessage) proxy?.$modal.msgSuccess(projectSubmitCategorySyncTitle.value);
  } catch (error: any) {
    if (showMessage) proxy?.$modal.msgError(error?.msg || error?.message || '同步当前活动类别失败');
  } finally {
    projectSubmitCategoryLoading.value = false;
  }
};

const openProjectSubmitConfig = async (row: WorkbenchLayoutVO) => {
  configTargetRow.value = row;
  await loadArtDetailDisplayConfig(true);
  assignProjectSubmitConfig(configForm, parseProjectSubmitConfig(row.configJson));
  configDialog.activeTab = 'homeHeader';
  configDialog.visible = true;
  await syncProjectSubmitCategoryTabs();
};

const restoreProjectSubmitConfig = async () => {
  const target = configTargetRow.value;
  if (!target) return;
  try {
    await ElMessageBox.confirm('仅恢复当前学校报送卡片的文字和展示配置，不影响卡片位置、宽度、显示状态及其他组件。是否继续？', '恢复默认配置', {
      type: 'warning',
      confirmButtonText: '恢复默认',
      cancelButtonText: '取消'
    });
    const defaults = cloneProjectSubmitConfig(defaultProjectSubmitConfig);
    assignProjectSubmitConfig(configForm, defaults);
    await syncProjectSubmitCategoryTabs();
    proxy?.$modal.msgSuccess('已恢复弹窗内的默认草稿；点击“应用到当前草稿”后才会带回首页布局草稿');
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      proxy?.$modal.msgError(error?.msg || error?.message || '恢复默认配置失败');
    }
  }
};

const assignAuditWorkbenchConfig = (target: AuditWorkbenchConfig, source: AuditWorkbenchConfig) => {
  Object.assign(target, cloneAuditWorkbenchConfig(source));
};

const assignReviewWorkbenchConfig = (target: ReviewWorkbenchConfig, source: ReviewWorkbenchConfig) => {
  Object.assign(target, cloneReviewWorkbenchConfig(source));
};

const assignStageNoticeConfig = (target: StageNoticeConfig, source: StageNoticeConfig) => {
  target.categoryConfigMode = source.categoryConfigMode;
  target.topHeight = source.topHeight;
  target.topGap = source.topGap;
  target.noticeRatio = source.noticeRatio;
  target.activityRatio = source.activityRatio;
  target.calendarWidth = source.calendarWidth;
  target.calendarHeight = source.calendarHeight;
  target.calendarRadius = source.calendarRadius;
  target.calendarBackground = source.calendarBackground;
  target.calendarTextColor = source.calendarTextColor;
  target.calendarMonthFontSize = source.calendarMonthFontSize;
  target.calendarDayFontSize = source.calendarDayFontSize;
  target.calendarWeekdayFontSize = source.calendarWeekdayFontSize;
  target.calendarTimeFontSize = source.calendarTimeFontSize;
  target.calendarTimeBackground = source.calendarTimeBackground;
  target.calendarShowTime = source.calendarShowTime;
  target.calendarShowSeconds = source.calendarShowSeconds;
  target.quotaSummaryEnabled = source.quotaSummaryEnabled;
  target.quotaSummaryFields = [...source.quotaSummaryFields];
  target.quotaSummaryTemplate = source.quotaSummaryTemplate;
  target.quotaSummaryFontSize = source.quotaSummaryFontSize;
  target.quotaSummaryTextColor = source.quotaSummaryTextColor;
  target.quotaSummaryNumberColor = source.quotaSummaryNumberColor;
  target.quotaSummaryBackground = source.quotaSummaryBackground;
  target.quotaSummaryBorderColor = source.quotaSummaryBorderColor;
  target.quotaSummaryRadius = source.quotaSummaryRadius;
  target.quotaSummaryGap = source.quotaSummaryGap;
  target.quotaSummaryCss = source.quotaSummaryCss;
  target.quotaRatioEnabled = source.quotaRatioEnabled;
  target.quotaRatioMaxItems = source.quotaRatioMaxItems;
  target.quotaRatioFontSize = source.quotaRatioFontSize;
  target.quotaRatioTextColor = source.quotaRatioTextColor;
  target.quotaRatioNumberColor = source.quotaRatioNumberColor;
  target.quotaRatioBackground = source.quotaRatioBackground;
  target.quotaRatioBorderColor = source.quotaRatioBorderColor;
  target.quotaRatioRadius = source.quotaRatioRadius;
  target.quotaRatioCss = source.quotaRatioCss;
  target.cardRatioEnabled = source.cardRatioEnabled;
  target.schoolTypeLabelReplacements = source.schoolTypeLabelReplacements.map((item) => ({ ...item }));
  target.categoryColumns = source.categoryColumns;
  target.categoryGap = source.categoryGap;
  target.categoryRadius = source.categoryRadius;
  target.hoverEffectMode = source.hoverEffectMode;
  target.hoverScaleEnabled = source.hoverScaleEnabled;
  target.hoverScale = source.hoverScale;
  target.categoryAreaBackground = source.categoryAreaBackground;
  target.componentCss = source.componentCss;
  target.dateCss = source.dateCss;
  target.categories = source.categories;
};

const openStageNoticeConfig = async (row: WorkbenchLayoutVO) => {
  stageConfigTargetRow.value = row;
  stageConfigKind.value =
    row.componentKey === ADMIN_NOTICE_COMPONENT_KEY
      ? 'admin'
      : row.componentKey === AUDIT_NOTICE_COMPONENT_KEY
        ? 'audit'
        : row.componentKey === REVIEW_NOTICE_COMPONENT_KEY
          ? 'review'
          : 'school';
  assignStageNoticeConfig(stageConfigForm, parseStageNoticeConfig(row.configJson, row.componentKey));
  stageConfigDialog.activeTab = 'top';
  stageConfigDialog.visible = true;
  stageConfigActivityId.value = undefined;
  stageActivityOptions.value = [];
  stageCategoryCatalog.value = [];
  stageCardSourceKey.value = '';
  if (stageConfigKind.value === 'school') {
    await loadStageActivityOptions();
  }
};

const openCategoryVisualConfig = (row: WorkbenchConfigItem) => {
  const index = stageConfigForm.categories.indexOf(row);
  if (index < 0) return;
  categoryVisualDialog.targetIndex = index;
  Object.assign(categoryVisualForm, {
    key: row.key,
    label: row.label,
    titleColor: row.titleColor,
    numberColor: row.numberColor,
    accentColor: row.accentColor,
    borderColor: row.borderColor,
    backgroundColor: row.backgroundColor,
    ...normalizeVisualConfig(row)
  });
  categoryVisualDialog.visible = true;
};

const applyAccentColorToPattern = () => {
  categoryVisualForm.patternColor = categoryVisualForm.accentColor || '#2563eb';
};

const resetCategoryVisualToDefault = () => {
  Object.assign(categoryVisualForm, defaultVisualConfig(categoryVisualForm));
};

const resetAllCategoryVisualToDefault = () => {
  stageConfigCategoryRows.value.forEach((category) => {
    Object.assign(category, defaultVisualConfig(category));
  });
  proxy?.$modal.msgSuccess(stageConfigKind.value === 'school' ? '已将当前活动的分类卡视觉恢复为默认样式' : '已将当前组件的统计卡视觉恢复为默认样式');
};

const handleCategoryVisualPatternModeChange = (mode: string | number | boolean | undefined) => {
  const value = String(mode || '');
  if (value === 'icon') {
    categoryVisualForm.patternIcon = normalizePatternIconValue(categoryVisualForm.patternIcon);
    if (!categoryVisualForm.patternColor) applyAccentColorToPattern();
    return;
  }
  if (value === 'text') {
    categoryVisualForm.patternText = normalizePatternTextValue(categoryVisualForm.patternText);
    if (!categoryVisualForm.patternColor) applyAccentColorToPattern();
    return;
  }
  if (value === 'preset' && !categoryVisualForm.patternColor) applyAccentColorToPattern();
};

const isAllowedCategoryVisualFile = (file: File) => {
  const extension = file.name.split('.').pop()?.toLowerCase() || '';
  const allowedExtensions = ['png', 'jpg', 'jpeg', 'webp'];
  const allowedTypes = ['', 'image/png', 'image/jpeg', 'image/webp'];
  if (!allowedExtensions.includes(extension) || !allowedTypes.includes(file.type)) {
    proxy?.$modal.msgError('仅支持 png、jpg、jpeg、webp 图片');
    return false;
  }
  if (file.size > 5 * 1024 * 1024) {
    proxy?.$modal.msgError('图片大小不能超过 5MB');
    return false;
  }
  return true;
};

const handleCategoryVisualAssetChange = async (uploadFile: { raw?: File; name?: string }) => {
  const file = uploadFile.raw;
  if (!file || !isAllowedCategoryVisualFile(file)) return;
  categoryVisualAssetUploading.value = true;
  try {
    const { data } = await uploadWorkbenchAsset(file);
    categoryVisualForm.patternMode = 'image';
    categoryVisualForm.patternImageKey = data.fileKey;
    categoryVisualForm.patternImageName = data.originalName || uploadFile.name || file.name;
    categoryVisualForm.patternImageSize = categoryVisualForm.patternImageSize || 'cover';
    categoryVisualForm.patternImageRepeat = categoryVisualForm.patternImageRepeat || 'no-repeat';
    proxy?.$modal.msgSuccess('图片已上传');
  } catch (error: any) {
    proxy?.$modal.msgError(error?.msg || '图片上传失败');
  } finally {
    categoryVisualAssetUploading.value = false;
  }
};

const clearCategoryVisualImage = async (deleteFile = false) => {
  const key = categoryVisualForm.patternImageKey;
  if (deleteFile && key) {
    categoryVisualAssetDeleting.value = true;
    try {
      await deleteWorkbenchAsset(key);
      proxy?.$modal.msgSuccess('图片文件已删除');
    } catch (error: any) {
      proxy?.$modal.msgError(error?.msg || '图片删除失败');
      categoryVisualAssetDeleting.value = false;
      return;
    }
    categoryVisualAssetDeleting.value = false;
  }
  categoryVisualForm.patternImageKey = '';
  categoryVisualForm.patternImageName = '';
};

const saveCategoryVisualConfig = () => {
  const target = stageConfigForm.categories[categoryVisualDialog.targetIndex];
  if (!target) return;
  Object.assign(target, normalizeVisualConfig({ ...target, ...categoryVisualForm }));
  categoryVisualDialog.visible = false;
};

const openAuditWorkbenchConfig = (row: WorkbenchLayoutVO) => {
  auditWorkbenchConfigTargetRow.value = row;
  assignAuditWorkbenchConfig(auditWorkbenchConfigForm, parseAuditWorkbenchConfig(row.configJson));
  auditWorkbenchConfigDialog.activeTab = 'base';
  auditWorkbenchConfigDialog.visible = true;
};

const openReviewWorkbenchConfig = (row: WorkbenchLayoutVO) => {
  reviewWorkbenchConfigTargetRow.value = row;
  assignReviewWorkbenchConfig(reviewWorkbenchConfigForm, parseReviewWorkbenchConfig(row.configJson));
  reviewWorkbenchConfigDialog.activeTab = 'base';
  reviewWorkbenchConfigDialog.visible = true;
};

const openUniversalDashboardConfig = (row: WorkbenchLayoutVO) => {
  universalDashboardConfigTargetRow.value = row;
  universalDashboardConfigDialog.visible = true;
};

const addSchoolTypeLabelReplacement = () => {
  stageConfigForm.schoolTypeLabelReplacements.push({ source: '', target: '' });
};

const removeSchoolTypeLabelReplacement = (index: number) => {
  stageConfigForm.schoolTypeLabelReplacements.splice(index, 1);
};

const normalizeConfigItems = (items: WorkbenchConfigItem[], withAliases = false) =>
  items.map((item) => ({
    key: item.key,
    label: item.label,
    sourceType: item.sourceType,
    sourceId: item.sourceId,
    sourceCode: item.sourceCode,
    sourceName: item.sourceName,
    sourceLevel: item.sourceLevel,
    visible: item.visible !== false,
    order: Number(item.order || 0),
    width: item.width ? Number(item.width) : undefined,
    showCount: item.showCount !== false,
    aliases: withAliases
      ? String(item.aliasesText || '')
          .split(',')
          .map((text) => text.trim())
          .filter(Boolean)
      : item.aliases
  }));

const normalizedDefaultTabKey = () => {
  const visibleTab = configForm.tabs.find((item) => item.key === configForm.defaultTabKey && item.visible !== false);
  if (visibleTab) return visibleTab.key;
  return configForm.tabs.find((item) => item.visible !== false)?.key || configForm.tabs[0]?.key || defaultProjectSubmitConfig.defaultTabKey || 'all';
};

const saveProjectSubmitConfig = () => {
  if (!configTargetRow.value) return;
  const config: ProjectSubmitConfig = {
    componentBackground: configForm.componentBackground,
    componentBackgroundOpacity: clampNumber(configForm.componentBackgroundOpacity, 0, 0, 100),
    componentBorderVisible: configForm.componentBorderVisible,
    componentBorderColor: configForm.componentBorderColor,
    componentBorderRadius: clampNumber(configForm.componentBorderRadius, 8, 0, 32),
    defaultTabKey: normalizedDefaultTabKey(),
    expandedPadding: clampNumber(configForm.expandedPadding, defaultProjectSubmitConfig.expandedPadding, 0, 32),
    expandedGap: clampNumber(configForm.expandedGap, defaultProjectSubmitConfig.expandedGap, 0, 32),
    expandedBackground: configForm.expandedBackground,
    expandedBackgroundOpacity: clampNumber(configForm.expandedBackgroundOpacity, defaultProjectSubmitConfig.expandedBackgroundOpacity, 0, 100),
    expandedLayoutVersion: WORKSPACE_HEADER_LAYOUT_VERSION,
    sceneHeadersVersion: 2,
    homeHeader: normalizeWorkspaceHeaderPage('schoolSubmit', configForm.homeHeader, WORKSPACE_HEADER_LAYOUT_VERSION),
    maximizedHeader: normalizeWorkspaceHeaderPage('schoolSubmit', configForm.maximizedHeader, WORKSPACE_HEADER_LAYOUT_VERSION),
    progress: normalizeProjectSubmitProgress(configForm.progress),
    tabs: normalizeConfigItems(configForm.tabs, true).map(({ width, ...item }) =>
      item.key === 'all' ? { ...item, label: normalizeSchoolProjectCategoryAllLabel(item.label) } : item
    )
  };
  configTargetRow.value.configJson = JSON.stringify(config);
  configDialog.visible = false;
  proxy?.$modal.msgSuccess('组件配置已应用到当前草稿，保存当前范围后生效');
};

const normalizeStageCategories = (items: WorkbenchConfigItem[], includeSecondary = false) =>
  items.map((item) => {
    const visual = normalizeVisualConfig(item);
    const normalized: WorkbenchConfigItem = {
      key: item.key,
      label: item.label,
      activityId: item.activityId,
      sourceType: item.sourceType,
      sourceId: item.sourceId,
      sourceCode: item.sourceCode || '',
      sourceName: item.sourceName || '',
      visible: item.visible !== false,
      order: Number(item.order || 0),
      aliases: String(item.aliasesText || '')
        .split(',')
        .map((text) => text.trim())
        .filter(Boolean),
      fontSize: Number(item.fontSize || 15),
      titleColor: item.titleColor || '#102a43',
      numberColor: item.numberColor || '#2563eb',
      backgroundColor: item.backgroundColor || '#f8fbff',
      borderColor: item.borderColor || '#dbeafe',
      accentColor: item.accentColor || '#2563eb',
      customCss: item.customCss || '',
      ...visual,
      showQuota: item.showQuota !== false,
      showSubmitted: item.showSubmitted !== false,
      linkEnabled: item.linkEnabled !== false,
      quotaDisplayMode: normalizeQuotaDisplayMode(item.quotaDisplayMode, item.showQuota),
      quotaFields: normalizeQuotaFields(item.quotaFields),
      quotaPosition: normalizeQuotaPosition(item.quotaPosition)
    };
    if (includeSecondary) {
      normalized.secondary = {
        visible: item.secondaryVisible === true,
        label: item.secondaryLabel || '',
        valueKey: item.secondaryValueKey || '',
        fontSize: clampNumber(item.secondaryFontSize, 13, 10, 20),
        textColor: item.secondaryTextColor || '#5c7087',
        numberColor: item.secondaryNumberColor || item.numberColor || '#2563eb'
      };
    }
    return normalized;
  });

const saveStageNoticeConfig = () => {
  if (!stageConfigTargetRow.value) return;
  if (stageConfigKind.value === 'school') {
    if (!stageConfigActivityId.value) {
      proxy?.$modal.msgError('请先选择配置活动');
      return;
    }
    const invalidRows = stageConfigForm.categories.filter(
      (item) => !item.activityId || !item.sourceType || item.sourceId === undefined || item.sourceId === null
    );
    if (invalidRows.length) {
      proxy?.$modal.msgError(`仍有 ${invalidRows.length} 张旧卡片未绑定活动类别，请删除后再保存`);
      return;
    }
  }
  const hoverEffectMode = normalizeHoverEffectMode(stageConfigForm.hoverEffectMode, stageConfigForm.hoverScaleEnabled);
  const config: StageNoticeConfig = {
    categoryConfigMode: stageConfigKind.value === 'school' ? 'activity' : stageConfigForm.categoryConfigMode,
    ...normalizeNoticeLayout(stageConfigForm, defaultNoticeConfig(stageConfigTargetRow.value.componentKey)),
    categoryColumns: Number(stageConfigForm.categoryColumns || 5),
    categoryGap: Number(stageConfigForm.categoryGap || 0),
    categoryRadius: Number(stageConfigForm.categoryRadius || 0),
    hoverEffectMode,
    hoverScaleEnabled: hoverEffectMode !== 'none',
    hoverScale: normalizeHoverScale(stageConfigForm.hoverScale),
    categoryAreaBackground: stageConfigForm.categoryAreaBackground || '#f8fbff',
    componentCss: stageConfigForm.componentCss || '',
    dateCss: stageConfigForm.dateCss || '',
    categories: normalizeStageCategories(stageConfigForm.categories, stageConfigKind.value === 'audit')
  };
  stageConfigTargetRow.value.configJson = JSON.stringify(config);
  stageConfigDialog.visible = false;
  proxy?.$modal.msgSuccess('组件配置已应用到当前草稿，保存当前范围后生效');
};

const saveAuditWorkbenchConfig = () => {
  if (!auditWorkbenchConfigTargetRow.value) return;
  const config: AuditWorkbenchConfig = {
    kickerVisible: auditWorkbenchConfigForm.kickerVisible !== false,
    kickerText: auditWorkbenchConfigForm.kickerText || defaultAuditWorkbenchConfig.kickerText,
    kickerFontSize: clampNumber(auditWorkbenchConfigForm.kickerFontSize, defaultAuditWorkbenchConfig.kickerFontSize, 10, 24),
    titleVisible: auditWorkbenchConfigForm.titleVisible !== false,
    titleText: auditWorkbenchConfigForm.titleText || '',
    titleFontSize: clampNumber(auditWorkbenchConfigForm.titleFontSize, defaultAuditWorkbenchConfig.titleFontSize, 14, 32),
    refreshVisible: auditWorkbenchConfigForm.refreshVisible !== false,
    defaultTab: auditWorkbenchConfigForm.defaultTab || defaultAuditWorkbenchConfig.defaultTab,
    pendingTabVisible: auditWorkbenchConfigForm.pendingTabVisible !== false,
    pendingTabLabel: auditWorkbenchConfigForm.pendingTabLabel || defaultAuditWorkbenchConfig.pendingTabLabel,
    pendingSectionTitle: auditWorkbenchConfigForm.pendingSectionTitle || defaultAuditWorkbenchConfig.pendingSectionTitle,
    auditListButtonVisible: auditWorkbenchConfigForm.auditListButtonVisible !== false,
    auditListButtonText: auditWorkbenchConfigForm.auditListButtonText || defaultAuditWorkbenchConfig.auditListButtonText,
    mineTabVisible: auditWorkbenchConfigForm.mineTabVisible !== false,
    mineTabLabel: auditWorkbenchConfigForm.mineTabLabel || defaultAuditWorkbenchConfig.mineTabLabel,
    metricsVisible: auditWorkbenchConfigForm.metricsVisible !== false,
    metricColumns: clampNumber(auditWorkbenchConfigForm.metricColumns, defaultAuditWorkbenchConfig.metricColumns, 1, 4),
    metricTotalLabel: auditWorkbenchConfigForm.metricTotalLabel || defaultAuditWorkbenchConfig.metricTotalLabel,
    metricPassLabel: auditWorkbenchConfigForm.metricPassLabel || defaultAuditWorkbenchConfig.metricPassLabel,
    metricReturnLabel: auditWorkbenchConfigForm.metricReturnLabel || defaultAuditWorkbenchConfig.metricReturnLabel,
    metricLabelColor: auditWorkbenchConfigForm.metricLabelColor || defaultAuditWorkbenchConfig.metricLabelColor,
    metricTotalColor: auditWorkbenchConfigForm.metricTotalColor || defaultAuditWorkbenchConfig.metricTotalColor,
    metricPassColor: auditWorkbenchConfigForm.metricPassColor || defaultAuditWorkbenchConfig.metricPassColor,
    metricReturnColor: auditWorkbenchConfigForm.metricReturnColor || defaultAuditWorkbenchConfig.metricReturnColor,
    metricFontSize: clampNumber(auditWorkbenchConfigForm.metricFontSize, defaultAuditWorkbenchConfig.metricFontSize, 10, 20),
    metricNumberFontSize: clampNumber(auditWorkbenchConfigForm.metricNumberFontSize, defaultAuditWorkbenchConfig.metricNumberFontSize, 12, 28),
    groupVisible: auditWorkbenchConfigForm.groupVisible !== false,
    groupSectionTitle: auditWorkbenchConfigForm.groupSectionTitle || defaultAuditWorkbenchConfig.groupSectionTitle,
    groupSectionSubtitle: auditWorkbenchConfigForm.groupSectionSubtitle || defaultAuditWorkbenchConfig.groupSectionSubtitle,
    groupColumns: clampNumber(auditWorkbenchConfigForm.groupColumns, defaultAuditWorkbenchConfig.groupColumns, 1, 6),
    recordsVisible: auditWorkbenchConfigForm.recordsVisible !== false,
    recordsSectionTitle: auditWorkbenchConfigForm.recordsSectionTitle || defaultAuditWorkbenchConfig.recordsSectionTitle,
    recordsSectionSubtitle: auditWorkbenchConfigForm.recordsSectionSubtitle || defaultAuditWorkbenchConfig.recordsSectionSubtitle,
    componentCss: auditWorkbenchConfigForm.componentCss || ''
  };
  if (config.pendingTabVisible === false && config.mineTabVisible === false) {
    config.mineTabVisible = true;
  }
  config.defaultTab = normalizeAuditWorkbenchDefaultTab(config);
  auditWorkbenchConfigTargetRow.value.configJson = JSON.stringify(config);
  auditWorkbenchConfigDialog.visible = false;
  proxy?.$modal.msgSuccess('组件配置已应用到当前草稿，保存当前范围后生效');
};

const normalizeReviewCards = (items: ReviewWorkbenchCardConfig[]) =>
  items.map((item) => ({
    key: item.key,
    label: item.label,
    valueKey: item.valueKey || item.key,
    visible: item.visible !== false,
    order: Number(item.order || 0),
    imageUrl: item.imageUrl || '',
    titleColor: item.titleColor || '#082f49',
    numberColor: item.numberColor || '#2563eb',
    backgroundColor: item.backgroundColor || '#f8fbff',
    borderColor: item.borderColor || '#bfd7ff',
    accentColor: item.accentColor || '#2563eb',
    linkStatus: item.linkStatus || ''
  }));

const saveReviewWorkbenchConfig = () => {
  if (!reviewWorkbenchConfigTargetRow.value) return;
  const config: ReviewWorkbenchConfig = {
    kickerVisible: reviewWorkbenchConfigForm.kickerVisible !== false,
    kickerText: reviewWorkbenchConfigForm.kickerText || defaultReviewWorkbenchConfig.kickerText,
    kickerFontSize: clampNumber(reviewWorkbenchConfigForm.kickerFontSize, defaultReviewWorkbenchConfig.kickerFontSize, 10, 24),
    titleVisible: reviewWorkbenchConfigForm.titleVisible !== false,
    titleText: reviewWorkbenchConfigForm.titleText || '',
    titleFontSize: clampNumber(reviewWorkbenchConfigForm.titleFontSize, defaultReviewWorkbenchConfig.titleFontSize, 14, 32),
    refreshVisible: reviewWorkbenchConfigForm.refreshVisible !== false,
    listButtonVisible: reviewWorkbenchConfigForm.listButtonVisible !== false,
    listButtonText: reviewWorkbenchConfigForm.listButtonText || defaultReviewWorkbenchConfig.listButtonText,
    cardColumns: clampNumber(reviewWorkbenchConfigForm.cardColumns, defaultReviewWorkbenchConfig.cardColumns, 1, 6),
    pendingTabLabel: reviewWorkbenchConfigForm.pendingTabLabel || defaultReviewWorkbenchConfig.pendingTabLabel,
    recentTabLabel: reviewWorkbenchConfigForm.recentTabLabel || defaultReviewWorkbenchConfig.recentTabLabel,
    componentCss: reviewWorkbenchConfigForm.componentCss || '',
    cards: normalizeReviewCards(reviewWorkbenchConfigForm.cards)
  };
  reviewWorkbenchConfigTargetRow.value.configJson = JSON.stringify(config);
  reviewWorkbenchConfigDialog.visible = false;
  proxy?.$modal.msgSuccess('组件配置已应用到当前草稿，保存当前范围后生效');
};

const saveUniversalDashboardConfig = (configJson: string) => {
  if (!universalDashboardConfigTargetRow.value) return;
  universalDashboardConfigTargetRow.value.configJson = configJson;
  proxy?.$modal.msgSuccess('组件配置已应用到当前草稿，保存当前范围后生效');
};

const saveLayout = async () => {
  if (!selectedRoleId.value) return;
  saving.value = true;
  try {
    const rows = layoutRows.value.map((item, index) => ({ ...item, sortOrder: index + 1 }));
    await saveRoleWorkbench(selectedRoleId.value, rows);
    await loadRoleLayout();
    proxy?.$modal.msgSuccess('当前角色首页配置已保存并完成回读');
  } finally {
    saving.value = false;
  }
};

const handleBeforeUnload = (event: BeforeUnloadEvent) => {
  if (!hasAnyWorkbenchDirty.value) return;
  event.preventDefault();
  event.returnValue = '';
};

onMounted(() => {
  void loadRoles();
  void loadBrandActivityOptions();
  window.addEventListener('beforeunload', handleBeforeUnload);
  nextTick(() => {
    if (!workbenchControlPanelRef.value) return;
    workbenchControlHeight.value = Math.ceil(workbenchControlPanelRef.value.getBoundingClientRect().height);
    workbenchControlObserver = new ResizeObserver(([entry]) => {
      workbenchControlHeight.value = Math.ceil(entry.contentRect.height);
    });
    workbenchControlObserver.observe(workbenchControlPanelRef.value);
  });
});

onBeforeRouteLeave(async () => {
  const parentManagedDirty = layoutDirty.value || roleShellDirty.value || styleDirty.value || tableLayoutDirty.value || tableAppearanceDirty.value;
  if (!parentManagedDirty) return true;
  return confirmDiscardCurrentScope('离开页面', parentManagedDirty);
});

onBeforeUnmount(() => {
  workbenchControlObserver?.disconnect();
  window.removeEventListener('beforeunload', handleBeforeUnload);
});
</script>

<style scoped lang="scss">
.workbench-config-page {
  min-width: 0;

  .config-package-input {
    display: none;
  }

  .workbench-config-control {
    position: sticky;
    top: 0;
    z-index: 30;
    display: grid;
    gap: 5px;
    margin-bottom: 12px;
    background: color-mix(in srgb, var(--el-bg-color-page) 94%, transparent);
    backdrop-filter: blur(10px);
  }

  .workbench-config-toolbar {
    display: flex;
    min-height: 42px;
    align-items: center;
    gap: 12px;
    overflow-x: auto;
    padding: 5px 10px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 9px;
    background: rgb(255 255 255 / 96%);
    box-shadow: 0 6px 18px rgb(24 50 82 / 5%);
    scrollbar-width: thin;
  }

  .workbench-config-toolbar__title {
    flex: 0 0 auto;
    color: var(--el-text-color-primary);
    font-size: 16px;
    white-space: nowrap;
  }

  .workbench-config-toolbar__activity {
    display: flex;
    min-width: 280px;
    flex: 1 1 420px;
    align-items: center;
    justify-content: center;
    gap: 8px;
    color: var(--el-text-color-regular);
    font-size: 13px;
    white-space: nowrap;
  }

  .workbench-config-toolbar__activity :deep(.el-select) {
    width: min(360px, 34vw);
    min-width: 220px;
  }

  .workbench-config-toolbar__actions {
    display: flex;
    min-width: max-content;
    flex: 0 0 auto;
    align-items: center;
    gap: 6px;
    margin-left: auto;
  }

  .workbench-config-toolbar__actions :deep(.el-button + .el-button) {
    margin-left: 0;
  }

  .workbench-config-selectors {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    min-width: 0;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    background: rgb(255 255 255 / 96%);
  }

  .workbench-config-selector-group {
    min-width: 0;
    padding: 4px 9px 5px;
  }

  .workbench-config-selector-group + .workbench-config-selector-group {
    border-left: 1px solid var(--el-border-color-lighter);
  }

  .workbench-config-selector-group__header {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr) auto;
    min-height: 27px;
    min-width: 0;
    align-items: center;
    gap: 10px;
  }

  .workbench-config-selector-group__header > span:first-child {
    color: var(--el-text-color-primary);
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
  }

  .workbench-config-selector-group__current {
    overflow: hidden;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .workbench-config-selector-group__current strong {
    color: var(--el-color-primary);
    font-weight: 600;
  }

  .workbench-config-selector-group__header :deep(.el-button) {
    min-height: 24px;
    padding-inline: 4px;
    font-size: 12px;
  }

  .workbench-config-selector-options {
    display: flex;
    min-width: 0;
    min-height: 31px;
    align-items: center;
    gap: 5px;
    overflow-x: auto;
    padding: 3px 0 1px;
    scrollbar-width: thin;
  }

  .workbench-config-selector-options button {
    display: inline-flex;
    min-height: 27px;
    flex: 0 0 auto;
    align-items: center;
    justify-content: center;
    gap: 4px;
    padding: 3px 9px;
    border: 1px solid transparent;
    border-radius: 5px;
    color: var(--el-text-color-regular);
    font-size: 12px;
    line-height: 18px;
    white-space: nowrap;
    background: var(--el-fill-color-light);
    cursor: pointer;
    transition:
      color 0.15s ease,
      border-color 0.15s ease,
      background 0.15s ease;
  }

  .workbench-config-selector-options button:hover:not(:disabled) {
    border-color: var(--el-color-primary-light-6);
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }

  .workbench-config-selector-options button.is-active {
    border-color: var(--el-color-primary-light-5);
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    font-weight: 600;
  }

  .workbench-config-selector-options button:focus-visible {
    outline: 2px solid var(--el-color-primary-light-5);
    outline-offset: 1px;
  }

  .workbench-config-selector-options button:disabled {
    border-color: var(--el-border-color-lighter);
    color: var(--el-text-color-placeholder);
    background: var(--el-fill-color-lighter);
    cursor: not-allowed;
  }

  .workbench-config-save-state {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    margin-right: 4px;
    color: var(--el-color-success);
    font-size: 12px;
    white-space: nowrap;
  }

  .workbench-config-save-state.is-dirty {
    color: var(--el-color-warning);
  }

  .workbench-config-nav__dirty {
    color: var(--el-color-warning);
  }

  .workbench-config-main {
    min-width: 0;
  }

  .workbench-config-panel {
    border-radius: 10px;
  }

  .workbench-config-panel :deep(.property-panel) {
    top: var(--workbench-config-sticky-offset);
  }

  .workbench-style-section,
  .workbench-public-editor {
    min-width: 0;
  }

  .workbench-public-editor > .panel-header {
    margin-bottom: 12px;
  }

  .business-config-tabs :deep(.el-tabs__header) {
    margin-bottom: 10px;
  }

  .business-config-tabs :deep(.el-tabs__item) {
    height: 36px;
    padding: 0 18px;
    font-weight: 700;
  }

  .business-config-tab-label {
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .workbench-config-subnav {
    display: inline-flex;
    gap: 4px;
    margin-bottom: 10px;
    padding: 4px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    background: var(--el-bg-color);
  }

  .workbench-config-subnav button {
    padding: 7px 14px;
    border: 0;
    border-radius: 6px;
    color: var(--el-text-color-secondary);
    background: transparent;
    cursor: pointer;
  }

  .workbench-config-subnav button.is-active {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    font-weight: 600;
  }

  .home-layout-designer {
    display: grid;
    grid-template-columns: minmax(210px, 250px) minmax(480px, 1fr) minmax(270px, 320px);
    align-items: start;
    gap: 12px;
  }

  .home-layout-designer > :deep(.el-card) {
    border-radius: 10px;
  }

  .home-layout-designer__library,
  .home-layout-designer__properties {
    position: sticky;
    top: var(--workbench-config-sticky-offset);
    max-height: calc(100vh - var(--workbench-config-sticky-offset) - 20px);
    overflow: auto;
  }

  .home-layout-designer__canvas {
    min-width: 0;
  }

  .layout-canvas {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 10px;
  }

  .layout-canvas__item {
    grid-column: span 6;
    min-width: 0;
    padding: 13px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    background: var(--el-fill-color-extra-light);
    cursor: pointer;
    transition:
      border-color 0.16s ease,
      box-shadow 0.16s ease,
      opacity 0.16s ease;
  }

  .layout-canvas__item.is-width-1-2 {
    grid-column: span 3;
  }

  .layout-canvas__item.is-width-1-3 {
    grid-column: span 2;
  }

  .layout-canvas__item.is-width-2-3 {
    grid-column: span 4;
  }

  .layout-canvas__item:hover,
  .layout-canvas__item.is-selected {
    border-color: var(--el-color-primary);
    box-shadow: 0 0 0 1px var(--el-color-primary-light-5);
  }

  .layout-canvas__item.is-hidden {
    opacity: 0.58;
  }

  .layout-canvas__head {
    display: grid;
    grid-template-columns: 24px minmax(0, 1fr) auto;
    align-items: center;
    gap: 8px;
  }

  .layout-canvas__head > div {
    display: grid;
    gap: 2px;
    min-width: 0;
  }

  .layout-canvas__head small {
    overflow: hidden;
    color: var(--el-text-color-secondary);
    font-size: 11px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .layout-canvas__preview {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 7px;
    margin-top: 12px;
  }

  .layout-canvas__preview span {
    height: 38px;
    border-radius: 5px;
    background: linear-gradient(135deg, var(--el-color-primary-light-9), var(--el-fill-color-light));
  }

  .layout-draft-tip {
    margin-top: 12px;
  }

  .selected-component-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 14px;
  }

  .home-layout-designer__properties .el-button + .el-button {
    margin-top: 9px;
    margin-left: 0;
  }

  .migration-panel {
    min-height: 280px;
  }

  .migration-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-top: 18px;
  }

  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .panel-header span {
    font-weight: 700;
  }

  .panel-header small {
    color: #909399;
  }

  .workbench-config-tabs {
    padding: 0 16px;
    border: 1px solid #e5edf5;
    border-radius: 8px;
    background: #ffffff;
  }

  .style-actions {
    display: inline-flex;
    align-items: center;
    gap: 8px;
  }

  .style-config-row {
    align-items: stretch;
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

  .school-type-replacements {
    width: 100%;
    display: grid;
    gap: 8px;
  }

  .school-type-replacements__row {
    display: grid;
    grid-template-columns: minmax(120px, 1fr) auto minmax(140px, 1fr) auto;
    align-items: center;
    gap: 8px;
  }

  .school-type-replacements__row span {
    color: #64748b;
    font-size: 13px;
    white-space: nowrap;
  }

  .stage-layout-config :deep(.el-input-number) {
    width: 100%;
  }

  .stage-config-preview {
    min-height: 100%;
    padding: 16px;
    border: 1px solid #d8e6f5;
    border-radius: 8px;
    background: #f5f9ff;
    overflow: auto;
  }

  .stage-config-preview__top {
    min-width: 640px;
    display: grid;
    grid-auto-rows: var(--stage-preview-row-height, 220px);
    grid-template-columns:
      minmax(170px, var(--stage-preview-notice-ratio, 0.9fr))
      minmax(220px, var(--stage-preview-activity-ratio, 1.4fr))
      var(--stage-preview-calendar-width, 190px);
    gap: var(--stage-preview-gap, 18px);
    align-items: stretch;
  }

  .stage-config-preview__notice,
  .stage-config-preview__activity {
    height: var(--stage-preview-top-height, 220px);
    min-height: var(--stage-preview-top-height, 220px);
    max-height: var(--stage-preview-top-height, 220px);
    padding: 16px;
    border: 1px solid #d8e6f5;
    border-radius: 8px;
    background: #ffffff;
    overflow: hidden;
  }

  .stage-config-preview__notice span,
  .stage-config-preview__activity span {
    display: block;
    color: #2563eb;
    font-size: 12px;
    font-weight: 900;
  }

  .stage-config-preview__notice strong,
  .stage-config-preview__activity strong {
    display: block;
    margin-top: 10px;
    color: #0f2f5f;
    font-size: 18px;
    line-height: 1.35;
  }

  .stage-config-preview__notice p {
    margin: 12px 0 0;
    color: #40566f;
    line-height: 1.7;
  }

  .stage-config-help {
    margin-top: 6px;
    color: #8a96a8;
    font-size: 12px;
    line-height: 1.5;
  }

  .stage-config-preview__quota {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    margin-top: 12px;
    padding: 8px 9px;
    border: 1px solid;
    line-height: 1.45;
  }

  .stage-config-preview__quota span,
  .stage-config-preview__quota strong {
    display: inline-flex;
    align-items: baseline;
    min-width: 0;
    color: inherit;
    font-size: inherit;
    line-height: inherit;
    white-space: nowrap;
  }

  .stage-config-preview__quota strong {
    color: var(--stage-preview-quota-number-color, #1d4ed8);
    font-weight: 900;
  }

  .stage-config-preview__ratios {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 10px;
    padding: 8px 9px;
    border: 1px solid;
    line-height: 1.45;
  }

  .stage-config-preview__ratios span {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    max-width: 100%;
    padding: 3px 8px;
    border: 1px solid rgba(148, 163, 184, 0.25);
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.72);
  }

  .stage-config-preview__ratios em {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-style: normal;
  }

  .stage-config-preview__ratios strong {
    color: var(--stage-preview-ratio-number-color, #2563eb);
    font-weight: 900;
  }

  .stage-config-preview__ratios small {
    color: #64748b;
    font-size: 1em;
  }

  .stage-config-preview__activity {
    display: grid;
    place-items: center;
    align-content: center;
    text-align: center;
    background: #f8fbff;
  }

  .stage-config-preview__calendar {
    width: var(--stage-preview-calendar-width, 190px);
    height: var(--stage-preview-calendar-height, 220px);
    min-height: var(--stage-preview-calendar-height, 220px);
    max-height: var(--stage-preview-calendar-height, 220px);
    padding: 18px;
    display: grid;
    place-items: center;
    align-content: center;
    gap: 7px;
    box-sizing: border-box;
    border-radius: var(--stage-preview-calendar-radius, 8px);
    color: var(--stage-preview-calendar-color, #ffffff);
    background: var(--stage-preview-calendar-bg, linear-gradient(180deg, #2563eb 0%, #0f62b9 100%));
    overflow: hidden;
  }

  .stage-config-preview__calendar div {
    font-size: var(--stage-preview-month-size, 14px);
    font-weight: 800;
  }

  .stage-config-preview__calendar strong {
    font-size: var(--stage-preview-day-size, 54px);
    line-height: 1;
    font-weight: 950;
  }

  .stage-config-preview__calendar span {
    font-size: var(--stage-preview-weekday-size, 17px);
    font-weight: 900;
  }

  .stage-config-preview__calendar small {
    padding: 5px 9px;
    border-radius: 999px;
    background: var(--stage-preview-time-bg, rgba(255, 255, 255, 0.18));
    font-size: var(--stage-preview-time-size, 12px);
  }
}

.config-import-preview {
  display: grid;
  gap: 14px;
}

.config-diff-value {
  display: -webkit-box;
  overflow: hidden;
  color: var(--el-text-color-regular);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  overflow-wrap: anywhere;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}

.category-visual-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 360px);
  gap: 18px;
  align-items: start;
}

.category-visual-form {
  min-width: 0;
}

.category-visual-upload {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.category-visual-upload__name {
  min-width: 0;
  max-width: 240px;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-visual-footer {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.category-visual-preview-shell {
  position: sticky;
  top: 12px;
  min-width: 0;
  padding: 10px;
  border: 1px solid #d8e6f5;
  border-radius: 8px;
  background: #f5f9ff;
}

.category-visual-preview.stage-notice-stat {
  position: relative;
  width: 100%;
  min-height: 118px;
  padding: 16px 14px 14px 18px;
  display: grid;
  grid-template-rows: auto auto auto;
  align-content: center;
  gap: 8px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  box-shadow: none;
  text-align: left;
  transition: none;
  overflow: hidden;
}

.category-visual-preview .stage-notice-stat__pattern,
.category-visual-preview .stage-notice-stat__glass {
  position: absolute;
  pointer-events: none;
}

.category-visual-preview .stage-notice-stat__pattern {
  z-index: 0;
  color: #2563eb;
  transform: scale(var(--stage-stat-pattern-scale, 1.75));
  transform-origin: center;
  transition: transform 0.18s ease;
}

.category-visual-preview .stage-notice-stat__pattern::before,
.category-visual-preview .stage-notice-stat__pattern::after {
  position: absolute;
  inset: 0;
  content: '';
}

.category-visual-preview.stage-notice-stat__pattern-position--center .stage-notice-stat__pattern {
  inset: -24%;
}

.category-visual-preview.stage-notice-stat__pattern-position--right .stage-notice-stat__pattern {
  top: -28%;
  right: -42%;
  width: 92%;
  height: 156%;
}

.category-visual-preview.stage-notice-stat__pattern-position--left .stage-notice-stat__pattern {
  top: -28%;
  left: -42%;
  width: 92%;
  height: 156%;
}

.category-visual-preview.stage-notice-stat__pattern-position--bottom .stage-notice-stat__pattern {
  left: -18%;
  right: -18%;
  bottom: -62%;
  height: 138%;
}

.category-visual-preview.stage-notice-stat__pattern-mode--image .stage-notice-stat__pattern {
  background-color: transparent;
}

.category-visual-preview.stage-notice-stat__pattern-mode--image .stage-notice-stat__pattern::before,
.category-visual-preview.stage-notice-stat__pattern-mode--image .stage-notice-stat__pattern::after {
  display: none;
}

.category-visual-preview.stage-notice-stat__pattern-mode--icon .stage-notice-stat__pattern::before,
.category-visual-preview.stage-notice-stat__pattern-mode--icon .stage-notice-stat__pattern::after {
  display: none;
}

.category-visual-preview.stage-notice-stat__pattern-mode--text .stage-notice-stat__pattern::before,
.category-visual-preview.stage-notice-stat__pattern-mode--text .stage-notice-stat__pattern::after {
  display: none;
}

.category-visual-preview .stage-notice-stat__pattern :deep(.stage-notice-stat__svg-icon) {
  width: 100%;
  height: 100%;
  color: currentColor;
  fill: currentColor;
}

.category-visual-preview .stage-notice-stat__text-symbol {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  overflow: hidden;
  color: currentColor;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
  font-size: 64px;
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0;
  white-space: nowrap;
}

.category-visual-preview.stage-notice-stat__pattern--softFlow .stage-notice-stat__pattern::before {
  background:
    radial-gradient(ellipse at 18% 28%, currentColor 0 13%, transparent 14%),
    radial-gradient(ellipse at 80% 76%, currentColor 0 16%, transparent 17%),
    linear-gradient(126deg, transparent 0 34%, currentColor 35% 42%, transparent 43% 100%);
  filter: blur(1.2px);
}

.category-visual-preview.stage-notice-stat__pattern--softFlow .stage-notice-stat__pattern::after {
  background:
    linear-gradient(112deg, transparent 0 32%, currentColor 33% 36%, transparent 37% 100%),
    linear-gradient(138deg, transparent 0 58%, currentColor 59% 61%, transparent 62% 100%);
  filter: blur(0.8px);
  opacity: 0.48;
}

.category-visual-preview.stage-notice-stat__pattern--stageBeam .stage-notice-stat__pattern::before {
  background:
    radial-gradient(circle at 50% 64%, currentColor 0 8%, transparent 9%),
    conic-gradient(
      from 205deg at 50% 78%,
      transparent 0 16deg,
      currentColor 18deg 26deg,
      transparent 28deg 72deg,
      currentColor 74deg 82deg,
      transparent 84deg
    );
}

.category-visual-preview.stage-notice-stat__pattern--stageBeam .stage-notice-stat__pattern::after {
  background:
    linear-gradient(68deg, transparent 0 44%, currentColor 45% 48%, transparent 49%),
    linear-gradient(112deg, transparent 0 44%, currentColor 45% 48%, transparent 49%);
  opacity: 0.62;
}

.category-visual-preview.stage-notice-stat__pattern--geoGrid .stage-notice-stat__pattern::before {
  background: linear-gradient(90deg, currentColor 1px, transparent 1px), linear-gradient(0deg, currentColor 1px, transparent 1px);
  background-size: 26px 26px;
}

.category-visual-preview.stage-notice-stat__pattern--geoGrid .stage-notice-stat__pattern::after {
  background:
    radial-gradient(circle at 28% 32%, currentColor 0 7%, transparent 8%), radial-gradient(circle at 72% 36%, currentColor 0 6%, transparent 7%),
    radial-gradient(circle at 58% 72%, currentColor 0 8%, transparent 9%);
}

.category-visual-preview.stage-notice-stat__pattern--inkPaper .stage-notice-stat__pattern::before {
  background:
    radial-gradient(ellipse at 32% 38%, currentColor 0 15%, transparent 16%),
    radial-gradient(ellipse at 62% 62%, currentColor 0 11%, transparent 12%),
    linear-gradient(118deg, transparent 0 42%, currentColor 43% 50%, transparent 51%);
  filter: blur(1.2px);
}

.category-visual-preview.stage-notice-stat__pattern--inkPaper .stage-notice-stat__pattern::after {
  background: radial-gradient(ellipse at 76% 30%, currentColor 0 7%, transparent 8%), linear-gradient(90deg, currentColor 1px, transparent 1px);
  background-size:
    100% 100%,
    18px 18px;
  opacity: 0.4;
}

.category-visual-preview.stage-notice-stat__pattern--medalRelief .stage-notice-stat__pattern::before {
  background:
    conic-gradient(from 18deg, currentColor 0 10deg, transparent 10deg 26deg, currentColor 26deg 36deg, transparent 36deg 52deg),
    radial-gradient(circle at 50% 50%, transparent 0 24%, currentColor 25% 31%, transparent 32%);
}

.category-visual-preview.stage-notice-stat__pattern--medalRelief .stage-notice-stat__pattern::after {
  top: 43%;
  background:
    linear-gradient(72deg, transparent 0 45%, currentColor 46% 49%, transparent 50%),
    linear-gradient(108deg, transparent 0 45%, currentColor 46% 49%, transparent 50%);
}

.category-visual-preview.stage-notice-stat__pattern--calligraphyMark .stage-notice-stat__pattern::before {
  border: 10px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  transform: rotate(-24deg);
}

.category-visual-preview.stage-notice-stat__pattern--calligraphyMark .stage-notice-stat__pattern::after {
  background:
    linear-gradient(118deg, transparent 0 42%, currentColor 43% 54%, transparent 55%),
    radial-gradient(ellipse at 66% 72%, currentColor 0 13%, transparent 14%);
  filter: blur(0.6px);
}

.category-visual-preview.stage-notice-stat__pattern--pureGlass .stage-notice-stat__pattern::before {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.42), rgba(255, 255, 255, 0.1) 48%, transparent 70%),
    repeating-linear-gradient(135deg, currentColor 0 1px, transparent 1px 18px);
  filter: blur(0.2px);
}

.category-visual-preview.stage-notice-stat__pattern--pureGlass .stage-notice-stat__pattern::after {
  display: none;
}

.category-visual-preview.stage-notice-stat__pattern--performance .stage-notice-stat__pattern::before {
  background:
    radial-gradient(ellipse at 50% 82%, transparent 35%, currentColor 36% 39%, transparent 40%),
    radial-gradient(circle at 50% 68%, currentColor 0 8%, transparent 9%),
    linear-gradient(63deg, transparent 0 43%, currentColor 44% 47%, transparent 48% 100%),
    linear-gradient(117deg, transparent 0 43%, currentColor 44% 47%, transparent 48% 100%);
}

.category-visual-preview.stage-notice-stat__pattern--performance .stage-notice-stat__pattern::after {
  background:
    radial-gradient(circle at 30% 25%, currentColor 0 4%, transparent 5%), radial-gradient(circle at 70% 25%, currentColor 0 4%, transparent 5%),
    linear-gradient(90deg, transparent 0 18%, currentColor 19% 21%, transparent 22% 78%, currentColor 79% 81%, transparent 82%);
  transform: rotate(-8deg);
}

.category-visual-preview.stage-notice-stat__pattern--artwork .stage-notice-stat__pattern::before {
  background:
    linear-gradient(135deg, transparent 0 26%, currentColor 27% 34%, transparent 35% 100%),
    linear-gradient(25deg, transparent 0 45%, currentColor 46% 53%, transparent 54% 100%),
    radial-gradient(ellipse at 64% 35%, currentColor 0 13%, transparent 14%);
}

.category-visual-preview.stage-notice-stat__pattern--artwork .stage-notice-stat__pattern::after {
  background:
    radial-gradient(ellipse at 34% 72%, currentColor 0 9%, transparent 10%), radial-gradient(ellipse at 48% 58%, currentColor 0 7%, transparent 8%),
    radial-gradient(ellipse at 58% 73%, currentColor 0 10%, transparent 11%);
  filter: blur(1px);
}

.category-visual-preview.stage-notice-stat__pattern--workshop .stage-notice-stat__pattern::before {
  background: linear-gradient(90deg, currentColor 1px, transparent 1px), linear-gradient(0deg, currentColor 1px, transparent 1px);
  background-size: 28px 28px;
}

.category-visual-preview.stage-notice-stat__pattern--workshop .stage-notice-stat__pattern::after {
  background:
    radial-gradient(circle at 28% 32%, currentColor 0 8%, transparent 9%), radial-gradient(circle at 72% 36%, currentColor 0 7%, transparent 8%),
    radial-gradient(circle at 58% 72%, currentColor 0 9%, transparent 10%),
    linear-gradient(42deg, transparent 0 47%, currentColor 48% 50%, transparent 51% 100%);
}

.category-visual-preview.stage-notice-stat__pattern--achievement .stage-notice-stat__pattern::before {
  background:
    conic-gradient(from 18deg, currentColor 0 10deg, transparent 10deg 26deg, currentColor 26deg 36deg, transparent 36deg 52deg),
    radial-gradient(circle at 50% 50%, transparent 0 24%, currentColor 25% 31%, transparent 32%);
}

.category-visual-preview.stage-notice-stat__pattern--achievement .stage-notice-stat__pattern::after {
  top: 43%;
  background:
    linear-gradient(72deg, transparent 0 45%, currentColor 46% 49%, transparent 50% 100%),
    linear-gradient(108deg, transparent 0 45%, currentColor 46% 49%, transparent 50% 100%);
}

.category-visual-preview.stage-notice-stat__pattern--calligraphy .stage-notice-stat__pattern::before {
  border: 10px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  transform: rotate(-24deg);
}

.category-visual-preview.stage-notice-stat__pattern--calligraphy .stage-notice-stat__pattern::after {
  background:
    linear-gradient(118deg, transparent 0 42%, currentColor 43% 54%, transparent 55% 100%),
    radial-gradient(ellipse at 66% 72%, currentColor 0 13%, transparent 14%);
  filter: blur(0.6px);
}

.category-visual-preview.stage-notice-stat__pattern--school .stage-notice-stat__pattern::before {
  background:
    linear-gradient(
      90deg,
      transparent 0 12%,
      currentColor 13% 18%,
      transparent 19% 31%,
      currentColor 32% 37%,
      transparent 38% 50%,
      currentColor 51% 56%,
      transparent 57% 69%,
      currentColor 70% 75%,
      transparent 76%
    ),
    linear-gradient(0deg, currentColor 0 7%, transparent 8% 100%), linear-gradient(135deg, transparent 0 44%, currentColor 45% 55%, transparent 56%);
}

.category-visual-preview.stage-notice-stat__pattern--review .stage-notice-stat__pattern::before {
  background:
    radial-gradient(circle at 30% 30%, currentColor 0 9%, transparent 10%), radial-gradient(circle at 70% 70%, currentColor 0 9%, transparent 10%),
    linear-gradient(45deg, transparent 0 45%, currentColor 46% 51%, transparent 52% 100%);
}

.category-visual-preview.stage-notice-stat__pattern--review .stage-notice-stat__pattern::after {
  border: 8px solid currentColor;
  border-left: 0;
  border-bottom: 0;
  transform: rotate(45deg) scale(0.64);
}

.category-visual-preview.stage-notice-stat__pattern--score .stage-notice-stat__pattern::before {
  background: linear-gradient(
    90deg,
    currentColor 0 9%,
    transparent 10% 20%,
    currentColor 21% 34%,
    transparent 35% 45%,
    currentColor 46% 64%,
    transparent 65% 75%,
    currentColor 76% 100%
  );
  clip-path: polygon(0 72%, 12% 52%, 24% 66%, 38% 28%, 52% 48%, 68% 18%, 82% 42%, 100% 12%, 100% 100%, 0 100%);
}

.category-visual-preview.stage-notice-stat__pattern--ribbon .stage-notice-stat__pattern::before {
  background:
    radial-gradient(ellipse at 28% 30%, currentColor 0 16%, transparent 17%),
    radial-gradient(ellipse at 72% 70%, currentColor 0 18%, transparent 19%),
    linear-gradient(126deg, transparent 0 38%, currentColor 39% 45%, transparent 46% 100%);
}

.category-visual-preview.stage-notice-stat__pattern--ribbon .stage-notice-stat__pattern::after {
  border: 7px solid currentColor;
  border-left-color: transparent;
  border-bottom-color: transparent;
  border-radius: 42% 58% 45% 55%;
  transform: rotate(26deg);
}

.category-visual-preview .stage-notice-stat__glass {
  inset: 0;
  z-index: 1;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.5), rgba(255, 255, 255, 0.12) 38%, rgba(255, 255, 255, 0.26) 72%),
    var(--stage-stat-glass-color, rgba(255, 255, 255, 1));
  opacity: var(--stage-stat-glass-opacity, 0.9);
  backdrop-filter: blur(var(--stage-stat-glass-blur, 28px)) saturate(1.32) contrast(1.04);
  -webkit-backdrop-filter: blur(var(--stage-stat-glass-blur, 28px)) saturate(1.32) contrast(1.04);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.76),
    inset 0 -1px 0 rgba(148, 163, 184, 0.16),
    inset 14px 0 26px rgba(255, 255, 255, 0.16);
}

.category-visual-preview .stage-notice-stat__glass::before,
.category-visual-preview .stage-notice-stat__glass::after {
  position: absolute;
  inset: 0;
  content: '';
}

.category-visual-preview .stage-notice-stat__glass::before {
  background:
    radial-gradient(circle at 22% 18%, rgba(255, 255, 255, 0.54), transparent 19%),
    linear-gradient(90deg, rgba(255, 255, 255, 0.48) 1px, transparent 1px), linear-gradient(0deg, rgba(255, 255, 255, 0.34) 1px, transparent 1px);
  background-size:
    100% 100%,
    18px 18px,
    18px 18px;
  opacity: var(--stage-stat-texture-opacity, 0.21);
}

.category-visual-preview .stage-notice-stat__glass::after {
  border: 1px solid rgba(255, 255, 255, 0.78);
  box-shadow:
    inset 0 0 24px rgba(255, 255, 255, var(--stage-stat-highlight-opacity, 0.5)),
    inset 0 -12px 22px rgba(148, 163, 184, 0.08);
  opacity: var(--stage-stat-highlight-opacity, 0.5);
}

.category-visual-preview strong {
  position: relative;
  z-index: 2;
  color: #102a43;
  font-size: 15px;
  line-height: 1.35;
}

.category-visual-preview .stage-notice-stat__accent {
  position: absolute;
  z-index: 3;
  left: 0;
  top: 14px;
  bottom: 14px;
  width: 4px;
  border-radius: 999px;
}

.category-visual-preview .stage-notice-stat__numbers {
  position: relative;
  z-index: 2;
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
  color: #40566f;
  font-size: 14px;
  line-height: 1.5;
  white-space: nowrap;
  transform-origin: left center;
  transition: transform 0.18s ease;
}

.category-visual-preview .stage-notice-stat__numbers b {
  font-size: 17px;
  font-weight: 950;
}

.category-visual-preview .stage-notice-stat__numbers em {
  color: currentColor;
  font-style: normal;
  font-weight: 800;
  opacity: 0.72;
}

.category-visual-preview .stage-notice-stat__legend {
  position: relative;
  z-index: 2;
  color: #64748b;
  font-size: 12px;
}

.category-visual-preview.is-hover-text-enabled:hover .stage-notice-stat__numbers,
.category-visual-preview.is-hover-text-enabled:focus-visible .stage-notice-stat__numbers {
  transform: scale(var(--stage-stat-hover-scale, 1.08));
}

.category-visual-preview.is-hover-pattern-enabled:hover .stage-notice-stat__pattern,
.category-visual-preview.is-hover-pattern-enabled:focus-visible .stage-notice-stat__pattern {
  transform: scale(var(--stage-stat-pattern-hover-scale, var(--stage-stat-pattern-scale, 1.75)));
}

@media (max-width: 900px) {
  .category-visual-layout {
    grid-template-columns: 1fr;
  }

  .category-visual-preview-shell {
    position: relative;
    top: auto;
  }
}

.workbench-config-page {
  .style-preview {
    min-height: 100%;
    padding: 18px;
    display: grid;
    grid-template-columns: 104px minmax(0, 1fr);
    gap: 14px;
    border: 1px solid var(--workbench-card-border, #d8e6f5);
    border-radius: 8px;
    background: var(--workbench-page-background, #f5f9ff);
    overflow: hidden;
  }

  .style-preview-sidebar {
    position: relative;
    min-height: 220px;
    padding: 12px 8px;
    display: grid;
    align-content: start;
    gap: 8px;
    border: 1px solid var(--workbench-sidebar-border, #d9e6f6);
    border-radius: 8px;
    background: var(--workbench-sidebar-bg, #f4f8ff);
    box-shadow: 1px 0 0 rgba(216, 230, 245, 0.7);
    backdrop-filter: blur(var(--workbench-sidebar-blur, 0px)) saturate(1.02);
    -webkit-backdrop-filter: blur(var(--workbench-sidebar-blur, 0px)) saturate(1.02);
    overflow: hidden;
  }

  .style-preview-sidebar::before {
    content: '';
    position: absolute;
    inset: 0;
    pointer-events: none;
    background: none;
  }

  .style-preview-sidebar__brand,
  .style-preview-sidebar__item {
    position: relative;
    z-index: 1;
  }

  .style-preview-sidebar__brand {
    padding: 0 8px 6px;
    color: var(--workbench-heading, #0f2f5f);
    font-size: 12px;
    font-weight: 900;
    letter-spacing: 0.14em;
  }

  .style-preview-sidebar__item {
    min-height: 30px;
    padding: 0 8px;
    display: flex;
    align-items: center;
    border-radius: 7px;
    background: var(--workbench-sidebar-item-bg, transparent);
    color: #263445;
    font-size: 12px;
    font-weight: 700;
    line-height: 1.25;
  }

  .style-preview-sidebar__item:not(.is-active):nth-child(3) {
    background: var(--workbench-sidebar-item-hover-bg, #e7f0ff);
  }

  .style-preview-sidebar__item.is-active {
    color: #ffffff;
    background: var(--el-color-primary, #2563eb);
  }

  .style-preview-main {
    min-width: 0;
  }

  .style-preview-card {
    min-height: 220px;
    padding: 18px;
    display: grid;
    align-content: start;
    gap: 10px;
    border: 1px solid var(--workbench-card-border, #d8e6f5);
    border-radius: 8px;
    background: var(--workbench-card-bg, #ffffff);
    box-shadow: var(--workbench-card-shadow, none);
  }

  .style-preview-card span {
    color: var(--workbench-accent, #2563eb);
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.14em;
  }

  .style-preview-card strong {
    color: var(--workbench-heading, #082f49);
    font-size: 20px;
    line-height: 1.3;
  }

  .style-preview-card p {
    margin: 0;
    color: #606f7b;
    line-height: 1.7;
  }

  .style-preview-calendar {
    width: 76px;
    height: 76px;
    margin-top: 8px;
    display: grid;
    place-items: center;
    border-radius: 8px;
    color: #ffffff;
    background: var(--workbench-calendar-bg, linear-gradient(180deg, #2563eb 0%, #0f62b9 100%));
    font-size: 34px;
    font-weight: 900;
  }

  @media (max-width: 1280px) {
    .home-layout-designer {
      grid-template-columns: minmax(210px, 240px) minmax(460px, 1fr);
    }

    .home-layout-designer__properties {
      position: relative;
      top: auto;
      grid-column: 1 / -1;
      max-height: none;
    }
  }

  @media (max-width: 900px) {
    .workbench-config-toolbar {
      padding-block: 4px;
    }

    .home-layout-designer {
      grid-template-columns: 1fr;
    }

    .home-layout-designer__library,
    .home-layout-designer__properties {
      position: relative;
      top: auto;
      max-height: none;
    }

    .layout-canvas__item {
      grid-column: 1 / -1 !important;
    }

    .category-visual-layout {
      grid-template-columns: 1fr;
    }

    .category-visual-preview-shell {
      position: relative;
      top: auto;
    }

    .style-preview {
      grid-template-columns: 1fr;
    }

    .style-preview-sidebar {
      min-height: auto;
    }

    .school-type-replacements__row {
      grid-template-columns: 1fr;
    }

    .school-type-replacements__row span {
      display: none;
    }
  }

  .component-palette {
    display: grid;
    gap: 10px;
  }

  .component-option {
    padding: 12px;
    display: flex;
    justify-content: space-between;
    gap: 12px;
    border: 1px solid #e5edf5;
    border-radius: 8px;
    background: #f8fbff;
  }

  .component-option strong,
  .component-option span {
    display: block;
  }

  .component-option span {
    margin-top: 6px;
    color: #909399;
    font-size: 12px;
  }

  .layout-list {
    display: grid;
    gap: 10px;
  }

  .layout-row {
    padding: 12px;
    display: flex;
    gap: 12px;
    border: 1px solid #e5edf5;
    border-radius: 8px;
    background: #ffffff;
  }

  .layout-row:hover {
    border-color: #2563eb;
  }

  .drag-handle {
    width: 32px;
    display: grid;
    place-items: center;
    color: #909399;
    cursor: move;
  }

  .layout-row-main {
    flex: 1;
    min-width: 0;
  }

  .layout-row-title {
    margin-bottom: 10px;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .row-actions {
    text-align: right;
  }
}
</style>
