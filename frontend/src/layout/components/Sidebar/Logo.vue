<template>
  <div class="sidebar-logo-container" :class="{ collapse: props.collapse }">
    <transition :enter-active-class="proxy?.animate.logoAnimate.enter" mode="out-in">
      <router-link v-if="props.collapse" key="collapse" class="sidebar-logo-link" to="/">
        <img v-if="logo" :src="logo" class="sidebar-logo" />
        <h1 v-else-if="props.showTitle" class="sidebar-title">
          {{ title }}
        </h1>
      </router-link>
      <router-link v-else key="expand" class="sidebar-logo-link" to="/">
        <img v-if="logo" :src="logo" class="sidebar-logo" />
        <h1 v-if="props.showTitle" class="sidebar-title">
          {{ title }}
        </h1>
      </router-link>
    </transition>
  </div>
</template>

<script setup lang="ts">
import variables from '@/assets/styles/variables.module.scss';
import logo from '@/assets/logo/logo.png';
import { useSettingsStore } from '@/store/modules/settings';
import { useSidebarLogoConfig } from '@/hooks/useSidebarLogoConfig';
const { proxy } = getCurrentInstance() as ComponentInternalInstance;
import { NavTypeEnum } from '@/enums/NavTypeEnum';

const props = withDefaults(
  defineProps<{
    collapse: boolean;
    title?: string;
    showTitle?: boolean;
  }>(),
  { title: '', showTitle: true }
);

const settingsStore = useSettingsStore();
const sidebarLogoConfig = useSidebarLogoConfig();
const sideTheme = computed(() => settingsStore.sideTheme);
const title = computed(() => props.title.trim() || sidebarLogoConfig.title.value || '工作台');

// 获取Logo背景色
const getLogoBackground = computed(() => {
  if (sidebarLogoConfig.backgroundColor.value) {
    return sidebarLogoConfig.backgroundColor.value;
  }
  if (settingsStore.dark) {
    return 'var(--sidebar-bg)';
  }
  if (settingsStore.navType == NavTypeEnum.TOP) {
    return variables.menuLightBackground;
  }
  return sideTheme.value === 'theme-dark' ? variables.menuBackground : variables.menuLightBackground;
});

// 获取Logo文字颜色
const getLogoTextColor = computed(() => {
  if (sidebarLogoConfig.textColor.value) {
    return sidebarLogoConfig.textColor.value;
  }
  if (settingsStore.dark) {
    return 'var(--sidebar-text)';
  }
  if (settingsStore.navType == NavTypeEnum.TOP) {
    return variables.logoLightTitleColor;
  }
  return sideTheme.value === 'theme-dark' ? variables.logoTitleColor : variables.logoLightTitleColor;
});
</script>

<style lang="scss" scoped>
.sidebarLogoFade-enter-active {
  transition: opacity 1.5s;
}

.sidebarLogoFade-enter,
.sidebarLogoFade-leave-to {
  opacity: 0;
}

.sidebar-logo-container {
  position: relative;
  height: 50px;
  line-height: 50px;
  background: v-bind(getLogoBackground);
  text-align: center;
  overflow: hidden;

  & .sidebar-logo-link {
    display: flex;
    height: 100%;
    width: 100%;
    align-items: center;
    justify-content: center;
    gap: 12px;

    & .sidebar-logo {
      width: 32px;
      height: 32px;
      margin: 0;
      flex: 0 0 auto;
    }

    & .sidebar-title {
      display: inline-block;
      margin: 0;
      color: v-bind(getLogoTextColor);
      font-weight: 600;
      line-height: 50px;
      font-size: 14px;
      font-family:
        Avenir,
        Helvetica Neue,
        Arial,
        Helvetica,
        sans-serif;
      flex: 0 1 auto;
    }
  }

  &.collapse {
    .sidebar-logo {
      margin: 0;
    }
  }
}
</style>
