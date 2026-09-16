/**
 * 调拨状态机，取值与后端 constants/DispatchStatus.java 完全一致。
 * 出现在：类型、构造器、日志模板、错误消息、列表筛选器、StatusBadge、ApprovalTimeline、store。
 */
export const DispatchStatus = [
  "DRAFT",
  "SUBMITTED",
  "APPROVED",
  "DISPATCHED",
  "RECEIVED",
  "REJECTED",
  "REFUSED",
  "CANCELLED"
] as const;

export type DispatchStatus = (typeof DispatchStatus)[number];

export const DispatchStatusText: Record<DispatchStatus, string> = {
  DRAFT: "草稿",
  SUBMITTED: "待审批",
  APPROVED: "已批准",
  DISPATCHED: "已出库",
  RECEIVED: "已签收",
  REJECTED: "已驳回",
  REFUSED: "已拒签",
  CANCELLED: "已撤销"
};

/** StatusBadge / 筛选器配色。 */
export const DispatchStatusType: Record<DispatchStatus, "info" | "warning" | "primary" | "success" | "danger"> = {
  DRAFT: "info",
  SUBMITTED: "warning",
  APPROVED: "primary",
  DISPATCHED: "primary",
  RECEIVED: "success",
  REJECTED: "danger",
  REFUSED: "danger",
  CANCELLED: "info"
};

/** 状态机可执行动作，用于审批详情按钮显隐；与后端迁移表保持一致。 */
export const DispatchStatusFlow: Record<DispatchStatus, DispatchStatus[]> = {
  DRAFT: ["SUBMITTED", "CANCELLED"],
  SUBMITTED: ["APPROVED", "REJECTED", "CANCELLED"],
  APPROVED: ["DISPATCHED", "CANCELLED"],
  DISPATCHED: ["RECEIVED", "REFUSED", "CANCELLED"],
  RECEIVED: [],
  REJECTED: [],
  REFUSED: [],
  CANCELLED: []
};
