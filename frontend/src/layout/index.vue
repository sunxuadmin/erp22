<template>
  <div :class="classObj" class="app-wrapper" :style="{ '--current-color': theme, '--layout-header-height': showTagsView ? '92px' : '50px' }">
    <div v-if="device === 'mobile' && sidebar.opened" class="drawer-bg" @click="handleClickOutside" />
    <side-bar v-if="!sidebar.hide" class="sidebar-container" />
    <div :class="{ hasTagsView: showTagsView, sidebarHide: sidebar.hide }" class="main-container">
      <!-- <el-scrollbar>
        <div :class="{ 'fixed-header': fixedHeader }">
          <navbar ref="navbarRef" @setLayout="setLayout" />
          <tags-view v-if="needTagsView" />
        </div>
        <app-main />
        <settings ref="settingRef" />
      </el-scrollbar> -->
      <div class="layout-header" :class="{ 'fixed-header': fixedHeader }">
        <navbar
          :breadcrumb-visible="roleShellVisibility.breadcrumbVisible"
          :hide-home-breadcrumb="roleShellVisibility.hideHomeBreadcrumb"
          :navbar-title-visible="roleShellVisibility.navbarTitleVisible"
          :navbar-title="roleShellVisibility.navbarTitle"
          :navbar-title-align="roleShellVisibility.navbarTitleAlign"
          :navbar-title-font-size="roleShellVisibility.navbarTitleFontSize"
          :navbar-title-color="roleShellVisibility.navbarTitleColor"
          :navbar-title-font-weight="roleShellVisibility.navbarTitleFontWeight"
          :navbar-title-font-family="roleShellVisibility.navbarTitleFontFamily"
          :navbar-title-logo-mode="roleShellVisibility.navbarTitleLogoMode"
          :navbar-title-logo-asset-key="roleShellVisibility.navbarTitleLogoAssetKey"
          :navbar-title-logo-height="roleShellVisibility.navbarTitleLogoHeight"
          :show-user-nickname="roleShellVisibility.showUserNickname"
          :show-screenfull="roleShellVisibility.showScreenfull"
          :show-size-select="roleShellVisibility.showSizeSelect"
          :show-user-avatar="roleShellVisibility.showUserAvatar"
          :user-display-mode="roleShellVisibility.userDisplayMode"
          :brand-title="roleShellVisibility.brandTitle"
          :brand-title-mode="roleShellVisibility.brandTitleMode"
          :brand-title-keep-visible-on-collapse="roleShellVisibility.brandTitleKeepVisibleOnCollapse"
          @set-layout="setLayout"
        />
        <tags-view v-if="showTagsView" :hide-home-tag="hideHomeTag" />
      </div>
      <app-main />
      <settings ref="settingRef" />
    </div>
  </div>
</template>

<script setup lang="ts">
import SideBar from './components/Sidebar/index.vue';
import { AppMain, Navbar, Settings, TagsView } from './components';
import { useAppStore } from '@/store/modules/app';
import { useSettingsStore } from '@/store/modules/settings';
import { usePermissionStore } from '@/store/modules/permission';
import { useTagsViewStore } from '@/store/modules/tagsView';
import { NavTypeEnum } from '@/enums/NavTypeEnum';
import { getCurrentRoleShellConfig, getWorkbenchStyleConfig } from '@/api/system/workbench';
import type { WorkbenchRoleShellConfig } from '@/api/system/workbench/types';
import { applyWorkbenchStyleVars, cloneWorkbenchStyle, defaultWorkbenchStyleConfig, parseWorkbenchStyleConfig } from '@/utils/workbenchStyle';
import { handleThemeStyle } from '@/utils/theme';
import { useDynamicTitle } from '@/utils/dynamicTitle';

