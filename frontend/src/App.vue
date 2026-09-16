<script setup lang="ts">
import { computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { APP_ROUTES } from "./router/routes";
import { useAuthStore } from "./stores/auth";
import { ROLE_TEXT } from "./constants/permission";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const current = computed(() => APP_ROUTES.find((r) => r.route === route.path));

const go = (path: string) => router.push(path);
const logout = () => {
  auth.logout();
  router.push("/login");
};

onMounted(() => {
  if (!auth.isLoggedIn) router.push("/login");
});
</script>

<template>
  <router-view v-if="route.path === '/login'" />
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        城市防灾应急物资调度
        <small>RESCUE-STOCK</small>
      </div>
      <nav>
        <button
          v-for="r in APP_ROUTES"
          :key="r.route"
          :class="{ active: route.path === r.route }"
          @click="go(r.route)"
        >
          <span>{{ r.title }}</span>
        </button>
      </nav>
      <div v-if="auth.user" class="user-box">
        <div>{{ auth.user.displayName }}</div>
        <div class="role">{{ ROLE_TEXT[auth.user.role] }}</div>
        <el-button size="small" plain @click="logout">退出登录</el-button>
      </div>
    </aside>
    <section class="main">
      <header class="topbar">
        <h2>{{ current?.title ?? "城市防灾应急物资调度系统" }}</h2>
        <div class="muted small">申请占用 → 审批 → 出库 → 签收 / 拒签 · 全链路批次可追溯</div>
      </header>
      <div class="content">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </div>
    </section>
  </div>
</template>
