<template>
  <div class="navbar" :class="'nav' + navType">
    <div v-if="hasSidebarBrandSlot" class="navbar-sidebar-brand" :class="{ 'navbar-sidebar-brand--collapsed': sidebarBrandCollapsed }">
      <logo v-show="showLogo" :collapse="sidebarBrandCollapsed" :title="brandDisplayTitle" :show-title="showBrandTitle" />
    </div>

    <hamburger
      v-if="appStore.device === 'mobile'"
      id="hamburger-container"
      :is-active="appStore.sidebar.opened"
      class="hamburger-container"
      @toggle-click="toggleSideBar"
    />

    <div
      v-if="showNavbarIdentity && navType !== NavTypeEnum.TOP"
      class="navbar-school-title"
      :class="[`is-${props.navbarTitleAlign}`, { 'is-brand-hidden': !showLogo }]"
      :style="navbarTitleStyle"
      :title="navbarDisplayTitle"
    >
      <img v-if="navbarLogoUrl" :src="navbarLogoUrl" class="navbar-title-logo" alt="" />
      <span v-if="navbarDisplayTitle" class="navbar-title-text">{{ navbarDisplayTitle }}</span>
    </div>
    <breadcrumb v-if="showBreadcrumb" id="breadcrumb-container" class="breadcrumb-container" />
    <top-nav v-if="navType == NavTypeEnum.MIX" id="topmenu-container" class="topmenu-container" />

    <template v-if="navType == NavTypeEnum.TOP">
      <logo v-show="showLogo" class="navbar-top-brand" :collapse="false" :title="brandDisplayTitle" :show-title="showBrandTitle" />
      <div
        v-if="showNavbarIdentity"
        class="navbar-school-title"
        :class="`is-${props.navbarTitleAlign}`"
        :style="navbarTitleStyle"
        :title="navbarDisplayTitle"
      >
        <img v-if="navbarLogoUrl" :src="navbarLogoUrl" class="navbar-title-logo" alt="" />
        <span v-if="navbarDisplayTitle" class="navbar-title-text">{{ navbarDisplayTitle }}</span>
      </div>
      <top-bar id="topbar-container" class="topbar-container" />
    </template>
    <div class="right-menu flex align-center">
      <div class="navbar-action-group">
        <template v-if="appStore.device !== 'mobile'">
          <el-tooltip v-if="props.showScreenfull" :content="proxy.$t('navbar.full')" effect="dark" placement="bottom">
            <screenfull id="screenfull" class="right-menu-item hover-effect" />
          </el-tooltip>

          <el-tooltip v-if="props.showSizeSelect" :content="proxy.$t('navbar.layoutSize')" effect="dark" placement="bottom">
            <size-select id="size-select" class="right-menu-item hover-effect" />
          </el-tooltip>
        </template>
        <div v-if="showUserEntry" class="avatar-container">
          <el-dropdown class="right-menu-item hover-effect" trigger="click" @command="handleCommand">
            <div class="avatar-wrapper">
              <img v-if="props.showUserAvatar" :src="userStore.avatar" class="user-avatar" />
              <span v-if="userDisplayText" class="user-nickname" :title="userDisplayText">{{ userDisplayText }}</span>
              <el-icon><caret-bottom /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <router-link to="/user/profile">
                  <el-dropdown-item>{{ proxy.$t('navbar.personalCenter') }}</el-dropdown-item>
                </router-link>
                <el-dropdown-item v-if="settingsStore.showSettings && isSuperAdmin" command="setLayout">
                  <span>{{ proxy.$t('navbar.layoutSetting') }}</span>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <span>{{ proxy.$t('navbar.logout') }}</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAppStore } from '@/store/modules/app';
import { useUserStore } from '@/store/modules/user';
import { useSettingsStore } from '@/store/modules/settings';
import router from '@/router';
import { ElMessageBoxOptions } from 'element-plus/es/components/message-box/src/message-box.type';
import { NavTypeEnum } from '@/enums/NavTypeEnum';
import Logo from '@/layout/components/Sidebar/Logo.vue';
import TopBar from './TopBar/index.vue';
import { getPublicSchoolOptions } from '@/api/login';
import { workbenchAssetUrl } from '@/api/system/workbench';
import systemLogo from '@/assets/logo/logo.png';

