import { defineStore } from 'pinia';
import router, { constantRoutes, dynamicRoutes } from '@/router';
import store from '@/store';
import { getRouters } from '@/api/menu';
import { listAvailableActivity, listSchoolCategoryCatalog } from '@/api/crehn/activity';
import type { ActivityCategoryVO, ActivityVO } from '@/api/crehn/types';
import { ActivityCategoryTreeNode, buildSchoolCategoryMenuTree, isCategoryGroup } from '@/utils/artCategory';
import auth from '@/plugins/auth';
import { RouteRecordRaw } from 'vue-router';
import Layout from '@/layout/index.vue';
import ParentView from '@/components/ParentView/index.vue';
import InnerLink from '@/layout/components/InnerLink/index.vue';
import { ref } from 'vue';
import { createCustomNameComponent } from '@/utils/createCustomNameComponent';
import { useUserStore } from '@/store/modules/user';

type ReportMenuStyle = {
  groupIcon?: string;
  groupColor?: string;
  categoryIcon?: string;
  categoryColor?: string;
};

// 匹配views里面所有的.vue文件
const modules = import.meta.glob('./../../views/**/*.vue');
export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<RouteRecordRaw[]>([]);
  const addRoutes = ref<RouteRecordRaw[]>([]);
  const defaultRoutes = ref<RouteRecordRaw[]>([]);
  const topbarRouters = ref<RouteRecordRaw[]>([]);
  const sidebarRouters = ref<RouteRecordRaw[]>([]);

  const getRoutes = (): RouteRecordRaw[] => {
    return routes.value as RouteRecordRaw[];
  };
  const getDefaultRoutes = (): RouteRecordRaw[] => {
    return defaultRoutes.value as RouteRecordRaw[];
  };
  const getSidebarRoutes = (): RouteRecordRaw[] => {
    return sidebarRouters.value as RouteRecordRaw[];
  };
  const getTopbarRoutes = (): RouteRecordRaw[] => {
    return topbarRouters.value as RouteRecordRaw[];
  };

  const setRoutes = (newRoutes: RouteRecordRaw[]): void => {
    addRoutes.value = newRoutes;
    routes.value = constantRoutes.concat(newRoutes);
  };
  const setDefaultRoutes = (routes: RouteRecordRaw[]): void => {
    defaultRoutes.value = constantRoutes.concat(routes);
  };
  const setTopbarRoutes = (routes: RouteRecordRaw[]): void => {
    topbarRouters.value = routes;
  };
  const setSidebarRouters = (routes: RouteRecordRaw[]): void => {
    sidebarRouters.value = routes;
  };
  const generateRoutes = async (): Promise<RouteRecordRaw[]> => {
    const res = await getRouters();
    const data = await withArtReviewMenuAdjustments(res.data || []);
    const sdata = JSON.parse(JSON.stringify(data));
    const rdata = JSON.parse(JSON.stringify(data));
    const defaultData = JSON.parse(JSON.stringify(data));
    const sidebarRoutes = filterAsyncRouter(sdata);
    const rewriteRoutes = filterAsyncRouter(rdata, undefined, true);
    const defaultRoutes = filterAsyncRouter(defaultData);
    const asyncRoutes = filterDynamicRoutes(dynamicRoutes);
    asyncRoutes.forEach((route) => {
      router.addRoute(route);
    });
    setRoutes(rewriteRoutes);
    setSidebarRouters(constantRoutes.concat(sidebarRoutes));
    setDefaultRoutes(sidebarRoutes);
    setTopbarRoutes(defaultRoutes);
    // 路由name重复检查
    duplicateRouteChecker(asyncRoutes, sidebarRoutes);
    return new Promise<RouteRecordRaw[]>((resolve) => resolve(rewriteRoutes));
  };

  /**
   * 遍历后台传来的路由字符串，转换为组件对象
   * @param asyncRouterMap 后台传来的路由字符串
   * @param lastRouter 上一级路由
   * @param type 是否是重写路由
   */
  const filterAsyncRouter = (asyncRouterMap: RouteRecordRaw[], lastRouter?: RouteRecordRaw, type = false): RouteRecordRaw[] => {
    return asyncRouterMap.filter((route) => {
      if (type && route.children) {
        route.children = filterChildren(route.children, undefined);
      }
      // Layout ParentView 组件特殊处理
      if (route.component?.toString() === 'Layout') {
        route.component = Layout;
      } else if (route.component?.toString() === 'ParentView') {
        route.component = ParentView;
      } else if (route.component?.toString() === 'InnerLink') {
        route.component = InnerLink;
      } else {
        route.component = loadView(route.component, route.name as string);
      }
      if (route.children != null && route.children && route.children.length) {
        route.children = filterAsyncRouter(route.children, route, type);
      } else {
        delete route.children;
        delete route.redirect;
      }
      return true;
    });
  };
  const filterChildren = (childrenMap: RouteRecordRaw[], lastRouter?: RouteRecordRaw): RouteRecordRaw[] => {
    let children: RouteRecordRaw[] = [];
    childrenMap.forEach((el) => {
      el.path = lastRouter ? lastRouter.path + '/' + el.path : el.path;
      if (el.children && el.children.length && el.component?.toString() === 'ParentView') {
        children = children.concat(filterChildren(el.children, el));
      } else {
        children.push(el);
      }
    });
    return children;
  };
  return {
    routes,
    topbarRouters,
    sidebarRouters,
    defaultRoutes,

    getRoutes,
    getDefaultRoutes,
    getSidebarRoutes,
    getTopbarRoutes,

    setRoutes,
    generateRoutes,
    setSidebarRouters
  };
});

