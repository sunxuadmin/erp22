import { readonly, ref } from 'vue';
import { getConfigKey } from '@/api/system/config';

const enabled = ref(false);
const title = ref('工作台');
const textColor = ref('');
const backgroundColor = ref('');
const loaded = ref(false);
let loadingPromise: Promise<void> | null = null;

const readConfig = async (key: string, fallback: string) => {
  try {
    const res = await getConfigKey(key);
    const value = String(res?.data ?? '').trim();
    return value || fallback;
  } catch {
    return fallback;
  }
};

const isEnabledValue = (value: string) => ['true', '1', 'yes', 'on', 'enabled'].includes(value.trim().toLowerCase());

const loadSidebarLogoConfig = async () => {
  if (loadingPromise) return loadingPromise;
  loadingPromise = Promise.all([
    readConfig('crehn.sidebar.logo.enabled', 'false'),
    readConfig('crehn.sidebar.logo.text', '工作台'),
    readConfig('crehn.sidebar.logo.textColor', ''),
    readConfig('crehn.sidebar.logo.backgroundColor', '')
  ]).then(([enabledValue, titleValue, textColorValue, backgroundColorValue]) => {
    enabled.value = isEnabledValue(enabledValue);
    title.value = titleValue || '工作台';
    textColor.value = textColorValue;
    backgroundColor.value = backgroundColorValue;
    loaded.value = true;
  }).finally(() => {
    loadingPromise = null;
  });
  return loadingPromise;
};

export const useSidebarLogoConfig = () => {
  if (!loaded.value) {
    void loadSidebarLogoConfig();
  }

  return {
    enabled: readonly(enabled),
    title: readonly(title),
    textColor: readonly(textColor),
    backgroundColor: readonly(backgroundColor),
    reload: loadSidebarLogoConfig
  };
};