const appStore = useAppStore();
const userStore = useUserStore();
const settingsStore = useSettingsStore();
const route = useRoute();
const props = withDefaults(
  defineProps<{
    breadcrumbVisible?: boolean;
    hideHomeBreadcrumb?: boolean;
    navbarTitleVisible?: boolean;
    navbarTitle?: string;
    navbarTitleAlign?: 'left' | 'center' | 'right';
    navbarTitleFontSize?: number;
    navbarTitleColor?: string;
    navbarTitleFontWeight?: 'normal' | 'medium' | 'bold';
    navbarTitleFontFamily?: 'system' | 'microsoft-yahei' | 'simhei' | 'simsun' | 'kaiti';
    navbarTitleLogoMode?: 'hidden' | 'system' | 'asset';
    navbarTitleLogoAssetKey?: string;
    navbarTitleLogoHeight?: number;
    showUserNickname?: boolean;
    showScreenfull?: boolean;
    showSizeSelect?: boolean;
    showUserAvatar?: boolean;
    userDisplayMode?: 'nickname' | 'username' | 'hidden';
    brandTitle?: string;
    brandTitleMode?: 'role' | 'custom' | 'activity' | 'hidden';
    brandTitleKeepVisibleOnCollapse?: boolean;
  }>(),
  {
    breadcrumbVisible: true,
    hideHomeBreadcrumb: false,
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
    brandTitle: '工作台',
    brandTitleMode: 'custom',
    brandTitleKeepVisibleOnCollapse: false
  }
);

const { proxy } = getCurrentInstance() as ComponentInternalInstance;

const navType = computed(() => settingsStore.navType);
const hasSidebarBrandSlot = computed(() => showLogo.value && appStore.device !== 'mobile' && navType.value !== NavTypeEnum.TOP);
const showBreadcrumb = computed(
  () =>
    props.breadcrumbVisible &&
    navType.value === NavTypeEnum.LEFT &&
    !(props.hideHomeBreadcrumb && (route.path === '/index' || route.name === 'Index'))
);
const showLogo = computed(() => settingsStore.sidebarLogo);
const showBrandTitle = computed(() => props.brandTitleMode !== 'hidden');
const sidebarBrandCollapsed = computed(
  () => !appStore.sidebar.opened && !(props.brandTitleKeepVisibleOnCollapse && showLogo.value && showBrandTitle.value)
);
const configuredUserDisplayMode = computed(() => {
  if (props.userDisplayMode) return props.userDisplayMode;
  return props.showUserNickname ? 'nickname' : 'hidden';
});
const userDisplayText = computed(() => {
  if (configuredUserDisplayMode.value === 'hidden') return '';
  if (appStore.device === 'mobile' && props.showUserAvatar) return '';
  return configuredUserDisplayMode.value === 'username' ? userStore.name : userStore.nickname;
});
const showUserEntry = computed(() => props.showUserAvatar || Boolean(userDisplayText.value));
const isSuperAdmin = computed(() => userStore.roles.includes('admin') || userStore.roles.includes('superadmin'));
const schoolDisplayName = ref('');
const hasRole = (...roleKeys: string[]) => {
  const normalizedRoles = userStore.roles.map((role) => String(role).trim().toLowerCase());
  return normalizedRoles.some((role) => roleKeys.some((roleKey) => role === roleKey || role.startsWith(`${roleKey}_`)));
};
const roleWorkbenchTitle = computed(() => {
  if (userStore.schoolId || hasRole('crehn_school', 'school')) return schoolDisplayName.value || '学校工作台';
  if (hasRole('crehn_admin', 'admin', 'superadmin', 'super_admin', 'crehn_ops', 'ops')) return '省级艺术作品评审管理平台';
  if (hasRole('crehn_auditor', 'auditor')) return '项目审核工作台';
  if (hasRole('crehn_expert', 'expert', 'crehn_reviewer', 'reviewer')) return '专家评审工作台';
  if (hasRole('crehn_project_viewer', 'project_viewer')) return '项目查看工作台';
  return '工作台';
});
const navbarDisplayTitle = computed(() => {
  if (!props.navbarTitleVisible) return '';
  const template = props.navbarTitle.trim() || roleWorkbenchTitle.value;
  return template.replace(/\{(roleName|nickName|userName)\}/g, (_match, key: string) => {
    if (key === 'nickName') return userStore.nickname || '';
    if (key === 'userName') return userStore.name || '';
    return roleWorkbenchTitle.value.replace(/工作台$/, '');
  });
});
const navbarLogoUrl = computed(() => {
  if (props.navbarTitleLogoMode === 'system') return systemLogo;
  if (props.navbarTitleLogoMode === 'asset') return workbenchAssetUrl(props.navbarTitleLogoAssetKey);
  return '';
});
const showNavbarIdentity = computed(() => Boolean(navbarDisplayTitle.value || navbarLogoUrl.value));
const brandDisplayTitle = computed(() => {
  const template = props.brandTitleMode === 'role' ? roleWorkbenchTitle.value : props.brandTitle;
  return template.replace(/\{(roleName|nickName|userName)\}/g, (_match, key: string) => {
    if (key === 'nickName') return userStore.nickname || '';
    if (key === 'userName') return userStore.name || '';
    return roleWorkbenchTitle.value.replace(/工作台$/, '');
  });
});
const navbarTitleFontFamilies = {
  system: 'Avenir, "Helvetica Neue", Arial, "Microsoft YaHei", sans-serif',
  'microsoft-yahei': '"Microsoft YaHei", "微软雅黑", Arial, sans-serif',
  simhei: 'SimHei, "黑体", "Microsoft YaHei", sans-serif',
  simsun: 'SimSun, "宋体", serif',
  kaiti: 'KaiTi, "楷体", "STKaiti", serif'
} as const;
const navbarTitleStyle = computed(() => ({
  '--navbar-title-font-size': `${Math.min(Math.max(Number(props.navbarTitleFontSize) || 15, 12), 28)}px`,
  '--navbar-title-color': props.navbarTitleColor || '#29445f',
  '--navbar-title-font-weight': props.navbarTitleFontWeight === 'normal' ? '400' : props.navbarTitleFontWeight === 'medium' ? '500' : '700',
  '--navbar-title-font-family': navbarTitleFontFamilies[props.navbarTitleFontFamily],
  '--navbar-title-logo-height': `${Math.min(Math.max(Number(props.navbarTitleLogoHeight) || 28, 16), 36)}px`
}));

