import type { LoginUser } from "../types/api";

/**
 * 前端 RBAC 按钮显隐映射，与后端 RbacMiddleware 完全对应；
 * 仅用于体验（禁用/隐藏按钮），真正鉴权在后端中间件。
 */
export type Role = LoginUser["role"];

export const ROLE_TEXT: Record<Role, string> = {
  STREET_ADMIN: "街道管理员",
  WAREHOUSE_KEEPER: "仓库员",
  APPROVER: "审批员",
  OBSERVER: "只读观察员"
};

export type DispatchActionName =
  | "apply"
  | "approve"
  | "reject"
  | "outbound"
  | "receive"
  | "refuse"
  | "cancel";

const ADMIN = "STREET_ADMIN";
const KEEPER = "WAREHOUSE_KEEPER";
const APPROVER = "APPROVER";

export const DISPATCH_ACTION_ROLES: Record<DispatchActionName, Role[]> = {
  apply: [ADMIN, KEEPER],
  approve: [ADMIN, APPROVER],
  reject: [ADMIN, APPROVER],
  outbound: [ADMIN, KEEPER],
  receive: [ADMIN, KEEPER],
  refuse: [ADMIN, KEEPER],
  cancel: [ADMIN]
};

export const canDispatch = (role: Role | undefined, action: DispatchActionName): boolean =>
  !!role && DISPATCH_ACTION_ROLES[action].includes(role);
