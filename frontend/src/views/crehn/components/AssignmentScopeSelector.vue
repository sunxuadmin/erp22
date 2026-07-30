<template>
  <div class="assignment-scope-grid" :class="{ 'is-grid': layout === 'grid', 'is-single': !showSchoolScope }">
    <el-form-item label="类别范围" :prop="categoryProp" :required="categoryRequired">
      <div class="assignment-scope-field">
        <el-select
          v-model="categoryIdsModel"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          :multiple-limit="categoryMultipleLimit"
          :disabled="disabled || categoryDisabled"
          :placeholder="categoryPlaceholder"
          style="width: 100%"
        >
          <el-option v-for="item in categoryOptions" :key="item.id" :label="item.categoryName" :value="item.id!" />
        </el-select>
        <div class="assignment-scope-actions">
          <div class="assignment-scope-action-buttons">
            <el-button link type="primary" :disabled="disabled || categoryDisabled || categoryMultipleLimit === 1" @click="selectAllCategories">
              全选类别
            </el-button>
            <el-button link :disabled="disabled || categoryDisabled" @click="categoryIdsModel = []">清空</el-button>
          </div>
          <span v-if="categoryMultipleLimit === 1">编辑单条授权时只允许选择一个类别</span>
          <span v-else>批量保存后会按账号和类别生成独立分配记录</span>
        </div>
      </div>
    </el-form-item>

    <el-form-item v-if="showSchoolScope" label="学校范围">
      <div class="assignment-scope-field">
        <el-radio-group v-model="schoolScopeModeModel" :disabled="disabled">
          <el-radio-button label="all">全部学校</el-radio-button>
          <el-radio-button label="whitelist">学校白名单</el-radio-button>
          <el-radio-button label="blacklist">学校黑名单</el-radio-button>
        </el-radio-group>
        <el-select
          v-if="schoolScopeModeModel !== 'all'"
          v-model="schoolIdsModel"
          class="school-scope-select"
          multiple
          filterable
          collapse-tags
          collapse-tags-tooltip
          :disabled="disabled"
          :placeholder="schoolScopeModeModel === 'whitelist' ? '请选择允许查看的学校' : '请选择需要排除的学校'"
          style="width: 100%"
        >
          <el-option v-for="item in schoolOptions" :key="item.id" :label="schoolOptionLabel(item)" :value="item.id!" />
        </el-select>
        <div class="assignment-scope-actions">
          <span v-if="schoolScopeModeModel === 'all'">该分配不限制学校。</span>
          <span v-else-if="schoolScopeModeModel === 'whitelist'">只允许查看所选学校的项目。</span>
          <span v-else>允许查看所选学校之外的项目。</span>
        </div>
      </div>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import type { ActivityCategoryVO, AssignmentSchoolOptionVO } from '@/api/crehn/types';

type SchoolScopeMode = 'all' | 'whitelist' | 'blacklist';

const props = withDefaults(
  defineProps<{
    categoryIds?: Array<string | number>;
    categoryOptions?: ActivityCategoryVO[];
    schoolScopeMode?: SchoolScopeMode;
    schoolIds?: Array<string | number>;
    schoolOptions?: AssignmentSchoolOptionVO[];
    categoryProp?: string;
    categoryRequired?: boolean;
    categoryMultipleLimit?: number;
    disabled?: boolean;
    categoryDisabled?: boolean;
    showSchoolScope?: boolean;
    categoryPlaceholder?: string;
    layout?: 'stacked' | 'grid';
  }>(),
  {
    categoryIds: () => [],
    categoryOptions: () => [],
    schoolScopeMode: 'all',
    schoolIds: () => [],
    schoolOptions: () => [],
    categoryProp: 'categoryIds',
    categoryRequired: true,
    categoryMultipleLimit: 0,
    disabled: false,
    categoryDisabled: false,
    showSchoolScope: true,
    categoryPlaceholder: '请选择一个或多个类别',
    layout: 'stacked'
  }
);

const emit = defineEmits<{
  (event: 'update:categoryIds', value: Array<string | number>): void;
  (event: 'update:schoolScopeMode', value: SchoolScopeMode): void;
  (event: 'update:schoolIds', value: Array<string | number>): void;
}>();

const categoryIdsModel = computed({
  get: () => props.categoryIds,
  set: (value: Array<string | number>) => emit('update:categoryIds', value)
});

const schoolScopeModeModel = computed({
  get: () => props.schoolScopeMode,
  set: (value: SchoolScopeMode) => emit('update:schoolScopeMode', value)
});

const schoolIdsModel = computed({
  get: () => props.schoolIds,
  set: (value: Array<string | number>) => emit('update:schoolIds', value)
});

watch(schoolScopeModeModel, (mode) => {
  if (mode === 'all' && props.schoolIds.length) {
    emit('update:schoolIds', []);
  }
});

const selectAllCategories = () => {
  emit(
    'update:categoryIds',
    props.categoryOptions.map((item) => item.id).filter((id): id is string | number => id !== undefined && id !== null)
  );
};

const schoolOptionLabel = (item: AssignmentSchoolOptionVO) =>
  item.schoolCode ? `${item.schoolName || item.id}（${item.schoolCode}）` : String(item.schoolName || item.id || '-');
</script>

<style scoped>
.assignment-scope-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 12px 16px;
  width: 100%;
}

.assignment-scope-grid.is-grid:not(.is-single) {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.assignment-scope-grid :deep(.el-form-item) {
  min-width: 0;
  margin-bottom: 12px;
}

.assignment-scope-grid :deep(.el-form-item__label) {
  height: auto;
  padding-bottom: 8px;
  line-height: 1.4;
}

.assignment-scope-field {
  width: 100%;
}

.assignment-scope-field :deep(.el-radio-group) {
  display: flex;
  flex-wrap: nowrap;
  width: 100%;
}

.assignment-scope-field :deep(.el-radio-button) {
  flex: 1 1 0;
  min-width: 0;
}

.assignment-scope-field :deep(.el-radio-button__inner) {
  width: 100%;
  padding-right: 10px;
  padding-left: 10px;
  white-space: nowrap;
}

.school-scope-select {
  margin-top: 8px;
}

.assignment-scope-actions {
  display: flex;
  min-height: 24px;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.assignment-scope-action-buttons {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
}

.assignment-scope-actions > span {
  flex: 1 1 180px;
  min-width: 0;
}

.assignment-scope-actions :deep(.el-button) {
  margin-left: 0;
  padding: 0;
}

@media (max-width: 900px) {
  .assignment-scope-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