// 动态路由遍历，验证是否具备权限
export const filterDynamicRoutes = (routes: RouteRecordRaw[]) => {
  const res: RouteRecordRaw[] = [];
  routes.forEach((route) => {
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) {
        res.push(route);
      }
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) {
        res.push(route);
      }
    }
  });
  return res;
};

export const loadView = (view: any, name: string) => {
  let res;
  for (const path in modules) {
    const viewsIndex = path.indexOf('/views/');
    let dir = path.substring(viewsIndex + 7);
    dir = dir.substring(0, dir.lastIndexOf('.vue'));
    if (dir === view) {
      res = createCustomNameComponent(modules[path], { name });
      return res;
    }
  }
  return res;
};

const projectListComponent = 'crehn/project/index';
const projectViewPath = 'project-view';
const projectViewComponent = 'crehn/project-view/index';
const projectAuditPath = 'audit';
const projectAuditComponent = 'crehn/audit/index';
const artReviewRootPath = 'crehn';
const participantReportingRootPath = 'project-submission';
const legacyM70CategoryGroupPaths = new Set(['performance', 'artwork', 'workshop', 'achievement']);
const legacyM70CategoryLeafCodesByParentPath: Record<string, Record<string, string>> = {
  performance: {
    vocal: 'performance_vocal',
    instrumental: 'performance_instrumental',
    dance: 'performance_dance',
    drama: 'performance_drama',
    recitation: 'performance_recitation',
    personal: 'performance_personal'
  },
  artwork: {
    'fine-art': 'artwork_fine_art',
    'design-exhibition': 'artwork_grand_design',
    design: 'artwork_design',
    video: 'artwork_film'
  },
  workshop: { 'workshop-item': 'workshop' },
  achievement: { paper: 'achievement_paper', 'teaching-case': 'achievement_case' }
};
const legacyM70PrincipalCategoryCode = 'artwork_principal';

