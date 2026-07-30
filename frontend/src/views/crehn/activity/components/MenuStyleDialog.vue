<template>
  <ArtPopupDialog v-model="dialog.visible" title="菜单外观" width="980px" append-to-body>
    <el-alert
      class="mb-3"
      type="info"
      :closable="false"
      title="仅控制学校端左侧报送菜单图标和图标颜色，菜单文字颜色保持不变；保存成功后刷新页面生效。"
    />
    <el-divider content-position="left">大类菜单</el-divider>
    <el-table :data="menuGroupStyleRows" border max-height="260" class="menu-style-table">
      <el-table-column label="大类" prop="categoryGroup" min-width="180" show-overflow-tooltip />
      <el-table-column label="图标" min-width="240">
        <template #default="{ row }">
          <icon-select v-model="row.groupIcon" width="200px" />
        </template>
      </el-table-column>
      <el-table-column label="颜色" width="150">
        <template #default="{ row }">
          <el-color-picker v-model="row.groupColor" show-alpha />
        </template>
      </el-table-column>
      <el-table-column label="预览" min-width="150" align="center">
        <template #default="{ row }">
          <span class="menu-style-preview" :style="{ color: row.groupColor || defaultMenuColor }">
            <svg-icon :icon-class="previewMenuIcon(row.groupIcon)" />
            <span>{{ row.categoryGroup }}</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="resetGroupMenuStyle(row)">默认</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-divider content-position="left">类别菜单</el-divider>
    <el-table :data="menuCategoryStyleRows" border max-height="360" class="menu-style-table">
      <el-table-column label="类别" prop="categoryName" min-width="160" show-overflow-tooltip />
      <el-table-column label="所属大类" prop="categoryGroup" min-width="160" show-overflow-tooltip />
      <el-table-column label="图标" min-width="240">
        <template #default="{ row }">
          <icon-select v-model="row.categoryIcon" width="200px" />
        </template>
      </el-table-column>
      <el-table-column label="颜色" width="150">
        <template #default="{ row }">
          <el-color-picker v-model="row.categoryColor" show-alpha />
        </template>
      </el-table-column>
      <el-table-column label="预览" min-width="150" align="center">
        <template #default="{ row }">
          <span class="menu-style-preview" :style="{ color: row.categoryColor || defaultMenuColor }">
            <svg-icon :icon-class="previewMenuIcon(row.categoryIcon)" />
            <span>{{ row.categoryName }}</span>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center">
        <template #default="{ row }">
          <el-button link type="primary" @click="resetCategoryMenuStyle(row)">默认</el-button>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button type="primary" :loading="dialog.saving" @click="submit">保存</el-button>
      <el-button @click="dialog.visible = false">取消</el-button>
    </template>
  </ArtPopupDialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';

import type { ActivityCategoryVO } from '@/api/crehn/types';
import { ActivityCategoryTreeNode, findCategoryParentGroup, isCategoryGroup, isLegacyGroupedCategory } from '@/utils/artCategory';

import { parseJsonObject } from '../utils/activityRule';

type MenuStyleConfig = {
  groupIcon?: string;
  groupColor?: string;
  categoryIcon?: string;
  categoryColor?: string;
};

type MenuGroupStyleRow = {
  categoryGroup: string;
  groupIcon: string;
  groupColor: string;
};

type MenuCategoryStyleRow = {
  id?: string | number;
  categoryName: string;
  categoryGroup: string;
  categoryIcon: string;
  categoryColor: string;
};

const props = defineProps<{
  categoryList: ActivityCategoryVO[];
  categoryGroupNodes: ActivityCategoryTreeNode[];
  saveCategories: (categories: ActivityCategoryVO[]) => Promise<void>;
}>();

const dialog = reactive({ visible: false, saving: false });
const menuGroupStyleRows = ref<MenuGroupStyleRow[]>([]);
const menuCategoryStyleRows = ref<MenuCategoryStyleRow[]>([]);
const defaultMenuIcon = 'solid-circle';
const defaultMenuColor = '#4b5563';

const categoryParentDisplayName = (category?: ActivityCategoryVO) => {
  if (!category || isCategoryGroup(category)) return '';
  const parent = findCategoryParentGroup(category, props.categoryList);
  return String(parent?.categoryName || (isLegacyGroupedCategory(category) ? category.categoryGroup : '') || '一级菜单');
};