const loadSchoolDisplayName = async () => {
  schoolDisplayName.value = '';
  if (!userStore.schoolId) return;
  const res = await getPublicSchoolOptions({ status: 'enabled' });
  const school = (res.data || []).find((item: any) => String(item.id) === String(userStore.schoolId));
  schoolDisplayName.value = school?.schoolName || '';
};

watch(() => userStore.schoolId, loadSchoolDisplayName, { immediate: true });

const toggleSideBar = () => {
  appStore.toggleSideBar(false);
};

const logout = async () => {
  await ElMessageBox.confirm('确定注销并退出系统吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  } as ElMessageBoxOptions);
  userStore.logout().then(() => {
    router.replace({
      path: '/login',
      query: {
        redirect: encodeURIComponent(router.currentRoute.value.fullPath || '/')
      }
    });
    proxy?.$tab.closeAllPage();
  });
};

const emits = defineEmits(['setLayout']);
const setLayout = () => {
  emits('setLayout');
};
// 定义Command方法对象 通过key直接调用方法
const commandMap: { [key: string]: any } = {
  setLayout,
  logout
};
const handleCommand = (command: string) => {
  // 判断是否存在该方法
  if (commandMap[command]) {
    commandMap[command]();
  }
};
</script>

<style lang="scss" scoped>
@use '@/assets/styles/variables.module.scss' as *;

.navbar.navtop {
  .hamburger-container {
    display: none !important;
  }
}

:deep(.el-select .el-input__wrapper) {
  height: 30px;
}

:deep(.el-badge__content.is-fixed) {
  top: 12px;
}

.flex {
  display: flex;
}

.align-center {
  align-items: center;
}

.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #ffffff;
  border-bottom: 1px solid #d9e6f6;
  box-shadow: none;
  display: flex;
  align-items: center;
  padding: 0 12px 0 0;
  box-sizing: border-box;

  .navbar-sidebar-brand {
    width: $base-sidebar-width;
    height: 50px;
    flex: 0 0 $base-sidebar-width;
    overflow: hidden;
    transition:
      width 0.28s,
      flex-basis 0.28s;

    &--collapsed {
      width: 54px;
      flex-basis: 54px;
    }
  }

  .navbar-top-brand {
    width: $base-sidebar-width;
    height: 50px;
    flex: 0 0 $base-sidebar-width;
  }

  .hamburger-container {
    width: 42px;
    height: 42px;
    line-height: 42px;
    //float: left;
    cursor: pointer;
    transition:
      background 0.2s ease,
      color 0.2s ease,
      box-shadow 0.2s ease,
      transform 0.2s ease;
    -webkit-tap-highlight-color: transparent;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    margin-right: 10px;
    margin-left: 12px;
    color: #1f3b64;
    border: 1px solid #d9e6f6;
    border-radius: 8px;
    background: #f8fbff;
    box-shadow: none;

    &:hover {
      color: var(--el-color-primary);
      background: #eef5ff;
      border-color: #bcd2f2;
      transform: none;
    }
  }

  .breadcrumb-container {
    //float: left;
    flex-shrink: 0;
  }

  .topmenu-container {
    position: relative;
    flex: 1;
    min-width: 0;
  }

  .topbar-container {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    overflow: hidden;
    margin-left: 8px;
  }

  .navbar-school-title {
    position: relative;
    max-width: min(38vw, 560px);
    min-width: 0;
    height: 50px;
    padding: 0 12px;
    display: flex;
    align-items: center;
    gap: 10px;
    justify-content: flex-start;
    overflow: hidden;
    flex: 1 1 auto;
    font-size: var(--navbar-title-font-size, 16px);
    font-weight: var(--navbar-title-font-weight, 800);
    font-family: var(--navbar-title-font-family, Avenir, 'Helvetica Neue', Arial, 'Microsoft YaHei', sans-serif);
    line-height: 34px;
    color: var(--navbar-title-color, #0f2f5f);
    text-align: left;
    text-overflow: ellipsis;
    &.is-center {
      justify-content: center;
      text-align: center;
    }
    &.is-right {
      justify-content: flex-end;
      text-align: right;
    }
    white-space: nowrap;
    flex: 0 1 auto;
    margin-left: 16px;
    pointer-events: none;
    background: transparent;
    border: 0;
    border-radius: 0;
    box-shadow: none;
    transform: none;

    &.is-brand-hidden {
      max-width: min(60vw, 760px);
    }

    .navbar-title-logo {
      width: auto;
      max-width: 120px;
      height: var(--navbar-title-logo-height, 28px);
      max-height: 36px;
      object-fit: contain;
      flex: 0 0 auto;
    }

    .navbar-title-text {
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
  .errLog-container {
    display: inline-block;
    vertical-align: top;
  }

  .right-menu {
    height: 100%;
    display: flex;
    align-items: center;
    margin-left: auto;

    &:focus {
      outline: none;
    }

    .navbar-action-group {
      height: 38px;
      padding: 2px;
      display: inline-flex;
      align-items: center;
      gap: 2px;
      border: 1px solid #d9e6f6;
      border-radius: 999px;
      background: #ffffff;
      box-shadow: none;
    }

    .right-menu-item {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 34px;
      height: 34px;
      padding: 0;
      font-size: 18px;
      color: #4b6380;
      border: 0;
      border-radius: 999px;
      background: transparent;

      &.hover-effect {
        cursor: pointer;
        transition:
          background 0.2s ease,
          color 0.2s ease,
          transform 0.2s ease;

        &:hover {
          background: #eef5ff;
          color: var(--el-color-primary);
          transform: none;
        }
      }
    }

    .avatar-container {
      .right-menu-item {
        width: auto;
      }

      .avatar-wrapper {
        margin-top: 0;
        position: relative;
        height: 34px;
        min-width: 52px;
        padding: 0 8px 0 2px;
        display: flex;
        align-items: center;
        gap: 5px;
        border: 0;
        border-radius: 999px;
        background: transparent;
        transition:
          background 0.2s ease,
          color 0.2s ease,
          transform 0.2s ease;

        &:hover {
          background: #eef5ff;
          transform: none;
        }

        .user-avatar {
          cursor: pointer;
          width: 28px;
          height: 28px;
          border-radius: 50%;
          margin-top: 0;
          display: block;
          border: 1px solid #c7d9f1;
        }

        .user-nickname {
          max-width: 112px;
          overflow: hidden;
          color: #334e70;
          font-size: 13px;
          font-weight: 700;
          line-height: 20px;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        i {
          cursor: pointer;
          position: static;
          font-size: 12px;
          color: #5f7693;
        }
      }
    }
  }
}
</style>
