<template>
  <div v-if="!item.hidden">
    <template v-if="hasOneShowingChild(item, item.children) && (!onlyOneChild.children || onlyOneChild.noShowingChildren) && !item.alwaysShow">
      <app-link v-if="onlyOneChild.meta" :to="resolvePath(onlyOneChild.path, onlyOneChild.query)">
        <el-menu-item
          :index="resolvePath(onlyOneChild.path)"
          :class="[{ 'submenu-title-noDropdown': !isNest }, levelClass, menuItemClass(onlyOneChild.meta), flashClass(resolvePath(onlyOneChild.path))]"
        >
          <span
            v-if="resolveMenuIcon(onlyOneChild.meta, item.meta)"
            class="menu-icon-box"
            :class="menuIconClass(resolveMenuIcon(onlyOneChild.meta, item.meta))"
            :style="menuIconStyle(onlyOneChild.meta, item.meta)"
          >
            <svg-icon :icon-class="resolveMenuIcon(onlyOneChild.meta, item.meta)" />
          </span>
          <template #title>
            <span class="menu-title" :class="menuTitleClass(onlyOneChild.meta)" :title="menuTitle(onlyOneChild.meta)">
              {{ onlyOneChild.meta.title }}
            </span>
            <span v-if="menuBadge(onlyOneChild.meta)" class="menu-count-badge">{{ menuBadge(onlyOneChild.meta) }}</span>
          </template>
        </el-menu-item>
      </app-link>
    </template>

    <el-sub-menu
      v-else
      ref="subMenu"
      :index="resolvePath(item.path)"
      :class="[levelClass, menuItemClass(item.meta), flashClass(resolvePath(item.path))]"
      teleported
    >
      <template v-if="item.meta" #title>
        <span
          v-if="resolveMenuIcon(item.meta)"
          class="menu-icon-box"
          :class="menuIconClass(resolveMenuIcon(item.meta))"
          :style="menuIconStyle(item.meta)"
        >
          <svg-icon :icon-class="resolveMenuIcon(item.meta)" />
        </span>
        <span class="menu-title" :class="menuTitleClass(item.meta)" :title="menuTitle(item.meta)">{{ item.meta?.title }}</span>
        <span v-if="menuBadge(item.meta)" class="menu-count-badge">{{ menuBadge(item.meta) }}</span>
      </template>

      <sidebar-item
        v-for="(child, index) in item.children"
        :key="child.path + index"
        :is-nest="true"
        :item="child"
        :base-path="resolvePath(child.path)"
        :depth="depth + 1"
        :flash-index="flashIndex"
        class="nest-menu"
      />
    </el-sub-menu>
  </div>
</template>

<script setup lang="ts">
import { isExternal } from '@/utils/validate';
import AppLink from './Link.vue';
import { getNormalPath } from '@/utils/ruoyi';
import { RouteRecordRaw } from 'vue-router';

const props = defineProps({
  item: {
    type: Object as PropType<RouteRecordRaw>,
    required: true
  },
  isNest: {
    type: Boolean,
    default: false
  },
  basePath: {
    type: String,
    default: ''
  },
  depth: {
    type: Number,
    default: 1
  },
  flashIndex: {
    type: String,
    default: ''
  }
});

const onlyOneChild = ref<any>({});

const hasOneShowingChild = (parent: RouteRecordRaw, children?: RouteRecordRaw[]) => {
  if (!children) {
    children = [];
  }
  const showingChildren = children.filter((item) => {
    if (item.hidden) {
      return false;
    }
    onlyOneChild.value = item;
    return true;
  });

  // When there is only one child router, the child router is displayed by default
  if (showingChildren.length === 1) {
    return true;
  }

  // Show parent if there are no child router to display
  if (showingChildren.length === 0) {
    onlyOneChild.value = { ...parent, path: '', noShowingChildren: true };
    return true;
  }

  return false;
};

const resolvePath = (routePath: string, routeQuery?: string): any => {
  if (isExternal(routePath)) {
    return routePath;
  }
  if (isExternal(props.basePath as string)) {
    return props.basePath;
  }
  if (routeQuery) {
    const query = JSON.parse(routeQuery);
    return { path: getNormalPath(props.basePath + '/' + routePath), query: query };
  }
  return getNormalPath(props.basePath + '/' + routePath);
};

const menuTitle = (meta?: any): string => {
  const title = String(meta?.fullTitle || meta?.title || '');
  return title && title.length > 5 ? title : '';
};

const menuTitleClass = (meta?: any) => ({
  'menu-title--multiline': Boolean(meta?.multilineTitle)
});

const menuItemClass = (meta?: any) => ({
  'menu-item--multiline': Boolean(meta?.multilineTitle)
});

const flashClass = (index: any) => ({
  'menu-report-flash': Boolean(props.flashIndex && typeof index === 'string' && index === props.flashIndex)
});

const levelClass = computed(() => `menu-level-${Math.min(props.depth, 3)}`);

const resolveMenuIcon = (meta?: any, parentMeta?: any) => String(meta?.icon || parentMeta?.icon || '');

const menuIconClass = (icon: string) => `menu-icon-${icon.replace(/[^a-zA-Z0-9_-]/g, '-')}`;

const resolveMenuIconColor = (meta?: any, parentMeta?: any) => String(meta?.menuIconColor || parentMeta?.menuIconColor || '');

const menuIconStyle = (meta?: any, parentMeta?: any) => {
  const color = resolveMenuIconColor(meta, parentMeta).trim();
  return color ? { color } : undefined;
};

const menuBadge = (meta?: any) => {
  const value = meta?.badge ?? meta?.count ?? meta?.total;
  return value === undefined || value === null || value === '' ? '' : String(value);
};
</script>
