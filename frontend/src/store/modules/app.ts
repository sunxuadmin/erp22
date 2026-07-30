import zhCN from 'element-plus/es/locale/lang/zh-cn';
import enUS from 'element-plus/es/locale/lang/en';
import { defineStore } from 'pinia';
import { useStorage } from '@vueuse/core';
import { ref, reactive, computed } from 'vue';
import {
  LAYOUT_DISPLAY_SCALE_DEFAULT_CUSTOM,
  normalizeLayoutDisplayScaleMode,
  normalizeLayoutDisplayScalePercent,
  resolveLayoutDisplayScalePercent,
  type LayoutDisplayScaleMode
} from '@/utils/layoutDisplayScale';

type SidebarReportGroupOpenRequest = {
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

export const useAppStore = defineStore('app', () => {
  const sidebarStatus = useStorage('sidebarStatus', '0');
  const sidebar = reactive({
    opened: sidebarStatus.value === '1',
    withoutAnimation: false,
    hide: false
  });
  const sidebarReportGroupRequest = ref<SidebarReportGroupOpenRequest>();
  let sidebarReportGroupRequestSeed = 0;
  const device = ref<string>('desktop');
  const size = ref<'default'>('default');
  const layoutDisplayScaleMode = ref<LayoutDisplayScaleMode>('default');
  const layoutDisplayCustomScalePercent = ref(LAYOUT_DISPLAY_SCALE_DEFAULT_CUSTOM);
  const layoutDisplayScalePercent = ref(100);

  // 语言
  const language = useStorage('language', 'zh_CN');
  const languageObj: any = {
    en_US: enUS,
    zh_CN: zhCN
  };
  const locale = computed(() => {
    return languageObj[language.value];
  });

  const toggleSideBar = (withoutAnimation: boolean) => {
    if (sidebar.hide) {
      return false;
    }

    sidebar.opened = !sidebar.opened;
    sidebar.withoutAnimation = withoutAnimation;
    if (sidebar.opened) {
      sidebarStatus.value = '1';
    } else {
      sidebarStatus.value = '0';
    }
  };

  const closeSideBar = ({ withoutAnimation }: any): void => {
    sidebarStatus.value = '0';
    sidebar.opened = false;
    sidebar.withoutAnimation = withoutAnimation;
  };
  const toggleDevice = (d: string): void => {
    device.value = d;
  };
  const setSize = (s: 'large' | 'default' | 'small'): void => {
    layoutDisplayScaleMode.value = s;
  };
  const configureLayoutDisplayScale = (mode: unknown, customScalePercent: unknown): void => {
    layoutDisplayScaleMode.value = normalizeLayoutDisplayScaleMode(mode);
    layoutDisplayCustomScalePercent.value = normalizeLayoutDisplayScalePercent(customScalePercent);
  };
  const setLayoutDisplayScaleMode = (mode: LayoutDisplayScaleMode): void => {
    layoutDisplayScaleMode.value = normalizeLayoutDisplayScaleMode(mode);
  };
  const setLayoutDisplayCustomScalePercent = (percent: number): void => {
    layoutDisplayCustomScalePercent.value = normalizeLayoutDisplayScalePercent(percent);
  };
  const updateLayoutDisplayScale = (viewportWidth: number, sidebarWidth: number, mobile: boolean): void => {
    layoutDisplayScalePercent.value = resolveLayoutDisplayScalePercent({
      mode: layoutDisplayScaleMode.value,
      customScalePercent: layoutDisplayCustomScalePercent.value,
      viewportWidth,
      sidebarWidth,
      mobile
    });
  };
  const resetLayoutDisplayScale = (): void => {
    layoutDisplayScaleMode.value = 'default';
    layoutDisplayCustomScalePercent.value = LAYOUT_DISPLAY_SCALE_DEFAULT_CUSTOM;
    layoutDisplayScalePercent.value = 100;
  };
  const toggleSideBarHide = (status: boolean): void => {
    sidebar.hide = status;
  };

  const requestSidebarReportGroupOpen = (payload: Omit<SidebarReportGroupOpenRequest, 'requestId'>): void => {
    if (sidebar.hide) {
      return;
    }
    sidebar.opened = true;
    sidebar.withoutAnimation = false;
    sidebarStatus.value = '1';
    sidebarReportGroupRequest.value = {
      requestId: ++sidebarReportGroupRequestSeed,
      ...payload
    };
  };

  const changeLanguage = (val: string): void => {
    language.value = val;
  };

  return {
    device,
    sidebar,
    sidebarReportGroupRequest,
    language,
    locale,
    size,
    layoutDisplayScaleMode,
    layoutDisplayCustomScalePercent,
    layoutDisplayScalePercent,
    changeLanguage,
    toggleSideBar,
    closeSideBar,
    toggleDevice,
    setSize,
    configureLayoutDisplayScale,
    setLayoutDisplayScaleMode,
    setLayoutDisplayCustomScalePercent,
    updateLayoutDisplayScale,
    resetLayoutDisplayScale,
    toggleSideBarHide,
    requestSidebarReportGroupOpen
  };
});
