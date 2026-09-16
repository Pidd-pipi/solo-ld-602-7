import { defineStore } from "pinia";
import { login as apiLogin } from "../api/auth";
import type { LoginUser } from "../types/api";

const TOKEN_KEY = "rescue-stock-token";
const USER_KEY = "rescue-stock-user";

/** 登录态：token 持久化到 localStorage，刷新不丢；登出/401 时清空。 */
export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) ?? "",
    user: JSON.parse(localStorage.getItem(USER_KEY) ?? "null") as LoginUser | null
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    role: (s) => s.user?.role
  },
  actions: {
    async login(username: string, password: string) {
      const result = await apiLogin(username, password);
      this.token = result.token;
      this.user = result.user;
      localStorage.setItem(TOKEN_KEY, result.token);
      localStorage.setItem(USER_KEY, JSON.stringify(result.user));
    },
    clear() {
      this.token = "";
      this.user = null;
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    },
    logout() {
      this.clear();
    }
  }
});