const reportGroupDefs = [
  { key: 'performance', label: '艺术表演类', aliases: ['艺术表演类', '表演类节目', '艺术表演类节目', '表演'] },
  { key: 'artwork', label: '艺术作品类', aliases: ['艺术作品类', '艺术作品', '作品'] },
  { key: 'workshop', label: '艺术实践工作坊', aliases: ['艺术实践工作坊', '工作坊'] },
  {
    key: 'achievement',
    label: '高校美育改革创新优秀成果',
    aliases: ['高校美育改革创新优秀成果', '美育改革创新优秀成果', '高校美育改革创新成果', '优秀成果', '美育']
  },
  { key: 'principal', label: '高校校长书画作品', aliases: ['高校校长书画作品', '校长书画'] }
];
const withArtReviewMenuAdjustments = async (routes: RouteRecordRaw[]): Promise<RouteRecordRaw[]> => {
  const artRoot = findArtReviewRoute(routes);
  if (artRoot) {
    artRoot.children = stripStaticActivityCategoryMenus(normalizeProjectViewMenus(artRoot.children || []), trimPath(artRoot.path));
  }
  return withParticipantReportingMenus(routes);
};

const withParticipantReportingMenus = async (routes: RouteRecordRaw[]): Promise<RouteRecordRaw[]> => {
  if (!currentUserCanUseParticipantProjectMenus()) {
    return routes;
  }
  const artRoot = findArtReviewRoute(routes);
  if (!artRoot) {
    return routes;
  }
  const reportingMenu = await buildParticipantReportingMenu();
  if (reportingMenu) {
    artRoot.children = [reportingMenu, ...(artRoot.children || [])];
  }
  return routes;
};

export const isParticipantProjectUser = (roleKeys?: readonly string[], hasProjectAddPermission?: boolean) => {
  const normalizedRoles = [...new Set((roleKeys || []).map((role) => String(role).trim().toLowerCase()).filter(Boolean))];
  return Boolean(hasProjectAddPermission) && normalizedRoles.length === 1 && normalizedRoles[0] === 'crehn_participant';
};

export const canUseParticipantProjectMenus = (
  roleKeys: readonly string[] | undefined,
  hasProjectAddPermission: boolean,
  schoolId: string | number | undefined
) => Boolean(schoolId) && isParticipantProjectUser(roleKeys, hasProjectAddPermission);

const currentUserCanUseParticipantProjectMenus = () => {
  const userStore = useUserStore();
  return canUseParticipantProjectMenus(userStore.roles, auth.hasPermi('crehn:project:add'), userStore.schoolId);
};

const ensureProjectAuditMenuForProjectViewers = (artRoot: RouteRecordRaw) => {
  if (!auth.hasPermi('crehn:projectView:list')) {
    return;
  }
  const children = artRoot.children || [];
  const hasAuditMenu = children.some((route) => trimPath(route.path) === projectAuditPath || String(route.component || '') === projectAuditComponent);
  if (hasAuditMenu) {
    return;
  }
  artRoot.children = [
    ...children,
    {
      path: projectAuditPath,
      component: projectAuditComponent as any,
      name: 'ArtAuditProjectView',
      meta: { title: '项目审核', icon: 'clipboard' },
      query: JSON.stringify({ browse: 'category' })
    }
  ];
};

const findArtReviewRoute = (routes: RouteRecordRaw[]) => {
  return routes.find((route) => trimPath(route.path) === artReviewRootPath);
};

const normalizeProjectViewMenus = (routes: RouteRecordRaw[]): RouteRecordRaw[] => {
  return routes.map((route) => {
    const normalized: RouteRecordRaw = {
      ...route,
      children: route.children ? normalizeProjectViewMenus(route.children) : route.children
    };
    if (isProjectViewMenu(route)) {
      (normalized as any).hidden = false;
      normalized.meta = { ...(normalized.meta || {}), title: '上报进度', icon: normalized.meta?.icon || 'eye-open' };
    }
    return normalized;
  });
};

const isProjectViewMenu = (route: RouteRecordRaw) => {
  const title = String(route.meta?.title || '');
  return (
    trimPath(route.path) === projectViewPath ||
    String(route.component || '') === projectViewComponent ||
    title === '作品上报查看' ||
    title === '上报进度'
  );
};