const appStore = useAppStore();
const settingsStore = useSettingsStore();
const permissionStore = usePermissionStore();
const tagsViewStore = useTagsViewStore();
const route = useRoute();
const theme = computed(() => settingsStore.theme);
const sidebar = computed(() => appStore.sidebar);
const device = computed(() => appStore.device);
const needTagsView = computed(() => settingsStore.tagsView);
const fixedHeader = computed(() => settingsStore.fixedHeader);
const layout = computed(() => settingsStore.navType);
const roleShellVisibility = reactive({
  breadcrumbVisible: true,
  hideHomeBreadcrumb: false,
  hideHomeTagsView: false,
  navbarTitleVisible: true,
  navbarTitle: '',
  navbarTitleAlign: 'left' as 'left' | 'center' | 'right',
  navbarTitleFontSize: 15,
  navbarTitleColor: '#29445f',
  navbarTitleFontWeight: 'bold' as 'normal' | 'medium' | 'bold',
  navbarTitleFontFamily: 'system' as 'system' | 'microsoft-yahei' | 'simhei' | 'simsun' | 'kaiti',
  navbarTitleLogoMode: 'hidden' as 'hidden' | 'system' | 'asset',
  navbarTitleLogoAssetKey: '',
  navbarTitleLogoHeight: 28,
  showUserNickname: true,
  showScreenfull: true,
  showSizeSelect: true,
  showUserAvatar: true,
  userDisplayMode: 'nickname' as 'nickname' | 'username' | 'hidden',
  brandTitleMode: 'role' as 'role' | 'custom' | 'activity' | 'hidden',
  brandTitleKeepVisibleOnCollapse: false,
  brandTitle: '工作台'
});
const isHomeRoute = computed(() => route.path === '/index' || route.name === 'Index');
const hasNonHomeTags = computed(() =>
  tagsViewStore.getVisitedViews().some((tag) => tag.path !== '/index' && tag.name !== 'Index' && tag.name !== 'Dashboard')
);
const hideHomeTag = computed(() => isHomeRoute.value && roleShellVisibility.hideHomeTagsView && !hasNonHomeTags.value);
const showTagsView = computed(() => needTagsView.value && !hideHomeTag.value);

// 根据布局模式判断是否显示侧边栏
const showSidebar = computed(() => {
  if (sidebar.value.hide) return false;
  return layout.value === NavTypeEnum.LEFT || layout.value === NavTypeEnum.MIX;
});

const classObj = computed(() => ({
  hideSidebar: !sidebar.value.opened,
  openSidebar: sidebar.value.opened,
  withoutAnimation: sidebar.value.withoutAnimation,
  mobile: device.value === 'mobile'
}));

const { width } = useWindowSize();
const WIDTH = 992; // refer to Bootstrap's responsive design
const SIDEBAR_OPEN_WIDTH = 260;
const SIDEBAR_COLLAPSED_WIDTH = 54;

watchEffect(() => {
  if (device.value === 'mobile') {
    appStore.closeSideBar({ withoutAnimation: false });
  }
  if (width.value - 1 < WIDTH) {
    appStore.toggleDevice('mobile');
    appStore.closeSideBar({ withoutAnimation: true });
  } else {
    appStore.toggleDevice('desktop');
  }
});

const layoutDisplaySidebarWidth = computed(() => {
  if (!showSidebar.value || device.value === 'mobile') return 0;
  return sidebar.value.opened ? SIDEBAR_OPEN_WIDTH : SIDEBAR_COLLAPSED_WIDTH;
});

watchEffect(() => {
  appStore.updateLayoutDisplayScale(width.value, layoutDisplaySidebarWidth.value, device.value === 'mobile');
});

const settingRef = ref<InstanceType<typeof Settings>>();

const handleClickOutside = () => {
  appStore.closeSideBar({ withoutAnimation: false });
};

const setLayout = () => {
  settingRef.value?.openSetting();
};

