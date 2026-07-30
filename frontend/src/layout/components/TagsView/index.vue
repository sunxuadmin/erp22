<template>
  <div id="tags-view-container" class="tags-view-container">
    <scroll-pane ref="scrollPaneRef" class="tags-view-wrapper" @scroll="handleScroll">
      <router-link
        v-for="tag in displayedVisitedViews"
        :key="tag.path"
        :data-path="tag.path"
        :class="{ 'active': isActive(tag), 'has-icon': tagsIcon }"
        :to="{ path: tag.path || '', query: tag.query }"
        class="tags-view-item"
        :style="activeStyle(tag)"
        @click.middle="!isAffix(tag) ? closeSelectedTag(tag) : ''"
        @contextmenu.prevent="openMenu(tag, $event)"
      >
        <svg-icon v-if="tagsIcon && tag.meta && tag.meta.icon && tag.meta.icon !== '#'" :icon-class="tag.meta.icon" />
        <span class="tags-view-item-title">{{ tag.title }}</span>
        <span v-if="!isAffix(tag)" @click.prevent.stop="closeSelectedTag(tag)">
          <close class="el-icon-close" style="width: 1em; height: 1em; vertical-align: middle" />
        </span>
      </router-link>
    </scroll-pane>
    <ul v-show="visible" :style="{ left: left + 'px', top: top + 'px' }" class="contextmenu">
      <li @click="refreshSelectedTag(selectedTag)"><refresh-right style="width: 1em; height: 1em" /> 刷新页面</li>
      <li v-if="!isAffix(selectedTag)" @click="closeSelectedTag(selectedTag)"><close style="width: 1em; height: 1em" /> 关闭当前</li>
      <li @click="closeOthersTags"><circle-close style="width: 1em; height: 1em" /> 关闭其他</li>
      <li v-if="!isFirstView()" @click="closeLeftTags"><back style="width: 1em; height: 1em" /> 关闭左侧</li>
      <li v-if="!isLastView()" @click="closeRightTags"><right style="width: 1em; height: 1em" /> 关闭右侧</li>
      <li @click="closeAllTags(selectedTag)"><circle-close style="width: 1em; height: 1em" /> 全部关闭</li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import ScrollPane from './ScrollPane.vue';
import { getNormalPath } from '@/utils/ruoyi';
import { useSettingsStore } from '@/store/modules/settings';
import { usePermissionStore } from '@/store/modules/permission';
import { useTagsViewStore } from '@/store/modules/tagsView';
import { RouteRecordRaw, RouteLocationNormalized } from 'vue-router';

const visible = ref(false);
const top = ref(0);
const left = ref(0);
const selectedTag = ref<RouteLocationNormalized>();
const affixTags = ref<RouteLocationNormalized[]>([]);
const scrollPaneRef = ref<InstanceType<typeof ScrollPane>>();

const { proxy } = getCurrentInstance() as ComponentInternalInstance;
const route = useRoute();
const router = useRouter();

const props = withDefaults(defineProps<{ hideHomeTag?: boolean }>(), {
  hideHomeTag: false
});

const visitedViews = computed(() => useTagsViewStore().getVisitedViews());
const isHomeTag = (tag: RouteLocationNormalized) => tag.path === '/index' || tag.name === 'Index' || tag.name === 'Dashboard';
const displayedVisitedViews = computed(() => (props.hideHomeTag ? visitedViews.value.filter((tag) => !isHomeTag(tag)) : visitedViews.value));
const routes = computed(() => usePermissionStore().getRoutes());
const theme = computed(() => useSettingsStore().theme);
const tagsIcon = computed(() => useSettingsStore().tagsIcon);

watch(route, () => {
  addTags();
  moveToCurrentTag();
});
watch(visible, (value) => {
  if (value) {
    document.body.addEventListener('click', closeMenu);
  } else {
    document.body.removeEventListener('click', closeMenu);
  }
});

