import { createRouter, createWebHashHistory, type RouteRecordRaw } from "vue-router";
import { useAuthStore } from "../stores/auth";

export const APP_ROUTES: { name: string; route: string; title: string }[] = [
  { name: "dashboard", route: "/dashboard", title: "应急态势大屏" },
  { name: "warehouses", route: "/warehouses", title: "仓库库存" },
  { name: "shelters", route: "/shelters", title: "避难点管理" },
  { name: "dispatch", route: "/dispatch", title: "调拨审批" },
  { name: "events", route: "/events", title: "事件响应" }
];

const routes: RouteRecordRaw[] = [
  { path: "/login", name: "login", component: () => import("../pages/LoginPage.vue"), meta: { public: true } },
  { path: "/", redirect: "/dashboard" },
  { path: "/dashboard", name: "dashboard", component: () => import("../pages/DashboardPage.vue") },
  { path: "/warehouses", name: "warehouses", component: () => import("../pages/WarehousesPage.vue") },
  { path: "/shelters", name: "shelters", component: () => import("../pages/SheltersPage.vue") },
  { path: "/dispatch", name: "dispatch", component: () => import("../pages/DispatchPage.vue") },
  { path: "/events", name: "events", component: () => import("../pages/EventsPage.vue") },
  { path: "/:pathMatch(.*)*", redirect: "/dashboard" }
];

export const router = createRouter({
  history: createWebHashHistory(),
  routes
});

// 路由守卫：未登录只允许进登录页
router.beforeEach((to) => {
  const auth = useAuthStore();
  if (!to.meta.public && !auth.isLoggedIn) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }
  if (to.path === "/login" && auth.isLoggedIn) {
    return { path: "/dashboard" };
  }
  return true;
});
