/**
 * 本地种子说明数据。真实业务数据全部来自后端数据库（database/init.sql），
 * 这里仅保留演示账号与默认值常量，供登录页与离线说明使用，不接入任何第三方 API。
 */
import type { LoginUser } from "../types/api";

export const DEMO_ACCOUNTS: { username: string; password: string; user: Omit<LoginUser, "id"> & { id: number } }[] = [
  { username: "admin", password: "password123", user: { id: 1, username: "admin", displayName: "街道管理员", role: "STREET_ADMIN" } },
  { username: "keeper", password: "password123", user: { id: 2, username: "keeper", displayName: "中心仓仓库员", role: "WAREHOUSE_KEEPER" } },
  { username: "approver", password: "password123", user: { id: 3, username: "approver", displayName: "应急审批员", role: "APPROVER" } },
  { username: "observer", password: "password123", user: { id: 4, username: "observer", displayName: "只读观察员", role: "OBSERVER" } }
];

export const SEED_NOTE =
  "仓库/物资/批次/避难点/事件种子数据见 database/init.sql，由 MySQL 容器首次启动自动导入。";