export const stripStaticActivityCategoryMenus = (routes: RouteRecordRaw[], parentPath = ''): RouteRecordRaw[] => {
  return routes
    .filter((route) => !isLegacyStaticActivityCategoryMenu(route, parentPath))
    .map((route) => ({
      ...route,
      children: route.children ? stripStaticActivityCategoryMenus(route.children, trimPath(route.path)) : route.children
    }));
};

const isLegacyStaticActivityCategoryMenu = (route: RouteRecordRaw, parentPath: string) => {
  const path = trimPath(route.path);
  const query = parseRouteJsonObject(String((route as any).query || ''));
  const categoryCode = String(query.categoryCode || '');
  if (parentPath === artReviewRootPath && legacyM70CategoryGroupPaths.has(path)) {
    return true;
  }
  if (parentPath === artReviewRootPath && path === 'principal') {
    return categoryCode === legacyM70PrincipalCategoryCode;
  }
  return legacyM70CategoryLeafCodesByParentPath[parentPath]?.[path] === categoryCode;
};

const buildParticipantReportingMenu = async (): Promise<RouteRecordRaw | undefined> => {
  try {
    const activityRes = await listAvailableActivity();
    const activities = activityRes.data || [];
    const activityCatalogs = (
      await Promise.all(
        activities
          .filter((activity) => activity.id !== undefined && activity.id !== null)
          .map(async (activity, index) => {
            try {
              const categoryRes = await listSchoolCategoryCatalog(activity.id as string | number);
              return { activity, categories: categoryRes.data || [], index };
            } catch (error) {
              console.warn('生成参赛者活动菜单失败', activity.activityName || activity.id, error);
              return undefined;
            }
          })
      )
    ).filter((item): item is { activity: ActivityVO; categories: ActivityCategoryVO[]; index: number } => Boolean(item));
    return buildParticipantReportingMenuForCatalogs(activityCatalogs);
  } catch (error) {
    console.warn('生成参赛者项目填报菜单失败', error);
    return undefined;
  }
};

export const buildParticipantReportingMenuForCatalogs = (
  activityCatalogs: Array<{ activity: ActivityVO; categories: ActivityCategoryVO[]; index: number }>
): RouteRecordRaw | undefined => {
  const children = buildParticipantReportingMenusForCatalogs(activityCatalogs);
  if (!children.length) {
    return undefined;
  }
  return {
    path: participantReportingRootPath,
    component: 'ParentView' as any,
    name: 'ArtParticipantReporting',
    alwaysShow: true,
    redirect: 'noRedirect',
    meta: { title: '项目报送', icon: 'form' },
    children
  };
};

export const buildParticipantReportingMenusForCatalogs = (
  activityCatalogs: Array<{ activity: ActivityVO; categories: ActivityCategoryVO[]; index: number }>
): RouteRecordRaw[] => {
  const availableCatalogs = activityCatalogs.filter((item) => participantMenuTree(item.categories).length > 0);
  if (!availableCatalogs.length) {
    return [];
  }
  if (availableCatalogs.length === 1) {
    const [catalog] = availableCatalogs;
    return buildActivityCategoryMenus(catalog.activity, catalog.categories, catalog.index, 'ArtParticipantReportingSingle');
  }
  return availableCatalogs
    .map((catalog) => buildActivityMenu(catalog.activity, catalog.categories, catalog.index))
    .filter((item): item is RouteRecordRaw => Boolean(item));
};

const buildActivityMenu = (activity: ActivityVO, categories: ActivityCategoryVO[], activityIndex: number): RouteRecordRaw | undefined => {
  if (!activity.id) {
    return undefined;
  }
  const menuNodes = participantMenuTree(categories);
  if (!menuNodes.length) {
    return undefined;
  }
  const activitySegment = routeSegment(`activity-${activity.id}`);
  const activityTitle = activityMenuTitle(activity, activityIndex);
  const activityFullTitle = activity.activityName || activityTitle;
  const routeNamePrefix = `ArtParticipantReportingActivity${safeRouteName(activity.id, activityIndex)}`;
  return {
    path: activitySegment,
    component: 'ParentView' as any,
    name: routeNamePrefix,
    alwaysShow: true,
    redirect: 'noRedirect',
    meta: { title: activityTitle, fullTitle: activityFullTitle, icon: 'calendar', multilineTitle: true },
    children: buildActivityCategoryMenus(activity, categories, activityIndex, routeNamePrefix)
  };
};

