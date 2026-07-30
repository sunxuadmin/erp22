<template>
  <el-breadcrumb class="app-breadcrumb" separator="/">
    <transition-group name="breadcrumb">
      <el-breadcrumb-item v-for="(item, index) in levelList" :key="item.path">
        <span v-if="item.redirect === 'noRedirect' || index == levelList.length - 1" class="no-redirect">{{ item.meta?.title }}</span>
        <a v-else @click.prevent="handleLink(item)">{{ item.meta?.title }}</a>
      </el-breadcrumb-item>
    </transition-group>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import { RouteLocationMatched } from 'vue-router';
import { usePermissionStore } from '@/store/modules/permission';

const route = useRoute();
const router = useRouter();
const permissionStore = usePermissionStore();
const levelList = ref<RouteLocationMatched[]>([]);

const getBreadcrumb = () => {
  // only show routes with meta.title
  let matched = [];
  const pathNum = findPathNum(route.path);
  // multi-level menu
  if (pathNum > 2) {
    const pathList = getPathList(route.path);
    getMatched(pathList, permissionStore.defaultRoutes, matched);
    if (!matched.length) {
      matched = route.matched.filter((item) => item.meta && item.meta.title);
    }
  } else {
    matched = route.matched.filter((item) => item.meta && item.meta.title);
  }
  matched = simplifySchoolReportingBreadcrumb(matched);
  // 判断是否为首页
  if (!isDashboard(matched[0])) {
    matched = [{ path: '/index', meta: { title: '首页' } }].concat(matched);
  }
  levelList.value = matched.filter((item) => item.meta && item.meta.title && item.meta.breadcrumb !== false);
};
const findPathNum = (str, char = '/') => {
  if (typeof str !== 'string' || str.length === 0) return 0;
  return str.split(char).length - 1;
};
const getPathList = (path: string) => path.split('/').filter(Boolean);
const getMatched = (pathList, routeList, matched) => {
  const currentPath = pathList.shift();
  if (!currentPath) return;
  const currentPathKey = trimPath(currentPath).toLowerCase();
  const data = routeList.find(
    (item) => trimPath(item.path).toLowerCase() === currentPathKey || String(item.name || '').toLowerCase() === currentPathKey
  );
  if (data) {
    matched.push(data);
    if (data.children && pathList.length) {
      getMatched(pathList, data.children, matched);
    }
  }
};
const simplifySchoolReportingBreadcrumb = (matched) => {
  if (!isSchoolReportingCategoryRoute(matched)) {
    return matched;
  }
  const root = matched.find((item) => trimPath(item.path) === 'crehn') || matched[0];
  const leaf = matched[matched.length - 1];
  return root && leaf && root !== leaf ? [root, leaf] : matched;
};
const isSchoolReportingCategoryRoute = (matched) => {
  const leaf = matched[matched.length - 1];
  return route.path.startsWith('/crehn/') && trimPath(leaf?.path).startsWith('category-') && Boolean(leaf?.meta?.title);
};
const trimPath = (path?: string) => String(path || '').replace(/^\/+|\/+$/g, '');
const isDashboard = (route: RouteLocationMatched) => {
  const name = route && (route.name as string);
  if (!name) {
    return false;
  }
  return name.trim() === 'Index';
};
const handleLink = (item) => {
  const { redirect, path } = item;
  redirect ? router.push(redirect) : router.push(path);
};

watchEffect(() => {
  // if you go to the redirect page, do not update the breadcrumbs
  if (route.path.startsWith('/redirect/')) return;
  getBreadcrumb();
});
onMounted(() => {
  getBreadcrumb();
});
</script>

<style lang="scss" scoped>
.app-breadcrumb.el-breadcrumb {
  display: inline-block;
  font-size: 14px;
  line-height: 50px;
  margin-left: 0;

  :deep(.el-breadcrumb__separator) {
    color: #9cb3c9;
    margin: 0 8px;
  }

  :deep(.el-breadcrumb__inner a),
  :deep(.el-breadcrumb__inner .no-redirect) {
    padding: 6px 12px;
    color: #42627f;
    font-weight: 700;
    border: 1px solid transparent;
    border-radius: 999px;
    transition: all 0.2s ease;
  }

  :deep(.el-breadcrumb__inner a:hover) {
    color: var(--el-color-primary);
    background: #eef6ff;
    border-color: #d5eaff;
  }

  .no-redirect {
    color: #7c93aa;
    background: #f6f9fd;
    border-color: #edf2f7;
    cursor: text;
  }
}
</style>
