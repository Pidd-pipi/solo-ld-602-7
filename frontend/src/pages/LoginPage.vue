<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useAuthStore } from "../stores/auth";
import { RequestError } from "../utils/request";

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();
const form = reactive({ username: "approver", password: "password123" });
const loading = ref(false);

const demoAccounts = [
  { username: "admin", role: "街道管理员" },
  { username: "keeper", role: "仓库员" },
  { username: "approver", role: "审批员" },
  { username: "observer", role: "只读观察员" }
];

const submit = async () => {
  loading.value = true;
  try {
    await auth.login(form.username, form.password);
    ElMessage.success("登录成功");
    router.push((route.query.redirect as string) || "/dashboard");
  } catch (e) {
    ElMessage.error(e instanceof RequestError ? e.message : "登录失败");
  } finally {
    loading.value = false;
  }
};

const fill = (username: string) => {
  form.username = username;
  form.password = "password123";
};
</script>

<template>
  <div class="login-wrap">
    <div class="login-card">
      <h1>城市防灾应急物资调度系统</h1>
      <div class="sub">rescue-stock · 本地演示环境</div>
      <el-form @submit.prevent="submit">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">登录</el-button>
      </el-form>
      <div class="login-tip">
        演示账号（密码均为 <code>password123</code>），点击可填充：
        <div style="display:flex;gap:6px;flex-wrap:wrap;margin-top:6px">
          <el-tag v-for="a in demoAccounts" :key="a.username" style="cursor:pointer" @click="fill(a.username)">
            {{ a.username }} · {{ a.role }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>