const normalizeMenuStyleValue = (value?: unknown) => (typeof value === 'string' ? value.trim() : '');

const menuStyleOf = (category?: ActivityCategoryVO): MenuStyleConfig => {
  const style = (parseJsonObject(category?.ruleJson) as Record<string, any>).menuStyle;
  if (!style || typeof style !== 'object' || Array.isArray(style)) {
    return {};
  }
  return {
    groupIcon: normalizeMenuStyleValue(style.groupIcon),
    groupColor: normalizeMenuStyleValue(style.groupColor),
    categoryIcon: normalizeMenuStyleValue(style.categoryIcon),
    categoryColor: normalizeMenuStyleValue(style.categoryColor)
  };
};

const previewMenuIcon = (icon?: string) => String(icon || defaultMenuIcon);

const setMenuStyleField = (target: Record<string, string>, key: keyof MenuStyleConfig, value?: string) => {
  const text = normalizeMenuStyleValue(value);
  if (text) {
    target[key] = text;
  } else {
    delete target[key];
  }
};

const resetGroupMenuStyle = (row: MenuGroupStyleRow) => {
  row.groupIcon = '';
  row.groupColor = '';
};

const resetCategoryMenuStyle = (row: MenuCategoryStyleRow) => {
  row.categoryIcon = '';
  row.categoryColor = '';
};

const open = () => {
  menuGroupStyleRows.value = props.categoryGroupNodes.map((group) => {
    const rows = group.virtualGroup ? group.children || [] : [group, ...(group.children || [])];
    const style = rows.map(menuStyleOf).find((item) => item.groupIcon || item.groupColor) || {};
    return {
      categoryGroup: String(group.categoryName || ''),
      groupIcon: String(style.groupIcon || ''),
      groupColor: String(style.groupColor || '')
    };
  });
  menuCategoryStyleRows.value = props.categoryList
    .filter((category) => !isCategoryGroup(category))
    .map((category) => {
      const style = menuStyleOf(category);
      return {
        id: category.id,
        categoryName: String(category.categoryName || ''),
        categoryGroup: categoryParentDisplayName(category),
        categoryIcon: String(style.categoryIcon || ''),
        categoryColor: String(style.categoryColor || '')
      };
    });
  dialog.visible = true;
};

const submit = async () => {
  dialog.saving = true;
  try {
    const groupRows = new Map(menuGroupStyleRows.value.map((row) => [row.categoryGroup, row]));
    const categoryRows = new Map(menuCategoryStyleRows.value.map((row) => [String(row.id || ''), row]));
    const changedCategories: ActivityCategoryVO[] = [];

    props.categoryList.forEach((category) => {
      if (!category.id) return;
      const groupName = isCategoryGroup(category) ? String(category.categoryName || '') : categoryParentDisplayName(category);
      const groupRow = groupName === '一级菜单' ? undefined : groupRows.get(groupName);
      const categoryRow = categoryRows.get(String(category.id));
      const rule = parseJsonObject(category.ruleJson) as Record<string, any>;
      const currentStyle = rule.menuStyle && typeof rule.menuStyle === 'object' && !Array.isArray(rule.menuStyle) ? rule.menuStyle : {};
      const nextStyle: Record<string, string> = { ...currentStyle };

      setMenuStyleField(nextStyle, 'groupIcon', groupRow?.groupIcon);
      setMenuStyleField(nextStyle, 'groupColor', groupRow?.groupColor);
      setMenuStyleField(nextStyle, 'categoryIcon', categoryRow?.categoryIcon);
      setMenuStyleField(nextStyle, 'categoryColor', categoryRow?.categoryColor);

      if (Object.keys(nextStyle).length) {
        rule.menuStyle = nextStyle;
      } else {
        delete rule.menuStyle;
      }

      const nextRuleJson = Object.keys(rule).length ? JSON.stringify(rule) : '';
      if (nextRuleJson !== String(category.ruleJson || '')) {
        changedCategories.push({ ...category, ruleJson: nextRuleJson });
      }
    });

    await props.saveCategories(changedCategories);
    dialog.visible = false;
  } finally {
    dialog.saving = false;
  }
};

defineExpose({ open });
</script>

<style scoped>
.menu-style-table {
  width: 100%;
}

.menu-style-preview {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  font-weight: 700;
}

.menu-style-preview :deep(.svg-icon) {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
  margin: 0;
}

.menu-style-preview span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
