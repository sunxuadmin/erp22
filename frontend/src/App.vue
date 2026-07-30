<template>
  <el-config-provider :locale="appStore.locale" :size="appStore.size">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { useSettingsStore } from '@/store/modules/settings';
import { handleThemeStyle } from '@/utils/theme';
import { useAppStore } from '@/store/modules/app';
import { useUserStore } from '@/store/modules/user';
import { applyDocumentLayoutDisplayScale } from '@/utils/layoutDisplayScale';

const appStore = useAppStore();
const userStore = useUserStore();

watch(
  () => appStore.layoutDisplayScalePercent,
  (percent) => applyDocumentLayoutDisplayScale(percent),
  { immediate: true }
);

const handleBeforeUnload = (event: BeforeUnloadEvent) => {
  event.preventDefault();
  event.returnValue = ' ';
};

watch(
  () => Boolean(userStore.token && userStore.closeWarningEnabled),
  (enabled) => {
    if (enabled) {
      window.addEventListener('beforeunload', handleBeforeUnload);
    } else {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    }
  },
  { immediate: true }
);

onMounted(() => {
  nextTick(() => {
    const settingsStore = useSettingsStore();
    settingsStore.dark = false;
    settingsStore.sideTheme = 'theme-light';
    document.documentElement.classList.remove('dark');
    localStorage.setItem('useDarkKey', 'light');
    // 初始化主题样式
    handleThemeStyle(settingsStore.theme);
  });
});

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload);
});
</script>

<style>
:root {
  --app-display-scale: 1;
  --app-display-scale-inverse: 1;
  --app-display-viewport-width: 100vw;
  --app-display-viewport-height: 100dvh;
  --app-display-min-height: 100vh;
}

body {
  zoom: var(--app-display-scale);
}

html.is-app-display-scaled {
  overflow-x: hidden;
}

html.is-app-display-scaled body {
  min-height: var(--app-display-min-height);
}

html.is-app-display-scaled #app {
  min-height: var(--app-display-min-height);
}

html.is-app-display-scaled body > .el-overlay,
html.is-app-display-scaled body > .el-overlay > .el-overlay-dialog {
  right: auto;
  bottom: auto;
  width: var(--app-display-viewport-width);
  height: var(--app-display-viewport-height);
}
</style>
