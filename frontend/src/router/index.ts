import { createWebHistory, createRouter, RouteRecordRaw } from 'vue-router';
/* Layout */
import Layout from '@/layout/index.vue';

/**
 * Note: 璺敱閰嶇疆椤?
 *
 * hidden: true                     // 褰撹缃?true 鐨勬椂鍊欒璺敱涓嶄細鍐嶄晶杈规爮鍑虹幇 濡?01锛宭ogin绛夐〉闈紝鎴栬€呭涓€浜涚紪杈戦〉闈?edit/1
 * alwaysShow: true                 // 褰撲綘涓€涓矾鐢变笅闈㈢殑 children 澹版槑鐨勮矾鐢卞ぇ浜?涓椂锛岃嚜鍔ㄤ細鍙樻垚宓屽鐨勬ā寮?-濡傜粍浠堕〉闈?
 *                                  // 鍙湁涓€涓椂锛屼細灏嗛偅涓瓙璺敱褰撳仛鏍硅矾鐢辨樉绀哄湪渚ц竟鏍?-濡傚紩瀵奸〉闈?
 *                                  // 鑻ヤ綘鎯充笉绠¤矾鐢变笅闈㈢殑 children 澹版槑鐨勪釜鏁伴兘鏄剧ず浣犵殑鏍硅矾鐢?
 *                                  // 浣犲彲浠ヨ缃?alwaysShow: true锛岃繖鏍峰畠灏变細蹇界暐涔嬪墠瀹氫箟鐨勮鍒欙紝涓€鐩存樉绀烘牴璺敱
 * redirect: noRedirect             // 褰撹缃?noRedirect 鐨勬椂鍊欒璺敱鍦ㄩ潰鍖呭睉瀵艰埅涓笉鍙鐐瑰嚮
 * name:'router-name'               // 璁惧畾璺敱鐨勫悕瀛楋紝涓€瀹氳濉啓涓嶇劧浣跨敤<keep-alive>鏃朵細鍑虹幇鍚勭闂
 * query: '{"id": 1, "name": "ry"}' // 璁块棶璺敱鐨勯粯璁や紶閫掑弬鏁?
 * roles: ['admin', 'common']       // 璁块棶璺敱鐨勮鑹叉潈闄?
 * permissions: ['a:a:a', 'b:b:b']  // 璁块棶璺敱鐨勮彍鍗曟潈闄?
 * meta : {
    noCache: true                   // 濡傛灉璁剧疆涓簍rue锛屽垯涓嶄細琚?<keep-alive> 缂撳瓨(榛樿 false)
    title: 'title'                  // 璁剧疆璇ヨ矾鐢卞湪渚ц竟鏍忓拰闈㈠寘灞戜腑灞曠ず鐨勫悕瀛?
    icon: 'svg-name'                // 璁剧疆璇ヨ矾鐢辩殑鍥炬爣锛屽搴旇矾寰剆rc/assets/icons/svg
    breadcrumb: false               // 濡傛灉璁剧疆涓篺alse锛屽垯涓嶄細鍦╞readcrumb闈㈠寘灞戜腑鏄剧ず
    activeMenu: '/system/user'      // 褰撹矾鐢辫缃簡璇ュ睘鎬э紝鍒欎細楂樹寒鐩稿搴旂殑渚ц竟鏍忋€?
  }
 */

// 鍏叡璺敱
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue')
      }
    ]
  },
  {
    path: '/social-callback',
    hidden: true,
    component: () => import('@/layout/components/SocialCallback/index.vue')
  },
  {
    path: '/login',
    component: () => import('@/views/login.vue'),
    hidden: true
  },
  {
    path: '/register',
    component: () => import('@/views/register.vue'),
    hidden: true
  },
  {
    path: '/activate',
    component: () => import('@/views/activate.vue'),
    hidden: true
  },
  {
    path: '/crehn/project/submit',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/crehn/project-submit/index.vue'),
        name: 'ArtProjectSubmit',
        meta: { title: '统一提交', activeMenu: '/crehn/project', noCache: true }
      }
    ]
  },
  {
    path: '/crehn/project/edit/:id?',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/crehn/project/edit.vue'),
        name: 'ArtProjectEdit',
        meta: {
          title: '项目填报',
          activeMenu: '/crehn/project',
          noCache: true,
          closeConfirm: true,
          closeConfirmMessage: '关闭项目填报页前请确认已保存草稿。确认继续关闭？'
        }
      }
    ]
  },
  {
    path: '/crehn/school',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/crehn/school/index.vue'),
        name: 'ArtSchool',
        meta: { title: '单位与账号', activeMenu: '/crehn/school' }
      }
    ]
  },
  {
    path: '/crehn/recycle',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/crehn/recycle/index.vue'),
        name: 'ArtRecycle',
        meta: { title: '作品/附件回收站', activeMenu: '/crehn/recycle' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/error/404.vue'),
    hidden: true
  },
  {
    path: '/401',
    component: () => import('@/views/error/401.vue'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: '/index',
    children: [
      {
        path: '/index',
        component: () => import('@/views/index.vue'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile',
        component: () => import('@/views/system/user/profile/index.vue'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  }
];

// 鍔ㄦ€佽矾鐢憋紝鍩轰簬鐢ㄦ埛鏉冮檺鍔ㄦ€佸幓鍔犺浇
export const dynamicRoutes: RouteRecordRaw[] = [

];

/**
 * 鍒涘缓璺敱
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.VITE_APP_CONTEXT_PATH),
  routes: constantRoutes,
  // 鍒锋柊鏃讹紝婊氬姩鏉′綅缃繕鍘?
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    }
    return { top: 0 };
  }
});

export default router;
