<template>
  <el-card shadow="hover" class="activity-list-card">
    <template #header>
      <div class="activity-header-toolbar">
        <div class="activity-header-actions">
          <el-button v-hasPermi="['crehn:activity:add']" type="primary" plain icon="Plus" @click="emit('add')">新增活动</el-button>
          <el-button
            class="toolbar-secondary-action"
            v-hasPermi="['crehn:activity:add']"
            type="success"
            plain
            icon="Upload"
            :loading="importing"
            @click="triggerImport"
          >
            导入配置
          </el-button>
          <el-dropdown class="toolbar-secondary-action" :disabled="!currentActivity?.id || selectedActivityDeleted" @command="handleExportCommand">
            <el-button type="warning" plain icon="Download" :disabled="!currentActivity?.id || selectedActivityDeleted">导出配置</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="json">导出 JSON</el-dropdown-item>
                <el-dropdown-item command="excel">导出 Excel</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="activity-header-tools">
          <el-button :type="activityDeleted ? 'warning' : 'default'" plain size="small" @click="emit('toggleDeleted')">已删活动</el-button>
          <el-dropdown class="toolbar-more-menu" trigger="click" @command="handleMoreCommand">
            <el-button plain icon="MoreFilled" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="import">导入配置</el-dropdown-item>
                <el-dropdown-item :disabled="!currentActivity?.id || selectedActivityDeleted" command="export-json">导出 JSON</el-dropdown-item>
                <el-dropdown-item :disabled="!currentActivity?.id || selectedActivityDeleted" command="export-excel">导出 Excel</el-dropdown-item>
                <el-dropdown-item command="deleted">{{ activityDeleted ? '查看正常活动' : '查看已删活动' }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <right-toolbar v-model:show-search="showSearchModel" @query-table="emit('refresh')" />
        </div>
      </div>
    </template>
    <el-table
      v-loading="loading"
      border
      highlight-current-row
      :data="activityList"
      row-key="id"
      :current-row-key="currentActivity?.id"
      height="100%"
      class="activity-table"
      @row-click="emit('select', $event)"
    >
      <el-table-column label="活动名称" prop="activityName" min-width="180" show-overflow-tooltip />
      <el-table-column label="菜单简称" prop="menuName" width="140" show-overflow-tooltip />
      <el-table-column label="报名时间" min-width="190">
        <template #default="scope">{{ scope.row.signupStartAt || '-' }} 至 {{ scope.row.signupEndAt || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="90" />
      <el-table-column label="操作" width="210" fixed="left" align="center" class-name="operation-column">
        <template #default="scope">
          <template v-if="scope.row.delFlag === '1'">
            <el-button v-hasPermi="['crehn:activity:edit']" link type="success" icon="RefreshLeft" @click.stop="emit('restore', scope.row)"
              >恢复</el-button
            >
            <el-button v-hasPermi="['crehn:activity:remove']" link type="danger" icon="Delete" @click.stop="emit('purge', scope.row)"
              >永久删除</el-button
            >
          </template>
          <template v-else>
            <el-button
              link
              type="primary"
              icon="Switch"
              :disabled="String(currentActivity?.id) === String(scope.row.id)"
              @click.stop="emit('select', scope.row)"
            >
              {{ String(currentActivity?.id) === String(scope.row.id) ? '当前' : '切换' }}
            </el-button>
            <el-button v-hasPermi="['crehn:activity:edit']" link type="primary" icon="Edit" @click.stop="emit('edit', scope.row)" />
            <el-button v-hasPermi="['crehn:activity:remove']" link type="danger" icon="Delete" @click.stop="emit('remove', scope.row)" />
          </template>
        </template>
      </el-table-column>
    </el-table>
    <pagination
      v-show="total > queryParams.pageSize"
      v-model:page="pageModel"
      v-model:limit="limitModel"
      :total="total"
      @pagination="emit('paginate')"
    />
  </el-card>

  <input ref="importRef" class="activity-config-import-input" type="file" accept=".json,.xlsx,.xls" @change="handleImportChange" />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';

import type { ActivityVO } from '@/api/crehn/types';

type ActivityListQueryParams = {
  pageNum: number;
  pageSize: number;
  activityName: string;
  status: string;
};

const props = defineProps<{
  loading: boolean;
  total: number;
  queryParams: ActivityListQueryParams;
  showSearch: boolean;
  activityList: ActivityVO[];
  currentActivity?: ActivityVO;
  activityDeleted: boolean;
  selectedActivityDeleted: boolean;
  importing: boolean;
}>();

const emit = defineEmits<{
  'update:showSearch': [value: boolean];
  'update:page': [value: number];
  'update:limit': [value: number];
  add: [];
  import: [file: File];
  export: [type: 'json' | 'excel'];
  toggleDeleted: [];
  refresh: [];
  select: [row: ActivityVO];
  edit: [row: ActivityVO];
  remove: [row: ActivityVO];
  restore: [row: ActivityVO];
  purge: [row: ActivityVO];
  paginate: [];
}>();

const importRef = ref<HTMLInputElement>();
const showSearchModel = computed({
  get: () => props.showSearch,
  set: (value: boolean) => emit('update:showSearch', value)
});
const pageModel = computed({
  get: () => props.queryParams.pageNum,
  set: (value: number) => emit('update:page', value)
});
const limitModel = computed({
  get: () => props.queryParams.pageSize,
  set: (value: number) => emit('update:limit', value)
});

const triggerImport = () => {
  if (!importRef.value) return;
  importRef.value.value = '';
  importRef.value.click();
};

const handleImportChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (file) {
    emit('import', file);
  }
  input.value = '';
};