const loadWorkbenchStyle = async () => {
  try {
    const res: any = await getWorkbenchStyleConfig();
    const config = parseWorkbenchStyleConfig(res.data);
    applyWorkbenchStyleVars(config);
    appStore.configureLayoutDisplayScale(config.layoutScaleMode, config.customScalePercent);
  } catch {
    const config = cloneWorkbenchStyle(defaultWorkbenchStyleConfig);
    applyWorkbenchStyleVars(config);
    appStore.configureLayoutDisplayScale(config.layoutScaleMode, config.customScalePercent);
  }
};

const applyRadius = (value: number) => {
  const radius = Math.min(Math.max(Math.round(Number(value) || 0), 0), 24);
  settingsStore.radiusBase = radius;
  const el = document.documentElement;
  el.style.setProperty('--app-radius-base', `${radius}px`);
  el.style.setProperty('--app-radius-sm', `${Math.round(radius * 0.6)}px`);
  el.style.setProperty('--app-radius-md', `${radius}px`);
  el.style.setProperty('--app-radius-lg', `${Math.round(radius * 1.4)}px`);
  el.style.setProperty('--el-border-radius-base', `${radius}px`);
  el.style.setProperty('--el-border-radius-small', `${Math.round(radius * 0.6)}px`);
};

const applyRoleShellConfig = (config: WorkbenchRoleShellConfig) => {
  roleShellVisibility.breadcrumbVisible = config.breadcrumbVisible !== false;
  roleShellVisibility.hideHomeBreadcrumb = config.hideHomeBreadcrumb === true;
  roleShellVisibility.hideHomeTagsView = config.hideHomeTagsView === true;
  roleShellVisibility.navbarTitleVisible = config.navbarTitleVisible !== false;
  roleShellVisibility.navbarTitle = String(config.navbarTitle || '');
  roleShellVisibility.navbarTitleAlign = config.navbarTitleAlign || 'left';
  roleShellVisibility.navbarTitleFontSize = Number(config.navbarTitleFontSize || 15);
  roleShellVisibility.navbarTitleColor = String(config.navbarTitleColor || '#29445f');
  roleShellVisibility.navbarTitleFontWeight = config.navbarTitleFontWeight || 'bold';
  roleShellVisibility.navbarTitleFontFamily = config.navbarTitleFontFamily || 'system';
  roleShellVisibility.navbarTitleLogoMode = config.navbarTitleLogoMode || 'hidden';
  roleShellVisibility.navbarTitleLogoAssetKey = String(config.navbarTitleLogoAssetKey || '');
  roleShellVisibility.navbarTitleLogoHeight = Number(config.navbarTitleLogoHeight || 28);
  roleShellVisibility.showUserNickname = config.showUserNickname !== false;
  roleShellVisibility.showScreenfull = config.showScreenfull !== false;
  roleShellVisibility.showSizeSelect = config.showSizeSelect !== false;
  roleShellVisibility.showUserAvatar = config.showUserAvatar !== false;
  roleShellVisibility.userDisplayMode = config.userDisplayMode || (config.showUserNickname === false ? 'hidden' : 'nickname');
  roleShellVisibility.brandTitleMode = config.brandTitleMode || 'role';
  roleShellVisibility.brandTitleKeepVisibleOnCollapse = config.brandTitleKeepVisibleOnCollapse === true;
  roleShellVisibility.brandTitle = String(config.brandTitle || '工作台');
  if (!config.configured) return;

  const navType = [NavTypeEnum.LEFT, NavTypeEnum.MIX, NavTypeEnum.TOP].includes(config.navType as NavTypeEnum)
    ? (config.navType as NavTypeEnum)
    : NavTypeEnum.LEFT;
  settingsStore.navType = navType;
  settingsStore.theme = config.theme;
  settingsStore.tagsView = config.tagsView;
  settingsStore.tagsIcon = config.tagsIcon;
  settingsStore.fixedHeader = config.fixedHeader;
  settingsStore.sidebarLogo = config.sidebarLogo;
  settingsStore.dynamicTitle = config.dynamicTitle;
  handleThemeStyle(config.theme);
  applyRadius(config.radiusBase);
  useDynamicTitle();

  if (navType === NavTypeEnum.TOP) {
    appStore.toggleSideBarHide(true);
    permissionStore.setSidebarRouters(permissionStore.defaultRoutes as any);
  } else {
    appStore.toggleSideBarHide(false);
    if (navType === NavTypeEnum.LEFT) {
      permissionStore.setSidebarRouters(permissionStore.defaultRoutes as any);
    }
  }
};