const buildActivityCategoryMenus = (
  activity: ActivityVO,
  categories: ActivityCategoryVO[],
  activityIndex: number,
  routeNamePrefix: string
): RouteRecordRaw[] => {
  return participantMenuTree(categories).map((node, nodeIndex) => {
    if (!isCategoryGroup(node)) {
      return buildCategoryMenu(activity, node, activityIndex, nodeIndex, 0, routeNamePrefix);
    }
    const style = groupMenuStyle(node, node.children || []);
    const groupName = String(node.categoryName || `大类${nodeIndex + 1}`);
    return {
      path: routeSegment(`group-${node.categoryCode || groupName}-${nodeIndex}`),
      component: 'ParentView' as any,
      name: `${routeNamePrefix}Group${safeRouteName(node.categoryCode || node.id || nodeIndex, nodeIndex)}`,
      alwaysShow: true,
      redirect: 'noRedirect',
      query: JSON.stringify({ activityId: activity.id, categoryNodeId: node.id, categoryGroup: groupName }),
      meta: { title: groupName, icon: style.groupIcon || groupIcon(groupName), menuIconColor: style.groupColor || undefined },
      children: (node.children || []).map((category, categoryIndex) =>
        buildCategoryMenu(activity, category, activityIndex, nodeIndex, categoryIndex, `${routeNamePrefix}Group${nodeIndex}`)
      )
    };
  });
};

const buildCategoryMenu = (
  activity: ActivityVO,
  category: ActivityCategoryVO,
  activityIndex: number,
  groupIndex: number,
  categoryIndex: number,
  routeNamePrefix: string
): RouteRecordRaw => {
  const style = categoryMenuStyle(category);
  const categoryKey = category.categoryCode || category.id || `${groupIndex}-${categoryIndex}`;
  const query: Record<string, string | number | undefined> = {
    activityId: activity.id,
    categoryNodeId: category.id,
    categoryId: category.id,
    categoryCode: category.categoryCode,
    categoryName: category.categoryName,
    categoryGroup: category.categoryGroup
  };
  return {
    path: routeSegment(`category-${categoryKey}-${activityIndex}-${groupIndex}-${categoryIndex}`),
    component: projectListComponent as any,
    name: `${routeNamePrefix}Category${safeRouteName(categoryKey, categoryIndex)}`,
    query: JSON.stringify(query),
    meta: {
      title: category.categoryName || `类别${categoryIndex + 1}`,
      icon: style.categoryIcon || categoryIcon(category),
      menuIconColor: style.categoryColor || undefined
    }
  };
};

const activityMenuTitle = (activity: ActivityVO, activityIndex: number) => {
  const configured = String(activity.menuName || '').trim();
  if (configured) {
    return configured;
  }
  return activity.activityName || `活动${activityIndex + 1}`;
};

const participantMenuTree = (categories: ActivityCategoryVO[]): ActivityCategoryTreeNode[] => buildSchoolCategoryMenuTree(categories);

const inferCategoryGroup = (name?: string) => {
  const text = name || '';
  if (text.includes('校长书画')) return '高校校长书画作品';
  if (['声乐', '器乐', '舞蹈', '戏剧', '朗诵', '个人'].some((item) => text.includes(item))) return '艺术表演类';
  if (['美术', '设计', '影视', '校长'].some((item) => text.includes(item))) return '艺术作品类';
  if (text.includes('工作坊')) return '艺术实践工作坊';
  if (['论文', '案例', '成果', '美育'].some((item) => text.includes(item))) return '高校美育改革创新优秀成果';
  return '其他';
};

const resolveReportGroupKey = (groupName?: string) => {
  const text = groupName || '';
  return reportGroupDefs.find((group) => [group.label, ...group.aliases].some((alias) => text.includes(alias)))?.key || 'other';
};

