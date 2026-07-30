<template>
  <div
    :class="{
      'sidebar-blank-hotspot': canUseSidebarBlankToggle,
      'sidebar-blank-hotspot--active': sidebarBlankTipVisible
    }"
    @mousemove="handleSidebarBlankMouseMove"
    @mouseleave="hideSidebarBlankTip"
    @click="handleSidebarBlankClick"
  >
    <el-scrollbar :class="sideTheme" wrap-class="scrollbar-wrapper">
      <transition :enter-active-class="proxy?.animate.menuSearchAnimate.enter" mode="out-in">
        <el-menu
          ref="menuRef"
          :default-active="activeMenu"
          :collapse="isCollapse"
          :background-color="bgColor"
          :text-color="textColor"
          :unique-opened="true"
          :active-text-color="theme"
          :collapse-transition="false"
          :popper-offset="12"
          :default-openeds="defaultOpeneds"
          mode="vertical"
        >
          <sidebar-item v-for="(r, index) in sidebarRouters" :key="r.path + index" :item="r" :base-path="r.path" :flash-index="flashMenuIndex" />
        </el-menu>
      </transition>
    </el-scrollbar>
    <button class="sidebar-collapse-toggle" type="button" :title="collapseButtonLabel" @click.stop="toggleSideBar">
      <hamburger :is-active="appStore.sidebar.opened" class="sidebar-collapse-toggle-icon" />
      <span class="sidebar-collapse-toggle-text">{{ collapseButtonLabel }}</span>
    </button>
    <Teleport to="body">
      <div v-if="sidebarBlankTipVisible" class="sidebar-blank-toggle-tip" :style="sidebarBlankTipStyle">{{ sidebarBlankTipText }}</div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import SidebarItem from './SidebarItem.vue';
import variables from '@/assets/styles/variables.module.scss';
import { useAppStore } from '@/store/modules/app';
import { useSettingsStore } from '@/store/modules/settings';
import { usePermissionStore } from '@/store/modules/permission';
import { getNormalPath } from '@/utils/ruoyi';
import { RouteRecordRaw } from 'vue-router';

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const route = useRoute();
const router = useRouter();
const appStore = useAppStore();
const permissionStore = usePermissionStore();
const menuRef = ref<any>();
const requestedOpeneds = ref<string[]>([]);
const flashMenuIndex = ref('');
const sidebarBlankTipVisible = ref(false);
const sidebarBlankTipStyle = ref<Record<string, string>>({ top: '0px', left: '62px' });
let flashTimer: number | undefined;
const sidebarRouters = computed<RouteRecordRaw[]>(() => permissionStore.getSidebarRoutes());
const settingsStore = useSettingsStore();
const sideTheme = computed(() => settingsStore.sideTheme);
const theme = computed(() => settingsStore.theme);
const isCollapse = computed(() => !appStore.sidebar.opened);
const canUseSidebarBlankToggle = computed(() => appStore.device !== 'mobile');
const collapseButtonLabel = computed(() => (appStore.sidebar.opened ? '折叠' : '展开'));
const sidebarBlankTipText = computed(() => (isCollapse.value ? '点击展开' : '点击折叠菜单'));

const sidebarBlankInteractiveSelector = [
  '.el-menu-item',
  '.el-sub-menu__title',
  '.el-menu-tooltip__trigger',
  '.sidebar-logo-container',
  '.sidebar-logo-link',
  '.sidebar-collapse-toggle',
  '.el-scrollbar__bar',
  '.el-scrollbar__thumb',
  'a',
  'button',
  '[role="button"]',
  '[role="menuitem"]'
].join(',');

const isExpandedMenuBlankTarget = (event: MouseEvent, target: HTMLElement) => {
  if (!target.closest('.el-scrollbar')) {
    return false;
  }
  const menuElement = menuRef.value?.$el as HTMLElement | undefined;
  if (!menuElement) {
    return false;
  }
  const menuItemBottoms = Array.from(menuElement.querySelectorAll<HTMLElement>('.el-menu-item, .el-sub-menu__title'))
    .filter((item) => item.getClientRects().length > 0)
    .map((item) => item.getBoundingClientRect().bottom);
  if (!menuItemBottoms.length) {
    return false;
  }
  return event.clientY >= Math.max(...menuItemBottoms) + 6;
};

