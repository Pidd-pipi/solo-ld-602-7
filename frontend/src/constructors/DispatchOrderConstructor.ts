import type { DispatchOrder } from "../types/DispatchOrder";
import type { ApplyDispatchPayload, ApplyLine } from "../types/api";

let seq = 0;
/** 申请端生成幂等键：同一申请多次提交沿用同一 requestId，后端据此不重复占库。 */
export const newRequestId = (): string => {
  seq += 1;
  const rand = Math.random().toString(16).slice(2, 10);
  return `REQ-${Date.now().toString(36).toUpperCase()}-${seq}-${rand}`;
};

export const createDefaultDispatchOrder = (overrides: Partial<DispatchOrder> = {}): DispatchOrder => ({
  id: 0,
  request_id: "",
  event_id: null,
  source_warehouse_id: 0,
  shelter_id: 0,
  priority: "NORMAL",
  status: "SUBMITTED",
  remark: "",
  requested_by: null,
  approved_by: null,
  reject_reason: "",
  refuse_reason: "",
  cancel_reason: "",
  submitted_at: "",
  approved_at: null,
  dispatched_at: null,
  received_at: null,
  refused_at: null,
  canceled_at: null,
  ...overrides
});

/** 新建调拨申请表单对象，页面/store 不得散写默认结构。 */
export const createDispatchApplyForm = (
  overrides: Partial<ApplyDispatchPayload> = {}
): ApplyDispatchPayload => ({
  requestId: newRequestId(),
  eventId: null,
  sourceWarehouseId: 0,
  shelterId: 0,
  priority: "NORMAL",
  remark: "",
  lines: [],
  ...overrides
});

export const createApplyLine = (supplyItemId = 0, requestedQty = 1): ApplyLine => ({
  supplyItemId,
  requestedQty
});

export const createDispatchOrderResponse = createDefaultDispatchOrder;
