import type { DispatchStatus } from "../constants/DispatchStatus";

/** 调拨单列表行（后端聚合了名称与数量）。 */
export interface DispatchOrder {
  id: number;
  request_id: string;
  event_id: number | null;
  source_warehouse_id: number;
  shelter_id: number;
  priority: string;
  status: DispatchStatus;
  remark: string;
  requested_by: number | null;
  approved_by: number | null;
  reject_reason?: string;
  refuse_reason?: string;
  cancel_reason?: string;
  submitted_at: string;
  approved_at: string | null;
  dispatched_at: string | null;
  received_at: string | null;
  refused_at: string | null;
  canceled_at: string | null;
  warehouse_name?: string;
  shelter_name?: string;
  shelter_district?: string;
  event_title?: string;
  requester_name?: string;
  line_count?: number;
  total_requested?: number;
  total_outbound?: number;
}

export interface DispatchLine {
  id: number;
  dispatch_order_id: number;
  supply_item_id: number;
  requested_qty: number;
  outbound_qty: number;
  supply_name?: string;
  sku_code?: string;
  category?: string;
  unit?: string;
}

export interface BatchAllocation {
  id: number;
  dispatch_order_id: number;
  dispatch_line_id: number;
  inventory_batch_id: number;
  allocated_qty: number;
  returned_qty: number;
  stage: "HELD" | "OUT" | "RETURNED";
  batch_no?: string;
  supply_name?: string;
  unit?: string;
  warehouse_name?: string;
}

export interface TimelineEntry {
  id: number;
  dispatch_order_id: number;
  action: string;
  from_status: string;
  to_status: string;
  actor: string;
  note: string;
  created_at: string;
}

/** 审批详情。 */
export interface DispatchDetail extends DispatchOrder {
  lines: DispatchLine[];
  allocations: BatchAllocation[];
  timeline: TimelineEntry[];
  heldUnits?: number;
  outUnits?: number;
  returnedUnits?: number;
}