const isSidebarBlankTarget = (event: MouseEvent) => {
  if (!canUseSidebarBlankToggle.value) {
    return false;
  }
  const target = event.target;
  const currentTarget = event.currentTarget;
  if (!(target instanceof HTMLElement) || !(currentTarget instanceof HTMLElement) || !currentTarget.contains(target)) {
    return false;
  }
  if (target.closest(sidebarBlankInteractiveSelector)) {
    return false;
  }
  return isCollapse.value || isExpandedMenuBlankTarget(event, target);
};

const hideSidebarBlankTip = () => {
  sidebarBlankTipVisible.value = false;
};

const toggleSideBar = () => {
  appStore.toggleSideBar(false);
  hideSidebarBlankTip();
};

const handleSidebarBlankMouseMove = (event: MouseEvent) => {
  if (!isSidebarBlankTarget(event)) {
    hideSidebarBlankTip();
    return;
  }
  const currentTarget = event.currentTarget;
  const sidebarRight = currentTarget instanceof HTMLElement ? currentTarget.getBoundingClientRect().right : 54;
  const top = Math.min(window.innerHeight - 24, Math.max(24, event.clientY));
  sidebarBlankTipStyle.value = { top: `${top}px`, left: `${sidebarRight + 8}px` };
  sidebarBlankTipVisible.value = true;
};

const handleSidebarBlankClick = (event: MouseEvent) => {
  if (!isSidebarBlankTarget(event)) {
    return;
  }
  appStore.toggleSideBar(false);
  hideSidebarBlankTip();
};

const activeMenu = computed(() => {
  const { meta, path, query } = route;
  const returnPath = Array.isArray(query.returnPath) ? query.returnPath[0] : query.returnPath;
  if (path.startsWith('/crehn/project/edit') && returnPath) {
    return String(returnPath).split('?')[0];
  }
  // if set path, the sidebar will highlight the path you set
  if (meta.activeMenu) {
    return meta.activeMenu;
  }
  return path;
});

const defaultOpeneds = computed(() => {
  const active = typeof activeMenu.value === 'string' ? activeMenu.value : route.path;
  const cleanPath = String(active || '').split('?')[0];
  const segments = cleanPath.split('/').filter(Boolean);
  const openeds: string[] = [];
  let current = '';
  segments.slice(0, -1).forEach((segment) => {
    current += `/${segment}`;
    openeds.push(current);
  });
  return Array.from(new Set(openeds));
});

type SidebarReportGroupRequest = {
  requestId: number;
  targetType?: 'group' | 'category';
  activityId?: string | number;
  categoryNodeId?: string | number;
  categoryId?: string | number;
  categoryCode?: string;
  categoryName?: string;
  groupKey: string;
  groupName: string;
};

const reportGroupDefs = [
  { key: 'performance', aliases: ['performance', '艺术表演类', '表演类节目', '艺术表演类节目', '表演'] },
  { key: 'principal', aliases: ['principal', '校长书画', '高校校长书画作品'] },
  { key: 'workshop', aliases: ['workshop', '艺术实践工作坊', '工作坊'] },
  {
    key: 'achievement',
    aliases: ['achievement', '高校美育改革创新优秀成果', '美育改革创新优秀成果', '高校美育改革创新成果', '优秀成果', '美育']
  },
  { key: 'artwork', aliases: ['artwork', '艺术作品类', '艺术作品', '作品'] }
];

const resolveReportGroupKey = (value?: string) => {
  const text = String(value || '').toLowerCase();
  return reportGroupDefs.find((group) => group.aliases.some((alias) => text.includes(alias.toLowerCase())))?.key || '';
};

const parseMenuQuery = (routeItem: RouteRecordRaw) => {
  const query = (routeItem as any).query;
  if (!query) {
    return {};
  }
  if (typeof query === 'string') {
    try {
      return JSON.parse(query) || {};
    } catch {
      return {};
    }
  }
  return typeof query === 'object' ? query : {};
};