const loadRoleShellConfig = async () => {
  try {
    const res = await getCurrentRoleShellConfig();
    applyRoleShellConfig(res.data);
  } catch {
    roleShellVisibility.breadcrumbVisible = true;
    roleShellVisibility.hideHomeBreadcrumb = false;
    roleShellVisibility.hideHomeTagsView = false;
    roleShellVisibility.navbarTitleVisible = true;
    roleShellVisibility.navbarTitle = '';
    roleShellVisibility.navbarTitleAlign = 'left';
    roleShellVisibility.navbarTitleFontSize = 15;
    roleShellVisibility.navbarTitleColor = '#29445f';
    roleShellVisibility.navbarTitleFontWeight = 'bold';
    roleShellVisibility.navbarTitleFontFamily = 'system';
    roleShellVisibility.navbarTitleLogoMode = 'hidden';
    roleShellVisibility.navbarTitleLogoAssetKey = '';
    roleShellVisibility.navbarTitleLogoHeight = 28;
    roleShellVisibility.showUserNickname = true;
    roleShellVisibility.showScreenfull = true;
    roleShellVisibility.showSizeSelect = true;
    roleShellVisibility.showUserAvatar = true;
    roleShellVisibility.userDisplayMode = 'nickname';
    roleShellVisibility.brandTitleMode = 'role';
    roleShellVisibility.brandTitleKeepVisibleOnCollapse = false;
    roleShellVisibility.brandTitle = '工作台';
  }
};

onMounted(() => {
  loadWorkbenchStyle();
  loadRoleShellConfig();
});

onBeforeUnmount(() => {
  appStore.resetLayoutDisplayScale();
});
</script>

<style lang="scss" scoped>
@use '@/assets/styles/mixin.scss';
@use '@/assets/styles/variables.module.scss' as *;

.app-wrapper {
  @include mixin.clearfix;
  position: relative;
  height: 100%;
  width: 100%;

  &.mobile.openSidebar {
    position: fixed;
    top: 0;
  }
}

.drawer-bg {
  background: #000;
  opacity: 0.3;
  width: 100%;
  top: 0;
  height: 100%;
  position: absolute;
  z-index: 999;
}

.fixed-header {
  position: fixed;
  top: 0;
  right: 0;
  left: 0;
  z-index: 9;
  width: 100%;
  background: $fixed-header-bg;
  box-shadow: 0 2px 8px rgba(0, 21, 41, 0.1);
}

.layout-header:not(.fixed-header) {
  position: relative;
  z-index: 1002;
  width: calc(100% + #{$base-sidebar-width});
  margin-left: -$base-sidebar-width;
}

.layout-header :deep(.tags-view-container) {
  margin-left: $base-sidebar-width;
  transition: margin-left 0.28s;
}

.hideSidebar .layout-header:not(.fixed-header) {
  width: calc(100% + 54px);
  margin-left: -54px;
}

.hideSidebar .layout-header :deep(.tags-view-container) {
  margin-left: 54px;
}

.sidebarHide .layout-header:not(.fixed-header),
.mobile .layout-header:not(.fixed-header) {
  width: 100%;
  margin-left: 0;
}

.sidebarHide .layout-header :deep(.tags-view-container),
.mobile .layout-header :deep(.tags-view-container) {
  margin-left: 0;
}
</style>