const isActive = (r: RouteLocationNormalized): boolean => {
  return r.path === route.path;
};
const activeStyle = (tag: RouteLocationNormalized) => {
  if (!isActive(tag)) return {};
  return {};
};
const isAffix = (tag: RouteLocationNormalized) => {
  return tag?.meta && tag?.meta?.affix;
};
const isFirstView = () => {
  try {
    return selectedTag.value.fullPath === '/index' || selectedTag.value.fullPath === visitedViews.value[1].fullPath;
  } catch (err) {
    return false;
  }
};
const isLastView = () => {
  try {
    return selectedTag.value.fullPath === visitedViews.value[visitedViews.value.length - 1].fullPath;
  } catch (err) {
    return false;
  }
};
const filterAffixTags = (routes: RouteRecordRaw[], basePath = '') => {
  let tags: RouteLocationNormalized[] = [];

  routes.forEach((route) => {
    if (route.meta && route.meta.affix) {
      const tagPath = getNormalPath(basePath + '/' + route.path);
      tags.push({
        hash: '',
        matched: [],
        params: undefined,
        query: undefined,
        redirectedFrom: undefined,
        fullPath: tagPath,
        path: tagPath,
        name: route.name as string,
        meta: { ...route.meta }
      });
    }
    if (route.children) {
      const tempTags = filterAffixTags(route.children, route.path);
      if (tempTags.length >= 1) {
        tags = [...tags, ...tempTags];
      }
    }
  });
  return tags;
};
const initTags = () => {
  const res = filterAffixTags(routes.value);
  affixTags.value = res;
  for (const tag of res) {
    // Must have tag name
    if (tag.name) {
      useTagsViewStore().addVisitedView(tag);
    }
  }
};
const addTags = () => {
  const { name } = route;
  if (route.query.title) {
    route.meta.title = route.query.title as string;
  }
  if (name) {
    useTagsViewStore().addView(route as any);
  }
};
const moveToCurrentTag = () => {
  nextTick(() => {
    for (const r of visitedViews.value) {
      if (r.path === route.path) {
        scrollPaneRef.value?.moveToTarget(r);
        // when query is different then update
        if (r.fullPath !== route.fullPath) {
          useTagsViewStore().updateVisitedView(route);
        }
      }
    }
  });
};
const refreshSelectedTag = (view: RouteLocationNormalized) => {
  proxy?.$tab.refreshPage(view);
  if (route.meta.link) {
    useTagsViewStore().delIframeView(route);
  }
};
const needsCloseConfirm = (view?: RouteLocationNormalized) => Boolean(view?.meta?.closeConfirm);
const closeConfirmMessage = (view?: RouteLocationNormalized) => {
  return (view?.meta?.closeConfirmMessage as string) || '关闭当前页前请确认已保存。确认继续关闭？';
};
const confirmCloseTag = async (view?: RouteLocationNormalized) => {
  if (!needsCloseConfirm(view)) return true;
  try {
    await proxy?.$modal.confirm(closeConfirmMessage(view));
    return true;
  } catch {
    return false;
  }
};
const confirmCloseViews = async (views: RouteLocationNormalized[]) => {
  const view = views.find((item) => needsCloseConfirm(item) && !isActive(item));
  if (!view) return true;
  try {
    await proxy?.$modal.confirm(closeConfirmMessage(view));
    return true;
  } catch {
    return false;
  }
};
const containsActiveView = (views: RouteLocationNormalized[]) => views.some((item) => isActive(item));
const sameView = (left: RouteLocationNormalized, right: RouteLocationNormalized) => left.path === right.path;
const navigateAwayBeforeClosingActive = async (remainingViews: RouteLocationNormalized[], fallbackView?: RouteLocationNormalized) => {
  const fromFullPath = route.fullPath;
  await toLastView(remainingViews, fallbackView).catch(() => {});
  return route.fullPath !== fromFullPath;
};
const closeSelectedTag = async (view: RouteLocationNormalized) => {
  if (isActive(view)) {
    const remainingViews = visitedViews.value.filter((item) => item.path !== view.path);
    if (!(await navigateAwayBeforeClosingActive(remainingViews, view))) return;
    await proxy?.$tab.closePage(view);
    return;
  }
  if (!(await confirmCloseTag(view))) return;
  proxy?.$tab.closePage(view).then(({ visitedViews }: any) => {
    if (isActive(view)) {
      toLastView(visitedViews, view);
    }
  });
};
const closeRightTags = async () => {
  const selectedIndex = visitedViews.value.findIndex((item) => item.fullPath === selectedTag.value?.fullPath);
  const closingViews = selectedIndex >= 0 ? visitedViews.value.slice(selectedIndex + 1).filter((item) => !isAffix(item)) : [];
  if (!(await confirmCloseViews(closingViews))) return;
  if (containsActiveView(closingViews)) {
    const remainingViews = visitedViews.value.filter((item) => !closingViews.some((closingView) => sameView(closingView, item)));
    if (!(await navigateAwayBeforeClosingActive(remainingViews, selectedTag.value))) return;
  }
  proxy?.$tab.closeRightPage(selectedTag.value).then((visitedViews: RouteLocationNormalized[]) => {
    if (!visitedViews.find((i: RouteLocationNormalized) => i.fullPath === route.fullPath)) {
      toLastView(visitedViews);
    }
  });
};
const closeLeftTags = async () => {
  const selectedIndex = visitedViews.value.findIndex((item) => item.fullPath === selectedTag.value?.fullPath);
  const closingViews = selectedIndex >= 0 ? visitedViews.value.slice(0, selectedIndex).filter((item) => !isAffix(item)) : [];
  if (!(await confirmCloseViews(closingViews))) return;
  if (containsActiveView(closingViews)) {
    const remainingViews = visitedViews.value.filter((item) => !closingViews.some((closingView) => sameView(closingView, item)));
    if (!(await navigateAwayBeforeClosingActive(remainingViews, selectedTag.value))) return;
  }
  proxy?.$tab.closeLeftPage(selectedTag.value).then((visitedViews: RouteLocationNormalized[]) => {
    if (!visitedViews.find((i: RouteLocationNormalized) => i.fullPath === route.fullPath)) {
      toLastView(visitedViews);
    }
  });
};
const closeOthersTags = async () => {
  if (!selectedTag.value) return;
  const closingViews = visitedViews.value.filter((item) => item.fullPath !== selectedTag.value?.fullPath && !isAffix(item));
  if (!(await confirmCloseViews(closingViews))) return;
  const selectedTarget = selectedTag.value.fullPath || selectedTag.value.path || '/';
  if (containsActiveView(closingViews)) {
    const fromFullPath = route.fullPath;
    await router.push(selectedTarget).catch(() => {});
    if (route.fullPath === fromFullPath) return;
  } else {
    router.push(selectedTarget).catch(() => {});
  }
  proxy?.$tab.closeOtherPage(selectedTag.value).then(() => {
    moveToCurrentTag();
  });
};
const closeAllTags = async (view: RouteLocationNormalized) => {
  const closingViews = visitedViews.value.filter((item) => !isAffix(item));
  if (!(await confirmCloseViews(closingViews))) return;
  if (containsActiveView(closingViews)) {
    const remainingViews = visitedViews.value.filter((item) => isAffix(item));
    if (!(await navigateAwayBeforeClosingActive(remainingViews, view))) return;
  }
  proxy?.$tab.closeAllPage().then(({ visitedViews }) => {
    if (affixTags.value.some((tag) => tag.path === route.path)) {
      return;
    }
    toLastView(visitedViews, view);
  });
};
const toLastView = (visitedViews: RouteLocationNormalized[], view?: RouteLocationNormalized) => {
  const latestView = visitedViews.slice(-1)[0];
  if (latestView) {
    return router.push(latestView.fullPath as string);
  } else {
    // now the default is to redirect to the home page if there is no tags-view,
    // you can adjust it according to your needs.
    if (view?.name === 'Dashboard') {
      // to reload home page
      return router.replace({ path: '/redirect' + view?.fullPath });
    } else {
      return router.push('/');
    }
  }
};
const openMenu = (tag: RouteLocationNormalized, e: MouseEvent) => {
  const menuMinWidth = 105;
  const offsetLeft = proxy?.$el.getBoundingClientRect().left; // container margin left
  const offsetWidth = proxy?.$el.offsetWidth; // container width
  const maxLeft = offsetWidth - menuMinWidth; // left boundary
  const l = e.clientX - offsetLeft + 15; // 15: margin right

  if (l > maxLeft) {
    left.value = maxLeft;
  } else {
    left.value = l;
  }

  top.value = e.clientY;
  visible.value = true;
  selectedTag.value = tag;
};
const closeMenu = () => {
  visible.value = false;
};
const handleScroll = () => {
  closeMenu();
};