const routeTextCandidates = (routeItem: RouteRecordRaw) => {
  const query = parseMenuQuery(routeItem) as Record<string, any>;
  return [
    routeItem.meta?.title,
    routeItem.meta?.fullTitle,
    query.categoryGroup,
    query.groupName,
    query.categoryName,
    routeItem.path,
    String(routeItem.name || '')
  ]
    .map((item) => String(item || '').trim())
    .filter(Boolean);
};

const isReportGroupRoute = (routeItem: RouteRecordRaw, request: SidebarReportGroupRequest) => {
  const query = parseMenuQuery(routeItem) as Record<string, any>;
  const activityMatches =
    request.activityId === undefined || query.activityId === undefined || String(query.activityId) === String(request.activityId);
  if (!activityMatches) return false;
  if (request.categoryNodeId !== undefined && query.categoryNodeId !== undefined) {
    return String(query.categoryNodeId) === String(request.categoryNodeId);
  }
  const candidates = routeTextCandidates(routeItem);
  const groupName = String(request.groupName || '').trim();
  if (groupName && candidates.some((item) => item === groupName || item.includes(groupName) || groupName.includes(item))) {
    return true;
  }
  return Boolean(request.groupKey && resolveReportGroupKey(candidates.join(' ')) === request.groupKey);
};
const isReportCategoryRoute = (routeItem: RouteRecordRaw, request: SidebarReportGroupRequest) => {
  const query = parseMenuQuery(routeItem) as Record<string, any>;
  const activityMatches =
    request.activityId === undefined || query.activityId === undefined || String(query.activityId) === String(request.activityId);
  if (!activityMatches) return false;
  if (request.categoryId !== undefined && query.categoryId !== undefined && String(query.categoryId) === String(request.categoryId)) {
    return true;
  }
  if (request.categoryCode && query.categoryCode && String(query.categoryCode) === String(request.categoryCode)) {
    return true;
  }
  return Boolean(request.categoryName && query.categoryName && String(query.categoryName) === String(request.categoryName));
};

const visibleChildrenOf = (routeItem: RouteRecordRaw) => (routeItem.children || []).filter((child) => !(child as any).hidden);

const isRenderedAsSubMenu = (routeItem: RouteRecordRaw) => {
  if (routeItem.alwaysShow) {
    return true;
  }
  const visibleChildren = visibleChildrenOf(routeItem);
  if (visibleChildren.length === 0) {
    return false;
  }
  if (visibleChildren.length === 1 && !visibleChildren[0].children?.length) {
    return false;
  }
  return true;
};

const resolveSidebarPath = (basePath: string, routePath?: string) => getNormalPath(`${basePath}/${routePath || ''}`);

const findReportGroupOpenedsFromRoute = (
  routeItem: RouteRecordRaw,
  request: SidebarReportGroupRequest,
  basePath: string,
  ancestors: string[]
): string[] | undefined => {
  if ((routeItem as any).hidden) {
    return undefined;
  }
  const children = visibleChildrenOf(routeItem);
  const isSubMenu = isRenderedAsSubMenu(routeItem);
  if (!isSubMenu) {
    return undefined;
  }

  const currentIndex = resolveSidebarPath(basePath, routeItem.path);
  const openChain = currentIndex ? [...ancestors, currentIndex] : ancestors;
  if (isReportGroupRoute(routeItem, request)) {
    return openChain;
  }

  for (const child of children) {
    const childBasePath = resolveSidebarPath(basePath, child.path);
    const childOpeneds = findReportGroupOpenedsFromRoute(child, request, childBasePath, openChain);
    if (childOpeneds) {
      return childOpeneds;
    }
  }
  return undefined;
};

