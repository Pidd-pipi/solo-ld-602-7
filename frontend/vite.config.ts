import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 20102,
    host: "0.0.0.0",
    proxy: {
      // 本地开发把 /api 转发到后端；容器内由 nginx 反代。前端代码始终只请求相对 /api
      "/api": {
        target: "http://localhost:21102",
        changeOrigin: true
      }
    }
  }
});