onMounted(() => {
  initTags();
  addTags();
});
</script>

<style lang="scss" scoped>
.tags-view-container {
  height: 42px;
  width: 100%;
  background: #f7faff;
  border-top: none;
  border-bottom: 1px solid #d9e6f6;
  box-shadow: none;
  .tags-view-wrapper {
    height: 42px;
    display: flex;
    align-items: center;
    .tags-view-item {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      position: relative;
      box-sizing: border-box;
      cursor: pointer;
      height: 32px;
      line-height: 1;
      background-color: #ffffff;
      border: 1px solid #d8e6f5;
      color: #39526d;
      padding: 0 12px;
      font-size: 14px;
      font-weight: 700;
      margin-left: 8px;
      margin-top: 0;
      border-radius: 8px;
      vertical-align: middle;
      box-shadow: none;
      transition:
        box-shadow 0.2s ease,
        transform 0.2s ease,
        border-color 0.2s ease,
        color 0.2s ease,
        background 0.2s ease;
      &:hover {
        color: var(--el-color-primary);
        border-color: #bcd2f2;
        background: #eef5ff;
        box-shadow: none;
        transform: none;
      }
      &:first-of-type {
        margin-left: 12px;
      }
      &:last-of-type {
        margin-right: 15px;
      }
      &.active {
        background: #2563eb;
        color: #fff;
        border-color: #2563eb;
        box-shadow: none;
        &::before {
          content: '';
          flex: 0 0 auto;
          background: rgba(255, 255, 255, 0.86);
          display: block;
          width: 7px;
          height: 7px;
          border-radius: 50%;
          margin-right: 0;
        }
      }

      > span {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        line-height: 1;
      }
    }
  }
  .tags-view-item.active.has-icon::before {
    content: none !important;
  }
  .tags-view-item-title {
    display: inline-flex;
    align-items: center;
    height: auto;
    line-height: 1;
    margin-left: 0;
    margin-right: 0;
  }
  .contextmenu {
    margin: 0;
    background: var(--el-bg-color);
    z-index: 3000;
    position: absolute;
    list-style-type: none;
    padding: 5px 0;
    border-radius: var(--app-radius-md);
    font-size: 13px;
    font-weight: 400;
    box-shadow: var(--app-shadow-md);
    li {
      margin: 0;
      padding: 7px 16px;
      cursor: pointer;
      &:hover {
        background: var(--el-fill-color-light);
      }
    }
  }
}
</style>

<style lang="scss">
//reset element css of el-icon-close
.tags-view-wrapper {
  .tags-view-item {
    .el-icon-close {
      box-sizing: border-box;
      width: 18px !important;
      height: 18px !important;
      padding: 3px;
      margin-left: 0;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      flex: 0 0 18px;
      vertical-align: middle;
      border-radius: 50%;
      text-align: center;
      color: currentColor;
      transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
      transform-origin: 100% 50%;
      &:before {
        transform: scale(0.6);
        display: block;
        vertical-align: 0;
      }
      &:hover {
        background-color: rgba(255, 255, 255, 0.28);
        color: #ffffff;
      }
    }

    &:not(.active) {
      .el-icon-close:hover {
        background-color: #e7f0ff;
        color: var(--el-color-primary);
      }
    }
  }
}
</style>
