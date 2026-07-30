<template>
  <!-- ART-OWNER: FE.ACTIVITY_CONFIG.FIELD_SCHEMA -->
  <div class="field-schema-tab">
    <div class="field-toolbar">
      <el-button
        :disabled="!hasCurrentCategory || categoryDeleted"
        :type="fieldDeleted ? 'warning' : 'default'"
        plain
        size="small"
        @click="emit('toggleDeleted')"
      >
        已删字段
      </el-button>
      <template v-if="!fieldDeleted">
        <el-button
          v-hasPermi="['crehn:field:add']"
          :disabled="!hasCurrentCategory || selectedActivityDeleted"
          type="primary"
          plain
          icon="Plus"
          @click="emit('add')"
        >
          新增字段
        </el-button>
        <el-button
          class="toolbar-secondary-action"
          :disabled="!hasCurrentCategory || selectedActivityDeleted"
          plain
          icon="Setting"
          @click="emit('openMemberTemplate')"
        >
          表格模板配置
        </el-button>
        <el-button
          v-hasPermi="['crehn:category:edit']"
          class="toolbar-secondary-action"
          :disabled="!hasCurrentCategory || categoryDeleted || selectedActivityDeleted"
          plain
          icon="Grid"
          @click="emit('openFormLayout')"
        >
          表单布局
        </el-button>
        <el-button
          v-hasPermi="['crehn:field:edit']"
          :disabled="!hasCurrentCategory || categoryDeleted || selectedActivityDeleted || !selectedFieldForCopy"
          plain
          class="toolbar-secondary-action"
          icon="CopyDocument"
          @click="emit('copy', selectedFieldForCopy as CategoryFieldSchemaVO)"
        >
          复制
        </el-button>
      </template>
      <el-dropdown class="toolbar-more-menu" trigger="click" @command="handleMoreCommand">
        <el-button plain icon="MoreFilled" />
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item :disabled="!hasCurrentCategory || fieldDeleted || selectedActivityDeleted" command="add">新增字段</el-dropdown-item>
            <el-dropdown-item :disabled="!hasCurrentCategory || fieldDeleted || selectedActivityDeleted" command="member"
              >表格模板配置</el-dropdown-item
            >
            <el-dropdown-item
              v-hasPermi="['crehn:field:edit']"
              :disabled="
                !hasCurrentCategory || categoryDeleted || selectedActivityDeleted || fieldDeleted || !selectedFieldForCopy || !canEditFieldSchema
              "
              command="copy"
            >
              复制
            </el-dropdown-item>
            <el-dropdown-item
              :disabled="!hasCurrentCategory || categoryDeleted || fieldDeleted || selectedActivityDeleted || !canEditCategory"
              command="layout"
            >
              表单布局
            </el-dropdown-item>
            <el-dropdown-item :disabled="!hasCurrentCategory || categoryDeleted" command="deleted">
              {{ fieldDeleted ? '查看正常字段' : '查看已删字段' }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <el-table
      border
      :data="fieldList"
      row-key="id"
      height="100%"
      highlight-current-row
      class="config-table"
      @current-change="handleCurrentFieldChange"
    >
      <el-table-column label="字段名称" min-width="120" show-overflow-tooltip>
        <template #default="scope">
          <div
            v-if="canOpenFieldByName(scope.row)"
            class="config-name-link"
            @click.stop="emit('edit', scope.row)"
          >
            {{ scope.row.fieldLabel || '-' }}
          </div>
          <span v-else>{{ scope.row.fieldLabel || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="编号" width="110" align="center">
        <template #default="{ row }">
          <div class="sort-control-group">
            <span class="sort-control-value">{{ row.sortOrder ?? '-' }}</span>
            <el-button
              link
              type="default"
              icon="ArrowUp"
              class="sort-control-btn"
              :disabled="selectedActivityDeleted || !canEditFieldSchema || row.delFlag === '1' || fieldDeleted"
              @click.stop="emit('moveSort', row, 'up')"
            />
            <el-button
              link
              type="default"
              icon="ArrowDown"
              class="sort-control-btn"
              :disabled="selectedActivityDeleted || !canEditFieldSchema || row.delFlag === '1' || fieldDeleted"
              @click.stop="emit('moveSort', row, 'down')"
            />
          </div>
        </template>
      </el-table-column>
      <el-table-column label="类型" prop="fieldType" width="100" />
      <el-table-column label="必填" width="70">
        <template #default="scope">{{ scope.row.required ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="脱敏" width="70">
        <template #default="scope">{{ scope.row.sensitive ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="132" fixed="left" align="center" class-name="operation-column">
        <template #default="scope">
          <span v-if="selectedActivityDeleted" class="category-purge-muted">只读</span>
          <el-button
            v-else-if="scope.row.delFlag === '1' || fieldDeleted"
            v-hasPermi="['crehn:field:edit']"
            link
            type="success"
            icon="RefreshLeft"
            @click.stop="emit('restore', scope.row)"
          />
          <template v-else>
            <el-button
              v-if="scope.row.fieldType === 'teacher_group'"
              v-hasPermi="['crehn:field:edit']"
              link
              type="primary"
              icon="Setting"
              @click.stop="emit('edit', scope.row)"
            />
            <el-button v-hasPermi="['crehn:field:edit']" link type="primary" icon="Edit" @click.stop="emit('edit', scope.row)" />
            <el-button v-hasPermi="['crehn:field:remove']" link type="danger" icon="Delete" @click.stop="emit('remove', scope.row)" />
          </template>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import type { CategoryFieldSchemaVO } from '@/api/crehn/types';

const props = defineProps<{
  fieldList: CategoryFieldSchemaVO[];
  hasCurrentCategory: boolean;
  fieldDeleted: boolean;
  categoryDeleted: boolean;
  selectedActivityDeleted: boolean;
  canEditCategory: boolean;
  canEditFieldSchema: boolean;
}>();

const emit = defineEmits<{
  toggleDeleted: [];
  add: [];
  openMemberTemplate: [];
  openFormLayout: [];
  edit: [row: CategoryFieldSchemaVO];
  copy: [row: CategoryFieldSchemaVO];
  restore: [row: CategoryFieldSchemaVO];
  remove: [row: CategoryFieldSchemaVO];
  moveSort: [row: CategoryFieldSchemaVO, direction: 'up' | 'down'];
}>();

const canOpenFieldByName = (row: CategoryFieldSchemaVO) => props.canEditFieldSchema && !props.fieldDeleted && row.delFlag !== '1';
const selectedFieldForCopy = ref<CategoryFieldSchemaVO | null>(null);

const handleCurrentFieldChange = (row: CategoryFieldSchemaVO | null) => {
  selectedFieldForCopy.value = row;
};

const handleMoreCommand = (command: string | number | object) => {
  if (command === 'add') {
    emit('add');
    return;
  }
  if (command === 'member') {
    emit('openMemberTemplate');
    return;
  }
  if (command === 'copy') {
    if (selectedFieldForCopy.value) {
      emit('copy', selectedFieldForCopy.value);
    }
    return;
  }
  if (command === 'layout') {
    emit('openFormLayout');
    return;
  }
  if (command === 'deleted') {
    emit('toggleDeleted');
  }
};
</script>

<style scoped>
.field-schema-tab {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.field-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.config-table {
  flex: 1 1 auto;
  min-height: 0;
}

.field-schema-tab :deep(.operation-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding-right: 6px;
  padding-left: 6px;
}

.field-schema-tab :deep(.operation-column .el-button.is-link) {
  min-width: 28px;
  min-height: 28px;
  padding: 4px 6px;
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
  font-variant-numeric: tabular-nums;
  min-width: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0 10px;
  border-right: 1px solid var(--el-border-color-light);
}

.sort-control-btn {
  height: 24px;
  min-height: 24px;
  min-width: 28px;
  padding: 0 6px;
  margin: 0;
  border-radius: 0;
}

.sort-control-btn + .sort-control-btn {
  border-left: 1px solid var(--el-border-color-light);
}

.config-name-link {
  cursor: pointer;
  display: block;
  width: 100%;
  height: auto;
  min-height: 24px;
  max-width: 100%;
  padding: 3px 0;
  color: var(--el-color-primary);
  font-weight: 400;
  vertical-align: baseline;
  line-height: 1.4;
  overflow: hidden;
  max-width: 100%;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}

.config-name-link:hover {
  color: var(--el-color-primary-light-3);
}

.toolbar-more-menu {
  display: none;
}

@media (max-width: 1440px) {
  .toolbar-secondary-action {
    display: none;
  }

  .toolbar-more-menu {
    display: inline-flex;
  }
}
</style>