const findReportGroupOpeneds = (routes: RouteRecordRaw[], request: SidebarReportGroupRequest): string[] | undefined => {
  for (const routeItem of routes) {
    const childOpeneds = findReportGroupOpenedsFromRoute(routeItem, request, String(routeItem.path || ''), []);
    if (childOpeneds) {
      return childOpeneds;
    }
  }
  return undefined;
};
type ReportCategoryMenuTarget = {
  path: string;
  query: Record<string, any>;
  openeds: string[];
};
const findReportCategoryTargetFromRoute = (
  routeItem: RouteRecordRaw,
  request: SidebarReportGroupRequest,
  basePath: string,
  ancestors: string[]
): ReportCategoryMenuTarget | undefined => {
  if ((routeItem as any).hidden) return undefined;
  const isSubMenu = isRenderedAsSubMenu(routeItem);
  const currentIndex = isSubMenu ? resolveSidebarPath(basePath, routeItem.path) : basePath;
  if (isReportCategoryRoute(routeItem, request)) {
    return {
      path: currentIndex,
      query: parseMenuQuery(routeItem) as Record<string, any>,
      openeds: ancestors
    };
  }
  const nextAncestors = isSubMenu ? [...ancestors, currentIndex] : ancestors;
  for (const child of visibleChildrenOf(routeItem)) {
    const childBasePath = resolveSidebarPath(basePath, child.path);
    const target = findReportCategoryTargetFromRoute(child, request, childBasePath, nextAncestors);
    if (target) return target;
  }
  return undefined;
};
const findReportCategoryTarget = (routes: RouteRecordRaw[], request: SidebarReportGroupRequest) => {
  for (const routeItem of routes) {
    const target = findReportCategoryTargetFromRoute(routeItem, request, String(routeItem.path || ''), []);
    if (target) return target;
  }
  return undefined;
};

const safeOpenMenu = (index: string) => {
  try {
    menuRef.value?.open(index);
    return true;
  } catch {
    return false;
  }
};

const openMenuChain = async (openeds: string[]) => {
  for (const index of openeds) {
    if (!safeOpenMenu(index)) return false;
    await nextTick();
  }
  return true;
};

const flashReportMenu = async (index: string) => {
  flashMenuIndex.value = '';
  await nextTick();
  flashMenuIndex.value = index;
  await nextTick();
  const menuElement = menuRef.value?.$el as HTMLElement | undefined;
  menuElement?.querySelector<HTMLElement>('.menu-report-flash')?.scrollIntoView({ block: 'nearest', inline: 'nearest' });
  if (flashTimer) {
    window.clearTimeout(flashTimer);
  }
  flashTimer = window.setTimeout(() => {
    flashMenuIndex.value = '';
  }, 1400);
};

const applyReportGroupOpenRequest = async (request?: SidebarReportGroupRequest) => {
  if (!request) {
    return;
  }
  await nextTick();
  if (request.targetType === 'category') {
    const target = findReportCategoryTarget(sidebarRouters.value, request);
    if (!target) return;
    if (!(await openMenuChain(target.openeds))) return;
    requestedOpeneds.value = target.openeds;
    await flashReportMenu(target.path);
    return;
  }
  const openeds = findReportGroupOpeneds(sidebarRouters.value, request);
  if (!openeds?.length) {
    return;
  }
  const nextOpenedSet = new Set(openeds);
  requestedOpeneds.value
    .filter((path) => !nextOpenedSet.has(path))
    .reverse()
    .forEach((path) => menuRef.value?.close(path));
  const targetIndex = openeds[openeds.length - 1];
  if (!targetIndex) {
    return;
  }
  if (!(await openMenuChain(openeds))) return;
  requestedOpeneds.value = openeds;
  await flashReportMenu(targetIndex);
};

watch(
  () => appStore.sidebarReportGroupRequest?.requestId,
  () => applyReportGroupOpenRequest(appStore.sidebarReportGroupRequest),
  { flush: 'post' }
);

watch(canUseSidebarBlankToggle, (enabled) => {
  if (!enabled) {
    hideSidebarBlankTip();
  }
});

watch(isCollapse, () => {
  hideSidebarBlankTip();
});

onBeforeUnmount(() => {
  if (flashTimer) {
    window.clearTimeout(flashTimer);
  }
  hideSidebarBlankTip();
});

const bgColor = computed(() => 'transparent');
const textColor = computed(() => (sideTheme.value === 'theme-dark' ? variables.menuColor : variables.menuLightColor));
</script>
