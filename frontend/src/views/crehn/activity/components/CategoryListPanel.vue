<template>
  <!-- ART-OWNER: FE.ACTIVITY_CONFIG.CATEGORY_LIST -->
  <el-card shadow="hover" class="category-list-card">
    <template #header>
      <div class="category-card-header">
        <div class="category-header-actions">
          <el-button
            :disabled="!currentActivity?.id"
            plain
            :icon="activityPanelExpanded ? 'ArrowUp' : 'ArrowDown'"
            @click="emit('toggleActivityPanel')"
          >
            {{ activityPanelExpanded ? '收起活动' : '展开活动' }}
          </el-button>
          <el-button
            :disabled="!currentActivity?.id"
            :type="categoryDeleted ? 'warning' : 'default'"
            plain
            size="small"
            @click="emit('toggleDeleted')"
            >已删类</el-button
          >
          <el-button
            v-if="!categoryDeleted"
            v-hasPermi="['crehn:category:add']"
            :disabled="!currentActivity?.id || selectedActivityDeleted"
            type="success"
            plain
            icon="FolderAdd"
            @click="emit('addGroup')"
          >
            新增大类
          </el-button>
          <el-button
            v-if="!categoryDeleted"
            v-hasPermi="['crehn:category:add']"
            :disabled="!currentActivity?.id || selectedActivityDeleted"
            type="primary"
            plain
            icon="Plus"
            @click="emit('add')"
          >
            新增类别
          </el-button>
          <el-button
            v-if="!categoryDeleted"
            v-hasPermi="['crehn:category:add']"
            :disabled="!canCopyCurrentCategory || selectedActivityDeleted"
            type="primary"
            plain
            icon="CopyDocument"
            @click="emit('copy')"
          >
            复制
          </el-button>
          <el-button
            v-if="!categoryDeleted"
            v-hasPermi="['crehn:category:edit']"
            :disabled="!currentActivity?.id || !hasCategories || selectedActivityDeleted"
            type="primary"
            plain
            icon="Setting"
            @click="emit('openMenuStyle')"
          >
            菜单外观
          </el-button>
          <el-button
            :disabled="!currentActivity?.id || selectedActivityDeleted"
            :loading="configExporting"
            type="warning"
            plain
            icon="Download"
            @click="emit('exportConfig')"
          >
            导出配置
          </el-button>
          <el-button
            v-if="canImportConfig"
            :disabled="!currentActivity?.id || selectedActivityDeleted || categoryDeleted"
            :loading="configImporting"
            type="warning"
            plain
            icon="Upload"
            @click="triggerConfigImport"
          >
            导入配置
          </el-button>
          <input ref="configFileInput" class="config-file-input" type="file" accept=".json,.xlsx,.xls" @change="handleConfigFileChange" />
          <el-dropdown class="toolbar-more-menu" trigger="click" @command="handleMoreCommand">
            <el-button plain icon="MoreFilled" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :disabled="!currentActivity?.id || categoryDeleted || selectedActivityDeleted" command="add-group"
                  >新增大类</el-dropdown-item
                >
                <el-dropdown-item :disabled="!currentActivity?.id || categoryDeleted || selectedActivityDeleted" command="add"
                  >新增类别</el-dropdown-item
                >
                <el-dropdown-item :disabled="!canCopyCurrentCategory || categoryDeleted || selectedActivityDeleted" command="copy"
                  >复制</el-dropdown-item
                >
                <el-dropdown-item
                  :disabled="!currentActivity?.id || !hasCategories || categoryDeleted || selectedActivityDeleted"
                  command="menu-style"
                  >菜单外观</el-dropdown-item
                >
                <el-dropdown-item :disabled="!currentActivity?.id || selectedActivityDeleted" command="export-config">导出配置</el-dropdown-item>
                <el-dropdown-item
                  v-if="canImportConfig"
                  :disabled="!currentActivity?.id || categoryDeleted || selectedActivityDeleted"
                  command="import-config"
                  >导入配置</el-dropdown-item
                >
                <el-dropdown-item :disabled="!currentActivity?.id" command="deleted">{{
                  categoryDeleted ? '查看正常类别' : '查看已删类别'
                }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </template>
    <el-table
      border
      highlight-current-row
      :data="categoryTableData"
      row-key="id"
      default-expand-all
      :tree-props="{ children: 'children' }"
      height="100%"
      class="category-table category-table-desktop"
      @row-click="emit('select', $event)"
    >
      <el-table-column class-name="category-collapse-column" label=">" width="36" align="center">
        <template #default />
      </el-table-column>
      <el-table-column label="名称" prop="categoryName" min-width="140">
        <template #default="{ row }">
            <div class="category-name-link">{{ categoryCellName(row) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="78">
        <template #default="{ row }">
          <div class="category-type-cell" @click.stop="emit('edit', row)">
            <span class="category-type-text" :class="categoryTypeThemeClass(row)">
              {{ categoryTypeLabel(row) }}
            </span>
            <el-button class="category-type-edit-btn" :class="categoryTypeThemeClass(row)" plain size="small">编辑</el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="排序" width="130" align="center">
        <template #default="{ row }">
          <div class="sort-control-group">
            <span class="sort-control-value">{{ row.sortOrder ?? '-' }}</span>
            <el-button
              link
              class="sort-control-btn"
              type="default"
              icon="ArrowUp"
              :disabled="!canEditCategory || categoryDeleted || row.virtualGroup"
              @click.stop="emit('moveSort', row, 'up')"
            />
            <el-button
              link
              class="sort-control-btn"
              type="default"
              icon="ArrowDown"
              :disabled="!canEditCategory || categoryDeleted || row.virtualGroup"
              @click.stop="emit('moveSort', row, 'down')"
            />
          </div>
        </template>
      </el-table-column>
      <el-table-column label="菜单显示" width="78" align="center">
        <template #default="{ row }">
          <el-tooltip v-if="row.virtualGroup" content="这是旧数据形成的虚拟大类，编辑保存后即可独立控制" placement="top">
            <el-tag type="info">待配置</el-tag>
          </el-tooltip>
          <el-switch
            v-else
            :model-value="isCategoryMenuVisible(row)"
            :disabled="!canEditCategory"
            @click.stop
            @change="emitMenuVisible(row, $event)"
          />
        </template>
      </el-table-column>
      <el-table-column label="业务启用" width="78" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.virtualGroup" type="info">继承</el-tag>
          <el-switch v-else :model-value="row.enabled !== false" :disabled="!canEditCategory" @click.stop @change="emitEnabled(row, $event)" />
        </template>
      </el-table-column>
    </el-table>

    <div class="category-mobile-list">
      <article
        v-for="item in flatCategoryRows"
        :key="String(item.row.id)"
        class="category-mobile-item"
        :class="{ 'is-current': String(item.row.id) === String(currentCategory?.id) }"
        @click="emit('select', item.row)"
      >
        <div
          class="category-mobile-title"
          :style="{ paddingLeft: `${item.level * 14}px` }"
        >
          <span class="category-mobile-name">{{ item.row.categoryName || '-' }}</span>
          <el-tag :type="isCategoryGroup(item.row) ? 'warning' : 'primary'">{{ categoryTypeLabel(item.row) }}</el-tag>
        </div>
        <dl class="category-mobile-meta">
          <div>
            <dt>编号</dt>
            <dd>{{ item.row.sortOrder ?? '-' }}</dd>
          </div>
        </dl>
        <div class="category-mobile-switches" @click.stop>
          <label>
            <span>菜单显示</span>
            <el-tag v-if="item.row.virtualGroup" type="info">待配置</el-tag>
            <el-switch
              v-else
              :model-value="isCategoryMenuVisible(item.row)"
              :disabled="!canEditCategory"
              @change="emitMenuVisible(item.row, $event)"
            />
          </label>
          <label>
            <span>业务启用</span>
            <el-tag v-if="item.row.virtualGroup" type="info">继承</el-tag>
            <el-switch v-else :model-value="item.row.enabled !== false" :disabled="!canEditCategory" @change="emitEnabled(item.row, $event)" />
          </label>
        </div>
    </article>
  </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';

import type { ActivityVO } from '@/api/crehn/types';
import { isCategoryGroup, isCategoryMenuVisible, isLegacyGroupedCategory, type ActivityCategoryTreeNode } from '@/utils/artCategory';

type SwitchValue = boolean | string | number;

const props = defineProps<{
  categoryTableData: ActivityCategoryTreeNode[];
  currentActivity?: ActivityVO;
  currentCategory?: ActivityCategoryTreeNode;
  categoryDeleted: boolean;
  selectedActivityDeleted: boolean;
  activityPanelExpanded: boolean;
  canEditCategory: boolean;
  canImportConfig: boolean;
  configExporting: boolean;
  configImporting: boolean;
  hasCategories: boolean;
}>();

const emit = defineEmits<{
  toggleActivityPanel: [];
  toggleDeleted: [];
  addGroup: [];
  add: [];
  copy: [];
  openMenuStyle: [];
  exportConfig: [];
  importConfig: [file: File];
  select: [row: ActivityCategoryTreeNode];
  toggleMenuVisible: [row: ActivityCategoryTreeNode, visible: SwitchValue];
  toggleEnabled: [row: ActivityCategoryTreeNode, enabled: SwitchValue];
  edit: [row: ActivityCategoryTreeNode];
  remove: [row: ActivityCategoryTreeNode];
  restore: [row: ActivityCategoryTreeNode];
  purge: [row: ActivityCategoryTreeNode];
  moveSort: [row: ActivityCategoryTreeNode, direction: 'up' | 'down'];
}>();

const canCopyCurrentCategory = computed(() => !!props.currentCategory?.id && !props.currentCategory?.virtualGroup);
const configFileInput = ref<HTMLInputElement>();
const categoryTypeLabel = (row: ActivityCategoryTreeNode) =>
  isCategoryGroup(row) ? '大类' : (row.parentId && String(row.parentId) !== '0') || isLegacyGroupedCategory(row) ? '子类' : '一级';
const categoryCellName = (row: ActivityCategoryTreeNode) => row.categoryName || '-';
const categoryTypeThemeClass = (row: ActivityCategoryTreeNode) => (isCategoryGroup(row) ? 'is-warning' : 'is-primary');
const flatCategoryRows = computed(() => {
  const result: Array<{ row: ActivityCategoryTreeNode; level: number }> = [];
  const appendRows = (rows: ActivityCategoryTreeNode[], level: number) => {
    rows.forEach((row) => {
      result.push({ row, level });
      if (row.children?.length) appendRows(row.children, level + 1);
    });
  };
  appendRows(props.categoryTableData, 0);
  return result;
});

const emitMenuVisible = (row: ActivityCategoryTreeNode, visible: SwitchValue) => emit('toggleMenuVisible', row, visible);
const emitEnabled = (row: ActivityCategoryTreeNode, enabled: SwitchValue) => emit('toggleEnabled', row, enabled);

const triggerConfigImport = () => {
  if (!props.canImportConfig || !props.currentActivity?.id || props.selectedActivityDeleted || props.categoryDeleted) return;
  configFileInput.value?.click();
};

const handleConfigFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (file) emit('importConfig', file);
};

const handleMoreCommand = (command: string | number | object) => {
  if (command === 'add-group') {
    emit('addGroup');
    return;
  }
  if (command === 'add') {
    emit('add');
    return;
  }
  if (command === 'copy') {
    emit('copy');
    return;
  }
  if (command === 'menu-style') {
    emit('openMenuStyle');
    return;
  }
  if (command === 'export-config') {
    emit('exportConfig');
    return;
  }
  if (command === 'import-config') {
    triggerConfigImport();
    return;
  }
  if (command === 'deleted') {
    emit('toggleDeleted');
  }
};
</script>

<style scoped>
.category-list-card {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  margin-top: 10px;
  overflow: hidden;
}

.category-list-card :deep(.el-card__body) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.category-list-card :deep(.el-card__header) {
  flex: 0 0 auto;
  padding-bottom: 12px;
}

.category-table {
  flex: 1 1 auto;
  min-height: 0;
  width: 100%;
}

.category-name-link {
  cursor: default;
  display: block;
  width: 100%;
  height: auto;
  min-height: 24px;
  padding: 3px 0;
  border-radius: 4px;
  color: var(--el-text-color-primary);
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.category-name-link:hover {
  color: var(--el-text-color-primary);
}

.category-mobile-name {
  display: inline-block;
  max-width: calc(100% - 58px);
  min-height: 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0;
}

.category-mobile-title {
  cursor: default;
}

.category-type-cell {
  display: inline-flex;
  flex-direction: column;
  align-items: stretch;
  gap: 3px;
  padding: 2px 0;
  cursor: pointer;
  width: 100%;
}

.category-type-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.2;
  width: 100%;
  box-sizing: border-box;
  white-space: nowrap;
  background: var(--el-color-white);
}

.category-type-text.is-primary,
.category-type-edit-btn.is-primary {
  --el-color: var(--el-color-primary);
  color: var(--el-color-primary);
  border-color: var(--el-color-primary-light-3);
}

.category-type-text.is-warning,
.category-type-edit-btn.is-warning {
  --el-color: var(--el-color-warning);
  color: var(--el-color-warning);
  border-color: var(--el-color-warning-light-3);
}

.category-type-edit-btn {
  width: 100%;
  min-width: auto;
  height: 24px;
  padding: 0 8px;
  margin: 0;
  font-size: 12px;
  line-height: 1.2;
  border-radius: 4px;
}

.category-collapse-column :deep(.cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
}

.category-collapse-column :deep(.el-table__header-cell) {
  background: var(--el-fill-color-light);
}

.category-collapse-column :deep(.el-table__header-wrapper .cell) {
  background: var(--el-fill-color-light);
}

.category-collapse-column :deep(.el-table__expand-icon) {
  margin: 0 auto;
}

.sort-control-group {
  display: inline-flex;
  align-items: stretch;
  justify-content: center;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;
  background: var(--el-bg-color);
}

.sort-control-value {
  min-width: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  font-variant-numeric: tabular-nums;
  border-right: 1px solid var(--el-border-color-light);
}

.sort-control-btn {
  height: 24px;
  min-height: 24px;
  min-width: 28px;
  padding: 0 6px;
  min-width: auto;
  margin: 0;
  border-radius: 0;
}

.sort-control-btn + .sort-control-btn {
  border-left: 1px solid var(--el-border-color-light);
}

.category-mobile-list {
  display: none;
}

.category-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  width: 100%;
}

.category-header-actions {
  display: flex;
  flex: 1 1 auto;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  min-width: 0;
}

.category-header-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.config-file-input {
  display: none;
}

.toolbar-more-menu {
  display: none;
}

@media (max-width: 1440px) {
  .category-card-header {
    align-items: flex-start;
  }

  .category-header-actions {
    justify-content: flex-start;
  }

  .toolbar-more-menu {
    display: inline-flex;
  }
}

@media (max-width: 1199px) {
  .category-list-card {
    min-height: 420px;
  }
}

@media (min-width: 1200px) and (max-width: 1600px) {
  .category-list-card {
    min-height: 420px;
    flex: 0 0 420px;
  }
}

@media (max-width: 768px) {
  .category-card-header {
    flex-direction: column;
  }

  .category-header-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .category-list-card {
    min-height: 360px;
  }

  .category-table-desktop {
    display: none;
  }

  .category-mobile-list {
    display: flex;
    flex: 1 1 auto;
    flex-direction: column;
    gap: 10px;
    min-height: 0;
    overflow-y: auto;
  }

  .category-mobile-item {
    padding: 12px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;
    background: var(--el-bg-color);
  }

  .category-mobile-item.is-current {
    border-color: var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
  }

  .category-mobile-title {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 10px;
    font-weight: 700;
  }

  .category-mobile-title > span,
  .category-mobile-meta dd {
    min-width: 0;
    overflow-wrap: anywhere;
    word-break: break-word;
  }

  .category-mobile-meta {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 72px;
    gap: 8px 16px;
    margin: 10px 0 0;
  }

  .category-mobile-meta > div {
    min-width: 0;
  }

  .category-mobile-meta dt {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  .category-mobile-meta dd {
    margin: 3px 0 0;
  }

  .category-mobile-switches {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
    margin-top: 10px;
  }

  .category-mobile-switches label {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    min-width: 0;
    padding: 8px;
    border-radius: 4px;
    background: var(--el-fill-color-light);
  }

}
</style>