const handleExportCommand = (command: string | number | object) => {
  emit('export', command === 'excel' ? 'excel' : 'json');
};

const handleMoreCommand = (command: string | number | object) => {
  if (command === 'import') {
    triggerImport();
    return;
  }
  if (command === 'export-json') {
    emit('export', 'json');
    return;
  }
  if (command === 'export-excel') {
    emit('export', 'excel');
    return;
  }
  if (command === 'deleted') {
    emit('toggleDeleted');
  }
};
</script>

<style scoped>
.activity-header-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.activity-header-actions,
.activity-header-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.activity-header-actions {
  flex-wrap: wrap;
}

.activity-header-tools {
  flex: 0 0 auto;
}

.toolbar-more-menu {
  display: none;
}

.activity-list-card {
  display: flex;
  height: 306px;
  flex: 0 0 306px;
  flex-direction: column;
  overflow: hidden;
}

.activity-list-card :deep(.el-card__body) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.activity-table {
  flex: 1 1 auto;
  min-height: 0;
}

.activity-list-card :deep(.operation-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding-right: 6px;
  padding-left: 6px;
}

.activity-list-card :deep(.operation-column .el-button.is-link) {
  min-width: 28px;
  min-height: 28px;
  padding: 4px 6px;
}

.activity-list-card :deep(.pagination-container) {
  flex: 0 0 auto;
  margin: 8px 0 0;
  padding: 0;
}

.activity-list-card :deep(.pagination-container .el-pagination) {
  justify-content: flex-end;
  font-size: 12px;
}

.activity-list-card :deep(.pagination-container .el-pagination__jump) {
  display: none;
}

.activity-config-import-input {
  display: none;
}

@media (max-width: 1440px) {
  .activity-header-toolbar {
    align-items: flex-start;
  }

  .activity-header-tools {
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .toolbar-secondary-action {
    display: none;
  }

  .toolbar-more-menu {
    display: inline-flex;
  }

  .activity-list-card {
    height: 280px;
    flex-basis: 280px;
  }
}

@media (max-width: 1199px) {
  .activity-list-card {
    height: 300px;
    flex: 0 0 300px;
  }
}

@media (max-width: 768px) {
  .activity-header-toolbar {
    flex-direction: column;
  }

  .activity-header-actions,
  .activity-header-tools {
    width: 100%;
    justify-content: flex-start;
  }

  .activity-list-card {
    min-height: 360px;
  }
}
</style>