const parseRouteJsonObject = (json?: string): Record<string, unknown> => {
  if (!json) return {};
  try {
    const parsed = JSON.parse(json);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
};

const menuStyleText = (value: unknown) => (typeof value === 'string' ? value.trim() : '');

const categoryMenuStyle = (category: ActivityCategoryVO): ReportMenuStyle => {
  const style = (parseRouteJsonObject(category.ruleJson) as any).menuStyle;
  if (!style || typeof style !== 'object' || Array.isArray(style)) {
    return {};
  }
  return {
    groupIcon: menuStyleText(style.groupIcon),
    groupColor: menuStyleText(style.groupColor),
    categoryIcon: menuStyleText(style.categoryIcon),
    categoryColor: menuStyleText(style.categoryColor)
  };
};

const groupMenuStyle = (group: ActivityCategoryVO, categories: ActivityCategoryVO[]): ReportMenuStyle => {
  for (const category of [group, ...categories]) {
    const style = categoryMenuStyle(category);
    if (style.groupIcon || style.groupColor) {
      return style;
    }
  }
  return {};
};

const useReportMenuSolidIcon = () => true;

const groupIcon = (groupName: string) => {
  if (useReportMenuSolidIcon()) return 'solid-circle';
  const iconMap: Record<string, string> = {
    performance: 'star',
    artwork: 'color',
    workshop: 'guide',
    achievement: 'finish',
    principal: 'education'
  };
  return iconMap[resolveReportGroupKey(groupName)] || 'list';
};

const categoryIcon = (category: ActivityCategoryVO) => {
  if (useReportMenuSolidIcon()) return 'solid-circle';
  const code = String(category.categoryCode || '');
  const name = String(category.categoryName || '');
  if (code.includes('vocal') || name.includes('声乐')) return 'message';
  if (code.includes('instrumental') || name.includes('器乐')) return 'skill';
  if (code.includes('dance') || name.includes('舞蹈')) return 'star';
  if (code.includes('drama') || name.includes('戏剧')) return 'component';
  if (code.includes('recitation') || name.includes('朗诵')) return 'documentation';
  if (code.includes('personal') || name.includes('个人')) return 'user';
  return groupIcon(category.categoryGroup || inferCategoryGroup(category.categoryName));
};

const trimPath = (path?: string) => String(path || '').replace(/^\/+|\/+$/g, '');

const routeSegment = (value: string) => {
  const text = value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return text || `group-${Date.now().toString(36)}`;
};

const safeRouteName = (value: string | number, fallback: number) => {
  const text = String(value || fallback)
    .replace(/[^a-zA-Z0-9]/g, '')
    .replace(/^0+/, '');
  if (!text) {
    return `Node${fallback}`;
  }
  return /^[a-zA-Z]/.test(text) ? text : `Node${text}`;
};

// 非setup
export const usePermissionStoreHook = () => {
  return usePermissionStore(store);
};

interface Route {
  name?: string | symbol;
  path: string;
  children?: Route[];
}

/**
 * 检查路由name是否重复
 * @param localRoutes 本地路由
 * @param routes 动态路由
 */
function duplicateRouteChecker(localRoutes: Route[], routes: Route[]) {
  // 展平
  function flatRoutes(routes: Route[]) {
    const res: Route[] = [];
    routes.forEach((route) => {
      if (route.children) {
        res.push(...flatRoutes(route.children));
      } else {
        res.push(route);
      }
    });
    return res;
  }

  const allRoutes = flatRoutes([...localRoutes, ...routes]);

  const nameList: string[] = [];
  allRoutes.forEach((route) => {
    const name = route.name.toString();
    if (name && nameList.includes(name)) {
      const message = `路由名称: [${name}] 重复, 会造成 404`;
      console.error(message);
      ElNotification({
        title: '路由名称重复',
        message,
        type: 'error'
      });
      return;
    }
    nameList.push(route.name.toString());
  });
}
